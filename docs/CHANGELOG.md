# Changelog - Vision Goggles

## [2.0.0] - 2026-11-21

### Added & Improved (2026-11-21 — Cierre M1/M2 + Animaciones 1P/3P Vinculadas al Jugador para Prototipo Tri-Lente)
- **Animaciones 1P y 3P Vinculadas al Jugador para Vista Previa Aislada (`ProtoTriLensPreviewState`, `ProtoTriLensGeoRenderer`, `FirstPersonVisorRenderer`, `MixinHumanoidModel`, `VisionRenderer`, `CommandVisionGoggles`, `OpenProtoPreviewPacket`):**
  - Añadida máquina de estados en juego (`FlipState`: `UP` / `FLIPPING_DOWN` / `DOWN_ACTIVE` / `FLIPPING_UP`, duración `0.78s`, fases `REACH=0.28` / `VISOR=0.84` / `HANDOFF=0.85`) sincronizada por frame vía `VisionRenderer.syncOpticsForRenderFrame(mc)` y activable con `N` (toque rápido `<= 220ms`), `V` (`zoomKey`), botón en `TriLensPreviewScreen` o `/vg preview flip`.
  - **Tercera persona (`F5`):** `MixinHumanoidModel` anima el brazo principal extendiéndose a la frente al subir/bajar el visor (`reach` + `visorUp`), y `GogglesRenderLayer` + `ProtoTriLensGeoRenderer.renderOnPlayerHead(...)` pivotan suavemente el hueso `visor` entre `DOWN` y `UP` sobre la cabeza del jugador local.
  - **Primera persona (`1P`):** `FirstPersonVisorRenderer` renderiza el visor Tri-Lente 3D (`ProtoTriLensGeoRenderer.renderFirstPersonVisor(...)`) junto con el brazo del jugador bajando/subiendo el visor y sello ocular progresivo durante la transición.
  - **Aislamiento garantizado:** Solo actúa cuando `!hasRealGoggles` (`!hasHelmet`) y `mc.screen == null` (sin interferir con gafas reales equipadas, `FirstPersonVisorModel`, `VisionGoggleModel`, `GogglesFlipAnimator`, `ModificationStationRenderer`, `chassis_neutral.png` ni las 16 localizaciones `lang/*.json`).
- **Resumen Integrado M1 + M2 Verificado:** Rework de manos 1P y zoom suave (`0.90× -> 0.85×`) en la mesa de modificación (`StationWorkAnimator`, `FirstPersonStationWorkRenderer`), 5 prototipos Blockbench Tri-Lente validados (`proto_tri_lens_{nvg,thermal,bio,hydro,modular}.bbmodel`), 10 texturas `64×64` + renders multi-ángulo (`models_src/tri_lens_proto/`, `scripts/generate_tri_lens_textures.py`), exportación GeckoLib 4 (`proto_tri_lens_*.geo.json` + `proto_tri_lens.animation.json`), e inspector aislado `/vg preview [variant|flip|off]` con toggle `F5 Head`.

### Added & Improved (2026-11-21 — Prototipo 3D No Destructivo Tri-Lente + Rework Manos/Zoom en Mesa)
- **Manos 1P y Zoom Suave en Mesa de Modificación (`StationWorkAnimator`, `FirstPersonStationWorkRenderer`):** Ajustado `getWorkbenchZoomMultiplier()` a `0.90×` en apertura y `0.85×` en pico de ensamblaje (reemplazando `0.62×`), y reubicadas las manos hacia abajo y adelante sobre el tapete verde (`±0.34, -0.37, -0.68`, `rotX = -62°` en espera; `±0.20, -0.30, -0.76` al colocar piezas; `-0.25, -0.31, -0.73` y `0.26→0.17, -0.31→-0.26, -0.72→-0.80` al ensamblar) manteniendo la vista del trabajo despejada.
- **Prototipo 3D Tri-Lente Táctico y Variantes (`models_src/tri_lens_proto/`, `scripts/generate_tri_lens_textures.py`):** Creados y validados vía Blockbench MCP (`bbmodel_validate` con 0 errores) 5 prototipos diferenciados (`proto_tri_lens_{nvg,thermal,bio,hydro,modular}.bbmodel`) con 10 texturas `64×64` (base + máscara emisiva `*_glow.png`), renders multi-ángulo y exportación GeckoLib 4 en `geo/item/proto_tri_lens_*.geo.json` + `animations/item/proto_tri_lens.animation.json`, conservando intactas todas las gafas actuales y `chassis_neutral.png`.
- **Vista Previa Aislada In-Game `/vg preview` (`TriLensPreviewScreen`, `ProtoTriLensGeoRenderer`, `ProtoTriLensVariant`, `ProtoTriLensPreviewState`, `OpenProtoPreviewPacket`, `CommandVisionGoggles`, `GogglesRenderLayer`, `VisionRenderer`):** Añadido el comando multi-loader `/vg preview [nvg|thermal|bio|hydro|modular|off]` con inspector 3D orbital en GUI biselada estilo Minecraft, cambio de variantes, animación de visor `DOWN/UP`, toggles de brillo emisivo, caja guía de cabeza Steve (`[-4..4, 24..32, -4..4]`), giro automático y vista previa local opcional en cabeza (`F5 Head`, apagada por defecto y reiniciada al salir del mundo).

### Improved (2026-11-21 — Plan v44b: Limpieza de Líneas de Bisel en `radial_wheel.png`)
- **Textura Limpia Sin Líneas (`radial_wheel.png`, `scripts/generate_radial_wheel_png.py`):** Eliminadas las líneas de bisel 2px exterior e interior, los remaches y el contorno doble del centro en el octágono `152×152`, dejando un panel plano semi-transparente limpio con borde único de `1px`.

### Improved (2026-11-21 — Plan v44: Rueda Octagonal Pixel-Art Compacta y Semi-Transparente)
- **Rediseño Compacto de `radial_wheel.png` y `TacticalRadialScreen`:** Reducida la rueda de `192×192` opaco a un octágono biselado estilo Minecraft de `152×152` semi-transparente (`alpha=212`, `NODE_RADIUS = 48`), evitando que bloquee la pantalla o que las etiquetas rocen el borde exterior.

### Reverted (2026-11-21 — Plan v45b: Restauración del Modelo 3D Original de Gafas NVG)
- **Rollback Limpio del Prototipo (`VisionGogglesGeoModel`, `VisionGogglesGeoRenderer`):** Se eliminaron `night_vision_goggles_proto.geo.json`, `nvg_proto.png`, `nvg_proto_glow.png` y el flag `USE_NVG_PROTOTYPE`, restaurando al 100% el modelo y texturas originales de las gafas (`night_vision_goggles.geo.json` + `nvg_texture.png`), conservando `chassis_neutral.png` para el chasis neutro en mesa.

### Added (2026-11-21 — Plan v45: Prototipo 3D No Destructivo Tri-Lente Táctico estilo Splinter Cell)
- **Nuevo Diseño Tri-Lente (`night_vision_goggles_proto.geo.json`, `nvg_proto.png`, `nvg_proto_glow.png`):** Prototipo completamente distinto al original con 3 ópticos circulares esmeralda dispuestos en triángulo invertido sobre puente en Y, brazos laterales al chasis, correa táctica continua y transceptor trasero con LED. Se mantiene intacto el modelo original (`USE_NVG_PROTOTYPE = true` en `VisionGogglesGeoModel`).

### Improved (2026-11-21 — Plan v44: Zoom de Cámara en Mesa, Manos Ensamblando en el Centro y Textura Neutra sin Luces Verdes)
- **Textura de Chasis Neutro (`chassis_neutral.png`, `VisionGogglesGeoRenderer`, `ModificationStationRenderer`):** Generada nueva textura `textures/entities/chassis_neutral.png` (128×128, lentes oscuros sin luces verdes) aplicada automáticamente al chasis durante la Etapa 1 y pre-lente de Etapa 2 mediante `vg_neutral_chassis`, para que el lente se ilumine con su color real al encajar.
- **Zoom Suave en Mesa y Ensamblaje (`StationWorkAnimator.getWorkbenchZoomMultiplier`, `MixinGameRenderer`):** Enfoque progresivo de cámara (`0.80×` al abrir la estación, `0.62×` durante el ensamblaje).
- **Manos 3D Ensamblando en el Centro (`FirstPersonStationWorkRenderer`):** Las manos alcanzan el centro (`±0.14f, y=-0.18f`) para sostener el chasis, presionar/acoplar los componentes y el lente frontal en 3 ciclos rítmicos, y retirarse al presentar las gafas terminadas.

### Added & Changed (2026-11-21 — Plan v43b: Rueda de Acciones Pixel-Art con Textura PNG Vanilla)
- **Sprite Sheet Pixel-Art (`textures/gui/radial_wheel.png`, `scripts/generate_radial_wheel_png.py`):** Generada textura `256×256` RGBA estilo GUI vanilla de Minecraft con rueda base `192×192` (bisel 3D top-left, contorno `1px`, 4 remaches y centro empotrado) y 3 variantes de slot `24×24` (normal, seleccionado estilo hotbar y deshabilitado).
- **Renderizado por Textura (`TacticalRadialScreen`):** Reemplazado el dibujo procedural por `guiGraphics.blit(RADIAL_TEXTURE, ...)` con `RenderSystem.enableBlend()` para la rueda y sus ranuras `18×18`.

### Improved (2026-11-21 — Plan v43: Construcción Progresiva Chasis -> Cambio de Color de Lente -> Gafas Terminadas)
- **Secuencia Visual de 3 Etapas (`ModificationStationBlockEntity`, `ModificationStationRenderer`, `StationWorkAnimator`):** Duración ampliada a `60` ticks (`3.0s`):
  1. **Armado del chasis (`0%–28%`):** El armazón base se eleva al centro formando primero el chasis neutro sin color (`MODULAR_GOGGLES`).
  2. **Instalación y cambio de color del lente (`28%–72%`):** Las piezas auxiliares se acoplan primero y el lente/módulo de visión vuela último hacia el visor frontal, cambiando en ese instante el modelo 3D del chasis neutro al modelo con lentes de color definitivo + sonido de cristal/chispas.
  3. **Gafas terminadas (`72%–100%`):** Giro de inspección `360°` y asentamiento final antes de entregarse al inventario.

### Changed & Reworked (2026-11-21 — Plan v42b: Rueda de Acciones Sin Líneas ni Anillos)
- **Unificación Minecrafty Total de `TacticalRadialScreen`:** Eliminados el anillo pixelado (`drawPixelRing`), el conector radial (`drawPixelSpoke`), los bordes de color neón y el texto `"SYSTEM READY"`. Los botones radiales y el hub central ahora usan exactamente el mismo panel gunmetal 3D biselado (`0xFF2B2F36` / `0xFF545B66`) y ranuras `18×18` de `ModificationStationScreen`, con highlight clásico de slot (`0x40FFFFFF`).

### Added & Changed (2026-11-21 — Plan v42: Animación 3D Real de Construcción Pieza por Pieza y Tooltip de Controles)
- **Construcción 3D Progresiva sobre el Tapete (`ModificationStationBlockEntity`, `ModificationStationRenderer`, `StationWorkAnimator`):** Al pulsar **Ensamblar**, se conservan y sincronizan las piezas (`AssemblingParts`) durante `48` ticks (`2.4s`): primero el chasis base se eleva al centro rotando a vertical, luego cada componente restante vuela en arco por turnos y se acopla al chasis con retroceso mecánico, chispas y clics progresivos antes del bloqueo final.
- **Feedback Preciso de Click Izquierdo/Derecho (`ModificationStationScreen`, `lang/*.json`):** Se valida `canAct` antes de disparar `triggerPlacePart` y se añade el tooltip `gui.vision_goggles.tooltip.stage_controls` (`Click izq.: Colocar 1 | Click der.: Quitar 1`) en los 16 idiomas.

### Changed & Reworked (2026-11-21 — Plan v41: Estética 100% Minecrafty en Estación de Modificación y Rework de Rueda de Acciones)
- **Limpieza Visual Minecrafty en `ModificationStationScreen`:** Eliminadas las barras/líneas de acento verde neón en la pestaña activa (`WorkstationButton`), en el plano seleccionado y en el borde izquierdo de las tarjetas de ingredientes (`renderAssembleTab`), así como la ranura divisoria sobre el inventario (`renderBg`). Reemplazados por botones biselados estilo Stonecutter, tinte de ranura tipo Recipe Book para materiales faltantes/completados y conectores `+` y `->` simétricos en gris sombreado clásico de Minecraft (`renderModifyTab`).
- **Rework Completo de la Rueda de Acciones (`TacticalRadialScreen`):** Eliminada la geometría vectorial sci-fi (`drawDonutWedge`, `drawCompassRingAndTicks`, `drawCenterReticle`, `drawTacticalSocket`) y textos `TAC-HUD` / `VG-OS`. Reemplazada por un anillo pixelado rasterizado por scanlines enteras, tarjetas biseladas estilo Minecraft (`54×34`) con ranuras empotradas `18×18` para los ítems, conector radial pixelado y hub central con estilo de tooltip clásico de Minecraft.

