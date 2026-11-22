"""Tier 4: Real-world E2E scenarios, 3D contact-sheet generation, Gradle build & docs gates (9 scenarios)."""

from __future__ import annotations

import os
import re
import subprocess
import time
from pathlib import Path
from typing import List, Optional, Tuple

import numpy as np
from PIL import Image

from .common_utils import (
    ALL_GOGGLE_ITEMS,
    ALL_TARGET_TEXTURES,
    ASSETS_ROOT,
    GEO_DIR,
    ITEM_MODEL_DIR,
    JAVA_COMMON,
    LEGACY_128_TEXTURES,
    LEGACY_MODEL_FILES,
    PREVIEW_SYMBOLS,
    REPO_ROOT,
    STATIC_GOGGLE_ITEMS,
    STATIC_TEXTURES,
    TEX_DIR,
    TestRecord,
    get_combined_java_views,
    has_main_hand_sync_guard,
    has_symbol_token,
    iter_java_files,
    load_png_rgba,
    read_json_safe,
    read_text_safe,
    resolve_geo_path_via_model,
    run_test_case,
    strip_java_comments,
)
from .rasterizer3d import (
    check_bone_steve_head_overlap,
    make_contact_sheet,
    make_modular_visibility_fn,
    parse_fp_visor_params,
    parse_geo_model,
    render_f5_head,
    render_fp_equip_frame,
    render_fp_flip_gui,
    render_item_view,
)


def _try_load_tex_with_baseline_fallback(target_name: str, fallback_candidates: Tuple[str, ...]) -> Tuple[np.ndarray, Optional[str]]:
    p = TEX_DIR / target_name
    if p.is_file():
        with Image.open(p) as im:
            return np.array(im.convert("RGBA"), dtype=np.uint8), None
    for fb in fallback_candidates:
        fp = TEX_DIR / fb
        if fp.is_file():
            with Image.open(fp) as im:
                return np.array(im.convert("RGBA"), dtype=np.uint8), f"Missing target texture '{target_name}' (rendered baseline '{fb}')"
    return np.full((64, 64, 4), 120, dtype=np.uint8), f"Missing texture '{target_name}'"


def _try_load_geo_with_fallback(item_id: str) -> Tuple[object, Optional[str]]:
    p = resolve_geo_path_via_model(item_id)
    if p.is_file():
        geo = parse_geo_model(p)
        if geo.tex_w == 64 and geo.tex_h == 64:
            return geo, None
        return geo, f"{p.name} is still legacy {geo.tex_w}x{geo.tex_h} (expected 64x64 Tri-Lens)"
    return None, f"Missing geo '{p}'"


