package gg.cinco.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
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
    @Unique private boolean star$handViewTinted;

    @Inject(method = "renderFirstPersonItem", at = @At("HEAD"), require = 0)
    private void star$beginHandView(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                    Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                    MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                    CallbackInfo ci) {
        ClientModule module = CincoClient.findModule("Hand View");
        if (module == null || !module.enabled) return;

        matrices.push();
        star$handViewPushed = true;

        // Positive Forward means farther outward into the scene, not closer to the camera.
        double forward = Math.max(-1.0, Math.min(1.50, module.setting));
        float scale = (float) Math.max(0.50, Math.min(2.0, module.setting2));
        matrices.translate(0.0, 0.0, -forward);
        matrices.scale(scale, scale, scale);

        // Best-effort opacity for vanilla hand/item layers; unsupported opaque layers simply ignore it.
        float alpha = (float) Math.max(0.25, Math.min(1.0, module.setting3));
        if (alpha < 0.999f) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);
            star$handViewTinted = true;
        }
    }

    @Inject(method = "renderFirstPersonItem", at = @At("RETURN"), require = 0)
    private void star$endHandView(AbstractClientPlayerEntity player, float tickDelta, float pitch,
                                  Hand hand, float swingProgress, ItemStack item, float equipProgress,
                                  MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light,
                                  CallbackInfo ci) {
        if (star$handViewTinted) {
            RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
            star$handViewTinted = false;
        }
        if (star$handViewPushed) {
            matrices.pop();
            star$handViewPushed = false;
        }
    }
}
