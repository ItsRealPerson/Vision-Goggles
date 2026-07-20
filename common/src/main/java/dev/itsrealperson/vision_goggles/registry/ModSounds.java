package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.SOUND_EVENT);

    public static final RegistrySupplier<SoundEvent> VISOR_ON = SOUNDS.register("visor_on",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Vision_goggles.MOD_ID, "visor_on")));

    public static final RegistrySupplier<SoundEvent> VISOR_OFF = SOUNDS.register("visor_off",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Vision_goggles.MOD_ID, "visor_off")));

    public static final RegistrySupplier<SoundEvent> BLIP = SOUNDS.register("blip",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Vision_goggles.MOD_ID, "blip")));

    public static final RegistrySupplier<SoundEvent> SONAR = SOUNDS.register("sonar",
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(Vision_goggles.MOD_ID, "sonar")));

    public static void register() {
        SOUNDS.register();
    }
}
