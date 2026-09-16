package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ProgrammableModuleItem extends Item {

    public ProgrammableModuleItem() {
        super(new Item.Properties().stacksTo(16));
    }

    public static boolean isProgrammed(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ModConstants.TAG_PROGRAMMED_ID);
    }

    public static String getProgrammedId(ItemStack stack) {
        if (!isProgrammed(stack)) return null;
        return stack.getTag().getString(ModConstants.TAG_PROGRAMMED_ID);
    }

    public static String getProgrammedName(ItemStack stack) {
        if (!isProgrammed(stack)) return "Módulo Programable (Virgen)";
        if (stack.getTag().contains(ModConstants.TAG_SCRIPT_NAME)) {
            return stack.getTag().getString(ModConstants.TAG_SCRIPT_NAME);
        }
        return getProgrammedId(stack);
    }

    public static String getProgrammedColor(ItemStack stack) {
        if (!isProgrammed(stack)) return "#FFFFFF";
        if (stack.getTag().contains(ModConstants.TAG_SCRIPT_COLOR)) {
            return stack.getTag().getString(ModConstants.TAG_SCRIPT_COLOR);
        }
        return "#00FFCC";
    }

    public static ItemStack createProgrammed(ItemStack base, String scriptId, String scriptName, String color) {
        ItemStack copy = base.copy();
        copy.setCount(1);
        CompoundTag tag = copy.getOrCreateTag();
        tag.putString(ModConstants.TAG_PROGRAMMED_ID, scriptId);
        tag.putString(ModConstants.TAG_SCRIPT_NAME, scriptName != null ? scriptName : scriptId);
        tag.putString(ModConstants.TAG_SCRIPT_COLOR, color != null ? color : "#00FFCC");
        return copy;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (isProgrammed(stack)) {
            String name = getProgrammedName(stack);
            String id = getProgrammedId(stack);
            tooltipComponents.add(Component.literal("§bMódulo Programado: §a" + name));
            tooltipComponents.add(Component.literal("§7Firmware: §e" + id));
            tooltipComponents.add(Component.literal("§8Instálalo en tus gafas en la Estación de Modificación.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.literal("§7Hardware virgen en blanco.").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("§8Combina con un Script de Instalación para programarlo.").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
