package dev.itsrealperson.vision_goggles.util.fabric;

import dev.itsrealperson.vision_goggles.registry.ModItems;
import io.wispforest.accessories.api.AccessoriesCapability;
import io.wispforest.accessories.api.slot.SlotEntryReference;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class PlatformMethodsImpl {
    public static boolean equipInSlot(Player player, ItemStack stack) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability != null) {
            String[] possibleSlots = {"face", "eyes", "eyewear", "head"};
            for (String slotId : possibleSlots) {
                var container = capability.getContainers().get(slotId);
                if (container != null) {
                    for (int i = 0; i < container.getSize(); i++) {
                        if (container.getAccessories().getItem(i).isEmpty()) {
                            container.getAccessories().setItem(i, stack.copy());
                            stack.setCount(0);
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static ItemStack getEquippedHelmet(Player player) {
        AccessoriesCapability capability = AccessoriesCapability.get(player);
        if (capability != null) {
            String[] possibleSlots = {"face", "eyes", "eyewear", "head"};
            for (String slotId : possibleSlots) {
                var container = capability.getContainers().get(slotId);
                if (container != null) {
                    for (int i = 0; i < container.getSize(); i++) {
                        ItemStack stack = container.getAccessories().getItem(i);
                        if (stack.getItem() == ModItems.NIGHT_VISION_GOGGLES.get() || 
                            stack.getItem() == ModItems.THERMAL_GOGGLES.get()) {
                            return stack;
                        }
                    }
                }
            }
        }
        return ItemStack.EMPTY;
    }
}

