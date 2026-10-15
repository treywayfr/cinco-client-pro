package gg.cinco.client.mixin;

import gg.cinco.client.PlayerChangerHandRenderer;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerChangerArmMixin {
    @Inject(method = "renderRightArm", at = @At("HEAD"), cancellable = true)
    private void star$rightArm(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                               AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (PlayerChangerHandRenderer.render(true, matrices, consumers, light, player)) ci.cancel();
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void star$leftArm(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                              AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (PlayerChangerHandRenderer.render(false, matrices, consumers, light, player)) ci.cancel();
    }
}
