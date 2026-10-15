package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/** Star Client's compact two-column animated ClickGUI. */
public final class CincoClickGuiScreen extends Screen {
    private static final int W = 700;
    private static final int H = 420;
    private static final int SIDE = 126;
    private static final int CARD_H = 58;
    private static final int GAP = 8;
    private static final Set<String> FAVORITES = new LinkedHashSet<>();

    private int scroll;
    private String search = "";
    private boolean searchFocused;
    private boolean draggingSlider;
    private ClientModule draggingModule;
    private int draggingSliderX;
    private int draggingSliderW;
    private final long openedAt = System.currentTimeMillis();
    private boolean closing;
    private long closeStarted;
    private final List<Star> stars = new ArrayList<>();
    private final Map<String, StarAnimations.Track> hoverAnim = new HashMap<>();
    private final Map<String, StarAnimations.Track> toggleAnim = new HashMap<>();
    private final Map<String, StarAnimations.Track> categoryAnim = new HashMap<>();
    private final Map<String, Long> clickAt = new HashMap<>();
    private final StarAnimations.Track searchAnim = new StarAnimations.Track(0f);

    private static final class Star {
        final double x, phase, speed;
        double y;
        Star(double x, double y, double speed, double phase) {
            this.x = x; this.y = y; this.speed = speed; this.phase = phase;
        }
    }

    public CincoClickGuiScreen() {
        super(Text.literal("Star Client"));
        for (int i = 0; i < 42; i++) {
            stars.add(new Star(i * 97.13 % 1600, i * 61.77 % 900,
                    0.07 + (i % 5) * 0.025, i * 0.37));
        }
    }

    private int left() { return (width - W) / 2; }
    private int top() { return (height - H) / 2; }
    private Text ui(String value, boolean bold) { return StarFonts.text(value, bold); }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        MinecraftClient client = MinecraftClient.getInstance();
        long now = System.currentTimeMillis();

        int openDuration = StarAnimations.duration(client, StarAnimations.MENU_OPEN_MS, true);
        float open = StarAnimations.progress(now - openedAt, openDuration, StarAnimations.Curve.CUBIC_OUT);
        if (closing) {
            int closeDuration = StarAnimations.duration(client, StarAnimations.MENU_CLOSE_MS, true);
            float close = StarAnimations.progress(now - closeStarted, closeDuration, StarAnimations.Curve.CUBIC_IN_OUT);
            open = 1f - close;
            if (close >= 1f) {
                super.close();
                return;
            }
        }

        c.fill(0, 0, width, height, 0xEE06050A);
        drawStars(c, now, delta);

        int l = left(), t = top();
        int py = t + Math.round((1f - open) * 18f);

        // One shadow for the entire interface. Performance tiers can remove it before FPS tanks.
        if (StarPerformance.allowMenuShadow(client))
            CincoDraw.softShadow(c, l, py, l + W, py + H, 18, CincoTheme.accent(), Math.round(52 * open));
        CincoDraw.bubble(c, l, py, l + W, py + H, 18, CincoTheme.accentMuted(), 0xF20B0911);
        CincoDraw.rounded(c, l + 6, py + 6, l + SIDE, py + H - 6, 14, 0xF00F0C17);

        // Brand block.
        CincoDraw.rounded(c, l + 14, py + 14, l + 46, py + 46, 12, CincoTheme.accentDark());
        c.drawCenteredTextWithShadow(textRenderer, ui("★", true), l + 30, py + 25, CincoTheme.starGold());
        c.drawText(textRenderer, ui("STAR", true), l + 55, py + 17, 0xFFFFFFFF, false);
        c.drawText(textRenderer, ui("CLIENT", false), l + 55, py + 31, CincoTheme.secondaryText(), false);

        int catTop = py + 62;
        for (ClientModule.Category category : ClientModule.Category.values()) {
            int y = catTop + category.ordinal() * 31;
            boolean selected = category == CincoClient.selectedCategory;
            float selectedAnim = animate(categoryAnim, "category:" + category.name(), selected ? 1f : 0f,
                    selected ? 1f : 0f, StarAnimations.CATEGORY_MS, StarAnimations.Curve.CUBIC_IN_OUT, true, now);
            if (selectedAnim > 0.01f) {
                CincoDraw.rounded(c, l + 11, y, l + SIDE - 10, y + 24, 9,
                        withAlpha(CincoTheme.accentDark(), Math.round(50 + 155 * selectedAnim)));
                int railH = Math.max(2, Math.round(10f * selectedAnim));
                int cy = y + 12;
                CincoDraw.rounded(c, l + 15, cy - railH / 2, l + 18, cy + (railH + 1) / 2,
                        2, mix(CincoTheme.magenta(), CincoTheme.cyan(), selectedAnim));
            }
            c.drawText(textRenderer, ui(category.label, selected), l + 25, y + 8,
                    mix(0xFF827C91, 0xFFFFFFFF, selectedAnim), false);
        }

