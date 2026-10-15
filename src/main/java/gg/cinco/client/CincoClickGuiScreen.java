package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Compact modern ClickGUI with anti-aliased cards and denser module sizing. */
public final class CincoClickGuiScreen extends Screen {
    private static final int W = 620;
    private static final int H = 388;
    private static final int SIDE = 112;
    private static final int ROW = 44;

    private int scroll;
    private String search = "";
    private boolean searchFocused;
    private boolean draggingSlider;
    private ClientModule draggingModule;
    private final long openedAt = System.currentTimeMillis();
    private final List<Star> stars = new ArrayList<>();

    private static final class Star {
        final double x, speed, phase;
        double y;
        Star(double x, double y, double speed, double phase) {
            this.x = x; this.y = y; this.speed = speed; this.phase = phase;
        }
    }

    public CincoClickGuiScreen() {
        super(Text.literal("Cinco Client"));
        for (int i = 0; i < 125; i++)
            stars.add(new Star(i * 73.37 % 1600, i * 47.19 % 1000,
                    0.12 + (i % 7) * 0.045, (i % 13) * 0.43));
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }

    private Text ui(String value, boolean bold) {
        ClientModule typography = CincoClient.findModule("Bold Typeface");
        boolean strong = bold || (typography != null && typography.enabled);
        return Text.literal(value).setStyle(Style.EMPTY
                .withFont(Identifier.of("minecraft", "uniform"))
                .withBold(strong));
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        long now = System.currentTimeMillis();
        c.fill(0, 0, width, height, CincoTheme.background());
        drawStars(c, now, delta);

        int l = left(), t = top();
        ClientModule anim = CincoClient.findModule("GUI Animation");
        double duration = anim == null || !anim.enabled ? 1.0 : Math.max(80.0, anim.setting);
        double progress = anim == null || !anim.enabled ? 1.0 : Math.min(1.0, (now - openedAt) / duration);
        int lift = (int) ((1.0 - progress) * 8.0);
        int py = t + lift;

        CincoDraw.softShadow(c, l, py, l + W, py + H, 16, CincoTheme.accent(), 80);
        CincoDraw.bubble(c, l, py, l + W, py + H, 16, CincoTheme.accent(), CincoTheme.panel());
        CincoDraw.rounded(c, l + 6, py + 6, l + SIDE - 4, py + H - 6, 12, CincoTheme.sidebar());

        // Compact brand / category rail.
        CincoDraw.rounded(c, l + 14, py + 14, l + 34, py + 34, 10, CincoTheme.accent());
        c.drawText(textRenderer, ui("C", true), l + 21, py + 20, 0xFFFFFFFF, false);
        c.drawText(textRenderer, ui("CINCO", true), l + 42, py + 15, CincoTheme.primaryText(), false);
        c.drawText(textRenderer, ui("CLIENT 2.3", false), l + 42, py + 27, CincoTheme.secondaryText(), false);

        int catTop = py + 54;
        for (ClientModule.Category category : ClientModule.Category.values()) {
            int y = catTop + category.ordinal() * 30;
            boolean selected = category == CincoClient.selectedCategory;
            if (selected) {
                CincoDraw.rounded(c, l + 10, y, l + SIDE - 10, y + 23, 9, CincoTheme.accentDark());
                CincoDraw.rounded(c, l + 13, y + 6, l + 17, y + 17, 2, CincoTheme.accent());
            }
            c.drawText(textRenderer, ui(category.label, selected), l + 24, y + 7,
                    selected ? CincoTheme.primaryText() : CincoTheme.secondaryText(), false);
        }

        int enabled = (int) CincoClient.MODULES.stream().filter(m -> m.enabled).count();
        CincoDraw.rounded(c, l + 10, py + H - 49, l + SIDE - 10, py + H - 12, 10, CincoTheme.card());
        c.drawText(textRenderer, ui("ACTIVE", true), l + 18, py + H - 41, CincoTheme.secondaryText(), false);
        c.drawText(textRenderer, ui(enabled + " MODULES", true), l + 18, py + H - 27, CincoTheme.accentLight(), false);

        int contentX = l + SIDE + 13;
        int contentRight = l + W - 12;
        c.drawText(textRenderer, ui(CincoClient.selectedCategory.label, true), contentX, py + 16,
                CincoTheme.primaryText(), false);
        c.drawText(textRenderer, ui("LEFT CLICK TOGGLE  •  RIGHT CLICK SETTINGS", false), contentX, py + 30,
                CincoTheme.secondaryText(), false);

        int searchW = 165, searchX = contentRight - searchW, searchY = py + 11;
        CincoDraw.bubble(c, searchX, searchY, contentRight, searchY + 25, 10,
                searchFocused ? CincoTheme.accent() : CincoTheme.line(),
                searchFocused ? CincoTheme.cardHover() : CincoTheme.card());
        String searchText = search.isEmpty() ? "SEARCH /" : search.toUpperCase(Locale.ROOT) + (searchFocused ? " _" : "");
        c.drawText(textRenderer, ui(searchText, false), searchX + 9, searchY + 8, CincoTheme.primaryText(), false);

        List<ClientModule> list = filteredModules();
        int viewportTop = py + 52;
        int viewportBottom = py + H - 12;
        int visibleHeight = viewportBottom - viewportTop;
        int maxScroll = Math.max(0, list.size() * ROW - visibleHeight);
        scroll = Math.max(0, Math.min(scroll, maxScroll));

        c.enableScissor(contentX, viewportTop, contentRight + 1, viewportBottom);
        for (int i = 0; i < list.size(); i++) {
            ClientModule module = list.get(i);
            int y = viewportTop + i * ROW - scroll;
            if (y + ROW < viewportTop || y > viewportBottom) continue;
            int bottom = y + ROW - 5;
            boolean hover = mx >= contentX && mx <= contentRight && my >= y && my <= bottom;

            if (hover) CincoDraw.softShadow(c, contentX, y, contentRight, bottom, 10, CincoTheme.accent(), 55);
            CincoDraw.bubble(c, contentX, y, contentRight, bottom, 10,
                    hover ? CincoTheme.accentMuted() : CincoTheme.line(),
                    hover ? CincoTheme.cardHover() : CincoTheme.card());

            CincoDraw.rounded(c, contentX + 7, y + 7, contentX + 11, bottom - 7, 2,
                    module.enabled ? CincoTheme.accent() : CincoTheme.disabled());
            c.drawText(textRenderer, ui(module.name.toUpperCase(Locale.ROOT), true),
                    contentX + 18, y + 6,
                    module.enabled ? CincoTheme.primaryText() : CincoTheme.secondaryText(), false);
            c.drawText(textRenderer, ui(trim(module.description, 51), false),
                    contentX + 18, y + 18, CincoTheme.secondaryText(), false);

            int toggleW = 46, toggleX = contentRight - toggleW - 8;
            CincoDraw.rounded(c, toggleX, y + 7, toggleX + toggleW, y + 23, 8,
                    module.enabled ? CincoTheme.enabled() : CincoTheme.disabled());
            String state = module.enabled ? "ON" : "OFF";
            int tw = textRenderer.getWidth(state);
            c.drawText(textRenderer, ui(state, true), toggleX + toggleW / 2 - tw / 2,
                    y + 12, 0xFFFFFFFF, false);

            int sliderX = contentRight - 196, sliderY = y + 31, sliderW = 100;
            CincoDraw.rounded(c, sliderX, sliderY, sliderX + sliderW, sliderY + 4, 2, CincoTheme.sliderTrack());
            double fraction = (module.setting - module.min) / Math.max(0.0001, module.max - module.min);
            int knob = sliderX + (int) Math.round(Math.max(0, Math.min(1, fraction)) * sliderW);
            CincoDraw.rounded(c, sliderX, sliderY, knob, sliderY + 4, 2, CincoTheme.accent());
            CincoDraw.rounded(c, knob - 3, sliderY - 2, knob + 4, sliderY + 6, 4, CincoTheme.accentLight());
            String setting = module.settingLabel + " " + format(module.setting);
            c.drawText(textRenderer, ui(setting.toUpperCase(Locale.ROOT), false), sliderX - 105, y + 29,
                    CincoTheme.secondaryText(), false);
        }
        c.disableScissor();
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private void drawStars(DrawContext c, long now, float delta) {
        ClientModule density = CincoClient.findModule("Purple Stars");
        int amount = density == null ? stars.size() : (!density.enabled ? 0 : (int) (stars.size() * density.setting / 3.0));
        amount = Math.max(0, Math.min(stars.size(), amount));
        for (int i = 0; i < amount; i++) {
            Star star = stars.get(i);
            star.y += star.speed * (0.8 + delta * 0.2);
            if (star.y > height) star.y = 0;
            double pulse = (Math.sin(now / 390.0 + star.phase) + 1.0) * 0.5;
            int alpha = 55 + (int) (pulse * 135);
            int x = (int) (star.x % Math.max(1, width)), y = (int) star.y;
            c.fill(x, y, x + 1, y + 1, (alpha << 24) | (CincoTheme.accent() & 0x00FFFFFF));
        }
    }

    private List<ClientModule> filteredModules() {
        List<ClientModule> out = new ArrayList<>();
        String q = search.trim().toLowerCase(Locale.ROOT);
        for (ClientModule module : CincoClient.MODULES) {
            if (module.category != CincoClient.selectedCategory) continue;
            if (!q.isEmpty() && !module.name.toLowerCase(Locale.ROOT).contains(q)
                    && !module.description.toLowerCase(Locale.ROOT).contains(q)) continue;
            out.add(module);
        }
        return out;
    }

    private String format(double value) {
        return Math.abs(value - Math.round(value)) < 0.0001
                ? Integer.toString((int) Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), t = top();
        int contentX = l + SIDE + 13, contentRight = l + W - 12;
        int searchW = 165, searchX = contentRight - searchW, searchY = t + 11;
        if (button == 0 && mx >= searchX && mx <= contentRight && my >= searchY && my <= searchY + 25) {
            searchFocused = true;
            return true;
        }
        if (button == 0) searchFocused = false;

        for (ClientModule.Category category : ClientModule.Category.values()) {
            int y = t + 54 + category.ordinal() * 30;
            if (button == 0 && mx >= l + 10 && mx <= l + SIDE - 10 && my >= y && my <= y + 23) {
                CincoClient.selectedCategory = category;
                scroll = 0;
                return true;
            }
        }

        List<ClientModule> list = filteredModules();
        int viewportTop = t + 52;
        for (int i = 0; i < list.size(); i++) {
            ClientModule module = list.get(i);
            int y = viewportTop + i * ROW - scroll;
            if (mx < contentX || mx > contentRight || my < y || my > y + ROW - 5) continue;

            if (button == 1) {
                if (module.name.equals("Storage ESP")) MinecraftClient.getInstance().setScreen(new StorageEspSettingsScreen(module));
                else MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(module));
                return true;
            }
            if (button != 0) continue;

            if (module.name.equals("Color Wheel")) {
                MinecraftClient.getInstance().setScreen(new CincoColorWheelScreen(module, this));
                return true;
            }
            int sliderX = contentRight - 196, sliderY = y + 28;
            if (mx >= sliderX - 4 && mx <= sliderX + 104 && my >= sliderY && my <= sliderY + 12) {
                draggingSlider = true;
                draggingModule = module;
                setFromMouse(module, mx, sliderX, 100);
                return true;
            }
            toggle(module);
            return true;
        }
        return super.mouseClicked(mx, my, button);
    }

    private void toggle(ClientModule module) {
        if (module.name.equals("Open GUI") || module.name.equals("Toggle HUD")) {
            MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(module));
            return;
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
    }

    private void setFromMouse(ClientModule module, double mouseX, int sliderX, int sliderWidth) {
        module.setSettingFromSlider((mouseX - sliderX) / (double) sliderWidth);
        ConfigStore.save();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider && draggingModule != null) {
            int contentRight = left() + W - 12;
            setFromMouse(draggingModule, mx, contentRight - 196, 100);
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
        if (key == GLFW.GLFW_KEY_SLASH) {
            searchFocused = true;
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
        return super.keyPressed(key, scan, modifiers);
    }

    @Override
    public void close() { MinecraftClient.getInstance().setScreen(null); }

    @Override
    public boolean shouldPause() { return false; }
}
