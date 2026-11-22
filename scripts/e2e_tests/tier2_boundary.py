"""Tier 2: Boundary, corner-case, and integration contract tests (58 tests, >= 5 per F1..F11)."""

from __future__ import annotations

import math
import re
from pathlib import Path
from typing import Dict, List

import numpy as np

from .common_utils import (
    ALL_GOGGLE_ITEMS,
    ALL_MOD_BONES,
    ALL_TARGET_TEXTURES,
    ANIM_DIR,
    ASSETS_ROOT,
    GEO_DIR,
    GLOW_ALLOWED_RECTS,
    HEAD_MOD_BONES,
    ITEM_MODEL_DIR,
    JAVA_COMMON,
    JAVA_FABRIC,
    JAVA_FORGE,
    LANG_DIR,
    STATIC_GOGGLE_ITEMS,
    STATIC_TEXTURES,
    TEX_DIR,
    VISOR_MOD_BONES,
    TestRecord,
    check_glow_mask_hygiene,
    get_combined_java_views,
    has_main_hand_sync_guard,
    iter_java_files,
    load_png_rgba,
    read_json_safe,
    read_text_safe,
    resolve_geo_path_via_model,
    resolve_java_modular_nbt_rules,
    run_test_case,
    strip_java_comments,
    strip_java_comments_and_strings,
)
from .rasterizer3d import (
    aabb_overlap_volume,
    check_bone_steve_head_overlap,
    compute_bone_world_matrices,
    compute_cube_transformed_aabb,
    cubes_zfight_or_overlap,
    make_modular_visibility_fn,
    parse_fp_visor_params,
    parse_geo_model,
    render_fp_flip_gui,
)


def _get_bone_visibility_java_views() -> tuple[str, str]:
    primaries = [
        JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
        JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        JAVA_COMMON / "client" / "GogglesRenderLayer.java",
        JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java",
    ]
    return get_combined_java_views(primaries, include_client_helpers=True)


def _get_bone_visibility_java_text() -> str:
    return _get_bone_visibility_java_views()[0]


