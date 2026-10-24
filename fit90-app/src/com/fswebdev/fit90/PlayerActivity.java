package com.fswebdev.fit90;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.session.PlayerService;
import com.fswebdev.fit90.session.Timeline;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.CueView;
import com.fswebdev.fit90.ui.Ui;

/** Session player for Kegel, breathwork and guided audio timelines. */
public class PlayerActivity extends BaseActivity {

    public static final String EXTRA_TIMELINE = "timeline";
    public static final String EXTRA_KIND = "kind";
    public static final String EXTRA_REF = "ref";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private CueView cue;
    private TextView label, countdown, counter, caption, pauseBtn;
    private String kind, ref;
    private boolean reflectionShown;
    private int ease, mood;

    public static void start(Context c, Timeline t, String kind, String ref) {
        c.startActivity(new Intent(c, PlayerActivity.class).putExtra(EXTRA_TIMELINE, t.toJson())
                .putExtra(EXTRA_KIND, kind).putExtra(EXTRA_REF, ref));
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        kind = getIntent().getStringExtra(EXTRA_KIND);
        ref = getIntent().getStringExtra(EXTRA_REF);
        PlayerService.State s = PlayerService.state();
        String json = getIntent().getStringExtra(EXTRA_TIMELINE);
        if (json != null && b == null) {
            Timeline t = Timeline.fromJson(json);
            PlayerService.start(this, t);
            s.timeline = t;
            s.index = -1;
            s.running = true;
            s.finished = false;
            s.pausedRemainingMs = -1;
        }
        if (s.timeline == null) {
            finish();
            return;
        }
        if (kind == null) kind = s.timeline.kind;
        if (ref == null) ref = s.timeline.id;
        build(s.timeline);
    }

