package com.fswebdev.fit90;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Last-longer practice log. Records only the technique, solo/partnered, and a 0–10
 * self-rating of control — never any graphic detail.
 */
public class PracticeLogActivity extends BaseActivity {

    private static final String[] TECHNIQUES = {"awareness", "stop_start", "squeeze", "breathing"};
    private static final String[] CONTEXTS = {"solo", "partner"};
    private int technique = 1, context = 0, control = 5;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        screen(getString(R.string.practice_title), true);
        content.addView(ui.text(getString(R.string.practice_intro), Ui.BODY, p.sub));
        content.addView(ui.space(8));
        for (final Content.Article a : Content.of(this).articlesInProgram("last-longer")) {
            content.addView(ui.listRow(R.drawable.ic_library, p.tone("gold"), a.title,
                    getString(R.string.n_min_read, a.minutes), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            open(ArticleActivity.class, ArticleActivity.EXTRA_ID, a.id);
                        }
                    }));
        }
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.log_practice), Ui.HEADING));
        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.technique), Ui.SUBHEAD));
        content.addView(ui.space(6));
        content.addView(ui.segmented(getResources().getStringArray(R.array.techniques), technique, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                technique = v;
                render();
            }
        }));
        content.addView(ui.segmented(getResources().getStringArray(R.array.practice_context), context, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                context = v;
                render();
            }
        }));
        content.addView(ui.slider(getString(R.string.control_rating), 0, 10, control, new Ui.Formatter() {
            @Override
            public String format(int v) {
                return v + " / 10";
            }
        }, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                control = v;
            }
        }));
        content.addView(ui.button(getString(R.string.save), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject d = new JSONObject();
                try {
                    d.put("technique", TECHNIQUES[technique]).put("context", CONTEXTS[context]).put("control", control);
                } catch (JSONException ignored) {
                    // Constant keys.
                }
                Vault.of(PracticeLogActivity.this).add("practice", d);
                Progress.record(PracticeLogActivity.this, "practice", TECHNIQUES[technique], 10, 0, 0, null);
                toast(getString(R.string.saved));
                render();
            }
        }));

        List<Vault.Event> logs = Vault.of(this).events("practice");
        if (!logs.isEmpty()) {
            LinearLayout card = ui.card();
            card.addView(ui.text(getString(R.string.recent_practice), Ui.SUBHEAD));
            String[] names = getResources().getStringArray(R.array.techniques);
            SimpleDateFormat fmt = new SimpleDateFormat("d MMM", Locale.getDefault());
            for (int i = logs.size() - 1; i >= Math.max(0, logs.size() - 8); i--) {
                JSONObject d = logs.get(i).data;
                int ti = 0;
                for (int k = 0; k < TECHNIQUES.length; k++) if (TECHNIQUES[k].equals(d.optString("technique"))) ti = k;
                card.addView(ui.text(fmt.format(new Date(logs.get(i).ts)) + " · " + names[ti] + " · "
                        + getString(R.string.control) + " " + d.optInt("control") + "/10", Ui.SMALL));
            }
            content.addView(card);
        }
    }
}
