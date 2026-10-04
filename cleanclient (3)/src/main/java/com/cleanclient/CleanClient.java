package com.cleanclient;

import com.cleanclient.gui.ClickGuiScreen;
import com.cleanclient.gui.Hud;
import com.cleanclient.module.ModuleManager;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class CleanClient implements ClientModInitializer {
    public static final String CATEGORY = "key.categories.cleanclient";

    public static KeyBinding openGui;
    public static KeyBinding toggleFreecam;

    @Override
    public void onInitializeClient() {
        ModuleManager.init();

        openGui = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cleanclient.gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, CATEGORY));
        toggleFreecam = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cleanclient.freecam", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, CATEGORY));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.wasPressed()) mc.setScreen(new ClickGuiScreen());
            while (toggleFreecam.wasPressed()) ModuleManager.FREECAM.toggle();
            ModuleManager.tick(mc);
        });

        WorldRenderEvents.LAST.register(ModuleManager.CHEST_ESP::render);
        HudRenderCallback.EVENT.register(Hud::render);

        // Never leave freecam on across worlds/servers
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (ModuleManager.FREECAM.isEnabled()) ModuleManager.FREECAM.toggle();
        });
    }
}
