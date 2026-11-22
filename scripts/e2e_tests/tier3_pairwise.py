"""Tier 3: Pairwise cross-feature interaction tests (18 tests, >= 15)."""

from __future__ import annotations

import re
import numpy as np
from typing import List

from .common_utils import (
    ALL_MOD_BONES,
    ANIM_DIR,
    GEO_DIR,
    HEAD_MOD_BONES,
    ITEM_MODEL_DIR,
    JAVA_COMMON,
    LEGACY_128_TEXTURES,
    LEGACY_MODEL_FILES,
    PREVIEW_SYMBOLS,
    STATIC_GOGGLE_ITEMS,
    STATIC_TEXTURES,
    TEX_DIR,
    VISOR_MOD_BONES,
    TestRecord,
    get_combined_java_views,
    has_symbol_token,
    iter_java_files,
    load_png_rgba,
    read_json_safe,
    read_text_safe,
    resolve_geo_path_via_model,
    resolve_java_modular_nbt_rules,
    run_test_case,
    strip_java_comments,
    strip_java_comments_and_strings,
    unwrap_anim_vec,
)
from .rasterizer3d import (
    aabb_overlap_volume,
    collect_triangles,
    compute_bone_world_matrices,
    compute_cube_transformed_aabb,
    cubes_zfight_or_overlap,
    make_modular_visibility_fn,
    parse_fp_visor_params,
    parse_geo_model,
    render_f5_head,
    render_fp_equip_frame,
    render_fp_flip_gui,
    render_item_view,
)


