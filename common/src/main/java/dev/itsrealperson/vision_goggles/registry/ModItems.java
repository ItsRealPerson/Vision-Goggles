package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.item.BatteryItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> NIGHT_VISION_GOGGLES = ITEMS.register("night_vision_goggles",
            () -> new VisionGogglesItem());

    public static final RegistrySupplier<Item> THERMAL_GOGGLES = ITEMS.register("thermal_goggles",
            () -> new VisionGogglesItem());

    public static final RegistrySupplier<Item> NVG_BATTERY = ITEMS.register("nvg_battery",
            () -> new BatteryItem(new Item.Properties().stacksTo(16)));

    public static void register() {
        ITEMS.register();
    }
}
