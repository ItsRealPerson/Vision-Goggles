"""
Build the Master Modular Tri-Lens Blockbench 5.0 (.bbmodel) sources:
  - models_src/tri_lens_master/tri_lens_master_modular.bbmodel (embeds tri_lens_modular.png)
  - models_src/tri_lens_master/tri_lens_master_pro_modular.bbmodel (embeds tri_lens_pro_modular.png)

Optionally stages temporary copies in common/src/main/resources/assets/vision_goggles/geo/
with --stage-tmp so the bbmodel MCP server can validate, render contact sheets, and export
to geo/item/{modular,pro_modular}_goggles.geo.json before cleanup.
"""

from __future__ import annotations

import argparse
import base64
import copy
import json
import uuid
from pathlib import Path
from typing import Dict, List, Optional, Tuple

ROOT = Path(__file__).resolve().parent.parent
PROTO_BB = ROOT / "models_src" / "tri_lens_proto" / "proto_tri_lens_modular.bbmodel"
TEX_DIR = ROOT / "common" / "src" / "main" / "resources" / "assets" / "vision_goggles" / "textures" / "entities"
MASTER_DIR = ROOT / "models_src" / "tri_lens_master"
GEO_DIR = ROOT / "common" / "src" / "main" / "resources" / "assets" / "vision_goggles" / "geo"

UUID_NS = uuid.UUID("a4f38c20-7b1e-4e9f-9d1a-5c7e8b3f1042")


def det_uuid(key: str) -> str:
    return str(uuid.uuid5(UUID_NS, key))


def make_faces(
    north: List[int],
    east: Optional[List[int]] = None,
    south: Optional[List[int]] = None,
    west: Optional[List[int]] = None,
    up: Optional[List[int]] = None,
    down: Optional[List[int]] = None,
) -> Dict[str, Dict]:
    def f(rect: List[int]) -> Dict:
        return {"uv": list(rect), "texture": 0}

    e = east if east is not None else north
    s = south if south is not None else north
    w = west if west is not None else e
    u = up if up is not None else north
    d = down if down is not None else u
    return {
        "north": f(north),
        "east": f(e),
        "south": f(s),
        "west": f(w),
        "up": f(u),
        "down": f(d),
    }


def geo_cube_to_bb(
    name: str,
    geo_from: List[float],
    geo_to: List[float],
    faces: Dict[str, Dict],
    geo_pivot: Optional[List[float]] = None,
    geo_rot: Optional[List[float]] = None,
) -> Dict:
    """Convert Bedrock .geo.json-space bounds/pivot/rotation into Blockbench 5.0 cube dict."""
    bb_from = [-geo_to[0], geo_from[1], geo_from[2]]
    bb_to = [-geo_from[0], geo_to[1], geo_to[2]]
    if geo_pivot is not None:
        bb_origin = [-geo_pivot[0], geo_pivot[1], geo_pivot[2]]
    else:
        bb_origin = [
            round((bb_from[0] + bb_to[0]) / 2.0, 6),
            round((bb_from[1] + bb_to[1]) / 2.0, 6),
            round((bb_from[2] + bb_to[2]) / 2.0, 6),
        ]
    cube: Dict = {
        "name": name,
        "box_uv": False,
        "rescale": False,
        "locked": False,
        "light_emission": 0,
        "render_order": "default",
        "allow_mirror_modeling": True,
        "from": bb_from,
        "to": bb_to,
        "autouv": 0,
        "color": 0,
        "origin": bb_origin,
        "faces": faces,
        "type": "cube",
        "uuid": det_uuid(f"cube:{name}"),
    }
    if geo_rot is not None:
        cube["rotation"] = [-geo_rot[0], -geo_rot[1], geo_rot[2]]
    return cube


def make_group(name: str, origin: List[float]) -> Dict:
    return {
        "name": name,
        "origin": origin,
        "rotation": [0, 0, 0],
        "bedrock_binding": "",
        "color": 0,
        "uuid": det_uuid(f"group:{name}"),
        "export": True,
        "mirror_uv": False,
        "isOpen": False,
        "locked": False,
        "visibility": True,
        "autouv": 0,
        "children": [],
    }


def make_keyframe(anim_name: str, channel: str, time: float, x: str, y: str, z: str, interp: str = "linear") -> Dict:
    return {
        "uuid": det_uuid(f"kf:{anim_name}:{channel}:{time}"),
        "channel": channel,
        "time": time,
        "data_points": [{"x": x, "y": y, "z": z}],
        "interpolation": interp,
        "color": -1,
    }


