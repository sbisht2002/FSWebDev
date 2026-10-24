package com.fswebdev.fit90;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TimePicker;

import com.fswebdev.fit90.core.RedFlags;
import com.fswebdev.fit90.data.Crypto;
import com.fswebdev.fit90.data.Settings;
import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.fitness.FitnessProfileActivity;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONObject;

import java.io.OutputStream;
import java.util.Map;

/** Profile, reminders, session feedback, appearance, privacy, data and legal. */
public class SettingsActivity extends BaseActivity {

    private static final int REQ_EXPORT = 7;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void section(int title) {
        content.addView(ui.space(10));
        content.addView(ui.text(getString(title), Ui.OVERLINE));
        content.addView(ui.space(6));
    }

    private void render() {
        screen(getString(R.string.settings), true);

        section(R.string.s_profile);
        content.addView(ui.listRow(R.drawable.ic_calendar, p.tone("accent"), getString(R.string.plan_title),
                getString(R.string.s_plan_sub), null, go(PlanActivity.class)));
        content.addView(ui.listRow(R.drawable.ic_refresh, p.tone("accent"), getString(R.string.retake_quiz),
                getString(R.string.s_retake_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this, OnboardingActivity.class).putExtra("retake", true));
                    }
                }));
        content.addView(ui.listRow(R.drawable.ic_dumbbell, p.tone("accent"), getString(R.string.s_body),
                getString(R.string.s_body_sub), null, go(FitnessProfileActivity.class)));

        section(R.string.s_reminders);
        content.addView(ui.toggleRow(getString(R.string.s_daily_reminder),
                getString(R.string.s_reminder_sub, Reminders.format(settings.reminderMinute())), settings.remindersOn(),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean on) {
                        settings.setRemindersOn(on);
                        Reminders.schedule(SettingsActivity.this);
                    }
                }));
        content.addView(ui.listRow(0, null, getString(R.string.s_reminder_time), Reminders.format(settings.reminderMinute()),
                null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new TimePickerDialog(SettingsActivity.this, new TimePickerDialog.OnTimeSetListener() {
                            @Override
                            public void onTimeSet(TimePicker t, int h, int m) {
                                settings.setReminderMinute(h * 60 + m);
                                Reminders.schedule(SettingsActivity.this);
                                render();
                            }
                        }, settings.reminderMinute() / 60, settings.reminderMinute() % 60,
                                android.text.format.DateFormat.is24HourFormat(SettingsActivity.this)).show();
                    }
                }));

        section(R.string.s_feedback);
        content.addView(toggle(R.string.voice, R.string.s_voice_sub, settings.voice(), 0));
        content.addView(toggle(R.string.sound, R.string.s_sound_sub, settings.sound(), 1));
        content.addView(toggle(R.string.vibration, R.string.s_haptics_sub, settings.haptics(), 2));
        content.addView(toggle(R.string.s_ambient, R.string.s_ambient_sub, settings.ambient(), 3));
        content.addView(toggle(R.string.s_captions, R.string.s_captions_sub, settings.captions(), 4));

        section(R.string.s_appearance);
        final String[] modes = {"system", "light", "dark"};
        int sel = 0;
        for (int i = 0; i < modes.length; i++) if (modes[i].equals(settings.theme())) sel = i;
        content.addView(ui.segmented(getResources().getStringArray(R.array.themes), sel, new Ui.IntCallback() {
            @Override
            public void on(int v) {
                settings.setTheme(modes[v]);
                recreate();
            }
        }));
        content.addView(toggle(R.string.s_reduce_motion, R.string.s_reduce_motion_sub, settings.reduceMotion(), 5));

        section(R.string.s_privacy);
        content.addView(ui.toggleRow(getString(R.string.s_lock), getString(R.string.s_lock_sub), settings.lockEnabled(),
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean on) {
                        if (on) setPin();
                        else {
                            settings.setLockEnabled(false);
                            settings.setBiometricEnabled(false);
                        }
                    }
                }));
        if (settings.lockEnabled() && android.os.Build.VERSION.SDK_INT >= 28) {
            content.addView(toggle(R.string.s_biometric, R.string.s_biometric_sub, settings.biometricEnabled(), 6));
        }
        content.addView(toggle(R.string.s_hide_recents, R.string.s_hide_recents_sub, settings.hideInRecents(), 7));
        content.addView(ui.toggleRow(getString(R.string.s_discreet), getString(R.string.s_discreet_sub, getString(R.string.discreet_app_name)),
                settings.discreet(), new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton b, boolean on) {
                        setDiscreet(on);
                    }
                }));

        section(R.string.s_data);
        content.addView(ui.listRow(R.drawable.ic_download, p.tone("blue"), getString(R.string.s_export),
                getString(R.string.s_export_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                                .setType("application/json").putExtra(Intent.EXTRA_TITLE, "fit90-export.json");
                        startActivityForResult(i, REQ_EXPORT);
                    }
                }));
        content.addView(ui.listRow(R.drawable.ic_trash, p.tone("danger"), getString(R.string.s_delete),
                getString(R.string.s_delete_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmDelete();
                    }
                }));

        section(R.string.s_help);
        content.addView(ui.listRow(R.drawable.ic_info, p.tone("blue"), getString(R.string.s_doctor),
                getString(R.string.s_doctor_sub), null, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        open(ArticleActivity.class, ArticleActivity.EXTRA_ID, "sa-01");
                    }
                }));
        content.addView(ui.listRow(R.drawable.ic_shield, p.tone("accent"), getString(R.string.s_disclaimer), null, null,
                doc("legal/disclaimer.md")));
        content.addView(ui.listRow(R.drawable.ic_lock, p.tone("accent"), getString(R.string.s_privacy_policy), null, null,
                doc("legal/privacy.md")));
        content.addView(ui.listRow(R.drawable.ic_doc, p.tone("accent"), getString(R.string.s_terms), null, null,
                doc("legal/terms.md")));
        content.addView(ui.listRow(R.drawable.ic_external, p.tone("accent"), getString(R.string.further_reading), null, null,
                doc("further-reading.md")));
        if (!RedFlags.evaluate(Plans.profile(this)).isEmpty()) {
            content.addView(ui.space(8));
            OnboardingActivity.addRedFlagCards(this, ui, content, RedFlags.evaluate(Plans.profile(this)));
        }
        content.addView(ui.text(getString(R.string.s_version, BuildConfig.VERSION_NAME,
                com.fswebdev.fit90.content.Content.of(this).version()), Ui.CAPTION));
    }

    private View.OnClickListener go(final Class<?> cls) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(cls);
            }
        };
    }

    private View.OnClickListener doc(final String path) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                open(ArticleActivity.class, ArticleActivity.EXTRA_DOC, path);
            }
        };
    }

    private View toggle(int title, int sub, boolean on, final int which) {
        return ui.toggleRow(getString(title), getString(sub), on, new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean v) {
                Settings s = settings;
                switch (which) {
                    case 0: s.setVoice(v); break;
                    case 1: s.setSound(v); break;
                    case 2: s.setHaptics(v); break;
                    case 3: s.setAmbient(v); break;
                    case 4: s.setCaptions(v); break;
                    case 5: s.setReduceMotion(v); break;
                    case 6: s.setBiometricEnabled(v); break;
                    case 7: s.setHideInRecents(v); applyPrivacy(); break;
                    default:
                }
            }
        });
    }

    private void setPin() {
        final EditText pin = new EditText(this);
        pin.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        pin.setHint(R.string.pin_hint);
        pin.setPadding(ui.dp(20), ui.dp(16), ui.dp(20), ui.dp(16));
        new AlertDialog.Builder(this).setTitle(R.string.set_pin).setView(pin)
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        render();
                    }
                })
                .setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        String v = pin.getText().toString();
                        if (v.length() < 4) {
                            toast(getString(R.string.pin_too_short));
                            render();
                            return;
                        }
                        String salt = Crypto.newSalt();
                        settings.setPin(Crypto.hashPin(v, salt), salt);
                        settings.setLockEnabled(true);
                        App.get().unlocked();
                        render();
                    }
                }).show();
    }

    private void setDiscreet(boolean on) {
        settings.setDiscreet(on);
        PackageManager pm = getPackageManager();
        pm.setComponentEnabledSetting(new ComponentName(this, getPackageName() + ".LauncherDiscreet"),
                on ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED : PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
        pm.setComponentEnabledSetting(new ComponentName(this, getPackageName() + ".Launcher"),
                on ? PackageManager.COMPONENT_ENABLED_STATE_DISABLED : PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
        toast(getString(on ? R.string.discreet_on : R.string.discreet_off));
    }

    @Override
    protected void onActivityResult(int req, int result, Intent data) {
        super.onActivityResult(req, result, data);
        if (req != REQ_EXPORT || result != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri uri = data.getData();
            JSONObject out = Vault.of(this).export();
            out.put("exported_at", System.currentTimeMillis());
            out.put("app_version", BuildConfig.VERSION_NAME);
            JSONObject s = new JSONObject();
            for (Map.Entry<String, ?> e : settings.all().entrySet()) s.put(e.getKey(), e.getValue());
            out.put("settings", s);
            JSONObject fit = new JSONObject();
            for (Map.Entry<String, ?> e : getSharedPreferences("fit90", MODE_PRIVATE).getAll().entrySet()) {
                fit.put(e.getKey(), e.getValue());
            }
            out.put("fitness", fit);
            OutputStream os = getContentResolver().openOutputStream(uri);
            os.write(out.toString(2).getBytes("UTF-8"));
            os.close();
            toast(getString(R.string.exported));
        } catch (Exception e) {
            toast(getString(R.string.export_failed));
        }
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this).setTitle(R.string.s_delete)
                .setMessage(R.string.delete_everything_body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete_everything, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        deleteEverything();
                    }
                }).show();
    }

    private void deleteEverything() {
        stopService(new Intent(this, com.fswebdev.fit90.session.PlayerService.class));
        if (settings.discreet()) setDiscreet(false);
        settings.setRemindersOn(false);
        Reminders.schedule(this);
        Vault.wipe(this);
        getSharedPreferences("fit90", MODE_PRIVATE).edit().clear().commit();
        settings.clear();
        Settings.reset();
        App.get().unlocked();
        startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        finish();
    }
}
