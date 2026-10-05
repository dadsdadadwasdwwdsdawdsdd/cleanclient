package com.cleanclient.module;

import com.cleanclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passive analysis of chunks you have loaded: flags chunks with an unusual number of
 * storage blocks (chests, barrels, shulker boxes). Uses only data the server already sent.
 */
public class StashFinder extends Module {
    public record Hit(int chunkX, int chunkZ, int score, int chests, int barrels, int shulkers,
                      int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int centerX() { return chunkX * 16 + 8; }
        public int centerZ() { return chunkZ * 16 + 8; }
    }

    private static final int MAX_RADIUS = 16;

    private final Map<Long, Hit> hits = new HashMap<>();
    private final Set<Long> announced = new HashSet<>();
    private int timer;

    public StashFinder() {
        super("Stash Finder", "Flags chunks with lots of storage");
    }

    @Override
    protected void onDisable() {
        hits.clear();
        announced.clear();
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        if (timer-- > 0) return;
        timer = 40; // scan every 2 seconds
        scan();
    }

    private static long key(int x, int z) { return ((long) x << 32) | (z & 0xffffffffL); }

    private void scan() {
        ClientLevel level = mc.level;
        int radius = Math.min(mc.options.renderDistance().get(), MAX_RADIUS);
        int pcx = mc.player.getBlockX() >> 4;
        int pcz = mc.player.getBlockZ() >> 4;

        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                int cx = pcx + dx, cz = pcz + dz;
                ChunkAccess access = level.getChunk(cx, cz, ChunkStatus.FULL, false);
                if (!(access instanceof LevelChunk chunk)) continue;

                int chests = 0, barrels = 0, shulkers = 0;
                int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;
                int maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE, maxZ = Integer.MIN_VALUE;

                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof ChestBlockEntity) chests++;
                    else if (be instanceof BarrelBlockEntity) barrels++;
                    else if (be instanceof ShulkerBoxBlockEntity) shulkers++;
                    else continue;
                    BlockPos p = be.getBlockPos();
                    minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
                    maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
                }

                int score = chests + barrels + shulkers * 3;
                long k = key(cx, cz);
                if (score >= Config.d.stashThreshold) {
                    Hit hit = new Hit(cx, cz, score, chests, barrels, shulkers, minX, minY, minZ, maxX, maxY, maxZ);
                    hits.put(k, hit);
                    if (announced.add(k)) {
                        mc.player.displayClientMessage(Component.literal(
                                "[CleanClient] Suspect chunk near " + hit.centerX() + ", " + hit.centerZ()
                                        + " (score " + score + ": " + chests + " chests, " + barrels + " barrels, "
                                        + shulkers + " shulkers)"), false);
                    }
                } else {
                    hits.remove(k); // chunk is loaded and no longer qualifies
                }
            }
        }
    }

    /** Called between EspRenderer.begin and EspRenderer.end. */
    public void render() {
        int drawn = 0;
        for (Hit h : sortedHits()) {
            if (drawn++ >= 40) break;
            EspRenderer.box(h.minX() - 0.1f, h.minY() - 0.1f, h.minZ() - 0.1f,
                    h.maxX() + 1.1f, h.maxY() + 1.1f, h.maxZ() + 1.1f,
                    1.00f, 0.42f, 0.10f, 0.22f);
        }
    }

    private List<Hit> sortedHits() {
        if (mc.player == null) return new ArrayList<>(hits.values());
        double px = mc.player.getX(), pz = mc.player.getZ();
        List<Hit> list = new ArrayList<>(hits.values());
        list.sort(Comparator.comparingDouble((Hit h) -> {
            double dx = h.centerX() - px, dz = h.centerZ() - pz;
            return dx * dx + dz * dz;
        }));
        return list;
    }

    public int count() { return hits.size(); }

    /** Up to 3 nearest suspect chunks, for the HUD. */
    public List<String> lines() {
        List<String> out = new ArrayList<>();
        if (mc.player == null) return out;
        double px = mc.player.getX(), pz = mc.player.getZ();
        for (Hit h : sortedHits()) {
            if (out.size() >= 3) break;
            double dx = h.centerX() - px, dz = h.centerZ() - pz;
            out.add(h.centerX() + ", " + h.centerZ() + "  score " + h.score() + "  " + (int) Math.sqrt(dx * dx + dz * dz) + "m");
        }
        return out;
    }
}
