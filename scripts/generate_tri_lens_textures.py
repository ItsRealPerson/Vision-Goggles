"""
Generate production 64x64 base textures and strictly-masked RGBA _glow.png emissive masks
for the Tactical Tri-Lens goggle lineup:
  - tri_lens_nvg (+ _glow)
  - tri_lens_thermal (+ _glow)
  - tri_lens_bio (+ _glow)
  - tri_lens_hydro (+ _glow)
  - tri_lens_modular (+ _glow)
  - tri_lens_pro_modular (+ _glow)
  - chassis_neutral (matte graphite, unlit smoked lenses, NO _glow)

Output directory: common/src/main/resources/assets/vision_goggles/textures/entities/
(Models in models_src/tri_lens_proto/textures/ are left intact.)
All coordinates follow the shared 64x64 per-face UV atlas (2.0 px / model_unit).
"""

from __future__ import annotations

from pathlib import Path
from typing import Dict, List, Optional, Tuple
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "common" / "src" / "main" / "resources" / "assets" / "vision_goggles" / "textures" / "entities"

RGBA = Tuple[int, int, int, int]
Rect = Tuple[int, int, int, int]


def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# Base emissive bounding boxes in (x1, y1, x2, y2) half-open pixel coordinates (9 rects, 76 px).
BASE_GLOW_ALLOWED_RECTS: List[Rect] = [
    (2, 41, 6, 45),    # Hero 4x4 lens core
    (8, 48, 11, 51),   # Hero IR 3x3 lens core
    (14, 41, 18, 45),  # Primary 4x4 ocular lens core
    (23, 40, 26, 43),  # Aux / compact 3x3 ocular lens core
    (31, 40, 35, 42),  # Brow micro-emitter 4x2 core
    (32, 45, 34, 47),  # Micro sensor port 2x2 core
    (41, 41, 42, 43),  # Battery indicator 1x2 bar
    (44, 41, 48, 43),  # Battery indicator 4x2 bar
    (53, 41, 55, 43),  # Status LED 2x2 dot
]

# 14 additional modular bone emissive bounding boxes (strictly >=1px inside host ATLAS_ZONES).
NEW_MODULAR_GLOW_RECTS: List[Rect] = [
    (44, 29, 47, 32),  # mod_flashlight_lens 3x3 core
    (55, 59, 59, 61),  # mod_battery_pack 4x2 charge bar
    (26, 30, 28, 32),  # mod_lua_pcb 2x2 programmable LED
    (2, 31, 4, 33),    # mod_sonar_pod 2x2 ping LED
    (15, 31, 19, 33),  # mod_vital_pod 4x2 pulse bar
    (56, 20, 58, 22),  # mod_env_pod 2x2 atmospheric LED
    (36, 46, 40, 50),  # mod_optics_nvg 4x4 lens core
    (60, 40, 63, 43),  # mod_optics_nvg_aux 3x3 lens core
    (42, 46, 46, 50),  # mod_optics_thermal 4x4 lens core
    (60, 45, 63, 48),  # mod_optics_thermal_aux 3x3 lens core
    (48, 46, 52, 50),  # mod_optics_bio 4x4 lens core
    (1, 48, 4, 51),    # mod_optics_bio_aux 3x3 lens core
    (54, 46, 58, 50),  # mod_optics_hydro 4x4 lens core
    (51, 1, 54, 4),    # mod_optics_hydro_aux 3x3 lens core
]

MODULAR_GLOW_ALLOWED_RECTS: List[Rect] = BASE_GLOW_ALLOWED_RECTS + NEW_MODULAR_GLOW_RECTS

# Mode-specific lens palettes for dedicated mod_optics_* islands in modular / pro_modular textures.
OPTIC_MODE_LENSES: Dict[str, Dict[str, RGBA]] = {
    "nvg": {
        "rim": hex_rgba("#0C3D20"),
        "base": hex_rgba("#14B854"),
        "bright": hex_rgba("#22E86B"),
        "core": hex_rgba("#7CFF1A"),
        "shine": hex_rgba("#E8FFE6"),
        "bezel": hex_rgba("#23272C"),
        "bezel_hi": hex_rgba("#464C54"),
    },
    "thermal": {
        "rim": hex_rgba("#4A1800"),
        "base": hex_rgba("#CC3D00"),
        "bright": hex_rgba("#FF6A00"),
        "core": hex_rgba("#FFAE19"),
        "shine": hex_rgba("#FFF2CC"),
        "bezel": hex_rgba("#7A4E22"),
        "bezel_hi": hex_rgba("#B57A3D"),
    },
    "bio": {
        "rim": hex_rgba("#4D0610"),
        "base": hex_rgba("#B80D22"),
        "bright": hex_rgba("#FF1E2D"),
        "core": hex_rgba("#FF4D6A"),
        "shine": hex_rgba("#FFE6EA"),
        "bezel": hex_rgba("#181A20"),
        "bezel_hi": hex_rgba("#5C6370"),
    },
    "hydro": {
        "rim": hex_rgba("#004455"),
        "base": hex_rgba("#00A8C8"),
        "bright": hex_rgba("#00E5FF"),
        "core": hex_rgba("#66F7FF"),
        "shine": hex_rgba("#E6FDFF"),
        "bezel": hex_rgba("#168A96"),
        "bezel_hi": hex_rgba("#24B8C7"),
    },
}

UNLIT_GLASS: Dict[str, RGBA] = {
    "rim": hex_rgba("#11151C"),
    "base": hex_rgba("#181E28"),
    "bright": hex_rgba("#242D3B"),
    "core": hex_rgba("#1E2633"),
    "shine": hex_rgba("#384457"),
}

