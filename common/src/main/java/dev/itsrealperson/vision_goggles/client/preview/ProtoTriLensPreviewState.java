package dev.itsrealperson.vision_goggles.client.preview;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.util.Mth;

@Environment(EnvType.CLIENT)
public final class ProtoTriLensPreviewState {

    public enum FlipState {
        UP,
        FLIPPING_DOWN,
        DOWN_ACTIVE,
        FLIPPING_UP
    }

    private static final float FLIP_DURATION_SECONDS = 0.30f;
    private static final float IN_GAME_FLIP_DURATION_SECONDS = 0.78f;
    private static final float REACH_PHASE_END = 0.28f;
    private static final float VISOR_PHASE_END = 0.84f;
    private static final float OPTICS_HANDOFF_T = 0.85f;
    private static final float MAX_DT_SECONDS = 0.05f;

    private static ProtoTriLensVariant selectedVariant = ProtoTriLensVariant.NVG;
    private static boolean visorUp = false;
    private static boolean glowEnabled = true;
    private static boolean headPreviewEnabled = false;
    private static boolean autoSpin = false;
    private static boolean showHeadGuide = true;

    private static float visorProgress = 0.0f; // 0.0 = down over eyes, 1.0 = flipped up on forehead
    private static long lastNanos = 0L;

    private static FlipState inGameFlipState = FlipState.UP;
    private static float inGameLinearProgress = 0.0f; // 0.0 = UP, 1.0 = DOWN
    private static long lastInGameNanos = 0L;

    private ProtoTriLensPreviewState() {}

    public static ProtoTriLensVariant getSelectedVariant() {
        return selectedVariant;
    }

    public static void setSelectedVariant(ProtoTriLensVariant variant) {
        if (variant != null) {
            selectedVariant = variant;
        }
    }

    public static boolean isVisorUp() {
        return visorUp;
    }

    public static void setVisorUp(boolean up) {
        visorUp = up;
        syncInGameOnVisorChange();
    }

    public static void toggleVisorUp() {
        visorUp = !visorUp;
        syncInGameOnVisorChange();
    }

    private static void syncInGameOnVisorChange() {
        if (!headPreviewEnabled) {
            syncInGameStateToVisorUp();
            return;
        }
        if (visorUp) {
            if (inGameLinearProgress > 0.0f) {
                inGameFlipState = FlipState.FLIPPING_UP;
                if (lastInGameNanos == 0L) {
                    lastInGameNanos = System.nanoTime();
                }
            } else {
                inGameFlipState = FlipState.UP;
                inGameLinearProgress = 0.0f;
                lastInGameNanos = 0L;
            }
        } else {
            if (inGameLinearProgress < 1.0f) {
                inGameFlipState = FlipState.FLIPPING_DOWN;
                if (lastInGameNanos == 0L) {
                    lastInGameNanos = System.nanoTime();
                }
            } else {
                inGameFlipState = FlipState.DOWN_ACTIVE;
                inGameLinearProgress = 1.0f;
                lastInGameNanos = 0L;
            }
        }
    }

    private static void syncInGameStateToVisorUp() {
        inGameFlipState = visorUp ? FlipState.UP : FlipState.DOWN_ACTIVE;
        inGameLinearProgress = visorUp ? 0.0f : 1.0f;
        lastInGameNanos = 0L;
    }

    public static boolean isGlowEnabled() {
        return glowEnabled;
    }

    public static void setGlowEnabled(boolean enabled) {
        glowEnabled = enabled;
    }

    public static void toggleGlowEnabled() {
        glowEnabled = !glowEnabled;
    }

    public static boolean isHeadPreviewEnabled() {
        return headPreviewEnabled;
    }

    public static void setHeadPreviewEnabled(boolean enabled) {
        headPreviewEnabled = enabled;
        visorProgress = visorUp ? 1.0f : 0.0f;
        lastNanos = 0L;
        syncInGameStateToVisorUp();
    }

    public static void toggleHeadPreviewEnabled() {
        setHeadPreviewEnabled(!headPreviewEnabled);
    }

    public static boolean isAutoSpin() {
        return autoSpin;
    }

    public static void setAutoSpin(boolean spin) {
        autoSpin = spin;
    }

    public static void toggleAutoSpin() {
        autoSpin = !autoSpin;
    }

    public static boolean isShowHeadGuide() {
        return showHeadGuide;
    }

    public static void setShowHeadGuide(boolean show) {
        showHeadGuide = show;
    }

    public static void toggleShowHeadGuide() {
        showHeadGuide = !showHeadGuide;
    }

