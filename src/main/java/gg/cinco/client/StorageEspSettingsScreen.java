package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.Locale;

/** Dedicated Storage ESP editor: range, per-block highlight, tracer style and color. */
public final class StorageEspSettingsScreen extends Screen {
    private static final int W = 700;
    private static final int H = 410;
    private static final int ROW_H = 32;
    private final ClientModule module;
    private boolean draggingRange;

    public StorageEspSettingsScreen(ClientModule module) {
        super(Text.literal("Star Storage ESP Settings"));
        this.module = module;
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private Text text(String value, boolean bold) { return StarFonts.text(value, bold); }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.fill(0, 0, width, height, 0xF006050A);
        int l = left(), t = top();

        CincoDraw.softShadow(c, l, t, l + W, t + H, 16, CincoTheme.accent(), 52);
        CincoDraw.bubble(c, l, t, l + W, t + H, 16, CincoTheme.accentMuted(), 0xF20B0911);

        c.drawText(textRenderer, text("★  STORAGE ESP", true), l + 20, t + 16, 0xFFFFFFFF, false);
        c.drawText(textRenderer, text("BLOCK OUTLINES  •  STRAIGHT OR SNAKE.IO-STYLE TRACERS  •  PER-TYPE COLORS", false),
                l + 20, t + 32, 0xFF91899D, false);

        int sx = l + 20, sy = t + 61, sw = W - 40;
        c.drawText(textRenderer, text("RANGE  " + (int) Math.round(module.setting) + "m", true),
                sx, sy - 12, 0xFFF8F4FF, false);
        CincoDraw.rounded(c, sx, sy, sx + sw, sy + 7, 4, 0xFF302A37);
        double fraction = (module.setting - module.min) / Math.max(0.001, module.max - module.min);
        int knob = sx + (int) Math.round(Math.max(0, Math.min(1, fraction)) * sw);
        if (knob > sx) CincoDraw.rounded(c, sx, sy, knob, sy + 7, 4, CincoTheme.accent());
        CincoDraw.rounded(c, knob - 4, sy - 3, knob + 5, sy + 10, 5, CincoTheme.accentLight());

        int headerY = t + 83;
        c.drawText(textRenderer, text("TYPE", true), l + 20, headerY, 0xFF91899D, false);
        c.drawText(textRenderer, text("HIGHLIGHT", true), l + 278, headerY, 0xFF91899D, false);
        c.drawText(textRenderer, text("TRACER", true), l + 382, headerY, 0xFF91899D, false);
        c.drawText(textRenderer, text("SNAKE", true), l + 466, headerY, 0xFF91899D, false);
        c.drawText(textRenderer, text("COLOR", true), l + 555, headerY, 0xFF91899D, false);

        int rowY = t + 100;
        StorageEspConfig.TypeKey[] types = StorageEspConfig.TypeKey.values();
        for (int i = 0; i < types.length; i++) {
            StorageEspConfig.TypeKey type = types[i];
            StorageEspConfig.Entry setting = StorageEspConfig.get(type);
            int y = rowY + i * ROW_H;
            boolean hover = mouseX >= l + 14 && mouseX <= l + W - 14 && mouseY >= y && mouseY <= y + 27;
            CincoDraw.rounded(c, l + 14, y, l + W - 14, y + 27, 9,
                    hover ? 0xFF201725 : 0xE615111D);

            CincoDraw.rounded(c, l + 22, y + 7, l + 34, y + 19, 6, setting.color);
            c.drawText(textRenderer, text(type.label.toUpperCase(Locale.ROOT), false),
                    l + 43, y + 9, 0xFFF8F4FF, false);

            pill(c, l + 268, y + 5, 82, 17, setting.highlight ? "ON" : "OFF",
                    setting.highlight ? CincoTheme.enabled() : 0xFF332D39);
            pill(c, l + 374, y + 5, 70, 17, setting.tracer ? "ON" : "OFF",
                    setting.tracer ? CincoTheme.enabled() : 0xFF332D39);
            pill(c, l + 458, y + 5, 72, 17, setting.snakeTracer ? "ON" : "OFF",
                    setting.snakeTracer ? CincoTheme.enabled() : 0xFF332D39);
            CincoDraw.bubble(c, l + 546, y + 5, l + 682, y + 22, 8,
                    setting.color, 0xD914111D);
            CincoDraw.rounded(c, l + 554, y + 9, l + 566, y + 18, 4, setting.color);
            c.drawText(textRenderer, text("CLICK TO CYCLE", false), l + 572, y + 9,
                    0xFFF7F4FF, false);
        }

        int by = t + H - 35;
        button(c, l + 18, by, 118, 22, "RESET TYPES", mouseX, mouseY);
        button(c, l + W - 116, by, 98, 22, "BACK", mouseX, mouseY);
        c.drawText(textRenderer, text("Snake ON automatically enables that type's tracer  •  Color click: L next / R previous", false),
                l + 152, by + 7, 0xFF91899D, false);
    }

