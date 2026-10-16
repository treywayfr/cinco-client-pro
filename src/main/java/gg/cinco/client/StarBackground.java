package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/** Lightweight animated galaxy background for Star Client screens. */
public final class StarBackground {
    private static final int STAR_COUNT = 72;
    private static final Star[] STARS = new Star[STAR_COUNT];
    private static final class Star {
        final double x,y,phase,driftX,driftY;
        final int size,shade;
        Star(double x,double y,double phase,double driftX,double driftY,int size,int shade){
            this.x=x;this.y=y;this.phase=phase;this.driftX=driftX;this.driftY=driftY;this.size=size;this.shade=shade;
        }
    }
    static {
        long seed=0x57A2C11EL;
        for(int i=0;i<STAR_COUNT;i++){
            seed=seed*6364136223846793005L+1442695040888963407L;
            double x=((seed>>>12)&0xFFFF)/65535.0;
            seed=seed*6364136223846793005L+1442695040888963407L;
            double y=((seed>>>12)&0xFFFF)/65535.0;
            double phase=i*.731+((seed>>>36)&0xFF)*.031;
            STARS[i]=new Star(x,y,phase,.35+(i%5)*.08,.12+(i%7)*.035,i%17==0?2:1,i%4);
        }
    }
    private StarBackground(){}

    public static void render(DrawContext c,int width,int height,float delta){
        if(width<=0||height<=0)return;
        MinecraftClient client=MinecraftClient.getInstance();
        ClientModule stars=CincoClient.findModule("Purple Stars");
        int tier=StarPerformance.animationTier(client);
        double sec=System.nanoTime()/1_000_000_000.0;

        // Deep space base.
        c.fill(0,0,width,height,0xFF03020A);
        c.fill(0,height/3,width,height,0xFF080516);

        // Low-cost animated nebula clouds. Large, translucent rounded blobs give the same rich
        // galaxy feeling as the mockup without a shader or full-screen texture upload every frame.
        double motion=tier==0?0:sec;
        int drift=(int)Math.round(Math.sin(motion*.10)*22.0);
        nebula(c,width,height,drift);
        planets(c,width,height,motion,tier);

        if(stars!=null&&!stars.enabled)return;
        int cap=Math.min(STAR_COUNT,StarPerformance.guiStarCap(client)+12);
        if(stars!=null){double f=Math.max(.20,Math.min(1.0,stars.setting/Math.max(1.0,stars.max)));cap=Math.min(cap,Math.max(12,(int)Math.round(STAR_COUNT*f)));}
        for(int i=0;i<cap;i++){
            Star s=STARS[i];
            double px=s.x*width+(tier==2?Math.sin(motion*s.driftX+s.phase)*5.0:0);
            double py=s.y*height+(tier==2?Math.cos(motion*s.driftY+s.phase)*3.0:0);
            double tw=tier==0?.50:(Math.sin(sec*1.9+s.phase)+1)*.5;
            int a=70+(int)Math.round(tw*135);
            int rgb=s.shade==0?0xFFFFFF:s.shade==1?0xFF72E6:s.shade==2?0xB66CFF:0x8FA8FF;
            int col=(Math.max(0,Math.min(255,a))<<24)|rgb;
            int x=(int)px,y=(int)py;
            c.fill(x,y,Math.min(width,x+s.size),Math.min(height,y+s.size),col);
            if(s.size>1){c.fill(x-2,y,x+4,y+1,(a/2<<24)|rgb);c.fill(x,y-2,x+1,y+4,(a/2<<24)|rgb);}
        }
    }

    private static void nebula(DrawContext c,int w,int h,int drift){
        CincoDraw.rounded(c,-w/7+drift,h/12,w/2+drift,h/2,Math.max(80,h/5),0x342B0A63);
        CincoDraw.rounded(c,w/5-drift/2,h/9,w*4/5-drift/2,h*3/5,Math.max(90,h/4),0x263E0A7A);
        CincoDraw.rounded(c,w/2+drift,h/3,w+w/6+drift,h+20,Math.max(100,h/3),0x2F6C0A80);
        CincoDraw.rounded(c,w/3,h*2/5,w*9/10,h+40,Math.max(100,h/3),0x202A4CB2);
        CincoDraw.rounded(c,w/2-140+drift/3,h/2-70,w/2+170+drift/3,h/2+120,90,0x1EFF4BD8);
    }

    private static void planets(DrawContext c,int w,int h,double sec,int tier){
        int ox=tier==2?(int)Math.round(Math.sin(sec*.08)*8):0;
        // Upper-left violet planet with halo.
        int p1=Math.max(54,Math.min(w,h)/9);
        CincoDraw.rounded(c,-p1/2+ox,-p1/3,p1+ox,p1, p1/2,0x245A2C88);
        CincoDraw.rounded(c,-p1/3+ox,-p1/5,p1*3/4+ox,p1*4/5,p1/2,0x70401875);
        CincoDraw.rounded(c,-p1/5+ox,0,p1/2+ox,p1*2/3,p1/2,0xA0201038);
        // Lower-right large rim-lit planet.
        int p2=Math.max(88,Math.min(w,h)/6);
        int x=w-p2/2-24+ox/2,y=h-p2/2+18;
        CincoDraw.rounded(c,x-8,y-8,x+p2+8,y+p2+8,p2/2+8,0x2AFF56D5);
        CincoDraw.rounded(c,x,y,x+p2,y+p2,p2/2,0xD00A0618);
        CincoDraw.rounded(c,x+10,y+8,x+p2-18,y+p2-20,p2/2,0xB01A0C34);
    }
}
