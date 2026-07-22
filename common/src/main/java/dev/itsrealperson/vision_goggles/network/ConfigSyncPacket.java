package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class ConfigSyncPacket {
    private final String jsonPayload;

    public ConfigSyncPacket(String jsonPayload) {
        this.jsonPayload = jsonPayload;
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
        this.jsonPayload = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.jsonPayload);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ModConfig.applyCommonJson(jsonPayload);
            System.out.println("[Vision Goggles] Config synced from server.");
        });
    }
}
