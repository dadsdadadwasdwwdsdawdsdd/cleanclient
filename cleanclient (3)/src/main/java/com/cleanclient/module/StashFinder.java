package com.cleanclient.module;

import com.cleanclient.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Passive analysis of the chunks your client has been sent: flags chunks with an unusual number of
 * storage blocks. Flagged chunks are REMEMBERED (saved to disk, per server + dimension), so they
 * still show after you walk away and the chunk unloads.
 *
 * Limit: a client only ever receives chunk data inside the server's view distance. Nothing outside
 * that can be read, so this cannot see chests in chunks that were never sent to you.
 */
public class StashFinder extends Module {
    public record Hit(String ctx, int chunkX, int chunkZ, int score, int chests, int barrels, int shulkers,
                      int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int centerX() { return chunkX * 16 + 8; }
        public int centerZ() { return chunkZ * 16 + 8; }
    }

    /** JSON form of a Hit. */
    public static class Saved {
        public String ctx;
        public int cx, cz, score, chests, barrels, shulkers, minX, minY, minZ, maxX, maxY, maxZ;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Hit> hits = new HashMap<>();
    private final Set<String> announced = new HashSet<>();
    private boolean loaded, dirty;
    private int timer;

    public StashFinder() {
        super("Stash Finder", "Flags chunks with lots of storage");
    }

    // ---------------------------------------------------------- persistence

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("cleanclient_stashes.json");
    }

    private static String hitKey(String ctx, int cx, int cz) {
        return ctx + "#" + cx + "," + cz;
    }

    private void loadSaved() {
        if (loaded) return;
        loaded = true;
        try {
            Path p = file();
            if (!Files.exists(p)) return;
            Saved[] arr = GSON.fromJson(Files.readString(p), Saved[].class);
            if (arr == null) return;
            for (Saved s : arr) {
                if (s == null || s.ctx == null) continue;
                Hit h = new Hit(s.ctx, s.cx, s.cz, s.score, s.chests, s.barrels, s.shulkers,
                        s.minX, s.minY, s.minZ, s.maxX, s.maxY, s.maxZ);
                String k = hitKey(h.ctx(), h.chunkX(), h.chunkZ());
                hits.put(k, h);
                announced.add(k);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveHits() {
        try {
            List<Saved> out = new ArrayList<>();
            for (Hit h : hits.values()) {
                Saved s = new Saved();
                s.ctx = h.ctx(); s.cx = h.chunkX(); s.cz = h.chunkZ(); s.score = h.score();
                s.chests = h.chests(); s.barrels = h.barrels(); s.shulkers = h.shulkers();
                s.minX = h.minX(); s.minY = h.minY(); s.minZ = h.minZ();
                s.maxX = h.maxX(); s.maxY = h.maxY(); s.maxZ = h.maxZ();
                out.add(s);
            }
            Files.writeString(file(), GSON.toJson(out));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void clearAll() {
        hits.clear();
        announced.clear();
        dirty = false;
        saveHits();
    }

    // ---------------------------------------------------------- scanning

    private String ctx() {
        if (mc.level == null) return "none";
        ServerData sd = mc.getCurrentServer();
        String server = sd == null ? "singleplayer" : sd.ip;
        return server + "|" + mc.level.dimension();
    }

    @Override
    protected void onEnable() {
        loadSaved();
        timer = 0;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        if (timer-- > 0) return;
        timer = 40; // every 2 seconds
        scan();
        if (dirty && Config.d.stashRemember) {
            saveHits();
        }
        dirty = false;
    }

    private void scan() {
        ClientLevel level = mc.level;
        String ctx = ctx();
        int radius = Mth_clamp(Config.d.stashRadius, 2, 32);
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
                String key = hitKey(ctx, cx, cz);
                if (score >= Config.d.stashThreshold) {
                    Hit hit = new Hit(ctx, cx, cz, score, chests, barrels, shulkers, minX, minY, minZ, maxX, maxY, maxZ);
                    Hit old = hits.put(key, hit);
                    if (old == null || old.score() != score) dirty = true;
                    if (announced.add(key)) {
                        mc.player.displayClientMessage(Component.literal(
                                "[CleanClient] Suspect chunk near " + hit.centerX() + ", " + hit.centerZ()
                                        + " (score " + score + ": " + chests + " chests, " + barrels + " barrels, "
                                        + shulkers + " shulkers)"), false);
                    }
                } else if (hits.remove(key) != null) {
                    dirty = true; // chunk is loaded and no longer qualifies
                }
            }
        }
    }

    private static int Mth_clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    // ---------------------------------------------------------- output

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
        String ctx = ctx();
        List<Hit> list = new ArrayList<>();
        for (Hit h : hits.values()) if (h.ctx().equals(ctx)) list.add(h);
        if (mc.player == null) return list;
        double px = mc.player.getX(), pz = mc.player.getZ();
        list.sort(Comparator.comparingDouble((Hit h) -> {
            double dx = h.centerX() - px, dz = h.centerZ() - pz;
            return dx * dx + dz * dz;
        }));
        return list;
    }

    public int count() { return sortedHits().size(); }

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
