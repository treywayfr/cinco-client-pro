package gg.cinco.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/** Compatibility facade; the detailed CJ renderer lives in CjReferenceRenderer. */
public final class CjCharacterRenderer {
    private CjCharacterRenderer() {}

    public static void render(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                              MatrixStack matrices, VertexConsumerProvider consumers, int light, int outfit) {
        CjReferenceRenderer.render(player, bodyYaw, tickDelta, matrices, consumers, light,
                Math.max(1, Math.min(6, outfit)), true);
    }
}
