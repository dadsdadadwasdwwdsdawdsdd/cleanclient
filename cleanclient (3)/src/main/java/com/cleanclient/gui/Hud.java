package com.cleanclient.gui;

import com.cleanclient.Config;
import com.cleanclient.module.Module;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.Comparator;
import java.util.List;

/** Logo, info widgets, stash finder readout and the active-module list. */
public class Hud {
    public static void render(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font font = mc.font;
        int leftY = 6;

        if (Config.d.hudWatermark) {
            Logo.draw(g, 6, leftY, 2);
            leftY += Logo.height(2) + 8;
        }

        if (Config.d.hudInfo) {
            leftY = infoLine(g, font, "XYZ", mc.player.getBlockX() + " " + mc.player.getBlockY() + " " + mc.player.getBlockZ(), leftY);
            leftY = infoLine(g, font, "Totems", String.valueOf(count(mc, Items.TOTEM_OF_UNDYING)), leftY);
            leftY = infoLine(g, font, "Crystals", String.valueOf(count(mc, Items.END_CRYSTAL)), leftY);
            ClientPacketListener conn = mc.getConnection();
            PlayerInfo info = conn == null ? null : conn.getPlayerInfo(mc.player.getUUID());
            if (info != null) leftY = infoLine(g, font, "Ping", info.getLatency() + " ms", leftY);
            leftY += 4;
        }

        if (ModuleManager.STASH_FINDER.isEnabled()) {
            leftY = infoLine(g, font, "Stash Finder", ModuleManager.STASH_FINDER.count() + " suspect chunk(s)", leftY);
            for (String line : ModuleManager.STASH_FINDER.lines()) {
                g.drawString(font, "  " + line, 6, leftY, Theme.TEXT_DIM);
                leftY += 11;
            }
            leftY += 4;
        }

        if (Config.d.hudModuleList) {
            List<Module> active = ModuleManager.all().stream()
                    .filter(Module::isEnabled)
                    .sorted(Comparator.comparingInt((Module m) -> font.width(m.getName())).reversed())
                    .toList();

            int bg = ((int) (Config.d.hudOpacity * 255) << 24) | 0x101014;
            int sw = g.guiWidth();
            boolean right = Config.d.hudListRight;
            int y = (right ? 6 : leftY) + Config.d.hudYOffset;
            for (int i = 0; i < active.size(); i++) {
                entry(g, font, active.get(i).getName(), sw, y, right, Config.d.hudStyle, Theme.dynamic(i * 0.08f), bg);
                y += 13;
            }
        }
    }

    /** One line of the module list in the chosen style (0 Bar, 1 Box, 2 Text, 3 Underline). */
    private static void entry(GuiGraphics g, Font font, String name, int sw, int y, boolean right,
                              int style, int col, int bg) {
        int w = font.width(name);
        int x0, x1, tx;
        if (right) { x1 = sw; tx = sw - w - 8; x0 = tx - 4; }
        else       { x0 = 0; tx = 8; x1 = tx + w + 4; }
        int top = y - 2, bot = y + 10;

        switch (style) {
            case 1 -> {
                g.fill(x0, top, x1, bot, bg);
                g.fill(x0, top, x1, top + 1, col);
                g.fill(x0, bot - 1, x1, bot, col);
                g.fill(x0, top, x0 + 1, bot, col);
                g.fill(x1 - 1, top, x1, bot, col);
            }
            case 2 -> { /* text only */ }
            case 3 -> {
                g.fill(x0, top, x1, bot, bg);
                g.fill(x0, bot - 1, x1, bot, col);
            }
            default -> {
                g.fill(x0, top, x1, bot, bg);
                if (right) g.fill(x1 - 2, top, x1, bot, col);
                else g.fill(x0, top, x0 + 2, bot, col);
            }
        }
        g.drawString(font, name, tx, y, style == 2 ? col : Theme.TEXT);
    }

    private static int infoLine(GuiGraphics g, Font font, String label, String value, int y) {
        g.drawString(font, label + ":", 6, y, Theme.accent());
        g.drawString(font, value, 6 + font.width(label + ": "), y, Theme.TEXT);
        return y + 11;
    }

    private static int count(Minecraft mc, Item item) {
        Inventory inv = mc.player.getInventory();
        int n = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }
}
