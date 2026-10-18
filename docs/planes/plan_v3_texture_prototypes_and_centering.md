# Plan de Desarrollo v3: Prototipos de Assets y Centrado de Texturas

## 1. Contexto y Requerimientos del Usuario
1. **Eliminación del Módulo Visor de Chunks (`chunk_viewer_module`)**:
   - Retirar completamente el ítem del registro, pestaña creativa, estación de modificación, recetas y traducciones, solucionando la textura faltante (bloque negro/morado).
2. **Centrado Horizontal de Texturas**:
   - Corregir el desplazamiento visual de los módulos en inventario y marcos (`item frames`), los cuales tenían 0px de margen a la izquierda y 3px a la derecha. Centrar todas las texturas de módulos a 1px izquierda / 2px derecha en el lienzo de 16x16.
3. **Prototipado de Nuevos Assets con Gemini 4 Argon**:
   - Emplear el modelo `gemini-4-argon-high` para diseñar una dirección de arte renovada y prototipos de texturas para todos los ítems del mod (módulos, baterías, componentes, linterna táctica, gafas), excluyendo la estación de modificación y el script de instalación.

---

## 2. Fase 1: Limpieza y Eliminación de `chunk_viewer_module`
- [x] Remover `CHUNK_VIEWER_MODULE` de `ModItems.java`.
- [x] Remover `CHUNK_VIEWER_MODULE` de `ModTabs.java`.
- [x] Remover `CHUNK_VIEWER_MODULE` de `ModificationStationBlockEntity.java`.
- [x] Eliminar receta `chunk_viewer_module_recipe.json`.
- [x] Limpiar entradas en todos los archivos de localización `lang/*.json`.

---

## 3. Fase 2: Corrección y Centrado de Texturas Existentes
- [x] Análisis de bounding box (`img.getbbox()`) en todos los archivos PNG.
- [x] Desplazamiento horizontal matemático (`(canvas_w - sprite_w) / 2`) aplicado a los 13 módulos, batería y linterna.
- [x] Verificación en marco de 16x16 y 32x32.

---

## 4. Fase 3: Consulta y Dirección de Arte con Gemini 4 Argon
- [ ] Ejecutar sesión de diseño con `gemini-4-argon-high` vía CLI `agy`.
- [ ] Generar paletas cromáticas tácticas y matrices de píxeles para prototipos.
- [ ] Implementar generador de prototipos en `docs/prototipos_texturas/` para evaluación del usuario sin romper los assets activos actuales.
- [ ] Desplegar JARs y presentar opciones al usuario.
