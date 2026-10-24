package com.fswebdev.fit90;

import android.os.Bundle;

import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

/**
 * AI coach. Hidden unless the APK is built with AI_COACH_ENABLED=true and a deployed
 * proxy URL; the streaming chat client lands with the coach proxy.
 */
public class ChatActivity extends BaseActivity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        screen(BuildConfig.COACH_NAME, true);
        content.addView(ui.text(getString(R.string.coach_unavailable), Ui.BODY));
    }
}