def build_animations(visor_uuid: str) -> List[Dict]:
    def anim_obj(name: str, length: float, kfs: List[Dict]) -> Dict:
        return {
            "uuid": det_uuid(f"anim:{name}"),
            "name": name,
            "loop": "hold",
            "length": length,
            "snapping": 24,
            "animators": {
                visor_uuid: {
                    "name": "visor_pivot",
                    "type": "bone",
                    "keyframes": kfs,
                }
            },
            "override": False,
            "selected": False,
            "anim_time_update": "",
            "blend_weight": "",
            "start_delay": "",
            "loop_delay": "",
        }

    return [
        anim_obj("idle_down", 0.05, [
            make_keyframe("idle_down", "rotation", 0.0, "0", "0", "0"),
            make_keyframe("idle_down", "position", 0.0, "0", "0", "0"),
        ]),
        anim_obj("idle_up", 0.05, [
            make_keyframe("idle_up", "rotation", 0.0, "60", "0", "0"),
            make_keyframe("idle_up", "position", 0.0, "0", "2", "-0.75"),
        ]),
        anim_obj("flip_down", 0.44, [
            make_keyframe("flip_down", "rotation", 0.0, "60", "0", "0"),
            make_keyframe("flip_down", "rotation", 0.44, "0", "0", "0"),
            make_keyframe("flip_down", "position", 0.0, "0", "2", "-0.75"),
            make_keyframe("flip_down", "position", 0.44, "0", "0", "0"),
        ]),
        anim_obj("flip_up", 0.44, [
            make_keyframe("flip_up", "rotation", 0.0, "0", "0", "0"),
            make_keyframe("flip_up", "rotation", 0.44, "60", "0", "0"),
            make_keyframe("flip_up", "position", 0.0, "0", "0", "0"),
            make_keyframe("flip_up", "position", 0.44, "0", "2", "-0.75"),
        ]),
    ]


