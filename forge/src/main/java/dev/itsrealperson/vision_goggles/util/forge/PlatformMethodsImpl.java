package dev.itsrealperson.vision_goggles.util.forge;

import dev.itsrealperson.vision_goggles.registry.ModItems;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.inventory.IDynamicStackHandler;

import java.util.concurrent.atomic.AtomicBoolean;

import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;

public class PlatformMethodsImpl {
    public static boolean equipInSlot(Player player, ItemStack stack) {
        AtomicBoolean success = new AtomicBoolean(false);
        CuriosApi.getCuriosHelper().getCuriosHandler(player).ifPresent(handler -> {
            String[] possibleSlots = {"eyes", "eyewear"};
            // First pass: look for empty slot
            for (String slotId : possibleSlots) {
                handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                    IDynamicStackHandler dynamicHandler = stacksHandler.getStacks();
                    for (int i = 0; i < dynamicHandler.getSlots(); i++) {
                        if (dynamicHandler.getStackInSlot(i).isEmpty()) {
                            dynamicHandler.setStackInSlot(i, stack.copy());
                            stack.setCount(0);
                            success.set(true);
                            return;
                        }
                    }
                });
                if (success.get()) break;
            }
            
            // Second pass: try to swap
            if (!success.get()) {
                for (String slotId : possibleSlots) {
                    handler.getStacksHandler(slotId).ifPresent(stacksHandler -> {
                        IDynamicStackHandler dynamicHandler = stacksHandler.getStacks();
                        for (int i = 0; i < dynamicHandler.getSlots(); i++) {
                            ItemStack existing = dynamicHandler.getStackInSlot(i);
                            if (existing.getItem() instanceof VisionGogglesItem) {
                                ItemStack copy = stack.copy();
                                stack.setCount(0);
                                if (!player.addItem(existing)) {
                                    player.drop(existing, false);
                                }
                                dynamicHandler.setStackInSlot(i, copy);
                                success.set(true);
                                return;
                            }
                        }
                    });
                    if (success.get()) break;
                }
            }
        });
        return success.get();
    }

    public static ItemStack getEquippedHelmet(Player player) {
        return CuriosApi.getCuriosHelper().findFirstCurio(player, stack -> 
            stack.getItem() instanceof VisionGogglesItem
        ).map(slot -> slot.stack()).orElse(ItemStack.EMPTY);
    }
}