PALETTES: Dict[str, Dict] = {
    "tri_lens_nvg": {
        "struct": {
            "deep": hex_rgba("#181B1F"),
            "shadow": hex_rgba("#23272C"),
            "base": hex_rgba("#2A2E33"),
            "mid": hex_rgba("#383E45"),
            "hi": hex_rgba("#464C54"),
            "bevel": hex_rgba("#636A73"),
            "trim": hex_rgba("#7B838E"),
            "gasket": hex_rgba("#1C2024"),
            "accent": hex_rgba("#525A63"),
            "cable": hex_rgba("#202429"),
        },
        "lens": {
            "rim": hex_rgba("#0C3D20"),
            "base": hex_rgba("#14B854"),
            "bright": hex_rgba("#22E86B"),
            "core": hex_rgba("#7CFF1A"),
            "shine": hex_rgba("#E8FFE6"),
        },
        "led": {
            "bar_hi": hex_rgba("#F2F0CE"),
            "bar_lo": hex_rgba("#D8D6B8"),
            "emitter": hex_rgba("#7CFF1A"),
            "dot": hex_rgba("#7CFF1A"),
        },
        "style": "nvg",
        "emit_glow": True,
        "glow_rects": BASE_GLOW_ALLOWED_RECTS,
    },
    "tri_lens_thermal": {
        "struct": {
            "deep": hex_rgba("#14161A"),
            "shadow": hex_rgba("#1C1F24"),
            "base": hex_rgba("#22252A"),
            "mid": hex_rgba("#343940"),
            "hi": hex_rgba("#5A6068"),
            "bevel": hex_rgba("#78808A"),
            "trim": hex_rgba("#9E6B3B"),
            "gasket": hex_rgba("#181B20"),
            "accent": hex_rgba("#8A5A2F"),
            "cable": hex_rgba("#191C21"),
        },
        "lens": {
            "rim": hex_rgba("#4A1800"),
            "base": hex_rgba("#CC3D00"),
            "bright": hex_rgba("#FF6A00"),
            "core": hex_rgba("#FFAE19"),
            "shine": hex_rgba("#FFF2CC"),
        },
        "led": {
            "bar_hi": hex_rgba("#FFAE19"),
            "bar_lo": hex_rgba("#CC3D00"),
            "emitter": hex_rgba("#FF6A00"),
            "dot": hex_rgba("#FF6A00"),
        },
        "style": "thermal",
        "emit_glow": True,
        "glow_rects": BASE_GLOW_ALLOWED_RECTS,
    },
    "tri_lens_bio": {
        "struct": {
            "deep": hex_rgba("#121318"),
            "shadow": hex_rgba("#181A20"),
            "base": hex_rgba("#1E2026"),
            "mid": hex_rgba("#252830"),
            "hi": hex_rgba("#353942"),
            "bevel": hex_rgba("#5C6370"),
            "trim": hex_rgba("#848D9C"),
            "gasket": hex_rgba("#15171C"),
            "accent": hex_rgba("#484E5A"),
            "cable": hex_rgba("#16181D"),
        },
        "lens": {
            "rim": hex_rgba("#4D0610"),
            "base": hex_rgba("#B80D22"),
            "bright": hex_rgba("#FF1E2D"),
            "core": hex_rgba("#FF4D6A"),
            "shine": hex_rgba("#FFE6EA"),
        },
        "led": {
            "bar_hi": hex_rgba("#FF4D6A"),
            "bar_lo": hex_rgba("#B80D22"),
            "emitter": hex_rgba("#FF1E2D"),
            "dot": hex_rgba("#FF4D6A"),
        },
        "style": "bio",
        "emit_glow": True,
        "glow_rects": BASE_GLOW_ALLOWED_RECTS,
    },
    "tri_lens_hydro": {
        "struct": {
            "deep": hex_rgba("#101A24"),
            "shadow": hex_rgba("#16222E"),
            "base": hex_rgba("#1D2B38"),
            "mid": hex_rgba("#283B4D"),
            "hi": hex_rgba("#334B5E"),
            "bevel": hex_rgba("#5E788C"),
            "trim": hex_rgba("#24B8C7"),
            "gasket": hex_rgba("#168A96"),
            "accent": hex_rgba("#3FD2E0"),
            "cable": hex_rgba("#14202B"),
        },
        "lens": {
            "rim": hex_rgba("#004455"),
            "base": hex_rgba("#00A8C8"),
            "bright": hex_rgba("#00E5FF"),
            "core": hex_rgba("#66F7FF"),
            "shine": hex_rgba("#E6FDFF"),
        },
        "led": {
            "bar_hi": hex_rgba("#66F7FF"),
            "bar_lo": hex_rgba("#00A8C8"),
            "emitter": hex_rgba("#00E5FF"),
            "dot": hex_rgba("#66F7FF"),
        },
        "style": "hydro",
        "emit_glow": True,
        "glow_rects": BASE_GLOW_ALLOWED_RECTS,
    },
    "tri_lens_modular": {
        "struct": {
            "deep": hex_rgba("#1F2329"),
            "shadow": hex_rgba("#2C3139"),
            "base": hex_rgba("#383E47"),
            "mid": hex_rgba("#4D5460"),
            "hi": hex_rgba("#636A73"),
            "bevel": hex_rgba("#8A939E"),
            "trim": hex_rgba("#A4ADB8"),
            "gasket": hex_rgba("#242930"),
            "accent": hex_rgba("#76808C"),
            "cable": hex_rgba("#25282E"),
        },
        "lens": {
            "rim": hex_rgba("#1D4B5E"),
            "base": hex_rgba("#4EB8D9"),
            "bright": hex_rgba("#A8ECFF"),
            "core": hex_rgba("#D9F7FF"),
            "shine": hex_rgba("#FFFFFF"),
        },
        "led": {
            "bar_hi": hex_rgba("#D9F7FF"),
            "bar_lo": hex_rgba("#4EB8D9"),
            "emitter": hex_rgba("#A8ECFF"),
            "dot": hex_rgba("#D9F7FF"),
        },
        "style": "modular",
        "emit_glow": True,
        "glow_rects": MODULAR_GLOW_ALLOWED_RECTS,
    },
    "tri_lens_pro_modular": {
        "struct": {
            "deep": hex_rgba("#0E1015"),
            "shadow": hex_rgba("#151820"),
            "base": hex_rgba("#1D212B"),
            "mid": hex_rgba("#2B313F"),
            "hi": hex_rgba("#3F4759"),
            "bevel": hex_rgba("#667085"),
            "trim": hex_rgba("#E0A42B"),   # Gold CNC anodized trim
            "gasket": hex_rgba("#12151B"),
            "accent": hex_rgba("#B87D14"), # Warm amber chamfer
            "cable": hex_rgba("#13161C"),
            "gold_hi": hex_rgba("#FFD966"),
            "gold_lo": hex_rgba("#7A4E0A"),
        },
        "lens": {
            "rim": hex_rgba("#1B5673"),
            "base": hex_rgba("#389CC4"),
            "bright": hex_rgba("#58C8F0"),
            "core": hex_rgba("#BDF2FF"),
            "shine": hex_rgba("#FFFFFF"),
        },
        "led": {
            "bar_hi": hex_rgba("#BDF2FF"),
            "bar_lo": hex_rgba("#58C8F0"),
            "emitter": hex_rgba("#58C8F0"),
            "dot": hex_rgba("#FFFFFF"),
        },
        "style": "pro_modular",
        "emit_glow": True,
        "glow_rects": MODULAR_GLOW_ALLOWED_RECTS,
    },
    "chassis_neutral": {
        "struct": {
            "deep": hex_rgba("#181A1D"),
            "shadow": hex_rgba("#212428"),
            "base": hex_rgba("#2A2E33"),
            "mid": hex_rgba("#363B42"),
            "hi": hex_rgba("#444A52"),
            "bevel": hex_rgba("#58606A"),
            "trim": hex_rgba("#6B737E"),
            "gasket": hex_rgba("#1C1F24"),
            "accent": hex_rgba("#4D545E"),
            "cable": hex_rgba("#1B1E22"),
        },
        "lens": UNLIT_GLASS,
        "led": {
            "bar_hi": hex_rgba("#242D3B"),
            "bar_lo": hex_rgba("#181E28"),
            "emitter": hex_rgba("#1E2633"),
            "dot": hex_rgba("#242D3B"),
        },
        "style": "neutral",
        "emit_glow": False,
        "glow_rects": [],
    },
}


