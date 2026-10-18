# Plan v10: Corrección de Layout de Módulos, Sockets Tácticos y Eliminación de Solapamiento - Alpha v2.0.0

## 1. Contexto y Diagnóstico
El usuario reportó sobre la captura `media_1792351772655_f76f3700.png`:
> *"No se ve mal... pero puede ser mejor no? tipo la cosa es que si traemos los items ahi, se ve que no estan bien puestas"*

### Análisis del Fallo:
1. **Solapamiento Crítico en Opciones Horizontales**:
   - En las opciones laterales (9 o'clock `LINTERNA` y 3 o'clock `STANDBY`), la separación polar en el mismo vector provocaba que el texto (de hasta 48 px de ancho) se renderizara directamente encima del icono del ítem.
   - En la captura, la linterna tapaba las letras de `"LINTERNA"`, pareciendo `"LINTERN.I"`, y las gafas cubrían `"STANDBY"`.
2. **Falta de Chasis de Montaje**:
   - Los ítems parecían "pegados" o flotando sin integración física dentro de las cuñas.
3. **Gafas Oscuras en Standby**:
   - Usar `helmet.copy()` renderizaba el modelo de cabeza 3D de armadura de Minecraft, viéndose oscuro y poco distinguible a 16x16.

---

## 2. Nueva Arquitectura Matemática y Visual de `TacticalRadialScreen`

### A. Geometría Calibrada del Donut
- **Radio Interior ($R_{in}$)**: $42.0\text{f}$ (proporciona mayor espacio al hub central).
- **Radio Exterior ($R_{out}$)**: $112.0\text{f}$ (profundidad radial de 70 px).
- **Radio Medio ($R_{mid}$)**: $77.0\text{f}$.
- **Radio de Compás ($R_{compass}$)**: $120.0\text{f}$.
- **Gap Angular**: $3.0^\circ$ entre cuñas adyacentes.

### B. Stacking Cartesiano Inmune a Colisiones
Para cada rebanada en $(cx_{wedge}, cy_{wedge}) = (cx + R_{mid} \cos\theta, cy + R_{mid} \sin\theta)$:
- **Socket Táctico e Icono**: Anclado a $(cx_{wedge}, cy_{wedge} - 6.0\text{f})$.
- **Etiqueta de Texto**: Anclada rígidamente a $(cx_{wedge}, cy_{wedge} + 7.0\text{f})$.
- **Garantía Matemática**: Despeje vertical positivo garantizado de $+1\text{ px}$ en cualquier ángulo. Nunca colisionan en horizontal, vertical ni diagonal.
- **Protección de Escala Angular**: Escala dinámica automática si el título excede el ancho de arco disponible de la cuña.

### C. Sockets Tácticos (Tactical Module Bezels)
- Placa de cristal blindado recesada ($18 \times 18\text{ px}$) con fondo pizarra oscuro `0xAA070E14` y brillo holográfico al seleccionarse.
- Marco biselado de contorno.
- Cuatro soportes angulares militares (Corner Reticle Brackets) en las esquinas que brillan con el color del módulo.

### D. Icono de Standby Limpio
- Reemplazo del modelo 3D por el ítem de registro limpio `ModItems.MODULAR_GOGGLES.get().getDefaultInstance()`.

---

## 3. Plan de Implementación
1. Reescribir `TacticalRadialScreen.java` con el nuevo sistema de stacking cartesiano y `drawTacticalSocket`.
2. Compilar con subagente (`forge` y `fabric`).
3. Desplegar JARs en ATLauncher.
4. Actualizar `docs/CHANGELOG.md`.
5. Guardar commit y push a GitHub.
