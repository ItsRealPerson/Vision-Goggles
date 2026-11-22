"""Tier 1: Atomic unit & asset/code contract tests (59 tests, >= 5 per feature F1..F11)."""

from __future__ import annotations

import re
from pathlib import Path
from typing import List

import numpy as np

from .common_utils import (
    ALL_GOGGLE_ITEMS,
    ANIM_DIR,
    GEO_DIR,
    GLOW_ALLOWED_RECTS,
    HEAD_MOD_BONES,
    ITEM_MODEL_DIR,
    JAVA_COMMON,
    JAVA_FABRIC,
    JAVA_FORGE,
    LEGACY_128_TEXTURES,
    LEGACY_MODEL_FILES,
    LEGACY_VARIANT_GEOS,
    MODULAR_GLOW_ALLOWED_RECTS,
    PREVIEW_DEDICATED_FILES,
    PREVIEW_SYMBOLS,
    STATIC_GOGGLE_ITEMS,
    STATIC_TEXTURES,
    TEX_DIR,
    VISOR_MOD_BONES,
    TestRecord,
    check_glow_mask_hygiene,
    get_combined_java_views,
    has_main_hand_sync_guard,
    has_symbol_token,
    is_in_glow_rects,
    iter_java_files,
    load_png_rgba,
    read_json_safe,
    read_text_safe,
    resolve_geo_path_via_model,
    run_test_case,
    strip_java_comments,
    strip_java_comments_and_strings,
    unwrap_anim_vec,
)
from .rasterizer3d import check_bone_steve_head_overlap, parse_fp_visor_params, parse_geo_model


def _check_geo_uvs_in_bounds(geo_path: Path) -> List[str]:
    geo = parse_geo_model(geo_path)
    oob: List[str] = []
    for bone in geo.bones_by_name.values():
        for idx, cube in enumerate(bone.cubes):
            for fname, ((u0, v0), (du, dv)) in cube.uv_faces.items():
                u1, v1 = u0 + du, v0 + dv
                umin, umax = min(u0, u1), max(u0, u1)
                vmin, vmax = min(v0, v1), max(v0, v1)
                if umin < 0 or umax > geo.tex_w or vmin < 0 or vmax > geo.tex_h:
                    oob.append(f"{bone.name}[{idx}].{fname}=({u0},{v0})+({du},{dv})")
    return oob


