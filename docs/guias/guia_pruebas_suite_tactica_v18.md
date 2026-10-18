# Guía Oficial de Pruebas — Suite de Mejoras Tácticas Integrales (v2.0-Alpha)

Esta guía detalla los comandos y pasos paso a paso para probar en juego las 5 nuevas mecánicas implementadas en la actualización v18.

---

## 1. Inicio del Juego
Los archivos JAR de producción ya están compilados y copiados en las instancias de ATLauncher:
- **Forge**: Instancia `Minecraft1201withForge`
- **Fabric**: Instancia `Minecraft1201withFabric`

Abre tu lanzador ATLauncher y dale a **Jugar** en cualquiera de las dos instancias (o ejecuta `./gradlew :forge:runClient` en terminal).

---

## 2. Prueba 1: Estación de Modificación 2.0 (Extracción y Reparación)

### Comandos de preparación:
```text
/give @p vision_goggles:modification_station
/give @p vision_goggles:modular_goggles
/give @p vision_goggles:nvg_module
/give @p vision_goggles:thermal_module
/give @p vision_goggles:tactical_frame
/give @p vision_goggles:electrical_parts
```

### A. Prueba de Extracción de Módulos (Desensamblaje):
1. Coloca y abre la **Estación de Modificación**.
2. Instala primero un módulo (ej. `nvg_module` o `thermal_module`) en las gafas modulares.
3. Ahora coloca las gafas con módulos en la **ranura izquierda (Slot 0)** y **deja la ranura central (Slot 1) vacía**.
4. Verás que en la **ranura de salida (Slot 2)** aparece el módulo instalado.
5. Haz clic para tomar el módulo: se irá a tu inventario y las gafas en la ranura izquierda quedarán libres con su socket disponible.

### B. Prueba de Reparación sin Yunque:
1. Daña tus gafas (puedes recibir golpes o usar unas gafas con durabilidad reducida).
2. Coloca las gafas dañadas en la ranura izquierda.
3. Coloca un `tactical_frame` en la ranura central: la salida entregará las gafas **100% reparadas** (0 daño).
4. O coloca `electrical_parts` en la ranura central: la salida restaurará un **+35% de vida**.
5. No requiere niveles de XP de Minecraft ni penalizaciones acumulativas.

---

## 3. Prueba 2: Linterna Táctica en Modo Estroboscópico (`STROBE`) y Freno a Mobs

### Comandos de preparación:
```text
/give @p vision_goggles:tactical_flashlight
/time set night
```

### Pasos:
1. Haz **Clic Derecho** para encender la linterna táctica.
2. Presiona **Shift + Clic Derecho** (o la tecla `K`) para ciclar entre los modos de apertura hasta llegar a:
   `§eLinterna: §cEstroboscópico Táctico (13Hz)`
3. Verás el haz parpadeando a 13 Hz con pulso estroboscópico militar.
4. Genera un Creeper o Zombie en frente de ti:
   ```text
   /summon creeper ~ ~ ~4
   ```
5. Apunta el haz de la linterna directo a su cabeza:
   - Verás que el Creeper sufre lentitud (*Slowness I*).
   - Acércate a él mientras le apuntas a la cara: el Creeper intentará inflarse pero la luz estroboscópica interrumpe su mecha, evitando que detone.

---

## 4. Prueba 3: Shaders Ópticos de Precisión y Zoom Dinámico (`focus`)

### Comandos de preparación:
```text
/give @p vision_goggles:modular_goggles
/give @p vision_goggles:zoom_module
/give @p vision_goggles:nvg_module
/give @p vision_goggles:thermal_module
```

### Pasos:
1. Instala el módulo de zoom y el de visión nocturna en las gafas y equípatelas en la cabeza.
2. Activa las gafas (tecla `V`).
3. Mantén presionada la tecla `Z` (zoom) y usa la rueda del ratón para ampliar hasta 8x:
   - Observa los bordes de la lente ocular: notarás la **aberración cromática perimetral** en los bordes y la contracción de la viñeta.
   - En modo térmico: al pasar de 4x de aumento, la imagen se pixelará con ruido térmico digital FLIR real.
4. **Prueba de Auto-Gating y Persistencia:**
   - De día al aire libre (`/time set day`), mira hacia arriba en dirección al Sol: los tubos intensificadores sufrirán un destello cegador que se disipa con curva analógica lenta de fósforo P43.
   - Acércate a un pozo de lava o fogata a menos de 4 bloques: verás que la proximidad directa activa la protección de sobrecarga del sensor.
   - Detona dinamita cerca:
     ```text
     /summon tnt
     ```
     Al estallar recibirás un fogonazo de sobreexposición instantánea.

---

## 5. Prueba 4: Firmas Térmicas de Bloques en el Entorno

### Pasos:
1. Activa el modo Térmico con las gafas puestas.
2. Coloca y enciende los siguientes bloques en la oscuridad:
   - Un horno cocinando (`furnace` o `blast_furnace`).
   - Una fogata encendida (`campfire`).
   - Un cubo de lava en el suelo.
3. Observa cómo emiten un calor incandescente en blanco/amarillo/naranja en la paleta Ironbow.
4. Ahora coloca un bloque de **Hielo Azul** (`blue_ice`) o **Nieve en Polvo**:
   - Observarás que actúan como sumideros criogénicos, viéndose en un tono negro/índigo gélido.

---

## 6. Prueba 5: Integración JEI (Just Enough Items)
1. Abre tu inventario y abre el panel de JEI en la derecha.
2. Busca `Estación de Modificación` (o presiona `U` sobre cualquier gafa modular, `tactical_frame` o `electrical_parts`).
3. Verás las recetas catalogadas:
   - Instalación de módulos de hardware.
   - Grabado de firmware en chips programables.
   - **Extracción de módulos** (receta con ranura central vacía).
   - **Reparación integral** (con armazón táctico) y **mantenimiento de campo** (con partes eléctricas).
