package dev.itsrealperson.vision_goggles.client.lighting;

import org.lwjgl.opengl.GL20;
import java.util.HashMap;
import java.util.Map;

public class SodiumFlashlightUniforms {
    private static final Map<Integer, int[]> locCache = new HashMap<>();

    // Indices into the cached location array
    private static final int LOC_COUNT     = 0;
    private static final int LOC_POS       = 1;
    private static final int LOC_DIR       = 2;
    private static final int LOC_COL       = 3;
    private static final int LOC_CONE_IN   = 4;
    private static final int LOC_CONE_OUT  = 5;
    private static final int LOC_RANGE     = 6;
    private static final int LOC_INTENSITY = 7;
    private static final int LOC_UP        = 8;
    private static final int LOC_RIGHT     = 9;
    private static final int LOC_RANGE0    = 10;
    private static final int LOC_RANGE1    = 11;
    private static final int LOC_RANGE2    = 12;
    private static final int LOC_RANGE3    = 13;
    private static final int LOC_RANGE4    = 14;
    private static final int LOC_RANGE5    = 15;
    private static final int LOC_RANGE6    = 16;
    private static final int LOC_RANGE7    = 17;
    private static final int LOC_RANGE8    = 18;
    private static final int LOC_IS_LOCAL  = 19;
    private static final int LOC_COUNT_TOTAL = 20;

    public static void upload(int program) {
        int[] locs = locCache.computeIfAbsent(program, p -> {
            int[] l = new int[LOC_COUNT_TOTAL];
            l[LOC_COUNT]     = GL20.glGetUniformLocation(p, "VisionFlashlightCount");
            l[LOC_POS]       = GL20.glGetUniformLocation(p, "VisionFlashlightsPos");
            l[LOC_DIR]       = GL20.glGetUniformLocation(p, "VisionFlashlightsDir");
            l[LOC_COL]       = GL20.glGetUniformLocation(p, "VisionFlashlightsColor");
            l[LOC_CONE_IN]   = GL20.glGetUniformLocation(p, "VisionFlashlightsConeInner");
            l[LOC_CONE_OUT]  = GL20.glGetUniformLocation(p, "VisionFlashlightsConeOuter");
            l[LOC_RANGE]     = GL20.glGetUniformLocation(p, "VisionFlashlightsRange");
            l[LOC_INTENSITY] = GL20.glGetUniformLocation(p, "VisionFlashlightsIntensity");
            l[LOC_UP]        = GL20.glGetUniformLocation(p, "VisionFlashlightsUp");
            l[LOC_RIGHT]     = GL20.glGetUniformLocation(p, "VisionFlashlightsRight");
            l[LOC_RANGE0]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange0");
            l[LOC_RANGE1]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange1");
            l[LOC_RANGE2]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange2");
            l[LOC_RANGE3]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange3");
            l[LOC_RANGE4]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange4");
            l[LOC_RANGE5]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange5");
            l[LOC_RANGE6]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange6");
            l[LOC_RANGE7]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange7");
            l[LOC_RANGE8]    = GL20.glGetUniformLocation(p, "VisionFlashlightsRange8");
            l[LOC_IS_LOCAL]  = GL20.glGetUniformLocation(p, "VisionFlashlightsIsLocal");
            return l;
        });

        if (locs[LOC_COUNT]     != -1) GL20.glUniform1i(locs[LOC_COUNT], FlashlightUniforms.currentCount);
        if (locs[LOC_POS]       != -1) GL20.glUniform3fv(locs[LOC_POS], FlashlightUniforms.posArray);
        if (locs[LOC_DIR]       != -1) GL20.glUniform3fv(locs[LOC_DIR], FlashlightUniforms.dirArray);
        if (locs[LOC_COL]       != -1) GL20.glUniform3fv(locs[LOC_COL], FlashlightUniforms.colArray);
        if (locs[LOC_CONE_IN]   != -1) GL20.glUniform1fv(locs[LOC_CONE_IN],   FlashlightUniforms.coneInnerArray);
        if (locs[LOC_CONE_OUT]  != -1) GL20.glUniform1fv(locs[LOC_CONE_OUT],  FlashlightUniforms.coneOuterArray);
        if (locs[LOC_RANGE]     != -1) GL20.glUniform1fv(locs[LOC_RANGE],     FlashlightUniforms.rangeArray);
        if (locs[LOC_INTENSITY] != -1) GL20.glUniform1fv(locs[LOC_INTENSITY], FlashlightUniforms.intensityArray);
        if (locs[LOC_UP]        != -1) GL20.glUniform3fv(locs[LOC_UP],        FlashlightUniforms.upArray);
        if (locs[LOC_RIGHT]     != -1) GL20.glUniform3fv(locs[LOC_RIGHT],     FlashlightUniforms.rightArray);
        if (locs[LOC_RANGE0]    != -1) GL20.glUniform1fv(locs[LOC_RANGE0],    FlashlightUniforms.range0Array);
        if (locs[LOC_RANGE1]    != -1) GL20.glUniform1fv(locs[LOC_RANGE1],    FlashlightUniforms.range1Array);
        if (locs[LOC_RANGE2]    != -1) GL20.glUniform1fv(locs[LOC_RANGE2],    FlashlightUniforms.range2Array);
        if (locs[LOC_RANGE3]    != -1) GL20.glUniform1fv(locs[LOC_RANGE3],    FlashlightUniforms.range3Array);
        if (locs[LOC_RANGE4]    != -1) GL20.glUniform1fv(locs[LOC_RANGE4],    FlashlightUniforms.range4Array);
        if (locs[LOC_RANGE5]    != -1) GL20.glUniform1fv(locs[LOC_RANGE5],    FlashlightUniforms.range5Array);
        if (locs[LOC_RANGE6]    != -1) GL20.glUniform1fv(locs[LOC_RANGE6],    FlashlightUniforms.range6Array);
        if (locs[LOC_RANGE7]    != -1) GL20.glUniform1fv(locs[LOC_RANGE7],    FlashlightUniforms.range7Array);
        if (locs[LOC_RANGE8]    != -1) GL20.glUniform1fv(locs[LOC_RANGE8],    FlashlightUniforms.range8Array);
        if (locs[LOC_IS_LOCAL]  != -1) GL20.glUniform1fv(locs[LOC_IS_LOCAL],  FlashlightUniforms.isLocalArray);
    }
}
