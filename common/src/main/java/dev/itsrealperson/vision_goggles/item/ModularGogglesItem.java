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
                    float newBattery = Math.max(0, currentBattery - ModConfig.getBatteryStandbyDrain());
                    stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, newBattery);
                    
                    // If battery just died, force sync to tracking clients so flashlights turn off
                    if (currentBattery > 0 && newBattery == 0) {
                        dev.itsrealperson.vision_goggles.network.NetworkManager.INSTANCE.sendToPlayers(
                            player.serverLevel().players(),
                            new dev.itsrealperson.vision_goggles.network.SyncFlashlightPacket(player.getId(), false)
                        );
                    }
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
        if (moduleName == null || moduleName.isEmpty()) return false;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            String strippedReq = moduleName.contains(":") ? moduleName.split(":")[1] : moduleName;

            for (int i = 0; i < modules.size(); i++) {
                String mod = modules.getString(i);
                if (mod.equalsIgnoreCase(moduleName)) return true;
                String strippedMod = mod.contains(":") ? mod.split(":")[1] : mod;
                if (strippedMod.equalsIgnoreCase(strippedReq)) return true;

                // Alias de tipeo: thread_scanner <-> threat_scanner
                if (("thread_scanner".equalsIgnoreCase(strippedReq) && "threat_scanner".equalsIgnoreCase(strippedMod)) ||
                    ("threat_scanner".equalsIgnoreCase(strippedReq) && "thread_scanner".equalsIgnoreCase(strippedMod))) {
                    return true;
                }

                // Alias biométrico: bio <-> biometric
                if (("bio".equalsIgnoreCase(strippedReq) || "biometric".equalsIgnoreCase(strippedReq)) &&
                    ("bio".equalsIgnoreCase(strippedMod) || "biometric".equalsIgnoreCase(strippedMod) || mod.contains("biometric"))) {
                    return true;
                }

                // Alias visión nocturna: nvg <-> night_vision
                if (("nvg".equalsIgnoreCase(strippedReq) || "night_vision".equalsIgnoreCase(strippedReq)) &&
                    ("nvg".equalsIgnoreCase(strippedMod) || "night_vision".equalsIgnoreCase(strippedMod) || mod.contains("night_vision"))) {
                    return true;
                }

                // Alias térmico: thermal
                if ("thermal".equalsIgnoreCase(strippedReq) && ("thermal".equalsIgnoreCase(strippedMod) || mod.contains("thermal"))) {
                    return true;
                }

                // Alias hydro: hydro
                if ("hydro".equalsIgnoreCase(strippedReq) && ("hydro".equalsIgnoreCase(strippedMod) || mod.contains("hydro"))) {
                    return true;
                }

                if ("programmable".equalsIgnoreCase(moduleName) || "generic".equalsIgnoreCase(moduleName)) {
                    if (mod.equals(ModConstants.ID_PROGRAMMABLE) || (!mod.contains("night_vision") && !mod.contains("thermal") && !mod.contains("hydro") && !mod.contains("biometric") && ModuleType.byId(mod) == null)) {
                        return true;
                    }
                }

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
            return (int) (base * ModConfig.getBatteryExpansionMultiplier());
        }
        return base;
    }

    public static int customModularMax = -1;
    public static int customProMax = -1;

    public static void setCustomModularMax(int max) { customModularMax = max; }
    public static void setCustomProMax(int max) { customProMax = max; }
    public static int getCustomModularMax() { return customModularMax; }
    public static int getCustomProMax() { return customProMax; }

    public int getMaxModules() {
        if (this instanceof ProModularGogglesItem) {
            if (customProMax > 0) return customProMax;
            return ModConfig.getProModularMaxModules();
        } else {
            if (customModularMax > 0) return customModularMax;
            return ModConfig.getModularMaxModules();
        }
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

        // Functional modules count (including custom programmed Lua modules)
        int customCount = 0;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag list = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String m = list.getString(i);
                if (VisionMode.byLocation(m) == null && ModuleType.byId(m) == null) {
                    customCount++;
                }
            }
        }
        int functionalCount = modes.size() + utils.size() + customCount;
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
        List<String> customModules = new ArrayList<>();
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag list = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < list.size(); i++) {
                String m = list.getString(i);
                if (VisionMode.byLocation(m) == null && ModuleType.byId(m) == null) {
                    customModules.add(m);
                }
            }
        }

        if (!modes.isEmpty() || !utils.isEmpty() || !customModules.isEmpty()) {
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
            for (String customId : customModules) {
                String name = tag != null && tag.contains("mod_name_" + customId) ? tag.getString("mod_name_" + customId) : customId;
                if (ModConstants.ID_PROGRAMMABLE.equals(customId)) {
                    name = "Módulo Programable (Virgen)";
                }
                tooltipComponents.add(Component.literal("- " + name).withStyle(ChatFormatting.GREEN));
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