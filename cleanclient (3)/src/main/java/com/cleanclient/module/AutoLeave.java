package com.cleanclient.module;

import com.cleanclient.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.chat.Component;

/** Disconnects from the server when you fall below a set Y level (default 0). */
public class AutoLeave extends Module {
    private boolean fired;

    public AutoLeave() {
        super("Auto Leave", "Disconnect below a chosen Y level");
    }

    @Override
    protected void onEnable() {
        fired = false;
    }

    @Override
    public void onTick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            fired = false; // reset once we are back at the menu
            return;
        }
        if (fired) return;
        // ignore the first couple of seconds after joining (position is not settled yet)
        if (mc.player.tickCount < 40) return;

        if (mc.player.getY() < Config.d.autoLeaveY) {
            fired = true;
            ClientPacketListener conn = mc.getConnection();
            if (conn != null) {
                conn.getConnection().disconnect(Component.literal(
                        "[CleanClient] Auto Leave: dropped below Y " + Config.d.autoLeaveY));
            }
        }
    }
}
