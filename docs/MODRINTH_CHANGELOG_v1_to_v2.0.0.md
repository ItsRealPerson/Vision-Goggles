# Vision Goggles — 1.0.0 → 2.0.0 (Full Diff Changelog)

> **Minecraft:** `1.20.1` (`Forge` & `Fabric` via Architectury)  
> **Dependencies:** Architectury API, Cloth Config, GeckoLib 4, Curios (`Forge`) / Accessories (`Fabric`)  
> **Optional Compatibility:** JEI (Just Enough Items), Sodium / Embeddium

---

## 📊 1.0.0 vs 2.0.0 At a Glance (JAR Comparison)

| Category | `1.0.0` | `2.0.0` |
|---|---:|---:|
| **Total JAR Entries** | `236` | `883` |
| **Compiled Classes** | `52` | `226` (`+176` new classes) |
| **3D `.geo.json` Models** | `1` (legacy `128×128`) | `7` (`64×64` Tri-Lens + `modification_station`) |
| **Shader Files** | `15` | `20` (`+5` flashlight & `ThermalMask` FBO shaders) |
| **Sound Assets (`.ogg`)** | `2` | `6` (`+blip`, `+sonar`, `+flashlight_switch`, `+flashlight_mode`) |
| **Item Sprites (`.png`)** | `18` | `26` (including `13` animated `.png.mcmeta` items) |
| **Crafting Recipes + Station Blueprints** | `17` table recipes | `22` table recipes + `7` Modification Station 3D blueprints |
| **Bundled Lua Scripts** | `0` | `6` tactical scripts + step-by-step guide |
| **Languages / Translation Keys** | `12` langs (`64` keys) | `16` langs (`182` keys, `+ja_jp`, `+ko_kr`, `+zh_cn`, `+zh_tw`) |

---

## English — Complete 1.0.0 → 2.0.0 Changelog

### 1. New Items, Components & Progression Overhaul
- **New Tactical Equipment & Modules**:
  - `tactical_flashlight` (**Tactical Flashlight**): Handheld or `body`-slot (Curios/Accessories) flashlight with 4 beam modes.
  - `flashlight_module` (**Flashlight Module**): Installable goggle attachment that works independently of the goggles' vision mode.
  - `programmable_module` (**Programmable Module**) & `installation_script` (**Installation Script**): Flash and run custom Lua HUD/AR/Shader scripts in-game.
- **6 New TEMS Intermediate Crafting Components** (replacing simplistic 1.0.0 recipes with realistic mechanical/electronic progression):
  - `optical_lens` (*Precision Optical Lens*)
  - `tactical_frame` (*Tactical Chassis Frame*)
  - `flashlight_body` (*Machined Flashlight Body*)
  - `tactical_processor` (*Tactical DSP Processor*)
  - `thermal_core` (*Thermal Sensor Core*)
  - `lifesign_sensor` (*Biometric Lifesign Sensor*)
- **New Vanilla Crafting Recipes Added in 2.0.0**: `environment_module`, `vital_info_module`, `flashlight_module`, `flashlight_body`, `optical_lens`, `tactical_frame`, `tactical_processor`, `thermal_core`, `lifesign_sensor`, `programmable_module`, `installation_script`.
- **Moved from Crafting Table to Modification Station (Exclusive 3D Assembly)**:
  - The 6 goggle recipes (`night_vision_goggles`, `thermal_vision_goggles`, `hydro_goggles`, `biometric_goggles`, `modular_goggles`, `pro_modular_goggles`) and `tactical_flashlight` are no longer crafted in a vanilla crafting table; they are assembled piece-by-piece in the **Modification Station** (`StationBlueprints`), including `Modular → Pro Modular` upgrades (`GogglesUpgradeRecipe`) that preserve installed NBT modules.
- **2D Pixel-Art Redesign (`Tactical Charcoal`)**: Redesigned module and component sprites (`16×16`) with **13 animated `.png.mcmeta` textures**, renamed `batteries.png` → `nvg_battery.png`, and retired unused 1.0.0 sprites (`goggles_item.png`, `wrench_item.png`).

