package com.cleanclient.module;

import com.cleanclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Detaches the camera from the player. The real player is frozen (input replaced) while a
 * client-side camera entity flies. Movement and rotation update every frame so the view is smooth.
 *
 * - Mouse look is redirected to the camera by EntityMixin (hold the "aim player" key to turn your
 *   real player instead).
 * - GameRendererMixin makes block mining / interaction use the REAL player, not the camera.
 */
public class Freecam extends Module {
    private RemotePlayer camera;
    private ClientInput savedInput;
    private LocalPlayer owner;
    private Entity swapped;
    private float camYaw, camPitch;
    private boolean mixinSeen;
    private long lastNanos;

    public Freecam() {
        super("Freecam", "Fly the camera, player stays put");
    }

    @Override
    protected void onEnable() {
        if (mc.player == null || mc.level == null) {
            mc.execute(() -> setEnabled(false));
            return;
        }
        owner = mc.player;
        camera = new RemotePlayer(mc.level, owner.getGameProfile());
        camera.setPos(owner.getX(), owner.getY(), owner.getZ());
        camera.noPhysics = true;
        camera.setNoGravity(true);
        camYaw = owner.getYRot();
        camPitch = owner.getXRot();
        mixinSeen = false;
        applyRotation();
        syncPosition();

        savedInput = owner.input;
        owner.input = new ClientInput() {}; // real player stands still
        mc.setCameraEntity(camera);
        lastNanos = System.nanoTime();
        owner.displayClientMessage(Component.literal("Freecam on: WASD / Space / Shift, Ctrl = faster"), true);
    }

    @Override
    protected void onDisable() {
        swapped = null;
        if (mc.player != null && mc.player == owner && savedInput != null) {
            mc.player.input = savedInput;
        }
        if (mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.displayClientMessage(Component.literal("Freecam off"), true);
        }
        camera = null;
        savedInput = null;
        owner = null;
    }

    public boolean isActive() { return camera != null; }

    // --- used by GameRendererMixin: aim/mine with the real player, not the camera ---
    public void beginPick() {
        if (camera == null || mc.player == null) return;
        if (mc.getCameraEntity() == camera) {
            swapped = camera;
            mc.setCameraEntity(mc.player);
        }
    }

    public void endPick() {
        if (swapped != null) {
            mc.setCameraEntity(swapped);
            swapped = null;
        }
    }

    /** Called by EntityMixin on mouse movement (yaw delta, pitch delta in raw mouse units). */
    public void rotate(double yawDelta, double pitchDelta) {
        mixinSeen = true;
        camYaw += (float) yawDelta * 0.15f;
        camPitch = Mth.clamp(camPitch + (float) pitchDelta * 0.15f, -90f, 90f);
        applyRotation();
    }

    private void applyRotation() {
        if (camera == null) return;
        camera.setYRot(camYaw);
        camera.setXRot(camPitch);
        camera.yRotO = camYaw;
        camera.xRotO = camPitch;
        // living entities are viewed through their HEAD rotation, so set that too
        camera.yHeadRot = camYaw;
        camera.yHeadRotO = camYaw;
        camera.yBodyRot = camYaw;
        camera.yBodyRotO = camYaw;
    }

    private void syncPosition() {
        camera.xo = camera.xOld = camera.getX();
        camera.yo = camera.yOld = camera.getY();
        camera.zo = camera.zOld = camera.getZ();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (camera == null) return;
        // new player instance (respawn / dimension change), left the world, or got hurt -> bail out
        if (mc.player == null || mc.level == null || mc.player != owner || mc.player.hurtTime > 0) {
            setEnabled(false);
            return;
        }
        if (mc.getCameraEntity() != camera) mc.setCameraEntity(camera);
    }

    /** Per-frame movement; called from the world render event. */
    public void frameUpdate() {
        if (camera == null || mc.player == null) return;
        if (swapped == null && mc.getCameraEntity() != camera) mc.setCameraEntity(camera);

        long now = System.nanoTime();
        double dt = Math.min((now - lastNanos) / 1.0e9, 0.1);
        lastNanos = now;

        // Fallback if the mouse mixin did not apply: follow the real player's rotation, every frame.
        if (!mixinSeen) {
            camYaw = mc.player.getYRot();
            camPitch = mc.player.getXRot();
            applyRotation();
        }

        if (mc.screen == null) {
            Options o = mc.options;
            double yaw = Math.toRadians(camYaw);
            double pitch = Math.toRadians(camPitch);

            Vec3 forward = new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
            Vec3 right = new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));

            Vec3 move = Vec3.ZERO;
            if (o.keyUp.isDown())    move = move.add(forward);
            if (o.keyDown.isDown())  move = move.subtract(forward);
            if (o.keyRight.isDown()) move = move.add(right);
            if (o.keyLeft.isDown())  move = move.subtract(right);
            if (o.keyJump.isDown())  move = move.add(0, 1, 0);
            if (o.keyShift.isDown()) move = move.add(0, -1, 0);

            if (move.lengthSqr() > 0) {
                double perSecond = Config.d.freecamSpeed * (o.keySprint.isDown() ? 2.0 : 1.0);
                camera.setPos(camera.position().add(move.normalize().scale(perSecond * dt)));
            }
        }

        syncPosition();
    }
}
