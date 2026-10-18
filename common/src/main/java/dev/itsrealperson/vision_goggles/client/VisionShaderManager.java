package dev.itsrealperson.vision_goggles.client;

import com.mojang.blaze3d.systems.RenderSystem;
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
    private static float currentInterference = 0.0f; // Smooth thermal biome noise

    /**
     * Our own reference to the compiled PostChain.
     *
     * KEY DESIGN: this chain is NEVER placed in GameRenderer.postEffect.
     * After compiling it via loadEffect(), we immediately remove it from
     * GameRenderer's control (setPostEffect(null)). This means:
     *   - Superb Warfare, acceleratedrendering, Embeddium, or ANY other mod
     *     cannot close, replace, or null our chain — they only see postEffect=null.
     *   - We call process() ourselves in MixinGameRenderer.render() TAIL.
     *   - Zero interference, zero flicker.
     */
    private static PostChain savedPostChain = null;
    private static ResourceLocation activeShaderLocation = null;

    private static PostChain savedFlashlightChain = null;
    private static int currentFlashlightMode = 0;
    private static long startupTimestamp = 0L;

    public static void update(VisionMode mode, float batteryPct, boolean flicker) {
        activeMode = mode;
        currentBatteryPct = batteryPct;
        damageFlicker = flicker;
        // Uniforms are updated inside processShader() every render frame.
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

        // Close previous chain if switching modes
        if (savedPostChain != null) {
            try { savedPostChain.close(); } catch (Exception ignored) {}
            savedPostChain = null;
        }

        try {
            // loadEffect() compiles the shader and sets GameRenderer.postEffect
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$loadEffect(loc);

            // Take exclusive ownership: remove from GameRenderer so no other mod can touch it
            savedPostChain = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$setPostEffect(null);

            activeShaderLocation = loc;
            startupTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to load shader: " + loc, e);
        }
    }

    public static void enableCustomShader(Minecraft mc, ResourceLocation loc) {
        if (loc == null || mc.player == null) return;
        if (savedPostChain != null) {
            try { savedPostChain.close(); } catch (Exception ignored) {}
            savedPostChain = null;
        }
        try {
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$loadEffect(loc);
            savedPostChain = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$setPostEffect(null);
            activeShaderLocation = loc;
            startupTimestamp = System.currentTimeMillis();
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to load custom shader: " + loc, e);
        }
    }

    public static void disableShader(Minecraft mc) {
        if (savedPostChain != null) {
            try { savedPostChain.close(); } catch (Exception ignored) {}
            savedPostChain = null;
        }
        activeShaderLocation = null;
        currentGlare = 0.0f;
        currentInterference = 0.0f;
        startupTimestamp = 0L;

        // Safety: also clear GameRenderer in case something else put a chain there
        try {
            PostChain effect = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
            if (effect != null) {
                effect.close();
                ((GameRendererAccessor) mc.gameRenderer).vision_goggles$setPostEffect(null);
            }
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to clear GameRenderer postEffect", e);
        }
    }

    public static void enableFlashlight(Minecraft mc, int modeId) {
        currentFlashlightMode = modeId;
        ResourceLocation loc = new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/flashlight.json");

        if (savedFlashlightChain != null) {
            try { savedFlashlightChain.close(); } catch (Exception ignored) {}
            savedFlashlightChain = null;
        }

        try {
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$loadEffect(loc);
            savedFlashlightChain = ((GameRendererAccessor) mc.gameRenderer).vision_goggles$getPostEffect();
            ((GameRendererAccessor) mc.gameRenderer).vision_goggles$setPostEffect(null);
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to load flashlight shader: " + loc, e);
        }
    }

    public static void disableFlashlight(Minecraft mc) {
        if (savedFlashlightChain != null) {
            try { savedFlashlightChain.close(); } catch (Exception ignored) {}
            savedFlashlightChain = null;
        }
    }

    /**
     * Called every render frame from MixinGameRenderer (TAIL of GameRenderer.render()).
     *
     * Since our PostChain is NOT in GameRenderer.postEffect, it is completely isolated
     * from any other mod's rendering pipeline. We update uniforms and process the chain
     * ourselves at the very end of the frame, guaranteeing the effect is always applied.
     */
    public static void processShader(float partialTick) {
        if (savedPostChain == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        try {
            updateUniforms(mc);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            savedPostChain.process(partialTick);
            RenderSystem.disableBlend();
            // Restore main render target binding after the chain's internal blit
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Exception e) {
            // Chain became invalid (resize race, resource reload, etc.)
            // Close so isShaderActive() returns false and VisionRenderer re-creates it next tick
            Vision_goggles.LOGGER.warn("[Vision Goggles] processShader failed, will reload: " + e.getMessage());
            try { savedPostChain.close(); } catch (Exception ignored) {}
            savedPostChain = null;
        }
    }

    /**
     * Called when GameRenderer.resize() fires so our chain's framebuffers
     * stay in sync with the window dimensions.
     */
    public static void onResize(int width, int height) {
        if (savedPostChain != null) {
            try {
                savedPostChain.resize(width, height);
            } catch (Exception e) {
                Vision_goggles.LOGGER.warn("[Vision Goggles] PostChain resize failed, will reload: " + e.getMessage());
                try { savedPostChain.close(); } catch (Exception ignored) {}
                savedPostChain = null;
            }
        }
        if (savedFlashlightChain != null) {
            try {
                savedFlashlightChain.resize(width, height);
            } catch (Exception e) {
                Vision_goggles.LOGGER.warn("[Vision Goggles] FlashlightChain resize failed, will reload: " + e.getMessage());
                try { savedFlashlightChain.close(); } catch (Exception ignored) {}
                savedFlashlightChain = null;
            }
        }
    }

    public static void processFlashlight(float partialTick) {
        if (savedFlashlightChain == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;

        try {
            float time = (float) mc.level.getGameTime() + mc.getFrameTime();
            float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) mc.getWindow().getScreenHeight();

            List<PostPass> passes = ((PostChainAccessor) savedFlashlightChain).vision_goggles$getPasses();
            for (PostPass pass : passes) {
                var shader = pass.getEffect();
                if (shader.getUniform("battery") != null) {
                    float finalBatt = currentBatteryPct;
                    if (damageFlicker && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f;
                    shader.getUniform("battery").set(finalBatt);
                }
                if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
                if (shader.getUniform("AspectRatio") != null) shader.getUniform("AspectRatio").set(aspectRatio);
                if (shader.getUniform("FlashlightMode") != null) shader.getUniform("FlashlightMode").set((float)currentFlashlightMode);
                if (shader.getUniform("u_DSPActive") != null) shader.getUniform("u_DSPActive").set(isDspAvailable() ? 1.0f : 0.0f);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            savedFlashlightChain.process(partialTick);
            RenderSystem.disableBlend();
            mc.getMainRenderTarget().bindWrite(false);
        } catch (Exception e) {
            Vision_goggles.LOGGER.warn("[Vision Goggles] processFlashlight failed, will reload: " + e.getMessage());
            try { savedFlashlightChain.close(); } catch (Exception ignored) {}
            savedFlashlightChain = null;
        }
    }

    /** True when our PostChain is compiled and ready to process. */
    public static boolean isShaderActive() {
        return savedPostChain != null;
    }

    private static void updateUniforms(Minecraft mc) {
        // Use our own saved reference, never GameRenderer.postEffect
        PostChain effect = savedPostChain;
        if (effect == null) return;

        List<PostPass> passes = ((PostChainAccessor) effect).vision_goggles$getPasses();
        float time = (float) mc.level.getGameTime() + mc.getFrameTime();
        float battery = currentBatteryPct;
        float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) mc.getWindow().getScreenHeight();

        int theme = ModConfig.getNvgColorTheme();
        float rf = 0.05f, gf = 1.0f, bf = 0.05f;
        if (theme == 1) { rf = 0.8f; gf = 0.9f; bf = 1.0f; }
        else if (theme == 2) { rf = 0.0f; gf = 0.8f; bf = 1.0f; }

        float targetInterference = 0.0f;
        if (activeMode == VisionMode.THERMAL) {
            if (mc.level.dimension() == net.minecraft.world.level.Level.NETHER) {
                // Nether: target máximo, pero entrada/salida es gradual (currentInterference hace la transición)
                targetInterference = 0.8f;
            } else if (mc.player != null) {
                net.minecraft.core.Holder<net.minecraft.world.level.biome.Biome> biome =
                    mc.level.getBiome(mc.player.blockPosition());
                float biomeTemp = biome.value().getBaseTemperature();

                // Gradiente real de temperatura:
                // temp <= 0.7 (bosque, océano, pradera)  → sin ruido térmico
                // temp  0.7-1.0 (sabana, jungla, badlands)→ ruido creciente suave
                // temp  1.0-1.5+ (desierto, mesa, biomas custom muy calientes) → ruido máximo 0.4
                // (el sol también modera: de noche el calor se irradia menos)
                if (biomeTemp > 0.7f) {
                    float heatFactor = Math.min(1.0f, (biomeTemp - 0.7f) / 0.8f); // 0 en temp=0.7, 1 en temp=1.5
                    // Suavizar la curva: valores medios tienen más "ganancia" que los extremos
                    heatFactor = heatFactor * heatFactor * (3f - 2f * heatFactor); // smoothstep
                    float maxHeat = 0.4f * heatFactor;

                    // Modular por luz solar: de noche hay menos radiancia ambiental (menos ruido)
                    float sun = 1.0f - (mc.level.getSkyDarken() / 11.0f);
                    sun = Math.max(0.1f, sun); // mínimo 10% aunque sea de noche (suelo retiene calor)
                    targetInterference = maxHeat * sun;
                }
            }
        }

        // Suavizar transición de interferencia:
        // Sube relativamente rápido al entrar en zona caliente, baja lento (el suelo retiene calor)
        float interferenceSpeed = (targetInterference > currentInterference) ? 0.04f : 0.012f;
        currentInterference += (targetInterference - currentInterference) * interferenceSpeed;
        if (Math.abs(currentInterference) < 0.001f) currentInterference = 0.0f;
        float interference = currentInterference;



        float targetGlare = 0.0f;
        if (activeMode == VisionMode.NIGHT_VISION && mc.player != null) {
            net.minecraft.core.BlockPos pos = mc.player.blockPosition().above();
            
            // 1. Ceguera solar basada estrictamente en la HORA DEL DIA (ticks) y si esta AFUERA SIN TECHO:
            long dayTime = Math.abs(mc.level.getDayTime()) % 24000L;
            boolean underOpenSky = mc.level.canSeeSky(pos);
            
            float sunGlare = 0.0f;
            if (underOpenSky && mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                // De noche (12500 a 23000 ticks, incluyendo 18000 medianoche): NADA de ceguera solar!
                if (dayTime < 12500L || dayTime > 23000L) {
                    // De dia al aire libre sin techo:
                    if (dayTime >= 1500L && dayTime <= 10500L) {
                        // Pleno dia (mediodia = 6000): ceguera maxima
                        sunGlare = 1.0f;
                    } else if (dayTime < 1500L) {
                        // Amanecer: sube suavemente de 0.0 a 1.0 (entre 23000 y 1500 ticks)
                        long dawnTicks = (dayTime > 23000L) ? (dayTime - 23000L) : (dayTime + 1000L);
                        float t = (float) dawnTicks / 2500.0f;
                        t = Math.max(0.0f, Math.min(1.0f, t));
                        sunGlare = t * t * (3f - 2f * t); // smoothstep
                    } else {
                        // Atardecer: baja suavemente de 1.0 a 0.0 (entre 10500 y 12500 ticks)
                        float t = 1.0f - ((float) (dayTime - 10500L) / 2000.0f);
                        t = Math.max(0.0f, Math.min(1.0f, t));
                        sunGlare = t * t * (3f - 2f * t); // smoothstep
                    }
                    
                    // Tormenta o lluvia reducen la ceguera por cobertura nubosa
                    if (mc.level.isThundering()) {
                        sunGlare *= 0.25f;
                    } else if (mc.level.isRaining()) {
                        sunGlare *= 0.50f;
                    }
                }
            }
            
            // 2. Luz de bloques (antorchas o lava pegadas a la cara):
            int blockLight = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
            float blockGlare = 0.0f;
            if (blockLight > 12) {
                blockGlare = (float) (blockLight - 12) / 3.0f * 0.4f;
            }
            
            targetGlare = Math.max(sunGlare, blockGlare);



            int maxRange = ModConfig.getNvgGlareMaxRange();
            double maxDistSq = (double) maxRange * maxRange;

            for (net.minecraft.world.entity.player.Player other : mc.level.players()) {
                if (other == mc.player) continue;
                double distSq = other.distanceToSqr(mc.player);
                if (distSq > maxDistSq) continue;

                net.minecraft.world.item.ItemStack otherHelmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(other);
                boolean hasModFlashlight = false;
                dev.itsrealperson.vision_goggles.util.FlashlightMode flashMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.FOCUSED;
                if (!otherHelmet.isEmpty() && otherHelmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem otherModular) {
                    java.util.List<dev.itsrealperson.vision_goggles.util.ModuleType> otherUtils = otherModular.getUtilityModules(otherHelmet);
                    boolean flashlightInstalled = otherUtils.contains(dev.itsrealperson.vision_goggles.util.ModuleType.FLASHLIGHT);
                    boolean flashlightOn = otherHelmet.getOrCreateTag().getBoolean(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_ACTIVE);
                    hasModFlashlight = flashlightInstalled && flashlightOn;
                    if (hasModFlashlight) {
                        int modeId = otherHelmet.getOrCreateTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_MODE);
                        flashMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(modeId);
                    }
                }

                net.minecraft.world.item.ItemStack main = other.getMainHandItem();
                net.minecraft.world.item.ItemStack off = other.getOffhandItem();
                boolean holdingLight = isHoldingLight(main) || isHoldingLight(off);
                if (!hasModFlashlight && !holdingLight) continue;

                net.minecraft.world.phys.Vec3 lookVec = other.getLookAngle();
                net.minecraft.world.phys.Vec3 toUs = mc.player.position().add(0, mc.player.getEyeHeight(), 0)
                        .subtract(other.position().add(0, other.getEyeHeight(), 0)).normalize();
                double dot = lookVec.dot(toUs);
                double threshold = hasModFlashlight ? flashMode.glareThreshold : 0.92;
                if (dot > threshold) {
                    net.minecraft.world.phys.Vec3 myLook = mc.player.getLookAngle();
                    net.minecraft.world.phys.Vec3 toOther = toUs.scale(-1.0);
                    double facingDot = myLook.dot(toOther);
                    float backFactor = ModConfig.getNvgGlareBackFactor();
                    float facingFactor = (float) Math.max(backFactor, (facingDot + 1.0) / 2.0);
                    float distanceFactor = 1.0f - (float) (Math.sqrt(distSq) / (float) maxRange);
                    float aimFactor = (float) ((dot - threshold) / (1.0 - threshold));
                    float intensity = hasModFlashlight ? flashMode.glareIntensity : ModConfig.getNvgGenericLightIntensity();
                    float glareFromPlayer = distanceFactor * aimFactor * intensity * facingFactor;
                    if (glareFromPlayer > targetGlare) targetGlare = glareFromPlayer;
                }
            }

            if (isHoldingLight(mc.player.getMainHandItem()) || isHoldingLight(mc.player.getOffhandItem())) {
                float selfGlare = 0.15f * ModConfig.getNvgGenericLightIntensity();
                if (targetGlare < selfGlare) targetGlare = selfGlare;
            }
            
            net.minecraft.world.item.ItemStack myHelmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
            if (!myHelmet.isEmpty() && myHelmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem myModular) {
                if (myModular.getUtilityModules(myHelmet).contains(dev.itsrealperson.vision_goggles.util.ModuleType.FLASHLIGHT)) {
                    if (myHelmet.getOrCreateTag().getBoolean(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_ACTIVE)) {
                        int myModeId = myHelmet.getOrCreateTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_MODE);
                        float flashIntensity = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(myModeId).glareIntensity;
                        if (targetGlare < flashIntensity) targetGlare = flashIntensity;
                    }
                }
            }
        }

        // Fast rise (sensor overload), slow recovery (eyes re-adapt gradually)
        float glareSpeed = (targetGlare > currentGlare) ? 0.08f : 0.018f;
        currentGlare += (targetGlare - currentGlare) * glareSpeed;
        if (Math.abs(currentGlare) < 0.001f) currentGlare = 0.0f;

        // Power-up sequence: Photonic capacitor spike, micro-strobe, and phosphor warm-up ramp
        float startupMultiplier = 1.0f;
        if (startupTimestamp > 0L) {
            long elapsed = System.currentTimeMillis() - startupTimestamp;
            if (elapsed < 650L) {
                if (elapsed < 75L) {
                    startupMultiplier = 1.85f; // Initial phosphor overcharge bloom
                    currentGlare = Math.max(currentGlare, 0.40f);
                } else if (elapsed < 165L) {
                    startupMultiplier = 0.0f; // Strobe gap / blackout
                } else if (elapsed < 230L) {
                    startupMultiplier = 1.35f; // Warmup flicker pulse 1
                } else if (elapsed < 280L) {
                    startupMultiplier = 0.08f; // Micro dip
                } else {
                    float t = (elapsed - 280L) / 370.0f;
                    startupMultiplier = 0.40f + 0.60f * t; // Smooth phosphor stabilization
                }
            }
        }

        for (PostPass pass : passes) {
            var shader = pass.getEffect();
            if (shader.getUniform("battery") != null) {
                float finalBatt = battery * startupMultiplier;
                if (damageFlicker && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f;
                shader.getUniform("battery").set(finalBatt);
            }
            if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
            if (shader.getUniform("colorFilter") != null) shader.getUniform("colorFilter").set(rf, gf, bf);
            if (shader.getUniform("Interference") != null) shader.getUniform("Interference").set(interference);
            if (shader.getUniform("Glare") != null) shader.getUniform("Glare").set(currentGlare);
            if (shader.getUniform("AspectRatio") != null) shader.getUniform("AspectRatio").set(aspectRatio);
            if (shader.getUniform("u_DSPActive") != null) shader.getUniform("u_DSPActive").set(isDspAvailable() ? 1.0f : 0.0f);
        }

        // Call Lua uniforms hook for active modules
        for (dev.itsrealperson.vision_goggles.scripting.LuaCustomModule mod : dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.getLoadedModules()) {
            if (mod.canRun(mc)) {
                mod.callUpdateUniforms(passes);
            }
        }
    }

    public static boolean isDspAvailable() {
        return dev.architectury.platform.Platform.isModLoaded("computercraft");
    }

    private static boolean isHoldingLight(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        String id = stack.getDescriptionId().toLowerCase();
        boolean isLight = id.contains("flashlight") || id.contains("linterna") || id.contains("torch")
                || id.contains("lantern") || id.contains("glow") || id.contains("lamp");
        if (isLight && (id.contains("off") || id.contains("unlit"))) isLight = false;
        if (isLight && stack.hasTag()) {
            String nbtStr = stack.getTag().toString().toLowerCase();
            if (nbtStr.contains("active:0") || nbtStr.contains("on:0") || nbtStr.contains("enabled:0")
                    || nbtStr.contains("is_on:0") || nbtStr.contains("flashlight_on:0") || nbtStr.contains("flashlight_on:false")) {
                isLight = false;
            }
        }
        return isLight;
    }
}

