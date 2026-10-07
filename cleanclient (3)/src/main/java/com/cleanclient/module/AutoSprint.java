package com.cleanclient.module;

import net.minecraft.client.Minecraft;

/** Holds the sprint key for you while you walk forward. */
public class AutoSprint extends Module {
    public AutoSprint() {
        super("Auto Sprint", "Sprint whenever you walk forward");
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || ModuleManager.FREECAM.isActive()) return;
        if (mc.options.keyUp.isDown() && !mc.player.isShiftKeyDown() && mc.player.getFoodData().getFoodLevel() > 6) {
            mc.options.keySprint.setDown(true);
        }
    }

    @Override
    protected void onDisable() {
        if (mc.options != null) mc.options.keySprint.setDown(false);
    }
}