def run_tier1() -> List[TestRecord]:
    cases = []

    # ---------------- F1: Promote Tri-Lens Static Geos, Textures & Animations (6) ----------------
    def t1_f1_01():
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            assert geo.tex_w == 64 and geo.tex_h == 64, (
                f"{item}.geo.json has texture {geo.tex_w}x{geo.tex_h}, expected 64x64 Tri-Lens"
            )

    def t1_f1_02():
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            assert "head" in geo.bones_by_name, f"{item}.geo.json missing 'head' bone"
            assert tuple(geo.bones_by_name["head"].pivot) == (0.0, 24.0, 0.0), (
                f"{item}.geo.json head pivot={geo.bones_by_name['head'].pivot}, expected (0,24,0)"
            )
            assert "visor_pivot" in geo.bones_by_name, (
                f"{item}.geo.json missing unified 'visor_pivot' bone (bones={list(geo.bones_by_name.keys())})"
            )
            vp = geo.bones_by_name["visor_pivot"]
            assert vp.parent == "head", f"{item}: visor_pivot parent={vp.parent}, expected 'head'"
            assert tuple(vp.pivot) == (0.0, 27.5, -5.5), f"{item}: visor_pivot pivot={vp.pivot}"
            assert "bone" not in geo.bones_by_name and "visor" not in geo.bones_by_name, (
                f"{item}.geo.json still contains legacy 'bone'/'visor'"
            )

    def t1_f1_03():
        for item in STATIC_GOGGLE_ITEMS:
            p = GEO_DIR / f"{item}.geo.json"
            geo = parse_geo_model(p)
            overlaps = check_bone_steve_head_overlap(geo)
            assert not overlaps, f"{item}.geo.json overlaps Steve head: {overlaps[:5]}"
            oob = _check_geo_uvs_in_bounds(p)
            assert not oob, f"{item}.geo.json has OOB UVs: {oob[:5]}"

    def t1_f1_04():
        for item, (base_name, glow_name) in STATIC_TEXTURES.items():
            base = load_png_rgba(TEX_DIR / base_name, (64, 64))
            glow = load_png_rgba(TEX_DIR / glow_name, (64, 64))
            assert int(base[:, :, 3].max()) == 255, f"{base_name} has no opaque pixels"
            assert int(glow[:, :, 3].max()) > 0, f"{glow_name} has no emissive pixels"

    def t1_f1_05():
        for item, (_, glow_name) in STATIC_TEXTURES.items():
            glow = load_png_rgba(TEX_DIR / glow_name, (64, 64))
            inside, bleed, sample = check_glow_mask_hygiene(glow)
            assert inside > 0, f"{glow_name} has 0 emissive pixels inside GLOW_ALLOWED_RECTS"
            assert bleed == 0, f"{glow_name} has {bleed} glow bleed pixels outside allowed rects: {sample}"

    def t1_f1_06():
        anim = read_json_safe(ANIM_DIR / "vision_goggles.animation.json", "vision_goggles.animation.json")
        anims = anim.get("animations", {})
        for clip in ("idle_down", "idle_up", "flip_down", "flip_up"):
            assert clip in anims, f"vision_goggles.animation.json missing '{clip}'"
            bones = anims[clip].get("bones", {})
            assert "visor_pivot" in bones, (
                f"vision_goggles.animation.json '{clip}' must animate 'visor_pivot' (found {list(bones.keys())})"
            )
            assert "visor" not in bones, f"'{clip}' still animates legacy 'visor' instead of 'visor_pivot'"
        up_bones = anims["idle_up"]["bones"]["visor_pivot"]
        rot = unwrap_anim_vec(up_bones.get("rotation"))
        pos = unwrap_anim_vec(up_bones.get("position"))
        assert rot == [-60, 0, 0], f"idle_up rot={rot}"
        assert pos == [0, 2, -0.75], f"idle_up pos={pos}"

    cases.extend([
        ("T1_F1_01", 1, "F1", "All 4 static goggle geos are 64x64 Tri-Lens", t1_f1_01),
        ("T1_F1_02", 1, "F1", "All 4 static geos use head [0,24,0] + visor_pivot [0,27.5,-5.5]", t1_f1_02),
        ("T1_F1_03", 1, "F1", "All 4 static geos have zero Steve-head overlap & zero OOB UVs", t1_f1_03),
        ("T1_F1_04", 1, "F1", "All 4 static tri_lens_*.png + *_glow.png exist as 64x64 RGBA", t1_f1_04),
        ("T1_F1_05", 1, "F1", "All 4 static *_glow.png have zero glow bleed outside allowed rects", t1_f1_05),
        ("T1_F1_06", 1, "F1", "vision_goggles.animation.json animates visor_pivot with Tri-Lens poses", t1_f1_06),
    ])

    # ---------------- F2: Regenerate chassis_neutral.png (64x64) (5) ----------------
    def t1_f2_01():
        arr = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        assert arr.shape == (64, 64, 4)

    def t1_f2_02():
        assert not (TEX_DIR / "chassis_neutral_glow.png").exists(), (
            "chassis_neutral_glow.png must NOT exist (zero glow for neutral chassis)"
        )

    def t1_f2_03():
        arr = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        max_lum = 0.0
        max_sat = 0
        for x1, y1, x2, y2 in GLOW_ALLOWED_RECTS:
            patch = arr[y1:y2, x1:x2, :3].astype(np.float32)
            lum = 0.2126 * patch[:, :, 0] + 0.7152 * patch[:, :, 1] + 0.0722 * patch[:, :, 2]
            sat = patch.max(axis=2) - patch.min(axis=2)
            max_lum = max(max_lum, float(lum.max()))
            max_sat = max(max_sat, int(sat.max()))
        assert max_lum < 90.0, f"chassis_neutral.png lenses are not matte dark (max_lum={max_lum:.1f})"
        assert max_sat < 35, f"chassis_neutral.png lenses are colored/lit (max_sat={max_sat})"

    def t1_f2_04():
        arr = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        # Structural headband zone (0..19, 0..9) should be opaque
        band = arr[0:7, 0:19, 3]
        assert int(band.min()) == 255, f"chassis_neutral.png structural strap has transparent pixels"

    def t1_f2_05():
        neutral = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64)).astype(np.int32)
        modular = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64)).astype(np.int32)
        lens_diffs = []
        for x1, y1, x2, y2 in GLOW_ALLOWED_RECTS[:4]:
            diff = np.abs(neutral[y1:y2, x1:x2, :3] - modular[y1:y2, x1:x2, :3]).mean()
            lens_diffs.append(float(diff))
        assert max(lens_diffs) > 30.0, f"chassis_neutral lenses too similar to tri_lens_modular ({lens_diffs})"

    cases.extend([
        ("T1_F2_01", 1, "F2", "chassis_neutral.png is 64x64 RGBA", t1_f2_01),
        ("T1_F2_02", 1, "F2", "No chassis_neutral_glow.png exists", t1_f2_02),
        ("T1_F2_03", 1, "F2", "chassis_neutral.png lens apertures are matte dark & desaturated", t1_f2_03),
        ("T1_F2_04", 1, "F2", "chassis_neutral.png structural strap pixels are fully opaque", t1_f2_04),
        ("T1_F2_05", 1, "F2", "chassis_neutral.png lens apertures clearly differ from tri_lens_modular", t1_f2_05),
    ])

    # ---------------- F3: Pro Modular Visual Differentiation (5) ----------------
    def t1_f3_01():
        base = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64))
        glow = load_png_rgba(TEX_DIR / "tri_lens_pro_modular_glow.png", (64, 64))
        assert int(base[:, :, 3].max()) == 255
        assert int(glow[:, :, 3].max()) > 0

    def t1_f3_02():
        glow = load_png_rgba(TEX_DIR / "tri_lens_pro_modular_glow.png", (64, 64))
        inside, bleed, sample = check_glow_mask_hygiene(glow, MODULAR_GLOW_ALLOWED_RECTS)
        assert inside > 0, "tri_lens_pro_modular_glow.png has no lit pixels in MODULAR_GLOW_ALLOWED_RECTS"
        assert bleed == 0, f"tri_lens_pro_modular_glow.png bleed outside allowed rects: {sample}"

    def t1_f3_03():
        mod_png = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64)).astype(np.float32)
        pro_png = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64)).astype(np.float32)
        mae = float(np.abs(mod_png[:, :, :3] - pro_png[:, :, :3]).mean())
        assert mae >= 4.0, f"tri_lens_pro_modular.png is visually too similar to tri_lens_modular.png (MAE={mae:.2f})"

    def t1_f3_04():
        geo = parse_geo_model(resolve_geo_path_via_model("pro_modular_goggles"))
        assert "mod_pro_armor" in geo.bones_by_name, "Master Modular geo missing 'mod_pro_armor' bone"
        assert geo.bones_by_name["mod_pro_armor"].parent == "head", (
            f"mod_pro_armor parent is '{geo.bones_by_name['mod_pro_armor'].parent}', expected 'head'"
        )
        assert len(geo.bones_by_name["mod_pro_armor"].cubes) >= 1, "mod_pro_armor has 0 cubes"

    def t1_f3_05():
        geo_path = resolve_geo_path_via_model("pro_modular_goggles")
        geo = parse_geo_model(geo_path)
        assert "mod_pro_armor" in geo.bones_by_name, "Missing mod_pro_armor"
        oob = _check_geo_uvs_in_bounds(geo_path)
        assert not oob, f"Pro modular geo OOB UVs: {oob[:5]}"
        overlaps = check_bone_steve_head_overlap(geo, lambda b: b in ("head", "mod_pro_armor"))
        assert not overlaps, f"mod_pro_armor overlaps Steve head: {overlaps}"

    cases.extend([
        ("T1_F3_01", 1, "F3", "tri_lens_pro_modular.png + _glow.png exist as 64x64 RGBA", t1_f3_01),
        ("T1_F3_02", 1, "F3", "tri_lens_pro_modular_glow.png obeys GLOW_ALLOWED_RECTS hygiene", t1_f3_02),
        ("T1_F3_03", 1, "F3", "tri_lens_pro_modular.png differs clearly from tri_lens_modular.png", t1_f3_03),
        ("T1_F3_04", 1, "F3", "Master geo contains mod_pro_armor parented to head with >=1 cube", t1_f3_04),
        ("T1_F3_05", 1, "F3", "mod_pro_armor has valid 64x64 UVs and zero Steve-head overlap", t1_f3_05),
    ])

    # ---------------- F4: Master Modular .geo.json & Hierarchy (6) ----------------
    def t1_f4_01():
        mod_geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        assert mod_geo.tex_w == 64 and mod_geo.tex_h == 64, (
            f"modular_goggles.geo.json is {mod_geo.tex_w}x{mod_geo.tex_h}, expected 64x64"
        )
        pro_path = GEO_DIR / "pro_modular_goggles.geo.json"
        if pro_path.is_file():
            pro_geo = parse_geo_model(pro_path)
            assert pro_geo.tex_w == 64 and pro_geo.tex_h == 64, (
                f"pro_modular_goggles.geo.json is {pro_geo.tex_w}x{pro_geo.tex_h}, expected 64x64"
            )

    def t1_f4_02():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        assert "head" in geo.bones_by_name
        missing = [b for b in HEAD_MOD_BONES if b not in geo.bones_by_name]
        assert not missing, f"modular_goggles.geo.json missing static head attachment bones: {missing}"
        wrong_parent = [
            f"{b}->{geo.bones_by_name[b].parent}"
            for b in HEAD_MOD_BONES
            if geo.bones_by_name[b].parent != "head"
        ]
        assert not wrong_parent, f"Static module bones must parent to 'head': {wrong_parent}"

    def t1_f4_03():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        assert "visor_pivot" in geo.bones_by_name, "modular_goggles.geo.json missing 'visor_pivot'"
        vp = geo.bones_by_name["visor_pivot"]
        assert vp.parent == "head" and tuple(vp.pivot) == (0.0, 27.5, -5.5), (
            f"visor_pivot parent={vp.parent}, pivot={vp.pivot}"
        )
        missing = [b for b in VISOR_MOD_BONES if b not in geo.bones_by_name]
        assert not missing, f"modular_goggles.geo.json missing flip-coupled visor bones: {missing}"
        wrong_parent = [
            f"{b}->{geo.bones_by_name[b].parent}"
            for b in VISOR_MOD_BONES
            if geo.bones_by_name[b].parent != "visor_pivot"
        ]
        assert not wrong_parent, f"Visor module bones must parent to 'visor_pivot': {wrong_parent}"

    def t1_f4_04():
        p = GEO_DIR / "modular_goggles.geo.json"
        geo = parse_geo_model(p)
        oob = _check_geo_uvs_in_bounds(p)
        assert not oob, f"modular_goggles.geo.json OOB UVs: {oob[:5]}"
        missing = [b for b in (*HEAD_MOD_BONES, *VISOR_MOD_BONES) if b not in geo.bones_by_name]
        assert not missing, f"Missing modular bones in modular_goggles.geo.json: {missing}"
        # Ensure every mod_* bone actually has geometry
        empty = [b for b in (*HEAD_MOD_BONES, *VISOR_MOD_BONES) if len(geo.bones_by_name[b].cubes) == 0]
        assert not empty, f"Modular bones with 0 cubes: {empty}"

    def t1_f4_05():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        overlaps = check_bone_steve_head_overlap(geo)
        assert not overlaps, f"modular_goggles.geo.json overlaps Steve head: {overlaps[:6]}"

    def t1_f4_06():
        base = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64))
        glow = load_png_rgba(TEX_DIR / "tri_lens_modular_glow.png", (64, 64))
        assert int(base[:, :, 3].max()) == 255
        inside, bleed, sample = check_glow_mask_hygiene(glow, MODULAR_GLOW_ALLOWED_RECTS)
        assert inside > 0 and bleed == 0, f"tri_lens_modular_glow hygiene failed: inside={inside}, bleed={sample}"

    cases.extend([
        ("T1_F4_01", 1, "F4", "modular_goggles.geo.json (+pro if present) is 64x64 valid Bedrock JSON", t1_f4_01),
        ("T1_F4_02", 1, "F4", "All 8 static mod_* attachment bones exist under head", t1_f4_02),
        ("T1_F4_03", 1, "F4", "All 6 flip-coupled mod_* bones exist under visor_pivot", t1_f4_03),
        ("T1_F4_04", 1, "F4", "Master Modular geo has no orphan/empty bones or OOB UVs", t1_f4_04),
        ("T1_F4_05", 1, "F4", "Master Modular geo has zero Steve-head overlap across all bones", t1_f4_05),
        ("T1_F4_06", 1, "F4", "tri_lens_modular.png + _glow.png exist and pass mask hygiene", t1_f4_06),
    ])

    # ---------------- F5: Unified VisionGogglesGeoModel & VisionGogglesGeoRenderer (5) ----------------
    def t1_f5_01():
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        clean, _ = get_combined_java_views([vgm_path])
        for bad in ("_thermal.geo.json", "_bio.geo.json", "_hydro.geo.json"):
            assert bad not in clean, f"VisionGogglesGeoModel still routes to legacy modular variant '{bad}'"
        assert "modular_goggles.geo.json" in clean or "geo/item/" in clean

    def t1_f5_02():
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        vgr_clean = strip_java_comments(read_text_safe(vgr_path))
        vgm_clean = strip_java_comments(read_text_safe(vgm_path))
        for legacy in ("nvg_texture", "thermal_vision", "hidro_vision", "bio_vision"):
            assert legacy not in vgr_clean, f"VisionGogglesGeoRenderer still references legacy texture '{legacy}'"
            assert legacy not in vgm_clean, f"VisionGogglesGeoModel still references legacy texture '{legacy}'"
        clean, _ = get_combined_java_views([vgr_path, vgm_path])
        assert "chassis_neutral" in clean, "VisionGogglesGeoRenderer/helpers missing promoted texture 'chassis_neutral'"
        for variant in ("nvg", "thermal", "bio", "hydro", "modular", "pro_modular"):
            expected = f"tri_lens_{variant}"
            assert expected in clean or ("tri_lens_" in clean and f'"{variant}"' in clean), (
                f"VisionGogglesGeoRenderer/helpers missing promoted texture '{expected}'"
            )

    def t1_f5_03():
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        clean, code = get_combined_java_views([vgr_path, vgm_path])
        assert "RenderType.eyes" in code, "VisionGogglesGeoRenderer missing RenderType.eyes glow pass"
        assert "_glow.png" in clean, "VisionGogglesGeoRenderer missing *_glow.png texture resolution"
        assert "vg_neutral_chassis" in clean, "VisionGogglesGeoRenderer missing vg_neutral_chassis guard"

    def t1_f5_04():
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        _, code = get_combined_java_views([vgr_path, vgm_path])
        has_y_center = bool(re.search(r"(?:-\s*29(?:\.0+)?[fFdD]?\s*/\s*16(?:\.0+)?[fFdD]?|-\s*1\.8125[fFdD]?)", code))
        has_z_center = bool(re.search(r"(?:2\.75[fFdD]?\s*/\s*16(?:\.0+)?[fFdD]?|0?\.171875[fFdD]?)", code))
        assert has_y_center and has_z_center, (
            "VisionGogglesGeoRenderer missing head-to-item centering (-29/16 or -1.8125f, +2.75/16 or 0.171875f)"
        )

    def t1_f5_05():
        for item in ALL_GOGGLE_ITEMS:
            model_json = read_json_safe(ITEM_MODEL_DIR / f"{item}.json", f"models/item/{item}.json")
            assert model_json.get("parent") == "builtin/entity", f"{item}.json parent != builtin/entity"
            disp = model_json.get("display", {})
            for key in ("gui", "ground", "fixed", "firstperson_righthand", "firstperson_lefthand", "thirdperson_righthand"):
                assert key in disp, f"models/item/{item}.json missing display.{key}"

    cases.extend([
        ("T1_F5_01", 1, "F5", "VisionGogglesGeoModel routes without legacy *_thermal/_bio/_hydro geos", t1_f5_01),
        ("T1_F5_02", 1, "F5", "VisionGogglesGeoRenderer uses tri_lens_* + chassis_neutral (no 128x128)", t1_f5_02),
        ("T1_F5_03", 1, "F5", "VisionGogglesGeoRenderer implements RenderType.eyes glow & neutral guard", t1_f5_03),
        ("T1_F5_04", 1, "F5", "VisionGogglesGeoRenderer applies (-29/16, +2.75/16) item-space centering", t1_f5_04),
        ("T1_F5_05", 1, "F5", "All 6 models/item/*_goggles.json define complete display transforms", t1_f5_05),
    ])

    # ---------------- F6: Remove /vg preview Command & Screen (5) ----------------
    def t1_f6_01():
        preview_dir = JAVA_COMMON / "client" / "preview"
        existing = [p.name for p in PREVIEW_DEDICATED_FILES if p.exists()]
        assert not existing, f"Dedicated preview files still exist: {existing}"
        assert not preview_dir.exists(), f"client/preview directory still exists at {preview_dir}"

    def t1_f6_02():
        for p in (JAVA_COMMON / "client" / "gui" / "TriLensPreviewScreen.java", JAVA_COMMON / "network" / "OpenProtoPreviewPacket.java"):
            assert not p.exists(), f"Preview file still exists: {p.name}"

    def t1_f6_03():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "command" / "CommandVisionGoggles.java"))
        assert '"preview"' not in txt, "CommandVisionGoggles.java still contains \"preview\" subcommand"
        assert "openProtoPreview" not in txt, "CommandVisionGoggles.java still contains openProtoPreview"
        assert "OpenProtoPreviewPacket" not in txt

    def t1_f6_04():
        nm = strip_java_comments(read_text_safe(JAVA_COMMON / "network" / "NetworkManager.java"))
        cgh = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "gui" / "ClientGuiHelper.java"))
        assert "OpenProtoPreviewPacket" not in nm, "NetworkManager still registers OpenProtoPreviewPacket"
        for sym in ("TriLensPreviewScreen", "openTriLensPreviewScreen", "handleProtoPreview"):
            assert sym not in cgh, f"ClientGuiHelper still contains '{sym}'"

    def t1_f6_05():
        hits = []
        for jf in iter_java_files():
            txt = strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))
            for sym in PREVIEW_SYMBOLS:
                if has_symbol_token(txt, sym):
                    hits.append(f"{jf.name}:{sym}")
        assert not hits, f"Remaining preview symbols in Java sources: {hits[:10]}"

    cases.extend([
        ("T1_F6_01", 1, "F6", "client/preview/ directory and classes are deleted", t1_f6_01),
        ("T1_F6_02", 1, "F6", "TriLensPreviewScreen & OpenProtoPreviewPacket are deleted", t1_f6_02),
        ("T1_F6_03", 1, "F6", "CommandVisionGoggles contains zero 'preview' subcommand or handler", t1_f6_03),
        ("T1_F6_04", 1, "F6", "NetworkManager & ClientGuiHelper stripped of preview hooks", t1_f6_04),
        ("T1_F6_05", 1, "F6", "Zero ProtoTriLens/Preview references across common, fabric, forge", t1_f6_05),
    ])

    # ---------------- F7: ModBench Neutral & Modular Preview Parity (5) ----------------
    def t1_f7_01():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        assert "vg_neutral_chassis" in txt, "ModificationStationRenderer missing vg_neutral_chassis tag"
        assert "ItemDisplayContext.FIXED" in txt, "ModificationStationRenderer missing FIXED upright display context"

    def t1_f7_02():
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        clean, code = get_combined_java_views([vgr_path, vgm_path])
        assert "vg_neutral_chassis" in clean and "chassis_neutral" in clean
        # Verify neutral check suppresses glow pass as well
        assert re.search(r"vg_neutral_chassis|neutralChassis|isNeutral", code, re.IGNORECASE), (
            "Neutral guard missing in renderer"
        )

    def t1_f7_03():
        # Check that bone visibility helper/renderer sets mod_optics_base visible & hides all other mod_* when vg_neutral_chassis=true
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        clean, code = get_combined_java_views([vgr_path, vgm_path])
        assert "setHidden" in code, "Dynamic bone visibility toggling via setHidden not implemented"
        assert "mod_optics_base" in clean and "vg_neutral_chassis" in clean, (
            "Neutral chassis mod_optics_base fallback not wired with setHidden"
        )

    def t1_f7_04():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        assert "activeModel" in txt and "result" in txt and "baseGoggles" in txt
        assert "renderUprightGoggles" in txt

    def t1_f7_05():
        # Fixed display transforms for ModBench cutting mat should exist & keep bounded scale
        for item in ALL_GOGGLE_ITEMS:
            mj = read_json_safe(ITEM_MODEL_DIR / f"{item}.json")
            fixed = mj.get("display", {}).get("fixed", {})
            sc = fixed.get("scale", [1.0, 1.0, 1.0])
            assert all(0.2 <= float(v) <= 2.0 for v in sc), f"{item} fixed scale out of range: {sc}"

    cases.extend([
        ("T1_F7_01", 1, "F7", "ModificationStationRenderer tags vg_neutral_chassis & renders FIXED", t1_f7_01),
        ("T1_F7_02", 1, "F7", "VisionGogglesGeoRenderer resolves chassis_neutral.png on vg_neutral_chassis", t1_f7_02),
        ("T1_F7_03", 1, "F7", "Neutral chassis hides optional mod_* bones and keeps mod_optics_base", t1_f7_03),
        ("T1_F7_04", 1, "F7", "ModificationStationRenderer forwards real NBT stacks for activeModel", t1_f7_04),
        ("T1_F7_05", 1, "F7", "All 6 item fixed display transforms stay upright & bounded", t1_f7_05),
    ])

    # ---------------- F8: Third-Person F5 Head Alignment & Dynamic Bones (5) ----------------
    def t1_f8_01():
        raw = read_text_safe(JAVA_COMMON / "client" / "GogglesRenderLayer.java")
        clean = strip_java_comments(raw)
        code = strip_java_comments_and_strings(raw)
        assert "VisionGoggleModel" not in code, "GogglesRenderLayer still uses legacy VisionGoggleModel"
        for legacy in ("nvg_texture", "thermal_vision", "hidro_vision", "bio_vision"):
            assert legacy not in clean, f"GogglesRenderLayer still references legacy '{legacy}'"

    def t1_f8_02():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        _, code = get_combined_java_views([grl_path])
        assert (
            "BakedGeoModel" in code
            or "GeckoLibCache" in code
            or "VisionGogglesGeoModel" in code
            or "VisionGogglesGeoRenderer" in code
        ), "GogglesRenderLayer does not render Tri-Lens BakedGeoModel"
        has_neg_scale = bool(
            re.search(
                r"(?:scale\s*\(\s*-1(?:\.0+)?[fF]?\s*,\s*-1(?:\.0+)?[fF]?\s*,\s*1(?:\.0+)?[fF]?\s*\)|ZP\.rotationDegrees\s*\(\s*180(?:\.0+)?[fF]?\s*\))",
                code,
            )
        )
        has_head_offset = bool(re.search(r"(?:-\s*24(?:\.0+)?[fF]?\s*/\s*16(?:\.0+)?[fF]?|-\s*1\.5[fF]?)", code))
        assert has_neg_scale and has_head_offset, (
            "GogglesRenderLayer missing +1-determinant head alignment scale(-1,-1,1) + translate(0,-24/16,0)"
        )

    def t1_f8_03():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        clean, code = get_combined_java_views([grl_path])
        assert "GogglesFlipAnimator" in code
        assert '"visor_pivot"' in clean, "GogglesRenderLayer does not drive 'visor_pivot' bone"

    def t1_f8_04():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        grl_clean = strip_java_comments(read_text_safe(grl_path))
        clean, code = get_combined_java_views([grl_path])
        for legacy_glow in ("nvg_glow.png", "thermal_glow.png", "hidro_glow.png", "bio_glow.png"):
            assert not has_symbol_token(grl_clean, legacy_glow), (
                f"GogglesRenderLayer still references legacy glow '{legacy_glow}'"
            )
        assert "RenderType.eyes" in code and (
            "tri_lens_" in clean
            or "VisionGogglesGeoRenderer" in code
            or "getGlow" in code
            or "resolveGlow" in code
        ), "GogglesRenderLayer missing Tri-Lens RenderType.eyes glow pass"
        assert "0.98" in code or "isActive" in code

    def t1_f8_05():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        _, code = get_combined_java_views([grl_path])
        assert ("ModularGogglesItem" in code or "applyModular" in code or "setHidden" in code), (
            "GogglesRenderLayer missing dynamic modular bone visibility handling"
        )
        assert "setHidden" in code or "BoneVisibility" in code or "applyModular" in code

    cases.extend([
        ("T1_F8_01", 1, "F8", "GogglesRenderLayer retires VisionGoggleModel & legacy 128x128 textures", t1_f8_01),
        ("T1_F8_02", 1, "F8", "GogglesRenderLayer uses BakedGeoModel with +1 det head transform", t1_f8_02),
        ("T1_F8_03", 1, "F8", "GogglesRenderLayer animates visor_pivot via GogglesFlipAnimator", t1_f8_03),
        ("T1_F8_04", 1, "F8", "GogglesRenderLayer gates Tri-Lens RenderType.eyes glow on active+down", t1_f8_04),
        ("T1_F8_05", 1, "F8", "GogglesRenderLayer toggles dynamic modular bones from stack NBT", t1_f8_05),
    ])

    # ---------------- F9: First-Person flip Orientation, Scale, Trajectory & Arm Sync (6) ----------------
    def t1_f9_01():
        raw = read_text_safe(JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java")
        clean = strip_java_comments(raw)
        code = strip_java_comments_and_strings(raw)
        assert "FirstPersonVisorModel" not in code, "FirstPersonVisorRenderer still uses legacy FirstPersonVisorModel"
        for legacy in ("nvg_texture", "thermal_vision", "hidro_vision", "bio_vision"):
            assert legacy not in clean, f"FirstPersonVisorRenderer still references legacy '{legacy}'"

    def t1_f9_02():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, code = get_combined_java_views([fpv_path])
        assert (
            "BakedGeoModel" in code
            or "GeckoLibCache" in code
            or "VisionGogglesGeoModel" in code
            or "VisionGogglesGeoRenderer" in code
        ), "FirstPersonVisorRenderer missing BakedGeoModel rendering"
        assert '"visor_pivot"' in clean, "FirstPersonVisorRenderer must extract/render 'visor_pivot' bone"
        assert ("ModularGogglesItem" in code or "setHidden" in code or "applyModular" in code or "BoneVisibility" in code), (
            "FirstPersonVisorRenderer missing modular bone visibility for visor_pivot children"
        )

    def t1_f9_03():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, _ = get_combined_java_views([fpv_path])
        params = parse_fp_visor_params(clean)
        assert params["cancels_y_flip"], "FirstPersonVisorRenderer missing GuiGraphics Y-flip cancellation scale(1,-1,1)"

    def t1_f9_04():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, _ = get_combined_java_views([fpv_path])
        params = parse_fp_visor_params(clean)
        assert params["has_180_yaw"], (
            "FirstPersonVisorRenderer does not rotate ~180 deg Y to present front objective lenses instead of +Z rear wall"
        )

    def t1_f9_05():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, code = get_combined_java_views([fpv_path])
        assert "4.85" not in code, "FirstPersonVisorRenderer still retains 4.85f viewport-blowup scale"
        params = parse_fp_visor_params(clean)
        assert params["scale_parsed"] and params["max_scale_mult"] is not None, (
            "FirstPersonVisorRenderer visor scale expression could not be determined"
        )
        assert params["max_scale_mult"] <= 2.8, f"FirstPersonVisorRenderer max scale mult too large: {params['max_scale_mult']}"

    def t1_f9_06():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, code = get_combined_java_views([fpv_path])
        assert "renderPlayerArm" in code and "gripScale" in code
        params = parse_fp_visor_params(clean)
        assert params["scale_parsed"] and params["max_scale_mult"] is not None and params["grip_max_mult"] is not None, (
            "FirstPersonVisorRenderer missing parseable gripScale / visor scale"
        )
        ratio = max(params["grip_max_mult"], 0.01) / max(params["max_scale_mult"], 0.01)
        assert 0.5 <= ratio <= 2.0, f"gripScale/visorScale mismatch ratio={ratio:.2f}"

    cases.extend([
        ("T1_F9_01", 1, "F9", "FirstPersonVisorRenderer retires FirstPersonVisorModel & legacy textures", t1_f9_01),
        ("T1_F9_02", 1, "F9", "FirstPersonVisorRenderer renders BakedGeoModel visor_pivot + modular children", t1_f9_02),
        ("T1_F9_03", 1, "F9", "FirstPersonVisorRenderer cancels GuiGraphics Y-down flip via scale(1,-1,1)", t1_f9_03),
        ("T1_F9_04", 1, "F9", "FirstPersonVisorRenderer orients front lenses (~180 deg Y) toward camera", t1_f9_04),
        ("T1_F9_05", 1, "F9", "FirstPersonVisorRenderer removes 4.85f blowup scale", t1_f9_05),
        ("T1_F9_06", 1, "F9", "FirstPersonVisorRenderer arm gripScale stays proportional to visor scale", t1_f9_06),
    ])

    # ---------------- F10: First-Person equip Animation & Dual-Hand Sync (5) ----------------
    def t1_f10_01():
        mi_txt = read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinItemInHandRenderer.java")
        gea_p = JAVA_COMMON / "client" / "GogglesEquipAnimator.java"
        gea_txt = gea_p.read_text(encoding="utf-8") if gea_p.is_file() else None
        ok, msg = has_main_hand_sync_guard(mi_txt, gea_txt)
        assert ok, msg

    def t1_f10_02():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "FirstPersonEquipRenderer.java"))
        assert "FIRST_PERSON_LEFT_HAND" in txt and "FIRST_PERSON_RIGHT_HAND" in txt
        assert "itemRenderCallback.render" in txt

    def t1_f10_03():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "GogglesEquipAnimator.java"))
        for st in ("IDLE", "EQUIPPING", "CANCELING", "COMPLETING"):
            assert st in txt, f"GogglesEquipAnimator missing state {st}"

    def t1_f10_04():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinHumanoidModel.java"))
        assert "isUsingItem" in txt and "VisionGogglesItem" in txt, (
            "MixinHumanoidModel missing isUsingItem/VisionGogglesItem brow-reach equip pose"
        )
        assert "ProtoTriLensPreviewState" not in txt, (
            "MixinHumanoidModel still references ProtoTriLensPreviewState"
        )

    def t1_f10_05():
        txt = strip_java_comments(read_text_safe(JAVA_COMMON / "item" / "VisionGogglesItem.java"))
        assert "onLocalEquipCompleted" in txt and "EquipPacket" in txt

    cases.extend([
        ("T1_F10_01", 1, "F10", "MixinItemInHandRenderer guards syncFromRender to MAIN_HAND once/frame", t1_f10_01),
        ("T1_F10_02", 1, "F10", "FirstPersonEquipRenderer delegates both hands via ItemRenderCallback", t1_f10_02),
        ("T1_F10_03", 1, "F10", "GogglesEquipAnimator maintains IDLE/EQUIPPING/CANCELING/COMPLETING", t1_f10_03),
        ("T1_F10_04", 1, "F10", "MixinHumanoidModel keeps 3rd-person equip pose without preview hook", t1_f10_04),
        ("T1_F10_05", 1, "F10", "VisionGogglesItem finishUsingItem invokes onLocalEquipCompleted & EquipPacket", t1_f10_05),
    ])

    # ---------------- F11: Legacy Asset/Class Retirement (6) ----------------
    def t1_f11_01():
        leftover = [p.name for p in LEGACY_MODEL_FILES if p.exists()]
        assert not leftover, f"Legacy Java model classes still exist: {leftover}"

    def t1_f11_02():
        mc = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "ModClient.java"))
        fg = strip_java_comments(read_text_safe(JAVA_FORGE / "forge" / "Vision_gogglesForge.java"))
        assert "VisionGoggleModel" not in mc, "ModClient still registers VisionGoggleModel.LAYER_LOCATION"
        assert "VisionGoggleModel" not in fg, "Vision_gogglesForge still registers VisionGoggleModel.LAYER_LOCATION"

    def t1_f11_03():
        leftover = [name for name in LEGACY_128_TEXTURES if (TEX_DIR / name).exists()]
        assert not leftover, f"Legacy 128x128 entity textures still present: {leftover}"

    def t1_f11_04():
        leftover = [name for name in LEGACY_VARIANT_GEOS if (GEO_DIR / name).exists()]
        assert not leftover, f"Legacy duplicate variant geos still present: {leftover}"

    def t1_f11_05():
        # Legacy gpnvg.geo.json / proto_tri_lens_* production duplicates should be retired
        leftover = []
        for pat in ("gpnvg.geo.json", "proto_tri_lens_nvg.geo.json", "proto_tri_lens_thermal.geo.json",
                    "proto_tri_lens_bio.geo.json", "proto_tri_lens_hydro.geo.json", "proto_tri_lens_modular.geo.json"):
            if (GEO_DIR / pat).exists():
                leftover.append(pat)
        assert not leftover, f"Unretired legacy/prototype geo files in resources: {leftover}"

    def t1_f11_06():
        retired_names = list(LEGACY_128_TEXTURES) + list(LEGACY_VARIANT_GEOS) + [
            "VisionGoggleModel", "FirstPersonVisorModel", "proto_tri_lens.animation.json",
        ]
        hits = []
        for jf in iter_java_files():
            txt = strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))
            for sym in retired_names:
                if has_symbol_token(txt, sym):
                    hits.append(f"{jf.name}:{sym}")
        assert not hits, f"Java references to retired legacy assets/classes remain: {hits[:10]}"

    cases.extend([
        ("T1_F11_01", 1, "F11", "VisionGoggleModel.java & FirstPersonVisorModel.java deleted", t1_f11_01),
        ("T1_F11_02", 1, "F11", "ModClient & Vision_gogglesForge remove exo_helmet layer registration", t1_f11_02),
        ("T1_F11_03", 1, "F11", "All 8 legacy 128x128 goggle textures deleted", t1_f11_03),
        ("T1_F11_04", 1, "F11", "All 6 legacy duplicate variant geos (*_bio/_hydro/_thermal) deleted", t1_f11_04),
        ("T1_F11_05", 1, "F11", "Retired gpnvg & proto_tri_lens_* production duplicates removed", t1_f11_05),
        ("T1_F11_06", 1, "F11", "Zero Java references to deleted legacy textures/geos/models", t1_f11_06),
    ])

    return [run_test_case(tid, tier, feat, desc, fn) for tid, tier, feat, desc, fn in cases]
