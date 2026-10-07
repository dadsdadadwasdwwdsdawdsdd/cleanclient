package com.cleanclient;

import com.cleanclient.gui.ClickGuiScreen;
import com.cleanclient.gui.Hud;
import com.cleanclient.gui.Sidebar;
import com.cleanclient.module.ModuleManager;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import com.cleanclient.gui.CyberCity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

public class CleanClient implements ClientModInitializer {
    public static final String MOD_ID = "cleanclient";

    public static KeyMapping openGui;
    public static KeyMapping toggleFreecam;
    public static KeyMapping aimPlayer;

    @Override
    public void onInitializeClient() {
        Config.load();
        ModuleManager.init();

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(MOD_ID, "main"));

        openGui = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cleanclient.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, category));
        toggleFreecam = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cleanclient.freecam", InputConstants.Type.KEYSYM, InputConstants.KEY_H, category));
        aimPlayer = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.cleanclient.freecam_aim", InputConstants.Type.KEYSYM, InputConstants.KEY_LALT, category));
        Macros.register(category);

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openGui.consumeClick()) mc.setScreen(new ClickGuiScreen());
            while (toggleFreecam.consumeClick()) ModuleManager.FREECAM.toggle();
            ModuleManager.tick(mc);
            Macros.tick(mc);
        });

        WorldRenderEvents.BEFORE_TRANSLUCENT.register(ModuleManager::renderWorld);

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(MOD_ID, "hud"),
                Hud::render);

        // Replace the vanilla sidebar only when the custom one is switched on
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, original -> (g, tracker) -> {
            if (Sidebar.active()) Sidebar.render(g, tracker);
            else original.render(g, tracker);
        });

        // Cyber city behind the main menu: draw it over the vanilla background, then redraw the buttons on top
        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) -> {
            if (!(screen instanceof TitleScreen)) return;
            ScreenEvents.afterRender(screen).register((s, g, mouseX, mouseY, tickDelta) -> {
                if (!Config.d.cyberTitle) return;
                CyberCity.draw(g, s.width, s.height, false);
                for (AbstractWidget button : Screens.getButtons(s)) {
                    button.render(g, mouseX, mouseY, tickDelta);
                }
                var font = Minecraft.getInstance().font;
                g.drawString(font, "Minecraft 1.21.11  -  CleanClient", 4, s.height - 12, 0xAAFFFFFF);
                String c = "Copyright Mojang AB. Do not distribute!";
                g.drawString(font, c, s.width - font.width(c) - 4, s.height - 12, 0x88FFFFFF);
            });
        });

        // Never leave freecam on across worlds/servers
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            if (ModuleManager.FREECAM.isEnabled()) ModuleManager.FREECAM.toggle();
        });
    }
}
