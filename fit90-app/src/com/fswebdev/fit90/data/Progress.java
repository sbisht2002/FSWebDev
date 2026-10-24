package com.fswebdev.fit90.data;

import android.content.Context;

import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.core.Gamify;
import com.fswebdev.fit90.core.Kegel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/** Records finished sessions and derives XP, level, streak, badges and trainer state from the log. */
public final class Progress {

    private Progress() {
    }

    /**
     * Logs a finished session. {@code kind}: kegel, breath, audio, lesson, practice,
     * sensate, journal, checkin, walk, workout. {@code key} links it to a plan item.
     */
    public static Vault.Event record(Context c, String kind, String ref, int minutes, int ease, int mood, String note) {
        JSONObject d = new JSONObject();
        try {
            d.put("kind", kind).put("ref", ref).put("minutes", minutes).put("xp", Gamify.xpForSession(minutes));
            if (ease > 0) d.put("ease", ease);
            if (mood > 0) d.put("mood", mood);
            if (note != null && !note.trim().isEmpty()) d.put("note", note.trim());
        } catch (JSONException ignored) {
            // Keys are constant strings.
        }
        Vault.Event e = Vault.of(c).add("session", d);
        if ("kegel".equals(kind)) updateKegel(c, ease);
        return e;
    }

    /** Adds a reflection to an already-recorded session. */
    public static void reflect(Context c, Vault.Event e, int ease, int mood, String note) {
        try {
            if (ease > 0) e.data.put("ease", ease);
            if (mood > 0) e.data.put("mood", mood);
            if (note != null && !note.trim().isEmpty()) e.data.put("note", note.trim());
        } catch (JSONException ignored) {
            // Keys are constant strings.
        }
        Vault.of(c).update(e);
        if ("kegel".equals(e.data.optString("kind")) && ease > 0) {
            JSONObject k = Vault.of(c).obj("kegel");
            JSONArray ratings = k.optJSONArray("ease");
            if (ratings != null && ratings.length() > 0) {
                try {
                    ratings.put(ratings.length() - 1, ease);
                    Vault.of(c).put("kegel", k);
                } catch (JSONException ignored) {
                    // Index is in range.
                }
            }
        }
    }

    // ---------------------------------------------------------------- Kegel

    public static int kegelLevel(Context c) {
        return Math.max(1, Vault.of(c).obj("kegel").optInt("level", 1));
    }

    public static int kegelSessionsAtLevel(Context c) {
        return Vault.of(c).obj("kegel").optInt("sessions", 0);
    }

    public static List<Integer> kegelEase(Context c) {
        List<Integer> out = new ArrayList<Integer>();
        JSONArray a = Vault.of(c).obj("kegel").optJSONArray("ease");
        if (a != null) for (int i = 0; i < a.length(); i++) if (a.optInt(i) > 0) out.add(a.optInt(i));
        return out;
    }

    private static void updateKegel(Context c, int ease) {
        JSONObject k = Vault.of(c).obj("kegel");
        try {
            k.put("level", Math.max(1, k.optInt("level", 1)));
            k.put("sessions", k.optInt("sessions", 0) + 1);
            JSONArray a = k.optJSONArray("ease");
            if (a == null) a = new JSONArray();
            a.put(ease);
            k.put("ease", a);
            JSONArray history = k.optJSONArray("history");
            if (history == null) history = new JSONArray();
            k.put("history", history);
        } catch (JSONException ignored) {
            // Keys are constant strings.
        }
        Vault.of(c).put("kegel", k);
    }

    public static void setKegelLevel(Context c, int level) {
        JSONObject k = Vault.of(c).obj("kegel");
        try {
            k.put("level", Math.max(1, Math.min(10, level)));
            k.put("sessions", 0);
            k.put("ease", new JSONArray());
            JSONArray history = k.optJSONArray("history");
            if (history == null) history = new JSONArray();
            history.put(new JSONObject().put("day", Days.today()).put("level", level));
            k.put("history", history);
        } catch (JSONException ignored) {
            // Keys are constant strings.
        }
        Vault.of(c).put("kegel", k);
    }

