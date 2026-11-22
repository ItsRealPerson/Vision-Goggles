"""Headless Z-buffered 3D Bedrock .geo.json software rasterizer (numpy + PIL only)."""

from __future__ import annotations

import math
import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Callable, Dict, List, Optional, Sequence, Tuple

import numpy as np
from PIL import Image, ImageDraw

from .common_utils import (
    ALL_MOD_BONES,
    GLOW_ALLOWED_RECTS,
    STEVE_HEAD_BOUNDS,
    is_in_glow_rects,
    load_png_rgba,
    read_json_safe,
    strip_java_comments,
    strip_java_comments_and_strings,
)


def _rot_x(rad: float) -> np.ndarray:
    c, s = math.cos(rad), math.sin(rad)
    return np.array(
        [[1, 0, 0, 0], [0, c, -s, 0], [0, s, c, 0], [0, 0, 0, 1]], dtype=np.float64
    )


def _rot_y(rad: float) -> np.ndarray:
    c, s = math.cos(rad), math.sin(rad)
    return np.array(
        [[c, 0, s, 0], [0, 1, 0, 0], [-s, 0, c, 0], [0, 0, 0, 1]], dtype=np.float64
    )


def _rot_z(rad: float) -> np.ndarray:
    c, s = math.cos(rad), math.sin(rad)
    return np.array(
        [[c, -s, 0, 0], [s, c, 0, 0], [0, 0, 1, 0], [0, 0, 0, 1]], dtype=np.float64
    )


def _translate(x: float, y: float, z: float) -> np.ndarray:
    m = np.eye(4, dtype=np.float64)
    m[0, 3], m[1, 3], m[2, 3] = x, y, z
    return m


def _scale(sx: float, sy: float, sz: float) -> np.ndarray:
    return np.diag([sx, sy, sz, 1.0]).astype(np.float64)


def _look_at(eye: np.ndarray, target: np.ndarray, up: np.ndarray) -> np.ndarray:
    f = target - eye
    f = f / (np.linalg.norm(f) + 1e-12)
    u = up / (np.linalg.norm(up) + 1e-12)
    s = np.cross(f, u)
    s = s / (np.linalg.norm(s) + 1e-12)
    u = np.cross(s, f)
    m = np.eye(4, dtype=np.float64)
    m[0, :3] = s
    m[1, :3] = u
    m[2, :3] = -f
    m[0, 3] = -np.dot(s, eye)
    m[1, 3] = -np.dot(u, eye)
    m[2, 3] = np.dot(f, eye)
    return m


def _perspective(fov_y_deg: float, aspect: float, near: float, far: float) -> np.ndarray:
    f = 1.0 / math.tan(math.radians(fov_y_deg) * 0.5)
    m = np.zeros((4, 4), dtype=np.float64)
    m[0, 0] = f / aspect
    m[1, 1] = f
    m[2, 2] = (far + near) / (near - far)
    m[2, 3] = (2.0 * far * near) / (near - far)
    m[3, 2] = -1.0
    return m


@dataclass
class CubeSpec:
    bone_name: str
    origin: Tuple[float, float, float]
    size: Tuple[float, float, float]
    inflate: float
    pivot: Tuple[float, float, float]
    rotation_deg: Tuple[float, float, float]
    uv_faces: Dict[str, Tuple[Tuple[float, float], Tuple[float, float]]]


@dataclass
class BoneSpec:
    name: str
    parent: Optional[str]
    pivot: Tuple[float, float, float]
    rotation_deg: Tuple[float, float, float]
    cubes: List[CubeSpec] = field(default_factory=list)
    children: List["BoneSpec"] = field(default_factory=list)


@dataclass
class GeoModel:
    identifier: str
    tex_w: int
    tex_h: int
    visible_bounds_w: float
    visible_bounds_h: float
    visible_bounds_offset: Tuple[float, float, float]
    bones_by_name: Dict[str, BoneSpec]
    top_bones: List[BoneSpec]


def parse_geo_model(path: Path) -> GeoModel:
    data = read_json_safe(path, label=f"geo model {path.name}")
    geos = data.get("minecraft:geometry")
    if not isinstance(geos, list) or not geos:
        raise AssertionError(f"{path.name}: missing non-empty 'minecraft:geometry' list")
    g = geos[0]
    desc = g.get("description", {})
    tex_w = int(desc.get("texture_width", 64))
    tex_h = int(desc.get("texture_height", 64))
    vb_w = float(desc.get("visible_bounds_width", 0.0))
    vb_h = float(desc.get("visible_bounds_height", 0.0))
    vb_off = tuple(float(v) for v in desc.get("visible_bounds_offset", [0.0, 0.0, 0.0]))

    raw_bones = g.get("bones", [])
    bones_by_name: Dict[str, BoneSpec] = {}
    for rb in raw_bones:
        bname = rb.get("name")
        if not bname:
            raise AssertionError(f"{path.name}: bone missing 'name'")
        if bname in bones_by_name:
            raise AssertionError(f"{path.name}: duplicate bone name '{bname}'")
        piv = tuple(float(v) for v in rb.get("pivot", [0.0, 0.0, 0.0]))
        rot = tuple(float(v) for v in rb.get("rotation", [0.0, 0.0, 0.0]))
        cubes: List[CubeSpec] = []
        for rc in rb.get("cubes", []):
            orig = tuple(float(v) for v in rc.get("origin", [0.0, 0.0, 0.0]))
            sz = tuple(float(v) for v in rc.get("size", [0.0, 0.0, 0.0]))
            inf = float(rc.get("inflate", 0.0))
            cpiv = tuple(float(v) for v in rc.get("pivot", orig))
            crot = tuple(float(v) for v in rc.get("rotation", [0.0, 0.0, 0.0]))
            uv_raw = rc.get("uv", {})
            uv_faces: Dict[str, Tuple[Tuple[float, float], Tuple[float, float]]] = {}
            if isinstance(uv_raw, dict):
                for fname, fval in uv_raw.items():
                    if isinstance(fval, dict) and "uv" in fval and "uv_size" in fval:
                        u0, v0 = float(fval["uv"][0]), float(fval["uv"][1])
                        du, dv = float(fval["uv_size"][0]), float(fval["uv_size"][1])
                        uv_faces[fname] = ((u0, v0), (du, dv))
            cubes.append(
                CubeSpec(
                    bone_name=bname,
                    origin=orig,
                    size=sz,
                    inflate=inf,
                    pivot=cpiv,
                    rotation_deg=crot,
                    uv_faces=uv_faces,
                )
            )
        bones_by_name[bname] = BoneSpec(
            name=bname,
            parent=rb.get("parent"),
            pivot=piv,
            rotation_deg=rot,
            cubes=cubes,
        )

    top_bones: List[BoneSpec] = []
    for b in bones_by_name.values():
        if b.parent is None:
            top_bones.append(b)
        else:
            if b.parent not in bones_by_name:
                raise AssertionError(
                    f"{path.name}: orphan bone '{b.name}' references missing parent '{b.parent}'"
                )
            bones_by_name[b.parent].children.append(b)

    return GeoModel(
        identifier=str(desc.get("identifier", "")),
        tex_w=tex_w,
        tex_h=tex_h,
        visible_bounds_w=vb_w,
        visible_bounds_h=vb_h,
        visible_bounds_offset=(vb_off[0], vb_off[1], vb_off[2]),
        bones_by_name=bones_by_name,
        top_bones=top_bones,
    )


