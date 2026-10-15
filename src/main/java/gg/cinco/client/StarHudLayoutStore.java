package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class StarHudLayoutStore {
    public static final class State {
        public int x;
        public int y;
        public float scale;
        public State() {}
        public State(int x, int y, float scale) { this.x = x; this.y = y; this.scale = scale; }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-hud.json");
    private static final Map<String, State> STATES = new LinkedHashMap<>();
    private static boolean loaded;

    private StarHudLayoutStore() {}

    public static State get(String id, int defaultX, int defaultY) {
        ensureLoaded();
        return STATES.computeIfAbsent(id, key -> new State(defaultX, defaultY, 1.0f));
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(STATES));
        } catch (Exception ignored) {}
    }

    private static void ensureLoaded() {
        if (loaded) return;
        loaded = true;
        if (!Files.exists(FILE)) return;
        try {
            Type type = new TypeToken<Map<String, State>>() {}.getType();
            Map<String, State> read = GSON.fromJson(Files.readString(FILE), type);
            if (read != null) {
                for (Map.Entry<String, State> entry : read.entrySet()) {
                    State s = entry.getValue();
                    if (s == null) continue;
                    s.scale = Math.max(0.5f, Math.min(2.5f, s.scale <= 0 ? 1.0f : s.scale));
                    STATES.put(entry.getKey(), s);
                }
            }
        } catch (Exception ignored) {}
    }
}
