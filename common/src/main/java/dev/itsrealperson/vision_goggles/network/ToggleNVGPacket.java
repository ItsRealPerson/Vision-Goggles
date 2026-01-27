package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import java.util.List;
import java.util.Objects;

public record ToggleNVGPacket(boolean switchMode) implements CustomPacketPayload {
    public static final Type<ToggleNVGPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "toggle_nvg"));

    public static final StreamCodec<FriendlyByteBuf, ToggleNVGPacket> CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeBoolean(packet.switchMode),
            buf -> new ToggleNVGPacket(buf.readBoolean())
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(NetworkManager.PacketContext context) {
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return;

            List<VisionMode> modes;
            if (goggles instanceof ModularGogglesItem modular) {
                modes = modular.getModes(helmet);
            } else {
                modes = goggles.getSupportedModes();
            }
            
            if (this.switchMode) {
                boolean isActive = Objects.requireNonNullElse(helmet.get(ModDataComponents.ACTIVE.get()), false);
                if (isActive) {
                    if (modes.size() > 1) {
                        int currentModeId = Objects.requireNonNullElse(helmet.get(ModDataComponents.MODE.get()), 0);
                        int index = -1;
                        for (int i = 0; i < modes.size(); i++) {
                            if (modes.get(i).getId() == currentModeId) {
                                index = i;
                                break;
                            }
                        }
                        int nextIndex = (index + 1) % modes.size();
                        helmet.set(ModDataComponents.MODE.get(), modes.get(nextIndex).getId());
                    }
                }
            } else {
                boolean currentState = Objects.requireNonNullElse(helmet.get(ModDataComponents.ACTIVE.get()), false);
                boolean newState = !currentState;
                
                if (newState) {
                    if (modes.isEmpty() && (goggles instanceof ModularGogglesItem modular && modular.getUtilityModules(helmet).isEmpty())) {
                        newState = false; 
                    } else {
                        if (!modes.isEmpty()) {
                            if (!helmet.has(ModDataComponents.MODE.get())) {
                                 helmet.set(ModDataComponents.MODE.get(), modes.get(0).getId());
                            }
                        } else {
                            helmet.set(ModDataComponents.MODE.get(), -1);
                        }

                        float currentBattery = Objects.requireNonNullElse(helmet.get(ModDataComponents.BATTERY.get()), 0.0f);
                        if (currentBattery <= 0) newState = false;
                    }
                }
                
                helmet.set(ModDataComponents.ACTIVE.get(), newState);
                
                if (!newState) {
                    VisionGogglesItem.cleanUpEffect(player);
                }
            }
        });
    }
}