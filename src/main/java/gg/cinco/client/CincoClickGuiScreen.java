package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Star Client dashboard matched to the polished three-column reference layout. */
public final class CincoClickGuiScreen extends StarLegacyScreen {
    private static final String[] CATS={"Combat","Movement","Player","Visuals","World","Exploit","Render","Misc"};
    private static final String[] TABS={"General","Targeting","Placement","Breaking","Misc"};
    private int category=0,moduleScroll=0;
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
        c.fill(0,0,width,height,0xFF020309);
        StarBackground.render(c,width,height,delta);

        gw=Math.min(1180,Math.max(760,width-28));
        gh=Math.min(680,Math.max(500,height-28));
        gx=(width-gw)/2; gy=(height-gh)/2;
        leftW=Math.max(176,(int)(gw*.225));
        midW=Math.max(270,(int)(gw*.34));
        rightX=gx+leftW+midW;
        rightW=gx+gw-rightX;

        // soft outer glow + reference-like glass shell
        CincoDraw.rounded(c,gx-3,gy-3,gx+gw+3,gy+gh+3,17,0x403F0A59);
        CincoDraw.rounded(c,gx-1,gy-1,gx+gw+1,gy+gh+1,16,0xFFB52DFF);
        CincoDraw.rounded(c,gx+1,gy+1,gx+gw-1,gy+gh-1,15,0xF2050610);
        c.fill(gx+2,gy+2,gx+gw-2,gy+4,0xFF8D32EA);
        c.fill(gx+gw-4,gy+5,gx+gw-2,gy+gh-7,0x99E848C1);
        c.fill(gx+leftW,gy+22,gx+leftW+1,gy+gh-1,0x663E1D59);
        c.fill(rightX,gy+22,rightX+1,gy+gh-1,0x663E1D59);

        ensureSelected();
        drawSidebar(c,mx,my);
        drawModules(c,mx,my);
        drawSettings(c,mx,my);

