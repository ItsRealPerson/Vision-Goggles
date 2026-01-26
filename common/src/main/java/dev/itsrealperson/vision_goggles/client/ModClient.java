package dev.itsrealperson.vision_goggles.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;
import dev.architectury.registry.menu.MenuRegistry;
import dev.itsrealperson.vision_goggles.client.gui.ModificationStationScreen;
import dev.itsrealperson.vision_goggles.registry.ModMenus;

public class ModClient {
    public static void init() {
        EntityModelLayerRegistry.register(VisionGoggleModel.LAYER_LOCATION, VisionGoggleModel::createBodyLayer);
        MenuRegistry.registerScreenFactory(ModMenus.MODIFICATION_STATION_MENU.get(), ModificationStationScreen::new);
        ModKeyMappings.init();
        VisionRenderer.init();
    }

    public static void registerLayers(EntityRendererEventConsumer consumer) {
        // This will be called from platform specific code
    }

    @FunctionalInterface
    public interface EntityRendererEventConsumer {
        void register(net.minecraft.world.entity.EntityType<? extends net.minecraft.world.entity.LivingEntity> type, net.minecraft.client.renderer.entity.LivingEntityRenderer<?, ?> renderer);
    }
}
