package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

/** Extra lifecycle wiring for the reliability/render overhaul without disturbing the polished GUI. */
public final class CincoPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Add the underground block-light detector as one clean world module.
        if (CincoClient.findModule("Light Finder") == null) {
            CincoClient.MODULES.add(new ClientModule(
                    "Light Finder",
                    "Highlight underground block-lit areas through walls using loaded chunk light data",
                    ClientModule.Category.WORLD,
                    false,
                    "Range",
                    48, 16, 96, 8));
            // CincoClient loaded the existing modules before this second entry point added Light Finder.
            // Loading again is safe and allows a saved Light Finder state to be restored too.
            ConfigStore.load();
        }

        ClientTickEvents.END_CLIENT_TICK.register(EnhancedWorldScan::tick);
        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
                EnhancedEspHud.render(ctx, MinecraftClient.getInstance()));
    }
}
