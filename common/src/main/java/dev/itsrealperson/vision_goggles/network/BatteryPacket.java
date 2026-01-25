package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
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

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty()) return;

            CompoundTag nbt = helmet.getOrCreateTag();
            boolean isThermal = helmet.getItem() == ModItems.THERMAL_GOGGLES.get();
            float max = isThermal ? (float)ModConfig.getThermalDuration() : (float)ModConfig.getNvgDuration();
            float current = nbt.getFloat(ModEvents.NBT_BATTERY);
            
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
                    
                    float news = Math.min(max, current + (max * chargeAmount));
                    nbt.putFloat(ModEvents.NBT_BATTERY, news);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}


