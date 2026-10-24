package com.fswebdev.fit90;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.content.Markdown;
import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.core.Gamify;
import com.fswebdev.fit90.core.PlanGenerator;
import com.fswebdev.fit90.core.RedFlags;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.fitness.FitnessActivity;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Home: Today, Programs, Library, Coach (if enabled) and Progress tabs. */
public class MainActivity extends BaseActivity {

    private static final int TODAY = 0, PROGRAMS = 1, LIBRARY = 2, COACH = 3, PROGRESS = 4;

    private int tab = TODAY;
    private LinearLayout nav;
    private final List<Integer> tabs = new ArrayList<Integer>();
    private String query = "";
    private String category = "";
    private boolean bookmarksOnly;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        if (!settings.onboarded()) {
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }
        if (b != null) tab = b.getInt("tab", TODAY);
        tabs.add(TODAY);
        tabs.add(PROGRAMS);
        tabs.add(LIBRARY);
        if (BuildConfig.AI_COACH_ENABLED && settings.coachOn()) tabs.add(COACH);
        tabs.add(PROGRESS);
        build();
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putInt("tab", tab);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (content != null) render();
    }

    private void build() {
        screen(getString(R.string.app_name), false);
        action(R.drawable.ic_profile, getString(R.string.settings), new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(SettingsActivity.class);
            }
        });
        nav = ui.row();
        nav.setBackgroundColor(p.card);
        nav.setPadding(0, ui.dp(4), 0, ui.dp(4));
        column.addView(ui.divider());
        column.addView(nav);
    }

    private void render() {
        nav.removeAllViews();
        for (final int t : tabs) {
            boolean on = t == tab;
            LinearLayout item = ui.column();
            item.setGravity(Gravity.CENTER);
            item.setMinimumHeight(ui.dp(60));
            item.addView(ui.icon(tabIcon(t), on ? p.accent : p.sub, 24));
            TextView label = ui.text(tabName(t), Ui.CAPTION, on ? p.accent : p.sub);
            label.setGravity(Gravity.CENTER);
            item.addView(label);
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (t == COACH) {
                        open(ChatActivity.class);
                        return;
                    }
                    tab = t;
                    render();
                    scroll.scrollTo(0, 0);
                }
            });
            ui.asButton(item, tabName(t) + (on ? ", selected" : ""));
            item.setSelected(on);
            nav.addView(item, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        }
        content.removeAllViews();
        switch (tab) {
            case PROGRAMS: renderPrograms(); break;
            case LIBRARY: renderLibrary(); break;
            case PROGRESS: renderProgress(); break;
            default: renderToday();
        }
    }

    private int tabIcon(int t) {
        switch (t) {
            case PROGRAMS: return R.drawable.ic_programs;
            case LIBRARY: return R.drawable.ic_library;
            case COACH: return R.drawable.ic_coach;
            case PROGRESS: return R.drawable.ic_progress;
            default: return R.drawable.ic_today;
        }
    }

    private String tabName(int t) {
        switch (t) {
            case PROGRAMS: return getString(R.string.tab_programs);
            case LIBRARY: return getString(R.string.tab_library);
            case COACH: return BuildConfig.COACH_NAME;
            case PROGRESS: return getString(R.string.tab_progress);
            default: return getString(R.string.tab_today);
        }
    }

    // ------------------------------------------------------------------- Today

    private void renderToday() {
        String date = new SimpleDateFormat("EEEE, d MMMM", Locale.getDefault()).format(new Date());
        content.addView(ui.text(date, Ui.OVERLINE));
        content.addView(ui.text(getString(R.string.today_title), Ui.DISPLAY));
        content.addView(ui.space(12));

        Gamify.Streak streak = Progress.streak(this);
        int xp = Progress.totalXp(this);
        int level = Gamify.levelForXp(xp);
        LinearLayout stats = ui.card();
        LinearLayout row = ui.row();
        row.addView(ui.stat(String.valueOf(streak.current), getString(R.string.day_streak)), ui.weight(1));
        row.addView(ui.stat(String.valueOf(level), getString(R.string.level)), ui.weight(1));
        row.addView(ui.stat(String.valueOf(streak.freezes), getString(R.string.freezes)), ui.weight(1));
        stats.addView(row);
        stats.addView(ui.space(10));
        int base = Gamify.xpForLevel(level), next = Gamify.xpForLevel(Math.min(Gamify.MAX_LEVEL, level + 1));
        stats.addView(ui.progress(next > base ? (xp - base) / (float) (next - base) : 1f, p.gold, p.goldSoft));
        TextView xpLine = ui.text(getString(R.string.xp_line, xp, Math.max(0, next - xp), Math.min(100, level + 1)), Ui.CAPTION);
        xpLine.setPadding(0, ui.dp(6), 0, 0);
        stats.addView(xpLine);
        content.addView(stats);

        // Quick start
        LinearLayout quick = ui.row();
        quick.addView(quick(R.drawable.ic_breath, getString(R.string.quick_breathe), "blue", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(BreathActivity.class);
            }
        }), ui.margins(ui.weight(1), 0, 0, 5, 0));
        quick.addView(quick(R.drawable.ic_kegel, getString(R.string.quick_kegel), "warm", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(KegelActivity.class);
            }
        }), ui.margins(ui.weight(1), 5, 0, 5, 0));
        quick.addView(quick(R.drawable.ic_audio, getString(R.string.quick_audio), "purple", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(AudioActivity.class);
            }
        }), ui.margins(ui.weight(1), 5, 0, 0, 0));
        content.addView(quick, ui.margins(ui.fill(), 0, 0, 0, 16));

        // Today's plan
        PlanGenerator.Plan plan = Plans.plan(this);
        int today = Days.today();
        int planDay = Plans.planDay(this, today);
        PlanGenerator.Week week = plan.weeks.get(planDay / 7);
        LinearLayout head = ui.row();
        head.addView(ui.text(getString(R.string.todays_sessions), Ui.HEADING), ui.weight(1));
        TextView edit = ui.text(getString(R.string.week_n_of_8, planDay / 7 + 1), Ui.LABEL, p.accent);
        ui.clickable(edit, p.bg, 12, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(PlanActivity.class);
            }
        }, getString(R.string.open_plan));
        edit.setPadding(ui.dp(8), ui.dp(8), ui.dp(8), ui.dp(8));
        head.addView(edit);
        content.addView(head);
        TextView focus = ui.text(week.title + " · " + week.focus, Ui.SMALL);
        focus.setPadding(0, 0, 0, ui.dp(10));
        content.addView(focus);
        for (final PlanGenerator.Session s : Plans.forDay(this, plan, planDay)) {
            boolean done = Plans.done(this, today, s);
            int[] tone = p.tone(Plans.tone(s.type));
            String sub = (s.optional ? getString(R.string.optional) + " · " : "") + getString(R.string.n_min, s.minutes);
            LinearLayout r = ui.listRow(done ? R.drawable.ic_check : Plans.icon(s.type),
                    done ? new int[]{p.accent, p.accentSoft} : tone, Plans.title(this, s),
                    done ? getString(R.string.done_today) : sub, null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Plans.launch(MainActivity.this, s);
                            if ("walk".equals(s.type)) render();
                        }
                    });
            content.addView(r);
        }

        // Fitness program
        content.addView(ui.space(12));
        content.addView(ui.text(getString(R.string.fitness_title), Ui.HEADING));
        content.addView(ui.space(8));
        content.addView(ui.listRow(R.drawable.ic_dumbbell, p.tone("accent"), getString(R.string.fitness_card_title),
                getString(R.string.fitness_card_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        open(FitnessActivity.class);
                    }
                }));

        // Gentle reminder of any doctor cards
        List<String> flags = RedFlags.evaluate(Plans.profile(this));
        if (!flags.isEmpty()) {
            content.addView(ui.space(8));
            LinearLayout n = ui.notice(R.drawable.ic_info, p.tone("blue"), getString(R.string.doctor_reminder_title),
                    getString(R.string.doctor_reminder_body));
            ui.clickable(n, p.blueSoft, 20, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    open(ArticleActivity.class, ArticleActivity.EXTRA_ID, "sa-01");
                }
            }, getString(R.string.doctor_reminder_title));
            content.addView(n);
        }
        TextView edu = ui.text(getString(R.string.education_note), Ui.CAPTION);
        edu.setPadding(0, ui.dp(12), 0, 0);
        content.addView(edu);
    }

    private View quick(int icon, String label, String tone, View.OnClickListener l) {
        int[] t = p.tone(tone);
        LinearLayout box = ui.column();
        box.setGravity(Gravity.CENTER);
        box.setPadding(ui.dp(8), ui.dp(16), ui.dp(8), ui.dp(14));
        box.addView(ui.iconBadge(icon, t[0], t[1], 44));
        TextView tv = ui.text(label, Ui.LABEL);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(0, ui.dp(8), 0, 0);
        box.addView(tv);
        return ui.clickable(box, p.card, 20, l, label);
    }

    // ---------------------------------------------------------------- Programs

    private void renderPrograms() {
        content.addView(ui.text(getString(R.string.tab_programs), Ui.DISPLAY));
        content.addView(ui.space(12));
        JSONArray programs = Content.of(this).json("programs.json").optJSONArray("programs");
        if (programs == null) return;
        for (int i = 0; i < programs.length(); i++) {
            final JSONObject pr = programs.optJSONObject(i);
            int icon = getResources().getIdentifier("ic_" + pr.optString("icon", "dot"), "drawable", getPackageName());
            content.addView(ui.listRow(icon == 0 ? R.drawable.ic_dot : icon, p.tone(pr.optString("tone")),
                    pr.optString("title"), pr.optString("subtitle"), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            openProgram(pr);
                        }
                    }));
        }
    }

    private void openProgram(JSONObject pr) {
        String screen = pr.optString("screen");
        String id = pr.optString("id");
        switch (screen) {
            case "plan": open(PlanActivity.class); break;
            case "fitness": open(FitnessActivity.class); break;
            case "kegel": open(KegelActivity.class); break;
            case "breath": open(BreathActivity.class); break;
            case "audio": open(AudioActivity.class); break;
            case "journal": open(JournalActivity.class); break;
            case "checkin": open(CheckInActivity.class); break;
            default: open(ProgramActivity.class, ProgramActivity.EXTRA_ID, id);
        }
    }

    // ----------------------------------------------------------------- Library

    private void renderLibrary() {
        content.addView(ui.text(getString(R.string.tab_library), Ui.DISPLAY));
        content.addView(ui.space(12));
        final EditText search = new EditText(this);
        search.setHint(R.string.search_library);
        search.setSingleLine(true);
        search.setText(query);
        search.setTextColor(p.text);
        search.setHintTextColor(p.faint);
        search.setBackground(ui.outline(p.card, 16, p.line, 1.5f));
        search.setPadding(ui.dp(16), ui.dp(12), ui.dp(16), ui.dp(12));
        search.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_search, 0, 0, 0);
        search.setCompoundDrawablePadding(ui.dp(10));
        if (search.getCompoundDrawablesRelative()[0] != null) search.getCompoundDrawablesRelative()[0].setTint(p.sub);
        content.addView(search, ui.margins(ui.fill(), 0, 0, 0, 10));
        final LinearLayout results = ui.column();

        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = ui.row();
        String[][] cats = {{"", getString(R.string.all)}, {"*", getString(R.string.bookmarks)},
                {"pelvic-floor", "Pelvic floor"}, {"last-longer", "Last longer"}, {"confidence", "Confidence"},
                {"breathwork", "Breathing"}, {"diet", "Diet"}, {"exercise", "Exercise"}, {"lifestyle", "Lifestyle"},
                {"intimacy", "Intimacy"}, {"safety", "Safety"}};
        for (final String[] c : cats) {
            boolean on = c[0].equals("*") ? bookmarksOnly : !bookmarksOnly && c[0].equals(category);
            TextView chip = ui.chip(c[1], on ? p.onAccent : p.text, on ? p.accent : p.card);
            chip.setMinHeight(ui.dp(40));
            chip.setGravity(Gravity.CENTER);
            chip.setPadding(ui.dp(14), ui.dp(8), ui.dp(14), ui.dp(8));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    bookmarksOnly = c[0].equals("*");
                    category = bookmarksOnly ? "" : c[0];
                    render();
                }
            });
            ui.asButton(chip, c[1]);
            chips.addView(chip, ui.margins(ui.wrap(), 0, 0, 8, 0));
        }
        hs.addView(chips);
        content.addView(hs, ui.margins(ui.fill(), 0, 0, 0, 12));
        content.addView(results);
        fillResults(results);
        search.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                fillResults(results);
            }
        });

        content.addView(ui.space(12));
        content.addView(ui.text(getString(R.string.guides_and_more), Ui.HEADING));
        content.addView(ui.space(8));
        content.addView(ui.listRow(R.drawable.ic_doc, p.tone("gold"), getString(R.string.pdf_guides),
                getString(R.string.pdf_guides_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        open(PdfActivity.class);
                    }
                }));
        content.addView(ui.listRow(R.drawable.ic_external, p.tone("blue"), getString(R.string.further_reading),
                getString(R.string.further_reading_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        open(ArticleActivity.class, ArticleActivity.EXTRA_DOC, "further-reading.md");
                    }
                }));
    }

    private void fillResults(LinearLayout results) {
        results.removeAllViews();
        Content c = Content.of(this);
        JSONObject marks = Vault.of(this).obj("bookmarks");
        String q = query.trim().toLowerCase(Locale.getDefault());
        int shown = 0;
        for (final Content.Article a : c.articles()) {
            if (bookmarksOnly && !marks.has(a.id)) continue;
            if (!category.isEmpty() && !category.equals(a.category)) continue;
            if (!q.isEmpty()) {
                String hay = (a.title + " " + a.summary + " " + a.tags).toLowerCase(Locale.getDefault());
                if (!hay.contains(q) && !Markdown.plain(c.articleBody(a.id)).toLowerCase(Locale.getDefault()).contains(q)) continue;
            }
            results.addView(ui.listRow(0, null, a.title, a.summary + " · " + getString(R.string.n_min_read, a.minutes),
                    null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            open(ArticleActivity.class, ArticleActivity.EXTRA_ID, a.id);
                        }
                    }));
            shown++;
        }
        if (shown == 0) results.addView(empty(getString(bookmarksOnly ? R.string.no_bookmarks : R.string.no_results)));
    }

    // ---------------------------------------------------------------- Progress

    private void renderProgress() {
        content.addView(ui.text(getString(R.string.tab_progress), Ui.DISPLAY));
        content.addView(ui.space(12));
        int xp = Progress.totalXp(this);
        int level = Gamify.levelForXp(xp);
        Gamify.Streak streak = Progress.streak(this);
        LinearLayout top = ui.card();
        LinearLayout row = ui.row();
        row.addView(ui.stat(String.valueOf(level), getString(R.string.level_of_100)), ui.weight(1));
        row.addView(ui.stat(String.valueOf(xp), getString(R.string.xp)), ui.weight(1));
        row.addView(ui.stat(String.valueOf(streak.best), getString(R.string.best_streak)), ui.weight(1));
        top.addView(row);
        TextView fz = ui.text(getString(R.string.freeze_explainer, streak.current, streak.freezes), Ui.CAPTION);
        fz.setPadding(0, ui.dp(10), 0, 0);
        top.addView(fz);
        content.addView(top);

        // Sessions per week
        int[][] weeks = Progress.weekly(this, 8);
        LinearLayout chart = ui.card();
        chart.addView(ui.text(getString(R.string.sessions_per_week), Ui.SUBHEAD));
        chart.addView(ui.space(10));
        chart.addView(bars(weeks, 0, p.accent));
        TextView mins = ui.text(getString(R.string.minutes_this_week, weeks[7][1], weeks[7][0]), Ui.CAPTION);
        mins.setPadding(0, ui.dp(8), 0, 0);
        chart.addView(mins);
        content.addView(chart);

        // Self-rated control and confidence from weekly check-ins
        List<Vault.Event> checkins = Vault.of(this).events("checkin");
        LinearLayout trend = ui.card();
        trend.addView(ui.text(getString(R.string.self_ratings), Ui.SUBHEAD));
        if (checkins.isEmpty()) {
            trend.addView(ui.text(getString(R.string.self_ratings_empty), Ui.SMALL));
        } else {
            int n = Math.min(8, checkins.size());
            int[][] control = new int[n][2], confidence = new int[n][2];
            for (int i = 0; i < n; i++) {
                JSONObject d = checkins.get(checkins.size() - n + i).data;
                control[i][0] = d.optInt("control", 0);
                confidence[i][0] = d.optInt("confidence", 0);
            }
            trend.addView(ui.text(getString(R.string.control), Ui.CAPTION));
            trend.addView(bars(control, 0, p.warm));
            trend.addView(ui.space(8));
            trend.addView(ui.text(getString(R.string.confidence), Ui.CAPTION));
            trend.addView(bars(confidence, 0, p.purple));
        }
        trend.addView(ui.button(getString(R.string.weekly_checkin), Ui.SECONDARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(CheckInActivity.class);
            }
        }));
        content.addView(trend);

        // Pelvic floor
        LinearLayout kegel = ui.card();
        kegel.addView(ui.text(getString(R.string.pelvic_level, Progress.kegelLevel(this)), Ui.SUBHEAD));
        kegel.addView(ui.space(8));
        kegel.addView(ui.progress(Progress.kegelLevel(this) / 10f, p.warm, p.warmSoft));
        content.addView(kegel);

        // Badges
        content.addView(ui.text(getString(R.string.badges), Ui.HEADING));
        content.addView(ui.space(8));
        JSONObject meta = Content.of(this).json("badges.json");
        LinearLayout grid = null;
        int i = 0;
        for (Map.Entry<String, Boolean> e : Progress.badges(this).entrySet()) {
            if (i % 2 == 0) {
                grid = ui.row();
                content.addView(grid);
            }
            JSONObject bm = meta.optJSONObject(e.getKey());
            String title = bm != null ? bm.optString("title") : e.getKey();
            String desc = bm != null ? bm.optString("description") : "";
            LinearLayout b = ui.card(e.getValue() ? p.goldSoft : p.card);
            b.setAlpha(e.getValue() ? 1f : 0.55f);
            b.addView(ui.icon(e.getValue() ? R.drawable.ic_star_filled : R.drawable.ic_star, e.getValue() ? p.gold : p.faint, 24));
            b.addView(ui.text(title, Ui.LABEL));
            b.addView(ui.text(desc, Ui.CAPTION));
            b.setContentDescription(title + ". " + desc + (e.getValue() ? ". Earned" : ". Not earned yet"));
            grid.addView(b, ui.margins(ui.weight(1), i % 2 == 0 ? 0 : 6, 0, i % 2 == 0 ? 6 : 0, 12));
            i++;
        }
        if (i % 2 == 1 && grid != null) grid.addView(new View(this), ui.margins(ui.weight(1), 6, 0, 0, 12));
    }

    /** Simple bar chart: one bar per value, scaled to the largest. */
    private View bars(int[][] values, int col, int color) {
        int max = 1;
        for (int[] v : values) max = Math.max(max, v[col]);
        LinearLayout chart = ui.row();
        chart.setGravity(Gravity.BOTTOM);
        StringBuilder desc = new StringBuilder();
        for (int[] v : values) {
            LinearLayout c = ui.column();
            c.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL);
            TextView n = ui.text(String.valueOf(v[col]), Ui.CAPTION);
            n.setGravity(Gravity.CENTER);
            c.addView(n, ui.fill());
            View bar = new View(this);
            bar.setBackground(ui.round(v[col] > 0 ? color : p.line, 6));
            int h = ui.dp(4 + 76f * v[col] / max);
            c.addView(bar, new LinearLayout.LayoutParams(ui.dp(18), h));
            chart.addView(c, new LinearLayout.LayoutParams(0, ui.dp(104), 1));
            desc.append(v[col]).append(", ");
        }
        chart.setContentDescription(desc.toString());
        return chart;
    }
}
