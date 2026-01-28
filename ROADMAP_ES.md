# Vision Goggles – Roadmap Oficial y Checklist

Este documento combina la visión pública del proyecto con los requisitos técnicos para cada fase. Ha sido revisado para **evitar la sobreingeniería**, manteniendo todas las funcionalidades clave.

---

## v1.0.0 – Cimientos y HUD Vital (Fase 1)
**Objetivo:** Refactorizar la arquitectura principal y entregar un HUD estable y funcional.

### 🛠️ Tareas Técnicas
- [ ] **Refactorizar el renderizador (`VisionRenderer`):**
    - [ ] Desacoplar la lógica de shaders y GUI en `VisionShaderManager` y `VisionHUDOverlay`.
    - [ ] Implementar un sistema simple de widgets con `IHudModule`.
    - [ ] Eliminar Reflection; usar Mixins o Accessors seguros.
- [ ] **Actualización de Assets:**
    - [ ] Rediseñar texturas de Gafas Hidro y Bio.
    - [ ] Unificar el estilo visual de los módulos.
    - [ ] Actualizar la textura y modelo de la *Modification Station*.

### ✨ Funcionalidades Incluidas
- [ ] **Módulo de Información Vital:** Coordenadas XYZ (Overworld/Nether), Saturación, Alerta de durabilidad baja.
- [ ] **Módulo de Entorno Básico:** Contador de oxígeno bajo el agua, overlay de daño en la lente.
- [ ] **Configuración:** Visuales del lado del cliente a través de Cloth Config.

---

## v1.1.0 – Backend, Seguridad y Estabilidad (Fase 2a)
**Objetivo:** Eliminar deuda técnica del servidor y asegurar el multijugador.

### 🛠️ Tareas Técnicas
- [ ] **Refactorización de `ModularGogglesItem`:**
    - [ ] Migrar NBT de `String` a `ResourceLocation`.
    - [ ] Implementar un `IPowerSourceModule` simple para solar/batería.
- [ ] **Refactorización de la *Modification Station*:**
    - [ ] Mover crafteo fuera de `tick()`, prevenir dupeo con tolvas, centralizar lógica de instalación.
- [ ] **Configuración:** Separar configuración Cliente/Común; solo Admin/OP puede sincronizar cambios de balance.

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

## v2.0.0 – Transición al 3D: Sistemas de Seguridad (Fase 3a)
**Objetivo:** Introducir renderizado 3D de forma segura.

- [ ] **Infraestructura 3D:** Pipeline simple del lado del cliente específico para el mod.
- [ ] **Módulo de Seguridad de Spawn:** Overlay de niveles de luz con tecla dedicada.
- [ ] **Compatibilidad:** Asegurar funcionamiento con Sodium/Iris sin romper nada. Evitar crear APIs abstractas o registros genéricos.

---

## v2.1.0 – Visión Técnica y Automatización (Fase 3b)
**Objetivo:** Herramientas para jugadores técnicos y automatización.

- [ ] **Módulos de automatización:** Visor de flujo de tolvas y vínculo de aldeanos (Cama/Mesa).
- [ ] **Visor de Chunks:** Visualizar chunks activos y cargados.
- [ ] **Gestión de overlays:** Sistema de prioridad simple para información 3D.

---

## v2.2.0 – Óptica Avanzada y Shaders (Fase 3c)
**Objetivo:** Implementar efectos visuales complejos.

- [ ] **Shaders:** Interferencia térmica (Nether), deslumbramiento/ceguera para NVG. Implementaciones aisladas únicamente.

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
- [ ] **Integración:** Plugins para JEI / REI.
- [ ] **Progresión:** Logros y balance final.

---

## Principio del Roadmap
> **Regla:** Si no se necesita ahora, no lo abstraigas. Cada versión debe ser entregable y centrada en Vision Goggles, no en un framework genérico.