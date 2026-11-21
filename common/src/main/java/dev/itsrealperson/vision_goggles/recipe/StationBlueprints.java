package dev.itsrealperson.vision_goggles.recipe;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Supplier;

/**
 * Exclusive blueprint recipes assembled inside the Modification Station.
 */
public final class StationBlueprints {

    public record IngredientReq(Supplier<Item> item, int count) {
        public ItemStack asStack() {
            return new ItemStack(item.get(), count);
        }
    }

    public record Blueprint(
            String id,
            Supplier<Item> result,
            String categoryKey,
            List<IngredientReq> ingredients,
            boolean isProUpgrade
    ) {
        public ItemStack createDefaultResult() {
            return new ItemStack(result.get());
        }
    }

    private static List<Blueprint> BLUEPRINTS;

    private StationBlueprints() {}

    public static List<Blueprint> getAll() {
        if (BLUEPRINTS == null) {
            BLUEPRINTS = List.of(
                    new Blueprint(
                            "night_vision_goggles",
                            ModItems.NIGHT_VISION_GOGGLES,
                            "gui.vision_goggles.category.specialized",
                            List.of(
                                    new IngredientReq(ModItems.TACTICAL_FRAME, 1),
                                    new IngredientReq(ModItems.NVG_MODULE, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1)
                            ),
                            false
                    ),
                    new Blueprint(
                            "thermal_goggles",
                            ModItems.THERMAL_GOGGLES,
                            "gui.vision_goggles.category.specialized",
                            List.of(
                                    new IngredientReq(ModItems.TACTICAL_FRAME, 1),
                                    new IngredientReq(ModItems.THERMAL_MODULE, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1)
                            ),
                            false
                    ),
                    new Blueprint(
                            "hydro_goggles",
                            ModItems.HYDRO_GOGGLES,
                            "gui.vision_goggles.category.specialized",
                            List.of(
                                    new IngredientReq(ModItems.TACTICAL_FRAME, 1),
                                    new IngredientReq(ModItems.HYDRO_MODULE, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1)
                            ),
                            false
                    ),
                    new Blueprint(
                            "biometric_goggles",
                            ModItems.BIOMETRIC_GOGGLES,
                            "gui.vision_goggles.category.specialized",
                            List.of(
                                    new IngredientReq(ModItems.TACTICAL_FRAME, 1),
                                    new IngredientReq(ModItems.BIO_MODULE, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1)
                            ),
                            false
                    ),
                    new Blueprint(
                            "modular_goggles",
                            ModItems.MODULAR_GOGGLES,
                            "gui.vision_goggles.category.modular",
                            List.of(
                                    new IngredientReq(ModItems.TACTICAL_FRAME, 2),
                                    new IngredientReq(ModItems.OPTICAL_LENS, 2),
                                    new IngredientReq(ModItems.TACTICAL_PROCESSOR, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1),
                                    new IngredientReq(ModItems.ELECTRICAL_PARTS, 2),
                                    new IngredientReq(() -> Items.GOLD_INGOT, 1)
                            ),
                            false
                    ),
                    new Blueprint(
                            "pro_modular_goggles",
                            ModItems.PRO_MODULAR_GOGGLES,
                            "gui.vision_goggles.category.modular",
                            List.of(
                                    new IngredientReq(ModItems.MODULAR_GOGGLES, 1),
                                    new IngredientReq(ModItems.TACTICAL_PROCESSOR, 2),
                                    new IngredientReq(() -> Items.NETHERITE_INGOT, 1),
                                    new IngredientReq(ModItems.ELECTRICAL_PARTS, 2),
                                    new IngredientReq(() -> Items.ECHO_SHARD, 2),
                                    new IngredientReq(ModItems.BATTERY_EXPANSION_MODULE, 1)
                            ),
                            true
                    ),
                    new Blueprint(
                            "tactical_flashlight",
                            ModItems.TACTICAL_FLASHLIGHT,
                            "gui.vision_goggles.category.tactical",
                            List.of(
                                    new IngredientReq(ModItems.FLASHLIGHT_MODULE, 1),
                                    new IngredientReq(ModItems.FLASHLIGHT_BODY, 1),
                                    new IngredientReq(ModItems.NVG_BATTERY, 1)
                            ),
                            false
                    )
            );
        }
        return BLUEPRINTS;
    }

    public static Blueprint byId(String id) {
        if (id == null || id.isEmpty()) return null;
        for (Blueprint bp : getAll()) {
            if (bp.id().equals(id)) {
                return bp;
            }
        }
        return null;
    }

    public static int countInList(List<ItemStack> list, Item target) {
        int total = 0;
        if (list == null) return 0;
        for (ItemStack s : list) {
            if (!s.isEmpty() && s.is(target)) {
                total += s.getCount();
            }
        }
        return total;
    }

    public static boolean areAllStaged(List<ItemStack> staged, Blueprint bp) {
        if (bp == null) return false;
        for (IngredientReq req : bp.ingredients()) {
            if (countInList(staged, req.item().get()) < req.count()) {
                return false;
            }
        }
        return true;
    }