def _cube_world_matrix(cube: CubeSpec) -> np.ndarray:
    rx, ry, rz = cube.rotation_deg
    if rx == 0.0 and ry == 0.0 and rz == 0.0:
        return np.eye(4, dtype=np.float64)
    px, py, pz = [v / 16.0 for v in cube.pivot]
    # GeckoLib converts Bedrock degrees [rx, ry, rz] to radians [-rx, -ry, rz]
    m_rot = (
        _rot_z(math.radians(rz))
        @ _rot_y(math.radians(-ry))
        @ _rot_x(math.radians(-rx))
    )
    return _translate(px, py, pz) @ m_rot @ _translate(-px, -py, -pz)


def _cube_faces(cube: CubeSpec, tex_w: int, tex_h: int):
    ox, oy, oz = cube.origin
    sx, sy, sz = cube.size
    d = cube.inflate
    x0, x1 = (ox - d) / 16.0, (ox + sx + d) / 16.0
    y0, y1 = (oy - d) / 16.0, (oy + sy + d) / 16.0
    z0, z1 = (oz - d) / 16.0, (oz + sz + d) / 16.0

    # Outward-CCW face quads with (u0, v0) at top-left when viewed from outside
    face_defs = {
        "north": ((x1, y1, z0), (x1, y0, z0), (x0, y0, z0), (x0, y1, z0)),
        "south": ((x0, y1, z1), (x0, y0, z1), (x1, y0, z1), (x1, y1, z1)),
        "west": ((x0, y1, z1), (x0, y0, z1), (x0, y0, z0), (x0, y1, z0)),
        "east": ((x1, y1, z0), (x1, y0, z0), (x1, y0, z1), (x1, y1, z1)),
        "up": ((x0, y1, z1), (x0, y1, z0), (x1, y1, z0), (x1, y1, z1)),
        "down": ((x0, y0, z0), (x0, y0, z1), (x1, y0, z1), (x1, y0, z0)),
    }
    out = []
    for fname, corners in face_defs.items():
        if fname not in cube.uv_faces:
            continue
        (u0, v0), (du, dv) = cube.uv_faces[fname]
        # Skip 0-area faces on flat planes
        if du == 0.0 or dv == 0.0:
            continue
        # corners are TL, BL, BR, TR
        uvs = (
            (u0 / tex_w, v0 / tex_h),
            (u0 / tex_w, (v0 + dv) / tex_h),
            ((u0 + du) / tex_w, (v0 + dv) / tex_h),
            ((u0 + du) / tex_w, v0 / tex_h),
        )
        out.append((fname, corners, uvs))
    return out


def compute_cube_transformed_aabb(cube: CubeSpec, bone_matrix: Optional[np.ndarray] = None) -> Tuple[float, float, float, float, float, float]:
    """Return (xmin, xmax, ymin, ymax, zmin, zmax) in Bedrock units (16 units = 1 block)."""
    ox, oy, oz = cube.origin
    sx, sy, sz = cube.size
    d = cube.inflate
    x0, x1 = (ox - d) / 16.0, (ox + sx + d) / 16.0
    y0, y1 = (oy - d) / 16.0, (oy + sy + d) / 16.0
    z0, z1 = (oz - d) / 16.0, (oz + sz + d) / 16.0
    corners = np.array(
        [
            [x, y, z, 1.0]
            for x in (x0, x1)
            for y in (y0, y1)
            for z in (z0, z1)
        ],
        dtype=np.float64,
    )
    m = _cube_world_matrix(cube)
    if bone_matrix is not None:
        m = bone_matrix @ m
    pts = (m @ corners.T).T[:, :3] * 16.0
    return (
        float(pts[:, 0].min()),
        float(pts[:, 0].max()),
        float(pts[:, 1].min()),
        float(pts[:, 1].max()),
        float(pts[:, 2].min()),
        float(pts[:, 2].max()),
    )


def aabb_overlap_volume(
    a: Tuple[float, float, float, float, float, float],
    b: Tuple[float, float, float, float, float, float],
    eps: float = 1e-4,
) -> float:
    ox = max(0.0, min(a[1], b[1]) - max(a[0], b[0]))
    oy = max(0.0, min(a[3], b[3]) - max(a[2], b[2]))
    oz = max(0.0, min(a[5], b[5]) - max(a[4], b[4]))
    if ox <= eps or oy <= eps or oz <= eps:
        return 0.0
    return ox * oy * oz


def compute_bone_world_matrices(geo: GeoModel, visor_t: float = 0.0) -> Dict[str, np.ndarray]:
    """Compute world-space 4x4 transform matrix per bone name (in block units, compatible with compute_cube_transformed_aabb)."""
    matrices: Dict[str, np.ndarray] = {}

    def walk(bone: BoneSpec, parent_mat: np.ndarray):
        mat = parent_mat @ _bone_local_matrix(bone, visor_t=visor_t)
        matrices[bone.name] = mat
        for child in bone.children:
            walk(child, mat)

    ident = np.eye(4, dtype=np.float64)
    for tb in geo.top_bones:
        walk(tb, ident)
    return matrices