def build_master_bbmodel(texture_filename: str, identifier: str) -> Dict:
    proto = json.loads(PROTO_BB.read_text(encoding="utf-8"))
    proto_by_name = {e["name"]: e for e in proto["elements"]}

    head_base_names = [
        "strap_brow", "strap_side_r", "strap_side_l", "strap_back",
        "strap_sagittal_top", "strap_sagittal_back", "strap_sagittal_front",
        "rear_buckle", "rail_socket_r", "rail_socket_l",
        "wilcox_shroud_frame", "wilcox_cheek_r", "wilcox_cheek_l",
        "wilcox_axle", "mount_latch_l",
    ]
    visor_base_names = [
        "visor_dovetail_lug", "bridge_lower_bar", "bridge_upper_bar",
        "bridge_strut_r", "bridge_strut_l", "bridge_center_block",
        "junction_box", "junction_latch_loop", "junction_boot_r", "junction_cable_r",
    ]

    # Non-overlapping UV helpers (strictly inside the 80 ATLAS_ZONES; non-LEDs never touch MODULAR_GLOW_ALLOWED_RECTS)
    UV_PRO_BROW = [0, 60, 14, 64]
    UV_PRO_PLATE = [14, 60, 26, 64]
    UV_PRO_RAIL = [26, 60, 34, 64]
    UV_PRO_VENT = [37, 12, 49, 16]

    UV_ZOOM_SLEEVE = [34, 60, 44, 64]
    UV_ZOOM_COLLAR = [58, 23, 63, 28]

    UV_FLASH_BODY = [44, 60, 52, 64]
    UV_FLASH_BEZEL = [43, 28, 48, 29]      # 1px dark bezel inside mod_flashlight_lens
    UV_FLASH_LENS_GLOW = [44, 29, 47, 32]  # 3x3 warm-white glow

    UV_SOLAR_PANEL = [48, 31, 58, 38]
    UV_SOLAR_FRAME = [58, 31, 64, 38]

    UV_BATT_BODY = [52, 57, 62, 59]        # Non-glow top housing inside mod_battery_pack
    UV_BATT_BEZEL = [52, 57, 55, 59]
    UV_BATT_LED_GLOW = [55, 59, 59, 61]    # 4x2 charge bar glow
    UV_BATT_CAP = [37, 28, 43, 34]

    UV_LUA_PCB = [23, 28, 31, 30]          # Non-glow upper PCB inside mod_lua_pcb
    UV_LUA_PINS = [24, 28, 30, 29]
    UV_LUA_LED_GLOW = [26, 30, 28, 32]     # 2x2 programmable LED glow

    UV_SONAR_POD = [0, 29, 6, 31]          # Non-glow grille inside mod_sonar_pod
    UV_SONAR_LED_GLOW = [2, 31, 4, 33]     # 2x2 ping LED glow

    UV_VITAL_POD = [13, 29, 21, 31]        # Non-glow housing inside mod_vital_pod
    UV_VITAL_LED_GLOW = [15, 31, 19, 33]   # 4x2 pulse bar glow

    UV_ENV_POD = [54, 18, 60, 20]          # Non-glow housing inside mod_env_pod
    UV_ENV_LED_GLOW = [56, 20, 58, 22]     # 2x2 sensor LED glow

    UV_OPT_COLLAR = [49, 12, 59, 16]
    UV_OPT_BASE_GLASS = [25, 46, 29, 50]   # 4x4 unlit dark glass inside mod_optics_base (ZERO glow)
    UV_OPT_BASE_BEZEL = [24, 45, 30, 46]   # 1px bezel strip inside mod_optics_base

    UV_OPT_NVG_GLOW = [36, 46, 40, 50]
    UV_OPT_NVG_BEZEL = [35, 45, 41, 46]

    UV_OPT_THERM_GLOW = [42, 46, 46, 50]
    UV_OPT_THERM_BEZEL = [41, 45, 47, 46]

    UV_OPT_BIO_GLOW = [48, 46, 52, 50]
    UV_OPT_BIO_AUX_GLOW = [1, 48, 4, 51]
    UV_OPT_BIO_BEZEL = [47, 45, 53, 46]

    UV_OPT_HYDRO_GLOW = [54, 46, 58, 50]
    UV_OPT_HYDRO_AUX_GLOW = [51, 1, 54, 4]
    UV_OPT_HYDRO_BEZEL = [53, 45, 59, 46]

    # Existing safe non-glowing structural zones
    UV_BARREL_SIDE = [0, 16, 8, 22]
    UV_BARREL_TOP = [9, 16, 15, 22]
    UV_KNURL_H = [16, 16, 18, 21]
    UV_KNURL_V = [19, 16, 24, 18]
    UV_HOOD_LIP = [19, 19, 24, 20]
    UV_BEZEL_FRONT = [25, 16, 30, 21]
    UV_BEZEL_SIDE = [31, 16, 35, 21]
    UV_EYEPIECE = [36, 16, 41, 21]
    UV_EDGE_TRIM = [42, 16, 54, 21]
    UV_ARMOR_SIDE = [0, 23, 10, 29]
    UV_ARMOR_EDGE = [11, 23, 13, 33]
    UV_EAR_DISC = [14, 23, 20, 29]
    UV_EAR_RIM = [21, 27, 23, 33]
    UV_BATT_SIDE = [25, 23, 33, 28]
    UV_BATT_EDGE = [34, 23, 37, 31]
    UV_CAP_DIAL = [38, 23, 42, 27]
    UV_JBOX = [43, 23, 49, 28]
    UV_HEATSINK = [50, 23, 58, 31]
    UV_CABLE_BOOT = [0, 34, 12, 38]
    UV_SECONDARY_TRIM = [31, 34, 48, 38]
    UV_VENTS = [0, 52, 16, 60]
    UV_DEEP_CAVITY = [36, 52, 52, 60]
    UV_CHASSIS_CORE = [53, 52, 64, 57]
    UV_STRAP_GASKET = [0, 10, 17, 14]
    UV_MOUNT_LATCH = [45, 0, 47, 3]

    # Build optional bone cube lists (exact Explorer 2 coordinates in Bedrock geo-space)
    head_mod_bones: Dict[str, List[Dict]] = {
        "mod_pro_armor": [
            geo_cube_to_bb("pro_brow_r", [-4.75, 30.00, -4.85], [-1.85, 31.05, -4.25],
                           make_faces(UV_PRO_BROW, UV_PRO_RAIL, UV_PRO_VENT, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_PLATE)),
            geo_cube_to_bb("pro_brow_l", [2.35, 30.00, -4.85], [4.75, 31.05, -4.25],
                           make_faces(UV_PRO_BROW, UV_PRO_RAIL, UV_PRO_VENT, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_PLATE)),
            geo_cube_to_bb("pro_temple_r", [-5.15, 28.40, 1.10], [-4.75, 30.25, 4.25],
                           make_faces(UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_VENT, UV_PRO_VENT)),
            geo_cube_to_bb("pro_temple_l", [4.75, 28.40, 1.10], [5.15, 30.25, 4.25],
                           make_faces(UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_VENT, UV_PRO_VENT)),
            geo_cube_to_bb("pro_collar_r", [-4.75, 28.50, 4.75], [-1.60, 30.00, 5.15],
                           make_faces(UV_PRO_VENT, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_RAIL, UV_PRO_BROW, UV_PRO_BROW)),
            geo_cube_to_bb("pro_collar_l", [1.60, 28.50, 4.75], [4.75, 30.00, 5.15],
                           make_faces(UV_PRO_VENT, UV_PRO_RAIL, UV_PRO_PLATE, UV_PRO_RAIL, UV_PRO_BROW, UV_PRO_BROW)),
        ],
        "mod_solar": [
            geo_cube_to_bb("solar_bezel", [-2.25, 32.75, -3.50], [2.25, 33.15, 3.50],
                           make_faces(UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("solar_panel_cells", [-2.00, 33.15, -3.25], [2.00, 33.30, 3.25],
                           make_faces(UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_SOLAR_FRAME, UV_SOLAR_PANEL, UV_SOLAR_FRAME)),
        ],
        "mod_battery_pack": [
            geo_cube_to_bb("batt_pack_main", [-2.25, 27.75, 5.25], [2.25, 30.50, 6.75],
                           make_faces(UV_BATT_SIDE, UV_BATT_EDGE, UV_BATT_SIDE, UV_BATT_EDGE, UV_BATT_BODY, UV_BATT_BODY)),
            geo_cube_to_bb("batt_pack_led", [-1.75, 29.75, 6.75], [-0.50, 30.25, 6.95],
                           make_faces(UV_BATT_BEZEL, UV_BATT_BEZEL, UV_BATT_LED_GLOW, UV_BATT_BEZEL, UV_BATT_BEZEL, UV_BATT_BEZEL)),
            geo_cube_to_bb("batt_conduit_r", [-3.25, 28.25, 5.25], [-2.25, 29.25, 5.75],
                           make_faces(UV_CABLE_BOOT, UV_BATT_CAP, UV_CABLE_BOOT, UV_BATT_CAP, UV_BATT_CAP, UV_BATT_CAP)),
        ],
        "mod_flashlight": [
            geo_cube_to_bb("flash_clamp_r", [-5.65, 28.90, -2.25], [-5.25, 29.60, -0.75],
                           make_faces(UV_MOUNT_LATCH, UV_FLASH_BODY, UV_MOUNT_LATCH, UV_FLASH_BODY, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("flash_body_r", [-6.65, 28.50, -3.25], [-5.65, 30.00, 0.25],
                           make_faces(UV_FLASH_BODY, UV_FLASH_BODY, UV_BATT_CAP, UV_FLASH_BODY, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("flash_bezel_r", [-6.80, 28.35, -4.25], [-5.50, 30.15, -3.25],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_BEZEL_SIDE, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("flash_lens_r", [-6.55, 28.60, -4.45], [-5.75, 29.90, -4.25],
                           make_faces(UV_FLASH_LENS_GLOW, UV_FLASH_BEZEL, UV_FLASH_BEZEL, UV_FLASH_BEZEL, UV_FLASH_BEZEL, UV_FLASH_BEZEL)),
        ],
        "mod_lua_chip": [
            geo_cube_to_bb("lua_cartridge_l", [5.25, 28.65, -2.75], [6.00, 29.85, -0.75],
                           make_faces(UV_LUA_PCB, UV_LUA_PCB, UV_LUA_PCB, UV_LUA_PCB, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("lua_pins_l", [5.45, 29.85, -2.50], [5.85, 30.15, -1.00],
                           make_faces(UV_LUA_PINS, UV_CAP_DIAL, UV_LUA_PINS, UV_CAP_DIAL, UV_CAP_DIAL, UV_LUA_PINS)),
            geo_cube_to_bb("lua_status_led_l", [5.35, 29.00, -3.00], [5.90, 29.50, -2.75],
                           make_faces(UV_LUA_LED_GLOW, UV_LUA_PCB, UV_LUA_PCB, UV_LUA_PCB, UV_LUA_PCB, UV_LUA_PCB)),
        ],
        "mod_sonar": [
            geo_cube_to_bb("sonar_pod_l", [5.25, 28.50, -0.50], [6.25, 30.00, 1.00],
                           make_faces(UV_SONAR_POD, UV_EAR_RIM, UV_SONAR_POD, UV_EAR_DISC, UV_EAR_RIM, UV_EAR_RIM)),
            geo_cube_to_bb("sonar_dish_l", [6.25, 28.75, -0.25], [6.50, 29.75, 0.75],
                           make_faces(UV_SONAR_POD, UV_SONAR_POD, UV_SONAR_POD, UV_SONAR_LED_GLOW, UV_VENTS, UV_VENTS)),
        ],
        "mod_vital_info": [
            geo_cube_to_bb("vital_housing_r", [-5.15, 26.85, -1.75], [-4.35, 28.50, 0.50],
                           make_faces(UV_VITAL_POD, UV_JBOX, UV_VITAL_POD, UV_JBOX, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("vital_led_r", [-5.35, 27.35, -1.25], [-5.15, 27.95, 0.00],
                           make_faces(UV_VITAL_POD, UV_VITAL_LED_GLOW, UV_VITAL_POD, UV_VITAL_POD, UV_VITAL_POD, UV_VITAL_POD)),
        ],
        "mod_environment": [
            geo_cube_to_bb("env_housing_l", [4.35, 26.85, -1.75], [5.15, 28.50, 0.50],
                           make_faces(UV_ENV_POD, UV_VENTS, UV_ENV_POD, UV_VENTS, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("env_intake_l", [5.15, 27.35, -1.25], [5.35, 27.95, 0.00],
                           make_faces(UV_ENV_POD, UV_ENV_POD, UV_ENV_POD, UV_ENV_LED_GLOW, UV_HEATSINK, UV_HEATSINK)),
        ],
    }

    visor_mod_bones: Dict[str, List[Dict]] = {
        "mod_optics_base": [
            geo_cube_to_bb("base_c_barrel", [-0.75, 27.75, -9.50], [0.75, 29.25, -8.75],
                           make_faces(UV_DEEP_CAVITY, UV_BARREL_SIDE, UV_EYEPIECE, UV_BARREL_SIDE, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("base_c_ring", [-1.00, 27.50, -10.25], [1.00, 29.50, -9.50],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_OPT_COLLAR, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("base_c_lens", [-0.75, 27.75, -10.75], [0.75, 29.25, -10.25],
                           make_faces(UV_OPT_BASE_GLASS, UV_OPT_BASE_BEZEL, UV_OPT_COLLAR, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL)),
            geo_cube_to_bb("base_r_barrel", [-3.75, 25.25, -10.00], [-1.75, 27.25, -8.25],
                           make_faces(UV_DEEP_CAVITY, UV_BARREL_SIDE, UV_EYEPIECE, UV_BARREL_SIDE, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("base_r_ring", [-4.00, 25.00, -10.50], [-1.50, 27.50, -10.00],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_OPT_COLLAR, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("base_r_lens", [-3.75, 25.25, -10.80], [-1.75, 27.25, -10.50],
                           make_faces(UV_OPT_BASE_GLASS, UV_OPT_BASE_BEZEL, UV_OPT_COLLAR, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL)),
            geo_cube_to_bb("base_l_barrel", [1.75, 25.25, -10.00], [3.75, 27.25, -8.25],
                           make_faces(UV_DEEP_CAVITY, UV_BARREL_SIDE, UV_EYEPIECE, UV_BARREL_SIDE, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("base_l_ring", [1.50, 25.00, -10.50], [4.00, 27.50, -10.00],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_OPT_COLLAR, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("base_l_lens", [1.75, 25.25, -10.80], [3.75, 27.25, -10.50],
                           make_faces(UV_OPT_BASE_GLASS, UV_OPT_BASE_BEZEL, UV_OPT_COLLAR, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL, UV_OPT_BASE_BEZEL)),
        ],
        "mod_optics_nvg": [
            geo_cube_to_bb("nvg_r_barrel", [-3.75, 25.25, -10.00], [-1.75, 27.25, -8.25],
                           make_faces(UV_OPT_COLLAR, UV_BARREL_SIDE, UV_EYEPIECE, UV_BARREL_SIDE, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("nvg_r_ring", [-4.00, 25.00, -10.50], [-1.50, 27.50, -10.00],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_OPT_COLLAR, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("nvg_r_bezel", [-3.75, 25.25, -10.85], [-1.75, 27.25, -10.50],
                           make_faces(UV_BEZEL_FRONT, UV_BEZEL_SIDE, UV_OPT_COLLAR, UV_BEZEL_SIDE, UV_BEZEL_SIDE, UV_BEZEL_SIDE)),
            geo_cube_to_bb("nvg_r_lens", [-3.50, 25.50, -11.00], [-2.00, 27.00, -10.85],
                           make_faces(UV_OPT_NVG_GLOW, UV_OPT_NVG_BEZEL, UV_OPT_COLLAR, UV_OPT_NVG_BEZEL, UV_OPT_NVG_BEZEL, UV_OPT_NVG_BEZEL)),
            geo_cube_to_bb("nvg_l_barrel", [1.75, 25.25, -10.00], [3.75, 27.25, -8.25],
                           make_faces(UV_OPT_COLLAR, UV_BARREL_SIDE, UV_EYEPIECE, UV_BARREL_SIDE, UV_BARREL_TOP, UV_BARREL_TOP)),
            geo_cube_to_bb("nvg_l_ring", [1.50, 25.00, -10.50], [4.00, 27.50, -10.00],
                           make_faces(UV_BEZEL_FRONT, UV_KNURL_H, UV_OPT_COLLAR, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("nvg_l_bezel", [1.75, 25.25, -10.85], [3.75, 27.25, -10.50],
                           make_faces(UV_BEZEL_FRONT, UV_BEZEL_SIDE, UV_OPT_COLLAR, UV_BEZEL_SIDE, UV_BEZEL_SIDE, UV_BEZEL_SIDE)),
            geo_cube_to_bb("nvg_l_lens", [2.00, 25.50, -11.00], [3.50, 27.00, -10.85],
                           make_faces(UV_OPT_NVG_GLOW, UV_OPT_NVG_BEZEL, UV_OPT_COLLAR, UV_OPT_NVG_BEZEL, UV_OPT_NVG_BEZEL, UV_OPT_NVG_BEZEL)),
        ],
        "mod_optics_thermal": [
            geo_cube_to_bb("therm_c_housing", [-1.00, 28.35, -10.25], [1.00, 29.85, -8.75],
                           make_faces(UV_CHASSIS_CORE, UV_BEZEL_SIDE, UV_OPT_COLLAR, UV_BEZEL_SIDE, UV_CHASSIS_CORE, UV_CHASSIS_CORE)),
            geo_cube_to_bb("therm_c_cowl_t", [-1.00, 29.85, -10.65], [1.00, 30.10, -8.90],
                           make_faces(UV_HOOD_LIP, UV_OPT_COLLAR, UV_HOOD_LIP, UV_OPT_COLLAR, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("therm_c_cowl_b", [-1.00, 28.10, -10.65], [1.00, 28.35, -8.90],
                           make_faces(UV_HOOD_LIP, UV_OPT_COLLAR, UV_HOOD_LIP, UV_OPT_COLLAR, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("therm_c_lens", [-0.65, 28.60, -10.75], [0.65, 29.60, -10.25],
                           make_faces(UV_OPT_THERM_GLOW, UV_OPT_THERM_BEZEL, UV_OPT_COLLAR, UV_OPT_THERM_BEZEL, UV_OPT_THERM_BEZEL, UV_OPT_THERM_BEZEL)),
            geo_cube_to_bb("therm_fin_arm_r", [-1.50, 28.45, -9.40], [-1.00, 29.05, -8.80],
                           make_faces(UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("therm_fin_r", [-3.25, 28.25, -10.00], [-1.50, 29.25, -8.55],
                           make_faces(UV_HEATSINK, UV_OPT_COLLAR, UV_HEATSINK, UV_OPT_COLLAR, UV_HEATSINK, UV_HEATSINK)),
            geo_cube_to_bb("therm_fin_arm_l", [1.00, 28.45, -9.40], [1.50, 29.05, -8.80],
                           make_faces(UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("therm_fin_l", [1.50, 28.25, -10.00], [3.25, 29.25, -8.55],
                           make_faces(UV_HEATSINK, UV_OPT_COLLAR, UV_HEATSINK, UV_OPT_COLLAR, UV_HEATSINK, UV_HEATSINK)),
        ],
        "mod_optics_hydro": [
            geo_cube_to_bb("hydro_c_housing", [-1.25, 25.00, -9.75], [1.25, 26.25, -8.25],
                           make_faces(UV_BEZEL_FRONT, UV_STRAP_GASKET, UV_EYEPIECE, UV_STRAP_GASKET, UV_OPT_COLLAR, UV_OPT_COLLAR)),
            geo_cube_to_bb("hydro_c_hood", [-1.25, 25.95, -10.25], [1.25, 26.25, -9.75],
                           make_faces(UV_HOOD_LIP, UV_STRAP_GASKET, UV_HOOD_LIP, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET)),
            geo_cube_to_bb("hydro_c_lens", [-0.75, 25.20, -10.15], [0.75, 25.95, -9.75],
                           make_faces(UV_OPT_HYDRO_GLOW, UV_OPT_HYDRO_BEZEL, UV_OPT_COLLAR, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL)),
            geo_cube_to_bb("hydro_arm_r", [-4.05, 26.60, -8.15], [-3.50, 27.40, -7.35],
                           make_faces(UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET)),
            geo_cube_to_bb("hydro_pod_r", [-5.05, 26.00, -9.25], [-4.05, 27.75, -7.25],
                           make_faces(UV_BEZEL_FRONT, UV_BEZEL_SIDE, UV_EYEPIECE, UV_BEZEL_SIDE, UV_OPT_COLLAR, UV_OPT_COLLAR)),
            geo_cube_to_bb("hydro_hood_r", [-5.05, 27.50, -9.75], [-4.05, 27.85, -9.25],
                           make_faces(UV_HOOD_LIP, UV_STRAP_GASKET, UV_HOOD_LIP, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET)),
            geo_cube_to_bb("hydro_lens_r", [-4.85, 26.25, -9.60], [-4.25, 27.50, -9.25],
                           make_faces(UV_OPT_HYDRO_AUX_GLOW, UV_OPT_HYDRO_BEZEL, UV_OPT_COLLAR, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL)),
            geo_cube_to_bb("hydro_arm_l", [3.50, 26.60, -8.15], [4.05, 27.40, -7.35],
                           make_faces(UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET)),
            geo_cube_to_bb("hydro_pod_l", [4.05, 26.00, -9.25], [5.05, 27.75, -7.25],
                           make_faces(UV_BEZEL_FRONT, UV_BEZEL_SIDE, UV_EYEPIECE, UV_BEZEL_SIDE, UV_OPT_COLLAR, UV_OPT_COLLAR)),
            geo_cube_to_bb("hydro_hood_l", [4.05, 27.50, -9.75], [5.05, 27.85, -9.25],
                           make_faces(UV_HOOD_LIP, UV_STRAP_GASKET, UV_HOOD_LIP, UV_STRAP_GASKET, UV_STRAP_GASKET, UV_STRAP_GASKET)),
            geo_cube_to_bb("hydro_lens_l", [4.25, 26.25, -9.60], [4.85, 27.50, -9.25],
                           make_faces(UV_OPT_HYDRO_AUX_GLOW, UV_OPT_HYDRO_BEZEL, UV_OPT_COLLAR, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL, UV_OPT_HYDRO_BEZEL)),
        ],
        "mod_optics_bio": [
            geo_cube_to_bb("bio_brow_arm_l", [1.00, 29.35, -8.70], [1.55, 29.75, -8.25],
                           make_faces(UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE)),
            geo_cube_to_bb("bio_brow_pod_l", [1.55, 29.35, -9.60], [3.30, 30.20, -8.25],
                           make_faces(UV_ARMOR_SIDE, UV_ARMOR_EDGE, UV_OPT_COLLAR, UV_ARMOR_EDGE, UV_ARMOR_SIDE, UV_ARMOR_SIDE)),
            geo_cube_to_bb("bio_brow_lens_l", [1.80, 29.50, -9.85], [3.05, 30.00, -9.60],
                           make_faces(UV_OPT_BIO_AUX_GLOW, UV_OPT_BIO_BEZEL, UV_OPT_COLLAR, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL)),
            geo_cube_to_bb("bio_brow_arm_r", [-1.80, 29.50, -8.70], [-1.00, 29.75, -8.25],
                           make_faces(UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE)),
            geo_cube_to_bb("bio_brow_pod_r", [-3.30, 29.35, -9.60], [-1.80, 30.15, -8.55],
                           make_faces(UV_ARMOR_SIDE, UV_ARMOR_EDGE, UV_OPT_COLLAR, UV_ARMOR_EDGE, UV_ARMOR_SIDE, UV_ARMOR_SIDE)),
            geo_cube_to_bb("bio_pylon_r", [-2.90, 24.95, -8.25], [-2.40, 26.50, -7.45],
                           make_faces(UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE)),
            geo_cube_to_bb("bio_pod_r", [-3.50, 24.15, -10.40], [-1.80, 24.95, -7.80],
                           make_faces(UV_ARMOR_SIDE, UV_ARMOR_SIDE, UV_OPT_COLLAR, UV_ARMOR_SIDE, UV_ARMOR_SIDE, UV_DEEP_CAVITY)),
            geo_cube_to_bb("bio_lens_r", [-3.20, 24.30, -10.65], [-2.10, 24.80, -10.40],
                           make_faces(UV_OPT_BIO_GLOW, UV_OPT_BIO_BEZEL, UV_OPT_COLLAR, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL)),
            geo_cube_to_bb("bio_pylon_l", [2.40, 24.95, -8.25], [2.90, 26.50, -7.45],
                           make_faces(UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE, UV_ARMOR_EDGE)),
            geo_cube_to_bb("bio_pod_l", [1.80, 24.15, -10.40], [3.50, 24.95, -7.80],
                           make_faces(UV_ARMOR_SIDE, UV_ARMOR_SIDE, UV_OPT_COLLAR, UV_ARMOR_SIDE, UV_ARMOR_SIDE, UV_DEEP_CAVITY)),
            geo_cube_to_bb("bio_lens_l", [2.10, 24.30, -10.65], [3.20, 24.80, -10.40],
                           make_faces(UV_OPT_BIO_GLOW, UV_OPT_BIO_BEZEL, UV_OPT_COLLAR, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL, UV_OPT_BIO_BEZEL)),
        ],
        "mod_zoom": [
            geo_cube_to_bb("zoom_lug_r", [-2.10, 25.90, -7.25], [-1.25, 26.50, -6.60],
                           make_faces(UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("zoom_tab_r", [-4.45, 25.30, -7.20], [-1.25, 25.90, -6.60],
                           make_faces(UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM)),
            geo_cube_to_bb("zoom_runner_r", [-4.45, 25.30, -11.55], [-4.05, 25.90, -7.20],
                           make_faces(UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM)),
            geo_cube_to_bb("zoom_wall_r_t", [-4.05, 26.90, -12.75], [-1.50, 27.50, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE, UV_DEEP_CAVITY)),
            geo_cube_to_bb("zoom_wall_r_b", [-4.05, 25.00, -12.75], [-1.50, 25.60, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_DEEP_CAVITY, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_wall_r_out", [-4.05, 25.60, -12.75], [-3.40, 26.90, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_DEEP_CAVITY, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_wall_r_in", [-2.10, 25.60, -12.75], [-1.50, 26.90, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_DEEP_CAVITY, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_ring_r_t", [-4.25, 27.50, -12.15], [-1.35, 27.75, -11.55],
                           make_faces(UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_r_b", [-4.25, 24.75, -12.15], [-1.35, 25.00, -11.55],
                           make_faces(UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_r_out", [-4.25, 25.00, -12.15], [-4.05, 27.50, -11.55],
                           make_faces(UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_r_in", [-1.50, 25.00, -12.15], [-1.30, 27.50, -11.55],
                           make_faces(UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_lug_l", [1.25, 25.90, -7.25], [2.10, 26.50, -6.60],
                           make_faces(UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM, UV_EDGE_TRIM)),
            geo_cube_to_bb("zoom_tab_l", [1.25, 25.30, -7.20], [4.45, 25.90, -6.60],
                           make_faces(UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM)),
            geo_cube_to_bb("zoom_runner_l", [4.05, 25.30, -11.55], [4.45, 25.90, -7.20],
                           make_faces(UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_EDGE_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM, UV_SECONDARY_TRIM)),
            geo_cube_to_bb("zoom_wall_l_t", [1.50, 26.90, -12.75], [4.05, 27.50, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE, UV_DEEP_CAVITY)),
            geo_cube_to_bb("zoom_wall_l_b", [1.50, 25.00, -12.75], [4.05, 25.60, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_DEEP_CAVITY, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_wall_l_out", [3.40, 25.60, -12.75], [4.05, 26.90, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_DEEP_CAVITY, UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_wall_l_in", [1.50, 25.60, -12.75], [2.10, 26.90, -11.20],
                           make_faces(UV_ZOOM_COLLAR, UV_ZOOM_SLEEVE, UV_ZOOM_COLLAR, UV_DEEP_CAVITY, UV_ZOOM_SLEEVE, UV_ZOOM_SLEEVE)),
            geo_cube_to_bb("zoom_ring_l_t", [1.35, 27.50, -12.15], [4.25, 27.75, -11.55],
                           make_faces(UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_l_b", [1.35, 24.75, -12.15], [4.25, 25.00, -11.55],
                           make_faces(UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_l_out", [4.05, 25.00, -12.15], [4.25, 27.50, -11.55],
                           make_faces(UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
            geo_cube_to_bb("zoom_ring_l_in", [1.30, 25.00, -12.15], [1.50, 27.50, -11.55],
                           make_faces(UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_H, UV_KNURL_V, UV_KNURL_V)),
        ],
    }

    # Assemble elements list & groups/outliner
    elements: List[Dict] = []
    groups: List[Dict] = []

    head_grp = make_group("head", [0, 24, 0])
    visor_grp = make_group("visor_pivot", [0, 27.5, -5.5])
    groups.extend([head_grp, visor_grp])

    head_base_uuids: List[str] = []
    for name in head_base_names:
        el = copy.deepcopy(proto_by_name[name])
        elements.append(el)
        head_base_uuids.append(el["uuid"])

    visor_base_uuids: List[str] = []
    for name in visor_base_names:
        el = copy.deepcopy(proto_by_name[name])
        elements.append(el)
        visor_base_uuids.append(el["uuid"])

    visor_child_outliner: List = []
    for bone_name, cubes in visor_mod_bones.items():
        grp = make_group(bone_name, [0, 27.5, -5.5])
        groups.append(grp)
        cube_uuids: List[str] = []
        for c in cubes:
            elements.append(c)
            cube_uuids.append(c["uuid"])
        visor_child_outliner.append({"uuid": grp["uuid"], "isOpen": False, "children": cube_uuids})

    visor_outliner = {
        "uuid": visor_grp["uuid"],
        "isOpen": False,
        "children": visor_child_outliner + visor_base_uuids,
    }

    head_child_outliner: List = [visor_outliner]
    for bone_name, cubes in head_mod_bones.items():
        grp = make_group(bone_name, [0, 24, 0])
        groups.append(grp)
        cube_uuids = []
        for c in cubes:
            elements.append(c)
            cube_uuids.append(c["uuid"])
        head_child_outliner.append({"uuid": grp["uuid"], "isOpen": False, "children": cube_uuids})

    outliner = [{
        "uuid": head_grp["uuid"],
        "isOpen": False,
        "children": head_child_outliner + head_base_uuids,
    }]

    # Embed 64x64 PNG as base64 data URL
    tex_bytes = (TEX_DIR / texture_filename).read_bytes()
    b64 = base64.b64encode(tex_bytes).decode("ascii")
    tex_entry = {
        "path": "",
        "name": texture_filename,
        "folder": "",
        "namespace": "",
        "id": "0",
        "width": 64,
        "height": 64,
        "uv_width": 64,
        "uv_height": 64,
        "particle": False,
        "use_as_default": False,
        "layers_enabled": False,
        "sync_to_project": "",
        "render_mode": "default",
        "render_sides": "auto",
        "frame_time": 1,
        "frame_order_type": "loop",
        "frame_order": "",
        "frame_interpolate": False,
        "visible": True,
        "internal": True,
        "saved": True,
        "uuid": det_uuid(f"tex:{texture_filename}"),
        "source": f"data:image/png;base64,{b64}",
    }

    return {
        "meta": {
            "format_version": "5.0",
            "model_format": "geckolib_model",
            "box_uv": False,
        },
        "name": identifier,
        "model_identifier": identifier,
        "visible_box": [1, 1, 0],
        "variable_placeholders": "",
        "variable_placeholder_buttons": [],
        "bedrock_animation_version": "1.8.0",
        "timeline_setups": [],
        "unhandled_root_fields": {},
        "resolution": {"width": 64, "height": 64},
        "elements": elements,
        "groups": groups,
        "outliner": outliner,
        "textures": [tex_entry],
        "animations": build_animations(visor_grp["uuid"]),
        "export_options": {},
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--stage-tmp", action="store_true", help="Also write _tmp_*.bbmodel inside geo/ for bbmodel MCP validation")
    args = parser.parse_args()

    MASTER_DIR.mkdir(parents=True, exist_ok=True)
    mod_bb = build_master_bbmodel("tri_lens_modular.png", "modular_goggles")
    pro_bb = build_master_bbmodel("tri_lens_pro_modular.png", "pro_modular_goggles")

    mod_path = MASTER_DIR / "tri_lens_master_modular.bbmodel"
    pro_path = MASTER_DIR / "tri_lens_master_pro_modular.bbmodel"
    mod_path.write_text(json.dumps(mod_bb, indent=2) + "\n", encoding="utf-8")
    pro_path.write_text(json.dumps(pro_bb, indent=2) + "\n", encoding="utf-8")
    print(f"[OK] Wrote {mod_path} ({len(mod_bb['elements'])} cubes, {len(mod_bb['groups'])} groups)")
    print(f"[OK] Wrote {pro_path} ({len(pro_bb['elements'])} cubes, {len(pro_bb['groups'])} groups)")

    if args.stage_tmp:
        tmp_mod = GEO_DIR / "_tmp_tri_lens_master.bbmodel"
        tmp_pro = GEO_DIR / "_tmp_tri_lens_master_pro.bbmodel"
        tmp_mod.write_text(json.dumps(mod_bb, indent=2) + "\n", encoding="utf-8")
        tmp_pro.write_text(json.dumps(pro_bb, indent=2) + "\n", encoding="utf-8")
        print(f"[OK] Staged {tmp_mod} and {tmp_pro} for bbmodel MCP validation")


if __name__ == "__main__":
    main()
