package dev.itsrealperson.vision_goggles.client;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Dedicated-server-safe state machine for the first-person and 3rd-person
 * two-handed assembly animation at the Modification Station.
 */
public final class StationWorkAnimator {

    private static final float WORK_DURATION_SECONDS = 1.25f;
    private static final float MAX_DT_SECONDS = 0.05f;
    private static final long REMOTE_EXPIRE_NANOS = 1_500_000_000L;

    private static boolean working = false;
    private static float progress = 0.0f;
    private static long lastNanos = 0L;
    private static BlockPos stationPos = null;

    private static final Map<UUID, Long> REMOTE_WORKERS = new ConcurrentHashMap<>();

    private StationWorkAnimator() {}

    public static void startLocalWork(BlockPos pos) {
        working = true;
        progress = 0.0f;
        lastNanos = System.nanoTime();
        stationPos = pos;
    }

    public static void reset() {
        working = false;
        progress = 0.0f;
        lastNanos = 0L;
        stationPos = null;
    }

    public static void markEntityWorking(UUID uuid) {
        if (uuid != null) {
            REMOTE_WORKERS.put(uuid, System.nanoTime());
        }
    }

    public static void clearEntityWorking(UUID uuid) {
        if (uuid != null) {
            REMOTE_WORKERS.remove(uuid);
        }
    }

    public static boolean isEntityWorking(UUID uuid, boolean isLocalPlayer) {
        if (isLocalPlayer && working) {
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
        if (!working) return;
        if (player == null || !player.isAlive()) {
            reset();
            return;
        }
        if (stationPos != null && player.distanceToSqr(stationPos.getX() + 0.5, stationPos.getY() + 0.5, stationPos.getZ() + 0.5) > 36.0) {
            reset();
            return;
        }

        long now = System.nanoTime();
        float dt = computeDeltaSeconds(now, lastNanos);
        lastNanos = now;

        if (dt > 0.0f) {
            progress += dt / WORK_DURATION_SECONDS;
            if (progress >= 1.0f) {
                reset();
            }
        }
    }

    public static boolean isWorking() {
        return working;
    }

    public static float getProgress() {
        return Mth.clamp(progress, 0.0f, 1.0f);
    }

    private static float computeDeltaSeconds(long nowNanos, long prevNanos) {
        if (prevNanos <= 0L) return 0.0f;
        float dt = (nowNanos - prevNanos) / 1_000_000_000.0f;
        if (dt < 0.0f) return 0.0f;
        return Math.min(MAX_DT_SECONDS, dt);
    }
}
