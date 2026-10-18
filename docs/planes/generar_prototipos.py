#!/usr/bin/env python3

# -*- coding: utf-8 -*-

"""

Generador de Prototipos de Texturas 16x16 (Vanilla+ Tactical Pixel Art)

Proyecto: Vision Goggles

Genera 13 m├│dulos t├ícticos centrados + 3 ├¡tems de hardware en docs/prototipos_texturas/

"""



from pathlib import Path

from PIL import Image



# Directorio de salida

OUTPUT_DIR = Path("docs/prototipos_texturas")



# ==============================================================================

# PALETA MAESTRA VANILLA+ MILITAR (RGBA)

# ==============================================================================

T = (0, 0, 0, 0)  # Transparente



# Chasis de Pol├¡mero Oscuro (Luz cenital: arriba-izquierda claro -> abajo-derecha oscuro)

POLY_SPEC = (91, 101, 115, 255)   # #5B6573 Brillo especular superior

POLY_LIGHT = (74, 82, 93, 255)    # #4A525D Borde superior iluminado

POLY_MID = (53, 60, 69, 255)      # #353C45 Cuerpo medio del marco

POLY_DARK = (36, 42, 50, 255)     # #242A32 Sombra lateral/interna

POLY_DEEP = (24, 28, 34, 255)     # #181C22 Borde inferior en sombra

POLY_BLACK = (15, 18, 23, 255)    # #0F1217 Cavidad profunda / contorno interior

RIVET_LIGHT = (112, 124, 140, 255)

RIVET_DARK = (42, 48, 56, 255)



# Conectores Dorados (Pines inferiores)

GOLD_BRIGHT = (255, 224, 115, 255)  # #FFE073

GOLD_MID = (235, 178, 56, 255)      # #EBB238

GOLD_DARK = (176, 112, 24, 255)     # #B07018

GOLD_SHADOW = (115, 68, 14, 255)    # #73440E

PCB_NECK = (28, 38, 30, 255)        # Sustrato de conexi├│n





def create_canvas() -> Image.Image:

    """Crea un lienzo RGBA de 16x16 completamente transparente."""

    return Image.new("RGBA", (16, 16), T)





def put(img: Image.Image, x: int, y: int, color: tuple):

    """Pinta un p├¡xel validando l├¡mites 16x16."""

    if 0 <= x < 16 and 0 <= y < 16:

        img.putpixel((x, y), color)





def fill_rect(img: Image.Image, x0: int, y0: int, x1: int, y1: int, color: tuple):

    """Rellena un rect├íngulo inclusivo [x0..x1, y0..y1]."""

    for y in range(y0, y1 + 1):

        for x in range(x0, x1 + 1):

            put(img, x, y, color)





