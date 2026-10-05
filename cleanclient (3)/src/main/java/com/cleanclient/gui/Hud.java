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

/** Watermark, info widgets, stash finder readout and the active-module list. */
public class Hud {
    public static void render(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.player == null) return;
        Font font = mc.font;
        int accent = Theme.accent();
        int leftY = 6;

        if (Config.d.hudWatermark) {
            g.drawString(font, "Clean", 6, leftY, accent);
            g.drawString(font, "Client", 6 + font.width("Clean"), leftY, Theme.TEXT);
            leftY += 14;
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
            int y = Config.d.hudListRight ? 6 + Config.d.hudYOffset : leftY + Config.d.hudYOffset;
            for (Module m : active) {
                int w = font.width(m.getName());
                if (Config.d.hudListRight) {
                    int x = sw - w - 8;
                    g.fill(x - 4, y - 2, sw, y + 10, bg);
                    g.fill(sw - 2, y - 2, sw, y + 10, accent);
                    g.drawString(font, m.getName(), x, y, Theme.TEXT);
                } else {
                    g.fill(0, y - 2, 6 + w + 6, y + 10, bg);
                    g.fill(0, y - 2, 2, y + 10, accent);
                    g.drawString(font, m.getName(), 8, y, Theme.TEXT);
                }
                y += 13;
            }
        }
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
