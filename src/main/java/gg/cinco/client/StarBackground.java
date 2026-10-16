package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Lightweight animated starfield shared by Star Client screens. */
public final class StarBackground {
    private static final int STAR_COUNT=64;
    private static final Star[] STARS=new Star[STAR_COUNT];
    private static final class Star{final double x,y,phase,fall,drift;final int size,shade;Star(double x,double y,double phase,double fall,double drift,int size,int shade){this.x=x;this.y=y;this.phase=phase;this.fall=fall;this.drift=drift;this.size=size;this.shade=shade;}}
    static{long seed=0x57A2C11EL;for(int i=0;i<STAR_COUNT;i++){seed=seed*6364136223846793005L+1442695040888963407L;double x=((seed>>>12)&0xFFFF)/65535.0;seed=seed*6364136223846793005L+1442695040888963407L;double y=((seed>>>12)&0xFFFF)/65535.0;double phase=i*.731+((seed>>>36)&0xFF)*.031;STARS[i]=new Star(x,y,phase,6.0+(i%7)*1.1,.35+(i%5)*.09,i%13==0?2:1,i%3);}}
    private StarBackground(){}

    public static void render(DrawContext c,int width,int height,float delta){
        if(width<=0||height<=0)return;ClientModule stars=CincoClient.findModule("Purple Stars");if(stars!=null&&!stars.enabled)return;
        MinecraftClient client=MinecraftClient.getInstance();int cap=Math.min(STAR_COUNT,StarPerformance.guiStarCap(client));
        if(stars!=null){double f=Math.max(.20,Math.min(1.0,stars.setting/Math.max(1.0,stars.max)));cap=Math.min(cap,Math.max(10,(int)Math.round(STAR_COUNT*f)));}
        int tier=StarPerformance.animationTier(client);double seconds=System.nanoTime()/1_000_000_000.0;
        for(int i=0;i<cap;i++){Star s=STARS[i];double motion=tier==0?0:seconds;double px=s.x*width+(tier==2?Math.sin(motion*s.drift+s.phase)*(2+s.size):0);double py=s.y*height+motion*s.fall;px%=width;py%=height;if(px<0)px+=width;if(py<0)py+=height;double tw=tier==0?.45:(Math.sin(seconds*1.7+s.phase)+1)*.5;int a=55+(int)Math.round(tw*105);int rgb=s.shade==1?0xB76CFF:s.shade==2?0x7C3AED:0x9B5CFF;int color=(Math.max(0,Math.min(255,a))<<24)|rgb;int x=(int)px,y=(int)py;c.fill(x,y,Math.min(width,x+s.size),Math.min(height,y+s.size),color);}
    }
}
