package com.fswebdev.fit90;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import com.fswebdev.fit90.data.Vault;
import com.fswebdev.fit90.ui.BaseActivity;
import com.fswebdev.fit90.ui.Ui;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Private thought journal (CBT style): situation → anxious thought → feeling (0–10)
 * → a more balanced thought → feeling now. Encrypted; never leaves the phone.
 */
public class JournalActivity extends BaseActivity {

    private EditText situation, thought, balanced;
    private int before = 6, after = 4;

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        screen(getString(R.string.journal_title), true);
        content.addView(ui.text(getString(R.string.journal_intro), Ui.BODY, p.sub));
        content.addView(ui.space(12));
        situation = field(getString(R.string.j_situation), getString(R.string.j_situation_hint));
        thought = field(getString(R.string.j_thought), getString(R.string.j_thought_hint));
        content.addView(ui.slider(getString(R.string.j_before), 0, 10, before, ten(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                before = v;
            }
        }));
        balanced = field(getString(R.string.j_balanced), getString(R.string.j_balanced_hint));
        content.addView(ui.slider(getString(R.string.j_after), 0, 10, after, ten(), new Ui.IntCallback() {
            @Override
            public void on(int v) {
                after = v;
            }
        }));
        content.addView(ui.button(getString(R.string.save_entry), Ui.PRIMARY, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (thought.getText().toString().trim().isEmpty()) {
                    toast(getString(R.string.j_need_thought));
                    return;
                }
                JSONObject d = new JSONObject();
                try {
                    d.put("situation", situation.getText().toString().trim())
                            .put("thought", thought.getText().toString().trim())
                            .put("balanced", balanced.getText().toString().trim())
                            .put("before", before).put("after", after);
                } catch (JSONException ignored) {
                    // Constant keys.
                }
                Vault.of(JournalActivity.this).add("journal", d);
                toast(getString(R.string.saved));
                render();
            }
        }));

        List<Vault.Event> entries = Vault.of(this).events("journal");
        if (!entries.isEmpty()) {
            content.addView(ui.space(12));
            content.addView(ui.text(getString(R.string.past_entries), Ui.HEADING));
            content.addView(ui.space(8));
        }
        SimpleDateFormat fmt = new SimpleDateFormat("d MMM, HH:mm", Locale.getDefault());
        for (int i = entries.size() - 1; i >= 0; i--) {
            final Vault.Event e = entries.get(i);
            LinearLayout card = ui.card();
            card.addView(ui.text(fmt.format(new Date(e.ts)), Ui.CAPTION));
            if (!e.data.optString("situation").isEmpty()) card.addView(ui.text(e.data.optString("situation"), Ui.SMALL));
            card.addView(ui.text("“" + e.data.optString("thought") + "”", Ui.BODY));
            if (!e.data.optString("balanced").isEmpty()) {
                card.addView(ui.text("→ " + e.data.optString("balanced"), Ui.BODY, p.accent));
            }
            card.addView(ui.text(getString(R.string.j_feelings, e.data.optInt("before"), e.data.optInt("after")), Ui.CAPTION));
            card.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    confirmDelete(e);
                    return true;
                }
            });
            content.addView(card);
        }
        if (!entries.isEmpty()) content.addView(ui.text(getString(R.string.long_press_delete), Ui.CAPTION));
    }

    private void confirmDelete(final Vault.Event e) {
        new AlertDialog.Builder(this).setTitle(R.string.delete_entry)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int w) {
                        Vault.of(JournalActivity.this).delete(e.id);
                        render();
                    }
                }).show();
    }

    private EditText field(String label, String hint) {
        content.addView(ui.text(label, Ui.SUBHEAD));
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        e.setMinLines(2);
        e.setTextColor(p.text);
        e.setHintTextColor(p.faint);
        e.setBackground(ui.outline(p.card, 14, p.line, 1.5f));
        e.setPadding(ui.dp(14), ui.dp(12), ui.dp(14), ui.dp(12));
        e.setContentDescription(label);
        content.addView(e, ui.margins(ui.fill(), 0, 6, 0, 14));
        return e;
    }

    private Ui.Formatter ten() {
        return new Ui.Formatter() {
            @Override
            public String format(int v) {
                return v + " / 10";
            }
        };
    }
}
