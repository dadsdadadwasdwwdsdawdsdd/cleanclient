package com.cleanclient.gui;

import com.cleanclient.CleanClient;
import com.cleanclient.Config;
import com.cleanclient.Macros;
import com.cleanclient.module.Module;
import com.cleanclient.module.ModuleManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Dropdown-style GUI: one draggable panel per category. Click a panel header to open/close it.
 * Left-click a module to toggle it, right-click a module to open its settings.
 */
public class ClickGuiScreen extends Screen {
    private static final int HEADER_H = 18, ROW_H = 22, SLIDER_H = 28, FIELD_H = 34, INFO_H = 14;
    private static final int TOP = 42, GAP = 8;

    // remembered between openings
    private static final Set<String> OPEN = new HashSet<>();
    private static final Set<String> COLLAPSED = new HashSet<>();
    private static final Map<String, int[]> POS = new HashMap<>();

    private final List<Panel> panels = new ArrayList<>();
    private final List<EditBox> boxes = new ArrayList<>();
    private Panel dragPanel;
    private int dragOffX, dragOffY, pressX, pressY;
    private boolean dragMoved;
    private Row activeSlider;

    public ClickGuiScreen() {
        super(Component.literal("CleanClient"));
    }

    @Override
    protected void init() {
        activeSlider = null;
        dragPanel = null;
        buildPanels();
    }

    @Override
    public void removed() {
        Config.save();
    }

    // ================================================================ panels

    private class Panel {
        final String title;
        final int w;
        final List<Row> rows;
        int x, y;

        Panel(String title, int w, List<Row> rows) {
            this.title = title;
            this.w = w;
            this.rows = rows;
        }

        boolean collapsed() { return COLLAPSED.contains(title); }

        int height() {
            int h = HEADER_H;
            if (!collapsed()) h += 2 + childrenHeight(rows) + 2;
            return h;
        }
    }

    private EditBox box(String initial, int max, Consumer<String> onChange) {
        EditBox b = new EditBox(font, 0, 0, 100, 14, Component.empty());
        b.setMaxLength(max);
        b.setValue(initial == null ? "" : initial);
        b.setResponder(onChange);
        b.visible = false;
        addWidget(b);
        boxes.add(b);
        return b;
    }

