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
                if (target.type != TargetType.NONE) {
                    String targetName = (target.type == TargetType.FLASHLIGHT) ? "Linterna Táctica" : "Gafas de Visión";
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§e[i] " + targetName + " ya está al 100%"), true);
                } else {
                    player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] No tienes equipo recargable equipado"), true);
                }
                return;
            }

            ItemStack batteryStack = findBattery(player);
            if (batteryStack.isEmpty()) {
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("§c[!] No tienes baterías en el inventario"), true);
                player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.4f, 0.6f);
                return;
            }

            float chargeAmount = ModConfig.getBatteryCharge(batteryStack);
            if (!player.getAbilities().instabuild) {
                batteryStack.shrink(1);
            }
            float news = Math.min(target.max, target.current + (target.max * chargeAmount));
            if (target.type == TargetType.FLASHLIGHT) {
                TacticalFlashlightItem.setBattery(target.stack, news);
            } else {
                target.stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, news);
            }

            String targetName = (target.type == TargetType.FLASHLIGHT) ? "Linterna Táctica" : "Gafas de Visión";
            int pct = (int) Math.round((news / target.max) * 100.0);
            player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_IRON, SoundSource.PLAYERS, 0.8f, 1.3f);
            player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.6f, 1.4f);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("§a[+] Batería Recargada: " + targetName + " (" + pct + "%)"), true);
            player.containerMenu.broadcastChanges();
        });
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
