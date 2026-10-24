package com.fswebdev.fit90.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

/**
 * Small view-building kit so every screen shares the same type scale, spacing,
 * rounded cards and large tap targets. All sizes are in dp/sp so system font
 * scaling (dynamic type) works everywhere.
 */
public final class Ui {

    public static final int DISPLAY = 0, TITLE = 1, HEADING = 2, SUBHEAD = 3, BODY = 4,
            SMALL = 5, CAPTION = 6, OVERLINE = 7, LABEL = 8, NUMBER = 9;
    public static final int PRIMARY = 0, SECONDARY = 1, GHOST = 2, DANGER = 3, OUTLINE = 4;

    public interface IntCallback {
        void on(int value);
    }

    public final Context c;
    public final Palette p;
    private final float density;

    public Ui(Context context, Palette palette) {
        c = context;
        p = palette;
        density = context.getResources().getDisplayMetrics().density;
    }

    public int dp(float v) {
        return Math.round(v * density);
    }

    // ------------------------------------------------------------------ text

    public TextView text(CharSequence s, int style) {
        return text(s, style, style == CAPTION || style == OVERLINE || style == SMALL ? p.sub : p.text);
    }

    public TextView text(CharSequence s, int style, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTextColor(color);
        t.setLinkTextColor(p.accent);
        float size;
        boolean medium = false;
        switch (style) {
            case DISPLAY: size = 30; medium = true; break;
            case TITLE: size = 23; medium = true; break;
            case HEADING: size = 18; medium = true; break;
            case SUBHEAD: size = 16; medium = true; break;
            case SMALL: size = 14; break;
            case CAPTION: size = 13; break;
            case OVERLINE: size = 12; medium = true; break;
            case LABEL: size = 15; medium = true; break;
            case NUMBER: size = 26; medium = true; break;
            default: size = 16;
        }
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
        t.setTypeface(Typeface.create(medium ? "sans-serif-medium" : "sans-serif", Typeface.NORMAL));
        if (style == BODY || style == SMALL) t.setLineSpacing(0, 1.25f);
        if (style == OVERLINE) {
            t.setLetterSpacing(0.08f);
            t.setAllCaps(true);
        }
        if ((style == DISPLAY || style == TITLE || style == HEADING) && android.os.Build.VERSION.SDK_INT >= 28) {
            t.setAccessibilityHeading(true);
        }
        t.setLayoutParams(wrap());
        return t;
    }

    // ---------------------------------------------------------------- layout

