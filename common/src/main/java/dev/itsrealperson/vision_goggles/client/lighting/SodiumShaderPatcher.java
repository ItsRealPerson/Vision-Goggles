package dev.itsrealperson.vision_goggles.client.lighting;

public class SodiumShaderPatcher {
    private static final String VSH_MARKER          = "_get_draw_translation";
    private static final String VSH_POSITION_ANCHOR = "vec3 position = _vert_position + translation;";
    private static final String FSH_FRAGCOLOR_ANCHOR = "fragColor = _linearFog(";
    private static final String GUARD               = "// VISION_FLASHLIGHT";

    public static String patch(String src) {
        if (src == null || src.isEmpty() || src.contains(GUARD)) {
            return src;
        }
        boolean isVsh = src.contains(VSH_MARKER) && src.contains(VSH_POSITION_ANCHOR);
        boolean isFsh = !isVsh && src.contains(FSH_FRAGCOLOR_ANCHOR) && src.contains("u_BlockTex");

        if (isVsh) return patchVertex(src);
        if (isFsh) return patchFragment(src);
        return src;
    }

    private static String patchVertex(String src) {
        String decl = "// VISION_FLASHLIGHT\nout vec3 v_VisionWorldPosRelCam;\nout vec4 v_VisionBiomeTint;\n";
        src = insertBeforeMain(src, decl);

        String assign = "vec3 position = _vert_position + translation;\n    v_VisionWorldPosRelCam = position; v_VisionBiomeTint = _vert_color; // VISION_FLASHLIGHT\n";
        src = src.replace(VSH_POSITION_ANCHOR, assign);
        return src;
    }

    private static String patchFragment(String src) {
        String preamble =
                "// VISION_FLASHLIGHT\n" +
                "#define MAX_VISION_FLASHLIGHTS 16\n" +
                "in vec3 v_VisionWorldPosRelCam;\n" +
                "in vec4 v_VisionBiomeTint;\n" +
                "uniform int   VisionFlashlightCount;\n" +
                "uniform vec3  VisionFlashlightsPos[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform vec3  VisionFlashlightsDir[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform vec3  VisionFlashlightsColor[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform float VisionFlashlightsConeInner[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform float VisionFlashlightsConeOuter[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform float VisionFlashlightsRange[MAX_VISION_FLASHLIGHTS];\n" +
                "uniform float VisionFlashlightsIntensity[MAX_VISION_FLASHLIGHTS];\n" +
                "\n" +
                "vec3 applyVisionFlashlight(vec3 color) {\n" +
                "    vec3 addedLight = vec3(0.0);\n" +
                "    for (int i = 0; i < VisionFlashlightCount; i++) {\n" +
                "        vec3 col = VisionFlashlightsColor[i];\n" +
                "        if (col.r <= 0.01 && col.g <= 0.01 && col.b <= 0.01) continue;\n" +
                "        float maxRange = VisionFlashlightsRange[i];\n" +
                "        vec3 lightDir = v_VisionWorldPosRelCam - VisionFlashlightsPos[i];\n" +
                "        float dist = length(lightDir);\n" +
                "        if (dist > maxRange) continue;\n" +
                "        lightDir = normalize(lightDir);\n" +
                "        float spotEffect = dot(lightDir, VisionFlashlightsDir[i]);\n" +
                "        float inner = VisionFlashlightsConeInner[i];\n" +
                "        float outer = VisionFlashlightsConeOuter[i];\n" +
                "        if (spotEffect > inner) {\n" +
                "            float attenuation = clamp(1.0 - (dist / maxRange), 0.0, 1.0);\n" +
                "            float falloff = smoothstep(inner, outer, spotEffect);\n" +
                "            addedLight += col * attenuation * falloff * VisionFlashlightsIntensity[i] * 2.0;\n" +
                "        }\n" +
                "    }\n" +
                "    return min(color + addedLight, vec3(1.0));\n" +
                "}\n";

        src = insertBeforeMain(src, preamble);

        String target = "diffuseColor.rgb *= v_Color.rgb;";
        if (src.contains(target)) {
            String patched = "diffuseColor.rgb *= min(v_Color.rgb + v_VisionBiomeTint.rgb * applyVisionFlashlight(vec3(0.0)), vec3(1.2)); // VISION_FLASHLIGHT";
            src = src.replace(target, patched);
        }
        String targetVanilla = "diffuseColor *= v_Color;";
        if (src.contains(targetVanilla)) {
            String patchedVanilla = "diffuseColor.rgb *= min(v_Color.rgb + v_VisionBiomeTint.rgb * applyVisionFlashlight(vec3(0.0)), vec3(1.2)); // VISION_FLASHLIGHT";
            src = src.replace(targetVanilla, patchedVanilla);
        }

        return src;
    }

    private static String insertBeforeMain(String src, String text) {
        int m = src.indexOf("void main(");
        if (m < 0) return src;
        return src.substring(0, m) + text + "\n" + src.substring(m);
    }
}
