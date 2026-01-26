package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.menu.ModificationStationMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;

public class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.MENU);

    // We will need the Menu class before this compiles fully, but this is the structure.
    public static final RegistrySupplier<MenuType<ModificationStationMenu>> MODIFICATION_STATION_MENU = MENUS.register("modification_station",
            () -> dev.architectury.registry.menu.MenuRegistry.ofExtended(ModificationStationMenu::new));

    public static void register() {
        MENUS.register();
    }
}
