package com.fswebdev.fit90;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import com.fswebdev.fit90.content.Content;
import com.fswebdev.fit90.data.Settings;
import com.fswebdev.fit90.data.Vault;

/** App-wide singletons and the "lock after leaving the app" timer. */
public class App extends Application {

    /** How long the app may sit in the background before the lock screen comes back. */
    private static final long LOCK_AFTER_MS = 30_000;

    private static App instance;
    private int started;
    private long backgroundAt;
    private boolean locked;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        locked = Settings.of(this).lockEnabled();
        Notifications.createChannels(this);
        Reminders.schedule(this);
        registerActivityLifecycleCallbacks(new ActivityLifecycleCallbacks() {
            @Override
            public void onActivityStarted(Activity a) {
                if (started++ == 0 && backgroundAt > 0
                        && System.currentTimeMillis() - backgroundAt > LOCK_AFTER_MS
                        && Settings.of(App.this).lockEnabled()) {
                    locked = true;
                }
            }

            @Override
            public void onActivityStopped(Activity a) {
                if (--started == 0) backgroundAt = System.currentTimeMillis();
            }

            @Override
            public void onActivityCreated(Activity a, Bundle b) {
            }

            @Override
            public void onActivityResumed(Activity a) {
            }

            @Override
            public void onActivityPaused(Activity a) {
            }

            @Override
            public void onActivitySaveInstanceState(Activity a, Bundle b) {
            }

            @Override
            public void onActivityDestroyed(Activity a) {
            }
        });
    }

    public static App get() {
        return instance;
    }

    public Vault vault() {
        return Vault.of(this);
    }

    public Content content() {
        return Content.of(this);
    }

    public boolean needsUnlock() {
        return locked && Settings.of(this).lockEnabled();
    }

    public void unlocked() {
        locked = false;
    }

    public void lockNow() {
        locked = Settings.of(this).lockEnabled();
    }
}
