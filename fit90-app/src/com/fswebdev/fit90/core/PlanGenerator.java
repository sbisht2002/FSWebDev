package com.fswebdev.fit90.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Builds the personalised 8-week plan. Deterministic: the same profile, config and
 * available content always give the same plan (no randomness, no clock).
 *
 * Each day has 1–3 short sessions that fit the user's minutes per day, plus an
 * optional walk for erection/fitness goals. Config (lesson tracks, week themes,
 * audio pools) comes from content/en/plan.json.
 */
public final class PlanGenerator {

    public static final int WEEKS = 8;

    public static final class Session {
        public final String type; // kegel, breath, audio, lesson, practice, sensate, journal, checkin, walk
        public final String ref;
        public final int minutes;
        public final boolean optional;

        public Session(String type, String ref, int minutes, boolean optional) {
            this.type = type;
            this.ref = ref;
            this.minutes = minutes;
            this.optional = optional;
        }

        public String key() {
            return type + ":" + ref;
        }

        public JSONObject toJson() {
            JSONObject o = new JSONObject();
            try {
                o.put("type", type).put("ref", ref).put("minutes", minutes).put("optional", optional);
            } catch (JSONException e) {
                throw new IllegalStateException(e);
            }
            return o;
        }

        public static Session fromJson(JSONObject o) {
            return new Session(o.optString("type"), o.optString("ref"), o.optInt("minutes", 5), o.optBoolean("optional"));
        }
    }

    public static final class Week {
        public final int index;
        public final String title, focus;
        public final List<List<Session>> days = new ArrayList<List<Session>>();

        Week(int index, String title, String focus) {
            this.index = index;
            this.title = title;
            this.focus = focus;
        }
    }

    public static final class Plan {
        public final List<Week> weeks = new ArrayList<Week>();
        public final boolean relaxationFirst;

        Plan(boolean relaxationFirst) {
            this.relaxationFirst = relaxationFirst;
        }

        /** Sessions for plan day 0..55 (clamped). */
        public List<Session> day(int planDay) {
            int d = Math.max(0, Math.min(WEEKS * 7 - 1, planDay));
            return weeks.get(d / 7).days.get(d % 7);
        }
    }

    private PlanGenerator() {
    }

    public static int kegelMinutes(int budget) {
        return budget <= 5 ? 4 : budget <= 10 ? 5 : budget <= 15 ? 6 : 8;
    }

    public static int lessonsPerWeek(int budget) {
        return budget <= 5 ? 2 : budget <= 10 ? 3 : budget <= 15 ? 4 : 5;
    }

    private static final int[] LESSON_DAYS = {0, 2, 4, 1, 3};

    public static Plan generate(Profile p, JSONObject config, Collection<String> availableArticles,
                                Collection<String> availableAudio) {
        int budget = p.minutes <= 5 ? 5 : p.minutes <= 10 ? 10 : p.minutes <= 15 ? 15 : 20;
        boolean pain = RedFlags.painPresent(p);
        boolean relax = RedFlags.relaxationFirst(p);
        Plan plan = new Plan(relax);
        List<String> lessons = lessonQueue(p, config, availableArticles);
        JSONObject audio = config.optJSONObject("audio");
        JSONArray themes = config.optJSONArray("week_themes");

        int lessonIndex = 0;
        for (int w = 0; w < WEEKS; w++) {
            JSONObject theme = themes != null ? themes.optJSONObject(w) : null;
            Week week = new Week(w, theme != null ? theme.optString("title") : "Week " + (w + 1),
                    theme != null ? theme.optString("focus") : "");
            Set<Integer> lessonDays = new LinkedHashSet<Integer>();
            int perWeek = lessonsPerWeek(budget);
            for (int i = 0; i < perWeek; i++) lessonDays.add(LESSON_DAYS[i]);

            for (int d = 0; d < 7; d++) {
                int dayIndex = w * 7 + d;
                List<Session> candidates = new ArrayList<Session>();
                String kegelVariant = relax && (pain || w < 2) ? "relax" : "standard";
                Session kegel = new Session("kegel", kegelVariant, kegelMinutes(budget), false);

                if (d == 6) {
                    // Sunday: weekly check-in and something calming instead of training.
                    candidates.add(new Session("checkin", "weekly", 2, false));
                    String calm = pick(audio, budget >= 10 ? "calm" : null, w, dayIndex, availableAudio);
                    candidates.add(calm != null ? new Session("audio", calm, 5, false)
                            : new Session("breath", "slow", 3, false));
                } else {
                    Session lesson = null;
                    if (lessonDays.contains(d) && lessonIndex < lessons.size()) {
                        lesson = new Session("lesson", lessons.get(lessonIndex), 5, false);
                    }
                    if (budget <= 5 && lesson != null) {
                        candidates.add(lesson);
                        candidates.add(kegel);
                    } else {
                        candidates.add(kegel);
                        if (lesson != null) candidates.add(lesson);
                    }
                    Session practice = practiceFor(p, w, d);
                    if (practice != null) candidates.add(practice);
                    if (budget >= 10) {
                        String cat = audioCategory(p, w, d);
                        String a = pick(audio, cat, w, dayIndex, availableAudio);
                        if (a != null && budget >= 15) candidates.add(new Session("audio", a, 5, false));
                        else candidates.add(new Session("breath", BREATH[dayIndex % BREATH.length], 3, false));
                    }
                }

                List<Session> day = new ArrayList<Session>();
                int used = 0;
                for (Session s : candidates) {
                    if (day.size() >= 3) break;
                    if (used + s.minutes > budget && !day.isEmpty()) continue;
                    day.add(s);
                    used += s.minutes;
                    if ("lesson".equals(s.type)) lessonIndex++;
                }
                if (wantsWalk(p) && (d == 0 || d == 2 || d == 4)) {
                    day.add(new Session("walk", "walk", Math.min(40, 15 + 5 * w), true));
                }
                week.days.add(day);
            }
            plan.weeks.add(week);
        }
        return plan;
    }

