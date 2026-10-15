package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

/** Extra lifecycle wiring for the reliability/render overhaul without disturbing the polished GUI. */
public final class CincoPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean added = false;
        if (CincoClient.findModule("Light Finder") == null) {
            CincoClient.MODULES.add(new ClientModule(
                    "Light Finder",
                    "Highlight underground block-lit areas through walls using loaded chunk light data",
                    ClientModule.Category.WORLD,
                    false,
                    "Range",
                    48, 16, 96, 8));
            added = true;
        }
        if (CincoClient.findModule("Free Cam") == null) {
            CincoClient.MODULES.add(new ClientModule(
                    "Free Cam",
                    "Detach the local camera and fly it around while your real player stays still",
                    ClientModule.Category.MOVEMENT,
                    false,
                    "Camera speed",
                    1.0, 0.2, 3.0, 0.2));
            added = true;
        }
        if (added) ConfigStore.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EnhancedWorldScan.tick(client);
            FreeCamController.tick(client);
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
                EnhancedEspHud.render(ctx, MinecraftClient.getInstance()));
    }
}
