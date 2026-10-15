package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/** Compact Star Client per-module settings screen. */
public final class ModuleSettingsScreen extends StarLegacyScreen {
    private static final int W = 520;
    private static final int H = 342;
    private final ClientModule module;
    private boolean waitingForKey;
    private int dragging;
    private final long openedAt = System.currentTimeMillis();
    private final Map<String, StarAnimations.Track> hoverAnim = new HashMap<>();

    public ModuleSettingsScreen(ClientModule module) {
        super(Text.literal("Star Module Settings"));
        this.module = module;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private Text label(String value, boolean bold) { return StarFonts.text(value, bold); }
    private boolean playerChanger() { return module.name.equals("Player Changer") || module.name.equals("GTA Skin Changer"); }
    private int sliderWidth() { return playerChanger() ? W - 166 : W - 36; }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();
        float open = StarAnimations.progress(now - openedAt,
                StarAnimations.duration(client, StarAnimations.PANEL_MS, true),
                StarAnimations.Curve.CUBIC_OUT);

        c.fill(0, 0, width, height, 0xF006050A);
        int l = left(), t = top() + Math.round((1f - open) * 12f);

        if (StarPerformance.allowMenuShadow(client))
            CincoDraw.softShadow(c, l, t, l + W, t + H, 16, CincoTheme.accent(), Math.round(52 * open));
        CincoDraw.bubble(c, l, t, l + W, t + H, 16, CincoTheme.accentMuted(), 0xF20B0911);
        CincoDraw.rounded(c, l + 15, t + 15, l + 19, t + 43, 2, CincoTheme.cyan());
        c.drawText(textRenderer, label(module.name.toUpperCase(Locale.ROOT), true), l + 29, t + 16,
                0xFFFFFFFF, false);
        c.drawText(textRenderer, label("★  MODULE SETTINGS", false), l + 29, t + 31,
                0xFF9B93A8, false);
        c.drawText(textRenderer, label(trim(module.description, playerChanger() ? 52 : 74), false), l + 18, t + 54,
                0xFF8D8598, false);

        drawSlider(c, l, t, 0, module.settingLabel, module.setting, module.min, module.max, mouseX, mouseY, now);
        drawSlider(c, l, t, 1, module.setting2Label, module.setting2, module.min2, module.max2, mouseX, mouseY, now);
        drawSlider(c, l, t, 2, module.setting3Label, module.setting3, module.min3, module.max3, mouseX, mouseY, now);

        if (playerChanger()) drawPlayerPreview(c, l, t);

        int colorY = t + 225;
        button(c, l + 18, colorY, W - 36, 24, "OPEN STAR COLOR WHEEL", mouseX, mouseY,
                0xFF17121F, 0xFF24172E, now);

        int keyY = t + 257;
        c.drawText(textRenderer, label("KEYBIND", true), l + 18, keyY, 0xFF9B93A8, false);
        CincoDraw.bubble(c, l + 92, keyY - 6, l + W - 18, keyY + 18, 9,
                waitingForKey ? CincoTheme.cyan() : 0xFF30283A,
                waitingForKey ? 0xFF21152A : 0xFF15111B);
        String binding = waitingForKey ? "PRESS A KEY  •  DELETE CLEARS  •  ESC CANCELS"
                : "CURRENT: " + keyName(module.keyCode) + "  •  CLICK TO CHANGE";
        c.drawText(textRenderer, label(binding, false), l + 104, keyY + 1, 0xFFF5F0FA, false);

        int by = t + H - 36;
        button(c, l + 18, by, 126, 23, "RESET SETTINGS", mouseX, mouseY,
                0xFF1B1322, 0xFF291A32, now);
        button(c, l + W - 116, by, 98, 23, "BACK", mouseX, mouseY,
                0xFF1B1322, 0xFF291A32, now);
    }

