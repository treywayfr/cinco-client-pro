package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.Random;

/** Cached procedural galaxy scene matched to the approved Star Client reference. */
public final class StarBackground {
    private static final int TEX_W = 512;
    private static final int TEX_H = 288;
    private static Identifier galaxyTexture;
    private static final Star[] OVERLAY = new Star[34];
    private static final Orbiter[] ORBITERS = new Orbiter[]{
            new Orbiter(.05,.13,.095,0xFF8B4BFF,.010,0.4),
            new Orbiter(.91,.26,.072,0xFFFF4FD6,-.008,2.1),
            new Orbiter(.78,.83,.052,0xFF5E82FF,.006,4.0)
    };

    private record Star(double x,double y,double phase,int size,int rgb,double speed) {}
    private record Orbiter(double x,double y,double size,int rgb,double speed,double phase) {}

    static {
        Random r = new Random(0x57A2C11EL);
        for (int i=0;i<OVERLAY.length;i++) {
            int rgb = switch (i%4) { case 0 -> 0xFFFFFF; case 1 -> 0xFF83E8; case 2 -> 0xC692FF; default -> 0x8DB7FF; };
            double speed=(i%2==0?1:-1)*(.0014+r.nextDouble()*.0036);
            OVERLAY[i] = new Star(r.nextDouble(), r.nextDouble(), r.nextDouble()*Math.PI*2, i%9==0?2:1, rgb, speed);
        }
    }

    private StarBackground() {}

    public static void render(DrawContext c,int width,int height,float delta) {
        if (width<=0||height<=0) return;
        ensureGalaxyTexture();
        if (galaxyTexture != null) {
            c.drawTexture(RenderPipelines.GUI_TEXTURED, galaxyTexture,
                    0,0,0,0,width,height,TEX_W,TEX_H,0xFFFFFFFF);
        } else {
            c.fillGradient(0,0,width,height,0xFF05020E,0xFF120629);
        }

        MinecraftClient client=MinecraftClient.getInstance();
        int tier=StarPerformance.animationTier(client);
        if (tier<=0) return;
        double sec=System.nanoTime()/1_000_000_000.0;

        // Very low-cost breathing nebula glows. These sit behind the glass and move slowly.
        int breathe=(int)(13+9*(.5+.5*Math.sin(sec*.20)));
        int nebulaSize=Math.max(80,Math.min(width,height)/3);
        int nx=(int)(width*(.42+.055*Math.sin(sec*.055)));
        int ny=(int)(height*(.28+.045*Math.cos(sec*.047)));
        CincoDraw.rounded(c,nx-nebulaSize/2,ny-nebulaSize/2,nx+nebulaSize/2,ny+nebulaSize/2,
                nebulaSize/2,(breathe<<24)|0x00B54CFF);
        if(tier>1){
            int n2=(int)(nebulaSize*.72);
            int nx2=(int)(width*(.67+.04*Math.cos(sec*.041)));
            int ny2=(int)(height*(.68+.05*Math.sin(sec*.049)));
            CincoDraw.rounded(c,nx2-n2/2,ny2-n2/2,nx2+n2/2,ny2+n2/2,n2/2,
                    ((breathe+3)<<24)|0x00FF45CB);
        }

        // Actual parallax movement, not only twinkling.
        int cap=tier==1?16:OVERLAY.length;
        for(int i=0;i<cap;i++) {
            Star s=OVERLAY[i];
            double tw=.55+.45*Math.sin(sec*1.6+s.phase);
            int a=(int)(45+145*tw);
            double px=s.x+sec*s.speed;
            px=px-Math.floor(px);
            double py=s.y+Math.sin(sec*.11+s.phase)*.008;
            py=py-Math.floor(py);
            int x=(int)(px*width), y=(int)(py*height);
            int col=(a<<24)|s.rgb;
            c.fill(x,y,x+s.size,y+s.size,col);
            if(s.size>1){
                c.fill(x-2,y,x+4,y+1,(Math.max(1,a/2)<<24)|s.rgb);
                c.fill(x,y-2,x+1,y+4,(Math.max(1,a/2)<<24)|s.rgb);
            }
        }

        // Slow edge-orbiting planets. Kept subtle so they never obscure controls.
        if(tier>1){
            for(Orbiter o:ORBITERS){
                double phase=sec*o.speed+o.phase;
                int size=Math.max(22,(int)(Math.min(width,height)*o.size));
                int x=(int)((o.x+.018*Math.sin(phase))*width);
                int y=(int)((o.y+.025*Math.cos(phase*.81))*height);
                int glow=Math.max(3,size/10);
                CincoDraw.rounded(c,x-size/2-glow,y-size/2-glow,x+size/2+glow,y+size/2+glow,
                        size/2+glow,0x213E1A72|(o.rgb&0x00FFFFFF));
                CincoDraw.rounded(c,x-size/2,y-size/2,x+size/2,y+size/2,size/2,
                        0xB0000000|(o.rgb&0x00FFFFFF));
                int inner=Math.max(8,(int)(size*.78));
                CincoDraw.rounded(c,x-inner/2,y-inner/2,x+inner/2,y+inner/2,inner/2,
                        0xB2080614);
                int rim=Math.max(1,size/22);
                CincoDraw.rounded(c,x-size/2-rim,y-size/2-rim,x+size/2+rim,y+size/2+rim,size/2+rim,
                        0x46FFFFFF);
                CincoDraw.rounded(c,x-size/2,y-size/2,x+size/2,y+size/2,size/2,
                        0x9A000000|(o.rgb&0x00FFFFFF));
            }
        }
    }

