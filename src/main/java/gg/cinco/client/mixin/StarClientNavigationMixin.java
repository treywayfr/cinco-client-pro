package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import gg.cinco.client.ClientModule;
import gg.cinco.client.ClientSettingsScreen;
import gg.cinco.client.KeybindsScreen;
import gg.cinco.client.LockerScreen;
import gg.cinco.client.StarCrispUi;
import gg.cinco.client.StarFonts;
import gg.cinco.client.StarGuiModeStore;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Mixin(value = CincoClickGuiScreen.class, remap = false)
public abstract class StarClientNavigationMixin {
    private static final Set<String> HIDDEN = Set.of("Custom Accessory","Animations","Player Changer","GTA Skin Changer","Crystal","Mace");

    @Inject(method="nebulaModules",at=@At("RETURN"),cancellable=true,remap=false,require=0)
    private void star$hideLockerInternals(CallbackInfoReturnable<List<ClientModule>> cir){
        List<ClientModule> original=cir.getReturnValue();
        if(original==null)return;
        List<ClientModule> list=new ArrayList<>(original);
        list.removeIf(m->m!=null&&HIDDEN.contains(m.name));
        cir.setReturnValue(list);
    }

    @Inject(method="orbitModules",at=@At("RETURN"),cancellable=true,remap=false,require=0)
    private void star$hideLockerInternalsOrbit(String id,CallbackInfoReturnable<List<ClientModule>> cir){
        List<ClientModule> original=cir.getReturnValue();
        if(original==null)return;
        List<ClientModule> list=new ArrayList<>(original);
        list.removeIf(m->m!=null&&HIDDEN.contains(m.name));
        cir.setReturnValue(list);
    }

    @Inject(method="render",at=@At("TAIL"),remap=false,require=0)
    private void star$sidebarNavigation(DrawContext c,int mx,int my,float delta,CallbackInfo ci){
        if(StarGuiModeStore.get()!=StarGuiModeStore.Mode.NEBULA)return;
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc==null||mc.getWindow()==null||mc.textRenderer==null)return;
        int width=mc.getWindow().getScaledWidth(),height=mc.getWindow().getScaledHeight();
        int w=Math.min(950,Math.max(750,width-82)),h=Math.min(580,Math.max(440,height-84)),x=(width-w)/2,y=(height-h)/2,side=170;
        c.fill(x+10,y+h-105,x+side-10,y+h-8,0xFF040408);
        nav(c,mc,x+14,y+h-98,side-28,"✦  Locker",inside(mx,my,x+14,y+h-98,side-28,25));
        nav(c,mc,x+14,y+h-68,side-28,"⚙  Settings",inside(mx,my,x+14,y+h-68,side-28,25));
        nav(c,mc,x+14,y+h-38,side-28,"⌨  Keybinds",inside(mx,my,x+14,y+h-38,side-28,25));
    }

    private static void nav(DrawContext c,MinecraftClient mc,int x,int y,int w,String text,boolean hover){
        if(hover)gg.cinco.client.CincoDraw.rounded(c,x,y,x+w,y+25,7,0xB72A153F);
        c.drawText(mc.textRenderer,StarFonts.text(text,hover),x+8,y+8,hover?StarCrispUi.PURPLE_HI:StarCrispUi.TEXT,false);
    }

    @Inject(method="mouseClicked",at=@At("HEAD"),cancellable=true,remap=false,require=0)
    private void star$navigationClick(double mx,double my,int button,CallbackInfoReturnable<Boolean> cir){
        if(button!=0||StarGuiModeStore.get()!=StarGuiModeStore.Mode.NEBULA)return;
        MinecraftClient mc=MinecraftClient.getInstance();
        if(mc==null||mc.getWindow()==null)return;
        int width=mc.getWindow().getScaledWidth(),height=mc.getWindow().getScaledHeight();
        int w=Math.min(950,Math.max(750,width-82)),h=Math.min(580,Math.max(440,height-84)),x=(width-w)/2,y=(height-h)/2,side=170;
        if(inside(mx,my,x+14,y+h-98,side-28,25)){mc.setScreen(new LockerScreen());cir.setReturnValue(true);return;}
        if(inside(mx,my,x+14,y+h-68,side-28,25)){mc.setScreen(new ClientSettingsScreen());cir.setReturnValue(true);return;}
        if(inside(mx,my,x+14,y+h-38,side-28,25)){mc.setScreen(new KeybindsScreen());cir.setReturnValue(true);}
    }

    private static boolean inside(double mx,double my,int x,int y,int w,int h){return mx>=x&&mx<=x+w&&my>=y&&my<=y+h;}
}