def build_module_chassis(reinforced_pins: bool = False) -> Image.Image:

    """

    Construye el chasis base centrado para todos los m├│dulos:

    - Ancho: 12px (x = 2..13), dejando 2px de margen a cada lado (0..1 y 14..15).

    - Alto del cuerpo: y = 1..12 con esquinas biseladas.

    - Conectores dorados inferiores en y = 13..14 (x = 4..11).

    - Ventana central para el n├║cleo tem├ítico en x = 4..11, y = 3..10 (8x8).

    """

    img = create_canvas()



    # 1. Relleno base del cuerpo (x=2..13, y=1..12 sin las 4 esquinas extremas)

    for y in range(1, 13):

        for x in range(2, 14):

            if (x in (2, 13)) and (y in (1, 12)):

                continue

            put(img, x, y, POLY_MID)



    # 2. Sombreado cenital del borde exterior

    for x in range(3, 13):

        put(img, x, 1, POLY_SPEC if 4 <= x <= 8 else POLY_LIGHT)

        put(img, x, 12, POLY_DEEP)



    for y in range(2, 12):

        # Lado izquierdo recibe algo m├ís de luz arriba, sombra abajo

        put(img, 2, y, POLY_LIGHT if y <= 5 else POLY_DARK)

        # Lado derecho en sombra media/profunda

        put(img, 13, y, POLY_DARK if y <= 6 else POLY_DEEP)



    # 3. Bisel interior y remaches t├ícticos

    for x in range(3, 13):

        put(img, x, 2, POLY_DARK)

        put(img, x, 11, POLY_LIGHT if 5 <= x <= 10 else POLY_MID)

    for y in range(2, 12):

        put(img, 3, y, POLY_DARK)

        put(img, 12, y, POLY_MID)



    # Remaches en las 4 esquinas del marco

    put(img, 3, 2, RIVET_LIGHT)

    put(img, 12, 2, RIVET_DARK)

    put(img, 3, 11, RIVET_DARK)

    put(img, 12, 11, RIVET_DARK)



    # 4. Cavidad central hundida 8x8 (x=4..11, y=3..10)

    fill_rect(img, 4, 3, 11, 10, POLY_BLACK)

    # Sombra interna superior/izquierda de la cavidad

    for x in range(4, 12):

        put(img, x, 3, (10, 12, 16, 255))



    # 5. Conectores dorados inferiores (y=13..14, x=4..11)

    fill_rect(img, 4, 13, 11, 14, PCB_NECK)

    pin_columns = [4, 5, 7, 8, 10, 11] if reinforced_pins else [4, 6, 9, 11]

    for px in pin_columns:

        put(img, px, 13, GOLD_BRIGHT)

        put(img, px, 14, GOLD_DARK)

    # Sombra bajo el chasis entre pines

    for x in range(4, 12):

        if x not in pin_columns:

            put(img, x, 13, POLY_DEEP)

            put(img, x, 14, T)



    return img





def draw_circular_lens(img: Image.Image, palette: list):

    """

    Dibuja una lente ├│ptica circular/octogonal en la cavidad 8x8 (x=4..11, y=3..10).

    palette = [borde_oscuro, tono_medio, tono_brillante, especular_blanco]

    """

    c_dark, c_mid, c_bright, c_spec = palette

    # Anillo exterior de la lente (6x6 centrado en x=5..10, y=4..9)

    for y in range(4, 10):

        for x in range(5, 11):

            if (x in (5, 10)) and (y in (4, 9)):

                put(img, x, y, POLY_BLACK)

            else:

                put(img, x, y, c_dark)



    # N├║cleo ├│ptico 4x4 (x=6..9, y=5..8)

    fill_rect(img, 6, 5, 9, 8, c_mid)

    # Degradado de luminiscencia interna (abajo-derecha)

    put(img, 8, 7, c_bright)

    put(img, 9, 7, c_bright)

    put(img, 7, 8, c_bright)

    put(img, 8, 8, c_bright)

    # Destello especular (arriba-izquierda)

    put(img, 6, 5, c_spec)

    put(img, 7, 5, c_bright)

    put(img, 6, 6, c_bright)





# ==============================================================================

# 13 M├ôDULOS DE VISION GOGGLES

# ==============================================================================



def make_nvg_module() -> Image.Image:

    """nvg_module: Lente verde f├│sforo militar con reflejo blanco/lima."""

    img = build_module_chassis()

    palette = [

        (16, 74, 34, 255),    # Verde militar oscuro

        (36, 150, 62, 255),   # Verde f├│sforo medio

        (110, 235, 94, 255),  # Lima brillante

        (235, 255, 230, 255)  # Blanco especular

    ]

    draw_circular_lens(img, palette)

    # Ret├¡cula t├íctica sutil en el centro

    put(img, 7, 6, (70, 200, 80, 255))

    put(img, 8, 6, (70, 200, 80, 255))

    return img