def cubes_zfight_or_overlap(
    a: Tuple[float, float, float, float, float, float],
    b: Tuple[float, float, float, float, float, float],
) -> Optional[str]:
    """
    Detect either 3D volumetric intersection (> 1e-4) OR same-facing coplanar face overlap
    (plane shared within 1e-4 with 2D overlap > 1e-3 on the other two axes).
    Opposite-facing flush attachment contact (a_min == b_max or a_max == b_min) is allowed.
    """
    vol = aabb_overlap_volume(a, b, eps=1e-4)
    if vol > 1e-4:
        return f"3D overlap vol={vol:.5f} between {a} and {b}"

    for axis in range(3):
        o1, o2 = (axis + 1) % 3, (axis + 2) % 3
        ox = max(0.0, min(a[2 * o1 + 1], b[2 * o1 + 1]) - max(a[2 * o1], b[2 * o1]))
        oy = max(0.0, min(a[2 * o2 + 1], b[2 * o2 + 1]) - max(a[2 * o2], b[2 * o2]))
        if ox > 1e-3 and oy > 1e-3:
            for side in (0, 1):
                pa = a[2 * axis + side]
                pb = b[2 * axis + side]
                if abs(pa - pb) < 1e-4:
                    return (
                        f"Coplanar same-facing z-fighting on axis={axis} side={side} "
                        f"plane={pa:.4f} 2D_overlap=({ox:.3f}x{oy:.3f})"
                    )
    return None


def check_bone_steve_head_overlap(
    geo: GeoModel,
    bone_filter: Optional[Callable[[str], bool]] = None,
    visor_t: float = 0.0,
) -> List[Tuple[str, float]]:
    """Return [(bone_name, overlap_vol)] for cubes strictly penetrating Steve head [-4..4, 24..32, -4..4]."""
    overlaps: List[Tuple[str, float]] = []

    def walk(bone: BoneSpec, parent_mat: np.ndarray):
        if bone_filter is not None and not bone_filter(bone.name):
            return
        mat = parent_mat @ _bone_local_matrix(bone, visor_t=visor_t)
        for cube in bone.cubes:
            aabb = compute_cube_transformed_aabb(cube, mat)
            vol = aabb_overlap_volume(aabb, STEVE_HEAD_BOUNDS, eps=1e-3)
            if vol > 1e-4:
                overlaps.append((bone.name, vol))
        for child in bone.children:
            walk(child, mat)

    ident = np.eye(4, dtype=np.float64)
    for tb in geo.top_bones:
        walk(tb, ident)
    return overlaps


def _bone_local_matrix(bone: BoneSpec, visor_t: float = 0.0) -> np.ndarray:
    rx, ry, rz = bone.rotation_deg
    # GeckoLib converts Bedrock bone rotation [-rx, -ry, rz]
    rot_x = math.radians(-rx)
    rot_y = math.radians(-ry)
    rot_z = math.radians(rz)
    pos_x, pos_y, pos_z = 0.0, 0.0, 0.0

    if bone.name in ("visor_pivot", "visor"):
        pos_y = visor_t * 2.0
        pos_z = visor_t * -0.75
        rot_x = visor_t * math.radians(60.0)
        rot_y = 0.0
        rot_z = 0.0

    px, py, pz = [v / 16.0 for v in bone.pivot]
    return (
        _translate(-pos_x / 16.0, pos_y / 16.0, pos_z / 16.0)
        @ _translate(px, py, pz)
        @ _rot_z(rot_z)
        @ _rot_y(rot_y)
        @ _rot_x(rot_x)
        @ _translate(-px, -py, -pz)
    )


def collect_triangles(
    geo: GeoModel,
    model_matrix: np.ndarray,
    visor_t: float = 0.0,
    visible_fn: Optional[Callable[[str], bool]] = None,
    root_bone_name: Optional[str] = None,
) -> List[Tuple[np.ndarray, np.ndarray, np.ndarray, str, str]]:
    """
    Traverse GeoModel bones and return list of
    (tri_verts_3x3, tri_uvs_3x2, face_normal_3, bone_name, face_name).
    """
    tris: List[Tuple[np.ndarray, np.ndarray, np.ndarray, str, str]] = []

    def walk(bone: BoneSpec, parent_mat: np.ndarray, active: bool):
        if visible_fn is not None and not visible_fn(bone.name):
            return
        mat = parent_mat @ _bone_local_matrix(bone, visor_t=visor_t)
        emit_here = active or (root_bone_name is None) or (bone.name == root_bone_name)
        if emit_here:
            for cube in bone.cubes:
                cmat = mat @ _cube_world_matrix(cube)
                for fname, corners, uvs in _cube_faces(cube, geo.tex_w, geo.tex_h):
                    pts = []
                    for cx, cy, cz in corners:
                        v = cmat @ np.array([cx, cy, cz, 1.0], dtype=np.float64)
                        pts.append(v[:3])
                    p0, p1, p2, p3 = pts
                    e1 = p1 - p0
                    e2 = p2 - p0
                    n = np.cross(e1, e2)
                    nlen = np.linalg.norm(n)
                    if nlen < 1e-10:
                        continue
                    n = n / nlen
                    uv0, uv1, uv2, uv3 = uvs
                    tris.append(
                        (
                            np.array([p0, p1, p2], dtype=np.float64),
                            np.array([uv0, uv1, uv2], dtype=np.float64),
                            n,
                            bone.name,
                            fname,
                        )
                    )
                    tris.append(
                        (
                            np.array([p0, p2, p3], dtype=np.float64),
                            np.array([uv0, uv2, uv3], dtype=np.float64),
                            n,
                            bone.name,
                            fname,
                        )
                    )
        for child in bone.children:
            walk(child, mat, emit_here)

    for tb in geo.top_bones:
        walk(tb, model_matrix, root_bone_name is None)
    return tris


