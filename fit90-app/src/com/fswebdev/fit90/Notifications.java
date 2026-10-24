package com.fswebdev.fit90;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/** Notification channels and the neutral daily reminder. Wording is never explicit. */
public final class Notifications {

    public static final String CHANNEL_REMINDERS = "reminders";
    public static final String CHANNEL_PLAYER = "player";
    public static final int ID_REMINDER = 1;
    public static final int ID_PLAYER = 2;

    private Notifications() {
    }

    public static void createChannels(Context c) {
        if (Build.VERSION.SDK_INT < 26) return;
        NotificationManager nm = c.getSystemService(NotificationManager.class);
        NotificationChannel reminders = new NotificationChannel(CHANNEL_REMINDERS,
                c.getString(R.string.channel_reminders), NotificationManager.IMPORTANCE_DEFAULT);
        reminders.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        nm.createNotificationChannel(reminders);
        NotificationChannel player = new NotificationChannel(CHANNEL_PLAYER,
                c.getString(R.string.channel_player), NotificationManager.IMPORTANCE_LOW);
        player.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
        player.setShowBadge(false);
        nm.createNotificationChannel(player);
    }

    @SuppressWarnings("deprecation")
    public static Notification.Builder builder(Context c, String channel) {
        return Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(c, channel) : new Notification.Builder(c);
    }

    public static void showReminder(Context c, int minutes) {
        Intent open = new Intent(c, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent pi = PendingIntent.getActivity(c, 0, open,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        String text = c.getString(R.string.reminder_text, minutes);
        Notification n = builder(c, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_today)
                .setContentTitle(c.getString(R.string.reminder_title))
                .setContentText(text)
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setVisibility(Notification.VISIBILITY_PRIVATE)
                .setPublicVersion(builder(c, CHANNEL_REMINDERS)
                        .setSmallIcon(R.drawable.ic_today)
                        .setContentTitle(c.getString(R.string.reminder_title))
                        .build())
                .build();
        try {
            c.getSystemService(NotificationManager.class).notify(ID_REMINDER, n);
        } catch (SecurityException e) {
            // Notification permission was revoked; nothing to do.
        }
    }
}
