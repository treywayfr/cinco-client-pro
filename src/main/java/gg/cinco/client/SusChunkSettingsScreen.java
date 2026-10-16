package gg.cinco.client;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/** Dedicated filters/presentation screen for Sus Chunk. */
public final class SusChunkSettingsScreen extends StarLegacyScreen {
    private static final String[] LABELS={"Rotated deepslate","Cobbled deepslate","Fully grown crops","Cactus","Kelp","Amethyst clusters"};
    private static final int[] COLORS={0xFFB05CFF,0xFFFF5C93,0xFFFFB84D,0xFF59E391,0xFF54C7FF,0xFFFFFFFF,0xFFFF5555,0xFF7B74FF};
    private int x,y,w,h;

    public SusChunkSettingsScreen(){super(Text.literal("Sus Chunk Settings"));}
    @Override public boolean shouldPause(){return false;}

    @Override public void render(DrawContext c,int mx,int my,float delta){
        c.fill(0,0,width,height,0xFF020204);StarBackground.render(c,width,height,delta);
        w=Math.min(660,width-42);h=Math.min(440,height-36);x=(width-w)/2;y=(height-h)/2;
        StarCrispUi.panel(c,x,y,w,h,13,false);
        c.drawText(textRenderer,StarFonts.text("SUS CHUNK",true),x+22,y+20,StarCrispUi.PURPLE_HI,false);
        c.drawText(textRenderer,StarFonts.text("Loaded-chunk player-activity signals",false),x+22,y+38,0xFFD8D1DF,false);
        SusChunkConfig cfg=SusChunkConfig.get();
        int left=x+24, yy=y+72;
        for(int i=0;i<LABELS.length;i++){
            boolean on=get(i,cfg);int rowY=yy+i*40;
            StarCrispUi.card(c,left,rowY,w/2-36,31,on,inside(mx,my,left,rowY,w/2-36,31));
            c.drawText(textRenderer,StarFonts.text(LABELS[i],on),left+12,rowY+11,on?0xFFFFFFFF:0xFFD5CFDB,false);
            StarCrispUi.miniToggle(c,left+w/2-68,rowY+8,on,on?1f:0f);
        }
        int rx=x+w/2+8;
        c.drawText(textRenderer,StarFonts.text("HIGHLIGHT MODE",true),rx,y+74,0xFFF0EAF6,false);
        option(c,rx,y+96,w/2-34,"Suspicious blocks",cfg.highlightBlocks,mx,my);
        option(c,rx,y+134,w/2-34,"Top of suspicious chunk",!cfg.highlightBlocks,mx,my);
        c.drawText(textRenderer,StarFonts.text("HIGHLIGHT COLOUR",true),rx,y+188,0xFFF0EAF6,false);
        for(int i=0;i<COLORS.length;i++){
            int sx=rx+(i%4)*54,sy=y+212+(i/4)*42;int col=COLORS[i];
            CincoDraw.bubble(c,sx,sy,sx+42,sy+28,8,(cfg.color&0xFFFFFF)==(col&0xFFFFFF)?0xFFFFFFFF:0x88FFFFFF,col);
        }
        c.drawText(textRenderer,StarFonts.text("Right-click Sus Chunk anytime to reopen this screen.",false),x+22,y+h-28,0xFFBDB4C5,false);
    }
    private void option(DrawContext c,int ox,int oy,int ow,String label,boolean selected,int mx,int my){StarCrispUi.card(c,ox,oy,ow,30,selected,inside(mx,my,ox,oy,ow,30));c.drawText(textRenderer,StarFonts.text(label,selected),ox+12,oy+10,selected?0xFFFFFFFF:0xFFD5CFDB,false);}
    @Override public boolean mouseClicked(double mx,double my,int button){if(button!=0)return false;SusChunkConfig cfg=SusChunkConfig.get();int left=x+24,yy=y+72;for(int i=0;i<LABELS.length;i++){int ry=yy+i*40;if(inside(mx,my,left,ry,w/2-36,31)){set(i,cfg,!get(i,cfg));SusChunkConfig.save();StarWorldFeatures.reset();return true;}}int rx=x+w/2+8;if(inside(mx,my,rx,y+96,w/2-34,30)){cfg.highlightBlocks=true;SusChunkConfig.save();return true;}if(inside(mx,my,rx,y+134,w/2-34,30)){cfg.highlightBlocks=false;SusChunkConfig.save();return true;}for(int i=0;i<COLORS.length;i++){int sx=rx+(i%4)*54,sy=y+212+(i/4)*42;if(inside(mx,my,sx,sy,42,28)){cfg.color=COLORS[i];SusChunkConfig.save();return true;}}return false;}
    private boolean get(int i,SusChunkConfig c){return switch(i){case 0->c.rotatedDeepslate;case 1->c.cobbledDeepslate;case 2->c.matureCrops;case 3->c.cactus;case 4->c.kelp;default->c.amethyst;};}
    private void set(int i,SusChunkConfig c,boolean v){switch(i){case 0->c.rotatedDeepslate=v;case 1->c.cobbledDeepslate=v;case 2->c.matureCrops=v;case 3->c.cactus=v;case 4->c.kelp=v;default->c.amethyst=v;}}
    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah){return mx>=ax&&mx<=ax+aw&&my>=ay&&my<=ay+ah;}
}