    private void buildPanels() {
        panels.clear();
        boxes.clear();

        // ---------------- Visuals
        List<Row> visuals = List.of(
                new ModuleRow(ModuleManager.CHEST_ESP, "Highlights chests, barrels, shulkers", List.of(
                        new SliderRow("Opacity", "Box transparency", 0.1, 0.8,
                                () -> Config.d.espAlpha, v -> Config.d.espAlpha = (float) v, "%.2f"),
                        new ToggleRow("Underground only", "Ignore chests above ground",
                                () -> Config.d.espUndergroundOnly, v -> Config.d.espUndergroundOnly = v)
                )),
                new ModuleRow(ModuleManager.FREECAM, "Fly the camera, player stays put", List.of(
                        new SliderRow("Speed", "Blocks per second (Ctrl = x2)", 4, 40,
                                () -> Config.d.freecamSpeed, v -> Config.d.freecamSpeed = (float) v, "%.0f b/s"),
                        new InfoRow(() -> "Hold " + CleanClient.aimPlayer.getTranslatedKeyMessage().getString() + ": aim player")
                )),
                new ModuleRow(ModuleManager.FULLBRIGHT, "See everything, even in the dark", List.of())
        );

        // ---------------- World
        List<Row> world = List.of(
                new ModuleRow(ModuleManager.STASH_FINDER, "Flags chunks with lots of storage", List.of(
                        new SliderRow("Threshold", "Score needed to flag a chunk", 3, 30,
                                () -> Config.d.stashThreshold, v -> Config.d.stashThreshold = (int) Math.round(v), "%.0f"),
                        new SliderRow("Scan radius", "Chunks to scan (only loaded ones exist)", 4, 32,
                                () -> Config.d.stashRadius, v -> Config.d.stashRadius = (int) Math.round(v), "%.0f"),
                        new SliderRow("Min depth", "Containers must be this far under the surface", 2, 30,
                                () -> Config.d.stashMinDepth, v -> Config.d.stashMinDepth = (int) Math.round(v), "%.0f"),
                        new SliderRow("Odd blocks", "Man-made blocks below Y 0 needed to flag", 2, 30,
                                () -> Config.d.stashDeepThreshold, v -> Config.d.stashDeepThreshold = (int) Math.round(v), "%.0f"),
                        new SliderRow("Forget beyond", "Drop chunks further than this (0 = never)", 0, 500,
                                () -> Config.d.stashForgetDist, v -> Config.d.stashForgetDist = (int) Math.round(v), "%.0f m"),
                        new ToggleRow("Remember", "Keep flagged chunks after they unload",
                                () -> Config.d.stashRemember, v -> Config.d.stashRemember = v),
                        new ButtonRow("Clear remembered", "Forget every saved suspect chunk",
                                () -> ModuleManager.STASH_FINDER.clearAll())
                )),
                new ModuleRow(ModuleManager.AUTO_LEAVE, "Disconnect below a chosen Y level", List.of(
                        new SliderRow("Leave below Y", "Disconnect when you drop under this", -64, 64,
                                () -> Config.d.autoLeaveY, v -> Config.d.autoLeaveY = (int) Math.round(v), "%.0f")
                )),
                new ModuleRow(ModuleManager.AUTO_MINER, "Tunnels forward, down or in stairs", List.of(
                        new CycleRow("Mode", "Forward, Down or Staircase",
                                () -> com.cleanclient.module.AutoMiner.MODES[Math.floorMod(Config.d.mineMode, com.cleanclient.module.AutoMiner.MODES.length)],
                                () -> Config.d.mineMode = (Config.d.mineMode + 1) % com.cleanclient.module.AutoMiner.MODES.length),
                        new SliderRow("Stop at Y", "Down/Staircase stop at this height", -64, 64,
                                () -> Config.d.mineStopY, v -> Config.d.mineStopY = (int) Math.round(v), "%.0f"),
                        new SliderRow("Min health", "Stops when health drops below this", 2, 20,
                                () -> Config.d.mineMinHealth, v -> Config.d.mineMinHealth = (int) Math.round(v), "%.0f"),
                        new ToggleRow("Stop near players", "Stops if a player is within 20 blocks",
                                () -> Config.d.mineStopNearPlayer, v -> Config.d.mineStopNearPlayer = v)
                ))
        );

        // ---------------- Player
        List<Row> player = List.of(
                new ModuleRow(ModuleManager.AUTO_TOOL, "Picks the best tool for the block", List.of(
                        new ToggleRow("Switch back", "Return to your slot when you stop mining",
                                () -> Config.d.autoToolSwitchBack, v -> Config.d.autoToolSwitchBack = v)
                )),
                new ModuleRow(ModuleManager.AUTO_SPRINT, "Sprint whenever you walk forward", List.of())
        );

        // ---------------- Macros
        List<Row> macros = new ArrayList<>();
        for (int i = 0; i < Config.MACRO_SLOTS; i++) {
            final int slot = i;
            EditBox b = box(Config.d.macros[slot], 120, s -> Config.d.macros[slot] = s);
            macros.add(new FieldRow(() -> "Macro " + (slot + 1) + "  [" + Macros.keyName(slot) + "]",
                    "Chat text or /command. Use ; to chain commands", b));
        }
        macros.add(new SliderRow("Delay", "Ticks between chained commands (20 = 1s)", 4, 40,
                () -> Config.d.macroDelayTicks, v -> Config.d.macroDelayTicks = (int) Math.round(v), "%.0f"));
        macros.add(new InfoRow(() -> "Bind keys: Options > Controls"));

        // ---------------- HUD
        List<Row> hud = List.of(
                new GroupRow("Layout", "Logo, lists and widgets", List.of(
                        new ToggleRow("Logo", "Show the CLEAN logo",
                                () -> Config.d.hudWatermark, v -> Config.d.hudWatermark = v),
                        new ToggleRow("Module list", "Show enabled modules",
                                () -> Config.d.hudModuleList, v -> Config.d.hudModuleList = v),
                        new CycleRow("List side", "Left or right of the screen",
                                () -> Config.d.hudListRight ? "Right" : "Left",
                                () -> Config.d.hudListRight = !Config.d.hudListRight),
                        new CycleRow("Style", "Look of the module list",
                                () -> Theme.STYLES[Math.floorMod(Config.d.hudStyle, Theme.STYLES.length)],
                                () -> Config.d.hudStyle = (Config.d.hudStyle + 1) % Theme.STYLES.length),
                        new ToggleRow("Info widgets", "XYZ, totems, ping, speed, armor...",
                                () -> Config.d.hudInfo, v -> Config.d.hudInfo = v),
                        new ToggleRow("Cyber loading", "Animated city on the loading screen",
                                () -> Config.d.cyberLoading, v -> Config.d.cyberLoading = v),
                        new ToggleRow("Cyber title", "Animated city on the main menu",
                                () -> Config.d.cyberTitle, v -> Config.d.cyberTitle = v),
                        new SliderRow("Opacity", "Background of the module list", 0.1, 1.0,
                                () -> Config.d.hudOpacity, v -> Config.d.hudOpacity = (float) v, "%.2f"),
                        new SliderRow("Y offset", "Move the module list up/down", -40, 100,
                                () -> Config.d.hudYOffset, v -> Config.d.hudYOffset = (int) Math.round(v), "%.0f")
                )),
                new GroupRow("Colors", "Colors and animated patterns", List.of(
                        new CycleRow("Pattern", "Static, rainbow, gradient or pulse",
                                () -> Theme.COLOR_MODES[Math.floorMod(Config.d.colorMode, Theme.COLOR_MODES.length)],
                                () -> Config.d.colorMode = (Config.d.colorMode + 1) % Theme.COLOR_MODES.length),
                        new CycleRow("Color", "Main accent color",
                                () -> Theme.accentName(Config.d.accentIndex),
                                () -> Config.d.accentIndex = (Config.d.accentIndex + 1) % Theme.accentCount()),
                        new CycleRow("Color 2", "Second color (gradient pattern)",
                                () -> Theme.accentName(Config.d.accent2Index),
                                () -> Config.d.accent2Index = (Config.d.accent2Index + 1) % Theme.accentCount()),
                        new SliderRow("Custom R", "Red of the Custom color", 0, 255,
                                () -> Config.d.customR, v -> Config.d.customR = (int) Math.round(v), "%.0f"),
                        new SliderRow("Custom G", "Green of the Custom color", 0, 255,
                                () -> Config.d.customG, v -> Config.d.customG = (int) Math.round(v), "%.0f"),
                        new SliderRow("Custom B", "Blue of the Custom color", 0, 255,
                                () -> Config.d.customB, v -> Config.d.customB = (int) Math.round(v), "%.0f"),
                        new SliderRow("Anim speed", "Speed of animated patterns", 0.2, 3.0,
                                () -> Config.d.colorSpeed, v -> Config.d.colorSpeed = (float) v, "%.1f")
                )),
                new GroupRow("Sidebar", "Customize the scoreboard sidebar", List.of(
                        new ToggleRow("Custom sidebar", "Use CleanClient's sidebar style",
                                () -> Config.d.sidebarCustom, v -> Config.d.sidebarCustom = v),
                        new ToggleRow("Hide scores", "Hide the numbers on the sidebar",
                                () -> Config.d.sidebarHideScores, v -> Config.d.sidebarHideScores = v),
                        new SliderRow("Y offset", "Move the sidebar up/down", -150, 150,
                                () -> Config.d.sidebarYOffset, v -> Config.d.sidebarYOffset = (int) Math.round(v), "%.0f")
                )),
                new GroupRow("Name spoof", "Swap your name in the sidebar (visual only)", List.of(
                        new ToggleRow("Enabled", "Replace your name in the sidebar text",
                                () -> Config.d.nameSpoofEnabled, v -> Config.d.nameSpoofEnabled = v),
                        new FieldRow(() -> "Name shown instead", "The name you will see",
                                box(Config.d.spoofName, 16, s -> Config.d.spoofName = s))
                ))
        );

        panels.add(new Panel("Visuals", 142, visuals));
        panels.add(new Panel("World", 142, world));
        panels.add(new Panel("Player", 142, player));
        panels.add(new Panel("Macros", 170, macros));
        panels.add(new Panel("HUD", 142, hud));

        int nextX = 12, rowY = TOP;
        for (Panel p : panels) {
            int[] saved = POS.get(p.title);
            if (saved != null) {
                p.x = saved[0];
                p.y = saved[1];
            } else {
                if (nextX > 12 && nextX + p.w > width - 8) {
                    nextX = 12;
                    rowY += 150;
                }
                p.x = nextX;
                p.y = rowY;
                POS.put(p.title, new int[]{p.x, p.y});
            }
            nextX += p.w + GAP;
        }
    }

