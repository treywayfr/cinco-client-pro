package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** HSV color wheel. Hue and saturation come from clicking/dragging the disk; brightness uses a slider. */
public final class CincoColorWheelScreen extends Screen {
    private final ClientModule wheel;
    private boolean draggingWheel;
    private boolean draggingBrightness;
    private static final int W = 560, H = 360;
    private static final int WHITE = 0xFFFFFFFF;

    public CincoColorWheelScreen(ClientModule wheel) {
        super(Text.literal("Cinco Color Wheel"));
        this.wheel = wheel;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    private Text label(String text) {
        ClientModule font = CincoClient.findModule("Bold Typeface");
        Style style = font != null && font.enabled
                ? Style.EMPTY.withBold(true).withFont(Identifier.of("minecraft", "uniform"))
                : Style.EMPTY.withFont(Identifier.of("minecraft", "default"));
        return Text.literal(text).setStyle(style);
    }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.fill(0, 0, width, height, CincoTheme.background());

        int l = left(), t = top();
        c.fill(l - 2, t - 2, l + W + 2, t + H + 2, CincoTheme.accent());
        c.fill(l, t, l + W, t + H, CincoTheme.panel());
        c.fill(l, t, l + W, t + 3, CincoTheme.accent());
        c.fill(l + 14, t + 13, l + 17, t + 42, CincoTheme.accent());

        c.drawText(textRenderer, label("CINCO COLOR WHEEL"), l + 27, t + 15, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, label("DRAG THE DISK TO PICK HUE + SATURATION"), l + 27, t + 30, CincoTheme.secondaryText(), false);

        int cx = l + 155, cy = t + 188, radius = 110;
        drawWheel(c, cx, cy, radius, (float) (wheel.setting3));

        int swatchX = l + 300, swatchY = t + 75;
        c.drawText(textRenderer, label("LIVE PREVIEW"), swatchX, swatchY, CincoTheme.secondaryText(), false);
        c.fill(swatchX, swatchY + 17, swatchX + 225, swatchY + 65, CincoTheme.accent());
        c.fill(swatchX + 4, swatchY + 21, swatchX + 221, swatchY + 61, CincoTheme.panel());
        c.fill(swatchX + 8, swatchY + 25, swatchX + 49, swatchY + 57, CincoTheme.accent());
        c.drawText(textRenderer, label("CINCO  /  LIVE CLIENT THEME"), swatchX + 59, swatchY + 30, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, label("HUE: " + (int) wheel.setting + "°"), swatchX, t + 156, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, label("SATURATION: " + Math.round(wheel.setting2 * 100) + "%"), swatchX, t + 173, CincoTheme.secondaryText(), false);
        c.drawText(textRenderer, label("BRIGHTNESS: " + Math.round(wheel.setting3 * 100) + "%"), swatchX, t + 190, CincoTheme.secondaryText(), false);

        c.drawText(textRenderer, label("BRIGHTNESS"), swatchX, t + 218, CincoTheme.secondaryText(), false);
        int bx = swatchX, by = t + 240, bw = 225;
        for (int i = 0; i < bw; i += 3) {
            float v = i / (float) (bw - 1);
            c.fill(bx + i, by, Math.min(bx + bw, bx + i + 3), by + 10, hsv((float) wheel.setting / 360.0f, (float) wheel.setting2, v));
        }
        int marker = bx + (int) ((wheel.setting3 - wheel.min3) / (wheel.max3 - wheel.min3) * bw);
        c.fill(marker - 2, by - 4, marker + 3, by + 14, WHITE);
        c.drawText(textRenderer, label("The selection updates the GUI, module screens and HUD immediately."), swatchX, t + 266, CincoTheme.secondaryText(), false);

        c.fill(l + 18, t + H - 36, l + 142, t + H - 13, CincoTheme.accentDark());
        drawCentered(c, label("RESET ORANGE"), l + 80, t + H - 28, CincoTheme.primaryText());
        c.fill(l + W - 132, t + H - 36, l + W - 18, t + H - 13, CincoTheme.accentDark());
        drawCentered(c, label("BACK"), l + W - 75, t + H - 28, CincoTheme.primaryText());
    }

