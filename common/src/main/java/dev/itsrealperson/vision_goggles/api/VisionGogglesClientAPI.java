package dev.itsrealperson.vision_goggles.api;

import dev.itsrealperson.vision_goggles.client.ThermalEntityPainter;
import dev.itsrealperson.vision_goggles.client.VisionHUDOverlay;
import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import dev.itsrealperson.vision_goggles.client.hud.IHudModule;
import dev.itsrealperson.vision_goggles.client.lighting.FlashlightUniforms;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Client-only public API for Vision Goggles 2.0.0 addons (optical spectrum state, IR spotlight injection,
 * NVG glare triggers, thermal entity overrides/modifiers, and HUD layers).
 */
@Environment(EnvType.CLIENT)
public final class VisionGogglesClientAPI {

    private record ThermalColorOverride(Predicate<LivingEntity> matcher, ToIntFunction<LivingEntity> colorProvider) {}
    private record QueuedIrSpot(Vector3f worldPos, Vector3f direction, Vector3f rgbColor, float beamAngleRad, float range, float intensity) {}

    private static final List<BiFunction<LivingEntity, Float, Float>> THERMAL_ENTITY_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<ThermalColorOverride> THERMAL_COLOR_OVERRIDES = new CopyOnWriteArrayList<>();
    private static final List<Function<LivingEntity, Integer>> THERMAL_COLOR_FUNCTIONS = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Float>> FRAME_LIGHT_EMITTERS = new CopyOnWriteArrayList<>();
    private static final List<QueuedIrSpot> QUEUED_IR_SPOTS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<GuiGraphics, Float>> HUD_OVERLAY_LAYERS = new CopyOnWriteArrayList<>();
    private static volatile boolean insideFrameLightCallback = false;
    private static volatile VisionMode externalVisionOverride = null;
    private static volatile float externalVisionScopeRadius = 0.0f;

    private VisionGogglesClientAPI() {
    }

    /**
     * Sets a full-screen external optical vision mode override.
     */
    public static void setExternalVisionOverride(@org.jetbrains.annotations.Nullable VisionMode mode) {
        setExternalVisionOverride(mode, 0.0f);
    }

    /**
     * Sets an external optical vision mode override (e.g. from weapon-mounted NVG or FLIR Thermal scopes).
     * When {@code scopeRadius > 0.0f}, the shader clips strictly to a centered circular lens aperture
     * ({@code length(vec2((uv.x - 0.5) * AspectRatio, uv.y - 0.5)) <= scopeRadius}) and leaves the
     * surrounding view and world lightmap 100% untouched.
     * Pass {@code null} to clear the override and immediately restore normal or goggles vision.
     */
    public static void setExternalVisionOverride(@org.jetbrains.annotations.Nullable VisionMode mode, float scopeRadius) {
        externalVisionOverride = mode;
        externalVisionScopeRadius = (mode != null && scopeRadius > 0.0f) ? scopeRadius : 0.0f;
    }

    /** Returns the active external optical vision mode override, or {@code null} if none is set. */
    @org.jetbrains.annotations.Nullable
    public static VisionMode getExternalVisionOverride() {
        return externalVisionOverride;
    }

    /** Returns the circular scope mask radius in aspect-corrected Y-normalized units (0.0 = full screen). */
    public static float getExternalVisionScopeRadius() {
        return externalVisionOverride != null ? externalVisionScopeRadius : 0.0f;
    }

    /** Returns true if the local player's visor is currently down and an optical shader mode is active, or an external override is active. */
    public static boolean isVisionActive() {
        return externalVisionOverride != null || (VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive());
    }

