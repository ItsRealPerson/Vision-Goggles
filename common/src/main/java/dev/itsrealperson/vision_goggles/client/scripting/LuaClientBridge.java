package dev.itsrealperson.vision_goggles.client.scripting;

import dev.itsrealperson.vision_goggles.client.VisionRenderer;
import dev.itsrealperson.vision_goggles.item.ModularGogglesItem;
import dev.itsrealperson.vision_goggles.scripting.*;
import dev.itsrealperson.vision_goggles.util.PlatformMethods;
import dev.itsrealperson.vision_goggles.util.VisionMode;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.PostPass;
import net.minecraft.world.item.ItemStack;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaTable;

import java.util.List;

@Environment(EnvType.CLIENT)
public class LuaClientBridge {

    public static void registerClientApis(Globals globals) {
        LuaHudCanvas.register(globals);
        LuaWorldScanner.register(globals);
        LuaSoundSystem.register(globals);
    }

    public static double getClientTime() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            return ((double) mc.level.getGameTime() + (double) mc.getFrameTime()) / 20.0;
        }
        return System.currentTimeMillis() / 1000.0;
    }

    public static boolean canModuleRun(LuaCustomModule mod) {
        if (!VisionRenderer.isVisorActive()) {
            return false;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return false;
        ItemStack goggles = PlatformMethods.getEquippedHelmet(mc.player);
        if (goggles.isEmpty() || !(goggles.getItem() instanceof ModularGogglesItem modular)) {
            return false;
        }
        if (!mod.isHardwareInstalled(goggles, modular)) {
            return false;
        }

        // If the script targets a specific hardware vision mode (e.g. "nvg", "thermal", "bio", "hydro"),
        // run it only while that VisionMode is active so switching modes in the radial menu doesn't leak effects.
        String req = mod.getRequiredModule();
        if (req != null && !req.isEmpty()) {
            VisionMode reqMode = ModularGogglesItem.resolveHardwareVisionMode(req);
            if (reqMode != null) {
                VisionMode activeMode = VisionRenderer.getCurrentVisionMode();
                if (activeMode != null && activeMode != reqMode) {
                    return false;
                }
            }
        }
        return true;
    }

    public static void onClientTick(float batteryPct) {
        boolean visorActive = VisionRenderer.isVisorActive();
        for (LuaCustomModule mod : LuaModuleManager.getLoadedModules()) {
            boolean shouldRun = visorActive && canModuleRun(mod);
            mod.updateActiveState(shouldRun);
            if (shouldRun) {
                mod.callOnTick(batteryPct);
            }
        }
    }

    public static void onRenderHud(GuiGraphics g, float partialTick) {
        if (!VisionRenderer.isVisorActive()) {
            return;
        }
        for (LuaCustomModule mod : LuaModuleManager.getLoadedModules()) {
            if (canModuleRun(mod)) {
                mod.callOnRenderHud();
            }
        }
        LuaHudCanvas.render(g, partialTick);
    }

    public static Integer hookThermalColor(String entityType, boolean isHostile) {
        for (LuaCustomModule mod : LuaModuleManager.getLoadedModules()) {
            if (canModuleRun(mod)) {
                Integer color = mod.evaluateThermalColor(entityType, isHostile);
                if (color != null) return color;
            }
        }
        return null;
    }

    public static void updateUniforms(LuaCustomModule mod, List<PostPass> passes) {
        if (passes != null) {
            LuaTable bridge = LuaUniformBridge.createBridge(passes);
            mod.callUpdateUniforms(bridge);
        }
    }
}
