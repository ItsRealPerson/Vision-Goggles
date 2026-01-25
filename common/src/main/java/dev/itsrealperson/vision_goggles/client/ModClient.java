package dev.itsrealperson.vision_goggles.client;

import dev.architectury.registry.client.level.entity.EntityModelLayerRegistry;

public class ModClient {
    public static void init() {
        EntityModelLayerRegistry.register(ExoHelmetModel.LAYER_LOCATION, ExoHelmetModel::createBodyLayer);
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
