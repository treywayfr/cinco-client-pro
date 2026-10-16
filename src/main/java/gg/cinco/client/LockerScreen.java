package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Star Locker with real 3D character/cosmetic previews and two articulated character slots. */
public final class LockerScreen extends StarLegacyScreen {
    private static final String[] TABS={"Cosmetics","Skins","Accessories"};
    private static final String[] ACCESSORIES={
            "Angel Wings","Demon Wings","Solid Halo","Star Crown","Orbit Crystals","Nebula Horns","Energy Ring","Nebula Cape",
            "Dragon Wings","Butterfly Wings","Shoulder Orbs","Galaxy Crown","Back Blades","Floating Crystals",
            "Phoenix Wings","Cat Ears","Devil Tail","Celestial Staff","Orbit Backpack","Crystal Antlers",
            "Webbed Void Wings","Seraph Wings XL","Baby Wyvern Wings","Deathflare Wings",
            "Astral Backpack","Shadow Cloak","Infernal Crown","Void Antlers"};
    private static final String[] EFFECTS={
            "Aura Rings","Purple Aura","Jump Circles","Speed Trails","Orbit Runes","Lightning Sparks","Footstep Rings","Cosmic Pulse",
            "Spiral Aura","Star Orbit","Comet Trail","Shockwave","Galaxy Cage","Crown Aura",
            "Meteor Halo","Void Helix","Heart Burst","Solar Rings","Rune Storm","Aurora Veil",
            "Starfire Dragon Aura","Tidal Orbit","Emberfall Aura","Prism Chroma Aura",
            "Heartfall Aura","Bat Eclipse","Skullstorm Aura","Windstream Aura",
            "Celestial Set","Demon Set","Nebula Set","Void Set"};
    private static final String[] SKINS={"CJ","Tung Tung Suhr"};
    private static final int EFFECT_NATIVE=20,ACCESSORY_NATIVE=20,EFFECT_PRESETS=8,ACCESSORY_PRESETS=8;

    private final int[] scrollRows={0,0,0};
    private int tab,x,y,w,h;
    private float previewYaw=22f,previewPitch=-4f;
    private boolean previewDragging;
    private int previewX,previewY,previewW,previewH;
    private int hoveredSkin=-1;

    private record Preview(String kind,int style,String name,int color,double size,boolean rainbow){}
    private record GridLayout(int startX,int startY,int cols,int gap,int cardW,int cardH,int pitch,int gridRight,int previewX,int previewW,int listBottom,int visibleRows){}

