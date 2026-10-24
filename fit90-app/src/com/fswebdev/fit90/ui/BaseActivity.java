package com.fswebdev.fit90.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Insets;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.fswebdev.fit90.App;
import com.fswebdev.fit90.LockActivity;
import com.fswebdev.fit90.R;
import com.fswebdev.fit90.data.Settings;

/**
 * Shared screen scaffolding: theme, system bars, edge-to-edge insets (including the
 * keyboard), "hide in app switcher", the app lock, and a header + scrolling body.
 */
public abstract class BaseActivity extends Activity {

    protected Palette p;
    protected Ui ui;
    protected Settings settings;
    protected FrameLayout root;
    protected LinearLayout column;
    protected LinearLayout header;
    protected ScrollView scroll;
    /** Where screens add their views. */
    protected LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        settings = Settings.of(this);
        p = Palette.of(this);
        setTheme(p.dark ? R.style.AppTheme_Dark : R.style.AppTheme_Light);
        super.onCreate(savedInstanceState);
        ui = new Ui(this, p);
        styleWindow();
    }

    @Override
    protected void onResume() {
        super.onResume();
        applyPrivacy();
        if (!(this instanceof LockActivity) && App.get() != null && App.get().needsUnlock()) {
            startActivity(new Intent(this, LockActivity.class).addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION));
        }
    }

    @SuppressWarnings("deprecation")
    private void styleWindow() {
        Window w = getWindow();
        w.setStatusBarColor(p.bg);
        w.setNavigationBarColor(p.bg);
        if (Build.VERSION.SDK_INT >= 30) {
            w.setDecorFitsSystemWindows(false);
            WindowInsetsController ctl = w.getInsetsController();
            if (ctl != null) {
                int light = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                ctl.setSystemBarsAppearance(p.dark ? 0 : light, light);
            }
        } else {
            int flags = 0;
            if (!p.dark) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
                if (Build.VERSION.SDK_INT >= 27) flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            w.getDecorView().setSystemUiVisibility(flags);
        }
    }

    /** Keeps screen content out of the recent-apps preview when the user asked for that. */
    protected void applyPrivacy() {
        boolean hide = settings.hideInRecents();
        if (Build.VERSION.SDK_INT >= 33) {
            setRecentsScreenshotEnabled(!hide);
        } else if (hide) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }
    }

    /** Wraps {@code body} in a full-screen root that pads itself for system bars and the keyboard. */
    protected FrameLayout frame(View body) {
        root = new FrameLayout(this);
        root.setBackgroundColor(p.bg);
        root.addView(body, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (Build.VERSION.SDK_INT >= 30) {
            root.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
                @Override
                public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                    Insets i = insets.getInsets(WindowInsets.Type.systemBars() | WindowInsets.Type.ime()
                            | WindowInsets.Type.displayCutout());
                    v.setPadding(i.left, i.top, i.right, i.bottom);
                    return WindowInsets.CONSUMED;
                }
            });
        } else {
            root.setFitsSystemWindows(true);
        }
        setContentView(root);
        return root;
    }

    /**
     * Standard screen: a header with an optional back button and title, then a
     * scrolling column ({@link #content}). Returns the header so actions can be added.
     */
    protected LinearLayout screen(CharSequence title, boolean back) {
        column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        header = ui.row();
        header.setPadding(ui.dp(back ? 4 : 20), ui.dp(6), ui.dp(8), ui.dp(2));
        header.setMinimumHeight(ui.dp(56));
        if (back) {
            header.addView(ui.iconButton(R.drawable.ic_back, p.text, getString(R.string.back),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            onBackPressed();
                        }
                    }));
        }
        TextView t = ui.text(title, Ui.HEADING);
        t.setSingleLine(false);
        t.setMaxLines(2);
        t.setPadding(ui.dp(back ? 4 : 0), 0, 0, 0);
        header.addView(t, ui.weight(1));
        column.addView(header);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setClipToPadding(false);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(32));
        scroll.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        column.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        frame(column);
        return header;
    }

    /** Adds a header action icon. */
    protected View action(int icon, CharSequence desc, View.OnClickListener l) {
        View v = ui.iconButton(icon, p.text, desc, l);
        header.addView(v);
        return v;
    }

    /** Bottom bar pinned under the scroll area (e.g. a primary button). */
    protected LinearLayout footer() {
        LinearLayout f = ui.column();
        f.setPadding(ui.dp(20), ui.dp(8), ui.dp(20), ui.dp(12));
        f.setBackgroundColor(p.bg);
        column.addView(f);
        return f;
    }

    protected void open(Class<?> cls) {
        startActivity(new Intent(this, cls));
    }

    protected void open(Class<?> cls, String key, String value) {
        startActivity(new Intent(this, cls).putExtra(key, value));
    }

    protected void toast(CharSequence s) {
        android.widget.Toast.makeText(this, s, android.widget.Toast.LENGTH_SHORT).show();
    }

    /** Centred empty-state text. */
    protected TextView empty(CharSequence s) {
        TextView t = ui.text(s, Ui.BODY, p.sub);
        t.setGravity(Gravity.CENTER);
        t.setPadding(ui.dp(16), ui.dp(24), ui.dp(16), ui.dp(24));
        t.setLayoutParams(ui.fill());
        return t;
    }

    /** Rebuild the screen (e.g. after the theme or data changed). */
    protected void refresh() {
        recreate();
    }
}
