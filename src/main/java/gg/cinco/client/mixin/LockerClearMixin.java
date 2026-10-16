package gg.cinco.client.mixin;

import gg.cinco.client.CincoClient;
import gg.cinco.client.ClientModule;
import gg.cinco.client.ConfigStore;
import gg.cinco.client.LockerScreen;
import gg.cinco.client.StarCrispUi;
import gg.cinco.client.StarFonts;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds a per-tab CLEAR control without coupling skins, accessories and cosmetics together. */
@Mixin(value = LockerScreen.class, remap = false)
public abstract class LockerClearMixin {
    @Shadow private int tab;
    @Shadow private int x;
    @Shadow private int y;
    @Shadow private int w;
    @Shadow private int h;

    @Inject(method = "render", at = @At("TAIL"), remap = false)
    private void star$renderClear(DrawContext c, int mx, int my, float delta, CallbackInfo ci) {
        int[] r = clearRect();
        ClientModule m = activeModule();
        boolean enabled = m != null && m.enabled;
        boolean hover = inside(mx,my,r[0],r[1],r[2],r[3]);
        StarCrispUi.card(c,r[0],r[1],r[2],r[3],enabled,hover);
        int color = enabled ? 0xFFFF8BE8 : 0xFFB9A9C5;
        c.drawCenteredTextWithShadow(net.minecraft.client.MinecraftClient.getInstance().textRenderer,
                StarFonts.text("CLEAR", true), r[0] + r[2] / 2, r[1] + 7, color);
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true, remap = false)
    private void star$clearOnlyCurrentTab(double mx, double my, int button, CallbackInfoReturnable<Boolean> cir) {
        if (button != 0) return;
        int[] r = clearRect();
        if (!inside(mx,my,r[0],r[1],r[2],r[3])) return;
        ClientModule m = activeModule();
        if (m != null) {
            m.enabled = false;
            ConfigStore.save();
        }
        cir.setReturnValue(true);
    }

    private ClientModule activeModule() {
        if (tab == 0) return CincoClient.findModule("Animations");
        if (tab == 2) return CincoClient.findModule("Custom Accessory");
        ClientModule skin = CincoClient.findModule("Player Changer");
        return skin != null ? skin : CincoClient.findModule("GTA Skin Changer");
    }

    private int[] clearRect() {
        int previewW = Math.max(150, Math.min(255, w / 3));
        int previewX = x + w - previewW - 12;
        return new int[]{previewX + previewW - 70, y + h - 48, 62, 22};
    }

    private boolean inside(double mx,double my,int ax,int ay,int aw,int ah) {
        return mx >= ax && mx <= ax + aw && my >= ay && my <= ay + ah;
    }
}
