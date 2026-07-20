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
    private static float currentGlare = 0.0f;

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
        
        if (mode != null) {
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

            float interference = 0.0f;
            if (activeMode == VisionMode.THERMAL) {
                if (mc.level.dimension() == net.minecraft.world.level.Level.NETHER) {
                    interference = 0.8f;
                } else if (mc.player != null) {
                    net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome = mc.level.getBiome(mc.player.blockPosition());
                    if (biome.value().getBaseTemperature() > 1.0f) {
                        // Suave transición térmica basada en el sol (falla de 5 AM a 6 PM)
                        float timeOfDay = mc.level.getTimeOfDay(1.0f);
                        float sun = (float) Math.cos(timeOfDay * Math.PI * 2.0);
                        sun = Math.max(0.0f, sun);
                        interference = 0.4f * sun;
                    }
                }
            }

            float targetGlare = 0.0f;
            if (activeMode == VisionMode.NIGHT_VISION && mc.player != null) {
                net.minecraft.core.BlockPos pos = mc.player.blockPosition().above();
                int blockLight = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
                
                int rawSkyLight = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, pos);
                float timeOfDay = mc.level.getTimeOfDay(1.0f);
                
                // timeOfDay va de 0.0 (mediodía) a 0.5 (medianoche) a 1.0 (mediodía).
                // Math.cos nos da una curva perfecta: 1.0 al mediodía, 0.0 al amanecer/atardecer, y negativo de noche.
                float sunBrightness = (float) Math.cos(timeOfDay * Math.PI * 2.0);
                sunBrightness = Math.max(0.0f, sunBrightness); // 0.0 en toda la noche
                
                float effectiveSkyLight = rawSkyLight * sunBrightness;
                
                float effectiveLight = Math.max((float)blockLight, effectiveSkyLight);
                if (effectiveLight > 11.0f) {
                    targetGlare = (effectiveLight - 11.0f) / 4.0f; // Solo ciega con luz 12+
                }
                
                // --- CUSTOM FLASHLIGHT/LIGHT ITEM DETECTION ---
                // Verifica si otro jugador nos está apuntando con una linterna o fuente de luz
                for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
                    if (other == mc.player) continue;
                    
                    double distSq = other.distanceToSqr(mc.player);
                    if (distSq > 625.0) continue; // max 25 blocks
                    
                    net.minecraft.world.item.ItemStack main = other.getMainHandItem();
                    net.minecraft.world.item.ItemStack off = other.getOffhandItem();
                    
                    boolean holdingLight = isHoldingLight(main) || isHoldingLight(off);
                                           
                    if (holdingLight) {
                        net.minecraft.world.phys.Vec3 lookVec = other.getLookAngle();
                        net.minecraft.world.phys.Vec3 toUs = mc.player.position().add(0, mc.player.getEyeHeight(), 0)
                                                             .subtract(other.position().add(0, other.getEyeHeight(), 0)).normalize();
                        
                        double dot = lookVec.dot(toUs);
                        if (dot > 0.92) { // Apuntando directo
                            float distanceFactor = 1.0f - (float)(Math.sqrt(distSq) / 25.0);
                            float aimFactor = (float)((dot - 0.92) / (1.0 - 0.92));
                            float glareFromPlayer = distanceFactor * aimFactor * 1.5f; 
                            
                            if (glareFromPlayer > targetGlare) {
                                targetGlare = glareFromPlayer;
                            }
                        }
                    }
                }
                
                // Si el jugador local tiene una linterna en la mano, también le afectará un poco
                if (isHoldingLight(mc.player.getMainHandItem()) || isHoldingLight(mc.player.getOffhandItem())) {
                    if (targetGlare < 0.6f) targetGlare = 0.6f; 
                }
            }
            
            // Interpolación suave para que el cegado sea progresivo
            currentGlare += (targetGlare - currentGlare) * 0.05f;
            if (Math.abs(currentGlare) < 0.001f) {
                currentGlare = 0.0f;
            }

            for (PostPass pass : passes) {
                var shader = pass.getEffect();
                if (shader.getUniform("battery") != null) {
                    float finalBatt = battery;
                    if (damageFlicker && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f; 
                    shader.getUniform("battery").set(finalBatt);
                }
                if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
                if (shader.getUniform("colorFilter") != null) shader.getUniform("colorFilter").set(rf, gf, bf);
                if (shader.getUniform("Interference") != null) shader.getUniform("Interference").set(interference);
                if (shader.getUniform("Glare") != null) shader.getUniform("Glare").set(currentGlare);
                
                // AspectRatio might still be needed by some shaders? 
                // We'll keep it for general use but FlashlightActive is definitely gone.
                if (shader.getUniform("AspectRatio") != null) shader.getUniform("AspectRatio").set(aspectRatio);
            }
        }
    }

    public static boolean isShaderActive() {
        return ((GameRendererAccessor) Minecraft.getInstance().gameRenderer).vision_goggles$getPostEffect() != null;
    }
    
    private static boolean isHoldingLight(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = stack.getDescriptionId().toLowerCase();
        boolean isLight = id.contains("flashlight") || id.contains("linterna") || id.contains("torch") || id.contains("lantern") || id.contains("glow") || id.contains("lamp");
        
        if (isLight && (id.contains("off") || id.contains("unlit"))) isLight = false;
        
        if (isLight && stack.hasTag()) {
            String nbtStr = stack.getTag().toString().toLowerCase();
            // Verifica etiquetas comunes de mods de linternas, incluyendo OmegaFlashlight ("flashlight_on:0")
            if (nbtStr.contains("active:0") || nbtStr.contains("on:0") || nbtStr.contains("enabled:0") || nbtStr.contains("is_on:0") || nbtStr.contains("flashlight_on:0") || nbtStr.contains("flashlight_on:false")) {
                isLight = false;
            }
        }
        return isLight;
    }
}
