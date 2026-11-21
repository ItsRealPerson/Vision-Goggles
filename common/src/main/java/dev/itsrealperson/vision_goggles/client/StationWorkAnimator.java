package dev.itsrealperson.vision_goggles.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated-server-safe state machine for the first-person and 3rd-person
 * interactive workbench hand animations at the Modification Station.
 */
public final class StationWorkAnimator {

    private static final float OPEN_DURATION_SECONDS = 0.22f;
    private static final float PLACE_DURATION_SECONDS = 0.28f;
    private static final float ASSEMBLE_DURATION_SECONDS = 3.00f;
    private static final float MAX_DT_SECONDS = 0.05f;
    private static final long REMOTE_EXPIRE_NANOS = 1_500_000_000L;

    private static boolean workbenchOpen = false;
    private static float openBlend = 0.0f;
    private static BlockPos stationPos = null;

    private static boolean placing = false;
    private static boolean placingRightHand = true;
    private static float placeProgress = 0.0f;

    private static boolean assembling = false;
    private static float assembleProgress = 0.0f;

    private static long lastNanos = 0L;

    private static final Map<UUID, Long> REMOTE_WORKERS = new ConcurrentHashMap<>();

    private StationWorkAnimator() {}

    public static void setWorkbenchOpen(boolean open, BlockPos pos) {
        if (open) {
            boolean newSession = !workbenchOpen || !java.util.Objects.equals(stationPos, pos);
            workbenchOpen = true;
            stationPos = pos;
            if (newSession) {
                openBlend = 0.0f;
                placing = false;
                placeProgress = 0.0f;
                assembling = false;
                assembleProgress = 0.0f;
            }
            lastNanos = System.nanoTime();
        } else {
            reset();
        }
    }

    public static void triggerPlacePart(boolean rightHand) {
        placing = true;
        placingRightHand = rightHand;
        placeProgress = 0.0f;
        if (lastNanos == 0L) {
            lastNanos = System.nanoTime();
        }
    }

    public static void startLocalWork(BlockPos pos) {
        assembling = true;
        assembleProgress = 0.0f;
        if (pos != null) {
            stationPos = pos;
        }
        if (lastNanos == 0L) {
            lastNanos = System.nanoTime();
        }
    }

    public static void reset() {
        workbenchOpen = false;
        openBlend = 0.0f;
        stationPos = null;
        placing = false;
        placeProgress = 0.0f;
        assembling = false;
        assembleProgress = 0.0f;
        lastNanos = 0L;
    }

    public static void markEntityWorking(UUID uuid) {
        if (uuid != null) {
            REMOTE_WORKERS.put(uuid, System.nanoTime());
        }
    }

    public static boolean isEntityWorking(UUID uuid, boolean isLocalPlayer) {
        if (isLocalPlayer && (workbenchOpen || assembling)) {
            return true;
        }
        if (uuid == null) return false;
        Long ts = REMOTE_WORKERS.get(uuid);
        if (ts == null) return false;
        if ((System.nanoTime() - ts) > REMOTE_EXPIRE_NANOS) {
            REMOTE_WORKERS.remove(uuid);
            return false;
        }
        return true;
    }

    public static void syncFromRender(Player player) {
        if (player == null || !player.isAlive()) {
            reset();
            return;
        }
        if (stationPos != null && player.distanceToSqr(stationPos.getX() + 0.5, stationPos.getY() + 0.5, stationPos.getZ() + 0.5) > 64.0) {
            reset();
            return;
        }

        long now = System.nanoTime();
        float dt = computeDeltaSeconds(now, lastNanos);
        lastNanos = now;

        if (dt <= 0.0f) return;

        boolean shouldStayActive = workbenchOpen || assembling || placing;
        if (shouldStayActive) {
            openBlend = Math.min(1.0f, openBlend + dt / OPEN_DURATION_SECONDS);
        } else if (openBlend > 0.0f) {
            openBlend = Math.max(0.0f, openBlend - dt / OPEN_DURATION_SECONDS);
            if (openBlend <= 0.001f) {
                openBlend = 0.0f;
            }
        }

        if (placing) {
            placeProgress += dt / PLACE_DURATION_SECONDS;
            if (placeProgress >= 1.0f) {
                placing = false;
                placeProgress = 0.0f;
            }
        }

        if (assembling) {
            assembleProgress += dt / ASSEMBLE_DURATION_SECONDS;
            if (assembleProgress >= 1.0f) {
                assembling = false;
                assembleProgress = 0.0f;
            }
        }
    }

    public static boolean isWorking() {
        return workbenchOpen || assembling || placing || openBlend > 0.001f;
    }

    public static float getOpenBlend() {
        return Mth.clamp(openBlend, 0.0f, 1.0f);
    }

    public static boolean isPlacing() {
        return placing;
    }

    public static boolean isPlacingRightHand() {
        return placingRightHand;
    }

    public static float getPlaceProgress() {
        return Mth.clamp(placeProgress, 0.0f, 1.0f);
    }

    public static boolean isAssembling() {
        return assembling;
    }

    public static float getProgress() {
        return Mth.clamp(assembleProgress, 0.0f, 1.0f);
    }

    public static float getWorkbenchZoomMultiplier() {
        if (openBlend <= 0.001f) return 1.0f;
        float openT = openBlend * openBlend * (3.0f - 2.0f * openBlend);
        float targetZoom = 0.80f;
        if (assembling) {
            float p = getProgress();
            float inW = Mth.clamp(p / 0.16f, 0.0f, 1.0f);
            float outW = 1.0f - Mth.clamp((p - 0.84f) / 0.16f, 0.0f, 1.0f);
            float w = (inW * inW * (3.0f - 2.0f * inW)) * (outW * outW * (3.0f - 2.0f * outW));
            targetZoom = Mth.lerp(w, 0.80f, 0.62f);
        }
        return Mth.lerp(openT, 1.0f, targetZoom);
    }

    private static float computeDeltaSeconds(long nowNanos, long prevNanos) {
        if (prevNanos <= 0L) return 0.0f;
        float dt = (nowNanos - prevNanos) / 1_000_000_000.0f;
        if (dt < 0.0f) return 0.0f;
        return Math.min(MAX_DT_SECONDS, dt);
    }
}
