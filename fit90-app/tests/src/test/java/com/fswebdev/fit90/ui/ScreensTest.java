package com.fswebdev.fit90.ui;

import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import com.fswebdev.fit90.*;
import com.fswebdev.fit90.core.Profile;
import com.fswebdev.fit90.data.Progress;
import com.fswebdev.fit90.data.Settings;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.fitness.FitnessActivity;
import com.fswebdev.fit90.fitness.FitnessProfileActivity;
import com.fswebdev.fit90.session.Timeline;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;

/** Opens every screen (catching crashes) and saves full-length screenshots. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35, qualifiers = "w411dp-h891dp-xxhdpi", application = App.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class ScreensTest {

    static void shot(Activity a, String name) throws Exception {
        View root = a.getWindow().getDecorView();
        View body = root;
        try {
            Class<?> owner = a instanceof BaseActivity ? BaseActivity.class : a.getClass();
            java.lang.reflect.Field f = owner.getDeclaredField("content");
            f.setAccessible(true);
            Object c = f.get(a);
            if (c != null) body = (View) c;
        } catch (NoSuchFieldException ignored) {
            // Not a BaseActivity.
        }
        int w = 1233;
        body.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED));
        int h = Math.max(400, Math.min(body.getMeasuredHeight(), 12000));
        body.layout(0, 0, w, h);
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.eraseColor(Palette.of(a).bg);
        body.draw(new Canvas(b));
        File dir = new File(System.getProperty("screenshots.dir", "build/screenshots"));
        dir.mkdirs();
        FileOutputStream o = new FileOutputStream(new File(dir, name + ".png"));
        b.compress(Bitmap.CompressFormat.PNG, 100, o);
        o.close();
    }

    @Before
    public void setUp() {
        Settings s = Settings.of(RuntimeEnvironment.getApplication());
        s.setOnboarded(true);
        Profile p = new Profile();
        p.goals.add(Profile.LAST_LONGER);
        p.goals.add(Profile.LESS_ANXIETY);
        p.goals.add(Profile.FIRMER);
        p.onset = "recent";
        p.health.add(Profile.HIGH_BP);
        p.partner = "partner";
        p.minutes = 15;
        p.age = 24;
        p.heightCm = 175;
        p.weightKg = 70;
        p.targetKg = 72;
        p.bodyGoal = "muscular";
        Vault.of(RuntimeEnvironment.getApplication()).put("profile", p.toJson());
        Progress.record(RuntimeEnvironment.getApplication(), "kegel", "level", 5, 4, 4, null);
        Progress.record(RuntimeEnvironment.getApplication(), "breath", "slow", 3, 4, 4, null);
    }

    static <T extends Activity> T start(Class<T> cls, Intent i) {
        if (i == null) i = new Intent(RuntimeEnvironment.getApplication(), cls);
        return Robolectric.buildActivity(cls, i).setup().get();
    }

    @Test
    public void mainTabs() throws Exception {
        MainActivity m = start(MainActivity.class, null);
        shot(m, "01_today");
        LinearLayout nav = findNav(m);
        String[] names = {"02_programs", "03_library", "04_progress"};
        for (int i = 1; i < nav.getChildCount(); i++) {
            nav.getChildAt(i).performClick();
            shot(m, names[i - 1]);
        }
    }

    static LinearLayout findNav(MainActivity m) throws Exception {
        java.lang.reflect.Field f = MainActivity.class.getDeclaredField("nav");
        f.setAccessible(true);
        return (LinearLayout) f.get(m);
    }

    @Test
    public void onboarding() throws Exception {
        Settings.of(RuntimeEnvironment.getApplication()).setOnboarded(false);
        OnboardingActivity o = start(OnboardingActivity.class, null);
        shot(o, "10_onboarding_age");
    }

    @Test
    public void programScreens() throws Exception {
        android.content.Context c = RuntimeEnvironment.getApplication();
        shot(start(KegelActivity.class, null), "20_kegel");
        shot(start(BreathActivity.class, null), "21_breath");
        shot(start(AudioActivity.class, null), "22_audio");
        shot(start(PlanActivity.class, null), "23_plan");
        shot(start(CheckInActivity.class, null), "24_checkin");
        shot(start(JournalActivity.class, null), "25_journal");
        shot(start(PracticeLogActivity.class, null), "26_practice");
        shot(start(SettingsActivity.class, null), "27_settings");
        shot(start(ProgramActivity.class, new Intent(c, ProgramActivity.class).putExtra(ProgramActivity.EXTRA_ID, "confidence")), "28_program_confidence");
        shot(start(PdfActivity.class, null), "29_pdfs");
        shot(start(FitnessProfileActivity.class, null), "30_body_profile");
        shot(start(FitnessActivity.class, null), "31_fitness");
    }

    @Test
    public void player() throws Exception {
        android.content.Context c = RuntimeEnvironment.getApplication();
        Timeline t = Timeline.breath("slow", "Slow breathing", 4, 0, 6, 0, 0, 1);
        Intent i = new Intent(c, PlayerActivity.class).putExtra(PlayerActivity.EXTRA_TIMELINE, t.toJson())
                .putExtra(PlayerActivity.EXTRA_KIND, "breath").putExtra(PlayerActivity.EXTRA_REF, "slow");
        PlayerActivity p = start(PlayerActivity.class, i);
        shot(p, "40_player");
        assertTrue(true);
    }
}