### Fixed & Improved (2026-11-21 — Reset Limpio de Animación de Manos al Entrar/Salir y Rediseño Visual Minecraft Industrial de la Estación)
- **Reset Completo de Animación al Entrar/Salir (`StationWorkAnimator`, `MixinItemInHandRenderer`):** `setWorkbenchOpen(true, pos)` reinicia limpiamente `openBlend`, `placing` y `assembling` en cada nueva apertura, `setWorkbenchOpen(false, null)` ejecuta `reset()` inmediato al cerrar la UI y `MixinItemInHandRenderer` sincroniza `syncFromRender(player)` incluso en tercera persona (`F5`) para evitar brazos congelados en alto.
- **Rediseño Industrial Minecraft de `ModificationStationScreen`:** Reemplazo del panel plano por marco metálico 3D biselado con remaches, ranuras empotradas estilo Minecraft (`drawSlotBox` / `drawRecessedBox`), botones tácticos `WorkstationButton` integrados y tarjetas de ingredientes con indicadores de estado discretos.

### Fixed (2026-11-21 — Paridad y Traducción Completa en los 16 Idiomas)
- **Traducciones 100% Sincronizadas (`assets/vision_goggles/lang/*.json`):** Todos los 16 idiomas (`de_de`, `en_gb`, `en_us`, `es_ar`, `es_cl`, `es_es`, `es_mx`, `fr_fr`, `it_it`, `ja_jp`, `ko_kr`, `pl_pl`, `pt_br`, `ru_ru`, `zh_cn`, `zh_tw`) quedaron con **181 claves exactas** (`missing=0`, `extra=0`), completando las 4 claves faltantes en `en_gb`/`es_ar`/`es_cl`/`es_mx`, traduciendo las 9 claves nuevas de la Estación/JEI en los 10 idiomas no hispano-ingleses y añadiendo las 4 claves de configuración `config.vision_goggles.*_max_modules`.

### Fixed (2026-11-21 — Ordenamiento de Piezas en Tapete 3D y Supresión de Recetas JEI en Pestaña Ensamblar)
- **Tapete 3D Ordenado por Grupos (`ModificationStationRenderer`):** Agrupamiento por tipo de ítem sobre cuadrícula simétrica (`GRID_2X2` / `GRID_3X2`, escala `0.21f`) evitando colisiones con la almohadilla azul inferior izquierda y apilando duplicados limpiamente sin z-fighting.
- **JEI Click Area Restringida a Modificar/Reparar (`VisionGogglesJeiPluginCommon`, `ModificationStationScreen`):** Se migró `addRecipeClickArea` a `IGuiContainerHandler#getGuiClickableAreas` condicionado a `screen.isModifyTabActive()`, eliminando el tooltip y apertura accidental de recetas JEI al hacer click sobre la tarjeta del segundo ingrediente en la pestaña `Ensamblar`.

### Added & Changed (2026-11-21 — Plan v40: Mesa Interactiva Paso a Paso, Manos 3D Visibles al Abrir y UI Acoplada a la Izquierda)
- **Manos 3D Visibles Desde la Apertura (`StationWorkAnimator`, `FirstPersonStationWorkRenderer`, `MixinItemInHandRenderer`, `MixinHumanoidModel`):** Al abrir la `ModificationStation`, ambas manos 3D del jugador se muestran inmediatamente sobre el tapete verde, con animación de alcance a una mano al colocar/retirar cada pieza y animación rítmica de dos manos durante 20 ticks (`1.0s`) al presionar **Armar**.
- **Interfaz Acoplada a la Izquierda sin Oscurecimiento (`ModificationStationScreen`, `ModificationStationMenu`, `VisionGogglesJeiPluginCommon`):** Panel `176×188` anclado a `leftPos = 6` sin `renderBackground` oscuro para dejar libre el tapete verde 3D central, con tira superior de 7 planos, tarjetas de ingredientes clicables (`click izq.` colocar / `click der.` retirar) y botón **Armar** (`FINALIZE_ASSEMBLY`).
- **Ensamblaje Progresivo sobre el Tapete (`StationBlueprints`, `AssembleBlueprintPacket`, `ModificationStationBlockEntity`, `ModificationStationBlock`, `ModificationStationRenderer`):** Persistencia de `ActiveBlueprint` y `StagedItems` en NBT/sync cliente, devolución automática de piezas al cerrar/romper el bloque, preservación de NBT al mejorar `modular_goggles` a `pro_modular_goggles`, y render 3D con gafas erguidas (`pitch=0°, yaw=180°`) y componentes planos (`pitch=90°`) sobre `MAT_OFFSETS`.

### Added & Changed (2026-11-21 — Plan v39: Ensamblaje Exclusivo de Gafas en la Estación con Manos 3D y Render sobre Tapete)
- **Ensamblaje Exclusivo en `ModificationStation` (`StationBlueprints`, `AssembleBlueprintPacket`)**:
  - Retiradas de la mesa de crafteo vanilla las recetas de las **6 gafas** (`night_vision_goggles`, `thermal_goggles`, `hydro_goggles`, `biometric_goggles`, `modular_goggles`, `pro_modular_goggles`) y de la **Linterna Táctica** (`tactical_flashlight`), trasladándolas exclusivamente a la Estación de Modificación (`StationBlueprints.java`).
  - Conservada la mejora de `modular_goggles` -> `pro_modular_goggles` con transferencia automática de NBT/módulos instalados y adición automática de `battery_expansion_module`.
- **Interfaz Táctica de 2 Pestañas (`ModificationStationMenu`, `ModificationStationScreen`)**:
  - **Pestaña 0 (`Ensamblar`)**: selector visual de planos, monitor táctico con vista previa, conteo en vivo de materiales (`tienes / necesitas` desde inventario + mesa con tooltips) y botón `Ensamblar`.
  - **Pestaña 1 (`Modificar / Reparar`)**: conserva los 3 slots clásicos (`56, 39`, `92, 39`, `128, 39`) y compatibilidad JEI para instalar/extraer módulos, grabar chips Lua y reparar durabilidad.
- **Manos del Jugador en 1ra/3ra Persona + Render 3D en el Tapete Verde (`StationWorkAnimator`, `FirstPersonStationWorkRenderer`, `ModificationStationRenderer`, `MixinItemInHandRenderer`, `MixinHumanoidModel`)**:
  - Al pulsar `Ensamblar`, el servidor ejecuta un ensamblaje de `24` ticks (`1.2s`) con partículas y audio mecánico mientras en primera persona (`FirstPersonStationWorkRenderer`) se muestran **ambas manos del jugador con su skin/mangas** trabajando sobre el tapete verde (sujetando y calibrando las gafas) y en tercera persona/multijugador (`MixinHumanoidModel`) los brazos se extienden hacia la mesa.
  - Añadido `ModificationStationRenderer` (registrado vía `BlockEntityRendererRegistry`) para renderizar en 3D sobre el tapete verde las gafas, módulos y piezas colocadas o en proceso de ensamblaje, sincronizado con `getUpdateTag()`/`getUpdatePacket()`.
- **JEI (`StationAssemblyCategory`, `VisionGogglesJeiPluginCommon`)**:
  - Registrada la categoría `vision_goggles:station_assembly` con los 7 planos y `modification_station` como catalizador.

## [2.0.0] - 2026-11-20

### Fixed & Polished (2026-11-20 — Plan v38: Suavizado Completo de Encendido/Apagado del Visor 1ra Persona)
- **Eliminación de Todos los Cortes al Finalizar el Visor 1P (`GogglesFlipAnimator`, `FirstPersonVisorRenderer`, `VisionRenderer`, `MixinGameRenderer`, `MixinItemInHandRenderer`, `VisionShaderManager`)**:
  - **Sincronización Frame-Exacta vs Tick 20 Hz (`MixinGameRenderer`, `VisionRenderer`, `GogglesFlipAnimator`)**: Añadido `VisionRenderer.syncOpticsForRenderFrame(mc)` invocado justo antes de `renderLevel(...)` y al cierre de `onClientTick()`, junto con `isOpticsEngaged()` (`linearProgress >= 0.85f`) y corrección de `isLocalTransitioning()` por estado (`FLIPPING_DOWN || FLIPPING_UP`) + reinicio limpio de `lastLocalNanos` en el arranque desde reposo (erradicando el salto de `0.05s` en el primer frame y el flash de 1–7 frames sin shader).
  - **Sello Ocular Progresivo y Visor Sin Pops (`FirstPersonVisorRenderer`)**: El visor parte oculto sobre el borde superior (`-0.70H -> -0.46H`), hace zoom hasta sellar los ojos (`scale: 1.05H -> 4.85H`) con rampa emisiva gradual (`0.60 -> 0.88`), funde suavemente hacia negro con un sello ocular (`sealAlpha`) que alcanza `1.0` exactamente al cruzar `0.85` (cambio invisible de shader, luz nocturna, sonido y HUD) y desvanece hacia la vista activa (e inversamente al apagar), dibujando `VisionHUDOverlay` antes que `FirstPersonVisorRenderer` con `gui.flush()` previo y capas Z separadas (`HUD < 1800 Visor < 5500 Sello < 7200 Brazo`). Adicionalmente conectado `ClientPlayerEvent.CLIENT_PLAYER_QUIT` en `VisionRenderer.init()` y lecturas snapshot en `FirstPersonVisorRenderer` para evitar drift intra-frame.
  - **Retracción Completa del Brazo y Retorno Suave de Mano Vanilla (`FirstPersonVisorRenderer`, `MixinItemInHandRenderer`)**: El brazo se retira hasta `handY = 1.32H` fuera del viewport y `MixinItemInHandRenderer` mantiene `mainHandHeight = 0.0f` / `oMainHandHeight = 0.0f` durante la transición para que la mano o ítem vanilla emerjan suavemente desde abajo en lugar de aparecer en un frame.
  - **Encendido Óptico Sin Apagón Estroboscópico (`VisionShaderManager`)**: Sustituido el apagón de `75..165 ms` (`startupMultiplier = 0.0f`) por un decaimiento suave de brillo de encendido (`0.28f -> 0.0f` en NVG).

## [2.0.0] - 2026-11-19

### Changed & Removed (2026-11-19 — Plan v37: Eliminadas Etiquetas `[CRIT]` y `[HOT]` de `06_suite_termica_flir.lua`)
- **Limpieza del Script FLIR (`06_suite_termica_flir.lua`, `LuaModuleManager.java`, `01..05*.lua`)**:
  - Eliminada por completo la Sección 3 de `onRenderHud()` y la recolección/ordenamiento `priorityTargets` en `onTick()` que dibujaban etiquetas 3D flotantes `[CRIT]` y `[HOT]` sobre mobs críticos u hostiles (responsabilidad ya cubierta por `02_visor_ar_objetivos.lua`).
  - Conservados intactos la paleta térmica IFF Ironbow (`#00FFFF`, `#FFBB00`, `#FF8800`), el estabilizador criogénico (`Interference = 0.12`), el censo FLIR (`C: H: P: | F:` filtrado por `e.emits_heat`), el panel superior derecho y la barra vertical Ironbow.
  - Elevado `BUNDLED_VERSION_MARKER` y la cabecera de los 6 scripts incluidos a `-- [VG-BUNDLED v37]` para auto-actualizar las copias en `config/vision_goggles/modules/` (respetando `-- [CUSTOM]` / `-- [USER-CUSTOM]`).
- **Documentación Lua (`00_GUIA_PASO_A_PASO.txt`, `LUA_API_REFERENCE.md`)**:
  - Añadido `thermal_visible` (conservado en `LuaWorldScanner.wrapEntity()` para scripts personalizados) a la referencia de campos de entidad y actualizada la descripción de `06_suite_termica_flir.lua`.

