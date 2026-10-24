package com.fswebdev.fit90.fitness;

import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fswebdev.fit90.core.FitnessTargets;
import com.fswebdev.fit90.core.Profile;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import java.util.Locale;

/** Height, weight, age, target weight, body goal and activity for the Fitness program. */
public class FitnessProfileActivity extends BaseActivity {

    private static final String[] GOALS = {"muscular", "lean", "maintain"};
    private static final String[] GOAL_LABELS = {"Muscular", "Lean / shredded", "Maintain"};
    private static final String[] ACTIVITY = {"sitting", "light", "active", "very_active"};
    private static final String[] ACTIVITY_LABELS = {"Mostly sitting", "Light", "Active", "Very active"};

    private Profile profile;
    private EditText age, height, weight, target;
    private TextView preview;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        profile = Profile.fromJson(Vault.of(this).obj("profile"));
        build();
    }

    private void build() {
        screen("Your body profile", true);
        content.addView(ui.text("Used only to set your Fitness targets (calories, protein, water, meal portions "
                + "and training volume). It stays encrypted on this phone.", Ui.SMALL));
        content.addView(ui.space(12));
        age = field("Age (years)", profile.age > 0 ? String.valueOf(profile.age) : "");
        height = field("Height (cm)", num(profile.heightCm));
        weight = field("Weight (kg)", num(profile.weightKg));
        target = field("Target weight (kg, optional)", num(profile.targetKg));

        content.addView(ui.text("Body goal", Ui.SUBHEAD));
        content.addView(ui.space(6));
        content.addView(ui.segmented(GOAL_LABELS, indexOf(GOALS, profile.bodyGoal), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                read();
                profile.bodyGoal = GOALS[v];
                build();
            }
        }));
        content.addView(ui.text("Activity outside workouts", Ui.SUBHEAD));
        content.addView(ui.space(6));
        content.addView(ui.segmented(ACTIVITY_LABELS, indexOf(ACTIVITY, profile.activity), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                read();
                profile.activity = ACTIVITY[v];
                build();
            }
        }));
        preview = ui.text("", Ui.SMALL, p.text);
        LinearLayout card = ui.card(p.accentSoft);
        card.addView(preview);
        content.addView(card);
        updatePreview();
        content.addView(ui.button("Save", Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                read();
                if (!profile.hasBody()) {
                    toast("Please enter your height (cm) and weight (kg).");
                    return;
                }
                Vault.of(FitnessProfileActivity.this).put("profile", profile.toJson());
                finish();
            }
        }));
    }

    private EditText field(String label, String value) {
        content.addView(ui.text(label, Ui.SUBHEAD));
        EditText e = new EditText(this);
        e.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setText(value);
        e.setTextColor(p.text);
        e.setHintTextColor(p.faint);
        e.setHint(label);
        e.setContentDescription(label);
        e.setBackground(ui.outline(p.card, 14, p.line, 1.5f));
        e.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        e.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean has) {
                if (!has) {
                    read();
                    updatePreview();
                }
            }
        });
        e.setLayoutParams(ui.margins(ui.fill(), 0, 6, 0, 14));
        content.addView(e);
        return e;
    }

    private static String num(double v) {
        if (v <= 0) return "";
        return v == Math.floor(v) ? String.valueOf((int) v) : String.valueOf(v);
    }

    private static double parse(EditText e) {
        try {
            return Double.parseDouble(e.getText().toString().trim());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void read() {
        profile.age = (int) parse(age);
        profile.heightCm = parse(height);
        profile.weightKg = parse(weight);
        profile.targetKg = parse(target);
    }

    private void updatePreview() {
        if (!profile.hasBody()) {
            preview.setText("Enter height and weight to see your targets.");
            return;
        }
        FitnessTargets t = FitnessTargets.compute(profile);
        String mode = "cut".equals(t.mode) ? "Lose fat" : "gain".equals(t.mode) ? "Build muscle" : "Maintain";
        StringBuilder sb = new StringBuilder();
        sb.append(String.format(Locale.US, "%s · %d kcal a day · %d g protein · %.1f L water\n", mode,
                t.calories, t.protein, t.waterMl / 1000.0));
        sb.append(String.format(Locale.US, "BMI %.1f (%s). Maintenance is about %d kcal.", t.bmi, t.bmiLabel(),
                Math.round(t.tdee)));
        if (t.weeksToTarget > 0) {
            sb.append(String.format(Locale.US, " At about %.2f kg a week you'd reach %d kg in roughly %d weeks.",
                    t.weeklyKg, Math.round(profile.targetKg), t.weeksToTarget));
        }
        if (t.lowImpact) sb.append(" Jumping moves will be swapped for joint-friendly versions.");
        if (t.extraSets > 0) sb.append(" You'll get an extra set on the main exercises.");
        if (t.finisher) sb.append(" Workouts end with a short cardio finisher.");
        preview.setText(sb.toString());
    }

    private static int indexOf(String[] a, String v) {
        for (int i = 0; i < a.length; i++) if (a[i].equals(v)) return i;
        return 0;
    }
}
