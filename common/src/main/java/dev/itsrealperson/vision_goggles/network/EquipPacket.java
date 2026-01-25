package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;

import java.util.function.Supplier;

public class EquipPacket {
    public EquipPacket() {}
    public EquipPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player != null) {
                // Get item from hand
                if (PlatformMethods.equipInSlot(player, player.getMainHandItem())) {
                    // Success logic if needed
                } else if (PlatformMethods.equipInSlot(player, player.getOffhandItem())) {
                    // Success logic
                }
            }
        });
    }
}
