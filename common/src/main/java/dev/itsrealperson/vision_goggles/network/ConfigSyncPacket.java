package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record ConfigSyncPacket(
        int nvg, int thermal, int hydro, int bio, int modular, int theme, List<String> batteries
) implements CustomPacketPayload {
    public static final Type<ConfigSyncPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "config_sync"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSyncPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.nvg);
                buf.writeInt(packet.thermal);
                buf.writeInt(packet.hydro);
                buf.writeInt(packet.bio);
                buf.writeInt(packet.modular);
                buf.writeInt(packet.theme);
                buf.writeCollection(packet.batteries, FriendlyByteBuf::writeUtf);
            },
            buf -> new ConfigSyncPacket(
                    buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                    buf.readList(FriendlyByteBuf::readUtf)
            )
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            ModConfig.updateFromSync(nvg, thermal, hydro, bio, modular, theme, batteries);
        });
    }
}