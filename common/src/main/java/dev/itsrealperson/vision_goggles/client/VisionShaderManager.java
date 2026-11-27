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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class VisionShaderManager {
    private static VisionMode activeMode = null;
    private static float currentBatteryPct = 1.0f;
    private static boolean damageFlicker = false;
    private static float currentGlare = 0.0f;
    private static float currentInterference = 0.0f; // Smooth thermal biome noise

    /**
     * Our own references to compiled PostChains.
     *
     * KEY DESIGN:
     *   1. Chains are NEVER placed in GameRenderer.postEffect (zero third-party interference).
     *   2. Compiled chains stay resident in SHADER_CACHE and are NOT closed on mode switch or toggle-off,
     *      preventing re-reads from modified mod JARs (ZipException) and eliminating reload hitches.
     *   3. Failed shader loads use a 5-second retry cooldown to prevent 20 Hz log/compile spam.
     */
    private static final Map<ResourceLocation, PostChain> SHADER_CACHE = new HashMap<>();
    private static final Map<ResourceLocation, Long> FAILED_SHADER_TIMESTAMPS = new HashMap<>();
    private static final long RETRY_COOLDOWN_MS = 5000L;
    private static final ResourceLocation FLASHLIGHT_SHADER_LOC =
            new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/flashlight.json");
    private static final java.util.function.IntSupplier THERMAL_MASK_SUPPLIER = ThermalEntityPainter::getMaskTextureId;

    private static PostChain savedPostChain = null;
    private static ResourceLocation activeShaderLocation = null;

    private static PostChain savedFlashlightChain = null;
    private static PostChain cachedFlashlightChain = null;
    private static boolean prewarmed = false;
    private static int lastWindowWidth = -1;
    private static int lastWindowHeight = -1;

    private static int currentFlashlightMode = 0;
    private static long startupTimestamp = 0L;
    private static float flashGlare = 0.0f;

    private static long lastGunFlash = 0L;
    private static long underwaterStartMs = 0L;
    private static VisionMode lastUnderwaterMode = null;

    public static float getUnderwaterShortProgress(Minecraft mc) {
        if (mc == null || mc.player == null || !VisionRenderer.isVisorActive()
                || (activeMode != VisionMode.NIGHT_VISION && activeMode != VisionMode.THERMAL)
                || !mc.player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) {
            underwaterStartMs = 0L;
            lastUnderwaterMode = null;
            return 0.0f;
        }
        long now = System.currentTimeMillis();
        if (underwaterStartMs == 0L || lastUnderwaterMode != activeMode) {
            underwaterStartMs = now;
            lastUnderwaterMode = activeMode;
        }
        return Math.min(1.0f, Math.max(0.0f, (now - underwaterStartMs) / 2000.0f));
    }

    public static void gunFlash(double x, double y, double z, boolean checkHeld) {
        net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
        boolean opticsOrExternalNvg = VisionRenderer.isVisorActive()
                || dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI.getExternalVisionOverride() == VisionMode.NIGHT_VISION;
        if (mc.player == null || !opticsOrExternalNvg || !isNvgOrThermalCustomShaderActive() || isThermalShaderActive()) return;
        long now = System.currentTimeMillis();
        if (now - lastGunFlash < 40) return;
        double d = Math.sqrt(mc.player.distanceToSqr(x, y, z));
        if (d > 24.0) return;
        if (checkHeld && d <= 3.5) {
            var tag = mc.player.getMainHandItem().getTag();
            String ts = tag == null ? "" : tag.toString().toLowerCase();
            if (ts.contains("muzzle_silence") || ts.contains("suppress")) { lastGunFlash = now; triggerMuzzleFlash(0.04f); return; }
        }
        lastGunFlash = now;
        triggerMuzzleFlash(d <= 3.5 ? 0.18f : 0.18f * (float) (1.0 - d / 24.0));
    }

    public static void triggerMuzzleFlash(float intensity) {
        flashGlare = Math.min(0.36f, Math.max(intensity, flashGlare + intensity * 0.65f));
    }

    public static void triggerFlash(float intensity) {
        flashGlare = Math.min(1.35f, Math.max(intensity, flashGlare + intensity * 0.45f));
    }

    public static void update(VisionMode mode, float batteryPct, boolean flicker) {
        activeMode = mode;
        currentBatteryPct = batteryPct;
        damageFlicker = flicker;
        // Uniforms are updated inside processShader() every render frame.
    }

    public static VisionMode getActiveMode() {
        return activeMode;
    }

    public static void updateFlashlightState(float batteryPct, boolean flicker) {
        currentBatteryPct = batteryPct;
        damageFlicker = flicker;
    }

    /**
     * Pre-compiles built-in goggle and flashlight PostChains while resources are freshly loaded,
     * so subsequent mode switches are instantaneous and immune to JAR file replacement.
     */
    public static void prewarmShaders(Minecraft mc) {
        if (prewarmed || mc == null || mc.getResourceManager() == null || mc.getMainRenderTarget() == null) {
            return;
        }
        prewarmed = true;
        getOrCreateChain(mc, VisionMode.NIGHT_VISION.getShaderLocation(), false);
        getOrCreateChain(mc, VisionMode.THERMAL.getShaderLocation(), false);
        getOrCreateChain(mc, VisionMode.BIOMETRIC.getShaderLocation(), false);
        getOrCreateChain(mc, VisionMode.HYDRO.getShaderLocation(), false);
        getOrCreateChain(mc, new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/hydro_dry.json"), false);
        getOrCreateFlashlightChain(mc);
    }

    private static PostChain getOrCreateChain(Minecraft mc, ResourceLocation loc, boolean isFlashlight) {
        if (mc == null || loc == null) return null;
        int w = Math.max(1, mc.getWindow().getWidth());
        int h = Math.max(1, mc.getWindow().getHeight());

        PostChain existing = isFlashlight ? cachedFlashlightChain : SHADER_CACHE.get(loc);
        if (existing != null) {
            if (w != lastWindowWidth || h != lastWindowHeight) {
                try {
                    existing.resize(w, h);
                    lastWindowWidth = w;
                    lastWindowHeight = h;
                } catch (Exception e) {
                    Vision_goggles.LOGGER.warn("[Vision Goggles] Cached PostChain resize failed for {}, evicting: {}", loc, e.getMessage());
                    try { existing.close(); } catch (Exception ignored) {}
                    if (isFlashlight) {
                        cachedFlashlightChain = null;
                    } else {
                        SHADER_CACHE.remove(loc);
                    }
                    existing = null;
                }
            }
            if (existing != null) {
                return existing;
            }
        }

        long now = System.currentTimeMillis();
        Long lastFail = FAILED_SHADER_TIMESTAMPS.get(loc);
        if (lastFail != null && (now - lastFail) < RETRY_COOLDOWN_MS) {
            return null;
        }

        try {
            PostChain chain = new PostChain(mc.getTextureManager(), mc.getResourceManager(), mc.getMainRenderTarget(), loc);
            chain.resize(w, h);
            lastWindowWidth = w;
            lastWindowHeight = h;
            if (isFlashlight) {
                cachedFlashlightChain = chain;
            } else {
                SHADER_CACHE.put(loc, chain);
            }
            FAILED_SHADER_TIMESTAMPS.remove(loc);
            return chain;
        } catch (Exception e) {
            FAILED_SHADER_TIMESTAMPS.put(loc, now);
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to load shader: " + loc + " (will retry in 5s)", e);
            return null;
        }
    }

    private static PostChain getOrCreateFlashlightChain(Minecraft mc) {
        return getOrCreateChain(mc, FLASHLIGHT_SHADER_LOC, true);
    }

    public static void enableShader(Minecraft mc, VisionMode mode) {
        if (mode == null || mc == null || mc.player == null) {
            return;
        }
        ResourceLocation loc = mode.getShaderLocation();
        if (mode == VisionMode.HYDRO && !mc.player.isUnderWater()) {
            loc = new ResourceLocation(Vision_goggles.MOD_ID, "shaders/post/hydro_dry.json");
        }
        activateVisionChain(mc, loc);
    }

    public static void enableCustomShader(Minecraft mc, ResourceLocation loc) {
        if (loc == null || mc == null || mc.player == null) return;
        activateVisionChain(mc, loc);
    }

    private static void activateVisionChain(Minecraft mc, ResourceLocation loc) {
        if (savedPostChain != null && loc.equals(activeShaderLocation)) {
            return;
        }
        IrisCompat.warnIfShaderPackActiveOnce(mc.player);
        PostChain chain = getOrCreateChain(mc, loc, false);
        if (chain != null) {
            boolean wasInactive = (savedPostChain == null);
            savedPostChain = chain;
            activeShaderLocation = loc;
            if (wasInactive || startupTimestamp == 0L) {
                startupTimestamp = System.currentTimeMillis();
            }
        } else {
            savedPostChain = null;
            activeShaderLocation = null;
        }
    }

    public static void disableShader(Minecraft mc) {
        // Keep compiled PostChain instances alive in SHADER_CACHE; only deactivate active pointers.
        savedPostChain = null;
        activeShaderLocation = null;
        activeMode = null;
        ThermalEntityPainter.invalidateFrameMask();
        currentGlare = 0.0f;
        flashGlare = 0.0f;
        currentInterference = 0.0f;
        startupTimestamp = 0L;
        underwaterStartMs = 0L;
        lastUnderwaterMode = null;
    }

    public static void enableFlashlight(Minecraft mc, int modeId) {
        currentFlashlightMode = modeId;
        PostChain chain = getOrCreateFlashlightChain(mc);
        savedFlashlightChain = chain;
    }

    public static void disableFlashlight(Minecraft mc) {
        // Keep cachedFlashlightChain alive to avoid recompiling on toggle.
        savedFlashlightChain = null;
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
            updateUniforms(mc, partialTick);
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            savedPostChain.process(partialTick);
        } catch (Exception e) {
            // Chain became invalid (resize race, resource reload, etc.) — evict & apply cooldown
            Vision_goggles.LOGGER.warn("[Vision Goggles] processShader failed, evicting shader {}: {}", activeShaderLocation, e.getMessage());
            try { savedPostChain.close(); } catch (Exception ignored) {}
            if (activeShaderLocation != null) {
                SHADER_CACHE.remove(activeShaderLocation);
                FAILED_SHADER_TIMESTAMPS.put(activeShaderLocation, System.currentTimeMillis());
            }
            savedPostChain = null;
            activeShaderLocation = null;
        } finally {
            RenderSystem.disableBlend();
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    /**
     * Called when GameRenderer.resize() fires so all cached PostChain framebuffers
     * stay in sync with the window dimensions.
     */
    public static void onResize(int width, int height) {
        if (width <= 0 || height <= 0) return;
        lastWindowWidth = width;
        lastWindowHeight = height;

        Iterator<Map.Entry<ResourceLocation, PostChain>> it = SHADER_CACHE.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<ResourceLocation, PostChain> entry = it.next();
            PostChain chain = entry.getValue();
            try {
                chain.resize(width, height);
            } catch (Exception e) {
                Vision_goggles.LOGGER.warn("[Vision Goggles] Cached PostChain resize failed for {}, evicting: {}", entry.getKey(), e.getMessage());
                try { chain.close(); } catch (Exception ignored) {}
                it.remove();
                if (chain == savedPostChain) {
                    savedPostChain = null;
                    activeShaderLocation = null;
                }
            }
        }

        if (cachedFlashlightChain != null) {
            try {
                cachedFlashlightChain.resize(width, height);
            } catch (Exception e) {
                Vision_goggles.LOGGER.warn("[Vision Goggles] FlashlightChain resize failed, evicting: {}", e.getMessage());
                try { cachedFlashlightChain.close(); } catch (Exception ignored) {}
                cachedFlashlightChain = null;
                savedFlashlightChain = null;
            }
        }
    }

    public static void processFlashlight(float partialTick) {
        if (isThermalShaderActive()) return;
        if (savedFlashlightChain == null) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) return;
        if (mc.options.getCameraType() != null && !mc.options.getCameraType().isFirstPerson()) return;

        try {
            float time = ((mc.level.getGameTime() % 100000L) + partialTick) / 20.0f;
            int screenHeight = Math.max(1, mc.getWindow().getScreenHeight());
            float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) screenHeight;
            boolean irisActive = IrisCompat.isShaderPackInUse();
            float beamDist = 16.0f;
            for (int i = 0; i < dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.currentCount; i++) {
                if (dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.isLocalArray[i] > 0.5f) {
                    beamDist = Math.max(0.5f, dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms.range0Array[i]);
                    break;
                }
            }

            List<PostPass> passes = ((PostChainAccessor) savedFlashlightChain).vision_goggles$getPasses();
            float strobeFactor = (currentFlashlightMode == dev.itsrealperson.vision_goggles.util.FlashlightMode.STROBE.id)
                    ? dev.itsrealperson.vision_goggles.util.FlashlightMode.getStrobeFactor(System.nanoTime())
                    : 1.0f;
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
                if (shader.getUniform("IrisActive") != null) shader.getUniform("IrisActive").set(irisActive ? 1.0f : 0.0f);
                if (shader.getUniform("BeamDistance") != null) shader.getUniform("BeamDistance").set(beamDist);
                if (shader.getUniform("StrobeFactor") != null) shader.getUniform("StrobeFactor").set(strobeFactor);
            }

            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            savedFlashlightChain.process(partialTick);
        } catch (Exception e) {
            Vision_goggles.LOGGER.warn("[Vision Goggles] processFlashlight failed, evicting: {}", e.getMessage());
            try { savedFlashlightChain.close(); } catch (Exception ignored) {}
            cachedFlashlightChain = null;
            savedFlashlightChain = null;
            FAILED_SHADER_TIMESTAMPS.put(FLASHLIGHT_SHADER_LOC, System.currentTimeMillis());
        } finally {
            RenderSystem.disableBlend();
            mc.getMainRenderTarget().bindWrite(false);
        }
    }

    /** True when our PostChain is compiled and ready to process. */
    public static boolean isShaderActive() {
        return savedPostChain != null;
    }

    public static ResourceLocation getActiveShaderLocation() {
        return activeShaderLocation;
    }

    public static boolean isNvgOrThermalCustomShaderActive() {
        if (activeShaderLocation == null) return false;
        String path = activeShaderLocation.getPath().toLowerCase();
        return path.contains("nvg") || path.contains("thermal");
    }

    public static boolean isThermalShaderActive() { if (savedPostChain == null) return false;
        return activeMode == VisionMode.THERMAL
                || (activeMode == null && activeShaderLocation != null
                    && activeShaderLocation.getPath().toLowerCase(java.util.Locale.ROOT).contains("thermal"));
    }

    public static void clearCache() {
        for (PostChain chain : SHADER_CACHE.values()) {
            try { chain.close(); } catch (Exception ignored) {}
        }
        SHADER_CACHE.clear();
        if (cachedFlashlightChain != null) {
            try { cachedFlashlightChain.close(); } catch (Exception ignored) {}
            cachedFlashlightChain = null;
        }
        savedPostChain = null;
        savedFlashlightChain = null;
        activeShaderLocation = null;
        activeMode = null;
        ThermalEntityPainter.invalidateFrameMask();
        FAILED_SHADER_TIMESTAMPS.clear();
        prewarmed = false;
        lastWindowWidth = -1;
        lastWindowHeight = -1;
    }

    private static List<PostPass> currentUniformPasses = null;
    private static org.luaj.vm2.LuaTable cachedUniformBridge = null;

    private static org.luaj.vm2.LuaTable getUniformBridge(List<PostPass> passes) {
        currentUniformPasses = passes;
        if (cachedUniformBridge == null) {
            org.luaj.vm2.LuaTable uniforms = new org.luaj.vm2.LuaTable();
            uniforms.set("setFloat", new org.luaj.vm2.lib.VarArgFunction() {
                @Override
                public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                    if (currentUniformPasses == null) return org.luaj.vm2.LuaValue.NIL;
                    String name = args.checkjstring(1);
                    float val = (float) args.checkdouble(2);
                    for (PostPass pass : currentUniformPasses) {
                        var u = pass.getEffect().getUniform(name);
                        if (u != null) u.set(val);
                    }
                    return org.luaj.vm2.LuaValue.NIL;
                }
            });
            uniforms.set("setVec2", new org.luaj.vm2.lib.VarArgFunction() {
                @Override
                public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                    if (currentUniformPasses == null) return org.luaj.vm2.LuaValue.NIL;
                    String name = args.checkjstring(1);
                    float x = (float) args.checkdouble(2);
                    float y = (float) args.checkdouble(3);
                    for (PostPass pass : currentUniformPasses) {
                        var u = pass.getEffect().getUniform(name);
                        if (u != null) u.set(x, y);
                    }
                    return org.luaj.vm2.LuaValue.NIL;
                }
            });
            uniforms.set("setVec3", new org.luaj.vm2.lib.VarArgFunction() {
                @Override
                public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                    if (currentUniformPasses == null) return org.luaj.vm2.LuaValue.NIL;
                    String name = args.checkjstring(1);
                    float r = (float) args.checkdouble(2);
                    float g = (float) args.checkdouble(3);
                    float b = (float) args.checkdouble(4);
                    for (PostPass pass : currentUniformPasses) {
                        var u = pass.getEffect().getUniform(name);
                        if (u != null) u.set(r, g, b);
                    }
                    return org.luaj.vm2.LuaValue.NIL;
                }
            });
            uniforms.set("setVec4", new org.luaj.vm2.lib.VarArgFunction() {
                @Override
                public org.luaj.vm2.Varargs invoke(org.luaj.vm2.Varargs args) {
                    if (currentUniformPasses == null) return org.luaj.vm2.LuaValue.NIL;
                    String name = args.checkjstring(1);
                    float r = (float) args.checkdouble(2);
                    float g = (float) args.checkdouble(3);
                    float b = (float) args.checkdouble(4);
                    float a = (float) args.checkdouble(5);
                    for (PostPass pass : currentUniformPasses) {
                        var u = pass.getEffect().getUniform(name);
                        if (u != null) u.set(r, g, b, a);
                    }
                    return org.luaj.vm2.LuaValue.NIL;
                }
            });
            cachedUniformBridge = uniforms;
        }
        return cachedUniformBridge;
    }

    private static void updateUniforms(Minecraft mc, float partialTick) {
        // Use our own saved reference, never GameRenderer.postEffect
        PostChain effect = savedPostChain;
        if (effect == null) return;

        List<PostPass> passes = ((PostChainAccessor) effect).vision_goggles$getPasses();
        float time = ((mc.level.getGameTime() % 100000L) + partialTick) / 20.0f;
        float battery = currentBatteryPct;
        int screenHeight = Math.max(1, mc.getWindow().getScreenHeight());
        float aspectRatio = (float) mc.getWindow().getScreenWidth() / (float) screenHeight;

        int theme = ModConfig.getNvgColorTheme();
        float rf = 0.05f, gf = 1.0f, bf = 0.05f;
        if (theme == 1) { rf = 0.8f; gf = 0.9f; bf = 1.0f; }
        else if (theme == 2) { rf = 0.0f; gf = 0.8f; bf = 1.0f; }

        boolean isThermalShader = activeMode == VisionMode.THERMAL
                || (activeMode == null && activeShaderLocation != null && activeShaderLocation.getPath().toLowerCase().contains("thermal"));
        boolean isNvgShader = activeMode == VisionMode.NIGHT_VISION
                || (activeMode == null && activeShaderLocation != null && activeShaderLocation.getPath().toLowerCase().contains("nvg"));

        float targetInterference = 0.0f;
        if (isThermalShader) {
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
        float strobeGlarePulse = 0.0f;
        if (isNvgShader && mc.player != null) {
            net.minecraft.core.BlockPos pos = mc.player.blockPosition().above();
            
            // 1. Ceguera solar basada en la hora del día y vector de mirada hacia el sol:
            long dayTime = Math.abs(mc.level.getDayTime()) % 24000L;
            boolean underOpenSky = mc.level.canSeeSky(pos);
            
            float sunGlare = 0.0f;
            if (mc.level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                // De noche (12500 a 23000 ticks, incluyendo 18000 medianoche): NADA de ceguera solar!
                if (dayTime < 12500L || dayTime > 23000L) {
                    // 1. Deslumbramiento ambiental difuso al aire libre (luminancia general de cielo diurno)
                    if (underOpenSky) {
                        if (dayTime >= 1500L && dayTime <= 10500L) {
                            sunGlare = 0.30f;
                        } else if (dayTime < 1500L) {
                            long dawnTicks = (dayTime > 23000L) ? (dayTime - 23000L) : (dayTime + 1000L);
                            float t = (float) dawnTicks / 2500.0f;
                            t = Math.max(0.0f, Math.min(1.0f, t));
                            sunGlare = t * t * (3f - 2f * t) * 0.30f;
                        } else {
                            float t = 1.0f - ((float) (dayTime - 10500L) / 2000.0f);
                            t = Math.max(0.0f, Math.min(1.0f, t));
                            sunGlare = t * t * (3f - 2f * t) * 0.30f;
                        }
                    }

                    // 2. Fogonazo directo por mirar al sol (CON OCLUSIÓN FÍSICA POR RAYCAST)
                    float sunAngle = mc.level.getSunAngle(1.0f);
                    net.minecraft.world.phys.Vec3 sunDir = new net.minecraft.world.phys.Vec3(-Math.sin(sunAngle), Math.cos(sunAngle), 0.0).normalize();
                    double sunDot = mc.player.getViewVector(1.0f).dot(sunDir);
                    
                    // Solo si la mirada apunta al sol y el sol está sobre el horizonte
                    if (sunDot > 0.65 && sunDir.y > 0.0) {
                        net.minecraft.world.phys.Vec3 eyePos = mc.player.getEyePosition(1.0f);
                        net.minecraft.world.phys.Vec3 sunRayEnd = eyePos.add(sunDir.scale(128.0));
                        net.minecraft.world.phys.HitResult hit = mc.level.clip(new net.minecraft.world.level.ClipContext(
                            eyePos, sunRayEnd, net.minecraft.world.level.ClipContext.Block.VISUAL, net.minecraft.world.level.ClipContext.Fluid.NONE, mc.player
                        ));
                        
                        // Solo hay fogonazo si la línea de visión al sol está 100% despejada (sin bloques intermedios)
                        if (hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS) {
                            float directFactor = (float) ((sunDot - 0.65) / 0.35);
                            sunGlare = Math.max(sunGlare, 0.70f + directFactor * 1.30f);
                        }
                    }
                    
                    // Tormenta o lluvia reducen la ceguera por cobertura nubosa
                    if (mc.level.isThundering()) {
                        sunGlare *= 0.25f;
                    } else if (mc.level.isRaining()) {
                        sunGlare *= 0.50f;
                    }
                }
            }
            
            // 2. Luz de bloques e incidencia directa de fuentes incandescentes a quemarropa:
            int blockLight = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, pos);
            float blockGlare = 0.0f;
            if (blockLight > 12) {
                blockGlare = (float) (blockLight - 12) / 3.0f * 0.4f;
            }
            if (mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult bHit && bHit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK) {
                net.minecraft.world.level.block.state.BlockState hitState = mc.level.getBlockState(bHit.getBlockPos());
                if (hitState.is(net.minecraft.world.level.block.Blocks.LAVA) || hitState.is(net.minecraft.world.level.block.Blocks.FIRE) ||
                    hitState.is(net.minecraft.world.level.block.Blocks.SOUL_FIRE) || hitState.is(net.minecraft.world.level.block.Blocks.CAMPFIRE) ||
                    hitState.is(net.minecraft.world.level.block.Blocks.SOUL_CAMPFIRE)) {
                    double distSq = mc.player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(bHit.getBlockPos()));
                    if (distSq < 16.0) {
                        float prox = 1.0f - (float)(Math.sqrt(distSq) / 4.0);
                        blockGlare = Math.max(blockGlare, prox * 0.95f);
                    }
                }
            }
            
            targetGlare = Math.max(sunGlare, blockGlare);

            if (flashGlare > 0.001f) {
                targetGlare = Math.max(targetGlare, flashGlare);
                flashGlare *= (flashGlare <= 0.40f) ? 0.76f : 0.88f;
            }



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

                if (!hasModFlashlight) {
                    net.minecraft.world.item.ItemStack otherTactical =
                            dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getHeldActiveFlashlight(other);
                    if (!otherTactical.isEmpty()) {
                        hasModFlashlight = true;
                        int modeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(otherTactical);
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
                    if (hasModFlashlight && flashMode == dev.itsrealperson.vision_goggles.util.FlashlightMode.STROBE) {
                        float residual = glareFromPlayer * 0.06f;
                        if (residual > targetGlare) targetGlare = residual;
                        float pulse = glareFromPlayer * dev.itsrealperson.vision_goggles.util.FlashlightMode.getStrobeFactor(System.nanoTime());
                        if (pulse > strobeGlarePulse) strobeGlarePulse = pulse;
                    } else if (glareFromPlayer > targetGlare) {
                        targetGlare = glareFromPlayer;
                    }
                }
            }

            if (isHoldingLight(mc.player.getMainHandItem()) || isHoldingLight(mc.player.getOffhandItem())) {
                float selfGlare = 0.15f * ModConfig.getNvgGenericLightIntensity();
                if (targetGlare < selfGlare) targetGlare = selfGlare;
            }
            
            net.minecraft.world.item.ItemStack myHelmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
            if (!myHelmet.isEmpty() && myHelmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem myModular) {
                if (myModular.getUtilityModules(myHelmet).contains(dev.itsrealperson.vision_goggles.util.ModuleType.FLASHLIGHT)) {
                    var myTag = myHelmet.getOrCreateTag();
                    float helmetBatt = myTag.contains(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_BATTERY)
                            ? myTag.getFloat(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_BATTERY)
                            : (float) myModular.getBatteryCapacity(myHelmet);
                    if (myTag.getBoolean(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_ACTIVE) && helmetBatt > 0.0f) {
                        int myModeId = myTag.getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_FLASHLIGHT_MODE);
                        dev.itsrealperson.vision_goggles.util.FlashlightMode myFlashMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(myModeId);
                        if (myFlashMode == dev.itsrealperson.vision_goggles.util.FlashlightMode.STROBE) {
                            float residual = 0.04f;
                            if (targetGlare < residual) targetGlare = residual;
                            float pulse = 0.38f * dev.itsrealperson.vision_goggles.util.FlashlightMode.getStrobeFactor(System.nanoTime());
                            if (strobeGlarePulse < pulse) strobeGlarePulse = pulse;
                        } else {
                            float flashIntensity = Math.max(1.25f, myFlashMode.glareIntensity);
                            if (targetGlare < flashIntensity) targetGlare = flashIntensity;
                        }

                    }
                }
            }

            // Active Tactical Flashlight worn on torso (Curios/Accessories) or held in either hand blinds active NVG tubes
            net.minecraft.world.item.ItemStack myTacticalFlashlight =
                    dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getHeldActiveFlashlight(mc.player);
            if (!myTacticalFlashlight.isEmpty()) {
                int tacticalModeId = dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.getMode(myTacticalFlashlight);
                dev.itsrealperson.vision_goggles.util.FlashlightMode tacticalMode = dev.itsrealperson.vision_goggles.util.FlashlightMode.byId(tacticalModeId);
                if (tacticalMode == dev.itsrealperson.vision_goggles.util.FlashlightMode.STROBE) {
                    float residual = 0.04f;
                    if (targetGlare < residual) targetGlare = residual;
                    float pulse = 0.38f * dev.itsrealperson.vision_goggles.util.FlashlightMode.getStrobeFactor(System.nanoTime());
                    if (strobeGlarePulse < pulse) strobeGlarePulse = pulse;
                } else {
                    float tacticalFlashGlare = Math.max(1.25f, tacticalMode.glareIntensity);
                    if (targetGlare < tacticalFlashGlare) targetGlare = tacticalFlashGlare;
                }

            }
        }

        // Fast rise (sensor overload), slow recovery (phosphor burn-in P43 tube simulation)
        float glareSpeed;
        if (targetGlare > currentGlare) {
            glareSpeed = 0.12f; // Fast sensory shock
        } else {
            glareSpeed = (currentGlare > 0.35f) ? 0.010f : 0.022f; // Realistic phosphor persistence
        }
        currentGlare += (targetGlare - currentGlare) * glareSpeed;
        if (Math.abs(currentGlare) < 0.001f) currentGlare = 0.0f;

        float zoomFactor = dev.itsrealperson.vision_goggles.client.VisionRenderer.getCurrentZoomFactor();
        float focus = Math.max(0.0f, Math.min(1.0f, (zoomFactor - 1.0f) / 7.0f));

        // Power-up sequence: Smooth optical tube bloom without jarring black strobe gaps
        float startupMultiplier = 1.0f;
        if (startupTimestamp > 0L) {
            long elapsed = System.currentTimeMillis() - startupTimestamp;
            if (elapsed < 240L) {
                float t = Math.max(0.0f, Math.min(1.0f, (float) elapsed / 240.0f));
                float decay = (1.0f - t) * (1.0f - t);
                if (activeMode == VisionMode.NIGHT_VISION) {
                    currentGlare = Math.max(currentGlare, 0.28f * decay);
                }
            }
        }

        float waterShort = getUnderwaterShortProgress(mc);
        boolean waterBlackout = false;
        float waterGlareSpike = 0.0f;
        if (waterShort > 0.0f) {
            // Erratic high-frequency electronic short-circuit pulses ramping up over 2.0s
            double tSec = System.nanoTime() * 1e-9;
            float n1 = (float) Math.abs(Math.sin(tSec * 47.0 + Math.cos(tSec * 19.0) * 3.1));
            float n2 = (float) Math.abs(Math.cos(tSec * 73.0 + Math.sin(tSec * 29.0) * 2.3));
            float blackoutChance = 0.14f + waterShort * 0.52f;
            waterBlackout = (n1 < blackoutChance);
            if (n2 > (0.82f - waterShort * 0.35f)) {
                waterGlareSpike = (0.18f + 0.32f * waterShort) * n2;
            }
            if (isThermalShader) {
                interference = Math.max(interference, 0.20f + 0.65f * waterShort);
            }
        }

        float scopeRadius = dev.itsrealperson.vision_goggles.api.VisionGogglesClientAPI.getExternalVisionScopeRadius();
        boolean dspActive = (scopeRadius <= 0.0f) && isDspAvailable();
        float gogglesOuterMode = 0.0f;
        float scopeOuterDim = 1.0f;
        if (scopeRadius > 0.0f) {
            if (VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive()) {
                VisionMode gMode = VisionRenderer.getCurrentVisionMode();
                gogglesOuterMode = (gMode == VisionMode.THERMAL) ? 2.0f : 1.0f;
            } else if (mc.player != null && mc.level != null) {
                net.minecraft.core.BlockPos eyePos = net.minecraft.core.BlockPos.containing(mc.player.getEyePosition());
                int skyDarken = mc.level.getSkyDarken();
                int rawSky = mc.level.getBrightness(net.minecraft.world.level.LightLayer.SKY, eyePos);
                int rawBlock = mc.level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, eyePos);
                if (mc.level.isNight() || skyDarken >= 4) {
                    // At night / under True Darkness, ignore residual sky light so the outer periphery stays genuinely dark
                    float blockAmb = Math.max(0.0f, Math.min(1.0f, rawBlock / 15.0f));
                    scopeOuterDim = 0.012f + 0.22f * (blockAmb * blockAmb * blockAmb);
                } else {
                    float skyAmb = Math.max(0.0f, Math.min(1.0f, (rawSky - skyDarken) / 15.0f));
                    float blockAmb = Math.max(0.0f, Math.min(1.0f, rawBlock / 15.0f));
                    float ambient = Math.max(skyAmb, blockAmb);
                    scopeOuterDim = 0.012f + 0.988f * (ambient * ambient);
                }
            }
        }

        for (PostPass pass : passes) {
            var shader = pass.getEffect();
            if (isThermalShader && shader.getName() != null
                    && shader.getName().toLowerCase(java.util.Locale.ROOT).contains("thermal")) {
                shader.setSampler("ThermalMask", THERMAL_MASK_SUPPLIER);
            }
            if (shader.getUniform("battery") != null) {
                float finalBatt = battery * startupMultiplier;
                if (damageFlicker && mc.level.random.nextFloat() > 0.5f) finalBatt = 0.0f;
                if (waterBlackout) finalBatt = Math.min(finalBatt, 0.04f);
                shader.getUniform("battery").set(finalBatt);
            }
            if (shader.getUniform("time") != null) shader.getUniform("time").set(time);
            if (shader.getUniform("colorFilter") != null) shader.getUniform("colorFilter").set(rf, gf, bf);
            if (shader.getUniform("Interference") != null) shader.getUniform("Interference").set(interference);
            if (shader.getUniform("Glare") != null) shader.getUniform("Glare").set(currentGlare + strobeGlarePulse + waterGlareSpike);
            if (shader.getUniform("focus") != null) shader.getUniform("focus").set(focus);
            if (shader.getUniform("AspectRatio") != null) shader.getUniform("AspectRatio").set(aspectRatio);
            if (shader.getUniform("ScopeRadius") != null) shader.getUniform("ScopeRadius").set(scopeRadius);
            if (shader.getUniform("ScopeOuterDim") != null) shader.getUniform("ScopeOuterDim").set(scopeOuterDim);
            if (shader.getUniform("GogglesOuterMode") != null) shader.getUniform("GogglesOuterMode").set(gogglesOuterMode);
            if (shader.getUniform("u_DSPActive") != null) shader.getUniform("u_DSPActive").set(dspActive ? 1.0f : 0.0f);
            if (shader.getUniform("IrisActive") != null) shader.getUniform("IrisActive").set(IrisCompat.isShaderPackInUse() ? 1.0f : 0.0f);
        }

        // Call Lua uniforms hook for active modules using cached LuaTable bridge
        for (dev.itsrealperson.vision_goggles.scripting.LuaCustomModule mod : dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.getLoadedModules()) {
            if (dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge.canModuleRun(mod)) {
                mod.callUpdateUniforms(getUniformBridge(passes));
            }
        }
        currentUniformPasses = null;
    }

    public static boolean isDspAvailable() {
        if (activeMode == VisionMode.BIOMETRIC) {
            return true;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc != null && mc.player != null) {
            net.minecraft.world.item.ItemStack helmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProModularGogglesItem) {
                if (activeMode == VisionMode.THERMAL || isThermalShaderActive()) {
                    return true;
                }
            }
        }
        return dev.architectury.platform.Platform.isModLoaded("computercraft");
    }

    private static boolean isHoldingLight(net.minecraft.world.item.ItemStack stack) {
        if (stack.isEmpty()) return false;
        if (stack.getItem() instanceof dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem) {
            return dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem.isActive(stack);
        }
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

