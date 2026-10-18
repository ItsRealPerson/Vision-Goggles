package dev.itsrealperson.vision_goggles.fabric.compat.jei;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.compat.jei.VisionGogglesJeiPluginCommon;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public class VisionGogglesJeiFabricPlugin implements IModPlugin {
    public static final ResourceLocation UID = new ResourceLocation(Vision_goggles.MOD_ID, "jei_plugin");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        VisionGogglesJeiPluginCommon.registerCategories(registration);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        VisionGogglesJeiPluginCommon.registerRecipes(registration);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        VisionGogglesJeiPluginCommon.registerRecipeCatalysts(registration);
    }

    @Override
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        VisionGogglesJeiPluginCommon.registerGuiHandlers(registration);
    }
}
