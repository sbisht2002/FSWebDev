package com.fswebdev.fit90;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.view.View;

/** Circular progress ring with a value label in the middle. */
public class RingView extends View {

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF bounds = new RectF();
    private float progress;
    private String value = "";
    private String caption = "";

    public RingView(Context context, int trackColor, int arcColor, int textColor, int subColor) {
        super(context);
        float d = getResources().getDisplayMetrics().density;
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(14 * d);
        track.setColor(trackColor);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(14 * d);
        arc.setStrokeCap(Paint.Cap.ROUND);
        arc.setColor(arcColor);
        big.setColor(textColor);
        big.setTextAlign(Paint.Align.CENTER);
        big.setTextSize(36 * getResources().getDisplayMetrics().scaledDensity);
        big.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        small.setColor(subColor);
        small.setTextAlign(Paint.Align.CENTER);
        small.setTextSize(14 * getResources().getDisplayMetrics().scaledDensity);
    }

    public void set(float progress, String value, String caption) {
        this.progress = Math.max(0f, Math.min(1f, progress));
        this.value = value;
        this.caption = caption;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float pad = track.getStrokeWidth() / 2f + 2;
        float size = Math.min(getWidth(), getHeight());
        float left = (getWidth() - size) / 2f;
        float top = (getHeight() - size) / 2f;
        bounds.set(left + pad, top + pad, left + size - pad, top + size - pad);
        canvas.drawArc(bounds, 0, 360, false, track);
        if (progress > 0) {
            canvas.drawArc(bounds, -90, 360 * progress, false, arc);
        }
        float cy = getHeight() / 2f;
        canvas.drawText(value, getWidth() / 2f, cy + big.getTextSize() * 0.25f, big);
        canvas.drawText(caption, getWidth() / 2f, cy + big.getTextSize() * 0.25f + small.getTextSize() * 1.6f, small);
    }
}
