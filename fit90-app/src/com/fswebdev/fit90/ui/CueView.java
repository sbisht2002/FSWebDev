package com.fswebdev.fit90.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.view.View;

import com.fswebdev.fit90.session.Timeline;

/**
 * The big animated cue: a soft circle that tightens on "squeeze", stays small on
 * "hold", opens on "release", and grows/shrinks with the breath. With reduced
 * motion it stays still and only the colour and progress ring change.
 */
public class CueView extends View {

    private final Palette p;
    private final boolean reduceMotion;
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private String kind = Timeline.PAUSE;
    private float progress;
    private float lastScale = 0.75f;

    public CueView(Context c, Palette p, boolean reduceMotion) {
        super(c);
        this.p = p;
        this.reduceMotion = reduceMotion;
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeCap(Paint.Cap.ROUND);
        track.setStyle(Paint.Style.STROKE);
        track.setColor(p.line);
        setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }

    public void set(String kind, float progress) {
        this.kind = kind;
        this.progress = progress;
        invalidate();
    }

    private float targetScale() {
        float t = progress;
        switch (kind) {
            case Timeline.SQUEEZE:
            case Timeline.FLICK:
                return lastScale + (0.45f - lastScale) * Math.min(1, t * 2);
            case Timeline.HOLD:
            case Timeline.HOLD_OUT:
                return 0.45f;
            case Timeline.HOLD_IN:
                return 1f;
            case Timeline.RELEASE:
            case Timeline.DROP:
                return 0.45f + 0.5f * ease(t);
            case Timeline.IN:
                return 0.45f + 0.55f * ease(t);
            case Timeline.OUT:
                return 1f - 0.55f * ease(t);
            default:
                return 0.75f;
        }
    }

    private static float ease(float x) {
        return x * x * (3 - 2 * x);
    }

    private int color() {
        switch (kind) {
            case Timeline.SQUEEZE:
            case Timeline.FLICK:
            case Timeline.HOLD:
                return p.warm;
            case Timeline.RELEASE:
            case Timeline.DROP:
            case Timeline.IN:
            case Timeline.OUT:
            case Timeline.HOLD_IN:
            case Timeline.HOLD_OUT:
                return p.blue;
            case Timeline.REST:
                return p.accent;
            default:
                return p.faint;
        }
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        float r = Math.min(w, h) / 2f * 0.92f;
        float cx = w / 2f, cy = h / 2f;
        float scale = reduceMotion ? 0.8f : targetScale();
        if (!Timeline.SQUEEZE.equals(kind) && !Timeline.FLICK.equals(kind)) lastScale = scale;
        int col = color();
        fill.setColor((col & 0x00FFFFFF) | 0x33000000);
        c.drawCircle(cx, cy, r * scale, fill);
        fill.setColor((col & 0x00FFFFFF) | 0x66000000);
        c.drawCircle(cx, cy, r * scale * 0.72f, fill);
        float stroke = r * 0.05f;
        track.setStrokeWidth(stroke);
        ring.setStrokeWidth(stroke);
        ring.setColor(col);
        rect.set(cx - r, cy - r, cx + r, cy + r);
        c.drawArc(rect, 0, 360, false, track);
        c.drawArc(rect, -90, 360 * progress, false, ring);
    }
}
