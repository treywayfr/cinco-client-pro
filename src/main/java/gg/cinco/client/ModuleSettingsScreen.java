package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.Locale;

/** Expanded per-module settings, including a click-to-record keyboard binding. */
public final class ModuleSettingsScreen extends Screen {
    private final ClientModule module;
    private boolean waitingForKey;
    private int dragging = 0;
    private static final int W = 540;
    private static final int H = 350;

    public ModuleSettingsScreen(ClientModule module) {
        super(Text.literal("Cinco Module Settings"));
        this.module = module;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    private Text label(String value) {
        ClientModule font = CincoClient.findModule("Bold Typeface");
        Style style = font != null && font.enabled
                ? Style.EMPTY.withBold(true).withFont(Identifier.of("minecraft", "uniform"))
                : Style.EMPTY.withFont(Identifier.of("minecraft", "default"));
        return Text.literal(value).setStyle(style);
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
        c.drawText(textRenderer, label(module.name.toUpperCase(Locale.ROOT)), l + 26, t + 15, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, label("MODULE SETTINGS"), l + 26, t + 29, CincoTheme.secondaryText(), false);
        c.drawText(textRenderer, label(module.description), l + 18, t + 54, CincoTheme.secondaryText(), false);

        drawSlider(c, l, t, 0, module.settingLabel, module.setting, module.min, module.max, mouseX, mouseY);
        drawSlider(c, l, t, 1, module.setting2Label, module.setting2, module.min2, module.max2, mouseX, mouseY);
        drawSlider(c, l, t, 2, module.setting3Label, module.setting3, module.min3, module.max3, mouseX, mouseY);

        c.fill(l + 18, t + 212, l + W - 18, t + 237, CincoTheme.card());
        if (mouseX >= l + 18 && mouseX <= l + W - 18 && mouseY >= t + 212 && mouseY <= t + 237)
            drawHoverGlow(c, l + 18, t + 212, l + W - 18, t + 237);
        c.fill(l + 18, t + 212, l + 21, t + 237, CincoTheme.accent());
        drawCenteredSharp(c, label("OPEN CLIENT COLOR WHEEL"), l + W / 2, t + 221, CincoTheme.primaryText());

        int keyY = t + 244;
        c.drawText(textRenderer, label("MODULE KEYBIND"), l + 18, keyY, CincoTheme.secondaryText(), false);
        c.fill(l + 18, keyY + 17, l + W - 18, keyY + 46, waitingForKey ? CincoTheme.accentDark() : CincoTheme.card());
        if (waitingForKey || (mouseX >= l + 18 && mouseX <= l + W - 18 && mouseY >= keyY + 17 && mouseY <= keyY + 46))
            drawHoverGlow(c, l + 18, keyY + 17, l + W - 18, keyY + 46);
        c.fill(l + 18, keyY + 17, l + 21, keyY + 46, CincoTheme.accent());
        String binding = waitingForKey ? "PRESS A KEY  •  DELETE TO CLEAR  •  ESC TO CANCEL" :
                "CURRENT: " + keyName(module.keyCode) + "     (CLICK TO CHANGE)";
        c.drawText(textRenderer, label(binding), l + 29, keyY + 27, CincoTheme.primaryText(), false);

        c.fill(l + 18, t + H - 37, l + 145, t + H - 13, CincoTheme.accentDark());
        drawCenteredSharp(c, label("RESET SETTINGS"), l + 81, t + H - 29, CincoTheme.primaryText());
        c.fill(l + W - 135, t + H - 37, l + W - 18, t + H - 13, CincoTheme.accentDark());
        drawCenteredSharp(c, label("BACK"), l + W - 76, t + H - 29, CincoTheme.primaryText());
    }

    private void drawCenteredSharp(DrawContext c, Text text, int centerX, int y, int color) {
        c.drawText(textRenderer, text, centerX - textRenderer.getWidth(text) / 2, y, color, false);
    }

    private void drawHoverGlow(DrawContext c, int x, int y, int right, int bottom) {
        ClientModule glow = CincoClient.findModule("Hover Glow");
        if (glow == null || !glow.enabled) return;
        double speed = Math.max(0.5, Math.min(2.5, glow.setting2));
        double pulse = (Math.sin(System.currentTimeMillis() / (170.0 / speed)) + 1.0) * 0.5;
        int strength = (int) (65 + 150 * Math.max(0.2, Math.min(1.0, glow.setting) * pulse));
        int width = Math.max(1, Math.min(4, (int) glow.setting3));
        int rgb = CincoTheme.accent() & 0x00FFFFFF;
        for (int layer = width; layer >= 1; layer--) {
            int color = (Math.max(16, strength / (layer + 1)) << 24) | rgb;
            c.fill(x - layer, y - layer, right + layer, y - layer + 1, color);
            c.fill(x - layer, bottom + layer - 1, right + layer, bottom + layer, color);
            c.fill(x - layer, y, x - layer + 1, bottom, color);
            c.fill(right + layer - 1, y, right + layer, bottom, color);
        }
    }

    private void drawSlider(DrawContext c, int l, int t, int which,
                            String name, double value, double min, double max, int mouseX, int mouseY) {
        int rowTop = t + 83 + which * 49;
        c.drawText(textRenderer, label(name), l + 18, rowTop, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, label(displaySetting(which, value)), l + W - 110, rowTop, CincoTheme.primaryText(), false);
        int x = l + 18, y = rowTop + 19, w = W - 36;
        c.fill(x, y, x + w, y + 5, CincoTheme.sliderTrack());
        if (mouseX >= x - 2 && mouseX <= x + w + 2 && mouseY >= y - 5 && mouseY <= y + 12)
            drawHoverGlow(c, x, y - 2, x + w, y + 7);
        double fraction = (value - min) / Math.max(0.0001, max - min);
        int knob = x + (int)(Math.max(0, Math.min(1, fraction)) * w);
        c.fill(x, y, knob, y + 5, CincoTheme.accent());
        c.fill(knob - 3, y - 3, knob + 4, y + 8, 0xFFFFE3C1);
    }

    private String displaySetting(int which, double value) {
        int selected = (int) Math.round(value);
        if (which == 0 && module.name.equals("Sky Changer")) {
            return switch (selected) {
                case 1 -> "Sunset";
                case 2 -> "Aurora";
                case 3 -> "Cyberpunk";
                case 4 -> "Cosmic";
                default -> "Ember";
            };
        }
        if (which == 0 && module.name.equals("Fortnite Style")) {
            return switch (selected) {
                case 1 -> "Vanguard";
                case 2 -> "Crimson";
                case 3 -> "Tidebreaker";
                default -> "Void";
            };
        }
        if (module.name.equals("Key Pearl") && which == 2) return selected == 1 ? "ON" : "OFF";
        if (module.name.equals("Auto Eat") && which == 2) return selected == 1 ? "ON" : "OFF";
        return format(value);
    }

    private String format(double value) {
        return Math.abs(value - Math.round(value)) < 0.0001
                ? Integer.toString((int)Math.round(value))
                : String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String keyName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN || code < 0) return "UNBOUND";
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) return String.valueOf((char)('A' + code - GLFW.GLFW_KEY_A));
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) return String.valueOf((char)('0' + code - GLFW.GLFW_KEY_0));
        if (code >= GLFW.GLFW_KEY_F1 && code <= GLFW.GLFW_KEY_F25) return "F" + (code - GLFW.GLFW_KEY_F1 + 1);
        return switch (code) {
            case GLFW.GLFW_KEY_SPACE -> "SPACE";
            case GLFW.GLFW_KEY_LEFT_SHIFT -> "LEFT SHIFT";
            case GLFW.GLFW_KEY_RIGHT_SHIFT -> "RIGHT SHIFT";
            case GLFW.GLFW_KEY_LEFT_CONTROL -> "LEFT CTRL";
            case GLFW.GLFW_KEY_RIGHT_CONTROL -> "RIGHT CTRL";
            case GLFW.GLFW_KEY_LEFT_ALT -> "LEFT ALT";
            case GLFW.GLFW_KEY_RIGHT_ALT -> "RIGHT ALT";
            case GLFW.GLFW_KEY_TAB -> "TAB";
            case GLFW.GLFW_KEY_ENTER -> "ENTER";
            case GLFW.GLFW_KEY_BACKSPACE -> "BACKSPACE";
            case GLFW.GLFW_KEY_DELETE -> "DELETE";
            case GLFW.GLFW_KEY_UP -> "UP";
            case GLFW.GLFW_KEY_DOWN -> "DOWN";
            case GLFW.GLFW_KEY_LEFT -> "LEFT";
            case GLFW.GLFW_KEY_RIGHT -> "RIGHT";
            case GLFW.GLFW_KEY_GRAVE_ACCENT -> "GRAVE";
            case GLFW.GLFW_KEY_MINUS -> "MINUS";
            case GLFW.GLFW_KEY_EQUAL -> "EQUALS";
            case GLFW.GLFW_KEY_LEFT_BRACKET -> "L BRACKET";
            case GLFW.GLFW_KEY_RIGHT_BRACKET -> "R BRACKET";
            case GLFW.GLFW_KEY_SEMICOLON -> "SEMICOLON";
            case GLFW.GLFW_KEY_APOSTROPHE -> "APOSTROPHE";
            case GLFW.GLFW_KEY_COMMA -> "COMMA";
            case GLFW.GLFW_KEY_PERIOD -> "PERIOD";
            case GLFW.GLFW_KEY_SLASH -> "SLASH";
            default -> "KEY " + code;
        };
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button != 0) return super.mouseClicked(mouseX, mouseY, button);
        int l = left(), t = top();
        if (mouseY >= t + 212 && mouseY <= t + 237) {
            ClientModule wheel = CincoClient.findModule("Color Wheel");
            if (wheel != null) MinecraftClient.getInstance().setScreen(new CincoColorWheelScreen(wheel, this));
            return true;
        }
        int keyY = t + 244;
        if (mouseX >= l + 18 && mouseX <= l + W - 18 && mouseY >= keyY + 17 && mouseY <= keyY + 46) {
            waitingForKey = true;
            return true;
        }
        if (mouseY >= t + H - 37 && mouseY <= t + H - 13) {
            if (mouseX >= l + 18 && mouseX <= l + 145) {
                module.resetSettings();
                ConfigStore.save();
                return true;
            }
            if (mouseX >= l + W - 135 && mouseX <= l + W - 18) {
                close();
                return true;
            }
        }
        for (int i = 0; i < 3; i++) {
            int y = t + 83 + i * 49 + 14;
            if (mouseY >= y && mouseY <= y + 17 && mouseX >= l + 18 && mouseX <= l + W - 18) {
                dragging = i + 1;
                updateSlider(mouseX);
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && dragging != 0) {
            updateSlider(mouseX);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = 0;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void updateSlider(double mouseX) {
        int x = left() + 18, w = W - 36;
        double fraction = (mouseX - x) / w;
        if (dragging == 1) module.setSettingFromSlider(fraction);
        if (dragging == 2) module.setSetting2FromSlider(fraction);
        if (dragging == 3) module.setSetting3FromSlider(fraction);
        ConfigStore.save();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (waitingForKey) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                waitingForKey = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.keyCode = GLFW.GLFW_KEY_UNKNOWN;
                waitingForKey = false;
                ConfigStore.save();
                return true;
            }
            module.keyCode = keyCode;
            waitingForKey = false;
            ConfigStore.save();
            return true;
        }
        if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(new CincoClickGuiScreen());
    }

    @Override
    public boolean shouldPause() { return false; }
}
