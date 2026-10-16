package gg.cinco.client;

import net.minecraft.client.MinecraftClient;

/** Small central performance governor. Reads expensive/shared state at most a few times per second. */
public final class StarPerformance {
    private static ClientModule performanceModule;
    private static long nextRefreshNs;
    private static int cachedTier=2;
    private static boolean cachedActive;

    private StarPerformance() {}

    private static void refresh(MinecraftClient client){
        long now=System.nanoTime();
        if(now<nextRefreshNs)return;
        nextRefreshNs=now+100_000_000L; // 100 ms: responsive enough, avoids repeated module scans per frame.
        if(performanceModule==null)performanceModule=CincoClient.findModule("Performance Mode");
        if(client==null){cachedTier=2;cachedActive=false;return;}
        int fps=Math.max(1,client.getCurrentFps());
        double target=performanceModule!=null&&performanceModule.enabled?Math.max(30.0,performanceModule.setting):75.0;
        cachedActive=performanceModule!=null&&performanceModule.enabled&&fps<performanceModule.setting;
        if(fps<Math.max(40.0,target*.72))cachedTier=0;
        else if(fps<Math.max(58.0,target*.95))cachedTier=1;
        else cachedTier=2;
    }

    public static boolean active(MinecraftClient client){refresh(client);return cachedActive;}

    /** 2 = full motion, 1 = reduced cosmetics, 0 = functional-only animation. */
    public static int animationTier(MinecraftClient client){refresh(client);return cachedTier;}

    public static int animatedCardCap(MinecraftClient client){return switch(animationTier(client)){case 0->2;case 1->5;default->10;};}
    public static boolean allowDecorativeHover(MinecraftClient client){return animationTier(client)>0;}
    public static boolean allowMenuShadow(MinecraftClient client){return animationTier(client)>1;}

    public static int guiStarCap(MinecraftClient client){return switch(animationTier(client)){case 0->6;case 1->18;default->36;};}
    public static int storageRenderCap(MinecraftClient client){return active(client)?48:110;}
    public static int lightRenderCap(MinecraftClient client){return active(client)?22:54;}
}
