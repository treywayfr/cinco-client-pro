package gg.cinco.client;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ConfigStore {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("cinco-client.json");
    private ConfigStore() {}

    private static final class State {
        boolean enabled;
        double setting;
        double setting2;
        double setting3;
        int keyCode;
        State(boolean enabled, double setting, double setting2, double setting3, int keyCode) {
            this.enabled = enabled;
            this.setting = setting;
            this.setting2 = setting2;
            this.setting3 = setting3;
            this.keyCode = keyCode;
        }
    }

    public static void save() {
        try {
            Map<String, State> data = new LinkedHashMap<>();
            for (ClientModule m : CincoClient.MODULES) {
                data.put(m.name, new State(m.enabled, m.setting, m.setting2, m.setting3, m.keyCode));
            }
            Files.createDirectories(FILE.getParent());
            Files.writeString(FILE, GSON.toJson(data));
        } catch (IOException ignored) {
            // Configuration persistence must never crash the client.
        }
    }

    public static void load() {
        if (!Files.exists(FILE)) return;
        try {
            Type type = new TypeToken<Map<String, State>>() {}.getType();
            Map<String, State> data = GSON.fromJson(Files.readString(FILE), type);
            if (data == null) return;
            for (ClientModule module : CincoClient.MODULES) {
                State state = data.get(module.name);
                if (state == null) continue;
                module.enabled = state.enabled;
                module.setting = Math.max(module.min, Math.min(module.max, state.setting));
                // Older configs do not have these fields, so keep each module's new defaults.
                if (state.setting2 != 0) module.setting2 = Math.max(module.min2, Math.min(module.max2, state.setting2));
                if (state.setting3 != 0) module.setting3 = Math.max(module.min3, Math.min(module.max3, state.setting3));
                // In legacy configs keyCode deserializes as zero; only accept valid GLFW keys or UNKNOWN.
                if (state.keyCode == -1 || state.keyCode >= 32) module.keyCode = state.keyCode;
            }
            ClientModule sprint = CincoClient.findModule("Sprint Assist");
            if (sprint != null) CincoClient.sprintAssistEnabled = sprint.enabled;
            ClientModule hud = CincoClient.findModule("Client HUD");
            if (hud != null) CincoClient.hudEnabled = hud.enabled;
        } catch (Exception ignored) {
            // Ignore malformed config and continue with defaults.
        }
    }
}
