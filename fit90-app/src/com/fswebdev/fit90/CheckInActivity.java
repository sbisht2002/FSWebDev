package com.fswebdev.fit90;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Locale;

/**
 * Weekly lifestyle check-in (sleep, activity, alcohol, smoking) plus simple 0–10
 * self-ratings of control and confidence. Not a clinical questionnaire.
 */
public class CheckInActivity extends BaseActivity {

    private int sleep = 7, activeDays = 3, drinks = 0, smokeDays = 0, control = 5, confidence = 5;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        List<Vault.Event> past = Vault.of(this).events("checkin");
        if (!past.isEmpty()) {
            JSONObject d = past.get(past.size() - 1).data;
            sleep = d.optInt("sleep", sleep);
            activeDays = d.optInt("active_days", activeDays);
            drinks = d.optInt("drinks", drinks);
            smokeDays = d.optInt("smoke_days", smokeDays);
            control = d.optInt("control", control);
            confidence = d.optInt("confidence", confidence);
        }
        render(past);
    }

    private void render(List<Vault.Event> past) {
        screen(getString(R.string.weekly_checkin), true);
        content.addView(ui.text(getString(R.string.checkin_intro), Ui.BODY, p.sub));
        content.addView(ui.space(12));
        content.addView(ui.slider(getString(R.string.ci_sleep), 3, 11, sleep, new Ui.Formatter() {
            @Override
            public String format(int v) {
                return getString(R.string.n_hours, v);
            }
        }, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                sleep = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.ci_active), 0, 7, activeDays, days(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                activeDays = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.ci_drinks), 0, 30, drinks, plain(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                drinks = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.ci_smoke), 0, 7, smokeDays, days(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                smokeDays = v;
            }
        }));
        content.addView(ui.text(getString(R.string.ci_ratings), Ui.HEADING));
        content.addView(ui.space(8));
        content.addView(ui.slider(getString(R.string.ci_control), 0, 10, control, outOf10(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                control = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.ci_confidence), 0, 10, confidence, outOf10(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                confidence = v;
            }
        }));
        if (!past.isEmpty()) {
            LinearLayout hist = ui.card();
            hist.addView(ui.text(getString(R.string.recent_checkins), Ui.SUBHEAD));
            for (int i = Math.max(0, past.size() - 6); i < past.size(); i++) {
                JSONObject d = past.get(i).data;
                hist.addView(ui.text(String.format(Locale.getDefault(), getString(R.string.checkin_row),
                        new java.text.SimpleDateFormat("d MMM", Locale.getDefault()).format(new java.util.Date(past.get(i).ts)),
                        d.optInt("sleep"), d.optInt("active_days"), d.optInt("drinks"), d.optInt("control"),
                        d.optInt("confidence")), Ui.SMALL));
            }
            content.addView(hist);
        }
        footer().addView(ui.button(getString(R.string.save), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject d = new JSONObject();
                try {
                    d.put("sleep", sleep).put("active_days", activeDays).put("drinks", drinks)
                            .put("smoke_days", smokeDays).put("control", control).put("confidence", confidence);
                } catch (JSONException ignored) {
                    // Constant keys.
                }
                Vault.of(CheckInActivity.this).add("checkin", d);
                toast(getString(R.string.saved));
                finish();
            }
        }));
    }

    private Ui.Formatter days() {
        return new Ui.Formatter() {
            @Override
            public String format(int v) {
                return getString(R.string.n_days, v);
            }
        };
    }

    private Ui.Formatter plain() {
        return new Ui.Formatter() {
            @Override
            public String format(int v) {
                return String.valueOf(v);
            }
        };
    }

    private Ui.Formatter outOf10() {
        return new Ui.Formatter() {
            @Override
            public String format(int v) {
                return v + " / 10";
            }
        };
    }
}
