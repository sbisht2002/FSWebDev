package com.fswebdev.fit90.ui;

import android.content.Context;
import android.content.res.Configuration;

import com.fswebdev.fit90.data.Settings;

/** Soft, warm light and dark colour sets. Every screen draws from one of these. */
public final class Palette {

    public final boolean dark;
    public final int bg, card, cardAlt, text, sub, faint, line;
    public final int accent, onAccent, accentSoft;
    public final int warm, warmSoft, blue, blueSoft, danger, dangerSoft;
    public final int purple, purpleSoft, gold, goldSoft;

    private Palette(boolean dark) {
        this.dark = dark;
        if (dark) {
            bg = 0xFF161412; card = 0xFF221F1C; cardAlt = 0xFF2A2622;
            text = 0xFFF1ECE6; sub = 0xFFA89E95; faint = 0xFF6F665E; line = 0xFF35302B;
            accent = 0xFF6BB696; onAccent = 0xFF0F1A15; accentSoft = 0xFF203A2F;
            warm = 0xFFE0916E; warmSoft = 0xFF3B2A22;
            blue = 0xFF7FAEEA; blueSoft = 0xFF1C2A3B;
            danger = 0xFFE07A6C; dangerSoft = 0xFF3B2320;
            purple = 0xFFB39BE6; purpleSoft = 0xFF2C2540;
            gold = 0xFFE3B65C; goldSoft = 0xFF3A3020;
        } else {
            bg = 0xFFF7F3EE; card = 0xFFFFFFFF; cardAlt = 0xFFF1EBE4;
            text = 0xFF2A2420; sub = 0xFF71665E; faint = 0xFFB5ABA2; line = 0xFFE8E1D9;
            accent = 0xFF357A61; onAccent = 0xFFFFFFFF; accentSoft = 0xFFDCEDE4;
            warm = 0xFFB8623E; warmSoft = 0xFFF6E3D9;
            blue = 0xFF3A72BA; blueSoft = 0xFFE1ECF8;
            danger = 0xFFB0412F; dangerSoft = 0xFFF6DFDB;
            purple = 0xFF6E52A8; purpleSoft = 0xFFEDE6F7;
            gold = 0xFF94701F; goldSoft = 0xFFF5EBD3;
        }
    }

    private static final Palette LIGHT = new Palette(false);
    private static final Palette DARK = new Palette(true);

    public static Palette of(Context context) {
        return isDark(context) ? DARK : LIGHT;
    }

    public static boolean isDark(Context context) {
        String mode = Settings.of(context).theme();
        if ("dark".equals(mode)) return true;
        if ("light".equals(mode)) return false;
        int night = context.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    /** A colour pair (strong, soft) for a named tone used by programs and chips. */
    public int[] tone(String name) {
        if ("warm".equals(name)) return new int[]{warm, warmSoft};
        if ("blue".equals(name)) return new int[]{blue, blueSoft};
        if ("purple".equals(name)) return new int[]{purple, purpleSoft};
        if ("gold".equals(name)) return new int[]{gold, goldSoft};
        if ("danger".equals(name)) return new int[]{danger, dangerSoft};
        return new int[]{accent, accentSoft};
    }
}
