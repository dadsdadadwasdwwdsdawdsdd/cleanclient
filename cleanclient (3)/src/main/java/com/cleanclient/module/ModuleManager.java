package com.cleanclient.module;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.Minecraft;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    public static final ChestEsp CHEST_ESP = new ChestEsp();
    public static final Freecam FREECAM = new Freecam();
    public static final StashFinder STASH_FINDER = new StashFinder();
    public static final AutoLeave AUTO_LEAVE = new AutoLeave();

    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.add(CHEST_ESP);
        MODULES.add(FREECAM);
        MODULES.add(STASH_FINDER);
        MODULES.add(AUTO_LEAVE);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    public static void tick(Minecraft mc) {
        for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
    }

    /** Runs every frame while the world is rendering. */
    public static void renderWorld(WorldRenderContext ctx) {
        FREECAM.frameUpdate();

        if (!CHEST_ESP.isEnabled() && !STASH_FINDER.isEnabled()) return;
        EspRenderer.begin(ctx);
        if (CHEST_ESP.isEnabled()) CHEST_ESP.render();
        if (STASH_FINDER.isEnabled()) STASH_FINDER.render();
        EspRenderer.end(Minecraft.getInstance());
    }
}