        int enabled = (int) CincoClient.MODULES.stream().filter(m -> m.enabled).count();
        CincoDraw.rounded(c, l + 12, py + H - 55, l + SIDE - 11, py + H - 13, 11, 0xCC171220);
        c.drawText(textRenderer, ui("★  " + enabled + " ACTIVE", true), l + 20, py + H - 43, CincoTheme.starGold(), false);
        ClientModule perf = CincoClient.findModule("Performance Mode");
        String perfText = perf != null && perf.enabled ? "ADAPTIVE FPS" : "QUALITY MODE";
        c.drawText(textRenderer, ui(perfText, false), l + 20, py + H - 28, 0xFF8C849A, false);

        int contentX = l + SIDE + 16;
        int contentRight = l + W - 14;
        c.drawText(textRenderer, ui(CincoClient.selectedCategory.label, true), contentX, py + 17, 0xFFFFFFFF, false);
        c.drawText(textRenderer, ui("LMB toggle  •  RMB settings  •  MMB ★ favorite", false),
                contentX, py + 32, 0xFF827C91, false);

        int searchW = 180, searchX = contentRight - searchW, searchY = py + 12;
        float sf = searchAnim.to(searchFocused ? 1f : 0f,
                StarAnimations.duration(client, StarAnimations.SEARCH_FOCUS_MS, false),
                StarAnimations.Curve.CUBIC_OUT, now);
        CincoDraw.bubble(c, searchX, searchY, contentRight, searchY + 29, 11,
                mix(0xFF2A2434, CincoTheme.cyan(), sf * 0.70f),
                mix(0xFF120F18, 0xFF181020, sf));
        String s = search.isEmpty() ? "⌕ SEARCH MODULES" : search.toUpperCase(Locale.ROOT) + (searchFocused ? "  _" : "");
        c.drawText(textRenderer, ui(s, false), searchX + 10, searchY + 10,
                mix(0xFF817A8C, 0xFFF5EFFF, sf), false);

        List<ClientModule> list = filteredModules();
        int viewportTop = py + 57;
        int viewportBottom = py + H - 13;
        int columns = 2;
        int cardW = (contentRight - contentX - GAP) / columns;
        int rows = (list.size() + 1) / 2;
        int rowPitch = CARD_H + GAP;
        int maxScroll = Math.max(0, rows * rowPitch - (viewportBottom - viewportTop));
        scroll = Math.max(0, Math.min(scroll, maxScroll));
        int animationCap = StarPerformance.animatedCardCap(client);

