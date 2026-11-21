package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class OpenProtoPreviewPacket {

    private final String actionOrVariant;

    public OpenProtoPreviewPacket(String actionOrVariant) {
        this.actionOrVariant = actionOrVariant == null ? "" : actionOrVariant;
    }

    public OpenProtoPreviewPacket(FriendlyByteBuf buf) {
        this.actionOrVariant = buf.readUtf(64);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(this.actionOrVariant, 64);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        if (context.getEnv() == EnvType.CLIENT) {
            context.queue(() -> {
                dev.itsrealperson.vision_goggles.client.gui.ClientGuiHelper.handleProtoPreview(this.actionOrVariant);
            });
        }
    }
}
