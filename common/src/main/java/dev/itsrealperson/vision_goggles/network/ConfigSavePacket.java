package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public record ConfigSavePacket(
        int nvg, int thermal, int hydro, int bio, int modular, int theme, List<String> batteries
) implements CustomPacketPayload {
    public static final Type<ConfigSavePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "config_save"));

    public static final StreamCodec<FriendlyByteBuf, ConfigSavePacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeInt(packet.nvg);
                buf.writeInt(packet.thermal);
                buf.writeInt(packet.hydro);
                buf.writeInt(packet.bio);
                buf.writeInt(packet.modular);
                buf.writeInt(packet.theme);
                buf.writeCollection(packet.batteries, FriendlyByteBuf::writeUtf);
            },
            buf -> new ConfigSavePacket(
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
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null || !player.hasPermissions(2)) return;

            ModConfig.data.nvgDurationTicks = nvg;
            ModConfig.data.thermalDurationTicks = thermal;
            ModConfig.data.hydroDurationTicks = hydro;
            ModConfig.data.biometricDurationTicks = bio;
            ModConfig.data.modularDurationTicks = modular;
            ModConfig.data.nvgColorTheme = theme;
            ModConfig.data.extraBatteryItems = batteries;
            ModConfig.save();
            ModConfig.updateBatteryMap();

            // Sync to all players
            for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                NetworkManager.sendToPlayer(p, new ConfigSyncPacket(nvg, thermal, hydro, bio, modular, theme, batteries));
            }
        });
    }
}