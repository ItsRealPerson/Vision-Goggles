package dev.itsrealperson.vision_goggles.compat.jei;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

public class ModificationStationRecipeCategory implements IRecipeCategory<ModificationStationJeiRecipe> {
    public static final ResourceLocation GUI_TEXTURE = new ResourceLocation(Vision_goggles.MOD_ID, "textures/gui/modification_station.png");

    private final IDrawable background;
    private final IDrawable icon;

    public ModificationStationRecipeCategory(IGuiHelper helper) {
        // Crop the 3-slot interaction strip from modification_station.png (u=38, v=29, w=100, h=30)
        this.background = helper.createDrawable(GUI_TEXTURE, 38, 29, 100, 30);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MODIFICATION_STATION.get()));
    }

    @Override
    public RecipeType<ModificationStationJeiRecipe> getRecipeType() {
        return VisionGogglesJeiPluginCommon.MODIFICATION_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("block.vision_goggles.modification_station");
    }

    @Override
    public IDrawable getBackground() {
        return this.background;
    }

    @Override
    public IDrawable getIcon() {
        return this.icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, ModificationStationJeiRecipe recipe, IFocusGroup focuses) {
        // Slot 0: Base wearable / blank chip (44 - 38 = 6, 35 - 29 = 6)
        builder.addSlot(RecipeIngredientRole.INPUT, 6, 6)
               .addItemStacks(recipe.baseInputs());

        // Slot 1: Module / installation script (80 - 38 = 42, 35 - 29 = 6)
        builder.addSlot(RecipeIngredientRole.INPUT, 42, 6)
               .addItemStacks(recipe.moduleInputs());

        // Slot 2: Output with NBT and active module list (116 - 38 = 78, 35 - 29 = 6)
        builder.addSlot(RecipeIngredientRole.OUTPUT, 78, 6)
               .addItemStack(recipe.output());
    }
}
