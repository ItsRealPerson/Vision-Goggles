package dev.itsrealperson.vision_goggles.fabric.client;

import dev.itsrealperson.vision_goggles.client.ModClient;
import net.fabricmc.api.ClientModInitializer;

import io.wispforest.accessories.api.client.AccessoriesRendererRegistry;
import io.wispforest.accessories.api.client.AccessoryRenderer;
import dev.itsrealperson.vision_goggles.registry.ModItems;

public final class Vision_gogglesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ModClient.init();
        FabricLayerRegistry.init();
    }
}

