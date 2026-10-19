package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import net.fabricmc.api.EnvType;
import net.minecraft.network.FriendlyByteBuf;

import java.util.function.Supplier;

public class OpenLuaScreenPacket {

    public OpenLuaScreenPacket() {}

    public OpenLuaScreenPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        if (context.getEnv() == EnvType.CLIENT) {
            context.queue(() -> {
                dev.itsrealperson.vision_goggles.client.gui.ClientGuiHelper.openLuaScreen();
            });
        }
    }
}
