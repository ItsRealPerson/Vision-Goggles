package dev.itsrealperson.vision_goggles.common;

import dev.itsrealperson.vision_goggles.VisionGoggles;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, VisionGoggles.MODID);

    public static final RegistryObject<SoundEvent> VISOR_ON = SOUND_EVENTS.register("visor_on", 
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VisionGoggles.MODID, "visor_on")));

    public static final RegistryObject<SoundEvent> VISOR_OFF = SOUND_EVENTS.register("visor_off", 
            () -> SoundEvent.createVariableRangeEvent(new ResourceLocation(VisionGoggles.MODID, "visor_off")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
