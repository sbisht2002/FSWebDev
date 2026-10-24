package com.fswebdev.fit90.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Decides which "also talk to a doctor" cards an onboarding profile should see.
 * Cards never block the app; the wording lives in content/en/redflags.json.
 */
public final class RedFlags {

    public static final String CHEST_PAIN = "chest_pain";
    public static final String ED_CARDIO = "ed_cardio";
    public static final String ED_RECENT = "ed_recent";
    public static final String PAIN_BLOOD = "pain_blood";
    public static final String PELVIC_PAIN = "pelvic_pain";
    public static final String LIFELONG_PE = "lifelong_pe";
    public static final String LOW_DESIRE = "low_desire";
    public static final String MEDICINES = "medicines";
    public static final String MENTAL_HEALTH = "mental_health";

    private RedFlags() {
    }

    /** Card ids, most urgent first. */
    public static List<String> evaluate(Profile p) {
        List<String> out = new ArrayList<String>();
        boolean erectionGoal = p.goals.contains(Profile.FIRMER);
        boolean recent = "recent".equals(p.onset) || "sudden".equals(p.onset);
        boolean cardio = p.health.contains(Profile.DIABETES) || p.health.contains(Profile.HIGH_BP)
                || p.health.contains(Profile.HEART) || p.health.contains(Profile.CHEST_PAIN);

        if (p.health.contains(Profile.CHEST_PAIN)) out.add(CHEST_PAIN);
        if (erectionGoal && cardio) out.add(ED_CARDIO);
        if (erectionGoal && recent) out.add(ED_RECENT);
        if (p.health.contains(Profile.PAIN_BLOOD) || p.health.contains(Profile.BEND_LUMP)) out.add(PAIN_BLOOD);
        if (p.health.contains(Profile.PELVIC_PAIN) || p.health.contains(Profile.URINARY)) out.add(PELVIC_PAIN);
        if (p.goals.contains(Profile.LAST_LONGER) && "lifelong".equals(p.onset) && "lot".equals(p.bother)) {
            out.add(LIFELONG_PE);
        }
        if (p.health.contains(Profile.LOW_DESIRE)) out.add(LOW_DESIRE);
        if (p.health.contains(Profile.MEDICINES)) out.add(MEDICINES);
        if (p.health.contains(Profile.ANXIETY_LOW_MOOD) || p.health.contains(Profile.RELATIONSHIP_STRESS)) {
            out.add(MENTAL_HEALTH);
        }
        return out;
    }

    /** Pain or high anxiety: start pelvic-floor work with relaxation, not squeezing. */
    public static boolean relaxationFirst(Profile p) {
        return p.health.contains(Profile.PELVIC_PAIN) || p.health.contains(Profile.URINARY)
                || p.health.contains(Profile.ANXIETY_LOW_MOOD);
    }

    /** Pain means the trainer should stay on the relaxation variant until a professional has checked. */
    public static boolean painPresent(Profile p) {
        return p.health.contains(Profile.PELVIC_PAIN) || p.health.contains(Profile.URINARY);
    }
}
