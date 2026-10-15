package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Lightweight desktop-media bridge for the Spotify HUD.
 * It never blocks Minecraft's render thread; OS media/window queries run on one daemon worker.
 */
public final class SpotifyHudController {
    private static final ExecutorService WORKER = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "star-spotify-hud");
        t.setDaemon(true);
        return t;
    });
    private static final AtomicBoolean polling = new AtomicBoolean();
    private static volatile long nextPoll;
    private static volatile String track = "Waiting for Spotify";
    private static volatile boolean active;

    private SpotifyHudController() {}

    public static void tick(MinecraftClient client) {
        ClientModule module = CincoClient.findModule("Spotify HUD");
        if (module == null || !module.enabled) return;
        long now = System.currentTimeMillis();
        if (now < nextPoll || !polling.compareAndSet(false, true)) return;
        long refresh = (long) (Math.max(1.0, Math.min(10.0, module.setting)) * 1000.0);
        nextPoll = now + refresh;
        WORKER.execute(() -> {
            try {
                String found = queryNowPlaying();
                if (found == null || found.isBlank()) {
                    active = false;
                    track = "Waiting for Spotify";
                } else {
                    active = true;
                    track = clean(found);
                }
            } catch (Throwable ignored) {
                active = false;
                track = "Spotify unavailable";
            } finally {
                polling.set(false);
            }
        });
    }

    public static boolean active() { return active; }
    public static String track() { return track; }
    public static String hudText() { return "♫  " + track; }

    private static String queryNowPlaying() throws Exception {
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Process process;
        if (os.contains("win")) {
            String script = "$p=Get-Process Spotify -ErrorAction SilentlyContinue | "
                    + "Where-Object {$_.MainWindowTitle -and $_.MainWindowTitle -notmatch '^Spotify( Premium)?$'} | "
                    + "Select-Object -First 1; if($p){$p.MainWindowTitle}";
            process = new ProcessBuilder("powershell", "-NoProfile", "-Command", script)
                    .redirectErrorStream(true).start();
        } else if (os.contains("mac")) {
            String script = "tell application \"Spotify\" to if player state is playing then return (artist of current track) & \" — \" & (name of current track)";
            process = new ProcessBuilder("osascript", "-e", script).redirectErrorStream(true).start();
        } else {
            process = new ProcessBuilder("sh", "-c",
                    "playerctl -p spotify metadata --format '{{artist}} — {{title}}' 2>/dev/null")
                    .redirectErrorStream(true).start();
        }

        if (!process.waitFor(1400, TimeUnit.MILLISECONDS)) {
            process.destroyForcibly();
            return null;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line = reader.readLine();
            return line == null ? null : line.trim();
        }
    }

    private static String clean(String value) {
        String s = value.replace('\t', ' ').replaceAll("\\s+", " ").trim();
        if (s.length() > 72) s = s.substring(0, 69) + "…";
        return s;
    }
}
