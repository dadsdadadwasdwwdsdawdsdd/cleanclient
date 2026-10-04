package com.cleanclient.module;

import net.minecraft.client.MinecraftClient;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ModuleManager {
    public static final ChestEsp CHEST_ESP = new ChestEsp();
    public static final Freecam FREECAM = new Freecam();

    private static final List<Module> MODULES = new ArrayList<>();

    public static void init() {
        MODULES.add(CHEST_ESP);
        MODULES.add(FREECAM);
    }

    public static List<Module> all() { return Collections.unmodifiableList(MODULES); }

    public static void tick(MinecraftClient mc) {
        for (Module m : MODULES) if (m.isEnabled()) m.onTick(mc);
    }
}