def make_thermal_module() -> Image.Image:

    """thermal_module: Sensor infrarrojo con degradado rojo/naranja y n├║cleo amarillo."""

    img = build_module_chassis()

    c_violet = (72, 20, 68, 255)

    c_red = (186, 34, 34, 255)

    c_orange = (242, 118, 24, 255)

    c_yellow = (255, 232, 84, 255)

    c_white = (255, 252, 224, 255)



    # Fondo t├®rmico violeta/rojo oscuro en la ventana

    draw_circular_lens(img, [c_violet, c_red, c_orange, c_yellow])

    # Firma de calor conc├®ntrica (punto caliente central-derecho)

    fill_rect(img, 6, 5, 9, 8, c_red)

    fill_rect(img, 7, 6, 9, 8, c_orange)

    put(img, 7, 6, c_yellow)

    put(img, 8, 6, c_yellow)

    put(img, 7, 7, c_yellow)

    put(img, 8, 7, c_white)  # Hot-spot m├íximo

    return img





def make_hydro_module() -> Image.Image:

    """hydro_module: Diafragma acu├ítico azul cobalto y cian con burbuja especular."""

    img = build_module_chassis()

    palette = [

        (14, 48, 110, 255),   # Azul abisal

        (26, 102, 194, 255),  # Azul cobalto

        (56, 204, 240, 255),  # Cian luminoso

        (224, 252, 255, 255)  # Reflejo cristalino

    ]

    draw_circular_lens(img, palette)

    # Anillo de diafragma hidrof├│bico

    put(img, 7, 5, (38, 148, 224, 255))

    put(img, 8, 8, (112, 232, 255, 255))

    put(img, 9, 6, (56, 204, 240, 255))

    return img





def make_bio_module() -> Image.Image:

    """bio_module: Monitor card├¡aco oscuro con pulso ECG ├ímbar/rojo luminoso."""

    img = build_module_chassis()

    screen_bg = (28, 12, 16, 255)

    screen_grid = (52, 20, 26, 255)

    ecg_red = (214, 40, 40, 255)

    ecg_amber = (255, 158, 44, 255)

    ecg_peak = (255, 238, 168, 255)



    fill_rect(img, 4, 3, 11, 10, screen_bg)

    # Rejilla m├®dica sutil

    for x in range(4, 12, 2):

        put(img, x, 4, screen_grid)

        put(img, x, 9, screen_grid)



    # L├¡nea de pulso ECG de izquierda a derecha (x=4..11)

    put(img, 4, 7, ecg_red)

    put(img, 5, 7, ecg_amber)

    put(img, 6, 8, ecg_red)       # Onda Q

    put(img, 7, 4, ecg_peak)      # Pico R alto

    put(img, 7, 5, ecg_amber)

    put(img, 7, 6, ecg_amber)

    put(img, 8, 9, ecg_red)       # Valle S bajo

    put(img, 8, 8, ecg_amber)

    put(img, 9, 6, ecg_amber)     # Onda T

    put(img, 10, 7, ecg_red)

    put(img, 11, 7, ecg_red)

    return img





def make_zoom_module() -> Image.Image:

    """zoom_module: Ensamble ├│ptico teleobjetivo con anillos conc├®ntricos azulados."""

    img = build_module_chassis()

    ring_outer = (64, 76, 92, 255)

    ring_dark = (30, 38, 48, 255)

    glass_deep = (24, 62, 118, 255)

    glass_cyan = (82, 178, 235, 255)

    glass_spec = (230, 248, 255, 255)



    # Anillo met├ílico de enfoque (x=4..11, y=3..10 sin esquinas)

    for y in range(3, 11):

        for x in range(4, 12):

            if (x in (4, 11)) and (y in (3, 10)):

                continue

            put(img, x, y, ring_outer if (x + y) % 2 == 0 else ring_dark)



    # Segundo barril telesc├│pico interno (x=5..10, y=4..9)

    fill_rect(img, 5, 4, 10, 9, (20, 26, 34, 255))

    # Lente teleobjetivo central 4x4 (x=6..9, y=5..8)

    fill_rect(img, 6, 5, 9, 8, glass_deep)

    put(img, 6, 5, glass_spec)

    put(img, 7, 5, glass_cyan)

    put(img, 6, 6, glass_cyan)

    put(img, 8, 7, glass_cyan)

    put(img, 9, 8, (130, 210, 250, 255))

    return img





