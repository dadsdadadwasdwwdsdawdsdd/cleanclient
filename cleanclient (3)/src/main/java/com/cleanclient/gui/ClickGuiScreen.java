package com.cleanclient.gui;

import com.cleanclient.Config;
import com.cleanclient.module.Module;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

public class ClickGuiScreen extends Screen {
    private static final int W = 270, HEADER_H = 24, TAB_H = 20, ROW_H = 22, SLIDER_H = 32, FOOTER_H = 20;
    private static final String[] TABS = {"Visuals", "World", "Customize", "Sidebar"};

    // remembered between openings
    private static int px = 30, py = 30, tab = 0;

    private boolean dragging;
    private int dragOffX, dragOffY, lastMx, lastMy;
    private Row activeSlider;
    private EditBox nameBox;
    private final List<List<Row>> pages = new ArrayList<>();

    public ClickGuiScreen() {
        super(Component.literal("CleanClient"));
        buildPages();
    }

    @Override
    protected void init() {
        nameBox = new EditBox(font, 0, 0, 100, 14, Component.literal("Spoof name"));
        nameBox.setMaxLength(16);
        nameBox.setValue(Config.d.spoofName == null ? "" : Config.d.spoofName);
        nameBox.setResponder(s -> Config.d.spoofName = s);
        nameBox.visible = false;
        addWidget(nameBox);
    }

    @Override
    public void removed() {
        Config.save();
    }

    // ------------------------------------------------------------ pages

    private void buildPages() {
        pages.add(List.of(
                new ModuleRow(ModuleManager.CHEST_ESP, "Highlights chests, barrels, shulkers"),
                new ModuleRow(ModuleManager.FREECAM, "Fly the camera, player stays put (H)"),
                new SliderRow("Freecam speed", "Blocks per second (Ctrl = x2)", 4, 40,
                        () -> Config.d.freecamSpeed, v -> Config.d.freecamSpeed = (float) v, "%.0f b/s")
        ));
        pages.add(List.of(
                new ModuleRow(ModuleManager.STASH_FINDER, "Flags chunks with lots of storage"),
                new SliderRow("Stash threshold", "Score needed to flag a chunk", 3, 30,
                        () -> Config.d.stashThreshold, v -> Config.d.stashThreshold = (int) Math.round(v), "%.0f"),
                new ModuleRow(ModuleManager.AUTO_LEAVE, "Disconnect below a chosen Y level"),
                new SliderRow("Leave below Y", "Disconnect when you drop under this", -64, 64,
                        () -> Config.d.autoLeaveY, v -> Config.d.autoLeaveY = (int) Math.round(v), "%.0f")
        ));
        pages.add(List.of(
                new ToggleRow("Watermark", "Show the CleanClient logo",
                        () -> Config.d.hudWatermark, v -> Config.d.hudWatermark = v),
                new ToggleRow("Module list", "Show enabled modules",
                        () -> Config.d.hudModuleList, v -> Config.d.hudModuleList = v),
                new CycleRow("List side", "Left or right of the screen",
                        () -> Config.d.hudListRight ? "Right" : "Left",
                        () -> Config.d.hudListRight = !Config.d.hudListRight),
                new ToggleRow("Info widgets", "XYZ, totems, crystals, ping",
                        () -> Config.d.hudInfo, v -> Config.d.hudInfo = v),
                new CycleRow("Accent color", "Click to cycle colors",
                        Theme::accentName,
                        () -> Config.d.accentIndex = (Config.d.accentIndex + 1) % Theme.ACCENTS.length),
                new SliderRow("HUD opacity", "Background of the module list", 0.1, 1.0,
                        () -> Config.d.hudOpacity, v -> Config.d.hudOpacity = (float) v, "%.2f"),
                new SliderRow("HUD Y offset", "Move the module list up/down", -40, 100,
                        () -> Config.d.hudYOffset, v -> Config.d.hudYOffset = (int) Math.round(v), "%.0f")
        ));
        pages.add(List.of(
                new ToggleRow("Custom sidebar", "Use CleanClient's sidebar style",
                        () -> Config.d.sidebarCustom, v -> Config.d.sidebarCustom = v),
                new ToggleRow("Hide scores", "Hide the numbers on the sidebar",
                        () -> Config.d.sidebarHideScores, v -> Config.d.sidebarHideScores = v),
                new SliderRow("Sidebar Y offset", "Move the sidebar up/down", -150, 150,
                        () -> Config.d.sidebarYOffset, v -> Config.d.sidebarYOffset = (int) Math.round(v), "%.0f"),
                new ToggleRow("Name spoof", "Swap your name in the sidebar (visual only)",
                        () -> Config.d.nameSpoofEnabled, v -> Config.d.nameSpoofEnabled = v),
                new TextRow("Spoof name", "Name shown instead of yours")
        ));
    }

