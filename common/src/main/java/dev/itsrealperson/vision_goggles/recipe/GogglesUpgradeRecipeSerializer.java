package dev.itsrealperson.vision_goggles.recipe;

import com.google.gson.JsonObject;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;

public class GogglesUpgradeRecipeSerializer implements RecipeSerializer<GogglesUpgradeRecipe> {
    @Override
    public GogglesUpgradeRecipe fromJson(ResourceLocation id, JsonObject json) {
        ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromJson(id, json);
        return new GogglesUpgradeRecipe(shaped);
    }

    @Override
    public GogglesUpgradeRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buf) {
        ShapedRecipe shaped = RecipeSerializer.SHAPED_RECIPE.fromNetwork(id, buf);
        if (shaped == null) return null;
        return new GogglesUpgradeRecipe(shaped);
    }

    @Override
    public void toNetwork(FriendlyByteBuf buf, GogglesUpgradeRecipe recipe) {
        RecipeSerializer.SHAPED_RECIPE.toNetwork(buf, recipe);
    }
}
