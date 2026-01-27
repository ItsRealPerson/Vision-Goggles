package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.Objects;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

public record BatteryPacket() implements CustomPacketPayload {
    public static final Type<BatteryPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "battery_packet"));

    public static final StreamCodec<FriendlyByteBuf, BatteryPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new BatteryPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return;

            float max = (float) goggles.getBatteryCapacity(helmet);
            float current = Objects.requireNonNullElse(helmet.get(ModDataComponents.BATTERY.get()), 0.0f);
            
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
                    helmet.set(ModDataComponents.BATTERY.get(), news);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
    }
}