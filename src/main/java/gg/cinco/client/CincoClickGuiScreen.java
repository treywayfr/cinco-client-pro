package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CincoClickGuiScreen extends Screen {
    private static final int W = 680, H = 430, SIDE = 132, ROW = 55;
    private static final int BACKGROUND = 0xFF080611;
    private static final int PANEL = 0xFF100C1A;
    private static final int SIDEBAR = 0xFF151020;
    private static final int CARD = 0xFF1A1424;
    private static final int CARD_HOVER = 0xFF2A1D3A;
    private static final int PURPLE = 0xFFFF8C24;
    private static final int TEXT = 0xFFFFC078;
    private static final int MUTED = 0xFFE89745;

    private int scroll;
    private final long openedAt = System.currentTimeMillis();
    private final List<Star> stars = new ArrayList<>();
    private String search = "";
    private boolean searchFocused;
    private boolean draggingSlider;
    private ClientModule draggingModule;

    private static final class Star {
        final double x, speed, phase;
        double y;
        Star(double x, double y, double speed, double phase) {
            this.x = x; this.y = y; this.speed = speed; this.phase = phase;
        }
    }

    public CincoClickGuiScreen() {
        super(uiText("Cinco Client"));
        for (int i = 0; i < 150; i++) {
            stars.add(new Star(i * 73.37 % 1600, i * 47.19 % 1000, .15 + (i % 7) * .055, (i % 13) * .43));
        }
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        // Paint vanilla's background first. Never draw it over the custom controls.
        super.render(c, mx, my, delta);
        long now = System.currentTimeMillis();
        c.fill(0, 0, width, height, BACKGROUND);
        drawStars(c, now, delta);

        int l = left(), t = top();
        ClientModule animationModule = CincoClient.findModule("GUI Animation");
        double duration = animationModule == null || !animationModule.enabled ? 1.0 : animationModule.setting;
        double animation = animationModule == null || !animationModule.enabled ? 1.0 : Math.min(1.0, (now - openedAt) / duration);
        int lift = (int) ((1.0 - animation) * 10);
        int panelY = t + lift;

        // Opaque surfaces deliberately prevent the world and stars showing through text and cards.
        c.fill(l - 2, panelY - 2, l + W + 2, panelY + H + 2, 0xFFFF8C24);
        c.fill(l, panelY, l + W, panelY + H, panelColor(PANEL));
        c.fill(l, panelY, l + W, panelY + 3, PURPLE);
        c.fill(l, panelY + 3, l + SIDE, panelY + H, panelColor(SIDEBAR));
        c.fill(l + SIDE, panelY + 3, l + SIDE + 1, panelY + H, 0xFF4B2B16);

        // Brand header
        c.fill(l + 12, panelY + 11, l + 15, panelY + 39, PURPLE);
        c.drawText(textRenderer, uiText("CINCO CLIENT"), l + 23, panelY + 12, uiColor(TEXT), false);
        c.drawText(textRenderer, uiText("PURPLE NEBULA  /  v2.1"), l + 23, panelY + 27, uiColor(PURPLE), false);
        c.fill(l + W - 111, panelY + 13, l + W - 14, panelY + 34, 0xFF29180C);
        drawCenteredTextSharp(c, uiText("SHARP UI  •  READY"), l + W - 62, panelY + 20, 0xFFFFD18C);

        int catY = panelY + 58;
        for (ClientModule.Category category : ClientModule.Category.values()) {
            int y = catY + category.ordinal() * 34;
            boolean selected = category == CincoClient.selectedCategory;
            if (selected) {
                c.fill(l + 8, y - 5, l + SIDE - 8, y + 22, 0xFF4A2A16);
                c.fill(l + 8, y - 5, l + 11, y + 22, PURPLE);
            } else {
                c.fill(l + 14, y + 7, l + 17, y + 10, 0xFF8C5125);
            }
            c.drawText(textRenderer, uiText(category.label), l + 24, y + 3, uiColor(selected ? TEXT : MUTED), false);
        }

        int enabled = (int) CincoClient.MODULES.stream().filter(m -> m.enabled).count();
        c.fill(l + 12, panelY + H - 58, l + SIDE - 12, panelY + H - 57, 0xFF663A1A);
        c.drawText(textRenderer, uiText("CLIENT STATUS"), l + 15, panelY + H - 46, uiColor(0xFFE88736), false);
        c.drawText(textRenderer, uiText(enabled + " modules enabled"), l + 15, panelY + H - 31, uiColor(TEXT), false);
        c.drawText(textRenderer, uiText("RIGHT SHIFT  /  ESC"), l + 15, panelY + H - 17, uiColor(0xFF9F8ABF), false);

        int contentX = l + SIDE + 16;
        c.drawText(textRenderer, uiText(CincoClient.selectedCategory.label + " MODULES"),
                contentX, panelY + 13, uiColor(TEXT), false);
        c.drawText(textRenderer, uiText("Click card to toggle  •  drag slider to adjust"),
                contentX, panelY + 28, uiColor(MUTED), false);

        int searchX = l + W - 213, searchY = panelY + 10;
        c.fill(searchX, searchY, searchX + 194, searchY + 23, panelColor(searchFocused ? 0xFF573019 : 0xFF29180C));
        c.fill(searchX, searchY, searchX + 2, searchY + 23, searchFocused ? PURPLE : 0xFF9C571C);
        c.drawText(textRenderer, uiText((search.isEmpty() ? "SEARCH MODULES" : search.toUpperCase(Locale.ROOT))
                + (searchFocused ? " _" : "")), searchX + 9, searchY + 7, uiColor(TEXT), false);

        List<ClientModule> list = filteredModules();
        int viewportTop = panelY + 47, viewportBottom = panelY + H - 15;
        int visibleHeight = viewportBottom - viewportTop;
        int maxScroll = Math.max(0, list.size() * ROW - visibleHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        c.enableScissor(contentX, viewportTop, l + W - 13, viewportBottom);
        for (int i = 0; i < list.size(); i++) {
            ClientModule module = list.get(i);
            int y = viewportTop + i * ROW - scroll;
            if (y + ROW < viewportTop || y > viewportBottom) continue;
            boolean hovered = mx >= contentX && mx <= l + W - 13 && my >= y && my <= y + ROW - 5;
            c.fill(contentX, y, l + W - 13, y + ROW - 5, panelColor(hovered ? CARD_HOVER : CARD));
            c.fill(contentX, y, contentX + 3, y + ROW - 5, module.enabled ? PURPLE : 0xFF7A431B);
            c.drawText(textRenderer, uiText(module.name), contentX + 11, y + 6, uiColor(module.enabled ? TEXT : 0xFFFFBC72), false);
            c.drawText(textRenderer, uiText(module.description), contentX + 11, y + 20, uiColor(MUTED), false);

            int sliderX = l + W - 285, sliderWidth = 78, sliderY = y + 39;
            c.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 3, 0xFF6D3C1B);
            double fraction = (module.setting - module.min) / Math.max(.0001, module.max - module.min);
            int knob = sliderX + (int) (Math.max(0, Math.min(1, fraction)) * sliderWidth);
            c.fill(sliderX, sliderY, knob, sliderY + 3, PURPLE);
            c.fill(knob - 2, sliderY - 2, knob + 3, sliderY + 5, 0xFFFFFFFF);
            c.drawText(textRenderer, uiText(module.settingLabel + ": " + format(module.setting)),
                    sliderX + sliderWidth + 8, y + 35, uiColor(0xFFFFB86B), false);

            int toggleX = l + W - 79;
            c.fill(toggleX, y + 9, toggleX + 57, y + 26, module.enabled ? 0xFFB65B18 : 0xFF49301C);
            drawCenteredTextSharp(c, uiText(module.enabled ? "ON" : "OFF"), toggleX + 28, y + 14, TEXT);
        }
        c.disableScissor();

        // Animated accent glint is kept to the border so it never crosses text.
        int glintX = l + 8 + (int) ((now / (animationModule == null || !animationModule.enabled ? 12L : Math.max(2L, (long) animationModule.setting / 8L))) % (W - 16));
        c.fill(glintX, panelY, Math.min(l + W, glintX + 28), panelY + 3, 0xFFFFFFFF);
    }


    private Text uiText(String value) {
        ClientModule typography = CincoClient.findModule("Bold Typeface");
        if (typography != null && typography.enabled) {
            return Text.literal(value).setStyle(Style.EMPTY.withBold(true).withFont(Identifier.of("minecraft", "uniform")));
        }
        return Text.literal(value).setStyle(Style.EMPTY.withFont(Identifier.of("minecraft", "default")));
    }

    private int uiColor(int original) {
        ClientModule palette = CincoClient.findModule("Text Palette");
        int choice = palette == null || !palette.enabled ? 1 : (int) Math.round(palette.setting);
        int r = (original >> 16) & 255, g = (original >> 8) & 255, b = original & 255;
        boolean bright = (r * 299 + g * 587 + b * 114) / 1000 >= 170;
        if (choice == 2) return bright ? 0xFFFFD36B : 0xFFE09A22;
        if (choice >= 3) return bright ? 0xFFD9B3FF : 0xFFA87BDE;
        return bright ? 0xFFFFC078 : 0xFFE88736;
    }

    private int panelColor(int rgb) {
        ClientModule opacity = CincoClient.findModule("Panel Opacity");
        int alpha = opacity == null || !opacity.enabled ? 255 : (int) (Math.max(0.75, Math.min(1.0, opacity.setting)) * 255);
        return (alpha << 24) | (rgb & 0x00FFFFFF);
    }

    private void drawCenteredTextSharp(DrawContext context, Text value, int centerX, int y, int color) {
        context.drawText(textRenderer, value, centerX - textRenderer.getWidth(value) / 2, y, uiColor(color), false);
    }

    private void drawStars(DrawContext c, long now, float delta) {
        ClientModule density = CincoClient.findModule("Purple Stars");
        int amount = density == null ? stars.size()
                : (!density.enabled ? 0 : (int) (stars.size() * density.setting / 3.0));
        amount = Math.max(0, Math.min(stars.size(), amount));
        for (int i = 0; i < amount; i++) {
            Star star = stars.get(i);
            star.y += star.speed * (.8 + delta * .2);
            if (star.y > height) star.y = 0;
            double twinkle = (Math.sin(now / 370.0 + star.phase) + 1.0) / 2.0;
            int alpha = 95 + (int) (twinkle * 160);
            int size = i % 5 == 0 ? 2 : 1;
            int x = (int) (star.x % Math.max(1, width)), y = (int) star.y;
            c.fill(x, y, x + size, y + size, (alpha << 24) | 0x00C49AFF);
            if (twinkle > .84) c.fill(x - 2, y, x + size + 3, y + 1, 0x887C4DFF);
        }
    }

    private List<ClientModule> filteredModules() {
        List<ClientModule> out = new ArrayList<>();
        String query = search.trim().toLowerCase(Locale.ROOT);
        for (ClientModule module : CincoClient.MODULES) {
            if (module.category != CincoClient.selectedCategory) continue;
            if (!query.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(query)
                    && !module.description.toLowerCase(Locale.ROOT).contains(query)) continue;
            out.add(module);
        }
        return out;
    }

    private String format(double value) {
        return Math.abs(value - Math.round(value)) < .0001 ? Integer.toString((int) Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), t = top();
        int searchX = l + W - 213, searchY = t + 10;
        if (button == 0 && mx >= searchX && mx <= searchX + 194 && my >= searchY && my <= searchY + 23) {
            searchFocused = true;
            return true;
        }
        if (button == 0) searchFocused = false;

        if (button == 0) {
            for (ClientModule.Category category : ClientModule.Category.values()) {
                int y = t + 58 + category.ordinal() * 34;
                if (mx >= l + 8 && mx <= l + SIDE - 8 && my >= y - 5 && my <= y + 22) {
                    CincoClient.selectedCategory = category;
                    scroll = 0;
                    return true;
                }
            }

            int contentX = l + SIDE + 16, viewportTop = t + 47;
            List<ClientModule> list = filteredModules();
            for (int i = 0; i < list.size(); i++) {
                ClientModule module = list.get(i);
                int y = viewportTop + i * ROW - scroll;
                if (my < y || my > y + ROW - 5 || mx < contentX || mx > l + W - 13) continue;
                int sliderX = l + W - 285, sliderY = y + 36;
                if (mx >= sliderX - 3 && mx <= sliderX + 81 && my >= sliderY && my <= y + ROW - 3) {
                    draggingSlider = true;
                    draggingModule = module;
                    setFromMouse(module, mx, sliderX, 78);
                    return true;
                }
                module.enabled = !module.enabled;
                if (module.name.equals("Sprint Assist")) CincoClient.sprintAssistEnabled = module.enabled;
                if (module.name.equals("Client HUD")) CincoClient.hudEnabled = module.enabled;
                if (module.name.equals("Container Finder")) {
                    ClientModule base = CincoClient.findModule("Base Finder");
                    if (base != null) base.enabled = module.enabled;
                }
                if (module.name.equals("Base Finder")) {
                    ClientModule containers = CincoClient.findModule("Container Finder");
                    if (containers != null) containers.enabled = module.enabled;
                    if (!module.enabled) BaseScanner.reset();
                }
                ConfigStore.save();
                return true;
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    private void setFromMouse(ClientModule module, double mouseX, int sliderX, int sliderWidth) {
        module.setSettingFromSlider((mouseX - sliderX) / (double) sliderWidth);
        ConfigStore.save();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider && draggingModule != null) {
            setFromMouse(draggingModule, mx, left() + W - 285, 78);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        draggingSlider = false;
        draggingModule = null;
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll -= (int) (vertical * ROW * 2);
        return true;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr)) {
            search += chr;
            scroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        if (searchFocused && key == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
            search = search.substring(0, search.length() - 1);
            scroll = 0;
            return true;
        }
        if (key == GLFW.GLFW_KEY_RIGHT_SHIFT || key == GLFW.GLFW_KEY_ESCAPE) {
            if (searchFocused && !search.isEmpty()) {
                search = "";
                searchFocused = false;
                return true;
            }
            close();
            return true;
        }
        if (key == GLFW.GLFW_KEY_SLASH) {
            searchFocused = true;
            return true;
        }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }

    @Override
    public boolean shouldPause() { return false; }
}
