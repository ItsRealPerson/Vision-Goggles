# Plan v20: Oclusión Solar, Fogonazo de TNT y Ergonomía de Linterna

## 1. Diagnóstico de Problemas y Requerimientos

### A. Ceguera Solar a través de Bloques ("Sol Troll")
- **Problema:** Mirar en dirección al sol causaba deslumbramiento máximo en las gafas NVG incluso estando dentro de una cueva, casa o detrás de una pared sólida.
- **Causa:** En `VisionShaderManager.java`, solo se calculaba el producto escalar entre el vector de visión del jugador y el vector del sol (`sunDot > 0.60`), sin comprobar si había bloques físicos obstruyendo la línea de visión hacia el astro.
- **Solución:**
  - Realizar un trazado de rayos (`mc.level.clip`) con `ClipContext.Block.VISUAL` desde los ojos del jugador hacia el sol a una distancia de 128 bloques.
  - Solo si el raycast no colisiona con ningún bloque (`HitResult.Type.MISS`), se aplica el fogonazo de ceguera solar directa. Si hay un bloque intermedio, la luz solar directa queda 100% bloqueada.

---

### B. Falta de Fogonazo con TNT y Explosiones
- **Problema:** Al detonar TNT, Creepers o bolas de fuego cerca del jugador con NVG puestas, no se producía ningún destello de sobrecarga fotónica.
- **Causa:** El evento `EntityEvent.LIVING_HURT` en `VisionRenderer.java` se registraba con la condición `entity.level().isClientSide`. Sin embargo, `LIVING_HURT` en Minecraft/Architectury es un evento exclusivo del servidor, por lo que nunca se ejecutaba en el cliente. Además, una explosión debe cegar al operador por el estallido lumínico cercano sin requerir que reciba daño físico.
- **Solución:**
  - Crear `MixinClientPacketListener` en `common` para interceptar `handleExplosion(ClientboundExplodePacket)`.
  - Cuando detona una explosión a menos de 36 bloques del jugador, se calcula la proximidad y se dispara `VisionShaderManager.triggerFlash(intensity)` de forma reactiva.
  - Interceptar `handleAddEntity(ClientboundAddEntityPacket)` para detectar rayos (`EntityType.LIGHTNING_BOLT`) cercanos y detonar el fogonazo.
  - Registrar el parpadeo de daño en `onClientTick` cuando `player.hurtTime == 9`.

---

### C. Animación de Mano Bajando/Subiendo al Usar Linterna
- **Problema:** Encender, apagar o ciclar la linterna táctica en mano (o el simple drenaje de batería tick a tick) provocaba que la mano en primera persona bajara y volviera a subir repetidamente.
- **Causa:** Minecraft Vanilla compara `ItemStack.matches(...)` cada tick. Al modificarse los tags NBT `TAG_FLASHLIGHT_ACTIVE` o `TAG_BATTERY`, considera que el ítem cambió y ejecuta la animación de re-equipado (`mainHandHeight` cae a 0 y sube a 1).
- **Solución:**
  - Crear la interfaz `CustomReequipItem` en `common` e implementarla en `TacticalFlashlightItem` con `shouldCauseReequipAnimation(...)` devolviendo `false` si el ítem permanece en la misma ranura.
  - En Forge, esto se enlaza automáticamente con el hook nativo de `IForgeItem`.
  - En Fabric, crear `ItemInHandRendererMixin` para omitir la animación de re-equipado en ítems que implementen `CustomReequipItem`.

---

### D. Ergonomía del Estroboscópico Táctico en `flashlight.fsh`
- **Problema:** El parpadeo a onda cuadrada instantánea (0% negro a 100% blanco) a alta frecuencia genera fatiga visual severa y mareo.
- **Solución:**
  - Adoptar el estándar táctico de simulación (*Ready or Not* / *Escape from Tarkov*): **Luz Base Ambiental + Pulsos de Sobretensión**.
  - Mantener un 30% de iluminación ambiental continua para que el ojo no sufra el impacto negro/blanco.
  - Modular los pulsos a ~9 Hz con una rampa de onda suave y mantener el cono táctico intermedio (cono de 35°–45°).
  - Conservar la mecánica de disuasión de mobs (`FlashlightDeterrence`).
