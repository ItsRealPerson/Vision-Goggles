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
import java.nio.file.StandardCopyOption;
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

    private static final List<String> LEGACY_EXAMPLE_FILES = List.of(
            "01_visor_cibernetico.lua",
            "02_hud_militar_brujula.lua",
            "03_detector_amenazas.lua",
            "04_filtro_termico_colores.lua",
            "05_vision_fosforo_blanco.lua",
            "06_modulo_programable_radar.lua",
            "07_hud_tactico_avanzado.lua",
            "08_nvg_adaptativo.lua",
            "09_bio_scanner_avanzado.lua",
            "10_geo_scanner_minero.lua",
            "11_predator_thermal_lock.lua",
            "12_visor_abissal_hydro.lua"
    );

    private static volatile boolean initialized = false;

    public static void init() {
        LuaScriptEngine.init();
        initialized = true;
        loadAllModules();
    }

    public static synchronized void syncExternalScriptIfInitialized(String fileName, String luaSource) {
        if (!initialized) {
            return;
        }
        try {
            Path modulesDir = Platform.getConfigFolder().resolve("vision_goggles").resolve("modules");
            Path scriptsDir = Platform.getConfigFolder().resolve("vision_goggles").resolve("scripts");
            Files.createDirectories(modulesDir);
            Files.createDirectories(scriptsDir);

            Path moduleTarget = modulesDir.resolve(fileName);
            Files.writeString(moduleTarget, luaSource, java.nio.charset.StandardCharsets.UTF_8);
            Path scriptTarget = scriptsDir.resolve(fileName);
            Files.writeString(scriptTarget, luaSource, java.nio.charset.StandardCharsets.UTF_8);

            loadModuleFile(moduleTarget.toFile());
        } catch (Exception e) {
            Vision_goggles.LOGGER.warn("[Vision Goggles] Failed to sync external Lua script " + fileName + ": " + e.getMessage());
        }
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

            removeLegacyExampleScripts(dir);
            generateExampleScripts(dir);

            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".lua"));
            if (files != null) {
                Arrays.sort(files, Comparator.comparing(File::getName));
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

    private static void removeLegacyExampleScripts(File dir) {
        for (String legacyName : LEGACY_EXAMPLE_FILES) {
            File legacyFile = new File(dir, legacyName);
            if (legacyFile.exists() && legacyFile.isFile()) {
                try {
                    Files.deleteIfExists(legacyFile.toPath());
                    Vision_goggles.LOGGER.info("[Vision Goggles] Removed obsolete legacy Lua example: " + legacyName);
                } catch (Exception e) {
                    Vision_goggles.LOGGER.warn("[Vision Goggles] Could not remove legacy Lua example " + legacyName + ": " + e.getMessage());
                }
            }
        }
    }

    private static final String BUNDLED_VERSION_MARKER = "-- [VG-BUNDLED v37]";

    private static boolean shouldUpgradeBundledScript(File target) {
        try {
            String content = Files.readString(target.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (content.contains("-- [CUSTOM]") || content.contains("-- [USER-CUSTOM]")) {
                return false;
            }
            String header = content.length() > 600 ? content.substring(0, 600) : content;
            if (header.contains(BUNDLED_VERSION_MARKER)) {
                return false;
            }
            if (header.contains("-- [VG-BUNDLED ")) {
                return true;
            }
            return (header.startsWith("-- ============================================================================")
                    || header.startsWith("\uFEFF-- ============================================================================"))
                    && header.contains("-- EJEMPLO ");
        } catch (Exception e) {
            return false;
        }
    }

    public static void generateExampleScripts(Path dirPath) {
        if (dirPath != null) {
            generateExampleScripts(dirPath.toFile());
        }
    }

    private static void generateExampleScripts(File dir) {
        try {
            for (String fileName : DEFAULT_RESOURCE_FILES) {
                File target = new File(dir, fileName);
                boolean isGuide = "00_GUIA_PASO_A_PASO.txt".equals(fileName);
                boolean isUpgrading = target.exists() && !isGuide && shouldUpgradeBundledScript(target);
                if (target.exists() && !isGuide && !isUpgrading) {
                    continue;
                }
                String resourcePath = "/assets/vision_goggles/lua_examples/" + fileName;
                try (InputStream in = LuaModuleManager.class.getResourceAsStream(resourcePath)) {
                    if (in == null) {
                        Vision_goggles.LOGGER.warn("[Vision Goggles] Missing bundled Lua example resource: " + resourcePath);
                        continue;
                    }
                    Files.copy(in, target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    if (isUpgrading) {
                        Vision_goggles.LOGGER.info("[Vision Goggles] Upgraded bundled Lua example to v37: " + fileName);
                    }
                }
            }

            Map<String, String> externalScripts = dev.itsrealperson.vision_goggles.api.VisionGogglesAPI.getRegisteredExternalLuaScripts();
            if (!externalScripts.isEmpty()) {
                Path scriptsDir = Platform.getConfigFolder().resolve("vision_goggles").resolve("scripts");
                Files.createDirectories(scriptsDir);
                for (Map.Entry<String, String> entry : externalScripts.entrySet()) {
                    File moduleTarget = new File(dir, entry.getKey());
                    if (!moduleTarget.exists() || shouldUpgradeBundledScript(moduleTarget)) {
                        Files.writeString(moduleTarget.toPath(), entry.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                    Path scriptTarget = scriptsDir.resolve(entry.getKey());
                    if (!Files.exists(scriptTarget)) {
                        Files.writeString(scriptTarget, entry.getValue(), java.nio.charset.StandardCharsets.UTF_8);
                    }
                }
            }
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to create example scripts and guide: " + e.getMessage());
        }
    }
}
