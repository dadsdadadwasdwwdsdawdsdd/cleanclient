package com.cleanclient.module;

import net.minecraft.client.Minecraft;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** Full brightness using a client-side, endless night-vision effect (the server is never told). */
public class Fullbright extends Module {
    public Fullbright() {
        super("Fullbright", "See everything, even in the dark");
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null) return;
        if (!mc.player.hasEffect(MobEffects.NIGHT_VISION)) {
            mc.player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, -1, 0, false, false, false));
        }
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) mc.player.removeEffect(MobEffects.NIGHT_VISION);
    }
}
