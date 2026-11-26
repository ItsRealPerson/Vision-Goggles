package dev.itsrealperson.vision_goggles.api;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.TacticalFlashlightItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import dev.itsrealperson.vision_goggles.util.FlashlightMode;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.ModuleType;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Common/server-safe public API for Vision Goggles addons.
 * Never imports client-only classes; use {@link VisionGogglesClientAPI} on the client.
 */
public final class VisionGogglesAPI {

    private static final Map<String, String> EXTERNAL_LUA_SCRIPTS = new ConcurrentHashMap<>();

    private VisionGogglesAPI() {}

    /** Returns the equipped Vision Goggles stack (helmet or Curios/Accessories), or {@link ItemStack#EMPTY}. */
    public static ItemStack getEquippedGoggles(Player player) {
        if (player == null) return ItemStack.EMPTY;
        ItemStack helmet = PlatformMethods.getEquippedHelmet(player);
        if (!helmet.isEmpty() && helmet.getItem() instanceof VisionGogglesItem) {
            return helmet;
        }
        return ItemStack.EMPTY;
    }

    /** Returns true if the player has Vision Goggles equipped, switched on, and with battery > 0. */
    public static boolean isVisionActive(Player player) {
        ItemStack goggles = getEquippedGoggles(player);
        if (goggles.isEmpty()) return false;
        CompoundTag tag = goggles.getTag();
        return tag != null && tag.getBoolean(ModConstants.TAG_ACTIVE) && getBattery(goggles) > 0;
    }

    /** Returns the active {@link VisionMode} for the player, or null if vision is off. */
    public static VisionMode getActiveVisionMode(Player player) {
        if (!isVisionActive(player)) return null;
        return VisionGogglesItem.getActiveVisionMode(getEquippedGoggles(player));
    }

    /** Returns true if the player's equipped modular goggles contain the given module ID. */
    public static boolean hasModule(Player player, String moduleId) {
        if (player == null || moduleId == null || moduleId.isEmpty()) return false;
        ItemStack goggles = getEquippedGoggles(player);
        if (goggles.isEmpty() || !(goggles.getItem() instanceof ModularGogglesItem modular)) {
            return false;
        }
        if (modular.getModules(goggles).contains(moduleId)) {
            return true;
        }
        ModuleType type = ModuleType.byId(moduleId);
        return type != null && modular.getUtilityModules(goggles).contains(type);
    }

    /** Returns true if the given goggles stack has the specified {@link ModuleType} installed. */
    public static boolean hasModule(ItemStack stack, ModuleType type) {
        if (stack == null || stack.isEmpty() || type == null) return false;
        if (!(stack.getItem() instanceof ModularGogglesItem modular)) return false;
        return modular.getUtilityModules(stack).contains(type) || modular.getModules(stack).contains(type.getId());
    }

    /** Returns true if the player has an active Tactical Flashlight or active Modular Flashlight module. */
    public static boolean isFlashlightActive(Player player) {
        if (player == null) return false;
        if (!TacticalFlashlightItem.getHeldActiveFlashlight(player).isEmpty()) {
            return true;
        }
        ItemStack goggles = getEquippedGoggles(player);
        if (!goggles.isEmpty() && goggles.getItem() instanceof ModularGogglesItem modular) {
            if (modular.getUtilityModules(goggles).contains(ModuleType.FLASHLIGHT)) {
                CompoundTag tag = goggles.getTag();
                return tag != null && tag.getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && getBattery(goggles) > 0;
            }
        }
        return false;
    }

    /** Returns the player's active {@link FlashlightMode}, or null if no flashlight is active. */
    public static FlashlightMode getActiveFlashlightMode(Player player) {
        if (player == null) return null;
        ItemStack tactical = TacticalFlashlightItem.getHeldActiveFlashlight(player);
        if (!tactical.isEmpty()) {
            return FlashlightMode.byId(TacticalFlashlightItem.getMode(tactical));
        }
        ItemStack goggles = getEquippedGoggles(player);
        if (!goggles.isEmpty() && goggles.getItem() instanceof ModularGogglesItem modular) {
            if (modular.getUtilityModules(goggles).contains(ModuleType.FLASHLIGHT)) {
                CompoundTag tag = goggles.getTag();
                if (tag != null && tag.getBoolean(ModConstants.TAG_FLASHLIGHT_ACTIVE) && getBattery(goggles) > 0) {
                    return FlashlightMode.byId(tag.getInt(ModConstants.TAG_FLASHLIGHT_MODE));
                }
            }
        }
        return null;
    }

    /** Returns the current battery charge stored on a goggles or tactical flashlight stack. */
    public static int getBattery(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        CompoundTag tag = stack.getTag();
        if (tag != null && tag.contains(ModConstants.TAG_BATTERY)) {
            return Math.max(0, Math.round(tag.getFloat(ModConstants.TAG_BATTERY)));
        }
        return getMaxBattery(stack);
    }

    /** Returns the maximum battery capacity for a goggles or tactical flashlight stack. */
    public static int getMaxBattery(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return 0;
        if (stack.getItem() instanceof VisionGogglesItem goggles) {
            return goggles.getBatteryCapacity(stack);
        }
        if (stack.getItem() instanceof TacticalFlashlightItem) {
            return Math.round(TacticalFlashlightItem.MAX_BATTERY);
        }
        return 0;
    }

    /** Returns the normalized battery ratio in [0.0, 1.0]. */
    public static float getBatteryRatio(ItemStack stack) {
        int max = getMaxBattery(stack);
        if (max <= 0) return 0.0f;
        return Math.max(0.0f, Math.min(1.0f, (float) getBattery(stack) / (float) max));
    }

    /**
     * Drains {@code amount} battery units from a goggles or tactical flashlight stack.
     * Returns true if any charge was drained.
     */
    public static boolean drainBattery(ItemStack stack, int amount) {
        if (stack == null || stack.isEmpty() || amount <= 0) return false;
        int max = getMaxBattery(stack);
        if (max <= 0) return false;
        CompoundTag tag = stack.getOrCreateTag();
        float current = tag.contains(ModConstants.TAG_BATTERY) ? tag.getFloat(ModConstants.TAG_BATTERY) : (float) max;
        if (current <= 0.0f) return false;
        tag.putFloat(ModConstants.TAG_BATTERY, Math.max(0.0f, current - amount));
        return true;
    }

    /** Registers a custom 3D assembly blueprint in the Modification Station. */
    public static void registerStationBlueprint(StationBlueprints.Blueprint blueprint) {
        StationBlueprints.registerBlueprint(blueprint);
    }

    /** Registers an external Lua script to be deployed alongside bundled scripts. */
    public static void registerExternalLuaScript(String fileName, String luaSource) {
        if (fileName != null && !fileName.isBlank() && luaSource != null) {
            EXTERNAL_LUA_SCRIPTS.put(fileName, luaSource);
        }
    }

    public static Map<String, String> getRegisteredExternalLuaScripts() {
        return Collections.unmodifiableMap(EXTERNAL_LUA_SCRIPTS);
    }
}
