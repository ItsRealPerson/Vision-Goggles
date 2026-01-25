package dev.itsrealperson.vision_goggles;

import com.mojang.logging.LogUtils;
import dev.itsrealperson.vision_goggles.client.ExoHelmetModel;
import dev.itsrealperson.vision_goggles.client.VisionCurioRenderer;
import dev.itsrealperson.vision_goggles.common.BatteryItem;
import dev.itsrealperson.vision_goggles.common.ModSounds;
import dev.itsrealperson.vision_goggles.common.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.network.ModMessages;
import dev.itsrealperson.vision_goggles.util.Config;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

@Mod(VisionGoggles.MODID)
public class VisionGoggles {
    public static final String MODID = "vision_goggles";
    private static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<Item> NIGHT_VISION_GOGGLES_HELMET = ITEMS.register("night_vision_goggles_helmet",
            () -> new VisionGogglesItem());

    public static final RegistryObject<Item> THERMAL_GOGGLES_HELMET = ITEMS.register("thermal_goggles_helmet",
            () -> new VisionGogglesItem());

    public static final RegistryObject<Item> NVG_BATTERY = ITEMS.register("nvg_battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    public static final RegistryObject<CreativeModeTab> VISION_TAB = CREATIVE_MODE_TABS.register("vision_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> NIGHT_VISION_GOGGLES_HELMET.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(NIGHT_VISION_GOGGLES_HELMET.get());
                output.accept(THERMAL_GOGGLES_HELMET.get());
                output.accept(NVG_BATTERY.get());
            }).build());

    public VisionGoggles() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(this::registerLayerDefinitions);
        modEventBus.addListener(this::clientSetup);

        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        ModSounds.register(modEventBus);

        MinecraftForge.EVENT_BUS.register(this);

        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, Config.SERVER_SPEC);
        
        ModMessages.register();
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    private void clientSetup(final net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(NIGHT_VISION_GOGGLES_HELMET.get(), VisionCurioRenderer::new);
            top.theillusivec4.curios.api.client.CuriosRendererRegistry.register(THERMAL_GOGGLES_HELMET.get(), VisionCurioRenderer::new);
        });
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.COMBAT) {
            event.accept(NIGHT_VISION_GOGGLES_HELMET.get());
            event.accept(THERMAL_GOGGLES_HELMET.get());
        }
    }

    private void registerLayerDefinitions(net.minecraftforge.client.event.EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(ExoHelmetModel.LAYER_LOCATION, ExoHelmetModel::createBodyLayer);
    }
}