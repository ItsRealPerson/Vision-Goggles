# Vision Goggles — 1.0.0 → 2.0.0 (Full Changelog)

> **Minecraft:** `1.20.1` (`Forge` & `Fabric` via Architectury)  
> **Required Dependencies:** Architectury API, Cloth Config, GeckoLib 4, Curios (`Forge`) / Accessories (`Fabric`)  
> **Optional Compatibility:** JEI (Just Enough Items), Sodium / Embeddium

---

## English — Full Release Notes (1.0.0 → 2.0.0)

### 1. Tri-Lens 3D Overhaul & Dynamic Modular Attachments
- **New `64×64` Tri-Lens 3D Models Across All Goggles**: Replaced all legacy `128×128` models with unified, high-detail Tri-Lens 3D geometry and emissive `*_glow.png` layers across all 6 goggle tiers:
  - **Night Vision Goggles** (emerald phosphor optics)
  - **Thermal Goggles** (amber/crimson FLIR optics)
  - **Biometric Goggles** (crimson-ruby tactical optics)
  - **Hydro Goggles** (cyan-teal aquatic optics)
  - **Modular Goggles** (`2` module sockets — industrial gunmetal finish)
  - **Pro Modular Goggles** (`4` module sockets + built-in battery expansion — matte stealth obsidian & dark titanium-slate finish with reinforced armor plates)
- **Coherent Variant-Matched Ocular Eyecups**: Sculpted 3D binocular rubber eyecups with recessed dark-coated optical glass on the interior (`+Z`) side of the visor and a full-width Wilcox upper brow mount.
- **Dynamic 3D Modular Hardware on Your Goggles**: `Modular` and `Pro Modular` goggles now dynamically show or hide 3D attachments directly on the model (in 3rd-person `F5`, 1st-person `equip` & `flip`, hand/GUI, and on the Modification Station) based on installed NBT modules:
  - **Static Chassis / Strap Attachments**: Top **Solar Panel** (`solar_module`), enlarged rear **Battery Expansion Pack** (`battery_expansion_module`), right-rail **Tactical Flashlight Pod** (`flashlight_module`), left-rail **Programmable Lua PCB Cartridge** (`programmable_module`), **Sonar** acoustic pod (`sonar_module`), **Vital Info** telemetry pod (`vital_info_module`), and **Environment** atmospheric intake (`environment_module`).
  - **Flip-Coupled Visor Attachments**: Telescopic **Zoom** lens sleeves (`zoom_module`) and variant-specific optical pods/lenses (`nvg_module`, `thermal_module`, `bio_module`, `hydro_module`) that pivot smoothly up and down with the visor.

### 2. First-Person & Third-Person Animations
- **4-Phase 3D Equip Animation (`Right-Click Hold`)**: Replaced the vanilla bow-aiming animation with a 4-phase 3D placement animation (`14` ticks / `0.70s`) using your player's real skin & sleeves (supporting both `wide` and `slim` arms, 1- or 2-handed grip, and `offhand` equipping) to lift the goggles onto your head.
- **Mechanical Visor Flip-Down / Flip-Up (`N` Quick Tap)**:
  - **First-Person (`1P`)**: Smooth `0.78s` sub-tick visor transition showing your main arm reaching up to grip the visor, pulling the interior ocular eyecups down from the upper brow mount onto your eyes, and sealing seamlessly into the active shader & HUD (with zero flash or pop).
  - **Third-Person (`F5` & Multiplayer)**: Smooth per-UUID `visor_pivot` flip interpolation on player heads, synced arm-reach animation toward the forehead, and emissive lens glow gated cleanly on active power.

### 3. Interactive 3D Modification Station (Workbench 2.0)
- **Exclusive Goggles & Flashlight Assembly**: All 6 goggles and the Tactical Flashlight are now crafted exclusively at the **Modification Station** (removed from the vanilla crafting table), with a dedicated loot table (`mineable/pickaxe`) and hopper/shift-click anti-duplication protection.
- **Left-Docked 2-Tab Industrial UI**:
  - **Assemble Tab**: Visual blueprint selector for all 7 recipes (plus `Modular → Pro Modular` upgrade preserving installed NBT modules), live material counts across inventory + station, and interactive piece placement onto the workbench (`Left-Click` place 1 / `Right-Click` remove 1).
  - **Modify / Repair Tab**: Install modules, flash Lua scripts onto Programmable Modules, extract installed modules cleanly **without destroying your goggles**, and perform field repairs (`electrical_parts` for `+35%` or `tactical_frame` for `100%` durability, zero XP cost).
