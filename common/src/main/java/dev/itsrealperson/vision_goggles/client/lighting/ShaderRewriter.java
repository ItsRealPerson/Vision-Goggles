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
            "uniform vec3  VisionFlashlightsColor[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsConeInner[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsConeOuter[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsRange[MAX_VISION_FLASHLIGHTS];\n" +
            "uniform float VisionFlashlightsIntensity[MAX_VISION_FLASHLIGHTS];\n" +
            "\n" +
            "vec3 getVisionFlashlight(vec3 vertexPos) {\n" +
            "    vec3 totalLight = vec3(0.0);\n" +
            "    for (int i = 0; i < VisionFlashlightCount; i++) {\n" +
            "        vec3 col = VisionFlashlightsColor[i];\n" +
            "        if (col.r <= 0.01 && col.g <= 0.01 && col.b <= 0.01) continue;\n" +
            "        float maxRange = VisionFlashlightsRange[i];\n" +
            "        vec3 lightDir = vertexPos - VisionFlashlightsPos[i];\n" +
            "        float dist = length(lightDir);\n" +
            "        if (dist > maxRange) continue;\n" +
            "        lightDir = normalize(lightDir);\n" +
            "        float spotEffect = dot(lightDir, VisionFlashlightsDir[i]);\n" +
            "        float inner = VisionFlashlightsConeInner[i];\n" +
            "        float outer = VisionFlashlightsConeOuter[i];\n" +
            "        if (spotEffect > inner) {\n" +
            "            float attenuation = clamp(1.0 - (dist / maxRange), 0.0, 1.0);\n" +
            "            float falloff = smoothstep(inner, outer, spotEffect);\n" +
            "            totalLight += col * attenuation * falloff * VisionFlashlightsIntensity[i];\n" +
            "        }\n" +
            "    }\n" +
            "    return totalLight;\n" +
            "}\n";

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
