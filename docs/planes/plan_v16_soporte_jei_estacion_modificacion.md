# Plan de Desarrollo v16 — Integración Oficial de JEI para la Estación de Modificación

## 1. Contexto y Objetivos
El usuario solicitó:
> *"y agreguemos soporte a la estacion de modificacion al jei"*

La Estación de Modificación (`modification_station`) maneja la instalación física de módulos en las gafas modulares y el flasheo de scripts Lua en módulos programables mediante lógica interna de Java en su `BlockEntity`, sin usar el `RecipeManager` de Minecraft. Por ello, JEI no tenía visibilidad nativa de este proceso.

El objetivo es integrar un plugin oficial de **Just Enough Items (JEI 15)** para Minecraft 1.20.1 en Architectury (Forge + Fabric) que exponga todas las capacidades de la Estación de Modificación.

## 2. Decisiones Técnicas y Arquitectura
1. **Dependencia Opcional (`modCompileOnly`)**:
   - `jei_version=15.20.0.106` en `gradle.properties`.
   - `common/build.gradle`: `mezz.jei:jei-1.20.1-common-api:${jei_version}` en `modCompileOnly`.
   - `forge/build.gradle`: `mezz.jei:jei-1.20.1-forge-api:${jei_version}` en `modCompileOnly`.
   - `fabric/build.gradle`: `mezz.jei:jei-1.20.1-fabric-api:${jei_version}` en `modCompileOnly`.
   - Garantiza que JEI sea 100% opcional en tiempo de ejecución: si el jugador no tiene JEI instalado, el mod funciona con normalidad sin ningún crasheo.
2. **Estructura Compartida (95% en `common`)**:
   - `dev.itsrealperson.vision_goggles.compat.jei.ModificationStationJeiRecipe`: Record con inputs (slot 0 y 1), output (slot 2 con NBT) y descripción.
   - `dev.itsrealperson.vision_goggles.compat.jei.ModificationStationRecipeCategory`: Categoría visual recortada de `modification_station.png` (`u=38, v=29, w=100, h=30`), alineando con exactitud los 3 slots y los símbolos de unión `+` y flecha `->`.
   - `dev.itsrealperson.vision_goggles.compat.jei.VisionGogglesJeiPluginCommon`: Registro de categorías, generación de recetas sintéticas (12 módulos x gafas modulares y pro + flasheo de scripts + chips programados), asignación de catalizador (`ModBlocks.MODIFICATION_STATION`) y click area interactiva en la flecha de progreso del GUI (`98, 36, 16x14`).
3. **Puntos de Entrada Multi-Cargador**:
   - Forge: `VisionGogglesJeiForgePlugin` con anotación `@JeiPlugin`.
   - Fabric: `VisionGogglesJeiFabricPlugin` registrado en `fabric.mod.json` como `"jei_mod_plugin"`.
4. **Localización**:
   - Claves añadidas en los 12 idiomas del mod (`jei.vision_goggles.install_module`, `jei.vision_goggles.flash_firmware`, `jei.vision_goggles.install_script_module`).

## 3. Verificación
- Compilación y verificación sin errores de `:common`, `:forge`, y `:fabric`.
- Despliegue de los nuevos JARs de producción a ATLauncher.
- Actualización acumulativa de `docs/CHANGELOG.md` y push inmediato a GitHub.
