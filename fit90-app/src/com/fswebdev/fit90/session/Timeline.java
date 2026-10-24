package com.fswebdev.fit90.session;

import com.fswebdev.fit90.core.Kegel;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * A session as a list of timed steps. Kegel sessions, breathwork and guided audio
 * scripts all compile to this, so one player (and one screen-off service) runs them all.
 */
public final class Timeline {

    public static final String SPEAK = "speak", PAUSE = "pause", SQUEEZE = "squeeze", HOLD = "hold",
            RELEASE = "release", REST = "rest", DROP = "drop", FLICK = "flick", IN = "in", HOLD_IN = "holdin",
            OUT = "out", HOLD_OUT = "holdout", CHIME = "chime";

    public static final class Step {
        public final String kind;
        /** Fixed duration; 0 means "as long as the spoken text takes". */
        public final double seconds;
        public final String text;
        /** Small progress line, e.g. "Set 1 of 2 · 3 of 8". */
        public final String counter;

        public Step(String kind, double seconds, String text, String counter) {
            this.kind = kind;
            this.seconds = seconds;
            this.text = text == null ? "" : text;
            this.counter = counter == null ? "" : counter;
        }

        /** Estimated length when speech decides (about 2.5 words a second). */
        public double estimatedSeconds() {
            if (seconds > 0) return seconds;
            int words = text.trim().isEmpty() ? 0 : text.trim().split("\\s+").length;
            return Math.max(1.5, words / 2.5);
        }
    }

    public final String id;
    public final String kind; // kegel, breath, audio
    public final String title;
    public final String ambient;
    public final List<Step> steps = new ArrayList<Step>();

    public Timeline(String id, String kind, String title, String ambient) {
        this.id = id;
        this.kind = kind;
        this.title = title;
        this.ambient = ambient == null ? "none" : ambient;
    }

    public Timeline add(String kind, double seconds, String text, String counter) {
        steps.add(new Step(kind, seconds, text, counter));
        return this;
    }

    public double totalSeconds() {
        double t = 0;
        for (Step s : steps) t += s.estimatedSeconds();
        return t;
    }

    public int minutes() {
        return (int) Math.max(1, Math.round(totalSeconds() / 60.0));
    }

    // ------------------------------------------------------------------ builders

    /** A pelvic-floor session for one level: long holds, quick flicks, then "drops". */
    public static Timeline kegel(Kegel.Level l) {
        Timeline t = new Timeline("kegel-" + l.number, "kegel", "Level " + l.number + " · " + l.name, "none");
        t.add(SPEAK, 0, "Get comfortable " + positionPhrase(l.position) + ". Breathe normally the whole time. "
                + "Squeeze and lift the pelvic floor, and keep your buttocks, thighs and belly relaxed.", "");
        t.add(PAUSE, 2, "", "");
        for (int s = 1; s <= l.sets; s++) {
            for (int r = 1; r <= l.reps; r++) {
                String counter = String.format(Locale.US, "Set %d of %d · %d of %d", s, l.sets, r, l.reps);
                t.add(SQUEEZE, 1.5, r == 1 ? "Squeeze and lift" : "Squeeze", counter);
                if (l.hold > 1.5) t.add(HOLD, l.hold - 1.5, r == 1 ? "Hold, keep breathing" : "Hold", counter);
                t.add(RELEASE, Math.max(2, l.rest), r == 1 ? "Release fully, let it go" : "Release", counter);
            }
            if (s < l.sets) t.add(REST, l.setRest, "Rest. Breathe slowly.", "Rest between sets");
        }
        if (l.flicks > 0) {
            t.add(SPEAK, 0, "Now quick flicks. Squeeze fast, let go fully each time.", "");
            for (int f = 1; f <= l.flicks; f++) {
                String counter = String.format(Locale.US, "Quick flicks · %d of %d", f, l.flicks);
                t.add(FLICK, 1, "", counter);
                t.add(RELEASE, 1, "", counter);
            }
        }
        if (l.drops > 0) {
            t.add(SPEAK, 0, "Finally, drops. As you breathe in, let the pelvic floor soften and drop. "
                    + "As you breathe out, let it settle. No squeezing.", "");
            for (int d = 1; d <= l.drops; d++) {
                String counter = String.format(Locale.US, "Drops · %d of %d", d, l.drops);
                t.add(DROP, l.dropSeconds, d == 1 ? "Breathe in, let it drop" : "Drop", counter);
                t.add(OUT, l.dropSeconds, d == 1 ? "Breathe out, settle" : "Settle", counter);
            }
        }
        t.add(CHIME, 1, "", "");
        t.add(SPEAK, 0, "Well done. That's the session finished.", "");
        return t;
    }

