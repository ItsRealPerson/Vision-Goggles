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
        VisionHUDOverlay.registerModule(new VitalInfoModule());
        VisionHUDOverlay.registerModule(new OxygenModule());
        VisionHUDOverlay.registerModule(new dev.itsrealperson.vision_goggles.client.hud.ElytraModule());
    }

}
