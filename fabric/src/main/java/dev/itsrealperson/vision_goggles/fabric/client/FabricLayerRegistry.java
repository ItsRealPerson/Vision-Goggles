package dev.itsrealperson.vision_goggles.fabric.client;

import dev.itsrealperson.vision_goggles.client.HeatSilhouetteLayer;
import net.fabricmc.fabric.api.client.rendering.v1.LivingEntityFeatureRendererRegistrationCallback;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;

public class FabricLayerRegistry {
    public static void init() {
        LivingEntityFeatureRendererRegistrationCallback.EVENT.register((entityType, entityRenderer, registrationHelper, context) -> {
            if (entityRenderer instanceof LivingEntityRenderer) {
                LivingEntityRenderer livingRenderer = (LivingEntityRenderer) entityRenderer;
                registrationHelper.register(new HeatSilhouetteLayer(livingRenderer));
            }
        });
    }
}
