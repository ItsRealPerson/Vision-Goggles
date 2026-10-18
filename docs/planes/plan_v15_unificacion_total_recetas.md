# Plan de Desarrollo v15 — Unificación Total de Recetas TEMS (Todas las Gafas y Módulos)

## 1. Contexto y Diagnóstico
En la fase previa (TEMS v14) se introdujeron los 6 nuevos componentes tácticos (`optical_lens`, `tactical_frame`, `flashlight_body`, `tactical_processor`, `thermal_core`, `lifesign_sensor`) y se rediseñaron las recetas de la linterna táctica, las gafas modulares, el módulo térmico y el biométrico.
Sin embargo, quedaban recetas heredadas que aún utilizaban la receta genérica de la versión Alpha (`night_vision_goggles`, `hydro_goggles`, `nvg_module`, `hydro_module`, `environment_module`, `vital_info_module`, `solar_module`, `spawn_security_module`, `battery_expansion_module`).

El usuario observó acertadamente:
> *"Pero una duda se cambiaron todos los crafteos? porque si metimos cosas para las gafas deberia de estar aplicado a todas"*

## 2. Principio de Coherencia Universal
Cada categoría de equipamiento ahora se rige por una regla física universal:
1. **Todas las Gafas Monolíticas (`night_vision_goggles`, `hydro_goggles`, `thermal_goggles`, `biometric_goggles`)**:
   - Requieren el **Armazón Táctico Balístico** (`tactical_frame`) en la parte superior.
   - Requieren su **Módulo Sensorial Específico** en el centro.
   - Requieren la **Batería Recargable** (`nvg_battery`) en la base.
   - Patrón: `[   F   ] / [   M   ] / [   B   ]`.
2. **Todos los Módulos Ópticos (`nvg_module`, `hydro_module`, `thermal_module`, `bio_module`, `zoom_module`, `flashlight_module`, `spawn_security_module`)**:
   - Requieren la **Lente Óptica de Precisión** (`optical_lens`) en la parte superior como proyector/sensor frontal.
   - Requieren su núcleo sensorial en el centro flanqueado por conductores (cobre, oro, amatista).
   - Requieren **Componentes Eléctricos** (`electrical_parts`) en la base.
   - Patrón: `[   L   ] / [ C P C ] / [   E   ]`.
3. **Módulos de Telemetría y Soporte (`environment_module`, `vital_info_module`, `solar_module`, `battery_expansion_module`, `programmable_module`)**:
   - Ensamblados con lógica de microcircuitos físicos:
     - `environment_module`: Cobre + Brújula geomagnética con resonadores de amatista + Electrónica.
     - `vital_info_module`: Amatista + Sonda bio-espectral de melón brillante y cobre + Electrónica.
     - `solar_module`: Sensor solar + Celda de cuarzo y oro + Electrónica.
     - `battery_expansion_module`: 2 baterías estándar + placas de cobre disipadoras + puente de oro y electrónica.
     - `programmable_module`: Bus de redstone + núcleo EEPROM de cuarzo + contactos de oro.

## 3. Estado de Ejecución
- [x] Actualizados todos los 10 archivos de receta JSON en `common/src/main/resources/data/vision_goggles/recipes/`.
- [x] Verificada la coherencia del 100% de recetas del mod mediante script de validación.
- [ ] Compilación y verificación multi-cargador con subagente (`:common`, `:forge`, `:fabric`).
- [ ] Despliegue de JARs de producción a instancias de ATLauncher.
- [ ] Registro cronológico en `docs/CHANGELOG.md` y push inmediato a GitHub.
