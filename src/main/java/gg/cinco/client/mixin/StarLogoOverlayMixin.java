package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import gg.cinco.client.StarLogoTexture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Draws the user-provided Star Client logo over the dashboard branding area. */
@Mixin(value=CincoClickGuiScreen.class,remap=false)
public abstract class StarLogoOverlayMixin {
    @Inject(method="render",at=@At("TAIL"),remap=false,require=0)
    private void star$drawUploadedLogo(DrawContext c,int mx,int my,float delta,CallbackInfo ci){
        MinecraftClient mc=MinecraftClient.getInstance();if(mc==null||mc.getWindow()==null)return;
        int width=mc.getWindow().getScaledWidth(),height=mc.getWindow().getScaledHeight();
        int gw=Math.min(1080,width-18),gh=Math.min(650,height-18),gx=(width-gw)/2,gy=(height-gh)/2,leftW=Math.max(150,(int)(gw*.225));
        int x=gx+16,y=gy+11,w=Math.max(105,leftW-30),h=Math.max(58,Math.min(82,(int)(w*.58)));
        c.drawTexture(RenderPipelines.GUI_TEXTURED,StarLogoTexture.get(),x,y,0,0,w,h,256,150);
    }
}
