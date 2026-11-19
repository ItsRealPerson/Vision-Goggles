package dev.itsrealperson.vision_goggles.scripting;

import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.resources.ResourceLocation;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LuaModuleManager {

    private static final Map<ResourceLocation, LuaCustomModule> LOADED_MODULES = new LinkedHashMap<>();
    private static LuaCustomModule activeVisionModeModule = null;

    private static final List<String> DEFAULT_RESOURCE_FILES = List.of(
            "00_GUIA_PASO_A_PASO.txt",
            "01_radar_tactico_360.lua",
            "02_visor_ar_objetivos.lua",
            "03_visor_combate_reactivo.lua",
            "04_hud_piloto_avionica.lua",
            "05_escaner_geologico_entorno.lua",
            "06_suite_termica_flir.lua"
    );

    public static void init() {
        LuaScriptEngine.init();
        loadAllModules();
    }

    public static synchronized void loadAllModules() {
        for (LuaCustomModule mod : LOADED_MODULES.values()) {
            mod.updateActiveState(false);
        }
        LOADED_MODULES.clear();
        activeVisionModeModule = null;

        try {
            Path dirPath = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
            File dir = dirPath.toFile();

            if (!dir.exists()) {
                dir.mkdirs();
            }

            File guide = new File(dir, "00_GUIA_PASO_A_PASO.txt");
            if (!guide.exists()) {
                generateExampleScripts(dir);
            }

            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".lua"));
            if (files == null || files.length == 0) {
                generateExampleScripts(dir);
                files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".lua"));
            }

            if (files != null) {
                for (File file : files) {
                    loadModuleFile(file);
                }
            }

            Vision_goggles.LOGGER.info("[Vision Goggles] Loaded " + LOADED_MODULES.size() + " Lua module(s).");
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Error scanning Lua modules directory: " + e.getMessage(), e);
        }
    }

    private static void loadModuleFile(File file) {
        try {
            String content = Files.readString(file.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (content.startsWith("\uFEFF")) {
                content = content.substring(1);
            }
            SandboxedDebugLib.resetInstructionCount();
            LuaScriptEngine.getGlobals().set("module", LuaValue.NIL);
            LuaValue chunk = LuaScriptEngine.getGlobals().load(content, file.getName());
            chunk.call();

            LuaValue moduleObj = LuaScriptEngine.getGlobals().get("module");
            if (moduleObj.istable()) {
                LuaCustomModule customMod = LuaCustomModule.fromLuaTable((LuaTable) moduleObj, file.getName());
                LOADED_MODULES.put(customMod.getId(), customMod);
                Vision_goggles.LOGGER.info("[Vision Goggles] Registered Lua module: " + customMod.getId() + " (" + customMod.getName() + ") [" + customMod.getType() + "]");
                
                // If this module is a VISION_MODE, set as active candidate
                if ("VISION_MODE".equalsIgnoreCase(customMod.getType()) && activeVisionModeModule == null) {
                    activeVisionModeModule = customMod;
                }
            } else {
                Vision_goggles.LOGGER.warn("[Vision Goggles] File " + file.getName() + " executed but did not define global 'module = { ... }'.");
            }
        } catch (LuaError error) {
            Vision_goggles.LOGGER.error("[Vision Goggles Lua Compile Error in " + file.getName() + "] " + error.getMessage());
        } catch (Throwable e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to read or execute " + file.getName() + ": " + e.getMessage(), e);
        }
    }

    public static Collection<LuaCustomModule> getLoadedModules() {
        return Collections.unmodifiableCollection(LOADED_MODULES.values());
    }

    public static LuaCustomModule getModule(ResourceLocation id) {
        return LOADED_MODULES.get(id);
    }

    public static LuaCustomModule findModuleByIdOrPath(String idOrPath) {
        if (idOrPath == null || idOrPath.isEmpty()) return null;
        String clean = idOrPath.contains(":") ? idOrPath.split(":")[1] : idOrPath;
        for (Map.Entry<ResourceLocation, LuaCustomModule> entry : LOADED_MODULES.entrySet()) {
            if (entry.getKey().toString().equalsIgnoreCase(idOrPath) || entry.getKey().getPath().equalsIgnoreCase(clean)) {
                return entry.getValue();
            }
            if ("thread_scanner".equalsIgnoreCase(clean) && "threat_scanner".equalsIgnoreCase(entry.getKey().getPath())) {
                return entry.getValue();
            }
        }
        return null;
    }

    public static LuaCustomModule getActiveVisionModeModule() {
        return activeVisionModeModule;
    }

    public static void setActiveVisionModeModule(LuaCustomModule module) {
        if (activeVisionModeModule != module) {
            if (activeVisionModeModule != null) activeVisionModeModule.callOnDisable();
            activeVisionModeModule = module;
            if (activeVisionModeModule != null) activeVisionModeModule.callOnEnable();
        }
    }

    public static void reload() {
        Vision_goggles.LOGGER.info("[Vision Goggles] Reloading Lua modules...");
        loadAllModules();
    }

    private static void generateExampleScripts(File dir) {
        try {
            for (String fileName : DEFAULT_RESOURCE_FILES) {
                File target = new File(dir, fileName);
                if (target.exists()) {
                    continue;
                }
                String resourcePath = "/assets/vision_goggles/lua_examples/" + fileName;
                try (InputStream in = LuaModuleManager.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        Vision_goggles.LOGGER.warn("[Vision Goggles] Missing bundled Lua example resource: " + resourcePath);
                        continue;
                    }
                    Files.copy(in, target.toPath());
                }
            }
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to create example scripts and guide: " + e.getMessage());
        }
    }
}