### Fixed (2026-11-19 — Plan v36: Fix X-Ray de Item Frames y Letras en `ThermalMask` + Higiene GL + Filtro `[CRIT]`/`[HOT]`)
- **Eliminado Flush Compartido sobre `maskFbo` (`ThermalEntityPainter.java`)**:
  - Eliminada la llamada `mc.renderBuffers().bufferSource().endBatch()` dentro de `captureAndComposite()` cuando `maskFbo` (sin depth buffer) estaba enlazado, lo que volcaba los lotes pendientes de `ItemFrameRenderer` (`translucentCullBlockSheet`), texto de carteles (`textPolygonOffset` en `lastState`), banners y nametags dentro de `ThermalMask` atravesando paredes sólidas (X-Ray) y dibujando letras fantasmas en pantalla.
- **Filtro Estricto de Texto/Ítems/ArmorStand y Higiene OpenGL (`ThermalEntityPainter.java`, `VisionShaderManager.java`)**:
  - Excluido `ArmorStand` en `emitsThermalHeat()`.
  - Ampliado `shouldDiscardRenderType()` para descartar variantes Forge/mods (`hasRtName && (rtName.contains("text") || rtName.contains("font"))`, `item_`, `item_layered`, `textures/font/`, `font/`, `glyph`) sin volver a colisionar con `"texture["` ni `"outlineproperty="`.
  - Reseteado `maskValidThisFrame = false` al inicio del `try`, añadido `forceActiveTexture` + `forceBindTexture2D` sincronizando `GL13` con `GlStateManager`, migrado `glUseProgram` a `GlStateManager._glUseProgram`, reseteado y restaurado `GL_UNPACK_*` en `ensureEmptyMaskTexture()`, desvinculado `GL_RENDERBUFFER` y `GL_ARRAY_BUFFER`, y cacheado `THERMAL_MASK_SUPPLIER` en `VisionShaderManager`.
- **Oclusión y Límites de Ocular para Etiquetas FLIR (`LuaWorldScanner.java`, `LuaModuleManager.java`, `01..06*.lua`)**:
  - Extraído `ThermalEntityPainter.computeEntityThermalHeat(...)` y expuesto como `e.thermal_visible` en `LuaWorldScanner.wrapEntity()`.
  - Actualizado `06_suite_termica_flir.lua` (marcador `-- [VG-BUNDLED v36]`) para ocultar etiquetas `[CRIT]`/`[HOT]` tras muros sólidos y fuera del área circular útil del ocular térmico (`r <= 0.48`), y elevado `BUNDLED_VERSION_MARKER` a v36 para auto-actualización segura en instancias existentes.

### Fixed & Refactored (2026-11-19 — Plan v35: Canal Dedicado `ThermalMask`, Proyección World-Up y Oclusión Depth)
- **Canal de Máscara Térmica Dedicado (`ThermalEntityPainter.java`, `VisionShaderManager.java`, `thermal.json`, `thermal.fsh`)**:
  - Eliminado el compositing por color-keying directamente sobre `minecraft:main`; introducido doble FBO offscreen (`captureFbo` + `maskFbo`) con textura `ThermalMask` dedicada (y textura fallback 1×1 transparente `(0,0,0,0)` en `emptyMaskTexture`) vinculada directamente a `thermal.fsh` vía `EffectInstance#setSampler("ThermalMask", ThermalEntityPainter::getMaskTextureId)`.
  - Eliminado el umbral frágil `cyanBias > 0.5` sobre el mundo en `thermal.fsh`; reemplazado por lectura directa y limpia de `ThermalMask` con curvas Ironbow suaves por tono normalizado (`normHue`), evitando falsos positivos en bloques cian del entorno, saltos bruscos naranja↔amarillo, saturación plana o contaminación por agua/cristales/partículas. Incorporado `ThermalMask.a` en `sobelEdge` para ENVG Edge Fusion (`u_DSPActive`) y activada la modulación real de `noiseIntensity`.
- **Invariancia de Cámara e Iluminación 3D en Siluetas (`ThermalEntityPainter.AlphaOverrideVertexConsumer`)**:
  - Corregido el deslizamiento de bandas horizontales y el salto de color según el pitch/yaw calculando la altura local mediante proyección world-up en View Space (`localY = dot(vertexView - footView, worldUpView)`), reemplazando los escalones duros por un gradiente continuo `smoothstep` combinado con sombreado 3D Fresnel sobre normales en la sobrecarga de 14 parámetros de `VertexConsumer#vertex`. Elevado el piso alfa a `26/255` (`0.102 > 0.10`) y calibrada la armadura a aislamiento térmico moderado (`~72%`) para evitar huecos negros/púrpura o descartes por cutout.
- **Oclusión Pixel-Perfect por Depth Blit y Higiene OpenGL (`ThermalEntityPainter.java`, `VisionWorldRenderer.java`)**:
  - Separadas entidades detrás de bloques `THERMAL_TRANSPARENT` (`0.11 < heat < 0.99`) de entidades con línea de visión directa (`heat >= 0.99`), aplicando `glBlitFramebuffer(GL_DEPTH_BUFFER_BIT)` con detección automática de formato depth/stencil del `RenderTarget` principal, `GL_POLYGON_OFFSET_FILL` y fallback automático sin spam.
  - Interpolado el AABB de raycast con `partialTick`, filtrado estricto de `RenderType` (`shouldDiscardRenderType` extrayendo el nombre antes de `:` para no colisionar con `"texture["` ni `"outlineproperty="`) descartando nametags, correas, ojos, glints, capas, ítems y sombras, y guardado/restauración íntegra del estado OpenGL (`prevDrawFbo`, `prevReadFbo`, `prevViewport`, `prevProgram`, `prevActiveTex`, `prevTex0`) sin romper `ShaderInstance`.
  - Eliminada clase muerta `ZonedThermalVertexConsumer.java` y activado `enableCull()` con alfa `0.28f` en `VisionWorldRenderer.HOT_BLOCKS`.

### Fixed & Improved (2026-11-19 — Plan v34: Fix Térmico Real + Upgrade DSP Sonar + Rediseño de Scripts `03` y `05`)
- **Fix Real de Visión Térmica y Clasificación FLIR (`ThermalEntityPainter.java`, `06_suite_termica_flir.lua`)**:
  - Eliminado el color frío `"#1144FF"` en `06_suite_termica_flir.lua` que disparaba `coldSignal = 0.85` en `thermal.fsh` y pintaba vacas/ovejas en negro sólido; reemplazada por paleta Ironbow cálida de 3 niveles (`#00FFFF` blanco crítico, `#FFBB00` amarillo-oro hostil, `#FF8800` naranja cálido pasivo).
  - Expuesto `ThermalEntityPainter.emitsThermalHeat(LivingEntity)` como `e.emits_heat` e `is_undead` en `LuaWorldScanner`, filtrando mobs no-muertos/arácnidos/slimes del conteo y etiquetas `[HOT]`/`[CRIT]` en `06`.
  - Corregidos 4 bugs residuales en `ThermalEntityPainter.java`: `dummyBufferSource.endBatch()` reemplazado por `DISCARD_VERTEX_CONSUMER` no-op; clasificación de capas orgánicas no-armadura (`!name.contains("armor")`) para no oscurecer lana de oveja/ropa de aldeano; reafirmación de `fboId` en `renderEntityMask` y compositing sobre `prevDrawFbo` (`bindCompositeTarget`); clamping `Math.max(1, ...)` y `cleanup()` ante fallos de `init()`.
- **Integración Radar 360° con Hardware Sonar y Balance de Drenaje (`01_radar_tactico_360.lua`, `SonarRadarModule.java`, `VisionGogglesItem.java`)**:
  - `01_radar_tactico_360.lua` (`Sonar Pro 360`) ahora requiere `required_module = "sonar"` (`battery_drain = 0.25`) y filtra contactos según `player.getSonarMode()` (`0:ALL`, `1:PLY`, `2:UND`).
  - `SonarRadarModule.shouldRender()` cede el HUD automáticamente cuando `vision_goggles:radar_amenazas` está instalado para evitar dos radares superpuestos.
  - `VisionGogglesItem.serverTick()` alinea el consumo Lua por tick con el `VisionMode` activo.
- **Rediseño Táctico de los Scripts `03` y `05` y Nuevas APIs Java (`LuaWorldScanner.java`, `LuaModuleManager.java`, `LUA_API_REFERENCE.md`, `00_GUIA_PASO_A_PASO.txt`)**:
  - `03_visor_combate_reactivo.lua` (**Visor Combate Gen-3 Auto-Gate**): activa `u_DSPActive = 1.0` en `nvg.fsh` (75% menos grano y Auto-Gating anti-ceguera), añade anillo direccional 360° alrededor de la mira para amenazas y proyectiles entrantes (`world.getProjectilesInRadius`), telemetría de daño recibido y alerta de durabilidad crítica de armadura (`player.getArmorDurabilityPct`), en esquina inferior derecha sin colisiones.
  - `05_escaner_geologico_entorno.lua` (**Prospector Geológico AR 14m**): usa `world.scanBlocks(radius, table, maxResults)` (min-heap acotado sobre búsqueda esférica Java `IdentityHashMap`) cada 15 ticks agrupando vetas contiguas, proyecta rombos AR 3D + flechas laterales, emite Geiger progresivo y alerta lava 4m bajo los pies y 3.5m frente a la mira (con inspector ubicado en `(8, 70)` libre de solapamiento con batería/HUD).
  - Auto-actualización inteligente en `LuaModuleManager.shouldUpgradeBundledScript()` (`-- [VG-BUNDLED v34]`), preservando siempre scripts personalizados o marcados con `-- [CUSTOM]`.

### Fixed (2026-11-19 — Plan v31: Ciclo de Vida de Shaders, Motor Lua y Siluetas Térmicas)
- **Pool Persistente de Shaders (`VisionShaderManager.java`, `VisionRenderer.java`)**:
  - Implementado `SHADER_CACHE` persistente y `prewarmShaders(mc)` para mantener compilados en GPU/RAM los 5 `PostChain` base (`nvg`, `thermal`, `bio`, `hydro`, `hydro_dry`) y `flashlight.json` sin cerrar ni destruir cadenas al apagar gafas o cambiar de modo, evitando re-lecturas de `.jar` reemplazados (`ZipException: invalid stored block lengths`) y tirones.
  - Añadido backoff de 5 segundos (`FAILED_SHADER_TIMESTAMPS`) y caché de redimensionado (`onResize`) para eliminar el bucle infinito de reintento a 20 Hz y el desplome de FPS cuando falla un shader.
  - Unificada la sincronización determinista de shaders por `expectedShader` en `VisionRenderer.onClientTick`, incluyendo soporte completo de luz nocturna y física `Glare`/`Interference` para modos `VISION_MODE` personalizados en Lua.
- **Resolución Estricta de Hardware vs Scripts Lua (`ModularGogglesItem.java`, `LuaClientBridge.java`)**:
  - Reemplazadas las comparaciones por subcadena en `hasModule()` y `removeModule()` por `resolveHardwareVisionMode(String)`, evitando que `vision_goggles:thermal_palette` (`06_suite_termica_flir.lua`) colisione con el `Módulo Térmico` o bloquee su instalación/remoción.
  - Filtrado de ejecución activa en `LuaClientBridge.canModuleRun()` para que scripts vinculados a un `required_module` de visión (`nvg`, `thermal`, `bio`, `hydro`) solo se ejecuten mientras ese `VisionMode` esté activo en las gafas.
- **Siluetas Térmicas Sólidas por Color Bucket e Higiene OpenGL (`ThermalEntityPainter.java`)**:
  - Agrupadas las entidades por color IFF con depth pre-pass + pasada de color/compositing por bucket usando tintado plano (`UseVertexColor = 0.0f`), eliminando las manchas y parches de textura oscura sobre Creepers/Wardens cuando `onThermalColor` está activo.
  - Restaurado `glClearColor` previo e invocado `BufferUploader.invalidate()` tras dibujar el quad de compositing para mantener intacto el caché VAO de Blaze3D.
- **Precisión Decimal en LuaJ y Estado del Lienzo HUD (`LuaScriptEngine.java`, `LuaWorldScanner.java`, `LuaHudCanvas.java`)**:
  - Parcheado `string.format` con `patchStringFormat()` (`Locale.ROOT`) en `LuaScriptEngine` para corregir el bug nativo de LuaJ 3.0.1 que ignoraba `.0f`/`.1f` e imprimía 15 decimales (`25.557666361660257m`), y redondeada `worldToScreen().distance` a 1 decimal.
  - Añadido `g.flush()` y restauración explícita de `RenderSystem.enableBlend()` + `defaultBlendFunc()` en `LuaHudCanvas` (`drawLine`, `drawCircle`, `drawDisc`, `render`).

