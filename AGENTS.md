# AGENTS.md — Guía Universal de Arquitectura (Mods, Plugins y Datapacks | MC 1.20.1 – 26.3+)

## 1. Autodetección Obligatoria del Proyecto (Paso Cero)
Antes de escribir código, determina el tipo de proyecto y versión leyendo ÚNICAMENTE el archivo raíz correspondiente (`gradle.properties`, `build.gradle`, `pom.xml`, `paper-plugin.yml`, `plugin.yml`, `fabric.mod.json`, `neoforge.mods.toml`, `mods.toml` o `pack.mcmeta`):
- **Rama Legacy-Moderna (1.20.1 – 1.20.4):** Java 17. Persistencia en ítems vía NBT (`ItemStack.getOrCreateTag()`). Recetas/Loot tables con nombres de carpetas en plural (`recipes/`, `loot_tables/`). Networking clásico (`FriendlyByteBuf` / `SimpleChannel`).
- **Rama Transición Data-Driven (1.20.5 – 1.21.4):** Java 21. PROHIBIDO `getOrCreateTag()` en ítems; usa `DataComponentType<T>`, `Codec` y `StreamCodec`. Carpetas de datapack en singular (`recipe/`, `loot_table/`, `item_modifier/`). Networking vía `CustomPacketPayload`.
- **Rama Moderna / Year-Based (1.21.5+ hasta 26.1 – 26.3+):** Java 21+. Respeta los cambios de formato de `pack.mcmeta`, definiciones de modelos de ítems client-side (`assets/<namespace>/items/*.json` introducidos desde 1.21.4+) y componentes de datos nativos de la serie 26.x.

## 2. Reglas de Ahorro de Tokens y Exploración (CRÍTICO)
- **Directorios Prohibidos:** NUNCA escanees ni leas `.gradle/`, `build/`, `target/`, `run/`, `.idea/`, `out/` ni binarios `.jar` o `.class`.
- **Archivos Multimedia:** No leas `.png`, `.ogg` o `.bbmodel` directamente salvo que se te pida procesarlos mediante un script en Python.
- **Compilación Rápida (Sin quemar cuota):**
  - **Mods Multi-Loader (Architectury/MultiProject):** Valida con `./gradlew :common:compileJava` (o el loader puntual `:fabric:compileJava`, `:neoforge:compileJava`, `:forge:compileJava`).
  - **Mods Single-Loader / Plugins Gradle:** Valida con `./gradlew compileJava`.
  - **Plugins Maven:** Valida con `mvn test-compile`.
  - **Datapacks:** No ejecutes compiladores; valida sintaxis JSON y referencias de namespaces/tags mediante scripts rápidos o inspección directa.
  - **PROHIBIDO** ejecutar `./gradlew build`, `remapJar` o `shadowJar` tras cada cambio menor; hazlo solo al cerrar una feature completa o si el usuario lo pide.
- **Salidas Concisas:** Al crear o editar JSONs, `.mcfunction` o YAMLs largos, escríbelos directo en disco sin imprimir todo el archivo en el chat.

## 3. Reglas para Mods (Forge / NeoForge / Fabric / Architectury)
- **Multi-Loader (Architectury):** Mantén +90% del código en `common`. Usa `@ExpectPlatform` o eventos de Architectury antes de recurrir a código específico de loader.
- **Separación Cliente/Servidor:** PROHIBIDO importar clases de `net.minecraft.client.*` (Screens, Renderers, Shaders GLSL, KeyMappings) en paquetes comunes o de servidor.
- **GeckoLib 4:**
  - Usa `AnimatableInstanceCache` vía `GeckoLibUtil.createInstanceCache(this)`.
  - En **1.20.1**, usa el sistema de `initializeClient` (Forge) o `GeoItemRenderer` correspondiente a esa versión. En **1.21+ / 26.x**, usa `GeoRenderProvider` de forma segura para el servidor.
  - Verifica que los nombres de los *bones* en Java coincidan exactamente con el `.geo.json`.
- **Mixins:** Último recurso si no existe evento nativo. Prefiere `@Inject` y **MixinExtras** (`@WrapOperation`, `@ModifyExpressionValue`). PROHIBIDO usar `@Overwrite` o `@Redirect` salvo orden explícita. Prefija métodos inyectados con `modid$`.

