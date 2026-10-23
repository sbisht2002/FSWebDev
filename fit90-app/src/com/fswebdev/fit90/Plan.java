package com.fswebdev.fit90;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 90-day home calisthenics program: bodyweight, a pull-up bar and two dumbbells. */
public final class Plan {

    public static final int DAYS = 90;

    public static final String BODY = "Bodyweight";
    public static final String BAR = "Pull-up bar";
    public static final String DB = "Dumbbells";

    public static final class Exercise {
        public final String name;
        public final String dose;
        public final String gear;
        public final String cue;

        public Exercise(String name, String dose, String gear, String cue) {
            this.name = name;
            this.dose = dose;
            this.gear = gear;
            this.cue = cue;
        }
    }

    public static final class Workout {
        public final String title;
        public final boolean rest;
        public final Exercise[] exercises;

        Workout(String title, boolean rest, Exercise... exercises) {
            this.title = title;
            this.rest = rest;
            this.exercises = exercises;
        }
    }

    private Plan() {
    }

    private static Exercise ex(String name, String dose, String gear, String cue) {
        return new Exercise(name, dose, gear, cue);
    }

    public static final String WARM_UP =
            "Warm-up (5 min): arm circles, 30 jumping jacks, cat-cow, 10 slow squats, "
            + "10 scapular shrugs on the bar.";
    public static final String COOL_DOWN =
            "Cool-down (5 min): chest, lat, hamstring and hip-flexor stretches, 30 s each.";

    private static final Workout REST = new Workout("Rest & recovery", true,
            ex("Brisk walk", "30 min", BODY, "Easy pace, nose breathing."),
            ex("Full-body stretch", "10 min", BODY, "Hips, hamstrings, chest, shoulders."),
            ex("Sleep", "7–8 h tonight", BODY, "Muscle is built while you recover."));

    // ---- Level 1: Beginner (days 1–30) ----
    private static final Workout L1_PUSH = new Workout("Push · chest, shoulders, triceps", false,
            ex("Incline push-ups", "3 × 12", BODY, "Hands on bed or table, body in one straight line."),
            ex("Knee push-ups", "3 × 10", BODY, "Move to full push-ups once 3 × 15 is easy."),
            ex("Pike push-ups", "3 × 6", BODY, "Hips high, lower your head between your hands."),
            ex("Seated dumbbell shoulder press", "3 × 10", DB, "Brace your core, don't arch the lower back."),
            ex("Chair dips (knees bent)", "3 × 8", BODY, "Elbows point back, shoulders down."),
            ex("Plank", "3 × 30 s", BODY, "Squeeze glutes, ribs down."));

    private static final Workout L1_PULL = new Workout("Pull · back, biceps, grip", false,
            ex("Dead hang", "3 × 20 s", BAR, "Relax and breathe; builds grip and shoulder health."),
            ex("Scapular pull-ups", "3 × 8", BAR, "Arms straight, pull shoulder blades down and back."),
            ex("Negative pull-ups", "3 × 4", BAR, "Jump to the top, lower yourself over 5 seconds."),
            ex("Dumbbell bent-over row", "3 × 10 each arm", DB, "Flat back, pull the elbow to your hip."),
            ex("Dumbbell biceps curl", "3 × 10", DB, "No swinging; lower slowly."),
            ex("Superman hold", "3 × 20 s", BODY, "Lift chest and legs, squeeze the lower back."));

    private static final Workout L1_LEGS = new Workout("Legs & core", false,
            ex("Bodyweight squats", "3 × 15", BODY, "Knees track over toes, chest up."),
            ex("Reverse lunges", "3 × 10 each leg", BODY, "Back knee lightly touches the floor."),
            ex("Dumbbell Romanian deadlift", "3 × 10", DB, "Push hips back, soft knees, flat back."),
            ex("Glute bridge", "3 × 15", BODY, "Pause 1 s at the top."),
            ex("Calf raises on a step", "3 × 15", BODY, "Full stretch at the bottom."),
            ex("Hanging knee raises", "3 × 8", BAR, "No swinging; curl the pelvis up."),
            ex("Dead bug", "3 × 10 each side", BODY, "Lower back stays pressed to the floor."));

    // ---- Level 2: Intermediate (days 31–60) ----
    private static final Workout L2_PUSH = new Workout("Push · chest, shoulders, triceps", false,
            ex("Push-ups", "4 × 12", BODY, "Chest to the floor every rep."),
            ex("Diamond push-ups", "3 × 8", BODY, "Hands together under the chest."),
            ex("Decline push-ups", "3 × 10", BODY, "Feet on the bed, body straight."),
            ex("Feet-elevated pike push-ups", "3 × 8", BODY, "Feet on a chair, hips stacked over shoulders."),
            ex("Chair dips (legs straight)", "4 × 12", BODY, "Go down until upper arms are parallel."),
            ex("Dumbbell lateral raise", "3 × 12", DB, "Lead with the elbows, stop at shoulder height."),
            ex("Hollow body hold", "3 × 30 s", BODY, "Lower back glued to the floor."));