        c.drawText(textRenderer,StarFonts.text("×",true),gx+gw-24,gy+13,0xFFE1CFF4,false);
    }

    private void ensureSelected(){
        if(selected!=null&&!hidden(selected))return;
        ClientModule crystal=CincoClient.findModule("Auto Crystal");
        if(crystal!=null&&!hidden(crystal)){selected=crystal;return;}
        List<ClientModule> list=visibleModules(); if(!list.isEmpty())selected=list.get(0);
    }

    private void drawSidebar(DrawContext c,int mx,int my){
        int x=gx+18,w=leftW-36;
        int logoY=gy+18,logoH=142;
        // logo zone: intentionally integrated, no floating texture/mixin rectangle
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦     ✧     ✦",true),x+w/2,logoY+10,0xFFC98CFF);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("STARS",true),x+w/2,logoY+48,0xFFE7CBFF);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("CLIENT",true),x+w/2,logoY+69,0xFFFFFFFF);
        c.fill(x+18,logoY+91,x+w/2-21,logoY+93,0x55A53DFF);
        c.fill(x+w/2+21,logoY+91,x+w-18,logoY+93,0x55FF8A24);
        CincoDraw.rounded(c,x+w/2-18,logoY+87,x+w/2+18,logoY+97,6,0x667C32A7);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦",true),x+w/2,logoY+88,0xFFFFAE55);

        int yy=gy+154;
        int bottomStart=gy+gh-126;
        int catH=Math.max(34,Math.min(43,(bottomStart-yy-7*7)/8));
        for(int i=0;i<CATS.length;i++){
            boolean sel=i==category,hov=inside(mx,my,x,yy,w,catH);
            if(sel){
                CincoDraw.rounded(c,x-2,yy-2,x+w+2,yy+catH+2,11,0x55FF7A16);
                CincoDraw.bubble(c,x,yy,x+w,yy+catH,9,0xFFFF8A25,0xC7291521);
            }else CincoDraw.bubble(c,x,yy,x+w,yy+catH,9,hov?0xAA8150B5:0x554A2867,hov?0xB0181024:0x98100B1B);
            String icon=switch(i){case 0->"⚔";case 1->"✦";case 2->"●";case 3->"◉";case 4->"◆";case 5->"⚙";case 6->"▣";default->"•••";};
            c.drawText(textRenderer,StarFonts.text(icon,true),x+13,yy+catH/2-4,sel?0xFFFFA64C:0xFFC185FF,false);
            c.drawText(textRenderer,StarFonts.text(CATS[i],sel),x+38,yy+catH/2-4,sel?0xFFFFC178:0xFFD9C5EC,false);
            yy+=catH+7;
        }

        nav(c,mx,my,x,bottomStart,w,"✦  Locker");
        nav(c,mx,my,x,bottomStart+38,w,"▣  Configs");
        nav(c,mx,my,x,bottomStart+76,w,"⚙  Settings");
    }

    private void nav(DrawContext c,int mx,int my,int x,int y,int w,String label){
        boolean hov=inside(mx,my,x,y,w,31);
        CincoDraw.bubble(c,x,y,x+w,y+31,8,hov?0xAA9A58D4:0x554B2C64,hov?0xB0191028:0x94100B1B);
        c.drawText(textRenderer,StarFonts.text(label,hov),x+12,y+11,hov?0xFFFFFFFF:0xFFD1BAEA,false);
    }

    private void drawModules(DrawContext c,int mx,int my){
        int x=gx+leftW+16,y=gy+18,w=midW-30;
        c.drawText(textRenderer,StarFonts.text("Modules",true),x,y+4,0xFFF8F1FF,false);
        CincoDraw.bubble(c,x,y+34,x+w,y+66,8,0x88603985,0xB50C0A17);
        c.drawText(textRenderer,StarFonts.text("⌕",true),x+11,y+45,0xFFC58BFF,false);
        c.drawText(textRenderer,StarFonts.text(search.isEmpty()?"Search modules...":search+(searchFocused?" _":""),false),x+34,y+45,search.isEmpty()?0xFF8E7EA5:0xFFE9DFF2,false);

        List<ClientModule> mods=visibleModules();
        int rowH=47,start=y+78,avail=gh-108,visible=Math.max(1,avail/rowH),max=Math.max(0,mods.size()-visible);
        moduleScroll=Math.max(0,Math.min(max,moduleScroll));
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll); int yy=start+j*rowH;
            boolean sel=m==selected,hov=inside(mx,my,x,yy,w,41);
            if(sel)CincoDraw.rounded(c,x-2,yy-2,x+w+2,yy+43,10,0x553D0B61);
            CincoDraw.bubble(c,x,yy,x+w,yy+41,8,sel?0xFF9E3DE8:(hov?0xAA7643A5:0x55452860),m.enabled?0xBA211226:(sel?0xBD151021:0xA30A0912));
            if(sel)c.fill(x+w-2,yy+5,x+w,yy+36,0xFFFF8A26);
            int badge=m.enabled?0xFF6C2B9A:0xFF281A38;
            CincoDraw.rounded(c,x+9,yy+7,x+37,yy+35,7,badge);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(m.name),true),x+23,yy+16,m.enabled?0xFFE4B8FF:0xFFB28CD0);
            c.drawText(textRenderer,StarFonts.text(trim(m.name,23),m.enabled||sel),x+47,yy+8,m.enabled?0xFFFFB05A:0xFFF2EBF6,false);
            c.drawText(textRenderer,StarFonts.text(trim(m.description,30),false),x+47,yy+24,0xFFA697B1,false);
            drawToggle(c,x+w-65,yy+12,m.enabled);
            c.drawText(textRenderer,StarFonts.text("⚙",false),x+w-25,yy+15,0xFFB38FCF,false);
        }
        if(max>0){
            int railX=x+w+7; c.fill(railX,start,railX+3,start+avail-7,0x332F1B43);
            int thumb=Math.max(28,(avail-7)/(max+1));
            int py=start+(int)((avail-7-thumb)*(moduleScroll/(double)Math.max(1,max)));
            CincoDraw.rounded(c,railX,py,railX+3,py+thumb,2,0xFFFF8A29);
        }
    }

    private void drawSettings(DrawContext c,int mx,int my){
        int x=rightX+14,y=gy+18,w=rightW-28;
        if(selected==null)return;
        // self-contained rounded right card, like the reference
        CincoDraw.bubble(c,x,y,x+w,gy+gh-18,10,0x885C3482,0x8A070812);
        // subtle nebula glow in lower right pane
        CincoDraw.rounded(c,x+w/2-20,gy+gh-150,x+w-28,gy+gh-28,58,0x24110A2E);
        CincoDraw.rounded(c,x+w/2+20,gy+gh-118,x+w-62,gy+gh-42,38,0x302F0A69);

        CincoDraw.rounded(c,x+15,y+14,x+65,y+64,12,0xFF4D176C);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(selected.name),true),x+40,y+33,0xFFE5C5FF);
        c.drawText(textRenderer,StarFonts.text(selected.name,true),x+82,y+20,0xFFF9F4FF,false);
        c.drawText(textRenderer,StarFonts.text(trim(selected.description,52),false),x+82,y+42,0xFFBAA8C8,false);
        drawToggle(c,x+w-58,y+24,selected.enabled);

        int tabsY=y+82,gap=4,tabW=(w-gap*4)/5;
        for(int i=0;i<TABS.length;i++){
            int tx=x+i*(tabW+gap); boolean active=i==0;
            CincoDraw.rounded(c,tx,tabsY,tx+tabW,tabsY+31,8,active?0xB52B1420:0xB00E0B1C);
            if(active)c.fill(tx+7,tabsY+29,tx+tabW-7,tabsY+31,0xFFFF861B);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],active),tx+tabW/2,tabsY+11,active?0xFFFFC077:0xFFBDA7D1);
        }

        int contentY=tabsY+50;
        if(selected.name.equals("HUD")){drawHudSettings(c,x+10,contentY,w-20);return;}
        if(selected.name.equals("Spotify HUD")){drawSpotifySettings(c,x+10,contentY,w-20);return;}

        int yy=contentY;
        yy=settingSlider(c,x+14,yy,w-28,selected,0,selected.settingLabel,selected.setting,selected.min,selected.max);
        yy=settingSlider(c,x+14,yy,w-28,selected,1,selected.setting2Label,selected.setting2,selected.min2,selected.max2);
        yy=settingSlider(c,x+14,yy,w-28,selected,2,selected.setting3Label,selected.setting3,selected.min3,selected.max3);
        yy+=8;
        settingRow(c,x+14,yy,w-28,"Keybind",keyName(selected.keyCode),false);
    }

    private int settingSlider(DrawContext c,int x,int y,int w,ClientModule m,int idx,String label,double value,double min,double max){
        if(label==null||label.isBlank()||max<=min)return y;
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+8,0xFFD9CBE4,false);
        int valueX=x+(int)(w*.39),valueW=54;
        CincoDraw.bubble(c,valueX,y,valueX+valueW,y+29,6,0x554B2C67,0x9D0E0B1B);
        String v=fmt(value); c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(v,true),valueX+valueW/2,y+10,0xFFE9DFF1);
        int sx=x+(int)(w*.55),sy=y+13,sw=Math.max(55,w-(sx-x)-8);
        c.fill(sx,sy,sx+sw,sy+4,0xFF2B203B);
        double t=Math.max(0,Math.min(1,(value-min)/(max-min)));
        c.fill(sx,sy,sx+(int)(sw*t),sy+4,0xFFB42DFF);
        CincoDraw.rounded(c,sx+(int)(sw*t)-5,sy-4,sx+(int)(sw*t)+6,sy+8,6,0xFFB52EFF);
        if(sliderModule==m&&sliderIndex==idx){sliderX=sx;sliderW=sw;}
        return y+51;
    }

    private void drawHudSettings(DrawContext c,int x,int y,int w){
        int yy=y;
        yy=boolSetting(c,x,yy,w,"Region Map",StarHudConfig.regionMap);
        yy=boolSetting(c,x,yy,w,"Status Bar",StarHudConfig.statusBar);
        yy=boolSetting(c,x,yy,w,"Potion HUD",StarHudConfig.potionHud);
        yy=boolSetting(c,x,yy,w,"Module List",StarHudConfig.moduleList);
        yy+=4;
        c.drawText(textRenderer,StarFonts.text("Region Map",true),x,yy,0xFFC995FF,false); yy+=22;
        yy=boolSetting(c,x,yy,w,"Show Players",StarHudConfig.regionPlayers);
        yy=boolSetting(c,x,yy,w,"Show Storage",StarHudConfig.regionStorage);
        yy=boolSetting(c,x,yy,w,"Show Legend",StarHudConfig.regionLegend);
        yy+=4;
        c.drawText(textRenderer,StarFonts.text("Status Bar",true),x,yy,0xFFC995FF,false); yy+=22;
        yy=boolSetting(c,x,yy,w,"Clock",StarHudConfig.statusClock);
        yy=boolSetting(c,x,yy,w,"FPS",StarHudConfig.statusFps);
        yy=boolSetting(c,x,yy,w,"Ping",StarHudConfig.statusPing);
        boolSetting(c,x,yy,w,"Coordinates",StarHudConfig.statusCoords);
    }

    private void drawSpotifySettings(DrawContext c,int x,int y,int w){
        int yy=y;
        yy=boolSetting(c,x,yy,w,"Album Cover",SpotifyHudConfig.albumCover);
        yy=boolSetting(c,x,yy,w,"Song Name",SpotifyHudConfig.song);
        yy=boolSetting(c,x,yy,w,"Artist",SpotifyHudConfig.artist);
        yy=boolSetting(c,x,yy,w,"Lyrics",SpotifyHudConfig.lyrics);
        yy=boolSetting(c,x,yy,w,"Playback Controls",SpotifyHudConfig.controls);
        yy=boolSetting(c,x,yy,w,"Progress / Seek",SpotifyHudConfig.progress);
        yy+=4;
        settingRow(c,x,yy,w,"Lyric Lines",Integer.toString(SpotifyHudConfig.lyricLines),false); yy+=34;
        settingRow(c,x,yy,w,"Previous Key",keyName(SpotifyHudConfig.previousKey),"prev".equals(captureSpotifyKey)); yy+=34;
        settingRow(c,x,yy,w,"Play/Pause Key",keyName(SpotifyHudConfig.playPauseKey),"play".equals(captureSpotifyKey)); yy+=34;
        settingRow(c,x,yy,w,"Next Key",keyName(SpotifyHudConfig.nextKey),"next".equals(captureSpotifyKey));
    }

    private int boolSetting(DrawContext c,int x,int y,int w,String label,boolean on){
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+8,0xFFDCCFE7,false);
        drawToggle(c,x+w-48,y+3,on);
        return y+34;
    }
    private void settingRow(DrawContext c,int x,int y,int w,String label,String value,boolean active){
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+9,0xFFDCCFE7,false);
        int boxW=Math.max(68,textRenderer.getWidth(value)+20);
        CincoDraw.bubble(c,x+w-boxW,y+1,x+w,y+29,7,active?0xFFFF861B:0x55502E6C,active?0xB52A1421:0x990E0B19);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(value,true),x+w-boxW/2,y+11,active?0xFFFFC17A:0xFFE5D7EF);
    }
    private void drawToggle(DrawContext c,int x,int y,boolean on){
        if(on)CincoDraw.rounded(c,x-3,y-3,x+37,y+23,12,0x33FF8A20);
        CincoDraw.rounded(c,x,y,x+34,y+20,10,on?0xFFFF8517:0xFF3B304E);
        CincoDraw.rounded(c,on?x+18:x+2,y+2,on?x+32:x+16,y+18,8,on?0xFFFFFFFF:0xFFD6CEE4);
    }

    private List<ClientModule> visibleModules(){
        List<ClientModule> out=new ArrayList<>(); String q=search.toLowerCase(Locale.ROOT);
        for(ClientModule m:CincoClient.MODULES){
            if(hidden(m))continue;
            if(!q.isBlank()&&!((m.name+" "+m.description).toLowerCase(Locale.ROOT).contains(q)))continue;
            if(q.isBlank()&&!matchesCategory(m))continue;
            out.add(m);
        }
        return out;
    }
    private boolean hidden(ClientModule m){return m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer")||m.name.equals("Interactive Spotify")||m.name.equals("Region Map")||m.name.equals("Crystal")||m.name.equals("Mace");}
    private boolean matchesCategory(ClientModule m){return switch(category){case 0->m.category==ClientModule.Category.COMBAT;case 1->m.category==ClientModule.Category.MOVEMENT;case 2->m.name.contains("Player")||m.name.contains("Skin")||m.name.contains("Totem");case 3->m.category==ClientModule.Category.RENDER&&!m.name.equals("HUD")&&!m.name.equals("Spotify HUD");case 4->m.category==ClientModule.Category.WORLD;case 5->m.category==ClientModule.Category.UTILITY&&isExploitish(m.name);case 6->m.category==ClientModule.Category.RENDER&&(m.name.equals("HUD")||m.name.equals("Spotify HUD")||m.name.contains("ESP")||m.name.contains("Visual"));default->m.category==ClientModule.Category.UTILITY||m.category==ClientModule.Category.PRACTICE||m.category==ClientModule.Category.SETTINGS;};}
    private boolean isExploitish(String n){String s=n.toLowerCase(Locale.ROOT);return s.contains("velocity")||s.contains("reach")||s.contains("flight")||s.contains("speed")||s.contains("free cam")||s.contains("fast");}
    private String moduleGlyph(String n){String s=n.toLowerCase(Locale.ROOT);if(s.contains("crystal"))return"◆";if(s.contains("anchor"))return"▣";if(s.contains("totem"))return"☻";if(s.contains("aura"))return"⚔";if(s.contains("web"))return"✣";if(s.contains("hud"))return"▦";if(s.contains("esp"))return"◉";return"✦";}

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;
        if(inside(mx,my,gx+gw-31,gy+7,27,27)){if(client!=null)client.setScreen(null);return true;}
        int sx=gx+18,sw=leftW-36,yy=gy+154,bottomStart=gy+gh-126,catH=Math.max(34,Math.min(43,(bottomStart-yy-7*7)/8));
        for(int i=0;i<CATS.length;i++){if(inside(mx,my,sx,yy+i*(catH+7),sw,catH)){category=i;search="";moduleScroll=0;selected=null;ensureSelected();return true;}}
        if(inside(mx,my,sx,bottomStart,sw,31)){if(client!=null)client.setScreen(new LockerScreen());return true;}
        if(inside(mx,my,sx,bottomStart+38,sw,31)){if(client!=null)client.setScreen(new KeybindsScreen());return true;}
        if(inside(mx,my,sx,bottomStart+76,sw,31)){if(client!=null)client.setScreen(new ClientSettingsScreen());return true;}

        int x=gx+leftW+16,y=gy+18,w=midW-30;
        if(inside(mx,my,x,y+34,w,32)){searchFocused=true;return true;} searchFocused=false;
        List<ClientModule> mods=visibleModules();int rowH=47,start=y+78,avail=gh-108,visible=Math.max(1,avail/rowH);
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll);int ry=start+j*rowH;if(!inside(mx,my,x,ry,w,41))continue;
            selected=m;
            if(mx>=x+w-72&&mx<=x+w-31){m.enabled=!m.enabled;ConfigStore.save();}
            return true;
        }

        int rx=rightX+14,ry=gy+18,rw=rightW-28;
        if(selected!=null&&inside(mx,my,rx+rw-64,ry+18,52,34)){selected.enabled=!selected.enabled;ConfigStore.save();return true;}
        int contentY=ry+82+50;
        if(selected!=null&&selected.name.equals("HUD")){return clickHud(mx,my,rx+24,contentY,rw-40);}
        if(selected!=null&&selected.name.equals("Spotify HUD")){return clickSpotify(mx,my,rx+24,contentY,rw-40);}
        if(selected!=null){
            int sy=contentY;
            for(int idx=0;idx<3;idx++){
                String label=idx==0?selected.settingLabel:idx==1?selected.setting2Label:selected.setting3Label;
                double min=idx==0?selected.min:idx==1?selected.min2:selected.min3,max=idx==0?selected.max:idx==1?selected.max2:selected.max3;
                if(label==null||label.isBlank()||max<=min)continue;
                int sx2=rx+28+(int)((rw-56)*.55),sw2=Math.max(55,(rw-56)-(int)((rw-56)*.55)-8);
                if(inside(mx,my,sx2,sy-2,sw2,34)){sliderModule=selected;sliderIndex=idx;sliderX=sx2;sliderW=sw2;setSlider(mx);return true;}
                sy+=51;
            }
        }
        return false;
    }

    private boolean clickHud(double mx,double my,int x,int y,int w){
        String[] ids={"region","status","potions","modules","players","storage","legend","clock","fps","ping","coords"};
        int[] ys={y,y+34,y+68,y+102,y+162,y+196,y+230,y+290,y+324,y+358,y+392};
        for(int i=0;i<ids.length;i++)if(inside(mx,my,x,ys[i],w,30)){StarHudConfig.toggle(ids[i]);return true;}return false;
    }
    private boolean clickSpotify(double mx,double my,int x,int y,int w){
        String[] ids={"cover","song","artist","lyrics","controls","progress"};
        for(int i=0;i<ids.length;i++)if(inside(mx,my,x,y+i*34,w,30)){SpotifyHudConfig.toggle(ids[i]);return true;}
        int yy=y+208;
        if(inside(mx,my,x,yy,w,30)){SpotifyHudConfig.lyricLines=SpotifyHudConfig.lyricLines>=6?0:SpotifyHudConfig.lyricLines+1;SpotifyHudConfig.save();return true;}yy+=34;
        if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="prev";return true;}yy+=34;
        if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="play";return true;}yy+=34;
        if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="next";return true;}return false;
    }

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&sliderModule!=null){setSlider(mx);return true;}return false;}
    @Override public boolean mouseReleased(double mx,double my,int button){if(sliderModule!=null){sliderModule=null;sliderIndex=-1;ConfigStore.save();return true;}return false;}
    private void setSlider(double mx){if(sliderModule==null||sliderW<=0)return;double t=Math.max(0,Math.min(1,(mx-sliderX)/sliderW));double min=sliderIndex==0?sliderModule.min:sliderIndex==1?sliderModule.min2:sliderModule.min3,max=sliderIndex==0?sliderModule.max:sliderIndex==1?sliderModule.max2:sliderModule.max3,step=sliderIndex==0?sliderModule.step:sliderIndex==1?sliderModule.step2:sliderModule.step3;double v=min+(max-min)*t;if(step>0)v=Math.round(v/step)*step;if(sliderIndex==0)sliderModule.setting=v;else if(sliderIndex==1)sliderModule.setting2=v;else sliderModule.setting3=v;}

    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){int x=gx+leftW+16,w=midW-30;if(inside(mx,my,x,gy+90,w,gh-105)){List<ClientModule> mods=visibleModules();int visible=Math.max(1,(gh-108)/47),max=Math.max(0,mods.size()-visible);moduleScroll=Math.max(0,Math.min(max,moduleScroll+(vertical<0?1:-1)));return true;}return false;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(captureSpotifyKey!=null){if("prev".equals(captureSpotifyKey))SpotifyHudConfig.previousKey=keyCode;else if("play".equals(captureSpotifyKey))SpotifyHudConfig.playPauseKey=keyCode;else SpotifyHudConfig.nextKey=keyCode;captureSpotifyKey=null;SpotifyHudConfig.save();return true;}if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){if(client!=null)client.setScreen(null);return true;}if(searchFocused){if(keyCode==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty())search=search.substring(0,search.length()-1);else if(keyCode==GLFW.GLFW_KEY_ENTER)searchFocused=false;return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&chr>=32&&chr!=127&&search.length()<40){search+=chr;moduleScroll=0;return true;}return super.charTyped(chr,modifiers);}

    private String keyName(int key){if(key<0)return"NONE";String n=GLFW.glfwGetKeyName(key,0);return n==null?Integer.toString(key):n.toUpperCase(Locale.ROOT);}
    private String fmt(double v){return Math.abs(v-Math.rint(v))<.0001?Integer.toString((int)Math.rint(v)):String.format(Locale.ROOT,"%.1f",v);}
    private String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
