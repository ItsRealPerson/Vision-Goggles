# Plan v12: Sincronización y Traducción Integral de Idiomas (i18n)

## 1. Contexto y Objetivos
El usuario ha solicitado formalmente:
> *"Otra cosa que no se te olvide las traducciones a los idiomas que soporta el mod"*

A lo largo de las fases de desarrollo de la versión `2.0.0-ALPHA` de Vision Goggles (menú radial táctico, soporte anatómico de linterna en tercera persona, recarga rápida en barra de acción, bloqueo de zoom y toggles dinámicos de sonar y seguridad de spawn), varias cadenas de texto en interfaces y mensajes se introdujeron temporalmente con textos literales (`Component.literal`) o sin registrar en los archivos de idioma.

Este plan tiene como objetivo:
1. Migrar el 100% de las cadenas visuales literales al sistema de traducciones nativo de Minecraft (`Component.translatable`).
2. Actualizar y sincronizar los **12 idiomas oficiales soportados** por Vision Goggles:
   - `en_us.json` (Inglés Americano - Base)
   - `en_gb.json` (Inglés Británico)
   - `es_es.json` (Español - España)
   - `es_mx.json` (Español - México)
   - `es_ar.json` (Español - Argentina)
   - `es_cl.json` (Español - Chile)
   - `de_de.json` (Alemán)
   - `fr_fr.json` (Francés)
   - `it_it.json` (Italiano)
   - `pl_pl.json` (Polaco)
   - `pt_br.json` (Portugués Brasileño)
   - `ru_ru.json` (Ruso)
3. Incorporar traducciones precisas y tácticas para cada una de las 12 regiones lingüísticas.
4. Validar compilación limpia multiplataforma (Forge y Fabric) y desplegar en instancias de ATLauncher.
5. Hacer commit y push inmediato a GitHub.

---

## 2. Inventario de Cadenas y Claves i18n

### A. Menú Radial Táctico (`TacticalRadialScreen.java`)
- `gui.vision_goggles.radial.title` -> Título de pantalla ("Menú Radial Táctico" / "Tactical Radial Menu")
- `gui.vision_goggles.radial.header` -> Encabezado HUD superior ("§7[DIAL TÁCTICO VG // §6BUILD 2.0.0-ALPHA§7]")
- `gui.vision_goggles.radial.ready` -> Estado de reposo del dial ("SISTEMA LISTO" / "SYSTEM READY")
- `gui.vision_goggles.radial.nvg` / `.desc` -> NVG ("NVG" / "Visión Nocturna" / "Night Vision")
- `gui.vision_goggles.radial.thermal` / `.desc` -> THERMAL ("TÉRMICO" / "Visor Térmico" / "Thermal Vision")
- `gui.vision_goggles.radial.bio` / `.desc` -> BIOMÉTRICO ("BIOMÉTRICO" / "Radar Vital" / "Vital Radar")
- `gui.vision_goggles.radial.hydro` / `.desc` -> HYDRO ("HIDRO" / "Visión Acuática" / "Aquatic Vision")
- `gui.vision_goggles.radial.standby` / `.desc` -> STANDBY ("STANDBY" / "Apagar / Levantar" / "Turn Off / Flip Up")
- `gui.vision_goggles.radial.security` / `.on` / `.off` -> SEGURIDAD ("SEGURIDAD" / "Radar Spawn ON" / "Radar Spawn OFF")
- `gui.vision_goggles.radial.sonar` / `.all` / `.players` / `.undead` -> SONAR ("SONAR" / "Filtro: Todos" / "Filtro: Jugadores" / "Filtro: No-Muertos")
- `gui.vision_goggles.radial.zoom` / `.locked` / `.desc` -> ZOOM ("ZOOM" / "Fijado ON" / "Aumento 2.5x")
- `gui.vision_goggles.radial.flashlight` / `.desc` -> LINTERNA ("LINTERNA" / "Alternar Haz" / "Toggle Beam")
- `gui.vision_goggles.radial.battery` / `.desc` -> BATERÍA ("BATERÍA" / "Recarga Rápida" / "Fast Recharge")
- `gui.vision_goggles.radial.no_gear` / `.desc` -> SIN EQUIPO ("SIN EQUIPO" / "Sin módulos disponibles" / "No modules available")