    private static final Workout L2_PULL = new Workout("Pull · back, biceps, grip", false,
            ex("Pull-ups", "4 × 5", BAR, "Chin over the bar, full hang at the bottom."),
            ex("Chin-ups", "3 × 6", BAR, "Palms facing you, elbows drive down."),
            ex("Dumbbell row", "4 × 12 each arm", DB, "Pause 1 s at the top."),
            ex("Dumbbell reverse fly", "3 × 12", DB, "Bent over, squeeze the upper back."),
            ex("Dumbbell hammer curl", "3 × 12", DB, "Neutral grip, slow on the way down."),
            ex("Hanging leg raises (bent knees → straight)", "3 × 8", BAR, "Control the swing."));

    private static final Workout L2_LEGS = new Workout("Legs & core", false,
            ex("Dumbbell goblet squat", "4 × 12", DB, "Hold one dumbbell at the chest, sit deep."),
            ex("Bulgarian split squat", "3 × 10 each leg", DB, "Rear foot on a chair, dumbbells in hands."),
            ex("Dumbbell Romanian deadlift", "4 × 12", DB, "Feel the hamstring stretch."),
            ex("Jump squats", "3 × 10", BODY, "Land softly and go straight into the next rep."),
            ex("Single-leg calf raise", "3 × 12 each leg", BODY, "Hold a dumbbell for extra load."),
            ex("Side plank", "3 × 30 s each side", BODY, "Hips high, body straight."),
            ex("Dumbbell Russian twist", "3 × 15 each side", DB, "Rotate from the ribs, not the arms."));

    // ---- Level 3: Advanced (days 61–90) ----
    private static final Workout L3_PUSH = new Workout("Push · chest, shoulders, triceps", false,
            ex("Clap push-ups", "3 × 6", BODY, "Explode up, land with soft elbows."),
            ex("Archer push-ups", "3 × 6 each side", BODY, "Shift weight to one arm, other arm straight."),
            ex("Pseudo-planche push-ups", "3 × 8", BODY, "Hands by the waist, lean forward."),
            ex("Wall handstand hold", "3 × 30 s", BODY, "Chest to wall, push the floor away."),
            ex("Wall handstand push-up negatives", "3 × 3", BODY, "Lower your head to a pillow over 4 s."),
            ex("Deep dips with pause", "4 × 15", BODY, "Add a dumbbell on your lap when easy."),
            ex("Tuck L-sit", "3 × 15 s", BODY, "On two chairs or the floor, knees to chest."));

    private static final Workout L3_PULL = new Workout("Pull · back, biceps, grip", false,
            ex("Pull-ups", "5 × 8", BAR, "Strict, no kipping."),
            ex("Weighted pull-ups", "3 × 5", BAR, "Hold a dumbbell between your feet."),
            ex("Archer pull-up negatives", "3 × 3 each side", BAR, "Pull to one side, lower slowly."),
            ex("Slow chin-ups", "3 × 8", BAR, "3 s on the way down."),
            ex("Tuck front lever hold", "3 × 10 s", BAR, "Straight arms, back parallel to the floor."),
            ex("Toes-to-bar", "3 × 8", BAR, "Use the lats; feet touch the bar."),
            ex("Slow dumbbell curl", "3 × 12", DB, "3 s lowering."));

    private static final Workout L3_LEGS = new Workout("Legs & core", false,
            ex("Pistol squat to chair", "3 × 6 each leg", BODY, "Sit back to a chair on one leg, stand up."),
            ex("Bulgarian split squat", "4 × 12 each leg", DB, "Dumbbells in hands, slow on the way down."),
            ex("Single-leg dumbbell RDL", "3 × 10 each leg", DB, "Hips square, reach the dumbbell to the shin."),
            ex("Jump lunges", "3 × 10 each leg", BODY, "Switch legs in the air."),
            ex("Tempo goblet squat", "3 × 15", DB, "3 s down, 1 s pause, up fast."),
            ex("Straight-leg hanging raises", "4 × 10", BAR, "Legs to bar height or higher."),
            ex("V-ups", "3 × 15", BODY, "Touch hands to toes at the top."));

    private static final Workout[][] LEVELS = {
            {L1_PUSH, L1_PULL, L1_LEGS},
            {L2_PUSH, L2_PULL, L2_LEGS},
            {L3_PUSH, L3_PULL, L3_LEGS},
    };

    private static final String[] LEVEL_NAMES = {"Beginner", "Intermediate", "Advanced"};

    /** Level 1, 2 or 3 for a plan day (1–90). */
    public static int level(int day) {
        return day <= 30 ? 1 : day <= 60 ? 2 : 3;
    }

    public static String levelName(int day) {
        return LEVEL_NAMES[level(day) - 1];
    }

