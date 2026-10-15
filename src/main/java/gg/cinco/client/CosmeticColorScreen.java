package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.Locale;

/** Dedicated color editor for accessories and world cosmetic effects. */
public final class CosmeticColorScreen extends StarLegacyScreen {
    public enum Target { ACCESSORY, EFFECT }

    private static final int W = 430;
    private static final int H = 250;
    private final Target target;
    private final ModuleSettingsScreen parent;
    private int dragging = -1;

    public CosmeticColorScreen(Target target, ModuleSettingsScreen parent) {
        super(Text.literal("Cosmetic Color"));
        this.target = target;
        this.parent = parent;
    }

    private CosmeticColorStore.Profile profile() {
        return target == Target.ACCESSORY ? CosmeticColorStore.accessory() : CosmeticColorStore.effect();
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    public void render(DrawContext c, int mouseX, int mouseY, float delta) {
        super.render(c, mouseX, mouseY, delta);
        c.fill(0, 0, width, height, 0xE8020205);
        StarBackground.render(c, width, height, delta);
        int l = left(), t = top();
        CincoDraw.softShadow(c, l, t, l + W, t + H, 15, 0xFFA968FF, 46);
        CincoDraw.bubble(c, l, t, l + W, t + H, 14, 0xFF5F3B82, 0xF40A0810);
        c.drawText(textRenderer, StarFonts.text(target == Target.ACCESSORY ? "ACCESSORY COLOR" : "ANIMATION / AURA COLOR", true), l + 20, t + 18, 0xFFFFFFFF, false);
        c.drawText(textRenderer, StarFonts.text("Live cosmetic palette", false), l + 20, t + 34, 0xFFC7BED2, false);

        CosmeticColorStore.Profile p = profile();
        drawSlider(c, l + 20, t + 70, W - 40, "HUE", p.hue, 0, 360);
        drawSlider(c, l + 20, t + 118, W - 40, "SATURATION", p.saturation, 0, 1);
        drawSlider(c, l + 20, t + 166, W - 40, "BRIGHTNESS", p.brightness, 0.20, 1);

        int preview = CosmeticColorStore.color(p, 255);
        CincoDraw.bubble(c, l + W - 72, t + 14, l + W - 20, t + 48, 10, 0xCFFFFFFF, preview);
        CincoDraw.bubble(c, l + 20, t + H - 38, l + 118, t + H - 14, 8, 0xAFFFFFFF, 0xFF16101E);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("RESET", true), l + 69, t + H - 30, 0xFFFFFFFF);
        CincoDraw.bubble(c, l + W - 118, t + H - 38, l + W - 20, t + H - 14, 8, 0xAFFFFFFF, 0xFF2B1640);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("BACK", true), l + W - 69, t + H - 30, 0xFFFFFFFF);
    }

    private void drawSlider(DrawContext c, int x, int y, int w, String name, double value, double min, double max) {
        c.drawText(textRenderer, StarFonts.text(name, true), x, y, 0xFFFFFFFF, false);
        String shown = name.equals("HUE") ? Integer.toString((int)Math.round(value)) + "°" : String.format(Locale.ROOT, "%d%%", (int)Math.round(value * 100.0));
        c.drawText(textRenderer, StarFonts.text(shown, true), x + w - textRenderer.getWidth(shown), y, 0xFFC9A7FF, false);
        int sy = y + 20;
        CincoDraw.rounded(c, x, sy, x + w, sy + 7, 4, 0xFF2B2434);
        double f = (value - min) / Math.max(0.000001, max - min);
        int fill = x + (int)Math.round(Math.max(0, Math.min(1, f)) * w);
        if (fill > x) CincoDraw.rounded(c, x, sy, fill, sy + 7, 4, CosmeticColorStore.color(profile(), 255));
        CincoDraw.rounded(c, fill - 4, sy - 3, fill + 5, sy + 10, 5, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (button != 0) return super.mouseClicked(mx, my, button);
        int l = left(), t = top();
        for (int i = 0; i < 3; i++) {
            int y = t + 70 + i * 48 + 16;
            if (mx >= l + 16 && mx <= l + W - 16 && my >= y && my <= y + 18) {
                dragging = i;
                update(mx);
                return true;
            }
        }
        if (my >= t + H - 38 && my <= t + H - 14) {
            if (mx >= l + 20 && mx <= l + 118) {
                CosmeticColorStore.Profile p = profile();
                p.hue = 276.0; p.saturation = target == Target.ACCESSORY ? 0.72 : 0.78; p.brightness = 1.0;
                CosmeticColorStore.save();
                return true;
            }
            if (mx >= l + W - 118 && mx <= l + W - 20) {
                close();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (dragging >= 0) { update(mx); return true; }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (dragging >= 0) {
            dragging = -1;
            CosmeticColorStore.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    private void update(double mx) {
        int x = left() + 20, w = W - 40;
        double f = Math.max(0, Math.min(1, (mx - x) / w));
        CosmeticColorStore.Profile p = profile();
        if (dragging == 0) p.hue = f * 360.0;
        else if (dragging == 1) p.saturation = f;
        else if (dragging == 2) p.brightness = 0.20 + f * 0.80;
    }

    @Override
    public void close() {
        CosmeticColorStore.save();
        MinecraftClient.getInstance().setScreen(parent);
    }
}
