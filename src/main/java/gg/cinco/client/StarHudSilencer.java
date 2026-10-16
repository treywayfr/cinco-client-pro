package gg.cinco.client;

/** Short-lived HUD suppression used when Star macros temporarily change held items. */
public final class StarHudSilencer {
    private static volatile long silentUntilNanos;

    private StarHudSilencer() {}

    public static void silenceFor(int ticks) {
        long duration = Math.max(1, ticks) * 50_000_000L;
        long until = System.nanoTime() + duration;
        if (until > silentUntilNanos) silentUntilNanos = until;
    }

    public static boolean active() {
        return System.nanoTime() < silentUntilNanos;
    }
}
