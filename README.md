# Vision Goggles - Minecraft 1.21.1 (Port in Progress)

**Branch Status: Unstable Build**

This branch contains the port of Vision Goggles to Minecraft 1.21.1 using the Architectury API. It features a complete overhaul of the data and networking systems to comply with modern Minecraft standards.

## 🌟 Overview
Vision Goggles is a modular equipment mod that adds high-tech eyewear to Minecraft. From Night Vision to Biometric scanning, these goggles provide tactical advantages while maintaining a balanced gameplay experience through a battery-consumption system.

## 🚀 Current Progress (1.21.1)
- **Data Components Migration**: Fully migrated from NBT (`CompoundTag`) to the new native Data Components system.
- **Payload Networking**: Networking has been rewritten to use the new `Payload` system required by NeoForge 21 and Architectury 13.
- **Platform Support**: Basic support for both **Fabric** and **NeoForge**.
- **Visuals**: Modernized rendering system using `VertexConsumer` and updated Shaders.
- **Accessories**: Automated slot registration for 'Face' (Accessories mod on Fabric) and 'Eyes' (Curios API on NeoForge).

## ⚠️ Known Bugs & Issues (Priority for next session)
- **Modification Station (Fabric)**: 
  - Prediction slot (Slot 3) is currently invisible due to sync issues.
  - Item consumption is instant instead of being deferred to the withdrawal action.
- **Zoom (NeoForge)**: The FOV modifier mixin is currently being ignored by the NeoForge camera system (investigating conflicts).
- **Battery Upgrade**: In some cases, the battery expansion module incorrectly counts towards the limited module slots.

## 🛠️ Requirements
- **Java**: 21
- **Fabric**: Fabric Loader 0.16+ & Fabric API
- **NeoForge**: 21.1.x
- **Dependencies**: 
  - Architectury API 13.0.8+
  - Cloth Config v15
  - Accessories (Fabric) / Curios (NeoForge)

## 🎮 How to use
1. **Crafting**: Use the Modification Station to install modules into Modular Goggles.
2. **Keybindings**:
   - `N`: Toggle Goggles On/Off.
   - `M`: Cycle through Vision Modes (if multiple modules are installed).
   - `V`: Zoom (requires Zoom Module).
3. **Recharging**: Right-click with a Battery or compatible energy item while wearing goggles to recharge.