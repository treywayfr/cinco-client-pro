package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Right-click editor for one Locker accessory or cosmetic effect. */
public final class LockerItemSettingsScreen extends StarLegacyScreen {
    private final String kind;
    private final int style;
    private final String itemName;
    private int x,y,w,h;

    public LockerItemSettingsScreen(String kind,int style,String itemName){
        super(Text.literal("Locker Item Settings"));
        this.kind=kind;this.style=style;this.itemName=itemName;
    }

    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(610,width-44);h=Math.min(470,height-38);x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,14,false);
        c.drawText(textRenderer,StarFonts.text("✦  "+itemName,true),x+24,y+22,0xFFFFFFFF,false);
        c.drawText(textRenderer,StarFonts.text("RIGHT-CLICK CUSTOMIZATION • "+kind.toUpperCase(),false),x+24,y+42,StarCrispUi.PURPLE_HI,false);
        StarCrispUi.divider(c,x+20,y+64,x+w-20);

        LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,style);
        int sy=y+88;
        slider(c,mx,my,"Hue",p.hue,0,360,sy,huePreview(p)); sy+=58;
        slider(c,mx,my,"Saturation",p.saturation,0,1,sy,0xFFB77CFF); sy+=58;
        slider(c,mx,my,"Brightness",p.brightness,0.15,1,sy,0xFFFFFFFF); sy+=58;
        slider(c,mx,my,"Size",p.size,0.35,2.75,sy,0xFF8DD8FF); sy+=58;
        slider(c,mx,my,"Rainbow Speed",p.rainbowSpeed,0.05,2.5,sy,0xFFFF87DA);

        int by=y+h-54;
        CincoDraw.bubble(c,x+24,by,x+210,by+32,9,p.rainbow?0xFFB88CFF:0x667A7182,p.rainbow?0xCC32184F:0xCC0A090D);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("RAINBOW  "+(p.rainbow?"ON":"OFF"),true),x+117,by+11,0xFFFFFFFF);
        CincoDraw.bubble(c,x+w-124,by,x+w-24,by+32,9,0x88FFFFFF,0xCC09070E);
        c.drawCenteredTextWithShadow(textRenderer,StarFonts.text("DONE",true),x+w-74,by+11,0xFFFFFFFF);
    }

    private void slider(DrawContext c,int mx,int my,String label,double value,double min,double max,int yy,int accent){
        int bx=x+190,bw=w-230;
        c.drawText(textRenderer,StarFonts.text(label,true),x+28,yy,0xFFEFEAF4,false);
        String v=label.equals("Hue")?String.format("%.0f°",value):String.format("%.2f",value);
        c.drawText(textRenderer,StarFonts.text(v,false),x+28,yy+18,0xFFAAA1B2,false);
        int barY=yy+16;
        c.fill(bx,barY,bx+bw,barY+8,0x663B3144);
        double t=(value-min)/(max-min);int fill=(int)Math.round(bw*Math.max(0,Math.min(1,t)));
        c.fill(bx,barY,bx+fill,barY+8,accent);
        int kx=bx+fill;
        c.fill(kx-2,barY-4,kx+2,barY+12,0xFFFFFFFF);
        if(mx>=bx&&mx<=bx+bw&&my>=barY-8&&my<=barY+16)c.fill(bx,barY-1,bx+bw,barY,0x99FFFFFF);
    }

    @Override public boolean mouseClicked(double mx,double my,int button){
        if(button!=0)return false;
        LockerCosmeticStore.ItemProfile p=LockerCosmeticStore.get(kind,style);
        int sy=y+88;
        if(setSlider(mx,my,sy,0,360,v->p.hue=v))return saved();sy+=58;
        if(setSlider(mx,my,sy,0,1,v->p.saturation=v))return saved();sy+=58;
        if(setSlider(mx,my,sy,0.15,1,v->p.brightness=v))return saved();sy+=58;
        if(setSlider(mx,my,sy,0.35,2.75,v->p.size=v))return saved();sy+=58;
        if(setSlider(mx,my,sy,0.05,2.5,v->p.rainbowSpeed=v))return saved();
        int by=y+h-54;
        if(inside(mx,my,x+24,by,186,32)){p.rainbow=!p.rainbow;return saved();}
        if(inside(mx,my,x+w-124,by,100,32)){if(client!=null)client.setScreen(new LockerScreen());return true;}
        return false;
    }

    private boolean setSlider(double mx,double my,int yy,double min,double max,Setter setter){
        int bx=x+190,bw=w-230,barY=yy+16;
        if(!inside(mx,my,bx,barY-10,bw,28))return false;
        double t=Math.max(0,Math.min(1,(mx-bx)/bw));setter.set(min+(max-min)*t);return true;
    }
    private boolean saved(){LockerCosmeticStore.save();return true;}
    private int huePreview(LockerCosmeticStore.ItemProfile p){return CosmeticColorStore.color(new CosmeticColorStore.Profile(p.hue,p.saturation,p.brightness),255);}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
    @FunctionalInterface private interface Setter{void set(double v);}
}
