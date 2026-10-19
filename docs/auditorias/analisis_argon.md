# Auditoría Técnica Exhaustiva Post-Plan v21 — Vision Goggles (1.20.1 Forge + Fabric Architectury)

**Fecha de Auditoría:** 18 de Octubre de 2026  
**Auditor:** Gemini 4 Argon (High)  
**Repositorio:** `Vision Goggles` (Minecraft 1.20.1 — Architectury Common + Forge + Fabric)  
**Commit Auditado:** `8080a242720edf83e670a301db5aaad61eaa7289` (*feat: completar plan v21 de pulido integral pre-lanzamiento*)  
**Calificación Final para Lanzamiento Público (Modrinth / CurseForge):** **6.4 / 10** (*Condicionado — Requiere Hotfix v22 antes de publicar*)

---

## 0. Resumen Ejecutivo y Evolución tras el Plan v21

La aplicación del **Plan v21** corrigió con éxito varios de los problemas reportados en la auditoría preliminar (`auditoria_pre_release.md`), destacando:
1. **Estación de Modificación (`ModificationStationBlock.java:85-104`):** Limpieza explícita del Slot 2 (`setItem(2, ItemStack.EMPTY)`) antes de soltar el inventario al romper el bloque, y bloqueo de caras en `WorldlyContainer` (`SLOTS_NONE`).
2. **Paridad básica en Fabric (`fabric/PlatformMethodsImpl.java:70-73` y `Vision_gogglesFabricClient.java:22`):** Fallback al slot `EquipmentSlot.HEAD` vanilla e inicialización de `FabricLayerRegistry.init()`.
3. **Corrección de Relación de Aspecto 16:9 / 21:9:** Integración del uniforme `AspectRatio` en `nvg.fsh`, `thermal.fsh`, `hydro.fsh`, `hydro_dry.fsh` y `bio.fsh`.
4. **Particionado del escaneo de Spawn Security (`VisionWorldRenderer.java:208-226`):** Rebanado vertical (`scanSpawnSlice`) en `clientTick` (radio 24×8×24, 3 capas Y por tick) en lugar de bloquear el hilo de renderizado.
5. **Eliminación de librerías nativas peligrosas en Lua (`LuaScriptEngine.java:24-32`):** Remoción de `luajava`, `os`, `io`, `package`, `loadfile`, `dofile` y sustitución por `SandboxedDebugLib`.

Sin embargo, una inspección estática y de flujo de ejecución **línea por línea** demuestra que **la implementación del Plan v21 dejó cabos sueltos críticos e introdujo regresiones funcionales** que impedirían un lanzamiento limpio en servidores dedicados y supervivencia pública:
* `LuaClientBridge` fue creado en el Plan v21, pero **nunca se eliminaron los métodos con firmas `net.minecraft.client.*` de `LuaModuleManager` ni `LuaCustomModule`**, por lo que el servidor dedicado **sigue crasheando con `NoClassDefFoundError`** al interactuar con chips Lua en la Estación de Modificación o al usar `/vg`.
* El límite de instrucciones de `SandboxedDebugLib` **no se reinicia entre frames/ticks** (rompiendo los módulos Lua legítimos tras pocos segundos de uso) y **puede evadirse al 100% con `pcall` / `xpcall` / `coroutine`**.
* El bloque `modification_station` **carece de Loot Table y del tag `mineable/pickaxe`**, perdiéndose para siempre al picarlo en supervivencia.
* En Fabric, la **Visión Térmica no resalta ninguna entidad** (`ThermalEntityPainter` nunca se invoca en Fabric) y la **Linterna Táctica no se puede equipar en Accessories** (faltan los JSON del slot `body`).

---

## 1. Seguridad en Servidores Dedicados (Client/Server Separation y Red)

**Calificación del Área:** **4.5 / 10**

### 1.1. [CRÍTICO - P0] Crasheo en Servidor Dedicado (`NoClassDefFoundError`) por firmas de Cliente residuales en `LuaModuleManager` y `LuaCustomModule`
* **Archivos y Líneas:**
  * [`LuaModuleManager.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L5-L6) y [líneas 126–159](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L126-L159)
  * [`LuaCustomModule.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L94-L106) y [líneas 162–183](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L162-L183)
  * [`ModificationStationBlockEntity.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L54-L59) y [líneas 156–173](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L156-L173)
  * [`CommandVisionGoggles.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L31-L48)