    private static void ensureGalaxyTexture() {
        if (galaxyTexture != null) return;
        MinecraftClient client=MinecraftClient.getInstance();
        if(client==null||client.getTextureManager()==null)return;

        NativeImage image=generateGalaxy();
        Identifier id=Identifier.of("cinco_client","dynamic/reference_galaxy_v2");
        NativeImageBackedTexture texture=new NativeImageBackedTexture(() -> "Star Reference Galaxy",image);
        client.getTextureManager().registerTexture(id,texture);
        texture.upload();
        galaxyTexture=id;
    }

    private static NativeImage generateGalaxy() {
        NativeImage img=new NativeImage(TEX_W,TEX_H,false);
        Random rand=new Random(0x5A7C11EEL);

        for(int y=0;y<TEX_H;y++) {
            double ny=y/(double)(TEX_H-1);
            for(int x=0;x<TEX_W;x++) {
                double nx=x/(double)(TEX_W-1);
                double noise=noise(nx*9.0,ny*9.0);
                double fine=noise(nx*27.0+13.1,ny*27.0-7.7);

                int r=3,g=2,b=12;

                double center=.12+.68*nx + .055*Math.sin(nx*8.0);
                double d=Math.abs(ny-center);
                double band=Math.exp(-(d*d)/.018)*(0.55+0.45*noise);
                r += (int)(72*band);
                g += (int)(23*band);
                b += (int)(115*band);

                double mag = cloud(nx,ny,.55,.22,.18,.13)+cloud(nx,ny,.79,.54,.23,.20)+cloud(nx,ny,.36,.64,.22,.16);
                double blue= cloud(nx,ny,.31,.37,.28,.22)+cloud(nx,ny,.67,.18,.24,.17);
                r += (int)(96*mag + 24*blue);
                g += (int)(18*mag + 38*blue);
                b += (int)(94*mag + 96*blue);

                double dust=Math.max(0,(fine-.56))*55.0*band;
                r+=(int)dust; g+=(int)(dust*.35); b+=(int)(dust*.9);

                double vx=(nx-.5)*2.0, vy=(ny-.5)*2.0;
                double vign=Math.max(0,Math.min(1,(vx*vx+vy*vy)*.33));
                r=(int)(r*(1-.42*vign));g=(int)(g*(1-.42*vign));b=(int)(b*(1-.30*vign));

                img.setColorArgb(x,y,0xFF000000|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b));
            }
        }

