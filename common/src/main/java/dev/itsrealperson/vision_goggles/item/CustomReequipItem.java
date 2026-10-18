package dev.itsrealperson.vision_goggles.item;

import net.minecraft.world.item.ItemStack;

public interface CustomReequipItem {
    /**
     * Determines whether changing an ItemStack to a new ItemStack should trigger
     * the first-person hand re-equip animation (bobbing / lowering and raising).
     *
     * In Forge, this matches the exact signature of {@code IForgeItem#shouldCauseReequipAnimation}.
     * In Fabric, our ItemInHandRendererMixin delegates to this method.
     *
     * @param oldStack The previous ItemStack in hand.
     * @param newStack The incoming ItemStack in hand.
     * @param slotChanged True if the hotbar slot index changed, false if in the same slot.
     * @return True to play the re-equip animation, false to keep the hand steady.
     */
    default boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        if (slotChanged) return true;
        return !ItemStack.isSameItem(oldStack, newStack);
    }
}
