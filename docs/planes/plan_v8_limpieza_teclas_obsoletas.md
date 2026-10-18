# Plan v8: Limpieza de Teclas Físicas Obsoletas (M, O, P) y Consolidación Radial

## 1. Contexto y Requerimiento del Usuario
El usuario solicitó:
> *"a ver considero de el ciclar el haz de la linterna deberia de quedarse pero elde la M, O y P si quitalos"*

### Configuración final de controles de teclado:
- **Conservar**:
  - **`N`** (`toggleGrayscaleKey`): Toque rápido = Encender/Apagar visor; Mantener presionado = Menú Radial Táctico.
  - **`V`** (`zoomKey`): Mantener presionado + rueda del ratón = Zoom óptico continuo (1.0x a 8.0x).
  - **`L`** (`toggleFlashlightKey`): Alternar linterna táctica ON/OFF rápidamente.
  - **`K`** (`cycleFlashlightModeKey`): Ciclar apertura del haz de linterna (FOCUSED -> WIDE -> CLOSE).
- **Eliminar por completo**:
  - **`M`** (`switchModeKey`): Reemplazado 100% por el Menú Radial Táctico.
  - **`O`** (`toggleSpawnSecurityKey`): Reemplazado 100% por la opción `SEGURIDAD` en el Menú Radial Táctico.
  - **`P`** (`switchSonarModeKey`): Reemplazado 100% por la opción `SONAR` en el Menú Radial Táctico.

---

## 2. Modificaciones de Código
1. **`ModKeyMappings.java`**:
   - Eliminar las declaraciones y registros de `switchModeKey`, `toggleSpawnSecurityKey` y `switchSonarModeKey`.
2. **`VisionRenderer.java`**:
   - Eliminar el consumo de clics de `ModKeyMappings.switchModeKey` y `ModKeyMappings.switchSonarModeKey`.
3. **`VisionWorldRenderer.java`**:
   - Eliminar el consumo de clics de `ModKeyMappings.toggleSpawnSecurityKey`.
4. **Archivos de Traducción (`lang/*.json`)**:
   - Eliminar las claves `key.vision_goggles.switch`, `key.vision_goggles.toggle_spawn_security` y `key.vision_goggles.switch_sonar` en los 12 idiomas soportados.

---

## 3. Verificación y Despliegue
1. Compilar Forge y Fabric mediante subagente (`compileJava`, `build -x test`).
2. Desplegar los artefactos JAR en las instancias de ATLauncher.
3. Registrar en `docs/CHANGELOG.md`.
4. Guardar cambios y subir a GitHub (`git commit` + `git push`).
