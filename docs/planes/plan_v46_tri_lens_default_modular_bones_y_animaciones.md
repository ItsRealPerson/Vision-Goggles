# Plan v46: Promoción Tri-Lente por Defecto, Huesos Modulares Dinámicos y Corrección de Animaciones (`equip`, `flip` y `F5`)

**Fecha:** 2026-11-21  
**Versión objetivo:** `2.0.0` (Architectury `1.20.1` — Forge `47.4.16` + Curios `5.4.3` y Fabric `0.18.4` + Accessories `1.0.0-beta.47`)

## 1. Diagnóstico y Objetivo
- Los modelos antiguos (`128×128`, `VisionGoggleModel`, `FirstPersonVisorModel` y `geo/item/*_goggles.geo.json` previos) coexistían con los prototipos `64×64` Tri-Lente (`proto_tri_lens_*`) y el comando temporal `/vg preview`, causando inconsistencias visuales entre cabeza (`F5`), primera persona (`flip` y `equip`), mano/GUI y Mesa de Modificación.
- En primera persona (`flip`), `FirstPersonVisorModel` presentaba recorte sobredimensionado y orientación invertida hacia cámara, mientras `MixinItemInHandRenderer` disparaba doble sincronización de `GogglesEquipAnimator` por cuadro y desalineación de brazos en `equip`.
- Las Gafas Modulares y Pro Modulares requerían diferenciación visual clara (acabado obsidiana-titanio con biselado CNC dorado para Pro Modular) y un modelo maestro con piezas 3D dinámicas acoplables por hueso según el NBT del `ItemStack`.

## 2. Arquitectura por Hitos (`M1`–`M4`)
1. **Hito M1 (`assets_and_models`) — Activos `64×64`, Modelo Maestro Modular y Documentación:**
   - Promover los 4 prototipos estáticos `proto_tri_lens_{nvg,thermal,bio,hydro}.geo.json` a `night_vision_goggles.geo.json`, `thermal_goggles.geo.json`, `biometric_goggles.geo.json` e `hydro_goggles.geo.json` renombrando el hueso `"visor"` a `"visor_pivot"`.
   - Promover `proto_tri_lens.animation.json` a `animations/item/vision_goggles.animation.json` animando `"visor_pivot"` (`idle_down`, `idle_up`, `flip_down`, `flip_up`).
   - Expandir `scripts/generate_tri_lens_textures.py` a un atlas `64×64` de **80 zonas sin solapamientos** (`54` base + `26` zonas modulares) y máscaras emisivas estrictas (`9` rectángulos en variantes estáticas, `23` en modulares), emitiendo `tri_lens_{nvg,thermal,bio,hydro,modular,pro_modular}{,_glow}.png` y `chassis_neutral.png` (`64×64` grafito mate sin brillo ni `_glow.png`) en `textures/entities/`.
   - Construir `models_src/tri_lens_master/tri_lens_master_{modular,pro_modular}.bbmodel` (16 huesos, 118 cubos, sin `rail_module_r` en el chasis base) y exportar `modular_goggles.geo.json` y `pro_modular_goggles.geo.json`, validando mediante `bbmodel_validate`, `bbmodel_validate_animations`, `bbmodel_contact_sheet` y verificación geométrica SAT/AABB/UV.
2. **Hito M2 (`core_renderer_and_cleanup`) — Renderizador Unificado, NBT Dinámico y Retiro de `/vg preview`:**
   - Actualizar `VisionGogglesGeoModel` y `VisionGogglesGeoRenderer` para unificar las 6 gafas + `vg_neutral_chassis` sobre los activos Tri-Lente `64×64`, centrar el espacio de ítem `(0, -29/16, +2.75/16)`, calibrar `models/item/*_goggles.json`, añadir pasada emisiva `RenderType.eyes` y aplicar `GeoBone#setHidden` por cuadro leyendo el NBT.
   - Garantizar paridad en `ModificationStationRenderer` (chasis neutro y vista previa modular dinámica).
   - Eliminar `/vg preview` (`client/preview/*`, `TriLensPreviewScreen`, `OpenProtoPreviewPacket` y ganchos en comandos/red/mixins).
3. **Hito M3 (`fp_and_f5_animations`) — Tercera Persona (`F5`), `flip` 1P, `equip` 1P y Limpieza Legacy:**
   - Migrar `GogglesRenderLayer` (`F5`) desde `VisionGoggleModel` a `BakedGeoModel` Tri-Lente sobre `humanoidModel.head` con `GogglesFlipAnimator`, pasada `_glow` y huesos modulares NBT.
   - Migrar `FirstPersonVisorRenderer` (`flip` 1P) al `BakedGeoModel` Tri-Lente (`visor_pivot` + hijos visibles), corrigiendo orientación frontal `180°`, escala natural, continuidad de subida/bajada y anclaje de brazos `renderPlayerArm`.
   - Corregir la doble llamada a `GogglesEquipAnimator.syncFromRender()` por cuadro en `MixinItemInHandRenderer` y alinear `FirstPersonEquipRenderer` + `MixinHumanoidModel`.
   - Retirar clases y activos heredados `128×128` (`VisionGoggleModel`, `FirstPersonVisorModel`, variantes `*_goggles_{thermal,bio,hydro}.geo.json` antiguas y texturas `128×128`).
4. **Hito M4 (`final_e2e_and_release`) — Pruebas E2E, Hardening, CHANGELOG y GitHub:**
   - Ejecutar batería E2E (Tiers 1–4) y hardening adversarial (Tier 5), actualizar `docs/CHANGELOG.md` y `AGENTS.md` § 7, validar compilación multi-loader y subir cambios a GitHub (`git commit` + `git push`).

## 3. Contratos de Activos y Huesos NBT (`Master Modular`)

