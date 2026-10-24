package com.fswebdev.fit90;

import android.content.Context;
import android.content.Intent;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.core.Kegel;
import com.fswebdev.fit90.core.PlanGenerator;
import com.fswebdev.fit90.core.Profile;
import com.fswebdev.fit90.core.RedFlags;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.session.Timeline;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Glue between the pure plan generator and the app: loading, edits, titles and launching sessions. */
public final class Plans {

    private Plans() {
    }

    public static Profile profile(Context c) {
        return Profile.fromJson(Vault.of(c).obj("profile"));
    }

    public static PlanGenerator.Plan plan(Context c) {
        Content content = Content.of(c);
        Set<String> articles = new HashSet<String>();
        for (Content.Article a : content.articles()) articles.add(a.id);
        Set<String> audio = new HashSet<String>();
        for (Content.Audio a : content.audio()) audio.add(a.id);
        return PlanGenerator.generate(profile(c), content.json("plan.json"), articles, audio);
    }

    public static int startDay(Context c) {
        JSONObject meta = Vault.of(c).obj("plan_meta");
        if (!meta.has("start_day")) {
            try {
                meta.put("start_day", Days.today());
            } catch (JSONException ignored) {
                // Constant key.
            }
            Vault.of(c).put("plan_meta", meta);
        }
        return meta.optInt("start_day", Days.today());
    }

    public static void restart(Context c) {
        JSONObject meta = new JSONObject();
        try {
            meta.put("start_day", Days.today());
        } catch (JSONException ignored) {
            // Constant key.
        }
        Vault.of(c).put("plan_meta", meta);
        Vault.of(c).remove("plan_edits");
    }

    /** Plan day 0..55 for a calendar day. */
    public static int planDay(Context c, int day) {
        return Math.max(0, Math.min(PlanGenerator.WEEKS * 7 - 1, day - startDay(c)));
    }

    public static String editKey(int planDay, PlanGenerator.Session s) {
        return planDay + "|" + s.key();
    }

    public static boolean skipped(Context c, int planDay, PlanGenerator.Session s) {
        return Vault.of(c).obj("plan_edits").optBoolean(editKey(planDay, s), false);
    }

    public static void setSkipped(Context c, int planDay, PlanGenerator.Session s, boolean skip) {
        JSONObject edits = Vault.of(c).obj("plan_edits");
        try {
            if (skip) edits.put(editKey(planDay, s), true);
            else edits.remove(editKey(planDay, s));
        } catch (JSONException ignored) {
            // Key is non-null.
        }
        Vault.of(c).put("plan_edits", edits);
    }

    /** Today's sessions after the user's edits. */
    public static List<PlanGenerator.Session> forDay(Context c, PlanGenerator.Plan plan, int planDay) {
        List<PlanGenerator.Session> out = new ArrayList<PlanGenerator.Session>();
        for (PlanGenerator.Session s : plan.day(planDay)) if (!skipped(c, planDay, s)) out.add(s);
        return out;
    }

    public static String title(Context c, PlanGenerator.Session s) {
        Content content = Content.of(c);
        switch (s.type) {
            case "kegel":
                return "relax".equals(s.ref) ? c.getString(R.string.plan_kegel_relax)
                        : c.getString(R.string.plan_kegel, Progress.kegelLevel(c));
            case "lesson": {
                Content.Article a = content.article(s.ref);
                return a != null ? a.title : s.ref;
            }
            case "audio": {
                Content.Audio a = content.audio(s.ref);
                return a != null ? a.title : s.ref;
            }
            case "breath": {
                JSONObject preset = breathPreset(c, s.ref);
                return c.getString(R.string.plan_breath, preset.optString("name", s.ref));
            }
            case "practice": return c.getString(R.string.plan_practice);
            case "sensate": return c.getString(R.string.plan_sensate, s.ref.replace("stage-", ""));
            case "journal": return c.getString(R.string.plan_journal);
            case "checkin": return c.getString(R.string.plan_checkin);
            case "walk": return c.getString(R.string.plan_walk, s.minutes);
            default: return s.ref;
        }
    }

