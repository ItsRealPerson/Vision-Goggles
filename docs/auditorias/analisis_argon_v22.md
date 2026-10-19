# Auditoría Técnica Exhaustiva Post-Plan v22 — Vision Goggles (1.20.1 Forge + Fabric Architectury)

**Fecha de Auditoría:** 18 de Octubre de 2026  
**Auditor:** Gemini 4 Argon (High)  
**Repositorio:** `Vision Goggles` (Minecraft 1.20.1 — Architectury Common + Forge + Fabric)  
**Commit Auditado:** `a4d595f` (*feat: completar plan v22 hotfix definitivo post-auditoria argon*)  
**Calificación Final para Lanzamiento Público (Modrinth / CurseForge):** **7.9 / 10 Global** (**8.5 / 10 en Forge** | **6.2 / 10 en Fabric**)

---

## 0. Resumen Ejecutivo: Evolución de v21 (6.4/10) a v22 (7.9/10)

La aplicación del **Plan v22 (`a4d595f` y `59c1099`)** resolvió de manera efectiva la gran mayoría de los bloqueadores estructurales reportados en la auditoría anterior (`docs/auditorias/analisis_argon.md`):
1. **Aislamiento de `LuaModuleManager` y `LuaCustomModule`:** Se erradicaron todos los imports y descriptores de métodos con clases de cliente (`Minecraft`, `GuiGraphics`, `PostPass`), delegando el ciclo de vida visual a [`LuaClientBridge.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/scripting/LuaClientBridge.java) e inicializando `LuaModuleManager.init()` desde [`Vision_goggles.init()`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/Vision_goggles.java#L33).
2. **Control de permisos OP en `/vg`:** Los subcomandos administrativos (`/vg reload`, `/vg slots`, `/vg run`, `/vg givescript`) ahora exigen `.requires(source -> source.hasPermission(2))` en [`CommandVisionGoggles.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L30-L122).
3. **Hardening del Sandbox Lua:** Se introdujo `ScriptQuotaExceededError extends Error` (no interceptable por `pcall`), se añadió `SandboxedDebugLib.resetInstructionCount()` antes de cada callback en [`LuaCustomModule.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L124-L174), y se acotaron los radios geométricos en [`LuaHudCanvas.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L147-L205) (`radius <= 512`, `steps <= 1920`) y [`LuaWorldScanner.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaWorldScanner.java#L41-L96) (`radius <= 64.0`).
4. **Supervivencia y Mejora Pro:** Se crearon [`modification_station.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/vision_goggles/loot_tables/blocks/modification_station.json) y [`pickaxe.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json), se corrigió la preservación/instalación de `battery_expansion` en [`GogglesUpgradeRecipe.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/recipe/GogglesUpgradeRecipe.java#L33-L40), y se añadió limpieza de estado y recorte de batería en [`ModularGogglesItem.removeModule()`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/ModularGogglesItem.java#L257-L307).
5. **Optimización de Raycasts de Linterna:** Se redujeron de 10 a **1 único raycast por linterna por frame** en [`VisionRenderer.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L508-L512) (-90% coste de CPU en colisiones de haz) y se añadió el tick de servidor para la linterna en el torso en [`ModEvents.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L122-L126).

No obstante, la auditoría profunda post-v22 revela **4 problemas críticos (P0) muy puntuales (3 de ellos exclusivos del empaquetado/ciclo de vida de Fabric y 1 en la carga inicial de archivos `.lua`)** junto con hallazgos secundarios P1/P2 que deben conocerse y corregirse antes de subir los archivos finales a Modrinth y CurseForge.

---

## 1. Aislamiento de Servidor Dedicado y Permisos en `/vg`

**Calificación del Área:** **8.2 / 10** *(Subió desde 4.5 / 10 en v21)*

### 1.1. Verificación de Correcciones del Plan v22 (Aprobadas)
* **Desacoplamiento de `LuaModuleManager` y `LuaCustomModule`:**
  * [`LuaModuleManager.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L1-L508) y [`LuaCustomModule.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L1-L191) ya no contienen ninguna referencia a `net.minecraft.client.Minecraft`, `GuiGraphics` ni `PostPass`.
  * [`Vision_goggles.java:33`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/Vision_goggles.java#L33) invoca `LuaModuleManager.init()` tanto en cliente como en servidor dedicado, permitiendo que `ModificationStationBlockEntity` y `/vg` reconozcan los módulos en servidores headless.
* **Permisos en `/vg` ([`CommandVisionGoggles.java`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L24-L163)):**
  * `/vg reload` ([línea 31](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L31)), `/vg slots` ([línea 69](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L69)), `/vg run` ([línea 110](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L110)) y `/vg givescript` ([línea 121](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L121)) están protegidos con `.requires(source -> source.hasPermission(2))`.
  * `/vg folder` ([líneas 54–57](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L54-L57)) aborta de forma segura cuando `Platform.getEnv() != EnvType.CLIENT`.

### 1.2. Hallazgos Residuales en Servidor Dedicado y Red

#### [ALTO - P1] Referencias directas a clases de cliente en `VisionGogglesItem$1` y `Vision_gogglesForge`
* **Archivos y Líneas:**
  * [`VisionGogglesItem.java:160-183`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/VisionGogglesItem.java#L160-L183)
  * [`Vision_gogglesForge.java:4, 27, 114-119`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/forge/Vision_gogglesForge.java#L114-L119)
  * [`LuaSoundSystem.java:18`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaSoundSystem.java#L18)
* **Diagnóstico Técnico:**
  1. En `VisionGogglesItem.createRenderer`, la clase anónima `VisionGogglesItem$1` declara el campo `private VisionGogglesGeoRenderer renderer;` y el método `public BlockEntityWithoutLevelRenderer getCustomRenderer()`. En un servidor dedicado, si cualquier mod o escáner de clases inspecciona los métodos de `VisionGogglesItem$1`, la JVM lanza `NoClassDefFoundError: net/minecraft/client/renderer/BlockEntityWithoutLevelRenderer`.
  2. En `Vision_gogglesForge.java`, la clase principal `@Mod` sigue importando `ModClient` ([línea 4](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/forge/Vision_gogglesForge.java#L4)) y registrando `modEventBus.addListener(this::clientSetup)` ([línea 27](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/forge/Vision_gogglesForge.java#L27)) en lugar de ubicar `onClientSetup` dentro de `ClientModEvents` (`value = Dist.CLIENT`).
  3. `LuaSoundSystem.java` reside en el paquete común `scripting/` e importa `Minecraft` y `SimpleSoundInstance`, pero le falta la anotación `@Environment(EnvType.CLIENT)` que sí tienen `LuaHudCanvas`, `LuaWorldScanner` y `LuaUniformBridge`.

#### [ALTO - P1] Validación ausente en `EquipPacket` / `PlatformMethodsImpl` y `SyncFlashlightPacket`
* **Archivos y Líneas:**
  * [`EquipPacket.java:21-25`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/EquipPacket.java#L21-L25)
  * [`forge/PlatformMethodsImpl.java:14-32`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/forge/src/main/java/dev/itsrealperson/vision_goggles/util/forge/PlatformMethodsImpl.java#L14-L32) y [`fabric/PlatformMethodsImpl.java:14-30`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/java/dev/itsrealperson/vision_goggles/util/fabric/PlatformMethodsImpl.java#L14-L30)
  * [`SyncFlashlightPacket.java:44-71`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/network/SyncFlashlightPacket.java#L44-L71)
* **Diagnóstico Técnico:**
  1. `PlatformMethodsImpl.equipInSlot(player, stack)` no comprueba `if (stack.isEmpty() || !(stack.getItem() instanceof VisionGogglesItem)) return false;`. Si el jugador tiene la mano principal vacía y las gafas en la Offhand, `equipInSlot(player, player.getMainHandItem())` escribe `ItemStack.EMPTY` en el slot de Curios/Accessories y devuelve `true`, impidiendo equipar desde la Offhand y permitiendo inyectar ítems arbitrarios mediante `EquipPacket`.
  2. `SyncFlashlightPacket` es un paquete S2C, pero si un cliente lo envía al servidor con `this.entityId == sender.getId()`, el servidor no hace `return;` en la línea 52 y continúa hacia `context.queue(...)`, activando `TAG_FLASHLIGHT_ACTIVE = true` en el servidor sin validar si las gafas tienen instalado el módulo de linterna ni si tienen batería.

#### [BAJO - P2] `/vg folder` en Servidor Integrado (LAN)
* **Archivo:** [`CommandVisionGoggles.java:52-66`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/command/CommandVisionGoggles.java#L52-L66)
* **Diagnóstico Técnico:** En una partida abierta en LAN, `Platform.getEnv() == EnvType.CLIENT` es `true` para el hilo del servidor integrado. Como `/vg folder` no tiene `.requires(s -> s.hasPermission(2))`, un jugador invitado en LAN puede ejecutar `/vg folder` y abrirá el Explorador de Archivos en el PC del anfitrión.

---

## 2. Hardening del Sandbox Lua

**Calificación del Área:** **8.0 / 10** *(Subió desde 5.0 / 10 en v21)*

### 2.1. Verificación de Correcciones del Plan v22 (Aprobadas)
* **Reinicio de cuota por callback (`SandboxedDebugLib.resetInstructionCount()`):**
  * Verificado en [`LuaCustomModule.java:125, 132, 139, 146, 153, 162`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaCustomModule.java#L124-L174). Cada ejecución de `onEnable`, `onDisable`, `onTick`, `onRenderHud`, `onUpdateUniforms` y `onThermalColor` reinicia el contador `ThreadLocal` a `0`, eliminando la muerte prematura de los scripts tras pocos segundos.
* **Protección frente a `pcall` (`ScriptQuotaExceededError extends Error`):**
  * Verificado en [`SandboxedDebugLib.java:10-26`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/SandboxedDebugLib.java#L10-L26). En LuaJ 3.0.1, `BaseLib$pcall` únicamente atrapa `LuaError` y `Exception`, por lo que `ScriptQuotaExceededError` (`java.lang.Error`) atraviesa limpiamente cualquier `pcall` malicioso y es capturado en Java por `catch (Throwable ignored)`.
* **Límites de Radio y Geometría:**
  * [`LuaHudCanvas.java:152, 171, 175, 195`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaHudCanvas.java#L147-L205): `drawLine` limita `steps <= 1920`, y tanto `drawCircle` como `drawDisc` limitan `radius` al rango `[1, 512]`, impidiendo el desbordamiento de `int` en `radius * radius` y el bloqueo del Render Thread.
  * [`LuaWorldScanner.java:42, 68, 95`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaWorldScanner.java#L41-L96): `getTargetEntity`, `getEntitiesInRadius` y `raycastBlock` limitan el radio/rango a `[1.0, 64.0]`.

### 2.2. Hallazgos Residuales en el Motor Lua

#### [CRÍTICO - P0] `LuaModuleManager.loadModuleFile()` no captura `ScriptQuotaExceededError` (`Error`) ni reinicia el contador antes de `chunk.call()`
* **Archivo y Líneas:** [`LuaModuleManager.java:61-88`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaModuleManager.java#L61-L88)
  ```java
  LuaValue chunk = LuaScriptEngine.getGlobals().load(content, file.getName());
  chunk.call();
  ...
  } catch (LuaError error) {
      Vision_goggles.LOGGER.error("[Vision Goggles Lua Compile Error in " + file.getName() + "] " + error.getMessage());
  } catch (Exception e) {
      Vision_goggles.LOGGER.error("[Vision Goggles] Failed to read " + file.getName() + ": " + e.getMessage(), e);
  }
  ```
* **Diagnóstico Técnico:**
  1. Como `ScriptQuotaExceededError` ahora extiende `java.lang.Error` (y no `Exception`), el bloque `catch (Exception e)` de `loadModuleFile()` (y el de `loadAllModules()` en la línea 56) **no lo captura**.
  2. Además, `loadModuleFile()` **no llama a `SandboxedDebugLib.resetInstructionCount()`** antes de `chunk.call()` (ni limpia `globals.set("module", LuaValue.NIL)`).
  3. **Impacto:** Si un script `.lua` tiene un bucle infinito en el nivel superior (fuera de las funciones `onTick`/`onRenderHud`) o si la carga acumulada de varios scripts supera 100,000 instrucciones durante `Vision_goggles.init()` o `/vg reload`, `ScriptQuotaExceededError` escapa sin capturar y **crashea el cliente o el servidor entero al iniciar**.
* **Solución Exacta (3 líneas):** En `LuaModuleManager.loadModuleFile()`, invocar `SandboxedDebugLib.resetInstructionCount();` y `LuaScriptEngine.getGlobals().set("module", LuaValue.NIL);` antes de `chunk.call()`, y cambiar `catch (Exception e)` por `catch (Throwable e)`.

#### [MEDIO - P2] Evasión con `xpcall` en LuaJ y escritura a disco sin límite en `goggles.setMaxModules`
* **Archivos y Líneas:**
  * [`SandboxedDebugLib.java:23-26`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/SandboxedDebugLib.java#L23-L26) y [`LuaScriptEngine.java:26-33`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaScriptEngine.java#L26-L33)
  * [`LuaWorldScanner.java:473-487`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/scripting/LuaWorldScanner.java#L473-L487)
* **Diagnóstico Técnico:**
  1. En LuaJ 3.0.1, a diferencia de `pcall`, el método interno `LuaClosure.errorHook` usado por `xpcall(f, errfunc)` captura `catch (Throwable)` al ejecutar `errfunc`. Como `onInstruction` hace `INSTRUCTION_COUNT.set(0)` antes de lanzar `ScriptQuotaExceededError`, un script malicioso que anide `xpcall` con bucles en `errfunc` puede absorber el `Error` con el contador ya reseteado. Solución: añadir `globals.set("xpcall", LuaValue.NIL);` y `globals.set("coroutine", LuaValue.NIL);` en `LuaScriptEngine.init()`.
  2. En `LuaWorldScanner.java:473-487`, `goggles.setMaxModules` sigue permitiendo a un script de cliente modificar `ModConfig.common` sin acotar a `[1, 16]` y llamar a `ModConfig.save()` en cada tick.

---

## 3. Supervivencia (Loot Table, Pickaxe Tag, Visión Fantasma y Mejora Pro)

**Calificación del Área:** **8.5 / 10** *(Subió desde 6.5 / 10 en v21)*

### 3.1. Verificación de Correcciones del Plan v22 (Aprobadas)
* **Loot Table y Tag de Pico:**
  * [`common/src/main/resources/data/vision_goggles/loot_tables/blocks/modification_station.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/vision_goggles/loot_tables/blocks/modification_station.json) y [`common/src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/minecraft/tags/blocks/mineable/pickaxe.json) están correctamente estructurados con `survives_explosion` y `"replace": false`.
* **Mejora a Gafas Pro (`GogglesUpgradeRecipe.java`):**
  * [`GogglesUpgradeRecipe.java:21-42`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/recipe/GogglesUpgradeRecipe.java#L21-L42) copia íntegramente el NBT de las `modular_goggles` e instala `ModConstants.ID_BATTERY_EXPANSION` si no estaba presente.
* **Desmontaje de Módulos (`ModularGogglesItem.removeModule`):**
  * [`ModularGogglesItem.java:257-307`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/ModularGogglesItem.java#L257-L307) apaga `TAG_ACTIVE` cuando las gafas quedan vacías o se retira el módulo del modo activo, apaga `TAG_FLASHLIGHT_ACTIVE` al retirar `flashlight`, y recorta `TAG_BATTERY` al nuevo `getMaxBattery(stack)`.

### 3.2. Hallazgos Residuales en Supervivencia

#### [ALTO - P1] `ModularGogglesItem.removeModule()` resetea `TAG_MODE` a `0` (`NIGHT_VISION`) en lugar de `-1`
* **Archivo y Líneas:** [`ModularGogglesItem.java:280-284`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/ModularGogglesItem.java#L280-L284)
  ```java
  int currentModeId = tag.getInt(ModConstants.TAG_MODE);
  if (getModules(stack).isEmpty() || (removedMode != null && removedMode.getId() == currentModeId)) {
      tag.putBoolean(ModConstants.TAG_ACTIVE, false);
      tag.putInt(ModConstants.TAG_MODE, 0);
  }
  ```
* **Diagnóstico Técnico:**
  * En [`VisionMode.java:8`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/VisionMode.java#L8), el ID `0` es **`VisionMode.NIGHT_VISION`**, mientras que el ID para "sin modo seleccionado" en [`VisionRenderer.java:148`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L148) y [`VisionGogglesItem.java:88`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/VisionGogglesItem.java#L88) es **`-1`**.
  * Además, `tag.getInt(ModConstants.TAG_MODE)` devuelve `0` si el tag no existe.
  * **Impacto:** Si unas gafas tienen instalados `thermal_module` (ID 1) + `vital_info_module` y se extrae el `thermal_module` mientras estaba seleccionado, `removeModule` escribe `TAG_MODE = 0` (`NIGHT_VISION`) en lugar de `-1`. Debe escribirse `tag.putInt(ModConstants.TAG_MODE, -1);`.

#### [ALTO - P1] El módulo de linterna integrado (`ModuleType.FLASHLIGHT`) no se apaga al agotar la batería y mantiene el estroboscópico gratis
* **Archivo y Líneas:** [`ModuleType.java:56-73`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/ModuleType.java#L56-L73) y [`ModularGogglesItem.java:37-64`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/ModularGogglesItem.java#L37-L64)
* **Diagnóstico Técnico:**
  * En `ModularGogglesItem.serverTick()`, `util.tickPower(stack, player)` se ejecuta en la línea 41 **antes** del bloque de standby (línea 45).
  * En `ModuleType.FLASHLIGHT.tickPower()` ([líneas 59–71](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/util/ModuleType.java#L59-L71)):
    ```java
    if (current > 0) {
        stack.getOrCreateTag().putFloat(ModConstants.TAG_BATTERY, Math.max(0, current - drain));
    }
    int modeId = stack.getOrCreateTag().getInt(ModConstants.TAG_FLASHLIGHT_MODE);
    if (modeId == FlashlightMode.STROBE.id && player.tickCount % 5 == 0) {
        FlashlightDeterrence.apply(player.serverLevel(), player, 24.0);
    }
    ```
  * Cuando `current - drain` llega a `0`:
    1. No se cambia `TAG_FLASHLIGHT_ACTIVE` a `false` ni se envía `SyncFlashlightPacket(player.getId(), false)` a los demás jugadores.
    2. Como `TAG_FLASHLIGHT_ACTIVE` sigue siendo `true`, `FlashlightDeterrence.apply(...)` **se sigue ejecutando indefinidamente cada 5 ticks con 0% de batería**.

#### [MEDIO - P2] Detalles adicionales de Supervivencia
1. **`ModificationStationBlockEntity.removeItemNoUpdate(int slot)` ([líneas 280–283](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L280-L283)):** No invoca `this.setChanged()` al retirar ítems de los slots 0 o 1, y `canTakeItemThroughFace` ([línea 328](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/block/entity/ModificationStationBlockEntity.java#L328)) permite que una tolva inferior extraiga las gafas de los slots 0 y 1 mientras están en la estación.
2. **Remoción de poción de Visión Nocturna vanilla en el cliente ([`VisionRenderer.java:208, 562`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L561-L563)):** A diferencia de `VisionGogglesItem.cleanUpEffect` en el servidor (que comprueba `!effect.isVisible() && !effect.showIcon()`), el cliente llama a `mc.player.removeEffect(MobEffects.NIGHT_VISION)` sin verificar si el efecto proviene de una poción vanilla visible.

---

## 4. Paridad Fabric (`ThermalEntityPainter`, Slot `body` de Accessories y Tick de Linterna en Torso)

**Calificación del Área:** **6.2 / 10** *(Subió desde 5.5 / 10 en v21, pero presenta 3 bloqueadores P0 en Fabric)*

### 4.1. Verificación de Correcciones del Plan v22 (Aprobadas)
* **Tick de servidor de la linterna táctica en el torso (`ModEvents.java:122-126`):**
  * En [`ModEvents.tickGoggles(ServerPlayer)`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L122-L126), ahora se obtiene `PlatformMethods.getEquippedFlashlight(player)` y se ejecutan `TacticalFlashlightItem.tickBattery(flashlight, player)` y `FlashlightDeterrence.apply(player.level(), player, flashlight)`. Esto resuelve por completo la batería infinita y la falta de disuasión estroboscópica en el chaleco tanto en Curios como en Accessories.

### 4.2. Hallazgos Críticos (P0) en Fabric

#### [CRÍTICO - P0] El JAR compilado de Fabric crashea al iniciar porque `fabric/build.gradle` no expande `${version}` en `fabric.mod.json`
* **Archivos y Líneas:**
  * [`fabric/src/main/resources/fabric.mod.json:4`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/resources/fabric.mod.json#L4) (`"version": "${version}"`)
  * [`fabric/build.gradle:1-62`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/build.gradle#L1-L62)
* **Diagnóstico Técnico:**
  * Mientras que `forge/build.gradle` incluye un bloque `processResources` para expandir `version` en `mods.toml`, **`fabric/build.gradle` carece por completo de `processResources`**.
  * Como resultado, el archivo `fabric.mod.json` empaquetado dentro de `vision-goggles-fabric-2.0.0.jar` contiene la cadena literal `"${version}"`. Al iniciar Minecraft con Fabric Loader en un entorno real, Fabric rechaza el mod con `FormattedException: Invalid metadata in fabric.mod.json: Version ${version} is invalid`.
* **Solución Exacta:** Añadir el bloque `processResources` en `fabric/build.gradle` para expandir `"version": project.version` en `fabric.mod.json`.

#### [CRÍTICO - P0] `ThermalEntityPainter.init()` falla silenciosamente en Fabric porque se ejecuta antes de que `ResourceManager` monte los shaders
* **Archivos y Líneas:**
  * [`Vision_gogglesFabricClient.java:20-21`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/java/dev/itsrealperson/vision_goggles/fabric/client/Vision_gogglesFabricClient.java#L20-L21)
  * [`ThermalEntityPainter.java:98-117, 131-133, 386-392`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ThermalEntityPainter.java#L98-L133)
* **Diagnóstico Técnico:**
  * En Fabric 1.20.1, `ClientModInitializer.onInitializeClient()` se ejecuta durante `Minecraft.<init>` **antes** de que `ReloadableResourceManager` cargue los resource packs de los mods.
  * Cuando `Vision_gogglesFabricClient.onInitializeClient()` llama a `ThermalEntityPainter.init()` ([línea 20](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/java/dev/itsrealperson/vision_goggles/fabric/client/Vision_gogglesFabricClient.java#L20)), `createShader()` invoca `mc.getResourceManager().getResource(new ResourceLocation("vision_goggles", "shaders/thermal/quad.vsh")).orElseThrow()`, lanzando `NoSuchElementException`.
  * `ThermalEntityPainter.init()` atrapa la excepción ([línea 114](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ThermalEntityPainter.java#L114)), deja `initialized = false`, y `captureAndComposite()` ([línea 132](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/ThermalEntityPainter.java#L132): `if (!initialized) return;`) **nunca vuelve a intentar inicializarlo**. Por ende, las siluetas térmicas siguen sin renderizarse en Fabric.
* **Solución Exacta:** En `ThermalEntityPainter.captureAndComposite(PoseStack)`, cambiar `if (!initialized) return;` (o tras comprobar que el modo térmico está activo en la línea 138) por inicialización perezosa:
  ```java
  if (!initialized) {
      init();
      if (!initialized) return;
  }
  ```

#### [CRÍTICO - P0] `fabric/src/main/resources/data/accessories/entities/player.json` sigue sobrescribiendo al de `common` y ocultando el slot `body`
* **Archivos y Líneas:**
  * [`common/src/main/resources/data/accessories/entities/player.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/resources/data/accessories/entities/player.json) (`["accessories:eyes", "accessories:body"]`)
  * [`fabric/src/main/resources/data/accessories/entities/player.json`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/fabric/src/main/resources/data/accessories/entities/player.json) (`["accessories:face"]`)
* **Diagnóstico Técnico:**
  * En el Plan v22 se añadió `"accessories:body"` a `common/src/main/resources/data/accessories/entities/player.json`, pero **no se eliminó ni actualizó `fabric/src/main/resources/data/accessories/entities/player.json`**.
  * Al compilar el JAR de Fabric, el archivo de `fabric/src/main/resources/` sobrescribe al de `common`, dejando únicamente `"accessories:face"` y eliminando `"accessories:eyes"` y `"accessories:body"`. Además, falta `common/src/main/resources/data/accessories/slots/body.json` (solo existe `slots/eyes.json`).

#### [ALTO - P1] Conflicto al recargar la linterna de torso con clic derecho (`ModEvents.RIGHT_CLICK_ITEM` vs `BatteryItem`)
* **Archivos y Líneas:** [`ModEvents.java:69-96`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/event/ModEvents.java#L69-L96) y [`BatteryItem.java:55-149`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/item/BatteryItem.java#L55-L149)
* **Diagnóstico Técnico:** `ModEvents.RIGHT_CLICK_ITEM` intercepta el clic derecho con baterías buscando únicamente `PlatformMethods.getEquippedHelmet(player)`. Si el jugador tiene las gafas al 99% y la linterna táctica en el torso al 0%, el evento gasta la batería en las gafas e impide que `BatteryItem.resolveTarget()` recargue la linterna; y si no lleva gafas puestas, no permite recargar la linterna con `extraBatteryItems`.

---

## 5. Optimización de Raycasts de Linterna y Shaders

**Calificación del Área:** **8.3 / 10** *(Subió desde 7.2 / 10 en v21)*

### 5.1. Verificación de Correcciones del Plan v22 (Aprobadas)
* **Reducción del 90% de Raycasts por Frame ([`VisionRenderer.java:508-512`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java#L508-L512)):**
  * Se eliminaron los 8 raycasts periféricos (`hitRanges[1..8]`), las transformaciones cuaterniónicas de cono y la consulta `checkIsWall`, dejando únicamente:
    ```java
    float[] hitRanges = new float[9];
    hitRanges[0] = performRaycast(player, originWorld, dir, fMode.range);
    ```
  * Asimismo, se verificó que la iluminación de la linterna es 100% por shader en cliente (sin bloques `Blocks.LIGHT` en el servidor), por lo que no existe posibilidad de bloques de luz huérfanos en el mundo.

### 5.2. Hallazgos Residuales de Rendimiento en Uniformes y Shaders

#### [ALTO - P1] Subida de 12 uniformes muertos en cada Draw Call y ausencia de Fast-Path cuando `currentCount == 0`
* **Archivos y Líneas:**
  * [`FlashlightUniforms.java:131-178`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/FlashlightUniforms.java#L131-L178)
  * [`SodiumFlashlightUniforms.java:50-78`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/SodiumFlashlightUniforms.java#L50-L78)
  * [`SodiumChunkUniformMixin.java:20-35`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/mixin/sodium/SodiumChunkUniformMixin.java#L20-L35)
* **Diagnóstico Técnico:**
  1. Aunque `VisionRenderer` ya no calcula `hitRanges[1..8]` ni `isWall`, `FlashlightUniforms.applyToVanilla(ShaderInstance)` sigue ejecutándose en cada `ShaderInstance.apply()` de Minecraft realizando **20 llamadas sin caché a `GL20.glGetUniformLocation`** y **20 llamadas a `GL20.glUniform*`** (incluyendo los 12 arreglos muertos `Up`, `Right`, `Range`, `Range1..8`), incluso cuando **ninguna linterna está activa (`currentCount == 0`)**.
  2. Si en `applyToVanilla` y `SodiumFlashlightUniforms.upload` se añade un retorno temprano tras subir `VisionFlashlightCount = 0` (`if (currentCount == 0) return;`), se elimina el **95% de las llamadas JNI a OpenGL** cuando las linternas están apagadas.
  3. En `SodiumChunkUniformMixin.java:23`, `this.getClass().getMethod("handle").invoke(this)` se sigue buscando por reflexión en cada `GlProgram.bind()` sin cachear el objeto `Method` en un campo `static`.

#### [MEDIO - P2] En 1.ª persona (`VisionFlashlightsIsLocal[i] > 0.5`), el shader GLSL no aplica el corte de distancia `VisionFlashlightsRange0[i]`
* **Archivos y Líneas:** [`ShaderRewriter.java:56-63`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/ShaderRewriter.java#L56-L63) y [`SodiumShaderPatcher.java:75-82`](file:///C:/Users/Pers0n/Documents/GitHub/Vision%20Goggles/common/src/main/java/dev/itsrealperson/vision_goggles/client/lighting/SodiumShaderPatcher.java#L75-L82)
* **Diagnóstico Técnico:** La rama `if (VisionFlashlightsIsLocal[i] > 0.5)` en el GLSL inyectado hace `continue;` sin comprobar `if (dist > VisionFlashlightsRange0[i]) continue;`, permitiendo que en primera persona la luz atraviese paredes delgadas de 1 bloque.

---

## 6. Calificación Final e Imparcial para Lanzamiento Público (Modrinth / CurseForge)

| Categoría Auditada | Peso | Nota Pre-v21 | Nota Post-v21 | Nota Post-v22 | Veredicto Técnico Post-v22 |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **1. Servidor Dedicado y `/vg`** | 25% | 2.0 / 10 | 4.5 / 10 | **8.2 / 10** | `LuaModuleManager`/`LuaCustomModule` limpios y `/vg` con permisos OP 2. Quedan P1 menores en `VisionGogglesItem$1` y `EquipPacket`. |
| **2. Hardening del Sandbox Lua** | 20% | 2.5 / 10 | 5.0 / 10 | **8.0 / 10** | Cuota reiniciada por hook, `Error` inmune a `pcall` y radios acotados. Falta `catch (Throwable)` y reset en `loadModuleFile()`. |
| **3. Supervivencia y Progresión** | 20% | 7.5 / 10 | 6.5 / 10 | **8.5 / 10** | Loot Table, tag de pico, mejora Pro y anti-visión fantasma implementados. Queda usar `-1` en `removeModule` y apagar linterna a 0% batería. |
| **4. Paridad Fabric / Forge** | 15% | 5.0 / 10 | 5.5 / 10 | **6.2 / 10** | Tick de linterna en torso listo, pero en Fabric faltan `processResources` (`${version}`), lazy-init de `ThermalEntityPainter` y limpiar `fabric/.../player.json`. |
| **5. Linterna, Shaders y Rendimiento** | 20% | 6.8 / 10 | 7.2 / 10 | **8.3 / 10** | Reducción del 90% de raycasts lograda (1 raycast/linterna). Queda fast-path `currentCount == 0` en uniformes GL y caché en mixin de Sodium. |
| **CALIFICACIÓN GLOBAL PONDERADA** | **100%** | **5.7 / 10** | **6.4 / 10** | **7.9 / 10** | **Forge (8.5/10) casi listo; Fabric (6.2/10) requiere micro-parche de 4 archivos antes de publicar.** |

### Checklist Exacto de Cierre (Micro-Parche para alcanzar >9.5 / 10 en Forge y Fabric)
1. **`fabric/build.gradle` (P0 Fabric):** Añadir `processResources` para expandir `"version": project.version` en `fabric.mod.json`.
2. **`ThermalEntityPainter.java:132` (P0 Fabric):** Cambiar `if (!initialized) return;` por `if (!initialized) { init(); if (!initialized) return; }` (después del chequeo de modo térmico en la línea 138) para que se inicialice cuando `ResourceManager` ya cargó los shaders en Fabric.
3. **`fabric/src/main/resources/data/accessories/entities/player.json` (P0 Fabric):** Eliminar este archivo duplicado de `fabric/` (para que no sobrescriba al de `common/`) y añadir `"accessories:face"` en `common/src/main/resources/data/accessories/entities/player.json` junto con `common/src/main/resources/data/accessories/slots/body.json`.
4. **`LuaModuleManager.java:67-87` (P0 Lua):** Añadir `SandboxedDebugLib.resetInstructionCount();` y `LuaScriptEngine.getGlobals().set("module", LuaValue.NIL);` antes de `chunk.call()`, y cambiar `catch (Exception e)` por `catch (Throwable e)` para atrapar `ScriptQuotaExceededError`.
5. **`ModularGogglesItem.java:283` y `ModuleType.java:64` (P1 Supervivencia):** Cambiar `tag.putInt(ModConstants.TAG_MODE, 0)` a `-1` en `removeModule()`, y en `ModuleType.FLASHLIGHT.tickPower()` apagar `TAG_FLASHLIGHT_ACTIVE = false` cuando la batería llegue a `0`.