    private static final String[] BREATH = {"slow", "extended", "box", "sigh"};

    static boolean wantsWalk(Profile p) {
        return p.goals.contains(Profile.FIRMER) || p.goals.contains(Profile.FITNESS)
                || "sitting".equals(p.activity) || "light".equals(p.activity);
    }

    private static Session practiceFor(Profile p, int w, int d) {
        if (p.goals.contains(Profile.LAST_LONGER) && w >= 2 && (d == 3 || (w >= 5 && d == 1))) {
            return new Session("practice", "last-longer", 10, false);
        }
        boolean sensateGoal = p.goals.contains(Profile.INTIMACY) || p.goals.contains(Profile.LESS_ANXIETY)
                || p.goals.contains(Profile.FIRMER);
        if (p.partnered() && sensateGoal && w >= 3 && d == 5) {
            return new Session("sensate", "stage-" + Math.min(4, w - 2), 15, false);
        }
        if (p.goals.contains(Profile.LESS_ANXIETY) && w >= 1 && d == 1) {
            return new Session("journal", "thought", 5, false);
        }
        return null;
    }

    private static String audioCategory(Profile p, int w, int d) {
        if ("under6".equals(p.sleep) && d % 2 == 1) return "sleep";
        if (p.goals.contains(Profile.LESS_ANXIETY)) return d % 3 == 0 ? "confidence" : d % 3 == 1 ? "calm" : "bodyscan";
        if (p.goals.contains(Profile.LAST_LONGER) && w >= 2) return d % 2 == 0 ? "arousal" : "bodyscan";
        if (p.goals.contains(Profile.INTIMACY)) return "calm";
        return d % 2 == 0 ? "bodyscan" : "pelvic";
    }

    /** Deterministic pick from the audio pool for a category, levels rising every two weeks. */
    static String pick(JSONObject audio, String category, int week, int dayIndex, Collection<String> available) {
        if (audio == null || category == null) return null;
        JSONArray pool = audio.optJSONArray(category);
        if (pool == null) return null;
        List<String> ok = new ArrayList<String>();
        for (int i = 0; i < pool.length(); i++) {
            JSONObject a = pool.optJSONObject(i);
            if (a == null) continue;
            if (a.optInt("level", 1) <= 1 + week / 2 && (available == null || available.contains(a.optString("id")))) {
                ok.add(a.optString("id"));
            }
        }
        return ok.isEmpty() ? null : ok.get(dayIndex % ok.size());
    }

    /** Order of lessons: safety first if needed, foundations, flagged lifestyle topics, then goal tracks interleaved. */
    static List<String> lessonQueue(Profile p, JSONObject config, Collection<String> available) {
        LinkedHashSet<String> q = new LinkedHashSet<String>();
        if (!RedFlags.evaluate(p).isEmpty()) q.add(config.optString("safety"));
        JSONArray f = config.optJSONArray("foundation");
        JSONObject life = config.optJSONObject("lifestyle");
        if (f != null && f.length() > 0) q.add(f.optString(0));
        if (life != null) {
            if (!"no".equals(p.smoking)) q.add(life.optString("smoking"));
            if ("medium".equals(p.alcohol) || "high".equals(p.alcohol)) q.add(life.optString("alcohol"));
            if ("under6".equals(p.sleep)) q.add(life.optString("sleep"));
            if ("sitting".equals(p.activity)) q.add(life.optString("sitting"));
        }
        if (f != null) for (int i = 1; i < f.length(); i++) q.add(f.optString(i));

        List<JSONArray> tracks = new ArrayList<JSONArray>();
        JSONObject t = config.optJSONObject("tracks");
        String[] order = {Profile.LAST_LONGER, Profile.FIRMER, Profile.LESS_ANXIETY, Profile.INTIMACY, Profile.FITNESS};
        for (String g : order) {
            if (p.goals.contains(g) && t != null && t.optJSONArray(g) != null) tracks.add(t.optJSONArray(g));
        }
        if (tracks.isEmpty() && t != null && t.optJSONArray(Profile.FITNESS) != null) tracks.add(t.optJSONArray(Profile.FITNESS));
        int longest = 0;
        for (JSONArray a : tracks) longest = Math.max(longest, a.length());
        for (int i = 0; i < longest; i++) {
            for (JSONArray a : tracks) if (i < a.length()) q.add(a.optString(i));
        }
        List<String> out = new ArrayList<String>();
        for (String id : q) {
            if (id != null && !id.isEmpty() && (available == null || available.contains(id))) out.add(id);
        }
        return out;
    }
}
