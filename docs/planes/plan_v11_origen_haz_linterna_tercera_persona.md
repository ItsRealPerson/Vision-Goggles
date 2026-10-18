# Plan v11: Origen Anatómico Diferenciado del Haz de Luz de Linterna (Torso, Mano y Cabeza) en 1ra y 3ra Persona

## 1. Contexto y Solicitud del Usuario
El usuario solicitó:
> *"Otra cosa podemos ver que el haz de luz de la linterna si la tengo puesto en el slot de torso, salga la luz en 3ra persona desde el torso y no la cara? y si lo tengo en la mano, salga desde la mano y no la cara"*

### Diagnóstico Técnico:
1. **Origen Fijo en los Ojos (`player.getEyePosition()`)**:
   - En `VisionRenderer.java` (línea 360), la posición del emisor de luz en la escena 3D se calcula invariablemente como `player.getEyePosition(partialTick) - camera.getPosition()`.
   - Tanto si la linterna está equipada en el chaleco/torso (Curios/Trinkets/Pechera), sostenida en la mano principal o secundaria, o montada en el casco, el haz siempre se emite desde los ojos del jugador.
2. **Dirección Fija en la Cabeza (`player.getViewVector()`)**:
   - La orientación del haz siempre sigue el pitch y yaw de la cabeza, incluso cuando la linterna está atornillada al pecho (MOLLE de chaleco) y debería seguir la orientación del torso (`yBodyRot`).
3. **Incoherencia del Shader 2D en Tercera Persona**:
   - `flashlight.fsh` es un shader de post-procesamiento 2D centrado estáticamente en la pantalla `(0.5, 0.5)`. En 3ra persona (F5), continúa dibujando un halo en el centro de la pantalla como si la cámara tuviera una linterna pegada a la lente, superponiéndose sobre el jugador en lugar de dejar que el mundo 3D se ilumine desde su cuerpo.

---

## 2. Arquitectura Matemática y Cinemática por Ranura de Equipamiento

### A. Ranuras Anatómicas (`FlashlightSlot`)
- `HEAD_GOGGLES`: Módulo de linterna instalado en las gafas modulares.
- `TORSO_CHEST`: Linterna táctica equipada en ranura de cuerpo/pecho (Curios/Trinkets o pechera).
- `MAIN_HAND`: Linterna táctica activa sostenida en la mano principal.
- `OFF_HAND`: Linterna táctica activa sostenida en la mano secundaria.

### B. Cinemática en Tercera Persona (F5 y Jugadores Remotos)
1. **Torso / Pecho (`TORSO_CHEST`)**:
   - Sincronizado exactamente con el modelo de renderizado en `GogglesRenderLayer.java`.
   - **Pivote del Torso**: `py + (player.isCrouching() ? 1.05 : 1.25)`.
   - **Rotación**: Orientada por `player.yBodyRot` (yaw del cuerpo) y el cabeceo de agachado (~28.6° si `isCrouching()`).
   - **Offset Local en el Chaleco**: $+0.18\text{ m}$ (derecha), $-0.20\text{ m}$ (abajo), $+0.16\text{ m}$ (adelante sobre la placa del chaleco).
   - **Dirección**: Sigue la normal del torso, apuntando al frente de donde mira el cuerpo.
2. **Mano Principal / Secundaria (`MAIN_HAND` / `OFF_HAND`)**:
   - **Pivote del Hombro**: `py + 1.25\text{ m}`, desplazado lateralmente $\pm 0.35\text{ m}$ según `yBodyRot`.
   - **Extensión del Brazo**: Desplazado hacia adelante en la dirección de la mirada del jugador con una longitud de brazo de $\approx 0.40\text{ m}$ y $-0.25\text{ m}$ de altura.
   - **Dirección**: Sigue la dirección de apuntado de la mano hacia el objetivo.
3. **Cabeza (`HEAD_GOGGLES`)**:
   - Mantiene el origen en la cabeza/ojos (`player.getEyePosition()`) y la dirección de la cabeza (`headYaw`, `headPitch`).

### C. Cinemática en Primera Persona
1. **Cabeza**: Coordenadas de cámara del jugador (`camPos` + sutil offset hacia la frente).
2. **Torso**: Offset de cámara hacia el pecho (`-camUp * 0.35 + camRight * 0.18 + camDir * 0.15`).
3. **Mano Principal / Secundaria**: Offset hacia la mano visible en pantalla (`-camUp * 0.25 ± camRight * 0.32 + camDir * 0.35`), convergiendo hacia el retículo a media-larga distancia.

### D. Supresión del Shader 2D de Pantalla en Tercera Persona
En `VisionShaderManager.processFlashlight()`, verificar si la cámara es de primera persona:
```java
if (!mc.options.getCameraType().isFirstPerson()) {
    return;
}
```
Esto permite que en 3ra persona la escena sea puramente iluminada en 3D desde el cuerpo del jugador (mediante Sodium y Vanilla shader patchers), sin que un sticker 2D en pantalla interfiera con la cámara.

### E. Soporte Multifuente Concurrente
Iterar sobre todas las fuentes de linterna equipadas y activas (gafas, chaleco y mano) y subirlas a `FlashlightUniforms` (soporta hasta 16 fuentes simultáneas).

---

## 3. Plan de Acción
1. Crear clase o enum auxiliar `FlashlightSlot` en `dev.itsrealperson.vision_goggles.util`.
2. Implementar los cálculos cinemáticos de origen y dirección en `VisionRenderer.java`.
3. Actualizar `VisionShaderManager.java` para suprimir el pase 2D en tercera persona.
4. Compilar con subagente de build (`forge` y `fabric`).
5. Desplegar a instancias ATLauncher.
6. Actualizar `docs/CHANGELOG.md`.
7. Realizar commit y push a GitHub.
