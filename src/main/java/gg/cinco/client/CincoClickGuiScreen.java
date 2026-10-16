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

/** Star Client dashboard: dense three-column layout matched to the supplied reference. */
public final class CincoClickGuiScreen extends StarLegacyScreen {
    private static final String[] CATS={"Combat","Movement","Player","Visuals","World","Exploit","Render","Misc"};
    private static final String[] TABS={"General","Targeting","Placement","Breaking","Misc"};
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
        c.fill(0,0,width,height,0xFF020309);
        StarBackground.render(c,width,height,delta);

        gw=Math.min(1220,Math.max(820,width-24));
        gh=Math.min(700,Math.max(520,height-24));
        gx=(width-gw)/2; gy=(height-gh)/2;
        leftW=Math.max(190,(int)(gw*.225));
        midW=Math.max(300,(int)(gw*.335));
        rightX=gx+leftW+midW;
        rightW=gx+gw-rightX;

        // outer shell + subtle neon edge
        CincoDraw.rounded(c,gx-5,gy-5,gx+gw+5,gy+gh+5,18,0x203D0B58);
        CincoDraw.rounded(c,gx-2,gy-2,gx+gw+2,gy+gh+2,17,0xFFB22CFF);
        CincoDraw.rounded(c,gx,gy,gx+gw,gy+gh,16,0xF4060710);
        c.fill(gx+4,gy+2,gx+gw-4,gy+4,0xCC8F2FE5);
        c.fill(gx+gw-4,gy+10,gx+gw-2,gy+gh-10,0x88EC51BE);
        c.fill(gx+leftW,gy+22,gx+leftW+1,gy+gh-12,0x573D1F55);
        c.fill(rightX,gy+22,rightX+1,gy+gh-12,0x573D1F55);

