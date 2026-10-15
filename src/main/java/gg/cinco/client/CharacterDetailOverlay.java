package gg.cinco.client;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;

/**
 * Compatibility hook retained for existing configs/mixins.
 * Detail geometry is now baked directly into the 1.21.11 procedural character renderers, which
 * avoids a second texture/render layer and keeps the preview/model path stable.
 */
public final class CharacterDetailOverlay {
    private CharacterDetailOverlay() {}
    public static void render(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,
                              MatrixStack matrices,VertexConsumerProvider consumers,int light) {
        // Intentionally folded into GtaCharacterRenderer / CjReferenceRenderer.
    }
}
