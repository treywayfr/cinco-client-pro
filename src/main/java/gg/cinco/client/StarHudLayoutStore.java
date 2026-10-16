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
    private static final Path POLISH_MARKER = FabricLoader.getInstance().getConfigDir().resolve("star-hud-reference-v5.flag");
    private static final Map<String, State> STATES = new LinkedHashMap<>();
    private static boolean loaded;
    private static boolean polishedChecked;

    private StarHudLayoutStore() {}

    public static State get(String id, int defaultX, int defaultY) {
        ensureLoaded();
        return STATES.computeIfAbsent(id, key -> new State(defaultX, defaultY, 1.0f));
    }

    /** One-time migration to the approved HUD composition; user edits remain persistent afterward. */
    public static void applyPolishedDefaults(int screenW, int screenH) {
        ensureLoaded();
        if (polishedChecked) return;
        polishedChecked = true;
        if (Files.exists(POLISH_MARKER)) return;

        int left=14;
        STATES.put("brand", new State(left, 12, 1.0f));
        STATES.put("coords", new State(left, 54, 1.0f));
        STATES.put("region_map", new State(left, 88, 1.0f));
        STATES.put("potion_hud", new State(left, 218, 1.0f));
        STATES.put("fps", new State(left, 263, 1.0f));
        STATES.put("armor", new State(left, 298, 1.0f));
        STATES.put("radar", new State(left, 333, 1.0f));

        STATES.put("status_bar", new State(Math.max(12,screenW/2-146), 12, 1.0f));
        STATES.put("spotify_hud", new State(Math.max(12,screenW-236), 18, 1.0f));
        STATES.put("module_list", new State(Math.max(12,screenW-160), 116, 1.0f));

        // Optional utility widgets stay out of the reference stack unless the user moves them there.
        String[] utility={"direction","potions","cps","session","keys","ping","target","inventory","players"};
        int x=14,y=Math.max(376,screenH-38);
        for(String id:utility){
            STATES.put(id,new State(x,y,0.88f));
            x+=108;
            if(x>screenW-104){x=14;y-=28;}
        }

        save();
        try { Files.createDirectories(POLISH_MARKER.getParent()); Files.writeString(POLISH_MARKER,"5"); } catch (Exception ignored) {}
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
