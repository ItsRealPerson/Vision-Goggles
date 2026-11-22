"""Vision Goggles E2E Test Suite Runner.

Usage:
    python scripts/e2e_tests/run_e2e.py --tier <1|2|3|4|all> [--with-gradle] [--skip-gradle] [--out-dir <path>] [--json]
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import time
from pathlib import Path
from typing import Dict, List

# Ensure repo root is on sys.path when invoked as `python scripts/e2e_tests/run_e2e.py`
_REPO_ROOT = Path(__file__).resolve().parent.parent.parent
if str(_REPO_ROOT) not in sys.path:
    sys.path.insert(0, str(_REPO_ROOT))

from scripts.e2e_tests.common_utils import DEFAULT_OUT_DIR, REPO_ROOT, TestRecord, run_test_case
from scripts.e2e_tests.tier1_unit import run_tier1
from scripts.e2e_tests.tier2_boundary import run_tier2
from scripts.e2e_tests.tier3_pairwise import run_tier3
from scripts.e2e_tests.tier4_e2e import run_tier4


def _run_standalone_gradle_check() -> TestRecord:
    import subprocess

    def _gradle():
        gradlew = REPO_ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew")
        assert gradlew.is_file(), f"Missing Gradle wrapper: {gradlew}"
        cmd = [
            str(gradlew),
            ":common:compileJava",
            ":fabric:compileJava",
            ":forge:compileJava",
            "--console=plain",
        ]
        proc = subprocess.run(cmd, cwd=str(REPO_ROOT), capture_output=True, text=True, timeout=300)
        tail = "\n".join((proc.stdout + "\n" + proc.stderr).splitlines()[-15:])
        assert proc.returncode == 0, f"Gradle compile failed (rc={proc.returncode}):\n{tail}"
        assert "BUILD SUCCESSFUL" in proc.stdout, f"Gradle missing BUILD SUCCESSFUL:\n{tail}"
        return {"command": " ".join(cmd)}

    return run_test_case("T4_E2E_08", 4, "F12", "Multi-loader Gradle compileJava (:common, :fabric, :forge)", _gradle)


def build_report(records: List[TestRecord], out_dir: Path, elapsed_ms: float) -> Dict:
    tiers_summary: Dict[str, Dict] = {}
    for rec in records:
        tkey = f"tier{rec.tier}"
        bucket = tiers_summary.setdefault(tkey, {"total": 0, "passed": 0, "failed": 0, "skipped": 0})
        bucket["total"] += 1
        if rec.status == "PASS":
            bucket["passed"] += 1
        elif rec.status == "SKIPPED":
            bucket["skipped"] += 1
        else:
            bucket["failed"] += 1

    total = len(records)
    passed = sum(1 for r in records if r.status == "PASS")
    failed = sum(1 for r in records if r.status == "FAIL")
    skipped = sum(1 for r in records if r.status == "SKIPPED")

    return {
        "summary": {
            "total": total,
            "passed": passed,
            "failed": failed,
            "skipped": skipped,
            "all_passed": failed == 0,
            "duration_ms": round(elapsed_ms, 2),
            "out_dir": str(out_dir),
        },
        "tiers": tiers_summary,
        "results": [r.to_dict() for r in records],
    }


def main(argv: List[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Vision Goggles E2E Test Suite")
    parser.add_argument(
        "--tier",
        choices=("1", "2", "3", "4", "all"),
        default="all",
        help="Test tier to execute (default: all)",
    )
    parser.add_argument(
        "--with-gradle",
        action="store_true",
        help="Force execution of multi-loader Gradle compileJava check even on Tiers 1-3",
    )
    parser.add_argument(
        "--skip-gradle",
        action="store_true",
        help="Skip Gradle compileJava check in Tier 4 / all for fast offline runs",
    )
    parser.add_argument(
        "--out-dir",
        default=DEFAULT_OUT_DIR,
        help=f"Directory for artifacts & e2e_report.json (default: {DEFAULT_OUT_DIR})",
    )
    parser.add_argument(
        "--json",
        action="store_true",
        help="Print structured JSON report to stdout",
    )
    args = parser.parse_args(argv)

    out_dir = Path(args.out_dir)
    out_dir.mkdir(parents=True, exist_ok=True)

    run_gradle_in_tier4 = (not args.skip_gradle) and (args.with_gradle or args.tier in ("4", "all"))
    t0 = time.perf_counter()
    records: List[TestRecord] = []

    if args.tier in ("1", "all"):
        records.extend(run_tier1())
    if args.tier in ("2", "all"):
        records.extend(run_tier2())
    if args.tier in ("3", "all"):
        records.extend(run_tier3())
    if args.tier in ("4", "all"):
        records.extend(run_tier4(out_dir=out_dir, run_gradle=run_gradle_in_tier4))
    elif args.with_gradle and not args.skip_gradle:
        records.append(_run_standalone_gradle_check())

    elapsed_ms = (time.perf_counter() - t0) * 1000.0
    report = build_report(records, out_dir=out_dir, elapsed_ms=elapsed_ms)

    report_path = out_dir / "e2e_report.json"
    report_path.write_text(json.dumps(report, indent=2), encoding="utf-8")

    if args.json:
        print(json.dumps(report, indent=2))
    else:
        s = report["summary"]
        print(f"=== Vision Goggles E2E Suite (tier={args.tier}) ===")
        print(
            f"Total: {s['total']} | PASS: {s['passed']} | FAIL: {s['failed']} | SKIPPED: {s['skipped']} | Time: {s['duration_ms']:.1f} ms"
        )
        for tname, tb in sorted(report["tiers"].items()):
            print(f"  {tname}: total={tb['total']} pass={tb['passed']} fail={tb['failed']} skip={tb['skipped']}")
        for r in records:
            if r.status != "PASS":
                print(f"  [{r.status}] {r.test_id} ({r.feature}): {r.message}")
        print(f"Report written to: {report_path}")

    return 0 if report["summary"]["all_passed"] else 1


if __name__ == "__main__":
    sys.exit(main())
