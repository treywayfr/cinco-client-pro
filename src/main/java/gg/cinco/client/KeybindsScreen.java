package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/** Shows every assigned keybind with a pill switch for its module and direct editing access. */
public final class KeybindsScreen extends StarLegacyScreen {
    private int x,y,w,h,scroll;
    public KeybindsScreen(){super(Text.literal("Star Keybinds"));}
    @Override public boolean shouldPause(){return false;}

    private List<ClientModule> bound(){List<ClientModule> out=new ArrayList<>();for(ClientModule m:CincoClient.MODULES)if(m.keyCode!=GLFW.GLFW_KEY_UNKNOWN&&m.keyCode>=0&&!hidden(m.name))out.add(m);return out;}
    private boolean hidden(String n){return n.equals("Custom Accessory")||n.equals("Animations")||n.equals("Player Changer")||n.equals("GTA Skin Changer")||n.equals("Crystal")||n.equals("Mace");}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);w=Math.min(700,width-44);h=Math.min(500,height-38);x=(width-w)/2;y=(height-h)/2;StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("⌨  KEYBINDS",true),x+24,y+20,0xFFFFFFFF,false);c.drawText(textRenderer,StarFonts.text("Every assigned Star Client shortcut",false),x+24,y+39,StarCrispUi.PURPLE_HI,false);StarCrispUi.divider(c,x+20,y+62,x+w-20);
        List<ClientModule> list=bound();int visible=Math.max(1,(h-100)/46),max=Math.max(0,list.size()-visible);scroll=Math.max(0,Math.min(max,scroll));int yy=y+78;
        for(int i=scroll;i<list.size()&&i<scroll+visible;i++){ClientModule m=list.get(i);boolean hov=inside(mx,my,x+24,yy,w-48,38);StarCrispUi.card(c,x+24,yy,w-48,38,m.enabled,hov);c.drawText(textRenderer,StarFonts.text(m.name,true),x+38,yy+10,m.enabled?0xFFFFFFFF:0xFFD2CBD8,false);String key=ModuleSettingsScreen.keyName(m.keyCode);int keyW=Math.max(58,textRenderer.getWidth(key)+22);CincoDraw.bubble(c,x+w-178-keyW,yy+6,x+w-178,yy+32,7,0xAFFFFFFF,0xD80A080F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(key,true),x+w-178-keyW/2,yy+14,0xFFFFFFFF);StarCrispUi.toggle(c,x+w-78,yy+8,m.enabled,m.enabled?1f:0f);yy+=46;}
        c.drawText(textRenderer,StarFonts.text("Click a row to edit its key • pill toggles the module",false),x+24,y+h-24,0xFFAFA6B7,false);
    }
    @Override public boolean mouseClicked(double mx,double my,int button){List<ClientModule> list=bound();int visible=Math.max(1,(h-100)/46),yy=y+78;for(int i=scroll;i<list.size()&&i<scroll+visible;i++){ClientModule m=list.get(i);if(inside(mx,my,x+24,yy,w-48,38)){if(mx>=x+w-115){m.enabled=!m.enabled;ConfigStore.save();}else MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));return true;}yy+=46;}return false;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){List<ClientModule> list=bound();int visible=Math.max(1,(h-100)/46);scroll=Math.max(0,Math.min(Math.max(0,list.size()-visible),scroll+(vertical<0?1:-1)));return true;}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