        for(int i=0;i<420;i++) {
            int x=rand.nextInt(TEX_W),y=rand.nextInt(TEX_H);
            int a=120+rand.nextInt(136);
            int rgb=switch(i%5){case 0->0xFFFFFF;case 1->0xFFD9FA;case 2->0xDCA9FF;case 3->0x9FD9FF;default->0xFF9DEB;};
            put(img,x,y,(a<<24)|rgb);
            if(i%27==0){put(img,x-1,y,0x80FFFFFF);put(img,x+1,y,0x80FFFFFF);put(img,x,y-1,0x80FFFFFF);put(img,x,y+1,0x80FFFFFF);}
        }

        planet(img,-18,10,105,0xFF421A72,0xFF10061E,0xFFFF4FE1);
        planet(img,TEX_W+17,95,88,0xFF291052,0xFF070411,0xFFB94FFF);
        planet(img,TEX_W-74,TEX_H-38,58,0xFF30104B,0xFF090412,0xFFFF4FCB);

        return img;
    }

    private static void planet(NativeImage img,int cx,int cy,int radius,int light,int dark,int rim) {
        int x0=Math.max(0,cx-radius-4),x1=Math.min(TEX_W-1,cx+radius+4);
        int y0=Math.max(0,cy-radius-4),y1=Math.min(TEX_H-1,cy+radius+4);
        for(int y=y0;y<=y1;y++)for(int x=x0;x<=x1;x++){
            double dx=x-cx,dy=y-cy,dist=Math.sqrt(dx*dx+dy*dy);
            if(dist>radius+3)continue;
            if(dist>radius){
                double a=(radius+3-dist)/3.0*.7;
                blend(img,x,y,rim,a);
                continue;
            }
            double nx=dx/radius,ny=dy/radius;
            double shade=Math.max(0,Math.min(1,.56-.48*nx-.32*ny));
            int col=mix(dark,light,shade);
            double edge=Math.max(0,(dist/radius-.82)/.18);
            col=mix(col,rim,edge*.34);
            blend(img,x,y,col,.94);
        }
    }

    private static double cloud(double x,double y,double cx,double cy,double sx,double sy){
        double dx=(x-cx)/sx,dy=(y-cy)/sy;return Math.exp(-(dx*dx+dy*dy)*1.7);
    }
    private static double noise(double x,double y){
        double v=Math.sin(x*12.9898+y*78.233)*43758.5453;return v-Math.floor(v);
    }
    private static int mix(int a,int b,double t){t=Math.max(0,Math.min(1,t));int ar=(a>>16)&255,ag=(a>>8)&255,ab=a&255,br=(b>>16)&255,bg=(b>>8)&255,bb=b&255;return 0xFF000000|((int)(ar+(br-ar)*t)<<16)|((int)(ag+(bg-ag)*t)<<8)|(int)(ab+(bb-ab)*t);}
    private static void blend(NativeImage img,int x,int y,int color,double alpha){if(x<0||y<0||x>=TEX_W||y>=TEX_H)return;int base=img.getColorArgb(x,y);int br=(base>>16)&255,bg=(base>>8)&255,bb=base&255,cr=(color>>16)&255,cg=(color>>8)&255,cb=color&255;int r=(int)(br+(cr-br)*alpha),g=(int)(bg+(cg-bg)*alpha),b=(int)(bb+(cb-bb)*alpha);img.setColorArgb(x,y,0xFF000000|(clamp(r)<<16)|(clamp(g)<<8)|clamp(b));}
    private static void put(NativeImage img,int x,int y,int color){if(x>=0&&y>=0&&x<TEX_W&&y<TEX_H)img.setColorArgb(x,y,color);}
    private static int clamp(int v){return Math.max(0,Math.min(255,v));}
}