### 2. Tri-Lens 3D Models & Dynamic Modular Bones
- **Complete 3D Model Overhaul (`64×64` Tri-Lens Family)**: Replaced 1.0.0's single legacy `128×128` model (`VisionGoggleModel` / `FirstPersonVisorModel`) with 6 dedicated Tri-Lens GeckoLib 4 models (`night_vision_goggles`, `thermal_goggles`, `biometric_goggles`, `hydro_goggles`, `modular_goggles`, and matte stealth obsidian/dark titanium-slate `pro_modular_goggles`) featuring `*_glow.png` emissive layers and coherent variant-matched interior ocular eyecups.
- **Dynamic 3D Module Visibility (Bone Toggling via NBT)**: `Modular` and `Pro Modular` goggles dynamically render or hide 3D parts on the player's head (`F5`), in 1st-person (`equip`/`flip`), in hand/GUI, and on the Modification Station:
  - **Chassis/Strap-Mounted Bones**: `mod_pro_armor` (Pro Modular reinforced plates), `mod_solar` (top solar panel), `mod_battery_pack` (rear counterweight battery pack), `mod_flashlight` (right-rail flashlight pod), `mod_lua_chip` (left-rail Lua PCB cartridge), `mod_sonar`, `mod_vital_info`, `mod_environment`.
  - **Visor-Pivot Bones (rotate with flip)**: `mod_zoom` (telescopic lens sleeves) and `mod_optics_{nvg,thermal,bio,hydro,base}`.
- **Item Frame & Display Alignment**: Calibrated `gui`, `ground`, `fixed` (front-facing in Item Frames), `firstperson_*`, and `thirdperson_*` transforms across all 6 goggle items.

### 3. First-Person & Third-Person Animations
- **4-Phase 3D Equip Animation (`Right-Click Hold`)**: Added `GogglesEquipAnimator` + `FirstPersonEquipRenderer` (`14` ticks / `0.70s`) using the player's actual skin/sleeves (`wide` & `slim`, 1- or 2-handed grip, `offhand` supported) instead of vanilla bow-aiming (`UseAnim.BOW`).
- **Mechanical Visor Flip (`N` Quick Tap)**:
  - **First-Person (`1P`)**: Added `FirstPersonVisorRenderer` (`0.78s` sub-tick `GogglesFlipAnimator`) showing the player's arm reaching up to pull the eye-facing ocular visor down from the upper brow mount and blending seamlessly via an ocular seal into the active shader & HUD.
  - **Third-Person (`F5` & Multiplayer)**: Smooth per-UUID `visor_pivot` rotation (`GogglesRenderLayer`), arm-reach pose (`MixinHumanoidModel`), and emissive lens glow only when powered on (plus independent `mod_flashlight` pod glow when only the flashlight is on).

### 4. Interactive 3D Modification Station & JEI Integration
- **2-Tab Left-Docked Tactical UI (`Assemble` & `Modify / Repair`)**:
  - **Assemble Tab**: Blueprint selector, live inventory + station material counter, interactive mat staging (`Left-Click` place 1 / `Right-Click` remove 1), 3D piece-by-piece rendering on the green cutting mat (`ModificationStationRenderer`), 1P/3P player hand assembly animations (`FirstPersonStationWorkRenderer`), and 3-stage `60`-tick (`3.0s`) construction (`neutral unlit chassis` → `lens snap` → `360° finished goggles`).
  - **Modify / Repair Tab**: Install modules, flash Lua scripts, extract modules cleanly without destroying goggles, and repair durability (`electrical_parts` `+35%` or `tactical_frame` `100%`, zero XP).
- **JEI Compatibility**: Added `station_assembly` and `modification_station` JEI categories (tab-gated click areas, live NBT output previews, zero-crash optional runtime).
- **Survival & Automation Protection**: Added `modification_station` loot table (`mineable/pickaxe`) and blocked hopper/shift-click output duplication.

