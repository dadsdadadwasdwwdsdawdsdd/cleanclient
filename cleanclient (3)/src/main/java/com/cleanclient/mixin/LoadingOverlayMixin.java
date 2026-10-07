package com.cleanclient.mixin;

import com.cleanclient.Config;
import com.cleanclient.gui.CyberCity;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LoadingOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the animated cyber city over the vanilla startup / reload loading screen. */
@Mixin(LoadingOverlay.class)
public abstract class LoadingOverlayMixin {
    @Inject(method = "render(Lnet/minecraft/client/gui/GuiGraphics;IIF)V", at = @At("TAIL"), require = 0)
    private void cleanclient$cyber(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        if (!Config.d.cyberLoading) return;
        CyberCity.draw(g, g.guiWidth(), g.guiHeight(), true);
    }
}