    private static String positionPhrase(String position) {
        if ("sitting".equals(position)) return "sitting upright";
        if ("standing".equals(position)) return "standing tall";
        if ("moving".equals(position)) return "standing, ready to move";
        return "lying on your back with knees bent";
    }

    /** Breath pacer: in, optional hold, out, optional hold, for about {@code minutes}. */
    public static Timeline breath(String id, String title, double in, double holdIn, double out, double holdOut,
                                  double topUp, int minutes) {
        Timeline t = new Timeline("breath-" + id, "breath", title, "none");
        double cycle = in + holdIn + out + holdOut + topUp;
        int cycles = (int) Math.max(1, Math.round(minutes * 60 / cycle));
        for (int c = 1; c <= cycles; c++) {
            String counter = String.format(Locale.US, "Breath %d of %d", c, cycles);
            t.add(IN, in, c <= 2 ? "Breathe in" : "In", counter);
            if (topUp > 0) t.add(IN, topUp, c <= 2 ? "A little more" : "More", counter);
            if (holdIn > 0) t.add(HOLD_IN, holdIn, c <= 2 ? "Hold" : "", counter);
            t.add(OUT, out, c <= 2 ? "Breathe out slowly" : "Out", counter);
            if (holdOut > 0) t.add(HOLD_OUT, holdOut, c <= 2 ? "Hold" : "", counter);
        }
        t.add(CHIME, 1, "", "");
        return t;
    }

    /**
     * Guided script (markdown with timing markers). Plain lines are spoken and shown
     * as captions; markers: [pause N] [in N] [out N] [hold N] [holdout N] [squeeze N]
     * [release N] [rest N] [drop N] [flick N] [chime] and [repeat N] ... [/repeat].
     * Text after a marker is spoken at the start of that timed step.
     */
    public static Timeline script(String id, String title, String ambient, String body) {
        Timeline t = new Timeline(id, "audio", title, ambient);
        List<String> lines = new ArrayList<String>();
        for (String raw : body.split("\n")) {
            String s = raw.trim();
            if (s.isEmpty() || s.startsWith("#") || s.startsWith("<!--")) continue;
            lines.add(s);
        }
        expand(t, lines, 0, lines.size(), 1);
        return t;
    }

    private static void expand(Timeline t, List<String> lines, int from, int to, int times) {
        for (int rep = 1; rep <= times; rep++) {
            int i = from;
            while (i < to) {
                String s = lines.get(i);
                if (s.startsWith("[repeat")) {
                    int n = (int) number(s, 1);
                    int depth = 1, j = i + 1;
                    while (j < to && depth > 0) {
                        if (lines.get(j).startsWith("[repeat")) depth++;
                        if (lines.get(j).startsWith("[/repeat")) depth--;
                        j++;
                    }
                    expand(t, lines, i + 1, j - 1, (int) n);
                    i = j;
                    continue;
                }
                if (s.startsWith("[")) {
                    int close = s.indexOf(']');
                    String marker = s.substring(1, close).trim();
                    String text = s.substring(close + 1).trim();
                    String kind = marker.split("\\s+")[0];
                    double secs = number(s, "chime".equals(kind) ? 1 : 3);
                    t.add(kind, secs, text, "");
                } else {
                    t.add(SPEAK, 0, s, "");
                }
                i++;
            }
        }
    }

    private static double number(String marker, double fallback) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\[\\S+\\s+(\\d+(\\.\\d+)?)").matcher(marker);
        return m.find() ? Double.parseDouble(m.group(1)) : fallback;
    }

    // --------------------------------------------------------------- serialise

    public String toJson() {
        try {
            JSONObject o = new JSONObject();
            o.put("id", id).put("kind", kind).put("title", title).put("ambient", ambient);
            JSONArray a = new JSONArray();
            for (Step s : steps) {
                a.put(new JSONArray().put(s.kind).put(s.seconds).put(s.text).put(s.counter));
            }
            o.put("steps", a);
            return o.toString();
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
    }

    public static Timeline fromJson(String json) {
        try {
            JSONObject o = new JSONObject(json);
            Timeline t = new Timeline(o.optString("id"), o.optString("kind"), o.optString("title"), o.optString("ambient"));
            JSONArray a = o.getJSONArray("steps");
            for (int i = 0; i < a.length(); i++) {
                JSONArray s = a.getJSONArray(i);
                t.add(s.getString(0), s.getDouble(1), s.getString(2), s.getString(3));
            }
            return t;
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
