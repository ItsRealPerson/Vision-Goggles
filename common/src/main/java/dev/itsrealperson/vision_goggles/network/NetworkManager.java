package dev.itsrealperson.vision_goggles.network;

import net.minecraft.server.level.ServerPlayer;

public class NetworkManager {

    public static void register() {
        // C2S (Client to Server)
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.c2s(), ToggleNVGPacket.TYPE, ToggleNVGPacket.CODEC, (payload, context) -> payload.handle(context));
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.c2s(), BatteryPacket.TYPE, BatteryPacket.CODEC, (payload, context) -> payload.handle(context));
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.c2s(), ConfigSavePacket.TYPE, ConfigSavePacket.CODEC, (payload, context) -> payload.handle(context));
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.c2s(), EquipPacket.TYPE, EquipPacket.CODEC, (payload, context) -> payload.handle(context));

        // S2C (Server to Client)
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.s2c(), BatterySyncPacket.TYPE, BatterySyncPacket.CODEC, (payload, context) -> payload.handle(context));
        dev.architectury.networking.NetworkManager.registerReceiver(dev.architectury.networking.NetworkManager.s2c(), ConfigSyncPacket.TYPE, ConfigSyncPacket.CODEC, (payload, context) -> payload.handle(context));
    }

    public static void sendToServer(net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        dev.architectury.networking.NetworkManager.sendToServer(payload);
    }

    public static void sendToPlayer(ServerPlayer player, net.minecraft.network.protocol.common.custom.CustomPacketPayload payload) {
        dev.architectury.networking.NetworkManager.sendToPlayer(player, payload);
    }
}