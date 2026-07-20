package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager.PacketContext;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigSavePacket {
    private final int nvgDuration;
    private final int thermalDuration;
    private final int hydroDuration;
    private final int biometricDuration;
    private final int modularDuration;
    private final List<String> extraBatteries;

    public ConfigSavePacket(int nvg, int thermal, int hydro, int bio, int modular, List<String> batteries) {
        this.nvgDuration = nvg;
        this.thermalDuration = thermal;
        this.hydroDuration = hydro;
        this.biometricDuration = bio;
        this.modularDuration = modular;
        this.extraBatteries = batteries;
    }

    public ConfigSavePacket(FriendlyByteBuf buf) {
        this.nvgDuration = buf.readInt();
        this.thermalDuration = buf.readInt();
        this.hydroDuration = buf.readInt();
        this.biometricDuration = buf.readInt();
        this.modularDuration = buf.readInt();
        int size = buf.readInt();
        this.extraBatteries = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            this.extraBatteries.add(buf.readUtf());
        }
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.nvgDuration);
        buf.writeInt(this.thermalDuration);
        buf.writeInt(this.hydroDuration);
        buf.writeInt(this.biometricDuration);
        buf.writeInt(this.modularDuration);
        buf.writeInt(this.extraBatteries.size());
        for (String s : this.extraBatteries) {
            buf.writeUtf(s);
        }
    }

    public void handle(Supplier<PacketContext> contextSupplier) {
        PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player != null && player.hasPermissions(2)) { // Check if OP
                // Update server config
                ModConfig.updateFromSync(nvgDuration, thermalDuration, hydroDuration, biometricDuration, modularDuration, extraBatteries);
                ModConfig.saveCommon(); // Save only common config on server
                
                // Sync back to ALL players
                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                    dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayer(p, new ConfigSyncPacket(nvgDuration, thermalDuration, hydroDuration, biometricDuration, modularDuration, extraBatteries));
                }
                System.out.println("[Vision Goggles] Config updated by " + player.getName().getString() + " and broadcasted.");
            }
        });
    }
}
