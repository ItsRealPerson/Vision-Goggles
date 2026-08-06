package dev.itsrealperson.vision_goggles.fabric.client;

import dev.itsrealperson.vision_goggles.client.ModClient;
import net.fabricmc.api.ClientModInitializer;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;


import net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry;
import dev.itsrealperson.vision_goggles.client.renderer.VisionGogglesGeoRenderer;
import dev.itsrealperson.vision_goggles.registry.ModItems;

public final class Vision_gogglesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModClient.init();
        VisionGogglesGeoRenderer renderer = new VisionGogglesGeoRenderer();
        net.fabricmc.fabric.api.client.rendering.v1.BuiltinItemRendererRegistry.DynamicItemRenderer wrapped = (stack, mode, matrices, vertexConsumers, light, overlay) -> {
            renderer.renderByItem(stack, mode, matrices, vertexConsumers, light, overlay);
        };
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.NIGHT_VISION_GOGGLES.get(), wrapped);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.THERMAL_GOGGLES.get(), wrapped);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.HYDRO_GOGGLES.get(), wrapped);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.BIOMETRIC_GOGGLES.get(), wrapped);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.MODULAR_GOGGLES.get(), wrapped);
        BuiltinItemRendererRegistry.INSTANCE.register(ModItems.PRO_MODULAR_GOGGLES.get(), wrapped);    }
}

