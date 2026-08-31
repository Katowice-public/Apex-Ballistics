"""Native 512px material sheets and GUI art for Apex Ballistics."""
from __future__ import annotations

import math
import struct
import zlib
from pathlib import Path

SIZE = 512
TAU = math.tau

# 5x7 caps for stencils on 512px sheets.
FONT = {
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "11110", "10001", "10001", "10001", "11110"],
    "C": ["01110", "10001", "10000", "10000", "10000", "10001", "01110"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "11110", "10000", "10000", "10000", "11111"],
    "F": ["11111", "10000", "11110", "10000", "10000", "10000", "10000"],
    "G": ["01110", "10001", "10000", "10111", "10001", "10001", "01110"],
    "H": ["10001", "10001", "11111", "10001", "10001", "10001", "10001"],
    "I": ["11111", "00100", "00100", "00100", "00100", "00100", "11111"],
    "J": ["00111", "00001", "00001", "00001", "00001", "10001", "01110"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "11011", "10001"],
    "X": ["10001", "01010", "00100", "00100", "00100", "01010", "10001"],
    "Y": ["10001", "01010", "00100", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00110", "01000", "10000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["10001", "10001", "10001", "11111", "00001", "00001", "00001"],
    "5": ["11111", "10000", "11110", "00001", "00001", "10001", "01110"],
    " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
}


