package gg.cinco.client;

import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.WorldChunk;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Loaded-world-only scanners with incremental light scanning. */
public final class EnhancedWorldScan {
    public record SusChunk(int chunkX,int chunkZ,int score,int storageCount,int lightSamples){}
    private static final List<BlockPos> storage=new ArrayList<>(),lightHotspots=new ArrayList<>(),lightPending=new ArrayList<>();
    private static final List<SusChunk> susChunks=new ArrayList<>();
    private static int tick;private static long worldIdentity;private static int lightCenterX,lightCenterY,lightCenterZ,lightRange,lightCursor,lightScanCompletedAt;private static boolean lightScanning;
    private EnhancedWorldScan(){}

    public static void tick(MinecraftClient client){if(client==null||client.player==null||client.world==null){reset();return;}long id=System.identityHashCode(client.world);if(id!=worldIdentity){reset();worldIdentity=id;}tick++;if(tick%8==0)refreshStorage(client);scanLightIncremental(client);if(tick%20==0)refreshSusChunks(client);}

    private static void refreshStorage(MinecraftClient client){
        ClientModule esp=CincoClient.findModule("Storage ESP");boolean finder=enabled("Base Finder")||enabled("Container Finder")||enabled("Spawner Finder")||enabled("Shulker Finder")||enabled("Stash Radar")||enabled("Sus Chunk Finder");
        if((esp==null||!esp.enabled)&&!finder){storage.clear();return;}
        int range=esp!=null&&esp.enabled?(int)Math.round(esp.setting):64;ClientModule base=CincoClient.findModule("Base Finder");if(base!=null&&base.enabled)range=Math.max(range,(int)Math.round(base.setting));ClientModule containers=CincoClient.findModule("Container Finder");if(containers!=null&&containers.enabled)range=Math.max(range,(int)Math.round(containers.setting));range=Math.max(8,Math.min(128,range));
        int pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4,chunkRadius=(range+15)/16;double maxSq=(range+16.0)*(range+16.0);Set<BlockPos> found=new HashSet<>();
        for(int cx=pcx-chunkRadius;cx<=pcx+chunkRadius;cx++)for(int cz=pcz-chunkRadius;cz<=pcz+chunkRadius;cz++){if(!client.world.isChunkLoaded(cx,cz))continue;WorldChunk chunk=client.world.getChunk(cx,cz);for(BlockPos raw:chunk.getBlockEntityPositions()){BlockPos pos=raw.toImmutable();if(client.player.squaredDistanceTo(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)>maxSq)continue;if(isStorageOrSpawner(client.world.getBlockState(pos)))found.add(pos);}}
        storage.clear();storage.addAll(found);storage.sort(Comparator.comparingDouble(p->client.player.squaredDistanceTo(p.getX()+.5,p.getY()+.5,p.getZ()+.5)));
    }

    private static void scanLightIncremental(MinecraftClient client){
        ClientModule module=CincoClient.findModule("Light Finder");if(module==null||!module.enabled){lightHotspots.clear();lightPending.clear();lightCursor=0;lightScanning=false;return;}
        int range=Math.max(16,Math.min(96,(int)Math.round(module.setting))),px=client.player.getBlockX(),py=client.player.getBlockY(),pz=client.player.getBlockZ();boolean moved=Math.abs(px-lightCenterX)>8||Math.abs(py-lightCenterY)>8||Math.abs(pz-lightCenterZ)>8,stale=!lightScanning&&tick-lightScanCompletedAt>=120;
        if(!lightScanning&&(lightCursor==0||moved||range!=lightRange||stale)){lightCenterX=px;lightCenterY=py;lightCenterZ=pz;lightRange=range;lightCursor=0;lightPending.clear();lightScanning=true;}else if(lightScanning&&(moved||range!=lightRange)){lightCenterX=px;lightCenterY=py;lightCenterZ=pz;lightRange=range;lightCursor=0;lightPending.clear();}
        if(!lightScanning||(tick&1)!=0)return;
        final int step=range>64?3:2;int width=range*2/step+1,height=range*2/step+1,volume=width*width*height,budget=2200;int worldTop=client.world.getBottomY()+client.world.getHeight();
        for(int i=0;i<budget&&lightCursor<volume;i++,lightCursor++){int layer=lightCursor/(width*width),within=lightCursor%(width*width),gx=within%width,gz=within/width,x=lightCenterX-range+gx*step,y=lightCenterY-range+layer*step,z=lightCenterZ-range+gz*step;if(y<client.world.getBottomY()||y>=worldTop)continue;if(!client.world.isChunkLoaded(x>>4,z>>4))continue;BlockPos pos=new BlockPos(x,y,z);int block=client.world.getLightLevel(LightType.BLOCK,pos),sky=client.world.getLightLevel(LightType.SKY,pos);if(block<8||sky>3)continue;BlockState state=client.world.getBlockState(pos);if(state.isOf(Blocks.LAVA)||state.isOf(Blocks.FIRE)||state.isOf(Blocks.SOUL_FIRE))continue;if(nearExisting(lightPending,pos,4.75))continue;if(lightPending.size()<144)lightPending.add(pos.toImmutable());}
        if(lightHotspots.isEmpty()&&tick%10==0&&!lightPending.isEmpty()){lightHotspots.clear();lightHotspots.addAll(lightPending);}if(lightCursor>=volume){lightPending.sort(Comparator.comparingDouble(p->client.player.squaredDistanceTo(p.getX()+.5,p.getY()+.5,p.getZ()+.5)));lightHotspots.clear();lightHotspots.addAll(lightPending);lightPending.clear();lightScanning=false;lightScanCompletedAt=tick;}
    }

