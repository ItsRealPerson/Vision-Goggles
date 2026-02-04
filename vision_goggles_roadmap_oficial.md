# Vision Goggles – Official Roadmap & Checklist

This document combines the public vision of the project with the technical requirements for each development phase. It has been reviewed and adjusted to **avoid over-engineering** while keeping all core functionality and milestones. It is ready for GitHub publication.

---

## v1.0.0 – Foundations and Vital HUD (Phase 1)

**Goal:** Refactor core architecture and deliver a stable, functional HUD.

### Technical Tasks

* Refactor the renderer (`VisionRenderer`):

  * Decouple shader logic and GUI rendering into `VisionShaderManager` and `VisionHUDOverlay`.
  * Implement widget-based system with `IHudModule`.
  * Remove reflection; use safe Mixins or Accessors.
* Asset updates:

  * Redesign Hydro and Bio Goggles textures.
  * Unify visual style of modules.
  * Update Modification Station texture and model.

### Included Features

* Vital Information Module: XYZ coordinates (Overworld/Nether), Saturation, Low Durability alert.
* Basic Environment Module: Underwater oxygen counter, lens damage overlay.
* Client-side configuration via Cloth Config.

**Note:** No 3D rendering or advanced effects included.

---

## v1.1.0 – Backend, Security, and Stability (Phase 2a)

**Goal:** Remove server technical debt and secure multiplayer.

### Technical Tasks

* ModularGogglesItem: Migrate NBT from `String` to `ResourceLocation`; implement simple `IPowerSourceModule` for solar/battery.
* Modification Station: Move crafting out of `tick()`, prevent dupe with hoppers, centralize module installation logic.
* Client/Common configuration separation; only Admin/OP can sync balance changes.

**Note:** Keep abstractions minimal; implement only what is needed now.

---

## v1.2.0 – Balance, Restrictions, and Dependencies (Phase 2b)

**Goal:** Enforce gameplay rules, progression, and platform dependencies.

### Features and Rules

* Slot system: Specialized (1), Modular (2), Pro (4).
* Dependencies: Curios (Forge) and Accessories (Fabric) mandatory for new installations.
* Incompatibilities: Block Solar on Hydro, Solar/Battery on Bio, prevent incompatible sensors (NVG + Thermal).
* Energy: Passive standby drain, solar penalty by durability.

**Note:** Keep slot and dependency logic simple; avoid generic extensible frameworks.

---

## v1.3.0 – Advanced Navigation and Immersion (End of Phases 1 & 2)

**Goal:** Polish 2D HUD and audio feedback.

* Elytra HUD: Altitude, Speed, Pitch.
* Audio: Night Vision hum, Oxygen alert, Power-off sound.
* HUD finalization: No further 2D architecture changes.

**Note:** Avoid micro-optimizing or extending HUD beyond planned modules.

---

## v2.0.0 – 3D Transition: Safety Systems (Phase 3a)

**Goal:** Introduce 3D world rendering safely.

### Adjusted Implementation

* Single client-side pipeline, specific to Vision Goggles.
* Single overlay: Spawn Safety.
* Keybinding for toggle.
* Compatibility check: ensure works with Sodium/Iris without breaking.
* **Do not create generic registries, factories, or abstract APIs.**

---

## v2.1.0 – Technical Vision & Automation (Phase 3b)

**Goal:** Add tools for automation and technical players.

* Hopper flow viewer.
* Villager link visualization (Bed/Workstation).
* Chunk viewer: visualize active and loaded chunks.
* Overlay priority system to prevent 3D information overlap.

**Note:** Keep modules hardcoded and simple; avoid dynamic or extensible systems.

---

## v2.2.0 – Advanced Optics & Shaders (Phase 3c)

**Goal:** Implement complex visual effects.

* Shaders and optics effects: Thermal interference (Nether), glare/blindness for NVG.

**Note:** Effects must be isolated and tied to Vision Goggles; no general-purpose shader frameworks.

---

## v3.0.0 – World Integration (Phase 4a)

**Goal:** Integrate mod naturally into Minecraft.

* Optics Villager: profession, workstation, trades.
* Loot structures: Ancient Cities, Bastions, Mineshafts.

**Note:** Implement only what's necessary; avoid generic profession/POI systems.

---

## v3.1.0 – Aesthetics & Customization (Phase 4b)

**Goal:** Cosmetic customization for players.

* Dyeable lenses.
* HUD themes: Retro, Cyber, Rusty.

**Note:** No dynamic HUD theming engine; hardcoded themes only.

---

## v3.2.0 – Ecosystem & Finalization (Phase 4c)

**Goal:** Ensure compatibility with other mods and finalize features.

* Energy compatibility: FE/Energy support (Mekanism, Create).
* JEI / REI plugin integration.
* Advancements and final balance pass.

**Note:** Only implement functional support; avoid open-ended APIs for other mods.

---

## Roadmap Principle

> **Rule:** If it’s not needed now, do not abstract it. Every version should be shippable and focused on Vision Goggles, not a generic framework.

**Result:** This roadmap balances feature growth with disciplined development, preventing over-engineering while allowing safe future expansion.
