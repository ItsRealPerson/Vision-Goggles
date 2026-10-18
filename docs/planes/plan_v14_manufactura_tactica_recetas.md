# Plan v14: Sistema de Manufactura Táctica y Rebalanceo Integral de Recetas (TEMS)

## 1. Contexto y Objetivos
El usuario solicitó que los crafteos de *Vision Goggles* tengan **100% de coherencia física y anatómica** con el objeto fabricado (sin recurrir a mezclas absurdas como una linterna hecha con fuego medieval o cubos macizos de 1 metro de hierro/diamante en wearables), y que al mismo tiempo **no sean baratos**, garantizando un alto coste e inversión mineral digno de equipo de operaciones especiales.

Siguiendo la directriz del usuario (*"ya por último creamos ítems nuevos para que los crafteos tengan sentido"*), se introduce el **Sistema de Manufactura Táctica (TEMS)** con **6 componentes intermedios de ingeniería**.

---

## 2. Los 6 Componentes Tácticos Intermedios
1. **`vision_goggles:optical_lens` (Lente Óptica de Precisión)**:
   - Representa: Lente colimadora convexa multicapa con montura micrométrica de oro.
   - Ingredientes: 2 Lingotes de Oro, 2 Amatistas, 1 Cristal Tintado (Rinde 2 unidades).
2. **`vision_goggles:tactical_frame` (Armazón Táctico Balístico)**:
   - Representa: Diadema ergonómica con arnés de cuero de ajuste occipital y puente de aleación.
   - Ingredientes: 4 Cuero militar, 2 Lingotes de Hierro, 1 Lingote de Cobre, 1 Redstone.
3. **`vision_goggles:flashlight_body` (Cuerpo Mecanizado de Linterna)**:
   - Representa: Tubo cilíndrico estriado mecanizado en hierro/aluminio con recubrimiento de cuero antideslizante, cámara de batería y pulsador trasero de clic.
   - Ingredientes: 3 Lingotes de Hierro, 2 Cuero, 1 Partes Eléctricas.
4. **`vision_goggles:tactical_processor` (Coprocesador Militar DSP)**:
   - Representa: SoC de alta frecuencia con silicio de diamante, osciladores de cuarzo y pines BGA de oro para procesamiento de HUD y enlace con el uniform `u_DSPActive`.
   - Ingredientes: 1 Diamante, 2 Lingotes de Oro, 2 Cuarzo del Nether, 1 Cobre, 1 Redstone.
5. **`vision_goggles:thermal_core` (Sensor Térmico Criogénico / Microbolómetro)**:
   - Representa: Sensor de infrarrojo de onda larga con matriz piroeléctrica de calor.
   - Ingredientes: 2 Varas de Blaze, 2 Cuarzo del Nether, 2 Lingotes de Oro, 1 Crema de Magma, 1 Diamante.
6. **`vision_goggles:lifesign_sensor` (Sensor Biométrico / Signos Vitales)**:
   - Representa: Detector de bioimpedancia y frecuencias vitales a través de obstáculos.
   - Ingredientes: 1 Ojo de Ender, 2 Lágrimas de Ghast, 1 Amatista, 1 Redstone.

---

## 3. Ensamblaje Físico de Ítems Finales
* **`flashlight_module`**: Lente Óptica + Glowstone Block / Linterna de Mar + 2 Partes Eléctricas + 1 Cobre.
* **`tactical_flashlight`**: Cabezal (`flashlight_module`) + Cuerpo (`flashlight_body`) + Batería (`nvg_battery`).
* **`modular_goggles` (2 slots)**: Armazón (`tactical_frame`) + 2x Lente Óptica + Coprocesador DSP + Batería + 2x Partes Eléctricas.
* **`pro_modular_goggles` (4 slots)**: Gafas Modulares previas + 2x Netherite Ingot + 2x Echo Shards + Batería de Expansión.
* **`thermal_module`**: Sensor Térmico (`thermal_core`) + Lente Óptica + 2x Partes Eléctricas + 2x Oro.
* **`thermal_goggles`**: Armazón (`tactical_frame`) + Módulo Térmico + Batería NVG.
* **`bio_module`**: Sensor Biométrico (`lifesign_sensor`) + Lente Óptica + 2x Partes Eléctricas.
* **`biometric_goggles`**: Armazón (`tactical_frame`) + Módulo Bio + Batería NVG.
* **`zoom_module`**: 2x Lente Óptica + Catalejo (Spyglass) + 2x Partes Eléctricas.
* **`sonar_module`**: Echo Shard / Sculk Sensor + 2x Amatistas + 2x Partes Eléctricas.

---

## 4. Plan de Ejecución
1. Registrar los 6 ítems en `ModItems.java` y `ModTabs.java`.
2. Crear modelos JSON en `assets/vision_goggles/models/item/`.
3. Generar texturas pixel art 16x16 de alta resolución con script Python en `assets/vision_goggles/textures/item/`.
4. Crear y actualizar las 16 recetas JSON en `data/vision_goggles/recipes/`.
5. Localizar los 6 ítems en los 12 archivos de idiomas (`lang/`).
6. Compilar y empaquetar con subagente (`self`).
7. Desplegar JARs en ATLauncher (Forge y Fabric).
8. Actualizar `docs/CHANGELOG.md` y hacer commit & push a GitHub.
