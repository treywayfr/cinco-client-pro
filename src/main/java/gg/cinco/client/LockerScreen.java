package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Star Locker: cosmetics, skins and accessories with live previews and per-item editing. */
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
    private static final String[] SKINS={"Peter Griffin","Bart Simpson","Street Hero","Anime Fighter","CJ / San Andreas"};
    private static final int EFFECT_NATIVE=20, ACCESSORY_NATIVE=20, EFFECT_PRESETS=8, ACCESSORY_PRESETS=8;
    private final int[] scrollRows={0,0,0};
    private int tab,x,y,w,h;

    private record Preview(String kind,int style,String name,int color,double size,boolean rainbow){}
    private record GridLayout(int startX,int startY,int cols,int gap,int cardW,int cardH,int pitch,int gridRight,int previewX,int previewW,int listBottom,int visibleRows){}

    public LockerScreen(){super(Text.literal("Star Locker"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.max(360,Math.min(980,width-16));h=Math.max(210,Math.min(580,height-14));x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦ STAR LOCKER",true),x+16,y+14,0xFFFFFFFF,false);
        if(w>570)c.drawText(textRenderer,StarFonts.text("Star-original cosmetics • live preview • per-item editing",false),x+16,y+31,StarCrispUi.PURPLE_HI,false);
        int tabW=Math.max(72,Math.min(108,(w-34)/3)),tabsX=x+w-(tabW*3)-12;
        for(int i=0;i<3;i++){int tx=tabsX+i*tabW;boolean sel=i==tab;StarCrispUi.card(c,tx,y+10,tabW-4,30,sel,inside(mx,my,tx,y+10,tabW-4,30));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],sel),tx+(tabW-4)/2,y+21,sel?0xFFFFFFFF:0xFFD5CEDB);}
        StarCrispUi.divider(c,x+12,y+50,x+w-12);
        if(tab==0)renderCosmeticTab(c,mx,my);else if(tab==1)skins(c,mx,my);else renderAccessoryTab(c,mx,my);
        if(h>250)c.drawText(textRenderer,StarFonts.text(tab==1?"Skins are local-only.":"LMB equip • RMB customize • wheel scroll • hover preview",false),x+14,y+h-17,0xFFAAA1B2,false);
    }

    private GridLayout layout(){
        int previewW=Math.max(116,Math.min(218,w/3));int previewX=x+w-previewW-12;int gridRight=previewX-8;
        int startX=x+12,startY=y+58,gap=6,avail=Math.max(170,gridRight-startX);
        int cols=avail>=590?4:avail>=360?3:2;int cardW=Math.max(72,(avail-gap*(cols-1))/cols);int cardH=h<300?34:40,pitch=cardH+5;
        int listBottom=y+h-(h>250?31:10);int visibleRows=Math.max(1,(listBottom-startY)/pitch);
        return new GridLayout(startX,startY,cols,gap,cardW,cardH,pitch,gridRight,previewX,previewW,listBottom,visibleRows);
    }

    private void renderCosmeticTab(DrawContext c,int mx,int my){ClientModule m=module("Animations");if(m==null)return;Preview p=grid(c,mx,my,EFFECTS,m,EFFECT_NATIVE,"effect");if(p==null)p=selectedPreview("effect",m,EFFECTS);renderPreview(c,p);}
    private void renderAccessoryTab(DrawContext c,int mx,int my){ClientModule m=module("Custom Accessory");if(m==null)return;Preview p=grid(c,mx,my,ACCESSORIES,m,ACCESSORY_NATIVE,"accessory");if(p==null)p=selectedPreview("accessory",m,ACCESSORIES);renderPreview(c,p);}

    private Preview grid(DrawContext c,int mx,int my,String[] names,ClientModule m,int nativeCount,String kind){
        GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);scrollRows[tab]=clamp(scrollRows[tab],0,maxScroll);Preview hovered=null;
        for(int i=0;i<names.length;i++){
            int col=i%l.cols,row=i/l.cols;if(row<scrollRows[tab]||row>=scrollRows[tab]+l.visibleRows)continue;
            int cx=l.startX+col*(l.cardW+l.gap),cy=l.startY+(row-scrollRows[tab])*l.pitch;
            boolean nativeItem=i<nativeCount,preset=isPreset(kind,i),combo=!nativeItem&&!preset,selected=nativeItem&&m.enabled&&(int)Math.round(m.setting)==i+1,hov=inside(mx,my,cx,cy,l.cardW,l.cardH);
            StarCrispUi.card(c,cx,cy,l.cardW,l.cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,cx+2,cy+5,Math.max(18,l.cardH-10));
            c.drawText(textRenderer,StarFonts.text(trim(names[i],l.cardW<105?12:20),selected),cx+8,cy+8,selected?0xFFFFFFFF:0xFFE4DDE8,false);
            if(l.cardH>=39){String sub=nativeItem?(selected?"EQUIPPED • RMB":"Equip • RMB"):preset?"STAR ORIGINAL":"COMBO PRESET";c.drawText(textRenderer,StarFonts.text(sub,false),cx+8,cy+24,preset?0xFFCDB5FF:(selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2),false);}
            if(nativeItem){LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,i+1);int dot=CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,i+1),255);c.fill(cx+l.cardW-13,cy+7,cx+l.cardW-6,cy+14,dot);if(ip.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+l.cardW-15,cy+20,0xFFFFFFFF,false);}
            else if(preset){Preview pp=presetPreview(kind,i-nativeCount,names[i]);c.fill(cx+l.cardW-13,cy+7,cx+l.cardW-6,cy+14,pp.color);if(pp.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+l.cardW-15,cy+20,0xFFFFFFFF,false);}
            if(hov&&!combo)hovered=nativeItem?nativePreview(kind,i+1,names[i]):presetPreview(kind,i-nativeCount,names[i]);
        }
        if(maxScroll>0){String sc=(scrollRows[tab]+1)+"/"+(maxScroll+1);c.drawText(textRenderer,Text.literal(sc),l.gridRight-textRenderer.getWidth(sc)-2,l.listBottom-9,0xFF94899E,false);}
        return hovered;
    }

    private void renderPreview(DrawContext c,Preview p){if(p==null)return;GridLayout l=layout();int py=y+58,ph=Math.max(118,h-(h>250?88:68));LockerPreviewRenderer.render(c,textRenderer,l.previewX,py,l.previewW,ph,p.kind,p.style,p.name,p.color,p.size,p.rainbow);}
    private Preview selectedPreview(String kind,ClientModule m,String[] names){int style=Math.max(1,Math.min(20,(int)Math.round(m.setting)));return nativePreview(kind,style,names[Math.min(style-1,names.length-1)]);}
    private Preview nativePreview(String kind,int style,String name){LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,style);return new Preview(kind,style,name,CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,style),255),LockerCosmeticStore.size(kind,style),p.rainbow);}
    private Preview presetPreview(String kind,int p,String name){int style=presetStyle(kind,p);double hue=presetHue(kind,p),size=presetSize(kind,p);boolean rainbow=presetRainbow(kind,p);double resolved=rainbow?(System.currentTimeMillis()/1000.0*57.6+style*23.0)%360.0:hue;int color=CosmeticColorStore.color(new CosmeticColorStore.Profile(resolved,presetSat(kind,p),presetBright(kind,p)),255);return new Preview(kind,style,name,color,size,rainbow);}

    private boolean isPreset(String kind,int index){return "effect".equals(kind)?index>=EFFECT_NATIVE&&index<EFFECT_NATIVE+EFFECT_PRESETS:index>=ACCESSORY_NATIVE&&index<ACCESSORY_NATIVE+ACCESSORY_PRESETS;}
    private int presetStyle(String kind,int p){return "effect".equals(kind)?new int[]{16,18,15,20,17,19,19,4}[clamp(p,0,7)]:new int[]{2,1,9,15,19,8,12,20}[clamp(p,0,7)];}
    private double presetHue(String kind,int p){return "effect".equals(kind)?new double[]{282,198,20,292,342,266,278,205}[p]:new double[]{274,48,196,350,284,260,12,272}[p];}
    private double presetSat(String kind,int p){return "effect".equals(kind)?new double[]{.88,.82,.95,.86,.76,.88,.72,.70}[p]:new double[]{.88,.18,.78,.92,.82,.68,.92,.80}[p];}
    private double presetBright(String kind,int p){return "effect".equals(kind)?new double[]{1,.95,1,1,1,.72,.68,.95}[p]:new double[]{.88,1,.95,.82,.95,.62,.95,.82}[p];}
    private double presetSize(String kind,int p){return "effect".equals(kind)?new double[]{1.18,1.08,1.20,1.15,1.08,1.22,1.12,1.16}[p]:new double[]{1.12,1.38,.72,1.20,1.10,1.08,1.10,1.22}[p];}
    private boolean presetRainbow(String kind,int p){return "effect".equals(kind)&&p==3;}

    private void skins(DrawContext c,int mx,int my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return;int startX=x+18,startY=y+68,cardW=Math.max(60,(w-48)/5);for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+3),cy=startY;boolean selected=m.enabled&&(int)Math.round(m.setting)==i+1;StarCrispUi.card(c,cx,cy,cardW,Math.min(180,h-110),selected,inside(mx,my,cx,cy,cardW,Math.min(180,h-110)));CincoDraw.rounded(c,cx+8,cy+10,cx+cardW-8,cy+Math.min(100,h-145),10,skinColor(i));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(i==4?"CJ":i==1?"BART":i==0?"PETER":"✦",true),cx+cardW/2,cy+38,0xFFFFFFFF);if(h>300)c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(trim(SKINS[i],14),selected),cx+cardW/2,cy+116,selected?0xFFFFFFFF:0xFFD9D1DF);}
        if(h>280){int by=y+h-62;String outfit="OUTFIT "+Math.max(1,(int)Math.round(m.setting2));CincoDraw.bubble(c,x+18,by,x+140,by+28,8,0xAFFFFFFF,0xD80A080F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(outfit,true),x+79,by+9,0xFFFFFFFF);}}

    @Override public boolean mouseClicked(double mx,double my,int button){
        int tabW=Math.max(72,Math.min(108,(w-34)/3)),tabsX=x+w-(tabW*3)-12;for(int i=0;i<3;i++){int tx=tabsX+i*tabW;if(inside(mx,my,tx,y+10,tabW-4,30)){tab=i;return true;}}
        if(tab==1)return button==0&&skinClick(mx,my);if(tab==0)return gridClick(mx,my,button,EFFECTS,module("Animations"),EFFECT_NATIVE,"effect");return gridClick(mx,my,button,ACCESSORIES,module("Custom Accessory"),ACCESSORY_NATIVE,"accessory");
    }

    private boolean gridClick(double mx,double my,int button,String[] names,ClientModule m,int nativeCount,String kind){
        if(m==null)return false;GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);scrollRows[tab]=clamp(scrollRows[tab],0,maxScroll);
        for(int i=0;i<names.length;i++){int col=i%l.cols,row=i/l.cols;if(row<scrollRows[tab]||row>=scrollRows[tab]+l.visibleRows)continue;int cx=l.startX+col*(l.cardW+l.gap),cy=l.startY+(row-scrollRows[tab])*l.pitch;if(!inside(mx,my,cx,cy,l.cardW,l.cardH))continue;
            if(i<nativeCount){if(button==1){if(client!=null)client.setScreen(new LockerItemSettingsScreen(kind,i+1,names[i]));return true;}if(button==0){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}return false;}
            if(isPreset(kind,i)){int p=i-nativeCount;applyPreset(kind,p,m);if(button==1&&client!=null)client.setScreen(new LockerItemSettingsScreen(kind,presetStyle(kind,p),names[i]));return true;}
            if(button==0&&"effect".equals(kind)){applyCombo(i-(EFFECT_NATIVE+EFFECT_PRESETS));return true;}return false;}
        return false;
    }

    @Override public boolean mouseScrolled(double mouseX,double mouseY,double horizontalAmount,double verticalAmount){
        if(tab==1)return false;String[] names=tab==0?EFFECTS:ACCESSORIES;GridLayout l=layout();int rows=(names.length+l.cols-1)/l.cols,maxScroll=Math.max(0,rows-l.visibleRows);if(maxScroll==0)return false;int step=verticalAmount>0?-1:verticalAmount<0?1:0;if(step==0)return false;scrollRows[tab]=clamp(scrollRows[tab]+step,0,maxScroll);return true;
    }

    private void applyPreset(String kind,int p,ClientModule m){int style=presetStyle(kind,p);m.setting=style;m.enabled=true;LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,style);ip.hue=presetHue(kind,p);ip.saturation=presetSat(kind,p);ip.brightness=presetBright(kind,p);ip.size=presetSize(kind,p);ip.rainbow=presetRainbow(kind,p);ip.rainbowSpeed=.48;LockerCosmeticStore.save();ConfigStore.save();}
    private boolean skinClick(double mx,double my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return false;int startX=x+18,startY=y+68,cardW=Math.max(60,(w-48)/5),cardH=Math.min(180,h-110);for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+3);if(inside(mx,my,cx,startY,cardW,cardH)){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}}if(h>280){int by=y+h-62;if(inside(mx,my,x+18,by,122,28)){m.setting2=m.setting2>=m.max2?m.min2:m.setting2+Math.max(1,m.step2);ConfigStore.save();return true;}}return false;}
    private void applyCombo(int combo){ClientModule a=module("Custom Accessory"),e=module("Animations");if(a==null||e==null)return;a.enabled=true;e.enabled=true;switch(combo){case 0->{a.setting=3;e.setting=10;}case 1->{a.setting=2;e.setting=6;}case 2->{a.setting=12;e.setting=13;}default->{a.setting=13;e.setting=9;}}ConfigStore.save();}
    private ClientModule module(String n){return CincoClient.findModule(n);}
    private int skinColor(int i){return switch(i){case 0->0xFF4E7B3A;case 1->0xFFF06B2B;case 2->0xFF2B2C35;case 3->0xFF6C3A83;default->0xFF263A55;};}
    private String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
    private int clamp(int v,int lo,int hi){return Math.max(lo,Math.min(hi,v));}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
