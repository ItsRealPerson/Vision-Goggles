# Plan v4: Aplicación y Validación de Prototipos de Texturas Argon en Vision Goggles

## 1. Contexto y Objetivo
El usuario ha solicitado aplicar de inmediato los 16 prototipos de texturas diseñados con Gemini 4 Argon (`gemini-4-argon-high`) para probarlos dentro del juego.
Los prototipos se encuentran actualmente en `docs/prototipos_texturas/`.
Se deben mantener intactos:
- `installation_script`
- `modification_station`

## 2. Alcance de Texturas
1. Mapeo de Texturas a aplicar en `common/src/main/resources/assets/vision_goggles/textures/item/`:
   - `nvg_module_v2.png` / `nvg_module.png`
   - `thermal_module_v2.png` / `thermal_module.png`
   - `bio_module_v2.png` / `bio_module.png`
   - `hydro_module_v2.png` / `hydro_module.png`
   - `zoom_module_v2.png` / `zoom_module.png`
   - `vital_info_module_v2.png` / `vital_info_module.png`
   - `battery_expansion_module_v2.png` / `battery_expansion_module.png`
   - `sonar_module_v2.png` / `sonar_module.png`
   - `solar_module_v2.png` / `solar_module.png`
   - `environment_module_v2.png` / `environment_module.png`
   - `spawn_security_module_v2.png` / `spawn_security_module.png`
   - `flashlight_module_v2.png` / `flashlight_module.png`
   - `programmable_module_v2.png` / `programmable_module.png`
   - `tactical_flashlight.png`
   - `batteries_v2.png` / `batteries.png` / `nvg_battery.png`
   - `electrical_parts.png`

2. Modelos JSON:
   - Verificar si los modelos en `models/item/` apuntan a `*_v2` o `*` sin sufijo.
   - Para máxima compatibilidad, actualizar tanto la versión `_v2.png` como la versión base si ambas existen.

3. Construcción y Empaquetado:
   - Compilar con `./gradlew.bat :common:compileJava :forge:compileJava :fabric:compileJava :forge:build :fabric:build -x test`.
   - Copiar los JARs resultantes a las instancias locales de ATLauncher (`Minecraft1201withForge` y `Minecraft1201withFabric`).

4. Control de Versiones:
   - `git add -A`
   - `git commit`
   - `git push origin refactor/modern-shaders-pipeline`
   - Registro en `docs/CHANGELOG.md`.
