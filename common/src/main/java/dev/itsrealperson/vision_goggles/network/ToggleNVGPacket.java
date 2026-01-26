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

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import java.util.List;
import java.util.ArrayList;
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
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return;

            CompoundTag nbt = helmet.getOrCreateTag();
            List<VisionMode> modes;
            if (goggles instanceof ModularGogglesItem modular) {
                modes = modular.getModes(helmet);
            } else {
                modes = goggles.getSupportedModes();
            }
            
            if (this.switchMode) {
                if (nbt.getBoolean(ModEvents.NBT_ACTIVE)) {
                    if (modes.size() > 1) {
                        int currentModeId = nbt.getInt(ModEvents.NBT_MODE);
                        
                        // Find current index
                        int index = -1;
                        for (int i = 0; i < modes.size(); i++) {
                            if (modes.get(i).getId() == currentModeId) {
                                index = i;
                                break;
                            }
                        }
                        
                        // Next index
                        int nextIndex = (index + 1) % modes.size();
                        nbt.putInt(ModEvents.NBT_MODE, modes.get(nextIndex).getId());
                    }
                }
            } else {
                boolean newState = !nbt.getBoolean(ModEvents.NBT_ACTIVE);
                if (newState) {
                    // Ensure mode is valid on startup
                    if (modes.isEmpty()) {
                        newState = false; // Cannot activate if no modules
                    } else {
                        if (!nbt.contains(ModEvents.NBT_MODE)) {
                             nbt.putInt(ModEvents.NBT_MODE, modes.get(0).getId());
                        } else {
                            // Validate current mode still exists
                            int currentModeId = nbt.getInt(ModEvents.NBT_MODE);
                            boolean exists = false;
                            for (VisionMode m : modes) {
                                if (m.getId() == currentModeId) {
                                    exists = true;
                                    break;
                                }
                            }
                            if (!exists) nbt.putInt(ModEvents.NBT_MODE, modes.get(0).getId());
                        }

                        float max;
                        if (goggles instanceof ModularGogglesItem modular) {
                            max = (float) modular.getBatteryCapacity(helmet);
                        } else {
                            max = (float) goggles.getBatteryCapacity();
                        }

                        if (!nbt.contains(ModEvents.NBT_BATTERY)) nbt.putFloat(ModEvents.NBT_BATTERY, max);
                        if (nbt.getFloat(ModEvents.NBT_BATTERY) <= 0) newState = false;
                    }
                }
                nbt.putBoolean(ModEvents.NBT_ACTIVE, newState);
            }
            player.containerMenu.broadcastChanges();
        });
    }
}


