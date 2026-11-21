package dev.itsrealperson.vision_goggles.compat.jei;

import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.ingredient.IRecipeSlotsView;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public class StationAssemblyCategory implements IRecipeCategory<StationBlueprints.Blueprint> {

    private final IDrawable background;
    private final IDrawable icon;

    public StationAssemblyCategory(IGuiHelper helper) {
        this.background = helper.createBlankDrawable(150, 44);
        this.icon = helper.createDrawableIngredient(VanillaTypes.ITEM_STACK, new ItemStack(ModBlocks.MODIFICATION_STATION.get()));
    }

    @Override
    public RecipeType<StationBlueprints.Blueprint> getRecipeType() {
        return VisionGogglesJeiPluginCommon.ASSEMBLY_TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.vision_goggles.station_assembly");
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
    public void setRecipe(IRecipeLayoutBuilder builder, StationBlueprints.Blueprint recipe, IFocusGroup focuses) {
        List<StationBlueprints.IngredientReq> reqs = recipe.ingredients();
        for (int i = 0; i < reqs.size(); i++) {
            int col = i % 3;
            int row = i / 3;
            builder.addSlot(RecipeIngredientRole.INPUT, 6 + col * 20, 4 + row * 20)
                    .addItemStack(reqs.get(i).asStack());
        }
        builder.addSlot(RecipeIngredientRole.OUTPUT, 120, 14)
                .addItemStack(recipe.createDefaultResult());
    }

    @Override
    public void draw(StationBlueprints.Blueprint recipe, IRecipeSlotsView recipeSlotsView, GuiGraphics guiGraphics, double mouseX, double mouseY) {
        guiGraphics.fill(0, 0, 150, 44, 0xFF181B20);
        for (int i = 0; i < recipe.ingredients().size(); i++) {
            int col = i % 3;
            int row = i / 3;
            int sx = 5 + col * 20;
            int sy = 3 + row * 20;
            guiGraphics.fill(sx, sy, sx + 18, sy + 18, 0xFF0E1013);
        }
        guiGraphics.fill(119, 13, 137, 31, 0xFF0E1013);
        guiGraphics.drawString(Minecraft.getInstance().font, "->", 88, 18, 0xFF3CFF82, false);
    }
}
