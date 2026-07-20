# Vision Goggles – Roadmap Oficial y Checklist

Este documento combina la visión pública del proyecto con los requisitos técnicos para cada fase. Ha sido revisado para **evitar la sobreingeniería**, manteniendo todas las funcionalidades clave.

---

## v1.0.0 – Cimientos y HUD Vital (Fase 1)
**Objetivo:** Refactorizar la arquitectura principal y entregar un HUD estable y funcional.

### 🛠️ Tareas Técnicas
- [x] **Refactorizar el renderizador (`VisionRenderer`):**
    - [x] Desacoplar la lógica de shaders y GUI en `VisionShaderManager` y `VisionHUDOverlay`.
    - [x] Implementar un sistema simple de widgets con `IHudModule`.
    - [x] Eliminar Reflection; usar Mixins o Accessors seguros.
- [x] **Actualización de Assets:**
    - [x] Rediseñar texturas de Gafas Hidro y Bio.
    - [x] Unificar el estilo visual de los módulos.
    - [x] Actualizar la textura y modelo de la *Modification Station*.

### ✨ Funcionalidades Incluidas
- [x] **Módulo de Información Vital:** Coordenadas XYZ (Overworld/Nether), Saturación, Alerta de durabilidad baja.
- [x] **Módulo de Entorno Básico:** Contador de oxígeno bajo el agua, overlay de daño en la lente.
- [x] **Configuración:** Visuales del lado del cliente a través de Cloth Config.

---

## v1.1.0 – Backend, Seguridad y Estabilidad (Fase 2a)
**Objetivo:** Eliminar deuda técnica del servidor, asegurar el multijugador e implementar iluminación confiable.

### 🛠️ Tareas Técnicas
- [x] **Refactorización de `ModularGogglesItem`:**
    - [x] Migrar NBT de `String` a `ResourceLocation`.
    - [x] Implementar un `IPowerSourceModule` simple para solar/batería.
- [x] **Refactorización de la *Modification Station*:**
    - [x] Mover crafteo fuera de `tick()`, prevenir dupeo con tolvas, centralizar lógica de instalación.
- [x] **Configuración:** Separar configuración Cliente/Común; solo Admin/OP puede sincronizar cambios de balance.

---

## v1.2.0 – Balance, Restricciones y Dependencias (Fase 2b)
**Objetivo:** Aplicar reglas de juego, progresión y dependencias de plataforma.

### ✨ Funcionalidades y Reglas
- [ ] **Sistema de slots:** Especializadas (1), Modulares (2), Pro (4).
- [ ] **Dependencias:** Curios (Forge) y Accessories (Fabric) obligatorios.
- [ ] **Incompatibilidades:** Bloquear Solar en Hidro, Solar/Batería en Bio, evitar sensores incompatibles (NVG + Térmico).
- [ ] **Energía:** Drenaje pasivo en standby, penalización solar por durabilidad.

---

## v1.3.0 – Navegación Avanzada e Inmersión (Fin de Fases 1 y 2)
**Objetivo:** Pulir el HUD 2D y el feedback de audio.

- [ ] **Elytra HUD:** Altitud, Velocidad, Pitch.
- [ ] **Audio:** Zumbido de Visión Nocturna, Alerta de oxígeno, Sonido de apagado.
- [ ] **Finalización:** Sin más cambios en la arquitectura 2D.

---

## v2.0.0 – Renderizado 3D y Óptica Avanzada Experimental (Fase 3)
**Objetivo:** Introducir renderizado 3D de forma segura, añadir overlays técnicos e implementar efectos visuales complejos.

- [x] **Infraestructura 3D:** Pipeline simple del lado del cliente específico para el mod.
- [x] **Módulo de Seguridad de Spawn:** Overlay de niveles de luz con tecla dedicada (Radio: 32 bloques).
- [x] **Módulo Visor de Chunks:** Renderiza los bordes de los chunks dinámicamente (similar a F3+G).
- [x] **Gestión de overlays:** Sistema de prioridad simple para información 3D.
- [x] **Shaders:** Interferencia térmica (Nether), deslumbramiento/ceguera para NVG.
- [x] **Compatibilidad:** Asegurar funcionamiento con Sodium/Iris sin romper nada. Evitar crear APIs abstractas o registros genéricos.

---

## v3.0.0 – Integración con el Mundo (Fase 4a)
**Objetivo:** Integrar el mod de forma natural en Minecraft.

- [ ] **Aldeano Óptico:** Profesión, estación de trabajo, comercios.
- [ ] **Botín estructural:** Ancient Cities, Bastiones, Minas.

---

## v3.1.0 – Estética y Personalización (Fase 4b)
**Objetivo:** Personalización estética para jugadores.

- [ ] **Cosmética:** Lentes teñibles.
- [ ] **Temas del HUD:** Retro, Cyber, Rusty. Usar temas predefinidos, sin motores complejos.

---

## v3.2.0 – Ecosistema y Finalización (Fase 4c)
**Objetivo:** Asegurar compatibilidad y finalizar funciones.

- [ ] **Compatibilidad:** Soporte FE/Energy (Mekanism, Create).
- [ ] **Integración:** JEI / REI plugins.
- [ ] **Progresión:** Logros y balance final.

---

## Principio del Roadmap
> **Regla:** Si no se necesita ahora, no lo abstraigas. Cada versión debe ser entregable y centrada en Vision Goggles, no en un framework genérico.
