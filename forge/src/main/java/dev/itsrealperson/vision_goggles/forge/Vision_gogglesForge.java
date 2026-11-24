package dev.itsrealperson.vision_goggles.forge;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.client.ModClient;
import dev.architectury.platform.forge.EventBuses;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraft.resources.ResourceLocation;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.SlotTypeMessage;

@Mod(Vision_goggles.MOD_ID)
public final class Vision_gogglesForge {
    public Vision_gogglesForge() {
        // Submit our event bus to let Architectury API register our content on the right time.
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        EventBuses.registerModEventBus(Vision_goggles.MOD_ID, modEventBus);

        modEventBus.addListener(this::clientSetup);
        modEventBus.addListener(this::enqueueIMC);
        
        // Register Cloth Config Screen for Forge safely on client only
        net.minecraftforge.fml.DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> Vision_gogglesForgeClient.registerConfigScreen());

        // Run our common setup.
        Vision_goggles.init();
    }

    @Mod.EventBusSubscriber(modid = Vision_goggles.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRegisterLayers(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        }

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onAddLayers(net.minecraftforge.client.event.EntityRenderersEvent.AddLayers event) {
            for (net.minecraft.world.entity.EntityType<?> entityType : net.minecraftforge.registries.ForgeRegistries.ENTITY_TYPES.getValues()) {
                try {
                    net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = event.getRenderer((net.minecraft.world.entity.EntityType)entityType);
                    if (renderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                        livingRenderer.addLayer(new dev.itsrealperson.vision_goggles.client.GogglesRenderLayer(livingRenderer));
                    }
                } catch (ClassCastException e) {
                    // Ignorar entidades no vivas (ej. NoopRenderer) en ciertas versiones de Forge/NeoForge
                }
            }

            for (String skin : event.getSkins()) {
                net.minecraft.client.renderer.entity.EntityRenderer<?> rawRenderer = event.getSkin(skin);
                if (rawRenderer instanceof net.minecraft.client.renderer.entity.LivingEntityRenderer livingRenderer) {
                    livingRenderer.addLayer(new dev.itsrealperson.vision_goggles.client.GogglesRenderLayer(livingRenderer));
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
                            dev.itsrealperson.vision_goggles.util.VisionMode mode = dev.itsrealperson.vision_goggles.item.VisionGogglesItem.getActiveVisionMode(helmet);
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

        @net.minecraftforge.eventbus.api.SubscribeEvent
        public static void onRenderLevel(net.minecraftforge.client.event.RenderLevelStageEvent event) {
            if (event.getStage() == net.minecraftforge.client.event.RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES) {
                dev.itsrealperson.vision_goggles.client.ThermalEntityPainter.captureAndComposite(event.getPoseStack());
            }
        }
    }

    private void enqueueIMC(final InterModEnqueueEvent event) {
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
            () -> new SlotTypeMessage.Builder("eyes")
                    .priority(10)
                    .size(1)
                    .icon(new ResourceLocation("curios", "slot/empty_head_slot"))
                    .build());
        InterModComms.sendTo(CuriosApi.MODID, SlotTypeMessage.REGISTER_TYPE,
            () -> new SlotTypeMessage.Builder("body")
                    .priority(20)
                    .size(1)
                    .build());
    }

    public void clientSetup(final FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            ModClient.init();
            dev.itsrealperson.vision_goggles.client.ThermalEntityPainter.init();
        });
    }
}

