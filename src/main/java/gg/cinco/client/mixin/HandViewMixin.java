package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Changes the entire first-person hand/held-item presentation without touching world state. */
@Mixin(HeldItemRenderer.class)
public abstract class HandViewMixin {
    @Unique private boolean star$handViewPushed;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void star$beginHandView(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                    Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    CallbackInfo ci) {
        ClientModule module = CincoClient.findModule("Hand View");
        if (module == null || !module.enabled) return;

        matrices.push();
        star$handViewPushed = true;

        double forward = Math.max(-0.50, Math.min(1.50, module.setting));
        float scale = (float)Math.max(0.55, Math.min(1.80, module.setting2));
        double spacing = Math.max(0.0, Math.min(0.60, module.setting3));

        boolean mainRight = player.getMainArm() == Arm.RIGHT;
        boolean right = hand == Hand.MAIN_HAND ? mainRight : !mainRight;

        // Push both hands outward symmetrically, then move the whole view farther into the scene.
        // This fixes the old left-heavy/bunched-up first-person positioning.
        matrices.translate(right ? spacing : -spacing, -0.025, -forward);
        matrices.scale(scale, scale, scale);
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void star$endHandView(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                  Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  CallbackInfo ci) {
        if (star$handViewPushed) {
            matrices.pop();
            star$handViewPushed = false;
        }
    }
}
