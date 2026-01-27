package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> VISOR_ON = SOUND_EVENTS.register("visor_on",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "visor_on")));

    public static final RegistrySupplier<SoundEvent> VISOR_OFF = SOUND_EVENTS.register("visor_off",
            () -> SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(Vision_goggles.MOD_ID, "visor_off")));

    public static void register() {
        SOUND_EVENTS.register();
    }
}