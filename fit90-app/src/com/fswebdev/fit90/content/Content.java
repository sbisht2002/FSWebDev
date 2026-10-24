package com.fswebdev.fit90.content;

import android.content.Context;
import android.content.res.AssetManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Reads the app's editable content (markdown + JSON, copied from /content into the APK
 * by tools/build_content.py). Looks in the phone's language first, then English.
 */
public final class Content {

    public static final class Article {
        public final String id, title, summary, category, program, version, lastReviewed, file;
        public final int minutes, order, words;
        public final List<String> tags;

        Article(JSONObject o) {
            id = o.optString("id");
            title = o.optString("title");
            summary = o.optString("summary");
            category = o.optString("category");
            program = o.optString("program");
            version = o.optString("version");
            lastReviewed = o.optString("last_reviewed");
            file = o.optString("file");
            minutes = o.optInt("minutes", 5);
            order = o.optInt("order", 0);
            words = o.optInt("words", 0);
            tags = strings(o.optJSONArray("tags"));
        }
    }

    public static final class Audio {
        public final String id, title, category, summary, ambient, file, version, lastReviewed;
        public final int level, minutes;

        Audio(JSONObject o) {
            id = o.optString("id");
            title = o.optString("title");
            category = o.optString("category");
            summary = o.optString("summary");
            ambient = o.optString("ambient", "none");
            file = o.optString("file");
            version = o.optString("version");
            lastReviewed = o.optString("last_reviewed");
            level = o.optInt("level", 1);
            minutes = o.optInt("minutes", 5);
        }
    }

    private static Content instance;
    private final AssetManager assets;
    private final String lang;
    private final Map<String, Object> cache = new HashMap<String, Object>();
    private List<Article> articles;
    private List<Audio> audio;
    private JSONObject index;

    private Content(Context context) {
        assets = context.getApplicationContext().getAssets();
        String l = Locale.getDefault().getLanguage();
        lang = exists("content/" + l + "/index.json") ? l : "en";
    }

    public static synchronized Content of(Context context) {
        if (instance == null) instance = new Content(context);
        return instance;
    }

    public String language() {
        return lang;
    }

    private boolean exists(String path) {
        try {
            assets.open(path).close();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    /** Raw text of a content file, e.g. "legal/privacy.md". */
    public synchronized String text(String rel) {
        String key = "t:" + rel;
        Object hit = cache.get(key);
        if (hit != null) return (String) hit;
        String path = "content/" + lang + "/" + rel;
        if (!exists(path)) path = "content/en/" + rel;
        String s = read(path);
        cache.put(key, s);
        return s;
    }

    private String read(String path) {
        try {
            InputStream in = assets.open(path);
            try {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
                return out.toString("UTF-8");
            } finally {
                in.close();
            }
        } catch (IOException e) {
            return "";
        }
    }

    public JSONObject json(String rel) {
        try {
            return new JSONObject(text(rel));
        } catch (JSONException e) {
            return new JSONObject();
        }
    }

    public JSONArray jsonArray(String rel) {
        try {
            return new JSONArray(text(rel));
        } catch (JSONException e) {
            return new JSONArray();
        }
    }

    public synchronized JSONObject index() {
        if (index == null) index = json("index.json");
        return index;
    }

    public String version() {
        return index().optString("version", "");
    }

    public synchronized List<Article> articles() {
        if (articles == null) {
            articles = new ArrayList<Article>();
            JSONArray a = index().optJSONArray("articles");
            if (a != null) for (int i = 0; i < a.length(); i++) articles.add(new Article(a.optJSONObject(i)));
        }
        return articles;
    }

    public Article article(String id) {
        for (Article a : articles()) if (a.id.equals(id)) return a;
        return null;
    }

    public List<Article> articlesInProgram(String program) {
        List<Article> out = new ArrayList<Article>();
        for (Article a : articles()) if (program.equals(a.program)) out.add(a);
        java.util.Collections.sort(out, new java.util.Comparator<Article>() {
            @Override
            public int compare(Article x, Article y) {
                return x.order - y.order;
            }
        });
        return out;
    }

    /** Article markdown without its front matter. */
    public String articleBody(String id) {
        Article a = article(id);
        return a == null ? "" : Markdown.stripFrontMatter(text(a.file));
    }

    public synchronized List<Audio> audio() {
        if (audio == null) {
            audio = new ArrayList<Audio>();
            JSONArray a = index().optJSONArray("audio");
            if (a != null) for (int i = 0; i < a.length(); i++) audio.add(new Audio(a.optJSONObject(i)));
        }
        return audio;
    }

    public Audio audio(String id) {
        for (Audio a : audio()) if (a.id.equals(id)) return a;
        return null;
    }

    public String audioScript(String id) {
        Audio a = audio(id);
        return a == null ? "" : text(a.file);
    }

    static List<String> strings(JSONArray a) {
        List<String> out = new ArrayList<String>();
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(a.optString(i));
        return out;
    }

    /** Test hook. */
    public static synchronized void reset() {
        instance = null;
    }
}
