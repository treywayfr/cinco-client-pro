package gg.cinco.client;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;

import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Runtime importer for CJ's GTA: San Andreas-derived mesh/rig data.
 *
 * Star intentionally does not embed the third-party GTA asset in its jar. On first use it downloads
 * the public converted mesh + atlas from the historical source revision below, caches them under
 * config/star-client/cj, then renders that mesh directly. Tung Tung Sahur remains completely separate.
 */
public final class GtaCjImportedModel {
    private static final String SOURCE_REV = "019e10c4be57c98d3b86742bf2be5ba61348fa8b";
    private static final URI MODEL_URI = URI.create("https://raw.githubusercontent.com/randyadr/Gen1Recomp-Character-Selector/" + SOURCE_REV + "/data/cj_model.lua");
    private static final URI ATLAS_URI = URI.create("https://raw.githubusercontent.com/randyadr/Gen1Recomp-Character-Selector/" + SOURCE_REV + "/assets/cj_atlas.png");
    private static final Identifier TEXTURE_ID = Identifier.of("cinco_client", "dynamic/cj_gtasa_imported");
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private static volatile Model MODEL;
    private static volatile boolean READY;
    private static volatile boolean FAILED;
    private static volatile String failureMessage = "";

    private GtaCjImportedModel() {}

    public static void bootstrap() {
        if (!STARTED.compareAndSet(false, true)) return;
        Thread.ofVirtual().name("star-cj-model-loader").start(() -> {
            try {
                Path dir = FabricLoader.getInstance().getConfigDir().resolve("star-client").resolve("cj");
                Files.createDirectories(dir);
                Path modelFile = dir.resolve("cj_model.lua");
                Path atlasFile = dir.resolve("cj_atlas.png");
                downloadIfNeeded(MODEL_URI, modelFile, 2_000_000L);
                downloadIfNeeded(ATLAS_URI, atlasFile, 250_000L);

                String lua = Files.readString(modelFile);
                Model parsed = Model.parse(lua);
                NativeImage image;
                try (InputStream in = Files.newInputStream(atlasFile)) {
                    image = NativeImage.read(in);
                }

                MinecraftClient client = MinecraftClient.getInstance();
                client.execute(() -> {
                    try {
                        NativeImageBackedTexture texture = new NativeImageBackedTexture(() -> "Star CJ GTA SA", image);
                        client.getTextureManager().registerTexture(TEXTURE_ID, texture);
                        texture.upload();
                        MODEL = parsed;
                        READY = true;
                    } catch (Throwable t) {
                        try { image.close(); } catch (Throwable ignored) {}
                        fail(t);
                    }
                });
            } catch (Throwable t) {
                fail(t);
            }
        });
    }

