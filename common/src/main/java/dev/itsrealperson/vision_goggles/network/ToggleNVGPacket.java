package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.event.ModEvents;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.function.Supplier;

public class ToggleNVGPacket {
    private final boolean switchMode;

    public ToggleNVGPacket(boolean switchMode) {
        this.switchMode = switchMode;
    }

    public ToggleNVGPacket(FriendlyByteBuf buf) {
        this.switchMode = buf.readBoolean();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBoolean(this.switchMode);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty()) return;

            CompoundTag nbt = helmet.getOrCreateTag();
            
            if (this.switchMode) {
                if (helmet.getItem() == ModItems.THERMAL_GOGGLES.get() && nbt.getBoolean(ModEvents.NBT_ACTIVE)) {
                    int mode = nbt.getInt(ModEvents.NBT_MODE);
                    nbt.putInt(ModEvents.NBT_MODE, mode == 0 ? 1 : 0);
                }
            } else {
                boolean newState = !nbt.getBoolean(ModEvents.NBT_ACTIVE);
                if (newState) {
                    boolean isThermal = helmet.getItem() == ModItems.THERMAL_GOGGLES.get();
                    float max = isThermal ? (float)ModConfig.getThermalDuration() : (float)ModConfig.getNvgDuration();
                    if (!nbt.contains(ModEvents.NBT_BATTERY)) nbt.putFloat(ModEvents.NBT_BATTERY, max);
                    if (nbt.getFloat(ModEvents.NBT_BATTERY) <= 0) newState = false;
                }
                nbt.putBoolean(ModEvents.NBT_ACTIVE, newState);
            }
            player.containerMenu.broadcastChanges();
        });
    }
}