def _steve_head_triangles(model_matrix: np.ndarray) -> List[Tuple[np.ndarray, np.ndarray, np.ndarray]]:
    """Build shaded Steve head box [-4..4, 24..32, -4..4] + eye-line strip at Y=28, Z=-4.03."""
    x0, x1 = -4.0 / 16.0, 4.0 / 16.0
    y0, y1 = 24.0 / 16.0, 32.0 / 16.0
    z0, z1 = -4.0 / 16.0, 4.0 / 16.0
    faces = [
        ((x1, y1, z0), (x0, y1, z0), (x0, y0, z0), (x1, y0, z0), (175, 138, 108)),  # face front
        ((x0, y1, z1), (x1, y1, z1), (x1, y0, z1), (x0, y0, z1), (138, 104, 78)),
        ((x0, y1, z0), (x0, y1, z1), (x0, y0, z1), (x0, y0, z0), (146, 112, 86)),
        ((x1, y1, z1), (x1, y1, z0), (x1, y0, z0), (x1, y0, z1), (146, 112, 86)),
        ((x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1), (120, 88, 62)),   # hair top
        ((x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0), (130, 96, 70)),
        # Eye-line strip at Y=28 (27.6..28.4) on front face
        ((x1 * 0.85, 28.35 / 16.0, z0 - 0.002), (x0 * 0.85, 28.35 / 16.0, z0 - 0.002),
         (x0 * 0.85, 27.65 / 16.0, z0 - 0.002), (x1 * 0.85, 27.65 / 16.0, z0 - 0.002), (90, 210, 120)),
    ]
    out = []
    for c0, c1, c2, c3, rgb in faces:
        pts = [(model_matrix @ np.array([c[0], c[1], c[2], 1.0]))[:3] for c in (c0, c1, c2, c3)]
        n = np.cross(pts[1] - pts[0], pts[2] - pts[0])
        n = n / (np.linalg.norm(n) + 1e-12)
        col = np.array(rgb, dtype=np.float64)
        out.append((np.array([pts[0], pts[1], pts[2]]), n, col))
        out.append((np.array([pts[0], pts[2], pts[3]]), n, col))
    return out


@dataclass
class RenderResult:
    image: Image.Image
    mask: np.ndarray          # bool (H, W) goggle pixels
    glow_pixels: int          # lit glow pixels rendered
    front_lens_pixels: int    # rendered pixels whose UV lies in GLOW_ALLOWED_RECTS
    bbox: Optional[Tuple[int, int, int, int]]  # xmin, ymin, xmax, ymax
    viewport_coverage: float
    bbox_w_frac: float
    bbox_h_frac: float
    bone_pixel_counts: Dict[str, int]


