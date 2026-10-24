package com.fswebdev.fit90.core;

import java.util.Calendar;
import java.util.TimeZone;

/** Local calendar days as integers (days since 1970-01-01 in the phone's time zone). */
public final class Days {

    private Days() {
    }

    /** Overridable clock for tests. */
    public static long nowOverride = 0;

    public static long now() {
        return nowOverride != 0 ? nowOverride : System.currentTimeMillis();
    }

    public static int today() {
        return dayOf(now());
    }

    public static int dayOf(long millis) {
        return (int) Math.floor((millis + TimeZone.getDefault().getOffset(millis)) / 86400000.0);
    }

    /** Monday-based week index, so all days of one Monday–Sunday week share a value. */
    public static int weekOf(int day) {
        // 1970-01-01 was a Thursday; shift so weeks start on Monday.
        return (int) Math.floor((day + 3) / 7.0);
    }

    /** 0 = Monday ... 6 = Sunday. */
    public static int weekday(int day) {
        return ((day + 3) % 7 + 7) % 7;
    }

    public static Calendar calendar(int day) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(day * 86400000L);
        c.setTimeZone(TimeZone.getTimeZone("UTC"));
        Calendar local = Calendar.getInstance();
        local.clear();
        local.set(c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        return local;
    }
}
