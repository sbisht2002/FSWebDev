package com.fswebdev.fit90.core;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Onboarding answers plus the optional body profile used by the Fitness program.
 * Stored encrypted in the vault under "profile".
 */
public final class Profile {

    // Goals
    public static final String LAST_LONGER = "last_longer";
    public static final String FIRMER = "firmer";
    public static final String LESS_ANXIETY = "less_anxiety";
    public static final String INTIMACY = "intimacy";
    public static final String FITNESS = "fitness";

    // Health check answers
    public static final String DIABETES = "diabetes";
    public static final String HIGH_BP = "high_bp";
    public static final String HEART = "heart";
    public static final String CHEST_PAIN = "chest_pain";
    public static final String PAIN_BLOOD = "pain_blood";
    public static final String BEND_LUMP = "bend_lump";
    public static final String PELVIC_PAIN = "pelvic_pain";
    public static final String URINARY = "urinary";
    public static final String LOW_DESIRE = "low_desire_mood";
    public static final String MEDICINES = "medicines";
    public static final String ANXIETY_LOW_MOOD = "anxiety_low_mood";
    public static final String RELATIONSHIP_STRESS = "relationship_stress";

    public final Set<String> goals = new LinkedHashSet<String>();
    /** lifelong, recent, sudden, unsure, none */
    public String onset = "none";
    /** little, lot */
    public String bother = "little";
    public final Set<String> health = new LinkedHashSet<String>();
    /** partner, dating, single, prefer_not */
    public String partner = "prefer_not";
    /** sitting, light, active, very_active */
    public String activity = "light";
    /** under6, 6_7, 7_9, over9 */
    public String sleep = "7_9";
    /** no, sometimes, daily */
    public String smoking = "no";
    /** none, low, medium, high (drinks a week: 0, 1–7, 8–14, 15+) */
    public String alcohol = "none";
    /** veg, nonveg, egg, vegan */
    public String diet = "veg";
    /** 5, 10, 15 or 20 */
    public int minutes = 10;
    public int reminderMinute = 20 * 60;

    // Optional body profile (Fitness program only).
    public int age = 0;
    public double heightCm = 0;
    public double weightKg = 0;
    public double targetKg = 0;
    /** muscular, lean, maintain */
    public String bodyGoal = "maintain";

    public boolean partnered() {
        return "partner".equals(partner) || "dating".equals(partner);
    }

    public boolean hasBody() {
        return heightCm >= 120 && weightKg >= 35;
    }

    public JSONObject toJson() {
        JSONObject o = new JSONObject();
        try {
            o.put("goals", new JSONArray(goals));
            o.put("onset", onset);
            o.put("bother", bother);
            o.put("health", new JSONArray(health));
            o.put("partner", partner);
            o.put("activity", activity);
            o.put("sleep", sleep);
            o.put("smoking", smoking);
            o.put("alcohol", alcohol);
            o.put("diet", diet);
            o.put("minutes", minutes);
            o.put("reminder_minute", reminderMinute);
            o.put("age", age);
            o.put("height_cm", heightCm);
            o.put("weight_kg", weightKg);
            o.put("target_kg", targetKg);
            o.put("body_goal", bodyGoal);
        } catch (JSONException e) {
            throw new IllegalStateException(e);
        }
        return o;
    }

    public static Profile fromJson(JSONObject o) {
        Profile p = new Profile();
        if (o == null) return p;
        JSONArray g = o.optJSONArray("goals");
        if (g != null) for (int i = 0; i < g.length(); i++) p.goals.add(g.optString(i));
        JSONArray h = o.optJSONArray("health");
        if (h != null) for (int i = 0; i < h.length(); i++) p.health.add(h.optString(i));
        p.onset = o.optString("onset", p.onset);
        p.bother = o.optString("bother", p.bother);
        p.partner = o.optString("partner", p.partner);
        p.activity = o.optString("activity", p.activity);
        p.sleep = o.optString("sleep", p.sleep);
        p.smoking = o.optString("smoking", p.smoking);
        p.alcohol = o.optString("alcohol", p.alcohol);
        p.diet = o.optString("diet", p.diet);
        p.minutes = o.optInt("minutes", p.minutes);
        p.reminderMinute = o.optInt("reminder_minute", p.reminderMinute);
        p.age = o.optInt("age", 0);
        p.heightCm = o.optDouble("height_cm", 0);
        p.weightKg = o.optDouble("weight_kg", 0);
        p.targetKg = o.optDouble("target_kg", 0);
        p.bodyGoal = o.optString("body_goal", p.bodyGoal);
        return p;
    }
}
