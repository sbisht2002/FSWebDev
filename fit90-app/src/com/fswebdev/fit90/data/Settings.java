package com.fswebdev.fit90.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.UUID;

/**
 * Non-sensitive app settings (theme, sound, reminder time...). Anything personal —
 * quiz answers, logs, journal, chat — lives in the encrypted {@link Vault} instead.
 */
public final class Settings {

    private static Settings instance;
    private final SharedPreferences prefs;

    private Settings(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences("settings", Context.MODE_PRIVATE);
    }

    public static synchronized Settings of(Context context) {
        if (instance == null) instance = new Settings(context);
        return instance;
    }

    /** Test hook: forget the cached instance after the preferences file was wiped. */
    public static synchronized void reset() {
        instance = null;
    }

    public void clear() {
        prefs.edit().clear().apply();
    }

    private String str(String k, String d) { return prefs.getString(k, d); }
    private boolean bool(String k, boolean d) { return prefs.getBoolean(k, d); }
    private int num(String k, int d) { return prefs.getInt(k, d); }

    // ---- Onboarding ----
    public boolean onboarded() { return bool("onboarded", false); }
    public void setOnboarded(boolean v) { prefs.edit().putBoolean("onboarded", v).apply(); }

    // ---- Appearance & accessibility ----
    /** "system", "light" or "dark". */
    public String theme() { return str("theme", "system"); }
    public void setTheme(String v) { prefs.edit().putString("theme", v).apply(); }
    public boolean reduceMotion() { return bool("reduce_motion", false); }
    public void setReduceMotion(boolean v) { prefs.edit().putBoolean("reduce_motion", v).apply(); }

    // ---- Session feedback ----
    public boolean voice() { return bool("voice", true); }
    public void setVoice(boolean v) { prefs.edit().putBoolean("voice", v).apply(); }
    public boolean sound() { return bool("sound", true); }
    public void setSound(boolean v) { prefs.edit().putBoolean("sound", v).apply(); }
    public boolean haptics() { return bool("haptics", true); }
    public void setHaptics(boolean v) { prefs.edit().putBoolean("haptics", v).apply(); }
    public boolean ambient() { return bool("ambient", false); }
    public void setAmbient(boolean v) { prefs.edit().putBoolean("ambient", v).apply(); }
    public boolean captions() { return bool("captions", true); }
    public void setCaptions(boolean v) { prefs.edit().putBoolean("captions", v).apply(); }
    public int speechRatePercent() { return num("speech_rate", 90); }
    public void setSpeechRatePercent(int v) { prefs.edit().putInt("speech_rate", v).apply(); }

    // ---- Reminders ----
    public boolean remindersOn() { return bool("reminders", false); }
    public void setRemindersOn(boolean v) { prefs.edit().putBoolean("reminders", v).apply(); }
    /** Minutes after midnight. */
    public int reminderMinute() { return num("reminder_minute", 20 * 60); }
    public void setReminderMinute(int v) { prefs.edit().putInt("reminder_minute", v).apply(); }
    public int reminderSessionMinutes() { return num("reminder_session_minutes", 10); }
    public void setReminderSessionMinutes(int v) { prefs.edit().putInt("reminder_session_minutes", v).apply(); }

    // ---- Privacy ----
    public boolean lockEnabled() { return bool("lock", false); }
    public void setLockEnabled(boolean v) { prefs.edit().putBoolean("lock", v).apply(); }
    public boolean biometricEnabled() { return bool("biometric", false); }
    public void setBiometricEnabled(boolean v) { prefs.edit().putBoolean("biometric", v).apply(); }
    /** PBKDF2 hash and salt of the PIN, base64. */
    public String pinHash() { return str("pin_hash", ""); }
    public String pinSalt() { return str("pin_salt", ""); }
    public void setPin(String hash, String salt) {
        prefs.edit().putString("pin_hash", hash).putString("pin_salt", salt).apply();
    }
    public int failedUnlocks() { return num("failed_unlocks", 0); }
    public void setFailedUnlocks(int v) { prefs.edit().putInt("failed_unlocks", v).apply(); }
    public boolean hideInRecents() { return bool("hide_recents", true); }
    public void setHideInRecents(boolean v) { prefs.edit().putBoolean("hide_recents", v).apply(); }
    public boolean discreet() { return bool("discreet", false); }
    public void setDiscreet(boolean v) { prefs.edit().putBoolean("discreet", v).apply(); }

    // ---- Coach ----
    public boolean coachOn() { return bool("coach", true); }
    public void setCoachOn(boolean v) { prefs.edit().putBoolean("coach", v).apply(); }

    /** Random ID used only to apply the coach's per-device daily message limit. */
    public String anonymousDeviceId() {
        String id = str("device_id", "");
        if (id.isEmpty()) {
            id = UUID.randomUUID().toString();
            prefs.edit().putString("device_id", id).apply();
        }
        return id;
    }

    /** Snapshot for "Export my data". */
    public java.util.Map<String, ?> all() {
        java.util.Map<String, Object> out = new java.util.TreeMap<String, Object>(prefs.getAll());
        out.remove("pin_hash");
        out.remove("pin_salt");
        return out;
    }
}
