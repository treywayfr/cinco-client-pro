package gg.cinco.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.hit.EntityHitResult;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Reference-style compact draggable Star HUD pills. */
public final class CincoHud {
    private static final long STARTED=System.currentTimeMillis();
    private static final int PINK=0xFFFF4FD1;
    private static String dragging;
    private static double dragOffsetX,dragOffsetY;
    private static boolean lastLeft;
    private static final List<Hit> hits=new ArrayList<>();
    private record Hit(String id,ClientModule module,StarHudLayoutStore.State state,int x,int y,int w,int h){}
    private CincoHud(){}

    public static void render(DrawContext ctx,MinecraftClient client){
        if(client==null||client.player==null||client.world==null||!CincoClient.hudEnabled)return;
        StarHudLayoutStore.applyPolishedDefaults(client.getWindow().getScaledWidth(),client.getWindow().getScaledHeight());
        boolean edit=client.currentScreen instanceof ChatScreen;hits.clear();int index=0;
        index=widget(ctx,client,edit,index,"brand","Client HUD","STAR CLIENT","✦",Items.NETHER_STAR,true);
        index=widget(ctx,client,edit,index,"fps","FPS Counter",Integer.toString(client.getCurrentFps()),"FPS",Items.CLOCK,false);
        index=widget(ctx,client,edit,index,"coords","Coordinates",String.format(Locale.ROOT,"%.0f  %.0f  %.0f",client.player.getX(),client.player.getY(),client.player.getZ()),"XYZ",Items.COMPASS,false);
        index=widget(ctx,client,edit,index,"direction","Direction",client.player.getHorizontalFacing().asString().toUpperCase(Locale.ROOT),"DIR",Items.COMPASS,false);
        index=widget(ctx,client,edit,index,"armor","Armor HUD",armorCount(client.player)+" / 4","ARM",Items.NETHERITE_CHESTPLATE,false);
        index=widget(ctx,client,edit,index,"potions","Potion Status",Integer.toString(client.player.getStatusEffects().size()),"FX",Items.POTION,false);
        index=widget(ctx,client,edit,index,"cps","CPS Display",Integer.toString(ClickRateTracker.getCps()),"CPS",Items.DIAMOND_SWORD,false);
        index=widget(ctx,client,edit,index,"session","Session Timer",session(),"TIME",Items.CLOCK,false);
        index=widget(ctx,client,edit,index,"keys","Keystrokes",keystrokes(client),"KEY",Items.TRIPWIRE_HOOK,false);
        index=widget(ctx,client,edit,index,"ping","Ping Display",ping(client)+" ms","PING",Items.ECHO_SHARD,false);
        index=widget(ctx,client,edit,index,"target","Target HUD",target(client),"TGT",Items.NETHERITE_SWORD,false);
        index=widget(ctx,client,edit,index,"inventory","Inventory Checklist",inventory(client),"BOX",Items.ENDER_CHEST,false);
        index=widget(ctx,client,edit,index,"radar","Container Radar HUD",Integer.toString(EnhancedWorldScan.storage().size()),"BOX",Items.ENDER_CHEST,false);
        widget(ctx,client,edit,index,"players","Nearby Player List",Integer.toString(Math.max(0,client.world.getPlayers().size()-1)),"PPL",Items.PLAYER_HEAD,false);
        if(edit)handleEditor(client);else if(dragging!=null){dragging=null;StarHudLayoutStore.save();}
    }

    private static int widget(DrawContext ctx,MinecraftClient client,boolean edit,int index,String id,String moduleName,String value,String tag,Item item,boolean brand){
        ClientModule module=CincoClient.findModule(moduleName);if(module==null||!module.enabled)return index;
        int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();
        int defaultX=12+(index%5)*118,defaultY=Math.max(12,sh-54-(index/5)*30);
        if(brand){defaultX=12;defaultY=12;}if(id.equals("coords")){defaultX=12;defaultY=55;}
        StarHudLayoutStore.State state=StarHudLayoutStore.get(id,defaultX,defaultY);state.scale=Math.max(.5f,Math.min(2.5f,state.scale));
        int textW=client.textRenderer.getWidth(value),baseW=brand?145:Math.max(96,Math.min(166,textW+57)),baseH=brand?34:29;
        int realW=Math.round(baseW*state.scale),realH=Math.round(baseH*state.scale);
        state.x=Math.max(0,Math.min(Math.max(0,sw-realW),state.x));state.y=Math.max(0,Math.min(Math.max(0,sh-realH),state.y));
        ctx.getMatrices().pushMatrix();ctx.getMatrices().translate(state.x,state.y);ctx.getMatrices().scale(state.scale,state.scale);

        int radius=baseH/2;
        CincoDraw.rounded(ctx,-4,-4,baseW+4,baseH+4,radius+4,0x17350064);
        CincoDraw.rounded(ctx,-2,-2,baseW+2,baseH+2,radius+2,edit?0xBFFF55D5:0x75C77CFF);
        CincoDraw.rounded(ctx,-1,-1,baseW+1,baseH+1,radius+1,0x76FF55D5);
        CincoDraw.rounded(ctx,0,0,baseW,baseH,radius,0x650A0712);

        int iconSize=brand?23:19,iy=(baseH-iconSize)/2,ix=7;
        CincoDraw.rounded(ctx,ix,iy,ix+iconSize,iy+iconSize,iconSize/2,0x553B244A);
        if(item!=null){ctx.getMatrices().pushMatrix();float sc=brand?.9f:.72f;ctx.getMatrices().translate(ix+(brand?3:4),iy+(brand?3:4));ctx.getMatrices().scale(sc,sc);ctx.drawItem(new ItemStack(item),0,0);ctx.getMatrices().popMatrix();}
        else ctx.drawCenteredTextWithShadow(client.textRenderer,StarFonts.text(tag,true),ix+iconSize/2,iy+iconSize/2-4,PINK);

        if(brand){ctx.drawText(client.textRenderer,StarFonts.text("STAR",true),36,12,0xFFFFFFFF,false);ctx.drawText(client.textRenderer,StarFonts.text("CLIENT",true),72,12,0xFFFF75D9,false);ctx.drawText(client.textRenderer,StarFonts.text("✦",true),baseW-18,12,0xFFFFB0EF,false);}
        else{
            int valueX=34;ctx.drawText(client.textRenderer,StarFonts.text(tag,true),valueX,10,PINK,false);
            int vx=baseW-11-client.textRenderer.getWidth(value);ctx.drawText(client.textRenderer,StarFonts.text(value,true),Math.max(valueX+28,vx),10,0xFFF8F4FA,false);
        }
        if(edit){CincoDraw.rounded(ctx,baseW-17,3,baseW-3,17,7,0xA8FF4B8D);ctx.drawCenteredTextWithShadow(client.textRenderer,Text.literal("×"),baseW-10,5,0xFFFFFFFF);}
        ctx.getMatrices().popMatrix();hits.add(new Hit(id,module,state,state.x,state.y,realW,realH));return index+1;
    }

