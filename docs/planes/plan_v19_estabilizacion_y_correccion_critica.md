# Plan v19: Estabilización y Corrección de Errores Críticos

## 1. Diagnóstico de Problemas Reportados

### A. Crash de Gafas Térmicas (`Pose stack not empty`)
- **Síntoma:** Al equipar o activar las gafas de visión térmica, el juego crashea con:
  `java.lang.IllegalStateException: Pose stack not empty` en `LevelRenderer.m_109588_ (LevelRenderer.java:1431)`.
- **Causa Raíz:** En `ThermalEntityPainter.captureAndComposite(PoseStack poseStack)`, se reutilizaba directamente la instancia del `PoseStack` de `LevelRenderer` provista por `RenderLevelStageEvent.Stage.AFTER_BLOCK_ENTITIES`. Al invocar `dispatcher.render(...)`, cualquier entidad o capa que generara una excepción dentro de un bloque `try-catch` interrumpía el ciclo antes de su respectivo `poseStack.popPose()`, desbalanceando la pila de matrices del renderizador principal de Minecraft.
- **Solución:**
  1. Aislar completamente la pila de matrices instanciando un `PoseStack entityPoseStack = new PoseStack();` local.
  2. Clonar únicamente las matrices de orientación y proyección actuales (`poseStack.last().pose()` y `poseStack.last().normal()`) sin mutar jamás el `poseStack` original del mundo.
  3. Envolver cada renderizado de entidad en bloques `try { entityPoseStack.pushPose(); ... } finally { entityPoseStack.popPose(); }` para garantizar neutralidad matricial absoluta.

---

### B. Modo Estroboscópico de la Linterna Inaccesible
- **Síntoma:** Ni la tecla de ciclo de linterna ('K') ni el Shift + Clic Derecho en el ítem `tactical_flashlight` conmutan al modo `STROBE`.
- **Causa Raíz:** El ciclo de modos estaba codificado de forma estática con módulo 3: `(currentMode + 1) % 3` tanto en `TacticalFlashlightItem.java` como en `CycleFlashlightModePacket.java`. Sin embargo, `FlashlightMode` cuenta con 4 modos (`FOCUSED=0`, `WIDE=1`, `SIGNAL=2`, `STROBE=3`). El modo 3 era inalcanzable.
- **Solución:** Reemplazar el cálculo estático con `FlashlightMode.byId(currentMode).next().id` en todas las clases de ciclo.

---

### C. Deslumbramiento NVG con Linterna Apagada de Creativo
- **Síntoma:** Al sacar una `tactical_flashlight` desde el inventario creativo y tener gafas NVG activas, se genera un fogonazo de destello blanco en la pantalla aunque la linterna esté físicamente apagada.
- **Causa Raíz:** En `VisionShaderManager.isHoldingLight(ItemStack stack)`, se detectaba la presencia del ítem mediante chequeo textual de `flashlight`, pero para instancias de `TacticalFlashlightItem` no se comprobaba `TacticalFlashlightItem.isActive(stack)`. Al salir de creativo sin NBT o con NBT default, se trataba como una fuente de luz activa.
- **Solución:** En `isHoldingLight`, delegar explícitamente en `TacticalFlashlightItem.isActive(stack)` cuando el ítem sea una linterna táctica. Si está inactiva, retorna `false`.

---

### D. Estación de Modificación: Desmontaje Bloquea Instalación
- **Síntoma:** Al colocar unas gafas con módulos en la Ranura 0 y luego colocar un ítem en la Ranura 1 (por ejemplo, para instalar un módulo o reparar), la Ranura 2 permanece congelada mostrando el módulo de desmontaje previo.
- **Causa Raíz:** En `ModificationStationBlockEntity.updateResult()`, cuando la Ranura 1 estaba vacía se previsualizaba el módulo extraíble en la Ranura 2. Al colocar luego un ítem en la Ranura 1, la línea `if (!outputStack.isEmpty()) return;` abortaba la ejecución inmediatamente sin limpiar ni recalcular la receta de instalación/reparación.
- **Solución:**
  1. Si la Ranura 1 está vacía, previsualizar la extracción del módulo si existen módulos en las gafas; de lo contrario limpiar Ranura 2.
  2. Si la Ranura 1 NO está vacía, inicializar la salida por defecto en `ItemStack.EMPTY` (eliminando la guarda `if (!outputStack.isEmpty()) return;`).
  3. Evaluar de forma limpia las recetas de reparación, grabado de scripts e instalación modular. Si ninguna coincide, la salida queda vacía.

---

## 2. Plan de Implementación
1. Modificar `ThermalEntityPainter.java` con `entityPoseStack` aislado y `try-finally`.
2. Modificar `TacticalFlashlightItem.java` y `CycleFlashlightModePacket.java` para usar `mode.next()`.
3. Modificar `VisionShaderManager.java` para verificar `TacticalFlashlightItem.isActive(stack)`.
4. Modificar `ModificationStationBlockEntity.java` para corregir `updateResult()`.
5. Orquestar compilación con subagente y desplegar JARs a ATLauncher.
6. Actualizar `CHANGELOG.md`, commitear y enviar a GitHub.
