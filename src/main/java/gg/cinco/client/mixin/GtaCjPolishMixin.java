package gg.cinco.client.mixin;

import gg.cinco.client.StarPlayerModels;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Locale;

/**
 * Hard visual cleanup for the imported GTA SA CJ model.
 *
 * Earlier builds tried to repair the animation hierarchy itself. That can still leave the rendered
 * mesh leaning because the bind rig and the animated pelvis/spine chain contain their own offsets.
 * This version fixes the thing the player actually sees: after skinning, rigidly rotate the completed
 * mesh around CJ's pelvis until the pelvis -> upper-spine axis is exactly aligned with source +Z
 * (Minecraft +Y after the model's coordinate conversion). This removes both sideways roll and
 * forward/backward lean without distorting individual limbs or the texture.
 *
 * The source asset is a low-poly triangle mesh. Per-triangle/per-seam normals make the white tank,
 * skin and jeans look like a patchwork quilt under Minecraft's directional entity lighting. For CJ
 * we intentionally use one continuous upward normal for all vertices. The diffuse GTA texture keeps
 * all of the actual visual detail while lighting can no longer expose every triangle boundary.
 */
@Mixin(targets = "gg.cinco.client.GtaCjImportedModel$Model", remap = false)
public abstract class GtaCjPolishMixin {
    @Shadow @Final private int boneCount;
    @Shadow @Final private int positionCount;
    @Shadow @Final private String[] boneNames;
    @Shadow @Final private float[][] world;
    @Shadow @Final private float[] skinned;
    @Shadow @Final private float[] smoothNormals;

    /**
     * Straighten the final skinned geometry, not just one animation bone.
     * Source coordinates are X/Y horizontal and Z vertical.
     */
    @Inject(method = "skinPositions", at = @At("TAIL"), remap = false)
    private void star$forceFinishedCjMeshUpright(CallbackInfo ci) {
        if (positionCount <= 0 || skinned.length < positionCount * 3) return;

        int pelvis = star$findBone("pelvis");
        int upperSpine = star$findBone("spine upper");
        if (pelvis < 0 || upperSpine < 0 || pelvis >= world.length || upperSpine >= world.length) return;

        float px = world[pelvis][3];
        float py = world[pelvis][7];
        float pz = world[pelvis][11];

        float vx = world[upperSpine][3] - px;
        float vy = world[upperSpine][7] - py;
        float vz = world[upperSpine][11] - pz;
        float length = (float)Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (length < 1.0e-5f) return;
        vx /= length;
        vy /= length;
        vz /= length;

        // Shortest rigid rotation that maps the current torso axis to exact source +Z.
        // cross(v, +Z) = (vy, -vx, 0).  This fixes roll and pitch while leaving yaw alone.
        float horizontal = (float)Math.sqrt(vx * vx + vy * vy);
        if (horizontal < 1.0e-5f) return;

        float ax = vy / horizontal;
        float ay = -vx / horizontal;
        float az = 0f;
        float sin = horizontal;
        float cos = Math.max(-1f, Math.min(1f, vz));
        float oneMinusCos = 1f - cos;

        for (int p = 0; p < positionCount; p++) {
            int i = p * 3;
            float x = skinned[i] - px;
            float y = skinned[i + 1] - py;
            float z = skinned[i + 2] - pz;

            // Rodrigues rotation: v*cos + (axis x v)*sin + axis*(axis dot v)*(1-cos)
            float crossX = ay * z - az * y;
            float crossY = az * x - ax * z;
            float crossZ = ax * y - ay * x;
            float dot = ax * x + ay * y + az * z;

            float rx = x * cos + crossX * sin + ax * dot * oneMinusCos;
            float ry = y * cos + crossY * sin + ay * dot * oneMinusCos;
            float rz = z * cos + crossZ * sin + az * dot * oneMinusCos;

            skinned[i] = px + rx;
            skinned[i + 1] = py + ry;
            skinned[i + 2] = pz + rz;
        }
    }

    /**
     * Kill polygon-by-polygon directional shading. The GTA diffuse atlas already contains the visual
     * detail we want; a shared normal makes the body shade as one continuous character instead of
     * hundreds of differently lit triangles.
     */
    @Inject(method = "buildSmoothNormals", at = @At("TAIL"), remap = false)
    private void star$makeCjLightingContinuous(CallbackInfo ci) {
        int count = Math.min(positionCount, smoothNormals.length / 3);
        for (int p = 0; p < count; p++) {
            int i = p * 3;
            smoothNormals[i] = 0f;
            smoothNormals[i + 1] = 1f;
            smoothNormals[i + 2] = 0f;
        }
    }

    private int star$findBone(String wanted) {
        for (int i = 0; i < boneCount && i < boneNames.length; i++) {
            String name = boneNames[i] == null ? "" : boneNames[i].toLowerCase(Locale.ROOT).trim();
            if (name.equals(wanted)) return i;
        }
        return -1;
    }
}
