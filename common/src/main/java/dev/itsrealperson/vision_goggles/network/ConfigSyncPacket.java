package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ConfigSyncPacket {
    private final int nvgDuration;
    private final int thermalDuration;
    private final int hydroDuration;
    private final int biometricDuration;
    private final int modularDuration;
    private final List<String> extraBatteries;

    public ConfigSyncPacket(int nvg, int thermal, int hydro, int bio, int modular, List<String> batteries) {
        this.nvgDuration = nvg;
        this.thermalDuration = thermal;
        this.hydroDuration = hydro;
        this.biometricDuration = bio;
        this.modularDuration = modular;
        this.extraBatteries = batteries;
    }

    public ConfigSyncPacket(FriendlyByteBuf buf) {
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

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ModConfig.updateFromSync(nvgDuration, thermalDuration, hydroDuration, biometricDuration, modularDuration, extraBatteries);
            System.out.println("[Vision Goggles] Config synced from server.");
        });
    }
}
