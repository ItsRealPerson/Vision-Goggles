package dev.itsrealperson.vision_goggles.registry;

import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import dev.itsrealperson.vision_goggles.block.ModificationStationBlock;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.BLOCK);

    public static final RegistrySupplier<Block> MODIFICATION_STATION = BLOCKS.register("modification_station",
            () -> new ModificationStationBlock(BlockBehaviour.Properties.ofFullCopy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion()));

    public static void register() {
        BLOCKS.register();
    }
}
