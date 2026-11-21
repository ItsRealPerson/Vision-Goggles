"""Generate pixel-art 256x256 GUI sprite sheet for TacticalRadialScreen."""
from pathlib import Path
import math
from PIL import Image

OUT_PATH = Path("common/src/main/resources/assets/vision_goggles/textures/gui/radial_wheel.png")

# Palette matching ModificationStationScreen & vanilla Minecraft GUI bevel rules
C_OUTLINE = (14, 16, 18, 255)      # #0E1012
C_BG_FILL = (43, 47, 54, 255)      # #2B2F36
C_BEVEL_LT = (84, 91, 102, 255)    # #545B66
C_BEVEL_DK = (25, 28, 32, 255)     # #191C20
C_RIVET = (66, 72, 82, 255)        # #424852

C_HUB_FILL = (29, 32, 37, 255)     # #1D2025
C_HUB_LT = (74, 80, 90, 255)       # #4A505A
C_HUB_DK = (16, 18, 21, 255)       # #101215

C_SEL_FRAME_OUT = (14, 16, 18, 255)
C_SEL_FRAME_HI = (232, 238, 246, 255)
C_SEL_FRAME_MID = (168, 178, 192, 255)
C_SEL_SLOT_FILL = (46, 52, 61, 255)

C_DIS_FILL = (26, 29, 34, 255)
C_DIS_LT = (44, 48, 56, 255)
C_DIS_DK = (16, 18, 21, 255)


def draw_wheel_base(img: Image.Image) -> None:
    """Draw 192x192 pixel-art wheel at (0, 0) with top-left Minecraft GUI bevels."""
    px = img.load()
    cx, cy = 96, 96
    r_out = 92.0
    r_in = 34.0

    for y in range(192):
        for x in range(192):
            dx = x + 0.5 - cx
            dy = y + 0.5 - cy
            d = math.hypot(dx, dy)

            if d > r_out:
                continue

            # Top-left directional factor for classic Minecraft 3D pixel bevel
            tl_dot = -(dx + dy) / max(1.0, d)
            is_tl = tl_dot >= 0.0

            if d >= r_out - 1.2:
                px[x, y] = C_OUTLINE
            elif d >= r_out - 3.2:
                px[x, y] = C_BEVEL_LT if is_tl else C_BEVEL_DK
            elif d > r_in + 2.2:
                px[x, y] = C_BG_FILL
            elif d > r_in + 0.8:
                # Inner hub border is inset (dark top-left, light bottom-right)
                px[x, y] = C_HUB_DK if is_tl else C_HUB_LT
            elif d > r_in - 0.5:
                px[x, y] = C_OUTLINE
            else:
                px[x, y] = (29, 32, 37, 235)

    # 4 subtle pixel rivets at cardinal outer rim (matching ModificationStationScreen)
    for rx, ry in [(cx - 1, 7), (cx - 1, 183), (7, cy - 1), (183, cy - 1)]:
        for oy in range(2):
            for ox in range(2):
                px[rx + ox, ry + oy] = C_RIVET


def draw_slot_sprite(img: Image.Image, ox: int, oy: int, selected: bool, disabled: bool) -> None:
    """Draw a 24x24 pixel-art Minecraft slot sprite with cut corners."""
    px = img.load()
    w, h = 24, 24

    for y in range(h):
        for x in range(w):
            # Cut corners (1px step at corners)
            if (x == 0 or x == w - 1) and (y == 0 or y == h - 1):
                continue

            # Outer 1px black outline
            if x == 0 or x == w - 1 or y == 0 or y == h - 1:
                px[ox + x, oy + y] = C_OUTLINE
                continue

            if selected:
                # Hotbar-style 2px bright selection frame around an 18x18 slot
                if x <= 2 or y <= 2 or x >= w - 3 or y >= h - 3:
                    is_tl = (x <= 2 and y < h - 2) or (y <= 2 and x < w - 2)
                    px[ox + x, oy + y] = C_SEL_FRAME_HI if is_tl else C_SEL_FRAME_MID
                else:
                    # 18x18 recessed slot inside (x: 3..20, y: 3..20)
                    sx, sy = x - 3, y - 3
                    if sx == 0 and sy == 17 or sx == 17 and sy == 0:
                        px[ox + x, oy + y] = C_SEL_SLOT_FILL
                    elif sx == 0 or sy == 0:
                        px[ox + x, oy + y] = C_HUB_DK
                    elif sx == 17 or sy == 17:
                        px[ox + x, oy + y] = C_HUB_LT
                    else:
                        px[ox + x, oy + y] = C_SEL_SLOT_FILL
            else:
                body = C_DIS_FILL if disabled else C_BG_FILL
                lt = C_DIS_LT if disabled else C_BEVEL_LT
                dk = C_DIS_DK if disabled else C_BEVEL_DK
                slot_fill = C_DIS_FILL if disabled else C_HUB_FILL

                # 2px raised bevel (x,y in 1..2 or 21..22)
                if x <= 2 or y <= 2 or x >= w - 3 or y >= h - 3:
                    is_tl = (x + y < w - 1) and (x <= 2 or y <= 2)
                    if x >= w - 3 and y <= 2:
                        is_tl = (x + y < w - 1)
                    if y >= h - 3 and x <= 2:
                        is_tl = (x + y < h - 1)
                    px[ox + x, oy + y] = lt if is_tl else dk
                else:
                    # 18x18 recessed slot inside (x: 3..20, y: 3..20)
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
