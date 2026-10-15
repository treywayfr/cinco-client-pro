package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

/**
 * Central animation language for Star Client's Nebula Luxe UI.
 *
 * Durations are intentionally short enough to feel immediate at high refresh rates while still
 * having visible motion. Performance Mode is allowed to shorten or snap non-essential effects so
 * animation never wins over game FPS.
 */
public final class StarAnimations {
    public enum Curve { CUBIC_OUT, CUBIC_IN_OUT, QUART_OUT, BACK_OUT, SMOOTH }

    // Nebula Luxe timing spec (milliseconds).
    public static final int MENU_OPEN_MS = 220;
    public static final int MENU_CLOSE_MS = 160;
    public static final int CATEGORY_MS = 190;
    public static final int HOVER_IN_MS = 115;
    public static final int HOVER_OUT_MS = 165;
    public static final int TOGGLE_MS = 175;
    public static final int CLICK_MS = 105;
    public static final int SEARCH_FOCUS_MS = 145;
    public static final int SLIDER_MS = 90;
    public static final int PANEL_MS = 180;

    private StarAnimations() {}

    /** A target-driven animation track that is frame-rate independent. */
    public static final class Track {
        private float value;
        private float from;
        private float target;
        private long startedAt;
        private int durationMs;
        private Curve curve = Curve.CUBIC_OUT;
        private boolean initialized;

        public Track(float initial) {
            value = initial;
            from = initial;
            target = initial;
            initialized = true;
        }

        public float to(float newTarget, int duration, Curve easing, long now) {
            if (!initialized) {
                value = from = target = newTarget;
                initialized = true;
            }
            if (Math.abs(newTarget - target) > 0.0001f) {
                value = sample(now);
                from = value;
                target = newTarget;
                startedAt = now;
                durationMs = Math.max(0, duration);
                curve = easing;
                if (durationMs == 0) value = target;
            }
            return sample(now);
        }

        public float sample(long now) {
            if (durationMs <= 0 || Math.abs(target - from) < 0.0001f) {
                value = target;
                return value;
            }
            float p = Math.max(0f, Math.min(1f, (now - startedAt) / (float) durationMs));
            float eased = ease(p, curve);
            value = from + (target - from) * eased;
            if (p >= 1f) {
                value = target;
                from = target;
                durationMs = 0;
            }
            return value;
        }
    }

    public static int duration(MinecraftClient client, int baseMs, boolean essential) {
        ClientModule animation = CincoClient.findModule("GUI Animation");
        if (animation != null && !animation.enabled) return 0;

        // GUI Animation's 220 default is the 1.0x Nebula Luxe pace.
        double pace = animation == null ? 1.0 : Math.max(0.50, Math.min(2.0, animation.setting / 220.0));
        int scaled = Math.max(1, (int) Math.round(baseMs * pace));

        int tier = StarPerformance.animationTier(client);
        if (tier >= 2) return scaled;
        if (tier == 1) return Math.max(65, Math.round(scaled * 0.72f));
        // Low-FPS mode keeps only functional motion, and makes it finish almost immediately.
        return essential ? Math.max(45, Math.round(scaled * 0.42f)) : 0;
    }

    public static float progress(long elapsedMs, int durationMs, Curve curve) {
        if (durationMs <= 0) return 1f;
        return ease(Math.max(0f, Math.min(1f, elapsedMs / (float) durationMs)), curve);
    }

    public static float ease(float p, Curve curve) {
        p = Math.max(0f, Math.min(1f, p));
        return switch (curve) {
            case CUBIC_OUT -> 1f - (float) Math.pow(1f - p, 3);
            case CUBIC_IN_OUT -> p < 0.5f
                    ? 4f * p * p * p
                    : 1f - (float) Math.pow(-2f * p + 2f, 3) / 2f;
            case QUART_OUT -> 1f - (float) Math.pow(1f - p, 4);
            case BACK_OUT -> {
                // Small overshoot only: enough to feel premium without looking bouncy/cartoonish.
                float c1 = 1.18f;
                float c3 = c1 + 1f;
                float q = p - 1f;
                yield 1f + c3 * q * q * q + c1 * q * q;
            }
            case SMOOTH -> p * p * (3f - 2f * p);
        };
    }

    public static float clickPulse(long clickedAt, long now, MinecraftClient client) {
        if (clickedAt <= 0L) return 0f;
        int duration = duration(client, CLICK_MS, false);
        if (duration <= 0) return 0f;
        long age = now - clickedAt;
        if (age < 0 || age >= duration) return 0f;
        float p = age / (float) duration;
        // 0 -> 1 -> 0 with smooth ends.
        return (float) Math.sin(Math.PI * ease(p, Curve.SMOOTH));
    }
}
