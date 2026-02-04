package dev.itsrealperson.vision_goggles.item;

import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModConfig;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
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

    public ModularGogglesItem() {
        super(ModConfig::getModularDuration, VisionMode.NIGHT_VISION);
    }
    
    @Override
    public void serverTick(ItemStack stack, ServerPlayer player) {
        CompoundTag nbt = stack.getOrCreateTag();
        List<ModuleType> utils = getUtilityModules(stack);
        boolean hasSolar = utils.contains(ModuleType.SOLAR);
        boolean hasFlashlight = utils.contains(ModuleType.FLASHLIGHT);
        boolean isActive = nbt.getBoolean(ModConstants.TAG_ACTIVE);
        
        // Solar charging logic
        if (hasSolar && player.level().isDay() && player.level().canSeeSky(player.blockPosition().above())) {
            if (player.level().getMaxLocalRawBrightness(player.blockPosition().above()) > 10) {
                float maxBattery = (float) getBatteryCapacity(stack);
                float currentBattery = nbt.getFloat(ModConstants.TAG_BATTERY);
                currentBattery = Math.min(maxBattery, currentBattery + 1.5f);
                nbt.putFloat(ModConstants.TAG_BATTERY, currentBattery);
            }
        }

        // Extra battery drain for Flashlight
        if (isActive && hasFlashlight) {
            float currentBattery = nbt.getFloat(ModConstants.TAG_BATTERY);
            // Flashlight adds an extra 0.5f drain per tick (on top of the base drain in super.serverTick)
            nbt.putFloat(ModConstants.TAG_BATTERY, Math.max(0, currentBattery - 0.5f));
        }
        
        super.serverTick(stack, player);
    }

    @Override
    public List<VisionMode> getSupportedModes() {
        return new ArrayList<>();
    }

    @Override
    public boolean hasModule(ItemStack stack, String moduleName) {
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_MODULES)) {
            ListTag modules = tag.getList(ModConstants.TAG_MODULES, Tag.TAG_STRING);
            for (int i = 0; i < modules.size(); i++) {
                if (modules.getString(i).equals(moduleName)) return true;
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
        return 2;
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
                tooltipComponents.add(Component.literal("- ").append(mode.getDisplayName()).withStyle(ChatFormatting.AQUA));
            }
            for (ModuleType util : utils) {
                tooltipComponents.add(Component.literal("- ").append(Component.translatable("item.vision_goggles." + util.getId().toLowerCase() + "_module")).withStyle(ChatFormatting.YELLOW));
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