package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.block.entity.ModificationStationBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.BLOCK_ENTITY_TYPE);

    public static final RegistrySupplier<BlockEntityType<ModificationStationBlockEntity>> MODIFICATION_STATION_BE = BLOCK_ENTITIES.register("modification_station",
            () -> BlockEntityType.Builder.of(ModificationStationBlockEntity::new, ModBlocks.MODIFICATION_STATION.get()).build(null));

    public static void register() {
        BLOCK_ENTITIES.register();
    }
}