def rasterize_scene(
    width: int,
    height: int,
    proj_matrix: np.ndarray,
    view_matrix: np.ndarray,
    geo_tris: List[Tuple[np.ndarray, np.ndarray, np.ndarray, str, str]],
    base_tex: np.ndarray,
    glow_tex: Optional[np.ndarray] = None,
    glow_intensity: float = 1.0,
    steve_tris: Optional[List[Tuple[np.ndarray, np.ndarray, np.ndarray]]] = None,
    backface_cull: bool = False,
    y_down_screen: bool = False,
    bg_rgba: Tuple[int, int, int, int] = (24, 28, 34, 255),
) -> RenderResult:
    """
    Z-buffered triangle rasterizer with perspective-correct / affine screen UV sampling.
    """
    fb = np.zeros((height, width, 4), dtype=np.uint8)
    fb[:, :] = bg_rgba
    zbuf = np.full((height, width), np.inf, dtype=np.float64)
    goggle_mask = np.zeros((height, width), dtype=bool)
    uv_hit_img = np.zeros((height, width, 2), dtype=np.int32) - 1
    bone_id_img = np.zeros((height, width), dtype=np.int32) - 1

    tex_h, tex_w = base_tex.shape[0], base_tex.shape[1]
    vp = proj_matrix @ view_matrix
    light_dir = np.array([0.35, 0.72, -0.60], dtype=np.float64)
    light_dir /= np.linalg.norm(light_dir)

    def project_pts(pts3: np.ndarray):
        homo = np.hstack([pts3, np.ones((3, 1), dtype=np.float64)])
        clip = (vp @ homo.T).T
        w = clip[:, 3:4]
        # Reject triangles behind near plane in perspective
        if np.any(w <= 1e-5):
            return None
        ndc = clip[:, :3] / w
        sx = (ndc[:, 0] * 0.5 + 0.5) * width
        if y_down_screen:
            sy = (ndc[:, 1] * 0.5 + 0.5) * height
        else:
            sy = (0.5 - ndc[:, 1] * 0.5) * height
        sz = ndc[:, 2]
        return sx, sy, sz, w[:, 0]

    # 1. Render optional Steve head guide
    if steve_tris:
        for pts3, normal, rgb in steve_tris:
            res = project_pts(pts3)
            if res is None:
                continue
            sx, sy, sz, _ = res
            area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sy[1] - sy[0]) * (sx[2] - sx[0])
            if abs(area) < 1e-4:
                continue
            x0 = max(0, int(math.floor(min(sx))))
            x1 = min(width - 1, int(math.ceil(max(sx))))
            y0 = max(0, int(math.floor(min(sy))))
            y1 = min(height - 1, int(math.ceil(max(sy))))
            if x1 < x0 or y1 < y0:
                continue
            xs = np.arange(x0, x1 + 1, dtype=np.float64) + 0.5
            ys = np.arange(y0, y1 + 1, dtype=np.float64) + 0.5
            gx, gy = np.meshgrid(xs, ys)
            w0 = ((sx[1] - gx) * (sy[2] - gy) - (sy[1] - gy) * (sx[2] - gx)) / area
            w1 = ((sx[2] - gx) * (sy[0] - gy) - (sy[2] - gy) * (sx[0] - gx)) / area
            w2 = 1.0 - w0 - w1
            inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
            z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2]
            sub_z = zbuf[y0 : y1 + 1, x0 : x1 + 1]
            vis = inside & (z < sub_z) & (z >= -1.0) & (z <= 1.0)
            if not np.any(vis):
                continue
            sub_z[vis] = z[vis]
            shade = 0.50 + 0.50 * max(0.0, float(np.dot(normal, light_dir)))
            shaded = np.clip(rgb * shade, 0, 255).astype(np.uint8)
            sub_fb = fb[y0 : y1 + 1, x0 : x1 + 1]
            sub_fb[vis, :3] = shaded
            sub_fb[vis, 3] = 255

    # 2. Render goggle triangles
    bone_names: List[str] = []
    bone_idx_map: Dict[str, int] = {}
    glow_lit_mask = np.zeros((height, width), dtype=bool)
    front_facing_hit = np.zeros((height, width), dtype=bool)

    for pts3, uvs, normal, bname, _fname in geo_tris:
        res = project_pts(pts3)
        if res is None:
            continue
        sx, sy, sz, cw = res
        area = (sx[1] - sx[0]) * (sy[2] - sy[0]) - (sy[1] - sy[0]) * (sx[2] - sx[0])
        if abs(area) < 1e-4:
            continue
        if backface_cull:
            if (not y_down_screen and area > 0) or (y_down_screen and area < 0):
                continue

        x0 = max(0, int(math.floor(min(sx))))
        x1 = min(width - 1, int(math.ceil(max(sx))))
        y0 = max(0, int(math.floor(min(sy))))
        y1 = min(height - 1, int(math.ceil(max(sy))))
        if x1 < x0 or y1 < y0:
            continue

        if bname not in bone_idx_map:
            bone_idx_map[bname] = len(bone_names)
            bone_names.append(bname)
        b_idx = bone_idx_map[bname]

        xs = np.arange(x0, x1 + 1, dtype=np.float64) + 0.5
        ys = np.arange(y0, y1 + 1, dtype=np.float64) + 0.5
        gx, gy = np.meshgrid(xs, ys)
        w0 = ((sx[1] - gx) * (sy[2] - gy) - (sy[1] - gy) * (sx[2] - gx)) / area
        w1 = ((sx[2] - gx) * (sy[0] - gy) - (sy[2] - gy) * (sx[0] - gx)) / area
        w2 = 1.0 - w0 - w1
        inside = (w0 >= -1e-4) & (w1 >= -1e-4) & (w2 >= -1e-4)
        if not np.any(inside):
            continue

        z = w0 * sz[0] + w1 * sz[1] + w2 * sz[2]
        sub_z = zbuf[y0 : y1 + 1, x0 : x1 + 1]
        vis = inside & (z < sub_z) & (z >= -1.0) & (z <= 1.0)
        if not np.any(vis):
            continue

        inv_w = w0 / cw[0] + w1 / cw[1] + w2 / cw[2]
        u = (w0 * (uvs[0, 0] / cw[0]) + w1 * (uvs[1, 0] / cw[1]) + w2 * (uvs[2, 0] / cw[2])) / inv_w
        v = (w0 * (uvs[0, 1] / cw[0]) + w1 * (uvs[1, 1] / cw[1]) + w2 * (uvs[2, 1] / cw[2])) / inv_w

        tx = np.clip(np.floor(u * tex_w).astype(np.int32), 0, tex_w - 1)
        ty = np.clip(np.floor(v * tex_h).astype(np.int32), 0, tex_h - 1)

        texels = base_tex[ty, tx]
        opaque = vis & (texels[:, :, 3] > 10)
        if not np.any(opaque):
            continue

        sub_z[opaque] = z[opaque]
        vnorm = view_matrix[:3, :3] @ normal
        shade = 0.50 + 0.50 * abs(float(np.dot(normal, light_dir)))
        rgb = np.clip(texels[:, :, :3].astype(np.float64) * shade, 0, 255).astype(np.uint8)

        sub_fb = fb[y0 : y1 + 1, x0 : x1 + 1]
        sub_fb[opaque, :3] = rgb[opaque]
        sub_fb[opaque, 3] = 255

        if glow_tex is not None and glow_intensity > 0.01:
            g_texels = glow_tex[ty, tx]
            g_vis = opaque & (g_texels[:, :, 3] > 10)
            if np.any(g_vis):
                g_rgb = np.clip(
                    g_texels[:, :, :3].astype(np.float64) * glow_intensity, 0, 255
                ).astype(np.uint8)
                sub_fb[g_vis, :3] = np.maximum(sub_fb[g_vis, :3], g_rgb[g_vis])
                glow_lit_mask[y0 : y1 + 1, x0 : x1 + 1][g_vis] = True

        goggle_mask[y0 : y1 + 1, x0 : x1 + 1][opaque] = True
        front_facing_hit[y0 : y1 + 1, x0 : x1 + 1][opaque] = bool(vnorm[2] > 0.10)
        uv_hit_img[y0 : y1 + 1, x0 : x1 + 1, 0][opaque] = tx[opaque]
        uv_hit_img[y0 : y1 + 1, x0 : x1 + 1, 1][opaque] = ty[opaque]
        bone_id_img[y0 : y1 + 1, x0 : x1 + 1][opaque] = b_idx

    # Metrics
    ys, xs = np.where(goggle_mask)
    total_px = float(width * height)
    if len(xs) == 0:
        bbox = None
        cov = 0.0
        w_frac = 0.0
        h_frac = 0.0
        front_lens = 0
    else:
        xmin, xmax = int(xs.min()), int(xs.max())
        ymin, ymax = int(ys.min()), int(ys.max())
        bbox = (xmin, ymin, xmax, ymax)
        cov = len(xs) / total_px
        w_frac = (xmax - xmin + 1) / float(width)
        h_frac = (ymax - ymin + 1) / float(height)
        tx_hits = uv_hit_img[ys, xs, 0].tolist()
        ty_hits = uv_hit_img[ys, xs, 1].tolist()
        ff_hits = front_facing_hit[ys, xs].tolist()
        front_lens = sum(
            1 for ux, uy, ff in zip(tx_hits, ty_hits, ff_hits)
            if ff and is_in_glow_rects(ux, uy)
        )

    bone_counts: Dict[str, int] = {}
    for bname, idx in bone_idx_map.items():
        cnt = int(np.sum(bone_id_img == idx))
        if cnt > 0:
            bone_counts[bname] = cnt

    return RenderResult(
        image=Image.fromarray(fb, mode="RGBA"),
        mask=goggle_mask,
        glow_pixels=int(np.sum(glow_lit_mask)),
        front_lens_pixels=front_lens,
        bbox=bbox,
        viewport_coverage=round(cov, 4),
        bbox_w_frac=round(w_frac, 4),
        bbox_h_frac=round(h_frac, 4),
        bone_pixel_counts=bone_counts,
    )


def render_f5_head(
    geo: GeoModel,
    base_tex: np.ndarray,
    glow_tex: Optional[np.ndarray],
    visor_t: float = 0.0,
    yaw_deg: float = -35.0,
    pitch_deg: float = 15.0,
    visible_fn: Optional[Callable[[str], bool]] = None,
    size: int = 240,
    include_steve_head: bool = True,
) -> RenderResult:
    """Context 1: Third-person F5 head mount in Bedrock Y-up coordinates."""
    ident = np.eye(4, dtype=np.float64)
    tris = collect_triangles(geo, ident, visor_t=visor_t, visible_fn=visible_fn)
    steve = _steve_head_triangles(ident) if include_steve_head else None

    target = np.array([0.0, 28.0 / 16.0, -2.5 / 16.0], dtype=np.float64)
    dist = 1.75
    yr = math.radians(yaw_deg)
    pr = math.radians(pitch_deg)
    # Camera orbits looking at -Z face when yaw=0
    eye = target + dist * np.array(
        [math.sin(yr) * math.cos(pr), math.sin(pr), -math.cos(yr) * math.cos(pr)],
        dtype=np.float64,
    )
    view = _look_at(eye, target, np.array([0.0, 1.0, 0.0]))
    proj = _perspective(38.0, 1.0, 0.1, 20.0)
    return rasterize_scene(
        size, size, proj, view, tris, base_tex,
        glow_tex=glow_tex if visor_t <= 0.05 else None,
        glow_intensity=1.0,
        steve_tris=steve,
    )


