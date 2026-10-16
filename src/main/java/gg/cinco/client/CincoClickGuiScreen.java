package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Star Client dashboard rebuilt against the supplied galaxy reference. */
public final class CincoClickGuiScreen extends StarLegacyScreen {
    private static final String[] CATS={"Combat","Movement","Player","Visuals","World","Exploit","Render","Misc"};
    private static final String[] TABS={"General","Targeting","Placement","Breaking","Misc"};
    private static final int PINK=0xFFFF43D1, PURPLE=0xFFB866FF, TEXT=0xFFF8F3FF, MUTED=0xFFC7B9D7;
    private int category,moduleScroll;
    private ClientModule selected;
    private String search="";
    private boolean searchFocused;
    private ClientModule sliderModule;
    private int sliderIndex=-1,sliderX,sliderW;
    private String captureSpotifyKey;
    private int gx,gy,gw,gh,leftW,midW,rightX,rightW;

    public CincoClickGuiScreen(){super(Text.literal("Star Client"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        StarBackground.render(c,width,height,delta);

        gw=Math.max(760,Math.min(width-34,(int)(width*.925)));
        gh=Math.max(440,Math.min(height-34,(int)(height*.905)));
        gx=(width-gw)/2; gy=(height-gh)/2;
        leftW=(int)(gw*.225);
        midW=(int)(gw*.355);
        rightX=gx+leftW+midW;
        rightW=gx+gw-rightX;

        // Reference shell: soft violet rim, translucent glass, no heavy black slab.
        glow(c,gx,gy,gx+gw,gy+gh,22);
        CincoDraw.rounded(c,gx-1,gy-1,gx+gw+1,gy+gh+1,20,0xD9D58CFF);
        CincoDraw.rounded(c,gx,gy,gx+gw,gy+gh,19,0xB40A0618);
        CincoDraw.rounded(c,gx+3,gy+3,gx+gw-3,gy+gh-3,17,0x520C0920);

        // Glass column separators like the reference.
        c.fill(gx+leftW,gy+26,gx+leftW+1,gy+gh-18,0x4B8E6BAA);
        c.fill(rightX,gy+26,rightX+1,gy+gh-18,0x4B8E6BAA);

        ensureSelected();
        drawSidebar(c,mx,my);
        drawModules(c,mx,my);
        drawSettings(c,mx,my);
        c.drawText(textRenderer,StarFonts.text("×",true),gx+gw-22,gy+12,0xFFF7F0FF,false);
    }

    private void glow(DrawContext c,int l,int t,int r,int b,int radius){
        CincoDraw.rounded(c,l-5,t-5,r+5,b+5,radius+5,0x18360066);
        CincoDraw.rounded(c,l-3,t-3,r+3,b+3,radius+3,0x2D8C26B3);
        CincoDraw.rounded(c,l-2,t-2,r+2,b+2,radius+2,0x55FF4CE1);
    }

    private void ensureSelected(){
        if(selected!=null&&!hidden(selected)&&matchesCategory(selected))return;
        List<ClientModule> list=visibleModules();
        if(category==0){ClientModule crystal=CincoClient.findModule("Auto Crystal");if(crystal!=null&&!hidden(crystal)){selected=crystal;return;}}
        selected=list.isEmpty()?null:list.get(0);
    }

    private void drawSidebar(DrawContext c,int mx,int my){
        int x=gx+16,w=leftW-31;
        int logoY=gy+16;
        // Reference brand block.
        CincoDraw.rounded(c,x+3,logoY+2,x+41,logoY+40,19,0x3DFF42D1);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦",true),x+22,logoY+15,PINK);
        c.drawText(textRenderer,StarFonts.text("STAR",true),x+48,logoY+16,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("CLIENT",true),x+83,logoY+16,0xFFFF65D5,false);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✧     ✧     ✦",true),x+w/2,logoY+53,0xFFE9B8FF);

        int catStart=gy+78;
        int footerStart=gy+gh-123;
        int gap=5;
        int catH=Math.max(31,Math.min(38,(footerStart-catStart-gap*7)/8));
        int yy=catStart;
        for(int i=0;i<CATS.length;i++){
            boolean sel=i==category,hov=inside(mx,my,x,yy,w,catH);
            int fill=sel?0x7F42134D:(hov?0x56251A39:0x42120D23);
            int border=sel?0xE8FF4BD6:(hov?0x9AB37BE8:0x5A9B70C0);
            if(sel){
                CincoDraw.rounded(c,x-3,yy-3,x+w+3,yy+catH+3,catH/2,0x22FF52DA);
                CincoDraw.rounded(c,x-1,yy-1,x+w+1,yy+catH+1,catH/2,0x49FF52DA);
            }
            CincoDraw.bubble(c,x,yy,x+w,yy+catH,catH/2,border,fill);
            String icon=switch(i){case 0->"⚔";case 1->"➜";case 2->"●";case 3->"◉";case 4->"◆";case 5->"⚙";case 6->"▣";default->"•••";};
            c.drawText(textRenderer,StarFonts.text(icon,true),x+14,yy+catH/2-4,sel?0xFFFFFFFF:0xFFE3B8FF,false);
            c.drawText(textRenderer,StarFonts.text(CATS[i],sel),x+39,yy+catH/2-4,sel?0xFFFFFFFF:0xFFE9DDFC,false);
            c.drawText(textRenderer,StarFonts.text("›",true),x+w-18,yy+catH/2-5,sel?0xFFFFFFFF:0xFFBCA6CE,false);
            yy+=catH+gap;
        }
        nav(c,mx,my,x,footerStart,w,"▣","Locker");
        nav(c,mx,my,x,footerStart+37,w,"▤","Configs");
        nav(c,mx,my,x,footerStart+74,w,"⚙","Settings");
    }

    private void nav(DrawContext c,int mx,int my,int x,int y,int w,String icon,String label){
        int h=32;boolean hov=inside(mx,my,x,y,w,h);
        CincoDraw.bubble(c,x,y,x+w,y+h,h/2,hov?0xA6C683F5:0x668968B0,hov?0x53241B39:0x43120D23);
        c.drawText(textRenderer,StarFonts.text(icon,true),x+13,y+12,0xFFE7C8FF,false);
        c.drawText(textRenderer,StarFonts.text(label,hov),x+39,y+12,hov?0xFFFFFFFF:0xFFE8DAF8,false);
        c.drawText(textRenderer,StarFonts.text("›",true),x+w-18,y+11,0xFFBCA6CE,false);
    }

    private void drawModules(DrawContext c,int mx,int my){
        int x=gx+leftW+18,y=gy+20,w=midW-34;
        c.drawText(textRenderer,StarFonts.text("Modules",true),x,y+1,TEXT,false);
        CincoDraw.rounded(c,x+w-32,y-3,x+w,y+25,12,0x6521183A);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("☷",true),x+w-16,y+6,0xFFF1DFFF);

        int searchY=y+31;
        CincoDraw.bubble(c,x,searchY,x+w,searchY+34,17,0x847759A6,0x6B151024);
        c.drawText(textRenderer,StarFonts.text("⌕",true),x+13,searchY+12,0xFFE7C8FF,false);
        c.drawText(textRenderer,StarFonts.text(search.isEmpty()?"Search modules...":search+(searchFocused?" _":""),false),x+38,searchY+12,search.isEmpty()?0xFFB7A5C7:0xFFF3EAF7,false);

        List<ClientModule> mods=visibleModules();
        int rowH=51,start=searchY+47,avail=gh-104,visible=Math.max(1,avail/rowH),max=Math.max(0,mods.size()-visible);
        moduleScroll=Math.max(0,Math.min(max,moduleScroll));
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll);int yy=start+j*rowH;
            boolean sel=m==selected,hov=inside(mx,my,x,yy,w,45);
            if(sel){
                CincoDraw.rounded(c,x-3,yy-3,x+w+3,yy+48,23,0x22FF4FD6);
                CincoDraw.rounded(c,x-1,yy-1,x+w+1,yy+46,22,0x56FF4FD6);
            }
            int border=sel?0xE9FF5BDA:(hov?0x9EDB8BFF:0x5D84649D);
            int fill=sel?0x77331455:(hov?0x52201838:0x47110D23);
            CincoDraw.bubble(c,x,yy,x+w,yy+45,22,border,fill);
            if(sel)rowSparkles(c,x,yy,w);

            // Reference-style circular icon ring.
            CincoDraw.rounded(c,x+7,yy+4,x+44,yy+41,18,sel?0x5AFF55D8:0x4D6F3C91);
            CincoDraw.rounded(c,x+9,yy+6,x+42,yy+39,16,0xA018102B);
            Item item=moduleItem(m.name);
            if(item!=null)c.drawItem(new ItemStack(item),x+17,yy+14);
            else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(m.name),true),x+25,yy+17,sel?0xFFFFFFFF:0xFFE8C6FF);

            c.drawText(textRenderer,StarFonts.text(trim(m.name,22),m.enabled||sel),x+53,yy+8,sel?0xFFFF6AD9:(m.enabled?0xFFFFD25E:TEXT),false);
            c.drawText(textRenderer,StarFonts.text(trim(m.description,30),false),x+53,yy+25,MUTED,false);
            drawToggle(c,x+w-66,yy+12,m.enabled);
            c.drawText(textRenderer,StarFonts.text("⚙",false),x+w-24,yy+17,0xFFE6D0F7,false);
        }
        if(max>0){int rail=x+w+7;CincoDraw.rounded(c,rail,start,rail+3,start+avail-7,2,0x44311E45);int thumb=Math.max(26,(avail-7)/(max+1));int py=start+(int)((avail-7-thumb)*(moduleScroll/(double)Math.max(1,max)));CincoDraw.rounded(c,rail,py,rail+3,py+thumb,2,PINK);}
    }

    private void rowSparkles(DrawContext c,int x,int y,int w){
        long t=System.currentTimeMillis()/120L;
        for(int i=0;i<7;i++){
            int sx=x+28+Math.floorMod((int)(t*3+i*67),Math.max(1,w-85));
            int sy=y+7+Math.floorMod(i*11+(int)t,29);
            int a=65+(i%3)*40;
            c.fill(sx,sy,sx+1,sy+1,(a<<24)|0x00FFD7FF);
        }
    }

    private void drawSettings(DrawContext c,int mx,int my){
        int x=rightX+14,y=gy+18,w=rightW-27;
        if(selected==null)return;
        CincoDraw.bubble(c,x,y,x+w,gy+gh-17,17,0x7DBA88F0,0x50120D25);
        // Nebula field / planets in lower settings card like target reference.
        int fieldY=y+121;
        CincoDraw.rounded(c,x+7,fieldY,x+w-7,gy+gh-24,16,0x2D170B36);
        CincoDraw.rounded(c,x+w/2-12,gy+gh-169,x+w-28,gy+gh-31,68,0x273C0A79);
        CincoDraw.rounded(c,x+w/2+31,gy+gh-126,x+w-70,gy+gh-42,42,0x6043156F);
        for(int i=0;i<20;i++){int sx=x+18+Math.floorMod(i*71,w-52);int sy=fieldY+24+Math.floorMod(i*37,Math.max(30,gh-190));c.fill(sx,sy,sx+1,sy+1,0x80FFD8FF);}

        // Header icon ring.
        CincoDraw.rounded(c,x+13,y+12,x+67,y+66,27,0x54FF58DD);
        CincoDraw.rounded(c,x+16,y+15,x+64,y+63,24,0x8A1C1132);
        Item item=moduleItem(selected.name);
        if(item!=null){c.getMatrices().pushMatrix();c.getMatrices().translate(x+24,y+23);c.getMatrices().scale(2.0f,2.0f);c.drawItem(new ItemStack(item),0,0);c.getMatrices().popMatrix();}
        else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(selected.name),true),x+40,y+34,0xFFFFFFFF);
        c.drawText(textRenderer,StarFonts.text(selected.name,true),x+82,y+21,TEXT,false);
        c.drawText(textRenderer,StarFonts.text(trim(selected.description,46),false),x+82,y+42,MUTED,false);
        drawToggle(c,x+w-61,y+22,selected.enabled);

        int tabsY=y+81,gap=5,tabW=(w-gap*4)/5;
        for(int i=0;i<TABS.length;i++){
            int tx=x+i*(tabW+gap);boolean active=i==0;
            if(active)CincoDraw.rounded(c,tx-2,tabsY-2,tx+tabW+2,tabsY+33,16,0x2FFF55D8);
            CincoDraw.bubble(c,tx,tabsY,tx+tabW,tabsY+31,15,active?0xDFFF63DC:0x5F755893,active?0x85321853:0x43100C20);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],active),tx+tabW/2,tabsY+11,active?0xFFFFFFFF:0xFFD0BDDF);
        }

        int contentY=tabsY+53;
        if(selected.name.equals("HUD")){drawHudSettings(c,x+14,contentY,w-28);return;}
        if(selected.name.equals("Spotify HUD")){drawSpotifySettings(c,x+14,contentY,w-28);return;}
        int yy=contentY;
        yy=settingSlider(c,x+14,yy,w-28,selected,0,selected.settingLabel,selected.setting,selected.min,selected.max);
        yy=settingSlider(c,x+14,yy,w-28,selected,1,selected.setting2Label,selected.setting2,selected.min2,selected.max2);
        yy=settingSlider(c,x+14,yy,w-28,selected,2,selected.setting3Label,selected.setting3,selected.min3,selected.max3);
        yy+=8;settingRow(c,x+14,yy,w-28,"Keybind",keyName(selected.keyCode),false);
    }

    private int settingSlider(DrawContext c,int x,int y,int w,ClientModule m,int idx,String label,double value,double min,double max){
        if(label==null||label.isBlank()||max<=min)return y;
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+10,0xFFF2EAF8,false);
        int valueX=x+(int)(w*.42),valueW=52;
        CincoDraw.bubble(c,valueX,y,valueX+valueW,y+29,14,0x6D8A6BAD,0x75110D20);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(fmt(value),true),valueX+valueW/2,y+10,0xFFFFFFFF);
        int sx=x+(int)(w*.57),sy=y+13,sw=Math.max(55,w-(sx-x)-8);
        CincoDraw.rounded(c,sx,sy,sx+sw,sy+4,2,0x6D5A4975);
        double t=Math.max(0,Math.min(1,(value-min)/(max-min)));
        CincoDraw.rounded(c,sx,sy,sx+(int)(sw*t),sy+4,2,0xFFAD48FF);
        CincoDraw.rounded(c,sx+(int)(sw*t)-5,sy-4,sx+(int)(sw*t)+6,sy+9,7,PINK);
        if(sliderModule==m&&sliderIndex==idx){sliderX=sx;sliderW=sw;}
        return y+51;
    }

    private void drawHudSettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Region Map",StarHudConfig.regionMap);yy=boolSetting(c,x,yy,w,"Status Bar",StarHudConfig.statusBar);yy=boolSetting(c,x,yy,w,"Potion HUD",StarHudConfig.potionHud);yy=boolSetting(c,x,yy,w,"Module List",StarHudConfig.moduleList);yy+=4;c.drawText(textRenderer,StarFonts.text("Region Map",true),x,yy,0xFFFF71D8,false);yy+=22;yy=boolSetting(c,x,yy,w,"Show Players",StarHudConfig.regionPlayers);yy=boolSetting(c,x,yy,w,"Show Storage",StarHudConfig.regionStorage);yy=boolSetting(c,x,yy,w,"Show Legend",StarHudConfig.regionLegend);yy+=4;c.drawText(textRenderer,StarFonts.text("Status Bar",true),x,yy,0xFFFF71D8,false);yy+=22;yy=boolSetting(c,x,yy,w,"Clock",StarHudConfig.statusClock);yy=boolSetting(c,x,yy,w,"FPS",StarHudConfig.statusFps);yy=boolSetting(c,x,yy,w,"Ping",StarHudConfig.statusPing);boolSetting(c,x,yy,w,"Coordinates",StarHudConfig.statusCoords);
    }
    private void drawSpotifySettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Album Cover",SpotifyHudConfig.albumCover);yy=boolSetting(c,x,yy,w,"Song Name",SpotifyHudConfig.song);yy=boolSetting(c,x,yy,w,"Artist",SpotifyHudConfig.artist);yy=boolSetting(c,x,yy,w,"Lyrics",SpotifyHudConfig.lyrics);yy=boolSetting(c,x,yy,w,"Playback Controls",SpotifyHudConfig.controls);yy=boolSetting(c,x,yy,w,"Progress / Seek",SpotifyHudConfig.progress);yy+=4;settingRow(c,x,yy,w,"Lyric Lines",Integer.toString(SpotifyHudConfig.lyricLines),false);yy+=34;settingRow(c,x,yy,w,"Previous Key",keyName(SpotifyHudConfig.previousKey),"prev".equals(captureSpotifyKey));yy+=34;settingRow(c,x,yy,w,"Play/Pause Key",keyName(SpotifyHudConfig.playPauseKey),"play".equals(captureSpotifyKey));yy+=34;settingRow(c,x,yy,w,"Next Key",keyName(SpotifyHudConfig.nextKey),"next".equals(captureSpotifyKey));
    }

    private int boolSetting(DrawContext c,int x,int y,int w,String label,boolean on){c.drawText(textRenderer,StarFonts.text(label,false),x,y+8,0xFFF0E7F6,false);drawToggle(c,x+w-48,y+3,on);return y+34;}
    private void settingRow(DrawContext c,int x,int y,int w,String label,String value,boolean active){c.drawText(textRenderer,StarFonts.text(label,false),x,y+9,0xFFF0E7F6,false);int boxW=Math.max(68,textRenderer.getWidth(value)+20);CincoDraw.bubble(c,x+w-boxW,y+1,x+w,y+30,14,active?0xC9FF55D4:0x5D7B5C98,active?0x7D35144B:0x68100C20);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(value,true),x+w-boxW/2,y+11,active?0xFFFFFFFF:0xFFE8DDEF);}
    private void drawToggle(DrawContext c,int x,int y,boolean on){if(on)CincoDraw.rounded(c,x-4,y-4,x+38,y+24,14,0x35FF47D2);CincoDraw.rounded(c,x,y,x+34,y+20,10,on?PINK:0xFF4A3C5E);CincoDraw.rounded(c,on?x+18:x+2,y+2,on?x+32:x+16,y+18,8,0xFFFFFFFF);}

    private Item moduleItem(String n){String s=n.toLowerCase(Locale.ROOT);if(s.contains("crystal"))return Items.END_CRYSTAL;if(s.contains("anchor"))return Items.RESPAWN_ANCHOR;if(s.contains("totem"))return Items.TOTEM_OF_UNDYING;if(s.contains("pearl"))return Items.ENDER_PEARL;if(s.contains("web"))return Items.COBWEB;if(s.contains("aura")||s.contains("sword"))return Items.DIAMOND_SWORD;if(s.contains("regear")||s.contains("armor"))return Items.NETHERITE_CHESTPLATE;if(s.contains("eat")||s.contains("apple"))return Items.ENCHANTED_GOLDEN_APPLE;if(s.contains("xp"))return Items.EXPERIENCE_BOTTLE;if(s.contains("elytra")||s.contains("flight"))return Items.ELYTRA;if(s.contains("storage")||s.contains("container"))return Items.ENDER_CHEST;if(s.contains("spawner"))return Items.SPAWNER;if(s.contains("bed"))return Items.RED_BED;if(s.contains("obsidian")||s.contains("surround"))return Items.OBSIDIAN;if(s.contains("torch")||s.contains("bright"))return Items.TORCH;return null;}
    private String moduleGlyph(String n){String s=n.toLowerCase(Locale.ROOT);if(s.contains("hud"))return"▦";if(s.contains("esp")||s.contains("visual"))return"◉";if(s.contains("speed"))return"➜";if(s.contains("reach"))return"↔";if(s.contains("sky"))return"✦";return"◆";}

    private List<ClientModule> visibleModules(){List<ClientModule> out=new ArrayList<>();String q=search.toLowerCase(Locale.ROOT);for(ClientModule m:CincoClient.MODULES){if(hidden(m))continue;if(!q.isBlank()&&!((m.name+" "+m.description).toLowerCase(Locale.ROOT).contains(q)))continue;if(q.isBlank()&&!matchesCategory(m))continue;out.add(m);}return out;}
    private boolean hidden(ClientModule m){return m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer")||m.name.equals("Interactive Spotify")||m.name.equals("Region Map")||m.name.equals("Crystal")||m.name.equals("Mace");}
    private boolean matchesCategory(ClientModule m){return switch(category){case 0->m.category==ClientModule.Category.COMBAT;case 1->m.category==ClientModule.Category.MOVEMENT;case 2->m.name.contains("Player")||m.name.contains("Skin")||m.name.contains("Totem");case 3->m.category==ClientModule.Category.RENDER&&!m.name.equals("HUD")&&!m.name.equals("Spotify HUD");case 4->m.category==ClientModule.Category.WORLD;case 5->m.category==ClientModule.Category.UTILITY&&isExploitish(m.name);case 6->m.category==ClientModule.Category.RENDER&&(m.name.equals("HUD")||m.name.equals("Spotify HUD")||m.name.contains("ESP")||m.name.contains("Visual"));default->m.category==ClientModule.Category.UTILITY||m.category==ClientModule.Category.PRACTICE||m.category==ClientModule.Category.SETTINGS;};}
    private boolean isExploitish(String n){String s=n.toLowerCase(Locale.ROOT);return s.contains("velocity")||s.contains("reach")||s.contains("flight")||s.contains("speed")||s.contains("free cam")||s.contains("fast");}

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;
        if(inside(mx,my,gx+gw-31,gy+7,27,27)){if(client!=null)client.setScreen(null);return true;}
        int sx=gx+16,sw=leftW-31,catStart=gy+78,footerStart=gy+gh-123,gap=5,catH=Math.max(31,Math.min(38,(footerStart-catStart-gap*7)/8));
        int yy=catStart;for(int i=0;i<CATS.length;i++){if(inside(mx,my,sx,yy,sw,catH)){category=i;search="";moduleScroll=0;selected=null;ensureSelected();return true;}yy+=catH+gap;}
        if(inside(mx,my,sx,footerStart,sw,32)){if(client!=null)client.setScreen(new LockerScreen());return true;}
        if(inside(mx,my,sx,footerStart+37,sw,32)){if(client!=null)client.setScreen(new KeybindsScreen());return true;}
        if(inside(mx,my,sx,footerStart+74,sw,32)){if(client!=null)client.setScreen(new ClientSettingsScreen());return true;}

        int x=gx+leftW+18,y=gy+20,w=midW-34,searchY=y+31;
        if(inside(mx,my,x,searchY,w,34)){searchFocused=true;return true;}searchFocused=false;
        List<ClientModule> mods=visibleModules();int rowH=51,start=searchY+47,avail=gh-104,visible=Math.max(1,avail/rowH);
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){ClientModule m=mods.get(j+moduleScroll);int ry=start+j*rowH;if(!inside(mx,my,x,ry,w,45))continue;selected=m;if(mx>=x+w-76&&mx<=x+w-29){m.enabled=!m.enabled;ConfigStore.save();}return true;}

        int rx=rightX+14,ry=gy+18,rw=rightW-27;
        if(selected!=null&&inside(mx,my,rx+rw-68,ry+16,58,40)){selected.enabled=!selected.enabled;ConfigStore.save();return true;}
        int contentY=ry+81+53;
        if(selected!=null&&selected.name.equals("HUD"))return clickHud(mx,my,rx+28,contentY,rw-56);
        if(selected!=null&&selected.name.equals("Spotify HUD"))return clickSpotify(mx,my,rx+28,contentY,rw-56);
        if(selected!=null){int sy=contentY;for(int idx=0;idx<3;idx++){String label=idx==0?selected.settingLabel:idx==1?selected.setting2Label:selected.setting3Label;double min=idx==0?selected.min:idx==1?selected.min2:selected.min3,max=idx==0?selected.max:idx==1?selected.max2:selected.max3;if(label==null||label.isBlank()||max<=min)continue;int sx2=rx+28+(int)((rw-56)*.57),sw2=Math.max(55,(rw-56)-(int)((rw-56)*.57)-8);if(inside(mx,my,sx2,sy-2,sw2,34)){sliderModule=selected;sliderIndex=idx;sliderX=sx2;sliderW=sw2;setSlider(mx);return true;}sy+=51;}}
        return false;
    }
    private boolean clickHud(double mx,double my,int x,int y,int w){String[] ids={"region","status","potions","modules","players","storage","legend","clock","fps","ping","coords"};int[] ys={y,y+34,y+68,y+102,y+162,y+196,y+230,y+290,y+324,y+358,y+392};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,ys[i],w,30)){StarHudConfig.toggle(ids[i]);return true;}return false;}
    private boolean clickSpotify(double mx,double my,int x,int y,int w){String[] ids={"cover","song","artist","lyrics","controls","progress"};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,y+i*34,w,30)){SpotifyHudConfig.toggle(ids[i]);return true;}int yy=y+208;if(inside(mx,my,x,yy,w,30)){SpotifyHudConfig.lyricLines=SpotifyHudConfig.lyricLines>=6?0:SpotifyHudConfig.lyricLines+1;SpotifyHudConfig.save();return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="prev";return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="play";return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="next";return true;}return false;}

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&sliderModule!=null){setSlider(mx);return true;}return false;}
    @Override public boolean mouseReleased(double mx,double my,int button){if(sliderModule!=null){sliderModule=null;sliderIndex=-1;ConfigStore.save();return true;}return false;}
    private void setSlider(double mx){if(sliderModule==null||sliderW<=0)return;double t=Math.max(0,Math.min(1,(mx-sliderX)/sliderW));double min=sliderIndex==0?sliderModule.min:sliderIndex==1?sliderModule.min2:sliderModule.min3,max=sliderIndex==0?sliderModule.max:sliderIndex==1?sliderModule.max2:sliderModule.max3,step=sliderIndex==0?sliderModule.step:sliderIndex==1?sliderModule.step2:sliderModule.step3;double v=min+(max-min)*t;if(step>0)v=Math.round(v/step)*step;if(sliderIndex==0)sliderModule.setting=v;else if(sliderIndex==1)sliderModule.setting2=v;else sliderModule.setting3=v;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){int x=gx+leftW+18,w=midW-34;if(inside(mx,my,x,gy+98,w,gh-111)){List<ClientModule> mods=visibleModules();int visible=Math.max(1,(gh-104)/51),max=Math.max(0,mods.size()-visible);moduleScroll=Math.max(0,Math.min(max,moduleScroll+(vertical<0?1:-1)));return true;}return false;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(captureSpotifyKey!=null){if("prev".equals(captureSpotifyKey))SpotifyHudConfig.previousKey=keyCode;else if("play".equals(captureSpotifyKey))SpotifyHudConfig.playPauseKey=keyCode;else SpotifyHudConfig.nextKey=keyCode;captureSpotifyKey=null;SpotifyHudConfig.save();return true;}if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){if(client!=null)client.setScreen(null);return true;}if(searchFocused){if(keyCode==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty())search=search.substring(0,search.length()-1);else if(keyCode==GLFW.GLFW_KEY_ENTER)searchFocused=false;return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&chr>=32&&chr!=127&&search.length()<40){search+=chr;moduleScroll=0;return true;}return super.charTyped(chr,modifiers);}

    private String keyName(int key){if(key<0)return"NONE";String n=GLFW.glfwGetKeyName(key,0);return n==null?Integer.toString(key):n.toUpperCase(Locale.ROOT);}
    private String fmt(double v){return Math.abs(v-Math.rint(v))<.0001?Integer.toString((int)Math.rint(v)):String.format(Locale.ROOT,"%.1f",v);}
    private String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