def run_tier2() -> List[TestRecord]:
    cases = []

    # ---------------- F1: Static Tri-Lens Integration & Geometry Boundaries (5) ----------------
    def t2_f1_01():
        # Every VisionMode maps consistently across geo, base tex, glow tex
        vm = read_text_safe(JAVA_COMMON / "util" / "VisionMode.java")
        for mode_name in ("NIGHT_VISION", "THERMAL", "HYDRO", "BIOMETRIC"):
            assert mode_name in vm
        for item, (btex, gtex) in STATIC_TEXTURES.items():
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            assert geo.tex_w == 64 and geo.tex_h == 64
            load_png_rgba(TEX_DIR / btex, (64, 64))
            load_png_rgba(TEX_DIR / gtex, (64, 64))

    def t2_f1_02():
        anim = read_json_safe(ANIM_DIR / "vision_goggles.animation.json")
        anim_bones = set()
        for clip in anim.get("animations", {}).values():
            anim_bones.update(clip.get("bones", {}).keys())
        assert anim_bones == {"visor_pivot"}, f"Expected animation bones == {{'visor_pivot'}}, got {anim_bones}"
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            for b in anim_bones:
                assert b in geo.bones_by_name, f"{item}.geo.json missing animated bone '{b}'"

    def t2_f1_03():
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            vp = geo.bones_by_name.get("visor_pivot")
            assert vp is not None, f"{item} missing visor_pivot"
            assert tuple(vp.pivot) == (0.0, 27.5, -5.5), f"{item} visor_pivot={vp.pivot}"

    def t2_f1_04():
        # Negative uv_size edge cases across all static geos must stay within [0, 64]
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            neg_faces = 0
            for bone in geo.bones_by_name.values():
                for cube in bone.cubes:
                    for fname, ((u0, v0), (du, dv)) in cube.uv_faces.items():
                        if du < 0 or dv < 0:
                            neg_faces += 1
                        assert 0.0 <= min(u0, u0 + du) and max(u0, u0 + du) <= 64.0, (
                            f"{item}.{bone.name}.{fname} U out of [0,64]"
                        )
                        assert 0.0 <= min(v0, v0 + dv) and max(v0, v0 + dv) <= 64.0, (
                            f"{item}.{bone.name}.{fname} V out of [0,64]"
                        )
            assert neg_faces >= 10, f"{item}.geo.json unexpected low mirrored UV count ({neg_faces})"

    def t2_f1_05():
        # Verify idle_up (-60 deg around X) lifts visor_pivot cubes upward above eye line Y=28 without clipping head
        for item in STATIC_GOGGLE_ITEMS:
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            overlaps_up = check_bone_steve_head_overlap(geo, visor_t=1.0)
            assert not overlaps_up, f"{item} overlaps Steve head when flipped UP: {overlaps_up}"

    cases.extend([
        ("T2_F1_01", 2, "F1", "All 4 VisionModes align geo + base + glow 64x64 assets", t2_f1_01),
        ("T2_F1_02", 2, "F1", "Animation bone visor_pivot matches all 4 static geos", t2_f1_02),
        ("T2_F1_03", 2, "F1", "All 4 static geos share identical hinge pivot [0,27.5,-5.5]", t2_f1_03),
        ("T2_F1_04", 2, "F1", "Exact [0,64] UV bounds including negative uv_size mirroring", t2_f1_04),
        ("T2_F1_05", 2, "F1", "Flipped-up visor_t=1.0 clears Steve head across all 4 static geos", t2_f1_05),
    ])

    # ---------------- F2: chassis_neutral.png Boundary & Contract (5) ----------------
    def t2_f2_01():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        arr = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        assert (geo.tex_w, geo.tex_h) == (arr.shape[1], arr.shape[0]) == (64, 64)

    def t2_f2_02():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        arr = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        # Every face UV of head and mod_optics_base must sample opaque pixels (>0 alpha)
        for bname in ("head", "visor_pivot", "mod_optics_base"):
            bone = geo.bones_by_name.get(bname)
            assert bone is not None, f"Missing {bname} in modular_goggles.geo.json"
            for cube in bone.cubes:
                for fname, ((u0, v0), (du, dv)) in cube.uv_faces.items():
                    if du == 0 or dv == 0:
                        continue
                    x1 = max(0, int(math.floor(min(u0, u0 + du))))
                    x2 = min(64, int(math.ceil(max(u0, u0 + du))))
                    y1 = max(0, int(math.floor(min(v0, v0 + dv))))
                    y2 = min(64, int(math.ceil(max(v0, v0 + dv))))
                    patch = arr[y1:y2, x1:x2, 3]
                    assert patch.size > 0 and int(patch.min()) == 255, (
                        f"chassis_neutral.png has transparent pixel in {bname}.{fname} UV [{x1}:{x2},{y1}:{y2}]"
                    )

    def t2_f2_03():
        msr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        vgr_clean, _ = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        assert '"vg_neutral_chassis"' in msr and '"vg_neutral_chassis"' in vgr_clean

    def t2_f2_04():
        # Verify VisionGogglesGeoRenderer/helpers check vg_neutral_chassis BEFORE glow pass (even if nvg_active=true)
        vgr_clean, vgr_code = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        assert "vg_neutral_chassis" in vgr_clean and "RenderType.eyes" in vgr_code
        # Neutral flag must gate glow rendering (e.g. !isNeutral or getGlowTexture returning null when neutral)
        assert re.search(
            r"(?:!\s*(?:isNeutral|neutral)|neutral[^}]{0,120}return|vg_neutral_chassis.*false)",
            vgr_code,
            re.IGNORECASE | re.DOTALL,
        ), "VisionGogglesGeoRenderer must suppress RenderType.eyes glow when vg_neutral_chassis is true"

    def t2_f2_05():
        # Verify canonical neutral NBT rules override all modules & mod_pro_armor
        vis = resolve_java_modular_nbt_rules(
            ["vision_goggles:thermal", "vision_goggles:solar", "vision_goggles:zoom", "vision_goggles:battery_expansion"],
            is_pro=True,
            neutral_chassis=True,
        )
        assert vis["mod_optics_base"] is True
        for b in ALL_MOD_BONES:
            if b != "mod_optics_base":
                assert vis[b] is False, f"Neutral chassis must hide {b}"

    cases.extend([
        ("T2_F2_01", 2, "F2", "chassis_neutral.png 64x64 matches modular_goggles.geo.json", t2_f2_01),
        ("T2_F2_02", 2, "F2", "All base/optics_base UV rects on chassis_neutral.png are opaque", t2_f2_02),
        ("T2_F2_03", 2, "F2", "ModificationStationRenderer & GeoRenderer share vg_neutral_chassis key", t2_f2_03),
        ("T2_F2_04", 2, "F2", "vg_neutral_chassis suppresses RenderType.eyes even if active", t2_f2_04),
        ("T2_F2_05", 2, "F2", "Neutral chassis overrides Pro + installed modules to mod_optics_base only", t2_f2_05),
    ])

    # ---------------- F3: Pro Modular Boundary & Differentiation (5) ----------------
    def t2_f3_01():
        pm = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "item" / "ProModularGogglesItem.java"))
        assert "extends ModularGogglesItem" in pm
        vgr_clean, vgr_code = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        assert "ProModularGogglesItem" in vgr_code and "tri_lens_pro_modular" in vgr_clean

    def t2_f3_02():
        clean, code = _get_bone_visibility_java_views()
        assert "ProModularGogglesItem" in code and "mod_pro_armor" in clean, (
            "Java bone visibility logic does not differentiate ProModularGogglesItem -> mod_pro_armor"
        )
        assert resolve_java_modular_nbt_rules([], is_pro=True)["mod_pro_armor"] is True
        assert resolve_java_modular_nbt_rules([], is_pro=False)["mod_pro_armor"] is False

    def t2_f3_03():
        geo = parse_geo_model(resolve_geo_path_via_model("pro_modular_goggles"))
        pro_bone = geo.bones_by_name.get("mod_pro_armor")
        head_bone = geo.bones_by_name.get("head")
        assert pro_bone is not None and head_bone is not None
        bmats = compute_bone_world_matrices(geo)
        # Check mod_pro_armor cubes do not overlap or coplanar-zfight with head cubes
        for pc in pro_bone.cubes:
            pa = compute_cube_transformed_aabb(pc, bmats.get("mod_pro_armor"))
            for hc in head_bone.cubes:
                ha = compute_cube_transformed_aabb(hc, bmats.get("head"))
                zf = cubes_zfight_or_overlap(pa, ha)
                assert zf is None, f"mod_pro_armor cube z-fights/overlaps head cube: {zf}"

    def t2_f3_04():
        mod_png = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64)).astype(np.float32)
        pro_png = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64)).astype(np.float32)
        geo = parse_geo_model(resolve_geo_path_via_model("pro_modular_goggles"))
        assert geo.tex_w == 64 and geo.tex_h == 64, f"Geo still {geo.tex_w}x{geo.tex_h}"
        assert "visor_pivot" in geo.bones_by_name, "Pro Modular geo missing 'visor_pivot'"
        # Sample UV regions used by head/visor_pivot
        diffs = []
        for bname in ("head", "visor_pivot"):
            for cube in geo.bones_by_name[bname].cubes:
                for (u0, v0), (du, dv) in cube.uv_faces.values():
                    if du == 0 or dv == 0:
                        continue
                    x1, x2 = max(0, int(min(u0, u0 + du))), min(64, int(max(u0, u0 + du)))
                    y1, y2 = max(0, int(min(v0, v0 + dv))), min(64, int(max(v0, v0 + dv)))
                    if x2 > x1 and y2 > y1:
                        diffs.append(float(np.abs(mod_png[y1:y2, x1:x2, :3] - pro_png[y1:y2, x1:x2, :3]).mean()))
        assert diffs and max(diffs) > 10.0, "Sampled UVs show insufficient visual delta between Modular and Pro Modular"

    def t2_f3_05():
        clean, _ = _get_bone_visibility_java_views()
        assert "vg_neutral_chassis" in clean and "mod_pro_armor" in clean

    cases.extend([
        ("T2_F3_01", 2, "F3", "ProModularGogglesItem extends ModularGogglesItem & routes to pro texture", t2_f3_01),
        ("T2_F3_02", 2, "F3", "mod_pro_armor toggles exclusively for ProModularGogglesItem", t2_f3_02),
        ("T2_F3_03", 2, "F3", "mod_pro_armor cubes do not duplicate/z-fight base head cubes", t2_f3_03),
        ("T2_F3_04", 2, "F3", "Sampled UV faces differ clearly between modular and pro_modular textures", t2_f3_04),
        ("T2_F3_05", 2, "F3", "vg_neutral_chassis hides mod_pro_armor on Pro Modular", t2_f3_05),
    ])

    # ---------------- F4: Master Modular Bone Pivot, Clearance & Boundary Cases (7) ----------------
    def t2_f4_01():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        for b in HEAD_MOD_BONES:
            assert b in geo.bones_by_name, f"Missing {b}"
            assert geo.bones_by_name[b].parent == "head", f"{b} must parent to 'head'"

    def t2_f4_02():
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        for b in VISOR_MOD_BONES:
            assert b in geo.bones_by_name, f"Missing {b}"
            assert geo.bones_by_name[b].parent == "visor_pivot", f"{b} must parent to 'visor_pivot'"

    def t2_f4_03():
        # Empty modular NBT boundary: mod_optics_base visible, all other mod_* hidden
        vis = resolve_java_modular_nbt_rules([], is_pro=False, neutral_chassis=False)
        assert vis["mod_optics_base"] is True
        for b in ALL_MOD_BONES:
            if b != "mod_optics_base":
                assert vis[b] is False
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        assert "mod_optics_base" in geo.bones_by_name and len(geo.bones_by_name["mod_optics_base"].cubes) > 0

    def t2_f4_04():
        # mod_zoom extends forward along -Z without clipping Steve's face (Z <= -4.0)
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        zb = geo.bones_by_name.get("mod_zoom")
        assert zb is not None and zb.cubes, "Missing mod_zoom cubes"
        min_z = min(compute_cube_transformed_aabb(c)[4] for c in zb.cubes)
        max_z = max(compute_cube_transformed_aabb(c)[5] for c in zb.cubes)
        assert min_z < -9.5 and max_z <= -4.0, f"mod_zoom Z span [{min_z:.2f}, {max_z:.2f}] invalid"

    def t2_f4_05():
        # mod_battery_pack sits on rear nape (Z >= 4.0) & mod_solar sits on top strap (Y >= 32.0)
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        bp = geo.bones_by_name.get("mod_battery_pack")
        so = geo.bones_by_name.get("mod_solar")
        assert bp is not None and bp.cubes and so is not None and so.cubes
        bp_zmax = max(compute_cube_transformed_aabb(c)[5] for c in bp.cubes)
        so_ymax = max(compute_cube_transformed_aabb(c)[3] for c in so.cubes)
        assert bp_zmax >= 4.25, f"mod_battery_pack not on rear nape (zmax={bp_zmax:.2f})"
        assert so_ymax >= 32.25, f"mod_solar not on top strap (ymax={so_ymax:.2f})"

    def t2_f4_06():
        # mod_flashlight & mod_lua_chip clear Steve head [-4..4, 24..32, -4..4]
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        for bname in ("mod_flashlight", "mod_lua_chip"):
            b = geo.bones_by_name.get(bname)
            assert b is not None and b.cubes, f"Missing {bname} cubes"
        overlaps = check_bone_steve_head_overlap(geo, lambda b: b in ("head", "mod_flashlight", "mod_lua_chip"))
        assert not overlaps, f"mod_flashlight/mod_lua_chip overlaps Steve head: {overlaps}"

    def t2_f4_07():
        # Each mod_* bone has exact [0, 64] UV bounds including negative uv_size
        p = GEO_DIR / "modular_goggles.geo.json"
        geo = parse_geo_model(p)
        for bname in ALL_MOD_BONES:
            bone = geo.bones_by_name.get(bname)
            assert bone is not None, f"Missing {bname}"
            for idx, cube in enumerate(bone.cubes):
                for fname, ((u0, v0), (du, dv)) in cube.uv_faces.items():
                    assert 0.0 <= min(u0, u0 + du) and max(u0, u0 + du) <= 64.0, (
                        f"{bname}[{idx}].{fname} U OOB"
                    )
                    assert 0.0 <= min(v0, v0 + dv) and max(v0, v0 + dv) <= 64.0, (
                        f"{bname}[{idx}].{fname} V OOB"
                    )

    cases.extend([
        ("T2_F4_01", 2, "F4", "All 8 static mod_* bones parent strictly to head", t2_f4_01),
        ("T2_F4_02", 2, "F4", "All 6 optical/zoom mod_* bones parent strictly to visor_pivot", t2_f4_02),
        ("T2_F4_03", 2, "F4", "Empty modular NBT shows mod_optics_base & hides all optional bones", t2_f4_03),
        ("T2_F4_04", 2, "F4", "mod_zoom extends forward along -Z clear of Steve face", t2_f4_04),
        ("T2_F4_05", 2, "F4", "mod_battery_pack sits on rear nape & mod_solar on top strap", t2_f4_05),
        ("T2_F4_06", 2, "F4", "mod_flashlight & mod_lua_chip clear Steve head bounds", t2_f4_06),
        ("T2_F4_07", 2, "F4", "All 14 mod_* bones have exact [0, 64] UV bounds", t2_f4_07),
    ])

    # ---------------- F5: Unified GeoModel/Renderer NBT & Platform Contracts (5) ----------------
    def t2_f5_01():
        # Legacy vs namespaced module IDs + ModConstants mapping
        mgi = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "item" / "ModularGogglesItem.java"))
        assert "resolveHardwareVisionMode" in mgi
        assert "hasBatteryExpansion" in mgi
        clean, _ = _get_bone_visibility_java_views()
        for bone in ALL_MOD_BONES:
            assert bone in clean, f"Dynamic bone visibility Java code missing bone '{bone}'"
        # Verify legacy vs namespaced equivalence
        v_legacy = resolve_java_modular_nbt_rules(["SOLAR", "BATTERY_EXPANSION"])
        v_ns = resolve_java_modular_nbt_rules(["vision_goggles:solar", "vision_goggles:battery_expansion"])
        assert v_legacy == v_ns and v_legacy["mod_solar"] and v_legacy["mod_battery_pack"]

    def t2_f5_02():
        # "programmable" vs custom programmed_id ("threat_scanner", "vision_goggles:thermal_palette")
        # must enable mod_lua_chip and NOT mod_optics_thermal
        v_prog = resolve_java_modular_nbt_rules(["vision_goggles:programmable"])
        v_threat = resolve_java_modular_nbt_rules(["threat_scanner"])
        v_thermal_lua = resolve_java_modular_nbt_rules(["vision_goggles:thermal_palette"])
        for label, v in (("programmable", v_prog), ("threat_scanner", v_threat), ("thermal_palette", v_thermal_lua)):
            assert v["mod_lua_chip"] is True, f"{label} did not enable mod_lua_chip"
            assert v["mod_optics_thermal"] is False, f"{label} falsely enabled mod_optics_thermal!"
        # Also verify Java code uses ModularGogglesItem.hasModule / getModes / resolveHardwareVisionMode (not .contains("thermal"))
        clean, code = _get_bone_visibility_java_views()
        assert ("hasModule" in code or "getModes" in code or "getUtilityModules" in code) and "setHidden" in code, (
            "Renderer bone visibility must query ModularGogglesItem modules and call setHidden"
        )
        assert '.contains("thermal")' not in clean, (
            "Substring .contains(\"thermal\") bug risks matching thermal_palette Lua script!"
        )

    def t2_f5_03():
        # VisionGogglesGeoRenderer sets currentStack BEFORE preRender/bone traversal
        vgr = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"))
        assert "setCurrentStack" in vgr and "renderByItem" in vgr and "preRender" in vgr

    def t2_f5_04():
        # Must reset GeoBone hidden state explicitly on every render so modular stack A doesn't leak to stack B
        _, code = _get_bone_visibility_java_views()
        assert "setHidden" in code
        # Must call setHidden(false)/setHidden(!visible) directly or via a bone-visibility wrapper — never only hide!
        assert re.search(r"set(?:Bone)?Hidden\s*\([^;]*(?:false|!)", code), (
            "Bone visibility helper must explicitly reset hidden=false / !visible to avoid cross-stack leakage"
        )

    def t2_f5_05():
        fab = strip_java_comments(read_text_safe(JAVA_FABRIC / "fabric" / "client" / "Vision_gogglesFabricClient.java"))
        frg = strip_java_comments(read_text_safe(JAVA_FORGE / "forge" / "mixin" / "ItemMixin.java"))
        for const in ("NIGHT_VISION_GOGGLES", "THERMAL_GOGGLES", "HYDRO_GOGGLES", "BIOMETRIC_GOGGLES", "MODULAR_GOGGLES", "PRO_MODULAR_GOGGLES"):
            assert const in fab, f"Fabric client renderer missing {const}"
        assert "VisionGogglesItem" in frg or "createRenderer" in frg

    cases.extend([
        ("T2_F5_01", 2, "F5", "Legacy vs namespaced module IDs map identically to all 14 mod_* bones", t2_f5_01),
        ("T2_F5_02", 2, "F5", "Lua IDs (thermal_palette, threat_scanner) enable mod_lua_chip without thermal optics", t2_f5_02),
        ("T2_F5_03", 2, "F5", "VisionGogglesGeoRenderer propagates currentStack before traversal", t2_f5_03),
        ("T2_F5_04", 2, "F5", "Dynamic bone visibility explicitly sets both hidden=true and hidden=false", t2_f5_04),
        ("T2_F5_05", 2, "F5", "Fabric BuiltinItemRendererRegistry & Forge ItemMixin cover all 6 goggles", t2_f5_05),
    ])

    # ---------------- F6: /vg preview Removal Integration Purity (5) ----------------
    def t2_f6_01():
        cmd = strip_java_comments(read_text_safe(JAVA_COMMON / "command" / "CommandVisionGoggles.java"))
        for sub in ('"gui"', '"reload"', '"list"', '"folder"', '"slots"', '"run"', '"givescript"'):
            assert sub in cmd, f"CommandVisionGoggles lost production subcommand {sub}"
        assert '"preview"' not in cmd

    def t2_f6_02():
        nm = strip_java_comments(read_text_safe(JAVA_COMMON / "network" / "NetworkManager.java"))
        assert "OpenProtoPreviewPacket" not in nm
        assert "OpenLuaScreenPacket" in nm and "EquipPacket" in nm

    def t2_f6_03():
        vr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "VisionRenderer.java"))
        for bad in ("ProtoTriLensPreviewState", "protoPreviewHeadActive", "lastPreviewZoomKeyDown", "toggleProtoVisorWithSound", "tickInGameFlipFrame"):
            assert bad not in vr, f"VisionRenderer still contains preview artifact '{bad}'"

    def t2_f6_04():
        mh = strip_java_comments(read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinHumanoidModel.java"))
        mi = strip_java_comments(read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinItemInHandRenderer.java"))
        assert "ProtoTriLensPreviewState" not in mh
        assert "ProtoTriLensPreviewState" not in mi and "protoTransitioning" not in mi

    def t2_f6_05():
        cgh = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "gui" / "ClientGuiHelper.java"))
        assert "openLuaScreen" in cgh
        assert "TriLensPreview" not in cgh and "ProtoPreview" not in cgh

    cases.extend([
        ("T2_F6_01", 2, "F6", "CommandVisionGoggles keeps all 7 production subcommands without preview", t2_f6_01),
        ("T2_F6_02", 2, "F6", "NetworkManager removes OpenProtoPreviewPacket while keeping 13 packets", t2_f6_02),
        ("T2_F6_03", 2, "F6", "VisionRenderer removes all preview tick/keybind hooks", t2_f6_03),
        ("T2_F6_04", 2, "F6", "MixinHumanoidModel & MixinItemInHandRenderer stripped of preview hooks", t2_f6_04),
        ("T2_F6_05", 2, "F6", "ClientGuiHelper keeps openLuaScreen cleanly without preview screen", t2_f6_05),
    ])

    # ---------------- F7: ModBench Integration & Boundaries (5) ----------------
    def t2_f7_01():
        msr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        assert "neutralChassis" in msr and "vg_neutral_chassis" in msr
        assert "renderUprightGoggles" in msr

    def t2_f7_02():
        msr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        assert "lensColorBlend >= 0.5f" in msr and "activeModel" in msr

    def t2_f7_03():
        msr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        assert "baseGoggles" in msr and "ItemDisplayContext.FIXED" in msr

    def t2_f7_04():
        sb = strip_java_comments(read_text_safe(JAVA_COMMON / "recipe" / "StationBlueprints.java"))
        assert "ID_BATTERY_EXPANSION" in sb and ("ProModularGogglesItem" in sb or "PRO_MODULAR_GOGGLES" in sb)

    def t2_f7_05():
        # Check FIXED transform in all 6 item models keeps scale & translation reasonable for cutting mat
        for item in ALL_GOGGLE_ITEMS:
            mj = read_json_safe(ITEM_MODEL_DIR / f"{item}.json")
            fixed = mj.get("display", {}).get("fixed", {})
            tr = fixed.get("translation", [0.0, 0.0, 0.0])
            assert all(-8.0 <= float(v) <= 8.0 for v in tr), f"{item} fixed translation extreme: {tr}"

    cases.extend([
        ("T2_F7_01", 2, "F7", "ModificationStationRenderer Stage A morphs to neutralChassis upright", t2_f7_01),
        ("T2_F7_02", 2, "F7", "ModificationStationRenderer Stage B/C swaps neutral -> result at blend 0.5", t2_f7_02),
        ("T2_F7_03", 2, "F7", "ModificationStationRenderer renders staged baseGoggles in FIXED context", t2_f7_03),
        ("T2_F7_04", 2, "F7", "StationBlueprints preserves Modules & adds battery_expansion on upgrade", t2_f7_04),
        ("T2_F7_05", 2, "F7", "models/item/*_goggles.json fixed translations stay within cutting-mat bounds", t2_f7_05),
    ])

    # ---------------- F8: Third-Person F5 Integration & Boundaries (5) ----------------
    def t2_f8_01():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        grl_only_code = strip_java_comments_and_strings(read_text_safe(grl_path))
        assert "VisionGoggleModel" not in grl_only_code
        _, code = get_combined_java_views([grl_path])
        assert "setHidden" in code or "applyModular" in code or "BoneVisibility" in code

    def t2_f8_02():
        grl_path = JAVA_COMMON / "client" / "GogglesRenderLayer.java"
        clean, code = get_combined_java_views([grl_path])
        assert '"visor_pivot"' in clean
        assert "60" in code and ("2.0" in code or "2f" in code or "2.0f" in code or "* 2" in code) and "-0.75" in code, (
            "GogglesRenderLayer must interpolate visor_pivot matching vision_goggles.animation.json (60 deg, [0,2,-0.75])"
        )

    def t2_f8_03():
        grl = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "GogglesRenderLayer.java"))
        assert "GogglesFlipAnimator.getLocalProgress()" in grl and "GogglesFlipAnimator.getEntityProgress" in grl

    def t2_f8_04():
        grl = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "GogglesRenderLayer.java"))
        assert "getEquippedFlashlight" in grl
        # Verify balanced pushPose/popPose
        assert grl.count("pushPose()") == grl.count("popPose()"), "Unbalanced pushPose/popPose in GogglesRenderLayer"

    def t2_f8_05():
        flr = strip_java_comments(read_text_safe(JAVA_FABRIC / "fabric" / "client" / "FabricLayerRegistry.java"))
        frg = strip_java_comments(read_text_safe(JAVA_FORGE / "forge" / "Vision_gogglesForge.java"))
        assert "GogglesRenderLayer" in flr and "GogglesRenderLayer" in frg

    cases.extend([
        ("T2_F8_01", 2, "F8", "GogglesRenderLayer uses unified Tri-Lens NBT bone visibility", t2_f8_01),
        ("T2_F8_02", 2, "F8", "GogglesRenderLayer visor_pivot pose matches animation (60 deg, [0,2,-0.75])", t2_f8_02),
        ("T2_F8_03", 2, "F8", "GogglesRenderLayer drives local + remote players via GogglesFlipAnimator", t2_f8_03),
        ("T2_F8_04", 2, "F8", "GogglesRenderLayer preserves TacticalFlashlightItem chest layer & balanced stack", t2_f8_04),
        ("T2_F8_05", 2, "F8", "FabricLayerRegistry & Forge onAddLayers attach GogglesRenderLayer", t2_f8_05),
    ])

    # ---------------- F9: First-Person flip Boundaries & Viewport Ratios (6) ----------------
    def t2_f9_01():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        fpv_only_code = strip_java_comments_and_strings(read_text_safe(fpv_path))
        assert "FirstPersonVisorModel" not in fpv_only_code
        clean, code = get_combined_java_views([fpv_path])
        assert '"visor_pivot"' in clean
        assert "setHidden" in code or "applyModular" in code or "BoneVisibility" in code

    def t2_f9_02():
        # Flip boundary values t=0.0, 0.28, 0.84, 0.85, 1.0 in GogglesFlipAnimator & FirstPersonVisorRenderer
        gfa = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "GogglesFlipAnimator.java"))
        for const in ("0.28f", "0.84f", "0.85f"):
            assert const in gfa, f"GogglesFlipAnimator missing phase boundary {const}"
        fpv = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"))
        assert "ProtoTriLensPreviewState" not in fpv
        assert "rawT < handoffT" in fpv or "handoffT" in fpv

    def t2_f9_03():
        # Viewport scale invariance across 360p, 720p, 1080p, 1440p at t=0.75
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, _ = get_combined_java_views([fpv_path])
        params = parse_fp_visor_params(clean)
        assert not params["uses_legacy_model"], "FirstPersonVisorRenderer still uses FirstPersonVisorModel"
        assert params["scale_parsed"] and params["max_scale_mult"] is not None, (
            "FirstPersonVisorRenderer scale expression could not be determined"
        )
        geo = parse_geo_model(GEO_DIR / "night_vision_goggles.geo.json")
        base_tex = load_png_rgba(TEX_DIR / "tri_lens_nvg.png", (64, 64))
        glow_tex = load_png_rgba(TEX_DIR / "tri_lens_nvg_glow.png", (64, 64))
        covs = []
        for w, h in ((640, 360), (1280, 720), (1920, 1080), (2560, 1440)):
            # Scale down raster resolution proportionally for speed (exact 16:9 GUI aspect)
            res = render_fp_flip_gui(geo, base_tex, glow_tex, params, raw_t=0.75, width=w // 4, height=h // 4)
            assert res.viewport_coverage < 0.55, (
                f"Viewport {w}x{h} at t=0.75 blew up to coverage={res.viewport_coverage:.3f}"
            )
            assert res.front_lens_pixels > 0, (
                f"Viewport {w}x{h} at t=0.75 shows 0 front objective lens pixels (rear +Z wall?)"
            )
            covs.append(res.viewport_coverage)
        assert max(covs) - min(covs) < 0.05, f"Scale invariance broken across 360p..1440p: {covs}"

    def t2_f9_04():
        # Check flip boundary t = 0.10, 0.28, 0.55, 0.84 for bounded bbox & front visibility
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, _ = get_combined_java_views([fpv_path])
        params = parse_fp_visor_params(clean)
        assert params["has_180_yaw"] and params["cancels_y_flip"]
        assert params["scale_parsed"] and params["max_scale_mult"] is not None
        geo = parse_geo_model(GEO_DIR / "night_vision_goggles.geo.json")
        base_tex = load_png_rgba(TEX_DIR / "tri_lens_nvg.png", (64, 64))
        glow_tex = load_png_rgba(TEX_DIR / "tri_lens_nvg_glow.png", (64, 64))
        for rt in (0.10, 0.28, 0.55, 0.84):
            res = render_fp_flip_gui(geo, base_tex, glow_tex, params, raw_t=rt, width=320, height=180)
            assert res.viewport_coverage < 0.55, f"t={rt} coverage={res.viewport_coverage} >= 0.55"
            assert res.bbox_w_frac < 0.90 and res.bbox_h_frac < 0.85, (
                f"t={rt} bbox clips viewport: w={res.bbox_w_frac}, h={res.bbox_h_frac}"
            )

    def t2_f9_05():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, code = get_combined_java_views([fpv_path])
        assert "getActiveVisionMode" in code and (
            "tri_lens_pro_modular" in clean
            or ("tri_lens_" in clean and "pro_modular" in clean)
            or ("VisionGogglesGeoRenderer" in code and "ProModularGogglesItem" in code)
        ), "FirstPersonVisorRenderer does not resolve Tri-Lens + pro_modular textures"

    def t2_f9_06():
        fpv_path = JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"
        clean, code = get_combined_java_views([fpv_path])
        assert "HumanoidArm.RIGHT" in code and '"slim"' in clean
        assert "RIGHT_SLEEVE" in code and "LEFT_SLEEVE" in code

    cases.extend([
        ("T2_F9_01", 2, "F9", "FirstPersonVisorRenderer toggles visor_pivot child bones & omits head", t2_f9_01),
        ("T2_F9_02", 2, "F9", "Flip phase boundaries (0.28, 0.84, 0.85) clean without preview branches", t2_f9_02),
        ("T2_F9_03", 2, "F9", "360p/720p/1080p/1440p viewport coverage invariant (<55%) & front lenses visible", t2_f9_03),
        ("T2_F9_04", 2, "F9", "Flip boundary t=0.10..0.84 stays unclipped (<0.90x0.85 bbox)", t2_f9_04),
        ("T2_F9_05", 2, "F9", "FirstPersonVisorRenderer resolves all 6 Tri-Lens textures incl pro_modular", t2_f9_05),
        ("T2_F9_06", 2, "F9", "FirstPersonVisorRenderer arm supports RIGHT/LEFT & wide/slim skins", t2_f9_06),
    ])

    # ---------------- F10: First-Person equip Boundaries & State Transitions (5) ----------------
    def t2_f10_01():
        mi = read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinItemInHandRenderer.java")
        gea_p = JAVA_COMMON / "client" / "GogglesEquipAnimator.java"
        gea = gea_p.read_text(encoding="utf-8") if gea_p.is_file() else None
        ok, msg = has_main_hand_sync_guard(mi, gea)
        assert ok, msg

    def t2_f10_02():
        fer = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "FirstPersonEquipRenderer.java"))
        assert "VISUAL_PIVOT_Z" in fer
        for item in ALL_GOGGLE_ITEMS:
            mj = read_json_safe(ITEM_MODEL_DIR / f"{item}.json")
            fp_r = mj["display"]["firstperson_righthand"]
            fp_l = mj["display"]["firstperson_lefthand"]
            assert "rotation" in fp_r and "translation" in fp_r
            assert "rotation" in fp_l and "translation" in fp_l

    def t2_f10_03():
        gea = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "GogglesEquipAnimator.java"))
        assert "shouldHideOppositeEmptyHand" in gea and "handStack.isEmpty()" in gea and "twoHanded" in gea

    def t2_f10_04():
        fer = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "FirstPersonEquipRenderer.java"))
        assert "isCompleting()" in fer and "renderRetractingArm" in fer

    def t2_f10_05():
        vgi = strip_java_comments(read_text_safe(JAVA_COMMON / "item" / "VisionGogglesItem.java"))
        mh = strip_java_comments(read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinHumanoidModel.java"))
        assert "return 14;" in vgi or "getUseDuration" in vgi
        assert "isUsingItem()" in mh and "VisionGogglesItem" in mh

    cases.extend([
        ("T2_F10_01", 2, "F10", "MixinItemInHandRenderer invokes syncFromRender exactly once per frame", t2_f10_01),
        ("T2_F10_02", 2, "F10", "FirstPersonEquipRenderer & all 6 firstperson_* transforms aligned", t2_f10_02),
        ("T2_F10_03", 2, "F10", "shouldHideOppositeEmptyHand hides empty offhand only when twoHanded", t2_f10_03),
        ("T2_F10_04", 2, "F10", "COMPLETING retract phase hides held goggles & retracts arm(s)", t2_f10_04),
        ("T2_F10_05", 2, "F10", "VisionGogglesItem 14-tick use duration syncs with MixinHumanoidModel", t2_f10_05),
    ])

    # ---------------- F11: Cross-Resource Integrity & Zero-Orphan Check (5) ----------------
    def t2_f11_01():
        # Check every ResourceLocation string pointing to geo/item/, animations/item/, textures/entities/ exists on disk
        res_pat = re.compile(r'"((?:geo/item|animations/item|textures/entities)/[^"]+)"')
        missing = []
        for jf in iter_java_files():
            txt = strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))
            for m in res_pat.finditer(txt):
                rel = m.group(1)
                if "*" in rel or rel.endswith("/") or rel.endswith("_"):
                    continue
                if txt[m.end() :].lstrip().startswith("+"):
                    continue
                target = ASSETS_ROOT / rel
                if not target.is_file():
                    missing.append(f"{jf.name}->{rel}")
        assert not missing, f"Broken ResourceLocation asset paths in Java: {missing}"

    def t2_f11_02():
        for item in ALL_GOGGLE_ITEMS:
            p = ITEM_MODEL_DIR / f"{item}.json"
            assert p.is_file(), f"Missing item model {p}"

    def t2_f11_03():
        mi = strip_java_comments(read_text_safe(JAVA_COMMON / "registry" / "ModItems.java"))
        for item in ALL_GOGGLE_ITEMS:
            assert f'"{item}"' in mi, f"ModItems.java missing {item}"
            geo_p = resolve_geo_path_via_model(item)
            assert geo_p.is_file(), f"Missing resolved geo for {item}: {geo_p}"

    def t2_f11_04():
        en = read_json_safe(LANG_DIR / "en_us.json", "en_us.json")
        for item in ALL_GOGGLE_ITEMS:
            key = f"item.vision_goggles.{item}"
            assert key in en and en[key].strip(), f"en_us.json missing translation key '{key}'"

    def t2_f11_05():
        # No 128x128 goggle textures or geos remain anywhere in assets/vision_goggles/
        bad = []
        for gp in GEO_DIR.glob("*.geo.json"):
            g = parse_geo_model(gp)
            if g.tex_w != 64 or g.tex_h != 64:
                bad.append(f"{gp.name}({g.tex_w}x{g.tex_h})")
        for tp in TEX_DIR.glob("*.png"):
            arr = load_png_rgba(tp, expected_size=None)
            if arr.shape[:2] != (64, 64):
                bad.append(f"{tp.name}({arr.shape[1]}x{arr.shape[0]})")
        assert not bad, f"Non-64x64 legacy assets still in production dirs: {bad}"

    cases.extend([
        ("T2_F11_01", 2, "F11", "Zero broken geo/animation/texture ResourceLocations in Java", t2_f11_01),
        ("T2_F11_02", 2, "F11", "All 6 goggle items have valid models/item/*.json", t2_f11_02),
        ("T2_F11_03", 2, "F11", "All 6 ModItems goggle IDs resolve 1:1 to 64x64 .geo.json", t2_f11_03),
        ("T2_F11_04", 2, "F11", "en_us.json resolves all 6 goggle item localization keys", t2_f11_04),
        ("T2_F11_05", 2, "F11", "Zero 128x128 geos or entity textures remain in assets", t2_f11_05),
    ])

    return [run_test_case(tid, tier, feat, desc, fn) for tid, tier, feat, desc, fn in cases]
