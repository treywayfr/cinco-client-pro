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
    private static final Path POLISH_MARKER = FabricLoader.getInstance().getConfigDir().resolve("star-hud-reference-v4.flag");
    private static final Map<String, State> STATES = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean polishedChecked;

    private StarHudLayoutStore() {}

    public static State get(String id, int defaultX, int defaultY) {
        ensureLoaded();
        return STATES.computeIfAbsent(id, key -> new State(defaultX, defaultY, 1.0f));
    }

    /** One-time migration to the exact reference composition. */
    public static void applyPolishedDefaults(int screenW, int screenH) {
        ensureLoaded();
        if (polishedChecked) return;
        polishedChecked = true;
        if (Files.exists(POLISH_MARKER)) return;

        int left=12;
        STATES.put("brand", new State(left, 12, 1.0f));
        STATES.put("coords", new State(left, 55, 1.0f));
        STATES.put("region_map", new State(left, 91, 1.0f));
        STATES.put("potion_hud", new State(left, 228, 1.0f));
        STATES.put("fps", new State(left, 274, 1.0f));
        STATES.put("armor", new State(left, 311, 1.0f));
        STATES.put("inventory", new State(left, 348, 1.0f));

        STATES.put("status_bar", new State(Math.max(12,screenW/2-148), 12, 1.0f));
        STATES.put("spotify_hud", new State(Math.max(12,screenW-282), 12, 1.0f));
        STATES.put("module_list", new State(Math.max(12,screenW-188), 126, 1.0f));

        // Less-prominent utility widgets start tucked near the lower-left instead of colliding.
        String[] utility={"direction","potions","cps","session","keys","ping","target","radar","players"};
        int x=12,y=Math.max(390,screenH-48);
        for(int i=0;i<utility.length;i++){
            STATES.put(utility[i],new State(x,y,0.90f));
            x+=118;
            if(x>screenW-115){x=12;y-=27;}
        }
        save();
        try { Files.createDirectories(POLISH_MARKER.getParent()); Files.writeString(POLISH_MARKER,"4"); } catch (Exception ignored) {}
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
