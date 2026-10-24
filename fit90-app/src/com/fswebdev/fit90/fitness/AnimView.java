package com.fswebdev.fit90.fitness;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.SystemClock;
import android.view.View;

/** Draws and loops an {@link Anim}. Only animates while it is on screen. */
public class AnimView extends View {

    private static final float TORSO = 26, HEAD_OFFSET = 7, HEAD_R = 5.5f;
    private static final float UPPER_ARM = 13, FOREARM = 12, THIGH = 17, SHIN = 17;

    private final Anim anim;
    private final long start = SystemClock.uptimeMillis();
    private final Paint near = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint far = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint headFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint prop = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint propLine = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dumbbell = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pulse = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF rect = new RectF();
    private final Rect visible = new Rect();
    private final float[] pose = new float[11];
    private float scale, ox, oy;
    private float frozen = -1;

    public AnimView(Context context, String exerciseName, int ink, int faint, int surface, int background) {
        super(context);
        anim = Anim.forExercise(exerciseName);
        for (Paint p : new Paint[]{near, far, propLine}) {
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeCap(Paint.Cap.ROUND);
            p.setStrokeJoin(Paint.Join.ROUND);
        }
        near.setColor(ink);
        far.setColor(faint);
        headFill.setColor(surface);
        prop.setColor(0xFFDCDCD5);
        propLine.setColor(0xFFC4C4BC);
        dumbbell.setColor(0xFF9A5B22);
        dumbbell.setStyle(Paint.Style.FILL);
        bg.setColor(background);
    }

    /** Shows a fixed point of the movement (0 = start, 1 = end) instead of looping. */
    public void freeze(float at) {
        frozen = at;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight();
        rect.set(0, 0, w, h);
        float r = 14 * getResources().getDisplayMetrics().density;
        c.drawRoundRect(rect, r, r, bg);

        scale = Math.min(w / 100f, h / 108f) * 0.92f;
        ox = (w - 100 * scale) / 2f;
        oy = (h - 108 * scale) / 2f + 8 * scale;

        long t = SystemClock.uptimeMillis() - start;
        int cycle = 2 * (anim.moveMs + anim.holdMs);
        long ph = t % cycle;
        float f;
        if (ph < anim.holdMs) f = 0;
        else if (ph < anim.holdMs + anim.moveMs) f = ease((ph - anim.holdMs) / (float) anim.moveMs);
        else if (ph < 2 * anim.holdMs + anim.moveMs) f = 1;
        else f = 1 - ease((ph - 2 * anim.holdMs - anim.moveMs) / (float) anim.moveMs);
        if (frozen >= 0) f = frozen;
        for (int i = 0; i < pose.length; i++) pose[i] = anim.a[i] + (anim.b[i] - anim.a[i]) * f;

        drawProps(c);
        drawFigure(c, t);

        if (frozen < 0 && getGlobalVisibleRect(visible)) postInvalidateOnAnimation();
    }

    private static float ease(float x) {
        return x * x * (3 - 2 * x);
    }

    private float X(float x) {
        return ox + x * scale;
    }

    private float Y(float y) {
        return oy + y * scale;
    }

    private void drawProps(Canvas c) {
        propLine.setStrokeWidth(2.2f * scale);
        c.drawLine(X(4), Y(anim.floor), X(96), Y(anim.floor), propLine);
        for (float[] b : anim.boxes) {
            rect.set(X(b[0]), Y(b[1]), X(b[0] + b[2]), Y(Math.min(b[1] + b[3], anim.floor)));
            c.drawRoundRect(rect, 2 * scale, 2 * scale, prop);
        }
        if (anim.bar != null) {
            propLine.setStrokeWidth(2.6f * scale);
            c.drawLine(X(anim.bar[0]), Y(anim.bar[2]), X(anim.bar[1]), Y(anim.bar[2]), propLine);
            c.drawLine(X(anim.bar[0]), Y(anim.bar[2] - 6), X(anim.bar[0]), Y(anim.bar[2]), propLine);
            c.drawLine(X(anim.bar[1]), Y(anim.bar[2] - 6), X(anim.bar[1]), Y(anim.bar[2]), propLine);
        }
        if (anim.wall >= 0) {
            propLine.setStrokeWidth(2.6f * scale);
            c.drawLine(X(anim.wall), Y(-4), X(anim.wall), Y(anim.floor), propLine);
        }
    }