### Added & Fixed (2026-11-19 — Plan v30: Auditoría del Motor Lua y Nueva Suite de 6 Scripts Tácticos)
- **Correcciones Críticas del Motor de Scripting Lua (`common/scripting/`)**:
  - **Fix Matemático de `world.worldToScreen(x, y, z)` (`LuaWorldScanner.java`)**: Corregida la inversión de ejes Z y X en Minecraft 1.20.1 reemplazando la transformación por cuaternión conjugado (`forward = -pos.z`) por proyección vectorial directa sobre `camera.getLookVector()`, `camera.getUpVector()` y `-camera.getLeftVector()`. Añadido el campo `behind` y suavizado por frame (`living.getPosition(partialTick)`) + `height` en `wrapEntity()` para eliminar el salto de 20 TPS en marcadores de Realidad Aumentada 3D.
  - **Unificación de `getTime()` en Segundos Reales (`LuaClientBridge.java`)**: Corregido `getClientTime()` dividiendo `(gameTime + frameTime) / 20.0` para devolver segundos continuos reales en lugar de ticks, alineando temporizadores y osciladores con la documentación y el fallback de servidor.
  - **Nuevas Funciones y Mejoras de API (`LuaWorldScanner.java`, `LuaHudCanvas.java`)**:
    - Implementados `world.getBlock(x, y, z)` y `world.getLightAt(x, y, z)` (con verificación `isLoaded`).
    - Añadido chequeo de oclusión de bloques por defecto a `world.getTargetEntity(range?, throughWalls?)` y alias `hit.id` en `world.raycastBlock()`.
    - Añadido `hud.getTextWidth(text, scale?)`, elevado `MAX_FRAME_COMMANDS` a `1,000` y optimizado `hud.drawDisc()` a un único batch GPU `VertexFormat.Mode.TRIANGLES` de 32 segmentos.
    - `player.isFlying()` ahora detecta vuelo con Elytras (`isFallFlying()`) y `player.getSpeed()` devuelve velocidad horizontal real en `m/s` (`bloques/s`).
  - **Hardening de Sandbox, Shaders y Diagnóstico (`LuaScriptEngine.java`, `LuaCustomModule.java`, `ThermalEntityPainter.java`, `VisionGogglesItem.java`)**:
    - Anulado `require = nil` en `LuaScriptEngine` para cerrar el acceso indirecto a `PackageLib`.
    - Asignado el namespace por defecto `vision_goggles:` cuando `module.shader` incluye `.json` sin `:`, y añadido logging de errores con rate-limit (5s) en todos los callbacks de `LuaCustomModule`.
    - Migrado el centinela de color personalizado en `ThermalEntityPainter` de `-1` (`int`) a `null` (`Integer`) para permitir el color blanco `"#FFFFFF"` (`0xFFFFFFFF`).
    - Verificado `luaMod.isHardwareInstalled(stack, modular)` antes de aplicar `battery_drain` en `VisionGogglesItem.serverTick()`, e invocado `updateActiveState(false)` al recargar módulos en `LuaModuleManager`.
- **Nueva Suite de 6 Scripts Lua Oficiales (`assets/vision_goggles/lua_examples/`)**:
  - Externalizados los scripts de ejemplo y `00_GUIA_PASO_A_PASO.txt` a recursos del JAR copiándose solo si no existen en `config/vision_goggles/modules/` (sin sobrescribir ediciones del usuario):
    1. `01_radar_tactico_360.lua` (`vision_goggles:radar_amenazas`): Radar circular rotatorio 360° con matriz de rotación 2D, anillos de rango, barrido animado, marcador Norte orbital, indicador de desnivel vertical y sonar acústico progresivo.
    2. `02_visor_ar_objetivos.lua` (`vision_goggles:threat_scanner`): Realidad Aumentada 3D con corchetes `[  ]` proyectados vía `world.worldToScreen`, chevrones laterales fuera de campo (`<`/`>`) y tarjeta IFF de fijación en mira.
    3. `03_visor_combate_reactivo.lua` (`vision_goggles:cyber_visor`): Procesador óptico GLSL reactivo (`colorFilter`, `Glare`) con adaptación cromática suave por entorno (Agua, Nether, End, Cueva P45), onda de choque por daño, pulso cardíaco doble y osciloscopio ECG en vivo.
    4. `04_hud_piloto_avionica.lua` (`vision_goggles:tactical_hud`): HUD de aviónica con cinta de brújula deslizante (`000°=Norte`), escalera de cabeceo en vuelo, velocímetro `m/s`, variómetro `VSI` (`dy/dt`), alarma GPWS `¡PULL UP!`, reloj solar 24h, clima y efectos de poción.
    5. `05_escaner_geologico_entorno.lua` (`vision_goggles:geo_scanner`): Escáner subterráneo por *time-slicing* (`9×7×9 = 567` bloques repartidos en 7 ticks), marcadores AR 3D sobre menas valiosas, alerta de lava bajo los pies y analizador de bloque en mira.
    6. `06_suite_termica_flir.lua` (`vision_goggles:thermal_palette`): Clasificador térmico IFF calibrado para la rampa Ironbow de `thermal.fsh`, estabilizador criogénico de `Interference` en el Nether, barra FLIR de 6 bandas, temperatura `°C` estimada y etiquetas térmicas 3D.
  - Actualizado `LUA_API_REFERENCE.md` al 100% de paridad con los bindings Java, incluyendo Plantilla Limpia (Sección 11), límites del sandbox Lua 5.2 (Sección 12) y catálogo completo de ejemplos.

### Fixed (2026-11-19)
- **Curios Battery Recharge Target Resolution & Feedback (`ModEvents`, `BatteryItem`, `BatteryPacket`, `BatterySyncPacket`, `ModularGogglesItem`)**: Fixed right-clicking with a battery while wearing both Vision Goggles and Tactical Flashlight in Curios slots failing to detect the Curios flashlight, silently consuming the battery on the goggles' `0.001f/tick` standby drain, and displaying no feedback message. `ModEvents.RIGHT_CLICK_ITEM` no longer hijacks `BatteryItem` before `BatteryItem.use()`, `RechargeTargetInfo.canCharge()` now enforces a `0.5%` standby-drain tolerance so full goggles (`>= 99.5%`) do not eat batteries, all recharge paths (right-click battery, extra configured battery items, and Tactical Radial Menu) now delegate to server-authoritative `BatteryPacket.rechargeWithStack` with ActionBar feedback (`message.vision_goggles.recharged_named`), and `BatterySyncPacket` now synchronizes both Curios goggles and Curios flashlight battery levels immediately to the client.

## [2.0.0] - 2026-11-14

### Fixed (2026-11-19)
- **Handheld Flashlight Aiming & Handedness (VisionRenderer)**: Fixed inverted left/right lateral basis (headRot/odyRot maps +X to the player's left and -X to the right in Minecraft's right-handed world space) for MAIN_HAND and OFF_HAND beams in both first-person and third-person (computeArmHandWorldPos), and added player.getMainArm() support for left-handed players in first-person while keeping Curios chest (TORSO_CHEST) and goggle (HEAD_GOGGLES) origins untouched.

### Added (2026-11-14)
- **Pure Client-Side Lightmap Night Vision (`MixinLivingEntity` & `MixinGameRenderer`)**: Replaced server-side `MobEffects.NIGHT_VISION` potion application in `VisionGogglesItem` with a virtual client-side `LivingEntity#hasEffect` + `GameRenderer#getNightVisionScale` override (`VisionRenderer.shouldApplyNightVisionLight()`). Eliminates the potion icon/particles, prevents server-client desync when swapping between active and inactive goggles, and preserves real vanilla Night Vision potions.
- **Third-Person Tactical Hand Animation (`MixinHumanoidModel`)**: Added smooth main-arm reach animation toward the helmet brow/mount during flip-down and flip-up transitions in 3rd person (automatically mirrored to sleeves via `PlayerModel.setupAnim`).
- **Mechanical Flip-Down / Flip-Up Toggle Sequence (`GogglesFlipAnimator`)**: Turning on Vision Goggles now runs a smooth ~280ms flip-down transition (`UP -> FLIPPING_DOWN -> DOWN_ACTIVE -> FLIPPING_UP`) with an immediate mechanical hinge sound (`ARMOR_EQUIP_IRON`). The shader, HUD, lens glow, night-vision scale, and cathode hum wait until the visor finishes flipping down before activating; turning off immediately disables the shader/HUD/hum and flips the visor back up.
- **First-Person 3D Animated Visor & Player Hand (`FirstPersonVisorModel` & `FirstPersonVisorRenderer`)**: Added a dedicated 3D tactical visor model and the player's actual skin arm/sleeve (`wide` & `slim`, respecting `mainArm`) reaching in during a 420ms flip sequence to grip the side of the goggles, pull them down over the eyes, and retract (reversing smoothly on toggle off), supporting all goggle variants (NVG, Thermal, Hydro, Bio, Modular, Pro Modular) and their emissive lens glow.
- **Third-Person Smooth Visor Interpolation (`GogglesRenderLayer`)**: Replaced instant visor snapping with per-UUID smooth `Mth.lerp` interpolation on the `visor` bone, gating `RenderType.eyes(glowTex)` until `progress >= 0.98f`.
- **GeckoLib Item Flip Animations (`animations/item/vision_goggles.animation.json`)**: Added `flip_down`, `flip_up`, `idle_down`, and `idle_up` GeckoLib animations on the `visor` bone and connected `VisionGogglesGeoModel` and `VisionGogglesItem` controllers to animate held/dropped goggle items according to `TAG_ACTIVE`.

### Fixed (2026-11-14)
- **First-Person Visor & Hand Orientation (`FirstPersonVisorRenderer` & `MixinItemInHandRenderer`)**: Corrected arm rotation (`+142°`) and local-axis offset so the fist grips the side of the goggles while the shoulder stays off-screen bottom-right, added eye-level approach zoom (`0.95x -> 1.85x`) at the end of the flip-down, and hid the duplicate vanilla first-person hand during transitions.
- **First-Person Visor Transition Visibility**: Moved `FirstPersonVisorRenderer.render(g, partialTicks)` to the top of `VisionRenderer.renderHUD` before the `if (!visorActive) return;` early exit so the 3D visor renders smoothly throughout both flip-down and flip-up transitions.

