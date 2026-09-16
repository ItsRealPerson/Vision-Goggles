# 🛠️ Plan Maestro de Migración y Modernización de Shaders: Vision Goggles

> **Objetivo:** Refactorizar y modernizar el pipeline de shaders de *Vision Goggles*, unificando a OpenGL 3.2 Core Profile (`#version 150`), eliminando artefactos gráficos y añadiendo una arquitectura de **Coprocesador Táctico / DSP (Digital Signal Processing)** que mejora automáticamente la calidad y funciones de los shaders si **CC: Tweaked** está instalado.

---

## 🌿 1. Estrategia de Rama (Git Workflow)

Crear una rama dedicada en el repositorio para aislar los cambios del renderizador, uniforms y shaders:

```bash
git checkout -b refactor/modern-shaders-pipeline
```

---

## 🧠 2. Concepto Arquitectónico: El "Coprocesador DSP"

Las gafas funcionarán en dos modos automáticos según el entorno de mods del jugador:

```
┌─────────────────────────┐     ┌────────────────────────────────────────────────────────┐
│ SIN CC: Tweaked         │ ──► │ MODO ANALÓGICO BÁSICO                                  │
│ (Vanilla / Modpack base)│     │ Grano de tubo clásico, térmico estándar, linterna fija │
└─────────────────────────┘     └────────────────────────────────────────────────────────┘

┌─────────────────────────┐     ┌────────────────────────────────────────────────────────┐
│ CON CC: Tweaked         │ ──► │ MODO COMPUTARIZADO / DSP                               │
│ (Detección de mod)      │     │ Denoising digital, Fusión ENVG (Edges), IFF y Adaptativo│
└─────────────────────────┘     └────────────────────────────────────────────────────────┘
```

En `VisionShaderManager.java`, se inyecta un nuevo uniform global a los shaders:
```java
boolean hasDSP = Platform.isModLoaded("computercraft"); // detecta CC: Tweaked
shader.safeGetUniform("u_DSPActive").set(hasDSP ? 1.0f : 0.0f);
```

---

## 📋 3. Fases de la Migración

### 🔹 Fase 1: Unificación de Perfil GLSL (`#version 150 Core`)
Actualmente existe una discrepancia técnica: `flashlight.fsh` usa `#version 150`, mientras que `nvg.fsh`, `thermal.fsh` y `bio.fsh` usan la sintaxis legacy `#version 120`.

* **Archivos afectados:**
  * `common/src/main/resources/assets/vision_goggles/shaders/program/nvg.fsh`
  * `common/src/main/resources/assets/vision_goggles/shaders/program/thermal.fsh`
  * `common/src/main/resources/assets/vision_goggles/shaders/program/bio.fsh`
  * `common/src/main/resources/assets/vision_goggles/shaders/program/hydro.fsh`
  * `common/src/main/resources/assets/vision_goggles/shaders/program/hydro_dry.fsh`
* **Cambios de sintaxis requeridos:**
  1. Reemplazar `#version 120` por `#version 150`.
  2. Migrar `varying vec2 texCoord;` $\rightarrow$ `in vec2 texCoord;`.
  3. Reemplazar `gl_FragColor = ...;` por un buffer de salida explícito:
     ```glsl
     out vec4 fragColor;
     // ...
     fragColor = vec4(...);
     ```
  4. Reemplazar llamadas a `texture2D(sampler, uv)` por `texture(sampler, uv)`.

---

### 🔹 Fase 2: Reemplazo del Generador de Ruido (Anti-Banding)
El generador actual basado en funciones trigonométricas (`fract(sin(dot(...)) * 43758.5453)`) produce bandas diagonales y pérdidas de precisión en tarjetas AMD e Intel.

* **Solución:** Implementar una función de ruido basada en hash entero / pseudo-aleatorio estable:
```glsl
float hashNoise(vec2 p, float t) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z + fract(t * 13.37));
}
```

---

### 🔹 Fase 3: Visión Nocturna (`nvg.fsh`) + Mejora DSP
* **Modo Estándar (Sin CC):**
  * Ruido analógico de fósforo verde con centelleo (*scintillation*) más denso en zonas oscuras.
  * Ojo de pez y viñeta de lente óptica militar.
* **Modo Mejorado (Con CC: Tweaked - `u_DSPActive == 1.0`):**
  * **Filtro de Reducción de Ruido Digital (DSP Denoising):** El ruido granulado se reduce al 25%, produciendo una imagen táctica ultra nítida de alta definición.
  * **Auto-Gating Inteligente:** Al mirar luces brillantes (lava, antorchas), la computadora digital atenúa selectivamente el punto de luz sin cegar toda la pantalla.
  * **Soporte Fósforo Blanco (P45):** Posibilidad de alternar entre verde clásico y fósforo blanco táctico de alta resolución.

