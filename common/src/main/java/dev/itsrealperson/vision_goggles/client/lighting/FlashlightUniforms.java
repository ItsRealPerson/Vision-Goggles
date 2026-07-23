package dev.itsrealperson.vision_goggles.client.lighting;

import dev.itsrealperson.vision_goggles.util.FlashlightMode;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Vector3f;

public class FlashlightUniforms {
    /** Hard GLSL limit — must match #define MAX_VISION_FLASHLIGHTS in the shaders. Never change without updating GLSL. */
    public static final int GLSL_MAX = 16;
    public static int currentCount = 0;
    public static final float[] posArray       = new float[GLSL_MAX * 3];
    public static final float[] dirArray       = new float[GLSL_MAX * 3];
    public static final float[] colArray       = new float[GLSL_MAX * 3];
    public static final float[] coneInnerArray = new float[GLSL_MAX];
    public static final float[] coneOuterArray = new float[GLSL_MAX];
    public static final float[] rangeArray     = new float[GLSL_MAX];
    public static final float[] intensityArray = new float[GLSL_MAX];

    public static final org.joml.Quaternionf currentCamRot = new org.joml.Quaternionf();

    public static void beginUpdate(org.joml.Quaternionf camRot) {
        currentCount = 0;
        for (int i = 0; i < GLSL_MAX * 3; i++) {
            posArray[i] = 0;
            dirArray[i] = 0;
            colArray[i] = 0;
        }
        for (int i = 0; i < GLSL_MAX; i++) {
            coneInnerArray[i] = 0;
            coneOuterArray[i] = 0;
            rangeArray[i]     = 0;
            intensityArray[i] = 0;
        }
        if (camRot != null) {
            currentCamRot.set(camRot);
        }
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f color, FlashlightMode mode) {
        // Java-side cap: respects server config (1–32). GLSL arrays are always sized to GLSL_MAX.
        int serverLimit = dev.itsrealperson.vision_goggles.util.ModConfig.getMaxFlashlights();
        if (currentCount >= serverLimit) return;

        int offset = currentCount * 3;
        posArray[offset]     = pos.x();
        posArray[offset + 1] = pos.y();
        posArray[offset + 2] = pos.z();

        dirArray[offset]     = dir.x();
        dirArray[offset + 1] = dir.y();
        dirArray[offset + 2] = dir.z();

        colArray[offset]     = color.x();
        colArray[offset + 1] = color.y();
        colArray[offset + 2] = color.z();

        coneInnerArray[currentCount] = mode.coneInner;
        coneOuterArray[currentCount] = mode.coneOuter;
        rangeArray[currentCount]     = mode.range;
        intensityArray[currentCount] = mode.intensity;

        currentCount++;
    }

    /** Aplica todos los uniforms a un shader Vanilla */
    public static void applyToVanilla(ShaderInstance shader) {
        if (shader == null) return;
        int prog = shader.getId();

        int locCount     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightCount");
        int locPos       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsPos");
        int locDir       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsDir");
        int locCol       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsColor");
        int locInner     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsConeInner");
        int locOuter     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsConeOuter");
        int locRange     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange");
        int locIntensity = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsIntensity");

        if (locCount != -1)     org.lwjgl.opengl.GL20.glUniform1i(locCount, currentCount);
        if (locPos != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locPos, posArray);
        if (locDir != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locDir, dirArray);
        if (locCol != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locCol, colArray);
        if (locInner != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locInner, coneInnerArray);
        if (locOuter != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locOuter, coneOuterArray);
        if (locRange != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locRange, rangeArray);
        if (locIntensity != -1) org.lwjgl.opengl.GL20.glUniform1fv(locIntensity, intensityArray);

        int locInv = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "IViewRotMat");
        if (locInv != -1) {
            net.minecraft.client.Camera cam = net.minecraft.client.Minecraft.getInstance().gameRenderer.getMainCamera();
            float pitch = cam.getXRot();
            float yaw   = cam.getYRot();
            org.joml.Matrix3f inv = new org.joml.Matrix3f()
                .rotationY((float) Math.toRadians(-(yaw + 180.0f)))
                .rotateX((float) Math.toRadians(-pitch));

            java.nio.FloatBuffer buf = org.lwjgl.system.MemoryUtil.memAllocFloat(9);
            inv.get(buf);
            org.lwjgl.opengl.GL20.glUniformMatrix3fv(locInv, false, buf);
            org.lwjgl.system.MemoryUtil.memFree(buf);
        }
    }
}
