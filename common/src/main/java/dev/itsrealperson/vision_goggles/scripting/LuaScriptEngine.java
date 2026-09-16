package dev.itsrealperson.vision_goggles.scripting;

import dev.itsrealperson.vision_goggles.Vision_goggles;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaScriptEngine {

    private static Globals globals;

    public static synchronized void init() {
        try {
            globals = JsePlatform.standardGlobals();

            // Register global utility functions
            globals.set("getTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                    if (mc.level != null) {
                        return LuaValue.valueOf((double) mc.level.getGameTime() + (double) mc.getFrameTime());
                    }
                    return LuaValue.valueOf(System.currentTimeMillis() / 1000.0);
                }
            });

            globals.set("printLog", new OneArgFunction() {
                @Override
                public LuaValue call(LuaValue arg) {
                    Vision_goggles.LOGGER.info("[Vision Goggles Lua] " + arg.tojstring());
                    return LuaValue.NIL;
                }
            });

            // Register sub-APIs
            LuaHudCanvas.register(globals);
            LuaWorldScanner.register(globals);
            LuaSoundSystem.register(globals);

            Vision_goggles.LOGGER.info("[Vision Goggles] LuaScriptEngine initialized successfully.");
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to initialize LuaScriptEngine: " + e.getMessage(), e);
        }
    }

    public static Globals getGlobals() {
        if (globals == null) {
            init();
        }
        return globals;
    }

    public static LuaValue executeString(String code) {
        try {
            LuaValue chunk = getGlobals().load(code);
            return chunk.call();
        } catch (LuaError error) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Error] " + error.getMessage());
            return LuaValue.valueOf("ERROR: " + error.getMessage());
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Exception] " + e.getMessage());
            return LuaValue.valueOf("EXCEPTION: " + e.getMessage());
        }
    }
}