### B. Mensajes en Barra de Acción
- `message.vision_goggles.target_full` -> "§e[i] ¡%s ya está al 100%!" / "§e[i] %s is already at 100%!"
- `message.vision_goggles.no_rechargeable` -> "§c[!] No tienes equipo recargable equipado" / "§c[!] You do not have rechargeable gear equipped"
- `message.vision_goggles.no_batteries` -> "§c[!] No tienes baterías en el inventario" / "§c[!] You do not have batteries in inventory"
- `message.vision_goggles.recharged_named` -> "§a[+] Batería Recargada: %s (%d%%)" / "§a[+] Battery Recharged: %s (%d%%)"
- `message.vision_goggles.zoom_locked` -> "§b[Zoom]: §aFijado (2.5x)" / "§b[Zoom]: §aLocked (2.5x)"
- `message.vision_goggles.zoom_unlocked` -> "§b[Zoom]: §cDesactivado" / "§b[Zoom]: §cDisabled"
- `message.vision_goggles.spawn_security_on` -> "§6[Radar de Spawn]: §aActivado" / "§6[Spawn Radar]: §aEnabled"
- `message.vision_goggles.spawn_security_off` -> "§6[Radar de Spawn]: §cDesactivado" / "§6[Spawn Radar]: §cDisabled"
- `message.vision_goggles.sonar_filter.all` -> "Todos" / "All"
- `message.vision_goggles.sonar_filter.players` -> "Jugadores" / "Players"
- `message.vision_goggles.sonar_filter.undead` -> "No-Muertos" / "Undead"
- `message.vision_goggles.low_battery` -> "§c[!] Batería Agotada" / "§c[!] Battery Depleted"
- `message.vision_goggles.flashlight_mode` -> "§eLinterna: §f%s" / "§eFlashlight: §f%s"

### C. Tooltips y HUD Crítico
- `tooltip.vision_goggles.state` -> "Estado: %s" / "State: %s"
- `tooltip.vision_goggles.active` -> "Activo" / "Active"
- `tooltip.vision_goggles.inactive` -> "Inactivo" / "Inactive"
- `tooltip.vision_goggles.flashlight.mode` -> "Modo: %s" / "Mode: %s"
- `tooltip.vision_goggles.flashlight.battery` -> "Batería: %s%%" / "Battery: %s%%"
- `tooltip.vision_goggles.flashlight.controls_toggle` -> "Click derecho: Encender / Apagar" / "Right Click: Turn On / Off"
- `tooltip.vision_goggles.flashlight.controls_cycle` -> "Shift + Click derecho: Cambiar haz" / "Shift + Right Click: Change beam"
- `tooltip.vision_goggles.script_firmware` -> "§6Firmware: §f%s" / "§6Firmware: §f%s"
- `tooltip.vision_goggles.script_id` -> "§7ID Script: §a%s" / "§7Script ID: §a%s"
- `tooltip.vision_goggles.script_combine` -> "Combina con un Módulo Programable para grabarlo." / "Combine with a Programmable Module to burn it."
- `tooltip.vision_goggles.script_give_help` -> "Usa /vg givescript <id> para configurar." / "Use /vg givescript <id> to configure."
- `tooltip.vision_goggles.prog_programmed` -> "§bMódulo Programado: §a%s" / "§bProgrammed Module: §a%s"
- `tooltip.vision_goggles.prog_firmware` -> "§7Firmware: §e%s" / "§7Firmware: §e%s"
- `tooltip.vision_goggles.prog_install` -> "Instálalo en tus gafas en la Estación de Modificación." / "Install into your goggles at the Modification Station."
- `tooltip.vision_goggles.prog_blank` -> "Hardware virgen en blanco." / "Blank unprogrammed hardware."
- `tooltip.vision_goggles.prog_burn_hint` -> "Combina con un Script de Instalación para programarlo." / "Combine with an Installation Script to program it."
- `item.vision_goggles.programmable_module.blank` -> "Módulo Programable (Virgen)" / "Programmable Module (Blank)"
- `item.vision_goggles.installation_script.unconfigured` -> "Script sin configurar" / "Unconfigured Script"
- `hud.vision_goggles.damage_critical` -> "!!! GOGGLES DAMAGE CRITICAL !!!" / "¡¡¡ DAÑO CRÍTICO DE GAFAS !!!"

---

## 3. Fases de Ejecución

1. **Refactorización de Código Java a `Component.translatable`**:
   - `TacticalRadialScreen.java`
   - `BatteryPacket.java`
   - `VisionRenderer.java`
   - `VisionWorldRenderer.java`
   - `TacticalFlashlightItem.java`
   - `ToggleSonarPacket.java`
   - `InstallationScriptItem.java`
   - `ProgrammableModuleItem.java`
   - `ModularGogglesItem.java`
   - `VitalInfoModule.java`
2. **Generación Automatizada de Diccionarios y Actualización de los 12 Archivos JSON**:
   - Script Python para inyectar todas las nuevas claves en los 12 idiomas sin romper la estructura ni formateo existente.
3. **Compilación y Empaquetado**:
   - Invocación de subagente para compilar y construir los JARs de Forge y Fabric.
4. **Despliegue a ATLauncher**:
   - Copia de JARs a instancias de Forge y Fabric.
5. **Documentación y Versionado**:
   - Actualización acumulativa de `docs/CHANGELOG.md`.
   - Commit y `git push` inmediato a GitHub en `refactor/modern-shaders-pipeline`.
