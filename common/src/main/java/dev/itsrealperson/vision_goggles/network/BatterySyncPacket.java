package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class BatterySyncPacket {
    private final float batteryLevel;

    public BatterySyncPacket(float batteryLevel) {
        this.batteryLevel = batteryLevel;
    }

    public BatterySyncPacket(FriendlyByteBuf buf) {
        this.batteryLevel = buf.readFloat();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeFloat(this.batteryLevel);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            Player player = context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
                CompoundTag nbt = helmet.getOrCreateTag();
                nbt.putFloat(ModConstants.TAG_BATTERY, batteryLevel);
            }
        });
    }
}