### 5. Tactical Flashlight, Dynamic Shadows & Sodium/Embeddium
- **4 Beam Modes**: *Focused* (`52m`), *Wide* (`32m`), *Close/Flood* (`18m`), and *Tactical Strobe (`10 Hz`)* (`FlashlightDeterrence`: applies *Slowness I* to hostile mobs and suppresses Creeper detonation within `24` blocks; real `10 Hz` / `~24%` duty-cycle world-space pulse in `FlashlightUniforms` across Vanilla and Sodium/Embeddium, paired with calibrated NVG glare flashes without solid whiteout).
- **Dynamic Lighting Pipeline**: Added `post/flashlight.json`, `program/flashlight.{fsh,json}`, `VisionShadowMapRenderer`, `FlashlightUniforms`, `SodiumFlashlightUniforms`, and `SodiumShaderPatcher` for real-time beams in vanilla, Sodium, and Embeddium, with `CustomReequipItem` suppressing hand re-equip bobbing on battery/mode updates and `IViewRotMat` uploaded in `FlashlightUniforms` so entities and armor are properly lit instead of rendering black.
- **Independent Power & Toggle**: Modular goggles' `FLASHLIGHT` module toggles and glows independently of `TAG_ACTIVE`, with `TAG_BATTERY` safely initialized before module ticks.