def render_item_view(
    geo: GeoModel,
    base_tex: np.ndarray,
    glow_tex: Optional[np.ndarray],
    display_transform: Dict,
    visible_fn: Optional[Callable[[str], bool]] = None,
    size: int = 240,
    extra_yaw_deg: float = 25.0,
    extra_pitch_deg: float = 18.0,
    apply_item_centering: bool = True,
    visor_t: float = 0.0,
) -> RenderResult:
    """Context 4: Item / GUI / ModBench FIXED display transform + (0, -29/16, +2.75/16) centering."""
    m = np.eye(4, dtype=np.float64)
    if apply_item_centering:
        m = _translate(0.0, -29.0 / 16.0, 2.75 / 16.0) @ m

    # Apply models/item/*.json display transform: translate -> rotate -> scale
    trans = display_transform.get("translation", [0.0, 0.0, 0.0])
    rot = display_transform.get("rotation", [0.0, 0.0, 0.0])
    sc = display_transform.get("scale", [1.0, 1.0, 1.0])
    d_mat = (
        _translate(trans[0] / 16.0, trans[1] / 16.0, trans[2] / 16.0)
        @ _rot_z(math.radians(rot[2]))
        @ _rot_y(math.radians(rot[1]))
        @ _rot_x(math.radians(rot[0]))
        @ _scale(sc[0], sc[1], sc[2])
    )
    m = d_mat @ m

    tris = collect_triangles(geo, m, visor_t=visor_t, visible_fn=visible_fn)
    target = np.array([0.0, 0.0, 0.0], dtype=np.float64)
    yr = math.radians(extra_yaw_deg)
    pr = math.radians(extra_pitch_deg)
    dist = 1.85
    eye = dist * np.array(
        [math.sin(yr) * math.cos(pr), math.sin(pr), math.cos(yr) * math.cos(pr)],
        dtype=np.float64,
    )
    view = _look_at(eye, target, np.array([0.0, 1.0, 0.0]))
    proj = _perspective(36.0, 1.0, 0.1, 20.0)
    return rasterize_scene(
        size, size, proj, view, tris, base_tex,
        glow_tex=glow_tex, glow_intensity=1.0,
    )


def render_fp_equip_frame(
    geo: GeoModel,
    base_tex: np.ndarray,
    glow_tex: Optional[np.ndarray],
    display_fp: Dict,
    t: float,
    visible_fn: Optional[Callable[[str], bool]] = None,
    width: int = 320,
    height: int = 180,
) -> RenderResult:
    """Context 3: FirstPersonEquipRenderer trajectory + firstperson_righthand + item centering."""
    def smoothstep01(v: float) -> float:
        c = max(0.0, min(1.0, v))
        return c * c * (3.0 - 2.0 * c)

    p1 = smoothstep01(t / 0.38)
    p2 = smoothstep01((t - 0.34) / 0.54)
    p3 = smoothstep01((t - 0.86) / 0.14)

    idle_x, idle_y, idle_z = 0.56, -0.52, -0.72
    x = idle_x * (1.0 - p1)
    y = (idle_y * (1.0 - p1) + (-0.24) * p1) + 0.68 * p2 - 0.05 * math.sin(p3 * math.pi)
    z = (idle_z * (1.0 - p1) + (-1.14) * p1) + 0.24 * p2 - 0.05 * math.sin(p2 * math.pi)

    rot_x = -34.0 * p1 - 18.0 * p2 + 4.0 * math.sin(p3 * math.pi)
    rot_y = 6.0 * p1 * (1.0 - p2)
    rot_z = -5.0 * p1 * (1.0 - p2)
    visual_pivot_z = 9.0 / 16.0

    # Item centering + display_fp
    trans = display_fp.get("translation", [0.0, 0.0, 0.0])
    d_rot = display_fp.get("rotation", [0.0, 0.0, 0.0])
    d_sc = display_fp.get("scale", [1.0, 1.0, 1.0])

    item_mat = (
        _translate(trans[0] / 16.0, trans[1] / 16.0, trans[2] / 16.0)
        @ _rot_z(math.radians(d_rot[2]))
        @ _rot_y(math.radians(d_rot[1]))
        @ _rot_x(math.radians(d_rot[0]))
        @ _scale(d_sc[0], d_sc[1], d_sc[2])
        @ _translate(0.0, -29.0 / 16.0, 2.75 / 16.0)
    )
    equip_mat = (
        _translate(x, y, z + visual_pivot_z)
        @ _rot_x(math.radians(rot_x))
        @ _rot_y(math.radians(rot_y))
        @ _rot_z(math.radians(rot_z))
        @ _translate(0.0, 0.0, -visual_pivot_z)
        @ item_mat
    )
    tris = collect_triangles(geo, equip_mat, visor_t=0.0, visible_fn=visible_fn)
    view = np.eye(4, dtype=np.float64)
    proj = _perspective(70.0, width / float(height), 0.05, 50.0)
    return rasterize_scene(
        width, height, proj, view, tris, base_tex,
        glow_tex=glow_tex, glow_intensity=1.0,
    )


