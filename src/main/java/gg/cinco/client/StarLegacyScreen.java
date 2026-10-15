package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/**
 * Compatibility bridge for Star's existing GUI code on Minecraft 1.21.11.
 * Mojang moved screen input callbacks to Click/KeyInput/CharInput records; this class converts
 * those records back into the coordinate/key values Star's GUI implementation already uses.
 */
public abstract class StarLegacyScreen extends Screen {
    protected StarLegacyScreen(Text title) {
        super(title);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return false;
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null) {
                // Orbit HUD mirrors movement keys while the GUI is open. Clear them before closing
                // so no movement key can remain logically held after returning to the game.
                client.options.forwardKey.setPressed(false);
                client.options.backKey.setPressed(false);
                client.options.leftKey.setPressed(false);
                client.options.rightKey.setPressed(false);
                client.options.jumpKey.setPressed(false);
                client.options.sneakKey.setPressed(false);
                client.options.sprintKey.setPressed(false);
                client.setScreen(null);
            }
            return true;
        }
        return false;
    }

    public boolean charTyped(char chr, int modifiers) {
        return false;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return mouseClicked(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        return mouseDragged(click.x(), click.y(), click.button(), deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(Click click) {
        return mouseReleased(click.x(), click.y(), click.button());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        return keyPressed(input.key(), input.scancode(), input.modifiers());
    }

    @Override
    public boolean charTyped(CharInput input) {
        int cp = input.codepoint();
        return cp >= Character.MIN_VALUE && cp <= Character.MAX_VALUE
                && charTyped((char) cp, input.modifiers());
    }
}
