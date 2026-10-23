package com.fswebdev.fit90;

/**
 * Daily pelvic-floor and stamina routine (about 10–15 min plus cardio). Pelvic-floor
 * training and aerobic exercise are the two exercise approaches with clinical evidence
 * for erection quality and ejaculation control. Levels follow the main plan.
 */
public final class Pelvic {

    public static final String PELVIC = "Pelvic floor";
    public static final String BODY = Plan.BODY;
    public static final String CARDIO = "Cardio";
    public static final String BREATH = "Breathing";

    private Pelvic() {
    }

    private static Plan.Exercise ex(String name, String dose, String tag, String cue) {
        return new Plan.Exercise(name, dose, tag, cue);
    }

    public static final String HOW_TO =
            "Finding the muscle: it is the one you would use to stop urine midstream and to hold in gas. "
            + "Try that once to identify it, then train with an empty bladder, never while urinating. "
            + "Keep breathing and keep the buttocks, thighs and abs relaxed — only the pelvic floor works.";

    private static final Plan.Exercise[][] LEVELS = {
            // Level 1 (days 1–30): learn the squeeze, build endurance lying down.
            {
                    ex("Slow Kegels (lying down)", "3 × 10 · hold 3 s, relax 3 s", PELVIC,
                            "Squeeze and lift, as if drawing the base of the penis up and in."),
                    ex("Quick flicks", "2 × 10", PELVIC, "Fast full squeeze, full release each rep."),
                    ex("Glute bridge + Kegel", "3 × 12 · hold 3 s at top", BODY,
                            "Squeeze the pelvic floor as your hips rise, release as you lower."),
                    ex("Butterfly stretch", "2 × 45 s", BODY, "Soles together, knees drop out; breathe slowly."),
                    ex("Belly breathing (reverse Kegel)", "3 min", BREATH,
                            "On each inhale let the pelvic floor drop and soften. This builds control."),
                    ex("Brisk walk", "30 min", CARDIO, "Better blood flow is the base of erection quality."),
            },
            // Level 2 (days 31–60): longer holds, upright positions, single-leg work.
            {
                    ex("Slow Kegels (sitting / standing)", "3 × 12 · hold 6 s, relax 6 s", PELVIC,
                            "Same lift, now against gravity."),
                    ex("Quick flicks", "3 × 15", PELVIC, "Stay crisp; stop when the squeeze gets weak."),
                    ex("Single-leg glute bridge + Kegel", "3 × 10 each leg", BODY,
                            "Hold 3 s at the top with the squeeze on."),
                    ex("Squat + Kegel at the top", "3 × 12", BODY, "Stand up, then squeeze for 2 s."),
                    ex("Deep squat hold + hip-flexor stretch", "2 × 45 s each", BODY,
                            "Tight hips pull on the pelvis; keep them loose."),
                    ex("Belly breathing (reverse Kegel)", "4 min", BREATH, "Slow inhale, pelvic floor softens."),
                    ex("Jog or skipping", "25 min", CARDIO, "Moderate pace you can still talk at."),
            },
            // Level 3 (days 61–90): long holds, control drills and intervals.
            {
                    ex("Long-hold Kegels", "3 × 10 · hold 10 s, relax 10 s", PELVIC,
                            "Full relaxation between reps matters as much as the hold."),
                    ex("Elevator Kegels", "3 × 5", PELVIC,
                            "Squeeze in 3 steps to full, then release in 3 steps. Trains fine control."),
                    ex("Quick flicks", "3 × 20", PELVIC, "Fast and complete."),
                    ex("Feet-elevated glute bridge + Kegel", "3 × 12 · hold 5 s at top", BODY,
                            "Feet on the bed; squeeze glutes and pelvic floor together."),
                    ex("Walking lunges + Kegel", "3 × 10 each leg", BODY,
                            "Hold a light squeeze through every step."),
                    ex("Belly breathing (reverse Kegel)", "5 min", BREATH, "Practise full release on demand."),
                    ex("Interval cardio", "20 min", CARDIO,
                            "1 min fast skipping or running, 1 min easy, repeat."),
            },
    };

    public static Plan.Exercise[] routine(int day) {
        return LEVELS[Plan.level(day) - 1];
    }

    public static final String[] NOTES = {
            "Most men notice better control and firmness after 6–12 weeks of daily practice.",
            "Stop and rest if you feel pain or soreness; overtraining makes the muscle tight, not stronger.",
            "Sleep 7–8 h, avoid smoking and limit alcohol — they affect stamina as much as exercise does.",
            "No exercise increases penis size. Skip jelqing, pumps and stretching devices: they don't work "
                    + "and can cause injury.",
            "If erection or early-ejaculation problems last more than a few weeks, see a doctor or urologist — "
                    + "it is common, treatable, and can be an early sign of a heart or blood-sugar issue.",
    };
}
