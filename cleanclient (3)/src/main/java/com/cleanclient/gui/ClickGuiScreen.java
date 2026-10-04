package com.cleanclient.gui;

import com.cleanclient.module.Module;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.HashMap;
import java.util.Map;

public class ClickGuiScreen extends Screen {
    private static final int W = 150, HEADER_H = 24, ROW_H = 22;

    // remembered between openings
    private static int px = 40, py = 40;

    private boolean dragging;
    private int dragOffX, dragOffY;
    private final Map<Module, Float> hover = new HashMap<>();
    private final Map<Module, Float> toggleAnim = new HashMap<>();

    public ClickGuiScreen() { super(Text.literal("CleanClient")); }

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        ctx.fill(0, 0, width, height, Theme.DIM_SCREEN);

        var mods = ModuleManager.all();
        int h = HEADER_H + mods.size() * ROW_H + 4;

        // panel
        ctx.fill(px, py, px + W, py + h, Theme.BG);
        ctx.fill(px, py, px + W, py + HEADER_H, Theme.HEADER);
        ctx.fill(px, py + HEADER_H - 1, px + W, py + HEADER_H, Theme.ACCENT);
        ctx.drawTextWithShadow(textRenderer, "CleanClient", px + 8, py + 8, Theme.TEXT);

        int y = py + HEADER_H + 2;
        for (Module m : mods) {
            boolean over = mx >= px && mx < px + W && my >= y && my < y + ROW_H;

            float hv = hover.getOrDefault(m, 0f);
            hv = MathHelper.lerp(0.25f, hv, over ? 1f : 0f);
            hover.put(m, hv);
            float ta = toggleAnim.getOrDefault(m, m.isEnabled() ? 1f : 0f);
            ta = MathHelper.lerp(0.25f, ta, m.isEnabled() ? 1f : 0f);
            toggleAnim.put(m, ta);

            ctx.fill(px + 2, y, px + W - 2, y + ROW_H, Theme.lerpColor(Theme.ROW, Theme.ROW_HOVER, hv));
            ctx.drawTextWithShadow(textRenderer, m.getName(), px + 10, y + 7,
                    Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(ta, hv)));

            // toggle pill
            int pw = 22, ph = 10, pxx = px + W - pw - 10, pyy = y + (ROW_H - ph) / 2;
            ctx.fill(pxx, pyy, pxx + pw, pyy + ph, Theme.lerpColor(Theme.OFF_PILL, Theme.ACCENT, ta));
            int knob = pxx + 1 + (int) ((pw - ph) * ta);
            ctx.fill(knob, pyy + 1, knob + ph - 2, pyy + ph - 1, Theme.TEXT);

            if (over) ctx.drawTooltip(textRenderer, Text.literal(m.getDescription()), mx, my);
            y += ROW_H;
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (mx >= px && mx < px + W) {
            if (my >= py && my < py + HEADER_H && button == 0) {
                dragging = true; dragOffX = (int) mx - px; dragOffY = (int) my - py;
                return true;
            }
            int y = py + HEADER_H + 2;
            for (Module m : ModuleManager.all()) {
                if (my >= y && my < y + ROW_H && button == 0) { m.toggle(); return true; }
                y += ROW_H;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging) { px = (int) mx - dragOffX; py = (int) my - dragOffY; return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        dragging = false;
        return super.mouseReleased(mx, my, button);
    }

    @Override public boolean shouldPause() { return false; }
}