    private static void refreshSusChunks(MinecraftClient client){
        ClientModule module=CincoClient.findModule("Sus Chunk Finder");if(module==null||!module.enabled){susChunks.clear();return;}int radius=Math.max(1,Math.min(6,(int)Math.round(module.setting))),minScore=Math.max(8,(int)Math.round(module.setting3)*4),pcx=client.player.getBlockX()>>4,pcz=client.player.getBlockZ()>>4;List<SusChunk> next=new ArrayList<>();int worldTop=client.world.getBottomY()+client.world.getHeight();
        for(int cx=pcx-radius;cx<=pcx+radius;cx++)for(int cz=pcz-radius;cz<=pcz+radius;cz++){if(!client.world.isChunkLoaded(cx,cz))continue;WorldChunk chunk=client.world.getChunk(cx,cz);int score=0,storageCount=0;for(BlockPos pos:chunk.getBlockEntityPositions()){BlockState state=client.world.getBlockState(pos);int weight=suspicionWeight(state);if(weight>0){score+=weight;storageCount++;}}if(storageCount>=3)score+=storageCount*storageCount;int lightSamples=0,bottom=client.world.getBottomY()+4,top=worldTop-4;for(int ox=2;ox<16;ox+=4)for(int oz=2;oz<16;oz+=4)for(int y=bottom;y<top;y+=16){BlockPos pos=new BlockPos((cx<<4)+ox,y,(cz<<4)+oz);if(client.world.getLightLevel(LightType.BLOCK,pos)>=8&&client.world.getLightLevel(LightType.SKY,pos)<=2){lightSamples++;if(lightSamples>=12)break;}}score+=Math.min(18,lightSamples*2);if(score>=minScore)next.add(new SusChunk(cx,cz,score,storageCount,lightSamples));}
        next.sort(Comparator.comparingInt(SusChunk::score).reversed());susChunks.clear();susChunks.addAll(next);
    }

    private static int suspicionWeight(BlockState state){if(state.getBlock()instanceof ShulkerBoxBlock)return 12;if(state.isOf(Blocks.CHEST)||state.isOf(Blocks.TRAPPED_CHEST)||state.isOf(Blocks.BARREL))return 6;if(state.isOf(Blocks.ENDER_CHEST))return 7;if(state.isOf(Blocks.HOPPER))return 5;if(state.isOf(Blocks.BREWING_STAND))return 5;if(state.isOf(Blocks.FURNACE)||state.isOf(Blocks.BLAST_FURNACE)||state.isOf(Blocks.SMOKER))return 3;if(state.isOf(Blocks.BEACON))return 20;return 0;}
    public static boolean isStorageOrSpawner(BlockState state){return state.isOf(Blocks.CHEST)||state.isOf(Blocks.TRAPPED_CHEST)||state.isOf(Blocks.BARREL)||state.isOf(Blocks.ENDER_CHEST)||state.isOf(Blocks.HOPPER)||state.isOf(Blocks.DISPENSER)||state.isOf(Blocks.DROPPER)||state.isOf(Blocks.FURNACE)||state.isOf(Blocks.BLAST_FURNACE)||state.isOf(Blocks.SMOKER)||state.isOf(Blocks.SPAWNER)||state.getBlock()instanceof ShulkerBoxBlock;}
    public static String storageLabel(BlockState state){if(state.isOf(Blocks.CHEST))return"CHEST";if(state.isOf(Blocks.TRAPPED_CHEST))return"TRAPPED CHEST";if(state.isOf(Blocks.BARREL))return"BARREL";if(state.isOf(Blocks.ENDER_CHEST))return"ENDER CHEST";if(state.getBlock()instanceof ShulkerBoxBlock)return"SHULKER";if(state.isOf(Blocks.SPAWNER))return"SPAWNER";if(state.isOf(Blocks.HOPPER))return"HOPPER";if(state.isOf(Blocks.DISPENSER))return"DISPENSER";if(state.isOf(Blocks.DROPPER))return"DROPPER";if(state.isOf(Blocks.BLAST_FURNACE))return"BLAST FURNACE";if(state.isOf(Blocks.SMOKER))return"SMOKER";return"FURNACE";}
    public static boolean storageMatchesFilter(BlockState state,int filter){if(filter<=1)return isStorageOrSpawner(state);if(filter==2)return state.isOf(Blocks.CHEST)||state.isOf(Blocks.TRAPPED_CHEST);if(filter==3)return state.isOf(Blocks.BARREL);if(filter==4)return state.getBlock()instanceof ShulkerBoxBlock;if(filter==5)return state.isOf(Blocks.SPAWNER);return state.isOf(Blocks.ENDER_CHEST);}
    private static boolean nearExisting(List<BlockPos> list,BlockPos pos,double radius){double r2=radius*radius;for(BlockPos other:list){double dx=other.getX()-pos.getX(),dy=other.getY()-pos.getY(),dz=other.getZ()-pos.getZ();if(dx*dx+dy*dy+dz*dz<=r2)return true;}return false;}
    private static boolean enabled(String name){ClientModule module=CincoClient.findModule(name);return module!=null&&module.enabled;}
    public static List<BlockPos> storage(){return List.copyOf(storage);}public static List<BlockPos> lightHotspots(){return List.copyOf(lightHotspots);}public static List<SusChunk> susChunks(){return List.copyOf(susChunks);}
    public static void reset(){storage.clear();lightHotspots.clear();lightPending.clear();susChunks.clear();tick=0;lightCursor=0;lightScanCompletedAt=0;lightScanning=false;worldIdentity=0;}
}
