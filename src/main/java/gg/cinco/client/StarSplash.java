package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Short, non-blocking Star Client intro shown only after entering a world. */
public final class StarSplash {
    private static long worldStart;
    private static int worldId;
    private StarSplash() {}

    public static void render(DrawContext ctx, MinecraftClient client) {
        if (client == null || client.player == null || client.world == null || client.currentScreen != null) return;
        int id = System.identityHashCode(client.world);
        if (id != worldId) {
            worldId = id;
            worldStart = System.currentTimeMillis();
        }
        long age = System.currentTimeMillis() - worldStart;
        if (age < 0 || age > 1350) return;

        float in = Math.min(1f, age / 220f);
        float out = Math.min(1f, (1350f - age) / 300f);
        float alphaF = Math.max(0f, Math.min(in, out));
        alphaF = alphaF * alphaF * (3f - 2f * alphaF);
        int alpha = Math.max(0, Math.min(255, Math.round(alphaF * 235f)));
        if (alpha <= 2) return;

        String title = "★  STAR CLIENT  ★";
        String sub = "NEBULA LUXE";
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        int tw = client.textRenderer.getWidth(title);
        int x = sw / 2;
        int y = sh / 2 - 26;

        int panel = (Math.min(190, alpha) << 24) | 0x06040B;
        int border = (Math.min(215, alpha) << 24) | (CincoTheme.magenta() & 0x00FFFFFF);
        CincoDraw.softShadow(ctx, x - tw / 2 - 18, y - 11, x + tw / 2 + 18, y + 34,
                12, CincoTheme.accent(), Math.min(44, alpha / 4));
        CincoDraw.bubble(ctx, x - tw / 2 - 18, y - 11, x + tw / 2 + 18, y + 34, 12, border, panel);
        CincoDraw.rounded(ctx, x - tw / 2 - 6, y + 28, x + tw / 2 + 6, y + 30, 1,
                (Math.min(alpha, 205) << 24) | (CincoTheme.cyan() & 0x00FFFFFF));
        ctx.drawCenteredTextWithShadow(client.textRenderer, StarFonts.text(title, true), x, y,
                (alpha << 24) | (CincoTheme.starGold() & 0x00FFFFFF));
        ctx.drawCenteredTextWithShadow(client.textRenderer, StarFonts.text(sub, false), x, y + 16,
                (Math.min(alpha, 205) << 24) | (CincoTheme.accentLight() & 0x00FFFFFF));
    }
}