def make_solar_module() -> Image.Image:

    """solar_module: Celda fotovoltaica azul oscuro cuadriculada con reflejo diagonal."""

    img = build_module_chassis()

    cell_dark = (18, 36, 82, 255)

    cell_mid = (30, 64, 136, 255)

    cell_shine = (72, 132, 214, 255)

    busbar_silver = (168, 188, 212, 255)

    busbar_gold = (224, 182, 74, 255)



    fill_rect(img, 4, 3, 11, 10, cell_dark)



    # Celdas solares con brillo diagonal

    for y in range(3, 11):

        for x in range(4, 12):

            dist = abs((x - 4) - (y - 3))

            if dist == 0:

                put(img, x, y, cell_shine)

            elif dist == 1:

                put(img, x, y, cell_mid)



    # L├¡neas conductoras (busbars) formando cuadr├¡cula fotovoltaica

    for x in range(4, 12):

        put(img, x, 6, busbar_silver)

        put(img, x, 7, (44, 82, 156, 255))

    for y in range(3, 11):

        put(img, 7, y, busbar_silver)

        put(img, 8, y, (44, 82, 156, 255))

    # Nodos de intersecci├│n dorados

    put(img, 7, 6, busbar_gold)

    put(img, 4, 3, (220, 240, 255, 255))

    return img





def make_sonar_module() -> Image.Image:

    """sonar_module: Transductor ac├║stico con ondas conc├®ntricas turquesa."""

    img = build_module_chassis()

    bg_deep = (10, 32, 38, 255)

    wave_dim = (22, 96, 104, 255)

    wave_mid = (42, 176, 182, 255)

    wave_bright = (116, 242, 236, 255)

    ping_white = (232, 255, 252, 255)



    fill_rect(img, 4, 3, 11, 10, bg_deep)



    # Onda exterior (arco superior-derecho)

    for px, py in [(6, 3), (7, 3), (8, 3), (9, 4), (10, 5), (11, 6), (11, 7), (11, 8)]:

        put(img, px, py, wave_dim)



    # Onda media ac├║stica

    for px, py in [(6, 5), (7, 5), (8, 5), (9, 6), (9, 7), (9, 8)]:

        put(img, px, py, wave_mid)



    # N├║cleo emisor del sonar (abajo-izquierda de la ventana)

    put(img, 5, 8, wave_bright)

    put(img, 6, 8, wave_bright)

    put(img, 5, 9, ping_white)

    put(img, 6, 9, wave_bright)

    return img





def make_flashlight_module() -> Image.Image:

    """flashlight_module: Mini reflector LED blanco c├ílido con halo amarillo."""

    img = build_module_chassis()

    reflector_edge = (110, 116, 128, 255)

    halo_orange = (230, 154, 36, 255)

    halo_yellow = (255, 222, 74, 255)

    led_warm = (255, 248, 204, 255)

    led_pure = (255, 255, 255, 255)



    # Paraboloide reflector met├ílico

    draw_circular_lens(img, [reflector_edge, halo_orange, halo_yellow, led_pure])

    # Diodo COB central de alta intensidad

    fill_rect(img, 6, 5, 9, 8, halo_yellow)

    fill_rect(img, 7, 6, 8, 7, led_warm)

    put(img, 7, 6, led_pure)

    put(img, 6, 5, led_warm)

    put(img, 9, 8, halo_orange)

    return img





