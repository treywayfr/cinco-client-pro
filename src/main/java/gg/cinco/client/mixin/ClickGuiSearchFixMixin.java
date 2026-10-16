package gg.cinco.client.mixin;

import gg.cinco.client.CincoClickGuiScreen;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Reliable search focus/input for the dense Star ClickGUI. */
@Mixin(value=CincoClickGuiScreen.class,remap=false)
public abstract class ClickGuiSearchFixMixin {
    @Shadow private String search;
    @Shadow private boolean searchFocused;
    @Shadow private int moduleScroll;
    @Shadow private int modX;
    @Shadow private int searchY;
    @Shadow private int modW;

    @Inject(method="mouseClicked(DDI)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private void star$focusSearch(double mx,double my,int button,CallbackInfoReturnable<Boolean> cir){
        if(button!=0)return;
        // Slightly oversized hit target prevents edge misses at non-integer GUI scales.
        if(mx>=modX-5&&mx<=modX+modW+5&&my>=searchY-5&&my<=searchY+34){
            searchFocused=true;moduleScroll=0;cir.setReturnValue(true);
        }
    }

    @Inject(method="keyPressed(III)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private void star$searchKeys(int key,int scan,int modifiers,CallbackInfoReturnable<Boolean> cir){
        if((modifiers&GLFW.GLFW_MOD_CONTROL)!=0&&key==GLFW.GLFW_KEY_F){searchFocused=true;moduleScroll=0;cir.setReturnValue(true);return;}
        if(!searchFocused)return;
        if(key==GLFW.GLFW_KEY_BACKSPACE){if(!search.isEmpty())search=search.substring(0,search.length()-1);moduleScroll=0;cir.setReturnValue(true);return;}
        if(key==GLFW.GLFW_KEY_DELETE){search="";moduleScroll=0;cir.setReturnValue(true);return;}
        if(key==GLFW.GLFW_KEY_ENTER||key==GLFW.GLFW_KEY_KP_ENTER){searchFocused=false;cir.setReturnValue(true);}
    }

    @Inject(method="charTyped(CI)Z",at=@At("HEAD"),cancellable=true,remap=false)
    private void star$searchChars(char chr,int modifiers,CallbackInfoReturnable<Boolean> cir){
        if(!searchFocused)return;
        if(chr>=32&&chr!=127&&search.length()<40){search+=chr;moduleScroll=0;cir.setReturnValue(true);}
    }
}
