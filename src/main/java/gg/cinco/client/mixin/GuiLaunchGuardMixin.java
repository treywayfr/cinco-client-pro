package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import gg.cinco.client.GuiRecoveryScreen;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Opens the Star GUI on the key edge and catches GUI construction failures so Minecraft stays alive. */
@Mixin(value = CincoClient.class, remap = false)
public abstract class GuiLaunchGuardMixin {
    private static boolean star$lastGuiKeyDown;

    @Inject(method = "pollModuleKeybinds", at = @At("HEAD"), remap = false, require = 0)
    private void star$guardGuiLaunch(MinecraftClient client, CallbackInfo ci) {
        if (client == null || client.getWindow() == null) return;
        ClientModule open = CincoClient.findModule("Open GUI");
        if (open == null || open.keyCode == GLFW.GLFW_KEY_UNKNOWN || open.keyCode < 0) return;
        long window = client.getWindow().getHandle();
        boolean down = GLFW.glfwGetKey(window, open.keyCode) == GLFW.GLFW_PRESS;
        boolean edge = down && !star$lastGuiKeyDown;
        star$lastGuiKeyDown = down;
        if (!edge || client.currentScreen != null) return;
        try {
            client.setScreen(new CincoClickGuiScreen());
        } catch (Throwable t) {
            String msg = t.getClass().getSimpleName() + ": " + (t.getMessage() == null ? "No message" : t.getMessage());
            client.setScreen(new GuiRecoveryScreen(msg));
        }
    }
}