def make_spawn_security_module() -> Image.Image:

    """spawn_security_module: Escudo t├íctico dorado/amarillo con n├║cleo de alerta."""

    img = build_module_chassis()

    bg = (22, 20, 16, 255)

    shield_dark = (168, 102, 16, 255)

    shield_gold = (238, 178, 38, 255)

    shield_light = (255, 232, 118, 255)

    core_red = (235, 52, 52, 255)

    core_bright = (255, 160, 120, 255)



    fill_rect(img, 4, 3, 11, 10, bg)



    # Silueta de escudo de 6x7 (x=5..10, y=3..9)

    shield_rows = [

        (3, 5, 10),

        (4, 5, 10),

        (5, 5, 10),

        (6, 5, 10),

        (7, 6, 9),

        (8, 6, 9),

        (9, 7, 8),

    ]

    for y, x0, x1 in shield_rows:

        for x in range(x0, x1 + 1):

            if y == 3 or x == x0:

                put(img, x, y, shield_light if y <= 5 else shield_gold)

            elif x == x1 or y >= 8:

                put(img, x, y, shield_dark)

            else:

                put(img, x, y, shield_gold)



    # N├║cleo sensor de amenaza en el centro del escudo

    fill_rect(img, 7, 5, 8, 7, core_red)

    put(img, 7, 5, core_bright)

    return img





def make_vital_info_module() -> Image.Image:

    """vital_info_module: Chip m├®dico con cruz de primeros auxilios roja y blanca."""

    img = build_module_chassis()

    bg_plate = (218, 226, 235, 255)

    plate_shadow = (158, 170, 184, 255)

    cross_red = (216, 34, 42, 255)

    cross_dark = (156, 18, 26, 255)

    cross_hi = (255, 112, 118, 255)



    # Placa m├®dica blanca/plateada con esquinas recortadas (x=5..10, y=4..9)

    for y in range(4, 10):

        for x in range(5, 11):

            if (x in (5, 10)) and (y in (4, 9)):

                continue

            put(img, x, y, bg_plate if (x + y) <= 13 else plate_shadow)



    # Cruz roja sim├®trica en el centro (x=6..9, y=5..8)

    for x in range(6, 10):

        put(img, x, 6, cross_red)

        put(img, x, 7, cross_dark)

    for y in range(5, 9):

        put(img, 7, y, cross_red)

        put(img, 8, y, cross_dark if y >= 7 else cross_red)



    put(img, 7, 5, cross_hi)

    put(img, 6, 6, cross_hi)

    return img





def make_environment_module() -> Image.Image:

    """environment_module: Br├║jula/sensor barom├®trico verde azulado con aguja dorada."""

    img = build_module_chassis()

    dial_dark = (14, 58, 56, 255)

    dial_mid = (28, 116, 108, 255)

    dial_light = (68, 184, 168, 255)

    needle_gold = (255, 214, 74, 255)

    needle_north = (240, 72, 62, 255)



    draw_circular_lens(img, [dial_dark, dial_mid, dial_light, (180, 245, 235, 255)])



    # Puntos cardinales en el bisel de la esfera

    put(img, 7, 4, GOLD_MID)

    put(img, 8, 9, GOLD_DARK)

    put(img, 5, 6, GOLD_MID)

    put(img, 10, 7, GOLD_DARK)



    # Aguja diagonal (Norte arriba-derecha en rojo/oro, Sur abajo-izquierda en dorado)

    put(img, 9, 5, needle_north)

    put(img, 8, 6, needle_gold)

    put(img, 7, 7, needle_gold)

    put(img, 6, 8, GOLD_DARK)

    return img





