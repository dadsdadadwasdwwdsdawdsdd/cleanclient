package com.cleanclient.module;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.EnderChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.entity.TrappedChestBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.Vec3;

/** Highlights storage blocks through walls (display only, reads data the server already sent). */
public class ChestEsp extends Module {
    private static final int MAX_CHUNK_RADIUS = 8;
    private static final float ALPHA = 0.38f;

    public ChestEsp() {
        super("Chest ESP", "Highlights storage blocks");
    }

    /** Called between EspRenderer.begin and EspRenderer.end. */
    public void render() {
        if (mc.level == null || mc.player == null) return;

        ClientLevel level = mc.level;
        Vec3 cam = EspRenderer.camera();
        int radius = Math.min(mc.options.renderDistance().get(), MAX_CHUNK_RADIUS);
        int ccx = (int) Math.floor(cam.x) >> 4;
        int ccz = (int) Math.floor(cam.z) >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                ChunkAccess access = level.getChunk(ccx + dx, ccz + dz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) continue;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    float[] rgb = colorFor(be);
                    if (rgb == null) continue;
                    BlockPos p = be.getBlockPos();
                    EspRenderer.box(
                            p.getX() + 0.04f, p.getY() + 0.0f, p.getZ() + 0.04f,
                            p.getX() + 0.96f, p.getY() + 0.92f, p.getZ() + 0.96f,
                            rgb[0], rgb[1], rgb[2], ALPHA);
                }
            }
        }
    }

    private static float[] colorFor(BlockEntity be) {
        if (be instanceof TrappedChestBlockEntity) return new float[]{1.00f, 0.27f, 0.27f}; // trapped = red
        if (be instanceof ChestBlockEntity)        return new float[]{1.00f, 0.75f, 0.16f}; // chest = gold
        if (be instanceof EnderChestBlockEntity)   return new float[]{0.67f, 0.31f, 1.00f}; // ender = purple
        if (be instanceof BarrelBlockEntity)       return new float[]{0.75f, 0.51f, 0.27f}; // barrel = brown
        if (be instanceof ShulkerBoxBlockEntity)   return new float[]{1.00f, 0.47f, 0.86f}; // shulker = pink
        return null;
    }
}
