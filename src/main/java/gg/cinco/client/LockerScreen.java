package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Star Locker: one polished home for accessories, cosmetic effects and local skins. */
public final class LockerScreen extends StarLegacyScreen {
    private static final String[] TABS={"Accessories","Cosmetics","Skins"};
    private static final String[] ACCESSORIES={"Angel Wings","Demon Wings","Solid Halo","Star Crown","Orbit Crystals","Nebula Horns","Energy Ring","Nebula Cape","Dragon Wings","Butterfly Wings","Shoulder Orbs","Galaxy Crown","Back Blades","Floating Crystals"};
    private static final String[] EFFECTS={"Aura Rings","Purple Aura","Jump Circles","Speed Trails","Orbit Runes","Lightning Sparks","Footstep Rings","Cosmic Pulse","Spiral Aura","Star Orbit","Comet Trail","Shockwave","Galaxy Cage","Crown Aura","Celestial Set","Demon Set","Nebula Set","Void Set"};
    private static final String[] SKINS={"Peter Griffin","Bart Simpson","Street Hero","Anime Fighter","CJ / San Andreas"};
    private int tab,x,y,w,h;

    public LockerScreen(){super(Text.literal("Star Locker"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(830,width-46);h=Math.min(520,height-38);x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦  STAR LOCKER",true),x+24,y+20,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("Build your look",false),x+24,y+39,StarCrispUi.PURPLE_HI,false);
        int tabsX=x+w-390;
        for(int i=0;i<3;i++){int tx=tabsX+i*122;boolean sel=i==tab;StarCrispUi.card(c,tx,y+16,112,34,sel,inside(mx,my,tx,y+16,112,34));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(TABS[i],sel),tx+56,y+28,sel?0xFFFFFFFF:0xFFD5CEDB);}
        StarCrispUi.divider(c,x+20,y+62,x+w-20);
        if(tab==0)grid(c,mx,my,ACCESSORIES,module("Custom Accessory"),14,false);
        else if(tab==1)grid(c,mx,my,EFFECTS,module("Animations"),14,true);
        else skins(c,mx,my);
        c.drawText(textRenderer,StarFonts.text("Locker cosmetics are local-only • clean client-side presentation",false),x+24,y+h-25,0xFFAAA1B2,false);
    }

    private void grid(DrawContext c,int mx,int my,String[] names,ClientModule m,int nativeCount,boolean effects){
        if(m==null)return;int startX=x+24,startY=y+84,cols=3,gap=12,cardW=(w-72)/3,cardH=54;
        for(int i=0;i<names.length;i++){int col=i%cols,row=i/cols,cx=startX+col*(cardW+gap),cy=startY+row*(cardH+9);boolean nativeItem=i<nativeCount;boolean selected=nativeItem&&m.enabled&&(int)Math.round(m.setting)==i+1;boolean hov=inside(mx,my,cx,cy,cardW,cardH);StarCrispUi.card(c,cx,cy,cardW,cardH,selected,hov);if(selected)StarCrispUi.accentRail(c,cx+2,cy+9,36);c.drawText(textRenderer,StarFonts.text(names[i],selected),cx+14,cy+13,selected?0xFFFFFFFF:0xFFE4DDE8,false);c.drawText(textRenderer,StarFonts.text(nativeItem?(selected?"EQUIPPED":"Equip cosmetic"):"Apply combo preset",false),cx+14,cy+31,selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2,false);}
        int by=y+h-58;String state=m.enabled?"ENABLED":"DISABLED";CincoDraw.bubble(c,x+w-145,by,x+w-24,by+28,8,m.enabled?0xFFB88CFF:0x667A7182,m.enabled?0xCC32184F:0xCC0A090D);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(state,true),x+w-84,by+9,0xFFFFFFFF);
    }