    private void drawWheel(DrawContext c, int cx, int cy, int radius, float value) {
        // A segmented polar disk avoids external textures while preserving a smooth-looking color wheel.
        int angleSteps = 80;
        int radialSteps = 16;
        for (int radial = 1; radial <= radialSteps; radial++) {
            double fraction = radial / (double) radialSteps;
            double r = radius * fraction;
            int tile = Math.max(3, (int) Math.ceil(2 * Math.PI * r / angleSteps) + 1);
            for (int a = 0; a < angleSteps; a++) {
                double angle = Math.PI * 2.0 * a / angleSteps;
                int x = cx + (int) Math.round(Math.cos(angle) * r);
                int y = cy + (int) Math.round(Math.sin(angle) * r);
                float hue = a / (float) angleSteps;
                c.fill(x - tile / 2, y - tile / 2, x + tile / 2 + 1, y + tile / 2 + 1,
                        hsv(hue, (float) fraction, value));
            }
        }

        double angle = Math.toRadians(wheel.setting);
        double r = radius * Math.max(0, Math.min(1, wheel.setting2));
        int markerX = cx + (int) Math.round(Math.cos(angle) * r);
        int markerY = cy + (int) Math.round(Math.sin(angle) * r);
        c.fill(markerX - 5, markerY - 5, markerX + 6, markerY + 6, 0xFF171018);
        c.fill(markerX - 3, markerY - 3, markerX + 4, markerY + 4, WHITE);
    }

    private static int hsv(float hue, float saturation, float brightness) {
        float h = (hue % 1.0f + 1.0f) % 1.0f;
        float s = Math.max(0.0f, Math.min(1.0f, saturation));
        float v = Math.max(0.0f, Math.min(1.0f, brightness));
        float c = v * s, section = h * 6.0f, x = c * (1 - Math.abs(section % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (section < 1) { r = c; g = x; }
        else if (section < 2) { r = x; g = c; }
        else if (section < 3) { g = c; b = x; }
        else if (section < 4) { g = x; b = c; }
        else if (section < 5) { r = x; b = c; }
        else { r = c; b = x; }
        float m = v - c;
        return 0xFF000000 | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private void drawCentered(DrawContext c, Text text, int centerX, int y, int color) {
        c.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int l = left(), t = top();
        int cx = l + 155, cy = t + 188, radius = 110;
        if (insideWheel(mouseX, mouseY, cx, cy, radius)) {
            draggingWheel = true;
            updateWheel(mouseX, mouseY, cx, cy, radius);
            return true;
        }
        int bx = l + 300, by = t + 240, bw = 225;
        if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by - 5 && mouseY <= by + 15) {
            draggingBrightness = true;
            updateBrightness(mouseX, bx, bw);
            return true;
        }
        if (mouseY >= t + H - 36 && mouseY <= t + H - 13) {
            if (mouseX >= l + 18 && mouseX <= l + 142) {
                wheel.setting = 28; wheel.setting2 = .90; wheel.setting3 = 1.0;
                ConfigStore.save();
                return true;
            }
            if (mouseX >= l + W - 132 && mouseX <= l + W - 18) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && draggingWheel) {
            updateWheel(mouseX, mouseY, left() + 155, top() + 188, 110);
            return true;
        }
        if (button == 0 && draggingBrightness) {
            updateBrightness(mouseX, left() + 300, 225);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingWheel = false;
        draggingBrightness = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private boolean insideWheel(double x, double y, int cx, int cy, int radius) {
        double dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void updateWheel(double x, double y, int cx, int cy, int radius) {
        double dx = x - cx, dy = y - cy;
        double distance = Math.min(radius, Math.sqrt(dx * dx + dy * dy));
        double hue = Math.toDegrees(Math.atan2(dy, dx));
        if (hue < 0) hue += 360.0;
        wheel.setting = Math.round(hue / wheel.step) * wheel.step;
        if (wheel.setting >= 360) wheel.setting = 0;
        wheel.setting2 = Math.max(wheel.min2, Math.min(wheel.max2, distance / radius));
        ConfigStore.save();
    }

    private void updateBrightness(double x, int sliderX, int width) {
        wheel.setSetting3FromSlider((x - sliderX) / (double) width);
        ConfigStore.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(wheel));
    }

    @Override
    public boolean shouldPause() { return false; }
}
