# Plan v22 — Hotfix y Estabilización Definitiva Post-Auditoría Argon

## 1. Contexto y Objetivos
A partir de la auditoría técnica profunda e imparcial de Gemini 4 Argon (`docs/auditorias/analisis_argon.md`, nota 6.4/10), se establecieron los 5 pilares necesarios para alcanzar la calificación de excelencia (9.8+ / 10) para publicación en Modrinth y CurseForge:

* **Área 1: Aislamiento Definitivo de Servidor y Permisos en `/vg` (P0)**
  * Retirar de `LuaModuleManager.java` y `LuaCustomModule.java` cualquier método con firmas de cliente (`Minecraft`, `GuiGraphics`, `PostPass`), centralizando el cliente 100% en `LuaClientBridge.java`.
  * Invocar `LuaModuleManager.init()` en `Vision_goggles.init()` para que el servidor dedicado reconozca los módulos.
  * Añadir `.requires(source -> source.hasPermission(2))` a los comandos administrativos `/vg reload`, `/vg slots`, `/vg run` y `/vg givescript`.

* **Área 2: Hardening del Sandbox Lua y Prevención de Congelamientos (P0)**
  * Invocar `SandboxedDebugLib.resetInstructionCount()` antes de cada callback de `LuaCustomModule` (`callOnTick`, `callOnRenderHud`, `callUpdateUniforms`, `evaluateThermalColor`).
  * Evitar la evasión del límite con `pcall` mediante una excepción no capturable por la VM de Lua (`ScriptQuotaExceededException extends RuntimeException`).
  * Acotar radios y coordenadas en `LuaHudCanvas.java` (`radius <= 512`, `length <= 1920`) y en `LuaWorldScanner.java` (`radius <= 64.0`).

* **Área 3: Supervivencia, Loot Table y Anti-Visión Fantasma (P0)**
  * Crear `data/vision_goggles/loot_tables/blocks/modification_station.json` y `data/minecraft/tags/blocks/mineable/pickaxe.json`.
  * En `ModularGogglesItem.removeModule()`: apagar `TAG_ACTIVE` y resetear `TAG_MODE` si el módulo activo fue retirado, apagar la linterna si se extrajo, y recortar `TAG_BATTERY` al nuevo tope máximo.
  * En `GogglesUpgradeRecipe.java`: asegurar que `pro_modular_goggles` conserve e instale `battery_expansion`.

* **Área 4: Paridad Fabric y Linterna Táctica en Torso (P0)**
  * Integrar `ThermalEntityPainter` en Fabric mediante `WorldRenderEvents.AFTER_ENTITIES`.
  * Crear tags y slot de Accessories en `data/accessories/tags/items/body.json` y `data/accessories/slots/body.json`.
  * Sincronizar el tick de servidor de la linterna táctica equipada en Curios/Accessories dentro de `ModEvents.java`.

* **Área 5: Rendimiento de Linterna en Shaders (P1)**
  * Eliminar los 9 raycasts muertos no leídos por los shaders GLSL en `VisionRenderer.updateFlashlightEveryFrame`.
