package com.cleanclient.module;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.input.Input;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * Detaches the camera from the player. The real player is frozen in place
 * (input replaced with an empty Input) while a client-side camera entity flies.
 */
public class Freecam extends Module {
    private OtherClientPlayerEntity camera;
    private Input savedInput;
    public static double speed = 0.9;

    public Freecam() {
        super("Freecam", "Fly the camera without moving your player");
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.world == null) { setEnabledSilently(); return; }

        camera = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());
        camera.copyPositionAndRotation(mc.player);
        camera.noClip = true;
        camera.setNoGravity(true);

        savedInput = mc.player.input;
        mc.player.input = new Input(); // player stands still
        mc.setCameraEntity(camera);
    }

    @Override
    protected void onDisable() {
        if (mc.player != null) {
            if (savedInput != null) mc.player.input = savedInput;
            mc.setCameraEntity(mc.player);
        }
        camera = null;
        savedInput = null;
    }

    private void setEnabledSilently() {
        // couldn't start (no world) -> flip back off on next tick
        new Thread(() -> mc.execute(() -> setEnabled(false))).start();
    }

    @Override
    public void onTick(MinecraftClient mc) {
        if (camera == null || mc.player == null) return;

        // smooth interpolation between ticks
        camera.prevX = camera.lastRenderX = camera.getX();
        camera.prevY = camera.lastRenderY = camera.getY();
        camera.prevZ = camera.lastRenderZ = camera.getZ();
        camera.prevYaw = camera.getYaw();
        camera.prevPitch = camera.getPitch();

        // NOTE: mouse look still turns the real player; see README for the Mouse mixin upgrade.
        camera.setYaw(mc.player.getYaw());
        camera.setPitch(mc.player.getPitch());

        if (mc.currentScreen != null) return;

        GameOptions o = mc.options;
        double yaw = Math.toRadians(camera.getYaw());
        double pitch = Math.toRadians(camera.getPitch());

        Vec3d forward = new Vec3d(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
        Vec3d right = new Vec3d(-Math.cos(yaw), 0, -Math.sin(yaw));

        Vec3d move = Vec3d.ZERO;
        if (o.forwardKey.isPressed()) move = move.add(forward);
        if (o.backKey.isPressed())    move = move.subtract(forward);
        if (o.rightKey.isPressed())   move = move.add(right.multiply(-1));
        if (o.leftKey.isPressed())    move = move.add(right);
        if (o.jumpKey.isPressed())    move = move.add(0, 1, 0);
        if (o.sneakKey.isPressed())   move = move.add(0, -1, 0);

        if (move.lengthSquared() > 0) {
            double s = speed * (o.sprintKey.isPressed() ? 2.0 : 1.0) * 0.5;
            camera.setPosition(camera.getPos().add(move.normalize().multiply(s)));
        }
    }
}
