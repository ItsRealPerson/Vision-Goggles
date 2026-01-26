package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> VISION_TAB = TABS.register("vision_tab", () ->
            CreativeTabRegistry.create(Component.translatable("itemGroup." + Vision_goggles.MOD_ID + ".vision_tab"),
                    () -> new ItemStack(ModItems.NIGHT_VISION_GOGGLES.get()))
    );

    public static void register() {
        TABS.register();
        
        // Use suppliers to avoid calling .get() too early
        CreativeTabRegistry.appendStack(VISION_TAB, 
            () -> new ItemStack(ModItems.NIGHT_VISION_GOGGLES.get()),
            () -> new ItemStack(ModItems.THERMAL_GOGGLES.get()),
            () -> new ItemStack(ModItems.HYDRO_GOGGLES.get()),
            () -> new ItemStack(ModItems.BIOMETRIC_GOGGLES.get()),
            () -> new ItemStack(ModItems.NVG_MODULE.get()),
            () -> new ItemStack(ModItems.THERMAL_MODULE.get()),
            () -> new ItemStack(ModItems.HYDRO_MODULE.get()),
            () -> new ItemStack(ModItems.BIO_MODULE.get()),
            () -> new ItemStack(ModItems.ELECTRICAL_PARTS.get()),
            () -> new ItemStack(ModItems.ZOOM_MODULE.get()),
            () -> new ItemStack(ModItems.SOLAR_MODULE.get()),
            () -> new ItemStack(ModItems.SONAR_MODULE.get()),
            () -> new ItemStack(ModItems.BATTERY_EXPANSION_MODULE.get()),
            () -> new ItemStack(ModItems.MODULAR_GOGGLES.get()),
            () -> new ItemStack(ModItems.PRO_MODULAR_GOGGLES.get()),
            () -> new ItemStack(ModItems.MODIFICATION_STATION.get()),
            () -> new ItemStack(ModItems.NVG_BATTERY.get())
        );
    }
}

