package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModuleType;
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
import net.minecraft.server.level.ServerPlayer;

public class ModularGogglesItem extends VisionGogglesItem {

    private final int maxModules;

    public ModularGogglesItem() {
        this(2);
    }

    public ModularGogglesItem(int maxModules) {
        super(ModConfig::getModularDuration, VisionMode.NIGHT_VISION);
        this.maxModules = maxModules;
    }
    
    @Override
    public void serverTick(ItemStack stack, ServerPlayer player) {
        List<ModuleType> utils = getUtilityModules(stack);
        // Apply power-related effects from modules
        for (ModuleType util : utils) {
            util.tickPower(stack, player);
        }

        // Standby drain if any module is installed and active mode is OFF
        if (!stack.getOrCreateTag().getBoolean(ModConstants.TAG_ACTIVE)) {
            if (!getModes(stack).isEmpty() || !utils.isEmpty()) {
                float currentBattery = stack.getOrCreateTag().getFloat(ModConstants.TAG_BATTERY);
                if (currentBattery > 0) {
                    stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, Math.max(0, currentBattery - 0.001f));
                }
            }
        }
        
        super.serverTick(stack, player);
    }

    @Override
    public List<VisionMode> getSupportedModes() {
        return List.of();
    }

    @Override
    public boolean hasModule(ItemStack stack, String moduleName) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                String mod = modules.getString(i);
                if (mod.equals(moduleName)) return true;

                // Check for ModuleType matches (handles both new ID and legacy name)
                ModuleType type = ModuleType.byId(mod);
                if (type != null) {
                    if (type.getId().equals(moduleName) || type.name().equals(moduleName)) return true;
                }

                // Check for VisionMode matches (handles both new ID and legacy name)
                VisionMode mode = VisionMode.byLocation(mod);
                if (mode != null) {
                    if (mode.getModuleLocation().toString().equals(moduleName) || mode.name().equals(moduleName)) return true;
                }
            }
        }
        return false;
    }

    public List<VisionMode> getModes(ItemStack stack) {
        List<VisionMode> modes = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                String moduleLocation = modules.getString(i);
                VisionMode mode = VisionMode.byLocation(moduleLocation);
                if (mode != null) {
                    modes.add(mode);
                }
            }
        }
        return modes;
    }
    
    public boolean hasBatteryExpansion(ItemStack stack) {
        return getUtilityModules(stack).contains(ModuleType.BATTERY_EXPANSION);
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
        return maxModules;
    }

    public boolean canInstallModule(ItemStack stack, String moduleId) {
        if (hasModule(stack, moduleId)) return false;

        List<VisionMode> modes = getModes(stack);
        List<ModuleType> utils = getUtilityModules(stack);
        int maxTotal = getMaxModules();

        // Incompatibilities: Block Solar on Hydro, Solar/Battery on Bio, prevent incompatible sensors (NVG + Thermal)
        VisionMode newMode = VisionMode.byLocation(moduleId);
        ModuleType newUtil = ModuleType.byId(moduleId);

        if (newMode == VisionMode.THERMAL && modes.contains(VisionMode.NIGHT_VISION)) return false;
        if (newMode == VisionMode.NIGHT_VISION && modes.contains(VisionMode.THERMAL)) return false;

        if (newUtil == ModuleType.SOLAR && (modes.contains(VisionMode.HYDRO) || modes.contains(VisionMode.BIOMETRIC))) return false;
        if (newMode == VisionMode.HYDRO && utils.contains(ModuleType.SOLAR)) return false;
        if (newMode == VisionMode.BIOMETRIC && utils.contains(ModuleType.SOLAR)) return false;

        if (newUtil == ModuleType.BATTERY_EXPANSION && modes.contains(VisionMode.BIOMETRIC)) return false;
        if (newMode == VisionMode.BIOMETRIC && utils.contains(ModuleType.BATTERY_EXPANSION)) return false;

        // Specific check for Battery Expansion
        if (newUtil == ModuleType.BATTERY_EXPANSION) {
            if (utils.contains(ModuleType.BATTERY_EXPANSION)) return false;
            return true;
        }

        // Functional modules count
        int functionalCount = modes.size() + utils.size();
        // Don't count Battery Expansion towards the limit if we are checking a functional module
        functionalCount -= utils.contains(ModuleType.BATTERY_EXPANSION) ? 1 : 0;

        return functionalCount < maxTotal;
    }

    public void installModule(ItemStack stack, String moduleId) {
        if (!canInstallModule(stack, moduleId)) return;

        CompoundTag nbt = stack.getOrCreateTag();
        ListTag modules = nbt.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
        modules.add(StringTag.valueOf(moduleId));
        nbt.put(ModConstants.TAG_MODULES, modules);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltipComponents, TooltipFlag isAdvanced) {
        // Battery Info
        int current = 0;
        if (stack.hasTag() && stack.getTag().contains(ModConstants.TAG_BATTERY)) {
            current = (int) stack.getTag().getFloat(ModConstants.TAG_BATTERY);
        }
        int max = getBatteryCapacity(stack);
        int percent = (int) (((float)current / (float)max) * 100);
        
        tooltipComponents.add(Component.translatable("tooltip.vision_goggles.battery_info", current, max)
                .append(Component.literal(" (" + percent + "%)"))
                .withStyle(ChatFormatting.GOLD));

        List<VisionMode> modes = getModes(stack);
        List<ModuleType> utils = getUtilityModules(stack);

        if (!modes.isEmpty() || !utils.isEmpty()) {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.installed_modules").withStyle(ChatFormatting.GRAY));
            for (VisionMode mode : modes) {
                String modeName = "nvg";
                if (mode == VisionMode.THERMAL) modeName = "thermal";
                else if (mode == VisionMode.HYDRO) modeName = "hydro";
                else if (mode == VisionMode.BIOMETRIC) modeName = "bio";
                tooltipComponents.add(Component.literal("- ").append(Component.translatable("item.vision_goggles." + modeName + "_module")).withStyle(ChatFormatting.AQUA));
            }
            for (ModuleType util : utils) {
                String utilName = util.getId().contains(":") ? util.getId().split(":")[1] : util.getId().toLowerCase();
                tooltipComponents.add(Component.literal("- ").append(Component.translatable("item.vision_goggles." + utilName + "_module")).withStyle(ChatFormatting.YELLOW));
            }
        } else {
            tooltipComponents.add(Component.translatable("tooltip.vision_goggles.no_modules").withStyle(ChatFormatting.RED));
        }
    }

    public List<ModuleType> getUtilityModules(ItemStack stack) {
        List<ModuleType> utils = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                String modStr = modules.getString(i);
                ModuleType type = ModuleType.byId(modStr);
                if (type != null) {
                    utils.add(type);
                }
            }
        }
        return utils;
    }
}