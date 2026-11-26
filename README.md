# Vision Goggles — Minecraft 1.20.1 (v2.0.0)

**Platform:** Minecraft `1.20.1` (`Forge` & `Fabric` via Architectury) | **Java:** `17`

Vision Goggles 2.0.0 is a tactical optics and lighting mod featuring custom GLSL post-processing shaders, 3D GeckoLib 4 Tri-Lens goggles with first- and third-person visor animations, an interactive 3D Modification Station, a dynamic Tactical Flashlight with real-time shadows (Vanilla, Sodium & Embeddium), and a sandboxed LuaJ 3.0.1 HUD/AR scripting engine.

---

## Key Features

- **4 Core Vision Modes + Modular Variants**:
  - **Night Vision (`NVG`)**: Pure client-side phosphor amplification (no vanilla potion icons/particles), P43 persistence, solar/explosion/muzzle-flash glare, and optional DSP auto-gating.
  - **Thermal Vision (`FLIR`)**: Isolated `ThermalMask` depth-occluded FBO with Ironbow heat palette, world-up 3D gradients, cryogenic/combustion block signatures, and **tiered Vanilla armor thermal insulation** (`thermal_insulation_{low,medium,high,full}` tags).
  - **Biometric Vision**: Organic health/status signature scanning with native crosshair Target Lock-On HUD (IFF, HP bar, armor & distance up to `32m`).
  - **Hydro Vision**: Underwater clarity & depth-tuned optical filter with electromagnetic **2× O2 Tactical Rebreather** (50% slower oxygen drain), **+20% Hydrodynamic Swim Propulsion**, and **Submerged Aqua-Mining** (removes underwater mining penalty while consuming extra battery in water).
  - **Modular & Pro Modular Goggles**: Customize optics, zoom, solar charging, battery expansion, sonar radar (with 3D acoustic echo projection), flashlight pod, and Lua programmable chips.
- **Tactical Field Manual (`tactical_manual`) & Public Addon API**: Craftable in-game guide covering all goggles, the 10 active hardware modules, flashlight/strobe modes, and station assembly across 16 languages, plus `VisionGogglesAPI` & `VisionGogglesClientAPI`.
- **Interactive 3D Modification Station**: Left-docked 2-tab tactical UI (*Assemble* piece-by-piece on a 3D cutting mat with player hand animations, and *Modify / Repair* for module installation, extraction, Lua flashing, and durability repair).
- **Tactical Flashlight & Strobe**: Handheld or `body`-slot flashlight (plus modular goggle pod) with 4 modes (*Focused*, *Wide*, *Close*, and *10 Hz Tactical Strobe* with mob deterrence and calibrated NVG glare).
- **Sandboxed LuaJ 3.0.1 Engine**: 6 bundled tactical HUD/AR/Shader scripts (`01_radar_tactico_360.lua` – `06_suite_termica_flir.lua`), world-to-screen AR projection, and `/vg` commands.
- **16 Languages**: Full localization across 16 languages (`180+` keys).

---

## Requirements & Compatibility

- **Java:** `17`
- **Loaders:** Minecraft Forge `1.20.1` or Fabric Loader + Fabric API `1.20.1`
- **Required Dependencies:**
  - Architectury API
  - Cloth Config
  - GeckoLib 4
  - Curios (`Forge`) / Accessories (`Fabric`)
- **Optional Integrations:**
  - **JEI** (Just Enough Items) — *Assemble* & *Modify* recipe categories
  - **Sodium / Embeddium** — Dynamic flashlight chunk & entity shader support
  - **Iris / Oculus** — Automatic shaderpack detection with crash-safe bypass for vision modes. **Note:** Dynamic flashlight (Tactical Flashlight & Flashlight Module) does **not** work when an Iris/Oculus shaderpack is active.

---

## Controls

| Key / Input | Action |
|---|---|
| `N` (Quick Tap) | Flip goggles visor up/down (toggle active vision) |
| `N` (Hold) | Open **Tactical Radial Wheel** (switch modes, toggle zoom lock, flashlight, sonar, smart battery recharge) |
| `V` + Mouse Wheel | Continuous optical zoom (`1.0x – 8.0x`, requires Zoom Module) |
| `L` | Toggle Tactical / Modular Flashlight |
| `K` | Cycle Flashlight Beam Mode (*Focused / Wide / Close / Strobe*) |
| `Right-Click` (Hold) | Equip goggles from hand with 3D animation |
