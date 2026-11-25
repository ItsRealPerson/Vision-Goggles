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
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.ToIntFunction;

/**
 * Public Client-only API for Vision Goggles 2.0.0.
 * Exposes active optical mode queries, optical glare triggers, per-frame IR spot emitters,
 * and thermal entity color overrides for satellite mods.
 */
@Environment(EnvType.CLIENT)
public final class VisionGogglesClientAPI {

    private record ThermalColorOverride(Predicate<LivingEntity> matcher, ToIntFunction<LivingEntity> colorProvider) {}
    private record QueuedIrSpot(Vector3f worldPos, Vector3f direction, Vector3f rgbColor, float beamAngleRad, float range, float intensity) {}

    private static final List<Consumer<Float>> FRAME_LIGHT_EMITTERS = new CopyOnWriteArrayList<>();
    private static final List<ThermalColorOverride> THERMAL_COLOR_OVERRIDES = new CopyOnWriteArrayList<>();
    private static final List<QueuedIrSpot> QUEUED_IR_SPOTS = new CopyOnWriteArrayList<>();
    private static volatile boolean insideFrameLightCallback = false;

    private VisionGogglesClientAPI() {
    }

    public static boolean isVisionActive() {
        return VisionRenderer.isVisorActive() && VisionRenderer.isVisionModeActive();
    }

    public static boolean isNightVisionActive() {
        return isVisionActive() && VisionRenderer.getCurrentVisionMode() == VisionMode.NIGHT_VISION;
    }

    public static boolean isThermalActive() {
        return isVisionActive() && VisionRenderer.getCurrentVisionMode() == VisionMode.THERMAL;
    }

    public static boolean isBiometricActive() {
        return isVisionActive() && VisionRenderer.getCurrentVisionMode() == VisionMode.BIOMETRIC;
    }

    public static boolean isHydroActive() {
        return isVisionActive() && VisionRenderer.getCurrentVisionMode() == VisionMode.HYDRO;
    }

    public static VisionMode getActiveVisionMode() {
        return VisionRenderer.isVisorActive() ? VisionRenderer.getCurrentVisionMode() : null;
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

    public static void triggerOpticalGlare(float intensity) {
        VisionShaderManager.triggerFlash(intensity);
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
     * Registers a thermal entity color override used by {@link ThermalEntityPainter#resolveEntityThermalColor(LivingEntity)}.
     */
    public static void registerThermalEntityColorOverride(Predicate<LivingEntity> matcher, ToIntFunction<LivingEntity> colorProvider) {
        if (matcher == null || colorProvider == null) {
            return;
        }
        THERMAL_COLOR_OVERRIDES.add(new ThermalColorOverride(matcher, colorProvider));
        ThermalEntityPainter.setHasPerEntityCustomColor(true);
    }

    public static Integer resolveThermalEntityColorOverride(LivingEntity entity) {
        if (entity == null || THERMAL_COLOR_OVERRIDES.isEmpty()) {
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
        return null;
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
