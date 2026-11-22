# Vision Goggles — 1.0.0 → 2.0.0 (Full Changelog)

> **Minecraft:** `1.20.1` (`Forge` & `Fabric` via Architectury)  
> **Required Dependencies:** Architectury API, Cloth Config, GeckoLib 4, Curios (`Forge`) / Accessories (`Fabric`)  
> **Optional Compatibility:** JEI (Just Enough Items), Sodium / Embeddium

---

## English (Modrinth Release Notes — Full Changes Since 1.0.0)

### Major Highlights
- **Tri-Lens 3D Overhaul & Dynamic Modular Attachments**: Every goggle variant (`Night Vision`, `Thermal`, `Biometric`, `Hydro`, `Modular`, and matte stealth obsidian/titanium `Pro Modular`) now uses unified `64×64` Tri-Lens 3D models with variant-matched ocular eyecups. `Modular` and `Pro Modular` goggles dynamically display or hide 3D hardware attachments on your head, in hand, in GUI, and on the Modification Station based on installed modules (`Solar Panel`, `Battery Expansion` rear pack, `Tactical Flashlight` pod, `Programmable Lua Chip`, `Zoom` telescopic sleeves, `Sonar`, `Vital Info`, `Environment`, and vision optics).
- **Interactive 3D Modification Station (Workbench 2.0)**: Exclusive assembly bench for all 6 goggles and the Tactical Flashlight (removed from the vanilla crafting table), featuring a left-docked 2-tab tactical UI (*Assemble* & *Modify / Repair*), live inventory material tracking, 3D piece-by-piece staging on the cutting mat, 1st/3rd-person player hand assembly animations, 3-stage construction (`neutral unlit chassis` → `lens snap` → `finished goggles`), non-destructive module extraction, and field repairs.
- **First-Person & Third-Person Animations**:
  - **4-Phase 3D Equip Animation**: Holding right-click smoothly lifts and places the 3D goggles onto your head with 1- or 2-handed player skin/sleeve arms.
  - **Visor Flip-Down / Flip-Up (`N` tap)**: Smooth sub-tick transition in 1st person (eye-facing ocular eyecups anchored to the top brow mount + arm reach + ocular blackout seal) and 3rd person (`F5` / multiplayer) with emissive optical glow when powered.