    private static void handleEditor(MinecraftClient client){int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=client.mouse.getX()*sw/Math.max(1.0,client.getWindow().getWidth()),my=client.mouse.getY()*sh/Math.max(1.0,client.getWindow().getHeight());boolean left=GLFW.glfwGetMouseButton(client.getWindow().getHandle(),GLFW.GLFW_MOUSE_BUTTON_LEFT)==GLFW.GLFW_PRESS;if(left&&!lastLeft)for(int i=hits.size()-1;i>=0;i--){Hit hit=hits.get(i);if(!inside(mx,my,hit))continue;int closeSize=Math.max(10,Math.round(17*hit.state.scale));if(mx>=hit.x+hit.w-closeSize&&my<=hit.y+closeSize){hit.module.enabled=false;ConfigStore.save();lastLeft=left;return;}dragging=hit.id;dragOffsetX=mx-hit.x;dragOffsetY=my-hit.y;break;}if(left&&dragging!=null)for(Hit hit:hits)if(hit.id.equals(dragging)){hit.state.x=(int)Math.round(mx-dragOffsetX);hit.state.y=(int)Math.round(my-dragOffsetY);break;}if(!left&&lastLeft&&dragging!=null){dragging=null;StarHudLayoutStore.save();}lastLeft=left;}
    public static boolean onScroll(MinecraftClient client,double mouseX,double mouseY,double vertical){if(!(client.currentScreen instanceof ChatScreen))return false;int sw=client.getWindow().getScaledWidth(),sh=client.getWindow().getScaledHeight();double mx=mouseX*sw/Math.max(1.0,client.getWindow().getWidth()),my=mouseY*sh/Math.max(1.0,client.getWindow().getHeight());for(int i=hits.size()-1;i>=0;i--){Hit hit=hits.get(i);if(!inside(mx,my,hit))continue;hit.state.scale=Math.max(.5f,Math.min(2.5f,hit.state.scale+(vertical>0?.1f:-.1f)));StarHudLayoutStore.save();return true;}return false;}
    private static boolean inside(double mx,double my,Hit h){return mx>=h.x&&mx<=h.x+h.w&&my>=h.y&&my<=h.y+h.h;}
    private static int armorCount(PlayerEntity player){int count=0;for(int i=0;i<4;i++){ItemStack stack=player.getInventory().getStack(36+i);if(!stack.isEmpty())count++;}return count;}
    private static String session(){long sec=(System.currentTimeMillis()-STARTED)/1000;return String.format(Locale.ROOT,"%02d:%02d",sec/60,sec%60);}
    private static String keystrokes(MinecraftClient c){return(c.options.forwardKey.isPressed()?"W":"·")+(c.options.leftKey.isPressed()?" A":" ·")+(c.options.backKey.isPressed()?" S":" ·")+(c.options.rightKey.isPressed()?" D":" ·");}
    private static int ping(MinecraftClient c){if(c.getNetworkHandler()==null||c.player==null)return 0;var entry=c.getNetworkHandler().getPlayerListEntry(c.player.getUuid());return entry==null?0:entry.getLatency();}
    private static String target(MinecraftClient c){if(c.crosshairTarget instanceof EntityHitResult hit)return trim(hit.getEntity().getName().getString(),13);return"—";}
    private static String inventory(MinecraftClient c){return"C"+count(c,Items.END_CRYSTAL)+" T"+count(c,Items.TOTEM_OF_UNDYING)+" G"+count(c,Items.GLOWSTONE);}
    private static int count(MinecraftClient c,Item item){int total=0;for(int i=0;i<c.player.getInventory().size();i++){ItemStack stack=c.player.getInventory().getStack(i);if(stack.isOf(item))total+=stack.getCount();}return total;}
    private static String trim(String s,int max){return s.length()<=max?s:s.substring(0,max-1)+"…";}
}
