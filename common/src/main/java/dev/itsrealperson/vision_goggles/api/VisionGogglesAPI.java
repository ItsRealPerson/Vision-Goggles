package dev.itsrealperson.vision_goggles.api;

import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.item.VisionGogglesItem;
import dev.itsrealperson.vision_goggles.recipe.StationBlueprints;
import dev.itsrealperson.vision_goggles.scripting.LuaModuleManager;
import dev.itsrealperson.vision_goggles.util.ModConstants;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Public Common/Server-safe API for Vision Goggles 2.0.0.
 * Contains zero client-only imports so satellite mods can safely reference it from common/server code.
 */
public final class VisionGogglesAPI {
    public static final String MOD_ID = "vision_goggles";

    private static final Map<String, String> EXTERNAL_LUA_SCRIPTS = new ConcurrentHashMap<>();

    private VisionGogglesAPI() {
    }

    /**
     * Returns true if the player currently has any Vision Goggles equipped in the head slot
     * or compatible accessory/curio slot.
     */
    public static boolean hasEquippedGoggles(Player player) {
        return !getEquippedGoggles(player).isEmpty();
    }

    /**
     * Returns the equipped Vision Goggles {@link ItemStack} from Curios/Accessories or {@link EquipmentSlot#HEAD},
     * or {@link ItemStack#EMPTY} if none are equipped.
     */
    public static ItemStack getEquippedGoggles(Player player) {
        if (player == null) {
            return ItemStack.EMPTY;
        }
        try {
            ItemStack equipped = PlatformMethods.getEquippedHelmet(player);
            if (!equipped.isEmpty() && equipped.getItem() instanceof VisionGogglesItem) {
                return equipped;
            }
        } catch (Throwable ignored) {
        }
        ItemStack headSlot = player.getItemBySlot(EquipmentSlot.HEAD);
        if (!headSlot.isEmpty() && headSlot.getItem() instanceof VisionGogglesItem) {
            return headSlot;
        }
        return ItemStack.EMPTY;
    }

    /**
     * Returns true if the given goggles stack is valid and currently switched ON (active in NBT).
     */
    public static boolean isGogglesActive(ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem)) {
            return false;
        }
        CompoundTag tag = stack.getTag();
        return tag != null && tag.getBoolean(ModConstants.TAG_ACTIVE);
    }

    /**
     * Returns true if the given goggles stack has the specified hardware or Lua module installed.
     */
    public static boolean hasModuleInstalled(ItemStack stack, String moduleId) {
        if (stack == null || stack.isEmpty() || moduleId == null || moduleId.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof VisionGogglesItem goggles) {
            return goggles.hasModule(stack, moduleId);
        }
        return false;
    }

    /**
     * Attempts to install a module into a {@link ModularGogglesItem} stack if compatible and capacity permits.
     *
     * @return true if the module was newly installed, false otherwise.
     */
    public static boolean installModule(ItemStack stack, String moduleId) {
        if (stack == null || stack.isEmpty() || moduleId == null || moduleId.isEmpty()) {
            return false;
        }
        if (stack.getItem() instanceof ModularGogglesItem modular) {
            if (!modular.canInstallModule(stack, moduleId)) {
                return false;
            }
            modular.installModule(stack, moduleId);
            return modular.hasModule(stack, moduleId);
        }
        return false;
    }

    /**
     * Registers an external Lua script provided by a satellite mod so {@link LuaModuleManager}
     * writes it to the config scripts directory and loads it on startup and reload.
     */
    public static void registerExternalLuaScript(String fileName, String luaSource) {
        if (fileName == null || fileName.isBlank() || luaSource == null || luaSource.isBlank()) {
            return;
        }
        String cleanName = fileName.trim();
        if (!cleanName.toLowerCase().endsWith(".lua")) {
            cleanName = cleanName + ".lua";
        }
        EXTERNAL_LUA_SCRIPTS.put(cleanName, luaSource);
        LuaModuleManager.syncExternalScriptIfInitialized(cleanName, luaSource);
    }

    public static Map<String, String> getRegisteredExternalLuaScripts() {
        return Collections.unmodifiableMap(EXTERNAL_LUA_SCRIPTS);
    }

    /**
     * Registers a custom Modification Station blueprint provided by a satellite mod.
     */
    public static void registerStationBlueprint(StationBlueprints.Blueprint blueprint) {
        StationBlueprints.registerBlueprint(blueprint);
    }
}