def make_battery_expansion_module() -> Image.Image:

    """battery_expansion_module: Banco doble de celdas de litio verdes y pines reforzados."""

    img = build_module_chassis(reinforced_pins=True)

    cell_dark = (20, 82, 38, 255)

    cell_mid = (42, 158, 68, 255)

    cell_bright = (118, 238, 108, 255)

    cap_gold = (242, 194, 68, 255)



    # Dos celdas verticales paralelas dentro de la cavidad:

    # Celda 1: x=5..6, y=4..9 | Celda 2: x=9..10, y=4..9

    for cx in (5, 9):

        # Cabezal positivo dorado

        put(img, cx, 3, cap_gold)

        put(img, cx + 1, 3, GOLD_DARK)

        # Cuerpo de litio verde

        for y in range(4, 10):

            put(img, cx, y, cell_bright if y <= 6 else cell_mid)

            put(img, cx + 1, y, cell_mid if y <= 6 else cell_dark)



    # Puente busbar central de alta corriente (x=7..8)

    put(img, 7, 5, GOLD_BRIGHT)

    put(img, 8, 5, GOLD_MID)

    put(img, 7, 8, GOLD_MID)

    put(img, 8, 8, GOLD_DARK)

    return img





def make_programmable_module() -> Image.Image:

    """programmable_module: Microcontrolador EEPROM oscuro con pistas de circuito doradas."""

    img = build_module_chassis()

    pcb_bg = (18, 42, 32, 255)

    trace_gold = (224, 174, 52, 255)

    trace_hi = (255, 224, 122, 255)

    ic_body = (26, 28, 34, 255)

    ic_hi = (58, 64, 76, 255)

    pin_silver = (176, 188, 202, 255)



    fill_rect(img, 4, 3, 11, 10, pcb_bg)



    # Pistas doradas en las 4 direcciones

    for x in (4, 11):

        put(img, x, 5, trace_gold)

        put(img, x, 8, trace_gold)

    for y in (3, 10):

        put(img, 6, y, trace_gold)

        put(img, 9, y, trace_gold)



    # Patillas met├ílicas del chip EEPROM

    for y in (5, 7, 8):

        put(img, 5, y, pin_silver)

        put(img, 10, y, pin_silver)



    # Encapsulado central del microcontrolador (x=6..9, y=4..9)

    fill_rect(img, 6, 4, 9, 9, ic_body)

    for x in range(6, 10):

        put(img, x, 4, ic_hi)

    # Grabado l├íser dorado del chip (Pin 1 dot + bus interno)

    put(img, 6, 5, trace_hi)

    put(img, 7, 7, trace_gold)

    put(img, 8, 7, trace_gold)

    put(img, 8, 6, trace_hi)

    return img





# ==============================================================================

# 3 ├ìTEMS INDEPENDIENTES DE HARDWARE T├üCTICO

# ==============================================================================



def make_electrical_parts() -> Image.Image:

    """electrical_parts: Placa PCB verde oliva con par de chips negros, trazas y condensador."""

    img = create_canvas()

    pcb_light = (76, 112, 52, 255)

    pcb_mid = (52, 82, 36, 255)

    pcb_dark = (34, 56, 24, 255)

    pcb_border = (22, 38, 16, 255)



    # Placa base centrada (x=2..13, y=2..13) con muescas t├®cnicas

    for y in range(2, 14):

        for x in range(2, 14):

            if (x in (2, 13)) and (y in (2, 13)):

                continue

            if y == 2 or x == 2:

                put(img, x, y, pcb_light)

            elif y == 13 or x == 13:

                put(img, x, y, pcb_border)

            else:

                put(img, x, y, pcb_mid if (x + y) < 16 else pcb_dark)



    # Pines dorados en el borde inferior derecho (x=6..11, y=13)

    for x in range(6, 12, 2):

        put(img, x, 13, GOLD_BRIGHT)

        put(img, x + 1, 13, GOLD_DARK)



    # Pistas de cobre dorado sobre la PCB

    for x, y in [(4, 9), (5, 9), (6, 9), (6, 10), (6, 11), (7, 7), (8, 7), (9, 10), (10, 10)]:

        put(img, x, y, GOLD_MID)



    # Chip IC #1 (arriba-izquierda, x=4..7, y=4..6)

    fill_rect(img, 4, 4, 7, 6, POLY_DEEP)

    put(img, 4, 4, POLY_SPEC)

    put(img, 5, 4, POLY_LIGHT)

    for x in range(4, 8):

        put(img, x, 3, RIVET_LIGHT)

        put(img, x, 7, RIVET_DARK)



    # Chip IC #2 (abajo-derecha, x=8..11, y=8..10)

    fill_rect(img, 8, 8, 11, 10, POLY_DEEP)

    put(img, 8, 8, POLY_LIGHT)

    put(img, 9, 9, GOLD_BRIGHT)



    # Condensador electrol├¡tico cil├¡ndrico azul/plata (arriba-derecha, x=9..11, y=3..6)

    fill_rect(img, 9, 4, 11, 6, (36, 108, 186, 255))

    put(img, 9, 4, (88, 172, 245, 255))

    put(img, 9, 5, (88, 172, 245, 255))

    put(img, 9, 3, (210, 220, 232, 255))  # Tapa de aluminio superior

    put(img, 10, 3, (164, 176, 190, 255))

    return img





