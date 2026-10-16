package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/** Persistent per-item Locker customization: color, size and animated rainbow. */
public final class LockerCosmeticStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-locker-items.json");
    private static final Map<String, ItemProfile> ITEMS = new HashMap<>();

    private LockerCosmeticStore() {}

    public static final class ItemProfile {
        public double hue = 276.0;
        public double saturation = 0.78;
        public double brightness = 1.0;
        public double size = 1.0;
        public boolean rainbow = false;
        public double rainbowSpeed = 0.35;
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Data d = GSON.fromJson(Files.readString(FILE), Data.class);
            if (d == null || d.items == null) return;
            ITEMS.clear();
            for (Map.Entry<String, ItemProfile> e : d.items.entrySet()) {
                if (e.getValue() != null) ITEMS.put(e.getKey(), sanitize(e.getValue()));
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Data d = new Data();
            d.items.putAll(ITEMS);
            Files.writeString(FILE, GSON.toJson(d));
        } catch (IOException ignored) {}
    }

    public static ItemProfile get(String kind, int style) {
        String key = key(kind, style);
        return ITEMS.computeIfAbsent(key, k -> defaults(kind, style));
    }

    public static CosmeticColorStore.Profile resolvedColor(String kind, int style) {
        ItemProfile p = get(kind, style);
        double hue = p.hue;
        if (p.rainbow) {
            double seconds = System.currentTimeMillis() / 1000.0;
            hue = (seconds * 120.0 * Math.max(0.05, p.rainbowSpeed) + style * 23.0) % 360.0;
        }
        return new CosmeticColorStore.Profile(hue, clamp01(p.saturation), clamp01(p.brightness));
    }

    public static double size(String kind, int style) {
        return Math.max(0.35, Math.min(2.75, get(kind, style).size));
    }

    private static ItemProfile defaults(String kind, int style) {
        ItemProfile p = new ItemProfile();
        p.hue = ((kind.equals("accessory") ? 276.0 : 288.0) + style * 7.0) % 360.0;
        return p;
    }

    private static ItemProfile sanitize(ItemProfile p) {
        p.hue = ((p.hue % 360.0) + 360.0) % 360.0;
        p.saturation = clamp01(p.saturation);
        p.brightness = clamp01(p.brightness);
        p.size = Math.max(0.35, Math.min(2.75, p.size));
        p.rainbowSpeed = Math.max(0.05, Math.min(2.5, p.rainbowSpeed));
        return p;
    }

    private static String key(String kind, int style) { return kind + ":" + style; }
    private static double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private static final class Data {
        Map<String, ItemProfile> items = new HashMap<>();
    }
}
