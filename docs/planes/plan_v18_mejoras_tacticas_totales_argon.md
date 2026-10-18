# Plan de Desarrollo v18 — Suite de Mejoras Tácticas Integrales con Arquitectura Argon

## 1. Contexto y Objetivos
A partir de la auditoría técnica exhaustiva del código fuente de Vision Goggles y la directriz del usuario ("Me gustan todas xd de paso si es posible usar argom hazlo"), se ejecuta la implementación secuencial continua de los 5 grandes pilares de evolución táctica:

1. **Óptica Realista y Despertar de Shaders:**
   - Conexión del uniform `focus` huérfano en `VisionShaderManager.java` para activar la aberración cromática, contracción de viñeta ocular y pixelado térmico ya programados en GLSL.
   - Deslumbramiento dinámico vectorial (Auto-Gating ante el sol, pozos de lava y explosiones directas) con persistencia de fósforo P43 (burn-in).
   - Paridad de despeje de niebla subacuática en Forge para el modo Hydro (unificación con Fabric).

2. **Estación de Modificación 2.0 (Mantenimiento y Modularidad):**
   - Sistema de desmontaje y extracción: poder recuperar módulos instalados (en forma de ítem físico con su NBT y scripts) liberando sockets de las gafas modulares.
   - Taller de reparación técnica: reparar durabilidad sin penalizaciones de yunque usando `tactical_frame` (100%) o `electrical_parts` (35%).
   - Actualización de integración JEI.

3. **Linterna Táctica Operativa:**
   - 4.º Modo: `STROBE` (12-15 Hz) para aturdir mobs hostiles (Slowness I / retraso de explosión de creepers).
   - Soporte de filtros ópticos utilizando el array RGB `colArray`: Filtro Rojo (no altera fauna) e Iluminador IR (haz solo visible con gafas NVG activas).

4. **Firmas Térmicas de Bloques:**
   - Resaltar en blanco/naranja incandescente bloques activos que emiten calor: hornos, ahumadores, fogatas, lava y magma.
   - Resaltar en azul/negro profundo fuentes de frío extremo: hielo azul, nieve en polvo.

5. **Expansión Lua de Realidad Aumentada (AR):**
   - Implementar `world.worldToScreen(x, y, z)` para proyectar coordenadas 3D del mundo a 2D en pantalla, permitiendo miras tácticas y corchetes flotantes dinámicos en scripts comunitarios.

## 2. Metodología de Ejecución
- Arquitectura orquestada con subagentes especializados y Gemini 4 Argon.
- Compilación y verificación estricta en cada fase (`compileJava`, `build`).
- Despliegue en ATLauncher (`Minecraft1201withForge` y `Minecraft1201withFabric`).
- Registro en `docs/CHANGELOG.md` y sincronización inmediata con GitHub.
