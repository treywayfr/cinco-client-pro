package gg.cinco.client;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Persists the user's selected Star Client GUI mode. */
public final class StarGuiModeStore {
    public enum Mode { ORBIT, NEBULA }

    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-gui-mode.txt");
    private static Mode mode;

    private StarGuiModeStore() {}

    public static Mode get() {
        if (mode != null) return mode;
        mode = Mode.NEBULA;
        try {
            if (Files.exists(FILE)) {
                String raw = Files.readString(FILE).trim();
                if (!raw.isEmpty()) mode = Mode.valueOf(raw);
            }
        } catch (Exception ignored) {}
        return mode;
    }

    public static void set(Mode next) {
        mode = next == null ? Mode.NEBULA : next;
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, mode.name());
        } catch (Exception ignored) {}
    }
}
