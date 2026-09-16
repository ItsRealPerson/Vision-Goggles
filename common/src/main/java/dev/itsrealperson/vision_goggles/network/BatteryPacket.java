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
            if (target.canCharge()) {
                ItemStack batteryStack = findBattery(player);
                if (!batteryStack.isEmpty()) {
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
                    player.level().playSound(null, player.blockPosition(), ModSounds.BLIP.get(), SoundSource.PLAYERS, 0.6f, 1.2f);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }

    private static ItemStack findBattery(ServerPlayer player) {
        ItemStack handStack = player.getMainHandItem();
        if (ModConfig.getBatteryCharge(handStack) > 0) return handStack;
        ItemStack offHandStack = player.getOffhandItem();
        if (ModConfig.getBatteryCharge(offHandStack) > 0) return offHandStack;
        return ItemStack.EMPTY;
    }
}
