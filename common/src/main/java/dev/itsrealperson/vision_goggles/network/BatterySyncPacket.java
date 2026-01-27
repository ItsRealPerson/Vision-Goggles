package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public record BatterySyncPacket(float batteryLevel) implements CustomPacketPayload {
    public static final Type<BatterySyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "battery_sync"));

    public static final StreamCodec<FriendlyByteBuf, BatterySyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeFloat(packet.batteryLevel),
            buf -> new BatterySyncPacket(buf.readFloat())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                helmet.set(ModDataComponents.BATTERY.get(), batteryLevel);
            }
        });
    }
}