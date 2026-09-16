package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.network.BatteryPacket;
import dev.itsrealperson.vision_goggles.network.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BatteryItem extends Item {

    public enum TargetType {
        NONE,
        HELMET,
        FLASHLIGHT
    }

    public static class RechargeTargetInfo {
        public final TargetType type;
        public final ItemStack stack;
        public final float current;
        public final float max;

        public RechargeTargetInfo(TargetType type, ItemStack stack, float current, float max) {
            this.type = type;
            this.stack = stack;
            this.current = current;
            this.max = max;
        }

        public boolean canCharge() {
            return type != TargetType.NONE && current < max;
        }

        public float getPercentage() {
            return max > 0 ? (current / max) : 1.0f;
        }
    }

    public BatteryItem(Properties properties) {
        super(properties);
    }

    public static boolean isBattery(ItemStack stack) {
        return ModConfig.getBatteryCharge(stack) > 0;
    }

    public static RechargeTargetInfo resolveTarget(Player player) {
        if (player == null) return new RechargeTargetInfo(TargetType.NONE, ItemStack.EMPTY, 0, 0);

        // 1. Hand Priority: Flashlight held in either hand
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();
        ItemStack heldFlashlight = ItemStack.EMPTY;
        if (mainHand.getItem() instanceof TacticalFlashlightItem) {
            heldFlashlight = mainHand;
        } else if (offHand.getItem() instanceof TacticalFlashlightItem) {
            heldFlashlight = offHand;
        }

        if (!heldFlashlight.isEmpty()) {
            float cur = TacticalFlashlightItem.getBattery(heldFlashlight);
            float max = TacticalFlashlightItem.MAX_BATTERY;
            return new RechargeTargetInfo(TargetType.FLASHLIGHT, heldFlashlight, cur, max);
        }

        // 2. Equipped items: Check both Goggles (head) and Flashlight (Curios body/chest)
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        RechargeTargetInfo helmetInfo = null;
        if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem goggles) {
            float max = (float) goggles.getBatteryCapacity();
            if (goggles instanceof ModularGogglesItem modular) {
                max = (float) modular.getBatteryCapacity(helmet);
            }
            float cur = helmet.hasTag() && helmet.getTag().contains(ModConstants.TAG_BATTERY)
                    ? helmet.getTag().getFloat(ModConstants.TAG_BATTERY) : max;
            helmetInfo = new RechargeTargetInfo(TargetType.HELMET, helmet, cur, max);
        }

        ItemStack equippedFlash = PlatformMethods.getEquippedFlashlight(player);
        RechargeTargetInfo flashInfo = null;
        if (!equippedFlash.isEmpty() && equippedFlash.getItem() instanceof TacticalFlashlightItem) {
            float cur = TacticalFlashlightItem.getBattery(equippedFlash);
            float max = TacticalFlashlightItem.MAX_BATTERY;
            flashInfo = new RechargeTargetInfo(TargetType.FLASHLIGHT, equippedFlash, cur, max);
        }

        // 3. Recharge whichever is more discharged (lowest %)
        boolean helmetCanCharge = helmetInfo != null && helmetInfo.canCharge();
        boolean flashCanCharge = flashInfo != null && flashInfo.canCharge();

        if (helmetCanCharge && flashCanCharge) {
            return (flashInfo.getPercentage() <= helmetInfo.getPercentage()) ? flashInfo : helmetInfo;
        } else if (flashCanCharge) {
            return flashInfo;
        } else if (helmetCanCharge) {
            return helmetInfo;
        }

        // Both equipped but already full
        if (flashInfo != null && helmetInfo != null) {
            return (flashInfo.getPercentage() <= helmetInfo.getPercentage()) ? flashInfo : helmetInfo;
        } else if (flashInfo != null) {
            return flashInfo;
        } else if (helmetInfo != null) {
            return helmetInfo;
        }

        return new RechargeTargetInfo(TargetType.NONE, ItemStack.EMPTY, 0, 0);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack batteryStack = player.getItemInHand(hand);
        RechargeTargetInfo target = resolveTarget(player);

        if (target.type != TargetType.NONE) {
            if (target.canCharge()) {
                if (level.isClientSide) {
                    NetworkManager.INSTANCE.sendToServer(new BatteryPacket());
                    float charge = ModConfig.getBatteryCharge(batteryStack);
                    int pct = (int)(charge * 100);
                    if (target.type == TargetType.FLASHLIGHT) {
                        player.displayClientMessage(Component.translatable("message.vision_goggles.recharged_flashlight", pct), true);
                    } else {
                        player.displayClientMessage(Component.translatable("message.vision_goggles.recharged", pct), true);
                    }
                }
                return InteractionResultHolder.sidedSuccess(batteryStack, level.isClientSide);
            } else {
                if (level.isClientSide) {
                    player.displayClientMessage(Component.translatable("message.vision_goggles.fully_charged").withStyle(ChatFormatting.YELLOW), true);
                }
                return InteractionResultHolder.fail(batteryStack);
            }
        } else {
            if (level.isClientSide) {
                player.displayClientMessage(Component.translatable("message.vision_goggles.equip_warning"), true);
            }
            return InteractionResultHolder.fail(batteryStack);
        }
    }
}