    public LinearLayout.LayoutParams wrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public LinearLayout.LayoutParams fill() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    public LinearLayout.LayoutParams weight(float w) {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, w);
    }

    public LinearLayout.LayoutParams margins(LinearLayout.LayoutParams lp, int l, int t, int r, int b) {
        lp.setMargins(dp(l), dp(t), dp(r), dp(b));
        return lp;
    }

    public LinearLayout column() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.VERTICAL);
        l.setLayoutParams(fill());
        return l;
    }

    public LinearLayout row() {
        LinearLayout l = new LinearLayout(c);
        l.setOrientation(LinearLayout.HORIZONTAL);
        l.setGravity(Gravity.CENTER_VERTICAL);
        l.setLayoutParams(fill());
        return l;
    }

    public View space(int dpHeight) {
        View v = new View(c);
        v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(dpHeight)));
        return v;
    }

    public View divider() {
        View v = new View(c);
        v.setBackgroundColor(p.line);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        v.setLayoutParams(lp);
        return v;
    }

    public GradientDrawable round(int color, float radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    public GradientDrawable outline(int fill, float radiusDp, int stroke, float strokeDp) {
        GradientDrawable g = round(fill, radiusDp);
        g.setStroke(Math.max(1, dp(strokeDp)), stroke);
        return g;
    }

    public GradientDrawable circle(int color) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(color);
        return g;
    }

    /** Background with a touch ripple clipped to the rounded shape. */
    public Drawable ripple(Drawable content, float radiusDp) {
        int rippleColor = p.dark ? 0x33FFFFFF : 0x1F000000;
        return new RippleDrawable(ColorStateList.valueOf(rippleColor), content, round(0xFF000000, radiusDp));
    }

    public LinearLayout card() {
        return card(p.card);
    }

    public LinearLayout card(int bg) {
        LinearLayout l = column();
        l.setBackground(round(bg, 20));
        l.setPadding(dp(18), dp(18), dp(18), dp(18));
        l.setLayoutParams(margins(fill(), 0, 0, 0, 12));
        return l;
    }

    /** Makes a card (or any view) tappable with ripple feedback and a button role. */
    public <T extends View> T clickable(T v, int bg, float radius, View.OnClickListener l, CharSequence desc) {
        v.setBackground(ripple(round(bg, radius), radius));
        v.setOnClickListener(l);
        v.setClickable(true);
        v.setFocusable(true);
        asButton(v, desc);
        return v;
    }

    public void asButton(View v, final CharSequence desc) {
        if (desc != null) v.setContentDescription(desc);
        v.setAccessibilityDelegate(new View.AccessibilityDelegate() {
            @Override
            public void onInitializeAccessibilityNodeInfo(View host, AccessibilityNodeInfo info) {
                super.onInitializeAccessibilityNodeInfo(host, info);
                info.setClassName(Button.class.getName());
            }
        });
    }

    // -------------------------------------------------------------- controls

    public TextView button(CharSequence label, int kind, View.OnClickListener l) {
        int bg, fg;
        switch (kind) {
            case SECONDARY: bg = p.accentSoft; fg = p.accent; break;
            case GHOST: bg = 0x00000000; fg = p.accent; break;
            case DANGER: bg = p.dangerSoft; fg = p.danger; break;
            case OUTLINE: bg = p.card; fg = p.text; break;
            default: bg = p.accent; fg = p.onAccent;
        }
        TextView t = text(label, LABEL, fg);
        t.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        t.setGravity(Gravity.CENTER);
        t.setMinHeight(dp(52));
        t.setPadding(dp(18), dp(12), dp(18), dp(12));
        Drawable shape = kind == OUTLINE ? outline(bg, 16, p.line, 1.5f) : round(bg, 16);
        t.setBackground(ripple(shape, 16));
        t.setOnClickListener(l);
        t.setClickable(true);
        t.setFocusable(true);
        asButton(t, null);
        t.setLayoutParams(margins(fill(), 0, 4, 0, 4));
        return t;
    }

    public TextView chip(CharSequence label, int fg, int bg) {
        TextView t = text(label, CAPTION, fg);
        t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setBackground(round(bg, 12));
        t.setPadding(dp(10), dp(4), dp(10), dp(4));
        return t;
    }

    public ImageView icon(int res, int color, int sizeDp) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setImageTintList(ColorStateList.valueOf(color));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        v.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return v;
    }

    /** Round icon "badge" used at the start of rows and program cards. */
    public FrameLayout iconBadge(int res, int fg, int bg, int sizeDp) {
        FrameLayout f = new FrameLayout(c);
        f.setBackground(circle(bg));
        ImageView i = icon(res, fg, Math.round(sizeDp * 0.5f));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(dp(sizeDp * 0.5f), dp(sizeDp * 0.5f), Gravity.CENTER);
        f.addView(i, lp);
        f.setLayoutParams(new LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp)));
        f.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        return f;
    }

    /** 48dp touch target holding a 24dp icon. */
    public ImageView iconButton(int res, int color, CharSequence desc, View.OnClickListener l) {
        ImageView v = new ImageView(c);
        v.setImageResource(res);
        v.setImageTintList(ColorStateList.valueOf(color));
        v.setScaleType(ImageView.ScaleType.CENTER);
        v.setPadding(dp(12), dp(12), dp(12), dp(12));
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(48), dp(48)));
        v.setBackground(ripple(round(0x00000000, 24), 24));
        v.setOnClickListener(l);
        v.setClickable(true);
        v.setFocusable(true);
        asButton(v, desc);
        return v;
    }

    /**
     * A tappable row: optional icon badge, title, subtitle, optional trailing text
     * and a chevron. Returns the row so callers can add it anywhere.
     */
    public LinearLayout listRow(int iconRes, int[] tone, CharSequence title, CharSequence subtitle,
                                CharSequence trailing, View.OnClickListener l) {
        LinearLayout row = row();
        row.setPadding(dp(14), dp(14), dp(10), dp(14));
        row.setMinimumHeight(dp(64));
        if (iconRes != 0) {
            FrameLayout badge = iconBadge(iconRes, tone[0], tone[1], 40);
            row.addView(badge);
            row.addView(space(0), new LinearLayout.LayoutParams(dp(14), 1));
        }
        LinearLayout texts = column();
        TextView t = text(title, SUBHEAD);
        texts.addView(t);
        if (subtitle != null && subtitle.length() > 0) {
            TextView s = text(subtitle, SMALL);
            s.setPadding(0, dp(2), 0, 0);
            texts.addView(s);
        }
        row.addView(texts, weight(1));
        if (trailing != null) {
            TextView tr = text(trailing, SMALL, p.sub);
            tr.setPadding(dp(8), 0, dp(4), 0);
            row.addView(tr);
        }
        if (l != null) {
            row.addView(icon(com.fswebdev.fit90.R.drawable.ic_chevron, p.faint, 20));
            clickable(row, p.card, 18, l, null);
        } else {
            row.setBackground(round(p.card, 18));
        }
        row.setLayoutParams(margins(fill(), 0, 0, 0, 8));
        return row;
    }

    public LinearLayout toggleRow(CharSequence title, CharSequence subtitle, boolean on,
                                  CompoundButton.OnCheckedChangeListener l) {
        LinearLayout row = row();
        row.setPadding(dp(16), dp(12), dp(12), dp(12));
        row.setMinimumHeight(dp(60));
        LinearLayout texts = column();
        texts.addView(text(title, SUBHEAD));
        if (subtitle != null) {
            TextView s = text(subtitle, SMALL);
            s.setPadding(0, dp(2), 0, 0);
            texts.addView(s);
        }
        row.addView(texts, weight(1));
        final Switch sw = new Switch(c);
        sw.setChecked(on);
        sw.setThumbTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{p.accent, p.dark ? 0xFFBDB5AD : 0xFFFFFFFF}));
        sw.setTrackTintList(new ColorStateList(new int[][]{{android.R.attr.state_checked}, {}},
                new int[]{p.accent, p.faint}));
        sw.setContentDescription(title);
        sw.setOnCheckedChangeListener(l);
        row.addView(sw);
        row.setBackground(ripple(round(p.card, 18), 18));
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sw.toggle();
            }
        });
        row.setLayoutParams(margins(fill(), 0, 0, 0, 8));
        return row;
    }

    /** Labelled slider; {@code format} turns the value into display text. */
    public LinearLayout slider(CharSequence title, final int min, int max, int value, final Formatter format,
                               final IntCallback onChange) {
        LinearLayout box = card();
        LinearLayout head = row();
        head.addView(text(title, SUBHEAD), weight(1));
        final TextView val = text(format.format(value), LABEL, p.accent);
        head.addView(val);
        box.addView(head);
        SeekBar bar = new SeekBar(c);
        bar.setMax(max - min);
        bar.setProgress(value - min);
        bar.setProgressTintList(ColorStateList.valueOf(p.accent));
        bar.setThumbTintList(ColorStateList.valueOf(p.accent));
        bar.setProgressBackgroundTintList(ColorStateList.valueOf(p.line));
        bar.setContentDescription(title);
        bar.setPadding(dp(8), dp(16), dp(8), dp(8));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar s, int progress, boolean fromUser) {
                val.setText(format.format(progress + min));
                if (onChange != null) onChange.on(progress + min);
            }

            @Override
            public void onStartTrackingTouch(SeekBar s) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar s) {
            }
        });
        box.addView(bar, fill());
        return box;
    }

    public interface Formatter {
        String format(int value);
    }

    /** A pill-shaped single-choice control. */
    public LinearLayout segmented(final CharSequence[] labels, int selected, final IntCallback onSelect) {
        LinearLayout seg = row();
        seg.setBackground(round(p.cardAlt, 16));
        seg.setPadding(dp(4), dp(4), dp(4), dp(4));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            boolean on = i == selected;
            TextView t = text(labels[i], LABEL, on ? p.text : p.sub);
            t.setGravity(Gravity.CENTER);
            t.setMinHeight(dp(44));
            t.setPadding(dp(4), dp(8), dp(4), dp(8));
            t.setMaxLines(2);
            t.setEllipsize(TextUtils.TruncateAt.END);
            if (on) t.setBackground(round(p.card, 13));
            t.setSelected(on);
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onSelect.on(index);
                }
            });
            asButton(t, labels[i] + (on ? ", selected" : ""));
            seg.addView(t, weight(1));
        }
        seg.setLayoutParams(margins(fill(), 0, 0, 0, 12));
        return seg;
    }

    public View progress(float fraction, int color, int track) {
        float f = Math.max(0f, Math.min(1f, fraction));
        FrameLayout bar = new FrameLayout(c);
        bar.setBackground(round(track, 4));
        LinearLayout inner = new LinearLayout(c);
        View fill = new View(c);
        fill.setBackground(round(color, 4));
        inner.addView(fill, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, f));
        inner.addView(new View(c), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1 - f));
        bar.addView(inner, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        bar.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(8)));
        bar.setContentDescription(Math.round(f * 100) + "%");
        return bar;
    }

    /** Small "stat" block: big value over a caption. */
    public LinearLayout stat(CharSequence value, CharSequence label) {
        LinearLayout box = column();
        box.addView(text(value, NUMBER));
        box.addView(text(label, CAPTION));
        return box;
    }

    /** Info/notice card with a coloured left accent; used for safety notes. */
    public LinearLayout notice(int iconRes, int[] tone, CharSequence title, CharSequence body) {
        LinearLayout card = card(tone[1]);
        LinearLayout head = row();
        head.addView(icon(iconRes, tone[0], 22));
        TextView t = text(title, SUBHEAD, p.text);
        t.setPadding(dp(10), 0, 0, 0);
        head.addView(t, weight(1));
        card.addView(head);
        if (body != null) {
            TextView b = text(body, SMALL, p.text);
            b.setPadding(0, dp(8), 0, 0);
            card.addView(b);
        }
        return card;
    }
}
