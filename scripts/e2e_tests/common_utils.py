"""Shared constants, safe workspace loaders, Java contract helpers, and test harness."""

from __future__ import annotations

import json
import os
import re
import tempfile
import time
import traceback
from dataclasses import dataclass, asdict
from pathlib import Path
from typing import Callable, Dict, Iterable, List, Optional, Sequence, Tuple

import numpy as np
from PIL import Image

REPO_ROOT = Path(__file__).resolve().parent.parent.parent
ASSETS_ROOT = REPO_ROOT / "common" / "src" / "main" / "resources" / "assets" / "vision_goggles"
GEO_DIR = ASSETS_ROOT / "geo" / "item"
ANIM_DIR = ASSETS_ROOT / "animations" / "item"
TEX_DIR = ASSETS_ROOT / "textures" / "entities"
ITEM_MODEL_DIR = ASSETS_ROOT / "models" / "item"
LANG_DIR = ASSETS_ROOT / "lang"

JAVA_COMMON = REPO_ROOT / "common" / "src" / "main" / "java" / "dev" / "itsrealperson" / "vision_goggles"
JAVA_FABRIC = REPO_ROOT / "fabric" / "src" / "main" / "java" / "dev" / "itsrealperson" / "vision_goggles"
JAVA_FORGE = REPO_ROOT / "forge" / "src" / "main" / "java" / "dev" / "itsrealperson" / "vision_goggles"

MODELS_SRC_PROTO = REPO_ROOT / "models_src" / "tri_lens_proto"
MODELS_SRC_MASTER = REPO_ROOT / "models_src" / "tri_lens_master"

DEFAULT_OUT_DIR = os.path.join(tempfile.gettempdir(), "vision_goggles_e2e_artifacts")

STATIC_GOGGLE_ITEMS = (
    "night_vision_goggles",
    "thermal_goggles",
    "biometric_goggles",
    "hydro_goggles",
)
MODULAR_GOGGLE_ITEMS = (
    "modular_goggles",
    "pro_modular_goggles",
)
ALL_GOGGLE_ITEMS = STATIC_GOGGLE_ITEMS + MODULAR_GOGGLE_ITEMS

STATIC_TEXTURES = {
    "night_vision_goggles": ("tri_lens_nvg.png", "tri_lens_nvg_glow.png"),
    "thermal_goggles": ("tri_lens_thermal.png", "tri_lens_thermal_glow.png"),
    "biometric_goggles": ("tri_lens_bio.png", "tri_lens_bio_glow.png"),
    "hydro_goggles": ("tri_lens_hydro.png", "tri_lens_hydro_glow.png"),
}

MODULAR_TEXTURES = {
    "modular_goggles": ("tri_lens_modular.png", "tri_lens_modular_glow.png"),
    "pro_modular_goggles": ("tri_lens_pro_modular.png", "tri_lens_pro_modular_glow.png"),
}

ALL_TARGET_TEXTURES = {**STATIC_TEXTURES, **MODULAR_TEXTURES}

# Allowed emissive bounding boxes in half-open (x1, y1, x2, y2) 64x64 pixel coords
# from scripts/generate_tri_lens_textures.py
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

