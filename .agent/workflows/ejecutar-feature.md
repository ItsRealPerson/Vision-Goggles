---
description: Ejecución continua autónoma de features: escribe código/assets a disco, resuelve bloqueos con auto-escalamiento y compila limpio.
---
1. Verifica la versión de Minecraft y plataforma del proyecto siguiendo `AGENTS.md` y toma el Plan de Implementación en `docs/planes/` (o créalo de inmediato si es una instrucción directa).
2. Escribe o modifica todas las clases Java, archivos YAML, `.mcfunction` o JSON directamente en disco sin imprimir bloques gigantes en el chat. Si son muchos assets repetitivos, apóyate en un script de Python en `scripts/`.
3. Si te enfrentas a un problema de alta complejidad (Mixins delicados, Shaders GLSL, concurrencia en Folia) o si la compilación rápida (`compileJava`) falla 2 veces consecutivas por el mismo error:
   - Ejecuta en terminal: `agy --model gemini-4-argon -p "Analiza [error/archivos] respetando AGENTS.md y escribe la solución técnica exacta en docs/planes/solucion_argon.md"` (o usa `invoke_subagent` si la CLI no estuviera en el PATH).
   - Lee el archivo generado, aplica la solución en el código y continúa de inmediato sin pedir intervención al usuario.
4. Valida una única vez con el comando de compilación rápida correspondiente (`./gradlew :common:compileJava`, `./gradlew compileJava` o validación JSON si es datapack).
5. Documenta el resultado en `docs/CHANGELOG.md` y `AGENTS.md` § 7 hasta dejar el código compilando limpio.