    // ------------------------------------------------------------ rows

    private abstract class Row {
        final String label, desc;
        float hv;

        Row(String label, String desc) { this.label = label; this.desc = desc; }

        int height() { return ROW_H; }
        boolean isSlider() { return false; }
        abstract void draw(GuiGraphics g, int x, int y, int w, boolean over);
        boolean press(double mx, int x, int w) { return false; }
        void drag(double mx, int x, int w) {}

        void hover(boolean over) { hv = Mth.lerp(0.25f, hv, over ? 1f : 0f); }

        void background(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 2, y, x + w - 2, y + h, Theme.lerpColor(Theme.ROW, Theme.ROW_HOVER, hv));
        }
    }

    private class ToggleRow extends Row {
        final BooleanSupplier get;
        final Consumer<Boolean> set;
        float anim;
        boolean init;

        ToggleRow(String label, String desc, BooleanSupplier get, Consumer<Boolean> set) {
            super(label, desc);
            this.get = get;
            this.set = set;
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, boolean over) {
            hover(over);
            float target = get.getAsBoolean() ? 1f : 0f;
            if (!init) { anim = target; init = true; }
            anim = Mth.lerp(0.25f, anim, target);

            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 10, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(anim, hv)));

            int pw = 22, ph = 10, pxx = x + w - pw - 10, pyy = y + (ROW_H - ph) / 2;
            g.fill(pxx, pyy, pxx + pw, pyy + ph, Theme.lerpColor(Theme.OFF_PILL, Theme.accent(), anim));
            int knob = pxx + 1 + (int) ((pw - ph) * anim);
            g.fill(knob, pyy + 1, knob + ph - 2, pyy + ph - 1, Theme.TEXT);
        }

        @Override
        boolean press(double mx, int x, int w) {
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    private class ModuleRow extends ToggleRow {
        ModuleRow(Module m, String desc) {
            super(m.getName(), desc, m::isEnabled, m::setEnabled);
        }
    }

    private class CycleRow extends Row {
        final Supplier<String> value;
        final Runnable next;

        CycleRow(String label, String desc, Supplier<String> value, Runnable next) {
            super(label, desc);
            this.value = value;
            this.next = next;
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, boolean over) {
            hover(over);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 10, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, hv));
            String v = value.get();
            g.drawString(font, v, x + w - 10 - font.width(v), y + 7, Theme.accent());
        }

        @Override
        boolean press(double mx, int x, int w) {
            next.run();
            return true;
        }
    }

    private class SliderRow extends Row {
        final double min, max;
        final DoubleSupplier get;
        final DoubleConsumer set;
        final String fmt;

        SliderRow(String label, String desc, double min, double max, DoubleSupplier get, DoubleConsumer set, String fmt) {
            super(label, desc);
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
            this.fmt = fmt;
        }

        @Override int height() { return SLIDER_H; }
        @Override boolean isSlider() { return true; }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, boolean over) {
            hover(over);
            background(g, x, y, w, SLIDER_H);
            double v = get.getAsDouble();
            g.drawString(font, label, x + 10, y + 5, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, hv));
            String txt = String.format(fmt, v);
            g.drawString(font, txt, x + w - 10 - font.width(txt), y + 5, Theme.TEXT_DIM);

            int bx = x + 10, bw = w - 20, by = y + 20;
            float t = (float) Mth.clamp((v - min) / (max - min), 0.0, 1.0);
            int fillW = (int) (bw * t);
            g.fill(bx, by, bx + bw, by + 3, Theme.OFF_PILL);
            g.fill(bx, by, bx + fillW, by + 3, Theme.accent());
            g.fill(bx + fillW - 2, by - 2, bx + fillW + 2, by + 5, Theme.TEXT);
        }

        @Override
        boolean press(double mx, int x, int w) { setFrom(mx, x, w); return true; }

        @Override
        void drag(double mx, int x, int w) { setFrom(mx, x, w); }

        private void setFrom(double mx, int x, int w) {
            double t = Mth.clamp((mx - (x + 10)) / (double) (w - 20), 0.0, 1.0);
            set.accept(min + t * (max - min));
        }
    }

    private class TextRow extends Row {
        TextRow(String label, String desc) { super(label, desc); }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, boolean over) {
            hover(over);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 10, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, hv));
            nameBox.setX(x + w - 110);
            nameBox.setY(y + 4);
            nameBox.setWidth(100);
            nameBox.visible = true;
            nameBox.render(g, lastMx, lastMy, 0f);
        }
        // press() returns false so the click reaches the text box
    }

    // ------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        lastMx = mx;
        lastMy = my;
        nameBox.visible = false;

        g.fill(0, 0, width, height, Theme.DIM_SCREEN);

        List<Row> rows = pages.get(tab);
        int bodyH = 0;
        for (Row r : rows) bodyH += r.height();
        int h = HEADER_H + TAB_H + 2 + bodyH + FOOTER_H;

        px = Mth.clamp(px, 0, Math.max(0, width - W));
        py = Mth.clamp(py, 0, Math.max(0, height - h));

        int accent = Theme.accent();
        g.fill(px, py, px + W, py + h, Theme.BG);
        g.fill(px, py, px + W, py + HEADER_H, Theme.HEADER);
        g.fill(px, py + HEADER_H - 1, px + W, py + HEADER_H, accent);
        g.drawString(font, "Clean", px + 8, py + 8, accent);
        g.drawString(font, "Client", px + 8 + font.width("Clean"), py + 8, Theme.TEXT);

        // tabs
        int ty = py + HEADER_H;
        int tw = W / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            int tx = px + i * tw;
            boolean sel = i == tab;
            boolean over = mx >= tx && mx < tx + tw && my >= ty && my < ty + TAB_H;
            if (over) g.fill(tx, ty, tx + tw, ty + TAB_H, 0x22FFFFFF);
            g.drawString(font, TABS[i], tx + (tw - font.width(TABS[i])) / 2, ty + 6, sel ? Theme.TEXT : Theme.TEXT_DIM);
            if (sel) g.fill(tx + 6, ty + TAB_H - 2, tx + tw - 6, ty + TAB_H, accent);
        }

        // rows
        String hoverDesc = "";
        int y = ty + TAB_H + 2;
        for (Row r : rows) {
            boolean over = mx >= px && mx < px + W && my >= y && my < y + r.height();
            if (over) hoverDesc = r.desc;
            r.draw(g, px, y, W, over);
            y += r.height();
        }

        String footer = hoverDesc.isEmpty() ? "Esc to close" : hoverDesc;
        g.drawString(font, font.plainSubstrByWidth(footer, W - 16), px + 8, py + h - FOOTER_H + 6, Theme.TEXT_DIM);
    }

    // ------------------------------------------------------------ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        nameBox.setFocused(false);

        if (event.button() == 0 && mx >= px && mx < px + W) {
            if (my >= py && my < py + HEADER_H) {
                dragging = true;
                dragOffX = (int) mx - px;
                dragOffY = (int) my - py;
                return true;
            }
            int ty = py + HEADER_H;
            if (my >= ty && my < ty + TAB_H) {
                tab = Mth.clamp((int) ((mx - px) / (W / TABS.length)), 0, TABS.length - 1);
                return true;
            }
            int y = ty + TAB_H + 2;
            for (Row r : pages.get(tab)) {
                if (my >= y && my < y + r.height()) {
                    if (r.press(mx, px, W)) {
                        if (r.isSlider()) activeSlider = r;
                        return true;
                    }
                    break;
                }
                y += r.height();
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging) {
            px = (int) event.x() - dragOffX;
            py = (int) event.y() - dragOffY;
            return true;
        }
        if (activeSlider != null) {
            activeSlider.drag(event.x(), px, W);
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = false;
        activeSlider = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
