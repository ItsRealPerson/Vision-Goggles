package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Supplier;

public class ConfigSavePacket {
    private final String jsonPayload;

    public ConfigSavePacket(String jsonPayload) {
        this.jsonPayload = jsonPayload;
    }

    public ConfigSavePacket(FriendlyByteBuf buf) {
        this.jsonPayload = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.jsonPayload);
    }

    public void handle(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null && player.hasPermissions(2)) { // Check if OP
                // Update server config
                ModConfig.applyCommonJson(jsonPayload);
                ModConfig.saveCommon(); // Save only common config on server
                
                // Sync back to ALL players
                String updatedJson = ModConfig.toCommonJson();
                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                    dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayer(p, new ConfigSyncPacket(updatedJson));
                }
                System.out.println("[Vision Goggles] Config updated by " + player.getName().getString() + " and broadcasted.");
            }
        });
    }
}
