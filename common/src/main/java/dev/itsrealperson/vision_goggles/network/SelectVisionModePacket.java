package dev.itsrealperson.vision_goggles.network;

import dev.architectury.networking.NetworkManager;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.function.Supplier;

public class SelectVisionModePacket {
    private final int modeId;

    public SelectVisionModePacket(int modeId) {
        this.modeId = modeId;
    }

    public SelectVisionModePacket(FriendlyByteBuf buf) {
        this.modeId = buf.readInt();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeInt(this.modeId);
    }

    public void handle(Supplier<NetworkManager.PacketContext> contextSupplier) {
        NetworkManager.PacketContext context = contextSupplier.get();
        context.queue(() -> {
            ServerPlayer player = (ServerPlayer) context.getPlayer();
            if (player == null) return;

            ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
            if (helmet.isEmpty() || !(helmet.getItem() instanceof VisionGogglesItem goggles)) return;

            CompoundTag nbt = helmet.getOrCreateTag();
            if (this.modeId < 0) {
                // Standby / Turn OFF
                nbt.putBoolean(ModConstants.TAG_ACTIVE, false);
                VisionGogglesItem.cleanUpEffect(player);
            } else {
                // Select specific mode and turn ON
                List<VisionMode> modes = (goggles instanceof ModularGogglesItem modular)
                        ? modular.getModes(helmet)
                        : goggles.getSupportedModes();

                boolean modeSupported = false;
                for (VisionMode m : modes) {
                    if (m.getId() == this.modeId) {
                        modeSupported = true;
                        break;
                    }
                }

                if (modeSupported) {
                    nbt.putInt(ModConstants.TAG_MODE, this.modeId);

                    float max = (goggles instanceof ModularGogglesItem modular)
                            ? (float) modular.getBatteryCapacity(helmet)
                            : (float) goggles.getBatteryCapacity();

                    if (!nbt.contains(ModConstants.TAG_BATTERY)) nbt.putFloat(ModConstants.TAG_BATTERY, max);

                    if (nbt.getFloat(ModConstants.TAG_BATTERY) > 0) {
                        nbt.putBoolean(ModConstants.TAG_ACTIVE, true);
                    }
                }
            }
            player.containerMenu.broadcastChanges();
        });
    }
}
