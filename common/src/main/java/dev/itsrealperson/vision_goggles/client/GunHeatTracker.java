package dev.itsrealperson.vision_goggles.client;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks residual barrel/muzzle heat per player when firing guns (TaCZ and other gun mods),
 * decaying smoothly over time (faster in water or rain) for FLIR thermal rendering.
 */
public final class GunHeatTracker {

    private static final float HEAT_PER_SHOT = 0.20f;
    private static final float NORMAL_DECAY_PER_SEC = 0.10f;
    private static final float WET_DECAY_PER_SEC = 0.35f;
    private static final double MAX_SHOOTER_MATCH_DIST_SQ = 3.5 * 3.5;
    private static final long SHOT_DEDUPE_MS = 40L;

    private static final Map<UUID, HeatState> PLAYER_HEAT = new ConcurrentHashMap<>();

    private static final class HeatState {
        float heat;
        long lastUpdateNanos;
        long lastShotMillis;

        HeatState(float heat, long lastUpdateNanos, long lastShotMillis) {
            this.heat = heat;
            this.lastUpdateNanos = lastUpdateNanos;
            this.lastShotMillis = lastShotMillis;
        }
    }

    private GunHeatTracker() {}

    /**
     * Records a gunshot near (x, y, z) and attributes barrel heat to the closest player within 3.5 blocks.
     * Runs regardless of whether thermal goggles are currently active.
     */
    public static void onGunshot(double x, double y, double z) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Player closest = null;
        double bestDistSq = MAX_SHOOTER_MATCH_DIST_SQ;
        for (Player player : mc.level.players()) {
            if (player == null || !player.isAlive()) continue;
            double dx = player.getX() - x;
            double dy = player.getEyeY() - y;
            double dz = player.getZ() - z;
            double dSq = dx * dx + dy * dy + dz * dz;
            if (dSq <= bestDistSq) {
                bestDistSq = dSq;
                closest = player;
            }
        }
        if (closest == null) return;

        long nowNanos = System.nanoTime();
        long nowMillis = System.currentTimeMillis();
        boolean wet = closest.isInWaterOrRain();

        PLAYER_HEAT.compute(closest.getUUID(), (uuid, state) -> {
            if (state == null) {
                return new HeatState(HEAT_PER_SHOT, nowNanos, nowMillis);
            }
            decayState(state, nowNanos, wet);
            if (nowMillis - state.lastShotMillis < SHOT_DEDUPE_MS) {
                return state;
            }
            state.heat = Math.min(1.0f, state.heat + HEAT_PER_SHOT);
            state.lastShotMillis = nowMillis;
            return state;
        });
    }

    /**
     * Returns the current decayed barrel heat in [0.0, 1.0] for the given player,
     * pruning the entry once it cools down to 0.
     */
    public static float getHeat(Player player) {
        if (player == null) return 0.0f;
        UUID uuid = player.getUUID();
        HeatState state = PLAYER_HEAT.get(uuid);
        if (state == null) return 0.0f;

        long nowNanos = System.nanoTime();
        boolean wet = player.isInWaterOrRain();
        synchronized (state) {
            decayState(state, nowNanos, wet);
            if (state.heat <= 0.0f) {
                PLAYER_HEAT.remove(uuid, state);
                return 0.0f;
            }
            return state.heat;
        }
    }

    private static void decayState(HeatState state, long nowNanos, boolean wet) {
        double dt = Math.max(0.0, (nowNanos - state.lastUpdateNanos) / 1_000_000_000.0);
        float rate = wet ? WET_DECAY_PER_SEC : NORMAL_DECAY_PER_SEC;
        state.heat = Math.max(0.0f, state.heat - (float) (rate * dt));
        state.lastUpdateNanos = nowNanos;
    }

    public static void clear() {
        PLAYER_HEAT.clear();
    }
}
