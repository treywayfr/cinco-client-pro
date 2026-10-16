package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;

public final class StarHotbarClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        HudRenderCallback.EVENT.register((ctx,tickCounter)->StarHotbarFrame.render(ctx, MinecraftClient.getInstance()));
    }
}