def run_tier3() -> List[TestRecord]:
    cases = []

    def t3_01_f1_f5():
        vgm_path = JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java"
        vgr_path = JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java"
        clean, code = get_combined_java_views([vgr_path, vgm_path])
        for item, (btex, gtex) in STATIC_TEXTURES.items():
            geo = parse_geo_model(GEO_DIR / f"{item}.geo.json")
            assert geo.tex_w == 64 and geo.tex_h == 64
            stem = btex.replace(".png", "")
            suffix = stem.replace("tri_lens_", "")
            assert stem in clean or ("tri_lens_" in clean and f'"{suffix}"' in clean), (
                f"Missing {stem} mapping in VisionGogglesGeoRenderer/Model/helpers"
            )
            assert "VisionGogglesGeoModel" in code or "geo/item/" in clean

    def t3_02_f1_f8():
        anim = read_json_safe(ANIM_DIR / "vision_goggles.animation.json")
        up = anim["animations"]["idle_up"]["bones"]["visor_pivot"]
        rot = unwrap_anim_vec(up.get("rotation"))
        pos = unwrap_anim_vec(up.get("position"))
        assert rot == [-60, 0, 0] and pos == [0, 2, -0.75], f"idle_up rot={rot}, pos={pos}"
        grl_clean, grl_code = get_combined_java_views([JAVA_COMMON / "client" / "GogglesRenderLayer.java"])
        assert "visor_pivot" in grl_clean and "60" in grl_code and "-0.75" in grl_code

    def t3_03_f2_f5():
        neutral = load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        vis = make_modular_visibility_fn(["vision_goggles:thermal", "vision_goggles:solar"], is_pro=True, neutral_chassis=True)
        mj = read_json_safe(ITEM_MODEL_DIR / "modular_goggles.json")
        res = render_item_view(geo, neutral, None, mj["display"]["gui"], visible_fn=vis, size=128)
        assert res.glow_pixels == 0
        assert "mod_optics_base" in res.bone_pixel_counts
        for opt in ("mod_optics_thermal", "mod_solar", "mod_pro_armor"):
            assert opt not in res.bone_pixel_counts, f"{opt} leaked into neutral chassis render"

    def t3_04_f2_f7():
        msr = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        vgr_clean, _ = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        assert "vg_neutral_chassis" in msr and "vg_neutral_chassis" in vgr_clean
        assert "chassis_neutral" in vgr_clean
        load_png_rgba(TEX_DIR / "chassis_neutral.png", (64, 64))

    def t3_05_f3_f4():
        geo = parse_geo_model(resolve_geo_path_via_model("pro_modular_goggles"))
        pro = geo.bones_by_name.get("mod_pro_armor")
        head = geo.bones_by_name.get("head")
        assert pro is not None and head is not None and pro.parent == "head"
        bmats = compute_bone_world_matrices(geo)
        for pc in pro.cubes:
            pa = compute_cube_transformed_aabb(pc, bmats.get("mod_pro_armor"))
            for hc in head.cubes:
                ha = compute_cube_transformed_aabb(hc, bmats.get("head"))
                zf = cubes_zfight_or_overlap(pa, ha)
                assert zf is None, f"mod_pro_armor cube z-fights/overlaps head cube: {zf}"

    def t3_06_f3_f8():
        grl_clean, grl_code = get_combined_java_views([JAVA_COMMON / "client" / "GogglesRenderLayer.java"])
        assert (
            "ProModularGogglesItem" in grl_code
            or "tri_lens_pro_modular" in grl_clean
            or ("tri_lens_" in grl_clean and "pro_modular" in grl_clean)
        ), "GogglesRenderLayer/helpers do not differentiate ProModularGogglesItem / tri_lens_pro_modular"
        geo = parse_geo_model(resolve_geo_path_via_model("pro_modular_goggles"))
        mod_tex = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64))
        pro_tex = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64))
        r_mod = render_f5_head(geo, mod_tex, None, visor_t=0.0, visible_fn=make_modular_visibility_fn([], is_pro=False), size=128)
        r_pro = render_f5_head(geo, pro_tex, None, visor_t=0.0, visible_fn=make_modular_visibility_fn([], is_pro=True), size=128)
        diff = float(np.abs(np.array(r_mod.image, dtype=np.int32) - np.array(r_pro.image, dtype=np.int32)).mean())
        assert diff > 0.5, f"F5 Pro vs Standard Modular visual delta too small ({diff:.3f})"

    def t3_07_f4_f5():
        # Full cross-combo SOLAR+BATTERY_EXPANSION+ZOOM+FLASHLIGHT+Lua+THERMAL/HYDRO/BIOMETRIC
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        mod_tex = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64))
        pro_tex = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64))
        mj = read_json_safe(ITEM_MODEL_DIR / "modular_goggles.json")
        for mode_id, opt_bone in (
            ("vision_goggles:thermal", "mod_optics_thermal"),
            ("vision_goggles:hydro", "mod_optics_hydro"),
            ("vision_goggles:biometric", "mod_optics_bio"),
        ):
            mods = ["SOLAR", "BATTERY_EXPANSION", "vision_goggles:zoom", "FLASHLIGHT", "threat_scanner", mode_id]
            for is_pro, tex in ((False, mod_tex), (True, pro_tex)):
                vis = make_modular_visibility_fn(mods, is_pro=is_pro)
                res = render_item_view(geo, tex, None, mj["display"]["gui"], visible_fn=vis, size=128)
                assert opt_bone in res.bone_pixel_counts and "mod_zoom" in res.bone_pixel_counts, (
                    f"Missing expected optical/zoom bone ({opt_bone}, mod_zoom) for {mode_id}: {sorted(res.bone_pixel_counts)}"
                )
                if is_pro:
                    assert "mod_pro_armor" in res.bone_pixel_counts, (
                        f"Pro Modular missing mod_pro_armor for {mode_id}"
                    )
                else:
                    assert "mod_pro_armor" not in res.bone_pixel_counts, (
                        f"Standard Modular leaked mod_pro_armor for {mode_id}"
                    )

    def t3_08_f4_f8():
        # F5 flip-up (-60 deg + [0,2,-0.75]): static head mod_* vertices remain 100% stationary while visor_pivot children move!
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        mods = ["vision_goggles:solar", "vision_goggles:battery_expansion", "vision_goggles:zoom", "vision_goggles:thermal"]
        vis = make_modular_visibility_fn(mods, is_pro=True)
        ident = np.eye(4, dtype=np.float64)
        tris_down = collect_triangles(geo, ident, visor_t=0.0, visible_fn=vis)
        tris_up = collect_triangles(geo, ident, visor_t=1.0, visible_fn=vis)

        for bname in ("mod_solar", "mod_battery_pack", "mod_pro_armor"):
            d_tris = [t[0] for t in tris_down if t[3] == bname]
            u_tris = [t[0] for t in tris_up if t[3] == bname]
            assert d_tris and u_tris, f"Missing geometry for static bone {bname} in modular_goggles.geo.json"
            pts_d = np.vstack(d_tris)
            pts_u = np.vstack(u_tris)
            assert np.allclose(pts_d, pts_u, atol=1e-6), f"Static bone {bname} moved during visor flip!"

        for bname in ("mod_zoom", "mod_optics_thermal"):
            d_tris = [t[0] for t in tris_down if t[3] == bname]
            u_tris = [t[0] for t in tris_up if t[3] == bname]
            assert d_tris and u_tris, f"Missing geometry for visor bone {bname} in modular_goggles.geo.json"
            pts_d = np.vstack(d_tris)
            pts_u = np.vstack(u_tris)
            delta_y = float(pts_u[:, 1].mean() - pts_d[:, 1].mean())
            assert delta_y > 0.08, f"Visor bone {bname} failed to lift during flip (dy={delta_y:.4f})"

    def t3_09_f4_f9():
        # First-person flip renders visor_pivot children (mod_zoom, mod_optics_thermal) and excludes head bones
        fpv_clean, _ = get_combined_java_views([JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"])
        params = parse_fp_visor_params(fpv_clean)
        assert params["uses_baked_geo"] and params["uses_visor_pivot_name"]
        assert params["scale_parsed"], "FirstPersonVisorRenderer scale expression unparseable"
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        tex = load_png_rgba(TEX_DIR / "tri_lens_pro_modular.png", (64, 64))
        glow = load_png_rgba(TEX_DIR / "tri_lens_pro_modular_glow.png", (64, 64))
        vis = make_modular_visibility_fn(["vision_goggles:zoom", "vision_goggles:thermal", "vision_goggles:solar"], is_pro=True)
        res = render_fp_flip_gui(geo, tex, glow, params, raw_t=0.65, width=320, height=180, visible_fn=vis)
        assert "mod_solar" not in res.bone_pixel_counts and "head" not in res.bone_pixel_counts
        assert "mod_zoom" in res.bone_pixel_counts and "mod_optics_thermal" in res.bone_pixel_counts

    def t3_10_f4_f10():
        # First-person equip renders modular item with installed module bones
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        tex = load_png_rgba(TEX_DIR / "tri_lens_modular.png", (64, 64))
        mj = read_json_safe(ITEM_MODEL_DIR / "modular_goggles.json")
        vis = make_modular_visibility_fn(["vision_goggles:solar", "vision_goggles:zoom", "vision_goggles:night_vision"])
        res = render_fp_equip_frame(geo, tex, None, mj["display"]["firstperson_righthand"], t=0.55, visible_fn=vis, width=240, height=135)
        assert res.viewport_coverage > 0.01

    def t3_11_f5_f7():
        _, vgr_code = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        msr = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "renderer" / "ModificationStationRenderer.java"))
        has_y = bool(re.search(r"(?:-\s*29(?:\.0+)?[fFdD]?\s*/\s*16(?:\.0+)?[fFdD]?|-\s*1\.8125[fFdD]?)", vgr_code))
        has_z = bool(re.search(r"(?:2\.75[fFdD]?\s*/\s*16(?:\.0+)?[fFdD]?|0?\.171875[fFdD]?)", vgr_code))
        assert has_y and has_z and "ItemDisplayContext.FIXED" in msr

    def t3_12_f5_f10():
        _, vgr_code = get_combined_java_views([
            JAVA_COMMON / "client" / "renderer" / "VisionGogglesGeoRenderer.java",
            JAVA_COMMON / "client" / "model" / "VisionGogglesGeoModel.java",
        ])
        fer = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "FirstPersonEquipRenderer.java"))
        has_y = bool(re.search(r"(?:-\s*29(?:\.0+)?[fFdD]?\s*/\s*16(?:\.0+)?[fFdD]?|-\s*1\.8125[fFdD]?)", vgr_code))
        assert has_y and "FIRST_PERSON_RIGHT_HAND" in fer

    def t3_13_f6_f8():
        grl = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "GogglesRenderLayer.java"))
        assert "ProtoTriLensGeoRenderer" not in grl and "ProtoTriLensPreviewState" not in grl

    def t3_14_f6_f9():
        fpv = strip_java_comments(read_text_safe(JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"))
        assert "ProtoTriLensGeoRenderer" not in fpv and "ProtoTriLensPreviewState" not in fpv

    def t3_15_f6_f11():
        hits = []
        retired = ["VisionGoggleModel", "FirstPersonVisorModel"] + list(LEGACY_128_TEXTURES)
        for jf in iter_java_files():
            txt = strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))
            for sym in PREVIEW_SYMBOLS:
                if has_symbol_token(txt, sym):
                    hits.append(f"{jf.name}:{sym}")
            for sym in retired:
                if has_symbol_token(txt, sym):
                    hits.append(f"{jf.name}:{sym}")
        assert not hits, f"Dead preview/legacy symbols remain: {hits[:8]}"

    def t3_16_f8_f9():
        gfa = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "GogglesFlipAnimator.java"))
        grl = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "GogglesRenderLayer.java"))
        fpv = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"))
        assert "GogglesFlipAnimator" in grl and "GogglesFlipAnimator" in fpv
        assert "0.28f" in gfa and "0.84f" in gfa and "0.85f" in gfa

    def t3_17_f9_f10():
        gea = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "client" / "GogglesEquipAnimator.java"))
        mi = strip_java_comments_and_strings(read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinItemInHandRenderer.java"))
        assert "GogglesFlipAnimator.isLocalTransitioning()" in gea
        assert "GogglesFlipAnimator.isLocalTransitioning()" in mi

    def t3_18_multi_optics_zfight():
        # Co-enabled vision modes (NVG+HYDRO, THERMAL+HYDRO, THERMAL+BIO) must NOT overlap or coplanar-zfight!
        geo = parse_geo_model(GEO_DIR / "modular_goggles.geo.json")
        optics_bones = ["mod_optics_nvg", "mod_optics_thermal", "mod_optics_bio", "mod_optics_hydro"]
        for b in optics_bones:
            assert b in geo.bones_by_name and geo.bones_by_name[b].cubes, f"Missing cubes in {b}"
        bmats = compute_bone_world_matrices(geo)
        for i in range(len(optics_bones)):
            for j in range(i + 1, len(optics_bones)):
                b1, b2 = geo.bones_by_name[optics_bones[i]], geo.bones_by_name[optics_bones[j]]
                for c1 in b1.cubes:
                    a1 = compute_cube_transformed_aabb(c1, bmats.get(b1.name))
                    for c2 in b2.cubes:
                        a2 = compute_cube_transformed_aabb(c2, bmats.get(b2.name))
                        zf = cubes_zfight_or_overlap(a1, a2)
                        assert zf is None, f"Co-enabled {b1.name} and {b2.name} z-fight/overlap: {zf}"

    cases = [
        ("T3_F1_F5", 3, "F1+F5", "Static Tri-Lens assets match VisionGogglesGeoModel/Renderer", t3_01_f1_f5),
        ("T3_F1_F8", 3, "F1+F8", "Static Tri-Lens animation keyframes match GogglesRenderLayer visor_pivot", t3_02_f1_f8),
        ("T3_F2_F5", 3, "F2+F5", "chassis_neutral suppresses glow & optional modules in GeoRenderer", t3_03_f2_f5),
        ("T3_F2_F7", 3, "F2+F7", "chassis_neutral integrates with ModificationStationRenderer Stage A->B", t3_04_f2_f7),
        ("T3_F3_F4", 3, "F3+F4", "Pro mod_pro_armor avoids z-fighting with Master head cubes", t3_05_f3_f4),
        ("T3_F3_F8", 3, "F3+F8", "F5 layer clearly differentiates Pro Modular vs Standard Modular", t3_06_f3_f8),
        ("T3_F4_F5", 3, "F4+F5", "Full modular combos render cleanly in GUI across Standard & Pro", t3_07_f4_f5),
        ("T3_F4_F8", 3, "F4+F8", "F5 flip leaves head mod_* stationary while visor_pivot children lift", t3_08_f4_f8),
        ("T3_F4_F9", 3, "F4+F9", "First-person flip renders visor_pivot children & omits head straps", t3_09_f4_f9),
        ("T3_F4_F10", 3, "F4+F10", "First-person equip renders installed modular bones cleanly", t3_10_f4_f10),
        ("T3_F5_F7", 3, "F5+F7", "GeoRenderer centering aligns with ModBench FIXED display", t3_11_f5_f7),
        ("T3_F5_F10", 3, "F5+F10", "GeoRenderer centering aligns with FirstPersonEquipRenderer", t3_12_f5_f10),
        ("T3_F6_F8", 3, "F6+F8", "GogglesRenderLayer has zero ProtoTriLens preview fallback", t3_13_f6_f8),
        ("T3_F6_F9", 3, "F6+F9", "FirstPersonVisorRenderer has zero ProtoTriLens preview fallback", t3_14_f6_f9),
        ("T3_F6_F11", 3, "F6+F11", "Zero preview or legacy symbols across common, fabric, forge", t3_15_f6_f11),
        ("T3_F8_F9", 3, "F8+F9", "F5 & First-person flip share GogglesFlipAnimator phase contract", t3_16_f8_f9),
        ("T3_F9_F10", 3, "F9+F10", "First-person flip & equip enforce mutual exclusion", t3_17_f9_f10),
        ("T3_F4_ZFIGHT", 3, "F4+F5", "Multi-mode co-enabled mod_optics_* bones never share identical AABB", t3_18_multi_optics_zfight),
    ]

    return [run_test_case(tid, tier, feat, desc, fn) for tid, tier, feat, desc, fn in cases]