    public static boolean kegelCanLevelUp(Context c) {
        return Kegel.canLevelUp(kegelLevel(c), kegelSessionsAtLevel(c), kegelEase(c));
    }

    // --------------------------------------------------------------- stats

    public static List<Vault.Event> sessions(Context c) {
        return Vault.of(c).events("session");
    }

    public static int totalXp(Context c) {
        int xp = 0;
        for (Vault.Event e : sessions(c)) xp += e.data.optInt("xp", 10);
        xp += 10 * Vault.of(c).events("checkin").size();
        xp += 5 * Vault.of(c).events("journal").size();
        return xp;
    }

    public static SortedSet<Integer> activeDays(Context c) {
        SortedSet<Integer> days = new TreeSet<Integer>();
        for (Vault.Event e : sessions(c)) days.add(e.day);
        for (Vault.Event e : Vault.of(c).events("checkin")) days.add(e.day);
        return days;
    }

    public static Gamify.Streak streak(Context c) {
        return Gamify.streak(activeDays(c), Days.today());
    }

    /** True if a session with this kind/ref was finished on {@code day}. */
    public static boolean doneOn(Context c, int day, String kind, String ref) {
        for (Vault.Event e : sessions(c)) {
            if (e.day == day && kind.equals(e.data.optString("kind"))
                    && (ref == null || ref.equals(e.data.optString("ref")))) return true;
        }
        if ("checkin".equals(kind)) {
            for (Vault.Event e : Vault.of(c).events("checkin")) if (e.day == day) return true;
        }
        if ("journal".equals(kind)) {
            for (Vault.Event e : Vault.of(c).events("journal")) if (e.day == day) return true;
        }
        if ("practice".equals(kind)) {
            for (Vault.Event e : Vault.of(c).events("practice")) if (e.day == day) return true;
        }
        return false;
    }

    public static int count(Context c, String kind) {
        int n = 0;
        for (Vault.Event e : sessions(c)) if (kind == null || kind.equals(e.data.optString("kind"))) n++;
        return n;
    }

    /** Sessions and minutes for each of the last {@code weeks} Monday-based weeks, oldest first. */
    public static int[][] weekly(Context c, int weeks) {
        int thisWeek = Days.weekOf(Days.today());
        int[][] out = new int[weeks][2];
        for (Vault.Event e : sessions(c)) {
            int i = weeks - 1 - (thisWeek - Days.weekOf(e.day));
            if (i >= 0 && i < weeks) {
                out[i][0]++;
                out[i][1] += e.data.optInt("minutes", 0);
            }
        }
        return out;
    }

    /** Badge id → day earned (or -1 if not yet). Rules are simple counts over the log. */
    public static Map<String, Boolean> badges(Context c) {
        Map<String, Boolean> b = new LinkedHashMap<String, Boolean>();
        Gamify.Streak s = streak(c);
        int total = count(c, null);
        b.put("first_session", total >= 1);
        b.put("first_week", activeDays(c).size() >= 7);
        b.put("streak_7", s.best >= 7);
        b.put("streak_30", s.best >= 30);
        b.put("sessions_25", total >= 25);
        b.put("sessions_100", total >= 100);
        b.put("kegel_5", kegelLevel(c) >= 5);
        b.put("kegel_10", kegelLevel(c) >= 10);
        b.put("breath_10", count(c, "breath") >= 10);
        b.put("audio_10", count(c, "audio") >= 10);
        b.put("lessons_10", count(c, "lesson") >= 10);
        b.put("journal_5", Vault.of(c).events("journal").size() >= 5);
        b.put("checkins_4", Vault.of(c).events("checkin").size() >= 4);
        b.put("workouts_30", count(c, "workout") >= 30);
        b.put("program_complete", programComplete(c));
        return b;
    }

    /** The 8-week plan counts as completed after 8 weeks with at least 40 active days in it. */
    public static boolean programComplete(Context c) {
        int start = Vault.of(c).obj("plan_meta").optInt("start_day", Integer.MAX_VALUE);
        if (Days.today() < start + 55) return false;
        int active = 0;
        for (int d : activeDays(c)) if (d >= start && d < start + 56) active++;
        return active >= 40;
    }
}
