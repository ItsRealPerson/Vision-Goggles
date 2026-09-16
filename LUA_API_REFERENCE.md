# Manual de Referencia de la API de Scripting Lua - Vision Goggles v2.0

Bienvenido al manual oficial y completo para la creacion de modulos, HUDs tacticos y control de shaders en **Vision Goggles**.

Este documento detalla el 100% de las funciones, estructuras, tablas globales y ciclo de vida expuestos por el motor de Lua en Java para Minecraft 1.20.1.

---

## Indice
1. [Estructura Basica de un Script](#1-estructura-basica-de-un-script)
2. [Ciclo de Vida del Modulo](#2-ciclo-de-vida-del-modulo)
3. [Tabla Global player](#3-tabla-global-player)
4. [Tabla Global world](#4-tabla-global-world)
5. [Tabla Global hud (Lienzo Tactico)](#5-tabla-global-hud-lienzo-tactico)
6. [Tabla Global sound](#6-tabla-global-sound)
7. [Puente de Shaders: Tabla uniforms](#7-puente-de-shaders-tabla-uniforms)
8. [Funciones Globales Auxiliares](#8-funciones-globales-auxiliares)
9. [Guia de Shaders GLSL y Post-Processing JSON](#9-guia-de-shaders-glsl-y-post-processing-json)
10. [Flujo de Flasheo Fisico en el Juego](#10-flujo-de-flasheo-fisico-en-el-juego)
11. [Plantilla Limpia (Boilerplate)](#11-plantilla-limpia-boilerplate)

---

## 1. Estructura Basica de un Script

Cada archivo `.lua` ubicado en la carpeta `config/vision_goggles/modules/` debe declarar una tabla global obligatoria llamada `module`:

```lua
module = {
    id              = "vision_goggles:mi_modulo_tactico",  -- Identificador unico (namespaced)
    name            = "Mi Modulo Tactico",                 -- Nombre visible en el juego y en el chip
    type            = "VISION_MODE",                     -- Tipo: "VISION_MODE" (controla shaders) o "UTILITY" (solo HUD)
    shader          = "shaders/post/nvg.json",             -- Archivo JSON de shader a cargar (opcional si es UTILITY)
    required_module = "nvg",                             -- Hardware requerido: "nvg", "thermal", "bio", o nil (autonomo)
    battery_drain   = 0.5,                                 -- Drenaje adicional de bateria por segundo
}
```

### Tipos de Modulo (`type`):
* **`"VISION_MODE"`**: Se comporta como un modo de vision completo. Al activarse, monta el pipeline de shaders indicado en `shader` y permite ajustar variables dinamicas mediante `onUpdateUniforms`.
* **`"UTILITY"`**: Funciona en segundo plano como modulo de utilidad tactica (HUD, alertas de proximidad, radar, etc.) sin cambiar el shader de vision del jugador.

### Dependencia de Hardware (`required_module`):
* `"nvg"`: Requiere que las gafas modulares tengan instalado el modulo de **Vision Nocturna**.
* `"thermal"`: Requiere el modulo de **Visor Termico**.
* `"bio"`: Requiere el modulo **Biometrico**.
* `nil` (o omitido): Modulo **autonomo**. Puede instalarse en cualquier gafa modular libremente (ideal para radares, brujulas o HUDs de supervivencia).

---

## 2. Ciclo de Vida del Modulo

El mod invoca funciones callback en momentos clave de la partida:

```lua
-- Se ejecuta una sola vez cuando el jugador enciende el visor o activa el modo
onEnable = function(player)
    sound.playBeep(1.5)
    hud.showAlert("SISTEMA INICIADO", "#00FFAA", 2.0)
end,

-- Se ejecuta una sola vez cuando el jugador apaga el visor o cambia a otro modo
onDisable = function(player)
    sound.playBeep(0.8)
end,

-- Se ejecuta en cada tick del cliente (20 veces por segundo)
-- Ideal para escanear entidades cercanas, timers y alertas
onTick = function(player, batteryPct)
    -- batteryPct: Float entre 0.0 (vacia) y 1.0 (100% llena)
end,

-- Se ejecuta en cada fotograma antes de dibujar el shader de pantalla
-- Ideal para cambiar colores, contraste, calor infrarrojo o aberracion cromatica
onUpdateUniforms = function(player, uniforms)
    uniforms.setVec3("colorFilter", 0.2, 1.0, 0.4)
end,

-- Se ejecuta en cada fotograma durante el pase de renderizado del HUD de Minecraft
-- Permite dibujar figuras geometricas, texto, barras y radares
onRenderHud = function(hud)
    local w = hud.getWidth()
    local h = hud.getHeight()
    hud.drawText(10, 10, "ESTADO OK", "#00FF88", 1.0)
end
```

---

## 3. Tabla Global `player`

La tabla `player` proporciona telemetria en tiempo real sobre el jugador local:

| Funcion | Retorno | Descripcion |
| :--- | :--- | :--- |
| `player.getHealth()` | `number` | Salud actual en medio-corazones (ej. `20.0`). |
| `player.getMaxHealth()` | `number` | Salud maxima del jugador (ej. `20.0` o mas con absorcion/vida extra). |
| `player.getFoodLevel()` | `number` | Nivel de hambre entre `0` y `20`. |
| `player.getSaturation()` | `number` | Nivel de saturacion exacta (ej. `5.0`). |
| `player.getArmor()` | `number` | Puntos de armadura fisica (`0` a `20+`). |
| `player.getOxygen()` | `number` | Oxigeno restante en porcentaje (`0.0` a `100.0`). |
| `player.getX()` | `number` | Coordenada X absoluta en el mundo. |
| `player.getY()` | `number` | Coordenada Y (altura / capa) en el mundo. |
| `player.getZ()` | `number` | Coordenada Z absoluta en el mundo. |
| `player.getYaw()` | `number` | Rotacion horizontal de la mirada (en grados `-180` a `180` o `0` a `360`). |
| `player.getPitch()` | `number` | Inclinacion vertical de la mirada (`-90` arriba, `+90` abajo). |
| `player.getSpeed()` | `number` | Velocidad de movimiento horizontal en bloques por segundo. |
| `player.getDimension()` | `string` | Identificador de la dimension (`"overworld"`, `"the_nether"`, `"the_end"`). |
| `player.getBiome()` | `string` | Nombre del bioma actual (ej. `"minecraft:plains"`, `"desert"`). |
| `player.getLightLevel()` | `number` | Nivel de luz en la posicion del jugador (`0` a `15`). |
| `player.isFlying()` | `boolean` | `true` si el jugador esta volando (creativo o elytras). |
| `player.isSprinting()` | `boolean` | `true` si el jugador esta corriendo. |
| `player.isCrouching()` | `boolean` | `true` si el jugador esta agachado (sneak). |
| `player.isInWater()` | `boolean` | `true` si el jugador tiene la cabeza o cuerpo en agua. |
| `player.isDay()` | `boolean` | `true` si es de dia en la superficie del Overworld. |
| `player.getBattery()` | `number` | Nivel de bateria de las gafas (`0.0` a `1.0`). |
| `player.hasGoggles()` | `boolean` | `true` si el jugador tiene puestas unas gafas de Vision Goggles. |
| `player.isGogglesActive()`| `boolean` | `true` si el visor esta encendido. |
| `player.hasModule(name)` | `boolean` | `true` si las gafas tienen instalado el modulo dado (ej. `"nvg"`, `"thermal"`). |
| `player.getEffects()` | `table` | Lista de efectos de pocion activos con `{ id, duration, amplifier, is_beneficial }`. |

---

## 4. Tabla Global `world`

Permite escanear el entorno, detectar entidades y realizar trazado de rayos (*raycasting*):

### `world.getTargetEntity(range)`
Lanza un rayo desde los ojos del jugador hacia donde apunta la mira y devuelve la entidad impactada (hasta `range` bloques, por defecto `30`):
```lua
local target = world.getTargetEntity(25)
if target ~= nil then
    print(target.name)          -- Nombre del mob (ej. "Zombie")
    print(target.health)        -- Vida actual
    print(target.max_health)    -- Vida maxima
    print(target.distance)      -- Distancia al jugador en bloques
    print(target.armor)         -- Puntos de armadura del mob
    print(target.is_hostile)    -- true si es monstruo agresivo
    print(target.is_player)     -- true si es otro jugador
end
```

### `world.getEntitiesInRadius(radius)`
Devuelve una lista de todas las entidades vivas en una esfera de `radius` bloques alrededor del jugador:
```lua
local nearby = world.getEntitiesInRadius(16)
for i = 1, #nearby do
    local e = nearby[i]
    if e.is_hostile then
        -- Procesar enemigo
    end
end
```

### `world.raycastBlock(range)`
Lanza un rayo hacia el bloque al que apunta la mira del jugador:
```lua
local hit = world.raycastBlock(10)
if hit ~= nil then
    print(hit.id)        -- ID de registro (ej. "minecraft:diamond_ore")
    print(hit.x, hit.y, hit.z) -- Coordenadas del bloque
    print(hit.distance)  -- Distancia exacta al bloque
end
```

### `world.getBlock(x, y, z)`
Devuelve el ID de bloque (`string`) en las coordenadas especificadas (ej. `"minecraft:lava"`).

### `world.getLightAt(x, y, z)`
Devuelve el nivel de luz combinado (`0` a `15`) en una posicion del mundo.

---

## 5. Tabla Global `hud` (Lienzo Tactico)

Disponible exclusivamente dentro de la funcion `onRenderHud(h)`. Todos los colores se especifican en formato hexadecimal `#RRGGBB` o `#AARRGGBB`:

| Funcion | Parametros | Descripcion |
| :--- | :--- | :--- |
| `h.getWidth()` | *ninguno* | Ancho de la pantalla escalada en pixeles. |
| `h.getHeight()` | *ninguno* | Alto de la pantalla escalada en pixeles. |
| `h.drawText(x, y, text, color, scale)` | `(int, int, string, string, float)` | Dibuja texto con escala configurable (`0.5` a `2.0`). |
| `h.drawRect(x, y, w, h, color)` | `(int, int, int, int, string)` | Dibuja un rectangulo solido o semitransparente. |
| `h.drawProgressBar(x, y, w, h, pct, color)` | `(int, int, int, int, float, string)` | Dibuja una barra de progreso con fondo oscuro y relleno proporcional (`pct` de 0.0 a 1.0). |
| `h.drawLine(x1, y1, x2, y2, color)` | `(int, int, int, int, string)` | Traza una linea recta entre dos puntos. |
| `h.drawCircle(cx, cy, radius, color)` | `(int, int, int, string)` | Dibuja la circunferencia vacia de un circulo (ideal para radares y reticulas). |
| `h.drawDisc(cx, cy, radius, color)` | `(int, int, int, string)` | Dibuja un circulo relleno / disco solido (ideal para puntos de radar). |
| `h.showAlert(msg, color, duration)` | `(string, string, float)` | Muestra un cartel de alerta tactica parpadeante en pantalla durante `duration` segundos. |

---

## 6. Tabla Global `sound`

Permite emitir efectos de sonido tacticos del visor:

* `sound.playBeep(pitch)`: Emite un pitido electronico. `pitch` modula el tono (`0.5` grave, `1.0` normal, `2.0` agudo).
* `sound.playAlert()`: Emite una alarma tactica de advertencia militar.
* `sound.playClick()`: Clic mecanico de boton o cambio de modo.
* `sound.playHum()`: Zumbido de condensador de encendido.

---

## 7. Puente de Shaders: Tabla `uniforms`

Disponible exclusivamente dentro de la funcion `onUpdateUniforms(player, uniforms)`. Permite enviar variables numericas y vectoriales en tiempo real a los shaders GLSL:

* `uniforms.setFloat("nombreUniform", floatVal)`
* `uniforms.setVec2("nombreUniform", x, y)`
* `uniforms.setVec3("nombreUniform", r, g, b)`
* `uniforms.setVec4("nombreUniform", r, g, b, a)`
* `uniforms.setInt("nombreUniform", intVal)`

### Uniforms Nativos en los Shaders de Vision Goggles:
* **`colorFilter` (vec3)**: Multiplicador RGB aplicado a la pantalla.
  * NVG Verde clasico: `(0.1, 1.0, 0.4)`
  * Fosforo Blanco Militar: `(0.80, 0.95, 1.12)`
  * Termico Predador Infrarrojo: `(1.10, 0.50, 0.15)`
  * Hydro Abisal: `(0.15, 0.90, 0.95)`
* **`gain` / `exposure` (float)**: Amplificacion luminica digital.
* **`time` (float)**: Tiempo continuo del juego para animaciones de ruido o estatica.
* **`battery` (float)**: Carga de la bateria transmitida al shader.

---

## 8. Funciones Globales Auxiliares

* `getTime()`: Devuelve el tiempo transcurrido en segundos (`double`) con precision de submilisegundo para animaciones de rotacion, pulsos u ondas sinusoidales (`math.sin(getTime() * velocidad)`).
* `printLog(mensaje)`: Imprime un mensaje en la consola de Minecraft y en el archivo `latest.log` con el prefijo `[Vision Goggles Lua]`.

---

## 9. Guia de Shaders GLSL y Post-Processing JSON

Para crear un efecto de post-procesamiento totalmente personalizado:

1. **Archivo PostChain JSON (`assets/vision_goggles/shaders/post/mi_shader.json`)**:
   Define los pases de renderizado y el programa GLSL asociado:
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
       fragColor = vec4(base.rgb * colorFilter, base.a);
   }
   ```

---

## 10. Flujo de Flasheo Fisico en el Juego

Los modulos programables no son comandos virtuales; son **chips fisicos de hardware** que se integran en el inventario y en las gafas:

1. **Escribir el Script**: Crea o edita tu archivo `.lua` en `config/vision_goggles/modules/`.
2. **Obtener el Script de Instalacion**:
   * Abre la terminal con `/vg` o `/vg gui`.
   * Selecciona tu archivo y haz clic en el boton **`Obtener Script`**.
   * O usa el comando: `/vg givescript <id_modulo> [nombre] [color_hex]`.
3. **Quemar el Chip en la Estacion de Modificacion**:
   * **Ranura Izquierda**: Coloca un `Modulo Programable` (en blanco).
   * **Ranura Central**: Coloca el `Script de Instalacion`.
   * **Ranura Derecha**: Retira el `Modulo Programado` con tu script grabado fisicamente en su NBT.
4. **Instalacion en las Gafas**:
   * Coloca tus **Gafas Modulares** en la estacion.
   * Inserta el chip programado en una ranura libre de modulo.
   * ¡Enciende el visor y disfruta de tu interfaz tactica personalizada!
