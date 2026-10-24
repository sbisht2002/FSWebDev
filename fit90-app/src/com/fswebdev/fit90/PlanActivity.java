package com.fswebdev.fit90;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.core.PlanGenerator;
import com.fswebdev.fit90.core.Profile;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

/** The 8-week plan as a weekly view, with skip/restore per session and quick edits. */
public class PlanActivity extends BaseActivity {

    private int week = -1;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        screen(getString(R.string.plan_title), true);
        final PlanGenerator.Plan plan = Plans.plan(this);
        int start = Plans.startDay(this);
        int todayPlanDay = Plans.planDay(this, Days.today());
        if (week < 0) week = todayPlanDay / 7;

        CharSequence[] labels = new CharSequence[PlanGenerator.WEEKS];
        for (int i = 0; i < labels.length; i++) labels[i] = String.valueOf(i + 1);
        content.addView(ui.text(getString(R.string.week), Ui.OVERLINE));
        content.addView(ui.space(6));
        content.addView(ui.segmented(labels, week, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                week = v;
                render();
            }
        }));
        PlanGenerator.Week w = plan.weeks.get(week);
        content.addView(ui.text(w.title, Ui.TITLE));
        content.addView(ui.text(w.focus, Ui.SMALL));
        content.addView(ui.space(12));

        String[] dayNames = getResources().getStringArray(R.array.day_names);
        for (int d = 0; d < 7; d++) {
            final int planDay = week * 7 + d;
            int calendarDay = start + planDay;
            LinearLayout day = ui.card(planDay == todayPlanDay ? p.accentSoft : p.card);
            String label = getString(R.string.day_n, planDay + 1) + " · " + dayNames[Days.weekday(calendarDay)]
                    + (planDay == todayPlanDay ? " · " + getString(R.string.tab_today) : "");
            day.addView(ui.text(label, Ui.SUBHEAD));
            for (final PlanGenerator.Session s : plan.day(planDay)) {
                final boolean skipped = Plans.skipped(this, planDay, s);
                boolean done = Plans.done(this, calendarDay, s);
                LinearLayout row = ui.row();
                row.setPadding(0, ui.dp(8), 0, 0);
                row.addView(ui.icon(done ? R.drawable.ic_check : Plans.icon(s.type),
                        done ? p.accent : p.tone(Plans.tone(s.type))[0], 20));
                TextView t = ui.text(Plans.title(this, s) + " · " + getString(R.string.n_min, s.minutes)
                        + (s.optional ? " · " + getString(R.string.optional) : ""), Ui.SMALL, skipped ? p.faint : p.text);
                t.setPadding(ui.dp(10), 0, ui.dp(8), 0);
                if (skipped) t.setPaintFlags(t.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                row.addView(t, ui.weight(1));
                TextView edit = ui.text(getString(skipped ? R.string.restore : R.string.skip), Ui.LABEL, p.accent);
                edit.setPadding(ui.dp(10), ui.dp(10), ui.dp(4), ui.dp(10));
                edit.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Plans.setSkipped(PlanActivity.this, planDay, s, !skipped);
                        render();
                    }
                });
                ui.asButton(edit, getString(skipped ? R.string.restore : R.string.skip) + " " + Plans.title(this, s));
                row.addView(edit);
                day.addView(row);
            }
            content.addView(day);
        }

        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.change_plan), Ui.HEADING));
        content.addView(ui.space(8));
        final Profile profile = Plans.profile(this);
        final int[] mins = {5, 10, 15, 20};
        CharSequence[] minLabels = new CharSequence[mins.length];
        int sel = 1;
        for (int i = 0; i < mins.length; i++) {
            minLabels[i] = getString(R.string.n_min, mins[i]);
            if (mins[i] == profile.minutes) sel = i;
        }
        content.addView(ui.text(getString(R.string.minutes_per_day), Ui.SUBHEAD));
        content.addView(ui.space(6));
        content.addView(ui.segmented(minLabels, sel, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                profile.minutes = mins[v];
                Vault.of(PlanActivity.this).put("profile", profile.toJson());
                settings.setReminderSessionMinutes(mins[v]);
                render();
            }
        }));
        content.addView(ui.button(getString(R.string.retake_quiz), Ui.OUTLINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(PlanActivity.this, OnboardingActivity.class).putExtra("retake", true));
            }
        }));
        content.addView(ui.button(getString(R.string.restart_plan), Ui.GHOST, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(PlanActivity.this).setTitle(R.string.restart_plan)
                        .setMessage(R.string.restart_plan_body)
                        .setNegativeButton(R.string.cancel, null)
                        .setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                Plans.restart(PlanActivity.this);
                                week = 0;
                                render();
                            }
                        }).show();
            }
        }));
    }
}
