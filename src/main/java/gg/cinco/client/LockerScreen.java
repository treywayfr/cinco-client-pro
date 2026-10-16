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
    private int tab,x,y,w,h;

    private record Preview(String kind,int style,String name,int color,double size,boolean rainbow){}

    public LockerScreen(){super(Text.literal("Star Locker"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(980,width-24);h=Math.min(580,height-22);x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦  STAR LOCKER",true),x+22,y+18,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("Star-original cosmetics • live preview • per-item editing",false),x+22,y+37,StarCrispUi.PURPLE_HI,false);
        int tabsX=x+w-390;
        for(int i=0;i<3;i++){int tx=tabsX+i*122;boolean sel=i==tab;StarCrispUi.card(c,tx,y+14,112,34,sel,inside(mx,my,tx,y+14,112,34));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],sel),tx+56,y+26,sel?0xFFFFFFFF:0xFFD5CEDB);}
        StarCrispUi.divider(c,x+18,y+60,x+w-18);
        if(tab==0)renderCosmeticTab(c,mx,my);
        else if(tab==1)skins(c,mx,my);
        else renderAccessoryTab(c,mx,my);
        c.drawText(textRenderer,StarFonts.text(tab==1?"Locker skins are local-only.":"LEFT CLICK equip  •  RIGHT CLICK customize  •  HOVER to preview",false),x+22,y+h-21,0xFFAAA1B2,false);
    }

    private void renderCosmeticTab(DrawContext c,int mx,int my){
        ClientModule m=module("Animations");if(m==null)return;
        Preview p=grid(c,mx,my,EFFECTS,m,EFFECT_NATIVE,"effect");
        if(p==null)p=selectedPreview("effect",m,EFFECTS);
        renderPreview(c,p);
    }

    private void renderAccessoryTab(DrawContext c,int mx,int my){
        ClientModule m=module("Custom Accessory");if(m==null)return;
        Preview p=grid(c,mx,my,ACCESSORIES,m,ACCESSORY_NATIVE,"accessory");
        if(p==null)p=selectedPreview("accessory",m,ACCESSORIES);
        renderPreview(c,p);
    }

    private Preview grid(DrawContext c,int mx,int my,String[] names,ClientModule m,int nativeCount,String kind){
        int previewW=226,gridRight=x+w-previewW-30,startX=x+18,startY=y+76,cols=3,gap=8,cardW=(gridRight-startX-gap*2)/3,cardH=43;
        Preview hovered=null;
        for(int i=0;i<names.length;i++){
            int col=i%cols,row=i/cols,cx=startX+col*(cardW+gap),cy=startY+row*(cardH+6);
            boolean nativeItem=i<nativeCount,preset=isPreset(kind,i),combo=!nativeItem&&!preset;
            int displayStyle=nativeItem?i+1:preset?presetStyle(kind,i-nativeCount):0;
            boolean selected=nativeItem&&m.enabled&&(int)Math.round(m.setting)==i+1;
            boolean hov=inside(mx,my,cx,cy,cardW,cardH);
            StarCrispUi.card(c,cx,cy,cardW,cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,cx+2,cy+6,30);
            c.drawText(textRenderer,StarFonts.text(trim(names[i],22),selected),cx+10,cy+9,selected?0xFFFFFFFF:0xFFE4DDE8,false);
            String sub=nativeItem?(selected?"EQUIPPED • RMB EDIT":"Equip • RMB edit"):preset?"STAR ORIGINAL • RMB EDIT":"Apply combo preset";
            c.drawText(textRenderer,StarFonts.text(sub,false),cx+10,cy+25,preset?0xFFCDB5FF:(selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2),false);
            if(nativeItem){LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,i+1);int dot=CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,i+1),255);c.fill(cx+cardW-15,cy+8,cx+cardW-7,cy+16,dot);if(ip.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+cardW-17,cy+23,0xFFFFFFFF,false);}
            else if(preset){Preview pp=presetPreview(kind,i-nativeCount,names[i]);c.fill(cx+cardW-15,cy+8,cx+cardW-7,cy+16,pp.color);if(pp.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+cardW-17,cy+23,0xFFFFFFFF,false);}
            if(hov&&!combo)hovered=nativeItem?nativePreview(kind,i+1,names[i]):presetPreview(kind,i-nativeCount,names[i]);
        }
        int by=y+h-52;String state=m.enabled?"ENABLED":"DISABLED";CincoDraw.bubble(c,gridRight-121,by,gridRight,by+28,8,m.enabled?0xFFB88CFF:0x667A7182,m.enabled?0xCC32184F:0xCC0A090D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(state,true),gridRight-60,by+9,0xFFFFFFFF);
        return hovered;
    }

    private void renderPreview(DrawContext c,Preview p){
        if(p==null)return;int px=x+w-238,py=y+76,pw=218,ph=h-112;
        LockerPreviewRenderer.render(c,textRenderer,px,py,pw,ph,p.kind,p.style,p.name,p.color,p.size,p.rainbow);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("STAR ORIGINAL PREVIEW",true),px+pw/2,py+ph-38,0xFFB98CFF);
    }

    private Preview selectedPreview(String kind,ClientModule m,String[] names){
        int style=Math.max(1,Math.min(20,(int)Math.round(m.setting)));String name=names[Math.min(style-1,names.length-1)];return nativePreview(kind,style,name);
    }
    private Preview nativePreview(String kind,int style,String name){LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,style);return new Preview(kind,style,name,CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,style),255),LockerCosmeticStore.size(kind,style),p.rainbow);}

    private Preview presetPreview(String kind,int p,String name){
        int style=presetStyle(kind,p);double hue=presetHue(kind,p),size=presetSize(kind,p);boolean rainbow=presetRainbow(kind,p);
        double resolved=rainbow?(System.currentTimeMillis()/1000.0*120.0*.48+style*23.0)%360.0:hue;
        int color=CosmeticColorStore.color(new CosmeticColorStore.Profile(resolved,presetSat(kind,p),presetBright(kind,p)),255);
        return new Preview(kind,style,name,color,size,rainbow);
    }

    private boolean isPreset(String kind,int index){return "effect".equals(kind)?index>=EFFECT_NATIVE&&index<EFFECT_NATIVE+EFFECT_PRESETS:index>=ACCESSORY_NATIVE&&index<ACCESSORY_NATIVE+ACCESSORY_PRESETS;}
    private int presetStyle(String kind,int p){
        if("effect".equals(kind))return new int[]{16,18,15,20,17,19,19,4}[Math.max(0,Math.min(7,p))];
        return new int[]{2,1,9,15,19,8,12,20}[Math.max(0,Math.min(7,p))];
    }
    private double presetHue(String kind,int p){if("effect".equals(kind))return new double[]{282,198,20,292,342,266,278,205}[p];return new double[]{274,48,196,350,284,260,12,272}[p];}
    private double presetSat(String kind,int p){if("effect".equals(kind))return new double[]{.88,.82,.95,.86,.76,.88,.72,.70}[p];return new double[]{.88,.18,.78,.92,.82,.68,.92,.80}[p];}
    private double presetBright(String kind,int p){if("effect".equals(kind))return new double[]{1,.95,1,1,1,.72,.68,.95}[p];return new double[]{.88,1,.95,.82,.95,.62,.95,.82}[p];}
    private double presetSize(String kind,int p){if("effect".equals(kind))return new double[]{1.18,1.08,1.20,1.15,1.08,1.22,1.12,1.16}[p];return new double[]{1.12,1.38,.72,1.20,1.10,1.08,1.10,1.22}[p];}
    private boolean presetRainbow(String kind,int p){return "effect".equals(kind)&&p==3;}

    private void skins(DrawContext c,int mx,int my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return;int startX=x+28,startY=y+92,cardW=(w-80)/5;
        for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+6),cy=startY;boolean selected=m.enabled&&(int)Math.round(m.setting)==i+1;StarCrispUi.card(c,cx,cy,cardW,180,selected,inside(mx,my,cx,cy,cardW,180));CincoDraw.rounded(c,cx+12,cy+14,cx+cardW-12,cy+116,12,skinColor(i));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(i==4?"CJ":i==1?"BART":i==0?"PETER":"✦",true),cx+cardW/2,cy+59,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(SKINS[i],selected),cx+cardW/2,cy+132,selected?0xFFFFFFFF:0xFFD9D1DF);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(selected?"EQUIPPED":"SELECT",false),cx+cardW/2,cy+153,selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2);}
        int by=y+h-92;String outfit="OUTFIT  "+Math.max(1,(int)Math.round(m.setting2));CincoDraw.bubble(c,x+28,by,x+180,by+30,8,0xAFFFFFFF,0xD80A080F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(outfit,true),x+104,by+10,0xFFFFFFFF);c.drawText(textRenderer,StarFonts.text("Click Outfit to cycle the selected skin's wardrobe",false),x+196,by+11,0xFFBDB4C5,false);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        int tabsX=x+w-390;for(int i=0;i<3;i++){int tx=tabsX+i*122;if(inside(mx,my,tx,y+14,112,34)){tab=i;return true;}}
        if(tab==1)return button==0&&skinClick(mx,my);
        if(tab==0)return gridClick(mx,my,button,EFFECTS,module("Animations"),EFFECT_NATIVE,"effect");
        return gridClick(mx,my,button,ACCESSORIES,module("Custom Accessory"),ACCESSORY_NATIVE,"accessory");
    }

    private boolean gridClick(double mx,double my,int button,String[] names,ClientModule m,int nativeCount,String kind){
        if(m==null)return false;int previewW=226,gridRight=x+w-previewW-30,startX=x+18,startY=y+76,cols=3,gap=8,cardW=(gridRight-startX-gap*2)/3,cardH=43;
        for(int i=0;i<names.length;i++){
            int cx=startX+(i%cols)*(cardW+gap),cy=startY+(i/cols)*(cardH+6);if(!inside(mx,my,cx,cy,cardW,cardH))continue;
            if(i<nativeCount){if(button==1){if(client!=null)client.setScreen(new LockerItemSettingsScreen(kind,i+1,names[i]));return true;}if(button==0){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}return false;}
            if(isPreset(kind,i)){
                int p=i-nativeCount;applyPreset(kind,p,m);if(button==1&&client!=null)client.setScreen(new LockerItemSettingsScreen(kind,presetStyle(kind,p),names[i]));return true;
            }
            if(button==0&&"effect".equals(kind)){applyCombo(i-(EFFECT_NATIVE+EFFECT_PRESETS));return true;}return false;
        }
        int by=y+h-52;if(button==0&&inside(mx,my,gridRight-121,by,121,28)){m.enabled=!m.enabled;ConfigStore.save();return true;}return false;
    }

    private void applyPreset(String kind,int p,ClientModule m){
        int style=presetStyle(kind,p);m.setting=style;m.enabled=true;LockerCosmeticStore.ItemProfile ip=LockerCosmeticStore.get(kind,style);ip.hue=presetHue(kind,p);ip.saturation=presetSat(kind,p);ip.brightness=presetBright(kind,p);ip.size=presetSize(kind,p);ip.rainbow=presetRainbow(kind,p);ip.rainbowSpeed=.48;LockerCosmeticStore.save();ConfigStore.save();
    }

    private boolean skinClick(double mx,double my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return false;int startX=x+28,startY=y+92,cardW=(w-80)/5;for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+6);if(inside(mx,my,cx,startY,cardW,180)){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}}int by=y+h-92;if(inside(mx,my,x+28,by,152,30)){m.setting2=m.setting2>=m.max2?m.min2:m.setting2+Math.max(1,m.step2);ConfigStore.save();return true;}return false;}
    private void applyCombo(int combo){ClientModule a=module("Custom Accessory"),e=module("Animations");if(a==null||e==null)return;a.enabled=true;e.enabled=true;switch(combo){case 0->{a.setting=3;e.setting=10;}case 1->{a.setting=2;e.setting=6;}case 2->{a.setting=12;e.setting=13;}default->{a.setting=13;e.setting=9;}}ConfigStore.save();}
    private ClientModule module(String n){return CincoClient.findModule(n);}
    private int skinColor(int i){return switch(i){case 0->0xFF4E7B3A;case 1->0xFFF06B2B;case 2->0xFF2B2C35;case 3->0xFF6C3A83;default->0xFF263A55;};}
    private String trim(String s,int n){return s.length()<=n?s:s.substring(0,Math.max(1,n-1))+"…";}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
