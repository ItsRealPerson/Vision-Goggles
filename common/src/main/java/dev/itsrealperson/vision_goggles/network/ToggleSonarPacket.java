package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

import java.util.function.Supplier;

public class ToggleSonarPacket {
    public ToggleSonarPacket() {}

    public ToggleSonarPacket(FriendlyByteBuf buf) {}

    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            if (context.getPlayer() instanceof ServerPlayer player) {
                ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
                if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                    CompoundTag nbt = helmet.getOrCreateTag();
                    int currentMode = nbt.getInt(ModConstants.TAG_SONAR_MODE); // 0=Todos, 1=Jugadores, 2=No-Muertos
                    currentMode = (currentMode + 1) % 3;
                    nbt.putInt(ModConstants.TAG_SONAR_MODE, currentMode);
                    
                    String modeName = currentMode == 0 ? "Todos" : (currentMode == 1 ? "Jugadores" : "No-Muertos");
                    player.displayClientMessage(Component.translatable("message.vision_goggles.sonar_mode", modeName), true);
                }
            }
        });
    }
}
