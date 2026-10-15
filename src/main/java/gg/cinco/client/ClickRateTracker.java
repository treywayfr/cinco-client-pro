package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayDeque;
import java.util.Deque;

/** Tracks physical left-button press edges while the player is in-world. */
public final class ClickRateTracker {
    private static final Deque<Long> clicks = new ArrayDeque<>();
    private static boolean wasDown;
    private ClickRateTracker() {}

    public static void tick(MinecraftClient client) {
        if (client.getWindow() == null || client.player == null || client.world == null || client.currentScreen != null) {
            wasDown = false;
            purge(System.currentTimeMillis());
            return;
        }
        boolean down = GLFW.glfwGetMouseButton(client.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_1) == GLFW.GLFW_PRESS;
        long now = System.currentTimeMillis();
        if (down && !wasDown) clicks.addLast(now);
        wasDown = down;
        purge(now);
    }

    public static int getCps() {
        purge(System.currentTimeMillis());
        return clicks.size();
    }

    private static void purge(long now) {
        while (!clicks.isEmpty() && now - clicks.peekFirst() > 1000) clicks.removeFirst();
    }
}
