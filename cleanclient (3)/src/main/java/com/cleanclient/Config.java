package com.cleanclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public static class Data {
        // HUD
        public boolean hudWatermark = true;
        public boolean hudModuleList = true;
        public boolean hudListRight = true;
        public boolean hudInfo = true;
        public int accentIndex = 0;
        public float hudOpacity = 0.55f;
        public int hudYOffset = 0;
        // Sidebar
        public boolean sidebarCustom = false;
        public boolean sidebarHideScores = false;
        public int sidebarYOffset = 0;
        public boolean nameSpoofEnabled = false;
        public String spoofName = "Player";
        // Modules
        public int stashThreshold = 8;
        public float freecamSpeed = 12f;
        public int autoLeaveY = 0;
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
    }

    public static void save() {
        try {
            Files.writeString(path(), GSON.toJson(d));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