def write_png(path: Path, width: int, height: int, rgba: bytes) -> None:
    # 512-wide sheets keep their height so armor can stay 2:1 (512x256).
    # Anything smaller is treated as a draft and squared up to 512.
    if width != 512:
        source = rgba
        detailed = bytearray(SIZE * SIZE * 4)
        seed = zlib.crc32(path.as_posix().encode("utf-8")) & 0xFFFFFFFF
        for y in range(SIZE):
            sy = min(height - 1, y * height // SIZE)
            for x in range(SIZE):
                sx = min(width - 1, x * width // SIZE)
                source_i = (sy * width + sx) * 4
                target_i = (y * SIZE + x) * 4
                alpha = source[source_i + 3]
                if alpha == 0:
                    continue
                grain = (((x * 73856093) ^ (y * 19349663) ^ seed) & 15) - 7
                for channel in range(3):
                    detailed[target_i + channel] = max(0, min(255, source[source_i + channel] + grain))
                detailed[target_i + 3] = alpha
        rgba = bytes(detailed)
        width = height = SIZE

    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + rgba[y * width * 4:(y + 1) * width * 4] for y in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def px(buf, w, x, y, color) -> None:
    if 0 <= x < w and 0 <= y < (len(buf) // (w * 4)):
        if len(color) == 3:
            color = (*color, 255)
        i = (y * w + x) * 4
        buf[i:i + 4] = bytes(color)


def fill(buf, w, h, color) -> None:
    c = bytes(color if len(color) == 4 else (*color, 255))
    for i in range(0, w * h * 4, 4):
        buf[i:i + 4] = c


def rect(buf, w, x0, y0, x1, y1, color) -> None:
    x0, x1 = max(0, x0), min(w, x1)
    h = len(buf) // (w * 4)
    y0, y1 = max(0, y0), min(h, y1)
    if len(color) == 3:
        color = (*color, 255)
    row = bytes(color) * (x1 - x0)
    for y in range(y0, y1):
        i = (y * w + x0) * 4
        buf[i:i + len(row)] = row


def hline(buf, w, x0, x1, y, color) -> None:
    rect(buf, w, x0, y, x1, y + 1, color)


def vline(buf, w, x, y0, y1, color) -> None:
    rect(buf, w, x, y0, x + 1, y1, color)


def shade(color, delta):
    return tuple(max(0, min(255, c + delta)) for c in color[:3]) + ((color[3],) if len(color) == 4 else (255,))


def grain_at(x, y, seed) -> int:
    return (((x * 73856093) ^ (y * 19349663) ^ seed) & 15) - 7


def metal_fill(buf, w, h, base, seed, panel=48) -> None:
    br, bg, bb = base
    for y in range(h):
        for x in range(w):
            g = grain_at(x, y, seed)
            brush = ((x + y * 3 + seed) & 7) - 3
            panel_line = (x % panel == 0) or (y % panel == 0)
            rivet = (x % panel == 8 and y % panel == 8)
            d = g + brush - (18 if panel_line else 0) + (30 if rivet else 0)
            px(buf, w, x, y, (max(0, min(255, br + d)), max(0, min(255, bg + d)), max(0, min(255, bb + d)), 255))


def concrete_fill(buf, w, h, base, seed) -> None:
    br, bg, bb = base
    for y in range(h):
        for x in range(w):
            n = ((x * 17 + y * 29 + seed) ^ (x * y)) & 31
            crack = 0
            if (x + y * 2 + seed) % 97 == 0:
                crack = -40
            d = n - 12 + crack
            px(buf, w, x, y, (max(0, min(255, br + d)), max(0, min(255, bg + d)), max(0, min(255, bb + d)), 255))


def stencil(buf, w, text, x, y, scale, color) -> None:
    cx = x
    for ch in text.upper():
        glyph = FONT.get(ch, FONT[" "])
        for gy, row in enumerate(glyph):
            for gx, bit in enumerate(row):
                if bit == "1":
                    rect(buf, w, cx + gx * scale, y + gy * scale,
                         cx + (gx + 1) * scale, y + (gy + 1) * scale, color)
        cx += 6 * scale


def hazard_band(buf, w, y0, y1, c1=(230, 190, 30, 255), c2=(20, 20, 22, 255)) -> None:
    for y in range(y0, y1):
        for x in range(w):
            stripe = ((x + y) // 24) & 1
            px(buf, w, x, y, c1 if stripe else c2)


MISSILE_LIVERY = {
    "icbm": ((214, 216, 220), (28, 28, 32), (180, 32, 28), "ICBM"),
    "slbm": ((36, 64, 96), (12, 18, 28), (200, 210, 220), "SLBM"),
    "srbm": ((86, 92, 54), (30, 32, 22), (210, 170, 40), "SRBM"),
    "alcm": ((70, 88, 48), (24, 28, 18), (40, 48, 32), "ALCM"),
    "cruise_missile": ((48, 50, 46), (18, 18, 16), (200, 180, 50), "CRUISE"),
    "sam": ((210, 208, 200), (40, 40, 38), (200, 160, 30), "SAM"),
    "aam": ((196, 204, 212), (36, 48, 62), (80, 170, 210), "AAM"),
    "interceptor": ((232, 234, 236), (24, 24, 26), (200, 40, 36), "KV"),
}


def missile_skin(buf, w, name) -> None:
    body, dark, accent, label = MISSILE_LIVERY[name]
    metal_fill(buf, w, w, body, zlib.crc32(name.encode()) & 255, panel=64)
    h = w
    rect(buf, w, 0, 0, w, 36, (*dark, 255))
    rect(buf, w, 0, h - 48, w, h, (*dark, 255))
    for y in (90, 200, 310, 400):
        rect(buf, w, 0, y, w, y + 14, (*accent, 255))
    vline(buf, w, 40, 40, h - 48, (*dark, 255))
    vline(buf, w, 48, 40, h - 48, (*dark, 255))
    stencil(buf, w, label, 70, 130, 8, (*dark, 255))
    stencil(buf, w, "APEX", 70, 210, 5, (*accent, 255))
    hazard_band(buf, w, h - 80, h - 48)


def item_texture(name: str) -> bytes:
    w = h = SIZE
    buf = bytearray(w * h * 4)
    if name in MISSILE_LIVERY:
        missile_skin(buf, w, name)
        return bytes(buf)

    palettes = {
        "apex_alloy": (28, 150, 160),
        "circuit_board": (28, 92, 40),
        "guidance_chip": (40, 70, 120),
        "solid_fuel": (160, 70, 28),
        "warhead": (140, 40, 36),
        "gauss_slug": (70, 160, 180),
        "manpads": (70, 86, 52),
        "gauss_rifle": (36, 70, 88),
        "railgun": (48, 40, 90),
        "plasma_blade": (20, 140, 170),
        "targeting_tablet": (24, 70, 96),
        "jammer": (70, 50, 90),
        "apex_helmet": (20, 120, 130),
        "apex_chestplate": (20, 120, 130),
        "apex_leggings": (20, 120, 130),
        "apex_boots": (20, 120, 130),
    }
    if name.startswith("guidance_") or name.endswith("_package"):
        base = (30, 90, 130)
    elif "payload" in name or "warhead" in name:
        base = (140, 42, 36)
    elif "fuse" in name:
        base = (170, 120, 30)
    elif name.endswith("_module"):
        base = (70, 50, 130)
    else:
        base = palettes.get(name, (50, 70, 80))

    if name in {"manpads", "gauss_rifle", "railgun", "plasma_blade", "targeting_tablet", "jammer"}:
        metal_fill(buf, w, w, base, zlib.crc32(name.encode()) & 255, panel=48)
        stencil(buf, w, name.split("_")[0][:6], 40, 40, 6, (20, 20, 22, 255))
        return bytes(buf)

    metal_fill(buf, w, w, shade(base, -20)[:3], zlib.crc32(name.encode()) & 255, panel=56)
    rect(buf, w, 24, 24, 488, 48, (*shade(base, 30)[:3], 255))
    rect(buf, w, 24, 464, 488, 488, (*shade(base, -40)[:3], 255))
    stencil(buf, w, name.replace("_", " ")[:10], 40, 80, 5, (240, 240, 240, 255))
    for i in range(10):
        x = 40 + i * 44
        rect(buf, w, x, 430, x + 20, 452, (220, 200, 80, 255))
    if "armor" in name or name.startswith("apex_") and name.split("_")[-1] in {
        "helmet", "chestplate", "leggings", "boots"
    }:
        rect(buf, w, 160, 160, 352, 240, (80, 230, 240, 255))
    if "payload" in name or "warhead" in name:
        hazard_band(buf, w, 360, 400)
    if "guidance" in name:
        for r in range(40, 8, -6):
            for a in range(0, 360, 8):
                px(buf, w, int(256 + math.cos(math.radians(a)) * r),
                   int(300 + math.sin(math.radians(a)) * r), (40, 200, 220, 255))
    return bytes(buf)


def metal_fill_region(buf, w, x0, y0, x1, y1, base, seed) -> None:
    br, bg, bb = base
    for y in range(y0, y1):
        for x in range(x0, x1):
            g = grain_at(x, y, seed)
            px(buf, w, x, y, (max(0, min(255, br + g)), max(0, min(255, bg + g)),
                              max(0, min(255, bb + g)), 255))


BLOCK_BASES = {
    "apex_alloy_block": (20, 140, 150),
    "icbm_silo": (118, 118, 112),
    "slbm_tube": (40, 70, 110),
    "cruise_pad": (70, 88, 52),
    "sam_battery": (90, 92, 48),
    "mobile_launcher": (72, 84, 46),
    "vls": (70, 74, 78),
    "radar": (48, 90, 120),
    "ciws": (200, 204, 208),
    "laser_defense": (36, 90, 110),
    "passive_radar": (50, 70, 80),
    "command_console": (28, 40, 52),
    "submarine_control": (30, 50, 80),
    "missile_rack": (70, 74, 50),
    "loading_crane": (180, 150, 40),
    "propellant_refinery": (70, 80, 48),
    "maintenance_station": (60, 66, 70),
    "capacitor_charger": (40, 90, 110),
    "missile_assembly": (55, 70, 76),
    "reinforced_concrete": (92, 94, 96),
    "white_reinforced_concrete": (198, 198, 194),
    "black_reinforced_concrete": (32, 34, 36),
    "olive_reinforced_concrete": (78, 86, 54),
    "hazard_concrete": (90, 88, 70),
    "blast_steel": (70, 74, 80),
    "blast_door": (62, 66, 72),
    "security_door": (36, 70, 88),
    "silo_hatch": (74, 72, 68),
    "bunker_glass": (70, 140, 160),
}


def block_texture(name: str) -> bytes:
    w = h = SIZE
    buf = bytearray(w * h * 4)
    base = BLOCK_BASES.get(name, (60, 68, 72))
    seed = zlib.crc32(name.encode()) & 255
    if "concrete" in name:
        concrete_fill(buf, w, h, base, seed)
        for i in range(0, w, 128):
            vline(buf, w, i, 0, h, (40, 42, 44, 255))
            hline(buf, w, 0, w, i, (40, 42, 44, 255))
        if name == "hazard_concrete":
            for y in range(h):
                for x in range(w):
                    if ((x + y) // 36) & 1:
                        i = (y * w + x) * 4
                        buf[i] = min(255, buf[i] + 90)
                        buf[i + 1] = min(255, buf[i + 1] + 50)
                        buf[i + 2] = max(0, buf[i + 2] - 30)
    elif name == "bunker_glass":
        metal_fill(buf, w, h, base, seed, panel=128)
        for y in range(h):
            for x in range(w):
                i = (y * w + x) * 4
                buf[i + 3] = 180
    elif name == "apex_alloy_block":
        metal_fill(buf, w, h, base, seed, panel=64)
        rect(buf, w, 192, 192, 320, 320, (160, 255, 255, 255))
        stencil(buf, w, "APEX", 160, 40, 10, (10, 40, 48, 255))
    else:
        metal_fill(buf, w, h, base, seed, panel=64)
        stencil(buf, w, name.replace("_", " ")[:10], 40, 40, 4, (20, 20, 22, 255))
        if name in {"icbm_silo", "vls", "slbm_tube", "blast_door", "silo_hatch"}:
            hazard_band(buf, w, 430, 480)
        if name == "security_door":
            rect(buf, w, 160, 160, 352, 320, (40, 160, 190, 180))
    return bytes(buf)


def armor_layer(layer: int) -> bytes:
    w, h = 512, 256
    buf = bytearray(w * h * 4)
    fill(buf, w, h, (0, 0, 0, 0))
    teal = (25, 130, 140, 255)
    dark = (10, 40, 50, 255)
    glow = (80, 230, 240, 255)

    def box(x, y, bw, bh, color):
        # Vanilla 64x32 UV scaled 8x into 512x256.
        rect(buf, w, x * 8, y * 8, (x + bw) * 8, (y + bh) * 8, color)
        # Inner panel lines.
        for px_ in range(x * 8, (x + bw) * 8, 8):
            vline(buf, w, px_, y * 8, (y + bh) * 8, shade(color, -25))
        for py_ in range(y * 8, (y + bh) * 8, 8):
            hline(buf, w, x * 8, (x + bw) * 8, py_, shade(color, -25))

    if layer == 1:
        box(8, 0, 8, 8, teal)
        box(8, 8, 8, 8, teal)
        box(10, 10, 4, 3, glow)
        box(20, 16, 8, 12, teal)
        box(20, 20, 8, 2, glow)
        box(40, 16, 4, 12, teal)
        box(44, 16, 4, 12, teal)
        box(4, 16, 4, 12, dark)
        box(8, 16, 4, 12, dark)
        box(21, 16, 6, 2, dark)
    else:
        box(20, 16, 8, 12, teal)
        box(4, 16, 4, 12, teal)
        box(8, 16, 4, 12, teal)
        box(20, 20, 8, 1, glow)
    return bytes(buf)


def launcher_gui_texture(launcher: str) -> bytes:
    w = h = SIZE
    buf = bytearray(w * h * 4)
    fill(buf, w, h, (6, 10, 14, 255))
    themes = {
        "silo": ((235, 72, 62, 255), "STRATCOM", "SILO"),
        "tube": ((55, 142, 225, 255), "SUBMARINE", "TUBE"),
        "pad": ((88, 184, 76, 255), "CRUISE", "PAD"),
        "sam_battery": ((226, 185, 55, 255), "AIR DEFENSE", "SAM"),
        "mobile": ((144, 176, 82, 255), "TEL", "MOBILE"),
        "vls": ((160, 92, 220, 255), "VERTICAL", "VLS"),
    }
    accent, heading, code = themes[launcher]
    rect(buf, w, 4, 4, 252, 216, (16, 26, 34, 255))
    rect(buf, w, 8, 8, 248, 34, (24, 38, 48, 255))
    hline(buf, w, 8, 248, 34, accent)
    hline(buf, w, 8, 248, 168, accent)
    rect(buf, w, 12, 38, 150, 164, (10, 18, 24, 255))
    rect(buf, w, 154, 38, 244, 164, (10, 18, 24, 255))
    for y in range(42, 160, 10):
        hline(buf, w, 16, 146, y, (18, 32, 40, 255))
    stencil(buf, w, heading, 16, 14, 2, accent)
    stencil(buf, w, code, 160, 14, 2, (230, 230, 230, 255))
    rect(buf, w, 14, 172, 122, 200, (22, 34, 40, 255))
    rect(buf, w, 134, 172, 242, 200, (22, 34, 40, 255))
    if launcher == "silo":
        for r in range(48, 6, -3):
            for a in range(0, 360, 2):
                x = int(199 + math.cos(math.radians(a)) * r)
                y = int(101 + math.sin(math.radians(a)) * r)
                px(buf, w, x, y, accent if r % 12 == 0 else (30, 50, 58, 255))
        vline(buf, w, 199, 54, 148, accent)
        hline(buf, w, 152, 246, 101, accent)
        stencil(buf, w, "RANGE", 162, 148, 2, accent)
    elif launcher == "tube":
        rect(buf, w, 158, 42, 240, 158, (8, 22, 40, 255))
        for i in range(14):
            y = 48 + i * 8
            width = 12 + ((i * 17) % 50)
            rect(buf, w, 164, y, 164 + width, y + 5, accent if i % 3 == 0 else (30, 70, 110, 255))
        stencil(buf, w, "SONAR", 168, 148, 2, accent)
    elif launcher == "pad":
        for x in range(160, 242, 10):
            vline(buf, w, x, 44, 152, (28, 64, 36, 255))
        for y in range(44, 152, 10):
            hline(buf, w, 160, 242, y, (28, 64, 36, 255))
        for x, y in ((176, 60), (196, 78), (214, 102), (228, 128)):
            rect(buf, w, x, y, x + 6, y + 6, accent)
        hline(buf, w, 176, 228, 80, (240, 240, 80, 255))
        stencil(buf, w, "ROUTE", 168, 148, 2, accent)
    elif launcher == "sam_battery":
        rect(buf, w, 158, 42, 240, 158, (24, 20, 8, 255))
        for i in range(10):
            h = 20 + (i * 13) % 90
            col = accent if i in (2, 5, 8) else (80, 70, 30, 255)
            rect(buf, w, 162 + i * 7, 150 - h, 168 + i * 7, 150, col)
        stencil(buf, w, "TRACK", 168, 148, 2, accent)
    elif launcher == "mobile":
        rect(buf, w, 164, 88, 236, 132, accent)
        rect(buf, w, 210, 64, 242, 118, (40, 56, 28, 255))
        rect(buf, w, 214, 72, 238, 108, (90, 160, 70, 255))
        for x in (170, 188, 206):
            rect(buf, w, x, 128, x + 12, 142, (18, 18, 18, 255))
        rect(buf, w, 168, 52, 198, 84, (50, 70, 32, 255))
        stencil(buf, w, "TEL", 176, 148, 2, accent)
    else:
        rect(buf, w, 158, 42, 240, 146, (22, 12, 36, 255))
        for i, (x, z) in enumerate(((164, 50), (204, 50), (164, 96), (204, 96))):
            rect(buf, w, x, z, x + 32, z + 40, (48, 28, 70, 255))
            hatch = i >= 2
            rect(buf, w, x + 6, z + (4 if hatch else 14), x + 26, z + (16 if hatch else 22), accent)
        stencil(buf, w, "MK41", 176, 148, 2, accent)
    return bytes(buf)