### 6. Shaders, Vision Modes, Audio & Controls
- **Thermal FLIR Overhaul (`ThermalMask` FBO) & Armor Thermal Insulation**: Added `thermal/quad.vsh` and `thermal/thermal_overlay.fsh` for an isolated depth-occluded offscreen entity mask (eliminating wall X-Ray, sign/Item Frame text ghosts, and glass/water leaks), world-up 3D height gradient + Fresnel shading, Ironbow heat palette, combustion/cryogenic block signatures, and ENVG edge fusion (`DSP`). Added tiered armor thermal insulation via `data/vision_goggles/tags/items/thermal_insulation_{low,medium,high,full}.json` (supporting Vanilla, `lrarmor` 15 sets, Create, Thermal, Mekanism, Immersive Engineering, Botania, Twilight Forest, Ad Astra, Alex's Caves, and Cataclysm, plus an `ArmorItem` stat fallback), linear mapping in `thermal.fsh`, and cape/elytra exclusion; visible flashlight beams are suppressed during Thermal mode with `disableShader()` / `isThermalShaderActive()` cleanly resuming the flashlight afterward.
- **Pure Client-Side Night Vision**: Replaced 1.0.0's server `MobEffects.NIGHT_VISION` potion injection with `MixinLivingEntity` + `MixinGameRenderer` (zero potion HUD icons/particles; real potions unaffected), adding P43 phosphor persistence, raycast-occluded solar glare, explosion/lightning flash blinding, NVG glare from flashlights, and gun muzzle-flash NVG glare for TaCZ and firearm mods (`MixinClientLevel`, `MixinSoundEngine`, `triggerMuzzleFlash`).
- **Aspect Ratio & Focus Uniforms**: Added `AspectRatio` circular correction (`16:9` / `21:9`) and `focus` scaling across `nvg`, `thermal`, `bio`, `hydro`, and `hydro_dry` (cross-loader fog removal in `MixinFogRenderer`).
- **New Controls & Tactical Radial Wheel (`Hold N`)**:
  - Replaced 1.0.0's `key.vision_goggles.switch` with the `152×152` semi-transparent octagonal **Tactical Radial Wheel** (`Hold N`) for switching installed vision modes, locking **Zoom (`2.5x`)**, toggling **Flashlight**, toggling **Sonar Radar** (*All / Players / Undead*), and **Smart Battery Recharge** across the whole inventory.
  - Added `key.vision_goggles.toggle_flashlight` (`L`) and `key.vision_goggles.cycle_flashlight_mode` (`K`), plus continuous optical zoom (`V` + Scroll, `1.0x–8.0x`) and **Elytra HUD**.
- **New Sounds**: Added `blip.ogg`, `sonar.ogg`, `flashlight_switch.ogg`, `flashlight_mode.ogg`, and ambient cathode hum (`GogglesHumSoundInstance`).

### 7. Lua Scripting Sandbox, Commands & Config
- **Relocated Sandboxed LuaJ 3.0.1 Engine**: Relocated to `shadow.org.luaj` (eliminating split-package crashes with mods like Figura/Superb Warfare), protected by `ScriptQuotaExceededError` CPU watchdog, `world.worldToScreen` AR projection, `world.scanBlocks`, `e.thermal_visible`, HUD canvas, custom shader hooks, and **6 official bundled scripts** (`01_radar_tactico_360.lua` – `06_suite_termica_flir.lua`).
- **Commands (`/vg`) & Split Client/Server Config**: Added `/vg` command suite (`reload`, `slots`, `run`, `givescript` gated to OP level `2`, plus client `folder`), split client visual config from server balance config (`goggles_durability`, `battery_drain_multiplier`, `battery_standby_drain`, `battery_expansion_multiplier`, `solar_charge_rate`, `modular_max_modules`, `pro_modular_max_modules`, `damage_transfer_ratio`, `max_flashlights`, `nvg_glare_*`), and expanded localization from **12 → 16 languages** (`64 → 182` keys).

---

## Español — Changelog Completo 1.0.0 → 2.0.0

### 1. Nuevos Ítems, Componentes y Progresión
- **Nuevo Equipamiento y Módulos Tácticos**:
  - `tactical_flashlight` (**Linterna Táctica**): De mano o en el slot `body` (Curios/Accessories) con 4 modos de haz.
  - `flashlight_module` (**Módulo de Linterna**): Acoplable en gafas modulares; funciona independiente del modo de visión de las gafas.
  - `programmable_module` (**Módulo Programable**) e `installation_script` (**Script de Instalación**): Graba y ejecuta scripts Lua personalizados de HUD/AR/Shaders in-game.
- **6 Nuevos Componentes Intermedios (TEMS)**: `optical_lens` (*Lente Óptica de Precisión*), `tactical_frame` (*Marco de Chasis Táctico*), `flashlight_body` (*Cuerpo de Linterna*), `tactical_processor` (*Procesador DSP Táctico*), `thermal_core` (*Núcleo de Sensor Térmico*) y `lifesign_sensor` (*Sensor Biométrico*).
- **Nuevas Recetas en Mesa de Crafteo**: `environment_module`, `vital_info_module`, `flashlight_module`, `flashlight_body`, `optical_lens`, `tactical_frame`, `tactical_processor`, `thermal_core`, `lifesign_sensor`, `programmable_module` e `installation_script`.
- **Ensamblaje Trasladado a la Estación de Modificación**: Las 6 gafas y `tactical_flashlight` ya no se craftean en mesa vanilla; se ensamblan en la **Estación de Modificación** (`StationBlueprints`), conservando la mejora `Modulares → Pro Modulares` (`GogglesUpgradeRecipe`) con sus módulos NBT intactos.
- **Rediseño Pixel-Art (`Tactical Charcoal`)**: Texturas `16×16` rediseñadas con **13 ítems animados `.png.mcmeta`**, `batteries.png` → `nvg_battery.png` y retiro de `goggles_item.png` / `wrench_item.png`.

### 2. Modelos 3D Tri-Lente y Huesos Modulares Dinámicos
- **Reemplazo Completo del Modelo 1.0.0 (`128×128` → `64×64` Tri-Lente)**: 6 modelos GeckoLib 4 diferenciados (`night_vision_goggles`, `thermal_goggles`, `biometric_goggles`, `hydro_goggles`, `modular_goggles` y `pro_modular_goggles` en acabado obsidiana mate y pizarra titanio oscuro) con capas emisivas `*_glow.png` y copas oculares interiores coherentes por variante.
- **Piezas 3D Dinámicas por NBT**: Las **Gafas Modulares** y **Pro Modulares** muestran u ocultan piezas 3D en tiempo real en cabeza (`F5`), 1ª persona, mano/GUI y Mesa de Modificación:
  - **Fijos al chasis/correa**: `mod_pro_armor`, `mod_solar` (panel solar), `mod_battery_pack` (batería trasera), `mod_flashlight` (linterna lateral), `mod_lua_chip` (cartucho Lua), `mod_sonar`, `mod_vital_info` y `mod_environment`.
  - **Acoplados al visor (`visor_pivot`)**: `mod_zoom` (extensiones telescópicas) y `mod_optics_{nvg,thermal,bio,hydro,base}`.
- **Alineación en Marcos (`Item Frame`) e Inventario**: Calibradas las vistas `gui`, `ground`, `fixed` (de frente en Item Frames), `firstperson_*` y `thirdperson_*`.

### 3. Animaciones en 1ª y 3ª Persona
- **Equipado 3D en 4 Fases (`Clic Der. Sostenido`)**: Reemplaza la animación de arco por colocación con las manos reales del jugador (`14` ticks / `0.70s`, `wide`/`slim`, 1 o 2 manos y `offhand`).
- **Subida/Bajada del Visor (`N` toque rápido)**: Transición fluida de `0.78s` en 1ª persona (con copas oculares interiores bajando desde la montura superior y sello ocular sin cortes) y en 3ª persona (`F5` / multijugador) con brazo alcanzando la frente y brillo emisivo al activarse.

### 4. Estación de Modificación 2.0 y JEI
- **UI Táctica de 2 Pestañas Acoplada a la Izquierda**: *Ensamblar* (7 planos, conteo de materiales en vivo, colocación en tapete verde 3D, animación de manos del jugador y construcción en 3 etapas de `3.0s`) y *Modificar / Reparar* (instalar/extraer módulos sin destruir las gafas, grabar scripts Lua y reparar sin XP).
- **Integración JEI por Pestaña**: Categorías `station_assembly` y `modification_station` con vista previa NBT en vivo, además de loot table (`mineable/pickaxe`) y protección anti-duplicación en tolvas/shift-click.

### 5. Linterna Táctica, Sombras Dinámicas y Sodium/Embeddium
- **4 Modos de Haz**: *Enfocado* (`52m`), *Amplio* (`32m`), *Cercano* (`18m`) y **Estrobo Táctico (`10 Hz`)** que desorienta mobs (*Lentitud I*) y frena Creepers en `24` bloques, con parpadeo real en `FlashlightUniforms` (`10 Hz`, duty cycle `~24%` en Vanilla y Sodium/Embeddium) y flashazos calibrados contra NVG sin saturación en blanco sólido.
- **Pipeline de Iluminación y Encendido Independiente**: Soporte nativo para Vanilla, Sodium y Embeddium, corrección de entidades/armaduras negras bajo la linterna (`IViewRotMat` en `FlashlightUniforms`), supresión de bobbing al alternar modos (`CustomReequipItem`), reducción del `90%` en raycasts de CPU y funcionamiento independiente de la linterna modular respecto a `TAG_ACTIVE`.

### 6. Shaders, Controles y Audio
- **Térmico FLIR (`ThermalMask` FBO) y Aislamiento Térmico en Armaduras**: Máscara offscreen aislada con oclusión por profundidad (sin X-Ray ni textos fantasma), sombreado Fresnel, paleta Ironbow, bloques calientes/criogénicos y fusión de bordes ENVG. Añadido aislamiento térmico por tiers (`data/vision_goggles/tags/items/thermal_insulation_{low,medium,high,full}.json` para Vanilla, `lrarmor` 15 sets, Create, Thermal, Mekanism, Immersive Engineering, Botania, Twilight Forest, Ad Astra, Alex's Caves y Cataclysm, más fallback por stats de `ArmorItem`), mapeo lineal en `thermal.fsh`, exclusión de capas/élitros y supresión del haz visible de linterna en Visión Térmica (con fix de `disableShader()` / `isThermalShaderActive()` para reanudar la linterna al salir).
- **Visión Nocturna 100% Cliente**: Sin iconos ni partículas de poción vanilla; persistencia P43, ceguera solar con raycast, fogonazos por explosiones/rayos, glare por linternas y deslumbramiento por fogonazos de armas (TaCZ y mods de armas vía `MixinClientLevel`, `MixinSoundEngine` y `triggerMuzzleFlash`).
- **Controles, Rueda Radial (`Mantener N`) y Audio**: Reemplaza `key.vision_goggles.switch` de 1.0.0 por la Rueda Radial octogonal (`152×152`), añade `L` / `K` para linterna, zoom continuo (`V` + Rueda), HUD de Élitros y 4 nuevos sonidos (`blip`, `sonar`, `flashlight_switch`, `flashlight_mode` + zumbido ambiental).

### 7. Sandbox Lua, Comandos, Config e Idiomas
- **Motor LuaJ 3.0.1 Reubicado y Seguro**: Sin conflictos de paquetes en modpacks, con watchdog `ScriptQuotaExceededError`, API AR `world.worldToScreen` y **6 scripts incluidos** (`01_radar_tactico_360.lua` – `06_suite_termica_flir.lua`).
- **Comandos `/vg`, Config Cliente/Servidor y 16 Idiomas**: Comandos protegidos por OP nivel `2`, configuración dividida de cliente/servidor y expansión de **12 → 16 idiomas** (`64 → 182` claves traducidas al 100%).
