package dev.itsrealperson.vision_goggles.neoforge;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.ModClient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModLoadingContext;

@Mod(Vision_goggles.MOD_ID)
public final class Vision_gogglesForge {
    public Vision_gogglesForge(IEventBus modEventBus) {
        
        modEventBus.addListener(this::clientSetup);
        
        // Register Cloth Config Screen for NeoForge 1.21.1
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class, () -> 
                (mc, parent) -> dev.itsrealperson.vision_goggles.client.ModConfigGui.createConfigScreen(parent)
            );
        }

        // Run our common setup.
        Vision_goggles.init();
    }

    @EventBusSubscriber(modid = Vision_goggles.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterScreens(net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) {
            event.register(dev.itsrealperson.vision_goggles.registry.ModMenus.MODIFICATION_STATION_MENU.get(), dev.itsrealperson.vision_goggles.client.gui.ModificationStationScreen::new);
        }

        @SubscribeEvent
        public static void onRegisterLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(dev.itsrealperson.vision_goggles.client.VisionGoggleModel.LAYER_LOCATION, dev.itsrealperson.vision_goggles.client.VisionGoggleModel::createBodyLayer);
        }

        @SubscribeEvent
        public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
            for (net.minecraft.world.entity.EntityType<?> entityType : BuiltInRegistries.ENTITY_TYPE) {
                net.minecraft.client.renderer.entity.EntityRenderer<? super net.minecraft.world.entity.LivingEntity> renderer = event.getRenderer((net.minecraft.world.entity.EntityType)entityType);
                if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new dev.itsrealperson.vision_goggles.client.HeatSilhouetteLayer(livingRenderer));
                }
            }

            for (net.minecraft.client.resources.PlayerSkin.Model skin : event.getSkins()) {
                net.minecraft.client.renderer.entity.LivingEntityRenderer renderer = event.getSkin(skin);
                if (renderer != null) {
                    renderer.addLayer(new dev.itsrealperson.vision_goggles.client.HeatSilhouetteLayer(renderer));
                }
            }
        }
    }

    private void clientSetup(FMLClientSetupEvent event) {
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModClient.init();
            event.enqueueWork(() -> {
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.NIGHT_VISION_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.THERMAL_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.HYDRO_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.BIOMETRIC_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.MODULAR_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.PRO_MODULAR_GOGGLES.get(), dev.itsrealperson.vision_goggles.neoforge.client.VisionCurioRendererForge::new);
            });
        }
    }
}