### Fixed (2026-09-16)
- **JPMS Split-Package Crash with Figura / Superb Warfare**: Relocated shaded `org.luaj` classes into `dev.itsrealperson.vision_goggles.shadow.org.luaj` via `shadowJar` and excluded `META-INF/services/**` (javax.script.ScriptEngineFactory) in both Forge and Fabric builds. Resolves `ResolutionException: Modules luaj.jse.figura and vision_goggles export package org.luaj.vm2.script`.
- **Chest Flashlight Orientation**: Fixed front facing direction and upright orientation on the player torso layer in `GogglesRenderLayer`.
- **Station Model Rotation**: Fixed invalid `-20.0` degree rotation on `screen_display` to `-22.5` to prevent `JsonParseException` missing texture block.
- **Smart Battery Recharging**: Implemented smart target resolution prioritizing held items and automatically choosing the most depleted equipped wearable.
- **Item Textures**: Updated electrical parts sprite to a 16x16 Minecraft-style handful of electronic components.
- **Flashlight Mode Coherent Beam & Static**: Optical geometry (hotspot, spill, cutoff) now adapts per mode. Volumetric static/dust is strictly bound to photons inside the illuminated cone, eliminating the oversized noise circle in the pitch-black sky.
- **Third-Person Flip-Up / Flip-Down Posing**: Goggles physically flip up onto the forehead/helmet when inactive, and deploy over the eyes when turned on.
- **Dynamic Emissive Optical Lenses**: Goggle lenses in 3rd person only emit glowing phosphor light when active.
- **Analog Power-Up Sequence**: Realistic 650ms phosphor overvoltage flash, micro-strobe blackout, and warm-up ramp in shaders upon activation.
- **Tactical Audio Immersion**: Added heavy Wilcox mount mechanical clack sounds on toggle and balanced subtle cathode tube ambient hum.
- **Continuous Smooth Zoom (1.0x - 8.0x)**: Holding the Zoom key (`V`) and scrolling the mouse wheel now smoothly scales optical magnification, overriding hotbar scrolling.
- **Tactical Rangefinder Telemetry HUD**: Displays holographic targeting brackets, active zoom factor (`[ 4.0x ]`), target entity tags (`< ZOMBIE >`), and live block/meter distance calculations up to 128 blocks.
- **Unified Key 'N' Tactical Radial Menu**: Holding 'N' (> 220ms) opens a 360-degree tactical dial for instant selection of vision modes (NVG, Thermal, Bio, Hydro), flashlight, battery recharge, and standby. Quick tap (< 220ms) retains instantaneous NVG toggle.
- **Direct Mode Selection Network Packet**: `SelectVisionModePacket` synchronizes client radial dial selections cleanly with server-side NBT state.
- **Tactical Radial Menu Keyhold Fix**: Query physical GLFW key state directly to keep the radial dial open smoothly while 'N' is held, executing on release or click.
- **Pure Optical Magnification**: Removed Rangefinder telemetry brackets and entity tracking from Zoom mode to maintain a clean optic view.
- **Fixed Inactive Goggle Posing**: Restored native Blockbench visor alignment when powered off; optical lenses stay dark without unnatural deformation.
- **Smooth Flashlight Beam & Extended Reach**: Replaced sparkling high-frequency hash noise with subtle smooth atmospheric haze (eliminating floating white static dots) and increased beam reach (Focused: 52m, Wide: 32m, Close: 18m).
- **Chunk Viewer Module Removal**: Removed `chunk_viewer_module` completely from registries (`ModItems`, `ModTabs`), GUI slot filters (`ModificationStationBlockEntity`), world renderer (`VisionWorldRenderer`), recipes, and 12 language files. This eliminates the purple/black checkerboard missing texture in the creative menu.
- **Item Sprite Centering**: Horizontally centered all 13 module sprites (`_v2.png`), battery item sprites, and tactical flashlight sprites in their 16x16 frames (shifting them +1px to +4px to balance left and right transparent padding).
- **Argon Art Direction & 16x16 Texture Prototypes**: Consulted Gemini 4 Argon (`gemini-4-argon-high`) for a comprehensive pixel art overhaul. Generated 16 centered prototype textures in `docs/prototipos_texturas/` (12px visible width, exactly 2px left/right margin) for user review without modifying `installation_script` or `modification_station`.
- **Argon Texture Suite Deployment**: Applied the 16x16 centered Argon prototype textures across all 13 module items (updating both `_v2.png` and base textures), `batteries_v2.png`, `batteries.png`, and `electrical_parts.png`. Preserved the 32x32 Blockbench UV layout for `tactical_flashlight.png` and left `installation_script` and `modification_station` untouched. Recompiled and packaged new JARs for Forge and Fabric.
- **Dynamic Tactical Radial Menu**: Refactored `TacticalRadialScreen` so it dynamically populates only the vision modes that are physically installed and supported (eliminating ghost options like NVG on Thermal Goggles). Automatically hides the Flashlight slice if no flashlight or flashlight module is equipped, and adapts circle slice angles dynamically.
- **Corrected Thermal Goggles Mode Registration**: Removed secondary `NIGHT_VISION` registration from `THERMAL_GOGGLES` constructor in `ModItems.java`, ensuring Thermal Goggles only provide thermal vision.
- **Fast Battery Recharge Overhaul**: Upgraded `BatteryPacket` to search the player's full inventory (main inventory, hotbar, and offhand) instead of only hand items. Added rich action bar feedback (`§a[+] Batería Recargada: ... (XX%)`, `§c[!] No tienes baterías`, `§e[i] Ya está al 100%`) with mechanical battery reload audio (`ARMOR_EQUIP_IRON` + `BLIP`). Registered key `B` (`rechargeKey`) in `ModKeyMappings` with full translation support across 12 languages.
- **Flashlight Equipment Action Guard**: Added `hasFlashlightAvailable` checks in `VisionRenderer` and packets (`ToggleFlashlightPacket`, `CycleFlashlightModePacket`) to completely block audio, state toggles, and networking if the player does not have a flashlight equipped or held.
- **Dynamic Wilcox Flip-Up Posing**: Anchored Wilcox mount pieces (`mount_lower`, `mount_upper`) to `head_bone` so the shroud stays permanently bolted to the helmet. Visor rotates up smoothly onto the helmet brow (`xRot = -1.10F` ~ -63°) with dark lenses when powered off, and flips down over the eyes with emissive glowing lenses when turned on.
- **Dynamic Radial Menu Integration for Utility Modules**: Exposed `SPAWN_SECURITY` (Radar de Spawn), `SONAR` (Pulso Acústico), and `ZOOM` (Aumento Óptico) directly onto the Tactical Radial Menu (`N`). They only appear on the wheel if physically installed on the equipped goggles.
- **Physical Key Guards & Telemetry**: Keys `O` (Spawn Security) and `P` (Sonar) now strictly verify whether the module is physically installed on the goggles before triggering client toggles or sending network packets, providing action bar feedback (`§6[Radar de Spawn]`, `message.vision_goggles.sonar_mode`) and tactile audio feedback (`VISOR_ON`/`VISOR_OFF`).
- **Server-Side Sonar Packet Guard**: `ToggleSonarPacket` validates on the server that the helmet actually contains the Sonar module and forces `player.inventoryMenu.broadcastChanges()` for instantaneous client-side NBT state synchronization.
- **Tactical Zoom Lock**: Added Zoom Lock toggle option in the radial menu (`2.5x` magnification) while preserving analog continuous scroll-zoom when holding `V`.
- **Dynamic Radial Spacing & Framed Hub**: Enhanced `TacticalRadialScreen` with dynamic radius expansion (`86px` when options > 5) and adaptive center hub telemetry framing for seamless tactical readability.
- **Recharge Keybind Removal**: Removed dedicated 'B' physical key mapping (`rechargeKey`) from `ModKeyMappings` and `VisionRenderer`, making quick battery recharge an exclusive and streamlined tactical action inside the Radial Menu (`N`). Cleaned up translation keys across all 12 languages.
- **Keybind Cleanup (M, O, P Removal)**: Removed dedicated physical key mappings for `switchModeKey` ('M'), `toggleSpawnSecurityKey` ('O'), and `switchSonarModeKey` ('P') from `ModKeyMappings`, `VisionRenderer`, and `VisionWorldRenderer`. Their functions are now exclusively consolidated in the Tactical Radial Menu (`N`), leaving only essential controls (`N`, `V`, `L`, `K`) and eliminating keyboard conflicts.
- **AAA Tactical Radial Menu Overhaul (Alpha 2.0.0)**: Completely redesigned `TacticalRadialScreen` from flat rectangular buttons into an authentic military HUD circular dial. Features real circular arc sectors (tessellated donut wedges with 3° angular padding and radial phosphor gradients), integrated 16x16 module and equipment icons with 1.15x hover scaling, an outer compass ring with 10° degree ticks and 90° cardinal notches, a center crosshair reticle, dynamic telemetry telemetry hub, and official Alpha build branding (`VG-OS // v2.0-ALPHA`).
- **Zero-Collision Cartesian Stacking & Tactical Module Sockets (Alpha 2.0.0)**: Fixed the critical overlap where horizontal wedge labels (`LINTERNA`, `STANDBY`) rendered directly on top of item icons. Refactored the positioning math from collinear polar rays to vertically stacked Cartesian anchors (Tactical Socket at $y - 6\text{px}$, Title Label at $y + 7\text{px}$), ensuring a guaranteed positive vertical separation at all angles. Introduced `drawTacticalSocket` military chassis bezels with tinted ballistic glass, beveled frames, and glowing corner reticle brackets for a physically mounted aesthetic. Replaced the dark 3D helmet model with the clean `ModItems.MODULAR_GOGGLES` item sprite for Standby, and added an automatic font scale guard for tight wedge arcs.
- **Full 12-Language i18n Synchronization (Alpha 2.0.0)**: Migrated 100% of hardcoded strings and UI text across the entire codebase to `Component.translatable` keys. Synchronized all 12 supported languages (`en_us`, `en_gb`, `es_es`, `es_mx`, `es_ar`, `es_cl`, `de_de`, `fr_fr`, `it_it`, `pl_pl`, `pt_br`, `ru_ru`) to exactly 161 keys each with zero missing or untranslated entries. Localized all Tactical Radial Menu wedges, Alpha build telemetry headers, center hub standby/ready telemetry, dynamic battery recharge messages with device resolution, zoom lock and spawn radar action bar alerts, sonar filters (`All`, `Players`, `Undead`), tactical flashlight mode tooltips and controls, and programmable firmware chip descriptors.
- **Tactical Engineering Manufacturing System (TEMS) & Recipe Overhaul (Alpha 2.0.0)**:
  - **6 Dedicated Intermediate Tactical Manufacturing Items**: Implemented physical intermediate components to establish 100% anatomical and mechanical realism in high-tech optic assembly:
    - `optical_lens` (`Precision Optical Lens`): Aspherical coated convex glass mounted in a machined gold alloy ring with amethyst chromatic dispersion filters (`tinted_glass`, `amethyst_shard`, `gold_ingot`).
    - `tactical_frame` (`Tactical Chassis Frame`): Ballistic facial alloy chassis with leather headstraps, copper pivot rivets, and an integrated Wilcox brow bridge (`iron_ingot`, `copper_ingot`, `leather`).
    - `flashlight_body` (`Machined Flashlight Body`): Mil-spec CNC-milled cylindrical aluminum tube with diamond knurling grip, sealed battery chamber, and tailcap click switch (`iron_ingot`, `leather`, `electrical_parts`).
    - `tactical_processor` (`Tactical DSP Processor`): Military signal processor on a diamond silicon die with redstone logic gates and peripheral gold BGA contacts, conceptually powering shader DSP matrices (`diamond`, `gold_ingot`, `redstone`, `electrical_parts`).
    - `thermal_core` (`Thermal Sensor Core`): Cryogenic long-wave infrared microbolometer sensor pod with gold thermal dissipation shielding and Nether-forged pyroelectric crystal (`blaze_rod`, `magma_cream`, `quartz`, `gold_ingot`).
    - `lifesign_sensor` (`Biometric Lifesign Sensor`): Bio-impedance spectral sensor featuring resonant amethyst focusing prisms and an ocular vital pulse core (`amethyst_shard`, `ghast_tear`, `ender_eye`, `electrical_parts`).
  - **Full Physical Assembly Recipes**:
    - `flashlight_module`: Handcrafted as an optical projector head (`optical_lens` + `sea_lantern` + `copper_ingot` + `electrical_parts`).
    - `tactical_flashlight`: True physical assembly combining the projector head (`flashlight_module`), machined body tube (`flashlight_body`), and power cell (`nvg_battery`).
    - `modular_goggles` (2 sockets): Rebalanced from RNG Enchanted Golden Apple into an earned, high-value tech headset (`optical_lens` x2, `tactical_processor`, `tactical_frame` x2, `nvg_battery`, `electrical_parts`, `gold_ingot`).
    - `pro_modular_goggles` (4 sockets): Elite military upgrade preserving all installed module NBT via `GogglesUpgradeRecipe` (`tactical_processor` x2, `netherite_ingot`, `modular_goggles`, `echo_shard` x2, `battery_expansion_module`).
    - Dedicated Modules (`thermal_module`, `bio_module`, `zoom_module`, `sonar_module`) and Goggles (`thermal_goggles`, `biometric_goggles`): Completely migrated to utilize corresponding physical sensor cores and tactical frames instead of mismatched boss drops.
    - **100% Mod-Wide Recipe Unification**: Extended TEMS architecture across all remaining wearable goggles and modules:
      - `night_vision_goggles` & `hydro_goggles`: Completely unified to assemble from `tactical_frame` + respective module (`nvg_module`, `hydro_module`) + `nvg_battery`.
      - `nvg_module` & `hydro_module`: Now require `optical_lens` at the objective head + copper heat conductors + biological sensory core (`golden_carrot`, `pufferfish`) + `electrical_parts`.
      - `spawn_security_module`: Equipped with `optical_lens` wide-angle scanner + redstone torch light photometric sensors + daylight detector + `electrical_parts`.
      - `environment_module`: Reconstructed with copper enclosure + amethyst geomagnetic orientation crystal + compass + `electrical_parts`.
      - `vital_info_module`: Precision health telemetric probe using amethyst resonance prism + glistering melon slice + copper bus + `electrical_parts`.
      - `solar_module`: Built with daylight collector + quartz photovoltaic die + gold bus + `electrical_parts`.
      - `battery_expansion_module`: Heavy-duty dual battery bank encased with copper heat dissipation brackets and gold bridge contacts (`nvg_battery` x2 + `copper_ingot` x4 + `gold_ingot` + `electrical_parts` x2).
      - `programmable_module`: Microchip EEPROM package using quartz flash die + redstone I/O bus + gold contacts + `electrical_parts`.
  - **Complete Assets & 12-Language Translations**: Created 6 custom 16x16 pixel art textures matching vanilla and Argon palette aesthetics, item models, creative tab integration, and synchronized translations across all 12 supported languages (`en_us`, `en_gb`, `es_es`, `es_mx`, `es_ar`, `es_cl`, `de_de`, `fr_fr`, `it_it`, `pl_pl`, `pt_br`, `ru_ru`).
