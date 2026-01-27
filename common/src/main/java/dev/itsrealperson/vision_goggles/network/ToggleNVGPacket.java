package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.ModuleType;
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
            List<ModuleType> utils = new java.util.ArrayList<>();
            if (goggles instanceof ModularGogglesItem modular) {
                modes = modular.getModes(helmet);
                utils = modular.getUtilityModules(helmet);
            } else {
                modes = goggles.getSupportedModes();
            }
            
            if (this.switchMode) {
                if (nbt.getBoolean(ModConstants.TAG_ACTIVE)) {
                    if (modes.size() > 1) {
                        int currentModeId = nbt.getInt(ModConstants.TAG_MODE);
                        int index = -1;
                        for (int i = 0; i < modes.size(); i++) {
                            if (modes.get(i).getId() == currentModeId) {
                                index = i;
                                break;
                            }
                        }
                        int nextIndex = (index + 1) % modes.size();
                        nbt.putInt(ModConstants.TAG_MODE, modes.get(nextIndex).getId());
                    }
                }
            } else {
                boolean newState = !nbt.getBoolean(ModConstants.TAG_ACTIVE);
                if (newState) {
                    // Can activate if has vision modes OR utility modules
                    if (modes.isEmpty() && utils.isEmpty()) {
                        newState = false; 
                    } else {
                        // If has modes, ensure one is selected
                        if (!modes.isEmpty()) {
                            if (!nbt.contains(ModConstants.TAG_MODE)) {
                                 nbt.putInt(ModConstants.TAG_MODE, modes.get(0).getId());
                            } else {
                                int currentModeId = nbt.getInt(ModConstants.TAG_MODE);
                                boolean exists = false;
                                for (VisionMode m : modes) {
                                    if (m.getId() == currentModeId) {
                                        exists = true;
                                        break;
                                    }
                                }
                                if (!exists) nbt.putInt(ModConstants.TAG_MODE, modes.get(0).getId());
                            }
                        } else {
                            // If no vision modes, set mode to -1 (None)
                            nbt.putInt(ModConstants.TAG_MODE, -1);
                        }

                        float max;
                        if (goggles instanceof ModularGogglesItem modular) {
                            max = (float) modular.getBatteryCapacity(helmet);
                        } else {
                            max = (float) goggles.getBatteryCapacity();
                        }

                        if (!nbt.contains(ModConstants.TAG_BATTERY)) nbt.putFloat(ModConstants.TAG_BATTERY, max);
                        if (nbt.getFloat(ModConstants.TAG_BATTERY) <= 0) newState = false;
                    }
                }
                nbt.putBoolean(ModConstants.TAG_ACTIVE, newState);
            }
            player.containerMenu.broadcastChanges();
        });
    }
}