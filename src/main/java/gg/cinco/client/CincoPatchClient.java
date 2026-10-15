package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

/** Extra lifecycle wiring for the reliability/render overhaul. */
public final class CincoPatchClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        boolean changedModules = false;

        ClientModule legacyPlayerChanger = CincoClient.findModule("GTA Skin Changer");
        if (legacyPlayerChanger != null) legacyPlayerChanger.name = "Player Changer";

        if (CincoClient.findModule("Light Finder") == null) {
            CincoClient.MODULES.add(new ClientModule(
                    "Light Finder",
                    "Highlight underground block-lit areas through walls using loaded chunk light data",
                    ClientModule.Category.WORLD,
                    false,
                    "Range",
                    48, 16, 96, 8));
            changedModules = true;
        }

        if (CincoClient.findModule("Free Cam") == null) {
            ClientModule freeCam = new ClientModule(
                    "Free Cam",
                    "Detach the camera locally while your real player remains at the activation point",
                    ClientModule.Category.MOVEMENT,
                    false,
                    "Camera speed",
                    1.0, 0.2, 3.0, 0.2);
            freeCam.keyCode = GLFW.GLFW_KEY_C;
            CincoClient.MODULES.add(freeCam);
            changedModules = true;
        }

        ClientModule oldFullbright = CincoClient.findModule("Fullbright");
        if (oldFullbright != null && oldFullbright.max < 20.0) {
            int index = CincoClient.MODULES.indexOf(oldFullbright);
            ClientModule strongFullbright = new ClientModule(
                    "Fullbright",
                    "Force a bright lightmap and choose an extended client gamma level",
                    ClientModule.Category.RENDER,
                    oldFullbright.enabled,
                    "Gamma",
                    8.0, 1.0, 20.0, 0.5);
            strongFullbright.keyCode = oldFullbright.keyCode;
            CincoClient.MODULES.set(index, strongFullbright);
            changedModules = true;
        }

        if (changedModules) ConfigStore.load();
        StorageEspConfig.load();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            EnhancedWorldScan.tick(client);
            FreeCamController.tick(client);
        });
        HudRenderCallback.EVENT.register((ctx, tickCounter) ->
                EnhancedEspHud.render(ctx, MinecraftClient.getInstance()));
        WorldRenderEvents.LAST.register(StorageEspWorldRenderer::render);
        WorldRenderEvents.LAST.register(CleanEspWorldRenderer::render);
    }
}
