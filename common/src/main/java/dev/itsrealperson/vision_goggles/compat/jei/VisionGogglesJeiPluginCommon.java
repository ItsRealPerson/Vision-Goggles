package dev.itsrealperson.vision_goggles.compat.jei;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.gui.ModificationStationScreen;
import dev.itsrealperson.vision_goggles.item.InstallationScriptItem;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.ProgrammableModuleItem;
import dev.itsrealperson.vision_goggles.registry.ModBlocks;
import dev.itsrealperson.vision_goggles.registry.ModItems;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class VisionGogglesJeiPluginCommon {
    public static final RecipeType<ModificationStationJeiRecipe> MODIFICATION_TYPE =
        RecipeType.create(Vision_goggles.MOD_ID, "modification_station", ModificationStationJeiRecipe.class);

    public static void registerCategories(IRecipeCategoryRegistration registration) {
        registration.addRecipeCategories(new ModificationStationRecipeCategory(registration.getJeiHelpers().getGuiHelper()));
    }

    public static void registerRecipes(IRecipeRegistration registration) {
        registration.addRecipes(MODIFICATION_TYPE, generateRecipes());
    }

    public static void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalyst(new ItemStack(ModBlocks.MODIFICATION_STATION.get()), MODIFICATION_TYPE);
    }

    public static void registerGuiHandlers(IGuiHandlerRegistration registration) {
        // Registers click area over progress arrow in ModificationStationScreen (x=98, y=36, w=16, h=14)
        registration.addRecipeClickArea(
            ModificationStationScreen.class,
            98, 36, 16, 14,
            MODIFICATION_TYPE
        );
    }

    public static List<ModificationStationJeiRecipe> generateRecipes() {
        List<ModificationStationJeiRecipe> recipes = new ArrayList<>();

        List<ItemStack> modularGogglesList = List.of(
            new ItemStack(ModItems.MODULAR_GOGGLES.get()),
            new ItemStack(ModItems.PRO_MODULAR_GOGGLES.get())
        );

        List<ModuleEntry> modules = List.of(
            new ModuleEntry(ModItems.NVG_MODULE.get(), VisionMode.NIGHT_VISION.getModuleLocation().toString()),
            new ModuleEntry(ModItems.THERMAL_MODULE.get(), VisionMode.THERMAL.getModuleLocation().toString()),
            new ModuleEntry(ModItems.HYDRO_MODULE.get(), VisionMode.HYDRO.getModuleLocation().toString()),
            new ModuleEntry(ModItems.BIO_MODULE.get(), VisionMode.BIOMETRIC.getModuleLocation().toString()),
            new ModuleEntry(ModItems.FLASHLIGHT_MODULE.get(), ModConstants.ID_FLASHLIGHT),
            new ModuleEntry(ModItems.ZOOM_MODULE.get(), ModConstants.ID_ZOOM),
            new ModuleEntry(ModItems.SONAR_MODULE.get(), ModConstants.ID_SONAR),
            new ModuleEntry(ModItems.VITAL_INFO_MODULE.get(), ModConstants.ID_VITAL_INFO),
            new ModuleEntry(ModItems.ENVIRONMENT_MODULE.get(), ModConstants.ID_ENVIRONMENT),
            new ModuleEntry(ModItems.SOLAR_MODULE.get(), ModConstants.ID_SOLAR),
            new ModuleEntry(ModItems.BATTERY_EXPANSION_MODULE.get(), ModConstants.ID_BATTERY_EXPANSION),
            new ModuleEntry(ModItems.SPAWN_SECURITY_MODULE.get(), ModConstants.ID_SPAWN_SECURITY)
        );

        // 1. Module installations on Modular and Pro Modular Goggles
        for (ItemStack baseGoggles : modularGogglesList) {
            for (ModuleEntry mod : modules) {
                if (baseGoggles.getItem() instanceof ModularGogglesItem modular && modular.canInstallModule(baseGoggles, mod.id())) {
                    ItemStack result = baseGoggles.copy();
                    modular.installModule(result, mod.id());

                    recipes.add(new ModificationStationJeiRecipe(
                        List.of(baseGoggles.copy()),
                        List.of(new ItemStack(mod.item())),
                        result,
                        Component.translatable("jei.vision_goggles.install_module")
                    ));
                }
            }
        }

        // 2. Flashing script onto Programmable Module
        ItemStack blankChip = new ItemStack(ModItems.PROGRAMMABLE_MODULE.get());
        ItemStack exampleScript = InstallationScriptItem.create("threat_scanner", "Threat Scanner", "#FF3333");
        ItemStack programmedChip = ProgrammableModuleItem.createProgrammed(blankChip, "threat_scanner", "Threat Scanner", "#FF3333");

        recipes.add(new ModificationStationJeiRecipe(
            List.of(blankChip),
            List.of(exampleScript),
            programmedChip,
            Component.translatable("jei.vision_goggles.flash_firmware")
        ));

        // 3. Installing programmed script chip onto Goggles
        for (ItemStack baseGoggles : modularGogglesList) {
            ItemStack result = baseGoggles.copy();
            if (result.getItem() instanceof ModularGogglesItem modular) {
                modular.installModule(result, "threat_scanner");
                result.getOrCreateTag().putString("mod_name_threat_scanner", "Threat Scanner");
                result.getOrCreateTag().putString("mod_color_threat_scanner", "#FF3333");

                recipes.add(new ModificationStationJeiRecipe(
                    List.of(baseGoggles.copy()),
                    List.of(programmedChip.copy()),
                    result,
                    Component.translatable("jei.vision_goggles.install_script_module")
                ));
            }
        }

        return recipes;
    }

    public record ModuleEntry(Item item, String id) {}
}