def make_nvg_battery() -> Image.Image:

    """nvg_battery: Cilindro t├íctico de litio centrado con indicador de carga verde/amarillo."""

    img = create_canvas()



    # Polo positivo de lat├│n dorado (x=6..9, y=1..2)

    fill_rect(img, 6, 1, 9, 2, GOLD_MID)

    put(img, 6, 1, GOLD_BRIGHT)

    put(img, 7, 1, GOLD_BRIGHT)

    put(img, 9, 2, GOLD_SHADOW)



    # Cuerpo cil├¡ndrico de pol├¡mero militar (x=4..11, y=3..14)

    for y in range(3, 15):

        for x in range(4, 12):

            if (x in (4, 11)) and (y in (3, 14)):

                continue

            # Sombreado cil├¡ndrico horizontal (luz en x=5..6, sombra en x=10..11)

            if x == 4 or x == 11:

                color = POLY_BLACK

            elif x == 5:

                color = POLY_SPEC if y < 12 else POLY_LIGHT

            elif x == 6:

                color = POLY_LIGHT

            elif x in (7, 8):

                color = POLY_MID

            elif x == 9:

                color = POLY_DARK

            else:

                color = POLY_DEEP

            put(img, x, y, color)



    # Anillos de blindaje superior e inferior

    for x in range(5, 11):

        put(img, x, 3, POLY_LIGHT if x <= 7 else POLY_DARK)

        put(img, x, 13, POLY_DARK if x <= 7 else POLY_BLACK)



    # Franja vertical de estado de carga LED (x=7..8, y=5..11)

    fill_rect(img, 6, 4, 9, 12, POLY_BLACK)  # Marco del medidor

    # Segmentos superiores: Verde carga alta

    put(img, 7, 5, (140, 255, 120, 255))

    put(img, 8, 5, (64, 210, 72, 255))

    put(img, 7, 6, (100, 235, 90, 255))

    put(img, 8, 6, (44, 176, 56, 255))

    # Segmento medio: Lima/Amarillo

    put(img, 7, 8, (210, 245, 78, 255))

    put(img, 8, 8, (168, 204, 42, 255))

    # Segmento inferior: ├ümbar energ├®tico

    put(img, 7, 10, (255, 212, 64, 255))

    put(img, 8, 10, (214, 146, 28, 255))

    put(img, 7, 11, (235, 168, 36, 255))

    put(img, 8, 11, (176, 108, 18, 255))



    return img





