package com.cleanclient.mixin;

import com.cleanclient.CleanClient;
import com.cleanclient.module.Freecam;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * While freecam is on, mouse look rotates the camera instead of the real player,
 * unless the "aim player" key is held (then your real player turns as normal).
 */
@Mixin(Entity.class)
public abstract class EntityMixin {
    @Inject(method = "turn", at = @At("HEAD"), cancellable = true, require = 0)
    private void cleanclient$turn(double yRot, double xRot, CallbackInfo ci) {
        Freecam freecam = ModuleManager.FREECAM;
        if (!freecam.isEnabled() || (Object) this != Minecraft.getInstance().player) return;
        if (CleanClient.aimPlayer != null && CleanClient.aimPlayer.isDown()) return;
        freecam.rotate(yRot, xRot);
        ci.cancel();
    }
}
