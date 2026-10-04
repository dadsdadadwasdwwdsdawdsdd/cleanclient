package com.cleanclient.gui;

import com.cleanclient.module.Module;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import java.util.Comparator;
import java.util.List;

/** Minimal HUD: watermark + right-aligned list of active modules. */
public class Hud {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.getDebugHud().shouldShowDebugHud()) return;
        var tr = mc.textRenderer;

        ctx.drawTextWithShadow(tr, "Clean", 6, 6, Theme.ACCENT);
        ctx.drawTextWithShadow(tr, "Client", 6 + tr.getWidth("Clean"), 6, Theme.TEXT);

        List<Module> active = ModuleManager.all().stream()
                .filter(Module::isEnabled)
                .sorted(Comparator.comparingInt((Module m) -> tr.getWidth(m.getName())).reversed())
                .toList();

        int sw = ctx.getScaledWindowWidth();
        int y = 6;
        for (Module m : active) {
            int w = tr.getWidth(m.getName());
            int x = sw - w - 8;
            ctx.fill(x - 4, y - 2, sw, y + 10, 0x88101014);
            ctx.fill(sw - 2, y - 2, sw, y + 10, Theme.ACCENT);
            ctx.drawTextWithShadow(tr, m.getName(), x, y, Theme.TEXT);
            y += 13;
        }
    }
}
