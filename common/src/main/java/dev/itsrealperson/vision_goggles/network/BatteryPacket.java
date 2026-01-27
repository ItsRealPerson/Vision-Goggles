package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

public class BatteryPacket {
    public BatteryPacket() {}
    public BatteryPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return;

            float max = (float) goggles.getBatteryCapacity();
            if (goggles instanceof ModularGogglesItem modular) {
                max = (float) modular.getBatteryCapacity(helmet);
            }

            CompoundTag nbt = helmet.getOrCreateTag();
            float current = nbt.getFloat(ModConstants.TAG_BATTERY);
            
            if (current < max) {
                boolean batteryFound = false;
                float chargeAmount = 0.0f;
                ItemStack batteryStack = ItemStack.EMPTY;
                
                ItemStack handStack = player.getMainHandItem();
                if (ModConfig.getBatteryCharge(handStack) > 0) {
                    batteryStack = handStack;
                    chargeAmount = ModConfig.getBatteryCharge(handStack);
                    batteryFound = true;
                } else {
                    ItemStack offHandStack = player.getOffhandItem();
                    if (ModConfig.getBatteryCharge(offHandStack) > 0) {
                        batteryStack = offHandStack;
                        chargeAmount = ModConfig.getBatteryCharge(offHandStack);
                        batteryFound = true;
                    }
                }

                if (batteryFound && !batteryStack.isEmpty()) {
                    if (!player.getAbilities().instabuild) {
                        batteryStack.shrink(1);
                    }
                    
                    // Recharge 50% of the CURRENT max capacity
                    float news = Math.min(max, current + (max * chargeAmount));
                    nbt.putFloat(ModConstants.TAG_BATTERY, news);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}