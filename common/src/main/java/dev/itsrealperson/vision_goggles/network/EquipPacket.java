package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

public record EquipPacket() implements CustomPacketPayload {
    public static final Type<EquipPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "equip_packet"));

    public static final StreamCodec<FriendlyByteBuf, EquipPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {},
            buf -> new EquipPacket()
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack stack = player.getMainHandItem();
            if (stack.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
                if (PlatformMethods.equipInSlot(player, stack)) {
                    // stack reduction is handled by the platform implementation if successful
                }
            }
        });
    }
}