---

### 🔹 Fase 4: Visión Térmica (`thermal.fsh`) + Modo Fusión ENVG
* **Modo Estándar (Sin CC):**
  * Rampa térmica realista de 5 paradas (**Ironbow FLIR**): Negro $\rightarrow$ Púrpura $\rightarrow$ Rojo $\rightarrow$ Naranja $\rightarrow$ Amarillo $\rightarrow$ Blanco.
* **Modo Mejorado (Con CC: Tweaked - `u_DSPActive == 1.0`):**
  * **Fusión ENVG (Enhanced Night Vision Goggles):** La computadora ejecuta un filtro Sobel 3x3 en tiempo real y superpone una **malla de contornos luminosos (edge overlay)** sobre el terreno.
  * El jugador no solo ve la firma de calor de los mobs, sino que **distingue perfectamente el relieve de las paredes, esquinas y desniveles de las cuevas**, eliminando la desorientación espacial.
  * **Calibración NUC Digital:** Micro-parpadeo sutil periódico simulando la recalibración digital del sensor.

---

### 🔹 Fase 5: Visor Biométrico (`bio.fsh`) + Sistema IFF
* **Modo Estándar (Sin CC):**
  * Tinte ámbar de alto contraste con cuadrícula estática y barra de barrido vertical.
* **Modo Mejorado (Con CC: Tweaked - `u_DSPActive == 1.0`):**
  * **Target Acquisition Brackets:** El shader proyecta corchetes de fijación `[ + ]` en el centro de la pantalla que se abren o cierran con el zoom (`focus`).
  * **Sistema IFF (Identify Friend or Foe):** Los contornos de las entidades detectadas se colorean dinámicamente:
    * **Verde esmeralda** para jugadores pacíficos y aldeanos.
    * **Rojo pulsante** para mobs hostiles.

---

### 🔹 Fase 6: Haz de Linterna (`flashlight.fsh`) + Foco Adaptativo
* **Modo Estándar (Sin CC):**
  * Óptica de reflector parabólico realista: Núcleo central (**Hotspot**), luz difusa perimetral (**Spill**) y borde de lente (**Cutoff**).
* **Modo Mejorado (Con CC: Tweaked - `u_DSPActive == 1.0`):**
  * **Foco Digital Adaptativo:** La apertura del haz se ajusta automáticamente según la distancia del bloque al que apuntas:
    * De cerca $\rightarrow$ Se abre en gran angular suave para no cegar al jugador con el rebote.
    * De lejos $\rightarrow$ Se concentra en un haz estrecho de largo alcance.

---

### 🔹 Fase 7: Mecánica de Inyección en Java (`VisionShaderManager.java`)
1. **Detección Dinámica:**
   ```java
   public static boolean isDspAvailable() {
       return Platform.isModLoaded("computercraft");
   }
   ```
2. **Paso de Parámetros al Shader:**
   En cada frame donde el shader esté activo, inyectar el uniform:
   ```java
   shader.safeGetUniform("u_DSPActive").set(isDspAvailable() ? 1.0f : 0.0f);
   ```
3. **Módulo Opcional en Modification Station (Opcional):**
   Si se desea una progresión más inmersiva, se puede crear el ítem `"Módulo de Coprocesador Táctico"` que requiere componentes de CC: Tweaked para ser crafteado e instalado en las gafas.

---

## ✅ 4. Matriz de Verificación y Testing

| Prueba | Entorno de Prueba | Criterio de Éxito |
| :--- | :--- | :--- |
| **Vanilla / Sin CC** | Solo Vision Goggles + Forge/NeoForge | Los shaders cargan en modo analógico (#version 150) sin errores. |
| **Con CC: Tweaked** | Vision Goggles + CC: Tweaked | `u_DSPActive` se activa en 1.0; se aprecian contornos Sobel en Térmico y reducción de ruido en NVG. |
| **Embeddium / Sodium** | Con Embeddium activo | El post-procesado con y sin DSP funciona de forma fluida a 60+ FPS. |
| **Cambio de Cámara (F5)** | Tercera y Primera persona | Los efectos mejorados se restauran instantáneamente al volver a primera persona. |

---

## 🚀 5. Plan de Commits Sugerido

1. `build(glsl): migrate all post-processing shaders to #version 150 core`
2. `refactor(noise): implement stable integer hash noise algorithm`
3. `feat(dsp): introduce u_DSPActive uniform based on CC: Tweaked detection`
4. `feat(nvg): implement scintillation and DSP digital denoising`
5. `feat(thermal): implement ironbow gradient and ENVG edge fusion mode`
6. `feat(bio): add targeting brackets and IFF enemy differentiation`
7. `feat(flashlight): add hotspot-spill optics and adaptive focus with DSP`
