package dev.itsrealperson.vision_goggles.block.entity;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionModuleItem;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public class ModificationStationBlockEntity extends BlockEntity implements WorldlyContainer, dev.architectury.registry.menu.ExtendedMenuProvider {
    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    public ModificationStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODIFICATION_STATION_BE.get(), pos, state);
    }

    @Override
    public void saveExtraData(net.minecraft.network.FriendlyByteBuf buf) {
        // No extra data needed for now, but we must implement it for ExtendedMenuProvider
    }

    @Override
    public net.minecraft.network.chat.Component getDisplayName() {
        return net.minecraft.network.chat.Component.translatable("block.vision_goggles.modification_station");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new dev.itsrealperson.vision_goggles.menu.ModificationStationMenu(id, playerInventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ModificationStationBlockEntity entity) {
        if (level.isClientSide) return;

        ItemStack gogglesStack = entity.items.get(0);
        ItemStack moduleStack = entity.items.get(1);
        ItemStack outputStack = entity.items.get(2);

        // Optimization: Only process if output is empty and we have inputs
        if (outputStack.isEmpty()) {
            if (gogglesStack.getItem() instanceof ModularGogglesItem && !moduleStack.isEmpty()) {
                CompoundTag nbt = gogglesStack.getOrCreateTag();
                ListTag modules = nbt.getList("Modules", Tag.TAG_STRING);

                String newModuleId = null;
                boolean isBatteryExpansion = false;

                if (moduleStack.getItem() instanceof VisionModuleItem visionModule) {
                    newModuleId = visionModule.getVisionMode().name();
                } else if (moduleStack.getItem() == ModItems.BATTERY_EXPANSION_MODULE.get()) {
                    newModuleId = "BATTERY_EXPANSION";
                    isBatteryExpansion = true;
                } else if (moduleStack.getItem() == ModItems.ZOOM_MODULE.get()) {
                    newModuleId = "ZOOM";
                } else if (moduleStack.getItem() == ModItems.SOLAR_MODULE.get()) {
                    newModuleId = "SOLAR";
                } else if (moduleStack.getItem() == ModItems.SONAR_MODULE.get()) {
                    newModuleId = "SONAR";
                }

                if (newModuleId != null) {
                    boolean alreadyInstalled = false;
                    int visionModuleCount = 0;
                    int utilityModuleCount = 0;
                    boolean hasBatteryExpansion = false;

                    for (int i = 0; i < modules.size(); i++) {
                        String mod = modules.getString(i);
                        if (mod.equals(newModuleId)) alreadyInstalled = true;
                        if (mod.equals("BATTERY_EXPANSION")) hasBatteryExpansion = true;
                        else if (mod.equals("ZOOM") || mod.equals("SOLAR") || mod.equals("SONAR")) utilityModuleCount++;
                        else visionModuleCount++; 
                    }

                    boolean canInstall = !alreadyInstalled;
                    int maxTotal = ((ModularGogglesItem)gogglesStack.getItem()).getMaxModules();

                    if (isBatteryExpansion) {
                        if (hasBatteryExpansion) canInstall = false; 
                    } else {
                        // Total count of functional modules (Vision + Utility)
                        if ((visionModuleCount + utilityModuleCount) >= maxTotal) canInstall = false;
                    }

                    if (canInstall) {
                        ItemStack result = gogglesStack.copy();
                        result.setCount(1);
                        CompoundTag resultNbt = result.getOrCreateTag();
                        ListTag resultModules = resultNbt.getList("Modules", Tag.TAG_STRING);
                        resultModules.add(StringTag.valueOf(newModuleId));
                        resultNbt.put("Modules", resultModules);

                        entity.items.set(2, result);
                        entity.setChanged();
                    }
                }
            }
        } else {
            // Output is NOT empty, verify if it should still be there
            if (gogglesStack.isEmpty() || moduleStack.isEmpty()) {
                entity.items.set(2, ItemStack.EMPTY);
                entity.setChanged();
            }
        }
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            this.setChanged();
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        this.setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        ContainerHelper.loadAllItems(tag, items);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
    }

    // WorldlyContainer methods (simplification for now)
    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return new int[]{0, 1, 2};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) {
        return slot != 2;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) {
        return slot == 2;
    }
}
