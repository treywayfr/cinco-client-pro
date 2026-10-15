package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityGlowMixin {
    @Inject(method = "isGlowing", at = @At("RETURN"), cancellable = true)
    private void cinco$playerEspGlow(CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) return;
        Entity self = (Entity) (Object) this;
        if (!(self instanceof PlayerEntity target)) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || target == client.player) return;
        ClientModule module = CincoClient.findModule("Player ESP");
        if (module == null || !module.enabled) return;
        double range = module.setting;
        if (client.player.squaredDistanceTo(target) <= range * range) cir.setReturnValue(true);
    }
}
