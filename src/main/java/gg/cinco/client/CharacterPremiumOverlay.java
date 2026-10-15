package gg.cinco.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/** Compatibility hook: premium details are baked into the 1.21.11 procedural character models. */
public final class CharacterPremiumOverlay {
    private CharacterPremiumOverlay() {}
    public static void render(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,
                              MatrixStack matrices,VertexConsumerProvider consumers,int light) {
        // Intentionally folded into the base procedural renderers to keep one stable render pass.
    }
}
