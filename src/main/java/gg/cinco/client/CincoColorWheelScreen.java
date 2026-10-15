package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

/** Star Client HSV color wheel with adaptive rendering cost. */
public final class CincoColorWheelScreen extends Screen {
    private static final int W = 540, H = 350;
    private static final int WHITE = 0xFFFFFFFF;
    private final ClientModule wheel;
    private final Screen parent;
    private boolean draggingWheel;
    private boolean draggingBrightness;

    public CincoColorWheelScreen(ClientModule wheel) {
        this(wheel, new ModuleSettingsScreen(wheel));
    }

    public CincoColorWheelScreen(ClientModule wheel, Screen parent) {
        super(Text.literal("Star Color Wheel"));
        this.wheel = wheel;
        this.parent = parent;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private Text label(String value, boolean bold) { return StarFonts.text(value, bold); }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.fill(0, 0, width, height, 0xF006050A);
        int l = left(), t = top();

        CincoDraw.softShadow(c, l, t, l + W, t + H, 16, CincoTheme.accent(), 52);
        CincoDraw.bubble(c, l, t, l + W, t + H, 16, CincoTheme.accentMuted(), 0xF20B0911);
        CincoDraw.rounded(c, l + 15, t + 15, l + 19, t + 43, 2, CincoTheme.accentLight());
        c.drawText(textRenderer, label("★  STAR COLOR WHEEL", true), l + 29, t + 16, 0xFFFFFFFF, false);
        c.drawText(textRenderer, label("HUE  •  SATURATION  •  BRIGHTNESS", false), l + 29, t + 31,
                0xFF91899D, false);

        int cx = l + 142, cy = t + 183, radius = 102;
        CincoDraw.rounded(c, cx - radius - 4, cy - radius - 4, cx + radius + 4, cy + radius + 4,
                radius + 4, 0x66000000);
        drawWheel(c, cx, cy, radius, (float) wheel.setting3);

        int panelX = l + 274, panelY = t + 67, panelW = 244;
        CincoDraw.rounded(c, panelX, panelY, panelX + panelW, panelY + 205, 13, 0xE615111D);
        c.drawText(textRenderer, label("LIVE PREVIEW", true), panelX + 14, panelY + 13, 0xFF968EA1, false);
        CincoDraw.bubble(c, panelX + 14, panelY + 31, panelX + panelW - 14, panelY + 79, 11,
                CincoTheme.accentMuted(), 0xED0E0B14);
        CincoDraw.rounded(c, panelX + 24, panelY + 41, panelX + 60, panelY + 69, 9, CincoTheme.accent());
        c.drawCenteredTextWithShadow(textRenderer, label("★", true), panelX + 42, panelY + 51, 0xFFFFFFFF);
        c.drawText(textRenderer, label("STAR", true), panelX + 72, panelY + 42, 0xFFFFFFFF, false);
        c.drawText(textRenderer, label("LIVE CLIENT THEME", false), panelX + 72, panelY + 57, 0xFF91899D, false);

        c.drawText(textRenderer, label("HUE  " + (int) wheel.setting + "°", true), panelX + 14, panelY + 94,
                0xFFF8F4FF, false);
        c.drawText(textRenderer, label("SATURATION  " + Math.round(wheel.setting2 * 100) + "%", false),
                panelX + 14, panelY + 112, 0xFF9B93A8, false);
        c.drawText(textRenderer, label("BRIGHTNESS  " + Math.round(wheel.setting3 * 100) + "%", false),
                panelX + 14, panelY + 128, 0xFF9B93A8, false);

        int bx = panelX + 14, by = panelY + 158, bw = panelW - 28;
        CincoDraw.rounded(c, bx - 2, by - 2, bx + bw + 2, by + 12, 7, 0xFF19131F);
        for (int i = 0; i < bw; i += 3) {
            float v = i / (float) Math.max(1, bw - 1);
            c.fill(bx + i, by, Math.min(bx + bw, bx + i + 3), by + 8,
                    hsv((float) wheel.setting / 360.0f, (float) wheel.setting2, v));
        }
        int marker = bx + (int) ((wheel.setting3 - wheel.min3) / Math.max(0.001, wheel.max3 - wheel.min3) * bw);
        CincoDraw.rounded(c, marker - 3, by - 4, marker + 4, by + 12, 4, WHITE);
        c.drawText(textRenderer, label("DRAG BRIGHTNESS", true), bx, by + 20, 0xFF8F879A, false);

        int by2 = t + H - 35;
        drawButton(c, l + 18, by2, 126, 22, "RESET THEME", mouseX, mouseY);
        drawButton(c, l + W - 116, by2, 98, 22, "BACK", mouseX, mouseY);
    }