    private void skins(DrawContext c,int mx,int my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return;int startX=x+28,startY=y+92,cardW=(w-80)/5;
        for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+6),cy=startY;boolean selected=m.enabled&&(int)Math.round(m.setting)==i+1;StarCrispUi.card(c,cx,cy,cardW,180,selected,inside(mx,my,cx,cy,cardW,180));CincoDraw.rounded(c,cx+12,cy+14,cx+cardW-12,cy+116,12,skinColor(i));c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(i==4?"CJ":i==1?"BART":i==0?"PETER":"✦",true),cx+cardW/2,cy+59,0xFFFFFFFF);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(SKINS[i],selected),cx+cardW/2,cy+132,selected?0xFFFFFFFF:0xFFD9D1DF);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(selected?"EQUIPPED":"SELECT",false),cx+cardW/2,cy+153,selected?StarCrispUi.PURPLE_HI:0xFFAAA1B2);}
        int by=y+h-92;String outfit="OUTFIT  "+Math.max(1,(int)Math.round(m.setting2));CincoDraw.bubble(c,x+28,by,x+180,by+30,8,0xAFFFFFFF,0xD80A080F);c.drawCenteredTextWithShadow(textRenderer,StarFonts.text(outfit,true),x+104,by+10,0xFFFFFFFF);c.drawText(textRenderer,StarFonts.text("Click Outfit to cycle the selected skin's wardrobe",false),x+196,by+11,0xFFBDB4C5,false);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){if(button!=0)return false;int tabsX=x+w-390;for(int i=0;i<3;i++){int tx=tabsX+i*122;if(inside(mx,my,tx,y+16,112,34)){tab=i;return true;}}if(tab==0)return gridClick(mx,my,ACCESSORIES,module("Custom Accessory"),14,false);if(tab==1)return gridClick(mx,my,EFFECTS,module("Animations"),14,true);return skinClick(mx,my);}
    private boolean gridClick(double mx,double my,String[] names,ClientModule m,int nativeCount,boolean effects){if(m==null)return false;int startX=x+24,startY=y+84,cols=3,gap=12,cardW=(w-72)/3,cardH=54;for(int i=0;i<names.length;i++){int cx=startX+(i%cols)*(cardW+gap),cy=startY+(i/cols)*(cardH+9);if(!inside(mx,my,cx,cy,cardW,cardH))continue;if(i<nativeCount){m.setting=i+1;m.enabled=true;}else applyCombo(i-nativeCount);ConfigStore.save();return true;}int by=y+h-58;if(inside(mx,my,x+w-145,by,121,28)){m.enabled=!m.enabled;ConfigStore.save();return true;}return false;}
    private boolean skinClick(double mx,double my){ClientModule m=module("Player Changer");if(m==null)m=module("GTA Skin Changer");if(m==null)return false;int startX=x+28,startY=y+92,cardW=(w-80)/5;for(int i=0;i<SKINS.length;i++){int cx=startX+i*(cardW+6);if(inside(mx,my,cx,startY,cardW,180)){m.setting=i+1;m.enabled=true;ConfigStore.save();return true;}}int by=y+h-92;if(inside(mx,my,x+28,by,152,30)){m.setting2=m.setting2>=m.max2?m.min2:m.setting2+Math.max(1,m.step2);ConfigStore.save();return true;}return false;}
    private void applyCombo(int combo){ClientModule a=module("Custom Accessory"),e=module("Animations");if(a==null||e==null)return;a.enabled=true;e.enabled=true;switch(combo){case 0->{a.setting=3;e.setting=10;}case 1->{a.setting=2;e.setting=6;}case 2->{a.setting=12;e.setting=13;}default->{a.setting=13;e.setting=9;}}ConfigStore.save();}
    private ClientModule module(String n){return CincoClient.findModule(n);}
    private int skinColor(int i){return switch(i){case 0->0xFF4E7B3A;case 1->0xFFF06B2B;case 2->0xFF2B2C35;case 3->0xFF6C3A83;default->0xFF263A55;};}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
