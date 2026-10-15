package gg.cinco.client;

/** Lightweight one-click presets. Custom (1) leaves the current configuration untouched. */
public final class StarProfiles {
    private StarProfiles() {}

    public static void apply(int profile) {
        if (profile <= 1) return;
        if (profile == 2) { // PvP
            set("Auto Crystal", false); set("Anchor Macro", true); set("Auto Totem", true);
            set("Player ESP", true); set("Tracers", false); set("Storage ESP", false);
            set("Fullbright", true); set("Performance Mode", true); setValue("ESP Style", 1);
        } else if (profile == 3) { // DonutSMP
            set("Storage ESP", true); set("Light Finder", true); set("Sus Chunk Finder", true);
            set("Fullbright", true); set("Player ESP", true); set("Tracers", false);
            set("Performance Mode", true); setValue("ESP Style", 1);
        } else if (profile == 4) { // Visual
            set("Player ESP", true); set("Storage ESP", true); set("Light Finder", true);
            set("Fullbright", true); set("Purple Stars", true); set("Hover Glow", true);
            set("Performance Mode", false); setValue("ESP Style", 2);
        } else if (profile == 5) { // Performance
            set("Purple Stars", false); set("Hover Glow", false); set("Light Finder", false);
            set("Sus Chunk Finder", false); set("Storage ESP", false); set("Performance Mode", true);
            setValue("ESP Style", 3);
        }
        ConfigStore.save();
    }

    private static void set(String name, boolean enabled) {
        ClientModule module = CincoClient.findModule(name);
        if (module != null) module.enabled = enabled;
    }

    private static void setValue(String name, double value) {
        ClientModule module = CincoClient.findModule(name);
        if (module != null) module.setting = Math.max(module.min, Math.min(module.max, value));
    }

    public static String name(int profile) {
        return switch (profile) {
            case 2 -> "PvP";
            case 3 -> "DonutSMP";
            case 4 -> "Visual";
            case 5 -> "Performance";
            default -> "Custom";
        };
    }
}
