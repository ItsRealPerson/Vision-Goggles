# Manual de Referencia de la API de Scripting Lua - Vision Goggles v2.0

Bienvenido al manual oficial y completo para la creacion de modulos, HUDs tacticos, realidad aumentada 3D y control de shaders en **Vision Goggles**.

Este documento detalla el **100% de las funciones, estructuras, tablas globales, uniforms reales y ciclo de vida** expuestos por el motor de Lua (**LuaJ 3.0.1 / Lua 5.2**) en Java para Minecraft 1.20.1.

---

## Indice
1. [Estructura Basica de un Script](#1-estructura-basica-de-un-script)
2. [Ciclo de Vida del Modulo](#2-ciclo-de-vida-del-modulo)
3. [Tabla Global player](#3-tabla-global-player)
4. [Tabla Global world (Escaneo y Proyeccion AR 3D)](#4-tabla-global-world-escaneo-y-proyeccion-ar-3d)
5. [Tabla Global hud (Lienzo Tactico Vectorial)](#5-tabla-global-hud-lienzo-tactico-vectorial)
6. [Tabla Global sound](#6-tabla-global-sound)
7. [Tabla Global goggles](#7-tabla-global-goggles)
8. [Puente de Shaders: Tabla uniforms](#8-puente-de-shaders-tabla-uniforms)
9. [Guia de Shaders GLSL y Post-Processing JSON](#9-guia-de-shaders-glsl-y-post-processing-json)
10. [Flujo de Flasheo Fisico en el Juego](#10-flujo-de-flasheo-fisico-en-el-juego)
11. [Plantilla Limpia (Boilerplate)](#11-plantilla-limpia-boilerplate)
12. [Limites del Sandbox, Buenas Practicas y Catalogo de Scripts](#12-limites-del-sandbox-buenas-practicas-y-catalogo-de-scripts)

---

## 1. Estructura Basica de un Script

Cada archivo `.lua` ubicado en la carpeta `config/vision_goggles/modules/` debe declarar una tabla global obligatoria llamada `module`. **Todas las variables y funciones auxiliares fuera de `module` deben declararse con `local`** (porque todos los scripts comparten un mismo entorno global en LuaJ):

```lua
local estadoLocal = 0

module = {
    id              = "vision_goggles:mi_modulo_tactico",  -- Identificador unico (namespaced, sin comillas en el comentario)
    name            = "Mi Modulo Tactico",                 -- Nombre visible en el juego y en el chip
    type            = "VISION_MODE",                       -- Tipo: "VISION_MODE", "HUD" o "UTILITY"
    shader          = "vision_goggles:shaders/post/nvg.json", -- Ruta JSON o nombre corto ("nvg", "thermal", "bio", "hydro")
    required_module = "nvg",                               -- Hardware requerido: "nvg", "thermal", "bio", "hydro" o nil (autonomo)
    battery_drain   = 0.10,                                -- Drenaje adicional de bateria POR TICK (20 ticks = 1 segundo)
}
```

### Tipos de Modulo (`type`):
* **`"VISION_MODE"`**: Se comporta como un modo de vision o procesador optico. Permite cargar el pipeline de shaders indicado en `shader` y ajustar variables dinamicas de GPU en tiempo real mediante `onUpdateUniforms`.
* **`"HUD"` / `"UTILITY"`**: Funciona en segundo plano como modulo de interfaz o utilidad tactica (HUD de avionica, alertas de proximidad, radar 360°, visor AR 3D, etc.).

### Dependencia de Hardware (`required_module`):
* `"nvg"`: Requiere que las Gafas Modulares (o Pro) tengan instalado el modulo de **Vision Nocturna**.
* `"thermal"`: Requiere el modulo de **Visor Termico**.
* `"bio"`: Requiere el modulo **Biometrico**.
* `"hydro"`: Requiere el modulo **Hydro**.
* `"sonar"`: Requiere el modulo **Sonar** (util para upgrades DSP del radar sin reemplazar al modulo fisico).
* `nil` (u omitido): Modulo **autonomo**. Puede instalarse en cualquier Gafa Modular libremente sin requerir un modulo base previo.

### Drenaje de Bateria (`battery_drain`):
* Se expresa en **unidades de bateria por tick** (`20 ticks = 1 segundo`).
* Referencia base de las Gafas Modulares (`6000` unidades de capacidad base):
  * Vision Nocturna / Bio / Hydro base = `1.0` por tick (`5 minutos` de autonomia base).
  * Visor Termico base = `2.0` por tick.
  * Un script `UTILITY` ligero suele usar entre `0.0` y `0.10` por tick; un `VISION_MODE` avanzado suele usar `0.15` a `0.30` por tick.
* Solo se descuenta cuando el visor esta encendido y el chip cumple su `required_module`.

---

## 2. Ciclo de Vida del Modulo

El motor invoca hasta **6 funciones callback** opcionales dentro de `module` (todas ejecutadas en el hilo del cliente):

```lua
-- 1. Se ejecuta una sola vez cuando el jugador enciende el visor con el chip activo
onEnable = function(player)
    sound.playBeep(1.5)
    hud.showAlert("SISTEMA INICIADO", "#00FFAA", 2.0)
end,

-- 2. Se ejecuta una sola vez cuando el jugador apaga el visor, se quita las gafas o recarga (/vg reload)
onDisable = function(player)
    sound.playBeep(0.8)
end,

-- 3. Se ejecuta en cada tick del cliente (20 veces por segundo)
-- IDEAL para escanear entidades cercanas, consultar bloques, timers y alertas sonoras
onTick = function(player, batteryPct)
    -- batteryPct: Float entre 0.0 (vacia) y 1.0 (100% llena)
end,

-- 4. Se ejecuta en cada fotograma antes de dibujar el shader de post-procesado activo
-- IDEAL para cambiar tintes cromaticos, deslumbramiento (Glare) o interferencia termica
onUpdateUniforms = function(player, uniforms)
    uniforms.setVec3("colorFilter", 0.2, 1.0, 0.4)
end,

-- 5. Se ejecuta en cada fotograma durante el pase de renderizado del HUD de Minecraft
-- Permite dibujar figuras vectoriales, texto, barras, radares y marcadores AR 3D
onRenderHud = function(hud)
    local w = hud.getWidth()
    local h = hud.getHeight()
    hud.drawText(10, 10, "ESTADO OK", "#00FF88", 1.0)
end,

-- 6. Se ejecuta en modo Termico para cada entidad viva con firma termica
-- Permite asignar la firma cromatica que entra al pipeline Ironbow de thermal.fsh
-- NOTA: Evita tonos azules puros (ej. #1144FF), ya que thermal.fsh interpreta
--       azul dominante como fuente criogenica (coldSignal -> negro/azul marino).
onThermalColor = function(entityType, isHostile)
    if entityType == "minecraft:player" or entityType == "minecraft:creeper" then
        return "#00FFFF" -- Firma blanca incandescente maxima en Ironbow
    elseif isHostile then
        return "#FFBB00" -- Firma caliente amarillo-oro intenso
    else
        return "#FF8800" -- Firma calida naranja para animales de sangre caliente
    end
end
```

---

## 3. Tabla Global `player`

La tabla `player` proporciona telemetria en tiempo real sobre el jugador local:

| Funcion | Retorno | Descripcion |
| :--- | :--- | :--- |
| `player.getHealth()` | `number` | Salud actual en medio-corazones (ej. `20.0`). |
| `player.getMaxHealth()` | `number` | Salud maxima del jugador (ej. `20.0`). |
| `player.getAbsorption()` | `number` | Corazones dorados de absorcion actuales (ej. `4.0`). |
| `player.getFoodLevel()` | `number` | Nivel de hambre entre `0` y `20`. |
| `player.getSaturation()` | `number` | Nivel de saturacion exacta (ej. `5.0`). |
| `player.getArmor()` | `number` | Puntos de armadura fisica (`0` a `20+`). |
| `player.getArmorDurabilityPct()` | `number` | Durabilidad minima normalizada (`0.0..1.0`) entre las piezas de armadura equipadas, o `-1.0` si no lleva armadura dañable. |
| `player.getOxygen()` | `number` | Oxigeno restante en porcentaje (`0.0` a `100.0`). |
| `player.getSonarMode()` | `number` | Filtro actual del modulo Sonar hardware en NBT (`0` = Todos, `1` = Jugadores, `2` = No-Muertos). |
| `player.getX()` | `number` | Coordenada X exacta en el mundo. |
| `player.getY()` | `number` | Coordenada Y (altura / capa) en el mundo. |
| `player.getZ()` | `number` | Coordenada Z exacta en el mundo. |
| `player.getYaw()` | `number` | Rotacion horizontal de la mirada en grados (`0° = Sur`, `90° = Oeste`, `180° = Norte`, `270° = Este`). Para rumbo de brujula norte: `((yaw + 180) % 360 + 360) % 360`. |
| `player.getPitch()` | `number` | Inclinacion vertical de la mirada (`-90°` arriba, `+90°` abajo). |
| `player.getSpeed()` | `number` | Velocidad de movimiento horizontal en **bloques por segundo (`m/s`)**. |
| `player.getDimension()` | `string` | Path de la dimension (`"overworld"`, `"the_nether"`, `"the_end"`). |
| `player.getBiome()` | `string` | ID de registro del bioma actual (ej. `"minecraft:plains"`, `"minecraft:deep_dark"`). |
| `player.getLightLevel()` | `number` | Nivel de luz combinado en la posicion del jugador (`0` a `15`). |
| `player.onGround()` | `boolean` | `true` si el jugador apoya los pies sobre suelo firme. |
| `player.isFlying()` | `boolean` | `true` si el jugador esta volando (vuelo creativo o planeando con **Elytras**). |
| `player.isSprinting()` | `boolean` | `true` si el jugador esta corriendo. |
| `player.isCrouching()` | `boolean` | `true` si el jugador esta agachado (sigilo). |
| `player.isInWater()` | `boolean` | `true` si el jugador esta en agua. |
| `player.isDay()` | `boolean` | `true` si es de dia en el mundo actual. |
| `player.getBattery()` | `number` | Nivel de bateria de las gafas (`0.0` a `1.0`). |
| `player.hasGoggles()` | `boolean` | `true` si lleva puestas unas gafas de Vision Goggles (casco o slot Curios/Accessories). |
| `player.isGogglesActive()`| `boolean` | `true` si el visor esta completamente desplegado y encendido. |
| `player.hasModule(name)` | `boolean` | `true` si las gafas tienen instalado el modulo dado (ej. `"nvg"`, `"thermal"`, `"bio"`). |
| `player.isProGoggles()` | `boolean` | `true` si las gafas equipadas son **Gafas Modulares Pro**. |
| `player.getMaxModules()` | `number` | Cantidad maxima de ranuras de las gafas equipadas (`2` en Modular, `4` en Pro). |
| `player.getGogglesType()` | `string` | ID del item de gafas equipado (ej. `"modular_goggles"`, `"pro_modular_goggles"`, `"none"`). |
| `player.getInstalledModules()` | `table` | Lista indexada desde `1` con los IDs en texto de todos los modulos instalados en las gafas. |
| `player.getEffects()` | `table` | Lista de efectos de pocion activos con `{ id, name, duration, amplifier, is_beneficial }`. (`duration` en ticks; `-1` si es infinito). |

---

## 4. Tabla Global `world` (Escaneo y Proyeccion AR 3D)

Permite escanear el entorno, detectar entidades, consultar bloques y proyectar coordenadas 3D del mundo a la pantalla 2D. Todos los rangos de busqueda estan acotados por seguridad a un maximo de **`64.0` bloques**.

### `world.getTargetEntity(range?, throughWalls?)`
Lanza un rayo desde los ojos del jugador hacia donde apunta la mira (por defecto `30.0` bloques, `throughWalls = false` para respetar muros solidos):
```lua
local target = world.getTargetEntity(32.0, false)
if target ~= nil then
    -- Campos disponibles en toda tabla de entidad:
    -- target.name        (string: nombre traducido, ej. "Creeper")
    -- target.type        (string: ID de registro, ej. "minecraft:creeper")
    -- target.health      (number: vida actual)
    -- target.max_health  (number: vida maxima)
    -- target.armor       (number: puntos de armadura)
    -- target.x, y, z     (number: posicion 3D interpolada por frame)
    -- target.eye_y       (number: altura exacta de los ojos)
    -- target.height      (number: altura de la hitbox en bloques)
    -- target.yaw, pitch  (number: orientacion de la entidad)
    -- target.distance    (number: distancia al jugador en bloques)
    -- target.is_hostile  (boolean: true si es monstruo hostil)
    -- target.is_player   (boolean: true si es otro jugador)
    -- target.emits_heat  (boolean: true si tiene firma termica real; false en no-muertos/aranas/slimes/golems)
    -- target.is_undead   (boolean: true si pertenece a MobType.UNDEAD)
end
```

### `world.getEntitiesInRadius(radius?)`
Devuelve un array Lua (1-indexado) con todas las entidades vivas (excepto el jugador local) dentro de una esfera de `radius` bloques (por defecto `16.0`, maximo `64.0`). Cada elemento contiene exactamente los mismos campos listados arriba.

### `world.getProjectilesInRadius(radius?)`
Devuelve un array Lua (1-indexado) con proyectiles en vuelo activos (excepto los disparados por el propio jugador y proyectiles detenidos en el suelo) dentro de `radius` bloques (por defecto `24.0`, maximo `48.0`):
```lua
local projs = world.getProjectilesInRadius(24.0)
for i = 1, #projs do
    local p = projs[i]
    -- p.type, p.x, p.y, p.z, p.vx, p.vy, p.vz, p.distance, p.approaching
    if p.approaching then
        hud.showAlert("¡PROYECTIL ENTRANTE!", "#FFCC00", 1.0)
    end
end
```

### `world.scanBlocks(radius, blockIdsTable, maxResults?)`
Realiza un escaneo esferico rapido en codigo nativo Java (`O(1)` por bloque mediante `IdentityHashMap<Block>`, hasta radio `16` y `32` resultados) devolviendo los bloques coincidentes mas cercanos ordenados por distancia:
```lua
local targets = { "minecraft:diamond_ore", "minecraft:deepslate_diamond_ore", "minecraft:ancient_debris" }
local hits = world.scanBlocks(14, targets, 24)
for i = 1, #hits do
    -- hits[i].id, hits[i].x, hits[i].y, hits[i].z, hits[i].distance
end
```

### `world.worldToScreen(x, y, z)`
Proyecta una coordenada 3D del mundo `(x, y, z)` a coordenadas 2D de la pantalla GUI escalada usando la matriz y el FOV real de la camara:
```lua
local proj = world.worldToScreen(ent.x, ent.y + ent.height * 0.5, ent.z)
if proj ~= nil and proj.visible and not proj.behind then
    -- proj.x        (number: coordenada X en pixeles GUI)
    -- proj.y        (number: coordenada Y en pixeles GUI)
    -- proj.visible  (boolean: true si cae dentro del recuadro de pantalla)
    -- proj.behind   (boolean: true si el punto queda detras de la camara)
    -- proj.distance (number: distancia euclidiana 3D a la camara)
    hud.drawCircle(math.floor(proj.x), math.floor(proj.y), 10, "#00FFAA")
end
```

### `world.raycastBlock(range?)`
Lanza un rayo hacia el bloque solido al que apunta la mira (por defecto `10.0`, maximo `64.0`):
```lua
local hit = world.raycastBlock(24.0)
if hit ~= nil then
    printLog(hit.name)        -- ID de registro (ej. "minecraft:diamond_ore")
    printLog(hit.id)          -- Alias identico a hit.name
    printLog(hit.x .. "," .. hit.y .. "," .. hit.z) -- Coordenadas enteras del bloque
    printLog(hit.light_level) -- Nivel de luz en el bloque (0 a 15)
    printLog(hit.can_see_sky) -- true si tiene vista directa al cielo
    printLog(hit.distance)    -- Distancia exacta en bloques
end
```

### `world.getBlock(x, y, z)`
Devuelve el ID de registro del bloque (`string`, ej. `"minecraft:lava"` o `"minecraft:air"` si el chunk no esta cargado) en las coordenadas dadas.

### `world.getLightAt(x, y, z)` y `world.getLightAtPlayer()`
Devuelven el nivel de luz combinado (`0` a `15`) en `(x, y, z)` o en la posicion actual del jugador.

### `world.getTimeOfDay()`, `world.isRaining()`, `world.isThundering()`
* `world.getTimeOfDay()`: Tick solar del dia entre `0` (`06:00 AM`) y `23999`.
* `world.isRaining()`: `true` si esta lloviendo o nevando.
* `world.isThundering()`: `true` si hay tormenta electrica activa.

---

## 5. Tabla Global `hud` (Lienzo Tactico Vectorial)

Disponible para encolar comandos de dibujo dentro de `onRenderHud(h)` (`showAlert` tambien puede llamarse desde `onEnable` u `onTick`). Todos los colores aceptan formato hexadecimal `"#RRGGBB"` (opaco) o `"#AARRGGBB"` (con canal alfa):

| Funcion | Parametros | Descripcion |
| :--- | :--- | :--- |
| `h.getWidth()` | *ninguno* | Ancho de la pantalla GUI escalada en pixeles. |
| `h.getHeight()` | *ninguno* | Alto de la pantalla GUI escalada en pixeles. |
| `h.getTextWidth(text, scale?)` | `(string, float=1.0)` | Devuelve el ancho exacto en pixeles que ocupara `text` a la escala indicada. |
| `h.drawText(x, y, text, color?, scale?)` | `(int, int, string, string="#00FF00", float=1.0)` | Dibuja texto con escala configurable. |
| `h.drawRect(x, y, w, h, color?)` | `(int, int, int, int, string="#FFFFFF")` | Dibuja un rectangulo solido o semitransparente (`#AARRGGBB`). |
| `h.drawProgressBar(x, y, w, h, pct, color?)` | `(int, int, int, int, float, string="#00FF88")` | Dibuja una barra de progreso con fondo oscuro (`#88111111`) y relleno proporcional (`0.0` a `1.0`). |
| `h.drawLine(x1, y1, x2, y2, color?)` | `(int, int, int, int, string="#FFFFFF")` | Traza una linea vectorial orientada de 1px de grosor entre dos puntos. |
| `h.drawCircle(cx, cy, radius, color?)` | `(int, int, int, string="#FFFFFF")` | Dibuja la circunferencia vacia de un circulo de 32 segmentos (radio `1..512`). |
| `h.drawDisc(cx, cy, radius, color?)` | `(int, int, int, string="#FFFFFF")` | Dibuja un circulo relleno acelerado por GPU (ideal para fondos de radar y blips). |
| `h.showAlert(msg, color?, durationSec?)` | `(string, string="#FF3333", float=2.5)` | Muestra un cartel de alerta tactica parpadeante en la parte superior central durante `durationSec` segundos. |

---

## 6. Tabla Global `sound`

Permite reproducir efectos de sonido tacticos locales (UI):

* `sound.playBeep(pitch?)`: Emite el pitido electronico del visor (`vision_goggles:blip`). `pitch` modula el tono (`0.5` grave, `1.0` normal, `2.0` agudo).
* `sound.playAlert()`: Emite una alarma tactica aguda (`block.note_block.pling`, tono `1.8`).
* `sound.playClick()`: Clic mecanico de fijacion (`ui.button.click`).
* `sound.play(soundId, pitch?, volume?)`: Reproduce cualquier sonido registrado en Minecraft por su `ResourceLocation` (ej. `sound.play("minecraft:block.beacon.activate", 1.5, 0.6)`).

---

## 7. Tabla Global `goggles`

Permite consultar o ajustar en cliente el limite de ranuras de las Gafas Modulares:

* `goggles.getMaxModules(type)`: Devuelve el maximo de ranuras para `"modular"` (por defecto `2`) o `"pro"` (por defecto `4`).
* `goggles.setMaxModules(type, count)`: Ajusta la capacidad de ranuras en cliente para `"modular"` o `"pro"`. *(Para sincronizar en servidor multijugador, usa el comando de administrador `/vg slots modular|pro <cantidad>`).*

---

## 8. Puente de Shaders: Tabla `uniforms`

Disponible exclusivamente dentro de `onUpdateUniforms(player, uniforms)`. Sobrescribe en tiempo real las variables uniformes del shader `PostChain` activo:

* `uniforms.setFloat("nombreUniform", floatVal)`
* `uniforms.setVec2("nombreUniform", x, y)`
* `uniforms.setVec3("nombreUniform", r, g, b)`
* `uniforms.setVec4("nombreUniform", r, g, b, a)`

### Uniforms Reales Declarados en los Shaders Nativos de Vision Goggles:

| Shader JSON | Uniforms modificables desde Lua |
| :--- | :--- |
| `vision_goggles:shaders/post/nvg.json` | `colorFilter` (`vec3`), `Glare` (`float` `0.0..1.0`), `focus` (`float` `0.0..1.0`), `battery` (`float`), `time` (`float`), `u_DSPActive` (`float`) |
| `vision_goggles:shaders/post/thermal.json` | `Interference` (`float` `0.0..1.0`), `focus` (`float`), `battery` (`float`), `time` (`float`), `u_DSPActive` (`float`) |
| `vision_goggles:shaders/post/bio.json` | `focus` (`float`), `battery` (`float`), `time` (`float`), `u_DSPActive` (`float`) |
| `vision_goggles:shaders/post/hydro.json` | `focus` (`float`), `battery` (`float`), `time` (`float`), `u_DSPActive` (`float`) |

### Funciones Globales Auxiliares:
* `getTime()`: Devuelve el tiempo continuo del juego en **segundos** (`double`, con precision sub-tick en mundo). Ideal para osciladores (`math.sin(getTime() * velocidad)`) y cooldowns (`now - lastAlert >= 3.0`).
* `printLog(mensaje)`: Imprime un mensaje en la consola y en `latest.log` con el prefijo `[Vision Goggles Lua]`.

---

## 9. Guia de Shaders GLSL y Post-Processing JSON

Todos los shaders del mod utilizan **OpenGL 3.2 Core Profile (`#version 150`)**:

1. **Archivo PostChain JSON (`assets/vision_goggles/shaders/post/mi_shader.json`)**:
   ```json
   {
     "targets": [ "swap" ],
     "passes": [
       {
         "name": "vision_goggles:mi_programa",
         "intarget": "minecraft:main",
         "outtarget": "swap"
       },
       {
         "name": "blit",
         "intarget": "swap",
         "outtarget": "minecraft:main"
       }
     ]
   }
   ```
2. **Fragment Shader (`assets/vision_goggles/shaders/program/mi_programa.fsh`)**:
   ```glsl
   #version 150
   uniform sampler2D DiffuseSampler;
   uniform vec3 colorFilter;
   uniform float time;
   in vec2 texCoord;
   out vec4 fragColor;

   void main() {
       vec4 base = texture(DiffuseSampler, texCoord);
       fragColor = vec4(base.rgb * colorFilter, 1.0);
   }
   ```

---

## 10. Flujo de Flasheo Fisico en el Juego

Los modulos programables son **chips fisicos de hardware** que se graban en la Estacion de Modificacion:

1. **Escribir o Editar el Script**: Coloca tu archivo `.lua` en `config/vision_goggles/modules/` o editalo desde `/vg`. *(Si modificas uno de los 6 ejemplos oficiales, agrega `-- [CUSTOM]` en la primera linea para conservar tus cambios entre actualizaciones del mod).*
2. **Obtener el Script de Instalacion**:
   * Abre la terminal con `/vg` o `/vg gui`, selecciona tu archivo y pulsa **`Obtener Script`**.
   * O usa el comando: `/vg givescript <id_modulo> [nombre] [color_hex]`.
3. **Quemar el Chip en la Estacion de Modificacion**:
   * **Ranura Izquierda**: `Modulo Programable` (en blanco).
   * **Ranura Central**: `Script de Instalacion` (no se consume al grabar).
   * **Ranura Derecha**: Retira el `Modulo Programado` con el firmware grabado en su NBT.
4. **Instalacion en las Gafas**:
   * Coloca tus **Gafas Modulares** (2 ranuras) o **Gafas Modulares Pro** (4 ranuras) en la estacion e inserta el chip programado.

---

## 11. Plantilla Limpia (Boilerplate)

Copia esta plantilla como punto de partida seguro para tus propios scripts:

```lua
-- ============================================================================
-- PLANTILLA OFICIAL DE MODULO LUA - VISION GOGGLES v2.0
-- ============================================================================

-- 1. Variables de estado SIEMPRE locales al archivo
local ultimoEscaneo = 0
local amenazasCache = 0

module = {
    id = "vision_goggles:mi_modulo_personalizado",
    name = "Mi Modulo Personalizado",
    type = "UTILITY",
    -- required_module = "nvg", -- Descomenta si requiere "nvg", "thermal", "bio" o "hydro"
    battery_drain = 0.05,

    onEnable = function(p)
        ultimoEscaneo = getTime()
        sound.playBeep(1.5)
        hud.showAlert("MODULO PERSONALIZADO ACTIVO", "#00FFAA", 2.0)
    end,

    onDisable = function(p)
        amenazasCache = 0
        sound.playBeep(0.8)
    end,

    onTick = function(p, batteryPct)
        local ahora = getTime()
        if (ahora - ultimoEscaneo) >= 0.5 then
            ultimoEscaneo = ahora
            local cercanos = world.getEntitiesInRadius(20.0)
            local cuenta = 0
            if cercanos ~= nil then
                for i = 1, #cercanos do
                    if cercanos[i].is_hostile then
                        cuenta = cuenta + 1
                    end
                end
            end
            amenazasCache = cuenta
        end
    end,

    onRenderHud = function(h)
        local x = 12
        local y = 44
        local color = amenazasCache > 0 and "#FF4444" or "#00FFAA"
        h.drawRect(x, y, 108, 18, "#AA081018")
        h.drawRect(x, y, 2, 18, color)
        h.drawText(x + 6, y + 5, "HOSTILES (20m): " .. amenazasCache, color, 0.75)
    end
}
```

---

## 12. Limites del Sandbox, Buenas Practicas y Catalogo de Scripts

### Reglas del Sandbox y Rendimiento:
1. **Compatibilidad Lua 5.2 (LuaJ 3.0.1)**: No utilices operadores exclusivos de Lua 5.3+ como `//` (division entera) ni operadores bitwise nativos (`&`, `|`, `>>`). Usa `math.floor(a / b)` y la libreria estandar `bit32`.
2. **Aislamiento con `local`**: Todos los scripts se cargan sobre una misma instancia de `Globals`. Cualquier variable o funcion auxiliar sin `local` puede ser sobrescrita por otro script.
3. **Presupuesto de CPU (`100,000` instrucciones por callback)**: `SandboxedDebugLib` aborta automaticamente cualquier bucle infinito o funcion que supere 100,000 instrucciones bytecode.
   * **Patron recomendado**: Ejecuta `world.getEntitiesInRadius` o barridos `world.getBlock` dentro de `onTick` (20 veces/seg) o mediante *time-slicing* (una capa por tick), guarda los resultados en una tabla `local`, y usa `onRenderHud` solo para proyectar y dibujar.
4. **Presupuesto de GPU (`1,000` comandos HUD por fotograma)**: `LuaHudCanvas` admite hasta 1,000 primitivas `draw*` por frame entre todos los modulos activos.
5. **Seguridad del Sandbox**: Las librerias `io`, `package`, `require`, `luajava`, `debug`, `loadfile` y `dofile` estan anuladas; `os` conserva unicamente `clock`, `time`, `difftime` y `date`.

### Catalogo de los 6 Scripts Oficiales Incluidos (`assets/vision_goggles/lua_examples/`):

| Archivo | ID (`module.id`) | Tipo / Req. | Que implementa y enseña |
| :--- | :--- | :--- | :--- |
| `01_radar_tactico_360.lua` | `vision_goggles:radar_amenazas` | `UTILITY` (Req: `"sonar"`) | **Upgrade DSP Sonar Pro 360°**: amplifica el modulo hardware `Sonar` (`SonarRadarModule` cede el HUD automaticamente), respeta `player.getSonarMode()` (`ALL`/`PLY`/`UND`), barrido 20 TPS hasta 36m, desnivel vertical y sonar acustico progresivo. |
| `02_visor_ar_objetivos.lua` | `vision_goggles:threat_scanner` | `UTILITY` (Req: `"bio"`) | **Realidad Aumentada 3D (`world.worldToScreen`)** con corchetes `[  ]` sobre enemigos en el mundo, chevrones laterales fuera de pantalla (`<`/`>`) y tarjeta IFF de fijacion en mira (`world.getTargetEntity`). |
| `03_visor_combate_reactivo.lua` | `vision_goggles:cyber_visor` | `VISION_MODE` (Req: `"nvg"`) | **Visor Combate Gen-3 Auto-Gate**: activa `u_DSPActive = 1.0` en `nvg.fsh` (75% menos grano y Smart Auto-Gating anti-ceguera), detector de proyectiles entrantes (`world.getProjectilesInRadius`), anillo direccional 360° alrededor de la mira, alerta de armadura critica y telemetria de dano. |
| `04_hud_piloto_avionica.lua` | `vision_goggles:tactical_hud` | `HUD` (Autonomo) | **HUD de avionica y vuelo** con cinta de brujula deslizante (`000°=Norte`), escalera de cabeceo (*Pitch Ladder*), velocimetro `m/s`, variometro `VSI` (`dy/dt`), alarma GPWS `¡PULL UP!`, reloj solar 24h, clima y efectos de pocion. |
| `05_escaner_geologico_entorno.lua` | `vision_goggles:geo_scanner` | `UTILITY` (Autonomo) | **Prospector Geologico AR de 14m**: escaneo rapido en Java (`world.scanBlocks`) con agrupacion automatica de vetas (`DIAMANTE x4 10.2m`), rombos AR 3D + flechas direccionales, Geiger progresivo y alerta de lava bajo los pies y frente a la mira. |
| `06_suite_termica_flir.lua` | `vision_goggles:thermal_palette` | `UTILITY` (Req: `"thermal"`) | **Clasificador termico IFF Ironbow calibrado** (`#00FFFF` / `#FFBB00` / `#FF8800`), filtrado por `e.emits_heat` para evitar etiquetas `[HOT]` sobre mobs frios/no-muertos, estabilizador criogenico en Nether, barra FLIR y temperatura `°C`. |