def make_tactical_flashlight() -> Image.Image:

    """tactical_flashlight: Linterna t├íctica diagonal de pol├¡mero negro con lente emisora blanca."""

    img = create_canvas()



    # Construcci├│n diagonal de abajo-izquierda (culote en 2,13) a arriba-derecha (cabezal en 13,2)

    # 1. Mango estriado (x + y entre 13 y 17, en la mitad inferior izquierda)

    for y in range(5, 15):

        for x in range(1, 11):

            d_axis = (x - 2) - (13 - y)  # Distancia perpendicular a la diagonal principal

            s_pos = (x - 2) + (13 - y)   # Posici├│n a lo largo del eje (0 = base, 14 = cabezal)

            if 0 <= s_pos <= 9 and abs(d_axis) <= 2:

                if abs(d_axis) == 2:

                    put(img, x, y, POLY_BLACK)

                elif d_axis == -1:

                    # Borde superior-izquierdo iluminado

                    put(img, x, y, POLY_SPEC if s_pos % 2 == 0 else POLY_LIGHT)

                elif d_axis == 0:

                    # N├║cleo con estr├¡as antideslizantes

                    put(img, x, y, POLY_MID if s_pos % 2 == 0 else POLY_DARK)

                else:

                    # Borde inferior-derecho en sombra

                    put(img, x, y, POLY_DEEP)



    # Bot├│n de encendido t├íctico (rojo/naranja sutil en la mitad del cuerpo)

    put(img, 5, 8, (214, 68, 48, 255))

    put(img, 6, 8, (148, 38, 26, 255))



    # 2. Cabezal ensanchado (Bezel t├íctico en x=8..14, y=1..7)

    for y in range(1, 8):

        for x in range(8, 15):

            d_axis = (x - 2) - (13 - y)

            s_pos = (x - 2) + (13 - y)

            if 10 <= s_pos <= 16 and abs(d_axis) <= 3:

                if abs(d_axis) == 3:

                    put(img, x, y, POLY_BLACK)

                elif d_axis < 0:

                    put(img, x, y, POLY_LIGHT)

                elif d_axis == 0:

                    put(img, x, y, POLY_MID)

                else:

                    put(img, x, y, POLY_DEEP)



    # 3. Lente emisora LED blanca/c├ílida de alto contraste en la boca (arriba-derecha)

    lens_white = (255, 255, 255, 255)

    lens_warm = (255, 246, 196, 255)

    lens_Gold = (255, 210, 82, 255)



    for px, py in [(11, 2), (12, 2), (12, 3), (13, 3), (13, 4)]:

        put(img, px, py, lens_warm)

    put(img, 12, 2, lens_white)

    put(img, 13, 3, lens_white)

    put(img, 11, 3, lens_Gold)

    put(img, 12, 4, lens_Gold)

    # Corona protectora del bisel frontal

    put(img, 13, 2, RIVET_LIGHT)

    put(img, 14, 3, POLY_MID)



    return img





def generate_all():

    """Genera las 16 texturas de Vision Goggles en docs/prototipos_texturas/."""

    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)



    sprites = {

        # 13 M├│dulos

        "nvg_module": make_nvg_module,

        "thermal_module": make_thermal_module,

        "hydro_module": make_hydro_module,

        "bio_module": make_bio_module,

        "zoom_module": make_zoom_module,

        "solar_module": make_solar_module,

        "sonar_module": make_sonar_module,

        "flashlight_module": make_flashlight_module,

        "spawn_security_module": make_spawn_security_module,

        "vital_info_module": make_vital_info_module,

        "environment_module": make_environment_module,

        "battery_expansion_module": make_battery_expansion_module,

        "programmable_module": make_programmable_module,

        # 3 Componentes / ├ìtems t├ícticos

        "electrical_parts": make_electrical_parts,

        "nvg_battery": make_nvg_battery,

        "tactical_flashlight": make_tactical_flashlight,

    }



    for name, builder in sprites.items():

        img = builder()

        out_path = OUTPUT_DIR / f"{name}.png"

        img.save(out_path, format="PNG")

        print(f"[OK] Generado: {out_path} ({img.size[0]}x{img.size[1]} RGBA)")





if __name__ == "__main__":

    generate_all()