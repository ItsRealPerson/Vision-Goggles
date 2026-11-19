package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.item.BatteryItem;
import dev.itsrealperson.vision_goggles.item.BatteryItem.RechargeTargetInfo;
import dev.itsrealperson.vision_goggles.item.BatteryItem.TargetType;
import dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class BatteryPacket {
    public BatteryPacket() {}
    public BatteryPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            RechargeTargetInfo target = BatteryItem.resolveTarget(player);
            if (!target.canCharge()) {
                rechargeWithStack(player, ItemStack.EMPTY, true);
                return;
            }

            ItemStack batteryStack = findBattery(player);
            rechargeWithStack(player, batteryStack, true);
        });
    }

    public static boolean rechargeWithStack(ServerPlayer player, ItemStack batteryStack, boolean notifyIfNotNeeded) {
        if (player == null) return false;

        RechargeTargetInfo target = BatteryItem.resolveTarget(player);
        if (!target.canCharge()) {
            if (notifyIfNotNeeded) {
                if (target.type != TargetType.NONE && !target.stack.isEmpty()) {
                    Component targetName = Component.translatable(target.stack.getDescriptionId());
                    player.displayClientMessage(Component.translatable("message.vision_goggles.target_full", targetName), true);
                } else {
                    player.displayClientMessage(Component.translatable("message.vision_goggles.no_rechargeable"), true);
                }
            }
            return false;
        }

        if (batteryStack.isEmpty() || ModConfig.getBatteryCharge(batteryStack) <= 0) {
            player.displayClientMessage(Component.translatable("message.vision_goggles.no_batteries"), true);
            player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.4f, 0.6f);
            return false;
        }

        float chargeAmount = ModConfig.getBatteryCharge(batteryStack);
        if (!player.getAbilities().instabuild) {
            batteryStack.shrink(1);
        }
        float news = Math.min(target.max, target.current + (target.max * chargeAmount));
        if (target.type == TargetType.FLASHLIGHT) {
            TacticalFlashlightItem.setBattery(target.stack, news);
            dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayer(player, new BatterySyncPacket(news, true));
        } else {
            target.stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, news);
            target.stack.getOrCreateTag().putFloat(ModConstants.TAG_LAST_SYNC, news);
            dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayer(player, new BatterySyncPacket(news, false));
        }

        Component targetName = Component.translatable(target.stack.getDescriptionId());
        int pct = (int) Math.round((news / target.max) * 100.0);
        player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8f, 1.3f);
        player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
        player.displayClientMessage(Component.translatable("message.vision_goggles.recharged_named", targetName, pct), true);
        player.containerMenu.broadcastChanges();
        return true;
    }

    private static ItemStack findBattery(ServerPlayer player) {
        // 1. Check hands
        ItemStack handStack = player.getMainHandItem();
        if (ModConfig.getBatteryCharge(handStack) > 0) return handStack;
        ItemStack offHandStack = player.getOffhandItem();
        if (ModConfig.getBatteryCharge(offHandStack) > 0) return offHandStack;
        // 2. Check full inventory
        for (ItemStack stack : player.getInventory().items) {
            if (ModConfig.getBatteryCharge(stack) > 0) return stack;
        }
        return ItemStack.EMPTY;
    }
}
