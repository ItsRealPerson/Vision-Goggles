package dev.itsrealperson.vision_goggles.network;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.client.ClientEvents;
import dev.itsrealperson.vision_goggles.util.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;
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

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;


            Optional<ItemStack> curio = CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> ClientEvents.isVisor(stack.getItem())).map(slot -> slot.stack());
            
            if (curio.isEmpty()) return;

            ItemStack helmet = curio.get();
            CompoundTag nbt = helmet.getOrCreateTag();
            
            if (this.switchMode) {
                if (helmet.getItem().toString().contains("thermal") && nbt.getBoolean("nvg_active")) {
                    int mode = nbt.getInt("vision_mode");
                    nbt.putInt("vision_mode", mode == 0 ? 1 : 0);
                }
            } else {
                boolean newState = !nbt.getBoolean("nvg_active");
                if (newState) {
                    float max = (helmet.getItem().toString().contains("thermal")) ? (float)Config.thermalDuration : (float)Config.nvgDuration;
                    if (!nbt.contains("nvg_battery")) nbt.putFloat("nvg_battery", max);
                    if (nbt.getFloat("nvg_battery") <= 0) newState = false;
                }
                nbt.putBoolean("nvg_active", newState);
            }
            player.containerMenu.broadcastChanges();
        });
        context.setPacketHandled(true);
    }
}
