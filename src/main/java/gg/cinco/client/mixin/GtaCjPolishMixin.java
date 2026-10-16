package gg.cinco.client.mixin;

import gg.cinco.client.StarPlayerModels;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Final posture/shading cleanup for the imported GTA SA CJ rig.
 *
 * The GTASA hierarchy is root ground -> root hips -> pelvis.  The animation clips can rotate the
 * latter two enough to make the whole character appear permanently rolled/sideways in Minecraft.
 * We restore only those global posture bones to their bind orientation after the clip is evaluated,
 * preserving their animated translation and every child joint's own animation.
 *
 * The converted texture atlas duplicates vertices along UV/material seams.  Lighting those copies
 * independently exposes triangle-shaped patches even though the diffuse texture itself is correct.
 * We weld normals by quantized skinned position so coincident seam vertices share one smooth normal.
 */
@Mixin(targets = "gg.cinco.client.GtaCjImportedModel$Model", remap = false)
public abstract class GtaCjPolishMixin {
    @Shadow @Final private int boneCount;
    @Shadow @Final private int positionCount;
    @Shadow @Final private String[] boneNames;
    @Shadow @Final private int[] boneParent;
    @Shadow @Final private float[] boneLocal;
    @Shadow @Final private float[][] world;
    @Shadow @Final private float[] skinned;
    @Shadow @Final private float[] smoothNormals;

    @Inject(method = "buildPose", at = @At("TAIL"), remap = false)
    private void star$keepCjActuallyUpright(StarPlayerModels.Pose pose, float seconds, boolean moving, CallbackInfo ci) {
        // root ground is already stripped of most travel by the source converter.  The visible lean
        // in F5 comes from the next two posture bones, so stabilize those instead of the top root.
        int rootHips = star$findBone("root hips");
        if (rootHips >= 0) star$restoreBindRotationForSubtree(rootHips);

        int pelvis = star$findBone("pelvis");
        if (pelvis >= 0) star$restoreBindRotationForSubtree(pelvis);
    }

    @Inject(method = "buildSmoothNormals", at = @At("TAIL"), remap = false)
    private void star$weldCjSeamNormals(CallbackInfo ci) {
        if (positionCount <= 0 || skinned.length < positionCount * 3 || smoothNormals.length < positionCount * 3) return;

        // Accumulate normals for vertices occupying the same skinned position.  The 1/5000 unit
        // quantization is tight enough not to merge distinct body surfaces while still catching
        // duplicate vertices created purely for GTA texture/material seams.
        Map<String, float[]> groups = new HashMap<>(positionCount * 2);
        for (int p = 0; p < positionCount; p++) {
            int i = p * 3;
            String key = star$positionKey(skinned[i], skinned[i + 1], skinned[i + 2]);
            float[] sum = groups.computeIfAbsent(key, ignored -> new float[3]);
            float nx = smoothNormals[i];
            float ny = smoothNormals[i + 1];
            float nz = smoothNormals[i + 2];
            float len = (float)Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (len > 1.0e-6f) {
                sum[0] += nx / len;
                sum[1] += ny / len;
                sum[2] += nz / len;
            }
        }

        for (float[] n : groups.values()) {
            float len = (float)Math.sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]);
            if (len > 1.0e-6f) {
                n[0] /= len;
                n[1] /= len;
                n[2] /= len;
            }
        }

        for (int p = 0; p < positionCount; p++) {
            int i = p * 3;
            float[] n = groups.get(star$positionKey(skinned[i], skinned[i + 1], skinned[i + 2]));
            if (n == null) continue;
            smoothNormals[i] = n[0];
            smoothNormals[i + 1] = n[1];
            smoothNormals[i + 2] = n[2];
        }
    }

    private int star$findBone(String wanted) {
        for (int i = 0; i < boneCount && i < boneNames.length; i++) {
            String name = boneNames[i] == null ? "" : boneNames[i].toLowerCase(Locale.ROOT).trim();
            if (name.equals(wanted)) return i;
        }
        return -1;
    }

    /**
     * Makes this bone's world rotation equal its bind-pose rotation under its already-corrected
     * current parent, while keeping the current animated world translation.  The resulting rigid
     * correction is propagated through the whole subtree, so child animation is preserved.
     */
    private void star$restoreBindRotationForSubtree(int bone) {
        if (bone < 0 || bone >= world.length || bone * 16 + 15 >= boneLocal.length) return;

        int parent = bone < boneParent.length ? boneParent[bone] - 1 : -1;
        float[] bindLocal = new float[16];
        System.arraycopy(boneLocal, bone * 16, bindLocal, 0, 16);

        float[] desired = (parent >= 0 && parent < world.length)
                ? star$mul(world[parent], bindLocal)
                : bindLocal.clone();

        // Keep GTA's subtle vertical/position motion, remove only the unwanted orientation.
        desired[3] = world[bone][3];
        desired[7] = world[bone][7];
        desired[11] = world[bone][11];
        desired[12] = 0f;
        desired[13] = 0f;
        desired[14] = 0f;
        desired[15] = 1f;
        star$orthonormalize(desired);

        float[] correction = star$mul(desired, star$inverseRigid(world[bone]));
        for (int candidate = 0; candidate < boneCount && candidate < world.length; candidate++) {
            if (!star$isInSubtree(candidate, bone)) continue;
            float[] fixed = star$mul(correction, world[candidate]);
            System.arraycopy(fixed, 0, world[candidate], 0, 16);
        }
    }

    private boolean star$isInSubtree(int candidate, int ancestor) {
        if (candidate == ancestor) return true;
        int current = candidate;
        for (int guard = 0; guard < boneCount; guard++) {
            if (current < 0 || current >= boneParent.length) return false;
            current = boneParent[current] - 1;
            if (current == ancestor) return true;
            if (current < 0 || current == candidate) return false;
        }
        return false;
    }

    private static String star$positionKey(float x, float y, float z) {
        int qx = Math.round(x * 5000f);
        int qy = Math.round(y * 5000f);
        int qz = Math.round(z * 5000f);
        return qx + ":" + qy + ":" + qz;
    }

    private static void star$orthonormalize(float[] m) {
        float x0 = m[0], x1 = m[1], x2 = m[2];
        float y0 = m[4], y1 = m[5], y2 = m[6];
        float oldZ0 = m[8], oldZ1 = m[9], oldZ2 = m[10];

        float xl = (float)Math.sqrt(x0 * x0 + x1 * x1 + x2 * x2);
        if (xl < 1.0e-6f) return;
        x0 /= xl; x1 /= xl; x2 /= xl;

        float dot = x0 * y0 + x1 * y1 + x2 * y2;
        y0 -= x0 * dot; y1 -= x1 * dot; y2 -= x2 * dot;
        float yl = (float)Math.sqrt(y0 * y0 + y1 * y1 + y2 * y2);
        if (yl < 1.0e-6f) return;
        y0 /= yl; y1 /= yl; y2 /= yl;

        float z0 = x1 * y2 - x2 * y1;
        float z1 = x2 * y0 - x0 * y2;
        float z2 = x0 * y1 - x1 * y0;
        if (z0 * oldZ0 + z1 * oldZ1 + z2 * oldZ2 < 0f) {
            z0 = -z0; z1 = -z1; z2 = -z2;
        }

        m[0] = x0; m[1] = x1; m[2] = x2;
        m[4] = y0; m[5] = y1; m[6] = y2;
        m[8] = z0; m[9] = z1; m[10] = z2;
    }

    private static float[] star$inverseRigid(float[] m) {
        float[] out = new float[16];
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
