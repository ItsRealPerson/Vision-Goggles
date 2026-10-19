package dev.itsrealperson.vision_goggles.recipe;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.registry.ModRecipes;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class GogglesUpgradeRecipe extends ShapedRecipe {
    public GogglesUpgradeRecipe(ShapedRecipe recipe) {
        super(recipe.getId(), recipe.getGroup(), recipe.category(), recipe.getWidth(), recipe.getHeight(), recipe.getIngredients(), recipe.getResultItem(null));
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registryAccess) {
        ItemStack result = super.assemble(container, registryAccess);
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty() && stack.getItem() instanceof ModularGogglesItem) {
                if (stack.hasTag()) {
                    result.setTag(stack.getTag().copy());
                }
                break;
            }
        }
        if (result.getItem() instanceof ModularGogglesItem modularItem) {
            if (!modularItem.hasBatteryExpansion(result)) {
                CompoundTag tag = result.getOrCreateTag();
                ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
                modules.add(StringTag.valueOf(ModConstants.ID_BATTERY_EXPANSION));
                tag.put(ModConstants.TAG_MODULES, modules);
            }
        }
        return result;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GOGGLES_UPGRADE_SERIALIZER.get();
    }
}
