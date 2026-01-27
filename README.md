# Vision Goggles - Minecraft 1.21.1 (Branch: 1.21.1)

**Estado: Migración en Progreso (Inestable)**

Este branch contiene el porte del mod Vision Goggles a Minecraft 1.21.1 utilizando Architectury.

## 🚀 Lo que funciona:
- **Carga del Mod**: El mod inicia correctamente en Fabric y NeoForge.
- **Data Components**: Migración total de NBT a los nuevos Data Components de Minecraft 1.21.
- **Networking**: Sistema de red migrado al nuevo sistema de Payloads (1.21+).
- **Traducciones**: Modos de visión y nombres de ítems corregidos.
- **Zoom (Fabric)**: El zoom funciona perfectamente en la versión de Fabric.
- **Accesorios**: Slots de 'Face' (Fabric) y 'Eyes' (NeoForge/Curios) registrados.

## ⚠️ Bugs Conocidos (En corrección):
- **Estación de Modificación (Fabric)**: 
  - El resultado no se visualiza en el slot de salida hasta que se interactúa.
  - El consumo de materiales es instantáneo.
- **Zoom (NeoForge)**: El zoom no se aplica correctamente a pesar del Mixin (investigando conflictos de FOV).
- **Batería (Modular)**: En Fabric, el módulo de batería sigue contando como un slot de módulo normal.

## 🛠️ Requisitos:
- Java 21
- Fabric Loader / NeoForge 21.1
- Architectury API 13+
- Cloth Config v15
- Accessories (Fabric) / Curios (NeoForge)
