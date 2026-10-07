package com.cleanclient.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Animated synthwave "cyber city" used as the loading screen. Only plain fills, no textures or text. */
public final class CyberCity {
    private static final long START = System.currentTimeMillis();

    public static void draw(GuiGraphics g, int w, int h, boolean loadingBar) {
        double t = (System.currentTimeMillis() - START) / 1000.0;
        int horizon = (int) (h * 0.62);

        // sky gradient
        for (int y = 0; y < horizon; y += 3) {
            float f = y / (float) horizon;
            int col = f < 0.55f
                    ? Theme.lerpColor(0xFF04010F, 0xFF2A0A5E, f / 0.55f)
                    : Theme.lerpColor(0xFF2A0A5E, 0xFFFF2E88, (f - 0.55f) / 0.45f);
            g.fill(0, y, w, Math.min(y + 3, horizon), col);
        }
        g.fill(0, horizon, w, h, 0xFF07020F);

        stars(g, w, horizon, t);
        sun(g, w, horizon);
        skyline(g, w, horizon, t, 0, 24, 0.33f, 6.0, 0xFF140A33, 0xFF6A2BD9, false);
        skyline(g, w, horizon, t, 1, 30, 0.55f, 14.0, 0xFF0B0620, 0xFF00E5FF, true);
        grid(g, w, h, horizon, t);

        // scanlines
        for (int y = 0; y < h; y += 4) g.fill(0, y, w, y + 1, 0x14000000);

        // logo + loading bar
        int px = Math.max(3, Math.min(6, w / 70));
        Logo.draw(g, (w - Logo.width(px)) / 2, (int) (h * 0.10), px);
        if (loadingBar) bar(g, w, h, t);
    }

    private static void stars(GuiGraphics g, int w, int horizon, double t) {
        for (int i = 0; i < 70; i++) {
            int x = hash(i, 1) % Math.max(1, w);
            int y = hash(i, 2) % Math.max(1, (int) (horizon * 0.55));
            float tw = (float) (0.5 + 0.5 * Math.sin(t * 2.0 + i));
            int a = (int) (60 + 150 * tw);
            g.fill(x, y, x + 1 + (i % 3 == 0 ? 1 : 0), y + 1 + (i % 3 == 0 ? 1 : 0), (a << 24) | 0xFFFFFF);
        }
    }

    private static void sun(GuiGraphics g, int w, int horizon) {
        int r = Math.max(30, Math.min(w, horizon) / 4);
        int cx = w / 2, cy = horizon - r / 3;
        for (int dy = -r; dy <= r; dy += 2) {
            int yy = cy + dy;
            if (yy >= horizon) break;
            if (dy > 0 && ((dy / 2) % Math.max(2, 7 - dy * 6 / r)) == 0) continue; // retro cut bands
            int half = (int) Math.sqrt(Math.max(0, (double) r * r - (double) dy * dy));
            float f = (dy + r) / (2f * r);
            g.fill(cx - half, yy, cx + half, yy + 2, Theme.lerpColor(0xFFFFE066, 0xFFFF2E88, f));
        }
    }

    private static void skyline(GuiGraphics g, int w, int horizon, double t, int layer, int slotW,
                                float heightFrac, double speed, int body, int neon, boolean windows) {
        double scroll = t * speed;
        int first = (int) Math.floor(scroll / slotW) - 1;
        int count = w / slotW + 4;
        int maxH = (int) (horizon * heightFrac);
        int[] lights = {0xFF00E5FF, 0xFFFF2E88, 0xFFFFE066};

        for (int k = 0; k < count; k++) {
            int slot = first + k;
            int hh = hash(slot, layer * 7 + 1);
            int bw = slotW - 2 - (hh % 5);
            int bh = (int) (maxH * (0.25 + 0.75 * ((hh >> 4) % 100) / 100.0));
            int x = (int) (slot * slotW - scroll);
            int top = horizon - bh;

            g.fill(x, top, x + bw, horizon, body);
            g.fill(x, top, x + bw, top + 1, neon);
            if (((hh >> 9) & 3) == 0) g.fill(x + bw / 2, top - 6, x + bw / 2 + 1, top, neon);

            if (!windows) continue;
            for (int wy = top + 4; wy < horizon - 3; wy += 5) {
                for (int wx = x + 3; wx < x + bw - 3; wx += 4) {
                    int wh = hash(slot * 131 + (wx - x), wy - top);
                    boolean lit = (wh % 7) < 3;
                    if (lit && (wh % 19) == 0 && (((int) (t * 4)) + slot) % 2 == 0) lit = false; // flicker
                    if (lit) g.fill(wx, wy, wx + 2, wy + 2, lights[(wh >> 3) % 3]);
                }
            }
        }
    }

    private static void grid(GuiGraphics g, int w, int h, int horizon, double t) {
        int gh = Math.max(1, h - horizon);
        double shift = (t * 0.6) % 1.0;
        for (int i = 0; i < 14; i++) {
            double f = (i + shift) / 14.0;
            f = f * f;
            int y = horizon + (int) (f * gh);
            int a = (int) (60 + 150 * f);
            g.fill(0, y, w, y + 1, (a << 24) | 0xFF2E88);
        }
        int vx = w / 2;
        for (int i = -9; i <= 9; i++) {
            int xb = vx + i * (w / 9);
            for (int y = horizon; y < h; y += 3) {
                int x = vx + (int) ((xb - vx) * (y - horizon) / (double) gh);
                g.fill(x, y, x + 1, Math.min(y + 3, h), 0x66B14DFF);
            }
        }
    }

    private static void bar(GuiGraphics g, int w, int h, double t) {
        int bw = (int) (w * 0.4), bh = 4;
        int bx = (w - bw) / 2, by = h - 28;
        g.fill(bx - 1, by - 1, bx + bw + 1, by + bh + 1, 0xFF00E5FF);
        g.fill(bx, by, bx + bw, by + bh, 0xFF05020D);
        double p = (t * 0.6) % 1.4 - 0.2; // sweeping segment
        int s0 = (int) (bx + (p - 0.0) * bw), s1 = s0 + bw / 4;
        s0 = Math.max(bx, s0);
        s1 = Math.min(bx + bw, s1);
        if (s1 > s0) {
            int mid = (s0 + s1) / 2;
            g.fill(s0, by, mid, by + bh, 0xFFFF2E88);
            g.fill(mid, by, s1, by + bh, 0xFF00E5FF);
        }
    }

    private static int hash(int a, int b) {
        int h = a * 374761393 + b * 668265263;
        h = (h ^ (h >>> 13)) * 1274126177;
        return (h ^ (h >>> 16)) & 0x7fffffff;
    }

    private CyberCity() {}
}
