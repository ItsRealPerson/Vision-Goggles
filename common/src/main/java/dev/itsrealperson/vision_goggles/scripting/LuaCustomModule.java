package dev.itsrealperson.vision_goggles.scripting;

import net.minecraft.resources.ResourceLocation;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

public class LuaCustomModule {

    private final ResourceLocation id;
    private final String name;
    private final String type; // "VISION_MODE", "UTILITY", "HUD"
    private final ResourceLocation shaderLocation;
    private final float batteryDrain;
    private final String requiredModule; // Optional module required to be installed in goggles

    private final LuaValue onUpdateUniforms;
    private final LuaValue onEnable;
    private final LuaValue onDisable;
    private final LuaValue onTick;
    private final LuaValue onThermalColor;
    private final LuaValue onRenderHud;

    public LuaCustomModule(ResourceLocation id, String name, String type, ResourceLocation shaderLocation, float batteryDrain,
                           String requiredModule,
                           LuaValue onUpdateUniforms, LuaValue onEnable, LuaValue onDisable, LuaValue onTick,
                           LuaValue onThermalColor, LuaValue onRenderHud) {
        this.id = id;
        this.name = name;
        this.type = type != null ? type.toUpperCase() : "UTILITY";
        this.shaderLocation = shaderLocation;
        this.batteryDrain = batteryDrain;
        this.requiredModule = requiredModule;
        this.onUpdateUniforms = onUpdateUniforms;
        this.onEnable = onEnable;
        this.onDisable = onDisable;
        this.onTick = onTick;
        this.onThermalColor = onThermalColor;
        this.onRenderHud = onRenderHud;
    }

    public static LuaCustomModule fromLuaTable(LuaTable table, String filename) {
        String rawId = table.get("id").isstring() ? table.get("id").tojstring() : "vision_goggles:custom_" + filename.replace(".lua", "");
        ResourceLocation id = rawId.contains(":") ? new ResourceLocation(rawId) : new ResourceLocation("vision_goggles", rawId);
        
        String name = table.get("name").isstring() ? table.get("name").tojstring() : filename;
        String type = table.get("type").isstring() ? table.get("type").tojstring() : "UTILITY";

        ResourceLocation shader = null;
        if (table.get("shader").isstring()) {
            String s = table.get("shader").tojstring();
            // Allow shorthand or full path: "minecraft:shaders/post/invert.json" or "mi_pack:cyber"
            if (s.contains(".json")) {
                shader = new ResourceLocation(s);
            } else if (s.contains(":")) {
                String[] parts = s.split(":", 2);
                shader = new ResourceLocation(parts[0], "shaders/post/" + parts[1] + ".json");
            } else {
                shader = new ResourceLocation("vision_goggles", "shaders/post/" + s + ".json");
            }
        }

        float drain = table.get("battery_drain").isnumber() ? (float) table.get("battery_drain").todouble() : 0.0f;
        String reqMod = table.get("required_module").isstring() ? table.get("required_module").tojstring() : null;

        LuaValue updateUniforms = table.get("onUpdateUniforms").isfunction() ? table.get("onUpdateUniforms") : null;
        LuaValue enable = table.get("onEnable").isfunction() ? table.get("onEnable") : null;
        LuaValue disable = table.get("onDisable").isfunction() ? table.get("onDisable") : null;
        LuaValue tick = table.get("onTick").isfunction() ? table.get("onTick") : null;
        LuaValue thermalColor = table.get("onThermalColor").isfunction() ? table.get("onThermalColor") : null;
        LuaValue renderHud = table.get("onRenderHud").isfunction() ? table.get("onRenderHud") : null;

        if (table.get("modular_slots").isnumber()) {
            dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomModularMax(table.get("modular_slots").toint());
        }
        if (table.get("pro_slots").isnumber()) {
            dev.itsrealperson.vision_goggles.item.ModularGogglesItem.setCustomProMax(table.get("pro_slots").toint());
        }

        return new LuaCustomModule(id, name, type, shader, drain, reqMod, updateUniforms, enable, disable, tick, thermalColor, renderHud);
    }