    public static ItemStack buildResultFromStaged(List<ItemStack> staged, Blueprint bp) {
        if (bp == null) return ItemStack.EMPTY;
        ItemStack result = bp.createDefaultResult();
        if (bp.isProUpgrade() && staged != null) {
            CompoundTag preservedTag = null;
            for (ItemStack s : staged) {
                if (!s.isEmpty() && s.getItem() instanceof ModularGogglesItem && s.hasTag()) {
                    preservedTag = s.getTag().copy();
                    break;
                }
            }
            if (preservedTag != null) {
                result.setTag(preservedTag);
            }
            if (result.getItem() instanceof ModularGogglesItem modularItem && !modularItem.hasBatteryExpansion(result)) {
                CompoundTag tag = result.getOrCreateTag();
                ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
                modules.add(StringTag.valueOf(ModConstants.ID_BATTERY_EXPANSION));
                tag.put(ModConstants.TAG_MODULES, modules);
            }
        }
        return result;
    }

    public static int countAvailable(Player player, Container station, Item target) {
        int total = 0;
        if (player != null) {
            Inventory inv = player.getInventory();
            for (int i = 0; i < inv.getContainerSize(); i++) {
                ItemStack s = inv.getItem(i);
                if (!s.isEmpty() && s.is(target)) {
                    total += s.getCount();
                }
            }
        }
        if (station != null) {
            int limit = Math.min(2, station.getContainerSize());
            for (int i = 0; i < limit; i++) {
                ItemStack s = station.getItem(i);
                if (!s.isEmpty() && s.is(target)) {
                    total += s.getCount();
                }
            }
        }
        return total;
    }

    public static boolean canAssemble(Player player, Container station, Blueprint bp) {
        if (player == null || bp == null) return false;
        if (player.isCreative()) return true;
        for (IngredientReq req : bp.ingredients()) {
            if (countAvailable(player, station, req.item().get()) < req.count()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Server-side authoritative ingredient consumption and result construction.
     * Preserves NBT/modules when upgrading {@code modular_goggles} -> {@code pro_modular_goggles}.
     */
    public static ItemStack consumeAndBuildResult(Player player, Container station, Blueprint bp) {
        if (!canAssemble(player, station, bp)) {
            return ItemStack.EMPTY;
        }

        ItemStack result = bp.createDefaultResult();
        CompoundTag preservedTag = null;

        if (!player.isCreative()) {
            for (IngredientReq req : bp.ingredients()) {
                Item target = req.item().get();
                int remaining = req.count();

                // Check station input slots 0..1 first
                if (station != null) {
                    int limit = Math.min(2, station.getContainerSize());
                    for (int i = 0; i < limit && remaining > 0; i++) {
                        ItemStack s = station.getItem(i);
                        if (!s.isEmpty() && s.is(target)) {
                            if (bp.isProUpgrade() && preservedTag == null && s.getItem() instanceof ModularGogglesItem && s.hasTag()) {
                                preservedTag = s.getTag().copy();
                            }
                            int take = Math.min(remaining, s.getCount());
                            s.shrink(take);
                            if (s.isEmpty()) {
                                station.setItem(i, ItemStack.EMPTY);
                            }
                            remaining -= take;
                        }
                    }
                }

                // Then consume from player inventory
                Inventory inv = player.getInventory();
                for (int i = 0; i < inv.getContainerSize() && remaining > 0; i++) {
                    ItemStack s = inv.getItem(i);
                    if (!s.isEmpty() && s.is(target)) {
                        if (bp.isProUpgrade() && preservedTag == null && s.getItem() instanceof ModularGogglesItem && s.hasTag()) {
                            preservedTag = s.getTag().copy();
                        }
                        int take = Math.min(remaining, s.getCount());
                        s.shrink(take);
                        if (s.isEmpty()) {
                            inv.setItem(i, ItemStack.EMPTY);
                        }
                        remaining -= take;
                    }
                }
            }
        } else if (bp.isProUpgrade()) {
            // Even in Creative mode, preserve NBT if player carries modular goggles
            ItemStack found = findFirstStack(player, station, ModItems.MODULAR_GOGGLES.get());
            if (!found.isEmpty() && found.hasTag()) {
                preservedTag = found.getTag().copy();
            }
        }

        if (bp.isProUpgrade()) {
            if (preservedTag != null) {
                result.setTag(preservedTag);
            }
            if (result.getItem() instanceof ModularGogglesItem modularItem && !modularItem.hasBatteryExpansion(result)) {
                CompoundTag tag = result.getOrCreateTag();
                ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
                modules.add(StringTag.valueOf(ModConstants.ID_BATTERY_EXPANSION));
                tag.put(ModConstants.TAG_MODULES, modules);
            }
        }

        return result;
    }

    private static ItemStack findFirstStack(Player player, Container station, Item target) {
        if (station != null) {
            for (int i = 0; i < Math.min(2, station.getContainerSize()); i++) {
                ItemStack s = station.getItem(i);
                if (!s.isEmpty() && s.is(target)) return s;
            }
        }
        Inventory inv = player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack s = inv.getItem(i);
            if (!s.isEmpty() && s.is(target)) return s;
        }
        return ItemStack.EMPTY;
    }
}