        ensureSelected();
        drawSidebar(c,mx,my);
        drawModules(c,mx,my);
        drawSettings(c,mx,my);
        c.drawText(textRenderer,StarFonts.text("×",true),gx+gw-25,gy+13,0xFFE7D9F3,false);
    }

    private void ensureSelected(){
        if(selected!=null&&!hidden(selected)&&matchesCategory(selected))return;
        List<ClientModule> list=visibleModules();
        if(category==0){ClientModule crystal=CincoClient.findModule("Auto Crystal");if(crystal!=null&&!hidden(crystal)){selected=crystal;return;}}
        selected=list.isEmpty()?null:list.get(0);
    }

    private void drawSidebar(DrawContext c,int mx,int my){
        int x=gx+20,w=leftW-40;
        int logoY=gy+14,logoH=112;

        // integrated brand zone — no separate overlay/mixin
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦      ✧      ✦",true),x+w/2,logoY+5,0xFFC98EFF);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("STARS",true),x+w/2,logoY+34,0xFFF1E2FF);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("CLIENT",true),x+w/2,logoY+55,0xFFFFFFFF);
        c.fill(x+18,logoY+81,x+w/2-22,logoY+83,0x555A29A6);
        c.fill(x+w/2+22,logoY+81,x+w-18,logoY+83,0x55FF8A20);
        CincoDraw.rounded(c,x+w/2-18,logoY+76,x+w/2+18,logoY+88,7,0x7A5C267B);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦",true),x+w/2,logoY+78,0xFFFFB05A);

        // fixed reserved category region so footer can never overlap
        int catStart=gy+126;
        int footerH=101;
        int footerStart=gy+gh-footerH-14;
        int gap=5;
        int catH=Math.max(28,Math.min(37,(footerStart-catStart-gap*7)/8));
        int yy=catStart;
        for(int i=0;i<CATS.length;i++){
            boolean sel=i==category,hov=inside(mx,my,x,yy,w,catH);
            if(sel){
                CincoDraw.rounded(c,x-3,yy-3,x+w+3,yy+catH+3,11,0x3AFF7B15);
                CincoDraw.bubble(c,x,yy,x+w,yy+catH,9,0xFFFF8B25,0xD42B1622);
            }else CincoDraw.bubble(c,x,yy,x+w,yy+catH,9,hov?0x9974459C:0x55472960,hov?0xB8191025:0x94100B1B);
            String icon=switch(i){case 0->"⚔";case 1->"➜";case 2->"●";case 3->"◉";case 4->"◆";case 5->"⚙";case 6->"▣";default->"•••";};
            c.drawText(textRenderer,StarFonts.text(icon,true),x+13,yy+catH/2-4,sel?0xFFFFB15D:0xFFBF83F5,false);
            c.drawText(textRenderer,StarFonts.text(CATS[i],sel),x+39,yy+catH/2-4,sel?0xFFFFC583:0xFFD8C6E9,false);
            yy+=catH+gap;
        }

        nav(c,mx,my,x,footerStart,w,"✦","Locker");
        nav(c,mx,my,x,footerStart+34,w,"▣","Configs");
        nav(c,mx,my,x,footerStart+68,w,"⚙","Settings");
    }

    private void nav(DrawContext c,int mx,int my,int x,int y,int w,String icon,String label){
        boolean hov=inside(mx,my,x,y,w,29);
        CincoDraw.bubble(c,x,y,x+w,y+29,8,hov?0xAA8B51BC:0x55462A5E,hov?0xBB1A1027:0x94100B1B);
        c.drawText(textRenderer,StarFonts.text(icon,true),x+12,y+10,0xFFC98CFF,false);
        c.drawText(textRenderer,StarFonts.text(label,hov),x+37,y+10,hov?0xFFFFFFFF:0xFFD5C2E8,false);
    }

    private void drawModules(DrawContext c,int mx,int my){
        int x=gx+leftW+18,y=gy+18,w=midW-34;
        c.drawText(textRenderer,StarFonts.text("Modules",true),x,y+4,0xFFF8F2FF,false);
        CincoDraw.bubble(c,x,y+33,x+w,y+66,9,0x8A643B88,0xBC0C0917);
        c.drawText(textRenderer,StarFonts.text("⌕",true),x+12,y+45,0xFFC58BFF,false);
        c.drawText(textRenderer,StarFonts.text(search.isEmpty()?"Search modules...":search+(searchFocused?" _":""),false),x+37,y+45,search.isEmpty()?0xFF9383A6:0xFFEDE3F2,false);

        List<ClientModule> mods=visibleModules();
        int rowH=49,start=y+79,avail=gh-111,visible=Math.max(1,avail/rowH),max=Math.max(0,mods.size()-visible);
        moduleScroll=Math.max(0,Math.min(max,moduleScroll));
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll);int yy=start+j*rowH;
            boolean sel=m==selected,hov=inside(mx,my,x,yy,w,43);
            if(sel)CincoDraw.rounded(c,x-3,yy-3,x+w+3,yy+46,11,0x493A0A61);
            CincoDraw.bubble(c,x,yy,x+w,yy+43,9,sel?0xFF9E3EE8:(hov?0x9A704298:0x5545285E),m.enabled?0xC2221328:(sel?0xC0171022:0xA30A0912));
            if(sel)c.fill(x+w-3,yy+6,x+w-1,yy+37,0xFFFF8A25);

            // module picture tile, using real Minecraft item icons whenever possible
            CincoDraw.rounded(c,x+9,yy+6,x+41,yy+38,8,m.enabled?0xFF6A2792:0xFF291A38);
            Item item=moduleItem(m.name);
            if(item!=null)c.drawItem(new ItemStack(item),x+17,yy+14);
            else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(m.name),true),x+25,yy+17,m.enabled?0xFFE5B9FF:0xFFB591CE);

            c.drawText(textRenderer,StarFonts.text(trim(m.name,22),m.enabled||sel),x+51,yy+8,m.enabled?0xFFFFB15C:0xFFF4EDF7,false);
            c.drawText(textRenderer,StarFonts.text(trim(m.description,31),false),x+51,yy+25,0xFFA899B3,false);
            drawToggle(c,x+w-67,yy+12,m.enabled);
            c.drawText(textRenderer,StarFonts.text("⚙",false),x+w-25,yy+16,0xFFB38FCE,false);
        }
        if(max>0){int rail=x+w+7;c.fill(rail,start,rail+3,start+avail-8,0x332F1A43);int thumb=Math.max(28,(avail-8)/(max+1));int py=start+(int)((avail-8-thumb)*(moduleScroll/(double)Math.max(1,max)));CincoDraw.rounded(c,rail,py,rail+3,py+thumb,2,0xFFFF8926);}
    }

    private void drawSettings(DrawContext c,int mx,int my){
        int x=rightX+15,y=gy+18,w=rightW-30;
        if(selected==null)return;
        CincoDraw.bubble(c,x,y,x+w,gy+gh-18,11,0x875C3582,0x8D070812);
        CincoDraw.rounded(c,x+w/2-10,gy+gh-155,x+w-34,gy+gh-26,64,0x20130A31);
        CincoDraw.rounded(c,x+w/2+30,gy+gh-121,x+w-68,gy+gh-42,40,0x302F0A69);

        CincoDraw.rounded(c,x+15,y+14,x+70,y+69,13,0xFF4B1769);
        Item item=moduleItem(selected.name);
        if(item!=null){c.getMatrices().pushMatrix();c.getMatrices().translate(x+27,y+26);c.getMatrices().scale(2.0f,2.0f);c.drawItem(new ItemStack(item),0,0);c.getMatrices().popMatrix();}
        else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(selected.name),true),x+42,y+36,0xFFE5C5FF);
        c.drawText(textRenderer,StarFonts.text(selected.name,true),x+86,y+21,0xFFFCF7FF,false);
        c.drawText(textRenderer,StarFonts.text(trim(selected.description,48),false),x+86,y+43,0xFFB8A7C6,false);
        drawToggle(c,x+w-60,y+24,selected.enabled);

        int tabsY=y+84,gap=4,tabW=(w-gap*4)/5;
        for(int i=0;i<TABS.length;i++){
            int tx=x+i*(tabW+gap);boolean active=i==0;
            CincoDraw.rounded(c,tx,tabsY,tx+tabW,tabsY+31,8,active?0xB52D1521:0xB00E0B1C);
            if(active)c.fill(tx+8,tabsY+29,tx+tabW-8,tabsY+31,0xFFFF871B);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],active),tx+tabW/2,tabsY+11,active?0xFFFFC17B:0xFFBCA7D0);
        }

        int contentY=tabsY+51;
        if(selected.name.equals("HUD")){drawHudSettings(c,x+14,contentY,w-28);return;}
        if(selected.name.equals("Spotify HUD")){drawSpotifySettings(c,x+14,contentY,w-28);return;}
        int yy=contentY;
        yy=settingSlider(c,x+14,yy,w-28,selected,0,selected.settingLabel,selected.setting,selected.min,selected.max);
        yy=settingSlider(c,x+14,yy,w-28,selected,1,selected.setting2Label,selected.setting2,selected.min2,selected.max2);
        yy=settingSlider(c,x+14,yy,w-28,selected,2,selected.setting3Label,selected.setting3,selected.min3,selected.max3);
        yy+=9;settingRow(c,x+14,yy,w-28,"Keybind",keyName(selected.keyCode),false);
    }

    private int settingSlider(DrawContext c,int x,int y,int w,ClientModule m,int idx,String label,double value,double min,double max){
        if(label==null||label.isBlank()||max<=min)return y;
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+9,0xFFD9CBE4,false);
        int valueX=x+(int)(w*.39),valueW=55;
        CincoDraw.bubble(c,valueX,y,valueX+valueW,y+30,7,0x554B2D67,0x9D0E0B1B);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(fmt(value),true),valueX+valueW/2,y+10,0xFFE9E0F1);
        int sx=x+(int)(w*.55),sy=y+13,sw=Math.max(58,w-(sx-x)-8);
        c.fill(sx,sy,sx+sw,sy+4,0xFF2D213D);
        double t=Math.max(0,Math.min(1,(value-min)/(max-min)));
        c.fill(sx,sy,sx+(int)(sw*t),sy+4,0xFFB52DFF);
        CincoDraw.rounded(c,sx+(int)(sw*t)-5,sy-4,sx+(int)(sw*t)+6,sy+8,6,0xFFB52EFF);
        if(sliderModule==m&&sliderIndex==idx){sliderX=sx;sliderW=sw;}
        return y+51;
    }

    private void drawHudSettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Region Map",StarHudConfig.regionMap);yy=boolSetting(c,x,yy,w,"Status Bar",StarHudConfig.statusBar);yy=boolSetting(c,x,yy,w,"Potion HUD",StarHudConfig.potionHud);yy=boolSetting(c,x,yy,w,"Module List",StarHudConfig.moduleList);yy+=4;c.drawText(textRenderer,StarFonts.text("Region Map",true),x,yy,0xFFC995FF,false);yy+=22;yy=boolSetting(c,x,yy,w,"Show Players",StarHudConfig.regionPlayers);yy=boolSetting(c,x,yy,w,"Show Storage",StarHudConfig.regionStorage);yy=boolSetting(c,x,yy,w,"Show Legend",StarHudConfig.regionLegend);yy+=4;c.drawText(textRenderer,StarFonts.text("Status Bar",true),x,yy,0xFFC995FF,false);yy+=22;yy=boolSetting(c,x,yy,w,"Clock",StarHudConfig.statusClock);yy=boolSetting(c,x,yy,w,"FPS",StarHudConfig.statusFps);yy=boolSetting(c,x,yy,w,"Ping",StarHudConfig.statusPing);boolSetting(c,x,yy,w,"Coordinates",StarHudConfig.statusCoords);
    }
    private void drawSpotifySettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Album Cover",SpotifyHudConfig.albumCover);yy=boolSetting(c,x,yy,w,"Song Name",SpotifyHudConfig.song);yy=boolSetting(c,x,yy,w,"Artist",SpotifyHudConfig.artist);yy=boolSetting(c,x,yy,w,"Lyrics",SpotifyHudConfig.lyrics);yy=boolSetting(c,x,yy,w,"Playback Controls",SpotifyHudConfig.controls);yy=boolSetting(c,x,yy,w,"Progress / Seek",SpotifyHudConfig.progress);yy+=4;settingRow(c,x,yy,w,"Lyric Lines",Integer.toString(SpotifyHudConfig.lyricLines),false);yy+=34;settingRow(c,x,yy,w,"Previous Key",keyName(SpotifyHudConfig.previousKey),"prev".equals(captureSpotifyKey));yy+=34;settingRow(c,x,yy,w,"Play/Pause Key",keyName(SpotifyHudConfig.playPauseKey),"play".equals(captureSpotifyKey));yy+=34;settingRow(c,x,yy,w,"Next Key",keyName(SpotifyHudConfig.nextKey),"next".equals(captureSpotifyKey));
    }

    private int boolSetting(DrawContext c,int x,int y,int w,String label,boolean on){c.drawText(textRenderer,StarFonts.text(label,false),x,y+8,0xFFDCCFE7,false);drawToggle(c,x+w-48,y+3,on);return y+34;}
    private void settingRow(DrawContext c,int x,int y,int w,String label,String value,boolean active){c.drawText(textRenderer,StarFonts.text(label,false),x,y+9,0xFFDCCFE7,false);int boxW=Math.max(68,textRenderer.getWidth(value)+20);CincoDraw.bubble(c,x+w-boxW,y+1,x+w,y+29,7,active?0xFFFF861B:0x55502E6C,active?0xB52A1421:0x990E0B19);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(value,true),x+w-boxW/2,y+11,active?0xFFFFC17A:0xFFE5D7EF);}
    private void drawToggle(DrawContext c,int x,int y,boolean on){if(on)CincoDraw.rounded(c,x-3,y-3,x+37,y+23,12,0x33FF8A20);CincoDraw.rounded(c,x,y,x+34,y+20,10,on?0xFFFF8517:0xFF3B304E);CincoDraw.rounded(c,on?x+18:x+2,y+2,on?x+32:x+16,y+18,8,on?0xFFFFFFFF:0xFFD6CEE4);}

    private Item moduleItem(String n){
        String s=n.toLowerCase(Locale.ROOT);
        if(s.contains("crystal"))return Items.END_CRYSTAL;
        if(s.contains("anchor"))return Items.RESPAWN_ANCHOR;
        if(s.contains("totem"))return Items.TOTEM_OF_UNDYING;
        if(s.contains("pearl"))return Items.ENDER_PEARL;
        if(s.contains("web"))return Items.COBWEB;
        if(s.contains("aura")||s.contains("sword"))return Items.DIAMOND_SWORD;
        if(s.contains("regear")||s.contains("armor"))return Items.NETHERITE_CHESTPLATE;
        if(s.contains("eat")||s.contains("apple"))return Items.ENCHANTED_GOLDEN_APPLE;
        if(s.contains("xp"))return Items.EXPERIENCE_BOTTLE;
        if(s.contains("elytra")||s.contains("flight"))return Items.ELYTRA;
        if(s.contains("storage")||s.contains("container"))return Items.ENDER_CHEST;
        if(s.contains("spawner"))return Items.SPAWNER;
        if(s.contains("bed"))return Items.RED_BED;
        if(s.contains("obsidian")||s.contains("surround"))return Items.OBSIDIAN;
        if(s.contains("torch")||s.contains("bright"))return Items.TORCH;
        return null;
    }
    private String moduleGlyph(String n){String s=n.toLowerCase(Locale.ROOT);if(s.contains("hud"))return"▦";if(s.contains("esp")||s.contains("visual"))return"◉";if(s.contains("speed"))return"➜";if(s.contains("reach"))return"↔";if(s.contains("sky"))return"✦";return"◆";}

    private List<ClientModule> visibleModules(){List<ClientModule> out=new ArrayList<>();String q=search.toLowerCase(Locale.ROOT);for(ClientModule m:CincoClient.MODULES){if(hidden(m))continue;if(!q.isBlank()&&!((m.name+" "+m.description).toLowerCase(Locale.ROOT).contains(q)))continue;if(q.isBlank()&&!matchesCategory(m))continue;out.add(m);}return out;}
    private boolean hidden(ClientModule m){return m.name.equals("Animations")||m.name.equals("Custom Accessory")||m.name.equals("Player Changer")||m.name.equals("GTA Skin Changer")||m.name.equals("Interactive Spotify")||m.name.equals("Region Map")||m.name.equals("Crystal")||m.name.equals("Mace");}
    private boolean matchesCategory(ClientModule m){return switch(category){case 0->m.category==ClientModule.Category.COMBAT;case 1->m.category==ClientModule.Category.MOVEMENT;case 2->m.name.contains("Player")||m.name.contains("Skin")||m.name.contains("Totem");case 3->m.category==ClientModule.Category.RENDER&&!m.name.equals("HUD")&&!m.name.equals("Spotify HUD");case 4->m.category==ClientModule.Category.WORLD;case 5->m.category==ClientModule.Category.UTILITY&&isExploitish(m.name);case 6->m.category==ClientModule.Category.RENDER&&(m.name.equals("HUD")||m.name.equals("Spotify HUD")||m.name.contains("ESP")||m.name.contains("Visual"));default->m.category==ClientModule.Category.UTILITY||m.category==ClientModule.Category.PRACTICE||m.category==ClientModule.Category.SETTINGS;};}
    private boolean isExploitish(String n){String s=n.toLowerCase(Locale.ROOT);return s.contains("velocity")||s.contains("reach")||s.contains("flight")||s.contains("speed")||s.contains("free cam")||s.contains("fast");}

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;
        if(inside(mx,my,gx+gw-31,gy+7,27,27)){if(client!=null)client.setScreen(null);return true;}
        int sx=gx+20,sw=leftW-40,catStart=gy+126,footerStart=gy+gh-115,gap=5,catH=Math.max(28,Math.min(37,(footerStart-catStart-gap*7)/8));
        int yy=catStart;for(int i=0;i<CATS.length;i++){if(inside(mx,my,sx,yy,sw,catH)){category=i;search="";moduleScroll=0;selected=null;ensureSelected();return true;}yy+=catH+gap;}
        if(inside(mx,my,sx,footerStart,sw,29)){if(client!=null)client.setScreen(new LockerScreen());return true;}
        if(inside(mx,my,sx,footerStart+34,sw,29)){if(client!=null)client.setScreen(new KeybindsScreen());return true;}
        if(inside(mx,my,sx,footerStart+68,sw,29)){if(client!=null)client.setScreen(new ClientSettingsScreen());return true;}

        int x=gx+leftW+18,y=gy+18,w=midW-34;
        if(inside(mx,my,x,y+33,w,33)){searchFocused=true;return true;}searchFocused=false;
        List<ClientModule> mods=visibleModules();int rowH=49,start=y+79,avail=gh-111,visible=Math.max(1,avail/rowH);
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){ClientModule m=mods.get(j+moduleScroll);int ry=start+j*rowH;if(!inside(mx,my,x,ry,w,43))continue;selected=m;if(mx>=x+w-75&&mx<=x+w-30){m.enabled=!m.enabled;ConfigStore.save();}return true;}

        int rx=rightX+15,ry=gy+18,rw=rightW-30;
        if(selected!=null&&inside(mx,my,rx+rw-66,ry+18,54,35)){selected.enabled=!selected.enabled;ConfigStore.save();return true;}
        int contentY=ry+84+51;
        if(selected!=null&&selected.name.equals("HUD"))return clickHud(mx,my,rx+29,contentY,rw-58);
        if(selected!=null&&selected.name.equals("Spotify HUD"))return clickSpotify(mx,my,rx+29,contentY,rw-58);
        if(selected!=null){int sy=contentY;for(int idx=0;idx<3;idx++){String label=idx==0?selected.settingLabel:idx==1?selected.setting2Label:selected.setting3Label;double min=idx==0?selected.min:idx==1?selected.min2:selected.min3,max=idx==0?selected.max:idx==1?selected.max2:selected.max3;if(label==null||label.isBlank()||max<=min)continue;int sx2=rx+29+(int)((rw-58)*.55),sw2=Math.max(58,(rw-58)-(int)((rw-58)*.55)-8);if(inside(mx,my,sx2,sy-2,sw2,34)){sliderModule=selected;sliderIndex=idx;sliderX=sx2;sliderW=sw2;setSlider(mx);return true;}sy+=51;}}
        return false;
    }
    private boolean clickHud(double mx,double my,int x,int y,int w){String[] ids={"region","status","potions","modules","players","storage","legend","clock","fps","ping","coords"};int[] ys={y,y+34,y+68,y+102,y+162,y+196,y+230,y+290,y+324,y+358,y+392};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,ys[i],w,30)){StarHudConfig.toggle(ids[i]);return true;}return false;}
    private boolean clickSpotify(double mx,double my,int x,int y,int w){String[] ids={"cover","song","artist","lyrics","controls","progress"};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,y+i*34,w,30)){SpotifyHudConfig.toggle(ids[i]);return true;}int yy=y+208;if(inside(mx,my,x,yy,w,30)){SpotifyHudConfig.lyricLines=SpotifyHudConfig.lyricLines>=6?0:SpotifyHudConfig.lyricLines+1;SpotifyHudConfig.save();return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="prev";return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="play";return true;}yy+=34;if(inside(mx,my,x,yy,w,30)){captureSpotifyKey="next";return true;}return false;}

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&sliderModule!=null){setSlider(mx);return true;}return false;}
    @Override public boolean mouseReleased(double mx,double my,int button){if(sliderModule!=null){sliderModule=null;sliderIndex=-1;ConfigStore.save();return true;}return false;}
    private void setSlider(double mx){if(sliderModule==null||sliderW<=0)return;double t=Math.max(0,Math.min(1,(mx-sliderX)/sliderW));double min=sliderIndex==0?sliderModule.min:sliderIndex==1?sliderModule.min2:sliderModule.min3,max=sliderIndex==0?sliderModule.max:sliderIndex==1?sliderModule.max2:sliderModule.max3,step=sliderIndex==0?sliderModule.step:sliderIndex==1?sliderModule.step2:sliderModule.step3;double v=min+(max-min)*t;if(step>0)v=Math.round(v/step)*step;if(sliderIndex==0)sliderModule.setting=v;else if(sliderIndex==1)sliderModule.setting2=v;else sliderModule.setting3=v;}
    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){int x=gx+leftW+18,w=midW-34;if(inside(mx,my,x,gy+92,w,gh-108)){List<ClientModule> mods=visibleModules();int visible=Math.max(1,(gh-111)/49),max=Math.max(0,mods.size()-visible);moduleScroll=Math.max(0,Math.min(max,moduleScroll+(vertical<0?1:-1)));return true;}return false;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(captureSpotifyKey!=null){if("prev".equals(captureSpotifyKey))SpotifyHudConfig.previousKey=keyCode;else if("play".equals(captureSpotifyKey))SpotifyHudConfig.playPauseKey=keyCode;else SpotifyHudConfig.nextKey=keyCode;captureSpotifyKey=null;SpotifyHudConfig.save();return true;}if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){if(client!=null)client.setScreen(null);return true;}if(searchFocused){if(keyCode==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty())search=search.substring(0,search.length()-1);else if(keyCode==GLFW.GLFW_KEY_ENTER)searchFocused=false;return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&chr>=32&&chr!=127&&search.length()<40){search+=chr;moduleScroll=0;return true;}return super.charTyped(chr,modifiers);}

    private String keyName(int key){if(key<0)return"NONE";String n=GLFW.glfwGetKeyName(key,0);return n==null?Integer.toString(key):n.toUpperCase(Locale.ROOT);}
    private String fmt(double v){return Math.abs(v-Math.rint(v))<.0001?Integer.toString((int)Math.rint(v)):String.format(Locale.ROOT,"%.1f",v);}
    private String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
