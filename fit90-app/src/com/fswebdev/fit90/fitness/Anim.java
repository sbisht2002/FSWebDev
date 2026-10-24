package com.fswebdev.fit90.fitness;

import java.util.Locale;

/**
 * Two-keyframe stick-figure animation for an exercise. A pose is the hip position,
 * the torso angle and where the hands and feet are; elbows and knees are solved
 * with two-bone IK, so planted hands and feet stay put while the body moves.
 *
 * Coordinates live in a 100 × 108 box (x 0–100, y −8–100), y pointing down,
 * the figure facing right. Angles are in degrees: 0 = right, 90 = down.
 */
final class Anim {

    static final int NO_DB = 0, DB_HANDS = 1, DB_NEAR = 2, DB_FEET = 3;
    static final int NO_PULSE = 0, PULSE_PELVIC = 1, PULSE_FAST = 2, PULSE_BREATH = 3;

    /** hipX, hipY, torso°, nearHand xy, farHand xy, nearFoot xy, farFoot xy. */
    final float[] a;
    final float[] b;
    boolean front;
    int db = NO_DB;
    int pulse = NO_PULSE;
    float[][] boxes = new float[0][];
    float[] bar;
    float wall = -1;
    float floor = 92;
    int moveMs = 900;
    int holdMs = 350;

    private Anim(float[] a, float[] b) {
        this.a = a;
        this.b = b;
    }

    private static float[] p(float... v) {
        return v;
    }

    private Anim box(float x, float y, float w, float h) {
        float[][] n = new float[boxes.length + 1][];
        System.arraycopy(boxes, 0, n, 0, boxes.length);
        n[boxes.length] = new float[]{x, y, w, h};
        boxes = n;
        return this;
    }

    private Anim bar(float y) {
        bar = new float[]{28, 72, y};
        floor = 99;
        return this;
    }

    private Anim speed(int move, int hold) {
        moveMs = move;
        holdMs = hold;
        return this;
    }

    private Anim db(int where) {
        db = where;
        return this;
    }

    private Anim pulse(int kind) {
        pulse = kind;
        return this;
    }

    private Anim front() {
        front = true;
        return this;
    }

    private Anim wall(float x) {
        wall = x;
        return this;
    }

    // ---- Shared poses ----

    private static final float[] STAND = p(50, 58, -90, 51, 57, 49, 57, 52, 92, 48, 92);
    private static final float[] PUSH_HIGH = p(53.1f, 78.2f, -24, 77, 92, 79, 92, 22, 92, 24, 92);
    private static final float[] PUSH_LOW = p(55.8f, 88.4f, -6, 77, 92, 79, 92, 22, 92, 24, 92);
    private static final float[] HANG = p(50, 57, -90, 50, 6, 52, 6, 44, 89, 42, 88);
    private static final float[] SQUAT_LOW = p(36, 74, -65, 70, 52, 68, 52, 52, 92, 48, 92);
    private static final float[] LIE_KNEES = p(50, 88, 180, 48, 91, 46, 91, 68, 92, 70, 92);
    private static final float[] BRIDGE_UP = p(48, 74, 152, 48, 91, 46, 91, 68, 92, 70, 92);
    private static final float[] PLANK = p(55.2f, 84.6f, -12.5f, 90, 92, 92, 92, 22, 92, 24, 92);
    private static final float[] HOLLOW = p(55, 88, 166, 8, 76, 8, 77, 88, 80, 88, 81);
    private static final float[] SEATED = p(50, 70, -90, 58, 69, 56, 69, 64, 92, 62, 92);

    /** Picks the animation for an exercise from its name. */
    static Anim forExercise(String name) {
        String n = name.toLowerCase(Locale.US);
        Anim anim = pick(n);
        if (n.contains("kegel") && anim.pulse == NO_PULSE) anim.pulse(PULSE_PELVIC);
        return anim;
    }