NEW_MODULAR_GLOW_RECTS: List[Tuple[int, int, int, int]] = [
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

MODULAR_GLOW_ALLOWED_RECTS: List[Tuple[int, int, int, int]] = (
    GLOW_ALLOWED_RECTS + NEW_MODULAR_GLOW_RECTS
)

# Master Modular bone hierarchy contract (PROJECT.md § Interface Contracts 2)
HEAD_MOD_BONES = (
    "mod_pro_armor",
    "mod_solar",
    "mod_battery_pack",
    "mod_flashlight",
    "mod_lua_chip",
    "mod_sonar",
    "mod_vital_info",
    "mod_environment",
)

VISOR_MOD_BONES = (
    "mod_zoom",
    "mod_optics_base",
    "mod_optics_nvg",
    "mod_optics_thermal",
    "mod_optics_bio",
    "mod_optics_hydro",
)

ALL_MOD_BONES = HEAD_MOD_BONES + VISOR_MOD_BONES

STEVE_HEAD_BOUNDS = (-4.0, 4.0, 24.0, 32.0, -4.0, 4.0)  # xmin, xmax, ymin, ymax, zmin, zmax

LEGACY_128_TEXTURES = (
    "nvg_texture.png",
    "nvg_glow.png",
    "thermal_vision.png",
    "thermal_glow.png",
    "hidro_vision.png",
    "hidro_glow.png",
    "bio_vision.png",
    "bio_glow.png",
)

LEGACY_VARIANT_GEOS = (
    "modular_goggles_bio.geo.json",
    "modular_goggles_hydro.geo.json",
    "modular_goggles_thermal.geo.json",
    "pro_modular_goggles_bio.geo.json",
    "pro_modular_goggles_hydro.geo.json",
    "pro_modular_goggles_thermal.geo.json",
)

PREVIEW_DEDICATED_FILES = (
    JAVA_COMMON / "network" / "OpenProtoPreviewPacket.java",
    JAVA_COMMON / "client" / "gui" / "TriLensPreviewScreen.java",
    JAVA_COMMON / "client" / "preview" / "ProtoTriLensGeoRenderer.java",
    JAVA_COMMON / "client" / "preview" / "ProtoTriLensPreviewState.java",
    JAVA_COMMON / "client" / "preview" / "ProtoTriLensVariant.java",
)

PREVIEW_SYMBOLS = (
    "ProtoTriLensGeoRenderer",
    "ProtoTriLensPreviewState",
    "ProtoTriLensVariant",
    "TriLensPreviewScreen",
    "OpenProtoPreviewPacket",
    "openTriLensPreviewScreen",
    "handleProtoPreview",
    "openProtoPreview",
)

LEGACY_MODEL_FILES = (
    JAVA_COMMON / "client" / "VisionGoggleModel.java",
    JAVA_COMMON / "client" / "FirstPersonVisorModel.java",
)


@dataclass
class TestRecord:
    test_id: str
    tier: int
    feature: str
    description: str
    status: str  # PASS, FAIL, SKIPPED
    duration_ms: float
    message: str = ""
    details: Optional[Dict] = None

    def to_dict(self) -> Dict:
        return asdict(self)


def run_test_case(
    test_id: str,
    tier: int,
    feature: str,
    description: str,
    fn: Callable[[], Optional[Dict]],
) -> TestRecord:
    """Execute one test inside an isolated wrapper catching AssertionError & Exception."""
    t0 = time.perf_counter()
    try:
        details = fn()
        dt = (time.perf_counter() - t0) * 1000.0
        return TestRecord(
            test_id=test_id,
            tier=tier,
            feature=feature,
            description=description,
            status="PASS",
            duration_ms=round(dt, 2),
            message="OK",
            details=details,
        )
    except AssertionError as exc:
        dt = (time.perf_counter() - t0) * 1000.0
        return TestRecord(
            test_id=test_id,
            tier=tier,
            feature=feature,
            description=description,
            status="FAIL",
            duration_ms=round(dt, 2),
            message=str(exc) or "Assertion failed",
        )
    except Exception as exc:  # pragma: no cover - defensive isolation
        dt = (time.perf_counter() - t0) * 1000.0
        tb = traceback.format_exc(limit=3).strip().splitlines()[-1]
        return TestRecord(
            test_id=test_id,
            tier=tier,
            feature=feature,
            description=description,
            status="FAIL",
            duration_ms=round(dt, 2),
            message=f"Unexpected {type(exc).__name__}: {exc} ({tb})",
        )


def read_text_safe(path: Path, label: str = "") -> str:
    if not path.is_file():
        desc = f"{label} ({path})" if label else str(path)
        raise AssertionError(f"Missing required file: {desc}")
    try:
        return path.read_text(encoding="utf-8")
    except Exception as exc:
        raise AssertionError(f"Failed reading text file {path}: {exc}") from exc


def read_json_safe(path: Path, label: str = "") -> Dict:
    text = read_text_safe(path, label=label)
    try:
        data = json.loads(text)
    except json.JSONDecodeError as exc:
        raise AssertionError(f"Malformed JSON in {path}: {exc}") from exc
    if not isinstance(data, dict):
        raise AssertionError(f"Expected JSON object in {path}, got {type(data).__name__}")
    return data


def load_png_rgba(path: Path, expected_size: Optional[Tuple[int, int]] = (64, 64)) -> np.ndarray:
    if not path.is_file():
        raise AssertionError(f"Missing PNG texture: {path}")
    try:
        with Image.open(path) as img:
            if expected_size is not None and img.size != expected_size:
                raise AssertionError(
                    f"Texture {path.name} has size {img.size}, expected {expected_size}"
                )
            rgba = img.convert("RGBA")
            return np.array(rgba, dtype=np.uint8)
    except AssertionError:
        raise
    except Exception as exc:
        raise AssertionError(f"Failed loading PNG {path}: {exc}") from exc


def strip_java_comments(code: str) -> str:
    """Remove // line comments and /* ... */ block comments while preserving string literals."""
    pattern = re.compile(
        r'("""[\s\S]*?""")|("(?:\\.|[^"\\])*")|(/\*[\s\S]*?\*/)|(//[^\n]*)',
        re.MULTILINE,
    )
    return pattern.sub(lambda m: m.group(1) or m.group(2) if (m.group(1) or m.group(2)) else " ", code)


def strip_java_comments_and_strings(code: str) -> str:
    """Remove comments and empty out string/char/text-block literals to prevent literal-string bypasses."""
    pattern = re.compile(
        r'("""[\s\S]*?""")|("(?:\\.|[^"\\])*")|(\'(?:\\.|[^\'\\])*\')|(/\*[\s\S]*?\*/)|(//[^\n]*)',
        re.MULTILINE,
    )

    def _repl(m: re.Match[str]) -> str:
        if m.group(1) or m.group(2):
            return '""'
        if m.group(3):
            return "''"
        return " "

    return pattern.sub(_repl, code)


def has_symbol_token(text: str, sym: str) -> bool:
    """Boundary-aware symbol check so e.g. 'nvg_glow.png' does not match 'tri_lens_nvg_glow.png'."""
    pat = r"(?<![A-Za-z0-9_])" + re.escape(sym) + r"(?![A-Za-z0-9_])"
    return bool(re.search(pat, text))


def unwrap_anim_vec(val):
    """Unwrap Blockbench/GeckoLib keyframe dict {'0.0': [...]} or {'0.0': {'post': [...]}} into a list."""
    if isinstance(val, list):
        return val
    if isinstance(val, dict):
        first = val.get("0.0") or val.get("0") or next(iter(val.values()), None)
        if isinstance(first, dict) and "post" in first:
            return first["post"]
        return first
    return val


def iter_client_render_helper_files() -> List[Path]:
    """Return non-preview client helper/model/animator Java files that may hold extracted render logic."""
    client_dir = JAVA_COMMON / "client"
    candidates: List[Path] = [
        client_dir / "model" / "VisionGogglesGeoModel.java",
        client_dir / "renderer" / "VisionGogglesClientRenderHelper.java",
        client_dir / "GogglesFlipAnimator.java",
    ]
    for pat in ("*Helper*.java", "*TriLens*.java", "*Bone*.java", "*Modular*.java"):
        for p in sorted(client_dir.rglob(pat)):
            if "preview" in p.parts or "Preview" in p.name or "ClientGuiHelper" in p.name:
                continue
            candidates.append(p)
    out: List[Path] = []
    seen = set()
    for p in candidates:
        if p.is_file() and p not in seen:
            seen.add(p)
            out.append(p)
    return out


def get_combined_java_views(
    primary_paths: Sequence[Path],
    include_client_helpers: bool = True,
) -> Tuple[str, str]:
    """
    Return (comment_stripped_with_strings, comments_and_strings_stripped) for primary files
    plus any non-preview client helper/model/animator files transitively referenced by them.
    """
    seen = set()
    clean_chunks: List[str] = []
    code_chunks: List[str] = []
    for p in primary_paths:
        if p.is_file() and p not in seen:
            seen.add(p)
            raw = p.read_text(encoding="utf-8", errors="ignore")
            clean_chunks.append(strip_java_comments(raw))
            code_chunks.append(strip_java_comments_and_strings(raw))

    if include_client_helpers:
        helpers = iter_client_render_helper_files()
        changed = True
        while changed:
            changed = False
            combined_code = "\n".join(code_chunks)
            for h in helpers:
                if h in seen or not h.is_file():
                    continue
                if re.search(r"\b" + re.escape(h.stem) + r"\b", combined_code):
                    seen.add(h)
                    raw = h.read_text(encoding="utf-8", errors="ignore")
                    clean_chunks.append(strip_java_comments(raw))
                    code_chunks.append(strip_java_comments_and_strings(raw))
                    changed = True

    return "\n".join(clean_chunks), "\n".join(code_chunks)


def _extract_balanced(text: str, open_idx: int, open_ch: str, close_ch: str) -> Optional[int]:
    depth = 0
    for i in range(open_idx, len(text)):
        ch = text[i]
        if ch == open_ch:
            depth += 1
        elif ch == close_ch:
            depth -= 1
            if depth == 0:
                return i
    return None


def _is_positive_main_hand_cond(cond: str) -> bool:
    if re.search(r"!\s*\([^)]*InteractionHand\.(?:MAIN_HAND|OFF_HAND)", cond):
        return False
    return bool(
        re.search(
            r"(?:\bhand\s*==\s*InteractionHand\.MAIN_HAND\b|\bInteractionHand\.MAIN_HAND\s*==\s*hand\b"
            r"|\bhand\s*!=\s*InteractionHand\.OFF_HAND\b|\bInteractionHand\.OFF_HAND\s*!=\s*hand\b)",
            cond,
        )
    )


def has_main_hand_sync_guard(
    mixin_java: str,
    equip_animator_java: Optional[str] = None,
) -> Tuple[bool, str]:
    """
    Verify MixinItemInHandRenderer calls GogglesEquipAnimator.syncFromRender at most/exactly once
    and strictly guarded to InteractionHand.MAIN_HAND (no string-literal or empty-if bypasses).
    """
    code = strip_java_comments_and_strings(mixin_java)
    calls = [m.start() for m in re.finditer(r"\bGogglesEquipAnimator\.syncFromRender\s*\(", code)]
    if len(calls) == 0:
        return False, "Missing GogglesEquipAnimator.syncFromRender call in MixinItemInHandRenderer"
    if len(calls) > 1:
        return False, f"Expected 1 GogglesEquipAnimator.syncFromRender call, found {len(calls)}"

    call_idx = calls[0]
    # Scan all `if (...) { ... }` or `if (...) ...;` statements to see if call_idx lies inside a positive MAIN_HAND guard
    for m in re.finditer(r"\bif\s*\(", code):
        lparen = m.end() - 1
        rparen = _extract_balanced(code, lparen, "(", ")")
        if rparen is None:
            continue
        cond = code[lparen + 1 : rparen]
        if not _is_positive_main_hand_cond(cond):
            continue
        j = rparen + 1
        while j < len(code) and code[j].isspace():
            j += 1
        if j >= len(code):
            continue
        if code[j] == "{":
            rbrace = _extract_balanced(code, j, "{", "}")
            if rbrace is not None and (j < call_idx < rbrace):
                return True, "OK"
        else:
            semi = code.find(";", j)
            if semi != -1 and (j <= call_idx < semi):
                return True, "OK"

    # Also allow delegating `hand` directly into GogglesEquipAnimator.syncFromRender(..., hand, ...)
    # provided GogglesEquipAnimator.syncFromRender itself guards on InteractionHand.MAIN_HAND/OFF_HAND.
    call_lparen = code.find("(", call_idx)
    call_rparen = _extract_balanced(code, call_lparen, "(", ")") if call_lparen != -1 else None
    if call_rparen is not None and equip_animator_java is not None:
        args_str = code[call_lparen + 1 : call_rparen]
        if re.search(r"\bhand\b", args_str):
            ea_code = strip_java_comments_and_strings(equip_animator_java)
            m_decl = re.search(r"\bsyncFromRender\s*\([^)]*\bInteractionHand\b[^)]*\)\s*\{", ea_code)
            if m_decl:
                brace_open = m_decl.end() - 1
                brace_close = _extract_balanced(ea_code, brace_open, "{", "}")
                if brace_close is not None:
                    body = ea_code[brace_open + 1 : brace_close]
                    early_ret = re.search(
                        r"\bif\s*\([^)]*(?:!=\s*InteractionHand\.MAIN_HAND|InteractionHand\.MAIN_HAND\s*!=|==\s*InteractionHand\.OFF_HAND|InteractionHand\.OFF_HAND\s*==)[^)]*\)\s*(?:\{\s*return\s*;\s*\}|return\s*;)",
                        body,
                        re.DOTALL,
                    )
                    pos_wrap = re.search(
                        r"\bif\s*\([^)]*(?:==\s*InteractionHand\.MAIN_HAND|InteractionHand\.MAIN_HAND\s*==|!=\s*InteractionHand\.OFF_HAND|InteractionHand\.OFF_HAND\s*!=)[^)]*\)\s*\{[^}]+\}",
                        body,
                        re.DOTALL,
                    )
                    if early_ret or pos_wrap:
                        return True, "OK (guarded inside GogglesEquipAnimator.syncFromRender)"

    return False, "MixinItemInHandRenderer calls GogglesEquipAnimator.syncFromRender without active MAIN_HAND guard"


def iter_java_files(roots: Sequence[Path] = (JAVA_COMMON, JAVA_FABRIC, JAVA_FORGE)) -> List[Path]:
    files: List[Path] = []
    for root in roots:
        if root.exists():
            files.extend(sorted(root.rglob("*.java")))
    return files


def is_in_glow_rects(x: int, y: int, rects: Sequence[Tuple[int, int, int, int]] = GLOW_ALLOWED_RECTS) -> bool:
    for x1, y1, x2, y2 in rects:
        if x1 <= x < x2 and y1 <= y < y2:
            return True
    return False


def check_glow_mask_hygiene(
    glow_arr: np.ndarray,
    allowed_rects: Sequence[Tuple[int, int, int, int]] = GLOW_ALLOWED_RECTS,
) -> Tuple[int, int, List[Tuple[int, int]]]:
    """Return (inside_lit_count, outside_bleed_count, sample_bleed_coords)."""
    alpha = glow_arr[:, :, 3]
    rgb_sum = glow_arr[:, :, :3].sum(axis=2)
    nonzero_ys, nonzero_xs = np.where((alpha > 0) | (rgb_sum > 0))
    inside = 0
    bleed_coords: List[Tuple[int, int]] = []
    for y, x in zip(nonzero_ys.tolist(), nonzero_xs.tolist()):
        if is_in_glow_rects(x, y, allowed_rects):
            if alpha[y, x] > 0:
                inside += 1
        else:
            bleed_coords.append((x, y))
    return inside, len(bleed_coords), bleed_coords[:12]


def resolve_modular_geo_path(item_id: str) -> Path:
    """Support either shared modular_goggles.geo.json or matched pro_modular_goggles.geo.json."""
    if item_id == "pro_modular_goggles":
        pro_path = GEO_DIR / "pro_modular_goggles.geo.json"
        mod_path = GEO_DIR / "modular_goggles.geo.json"
        if pro_path.is_file():
            # Check if it's already migrated to 64x64; if VisionGogglesGeoModel routes to modular_goggles, prefer modular_goggles if pro_path is absent
            return pro_path
        return mod_path
    return GEO_DIR / f"{item_id}.geo.json"


def resolve_geo_path_via_model(item_id: str) -> Path:
    """Inspect VisionGogglesGeoModel.java if available to respect routing pro_modular -> modular_goggles."""
    candidate = GEO_DIR / f"{item_id}.geo.json"
    if item_id == "pro_modular_goggles":
        vg_model_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        if vg_model_path.is_file():
            txt = strip_java_comments(vg_model_path.read_text(encoding="utf-8"))
            if "pro_modular_goggles.geo.json" not in txt and "modular_goggles.geo.json" in txt:
                return GEO_DIR / "modular_goggles.geo.json"
        if not candidate.is_file() and (GEO_DIR / "modular_goggles.geo.json").is_file():
            return GEO_DIR / "modular_goggles.geo.json"
    return candidate


def resolve_java_modular_nbt_rules(
    modules: Iterable[str],
    is_pro: bool = False,
    neutral_chassis: bool = False,
) -> Dict[str, bool]:
    """
    Canonical opaque-box expectation for Master Modular NBT -> bone visibility
    per PROJECT.md § Interface Contracts 2 & Explorer 2 § 1.2.
    """
    vis = {bone: False for bone in ALL_MOD_BONES}
    if neutral_chassis:
        vis["mod_optics_base"] = True
        return vis

    if is_pro:
        vis["mod_pro_armor"] = True

    utility_map = {
        "solar": "mod_solar",
        "battery_expansion": "mod_battery_pack",
        "zoom": "mod_zoom",
        "flashlight": "mod_flashlight",
        "sonar": "mod_sonar",
        "vital_info": "mod_vital_info",
        "environment": "mod_environment",
    }
    vision_map = {
        "nvg": "mod_optics_nvg",
        "night_vision": "mod_optics_nvg",
        "nvg_module": "mod_optics_nvg",
        "night_vision_module": "mod_optics_nvg",
        "thermal": "mod_optics_thermal",
        "thermal_module": "mod_optics_thermal",
        "bio": "mod_optics_bio",
        "biometric": "mod_optics_bio",
        "bio_module": "mod_optics_bio",
        "biometric_module": "mod_optics_bio",
        "hydro": "mod_optics_hydro",
        "hydro_module": "mod_optics_hydro",
    }

    has_any_vision = False
    for raw in modules:
        norm = raw.strip().lower()
        short = norm.split(":", 1)[1] if ":" in norm else norm
        if short in vision_map:
            vis[vision_map[short]] = True
            has_any_vision = True
        elif short in utility_map:
            vis[utility_map[short]] = True
        elif short in ("programmable", "generic") or short not in (
            "chunk_viewer",
        ):
            # Programmed Lua script ID (e.g. "threat_scanner", "vision_goggles:thermal_palette")
            # activates mod_lua_chip WITHOUT triggering hardware vision modes!
            vis["mod_lua_chip"] = True

    vis["mod_optics_base"] = not has_any_vision
    return vis
