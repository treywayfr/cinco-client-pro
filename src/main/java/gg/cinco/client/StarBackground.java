package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Shared black/purple animated starfield used by every Star Client GUI screen.
 * Positions are time-based rather than screen-instance state, so opening a module/settings page
 * cannot reset or make the stars disappear.
 */
public final class StarBackground {
    private static final int STAR_COUNT = 128;
    private static final Star[] STARS = new Star[STAR_COUNT];

    private static final class Star {
        final double x;
        final double y;
        final double phase;
        final double fallSpeed;
        final double driftSpeed;
        final int size;
        final int shade;

        Star(double x, double y, double phase, double fallSpeed, double driftSpeed, int size, int shade) {
            this.x = x;
            this.y = y;
            this.phase = phase;
            this.fallSpeed = fallSpeed;
            this.driftSpeed = driftSpeed;
            this.size = size;
            this.shade = shade;
        }
    }

    static {
        // Deterministic distribution: dense enough to feel alive, but no allocations while rendering.
        long seed = 0x57A2C11EL;
        for (int i = 0; i < STAR_COUNT; i++) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double x = ((seed >>> 12) & 0xFFFF) / 65535.0;
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            double y = ((seed >>> 12) & 0xFFFF) / 65535.0;
            double phase = i * 0.731 + ((seed >>> 36) & 0xFF) * 0.031;
            double fall = 7.0 + (i % 9) * 1.35;
            double drift = 0.45 + (i % 7) * 0.10;
            int size = i % 11 == 0 ? 2 : 1;
            STARS[i] = new Star(x, y, phase, fall, drift, size, i % 3);
        }
    }

    private StarBackground() {}

    public static void render(DrawContext context, int width, int height, float delta) {
        if (width <= 0 || height <= 0) return;
        ClientModule starsModule = CincoClient.findModule("Purple Stars");
        if (starsModule != null && !starsModule.enabled) return;

        MinecraftClient client = MinecraftClient.getInstance();
        int cap = Math.min(STAR_COUNT, StarPerformance.guiStarCap(client));
        if (starsModule != null) {
            // Existing Purple Stars setting still controls density, but full density now means the
            // whole persistent field instead of the old 42-star local list.
            double fraction = Math.max(0.20, Math.min(1.0, starsModule.setting / Math.max(1.0, starsModule.max)));
            cap = Math.min(cap, Math.max(20, (int) Math.round(STAR_COUNT * fraction)));
        }

        int tier = StarPerformance.animationTier(client);
        double seconds = System.nanoTime() / 1_000_000_000.0;
        for (int i = 0; i < cap; i++) {
            Star star = STARS[i];
            double motion = tier == 0 ? 0.0 : seconds;
            double px = star.x * width + Math.sin(motion * star.driftSpeed + star.phase) * (2.5 + star.size);
            double py = star.y * height + motion * star.fallSpeed;
            px %= width;
            py %= height;
            if (px < 0) px += width;
            if (py < 0) py += height;

            double twinkle = tier == 0 ? 0.45 : (Math.sin(seconds * 2.0 + star.phase) + 1.0) * 0.5;
            int alpha = 64 + (int) Math.round(twinkle * 126.0);
            int rgb = switch (star.shade) {
                case 1 -> 0xB76CFF;
                case 2 -> 0x7C3AED;
                default -> 0x9B5CFF;
            };
            int color = (Math.max(0, Math.min(255, alpha)) << 24) | rgb;
            int x = (int) Math.round(px);
            int y = (int) Math.round(py);
            context.fill(x, y, Math.min(width, x + star.size), Math.min(height, y + star.size), color);
        }
    }
}
