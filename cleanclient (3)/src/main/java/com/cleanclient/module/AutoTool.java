package com.cleanclient.module;

import com.cleanclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** While you mine a block, switches your hotbar to the fastest tool for it (and back afterwards). */
public class AutoTool extends Module {
    private int prevSlot = -1;
    private int idle;

    public AutoTool() {
        super("Auto Tool", "Picks the best tool for the block");
    }

    @Override
    protected void onDisable() {
        restore();
    }

    private void restore() {
        if (prevSlot >= 0 && mc.player != null) mc.player.getInventory().setSelectedSlot(prevSlot);
        prevSlot = -1;
        idle = 0;
    }

    @Override
    public void onTick(Minecraft mc) {
        LocalPlayer p = mc.player;
        if (p == null || mc.level == null || mc.screen != null) return;

        boolean mining = mc.options.keyAttack.isDown()
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK;

        if (mining) {
            idle = 0;
            BlockPos pos = ((BlockHitResult) mc.hitResult).getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (state.getDestroySpeed(mc.level, pos) < 0) return; // unbreakable

            int current = p.getInventory().getSelectedSlot();
            int best = current;
            float bestSpeed = speed(p.getInventory().getItem(current), state);
            for (int i = 0; i < 9; i++) {
                ItemStack s = p.getInventory().getItem(i);
                if (nearlyBroken(s)) continue;
                float sp = speed(s, state);
                if (sp > bestSpeed + 0.01f) {
                    bestSpeed = sp;
                    best = i;
                }
            }
            if (best != current) {
                if (prevSlot < 0) prevSlot = current;
                p.getInventory().setSelectedSlot(best);
            }
        } else if (prevSlot >= 0 && Config.d.autoToolSwitchBack && ++idle > 10) {
            restore();
        }
    }

    private static float speed(ItemStack stack, BlockState state) {
        return stack.isEmpty() ? 1.0f : stack.getDestroySpeed(state);
    }

    private static boolean nearlyBroken(ItemStack s) {
        return s.isDamageableItem() && s.getMaxDamage() - s.getDamageValue() <= 2;
    }
}