    /** Returns true if the local player's visor or an external optic is currently active in Night Vision mode. */
    public static boolean isNightVisionActive() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext == VisionMode.NIGHT_VISION;
        }
        return VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive()
                && VisionRenderer.getCurrentVisionMode() == VisionMode.NIGHT_VISION;
    }

    /** Alias for {@link #isNightVisionActive()}. */
    public static boolean isLocalNightVisionActive() {
        return isNightVisionActive();
    }

    /** Returns true if the local player's visor or an external optic is currently active in Thermal mode. */
    public static boolean isThermalActive() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext == VisionMode.THERMAL;
        }
        return (VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive() && VisionRenderer.getCurrentVisionMode() == VisionMode.THERMAL)
                || (VisionRenderer.isVisorActive() && VisionShaderManager.isThermalShaderActive());
    }

    /** Alias for {@link #isThermalActive()}. */
    public static boolean isLocalThermalActive() {
        return isThermalActive();
    }

    /** Returns true if the local player's visor or an external optic is currently active in Biometric mode. */
    public static boolean isBiometricActive() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext == VisionMode.BIOMETRIC;
        }
        return VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive()
                && VisionRenderer.getCurrentVisionMode() == VisionMode.BIOMETRIC;
    }

    /** Returns true if the local player's visor or an external optic is currently active in Hydro mode. */
    public static boolean isHydroActive() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext == VisionMode.HYDRO;
        }
        return VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive()
                && VisionRenderer.getCurrentVisionMode() == VisionMode.HYDRO;
    }

    /** Returns the active {@link VisionMode} on the local client, or null if inactive. */
    public static VisionMode getActiveVisionMode() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext;
        }
        return VisionRenderer.isVisorActive() ? VisionRenderer.getCurrentVisionMode() : null;
    }

    /** Alias for {@link #getActiveVisionMode()}. */
    public static VisionMode getLocalActiveMode() {
        VisionMode ext = externalVisionOverride;
        if (ext != null) {
            return ext;
        }
        if (!VisionRenderer.isVisorActive()) return null;
        VisionMode mode = VisionShaderManager.getActiveMode();
        return mode != null ? mode : VisionRenderer.getCurrentVisionMode();
    }

    public static float getBatteryPercent() {
        return VisionRenderer.getCurrentBatteryPct();
    }

    public static boolean isZoomActive() {
        return VisionRenderer.isZoomActive();
    }

    public static float getZoomFactor() {
        return VisionRenderer.getCurrentZoomFactor();
    }

    /** Triggers an immediate NVG optical glare/flash pulse (e.g. from IR lasers, flares, or weapon flashes). */
    public static void triggerOpticalGlare(float intensity) {
        if (intensity > 0.0f) {
            VisionShaderManager.triggerFlash(intensity);
        }
    }

    /** Alias for {@link #triggerOpticalGlare(float)}. */
    public static void triggerNvgGlare(float intensity) {
        if (intensity > 0.0f) {
            VisionShaderManager.triggerMuzzleFlash(intensity);
        }
    }

    /**
     * Registers a per-frame callback invoked inside {@link VisionRenderer#updateFlashlightEveryFrame}
     * immediately after {@link FlashlightUniforms#beginUpdate}, receiving the frame {@code partialTick}.
     */
    public static void registerFrameLightEmitter(Consumer<Float> emitter) {
        if (emitter != null && !FRAME_LIGHT_EMITTERS.contains(emitter)) {
            FRAME_LIGHT_EMITTERS.add(emitter);
        }
    }

    /**
     * Emits a collimated infrared beam or IR floodlight into {@link FlashlightUniforms} when
     * {@link #isNightVisionActive()} is true. Can be called either inside a {@link #registerFrameLightEmitter}
     * callback (recommended for frame-exact interpolation) or during client tick/render hooks.
     */
    public static void emitInfraredSpot(Vector3f worldPos, Vector3f direction, Vector3f rgbColor, float beamAngleRad, float range, float intensity) {
        if (!isNightVisionActive() || worldPos == null || direction == null || rgbColor == null || range <= 0.0f || intensity <= 0.0f) {
            return;
        }
        if (!insideFrameLightCallback) {
            if (QUEUED_IR_SPOTS.size() < FlashlightUniforms.GLSL_MAX) {
                QUEUED_IR_SPOTS.add(new QueuedIrSpot(
                        new Vector3f(worldPos),
                        new Vector3f(direction),
                        new Vector3f(rgbColor),
                        beamAngleRad,
                        range,
                        intensity
                ));
            }
            return;
        }
        submitInfraredSpotImmediate(worldPos, direction, rgbColor, beamAngleRad, range, intensity);
    }

    private static void submitInfraredSpotImmediate(Vector3f worldPos, Vector3f direction, Vector3f rgbColor, float beamAngleRad, float range, float intensity) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.gameRenderer == null || mc.gameRenderer.getMainCamera() == null) {
            return;
        }
        Vec3 camPos = mc.gameRenderer.getMainCamera().getPosition();
        Vector3f relPos = new Vector3f(
                worldPos.x() - (float) camPos.x,
                worldPos.y() - (float) camPos.y,
                worldPos.z() - (float) camPos.z
        );
        Vector3f dir = new Vector3f(direction);
        if (dir.lengthSquared() > 1.0e-6f) {
            dir.normalize();
        } else {
            dir.set(0.0f, 0.0f, 1.0f);
        }

        float clampedAngle = Math.max(0.005f, Math.min(1.45f, beamAngleRad));
        float outerEdgeCos = (float) Math.cos(clampedAngle);
        float innerCoreCos = (float) Math.cos(clampedAngle * 0.45f);
        float coneInner = Math.min(outerEdgeCos, innerCoreCos - 0.002f);
        float coneOuter = Math.max(innerCoreCos, coneInner + 0.002f);

        float actualRange = range;
        if (mc.player != null && mc.level != null) {
            Vec3 startVec = new Vec3(worldPos.x(), worldPos.y(), worldPos.z());
            Vec3 endVec = startVec.add(dir.x() * range, dir.y() * range, dir.z() * range);
            var hitResult = mc.level.clip(new net.minecraft.world.level.ClipContext(
                    startVec,
                    endVec,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,
                    net.minecraft.world.level.ClipContext.Fluid.NONE,
                    mc.player
            ));
            if (hitResult.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
                actualRange = (float) Math.min(range, hitResult.getLocation().distanceTo(startVec) + 1.0);
            }
        }

        FlashlightUniforms.addCustomSpot(relPos, dir, rgbColor, coneInner, coneOuter, actualRange, intensity);
    }

    /**
     * Internal hook called by {@link VisionRenderer#updateFlashlightEveryFrame} right after {@code FlashlightUniforms.beginUpdate(...)}.
     */
    public static void invokeFrameLightEmitters(float partialTick) {
        insideFrameLightCallback = true;
        try {
            for (Consumer<Float> emitter : FRAME_LIGHT_EMITTERS) {
                try {
                    emitter.accept(partialTick);
                } catch (Throwable ignored) {
                }
            }
            if (!QUEUED_IR_SPOTS.isEmpty()) {
                if (isNightVisionActive()) {
                    for (QueuedIrSpot spot : QUEUED_IR_SPOTS) {
                        submitInfraredSpotImmediate(spot.worldPos(), spot.direction(), spot.rgbColor(), spot.beamAngleRad(), spot.range(), spot.intensity());
                    }
                }
                QUEUED_IR_SPOTS.clear();
            }
        } finally {
            insideFrameLightCallback = false;
        }
    }

    /**
     * Registers a modifier callback {@code (entity, baseHeat) -> modifiedHeat} invoked when
     * computing thermal signatures in {@code ThermalEntityPainter}.
     */
    public static void registerThermalEntityModifier(BiFunction<LivingEntity, Float, Float> modifier) {
        if (modifier != null) {
            THERMAL_ENTITY_MODIFIERS.add(modifier);
        }
    }

    public static float applyThermalEntityModifiers(LivingEntity entity, float baseHeat) {
        float heat = baseHeat;
        for (BiFunction<LivingEntity, Float, Float> mod : THERMAL_ENTITY_MODIFIERS) {
            try {
                Float res = mod.apply(entity, heat);
                if (res != null) {
                    heat = Math.max(0.0f, Math.min(1.0f, res));
                }
            } catch (Throwable ignored) {
            }
        }
        return heat;
    }

    /**
     * Registers a thermal entity color override pair {@code (matcher, colorProvider)} used by
     * {@link ThermalEntityPainter#resolveEntityThermalColor(LivingEntity)}.
     */
    public static void registerThermalEntityColorOverride(Predicate<LivingEntity> matcher, ToIntFunction<LivingEntity> colorProvider) {
        if (matcher == null || colorProvider == null) {
            return;
        }
        THERMAL_COLOR_OVERRIDES.add(new ThermalColorOverride(matcher, colorProvider));
        ThermalEntityPainter.setHasPerEntityCustomColor(true);
    }

    /**
     * Single-function overload for thermal entity color override {@code entity -> nullableRgbInt}.
     */
    public static void registerThermalEntityColorOverride(Function<LivingEntity, Integer> override) {
        if (override != null) {
            THERMAL_COLOR_FUNCTIONS.add(override);
            ThermalEntityPainter.setHasPerEntityCustomColor(true);
        }
    }

    public static Integer resolveThermalEntityColorOverride(LivingEntity entity) {
        if (entity == null) {
            return null;
        }
        for (ThermalColorOverride entry : THERMAL_COLOR_OVERRIDES) {
            try {
                if (entry.matcher().test(entity)) {
                    return entry.colorProvider().applyAsInt(entity) & 0x00FFFFFF;
                }
            } catch (Throwable ignored) {
            }
        }
        for (Function<LivingEntity, Integer> fn : THERMAL_COLOR_FUNCTIONS) {
            try {
                Integer col = fn.apply(entity);
                if (col != null) {
                    return col & 0x00FFFFFF;
                }
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    /**
     * Registers a custom HUD overlay layer {@code (guiGraphics, partialTicks)} rendered when
     * the local player's goggles visor is active.
     */
    public static void registerHudOverlayLayer(BiConsumer<GuiGraphics, Float> layer) {
        if (layer != null) {
            HUD_OVERLAY_LAYERS.add(layer);
        }
    }

    public static void renderHudOverlayLayers(GuiGraphics gui, float partialTicks) {
        for (BiConsumer<GuiGraphics, Float> layer : HUD_OVERLAY_LAYERS) {
            try {
                layer.accept(gui, partialTicks);
            } catch (Throwable ignored) {
            }
        }
    }

    @FunctionalInterface
    public interface HudOverlayLayer {
        void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height);
    }

    /**
     * Registers a custom HUD overlay module rendered inside {@link VisionHUDOverlay#render(GuiGraphics, float)}
     * whenever Vision Goggles are equipped and active.
     */
    public static void registerHudOverlayLayer(IHudModule layer) {
        if (layer != null) {
            VisionHUDOverlay.registerModule(layer);
        }
    }

    /**
     * Functional overload to register a HUD overlay layer that always renders when the visor is active.
     */
    public static void registerHudOverlayLayer(HudOverlayLayer renderer) {
        if (renderer == null) {
            return;
        }
        VisionHUDOverlay.registerModule(new IHudModule() {
            @Override
            public boolean shouldRender(Player player, ItemStack goggles) {
                return true;
            }

            @Override
            public void render(GuiGraphics gui, ItemStack goggles, float partialTicks, int width, int height) {
                renderer.render(gui, goggles, partialTicks, width, height);
            }
        });
    }
}
