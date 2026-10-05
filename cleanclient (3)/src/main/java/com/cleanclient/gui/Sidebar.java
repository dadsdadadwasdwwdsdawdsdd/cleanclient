package com.cleanclient.gui;

import com.cleanclient.Config;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.numbers.NumberFormat;
import net.minecraft.network.chat.numbers.StyledFormat;
import net.minecraft.world.scores.DisplaySlot;
import net.minecraft.world.scores.Objective;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Customizable replacement for the vanilla scoreboard sidebar. It only redraws data the server
 * already sent; "name spoof" replaces your username in the text you SEE (nothing is sent anywhere).
 */
public final class Sidebar {
    private record Line(Component name, Component score) {}

    public static boolean active() {
        return Config.d.sidebarCustom || Config.d.nameSpoofEnabled;
    }

    public static void render(GuiGraphics g, DeltaTracker tracker) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.options.hideGui || mc.level == null) return;

        Scoreboard sb = mc.level.getScoreboard();
        Objective obj = sb.getDisplayObjective(DisplaySlot.SIDEBAR);
        if (obj == null) return;

        Font font = mc.font;
        NumberFormat nf = obj.numberFormatOrDefault(StyledFormat.SIDEBAR_DEFAULT);

        List<Line> lines = new ArrayList<>();
        sb.listPlayerScores(obj).stream()
                .filter(e -> !e.owner().startsWith("#"))
                .sorted(Comparator.<PlayerScoreEntry>comparingInt(PlayerScoreEntry::value).reversed())
                .limit(15)
                .forEach(e -> {
                    PlayerTeam team = sb.getPlayersTeam(e.owner());
                    Component name = spoof(PlayerTeam.formatNameForTeam(team, e.ownerName()));
                    Component score = Config.d.sidebarHideScores ? Component.empty() : e.formatValue(nf);
                    lines.add(new Line(name, score));
                });

        Component title = spoof(obj.getDisplayName());
        int lineH = font.lineHeight + 1;
        int w = font.width(title);
        for (Line l : lines) {
            int lw = font.width(l.name()) + (font.width(l.score()) > 0 ? 8 + font.width(l.score()) : 0);
            w = Math.max(w, lw);
        }

        int totalH = lines.size() * lineH;
        int x2 = g.guiWidth() - 3;
        int x1 = x2 - w - 4;
        int y0 = g.guiHeight() / 2 - totalH / 2 + Config.d.sidebarYOffset;

        g.fill(x1 - 2, y0 - lineH - 1, x2, y0 - 1, 0x66000000);
        g.fill(x1 - 2, y0 - lineH - 1, x1, y0 - 1, Theme.accent());
        g.fill(x1 - 2, y0 - 1, x2, y0 + totalH + 1, 0x4D000000);
        g.drawString(font, title, x1 + (w + 4 - font.width(title)) / 2, y0 - lineH + 1, 0xFFFFFFFF, false);

        int y = y0;
        for (Line l : lines) {
            g.drawString(font, l.name(), x1 + 2, y, 0xFFFFFFFF, false);
            int sw = font.width(l.score());
            if (sw > 0) g.drawString(font, l.score(), x2 - 2 - sw, y, 0xFFFFFFFF, false);
            y += lineH;
        }
    }

    /** Rebuilds a component with your real name swapped for the spoof name, keeping styles. */
    private static Component spoof(Component c) {
        if (!Config.d.nameSpoofEnabled) return c;
        String real = Minecraft.getInstance().getUser().getName();
        String fake = Config.d.spoofName == null ? "" : Config.d.spoofName;
        if (real == null || real.isEmpty()) return c;

        Pattern pattern = Pattern.compile(Pattern.quote(real), Pattern.CASE_INSENSITIVE);
        String replacement = Matcher.quoteReplacement(fake);
        MutableComponent out = Component.empty();
        c.<Object>visit((style, text) -> {
            out.append(Component.literal(pattern.matcher(text).replaceAll(replacement)).withStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    private Sidebar() {}
}
