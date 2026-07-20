package dev.itsrealperson.vision_goggles.menu;

import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import dev.itsrealperson.vision_goggles.registry.ModMenus;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModificationStationMenu extends AbstractContainerMenu {
    private final Container container;
    private final ContainerLevelAccess access;

    // Client-side constructor
    public ModificationStationMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new SimpleContainer(3), ContainerLevelAccess.NULL);
    }

    // Server-side constructor
    public ModificationStationMenu(int id, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        super(ModMenus.MODIFICATION_STATION_MENU.get(), id);
        this.container = container;
        this.access = access;
        checkContainerSize(container, 3);
        container.startOpen(playerInventory.player);

        // Station Slots
        // Input 0: Goggles
        this.addSlot(new Slot(container, 0, 44, 35));
        // Input 1: Module
        this.addSlot(new Slot(container, 1, 80, 35));
        // Output 2
        this.addSlot(new Slot(container, 2, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                super.onTake(player, stack);
            }
        });

        // Player Inventory
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, 8 + k * 18, 142));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack itemstack1 = slot.getItem();
            itemstack = itemstack1.copy();
            if (index == 2) { // From Station Output to Player
                if (!this.moveItemStackTo(itemstack1, 3, 39, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickCraft(itemstack1, itemstack);
                // Consume inputs because moveItemStackTo bypasses removeItem
                this.container.removeItem(0, 1);
                this.container.removeItem(1, 1);
            } else if (index < 3) { // From Station Inputs to Player
                if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Station
                if (this.moveItemStackTo(itemstack1, 0, 2, false)) { // Try inputs only
                    // success
                } else if (index < 30) {
                    if (!this.moveItemStackTo(itemstack1, 30, 39, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (index < 39 && !this.moveItemStackTo(itemstack1, 3, 30, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (itemstack1.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }

            if (itemstack1.getCount() == itemstack.getCount()) {
                return ItemStack.EMPTY;
            }

            slot.onTake(player, itemstack1);
        }

        return itemstack;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, ModBlocks.MODIFICATION_STATION.get());
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.container.stopOpen(player);
    }
}
