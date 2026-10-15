package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.math.RotationAxis;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Global first-person hand/item controls owned by Custom FOV / FOV Changer. */
@Mixin(HeldItemRenderer.class)
public abstract class HandViewMixin {
    @Unique private boolean star$fovHandPushed;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), cancellable = true, require = 0)
    private void star$beginFovHand(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                   Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                   MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                   CallbackInfo ci) {
        star$fovHandPushed = false;
        ClientModule fov = CincoClient.findModule("FOV Changer");
        if (fov == null || !fov.enabled) return;

        // Show Hand belongs here now. OFF hides both the vanilla/custom arm and the held item.
        if (fov.setting2 < 0.5) {
            ci.cancel();
            return;
        }

        float rotation = (float)Math.max(-90.0, Math.min(90.0, fov.setting3));
        if (Math.abs(rotation) < 0.001f) return;

        matrices.push();
        star$fovHandPushed = true;
        // Rotate the whole first-person presentation so the hand and held item stay together.
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void star$endFovHand(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                 Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                 MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                 CallbackInfo ci) {
        if (star$fovHandPushed) {
            matrices.pop();
            star$fovHandPushed = false;
        }
    }
}
