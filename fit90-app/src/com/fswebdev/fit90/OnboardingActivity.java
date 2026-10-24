package com.fswebdev.fit90;

import android.Manifest;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.content.Markdown;
import com.fswebdev.fit90.core.PlanGenerator;
import com.fswebdev.fit90.core.Profile;
import com.fswebdev.fit90.core.RedFlags;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.fitness.FitnessProfileActivity;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 18+ gate → welcome → privacy promise → quiz (content/en/onboarding.json) → body
 * profile (optional) → "also talk to a doctor" cards → plan preview.
 */
public class OnboardingActivity extends BaseActivity {

    private static final int AGE = 0, WELCOME = 1, PRIVACY = 2, QUIZ = 3, BODY = 4, DOCTOR = 5, PREVIEW = 6, BLOCKED = 7;

    private int stage = AGE;
    private int question = 0;
    private Profile profile = new Profile();
    private JSONArray questions;
    private final List<Integer> history = new ArrayList<Integer>();

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        questions = Content.of(this).json("onboarding.json").optJSONArray("questions");
        if (Vault.of(this).has("profile")) profile = Profile.fromJson(Vault.of(this).obj("profile"));
        if (getIntent().getBooleanExtra("retake", false)) stage = QUIZ;
        render();
    }

    @Override
    public void onBackPressed() {
        if (stage == QUIZ && question > 0) {
            question = history.isEmpty() ? question - 1 : history.remove(history.size() - 1);
            render();
        } else if (stage > AGE && stage != BLOCKED) {
            stage = stage == BODY ? QUIZ : stage - 1;
            render();
        } else {
            super.onBackPressed();
        }
    }

    private void go(int s) {
        stage = s;
        render();
    }

    private void render() {
        screen("", stage > AGE && stage != BLOCKED);
        switch (stage) {
            case AGE: age(); break;
            case WELCOME: welcome(); break;
            case PRIVACY: privacy(); break;
            case QUIZ: quiz(); break;
            case BODY: body(); break;
            case DOCTOR: doctor(); break;
            case PREVIEW: preview(); break;
            default: blocked();
        }
    }

    private void age() {
        content.addView(ui.space(40));
        content.addView(ui.iconBadge(R.drawable.ic_shield, p.accent, p.accentSoft, 64));
        content.addView(ui.space(20));
        content.addView(ui.text(getString(R.string.age_title), Ui.DISPLAY));
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.age_body), Ui.BODY, p.sub));
        content.addView(ui.space(24));
        content.addView(ui.button(getString(R.string.age_yes), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(WELCOME);
            }
        }));
        content.addView(ui.button(getString(R.string.age_no), Ui.OUTLINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(BLOCKED);
            }
        }));
    }

    private void blocked() {
        content.addView(ui.space(40));
        content.addView(ui.text(getString(R.string.blocked_title), Ui.TITLE));
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.blocked_body), Ui.BODY));
    }

    private void welcome() {
        content.addView(ui.space(24));
        content.addView(ui.text(getString(R.string.welcome_title), Ui.DISPLAY));
        content.addView(ui.space(10));
        content.addView(ui.text(getString(R.string.welcome_body), Ui.BODY));
        content.addView(ui.space(16));
        String[] points = getResources().getStringArray(R.array.welcome_points);
        int[] icons = {R.drawable.ic_kegel, R.drawable.ic_breath, R.drawable.ic_library, R.drawable.ic_progress};
        for (int i = 0; i < points.length; i++) {
            LinearLayout row = ui.row();
            row.addView(ui.iconBadge(icons[i % icons.length], p.accent, p.accentSoft, 40));
            TextView t = ui.text(points[i], Ui.BODY);
            t.setPadding(ui.dp(12), 0, 0, 0);
            row.addView(t, ui.weight(1));
            content.addView(row, ui.margins(ui.fill(), 0, 0, 0, 12));
        }
        footer().addView(ui.button(getString(R.string.continue_), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(PRIVACY);
            }
        }));
    }

    private void privacy() {
        content.addView(ui.text(getString(R.string.privacy_promise_title), Ui.DISPLAY));
        content.addView(ui.space(12));
        Markdown.render(ui, getString(R.string.privacy_promise_body), content, null);
        footer().addView(ui.button(getString(R.string.privacy_agree), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                question = 0;
                go(QUIZ);
            }
        }));
    }

    private boolean visible(JSONObject q) {
        JSONArray needs = q.optJSONArray("show_if_goal");
        if (needs == null) return true;
        for (int i = 0; i < needs.length(); i++) if (profile.goals.contains(needs.optString(i))) return true;
        return false;
    }

    private void quiz() {
        while (question < questions.length() && !visible(questions.optJSONObject(question))) question++;
        if (question >= questions.length()) {
            go(BODY);
            return;
        }
        final JSONObject q = questions.optJSONObject(question);
        final String id = q.optString("id");
        String type = q.optString("type", "single");
        content.addView(ui.text(getString(R.string.question_n, question + 1, questions.length()), Ui.OVERLINE));
        content.addView(ui.space(6));
        content.addView(ui.progress((question + 1f) / questions.length(), p.accent, p.accentSoft));
        content.addView(ui.space(16));
        content.addView(ui.text(q.optString("title"), Ui.TITLE));
        if (!q.optString("subtitle").isEmpty()) {
            TextView s = ui.text(q.optString("subtitle"), Ui.SMALL);
            s.setPadding(0, ui.dp(6), 0, 0);
            content.addView(s);
        }
        content.addView(ui.space(16));

        if ("time".equals(type)) {
            final TextView time = ui.text(Reminders.format(profile.reminderMinute), Ui.DISPLAY, p.accent);
            content.addView(time);
            content.addView(ui.button(getString(R.string.change_time), Ui.SECONDARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new TimePickerDialog(OnboardingActivity.this, new TimePickerDialog.OnTimeSetListener() {
                        @Override
                        public void onTimeSet(TimePicker view, int h, int m) {
                            profile.reminderMinute = h * 60 + m;
                            time.setText(Reminders.format(profile.reminderMinute));
                        }
                    }, profile.reminderMinute / 60, profile.reminderMinute % 60,
                            android.text.format.DateFormat.is24HourFormat(OnboardingActivity.this)).show();
                }
            }));
            footer().addView(ui.button(getString(R.string.continue_), Ui.PRIMARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    nextQuestion();
                }
            }));
            return;
        }

        JSONArray options = q.optJSONArray("options");
        final boolean multi = "multi".equals(type);
        for (int i = 0; i < options.length(); i++) {
            final JSONObject o = options.optJSONObject(i);
            final String value = o.optString("id");
            boolean on = multi ? multiSet(id).contains(value) : value.equals(single(id));
            TextView opt = ui.text(o.optString("label"), Ui.SUBHEAD, on ? p.accent : p.text);
            opt.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
            opt.setMinHeight(ui.dp(56));
            opt.setGravity(android.view.Gravity.CENTER_VERTICAL);
            if (on) {
                opt.setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, R.drawable.ic_check, 0);
                if (opt.getCompoundDrawablesRelative()[2] != null) opt.getCompoundDrawablesRelative()[2].setTint(p.accent);
            }
            ui.clickable(opt, on ? p.accentSoft : p.card, 16, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (multi) {
                        Set<String> set = multiSet(id);
                        boolean none = value.equals("none");
                        if (set.contains(value)) set.remove(value);
                        else {
                            if (none) set.clear();
                            else set.remove("none");
                            set.add(value);
                        }
                        render();
                    } else {
                        setSingle(id, value);
                        nextQuestion();
                    }
                }
            }, o.optString("label") + (on ? ", selected" : ""));
            opt.setLayoutParams(ui.margins(ui.fill(), 0, 0, 0, 8));
            content.addView(opt);
        }
        if (multi) {
            final boolean required = q.optBoolean("required", false);
            footer().addView(ui.button(getString(R.string.continue_), Ui.PRIMARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (required && multiSet(id).isEmpty()) {
                        toast(getString(R.string.pick_one));
                        return;
                    }
                    nextQuestion();
                }
            }));
        }
    }

    private void nextQuestion() {
        history.add(question);
        question++;
        render();
    }

    private Set<String> multiSet(String id) {
        return "goals".equals(id) ? profile.goals : profile.health;
    }

    private String single(String id) {
        switch (id) {
            case "onset": return profile.onset;
            case "bother": return profile.bother;
            case "partner": return profile.partner;
            case "activity": return profile.activity;
            case "sleep": return profile.sleep;
            case "smoking": return profile.smoking;
            case "alcohol": return profile.alcohol;
            case "diet": return profile.diet;
            case "minutes": return String.valueOf(profile.minutes);
            default: return "";
        }
    }

    private void setSingle(String id, String v) {
        switch (id) {
            case "onset": profile.onset = v; break;
            case "bother": profile.bother = v; break;
            case "partner": profile.partner = v; break;
            case "activity": profile.activity = v; break;
            case "sleep": profile.sleep = v; break;
            case "smoking": profile.smoking = v; break;
            case "alcohol": profile.alcohol = v; break;
            case "diet": profile.diet = v; break;
            case "minutes": profile.minutes = Integer.parseInt(v); break;
            default:
        }
    }

    private void body() {
        Vault.of(this).put("profile", profile.toJson());
        content.addView(ui.text(getString(R.string.body_title), Ui.DISPLAY));
        content.addView(ui.space(10));
        content.addView(ui.text(getString(R.string.body_body), Ui.BODY));
        content.addView(ui.space(16));
        content.addView(ui.button(getString(R.string.body_set_up), Ui.SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(OnboardingActivity.this, FitnessProfileActivity.class));
            }
        }));
        footer().addView(ui.button(getString(R.string.continue_), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                profile = Profile.fromJson(Vault.of(OnboardingActivity.this).obj("profile"));
                go(RedFlags.evaluate(profile).isEmpty() ? PREVIEW : DOCTOR);
            }
        }));
    }

    private void doctor() {
        content.addView(ui.text(getString(R.string.doctor_title), Ui.DISPLAY));
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.doctor_body), Ui.BODY));
        content.addView(ui.space(16));
        addRedFlagCards(this, ui, content, RedFlags.evaluate(profile));
        footer().addView(ui.button(getString(R.string.continue_), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                go(PREVIEW);
            }
        }));
    }

    /** Renders red-flag cards from content/en/redflags.json (shared with Settings and the coach screen). */
    public static void addRedFlagCards(BaseActivity a, Ui ui, LinearLayout into, List<String> ids) {
        JSONObject cards = Content.of(a).json("redflags.json");
        for (String id : ids) {
            JSONObject c = cards.optJSONObject(id);
            if (c == null) continue;
            boolean urgent = c.optBoolean("urgent");
            LinearLayout n = ui.notice(urgent ? R.drawable.ic_alert : R.drawable.ic_info,
                    ui.p.tone(urgent ? "danger" : "blue"), c.optString("title"), null);
            Markdown.render(ui, c.optString("body"), n, null);
            into.addView(n);
        }
        JSONObject res = cards.optJSONObject("resources");
        if (res != null) {
            LinearLayout r = ui.card();
            r.addView(ui.text(res.optString("title"), Ui.SUBHEAD));
            Markdown.render(ui, res.optString("body"), r, null);
            into.addView(r);
        }
    }

    private void preview() {
        PlanGenerator.Plan plan = Plans.plan(this);
        content.addView(ui.text(getString(R.string.preview_title), Ui.DISPLAY));
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.preview_body, profile.minutes), Ui.BODY));
        content.addView(ui.space(12));
        if (plan.relaxationFirst) {
            content.addView(ui.notice(R.drawable.ic_info, p.tone("blue"), getString(R.string.relax_first_title),
                    getString(R.string.relax_first_body)));
        }
        for (PlanGenerator.Week w : plan.weeks) {
            LinearLayout row = ui.card();
            row.addView(ui.text(getString(R.string.week_n, w.index + 1) + " · " + w.title, Ui.SUBHEAD));
            row.addView(ui.text(w.focus, Ui.SMALL));
            content.addView(row);
        }
        footer().addView(ui.button(getString(R.string.start_plan), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishOnboarding();
            }
        }));
    }

    private void finishOnboarding() {
        Vault.of(this).put("profile", profile.toJson());
        Plans.startDay(this);
        settings.setOnboarded(true);
        settings.setReminderMinute(profile.reminderMinute);
        settings.setReminderSessionMinutes(profile.minutes);
        settings.setRemindersOn(true);
        Reminders.schedule(this);
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            return;
        }
        openHome();
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] results) {
        super.onRequestPermissionsResult(code, perms, results);
        openHome();
    }

    private void openHome() {
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK));
        finish();
    }
}
