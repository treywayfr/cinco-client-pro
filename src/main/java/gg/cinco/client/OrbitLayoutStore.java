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

/** Saves Orbit HUD panel positions, sizes and collapsed state. */
public final class OrbitLayoutStore {
    public static final class State {
        public int x;
        public int y;
        public int w;
        public int h;
        public boolean collapsed;
        public State() {}
        public State(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.w = w; this.h = h;
        }
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-orbit-layout.json");
    private static final Map<String, State> STATES = new LinkedHashMap<>();
    private static boolean loaded;

    private OrbitLayoutStore() {}

    public static State get(String id, int x, int y, int w, int h) {
        ensureLoaded();
        State state = STATES.computeIfAbsent(id, key -> new State(x, y, w, h));
        state.w = Math.max(132, Math.min(360, state.w <= 0 ? w : state.w));
        state.h = Math.max(64, Math.min(520, state.h <= 0 ? h : state.h));
        return state;
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
            if (read != null) STATES.putAll(read);
        } catch (Exception ignored) {}
    }
}
