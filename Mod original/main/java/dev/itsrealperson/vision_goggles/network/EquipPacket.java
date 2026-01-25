package dev.itsrealperson.vision_goggles.network;

import dev.itsrealperson.vision_goggles.client.ClientEvents;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.function.Supplier;

public class EquipPacket {
    public EquipPacket() {}
    public EquipPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
            if (!stack.isEmpty() && ClientEvents.isVisor(stack.getItem())) {
                CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
                    String[] possibleSlots = {"eyes", "eyewear"};
                    for (String slotId : possibleSlots) {
                        handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                            IDynamicStackHandler dynamicHandler = stacksHandler.getStacks();
                            for (int i = 0; i < dynamicHandler.getSlots(); i++) {
                                if (dynamicHandler.getStackInSlot(i).isEmpty()) {
                                    dynamicHandler.setStackInSlot(i, stack.copy());
                                    stack.setCount(0);
                                    player.containerMenu.broadcastChanges();
                                    return;
                                }
                            }
                        });
                        if (stack.isEmpty()) break;
                    }
                });
            }
        });
        context.setPacketHandled(true);
    }
}
