package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/** Nested black/purple module folder used by the Crystal and Mace pills. */
public final class PvpFolderScreen extends StarLegacyScreen {
    private static final int W=430,H=330;
    private final String folder;

    public PvpFolderScreen(String folder){super(Text.literal(folder+" PvP"));this.folder=folder;}
    private int left(){return(width-W)/2;} private int top(){return(height-H)/2;}

    private List<ClientModule> modules(){
        String[] names=folder.equals("Mace")
                ?new String[]{"Auto Mace","Breachswap","Lunge Macro","Auto Jump Reset","Shield Drain with Mace","Aim Assist","Silent Aim","Auto Sprint","Auto Clicker"}
                :new String[]{"Auto Crystal","Anchor Macro","Auto Safe Anchor","Auto Totem","Offhand Manager","Auto Regear","Crystal Practice Timer","Aim Assist","Silent Aim","Auto Clicker"};
        List<ClientModule> out=new ArrayList<>();
        for(String name:names){ClientModule m=CincoClient.findModule(name);if(m!=null)out.add(m);}return out;
    }

    @Override public void render(DrawContext c,int mx,int my,float delta){
        super.render(c,mx,my,delta);c.fill(0,0,width,height,0xF0020205);StarBackground.render(c,width,height,delta);
        int l=left(),t=top();CincoDraw.softShadow(c,l,t,l+W,t+H,16,0xFFA968FF,48);CincoDraw.bubble(c,l,t,l+W,t+H,15,0xFF6A3A91,0xF50A0810);
        c.drawText(textRenderer,StarFonts.text("★ "+folder.toUpperCase()+" PVP",true),l+18,t+16,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("left click toggle  •  right click settings",false),l+18,t+34,0xFFBDB2C9,false);
        int y=t+62;
        for(ClientModule m:modules()){
            boolean hov=mx>=l+16&&mx<=l+W-16&&my>=y&&my<=y+28;
            int border=m.enabled?0xFFD0A8FF:(hov?0xA8FFFFFF:0x4B6A5575);
            int fill=m.enabled?0xC9431D69:(hov?0xE315111B:0xD809080D);
            CincoDraw.bubble(c,l+16,y,l+W-16,y+28,8,border,fill);
            if(m.enabled)CincoDraw.rounded(c,l+19,y+5,l+23,y+23,2,0xFFA968FF);
            c.drawText(textRenderer,StarFonts.text(m.name,true),l+31,y+9,m.enabled?0xFFFFFFFF:0xFFE9E4ED,false);
            String state=m.enabled?"ON":"OFF";c.drawText(textRenderer,StarFonts.text(state,true),l+W-31-textRenderer.getWidth(state),y+9,m.enabled?0xFFC9A7FF:0xFF8E8797,false);
            y+=34;if(y>t+H-56)break;
        }
        CincoDraw.bubble(c,l+W-112,t+H-38,l+W-18,t+H-14,8,0xAFFFFFFF,0xFF241431);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("BACK",true),l+W-65,t+H-30,0xFFFFFFFF);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        int l=left(),t=top();int y=t+62;
        for(ClientModule m:modules()){
            if(mx>=l+16&&mx<=l+W-16&&my>=y&&my<=y+28){
                if(button==0){m.enabled=!m.enabled;if(m.name.equals("Auto Sprint"))CincoClient.sprintAssistEnabled=m.enabled;ConfigStore.save();}
                else if(button==1)MinecraftClient.getInstance().setScreen(new ModuleSettingsScreen(m));
                return true;
            }
            y+=34;if(y>t+H-56)break;
        }
        if(button==0&&mx>=l+W-112&&mx<=l+W-18&&my>=t+H-38&&my<=t+H-14){close();return true;}
        return super.mouseClicked(mx,my,button);
    }

    @Override public void close(){MinecraftClient.getInstance().setScreen(new CincoClickGuiScreen());}
    @Override public boolean shouldPause(){return false;}
}
