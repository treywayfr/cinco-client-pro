package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import gg.cinco.client.StarPlayerSkins;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Minecraft 1.21.11 moved player rendering to PlayerEntityRenderState.
 * Keep Star's local Player Changer on the stable state-based hooks instead of
 * the removed AbstractClientPlayerEntity render signature.
 */
@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin {
    @Inject(method = "getTexture", at = @At("HEAD"), cancellable = true, require = 0)
    private void star$playerChangerTexture(PlayerEntityRenderState state,
                                           CallbackInfoReturnable<Identifier> cir) {
        ClientModule module = star$module();
        if (module == null || !module.enabled || !star$isLocal(state)) return;
        int character = Math.max(1, Math.min(5, (int)Math.round(module.setting)));
        int outfit = Math.max(1, Math.min(6, (int)Math.round(module.setting2)));
        cir.setReturnValue(StarPlayerSkins.texture(character, outfit));
    }

    @Inject(method = "scale", at = @At("TAIL"), require = 0)
    private void star$playerChangerShape(PlayerEntityRenderState state, MatrixStack matrices, CallbackInfo ci) {
        ClientModule module = star$module();
        if (module == null || !module.enabled || !star$isLocal(state)) return;
        int character = Math.max(1, Math.min(5, (int)Math.round(module.setting)));
        switch (character) {
            case 1 -> matrices.scale(1.14f, 1.00f, 1.10f); // Peter: wider torso silhouette
            case 2 -> matrices.scale(0.82f, 0.80f, 0.82f); // Bart: shorter/smaller silhouette
            case 3 -> matrices.scale(1.00f, 1.04f, 1.00f); // Street Hero
            case 4 -> matrices.scale(0.94f, 1.03f, 0.94f); // Anime Fighter
            case 5 -> matrices.scale(1.02f, 1.04f, 1.02f); // CJ: athletic proportions
            default -> { }
        }
    }

    private static ClientModule star$module() {
        ClientModule module = CincoClient.findModule("Player Changer");
        return module != null ? module : CincoClient.findModule("GTA Skin Changer");
    }

    private static boolean star$isLocal(PlayerEntityRenderState state) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null || state == null) return false;
        double dx = state.x - client.player.getX();
        double dy = state.y - client.player.getY();
        double dz = state.z - client.player.getZ();
        if (dx * dx + dy * dy + dz * dz > 0.25) return false;
        return state.displayName == null
                || state.displayName.getString().equals(client.player.getDisplayName().getString());
    }
}
