# Auditoría Exhaustiva Pre-Lanzamiento — Vision Goggles v2.0.0

**Fecha:** 18 de Octubre de 2026  
**Objetivo:** Evaluación de calidad, estabilidad, seguridad y paridad previa a la publicación en CurseForge / Modrinth.  
**Plataformas evaluadas:** Minecraft 1.20.1 (Forge + Fabric via Architectury Common).  
**Calificación Global:** **5.7 / 10** (Inmersión y Mecánicas: 8.5/10 | Estabilidad Servidor y Paridad: 2.8/10).

---

## 1. Resumen Ejecutivo

Vision Goggles cuenta con una base estética, sonora y jugable sobresaliente: los shaders de visión nocturna con bloom de fósforo, la linterna táctica con estroboscópico de 9.5 Hz, la integración visual con JEI, el sistema de crafteo físico modular TEMS y el sistema de audio táctico son de nivel comercial profesional.

Sin embargo, **el mod NO está listo para su publicación pública en su estado actual**. La auditoría automatizada y estática reveló bloqueadores críticos que provocarían:
1. Crasheo inmediato al iniciar cualquier servidor dedicado multijugador (`NoClassDefFoundError: net/minecraft/client/Minecraft`).
2. Duplicación masiva de ítems en supervivencia mediante la Estación de Modificación.
3. Vulnerabilidad crítica de Ejecución Remota de Código (RCE) y congelamiento de CPU por el motor Lua desprotegido.
4. Incompatibilidad total con el slot de casco vanilla en Fabric.
5. Deformación visual de los visores en monitores 16:9 y caídas de FPS en el hilo de renderizado.

---

## 2. Desglose por Áreas y Puntuaciones

| Área | Puntuación | Estado |
| :--- | :---: | :--- |
| **Mecánicas de Supervivencia y Progresión** | **7.5 / 10** | Notable, pero con exploits de duplicación en la Estación de Modificación. |
| **Shaders, Renderizado e Inmersión Visual** | **6.8 / 10** | Excelente inmersión, pero con deformación de aspecto (huevo) y tirones de FPS. |
| **Multijugador, Servidor Dedicado y Seguridad** | **2.8 / 10** | **CRÍTICO:** Crasheos en servidor dedicado, RCE en Lua y paquetes spoofables. |
| **Paridad Forge / Fabric** | **5.0 / 10** | El slot de casco y los renders de capas 3D no funcionan correctamente en Fabric. |

---

## 3. Hallazgos Críticos Detallados (Bloqueadores P0)

### 3.1 Crasheos en Servidores Dedicados (Headless Server Crash)
* **`OpenLuaScreenPacket.java`:** Importa directamente `net.minecraft.client.Minecraft` y la pantalla de GUI de cliente. Al registrarse en el canal de red común, el cargador de clases de un servidor dedicado lanza `NoClassDefFoundError`.
* **`CommandVisionGoggles.java`:** Registra comandos `/vg run`, `/vg list`, `/vg reload` y `/vg gui` en el servidor con dependencias directas a `LuaScriptEngine` y `Minecraft.getInstance()`.
* **`ModificationStationBlockEntity.java`:** En el servidor, consulta `LuaModuleManager` para previsualizar chips programados, cargando clases de cliente en el servidor.
* **`Vision_gogglesForge.java`:** Referencia `ConfigScreenHandler.ConfigScreenFactory` en el constructor sin envolverlo en un proxy seguro (`DistExecutor`), crasheando en servidores Forge.

### 3.2 Exploits de Duplicación en Supervivencia
* **Duplicación al romper el bloque (`ModificationStationBlock`):** Cuando se coloca una gola o módulo en la estación, el slot 2 muestra una previsualización del resultado antes de ser retirado. Si el jugador rompe el bloque con el pico, `Containers.dropContents` suelta tanto las entradas como la salida, duplicando gafas, módulos o chips infinitamente.
* **Extracción ilegal por tolvas (`ModificationStationBlockEntity`):** Implementa `WorldlyContainer` permitiendo `canTakeItemThroughFace(2, ...)` como `true`. Una tolva conectada debajo puede succionar el resultado sin consumir los materiales de entrada.

### 3.3 Vulnerabilidad de Seguridad RCE y DoS en Motor Lua
* **Ejecución Remota de Código (RCE):** `LuaScriptEngine.java` inicializa el entorno con `JsePlatform.standardGlobals()`. Esto expone `luajava` (acceso a cualquier clase de Java como `java.lang.Runtime`), `os.execute` y librerías `io`, permitiendo a scripts maliciosos ejecutar comandos de consola en el PC del usuario.
* **Bloqueo de CPU (DoS):** No existe límite de instrucciones ni watchdog de tiempo. Un bucle `while true do end` congela Minecraft para siempre.

### 3.4 Fallos de Paridad en Fabric
* **Casco Vanilla Ignorado:** En `PlatformMethodsImpl.java` de Fabric, si el jugador no tiene instalada la API de Accessories o no equipa las gafas en ese slot, la función retorna `ItemStack.EMPTY`, ignorando `player.getItemBySlot(EquipmentSlot.HEAD)`. En Fabric survival puro las gafas no funcionan en la cabeza.
* **Modelos 3D Invisibles:** `FabricLayerRegistry.init()` jamás se invoca en el cliente de Fabric, impidiendo ver los modelos 3D de las gafas en tercera persona o en otros jugadores.

### 3.5 Rendimiento y Deformación Visual de Shaders
* **Tirón Masivo de FPS (Render Thread):** `VisionWorldRenderer.updateSpawnBlocks` escanea un cubo de 65x65x65 bloques (274.625 bloques) cada 20 ticks dentro del hilo de renderizado principal.
* **Efecto Huevo en 16:9 / 21:9:** Solo `flashlight.fsh` compensa la relación de aspecto. En monitores panorámicos, `nvg.fsh`, `thermal.fsh`, `hydro.fsh` y `bio.fsh` muestran círculos estirados horizontalmente.

---

## 4. Fortalezas Destacadas (Lo que está listo)
1. **Sistema de Crafteo TEMS:** 100% coherente a nivel de ingeniería (Lente Óptica, Armazón Balístico, Cuerpo de Linterna, Núcleo IR, Sensor Vital, Procesador Táctico).
2. **Audio Táctico:** Clics mecánicos diferenciados al encender/apagar o cambiar modos.
3. **Ergonomía de Linterna:** Estroboscópico de 9.5 Hz balanceado con luz de relleno para evitar fatiga ocular.
4. **Fogonazo Óptico Realista:** Oclusión física por raycasting para el sol y fogonazos en explosiones de TNT/rayos.
5. **Animación en Primera Persona:** Suprimido el balanceo molesto de reequipación (`CustomReequipItem`).