    // ================================================================ row helpers

    private int childrenHeight(List<Row> rows) {
        int h = 0;
        for (Row r : rows) h += r.height();
        return h;
    }

    private void drawChildren(GuiGraphics g, List<Row> rows, int x, int y, int w, int mx, int my) {
        for (Row r : rows) {
            r.draw(g, x, y, w, mx, my);
            y += r.height();
        }
    }

    private Row pressChildren(List<Row> rows, double mx, double my, int button, int x, int y, int w) {
        for (Row r : rows) {
            int h = r.height();
            if (my >= y && my < y + h) return r.press(mx, my, button, x, y, w);
            y += h;
        }
        return null;
    }

    private String describeChildren(List<Row> rows, double mx, double my, int x, int y, int w) {
        for (Row r : rows) {
            int h = r.height();
            if (my >= y && my < y + h) return r.describe(mx, my, x, y, w);
            y += h;
        }
        return null;
    }

    // ================================================================ rows

    private abstract class Row {
        final String label, desc;
        float hv;

        Row(String label, String desc) {
            this.label = label;
            this.desc = desc;
        }

        int height() { return ROW_H; }
        boolean isSlider() { return false; }

        abstract void draw(GuiGraphics g, int x, int y, int w, int mx, int my);

        /** Returns the row that consumed the click, or null if the click should fall through. */
        Row press(double mx, double my, int button, int x, int y, int w) { return null; }

