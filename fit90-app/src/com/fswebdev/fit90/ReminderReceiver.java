package com.fswebdev.fit90;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.fswebdev.fit90.data.Settings;

/** Fires the neutral daily reminder, then schedules tomorrow's. */
public class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Settings s = Settings.of(context);
        if (s.remindersOn()) Notifications.showReminder(context, s.reminderSessionMinutes());
        Reminders.schedule(context);
    }
}
