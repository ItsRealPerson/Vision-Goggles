package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.registry.ModItems;
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

public class InstallationScriptItem extends Item {

    public InstallationScriptItem() {
        super(new Item.Properties().stacksTo(16));
    }

    public static ItemStack create(String scriptId, String scriptName, String color) {
        ItemStack stack = new ItemStack(ModItems.INSTALLATION_SCRIPT.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(ModConstants.TAG_PROGRAMMED_ID, scriptId);
        tag.putString(ModConstants.TAG_SCRIPT_NAME, scriptName != null ? scriptName : scriptId);
        tag.putString(ModConstants.TAG_SCRIPT_COLOR, color != null ? color : "#00FFCC");
        return stack;
    }

    public static boolean hasScript(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(ModConstants.TAG_PROGRAMMED_ID);
    }

    public static String getScriptId(ItemStack stack) {
        if (!hasScript(stack)) return "vacio";
        return stack.getTag().getString(ModConstants.TAG_PROGRAMMED_ID);
    }

    public static String getScriptName(ItemStack stack) {
        if (!hasScript(stack)) return "Script sin configurar";
        if (stack.getTag().contains(ModConstants.TAG_SCRIPT_NAME)) {
            return stack.getTag().getString(ModConstants.TAG_SCRIPT_NAME);
        }
        return getScriptId(stack);
    }

    public static String getScriptColor(ItemStack stack) {
        if (!hasScript(stack)) return "#FFFFFF";
        if (stack.getTag().contains(ModConstants.TAG_SCRIPT_COLOR)) {
            return stack.getTag().getString(ModConstants.TAG_SCRIPT_COLOR);
        }
        return "#00FFCC";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        if (hasScript(stack)) {
            String name = getScriptName(stack);
            String id = getScriptId(stack);
            tooltipComponents.add(Component.literal("§6Firmware: §f" + name));
            tooltipComponents.add(Component.literal("§7ID Script: §a" + id));
            tooltipComponents.add(Component.literal("§8Combina con un Módulo Programable para grabarlo.").withStyle(ChatFormatting.DARK_GRAY));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.empty_script").withStyle(ChatFormatting.GRAY));
            tooltipComponents.add(Component.literal("§8Usa /vg givescript <id> para configurar.").withStyle(ChatFormatting.DARK_GRAY));
        }
        super.appendHoverText(stack, level, tooltipComponents, isAdvanced);
    }
}
