package com.cleanclient;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Text macros: press a key to send a chat message or command, exactly as if you typed it.
 * Separate several commands with ';' (they are sent one after another with a short delay).
 * Keys are bound in Options > Controls (CleanClient category).
 */
public final class Macros {
    public static final KeyMapping[] KEYS = new KeyMapping[Config.MACRO_SLOTS];
    private static final Deque<String> QUEUE = new ArrayDeque<>();
    private static int cooldown;

    public static void register(KeyMapping.Category category) {
        for (int i = 0; i < KEYS.length; i++) {
            KEYS[i] = net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.cleanclient.macro" + (i + 1), InputConstants.Type.KEYSYM,
                    InputConstants.UNKNOWN.getValue(), category));
        }
    }

    public static String keyName(int slot) {
        return KEYS[slot] == null ? "?" : KEYS[slot].getTranslatedKeyMessage().getString();
    }

    public static void tick(Minecraft mc) {
        ClientPacketListener conn = mc.getConnection();
        if (mc.player == null || conn == null) {
            QUEUE.clear();
            cooldown = 0;
            return;
        }

        for (int i = 0; i < KEYS.length; i++) {
            boolean pressed = false;
            while (KEYS[i].consumeClick()) pressed = true;
            if (pressed && mc.screen == null) enqueue(Config.d.macros[i]);
        }

        if (cooldown > 0) { cooldown--; return; }
        String next = QUEUE.poll();
        if (next == null) return;

        if (next.length() > 256) next = next.substring(0, 256);
        if (next.startsWith("/")) conn.sendCommand(next.substring(1));
        else conn.sendChat(next);
        cooldown = Math.max(2, Config.d.macroDelayTicks);
    }

    private static void enqueue(String text) {
        if (text == null || text.isBlank() || QUEUE.size() > 20) return;
        for (String part : text.split(";")) {
            String s = part.trim();
            if (!s.isEmpty()) QUEUE.add(s);
        }
    }

    private Macros() {}
}
