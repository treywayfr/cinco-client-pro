package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Lightweight glass frame around the vanilla hotbar to match the approved HUD reference. */
public final class StarHotbarFrame {
    private StarHotbarFrame() {}

    public static void render(DrawContext c, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || !CincoClient.hudEnabled) return;
        ClientModule hud = CincoClient.findModule("HUD");
        if (hud == null || !hud.enabled) return;

        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int w = 184, h = 24;
        int x = sw / 2 - w / 2;
        int y = sh - 25;

        CincoDraw.softShadow(c,x,y,x+w,y+h,10,0xFF55D8,24);
        CincoDraw.bubble(c,x,y,x+w,y+h,10,0x94C887EF,0x300B0714);

        int selected = Math.max(0,Math.min(8,client.player.getInventory().getSelectedSlot()));
        int sx = x + 3 + selected * 20;
        CincoDraw.softShadow(c,sx,y+2,sx+20,y+h-2,7,0xFF55D8,34);
        CincoDraw.bubble(c,sx,y+2,sx+20,y+h-2,7,0xDFFF6ADD,0x1610081A);
    }
}
