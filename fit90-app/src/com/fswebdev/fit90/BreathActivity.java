package com.fswebdev.fit90;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.session.Timeline;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

/** Breathwork presets plus custom timings; everything runs offline. */
public class BreathActivity extends BaseActivity {

    private static final int[] MINUTES = {1, 3, 5, 10};
    private int minutesIndex = 1;
    private int in = 4, holdIn = 0, out = 6, holdOut = 0;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    private void render() {
        screen(getString(R.string.breath_title), true);
        content.addView(ui.text(getString(R.string.breath_intro), Ui.BODY, p.sub));
        content.addView(ui.space(12));
        content.addView(ui.text(getString(R.string.length), Ui.SUBHEAD));
        content.addView(ui.space(6));
        CharSequence[] labels = new CharSequence[MINUTES.length];
        for (int i = 0; i < MINUTES.length; i++) labels[i] = getString(R.string.n_min, MINUTES[i]);
        content.addView(ui.segmented(labels, minutesIndex, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                minutesIndex = v;
                render();
            }
        }));
        JSONArray presets = Content.of(this).json("programs/breath.json").optJSONArray("presets");
        for (int i = 0; presets != null && i < presets.length(); i++) {
            final JSONObject o = presets.optJSONObject(i);
            content.addView(ui.listRow(R.drawable.ic_breath, p.tone("blue"), o.optString("name"),
                    o.optString("summary"), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            PlayerActivity.start(BreathActivity.this, Plans.breathTimeline(BreathActivity.this,
                                    o.optString("id"), MINUTES[minutesIndex]), "breath", o.optString("id"));
                        }
                    }));
        }
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.custom), Ui.HEADING));
        content.addView(ui.space(8));
        final Ui.Formatter secs = new Ui.Formatter() {
            @Override
            public String format(int v) {
                return getString(R.string.n_sec, v);
            }
        };
        content.addView(ui.slider(getString(R.string.breathe_in), 2, 10, in, secs, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                in = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.hold_after_in), 0, 10, holdIn, secs, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                holdIn = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.breathe_out), 2, 12, out, secs, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                out = v;
            }
        }));
        content.addView(ui.slider(getString(R.string.hold_after_out), 0, 10, holdOut, secs, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                holdOut = v;
            }
        }));
        LinearLayout f = footer();
        f.addView(ui.button(getString(R.string.start_custom), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerActivity.start(BreathActivity.this, Timeline.breath("custom", getString(R.string.custom), in, holdIn,
                        out, holdOut, 0, MINUTES[minutesIndex]), "breath", "custom");
            }
        }));
    }
}
