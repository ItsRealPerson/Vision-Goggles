package dev.itsrealperson.vision_goggles.client.lighting;

import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.resources.ResourceLocation;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.stream.Collectors;

public class ShaderRewriter {

    private static final String FLASHLIGHT_GLSL =
            "#define MAX_VISION_FLASHLIGHTS 16\n" +
            "uniform int   VisionFlashlightCount;\n" +
            "uniform vec3  VisionFlashlightsPos[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform vec3  VisionFlashlightsDir[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform vec3  VisionFlashlightsUp[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform vec3  VisionFlashlightsRight[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform vec3  VisionFlashlightsColor[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsConeInner[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsConeOuter[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsIntensity[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsIsLocal[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsIsWall[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange0[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange1[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange2[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange3[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange4[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange5[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange6[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange7[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange8[MAX_VISION_FLASHLIGHTS];\n" +
            "\n" +
            "vec3 getVisionFlashlight(vec3 vertexPos) {\n" +
            "    vec3 totalLight = vec3(0.0);\n" +
            "    for (int i = 0; i < VisionFlashlightCount; i++) {\n" +
            "        vec3 col = VisionFlashlightsColor[i];\n" +
            "        if (col.r <= 0.01 && col.g <= 0.01 && col.b <= 0.01) continue;\n" +
            "        vec3 toFragment = vertexPos - VisionFlashlightsPos[i];\n" +
            "        float depthAlongAxis = dot(toFragment, VisionFlashlightsDir[i]);\n" +
            "        if (depthAlongAxis <= 0.0) continue;\n" +
            "        float dist = length(toFragment);\n" +
            "        vec3 lightDir = normalize(toFragment);\n" +
            "        float spotEffect = dot(lightDir, VisionFlashlightsDir[i]);\n" +
            "        float inner = VisionFlashlightsConeInner[i];\n" +
            "        float outer = VisionFlashlightsConeOuter[i];\n" +
            "        if (spotEffect > inner) {\n" +
            "            float physRange = (inner > 0.90) ? 32.0 : ((inner > 0.50) ? 16.0 : 8.0);\n" +
            "            vec3 fNormal = normalize(cross(dFdx(vertexPos), dFdy(vertexPos)));\n" +
            "            if (dot(fNormal, -lightDir) <= 0.01) continue;\n" +
            "            if (VisionFlashlightsIsLocal[i] > 0.5) {\n" +
            "                if (depthAlongAxis > physRange) continue;\n" +
            "                float attenuation = 1.0 / (1.0 + 0.05 * dist + 0.02 * dist * dist);\n" +
            "                attenuation *= 1.0 - smoothstep(physRange * 0.8, physRange, dist);\n" +
            "                float falloff = smoothstep(inner, outer, spotEffect);\n" +
            "                totalLight += col * attenuation * falloff * VisionFlashlightsIntensity[i];\n" +
            "                continue;\n" +
            "            }\n" +
            "            float rCenter = VisionFlashlightsRange0[i];\n" +
            "            float maxDepth = (rCenter > 0.5 && rCenter < physRange) ? (rCenter + 0.6) : physRange;\n" +
            "            if (depthAlongAxis > maxDepth) continue;\n" +
            "            float attenuation = 1.0 / (1.0 + 0.05 * dist + 0.02 * dist * dist);\n" +
            "            attenuation *= 1.0 - smoothstep(physRange * 0.8, physRange, dist);\n" +
            "            if (rCenter > 0.5 && rCenter < physRange) {\n" +
            "                attenuation *= 1.0 - smoothstep(rCenter - 0.2, maxDepth, depthAlongAxis);\n" +
            "            }\n" +
            "            float falloff = smoothstep(inner, outer, spotEffect);\n" +
            "            totalLight += col * attenuation * falloff * VisionFlashlightsIntensity[i];\n" +
            "        }\n" +
            "    }\n" +
            "    return totalLight;\n" +
            "}";

    public static ResourceProvider wrap(ResourceProvider provider) {
        return location -> {
            Optional<Resource> resourceOpt = provider.getResource(location);
            if (resourceOpt.isEmpty()) return resourceOpt;

            String path = location.getPath();
            boolean isFsh = path.endsWith(".fsh");
            boolean isVsh = path.endsWith(".vsh");
            if ((isFsh || isVsh) && (path.contains("rendertype_solid") || path.contains("rendertype_cutout") || path.contains("rendertype_translucent") || path.contains("rendertype_entity") || path.contains("rendertype_water") || path.contains("particle"))) {
                try {
                    Resource res = resourceOpt.get();
                    InputStream is = res.open();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
                    String source = reader.lines().collect(Collectors.joining("\n"));

                    String patched = isFsh ? patchFsh(source) : patchVsh(source);

                    InputStream newIs = new ByteArrayInputStream(patched.getBytes(StandardCharsets.UTF_8));
                    return Optional.of(new Resource(res.source(), () -> newIs, () -> res.metadata()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return resourceOpt;
        };
    }

    public static String patchFsh(String source) {
        if (source.contains("VisionFlashlightCount")) return source;
        source = source.replaceFirst("#version 150", "#version 150\n" + "in vec3 v_VisionPos;\n" + FLASHLIGHT_GLSL);

        int mainIdx = source.indexOf("void main()");
        if (mainIdx != -1) {
            String beforeMain = source.substring(0, mainIdx);
            String afterMain  = source.substring(mainIdx);

            if (afterMain.contains("lightMapColor")) {
                afterMain = afterMain.replace("lightMapColor", "min(lightMapColor + vec4(getVisionFlashlight(v_VisionPos), 0.0), vec4(1.0))");
            } else if (afterMain.contains("vertexColor")) {
                afterMain = afterMain.replace("vertexColor", "min(vertexColor + vec4(getVisionFlashlight(v_VisionPos), 0.0), vec4(1.0))");
            } else if (afterMain.contains("ColorModulator")) {
                afterMain = afterMain.replace("ColorModulator", "min(ColorModulator + vec4(getVisionFlashlight(v_VisionPos), 0.0), vec4(1.0))");
            }
            source = beforeMain + afterMain;
        }
        return source;
    }

    public static String patchVsh(String source) {
        if (source.contains("v_VisionPos")) return source;

        String declaration = "#version 150\nout vec3 v_VisionPos;\n";
        if (!source.contains("ChunkOffset") && source.contains("ModelViewMat") && !source.contains("IViewRotMat")) {
            declaration += "uniform mat3 IViewRotMat;\n";
        }
        source = source.replaceFirst("#version 150", declaration);

        int mainEnd = source.lastIndexOf("}");
        if (mainEnd != -1) {
            String transform = "    v_VisionPos = Position;\n}\n";
            if (source.contains("ChunkOffset")) {
                transform = "    v_VisionPos = Position + ChunkOffset;\n}\n";
            } else if (source.contains("ModelViewMat")) {
                transform = "    v_VisionPos = IViewRotMat * (ModelViewMat * vec4(Position, 1.0)).xyz;\n}\n";
            }
            source = source.substring(0, mainEnd) + transform;
        }
        return source;
    }
}
