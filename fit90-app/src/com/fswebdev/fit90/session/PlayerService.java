package com.fswebdev.fit90.session;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.ToneGenerator;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.fswebdev.fit90.Notifications;
import com.fswebdev.fit90.PlayerActivity;
import com.fswebdev.fit90.R;
import com.fswebdev.fit90.data.Settings;

import java.util.Locale;
import java.util.Random;

/**
 * Runs a {@link Timeline} as a foreground media service so cues keep coming with the
 * screen locked: text-to-speech, short tones, vibration and optional soft background
 * noise. The player screen reads {@link #state()} to draw itself.
 */
public class PlayerService extends Service {

    public static final String EXTRA_TIMELINE = "timeline";
    public static final String ACTION_PAUSE = "pause", ACTION_RESUME = "resume", ACTION_STOP = "stop";

    /** Snapshot read by the UI thread. */
    public static final class State {
        public Timeline timeline;
        public int index = -1;
        public long stepStartedAt;
        public long stepLengthMs;
        public long pausedRemainingMs = -1;
        public boolean finished;
        public boolean running;
        public long startedAt;
        public long activeMs;

        public Timeline.Step step() {
            return timeline != null && index >= 0 && index < timeline.steps.size() ? timeline.steps.get(index) : null;
        }

        public boolean paused() {
            return pausedRemainingMs >= 0;
        }

        /** 0..1 progress through the current step. */
        public float stepProgress() {
            if (stepLengthMs <= 0) return 0;
            long elapsed = paused() ? stepLengthMs - pausedRemainingMs : SystemClock.uptimeMillis() - stepStartedAt;
            return Math.max(0f, Math.min(1f, elapsed / (float) stepLengthMs));
        }

        public int secondsLeftInStep() {
            long left = paused() ? pausedRemainingMs : stepLengthMs - (SystemClock.uptimeMillis() - stepStartedAt);
            return (int) Math.max(0, Math.ceil(left / 1000.0));
        }
    }

    private static final State STATE = new State();

    public static State state() {
        return STATE;
    }

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextToSpeech tts;
    private boolean ttsReady;
    private ToneGenerator tones;
    private Vibrator vibrator;
    private PowerManager.WakeLock wakeLock;
    private Ambient ambient;
    private Settings settings;
    private String waitingUtterance;

    private final Runnable advance = new Runnable() {
        @Override
        public void run() {
            next();
        }
    };

    public static void start(Context c, Timeline t) {
        Intent i = new Intent(c, PlayerService.class).putExtra(EXTRA_TIMELINE, t.toJson());
        if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
        else c.startService(i);
    }

