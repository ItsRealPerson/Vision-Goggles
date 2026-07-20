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

import net.minecraftforge.fml.InterModComms;
import top.theillusivec4.curios.api.SlotTypeMessage;
import top.theillusivec4.curios.api.CuriosApi;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;

@Mod(Vision_goggles.MOD_ID)
public final class Vision_gogglesForge {
    public Vision_gogglesForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Vision_goggles.MOD_ID, modEventBus);

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::enqueueIMC);
        
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
                net.minecraft.client.renderer.entity.EntityRenderer<?> rawRenderer = event.getSkin(skin);
                if (rawRenderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new dev.itsrealperson.vision_goggles.client.HeatSilhouetteLayer(livingRenderer));
                }
            }
        }
    }

    @Mod.EventBusSubscriber(modid = Vision_goggles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRenderFog(net.minecraftforge.client.event.ViewportEvent.RenderFog event) {
            if (event.getCamera().getFluidInCamera() == net.minecraft.world.level.material.FogType.WATER) {
                net.minecraft.world.entity.Entity entity = event.getCamera().getEntity();
                if (entity instanceof net.minecraft.world.entity.player.Player player) {
                    net.minecraft.world.item.ItemStack helmet = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(player);
                    if (!helmet.isEmpty() && helmet.getItem() instanceof dev.itsrealperson.vision_goggles.item.VisionGogglesItem) {
                        if (helmet.getOrCreateTag().getBoolean(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_ACTIVE)) {
                            int modeId = helmet.getOrCreateTag().getInt(dev.itsrealperson.vision_goggles.util.ModConstants.TAG_MODE);
                            dev.itsrealperson.vision_goggles.util.VisionMode mode = dev.itsrealperson.vision_goggles.util.VisionMode.byId(modeId);
                            if (mode == dev.itsrealperson.vision_goggles.util.VisionMode.HYDRO) {
                                float renderDistance = net.minecraft.client.Minecraft.getInstance().gameRenderer.getRenderDistance();
                                event.setNearPlaneDistance(renderDistance * 0.5F);
                                event.setFarPlaneDistance(renderDistance * 5.0F);
                                event.setCanceled(true); 
                            }
                        }
                    }
                }
            }
        }
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE, 
            () -> new SlotTypeMessage.Builder("eyes")
                    .priority(10)
                    .size(1)
                    .icon(new ResourceLocation(Vision_goggles.MOD_ID, "slot/empty_eyes_slot"))
                    .build());
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModClient.init();
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.NIGHT_VISION_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.THERMAL_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.HYDRO_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.BIOMETRIC_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.MODULAR_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(dev.itsrealperson.vision_goggles.registry.ModItems.PRO_MODULAR_GOGGLES.get(), () -> new dev.itsrealperson.vision_goggles.forge.client.VisionCurioRendererForge());
        });
    }
}

