package com.fswebdev.fit90.core;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;

/** XP, the 100-level curve, and streaks with freeze days. Pure functions over history. */
public final class Gamify {

    public static final int MAX_LEVEL = 100;
    public static final int MAX_FREEZES = 2;

    private Gamify() {
    }

    /** XP for a finished session: 10 base plus 2 per minute, capped at 40. */
    public static int xpForSession(int minutes) {
        return Math.min(40, 10 + 2 * Math.max(0, minutes));
    }

    /** XP needed to go from {@code level} to {@code level + 1}: 40, 46, 52 ... (gentle). */
    public static int xpToNext(int level) {
        return 40 + 6 * (level - 1);
    }

    /** Total XP needed to reach {@code level} (level 1 = 0). */
    public static int xpForLevel(int level) {
        int total = 0;
        for (int l = 1; l < level; l++) total += xpToNext(l);
        return total;
    }

    public static int levelForXp(int xp) {
        int level = 1;
        while (level < MAX_LEVEL && xp >= xpForLevel(level + 1)) level++;
        return level;
    }

    public static final class Streak {
        public int current;
        public int best;
        public int freezes;
        public final List<Integer> frozenDays = new ArrayList<Integer>();
    }

    /**
     * Walks day by day from the first active day to today. Active days add to the
     * streak; every 7 days of streak earns a freeze (max 2). A missed day uses a
     * freeze if one is banked, otherwise the streak resets. Today not being done yet
     * never breaks the streak.
     */
    public static Streak streak(SortedSet<Integer> activeDays, int today) {
        Streak s = new Streak();
        if (activeDays.isEmpty()) return s;
        for (int day = activeDays.first(); day <= today; day++) {
            if (activeDays.contains(day)) {
                s.current++;
                if (s.current % 7 == 0) s.freezes = Math.min(MAX_FREEZES, s.freezes + 1);
            } else if (day == today) {
                break;
            } else if (s.freezes > 0 && s.current > 0) {
                s.freezes--;
                s.frozenDays.add(day);
            } else {
                s.current = 0;
            }
            s.best = Math.max(s.best, s.current);
        }
        return s;
    }
}
