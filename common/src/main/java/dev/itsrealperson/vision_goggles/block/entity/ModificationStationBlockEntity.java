package dev.itsrealperson.vision_goggles.block.entity;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionModuleItem;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import dev.itsrealperson.vision_goggles.registry.ModBlockEntities;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.registry.ModSounds;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class ModificationStationBlockEntity extends BlockEntity implements WorldlyContainer, dev.architectury.registry.menu.ExtendedMenuProvider {
    public static final int ASSEMBLY_DURATION_TICKS = 48;

    private final NonNullList<ItemStack> items = NonNullList.withSize(3, ItemStack.EMPTY);
    private String activeBlueprintId = "night_vision_goggles";
    private final List<ItemStack> stagedItems = new ArrayList<>();
    private final List<ItemStack> assemblingParts = new ArrayList<>();
    private int assemblingTicks = 0;
    private ItemStack assemblingResult = ItemStack.EMPTY;
    @Nullable
    private UUID assemblingPlayerUuid = null;

    public ModificationStationBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MODIFICATION_STATION_BE.get(), pos, state);
    }

    @Override
    public void saveExtraData(net.minecraft.network.FriendlyByteBuf buf) {
        buf.writeBlockPos(this.worldPosition);
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
            syncToClient();
        }
    }

    public void syncToClient() {
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.worldPosition, this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public boolean isAssembling() {
        return this.assemblingTicks > 0 && !this.assemblingResult.isEmpty();
    }

    public int getAssemblingTicks() {
        return this.assemblingTicks;
    }

    public ItemStack getAssemblingResult() {
        return this.assemblingResult;
    }

    @Nullable
    public UUID getAssemblingPlayerUuid() {
        return this.assemblingPlayerUuid;
    }

    public String getActiveBlueprintId() {
        return this.activeBlueprintId;
    }

    public List<ItemStack> getStagedItems() {
        return Collections.unmodifiableList(this.stagedItems);
    }

    public List<ItemStack> getAssemblingParts() {
        return Collections.unmodifiableList(this.assemblingParts);
    }

    public static boolean isCreativeStaged(ItemStack stack) {
        return !stack.isEmpty() && stack.hasTag() && stack.getTag().getBoolean("vg_creative_staged");
    }

    public void clearStagedItems() {
        this.stagedItems.clear();
        this.stagedOwnerUuid = null;
    }

    private UUID stagedOwnerUuid = null;

    public void selectBlueprint(ServerPlayer player, String blueprintId) {
        if (this.level == null || this.level.isClientSide || player == null || isAssembling()) return;
        StationBlueprints.Blueprint bp = StationBlueprints.byId(blueprintId);
        if (bp == null) return;
        if (!bp.id().equals(this.activeBlueprintId)) {
            if (!this.stagedItems.isEmpty() && this.stagedOwnerUuid != null && !this.stagedOwnerUuid.equals(player.getUUID())) {
                return;
            }
            refundStagedItems(player);
            this.activeBlueprintId = bp.id();
            super.setChanged();
            syncToClient();
        }
    }

    public void addIngredient(ServerPlayer player, int reqIndex) {
        if (this.level == null || this.level.isClientSide || player == null || isAssembling()) return;
        StationBlueprints.Blueprint bp = StationBlueprints.byId(this.activeBlueprintId);
        if (bp == null || reqIndex < 0 || reqIndex >= bp.ingredients().size()) return;

        StationBlueprints.IngredientReq req = bp.ingredients().get(reqIndex);
        Item target = req.item().get();
        int alreadyStaged = StationBlueprints.countInList(this.stagedItems, target);
        if (alreadyStaged >= req.count()) return;

        ItemStack placed = ItemStack.EMPTY;
        if (player.isCreative()) {
            placed = new ItemStack(target, 1);
            placed.getOrCreateTag().putBoolean("vg_creative_staged", true);
        } else {
            // Take 1 matching item from station slots 0..1 or player inventory
            for (int i = 0; i < 2; i++) {
                ItemStack s = this.items.get(i);
                if (!s.isEmpty() && s.is(target)) {
                    placed = s.split(1);
                    break;
                }
            }
            if (placed.isEmpty()) {
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize(); i++) {
                    ItemStack s = inv.getItem(i);
                    if (!s.isEmpty() && s.is(target)) {
                        placed = s.split(1);
                        break;
                    }
                }
            }
        }

        if (placed.isEmpty()) return;

        if (this.stagedItems.isEmpty()) {
            this.stagedOwnerUuid = player.getUUID();
        }
        this.stagedItems.add(placed);
        this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.75F, 1.15F);
        player.inventoryMenu.broadcastChanges();
        this.setChanged();
    }

    public void removeIngredient(ServerPlayer player, int reqIndex) {
        if (this.level == null || this.level.isClientSide || player == null || isAssembling()) return;
        StationBlueprints.Blueprint bp = StationBlueprints.byId(this.activeBlueprintId);
        if (bp == null || reqIndex < 0 || reqIndex >= bp.ingredients().size()) return;

        Item target = bp.ingredients().get(reqIndex).item().get();
        for (int i = this.stagedItems.size() - 1; i >= 0; i--) {
            ItemStack s = this.stagedItems.get(i);
            if (!s.isEmpty() && s.is(target)) {
                ItemStack removed = this.stagedItems.remove(i);
                if (this.stagedItems.isEmpty()) {
                    this.stagedOwnerUuid = null;
                }
                if (!isCreativeStaged(removed)) {
                    player.getInventory().placeItemBackInInventory(removed);
                    player.inventoryMenu.broadcastChanges();
                }
                this.level.playSound(null, this.worldPosition, SoundEvents.ITEM_FRAME_REMOVE_ITEM, SoundSource.BLOCKS, 0.75F, 1.05F);
                super.setChanged();
                syncToClient();
                return;
            }
        }
    }

    public void finalizeAssembly(ServerPlayer player) {
        if (this.level == null || this.level.isClientSide || player == null || isAssembling()) return;
        StationBlueprints.Blueprint bp = StationBlueprints.byId(this.activeBlueprintId);
        if (bp == null || !StationBlueprints.areAllStaged(this.stagedItems, bp)) return;

        ItemStack built = StationBlueprints.buildResultFromStaged(this.stagedItems, bp);
        if (built.isEmpty()) return;
        if (built.hasTag() && built.getTag().contains("vg_creative_staged")) {
            built.getTag().remove("vg_creative_staged");
            if (built.getTag().isEmpty()) {
                built.setTag(null);
            }
        }

        this.assemblingParts.clear();
        for (ItemStack s : this.stagedItems) {
            if (!s.isEmpty()) {
                this.assemblingParts.add(s.copy());
            }
        }
        this.stagedItems.clear();
        this.stagedOwnerUuid = null;
        this.assemblingResult = built;
        this.assemblingTicks = ASSEMBLY_DURATION_TICKS;
        this.assemblingPlayerUuid = player.getUUID();

        this.level.playSound(null, this.worldPosition, SoundEvents.ARMOR_EQUIP_IRON, SoundSource.BLOCKS, 0.80F, 1.15F);
        super.setChanged();
        syncToClient();
    }

    private void refundStagedItems(ServerPlayer player) {
        if (this.stagedItems.isEmpty()) return;
        for (ItemStack s : this.stagedItems) {
            if (!s.isEmpty() && !isCreativeStaged(s)) {
                player.getInventory().placeItemBackInInventory(s);
            }
        }
        this.stagedItems.clear();
        this.stagedOwnerUuid = null;
        player.inventoryMenu.broadcastChanges();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, ModificationStationBlockEntity station) {
        if (level.isClientSide || !station.isAssembling()) return;

        station.assemblingTicks--;

        if (station.assemblingTicks == 36 || station.assemblingTicks == 26 || station.assemblingTicks == 16 || station.assemblingTicks == 8) {
            float pitch = 1.20F + (48 - station.assemblingTicks) * 0.01F;
            level.playSound(null, pos, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 0.40F, pitch);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.01, pos.getZ() + 0.5, 4, 0.10, 0.04, 0.10, 0.02);
            }
        }

        if (station.assemblingTicks <= 0) {
            ItemStack finished = station.assemblingResult.copy();
            Player owner = (station.assemblingPlayerUuid != null) ? level.getPlayerByUUID(station.assemblingPlayerUuid) : null;
            if (owner instanceof ServerPlayer serverOwner && serverOwner.isAlive() && serverOwner.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64.0) {
                serverOwner.getInventory().placeItemBackInInventory(finished);
            } else {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5, finished);
            }

            level.playSound(null, pos, ModSounds.VISOR_ON.get(), SoundSource.BLOCKS, 0.70F, 1.10F);
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.30F, 1.55F);
            if (level instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.getX() + 0.5, pos.getY() + 1.02, pos.getZ() + 0.5, 10, 0.18, 0.06, 0.18, 0.04);
            }

            station.assemblingResult = ItemStack.EMPTY;
            station.assemblingParts.clear();
            station.assemblingPlayerUuid = null;
            station.assemblingTicks = 0;
            station.setChanged();
        } else {
            station.syncToClient();
        }
    }

    private void updateResult() {
        ItemStack slot0 = this.items.get(0);
        ItemStack slot1 = this.items.get(1);

        // Reset output if slot 0 is completely empty
        if (slot0.isEmpty()) {
            this.items.set(2, ItemStack.EMPTY);
            return;
        }

        // SCENARIO 0: Module Extraction / Dismantling (Goggles in Slot 0, Slot 1 is empty)
        if (slot1.isEmpty()) {
            if (slot0.getItem() instanceof ModularGogglesItem modular) {
                String lastMod = modular.getLastInstalledModule(slot0);
                if (lastMod != null) {
                    ItemStack extracted = ModularGogglesItem.createModuleItemFromId(lastMod, slot0.getTag());
                    this.items.set(2, extracted);
                    return;
                }
            }
            this.items.set(2, ItemStack.EMPTY);
            return;
        }

        ItemStack newOutput = ItemStack.EMPTY;

        // SCENARIO 1: Tactical Durability Repair (Goggles + Tactical Frame or Electrical Parts)
        ItemStack repairGoggles = ItemStack.EMPTY;
        ItemStack repairMaterial = ItemStack.EMPTY;
        if (slot0.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
            repairGoggles = slot0;
            repairMaterial = slot1;
        } else if (slot1.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
            repairGoggles = slot1;
            repairMaterial = slot0;
        }

        if (!repairGoggles.isEmpty() && repairGoggles.isDamaged()) {
            if (repairMaterial.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.TACTICAL_FRAME.get()) {
                ItemStack repaired = repairGoggles.copy();
                repaired.setDamageValue(0);
                newOutput = repaired;
            } else if (repairMaterial.getItem() == dev.itsrealperson.vision_goggles.registry.ModItems.ELECTRICAL_PARTS.get()) {
                ItemStack repaired = repairGoggles.copy();
                int repairAmt = Math.max(1, (int)(repaired.getMaxDamage() * 0.35f));
                repaired.setDamageValue(Math.max(0, repaired.getDamageValue() - repairAmt));
                newOutput = repaired;
            }
        }

        // SCENARIO 2: Flashing / Programming a Programmable Module (works in either slot!)
        if (newOutput.isEmpty()) {
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

                newOutput = dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem.createProgrammed(chip, scriptId, scriptName, scriptColor);
            }
        }

        // SCENARIO 3: Installing Module into Modular Goggles (works in either slot!)
        if (newOutput.isEmpty()) {
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
                    boolean allowed = true;
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
                                    allowed = false;
                                }
                            }
                        }
                    }

                    if (allowed) {
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

                        newOutput = result;
                    }
                }
            }
        }

        this.items.set(2, newOutput);
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

    public void onTakeOutput(Player player) {
        ItemStack slot0 = this.items.get(0);
        ItemStack slot1 = this.items.get(1);

        // Extracción de módulo: slot 1 vacío, remover módulo de las gafas en slot 0 sin destruir las gafas
        if (slot1.isEmpty() && slot0.getItem() instanceof ModularGogglesItem modular) {
            String lastMod = modular.getLastInstalledModule(slot0);
            if (lastMod != null) {
                modular.removeModule(slot0, lastMod);
            }
        } else {
            // Ensamblaje, reparación o grabado de firmware: consumir entradas correspondientes
            if (!slot0.isEmpty() && !(slot0.getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem)) {
                slot0.shrink(1);
            }
            if (!slot1.isEmpty() && !(slot1.getItem() instanceof dev.itsrealperson.vision_goggles.item.InstallationScriptItem)) {
                slot1.shrink(1);
            }
        }
        this.setChanged();
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack result = ContainerHelper.removeItem(items, slot, amount);
        if (!result.isEmpty()) {
            if (slot != 2) {
                this.setChanged();
            }
        }
        return result;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack result = ContainerHelper.takeItem(items, slot);
        this.setChanged();
        return result;
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
        this.items.clear();
        ContainerHelper.loadAllItems(tag, items);
        if (tag.contains("ActiveBlueprint", Tag.TAG_STRING)) {
            this.activeBlueprintId = tag.getString("ActiveBlueprint");
        }
        this.stagedItems.clear();
        if (tag.contains("StagedItems", Tag.TAG_LIST)) {
            ListTag list = tag.getList("StagedItems", Tag.TAG_COMPOUND);
            for (int i = 0; i < list.size(); i++) {
                ItemStack s = ItemStack.of(list.getCompound(i));
                if (!s.isEmpty()) {
                    this.stagedItems.add(s);
                }
            }
        }
        this.assemblingParts.clear();
        if (tag.contains("AssemblingParts", Tag.TAG_LIST)) {
            ListTag partList = tag.getList("AssemblingParts", Tag.TAG_COMPOUND);
            for (int i = 0; i < partList.size(); i++) {
                ItemStack s = ItemStack.of(partList.getCompound(i));
                if (!s.isEmpty()) {
                    this.assemblingParts.add(s);
                }
            }
        }
        this.assemblingTicks = tag.getInt("AssemblingTicks");
        if (tag.contains("AssemblingResult", Tag.TAG_COMPOUND)) {
            this.assemblingResult = ItemStack.of(tag.getCompound("AssemblingResult"));
        } else {
            this.assemblingResult = ItemStack.EMPTY;
        }
        if (tag.hasUUID("AssemblingPlayer")) {
            this.assemblingPlayerUuid = tag.getUUID("AssemblingPlayer");
        } else {
            this.assemblingPlayerUuid = null;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        ContainerHelper.saveAllItems(tag, items);
        tag.putString("ActiveBlueprint", this.activeBlueprintId != null ? this.activeBlueprintId : "");
        ListTag list = new ListTag();
        for (ItemStack s : this.stagedItems) {
            if (!s.isEmpty()) {
                list.add(s.save(new CompoundTag()));
            }
        }
        tag.put("StagedItems", list);
        ListTag partList = new ListTag();
        for (ItemStack s : this.assemblingParts) {
            if (!s.isEmpty()) {
                partList.add(s.save(new CompoundTag()));
            }
        }
        tag.put("AssemblingParts", partList);
        tag.putInt("AssemblingTicks", this.assemblingTicks);
        if (!this.assemblingResult.isEmpty()) {
            tag.put("AssemblingResult", this.assemblingResult.save(new CompoundTag()));
        }
        if (this.assemblingPlayerUuid != null) {
            tag.putUUID("AssemblingPlayer", this.assemblingPlayerUuid);
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    // WorldlyContainer methods
    @Override
    public int[] getSlotsForFace(net.minecraft.core.Direction side) {
        return new int[]{0, 1};
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) {
        return slot != 2;
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction dir) {
        if (slot == 2) {
            return false;
        }
        return true;
    }
}
