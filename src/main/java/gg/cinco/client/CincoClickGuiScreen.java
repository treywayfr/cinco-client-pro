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

/** Star Client ClickGUI rebuilt directly from the approved galaxy reference layout. */
public final class CincoClickGuiScreen extends StarLegacyScreen {
    private static final String[] CATS={"Combat","Movement","Player","Visuals","World","Exploit","Render","Misc"};
    private static final String[] TABS={"General","Targeting","Placement","Breaking","Misc"};

    private static final int PINK=0xFFFF55D8;
    private static final int PINK_SOFT=0xFFFF8BE8;
    private static final int PURPLE=0xFFB873FF;
    private static final int TEXT=0xFFF8F4FF;
    private static final int MUTED=0xFFB9ABC9;
    private static final int GLASS=0x96100A20;
    private static final int GLASS_SOFT=0x5A110A22;
    private static final int LINE=0x668A6EAC;

    private int category,moduleScroll;
    private ClientModule selected;
    private String search="";
    private boolean searchFocused;
    private ClientModule sliderModule;
    private int sliderIndex=-1,sliderX,sliderW;
    private String captureSpotifyKey;

    // Stored render geometry; click handling uses these exact values.
    private int gx,gy,gw,gh,leftW,midW,rightX,rightW;
    private int sideX,sideW,catStart,catH,footerStart;
    private int modX,modY,modW,searchY,rowStart,rowH,rowCardH;
    private int setX,setY,setW,tabsY,contentY;

    public CincoClickGuiScreen(){super(Text.literal("Star Client"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        computeLayout();
        StarBackground.render(c,width,height,delta);

        // Reference shell: one soft bloom, one thin bright rim, dark translucent glass.
        CincoDraw.softShadow(c,gx,gy,gx+gw,gy+gh,18,0xFF39D9,46);
        CincoDraw.rounded(c,gx-1,gy-1,gx+gw+1,gy+gh+1,18,0xB7D08CFF);
        CincoDraw.rounded(c,gx,gy,gx+gw,gy+gh,17,0xA2070414);
        CincoDraw.rounded(c,gx+2,gy+2,gx+gw-2,gy+gh-2,15,0x2A1A0D2C);

        // Thin separators from the reference. Keep them subtle so the galaxy still reads through.
        c.fill(gx+leftW,gy+18,gx+leftW+1,gy+gh-14,0x42785F94);
        c.fill(rightX,gy+18,rightX+1,gy+gh-14,0x42785F94);

        ensureSelected();
        drawSidebar(c,mx,my);
        drawModules(c,mx,my);
        drawSettings(c,mx,my);

        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("×",true),gx+gw-14,gy+9,0xFFF7F0FF);
    }

    private void computeLayout(){
        // Approved reference is ~90% screen width and ~88% height.
        gw=Math.max(710,Math.min(width-24,(int)Math.round(width*.900)));
        gh=Math.max(410,Math.min(height-24,(int)Math.round(height*.880)));
        gx=(width-gw)/2; gy=(height-gh)/2;
        leftW=(int)Math.round(gw*.222);
        midW=(int)Math.round(gw*.360);
        rightX=gx+leftW+midW;
        rightW=gx+gw-rightX;

        sideX=gx+14; sideW=leftW-27;
        catStart=gy+73;
        footerStart=gy+gh-105;
        catH=Math.max(25,Math.min(30,(footerStart-catStart-7*5)/8));

        modX=gx+leftW+16; modY=gy+17; modW=midW-29;
        searchY=modY+31;
        rowStart=searchY+41;
        rowH=40; rowCardH=35;

        setX=rightX+12; setY=gy+16; setW=rightW-23;
        tabsY=setY+78;
        contentY=tabsY+45;
    }

    private void ensureSelected(){
        if(selected!=null&&!hidden(selected)&&matchesCategory(selected))return;
        List<ClientModule> list=visibleModules();
        if(category==0){
            ClientModule crystal=CincoClient.findModule("Auto Crystal");
            if(crystal!=null&&!hidden(crystal)){selected=crystal;return;}
        }
        selected=list.isEmpty()?null:list.get(0);
    }

    private void drawSidebar(DrawContext c,int mx,int my){
        int logoY=gy+14;

        // Brand line exactly like the reference: glowing star mark + STAR CLIENT on one line.
        CincoDraw.rounded(c,sideX+2,logoY+1,sideX+39,logoY+38,19,0x28FF55D8);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✦",true),sideX+21,logoY+14,PINK);
        c.drawText(textRenderer,StarFonts.text("STAR",true),sideX+47,logoY+14,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("CLIENT",true),sideX+81,logoY+14,PINK_SOFT,false);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("✧     ✧     ✦",true),sideX+sideW/2,logoY+48,0xFFD8A7F7);

        int yy=catStart;
        for(int i=0;i<CATS.length;i++){
            boolean sel=i==category, hov=inside(mx,my,sideX,yy,sideW,catH);
            int border=sel?0xDFFF5AD9:(hov?0x8FC78FEA:0x5A82689E);
            int fill=sel?0x76360D49:(hov?0x4D1A102C:0x38100A1D);
            if(sel)CincoDraw.softShadow(c,sideX,yy,sideX+sideW,yy+catH,catH/2,0xFF4AD8,38);
            CincoDraw.bubble(c,sideX,yy,sideX+sideW,yy+catH,catH/2,border,fill);

            String icon=switch(i){case 0->"⚔";case 1->"➜";case 2->"●";case 3->"◉";case 4->"◆";case 5->"⚙";case 6->"▣";default->"•••";};
            c.drawText(textRenderer,StarFonts.text(icon,true),sideX+12,yy+catH/2-4,sel?0xFFFFFFFF:0xFFE2C0FA,false);
            c.drawText(textRenderer,StarFonts.text(CATS[i],sel),sideX+37,yy+catH/2-4,sel?0xFFFFFFFF:0xFFEADFF4,false);
            c.drawText(textRenderer,StarFonts.text("›",true),sideX+sideW-16,yy+catH/2-5,sel?0xFFFFFFFF:0xFFAFA0BF,false);
            yy+=catH+5;
        }

        nav(c,mx,my,footerStart,"▣","Locker");
        nav(c,mx,my,footerStart+34,"▤","Configs");
        nav(c,mx,my,footerStart+68,"⚙","Settings");
    }

