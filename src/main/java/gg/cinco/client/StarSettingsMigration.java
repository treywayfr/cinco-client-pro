package gg.cinco.client;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** One-time migration for settings whose meaning changed in the hand/freecam/autototem update. */
public final class StarSettingsMigration {
    private static final Path MARKER = FabricLoader.getInstance().getConfigDir()
            .resolve("star-client-settings-v262.flag");

    private StarSettingsMigration() {}

    public static void apply() {
        if (Files.exists(MARKER)) return;
        try {
            ClientModule fov = CincoClient.findModule("FOV Changer");
            if (fov != null) {
                fov.setting2 = 1.0; // Show hand ON by default after moving it here.
                fov.setting3 = 0.0; // Neutral rotation.
            }

            ClientModule totem = CincoClient.findModule("Auto Totem");
            if (totem != null) {
                totem.setting2 = 0.0; // Do not silently enable Double Hand for old configs.
                totem.setting3 = 1.0; // Slot 1 default.
            }

            ClientModule changer = CincoClient.findModule("Player Changer");
            if (changer != null) changer.setting3 = 1.0; // High model detail by default.

            Files.createDirectories(MARKER.getParent());
            Files.writeString(MARKER, "migrated\n");
            ConfigStore.save();
        } catch (Exception ignored) {
            // Migration must never block game startup.
        }
    }
}
