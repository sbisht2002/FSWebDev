package com.fswebdev.fit90;

import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.fswebdev.fit90.data.Crypto;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

/** PIN (and optional biometric) unlock. Shows nothing about the app's content. */
public class LockActivity extends BaseActivity {

    private TextView status;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        screen("", false);
        content.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(ui.space(60));
        content.addView(ui.iconBadge(R.drawable.ic_lock, p.accent, p.accentSoft, 64));
        content.addView(ui.space(16));
        TextView t = ui.text(getString(R.string.locked_title), Ui.TITLE);
        t.setGravity(Gravity.CENTER);
        content.addView(t, ui.fill());
        final EditText pin = new EditText(this);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setHint(R.string.pin_hint);
        pin.setGravity(Gravity.CENTER);
        pin.setTextSize(24);
        pin.setTextColor(p.text);
        pin.setHintTextColor(p.faint);
        pin.setBackground(ui.outline(p.card, 16, p.line, 1.5f));
        pin.setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14));
        content.addView(pin, ui.margins(ui.fill(), 24, 24, 24, 8));
        status = ui.text("", Ui.SMALL, p.danger);
        status.setGravity(Gravity.CENTER);
        content.addView(status, ui.fill());
        pin.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() >= 4 && check(s.toString())) unlock();
            }
        });
        content.addView(ui.button(getString(R.string.unlock), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (check(pin.getText().toString())) unlock();
                else {
                    settings.setFailedUnlocks(settings.failedUnlocks() + 1);
                    status.setText(R.string.wrong_pin);
                    pin.setText("");
                }
            }
        }), ui.margins(ui.fill(), 24, 8, 24, 0));
        if (settings.biometricEnabled() && Build.VERSION.SDK_INT >= 28) {
            content.addView(ui.button(getString(R.string.use_biometric), Ui.GHOST, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    biometric();
                }
            }), ui.margins(ui.fill(), 24, 0, 24, 0));
            biometric();
        }
    }

    private boolean check(String pin) {
        return Crypto.sameHash(Crypto.hashPin(pin, settings.pinSalt()), settings.pinHash());
    }

    private void unlock() {
        settings.setFailedUnlocks(0);
        App.get().unlocked();
        finish();
    }

    private void biometric() {
        if (Build.VERSION.SDK_INT < 28) return;
        new BiometricPrompt.Builder(this).setTitle(getString(R.string.unlock))
                .setNegativeButton(getString(R.string.use_pin), getMainExecutor(), new android.content.DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(android.content.DialogInterface d, int w) {
                    }
                }).build()
                .authenticate(new CancellationSignal(), getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) {
                        unlock();
                    }
                });
    }

    @Override
    public void onBackPressed() {
        moveTaskToBack(true);
    }
}