def run_tier4(out_dir: Path, run_gradle: bool = True) -> List[TestRecord]:
    out_dir.mkdir(parents=True, exist_ok=True)
    records: List[TestRecord] = []

    # 1. T4_E2E_01 — F5 Static Lineup (visor_down & visor_up, Front-3/4 & Side)
    def t4_e2e_01():
        tiles = []
        errors = []
        fallbacks = {
            "night_vision_goggles": ("nvg_texture.png", "proto_tri_lens_nvg.png"),
            "thermal_goggles": ("thermal_vision.png", "proto_tri_lens_thermal.png"),
            "biometric_goggles": ("bio_vision.png", "proto_tri_lens_bio.png"),
            "hydro_goggles": ("hidro_vision.png", "proto_tri_lens_hydro.png"),
        }
        for item in STATIC_GOGGLE_ITEMS:
            btex_name, gtex_name = STATIC_TEXTURES[item]
            geo, g_err = _try_load_geo_with_fallback(item)
            if g_err:
                errors.append(g_err)
            btex, b_err = _try_load_tex_with_baseline_fallback(btex_name, fallbacks[item])
            if b_err:
                errors.append(b_err)
            gtex_p = TEX_DIR / gtex_name
            gtex = load_png_rgba(gtex_p, (64, 64)) if gtex_p.is_file() else None

            if geo is None:
                continue

            if "visor_pivot" not in geo.bones_by_name:
                errors.append(f"{item}.geo.json missing 'visor_pivot' bone")
            overlaps = check_bone_steve_head_overlap(geo)
            if overlaps:
                errors.append(f"{item} overlaps Steve head: {overlaps[:3]}")

            for pose_lbl, vt in (("DOWN", 0.0), ("UP", 1.0)):
                for view_lbl, yaw, pitch in (("3/4", -35.0, 15.0), ("SIDE", 90.0, 5.0)):
                    res = render_f5_head(
                        geo, btex, gtex if vt == 0.0 else None,
                        visor_t=vt, yaw_deg=yaw, pitch_deg=pitch, size=200,
                    )
                    if pose_lbl == "DOWN" and gtex is not None and res.glow_pixels == 0 and view_lbl == "3/4":
                        errors.append(f"{item} F5 DOWN rendered 0 glow pixels")
                    tiles.append((res.image, f"{item.replace('_goggles','')}_{pose_lbl}_{view_lbl}"))

        sheet_path = make_contact_sheet(tiles, cols=4, out_path=out_dir / "f5_static_contact_sheet.png",
                                        title="T4_E2E_01: F5 Static Tri-Lens Lineup (DOWN/UP x 3/4 & SIDE)")
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path), "tiles": len(tiles)}

    # 2. T4_E2E_02 — F5 Modular & Pro Modular Dynamic Bone Combos
    def t4_e2e_02():
        combos = [
            ("Empty_Mod", False, []),
            ("Recon_NVG", False, ["vision_goggles:night_vision", "vision_goggles:solar", "vision_goggles:zoom"]),
            ("Breach_Therm", False, ["vision_goggles:thermal", "vision_goggles:flashlight", "vision_goggles:battery_expansion"]),
            ("Diver_Hydro", False, ["vision_goggles:hydro", "vision_goggles:sonar", "vision_goggles:environment"]),
            ("Bio_Lua", False, ["vision_goggles:biometric", "vision_goggles:vital_info", "vision_goggles:thermal_palette"]),
            ("Pro_Full", True, ["vision_goggles:thermal", "vision_goggles:zoom", "vision_goggles:flashlight", "vision_goggles:solar", "vision_goggles:battery_expansion", "threat_scanner"]),
        ]
        errors = []
        geo, g_err = _try_load_geo_with_fallback("modular_goggles")
        if g_err:
            errors.append(g_err)
        mod_tex, m_err = _try_load_tex_with_baseline_fallback("tri_lens_modular.png", ("proto_tri_lens_modular.png", "nvg_texture.png"))
        if m_err:
            errors.append(m_err)
        pro_tex, p_err = _try_load_tex_with_baseline_fallback("tri_lens_pro_modular.png", ("proto_tri_lens_modular.png", "nvg_texture.png"))
        if p_err:
            errors.append(p_err)

        tiles = []
        down_masks = {}
        if geo is not None:
            for name, is_pro, mods in combos:
                vis = make_modular_visibility_fn(mods, is_pro=is_pro)
                tex = pro_tex if is_pro else mod_tex
                r_down = render_f5_head(geo, tex, None, visor_t=0.0, yaw_deg=-42.0, pitch_deg=18.0, visible_fn=vis, size=200)
                r_up = render_f5_head(geo, tex, None, visor_t=1.0, yaw_deg=-42.0, pitch_deg=18.0, visible_fn=vis, size=200)
                tiles.append((r_down.image, f"{name}_DOWN"))
                tiles.append((r_up.image, f"{name}_UP"))
                down_masks[name] = r_down.mask

        # Check empty vs combos produce distinct rendered masks once modular bones exist
        if not errors and down_masks:
            base_m = down_masks["Empty_Mod"]
            for name, _, _ in combos[1:]:
                diff_px = int(np.sum(base_m ^ down_masks[name]))
                if diff_px < 10:
                    errors.append(f"Modular combo {name} produced insufficient visual bone delta vs Empty ({diff_px} px)")

        sheet_path = None
        if tiles:
            sheet_path = make_contact_sheet(
                tiles, cols=4, out_path=out_dir / "f5_modular_combos_contact_sheet.png",
                title="T4_E2E_02: F5 Modular & Pro Modular Dynamic Bone Combos",
            )
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path), "tiles": len(tiles)}

    # 3. T4_E2E_03 — First-Person flip Trajectory & Anti-Blowup Verification
    def t4_e2e_03():
        fpv_clean, _ = get_combined_java_views([JAVA_COMMON / "client" / "FirstPersonVisorRenderer.java"])
        params = parse_fp_visor_params(fpv_clean)
        errors = []
        if params["uses_legacy_model"]:
            errors.append("FirstPersonVisorRenderer still uses legacy FirstPersonVisorModel")
        if not params["cancels_y_flip"]:
            errors.append("FirstPersonVisorRenderer missing scale(1,-1,1) ortho Y-flip cancellation")
        if not params["has_180_yaw"]:
            errors.append("FirstPersonVisorRenderer does not rotate ~180 deg Y (shows rear +Z wall instead of front lenses)")
        if not params["uses_visor_pivot_name"]:
            errors.append("FirstPersonVisorRenderer does not reference 'visor_pivot' bone")
        if not params["scale_parsed"] or params["max_scale_mult"] is None:
            errors.append("FirstPersonVisorRenderer visor scale expression could not be determined")

        geo_nvg, g_err = _try_load_geo_with_fallback("night_vision_goggles")
        if g_err:
            errors.append(g_err)
        tex_nvg, t_err = _try_load_tex_with_baseline_fallback("tri_lens_nvg.png", ("proto_tri_lens_nvg.png", "nvg_texture.png"))
        if t_err:
            errors.append(t_err)
        glow_p = TEX_DIR / "tri_lens_nvg_glow.png"
        glow_nvg = load_png_rgba(glow_p, (64, 64)) if glow_p.is_file() else None

        tiles = []
        if geo_nvg is not None and params["scale_parsed"] and params["max_scale_mult"] is not None:
            for rt in (0.10, 0.28, 0.45, 0.65, 0.80, 0.84):
                res = render_fp_flip_gui(geo_nvg, tex_nvg, glow_nvg, params, raw_t=rt, width=320, height=180)
                tiles.append((res.image, f"NVG t={rt:.2f} cov={res.viewport_coverage:.2f}"))
                if res.viewport_coverage >= 0.55:
                    errors.append(f"t={rt:.2f}: viewport_coverage={res.viewport_coverage:.3f} >= 0.55 (144216.mp4 blowup!)")
                if res.bbox_w_frac >= 0.90 or res.bbox_h_frac >= 0.85:
                    errors.append(f"t={rt:.2f}: bbox clips viewport ({res.bbox_w_frac:.2f}x{res.bbox_h_frac:.2f})")
                if (rt >= 0.55 or res.viewport_coverage >= 0.03) and res.front_lens_pixels == 0:
                    errors.append(f"t={rt:.2f}: 0 front objective lens pixels visible (facing rear +Z wall)")

        sheet_path = None
        if tiles:
            sheet_path = make_contact_sheet(
                tiles, cols=3, out_path=out_dir / "fp_flip_trajectory_sheet.png",
                title="T4_E2E_03: First-Person Flip Trajectory (GuiGraphics Ortho)",
            )
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path), "tiles": len(tiles)}

    # 4. T4_E2E_04 — First-Person equip Trajectory & Single-Sync Integrity
    def t4_e2e_04():
        errors = []
        mi_txt = read_text_safe(JAVA_COMMON / "mixin" / "client" / "MixinItemInHandRenderer.java")
        gea_p = JAVA_COMMON / "client" / "GogglesEquipAnimator.java"
        gea_txt = gea_p.read_text(encoding="utf-8") if gea_p.is_file() else None
        ok_guard, guard_msg = has_main_hand_sync_guard(mi_txt, gea_txt)
        if not ok_guard:
            errors.append(guard_msg)

        geo, g_err = _try_load_geo_with_fallback("night_vision_goggles")
        if g_err:
            errors.append(g_err)
        tex, t_err = _try_load_tex_with_baseline_fallback("tri_lens_nvg.png", ("proto_tri_lens_nvg.png", "nvg_texture.png"))
        if t_err:
            errors.append(t_err)
        mj = read_json_safe(ITEM_MODEL_DIR / "night_vision_goggles.json")
        fp_r = mj["display"]["firstperson_righthand"]

        tiles = []
        y_centers = []
        if geo is not None:
            for t in (0.0, 0.20, 0.38, 0.65, 0.86, 0.98):
                res = render_fp_equip_frame(geo, tex, None, fp_r, t=t, width=280, height=160)
                tiles.append((res.image, f"Equip t={t:.2f}"))
                if res.bbox is not None:
                    cy = 0.5 * (res.bbox[1] + res.bbox[3])
                    y_centers.append((t, cy))
        if len(y_centers) >= 2:
            # Screen Y=0 is top; lifting to brow should move cy upward (smaller Y) by t=0.86 vs t=0.0
            if y_centers[-2][1] >= y_centers[0][1]:
                errors.append(f"Equip trajectory failed to lift upward toward brow (y0={y_centers[0][1]:.1f}, y86={y_centers[-2][1]:.1f})")

        sheet_path = None
        if tiles:
            sheet_path = make_contact_sheet(
                tiles, cols=3, out_path=out_dir / "fp_equip_trajectory_sheet.png",
                title="T4_E2E_04: First-Person Equip Trajectory",
            )
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path), "tiles": len(tiles)}

    # 5. T4_E2E_05 — ModBench Neutral -> Pro Modular Upgrade Transition
    def t4_e2e_05():
        errors = []
        geo, g_err = _try_load_geo_with_fallback("modular_goggles")
        if g_err:
            errors.append(g_err)
        neutral_tex, n_err = _try_load_tex_with_baseline_fallback("chassis_neutral.png", ("chassis_neutral.png",))
        if neutral_tex.shape[:2] != (64, 64):
            errors.append(f"chassis_neutral.png is {neutral_tex.shape[1]}x{neutral_tex.shape[0]}, expected 64x64")
        if n_err:
            errors.append(n_err)
        pro_tex, p_err = _try_load_tex_with_baseline_fallback("tri_lens_pro_modular.png", ("proto_tri_lens_modular.png", "nvg_texture.png"))
        if p_err:
            errors.append(p_err)
        pro_glow_p = TEX_DIR / "tri_lens_pro_modular_glow.png"
        pro_glow = load_png_rgba(pro_glow_p, (64, 64)) if pro_glow_p.is_file() else None

        mj = read_json_safe(ITEM_MODEL_DIR / "pro_modular_goggles.json")
        fixed_disp = mj.get("display", {}).get("fixed")
        if fixed_disp is None:
            errors.append("pro_modular_goggles.json missing display.fixed transform")
            fixed_disp = {}

        sheet_path = None
        if geo is not None:
            vis_a = make_modular_visibility_fn(["vision_goggles:thermal", "vision_goggles:zoom"], is_pro=True, neutral_chassis=True)
            vis_c = make_modular_visibility_fn(["vision_goggles:thermal", "vision_goggles:zoom", "vision_goggles:battery_expansion"], is_pro=True, neutral_chassis=False)

            r_a = render_item_view(geo, neutral_tex, None, fixed_disp, visible_fn=vis_a, size=220, extra_yaw_deg=180.0, extra_pitch_deg=15.0)
            r_c = render_item_view(geo, pro_tex, pro_glow, fixed_disp, visible_fn=vis_c, size=220, extra_yaw_deg=180.0, extra_pitch_deg=15.0)

            if r_a.glow_pixels != 0:
                errors.append(f"Stage A neutral chassis emitted {r_a.glow_pixels} glow pixels")
            if pro_glow is not None and r_c.glow_pixels == 0:
                errors.append("Stage C Pro Modular emitted 0 glow pixels")

            sheet_path = make_contact_sheet(
                [(r_a.image, "Stage A: vg_neutral_chassis"), (r_c.image, "Stage C: Pro+Thermal+Zoom+Batt")],
                cols=2, out_path=out_dir / "modbench_transition_sheet.png",
                title="T4_E2E_05: ModBench Neutral -> Pro Modular Upgrade",
            )
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path)}

    # 6. T4_E2E_06 — Inventory / GUI All-6 Lineup
    def t4_e2e_06():
        errors = []
        tiles = []
        renders = {}
        fallbacks = {
            "night_vision_goggles": ("nvg_texture.png", "proto_tri_lens_nvg.png"),
            "thermal_goggles": ("thermal_vision.png", "proto_tri_lens_thermal.png"),
            "biometric_goggles": ("bio_vision.png", "proto_tri_lens_bio.png"),
            "hydro_goggles": ("hidro_vision.png", "proto_tri_lens_hydro.png"),
            "modular_goggles": ("proto_tri_lens_modular.png", "nvg_texture.png"),
            "pro_modular_goggles": ("proto_tri_lens_modular.png", "nvg_texture.png"),
        }
        for item in ALL_GOGGLE_ITEMS:
            geo, g_err = _try_load_geo_with_fallback(item)
            if g_err:
                errors.append(g_err)
            btex_name, gtex_name = ALL_TARGET_TEXTURES[item]
            btex, b_err = _try_load_tex_with_baseline_fallback(btex_name, fallbacks[item])
            if b_err:
                errors.append(b_err)
            if geo is None:
                continue
            gtex_p = TEX_DIR / gtex_name
            gtex = load_png_rgba(gtex_p, (64, 64)) if gtex_p.is_file() else None
            mj = read_json_safe(ITEM_MODEL_DIR / f"{item}.json")
            vis = make_modular_visibility_fn([], is_pro=(item == "pro_modular_goggles")) if "modular" in item else None
            res = render_item_view(geo, btex, gtex, mj["display"]["gui"], visible_fn=vis, size=200)
            tiles.append((res.image, item.replace("_goggles", "")))
            renders[item] = np.array(res.image, dtype=np.int32)
            if res.bbox is not None:
                xmin, ymin, xmax, ymax = res.bbox
                if xmin <= 2 or ymin <= 2 or xmax >= 197 or ymax >= 197:
                    errors.append(f"{item} GUI render touches/clips viewport edge bbox={res.bbox}")
            else:
                errors.append(f"{item} GUI render produced 0 pixels")

        if not errors and "modular_goggles" in renders and "pro_modular_goggles" in renders:
            delta = float(np.abs(renders["modular_goggles"] - renders["pro_modular_goggles"]).mean())
            if delta < 0.4:
                errors.append(f"GUI modular vs pro_modular visual delta too small ({delta:.3f})")

        sheet_path = None
        if tiles:
            sheet_path = make_contact_sheet(
                tiles, cols=3, out_path=out_dir / "gui_all6_sheet.png",
                title="T4_E2E_06: Inventory / GUI All-6 Lineup",
            )
        assert not errors, " | ".join(errors[:5])
        return {"contact_sheet": str(sheet_path), "tiles": len(tiles)}

    # 7. T4_E2E_07 — Complete Resource Integrity & Zero-Legacy / Zero-Preview Sweep
    def t4_e2e_07():
        issues = []
        for p in LEGACY_MODEL_FILES:
            if p.exists():
                issues.append(f"Legacy model exists: {p.name}")
        for name in LEGACY_128_TEXTURES:
            if (TEX_DIR / name).exists():
                issues.append(f"Legacy 128x128 texture exists: {name}")
        for jf in iter_java_files():
            txt = strip_java_comments(jf.read_text(encoding="utf-8", errors="ignore"))
            for sym in PREVIEW_SYMBOLS:
                if has_symbol_token(txt, sym):
                    issues.append(f"{jf.name} references {sym}")
        assert not issues, f"Resource/cleanup sweep failed ({len(issues)} issues): {issues[:6]}"

    # 8. T4_E2E_08 — Multi-Loader Gradle Compilation Check
    def t4_e2e_08():
        gradlew = REPO_ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
        assert gradlew.is_file(), f"Missing Gradle wrapper: {gradlew}"
        cmd = [
            str(gradlew),
            ":common:compileJava",
            ":fabric:compileJava",
            ":forge:compileJava",
            "--console=plain",
        ]
        proc = subprocess.run(
            cmd,
            cwd=str(REPO_ROOT),
            capture_output=True,
            text=True,
            timeout=300,
        )
        tail = "\n".join((proc.stdout + "\n" + proc.stderr).splitlines()[-15:])
        assert proc.returncode == 0, f"Gradle compile failed (rc={proc.returncode}):\n{tail}"
        assert "BUILD SUCCESSFUL" in proc.stdout, f"Gradle output missing BUILD SUCCESSFUL:\n{tail}"
        return {"command": " ".join(cmd)}

    # 9. T4_E2E_09 — Documentation Continuity Check (docs/planes/, CHANGELOG.md, AGENTS.md § 7)
    def t4_e2e_09():
        planes = sorted((REPO_ROOT / "docs" / "planes").glob("plan_v46_*.md"))
        assert planes, "Missing M1 plan document matching docs/planes/plan_v46_*.md"
        # Ensure prior plans weren't deleted
        assert (REPO_ROOT / "docs" / "planes" / "plan_v44_rueda_octagonal_compacta.md").is_file(), (
            "Existing plan_v44_rueda_octagonal_compacta.md missing (plans must never be overwritten)"
        )
        changelog = read_text_safe(REPO_ROOT / "docs" / "CHANGELOG.md")
        lines = changelog.splitlines()
        assert len(lines) > 425, (
            f"docs/CHANGELOG.md missing new M4 Tri-Lens/modular changelog entry (lines={len(lines)} <= 425)"
        )
        assert ("mod_pro_armor" in changelog or "mod_optics_base" in changelog or "visor_pivot" in changelog), (
            "docs/CHANGELOG.md missing modular bone / visor_pivot changelog markers"
        )
        assert ("tri-lente" in changelog.lower() or "tri-lens" in changelog.lower()), (
            "docs/CHANGELOG.md missing Tri-Lens changelog entry"
        )
        agents = read_text_safe(REPO_ROOT / "AGENTS.md")
        assert "## 7. Memoria de Bugs Resueltos" in agents and "## 8. Ciclo de Ejecución" in agents

    scenarios = [
        ("T4_E2E_01", 4, "F1+F8", "F5 Steve head static lineup (DOWN/UP) + contact sheet", t4_e2e_01),
        ("T4_E2E_02", 4, "F3+F4+F8", "F5 Modular & Pro Modular 6-combo bone isolation + contact sheet", t4_e2e_02),
        ("T4_E2E_03", 4, "F9", "First-person flip trajectory anti-blowup & front-lens check + contact sheet", t4_e2e_03),
        ("T4_E2E_04", 4, "F5+F10", "First-person equip trajectory & MAIN_HAND single-sync + contact sheet", t4_e2e_04),
        ("T4_E2E_05", 4, "F2+F7", "ModBench vg_neutral_chassis -> Pro Modular transition + contact sheet", t4_e2e_05),
        ("T4_E2E_06", 4, "F1+F3+F5", "Inventory/GUI all-6 lineup & modular vs pro visual delta + contact sheet", t4_e2e_06),
        ("T4_E2E_07", 4, "F6+F11", "Full workspace zero-legacy & zero-preview symbol sweep", t4_e2e_07),
    ]

    for tid, tier, feat, desc, fn in scenarios:
        records.append(run_test_case(tid, tier, feat, desc, fn))

    if run_gradle:
        records.append(run_test_case("T4_E2E_08", 4, "F12", "Multi-loader Gradle compileJava (:common, :fabric, :forge)", t4_e2e_08))
    else:
        records.append(TestRecord(
            test_id="T4_E2E_08",
            tier=4,
            feature="F12",
            description="Multi-loader Gradle compileJava (:common, :fabric, :forge)",
            status="SKIPPED",
            duration_ms=0.0,
            message="Skipped (--skip-gradle enabled)",
        ))

    records.append(run_test_case("T4_E2E_09", 4, "F12", "Documentation continuity (docs/planes/plan_v46_*, CHANGELOG.md, AGENTS.md § 7)", t4_e2e_09))
    return records