def parse_fp_visor_params(java_text: str) -> Dict:
    """Extract actual transform contract parameters from FirstPersonVisorRenderer (+ helpers)."""
    clean = strip_java_comments(java_text)
    code = strip_java_comments_and_strings(java_text)
    uses_legacy_model = "FirstPersonVisorModel" in code
    uses_baked_geo = (
        ("BakedGeoModel" in code)
        or ("GeckoLibCache" in code)
        or ("VisionGogglesGeoModel" in code)
        or ("VisionGogglesGeoRenderer" in code)
    )
    cancels_y_flip = bool(
        re.search(r"\bscale\s*\(\s*1(?:\.0+)?[fF]?\s*,\s*-1(?:\.0+)?[fF]?\s*,\s*1(?:\.0+)?[fF]?\s*\)", code)
        or re.search(r"\bscale\s*\(\s*(?!-)[^,)]+\s*,\s*-[^,)]+\s*,\s*(?!-)[^,)]+\s*\)", code)
    )
    uses_visor_pivot_name = '"visor_pivot"' in clean

    # Extract simple local numeric variables & expressions for local var resolution
    local_floats: Dict[str, float] = {}
    for m in re.finditer(r"\b(?:final\s+)?(?:float|double)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*([+-]?[0-9]+(?:\.[0-9]+)?)[fFdD]?\s*;", code):
        local_floats[m.group(1)] = float(m.group(2))
    local_exprs: Dict[str, str] = {}
    for m in re.finditer(r"\b(?:final\s+)?(?:float|double)\s+([A-Za-z_][A-Za-z0-9_]*)\s*=\s*([^;]+);", code):
        local_exprs[m.group(1)] = m.group(2)

    def _resolve_num(tok: str) -> Optional[float]:
        t = tok.strip()
        m_num = re.fullmatch(r"([+-]?[0-9]+(?:\.[0-9]+)?)[fFdD]?", t)
        if m_num:
            return float(m_num.group(1))
        return local_floats.get(t)

    # Check scale multipliers: Mth.lerp(...) OR direct height * <const>[fF]?
    min_scale_mult: Optional[float] = None
    max_scale_mult: Optional[float] = None
    scale_parsed = False

    scale_lerp = re.search(
        r"\b(?:visor[Ss]cale|scale)\s*=\s*height\s*\*\s*Mth\.lerp\s*\([^,]+,\s*([^,]+?)\s*,\s*([^)]+?)\s*\)",
        code,
    ) or re.search(
        r"\b(?:visor[Ss]cale|scale)\s*=\s*Mth\.lerp\s*\([^,]+,\s*([^,]+?)\s*,\s*([^)]+?)\s*\)\s*\*\s*height",
        code,
    )
    if scale_lerp:
        v1 = _resolve_num(scale_lerp.group(1))
        v2 = _resolve_num(scale_lerp.group(2))
        if v1 is not None and v2 is not None:
            min_scale_mult, max_scale_mult = v1, v2
            scale_parsed = True
    else:
        scale_const = re.search(
            r"\b(?:visor[Ss]cale|scale)\s*=\s*height\s*\*\s*([+-]?[0-9]+(?:\.[0-9]+)?[fFdD]?|[A-Za-z_][A-Za-z0-9_]*)\s*;",
            code,
        ) or re.search(
            r"\b(?:visor[Ss]cale|scale)\s*=\s*([+-]?[0-9]+(?:\.[0-9]+)?[fFdD]?|[A-Za-z_][A-Za-z0-9_]*)\s*\*\s*height\s*;",
            code,
        )
        if scale_const:
            v = _resolve_num(scale_const.group(1))
            if v is not None:
                min_scale_mult = v
                max_scale_mult = v
                scale_parsed = True

    # Check gripScale
    grip_min: Optional[float] = None
    grip_max: Optional[float] = None
    grip_lerp = re.search(
        r"\b(?:arm[Gg]rip[Ss]cale|grip[Ss]cale)\s*=\s*height\s*\*\s*Mth\.lerp\s*\([^,]+,\s*([^,]+?)\s*,\s*([^)]+?)\s*\)",
        code,
    )
    if grip_lerp:
        g1 = _resolve_num(grip_lerp.group(1))
        g2 = _resolve_num(grip_lerp.group(2))
        if g1 is not None and g2 is not None:
            grip_min, grip_max = g1, g2
    else:
        grip_const = re.search(
            r"\b(?:arm[Gg]rip[Ss]cale|grip[Ss]cale)\s*=\s*height\s*\*\s*([+-]?[0-9]+(?:\.[0-9]+)?[fFdD]?|[A-Za-z_][A-Za-z0-9_]*)\s*;",
            code,
        )
        if grip_const:
            gv = _resolve_num(grip_const.group(1))
            if gv is not None:
                grip_min = gv
                grip_max = gv

    # Check Y rotation (front-facing ~180 deg inline or via local yaw variable)
    has_180_yaw = False
    for ym in re.finditer(r"YP\.rotationDegrees\s*\(([^;{}]+)\)", code):
        arg = ym.group(1)
        if re.search(r"\b180(?:\.0+)?[fFdD]?\b", arg):
            has_180_yaw = True
            break
        for ident in re.findall(r"\b([A-Za-z_][A-Za-z0-9_]*)\b", arg):
            expr = local_exprs.get(ident, "")
            if re.search(r"\b180(?:\.0+)?[fFdD]?\b", expr):
                has_180_yaw = True
                break
        if has_180_yaw:
            break
    yaw_base_deg = 180.0 if has_180_yaw else 0.0

    # Check pitch
    pitch_match = re.search(r"XP\.rotationDegrees\s*\(\s*([+-]?[0-9]+(?:\.[0-9]+)?)[fF]?", code)
    pitch_deg = float(pitch_match.group(1)) if pitch_match else 14.0

    # Check yOffset lerp
    y_match = re.search(
        r"yOffset\s*=\s*height\s*\*\s*\(\s*Mth\.lerp\([^,]+,\s*([+-]?[0-9]+(?:\.[0-9]+)?)[fF]?\s*,\s*([+-]?[0-9]+(?:\.[0-9]+)?)[fF]?\)\s*\+\s*([+-]?[0-9]+(?:\.[0-9]+)?)[fF]?",
        code,
    )
    if y_match:
        y_start, y_brow, y_drop = float(y_match.group(1)), float(y_match.group(2)), float(y_match.group(3))
    else:
        y_start, y_brow, y_drop = -0.70, -0.46, 0.38

    return {
        "uses_legacy_model": uses_legacy_model,
        "uses_baked_geo": uses_baked_geo,
        "cancels_y_flip": cancels_y_flip,
        "uses_visor_pivot_name": uses_visor_pivot_name,
        "scale_parsed": scale_parsed,
        "min_scale_mult": min_scale_mult,
        "max_scale_mult": max_scale_mult,
        "grip_min_mult": grip_min,
        "grip_max_mult": grip_max,
        "has_180_yaw": has_180_yaw,
        "yaw_base_deg": yaw_base_deg,
        "pitch_deg": pitch_deg,
        "y_start": y_start,
        "y_brow": y_brow,
        "y_drop": y_drop,
    }


