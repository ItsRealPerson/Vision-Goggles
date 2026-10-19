package dev.itsrealperson.vision_goggles.scripting;

import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.fabricmc.api.EnvType;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.OneArgFunction;
import org.luaj.vm2.lib.ZeroArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class LuaScriptEngine {

    private static Globals globals;

    public static synchronized void init() {
        try {
            globals = JsePlatform.standardGlobals();

            // Enforce instruction count hook (100,000 instructions limit per execution)
            SandboxedDebugLib debugLib = new SandboxedDebugLib();
            globals.load(debugLib);

            // Sanitize globals: nullify dangerous tables and functions
            globals.set("luajava", LuaValue.NIL);
            globals.set("package", LuaValue.NIL);
            globals.set("io", LuaValue.NIL);
            globals.set("loadfile", LuaValue.NIL);
            globals.set("dofile", LuaValue.NIL);
            globals.set("debug", LuaValue.NIL);

            // Sanitize 'os' table: retain only safe clock/time functions
            LuaValue os = globals.get("os");
            if (os.istable()) {
                LuaTable safeOs = new LuaTable();
                safeOs.set("clock", os.get("clock"));
                safeOs.set("time", os.get("time"));
                safeOs.set("difftime", os.get("difftime"));
                safeOs.set("date", os.get("date"));
                globals.set("os", safeOs);
            }

            // Register global utility functions
            globals.set("getTime", new ZeroArgFunction() {
                @Override
                public LuaValue call() {
                    if (Platform.getEnv() == EnvType.CLIENT) {
                        try {
                            Class<?> bridge = Class.forName("dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge");
                            return LuaValue.valueOf((Double) bridge.getMethod("getClientTime").invoke(null));
                        } catch (Throwable ignored) {}
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

            // Register client-specific sub-APIs only if running on a client environment
            if (Platform.getEnv() == EnvType.CLIENT) {
                try {
                    Class<?> bridge = Class.forName("dev.itsrealperson.vision_goggles.client.scripting.LuaClientBridge");
                    bridge.getMethod("registerClientApis", Globals.class).invoke(null, globals);
                } catch (Throwable t) {
                    Vision_goggles.LOGGER.error("[Vision Goggles] Failed to register client Lua APIs: " + t.getMessage());
                }
            }

            Vision_goggles.LOGGER.info("[Vision Goggles] LuaScriptEngine initialized successfully with sandbox protection.");
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
            SandboxedDebugLib.resetInstructionCount();
            LuaValue chunk = getGlobals().load(code);
            return chunk.call();
        } catch (LuaError error) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Error] " + error.getMessage());
            return LuaValue.valueOf("ERROR: " + error.getMessage());
        } catch (Throwable e) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Error/Exception] " + e.getMessage());
            return LuaValue.valueOf("ERROR: " + e.getMessage());
        }
    }
}