- **Official JEI (Just Enough Items) Integration for Modification Station (Alpha 2.0.0)**:
  - **Modification Station Recipe Category (`ModificationStationRecipeCategory`)**: Created a dedicated JEI recipe category rendering directly from the workbench GUI texture (`textures/gui/modification_station.png`, `96x28`), displaying the 3 interaction slots, operational `+` sign, and progression arrow `->`.
  - **Dynamic NBT Output Preview**: Generated synthetic recipes for all 12 hardware modules across both `modular_goggles` (2 slots) and `pro_modular_goggles` (4 slots). The JEI recipe preview populates exact NBT tags (`modules: ["..."]`), triggering `ModularGogglesItem.appendHoverText()` so hovering over the output stack in JEI shows the live installed modules list, active abilities, and expanded battery capacity.
  - **Lua Script Flashing & Firmware Installation Recipes**: Exposed the flashing pipeline in JEI: `InstallationScriptItem` + blank `ProgrammableModuleItem` $\rightarrow$ programmed module, plus installing the programmed script module into modular goggles.
  - **Interactive Recipe Click Area & Catalyst**: Registered `ModBlocks.MODIFICATION_STATION` as the official JEI category catalyst and bound the workbench GUI progress arrow (`x=98, y=36, 16x14`) in `ModificationStationScreen` so clicking it in-game instantly opens the JEI recipe catalog.
  - **Zero-Crash Optional Runtime (`modCompileOnly`)**: Wired with `mezz.jei:jei-1.20.1-*-api:15.20.0.106` across `common`, `forge`, and `fabric`. If a player launches without JEI installed, the mod runs with zero overhead and zero missing class exceptions.
  - **Comprehensive Visual Redesign of 6 TEMS Intermediate Items (Vanilla+ 16x16)**:
  - Completely redesigned all 6 physical manufacturing components to establish authentic tactical, mechanical, and electronic identity matching Minecraft vanilla aesthetics and Argon art standards:
    - `optical_lens`: 12x12 centered circular collimator objective with threaded brass bezel (`#D4AF37`), sapphire anti-reflective glass, and diagonal specular shine.
    - `tactical_frame`: Front-facing tactical goggle chassis with dual hollow hexagonal eye-ports, anatomical bridge, Wilcox brow mount, and ballistic straps with steel buckles.
    - `flashlight_body`: Robust 4-5px thick cylindrical aluminum body with diamond knurling grip, steel pocket clip, threaded brass neck, and burgundy tactical tailcap switch.
    - `tactical_processor`: 12x12 square DSP chip with dark slate ceramic package (`#2D333D`), gold Pin 1 index notch, nickel-plated IHS heat spreader, and cyan/cobalt silicon die matrix.
    - `thermal_core`: Cryogenic FLIR microbolometer pod with hermetic gold casing, dual copper heat dissipation fins, germanium crimson lens, and incandescent focal core.
    - `lifesign_sensor`: 12x14 biometric telemetry module with top bio-impedance contacts, coral red pulse LED, and high-contrast OLED screen showing an active ECG vital trace.
  - Multi-loader compiled and cleanly packaged into production JARs for Forge (`vision-goggles-forge-2.0.0.jar`) and Fabric (`vision-goggles-fabric-2.0.0.jar`).
