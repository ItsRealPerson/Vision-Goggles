"""
Generate 64x64 base textures and strictly-masked RGBA _glow.png emissive masks
for all 5 Tactical Tri-Lens prototype variants:
  - proto_tri_lens_nvg
  - proto_tri_lens_thermal
  - proto_tri_lens_bio
  - proto_tri_lens_hydro
  - proto_tri_lens_modular

Output directory: models_src/tri_lens_proto/textures/
All coordinates follow the shared 64x64 per-face UV atlas (2.0 px / model_unit).
"""

from __future__ import annotations

import os
from pathlib import Path
from typing import Dict, List, Tuple
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
OUT_DIR = ROOT / "models_src" / "tri_lens_proto" / "textures"

RGBA = Tuple[int, int, int, int]


def hex_rgba(h: str, a: int = 255) -> RGBA:
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


# Allowed emissive bounding boxes in (x1, y1, x2, y2) half-open pixel coordinates.
# Every pixel outside these rects MUST be strictly (0, 0, 0, 0) in *_glow.png.
GLOW_ALLOWED_RECTS: List[Tuple[int, int, int, int]] = [
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


# Variant palettes derived from the 5 reference images & Explorer 3 spec.
# Structural ramp keys: deep, shadow, base, mid, hi, bevel, trim
# Lens ramp keys: rim, base, bright, core, shine
PALETTES: Dict[str, Dict] = {
    "proto_tri_lens_nvg": {
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
    },
    "proto_tri_lens_thermal": {
        "struct": {
            "deep": hex_rgba("#14161A"),
            "shadow": hex_rgba("#1C1F24"),
            "base": hex_rgba("#22252A"),
            "mid": hex_rgba("#343940"),
            "hi": hex_rgba("#5A6068"),
            "bevel": hex_rgba("#78808A"),
            "trim": hex_rgba("#9E6B3B"),  # Brass/copper dial & contact trim
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
    },
    "proto_tri_lens_bio": {
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
    },
    "proto_tri_lens_hydro": {
        "struct": {
            "deep": hex_rgba("#101A24"),
            "shadow": hex_rgba("#16222E"),
            "base": hex_rgba("#1D2B38"),
            "mid": hex_rgba("#283B4D"),
            "hi": hex_rgba("#334B5E"),
            "bevel": hex_rgba("#5E788C"),
            "trim": hex_rgba("#24B8C7"),   # High-vis teal seal trim
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
    },
    "proto_tri_lens_modular": {
        "struct": {
            "deep": hex_rgba("#1F2329"),
            "shadow": hex_rgba("#2C3139"),
            "base": hex_rgba("#383E47"),
            "mid": hex_rgba("#4D5460"),
            "hi": hex_rgba("#636A73"),
            "bevel": hex_rgba("#8A939E"),
            "trim": hex_rgba("#A4ADB8"),   # Raw machined titanium CNC bevel
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
    },
}


# Clean non-overlapping 64x64 atlas zones
ATLAS_ZONES: Dict[str, Tuple[int, int, int, int]] = {
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
}

def verify_non_overlapping_zones(zones: Dict[str, Tuple[int, int, int, int]]) -> None:
    items = list(zones.items())
    for i in range(len(items)):
        name_a, (ax1, ay1, ax2, ay2) = items[i]
        assert 0 <= ax1 < ax2 <= 64 and 0 <= ay1 < ay2 <= 64, f"Out of bounds: {name_a}"
        for j in range(i + 1, len(items)):
            name_b, (bx1, by1, bx2, by2) = items[j]
            if ax1 < bx2 and ax2 > bx1 and ay1 < by2 and ay2 > by1:
                raise AssertionError(f"Atlas overlap between {name_a} {items[i][1]} and {name_b} {items[j][1]}")


# Verify zero overlap across all atlas islands at import time!
verify_non_overlapping_zones(ATLAS_ZONES)


def put(img: Image.Image, x: int, y: int, c: RGBA) -> None:
    if 0 <= x < 64 and 0 <= y < 64:
        img.putpixel((x, y), c)


def fill_rect(img: Image.Image, rect: Tuple[int, int, int, int], c: RGBA) -> None:
    x1, y1, x2, y2 = rect
    for y in range(y1, y2):
        for x in range(x1, x2):
            img.putpixel((x, y), c)


def draw_beveled_panel(img: Image.Image, rect: Tuple[int, int, int, int], s: Dict[str, RGBA],
                       rivets: bool = True, highlight_top: bool = True) -> None:
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


def draw_lens_4x4(base: Image.Image, glow: Image.Image, x0: int, y0: int,
                  s: Dict[str, RGBA], l: Dict[str, RGBA], has_outer_bezel: bool = True) -> None:
    """Draw a 4x4 emissive lens with 2x2 stepped diagonal white shine at (x0+2, y0) and (x0+3, y0+1)."""
    if has_outer_bezel:
        fill_rect(base, (x0 - 2, y0 - 2, x0 + 6, y0 + 6), s["shadow"])
        fill_rect(base, (x0 - 1, y0 - 1, x0 + 5, y0 + 5), s["deep"])
        for i in range(-2, 6):
            put(base, x0 + i, y0 - 2, s["bevel"])
            put(base, x0 + i, y0 + 5, s["deep"])

    # 4x4 glass gradient + radial core + 2x2 diagonal specular shine (Ref 1 style)
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
            put(glow, x0 + dx, y0 + dy, c)


def draw_lens_3x3(base: Image.Image, glow: Image.Image, x0: int, y0: int,
                  s: Dict[str, RGBA], l: Dict[str, RGBA], bezel_pad: int = 1) -> None:
    """Draw a 3x3 emissive lens with crisp diagonal specular shine."""
    fill_rect(base, (x0 - bezel_pad, y0 - bezel_pad, x0 + 3 + bezel_pad, y0 + 3 + bezel_pad), s["deep"])
    for x in range(x0 - bezel_pad, x0 + 3 + bezel_pad):
        put(base, x, y0 - bezel_pad, s["hi"])
    grid = [
        [l["base"],   l["bright"], l["shine"]],
        [l["bright"], l["core"],   l["bright"]],
        [l["rim"],    l["bright"], l["base"]],
    ]
    for dy in range(3):
        for dx in range(3):
            c = grid[dy][dx]
            put(base, x0 + dx, y0 + dy, c)
            put(glow, x0 + dx, y0 + dy, c)


def generate_variant(variant: str, cfg: Dict) -> Tuple[Path, Path]:
    s = cfg["struct"]
    l = cfg["lens"]
    led = cfg["led"]
    style = cfg["style"]

    base = Image.new("RGBA", (64, 64), s["deep"])
    glow = Image.new("RGBA", (64, 64), (0, 0, 0, 0))

    # 1. Straps & Harness
    # strap_outer (0,0,19,3) -> 19x3 diagonal nylon weave + edge stitch
    x1, y1, x2, y2 = ATLAS_ZONES["strap_outer"]
    for y in range(y1, y2):
        for x in range(x1, x2):
            if y == y1:
                c = s["mid"] if x % 2 == 0 else s["base"]
            elif y == y2 - 1:
                c = s["shadow"] if x % 2 == 1 else s["deep"]
            else:
                c = s["hi"] if (x + y) % 3 == 0 else s["base"]
            put(base, x, y, c)

    # strap_inner (0,4,19,7) -> padded dark lining
    fill_rect(base, ATLAS_ZONES["strap_inner"], s["deep"])
    for x in range(0, 19, 2):
        put(base, x, 5, s["shadow"])

    # strap_edge (0,8,19,9) -> 1px piping
    for x in range(19):
        put(base, x, 8, s["trim"] if (style == "hydro" and x % 4 == 0) else s["mid"])

    # strap_gasket (0,10,17,14) -> rubberized brow seal
    gx1, gy1, gx2, gy2 = ATLAS_ZONES["strap_gasket"]
    fill_rect(base, ATLAS_ZONES["strap_gasket"], s["gasket"])
    for x in range(gx1, gx2):
        put(base, x, gy1, s["trim"] if style == "hydro" else s["mid"])
        put(base, x, gy2 - 1, s["deep"])

    # rear_buckle (20,0,28,5) & side & strap_cap
    draw_beveled_panel(base, ATLAS_ZONES["rear_buckle"], s, rivets=True)
    fill_rect(base, (22, 1, 26, 4), s["deep"])
    fill_rect(base, (23, 2, 25, 3), s["bevel"])
    draw_beveled_panel(base, ATLAS_ZONES["rear_buckle_side"], s, rivets=False)
    fill_rect(base, ATLAS_ZONES["strap_cap"], s["shadow"])

    # wilcox_plate (33,0,41,6), wilcox_arm, mount_latch, hinge_saddle
    draw_beveled_panel(base, ATLAS_ZONES["wilcox_plate"], s, rivets=True)
    fill_rect(base, (35, 1, 39, 5), s["mid"])
    fill_rect(base, (36, 2, 38, 4), s["bevel"])  # CNC dovetail recess
    draw_beveled_panel(base, ATLAS_ZONES["wilcox_arm"], s, rivets=False)
    put(base, 43, 1, s["bevel"])
    draw_beveled_panel(base, ATLAS_ZONES["mount_latch"], s, rivets=False)
    put(base, 46, 1, s["trim"])
    draw_beveled_panel(base, ATLAS_ZONES["hinge_saddle"], s, rivets=False)
    for x in range(46, 49):
        put(base, x, 5, s["bevel"])

    # sagittal_top (59,0,63,18) -> 4x18 vertical webbing with central stripe
    for y in range(0, 18):
        put(base, 59, y, s["shadow"])
        put(base, 60, y, s["hi"] if style != "hydro" else s["gasket"])
        put(base, 61, y, s["mid"])
        put(base, 62, y, s["deep"])
        if y % 3 == 0:
            put(base, 60, y, s["bevel"])
    # strap_edge_vert (63,0,64,18) -> 1x18 vertical piping for side straps
    for y in range(18):
        put(base, 63, y, s["trim"] if (style == "hydro" and y % 4 == 0) else s["mid"])
    draw_beveled_panel(base, ATLAS_ZONES["sagittal_cap"], s, rivets=False)
    fill_rect(base, ATLAS_ZONES["sagittal_edge"], s["shadow"])

    # 2. Brow / Bridge / Chassis
    draw_beveled_panel(base, ATLAS_ZONES["brow_front"], s, rivets=True)
    # Central brow accent seam
    for x in range(22, 35):
        put(base, x, 9, s["mid"] if x % 2 == 0 else s["hi"])
    if style == "modular":
        # Open CNC cutouts along bridge bar
        for x in range(22, 35, 3):
            put(base, x, 8, s["deep"])
            put(base, x, 9, s["trim"])
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
    # barrel_side (0,16,8,22) -> horizontal tube shading
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

    # barrel_top (9,16,15,22) -> vertical orientation along Z (V)
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

    # knurl_horiz (16,16,18,21) -> for east/west of rings (vertical groove/ridge pattern)
    for y in range(16, 21):
        for x in range(16, 18):
            put(base, x, y, s["bevel"] if y % 2 == 0 else s["deep"])

    # knurl_vert (19,16,24,18) -> for up/down of rings (horizontal rib pattern along X)
    for y in range(16, 18):
        for x in range(19, 24):
            put(base, x, y, s["bevel"] if x % 2 == 0 else s["deep"])

    # hood_lip (19,19,24,20)
    for x in range(19, 24):
        put(base, x, 19, s["trim"] if style == "hydro" else s["bevel"])

    # bezel_front (25,16,30,21) -> 5x5 metallic bezel ring
    draw_beveled_panel(base, ATLAS_ZONES["bezel_front"], s, rivets=False)
    fill_rect(base, (26, 17, 29, 20), s["deep"])
    put(base, 27, 18, s["shadow"])

    # bezel_side (31,16,35,21)
    draw_beveled_panel(base, ATLAS_ZONES["bezel_side"], s, rivets=False)

    # eyepiece_rear (36,16,41,21) -> NON-GLOWING anti-reflective rear glass
    draw_beveled_panel(base, ATLAS_ZONES["eyepiece_rear"], s, rivets=False)
    fill_rect(base, (37, 17, 40, 20), hex_rgba("#121824"))
    put(base, 39, 17, hex_rgba("#384C6E"))

    # edge_trim (42,16,54,21)
    draw_beveled_panel(base, ATLAS_ZONES["edge_trim"], s, rivets=False)
    for x in range(42, 54):
        put(base, x, 17, s["trim"])
        if x % 3 == 0:
            put(base, x, 18, s["bevel"])

    # 4. Variant Hardware (Cheek shrouds, Ear housing, Battery/Canister, Dials, Heatsinks)
    # armor_side (0,23,10,29) -> 2x2 carbon fiber weave for Bio, riveted plate for others
    ax1, ay1, ax2, ay2 = ATLAS_ZONES["armor_side"]
    for y in range(ay1, ay2):
        for x in range(ax1, ax2):
            if y == ay1:
                c = s["bevel"] if style == "bio" else s["hi"]
            elif y == ay2 - 1:
                c = s["deep"]
            elif style == "bio":
                c = s["mid"] if ((x + y) % 2 == 0) else s["shadow"]
            else:
                c = s["mid"] if (x % 3 == 0) else s["base"]
            put(base, x, y, c)
    for y in range(23, 33):
        put(base, 11, y, s["hi"])
        put(base, 12, y, s["shadow"])

    # ear_disc (14,23,20,29) -> 6x6 circular ear housing (Ref 4)
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

    # battery_side (25,23,33,28) -> 8x5 battery/canister side
    draw_beveled_panel(base, ATLAS_ZONES["battery_side"], s, rivets=True)
    for x in range(26, 32):
        put(base, x, 25, s["deep"])
        put(base, x, 26, s["trim"] if style == "hydro" else s["mid"])
    draw_beveled_panel(base, ATLAS_ZONES["battery_edge"], s, rivets=False)

    # cap_dial (38,23,42,27) -> rotary dial / valve cap
    draw_beveled_panel(base, ATLAS_ZONES["cap_dial"], s, rivets=False)
    fill_rect(base, (39, 24, 41, 26), s["trim"])

    # junction_box (43,23,49,28)
    draw_beveled_panel(base, ATLAS_ZONES["junction_box"], s, rivets=True)
    put(base, 45, 25, s["bevel"])
    put(base, 46, 25, s["bevel"])

    # heatsink_rails (50,23,58,31) -> alternating 1px fins / Picatinny slots
    for y in range(23, 31):
        for x in range(50, 58):
            put(base, x, y, s["bevel"] if (x + (1 if style == "thermal" else 0)) % 2 == 0 else s["deep"])

    # cable_boot (0,34,12,38)
    for y in range(34, 38):
        for x in range(0, 12):
            put(base, x, y, s["hi"] if x % 2 == 0 else s["cable"])

    # underside_chassis (13,34,30,38)
    draw_beveled_panel(base, ATLAS_ZONES["underside_chassis"], s, rivets=False, highlight_top=False)
    # secondary_trim (31,34,48,38)
    draw_beveled_panel(base, ATLAS_ZONES["secondary_trim"], s, rivets=True)
    for x in range(32, 47):
        put(base, x, 35, s["trim"])

    # 5. Isolated Emissive Zones (V: 39..51) -> strictly mirrored to _glow.png inside GLOW_ALLOWED_RECTS
    # hero_lens_4x4 at (2,41,6,45)
    draw_lens_4x4(base, glow, 2, 41, s, l, has_outer_bezel=True)
    # hero_ir_3x3 at (8,48,11,51)
    draw_lens_3x3(base, glow, 8, 48, s, l, bezel_pad=1)
    # primary_lens_4x4 at (14,41,18,45)
    draw_lens_4x4(base, glow, 14, 41, s, l, has_outer_bezel=True)
    # aux_lens_3x3 at (23,40,26,43)
    draw_lens_3x3(base, glow, 23, 40, s, l, bezel_pad=1)

    # micro_emitter_4x2: housing (30,39,36,43), core (31,40,35,42)
    draw_beveled_panel(base, ATLAS_ZONES["micro_emitter_4x2"], s, rivets=False)
    for x in range(31, 35):
        put(base, x, 40, led["emitter"])
        put(glow, x, 40, led["emitter"])
        c2 = l["shine"] if x == 33 else led["bar_hi"]
        put(base, x, 41, c2)
        put(glow, x, 41, c2)

    # micro_port_2x2: housing (31,44,35,48), core (32,45,34,47)
    draw_beveled_panel(base, ATLAS_ZONES["micro_port_2x2"], s, rivets=False)
    for dy in range(2):
        for dx in range(2):
            c = l["shine"] if (dx == 1 and dy == 0) else led["dot"]
            put(base, 32 + dx, 45 + dy, c)
            put(glow, 32 + dx, 45 + dy, c)

    # battery_bar: housing (39,39,49,45), 1x2 bar (41,41,42,43), 4x2 bar (44,41,48,43)
    draw_beveled_panel(base, ATLAS_ZONES["battery_bar"], s, rivets=False)
    put(base, 41, 41, led["bar_hi"])
    put(glow, 41, 41, led["bar_hi"])
    put(base, 41, 42, led["bar_lo"])
    put(glow, 41, 42, led["bar_lo"])
    for x in range(44, 48):
        put(base, x, 41, led["bar_hi"])
        put(glow, x, 41, led["bar_hi"])
        put(base, x, 42, led["bar_lo"])
        put(glow, x, 42, led["bar_lo"])

    # status_dot: housing (51,39,57,45), 2x2 dot (53,41,55,43)
    draw_beveled_panel(base, ATLAS_ZONES["status_dot"], s, rivets=False)
    for dy in range(2):
        for dx in range(2):
            c = l["shine"] if (dx == 1 and dy == 0) else led["dot"]
            put(base, 53 + dx, 41 + dy, c)
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

    # Assertions for glow mask hygiene
    assert base.size == (64, 64) and glow.size == (64, 64)
    assert glow.mode == "RGBA"
    non_zero_glow = 0
    for y in range(64):
        for x in range(64):
            px = glow.getpixel((x, y))
            in_allowed = any(ax1 <= x < ax2 and ay1 <= y < ay2 for (ax1, ay1, ax2, ay2) in GLOW_ALLOWED_RECTS)
            if not in_allowed:
                assert px == (0, 0, 0, 0), f"Glow bleed at ({x},{y}) in {variant}: {px}"
            else:
                assert px[3] == 255, f"Missing opaque glow pixel inside aperture ({x},{y}) in {variant}: {px}"
                non_zero_glow += 1
    assert non_zero_glow > 0

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    base_path = OUT_DIR / f"{variant}.png"
    glow_path = OUT_DIR / f"{variant}_glow.png"
    base.save(base_path, format="PNG")
    glow.save(glow_path, format="PNG")
    return base_path, glow_path


def main() -> None:
    print(f"Generating 64x64 tri-lens textures into {OUT_DIR}")
    for variant, cfg in PALETTES.items():
        b, g = generate_variant(variant, cfg)
        print(f"  [OK] {b.name} & {g.name} (64x64, glow mask verified)")


if __name__ == "__main__":
    main()
