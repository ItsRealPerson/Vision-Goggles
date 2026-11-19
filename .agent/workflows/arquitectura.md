---
description: Autodetecta versión/plataforma, diseña la arquitectura e implementa de corrido en flujo continuo 100% autónomo sin pausas.
---
1. Identifica primero si el proyecto es Mod, Plugin o Datapack y su versión exacta (1.20.1 a 26.3) leyendo únicamente el descriptor raíz según la sección 1 de `AGENTS.md`.
2. Analiza únicamente los archivos estrictamente necesarios respetando `.agyignore` y las restricciones de `AGENTS.md`.
3. Genera el Plan de Implementación en `docs/planes/plan_vX.md` detallando arquitectura, firmas exactas (según versión de MC), concurrencia/red y archivos a crear o modificar.
4. Continúa inmediatamente sin detenerte ni pedir confirmaciones:
   - Escribe y modifica las clases, YAMLs, JSONs o scripts Python directo en disco.
   - Si la tarea presenta alta complejidad (Mixins delicados, Shaders GLSL, concurrencia en Folia) o la compilación falla 2 veces seguidas por el mismo error, ejecuta escalamiento automático por terminal con `agy --model gemini-4-argon -p "Analiza [error/archivos] respetando AGENTS.md y escribe la solución técnica exacta en docs/planes/solucion_argon.md"` (o `invoke_subagent`), aplica la solución y continúa.
5. Valida una única vez con la compilación rápida correspondiente (`compileJava` o `mvn test-compile`), registra bugs en `AGENTS.md` § 7 y actualiza `docs/CHANGELOG.md` hasta dejar el código compilando limpio.
