package com.cleanclient.gui;

import com.cleanclient.Config;

public final class Theme {
    public static final int BG         = 0xE6141418;
    public static final int HEADER     = 0xFF1C1C24;
    public static final int ROW        = 0x00FFFFFF;
    public static final int ROW_HOVER  = 0x33FFFFFF;
    public static final int TEXT       = 0xFFEDEDF2;
    public static final int TEXT_DIM   = 0xFF8A8A99;
    public static final int OFF_PILL   = 0xFF3A3A46;
    public static final int DIM_SCREEN = 0x66000000;

    public static final int[] ACCENTS = {0xFF7C5CFF, 0xFF4C8DFF, 0xFF3DDC84, 0xFFFF5C5C, 0xFFFFA23D, 0xFFFF6FD8};
    public static final String[] ACCENT_NAMES = {"Violet", "Blue", "Green", "Red", "Orange", "Pink"};

    public static int accent() {
        return ACCENTS[Math.floorMod(Config.d.accentIndex, ACCENTS.length)];
    }

    public static String accentName() {
        return ACCENT_NAMES[Math.floorMod(Config.d.accentIndex, ACCENT_NAMES.length)];
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private Theme() {}
}
