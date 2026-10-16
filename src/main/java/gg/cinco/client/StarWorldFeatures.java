package gg.cinco.client;

import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Loaded-world-only Sus Chunk detection and spawner notifications. Never force-loads chunks. */
public final class StarWorldFeatures {
    public record SusChunk(int chunkX,int chunkZ,int topY,int count){}
    private static final List<BlockPos> SUS_BLOCKS=new ArrayList<>();
    private static final Map<Long,MutableChunk> SUS_CHUNKS=new LinkedHashMap<>();
    private static final Set<BlockPos> SUS_SEEN=new LinkedHashSet<>(),SPAWNERS=new LinkedHashSet<>(),NOTIFIED_SPAWNERS=new LinkedHashSet<>();
    private static int centerX,centerY,centerZ,radiusBlocks,cursor,tickGate;
    private static long worldIdentity;private static boolean started;private static String popup;private static long popupUntil;
    private StarWorldFeatures(){}

    public static void tick(MinecraftClient client){
        if(client==null||client.player==null||client.world==null){reset();return;}
        ClientModule sus=CincoClient.findModule("Sus Chunk"),spawner=CincoClient.findModule("Spawner Notifier");boolean scanSus=sus!=null&&sus.enabled,scanSpawner=spawner!=null&&spawner.enabled;
        if(!scanSus&&!scanSpawner){clearResults();return;}
        // Scanners do not need 20 Hz updates. Spread work across frames to protect frametime.
        if((++tickGate&1)!=0)return;
        int chunkRadius=sus!=null?Math.max(1,Math.min(6,(int)Math.round(sus.setting))):3;if(!scanSus&&spawner!=null)chunkRadius=Math.max(1,Math.min(6,(int)Math.round(spawner.setting)));
        int wantedRadius=chunkRadius*16,px=client.player.getBlockX(),py=client.player.getBlockY(),pz=client.player.getBlockZ();long identity=System.identityHashCode(client.world);boolean worldChanged=started&&identity!=worldIdentity;
        if(!started||worldChanged||Math.abs(px-centerX)>16||Math.abs(pz-centerZ)>16||Math.abs(py-centerY)>32||wantedRadius!=radiusBlocks){if(worldChanged)NOTIFIED_SPAWNERS.clear();centerX=px;centerY=py;centerZ=pz;radiusBlocks=wantedRadius;cursor=0;worldIdentity=identity;started=true;SUS_BLOCKS.clear();SUS_CHUNKS.clear();SUS_SEEN.clear();SPAWNERS.clear();}
        int width=radiusBlocks*2+1,vertical=129,volume=width*width*vertical,budget=StarPerformance.active(client)?1100:2600;SusChunkConfig cfg=SusChunkConfig.get();
        for(int i=0;i<budget&&cursor<volume;i++,cursor++){int layer=cursor/(width*width),within=cursor%(width*width),dx=within%width-radiusBlocks,dz=within/width-radiusBlocks,dy=layer-64;BlockPos pos=new BlockPos(centerX+dx,centerY+dy,centerZ+dz);if(!client.world.isChunkLoaded(pos.getX()>>4,pos.getZ()>>4))continue;BlockState state=client.world.getBlockState(pos);if(scanSpawner&&state.isOf(Blocks.SPAWNER)){BlockPos stable=pos.toImmutable();SPAWNERS.add(stable);if(NOTIFIED_SPAWNERS.add(stable))showSpawner(stable);}if(scanSus&&suspicious(state,cfg))addSus(pos);}if(cursor>=volume)cursor=volume;
    }

    private static boolean suspicious(BlockState state,SusChunkConfig cfg){if(cfg.rotatedDeepslate&&state.isOf(Blocks.DEEPSLATE)&&state.contains(Properties.AXIS)&&state.get(Properties.AXIS)!=Direction.Axis.Y)return true;if(cfg.cobbledDeepslate&&state.isOf(Blocks.COBBLED_DEEPSLATE))return true;if(cfg.cactus&&state.isOf(Blocks.CACTUS))return true;if(cfg.kelp&&(state.isOf(Blocks.KELP)||state.isOf(Blocks.KELP_PLANT)))return true;if(cfg.amethyst&&state.isOf(Blocks.AMETHYST_CLUSTER))return true;if(!cfg.matureCrops)return false;if((state.isOf(Blocks.WHEAT)||state.isOf(Blocks.CARROTS)||state.isOf(Blocks.POTATOES)||state.isOf(Blocks.PUMPKIN_STEM)||state.isOf(Blocks.MELON_STEM))&&state.contains(Properties.AGE_7)&&state.get(Properties.AGE_7)==7)return true;if((state.isOf(Blocks.BEETROOTS)||state.isOf(Blocks.NETHER_WART))&&state.contains(Properties.AGE_3)&&state.get(Properties.AGE_3)==3)return true;return state.isOf(Blocks.ATTACHED_PUMPKIN_STEM)||state.isOf(Blocks.ATTACHED_MELON_STEM);}
    private static void addSus(BlockPos pos){BlockPos stable=pos.toImmutable();if(!SUS_SEEN.add(stable))return;if(SUS_BLOCKS.size()<1800)SUS_BLOCKS.add(stable);int cx=pos.getX()>>4,cz=pos.getZ()>>4;long key=(((long)cx)<<32)^(cz&0xffffffffL);MutableChunk chunk=SUS_CHUNKS.computeIfAbsent(key,k->new MutableChunk(cx,cz));chunk.count++;chunk.topY=Math.max(chunk.topY,pos.getY());}