        void drag(double mx) {}

        String describe(double mx, double my, int x, int y, int w) { return desc; }

        boolean hovered(int mx, int my, int x, int y, int w, int h) {
            boolean over = mx >= x && mx < x + w && my >= y && my < y + h;
            hv = Mth.lerp(0.25f, hv, over ? 1f : 0f);
            return over;
        }

        void background(GuiGraphics g, int x, int y, int w, int h) {
            g.fill(x + 2, y, x + w - 2, y + h, Theme.lerpColor(Theme.ROW, Theme.ROW_HOVER, hv));
        }

        void pill(GuiGraphics g, int x, int y, int w, float anim) {
            int pw = 22, ph = 10, pxx = x + w - pw - 8, pyy = y + (ROW_H - ph) / 2;
            g.fill(pxx, pyy, pxx + pw, pyy + ph, Theme.lerpColor(Theme.OFF_PILL, Theme.accent(), anim));
            int knob = pxx + 1 + (int) ((pw - ph) * anim);
            g.fill(knob, pyy + 1, knob + ph - 2, pyy + ph - 1, Theme.TEXT);
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
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, ROW_H);
            float target = get.getAsBoolean() ? 1f : 0f;
            if (!init) { anim = target; init = true; }
            anim = Mth.lerp(0.25f, anim, target);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 8, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(anim, hv)));
            pill(g, x, y, w, anim);
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (button != 0) return null;
            set.accept(!get.getAsBoolean());
            return this;
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
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, ROW_H);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 8, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, hv));
            String v = value.get();
            g.drawString(font, v, x + w - 8 - font.width(v), y + 7, Theme.accent());
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (button != 0) return null;
            next.run();
            return this;
        }
    }

    private class SliderRow extends Row {
        final double min, max;
        final DoubleSupplier get;
        final DoubleConsumer set;
        final String fmt;
        int barX, barW = 1;

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
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, SLIDER_H);
            background(g, x, y, w, SLIDER_H);
            double v = get.getAsDouble();
            g.drawString(font, label, x + 8, y + 4, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, hv));
            String txt = String.format(fmt, v);
            g.drawString(font, txt, x + w - 8 - font.width(txt), y + 4, Theme.TEXT_DIM);

            barX = x + 8;
            barW = Math.max(1, w - 16);
            int by = y + 18;
            float t = (float) Mth.clamp((v - min) / (max - min), 0.0, 1.0);
            int fillW = (int) (barW * t);
            g.fill(barX, by, barX + barW, by + 3, Theme.OFF_PILL);
            g.fill(barX, by, barX + fillW, by + 3, Theme.accent());
            g.fill(barX + fillW - 2, by - 2, barX + fillW + 2, by + 5, Theme.TEXT);
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (button != 0) return null;
            drag(mx);
            return this;
        }

        @Override
        void drag(double mx) {
            double t = Mth.clamp((mx - barX) / (double) barW, 0.0, 1.0);
            set.accept(min + t * (max - min));
        }
    }

    private class ButtonRow extends Row {
        final Runnable action;

        ButtonRow(String label, String desc, Runnable action) {
            super(label, desc);
            this.action = action;
        }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, ROW_H);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + (w - font.width(label)) / 2, y + 7, Theme.lerpColor(Theme.accent(), Theme.TEXT, hv));
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (button != 0) return null;
            action.run();
            return this;
        }
    }

    private class InfoRow extends Row {
        final Supplier<String> text;

        InfoRow(Supplier<String> text) {
            super("", "");
            this.text = text;
        }

        @Override int height() { return INFO_H; }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            g.drawString(font, font.plainSubstrByWidth(text.get(), w - 12), x + 8, y + 3, Theme.TEXT_DIM);
        }

        @Override String describe(double mx, double my, int x, int y, int w) { return null; }
    }

    /** Label on top, text box underneath. */
    private class FieldRow extends Row {
        final Supplier<String> labelText;
        final EditBox box;

        FieldRow(Supplier<String> labelText, String desc, EditBox box) {
            super("", desc);
            this.labelText = labelText;
            this.box = box;
        }

        @Override int height() { return FIELD_H; }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, FIELD_H);
            background(g, x, y, w, FIELD_H);
            g.drawString(font, font.plainSubstrByWidth(labelText.get(), w - 12), x + 8, y + 3, Theme.TEXT_DIM);
            box.setX(x + 6);
            box.setY(y + 15);
            box.setWidth(w - 12);
            box.visible = true;
            box.render(g, lastMouseX, lastMouseY, 0f);
        }
        // press() returns null so the click reaches the text box
    }

    /** A collapsible group of settings (no toggle of its own). */
    private class GroupRow extends Row {
        final List<Row> children;

        GroupRow(String label, String desc, List<Row> children) {
            super(label, desc);
            this.children = children;
        }

        boolean open() { return OPEN.contains("g:" + label); }

        @Override
        int height() { return ROW_H + (open() ? childrenHeight(children) : 0); }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, ROW_H);
            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 8, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(hv, open() ? 1f : 0f)));
            g.drawString(font, open() ? "v" : ">", x + w - 14, y + 7, Theme.TEXT_DIM);
            if (open()) drawChildren(g, children, x + 6, y + ROW_H, w - 6, mx, my);
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (my < y + ROW_H) {
                if (button != 0) return null;
                String k = "g:" + label;
                if (!OPEN.remove(k)) OPEN.add(k);
                return this;
            }
            return open() ? pressChildren(children, mx, my, button, x + 6, y + ROW_H, w - 6) : null;
        }

        @Override
        String describe(double mx, double my, int x, int y, int w) {
            if (my < y + ROW_H || !open()) return desc;
            return describeChildren(children, mx, my, x + 6, y + ROW_H, w - 6);
        }
    }

    /** A module: left-click toggles it, right-click opens its settings. */
    private class ModuleRow extends Row {
        final Module module;
        final List<Row> children;
        float anim;
        boolean init;

        ModuleRow(Module module, String desc, List<Row> children) {
            super(module.getName(), desc);
            this.module = module;
            this.children = children;
        }

        String key() { return "m:" + module.getName(); }
        boolean open() { return !children.isEmpty() && OPEN.contains(key()); }

        @Override
        int height() { return ROW_H + (open() ? childrenHeight(children) : 0); }

        @Override
        void draw(GuiGraphics g, int x, int y, int w, int mx, int my) {
            hovered(mx, my, x, y, w, ROW_H);
            float target = module.isEnabled() ? 1f : 0f;
            if (!init) { anim = target; init = true; }
            anim = Mth.lerp(0.25f, anim, target);

            background(g, x, y, w, ROW_H);
            g.drawString(font, label, x + 8, y + 7, Theme.lerpColor(Theme.TEXT_DIM, Theme.TEXT, Math.max(anim, hv)));
            if (anim > 0.02f) {
                g.fill(x + 2, y + 3, x + 4, y + ROW_H - 3, ((int) (anim * 255) << 24) | (Theme.accent() & 0xFFFFFF));
            }
            if (!children.isEmpty()) g.drawString(font, open() ? "v" : ">", x + w - 42, y + 7, Theme.TEXT_DIM);
            pill(g, x, y, w, anim);

            if (open()) drawChildren(g, children, x + 6, y + ROW_H, w - 6, mx, my);
        }

        @Override
        Row press(double mx, double my, int button, int x, int y, int w) {
            if (my < y + ROW_H) {
                if (button == 0) {
                    module.toggle();
                    return this;
                }
                if (button == 1 && !children.isEmpty()) {
                    if (!OPEN.remove(key())) OPEN.add(key());
                    return this;
                }
                return null;
            }
            return open() ? pressChildren(children, mx, my, button, x + 6, y + ROW_H, w - 6) : null;
        }

        @Override
        String describe(double mx, double my, int x, int y, int w) {
            if (my < y + ROW_H || !open()) {
                return children.isEmpty() ? desc : desc + "  (right-click: settings)";
            }
            return describeChildren(children, mx, my, x + 6, y + ROW_H, w - 6);
        }
    }

    // ================================================================ rendering

    private int lastMouseX, lastMouseY;

    @Override
    public void render(GuiGraphics g, int mx, int my, float delta) {
        lastMouseX = mx;
        lastMouseY = my;
        for (EditBox b : boxes) b.visible = false;

        g.fill(0, 0, width, height, Theme.DIM_SCREEN);
        Logo.draw(g, (width - Logo.width(3)) / 2, 8, 3);

        int accent = Theme.accent();
        for (Panel p : panels) {
            p.x = Mth.clamp(p.x, 0, Math.max(0, width - p.w));
            p.y = Mth.clamp(p.y, 0, Math.max(0, height - HEADER_H));
            int h = p.height();

            // soft shadow
            g.fill(p.x + 3, p.y + 3, p.x + p.w + 3, p.y + h + 3, 0x30000000);
            g.fill(p.x + 1, p.y + 1, p.x + p.w + 1, p.y + h + 1, 0x30000000);
            // body + faint outline in the accent color
            g.fill(p.x, p.y, p.x + p.w, p.y + h, Theme.BG);
            int edge = (0x44 << 24) | (accent & 0xFFFFFF);
            g.fill(p.x, p.y, p.x + p.w, p.y + 1, edge);
            g.fill(p.x, p.y + h - 1, p.x + p.w, p.y + h, edge);
            g.fill(p.x, p.y, p.x + 1, p.y + h, edge);
            g.fill(p.x + p.w - 1, p.y, p.x + p.w, p.y + h, edge);
            // header: slight vertical gradient + animated accent line
            for (int band = 0; band < 3; band++) {
                g.fill(p.x + 1, p.y + band * 6, p.x + p.w - 1, p.y + (band + 1) * 6,
                        Theme.lerpColor(0xFF22222C, Theme.HEADER, band / 2f));
            }
            int seg = Math.max(1, p.w / 8);
            for (int sx = 0; sx < p.w; sx += seg) {
                g.fill(p.x + sx, p.y + HEADER_H - 2, Math.min(p.x + sx + seg, p.x + p.w), p.y + HEADER_H,
                        Theme.dynamic(sx / (float) p.w * 0.6f));
            }
            g.drawString(font, p.title, p.x + 8, p.y + 5, Theme.TEXT);

            int on = 0, total = 0;
            for (Row r : p.rows) {
                if (r instanceof ModuleRow mr) {
                    total++;
                    if (mr.module.isEnabled()) on++;
                }
            }
            if (total > 0) {
                String badge = on + "/" + total;
                g.drawString(font, badge, p.x + p.w - 22 - font.width(badge), p.y + 5,
                        on > 0 ? accent : Theme.TEXT_DIM);
            }
            g.drawString(font, p.collapsed() ? ">" : "v", p.x + p.w - 14, p.y + 5, Theme.TEXT_DIM);

            if (!p.collapsed()) drawChildren(g, p.rows, p.x, p.y + HEADER_H + 2, p.w, mx, my);
        }

        // hint line at the bottom of the screen
        String hint = null;
        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (mx < p.x || mx >= p.x + p.w || my < p.y || my >= p.y + p.height()) continue;
            if (my < p.y + HEADER_H) hint = "Click to open/close, drag to move";
            else if (!p.collapsed()) hint = describeChildren(p.rows, mx, my, p.x, p.y + HEADER_H + 2, p.w);
            break;
        }
        String text = hint == null || hint.isEmpty() ? "Esc to close" : hint;
        g.fill(0, height - 20, width, height, 0x88000000);
        g.fill(0, height - 20, width, height - 19, (0x66 << 24) | (accent & 0xFFFFFF));
        g.drawString(font, text, (width - font.width(text)) / 2, height - 13, Theme.TEXT_DIM);
    }

    // ================================================================ input

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x(), my = event.y();
        int button = event.button();
        for (EditBox b : boxes) b.setFocused(false);

        for (int i = panels.size() - 1; i >= 0; i--) {
            Panel p = panels.get(i);
            if (mx < p.x || mx >= p.x + p.w || my < p.y || my >= p.y + p.height()) continue;

            if (my < p.y + HEADER_H) {
                if (button == 0) {
                    dragPanel = p;
                    dragOffX = (int) mx - p.x;
                    dragOffY = (int) my - p.y;
                    pressX = (int) mx;
                    pressY = (int) my;
                    dragMoved = false;
                }
                return true;
            }
            if (!p.collapsed()) {
                Row r = pressChildren(p.rows, mx, my, button, p.x, p.y + HEADER_H + 2, p.w);
                if (r != null) {
                    if (r.isSlider()) activeSlider = r;
                    return true;
                }
            }
            break; // clicked inside a panel on something that doesn't consume it (a text box)
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragPanel != null) {
            int nx = (int) event.x(), ny = (int) event.y();
            if (Math.abs(nx - pressX) > 3 || Math.abs(ny - pressY) > 3) dragMoved = true;
            if (dragMoved) {
                dragPanel.x = nx - dragOffX;
                dragPanel.y = ny - dragOffY;
                POS.put(dragPanel.title, new int[]{dragPanel.x, dragPanel.y});
            }
            return true;
        }
        if (activeSlider != null) {
            activeSlider.drag(event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        if (dragPanel != null && !dragMoved) {
            if (!COLLAPSED.remove(dragPanel.title)) COLLAPSED.add(dragPanel.title);
        }
        dragPanel = null;
        activeSlider = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
