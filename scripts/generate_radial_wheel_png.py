"""Generate compact, semi-transparent pixel-art 256x256 GUI sprite sheet for TacticalRadialScreen."""
from pathlib import Path
from PIL import Image

OUT_PATH = Path("common/src/main/resources/assets/vision_goggles/textures/gui/radial_wheel.png")

# Semi-transparent gunmetal palette matching ModificationStationScreen
C_OUTLINE = (14, 16, 18, 245)       # #0E1012
C_BG_FILL = (38, 42, 49, 212)       # #262A31 semi-transparent
C_BEVEL_LT = (84, 91, 102, 235)     # #545B66
C_BEVEL_DK = (25, 28, 32, 235)      # #191C20
C_RIVET = (74, 82, 94, 240)

C_HUB_FILL = (20, 22, 26, 200)      # #14161A darker glass center
C_HUB_LT = (74, 80, 90, 230)        # #4A505A
C_HUB_DK = (14, 16, 18, 235)        # #0E1012

C_SLOT_FILL = (29, 32, 37, 235)     # #1D2025
C_SEL_FRAME_HI = (238, 242, 248, 255)
C_SEL_FRAME_MID = (168, 178, 192, 255)
C_SEL_SLOT_FILL = (50, 56, 66, 245)

C_DIS_FILL = (24, 27, 32, 210)
C_DIS_LT = (44, 48, 56, 220)
C_DIS_DK = (16, 18, 21, 225)


def octagon_dist(dx: float, dy: float) -> float:
    """Chebyshev-like octagonal distance (chamfered 45-deg Minecraft corners)."""
    ax = abs(dx)
    ay = abs(dy)
    # Regular octagon metric: max(ax, ay, (ax + ay) / sqrt(2))
    return max(ax, ay, (ax + ay) * 0.70710678)


def draw_wheel_base(img: Image.Image) -> None:
    """Draw compact 152x152 flat chamfered-octagon Minecraft panel at (0, 0) without bevel lines."""
    px = img.load()
    size = 152
    cx = size / 2.0
    cy = size / 2.0
    r_out = 75.0
    r_in = 28.0

    for y in range(size):
        for x in range(size):
            dx = (x + 0.5) - cx
            dy = (y + 0.5) - cy
            d = octagon_dist(dx, dy)

            if d > r_out:
                continue

            if d >= r_out - 1.0:
                px[x, y] = C_OUTLINE
            elif d > r_in:
                px[x, y] = C_BG_FILL
            else:
                px[x, y] = C_HUB_FILL


def draw_slot_sprite(img: Image.Image, ox: int, oy: int, selected: bool, disabled: bool) -> None:
    """Draw a 24x24 pixel-art Minecraft slot sprite with cut corners."""
    px = img.load()
    w, h = 24, 24

    for y in range(h):
        for x in range(w):
            if (x == 0 or x == w - 1) and (y == 0 or y == h - 1):
                continue

            if x == 0 or x == w - 1 or y == 0 or y == h - 1:
                px[ox + x, oy + y] = C_OUTLINE
                continue

            if selected:
                if x <= 2 or y <= 2 or x >= w - 3 or y >= h - 3:
                    is_tl = (x <= 2 and y < h - 2) or (y <= 2 and x < w - 2)
                    px[ox + x, oy + y] = C_SEL_FRAME_HI if is_tl else C_SEL_FRAME_MID
                else:
                    sx, sy = x - 3, y - 3
                    if (sx == 0 and sy == 17) or (sx == 17 and sy == 0):
                        px[ox + x, oy + y] = C_SEL_SLOT_FILL
                    elif sx == 0 or sy == 0:
                        px[ox + x, oy + y] = C_HUB_DK
                    elif sx == 17 or sy == 17:
                        px[ox + x, oy + y] = C_HUB_LT
                    else:
                        px[ox + x, oy + y] = C_SEL_SLOT_FILL
            else:
                lt = C_DIS_LT if disabled else C_BEVEL_LT
                dk = C_DIS_DK if disabled else C_BEVEL_DK
                slot_fill = C_DIS_FILL if disabled else C_SLOT_FILL

                if x <= 2 or y <= 2 or x >= w - 3 or y >= h - 3:
                    is_tl = (x + y < w - 1) and (x <= 2 or y <= 2)
                    px[ox + x, oy + y] = lt if is_tl else dk
                else:
                    sx, sy = x - 3, y - 3
                    if (sx == 0 and sy == 17) or (sx == 17 and sy == 0):
                        px[ox + x, oy + y] = slot_fill
                    elif sx == 0 or sy == 0:
                        px[ox + x, oy + y] = C_HUB_DK
                    elif sx == 17 or sy == 17:
                        px[ox + x, oy + y] = C_HUB_LT
                    else:
                        px[ox + x, oy + y] = slot_fill


def main() -> None:
    OUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    draw_wheel_base(img)
    draw_slot_sprite(img, 192, 0, selected=False, disabled=False)
    draw_slot_sprite(img, 192, 24, selected=True, disabled=False)
    draw_slot_sprite(img, 192, 48, selected=False, disabled=True)
    img.save(OUT_PATH)
    print(f"Generated {OUT_PATH}")


if __name__ == "__main__":
    main()