    public static void renderWorld(WorldRenderContext context){MinecraftClient client=MinecraftClient.getInstance();ClientModule sus=CincoClient.findModule("Sus Chunk");if(sus==null||!sus.enabled||client.player==null||context.consumers()==null)return;Vec3d camera=context.worldState().cameraRenderState.pos;VertexConsumer lines=context.consumers().getBuffer(RenderLayers.linesTranslucent());int color=SusChunkConfig.get().color;if(SusChunkConfig.get().highlightBlocks){int cap=StarPerformance.active(client)?220:520,n=0;for(BlockPos pos:SUS_BLOCKS){if(n++>=cap)break;float x=(float)(pos.getX()-camera.x),y=(float)(pos.getY()-camera.y),z=(float)(pos.getZ()-camera.z);box(lines,x,y,z,x+1,y+1,z+1,color,1.6f);}}else for(MutableChunk chunk:SUS_CHUNKS.values()){float x=(float)(chunk.x*16-camera.x),z=(float)(chunk.z*16-camera.z),y=(float)(chunk.topY+1.18-camera.y);chunkMarker(lines,x,y,z,color);}}
    private static void chunkMarker(VertexConsumer b,float x,float y,float z,int c){float x1=x+16,z1=z+16;square(b,x,y,z,x1,z1,c,3.2f);square(b,x+.35f,y+.10f,z+.35f,x1-.35f,z1-.35f,c,1.6f);line(b,x,y,z,x1,y,z1,c,2f);line(b,x1,y,z,x,y,z1,c,2f);line(b,x+8,y,z,x+8,y,z1,c,1.3f);line(b,x,y,z+8,x1,y,z+8,c,1.3f);float down=y-2.4f;line(b,x,y,z,x,down,z,c,1.7f);line(b,x1,y,z,x1,down,z,c,1.7f);line(b,x1,y,z1,x1,down,z1,c,1.7f);line(b,x,y,z1,x,down,z1,c,1.7f);}

    public static void renderHud(DrawContext ctx,MinecraftClient client){if(popup==null||System.currentTimeMillis()>popupUntil||client==null)return;int w=Math.max(210,client.textRenderer.getWidth(popup)+28),x=(client.getWindow().getScaledWidth()-w)/2,y=32;CincoDraw.bubble(ctx,x,y,x+w,y+38,10,0xE7B05CFF,0xEE09070E);ctx.drawCenteredTextWithShadow(client.textRenderer,Text.literal("SPAWNERS!"),x+w/2,y+8,0xFFFFD86A);ctx.drawCenteredTextWithShadow(client.textRenderer,Text.literal(popup),x+w/2,y+22,0xFFFFFFFF);}
    private static void showSpawner(BlockPos p){popup="X "+p.getX()+"   Y "+p.getY()+"   Z "+p.getZ();popupUntil=System.currentTimeMillis()+4500;}
    public static List<BlockPos> susBlocks(){return List.copyOf(SUS_BLOCKS);}public static List<SusChunk> susChunks(){List<SusChunk> out=new ArrayList<>();for(MutableChunk c:SUS_CHUNKS.values())out.add(new SusChunk(c.x,c.z,c.topY,c.count));return out;}public static Set<BlockPos> spawners(){return Set.copyOf(SPAWNERS);}
    private static void clearResults(){SUS_BLOCKS.clear();SUS_CHUNKS.clear();SUS_SEEN.clear();SPAWNERS.clear();cursor=0;started=false;}public static void reset(){clearResults();NOTIFIED_SPAWNERS.clear();popup=null;tickGate=0;}
    private static final class MutableChunk{final int x,z;int topY=-64,count;MutableChunk(int x,int z){this.x=x;this.z=z;}}
    private static void square(VertexConsumer b,float x0,float y,float z0,float x1,float z1,int c,float w){line(b,x0,y,z0,x1,y,z0,c,w);line(b,x1,y,z0,x1,y,z1,c,w);line(b,x1,y,z1,x0,y,z1,c,w);line(b,x0,y,z1,x0,y,z0,c,w);}private static void box(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){square(b,x0,y0,z0,x1,z1,c,w);square(b,x0,y1,z0,x1,z1,c,w);line(b,x0,y0,z0,x0,y1,z0,c,w);line(b,x1,y0,z0,x1,y1,z0,c,w);line(b,x1,y0,z1,x1,y1,z1,c,w);line(b,x0,y0,z1,x0,y1,z1,c,w);}private static void line(VertexConsumer b,float x0,float y0,float z0,float x1,float y1,float z1,int c,float w){b.vertex(x0,y0,z0).color(c).normal(0,1,0).lineWidth(w);b.vertex(x1,y1,z1).color(c).normal(0,1,0).lineWidth(w);}
}
