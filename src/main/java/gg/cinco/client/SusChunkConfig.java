package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Persistent filters and presentation options for the loaded-chunk Sus Chunk scanner. */
public final class SusChunkConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("star-sus-chunk.json");

    public boolean rotatedDeepslate = true;
    public boolean cobbledDeepslate = true;
    public boolean matureCrops = true;
    public boolean cactus = true;
    public boolean kelp = true;
    public boolean amethyst = true;
    public boolean highlightBlocks = false;
    public int color = 0xFFB05CFF;

    private static SusChunkConfig INSTANCE = new SusChunkConfig();
    private SusChunkConfig() {}

    public static SusChunkConfig get() { return INSTANCE; }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                SusChunkConfig loaded = GSON.fromJson(Files.readString(FILE), SusChunkConfig.class);
                if (loaded != null) INSTANCE = loaded;
            }
        } catch (Exception ignored) {}
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception ignored) {}
    }
}