    private boolean wasActive = false;

    public void updateActiveState(boolean isRunning) {
        if (isRunning && !wasActive) {
            wasActive = true;
            callOnEnable();
        } else if (!isRunning && wasActive) {
            wasActive = false;
            callOnDisable();
        }
    }

    public boolean canRun(net.minecraft.client.Minecraft mc) {
        // Must be wearing goggles AND goggles must be turned on with battery
        if (!dev.itsrealperson.vision_goggles.client.VisionRenderer.isVisorActive()) {
            return false;
        }
        if (mc.player == null) return false;
        net.minecraft.world.item.ItemStack goggles = dev.itsrealperson.vision_goggles.util.PlatformMethods.getEquippedHelmet(mc.player);
        if (goggles.isEmpty() || !(goggles.getItem() instanceof dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular)) {
            // Las gafas básicas fijas (Térmicas, NVG, Hydro, Bio) NUNCA ejecutan módulos/scripts de Lua
            return false;
        }
        return isHardwareInstalled(goggles, modular);
    }

    public boolean isHardwareInstalled(net.minecraft.world.item.ItemStack goggles, dev.itsrealperson.vision_goggles.item.ModularGogglesItem modular) {
        // 1. Debe tener físicamente instalado el chip de este script en las gafas modulares!
        String myId = (id != null) ? id.getPath() : null;
        boolean hasChipInstalled = false;
        if (myId != null) {
            hasChipInstalled = modular.hasModule(goggles, myId) || (id != null && modular.hasModule(goggles, id.toString()));
        }
        if (!hasChipInstalled) {
            return false;
        }

        // 2. Si el script además requiere un hardware base previo DISTINTO de sí mismo (ej. bio, thermal, nvg):
        if (requiredModule != null && !requiredModule.isEmpty()) {
            boolean isSelf = (myId != null && requiredModule.equalsIgnoreCase(myId)) 
                || (id != null && requiredModule.equalsIgnoreCase(id.toString()));
            if (!isSelf && !modular.hasModule(goggles, requiredModule)) {
                return false;
            }
        }

        return true;
    }

    public ResourceLocation getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public ResourceLocation getShaderLocation() { return shaderLocation; }
    public float getBatteryDrain() { return batteryDrain; }
    public String getRequiredModule() { return requiredModule; }

    public void callOnEnable() {
        if (onEnable != null) {
            try { onEnable.call(LuaScriptEngine.getGlobals().get("player")); } catch (Exception ignored) {}
        }
    }

    public void callOnDisable() {
        if (onDisable != null) {
            try { onDisable.call(LuaScriptEngine.getGlobals().get("player")); } catch (Exception ignored) {}
        }
    }

    public void callOnTick(float batteryPct) {
        if (onTick != null) {
            try { onTick.call(LuaScriptEngine.getGlobals().get("player"), LuaValue.valueOf(batteryPct)); } catch (Exception ignored) {}
        }
    }

    public void callOnRenderHud() {
        if (onRenderHud != null) {
            try { onRenderHud.call(LuaScriptEngine.getGlobals().get("hud")); } catch (Exception ignored) {}
        }
    }

    public void callUpdateUniforms(java.util.List<net.minecraft.client.renderer.PostPass> passes) {
        if (onUpdateUniforms != null && passes != null) {
            try {
                LuaTable bridge = LuaUniformBridge.createBridge(passes);
                onUpdateUniforms.call(LuaScriptEngine.getGlobals().get("player"), bridge);
            } catch (Exception ignored) {}
        }
    }

    public Integer evaluateThermalColor(String entityType, boolean isHostile) {
        if (onThermalColor != null) {
            try {
                LuaValue result = onThermalColor.call(LuaValue.valueOf(entityType), LuaValue.valueOf(isHostile));
                if (result.isstring()) {
                    return LuaHudCanvas.parseColor(result.tojstring());
                } else if (result.isnumber()) {
                    return result.toint();
                }
            } catch (Exception ignored) {}
        }
        return null;
    }
}