    private void pill(DrawContext c, int x, int y, int w, int h, String label, int color) {
        CincoDraw.rounded(c, x, y, x + w, y + h, h / 2, color);
        int tw = textRenderer.getWidth(label);
        c.drawText(textRenderer, text(label, true), x + w / 2 - tw / 2, y + 5, 0xFFFFFFFF, false);
    }

    private void button(DrawContext c, int x, int y, int w, int h, String label, int mx, int my) {
        boolean hover = mx >= x && mx <= x + w && my >= y && my <= y + h;
        CincoDraw.rounded(c, x, y, x + w, y + h, 10, hover ? 0xFF291A32 : 0xFF1A1322);
        int tw = textRenderer.getWidth(label);
        c.drawText(textRenderer, text(label, true), x + w / 2 - tw / 2, y + 7, 0xFFF8F4FF, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int l = left(), t = top();
        int sx = l + 20, sy = t + 61, sw = W - 40;
        if (button == 0 && mouseX >= sx && mouseX <= sx + sw && mouseY >= sy - 7 && mouseY <= sy + 14) {
            draggingRange = true;
            setRange(mouseX, sx, sw);
            return true;
        }

        int rowY = t + 100;
        StorageEspConfig.TypeKey[] types = StorageEspConfig.TypeKey.values();
        for (int i = 0; i < types.length; i++) {
            int y = rowY + i * ROW_H;
            if (mouseY < y || mouseY > y + 27) continue;
            StorageEspConfig.TypeKey type = types[i];
            StorageEspConfig.Entry setting = StorageEspConfig.get(type);
            if (mouseX >= l + 268 && mouseX <= l + 350 && button == 0) {
                setting.highlight = !setting.highlight;
                StorageEspConfig.save();
                return true;
            }
            if (mouseX >= l + 374 && mouseX <= l + 444 && button == 0) {
                setting.tracer = !setting.tracer;
                StorageEspConfig.save();
                return true;
            }
            if (mouseX >= l + 458 && mouseX <= l + 530 && button == 0) {
                setting.snakeTracer = !setting.snakeTracer;
                if (setting.snakeTracer) setting.tracer = true;
                StorageEspConfig.save();
                return true;
            }
            if (mouseX >= l + 546 && mouseX <= l + 682 && (button == 0 || button == 1)) {
                StorageEspConfig.cycleColor(type, button == 1 ? -1 : 1);
                return true;
            }
        }

        int by = t + H - 35;
        if (button == 0 && mouseY >= by && mouseY <= by + 22) {
            if (mouseX >= l + 18 && mouseX <= l + 136) {
                StorageEspConfig.resetDefaults();
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
        if (button == 0 && draggingRange) {
            setRange(mouseX, left() + 20, W - 40);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        draggingRange = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void setRange(double mouseX, int x, int w) {
        module.setSettingFromSlider((mouseX - x) / w);
        ConfigStore.save();
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(new CincoClickGuiScreen());
    }

    @Override
    public boolean shouldPause() { return false; }
}
