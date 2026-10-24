package com.fswebdev.fit90;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.core.Kegel;
import com.fswebdev.fit90.core.RedFlags;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.session.Timeline;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import java.util.List;
import java.util.Locale;

/** Pelvic-floor trainer: current level, lessons, start, level list and safety. */
public class KegelActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        screen(getString(R.string.kegel_title), true);
        final List<Kegel.Level> levels = Kegel.levels(Content.of(this).json("programs/kegel.json"));
        final int current = Progress.kegelLevel(this);
        final boolean pain = RedFlags.painPresent(Plans.profile(this));
        final boolean relaxFirst = RedFlags.relaxationFirst(Plans.profile(this));
        Kegel.Level level = Plans.kegelLevel(this, false);

        if (pain) {
            content.addView(ui.notice(R.drawable.ic_alert, p.tone("danger"), getString(R.string.kegel_pain_title),
                    getString(R.string.kegel_pain_body)));
        }
        LinearLayout hero = ui.card(p.warmSoft);
        hero.addView(ui.text(getString(R.string.level_n_of_10, current), Ui.OVERLINE, p.warm));
        hero.addView(ui.text(level.name, Ui.TITLE));
        TextView focus = ui.text(level.focus, Ui.SMALL, p.text);
        focus.setPadding(0, ui.dp(4), 0, ui.dp(10));
        hero.addView(focus);
        hero.addView(ui.text(String.format(Locale.getDefault(), getString(R.string.kegel_dose), level.sets, level.reps,
                level.hold, level.rest, level.flicks, level.drops, level.minutes()), Ui.SMALL, p.text));
        hero.addView(ui.space(12));
        hero.addView(ui.progress(Math.min(1f, Progress.kegelSessionsAtLevel(this) / (float) Kegel.SESSIONS_TO_LEVEL_UP),
                p.warm, p.card));
        TextView sessions = ui.text(getString(R.string.sessions_at_level, Progress.kegelSessionsAtLevel(this),
                Kegel.SESSIONS_TO_LEVEL_UP), Ui.CAPTION, p.text);
        sessions.setPadding(0, ui.dp(6), 0, ui.dp(8));
        hero.addView(sessions);
        hero.addView(ui.button(getString(R.string.start_session), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerActivity.start(KegelActivity.this, Timeline.kegel(Plans.kegelLevel(KegelActivity.this, false)),
                        "kegel", "level");
            }
        }));
        if (!pain) {
            hero.addView(ui.button(getString(R.string.relax_variant), Ui.OUTLINE, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    PlayerActivity.start(KegelActivity.this, Timeline.kegel(Plans.kegelLevel(KegelActivity.this, true)),
                            "kegel", "relax");
                }
            }));
        }
        content.addView(hero);
        if (relaxFirst && !pain) {
            content.addView(ui.notice(R.drawable.ic_info, p.tone("blue"), getString(R.string.relax_first_title),
                    getString(R.string.relax_first_body)));
        }
        if (Progress.kegelCanLevelUp(this)) {
            content.addView(ui.button(getString(R.string.level_up_title, current + 1), Ui.SECONDARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Progress.setKegelLevel(KegelActivity.this, current + 1);
                    render();
                }
            }));
        } else if (Kegel.suggestEasier(current, Progress.kegelEase(this))) {
            content.addView(ui.notice(R.drawable.ic_info, p.tone("gold"), getString(R.string.step_back_title),
                    getString(R.string.step_back_body)));
            content.addView(ui.button(getString(R.string.go_to_level, current - 1), Ui.SECONDARY, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Progress.setKegelLevel(KegelActivity.this, current - 1);
                    render();
                }
            }));
        }

        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.lessons), Ui.HEADING));
        content.addView(ui.space(8));
        for (final Content.Article a : Content.of(this).articlesInProgram("pelvic-floor")) {
            content.addView(ui.listRow(R.drawable.ic_library, p.tone("gold"), a.title,
                    getString(R.string.n_min_read, a.minutes), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            open(ArticleActivity.class, ArticleActivity.EXTRA_ID, a.id);
                        }
                    }));
        }

        content.addView(ui.space(8));
        content.addView(ui.text(getString(R.string.all_levels), Ui.HEADING));
        TextView rules = ui.text(getString(R.string.level_rules, Kegel.SESSIONS_TO_LEVEL_UP), Ui.SMALL);
        rules.setPadding(0, ui.dp(4), 0, ui.dp(8));
        content.addView(rules);
        for (final Kegel.Level l : levels) {
            boolean here = l.number == current;
            LinearLayout row = ui.listRow(0, null, getString(R.string.level_n, l.number) + " · " + l.name,
                    String.format(Locale.getDefault(), getString(R.string.level_summary), l.sets, l.reps, l.hold, l.flicks,
                            l.drops), here ? getString(R.string.current) : null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            chooseLevel(l.number);
                        }
                    });
            content.addView(row);
        }
    }

    private void chooseLevel(final int n) {
        new AlertDialog.Builder(this).setTitle(getString(R.string.level_n, n))
                .setMessage(R.string.choose_level_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.use_this_level, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Progress.setKegelLevel(KegelActivity.this, n);
                        render();
                    }
                }).show();
    }
}
