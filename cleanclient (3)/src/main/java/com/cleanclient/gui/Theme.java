package com.cleanclient.gui;

public final class Theme {
    public static final int BG        = 0xE6141418;
    public static final int HEADER    = 0xFF1C1C24;
    public static final int ROW       = 0x00000000;
    public static final int ROW_HOVER = 0x33FFFFFF;
    public static final int ACCENT    = 0xFF7C5CFF;
    public static final int TEXT      = 0xFFEDEDF2;
    public static final int TEXT_DIM  = 0xFF8A8A99;
    public static final int OFF_PILL  = 0xFF3A3A46;
    public static final int DIM_SCREEN = 0x66000000;

    public static int lerpColor(int a, int b, float t) {
        int ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255, aa = a >>> 24;
        int br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255, ba = b >>> 24;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }
    private Theme() {}
}
