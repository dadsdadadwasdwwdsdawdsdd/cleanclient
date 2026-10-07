package com.cleanclient.module;

import com.cleanclient.Config;
import com.cleanclient.gui.Theme;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BedBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.level.levelgen.Heightmap;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Overworld only, UNDERGROUND only. Flags loaded chunks that have
 *  - several storage blocks buried below the surface (chests, barrels, shulkers), or
 *  - several man-made blocks below Y 0 (the deepslate layer): hoppers, furnaces, beds, ...
 * Anything above ground or in the sky is ignored. Chunks you have not loaded cannot be read: a
 * client only ever receives chunks inside the server's view distance.
 */
public class StashFinder extends Module {
    public record Hit(String ctx, int chunkX, int chunkZ, int score, int chests, int barrels, int shulkers, int deep,
                      int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        public int centerX() { return chunkX * 16 + 8; }
        public int centerZ() { return chunkZ * 16 + 8; }
    }

    /** JSON form of a Hit. */
    public static class Saved {
        public String ctx;
        public int cx, cz, score, chests, barrels, shulkers, deep, minX, minY, minZ, maxX, maxY, maxZ;
    }

    /** Blocks that do not generate naturally in the deepslate layer. */
    private static final Set<Block> UNNATURAL = Set.of(
            Blocks.HOPPER, Blocks.FURNACE, Blocks.BLAST_FURNACE, Blocks.SMOKER, Blocks.CRAFTING_TABLE,
            Blocks.ENDER_CHEST, Blocks.BARREL, Blocks.DISPENSER, Blocks.DROPPER, Blocks.OBSERVER,
            Blocks.PISTON, Blocks.STICKY_PISTON, Blocks.BEACON, Blocks.ANVIL, Blocks.ENCHANTING_TABLE,
            Blocks.BREWING_STAND, Blocks.TNT, Blocks.LECTERN, Blocks.SMITHING_TABLE, Blocks.GRINDSTONE,
            Blocks.COMPARATOR, Blocks.REPEATER, Blocks.REDSTONE_LAMP);

    private static final int PER_TICK = 24;
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Map<String, Hit> hits = new HashMap<>();
    private final Set<String> announced = new HashSet<>();
    private final ArrayDeque<int[]> queue = new ArrayDeque<>();
    private boolean loaded, dirty;
    private int wait;

    public StashFinder() {
        super("Stash Finder", "Underground storage and odd blocks");
    }

    private static boolean unnatural(BlockState s) {
        Block b = s.getBlock();
        return UNNATURAL.contains(b) || b instanceof BedBlock || b instanceof ShulkerBoxBlock;
    }

    // ---------------------------------------------------------- persistence

