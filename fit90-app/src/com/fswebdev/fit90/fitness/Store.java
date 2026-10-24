package com.fswebdev.fit90.fitness;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

/** All app state, kept in SharedPreferences. */
public final class Store {

    public static final int WATER_GOAL_ML = 2000;

    private final SharedPreferences prefs;

    public Store(Context context) {
        prefs = context.getSharedPreferences("fit90", Context.MODE_PRIVATE);
        if (!prefs.contains("start")) {
            prefs.edit().putLong("start", today()).apply();
        }
    }

    /** Days since 1970-01-01 in the phone's local time zone. */
    public static long today() {
        long now = System.currentTimeMillis();
        return (now + TimeZone.getDefault().getOffset(now)) / 86400000L;
    }

    /** Raw plan day (1 on the start date); may exceed 90 once the plan is over. */
    public int rawPlanDay() {
        return (int) (today() - prefs.getLong("start", today())) + 1;
    }

    public int planDay() {
        return Math.max(1, Math.min(Plan.DAYS, rawPlanDay()));
    }

    public void restart() {
        boolean veg = isVeg();
        prefs.edit().clear().putLong("start", today()).putBoolean("veg", veg).apply();
    }

    // ---- Workout and meal marks (bitmask per plan day) ----

    public boolean isWorkoutDone(int day, int index) {
        return (prefs.getInt("w" + day, 0) & (1 << index)) != 0;
    }

    public void setWorkoutDone(int day, int index, boolean done) {
        toggle("w" + day, index, done);
    }

    public int workoutDoneCount(int day) {
        return Integer.bitCount(prefs.getInt("w" + day, 0));
    }

    public boolean isPelvicDone(int day, int index) {
        return (prefs.getInt("k" + day, 0) & (1 << index)) != 0;
    }

    public void setPelvicDone(int day, int index, boolean done) {
        toggle("k" + day, index, done);
    }

    public int pelvicDoneCount(int day) {
        return Integer.bitCount(prefs.getInt("k" + day, 0));
    }

    public boolean isMealDone(int day, int index) {
        return (prefs.getInt("m" + day, 0) & (1 << index)) != 0;
    }

    public void setMealDone(int day, int index, boolean done) {
        toggle("m" + day, index, done);
    }

    private void toggle(String key, int index, boolean on) {
        int mask = prefs.getInt(key, 0);
        mask = on ? mask | (1 << index) : mask & ~(1 << index);
        prefs.edit().putInt(key, mask).apply();
    }

    // ---- Diet preference ----

    public boolean isVeg() {
        return prefs.getBoolean("veg", true);
    }

    public void setVeg(boolean veg) {
        prefs.edit().putBoolean("veg", veg).apply();
    }

    // ---- Water (per calendar date) ----

    /** Today's entries as {minuteOfDay, ml} pairs, oldest first. */
    public List<int[]> waterLog() {
        List<int[]> out = new ArrayList<>();
        String raw = prefs.getString("water" + today(), "");
        for (String entry : raw.split(";")) {
            if (entry.isEmpty()) continue;
            String[] parts = entry.split(":");
            out.add(new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])});
        }
        return out;
    }

    public int waterToday() {
        int total = 0;
        for (int[] e : waterLog()) total += e[1];
        return total;
    }

    public void addWater(int ml) {
        long now = System.currentTimeMillis();
        long local = now + TimeZone.getDefault().getOffset(now);
        int minute = (int) ((local / 60000L) % (24 * 60));
        String key = "water" + today();
        String raw = prefs.getString(key, "");
        prefs.edit().putString(key, raw + minute + ":" + ml + ";").apply();
    }

    public void undoWater() {
        String key = "water" + today();
        String raw = prefs.getString(key, "");
        if (raw.isEmpty()) return;
        int cut = raw.lastIndexOf(';', raw.length() - 2);
        prefs.edit().putString(key, cut < 0 ? "" : raw.substring(0, cut + 1)).apply();
    }
}
