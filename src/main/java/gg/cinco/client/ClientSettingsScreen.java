package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Functional client-wide settings page opened from the ClickGUI sidebar. */
public final class ClientSettingsScreen extends StarLegacyScreen {
    private static final String[] MODULES={"Performance Mode","GUI Sounds","Purple Stars","Hover Glow","Compact HUD","Config Auto-Save"};
    private int x,y,w,h;
    public ClientSettingsScreen(){super(Text.literal("Star Settings"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(720,width-44);h=Math.min(470,height-38);x=(width-w)/2;y=(height-h)/2;StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("⚙  SETTINGS",true),x+24,y+20,0xFFFFFFFF,false);c.drawText(textRenderer,StarFonts.text("Star Client preferences",false),x+24,y+39,StarCrispUi.PURPLE_HI,false);StarCrispUi.divider(c,x+20,y+62,x+w-20);
        int yy=y+82;for(String name:MODULES){ClientModule m=CincoClient.findModule(name);if(m==null)continue;boolean hov=inside(mx,my,x+24,yy,w-48,48);StarCrispUi.card(c,x+24,yy,w-48,48,m.enabled,hov);c.drawText(textRenderer,StarFonts.text(name,true),x+39,yy+12,m.enabled?0xFFFFFFFF:0xFFE2DAE8,false);c.drawText(textRenderer,StarFonts.text(m.description,false),x+39,yy+29,0xFFAAA2B0,false);StarCrispUi.toggle(c,x+w-80,yy+12,m.enabled,m.enabled?1f:0f);yy+=58;}
        ClientModule font=CincoClient.findModule("Font Style"),profile=CincoClient.findModule("Client Profile");int by=y+h-66;if(font!=null){CincoDraw.bubble(c,x+24,by,x+238,by+30,8,0x99FFFFFF,0xDD09070D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("FONT  "+StarFonts.name((int)Math.round(font.setting)),true),x+131,by+10,0xFFFFFFFF);}if(profile!=null){CincoDraw.bubble(c,x+252,by,x+492,by+30,8,0x99FFFFFF,0xDD09070D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("PROFILE  "+StarProfiles.name((int)Math.round(profile.setting)),true),x+372,by+10,0xFFFFFFFF);}
    }
    @Override public boolean mouseClicked(double mx,double my,int button){if(button!=0)return false;int yy=y+82;for(String name:MODULES){ClientModule m=CincoClient.findModule(name);if(m==null)continue;if(inside(mx,my,x+24,yy,w-48,48)){m.enabled=!m.enabled;ConfigStore.save();return true;}yy+=58;}int by=y+h-66;ClientModule font=CincoClient.findModule("Font Style");if(font!=null&&inside(mx,my,x+24,by,214,30)){font.adjustSetting();ConfigStore.save();return true;}ClientModule profile=CincoClient.findModule("Client Profile");if(profile!=null&&inside(mx,my,x+252,by,240,30)){profile.adjustSetting();StarProfiles.apply((int)Math.round(profile.setting));ConfigStore.save();return true;}return false;}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