# Clean non-overlapping 64x64 atlas zones (54 base zones + 26 Master Modular zones = 80 total)
ATLAS_ZONES: Dict[str, Rect] = {
    # --- Existing 54 zones (untouched) ---
    "strap_outer": (0, 0, 19, 3),
    "strap_inner": (0, 4, 19, 7),
    "strap_edge": (0, 8, 19, 9),
    "strap_gasket": (0, 10, 17, 14),
    "rear_buckle": (20, 0, 28, 5),
    "rear_buckle_side": (29, 0, 32, 5),
    "wilcox_plate": (33, 0, 41, 6),
    "wilcox_arm": (42, 0, 44, 5),
    "mount_latch": (45, 0, 47, 3),
    "strap_cap": (48, 0, 49, 3),
    "hinge_saddle": (45, 4, 50, 6),
    "sagittal_cap": (55, 0, 58, 5),
    "sagittal_edge": (55, 6, 56, 11),
    "sagittal_top": (59, 0, 63, 18),
    "strap_edge_vert": (63, 0, 64, 18),
    "brow_front": (20, 7, 37, 11),
    "brow_top": (20, 12, 37, 16),
    "bridge_cap": (38, 7, 44, 12),
    "recessed_interior": (45, 7, 54, 12),
    "chassis_core": (53, 52, 64, 57),
    "chassis_top": (13, 47, 24, 52),
    "barrel_side": (0, 16, 8, 22),
    "barrel_top": (9, 16, 15, 22),
    "knurl_horiz": (16, 16, 18, 21),
    "knurl_vert": (19, 16, 24, 18),
    "hood_lip": (19, 19, 24, 20),
    "bezel_front": (25, 16, 30, 21),
    "bezel_side": (31, 16, 35, 21),
    "eyepiece_rear": (36, 16, 41, 21),
    "edge_trim": (42, 16, 54, 21),
    "armor_side": (0, 23, 10, 29),
    "armor_edge": (11, 23, 13, 33),
    "ear_disc": (14, 23, 20, 29),
    "ear_hub": (21, 23, 24, 26),
    "ear_rim": (21, 27, 23, 33),
    "battery_side": (25, 23, 33, 28),
    "battery_edge": (34, 23, 37, 31),
    "cap_dial": (38, 23, 42, 27),
    "junction_box": (43, 23, 49, 28),
    "heatsink_rails": (50, 23, 58, 31),
    "cable_boot": (0, 34, 12, 38),
    "underside_chassis": (13, 34, 30, 38),
    "secondary_trim": (31, 34, 48, 38),
    "hero_lens_4x4": (0, 39, 8, 47),
    "hero_ir_3x3": (7, 47, 12, 52),
    "primary_lens_4x4": (12, 39, 20, 47),
    "aux_lens_3x3": (22, 39, 27, 44),
    "micro_emitter_4x2": (30, 39, 36, 43),
    "micro_port_2x2": (31, 44, 35, 48),
    "battery_bar": (39, 39, 49, 45),
    "status_dot": (51, 39, 57, 45),
    "vents_rivets": (0, 52, 16, 60),
    "scuffed_alloy": (18, 52, 34, 60),
    "deep_cavity": (36, 52, 52, 60),

    # --- 26 new Master Modular zones (Explorer 1 non-overlapping spec) ---
    "mod_pro_brow": (0, 60, 14, 64),
    "mod_pro_plate": (14, 60, 26, 64),
    "mod_pro_rail": (26, 60, 34, 64),
    "mod_pro_vent": (37, 12, 49, 16),
    "mod_zoom_sleeve": (34, 60, 44, 64),
    "mod_zoom_collar": (58, 23, 63, 28),
    "mod_flashlight_body": (44, 60, 52, 64),
    "mod_flashlight_lens": (43, 28, 48, 33),
    "mod_solar_panel": (48, 31, 58, 38),
    "mod_solar_frame": (58, 31, 64, 38),
    "mod_battery_pack": (52, 57, 62, 63),
    "mod_battery_cap": (37, 28, 43, 34),
    "mod_lua_pcb": (23, 28, 31, 34),
    "mod_sonar_pod": (0, 29, 6, 34),
    "mod_vital_pod": (13, 29, 21, 34),
    "mod_env_pod": (54, 18, 60, 23),
    "mod_optics_collar": (49, 12, 59, 16),
    "mod_optics_base": (24, 45, 30, 51),
    "mod_optics_nvg": (35, 45, 41, 51),
    "mod_optics_thermal": (41, 45, 47, 51),
    "mod_optics_bio": (47, 45, 53, 51),
    "mod_optics_hydro": (53, 45, 59, 51),
    "mod_optics_nvg_aux": (59, 39, 64, 44),
    "mod_optics_thermal_aux": (59, 44, 64, 49),
    "mod_optics_bio_aux": (0, 47, 5, 52),
    "mod_optics_hydro_aux": (50, 0, 55, 5),
}


def verify_non_overlapping_zones(zones: Dict[str, Rect]) -> None:
    items = list(zones.items())
    for i in range(len(items)):
        name_a, (ax1, ay1, ax2, ay2) = items[i]
        assert 0 <= ax1 < ax2 <= 64 and 0 <= ay1 < ay2 <= 64, f"Out of bounds: {name_a}"
        for j in range(i + 1, len(items)):
            name_b, (bx1, by1, bx2, by2) = items[j]
            if ax1 < bx2 and ax2 > bx1 and ay1 < by2 and ay2 > by1:
                raise AssertionError(f"Atlas overlap between {name_a} {items[i][1]} and {name_b} {items[j][1]}")


