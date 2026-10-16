package gg.cinco.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

/** Registers Star's replacement player-model and premium Locker detail passes. */
public final class StarModelClient implements ClientModInitializer {
    @Override public void onInitializeClient(){
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(StarPlayerModelWorldRenderer::render);
        WorldRenderEvents.BEFORE_TRANSLUCENT.register(PremiumCosmeticDetailRenderer::render);
    }
}
