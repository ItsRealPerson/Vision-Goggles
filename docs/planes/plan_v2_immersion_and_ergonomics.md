# Plan de Desarrollo v2.1: Inmersión Sensorial y Ergonomía Táctica

## 1. Contexto y Objetivos
Transformar la experiencia táctica de **Vision Goggles** añadiendo "jugo sensorial" (tactilidad, audio inmersivo, físicas) y ergonomía de combate moderna (menú radial y zoom continuo gradual), atendiendo directamente al feedback del usuario.

---

## 2. Fase 1: Coherencia de Haz y Estática de Linterna (Completado)
- **Problema en Capturas:** La estática y partículas de polvo formaban un círculo gigante fijo en el cielo oscuro independientemente del modo activo (FOCUSED / WIDE / CLOSE).
- **Causa:** `spillRadius` y `cutoffRadius` eran excesivos en modo enfocado y el ruido de motas se multiplicaba por `cutoff` en vez de por los fotones reales del haz.
- **Solución implementada:**
  - `FOCUSED`: Cono cerrado con `cutoffRadius = 0.26` y haz concentrado.
  - `WIDE`: Cono equilibrado con `cutoffRadius = 0.50`.
  - `CLOSE / FLOOD`: Gran angular con `cutoffRadius = 0.72`.
  - Motes y turbulencia estrictamente modulados por `beamCore` (cero estática en el vacío oscuro).

---

## 3. Fase 2: El "Jugo" Sensorial y la Tactilidad (Audio y Físicas)
1. **Audio de Tubo Fotónico (*Tube Whine* & *Capacitor Charge*):**
   - Sonido inicial de arranque: Carga de condensador agudo (*Eeeeee-zt*) al encender NVG.
   - Sonido ambiental en bucle (*ambient hum*): Zumbido sutil analógico mientras las gafas están encendidas.
   - Sonido de corte y descarga al apagarse.
2. **Ritual de Encendido Físico (*Power-Up Flicker*):**
   - Micro-secuencia de calentamiento (0.15s) en los shaders al encender:
     - Frame 0-3: Flash inicial blanco/verde brillante (sobrecarga de encendido).
     - Frame 4-7: Breve parpadeo analógico.
     - Frame 8+: Estabilización en ganancia normal.
3. **Físicas y Animación en 3ª Persona (Flip-Up / Flip-Down):**
   - Cuando las gafas están apagadas: Modelo levantado mecánicamente en la frente o en el casco.
   - Al encender: Animación y sonido de montura Wilcox (*clack-clack*) bajando las gafas sobre los ojos.
   - Puntos emisivos en 3ª persona: Los tubos frontales brillan en la oscuridad para otros jugadores (verde para NVG, azul/rojo para Thermal, cian para Hydro).

---

## 4. Fase 3: Ergonomía en Combate
1. **Rediseño del Módulo de Zoom (Continuo / Gradual):**
   - Reemplazar el salto brusco de un solo nivel por un sistema suave y continuo de aumento (1x a 8x).
   - Control intuitivo: Mantener pulsada la tecla de zoom (o apuntar) y girar la rueda del ratón (`Mouse Scroll`) para regular el aumento milimétricamente.
   - Transición fluida con interpolación de FOV (`MathHelper.lerp`).
   - Telémetro táctico (*Rangefinder*): Mostrar en el HUD la distancia en bloques al objetivo y el zoom actual (ej. `[4.2x] 48m`).
2. **Menú Radial Táctico:**
   - Mantener pulsada una tecla configurable (ej. `G`) para abrir una rueda de selección táctica semi-transparente alrededor de la retícula.
   - Opciones accesibles con movimiento de ratón o teclado:
     - Modos de visión (Off, NVG, Thermal, Bio, Hydro).
     - Modos de linterna (Off, Spot, Wide, Flood).
     - Acceso rápido a recarga de batería.
