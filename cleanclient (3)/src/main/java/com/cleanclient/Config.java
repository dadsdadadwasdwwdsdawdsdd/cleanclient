package com.cleanclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public static final int MACRO_SLOTS = 6;

    public static class Data {
        // HUD layout
        public boolean hudWatermark = true;
        public boolean hudModuleList = true;
        public boolean hudListRight = true;
        public boolean hudInfo = true;
        public int hudStyle = 0;            // 0 Bar, 1 Box, 2 Text, 3 Underline
        public float hudOpacity = 0.55f;
        public int hudYOffset = 0;
        // HUD colors
        public int colorMode = 0;           // 0 Static, 1 Rainbow, 2 Gradient, 3 Pulse
        public int accentIndex = 0;
        public int accent2Index = 1;
        public int customR = 124, customG = 92, customB = 255;
        public float colorSpeed = 1.0f;
        // Sidebar
        public boolean sidebarCustom = false;
        public boolean sidebarHideScores = false;
        public int sidebarYOffset = 0;
        public boolean nameSpoofEnabled = false;
        public String spoofName = "Player";
        // Modules
        public float espAlpha = 0.38f;
        public int stashThreshold = 8;
        public int stashRadius = 16;
        public boolean stashRemember = true;
        public float freecamSpeed = 12f;
        public int autoLeaveY = 0;
        // Macros
        public String[] macros = new String[]{"", "", "", "", "", ""};
        public int macroDelayTicks = 12;
    }

    public static Data d = new Data();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("cleanclient.json");
    }

    public static void load() {
        try {
            Path p = path();
            if (Files.exists(p)) {
                Data loaded = GSON.fromJson(Files.readString(p), Data.class);
                if (loaded != null) d = loaded;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        // make sure the macro array is always the right size
        String[] fixed = new String[MACRO_SLOTS];
        for (int i = 0; i < MACRO_SLOTS; i++) {
            fixed[i] = (d.macros != null && i < d.macros.length && d.macros[i] != null) ? d.macros[i] : "";
        }
        d.macros = fixed;
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
