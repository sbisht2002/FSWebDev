package com.fswebdev.fit90;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.fswebdev.fit90.data.Settings;

import java.util.Calendar;

/** Schedules the daily reminder at the user's chosen time (inexact, battery friendly). */
public final class Reminders {

    private Reminders() {
    }

    private static PendingIntent intent(Context c) {
        return PendingIntent.getBroadcast(c, 1, new Intent(c, ReminderReceiver.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    public static void schedule(Context c) {
        AlarmManager am = (AlarmManager) c.getSystemService(Context.ALARM_SERVICE);
        if (am == null) return;
        PendingIntent pi = intent(c);
        am.cancel(pi);
        Settings s = Settings.of(c);
        if (!s.remindersOn() || !s.onboarded()) return;
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextTrigger(System.currentTimeMillis(), s.reminderMinute()), pi);
    }

    /** Next time of day {@code minuteOfDay} strictly after {@code now}. */
    public static long nextTrigger(long now, int minuteOfDay) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(now);
        cal.set(Calendar.HOUR_OF_DAY, minuteOfDay / 60);
        cal.set(Calendar.MINUTE, minuteOfDay % 60);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        if (cal.getTimeInMillis() <= now) cal.add(Calendar.DAY_OF_MONTH, 1);
        return cal.getTimeInMillis();
    }

    public static String format(int minuteOfDay) {
        int h = minuteOfDay / 60;
        int m = minuteOfDay % 60;
        return String.format(java.util.Locale.getDefault(), "%02d:%02d", h, m);
    }
}
