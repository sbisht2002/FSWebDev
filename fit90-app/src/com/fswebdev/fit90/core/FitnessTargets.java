package com.fswebdev.fit90.core;

/**
 * Personal targets for the Fitness program from height, weight, age, target weight,
 * activity and body goal. Calorie targets are used only inside the Fitness program.
 *
 * Energy: Mifflin–St Jeor BMR × activity factor. Cutting aims for at most ~0.75% of
 * body weight a week (max 20% deficit, never below BMR); gaining uses a ~10% surplus
 * (~0.25% of body weight a week). Protein 1.6–2.0 g/kg of a reference weight (capped
 * at BMI 25 for heavier people so targets stay realistic).
 */
public final class FitnessTargets {

    public static final String CUT = "cut", GAIN = "gain", MAINTAIN = "maintain";

    public double bmi, bmr, tdee, weeklyKg;
    public int calories, protein, waterMl, weeksToTarget;
    public String mode = MAINTAIN;
    /** Swap jumping moves for low-impact versions (BMI ≥ 30 or ≥ 100 kg). */
    public boolean lowImpact;
    /** Extra sets on the first two (compound) exercises of each workout. */
    public int extraSets;
    /** Add a 10–15 minute cardio finisher after workouts. */
    public boolean finisher;
    /** Rest between sets, seconds. */
    public int restSeconds = 75;

    private FitnessTargets() {
    }

    public static double activityFactor(String activity) {
        if ("sitting".equals(activity)) return 1.2;
        if ("active".equals(activity)) return 1.55;
        if ("very_active".equals(activity)) return 1.725;
        return 1.375;
    }

    public static FitnessTargets compute(Profile p) {
        FitnessTargets t = new FitnessTargets();
        double h = p.heightCm, w = p.weightKg;
        int age = p.age > 0 ? p.age : 25;
        double m = h / 100.0;
        t.bmi = w / (m * m);
        t.bmr = 10 * w + 6.25 * h - 5 * age + 5;
        // The workouts themselves add activity, so even "sitting" users get at least light.
        t.tdee = t.bmr * Math.max(1.375, activityFactor(p.activity));

        double delta = p.targetKg > 0 ? p.targetKg - w : 0;
        if (p.targetKg > 0 && delta <= -1) t.mode = CUT;
        else if (p.targetKg > 0 && delta >= 1) t.mode = GAIN;
        else if (p.targetKg > 0) t.mode = MAINTAIN;
        else if ("lean".equals(p.bodyGoal)) t.mode = CUT;
        else if ("muscular".equals(p.bodyGoal)) t.mode = GAIN;
        else t.mode = MAINTAIN;
        if (t.bmi < 18.5 && CUT.equals(t.mode)) t.mode = MAINTAIN; // never cut when underweight

        double cal;
        if (CUT.equals(t.mode)) {
            t.weeklyKg = Math.min(0.0075 * w, 0.75);
            double deficit = Math.min(0.20 * t.tdee, t.weeklyKg * 7700 / 7);
            cal = Math.max(t.tdee - deficit, Math.max(t.bmr, 1500));
            t.weeklyKg = (t.tdee - cal) * 7 / 7700;
        } else if (GAIN.equals(t.mode)) {
            cal = t.tdee * 1.10;
            t.weeklyKg = 0.0025 * w;
        } else {
            cal = t.tdee;
            t.weeklyKg = 0;
        }
        t.calories = (int) (Math.round(cal / 50.0) * 50);

        double refWeight = t.bmi >= 30 ? 25 * m * m : w;
        double perKg = CUT.equals(t.mode) ? 2.0 : GAIN.equals(t.mode) ? 1.8 : 1.6;
        t.protein = (int) (Math.round(refWeight * perKg / 5.0) * 5);

        int water = (int) (Math.round(w * 35 / 100.0) * 100);
        t.waterMl = Math.max(2000, Math.min(4000, water));

        if (t.weeklyKg > 0 && Math.abs(delta) >= 1) {
            t.weeksToTarget = (int) Math.ceil(Math.abs(delta) / t.weeklyKg);
        }

        t.lowImpact = t.bmi >= 30 || w >= 100;
        if (GAIN.equals(t.mode) || "muscular".equals(p.bodyGoal)) {
            t.extraSets = 1;
            t.restSeconds = 105;
        }
        if (CUT.equals(t.mode) || "lean".equals(p.bodyGoal)) {
            t.finisher = true;
            t.restSeconds = 50;
        }
        return t;
    }

    public String bmiLabel() {
        if (bmi < 18.5) return "Underweight";
        if (bmi < 25) return "Healthy range";
        if (bmi < 30) return "Overweight";
        return "Obese range";
    }
}
