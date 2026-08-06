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
    public static final float[] upArray        = new float[GLSL_MAX * 3];
    public static final float[] rightArray     = new float[GLSL_MAX * 3];
    public static final float[] range0Array    = new float[GLSL_MAX];
    public static final float[] range1Array    = new float[GLSL_MAX];
    public static final float[] range2Array    = new float[GLSL_MAX];
    public static final float[] range3Array    = new float[GLSL_MAX];
    public static final float[] range4Array    = new float[GLSL_MAX];
    public static final float[] range5Array    = new float[GLSL_MAX];
    public static final float[] range6Array    = new float[GLSL_MAX];
    public static final float[] range7Array    = new float[GLSL_MAX];
    public static final float[] range8Array    = new float[GLSL_MAX];
    public static final float[] isLocalArray   = new float[GLSL_MAX];
    public static final float[] isWallArray   = new float[GLSL_MAX];

    public static final org.joml.Quaternionf currentCamRot = new org.joml.Quaternionf();

    public static void beginUpdate(org.joml.Quaternionf camRot) {
        currentCount = 0;
        for (int i = 0; i < GLSL_MAX * 3; i++) {
            posArray[i] = 0;
            dirArray[i] = 0;
            colArray[i] = 0;
            upArray[i] = 0;
            rightArray[i] = 0;
        }
        for (int i = 0; i < GLSL_MAX; i++) {
            coneInnerArray[i] = 0;
            coneOuterArray[i] = 0;
            rangeArray[i]     = 0;
            intensityArray[i] = 0;
            range0Array[i]    = 0;
            range1Array[i]    = 0;
            range2Array[i]    = 0;
            range3Array[i]    = 0;
            range4Array[i]    = 0;
            range5Array[i]    = 0;
            range6Array[i]    = 0;
            range7Array[i]    = 0;
            range8Array[i]    = 0;
            isLocalArray[i]   = 0;
            isWallArray[i]   = 0;
        }
        if (camRot != null) {
            currentCamRot.set(camRot);
        }
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f color, FlashlightMode mode) {
        float[] dummy = new float[9];
        for (int i = 0; i < 9; i++) dummy[i] = mode.range;
        addFlashlight(pos, dir, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), color, mode, dummy, false, false);
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f color, FlashlightMode mode, float range) {
        float[] dummy = new float[9];
        for (int i = 0; i < 9; i++) dummy[i] = range;
        addFlashlight(pos, dir, new Vector3f(0, 1, 0), new Vector3f(1, 0, 0), color, mode, dummy, false, false);
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f up, Vector3f right, Vector3f color, FlashlightMode mode, float[] ranges) {
        addFlashlight(pos, dir, up, right, color, mode, ranges, false, false);
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f up, Vector3f right, Vector3f color, FlashlightMode mode, float[] ranges, boolean isLocal) {
        addFlashlight(pos, dir, up, right, color, mode, ranges, isLocal, false);
    }

    public static void addFlashlight(Vector3f pos, Vector3f dir, Vector3f up, Vector3f right, Vector3f color, FlashlightMode mode, float[] ranges, boolean isLocal, boolean isWall) {
        int serverLimit = dev.itsrealperson.vision_goggles.util.ModConfig.getMaxFlashlights();
        if (currentCount >= serverLimit) return;

        int offset = currentCount * 3;
        posArray[offset]     = pos.x();
        posArray[offset + 1] = pos.y();
        posArray[offset + 2] = pos.z();

        dirArray[offset]     = dir.x();
        dirArray[offset + 1] = dir.y();
        dirArray[offset + 2] = dir.z();

        upArray[offset]      = up.x();
        upArray[offset + 1]  = up.y();
        upArray[offset + 2]  = up.z();

        rightArray[offset]     = right.x();
        rightArray[offset + 1] = right.y();
        rightArray[offset + 2] = right.z();

        colArray[offset]     = color.x();
        colArray[offset + 1] = color.y();
        colArray[offset + 2] = color.z();

        coneInnerArray[currentCount] = mode.coneInner;
        coneOuterArray[currentCount] = mode.coneOuter;
        rangeArray[currentCount]     = ranges[0];
        intensityArray[currentCount] = mode.intensity;
        isLocalArray[currentCount]   = isLocal ? 1.0f : 0.0f;
        isWallArray[currentCount]   = isWall ? 1.0f : 0.0f;

        range0Array[currentCount] = ranges[0];
        range1Array[currentCount] = ranges[1];
        range2Array[currentCount] = ranges[2];
        range3Array[currentCount] = ranges[3];
        range4Array[currentCount] = ranges[4];
        range5Array[currentCount] = (ranges.length >= 9) ? ranges[5] : ranges[0];
        range6Array[currentCount] = (ranges.length >= 9) ? ranges[6] : ranges[0];
        range7Array[currentCount] = (ranges.length >= 9) ? ranges[7] : ranges[0];
        range8Array[currentCount] = (ranges.length >= 9) ? ranges[8] : ranges[0];

        currentCount++;
    }

    /** Aplica todos los uniforms a un shader Vanilla */
    public static void applyToVanilla(ShaderInstance shader) {
        if (shader == null) return;
        int prog = shader.getId();

        int locCount     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightCount");
        int locPos       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsPos");
        int locDir       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsDir");
        int locUp        = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsUp");
        int locRight     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRight");
        int locCol       = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsColor");
        int locInner     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsConeInner");
        int locOuter     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsConeOuter");
        int locRange     = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange");
        int locIntensity = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsIntensity");
        int locIsLocal   = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsIsLocal");

        int locRange0    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange0");
        int locRange1    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange1");
        int locRange2    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange2");
        int locRange3    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange3");
        int locRange4    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange4");
        int locRange5    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange5");
        int locRange6    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange6");
        int locRange7    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange7");
        int locRange8    = org.lwjgl.opengl.GL20.glGetUniformLocation(prog, "VisionFlashlightsRange8");

        if (locCount != -1)     org.lwjgl.opengl.GL20.glUniform1i(locCount, currentCount);
        if (locPos != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locPos, posArray);
        if (locDir != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locDir, dirArray);
        if (locUp != -1)        org.lwjgl.opengl.GL20.glUniform3fv(locUp, upArray);
        if (locRight != -1)     org.lwjgl.opengl.GL20.glUniform3fv(locRight, rightArray);
        if (locCol != -1)       org.lwjgl.opengl.GL20.glUniform3fv(locCol, colArray);
        if (locInner != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locInner, coneInnerArray);
        if (locOuter != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locOuter, coneOuterArray);
        if (locRange != -1)     org.lwjgl.opengl.GL20.glUniform1fv(locRange, rangeArray);
        if (locIntensity != -1) org.lwjgl.opengl.GL20.glUniform1fv(locIntensity, intensityArray);
        if (locIsLocal != -1)   org.lwjgl.opengl.GL20.glUniform1fv(locIsLocal, isLocalArray);

        if (locRange0 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange0, range0Array);
        if (locRange1 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange1, range1Array);
        if (locRange2 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange2, range2Array);
        if (locRange3 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange3, range3Array);
        if (locRange4 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange4, range4Array);
        if (locRange5 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange5, range5Array);
        if (locRange6 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange6, range6Array);
        if (locRange7 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange7, range7Array);
        if (locRange8 != -1)    org.lwjgl.opengl.GL20.glUniform1fv(locRange8, range8Array);

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
