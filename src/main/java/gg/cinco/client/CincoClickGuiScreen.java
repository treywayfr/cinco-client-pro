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
 * Star Client's two-mode interface.
 * ORBIT: independent draggable/resizable floating windows with movement pass-through.
 * NEBULA: a polished sidebar + module list + inline settings ClickGUI.
 */
public final class CincoClickGuiScreen extends Screen {
    private static final int BLUE = 0xFF4E6BFF;
    private static final int BLUE_HI = 0xFF6E8BFF;
    private static final int PURPLE = 0xFF7A5CFF;
    private static final int PURPLE_HI = 0xFF9B7BFF;
    private static final int PANEL = 0xE80A0F1E;
    private static final int PANEL_2 = 0xEA0E1426;
    private static final int BORDER = 0xB84E65C8;
    private static final int TEXT = 0xFFEAF0FF;
    private static final int MUTED = 0xFFAEB8D8;
    private static final int DIM = 0xFF647092;

    private StarGuiModeStore.Mode mode = StarGuiModeStore.get();
    private ClientModule.Category selectedCategory = CincoClient.selectedCategory;
    private ClientModule selectedModule;
    private String search = "";
    private boolean searchFocused;
    private int nebulaScroll;

    private String draggingPanel;
    private String resizingPanel;
    private double dragOffX, dragOffY;
    private int resizeStartW, resizeStartH;
    private double resizeStartX, resizeStartY;
    private final Map<String, Integer> orbitScroll = new HashMap<>();
    private final Map<String, StarAnimations.Track> hover = new HashMap<>();
    private final Map<String, StarAnimations.Track> toggle = new HashMap<>();