    private void drawFigure(Canvas c, long t) {
        float hx = pose[0], hy = pose[1];
        double ta = Math.toRadians(pose[2]);
        float tx = (float) Math.cos(ta), ty = (float) Math.sin(ta);
        float sx = hx + TORSO * tx, sy = hy + TORSO * ty;
        float headX = sx + HEAD_OFFSET * tx, headY = sy + HEAD_OFFSET * ty;
        float maxHeadY = anim.floor - HEAD_R - 0.5f;
        if (headY > maxHeadY) headY = maxHeadY;

        // In front view the shoulders and hips spread sideways.
        float shN = anim.front ? -6 : 0, shF = anim.front ? 6 : 0;
        float hpN = anim.front ? -4 : 0, hpF = anim.front ? 4 : 0;
        int elbowN = 1, elbowF = anim.front ? -1 : 1;
        int kneeN = anim.front ? 1 : -1, kneeF = -1;

        float[] armF = limb(sx + shF, sy + (anim.front ? 2 : 0), pose[5], pose[6], UPPER_ARM, FOREARM, elbowF);
        float[] legF = limb(hx + hpF, hy, pose[9], pose[10], THIGH, SHIN, kneeF);
        float[] armN = limb(sx + shN, sy + (anim.front ? 2 : 0), pose[3], pose[4], UPPER_ARM, FOREARM, elbowN);
        float[] legN = limb(hx + hpN, hy, pose[7], pose[8], THIGH, SHIN, kneeN);

        float stroke = 3.4f * scale;
        far.setStrokeWidth(stroke);
        near.setStrokeWidth(stroke);

        drawLimb(c, armF, far);
        drawLimb(c, legF, far);
        if (anim.front) {
            near.setStrokeWidth(stroke);
            c.drawLine(X(sx + shN), Y(sy + 2), X(sx + shF), Y(sy + 2), near);
            c.drawLine(X(hx + hpN), Y(hy), X(hx + hpF), Y(hy), near);
        }
        near.setStrokeWidth(3.8f * scale);
        c.drawLine(X(hx), Y(hy), X(sx), Y(sy), near);
        c.drawLine(X(sx), Y(sy), X(headX), Y(headY), near);
        near.setStrokeWidth(stroke);
        drawLimb(c, legN, near);
        drawLimb(c, armN, near);

        c.drawCircle(X(headX), Y(headY), HEAD_R * scale, headFill);
        c.drawCircle(X(headX), Y(headY), HEAD_R * scale, near);

        if (anim.db == Anim.DB_HANDS || anim.db == Anim.DB_NEAR) {
            drawDumbbell(c, armN[4], armN[5]);
            if (anim.db == Anim.DB_HANDS) drawDumbbell(c, armF[4], armF[5]);
        } else if (anim.db == Anim.DB_FEET) {
            drawDumbbell(c, (legN[4] + legF[4]) / 2, (legN[5] + legF[5]) / 2 + 2);
        }

        if (anim.pulse != Anim.NO_PULSE) {
            float period = anim.pulse == Anim.PULSE_FAST ? 600 : anim.pulse == Anim.PULSE_BREATH ? 4000 : 2400;
            float s = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * (t % (long) period) / period));
            float px, py, radius;
            if (anim.pulse == Anim.PULSE_BREATH) {
                px = hx + (sx - hx) * 0.35f;
                py = hy + (sy - hy) * 0.35f - 3;
                radius = 3 + 3 * s;
                pulse.setColor(0x552F7FE6);
            } else {
                px = hx;
                py = hy;
                radius = 2.5f + 3.5f * s;
                pulse.setColor(0x66C2416B);
            }
            c.drawCircle(X(px), Y(py), radius * scale, pulse);
        }
    }

    private void drawLimb(Canvas c, float[] l, Paint p) {
        c.drawLine(X(l[0]), Y(l[1]), X(l[2]), Y(l[3]), p);
        c.drawLine(X(l[2]), Y(l[3]), X(l[4]), Y(l[5]), p);
    }

    private void drawDumbbell(Canvas c, float x, float y) {
        rect.set(X(x - 4), Y(y - 0.8f), X(x + 4), Y(y + 0.8f));
        c.drawRect(rect, dumbbell);
        rect.set(X(x - 5.5f), Y(y - 2.8f), X(x - 3.2f), Y(y + 2.8f));
        c.drawRoundRect(rect, 0.6f * scale, 0.6f * scale, dumbbell);
        rect.set(X(x + 3.2f), Y(y - 2.8f), X(x + 5.5f), Y(y + 2.8f));
        c.drawRoundRect(rect, 0.6f * scale, 0.6f * scale, dumbbell);
    }

    /** Two-bone IK: returns {rootX, rootY, jointX, jointY, endX, endY}. */
    private static float[] limb(float ax, float ay, float bx, float by, float l1, float l2, int sign) {
        float dx = bx - ax, dy = by - ay;
        float d = (float) Math.sqrt(dx * dx + dy * dy);
        if (d < 0.001f) {
            dx = 0;
            dy = 1;
            d = 0.001f;
        }
        float ux = dx / d, uy = dy / d;
        float reach = l1 + l2;
        if (d >= reach) {
            return new float[]{ax, ay, ax + ux * l1, ay + uy * l1, ax + ux * reach, ay + uy * reach};
        }
        d = Math.max(d, Math.abs(l1 - l2) + 0.01f);
        float along = (l1 * l1 - l2 * l2 + d * d) / (2 * d);
        float off = (float) Math.sqrt(Math.max(0, l1 * l1 - along * along));
        float nx = -uy * sign, ny = ux * sign;
        return new float[]{ax, ay, ax + ux * along + nx * off, ay + uy * along + ny * off, bx, by};
    }
}
