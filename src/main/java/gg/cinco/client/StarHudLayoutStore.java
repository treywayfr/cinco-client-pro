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
    private static final Path POLISH_MARKER = FabricLoader.getInstance().getConfigDir().resolve("star-hud-polish-v3.flag");
    private static final Map<String, State> STATES = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean polishedChecked;

    private StarHudLayoutStore() {}

    public static State get(String id, int defaultX, int defaultY) {
        ensureLoaded();
        return STATES.computeIfAbsent(id, key -> new State(defaultX, defaultY, 1.0f));
    }

    /** One-time migration from older overlapping defaults into the polished layout. */
    public static void applyPolishedDefaults(int screenW, int screenH) {
        ensureLoaded();
        if (polishedChecked) return;
        polishedChecked = true;
        if (Files.exists(POLISH_MARKER)) return;
        STATES.put("brand", new State(12, 12, 1.0f));
        STATES.put("coords", new State(12, 43, 1.0f));
        STATES.put("region_map", new State(12, 76, 1.0f));
        STATES.put("potion_hud", new State(12, 205, 1.0f));
        STATES.put("status_bar", new State(Math.max(12, screenW / 2 - 120), 12, 1.0f));
        STATES.put("spotify_hud", new State(Math.max(12, screenW - 300), 12, 1.0f));
        STATES.put("module_list", new State(Math.max(12, screenW - 196), 142, 1.0f));
        String[] utility={"fps","direction","armor","potions","cps","session","keys","ping","target","inventory","radar","players"};
        int x=12,y=Math.max(12,screenH-58);
        for(int i=0;i<utility.length;i++){
            STATES.put(utility[i],new State(x,y,0.92f));
            x+=126;
            if(x>screenW-125){x=12;y-=28;}
        }
        save();
        try { Files.createDirectories(POLISH_MARKER.getParent()); Files.writeString(POLISH_MARKER,"3"); } catch (Exception ignored) {}
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