- **3D Cutting Mat Staging & Player Hand Work**: Placed parts appear symmetrically on the station's 3D green cutting mat; clicking **Assemble** triggers a 3-stage `60`-tick (`3.0s`) construction sequence (`neutral unlit chassis` → `module & lens snap with color shift` → `360° finished inspection`) accompanied by 1st-person and 3rd-person player hand animations.
- **Tab-Gated JEI Integration**: Dedicated `station_assembly` (7 blueprints) and `modification_station` (6 operations: *Install Module*, *Install Script Module*, *Flash Firmware*, *Extract Module*, *Maintenance Repair*, *Overhaul Repair*) categories with live NBT output tooltips, restricted cleanly to the active tab.

### 4. Tactical Flashlight & Dynamic Lighting
- **3 Ways to Carry & Independent Toggle**: Use the **Tactical Flashlight** in your hand, equipped in the `body` slot (Curios/Accessories, rendered on the player's chest), or installed as a **Flashlight Module** on `Modular` / `Pro Modular` goggles (`L` to toggle, `K` or `Shift + Right-Click` to cycle mode).
  - Goggle-mounted flashlights now operate **independently of the goggles' vision mode** (`TAG_ACTIVE`), lighting up only the `mod_flashlight` 3D pod glow when the visor is flipped up.
- **4 Beam Modes**:
  1. **Focused** (`52m` long-throw spot)
  2. **Wide** (`32m` balanced duty beam)
  3. **Close / Flood** (`18m` wide room-clearing flood)
  4. **Tactical Strobe (`~9.5 Hz`)**: Continuous `35%` base fill + high-frequency deterrent pulses (`FlashlightDeterrence`) that disorient hostile mobs (*Slowness I*) and suppress Creeper detonation countdowns within `24` blocks.
- **Dynamic Shadow Maps & Sodium / Embeddium Support**: Real-time beam projection with `90%` reduced CPU raycasts per frame, smooth volumetric haze (zero sky-noise artifacts), and suppressed vanilla hand re-equip bobbing (`CustomReequipItem`).

### 5. Shaders, Vision Modes & Tactical Telemetry
- **Thermal FLIR (`ThermalMask` FBO Overhaul)**:
  - Dedicated depth-occluded offscreen entity mask (`ThermalMask`) eliminating X-Ray artifacts through walls, Item Frame/sign text ghosts, and water/glass color-keying leaks.
  - World-up 3D height gradient + Fresnel shading, smooth Ironbow palette, `thermal_transparent` block support, combustion block glow (furnaces, campfires, lava, magma), cryogenic cold sinks (ice, packed/blue ice, powder snow), and ENVG edge fusion (`DSP` / `focus`).
- **Pure Client-Side Night Vision & Photonic Glare**:
  - Removed vanilla Night Vision potion effects (zero HUD potion icons or particles; real potions remain unaffected).
  - Realistic analog P43 phosphor startup & persistence, **raycast-occluded solar blinding**, explosion/lightning flash blinding (`40`–`64` blocks), and reactive NVG glare from other players' flashlights or your own torso/handheld flashlight.
- **Hydro & Biometric**: Cross-loader underwater fog removal (`MixinFogRenderer` in `common`), `AspectRatio`-corrected circular lenses/reticles for `16:9` and `21:9` displays, and `focus` uniform scaling (chromatic aberration, mil-dot density, and thermal pixelation at `>4x` zoom).
- **Continuous Optical Zoom (`V` + Scroll) & Rangefinder Telemetry**: Smooth `1.0x–8.0x` zoom, target brackets/entity tags (`< ZOMBIE >`), live distance up to `128` blocks, and **Elytra Flight HUD** telemetry.

### 6. Tactical Radial Wheel & Streamlined Controls
- **Octagonal Pixel-Art Radial Wheel (`Hold N`, `152×152`)**: Compact, semi-transparent Minecraft-styled radial dial dynamically populated with only your installed hardware:
  - Vision mode switching (`NVG`, `Thermal`, `Biometric`, `Hydro`, `Standby`)
  - **Zoom Lock** (`2.5x` toggle)
  - **Flashlight** toggle
  - **Sonar Radar** acoustic pulse (`All / Players / Undead`)
  - **Smart Battery Recharge** (scans hotbar, main inventory, and offhand automatically with ActionBar feedback)
- **Consolidated Keybinds**: `N` (tap = toggle visor, hold = radial menu), `V` (zoom + scroll), `L` (flashlight toggle), `K` (flashlight mode) — removed redundant `B`, `M`, `O`, `P` binds and retired `chunk_viewer_module` and `spawn_security_module`.

### 7. In-Game Lua Scripting Sandbox & Bundled Tactical Suite
- **LuaJ 3.0.1 Relocated Sandbox**: Relocated to avoid split-package crashes with mods like Figura or Superb Warfare, enforced by an unstoppable CPU instruction watchdog (`ScriptQuotaExceededError`), stripped of `luajava`/`io`/`package`/`require`, and safe on dedicated servers.
- **AR & Shader APIs**: Includes `world.worldToScreen(x, y, z)`, `world.scanBlocks(...)`, `world.getProjectilesInRadius(...)`, `e.thermal_visible`, `e.emits_heat`, `hud.drawLine/Circle/Disc`, custom `VISION_MODE` shaders, and `onThermalColor` entity IFF hooks.
- **6 Official Bundled Scripts** (auto-provisioned & auto-updated unless marked `-- [CUSTOM]`):
  1. `01_radar_tactico_360.lua` — Rotating 360° Sonar Threat Radar
  2. `02_visor_ar_objetivos.lua` — 3D AR Target Brackets & Off-screen Chevrons
  3. `03_visor_combate_reactivo.lua` — Gen-3 Auto-Gate Combat Visor & Projectile Ring
  4. `04_hud_piloto_avionica.lua` — Avionics Compass, Pitch Ladder, VSI & GPWS (`PULL UP!`)
  5. `05_escaner_geologico_entorno.lua` — Time-Sliced 3D Ore & Lava Prospector
  6. `06_suite_termica_flir.lua` — Ironbow FLIR Classifier & Cryogenic Stabilizer

### 8. Crafting Components, 2D Art, Config & Localization
- **6 New TEMS Crafting Components**: *Tactical Chassis Frame*, *Tactical DSP Processor*, *Precision Optical Lens*, *Thermal Sensor Core*, *Biometric Lifesign Sensor*, and *Machined Flashlight Body*.
- **21 Redesigned `16×16` Item Sprites**: *Tactical Charcoal* pixel art across all modules and parts, including **13 animated `.png.mcmeta` textures** (plus a tactical OLED cartridge sprite for `installation_script`).
- **Tactical Audio**: Mechanical mount clacks, ambient cathode hum, sonar pings, flashlight clicks, and assembly chimes.
- **Split Client / Server Config & OP Commands**: Client visual settings separated from OP-protected server balance settings (battery drain, standby drain, solar rate, glare, max flashlights, module slots), with OP level 2 gating on `/vg reload`, `/vg slots`, `/vg run`, and `/vg givescript`.
- **16-Language Full Parity (`181` Keys)**: Complete translations across `de_de`, `en_gb`, `en_us`, `es_ar`, `es_cl`, `es_es`, `es_mx`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pl_pl`, `pt_br`, `ru_ru`, `zh_cn`, and `zh_tw`.

---

## Español — Notas Completas de Lanzamiento (1.0.0 → 2.0.0)

### 1. Rediseño 3D Tri-Lente y Módulos Dinámicos Visibles
- **Nuevos Modelos 3D `64×64` Tri-Lente en Toda la Línea**: Se reemplazaron todos los modelos antiguos `128×128` por geometría Tri-Lente unificada con capas emisivas `*_glow.png` en las 6 gafas:
  - **Gafas de Visión Nocturna** (óptica verde esmeralda)
  - **Gafas Térmicas** (óptica FLIR ámbar/carmesí)
  - **Gafas Biométricas** (óptica táctica rubí)
  - **Gafas Hydro** (óptica acuática cian)
  - **Gafas Modulares** (`2` sockets — acabado gris metálico industrial)
  - **Gafas Pro Modulares** (`4` sockets + expansión de batería integrada — acabado obsidiana mate y pizarra titanio oscuro con placas de blindaje reforzadas)
- **Copas Oculares Coherentes por Variante**: Aros oculares binoculares 3D en goma oscura con cristal óptico recubierto en la cara interior (`+Z`) del visor y soporte frontal superior Wilcox de ancho completo.
- **Piezas 3D Dinámicas en Tiempo Real**: Las **Gafas Modulares** y **Pro Modulares** muestran u ocultanan automáticamente sus piezas 3D en cabeza (`F5`), 1ª persona (`equip` y `flip`), mano/GUI y Mesa de Modificación según el NBT del ítem:
  - **Fijos al chasis/correa**: **Panel Solar** superior (`solar_module`), **Expansión de Batería** trasera ampliada (`battery_expansion_module`), **Linterna Táctica** en riel derecho (`flashlight_module`), **Cartucho Chip Lua** en riel izquierdo (`programmable_module`), pod acústico **Sonar** (`sonar_module`), telemetría **Info Vital** (`vital_info_module`) y toma atmosférica **Entorno** (`environment_module`).
  - **Acoplados al visor abatible (`visor_pivot`)**: Extensiones telescópicas de **Zoom** (`zoom_module`) y módulos ópticos diferenciados (`nvg_module`, `thermal_module`, `bio_module`, `hydro_module`) que suben y bajan junto con el visor.

### 2. Animaciones en 1ª y 3ª Persona (`equip` y `flip`)
- **Animación 3D de Equipado en 4 Fases (`Clic Derecho Sostenido`)**: Reemplaza la animación de arco por una colocación táctica real (`14` ticks / `0.70s`) usando las manos/mangas de la skin del jugador (compatible con brazos `wide`/`slim`, agarre a 1 o 2 manos y mano secundaria).
- **Subida y Bajada Mecánica del Visor (`N` toque rápido)**:
  - **Primera Persona (`1P`)**: Transición fluida de `0.78s` donde el brazo sube al visor, desciende las copas oculares interiores desde la montura superior hasta los ojos y sella de forma invisible el paso al shader y HUD (sin parpadeos ni cortes).
  - **Tercera Persona (`F5` y Multijugador)**: Interpolación suave por jugador del pivote del visor, brazo alcanzando la frente y brillo emisivo en lentes solo cuando está encendido.

### 3. Estación de Modificación Interactiva 2.0 (Workbench 3D)
- **Ensamblaje Exclusivo**: Las 6 gafas y la Linterna Táctica ahora se fabrican exclusivamente en la **Estación de Modificación** (retiradas de la mesa de crafteo vanilla), con loot table propia (`mineable/pickaxe`) y protección anti-duplicación en tolvas y shift-click.
- **Interfaz Táctica de 2 Pestañas Acoplada a la Izquierda**:
  - **Pestaña Ensamblar**: Selector visual de los 7 planos (incluyendo la mejora `Modulares → Pro Modulares` conservando los módulos instalados), conteo en vivo de materiales e interacción directa sobre el tapete (*Click izq.* colocar 1 / *Click der.* retirar 1).
  - **Pestaña Modificar / Reparar**: Instala módulos, graba scripts Lua, extrae módulos instalados **sin destruir las gafas** y repara durabilidad (`electrical_parts` `+35%` o `tactical_frame` `100%`, sin costo de XP).
- **Construcción 3D sobre el Tapete Verde y Manos del Jugador**: Las piezas colocadas se ordenan sobre el tapete 3D; al pulsar **Ensamblar** se ejecuta una construcción de `60` ticks (`3.0s`) en 3 etapas (*chasis neutro sin brillo* → *encaje de piezas y cambio de color del lente* → *giro de inspección `360°`*) acompañada de animación de manos en 1ª y 3ª persona.
- **Integración con JEI por Pestaña**: Categorías `station_assembly` (7 planos) y `modification_station` (6 operaciones) con vista previa NBT en vivo y áreas de clic restringidas a la pestaña activa.

### 4. Linterna Táctica e Iluminación Dinámica
- **3 Formas de Uso y Encendido Independiente**: Úsala en la mano, equipada en el slot `body` (Curios/Accessories, visible en el pecho del jugador) o instalada como **Módulo de Linterna** en Gafas Modulares/Pro Modulares (`L` encender/apagar, `K` o `Shift + Clic Der.` cambiar modo).
  - En las Gafas Modulares, la linterna funciona **totalmente independiente del modo de visión de las gafas** (`TAG_ACTIVE`), iluminando únicamente el pod 3D `mod_flashlight` cuando el visor está arriba.
- **4 Modos de Haz**: *Enfocado* (`52m`), *Amplio* (`32m`), *Cercano* (`18m`) y **Estrobo Táctico (`~9.5 Hz`)** con `35%` de luz base continua + pulsos disuasorios que aplican *Lentitud I* a mobs hostiles y frenan Creepers en `24` bloques.
- **Mapas de Sombra Dinámicos y Sodium/Embeddium**: Reducción del `90%` en raycasts de CPU por frame, niebla volumétrica suave y supresión del balanceo de mano al alternar modos (`CustomReequipItem`).

### 5. Shaders, Modos de Visión y Telemetría
- **Visión Térmica FLIR (`ThermalMask` FBO)**: Máscara dedicada con oclusión por profundidad (cero X-Ray en marcos/carteles/muros ni contaminación por agua o cristal), gradiente 3D con sombreado Fresnel, paleta Ironbow, detección de bloques calientes/criogénicos y fusión de bordes ENVG (`DSP` / `focus`).
- **Visión Nocturna 100% Cliente y Deslumbramiento**: Sin iconos ni partículas de poción vanilla; persistencia de fósforo P43 y ceguera por sol (con oclusión por raycast), explosiones, rayos y linternas.
- **Hydro y Biométrico**: Eliminación de niebla subacuática en Forge y Fabric, retículas circulares adaptadas a `16:9` / `21:9` (`AspectRatio`) y escalado óptico de `focus` al superar `4x` de zoom.
- **Zoom Óptico Continuo (`V` + Rueda) y Telémetro**: Zoom suave `1.0x–8.0x`, corchetes de objetivo (`< ZOMBIE >`), distancia hasta `128` bloques y **HUD de vuelo con Élitros**.

### 6. Rueda Radial Táctica y Controles Simplificados
- **Rueda Octagonal Pixel-Art (`Mantener N`, `152×152`)**: Menú compacto semi-transparente estilo Minecraft que muestra únicamente los módulos instalados: cambio de modo de visión, **Bloqueo de Zoom** (`2.5x`), **Linterna**, **Radar Sonar** (*Todos / Jugadores / No-muertos*) y **Recarga Inteligente de Batería** en todo el inventario.
- **Controles Unificados**: `N` (toque = visor, mantener = rueda radial), `V` (zoom + rueda), `L` (linterna), `K` (modo de linterna). Se retiraron teclas redundantes (`B`, `M`, `O`, `P`) y los módulos obsoletos `chunk_viewer_module` y `spawn_security_module`.

### 7. Sandbox Lua In-Game y Suite de 6 Scripts Tácticos
- **Motor LuaJ 3.0.1 Reubicado y Seguro**: Sin conflictos con mods como Figura o Superb Warfare, protegido con watchdog imparable de CPU (`ScriptQuotaExceededError`), sin acceso a `luajava`/`io`/`package`/`require` y compatible con servidores dedicados.
- **APIs AR y Shaders**: Proyección 3D `world.worldToScreen`, `world.scanBlocks`, `world.getProjectilesInRadius`, `e.thermal_visible`, lienzo HUD y modos `VISION_MODE` personalizados.
- **6 Scripts Incluidos** (actualización automática segura respetando scripts `-- [CUSTOM]`):
  1. `01_radar_tactico_360.lua` — Radar Sonar Rotatorio 360°
  2. `02_visor_ar_objetivos.lua` — Corchetes AR 3D y Chevrones de Amenazas
  3. `03_visor_combate_reactivo.lua` — Visor Combate Gen-3 Auto-Gate y Anillo de Proyectiles
  4. `04_hud_piloto_avionica.lua` — Brújula, Escala de Cabeceo, VSI y Alarma GPWS (`¡PULL UP!`)
  5. `05_escaner_geologico_entorno.lua` — Prospector Geológico AR 3D de Menas y Lava
  6. `06_suite_termica_flir.lua` — Clasificador FLIR Ironbow y Estabilizador Criogénico

### 8. Componentes TEMS, Arte 2D, Configuración e Idiomas
- **6 Nuevos Componentes**: *Marco de Chasis Táctico*, *Procesador DSP Táctico*, *Lente Óptica de Precisión*, *Núcleo de Sensor Térmico*, *Sensor Biométrico* y *Cuerpo de Linterna*.
- **21 Ítems `16×16` Rediseñados**: Estética *Tactical Charcoal* con **13 texturas animadas `.png.mcmeta`** y diseño de cartucho OLED para `installation_script`.
- **Audio Táctico**: Sonidos mecánicos de montura, zumbido ambiental, pings de sonar, interruptores de linterna y efectos de ensamblaje.
- **Configuración Cliente/Servidor y 16 Idiomas (`181` Claves)**: Balance de servidor separado y protegido por nivel OP 2 en `/vg`, con paridad del `100%` en `es_es`, `es_mx`, `es_ar`, `es_cl`, `en_us`, `en_gb`, `pt_br`, `fr_fr`, `de_de`, `it_it`, `pl_pl`, `ru_ru`, `ja_jp`, `ko_kr`, `zh_cn` y `zh_tw`.