# Verify zero overlap across all 80 atlas islands at import time!
verify_non_overlapping_zones(ATLAS_ZONES)


def put(img: Image.Image, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < 64 and 0 <= y < 64:
        img.putpixel((x, y), c)


def fill_rect(img: Image.Image, rect: Rect, c: RGBA) -> None:
    x1, y1, x2, y2 = rect
    for y in range(y1, y2):
        for x in range(x1, x2):
            img.putpixel((x, y), c)


def draw_beveled_panel(
    img: Image.Image,
    rect: Rect,
    s: Dict[str, RGBA],
    rivets: bool = True,
    highlight_top: bool = True,
) -> None:
    x1, y1, x2, y2 = rect
    fill_rect(img, rect, s["base"])
    for x in range(x1, x2):
        put(img, x, y1, s["hi"] if highlight_top else s["mid"])
        put(img, x, y2 - 1, s["deep"])
    for y in range(y1, y2):
        put(img, x1, y, s["mid"])
        put(img, x2 - 1, y, s["shadow"])
    if rivets and (x2 - x1) >= 4 and (y2 - y1) >= 4:
        put(img, x1 + 1, y1 + 1, s["bevel"])
        put(img, x2 - 2, y1 + 1, s["bevel"])


def draw_lens_4x4(
    base: Image.Image,
    glow: Optional[Image.Image],
    x0: int,
    y0: int,
    s: Dict[str, RGBA],
    l: Dict[str, RGBA],
    has_outer_bezel: bool = True,
    bezel_pad: int = 2,
    bezel_col: Optional[RGBA] = None,
    bezel_hi: Optional[RGBA] = None,
    emit_glow: bool = True,
) -> None:
    """Draw a 4x4 lens with configurable bezel_pad (2 for 8x8 zones, 1 for 6x6 zones)."""
    if has_outer_bezel and bezel_pad > 0:
        b_outer = bezel_col if bezel_col is not None else s["shadow"]
        b_top = bezel_hi if bezel_hi is not None else s["bevel"]
        fill_rect(base, (x0 - bezel_pad, y0 - bezel_pad, x0 + 4 + bezel_pad, y0 + 4 + bezel_pad), b_outer)
        if bezel_pad >= 2:
            fill_rect(base, (x0 - 1, y0 - 1, x0 + 5, y0 + 5), s["deep"])
        for dx in range(-bezel_pad, 4 + bezel_pad):
            put(base, x0 + dx, y0 - bezel_pad, b_top)
            put(base, x0 + dx, y0 + 3 + bezel_pad, s["deep"])

    grid = [
        [l["rim"],    l["base"],   l["shine"],  l["bright"]],
        [l["base"],   l["core"],   l["core"],   l["shine"]],
        [l["bright"], l["core"],   l["bright"], l["base"]],
        [l["base"],   l["bright"], l["base"],   l["rim"]],
    ]
    for dy in range(4):
        for dx in range(4):
            c = grid[dy][dx]
            put(base, x0 + dx, y0 + dy, c)
            if emit_glow and glow is not None:
                put(glow, x0 + dx, y0 + dy, c)


def draw_lens_3x3(
    base: Image.Image,
    glow: Optional[Image.Image],
    x0: int,
    y0: int,
    s: Dict[str, RGBA],
    l: Dict[str, RGBA],
    bezel_pad: int = 1,
    bezel_col: Optional[RGBA] = None,
    bezel_hi: Optional[RGBA] = None,
    emit_glow: bool = True,
) -> None:
    """Draw a 3x3 lens with crisp diagonal specular shine."""
    if bezel_pad > 0:
        b_outer = bezel_col if bezel_col is not None else s["deep"]
        b_top = bezel_hi if bezel_hi is not None else s["hi"]
        fill_rect(base, (x0 - bezel_pad, y0 - bezel_pad, x0 + 3 + bezel_pad, y0 + 3 + bezel_pad), b_outer)
        for x in range(x0 - bezel_pad, x0 + 3 + bezel_pad):
            put(base, x, y0 - bezel_pad, b_top)
    grid = [
        [l["base"],   l["bright"], l["shine"]],
        [l["bright"], l["core"],   l["bright"]],
        [l["rim"],    l["bright"], l["base"]],
    ]
    for dy in range(3):
        for dx in range(3):
            c = grid[dy][dx]
            put(base, x0 + dx, y0 + dy, c)
            if emit_glow and glow is not None:
                put(glow, x0 + dx, y0 + dy, c)


def draw_modular_attachment_zones(
    base: Image.Image,
    glow: Optional[Image.Image],
    s: Dict[str, RGBA],
    style: str,
    emit_mod_glow: bool,
) -> None:
    """Render all 26 Master Modular atlas zones cleanly inside their dedicated rects."""
    is_pro = style == "pro_modular"
    is_neutral = style == "neutral"

    # 1. Pro Armor zones (obsidian-titanium + gold CNC accents for pro_modular; matte alloy for others)
    gold_hi = s.get("gold_hi", s["trim"])
    gold_lo = s.get("gold_lo", s["accent"])
    for zone_name in ("mod_pro_brow", "mod_pro_plate", "mod_pro_rail", "mod_pro_vent"):
        x1, y1, x2, y2 = ATLAS_ZONES[zone_name]
        draw_beveled_panel(base, (x1, y1, x2, y2), s, rivets=True)
        if is_pro:
            for y in range(y1 + 1, y2 - 1):
                for x in range(x1 + 1, x2 - 1):
                    put(base, x, y, s["mid"] if (x + y) % 2 == 0 else s["deep"])
            for x in range(x1, x2):
                put(base, x, y1, gold_hi if x % 3 == 0 else s["trim"])
                put(base, x, y2 - 1, gold_lo)
        if zone_name == "mod_pro_brow":
            for x in range(x1 + 2, x2 - 2, 2):
                put(base, x, y1 + 1, s["trim"] if is_pro else s["bevel"])
        elif zone_name == "mod_pro_vent":
            for x in range(x1 + 2, x2 - 2, 2):
                put(base, x, y1 + 1, s["deep"])
                put(base, x, y1 + 2, s["deep"])

    # 2. Zoom telescopic sleeve & objective collar
    zx1, zy1, zx2, zy2 = ATLAS_ZONES["mod_zoom_sleeve"]
    draw_beveled_panel(base, (zx1, zy1, zx2, zy2), s, rivets=False)
    for x in range(zx1 + 1, zx2 - 1):
        put(base, x, zy1 + 1, s["bevel"] if x % 2 == 0 else s["shadow"])
        put(base, x, zy1 + 2, s["trim"] if (x in (zx1 + 2, zx1 + 5, zx1 + 7)) else s["mid"])
    draw_beveled_panel(base, ATLAS_ZONES["mod_zoom_collar"], s, rivets=False)
    fill_rect(base, (59, 24, 62, 27), s["deep"])
    put(base, 60, 25, s["bevel"])

    # 3. Flashlight pod body & warm-white 3x3 lens (glow at (44, 29, 47, 32))
    fx1, fy1, fx2, fy2 = ATLAS_ZONES["mod_flashlight_body"]
    draw_beveled_panel(base, (fx1, fy1, fx2, fy2), s, rivets=False)
    for x in range(fx1 + 1, fx2 - 1, 2):
        put(base, x, fy1 + 1, s["bevel"])
        put(base, x, fy1 + 2, s["deep"])
    flash_lens = UNLIT_GLASS if is_neutral else {
        "rim": hex_rgba("#A88632"),
        "base": hex_rgba("#FFE08A"),
        "bright": hex_rgba("#FFF4D6"),
        "core": hex_rgba("#FFFFFF"),
        "shine": hex_rgba("#FFFFFF"),
    }
    draw_lens_3x3(
        base, glow, 44, 29, s, flash_lens, bezel_pad=1,
        bezel_col=s["deep"], bezel_hi=s["trim"] if is_pro else s["hi"],
        emit_glow=emit_mod_glow,
    )

    # 4. Solar PV panel & frame
    sx1, sy1, sx2, sy2 = ATLAS_ZONES["mod_solar_panel"]
    draw_beveled_panel(base, (sx1, sy1, sx2, sy2), s, rivets=False)
    pv_dark = hex_rgba("#10243E") if not is_neutral else s["deep"]
    pv_cell = hex_rgba("#1B3E6B") if not is_neutral else s["shadow"]
    pv_bus = hex_rgba("#8FA6C4") if not is_neutral else s["mid"]
    for y in range(sy1 + 1, sy2 - 1):
        for x in range(sx1 + 1, sx2 - 1):
            if (x - sx1) % 3 == 0:
                c = pv_bus
            elif (y - sy1) % 2 == 0:
                c = pv_dark
            else:
                c = pv_cell
            put(base, x, y, c)
    draw_beveled_panel(base, ATLAS_ZONES["mod_solar_frame"], s, rivets=True)
    if is_pro:
        for x in range(58, 64):
            put(base, x, 31, s["trim"])

    # 5. Battery pack & end cap (glow charge bar at (55, 59, 59, 61))
    bx1, by1, bx2, by2 = ATLAS_ZONES["mod_battery_pack"]
    draw_beveled_panel(base, (bx1, by1, bx2, by2), s, rivets=True)
    fill_rect(base, (54, 58, 60, 62), s["deep"])
    batt_cols = (
        [hex_rgba("#181E28"), hex_rgba("#181E28"), hex_rgba("#181E28"), hex_rgba("#181E28")]
        if is_neutral
        else [hex_rgba("#00E5FF"), hex_rgba("#66F7FF"), hex_rgba("#FFAE19"), hex_rgba("#7CFF1A")]
    )
    for idx, x in enumerate(range(55, 59)):
        for y in range(59, 61):
            c = batt_cols[idx] if y == 59 else (s["deep"] if is_neutral else hex_rgba("#1C6E8C") if idx < 2 else hex_rgba("#995800") if idx == 2 else hex_rgba("#1B8A3A"))
            put(base, x, y, c)
            if emit_mod_glow and glow is not None:
                put(glow, x, y, c)
    draw_beveled_panel(base, ATLAS_ZONES["mod_battery_cap"], s, rivets=False)
    for y in range(29, 33):
        put(base, 39, y, s["deep"] if y % 2 == 0 else s["bevel"])
        put(base, 40, y, s["deep"] if y % 2 == 0 else s["bevel"])

    # 6. Lua PCB cartridge (glow LED at (26, 30, 28, 32))
    lx1, ly1, lx2, ly2 = ATLAS_ZONES["mod_lua_pcb"]
    pcb_fill = s["shadow"] if is_neutral else hex_rgba("#133D29")
    pcb_pin = s["bevel"] if is_neutral else hex_rgba("#E0A42B")
    fill_rect(base, (lx1, ly1, lx2, ly2), s["deep"])
    fill_rect(base, (lx1 + 1, ly1 + 1, lx2 - 1, ly2 - 1), pcb_fill)
    for x in range(lx1 + 1, lx2 - 1, 2):
        put(base, x, ly1, pcb_pin)
    lua_led = [
        [hex_rgba("#7CFF1A"), hex_rgba("#E8FFE6")],
        [hex_rgba("#14B854"), hex_rgba("#7CFF1A")],
    ]
    for dy in range(2):
        for dx in range(2):
            c = s["mid"] if is_neutral else lua_led[dy][dx]
            put(base, 26 + dx, 30 + dy, c)
            if emit_mod_glow and glow is not None:
                put(glow, 26 + dx, 30 + dy, c)

    # 7. Sonar acoustic pod (glow LED at (2, 31, 4, 33))
    sox1, soy1, sox2, soy2 = ATLAS_ZONES["mod_sonar_pod"]
    draw_beveled_panel(base, (sox1, soy1, sox2, soy2), s, rivets=False)
    for y in range(soy1 + 1, soy2 - 1):
        put(base, sox1 + 1, y, s["deep"])
        put(base, sox2 - 2, y, s["deep"])
    for dy in range(2):
        for dx in range(2):
            c = s["mid"] if is_neutral else (hex_rgba("#E6FDFF") if (dx == 1 and dy == 0) else hex_rgba("#00E5FF"))
            put(base, 2 + dx, 31 + dy, c)
            if emit_mod_glow and glow is not None:
                put(glow, 2 + dx, 31 + dy, c)

    # 8. Vital Info biometric pod (glow pulse bar at (15, 31, 19, 33))
    vx1, vy1, vx2, vy2 = ATLAS_ZONES["mod_vital_pod"]
    draw_beveled_panel(base, (vx1, vy1, vx2, vy2), s, rivets=False)
    fill_rect(base, (14, 30, 20, 33), s["deep"])
    pulse_row = [hex_rgba("#14B854"), hex_rgba("#7CFF1A"), hex_rgba("#FF4D6A"), hex_rgba("#FF1E2D")]
    for idx, x in enumerate(range(15, 19)):
        for y in range(31, 33):
            c = s["mid"] if is_neutral else (pulse_row[idx] if y == 31 else hex_rgba("#0E6E35") if idx < 2 else hex_rgba("#8F1022"))
            put(base, x, y, c)
            if emit_mod_glow and glow is not None:
                put(glow, x, y, c)

    # 9. Environment atmospheric pod (glow LED at (56, 20, 58, 22))
    ex1, ey1, ex2, ey2 = ATLAS_ZONES["mod_env_pod"]
    draw_beveled_panel(base, (ex1, ey1, ex2, ey2), s, rivets=False)
    for y in range(ey1 + 1, ey2 - 1):
        put(base, ex1 + 1, y, s["deep"])
        put(base, ex2 - 2, y, s["deep"])
    for dy in range(2):
        for dx in range(2):
            c = s["mid"] if is_neutral else (hex_rgba("#FFAE19") if dx == 0 else hex_rgba("#3FD2E0"))
            put(base, 56 + dx, 20 + dy, c)
            if emit_mod_glow and glow is not None:
                put(glow, 56 + dx, 20 + dy, c)

    # 10. Shared optical collar & mod_optics_base (ZERO glow everywhere!)
    draw_beveled_panel(base, ATLAS_ZONES["mod_optics_collar"], s, rivets=False)
    bx0, by0, _, _ = ATLAS_ZONES["mod_optics_base"]
    draw_lens_4x4(
        base, None, bx0 + 1, by0 + 1, s, UNLIT_GLASS,
        has_outer_bezel=True, bezel_pad=1,
        bezel_col=s["deep"], bezel_hi=s["hi"],
        emit_glow=False,
    )

    # 11. Dedicated mod_optics_{nvg,thermal,bio,hydro} + *_aux islands
    primary_coords = {
        "nvg": (35, 45),
        "thermal": (41, 45),
        "bio": (47, 45),
        "hydro": (53, 45),
    }
    aux_coords = {
        "nvg": (59, 39),
        "thermal": (59, 44),
        "bio": (0, 47),
        "hydro": (50, 0),
    }
    for mode_key, (px, py) in primary_coords.items():
        ml = UNLIT_GLASS if is_neutral else OPTIC_MODE_LENSES[mode_key]
        bcol = s["deep"] if is_neutral else OPTIC_MODE_LENSES[mode_key]["bezel"]
        bhi = s["hi"] if is_neutral else OPTIC_MODE_LENSES[mode_key]["bezel_hi"]
        draw_lens_4x4(
            base, glow, px + 1, py + 1, s, ml,
            has_outer_bezel=True, bezel_pad=1,
            bezel_col=bcol, bezel_hi=bhi,
            emit_glow=emit_mod_glow,
        )
        ax, ay = aux_coords[mode_key]
        draw_lens_3x3(
            base, glow, ax + 1, ay + 1, s, ml, bezel_pad=1,
            bezel_col=bcol, bezel_hi=bhi,
            emit_glow=emit_mod_glow,
        )


def generate_variant(variant: str, cfg: Dict) -> Tuple[Path, Optional[Path]]:
    s = cfg["struct"]
    l = cfg["lens"]
    led = cfg["led"]
    style = cfg["style"]
    emit_glow: bool = cfg["emit_glow"]
    allowed_glow_rects: List[Rect] = cfg["glow_rects"]
    is_modular_glow = emit_glow and (allowed_glow_rects is MODULAR_GLOW_ALLOWED_RECTS)

    base = Image.new("RGBA", (64, 64), s["deep"])
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # 1. Straps & Harness
    x1, y1, x2, y2 = ATLAS_ZONES["strap_outer"]
    for y in range(y1, y2):
        for x in range(x1, x2):
            if style == "pro_modular":
                if y == y1:
                    c = s["trim"] if x % 4 == 0 else s["hi"]
                elif y == y2 - 1:
                    c = s["deep"]
                else:
                    c = s["mid"] if ((x + y) % 2 == 0) else s["deep"]
            else:
                if y == y1:
                    c = s["mid"] if x % 2 == 0 else s["base"]
                elif y == y2 - 1:
                    c = s["shadow"] if x % 2 == 1 else s["deep"]
                else:
                    c = s["hi"] if (x + y) % 3 == 0 else s["base"]
            put(base, x, y, c)

    fill_rect(base, ATLAS_ZONES["strap_inner"], s["deep"])
    for x in range(0, 19, 2):
        put(base, x, 5, s["shadow"])

    for x in range(19):
        if style == "hydro" and x % 4 == 0:
            put(base, x, 8, s["trim"])
        elif style == "pro_modular" and x % 3 == 0:
            put(base, x, 8, s["trim"])
        else:
            put(base, x, 8, s["mid"])

    gx1, gy1, gx2, gy2 = ATLAS_ZONES["strap_gasket"]
    fill_rect(base, ATLAS_ZONES["strap_gasket"], s["gasket"])
    for x in range(gx1, gx2):
        put(base, x, gy1, s["trim"] if style == "hydro" else s["mid"])
        put(base, x, gy2 - 1, s["deep"])

    draw_beveled_panel(base, ATLAS_ZONES["rear_buckle"], s, rivets=True)
    fill_rect(base, (22, 1, 26, 4), s["deep"])
    fill_rect(base, (23, 2, 25, 3), s["trim"] if style == "pro_modular" else s["bevel"])
    draw_beveled_panel(base, ATLAS_ZONES["rear_buckle_side"], s, rivets=False)
    fill_rect(base, ATLAS_ZONES["strap_cap"], s["shadow"])

    draw_beveled_panel(base, ATLAS_ZONES["wilcox_plate"], s, rivets=True)
    fill_rect(base, (35, 1, 39, 5), s["mid"])
    fill_rect(base, (36, 2, 38, 4), s["trim"] if style == "pro_modular" else s["bevel"])
    if style == "pro_modular":
        put(base, 33, 0, s["gold_hi"])
        put(base, 40, 0, s["gold_hi"])
    draw_beveled_panel(base, ATLAS_ZONES["wilcox_arm"], s, rivets=False)
    put(base, 43, 1, s["trim"] if style == "pro_modular" else s["bevel"])
    draw_beveled_panel(base, ATLAS_ZONES["mount_latch"], s, rivets=False)
    put(base, 46, 1, s["gold_hi"] if style == "pro_modular" else s["trim"])
    draw_beveled_panel(base, ATLAS_ZONES["hinge_saddle"], s, rivets=False)
    for x in range(46, 49):
        put(base, x, 5, s["trim"] if style == "pro_modular" else s["bevel"])

    for y in range(0, 18):
        if style == "pro_modular":
            put(base, 59, y, s["mid"] if y % 2 == 0 else s["deep"])
            put(base, 60, y, s["trim"] if y % 3 == 0 else s["hi"])
            put(base, 61, y, s["deep"] if y % 2 == 0 else s["mid"])
            put(base, 62, y, s["shadow"])
        else:
            put(base, 59, y, s["shadow"])
            put(base, 60, y, s["hi"] if style != "hydro" else s["gasket"])
            put(base, 61, y, s["mid"])
            put(base, 62, y, s["deep"])
            if y % 3 == 0:
                put(base, 60, y, s["bevel"])
    for y in range(18):
        if style == "hydro" and y % 4 == 0:
            put(base, 63, y, s["trim"])
        elif style == "pro_modular" and y % 3 == 0:
            put(base, 63, y, s["trim"])
        else:
            put(base, 63, y, s["mid"])
    draw_beveled_panel(base, ATLAS_ZONES["sagittal_cap"], s, rivets=False)
    fill_rect(base, ATLAS_ZONES["sagittal_edge"], s["shadow"])

    # 2. Brow / Bridge / Chassis
    draw_beveled_panel(base, ATLAS_ZONES["brow_front"], s, rivets=True)
    for x in range(22, 35):
        put(base, x, 9, s["mid"] if x % 2 == 0 else s["hi"])
    if style == "modular":
        for x in range(22, 35, 3):
            put(base, x, 8, s["deep"])
            put(base, x, 9, s["trim"])
    elif style == "pro_modular":
        for x in range(20, 37):
            put(base, x, 7, s["trim"] if x % 2 == 0 else s["gold_hi"])
        for x in range(22, 35, 3):
            put(base, x, 8, s["deep"])
            put(base, x, 9, s["gold_hi"])
    elif style == "hydro":
        for x in range(20, 37):
            put(base, x, 7, s["trim"])
    elif style == "thermal":
        for x in range(24, 33):
            put(base, x, 8, s["hi"])
        put(base, 28, 9, s["trim"])

    draw_beveled_panel(base, ATLAS_ZONES["brow_top"], s, rivets=False)
    for x in range(21, 36):
        put(base, x, 13, s["mid"])
        put(base, x, 14, s["shadow"])
    draw_beveled_panel(base, ATLAS_ZONES["bridge_cap"], s, rivets=True)
    fill_rect(base, ATLAS_ZONES["recessed_interior"], s["deep"])
    draw_beveled_panel(base, ATLAS_ZONES["chassis_core"], s, rivets=True)
    draw_beveled_panel(base, ATLAS_ZONES["chassis_top"], s, rivets=False)

    # 3. Optical Barrels, Knurled Rings, Bezels, Rear Eyepiece
    for y in range(16, 22):
        for x in range(0, 8):
            if y == 16:
                c = s["hi"]
            elif y == 17:
                c = s["mid"]
            elif y in (18, 19):
                c = s["base"]
            elif y == 20:
                c = s["shadow"]
            else:
                c = s["deep"]
            if x in (0, 7):
                c = s["bevel"] if y == 17 else s["shadow"]
            put(base, x, y, c)

    for y in range(16, 22):
        for x in range(9, 15):
            if x in (9, 14):
                c = s["shadow"]
            elif x in (11, 12):
                c = s["hi"]
            else:
                c = s["base"]
            if y in (16, 21):
                c = s["mid"]
            put(base, x, y, c)

    for y in range(16, 21):
        for x in range(16, 18):
            put(base, x, y, s["bevel"] if y % 2 == 0 else s["deep"])

    for y in range(16, 18):
        for x in range(19, 24):
            put(base, x, y, s["bevel"] if x % 2 == 0 else s["deep"])

    for x in range(19, 24):
        put(base, x, 19, s["trim"] if style in ("hydro", "pro_modular") else s["bevel"])

    draw_beveled_panel(base, ATLAS_ZONES["bezel_front"], s, rivets=False)
    fill_rect(base, (26, 17, 29, 20), s["deep"])
    put(base, 27, 18, s["shadow"])

    draw_beveled_panel(base, ATLAS_ZONES["bezel_side"], s, rivets=False)

    draw_beveled_panel(base, ATLAS_ZONES["eyepiece_rear"], s, rivets=False)
    fill_rect(base, (37, 17, 40, 20), hex_rgba("#121824"))
    put(base, 39, 17, hex_rgba("#384C6E"))

    draw_beveled_panel(base, ATLAS_ZONES["edge_trim"], s, rivets=False)
    for x in range(42, 54):
        put(base, x, 17, s["trim"])
        if x % 3 == 0:
            put(base, x, 18, s["gold_hi"] if style == "pro_modular" else s["bevel"])

    # 4. Variant Hardware
    ax1, ay1, ax2, ay2 = ATLAS_ZONES["armor_side"]
    for y in range(ay1, ay2):
        for x in range(ax1, ax2):
            if y == ay1:
                c = s["trim"] if style == "pro_modular" else (s["bevel"] if style == "bio" else s["hi"])
            elif y == ay2 - 1:
                c = s["deep"]
            elif style in ("bio", "pro_modular"):
                c = s["mid"] if ((x + y) % 2 == 0) else s["shadow"]
            else:
                c = s["mid"] if (x % 3 == 0) else s["base"]
            put(base, x, y, c)
    for y in range(23, 33):
        put(base, 11, y, s["hi"])
        put(base, 12, y, s["shadow"])

    fill_rect(base, ATLAS_ZONES["ear_disc"], s["deep"])
    for dy in range(6):
        for dx in range(6):
            dist2 = (dx - 2.5) ** 2 + (dy - 2.5) ** 2
            if dist2 <= 2.5:
                put(base, 14 + dx, 23 + dy, s["hi"])
            elif dist2 <= 6.5:
                put(base, 14 + dx, 23 + dy, s["mid"])
            elif dist2 <= 9.0:
                put(base, 14 + dx, 23 + dy, s["bevel"] if dy < 3 else s["shadow"])
    draw_beveled_panel(base, ATLAS_ZONES["ear_hub"], s, rivets=False)
    put(base, 22, 24, s["trim"])
    draw_beveled_panel(base, ATLAS_ZONES["ear_rim"], s, rivets=False)

    draw_beveled_panel(base, ATLAS_ZONES["battery_side"], s, rivets=True)
    for x in range(26, 32):
        put(base, x, 25, s["deep"])
        put(base, x, 26, s["trim"] if style in ("hydro", "pro_modular") else s["mid"])
    draw_beveled_panel(base, ATLAS_ZONES["battery_edge"], s, rivets=False)

    draw_beveled_panel(base, ATLAS_ZONES["cap_dial"], s, rivets=False)
    fill_rect(base, (39, 24, 41, 26), s["gold_hi"] if style == "pro_modular" else s["trim"])

    draw_beveled_panel(base, ATLAS_ZONES["junction_box"], s, rivets=True)
    put(base, 45, 25, s["trim"] if style == "pro_modular" else s["bevel"])
    put(base, 46, 25, s["trim"] if style == "pro_modular" else s["bevel"])

    for y in range(23, 31):
        for x in range(50, 58):
            put(base, x, y, s["bevel"] if (x + (1 if style == "thermal" else 0)) % 2 == 0 else s["deep"])

    for y in range(34, 38):
        for x in range(0, 12):
            put(base, x, y, s["hi"] if x % 2 == 0 else s["cable"])

    draw_beveled_panel(base, ATLAS_ZONES["underside_chassis"], s, rivets=False, highlight_top=False)
    draw_beveled_panel(base, ATLAS_ZONES["secondary_trim"], s, rivets=True)
    for x in range(32, 47):
        put(base, x, 35, s["trim"])

    # 5. Isolated Base Emissive Zones (V: 39..51)
    draw_lens_4x4(base, glow, 2, 41, s, l, has_outer_bezel=True, bezel_pad=2, emit_glow=emit_glow)
    draw_lens_3x3(base, glow, 8, 48, s, l, bezel_pad=1, emit_glow=emit_glow)
    draw_lens_4x4(base, glow, 14, 41, s, l, has_outer_bezel=True, bezel_pad=2, emit_glow=emit_glow)
    draw_lens_3x3(base, glow, 23, 40, s, l, bezel_pad=1, emit_glow=emit_glow)

    draw_beveled_panel(base, ATLAS_ZONES["micro_emitter_4x2"], s, rivets=False)
    for x in range(31, 35):
        put(base, x, 40, led["emitter"])
        c2 = l["shine"] if x == 33 else led["bar_hi"]
        put(base, x, 41, c2)
        if emit_glow:
            put(glow, x, 40, led["emitter"])
            put(glow, x, 41, c2)

    draw_beveled_panel(base, ATLAS_ZONES["micro_port_2x2"], s, rivets=False)
    for dy in range(2):
        for dx in range(2):
            c = l["shine"] if (dx == 1 and dy == 0) else led["dot"]
            put(base, 32 + dx, 45 + dy, c)
            if emit_glow:
                put(glow, 32 + dx, 45 + dy, c)

    draw_beveled_panel(base, ATLAS_ZONES["battery_bar"], s, rivets=False)
    put(base, 41, 41, led["bar_hi"])
    put(base, 41, 42, led["bar_lo"])
    if emit_glow:
        put(glow, 41, 41, led["bar_hi"])
        put(glow, 41, 42, led["bar_lo"])
    for x in range(44, 48):
        put(base, x, 41, led["bar_hi"])
        put(base, x, 42, led["bar_lo"])
        if emit_glow:
            put(glow, x, 41, led["bar_hi"])
            put(glow, x, 42, led["bar_lo"])

    draw_beveled_panel(base, ATLAS_ZONES["status_dot"], s, rivets=False)
    for dy in range(2):
        for dx in range(2):
            c = l["shine"] if (dx == 1 and dy == 0) else led["dot"]
            put(base, 53 + dx, 41 + dy, c)
            if emit_glow:
                put(glow, 53 + dx, 41 + dy, c)

    # 6. Stencils / Vents / Scuffed Alloy / Cavity (non-emissive)
    draw_beveled_panel(base, ATLAS_ZONES["vents_rivets"], s, rivets=True)
    for x in range(2, 14, 2):
        for y in range(54, 58):
            put(base, x, y, s["deep"])
    draw_beveled_panel(base, ATLAS_ZONES["scuffed_alloy"], s, rivets=False)
    for x in range(20, 32, 3):
        put(base, x, 54, s["bevel"])
        put(base, x + 1, 56, s["trim"])
    fill_rect(base, ATLAS_ZONES["deep_cavity"], s["deep"])

    # 7. Master Modular 26 new zones (only modular/pro_modular emit glow inside NEW_MODULAR_GLOW_RECTS)
    draw_modular_attachment_zones(base, glow, s, style=style, emit_mod_glow=is_modular_glow)

    # Assertions for size, RGBA mode, and strict glow mask hygiene
    assert base.size == (64, 64) and base.mode == "RGBA"
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    base_path = OUT_DIR / f"{variant}.png"
    base.save(base_path, format="PNG")

    if not emit_glow:
        glow_path = OUT_DIR / f"{variant}_glow.png"
        if glow_path.exists():
            glow_path.unlink()
        return base_path, None

    assert glow.size == (64, 64) and glow.mode == "RGBA"
    non_zero_glow = 0
    for y in range(64):
        for x in range(64):
            px = glow.getpixel((x, y))
            in_allowed = any(ax1 <= x < ax2 and ay1 <= y < ay2 for (ax1, ay1, ax2, ay2) in allowed_glow_rects)
            if not in_allowed:
                assert px == (0, 0, 0, 0), f"Glow bleed at ({x},{y}) in {variant}: {px}"
            else:
                assert px[3] == 255, f"Missing opaque glow pixel inside aperture ({x},{y}) in {variant}: {px}"
                non_zero_glow += 1
    assert non_zero_glow > 0

    # Extra explicit guarantee: mod_optics_base [24,45,30,51] has ZERO glow pixels
    bx1, by1, bx2, by2 = ATLAS_ZONES["mod_optics_base"]
    for y in range(by1, by2):
        for x in range(bx1, bx2):
            assert glow.getpixel((x, y)) == (0, 0, 0, 0), f"mod_optics_base glow bleed at ({x},{y}) in {variant}"

    glow_path = OUT_DIR / f"{variant}_glow.png"
    glow.save(glow_path, format="PNG")
    return base_path, glow_path


def main() -> None:
    print(f"Generating 64x64 tri-lens production textures into {OUT_DIR}")
    for variant, cfg in PALETTES.items():
        b, g = generate_variant(variant, cfg)
        if g is not None:
            print(f"  [OK] {b.name} & {g.name} (64x64, glow mask verified: {len(cfg['glow_rects'])} rects)")
        else:
            print(f"  [OK] {b.name} (64x64, matte neutral, no _glow)")


if __name__ == "__main__":
    main()
