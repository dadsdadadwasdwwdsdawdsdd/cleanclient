package com.cleanclient.module;

import net.minecraft.client.Minecraft;

public abstract class Module {
    private final String name;
    private final String description;
    private boolean enabled;

    protected final Minecraft mc = Minecraft.getInstance();

    protected Module(String name, String description) {
        this.name = name;
        this.description = description;
    }

    public String getName() { return name; }
    public String getDescription() { return description; }
    public boolean isEnabled() { return enabled; }

    public void toggle() { setEnabled(!enabled); }

    public void setEnabled(boolean value) {
        if (enabled == value) return;
        enabled = value;
        if (value) onEnable(); else onDisable();
    }

    protected void onEnable() {}
    protected void onDisable() {}
    public void onTick(Minecraft mc) {}
}
