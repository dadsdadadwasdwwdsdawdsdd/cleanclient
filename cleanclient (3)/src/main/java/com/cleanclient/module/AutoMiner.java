package com.cleanclient.module;

import com.cleanclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tunnels for you by holding the attack / forward keys and aiming at the next block.
 * Modes: Forward (1x2 tunnel), Down (straight down), Staircase (diagonal down).
 * It stops by itself on lava, big drops, low health, damage, full inventory, a nearly broken
 * tool, a nearby player, an unbreakable block or the chosen Y level.
 */
public class AutoMiner extends Module {
    public static final String[] MODES = {"Forward", "Down", "Staircase"};
    private static final int FORWARD = 0, DOWN = 1, STAIRS = 2;

    private Direction dir = Direction.NORTH;

    public AutoMiner() {
        super("Auto Miner", "Tunnels forward, down or in stairs");
    }

    @Override
    protected void onEnable() {
        if (mc.player == null) {
            mc.execute(() -> setEnabled(false));
            return;
        }
        dir = mc.player.getDirection();
    }

    @Override
    protected void onDisable() {
        release();
    }

    private void release() {
        if (mc.options == null) return;
        mc.options.keyAttack.setDown(false);
        mc.options.keyUp.setDown(false);
    }

    private void stop(String reason) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal("[Auto Miner] stopped: " + reason), false);
        }
        setEnabled(false);
    }

    @Override
    public void onTick(Minecraft mc) {
        LocalPlayer p = mc.player;
        ClientLevel level = mc.level;
        if (p == null || level == null) {
            setEnabled(false);
            return;
        }
        // pause while freecam is on or a menu is open
        if (ModuleManager.FREECAM.isActive() || mc.screen != null) {
            release();
            return;
        }

        int mode = Math.floorMod(Config.d.mineMode, MODES.length);

        String reason = safetyStop(p, level, mode);
        if (reason != null) {
            stop(reason);
            return;
        }

        BlockPos feet = p.blockPosition();
        BlockPos fl = feet.relative(dir);   // block in front, feet level
        BlockPos fu = fl.above();           // block in front, head level
        BlockPos fb = fl.below();           // block in front, one down
        BlockPos target = null;
        boolean move = false;

        if (mode == DOWN) {
            BlockPos below = feet.below();
            if (needsMining(level, below)) target = below;
        } else {
            if (needsMining(level, fu)) target = fu;
            else if (needsMining(level, fl)) target = fl;
            else if (mode == STAIRS && needsMining(level, fb)) target = fb;
            else move = true;
        }

        if (target != null) {
            BlockState state = level.getBlockState(target);
            if (state.getDestroySpeed(level, target) < 0) {
                stop("unbreakable block");
                return;
            }
            aim(p, target, mode);
        } else if (mode != DOWN) {
            p.setYRot(dir.toYRot()); // keep walking straight
        }

        mc.options.keyAttack.setDown(target != null);
        mc.options.keyUp.setDown(move);
    }

    private void aim(LocalPlayer p, BlockPos t, int mode) {
        if (mode == DOWN) {
            p.setXRot(90f);
            return;
        }
        Vec3 eye = p.getEyePosition();
        Vec3 c = Vec3.atCenterOf(t);
        double dx = c.x - eye.x, dy = c.y - eye.y, dz = c.z - eye.z;
        double h = Math.sqrt(dx * dx + dz * dz);
        float pitch = (float) -Math.toDegrees(Math.atan2(dy, h));
        p.setYRot(dir.toYRot());
        p.setXRot(Mth.clamp(pitch, -90f, 90f));
    }

    // ---------------------------------------------------------------- safety

    private String safetyStop(LocalPlayer p, ClientLevel level, int mode) {
        if (p.getHealth() < Config.d.mineMinHealth) return "health is low";
        if (p.hurtTime > 0) return "you took damage";
        if (p.getInventory().getFreeSlot() < 0) return "inventory is full";

        ItemStack held = p.getMainHandItem();
        if (held.isDamageableItem() && held.getMaxDamage() - held.getDamageValue() <= 8) {
            return "tool is almost broken";
        }
        if (mode != FORWARD && p.getBlockY() <= Config.d.mineStopY) {
            return "reached Y " + Config.d.mineStopY;
        }
        if (Config.d.mineStopNearPlayer) {
            for (Player other : level.players()) {
                if (other != p && p.distanceTo(other) < 20f) {
                    return "player nearby (" + other.getName().getString() + ")";
                }
            }
        }

        BlockPos feet = p.blockPosition();
        if (mode == DOWN) {
            for (int i = 1; i <= 3; i++) {
                if (lava(level, feet.below(i))) return "lava below";
            }
            if (airGap(level, feet.below(2), 4)) return "big drop below";
        } else {
            BlockPos fl = feet.relative(dir);
            BlockPos f2 = fl.relative(dir);
            BlockPos[] check = {fl, fl.above(), fl.below(), f2, f2.above(), f2.below()};
            for (BlockPos q : check) {
                if (lava(level, q)) return "lava ahead";
            }
            if (mode == FORWARD && airGap(level, fl.below(), 3)) return "drop ahead";
        }
        return null;
    }

    private static boolean needsMining(ClientLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        return !s.getCollisionShape(level, pos).isEmpty();
    }

    private static boolean lava(ClientLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.LAVA);
    }

    /** True if `count` blocks starting at `start` and going down are all empty air (no water to land in). */
    private static boolean airGap(ClientLevel level, BlockPos start, int count) {
        for (int i = 0; i < count; i++) {
            BlockPos q = start.below(i);
            BlockState s = level.getBlockState(q);
            if (!s.getCollisionShape(level, q).isEmpty()) return false;
            if (!level.getFluidState(q).isEmpty()) return false;
        }
        return true;
    }
}
