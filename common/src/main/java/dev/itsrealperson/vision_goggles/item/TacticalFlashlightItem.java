package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.FlashlightMode;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TacticalFlashlightItem extends Item {
    public static final float MAX_BATTERY = 6000.0f;

    public TacticalFlashlightItem(Properties properties) {
        super(properties);
    }

    public static boolean isActive(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof TacticalFlashlightItem)) return false;
        return stack.hasTag() && stack.getTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && getBattery(stack) > 0;
    }

    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag()) return 0;
        return stack.getTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
    }

    public static float getBattery(ItemStack stack) {
        if (stack.isEmpty() || !stack.hasTag() || !stack.getTag().contains(ModConstants.TAG_BATTERY)) {
            return MAX_BATTERY;
        }
        return stack.getTag().getFloat(ModConstants.TAG_BATTERY);
    }

    public static void setBattery(ItemStack stack, float battery) {
        stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, Math.max(0, Math.min(MAX_BATTERY, battery)));
    }

    public static ItemStack getHeldActiveFlashlight(Player player) {
        if (player == null) return ItemStack.EMPTY;
        // 1. Check equipped vest flashlight
        ItemStack equipped = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedFlashlight(player);
        if (isActive(equipped)) return equipped;

        // 2. Check held hands
        ItemStack main = player.getMainHandItem();
        if (isActive(main)) return main;
        ItemStack off = player.getOffhandItem();
        if (isActive(off)) return off;
        return ItemStack.EMPTY;
    }

    public static ItemStack getAnyHeldFlashlight(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack equipped = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedFlashlight(player);
        if (!equipped.isEmpty() && equipped.getItem() instanceof TacticalFlashlightItem) return equipped;
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof TacticalFlashlightItem) return main;
        ItemStack off = player.getOffhandItem();
        if (off.getItem() instanceof TacticalFlashlightItem) return off;
        return ItemStack.EMPTY;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player.isCrouching()) {
            // Shift + Right Click: Equip to Curios body/chest slot!
            if (dev.itsrealperson.vision_goggles.util.PlatformMethods.equipFlashlight(player, stack)) {
                level.playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC, SoundSource.PLAYERS, 0.8f, 1.2f);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
            }
            // If already full, cycle modes
            int currentMode = getMode(stack);
            int nextMode = (currentMode + 1) % 3;
            stack.getOrCreateTag().putInt(ModConstants.TAG_FLASHLIGHT_MODE, nextMode);
            level.playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.4f, 1.2f + nextMode * 0.15f);

            if (level.isClientSide) {
                FlashlightMode fMode = FlashlightMode.byId(nextMode);
                player.displayClientMessage(Component.translatable("message.vision_goggles.flashlight_mode." + fMode.name), true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        } else {
            // Toggle active
            toggle(stack, player);
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
    }

    public static void toggle(ItemStack stack, Player player) {
        if (stack.isEmpty()) return;
        float battery = getBattery(stack);
        if (battery <= 0) {
            player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.3f, 0.5f);
            if (player.level().isClientSide) {
                player.displayClientMessage(Component.translatable("message.vision_goggles.low_battery").withStyle(ChatFormatting.RED), true);
            }
            return;
        }

        boolean active = !stack.getOrCreateTag().getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE);
        stack.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, active);
        player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.5f, active ? 1.0f : 0.7f);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slotId, boolean isSelected) {
        if (!level.isClientSide && entity instanceof Player player) {
            boolean isHeld = player.getMainHandItem() == stack || player.getOffhandItem() == stack;
            boolean isEquipped = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedFlashlight(player) == stack;
            if (isActive(stack)) {
                if (isHeld || isEquipped) {
                    float current = getBattery(stack);
                    float drain = ModConfig.getBatteryDrainMultiplier();
                    float next = Math.max(0, current - drain);
                    setBattery(stack, next);

                    if (next <= 0) {
                        stack.getOrCreateTag().putBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE, false);
                        level.playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.4f, 0.5f);
                    }
                }
            }
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getBattery(stack) < MAX_BATTERY;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13.0F * getBattery(stack) / MAX_BATTERY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        float pct = getBattery(stack) / MAX_BATTERY;
        if (pct > 0.5f) return 0x00FF00;
        if (pct > 0.2f) return 0xFFFF00;
        return 0xFF0000;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        boolean active = isActive(stack);
        int modeId = getMode(stack);
        FlashlightMode mode = FlashlightMode.byId(modeId);
        float pct = (getBattery(stack) / MAX_BATTERY) * 100.0f;

        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.state",
                active ? Component.translatable("tooltip.vision_goggles.active").withStyle(ChatFormatting.GREEN)
                        : Component.translatable("tooltip.vision_goggles.inactive").withStyle(ChatFormatting.RED)));
        Component modeName = Component.translatable("message.vision_goggles.flashlight_mode." + mode.name);
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.flashlight.mode", modeName).withStyle(ChatFormatting.GRAY));
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.flashlight.battery", String.format("%.1f", pct)).withStyle(ChatFormatting.YELLOW));
        tooltipComponents.add(Component.literal(""));
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.flashlight.controls_toggle").withStyle(ChatFormatting.DARK_GRAY));
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.flashlight.controls_cycle").withStyle(ChatFormatting.DARK_GRAY));
    }
}
