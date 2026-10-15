package gg.cinco.client.mixin;

import gg.cinco.client.CharacterDetailOverlay;
import gg.cinco.client.CharacterPremiumOverlay;
import gg.cinco.client.CincoClient;
import gg.cinco.client.CjReferenceRenderer;
import gg.cinco.client.GtaCharacterRenderer;
import gg.cinco.client.GtaExpandedOutfitOverlay;
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
    @Inject(method="render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V",at=@At("HEAD"),cancellable=true)
    private void star$renderCustomCharacter(AbstractClientPlayerEntity player,float bodyYaw,float tickDelta,MatrixStack matrices,VertexConsumerProvider consumers,int light,CallbackInfo ci){
        var module=CincoClient.findModule("Player Changer");MinecraftClient client=MinecraftClient.getInstance();if(module==null||!module.enabled||player!=client.player)return;
        int character=Math.max(1,Math.min(5,(int)Math.round(module.setting))),outfit=Math.max(1,Math.min(6,(int)Math.round(module.setting2)));boolean detailed=module.setting3>=.5;
        if(character==5){CjReferenceRenderer.render(player,bodyYaw,tickDelta,matrices,consumers,light,outfit,detailed);ci.cancel();return;}
        GtaCharacterRenderer.render(player,bodyYaw,tickDelta,matrices,consumers,light);
        double detailRangeSq=18.0*18.0;boolean nearCamera=client.player==null||client.player.squaredDistanceTo(player)<=detailRangeSq;
        if(nearCamera){CharacterDetailOverlay.render(player,bodyYaw,tickDelta,matrices,consumers,light);if(detailed)CharacterPremiumOverlay.render(player,bodyYaw,tickDelta,matrices,consumers,light);}
        if(character==1)GtaExpandedOutfitOverlay.renderFranklin(player,bodyYaw,matrices,consumers,light,outfit);ci.cancel();
    }
}