    public static int icon(String type) {
        switch (type) {
            case "kegel": return R.drawable.ic_kegel;
            case "lesson": return R.drawable.ic_library;
            case "audio": return R.drawable.ic_audio;
            case "breath": return R.drawable.ic_breath;
            case "practice": return R.drawable.ic_timer;
            case "sensate": return R.drawable.ic_people;
            case "journal": return R.drawable.ic_journal;
            case "checkin": return R.drawable.ic_calendar;
            case "walk": return R.drawable.ic_pulse;
            default: return R.drawable.ic_dot;
        }
    }

    public static String tone(String type) {
        switch (type) {
            case "kegel": return "warm";
            case "breath":
            case "audio": return "blue";
            case "lesson": return "gold";
            case "sensate":
            case "journal":
            case "practice": return "purple";
            default: return "accent";
        }
    }

    public static boolean done(Context c, int day, PlanGenerator.Session s) {
        if ("kegel".equals(s.type) || "checkin".equals(s.type) || "journal".equals(s.type)
                || "practice".equals(s.type) || "walk".equals(s.type)) {
            return Progress.doneOn(c, day, s.type, null);
        }
        return Progress.doneOn(c, day, s.type, s.ref);
    }

    public static JSONObject breathPreset(Context c, String id) {
        org.json.JSONArray a = Content.of(c).json("programs/breath.json").optJSONArray("presets");
        if (a != null) {
            for (int i = 0; i < a.length(); i++) {
                JSONObject o = a.optJSONObject(i);
                if (o != null && id.equals(o.optString("id"))) return o;
            }
        }
        return new JSONObject();
    }

    public static Timeline breathTimeline(Context c, String id, int minutes) {
        JSONObject o = breathPreset(c, id);
        return Timeline.breath(id, o.optString("name", "Breathing"), o.optDouble("in", 4), o.optDouble("hold_in", 0),
                o.optDouble("out", 6), o.optDouble("hold_out", 0), o.optDouble("top_up", 0), minutes);
    }

    public static Kegel.Level kegelLevel(Context c, boolean relax) {
        List<Kegel.Level> levels = Kegel.levels(Content.of(c).json("programs/kegel.json"));
        int n = Progress.kegelLevel(c);
        Kegel.Level l = levels.get(Math.max(0, Math.min(levels.size() - 1, n - 1)));
        return relax || RedFlags.painPresent(profile(c)) ? Kegel.relaxed(l) : l;
    }

    /** Opens whatever a plan item needs. */
    public static void launch(Context c, PlanGenerator.Session s) {
        switch (s.type) {
            case "kegel":
                PlayerActivity.start(c, Timeline.kegel(kegelLevel(c, "relax".equals(s.ref))), "kegel", "level");
                break;
            case "breath":
                PlayerActivity.start(c, breathTimeline(c, s.ref, s.minutes), "breath", s.ref);
                break;
            case "audio":
                AudioActivity.play(c, s.ref);
                break;
            case "lesson":
                c.startActivity(new Intent(c, ArticleActivity.class).putExtra(ArticleActivity.EXTRA_ID, s.ref));
                break;
            case "practice":
                c.startActivity(new Intent(c, PracticeLogActivity.class));
                break;
            case "sensate":
                c.startActivity(new Intent(c, ArticleActivity.class).putExtra(ArticleActivity.EXTRA_ID, "ca-07"));
                break;
            case "journal":
                c.startActivity(new Intent(c, JournalActivity.class));
                break;
            case "checkin":
                c.startActivity(new Intent(c, CheckInActivity.class));
                break;
            case "walk":
                Progress.record(c, "walk", "walk", s.minutes, 0, 0, null);
                break;
            default:
        }
    }
}
