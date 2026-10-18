# Plan v6: Integración Dinámica de Módulos de Utilidad en el Menú Radial Táctico

## 1. Contexto y Requerimiento del Usuario
El usuario solicitó:
> *"Y con respecto a los modulos que se siguen activando con las teclas? se pueden pasar al menu radial si lo tiene instalado? es decir si esta instalado que salga, creo que es mejor asi en vez de que salgan modulos que no tengan instalados, que solo salga los instalados/disponibles"*

### Objetivos:
1. Integrar los módulos de utilidad que tienen teclas físicas directas (`SPAWN_SECURITY`, `SONAR`, `ZOOM`) en el **Menú Radial Táctico (`N`)**.
2. Mostrar cada módulo en la rueda radial **ÚNICA Y EXCLUSIVAMENTE si está instalado** en las gafas equipadas (o si la función está disponible para el jugador).
3. Blindar las teclas físicas directas ('O' para Spawn Security, 'P' para Sonar, 'V' para Zoom) para que no hagan nada, no suenen ni envíen paquetes de red si el módulo correspondiente no está físicamente instalado.
4. Mejorar el feedback táctil/auditivo al activar/desactivar estos módulos (sonidos tácticos + mensaje en actionbar).
5. Ajustar el radio dinámico del dial si se presentan 6 o más opciones para asegurar legibilidad, separación visual y estética militar de alta tecnología.

---

## 2. Especificación Técnica de los Módulos de Utilidad

### A. Módulo de Seguridad de Spawn (`SPAWN_SECURITY`)
- **ID**: `ModConstants.ID_SPAWN_SECURITY` (`vision_goggles:spawn_security`)
- **Tecla física**: 'O' (`toggleSpawnSecurityKey`)
- **Condición**: `hasHelmet && goggles.hasModule(helmet, ModConstants.ID_SPAWN_SECURITY)`
- **Opción Radial**:
  - Título: `SEGURIDAD`
  - Subtítulo: `VisionWorldRenderer.isSpawnSecurityEnabled() ? "Radar ON" : "Radar OFF"`
  - Color: Ámbar Táctico (`0xFFFF9900`)
  - Acción: `VisionWorldRenderer.toggleSpawnSecurity(mc)`
- **Comportamiento Tecla 'O'**:
  - Si no está instalado: Ignora el click.
  - Si está instalado: Alterna el estado, reproduce sonido (`VISOR_ON`/`VISOR_OFF`) y emite mensaje en actionbar: `§6[Radar de Spawn]: §aActivado` / `§cDesactivado`.

### B. Módulo de Sonar / Pulso Acústico (`SONAR`)
- **ID**: `ModConstants.ID_SONAR` (`vision_goggles:sonar`)
- **Tecla física**: 'P' (`switchSonarModeKey`)
- **Condición**: `hasHelmet && goggles.hasModule(helmet, ModConstants.ID_SONAR)`
- **Opción Radial**:
  - Título: `SONAR`
  - Subtítulo: `Filtro: [Todos | Jugadores | No-Muertos]`
  - Color: Cian Eléctrico (`0xFF00EEEE`)
  - Acción: `NetworkManager.INSTANCE.sendToServer(new ToggleSonarPacket())`
- **Comportamiento Tecla 'P' y Packet**:
  - Si no está instalado: Ignora el click en el cliente y descarta el paquete en el servidor.
  - Al cambiar de modo: `player.inventoryMenu.broadcastChanges()` para sincronización inmediata del NBT y mensaje en actionbar.

### C. Módulo de Zoom Óptico (`ZOOM`)
- **ID**: `ModConstants.ID_ZOOM` (`vision_goggles:zoom`)
- **Tecla física**: 'V' (`zoomKey` - mantener presionado para zoom gradual)
- **Condición**: `hasHelmet && goggles.hasModule(helmet, ModConstants.ID_ZOOM)`
- **Opción Radial**:
  - Título: `ZOOM`
  - Subtítulo: `VisionRenderer.isZoomLocked() ? "Fijado ON" : "Aumento 2.5x"`
  - Color: Celeste Táctico (`0xFF55FFFF`)
  - Acción: `VisionRenderer.toggleZoomLock(mc)`
- **Comportamiento**:
  - Permite fijar el aumento táctico a 2.5x sin necesidad de mantener pulsada la tecla 'V'.
  - Mantener pulsada la tecla 'V' sigue permitiendo zoom analógico fluido.

---

## 3. Matriz de Visualización en el Menú Radial
| Tipo de Gafas / Equipamiento | Módulos Instalados | Opciones Mostradas en Radial |
| :--- | :--- | :--- |
| **Gafas Especializadas NVG** | Ninguno (solo modo nocturno nativo) | `[NVG, STANDBY, BATERÍA]` (o `+LINTERNA` si lleva linterna) |
| **Gafas Especializadas Thermal** | Ninguno (solo modo térmico nativo) | `[THERMAL, STANDBY, BATERÍA]` (o `+LINTERNA` si lleva linterna) |
| **Gafas Modulares Básicas** | Solo `NVG_MODULE` | `[NVG, STANDBY, BATERÍA]` |
| **Gafas Modulares Avanzadas** | `NVG` + `THERMAL` + `SPAWN_SECURITY` | `[NVG, THERMAL, STANDBY, SEGURIDAD, BATERÍA]` |
| **Gafas Tácticas Completas** | `NVG` + `SONAR` + `ZOOM` + `FLASHLIGHT` | `[NVG, STANDBY, SEGURIDAD?, SONAR, ZOOM, LINTERNA, BATERÍA]` |
| **Sin Gafas ni Linterna** | Ninguno | `[SIN EQUIPO]` |

---

## 4. Plan de Implementación
1. **`VisionWorldRenderer.java`**:
   - Exponer métodos públicos `isSpawnSecurityEnabled()` y `toggleSpawnSecurity(Minecraft mc)`.
   - Proteger la tecla 'O' con feedback de sonido y mensaje.
2. **`VisionRenderer.java`**:
   - Agregar control de bloqueo de zoom `isZoomLocked()` y `toggleZoomLock(Minecraft mc)`.
   - Proteger la tecla 'P' (`switchSonarModeKey`) para que solo envíe paquete si `hasSonar` es verdadero.
3. **`ToggleSonarPacket.java`**:
   - Verificar en servidor `vgItem.hasModule(helmet, ModConstants.ID_SONAR)` antes de alterar el tag NBT.
   - Forzar `player.inventoryMenu.broadcastChanges()`.
4. **`TacticalRadialScreen.java`**:
   - Añadir dinámicamente opciones para `SEGURIDAD`, `SONAR` y `ZOOM` si y solo si están instalados en las gafas.
   - Ajustar el radio del dial radial (`count > 5 ? 88.0f : 78.0f`) y ancho de botones para espaciado táctico limpio.
5. **Compilación y Empaquetado**:
   - Compilación multiplataforma con subagente (`forge` y `fabric`).
   - Copia de JARs generados al lanzador ATLauncher.
6. **Git Commit, Push y Changelog**:
   - Actualizar `docs/CHANGELOG.md`.
   - `git add`, `git commit` y `git push`.
