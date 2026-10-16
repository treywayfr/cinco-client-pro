package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Suppress only the vanilla LOCAL third-person player body when a Star Locker model is active. */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererSkinMixin {
    @Inject(method="render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V",at=@At("HEAD"),cancellable=true,require=0)
    private void star$replaceLocalPlayer(LivingEntityRenderState state, MatrixStack matrices,
                                         OrderedRenderCommandQueue queue, CameraRenderState camera,
                                         CallbackInfo ci){
        if(!(state instanceof PlayerEntityRenderState playerState))return;
        MinecraftClient client=MinecraftClient.getInstance();
        if(client==null||client.player==null||client.currentScreen!=null||client.options.getPerspective().isFirstPerson())return;
        ClientModule module=CincoClient.findModule("Player Changer");
        if(module==null)module=CincoClient.findModule("GTA Skin Changer");
        if(module==null||!module.enabled)return;

        // 1.21.11's render-state position is interpolated while Entity#getX/Y/Z is tick-position.
        // Comparing those positions caused the vanilla body to intermittently escape cancellation in F5.
        // PlayerEntityRenderState carries the actual entity id, so use the exact local-player identity instead.
        if(playerState.id!=client.player.getId())return;
        ci.cancel();
    }
}
