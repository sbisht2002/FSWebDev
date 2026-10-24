package com.fswebdev.fit90;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.content.Markdown;
import com.fswebdev.fit90.core.Days;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONException;
import org.json.JSONObject;

/** Reads an article (by id) or a plain content document (legal pages, further reading). */
public class ArticleActivity extends BaseActivity {

    public static final String EXTRA_ID = "id";
    public static final String EXTRA_DOC = "doc";

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        String doc = getIntent().getStringExtra(EXTRA_DOC);
        if (doc != null) {
            String md = Content.of(this).text(doc);
            java.util.Map<String, String> fm = Markdown.frontMatter(md);
            screen(fm.containsKey("title") ? fm.get("title") : "", true);
            Markdown.render(ui, md, content, links());
            return;
        }
        final String id = getIntent().getStringExtra(EXTRA_ID);
        final Content.Article a = Content.of(this).article(id);
        if (a == null) {
            screen("", true);
            content.addView(empty(getString(R.string.not_found)));
            return;
        }
        screen("", true);
        final ImageView mark = (ImageView) action(bookmarked(id) ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark,
                getString(R.string.bookmark), null);
        mark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean now = toggleBookmark(id);
                mark.setImageResource(now ? R.drawable.ic_bookmark_filled : R.drawable.ic_bookmark);
                toast(getString(now ? R.string.bookmarked : R.string.bookmark_removed));
            }
        });
        content.addView(ui.text(a.category.replace('-', ' '), Ui.OVERLINE, p.accent));
        content.addView(ui.text(a.title, Ui.TITLE));
        content.addView(ui.text(getString(R.string.n_min_read, a.minutes), Ui.CAPTION));
        content.addView(ui.space(8));
        Markdown.render(ui, Content.of(this).articleBody(id), content, links());
        content.addView(ui.space(12));
        boolean read = Progress.doneOn(this, Days.today(), "lesson", id);
        content.addView(ui.button(getString(read ? R.string.marked_read : R.string.mark_read), read ? Ui.SECONDARY : Ui.PRIMARY,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (!Progress.doneOn(ArticleActivity.this, Days.today(), "lesson", a.id)) {
                            Progress.record(ArticleActivity.this, "lesson", a.id, a.minutes, 0, 0, null);
                        }
                        finish();
                    }
                }));
    }

    /** In-app links: "app:article/<id>" opens another article. */
    private Markdown.LinkHandler links() {
        return new Markdown.LinkHandler() {
            @Override
            public boolean open(String url) {
                if (url.startsWith("app:article/")) {
                    ArticleActivity.this.open(ArticleActivity.class, EXTRA_ID, url.substring("app:article/".length()));
                    return true;
                }
                return false;
            }
        };
    }

    private boolean bookmarked(String id) {
        return Vault.of(this).obj("bookmarks").has(id);
    }

    private boolean toggleBookmark(String id) {
        JSONObject b = Vault.of(this).obj("bookmarks");
        boolean now = !b.has(id);
        try {
            if (now) b.put(id, System.currentTimeMillis());
            else b.remove(id);
        } catch (JSONException ignored) {
            // id is non-null.
        }
        Vault.of(this).put("bookmarks", b);
        return now;
    }
}
