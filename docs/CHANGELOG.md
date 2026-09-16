# Changelog - Vision Goggles

## [2.0.0] - 2026-09-16

### Fixed
- **JPMS Split-Package Crash with Figura / Superb Warfare**: Relocated shaded org.luaj classes into dev.itsrealperson.vision_goggles.shadow.org.luaj via shadowJar in both Forge and Fabric builds. Resolves ResolutionException: Modules vision_goggles and luaj.core.figura export package org.luaj.vm2 to module superbwarfare.
- **Chest Flashlight Orientation**: Fixed front facing direction and upright orientation on the player torso layer in GogglesRenderLayer.
- **Station Model Rotation**: Fixed invalid -20.0 degree rotation on screen_display to -22.5 to prevent JsonParseException missing texture block.
- **Smart Battery Recharging**: Implemented smart target resolution prioritizing held items and automatically choosing the most depleted equipped wearable.
- **Item Textures**: Updated electrical parts sprite to a 16x16 Minecraft-style handful of electronic components.