    private void nav(DrawContext c,int mx,int my,int y,String icon,String label){
        int h=28;boolean hov=inside(mx,my,sideX,y,sideW,h);
        CincoDraw.bubble(c,sideX,y,sideX+sideW,y+h,h/2,hov?0x86C98EF0:0x55816A9B,hov?0x4A1B102D:0x36100A1D);
        c.drawText(textRenderer,StarFonts.text(icon,true),sideX+12,y+10,0xFFE3C5F8,false);
        c.drawText(textRenderer,StarFonts.text(label,hov),sideX+37,y+10,hov?0xFFFFFFFF:0xFFE8DCF1,false);
        c.drawText(textRenderer,StarFonts.text("›",true),sideX+sideW-16,y+9,0xFFAD9DBB,false);
    }

    private void drawModules(DrawContext c,int mx,int my){
        c.drawText(textRenderer,StarFonts.text("Modules",true),modX,modY+2,TEXT,false);
        CincoDraw.bubble(c,modX+modW-29,modY-2,modX+modW,modY+24,12,0x5F735A8C,0x48120C21);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("☷",true),modX+modW-15,modY+6,0xFFF2E5FA);

        CincoDraw.bubble(c,modX,searchY,modX+modW,searchY+29,15,0x708166A0,0x59100A1E);
        c.drawText(textRenderer,StarFonts.text("⌕",true),modX+12,searchY+10,0xFFE1C3F7,false);
        c.drawText(textRenderer,StarFonts.text(search.isEmpty()?"Search modules...":search+(searchFocused?" _":""),false),modX+36,searchY+10,search.isEmpty()?0xFFAD9CBC:0xFFF4ECF8,false);