    public LockerScreen(){super(Text.literal("Star Locker"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        StarBackground.render(c,width,height,delta);
        w=Math.max(390,Math.min(980,width-16));h=Math.max(230,Math.min(580,height-14));x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦ STAR LOCKER",true),x+16,y+14,0xFFFFFFFF,false);
        if(w>570)c.drawText(textRenderer,StarFonts.text("Real 3D previews • drag preview to rotate • RMB customize",false),x+16,y+31,StarCrispUi.PURPLE_HI,false);
        int tabW=Math.max(72,Math.min(108,(w-34)/3)),tabsX=x+w-tabW*3-12;
        for(int i=0;i<3;i++){int tx=tabsX+i*tabW;boolean sel=i==tab;StarCrispUi.card(c,tx,y+10,tabW-4,30,sel,inside(mx,my,tx,y+10,tabW-4,30));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],sel),tx+(tabW-4)/2,y+21,sel?0xFFFFFFFF:0xFFD5CEDB);}
        StarCrispUi.divider(c,x+12,y+50,x+w-12);
        if(tab==0)renderCosmeticTab(c,mx,my,delta);else if(tab==1)skins(c,mx,my,delta);else renderAccessoryTab(c,mx,my,delta);
        if(h>250)c.drawText(textRenderer,StarFonts.text(tab==1?"LMB equip • drag model to spin • local visual replacement":"LMB equip • RMB customize • wheel scroll • drag preview",false),x+14,y+h-17,0xFFAAA1B2,false);
    }

    private GridLayout layout(){
        int pw=Math.max(150,Math.min(255,w/3));int px=x+w-pw-12,gridRight=px-8;
        int startX=x+12,startY=y+58,gap=6,avail=Math.max(170,gridRight-startX);
        int cols=avail>=590?4:avail>=360?3:2;int cardW=Math.max(72,(avail-gap*(cols-1))/cols),cardH=h<300?34:40,pitch=cardH+5;
        int listBottom=y+h-(h>250?31:10),visibleRows=Math.max(1,(listBottom-startY)/pitch);
        return new GridLayout(startX,startY,cols,gap,cardW,cardH,pitch,gridRight,px,pw,listBottom,visibleRows);
    }

    private void setPreviewRect(int px,int py,int pw,int ph){previewX=px;previewY=py;previewW=pw;previewH=ph;}

    private void renderCosmeticTab(DrawContext c,int mx,int my,float delta){ClientModule m=module("Animations");if(m==null)return;Preview p=grid(c,mx,my,EFFECTS,m,EFFECT_NATIVE,"effect");if(p==null)p=selectedPreview("effect",m,EFFECTS);renderPreview(c,p,delta);}
    private void renderAccessoryTab(DrawContext c,int mx,int my,float delta){ClientModule m=module("Custom Accessory");if(m==null)return;Preview p=grid(c,mx,my,ACCESSORIES,m,ACCESSORY_NATIVE,"accessory");if(p==null)p=selectedPreview("accessory",m,ACCESSORIES);renderPreview(c,p,delta);}

    private Preview grid(DrawContext c,int mx,int my,String[] names,ClientModule m,int nativeCount,String kind){
        GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);scrollRows[tab]=clamp(scrollRows[tab],0,maxScroll);Preview hovered=null;
        for(int i=0;i<names.length;i++){
            int col=i%l.cols,row=i/l.cols;if(row<scrollRows[tab]||row>=scrollRows[tab]+l.visibleRows)continue;
            int cx=l.startX+col*(l.cardW+l.gap),cy=l.startY+(row-scrollRows[tab])*l.pitch;
            boolean nativeItem=i<nativeCount,preset=isPreset(kind,i),combo=!nativeItem&&!preset,selected=nativeItem&&m.enabled&&(int)Math.round(m.setting)==i+1,hov=inside(mx,my,cx,cy,l.cardW,l.cardH);
            StarCrispUi.card(c,cx,cy,l.cardW,l.cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,cx+2,cy+5,Math.max(18,l.cardH-10));
            c.drawText(textRenderer,StarFonts.text(trim(names[i],l.cardW<105?12:20),selected),cx+8,cy+8,selected?0xFFFFFFFF:0xFFE4DDE8,false);
            if(l.cardH>=39){String sub=nativeItem?(selected?"EQUIPPED • RMB":"Equip • RMB"):preset?"STAR ORIGINAL":"COMBO PRESET";c.drawText(textRenderer,StarFonts.text(sub,false),cx+8,cy+24,preset?0xFFCDB5FF:(selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2),false);}
            if(nativeItem){LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,i+1);int dot=CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,i+1),255);CincoDraw.rounded(c,cx+l.cardW-14,cy+6,cx+l.cardW-5,cy+15,5,dot);if(ip.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+l.cardW-15,cy+20,0xFFFFFFFF,false);}
            else if(preset){Preview pp=presetPreview(kind,i-nativeCount,names[i]);CincoDraw.rounded(c,cx+l.cardW-14,cy+6,cx+l.cardW-5,cy+15,5,pp.color);}
            if(hov&&!combo)hovered=nativeItem?nativePreview(kind,i+1,names[i]):presetPreview(kind,i-nativeCount,names[i]);
        }
        if(maxScroll>0){String sc=(scrollRows[tab]+1)+"/"+(maxScroll+1);c.drawText(textRenderer,Text.literal(sc),l.gridRight-textRenderer.getWidth(sc)-2,l.listBottom-9,0xFF94899E,false);}
        return hovered;
    }

    private void renderPreview(DrawContext c,Preview p,float delta){if(p==null)return;GridLayout l=layout();int py=y+58,ph=Math.max(130,h-(h>250?88:68));setPreviewRect(l.previewX,py,l.previewW,ph);Locker3dPreviewRenderer.renderCosmetic(c,l.previewX,py,l.previewW,ph,p.kind,p.style,p.name,p.color,p.size,p.rainbow,previewYaw,previewPitch,delta);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(trim(p.name,26),true),l.previewX+l.previewW/2,py+11,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("DRAG TO ROTATE",false),l.previewX+l.previewW/2,py+27,0xFFB9A8CA);}
    private Preview selectedPreview(String kind,ClientModule m,String[] names){int style=Math.max(1,Math.min(20,(int)Math.round(m.setting)));return nativePreview(kind,style,names[Math.min(style-1,names.length-1)]);}
    private Preview nativePreview(String kind,int style,String name){LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,style);return new Preview(kind,style,name,CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,style),255),LockerCosmeticStore.size(kind,style),p.rainbow);}
    private Preview presetPreview(String kind,int p,String name){int style=presetStyle(kind,p);double hue=presetHue(kind,p),size=presetSize(kind,p);boolean rainbow=presetRainbow(kind,p);double resolved=rainbow?(System.currentTimeMillis()/1000.0*57.6+style*23.0)%360.0:hue;int color=CosmeticColorStore.color(new CosmeticColorStore.Profile(resolved,presetSat(kind,p),presetBright(kind,p)),255);return new Preview(kind,style,name,color,size,rainbow);}

    private void skins(DrawContext c,int mx,int my,float delta){
        ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return;
        GridLayout l=layout();hoveredSkin=-1;
        int listW=Math.max(170,l.gridRight-l.startX),cardH=Math.min(105,Math.max(72,(h-105)/2));
        for(int i=0;i<2;i++){
            int cy=l.startY+i*(cardH+10);boolean selected=m.enabled&&(int)Math.round(m.setting)==i+1,hov=inside(mx,my,l.startX,cy,listW,cardH);if(hov)hoveredSkin=i;
            StarCrispUi.card(c,l.startX,cy,listW,cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,l.startX+3,cy+8,cardH-16);
            int badge=cardH-18;CincoDraw.rounded(c,l.startX+10,cy+9,l.startX+10+badge,cy+9+badge,badge/2,i==0?0x883E2C4C:0x885C361B);
            c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(i==0?"CJ":"TT",true),l.startX+10+badge/2,cy+cardH/2-4,0xFFFFFFFF);
            int tx=l.startX+badge+22;c.drawText(textRenderer,StarFonts.text(SKINS[i],true),tx,cy+20,selected?0xFFFF8BE8:0xFFFFFFFF,false);
            c.drawText(textRenderer,StarFonts.text(i==0?"San Andreas-inspired 3D model":"Animated wooden character model",false),tx,cy+39,0xFFB8A8C7,false);
            c.drawText(textRenderer,StarFonts.text(selected?"EQUIPPED":"CLICK TO EQUIP",false),tx,cy+60,selected?0xFFFF71DC:0xFF9B8DA8,false);
        }
        int character=hoveredSkin>=0?hoveredSkin+1:Math.max(1,Math.min(2,(int)Math.round(m.setting)));
        int py=y+58,ph=Math.max(140,h-(h>250?88:68));setPreviewRect(l.previewX,py,l.previewW,ph);
        Locker3dPreviewRenderer.renderSkin(c,l.previewX,py,l.previewW,ph,character,previewYaw,previewPitch,delta);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(SKINS[character-1],true),l.previewX+l.previewW/2,py+11,0xFFFFFFFF);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("REAL 3D • DRAG TO ROTATE",false),l.previewX+l.previewW/2,py+27,0xFFC4B3D2);
        if(h>300){int by=y+h-50;String scale=((int)Math.round(m.setting2*100))+"% MODEL SCALE";CincoDraw.bubble(c,l.startX,by,l.startX+150,by+26,13,0x687C5F96,0x45120A20);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(scale,true),l.startX+75,by+9,0xFFEADFF5);}
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        int tabW=Math.max(72,Math.min(108,(w-34)/3)),tabsX=x+w-tabW*3-12;for(int i=0;i<3;i++){int tx=tabsX+i*tabW;if(inside(mx,my,tx,y+10,tabW-4,30)){tab=i;previewDragging=false;return true;}}
        if(button==0&&inside(mx,my,previewX,previewY,previewW,previewH)){previewDragging=true;return true;}
        if(tab==1)return button==0&&skinClick(mx,my);if(tab==0)return gridClick(mx,my,button,EFFECTS,module("Animations"),EFFECT_NATIVE,"effect");return gridClick(mx,my,button,ACCESSORIES,module("Custom Accessory"),ACCESSORY_NATIVE,"accessory");
    }

    @Override public boolean mouseDragged(double mx,double my,int button,double dx,double dy){if(button==0&&previewDragging){previewYaw+=dx*.85f;previewPitch=clampf(previewPitch-(float)dy*.50f,-28f,28f);return true;}return false;}
    @Override public boolean mouseReleased(double mx,double my,int button){if(previewDragging){previewDragging=false;return true;}return false;}

    private boolean gridClick(double mx,double my,int button,String[] names,ClientModule m,int nativeCount,String kind){
        if(m==null)return false;GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);scrollRows[tab]=clamp(scrollRows[tab],0,maxScroll);
        for(int i=0;i<names.length;i++){int col=i%l.cols,row=i/l.cols;if(row<scrollRows[tab]||row>=scrollRows[tab]+l.visibleRows)continue;int cx=l.startX+col*(l.cardW+l.gap),cy=l.startY+(row-scrollRows[tab])*l.pitch;if(!inside(mx,my,cx,cy,l.cardW,l.cardH))continue;
            if(i<nativeCount){if(button==1){if(client!=null)client.setScreen(new LockerItemSettingsScreen(kind,i+1,names[i]));return true;}if(button==0){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}return false;}
            if(isPreset(kind,i)){int p=i-nativeCount;applyPreset(kind,p,m);if(button==1&&client!=null)client.setScreen(new LockerItemSettingsScreen(kind,presetStyle(kind,p),names[i]));return true;}
            if(button==0&&"effect".equals(kind)){applyCombo(i-(EFFECT_NATIVE+EFFECT_PRESETS));return true;}return false;
        }
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){if(tab==1)return false;String[] names=tab==0?EFFECTS:ACCESSORIES;GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);if(maxScroll==0)return false;int step=verticalAmount>0?-1:verticalAmount<0?1:0;if(step==0)return false;scrollRows[tab]=clamp(scrollRows[tab]+step,0,maxScroll);return true;}

    private boolean skinClick(double mx,double my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return false;GridLayout l=layout();int listW=Math.max(170,l.gridRight-l.startX),cardH=Math.min(105,Math.max(72,(h-105)/2));for(int i=0;i<2;i++){int cy=l.startY+i*(cardH+10);if(inside(mx,my,l.startX,cy,listW,cardH)){m.setting=i+1;m.enabled=true;if(m.setting2<.8||m.setting2>1.25)m.setting2=1.0;ConfigStore.save();return true;}}return false;}

    private boolean isPreset(String kind,int index){return "effect".equals(kind)?index>=EFFECT_NATIVE&&index<EFFECT_NATIVE+EFFECT_PRESETS:index>=ACCESSORY_NATIVE&&index<ACCESSORY_NATIVE+ACCESSORY_PRESETS;}
    private int presetStyle(String kind,int p){return "effect".equals(kind)?new int[]{16,18,15,20,17,19,19,4}[clamp(p,0,7)]:new int[]{2,1,9,15,19,8,12,20}[clamp(p,0,7)];}
    private double presetHue(String kind,int p){return "effect".equals(kind)?new double[]{282,198,20,292,342,266,278,205}[p]:new double[]{274,48,196,350,284,260,12,272}[p];}
    private double presetSat(String kind,int p){return "effect".equals(kind)?new double[]{.88,.82,.95,.86,.76,.88,.72,.70}[p]:new double[]{.88,.18,.78,.92,.82,.68,.92,.80}[p];}
    private double presetBright(String kind,int p){return "effect".equals(kind)?new double[]{1,.95,1,1,1,.72,.68,.95}[p]:new double[]{.88,1,.95,.82,.95,.62,.95,.82}[p];}
    private double presetSize(String kind,int p){return "effect".equals(kind)?new double[]{1.18,1.08,1.20,1.15,1.08,1.22,1.12,1.16}[p]:new double[]{1.12,1.38,.72,1.20,1.10,1.08,1.10,1.22}[p];}
    private boolean presetRainbow(String kind,int p){return "effect".equals(kind)&&p==3;}
    private void applyPreset(String kind,int p,ClientModule m){int style=presetStyle(kind,p);m.setting=style;m.enabled=true;LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,style);ip.hue=presetHue(kind,p);ip.saturation=presetSat(kind,p);ip.brightness=presetBright(kind,p);ip.size=presetSize(kind,p);ip.rainbow=presetRainbow(kind,p);ip.rainbowSpeed=.48;LockerCosmeticStore.save();ConfigStore.save();}
    private void applyCombo(int combo){ClientModule a=module("Custom Accessory"),e=module("Animations");if(a==null||e==null)return;a.enabled=true;e.enabled=true;switch(combo){case 0->{a.setting=3;e.setting=10;}case 1->{a.setting=2;e.setting=6;}case 2->{a.setting=12;e.setting=13;}default->{a.setting=13;e.setting=9;}}ConfigStore.save();}
    private ClientModule module(String n){return CincoClient.findModule(n);}
    private String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
    private int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private float clampf(float v,float lo,float hi){return Math.max(lo,Math.min(hi,v));}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
