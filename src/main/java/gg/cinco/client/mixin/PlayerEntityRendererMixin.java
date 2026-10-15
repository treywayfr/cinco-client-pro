package gg.cinco.client.mixin;

import gg.cinco.client.CharacterDetailOverlay;
import gg.cinco.client.CharacterPremiumOverlay;
import gg.cinco.client.CincoClient;
import gg.cinco.client.GtaCharacterRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(
            method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",
            at = @At("HEAD"),
            cancellable = true
    )
    private void star$renderCustomCharacter(AbstractClientPlayerEntity player, float bodyYaw, float tickDelta,
                                            MatrixStack matrices, VertexConsumerProvider consumers, int light,
                                            CallbackInfo ci) {
        var module = CincoClient.findModule("Player Changer");
        MinecraftClient client = MinecraftClient.getInstance();
        if (module != null && module.enabled && player == client.player) {
            GtaCharacterRenderer.render(player, bodyYaw, tickDelta, matrices, consumers, light);

            // Detail uses camera-distance LOD instead of a user toggle. That keeps the close-up
            // characters rich while avoiding the old FPS hit when Free Cam is far away.
            double detailRangeSq = 18.0 * 18.0;
            boolean nearCamera = client.gameRenderer.getCamera() == null
                    || client.gameRenderer.getCamera().getPos().squaredDistanceTo(player.getPos()) <= detailRangeSq;
            if (nearCamera) {
                CharacterDetailOverlay.render(player, bodyYaw, tickDelta, matrices, consumers, light);
                CharacterPremiumOverlay.render(player, bodyYaw, tickDelta, matrices, consumers, light);
            }
            ci.cancel();
        }
    }
}
