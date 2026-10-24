package com.fswebdev.fit90.fitness;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FitnessActivity extends Activity {

    private static final int BG = 0xFFF6F6F3;
    private static final int CARD = 0xFFFFFFFF;
    private static final int TEXT = 0xFF17181A;
    private static final int SUB = 0xFF6E7076;
    private static final int LINE = 0xFFE7E7E2;
    private static final int ACCENT = 0xFF1E8A5A;
    private static final int ACCENT_SOFT = 0xFFDDF0E5;
    private static final int WATER = 0xFF2F7FE6;
    private static final int WATER_SOFT = 0xFFE2EDFC;
    private static final int MISSED = 0xFFF4E1DE;

    private static final String[] TABS = {"Workout", "Stamina", "Diet", "Water", "Progress"};
    private static final int[] WATER_AMOUNTS = {100, 200, 500};

    private Store store;
    private com.fswebdev.fit90.core.FitnessTargets targets;
    private com.fswebdev.fit90.core.Profile profile;
    private int tab;
    private int selectedDay;
    private int lastToday;

    private ScrollView scroll;
    private LinearLayout content;
    private final TextView[] tabViews = new TextView[TABS.length];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new Store(this);
        lastToday = store.planDay();
        selectedDay = lastToday;
        if (savedInstanceState != null) {
            tab = savedInstanceState.getInt("tab");
            selectedDay = savedInstanceState.getInt("day", selectedDay);
        }
        lightSystemBars();
        buildShell();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
        out.putInt("day", selectedDay);
    }

    @Override
    protected void onResume() {
        super.onResume();
        int today = store.planDay();
        profile = com.fswebdev.fit90.core.Profile.fromJson(
                com.fswebdev.fit90.data.Vault.of(this).obj("profile"));
        targets = profile.hasBody() ? com.fswebdev.fit90.core.FitnessTargets.compute(profile) : null;
        if (today != lastToday) {
            // The date rolled over while the app was in the background.
            lastToday = today;
            selectedDay = today;
        }
        render();
    }

    // ------------------------------------------------------------------ shell

    @SuppressWarnings("deprecation")
    private void lightSystemBars() {
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        int flags = 0;
        if (Build.VERSION.SDK_INT >= 23) flags |= 0x00002000; // SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        if (Build.VERSION.SDK_INT >= 27) flags |= 0x00000010; // SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        getWindow().getDecorView().setSystemUiVisibility(flags);
    }

    private void buildShell() {
        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(BG);
        root.setFitsSystemWindows(true);

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        root.addView(column, new FrameLayout.LayoutParams(-1, -1));

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(24));
        scroll.addView(content, new FrameLayout.LayoutParams(-1, -2));
        column.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        View divider = new View(this);
        divider.setBackgroundColor(LINE);
        column.addView(divider, new LinearLayout.LayoutParams(-1, dp(1)));

        LinearLayout nav = new LinearLayout(this);
        nav.setBackgroundColor(CARD);
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            TextView t = new TextView(this);
            t.setText(TABS[i]);
            t.setGravity(Gravity.CENTER);
            t.setTextSize(13);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    tab = index;
                    render();
                    scroll.scrollTo(0, 0);
                }
            });
            tabViews[i] = t;
            nav.addView(t, new LinearLayout.LayoutParams(0, dp(60), 1));
        }
        column.addView(nav, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private void render() {
        for (int i = 0; i < tabViews.length; i++) {
            boolean on = i == tab;
            tabViews[i].setTextColor(on ? ACCENT : SUB);
            tabViews[i].setTypeface(Typeface.create("sans-serif-medium", on ? Typeface.BOLD : Typeface.NORMAL));
            tabViews[i].setCompoundDrawablesWithIntrinsicBounds(null, on ? dot(ACCENT) : dot(Color.TRANSPARENT), null, null);
            tabViews[i].setCompoundDrawablePadding(dp(4));
        }
        content.removeAllViews();
        switch (tab) {
            case 0: renderWorkout(); break;
            case 1: renderStamina(); break;
            case 2: renderDiet(); break;
            case 3: renderWater(); break;
            default: renderProgress(); break;
        }
    }

    // ---------------------------------------------------------------- workout

    private void renderWorkout() {
        final int day = selectedDay;
        Plan.Workout w = Plan.workout(day);

        content.addView(overline("LEVEL " + Plan.level(day) + " · " + Plan.levelName(day).toUpperCase(Locale.US)
                + " · WEEK " + Plan.weekInLevel(day)));
        content.addView(title(w.rest ? "Rest day" : "Workout"));
        content.addView(dayNav());
        if (store.rawPlanDay() > Plan.DAYS) {
            content.addView(note("You finished the 90-day plan. Restart it any time from Progress."));
        }

        int done = store.workoutDoneCount(day);
        int total = w.exercises.length;
        int session = w.rest ? 0 : Plan.sessionIndex(day);
        LinearLayout head = card();
        head.addView(text(w.title, 17, TEXT, true));
        TextView status = text(done == total ? "Day complete ✓" : done + " of " + total + " done", 13,
                done == total ? ACCENT : SUB, false);
        status.setPadding(0, dp(4), 0, dp(10));
        head.addView(status);
        head.addView(bar(done / (float) total, ACCENT, ACCENT_SOFT));
        if (!w.rest) {
            TextView tip = text("Session " + (session + 1) + " of this workout in Level " + Plan.level(day)
                    + ". " + Plan.progressionTip(day), 13, SUB, false);
            tip.setPadding(0, dp(12), 0, 0);
            head.addView(tip);
        }
        content.addView(head);
        content.addView(targetsCard());

        if (!w.rest) content.addView(note(Plan.WARM_UP));

        for (int i = 0; i < w.exercises.length; i++) {
            final int index = i;
            Plan.Exercise e = w.rest ? w.exercises[i] : personalise(w.exercises[i]);
            boolean checked = store.isWorkoutDone(day, i);
            LinearLayout row = checkRow(checked, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (guardFuture(day)) return;
                    store.setWorkoutDone(day, index, !store.isWorkoutDone(day, index));
                    recordIfComplete(day);
                    render();
                }
            });
            fillExercise((LinearLayout) row.getChildAt(1), e, session, 1,
                    !w.rest && targets != null && i < 2 ? targets.extraSets : 0);
            content.addView(row);
        }

        if (!w.rest && targets != null && targets.finisher) {
            content.addView(note("Finisher (10–15 min): brisk walk, stairs or skipping at a pace where talking is hard. "
                    + "Aim for 8,000–10,000 steps a day. Rest " + targets.restSeconds + " s between sets today."));
        } else if (!w.rest && targets != null) {
            content.addView(note("Rest " + targets.restSeconds + " s between sets"
                    + (targets.extraSets > 0 ? "; the first two exercises have an extra set for muscle growth." : ".")));
        }
        if (!w.rest) content.addView(note(Plan.COOL_DOWN));
    }

    private int gearColor(String gear) {
        if (Plan.BAR.equals(gear)) return 0xFF7A4BC2;
        if (Plan.DB.equals(gear)) return 0xFFB5651D;
        if (Pelvic.PELVIC.equals(gear)) return 0xFFC2416B;
        if (Pelvic.CARDIO.equals(gear) || Pelvic.BREATH.equals(gear)) return WATER;
        return ACCENT;
    }

    private int gearSoft(String gear) {
        if (Plan.BAR.equals(gear)) return 0xFFEEE6F8;
        if (Plan.DB.equals(gear)) return 0xFFF8ECDF;
        if (Pelvic.PELVIC.equals(gear)) return 0xFFF9E3EA;
        if (Pelvic.CARDIO.equals(gear) || Pelvic.BREATH.equals(gear)) return WATER_SOFT;
        return ACCENT_SOFT;
    }

    // ---------------------------------------------------------------- stamina

    private void renderStamina() {
        final int day = selectedDay;
        Plan.Exercise[] routine = Pelvic.routine(day);

        content.addView(overline("LEVEL " + Plan.level(day) + " · " + Plan.levelName(day).toUpperCase(Locale.US)
                + " · EVERY DAY"));
        content.addView(title("Stamina"));
        content.addView(dayNav());

        int done = store.pelvicDoneCount(day);
        int total = routine.length;
        int streak = 0;
        int d = store.pelvicDoneCount(store.planDay()) >= total ? store.planDay() : store.planDay() - 1;
        while (d >= 1 && store.pelvicDoneCount(d) >= Pelvic.routine(d).length) {
            streak++;
            d--;
        }
        LinearLayout head = card();
        head.addView(text("Pelvic floor & stamina", 17, TEXT, true));
        TextView status = text((done == total ? "Routine complete ✓" : done + " of " + total + " done")
                + " · " + streak + "-day streak", 13, done == total ? ACCENT : SUB, false);
        status.setPadding(0, dp(4), 0, dp(10));
        head.addView(status);
        head.addView(bar(done / (float) total, ACCENT, ACCENT_SOFT));
        TextView how = text(Pelvic.HOW_TO, 13, SUB, false);
        how.setPadding(0, dp(12), 0, 0);
        how.setLineSpacing(0, 1.15f);
        head.addView(how);
        content.addView(head);

        for (int i = 0; i < routine.length; i++) {
            final int index = i;
            Plan.Exercise e = routine[i];
            LinearLayout row = checkRow(store.isPelvicDone(day, i), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (guardFuture(day)) return;
                    store.setPelvicDone(day, index, !store.isPelvicDone(day, index));
                    render();
                }
            });
            fillExercise((LinearLayout) row.getChildAt(1), e, (day - 1) % 30, 3);
            content.addView(row);
        }

        LinearLayout notes = card();
        notes.addView(text("Good to know", 15, TEXT, true));
        for (String n : Pelvic.NOTES) {
            TextView tv = text("•  " + n, 13, SUB, false);
            tv.setPadding(0, dp(6), 0, 0);
            notes.addView(tv);
        }
        content.addView(notes);
    }

    private void fillExercise(LinearLayout body, Plan.Exercise e, int session, int pace) {
        fillExercise(body, e, session, pace, 0);
    }

    /** A fully ticked workout day counts as a session for streaks, XP and badges (once per plan day). */
    private void recordIfComplete(int day) {
        Plan.Workout w = Plan.workout(day);
        if (store.workoutDoneCount(day) < w.exercises.length) return;
        String ref = "day-" + day;
        for (com.fswebdev.fit90.data.Vault.Event e : com.fswebdev.fit90.data.Progress.sessions(this)) {
            if (ref.equals(e.data.optString("ref"))) return;
        }
        com.fswebdev.fit90.data.Progress.record(this, w.rest ? "walk" : "workout", ref, w.rest ? 30 : 35, 0, 0, null);
    }

    /** Low-impact swaps for heavier bodies (protects knees and ankles). */
    private Plan.Exercise personalise(Plan.Exercise e) {
        if (targets == null || !targets.lowImpact) return e;
        String n = e.name.toLowerCase(Locale.US);
        if (n.contains("jump squat")) return new Plan.Exercise("Squat to calf raise", e.dose, Plan.BODY,
                "Low-impact swap: squat, stand tall, rise onto your toes.");
        if (n.contains("jump lunge")) return new Plan.Exercise("Reverse lunges", e.dose, Plan.BODY,
                "Low-impact swap: step back slowly, alternate legs.");
        if (n.contains("clap")) return new Plan.Exercise("Push-ups", e.dose, Plan.BODY,
                "Low-impact swap: smooth, controlled push-ups.");
        if (n.contains("pistol")) return new Plan.Exercise("Bulgarian split squat", e.dose, Plan.DB,
                "Joint-friendly swap: rear foot on a chair.");
        return e;
    }

    private View targetsCard() {
        LinearLayout c = card();
        if (targets == null) {
            c.addView(text("Personalise your plan", 16, TEXT, true));
            TextView t = text("Add your height, weight, target weight and goal (muscular, lean or maintain) "
                    + "to get your own calorie, protein and water targets, scaled meals and training volume.", 13, SUB, false);
            t.setPadding(0, dp(4), 0, dp(10));
            c.addView(t);
        } else {
            String mode = "cut".equals(targets.mode) ? "Losing fat" : "gain".equals(targets.mode) ? "Building muscle" : "Maintaining";
            c.addView(text(mode + " · " + targets.calories + " kcal · " + targets.protein + " g protein", 16, TEXT, true));
            String eta = targets.weeksToTarget > 0 ? " · about " + targets.weeksToTarget + " weeks to "
                    + Math.round(profile.targetKg) + " kg at a safe pace" : "";
            TextView t = text(String.format(Locale.US, "BMI %.1f (%s) · water %.1f L%s", targets.bmi, targets.bmiLabel(),
                    targets.waterMl / 1000.0, eta), 13, SUB, false);
            t.setPadding(0, dp(4), 0, dp(10));
            c.addView(t);
        }
        TextView b = text(targets == null ? "Set up" : "Edit body profile", 14, ACCENT, true);
        b.setPadding(0, dp(4), 0, dp(4));
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new android.content.Intent(FitnessActivity.this, FitnessProfileActivity.class));
            }
        });
        c.addView(b);
        return c;
    }

    private void fillExercise(LinearLayout body, Plan.Exercise e, int session, int pace, int extraSets) {
        AnimView anim = new AnimView(this, e.name, TEXT, 0xFFB7BAC0, CARD, BG);
        LinearLayout.LayoutParams alp = new LinearLayout.LayoutParams(-1, dp(150));
        alp.bottomMargin = dp(12);
        body.addView(anim, alp);

        String[] dose = Plan.dose(e, session, pace);
        if (extraSets > 0 && dose[0].matches("^\\d+ × .*")) {
            int sp = dose[0].indexOf(' ');
            dose[0] = (Integer.parseInt(dose[0].substring(0, sp)) + extraSets) + dose[0].substring(sp);
        }
        body.addView(text(e.name, 16, TEXT, true));
        LinearLayout meta = new LinearLayout(this);
        meta.setGravity(Gravity.CENTER_VERTICAL);
        meta.setPadding(0, dp(4), 0, dp(4));
        TextView doseView = text(dose[0], 15, TEXT, true);
        meta.addView(doseView, new LinearLayout.LayoutParams(0, -2, 1));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.leftMargin = dp(8);
        meta.addView(chip(e.gear, gearColor(e.gear), gearSoft(e.gear)), lp);
        body.addView(meta);
        if (dose[1] != null) {
            TextView up = text("↑ " + dose[1] + " vs last time", 12, ACCENT, true);
            up.setPadding(0, 0, 0, dp(4));
            body.addView(up);
        }
        body.addView(text(e.cue, 13, SUB, false));
    }

    private boolean guardFuture(int day) {
        if (day > store.rawPlanDay()) {
            Toast.makeText(this, "You can mark this on day " + day, Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------- diet

    private void renderDiet() {
        final int day = selectedDay;
        boolean veg = store.isVeg();
        Diet.Menu menu = Diet.menu(day, veg);
        if (targets != null) menu = Diet.scaled(menu, targets.protein, targets.calories);

        content.addView(overline(targets == null ? "₹100 · 120 G PROTEIN · MAINTAIN 70 KG"
                : "YOUR TARGET · " + targets.protein + " G PROTEIN · " + targets.calories + " KCAL"));
        content.addView(title("Diet"));
        content.addView(dayNav());
        content.addView(segmented(veg));

        LinearLayout summary = card();
        summary.addView(text(menu.name, 17, TEXT, true));
        LinearLayout stats = new LinearLayout(this);
        stats.setPadding(0, dp(12), 0, 0);
        stats.addView(stat(Math.round(menu.protein()) + " g", "protein"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat(String.format(Locale.US, "%,d", Math.round(menu.kcal())), "kcal"),
                new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(stat("₹" + Math.round(menu.rupees()), "per day"), new LinearLayout.LayoutParams(0, -2, 1));
        summary.addView(stats);
        content.addView(summary);

        for (int i = 0; i < menu.meals.length; i++) {
            final int index = i;
            Diet.Meal meal = menu.meals[i];
            LinearLayout row = checkRow(store.isMealDone(day, i), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (guardFuture(day)) return;
                    store.setMealDone(day, index, !store.isMealDone(day, index));
                    render();
                }
            });
            LinearLayout body = (LinearLayout) row.getChildAt(1);
            body.addView(text(meal.time.toUpperCase(Locale.US), 11, SUB, true));
            TextView dish = text(meal.dish, 16, TEXT, true);
            dish.setPadding(0, dp(2), 0, dp(8));
            body.addView(dish);
            for (Diet.Item item : meal.items) {
                LinearLayout line = new LinearLayout(this);
                line.setPadding(0, dp(2), 0, dp(2));
                TextView name = text(item.label, 14, TEXT, false);
                line.addView(name, new LinearLayout.LayoutParams(0, -2, 1));
                line.addView(text(item.amount(), 14, SUB, false));
                TextView p = text(item.protein() >= 0.5 ? Math.round(item.protein()) + " g" : "–", 14, ACCENT, false);
                p.setGravity(Gravity.END);
                line.addView(p, new LinearLayout.LayoutParams(dp(48), -2));
                body.addView(line);
            }
            TextView foot = text(Math.round(meal.protein()) + " g protein · " + Math.round(meal.kcal())
                    + " kcal · ₹" + Math.round(meal.rupees()), 12, SUB, false);
            foot.setPadding(0, dp(8), 0, 0);
            body.addView(foot);
            content.addView(row);
        }

        LinearLayout tips = card();
        tips.addView(text("Tips", 15, TEXT, true));
        for (String t : Diet.TIPS) {
            TextView tv = text("•  " + t, 13, SUB, false);
            tv.setPadding(0, dp(6), 0, 0);
            tips.addView(tv);
        }
        content.addView(tips);

        LinearLayout prices = card();
        prices.addView(text("Prices assumed", 15, TEXT, true));
        TextView pn = text("Typical local market rates. Yours may differ a little by city and season. "
                + "Onion, tomato and spices: ₹" + Math.round(Diet.MASALA_RUPEES) + "/day.", 12, SUB, false);
        pn.setPadding(0, dp(4), 0, dp(6));
        prices.addView(pn);
        for (Diet.Food f : Diet.PRICE_LIST) {
            LinearLayout line = new LinearLayout(this);
            line.setPadding(0, dp(3), 0, dp(3));
            line.addView(text(f.name, 13, TEXT, false), new LinearLayout.LayoutParams(0, -2, 1));
            String price = "pc".equals(f.unit) ? "₹" + Math.round(f.rupees) + " each"
                    : "₹" + Math.round(f.rupees * 10) + ("ml".equals(f.unit) ? "/L" : "/kg");
            line.addView(text(price, 13, SUB, false));
            prices.addView(line);
        }
        content.addView(prices);
        content.addView(note("General guidance, not medical advice. Check with a doctor if you have a "
                + "health condition or a soya, nut or dairy allergy."));
    }

    private View segmented(boolean veg) {
        LinearLayout seg = new LinearLayout(this);
        seg.setBackground(round(LINE, 12));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        String[] labels = {"Veg", "Non-veg"};
        for (int i = 0; i < 2; i++) {
            final boolean wantVeg = i == 0;
            boolean on = wantVeg == veg;
            TextView t = text(labels[i], 14, on ? TEXT : SUB, on);
            t.setGravity(Gravity.CENTER);
            t.setPadding(0, dp(9), 0, dp(9));
            if (on) t.setBackground(round(CARD, 9));
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.setVeg(wantVeg);
                    render();
                }
            });
            seg.addView(t, new LinearLayout.LayoutParams(0, -2, 1));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        seg.setLayoutParams(lp);
        return seg;
    }

    private View stat(String value, String label) {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.addView(text(value, 20, TEXT, true));
        box.addView(text(label, 12, SUB, false));
        return box;
    }

    // ------------------------------------------------------------------ water

    private void renderWater() {
        int total = store.waterToday();
        int goal = targets != null ? targets.waterMl : Store.WATER_GOAL_ML;

        content.addView(overline(new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault())
                .format(new Date()).toUpperCase(Locale.getDefault())));
        content.addView(title("Water"));

        LinearLayout ringCard = card();
        ringCard.setGravity(Gravity.CENTER_HORIZONTAL);
        RingView ring = new RingView(this, WATER_SOFT, WATER, TEXT, SUB);
        ring.set(total / (float) goal, String.format(Locale.US, "%,d ml", total),
                String.format(Locale.US, "of %,d ml", goal));
        ringCard.addView(ring, new LinearLayout.LayoutParams(dp(220), dp(220)));
        String left = total >= goal ? "Goal reached — nice work"
                : String.format(Locale.US, "%,d ml to go (about %d glasses)", goal - total,
                (goal - total + 199) / 200);
        TextView status = text(left, 14, total >= goal ? WATER : SUB, total >= goal);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, dp(12), 0, dp(4));
        ringCard.addView(status);
        content.addView(ringCard);

        LinearLayout buttons = new LinearLayout(this);
        for (int i = 0; i < WATER_AMOUNTS.length; i++) {
            final int ml = WATER_AMOUNTS[i];
            TextView b = text("+" + ml + " ml", 16, WATER, true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(0, dp(16), 0, dp(16));
            b.setBackground(round(WATER_SOFT, 14));
            b.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.addWater(ml);
                    render();
                }
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1);
            if (i > 0) lp.leftMargin = dp(10);
            buttons.addView(b, lp);
        }
        LinearLayout.LayoutParams blp = new LinearLayout.LayoutParams(-1, -2);
        blp.bottomMargin = dp(12);
        content.addView(buttons, blp);

        List<int[]> log = store.waterLog();
        LinearLayout list = card();
        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("Today", 15, TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
        if (!log.isEmpty()) {
            TextView undo = text("Undo last", 14, WATER, true);
            undo.setPadding(dp(8), dp(4), 0, dp(4));
            undo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    store.undoWater();
                    render();
                }
            });
            header.addView(undo);
        }
        list.addView(header);
        if (log.isEmpty()) {
            TextView empty = text("Nothing logged yet. Tap a button above each time you drink.", 13, SUB, false);
            empty.setPadding(0, dp(8), 0, 0);
            list.addView(empty);
        }
        for (int i = log.size() - 1; i >= 0; i--) {
            int[] e = log.get(i);
            LinearLayout line = new LinearLayout(this);
            line.setPadding(0, dp(8), 0, 0);
            line.addView(text(String.format(Locale.US, "%02d:%02d", e[0] / 60, e[0] % 60), 14, SUB, false),
                    new LinearLayout.LayoutParams(0, -2, 1));
            line.addView(text("+" + e[1] + " ml", 14, TEXT, false));
            list.addView(line);
        }
        content.addView(list);
    }

    // --------------------------------------------------------------- progress

    private void renderProgress() {
        final int today = store.planDay();
        content.addView(overline("90-DAY PLAN"));
        content.addView(title("Progress"));

        int completed = 0;
        int workoutsDone = 0;
        int workoutDays = 0;
        int[] perLevel = new int[3];
        for (int d = 1; d <= Plan.DAYS; d++) {
            Plan.Workout w = Plan.workout(d);
            boolean complete = isComplete(d);
            if (!w.rest) workoutDays++;
            if (complete) {
                completed++;
                perLevel[Plan.level(d) - 1]++;
                if (!w.rest) workoutsDone++;
            }
        }
        int streak = 0;
        int d = isComplete(today) ? today : today - 1;
        while (d >= 1 && isComplete(d)) {
            streak++;
            d--;
        }

        LinearLayout stats = card();
        LinearLayout row = new LinearLayout(this);
        row.addView(stat("Day " + today, "of " + Plan.DAYS), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(stat(workoutsDone + "/" + workoutDays, "workouts"), new LinearLayout.LayoutParams(0, -2, 1));
        row.addView(stat(streak + (streak == 1 ? " day" : " days"), "streak"), new LinearLayout.LayoutParams(0, -2, 1));
        stats.addView(row);
        TextView pct = text(Math.round(completed * 100f / Plan.DAYS) + "% of the plan complete", 13, SUB, false);
        pct.setPadding(0, dp(14), 0, dp(8));
        stats.addView(pct);
        stats.addView(bar(completed / (float) Plan.DAYS, ACCENT, ACCENT_SOFT));
        content.addView(stats);

        LinearLayout levels = card();
        String[] names = {"Level 1 · Beginner", "Level 2 · Intermediate", "Level 3 · Advanced"};
        String[] ranges = {"Days 1–30", "Days 31–60", "Days 61–90"};
        for (int i = 0; i < 3; i++) {
            LinearLayout line = new LinearLayout(this);
            line.setGravity(Gravity.CENTER_VERTICAL);
            line.setPadding(0, i == 0 ? 0 : dp(12), 0, dp(6));
            boolean current = Plan.level(today) == i + 1;
            line.addView(text(names[i], 15, current ? ACCENT : TEXT, true), new LinearLayout.LayoutParams(0, -2, 1));
            line.addView(text(ranges[i] + " · " + perLevel[i] + "/30", 13, SUB, false));
            levels.addView(line);
            levels.addView(bar(perLevel[i] / 30f, ACCENT, ACCENT_SOFT));
        }
        content.addView(levels);

        LinearLayout grid = card();
        grid.addView(text("All 90 days", 15, TEXT, true));
        TextView hint = text("Tap a day to open it.", 12, SUB, false);
        hint.setPadding(0, dp(2), 0, dp(10));
        grid.addView(hint);
        for (int r = 0; r < 9; r++) {
            LinearLayout line = new LinearLayout(this);
            for (int c = 0; c < 10; c++) {
                final int day = r * 10 + c + 1;
                TextView cell = text(String.valueOf(day), 11, SUB, false);
                cell.setGravity(Gravity.CENTER);
                int done = store.workoutDoneCount(day);
                if (isComplete(day)) {
                    cell.setBackground(round(ACCENT, 8));
                    cell.setTextColor(Color.WHITE);
                } else if (done > 0) {
                    cell.setBackground(round(ACCENT_SOFT, 8));
                    cell.setTextColor(ACCENT);
                } else if (day < today) {
                    cell.setBackground(round(MISSED, 8));
                } else {
                    cell.setBackground(round(BG, 8));
                }
                if (day == today) {
                    GradientDrawable g = (GradientDrawable) cell.getBackground();
                    g.setStroke(dp(2), TEXT);
                    cell.setTypeface(Typeface.DEFAULT_BOLD);
                }
                cell.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        selectedDay = day;
                        tab = 0;
                        render();
                        scroll.scrollTo(0, 0);
                    }
                });
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(30), 1);
                lp.setMargins(dp(2), dp(2), dp(2), dp(2));
                line.addView(cell, lp);
            }
            grid.addView(line);
        }
        LinearLayout legend = new LinearLayout(this);
        legend.setGravity(Gravity.CENTER_VERTICAL);
        legend.setPadding(0, dp(12), 0, 0);
        legend.addView(legendItem(ACCENT, "Done"));
        legend.addView(legendItem(ACCENT_SOFT, "Partly"));
        legend.addView(legendItem(MISSED, "Missed"));
        grid.addView(legend);
        content.addView(grid);

        TextView restart = text("Restart plan from day 1", 14, 0xFFC0392B, true);
        restart.setGravity(Gravity.CENTER);
        restart.setPadding(0, dp(14), 0, dp(14));
        restart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(FitnessActivity.this)
                        .setTitle("Restart plan?")
                        .setMessage("Today becomes day 1 and all workout and meal marks are cleared.")
                        .setNegativeButton("Cancel", null)
                        .setPositiveButton("Restart", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                store.restart();
                                lastToday = store.planDay();
                                selectedDay = lastToday;
                                render();
                            }
                        })
                        .show();
            }
        });
        content.addView(restart);
    }

    private boolean isComplete(int day) {
        return store.workoutDoneCount(day) >= Plan.workout(day).exercises.length;
    }

    private View legendItem(int color, String label) {
        LinearLayout item = new LinearLayout(this);
        item.setGravity(Gravity.CENTER_VERTICAL);
        item.setPadding(0, 0, dp(16), 0);
        View sw = new View(this);
        sw.setBackground(round(color, 4));
        item.addView(sw, new LinearLayout.LayoutParams(dp(12), dp(12)));
        TextView t = text(label, 12, SUB, false);
        t.setPadding(dp(6), 0, 0, 0);
        item.addView(t);
        return item;
    }

    // ---------------------------------------------------------------- widgets

    private View dayNav() {
        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER_VERTICAL);
        nav.setBackground(round(CARD, 14));
        TextView prev = navArrow("‹", selectedDay > 1, -1);
        TextView next = navArrow("›", selectedDay < Plan.DAYS, 1);
        LinearLayout mid = new LinearLayout(this);
        mid.setOrientation(LinearLayout.VERTICAL);
        mid.setGravity(Gravity.CENTER);
        mid.addView(text("Day " + selectedDay + " of " + Plan.DAYS, 16, TEXT, true));
        int today = store.planDay();
        String sub = selectedDay == today ? "Today" : "Go to today";
        TextView subView = text(sub, 12, selectedDay == today ? SUB : ACCENT, selectedDay != today);
        mid.addView(subView);
        if (selectedDay != today) {
            mid.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDay = store.planDay();
                    render();
                }
            });
        }
        nav.addView(prev, new LinearLayout.LayoutParams(dp(56), dp(56)));
        nav.addView(mid, new LinearLayout.LayoutParams(0, -2, 1));
        nav.addView(next, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        nav.setLayoutParams(lp);
        return nav;
    }

    private TextView navArrow(String glyph, boolean enabled, final int delta) {
        TextView t = text(glyph, 28, enabled ? TEXT : LINE, false);
        t.setGravity(Gravity.CENTER);
        if (enabled) {
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectedDay += delta;
                    render();
                }
            });
        }
        return t;
    }

    /** A card with a round check mark on the left and an empty body column on the right. */
    private LinearLayout checkRow(boolean checked, View.OnClickListener onClick) {
        LinearLayout row = card();
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setOnClickListener(onClick);

        TextView check = new TextView(this);
        check.setGravity(Gravity.CENTER);
        check.setTextSize(14);
        check.setTypeface(Typeface.DEFAULT_BOLD);
        GradientDrawable circle = new GradientDrawable();
        circle.setShape(GradientDrawable.OVAL);
        if (checked) {
            circle.setColor(ACCENT);
            check.setText("✓");
            check.setTextColor(Color.WHITE);
        } else {
            circle.setColor(CARD);
            circle.setStroke(dp(2), LINE);
        }
        check.setBackground(circle);
        LinearLayout.LayoutParams clp = new LinearLayout.LayoutParams(dp(26), dp(26));
        clp.rightMargin = dp(14);
        clp.topMargin = dp(1);
        row.addView(check, clp);

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        if (checked) body.setAlpha(0.55f);
        row.addView(body, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(round(CARD, 16));
        card.setPadding(dp(16), dp(16), dp(16), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = dp(12);
        card.setLayoutParams(lp);
        return card;
    }

    private View bar(float fraction, int color, int trackColor) {
        FrameLayout track = new FrameLayout(this);
        track.setBackground(round(trackColor, 3));
        track.setLayoutParams(new LinearLayout.LayoutParams(-1, dp(6)));
        View fill = new View(this);
        fill.setBackground(round(color, 3));
        LinearLayout weights = new LinearLayout(this);
        float f = Math.max(0f, Math.min(1f, fraction));
        weights.addView(fill, new LinearLayout.LayoutParams(0, -1, f));
        weights.addView(new View(this), new LinearLayout.LayoutParams(0, -1, 1 - f));
        track.addView(weights, new FrameLayout.LayoutParams(-1, -1));
        return track;
    }

    private TextView chip(String label, int fg, int bg) {
        TextView t = text(label, 11, fg, true);
        t.setBackground(round(bg, 10));
        t.setPadding(dp(8), dp(2), dp(8), dp(2));
        return t;
    }

    private TextView overline(String s) {
        TextView t = text(s, 12, SUB, true);
        t.setLetterSpacing(0.06f);
        t.setSingleLine(true);
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private TextView title(String s) {
        TextView t = text(s, 30, TEXT, true);
        t.setPadding(0, dp(2), 0, dp(14));
        return t;
    }

    private TextView note(String s) {
        TextView t = text(s, 13, SUB, false);
        t.setPadding(dp(4), dp(2), dp(4), dp(14));
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        t.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        return t;
    }

    private GradientDrawable round(int color, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private GradientDrawable dot(int color) {
        GradientDrawable g = round(color, 3);
        g.setSize(dp(18), dp(3));
        return g;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