* **Diagnóstico Técnico:**
  En el Plan v21 se creó [`LuaClientBridge.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/scripting/LuaClientBridge.java#L35-L85), pero **no se retiraron los métodos de cliente de `LuaModuleManager` ni de `LuaCustomModule`**:
  ```java
  // LuaModuleManager.java:5-6, 126-143
  import net.minecraft.client.Minecraft;
  import net.minecraft.client.gui.GuiGraphics;
  ...
  public static void onClientTick(Minecraft mc, float batteryPct) { ... }
  public static void onRenderHud(GuiGraphics g, float partialTick) { ... }
  ```
  ```java
  // LuaCustomModule.java:94, 162
  public boolean canRun(net.minecraft.client.Minecraft mc) { ... }
  public void callUpdateUniforms(java.util.List<net.minecraft.client.renderer.PostPass> passes) { ... }
  ```
  Cuando el servidor dedicado ejecuta `ModificationStationBlockEntity.updateResult()` (al colocar un `ProgrammableModuleItem`) o cuando se invoca `/vg reload`, `/vg list` o el autocompletado de `/vg givescript`, el `ClassLoader` del servidor carga `LuaModuleManager` y `LuaCustomModule`. Al resolver los descriptores de método que contienen `net.minecraft.client.gui.GuiGraphics`, `net.minecraft.client.Minecraft` y `net.minecraft.client.renderer.PostPass`, el servidor colapsa con `java.lang.NoClassDefFoundError`.
* **Además:** `LuaModuleManager.init()` solo se invoca desde [`ModClient.init()`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ModClient.java#L19), nunca desde `Vision_goggles.init()`. En un servidor dedicado `LOADED_MODULES` está vacío al arrancar, por lo que `LuaModuleManager.findModuleByIdOrPath(scriptId)` devuelve `null` hasta que alguien ejecute `/vg reload`.

### 1.2. [CRÍTICO - P0] `CommandVisionGoggles.java`: Ausencia Total de Permisos (`hasPermission`) y Ejecución de `openFile` en el Host del Servidor
* **Archivo:** [`CommandVisionGoggles.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L24-L191)
* **Diagnóstico Técnico:**
  1. **Cero control de permisos:** A diferencia de `/vgdebug` ([`ModEvents.java:33`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L33)), el árbol `/vg` no incluye `.requires(source -> source.hasPermission(2))`. Cualquier jugador sin OP en supervivencia puede:
     * Ejecutar `/vg givescript <id>` ([líneas 113–179](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L113-L179)) o pulsar el botón `"📥 Obtener Script"` en [`LuaScriptViewerScreen.java:95`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/gui/LuaScriptViewerScreen.java#L95) para obtener infinitos `InstallationScriptItem` gratis en Survival.
     * Ejecutar `/vg slots modular <1..16>` o `/vg slots pro <1..16>` ([líneas 63–102](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L63-L102)), alterando la configuración global del servidor y forzando escritura en disco (`ModConfig.save()`).
     * Ejecutar `/vg run <código>` ([líneas 103–112](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L103-L112)), corriendo código Lua arbitrario directamente en el **Server Thread** del servidor dedicado.
  2. **Apertura de ventana de archivos en el servidor físico (`/vg folder`, [líneas 51–62](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L51-L62)):** Al estar registrado en `CommandRegistrationEvent` (servidor), `Util.getPlatform().openFile(dir.toFile())` se ejecuta en el sistema operativo de la máquina host del servidor dedicado cuando un cliente remoto escribe `/vg folder`.

### 1.3. [ALTO - P1] `Vision_gogglesForge.java` y `VisionGogglesItem.java`: Referencias Directas a Clases de Cliente en Clases Comunes/Servidor
* **[`Vision_gogglesForge.java:4, 27, 114-119`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/forge/Vision_gogglesForge.java#L114-L119):**
  La clase principal `@Mod` importa `dev.itsrealperson.vision_goggles.client.ModClient` y registra `modEventBus.addListener(this::clientSetup)` con un método de instancia que referencia directamente `ModClient.init()` y `ThermalEntityPainter.init()`. Aunque el Plan v21 aisló `Vision_gogglesForgeClient.registerConfigScreen()`, dejó `clientSetup` en la clase principal en lugar de moverlo a `ClientModEvents` (`@Mod.EventBusSubscriber(..., value = Dist.CLIENT)`).
* **[`VisionGogglesItem.java:160-188`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/VisionGogglesItem.java#L160-L188):**
  La clase anónima dentro de `createRenderer` declara un campo `private VisionGogglesGeoRenderer renderer;` y el método `public BlockEntityWithoutLevelRenderer getCustomRenderer()`. Ambos tipos pertenecen exclusivamente al cliente.
* **[`OpenLuaScreenPacket.java:17-24`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/OpenLuaScreenPacket.java#L17-L24):**
  El lambda `context.queue(() -> ClientGuiHelper.openLuaScreen())` compila un método sintético `lambda$handle$0` dentro de `OpenLuaScreenPacket` que referencia `ClientGuiHelper`, la cual está anotada con `@Environment(EnvType.CLIENT)` ([`ClientGuiHelper.java:7`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/gui/ClientGuiHelper.java#L7)). En Fabric Server, las clases anotadas con `@Environment(EnvType.CLIENT)` son eliminadas por el loader, haciendo insegura la verificación de `OpenLuaScreenPacket`.

### 1.4. [ALTO - P1] Vulnerabilidades en Paquetes de Red (C2S / S2C)
1. **Inyección de cualquier ítem en Curios/Accessories y fallo de Offhand (`EquipPacket` + `PlatformMethodsImpl`):**
   * En [`EquipPacket.java:21-25`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/EquipPacket.java#L21-L25):
     ```java
     if (PlatformMethods.equipInSlot(player, player.getMainHandItem())) {
     } else if (PlatformMethods.equipInSlot(player, player.getOffhandItem())) {
     }
     ```
   * En [`forge/PlatformMethodsImpl.java:14-32`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/util/forge/PlatformMethodsImpl.java#L14-L32) y [`fabric/PlatformMethodsImpl.java:14-30`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/java/dev/itsrealperson/vision_goggles/util/fabric/PlatformMethodsImpl.java#L14-L30), `equipInSlot(Player player, ItemStack stack)` **no verifica `if (stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem)) return false;`** al inicio.
   * **Impacto doble:**
     * Si el jugador lleva las gafas en la **mano secundaria (Offhand)** y la mano principal vacía (`ItemStack.EMPTY`), la primera llamada `equipInSlot(player, player.getMainHandItem())` encuentra un slot de Curios/Accessories vacío, escribe `ItemStack.EMPTY`, devuelve `true` y **jamás equipa las gafas de la mano secundaria**.
     * Si un cliente envía `EquipPacket` sosteniendo cualquier ítem (ej. una Shulker Box o un bloque de tierra), el servidor lo inserta sin validación dentro del slot `eyes`/`face` de Curios/Accessories.
2. **`SyncFlashlightPacket` (S2C) aceptado desde el Cliente (C2S) sin validar módulo ni batería:**
   * En [`SyncFlashlightPacket.java:44-70`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/SyncFlashlightPacket.java#L44-L70), si el paquete llega al servidor (`context.getEnv() != EnvType.CLIENT`) con `sender.getId() == this.entityId`, el servidor **continúa ejecutando `context.queue(...)`** y escribe `TAG_FLASHLIGHT_ACTIVE = true` en las gafas del jugador sin verificar si tiene instalado `ModuleType.FLASHLIGHT` ni si tiene batería `> 0`. Al ser un paquete exclusivamente S2C, debe hacer `return;` inmediato en el servidor igual que [`BatterySyncPacket.java:33`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/BatterySyncPacket.java#L33).
3. **`ConfigSyncPacket` y Deserialización JSON sin sanitización (`ModConfig.applyCommonJson`):**
   * [`ConfigSyncPacket.java:28-38`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/ConfigSyncPacket.java#L28-L38) es un paquete S2C, pero si un cliente OP lo envía al servidor, sobrescribe `ModConfig.common` sin guardar ni validar rangos. Debe bloquearse en servidor con `if (context.getEnv() != EnvType.CLIENT) return;`.
   * En [`ModConfig.java:84-91`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/ModConfig.java#L84-L91), `GSON.fromJson(json, CommonConfig.class)` puede asignar `common = null` si el JSON es `"null"`, o dejar `common.extraBatteryItems = null` causando `NullPointerException` en `updateBatteryMap()` ([línea 177](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/ModConfig.java#L177)). Además, los getters `getNvgDuration()`, `getThermalDuration()`, etc. ([líneas 138–142](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/ModConfig.java#L138-L142)) no aplican `Math.max(1, ...)`, permitiendo división por cero si se configuran en `0`.

---

## 2. Seguridad y Sandbox en Lua (`SandboxedDebugLib`, `LuaScriptEngine`, `LuaHudCanvas`, `LuaClientBridge`)

**Calificación del Área:** **5.0 / 10**

### 2.1. [CRÍTICO - P0] Regresión Funcional: El Contador de Instrucciones Nunca se Reinicia en el Ciclo de Render/Tick
* **Archivos y Líneas:**
  * [`SandboxedDebugLib.java:11-24`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/SandboxedDebugLib.java#L11-L24)
  * [`LuaScriptEngine.java:82-93`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaScriptEngine.java#L82-L93)
  * [`LuaCustomModule.java:138-183`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L138-L183)
  * [`LuaModuleManager.java:69-71`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L69-L71)
* **Diagnóstico Técnico:**
  `SandboxedDebugLib.resetInstructionCount()` **únicamente** se llama dentro de `LuaScriptEngine.executeString(String code)` ([línea 84](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaScriptEngine.java#L84)).
  **Jamás se llama** antes de:
  * `chunk.call()` en `LuaModuleManager.loadModuleFile()`
  * `callOnEnable()` / `callOnDisable()`
  * `callOnTick()`
  * `callOnRenderHud()`
  * `callUpdateUniforms()`
  * `evaluateThermalColor()`
  Como resultado, durante una partida normal con un módulo Lua activo, el `ThreadLocal<Integer> INSTRUCTION_COUNT` se acumula frame tras frame hasta alcanzar `100,000` instrucciones (en pocos segundos a 60 FPS) y lanza `LuaError` a mitad de un frame legítimo.

### 2.2. [CRÍTICO - P0] Evasión Total del Watchdog de CPU (`SandboxedDebugLib`) mediante `pcall` / `xpcall` / `coroutine`
* **Archivo:** [`SandboxedDebugLib.java:16-24`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/SandboxedDebugLib.java#L16-L24)
  ```java
  @Override
  public void onInstruction(int pc, Varargs varargs, int top) {
      int count = INSTRUCTION_COUNT.get() + 1;
      if (count > MAX_INSTRUCTIONS) {
          INSTRUCTION_COUNT.set(0);
          throw new LuaError("Script execution exceeded limit of " + MAX_INSTRUCTIONS + " instructions (potential infinite loop or DoS).");
      }
      INSTRUCTION_COUNT.set(count);
      super.onInstruction(pc, varargs, top);
  }
  ```
* **Diagnóstico Técnico:**
  1. `JsePlatform.standardGlobals()` expone `pcall`, `xpcall` y `coroutine`.
  2. En LuaJ, `pcall` captura cualquier excepción de tipo `LuaError`.
  3. Como `SandboxedDebugLib` hace `INSTRUCTION_COUNT.set(0)` antes de lanzar `LuaError`, un script malicioso con:
     ```lua
     while true do pcall(function() while true do end end) end
     ```
     atrapa el `LuaError` con el contador ya reseteado a `0` y congela el cliente o el servidor para siempre.
  4. **Solución requerida:** Lanzar una subclase de `java.lang.Error` que **no** herede de `LuaError` (p.ej. `ScriptQuotaExceededError extends Error`, capturada en Java por `catch (Throwable t)`), o bien eliminar `pcall`/`xpcall`/`coroutine` del sandbox y no reiniciar `INSTRUCTION_COUNT` dentro de `onInstruction`.

### 2.3. [CRÍTICO - P0] Congelamiento del Render Thread y Desbordamiento de Enteros en `LuaHudCanvas`
* **Archivo:** [`LuaHudCanvas.java:138-204`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L138-L204)
* **Diagnóstico Técnico:**
  Aunque el Plan v21 añadió `MAX_FRAME_COMMANDS = 500`, el trabajo pesado ocurre **dentro de cada lambda en `render()` (fuera de la VM de Lua y fuera de `SandboxedDebugLib`)** sin acotar dimensiones ni radios:
  * **`drawCircle(cx, cy, radius, color)` ([líneas 165–185](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L165-L185)):** Calcula `int segments = Math.max(16, radius * 2);`. Una sola llamada `hud.drawCircle(0, 0, 500000000, "#FFFFFF")` ejecuta un bucle de **1,000,000,000 de iteraciones** en el hilo de renderizado, congelando Minecraft instantáneamente.
  * **`drawDisc(cx, cy, radius, color)` ([líneas 188–204](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L188-L204)):** Itera `for (int dy2 = -radius; dy2 <= radius; dy2++)` y calcula `radius * radius` en `int` de 32 bits (que desborda a negativo si `radius > 46340`, produciendo `Math.sqrt(negativo) -> NaN`). Con `radius = 100000000`, ejecuta 200 millones de llamadas a `g.fill()`.
  * **`drawLine(x1, y1, x2, y2, color)` ([líneas 138–162](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L138-L162)):** Calcula `dx * dx + dy * dy` en `int` (overflow si `|dx| > 46340`) y dibuja un `g.fill` por cada píxel de longitud (`steps`).

### 2.4. [ALTO - P1] Vectores Adicionales de DoS, Bytecode y Mutación de Configuración en el Sandbox Lua
1. **Inyección de Bytecode Binario (`load` y `string.dump`) en [`LuaScriptEngine.java:21-33`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaScriptEngine.java#L21-L33):** `globals` conserva `load` (que por defecto acepta bytecode binario `\027Lua`) y `string.dump`. En LuaJ 3.0.1, el bytecode binario malformado puede provocar `ArrayIndexOutOfBoundsException` o `ClassCastException` no controladas en la JVM.
2. **Modificación de Configuración del Juego y Spam de Disco desde Lua (`goggles.setMaxModules`):**
   * En [`LuaWorldScanner.java:467-481`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaWorldScanner.java#L467-L481) y [`LuaCustomModule.java:72-77`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L72-L77), cualquier script Lua puede alterar `modularMaxModules` / `proModularMaxModules` sin cota y llamar a `ModConfig.save()` en cada tick/frame, saturando el disco con escrituras JSON síncronas.
3. **Consultas de Mundo sin Límite de Radio en [`LuaWorldScanner.java:35-118`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaWorldScanner.java#L35-L118):**
   * `world.getEntitiesInRadius(radius)`, `world.getTargetEntity(range)` y `world.raycastBlock(range)` no acotan `radius`/`range` (p.ej. a `64.0`) ni verifican `Double.isNaN`. Pasar `1e7` o `math.huge` congela el cliente buscando en millones de chunks/bloques en una sola instrucción Lua.
4. **Agotamiento de Heap (`OutOfMemoryError`) con `string.rep("A", 1e9)`:** Consume una sola instrucción Lua y agota la memoria de la JVM.
5. **Estado Global Compartido (`Globals`):** `LuaModuleManager.loadModuleFile` ([línea 72](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L72)) no limpia `globals.set("module", LuaValue.NIL)` antes de cargar cada archivo, y `getmetatable("")` permite mutar el metatable global de todos los strings.

---

## 3. Supervivencia y Duplicación (`ModificationStation`, Recetas, NBT y JEI)

**Calificación del Área:** **6.5 / 10**

### 3.1. [CRÍTICO - P0] Pérdida Permanente de la Estación de Modificación al Romperla (Falta Loot Table y Tag `mineable/pickaxe`)
* **Archivo:** [`ModBlocks.java:17-18`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/registry/ModBlocks.java#L17-L18)
  ```java
  public static final RegistrySupplier<Block> MODIFICATION_STATION = BLOCKS.register("modification_station",
          () -> new ModificationStationBlock(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).strength(4.0f).requiresCorrectToolForDrops().noOcclusion()));
  ```
* **Diagnóstico Técnico:**
  No existen en `common/src/main/resources/data/` ni la tabla de botín `vision_goggles/loot_tables/blocks/modification_station.json` ni el tag `minecraft/tags/blocks/mineable/pickaxe.json`. Debido a `.requiresCorrectToolForDrops()`, el bloque tarda ~20 segundos en romperse con cualquier pico y **nunca dropea el ítem `modification_station`** en supervivencia.

### 3.2. [CRÍTICO - P0] Exploit de "Visión Fantasma" al Extraer Módulos en la Estación de Modificación
* **Archivos y Líneas:**
  * [`ModularGogglesItem.java:241-255`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/ModularGogglesItem.java#L241-L255) (`removeModule`)
  * [`VisionRenderer.java:164-175`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L164-L175)
  * [`VisionGogglesItem.java:76-98`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/VisionGogglesItem.java#L76-L98)
* **Diagnóstico Técnico:**
  Cuando se extrae un módulo en la Estación de Modificación (`ModularGogglesItem.removeModule(slot0, lastMod)`), el método solo elimina el string de la lista NBT `Modules`, pero **no actualiza `VisionMode`, `Active`, `FlashlightOn` ni recorta `Battery` al nuevo máximo**:
  1. Un jugador instala `thermal_module` en unas `ModularGoggles`, activa la visión térmica (`VisionMode = 1`, `Active = true`), se quita las gafas, las mete en la Estación de Modificación con el Slot 1 vacío y **extrae el `thermal_module`**.
  2. Las gafas quedan con `0` módulos instalados, pero conservan `VisionMode = 1` y `Active = true` en su NBT.
  3. Al volver a equiparlas, `VisionRenderer.onRenderTick()` lee `nbtModeId = helmet.getOrCreateTag().getInt(ModConstants.TAG_MODE)` **sin comprobar si las gafas aún poseen ese módulo**, otorgando **Visión Térmica/NVG/Hydro/Bio permanente sin módulo** y permitiendo compartir un único módulo entre infinitos jugadores o gafas.
  4. De igual forma, si se extrae `battery_expansion_module` de unas gafas cargadas al 100%, `TAG_BATTERY` permanece por encima de la capacidad máxima base (150% de carga).

### 3.3. [ALTO - P1] Duplicación por Automatización de Mods (`Container.removeItem`) y Persistencia del Slot 2 en NBT
* **Archivo:** [`ModificationStationBlockEntity.java:31, 251-283, 305-314`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L251-L314)
* **Diagnóstico Técnico:**
  1. Aunque `getSlotsForFace` devuelve `SLOTS_NONE` (bloqueando tolvas vanilla), `removeItem(int slot, int amount)` y `removeItemNoUpdate(int slot)` de la interfaz base `Container` permiten extraer el Slot 2 directamente sin llamar a `onTakeOutput()`. Cualquier mod de transporte o tubería que consulte `Container` sin `WorldlyContainer` extraerá la previsualización del Slot 2 infinitamente sin consumir los Slots 0 y 1.
  2. `saveAdditional` guarda la lista `items` de tamaño 3 en el NBT del chunk, persistiendo innecesariamente el Slot 2 virtual en disco.
  3. En `onTakeOutput` ([líneas 262–263](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L262-L263)), `InstallationScriptItem` nunca se consume (diseño intencional como disquete maestro reutilizable, pero combinado con `/vg givescript` sin permisos carece de coste en supervivencia).

### 3.4. [ALTO - P1] Pérdida del `battery_expansion_module` en `GogglesUpgradeRecipe`
* **Archivos:**
  * [`pro_modular_goggles_recipe.json:10-12`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/vision_goggles/recipes/pro_modular_goggles_recipe.json#L10-L12)
  * [`GogglesUpgradeRecipe.java:17-29`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/recipe/GogglesUpgradeRecipe.java#L17-L29)
* **Diagnóstico Técnico:**
  La receta de mejora de `modular_goggles` a `pro_modular_goggles` consume un `vision_goggles:battery_expansion_module` en la mesa de crafteo, pero `GogglesUpgradeRecipe.assemble()` solo copia el NBT de las `modular_goggles` de entrada sin instalar el módulo `battery_expansion` en el resultado (o destruyéndolo si las gafas ya tenían uno instalado).

### 3.5. [BAJO - P2] Discrepancia de Prefijo NBT en Receta de Ejemplo de JEI
* **Archivo:** [`VisionGogglesJeiPluginCommon.java:105`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/compat/jei/VisionGogglesJeiPluginCommon.java#L105)
* **Diagnóstico Técnico:**
  En la categoría 3 de JEI (*Installing programmed script chip onto Goggles*), se invoca `modular.installModule(result, "threat_scanner")` sin el prefijo `"custom:"`, mientras que `ModificationStationBlockEntity.java:176` instala `"custom:" + scriptId`.

---

## 4. Paridad Fabric / Forge (`PlatformMethodsImpl`, 3D Layers, Curios / Accessories)

**Calificación del Área:** **5.5 / 10**

### 4.1. [CRÍTICO - P0] Visión Térmica de Entidades 100% Inoperativa en Fabric
* **Archivos y Líneas:**
  * [`Vision_gogglesForge.java:93-97, 117`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/forge/Vision_gogglesForge.java#L93-L97)
  * [`Vision_gogglesFabricClient.java:19-46`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/java/dev/itsrealperson/vision_goggles/fabric/client/Vision_gogglesFabricClient.java#L19-L46)
  * [`MixinLevelRenderer.java:12-18`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/mixin/client/MixinLevelRenderer.java#L12-L18)
* **Diagnóstico Técnico:**
  En Forge, `ThermalEntityPainter.init()` se llama en `FMLClientSetupEvent` y `ThermalEntityPainter.captureAndComposite(event.getPoseStack())` se llama en `RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES`.
  En Fabric, **no existe ninguna llamada a `ThermalEntityPainter.init()` ni a `ThermalEntityPainter.captureAndComposite()` en todo el módulo Fabric ni en `MixinLevelRenderer`**. Además, `HeatSilhouetteLayer.render()` ([`HeatSilhouetteLayer.java:21-24`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/HeatSilhouetteLayer.java#L21-L24)) hace `return;` inmediato.
  **Consecuencia:** En Fabric, la Visión Térmica tiñe el mundo de azul frío pero **jamás renderiza las siluetas térmicas de ningún mob ni jugador**.

### 4.2. [CRÍTICO - P0] `TacticalFlashlightItem` No Se Puede Equipar en Accessories en Fabric y Sobrescritura de `player.json`
* **Archivos:**
  * `common/src/main/resources/data/curios/entities/player.json` (define `["eyes", "body"]`)
  * `common/src/main/resources/data/curios/tags/items/body.json` (incluye `vision_goggles:tactical_flashlight`)
  * `common/src/main/resources/data/accessories/entities/player.json` (define `["accessories:eyes"]`)
  * `fabric/src/main/resources/data/accessories/entities/player.json` (sobrescribe con `["accessories:face"]`)
* **Diagnóstico Técnico:**
  1. Faltan por completo las definiciones de tag y slot `body` (o `chest`/`belt`) para Accessories en Fabric (`data/accessories/tags/items/body.json`), por lo que la `TacticalFlashlightItem` **no puede equiparse en el torso en Fabric**.
  2. El archivo `fabric/src/main/resources/data/accessories/entities/player.json` pisa al de `common`, eliminando `accessories:eyes` y dejando solo `accessories:face`.

### 4.3. [CRÍTICO - P0] Batería Infinita y Estroboscópico Inactivo cuando `TacticalFlashlightItem` está Equipada en Curios / Accessories
* **Archivos y Líneas:**
  * [`TacticalFlashlightItem.java:127-147`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/TacticalFlashlightItem.java#L127-L147)
  * [`ModEvents.java:97-105`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L97-L105)
* **Diagnóstico Técnico:**
  La lógica de consumo de batería y disuasión estroboscópica (`FlashlightDeterrence.apply`) de `TacticalFlashlightItem` reside en `Item.inventoryTick()`. Minecraft vanilla solo invoca `inventoryTick` en los 36 slots de inventario, 4 de armadura y 1 de offhand; Curios y Accessories **no llaman a `Item.inventoryTick()`**.
  Mientras que `ModEvents.tickGoggles` ([`ModEvents.java:98-105`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L98-L105)) consulta `PlatformMethods.getEquippedHelmet(player)` en `TickEvent.PLAYER_POST`, **no hace lo mismo para `PlatformMethods.getEquippedFlashlight(player)`**.
  **Consecuencia:** Equipada en el chaleco (Curios/Accessories), la linterna táctica **tiene batería infinita y su estroboscópico nunca ciega ni ralentiza a los mobs**.

### 4.4. [ALTO - P1] Desincronización Multijugador de `TacticalFlashlightItem`
* **Archivos:** [`ToggleFlashlightPacket.java:58-64`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/ToggleFlashlightPacket.java#L58-L64), [`CycleFlashlightModePacket.java:55-60`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/CycleFlashlightModePacket.java#L55-L60) y [`SyncFlashlightPacket.java:63`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/SyncFlashlightPacket.java#L63).
* **Diagnóstico Técnico:** Cuando un jugador enciende/apaga o cambia el modo de una `TacticalFlashlightItem` equipada en el pecho (Curios/Accessories), el servidor no envía `SyncFlashlightPacket` a los demás jugadores, y `SyncFlashlightPacket` solo actualiza `getEquippedHelmet(targetPlayer)`, por lo que el resto de jugadores en el servidor no ven encenderse la linterna de pecho de otros jugadores en Curios/Accessories.

---

## 5. Shaders y Rendimiento (GLSL, `VisionShaderManager`, `VisionWorldRenderer`, `ThermalEntityPainter`)

**Calificación del Área:** **7.2 / 10**

### 5.1. [ALTO - P1] 90% de los Raycasts de Linterna por Frame son Código Muerto en los Shaders GLSL
* **Archivos y Líneas:**
  * [`VisionRenderer.java:511-534`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L511-L534)
  * [`ShaderRewriter.java:64`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/ShaderRewriter.java#L64)
  * [`SodiumShaderPatcher.java:83`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/SodiumShaderPatcher.java#L83)
* **Diagnóstico Técnico:**
  En cada frame (`updateFlashlightEveryFrame`), por cada linterna activa se ejecutan **9 raycasts (`performRaycast`) + 1 `checkIsWall` = 10 `level.clip()`** para rellenar `VisionFlashlightsRange0..8` y `VisionFlashlightsIsWall`.
  Sin embargo, tanto `ShaderRewriter.java:64` como `SodiumShaderPatcher.java:83` contienen únicamente:
  ```glsl
  float distLimit = VisionFlashlightsRange0[i];
  ```
  Los uniformes `VisionFlashlightsRange1` a `VisionFlashlightsRange8`, `VisionFlashlightsIsWall`, `VisionFlashlightsUp`, `VisionFlashlightsRight` y `VisionFlashlightsRange` se calculan en CPU en cada frame y se suben por OpenGL en cada draw call, pero **jamás se leen en el código GLSL**. Eliminar los 9 raycasts y uniformes muertos reduce en un **90%** el coste de CPU de la linterna dinámica.

### 5.2. [ALTO - P1] Cuello de Botella en `ThermalEntityPainter`: Hasta 2,304 Consultas de Bloques por Entidad por Frame y Oclusión de Profundidad Incompleta
* **Archivo:** [`ThermalEntityPainter.java:146-147, 170, 197-225, 347, 421-476`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ThermalEntityPainter.java#L197-L225)
* **Diagnóstico Técnico:**
  1. **Coste en Render Thread:** Por cada `LivingEntity` en un radio de 64 bloques, lanza 9 rayos llamando a `getThermalAttenuation()`, el cual avanza de `0.25` en `0.25` bloques (hasta 256 pasos por rayo = **2,304 llamadas a `level.getBlockState()` por entidad en cada frame**). Con 25 entidades cercanas, realiza más de **50,000 lecturas de bloques por frame**.
  2. **Oclusión visual parcial a través de muros:** `ThermalEntityPainter` usa un `depthRenderbuffer` propio limpio ([línea 170](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ThermalEntityPainter.java#L170)) sin copiar el depth buffer del mundo (`mc.getMainRenderTarget().getDepthTextureId()`). Si una entidad asoma un píxel por la esquina de una pared (pasando 1 de los 9 rayos), toda su silueta 3D se dibuja por encima del muro sólido.

### 5.3. [ALTO - P1] Reflexión sin Caché en `SodiumChunkUniformMixin` y `glGetUniformLocation` sin Caché en `FlashlightUniforms`
* **Archivos y Líneas:**
  * [`SodiumChunkUniformMixin.java:20-35`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/mixin/sodium/SodiumChunkUniformMixin.java#L20-L35)
  * [`FlashlightUniforms.java:131-178`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/FlashlightUniforms.java#L131-L178)
* **Diagnóstico Técnico:**
  1. `SodiumChunkUniformMixin` invoca `this.getClass().getMethod("handle")` por reflexión en cada `GlProgram.bind()` de Sodium/Embeddium sin cachear el `Method`/`Field`.
  2. `FlashlightUniforms.applyToVanilla(ShaderInstance)` se ejecuta en cada `ShaderInstance.apply()` de Minecraft (cientos de veces por frame) haciendo 20 llamadas a `GL20.glGetUniformLocation` sin caché y subiendo todos los arreglos incluso cuando `currentCount == 0`.
  3. En [`VisionShaderManager.java:409`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionShaderManager.java#L409), `isHoldingLight` ejecuta `String nbt = stack.getTag().toString().toLowerCase();` en cada frame para los ítems en mano de cada jugador, generando presión innecesaria en el Garbage Collector.

### 5.4. [MEDIO - P2] Detalles en Shaders GLSL (`nvg.fsh` y `thermal.fsh`)
* **[`nvg.fsh:31-40`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/assets/vision_goggles/shaders/program/nvg.fsh#L31-L40):** Realiza 3 lecturas `texture(DiffuseSampler, ...)` dentro de `if (focus > 0.0)` asignadas a `r, g, b` que nunca se utilizan porque las líneas 67–71 vuelven a declarar y muestrear `r, g, b`.
* **[`thermal.fsh:149`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/assets/vision_goggles/shaders/program/thermal.fsh#L149):** El uniforme `Time` se envía en **ticks** (`mc.level.getGameTime() + mc.getFrameTime()`), pero el comentario y cálculo `float nucCycle = mod(time, 35.0); if (nucCycle < 0.06)` asumen segundos (ejecutándose cada 1.75 s durante 3 ms en lugar de cada 35 s).

---

## 6. Calificación Final e Imparcial para Lanzamiento Público (Modrinth / CurseForge)

| Categoría Auditada | Peso | Nota Pre-v21 | Nota Post-v21 | Veredicto Técnico |
| :--- | :---: | :---: | :---: | :--- |
| **1. Seguridad en Servidores Dedicados** | 25% | 2.0 / 10 | **4.5 / 10** | Persiste `NoClassDefFoundError` en `LuaModuleManager`/`LuaCustomModule` y `/vg` carece de permisos OP. |
| **2. Seguridad y Sandbox en Lua** | 20% | 2.5 / 10 | **5.0 / 10** | Se aislaron librerías nativas, pero falta `resetInstructionCount()` por frame, `pcall` evade el límite y `drawCircle` permite DoS. |
| **3. Supervivencia y Duplicación** | 20% | 7.5 / 10 | **6.5 / 10** | Se corrigió el drop del Slot 2, pero falta la Loot Table de la estación y extraer módulos deja el modo de visión activo gratis. |
| **4. Paridad Fabric / Forge** | 15% | 5.0 / 10 | **5.5 / 10** | Funciona el casco vanilla y la capa 3D en Fabric, pero la visión térmica de entidades y el slot de linterna en Accessories no funcionan en Fabric. |
| **5. Shaders, Estética y Rendimiento** | 20% | 6.8 / 10 | **7.2 / 10** | Excelente estética 16:9 y `VisionWorldRenderer` optimizado; quedan raycasts muertos en linterna y cuello de botella en `ThermalEntityPainter`. |
| **CALIFICACIÓN GLOBAL PONDERADA** | **100%** | **5.7 / 10** | **6.4 / 10** | **NO APTO AÚN PARA LANZAMIENTO PÚBLICO SIN HOTFIX v22** |

### Hoja de Ruta Prioritaria (Checklist Exacto para Alcanzar 9.5+ / 10 en un Plan v22)
1. **Aislamiento definitivo de `LuaModuleManager` y `LuaCustomModule` (P0):** Eliminar los métodos con tipos `Minecraft`, `GuiGraphics` y `PostPass` de ambas clases (redirigiendo todas las llamadas de cliente a `LuaClientBridge`), e invocar `LuaModuleManager.init()` en `Vision_goggles.init()`.
2. **Permisos y lado correcto en `/vg` (P0):** Añadir `.requires(source -> source.hasPermission(2))` a `/vg reload`, `/vg slots`, `/vg run` y `/vg givescript`; eliminar o mover `/vg folder` exclusivamente al cliente.
3. **Hardening real del Sandbox Lua (P0):** Llamar a `SandboxedDebugLib.resetInstructionCount()` antes de cada callback de `LuaCustomModule`; lanzar una subclase de `Error` no interceptable por `pcall`; y acotar `radius` (`<= 512`) y longitud de línea en `LuaHudCanvas`, así como `radius`/`range` (`<= 64.0`) en `LuaWorldScanner`.
4. **Supervivencia y Anti-Exploit de Módulos (P0):** Crear `loot_tables/blocks/modification_station.json` y `tags/blocks/mineable/pickaxe.json`; y en `ModularGogglesItem.removeModule()` resetear `TAG_MODE`, `TAG_ACTIVE`, `TAG_FLASHLIGHT_ON` y recortar `TAG_BATTERY` al nuevo máximo.
5. **Paridad Térmica y de Linterna en Fabric (P0):** Llamar a `ThermalEntityPainter.init()` y `ThermalEntityPainter.captureAndComposite()` en Fabric (`WorldRenderEvents.AFTER_ENTITIES` o `MixinLevelRenderer`), añadir los archivos JSON del slot `body` en `data/accessories`, y hacer tick de `PlatformMethods.getEquippedFlashlight(player)` en `ModEvents.tickGoggles`.
