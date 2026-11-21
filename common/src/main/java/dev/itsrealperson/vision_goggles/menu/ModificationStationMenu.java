package dev.itsrealperson.vision_goggles.menu;

import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import dev.itsrealperson.vision_goggles.registry.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class ModificationStationMenu extends AbstractContainerMenu {
    public static final int TAB_ASSEMBLE = 0;
    public static final int TAB_MODIFY = 1;

    private final Container container;
    private final ContainerLevelAccess access;
    private final BlockPos stationPos;
    private final ContainerData data;
    private int currentTab = TAB_ASSEMBLE;

    // Client-side constructor
    public ModificationStationMenu(int id, Inventory playerInventory, FriendlyByteBuf extraData) {
        this(id, playerInventory, new SimpleContainer(3), ContainerLevelAccess.NULL,
                (extraData != null && extraData.readableBytes() >= 8) ? extraData.readBlockPos() : BlockPos.ZERO,
                new SimpleContainerData(2));
    }

    // Server-side constructor
    public ModificationStationMenu(int id, Inventory playerInventory, Container container, ContainerLevelAccess access) {
        this(id, playerInventory, container, access,
                (container instanceof ModificationStationBlockEntity station) ? station.getBlockPos() : BlockPos.ZERO,
                createServerData(container));
    }

    private static ContainerData createServerData(Container container) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                if (index == 1 && container instanceof ModificationStationBlockEntity station) {
                    return station.getAssemblingTicks();
                }
                return 0;
            }

            @Override
            public void set(int index, int value) {
                // tab is tracked per-menu via clickMenuButton & synced via DataSlot
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    private ModificationStationMenu(int id, Inventory playerInventory, Container container, ContainerLevelAccess access, BlockPos stationPos, ContainerData data) {
        super(ModMenus.MODIFICATION_STATION_MENU.get(), id);
        this.container = container;
        this.access = access;
        this.stationPos = stationPos;
        this.data = data;
        checkContainerSize(container, 3);
        checkContainerDataCount(data, 2);
        container.startOpen(playerInventory.player);

        this.addDataSlots(data);
        this.addDataSlot(new net.minecraft.world.inventory.DataSlot() {
            @Override
            public int get() {
                return currentTab;
            }

            @Override
            public void set(int value) {
                currentTab = (value == TAB_MODIFY) ? TAB_MODIFY : TAB_ASSEMBLE;
            }
        });

        // Station Slots (only active on TAB_MODIFY)
        // Input 0: Goggles
        this.addSlot(new Slot(container, 0, 56, 39) {
            @Override
            public boolean isActive() {
                return currentTab == TAB_MODIFY;
            }
        });
        // Input 1: Module
        this.addSlot(new Slot(container, 1, 92, 39) {
            @Override
            public boolean isActive() {
                return currentTab == TAB_MODIFY;
            }
        });
        // Output 2
        this.addSlot(new Slot(container, 2, 128, 39) {
            @Override
            public boolean isActive() {
                return currentTab == TAB_MODIFY;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }

            @Override
            public void onTake(Player player, ItemStack stack) {
                if (container instanceof ModificationStationBlockEntity station) {
                    station.onTakeOutput(player);
                }
                super.onTake(player, stack);
            }
        });

        // Player Inventory (centered for 220x196 tactical GUI)
        int invOffsetX = 30;
        int invOffsetY = 114;
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, invOffsetX + j * 18, invOffsetY + i * 18));
            }
        }

        // Hotbar
        for (int k = 0; k < 9; ++k) {
            this.addSlot(new Slot(playerInventory, k, invOffsetX + k * 18, invOffsetY + 58));
        }
    }

    public BlockPos getStationPos() {
        return this.stationPos;
    }

    public Container getStationContainer() {
        return this.container;
    }

    public int getCurrentTab() {
        return this.currentTab;
    }

    public int getAssemblingTicks() {
        return this.data.get(1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == TAB_ASSEMBLE || id == TAB_MODIFY) {
            this.currentTab = id;
            return true;
        }
        return false;
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
                slot.onTake(player, itemstack);
                return itemstack;
            } else if (index < 3) { // From Station Inputs to Player
                if (!this.moveItemStackTo(itemstack1, 3, 39, false)) {
                    return ItemStack.EMPTY;
                }
            } else { // From Player to Station (only if Modify tab is active)
                if (this.currentTab == TAB_MODIFY && this.moveItemStackTo(itemstack1, 0, 2, false)) {
                    // moved to station input
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
