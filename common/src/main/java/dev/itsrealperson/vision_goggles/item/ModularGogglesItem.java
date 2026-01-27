package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.registry.ModDataComponents;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;

public class ModularGogglesItem extends VisionGogglesItem {

    public ModularGogglesItem() {
        super(ModConfig::getModularDuration, VisionMode.NIGHT_VISION);
    }
    
    @Override
    public void serverTick(ItemStack stack, ServerPlayer player) {
        boolean hasSolar = getUtilityModules(stack).contains(ModuleType.SOLAR);
        
        if (hasSolar && player.level().isDay() && player.level().canSeeSky(player.blockPosition().above())) {
            if (player.level().getMaxLocalRawBrightness(player.blockPosition().above()) > 10) {
                float maxBattery = (float) getBatteryCapacity(stack);
                float currentBattery = Objects.requireNonNullElse(stack.get(ModDataComponents.BATTERY.get()), maxBattery);
                currentBattery = Math.min(maxBattery, currentBattery + 1.5f);
                stack.set(ModDataComponents.BATTERY.get(), currentBattery);
            }
        }
        
        super.serverTick(stack, player);
    }

    @Override
    public List<VisionMode> getSupportedModes() {
        return new ArrayList<>();
    }

    public List<VisionMode> getModes(ItemStack stack) {
        List<VisionMode> modes = new ArrayList<>();
        List<String> modules = stack.get(ModDataComponents.MODULES.get());
        if (modules != null) {
            for (String moduleName : modules) {
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
        return Objects.requireNonNullElse(stack.get(ModDataComponents.BATTERY_UPGRADE.get()), false);
    }

    @Override
    public int getBatteryCapacity() {
        return super.getBatteryCapacity(); 
    }
    
    @Override
    public int getBatteryCapacity(ItemStack stack) {
        int base = super.getBatteryCapacity();
        if (hasBatteryExpansion(stack)) {
            return (int) (base * 1.5f);
        }
        return base;
    }

    public int getMaxModules() {
        return 2;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        // Battery Info
        float current = Objects.requireNonNullElse(stack.get(ModDataComponents.BATTERY.get()), 0.0f);
        int max = getBatteryCapacity(stack);
        int percent = (int) ((current / (float)max) * 100);
        
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.battery_info", (int)current, max)
                .append(Component.literal(" (" + percent + "%)"))
                .withStyle(ChatFormatting.GOLD));

        if (hasBatteryExpansion(stack)) {
            tooltipComponents.add(Component.literal("+ ").append(Component.translatable("item.vision_goggles.battery_expansion_module")).withStyle(ChatFormatting.GREEN));
        }

        List<VisionMode> modes = getModes(stack);
        List<ModuleType> utils = getUtilityModules(stack);

        if (!modes.isEmpty() || !utils.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.installed_modules").withStyle(ChatFormatting.GRAY));
            for (VisionMode mode : modes) {
                tooltipComponents.add(Component.literal("- ").append(mode.getDisplayName()).withStyle(ChatFormatting.AQUA));
            }
            for (ModuleType util : utils) {
                // Skip battery expansion as it's handled above
                if (util == ModuleType.BATTERY_EXPANSION) continue;
                tooltipComponents.add(Component.literal("- ").append(Component.translatable("item.vision_goggles." + util.getId().toLowerCase() + "_module")).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.no_modules").withStyle(ChatFormatting.RED));
        }
    }

    public List<ModuleType> getUtilityModules(ItemStack stack) {
        List<ModuleType> utils = new ArrayList<>();
        List<String> modules = stack.get(ModDataComponents.MODULES.get());
        if (modules != null) {
            for (String modStr : modules) {
                ModuleType type = ModuleType.byId(modStr);
                if (type != null) {
                    utils.add(type);
                }
            }
        }
        return utils;
    }
}