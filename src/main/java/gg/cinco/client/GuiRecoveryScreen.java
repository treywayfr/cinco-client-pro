package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Crash-safe fallback when the full Right Shift GUI fails to construct. */
public final class GuiRecoveryScreen extends StarLegacyScreen {
    private final String reason;
    private int x,y,w,h;

    public GuiRecoveryScreen(String reason){
        super(Text.literal("Star Client Recovery"));
        this.reason = reason == null ? "Unknown GUI error" : reason;
    }

    @Override public boolean shouldPause(){ return false; }

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);
        StarBackground.render(c,width,height,delta);
        w=Math.min(620,width-40); h=Math.min(360,height-36); x=(width-w)/2; y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦  STAR CLIENT SAFE MODE",true),x+24,y+22,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("The full Right Shift GUI hit an error, so the game stayed open.",false),x+24,y+44,0xFFD6CFDC,false);
        c.drawText(textRenderer,StarFonts.text(trim(reason,74),false),x+24,y+61,0xFFFF8A9B,false);
        button(c,mx,my,x+24,y+96,w-48,42,"✦  Locker","Cosmetics • Skins • Accessories");
        button(c,mx,my,x+24,y+148,w-48,42,"⚙  Settings","Client preferences and profiles");
        button(c,mx,my,x+24,y+200,w-48,42,"⌨  Keybinds","View and toggle assigned keybinds");
        button(c,mx,my,x+24,y+252,w-48,42,"↻  Retry Full GUI","Try the normal Star ClickGUI again");
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("Esc closes this screen",false),x+w/2,y+h-24,0xFFAAA1B2);
    }

    private void button(DrawContext c,int mx,int my,int bx,int by,int bw,int bh,String title,String sub){
        boolean hov=inside(mx,my,bx,by,bw,bh);
        StarCrispUi.card(c,bx,by,bw,bh,false,hov);
        c.drawText(textRenderer,StarFonts.text(title,hov),bx+14,by+10,hov?StarCrispUi.PURPLE_HI:0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text(sub,false),bx+14,by+25,0xFFAFA6B7,false);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;
        MinecraftClient mc=MinecraftClient.getInstance();
        if(inside(mx,my,x+24,y+96,w-48,42)){mc.setScreen(new LockerScreen());return true;}
        if(inside(mx,my,x+24,y+148,w-48,42)){mc.setScreen(new ClientSettingsScreen());return true;}
        if(inside(mx,my,x+24,y+200,w-48,42)){mc.setScreen(new KeybindsScreen());return true;}
        if(inside(mx,my,x+24,y+252,w-48,42)){
            try{mc.setScreen(new CincoClickGuiScreen());}
            catch(Throwable t){mc.setScreen(new GuiRecoveryScreen(t.getClass().getSimpleName()+": "+safe(t.getMessage())));}
            return true;
        }
        return super.mouseClicked(mx,my,button);
    }

    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
    private String trim(String s,int max){if(s==null)return "";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private static String safe(String s){return s==null?"No message":s.replace('\n',' ').replace('\r',' ');}
}