    private static void downloadIfNeeded(URI uri, Path path, long minimumBytes) throws Exception {
        if (Files.isRegularFile(path) && Files.size(path) >= minimumBytes) return;
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(12)).followRedirects(HttpClient.Redirect.NORMAL).build();
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(35))
                .header("User-Agent", "Star-Client-CJ-Importer/1.1")
                .GET().build();
        HttpResponse<byte[]> response = http.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() != 200 || response.body().length < minimumBytes) {
            throw new IllegalStateException("CJ source download failed: HTTP " + response.statusCode() + " (" + response.body().length + " bytes)");
        }
        Files.write(path, response.body());
    }

    private static void fail(Throwable t) {
        FAILED = true;
        failureMessage = t.getClass().getSimpleName() + ": " + String.valueOf(t.getMessage());
        System.err.println("[Star Client] Real CJ model failed to load: " + failureMessage);
    }

    public static boolean ready() {
        if (!STARTED.get()) bootstrap();
        return READY && MODEL != null;
    }

    public static boolean failed() { return FAILED; }
    public static String failureMessage() { return failureMessage; }

    /** Returns true only when the imported CJ mesh was rendered. */
    public static boolean render(MatrixStack matrices, VertexConsumerProvider consumers, StarPlayerModels.Pose pose) {
        if (!ready()) return false;
        Model model = MODEL;
        if (model == null) return false;
        model.render(matrices, consumers, pose);
        return true;
    }

    private static final class Model {
        final int boneCount, positionCount, triangleCount;
        final int idleFrames, runFrames;
        final float idleDuration, runDuration;
        final String[] boneNames;
        final int[] boneParent, posFirst, posCount, infBone, cornerPos;
        final float[] boneLocal, infX, infY, infZ, infW, cornerU, cornerV, idleDelta, runDelta;
        final Map<String,Integer> boneIndex = new HashMap<>();
        final float[][] world;
        final float[][] localScratch;
        final float[] skinned;
        final float[] smoothNormals;
        final float[] deltaScratch = new float[16];
        final float[] mulScratch = new float[16];
        final float scale, centerX, centerZ, floorY;

        private Model(int boneCount, int positionCount, int triangleCount,
                      int idleFrames, float idleDuration, int runFrames, float runDuration,
                      String[] boneNames, int[] boneParent, float[] boneLocal,
                      int[] posFirst, int[] posCount, int[] infBone,
                      float[] infX, float[] infY, float[] infZ, float[] infW,
                      int[] cornerPos, float[] cornerU, float[] cornerV,
                      float[] idleDelta, float[] runDelta) {
            this.boneCount = boneCount;
            this.positionCount = positionCount;
            this.triangleCount = triangleCount;
            this.idleFrames = idleFrames;
            this.idleDuration = idleDuration;
            this.runFrames = runFrames;
            this.runDuration = runDuration;
            this.boneNames = boneNames;
            this.boneParent = boneParent;
            this.boneLocal = boneLocal;
            this.posFirst = posFirst;
            this.posCount = posCount;
            this.infBone = infBone;
            this.infX = infX;
            this.infY = infY;
            this.infZ = infZ;
            this.infW = infW;
            this.cornerPos = cornerPos;
            this.cornerU = cornerU;
            this.cornerV = cornerV;
            this.idleDelta = idleDelta;
            this.runDelta = runDelta;
            this.world = new float[boneCount][16];
            this.localScratch = new float[boneCount][16];
            this.skinned = new float[positionCount * 3];
            this.smoothNormals = new float[positionCount * 3];
            for (int i = 0; i < boneNames.length; i++) boneIndex.put(boneNames[i].toLowerCase(Locale.ROOT), i);

            buildPose(null, 0f, false);
            skinPositions();
            float minX=Float.POSITIVE_INFINITY,minY=Float.POSITIVE_INFINITY,minZ=Float.POSITIVE_INFINITY;
            float maxX=Float.NEGATIVE_INFINITY,maxY=Float.NEGATIVE_INFINITY,maxZ=Float.NEGATIVE_INFINITY;
            for (int i=0;i<positionCount;i++) {
                float sx=skinned[i*3], sy=skinned[i*3+1], sz=skinned[i*3+2];
                float x=sx, y=sz, z=-sy;
                minX=Math.min(minX,x);maxX=Math.max(maxX,x);
                minY=Math.min(minY,y);maxY=Math.max(maxY,y);
                minZ=Math.min(minZ,z);maxZ=Math.max(maxZ,z);
            }
            float height=Math.max(.001f,maxY-minY);
            this.scale=1.84f/height;
            this.centerX=(minX+maxX)*.5f;
            this.centerZ=(minZ+maxZ)*.5f;
            this.floorY=minY;
        }

        static Model parse(String lua) {
            int boneCount = scalarInt(lua,"boneCount");
            int positionCount = scalarInt(lua,"positionCount");
            int triangleCount = scalarInt(lua,"triangleCount");
            int idleFrames = scalarInt(lua,"idleFrameCount");
            int runFrames = scalarInt(lua,"runFrameCount");
            float idleDuration = scalarFloat(lua,"idleDuration");
            float runDuration = scalarFloat(lua,"runDuration");
            String[] names = stringArray(lua,"boneName");
            int[] parents = intArray(lua,"boneParent");
            float[] locals = floatArray(lua,"boneLocal");
            int[] posFirst = intArray(lua,"posFirst");
            int[] posCount = intArray(lua,"posCount");
            int[] infBone = intArray(lua,"infBone");
            float[] infX = floatArray(lua,"infX");
            float[] infY = floatArray(lua,"infY");
            float[] infZ = floatArray(lua,"infZ");
            float[] infW = floatArray(lua,"infW");
            int[] cornerPos = intArray(lua,"cornerPos");
            float[] cornerU = floatArray(lua,"cornerU");
            float[] cornerV = floatArray(lua,"cornerV");
            float[] idle = floatArray(lua,"idleDelta");
            float[] run = floatArray(lua,"runDelta");

            require(names.length==boneCount,"boneName");
            require(parents.length>=boneCount,"boneParent");
            require(locals.length>=boneCount*16,"boneLocal");
            require(posFirst.length>=positionCount && posCount.length>=positionCount,"position tables");
            require(infBone.length==infW.length && infX.length==infW.length && infY.length==infW.length && infZ.length==infW.length,"influence tables");
            require(cornerPos.length>=triangleCount*3 && cornerU.length>=triangleCount*3 && cornerV.length>=triangleCount*3,"triangle tables");
            require(idle.length>=boneCount*idleFrames*16,"idleDelta");
            require(run.length>=boneCount*runFrames*16,"runDelta");
            return new Model(boneCount,positionCount,triangleCount,idleFrames,idleDuration,runFrames,runDuration,
                    names,parents,locals,posFirst,posCount,infBone,infX,infY,infZ,infW,cornerPos,cornerU,cornerV,idle,run);
        }

        void render(MatrixStack matrices, VertexConsumerProvider consumers, StarPlayerModels.Pose pose) {
            float amount=clamp(pose.limbAmount(),0f,1f);
            boolean moving=amount>.055f;
            float seconds=pose.age()/20f;
            buildPose(pose,seconds,moving);
            skinPositions();
            buildSmoothNormals();

            // The imported GTA mesh has mixed triangle winding in a few clothing/neck regions.
            // No-cull keeps those valid faces from disappearing as the camera moves around CJ.
            VertexConsumer out=consumers.getBuffer(RenderLayers.entityCutoutNoCull(TEXTURE_ID));
            MatrixStack.Entry entry=matrices.peek();
            int corners=Math.min(triangleCount*3,Math.min(cornerPos.length,Math.min(cornerU.length,cornerV.length)));
            for(int c=0;c+2<corners;c+=3){
                int pa=cornerPos[c]-1,pb=cornerPos[c+1]-1,pc=cornerPos[c+2]-1;
                int ia=pa*3,ib=pb*3,ic=pc*3;
                if(pa<0||pb<0||pc<0||ia+2>=skinned.length||ib+2>=skinned.length||ic+2>=skinned.length)continue;
                vertex(out,entry,toX(skinned[ia]),toY(skinned[ia+2]),toZ(skinned[ia+1]),cornerU[c],cornerV[c],normalX(pa),normalY(pa),normalZ(pa));
                vertex(out,entry,toX(skinned[ib]),toY(skinned[ib+2]),toZ(skinned[ib+1]),cornerU[c+1],cornerV[c+1],normalX(pb),normalY(pb),normalZ(pb));
                vertex(out,entry,toX(skinned[ic]),toY(skinned[ic+2]),toZ(skinned[ic+1]),cornerU[c+2],cornerV[c+2],normalX(pc),normalY(pc),normalZ(pc));
            }
        }

        private void buildSmoothNormals(){
            Arrays.fill(smoothNormals,0f);
            int corners=Math.min(triangleCount*3,cornerPos.length);
            for(int c=0;c+2<corners;c+=3){
                int pa=cornerPos[c]-1,pb=cornerPos[c+1]-1,pc=cornerPos[c+2]-1;
                if(pa<0||pb<0||pc<0||pa>=positionCount||pb>=positionCount||pc>=positionCount)continue;
                int ia=pa*3,ib=pb*3,ic=pc*3;
                float ax=toX(skinned[ia]),ay=toY(skinned[ia+2]),az=toZ(skinned[ia+1]);
                float bx=toX(skinned[ib]),by=toY(skinned[ib+2]),bz=toZ(skinned[ib+1]);
                float cx=toX(skinned[ic]),cy=toY(skinned[ic+2]),cz=toZ(skinned[ic+1]);
                float ux=bx-ax,uy=by-ay,uz=bz-az,vx=cx-ax,vy=cy-ay,vz=cz-az;
                float nx=uy*vz-uz*vy,ny=uz*vx-ux*vz,nz=ux*vy-uy*vx;
                float len2=nx*nx+ny*ny+nz*nz;
                if(len2<1.0e-12f)continue;
                // Area-weighted accumulation gives the low-poly GTA mesh smooth character lighting
                // without rounding away the actual silhouette.
                addNormal(pa,nx,ny,nz);addNormal(pb,nx,ny,nz);addNormal(pc,nx,ny,nz);
            }
            for(int p=0;p<positionCount;p++){
                int i=p*3;float nx=smoothNormals[i],ny=smoothNormals[i+1],nz=smoothNormals[i+2];
                float len=(float)Math.sqrt(nx*nx+ny*ny+nz*nz);
                if(len<1.0e-6f){smoothNormals[i]=0;smoothNormals[i+1]=1;smoothNormals[i+2]=0;}
                else{smoothNormals[i]=nx/len;smoothNormals[i+1]=ny/len;smoothNormals[i+2]=nz/len;}
            }
        }

        private void addNormal(int p,float x,float y,float z){int i=p*3;smoothNormals[i]+=x;smoothNormals[i+1]+=y;smoothNormals[i+2]+=z;}
        private float normalX(int p){return smoothNormals[p*3];}
        private float normalY(int p){return smoothNormals[p*3+1];}
        private float normalZ(int p){return smoothNormals[p*3+2];}

        private void vertex(VertexConsumer out, MatrixStack.Entry entry,float x,float y,float z,float u,float v,float nx,float ny,float nz){
            out.vertex(entry,x,y,z)
                    .color(0xFFFFFFFF)
                    .texture(u,v)
                    .overlay(OverlayTexture.DEFAULT_UV)
                    .light(LightmapTextureManager.MAX_LIGHT_COORDINATE)
                    .normal(entry,nx,ny,nz);
        }

        private float toX(float sourceX){ return (sourceX-centerX)*scale; }
        private float toY(float sourceZ){ return (sourceZ-floorY)*scale; }
        private float toZ(float sourceY){ return (-sourceY-centerZ)*scale; }

        private void buildPose(StarPlayerModels.Pose pose,float seconds,boolean moving){
            float[] clip=moving?runDelta:idleDelta;
            int frames=moving?runFrames:idleFrames;
            float duration=moving?runDuration:idleDuration;
            float speed=moving?(0.72f+clamp(pose==null?0f:pose.limbAmount(),0f,1f)*.72f):1f;
            float phase=duration>1.0e-5f?positiveModulo(seconds*speed,duration)/duration*frames:0f;
            int f0=frames>0?((int)Math.floor(phase))%frames:0;
            int f1=frames>0?(f0+1)%frames:0;
            float blend=phase-(float)Math.floor(phase);

            for(int bone=0;bone<boneCount;bone++){
                float[] local=localScratch[bone];
                if(pose==null || clip.length<boneCount*frames*16){
                    System.arraycopy(boneLocal,bone*16,local,0,16);
                }else{
                    int a=(bone*frames+f0)*16,b=(bone*frames+f1)*16;
                    for(int k=0;k<16;k++)deltaScratch[k]=clip[a+k]+(clip[b+k]-clip[a+k])*blend;
                    // Raw element-by-element matrix interpolation can shear/skew the skeleton between
                    // GTA animation frames. Re-orthonormalize the rotation basis before skinning.
                    orthonormalize(deltaScratch);
                    mul(boneLocal,bone*16,deltaScratch,0,local);
                    addLookRotation(bone,local,pose);
                }
                int parent=(bone<boneParent.length?boneParent[bone]:0)-1;
                if(parent>=0&&parent<bone)mul(world[parent],0,local,0,world[bone]);
                else System.arraycopy(local,0,world[bone],0,16);
            }
        }

        private static void orthonormalize(float[] m){
            float x0=m[0],x1=m[1],x2=m[2];
            float y0=m[4],y1=m[5],y2=m[6];
            float oldZ0=m[8],oldZ1=m[9],oldZ2=m[10];
            float xl=(float)Math.sqrt(x0*x0+x1*x1+x2*x2);
            if(xl<1.0e-6f)return;
            x0/=xl;x1/=xl;x2/=xl;
            float dot=x0*y0+x1*y1+x2*y2;
            y0-=x0*dot;y1-=x1*dot;y2-=x2*dot;
            float yl=(float)Math.sqrt(y0*y0+y1*y1+y2*y2);
            if(yl<1.0e-6f)return;
            y0/=yl;y1/=yl;y2/=yl;
            float z0=x1*y2-x2*y1,z1=x2*y0-x0*y2,z2=x0*y1-x1*y0;
            if(z0*oldZ0+z1*oldZ1+z2*oldZ2<0f){z0=-z0;z1=-z1;z2=-z2;}
            m[0]=x0;m[1]=x1;m[2]=x2;
            m[4]=y0;m[5]=y1;m[6]=y2;
            m[8]=z0;m[9]=z1;m[10]=z2;
            m[12]=0f;m[13]=0f;m[14]=0f;m[15]=1f;
        }

        private void addLookRotation(int bone,float[] local,StarPlayerModels.Pose pose){
            String name=boneNames[bone].toLowerCase(Locale.ROOT);
            float yaw=clamp(pose.headYaw(),-62f,62f)*(float)Math.PI/180f;
            float pitch=clamp(pose.pitch(),-38f,38f)*(float)Math.PI/180f;
            float yawShare=0f,pitchShare=0f;
            if(name.equals("head neck lower")){yawShare=.28f;pitchShare=.22f;}
            else if(name.equals("head neck upper")){yawShare=.72f;pitchShare=.78f;}
            if(yawShare==0f&&pitchShare==0f)return;
            float[] rx=rotationX(pitch*pitchShare);
            float[] rz=rotationZ(yaw*yawShare);
            mul(local,0,rz,0,mulScratch);
            mul(mulScratch,0,rx,0,local);
        }

        private void skinPositions(){
            for(int p=0;p<positionCount;p++){
                int first=(p<posFirst.length?posFirst[p]:1)-1;
                int count=p<posCount.length?posCount[p]:0;
                float x=0,y=0,z=0,total=0;
                for(int n=0;n<count;n++){
                    int j=first+n;
                    if(j<0||j>=infW.length)continue;
                    int bone=(infBone[j]-1);
                    if(bone<0||bone>=world.length)continue;
                    float[] w=world[bone];
                    float lx=infX[j],ly=infY[j],lz=infZ[j],weight=infW[j];
                    float tx=w[0]*lx+w[1]*ly+w[2]*lz+w[3];
                    float ty=w[4]*lx+w[5]*ly+w[6]*lz+w[7];
                    float tz=w[8]*lx+w[9]*ly+w[10]*lz+w[11];
                    x+=tx*weight;y+=ty*weight;z+=tz*weight;total+=weight;
                }
                if(total>1.0e-6f&&Math.abs(total-1f)>1.0e-4f){x/=total;y/=total;z/=total;}
                skinned[p*3]=x;skinned[p*3+1]=y;skinned[p*3+2]=z;
            }
        }

        private static void mul(float[] a,int ao,float[] b,int bo,float[] out){
            float[] temp=out==a||out==b?new float[16]:out;
            for(int r=0;r<4;r++){
                int rr=r*4;
                for(int c=0;c<4;c++){
                    temp[rr+c]=a[ao+rr]*b[bo+c]+a[ao+rr+1]*b[bo+4+c]+a[ao+rr+2]*b[bo+8+c]+a[ao+rr+3]*b[bo+12+c];
                }
            }
            if(temp!=out)System.arraycopy(temp,0,out,0,16);
        }

        private static float[] rotationX(float a){
            float c=(float)Math.cos(a),s=(float)Math.sin(a);
            return new float[]{1,0,0,0, 0,c,-s,0, 0,s,c,0, 0,0,0,1};
        }
        private static float[] rotationZ(float a){
            float c=(float)Math.cos(a),s=(float)Math.sin(a);
            return new float[]{c,-s,0,0, s,c,0,0, 0,0,1,0, 0,0,0,1};
        }
        private static float positiveModulo(float value,float mod){float r=value%mod;return r<0?r+mod:r;}

        private static int scalarInt(String text,String key){return Math.round(scalarFloat(text,key));}
        private static float scalarFloat(String text,String key){
            Matcher m=Pattern.compile("\\b"+Pattern.quote(key)+"\\s*=\\s*([-+0-9.eE]+)").matcher(text);
            if(!m.find())throw new IllegalArgumentException("Missing CJ field "+key);
            return Float.parseFloat(m.group(1));
        }
        private static String[] stringArray(String text,String key){
            int[] range=arrayRange(text,key);String body=text.substring(range[0],range[1]);
            Matcher m=Pattern.compile("\\\"([^\\\"]*)\\\"").matcher(body);
            String[] tmp=new String[96];int n=0;
            while(m.find()){if(n==tmp.length)tmp=Arrays.copyOf(tmp,tmp.length*2);tmp[n++]=m.group(1);}
            return Arrays.copyOf(tmp,n);
        }
        private static int[] intArray(String text,String key){
            int[] range=arrayRange(text,key);IntGrow out=new IntGrow();int i=range[0];
            while(i<range[1]){
                while(i<range[1]&&!numberStart(text.charAt(i)))i++;
                if(i>=range[1])break;int s=i++;
                while(i<range[1]&&numberPart(text.charAt(i)))i++;
                String token=text.substring(s,i);
                if(token.indexOf('.')>=0||token.indexOf('e')>=0||token.indexOf('E')>=0)out.add(Math.round(Float.parseFloat(token)));else out.add(Integer.parseInt(token));
            }
            return out.toArray();
        }
        private static float[] floatArray(String text,String key){
            int[] range=arrayRange(text,key);FloatGrow out=new FloatGrow();int i=range[0];
            while(i<range[1]){
                while(i<range[1]&&!numberStart(text.charAt(i)))i++;
                if(i>=range[1])break;int s=i++;
                while(i<range[1]&&numberPart(text.charAt(i)))i++;
                out.add(Float.parseFloat(text.substring(s,i)));
            }
            return out.toArray();
        }
        private static int[] arrayRange(String text,String key){
            Matcher m=Pattern.compile("\\b"+Pattern.quote(key)+"\\s*=\\s*\\{").matcher(text);
            if(!m.find())throw new IllegalArgumentException("Missing CJ array "+key);
            int start=m.end(),end=text.indexOf('}',start);
            if(end<0)throw new IllegalArgumentException("Unclosed CJ array "+key);
            return new int[]{start,end};
        }
        private static boolean numberStart(char c){return(c>='0'&&c<='9')||c=='-'||c=='+'||c=='.';}
        private static boolean numberPart(char c){return(c>='0'&&c<='9')||c=='-'||c=='+'||c=='.'||c=='e'||c=='E';}
        private static void require(boolean ok,String name){if(!ok)throw new IllegalArgumentException("Invalid CJ "+name);}
        private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
    }

    private static final class FloatGrow{
        float[] a=new float[4096];int n;
        void add(float v){if(n==a.length)a=Arrays.copyOf(a,a.length*2);a[n++]=v;}
        float[] toArray(){return Arrays.copyOf(a,n);}
    }
    private static final class IntGrow{
        int[] a=new int[4096];int n;
        void add(int v){if(n==a.length)a=Arrays.copyOf(a,a.length*2);a[n++]=v;}
        int[] toArray(){return Arrays.copyOf(a,n);}
    }

    private static float clamp(float v,float min,float max){return Math.max(min,Math.min(max,v));}
}
