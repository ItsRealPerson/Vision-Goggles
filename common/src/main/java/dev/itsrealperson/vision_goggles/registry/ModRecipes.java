package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.recipe.GogglesUpgradeRecipe;
import dev.itsrealperson.vision_goggles.recipe.GogglesUpgradeRecipeSerializer;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.RECIPE_SERIALIZER);

    public static final RegistrySupplier<RecipeSerializer<GogglesUpgradeRecipe>> GOGGLES_UPGRADE_SERIALIZER = RECIPE_SERIALIZERS.register("goggles_upgrade",
            GogglesUpgradeRecipeSerializer::new);

    public static void register() {
        RECIPE_SERIALIZERS.register();
    }
}
