package dev.itsrealperson.vision_goggles.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.itsrealperson.vision_goggles.client.gui.ModificationStationScreen;
import dev.itsrealperson.vision_goggles.registry.ModMenus;

import dev.itsrealperson.vision_goggles.client.hud.VitalInfoModule;
import dev.itsrealperson.vision_goggles.client.hud.OxygenModule;
import dev.itsrealperson.vision_goggles.client.hud.DurabilityModule;
import dev.itsrealperson.vision_goggles.client.hud.GogglesStatusModule;

public class ModClient {
    public static void init() {
        EntityModelLayerRegistry.register(VisionGoggleModel.LAYER_LOCATION, VisionGoggleModel::createBodyLayer);
        MenuRegistry.registerScreenFactory(ModMenus.MODIFICATION_STATION_MENU.get(), ModificationStationScreen::new);
        ModKeyMappings.init();
        VisionRenderer.init();

        // Registrar módulos del HUD (El orden importa: los primeros se dibujan debajo)
        VisionHUDOverlay.registerModule(new DurabilityModule()); // Primero las grietas (fondo)
        VisionHUDOverlay.registerModule(new GogglesStatusModule()); // Batería y Modo (Encima)
        VisionHUDOverlay.registerModule(new dev.itsrealperson.vision_goggles.client.hud.SonarRadarModule());
        VisionHUDOverlay.registerModule(new VitalInfoModule());
        VisionHUDOverlay.registerModule(new OxygenModule());
        VisionHUDOverlay.registerModule(new dev.itsrealperson.vision_goggles.client.hud.ElytraModule());

        // Registrar ItemProperties para las gafas modulares
        dev.architectury.registry.item.ItemPropertiesRegistry.register(
            dev.itsrealperson.vision_goggles.registry.ModItems.MODULAR_GOGGLES.get(),
            new net.minecraft.resources.ResourceLocation(dev.itsrealperson.vision_goggles.Vision_goggles.MOD_ID, "mode"),
            (stack, level, entity, seed) -> {
                if (stack.hasTag() && stack.getTag().contains(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODE)) {
                    return stack.getTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODE);
                }
                return 0.0f;
            }
        );

        dev.architectury.registry.item.ItemPropertiesRegistry.register(
            dev.itsrealperson.vision_goggles.registry.ModItems.PRO_MODULAR_GOGGLES.get(),
            new net.minecraft.resources.ResourceLocation(dev.itsrealperson.vision_goggles.Vision_goggles.MOD_ID, "mode"),
            (stack, level, entity, seed) -> {
                if (stack.hasTag() && stack.getTag().contains(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODE)) {
                    return stack.getTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODE);
                }
                return 0.0f;
            }
        );
    }

}