### 3.1 Rutas de Activos Promovidos (`assets/vision_goggles/`)
- **Geometrías (`geo/item/`, `64×64`):** `night_vision_goggles.geo.json`, `thermal_goggles.geo.json`, `biometric_goggles.geo.json`, `hydro_goggles.geo.json`, `modular_goggles.geo.json`, `pro_modular_goggles.geo.json`.
- **Animación (`animations/item/`):** `vision_goggles.animation.json` (hueso `"visor_pivot"`).
- **Texturas (`textures/entities/`, `64×64`):** `tri_lens_{nvg,thermal,bio,hydro,modular,pro_modular}.png` + `*_glow.png`, más `chassis_neutral.png` (sin `_glow`).

### 3.2 Jerarquía de Huesos y Visibilidad por NBT
- **Rama estática en `head` (`pivot = [0, 24, 0]` — no rota en `flip`):**
  - Base siempre visible: correas, montura Wilcox, hebilla trasera y rieles Picatinny limpios.
  - `mod_pro_armor`: visible solo si `stack.getItem() instanceof ProModularGogglesItem` y `!vg_neutral_chassis`.
  - `mod_solar`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_SOLAR)` (panel fotovoltaico superior).
  - `mod_battery_pack`: visible si `ModularGogglesItem.hasBatteryExpansion(stack)` (paquete trasero ampliado + barra de carga + conducto).
  - `mod_flashlight`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_FLASHLIGHT)` (linterna táctica en riel derecho `-X`).
  - `mod_lua_chip`: visible si `ModularGogglesItem.hasModule(stack, "programmable")` (cartucho micro-PCB + LED en riel izquierdo `+X`).
  - `mod_sonar`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_SONAR)` (módulo acústico trasero izquierdo).
  - `mod_vital_info`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_VITAL_INFO)` (telemetría bajo correa derecha).
  - `mod_environment`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_ENVIRONMENT)` (toma atmosférica bajo correa izquierda).
- **Rama articulada en `visor_pivot` (`parent = "head"`, `pivot = [0, 27.5, -5.5]` — acompaña el `flip`):**
  - Base siempre visible: puente superior/inferior, bloque central y caja de conexiones.
  - `mod_zoom`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_ZOOM)` (extensiones telescópicas abiertas que dejan visibles las lentes).
  - `mod_optics_base`: visible cuando `getModes(stack).isEmpty() || vg_neutral_chassis` (lentes oscuras apagadas; `0` píxeles emisivos).
  - `mod_optics_nvg`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_NVG)`.
  - `mod_optics_thermal`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_THERMAL)`.
  - `mod_optics_bio`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_BIO)`.
  - `mod_optics_hydro`: visible si `ModularGogglesItem.hasModule(stack, ModConstants.ID_HYDRO)`.
- **Modo `vg_neutral_chassis = true`:** Oculta todos los `mod_*` opcionales, muestra únicamente `mod_optics_base`, fuerza la textura `chassis_neutral.png` y omite la pasada emisiva.

## 4. Archivos Creados / Modificados (M1 y Proyección M2–M4)
- **M1 (Activos, scripts y plan):**
  - `scripts/generate_tri_lens_textures.py`
  - `scripts/build_tri_lens_master.py`
  - `models_src/tri_lens_master/tri_lens_master_{modular,pro_modular}.bbmodel` + `renders/*.png`
  - `common/src/main/resources/assets/vision_goggles/textures/entities/{tri_lens_*,chassis_neutral}.png`
  - `common/src/main/resources/assets/vision_goggles/geo/item/{night_vision,thermal,biometric,hydro,modular,pro_modular}_goggles.geo.json`
  - `common/src/main/resources/assets/vision_goggles/animations/item/vision_goggles.animation.json`
  - `docs/planes/plan_v46_tri_lens_default_modular_bones_y_animaciones.md`
- **M2–M3 (Código Java y JSONs de ítem, diferidos a sus hitos):** `VisionGogglesGeoModel.java`, `VisionGogglesGeoRenderer.java`, `ModificationStationRenderer.java`, `GogglesRenderLayer.java`, `FirstPersonVisorRenderer.java`, `FirstPersonEquipRenderer.java`, `MixinItemInHandRenderer.java`, `MixinHumanoidModel.java`, `models/item/*_goggles.json`, retiro de `client/preview/*` y activos `128×128`.

## 5. Plan de Validación
1. **Validación de atlas y texturas (`python scripts/generate_tri_lens_textures.py`):** 80 `ATLAS_ZONES` sin solapamiento, `0` fugas en `*_glow.png`, `mod_optics_base` con `0` píxeles emisivos y ausencia total de `chassis_neutral_glow.png`.
2. **Validación MCP Blockbench (`bbmodel_validate` + `bbmodel_validate_animations` + `bbmodel_contact_sheet`):** `0` errores de outliner/nodos/animación en los 4 clips (`idle_down`, `idle_up`, `flip_down`, `flip_up`), `0` partes flotantes y revisión visual multi-ángulo.
3. **Verificación matemática sobre `.geo.json` exportados:** UVs dentro de `[0..64]` y `ATLAS_ZONES`, `0` colisiones AABB, `0` caras coplanares con misma normal, `0` piezas flotantes y `0` colisiones OBB SAT en pose abatida hacia arriba (`rot=[-60,0,0]`, `pos=[0,2,-0.75]`).
4. **Compilación multi-loader:** `./gradlew :common:compileJava :fabric:compileJava :forge:compileJava` (`BUILD SUCCESSFUL`).
5. **Cierre final (M4):** Pruebas E2E, actualización de `docs/CHANGELOG.md` y `AGENTS.md` § 7, `git commit` y `git push`.
