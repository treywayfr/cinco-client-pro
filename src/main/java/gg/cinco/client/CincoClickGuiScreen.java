package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Star Client GUI inspired by the supplied two-mode concept.
 * Orbit HUD = compact floating draggable/resizable windows.
 * Nebula ClickGUI = one polished sidebar/list/settings workspace.
 */
public final class CincoClickGuiScreen extends Screen {
    private static final long SESSION_START = System.currentTimeMillis();
    private static final int MODE_W = 286;
    private static final int MODE_H = 34;

    private StarGuiModeStore.Mode mode = StarGuiModeStore.get();
    private ClientModule.Category selectedCategory = CincoClient.selectedCategory;
    private ClientModule selectedModule;
    private String search = "";
    private boolean searchFocused;
    private int nebulaScroll;

    private String draggingPanel;
    private String resizingPanel;
    private double dragOffX;
    private double dragOffY;
    private double resizeStartX;
    private double resizeStartY;
    private int resizeStartW;
    private int resizeStartH;

    private ClientModule draggingSlider;
    private int draggingSetting = -1;
    private int sliderX;
    private int sliderW;

    private final Map<String, Integer> orbitScroll = new HashMap<>();
    private final Map<String, StarAnimations.Track> hover = new HashMap<>();
    private final Map<String, StarAnimations.Track> toggle = new HashMap<>();

    private record OrbitPanel(String id, String title, String icon, int x, int y, int w, int h) {}

    public CincoClickGuiScreen() {
        super(Text.literal("Star Client"));
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        if (mode == StarGuiModeStore.Mode.ORBIT) updateOrbitMovement();
        else releaseOrbitMovement();
    }