def _ortho_gui_matrix(width: float, height: float, z_near: float = -10000.0, z_far: float = 10000.0) -> np.ndarray:
    """Minecraft GuiGraphics ortho projection: (0,0) top-left, (width,height) bottom-right."""
    m = np.eye(4, dtype=np.float64)
    m[0, 0] = 2.0 / width
    m[0, 3] = -1.0
    m[1, 1] = -2.0 / height
    m[1, 3] = 1.0
    m[2, 2] = -2.0 / (z_far - z_near)
    m[2, 3] = -(z_far + z_near) / (z_far - z_near)
    return m


def render_fp_flip_gui(
    geo: GeoModel,
    base_tex: np.ndarray,
    glow_tex: Optional[np.ndarray],
    fp_params: Dict,
    raw_t: float,
    width: int = 480,
    height: int = 270,
    visible_fn: Optional[Callable[[str], bool]] = None,
) -> RenderResult:
    """
    Context 2: Simulate FirstPersonVisorRenderer GuiGraphics orthographic projection
    using actual parsed transform parameters from FirstPersonVisorRenderer.java.
    """
    reach_end = 0.28
    visor_end = 0.84

    def smoothstep01(v: float) -> float:
        c = max(0.0, min(1.0, v))
        return c * c * (3.0 - 2.0 * c)

    if raw_t <= reach_end:
        visor_progress = 0.0
    elif raw_t >= visor_end:
        visor_progress = 1.0
    else:
        visor_progress = smoothstep01((raw_t - reach_end) / (visor_end - reach_end))

    brow_presence = smoothstep01(raw_t / reach_end)
    ez = smoothstep01(visor_progress)
    eye_zoom = 0.40 * ez + 0.60 * (visor_progress * ez)

    if not fp_params.get("scale_parsed", True) or fp_params.get("min_scale_mult") is None or fp_params.get("max_scale_mult") is None:
        raise AssertionError("FirstPersonVisorRenderer scale pattern could not be determined")

    scale = height * (fp_params["min_scale_mult"] + eye_zoom * (fp_params["max_scale_mult"] - fp_params["min_scale_mult"]))
    y_offset = height * (
        fp_params["y_start"] + brow_presence * (fp_params["y_brow"] - fp_params["y_start"]) + fp_params["y_drop"] * ez
    )
    z_arc = math.sin(visor_progress * math.pi) * 35.0

    # PoseStack in GuiGraphics space (+X right, +Y down)
    pose = (
        _translate(width * 0.5, height * 0.5 + y_offset, 1800.0 + z_arc)
        @ _scale(scale, scale, scale)
        @ _rot_y(math.radians(fp_params["yaw_base_deg"] + 6.0 * (1.0 - visor_progress * 0.5)))
        @ _rot_x(math.radians(fp_params["pitch_deg"]))
    )

    if fp_params["cancels_y_flip"]:
        pose = pose @ _scale(1.0, -1.0, 1.0)

    visor_bone_name = "visor_pivot" if "visor_pivot" in geo.bones_by_name else "visor"
    v_bone = geo.bones_by_name.get(visor_bone_name)
    if v_bone is not None:
        px, py, pz = v_bone.pivot
        pose = pose @ _translate(-px / 16.0, -py / 16.0, -pz / 16.0)

    visor_t = 1.0 - visor_progress
    # Subtract parent head pivot contribution by extracting triangles directly from visor_bone
    ident = np.eye(4, dtype=np.float64)
    tris_local = collect_triangles(
        geo, ident, visor_t=visor_t, visible_fn=visible_fn, root_bone_name=visor_bone_name
    )
    # Transform local triangles by pose
    tris = []
    rot3 = pose[:3, :3]
    for pts3, uvs, n, bname, fname in tris_local:
        homo = np.hstack([pts3, np.ones((3, 1), dtype=np.float64)])
        tpts = (pose @ homo.T).T[:, :3]
        tn = rot3 @ n
        tn_len = np.linalg.norm(tn)
        if tn_len > 1e-10:
            tn = tn / tn_len
        tris.append((tpts, uvs, tn, bname, fname))

    proj = _ortho_gui_matrix(float(width), float(height))
    view = np.eye(4, dtype=np.float64)
    glow_int = smoothstep01((visor_progress - 0.60) / 0.28)
    return rasterize_scene(
        width,
        height,
        proj,
        view,
        tris,
        base_tex,
        glow_tex=glow_tex,
        glow_intensity=glow_int,
        backface_cull=False,
        y_down_screen=False,
    )


def make_modular_visibility_fn(
    modules: Sequence[str],
    is_pro: bool = False,
    neutral_chassis: bool = False,
) -> Callable[[str], bool]:
    from .common_utils import resolve_java_modular_nbt_rules

    vis_map = resolve_java_modular_nbt_rules(modules, is_pro=is_pro, neutral_chassis=neutral_chassis)

    def _pred(bone_name: str) -> bool:
        if bone_name in vis_map:
            return vis_map[bone_name]
        return True

    return _pred


def make_contact_sheet(
    tiles: Sequence[Tuple[Image.Image, str]],
    cols: int,
    out_path: Path,
    title: str = "",
) -> Path:
    if not tiles:
        raise AssertionError("Cannot create contact sheet from empty tile list")
    tile_w, tile_h = tiles[0][0].size
    pad = 8
    header_h = 26
    label_h = 20
    rows = math.ceil(len(tiles) / cols)
    sheet_w = cols * tile_w + (cols + 1) * pad
    sheet_h = header_h + rows * (tile_h + label_h + pad) + pad

    sheet = Image.new("RGBA", (sheet_w, sheet_h), (16, 19, 24, 255))
    draw = ImageDraw.Draw(sheet)
    if title:
        draw.text((pad, 6), title, fill=(220, 232, 245, 255))

    for idx, (im, label) in enumerate(tiles):
        r = idx // cols
        c = idx % cols
        x = pad + c * (tile_w + pad)
        y = header_h + r * (tile_h + label_h + pad)
        if im.size != (tile_w, tile_h):
            im = im.resize((tile_w, tile_h), Image.Resampling.NEAREST)
        sheet.paste(im.convert("RGBA"), (x, y))
        draw.rectangle([x, y, x + tile_w - 1, y + tile_h - 1], outline=(58, 68, 82, 255))
        draw.text((x + 4, y + tile_h + 3), label[:42], fill=(195, 210, 225, 255))

    out_path.parent.mkdir(parents=True, exist_ok=True)
    sheet.save(out_path, format="PNG")
    return out_path
