package dev.itsrealperson.vision_goggles.network;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import dev.itsrealperson.vision_goggles.util.Config;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import top.theillusivec4.curios.api.CuriosApi;

import java.util.Optional;
import java.util.function.Supplier;

public class BatteryPacket {
    public BatteryPacket() {}
    public BatteryPacket(FriendlyByteBuf buf) {}
    public void encode(FriendlyByteBuf buf) {}

    public void handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;

            ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
            boolean isVisor = helmet.getItem() instanceof dev.itsrealperson.vision_goggles.common.VisionGogglesItem;
            
            if (!isVisor) {

                Optional<ItemStack> curio = CuriosApi.getCuriosHelper().findFirstCurio(player, 
                    stack -> stack.getItem() instanceof dev.itsrealperson.vision_goggles.common.VisionGogglesItem)
                    .map(slot -> slot.stack());
                
                if (curio.isPresent()) {
                    helmet = curio.get();
                } else {
                    return;
                }
            }

            CompoundTag nbt = helmet.getOrCreateTag();
            float max = (helmet.getItem() == VisionGoggles.THERMAL_GOGGLES_HELMET.get()) ? (float)Config.SERVER.thermalDurationTicks.get() : (float)Config.SERVER.nvgDurationTicks.get();
            float current = nbt.getFloat("nvg_battery");
            
            if (current < max) {
                boolean batteryFound = false;
                float chargeAmount = 0.0f;
                ItemStack batteryStack = ItemStack.EMPTY;
                
                ItemStack handStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                if (dev.itsrealperson.vision_goggles.common.BatteryItem.isBattery(handStack)) {
                    batteryStack = handStack;
                    chargeAmount = Config.getBatteryCharge(handStack);
                    batteryFound = true;
                } else {
                    ItemStack offHandStack = player.getItemInHand(InteractionHand.OFF_HAND);
                    if (dev.itsrealperson.vision_goggles.common.BatteryItem.isBattery(offHandStack)) {
                        batteryStack = offHandStack;
                        chargeAmount = Config.getBatteryCharge(offHandStack);
                        batteryFound = true;
                    }
                }

                if (batteryFound && !batteryStack.isEmpty()) {
                    if (!player.getAbilities().instabuild) {
                        batteryStack.shrink(1);
                    }
                    
                    float news = Math.min(max, current + (max * chargeAmount));
                    nbt.putFloat("nvg_battery", news);
                    player.containerMenu.broadcastChanges();
                }
            }
        });
        context.setPacketHandled(true);
    }
}