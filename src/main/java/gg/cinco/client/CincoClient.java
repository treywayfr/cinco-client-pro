package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public final class CincoClient implements ClientModInitializer {
    public static final List<ClientModule> MODULES = new ArrayList<>();
    public static ClientModule.Category selectedCategory = ClientModule.Category.COMBAT;
    public static boolean hudEnabled = true;
    public static boolean sprintAssistEnabled = false;
    private KeyBinding guiKey;
    private KeyBinding hudKey;
    private int tickCounter;

    @Override
    public void onInitializeClient() {
        registerModules();
        ConfigStore.load();
        guiKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cinco_client.open_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, "category.cinco_client"));
        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.cinco_client.toggle_hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.cinco_client"));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (guiKey.wasPressed()) client.setScreen(new CincoClickGuiScreen());
            while (hudKey.wasPressed()) {
                hudEnabled = !hudEnabled;
                ClientModule master = findModule("Client HUD");
                if (master != null) master.enabled = hudEnabled;
                if (client.player != null) client.player.sendMessage(Text.literal("§dCinco HUD " + (hudEnabled ? "enabled" : "disabled")), true);
                ConfigStore.save();
            }
            if (sprintAssistEnabled && client.player != null && client.options.forwardKey.isPressed()) client.player.setSprinting(true);
            ClickRateTracker.tick(client);
            CombatModules.tick(client);
            if (++tickCounter % 100 == 0) {
                ClientModule autoSave = findModule("Config Auto-Save");
                if (autoSave != null && autoSave.enabled) ConfigStore.save();
            }
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) -> CincoHud.render(ctx, MinecraftClient.getInstance()));
    }

    private static void add(String n, String d, ClientModule.Category c, boolean e,
                            String label, double value, double min, double max, double step) {
        MODULES.add(new ClientModule(n, d, c, e, label, value, min, max, step));
    }

    private static void registerModules() {
        MODULES.clear();
        add("Auto Crystal", "Break nearby crystals and place near a visible player", ClientModule.Category.COMBAT, false, "Range", 4.5, 2.0, 6.0, 0.5);
        add("Anchor Macro", "Use the crosshair anchor; charges with hotbar glowstone", ClientModule.Category.COMBAT, false, "Delay", 4, 3, 12, 1);
        add("Crystal Breaker", "Automatically attack nearby end crystals", ClientModule.Category.COMBAT, false, "Range", 4.5, 2.0, 6.0, 0.5);
        add("Totem Reminder", "Warn when your offhand is not holding a totem", ClientModule.Category.COMBAT, false, "Interval", 5, 1, 15, 1);
        add("Gapple Reminder", "Display a reminder to check your healing items", ClientModule.Category.COMBAT, false, "Interval", 8, 1, 20, 1);
        add("Crystal Practice Timer", "Practice cadence helper; does not automate attacks", ClientModule.Category.COMBAT, false, "Seconds", 2, 1, 5, 1);

        add("Sprint Assist", "Sprint while the forward key is held", ClientModule.Category.MOVEMENT, false, "Mode", 1, 1, 2, 1);
        add("Safe Walk Guide", "Movement-practice toggle placeholder", ClientModule.Category.MOVEMENT, false, "Sensitivity", 5, 1, 10, 1);
        add("Elytra Reminder", "HUD reminder when not wearing an elytra", ClientModule.Category.MOVEMENT, false, "Interval", 10, 2, 20, 2);

        add("Client HUD", "Master switch for the Cinco overlay", ClientModule.Category.RENDER, true, "Opacity", 0.88, 0.35, 1.0, 0.13);
        add("FPS Counter", "Display client FPS", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Coordinates", "Display XYZ coordinates", ClientModule.Category.RENDER, true, "Decimals", 1, 0, 2, 1);
        add("Direction", "Display facing direction", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Armor HUD", "Show armor durability percentage", ClientModule.Category.RENDER, true, "Scale", 1, 1, 3, 1);
        add("Potion Status", "Show active status-effect count", ClientModule.Category.RENDER, false, "Scale", 1, 1, 3, 1);
        add("Container Radar HUD", "Show current base-scanner results", ClientModule.Category.RENDER, true, "Rows", 3, 1, 6, 1);
        add("Purple Stars", "Animated purple stars in the ClickGUI", ClientModule.Category.RENDER, true, "Density", 2, 1, 3, 1);
        add("Accent Glow", "Adjust interface accent intensity", ClientModule.Category.RENDER, true, "Intensity", 0.8, 0.2, 1.0, 0.2);
        add("Compact HUD", "Tighter layout setting", ClientModule.Category.RENDER, false, "Padding", 4, 2, 10, 2);
        add("Crosshair Guide", "Draw a simple crosshair overlay", ClientModule.Category.RENDER, false, "Size", 5, 3, 11, 2);

        add("Base Finder", "Incrementally scan nearby loaded blocks for containers", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Container Finder", "Alias toggle for the nearby container scan", ClientModule.Category.WORLD, false, "Radius", 32, 16, 64, 16);
        add("Nearby Player List", "List visible players in the local world", ClientModule.Category.WORLD, false, "Range", 32, 8, 64, 8);
        add("Block Notes", "Local target-block notes foundation", ClientModule.Category.WORLD, false, "Range", 8, 4, 16, 4);

        add("Config Auto-Save", "Persist module toggles and settings", ClientModule.Category.UTILITY, true, "Interval", 5, 1, 10, 1);
        add("Session Timer", "Show time since client initialization", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("Keystrokes", "Show movement-key status foundation", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("CPS Display", "Show click-rate foundation", ClientModule.Category.UTILITY, false, "Scale", 1, 1, 3, 1);
        add("Safe Logout Reminder", "Local reminder to save before leaving", ClientModule.Category.UTILITY, false, "Interval", 10, 5, 30, 5);

        add("Anchor Practice Timer", "Practice cadence helper; no automatic detonation", ClientModule.Category.PRACTICE, false, "Seconds", 2, 1, 5, 1);
        add("Reach Trainer", "Local distance-reference setting", ClientModule.Category.PRACTICE, false, "Distance", 3.0, 2.0, 6.0, 0.5);
        add("Cooldown Coach", "Reminder to watch attack cooldowns", ClientModule.Category.PRACTICE, false, "Interval", 3, 1, 8, 1);
        add("Inventory Checklist", "Checklist-oriented HUD utility foundation", ClientModule.Category.PRACTICE, false, "Rows", 4, 1, 8, 1);
    }

    public static ClientModule findModule(String name) {
        for (ClientModule module : MODULES) if (module.name.equals(name)) return module;
        return null;
    }
}
