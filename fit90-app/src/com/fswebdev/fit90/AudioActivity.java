package com.fswebdev.fit90;

import android.content.Context;
import android.os.Bundle;
import android.view.View;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.content.Markdown;
import com.fswebdev.fit90.session.Timeline;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import java.util.LinkedHashMap;
import java.util.Map;

/** Guided audio library: scripts read aloud on-device with captions. */
public class AudioActivity extends BaseActivity {

    private static final Map<String, String> CATEGORIES = new LinkedHashMap<String, String>();

    static {
        CATEGORIES.put("pelvic", "Guided pelvic floor");
        CATEGORIES.put("breath", "Breathwork");
        CATEGORIES.put("bodyscan", "Body scan");
        CATEGORIES.put("arousal", "Arousal awareness");
        CATEGORIES.put("calm", "Pre-intimacy calm");
        CATEGORIES.put("confidence", "Confidence");
        CATEGORIES.put("sleep", "Sleep wind-down");
    }

    private String category = "pelvic";

    public static void play(Context c, String id) {
        Content content = Content.of(c);
        Content.Audio a = content.audio(id);
        if (a == null) return;
        String body = Markdown.stripFrontMatter(content.audioScript(id));
        PlayerActivity.start(c, Timeline.script(a.id, a.title, a.ambient, body), "audio", a.id);
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        render();
    }

    private void render() {
        screen(getString(R.string.audio_title), true);
        content.addView(ui.text(getString(R.string.audio_intro), Ui.BODY, p.sub));
        content.addView(ui.space(12));
        final String[] keys = CATEGORIES.keySet().toArray(new String[0]);
        int sel = 0;
        for (int i = 0; i < keys.length; i++) if (keys[i].equals(category)) sel = i;
        for (int row = 0; row < keys.length; row += 4) {
            int n = Math.min(4, keys.length - row);
            CharSequence[] labels = new CharSequence[n];
            for (int i = 0; i < n; i++) labels[i] = CATEGORIES.get(keys[row + i]);
            final int offset = row;
            content.addView(ui.segmented(labels, sel >= row && sel < row + n ? sel - row : -1, new Ui.IntCallback() {
                @Override
                public void on(int v) {
                    category = keys[offset + v];
                    render();
                }
            }));
        }
        int shown = 0;
        for (final Content.Audio a : Content.of(this).audio()) {
            if (!a.category.equals(category)) continue;
            content.addView(ui.listRow(R.drawable.ic_play, p.tone("purple"), a.title,
                    getString(R.string.audio_meta, a.level, a.minutes) + (a.summary.isEmpty() ? "" : " · " + a.summary),
                    null, new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            play(AudioActivity.this, a.id);
                        }
                    }));
            shown++;
        }
        if (shown == 0) content.addView(empty(getString(R.string.no_audio_yet)));
    }
}