    /** Week within the current level, 1–5. */
    public static int weekInLevel(int day) {
        return ((day - 1) % 30) / 7 + 1;
    }

    /** Mon–Sat style split: Push, Pull, Legs, Push, Pull, Legs, Rest. */
    public static Workout workout(int day) {
        int slot = (day - 1) % 7;
        if (slot == 6) {
            return REST;
        }
        return LEVELS[level(day) - 1][slot % 3];
    }

    public static String progressionTip(int day) {
        switch (weekInLevel(day)) {
            case 1: return "New level: learn clean form. Targets rise a little every session.";
            case 2: return "Rest 60–90 s between sets so you can hit every target.";
            case 3: return "Slow the lowering phase to 3 seconds on every rep.";
            case 4: return "Push the last set close to failure, form still clean.";
            default: return "Last week of the level: hit every target before moving up.";
        }
    }

    // ---- Automatic progression ----

    private static final Pattern SETS = Pattern.compile("^(\\d+) × (\\d+)( s| min)?(.*)$");
    private static final Pattern MINUTES = Pattern.compile("^(\\d+) min(.*)$");
    private static final Pattern HOLD = Pattern.compile("(hold|relax) (\\d+) s");

    /**
     * How many times this day's workout type (push, pull or legs) has already come up
     * in the current level. Targets grow with it, so every session asks for a bit more.
     */
    public static int sessionIndex(int day) {
        int slot = (day - 1) % 7;
        if (slot == 6) return 0;
        int start = (level(day) - 1) * 30 + 1;
        int count = 0;
        for (int d = start; d < day; d++) {
            int s = (d - 1) % 7;
            if (s != 6 && s % 3 == slot % 3) count++;
        }
        return count;
    }

    /**
     * Today's target for an exercise: {text, increase-since-last-time or null}.
     * {@code session} counts sessions within the level; {@code pace} slows growth
     * (1 for workouts, 3 for the daily stamina routine).
     */
    public static String[] dose(Exercise e, int session, int pace) {
        String now = target(e.dose, session, pace);
        if (session == 0) return new String[]{now, null};
        String before = target(e.dose, session - 1, pace);
        return new String[]{now, now.equals(before) ? null : delta(e.dose, session, pace)};
    }

    private static String target(String dose, int k, int pace) {
        Matcher m = SETS.matcher(dose);
        if (m.matches()) {
            String unit = m.group(3) == null ? "" : m.group(3);
            int v = grow(Integer.parseInt(m.group(2)), unit, k, pace);
            return m.group(1) + " × " + v + unit + holds(m.group(4), k, pace);
        }
        m = MINUTES.matcher(dose);
        if (m.matches()) {
            return grow(Integer.parseInt(m.group(1)), " min", k, pace) + " min" + holds(m.group(2), k, pace);
        }
        return dose;
    }

    private static String delta(String dose, int k, int pace) {
        Matcher m = SETS.matcher(dose);
        String unit;
        int base;
        String rest;
        if (m.matches()) {
            unit = m.group(3) == null ? "" : m.group(3);
            base = Integer.parseInt(m.group(2));
            rest = m.group(4);
        } else {
            m = MINUTES.matcher(dose);
            if (!m.matches()) return null;
            unit = " min";
            base = Integer.parseInt(m.group(1));
            rest = m.group(2);
        }
        int d = grow(base, unit, k, pace) - grow(base, unit, k - 1, pace);
        if (d > 0) {
            if (unit.isEmpty()) return "+" + d + (d == 1 ? " rep" : " reps");
            return "+" + d + unit;
        }
        Matcher h = HOLD.matcher(rest);
        if (h.find()) {
            int hb = Integer.parseInt(h.group(2));
            int hd = growHold(hb, k, pace) - growHold(hb, k - 1, pace);
            if (hd > 0) return "+" + hd + " s hold";
        }
        return null;
    }

    private static int grow(int base, String unit, int k, int pace) {
        int inc;
        int every;
        int cap;
        if (unit.equals(" s")) {
            inc = base >= 20 ? 5 : 2;
            every = 1;
            cap = base * 2;
        } else if (unit.equals(" min")) {
            inc = 1;
            every = 3;
            cap = base + Math.max(2, base / 3);
        } else {
            inc = 1;
            every = base >= 10 ? 1 : 2;
            cap = base * 2;
        }
        return Math.min(cap, base + inc * (k / (every * pace)));
    }

    private static int growHold(int base, int k, int pace) {
        return Math.min(base * 2, base + k / (2 * pace));
    }

    private static String holds(String rest, int k, int pace) {
        Matcher h = HOLD.matcher(rest);
        StringBuffer out = new StringBuffer();
        while (h.find()) {
            h.appendReplacement(out, h.group(1) + " " + growHold(Integer.parseInt(h.group(2)), k, pace) + " s");
        }
        h.appendTail(out);
        return out.toString();
    }
}