## 4. Reglas para Plugins (Paper / Folia / Spigot / Velocity)
- **Compatibilidad Folia y Multithreading:**
  - Si el proyecto soporta **Folia** (`folia-supported: true`), **PROHIBIDO** usar `Bukkit.getScheduler()`. Usa `EntityScheduler`, `RegionScheduler`, `GlobalRegionScheduler` o `AsyncScheduler`.
  - Nunca accedas ni modifiques bloques, entidades o chunks desde hilos asíncronos.
- **API de Texto y Datos:**
  - Usa **Adventure API** (`Component`, `MiniMessage`) en lugar del obsoleto `ChatColor` con códigos `§`.
  - Para datos persistentes en ítems, entidades o chunks, usa siempre `PersistentDataContainer` (PDC) con `NamespacedKey`.
- **Rendimiento:** Diseña listeners de eventos de alta frecuencia (`PlayerMoveEvent`, `BlockPhysicsEvent`, ticks) con *early returns* inmediatos para no degradar los MSPT (validable con Spark).

## 5. Reglas para Datapacks y Resourcepacks (1.20.1 – 26.3)
- **Nomenclatura según `pack_format`:** Verifica el `pack.mcmeta` antes de crear carpetas. En 1.20.x (<1.20.5) usa plurales (`functions/`, `recipes/`, `loot_tables/`, `structures/`). Desde 1.21+ hasta 26.3 usa singulares (`function/`, `recipe/`, `loot_table/`, `structure/`).
- **Macros y Rendimiento (1.20.2+ hasta 26.3):** Aprovecha macros de funciones (`$comando $(variable)`) y `return run` en lugar de cadenas gigantes de `execute if`. Evita `@e` sin filtros de `type`, `distance` o `tag` en `tick.mcfunction`.

## 6. Automatización con Python (Texturas, Modelos y JSON Masivos)
- Cuando la tarea requiera generar múltiples recetas, blockstates, loot tables, funciones o texturas pixel art 16x16 (estilo Jappa), escribe y ejecuta un script en `scripts/` con Python (`PIL`, `numpy`, `json`) en lugar de crear decenas de archivos a mano.

## 7. Memoria de Bugs Resueltos (Auto-Actualizable)
- Cuando resuelvas un bug no trivial (Mixins, concurrencia en Folia, versión de Data Components o shaders), añade una línea concisa al final de este archivo con la causa y la solución para no re-investigarlo en futuras sesiones.
- **Lateralidad invertida de linterna en mano (`VisionRenderer`)**: En Minecraft (mano derecha, `yaw=0` mira a `+Z` Sur), `Quaternionf().rotationY(-yaw)` transforma `(+1,0,0)` a la **izquierda** (`+X` Este) y `(-1,0,0)` a la **derecha** (`-X` Oeste). Separar `camLeft`/`camRight`, usar `camRight` + `player.getMainArm()` en 1ra persona (`MAIN_HAND`/`OFF_HAND`) y `sideSign = rightArm ? -1.0f : 1.0f` en `computeArmHandWorldPos` (3ra persona), manteniendo `TORSO_CHEST` en pectoral izquierdo (`camLeft * 0.18` / `+0.18f`).

## 8. Ciclo de Ejecución 100% Autónoma y Escalamiento Automático (Cero Pausas)
- **Ejecución Continua:** Todo el ciclo (Paso 0: Bootstrap -> Paso 1: Detección y Plan en `docs/planes/plan_vX.md` -> Paso 2: Escritura directa a disco de código/assets/scripts Python -> Paso 3: Compilación rápida única -> Paso 4: Registro de bugs en § 7 y `docs/CHANGELOG.md`) debe ejecutarse de corrido en una sola pasada continua (modo Turbo) sin frenos manuales ni solicitar cambios de modelo al usuario.
- **Enrutamiento Estándar:** Tareas estándar (bloques, ítems, eventos, datapacks, JSONs, configuración) se diseñan e implementan directamente de principio a fin.
- **Escalamiento Automático:** Ante alta complejidad (Mixins delicados, Shaders GLSL, concurrencia en Folia) o si la compilación rápida (`compileJava`) falla 2 veces seguidas por el mismo error:
  - Ejecutar en terminal: `agy --model gemini-4-argon -p "Analiza [error/archivos] respetando AGENTS.md y escribe la solución técnica exacta en docs/planes/solucion_argon.md"` (o usar `invoke_subagent` si la CLI no estuviera en el PATH).
  - Leer el archivo generado en disco, aplicar la solución en el código y validar con `compileJava` de inmediato sin detener el flujo ni pedir intervención al usuario.