    public static float getVisorProgress() {
        long now = System.nanoTime();
        if (lastNanos == 0L) {
            lastNanos = now;
            visorProgress = visorUp ? 1.0f : 0.0f;
        } else {
            float dt = Math.min((now - lastNanos) / 1_000_000_000.0f, MAX_DT_SECONDS);
            lastNanos = now;
            float target = visorUp ? 1.0f : 0.0f;
            float step = dt / FLIP_DURATION_SECONDS;
            if (visorProgress < target) {
                visorProgress = Math.min(target, visorProgress + step);
            } else if (visorProgress > target) {
                visorProgress = Math.max(target, visorProgress - step);
            }
        }
        // Smoothstep easing for clean mechanical hinge feel
        float t = Mth.clamp(visorProgress, 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private static float easeSmoothstep(float t) {
        float clamped = Mth.clamp(t, 0.0f, 1.0f);
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static float computeVisorProgress(float rawT) {
        if (rawT <= REACH_PHASE_END) {
            return 0.0f;
        }
        if (rawT >= VISOR_PHASE_END) {
            return 1.0f;
        }
        return easeSmoothstep((rawT - REACH_PHASE_END) / (VISOR_PHASE_END - REACH_PHASE_END));
    }

    private static float computeArmReach(float rawT) {
        if (rawT <= 0.001f || rawT >= 0.999f) {
            return 0.0f;
        }
        if (rawT <= REACH_PHASE_END) {
            return easeSmoothstep(rawT / REACH_PHASE_END);
        }
        if (rawT <= VISOR_PHASE_END) {
            return 1.0f;
        }
        return 1.0f - easeSmoothstep((rawT - VISOR_PHASE_END) / (1.0f - VISOR_PHASE_END));
    }

    public static void tickInGameFlipFrame() {
        if (inGameFlipState != FlipState.FLIPPING_DOWN && inGameFlipState != FlipState.FLIPPING_UP) {
            lastInGameNanos = 0L;
            return;
        }

        long now = System.nanoTime();
        if (lastInGameNanos <= 0L) {
            lastInGameNanos = now;
            return;
        }

        float dt = (now - lastInGameNanos) / 1_000_000_000.0f;
        lastInGameNanos = now;
        if (dt <= 0.0f) {
            return;
        }
        dt = Math.min(dt, MAX_DT_SECONDS);

        float step = dt / IN_GAME_FLIP_DURATION_SECONDS;
        if (inGameFlipState == FlipState.FLIPPING_DOWN) {
            inGameLinearProgress += step;
            if (inGameLinearProgress >= 1.0f) {
                inGameLinearProgress = 1.0f;
                inGameFlipState = FlipState.DOWN_ACTIVE;
                lastInGameNanos = 0L;
            }
        } else {
            inGameLinearProgress -= step;
            if (inGameLinearProgress <= 0.0f) {
                inGameLinearProgress = 0.0f;
                inGameFlipState = FlipState.UP;
                lastInGameNanos = 0L;
            }
        }
    }

    /**
     * Toggles the in-game prototype visor flip direction and returns {@code true} if flipping down.
     */
    public static boolean toggleInGameVisor() {
        visorUp = !visorUp;
        inGameFlipState = visorUp ? FlipState.FLIPPING_UP : FlipState.FLIPPING_DOWN;
        if (lastInGameNanos == 0L) {
            lastInGameNanos = System.nanoTime();
        }
        if (lastNanos == 0L) {
            lastNanos = System.nanoTime();
        }
        return !visorUp;
    }

    public static boolean isInGameFlipTransitioning() {
        return headPreviewEnabled && (inGameFlipState == FlipState.FLIPPING_DOWN || inGameFlipState == FlipState.FLIPPING_UP);
    }

    public static float peekInGameLinearProgress() {
        return inGameLinearProgress;
    }

    public static float peekInGameVisorDownProgress() {
        return computeVisorProgress(inGameLinearProgress);
    }

    public static float getInGameVisorDownProgress() {
        tickInGameFlipFrame();
        return computeVisorProgress(inGameLinearProgress);
    }

    public static float getInGameVisorUpProgress() {
        tickInGameFlipFrame();
        return 1.0f - computeVisorProgress(inGameLinearProgress);
    }

    public static float getInGameArmReach() {
        if (!headPreviewEnabled) {
            return 0.0f;
        }
        tickInGameFlipFrame();
        return computeArmReach(inGameLinearProgress);
    }

    public static float getInGameReachPhaseEnd() {
        return REACH_PHASE_END;
    }

    public static float getInGameVisorPhaseEnd() {
        return VISOR_PHASE_END;
    }

    public static float getInGameOpticsHandoffThreshold() {
        return OPTICS_HANDOFF_T;
    }

    public static void reset() {
        selectedVariant = ProtoTriLensVariant.NVG;
        visorUp = false;
        glowEnabled = true;
        headPreviewEnabled = false;
        autoSpin = false;
        showHeadGuide = true;
        visorProgress = 0.0f;
        lastNanos = 0L;
        inGameFlipState = FlipState.UP;
        inGameLinearProgress = 0.0f;
        lastInGameNanos = 0L;
    }
}