    private void updateOrbitMovement() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.getWindow() == null || searchFocused) return;
        long window = c.getWindow().getHandle();
        c.options.forwardKey.setPressed(key(window, GLFW.GLFW_KEY_W));
        c.options.backKey.setPressed(key(window, GLFW.GLFW_KEY_S));
        c.options.leftKey.setPressed(key(window, GLFW.GLFW_KEY_A));
        c.options.rightKey.setPressed(key(window, GLFW.GLFW_KEY_D));
        c.options.jumpKey.setPressed(key(window, GLFW.GLFW_KEY_SPACE));
        c.options.sneakKey.setPressed(key(window, GLFW.GLFW_KEY_LEFT_SHIFT) || key(window, GLFW.GLFW_KEY_RIGHT_SHIFT));
        c.options.sprintKey.setPressed(key(window, GLFW.GLFW_KEY_LEFT_CONTROL) || key(window, GLFW.GLFW_KEY_RIGHT_CONTROL));
    }

    private boolean key(long window, int code) {
        return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }

    private void releaseOrbitMovement() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null) return;
        c.options.forwardKey.setPressed(false);
        c.options.backKey.setPressed(false);
        c.options.leftKey.setPressed(false);
        c.options.rightKey.setPressed(false);
        c.options.jumpKey.setPressed(false);
        c.options.sneakKey.setPressed(false);
        c.options.sprintKey.setPressed(false);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        drawBackdrop(c);
        if (mode == StarGuiModeStore.Mode.ORBIT) renderOrbit(c, mx, my);
        else renderNebula(c, mx, my);
        renderModeSelector(c, mx, my);
    }

    private void drawBackdrop(DrawContext c) {
        // Keep the world visible under Orbit like the reference; Nebula gets a slightly darker veil.
        c.fill(0, 0, width, height, mode == StarGuiModeStore.Mode.ORBIT ? 0x72040710 : 0xB9040711);
        long now = System.currentTimeMillis();
        int stars = Math.min(54, Math.max(18, StarPerformance.guiStarCap(MinecraftClient.getInstance())));
        for (int i = 0; i < stars; i++) {
            int x = Math.floorMod(41 + i * 137, Math.max(1, width));
            int y = Math.floorMod(17 + i * 79, Math.max(1, height));
            int alpha = 30 + (int)((Math.sin(now / 900.0 + i * 0.73) + 1.0) * 26.0);
            int rgb = (i % 4 == 0 ? StarCrispUi.PURPLE_HI : StarCrispUi.BLUE_HI) & 0x00FFFFFF;
            c.fill(x, y, x + 1, y + 1, (alpha << 24) | rgb);
        }
    }

    // ------------------------------------------------------------
    // ORBIT HUD
    // ------------------------------------------------------------

    private void renderOrbit(DrawContext c, int mx, int my) {
        for (OrbitPanel panel : orbitPanels()) renderOrbitPanel(c, panel, mx, my);
        c.drawText(textRenderer, StarFonts.text("ORBIT HUD   drag headers  •  resize corners  •  RMB settings  •  WASD movement", false),
                12, height - 16, 0x9CB2BDD8, false);
    }

    private List<OrbitPanel> orbitPanels() {
        int gap = 10;
        int x0 = 20;
        int y0 = 22;
        int w = 176;
        List<OrbitPanel> out = new ArrayList<>();
        out.add(new OrbitPanel("render", "Render", "◉", x0, y0, w, 218));
        out.add(new OrbitPanel("combat", "Combat", "⚔", x0 + w + gap, y0 + 8, w, 224));
        out.add(new OrbitPanel("search", "Search", "⌕", x0 + (w + gap) * 2, y0 + 3, 184, 116));
        out.add(new OrbitPanel("visuals", "Visuals", "◈", x0 + (w + gap) * 3, y0 + 12, w, 224));
        out.add(new OrbitPanel("keys", "Keybinds", "⌨", x0, y0 + 238, 186, 142));
        out.add(new OrbitPanel("session", "Session Info", "▥", x0 + 196, y0 + 246, 180, 142));
        out.add(new OrbitPanel("hud", "HUD Elements", "▦", x0 + 386, y0 + 150, 176, 206));
        out.add(new OrbitPanel("movement", "Movement", "↗", x0 + 572, y0 + 248, 176, 180));
        out.add(new OrbitPanel("base", "Base Finding", "⌖", x0 + 758, y0 + 236, 176, 194));
        out.add(new OrbitPanel("macro", "Macro", "M", x0 + 572, y0 + 32, 176, 184));
        out.add(new OrbitPanel("misc", "Misc", "⋯", x0 + 758, y0 + 20, 176, 188));
        out.add(new OrbitPanel("active", "Active Modules", "✦", 20, Math.max(396, height - 138), 320, 98));
        out.add(new OrbitPanel("coords", "Coordinates", "⌖", 352, Math.max(396, height - 132), 242, 92));
        return out;
    }

    private void renderOrbitPanel(DrawContext c, OrbitPanel p, int mx, int my) {
        OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
        int header = 28;
        int renderH = s.collapsed ? header : s.h;
        s.x = clamp(s.x, 2, Math.max(2, width - s.w - 2));
        s.y = clamp(s.y, 2, Math.max(2, height - renderH - 2));
        boolean focused = inside(mx, my, s.x, s.y, s.w, renderH);

        StarCrispUi.panel(c, s.x, s.y, s.w, renderH, 10, focused);
        c.fill(s.x + 1, s.y + header - 1, s.x + s.w - 1, s.y + header, 0x573B4B7B);

        // Compact concept-like header: icon, title, ellipsis, collapse arrow.
        c.drawText(textRenderer, StarFonts.text(p.icon(), true), s.x + 9, s.y + 10, StarCrispUi.BLUE_HI, false);
        c.drawText(textRenderer, StarFonts.text(p.title(), true), s.x + 27, s.y + 9, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, Text.literal("⋮"), s.x + s.w - 27, s.y + 8, StarCrispUi.DIM, false);
        c.drawText(textRenderer, Text.literal(s.collapsed ? "⌄" : "⌃"), s.x + s.w - 14, s.y + 9, StarCrispUi.MUTED, false);

        if (s.collapsed) return;

        switch (p.id()) {
            case "search" -> renderOrbitSearch(c, s, mx, my);
            case "session" -> renderOrbitSession(c, s);
            case "keys" -> renderOrbitKeys(c, s);
            case "active" -> renderOrbitActive(c, s);
            case "coords" -> renderOrbitCoords(c, s);
            default -> renderOrbitModules(c, p.id(), s, mx, my);
        }

        // Minimal resize handle instead of a chunky corner control.
        c.fill(s.x + s.w - 10, s.y + s.h - 4, s.x + s.w - 3, s.y + s.h - 3, 0x965E7DCE);
        c.fill(s.x + s.w - 7, s.y + s.h - 7, s.x + s.w - 3, s.y + s.h - 6, 0xA57657FF);
    }

    private void renderOrbitSearch(DrawContext c, OrbitLayoutStore.State s, int mx, int my) {
        int x = s.x + 8;
        int y = s.y + 36;
        int w = s.w - 16;
        StarCrispUi.searchBox(c, textRenderer, x, y, w,
                search.isEmpty() ? "Search modules..." : search + (searchFocused ? " _" : ""), searchFocused);
        if (search.isBlank()) return;
        int yy = y + 34;
        int shown = 0;
        for (ClientModule m : CincoClient.MODULES) {
            if (!matches(m, search)) continue;
            if (yy + 21 > s.y + s.h - 7 || shown++ >= 3) break;
            drawOrbitModuleRow(c, m, x, yy, w, mx, my);
            yy += 22;
        }
    }

    private void renderOrbitModules(DrawContext c, String id, OrbitLayoutStore.State s, int mx, int my) {
        List<ClientModule> modules = orbitModules(id);
        int rowH = 22;
        int visible = Math.max(1, (s.h - 36) / rowH);
        int maxScroll = Math.max(0, modules.size() - visible);
        int scroll = clamp(orbitScroll.getOrDefault(id, 0), 0, maxScroll);
        orbitScroll.put(id, scroll);
        int y = s.y + 33;
        for (int i = scroll; i < modules.size() && i < scroll + visible; i++) {
            drawOrbitModuleRow(c, modules.get(i), s.x + 7, y, s.w - 14, mx, my);
            y += rowH;
        }
        if (maxScroll > 0) {
            String count = (scroll + 1) + "/" + (maxScroll + 1);
            c.drawText(textRenderer, Text.literal(count), s.x + s.w - textRenderer.getWidth(count) - 7, s.y + s.h - 11,
                    0x997180A6, false);
        }
    }

    private void drawOrbitModuleRow(DrawContext c, ClientModule m, int x, int y, int w, int mx, int my) {
        boolean hov = inside(mx, my, x, y, w, 20);
        float hv = track(hover, "orbit-hover:" + m.name, hov ? 1f : 0f, hov ? 105 : 155);
        float en = track(toggle, "orbit-toggle:" + m.name, m.enabled ? 1f : 0f, 155);

        // Reference look: enabled rows are a strong clean blue bar; disabled rows stay almost flat.
        int fill = m.enabled
                ? StarCrispUi.mix(0xDD285AD8, 0xEF3F7BFF, hv * 0.55f)
                : StarCrispUi.mix(0x17141B2C, 0x84192238, hv);
        CincoDraw.rounded(c, x, y, x + w, y + 20, 6, fill);
        if (m.enabled) c.fill(x + 1, y + 3, x + 2, y + 17, 0xB9A7C4FF);

        c.drawText(textRenderer, StarFonts.text(trim(m.name, 19), false), x + 7, y + 7,
                m.enabled ? 0xFFFFFFFF : StarCrispUi.mix(StarCrispUi.MUTED, StarCrispUi.TEXT, hv), false);
        StarCrispUi.toggle(c, x + w - 31, y + 3, m.enabled, en);
    }

    private List<ClientModule> orbitModules(String id) {
        List<ClientModule> list = new ArrayList<>();
        for (ClientModule m : CincoClient.MODULES) {
            boolean include = switch (id) {
                case "combat" -> m.category == ClientModule.Category.COMBAT;
                case "movement" -> m.category == ClientModule.Category.MOVEMENT;
                case "base" -> m.category == ClientModule.Category.WORLD;
                case "macro" -> m.category == ClientModule.Category.PRACTICE;
                case "misc" -> m.category == ClientModule.Category.UTILITY;
                case "hud" -> m.category == ClientModule.Category.RENDER && isHudish(m.name);
                case "render" -> m.category == ClientModule.Category.SETTINGS;
                case "visuals" -> m.category == ClientModule.Category.RENDER && !isHudish(m.name);
                default -> false;
            };
            if (include) list.add(m);
        }
        list.sort(Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    private boolean isHudish(String name) {
        return name.contains("HUD") || name.contains("Display") || name.equals("FPS Counter")
                || name.equals("Coordinates") || name.equals("Direction") || name.equals("Potion Status")
                || name.equals("Purple Stars") || name.equals("Compact HUD") || name.equals("Crosshair Guide")
                || name.equals("Spotify HUD") || name.equals("Region Map") || name.equals("Target HUD")
                || name.equals("Ping Display");
    }

    private void renderOrbitSession(DrawContext c, OrbitLayoutStore.State s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int ping = ping(mc);
        long sec = Math.max(0, (System.currentTimeMillis() - SESSION_START) / 1000);
        int y = s.y + 39;
        orbitValueLine(c, s, y, "◷  Play Time", String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60)); y += 19;
        orbitValueLine(c, s, y, "▥  FPS", Integer.toString(mc.getCurrentFps())); y += 19;
        orbitValueLine(c, s, y, "⌁  Ping", ping + " ms"); y += 19;
        orbitValueLine(c, s, y, "✦  Active", Integer.toString((int)CincoClient.MODULES.stream().filter(m -> m.enabled).count()));
    }

    private void orbitValueLine(DrawContext c, OrbitLayoutStore.State s, int y, String left, String right) {
        c.drawText(textRenderer, StarFonts.text(left, false), s.x + 10, y, StarCrispUi.MUTED, false);
        int tw = textRenderer.getWidth(right);
        c.drawText(textRenderer, StarFonts.text(right, true), s.x + s.w - 10 - tw, y, StarCrispUi.BLUE_HI, false);
    }

    private void renderOrbitCoords(DrawContext c, OrbitLayoutStore.State s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        int y = s.y + 40;
        String xyz = "X: " + (int)mc.player.getX() + "   Y: " + (int)mc.player.getY() + "   Z: " + (int)mc.player.getZ();
        c.drawText(textRenderer, StarFonts.text(xyz, false), s.x + 11, y, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, StarFonts.text("Facing  " + mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT), false),
                s.x + 11, y + 20, StarCrispUi.MUTED, false);
    }

    private void renderOrbitKeys(DrawContext c, OrbitLayoutStore.State s) {
        int y = s.y + 38;
        int shown = 0;
        for (ClientModule m : CincoClient.MODULES) {
            if (m.keyCode < 0) continue;
            if (shown++ >= 5 || y + 18 > s.y + s.h - 7) break;
            c.drawText(textRenderer, StarFonts.text(trim(m.name, 16), false), s.x + 10, y, StarCrispUi.MUTED, false);
            String key = ModuleSettingsScreen.keyName(m.keyCode);
            int kw = Math.max(20, textRenderer.getWidth(key) + 10);
            CincoDraw.rounded(c, s.x + s.w - kw - 9, y - 4, s.x + s.w - 9, y + 13, 5, 0xA5222C48);
            c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(key, true), s.x + s.w - kw / 2 - 9, y, StarCrispUi.TEXT);
            y += 19;
        }
    }

    private void renderOrbitActive(DrawContext c, OrbitLayoutStore.State s) {
        int startX = s.x + 10;
        int x = startX;
        int y = s.y + 38;
        for (ClientModule m : CincoClient.MODULES) {
            if (!m.enabled) continue;
            String name = trim(m.name, 12);
            int w = textRenderer.getWidth(name) + 16;
            if (x + w > s.x + s.w - 9) { x = startX; y += 23; }
            if (y + 19 > s.y + s.h - 6) break;
            CincoDraw.rounded(c, x, y, x + w, y + 19, 6, 0xE33971F0);
            c.drawText(textRenderer, StarFonts.text(name, false), x + 8, y + 6, 0xFFFFFFFF, false);
            x += w + 6;
        }
    }

    // ------------------------------------------------------------
    // NEBULA CLICKGUI
    // ------------------------------------------------------------

    private void renderNebula(DrawContext c, int mx, int my) {
        int w = Math.min(960, Math.max(760, width - 76));
        int h = Math.min(590, Math.max(450, height - 78));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int side = 174;
        int top = 70;

        StarCrispUi.panel(c, x, y, w, h, 14, true);
        CincoDraw.rounded(c, x + 7, y + 7, x + side, y + h - 7, 11, 0xF0080D1B);
        c.fill(x + side, y + 1, x + side + 1, y + h - 1, 0x5D455887);

        // Sidebar brand matches the concept: compact mark + two-line identity.
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("✦"), x + 31, y + 26, StarCrispUi.BLUE_HI);
        c.drawText(textRenderer, StarFonts.text("STAR CLIENT", true), x + 50, y + 18, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, StarFonts.text("CRISP NEBULA", false), x + 50, y + 34, StarCrispUi.DIM, false);
        c.drawText(textRenderer, StarFonts.text("MODULES", true), x + 18, y + 67, 0xFF68779F, false);

        int catY = y + 82;
        for (ClientModule.Category cat : ClientModule.Category.values()) {
            int rowY = catY + cat.ordinal() * 34;
            boolean active = cat == selectedCategory;
            boolean hov = inside(mx, my, x + 12, rowY, side - 24, 27);
            float a = track(hover, "neb-cat:" + cat.name(), active ? 1f : hov ? 0.45f : 0f, active ? 145 : 105);
            if (a > 0.01f) CincoDraw.rounded(c, x + 12, rowY, x + side - 12, rowY + 27, 7,
                    StarCrispUi.mix(0x00131A30, 0xC7392EB0, a));
            if (active) StarCrispUi.accentRail(c, x + 15, rowY + 7, 13);
            c.drawText(textRenderer, StarFonts.text(categoryIcon(cat), true), x + 25, rowY + 9,
                    active ? StarCrispUi.BLUE_HI : StarCrispUi.DIM, false);
            c.drawText(textRenderer, StarFonts.text(nebulaCategoryName(cat), active), x + 44, rowY + 9,
                    active ? StarCrispUi.TEXT : StarCrispUi.MUTED, false);
            String count = Integer.toString(categoryCount(cat));
            c.drawText(textRenderer, Text.literal(count), x + side - 21 - textRenderer.getWidth(count), rowY + 9,
                    active ? StarCrispUi.BLUE_HI : 0xFF627095, false);
        }

        StarCrispUi.divider(c, x + 18, y + h - 104, x + side - 18);
        c.drawText(textRenderer, StarFonts.text("GENERAL", true), x + 18, y + h - 88, 0xFF68779F, false);
        c.drawText(textRenderer, StarFonts.text("⚙  Settings", false), x + 20, y + h - 65, StarCrispUi.MUTED, false);
        c.drawText(textRenderer, StarFonts.text("⌨  Keybinds", false), x + 20, y + h - 42, StarCrispUi.MUTED, false);

        int contentX = x + side + 17;
        int contentRight = x + w - 17;
        c.drawText(textRenderer, StarFonts.text(nebulaCategoryName(selectedCategory) + " Modules", true), contentX, y + 20, StarCrispUi.TEXT, false);
        int enabled = enabledCount(selectedCategory);
        c.drawText(textRenderer, StarFonts.text(categoryCount(selectedCategory) + " modules  •  " + enabled + " enabled", false),
                contentX, y + 39, StarCrispUi.MUTED, false);

        int searchW = 225;
        int sx = contentRight - searchW;
        int sy = y + 17;
        StarCrispUi.searchBox(c, textRenderer, sx, sy, searchW,
                search.isEmpty() ? "Search modules..." : search + (searchFocused ? " _" : ""), searchFocused);

        int listY = y + top;
        int bodyH = h - top - 16;
        int listW = Math.min(264, Math.max(226, (contentRight - contentX) / 3));
        int detailX = contentX + listW + 12;
        int detailW = contentRight - detailX;

        StarCrispUi.panel(c, contentX, listY, listW, bodyH, 10, false);
        renderNebulaList(c, contentX, listY, listW, bodyH, mx, my);
        StarCrispUi.panel(c, detailX, listY, detailW, bodyH, 10, false);
        renderNebulaDetails(c, detailX, listY, detailW, bodyH, mx, my);
    }

    private void renderNebulaList(DrawContext c, int x, int y, int w, int h, int mx, int my) {
        List<ClientModule> modules = nebulaModules();
        int rowH = 48;
        int visible = Math.max(1, (h - 12) / rowH);
        int maxScroll = Math.max(0, modules.size() - visible);
        nebulaScroll = clamp(nebulaScroll, 0, maxScroll);
        int yy = y + 6;
        for (int i = nebulaScroll; i < modules.size() && i < nebulaScroll + visible; i++) {
            ClientModule m = modules.get(i);
            boolean selected = m == selectedModule;
            boolean hov = inside(mx, my, x + 6, yy, w - 12, 41);
            float hv = track(hover, "neb-row:" + m.name, (selected || hov) ? 1f : 0f, hov ? 105 : 150);
            StarCrispUi.card(c, x + 6, yy, w - 12, 41, selected, hov);
            if (selected) StarCrispUi.accentRail(c, x + 8, yy + 8, 25);

            c.drawText(textRenderer, StarFonts.text(trim(m.name, 22), true), x + 16, yy + 8,
                    selected ? 0xFFFFFFFF : StarCrispUi.mix(StarCrispUi.MUTED, StarCrispUi.TEXT, hv), false);
            c.drawText(textRenderer, StarFonts.text(trim(m.description, 28), false), x + 16, yy + 24,
                    selected ? 0xFFC6D1EC : StarCrispUi.DIM, false);
            float en = track(toggle, "neb-toggle:" + m.name, m.enabled ? 1f : 0f, 150);
            StarCrispUi.toggle(c, x + w - 39, yy + 7, m.enabled, en);
            yy += rowH;
        }
    }

    private void renderNebulaDetails(DrawContext c, int x, int y, int w, int h, int mx, int my) {
        ClientModule m = selectedModule;
        if (m == null || m.category != selectedCategory || (!search.isBlank() && !matches(m, search))) {
            List<ClientModule> list = nebulaModules();
            if (!list.isEmpty()) { m = list.get(0); selectedModule = m; }
        }
        if (m == null) {
            c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("No matching modules", true), x + w / 2, y + h / 2, StarCrispUi.MUTED);
            return;
        }

        c.drawText(textRenderer, StarFonts.text(m.name, true), x + 16, y + 15, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, StarFonts.text(trim(m.description, Math.max(30, (w - 86) / 6)), false),
                x + 16, y + 32, StarCrispUi.MUTED, false);
        float en = track(toggle, "neb-detail-toggle:" + m.name, m.enabled ? 1f : 0f, 150);
        StarCrispUi.toggle(c, x + w - 47, y + 14, m.enabled, en);

        // Segmented tabs from the concept. General is the live settings page in this build.
        int tabsY = y + 56;
        CincoDraw.rounded(c, x + 14, tabsY, x + w - 14, tabsY + 30, 7, 0xBA0D1427);
        String[] tabs = {"General", "Render", "Keybind"};
        int tabW = (w - 36) / 3;
        for (int i = 0; i < tabs.length; i++) {
            int bx = x + 18 + i * tabW;
            if (i == 0) CincoDraw.rounded(c, bx, tabsY + 4, bx + tabW - 5, tabsY + 26, 6, 0xD54234B2);
            c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(tabs[i], i == 0),
                    bx + (tabW - 5) / 2, tabsY + 10, i == 0 ? StarCrispUi.TEXT : StarCrispUi.MUTED);
        }

        int rowY = tabsY + 44;
        drawSettingRow(c, m, 0, x + 16, rowY, w - 32); rowY += 63;
        drawSettingRow(c, m, 1, x + 16, rowY, w - 32); rowY += 63;
        drawSettingRow(c, m, 2, x + 16, rowY, w - 32);

        int keyY = y + h - 43;
        StarCrispUi.divider(c, x + 16, keyY - 9, x + w - 16);
        c.drawText(textRenderer, StarFonts.text("KEYBIND", true), x + 16, keyY + 4, StarCrispUi.DIM, false);
        String key = ModuleSettingsScreen.keyName(m.keyCode);
        int kw = Math.max(62, textRenderer.getWidth(key) + 20);
        CincoDraw.bubble(c, x + w - kw - 16, keyY - 2, x + w - 16, keyY + 25, 7, 0x7A5367A1, 0xCA12192B);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(key, true), x + w - kw / 2 - 16, keyY + 7, StarCrispUi.BLUE_HI);
    }

    private void drawSettingRow(DrawContext c, ClientModule m, int which, int x, int y, int w) {
        String label = settingLabel(m, which);
        double value = settingValue(m, which);
        double min = settingMin(m, which);
        double max = settingMax(m, which);
        c.drawText(textRenderer, StarFonts.text(label, true), x, y, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, StarFonts.text("Adjust " + label.toLowerCase(Locale.ROOT), false), x, y + 15, StarCrispUi.DIM, false);
        String valueText = format(value);
        int boxW = Math.max(50, textRenderer.getWidth(valueText) + 18);
        int trackW = Math.max(42, w - boxW - 16);
        StarCrispUi.slider(c, x, y + 38, trackW, (value - min) / Math.max(0.000001, max - min));
        CincoDraw.bubble(c, x + w - boxW, y + 27, x + w, y + 51, 6, 0x72495D93, 0xCA12192B);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(valueText, false), x + w - boxW / 2, y + 35, StarCrispUi.MUTED);
    }

    // ------------------------------------------------------------
    // INPUT
    // ------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleModeSelector(mx, my, button)) return true;
        if (mode == StarGuiModeStore.Mode.ORBIT) return orbitClick(mx, my, button);
        return nebulaClick(mx, my, button);
    }

    private boolean orbitClick(double mx, double my, int button) {
        List<OrbitPanel> panels = orbitPanels();
        for (int i = panels.size() - 1; i >= 0; i--) {
            OrbitPanel p = panels.get(i);
            OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
            int renderH = s.collapsed ? 28 : s.h;
            if (!inside(mx, my, s.x, s.y, s.w, renderH)) continue;

            if (button == 0 && inside(mx, my, s.x + s.w - 18, s.y + 3, 17, 23)) {
                s.collapsed = !s.collapsed;
                OrbitLayoutStore.save();
                return true;
            }
            if (button == 0 && inside(mx, my, s.x + s.w - 34, s.y + 3, 15, 23)) {
                s.w = p.w(); s.h = p.h();
                OrbitLayoutStore.save();
                return true;
            }
            if (!s.collapsed && button == 0 && inside(mx, my, s.x + s.w - 14, s.y + s.h - 14, 14, 14)) {
                resizingPanel = p.id();
                resizeStartX = mx; resizeStartY = my;
                resizeStartW = s.w; resizeStartH = s.h;
                return true;
            }
            if (button == 0 && inside(mx, my, s.x, s.y, s.w - 36, 28)) {
                draggingPanel = p.id();
                dragOffX = mx - s.x; dragOffY = my - s.y;
                return true;
            }
            if (s.collapsed) return true;

            if (p.id().equals("search")) {
                if (button == 0 && inside(mx, my, s.x + 8, s.y + 36, s.w - 16, 28)) {
                    searchFocused = true;
                    return true;
                }
                if (!search.isBlank()) {
                    int yy = s.y + 70;
                    int shown = 0;
                    for (ClientModule m : CincoClient.MODULES) {
                        if (!matches(m, search)) continue;
                        if (yy + 20 > s.y + s.h - 7 || shown++ >= 3) break;
                        if (inside(mx, my, s.x + 8, yy, s.w - 16, 20)) {
                            if (button == 0) toggle(m);
                            else if (button == 1) MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));
                            return true;
                        }
                        yy += 22;
                    }
                }
                return true;
            }

            List<ClientModule> modules = orbitModules(p.id());
            if (!modules.isEmpty()) {
                int visible = Math.max(1, (s.h - 36) / 22);
                int scroll = clamp(orbitScroll.getOrDefault(p.id(), 0), 0, Math.max(0, modules.size() - visible));
                int yy = s.y + 33;
                for (int idx = scroll; idx < modules.size() && idx < scroll + visible; idx++) {
                    ClientModule m = modules.get(idx);
                    if (inside(mx, my, s.x + 7, yy, s.w - 14, 20)) {
                        if (button == 0) toggle(m);
                        else if (button == 1) MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));
                        return true;
                    }
                    yy += 22;
                }
            }
            return true;
        }
        searchFocused = false;
        return super.mouseClicked(mx, my, button);
    }

    private boolean nebulaClick(double mx, double my, int button) {
        int w = Math.min(960, Math.max(760, width - 76));
        int h = Math.min(590, Math.max(450, height - 78));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int side = 174;
        int top = 70;
        int contentX = x + side + 17;
        int contentRight = x + w - 17;

        int catY = y + 82;
        for (ClientModule.Category cat : ClientModule.Category.values()) {
            int rowY = catY + cat.ordinal() * 34;
            if (button == 0 && inside(mx, my, x + 12, rowY, side - 24, 27)) {
                selectedCategory = cat;
                CincoClient.selectedCategory = cat;
                selectedModule = null;
                nebulaScroll = 0;
                searchFocused = false;
                return true;
            }
        }

        int searchW = 225;
        int sx = contentRight - searchW;
        int sy = y + 17;
        if (button == 0 && inside(mx, my, sx, sy, searchW, 28)) {
            searchFocused = true;
            return true;
        }

        int listY = y + top;
        int bodyH = h - top - 16;
        int listW = Math.min(264, Math.max(226, (contentRight - contentX) / 3));
        int detailX = contentX + listW + 12;
        int detailW = contentRight - detailX;

        List<ClientModule> modules = nebulaModules();
        int visible = Math.max(1, (bodyH - 12) / 48);
        nebulaScroll = clamp(nebulaScroll, 0, Math.max(0, modules.size() - visible));
        int yy = listY + 6;
        for (int i = nebulaScroll; i < modules.size() && i < nebulaScroll + visible; i++) {
            ClientModule m = modules.get(i);
            if (inside(mx, my, contentX + 6, yy, listW - 12, 41)) {
                if (button == 0 && mx >= contentX + listW - 45) toggle(m);
                else if (button == 0) selectedModule = m;
                else if (button == 1) MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));
                searchFocused = false;
                return true;
            }
            yy += 48;
        }

        ClientModule m = selectedModule;
        if (m == null || m.category != selectedCategory) {
            if (!modules.isEmpty()) { m = modules.get(0); selectedModule = m; }
        }
        if (m != null) {
            if (button == 0 && inside(mx, my, detailX + detailW - 47, listY + 14, 28, 14)) {
                toggle(m); return true;
            }
            int rowY = listY + 56 + 44;
            for (int which = 0; which < 3; which++, rowY += 63) {
                int rowW = detailW - 32;
                double value = settingValue(m, which);
                double min = settingMin(m, which);
                double max = settingMax(m, which);
                String valueText = format(value);
                int boxW = Math.max(50, textRenderer.getWidth(valueText) + 18);
                int sw = Math.max(42, rowW - boxW - 16);
                int sx2 = detailX + 16;
                int sy2 = rowY + 38;
                if (button == 0 && inside(mx, my, sx2 - 4, sy2 - 8, sw + 8, 18)) {
                    draggingSlider = m;
                    draggingSetting = which;
                    sliderX = sx2;
                    sliderW = sw;
                    updateSlider(mx);
                    return true;
                }
            }
            int keyY = listY + bodyH - 43;
            int key = m.keyCode;
            String keyName = ModuleSettingsScreen.keyName(key);
            int kw = Math.max(62, textRenderer.getWidth(keyName) + 20);
            if (button == 0 && inside(mx, my, detailX + detailW - kw - 16, keyY - 2, kw, 27)) {
                MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));
                return true;
            }
        }

        searchFocused = false;
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int button, double dx, double dy) {
        if (mode == StarGuiModeStore.Mode.ORBIT) {
            if (draggingPanel != null) {
                OrbitPanel p = findOrbitPanel(draggingPanel);
                if (p != null) {
                    OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
                    s.x = (int)Math.round(mx - dragOffX);
                    s.y = (int)Math.round(my - dragOffY);
                }
                return true;
            }
            if (resizingPanel != null) {
                OrbitPanel p = findOrbitPanel(resizingPanel);
                if (p != null) {
                    OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
                    s.w = clamp(resizeStartW + (int)Math.round(mx - resizeStartX), 132, 360);
                    s.h = clamp(resizeStartH + (int)Math.round(my - resizeStartY), 64, 520);
                }
                return true;
            }
        } else if (draggingSlider != null) {
            updateSlider(mx);
            return true;
        }
        return super.mouseDragged(mx, my, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int button) {
        if (draggingPanel != null || resizingPanel != null) {
            draggingPanel = null;
            resizingPanel = null;
            OrbitLayoutStore.save();
            return true;
        }
        if (draggingSlider != null) {
            draggingSlider = null;
            draggingSetting = -1;
            ConfigStore.save();
            return true;
        }
        return super.mouseReleased(mx, my, button);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mode == StarGuiModeStore.Mode.ORBIT) {
            for (OrbitPanel p : orbitPanels()) {
                OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
                if (s.collapsed || !inside(mx, my, s.x, s.y, s.w, s.h)) continue;
                List<ClientModule> modules = orbitModules(p.id());
                if (modules.isEmpty()) return true;
                int visible = Math.max(1, (s.h - 36) / 22);
                int max = Math.max(0, modules.size() - visible);
                int value = orbitScroll.getOrDefault(p.id(), 0) + (vertical < 0 ? 1 : -1);
                orbitScroll.put(p.id(), clamp(value, 0, max));
                return true;
            }
        } else {
            List<ClientModule> modules = nebulaModules();
            int w = Math.min(960, Math.max(760, width - 76));
            int h = Math.min(590, Math.max(450, height - 78));
            int bodyH = h - 70 - 16;
            int visible = Math.max(1, (bodyH - 12) / 48);
            nebulaScroll = clamp(nebulaScroll + (vertical < 0 ? 1 : -1), 0, Math.max(0, modules.size() - visible));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (searchFocused && !Character.isISOControl(chr)) {
            search += chr;
            nebulaScroll = 0;
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchFocused) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !search.isEmpty()) {
                search = search.substring(0, search.length() - 1);
                nebulaScroll = 0;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER) {
                searchFocused = false;
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                if (!search.isEmpty()) { search = ""; return true; }
                searchFocused = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private void updateSlider(double mx) {
        if (draggingSlider == null) return;
        double f = clamp01((mx - sliderX) / Math.max(1.0, sliderW));
        if (draggingSetting == 0) draggingSlider.setSettingFromSlider(f);
        else if (draggingSetting == 1) draggingSlider.setSetting2FromSlider(f);
        else if (draggingSetting == 2) draggingSlider.setSetting3FromSlider(f);
        ConfigStore.save();
    }

    // ------------------------------------------------------------
    // MODE SWITCH / HELPERS
    // ------------------------------------------------------------

    private void renderModeSelector(DrawContext c, int mx, int my) {
        int x = (width - MODE_W) / 2;
        int y = height - 47;
        CincoDraw.bubble(c, x, y, x + MODE_W, y + MODE_H, 12, 0xAA5D7DE0, 0xEE080E1D);
        int half = MODE_W / 2;
        if (mode == StarGuiModeStore.Mode.ORBIT)
            CincoDraw.rounded(c, x + 4, y + 4, x + half - 2, y + MODE_H - 4, 9, 0xE73A73F2);
        else
            CincoDraw.rounded(c, x + half + 2, y + 4, x + MODE_W - 4, y + MODE_H - 4, 9, 0xE75038C3);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("▣  Orbit HUD", mode == StarGuiModeStore.Mode.ORBIT),
                x + half / 2, y + 12, mode == StarGuiModeStore.Mode.ORBIT ? 0xFFFFFFFF : StarCrispUi.MUTED);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("▤  Nebula ClickGUI", mode == StarGuiModeStore.Mode.NEBULA),
                x + half + half / 2, y + 12, mode == StarGuiModeStore.Mode.NEBULA ? 0xFFFFFFFF : StarCrispUi.MUTED);
    }

    private boolean handleModeSelector(double mx, double my, int button) {
        if (button != 0) return false;
        int x = (width - MODE_W) / 2;
        int y = height - 47;
        if (!inside(mx, my, x, y, MODE_W, MODE_H)) return false;
        StarGuiModeStore.Mode next = mx < x + MODE_W / 2 ? StarGuiModeStore.Mode.ORBIT : StarGuiModeStore.Mode.NEBULA;
        if (next != mode) {
            releaseOrbitMovement();
            mode = next;
            StarGuiModeStore.set(mode);
            searchFocused = false;
        }
        return true;
    }

    private List<ClientModule> nebulaModules() {
        List<ClientModule> list = new ArrayList<>();
        for (ClientModule m : CincoClient.MODULES) {
            if (m.category != selectedCategory) continue;
            if (!search.isBlank() && !matches(m, search)) continue;
            list.add(m);
        }
        list.sort(Comparator.comparing((ClientModule m) -> !m.enabled)
                .thenComparing(m -> m.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    private int categoryCount(ClientModule.Category category) {
        int n = 0;
        for (ClientModule m : CincoClient.MODULES) if (m.category == category) n++;
        return n;
    }

    private int enabledCount(ClientModule.Category category) {
        int n = 0;
        for (ClientModule m : CincoClient.MODULES) if (m.category == category && m.enabled) n++;
        return n;
    }

    private String nebulaCategoryName(ClientModule.Category cat) {
        return switch (cat) {
            case COMBAT -> "Combat";
            case MOVEMENT -> "Movement";
            case RENDER -> "Visuals";
            case WORLD -> "Base Finding";
            case UTILITY -> "Misc";
            case PRACTICE -> "Macro";
            case SETTINGS -> "Client";
        };
    }

    private String categoryIcon(ClientModule.Category cat) {
        return switch (cat) {
            case COMBAT -> "⚔";
            case MOVEMENT -> "↗";
            case RENDER -> "◉";
            case WORLD -> "⌖";
            case UTILITY -> "⋯";
            case PRACTICE -> "M";
            case SETTINGS -> "⚙";
        };
    }

    private String settingLabel(ClientModule m, int which) {
        String label = which == 0 ? m.settingLabel : which == 1 ? m.setting2Label : m.setting3Label;
        return label == null || label.isBlank() ? "Setting" : label;
    }

    private double settingValue(ClientModule m, int which) {
        return which == 0 ? m.setting : which == 1 ? m.setting2 : m.setting3;
    }

    private double settingMin(ClientModule m, int which) {
        return which == 0 ? m.min : which == 1 ? m.min2 : m.min3;
    }

    private double settingMax(ClientModule m, int which) {
        return which == 0 ? m.max : which == 1 ? m.max2 : m.max3;
    }

    private void toggle(ClientModule m) {
        m.enabled = !m.enabled;
        if (m.name.equals("Sprint Assist")) CincoClient.sprintAssistEnabled = m.enabled;
        if (m.name.equals("Client HUD")) CincoClient.hudEnabled = m.enabled;
        if (m.name.equals("Container Finder")) {
            ClientModule base = CincoClient.findModule("Base Finder");
            if (base != null) base.enabled = m.enabled;
        }
        if (m.name.equals("Base Finder")) {
            ClientModule containers = CincoClient.findModule("Container Finder");
            if (containers != null) containers.enabled = m.enabled;
            if (!m.enabled) BaseScanner.reset();
        }
        ConfigStore.save();
    }

    private int ping(MinecraftClient c) {
        if (c == null || c.player == null || c.getNetworkHandler() == null) return 0;
        var entry = c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());
        return entry == null ? 0 : entry.getLatency();
    }

    private OrbitPanel findOrbitPanel(String id) {
        for (OrbitPanel p : orbitPanels()) if (p.id().equals(id)) return p;
        return null;
    }

    private boolean matches(ClientModule m, String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        return m.name.toLowerCase(Locale.ROOT).contains(q)
                || m.description.toLowerCase(Locale.ROOT).contains(q);
    }

    private float track(Map<String, StarAnimations.Track> map, String key, float target, int duration) {
        StarAnimations.Track t = map.computeIfAbsent(key, k -> new StarAnimations.Track(target));
        return t.to(target, StarAnimations.duration(MinecraftClient.getInstance(), duration, false),
                StarAnimations.Curve.CUBIC_OUT, System.currentTimeMillis());
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String format(double v) {
        if (Math.abs(v - Math.round(v)) < 0.0001) return Integer.toString((int)Math.round(v));
        return String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    @Override
    public void close() {
        releaseOrbitMovement();
        OrbitLayoutStore.save();
        super.close();
    }
}
