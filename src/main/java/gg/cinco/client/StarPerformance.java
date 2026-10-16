package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

public final class StarPerformance {
    private StarPerformance() {}

    public static boolean active(MinecraftClient client){ClientModule m=CincoClient.findModule("Performance Mode");return m!=null&&m.enabled&&client!=null&&client.getCurrentFps()<m.setting;}

    /** 2 = full motion, 1 = reduced cosmetics, 0 = functional-only animation. */
    public static int animationTier(MinecraftClient client){
        if(client==null)return 2;
        int fps=Math.max(1,client.getCurrentFps());ClientModule m=CincoClient.findModule("Performance Mode");
        double target=m!=null&&m.enabled?Math.max(30.0,m.setting):75.0;
        // Reduce decoration before frame-time gets visibly bad instead of waiting for a hard drop.
        if(fps<Math.max(40.0,target*.72))return 0;
        if(fps<Math.max(58.0,target*.95))return 1;
        return 2;
    }

    public static int animatedCardCap(MinecraftClient client){return switch(animationTier(client)){case 0->2;case 1->6;default->12;};}
    public static boolean allowDecorativeHover(MinecraftClient client){return animationTier(client)>0;}
    public static boolean allowMenuShadow(MinecraftClient client){return animationTier(client)>1;}

    /** Starfield is decorative, so keep the budget intentionally small for responsive menus. */
    public static int guiStarCap(MinecraftClient client){return switch(animationTier(client)){case 0->12;case 1->28;default->52;};}
    public static int storageRenderCap(MinecraftClient client){return active(client)?64:140;}
    public static int lightRenderCap(MinecraftClient client){return active(client)?30:72;}
}
