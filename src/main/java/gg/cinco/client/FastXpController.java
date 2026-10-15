package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import org.lwjgl.glfw.GLFW;

/** Uses one XP bottle per client tick while right-click is held. */
public final class FastXpController {
    private static int cooldown;
    private FastXpController() {}

    public static void tick(MinecraftClient client) {
        if (cooldown > 0) cooldown--;
        ClientModule module = CincoClient.findModule("Fast XP");
        if (module == null || !module.enabled || client == null || client.player == null
                || client.interactionManager == null || client.getWindow() == null || client.currentScreen != null) return;
        if (!client.player.getMainHandStack().isOf(Items.EXPERIENCE_BOTTLE)) return;

        boolean rightDown = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS;
        if (module.setting3 >= 0.5 && !rightDown) return;
        if (cooldown > 0) return;

        client.interactionManager.interactItem(client.player, Hand.MAIN_HAND);
        client.player.swingHand(Hand.MAIN_HAND);
        // Old default of 1 now means maximum practical client rate: once per tick.
        cooldown = Math.max(0, (int) Math.round(module.setting) - 1);
    }
}
