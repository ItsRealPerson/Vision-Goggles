package dev.itsrealperson.vision_goggles.block.entity;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionModuleItem;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.VisionMode;
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

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            updateResult();
        }
    }

    private void updateResult() {
        ItemStack slot0 = this.items.get(0);
        ItemStack slot1 = this.items.get(1);
        ItemStack outputStack = this.items.get(2);

        // Reset output if inputs are invalid
        if (slot0.isEmpty() || slot1.isEmpty()) {
            if (!outputStack.isEmpty()) {
                this.items.set(2, ItemStack.EMPTY);
            }
            return;
        }

        // Only process if output is empty
        if (!outputStack.isEmpty()) return;

        // SCENARIO 1: Flashing / Programming a Programmable Module (works in either slot!)
        ItemStack chip = ItemStack.EMPTY;
        ItemStack script = ItemStack.EMPTY;
        if (slot0.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem && 
            slot1.getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem) {
            chip = slot0;
            script = slot1;
        } else if (slot1.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem && 
                   slot0.getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem) {
            chip = slot1;
            script = slot0;
        }

        if (!chip.isEmpty() && dev.itsrealperson.vision_goggles.item.InstallationScriptItem.hasScript(script)) {
            String scriptId = dev.itsrealperson.vision_goggles.item.InstallationScriptItem.getScriptId(script);
            String scriptName = dev.itsrealperson.vision_goggles.item.InstallationScriptItem.getScriptName(script);
            String scriptColor = dev.itsrealperson.vision_goggles.item.InstallationScriptItem.getScriptColor(script);

            ItemStack programmedResult = dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.createProgrammed(chip, scriptId, scriptName, scriptColor);
            this.items.set(2, programmedResult);
            return;
        }

        // SCENARIO 2: Installing Module into Modular Goggles (works in either slot!)
        ItemStack gogglesStack = ItemStack.EMPTY;
        ItemStack moduleStack = ItemStack.EMPTY;

        if (slot0.getItem() instanceof ModularGogglesItem) {
            gogglesStack = slot0;
            moduleStack = slot1;
        } else if (slot1.getItem() instanceof ModularGogglesItem) {
            gogglesStack = slot1;
            moduleStack = slot0;
        }

        if (!gogglesStack.isEmpty() && gogglesStack.getItem() instanceof ModularGogglesItem modularGoggles) {
            String newModuleId = getModuleIdFromStack(moduleStack);

            if (newModuleId != null && modularGoggles.canInstallModule(gogglesStack, newModuleId)) {
                // Compatibility check: If moduleStack is a programmed chip with a Lua script, check requirements!
                if (moduleStack.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem && 
                    dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.isProgrammed(moduleStack)) {
                    
                    String scriptId = dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.getProgrammedId(moduleStack);
                    dev.itsrealperson.vision_goggles.scripting.LuaCustomModule luaMod = 
                        dev.itsrealperson.vision_goggles.scripting.LuaModuleManager.findModuleByIdOrPath(scriptId);
                    
                    if (luaMod != null) {
                        String req = luaMod.getRequiredModule();
                        // If script specifies a required prerequisite module (e.g. bio, nvg, thermal) distinct from itself:
                        boolean isSelf = req != null && (req.equalsIgnoreCase(scriptId) || req.equalsIgnoreCase(newModuleId) 
                            || (luaMod.getId() != null && (req.equalsIgnoreCase(luaMod.getId().getPath()) || req.equalsIgnoreCase(luaMod.getId().toString()))));
                        
                        if (req != null && !req.isEmpty() && !isSelf) {
                            // Check if goggles already have this prerequisite hardware module installed!
                            if (!modularGoggles.hasModule(gogglesStack, req)) {
                                // Goggles do NOT have the required prerequisite hardware module!
                                this.items.set(2, ItemStack.EMPTY);
                                return;
                            }
                        }
                    }
                }

                ItemStack result = gogglesStack.copy();
                result.setCount(1);
                modularGoggles.installModule(result, newModuleId);

                // If installing a custom programmed module, record display name and color!
                if (moduleStack.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem && 
                    dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.isProgrammed(moduleStack)) {
                    String name = dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.getProgrammedName(moduleStack);
                    String color = dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.getProgrammedColor(moduleStack);
                    CompoundTag tag = result.getOrCreateTag();
                    tag.putString("mod_name_" + newModuleId, name);
                    tag.putString("mod_color_" + newModuleId, color);
                }

                this.items.set(2, result);
            }
        }
    }

    private static java.util.Map<net.minecraft.world.item.Item, String> MODULE_MAP = null;

    private String getModuleIdFromStack(ItemStack stack) {
        if (stack.getItem() instanceof VisionModuleItem visionModule) {
            return visionModule.getVisionMode().getModuleLocation().toString();
        }
        if (stack.getItem() instanceof dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem) {
            if (dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.isProgrammed(stack)) {
                return dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.getProgrammedId(stack);
            } else {
                return ModConstants.ID_PROGRAMMABLE;
            }
        }
        // Nota: InstallationScriptItem NO se instala directamente en las gafas.
        // Debe grabarse primero en un ProgrammableModuleItem para mantener la mecánica de hardware/software.
        if (MODULE_MAP == null) {
            MODULE_MAP = java.util.Map.of(
                ModItems.BATTERY_EXPANSION_MODULE.get(), ModConstants.ID_BATTERY_EXPANSION,
                ModItems.ZOOM_MODULE.get(), ModConstants.ID_ZOOM,
                ModItems.SOLAR_MODULE.get(), ModConstants.ID_SOLAR,
                ModItems.SONAR_MODULE.get(), ModConstants.ID_SONAR,
                ModItems.VITAL_INFO_MODULE.get(), ModConstants.ID_VITAL_INFO,
                ModItems.ENVIRONMENT_MODULE.get(), ModConstants.ID_ENVIRONMENT,
                ModItems.SPAWN_SECURITY_MODULE.get(), ModConstants.ID_SPAWN_SECURITY,
                ModItems.CHUNK_VIEWER_MODULE.get(), ModConstants.ID_CHUNK_VIEWER,
                ModItems.FLASHLIGHT_MODULE.get(), ModConstants.ID_FLASHLIGHT
            );
        }
        return MODULE_MAP.get(stack.getItem());
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
            // If the output was taken, consume inputs
            if (slot == 2) {
                if (!(this.items.get(0).getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem)) {
                    this.items.get(0).shrink(1);
                }
                if (!(this.items.get(1).getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem)) {
                    this.items.get(1).shrink(1);
                }
            }
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
