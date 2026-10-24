package com.fswebdev.fit90.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.fswebdev.fit90.core.Days;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Encrypted local database. Each value is a JSON document encrypted with AES-GCM
 * (see {@link Crypto}); only timestamps are stored in the clear so entries can be
 * sorted. Nothing here ever leaves the phone except through "Export my data".
 *
 * Two tables:
 *   kv(k, v)              named documents: profile, plan edits, trainer state...
 *   events(id, ts, day, v) an append-only log: sessions, reflections, journal,
 *                          check-ins, practice logs, chat messages.
 */
public final class Vault extends SQLiteOpenHelper {

    public static final String DB_NAME = "vault.db";
    private static final Charset UTF8 = Charset.forName("UTF-8");

    public static final class Event {
        public final long id;
        public final long ts;
        public final int day;
        public final String type;
        public final JSONObject data;

        Event(long id, long ts, int day, JSONObject data) {
            this.id = id;
            this.ts = ts;
            this.day = day;
            this.data = data;
            this.type = data.optString("type");
        }
    }

    private static Vault instance;
    private final Crypto crypto;
    private final Map<String, String> kvCache = new HashMap<String, String>();
    private List<Event> eventCache;

    private Vault(Context context) {
        super(context.getApplicationContext(), DB_NAME, null, 1);
        crypto = new Crypto(context.getApplicationContext());
    }

    public static synchronized Vault of(Context context) {
        if (instance == null) instance = new Vault(context);
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE kv (k TEXT PRIMARY KEY, v BLOB NOT NULL)");
        db.execSQL("CREATE TABLE events (id INTEGER PRIMARY KEY AUTOINCREMENT, ts INTEGER NOT NULL, "
                + "day INTEGER NOT NULL, v BLOB NOT NULL)");
        db.execSQL("CREATE INDEX events_day ON events(day)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 is the first schema.
    }

    private byte[] seal(String json) {
        return crypto.encrypt(json.getBytes(UTF8));
    }

    private String open(byte[] blob) {
        return new String(crypto.decrypt(blob), UTF8);
    }

    // ---------------------------------------------------------------- documents

    private synchronized String raw(String key) {
        if (kvCache.containsKey(key)) return kvCache.get(key);
        String value = null;
        Cursor c = getReadableDatabase().query("kv", new String[]{"v"}, "k = ?", new String[]{key},
                null, null, null);
        try {
            if (c.moveToFirst()) value = open(c.getBlob(0));
        } finally {
            c.close();
        }
        kvCache.put(key, value);
        return value;
    }

    /** A named document, or an empty object. Changes are saved only by {@link #put}. */
    public JSONObject obj(String key) {
        String s = raw(key);
        try {
            return s == null ? new JSONObject() : new JSONObject(s);
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public JSONArray arr(String key) {
        String s = raw(key);
        try {
            return s == null ? new JSONArray() : new JSONArray(s);
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public boolean has(String key) {
        return raw(key) != null;
    }

    public synchronized void put(String key, Object jsonValue) {
        String s = jsonValue.toString();
        ContentValues v = new ContentValues();
        v.put("k", key);
        v.put("v", seal(s));
        getWritableDatabase().insertWithOnConflict("kv", null, v, SQLiteDatabase.CONFLICT_REPLACE);
        kvCache.put(key, s);
    }

    public synchronized void remove(String key) {
        getWritableDatabase().delete("kv", "k = ?", new String[]{key});
        kvCache.remove(key);
    }

    // ------------------------------------------------------------------- events

    private synchronized List<Event> loadEvents() {
        if (eventCache != null) return eventCache;
        List<Event> list = new ArrayList<Event>();
        Cursor c = getReadableDatabase().query("events", new String[]{"id", "ts", "day", "v"},
                null, null, null, null, "ts ASC, id ASC");
        try {
            while (c.moveToNext()) {
                try {
                    list.add(new Event(c.getLong(0), c.getLong(1), c.getInt(2), new JSONObject(open(c.getBlob(3)))));
                } catch (Exception skip) {
                    // A row that cannot be decrypted (e.g. after a key reset) is ignored.
                }
            }
        } finally {
            c.close();
        }
        eventCache = list;
        return list;
    }

    /** Appends an event of {@code type}. {@code data} may be null. */
    public synchronized Event add(String type, JSONObject data) {
        return addAt(type, data, System.currentTimeMillis());
    }

    public synchronized Event addAt(String type, JSONObject data, long ts) {
        JSONObject d = data == null ? new JSONObject() : data;
        try {
            d.put("type", type);
        } catch (JSONException ignored) {
            // put() with a String key never fails.
        }
        int day = Days.dayOf(ts);
        ContentValues v = new ContentValues();
        v.put("ts", ts);
        v.put("day", day);
        v.put("v", seal(d.toString()));
        long id = getWritableDatabase().insert("events", null, v);
        Event e = new Event(id, ts, day, d);
        loadEvents().add(e);
        return e;
    }

    public synchronized void update(Event e) {
        ContentValues v = new ContentValues();
        v.put("v", seal(e.data.toString()));
        getWritableDatabase().update("events", v, "id = ?", new String[]{String.valueOf(e.id)});
    }

    public synchronized void delete(long id) {
        getWritableDatabase().delete("events", "id = ?", new String[]{String.valueOf(id)});
        List<Event> list = loadEvents();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (list.get(i).id == id) list.remove(i);
        }
    }

    public synchronized void deleteAll(String type) {
        for (Event e : events(type)) delete(e.id);
    }

    /** Events of one type, oldest first. */
    public synchronized List<Event> events(String type) {
        List<Event> out = new ArrayList<Event>();
        for (Event e : loadEvents()) {
            if (type == null || type.equals(e.type)) out.add(e);
        }
        return out;
    }

    public synchronized List<Event> all() {
        return new ArrayList<Event>(loadEvents());
    }

    // -------------------------------------------------------------- export/wipe

    public synchronized JSONObject export() throws JSONException {
        JSONObject out = new JSONObject();
        JSONObject docs = new JSONObject();
        Cursor c = getReadableDatabase().query("kv", new String[]{"k"}, null, null, null, null, "k");
        try {
            while (c.moveToNext()) {
                String k = c.getString(0);
                String s = raw(k);
                if (s == null) continue;
                Object v = s.trim().startsWith("[") ? new JSONArray(s) : new JSONObject(s);
                docs.put(k, v);
            }
        } finally {
            c.close();
        }
        out.put("documents", docs);
        JSONArray events = new JSONArray();
        for (Event e : loadEvents()) {
            JSONObject o = new JSONObject();
            o.put("time", e.ts);
            o.put("data", e.data);
            events.put(o);
        }
        out.put("events", events);
        return out;
    }

    /** Deletes the database file and the encryption key. */
    public static synchronized void wipe(Context context) {
        if (instance != null) {
            instance.close();
            instance = null;
        }
        context.getApplicationContext().deleteDatabase(DB_NAME);
        Crypto.destroyKey(context.getApplicationContext());
    }
}
