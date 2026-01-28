# Vision Goggles – Official Roadmap & Checklist

This document combines the public vision of the project with the technical requirements for each development phase. It has been reviewed and adjusted to **avoid over-engineering** while keeping all core functionality and milestones.

---

## v1.0.0 – Foundations and Vital HUD (Phase 1)
**Goal:** Refactor core architecture and deliver a stable, functional HUD.

### 🛠️ Technical Tasks
- [ ] **Refactor the renderer (`VisionRenderer`):**
    - [ ] Decouple shader logic and GUI rendering into `VisionShaderManager` and `VisionHUDOverlay`.
    - [ ] Implement a simple widget system with `IHudModule`.
    - [ ] Remove reflection; use safe Mixins or Accessors.
- [ ] **Asset updates:**
    - [ ] Redesign Hydro and Bio Goggles textures.
    - [ ] Unify visual style of modules.
    - [ ] Update Modification Station texture and model.

### ✨ Included Features
- [ ] **Vital Information Module:** XYZ coordinates (Overworld/Nether), Saturation, Low Durability alert.
- [ ] **Basic Environment Module:** Underwater oxygen counter, lens damage overlay.
- [ ] **Configuration:** Client-side visuals only via Cloth Config.

---

## v1.1.0 – Backend, Security, and Stability (Phase 2a)
**Goal:** Remove server technical debt and secure multiplayer.

### 🛠️ Technical Tasks
- [ ] **ModularGogglesItem refactor:**
    - [ ] Migrate NBT from `String` to `ResourceLocation`.
    - [ ] Implement simple `IPowerSourceModule` for solar/battery.
- [ ] **Modification Station refactor:**
    - [ ] Move crafting out of `tick()`, prevent dupe with hoppers, centralize module installation logic.
- [ ] **Configuration:** Separate Client/Common configuration; only Admin/OP can sync balance changes.

---

## v1.2.0 – Balance, Restrictions, and Dependencies (Phase 2b)
**Goal:** Enforce gameplay rules, progression, and platform dependencies.

### ✨ Features and Rules
- [ ] **Slot system:** Specialized (1), Modular (2), Pro (4).
- [ ] **Dependencies:** Curios (Forge) and Accessories (Fabric) mandatory.
- [ ] **Incompatibilities:** Block Solar on Hydro, Solar/Battery on Bio, prevent incompatible sensors (NVG + Thermal).
- [ ] **Energy:** Passive standby drain, solar penalty by durability.

---

## v1.3.0 – Advanced Navigation and Immersion (End of Phases 1 & 2)
**Goal:** Polish 2D HUD and audio feedback.

- [ ] **Elytra HUD:** Altitude, Speed, Pitch.
- [ ] **Audio:** Night Vision hum, Oxygen alert, Power-off sound.
- [ ] **Finalization:** No further 2D architecture changes.

---

## v2.0.0 – 3D Transition: Safety Systems (Phase 3a)
**Goal:** Introduce 3D world rendering safely.

- [ ] **3D Infrastructure:** Single client-side pipeline specific to Vision Goggles.
- [ ] **Spawn Safety module:** Light level overlay with dedicated keybinding.
- [ ] **Compatibility:** Ensure cooperation with Sodium/Iris. Avoid creating generic registries or abstract APIs.

---

## v2.1.0 – Technical Vision & Automation (Phase 3b)
**Goal:** Add tools for automation and technical players.

- [ ] **Automation modules:** Hopper flow viewer and Villager link visualization (Bed/Workstation).
- [ ] **Chunk viewer:** Visualize active and loaded chunks.
- [ ] **Overlay management:** Simple priority system for 3D information.

---

## v2.2.0 – Advanced Optics & Shaders (Phase 3c)
**Goal:** Implement complex visual effects.

- [ ] **Shaders:** Thermal interference (Nether), glare/blindness for NVG. Isolated implementations only.

---

## v3.0.0 – World Integration (Phase 4a)
**Goal:** Integrate mod naturally into Minecraft.

- [ ] **Optics Villager:** Profession, workstation, trades.
- [ ] **Loot structures:** Ancient Cities, Bastions, Mineshafts.

---

## v3.1.0 – Aesthetics & Customization (Phase 4b)
**Goal:** Cosmetic customization for players.

- [ ] **Cosmetics:** Dyeable lenses.
- [ ] **HUD themes:** Retro, Cyber, Rusty. Use hardcoded themes, no complex engines.

---

## v3.2.0 – Ecosystem & Finalization (Phase 4c)
**Goal:** Ensure compatibility and finalize features.

- [ ] **Compatibility:** FE/Energy support (Mekanism, Create).
- [ ] **Integration:** JEI / REI plugins.
- [ ] **Progression:** Advancements and final balance pass.

---

## Roadmap Principle
> **Rule:** If it’s not needed now, do not abstract it. Every version should be shippable and focused on Vision Goggles, not a generic framework.