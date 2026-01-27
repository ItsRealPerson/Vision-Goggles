package dev.itsrealperson.vision_goggles.registry;

import com.mojang.serialization.Codec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;

import java.util.List;
import java.util.function.UnaryOperator;

public class ModDataComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Vision_goggles.MOD_ID, Registries.DATA_COMPONENT_TYPE);

    public static final RegistrySupplier<DataComponentType<Float>> BATTERY = register("battery", builder -> builder.persistent(Codec.FLOAT).networkSynchronized(ByteBufCodecs.FLOAT));
    public static final RegistrySupplier<DataComponentType<Boolean>> ACTIVE = register("active", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));
    public static final RegistrySupplier<DataComponentType<Integer>> MODE = register("mode", builder -> builder.persistent(Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    public static final RegistrySupplier<DataComponentType<List<String>>> MODULES = register("modules", builder -> builder.persistent(Codec.STRING.listOf()).networkSynchronized(ByteBufCodecs.collection(java.util.ArrayList::new, ByteBufCodecs.STRING_UTF8)));
    public static final RegistrySupplier<DataComponentType<Boolean>> BATTERY_UPGRADE = register("battery_upgrade", builder -> builder.persistent(Codec.BOOL).networkSynchronized(ByteBufCodecs.BOOL));

    private static <T> RegistrySupplier<DataComponentType<T>> register(String name, UnaryOperator<DataComponentType.Builder<T>> builder) {
        return COMPONENTS.register(name, () -> builder.apply(DataComponentType.builder()).build());
    }

    public static void register() {
        COMPONENTS.register();
    }
}