    public static void command(Context c, String action) {
        c.startService(new Intent(c, PlayerService.class).setAction(action));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        settings = Settings.of(this);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        try {
            tones = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
        } catch (RuntimeException e) {
            tones = null;
        }
        tts = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS && tts != null) {
                    int r = tts.setLanguage(Locale.getDefault());
                    if (r == TextToSpeech.LANG_MISSING_DATA || r == TextToSpeech.LANG_NOT_SUPPORTED) tts.setLanguage(Locale.US);
                    tts.setSpeechRate(settings.speechRatePercent() / 100f);
                    ttsReady = true;
                }
            }
        });
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String id) {
            }

            @Override
            public void onDone(final String id) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (id.equals(waitingUtterance) && !STATE.paused()) {
                            waitingUtterance = null;
                            handler.removeCallbacks(advance);
                            handler.postDelayed(advance, 400);
                        }
                    }
                });
            }

            @Override
            public void onError(String id) {
                onDone(id);
            }
        });
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "fit90:session");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent == null ? null : intent.getAction();
        if (ACTION_PAUSE.equals(action)) {
            pause();
        } else if (ACTION_RESUME.equals(action)) {
            resume();
        } else if (ACTION_STOP.equals(action)) {
            stopSession(false);
        } else if (intent != null && intent.hasExtra(EXTRA_TIMELINE)) {
            begin(Timeline.fromJson(intent.getStringExtra(EXTRA_TIMELINE)));
        }
        return START_NOT_STICKY;
    }

    private void begin(Timeline t) {
        handler.removeCallbacks(advance);
        STATE.timeline = t;
        STATE.index = -1;
        STATE.finished = false;
        STATE.running = true;
        STATE.pausedRemainingMs = -1;
        STATE.startedAt = SystemClock.uptimeMillis();
        STATE.activeMs = 0;
        goForeground();
        if (!wakeLock.isHeld()) wakeLock.acquire(2 * 60 * 60 * 1000L);
        if (settings.ambient() && (!"none".equals(t.ambient) || "audio".equals(t.kind))) {
            ambient = new Ambient();
            ambient.start();
        }
        // Give text-to-speech a moment to initialise before the first spoken line.
        handler.postDelayed(advance, ttsReady ? 300 : 1200);
    }

    private void next() {
        State s = STATE;
        if (s.timeline == null) return;
        if (s.index >= 0) s.activeMs += s.stepLengthMs;
        s.index++;
        if (s.index >= s.timeline.steps.size()) {
            stopSession(true);
            return;
        }
        Timeline.Step step = s.step();
        cue(step);
        boolean speak = settings.voice() && ttsReady && !step.text.isEmpty();
        if (speak) {
            String id = "u" + s.index;
            tts.speak(step.text, TextToSpeech.QUEUE_FLUSH, null, id);
            if (step.seconds <= 0) waitingUtterance = id;
        }
        long ms = (long) (step.estimatedSeconds() * 1000);
        s.stepStartedAt = SystemClock.uptimeMillis();
        s.stepLengthMs = ms;
        if (step.seconds > 0 || !speak) {
            handler.postDelayed(advance, ms);
        } else {
            // Spoken step: advance when speech ends (with a safety timeout).
            handler.postDelayed(advance, ms * 3 + 4000);
        }
        updateNotification();
    }

    /** Tone and vibration for cue changes. */
    private void cue(Timeline.Step step) {
        String k = step.kind;
        boolean strong = Timeline.SQUEEZE.equals(k) || Timeline.FLICK.equals(k);
        boolean soft = Timeline.RELEASE.equals(k) || Timeline.IN.equals(k) || Timeline.OUT.equals(k)
                || Timeline.DROP.equals(k) || Timeline.REST.equals(k);
        if (settings.haptics() && vibrator != null && vibrator.hasVibrator() && (strong || soft)) {
            long len = strong ? 120 : 40;
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator.vibrate(VibrationEffect.createOneShot(len, strong ? 200 : 80));
            } else {
                vibrator.vibrate(len);
            }
        }
        if (settings.sound() && tones != null) {
            if (Timeline.CHIME.equals(k)) tones.startTone(ToneGenerator.TONE_PROP_ACK, 300);
            else if (strong && !settings.voice()) tones.startTone(ToneGenerator.TONE_PROP_BEEP, 120);
            else if (Timeline.REST.equals(k)) tones.startTone(ToneGenerator.TONE_PROP_BEEP2, 120);
        }
    }

    private void pause() {
        State s = STATE;
        if (!s.running || s.paused()) return;
        handler.removeCallbacks(advance);
        long left = s.stepLengthMs - (SystemClock.uptimeMillis() - s.stepStartedAt);
        s.pausedRemainingMs = Math.max(0, left);
        if (tts != null) tts.stop();
        if (ambient != null) ambient.mute(true);
        updateNotification();
    }

    private void resume() {
        State s = STATE;
        if (!s.running || !s.paused()) return;
        long left = s.pausedRemainingMs;
        s.pausedRemainingMs = -1;
        Timeline.Step step = s.step();
        if (ambient != null) ambient.mute(false);
        if (step != null && step.seconds <= 0) {
            // Restart a spoken line from the beginning.
            s.index--;
            next();
            return;
        }
        s.stepStartedAt = SystemClock.uptimeMillis() - (s.stepLengthMs - left);
        handler.postDelayed(advance, left);
        updateNotification();
    }

    private void stopSession(boolean completed) {
        handler.removeCallbacks(advance);
        STATE.running = false;
        STATE.finished = completed;
        STATE.pausedRemainingMs = -1;
        if (tts != null) tts.stop();
        if (ambient != null) {
            ambient.stopNow();
            ambient = null;
        }
        if (wakeLock.isHeld()) wakeLock.release();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE);
        else stopForeground(true);
        stopSelf();
    }

    private Notification buildNotification() {
        State s = STATE;
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, PlayerActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String act = s.paused() ? ACTION_RESUME : ACTION_PAUSE;
        PendingIntent toggle = PendingIntent.getService(this, 1, new Intent(this, PlayerService.class).setAction(act),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        PendingIntent stop = PendingIntent.getService(this, 2, new Intent(this, PlayerService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        // Neutral wording only: the notification can show on the lock screen.
        return Notifications.builder(this, Notifications.CHANNEL_PLAYER)
                .setSmallIcon(R.drawable.ic_audio)
                .setContentTitle(getString(R.string.player_notification_title))
                .setContentText(getString(s.paused() ? R.string.paused : R.string.in_progress))
                .setContentIntent(open)
                .setOngoing(!s.paused())
                .setOnlyAlertOnce(true)
                .setVisibility(Notification.VISIBILITY_PUBLIC)
                .addAction(new Notification.Action.Builder(null, getString(s.paused() ? R.string.resume : R.string.pause), toggle).build())
                .addAction(new Notification.Action.Builder(null, getString(R.string.stop), stop).build())
                .build();
    }

    private void goForeground() {
        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notifications.ID_PLAYER, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(Notifications.ID_PLAYER, n);
        }
    }

    private void updateNotification() {
        if (!STATE.running) return;
        try {
            getSystemService(android.app.NotificationManager.class).notify(Notifications.ID_PLAYER, buildNotification());
        } catch (SecurityException ignored) {
            // Notifications disabled; the session still runs.
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        if (tones != null) tones.release();
        if (ambient != null) ambient.stopNow();
        if (wakeLock != null && wakeLock.isHeld()) wakeLock.release();
        STATE.running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /** Very quiet, slowly drifting brown noise as an optional background bed. */
    static final class Ambient extends Thread {
        private volatile boolean stop, muted;

        void mute(boolean m) {
            muted = m;
        }

        void stopNow() {
            stop = true;
        }

        @Override
        public void run() {
            int rate = 22050;
            int buf = AudioTrack.getMinBufferSize(rate, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            AudioTrack track;
            try {
                track = new AudioTrack(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build(),
                        new AudioFormat.Builder().setSampleRate(rate).setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build(),
                        Math.max(buf, 4096), AudioTrack.MODE_STREAM, AudioManager.AUDIO_SESSION_ID_GENERATE);
                track.play();
            } catch (RuntimeException e) {
                return;
            }
            short[] chunk = new short[1024];
            Random rnd = new Random();
            double last = 0, t = 0;
            while (!stop) {
                for (int i = 0; i < chunk.length; i++) {
                    last = (last + 0.02 * (rnd.nextDouble() * 2 - 1)) / 1.02;
                    t += 1.0 / rate;
                    double swell = 0.6 + 0.4 * Math.sin(2 * Math.PI * t / 9.0);
                    chunk[i] = muted ? 0 : (short) (last * 9000 * swell);
                }
                track.write(chunk, 0, chunk.length);
            }
            track.stop();
            track.release();
        }
    }
}