        c.enableScissor(contentX - 2, viewportTop, contentRight + 2, viewportBottom);
        for (int i = 0; i < list.size(); i++) {
            ClientModule module = list.get(i);
            int col = i & 1;
            int row = i >> 1;
            int x = contentX + col * (cardW + GAP);
            int y = viewportTop + row * rowPitch - scroll;
            if (y + CARD_H < viewportTop || y > viewportBottom) continue;

            boolean hover = mx >= x && mx <= x + cardW && my >= y && my <= y + CARD_H;
            boolean animated = i < animationCap || hover;
            boolean decorativeHover = hover && StarPerformance.allowDecorativeHover(client);
            float h = animate(hoverAnim, module.name, decorativeHover ? 1f : 0f, 0f,
                    decorativeHover ? StarAnimations.HOVER_IN_MS : StarAnimations.HOVER_OUT_MS,
                    StarAnimations.Curve.CUBIC_OUT, false, now, animated);
            float e = animate(toggleAnim, module.name, module.enabled ? 1f : 0f,
                    module.enabled ? 1f : 0f, StarAnimations.TOGGLE_MS,
                    StarAnimations.Curve.CUBIC_IN_OUT, true, now, animated);
            float click = StarAnimations.clickPulse(clickAt.getOrDefault(module.name, 0L), now, client);
            int drawY = y - Math.round(h * 2f) + Math.round(click);

            int border = mix(0xFF282231, CincoTheme.magenta(), h * 0.72f);
            border = mix(border, CincoTheme.starGold(), click * 0.20f);
            int fill = mix(0xE5120F18, 0xF0211728, h);
            CincoDraw.bubble(c, x, drawY, x + cardW, drawY + CARD_H, 12, border, fill);

            // Thin animated status rail instead of expensive per-card glows.
            CincoDraw.rounded(c, x + 7, drawY + 8, x + 10, drawY + CARD_H - 8, 2,
                    mix(0xFF373140, CincoTheme.cyan(), e));

            boolean favorite = FAVORITES.contains(module.name);
            c.drawText(textRenderer, ui((favorite ? "★ " : "") + module.name, true), x + 17, drawY + 8,
                    mix(0xFFAAA4B2, 0xFFFFFFFF, Math.max(h, e)), false);
            c.drawText(textRenderer, ui(trim(module.description, 30), false), x + 17, drawY + 22,
                    mix(0xFF716B7B, 0xFFAAA2B5, h), false);

            // Toggle: 175 ms cubic transition; no overshoot so the knob never escapes its track.
            int sw = 34, sx = x + cardW - sw - 10, sy = drawY + 9;
            CincoDraw.rounded(c, sx, sy, sx + sw, sy + 15, 8,
                    mix(0xFF312B38, CincoTheme.enabled(), e));
            int knob = sx + 3 + Math.round(e * (sw - 14));
            CincoDraw.rounded(c, knob, sy + 3, knob + 9, sy + 12, 5, 0xFFFFFFFF);

            // Compact main-setting slider. Direct dragging remains immediate; visual emphasis is cheap.
            int sliderX = x + 17, sliderY = drawY + 43, sliderW = cardW - 34;
            CincoDraw.rounded(c, sliderX, sliderY, sliderX + sliderW, sliderY + 3, 2, 0xFF2E2935);
            double f = (module.setting - module.min) / Math.max(0.0001, module.max - module.min);
            int fillX = sliderX + (int) Math.round(Math.max(0, Math.min(1, f)) * sliderW);
            if (fillX > sliderX) CincoDraw.rounded(c, sliderX, sliderY, fillX, sliderY + 3, 2, CincoTheme.accent());
            CincoDraw.rounded(c, fillX - 2, sliderY - 2, fillX + 3, sliderY + 5, 3,
                    mix(CincoTheme.accentLight(), CincoTheme.starGold(), click * 0.30f));
            String settingText = shortSetting(module);
            int tw = textRenderer.getWidth(settingText);
            c.drawText(textRenderer, ui(settingText, false), x + cardW - tw - 10, drawY + 32, 0xFF948DA0, false);
        }
        c.disableScissor();
    }

    private float animate(Map<String, StarAnimations.Track> map, String key, float target, float initial,
                          int baseDuration, StarAnimations.Curve curve, boolean essential, long now) {
        return animate(map, key, target, initial, baseDuration, curve, essential, now, true);
    }

    private float animate(Map<String, StarAnimations.Track> map, String key, float target, float initial,
                          int baseDuration, StarAnimations.Curve curve, boolean essential, long now,
                          boolean allowAnimation) {
        StarAnimations.Track track = map.computeIfAbsent(key, k -> new StarAnimations.Track(initial));
        int duration = allowAnimation ? StarAnimations.duration(MinecraftClient.getInstance(), baseDuration, essential) : 0;
        return track.to(target, duration, curve, now);
    }

    private void drawStars(DrawContext c, long now, float delta) {
        ClientModule density = CincoClient.findModule("Purple Stars");
        if (density != null && !density.enabled) return;
        MinecraftClient client = MinecraftClient.getInstance();
        int requested = density == null ? stars.size() : (int) Math.round(stars.size() * density.setting / 3.0);
        int amount = Math.min(Math.min(stars.size(), requested), StarPerformance.guiStarCap(client));
        boolean lowMotion = StarPerformance.animationTier(client) == 0;
        for (int i = 0; i < amount; i++) {
            Star star = stars.get(i);
            if (!lowMotion) star.y += star.speed * Math.max(0.6, delta);
            if (star.y > height) star.y = 0;
            int alpha = lowMotion ? 42 : 35 + (int) (((Math.sin(now / 620.0 + star.phase) + 1.0) * 0.5) * 90);
            int x = (int) (star.x % Math.max(1, width));
            int y = (int) star.y;
            c.fill(x, y, x + 1, y + 1, (alpha << 24) | (CincoTheme.accentLight() & 0x00FFFFFF));
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
        out.sort(Comparator.comparing((ClientModule m) -> !FAVORITES.contains(m.name))
                .thenComparing(m -> m.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    private String shortSetting(ClientModule module) {
        if (module.name.equals("Font Style")) return StarFonts.name((int) Math.round(module.setting));
        if (module.name.equals("Client Profile")) return StarProfiles.name((int) Math.round(module.setting));
        if (module.name.equals("ESP Style")) return switch ((int) Math.round(module.setting)) {
            case 2 -> "Neon";
            case 3 -> "Minimal";
            case 4 -> "Solid";
            default -> "Clean";
        };
        return trim(module.settingLabel, 11) + " " + format(module.setting);
    }

    private String trim(String value, int max) {
        return value.length() <= max ? value : value.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String format(double value) {
        return Math.abs(value - Math.round(value)) < 0.0001
                ? Integer.toString((int) Math.round(value))
                : String.format(Locale.ROOT, "%.1f", value);
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int l = left(), t = top();
        int contentX = l + SIDE + 16, contentRight = l + W - 14;
        int searchW = 180, searchX = contentRight - searchW, searchY = t + 12;
        long now = System.currentTimeMillis();
        if (button == 0 && mx >= searchX && mx <= contentRight && my >= searchY && my <= searchY + 29) {
            searchFocused = true;
            return true;
        }
        if (button == 0) searchFocused = false;

        for (ClientModule.Category category : ClientModule.Category.values()) {
            int y = t + 62 + category.ordinal() * 31;
            if (button == 0 && mx >= l + 11 && mx <= l + SIDE - 10 && my >= y && my <= y + 24) {
                CincoClient.selectedCategory = category;
                scroll = 0;
                return true;
            }
        }

        List<ClientModule> list = filteredModules();
        int viewportTop = t + 57;
        int cardW = (contentRight - contentX - GAP) / 2;
        int rowPitch = CARD_H + GAP;
        for (int i = 0; i < list.size(); i++) {
            ClientModule module = list.get(i);
            int x = contentX + (i & 1) * (cardW + GAP);
            int y = viewportTop + (i >> 1) * rowPitch - scroll;
            if (mx < x || mx > x + cardW || my < y || my > y + CARD_H) continue;

            clickAt.put(module.name, now);
            if (button == 2) {
                if (!FAVORITES.add(module.name)) FAVORITES.remove(module.name);
                return true;
            }
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
            int sliderX = x + 17, sliderY = y + 37, sliderW = cardW - 34;
            if (mx >= sliderX - 4 && mx <= sliderX + sliderW + 4 && my >= sliderY && my <= y + CARD_H) {
                draggingSlider = true;
                draggingModule = module;
                draggingSliderX = sliderX;
                draggingSliderW = sliderW;
                setFromMouse(module, mx, sliderX, sliderW);
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
        if (module.name.equals("Client Profile")) StarProfiles.apply((int) Math.round(module.setting));
        ConfigStore.save();
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (draggingSlider && draggingModule != null) {
            setFromMouse(draggingModule, mx, draggingSliderX, draggingSliderW);
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
        scroll -= (int) (vertical * 45);
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
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                scroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void close() {
        if (closing) return;
        int duration = StarAnimations.duration(MinecraftClient.getInstance(), StarAnimations.MENU_CLOSE_MS, true);
        if (duration <= 0) {
            super.close();
            return;
        }
        closing = true;
        closeStarted = System.currentTimeMillis();
    }

    private int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa = (a >>> 24) & 255, ar = (a >>> 16) & 255, ag = (a >>> 8) & 255, ab = a & 255;
        int ba = (b >>> 24) & 255, br = (b >>> 16) & 255, bg = (b >>> 8) & 255, bb = b & 255;
        return ((int)(aa + (ba-aa)*t) << 24) | ((int)(ar + (br-ar)*t) << 16)
                | ((int)(ag + (bg-ag)*t) << 8) | (int)(ab + (bb-ab)*t);
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }
}
