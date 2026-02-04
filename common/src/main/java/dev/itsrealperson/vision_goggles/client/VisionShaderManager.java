package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.mixin.client.GameRendererAccessor;
import dev.itsrealperson.vision_goggles.mixin.client.PostChainAccessor;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public class VisionShaderManager {
    private static VisionMode activeMode = null;
    private static float currentBatteryPct = 1.0f;
    private static boolean damageFlicker = false;
    private static boolean flashlightActive = false;

    public static void setFlashlightActive(boolean active) {
        flashlightActive = active;
    }

    public static void update(VisionMode mode, float batteryPct, boolean flicker) {
        activeMode = mode;
        currentBatteryPct = batteryPct;
        damageFlicker = flicker;
        
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            updateUniforms(mc);
        }
    }

    public static void enableShader(Minecraft mc, VisionMode mode) {
        ResourceLocation loc;
        
        if (mode == null && flashlightActive) {
            loc = new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/flashlight.json");
        } else if (mode != null) {
            loc = mode.getShaderLocation();
            if (mode == VisionMode.HYDRO && !mc.player.isUnderWater()) {
                loc = new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/hydro_dry.json");
            }
        } else {
            return;
        }

        try {
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$loadEffect(loc);
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("Failed to load shader: " + loc, e);
        }
    }

    public static void disableShader(Minecraft mc) {
        try {
            PostChain effect = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
            if (effect != null) {
                effect.close();
                ((GameRendererAccessor) mc.gameRenderer).vision_goggles$setPostEffect(null);
            }
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("Failed to disable shader", e);
        }
    }

    private static void updateUniforms(Minecraft mc) {
        PostChain effect = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
        if (effect != null) {
            List<PostPass> passes = ((PostChainAccessor) effect).vision_goggles$getPasses();
            float time = (float) mc.level.getGameTime() + mc.getFrameTime();
            float battery = currentBatteryPct;
            float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) mc.getWindow().getScreenHeight();
            
            // Lógica de tema de color
            int theme = ModConfig.getNvgColorTheme();
            float rf = 0.05f, gf = 1.0f, bf = 0.05f; // Verde por defecto
            if (theme == 1) { rf = 0.8f; gf = 0.9f; bf = 1.0f; } // Fósforo blanco
            else if (theme == 2) { rf = 0.0f; gf = 0.8f; bf = 1.0f; } // Cian digital

            for (PostPass pass : passes) {
                var shader = pass.getEffect();
                if (shader.getUniform("battery") != null) {
                    float finalBatt = battery;
                    if (damageFlicker && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f; 
                    shader.getUniform("battery").set(finalBatt);
                }
                if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
                if (shader.getUniform("colorFilter") != null) shader.getUniform("colorFilter").set(rf, gf, bf);
                
                // Flashlight uniforms
                if (shader.getUniform("FlashlightActive") != null) shader.getUniform("FlashlightActive").set(flashlightActive ? 1.0f : 0.0f);
                if (shader.getUniform("AspectRatio") != null) shader.getUniform("AspectRatio").set(aspectRatio);
            }
        }
    }

    public static boolean isShaderActive() {
        return ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).vision_goggles$getPostEffect() != null;
    }
}