    private ClientModule draggingSlider;
    private int draggingSetting = -1;
    private int sliderX, sliderW;

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
        if (c.player == null || c.getWindow() == null) return;
        long w = c.getWindow().getHandle();
        c.options.forwardKey.setPressed(key(w, GLFW.GLFW_KEY_W));
        c.options.backKey.setPressed(key(w, GLFW.GLFW_KEY_S));
        c.options.leftKey.setPressed(key(w, GLFW.GLFW_KEY_A));
        c.options.rightKey.setPressed(key(w, GLFW.GLFW_KEY_D));
        c.options.jumpKey.setPressed(key(w, GLFW.GLFW_KEY_SPACE));
        c.options.sneakKey.setPressed(key(w, GLFW.GLFW_KEY_LEFT_SHIFT) || key(w, GLFW.GLFW_KEY_RIGHT_SHIFT));
        c.options.sprintKey.setPressed(key(w, GLFW.GLFW_KEY_LEFT_CONTROL) || key(w, GLFW.GLFW_KEY_RIGHT_CONTROL));
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
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);
        drawCosmicBackground(ctx);
        if (mode == StarGuiModeStore.Mode.ORBIT) renderOrbit(ctx, mx, my);
        else renderNebula(ctx, mx, my);
        renderModeSelector(ctx, mx, my);
    }

    private void drawCosmicBackground(DrawContext c) {
        c.fill(0, 0, width, height, 0xB8060914);
        long now = System.currentTimeMillis();
        int stars = Math.min(96, StarPerformance.guiStarCap(MinecraftClient.getInstance()) + 28);
        for (int i = 0; i < stars; i++) {
            int x = Math.floorMod(i * 149 + 47, Math.max(1, width));
            int y = Math.floorMod(i * 83 + 19, Math.max(1, height));
            int a = 38 + (int) ((Math.sin(now / 730.0 + i * 0.61) + 1.0) * 38.0);
            int rgb = (i % 3 == 0 ? PURPLE_HI : BLUE_HI) & 0x00FFFFFF;
            c.fill(x, y, x + (i % 11 == 0 ? 2 : 1), y + (i % 11 == 0 ? 2 : 1), (a << 24) | rgb);
        }
    }

    // ------------------------------------------------------------
    // ORBIT HUD
    // ------------------------------------------------------------

    private void renderOrbit(DrawContext c, int mx, int my) {
        List<OrbitPanel> panels = orbitPanels();
        for (OrbitPanel p : panels) renderOrbitPanel(c, p, mx, my);

        c.drawText(textRenderer, StarFonts.text("ORBIT HUD  •  drag headers  •  resize corners  •  RMB module settings  •  WASD moves", false),
                12, height - 18, 0xB9AEB8D8, false);
    }

    private List<OrbitPanel> orbitPanels() {
        int gap = 12;
        int pw = 184;
        int ph = 226;
        int x0 = 18;
        int y0 = 24;
        List<OrbitPanel> out = new ArrayList<>();
        out.add(new OrbitPanel("render", "Render", "◈", x0, y0, pw, ph));
        out.add(new OrbitPanel("combat", "Combat", "⚔", x0 + pw + gap, y0 + 10, pw, ph));
        out.add(new OrbitPanel("search", "Search", "⌕", x0 + (pw + gap) * 2, y0 + 4, 194, 126));
        out.add(new OrbitPanel("visuals", "Visuals", "◉", x0 + (pw + gap) * 3, y0 + 14, pw, ph));
        out.add(new OrbitPanel("movement", "Movement", "↗", x0, y0 + ph + 18, pw, 178));
        out.add(new OrbitPanel("base", "Base Finding", "⌖", x0 + pw + gap, y0 + ph + 28, pw, 194));
        out.add(new OrbitPanel("macro", "Macro", "M", x0 + (pw + gap) * 2, y0 + 144, 194, 188));
        out.add(new OrbitPanel("misc", "Misc", "⋯", x0 + (pw + gap) * 3, y0 + ph + 24, pw, 192));
        out.add(new OrbitPanel("active", "Active Modules", "★", Math.max(18, width - 380), Math.max(30, height - 180), 350, 116));
        out.add(new OrbitPanel("status", "Session Info", "▥", Math.max(18, width - 590), Math.max(30, height - 184), 194, 126));
        out.add(new OrbitPanel("keys", "Keybinds", "⌨", 18, Math.max(30, height - 190), 192, 132));
        return out;
    }

    private void renderOrbitPanel(DrawContext c, OrbitPanel panel, int mx, int my) {
        OrbitLayoutStore.State s = OrbitLayoutStore.get(panel.id(), panel.x(), panel.y(), panel.w(), panel.h());
        int headerH = 30;
        int renderH = s.collapsed ? headerH : s.h;
        s.x = clamp(s.x, 2, Math.max(2, width - s.w - 2));
        s.y = clamp(s.y, 2, Math.max(2, height - renderH - 2));

        boolean panelHover = inside(mx, my, s.x, s.y, s.w, renderH);
        if (StarPerformance.allowMenuShadow(MinecraftClient.getInstance()))
            CincoDraw.softShadow(c, s.x, s.y, s.x + s.w, s.y + renderH, 11, panelHover ? BLUE_HI : PURPLE, panelHover ? 58 : 32);
        CincoDraw.bubble(c, s.x, s.y, s.x + s.w, s.y + renderH, 12, panelHover ? BLUE_HI : BORDER, PANEL);
        c.fill(s.x + 1, s.y + headerH - 1, s.x + s.w - 1, s.y + headerH, 0x663D55A7);

        CincoDraw.rounded(c, s.x + 8, s.y + 7, s.x + 24, s.y + 23, 6, 0xB425376E);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal(panel.icon()), s.x + 16, s.y + 11, BLUE_HI);
        c.drawText(textRenderer, StarFonts.text(panel.title(), true), s.x + 30, s.y + 10, TEXT, false);
        c.drawText(textRenderer, Text.literal("⋮"), s.x + s.w - 30, s.y + 10, MUTED, false);
        c.drawText(textRenderer, Text.literal(s.collapsed ? "⌄" : "⌃"), s.x + s.w - 15, s.y + 10, BLUE_HI, false);

        if (s.collapsed) return;

        if (panel.id().equals("search")) {
            renderOrbitSearch(c, s, mx, my);
        } else if (panel.id().equals("status")) {
            renderOrbitStatus(c, s);
        } else if (panel.id().equals("active")) {
            renderOrbitActive(c, s);
        } else if (panel.id().equals("keys")) {
            renderOrbitKeys(c, s);
        } else {
            renderOrbitModules(c, panel.id(), s, mx, my);
        }

        // Resize grip.
        c.fill(s.x + s.w - 9, s.y + s.h - 3, s.x + s.w - 3, s.y + s.h - 2, 0xAA6E8BFF);
        c.fill(s.x + s.w - 6, s.y + s.h - 6, s.x + s.w - 3, s.y + s.h - 5, 0xAA9B7BFF);
    }

    private void renderOrbitSearch(DrawContext c, OrbitLayoutStore.State s, int mx, int my) {
        int x = s.x + 9, y = s.y + 39, w = s.w - 18;
        boolean focus = searchFocused;
        CincoDraw.bubble(c, x, y, x + w, y + 26, 8, focus ? BLUE_HI : 0xAA38446A, 0xC90B1122);
        c.drawText(textRenderer, StarFonts.text(search.isEmpty() ? "Search modules..." : search + (focus ? " _" : ""), false),
                x + 9, y + 9, search.isEmpty() ? DIM : TEXT, false);
        c.drawText(textRenderer, Text.literal("⌘K"), x + w - 27, y + 9, PURPLE_HI, false);

        if (!search.isBlank()) {
            int yy = y + 33;
            int shown = 0;
            for (ClientModule m : CincoClient.MODULES) {
                if (!matches(m, search)) continue;
                if (yy + 19 > s.y + s.h - 8 || shown++ >= 3) break;
                drawOrbitModuleRow(c, m, x, yy, w, mx, my, false);
                yy += 21;
            }
        }
    }

    private void renderOrbitModules(DrawContext c, String id, OrbitLayoutStore.State s, int mx, int my) {
        List<ClientModule> modules = orbitModules(id);
        int rowH = 23;
        int visible = Math.max(1, (s.h - 39) / rowH);
        int maxScroll = Math.max(0, modules.size() - visible);
        int scroll = clamp(orbitScroll.getOrDefault(id, 0), 0, maxScroll);
        orbitScroll.put(id, scroll);
        int y = s.y + 35;
        for (int i = scroll; i < modules.size() && i < scroll + visible; i++) {
            drawOrbitModuleRow(c, modules.get(i), s.x + 7, y, s.w - 14, mx, my, true);
            y += rowH;
        }
        if (maxScroll > 0) c.drawText(textRenderer, Text.literal((scroll + 1) + "/" + (maxScroll + 1)),
                s.x + 8, s.y + s.h - 12, DIM, false);
    }

    private void drawOrbitModuleRow(DrawContext c, ClientModule m, int x, int y, int w, int mx, int my, boolean glow) {
        boolean h = inside(mx, my, x, y, w, 20);
        float hv = track(hover, "orbit:" + m.name, h ? 1f : 0f, h ? 110 : 160);
        float en = track(toggle, "orbit-toggle:" + m.name, m.enabled ? 1f : 0f, 165);
        int fill = mix(0x6A11182B, 0xE23A58D8, Math.max(hv * 0.45f, en * 0.88f));
        CincoDraw.rounded(c, x, y, x + w, y + 20, 6, fill);
        if (m.enabled && glow) c.fill(x + 1, y + 3, x + 3, y + 17, BLUE_HI);
        c.drawText(textRenderer, StarFonts.text(trim(m.name, 20), false), x + 7, y + 7,
                mix(MUTED, TEXT, Math.max(hv, en)), false);

        int tx = x + w - 31;
        CincoDraw.rounded(c, tx, y + 4, tx + 25, y + 16, 7, mix(0xFF25304B, PURPLE, en));
        int knob = tx + 3 + Math.round(en * 12);
        CincoDraw.rounded(c, knob, y + 6, knob + 8, y + 14, 5, 0xFFF3F6FF);
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
                case "render" -> m.category == ClientModule.Category.SETTINGS || (m.category == ClientModule.Category.RENDER && isHudish(m.name));
                case "visuals" -> m.category == ClientModule.Category.RENDER && !isHudish(m.name);
                default -> false;
            };
            if (include) list.add(m);
        }
        list.sort(Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT)));
        return list;
    }

    private boolean isHudish(String name) {
        return name.contains("HUD") || name.contains("Display") || name.equals("Client HUD") || name.equals("FPS Counter")
                || name.equals("Coordinates") || name.equals("Direction") || name.equals("Potion Status")
                || name.equals("Purple Stars") || name.equals("Accent Glow") || name.equals("Compact HUD")
                || name.equals("Crosshair Guide") || name.equals("Hover Glow") || name.equals("Spotify HUD")
                || name.equals("Region Map") || name.equals("Hand View");
    }

    private void renderOrbitStatus(DrawContext c, OrbitLayoutStore.State s) {
        MinecraftClient mc = MinecraftClient.getInstance();
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());
            if (entry != null) ping = entry.getLatency();
        }
        long sec = System.currentTimeMillis() / 1000;
        int y = s.y + 40;
        line(c, s.x + 11, y, "◷  Play Time", String.format(Locale.ROOT, "%02d:%02d", (sec / 60) % 60, sec % 60)); y += 18;
        line(c, s.x + 11, y, "▥  FPS", Integer.toString(mc.getCurrentFps())); y += 18;
        line(c, s.x + 11, y, "⌁  Ping", ping + " ms"); y += 18;
        if (mc.player != null) line(c, s.x + 11, y, "⌖  XYZ", (int)mc.player.getX() + " " + (int)mc.player.getY() + " " + (int)mc.player.getZ());
    }

    private void line(DrawContext c, int x, int y, String left, String right) {
        c.drawText(textRenderer, StarFonts.text(left, false), x, y, MUTED, false);
        int tw = textRenderer.getWidth(right);
        c.drawText(textRenderer, StarFonts.text(right, true), x + 170 - tw, y, BLUE_HI, false);
    }

    private void renderOrbitActive(DrawContext c, OrbitLayoutStore.State s) {
        int x = s.x + 11, y = s.y + 40;
        int cx = x;
        for (ClientModule m : CincoClient.MODULES) {
            if (!m.enabled) continue;
            String name = trim(m.name, 13);
            int w = textRenderer.getWidth(name) + 16;
            if (cx + w > s.x + s.w - 10) { cx = x; y += 25; }
            if (y + 22 > s.y + s.h - 7) break;
            CincoDraw.rounded(c, cx, y, cx + w, y + 20, 7, 0xDF315FE5);
            c.drawText(textRenderer, StarFonts.text(name, false), cx + 8, y + 7, 0xFFFFFFFF, false);
            cx += w + 6;
        }
    }

    private void renderOrbitKeys(DrawContext c, OrbitLayoutStore.State s) {
        int y = s.y + 39;
        int shown = 0;
        for (ClientModule m : CincoClient.MODULES) {
            if (m.keyCode < 0 || shown++ >= 5) continue;
            c.drawText(textRenderer, StarFonts.text(trim(m.name, 18), false), s.x + 10, y, MUTED, false);
            String key = ModuleSettingsScreen.keyName(m.keyCode);
            int kw = Math.max(18, textRenderer.getWidth(key) + 10);
            CincoDraw.rounded(c, s.x + s.w - kw - 9, y - 4, s.x + s.w - 9, y + 13, 5, 0xB9273454);
            c.drawText(textRenderer, StarFonts.text(key, true), s.x + s.w - kw - 4, y, TEXT, false);
            y += 18;
        }
    }

    // ------------------------------------------------------------
    // NEBULA CLICKGUI
    // ------------------------------------------------------------

    private void renderNebula(DrawContext c, int mx, int my) {
        int w = Math.min(980, Math.max(720, width - 70));
        int h = Math.min(600, Math.max(430, height - 70));
        int x = (width - w) / 2;
        int y = (height - h) / 2;
        int side = 190;
        int top = 62;

        if (StarPerformance.allowMenuShadow(MinecraftClient.getInstance()))
            CincoDraw.softShadow(c, x, y, x + w, y + h, 18, PURPLE, 70);
        CincoDraw.bubble(c, x, y, x + w, y + h, 17, BLUE_HI, 0xF20A0F20);
        CincoDraw.rounded(c, x + 7, y + 7, x + side, y + h - 7, 13, 0xEE0A1020);

        // Brand.
        CincoDraw.rounded(c, x + 18, y + 16, x + 50, y + 48, 10, 0xD42B3D82);
        c.drawCenteredTextWithShadow(textRenderer, Text.literal("★"), x + 34, y + 27, PURPLE_HI);
        c.drawText(textRenderer, StarFonts.text("STAR CLIENT", true), x + 60, y + 20, TEXT, false);
        c.drawText(textRenderer, StarFonts.text("NEBULA CLICKGUI", false), x + 60, y + 35, DIM, false);

        int cy = y + 74;
        for (ClientModule.Category cat : ClientModule.Category.values()) {
            int rowY = cy + cat.ordinal() * 38;
            boolean active = cat == selectedCategory;
            float a = track(hover, "neb-cat:" + cat.name(), active ? 1f : 0f, 170);
            if (a > 0.01f) CincoDraw.rounded(c, x + 14, rowY, x + side - 12, rowY + 30, 8,
                    mix(0x00131A31, 0xD53C2FA3, a));
            String label = nebulaCategoryName(cat);
            c.drawText(textRenderer, StarFonts.text(categoryIcon(cat) + "  " + label, active), x + 26, rowY + 10,
                    mix(MUTED, TEXT, a), false);
            int count = 0;
            for (ClientModule m : CincoClient.MODULES) if (m.category == cat) count++;
            String ct = Integer.toString(count);
            c.drawText(textRenderer, Text.literal(ct), x + side - 28 - textRenderer.getWidth(ct), rowY + 10, active ? BLUE_HI : DIM, false);
        }

        c.fill(x + 22, y + h - 104, x + side - 20, y + h - 103, 0x553A4568);
        c.drawText(textRenderer, StarFonts.text("GENERAL", true), x + 24, y + h - 88, DIM, false);
        c.drawText(textRenderer, StarFonts.text("⚙  Settings", false), x + 25, y + h - 66, MUTED, false);
        c.drawText(textRenderer, StarFonts.text("⌨  Keybinds", false), x + 25, y + h - 43, MUTED, false);

        int contentX = x + side + 18;
        int contentRight = x + w - 18;
        c.drawText(textRenderer, StarFonts.text(nebulaCategoryName(selectedCategory) + " Modules", true), contentX, y + 21, TEXT, false);
        int enabled = 0, total = 0;
        for (ClientModule m : CincoClient.MODULES) if (m.category == selectedCategory) { total++; if (m.enabled) enabled++; }
        c.drawText(textRenderer, StarFonts.text(total + " modules  •  " + enabled + " enabled", false), contentX, y + 39, MUTED, false);

        int searchW = 235;
        int sx = contentRight - searchW, sy = y + 17;
        CincoDraw.bubble(c, sx, sy, contentRight, sy + 31, 9, searchFocused ? BLUE_HI : 0xAA384667, 0xB90B1122);
        c.drawText(textRenderer, StarFonts.text(search.isEmpty() ? "⌕  Search modules..." : "⌕  " + search + (searchFocused ? " _" : ""), false),
                sx + 10, sy + 11, search.isEmpty() ? DIM : TEXT, false);

        int listX = contentX;
        int listY = y + top;
        int listW = Math.min(278, Math.max(230, (contentRight - contentX) / 3));
        int detailX = listX + listW + 14;
        int detailW = contentRight - detailX;
        int bodyH = h - top - 18;

        CincoDraw.bubble(c, listX, listY, listX + listW, listY + bodyH, 12, 0x773A4B78, 0xC50C1222);
        renderNebulaList(c, listX, listY, listW, bodyH, mx, my);

        CincoDraw.bubble(c, detailX, listY, detailX + detailW, listY + bodyH, 12, 0x88425284, 0xD20D1326);
        renderNebulaDetails(c, detailX, listY, detailW, bodyH, mx, my);
    }

    private void renderNebulaList(DrawContext c, int x, int y, int w, int h, int mx, int my) {
        List<ClientModule> modules = nebulaModules();
        int rowH = 50;
        int visible = Math.max(1, (h - 12) / rowH);
        int maxScroll = Math.max(0, modules.size() - visible);
        nebulaScroll = clamp(nebulaScroll, 0, maxScroll);
        int yy = y + 7;
        for (int i = nebulaScroll; i < modules.size() && i < nebulaScroll + visible; i++) {
            ClientModule m = modules.get(i);
            boolean selected = m == selectedModule;
            boolean hov = inside(mx, my, x + 6, yy, w - 12, 43);
            float hv = track(hover, "neb-mod:" + m.name, (selected || hov) ? 1f : 0f, hov ? 110 : 170);
            CincoDraw.rounded(c, x + 6, yy, x + w - 6, yy + 43, 8,
                    mix(0x55151B30, selected ? 0xCF332A79 : 0xA3192549, hv));
            if (selected) c.fill(x + 7, yy + 7, x + 10, yy + 36, BLUE_HI);
            c.drawText(textRenderer, StarFonts.text(trim(m.name, 25), true), x + 16, yy + 9, mix(MUTED, TEXT, hv), false);
            c.drawText(textRenderer, StarFonts.text(trim(m.description, 34), false), x + 16, yy + 25, DIM, false);
            int tx = x + w - 39;
            float en = track(toggle, "neb-list-toggle:" + m.name, m.enabled ? 1f : 0f, 165);
            CincoDraw.rounded(c, tx, yy + 8, tx + 27, yy + 21, 7, mix(0xFF25304B, PURPLE, en));
            int knob = tx + 3 + Math.round(en * 13);
            CincoDraw.rounded(c, knob, yy + 10, knob + 8, yy + 18, 5, 0xFFF4F7FF);
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
            c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("No modules match this search", true), x + w / 2, y + h / 2, MUTED);
            return;
        }

        c.drawText(textRenderer, StarFonts.text(m.name, true), x + 18, y + 17, TEXT, false);
        c.drawText(textRenderer, StarFonts.text(trim(m.description, Math.max(34, (w - 92) / 6)), false), x + 18, y + 35, MUTED, false);
        float en = track(toggle, "neb-detail-toggle:" + m.name, m.enabled ? 1f : 0f, 165);
        int tx = x + w - 53;
        CincoDraw.rounded(c, tx, y + 15, tx + 36, y + 32, 9, mix(0xFF252E49, PURPLE, en));
        int knob = tx + 3 + Math.round(en * 18);
        CincoDraw.rounded(c, knob, y + 18, knob + 11, y + 29, 6, 0xFFF5F7FF);

        int tabsY = y + 59;
        CincoDraw.rounded(c, x + 14, tabsY, x + w - 14, tabsY + 31, 8, 0xA8121930);
        String[] tabs = {"General", "Render", "Keybind"};
        int tabW = (w - 36) / 3;
        for (int i = 0; i < tabs.length; i++) {
            int bx = x + 18 + i * tabW;
            if (i == 0) CincoDraw.rounded(c, bx, tabsY + 4, bx + tabW - 5, tabsY + 27, 7, 0xCC4937B8);
            c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(tabs[i], i == 0), bx + (tabW - 5) / 2, tabsY + 11, i == 0 ? TEXT : MUTED);
        }

        int rowY = tabsY + 48;
        drawSettingRow(c, m, 0, x + 18, rowY, w - 36, mx, my); rowY += 67;
        drawSettingRow(c, m, 1, x + 18, rowY, w - 36, mx, my); rowY += 67;
        drawSettingRow(c, m, 2, x + 18, rowY, w - 36, mx, my);

        int keyY = y + h - 48;
        c.drawText(textRenderer, StarFonts.text("KEYBIND", true), x + 18, keyY + 6, MUTED, false);
        String keyName = ModuleSettingsScreen.keyName(m.keyCode);
        int kw = Math.max(62, textRenderer.getWidth(keyName) + 22);
        CincoDraw.bubble(c, x + w - kw - 18, keyY, x + w - 18, keyY + 28, 8, 0x99415383, 0xC9141A2D);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(keyName, true), x + w - kw / 2 - 18, keyY + 10, BLUE_HI);
    }

    private void drawSettingRow(DrawContext c, ClientModule m, int which, int x, int y, int w, int mx, int my) {
        String label;
        double value, min, max;
        if (which == 0) { label = m.settingLabel; value = m.setting; min = m.min; max = m.max; }
        else if (which == 1) { label = m.setting2Label; value = m.setting2; min = m.min2; max = m.max2; }
        else { label = m.setting3Label; value = m.setting3; min = m.min3; max = m.max3; }
        c.drawText(textRenderer, StarFonts.text(label == null ? "Setting" : label, true), x, y, TEXT, false);
        String valueText = format(value);
        c.drawText(textRenderer, StarFonts.text(valueText, false), x + w - textRenderer.getWidth(valueText), y, BLUE_HI, false);
        int sy = y + 28;
        int sw = w - 76;
        CincoDraw.rounded(c, x, sy, x + sw, sy + 5, 3, 0xFF2A3453);
        double f = (value - min) / Math.max(0.000001, max - min);
        int fx = x + (int)Math.round(clamp01(f) * sw);
        if (fx > x) CincoDraw.rounded(c, x, sy, fx, sy + 5, 3, PURPLE);
        CincoDraw.rounded(c, fx - 4, sy - 4, fx + 5, sy + 9, 6, BLUE_HI);
        CincoDraw.bubble(c, x + sw + 13, sy - 10, x + w, sy + 15, 7, 0x7740547E, 0xB9141A2D);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text(valueText, false), x + sw + 13 + (w - sw - 13) / 2, sy - 2, MUTED);
    }

    private List<ClientModule> nebulaModules() {
        List<ClientModule> out = new ArrayList<>();
        for (ClientModule m : CincoClient.MODULES) {
            if (m.category != selectedCategory) continue;
            if (!search.isBlank() && !matches(m, search)) continue;
            out.add(m);
        }
        out.sort(Comparator.comparing((ClientModule m) -> !m.enabled).thenComparing(m -> m.name.toLowerCase(Locale.ROOT)));
        return out;
    }

    // ------------------------------------------------------------
    // INPUT
    // ------------------------------------------------------------

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        if (handleModeSelector(mx, my, button)) return true;
        if (mode == StarGuiModeStore.Mode.ORBIT) return orbitMouseClicked(mx, my, button);
        return nebulaMouseClicked(mx, my, button);
    }

    private boolean orbitMouseClicked(double mx, double my, int button) {
        List<OrbitPanel> panels = orbitPanels();
        for (int pi = panels.size() - 1; pi >= 0; pi--) {
            OrbitPanel p = panels.get(pi);
            OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
            int rh = s.collapsed ? 30 : s.h;
            if (!inside(mx, my, s.x, s.y, s.w, rh)) continue;

            if (button == 0 && mx >= s.x + s.w - 20 && my <= s.y + 29) {
                s.collapsed = !s.collapsed;
                OrbitLayoutStore.save();
                return true;
            }
            if (!s.collapsed && button == 0 && mx >= s.x + s.w - 14 && my >= s.y + s.h - 14) {
                resizingPanel = p.id();
                resizeStartW = s.w; resizeStartH = s.h;
                resizeStartX = mx; resizeStartY = my;
                return true;
            }
            if (button == 0 && my <= s.y + 30) {
                draggingPanel = p.id();
                dragOffX = mx - s.x; dragOffY = my - s.y;
                return true;
            }
            if (s.collapsed) return true;

            if (p.id().equals("search")) {
                if (button == 0 && inside(mx, my, s.x + 9, s.y + 39, s.w - 18, 26)) {
                    searchFocused = true;
                    return true;
                }
                if (!search.isBlank()) {
                    int yy = s.y + 72, shown = 0;
                    for (ClientModule m : CincoClient.MODULES) {
                        if (!matches(m, search)) continue;
                        if (yy + 19 > s.y + s.h - 8 || shown++ >= 3) break;
                        if (inside(mx, my, s.x + 9, yy, s.w - 18, 20)) return activateModuleClick(m, button);
                        yy += 21;
                    }
                }
                return true;
            }

            List<ClientModule> modules = orbitModules(p.id());
            if (!modules.isEmpty()) {
                int rowH = 23;
                int visible = Math.max(1, (s.h - 39) / rowH);
                int scroll = clamp(orbitScroll.getOrDefault(p.id(), 0), 0, Math.max(0, modules.size() - visible));
                int yy = s.y + 35;
                for (int i = scroll; i < modules.size() && i < scroll + visible; i++) {
                    if (inside(mx, my, s.x + 7, yy, s.w - 14, 20)) return activateModuleClick(modules.get(i), button);
                    yy += rowH;
                }
            }
            return true;
        }
        searchFocused = false;
        return super.mouseClicked(mx, my, button);
    }

    private boolean activateModuleClick(ClientModule m, int button) {
        if (button == 1) {
            MinecraftClient.getInstance().setScreen(m.name.equals("Storage ESP") ? new StorageEspSettingsScreen(m) : new ModuleSettingsScreen(m));
            return true;
        }
        if (button == 0) {
            toggle(m);
            return true;
        }
        return false;
    }

    private boolean nebulaMouseClicked(double mx, double my, int button) {
        int w = Math.min(980, Math.max(720, width - 70));
        int h = Math.min(600, Math.max(430, height - 70));
        int x = (width - w) / 2, y = (height - h) / 2, side = 190, top = 62;
        int contentX = x + side + 18, contentRight = x + w - 18;
        int searchW = 235, sx = contentRight - searchW, sy = y + 17;

        if (button == 0 && inside(mx, my, sx, sy, searchW, 31)) {
            searchFocused = true;
            return true;
        }
        if (button == 0) searchFocused = false;

        int cy = y + 74;
        for (ClientModule.Category cat : ClientModule.Category.values()) {
            int rowY = cy + cat.ordinal() * 38;
            if (button == 0 && inside(mx, my, x + 14, rowY, side - 26, 30)) {
                selectedCategory = cat;
                CincoClient.selectedCategory = cat;
                selectedModule = null;
                nebulaScroll = 0;
                return true;
            }
        }

        int listX = contentX, listY = y + top;
        int listW = Math.min(278, Math.max(230, (contentRight - contentX) / 3));
        int detailX = listX + listW + 14;
        int detailW = contentRight - detailX;
        int bodyH = h - top - 18;
        List<ClientModule> modules = nebulaModules();
        int rowH = 50, visible = Math.max(1, (bodyH - 12) / rowH);
        int yy = listY + 7;
        for (int i = nebulaScroll; i < modules.size() && i < nebulaScroll + visible; i++) {
            ClientModule m = modules.get(i);
            if (inside(mx, my, listX + 6, yy, listW - 12, 43)) {
                if (button == 1) {
                    MinecraftClient.getInstance().setScreen(m.name.equals("Storage ESP") ? new StorageEspSettingsScreen(m) : new ModuleSettingsScreen(m));
                    return true;
                }
                if (button == 0 && mx >= listX + listW - 45) toggle(m);
                else if (button == 0) selectedModule = m;
                return true;
            }
            yy += rowH;
        }

        ClientModule m = selectedModule;
        if (m != null) {
            int tx = detailX + detailW - 53;
            if (button == 0 && inside(mx, my, tx, listY + 15, 36, 17)) {
                toggle(m); return true;
            }
            int rowY = listY + 59 + 48;
            for (int which = 0; which < 3; which++, rowY += 67) {
                int sx2 = detailX + 18, sy2 = rowY + 28, sw = detailW - 36 - 76;
                if (button == 0 && inside(mx, my, sx2 - 4, sy2 - 8, sw + 8, 22)) {
                    draggingSlider = m;
                    draggingSetting = which;
                    sliderX = sx2; sliderW = sw;
                    updateSlider(mx);
                    return true;
                }
            }
        }
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
                int visible = Math.max(1, (s.h - 39) / 23);
                int max = Math.max(0, modules.size() - visible);
                int value = orbitScroll.getOrDefault(p.id(), 0) + (vertical < 0 ? 1 : -1);
                orbitScroll.put(p.id(), clamp(value, 0, max));
                return true;
            }
        } else {
            List<ClientModule> modules = nebulaModules();
            nebulaScroll = clamp(nebulaScroll + (vertical < 0 ? 1 : -1), 0, Math.max(0, modules.size() - 1));
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
            if (keyCode == GLFW.GLFW_KEY_ENTER) { searchFocused = false; return true; }
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

    private boolean handleModeSelector(double mx, double my, int button) {
        if (button != 0) return false;
        int totalW = 286, h = 34, x = (width - totalW) / 2, y = height - 48;
        if (!inside(mx, my, x, y, totalW, h)) return false;
        StarGuiModeStore.Mode next = mx < x + totalW / 2 ? StarGuiModeStore.Mode.ORBIT : StarGuiModeStore.Mode.NEBULA;
        if (next != mode) {
            releaseOrbitMovement();
            mode = next;
            StarGuiModeStore.set(mode);
            searchFocused = false;
        }
        return true;
    }

    private void renderModeSelector(DrawContext c, int mx, int my) {
        int totalW = 286, h = 34, x = (width - totalW) / 2, y = height - 48;
        CincoDraw.softShadow(c, x, y, x + totalW, y + h, 12, PURPLE, 50);
        CincoDraw.bubble(c, x, y, x + totalW, y + h, 13, BLUE_HI, 0xEC091020);
        int half = totalW / 2;
        if (mode == StarGuiModeStore.Mode.ORBIT)
            CincoDraw.rounded(c, x + 4, y + 4, x + half - 2, y + h - 4, 10, 0xE43666E8);
        else
            CincoDraw.rounded(c, x + half + 2, y + 4, x + totalW - 4, y + h - 4, 10, 0xE44C35B8);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("▣  Orbit HUD", mode == StarGuiModeStore.Mode.ORBIT), x + half / 2, y + 13,
                mode == StarGuiModeStore.Mode.ORBIT ? 0xFFFFFFFF : MUTED);
        c.drawCenteredTextWithShadow(textRenderer, StarFonts.text("▤  Nebula ClickGUI", mode == StarGuiModeStore.Mode.NEBULA), x + half + half / 2, y + 13,
                mode == StarGuiModeStore.Mode.NEBULA ? 0xFFFFFFFF : MUTED);
    }

    private void toggle(ClientModule module) {
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

    @Override
    public void close() {
        releaseOrbitMovement();
        OrbitLayoutStore.save();
        super.close();
    }

    // ------------------------------------------------------------
    // HELPERS
    // ------------------------------------------------------------

    private OrbitPanel findOrbitPanel(String id) {
        for (OrbitPanel p : orbitPanels()) if (p.id().equals(id)) return p;
        return null;
    }

    private float track(Map<String, StarAnimations.Track> map, String key, float target, int duration) {
        StarAnimations.Track t = map.computeIfAbsent(key, k -> new StarAnimations.Track(target));
        return t.to(target, StarAnimations.duration(MinecraftClient.getInstance(), duration, false), StarAnimations.Curve.CUBIC_OUT, System.currentTimeMillis());
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

    private boolean matches(ClientModule m, String query) {
        String q = query.toLowerCase(Locale.ROOT).trim();
        return m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q);
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private String trim(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, Math.max(0, max - 1)) + "…";
    }

    private String format(double v) {
        return Math.abs(v - Math.round(v)) < 0.0001 ? Integer.toString((int)Math.round(v)) : String.format(Locale.ROOT, "%.2f", v).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private int clamp(int v, int min, int max) { return Math.max(min, Math.min(max, v)); }
    private double clamp01(double v) { return Math.max(0.0, Math.min(1.0, v)); }

    private int mix(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int aa=(a>>>24)&255, ar=(a>>>16)&255, ag=(a>>>8)&255, ab=a&255;
        int ba=(b>>>24)&255, br=(b>>>16)&255, bg=(b>>>8)&255, bb=b&255;
        return ((int)(aa+(ba-aa)*t)<<24)|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);
    }
}
