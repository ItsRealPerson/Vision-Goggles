"""Add 3D ocular eyecup rings + recessed inner lenses on the +Z (eye) side of visor_pivot.

Z range [-5.5, -4.85] (never touches head strap at Z >= -4.75). Idempotent.
Run from repo root: python scripts/add_eyecups.py
"""
import json
from pathlib import Path

GEO = Path("common/src/main/resources/assets/vision_goggles/geo/item")
RIM_UV = [42, 16]        # UV_EDGE_TRIM dark housing (zero glow)
DARK_GLASS = [25, 46]    # UV_OPT_BASE_GLASS
GLOW = {"nvg": [36, 46], "thermal": [42, 46], "bio": [48, 46], "hydro": [54, 46]}
STATIC = {
    "night_vision_goggles": "nvg", "thermal_goggles": "thermal",
    "biometric_goggles": "bio", "hydro_goggles": "hydro",
}
CY = 26.75
Z0, DEPTH = -5.5, 0.65
MARK_SIZE = [3.4, 0.5, 0.65]


def faces(uv, w=2, h=2):
    return {k: {"uv": list(uv), "uv_size": [w, h]} for k in ("north", "east", "south", "west", "up", "down")}


def cube(x0, y0, z0, sx, sy, sz, uv, w=2, h=2):
    return {"origin": [x0, y0, z0], "size": [sx, sy, sz], "uv": faces(uv, w, h)}


def rim(cx):
    return [
        cube(cx - 1.7, CY + 1.2, Z0, 3.4, 0.5, DEPTH, RIM_UV),   # top (MARK_SIZE)
        cube(cx - 1.7, CY - 1.7, Z0, 3.4, 0.5, DEPTH, RIM_UV),   # bottom
        cube(cx - 1.7, CY - 1.2, Z0, 0.5, 2.4, DEPTH, RIM_UV),   # left
        cube(cx + 1.2, CY - 1.2, Z0, 0.5, 2.4, DEPTH, RIM_UV),   # right
    ]


def lens(cx, uv, dz=0.0):
    return cube(cx - 1.2, CY - 1.2, Z0 + dz, 2.4, 2.4, 0.25, uv, 4, 4)


def bone(d, name):
    return next((b for b in d["minecraft:geometry"][0]["bones"] if b["name"] == name), None)


def main():
    for f in sorted(GEO.glob("*.geo.json")):
        d = json.loads(f.read_text(encoding="utf-8"))
        visor = bone(d, "visor_pivot")
        if any(c["size"] == MARK_SIZE and c["origin"][2] == Z0 for c in visor["cubes"]):
            print("skip", f.name)
            continue
        for cx in (-2.1, 2.1):
            visor["cubes"].extend(rim(cx))
        name = f.name.replace(".geo.json", "")
        if name in STATIC:
            for cx in (-2.1, 2.1):
                visor["cubes"].append(lens(cx, GLOW[STATIC[name]]))
        else:
            for i, (b, uv) in enumerate([("mod_optics_base", DARK_GLASS)] + [(f"mod_optics_{k}", v) for k, v in GLOW.items()]):
                tb = bone(d, b)
                if tb is None:
                    continue
                for cx in (-2.1, 2.1):
                    tb.setdefault("cubes", []).append(lens(cx, uv, 0.01 * i))
        f.write_text(json.dumps(d, indent=2), encoding="utf-8")
        print("patched", f.name)


if __name__ == "__main__":
    main()
