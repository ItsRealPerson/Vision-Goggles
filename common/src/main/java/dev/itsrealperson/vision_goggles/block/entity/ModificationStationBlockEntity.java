package dev.itsrealperson.vision_goggles.block.entity;

import dev.architectury.registry.menu.ExtendedMenuProvider;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import dev.itsrealperson.vision_goggles.menu.ModificationStationMenu;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ModificationStationBlockEntity extends BaseContainerBlockEntity implements ExtendedMenuProvider {
    private NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);

    public ModificationStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODIFICATION_STATION_BE.get(), pos, state);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.vision_goggles.modification_station");
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new ModificationStationMenu(containerId, inventory, this, ContainerLevelAccess.create(this.level, this.worldPosition));
    }

    @Override
    public void saveExtraData(net.minecraft.network.FriendlyByteBuf buf) {
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return this.items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, ModificationStationBlockEntity be) {
        if (level.isClientSide) return;

        ItemStack gogglesStack = be.items.get(0);
        ItemStack upgradeStack = be.items.get(1);
        ItemStack currentOutput = be.items.get(2);

        boolean changed = false;

        if (!gogglesStack.isEmpty() && gogglesStack.getItem() instanceof ModularGogglesItem goggles && !upgradeStack.isEmpty()) {
            
            // 1. Manejo de Batería (Upgrade independiente)
            if (upgradeStack.is(ModItems.BATTERY_EXPANSION_MODULE.get())) {
                boolean hasUpgrade = Objects.requireNonNullElse(gogglesStack.get(ModDataComponents.BATTERY_UPGRADE.get()), false);
                if (!hasUpgrade) {
                    if (currentOutput.isEmpty()) {
                        ItemStack result = gogglesStack.copy();
                        result.set(ModDataComponents.BATTERY_UPGRADE.get(), true);
                        be.items.set(2, result);
                        changed = true;
                    }
                }
            } else {
                // 2. Manejo de Módulos Normales
                String moduleName = getModuleNameFromUpgrade(upgradeStack);
                if (moduleName != null) {
                    List<String> modules = gogglesStack.getOrDefault(ModDataComponents.MODULES.get(), List.of());
                    
                    if (modules.size() < goggles.getMaxModules() && !modules.contains(moduleName)) {
                        if (currentOutput.isEmpty()) {
                            ItemStack result = gogglesStack.copy();
                            List<String> newModules = new ArrayList<>(modules);
                            newModules.add(moduleName);
                            result.set(ModDataComponents.MODULES.get(), newModules);
                            be.items.set(2, result);
                            changed = true;
                        }
                    }
                }
            }
        } else {
            // Limpiar salida si faltan inputs
            if (!currentOutput.isEmpty()) {
                be.items.set(2, ItemStack.EMPTY);
                changed = true;
            }
        }

        if (changed) {
            be.setChanged();
            level.sendBlockUpdated(pos, state, state, 3);
        }
    }

    private static String getModuleNameFromUpgrade(ItemStack stack) {
        if (stack.is(ModItems.NVG_MODULE.get())) return "NIGHT_VISION";
        if (stack.is(ModItems.THERMAL_MODULE.get())) return "THERMAL";
        if (stack.is(ModItems.HYDRO_MODULE.get())) return "HYDRO";
        if (stack.is(ModItems.BIO_MODULE.get())) return "BIOMETRIC";
        if (stack.is(ModItems.SOLAR_MODULE.get())) return "SOLAR";
        if (stack.is(ModItems.ZOOM_MODULE.get())) return "ZOOM";
        if (stack.is(ModItems.SONAR_MODULE.get())) return "SONAR";
        // Battery Expansion NO se devuelve como modulo normal para evitar que cuente en el limite
        return null;
    }

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemStack : items) {
            if (!itemStack.isEmpty()) return false;
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
            setChanged();
            if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
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
        if (stack.getCount() > getMaxStackSize()) stack.setCount(getMaxStackSize());
        setChanged();
        if (this.level != null) this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.items = NonNullList.withSize(this.getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, this.items, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, this.items, registries);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }
}
