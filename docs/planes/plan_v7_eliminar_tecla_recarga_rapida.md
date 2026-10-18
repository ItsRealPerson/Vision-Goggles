# Plan v7: Eliminación de Tecla Física de Recarga Rápida y Exclusividad en Menú Radial

## 1. Contexto y Requerimiento del Usuario
El usuario solicitó:
> *"otra cosa, mejor saca la tecla de recarga rapida, que se haga directo con el menu radial"*

### Objetivos:
1. Eliminar la asignación de tecla física dedicada (`rechargeKey`, tecla 'B') de `ModKeyMappings.java`.
2. Eliminar la verificación de pulsación en `VisionRenderer.java`.
3. Limpiar las traducciones de la tecla `key.vision_goggles.recharge` en los 12 archivos de idioma para no dejar entradas huérfanas en la pantalla de Controles de Minecraft.
4. Mantener la opción **BATERÍA** ("Recarga Rápida") activa y disponible dentro del **Menú Radial Táctico (`N`)** cuando el jugador posea equipo recargable (gafas o linterna).
5. Compilar mediante subagente, desplegar a ATLauncher, registrar en CHANGELOG y subir a GitHub.