        List<ClientModule> mods=visibleModules();
        int avail=gy+gh-17-rowStart;
        int visible=Math.max(1,avail/rowH),max=Math.max(0,mods.size()-visible);
        moduleScroll=Math.max(0,Math.min(max,moduleScroll));

        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll);int yy=rowStart+j*rowH;
            boolean sel=m==selected,hov=inside(mx,my,modX,yy,modW,rowCardH);
            if(sel)CincoDraw.softShadow(c,modX,yy,modX+modW,yy+rowCardH,rowCardH/2,0xFF48D4,46);
            int border=sel?0xE5FF62DD:(hov?0x8BBF8CDC:0x4E79608F);
            int fill=sel?0x7A2B0A44:(hov?0x4B170D29:0x40100A1C);
            CincoDraw.bubble(c,modX,yy,modX+modW,yy+rowCardH,rowCardH/2,border,fill);
            if(sel)drawSelectedNebula(c,modX,yy,modW,rowCardH);

            int d=29,ix=modX+6,iy=yy+3;
            CincoDraw.rounded(c,ix,iy,ix+d,iy+d,d/2,sel?0x56FF55D8:0x4B633B80);
            CincoDraw.rounded(c,ix+2,iy+2,ix+d-2,iy+d-2,(d-4)/2,0xA1100A1E);
            Item item=moduleItem(m.name);
            if(item!=null)c.drawItem(new ItemStack(item),ix+7,iy+7);
            else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(m.name),true),ix+d/2,iy+11,sel?0xFFFFFFFF:0xFFE1C4F6);

            int textX=modX+41;
            c.drawText(textRenderer,StarFonts.text(trim(m.name,22),m.enabled||sel),textX,yy+6,sel?PINK_SOFT:(m.enabled?0xFFFFD35E:TEXT),false);
            c.drawText(textRenderer,StarFonts.text(trim(m.description,31),false),textX,yy+21,MUTED,false);
            drawToggle(c,modX+modW-58,yy+8,m.enabled);
            c.drawText(textRenderer,StarFonts.text("⚙",false),modX+modW-20,yy+12,0xFFD8C7E4,false);
        }

        if(max>0){
            int rail=modX+modW+5;
            CincoDraw.rounded(c,rail,rowStart,rail+3,rowStart+avail-3,2,0x38352146);
            int thumb=Math.max(22,(avail-3)/(max+1));
            int py=rowStart+(int)((avail-3-thumb)*(moduleScroll/(double)Math.max(1,max)));
            CincoDraw.rounded(c,rail,py,rail+3,py+thumb,2,PINK);
        }
    }

    private void drawSelectedNebula(DrawContext c,int x,int y,int w,int h){
        // Sparse light points from the approved selected Auto Crystal row; no moving scanline noise.
        int[][] pts={{22,8},{74,25},{128,7},{171,23},{213,10},{252,26},{288,8}};
        for(int[] p:pts){int px=x+Math.floorMod(p[0],Math.max(1,w-8)),py=y+Math.floorMod(p[1],Math.max(1,h-5));c.fill(px,py,px+1,py+1,0xB8FFD7FF);}
    }

    private void drawSettings(DrawContext c,int mx,int my){
        if(selected==null)return;
        CincoDraw.bubble(c,setX,setY,setX+setW,gy+gh-15,17,0x75947AB3,0x6A0D081A);

        // Lower galaxy field remains transparent so the actual cached galaxy shows through.
        int fieldY=tabsY+35;
        CincoDraw.rounded(c,setX+6,fieldY,setX+setW-6,gy+gh-22,15,0x24120727);
        drawPlanetField(c);

        // Header icon circle.
        int ix=setX+12,iy=setY+10,d=49;
        CincoDraw.rounded(c,ix,iy,ix+d,iy+d,d/2,0x50FF5ADB);
        CincoDraw.rounded(c,ix+3,iy+3,ix+d-3,iy+d-3,(d-6)/2,0x8C100A20);
        Item item=moduleItem(selected.name);
        if(item!=null){c.getMatrices().pushMatrix();c.getMatrices().translate(ix+9,iy+9);c.getMatrices().scale(1.9f,1.9f);c.drawItem(new ItemStack(item),0,0);c.getMatrices().popMatrix();}
        else c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(moduleGlyph(selected.name),true),ix+d/2,iy+20,0xFFFFFFFF);

        c.drawText(textRenderer,StarFonts.text(selected.name,true),setX+73,setY+19,TEXT,false);
        c.drawText(textRenderer,StarFonts.text(trim(selected.description,48),false),setX+73,setY+37,MUTED,false);
        drawToggle(c,setX+setW-55,setY+18,selected.enabled);

        int gap=4,tabW=(setW-gap*4)/5;
        for(int i=0;i<TABS.length;i++){
            int tx=setX+i*(tabW+gap);boolean active=i==0;
            if(active)CincoDraw.softShadow(c,tx,tabsY,tx+tabW,tabsY+27,13,0xFF48D8,36);
            CincoDraw.bubble(c,tx,tabsY,tx+tabW,tabsY+27,13,active?0xDFFF66DF:0x55725A89,active?0x74310C4A:0x38100A1C);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],active),tx+tabW/2,tabsY+9,active?0xFFFFFFFF:0xFFCAB9D6);
        }

        if(selected.name.equals("HUD")){drawHudSettings(c,setX+14,contentY,setW-28);return;}
        if(selected.name.equals("Spotify HUD")){drawSpotifySettings(c,setX+14,contentY,setW-28);return;}

        int yy=contentY;
        yy=settingSlider(c,setX+14,yy,setW-28,selected,0,selected.settingLabel,selected.setting,selected.min,selected.max);
        yy=settingSlider(c,setX+14,yy,setW-28,selected,1,selected.setting2Label,selected.setting2,selected.min2,selected.max2);
        yy=settingSlider(c,setX+14,yy,setW-28,selected,2,selected.setting3Label,selected.setting3,selected.min3,selected.max3);
        yy+=6;settingRow(c,setX+14,yy,setW-28,"Keybind",keyName(selected.keyCode),false);
    }

    private void drawPlanetField(DrawContext c){
        int cx=setX+setW-92,cy=gy+gh-73,r=54;
        CincoDraw.rounded(c,cx-r-3,cy-r-3,cx+r+3,cy+r+3,r+3,0x24FF53D4);
        CincoDraw.rounded(c,cx-r,cy-r,cx+r,cy+r,r,0xA0090415);
        CincoDraw.rounded(c,cx-r+9,cy-r+8,cx+r-17,cy+r-18,r-13,0x8A241044);
        int sx=setX+setW-166,sy=gy+gh-43,sr=23;
        CincoDraw.rounded(c,sx-sr,sy-sr,sx+sr,sy+sr,sr,0x8A100725);
        for(int i=0;i<13;i++){int px=setX+18+Math.floorMod(i*61,setW-36);int py=tabsY+48+Math.floorMod(i*31,Math.max(20,gh-180));c.fill(px,py,px+1,py+1,0x7CFFD8FF);}
    }

    private int settingSlider(DrawContext c,int x,int y,int w,ClientModule m,int idx,String label,double value,double min,double max){
        if(label==null||label.isBlank()||max<=min)return y;
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+9,0xFFF0E8F5,false);
        int valueX=x+(int)(w*.43),valueW=47;
        CincoDraw.bubble(c,valueX,y,valueX+valueW,y+27,13,0x60806999,0x66100A1C);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(fmt(value),true),valueX+valueW/2,y+9,0xFFFFFFFF);
        int sx=x+(int)(w*.57),sy=y+12,sw=Math.max(50,w-(sx-x)-7);
        CincoDraw.rounded(c,sx,sy,sx+sw,sy+4,2,0x5C52415F);
        double t=Math.max(0,Math.min(1,(value-min)/(max-min)));
        CincoDraw.rounded(c,sx,sy,sx+(int)(sw*t),sy+4,2,PURPLE);
        CincoDraw.rounded(c,sx+(int)(sw*t)-5,sy-4,sx+(int)(sw*t)+6,sy+9,7,PINK);
        if(sliderModule==m&&sliderIndex==idx){sliderX=sx;sliderW=sw;}
        return y+45;
    }

    private void drawHudSettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Region Map",StarHudConfig.regionMap);yy=boolSetting(c,x,yy,w,"Status Bar",StarHudConfig.statusBar);yy=boolSetting(c,x,yy,w,"Potion HUD",StarHudConfig.potionHud);yy=boolSetting(c,x,yy,w,"Module List",StarHudConfig.moduleList);yy+=3;c.drawText(textRenderer,StarFonts.text("Region Map",true),x,yy,PINK_SOFT,false);yy+=19;yy=boolSetting(c,x,yy,w,"Show Players",StarHudConfig.regionPlayers);yy=boolSetting(c,x,yy,w,"Show Storage",StarHudConfig.regionStorage);yy=boolSetting(c,x,yy,w,"Show Legend",StarHudConfig.regionLegend);yy+=3;c.drawText(textRenderer,StarFonts.text("Status Bar",true),x,yy,PINK_SOFT,false);yy+=19;yy=boolSetting(c,x,yy,w,"Clock",StarHudConfig.statusClock);yy=boolSetting(c,x,yy,w,"FPS",StarHudConfig.statusFps);yy=boolSetting(c,x,yy,w,"Ping",StarHudConfig.statusPing);boolSetting(c,x,yy,w,"Coordinates",StarHudConfig.statusCoords);
    }

    private void drawSpotifySettings(DrawContext c,int x,int y,int w){
        int yy=y;yy=boolSetting(c,x,yy,w,"Album Cover",SpotifyHudConfig.albumCover);yy=boolSetting(c,x,yy,w,"Song Name",SpotifyHudConfig.song);yy=boolSetting(c,x,yy,w,"Artist",SpotifyHudConfig.artist);yy=boolSetting(c,x,yy,w,"Lyrics",SpotifyHudConfig.lyrics);yy=boolSetting(c,x,yy,w,"Playback Controls",SpotifyHudConfig.controls);yy=boolSetting(c,x,yy,w,"Progress / Seek",SpotifyHudConfig.progress);yy+=3;settingRow(c,x,yy,w,"Lyric Lines",Integer.toString(SpotifyHudConfig.lyricLines),false);yy+=31;settingRow(c,x,yy,w,"Previous Key",keyName(SpotifyHudConfig.previousKey),"prev".equals(captureSpotifyKey));yy+=31;settingRow(c,x,yy,w,"Play/Pause Key",keyName(SpotifyHudConfig.playPauseKey),"play".equals(captureSpotifyKey));yy+=31;settingRow(c,x,yy,w,"Next Key",keyName(SpotifyHudConfig.nextKey),"next".equals(captureSpotifyKey));
    }

    private int boolSetting(DrawContext c,int x,int y,int w,String label,boolean on){
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+7,0xFFF0E8F5,false);drawToggle(c,x+w-45,y+2,on);return y+30;
    }

    private void settingRow(DrawContext c,int x,int y,int w,String label,String value,boolean active){
        c.drawText(textRenderer,StarFonts.text(label,false),x,y+8,0xFFF0E8F5,false);
        int boxW=Math.max(64,textRenderer.getWidth(value)+18);
        CincoDraw.bubble(c,x+w-boxW,y,x+w,y+27,13,active?0xC8FF5ADA:0x5D785C91,active?0x78320D49:0x5A100A1C);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(value,true),x+w-boxW/2,y+9,active?0xFFFFFFFF:0xFFE7DAEE);
    }

    private void drawToggle(DrawContext c,int x,int y,boolean on){
        if(on)CincoDraw.softShadow(c,x,y,x+32,y+18,9,0xFF48D2,36);
        CincoDraw.rounded(c,x,y,x+32,y+18,9,on?PINK:0xFF40354F);
        CincoDraw.rounded(c,on?x+17:x+2,y+2,on?x+30:x+15,y+16,7,0xFFFFFFFF);
    }

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

    private String moduleGlyph(String n){
        String s=n.toLowerCase(Locale.ROOT);
        if(s.contains("hud"))return"▦";if(s.contains("esp")||s.contains("visual"))return"◉";if(s.contains("speed"))return"➜";if(s.contains("reach"))return"↔";if(s.contains("sky"))return"✦";return"◆";
    }

    private List<ClientModule> visibleModules(){
        List<ClientModule> out=new ArrayList<>();String q=search.toLowerCase(Locale.ROOT);
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

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;computeLayout();
        if(inside(mx,my,gx+gw-27,gy+3,24,24)){if(client!=null)client.setScreen(null);return true;}

        int yy=catStart;
        for(int i=0;i<CATS.length;i++){
            if(inside(mx,my,sideX,yy,sideW,catH)){category=i;search="";moduleScroll=0;selected=null;ensureSelected();return true;}
            yy+=catH+5;
        }
        if(inside(mx,my,sideX,footerStart,sideW,28)){if(client!=null)client.setScreen(new LockerScreen());return true;}
        if(inside(mx,my,sideX,footerStart+34,sideW,28)){if(client!=null)client.setScreen(new KeybindsScreen());return true;}
        if(inside(mx,my,sideX,footerStart+68,sideW,28)){if(client!=null)client.setScreen(new ClientSettingsScreen());return true;}

        if(inside(mx,my,modX,searchY,modW,29)){searchFocused=true;return true;}searchFocused=false;
        List<ClientModule> mods=visibleModules();
        int avail=gy+gh-17-rowStart,visible=Math.max(1,avail/rowH);
        for(int j=0;j<visible&&j+moduleScroll<mods.size();j++){
            ClientModule m=mods.get(j+moduleScroll);int ry=rowStart+j*rowH;
            if(!inside(mx,my,modX,ry,modW,rowCardH))continue;
            selected=m;
            if(mx>=modX+modW-65&&mx<=modX+modW-25){m.enabled=!m.enabled;ConfigStore.save();}
            return true;
        }

        if(selected!=null&&inside(mx,my,setX+setW-62,setY+12,56,36)){selected.enabled=!selected.enabled;ConfigStore.save();return true;}
        if(selected!=null&&selected.name.equals("HUD"))return clickHud(mx,my,setX+14,contentY,setW-28);
        if(selected!=null&&selected.name.equals("Spotify HUD"))return clickSpotify(mx,my,setX+14,contentY,setW-28);
        if(selected!=null){
            int sy=contentY;
            for(int idx=0;idx<3;idx++){
                String label=idx==0?selected.settingLabel:idx==1?selected.setting2Label:selected.setting3Label;
                double min=idx==0?selected.min:idx==1?selected.min2:selected.min3,max=idx==0?selected.max:idx==1?selected.max2:selected.max3;
                if(label==null||label.isBlank()||max<=min)continue;
                int sx2=setX+14+(int)((setW-28)*.57),sw2=Math.max(50,(setW-28)-(int)((setW-28)*.57)-7);
                if(inside(mx,my,sx2,sy-2,sw2,32)){sliderModule=selected;sliderIndex=idx;sliderX=sx2;sliderW=sw2;setSlider(mx);return true;}
                sy+=45;
            }
        }
        return false;
    }

    private boolean clickHud(double mx,double my,int x,int y,int w){String[] ids={"region","status","potions","modules","players","storage","legend","clock","fps","ping","coords"};int[] ys={y,y+30,y+60,y+90,y+145,y+175,y+205,y+260,y+290,y+320,y+350};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,ys[i],w,27)){StarHudConfig.toggle(ids[i]);return true;}return false;}
    private boolean clickSpotify(double mx,double my,int x,int y,int w){String[] ids={"cover","song","artist","lyrics","controls","progress"};for(int i=0;i<ids.length;i++)if(inside(mx,my,x,y+i*30,w,27)){SpotifyHudConfig.toggle(ids[i]);return true;}int yy=y+183;if(inside(mx,my,x,yy,w,27)){SpotifyHudConfig.lyricLines=SpotifyHudConfig.lyricLines>=6?0:SpotifyHudConfig.lyricLines+1;SpotifyHudConfig.save();return true;}yy+=31;if(inside(mx,my,x,yy,w,27)){captureSpotifyKey="prev";return true;}yy+=31;if(inside(mx,my,x,yy,w,27)){captureSpotifyKey="play";return true;}yy+=31;if(inside(mx,my,x,yy,w,27)){captureSpotifyKey="next";return true;}return false;}

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&sliderModule!=null){setSlider(mx);return true;}return false;}
    @Override public boolean mouseReleased(double mx,double my,int button){if(sliderModule!=null){sliderModule=null;sliderIndex=-1;ConfigStore.save();return true;}return false;}
    private void setSlider(double mx){if(sliderModule==null||sliderW<=0)return;double t=Math.max(0,Math.min(1,(mx-sliderX)/sliderW));double min=sliderIndex==0?sliderModule.min:sliderIndex==1?sliderModule.min2:sliderModule.min3,max=sliderIndex==0?sliderModule.max:sliderIndex==1?sliderModule.max2:sliderModule.max3,step=sliderIndex==0?sliderModule.step:sliderIndex==1?sliderModule.step2:sliderModule.step3;double v=min+(max-min)*t;if(step>0)v=Math.round(v/step)*step;if(sliderIndex==0)sliderModule.setting=v;else if(sliderIndex==1)sliderModule.setting2=v;else sliderModule.setting3=v;}

    @Override public boolean mouseScrolled(double mx,double my,double horizontal,double vertical){computeLayout();if(inside(mx,my,modX,rowStart,modW,gy+gh-17-rowStart)){List<ClientModule> mods=visibleModules();int visible=Math.max(1,(gy+gh-17-rowStart)/rowH),max=Math.max(0,mods.size()-visible);moduleScroll=Math.max(0,Math.min(max,moduleScroll+(vertical<0?1:-1)));return true;}return false;}
    @Override public boolean keyPressed(int keyCode,int scanCode,int modifiers){if(captureSpotifyKey!=null){if("prev".equals(captureSpotifyKey))SpotifyHudConfig.previousKey=keyCode;else if("play".equals(captureSpotifyKey))SpotifyHudConfig.playPauseKey=keyCode;else SpotifyHudConfig.nextKey=keyCode;captureSpotifyKey=null;SpotifyHudConfig.save();return true;}if(keyCode==GLFW.GLFW_KEY_ESCAPE||keyCode==GLFW.GLFW_KEY_RIGHT_SHIFT){if(client!=null)client.setScreen(null);return true;}if(searchFocused){if(keyCode==GLFW.GLFW_KEY_BACKSPACE&&!search.isEmpty())search=search.substring(0,search.length()-1);else if(keyCode==GLFW.GLFW_KEY_ENTER)searchFocused=false;return true;}return super.keyPressed(keyCode,scanCode,modifiers);}
    @Override public boolean charTyped(char chr,int modifiers){if(searchFocused&&chr>=32&&chr!=127&&search.length()<40){search+=chr;moduleScroll=0;return true;}return super.charTyped(chr,modifiers);}

    private String keyName(int key){if(key<0)return"NONE";String n=GLFW.glfwGetKeyName(key,0);return n==null?Integer.toString(key):n.toUpperCase(Locale.ROOT);}
    private String fmt(double v){return Math.abs(v-Math.rint(v))<.0001?Integer.toString((int)Math.rint(v)):String.format(Locale.ROOT,"%.1f",v);}
    private String trim(String s,int max){if(s==null)return"";return s.length()<=max?s:s.substring(0,Math.max(0,max-1))+"…";}
    private boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
