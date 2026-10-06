package com.cleanclient.mixin;

import com.cleanclient.module.ModuleManager;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While freecam is on, what you mine / hit / interact with is worked out from your REAL player
 * (not from the detached camera), so mining keeps working on the block your player faces.
 */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method = "pick(F)V", at = @At("HEAD"), require = 0)
    private void cleanclient$pickHead(float partialTicks, CallbackInfo ci) {
        ModuleManager.FREECAM.beginPick();
    }

    @Inject(method = "pick(F)V", at = @At("TAIL"), require = 0)
    private void cleanclient$pickTail(float partialTicks, CallbackInfo ci) {
        ModuleManager.FREECAM.endPick();
    }
}