    private static Anim pick(String n) {
        // Stamina routine
        if (n.contains("belly breathing")) return new Anim(LIE_KNEES, LIE_KNEES).pulse(PULSE_BREATH).speed(2000, 500);
        if (n.contains("butterfly")) {
            return new Anim(p(50, 78, -90, 46, 79, 54, 79, 49, 89, 51, 89),
                    p(50, 78, -90, 46, 80, 54, 80, 49, 91, 51, 91)).front().speed(700, 200);
        }
        if (n.contains("flick")) return new Anim(LIE_KNEES, LIE_KNEES).pulse(PULSE_FAST);
        if (n.contains("kegel") && !n.contains("bridge") && !n.contains("squat") && !n.contains("lunge")) {
            boolean upright = n.contains("sitting") || n.contains("standing");
            float[] pose = upright ? SEATED : LIE_KNEES;
            Anim k = new Anim(pose, pose).pulse(PULSE_PELVIC).speed(1500, 900);
            return upright ? k.box(40, 72, 20, 20) : k;
        }
        if (n.contains("deep squat")) return new Anim(SQUAT_LOW, p(36, 75, -63, 70, 53, 68, 53, 52, 92, 48, 92)).speed(1500, 500);
        if (n.contains("jog") || n.contains("skipping") && !n.contains("interval")) {
            return new Anim(p(50, 57, -84, 60, 46, 42, 54, 62, 90, 36, 84),
                    p(50, 57, -84, 42, 54, 60, 46, 36, 84, 62, 90)).speed(380, 0);
        }
        if (n.contains("interval") || n.contains("skipping")) {
            return new Anim(p(50, 58, -90, 60, 58, 58, 58, 51, 92, 49, 92),
                    p(50, 53, -90, 60, 53, 58, 53, 51, 87, 49, 87)).speed(260, 0);
        }
        if (n.contains("walk") && !n.contains("lunge")) {
            return new Anim(p(50, 58, -88, 43, 56, 58, 55, 61, 92, 40, 92),
                    p(50, 58, -88, 58, 55, 43, 56, 40, 92, 61, 92)).speed(650, 0);
        }
        if (n.contains("stretch")) {
            return new Anim(p(50, 58, -90, 51, 7, 49, 7, 52, 92, 48, 92),
                    p(46, 59, 68, 58, 92, 60, 92, 52, 92, 48, 92)).speed(1300, 700);
        }
        if (n.contains("sleep")) {
            float[] lie = p(50, 88, 180, 48, 91, 46, 91, 84, 91, 84, 90);
            return new Anim(lie, lie).pulse(PULSE_BREATH).speed(2400, 600);
        }

        // Push
        if (n.contains("incline push")) {
            return new Anim(p(51.4f, 70.6f, -39, 72, 78, 74, 78, 25, 92, 27, 92),
                    p(55.6f, 77.1f, -26, 72, 78, 74, 78, 25, 92, 27, 92)).box(64, 78, 28, 14);
        }
        if (n.contains("knee push")) {
            return new Anim(p(54.1f, 82.5f, -34, 76, 92, 78, 92, 25.3f, 83.5f, 27, 83.5f),
                    p(56.8f, 89.6f, -8, 76, 92, 78, 92, 25.3f, 83.5f, 27, 83.5f));
        }
        if (n.contains("decline push")) {
            return new Anim(p(55.4f, 71.7f, -10.6f, 81, 92, 83, 92, 22, 78, 24, 78),
                    p(55.8f, 82, 6.7f, 81, 92, 83, 92, 22, 78, 24, 78)).box(8, 78, 24, 14);
        }
        if (n.contains("feet-elevated pike")) {
            return new Anim(p(46, 41, 80, 52, 92, 54, 92, 32, 72, 34, 72),
                    p(48, 48, 80, 52, 92, 54, 92, 32, 72, 34, 72)).box(18, 72, 24, 20);
        }
        if (n.contains("pike push")) {
            return new Anim(p(48.4f, 61.2f, 15.6f, 72, 92, 74, 92, 34, 92, 36, 92),
                    p(50, 62, 45, 72, 92, 74, 92, 34, 92, 36, 92));
        }
        if (n.contains("handstand push")) {
            return new Anim(p(61, 41, 90, 60, 92, 62, 92, 67, 8, 65, 8),
                    p(61, 52, 90, 60, 92, 62, 92, 67, 19, 65, 19)).wall(69).speed(1600, 400);
        }
        if (n.contains("handstand")) {
            return new Anim(p(61, 41, 90, 60, 92, 62, 92, 67, 8, 65, 8),
                    p(60.5f, 41.5f, 90, 60, 92, 62, 92, 66.5f, 8.5f, 64.5f, 8.5f)).wall(69).speed(1500, 300);
        }
        if (n.contains("pseudo-planche")) {
            return new Anim(p(53.1f, 78.2f, -24, 66, 92, 68, 92, 22, 92, 24, 92),
                    p(57, 88, -6, 66, 92, 68, 92, 22, 92, 24, 92));
        }
        if (n.contains("clap")) {
            return new Anim(PUSH_LOW, p(55, 74, -26, 82, 76, 84, 76, 22, 92, 24, 92)).speed(450, 200);
        }
        if (n.contains("push-up")) return new Anim(PUSH_HIGH, PUSH_LOW);
        if (n.contains("side plank")) {
            return new Anim(p(55.2f, 84.6f, -12.5f, 90, 92, 80, 56, 22, 92, 24, 92),
                    p(55.2f, 83.6f, -12.5f, 90, 92, 80, 55, 22, 92, 24, 92)).speed(1500, 300);
        }
        if (n.contains("plank")) {
            return new Anim(PLANK, p(55.2f, 83.8f, -12.5f, 90, 92, 92, 92, 22, 92, 24, 92)).speed(1500, 300);
        }
        if (n.contains("dip")) {
            return new Anim(p(52, 67, -90, 47, 66, 49, 66, 70, 92, 72, 92),
                    p(53, 80, -90, 47, 66, 49, 66, 70, 92, 72, 92)).box(28, 66, 20, 26);
        }
        if (n.contains("shoulder press")) {
            return new Anim(p(50, 70, -90, 54, 41, 52, 41, 64, 92, 62, 92),
                    p(50, 70, -90, 51, 20, 49, 20, 64, 92, 62, 92)).box(40, 72, 20, 20).db(DB_HANDS);
        }
        if (n.contains("lateral raise") || n.contains("reverse fly")) {
            return new Anim(p(50, 58, -90, 44, 56, 56, 56, 46, 92, 54, 92),
                    p(50, 58, -90, 27, 35, 73, 35, 46, 92, 54, 92)).front().db(DB_HANDS);
        }

        // Pull
        if (n.contains("dead hang")) return new Anim(HANG, p(50, 57.5f, -90, 50, 6, 52, 6, 44, 89.5f, 42, 88.5f)).bar(6).speed(1500, 300);
        if (n.contains("scapular")) return new Anim(HANG, p(50, 55, -90, 50, 6, 52, 6, 44, 87, 42, 86)).bar(6).speed(700, 300);
        if (n.contains("front lever")) {
            return new Anim(p(24, 32, 0, 50, 6, 52, 6, 37, 39, 39, 39),
                    p(24, 33, 0, 50, 6, 52, 6, 37, 40, 39, 40)).bar(6).speed(1500, 300);
        }
        if (n.contains("toes-to-bar")) return new Anim(HANG, p(61, 54.6f, -115, 50, 6, 52, 6, 54, 10, 52, 10)).bar(6);
        if (n.contains("hanging knee")) return new Anim(HANG, p(50, 57, -90, 50, 6, 52, 6, 66, 72, 64, 72)).bar(6);
        if (n.contains("leg raise") || n.contains("hanging raise")) {
            return new Anim(HANG, p(48, 57, -90, 50, 6, 52, 6, 82, 57, 80, 57)).bar(6);
        }
        if (n.contains("pull-up") || n.contains("chin-up")) {
            Anim pu = new Anim(HANG, p(47, 35, -90, 50, 6, 52, 6, 47, 69, 45, 68)).bar(6).speed(1000, 300);
            return n.contains("weighted") ? pu.db(DB_FEET) : pu;
        }
        if (n.contains("row")) {
            return new Anim(p(44, 60, -20, 68, 76, 59, 70, 50, 92, 47, 92),
                    p(44, 60, -20, 56, 58, 59, 70, 50, 92, 47, 92)).db(DB_NEAR);
        }
        if (n.contains("curl")) {
            return new Anim(p(50, 58, -90, 51, 57, 49, 57, 52, 92, 48, 92),
                    p(50, 58, -90, 56, 36, 54, 36, 52, 92, 48, 92)).db(DB_HANDS);
        }
        if (n.contains("superman")) {
            return new Anim(p(42, 89, 0, 92, 89, 92, 88, 8, 90, 8, 89),
                    p(42, 89, -10, 90, 78, 90, 77, 9, 82, 9, 81)).speed(900, 600);
        }

        // Legs & core
        if (n.contains("pistol")) {
            return new Anim(p(50, 58, -90, 74, 35, 74, 36, 52, 92, 80, 76),
                    p(38, 70, -70, 66, 54, 66, 55, 52, 92, 72, 72)).box(18, 70, 22, 22).speed(1200, 300);
        }
        if (n.contains("bulgarian")) {
            return new Anim(p(48, 62, -90, 49, 61, 47, 61, 60, 92, 22, 74),
                    p(44, 76, -90, 45, 75, 43, 75, 60, 92, 22, 74)).box(8, 74, 22, 18).db(DB_HANDS);
        }
        if (n.contains("single-leg") && n.contains("rdl")) {
            return new Anim(p(50, 58, -90, 51, 57, 49, 57, 50, 92, 48, 92),
                    p(44, 59, -5, 70, 80, 72, 80, 50, 92, 12, 54)).db(DB_HANDS).speed(1200, 400);
        }
        if (n.contains("romanian") || n.contains("rdl")) {
            return new Anim(p(50, 58, -90, 51, 57, 49, 57, 50, 92, 48, 92),
                    p(42, 60, -10, 67, 79, 69, 79, 50, 92, 48, 92)).db(DB_HANDS);
        }
        if (n.contains("jump squat")) {
            return new Anim(SQUAT_LOW, p(50, 50, -90, 53, 48, 47, 48, 51, 84, 49, 84)).speed(420, 150);
        }
        if (n.contains("goblet")) {
            return new Anim(p(50, 58, -90, 55, 43, 54, 43, 52, 92, 48, 92),
                    p(36, 74, -65, 53, 57, 52, 57, 52, 92, 48, 92)).db(DB_NEAR);
        }
        if (n.contains("squat")) return new Anim(STAND, SQUAT_LOW);
        if (n.contains("jump lunge")) {
            return new Anim(p(44, 70, -90, 52, 62, 36, 62, 58, 92, 26, 92),
                    p(44, 70, -90, 36, 62, 52, 62, 26, 92, 58, 92)).speed(550, 150);
        }
        if (n.contains("lunge")) return new Anim(STAND, p(44, 70, -90, 45, 69, 43, 69, 58, 92, 24, 92));
        if (n.contains("single-leg glute bridge")) {
            return new Anim(p(50, 88, 180, 48, 91, 46, 91, 68, 92, 83, 80),
                    p(48, 74, 152, 48, 91, 46, 91, 68, 92, 80, 63));
        }
        if (n.contains("feet-elevated glute bridge")) {
            return new Anim(p(50, 88, 180, 48, 91, 46, 91, 70, 78, 72, 78),
                    p(48, 70, 136, 48, 91, 46, 91, 70, 78, 72, 78)).box(62, 78, 26, 14);
        }
        if (n.contains("bridge")) return new Anim(LIE_KNEES, BRIDGE_UP);
        if (n.contains("calf")) {
            return new Anim(p(50, 52, -90, 51, 51, 49, 51, 52, 86, 48, 86),
                    p(50, 47, -90, 51, 46, 49, 46, 52, 81, 48, 81)).box(36, 86, 30, 6).speed(700, 300);
        }
        if (n.contains("dead bug")) {
            return new Anim(p(55, 88, 180, 29, 63, 31, 63, 72, 71, 70, 71),
                    p(55, 88, 180, 5, 86, 31, 63, 72, 71, 89, 84));
        }
        if (n.contains("hollow")) return new Anim(HOLLOW, p(55, 88, 164, 8, 74, 8, 75, 88, 78, 88, 79)).speed(1500, 300);
        if (n.contains("v-up")) return new Anim(HOLLOW, p(50, 88, -125, 58, 56, 58, 57, 66, 58, 67, 58)).speed(700, 250);
        if (n.contains("russian twist")) {
            return new Anim(p(50, 86, -125, 52, 66, 50, 68, 75, 76, 73, 76),
                    p(50, 86, -125, 41, 72, 40, 73, 75, 76, 73, 76)).db(DB_NEAR).speed(600, 150);
        }
        if (n.contains("l-sit")) {
            return new Anim(p(50, 70, -90, 47, 72, 49, 72, 66, 70, 68, 71),
                    p(50, 69, -90, 47, 72, 49, 72, 66, 69, 68, 70)).box(42, 72, 10, 20).speed(1500, 300);
        }
        return new Anim(STAND, p(50, 58.5f, -90, 51, 57.5f, 49, 57.5f, 52, 92, 48, 92)).speed(1500, 300);
    }
}
