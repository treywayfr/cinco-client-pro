package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;

import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/** Persistent per-storage-type ESP configuration. */
public final class StorageEspConfig {
    public enum TypeKey {
        CHESTS("Chests", 0xFFFFDF67),
        BARRELS("Barrels", 0xFFFFA94F),
        SHULKERS("Shulkers", 0xFFD98CFF),
        ENDER_CHESTS("Ender Chests", 0xFF52FFF2),
        HOPPERS("Hoppers", 0xFFA9C7FF),
        FURNACES("Furnaces", 0xFFFF7D45),
        DROPPERS("Dispensers / Droppers", 0xFFFF7EDB),
        SPAWNERS("Spawners", 0xFFFF4F6F);

        public final String label;
        public final int defaultColor;
        TypeKey(String label, int defaultColor) {
            this.label = label;
            this.defaultColor = defaultColor;
        }
    }

    public static final class Entry {
        public boolean highlight = true;
        public boolean tracer = false;
        public int color;
        public Entry() {}
        Entry(int color) { this.color = color; }
    }

    private static final int[] COLOR_PALETTE = {
            0xFFFFDF67, 0xFFFFA94F, 0xFFFF675F, 0xFFFF4F9A,
            0xFFD98CFF, 0xFF9C7DFF, 0xFF63A7FF, 0xFF52FFF2,
            0xFF62FF9A, 0xFFA9FF68, 0xFFFFFFFF, 0xFFA9C7FF
    };
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cinco-storage-esp.json");
    private static final Map<String, Entry> SETTINGS = new LinkedHashMap<>();

    static { resetDefaults(false); }
    private StorageEspConfig() {}

    public static Entry get(TypeKey type) {
        return SETTINGS.computeIfAbsent(type.name(), k -> new Entry(type.defaultColor));
    }

    public static TypeKey typeOf(BlockState state) {
        if (state.isOf(Blocks.CHEST) || state.isOf(Blocks.TRAPPED_CHEST)) return TypeKey.CHESTS;
        if (state.isOf(Blocks.BARREL)) return TypeKey.BARRELS;
        if (state.getBlock() instanceof ShulkerBoxBlock) return TypeKey.SHULKERS;
        if (state.isOf(Blocks.ENDER_CHEST)) return TypeKey.ENDER_CHESTS;
        if (state.isOf(Blocks.HOPPER)) return TypeKey.HOPPERS;
        if (state.isOf(Blocks.FURNACE) || state.isOf(Blocks.BLAST_FURNACE) || state.isOf(Blocks.SMOKER)) return TypeKey.FURNACES;
        if (state.isOf(Blocks.DISPENSER) || state.isOf(Blocks.DROPPER)) return TypeKey.DROPPERS;
        if (state.isOf(Blocks.SPAWNER)) return TypeKey.SPAWNERS;
        return null;
    }

    public static void cycleColor(TypeKey type, int direction) {
        Entry entry = get(type);
        int current = 0;
        for (int i = 0; i < COLOR_PALETTE.length; i++) {
            if ((COLOR_PALETTE[i] & 0x00FFFFFF) == (entry.color & 0x00FFFFFF)) {
                current = i;
                break;
            }
        }
        int next = Math.floorMod(current + (direction < 0 ? -1 : 1), COLOR_PALETTE.length);
        entry.color = COLOR_PALETTE[next];
        save();
    }

    public static void resetDefaults() {
        resetDefaults(true);
    }

    private static void resetDefaults(boolean save) {
        SETTINGS.clear();
        for (TypeKey type : TypeKey.values()) SETTINGS.put(type.name(), new Entry(type.defaultColor));
        // Spawners are useful enough to trace by default without cluttering every chest.
        SETTINGS.get(TypeKey.SPAWNERS.name()).tracer = true;
        if (save) save();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(SETTINGS));
        } catch (Exception ignored) {}
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Type type = new TypeToken<Map<String, Entry>>() {}.getType();
            Map<String, Entry> loaded = GSON.fromJson(Files.readString(FILE), type);
            if (loaded == null) return;
            for (TypeKey key : TypeKey.values()) {
                Entry incoming = loaded.get(key.name());
                if (incoming == null) continue;
                if ((incoming.color >>> 24) == 0) incoming.color = 0xFF000000 | (incoming.color & 0x00FFFFFF);
                SETTINGS.put(key.name(), incoming);
            }
        } catch (Exception ignored) {}
    }
}
