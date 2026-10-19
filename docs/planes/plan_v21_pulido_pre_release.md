# Plan v21 — Pulido Integral Pre-Lanzamiento (Calidad Producción 9.8/10)

## 1. Contexto y Objetivos
A raíz de la auditoría exhaustiva pre-lanzamiento (`docs/auditorias/auditoria_pre_release.md`), se detectaron bloqueadores críticos en estabilidad de servidores dedicados, duplicación de ítems en supervivencia, vulnerabilidades de seguridad en el motor Lua, paridad en Fabric y rendimiento gráfico.

Este plan resuelve el 100% de los hallazgos en 5 fases continuas:
* **Fase 1: Estabilidad Total en Servidor Dedicado (Multiplayer Safe)**
  * Aislamiento estricto de clases de cliente (`Minecraft.getInstance()`, GUI, `ConfigScreenHandler`).
  * Desacoplamiento de `LuaModuleManager` y `LuaCustomModule` de clases gráficas en servidor.
* **Fase 2: Corrección de Exploits de Duplicación y Seguridad de Red**
  * Bloqueo de caída del slot 2 en `ModificationStationBlock.onRemove()`.
  * Prohibición de extracción por tolvas en slot 2 (`canTakeItemThroughFace = false`).
  * Validación autoritativa en paquetes de red (`BatterySyncPacket`, `SyncFlashlightPacket`, `ConfigSyncPacket`).
* **Fase 3: Sandbox Estricto y Watchdog de CPU en Motor Lua**
  * Eliminación de `luajava`, `os`, `io`, `package`, `loadfile` y `dofile`.
  * Hook de límite de instrucciones en Luaj para evitar congelamientos por bucles infinitos.
  * Límite de comandos gráficos por frame y registro de errores con logger.
* **Fase 4: Paridad Completa en Fabric**
  * Soporte para el slot de casco vanilla en `fabric/PlatformMethodsImpl.java`.
  * Invocación de `FabricLayerRegistry.init()` en `Vision_gogglesFabricClient.onInitializeClient()`.
* **Fase 5: Corrección de Relación de Aspecto y Optimización Gráfica**
  * Integración de `AspectRatio` en `nvg.fsh`, `thermal.fsh`, `hydro.fsh` y `bio.fsh`.
  * Optimización del escaneo de bloques en `VisionWorldRenderer.updateSpawnBlocks`.