    private void drawWheel(DrawContext c, int cx, int cy, int radius, float value) {
        boolean performance = StarPerformance.active(MinecraftClient.getInstance());
        int angleSteps = performance ? 48 : 72;
        int radialSteps = performance ? 11 : 15;
        for (int radial = radialSteps; radial >= 1; radial--) {
            double fraction = radial / (double) radialSteps;
            double r = radius * fraction;
            int tile = Math.max(3, (int) Math.ceil(2 * Math.PI * Math.max(1, r) / angleSteps) + 1);
            for (int a = 0; a < angleSteps; a++) {
                double angle = Math.PI * 2.0 * a / angleSteps;
                int x = cx + (int) Math.round(Math.cos(angle) * r);
                int y = cy + (int) Math.round(Math.sin(angle) * r);
                c.fill(x - tile / 2, y - tile / 2, x + tile / 2 + 1, y + tile / 2 + 1,
                        hsv(a / (float) angleSteps, (float) fraction, value));
            }
        }
        double angle = Math.toRadians(wheel.setting);
        double r = radius * Math.max(0, Math.min(1, wheel.setting2));
        int mx = cx + (int) Math.round(Math.cos(angle) * r);
        int my = cy + (int) Math.round(Math.sin(angle) * r);
        CincoDraw.bubble(c, mx - 6, my - 6, mx + 7, my + 7, 7, 0xFFFFFFFF, 0xFF151019);
        CincoDraw.rounded(c, mx - 3, my - 3, mx + 4, my + 4, 4, CincoTheme.accentLight());
    }

    private void drawButton(DrawContext c, int x, int y, int w, int h, String value, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        CincoDraw.rounded(c, x, y, x + w, y + h, 10, hover ? 0xFF291A32 : 0xFF1A1322);
        int tw = textRenderer.getWidth(value);
        c.drawText(textRenderer, label(value, true), x + w / 2 - tw / 2, y + 7, 0xFFF8F4FF, false);
    }

    private static int hsv(float hue, float saturation, float brightness) {
        float h = (hue % 1.0f + 1.0f) % 1.0f;
        float s = Math.max(0.0f, Math.min(1.0f, saturation));
        float v = Math.max(0.0f, Math.min(1.0f, brightness));
        float chroma = v * s, section = h * 6.0f, x = chroma * (1 - Math.abs(section % 2 - 1));
        float r = 0, g = 0, b = 0;
        if (section < 1) { r = chroma; g = x; }
        else if (section < 2) { r = x; g = chroma; }
        else if (section < 3) { g = chroma; b = x; }
        else if (section < 4) { g = x; b = chroma; }
        else if (section < 5) { r = x; b = chroma; }
        else { r = chroma; b = x; }
        float m = v - chroma;
        return 0xFF000000 | (Math.round((r + m) * 255) << 16)
                | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }

    private boolean insideWheel(double x, double y, int cx, int cy, int radius) {
        double dx = x - cx, dy = y - cy;
        return dx * dx + dy * dy <= radius * radius;
    }

    private void updateWheel(double mouseX, double mouseY, int cx, int cy, int radius) {
        double dx = mouseX - cx, dy = mouseY - cy;
        double distance = Math.min(radius, Math.hypot(dx, dy));
        double angle = Math.toDegrees(Math.atan2(dy, dx));
        if (angle < 0) angle += 360.0;
        wheel.setting = angle;
        wheel.setting2 = Math.max(wheel.min2, Math.min(wheel.max2, distance / radius));
        ConfigStore.save();
    }

    private void updateBrightness(double mouseX, int bx, int bw) {
        double fraction = Math.max(0, Math.min(1, (mouseX - bx) / bw));
        wheel.setting3 = wheel.min3 + (wheel.max3 - wheel.min3) * fraction;
        ConfigStore.save();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int l = left(), t = top();
        int cx = l + 142, cy = t + 183, radius = 102;
        if (insideWheel(mouseX, mouseY, cx, cy, radius)) {
            draggingWheel = true;
            updateWheel(mouseX, mouseY, cx, cy, radius);
            return true;
        }
        int panelX = l + 274, panelY = t + 67, panelW = 244;
        int bx = panelX + 14, by = panelY + 158, bw = panelW - 28;
        if (mouseX >= bx && mouseX <= bx + bw && mouseY >= by - 6 && mouseY <= by + 16) {
            draggingBrightness = true;
            updateBrightness(mouseX, bx, bw);
            return true;
        }
        int by2 = t + H - 35;
        if (mouseY >= by2 && mouseY <= by2 + 22) {
            if (mouseX >= l + 18 && mouseX <= l + 144) {
                wheel.setting = 28;
                wheel.setting2 = 0.94;
                wheel.setting3 = 1.0;
                ConfigStore.save();
                return true;
            }
            if (mouseX >= l + W - 116 && mouseX <= l + W - 18) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && draggingWheel) {
            updateWheel(mouseX, mouseY, left() + 142, top() + 183, 102);
            return true;
        }
        if (button == 0 && draggingBrightness) {
            int panelX = left() + 274, panelW = 244;
            updateBrightness(mouseX, panelX + 14, panelW - 28);
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
        MinecraftClient.getInstance().setScreen(parent == null ? new CincoClickGuiScreen() : parent);
    }

    @Override
    public boolean shouldPause() { return false; }
}