    private Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("cleanclient_stashes.json");
    }

    private static String hitKey(String ctx, int cx, int cz) { return ctx + "#" + cx + "," + cz; }

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
                Hit h = new Hit(s.ctx, s.cx, s.cz, s.score, s.chests, s.barrels, s.shulkers, s.deep,
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
                s.chests = h.chests(); s.barrels = h.barrels(); s.shulkers = h.shulkers(); s.deep = h.deep();
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
        queue.clear();
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
        queue.clear();
        wait = 0;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.level == null || mc.player == null) return;
        if (mc.level.dimension() != Level.OVERWORLD) return; // underground scan only makes sense here

        forgetFar();

        if (wait > 0) { wait--; return; }
        if (queue.isEmpty()) buildQueue();

        String ctx = ctx();
        for (int i = 0; i < PER_TICK && !queue.isEmpty(); i++) {
            int[] c = queue.poll();
            scanChunk(mc.level, ctx, c[0], c[1]);
        }
        if (queue.isEmpty()) {
            wait = 40;
            if (dirty && Config.d.stashRemember) saveHits();
            dirty = false;
        }
    }

    private void buildQueue() {
        int radius = Math.max(2, Math.min(32, Config.d.stashRadius));
        final int pcx = mc.player.getBlockX() >> 4;
        final int pcz = mc.player.getBlockZ() >> 4;
        List<int[]> list = new ArrayList<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) list.add(new int[]{pcx + dx, pcz + dz});
        }
        list.sort(Comparator.comparingInt((int[] c) -> (c[0] - pcx) * (c[0] - pcx) + (c[1] - pcz) * (c[1] - pcz)));
        queue.addAll(list);
    }

    /** Drops flagged chunks that are further than the "forget" distance (default 50 blocks). */
    private void forgetFar() {
        int limit = Config.d.stashForgetDist;
        if (limit <= 0) return;
        String ctx = ctx();
        double px = mc.player.getX(), pz = mc.player.getZ();
        Iterator<Map.Entry<String, Hit>> it = hits.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Hit> e = it.next();
            Hit h = e.getValue();
            double dx = h.centerX() - px, dz = h.centerZ() - pz;
            if (!h.ctx().equals(ctx) || Math.sqrt(dx * dx + dz * dz) > limit) {
                announced.remove(e.getKey());
                it.remove();
                dirty = true;
            }
        }
    }

    private void scanChunk(ClientLevel level, String ctx, int cx, int cz) {
        ChunkAccess access = level.getChunk(cx, cz, ChunkStatus.FULL, false);
        if (!(access instanceof LevelChunk chunk)) return;

        int chests = 0, barrels = 0, shulkers = 0, deep = 0;
        int[] b = {Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        int minDepth = Config.d.stashMinDepth;

        // 1) storage blocks buried below the surface
        for (BlockEntity be : chunk.getBlockEntities().values()) {
            boolean isChest = be instanceof ChestBlockEntity;
            boolean isBarrel = be instanceof BarrelBlockEntity;
            boolean isShulker = be instanceof ShulkerBoxBlockEntity;
            if (!isChest && !isBarrel && !isShulker) continue;

            BlockPos p = be.getBlockPos();
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, p.getX(), p.getZ());
            if (p.getY() >= surface - minDepth) continue; // above ground, in the sky, or too shallow: ignore

            if (isChest) chests++; else if (isBarrel) barrels++; else shulkers++;
            grow(b, p.getX(), p.getY(), p.getZ());
        }

        // 2) man-made blocks below Y 0 (deepslate layer)
        LevelChunkSection[] sections = chunk.getSections();
        int baseX = chunk.getPos().getMinBlockX(), baseZ = chunk.getPos().getMinBlockZ();
        for (int i = 0; i < sections.length; i++) {
            int secY = chunk.getSectionYFromSectionIndex(i);
            if (secY >= 0) continue;
            LevelChunkSection sec = sections[i];
            if (sec == null || sec.hasOnlyAir() || !sec.maybeHas(StashFinder::unnatural)) continue;
            int baseY = secY << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (unnatural(sec.getBlockState(x, y, z))) {
                            deep++;
                            grow(b, baseX + x, baseY + y, baseZ + z);
                        }
                    }
                }
            }
        }

        int containers = chests + barrels + shulkers * 3;
        boolean flag = containers >= Config.d.stashThreshold || deep >= Config.d.stashDeepThreshold;
        String key = hitKey(ctx, cx, cz);

        if (flag) {
            Hit hit = new Hit(ctx, cx, cz, containers + deep, chests, barrels, shulkers, deep,
                    b[0], b[1], b[2], b[3], b[4], b[5]);
            Hit old = hits.put(key, hit);
            if (old == null || old.score() != hit.score()) dirty = true;
            if (announced.add(key)) {
                mc.player.displayClientMessage(Component.literal(
                        "[CleanClient] Underground chunk near " + hit.centerX() + ", " + hit.centerZ()
                                + ": " + chests + " chests, " + barrels + " barrels, " + shulkers
                                + " shulkers, " + deep + " odd blocks below Y 0"), false);
            }
        } else if (hits.remove(key) != null) {
            dirty = true;
        }
    }

    private static void grow(int[] b, int x, int y, int z) {
        b[0] = Math.min(b[0], x); b[1] = Math.min(b[1], y); b[2] = Math.min(b[2], z);
        b[3] = Math.max(b[3], x); b[4] = Math.max(b[4], y); b[5] = Math.max(b[5], z);
    }

    // ---------------------------------------------------------- output

    /** Called between EspRenderer.begin and EspRenderer.end. Rainbow-colored chunk volumes. */
    public void render() {
        int idx = 0;
        for (Hit h : sortedHits()) {
            if (idx >= 30) break;
            int c = Theme.rainbow(idx * 0.09f);
            idx++;
            float r = (c >> 16 & 255) / 255f, g = (c >> 8 & 255) / 255f, bl = (c & 255) / 255f;

            float x0 = h.chunkX() * 16f, z0 = h.chunkZ() * 16f;
            float y0 = h.minY() - 2f, y1 = h.maxY() + 3f;
            EspRenderer.box(x0, y0, z0, x0 + 16f, y1, z0 + 16f, r, g, bl, 0.14f);
            EspRenderer.box(h.minX() - 0.1f, h.minY() - 0.1f, h.minZ() - 0.1f,
                    h.maxX() + 1.1f, h.maxY() + 1.1f, h.maxZ() + 1.1f, r, g, bl, 0.42f);
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

    /** Up to 3 nearest flagged chunks, for the HUD. */
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
