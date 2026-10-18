# Changelog - Vision Goggles

## [2.0.0] - 2026-09-16

### Fixed
- **JPMS Split-Package Crash with Figura / Superb Warfare**: Relocated shaded `org.luaj` classes into `dev.itsrealperson.vision_goggles.shadow.org.luaj` via `shadowJar` and excluded `META-INF/services/**` (javax.script.ScriptEngineFactory) in both Forge and Fabric builds. Resolves `ResolutionException: Modules luaj.jse.figura and vision_goggles export package org.luaj.vm2.script`.
- **Chest Flashlight Orientation**: Fixed front facing direction and upright orientation on the player torso layer in `GogglesRenderLayer`.
- **Station Model Rotation**: Fixed invalid `-20.0` degree rotation on `screen_display` to `-22.5` to prevent `JsonParseException` missing texture block.
- **Smart Battery Recharging**: Implemented smart target resolution prioritizing held items and automatically choosing the most depleted equipped wearable.
- **Item Textures**: Updated electrical parts sprite to a 16x16 Minecraft-style handful of electronic components.
- **Flashlight Mode Coherent Beam & Static**: Optical geometry (hotspot, spill, cutoff) now adapts per mode. Volumetric static/dust is strictly bound to photons inside the illuminated cone, eliminating the oversized noise circle in the pitch-black sky.
