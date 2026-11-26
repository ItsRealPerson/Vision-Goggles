package dev.itsrealperson.vision_goggles.api;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.client.VisionShaderManager;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Client-only public API for Vision Goggles addons (HUD layers, thermal entity modifiers, NVG glare).
 * Only invoke from client-side code.
 */
public final class VisionGogglesClientAPI {

    private static final List<BiFunction<LivingEntity, Float, Float>> THERMAL_ENTITY_MODIFIERS = new CopyOnWriteArrayList<>();
    private static final List<Function<LivingEntity, Integer>> THERMAL_COLOR_OVERRIDES = new CopyOnWriteArrayList<>();
    private static final List<Consumer<Float>> FRAME_LIGHT_EMITTERS = new CopyOnWriteArrayList<>();
    private static final List<BiConsumer<GuiGraphics, Float>> HUD_OVERLAY_LAYERS = new CopyOnWriteArrayList<>();

    private VisionGogglesClientAPI() {}

    /** Returns true if the local player's visor is currently active in Night Vision mode. */
    public static boolean isLocalNightVisionActive() {
        return VisionRenderer.isVisorActive() && getLocalActiveMode() == VisionMode.NIGHT_VISION;
    }

    /** Returns true if the local player's visor is currently active in Thermal mode. */
    public static boolean isLocalThermalActive() {
        return VisionRenderer.isVisorActive() && VisionShaderManager.isThermalShaderActive();
    }

    /** Returns the active {@link VisionMode} on the local client, or null if inactive. */
    public static VisionMode getLocalActiveMode() {
        if (!VisionRenderer.isVisorActive()) return null;
        return VisionShaderManager.getActiveMode();
    }

    /** Triggers an immediate NVG glare/flash pulse (e.g. from IR lasers, flares, or weapon flashes). */
    public static void triggerNvgGlare(float intensity) {
        if (intensity > 0.0f) {
            VisionShaderManager.triggerMuzzleFlash(intensity);
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

    /**
     * Registers a custom HUD overlay layer {@code (guiGraphics, partialTicks)} rendered when
     * the local player's goggles visor is active.
     */
    public static void registerHudOverlayLayer(BiConsumer<GuiGraphics, Float> layer) {
        if (layer != null) {
            HUD_OVERLAY_LAYERS.add(layer);
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

    public static void renderHudOverlayLayers(GuiGraphics gui, float partialTicks) {
        for (BiConsumer<GuiGraphics, Float> layer : HUD_OVERLAY_LAYERS) {
            try {
                layer.accept(gui, partialTicks);
            } catch (Throwable ignored) {
            }
        }
    }

    public static void registerThermalEntityColorOverride(Function<LivingEntity, Integer> override) {
        if (override != null) {
            THERMAL_COLOR_OVERRIDES.add(override);
        }
    }

    public static Integer resolveThermalEntityColorOverride(LivingEntity entity) {
        for (Function<LivingEntity, Integer> fn : THERMAL_COLOR_OVERRIDES) {
            try {
                Integer col = fn.apply(entity);
                if (col != null) return col;
            } catch (Throwable ignored) {
            }
        }
        return null;
    }

    public static void registerFrameLightEmitter(Consumer<Float> emitter) {
        if (emitter != null) {
            FRAME_LIGHT_EMITTERS.add(emitter);
        }
    }

    public static void invokeFrameLightEmitters(float partialTick) {
        for (Consumer<Float> emitter : FRAME_LIGHT_EMITTERS) {
            try {
                emitter.accept(partialTick);
            } catch (Throwable ignored) {
            }
        }
    }
}
