package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Star Locker: the only user-facing home for cosmetic effects, skins and accessories. */
public final class LockerScreen extends StarLegacyScreen {
    private static final String[] TABS={"Cosmetics","Skins","Accessories"};
    private static final String[] ACCESSORIES={
            "Angel Wings","Demon Wings","Solid Halo","Star Crown","Orbit Crystals","Nebula Horns","Energy Ring","Nebula Cape",
            "Dragon Wings","Butterfly Wings","Shoulder Orbs","Galaxy Crown","Back Blades","Floating Crystals",
            "Phoenix Wings","Cat Ears","Devil Tail","Celestial Staff","Orbit Backpack","Crystal Antlers"};
    private static final String[] EFFECTS={
            "Aura Rings","Purple Aura","Jump Circles","Speed Trails","Orbit Runes","Lightning Sparks","Footstep Rings","Cosmic Pulse",
            "Spiral Aura","Star Orbit","Comet Trail","Shockwave","Galaxy Cage","Crown Aura",
            "Meteor Halo","Void Helix","Heart Burst","Solar Rings","Rune Storm","Aurora Veil",
            "Celestial Set","Demon Set","Nebula Set","Void Set"};
    private static final String[] SKINS={"Peter Griffin","Bart Simpson","Street Hero","Anime Fighter","CJ / San Andreas"};
    private static final int EFFECT_NATIVE=20, ACCESSORY_NATIVE=20;
    private int tab,x,y,w,h;

    public LockerScreen(){super(Text.literal("Star Locker"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(900,width-34);h=Math.min(560,height-26);x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦  STAR LOCKER",true),x+24,y+18,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("Cosmetics • Skins • Accessories",false),x+24,y+37,StarCrispUi.PURPLE_HI,false);
        int tabsX=x+w-390;
        for(int i=0;i<3;i++){int tx=tabsX+i*122;boolean sel=i==tab;StarCrispUi.card(c,tx,y+14,112,34,sel,inside(mx,my,tx,y+14,112,34));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],sel),tx+56,y+26,sel?0xFFFFFFFF:0xFFD5CEDB);}
        StarCrispUi.divider(c,x+20,y+60,x+w-20);
        if(tab==0)grid(c,mx,my,EFFECTS,module("Animations"),EFFECT_NATIVE,"effect");
        else if(tab==1)skins(c,mx,my);
        else grid(c,mx,my,ACCESSORIES,module("Custom Accessory"),ACCESSORY_NATIVE,"accessory");
        c.drawText(textRenderer,StarFonts.text(tab==1?"Locker skins are local-only.":"LEFT CLICK equip  •  RIGHT CLICK color, size & animated rainbow",false),x+24,y+h-22,0xFFAAA1B2,false);
    }

    private void grid(DrawContext c,int mx,int my,String[] names,ClientModule m,int nativeCount,String kind){
        if(m==null)return;int startX=x+20,startY=y+76,cols=4,gap=8,cardW=(w-64)/4,cardH=46;
        for(int i=0;i<names.length;i++){
            int col=i%cols,row=i/cols,cx=startX+col*(cardW+gap),cy=startY+row*(cardH+7);
            boolean nativeItem=i<nativeCount,selected=nativeItem&&m.enabled&&(int)Math.round(m.setting)==i+1,hov=inside(mx,my,cx,cy,cardW,cardH);
            StarCrispUi.card(c,cx,cy,cardW,cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,cx+2,cy+7,32);
            c.drawText(textRenderer,StarFonts.text(names[i],selected),cx+11,cy+10,selected?0xFFFFFFFF:0xFFE4DDE8,false);
            String sub=nativeItem?(selected?"EQUIPPED • RMB EDIT":"Equip • RMB edit"):"Apply combo preset";
            c.drawText(textRenderer,StarFonts.text(sub,false),cx+11,cy+27,selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2,false);
            if(nativeItem){LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,i+1);int dot=LockerCosmeticStore.resolvedColor(kind,i+1).hue>=0?CosmeticColorStore.color(LockerCosmeticStore.resolvedColor(kind,i+1),255):0xFFFFFFFF;c.fill(cx+cardW-16,cy+9,cx+cardW-8,cy+17,dot);if(p.rainbow)c.drawText(textRenderer,Text.literal("R"),cx+cardW-18,cy+25,0xFFFFFFFF,false);}
        }
        int by=y+h-53;String state=m.enabled?"ENABLED":"DISABLED";CincoDraw.bubble(c,x+w-145,by,x+w-24,by+28,8,m.enabled?0xFFB88CFF:0x667A7182,m.enabled?0xCC32184F:0xCC0A090D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(state,true),x+w-84,by+9,0xFFFFFFFF);
    }

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
        if(m==null)return false;int startX=x+20,startY=y+76,cols=4,gap=8,cardW=(w-64)/4,cardH=46;
        for(int i=0;i<names.length;i++){
            int cx=startX+(i%cols)*(cardW+gap),cy=startY+(i/cols)*(cardH+7);if(!inside(mx,my,cx,cy,cardW,cardH))continue;
            if(i<nativeCount&&button==1){if(client!=null)client.setScreen(new LockerItemSettingsScreen(kind,i+1,names[i]));return true;}
            if(button!=0)return false;
            if(i<nativeCount){m.setting=i+1;m.enabled=true;}else applyCombo(i-nativeCount);ConfigStore.save();return true;
        }
        int by=y+h-53;if(button==0&&inside(mx,my,x+w-145,by,121,28)){m.enabled=!m.enabled;ConfigStore.save();return true;}return false;
    }

    private boolean skinClick(double mx,double my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return false;int startX=x+28,startY=y+92,cardW=(w-80)/5;for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+6);if(inside(mx,my,cx,startY,cardW,180)){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}}int by=y+h-92;if(inside(mx,my,x+28,by,152,30)){m.setting2=m.setting2>=m.max2?m.min2:m.setting2+Math.max(1,m.step2);ConfigStore.save();return true;}return false;}
    private void applyCombo(int combo){ClientModule a=module("Custom Accessory"),e=module("Animations");if(a==null||e==null)return;a.enabled=true;e.enabled=true;switch(combo){case 0->{a.setting=3;e.setting=10;}case 1->{a.setting=2;e.setting=6;}case 2->{a.setting=12;e.setting=13;}default->{a.setting=13;e.setting=9;}}ConfigStore.save();}
    private ClientModule module(String n){return CincoClient.findModule(n);}
    private int skinColor(int i){return switch(i){case 0->0xFF4E7B3A;case 1->0xFFF06B2B;case 2->0xFF2B2C35;case 3->0xFF6C3A83;default->0xFF263A55;};}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
