package dev.itsrealperson.vision_goggles.scripting;

import dev.architectury.platform.Platform;
import dev.itsrealperson.vision_goggles.Vision_goggles;
import net.minecraft.resources.ResourceLocation;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;

import java.io.File;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LuaModuleManager {

    private static final Map<ResourceLocation, LuaCustomModule> LOADED_MODULES = new LinkedHashMap<>();
    private static LuaCustomModule activeVisionModeModule = null;

    public static void init() {
        LuaScriptEngine.init();
        loadAllModules();
    }

    public static synchronized void loadAllModules() {
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
        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to read " + file.getName() + ": " + e.getMessage(), e);
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
            // Documentación Completa Paso a Paso
            File guideFile = new File(dir, "00_GUIA_PASO_A_PASO.txt");
            try (FileWriter w = new FileWriter(guideFile)) {
                w.write("""
================================================================================
          GUÍA Y MANUAL DE SCRIPTING EN LUA - VISION GOGGLES
================================================================================

Bienvenido al sistema de scripting en Lua de Vision Goggles.
Todos los archivos con extensión '.lua' colocados en esta carpeta son cargados
automáticamente por el juego. Puedes editarlos en vivo y usar el comando '/vg reload'
en el chat para aplicar cambios al instante sin reiniciar Minecraft.

--------------------------------------------------------------------------------
1. ESTRUCTURA BÁSICA DE UN SCRIPT
--------------------------------------------------------------------------------
Cada archivo '.lua' debe definir una tabla global llamada 'module':

module = {
    id = "mi_mod:mi_visor",           -- Identificador único
    name = "Nombre en Pantalla",      -- Nombre legible
    type = "VISION_MODE",             -- Tipo: "VISION_MODE", "HUD" o "UTILITY"
    shader = "shaders/post/nvg.json", -- (Opcional) Ruta al shader JSON
    battery_drain = 0.5,              -- (Opcional) Drenaje de batería por segundo

    -- Se ejecuta al encender el visor
    onEnable = function(player)
        sound.playBeep(1.5)
        hud.showAlert("MODO ACTIVADO", "#00FFAA", 2.0)
    end,

    -- Se ejecuta al apagar el visor
    onDisable = function(player)
        sound.playBeep(0.7)
    end,

    -- Se ejecuta cada tick de juego (20 veces por segundo)
    onTick = function(player, battery_pct)
        -- Tu lógica aquí
    end,

    -- Se ejecuta cada frame gráfico para inyectar variables a la GPU (Shaders)
    onUpdateUniforms = function(player, uniforms)
        uniforms.setVec3("colorFilter", 0.0, 1.0, 0.8)
    end,

    -- Se ejecuta al renderizar el HUD en pantalla
    onRenderHud = function(hud)
        hud.drawText(10, 10, "ESTADO: OK", "#00FF00", 1.0)
    end,

    -- Se ejecuta para decidir el color de un mob en visión térmica
    onThermalColor = function(entityType, isHostile)
        return isHostile and "#FF1100" or "#00FF66"
    end
}

--------------------------------------------------------------------------------
2. REFERENCIA COMPLETA DE FUNCIONES (APIs)
--------------------------------------------------------------------------------

[ API: hud ]
* hud.getWidth() -> Número (Ancho de la pantalla en píxeles de GUI)
* hud.getHeight() -> Número (Alto de la pantalla en píxeles de GUI)
* hud.drawText(x, y, "texto", "#RRGGBB", escala) -> Dibuja texto
* hud.drawRect(x, y, ancho, alto, "#RRGGBB") -> Dibuja rectángulo sólido
* hud.drawProgressBar(x, y, ancho, alto, progreso_0_a_1, "#RRGGBB") -> Barra
* hud.showAlert("MENSAJE", "#RRGGBB", duracion_segundos) -> Banner parpadeante

[ API: world ]
* world.getTargetEntity(rango) -> Devuelve el mob al que apuntas:
    - target.name (Nombre del mob)
    - target.type (ID: ej. 'minecraft:creeper')
    - target.health (Vida actual)
    - target.max_health (Vida máxima)
    - target.armor (Puntos de armadura)
    - target.distance (Distancia en bloques)
    - target.is_hostile (true si es hostil)
    - target.is_player (true si es un jugador)
* world.getEntitiesInRadius(radio) -> Lista de todos los mobs cercanos
* world.raycastBlock(rango) -> Bloque al que apuntas:
    - block.name (ID del bloque)
    - block.light_level (Luz de 0 a 15)
    - block.can_see_sky (true si da al cielo)
    - block.distance (Distancia)

[ API: player ]
* player.getX(), player.getY(), player.getZ() -> Coordenadas
* player.getYaw(), player.getPitch() -> Ángulos de mirada (Dirección)
* player.getHealth(), player.getMaxHealth() -> Vida
* player.isInWater() -> true si está en agua
* player.isDay() -> true si es de día

[ API: sound ]
* sound.playBeep(tono) -> Pitido táctico (tono de 0.5 a 2.0)
* sound.playAlert() -> Sonido de advertencia / alarma
* sound.playClick() -> Sonido de clic
* sound.play("minecraft:sonido", tono, volumen) -> Cualquier sonido del juego

[ API: uniforms ] (Solo disponible dentro de onUpdateUniforms)
* uniforms.setFloat("nombre", valor)
* uniforms.setVec2("nombre", x, y)
* uniforms.setVec3("nombre", r, g, b)
* uniforms.setVec4("nombre", r, g, b, a)

[ Utilidades Globales ]
* getTime() -> Tiempo actual del juego en segundos continuos (para animaciones)
* printLog("texto") -> Imprime en la consola/log de Minecraft

--------------------------------------------------------------------------------
3. COMANDOS DENTRO DEL JUEGO
--------------------------------------------------------------------------------
* /vg reload -> Recarga todos los scripts modificados al instante.
* /vg run <código> -> Ejecuta Lua en vivo desde el chat.
  Ejemplo: /vg run hud.showAlert("SISTEMA LISTO", "#00FFAA", 3.0)
================================================================================
""");
            }

            // Ejemplo 1: Visor Cibernético con Shaders Reactivos
            File cyberFile = new File(dir, "01_visor_cibernetico.lua");
            try (FileWriter w = new FileWriter(cyberFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 1: VISOR CIBERNÉTICO CON SHADER GLSL REACTIVO
-- ============================================================================
-- En este ejemplo usamos un shader nativo ('nvg.json') y modificamos su tinte
-- dinámicamente: la pantalla es cian neón, pero si el jugador recibe daño
-- o tiene poca vida (< 6 HP), el shader se torna en alerta roja parpadeante.
-- ============================================================================

module = {
    id = "vision_goggles:cyber_visor",
    name = "Visor Cibernético Neón",
    type = "VISION_MODE",
    shader = "shaders/post/nvg.json",
    required_module = "nvg", -- Hardware requerido: Modulo NVG (Night Vision)
    battery_drain = 0.5,

    onEnable = function(player)
        sound.playBeep(1.6)
        hud.showAlert("SISTEMA CIBERNÉTICO EN LÍNEA", "#00FFAA", 2.0)
    end,

    onDisable = function(player)
        sound.playBeep(0.8)
    end,

    -- Control de variables de GPU en tiempo real (cada cuadro renderizado)
    onUpdateUniforms = function(player, uniforms)
        local hp = player.getHealth()

        if hp < 6.0 then
            -- Alerta roja si la salud es crítica
            uniforms.setVec3("colorFilter", 1.0, 0.15, 0.15)
        else
            -- Color cian neón táctico normal
            uniforms.setVec3("colorFilter", 0.05, 0.95, 0.75)
        end
    end
}
""");
            }

            // Ejemplo 2: HUD Militar con Brújula y Altímetro
            File hudFile = new File(dir, "02_hud_militar_brujula.lua");
            try (FileWriter w = new FileWriter(hudFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 2: HUD MILITAR CON BRÚJULA ANGULAR Y ALTÍMETRO
-- ============================================================================
-- En este ejemplo dibujamos elementos 2D en el visor:
-- 1. Una brújula con rumbo numérico (0° a 360°) en el centro superior.
-- 2. La altitud del jugador en metros en la esquina inferior izquierda.
-- 3. Una barra de salud visual estilizada en el visor.
-- ============================================================================

module = {
    id = "vision_goggles:tactical_hud",
    name = "HUD Militar Táctico",
    type = "HUD",
    -- Modulo autonomo (sin requisito de hardware previo)

    onRenderHud = function(h)
        local screenW = h.getWidth()
        local screenH = h.getHeight()
        local midX = screenW / 2

        -- 1. Brújula en grados en la parte superior central
        local yaw = math.floor((player.getYaw() % 360 + 360) % 360)
        h.drawRect(midX - 35, 10, 70, 14, "#88000000") -- Fondo negro semitransparente
        h.drawText(midX - 25, 13, "HDG: " .. string.format("%03d°", yaw), "#00FFCC", 1.0)

        -- 2. Altímetro en la esquina inferior
        local y = math.floor(player.getY())
        h.drawText(12, screenH - 45, "ALT: " .. y .. "m", "#AAAAAA", 0.9)

        -- 3. Barra de vida visual en el visor
        local hpPct = player.getHealth() / player.getMaxHealth()
        local hpColor = hpPct < 0.3 and "#FF2222" or "#00FFAA"
        h.drawProgressBar(12, screenH - 30, 80, 5, hpPct, hpColor)
    end
}
""");
            }

            // Ejemplo 3: Escáner de Amenazas con Raycast y Alerta
            File scannerFile = new File(dir, "03_detector_amenazas.lua");
            try (FileWriter w = new FileWriter(scannerFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 3: ESCÁNER DE ENTIDADES Y ALERTA DE PROXIMIDAD
-- ============================================================================
-- Este módulo demuestra dos funciones de análisis del entorno:
-- 1. Si miras a un mob, muestra en pantalla su nombre, vida y armadura.
-- 2. Monitorea cada 3 segundos si hay Creepers o Wardens cerca y avisa con sonido.
-- ============================================================================

local lastCheck = 0

module = {
    id = "vision_goggles:threat_scanner",
    name = "Escáner Biológico",
    type = "UTILITY",
    required_module = "bio", -- Hardware requerido: Modulo Bio (Biometric)

    onTick = function(p, battery)
        local t = getTime()
        if t - lastCheck > 3.0 then
            -- Buscar mobs en un radio de 10 bloques
            local nearby = world.getEntitiesInRadius(10)
            for i = 1, #nearby do
                local ent = nearby[i]
                if ent.type == "minecraft:creeper" or ent.type == "minecraft:warden" then
                    sound.playAlert()
                    hud.showAlert("¡ALERTA: " .. ent.name:upper() .. " CERCANO!", "#FF1100", 2.0)
                    lastCheck = t
                    break
                end
            end
        end
    end,

    onRenderHud = function(h)
        -- Analizar el mob al que estamos apuntando directamente con la mira
        local target = world.getTargetEntity(25)
        if target ~= nil then
            local midX = h.getWidth() / 2
            local midY = h.getHeight() / 2

            h.drawRect(midX - 45, midY + 12, 90, 24, "#99000000")
            h.drawText(midX - 40, midY + 15, target.name, "#FFFFFF", 0.9)
            local hp = math.floor(target.health * 10 + 0.5) / 10
            local maxHp = math.floor(target.max_health * 10 + 0.5) / 10
            local hpText = "HP: " .. hp .. " / " .. maxHp
            h.drawText(midX - 40, midY + 25, hpText, target.is_hostile and "#FF4444" or "#44FF44", 0.8)
        end
    end
}
""");
            }

            // Ejemplo 4: Filtro de Colores Térmicos
            File thermalFile = new File(dir, "04_filtro_termico_colores.lua");
            try (FileWriter w = new FileWriter(thermalFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 4: PERSONALIZACIÓN DE COLORES EN EL VISOR TÉRMICO
-- ============================================================================
-- Permite que la visión térmica use colores tácticos diferenciados:
-- - Monstruos hostiles -> Rojo brillante (#FF1100)
-- - Jugadores aliados -> Cian cibernético (#00FFFF)
-- - Animales pacíficos -> Verde cálido (#33FF66)
-- ============================================================================

module = {
    id = "vision_goggles:thermal_palette",
    name = "Paleta Térmica Táctica",
    type = "UTILITY",
    required_module = "thermal", -- Hardware requerido: Modulo Termico (Thermal)

    onThermalColor = function(entityType, isHostile)
        if entityType == "minecraft:player" then
            return "#00FFFF" -- Jugadores en cian
        elseif isHostile then
            return "#FF1100" -- Hostiles en rojo
        else
            return "#33FF66" -- Animales en verde
        end
    end
}
""");
            }

            // Ejemplo 5: Visor de Fósforo Blanco Táctico
            File phosphorFile = new File(dir, "05_vision_fosforo_blanco.lua");
            try (FileWriter w = new FileWriter(phosphorFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 5: VISOR DE FÓSFORO BLANCO (WHITE PHOSPHOR NVG)
-- ============================================================================
-- Aplica el tono militar moderno de fósforo blanco (luz azulada/blanca fría),
-- con pitido de encendido y mensaje en el visor.
-- ============================================================================

module = {
    id = "vision_goggles:white_phosphor",
    name = "Fósforo Blanco Táctico",
    type = "VISION_MODE",
    shader = "shaders/post/nvg.json",
    required_module = "nvg", -- Hardware requerido: Modulo NVG (Night Vision)
    battery_drain = 0.6,

    onEnable = function(player)
        sound.playBeep(1.8)
        hud.showAlert("SISTEMA: FÓSFORO BLANCO", "#E0F7FA", 1.5)
    end,

    onUpdateUniforms = function(player, uniforms)
        -- Tinte de alta resolución fósforo blanco
        uniforms.setVec3("colorFilter", 0.88, 0.94, 1.0)
    end
}
""");
            }

            // Ejemplo 6: Módulo Físico Programable con Script de Instalación
            File progFile = new File(dir, "06_modulo_programable_radar.lua");
            try (FileWriter w = new FileWriter(progFile)) {
                w.write("""
-- ============================================================================
-- EJEMPLO 6: MÓDULO FÍSICO PROGRAMABLE CON SCRIPT DE INSTALACIÓN
-- ============================================================================
-- Este módulo demuestra el sistema de flasheo físico de chips:
-- 1. Consigue un 'Script de Instalación' con el comando:
--    /vg givescript radar_amenazas "Radar Táctico" #00FFAA
-- 2. En la Estación de Modificación:
--    - Slot Izquierdo: Módulo Programable (en blanco)
--    - Slot Central: Script de Instalación
--    - Slot Derecho: ¡Obtienes el Módulo Programado!
-- 3. Luego instala el Módulo Programado en tus Gafas Modulares (o Pro).
-- ============================================================================

module = {
    id = "vision_goggles:radar_amenazas",
    name = "Radar Táctico de Amenazas",
    type = "UTILITY",
    -- Modulo autonomo (sin requisito de hardware previo)

    onRenderHud = function(h)
        local x = 12
        local y = h.getHeight() - 48

        h.drawRect(x, y, 115, 20, "#BB0a121d")
        h.drawText(x + 5, y + 4, "[RADAR TÁCTICO ON]", "#00FFAA", 0.85)

        -- Escanear amenazas en 15 bloques
        local nearby = world.getEntitiesInRadius(15)
        local hostileCount = 0
        for i = 1, #nearby do
            if nearby[i].is_hostile then
                hostileCount = hostileCount + 1
            end
        end

        local countColor = hostileCount > 0 and "#FF3333" or "#88FF88"
        h.drawText(x + 5, y + 12, "Amenazas cerca: " .. hostileCount, countColor, 0.75)
    end
}
""");
            }

        } catch (Exception e) {
            Vision_goggles.LOGGER.error("[Vision Goggles] Failed to create example scripts and guide: " + e.getMessage());
        }
    }
}
