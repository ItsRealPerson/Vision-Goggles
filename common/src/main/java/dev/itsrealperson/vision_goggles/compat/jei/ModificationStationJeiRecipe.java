package dev.itsrealperson.vision_goggles.compat.jei;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import java.util.List;

public record ModificationStationJeiRecipe(
    List<ItemStack> baseInputs,
    List<ItemStack> moduleInputs,
    ItemStack output,
    Component description
) {}