    private void build(Timeline t) {
        screen(t.title, true);
        cue = new CueView(this, p, settings.reduceMotion());
        FrameLayout stage = new FrameLayout(this);
        stage.addView(cue, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout mid = ui.column();
        mid.setGravity(Gravity.CENTER);
        label = ui.text("", Ui.TITLE);
        label.setGravity(Gravity.CENTER);
        label.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        countdown = ui.text("", Ui.DISPLAY, p.sub);
        countdown.setGravity(Gravity.CENTER);
        countdown.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        mid.addView(label, ui.fill());
        mid.addView(countdown, ui.fill());
        stage.addView(mid, new FrameLayout.LayoutParams(-1, -2, Gravity.CENTER));
        content.addView(stage, new LinearLayout.LayoutParams(-1, ui.dp(300)));

        counter = ui.text("", Ui.LABEL, p.sub);
        counter.setGravity(Gravity.CENTER);
        content.addView(counter, ui.fill());
        caption = ui.text("", Ui.BODY);
        caption.setGravity(Gravity.CENTER);
        caption.setMinHeight(ui.dp(72));
        caption.setPadding(ui.dp(8), ui.dp(12), ui.dp(8), ui.dp(12));
        caption.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        content.addView(caption, ui.fill());

        LinearLayout controls = ui.row();
        pauseBtn = ui.button(getString(R.string.pause), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PlayerService.command(PlayerActivity.this,
                        PlayerService.state().paused() ? PlayerService.ACTION_RESUME : PlayerService.ACTION_PAUSE);
            }
        });
        controls.addView(pauseBtn, ui.margins(ui.weight(1), 0, 0, 6, 0));
        controls.addView(ui.button(getString(R.string.stop), Ui.OUTLINE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmStop();
            }
        }), ui.margins(ui.weight(1), 6, 0, 0, 0));
        content.addView(controls);

        LinearLayout toggles = ui.row();
        toggles.setGravity(Gravity.CENTER);
        toggles.addView(toggle(R.drawable.ic_volume, getString(R.string.voice), settings.voice(), 0));
        toggles.addView(toggle(R.drawable.ic_audio, getString(R.string.sound), settings.sound(), 1));
        toggles.addView(toggle(R.drawable.ic_vibrate, getString(R.string.vibration), settings.haptics(), 2));
        content.addView(toggles, ui.margins(ui.fill(), 0, 12, 0, 0));
        TextView hint = ui.text(getString(R.string.player_hint), Ui.CAPTION);
        hint.setGravity(Gravity.CENTER);
        content.addView(hint, ui.margins(ui.fill(), 0, 12, 0, 0));
    }

    private View toggle(int icon, final String name, boolean on, final int which) {
        final TextView t = ui.chip(name + (on ? " on" : " off"), on ? p.accent : p.sub, on ? p.accentSoft : p.cardAlt);
        t.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, 0, 0, 0);
        t.setCompoundDrawablePadding(ui.dp(6));
        t.setMinHeight(ui.dp(44));
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(ui.dp(12), ui.dp(6), ui.dp(14), ui.dp(6));
        if (t.getCompoundDrawablesRelative()[0] != null) {
            t.getCompoundDrawablesRelative()[0].setTint(on ? p.accent : p.sub);
        }
        t.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean now;
                if (which == 0) { now = !settings.voice(); settings.setVoice(now); }
                else if (which == 1) { now = !settings.sound(); settings.setSound(now); }
                else { now = !settings.haptics(); settings.setHaptics(now); }
                t.setText(name + (now ? " on" : " off"));
                t.setTextColor(now ? p.accent : p.sub);
                t.setBackground(ui.round(now ? p.accentSoft : p.cardAlt, 12));
                if (t.getCompoundDrawablesRelative()[0] != null) {
                    t.getCompoundDrawablesRelative()[0].setTint(now ? p.accent : p.sub);
                }
            }
        });
        ui.asButton(t, name);
        t.setLayoutParams(ui.margins(ui.wrap(), 4, 0, 4, 0));
        return t;
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            update();
            handler.postDelayed(this, 80);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(tick);
    }

    private void update() {
        PlayerService.State s = PlayerService.state();
        if (!s.running) {
            if (s.finished && !reflectionShown) showReflection();
            else if (!s.finished && !reflectionShown) finish();
            return;
        }
        Timeline.Step step = s.step();
        if (step == null) {
            label.setText(R.string.get_ready);
            return;
        }
        cue.set(step.kind, s.stepProgress());
        String word = cueWord(step.kind);
        if (!word.contentEquals(label.getText())) label.setText(word);
        countdown.setText(step.seconds > 0 ? String.valueOf(s.secondsLeftInStep()) : "");
        counter.setText(step.counter);
        if (settings.captions() && !step.text.isEmpty() && !step.text.contentEquals(caption.getText())) {
            caption.setText(step.text);
        }
        pauseBtn.setText(s.paused() ? R.string.resume : R.string.pause);
    }

    private String cueWord(String k) {
        switch (k) {
            case Timeline.SQUEEZE: return getString(R.string.cue_squeeze);
            case Timeline.FLICK: return getString(R.string.cue_flick);
            case Timeline.HOLD: return getString(R.string.cue_hold);
            case Timeline.RELEASE: return getString(R.string.cue_release);
            case Timeline.REST: return getString(R.string.cue_rest);
            case Timeline.DROP: return getString(R.string.cue_drop);
            case Timeline.IN: return getString(R.string.cue_in);
            case Timeline.OUT: return getString(R.string.cue_out);
            case Timeline.HOLD_IN:
            case Timeline.HOLD_OUT: return getString(R.string.cue_hold);
            case Timeline.CHIME: return getString(R.string.cue_done);
            default: return getString(R.string.cue_listen);
        }
    }

    private void confirmStop() {
        new AlertDialog.Builder(this).setTitle(R.string.stop_session_title)
                .setMessage(R.string.stop_session_body)
                .setNegativeButton(R.string.keep_going, null)
                .setPositiveButton(R.string.stop, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        PlayerService.command(PlayerActivity.this, PlayerService.ACTION_STOP);
                        finish();
                    }
                }).show();
    }

    private void showReflection() {
        reflectionShown = true;
        handler.removeCallbacks(tick);
        PlayerService.State s = PlayerService.state();
        int minutes = (int) Math.max(1, Math.round(s.timeline.totalSeconds() / 60.0));
        final Vault.Event event = Progress.record(this, kind, ref, minutes, 0, 0, null);
        content.removeAllViews();
        content.addView(ui.text(getString(R.string.session_done_title), Ui.TITLE));
        content.addView(ui.text(getString(R.string.session_done_xp, event.data.optInt("xp")), Ui.BODY, p.accent));
        content.addView(ui.space(16));
        boolean askEase = "kegel".equals(kind) || "workout".equals(kind) || "breath".equals(kind);
        if (askEase) {
            content.addView(ui.text(getString(R.string.how_did_it_feel), Ui.SUBHEAD));
            content.addView(ui.space(6));
            content.addView(ui.segmented(getResources().getStringArray(R.array.ease), -1, new Ui.IntCallback() {
                @Override
                public void on(int v) {
                    ease = v + 1;
                    saveReflection(event, null, false);
                }
            }));
        }
        content.addView(ui.text(getString(R.string.how_is_your_mood), Ui.SUBHEAD));
        content.addView(ui.space(6));
        content.addView(ui.segmented(getResources().getStringArray(R.array.mood), -1, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                mood = v + 1;
                saveReflection(event, null, false);
            }
        }));
        final EditText note = new EditText(this);
        note.setHint(R.string.optional_note);
        note.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        note.setTextColor(p.text);
        note.setHintTextColor(p.faint);
        note.setBackground(ui.outline(p.card, 14, p.line, 1.5f));
        note.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        note.setMinLines(2);
        content.addView(note, ui.margins(ui.fill(), 0, 0, 0, 12));
        content.addView(ui.button(getString(R.string.done), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                saveReflection(event, note.getText().toString(), true);
            }
        }));
    }

    private void saveReflection(Vault.Event e, String note, boolean close) {
        Progress.reflect(this, e, ease, mood, note);
        if (!close) return;
        if ("kegel".equals(kind) && Progress.kegelCanLevelUp(this)) {
            final int next = Progress.kegelLevel(this) + 1;
            new AlertDialog.Builder(this).setTitle(getString(R.string.level_up_title, next))
                    .setMessage(R.string.level_up_body)
                    .setNegativeButton(R.string.not_yet, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            finish();
                        }
                    })
                    .setPositiveButton(R.string.move_up, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface d, int w) {
                            Progress.setKegelLevel(PlayerActivity.this, next);
                            finish();
                        }
                    }).show();
            return;
        }
        finish();
    }
}
