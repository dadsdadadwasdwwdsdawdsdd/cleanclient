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

    public static final int[] ACCENTS = {0xFF7C5CFF, 0xFF4C8DFF, 0xFF3DDC84, 0xFFFF5C5C, 0xFFFFA23D, 0xFFFF6FD8, 0xFF3DD9EB};
    public static final String[] ACCENT_NAMES = {"Violet", "Blue", "Green", "Red", "Orange", "Pink", "Cyan", "Custom"};
    public static final String[] COLOR_MODES = {"Static", "Rainbow", "Gradient", "Pulse"};
    public static final String[] STYLES = {"Bar", "Box", "Text", "Underline"};

    public static int accentCount() { return ACCENT_NAMES.length; }

    public static String accentName(int i) {
        return ACCENT_NAMES[Math.floorMod(i, ACCENT_NAMES.length)];
    }

    /** Base color for an accent index (last index = the custom RGB color). */
    public static int base(int i) {
        i = Math.floorMod(i, ACCENT_NAMES.length);
        if (i == ACCENTS.length) {
            return 0xFF000000 | (clamp255(Config.d.customR) << 16) | (clamp255(Config.d.customG) << 8) | clamp255(Config.d.customB);
        }
        return ACCENTS[i];
    }

    /** The main accent color, animated according to the chosen color mode. */
    public static int accent() { return dynamic(0f); }

    /** Color for something at position `offset` (0..1-ish) in a list or word, animated by mode. */
    public static int dynamic(float offset) {
        Config.Data d = Config.d;
        int c1 = base(d.accentIndex);
        int c2 = base(d.accent2Index);
        double t = (System.currentTimeMillis() % 3_600_000L) / 1000.0 * d.colorSpeed;
        switch (d.colorMode) {
            case 1: { // rainbow
                float h = (float) (((t * 0.15 + offset) % 1.0 + 1.0) % 1.0);
                return hsv(h, 0.65f, 1f);
            }
            case 2: { // gradient between accent and accent 2
                float s = (float) (0.5 + 0.5 * Math.sin(t * 1.2 + offset * 6.2832));
                return lerpColor(c1, c2, s);
            }
            case 3: { // pulse
                float s = (float) (0.65 + 0.35 * Math.sin(t * 2.0 + offset * 3.0));
                int dark = 0xFF000000 | ((c1 >> 1) & 0x7F7F7F);
                return lerpColor(dark, c1, s);
            }
            default:
                return c1;
        }
    }

    private static int clamp255(int v) { return Math.max(0, Math.min(255, v)); }

    private static int hsv(float h, float s, float v) {
        int i = (int) (h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s), q = v * (1 - f * s), t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i % 6) {
            case 0: r = v; g = t; b = p; break;
            case 1: r = q; g = v; b = p; break;
            case 2: r = p; g = v; b = t; break;
            case 3: r = p; g = q; b = v; break;
            case 4: r = t; g = p; b = v; break;
            default: r = v; g = p; b = q; break;
        }
        return 0xFF000000 | ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public static int lerpColor(int a, int b, float t) {
        int aa = a >>> 24, ar = a >> 16 & 255, ag = a >> 8 & 255, ab = a & 255;
        int ba = b >>> 24, br = b >> 16 & 255, bg = b >> 8 & 255, bb = b & 255;
        return ((int) (aa + (ba - aa) * t) << 24) | ((int) (ar + (br - ar) * t) << 16)
                | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    private Theme() {}
}
