package gg.cinco.client.mixin;

import gg.cinco.client.CombatModules;
import gg.cinco.client.CombatReliabilityV3;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CombatModules.class, remap = false)
public abstract class CombatModulesMixin {
    @Inject(method = "tickAutoCrystal", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinco$targetedCrystal(MinecraftClient client, boolean allowPlace, CallbackInfo ci) {
        if (allowPlace) CombatReliabilityV3.tickAutoCrystal(client);
        ci.cancel();
    }

    @Inject(method = "triggerAnchorMacro", at = @At("HEAD"), cancellable = true, remap = false)
    private static void cinco$reliableAnchor(MinecraftClient client, CallbackInfo ci) {
        CombatReliabilityV3.triggerAnchorMacro(client);
        ci.cancel();
    }

    @Inject(method = "tick", at = @At("TAIL"), remap = false)
    private static void cinco$tickPatch(MinecraftClient client, CallbackInfo ci) {
        CombatReliabilityV3.tick(client);
    }
}
