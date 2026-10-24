package com.fswebdev.fit90.core;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** The 10 pelvic-floor levels (from content/en/programs/kegel.json) and the level-up rules. */
public final class Kegel {

    public static final class Level {
        public final int number, sets, reps, hold, rest, flicks, drops, dropSeconds, setRest;
        public final String name, position, focus;

        public Level(JSONObject o) {
            number = o.optInt("level");
            name = o.optString("name");
            position = o.optString("position", "lying");
            focus = o.optString("focus");
            sets = o.optInt("sets", 2);
            reps = o.optInt("reps", 8);
            hold = o.optInt("hold", 3);
            rest = o.optInt("rest", hold);
            flicks = o.optInt("flicks", 0);
            drops = o.optInt("drops", 5);
            dropSeconds = o.optInt("drop_seconds", 5);
            setRest = o.optInt("set_rest", 30);
        }

        /** Rough session length in minutes. */
        public int minutes() {
            int s = sets * reps * (hold + rest) + (sets - 1) * setRest + flicks * 2 + drops * dropSeconds * 2 + 40;
            return Math.max(3, Math.round(s / 60f));
        }
    }

    /** Sessions needed at a level before moving up. */
    public static final int SESSIONS_TO_LEVEL_UP = 5;
    /** Ease ratings: 1 very hard ... 5 very easy. */
    public static final double EASY_ENOUGH = 3.5;
    public static final double TOO_HARD = 1.7;

    private Kegel() {
    }

    public static List<Level> levels(JSONObject config) {
        List<Level> out = new ArrayList<Level>();
        JSONArray a = config.optJSONArray("levels");
        if (a != null) for (int i = 0; i < a.length(); i++) out.add(new Level(a.optJSONObject(i)));
        return out;
    }

    static double averageOfLast(List<Integer> ratings, int n) {
        if (ratings.size() < n) return -1;
        double sum = 0;
        for (int i = ratings.size() - n; i < ratings.size(); i++) sum += ratings.get(i);
        return sum / n;
    }

    /** Enough sessions at this level and the last three felt fairly easy. */
    public static boolean canLevelUp(int level, int sessionsAtLevel, List<Integer> easeAtLevel) {
        if (level >= 10 || sessionsAtLevel < SESSIONS_TO_LEVEL_UP) return false;
        double avg = averageOfLast(easeAtLevel, 3);
        return avg >= EASY_ENOUGH;
    }

    /** The last three sessions at this level all felt very hard: suggest stepping back. */
    public static boolean suggestEasier(int level, List<Integer> easeAtLevel) {
        if (level <= 1) return false;
        double avg = averageOfLast(easeAtLevel, 3);
        return avg >= 0 && avg <= TOO_HARD;
    }

    /**
     * Relaxation-first variant for pain or high anxiety: gentle 2–3 s squeezes, long
     * rests, double the "drop" (reverse Kegel) work, no quick flicks.
     */
    public static Level relaxed(Level l) {
        try {
            JSONObject o = new JSONObject();
            o.put("level", l.number).put("name", l.name + " · relaxation first").put("position", "lying")
                    .put("focus", "Gentle squeezes with long, complete letting go.")
                    .put("sets", Math.min(2, l.sets)).put("reps", Math.min(6, l.reps))
                    .put("hold", Math.min(3, l.hold)).put("rest", Math.max(6, l.rest * 2))
                    .put("flicks", 0).put("drops", l.drops * 2).put("drop_seconds", Math.max(6, l.dropSeconds))
                    .put("set_rest", Math.max(45, l.setRest));
            return new Level(o);
        } catch (org.json.JSONException e) {
            return l;
        }
    }
}