    private void drawPlayerPreview(DrawContext c, int l, int t) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        int x = l + W - 132;
        int y = t + 75;
        int w = 114;
        int h = 133;
        CincoDraw.bubble(c, x, y, x + w, y + h, 12, 0xFF352742, 0xE8110D17);
        c.drawCenteredTextWithShadow(textRenderer, label("★ PREVIEW", true), x + w / 2, y + 8, CincoTheme.starGold());
        float orbit = StarPerformance.animationTier(client) == 0 ? 0f
                : (float) Math.sin(System.currentTimeMillis() / 850.0) * 26.0f;
        try {
            InventoryScreen.drawEntity(c, x + 8, y + 22, x + w - 8, y + h - 7,
                    43, 0.0f, x + w / 2.0f + orbit, y + 53.0f, client.player);
        } catch (RuntimeException ignored) {
            // Preview should never make the settings screen unusable on renderer transitions.
        }
    }

    private void drawSlider(DrawContext c, int l, int t, int which,
                            String name, double value, double min, double max,
                            int mouseX, int mouseY, long now) {
        int rowTop = t + 82 + which * 45;
        String shownName = name;
        if (playerChanger() && which == 2) shownName = "First-person hands";
        c.drawText(textRenderer, label(shownName.toUpperCase(Locale.ROOT), true), l + 18, rowTop,
                0xFFF8F4FF, false);
        String valueText = displaySetting(which, value);
        int valueWidth = textRenderer.getWidth(valueText);
        int x = l + 18, w = sliderWidth();
        c.drawText(textRenderer, label(valueText, true), x + w - valueWidth, rowTop,
                CincoTheme.accentLight(), false);

        int y = rowTop + 19;
        boolean hover = mouseX >= x - 3 && mouseX <= x + w + 3 && mouseY >= y - 6 && mouseY <= y + 13;
        float h = hover("slider:" + which, hover, now);
        CincoDraw.rounded(c, x, y, x + w, y + 6, 3, mix(0xFF302A37, 0xFF3E3148, h));
        double fraction = (value - min) / Math.max(0.0001, max - min);
        int knob = x + (int) Math.round(Math.max(0, Math.min(1, fraction)) * w);
        if (knob > x) CincoDraw.rounded(c, x, y, knob, y + 6, 3,
                mix(CincoTheme.accent(), CincoTheme.magenta(), h * 0.45f));
        CincoDraw.rounded(c, knob - 4, y - 3, knob + 5, y + 10, 5,
                mix(CincoTheme.accentLight(), CincoTheme.cyan(), h));
    }

    private void button(DrawContext c, int x, int y, int w, int h, String text,
                        int mx, int my, int normal, int hoverColor, long now) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        float a = hover("button:" + text, hover, now);
        int drawY = y - Math.round(a);
        CincoDraw.rounded(c, x, drawY, x + w, drawY + h, 10, mix(normal, hoverColor, a));
        int tw = textRenderer.getWidth(text);
        c.drawText(textRenderer, label(text, true), x + w / 2 - tw / 2, drawY + 8,
                mix(0xFFF8F4FF, CincoTheme.cyan(), a * 0.45f), false);
    }

    private float hover(String key, boolean active, long now) {
        StarAnimations.Track track = hoverAnim.computeIfAbsent(key, k -> new StarAnimations.Track(0f));
        int duration = StarAnimations.duration(MinecraftClient.getInstance(),
                active ? StarAnimations.HOVER_IN_MS : StarAnimations.HOVER_OUT_MS, false);
        return track.to(active ? 1f : 0f, duration, StarAnimations.Curve.CUBIC_OUT, now);
    }

    private int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        return ((int)(aa + (ba-aa)*t) << 24) | ((int)(ar + (br-ar)*t) << 16)
                | ((int)(ag + (bg-ag)*t) << 8) | (int)(ab + (bb-ab)*t);
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
        if (which == 0 && playerChanger()) {
            return switch (selected) {
                case 2 -> "Spiky Cartoon Kid";
                case 3 -> "Anime Heroine";
                case 4 -> "Tung Tung Sahur";
                default -> "Franklin / GTA V";
            };
        }
        if (playerChanger() && which == 2) return selected == 1 ? "ON" : "OFF";
        if (module.name.equals("Font Style") && which == 0) return StarFonts.name(selected);
        if (module.name.equals("Client Profile") && which == 0) return StarProfiles.name(selected);
        if ((module.name.equals("Key Pearl") || module.name.equals("Auto Eat")) && which == 2)
            return selected == 1 ? "ON" : "OFF";
        return format(value);
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private String format(double value) {
        return Math.abs(value - Math.round(value)) < 0.0001
                ? Integer.toString((int) Math.round(value))
                : String.format(Locale.ROOT, "%.2f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    public static String keyName(int code) {
        if (code == GLFW.GLFW_KEY_UNKNOWN || code < 0) return "UNBOUND";
        if (code >= GLFW.GLFW_KEY_A && code <= GLFW.GLFW_KEY_Z) return String.valueOf((char) ('A' + code - GLFW.GLFW_KEY_A));
        if (code >= GLFW.GLFW_KEY_0 && code <= GLFW.GLFW_KEY_9) return String.valueOf((char) ('0' + code - GLFW.GLFW_KEY_0));
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
        int colorY = t + 225;
        if (mouseX >= l + 18 && mouseX <= l + W - 18 && mouseY >= colorY && mouseY <= colorY + 24) {
            ClientModule wheel = CincoClient.findModule("Color Wheel");
            if (wheel != null) MinecraftClient.getInstance().setScreen(new CincoColorWheelScreen(wheel, this));
            return true;
        }

        int keyY = t + 257;
        if (mouseX >= l + 92 && mouseX <= l + W - 18 && mouseY >= keyY - 6 && mouseY <= keyY + 18) {
            waitingForKey = true;
            return true;
        }

        int by = t + H - 36;
        if (mouseY >= by && mouseY <= by + 23) {
            if (mouseX >= l + 18 && mouseX <= l + 144) {
                module.resetSettings();
                ConfigStore.save();
                return true;
            }
            if (mouseX >= l + W - 116 && mouseX <= l + W - 18) {
                close();
                return true;
            }
        }

        int sliderX = l + 18, sliderW = sliderWidth();
        for (int i = 0; i < 3; i++) {
            int y = t + 82 + i * 45 + 13;
            if (mouseX >= sliderX - 3 && mouseX <= sliderX + sliderW + 3 && mouseY >= y && mouseY <= y + 21) {
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
        int x = left() + 18, w = sliderWidth();
        double fraction = (mouseX - x) / w;
        if (dragging == 1) module.setSettingFromSlider(fraction);
        if (dragging == 2) module.setSetting2FromSlider(fraction);
        if (dragging == 3) module.setSetting3FromSlider(fraction);
        if (module.name.equals("Client Profile") && dragging == 1) StarProfiles.apply((int) Math.round(module.setting));
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
