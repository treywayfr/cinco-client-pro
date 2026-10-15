package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class CincoClickGuiScreen extends Screen {
    private static final int W = 680, H = 430, SIDE = 132, ROW = 55;
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
        Star(double x, double y, double speed, double phase) { this.x=x; this.y=y; this.speed=speed; this.phase=phase; }
    }

    public CincoClickGuiScreen() {
        super(Text.literal("Cinco Client"));
        for (int i=0; i<150; i++) stars.add(new Star(i*73.37%1600, i*47.19%1000, .15+(i%7)*.055, (i%13)*.43));
    }
    private int left() { return (width-W)/2; }
    private int top() { return (height-H)/2; }

    @Override public void render(DrawContext c, int mx, int my, float delta) {
        long now = System.currentTimeMillis();
        c.fill(0,0,width,height,0xFF070610);
        drawStars(c, now, delta);
        int l=left(), t=top();
        double anim=Math.min(1.0,(now-openedAt)/240.0);
        int lift=(int)((1-anim)*13);
        int panelY=t+lift;
        c.fill(l-2,panelY-2,l+W+2,panelY+H+2,0xFF9B61FF);
        c.fill(l,panelY,l+W,panelY+H,0xF20C0915);
        c.fill(l,panelY,l+W,panelY+3,0xFFC59AFF);
        c.fill(l,panelY+3,l+SIDE,panelY+H,0xF20E0A19);
        c.fill(l+SIDE,panelY+3,l+SIDE+1,panelY+H,0xFF3B2851);
        c.drawTextWithShadow(textRenderer,Text.literal("✦ CINCO CLIENT"),l+16,panelY+14,0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer,Text.literal("v2.0  /  PURPLE NEBULA"),l+16,panelY+29,0xFFB991FF);
        c.drawTextWithShadow(textRenderer,Text.literal("CLIENT STATUS"),l+15,panelY+H-46,0xFF8F7AAE);
        int enabled=(int)CincoClient.MODULES.stream().filter(m->m.enabled).count();
        c.drawTextWithShadow(textRenderer,Text.literal(enabled+" modules enabled"),l+15,panelY+H-31,0xFFDCCBFF);
        int catY=panelY+57;
        for (ClientModule.Category cat:ClientModule.Category.values()) {
            int y=catY+cat.ordinal()*34;
            boolean selected=cat==CincoClient.selectedCategory;
            if(selected){c.fill(l+8,y-5,l+SIDE-8,y+22,0xFF39234F);c.fill(l+8,y-5,l+11,y+22,0xFFC59AFF);}
            c.drawTextWithShadow(textRenderer,Text.literal(cat.label),l+18,y+3,selected?0xFFFFFFFF:0xFFB3A6C9);
        }
        int x=l+SIDE+16;
        c.drawTextWithShadow(textRenderer,Text.literal(CincoClient.selectedCategory.label+" MODULES"),x,panelY+14,0xFFFFFFFF);
        c.drawTextWithShadow(textRenderer,Text.literal("Left-click: toggle    Drag: adjust setting"),x,panelY+28,0xFFB9A9D0);
        int searchX=l+W-213, searchY=panelY+11;
        c.fill(searchX,searchY,searchX+194,searchY+21,searchFocused?0xFF36234A:0xFF21182E);
        c.drawTextWithShadow(textRenderer,Text.literal((search.isEmpty()?"⌕ Search modules":search)+(searchFocused?"_":"")),searchX+7,searchY+6,0xFFD9C6FF);
        List<ClientModule> list=filteredModules();
        int viewportTop=panelY+47, viewportBottom=panelY+H-15;
        int visibleH=viewportBottom-viewportTop;
        int maxScroll=Math.max(0,list.size()*ROW-visibleH);
        scroll=Math.max(0,Math.min(scroll,maxScroll));
        c.enableScissor(x,viewportTop,l+W-13,viewportBottom);
        for(int i=0;i<list.size();i++){
            ClientModule m=list.get(i);int y=viewportTop+i*ROW-scroll;
            if(y+ROW<viewportTop||y>viewportBottom)continue;
            boolean hov=mx>=x&&mx<=l+W-13&&my>=y&&my<=y+ROW-4;
            c.fill(x,y,l+W-13,y+ROW-5,hov?0xE52D203F:0xC3171222);
            c.fill(x,y,x+2,y+ROW-5,m.enabled?0xFFC19AFF:0xFF493952);
            c.drawTextWithShadow(textRenderer,Text.literal(m.name),x+10,y+6,m.enabled?0xFFFFFFFF:0xFFD5C9E5);
            c.drawTextWithShadow(textRenderer,Text.literal(m.description),x+10,y+19,0xFF9F92B6);
            int sliderX=l+W-285, sliderW=78, sliderY=y+37;
            c.fill(sliderX,sliderY,sliderX+sliderW,sliderY+3,0xFF473854);
            double fraction=(m.setting-m.min)/Math.max(.0001,m.max-m.min);
            int knob=sliderX+(int)(Math.max(0,Math.min(1,fraction))*sliderW);
            c.fill(sliderX,sliderY,knob,sliderY+3,0xFFB58CFF);
            c.fill(knob-2,sliderY-2,knob+3,sliderY+5,0xFFF0E4FF);
            c.drawTextWithShadow(textRenderer,Text.literal(m.settingLabel+": "+fmt(m.setting)),sliderX+sliderW+8,y+33,0xFFD7C5FF);
            int tx=l+W-79;
            c.fill(tx,y+9,tx+57,y+25,m.enabled?0xFF8C50E2:0xFF44364E);
            c.drawCenteredTextWithShadow(textRenderer,Text.literal(m.enabled?"ON":"OFF"),tx+28,y+13,0xFFFFFFFF);
        }
        c.disableScissor();
        c.drawTextWithShadow(textRenderer,Text.literal("RIGHT SHIFT / ESC CLOSE  •  PURPLE STARS • CONFIG AUTO-SAVE"),l+SIDE+16,panelY+H-10,0xFF8F80A7);
        super.render(c,mx,my,delta);
    }

    private void drawStars(DrawContext c,long now,float delta){
        ClientModule density=CincoClient.findModule("Purple Stars");
        int amount=density==null?stars.size():(!density.enabled?0:(int)(stars.size()*density.setting/3.0));
        amount=Math.max(0,Math.min(stars.size(),amount));
        for(int i=0;i<amount;i++){
            Star s=stars.get(i);s.y+=s.speed*(.8+delta*.2);if(s.y>height)s.y=0;
            double tw=(Math.sin(now/370.0+s.phase)+1)/2.0;int a=65+(int)(tw*175);int size=(i%5==0)?2:1;
            int sx=(int)(s.x%Math.max(1,width)),sy=(int)s.y;
            c.fill(sx,sy,sx+size,sy+size,(a<<24)|0x00B58CFF);
            if(tw>.84)c.fill(sx-2,sy,sx+size+3,sy+1,0x777C4DFF);
        }
    }

    private List<ClientModule> filteredModules(){
        List<ClientModule> out=new ArrayList<>();String q=search.trim().toLowerCase(Locale.ROOT);
        for(ClientModule m:CincoClient.MODULES){if(m.category!=CincoClient.selectedCategory)continue;if(!q.isEmpty()&&!m.name.toLowerCase(Locale.ROOT).contains(q)&&!m.description.toLowerCase(Locale.ROOT).contains(q))continue;out.add(m);}return out;
    }
    private String fmt(double v){return Math.abs(v-Math.round(v))<.0001?Integer.toString((int)Math.round(v)):String.format(Locale.ROOT,"%.1f",v);}

    @Override public boolean mouseClicked(double mx,double my,int button){
        int l=left(),t=top();int searchX=l+W-213,searchY=t+11;
        if(button==0&&mx>=searchX&&mx<=searchX+194&&my>=searchY&&my<=searchY+21){searchFocused=true;return true;}
        if(button==0)searchFocused=false;
        if(button==0){
            for(ClientModule.Category cat:ClientModule.Category.values()){
                int y=t+57+cat.ordinal()*34;
                if(mx>=l+8&&mx<=l+SIDE-8&&my>=y-5&&my<=y+22){CincoClient.selectedCategory=cat;scroll=0;return true;}
            }
            int x=l+SIDE+16,viewportTop=t+47;
            List<ClientModule> list=filteredModules();
            for(int i=0;i<list.size();i++){
                ClientModule m=list.get(i);int y=viewportTop+i*ROW-scroll;
                if(my<y||my>y+ROW-5||mx<x||mx>l+W-13)continue;
                int sliderX=l+W-285,sliderY=y+34;
                if(mx>=sliderX-3&&mx<=sliderX+81&&my>=sliderY&&my<=y+ROW-3){draggingSlider=true;draggingModule=m;setFromMouse(m,mx,sliderX,78);return true;}
                m.enabled=!m.enabled;
                if(m.name.equals("Sprint Assist"))CincoClient.sprintAssistEnabled=m.enabled;
                if(m.name.equals("Client HUD"))CincoClient.hudEnabled=m.enabled;
                if(m.name.equals("Container Finder")){ClientModule base=CincoClient.findModule("Base Finder");if(base!=null)base.enabled=m.enabled;}
                if(m.name.equals("Base Finder")){ClientModule containers=CincoClient.findModule("Container Finder");if(containers!=null)containers.enabled=m.enabled;if(!m.enabled)BaseScanner.reset();}
                ConfigStore.save();return true;
            }
        }
        return super.mouseClicked(mx,my,button);
    }
    private void setFromMouse(ClientModule m,double mouseX,int sliderX,int sliderWidth){m.setSettingFromSlider((mouseX-sliderX)/(double)sliderWidth);ConfigStore.save();}
    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(draggingSlider&&draggingModule!=null){int sliderX=left()+W-285;setFromMouse(draggingModule,mx,sliderX,78);return true;}return super.mouseDragged(mx,my,button,dx,dy);}
    @Override public boolean mouseReleased(double mx,double my,int button){draggingSlider=false;draggingModule=null;return super.mouseReleased(mx,my,button);}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){scroll-=(int)(vertical*ROW*2);return true;}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&!Character.isISOControl(chr)){search+=chr;scroll=0;return true;}return super.charTyped(chr,modifiers);}
    @Override public boolean keyPressed(int key,int scan,int modifiers){
        if(searchFocused&&key==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty()){search=search.substring(0,search.length()-1);scroll=0;return true;}
        if(key==GLFW.GLFW_KEY_RIGHT_SHIFT||key==GLFW.GLFW_KEY_ESCAPE){if(searchFocused&&!search.isEmpty()){search="";searchFocused=false;return true;}close();return true;}
        if(key==GLFW.GLFW_KEY_SLASH){searchFocused=true;return true;}
        return super.keyPressed(key,scan,modifiers);
    }
    @Override public void close(){MinecraftClient.getInstance().setScreen(null);}
    @Override public boolean shouldPause(){return false;}
}
