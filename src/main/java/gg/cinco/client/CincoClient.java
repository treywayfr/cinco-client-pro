package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class CincoClient implements ClientModInitializer {
    public static final List<ClientModule> MODULES = new ArrayList<>();
    public static ClientModule.Category selectedCategory = ClientModule.Category.COMBAT;
    public static boolean hudEnabled = true;
    public static boolean sprintAssistEnabled = false;
    private final Map<String, Boolean> lastModuleKeyStates = new HashMap<>();
    private int tickCounter;

    @Override
    public void onInitializeClient() {
        registerModules();
        ConfigStore.load();
        hudEnabled = isEnabled("Client HUD", true);
        sprintAssistEnabled = isEnabled("Sprint Assist", false);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            pollModuleKeybinds(client);

            if (sprintAssistEnabled && client.player != null && client.options.forwardKey.isPressed()) client.player.setSprinting(true);
            ClickRateTracker.tick(client);
            CombatModules.tick(client);
            VisualModules.tick(client);
            if (++tickCounter % 100 == 0) {
                ClientModule autoSave = findModule("Config Auto-Save");
                if (autoSave != null && autoSave.enabled) ConfigStore.save();
            }
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> CincoHud.render(ctx, MinecraftClient.getInstance()));
    }

    private void pollModuleKeybinds(MinecraftClient client) {
        if (client.getWindow() == null) return;
        long window = client.getWindow().getHandle();
        boolean screenOpen = client.currentScreen != null;

        for (ClientModule module : MODULES) {
            int code = module.keyCode;
            if (code == GLFW.GLFW_KEY_UNKNOWN || code < 0) continue;
            boolean down = GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
            boolean wasDown = lastModuleKeyStates.getOrDefault(module.name, false);
            lastModuleKeyStates.put(module.name, down);

            if (screenOpen || !down || wasDown) continue;
            if (module.name.equals("Open GUI")) {
                client.setScreen(new CincoClickGuiScreen());
                continue;
            }
            if (module.name.equals("Toggle HUD")) {
                hudEnabled = !hudEnabled;
                ClientModule master = findModule("Client HUD");
                if (master != null) master.enabled = hudEnabled;
                if (client.player != null) client.player.sendMessage(Text.literal("Cinco HUD " + (hudEnabled ? "enabled" : "disabled")), true);
                ConfigStore.save();
                continue;
            }
            if (module.name.equals("Anchor Macro")) {
                if (module.enabled) CombatModules.triggerAnchorMacro(client);
                else if (client.player != null) client.player.sendMessage(Text.literal("Enable Anchor Macro in the GUI first."), true);
                continue;
            }
            if (module.name.equals("Key Pearl")) {
                if (module.enabled) CombatModules.triggerPearl(client);
                else if (client.player != null) client.player.sendMessage(Text.literal("Enable Key Pearl in the GUI first."), true);
                continue;
            }

            module.enabled = !module.enabled;
            if (module.name.equals("Sprint Assist")) sprintAssistEnabled = module.enabled;
            if (module.name.equals("Client HUD")) hudEnabled = module.enabled;
            if (module.name.equals("Base Finder") || module.name.equals("Container Finder")) {
                ClientModule base = findModule("Base Finder");
                ClientModule containers = findModule("Container Finder");
                if (base != null && containers != null) {
                    base.enabled = module.enabled;
                    containers.enabled = module.enabled;
                }
                if (!module.enabled) BaseScanner.reset();
            }
            if (client.player != null) {
                client.player.sendMessage(Text.literal("Cinco: " + module.name + (module.enabled ? " enabled" : " disabled")), true);
            }
            ConfigStore.save();
        }
    }

    private static boolean isEnabled(String name, boolean fallback) {
        ClientModule m = findModule(name);
        return m == null ? fallback : m.enabled;
    }

    private static void add(String n, String d, ClientModule.Category c, boolean e,
                            String label, double value, double min, double max, double step) {
        MODULES.add(new ClientModule(n, d, c, e, label, value, min, max, step));
    }

    private static void registerModules() {
        MODULES.clear();
        // COMBAT — keep automation explicit and off by default.
        add("Auto Crystal", "While holding crystals and right-clicking: place on the targeted base and break nearby crystals", ClientModule.Category.COMBAT, false, "Action delay", 0, 0, 4, 1);
        add("Kill Aura", "Attack the nearest visible player in range while enabled", ClientModule.Category.COMBAT, false, "Range", 3.5, 3.0, 6.0, 0.5);
        add("Reach", "Change the client interaction reach value", ClientModule.Category.COMBAT, false, "Reach", 4.5, 3.0, 6.5, 0.5);
        add("Anchor Macro", "One key: place an anchor, charge with glowstone, then activate using a totem", ClientModule.Category.COMBAT, false, "Step delay", 1, 0, 5, 1);
        add("Key Pearl", "Press its key to select a hotbar pearl and throw it instantly", ClientModule.Category.COMBAT, false, "Cooldown", 0, 0, 10, 1);
        add("Auto Safe Anchor", "During an anchor macro, place glowstone at your feet when looking down", ClientModule.Category.COMBAT, false, "Look Angle", 70, 50, 85, 5);
        add("Auto Totem", "Move a totem into the offhand when health reaches the threshold", ClientModule.Category.COMBAT, false, "Health", 12, 4, 20, 2);
        add("Auto Eat", "Eat a hotbar food when hunger falls below the threshold", ClientModule.Category.COMBAT, false, "Hunger", 8, 2, 18, 2);
        add("Offhand Manager", "Offhand management UI/settings foundation", ClientModule.Category.COMBAT, false, "Mode", 1, 1, 3, 1);
        add("Auto Regear", "Regear checklist helper foundation", ClientModule.Category.COMBAT, false, "Preset", 1, 1, 4, 1);
        add("Crystal Breaker", "Attack nearby end crystals", ClientModule.Category.COMBAT, false, "Range", 4.0, 2.0, 6.0, 0.5);
        add("Totem Reminder", "Warn when your offhand is not holding a totem", ClientModule.Category.COMBAT, false, "Interval", 5, 1, 15, 1);
        add("Gapple Reminder", "Warn when no golden apples remain", ClientModule.Category.COMBAT, false, "Interval", 8, 1, 20, 1);
        add("Crystal Practice Timer", "Practice timing helper; no automated attack", ClientModule.Category.COMBAT, false, "Seconds", 2, 1, 5, 1);

        // MOVEMENT
        add("Sprint Assist", "Sprint while the forward key is held", ClientModule.Category.MOVEMENT, false, "Mode", 1, 1, 2, 1);
        add("Speed", "Adjust horizontal movement while walking", ClientModule.Category.MOVEMENT, false, "Multiplier", 1.3, 1.0, 2.5, 0.1);
        add("Flight", "Client-side directional flight controls", ClientModule.Category.MOVEMENT, false, "Vertical speed", 0.5, 0.1, 1.5, 0.1);
        add("No Slow", "Reduce local movement slowdowns while using items", ClientModule.Category.MOVEMENT, false, "Multiplier", 1.0, 1.0, 1.5, 0.1);
        add("Safe Walk Guide", "Movement-practice helper foundation", ClientModule.Category.MOVEMENT, false, "Sensitivity", 5, 1, 10, 1);
        add("Elytra Reminder", "Warn when an elytra is not equipped", ClientModule.Category.MOVEMENT, false, "Interval", 10, 2, 20, 2);
        add("Auto Swim", "Swimming convenience helper foundation", ClientModule.Category.MOVEMENT, false, "Mode", 1, 1, 2, 1);
        add("Fall Alert", "Warn when you are falling quickly", ClientModule.Category.MOVEMENT, false, "Velocity", 0.8, 0.3, 1.5, 0.1);

        // RENDER / HUD
        add("Client HUD", "Master switch for the Cinco overlay", ClientModule.Category.RENDER, true, "Opacity", 0.95, 0.65, 1.0, 0.05);
        add("FPS Counter", "Display client FPS", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Coordinates", "Display XYZ coordinates", ClientModule.Category.RENDER, true, "Decimals", 1, 0, 2, 1);
        add("Direction", "Display facing direction", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Armor HUD", "Show armor durability percentage", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Potion Status", "Show active status-effect count", ClientModule.Category.RENDER, false, "Scale", 1, 1, 3, 1);
        add("Container Radar HUD", "Show local scan results", ClientModule.Category.RENDER, true, "Rows", 3, 1, 6, 1);
        add("Purple Stars", "Animated purple stars behind the GUI", ClientModule.Category.RENDER, true, "Density", 2, 1, 3, 1);
        add("Accent Glow", "Control the interface accent intensity", ClientModule.Category.RENDER, true, "Intensity", 0.8, 0.2, 1.0, 0.2);
        add("Compact HUD", "Use tighter HUD spacing", ClientModule.Category.RENDER, false, "Padding", 4, 2, 10, 2);
        add("Crosshair Guide", "Draw a simple crosshair guide", ClientModule.Category.RENDER, false, "Size", 5, 3, 11, 2);
        add("Hover Glow", "Subtle animated glow around hovered module cards and settings", ClientModule.Category.RENDER, true, "Intensity", 0.8, 0.2, 1.0, 0.2);
        add("Player ESP", "Screen-space player outlines, names and distances", ClientModule.Category.RENDER, false, "Range", 64, 16, 128, 16);
        add("Tracers", "Draw clean screen-space tracer lines to nearby players", ClientModule.Category.RENDER, false, "Range", 64, 16, 128, 16);
        add("Storage ESP", "On-screen markers for selected nearby storage blocks", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Chests", "Show chest storage markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Trapped Chests", "Show trapped chest markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Barrels", "Show barrel markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Ender Chests", "Show ender chest markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Shulkers", "Show shulker box markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("ESP Spawners", "Show spawner markers when Storage ESP is active", ClientModule.Category.RENDER, false, "Range", 48, 8, 96, 8);
        add("Fullbright", "Increase the local gamma for easier visibility", ClientModule.Category.RENDER, false, "Gamma", 1.0, 0.5, 5.0, 0.5);
        add("FOV Changer", "Set a custom field of view while enabled", ClientModule.Category.RENDER, false, "FOV", 100, 70, 110, 1);
        add("Sky Changer", "Change sky and cloud colors with animated presets", ClientModule.Category.RENDER, false, "Style", 1, 1, 5, 1);
        add("GTA Skin Changer", "Local-only Franklin-inspired streetwear skin on the 3D Minecraft player model", ClientModule.Category.RENDER, false, "Outfit", 1, 1, 4, 1);
        add("Target HUD", "Show distance to the entity under your crosshair", ClientModule.Category.RENDER, false, "Range", 6, 2, 12, 2);
        add("Ping Display", "Show current player's latency when available", ClientModule.Category.RENDER, false, "Scale", 1, 1, 3, 1);

        // WORLD / DONUT SMP — only inspect information already loaded by Minecraft.
        add("Base Finder", "Scan nearby loaded blocks for containers", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Container Finder", "Find containers in the nearby loaded scan volume", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Sus Chunk Finder", "Rank already-loaded chunks by unusual container/utility-block density", ClientModule.Category.WORLD, false, "Radius", 2, 1, 4, 1);
        add("Portal Finder", "Find nearby nether portal blocks in loaded chunks", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Spawner Finder", "Show known spawners in the local scan", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Shulker Finder", "Show known shulker boxes in the local scan", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Stash Radar", "Look for clusters of storage blocks in loaded chunks", ClientModule.Category.WORLD, false, "Min Blocks", 4, 2, 12, 1);
        add("Chunk Density", "Show loaded-chunk scan density diagnostics", ClientModule.Category.WORLD, false, "Radius", 2, 1, 4, 1);
        add("Nearby Player List", "List visible players in the local world", ClientModule.Category.WORLD, false, "Range", 32, 8, 64, 8);
        add("Block Notes", "Show details for the block under your crosshair", ClientModule.Category.WORLD, false, "Range", 8, 4, 16, 4);

        // UTILITY
        add("Config Auto-Save", "Persist module toggles and settings", ClientModule.Category.UTILITY, true, "Interval", 5, 1, 10, 1);
        add("Session Timer", "Show time since client initialization", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("Keystrokes", "Show movement-key states", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("CPS Display", "Show click-rate per second", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("Safe Logout Reminder", "Local reminder before disconnecting", ClientModule.Category.UTILITY, false, "Interval", 10, 5, 30, 5);
        add("Auto Refill", "Hotbar refill helper foundation", ClientModule.Category.UTILITY, false, "Stack Size", 16, 8, 64, 8);
        add("Inventory Checklist", "Display crystal, glowstone, totem and food counts", ClientModule.Category.UTILITY, false, "Rows", 4, 1, 8, 1);
        add("Fast XP", "Repeatedly use XP bottles while the selected input is held", ClientModule.Category.UTILITY, false, "Cooldown", 1, 0, 5, 1);
        add("Auto Clicker", "Repeat attack clicks while left mouse is held", ClientModule.Category.UTILITY, false, "Delay", 2, 1, 10, 1);

        // PRACTICE
        add("Anchor Practice Timer", "Practice timing helper; no automatic detonation", ClientModule.Category.PRACTICE, false, "Seconds", 2, 1, 5, 1);
        add("Reach Trainer", "Show target distance as a local training reference", ClientModule.Category.PRACTICE, false, "Distance", 3.0, 2.0, 6.0, 0.5);
        add("Cooldown Coach", "Show attack cooldown progress", ClientModule.Category.PRACTICE, false, "Interval", 3, 1, 8, 1);
        add("Surround Trainer", "Surround-building practice reminder foundation", ClientModule.Category.PRACTICE, false, "Mode", 1, 1, 3, 1);
        add("Pearl Reminder", "Remind when no ender pearls are in the hotbar", ClientModule.Category.PRACTICE, false, "Interval", 5, 1, 10, 1);

        // SETTINGS — these options actually drive GUI typography, animation, and palette.
        add("Color Wheel", "Open the full hue and saturation color picker for the entire client", ClientModule.Category.SETTINGS, true, "Hue", 28, 0, 355, 5);
        add("Open GUI", "Open the Cinco Client interface using your chosen key", ClientModule.Category.SETTINGS, true, "Unused", 1, 1, 1, 1);
        add("Toggle HUD", "Toggle the in-game top status bar using your chosen key", ClientModule.Category.SETTINGS, true, "Unused", 1, 1, 1, 1);
        add("Bold Typeface", "Use Minecraft's clean uniform font with bold weight", ClientModule.Category.SETTINGS, true, "Weight", 1, 1, 1, 1);
        add("GUI Animation", "Animate the panel entrance and border sweep", ClientModule.Category.SETTINGS, true, "Speed", 220, 100, 500, 40);
        add("Panel Opacity", "Adjust how opaque the main panels appear", ClientModule.Category.SETTINGS, true, "Opacity", 1.0, 0.75, 1.0, 0.05);
    }

    public static ClientModule findModule(String name) {
        for (ClientModule module : MODULES) if (module.name.equals(name)) return module;
        return null;
    }
}
