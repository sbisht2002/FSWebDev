package com.fswebdev.fit90;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.content.Markdown;
import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * A program page driven by content/en/programs.json: intro, safety note, tools
 * (practice log, journal, check-in, audio, breathing) and its lessons in order.
 */
public class ProgramActivity extends BaseActivity {

    public static final String EXTRA_ID = "id";

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private JSONObject program(String id) {
        JSONArray all = Content.of(this).json("programs.json").optJSONArray("programs");
        for (int i = 0; all != null && i < all.length(); i++) {
            if (id.equals(all.optJSONObject(i).optString("id"))) return all.optJSONObject(i);
        }
        return new JSONObject();
    }

    private void render() {
        JSONObject pr = program(getIntent().getStringExtra(EXTRA_ID));
        screen(pr.optString("title"), true);
        if (!pr.optString("intro").isEmpty()) {
            Markdown.render(ui, pr.optString("intro"), content, null);
        }
        if (!pr.optString("safety").isEmpty()) {
            LinearLayout n = ui.notice(R.drawable.ic_info, p.tone("blue"), getString(R.string.safety_first), null);
            Markdown.render(ui, pr.optString("safety"), n, null);
            content.addView(n);
        }
        JSONArray tools = pr.optJSONArray("tools");
        if (tools != null && tools.length() > 0) {
            content.addView(ui.space(4));
            content.addView(ui.text(getString(R.string.practice), Ui.HEADING));
            content.addView(ui.space(8));
            for (int i = 0; i < tools.length(); i++) {
                final JSONObject t = tools.optJSONObject(i);
                int icon = getResources().getIdentifier("ic_" + t.optString("icon", "dot"), "drawable", getPackageName());
                content.addView(ui.listRow(icon, p.tone(t.optString("tone", "purple")), t.optString("title"),
                        t.optString("subtitle"), null, new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                openTool(t);
                            }
                        }));
            }
        }
        content.addView(ui.space(4));
        content.addView(ui.text(getString(R.string.lessons), Ui.HEADING));
        content.addView(ui.space(8));
        int n = 1;
        for (final Content.Article a : Content.of(this).articlesInProgram(pr.optString("id"))) {
            boolean read = false;
            for (com.fswebdev.fit90.data.Vault.Event e : Progress.sessions(this)) {
                if ("lesson".equals(e.data.optString("kind")) && a.id.equals(e.data.optString("ref"))) {
                    read = true;
                    break;
                }
            }
            content.addView(ui.listRow(read ? R.drawable.ic_check : R.drawable.ic_library,
                    read ? p.tone("accent") : p.tone("gold"), n++ + ". " + a.title,
                    getString(R.string.n_min_read, a.minutes), null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            open(ArticleActivity.class, ArticleActivity.EXTRA_ID, a.id);
                        }
                    }));
        }
        TextViewHolder.edu(this, ui, content);
    }

    private void openTool(JSONObject t) {
        String type = t.optString("type");
        String ref = t.optString("ref");
        switch (type) {
            case "practice": open(PracticeLogActivity.class); break;
            case "journal": open(JournalActivity.class); break;
            case "checkin": open(CheckInActivity.class); break;
            case "breath": PlayerActivity.start(this, Plans.breathTimeline(this, ref, t.optInt("minutes", 3)), "breath", ref); break;
            case "audio": AudioActivity.play(this, ref); break;
            case "article": open(ArticleActivity.class, ArticleActivity.EXTRA_ID, ref); break;
            case "doc": open(ArticleActivity.class, ArticleActivity.EXTRA_DOC, ref); break;
            case "kegel": open(KegelActivity.class); break;
            case "fitness": open(com.fswebdev.fit90.fitness.FitnessActivity.class); break;
            case "done":
                if (!Progress.doneOn(this, Days.today(), t.optString("kind", "practice"), ref)) {
                    Progress.record(this, t.optString("kind", "practice"), ref, t.optInt("minutes", 10), 0, 0, null);
                }
                toast(getString(R.string.logged));
                break;
            default:
        }
    }

    /** Shared "education, not medical advice" footer. */
    static final class TextViewHolder {
        static void edu(BaseActivity a, Ui ui, LinearLayout into) {
            android.widget.TextView t = ui.text(a.getString(R.string.education_note), Ui.CAPTION);
            t.setPadding(0, ui.dp(12), 0, 0);
            into.addView(t);
        }
    }
}
