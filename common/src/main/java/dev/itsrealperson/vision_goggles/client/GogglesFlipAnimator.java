package dev.itsrealperson.vision_goggles.client;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side state machine and frame-rate-independent interpolator for the
 * Vision Goggles flip-down / flip-up visor animation.
 *
 * <p>Uses {@link System#nanoTime()} with a clamped maximum frame delta so
 * 60+ FPS rendering gets smooth sub-tick interpolation without skipping on
 * window focus loss or alt-tabs.</p>
 */
public final class GogglesFlipAnimator {

    public enum FlipState {
        UP,
        FLIPPING_DOWN,
        DOWN_ACTIVE,
        FLIPPING_UP
    }

    private static final float DURATION_SECONDS = 0.28f;
    private static final float MAX_DT_SECONDS = 0.05f;
    private static final long PRUNE_INTERVAL_NANOS = 10_000_000_000L;
    private static final long PRUNE_MAX_AGE_NANOS = 10_000_000_000L;

    private static FlipState state = FlipState.UP;
    private static float linearProgress = 0.0f;
    private static long lastLocalNanos = 0L;

    private static final Map<UUID, EntityAnim> ENTITY_ANIMS = new ConcurrentHashMap<>();
    private static long lastPruneNanos = 0L;

    private static final class EntityAnim {
        float progress;
        long lastNanos;

        EntityAnim(float initialProgress, long nowNanos) {
            this.progress = initialProgress;
            this.lastNanos = nowNanos;
        }
    }

    private GogglesFlipAnimator() {}

    /**
     * Smoothstep easing function: {@code t * t * (3.0f - 2.0f * t)}.
     */
    private static float easeSmoothstep(float t) {
        float clamped = Math.max(0.0f, Math.min(1.0f, t));
        return clamped * clamped * (3.0f - 2.0f * clamped);
    }

    private static float computeDeltaSeconds(long nowNanos, long prevNanos) {
        if (prevNanos <= 0L) {
            return 0.0f;
        }
        float dt = (nowNanos - prevNanos) / 1_000_000_000.0f;
        if (dt < 0.0f) {
            return 0.0f;
        }
        return Math.min(MAX_DT_SECONDS, dt);
    }

    /**
     * Advances the local flip animation using wall-clock frame delta.
     * Safe to call every render frame (60+ FPS) as well as every client tick.
     */
    public static void tickFrame() {
        long now = System.nanoTime();
        float dt = computeDeltaSeconds(now, lastLocalNanos);
        lastLocalNanos = now;

        if (dt <= 0.0f) {
            return;
        }

        float step = dt / DURATION_SECONDS;
        if (state == FlipState.FLIPPING_DOWN) {
            linearProgress += step;
            if (linearProgress >= 1.0f) {
                linearProgress = 1.0f;
                state = FlipState.DOWN_ACTIVE;
            }
        } else if (state == FlipState.FLIPPING_UP) {
            linearProgress -= step;
            if (linearProgress <= 0.0f) {
                linearProgress = 0.0f;
                state = FlipState.UP;
            }
        }
    }

    /**
     * Updates the local flip animation target state (typically called from client tick).
     *
     * @param targetActive whether the server-side goggles active NBT flag is enabled
     * @param hasHelmet    whether the local player currently has Vision Goggles equipped
     */
    public static void updateLocal(boolean targetActive, boolean hasHelmet) {
        if (!hasHelmet) {
            resetLocal();
            return;
        }

        if (targetActive) {
            if (state == FlipState.UP || state == FlipState.FLIPPING_UP) {
                state = FlipState.FLIPPING_DOWN;
                if (lastLocalNanos == 0L) {
                    lastLocalNanos = System.nanoTime();
                }
            }
        } else {
            if (state == FlipState.DOWN_ACTIVE || state == FlipState.FLIPPING_DOWN) {
                state = FlipState.FLIPPING_UP;
                if (lastLocalNanos == 0L) {
                    lastLocalNanos = System.nanoTime();
                }
            }
        }

        tickFrame();
    }

    /**
     * Immediately resets the local animation state to {@link FlipState#UP} with zero progress.
     */
    public static void resetLocal() {
        state = FlipState.UP;
        linearProgress = 0.0f;
        lastLocalNanos = 0L;
    }

    public static FlipState getLocalState() {
        return state;
    }

    public static float getLocalLinearProgress() {
        tickFrame();
        return linearProgress;
    }

    /**
     * Returns the eased local flip progress in {@code [0.0f, 1.0f]},
     * advancing sub-tick interpolation on every render call.
     */
    public static float getLocalProgress() {
        tickFrame();
        return easeSmoothstep(linearProgress);
    }

    public static boolean isLocalFullyDown() {
        return state == FlipState.DOWN_ACTIVE;
    }

    public static boolean isLocalTransitioning() {
        return linearProgress > 0.001f && linearProgress < 0.999f;
    }

    /**
     * Returns the eased flip progress {@code [0.0f, 1.0f]} for any entity
     * (used for 3rd-person local rendering and remote multiplayer players).
     */
    public static float getEntityProgress(UUID entityId, boolean targetActive) {
        if (entityId == null) {
            return targetActive ? 1.0f : 0.0f;
        }

        long now = System.nanoTime();
        EntityAnim anim = ENTITY_ANIMS.computeIfAbsent(
            entityId,
            id -> new EntityAnim(targetActive ? 1.0f : 0.0f, now)
        );

        float dt = computeDeltaSeconds(now, anim.lastNanos);
        anim.lastNanos = now;

        if (dt > 0.0f) {
            float step = dt / DURATION_SECONDS;
            if (targetActive) {
                anim.progress = Math.min(1.0f, anim.progress + step);
            } else {
                anim.progress = Math.max(0.0f, anim.progress - step);
            }
        }

        if (now - lastPruneNanos > PRUNE_INTERVAL_NANOS) {
            lastPruneNanos = now;
            ENTITY_ANIMS.entrySet().removeIf(entry -> (now - entry.getValue().lastNanos) > PRUNE_MAX_AGE_NANOS);
        }

        return easeSmoothstep(anim.progress);
    }

    /**
     * Returns the current eased flip progress {@code [0.0f, 1.0f]} for an entity
     * without advancing the per-entity delta timer (which is already updated by
     * {@link #getEntityProgress(UUID, boolean)} during layer rendering).
     */
    public static float getEntityRenderProgress(UUID entityId, boolean isLocalPlayer) {
        if (isLocalPlayer) {
            return getLocalProgress();
        }
        if (entityId == null) {
            return 0.0f;
        }
        EntityAnim anim = ENTITY_ANIMS.get(entityId);
        return anim != null ? easeSmoothstep(anim.progress) : 0.0f;
    }
}
