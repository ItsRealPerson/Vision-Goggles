# Plan v5: Corrección de Menú Radial Táctico, Recarga de Batería, Filtro de Linterna y Pose Flip-Up

## 1. Objetivos del Plan
1. **Pose Flip-Up Táctica Dinámica**:
   - Restaurar el movimiento de las gafas cuando están apagadas (inactivas).
   - Ajustar el ángulo de rotación a un ángulo táctico natural (`xRot = -0.90F` ~ -52°, `y = -3.8F`, `z = -5.8F`) para que descansen sobre la frente/casco en la montura Wilcox sin deformaciones.
   - Lentes apagadas cuando están levantadas, y emisivas cuando están bajadas y encendidas.

2. **Menú Radial Dinámico y Validación de Modos**:
   - Corregir el registro de `THERMAL_GOGGLES` en `ModItems.java` para que NO incluya `NIGHT_VISION` (solo `THERMAL`).
   - Modificar `TacticalRadialScreen.java` para que la rueda solo contenga los modos que el jugador realmente tiene instalados / disponibles:
     - Gafas fijas: solo su modo nativo (`NVG`, `THERMAL`, `BIO` o `HYDRO`).
     - Gafas modulares: consultar `getModes(helmet)` y agregar exclusivamente los modos instalados. Si no hay módulos de visión, no se muestran modos de visión.
     - Linterna: solo se agrega al menú si el jugador tiene una linterna equipada, sostenida o el módulo instalado en las gafas.
     - Batería: solo se agrega si el jugador tiene equipo recargable y/o baterías.
     - Standby: apagar las gafas (solo si están equipadas).
   - Distribuir las porciones de la rueda dinámicamente entre las opciones activas.

3. **Recarga Rápida de Batería ('B' y Menú Radial)**:
   - Corregir `BatteryPacket.findBattery(player)` para buscar en todo el inventario del jugador (`items` y `offhand`), no únicamente en las manos.
   - Añadir retroalimentación sonora y visual en la barra de acción (`ActionBar`):
     - `§a[+] Batería Recargada: Gafas / Linterna (XX%)`
     - `§c[!] No tienes baterías en el inventario`
     - `§e[i] Gafas / Linterna ya tienen la carga completa`

4. **Filtro de Linterna Táctica**:
   - En `VisionRenderer.java`, verificar `hasFlashlightAvailable` antes de procesar la tecla `Y` o `Shift+Y`. Si el jugador no posee ninguna linterna ni módulo de linterna, no enviar paquetes ni reproducir ningún sonido.
   - En `ToggleFlashlightPacket` y `CycleFlashlightModePacket`, asegurar soporte completo para linterna sostenida en mano además de la montada en el chaleco/curios.

## 2. Archivos Afectados
- `common/src/main/java/dev/itsrealperson/vision_goggles/registry/ModItems.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/client/gui/TacticalRadialScreen.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/client/VisionRenderer.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/network/BatteryPacket.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/network/ToggleFlashlightPacket.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/network/CycleFlashlightModePacket.java`
- `common/src/main/java/dev/itsrealperson/vision_goggles/client/GogglesRenderLayer.java`
- `docs/CHANGELOG.md`
