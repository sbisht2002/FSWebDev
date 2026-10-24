package com.fswebdev.fit90.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fswebdev.fit90.session.Timeline;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.TreeSet;

public class LogicTest {

    // ---- Kegel level-up rules ----

    @Test
    public void levelUpNeedsFiveSessionsAndEasyRatings() {
        List<Integer> easy = Arrays.asList(3, 4, 4, 4);
        assertFalse(Kegel.canLevelUp(1, 4, easy));
        assertTrue(Kegel.canLevelUp(1, 5, easy));
        assertFalse(Kegel.canLevelUp(1, 9, Arrays.asList(5, 5, 2, 3, 3)));
        assertFalse("no level 11", Kegel.canLevelUp(10, 20, easy));
        assertFalse("needs three ratings", Kegel.canLevelUp(2, 6, Arrays.asList(5, 5)));
    }

    @Test
    public void suggestEasierAfterThreeVeryHardSessions() {
        assertTrue(Kegel.suggestEasier(4, Arrays.asList(3, 1, 2, 1)));
        assertFalse(Kegel.suggestEasier(1, Arrays.asList(1, 1, 1)));
        assertFalse(Kegel.suggestEasier(4, Arrays.asList(1, 3, 2)));
    }

    // ---- XP, levels, streaks ----

    @Test
    public void levelCurveIsGentleAndCapsAt100() {
        assertEquals(1, Gamify.levelForXp(0));
        assertEquals(2, Gamify.levelForXp(40));
        assertEquals(2, Gamify.levelForXp(85));
        assertEquals(3, Gamify.levelForXp(86));
        assertEquals(100, Gamify.levelForXp(10_000_000));
        assertEquals(33066, Gamify.xpForLevel(100));
        assertEquals(20, Gamify.xpForSession(5));
        assertEquals(40, Gamify.xpForSession(60));
    }

    static TreeSet<Integer> days(int... d) {
        TreeSet<Integer> s = new TreeSet<Integer>();
        for (int x : d) s.add(x);
        return s;
    }

    @Test
    public void streakCountsAndTodayNeverBreaksIt() {
        assertEquals(3, Gamify.streak(days(1, 2, 3), 3).current);
        assertEquals(3, Gamify.streak(days(1, 2, 3), 4).current);
        assertEquals(0, Gamify.streak(days(1, 2, 3), 5).current);
    }

    @Test
    public void freezeCoversOneMissedDayAfterSevenInARow() {
        Gamify.Streak s = Gamify.streak(days(1, 2, 3, 4, 5, 6, 7, 9, 10), 10);
        assertEquals(9, s.current);
        assertEquals(Arrays.asList(8), s.frozenDays);
        assertEquals(0, s.freezes);
        Gamify.Streak broken = Gamify.streak(days(1, 2, 3, 4, 5, 6, 7, 10), 10);
        assertEquals(1, broken.current);
        assertEquals(7, broken.best);
    }

    @Test
    public void atMostTwoFreezesBanked() {
        TreeSet<Integer> d = new TreeSet<Integer>();
        for (int i = 1; i <= 28; i++) d.add(i);
        assertEquals(2, Gamify.streak(d, 28).freezes);
    }

    // ---- Red flags ----

    @Test
    public void redFlags() {
        Profile p = new Profile();
        p.goals.add(Profile.FIRMER);
        p.onset = "recent";
        p.health.add(Profile.DIABETES);
        assertEquals(Arrays.asList(RedFlags.ED_CARDIO, RedFlags.ED_RECENT), RedFlags.evaluate(p));
        Profile pe = new Profile();
        pe.goals.add(Profile.LAST_LONGER);
        pe.onset = "lifelong";
        pe.bother = "lot";
        assertEquals(Arrays.asList(RedFlags.LIFELONG_PE), RedFlags.evaluate(pe));
        pe.bother = "little";
        assertTrue(RedFlags.evaluate(pe).isEmpty());
        Profile chest = new Profile();
        chest.health.add(Profile.CHEST_PAIN);
        assertEquals(RedFlags.CHEST_PAIN, RedFlags.evaluate(chest).get(0));
    }

    // ---- Fitness targets ----

    static Profile body(int age, double h, double w, double target, String goal) {
        Profile p = new Profile();
        p.age = age;
        p.heightCm = h;
        p.weightKg = w;
        p.targetKg = target;
        p.bodyGoal = goal;
        p.activity = "light";
        return p;
    }

    @Test
    public void maintainAt70kg() {
        FitnessTargets t = FitnessTargets.compute(body(24, 175, 70, 0, "maintain"));
        assertEquals("maintain", t.mode);
        // BMR = 700 + 1093.75 - 120 + 5 = 1678.75; x1.375 = 2308
        assertEquals(2300, t.calories);
        assertEquals(110, t.protein);
        assertEquals(2500, t.waterMl);
        assertFalse(t.lowImpact);
    }

    @Test
    public void cuttingIsSafeAndNeverBelowBmr() {
        FitnessTargets t = FitnessTargets.compute(body(30, 170, 95, 80, "lean"));
        assertEquals("cut", t.mode);
        assertTrue(t.calories >= t.bmr);
        assertTrue(t.weeklyKg <= 0.75);
        assertTrue(t.weeksToTarget > 15);
        assertTrue(t.finisher);
        assertTrue("BMI 32.9 → joint-friendly", t.lowImpact);
        // protein from BMI-25 reference weight: 25*1.7^2*2.0 = 144.5 → 145
        assertEquals(145, t.protein);
    }

    @Test
    public void bulkingAddsVolume() {
        FitnessTargets t = FitnessTargets.compute(body(24, 180, 62, 70, "muscular"));
        assertEquals("gain", t.mode);
        assertEquals(1, t.extraSets);
        assertTrue(t.calories > t.tdee);
    }

    @Test
    public void neverCutWhenUnderweight() {
        assertEquals("maintain", FitnessTargets.compute(body(24, 180, 55, 0, "lean")).mode);
    }

    // ---- Script timelines ----

    @Test
    public void scriptMarkersAndRepeats() {
        Timeline t = Timeline.script("x", "X", "none",
                "Welcome.\n[pause 2]\n[repeat 3]\n[in 4] Breathe in\n[out 6]\n[/repeat]\n[chime]");
        assertEquals(1 + 1 + 6 + 1, t.steps.size());
        assertEquals(Timeline.SPEAK, t.steps.get(0).kind);
        assertEquals(4.0, t.steps.get(2).seconds, 0);
        assertEquals("Breathe in", t.steps.get(2).text);
        assertEquals(Timeline.CHIME, t.steps.get(8).kind);
        assertEquals(t.toJson(), Timeline.fromJson(t.toJson()).toJson());
    }

    @Test
    public void breathTimelineMatchesMinutes() {
        Timeline t = Timeline.breath("slow", "Slow", 4, 0, 6, 0, 0, 3);
        assertEquals(3, t.minutes());
    }
}
