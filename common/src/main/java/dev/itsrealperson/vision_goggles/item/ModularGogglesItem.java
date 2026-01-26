package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import java.util.ArrayList;
import java.util.List;

public class ModularGogglesItem extends VisionGogglesItem {

    public ModularGogglesItem() {
        // Pass a dummy mode to satisfy the super constructor, though we won't use it directly
        // Pass a base battery capacity from config
        super(ModConfig::getModularDuration, VisionMode.NIGHT_VISION);
    }

    @Override
    public List<VisionMode> getSupportedModes() {
        return new ArrayList<>();
    }

    public List<VisionMode> getModes(ItemStack stack) {
        List<VisionMode> modes = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Modules")) {
            ListTag modules = tag.getList("Modules", Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                String moduleName = modules.getString(i);
                try {
                    VisionMode mode = VisionMode.valueOf(moduleName);
                    modes.add(mode);
                } catch (IllegalArgumentException ignored) {
                }
            }
        }
        return modes;
    }
    
    public boolean hasBatteryExpansion(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Modules")) {
             ListTag modules = tag.getList("Modules", Tag.TAG_STRING);
             for (int i = 0; i < modules.size(); i++) {
                 if (modules.getString(i).equals("BATTERY_EXPANSION")) {
                     return true;
                 }
             }
        }
        return false;
    }

    @Override
    public int getBatteryCapacity() {
        return super.getBatteryCapacity(); 
    }
    
    public int getBatteryCapacity(ItemStack stack) {
        int base = super.getBatteryCapacity();
        if (hasBatteryExpansion(stack)) {
            return (int) (base * 1.5f);
        }
        return base;
    }

    public int getMaxModules() {
        return 2; // Default for standard modular goggles
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        // Battery Info
        int current = 0;
        if (stack.hasTag() && stack.getTag().contains("nvg_battery")) {
            current = (int) stack.getTag().getFloat("nvg_battery");
        }
        int max = getBatteryCapacity(stack);
        int percent = (int) (((float)current / (float)max) * 100);
        
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.battery_info", current, max)
                .append(Component.literal(" (" + percent + "%)"))
                .withStyle(ChatFormatting.GOLD));

        List<VisionMode> modes = getModes(stack);
        List<String> utils = getUtilityModules(stack);

        if (!modes.isEmpty() || !utils.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.installed_modules").withStyle(ChatFormatting.GRAY));
            for (VisionMode mode : modes) {
                tooltipComponents.add(Component.literal("- ").append(mode.getDisplayName()).withStyle(ChatFormatting.AQUA));
            }
            for (String util : utils) {
                tooltipComponents.add(Component.literal("- ").append(Component.translatable("item.vision_goggles." + util.toLowerCase() + "_module")).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.no_modules").withStyle(ChatFormatting.RED));
        }
        
        if (hasBatteryExpansion(stack)) {
            tooltipComponents.add(Component.translatable("item.vision_goggles.battery_expansion_module").withStyle(ChatFormatting.GREEN));
        }
    }

    public List<String> getUtilityModules(ItemStack stack) {
        List<String> utils = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains("Modules")) {
            ListTag modules = tag.getList("Modules", Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                String mod = modules.getString(i);
                if (mod.equals("ZOOM") || mod.equals("SOLAR") || mod.equals("SONAR")) {
                    utils.add(mod);
                }
            }
        }
        return utils;
    }
}
