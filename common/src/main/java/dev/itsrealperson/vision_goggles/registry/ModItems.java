package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.BatteryItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionModuleItem;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

import dev.itsrealperson.vision_goggles.util.ModConfig;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.ITEM);

    // Goggles
    public static final RegistrySupplier<Item> NIGHT_VISION_GOGGLES = ITEMS.register("night_vision_goggles",
            () -> new VisionGogglesItem(ModConfig::getNvgDuration, VisionMode.NIGHT_VISION));

    public static final RegistrySupplier<Item> THERMAL_GOGGLES = ITEMS.register("thermal_goggles",
            () -> new VisionGogglesItem(ModConfig::getThermalDuration, VisionMode.THERMAL, VisionMode.NIGHT_VISION));

    public static final RegistrySupplier<Item> HYDRO_GOGGLES = ITEMS.register("hydro_goggles",
            () -> new VisionGogglesItem(ModConfig::getHydroDuration, VisionMode.HYDRO));

    public static final RegistrySupplier<Item> BIOMETRIC_GOGGLES = ITEMS.register("biometric_goggles",
            () -> new VisionGogglesItem(ModConfig::getBiometricDuration, VisionMode.BIOMETRIC));

    // Modules
    public static final RegistrySupplier<Item> NVG_MODULE = ITEMS.register("nvg_module",
            () -> new VisionModuleItem(VisionMode.NIGHT_VISION));
    public static final RegistrySupplier<Item> THERMAL_MODULE = ITEMS.register("thermal_module",
            () -> new VisionModuleItem(VisionMode.THERMAL));
    public static final RegistrySupplier<Item> HYDRO_MODULE = ITEMS.register("hydro_module",
            () -> new VisionModuleItem(VisionMode.HYDRO));
    public static final RegistrySupplier<Item> BIO_MODULE = ITEMS.register("bio_module",
            () -> new VisionModuleItem(VisionMode.BIOMETRIC));
    
    public static final RegistrySupplier<Item> ELECTRICAL_PARTS = ITEMS.register("electrical_parts",
            () -> new Item(new Item.Properties()));
    
    public static final RegistrySupplier<Item> BATTERY_EXPANSION_MODULE = ITEMS.register("battery_expansion_module",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MODULAR_GOGGLES = ITEMS.register("modular_goggles",
            () -> new dev.itsrealperson.vision_goggles.item.ModularGogglesItem());

    public static final RegistrySupplier<Item> PRO_MODULAR_GOGGLES = ITEMS.register("pro_modular_goggles",
            () -> new dev.itsrealperson.vision_goggles.item.ProModularGogglesItem());

    // Utility Modules
    public static final RegistrySupplier<Item> ZOOM_MODULE = ITEMS.register("zoom_module",
            () -> new Item(new Item.Properties().stacksTo(1)));
    
    public static final RegistrySupplier<Item> SOLAR_MODULE = ITEMS.register("solar_module",
            () -> new Item(new Item.Properties().stacksTo(1)));
    
    public static final RegistrySupplier<Item> SONAR_MODULE = ITEMS.register("sonar_module",
            () -> new Item(new Item.Properties().stacksTo(1)));

    public static final RegistrySupplier<Item> MODIFICATION_STATION = ITEMS.register("modification_station",
            () -> new net.minecraft.world.item.BlockItem(ModBlocks.MODIFICATION_STATION.get(), new Item.Properties()));

    public static final RegistrySupplier<Item> NVG_BATTERY = ITEMS.register("nvg_battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    public static void register() {
        ITEMS.register();
    }
}
