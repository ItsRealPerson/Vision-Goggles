# Plan v13: Restauración y Sincronización de Audio Táctico de la Linterna en Gafas Modulares

## 1. Contexto y Diagnóstico
Al alternar la linterna táctica desde el ítem de mano (`TacticalFlashlightItem`), el jugador escucha claramente el sonido de retroalimentación mecánica/electrónica (`ModSounds.BLIP`).
Sin embargo, al utilizar la linterna integrada en las gafas modulares (vía tecla rápida 'L', selector de haz 'K' o a través del Tactical Radial Menu 'N'), no se reproducía sonido alguno para el usuario o resultaba inaudible/ausente.

### Causas Raíz Identificadas:
1. **`CycleFlashlightModePacket.java` (Omisión de Audio en Gafas)**:
   - Al ciclar el haz de la linterna con gafas modulares puestas, se modificaba el tag NBT y se enviaba `SyncFlashlightPacket`, pero **no se invocaba ninguna llamada de audio ni mensaje en Action Bar**, a diferencia de la rama de linterna en mano que sí ejecutaba `playSound` y mostraba el modo en pantalla.
2. **`VisionRenderer.java` (Ausencia de Audio Inmediato en Cliente)**:
   - Los bucles de captura de teclado `ModKeyMappings.toggleFlashlightKey` ('L') y `cycleFlashlightModeKey` ('K') solo enviaban el paquete C2S al servidor sin reproducir retroalimentación sonora inmediata de interfaz (`SimpleSoundInstance.forUI`), generando una sensación de retardo o desconexión acústica.
3. **`ToggleFlashlightPacket.java` (Optimización de Capa Sonora)**:
   - El volumen era bajo (0.5f) y faltaba el clic mecánico táctico de interruptor (`SoundEvents.UI_BUTTON_CLICK`), además de verificar el estado de batería para emitir sonido de agotamiento si la energía es 0.
4. **`SyncFlashlightPacket.java` (Sincronización Sonora para Terceros)**:
   - Los jugadores remotos no recibían sonido posicional cuando otro jugador encendía o apagaba su linterna de casco en multijugador.

---

## 2. Acciones Técnicas
1. **`CycleFlashlightModePacket.java`**:
   - Agregar en la rama de gafas modulares el sonido `ModSounds.BLIP` modulado por el ID del modo (`1.2f + next.id * 0.15f`) y la notificación de Action Bar con el nombre del modo traducido.
   - Sincronizar también la notificación de Action Bar en la rama de linterna en mano.
2. **`ToggleFlashlightPacket.java`**:
   - Agregar comprobación de batería con alerta sonora y mensaje si está agotada.
   - Emitir capa doble de sonido en el servidor (`ModSounds.BLIP` + `SoundEvents.UI_BUTTON_CLICK`) con tono dinámico (encendido: 1.2f / apagado: 0.8f).
3. **`VisionRenderer.java`**:
   - En el consumo de pulsación de `toggleFlashlightKey` y `cycleFlashlightModeKey`, ejecutar instantáneamente audio local de interfaz (`SimpleSoundInstance.forUI`) para respuesta táctil a latencia cero.
4. **`TacticalRadialScreen.java`**:
   - Añadir clic mecánico al accionar la opción radial de linterna.
5. **`SyncFlashlightPacket.java`**:
   - Reproducir sonido posicional para jugadores remotos al sincronizar el cambio de estado de la linterna de casco.

---

## 3. Verificación y Despliegue
- Compilación de `common`, `forge` y `fabric` mediante subagente `self`.
- Generación y copia de JARs a las instancias de ATLauncher de Forge y Fabric.
- Actualización de `docs/CHANGELOG.md`.
- Commit y push a GitHub (`refactor/modern-shaders-pipeline`).
