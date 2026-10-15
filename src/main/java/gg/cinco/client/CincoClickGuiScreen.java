package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
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
 * Orbit HUD = compact floating draggable/resizable windows with movement passthrough.
 * Nebula ClickGUI = black/purple sidebar, module list and detailed settings workspace.
 */
public final class CincoClickGuiScreen extends StarLegacyScreen {
    private static final long SESSION_START = System.currentTimeMillis();
    private static final int MODE_W = 286;
    private static final int MODE_H = 32;

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
    public boolean shouldPause() { return false; }

    @Override
    public void tick() {
        super.tick();
        if (mode == StarGuiModeStore.Mode.ORBIT && !searchFocused) updateOrbitMovement();
        else releaseOrbitMovement();
    }

    private void updateOrbitMovement() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c.player == null || c.getWindow() == null) return;
        long window = c.getWindow().getHandle();
        c.options.forwardKey.setPressed(key(window, GLFW.GLFW_KEY_W));
        c.options.backKey.setPressed(key(window, GLFW.GLFW_KEY_S));
        c.options.leftKey.setPressed(key(window, GLFW.GLFW_KEY_A));
        c.options.rightKey.setPressed(key(window, GLFW.GLFW_KEY_D));
        c.options.jumpKey.setPressed(key(window, GLFW.GLFW_KEY_SPACE));
        c.options.sneakKey.setPressed(key(window, GLFW.GLFW_KEY_LEFT_SHIFT) || key(window, GLFW.GLFW_KEY_RIGHT_SHIFT));
        c.options.sprintKey.setPressed(key(window, GLFW.GLFW_KEY_LEFT_CONTROL) || key(window, GLFW.GLFW_KEY_RIGHT_CONTROL));
    }

    private boolean key(long window, int code) { return GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS; }

    private void releaseOrbitMovement() {
        MinecraftClient c = MinecraftClient.getInstance();
        if (c == null) return;
        c.options.forwardKey.setPressed(false); c.options.backKey.setPressed(false);
        c.options.leftKey.setPressed(false); c.options.rightKey.setPressed(false);
        c.options.jumpKey.setPressed(false); c.options.sneakKey.setPressed(false); c.options.sprintKey.setPressed(false);
    }

    @Override
    public void render(DrawContext c, int mx, int my, float delta) {
        super.render(c, mx, my, delta);
        drawBackdrop(c, delta);
        if (mode == StarGuiModeStore.Mode.ORBIT) renderOrbit(c, mx, my);
        else renderNebula(c, mx, my);
        renderModeSelector(c);
    }

    private void drawBackdrop(DrawContext c, float delta) {
        c.fill(0, 0, width, height, 0xFF020204);
        StarBackground.render(c, width, height, delta);
        int cx = width / 2, cy = height / 2;
        CincoDraw.rounded(c, cx - Math.min(430, width / 3), cy - 2, cx + Math.min(430, width / 3), cy + 2, 2, 0x182D1240);
    }

    private void renderOrbit(DrawContext c, int mx, int my) {
        for (OrbitPanel panel : orbitPanels()) renderOrbitPanel(c, panel, mx, my);
        c.drawText(textRenderer, StarFonts.text("ORBIT HUD   drag headers  •  resize corners  •  RMB settings  •  WASD movement", false), 12, height - 15, 0xCCFFFFFF, false);
    }

    private List<OrbitPanel> orbitPanels() {
        int gap = 8, x0 = 18, y0 = 20, w = 158;
        List<OrbitPanel> out = new ArrayList<>();
        out.add(new OrbitPanel("render", "Render", "◉", x0, y0, w, 190));
        out.add(new OrbitPanel("combat", "Combat", "⚔", x0 + w + gap, y0 + 7, w, 196));
        out.add(new OrbitPanel("search", "Search", "⌕", x0 + (w + gap) * 2, y0 + 2, 170, 106));
        out.add(new OrbitPanel("visuals", "Visuals", "◈", x0 + (w + gap) * 3 + 4, y0 + 10, w, 196));
        out.add(new OrbitPanel("macro", "Macro", "M", x0 + (w + gap) * 4 + 4, y0 + 24, w, 166));
        out.add(new OrbitPanel("misc", "Misc", "⋯", x0 + (w + gap) * 5 + 4, y0 + 14, w, 172));
        out.add(new OrbitPanel("keys", "Keybinds", "⌨", x0, y0 + 208, 172, 126));
        out.add(new OrbitPanel("session", "Session Info", "▥", x0 + 180, y0 + 214, 168, 126));
        out.add(new OrbitPanel("hud", "HUD Elements", "▦", x0 + 356, y0 + 132, 158, 184));
        out.add(new OrbitPanel("movement", "Movement", "↗", x0 + 522, y0 + 216, 158, 160));
        out.add(new OrbitPanel("base", "Base Finding", "⌖", x0 + 688, y0 + 210, 158, 170));
        out.add(new OrbitPanel("active", "Active Modules", "✦", 18, Math.max(350, height - 122), 304, 84));
        out.add(new OrbitPanel("coords", "Coordinates", "⌖", 334, Math.max(350, height - 116), 226, 78));
        return out;
    }

    private void renderOrbitPanel(DrawContext c, OrbitPanel p, int mx, int my) {
        OrbitLayoutStore.State s = OrbitLayoutStore.get(p.id(), p.x(), p.y(), p.w(), p.h());
        int header = 24, renderH = s.collapsed ? header : s.h;
        s.x = clamp(s.x, 2, Math.max(2, width - s.w - 2));
        s.y = clamp(s.y, 2, Math.max(2, height - renderH - 2));
        boolean selected = p.id().equals(draggingPanel) || p.id().equals(resizingPanel);
        StarCrispUi.panel(c, s.x, s.y, s.w, renderH, 9, selected);
        StarCrispUi.divider(c, s.x + 1, s.y + header - 1, s.x + s.w - 1);
        int stateColor = selected ? StarCrispUi.PURPLE_HI : StarCrispUi.TEXT;
        c.drawText(textRenderer, StarFonts.text(p.icon(), true), s.x + 8, s.y + 8, stateColor, false);
        c.drawText(textRenderer, StarFonts.text(p.title(), true), s.x + 24, s.y + 8, stateColor, false);
        c.drawText(textRenderer, Text.literal("⋮"), s.x + s.w - 25, s.y + 6, StarCrispUi.TEXT, false);
        c.drawText(textRenderer, Text.literal(s.collapsed ? "⌄" : "⌃"), s.x + s.w - 13, s.y + 7, s.collapsed ? StarCrispUi.PURPLE_HI : StarCrispUi.TEXT, false);
        if (s.collapsed) return;
        switch (p.id()) {
            case "search" -> renderOrbitSearch(c, s, mx, my);
            case "session" -> renderOrbitSession(c, s);
            case "keys" -> renderOrbitKeys(c, s);
            case "active" -> renderOrbitActive(c, s);
            case "coords" -> renderOrbitCoords(c, s);
            default -> renderOrbitModules(c, p.id(), s, mx, my);
        }
        c.fill(s.x + s.w - 9, s.y + s.h - 3, s.x + s.w - 3, s.y + s.h - 2, 0xC6FFFFFF);
        c.fill(s.x + s.w - 6, s.y + s.h - 6, s.x + s.w - 3, s.y + s.h - 5, 0xC6B58AFF);
    }

    private void renderOrbitSearch(DrawContext c, OrbitLayoutStore.State s, int mx, int my) {
        int x = s.x + 7, y = s.y + 31, w = s.w - 14;
        StarCrispUi.searchBox(c, textRenderer, x, y, w, search.isEmpty() ? "Search modules..." : search + (searchFocused ? " _" : ""), searchFocused);
        if (search.isBlank()) return;
        int yy = y + 31, shown = 0;
        for (ClientModule m : CincoClient.MODULES) {
            if (!matches(m, search)) continue;
            if (yy + 17 > s.y + s.h - 6 || shown++ >= 3) break;
            drawOrbitModuleRow(c, m, x, yy, w, mx, my); yy += 19;
        }
    }

    private void renderOrbitModules(DrawContext c, String id, OrbitLayoutStore.State s, int mx, int my) {
        List<ClientModule> modules = orbitModules(id);
        int rowPitch = 19, visible = Math.max(1, (s.h - 31) / rowPitch), maxScroll = Math.max(0, modules.size() - visible);
        int scroll = clamp(orbitScroll.getOrDefault(id, 0), 0, maxScroll); orbitScroll.put(id, scroll);
        int y = s.y + 29;
        for (int i = scroll; i < modules.size() && i < scroll + visible; i++) { drawOrbitModuleRow(c, modules.get(i), s.x + 6, y, s.w - 12, mx, my); y += rowPitch; }
        if (maxScroll > 0) { String count = (scroll + 1) + "/" + (maxScroll + 1); c.drawText(textRenderer, Text.literal(count), s.x + s.w - textRenderer.getWidth(count) - 6, s.y + s.h - 10, 0xBFFFFFFF, false); }
    }

    private void drawOrbitModuleRow(DrawContext c, ClientModule m, int x, int y, int w, int mx, int my) {
        boolean hov = inside(mx, my, x, y, w, 16); float en = track(toggle, "orbit-toggle:" + m.name, m.enabled ? 1f : 0f, 140);
        int fill = m.enabled ? 0xE77945E8 : (hov ? 0xD315111A : 0xB808080C); int border = m.enabled ? 0xE6A77BFF : (hov ? 0x8AFFFFFF : 0x384A3E50);
        CincoDraw.bubble(c, x, y, x + w, y + 16, 5, border, fill); if (m.enabled) StarCrispUi.accentRail(c, x + 1, y + 3, 10);
        c.drawText(textRenderer, StarFonts.text(trim(m.name, 18), false), x + 6, y + 5, m.enabled ? 0xFFFFFFFF : StarCrispUi.TEXT, false);
        StarCrispUi.miniToggle(c, x + w - 25, y + 3, m.enabled, en);
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
        list.sort(Comparator.comparing(m -> m.name.toLowerCase(Locale.ROOT))); return list;
    }

    private boolean isHudish(String name) {
        return name.contains("HUD") || name.contains("Display") || name.equals("FPS Counter") || name.equals("Coordinates") || name.equals("Direction") || name.equals("Potion Status") || name.equals("Purple Stars") || name.equals("Compact HUD") || name.equals("Crosshair Guide") || name.equals("Spotify HUD") || name.equals("Region Map") || name.equals("Target HUD") || name.equals("Ping Display");
    }

    private void renderOrbitSession(DrawContext c, OrbitLayoutStore.State s) {
        MinecraftClient mc = MinecraftClient.getInstance(); long sec = Math.max(0, (System.currentTimeMillis() - SESSION_START) / 1000); int y = s.y + 33;
        orbitValueLine(c, s, y, "◷  Play Time", String.format(Locale.ROOT, "%02d:%02d", sec / 60, sec % 60)); y += 18;
        orbitValueLine(c, s, y, "▥  FPS", Integer.toString(mc.getCurrentFps())); y += 18;
        orbitValueLine(c, s, y, "⌁  Ping", ping(mc) + " ms"); y += 18;
        orbitValueLine(c, s, y, "✦  Active", Integer.toString((int)CincoClient.MODULES.stream().filter(m -> m.enabled).count()));
    }
    private void orbitValueLine(DrawContext c, OrbitLayoutStore.State s, int y, String left, String right) { c.drawText(textRenderer, StarFonts.text(left, false), s.x + 9, y, StarCrispUi.TEXT, false); int tw = textRenderer.getWidth(right); c.drawText(textRenderer, StarFonts.text(right, true), s.x + s.w - 9 - tw, y, StarCrispUi.PURPLE_HI, false); }
    private void renderOrbitCoords(DrawContext c, OrbitLayoutStore.State s) { MinecraftClient mc=MinecraftClient.getInstance(); if(mc.player==null)return; int y=s.y+34;String xyz="X: "+(int)mc.player.getX()+"   Y: "+(int)mc.player.getY()+"   Z: "+(int)mc.player.getZ();c.drawText(textRenderer,StarFonts.text(xyz,false),s.x+10,y,StarCrispUi.TEXT,false);c.drawText(textRenderer,StarFonts.text("Facing  "+mc.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT),false),s.x+10,y+18,StarCrispUi.TEXT,false); }

    private void renderOrbitKeys(DrawContext c, OrbitLayoutStore.State s) {
        int y=s.y+33,shown=0;for(ClientModule m:CincoClient.MODULES){if(m.keyCode<0)continue;if(shown++>=5||y+17>s.y+s.h-6)break;c.drawText(textRenderer,StarFonts.text(trim(m.name,15),false),s.x+9,y,StarCrispUi.TEXT,false);String key=ModuleSettingsScreen.keyName(m.keyCode);int kw=Math.max(18,textRenderer.getWidth(key)+8);CincoDraw.bubble(c,s.x+s.w-kw-8,y-3,s.x+s.w-8,y+12,5,0xBFFFFFFF,0xD608080D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(key,true),s.x+s.w-kw/2-8,y,StarCrispUi.TEXT);y+=18;}
    }
    private void renderOrbitActive(DrawContext c, OrbitLayoutStore.State s) { int startX=s.x+9,x=startX,y=s.y+33;for(ClientModule m:CincoClient.MODULES){if(!m.enabled)continue;String name=trim(m.name,12);int w=textRenderer.getWidth(name)+14;if(x+w>s.x+s.w-8){x=startX;y+=20;}if(y+17>s.y+s.h-5)break;CincoDraw.rounded(c,x,y,x+w,y+17,5,0xE67945E8);c.drawText(textRenderer,StarFonts.text(name,false),x+7,y+5,0xFFFFFFFF,false);x+=w+5;} }

    private void renderNebula(DrawContext c, int mx, int my) {
        int w=Math.min(950,Math.max(750,width-82)),h=Math.min(580,Math.max(440,height-84)),x=(width-w)/2,y=(height-h)/2,side=170,top=68;
        StarCrispUi.panel(c,x,y,w,h,13,false);CincoDraw.rounded(c,x+7,y+7,x+side,y+h-7,10,0xF6040408);StarCrispUi.divider(c,x+side,y+8,x+side+1);
        c.drawCenteredTextWithShadow(textRenderer,Text.literal("✦"),x+29,y+24,StarCrispUi.PURPLE_HI);c.drawText(textRenderer,StarFonts.text("STAR CLIENT",true),x+48,y+17,StarCrispUi.TEXT,false);c.drawText(textRenderer,StarFonts.text("Cinco",false),x+48,y+33,StarCrispUi.PURPLE_HI,false);c.drawText(textRenderer,StarFonts.text("MODULES",true),x+17,y+65,0xFFCFC7D5,false);
        int catY=y+80;for(ClientModule.Category cat:ClientModule.Category.values()){int rowY=catY+cat.ordinal()*33;boolean active=cat==selectedCategory,hov=inside(mx,my,x+11,rowY,side-22,26);int fill=active?0xD1261239:hov?0xB70C0A10:0;if(fill!=0)CincoDraw.rounded(c,x+11,rowY,x+side-11,rowY+26,7,fill);if(active)StarCrispUi.accentRail(c,x+14,rowY+6,14);int stateColor=active?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT;c.drawText(textRenderer,StarFonts.text(categoryIcon(cat),true),x+24,rowY+8,stateColor,false);c.drawText(textRenderer,StarFonts.text(nebulaCategoryName(cat),active),x+42,rowY+8,stateColor,false);String count=Integer.toString(categoryCount(cat));c.drawText(textRenderer,Text.literal(count),x+side-19-textRenderer.getWidth(count),rowY+8,stateColor,false);}
        StarCrispUi.divider(c,x+17,y+h-100,x+side-17);c.drawText(textRenderer,StarFonts.text("GENERAL",true),x+17,y+h-84,0xFFCFC7D5,false);c.drawText(textRenderer,StarFonts.text("⚙  Settings",false),x+19,y+h-61,StarCrispUi.TEXT,false);c.drawText(textRenderer,StarFonts.text("⌨  Keybinds",false),x+19,y+h-39,StarCrispUi.TEXT,false);
        int contentX=x+side+16,contentRight=x+w-16;c.drawText(textRenderer,StarFonts.text(nebulaCategoryName(selectedCategory)+" Modules",true),contentX,y+19,StarCrispUi.TEXT,false);c.drawText(textRenderer,StarFonts.text(categoryCount(selectedCategory)+" modules  •  "+enabledCount(selectedCategory)+" enabled",false),contentX,y+38,StarCrispUi.TEXT,false);
        int searchW=220,searchX=contentRight-searchW,searchY=y+16;StarCrispUi.searchBox(c,textRenderer,searchX,searchY,searchW,search.isEmpty()?"Search modules...":search+(searchFocused?" _":""),searchFocused);
        int listY=y+top,bodyH=h-top-15,listW=Math.min(254,Math.max(218,(contentRight-contentX)/3)),detailX=contentX+listW+11,detailW=contentRight-detailX;
        StarCrispUi.panel(c,contentX,listY,listW,bodyH,9,false);renderNebulaList(c,contentX,listY,listW,bodyH,mx,my);StarCrispUi.panel(c,detailX,listY,detailW,bodyH,9,false);renderNebulaDetails(c,detailX,listY,detailW,bodyH);
    }

    private void renderNebulaList(DrawContext c,int x,int y,int w,int h,int mx,int my){List<ClientModule> modules=nebulaModules();int rowPitch=44,visible=Math.max(1,(h-10)/rowPitch),maxScroll=Math.max(0,modules.size()-visible);nebulaScroll=clamp(nebulaScroll,0,maxScroll);int yy=y+5;for(int i=nebulaScroll;i<modules.size()&&i<nebulaScroll+visible;i++){ClientModule m=modules.get(i);boolean selected=m==selectedModule,hov=inside(mx,my,x+5,yy,w-10,37);StarCrispUi.card(c,x+5,yy,w-10,37,selected,hov);if(selected)StarCrispUi.accentRail(c,x+7,yy+7,23);int nameColor=selected?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT;c.drawText(textRenderer,StarFonts.text(trim(m.name,21),true),x+14,yy+7,nameColor,false);c.drawText(textRenderer,StarFonts.text(trim(m.description,27),false),x+14,yy+22,selected?0xFFE6D7F2:0xFFD2CBD8,false);float en=track(toggle,"neb-toggle:"+m.name,m.enabled?1f:0f,140);StarCrispUi.toggle(c,x+w-34,yy+6,m.enabled,en);yy+=rowPitch;}}

    private void renderNebulaDetails(DrawContext c,int x,int y,int w,int h){ClientModule m=selectedModule;if(m==null||m.category!=selectedCategory||(!search.isBlank()&&!matches(m,search))){List<ClientModule> list=nebulaModules();if(!list.isEmpty()){m=list.get(0);selectedModule=m;}}if(m==null){c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("No matching modules",true),x+w/2,y+h/2,StarCrispUi.TEXT);return;}c.drawText(textRenderer,StarFonts.text(m.name,true),x+15,y+14,StarCrispUi.PURPLE_HI,false);c.drawText(textRenderer,StarFonts.text(trim(m.description,Math.max(28,(w-84)/6)),false),x+15,y+31,StarCrispUi.TEXT,false);float en=track(toggle,"neb-detail-toggle:"+m.name,m.enabled?1f:0f,140);StarCrispUi.toggle(c,x+w-43,y+13,m.enabled,en);int tabsY=y+54;CincoDraw.rounded(c,x+13,tabsY,x+w-13,tabsY+28,7,0xDC08080D);String[] tabs={"General","Render","Keybind"};int tabW=(w-34)/3;for(int i=0;i<tabs.length;i++){int bx=x+17+i*tabW;if(i==0)CincoDraw.rounded(c,bx,tabsY+4,bx+tabW-5,tabsY+24,6,0xBE32184F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(tabs[i],i==0),bx+(tabW-5)/2,tabsY+9,i==0?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT);}int rowY=tabsY+42;drawSettingRow(c,m,0,x+15,rowY,w-30);rowY+=61;drawSettingRow(c,m,1,x+15,rowY,w-30);rowY+=61;drawSettingRow(c,m,2,x+15,rowY,w-30);int keyY=y+h-41;StarCrispUi.divider(c,x+15,keyY-8,x+w-15);c.drawText(textRenderer,StarFonts.text("KEYBIND",true),x+15,keyY+4,StarCrispUi.TEXT,false);String key=ModuleSettingsScreen.keyName(m.keyCode);int kw=Math.max(58,textRenderer.getWidth(key)+18);CincoDraw.bubble(c,x+w-kw-15,keyY-2,x+w-15,keyY+24,6,0xBFFFFFFF,0xE308080D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(key,true),x+w-kw/2-15,keyY+7,StarCrispUi.TEXT);}

    private void drawSettingRow(DrawContext c,ClientModule m,int which,int x,int y,int w){String label=settingLabel(m,which);double value=settingValue(m,which),min=settingMin(m,which),max=settingMax(m,which);c.drawText(textRenderer,StarFonts.text(label,true),x,y,StarCrispUi.TEXT,false);c.drawText(textRenderer,StarFonts.text("Adjust "+label.toLowerCase(Locale.ROOT),false),x,y+14,0xFFCFC8D5,false);String valueText=format(value);int boxW=Math.max(48,textRenderer.getWidth(valueText)+16),trackW=Math.max(40,w-boxW-14);StarCrispUi.slider(c,x,y+36,trackW,(value-min)/Math.max(0.000001,max-min));CincoDraw.bubble(c,x+w-boxW,y+25,x+w,y+49,6,0x98FFFFFF,0xE308080D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(valueText,false),x+w-boxW/2,y+33,StarCrispUi.TEXT);}

    @Override
    public boolean mouseClicked(double mx,double my,int button){if(handleModeSelector(mx,my,button))return true;if(mode==StarGuiModeStore.Mode.ORBIT)return orbitClick(mx,my,button);return nebulaClick(mx,my,button);}

    private boolean orbitClick(double mx,double my,int button){List<OrbitPanel> panels=orbitPanels();for(int i=panels.size()-1;i>=0;i--){OrbitPanel p=panels.get(i);OrbitLayoutStore.State s=OrbitLayoutStore.get(p.id(),p.x(),p.y(),p.w(),p.h());int renderH=s.collapsed?24:s.h;if(!inside(mx,my,s.x,s.y,s.w,renderH))continue;if(button==0&&inside(mx,my,s.x+s.w-17,s.y+2,16,20)){s.collapsed=!s.collapsed;OrbitLayoutStore.save();return true;}if(button==0&&inside(mx,my,s.x+s.w-31,s.y+2,13,20)){s.w=p.w();s.h=p.h();OrbitLayoutStore.save();return true;}if(!s.collapsed&&button==0&&inside(mx,my,s.x+s.w-13,s.y+s.h-13,13,13)){resizingPanel=p.id();resizeStartX=mx;resizeStartY=my;resizeStartW=s.w;resizeStartH=s.h;return true;}if(button==0&&inside(mx,my,s.x,s.y,s.w-32,24)){draggingPanel=p.id();dragOffX=mx-s.x;dragOffY=my-s.y;return true;}if(s.collapsed)return true;if(p.id().equals("search")){if(button==0&&inside(mx,my,s.x+7,s.y+31,s.w-14,26)){searchFocused=true;releaseOrbitMovement();return true;}if(!search.isBlank()){int yy=s.y+62,shown=0;for(ClientModule m:CincoClient.MODULES){if(!matches(m,search))continue;if(yy+16>s.y+s.h-6||shown++>=3)break;if(inside(mx,my,s.x+7,yy,s.w-14,16)){if(button==0)toggle(m);else if(button==1)MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));return true;}yy+=19;}}return true;}List<ClientModule> modules=orbitModules(p.id());if(!modules.isEmpty()){int visible=Math.max(1,(s.h-31)/19),scroll=clamp(orbitScroll.getOrDefault(p.id(),0),0,Math.max(0,modules.size()-visible)),yy=s.y+29;for(int idx=scroll;idx<modules.size()&&idx<scroll+visible;idx++){ClientModule m=modules.get(idx);if(inside(mx,my,s.x+6,yy,s.w-12,16)){if(button==0)toggle(m);else if(button==1)MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));return true;}yy+=19;}}return true;}searchFocused=false;return super.mouseClicked(mx,my,button);}

    private boolean nebulaClick(double mx,double my,int button){int w=Math.min(950,Math.max(750,width-82)),h=Math.min(580,Math.max(440,height-84)),x=(width-w)/2,y=(height-h)/2,side=170,top=68,contentX=x+side+16,contentRight=x+w-16;int catY=y+80;for(ClientModule.Category cat:ClientModule.Category.values()){int rowY=catY+cat.ordinal()*33;if(button==0&&inside(mx,my,x+11,rowY,side-22,26)){selectedCategory=cat;CincoClient.selectedCategory=cat;selectedModule=null;nebulaScroll=0;searchFocused=false;return true;}}int searchW=220,searchX=contentRight-searchW,searchY=y+16;if(button==0&&inside(mx,my,searchX,searchY,searchW,26)){searchFocused=true;return true;}int listY=y+top,bodyH=h-top-15,listW=Math.min(254,Math.max(218,(contentRight-contentX)/3)),detailX=contentX+listW+11,detailW=contentRight-detailX;List<ClientModule> modules=nebulaModules();int visible=Math.max(1,(bodyH-10)/44);nebulaScroll=clamp(nebulaScroll,0,Math.max(0,modules.size()-visible));int yy=listY+5;for(int i=nebulaScroll;i<modules.size()&&i<nebulaScroll+visible;i++){ClientModule m=modules.get(i);if(inside(mx,my,contentX+5,yy,listW-10,37)){if(button==0&&mx>=contentX+listW-40)toggle(m);else if(button==0)selectedModule=m;else if(button==1)MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));searchFocused=false;return true;}yy+=44;}ClientModule m=selectedModule;if(m==null||m.category!=selectedCategory){if(!modules.isEmpty()){m=modules.get(0);selectedModule=m;}}if(m!=null){if(button==0&&inside(mx,my,detailX+detailW-43,listY+13,24,12)){toggle(m);return true;}int rowY=listY+54+42;for(int which=0;which<3;which++,rowY+=61){int rowW=detailW-30;double value=settingValue(m,which),min=settingMin(m,which),max=settingMax(m,which);String valueText=format(value);int boxW=Math.max(48,textRenderer.getWidth(valueText)+16),sw=Math.max(40,rowW-boxW-14),sx=detailX+15,sy=rowY+36;if(button==0&&inside(mx,my,sx-4,sy-8,sw+8,18)){draggingSlider=m;draggingSetting=which;sliderX=sx;sliderW=sw;updateSlider(mx);return true;}}int keyY=listY+bodyH-41;String keyName=ModuleSettingsScreen.keyName(m.keyCode);int kw=Math.max(58,textRenderer.getWidth(keyName)+18);if(button==0&&inside(mx,my,detailX+detailW-kw-15,keyY-2,kw,26)){MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));return true;}}searchFocused=false;return super.mouseClicked(mx,my,button);}

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(mode==StarGuiModeStore.Mode.ORBIT){if(draggingPanel!=null){OrbitPanel p=findOrbitPanel(draggingPanel);if(p!=null){OrbitLayoutStore.State s=OrbitLayoutStore.get(p.id(),p.x(),p.y(),p.w(),p.h());s.x=(int)Math.round(mx-dragOffX);s.y=(int)Math.round(my-dragOffY);}return true;}if(resizingPanel!=null){OrbitPanel p=findOrbitPanel(resizingPanel);if(p!=null){OrbitLayoutStore.State s=OrbitLayoutStore.get(p.id(),p.x(),p.y(),p.w(),p.h());s.w=clamp(resizeStartW+(int)Math.round(mx-resizeStartX),126,340);s.h=clamp(resizeStartH+(int)Math.round(my-resizeStartY),54,500);}return true;}}else if(draggingSlider!=null){updateSlider(mx);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){if(draggingPanel!=null||resizingPanel!=null){draggingPanel=null;resizingPanel=null;OrbitLayoutStore.save();return true;}if(draggingSlider!=null){draggingSlider=null;draggingSetting=-1;ConfigStore.save();return true;}return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){if(mode==StarGuiModeStore.Mode.ORBIT){for(OrbitPanel p:orbitPanels()){OrbitLayoutStore.State s=OrbitLayoutStore.get(p.id(),p.x(),p.y(),p.w(),p.h());if(s.collapsed||!inside(mx,my,s.x,s.y,s.w,s.h))continue;List<ClientModule> modules=orbitModules(p.id());if(modules.isEmpty())return true;int visible=Math.max(1,(s.h-31)/19),max=Math.max(0,modules.size()-visible),value=orbitScroll.getOrDefault(p.id(),0)+(vertical<0?1:-1);orbitScroll.put(p.id(),clamp(value,0,max));return true;}}else{List<ClientModule> modules=nebulaModules();int h=Math.min(580,Math.max(440,height-84)),bodyH=h-68-15,visible=Math.max(1,(bodyH-10)/44);nebulaScroll=clamp(nebulaScroll+(vertical<0?1:-1),0,Math.max(0,modules.size()-visible));return true;}return super.mouseScrolled(mx,my,horizontal,vertical);}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&!Character.isISOControl(chr)){search+=chr;nebulaScroll=0;return true;}return super.charTyped(chr,modifiers);}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(searchFocused){if(keyCode==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);nebulaScroll=0;return true;}if(keyCode==GLFW.GLFW_KEY_ENTER){searchFocused=false;return true;}if(keyCode==GLFW.GLFW_KEY_ESCAPE){if(!search.isEmpty()){search="";return true;}searchFocused=false;return true;}}return super.keyPressed(keyCode,scanCode,modifiers);}

    private void updateSlider(double mx){if(draggingSlider==null)return;double f=clamp01((mx-sliderX)/Math.max(1.0,sliderW));if(draggingSetting==0)draggingSlider.setSettingFromSlider(f);else if(draggingSetting==1)draggingSlider.setSetting2FromSlider(f);else if(draggingSetting==2)draggingSlider.setSetting3FromSlider(f);ConfigStore.save();}

    private void renderModeSelector(DrawContext c){int x=(width-MODE_W)/2,y=height-45;CincoDraw.bubble(c,x,y,x+MODE_W,y+MODE_H,11,0xAFFFFFFF,0xF208080D);int half=MODE_W/2;if(mode==StarGuiModeStore.Mode.ORBIT)CincoDraw.rounded(c,x+4,y+4,x+half-2,y+MODE_H-4,8,0xD632184F);else CincoDraw.rounded(c,x+half+2,y+4,x+MODE_W-4,y+MODE_H-4,8,0xD632184F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("▣  Orbit HUD",mode==StarGuiModeStore.Mode.ORBIT),x+half/2,y+11,mode==StarGuiModeStore.Mode.ORBIT?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("▤  Nebula ClickGUI",mode==StarGuiModeStore.Mode.NEBULA),x+half+half/2,y+11,mode==StarGuiModeStore.Mode.NEBULA?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT);}
    private boolean handleModeSelector(double mx,double my,int button){if(button!=0)return false;int x=(width-MODE_W)/2,y=height-45;if(!inside(mx,my,x,y,MODE_W,MODE_H))return false;StarGuiModeStore.Mode next=mx<x+MODE_W/2?StarGuiModeStore.Mode.ORBIT:StarGuiModeStore.Mode.NEBULA;if(next!=mode){releaseOrbitMovement();mode=next;StarGuiModeStore.set(mode);searchFocused=false;}return true;}
    private List<ClientModule> nebulaModules(){List<ClientModule> list=new ArrayList<>();for(ClientModule m:CincoClient.MODULES){if(m.category!=selectedCategory)continue;if(!search.isBlank()&&!matches(m,search))continue;list.add(m);}list.sort(Comparator.comparing((ClientModule m)->!m.enabled).thenComparing(m->m.name.toLowerCase(Locale.ROOT)));return list;}
    private int categoryCount(ClientModule.Category category){int n=0;for(ClientModule m:CincoClient.MODULES)if(m.category==category)n++;return n;}
    private int enabledCount(ClientModule.Category category){int n=0;for(ClientModule m:CincoClient.MODULES)if(m.category==category&&m.enabled)n++;return n;}
    private String nebulaCategoryName(ClientModule.Category cat){return switch(cat){case COMBAT->"Combat";case MOVEMENT->"Movement";case RENDER->"Visuals";case WORLD->"Base Finding";case UTILITY->"Misc";case PRACTICE->"Macro";case SETTINGS->"Client";};}
    private String categoryIcon(ClientModule.Category cat){return switch(cat){case COMBAT->"⚔";case MOVEMENT->"↗";case RENDER->"◉";case WORLD->"⌖";case UTILITY->"⋯";case PRACTICE->"M";case SETTINGS->"⚙";};}
    private String settingLabel(ClientModule m,int which){String label=which==0?m.settingLabel:which==1?m.setting2Label:m.setting3Label;return label==null||label.isBlank()?"Setting":label;}
    private double settingValue(ClientModule m,int which){return which==0?m.setting:which==1?m.setting2:m.setting3;}
    private double settingMin(ClientModule m,int which){return which==0?m.min:which==1?m.min2:m.min3;}
    private double settingMax(ClientModule m,int which){return which==0?m.max:which==1?m.max2:m.max3;}
    private void toggle(ClientModule m){m.enabled=!m.enabled;if(m.name.equals("Sprint Assist"))CincoClient.sprintAssistEnabled=m.enabled;if(m.name.equals("Client HUD"))CincoClient.hudEnabled=m.enabled;if(m.name.equals("Container Finder")){ClientModule base=CincoClient.findModule("Base Finder");if(base!=null)base.enabled=m.enabled;}if(m.name.equals("Base Finder")){ClientModule containers=CincoClient.findModule("Container Finder");if(containers!=null)containers.enabled=m.enabled;if(!m.enabled)BaseScanner.reset();}ConfigStore.save();}
    private int ping(MinecraftClient c){if(c==null||c.player==null||c.getNetworkHandler()==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private OrbitPanel findOrbitPanel(String id){for(OrbitPanel p:orbitPanels())if(p.id().equals(id))return p;return null;}
    private boolean matches(ClientModule m,String query){String q=query.toLowerCase(Locale.ROOT).trim();return m.name.toLowerCase(Locale.ROOT).contains(q)||m.description.toLowerCase(Locale.ROOT).contains(q);}
    private float track(Map<String,StarAnimations.Track> map,String key,float target,int duration){StarAnimations.Track t=map.computeIfAbsent(key,k->new StarAnimations.Track(target));return t.to(target,StarAnimations.duration(MinecraftClient.getInstance(),duration,false),StarAnimations.Curve.CUBIC_OUT,System.currentTimeMillis());}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
    private String trim(String s,int max){if(s==null)return "";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private String format(double v){if(Math.abs(v-Math.round(v))<0.0001)return Integer.toString((int)Math.round(v));return String.format(Locale.ROOT,"%.2f",v).replaceAll("0+$","").replaceAll("\\.$","");}
    private int clamp(int v,int min,int max){return Math.max(min,Math.min(max,v));}
    private double clamp01(double v){return Math.max(0.0,Math.min(1.0,v));}

    @Override public void close(){releaseOrbitMovement();OrbitLayoutStore.save();super.close();}
}
