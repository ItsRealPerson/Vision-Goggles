package dev.itsrealperson.vision_goggles.forge;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.ModClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

@Mod(Vision_goggles.MOD_ID)
public final class Vision_gogglesForge {
    public Vision_gogglesForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Vision_goggles.MOD_ID, modEventBus);

        modEventBus.addListener(this::clientSetup);
        
        // Register Cloth Config Screen for Forge
        if (FMLEnvironment.dist == Dist.CLIENT) {
            net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory factory = new net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory((mc, parent) -> {
                return dev.itsrealperson.vision_goggles.client.ModConfigGui.createConfigScreen(parent);
            });
            FMLJavaModLoadingContext.get().registerExtensionPoint(net.minecraftforge.client.ConfigScreenHandler.ConfigScreenFactory.class, () -> factory);
        }

        // Run our common setup.
        Vision_goggles.init();
    }

    @Mod.EventBusSubscriber(modid = Vision_goggles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
            event.registerLayerDefinition(dev.itsrealperson.vision_goggles.client.VisionGoggleModel.LAYER_LOCATION, dev.itsrealperson.vision_goggles.client.VisionGoggleModel::createBodyLayer);
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onAddLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
            for (net.minecraft.world.entity.EntityType<?> entityType : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
                net.minecraft.client.renderer.entity.EntityRenderer<? super net.minecraft.world.entity.LivingEntity> renderer = event.getRenderer((net.minecraft.world.entity.EntityType)entityType);
                if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new dev.itsrealperson.vision_goggles.client.HeatSilhouetteLayer(livingRenderer));
                }
            }

            for (String skin : event.getSkins()) {
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
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.NIGHT_VISION_GOGGLES.get(), dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.THERMAL_GOGGLES.get(), dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.HYDRO_GOGGLES.get(), dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.BIOMETRIC_GOGGLES.get(), dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge::new);
                top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.MODULAR_GOGGLES.get(), dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge::new);
            });
        }
    }
}

