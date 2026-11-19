package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Dedicated-server-safe client state machine for the first-person goggles equip animation.
 *
 * <p>Do not import client-only classes here because {@link VisionGogglesItem#finishUsingItem}
 * notifies {@link #onLocalEquipCompleted(InteractionHand, ItemStack)} under a runtime
 * {@code level.isClientSide} guard.</p>
 */
public final class GogglesEquipAnimator {

    public enum EquipState {
        IDLE,
        EQUIPPING,
        CANCELING,
        COMPLETING
    }

    private static final float MAX_DT_SECONDS = 0.05f;
    private static final float CANCEL_DURATION_SECONDS = 0.18f;
    private static final float RETRACT_DURATION_SECONDS = 0.26f;

    private static EquipState state = EquipState.IDLE;
    private static InteractionHand activeHand = InteractionHand.MAIN_HAND;
    private static float progress = 0.0f;
    private static float retractProgress = 0.0f;
    private static boolean twoHanded = false;
    private static ItemStack trackedStack = ItemStack.EMPTY;
    private static boolean startSoundPending = false;
    private static boolean strapSoundPlayed = false;
    private static long lastNanos = 0L;

    private GogglesEquipAnimator() {}

    public static void onLocalEquipCompleted(InteractionHand hand, ItemStack stack) {
        state = EquipState.COMPLETING;
        activeHand = (hand != null) ? hand : InteractionHand.MAIN_HAND;
        trackedStack = (stack != null && !stack.isEmpty()) ? stack.copy() : ItemStack.EMPTY;
        progress = 1.0f;
        retractProgress = 0.0f;
        startSoundPending = false;
        lastNanos = System.nanoTime();
    }

    public static void syncFromRender(Player player, float partialTicks) {
        long now = System.nanoTime();
        float dt = computeDeltaSeconds(now, lastNanos);
        lastNanos = now;

        if (player == null || !player.isAlive()) {
            reset();
            return;
        }

        if (state == EquipState.COMPLETING && GogglesFlipAnimator.isLocalTransitioning()) {
            reset();
            return;
        }

        boolean usingGoggles = player.isUsingItem() && player.getUseItem().getItem() instanceof VisionGogglesItem;
        if (usingGoggles) {
            InteractionHand usedHand = player.getUsedItemHand();
            ItemStack useStack = player.getUseItem();

            if (state != EquipState.EQUIPPING || activeHand != usedHand) {
                state = EquipState.EQUIPPING;
                activeHand = usedHand;
                strapSoundPlayed = false;
                startSoundPending = true;
                retractProgress = 0.0f;
            }

            trackedStack = useStack.copy();
            InteractionHand opposite = (usedHand == InteractionHand.MAIN_HAND)
                    ? InteractionHand.OFF_HAND
                    : InteractionHand.MAIN_HAND;
            twoHanded = player.getItemInHand(opposite).isEmpty();

            int duration = Math.max(1, useStack.getUseDuration());
            float usedTicks = (float) duration - ((float) player.getUseItemRemainingTicks() - partialTicks);
            progress = Mth.clamp(usedTicks / (float) duration, 0.0f, 1.0f);
            return;
        }

        if (state == EquipState.EQUIPPING) {
            state = EquipState.CANCELING;
        }

        if (state == EquipState.CANCELING) {
            ItemStack current = player.getItemInHand(activeHand);
            if (current.isEmpty()
                    || !(current.getItem() instanceof VisionGogglesItem)
                    || (!trackedStack.isEmpty() && !ItemStack.isSameItem(current, trackedStack))) {
                reset();
                return;
            }
            progress -= dt / CANCEL_DURATION_SECONDS;
            if (progress <= 0.001f) {
                reset();
            }
        } else if (state == EquipState.COMPLETING) {
            retractProgress += dt / RETRACT_DURATION_SECONDS;
            ItemStack current = player.getItemInHand(activeHand);
            if (!current.isEmpty() && !trackedStack.isEmpty() && !ItemStack.matches(current, trackedStack)) {
                reset();
                return;
            }
            if (retractProgress >= 1.0f) {
                reset();
            }
        }
    }

    public static void reset() {
        state = EquipState.IDLE;
        activeHand = InteractionHand.MAIN_HAND;
        progress = 0.0f;
        retractProgress = 0.0f;
        twoHanded = false;
        trackedStack = ItemStack.EMPTY;
        startSoundPending = false;
        strapSoundPlayed = false;
        lastNanos = 0L;
    }

    public static boolean isActive() {
        return state != EquipState.IDLE;
    }

    public static boolean isCompleting() {
        return state == EquipState.COMPLETING;
    }

    public static boolean shouldRenderCustomEquip(InteractionHand hand) {
        return state != EquipState.IDLE && hand == activeHand;
    }

    public static boolean shouldHideOppositeEmptyHand(InteractionHand hand, ItemStack handStack) {
        return state != EquipState.IDLE && twoHanded && hand != activeHand && handStack.isEmpty();
    }

    public static float getProgress() {
        return Mth.clamp(progress, 0.0f, 1.0f);
    }

    public static float getRetractProgress() {
        return Mth.clamp(retractProgress, 0.0f, 1.0f);
    }

    public static boolean isTwoHanded() {
        return twoHanded;
    }

    public static boolean consumeStartSound() {
        if (startSoundPending) {
            startSoundPending = false;
            return true;
        }
        return false;
    }

    public static boolean consumeStrapSound() {
        if (state == EquipState.EQUIPPING && progress >= 0.32f && !strapSoundPlayed) {
            strapSoundPlayed = true;
            return true;
        }
        return false;
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
}
