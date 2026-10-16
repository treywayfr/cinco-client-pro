package gg.cinco.client.mixin;

import gg.cinco.client.StarPlayerModels;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Final polish for the imported GTA SA CJ rig.
 *
 * The source animation contains root motion/rotation that makes the whole model lean slightly in
 * Minecraft F5.  We remove only that global root transform after the GTA clip is evaluated, leaving
 * the child joint animation intact.  We also orient/smooth the generated vertex normals so mixed
 * source winding cannot create dark triangular patches on CJ's tank, skin and jeans.
 */
@Mixin(targets = "gg.cinco.client.GtaCjImportedModel$Model", remap = false)
public abstract class GtaCjPolishMixin {
    @Shadow @Final private int boneCount;
    @Shadow @Final private int positionCount;
    @Shadow @Final private String[] boneNames;
    @Shadow @Final private float[] boneLocal;
    @Shadow @Final private float[][] world;
    @Shadow @Final private float[] skinned;
    @Shadow @Final private float[] smoothNormals;
    @Shadow @Final private float centerX;
    @Shadow @Final private float centerZ;

    @Inject(method = "buildPose", at = @At("TAIL"), remap = false)
    private void star$keepCjUpright(StarPlayerModels.Pose pose, float seconds, boolean moving, CallbackInfo ci) {
        int root = -1;
        for (int i = 0; i < boneCount; i++) {
            String name = boneNames[i] == null ? "" : boneNames[i].toLowerCase(java.util.Locale.ROOT);
            if (name.equals("root ground") || name.equals("root_ground") || name.equals("root")) {
                root = i;
                break;
            }
        }
        if (root < 0 || root >= world.length || root * 16 + 15 >= boneLocal.length) return;

        // Current root contains the GTA clip's global lean/root motion.  Build a correction which
        // maps it back to the static bind root, then apply that same correction to every bone.
        float[] bind = new float[16];
        System.arraycopy(boneLocal, root * 16, bind, 0, 16);
        float[] invCurrent = star$inverseRigid(world[root]);
        float[] correction = star$mul(bind, invCurrent);
        for (int bone = 0; bone < boneCount && bone < world.length; bone++) {
            float[] corrected = star$mul(correction, world[bone]);
            System.arraycopy(corrected, 0, world[bone], 0, 16);
        }
    }

    @Inject(method = "buildSmoothNormals", at = @At("TAIL"), remap = false)
    private void star$cleanCjNormals(CallbackInfo ci) {
        // The converted FBX contains a few regions with opposite triangle winding.  The base
        // renderer already averages normals by vertex; this pass makes every side-facing normal
        // consistently point away from the character's centerline, then adds a small radial blend.
        // It removes the checker/triangle shading without flattening CJ into an emissive texture.
        for (int p = 0; p < positionCount; p++) {
            int i = p * 3;
            if (i + 2 >= skinned.length || i + 2 >= smoothNormals.length) break;

            float rx = skinned[i] - centerX;
            float rz = -skinned[i + 1] - centerZ;
            float radialLength = (float)Math.sqrt(rx * rx + rz * rz);
            if (radialLength < 1.0e-4f) continue;
            rx /= radialLength;
            rz /= radialLength;

            float nx = smoothNormals[i];
            float ny = smoothNormals[i + 1];
            float nz = smoothNormals[i + 2];

            if (nx * rx + nz * rz < 0f) {
                nx = -nx;
                ny = -ny;
                nz = -nz;
            }

            // Subtle radial bias = smoother GTA-style character lighting while preserving face,
            // shoulder, clothing and shoe shape from the real mesh.
            final float radialBlend = 0.22f;
            nx = nx * (1f - radialBlend) + rx * radialBlend;
            nz = nz * (1f - radialBlend) + rz * radialBlend;
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1.0e-6f) {
                smoothNormals[i] = nx / len;
                smoothNormals[i + 1] = ny / len;
                smoothNormals[i + 2] = nz / len;
            }
        }
    }

    private static float[] star$inverseRigid(float[] m) {
        float[] out = new float[16];
        // Inverse of an orthonormal affine matrix: R^-1 = R^T, t^-1 = -R^T t.
        out[0] = m[0]; out[1] = m[4]; out[2] = m[8];
        out[4] = m[1]; out[5] = m[5]; out[6] = m[9];
        out[8] = m[2]; out[9] = m[6]; out[10] = m[10];
        out[3] = -(out[0] * m[3] + out[1] * m[7] + out[2] * m[11]);
        out[7] = -(out[4] * m[3] + out[5] * m[7] + out[6] * m[11]);
        out[11] = -(out[8] * m[3] + out[9] * m[7] + out[10] * m[11]);
        out[12] = 0f; out[13] = 0f; out[14] = 0f; out[15] = 1f;
        return out;
    }

    private static float[] star$mul(float[] a, float[] b) {
        float[] out = new float[16];
        for (int r = 0; r < 4; r++) {
            int rr = r * 4;
            for (int c = 0; c < 4; c++) {
                out[rr + c] = a[rr] * b[c]
                        + a[rr + 1] * b[4 + c]
                        + a[rr + 2] * b[8 + c]
                        + a[rr + 3] * b[12 + c];
            }
        }
        return out;
    }
}
