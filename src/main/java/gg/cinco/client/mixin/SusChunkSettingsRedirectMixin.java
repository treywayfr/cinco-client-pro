package gg.cinco.client.mixin;

import gg.cinco.client.ClientModule;
import gg.cinco.client.ModuleSettingsScreen;
import gg.cinco.client.SusChunkSettingsScreen;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value=ModuleSettingsScreen.class,remap=false)
public abstract class SusChunkSettingsRedirectMixin {
    @Shadow @Final private ClientModule module;
    @Inject(method="init",at=@At("TAIL"),remap=false)
    private void star$susChunkSettings(CallbackInfo ci){if(module!=null&&module.name.equals("Sus Chunk"))MinecraftClient.getInstance().setScreen(new SusChunkSettingsScreen());}
}
