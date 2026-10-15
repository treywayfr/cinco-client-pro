package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.CjReferenceHandRenderer;
import gg.cinco.client.ClientModule;
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
        if (star$renderArm(true, matrices, consumers, light, player)) ci.cancel();
    }

    @Inject(method = "renderLeftArm", at = @At("HEAD"), cancellable = true)
    private void star$leftArm(MatrixStack matrices, VertexConsumerProvider consumers, int light,
                              AbstractClientPlayerEntity player, CallbackInfo ci) {
        if (star$renderArm(false, matrices, consumers, light, player)) ci.cancel();
    }

    private boolean star$renderArm(boolean right, MatrixStack matrices, VertexConsumerProvider consumers,
                                   int light, AbstractClientPlayerEntity player) {
        ClientModule module = CincoClient.findModule("Player Changer");
        if (module == null) module = CincoClient.findModule("GTA Skin Changer");
        if (module == null || !module.enabled) return false;

        if (Math.round(module.setting) == 5) {
            return CjReferenceHandRenderer.render(right, matrices, consumers, light, player);
        }

        // PlayerChangerHandRenderer historically used setting3 as its visibility gate. setting3 is
        // now Model Detail, while Show Hand lives in FOV Changer. Temporarily satisfy the old
        // renderer gate so all non-CJ custom arms follow the global FOV Show Hand control instead.
        double detail = module.setting3;
        try {
            module.setting3 = 1.0;
            return PlayerChangerHandRenderer.render(right, matrices, consumers, light, player);
        } finally {
            module.setting3 = detail;
        }
    }
}
