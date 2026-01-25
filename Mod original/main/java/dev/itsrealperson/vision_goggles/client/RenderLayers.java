package dev.itsrealperson.vision_goggles.client;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid = VisionGoggles.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class RenderLayers {

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {

        for (EntityType<?> entityType : ForgeRegistries.ENTITY_TYPES.getValues()) {
            LivingEntityRenderer renderer = event.getRenderer((EntityType)entityType);
            if (renderer != null) {
                renderer.addLayer(new HeatSilhouetteLayer(renderer));
            }
        }

        for (String skin : event.getSkins()) {
            LivingEntityRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new HeatSilhouetteLayer(renderer));
            }
        }
    }
}