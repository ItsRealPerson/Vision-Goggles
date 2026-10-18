# Plan v9: Rediseño AAA del Menú Radial Táctico (Cuñas Donut, Iconos de Ítems y Retícula Militar) - Alpha v2.0.0

## 1. Contexto y Requerimiento del Usuario
El usuario solicitó:
> *"Otra cosa, no hay manera de que se pueda diseñar un menu radial mejor? y otra cosa, esto sigue siendo una alpha de la 2.0.0 no lo olvides"*

### Diagnóstico del Menú Radial Previo:
- La versión anterior utilizaba botones rectangulares planos distribuidos sobre un círculo (`guiGraphics.fill` y `renderOutline`), lo cual lucía como un prototipo básico y no como un menú radial moderno de grado militar.
- Faltaban iconos visuales de los módulos/ítems en cada opción.
- Faltaban arcos circulares reales (donut wedges), gradientes radiales de fósforo/energía y marcas de telemetría de compás (ticks de grados y retícula militar).

---

## 2. Nueva Arquitectura Visual AAA de `TacticalRadialScreen`

### A. Geometría Curva Real (Sectores Donut con Blaze3D / Tesselator)
- **Cuñas Anulares**: Generadas matemáticamente mediante descomposición en triángulos con `DefaultVertexFormat.POSITION_COLOR` y `GameRenderer.getPositionColorShader()`.
- **Radio Interior ($R_{in}$)**: 38.0 px.
- **Radio Exterior ($R_{out}$)**: 96.0 px (se expande a 99.0 px al pasar el cursor para feedback táctil).
- **Separación Angular (Gap)**: 3.0° entre cuñas adyacentes para una separación limpia.
- **Gradientes Radiales**: Cada cuña interpola automáticamente desde un tono oscuro militar (`0xAA0B1116`) en el radio interior hacia un brillo holográfico con el color temático del módulo (`0x88...`) en el radio exterior.

### B. Iconografía Integrada en Cada Rebanada
- Cada opción radial ahora lleva asociado su `ItemStack` representativo:
  - **NVG**: `ModItems.NVG_MODULE`
  - **THERMAL**: `ModItems.THERMAL_MODULE`
  - **BIOMÉTRICO**: `ModItems.BIO_MODULE`
  - **HYDRO**: `ModItems.HYDRO_MODULE`
  - **STANDBY**: `ModItems.MODULAR_GOGGLES`
  - **SEGURIDAD**: `ModItems.SPAWN_SECURITY_MODULE`
  - **SONAR**: `ModItems.SONAR_MODULE`
  - **ZOOM**: `ModItems.ZOOM_MODULE`
  - **LINTERNA**: `ModItems.TACTICAL_FLASHLIGHT`
  - **BATERÍA**: `ModItems.NVG_BATTERY`
- Los iconos se renderizan en el radio medio ($R = 64\text{ px}$) con escala dinámica aumentada a **1.15x** al hacer hover.

### C. HUD Militar Táctico y Compás de Grados
- **Anillo Exterior de Compás ($R = 104\text{ px}$)**: Anillo cian translúcido con muescas de grados militares:
  - Ticks menores cada 10°.
  - Ticks medianos cada 30°.
  - Ticks cardinales prominentes cada 90°.
- **Retícula / Mira Central**: Mira militar con brazos de precisión y punto de mira central.
- **Identificador de Versión Alpha**: Etiqueta militar central `VG-OS // v2.0-ALPHA` y estado `SISTEMA LISTO`.

---

## 3. Plan de Ejecución
1. Actualizar `TacticalRadialScreen.java` con el nuevo pipeline matemático de renderizado en Blaze3D.
2. Compilar con subagente (`forge` y `fabric`).
3. Desplegar los JARs actualizados en ATLauncher.
4. Documentar en `docs/CHANGELOG.md`.
5. Commitear y pushear a GitHub.