- **Tactical Flashlight & Dynamic Lighting**: Handheld, `body`-slot (Curios/Accessories), and goggle-mounted **Flashlight Module** (toggleable independently of the goggles' vision mode) with **4 beam modes** (*Focused*, *Wide*, *Close/Flood*, and *Tactical Strobe ~9.5 Hz* with mob deterrence & Creeper fuse suppression), dynamic shadow maps, and Sodium/Embeddium chunk shader support.
- **Upgraded Shaders & Thermal FLIR (`ThermalMask` FBO)**:
  - **Thermal**: Isolated depth-occluded offscreen entity mask (`ThermalMask`), world-up 3D height gradient + Fresnel shading, Ironbow heat palette, combustion/cryogenic block signatures, and ENVG edge fusion (`DSP`).
  - **Pure Client-Side Night Vision**: Eliminates vanilla Night Vision potion icons and particles while adding realistic P43 phosphor persistence and photonic glare from sun raycasts, explosions, lightning, and active flashlights.
  - **Hydro & Biometric**: Cross-loader underwater fog removal and `AspectRatio`-correct circular reticles/mil-dot focus scaling.
- **Tactical Radial Wheel (`Hold N`) & Optical Zoom (`V` + Scroll)**: Compact semi-transparent octagonal pixel-art wheel dynamically populated with your installed vision modes, Zoom lock, Flashlight toggle/mode, Sonar Radar, and smart inventory-wide battery recharge.
- **In-Game Lua Scripting (`Programmable Module` & `Installation Script`)**: Sandboxed LuaJ engine with CPU instruction watchdog, `world.worldToScreen` AR projection, HUD canvas, custom shader/uniform hooks, and 6 bundled tactical scripts (`01_radar_tactico_360.lua` – `06_suite_termica_flir.lua`).

### Added
- **6 New Crafting Components & Progression**: *Tactical Chassis Frame*, *Tactical DSP Processor*, *Precision Optical Lens*, *Thermal Sensor Core*, *Biometric Lifesign Sensor*, and *Machined Flashlight Body*, plus a programmatic upgrade path from `Modular` to `Pro Modular` (`4` module sockets + `Battery Expansion`) preserving installed NBT modules.
- **JEI Integration**: Full recipe support for `station_assembly` (7 blueprints) and `modification_station` (*Install Module*, *Install Script Module*, *Flash Firmware*, *Extract Module*, *Maintenance Repair*, *Overhaul Repair*), gated cleanly to the active tab.
- **Tactical Audio**: Mechanical mount clacks, ambient visor hum, sonar pings, flashlight switches, and assembly sounds.
- **21 Redesigned `16×16` Item Sprites**: Tactical Charcoal pixel art across all modules and components, including **13 animated `.png.mcmeta` items**.
- **16-Language Localization**: Expanded to 181 synchronized keys across `de_de`, `en_gb`, `en_us`, `es_ar`, `es_cl`, `es_es`, `es_mx`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pl_pl`, `pt_br`, `ru_ru`, `zh_cn`, and `zh_tw`.

### Changed & Fixed
- **Multiplatform & Dedicated Server Safety**: Rebuilt on Architectury (`Forge` & `Fabric`) with strict client/server separation, OP-gated `/vg` commands, and relocated scripting libraries to avoid modpack conflicts.
- **Survival & Automation Integrity**: Added `modification_station` loot table (`mineable/pickaxe`), blocked hopper/shift-click output duplication exploits, and ensured `TAG_BATTERY` initializes cleanly before module power ticks.
- **Shader & Camera Stability**: Persistent `PostChain` caching across `F5` perspective switches and window resizes, `90%` reduction in per-frame flashlight CPU raycasts, and fixed `180°` Item Frame display orientation (`fixed`).

---

## Español (Notas de Lanzamiento en Modrinth — Cambios Completos desde 1.0.0)

### Novedades Principales
- **Rediseño 3D Tri-Lente y Módulos Dinámicos Visibles**: Todas las gafas (`Visión Nocturna`, `Térmicas`, `Biométricas`, `Hydro`, `Modulares` y las `Pro Modulares` en acabado obsidiana mate y titanio oscuro) ahora usan modelos 3D `64×64` Tri-Lente con copas oculares coherentes. Las **Gafas Modulares** y **Pro Modulares** muestran u ocultanan dinámicamente en 3D sus piezas instaladas tanto en cabeza (`F5`), como en mano, GUI y Mesa de Modificación (`Panel Solar`, `Expansión de Batería` trasera, `Linterna Táctica` lateral, `Chip Lua`, extensiones de `Zoom`, `Sonar`, `Info Vital`, `Entorno` y ópticos de modo de visión).
- **Estación de Modificación Interactiva 2.0**: Mesa de ensamblaje exclusiva para las 6 gafas y la Linterna Táctica (retiradas del crafteo vanilla), con interfaz táctica de 2 pestañas (*Ensamblar* y *Modificar / Reparar*) acoplada a la izquierda, conteo en vivo de materiales, colocación 3D ordenada sobre el tapete verde, animación de manos del jugador trabajando sobre la mesa, construcción visual en 3 etapas (*chasis neutro sin brillo* → *acople de lentes* → *gafas terminadas*), extracción de módulos sin destruir las gafas y reparación de campo.
- **Animaciones en 1ª y 3ª Persona (`equip` y `flip`)**:
  - **Equipar en 3D (4 fases)**: Al mantener clic derecho, el jugador levanta las gafas con sus manos reales y se las coloca en la cabeza.
  - **Subir/Bajar Visor (`N` toque rápido)**: Transición fluida en 1ª persona (con vista interior ocular anclada al soporte superior + brazo + sello ocular) y en 3ª persona (`F5` / multijugador) con brillo emisivo en los lentes al encenderse.
- **Linterna Táctica e Iluminación Dinámica**: Linterna de mano, en slot `body` (Curios/Accessories) y como módulo acoplado a las gafas (encendido independiente del modo de visión de las gafas) con **4 modos de haz** (*Enfocado*, *Amplio*, *Cercano* y *Estrobo Táctico ~9.5 Hz* con disuasión de mobs y cancelación de Creepers), sombras dinámicas y soporte para Sodium/Embeddium.
- **Shaders Mejorados y Visión Térmica FLIR (`ThermalMask` FBO)**:
  - **Térmico**: Máscara dedicada de entidades con oclusión por profundidad (sin X-Ray ni textos fantasma), gradiente 3D real, paleta Ironbow, detección de bloques calientes/criogénicos y fusión de bordes ENVG (`DSP`).
  - **Visión Nocturna 100% Cliente**: Sin iconos ni partículas de poción vanilla; persistencia realista de fósforo P43 y deslumbramiento por sol (con oclusión por raycast), explosiones, rayos y linternas.
  - **Hydro y Biométrico**: Eliminación de niebla subacuática en Forge y Fabric, y retículas circulares corregidas para pantallas panorámicas (`AspectRatio`).
- **Rueda Radial Táctica (`Mantener N`) y Zoom Óptico (`V` + Rueda)**: Menú octagonal semi-transparente pixel-art con acceso rápido a modos instalados, bloqueo de Zoom, Linterna, Radar Sonar y recarga inteligente de batería desde todo el inventario.
- **Scripting Lua In-Game (`Módulo Programable` y `Script de Instalación`)**: Sandbox LuaJ seguro con watchdog de CPU, proyección AR `world.worldToScreen`, lienzo HUD, uniformes de shader personalizados y 6 scripts incluidos (`01_radar_tactico_360.lua` – `06_suite_termica_flir.lua`).

### Añadido
- **6 Nuevos Componentes de Fabricación y Progresión**: *Marco de Chasis Táctico*, *Procesador DSP Táctico*, *Lente Óptica de Precisión*, *Núcleo de Sensor Térmico*, *Sensor Biométrico* y *Cuerpo de Linterna*, además de mejora directa de `Modulares` a `Pro Modulares` (`4` sockets + `Expansión de Batería`) conservando los módulos instalados.
- **Integración con JEI**: Recetas para las 2 pestañas de la Estación de Modificación (*Ensamblar*, *Instalar/Extraer Módulos*, *Grabar Firmware Lua* y *Reparar*), activas contextualmente por pestaña.
- **Audio Táctico**: Sonidos mecánicos de acople, zumbido ambiental del visor, pings de sonar, interruptores de linterna y efectos de ensamblaje.
- **21 Ítems `16×16` Rediseñados**: Estética *Tactical Charcoal* con **13 micro-animaciones `.png.mcmeta`**.
- **Localización en 16 Idiomas**: 181 claves sincronizadas al 100% (`es_es`, `es_mx`, `es_ar`, `es_cl`, `en_us`, `en_gb`, `pt_br`, `fr_fr`, `de_de`, `it_it`, `pl_pl`, `ru_ru`, `ja_jp`, `ko_kr`, `zh_cn`, `zh_tw`).

### Cambiado y Corregido
- **Estabilidad Multiplataforma y Servidores Dedicados**: Arquitectura Architectury (`Forge` y `Fabric`) con aislamiento estricto cliente/servidor, comandos `/vg` protegidos por nivel OP y librerías reubicadas para evitar conflictos en modpacks.
- **Integridad en Supervivencia**: Loot table propia de `modification_station` (`mineable/pickaxe`), cierre de exploits de duplicación en tolvas/shift-click e inicialización segura de batería antes del tick de módulos.
- **Rendimiento y Orientación**: Caché persistente de shaders al alternar `F5` o redimensionar ventana, reducción del `90%` en raycasts de CPU por linterna y orientación frontal correcta en Marcos de Ítems (`Item Frame`).
