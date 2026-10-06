package com.cleanclient.gui;

import net.minecraft.client.gui.GuiGraphics;

/** Pixel-art "CLEAN" logo; the A is wearing sunglasses. Colors follow the HUD color mode. */
public final class Logo {
    private static final String[][] LETTERS = {
            {".####", "#....", "#....", "#....", "#....", "#....", ".####"}, // C
            {"#....", "#....", "#....", "#....", "#....", "#....", "#####"}, // L
            {"#####", "#....", "#....", "####.", "#....", "#....", "#####"}, // E
            {".###.", "#...#", "#...#", "#####", "#...#", "#...#", "#...#"}, // A
            {"#...#", "##..#", "#.#.#", "#..##", "#...#", "#...#", "#...#"}  // N
    };

    public static int width(int px) { return (5 * 6 - 1) * px; }
    public static int height(int px) { return 7 * px; }

    public static void draw(GuiGraphics g, int x, int y, int px) {
        // drop shadow
        for (int i = 0; i < LETTERS.length; i++) {
            for (int r = 0; r < 7; r++) {
                for (int c = 0; c < 5; c++) {
                    if (LETTERS[i][r].charAt(c) != '#') continue;
                    int lx = x + (i * 6 + c) * px, ly = y + r * px;
                    g.fill(lx + 1, ly + 1, lx + px + 1, ly + px + 1, 0x66000000);
                }
            }
        }
        // letters, lighter at the top
        for (int i = 0; i < LETTERS.length; i++) {
            int base = Theme.dynamic(i * 0.12f);
            for (int r = 0; r < 7; r++) {
                int color = Theme.lerpColor(base, 0xFFFFFFFF, (6 - r) / 6f * 0.4f);
                for (int c = 0; c < 5; c++) {
                    if (LETTERS[i][r].charAt(c) != '#') continue;
                    int lx = x + (i * 6 + c) * px, ly = y + r * px;
                    g.fill(lx, ly, lx + px, ly + px, color);
                }
            }
        }
        sunglasses(g, x + 3 * 6 * px, y, px);
    }

    private static void sunglasses(GuiGraphics g, int ax, int y, int px) {
        int top = y + px; // rows 1-2 of the A
        int frame = 0xFF05050A, lensHigh = 0xFF0B0B14, lensLow = 0xFF1B2145, glint = 0xAAFFFFFF;

        g.fill(ax - px, top, ax, top + px, frame);                 // left arm
        g.fill(ax + 5 * px, top, ax + 6 * px, top + px, frame);    // right arm
        g.fill(ax + 2 * px, top, ax + 3 * px, top + px, frame);    // bridge

        for (int side = 0; side < 2; side++) {
            int lx = ax + (side == 0 ? 0 : 3 * px);
            g.fill(lx, top, lx + 2 * px, top + 2 * px, frame);
            g.fill(lx + 1, top + 1, lx + 2 * px - 1, top + px, lensHigh);
            g.fill(lx + 1, top + px, lx + 2 * px - 1, top + 2 * px - 1, lensLow);
            int gs = Math.max(1, px / 2);
            g.fill(lx + 1, top + 1, lx + 1 + gs, top + 1 + gs, glint);
        }
    }

    private Logo() {}
}