- **Suite de Mejoras Tácticas Integrales (Arquitectura Argon // Build 2.0.0-Alpha)**:
  - **Óptica y Shaders Realistas (Activación de Uniforms Dormidos)**:
    - **Conexión del Uniform `focus`**: Enlazado el factor de zoom dinámico (`VisionRenderer.getCurrentZoomFactor()`) con la variable `uniform float focus;` en `VisionShaderManager.java`. Esto activa automáticamente en GLSL la aberración cromática perimetral en NVG, la densificación de retículo mil-dot en Biométrico, y el pixelado digital térmico al superar 4x de magnificación.
    - **Deslumbramiento Vectorial (Look-At Blinding)**: Reemplazada la comprobación estática por un cálculo vectorial dinámico. Mirar directamente hacia el sol (`sunDot > 0.60`) o a fuentes incandescentes a quemarropa (lava, fuego, fogatas a menos de 4 bloques) provoca ceguera fotónica real con sobrecarga del sensor.
    - **Persistencia de Fósforo P43 (Burn-In Buffer)**: Simulación analógica de tubos intensificadores de imagen: al sufrir sobrecarga lumínica (`Glare > 0.35f`), el decaimiento se ralentiza gradualmente para imitar la retención de fósforo real.
    - **Destello por Explosiones y Rayos**: Daños recibidos por detonaciones (`DamageTypeTags.IS_EXPLOSION`) o impactos de rayo (`DamageTypeTags.IS_LIGHTNING`) disparan un fogonazo de ceguera instantánea (`triggerFlash(1.8f)`).
    - **Paridad de Niebla Hydro en Forge**: Trasladado `MixinFogRenderer` al paquete común (`vision_goggles.mixins.json`), eliminando la niebla densa de agua tanto en Fabric como en Forge.
  - **Estación de Modificación 2.0 (Mantenimiento y Extracción)**:
    - **Desensamblaje y Extracción de Módulos**: Ahora es posible extraer módulos instalados colocando las gafas en el Slot 0 con el Slot 1 vacío. El Slot 2 muestra el módulo instalado (incluyendo chips Lua grabados con sus scripts intactos). Al retirarlo, las gafas conservan su batería y sockets liberados sin destruirse.
    - **Taller de Reparación de Campo**: Reparación de durabilidad sin costo de XP ni penalizaciones de yunque combinando las gafas dañadas con `tactical_frame` (reparación completa al 100%) o `electrical_parts` (mantenimiento de campo al +35%).
    - **Catálogo JEI Actualizado**: Integradas las recetas de extracción de módulos y reparación táctica en la categoría de JEI de la Estación de Modificación.
  - **Linterna Táctica Operativa (Modo Estroboscópico)**:
    - **4.º Modo: `STROBE` (13 Hz)**: Añadido modo estroboscópico táctico en `FlashlightMode` y `flashlight.fsh`, modulando la intensidad a alta frecuencia disuasoria.
    - **Efecto de Disuasión Táctica (`FlashlightDeterrence`)**: Enemigos hostiles que miren de frente al haz estroboscópico a menos de 24 bloques sufren desorientación (*Slowness I*) y los Creepers tienen su cuenta regresiva de detonación suprimida.
  - **Firmas Térmicas Ambientales Ampliadas**:
    - **Detección de Combustión**: En `thermal.fsh`, fuentes de calor activo como hornos, ahumadores encendidos, fogatas, magma y lava son detectadas por cromatismo cálido e intensidad lumínica, proyectando auras incandescentes blanco/naranja en la paleta Ironbow.
    - **Sumideros Criogénicos**: Bloques de frío extremo (hielo, hielo compacto, hielo azul y nieve en polvo) absorben calor en el shader, renderizándose en índigo/negro profundo.
  - **Realidad Aumentada Lua (`world.worldToScreen`)**:
    - Implementada proyección matemática de cámara 3D a coordenadas 2D de pantalla (`worldToScreen(x, y, z)`) en `LuaWorldScanner.java`, permitiendo a los scripts crear retículas de seguimiento AR, waypoints y corchetes dinámicos sobre entidades en pantalla.
    - Ampliado `wrapEntity` con coordenadas `eye_y`, `pitch` y `yaw`.
- **Hotfix de Estabilización y Corrección Crítica (Build 2.0.0-Alpha.1 // Plan v19)**:
  - **Corrección de Crash en Visión Térmica (`Pose stack not empty`)**:
    - Aislamiento completo de la pila de transformaciones en `ThermalEntityPainter.java`. Ahora se instancia un `PoseStack` local para cada entidad proyectada con copia exacta de la matriz de vista, eliminando cualquier mutación sobre el `PoseStack` global de `LevelRenderer`.
    - Envoltorio de renderizado de entidades en bloques `try ... finally` para garantizar que `endBatch()` y la limpieza matricial se ejecuten sin excepción, solucionando el crash `IllegalStateException: Pose stack not empty` en Forge.
  - **Activación del Modo Estroboscópico de la Linterna**:
    - Reemplazado el cálculo estático de conmutación `(currentMode + 1) % 3` por `FlashlightMode.byId(currentMode).next().id` tanto en `TacticalFlashlightItem.java` (Shift + Clic Derecho) como en `CycleFlashlightModePacket.java` (tecla 'K').
    - El 4.º modo (`STROBE`) ahora es 100% accesible e interactivo.
  - **Corrección de Deslumbramiento Falso en NVG con Linterna Apagada**:
    - Actualizado `VisionShaderManager.isHoldingLight()` para verificar formalmente `TacticalFlashlightItem.isActive(stack)`.
    - Extraer una linterna desde el inventario creativo o sostener una linterna apagada ya no activa el destello blanco de sobrecarga en gafas de visión nocturna.
  - **Corrección de Ranuras y Salida en Estación de Modificación**:
    - Refactorizado `ModificationStationBlockEntity.updateResult()`: eliminada la guarda residual `if (!outputStack.isEmpty()) return;` que dejaba congelada la ranura de salida con el módulo de desmontaje.
    - Ahora, al insertar cualquier ítem en la Ranura 1, la salida se limpia y recalcula de forma inmediata, permitiendo instalar nuevos módulos, reparar gafas o colocar ítems sin conflicto con la mecánica de extracción.
- **Oclusión Solar, Fogonazo de TNT y Ergonomía de Linterna (Build 2.0.0-Alpha.2 // Plan v20)**:
  - **Oclusión Física del Sol por Raycast (`ClipContext.Block.VISUAL`)**:
    - Implementado trazado de rayos óptico en `VisionShaderManager.java` desde los ojos del jugador hacia el sol a una distancia de 128 bloques.
    - Si la línea de visión al sol está obstruida por bloques sólidos, techos, cuevas, muros o árboles (`HitResult.Type.BLOCK`), el fogonazo de ceguera solar directa se anula al 100%. Solo se produce deslumbramiento cuando el astro está en el campo visual libre y sin obstáculos.
    - Calibrada la luminancia ambiental diurna difusa a un valor natural (`0.30f`), evitando sobrecargas extremas bajo cielo abierto.
  - **Fogonazo Fotónico por Explosiones (TNT / Creepers / Rayos)**:
    - Creado `MixinClientPacketListener.java` en `common` para capturar eventos de red de explosiones (`ClientboundExplodePacket`).
    - Al detonar TNT, Creepers, cristales del End o bolas de fuego a menos de 40 bloques del jugador, se calcula la distancia y se detona un fogonazo reactivo en las gafas NVG (`VisionShaderManager.triggerFlash`) escalado con la proximidad.
    - Detectada la caída de rayos (`EntityType.LIGHTNING_BOLT`) a menos de 64 bloques, activando el fogonazo correspondiente.
    - Activado el parpadeo de daño (`damageFlicker`) en el cliente sincronizado con `mc.player.hurtTime == 9`.
  - **Eliminación del Bobbing de la Mano al Usar Linterna (`CustomReequipItem`)**:
    - Creada la interfaz `CustomReequipItem` implementada en `TacticalFlashlightItem` y `VisionGogglesItem`.
    - En Forge, aprovecha el hook nativo `shouldCauseReequipAnimation(...)` devolviendo `false` cuando el ítem se mantiene en la misma ranura.
    - En Fabric, se implementó `ItemInHandRendererMixin` para suprimir la animación de guardado/sacado ante modificaciones de tags NBT de batería y estado activo.
    - La mano en primera persona se mantiene firme y fija al encender, apagar, cambiar de modo o agotar batería.
  - **Remasterización Ergonómica del Modo Estroboscópico (`flashlight.fsh`)**:
    - Implementada arquitectura de iluminación de simulación táctica: **Luz Base Ambiental + Pulsos de Sobretensión**.
    - La linterna conserva un 35% de luz base continua para evitar cortes bruscos a oscuridad total y fatiga ocular / mareo.
    - Pulsos dinámicos a ~9.5 Hz con caída suave y cono táctico intermedio, conservando la disuasión completa contra mobs (`FlashlightDeterrence`).
- **Pulido Integral Pre-Lanzamiento y Estabilización Universal (Build 2.0.0-ReleaseCandidate // Plan v21)**:
  - **Estabilidad Total en Servidores Dedicados Multijugador (Zero-Crash Headless)**:
    - Desacoplado el cargador de pantallas de cliente en `OpenLuaScreenPacket.java` delegando en `ClientGuiHelper.java` únicamente bajo entornos de cliente (`EnvType.CLIENT`).
    - Aislado el registro de pantallas de configuración de Forge (`ConfigScreenHandler`) en `Vision_gogglesForgeClient.java` protegido contra la carga de clases en entornos de servidor dedicado.
    - Desacoplado el motor Lua de clases de renderizado (`LuaClientBridge.java`), permitiendo la ejecución segura de comandos y lógica de servidor sin referencias a `Minecraft.getInstance()`.
  - **Eliminación Definitiva de Exploits de Duplicación en Supervivencia**:
    - `ModificationStationBlock.onRemove`: Al destruir la estación de modificación, se vacía y anula el slot 2 (previsualización de resultado interactivo) antes de soltar el contenido al mundo con `Containers.dropContents`. Solo se sueltan los ítems reales de las ranuras 0 y 1.
    - `ModificationStationBlockEntity`: Ocultado el slot 2 de la interfaz `WorldlyContainer` (`getSlotsForFace` solo expone ranuras 0 y 1) y configurado `canTakeItemThroughFace(2) = false`. Las tolvas y conductos de automatización ya no pueden extraer resultados sin consumir ingredientes.
    - `ModificationStationMenu`: Validación atómica en shift-click (`quickMoveStack`) vaciando el slot antes de ejecutar `onTake`.
  - **Sandbox Estricto y Watchdog de CPU en Motor Lua**:
    - Implementado `SandboxedDebugLib.java` con límite de 100.000 instrucciones por llamada, protegiendo al juego de congelamientos por bucles infinitos (`while true do`).
    - Eliminadas librerías peligrosas de `globals`: `luajava` (acceso a JVM/Runtime), `io`, `package`, `loadfile` y `dofile`.
    - Sanetizado el módulo `os`, restringiéndolo exclusivamente a llamadas seguras de reloj y tiempo (`clock`, `time`, `difftime`, `date`).
    - Limitada la cola gráfica de `LuaHudCanvas.java` a un máximo de 500 comandos por frame para evitar sobrecargas de memoria o DoS visual.
  - **Paridad y Correcciones Críticas en Fabric**:
    - En `fabric/PlatformMethodsImpl.java`, implementado fallback a `player.getItemBySlot(EquipmentSlot.HEAD)` si no hay accesorios Curios/Accessories equipados, permitiendo el uso normal de las gafas en el slot vanilla de casco.
    - Activado `FabricLayerRegistry.init()` en `Vision_gogglesFabricClient.java`, registrando las capas de renderizado 3D de las gafas y siluetas térmicas en Fabric.
  - **Corrección de Relación de Aspecto y Rendimiento Gráfico**:
    - Integrado el uniform `AspectRatio` y corrección de coordenadas UV en `nvg.fsh`, `thermal.fsh`, `hydro.fsh`, `hydro_dry.fsh` y `bio.fsh`. Los visores y retículas mantienen forma circular perfecta en pantallas panorámicas 16:9 y ultra-panorámicas 21:9.
    - Optimizado el radio y la frecuencia del escáner de bloques de spawn en `VisionWorldRenderer.java`, eliminando el análisis masivo de 274.625 bloques en el hilo principal de renderizado.
- **Auditoría Técnica Profunda Post-Plan v21 (`docs/auditorias/analisis_argon.md`)**:
  - Realizada auditoría estática y de flujo línea por línea en 5 áreas críticas (Seguridad en Servidores Dedicados, Sandbox Lua, Supervivencia y Duplicación, Paridad Fabric/Forge, y Shaders/Rendimiento).
  - Documentados los hallazgos residuales P0/P1/P2 tras el Plan v21 (métodos cliente aún presentes en `LuaModuleManager`/`LuaCustomModule`, falta de permisos OP en `/vg`, bypass `pcall` y falta de `resetInstructionCount()` por frame en `SandboxedDebugLib`, ausencia de Loot Table para `modification_station`, exploit de modo de visión al extraer módulos, ausencia de `ThermalEntityPainter` y slot `body` de Accessories en Fabric, y raycasts muertos en shaders de linterna) con calificación global de **6.4 / 10** y checklist para el Hotfix v22.
- **Optimización de Raycasts de Linterna por Frame (Área 5 - Plan v22)**:
  - Eliminados los 8 raycasts secundarios muertos (`VisionFlashlightsRange1..8`), rotaciones y cálculos vectoriales no leídos en `VisionRenderer.updateFlashlightEveryFrame`.
  - Eliminada la consulta redundante `checkIsWall` que ejecutaba un `level.clip` adicional en cada frame sin ser consumida por los shaders GLSL.
  - Reducción del 90% en raycasts de CPU por linterna por frame (de 10 raycasts a solo 1 raycast para `VisionFlashlightsRange0`), manteniendo un `hitRanges` seguro y zero-filled para los uniformes de OpenGL.
- **Hotfix Definitivo Post-Auditoría Argon (Build 2.0.0-GoldMaster // Plan v22)**:
  - **Aislamiento Total de Servidores Dedicados y Permisos Administrativos (Área 1)**:
    - Retiradas todas las referencias de cliente (`Minecraft`, `GuiGraphics`, `PostPass`) de `LuaModuleManager.java` y `LuaCustomModule.java`.
    - Eliminados métodos redundantes (`onClientTick`, `onRenderHud`, `hookThermalColor`) redirigidos completamente a `LuaClientBridge.java`.
    - Inicializado `LuaModuleManager.init()` desde `Vision_goggles.init()` común de forma segura para que el servidor dedicado cargue y reconozca scripts sin tocar clases de renderizado.
    - Exigido nivel OP 2 (`.requires(source -> source.hasPermission(2))`) en `/vg reload`, `/vg slots`, `/vg run` y `/vg givescript`. `/vg folder` restringido exclusivamente al entorno cliente.
  - **Hardening Avanzado del Sandbox Lua y Watchdog Imparable (Área 2)**:
    - Creado `ScriptQuotaExceededError extends Error` que la VM de Lua (`pcall` / `xpcall`) no puede interceptar ni anular, erradicando cualquier vector de bypass contra el watchdog de CPU.
    - Invocado `SandboxedDebugLib.resetInstructionCount()` al inicio de cada ciclo de `callOnEnable`, `callOnDisable`, `callOnTick`, `callOnRenderHud`, `callUpdateUniforms` y `evaluateThermalColor`, garantizando que scripts legítimos nunca agoten cuota por acumulación entre frames.
    - Acotado el radio a un máximo de 512 y longitud de línea a 1920 en `LuaHudCanvas.java`, y limitado el radio de búsqueda de entidades y bloques a 64.0 en `LuaWorldScanner.java`.
  - **Loot Table, Tag de Pico y Corrección de "Visión Fantasma" (Área 3)**:
    - Creado `loot_tables/blocks/modification_station.json` y añadido al tag `mineable/pickaxe.json` para que la Estación de Modificación se recupere normalmente al romperse con cualquier pico.
    - `ModularGogglesItem.removeModule()`: Al desmontar un módulo en la estación, si las gafas se quedan sin ese módulo o sin módulos en total, se desactivan automáticamente (`TAG_ACTIVE = false`, `TAG_MODE = 0`). Si se desmonta la linterna, se apaga de inmediato (`TAG_FLASHLIGHT_ACTIVE = false`, `TAG_FLASHLIGHT_ON = false`). Recortada la batería actual al nuevo límite máximo con `clamp`.
    - `GogglesUpgradeRecipe.assemble()`: Garantizado que las `pro_modular_goggles` conserven e instalen formalmente el módulo `battery_expansion` consumido en el crafteo.
  - **Paridad Fabric y Consumo de Batería en Torso (Área 4)**:
    - Conectado `ThermalEntityPainter.init()` y registrado `WorldRenderEvents.AFTER_ENTITIES` en Fabric, habilitando siluetas térmicas completas de entidades en Fabric.
    - Creado slot y tags de Accessories para `vision_goggles:tactical_flashlight` (`data/accessories/tags/items/body.json` y `data/accessories/entities/player.json`).
    - Añadido el tick de servidor para la linterna táctica equipada en el torso en `ModEvents.tickGoggles`, garantizando consumo real de batería y efecto de disuasión estroboscópico contra mobs al llevarla en Curios o Accessories.
- **Auditoría Técnica Profunda Post-Plan v22 (`docs/auditorias/analisis_argon_v22.md`)**:
  - Verificada la resolución de los bloqueadores de servidor dedicado (`LuaModuleManager`/`LuaCustomModule` limpios y permisos OP nivel 2 en `/vg`), hardening de Lua (`resetInstructionCount` por callback, `ScriptQuotaExceededError extends Error` inmune a `pcall`, cotas geométricas en `LuaHudCanvas`/`LuaWorldScanner`), supervivencia (`modification_station.json` loot table, `mineable/pickaxe.json`, preservación de `battery_expansion` en `GogglesUpgradeRecipe`) y reducción del 90% de raycasts por frame en `VisionRenderer`.
  - Documentados los 4 hallazgos puntuales restantes para el cierre definitivo en Fabric y Lua (`processResources` para expandir `${version}` en `fabric/build.gradle`, inicialización perezosa de `ThermalEntityPainter` en Fabric, eliminación de `fabric/src/main/resources/data/accessories/entities/player.json` que sobrescribe al de `common`, y `catch (Throwable)` + `resetInstructionCount()` en `LuaModuleManager.loadModuleFile()`), otorgando una calificación global de **7.9 / 10** (**8.5 / 10 en Forge** y **6.2 / 10 en Fabric**).
- **Cierre Definitivo Pre-Lanzamiento y Paridad 100% (Build 2.0.0-FinalRelease // Post-Auditoría v22)**:
  - **Expansión Automática de Versión en Fabric (`fabric/build.gradle`)**: Añadido bloque `processResources` para expandir formalmente `${version}` en `fabric.mod.json`, evitando errores de validación de metadatos al cargar en Fabric Loader.
  - **Inicialización Perezosa de Siluetas Térmicas (`ThermalEntityPainter.java`)**: Incorporado chequeo dinámico de inicialización (`if (!initialized) init()`) en `captureAndComposite()` tras comprobar que el modo térmico está activo, garantizando la carga correcta de shaders en Fabric una vez que `ResourceManager` y la ventana OpenGL están listos.
  - **Unificación de Slots de Accesorios en Fabric y Common**: Eliminado `fabric/.../player.json` y consolidado en `common/.../player.json` con soporte simultáneo para `"accessories:eyes"`, `"accessories:face"` y `"accessories:body"`.
  - **Captura Segura de Excepciones y Reset en Carga de Módulos (`LuaModuleManager.java`)**: Implementado `SandboxedDebugLib.resetInstructionCount()` y limpieza de `module = nil` antes de compilar cada archivo `.lua`, y sustituido `catch (Exception)` por `catch (Throwable)` para registrar limpiamente cualquier intento de desbordamiento de cuota durante el arranque.
  - **Reseteo a Modo Standby y Apagado Automático de Linterna**: Configurado `TAG_MODE = -1` (Standby) en `ModularGogglesItem.removeModule()` al desmontar el visor activo, y apagado forzado de `TAG_FLASHLIGHT_ACTIVE = false` en `ModuleType.FLASHLIGHT` cuando la batería se agota por completo.
  - **Fast-Path en Uniformes de Linterna**: Añadido retorno temprano `if (currentCount == 0) return;` en `FlashlightUniforms.applyToVanilla` y `SodiumFlashlightUniforms.upload`, eliminando la subida innecesaria de arrays matriciales a la GPU cuando no hay linternas encendidas.
  - **Blindaje de Equipamiento en Offhand**: Validado que `stack.getItem() instanceof VisionGogglesItem` en `PlatformMethodsImpl.equipInSlot()` de Forge y Fabric, permitiendo el equipamiento directo de gafas sostenidas en la mano secundaria.

- **Animación Mecánica 1ra/3ra Persona, Visión Nocturna Cliente y Auditoría Integral v26 (Build 2.0.0-GoldMaster // Plan v25 & v26)**:
  - **Cinemática de Visor Abatible y Mano en 1ra/3ra Persona (`GogglesFlipAnimator`, `FirstPersonVisorRenderer`, `MixinHumanoidModel`, `MixinItemInHandRenderer`)**:
    - Implementada máquina de estados interpolada (`0.55s`) al encender/apagar las gafas (`N`), retrasando la activación del shader, HUD, zumbido de cátodo y brillo de lentes hasta que el visor termina de bajar sobre los ojos.
    - Renderizado del visor 3D y del brazo/manga del jugador en primera persona sujetando el tubo derecho y abatiendo las gafas, ocultando únicamente el brazo principal vanilla durante la transición (`MixinItemInHandRenderer` en `renderArmWithItem`) para mantener visibles escudos, tótems o linternas en la mano secundaria.
    - Restaurado `Lighting.setupForFlatItems()` tras el pase 3D en `FirstPersonVisorRenderer` para evitar fugas de iluminación direccional en el HUD 2D.
  - **Visión Nocturna 100% Cliente sin Efecto de Poción (`MixinLivingEntity`, `MixinGameRenderer`)**:
    - Reemplazada la inyección servidor de `MobEffects.NIGHT_VISION` por intercepción cliente en `LivingEntity#hasEffect` y `GameRenderer#getNightVisionScale`, erradicando el bug de desincronización al cambiar de casco y los iconos/partículas de poción.
  - **Resolución Unificada de Modo Activo (`VisionGogglesItem.getActiveVisionMode`)**:
    - Centralizada la detección del `VisionMode` activo en `VisionGogglesItem`, `VisionRenderer`, `FirstPersonVisorRenderer`, `GogglesRenderLayer`, `DurabilityModule`, `MixinFogRenderer` y `Vision_gogglesForge`, corrigiendo la eliminación de niebla subacuática en las Hydro Goggles dedicadas y el color de lentes/grietas en gafas modulares multimodo.
  - **Aislamiento de `PostChain` y Servidor Dedicado (`VisionShaderManager`, `VisionGogglesClientRenderHelper`)**:
    - `VisionShaderManager` ahora instancia `new PostChain(...)` directamente sin invocar `mc.gameRenderer.loadEffect()` ni destruir `GameRenderer.postEffect` al apagar las gafas, preservando shaders de modo Espectador y de otros mods.
    - Extraídas las referencias a `VisionGogglesGeoRenderer` y `BlockEntityWithoutLevelRenderer` hacia `VisionGogglesClientRenderHelper`, eliminando riesgos de `NoClassDefFoundError` en servidores dedicados.
    - Corregido el desfase de 1 frame en variantes de modelos GeckoLib sobrescribiendo `VisionGogglesGeoRenderer.renderByItem`, y fijada la transición de `AnimationController` a `0` ticks.
  - **Motor Lua Avanzado, Red y Estación de Modificación**:
    - Conectado `LuaClientBridge.hookThermalColor(entityTypeId, isHostile)` por entidad individual en `ThermalEntityPainter`, `MixinEntityRenderDispatcher` y `thermal_vision.fsh`, permitiendo colorear jugadores, hostiles y pasivos por separado desde scripts Lua.
    - Conectados los shaders personalizados (`VISION_MODE`) y `getBatteryDrain()` de `LuaCustomModule`, y optimizado `LuaHudCanvas` (`drawLine` y `drawCircle`) mediante quads vectoriales con `Matrix4f` + `BufferBuilder`.
    - Corregido el orden de `slot.onTake` en `ModificationStationMenu.quickMoveStack` y añadido `setChanged()` en `ModificationStationBlockEntity.removeItemNoUpdate`.
    - Añadida validación `Env.CLIENT` en `SyncFlashlightPacket` y `ConfigSyncPacket`, sincronización de apagado de linterna al agotar batería en `ModuleType.FLASHLIGHT`, broadcast de `ConfigSyncPacket` en `/vg slots`, sliders de capacidad de slots en `ModConfigGui`, declaración de `geckolib` en `mods.toml`/`fabric.mod.json` y completadas las 174 claves de traducción en los 12 idiomas.
  - **Animación Mecánica en 2 Fases (Subir Mano -> Bajar Gafas)**:
    - Actualizado `GogglesFlipAnimator` (`TRANSITION_DURATION_SECONDS = 0.72f`, `REACH_PHASE_END = 0.35f`) con `computeVisorProgress` y `computeArmReach`.
    - En 1ra persona (`FirstPersonVisorRenderer`) y 3ra persona (`MixinHumanoidModel` + `GogglesRenderLayer`), la mano principal sube primero desde abajo-derecha hasta sujetar el visor arriba (`0.0 -> 0.35`), luego baja el visor junto con la mano (`0.35 -> 0.88`) y finalmente retira la mano al bloquearse sobre los ojos (`0.88 -> 1.0`).
  - **Eliminación del Módulo de Seguridad de Spawn (`spawn_security_module`)**:
    - Retirado por completo de `ModConstants`, `ModuleType`, `ModItems`, `ModTabs`, `ModularGogglesItem`, `ModificationStationBlockEntity`, `VisionGogglesJeiPluginCommon`, `TacticalRadialScreen` y `VisionWorldRenderer` (conservando el renderizado 3D de bloques calientes en modo Térmico).
    - Eliminados modelo, texturas, receta y claves de traducción asociadas en los 16 idiomas, y actualizado `docs/MODRINTH_CHANGELOG_2.0.0.md`.
  - **Rediseño Integral de 21 Ítems 2D `16×16` v3 con Micro-animaciones `.png.mcmeta` (`Plan v27/v28/v29`)**:
    - Instaladas y enlazadas globalmente en Antigravity las skills especializadas de pixel art (`minecraft-pixel-art`, `mc-asset`, `pixel-art-sprites`, `pixel-art-professional`, `pixel-art-animator`, `minecraft-imagegen`, `minecraft-resource-pack`).
    - Creado el generador `docs/planes/generar_items_v3_completo.py` con **21 ítems `16×16`** en estética *Tactical Charcoal* (5 familias de silueta única: tubos ópticos acoplables, utilidad física, tarjetas HUD SD, energía/firmware y hardware TEMS), validados con `mc-asset validate --profile minecraft:item --minecraft-version 1.20.1`.
    - Añadidas **13 micro-animaciones `.png.mcmeta`** (`nvg_module`, `thermal_module`, `hydro_module`, `bio_module`, `sonar_module`, `solar_module`, `battery_expansion_module`, `vital_info_module`, `programmable_module`, `nvg_battery`, `installation_script`, `thermal_core`, `lifesign_sensor`) y **8 sprites estáticos** de alta nitidez (`zoom_module`, `flashlight_module`, `environment_module`, `tactical_processor`, `optical_lens`, `tactical_frame`, `flashlight_body`, `electrical_parts`).
    - Rediseñado **`installation_script`** como cartucho/disquete táctico con shutter metálico superior y pantalla OLED con scroll de código Lua.
    - Actualizados los 13 JSON de `models/item/` para unificar nombres sin `_v2`, y eliminados **18 PNGs huérfanos/obsoletos** en `textures/item/` (dejando intactos las 6 gafas, `modification_station` y la linterna 3D `tactical_flashlight`).
  - **Animación 3D en 4 Fases para Equipar Gafas en Primera y Tercera Persona (`Plan v30`)**:
    - Actualizado `EquipPacket` para incluir el `InteractionHand` de origen y reproducir sonido táctico en mundo (`ARMOR_EQUIP_IRON` + `ARMOR_EQUIP_LEATHER`) al insertar en el slot de Curios/Accessories.
    - Actualizado `VisionGogglesItem` (`getUseDuration = 14` ticks, `UseAnim.NONE`) para reemplazar la animación lateral de arco (`UseAnim.BOW`) y notificar al animador al completar el equipado.
    - Creados `GogglesEquipAnimator` (máquina de estados `IDLE`, `EQUIPPING`, `CANCELING`, `COMPLETING` segura para servidor dedicado) y `FirstPersonEquipRenderer` (centrado y agarre a 1 o 2 manos, elevación hacia la frente, encaje en montura y retracción suave post-equipado sin parpadeo del ítem ni ocultar mano secundaria ocupada).
    - Integrados `MixinItemInHandRenderer` (inyectado en `renderArmWithItem` usando `renderItem`), `MixinHumanoidModel` (sincronización 3ra persona/multijugador al equipar con guardia contra `PlayerRenderer.renderHand`) y limpieza de estado en `VisionRenderer`.
  - **Migración Automática de Scripts Lua Antiguos y Ceguera (`Glare`) con Linterna en Torso/Mano en NVG (`Plan v32`)**:
    - `LuaModuleManager.java`: Añadida limpieza automática (`LEGACY_EXAMPLE_FILES`) de los 12 scripts de ejemplo anteriores (`01_visor_cibernetico.lua` .. `12_visor_abissal_hydro.lua`), aprovisionamiento continuo de los 6 nuevos scripts oficiales (`01_radar_tactico_360.lua` .. `06_suite_termica_flir.lua`) y actualización de `00_GUIA_PASO_A_PASO.txt` al iniciar o pulsar **🔄 Recargar**.
    - `VisionShaderManager.java` y `nvg.fsh`: Conectado `TacticalFlashlightItem.getHeldActiveFlashlight(...)` (linterna equipada en el torso vía Curios/Accessories o sostenida en mano principal/secundaria) al cálculo de sobreexposición (`targetGlare = Math.max(1.25f, mode.glareIntensity)`) cuando las gafas de Visión Nocturna están encendidas, aplicando ceguera tanto al portador como a otros jugadores, y acotado `clamp(Glare, 0.0, 1.0)` en el `mix(...)` de `nvg.fsh`.
  - **Eliminación de Huecos en Brazo y Parte Superior del Visor 1ra Persona con Zoom Progresivo (`Plan v33`)**:
    - `FirstPersonVisorRenderer.java` y `FirstPersonVisorModel.java`: Añadido shroud frontal superior de ancho completo en `visor_pivot` (`addBox(-4.5F, -3.2F, -1.7F, 9.0F, 2.4F, 3.0F)`) y curva continua de zoom de aproximación al ojo (`scale: 1.05H -> 3.85H`, `yOffset: -0.46H -> -0.21H`) para que los 4 tubos ópticos cubran la vista sin abrir huecos de cielo por encima de la barra superior.
    - `FirstPersonVisorRenderer.java` y `FirstPersonEquipRenderer.java`: Escalado axial del antebrazo sobre su eje longitudinal local (`armLengthScale = 2.30H` tras las rotaciones 3D y antes del pivote de nudillos `(0, -9/16, 0)`), manteniendo el puño anclado en el borde del visor mientras el hombro se extiende siempre fuera de la esquina inferior-derecha de la pantalla.

## v46 (M3+M4) - Tri-Lens unificado 64x64
- GogglesRenderLayer y FirstPersonVisorRenderer migrados a TriLensManualRenderer (BakedGeoModel, bones modulares, glow). syncFromRender solo MAIN_HAND. Retiradas clases/assets legacy 128x128. E2E tiers 1-4 OK, build forge/fabric OK.

- v46 refinement: TriLensManualRenderer.renderVisorOnly (solo visor_pivot en 1P), tilt -6+14*progress.

- v46 fix: 1P visor scale(-s,-s,s) + hinge pitch; equip gira 180 para mostrar lado ocular; eyecups 3D en visor_pivot (scripts/add_eyecups.py).

- v46 fix (#19): Revertidos eyecups traseros y eliminado add_eyecups.py; VisionGogglesItem usa IDLE_DOWN en item render para que equip 1P no pliegue los tubos hacia atras; fixed rotation [0,0,0] en los 6 item JSONs + 180+yawOffset en ModificationStationRenderer.

- v46 fix (#22): FirstPersonVisorRenderer orientacion ocular +Z (scale(-s,-s,s), sin YP 180) coincidente con equip; eyecups binoculares 3D coherentes por variante en visor_pivot con cristal oscuro eyepiece_rear (sin glow bleed); rasterizer3d.py actualizado.

- **1P Flip Upper Brow Base & Forehead Anchoring (FirstPersonVisorRenderer, *_goggles.geo.json)**: Added full-width upper brow shroud bar and Wilcox forehead cushion to isor_pivot across all 6 Tri-Lens models (with zero Steve-head overlap in idle_down/idle_up) and anchored FirstPersonVisorRenderer yOffset (Mth.lerp(browPresence, -0.58f, -0.42f) + 0.22f * ez) to the top viewport edge so the visor hinges down from a solid upper base without floating.

- v46 fix (#27): Linterna de Modular Goggles independiente de TAG_ACTIVE (glow de mod_flashlight en GogglesRenderLayer/TriLensManualRenderer, orden de inicializacion TAG_BATTERY); paleta Pro Modular actualizada a obsidiana mate + titanio cepillado + azul acero (sin amarillo).

- v46 fix (#30): Paleta de tri_lens_pro_modular refinada a obsidiana mate + titanio-pizarra oscuro de 10 pasos suaves (sin franjas zebra de alto contraste).

## 2026-11-22 - Thermal: armor emits no heat
- ThermalEntityPainter: armor layers drawn at minimum alpha (26) instead of 72% heat; still occludes body by depth. Flashlight-armor bug (night: armor/body black under beam) still under investigation.
