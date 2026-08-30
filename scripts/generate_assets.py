#!/usr/bin/env python3
"""Generate Apex Ballistics item/block textures, models, recipes, loot tables, and tags."""
from __future__ import annotations

import json
import struct
import zlib
from pathlib import Path

ROOT = Path("/workspace/src/main/resources")
ASSETS = ROOT / "assets" / "apexballistics"
DATA = ROOT / "data" / "apexballistics"
MC_DATA = ROOT / "data" / "minecraft"

ITEMS = [
    "apex_alloy", "circuit_board", "guidance_chip", "solid_fuel", "warhead", "gauss_slug",
    "icbm", "slbm", "srbm", "alcm", "cruise_missile", "sam", "aam",
    "manpads", "gauss_rifle", "railgun", "plasma_blade", "targeting_tablet",
    "apex_helmet", "apex_chestplate", "apex_leggings", "apex_boots",
]
HANDHELD = {"gauss_rifle", "railgun", "plasma_blade", "manpads"}
BLOCKS = ["apex_alloy_block", "icbm_silo", "slbm_tube", "cruise_pad", "sam_battery", "radar"]


def write_png(path: Path, width: int, height: int, rgba: bytes) -> None:
    def chunk(tag: bytes, data: bytes) -> bytes:
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", zlib.crc32(tag + data) & 0xFFFFFFFF)

    raw = b"".join(b"\x00" + rgba[y * width * 4 : (y + 1) * width * 4] for y in range(height))
    ihdr = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", ihdr) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_bytes(png)


def px(buf: bytearray, w: int, x: int, y: int, color: tuple[int, int, int, int]) -> None:
    if 0 <= x < w and 0 <= y < (len(buf) // (w * 4)):
        i = (y * w + x) * 4
        buf[i : i + 4] = bytes(color)


def fill(buf: bytearray, w: int, h: int, color: tuple[int, int, int, int]) -> None:
    for y in range(h):
        for x in range(w):
            px(buf, w, x, y, color)


def rect(buf: bytearray, w: int, x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1):
        for x in range(x0, x1):
            px(buf, w, x, y, color)


def hline(buf: bytearray, w: int, x0: int, x1: int, y: int, color: tuple[int, int, int, int]) -> None:
    for x in range(x0, x1):
        px(buf, w, x, y, color)


def vline(buf: bytearray, w: int, x: int, y0: int, y1: int, color: tuple[int, int, int, int]) -> None:
    for y in range(y0, y1):
        px(buf, w, x, y, color)


def noise_metal(buf: bytearray, w: int, h: int, base: tuple[int, int, int], seed: int) -> None:
    for y in range(h):
        for x in range(w):
            n = ((x * 13 + y * 31 + seed * 17) ^ (x * y + seed)) & 15
            shade = n - 8
            px(buf, w, x, y, (
                max(0, min(255, base[0] + shade * 3)),
                max(0, min(255, base[1] + shade * 3)),
                max(0, min(255, base[2] + shade * 3)),
                255,
            ))


def item_texture(name: str) -> bytes:
    w = h = 16
    buf = bytearray(w * h * 4)
    fill(buf, w, h, (0, 0, 0, 0))
    palettes = {
        "apex_alloy": ((20, 80, 90), (40, 210, 220), (180, 255, 255)),
        "circuit_board": ((20, 70, 30), (40, 160, 50), (220, 180, 40)),
        "guidance_chip": ((30, 30, 50), (80, 160, 255), (240, 240, 255)),
        "solid_fuel": ((70, 40, 20), (200, 90, 30), (255, 200, 80)),
        "warhead": ((50, 50, 50), (180, 40, 40), (240, 220, 80)),
        "gauss_slug": ((40, 50, 60), (90, 200, 230), (230, 250, 255)),
        "icbm": ((30, 30, 35), (220, 220, 230), (200, 30, 30)),
        "slbm": ((15, 30, 70), (50, 110, 180), (220, 230, 255)),
        "srbm": ((40, 35, 20), (180, 150, 70), (230, 50, 40)),
        "alcm": ((30, 50, 20), (90, 130, 50), (200, 210, 80)),
        "cruise_missile": ((25, 25, 25), (70, 70, 70), (180, 180, 40)),
        "sam": ((50, 45, 20), (200, 170, 60), (40, 40, 40)),
        "aam": ((40, 50, 60), (170, 190, 210), (80, 200, 255)),
        "manpads": ((30, 40, 30), (70, 90, 50), (30, 30, 30)),
        "gauss_rifle": ((20, 25, 30), (40, 80, 100), (80, 220, 255)),
        "railgun": ((15, 15, 20), (50, 50, 80), (180, 80, 255)),
        "plasma_blade": ((15, 20, 30), (20, 180, 220), (180, 255, 255)),
        "targeting_tablet": ((20, 20, 25), (30, 90, 140), (80, 220, 180)),
        "apex_helmet": ((15, 40, 50), (30, 160, 170), (200, 255, 255)),
        "apex_chestplate": ((15, 40, 50), (30, 160, 170), (200, 255, 255)),
        "apex_leggings": ((15, 40, 50), (30, 160, 170), (200, 255, 255)),
        "apex_boots": ((15, 40, 50), (30, 160, 170), (200, 255, 255)),
    }
    dark, mid, light = palettes[name]

    if name in {"icbm", "slbm", "srbm", "alcm", "cruise_missile", "sam", "aam"}:
        # Vertical missile body
        rect(buf, w, 6, 1, 10, 15, mid)
        rect(buf, w, 7, 0, 9, 2, light)
        rect(buf, w, 6, 13, 10, 16, dark)
        px(buf, w, 5, 13, (*dark, 255))
        px(buf, w, 10, 13, (*dark, 255))
        px(buf, w, 4, 14, (*dark, 255))
        px(buf, w, 11, 14, (*dark, 255))
        hline(buf, w, 6, 10, 4, (*light, 255))
        hline(buf, w, 6, 10, 8, (*dark, 255) if name != "icbm" else (*light, 255))
        if name == "icbm":
            hline(buf, w, 6, 10, 6, (200, 30, 30, 255))
            hline(buf, w, 6, 10, 10, (200, 30, 30, 255))
        if name in {"sam", "aam"}:
            px(buf, w, 5, 6, (*mid, 255))
            px(buf, w, 10, 6, (*mid, 255))
            px(buf, w, 4, 7, (*mid, 255))
            px(buf, w, 11, 7, (*mid, 255))
    elif name in {"gauss_rifle", "railgun", "manpads"}:
        rect(buf, w, 1, 7, 15, 10, mid)
        rect(buf, w, 12, 6, 16, 11, light)
        rect(buf, w, 4, 9, 7, 14, dark)
        rect(buf, w, 6, 5, 9, 8, dark)
        if name == "railgun":
            hline(buf, w, 2, 12, 8, (*light, 255))
        if name == "manpads":
            rect(buf, w, 1, 6, 15, 11, mid)
            rect(buf, w, 13, 5, 16, 12, dark)
    elif name == "plasma_blade":
        rect(buf, w, 7, 8, 9, 16, dark)
        rect(buf, w, 6, 7, 10, 9, mid)
        rect(buf, w, 7, 0, 9, 8, light)
        px(buf, w, 6, 2, (*light, 255))
        px(buf, w, 9, 2, (*light, 255))
    elif name == "targeting_tablet":
        rect(buf, w, 3, 2, 13, 14, dark)
        rect(buf, w, 4, 3, 12, 12, mid)
        rect(buf, w, 5, 5, 11, 10, light)
        px(buf, w, 6, 13, (*mid, 255))
        px(buf, w, 9, 13, (*mid, 255))
    elif name == "apex_alloy":
        rect(buf, w, 3, 4, 13, 13, mid)
        rect(buf, w, 4, 5, 12, 12, light)
        hline(buf, w, 3, 13, 4, (*dark, 255))
        vline(buf, w, 3, 4, 13, (*dark, 255))
    elif name == "circuit_board":
        rect(buf, w, 2, 2, 14, 14, mid)
        for x, y in [(4, 4), (8, 4), (12, 4), (6, 8), (10, 8), (4, 12), (12, 12)]:
            px(buf, w, x, y, (*light, 255))
        hline(buf, w, 4, 12, 6, (*dark, 255))
        vline(buf, w, 8, 4, 12, (*dark, 255))
    elif name == "guidance_chip":
        rect(buf, w, 4, 4, 12, 12, dark)
        rect(buf, w, 5, 5, 11, 11, mid)
        px(buf, w, 8, 8, (*light, 255))
        hline(buf, w, 3, 13, 8, (*mid, 255))
        vline(buf, w, 8, 3, 13, (*mid, 255))
    elif name == "solid_fuel":
        rect(buf, w, 5, 1, 11, 15, mid)
        rect(buf, w, 6, 2, 10, 14, light)
        hline(buf, w, 5, 11, 5, (*dark, 255))
        hline(buf, w, 5, 11, 10, (*dark, 255))
    elif name == "warhead":
        rect(buf, w, 5, 3, 11, 14, mid)
        rect(buf, w, 6, 1, 10, 4, light)
        rect(buf, w, 6, 8, 10, 12, (200, 40, 40, 255))
    elif name == "gauss_slug":
        rect(buf, w, 4, 6, 12, 10, mid)
        rect(buf, w, 11, 5, 15, 11, light)
        rect(buf, w, 2, 6, 5, 10, dark)
    elif name == "apex_helmet":
        rect(buf, w, 4, 3, 12, 10, mid)
        rect(buf, w, 5, 6, 11, 9, light)
        rect(buf, w, 3, 8, 5, 11, dark)
        rect(buf, w, 11, 8, 13, 11, dark)
    elif name == "apex_chestplate":
        rect(buf, w, 3, 3, 13, 14, mid)
        rect(buf, w, 6, 5, 10, 11, light)
        vline(buf, w, 8, 4, 12, (*dark, 255))
    elif name == "apex_leggings":
        rect(buf, w, 4, 2, 12, 7, mid)
        rect(buf, w, 4, 7, 8, 15, dark)
        rect(buf, w, 8, 7, 12, 15, dark)
        vline(buf, w, 8, 7, 15, (*light, 255))
    elif name == "apex_boots":
        rect(buf, w, 3, 8, 7, 15, mid)
        rect(buf, w, 9, 8, 13, 15, mid)
        rect(buf, w, 2, 13, 8, 16, dark)
        rect(buf, w, 8, 13, 14, 16, dark)
    return bytes(buf)


def block_texture(name: str) -> bytes:
    w = h = 16
    buf = bytearray(w * h * 4)
    bases = {
        "apex_alloy_block": (20, 140, 150),
        "icbm_silo": (50, 55, 60),
        "slbm_tube": (30, 60, 110),
        "cruise_pad": (50, 80, 40),
        "sam_battery": (120, 100, 40),
        "radar": (40, 90, 130),
    }
    # Python randomizes hash() between processes, so use a stable seed to keep
    # generated resources reproducible across developer machines and CI.
    noise_metal(buf, w, h, bases[name], zlib.crc32(name.encode("utf-8")) & 255)
    if name == "icbm_silo":
        rect(buf, w, 4, 4, 12, 12, (20, 20, 22, 255))
        rect(buf, w, 6, 2, 10, 14, (180, 180, 190, 255))
        hline(buf, w, 6, 10, 6, (200, 40, 40, 255))
    elif name == "slbm_tube":
        rect(buf, w, 5, 1, 11, 15, (20, 40, 80, 255))
        rect(buf, w, 6, 2, 10, 14, (80, 160, 220, 255))
    elif name == "cruise_pad":
        rect(buf, w, 1, 12, 15, 15, (30, 30, 30, 255))
        rect(buf, w, 6, 4, 10, 13, (90, 140, 50, 255))
    elif name == "sam_battery":
        rect(buf, w, 2, 10, 14, 15, (40, 40, 40, 255))
        rect(buf, w, 7, 2, 10, 12, (210, 180, 70, 255))
        px(buf, w, 5, 6, (210, 180, 70, 255))
        px(buf, w, 11, 6, (210, 180, 70, 255))
    elif name == "radar":
        rect(buf, w, 7, 8, 9, 16, (30, 30, 30, 255))
        rect(buf, w, 3, 3, 13, 9, (80, 200, 230, 255))
        hline(buf, w, 3, 13, 6, (20, 40, 50, 255))
    elif name == "apex_alloy_block":
        for i in range(0, 16, 4):
            hline(buf, w, 0, 16, i, (10, 60, 70, 255))
            vline(buf, w, i, 0, 16, (10, 60, 70, 255))
        rect(buf, w, 6, 6, 10, 10, (180, 255, 255, 255))
    return bytes(buf)


def armor_layer(layer: int) -> bytes:
    w, h = 64, 32
    buf = bytearray(w * h * 4)
    fill(buf, w, h, (0, 0, 0, 0))
    teal = (25, 120, 130, 255)
    dark = (10, 40, 50, 255)
    glow = (80, 230, 240, 255)

    def box(x, y, bw, bh, color):
        rect(buf, w, x, y, x + bw, y + bh, color)

    if layer == 1:
        # head
        box(8, 0, 8, 8, teal)
        box(8, 8, 8, 8, teal)
        box(10, 10, 4, 3, glow)
        # body
        box(20, 16, 8, 12, teal)
        box(20, 20, 8, 2, glow)
        # arms
        box(40, 16, 4, 12, teal)
        box(44, 16, 4, 12, teal)
        # legs
        box(4, 16, 4, 12, dark)
        box(8, 16, 4, 12, dark)
        # overlay bits
        box(21, 16, 6, 2, dark)
    else:
        # leggings layer
        box(20, 16, 8, 12, teal)
        box(4, 16, 4, 12, teal)
        box(8, 16, 4, 12, teal)
        box(20, 20, 8, 1, glow)
    return bytes(buf)


def missile_entity_texture() -> bytes:
    """Neutral metal atlas tinted per missile type by the entity renderer."""
    w, h = 64, 32
    buf = bytearray(w * h * 4)
    noise_metal(buf, w, h, (210, 218, 224), 73)
    # Dark thermal shielding and panel seams make the cuboid model readable.
    rect(buf, w, 0, 20, 64, 32, (62, 68, 72, 255))
    for x in range(0, w, 8):
        vline(buf, w, x, 0, h, (120, 128, 134, 255))
    for y in (7, 15, 23):
        hline(buf, w, 0, w, y, (105, 112, 118, 255))
    rect(buf, w, 16, 0, 28, 8, (45, 48, 52, 255))
    rect(buf, w, 28, 0, 44, 12, (150, 158, 164, 255))
    return bytes(buf)


def write_json(path: Path, obj) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, indent=2) + "\n")


def shaped(result: str, count: int, pattern: list[str], key: dict) -> dict:
    return {
        "type": "minecraft:crafting_shaped",
        "category": "misc",
        "key": key,
        "pattern": pattern,
        "result": {"count": count, "id": result},
    }


def shapeless(result: str, count: int, ingredients: list) -> dict:
    return {
        "type": "minecraft:crafting_shapeless",
        "category": "misc",
        "ingredients": ingredients,
        "result": {"count": count, "id": result},
    }


def item_ing(item: str) -> dict:
    return {"item": item}


def main() -> None:
    tex_item = ASSETS / "textures" / "item"
    tex_block = ASSETS / "textures" / "block"
    tex_armor = ASSETS / "textures" / "models" / "armor"
    tex_entity = ASSETS / "textures" / "entity"
    models_item = ASSETS / "models" / "item"
    models_block = ASSETS / "models" / "block"
    blockstates = ASSETS / "blockstates"

    for name in ITEMS:
        write_png(tex_item / f"{name}.png", 16, 16, item_texture(name))
        parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
        write_json(models_item / f"{name}.json", {
            "parent": parent,
            "textures": {"layer0": f"apexballistics:item/{name}"},
        })

    for name in BLOCKS:
        write_png(tex_block / f"{name}.png", 16, 16, block_texture(name))
        write_json(models_block / f"{name}.json", {
            "parent": "minecraft:block/cube_all",
            "textures": {"all": f"apexballistics:block/{name}"},
        })
        write_json(models_item / f"{name}.json", {"parent": f"apexballistics:block/{name}"})
        if name in {"icbm_silo", "slbm_tube", "cruise_pad", "sam_battery"}:
            write_json(blockstates / f"{name}.json", {
                "variants": {
                    "facing=north": {"model": f"apexballistics:block/{name}"},
                    "facing=south": {"model": f"apexballistics:block/{name}", "y": 180},
                    "facing=west": {"model": f"apexballistics:block/{name}", "y": 270},
                    "facing=east": {"model": f"apexballistics:block/{name}", "y": 90},
                }
            })
        else:
            write_json(blockstates / f"{name}.json", {
                "variants": {"": {"model": f"apexballistics:block/{name}"}}
            })

    write_png(tex_armor / "apex_composite_layer_1.png", 64, 32, armor_layer(1))
    write_png(tex_armor / "apex_composite_layer_2.png", 64, 32, armor_layer(2))
    write_png(tex_entity / "missile.png", 64, 32, missile_entity_texture())

    recipes = DATA / "recipe"
    recipes.mkdir(parents=True, exist_ok=True)

    write_json(recipes / "apex_alloy.json", shaped("apexballistics:apex_alloy", 1, [
        "IDI",
        "NRN",
        "IBI",
    ], {
        "I": item_ing("minecraft:iron_ingot"),
        "D": item_ing("minecraft:diamond"),
        "N": item_ing("minecraft:netherite_scrap"),
        "R": item_ing("minecraft:redstone_block"),
        "B": item_ing("minecraft:blaze_rod"),
    }))
    write_json(recipes / "apex_alloy_block.json", shaped("apexballistics:apex_alloy_block", 1, [
        "AAA", "AAA", "AAA"
    ], {"A": item_ing("apexballistics:apex_alloy")}))
    write_json(recipes / "apex_alloy_from_block.json", shapeless("apexballistics:apex_alloy", 9, [
        item_ing("apexballistics:apex_alloy_block")
    ]))
    write_json(recipes / "circuit_board.json", shaped("apexballistics:circuit_board", 2, [
        "GRG",
        "RCR",
        "GRG",
    ], {
        "G": item_ing("minecraft:gold_ingot"),
        "R": item_ing("minecraft:redstone"),
        "C": item_ing("minecraft:copper_ingot"),
    }))
    write_json(recipes / "guidance_chip.json", shaped("apexballistics:guidance_chip", 1, [
        "AEA",
        "QCQ",
        "AEA",
    ], {
        "A": item_ing("minecraft:amethyst_shard"),
        "E": item_ing("minecraft:ender_eye"),
        "Q": item_ing("minecraft:quartz"),
        "C": item_ing("apexballistics:circuit_board"),
    }))
    write_json(recipes / "solid_fuel.json", shapeless("apexballistics:solid_fuel", 2, [
        item_ing("minecraft:gunpowder"),
        item_ing("minecraft:blaze_powder"),
        item_ing("minecraft:coal"),
        item_ing("minecraft:gunpowder"),
    ]))
    write_json(recipes / "warhead.json", shaped("apexballistics:warhead", 1, [
        " T ",
        "TGT",
        " T ",
    ], {
        "T": item_ing("minecraft:tnt"),
        "G": item_ing("minecraft:ghast_tear"),
    }))
    write_json(recipes / "gauss_slug.json", shapeless("apexballistics:gauss_slug", 4, [
        item_ing("minecraft:iron_nugget"),
        item_ing("minecraft:copper_ingot"),
        item_ing("minecraft:redstone"),
    ]))

    def missile_recipe(name: str, extra: str) -> dict:
        return shaped(f"apexballistics:{name}", 1, [
            " A ",
            "FWE",
            " G ",
        ], {
            "A": item_ing("apexballistics:apex_alloy"),
            "F": item_ing("apexballistics:solid_fuel"),
            "W": item_ing("apexballistics:warhead"),
            "E": item_ing(extra),
            "G": item_ing("apexballistics:guidance_chip"),
        })

    write_json(recipes / "icbm.json", missile_recipe("icbm", "minecraft:nether_star"))
    write_json(recipes / "slbm.json", missile_recipe("slbm", "minecraft:heart_of_the_sea"))
    write_json(recipes / "srbm.json", missile_recipe("srbm", "minecraft:fire_charge"))
    write_json(recipes / "alcm.json", missile_recipe("alcm", "minecraft:phantom_membrane"))
    write_json(recipes / "cruise_missile.json", missile_recipe("cruise_missile", "minecraft:compass"))
    write_json(recipes / "sam.json", missile_recipe("sam", "minecraft:ender_pearl"))
    write_json(recipes / "aam.json", missile_recipe("aam", "minecraft:feather"))

    write_json(recipes / "icbm_silo.json", shaped("apexballistics:icbm_silo", 1, [
        "AIA",
        "ODO",
        "ARA",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "I": item_ing("minecraft:iron_block"),
        "O": item_ing("minecraft:obsidian"),
        "D": item_ing("minecraft:dispenser"),
        "R": item_ing("apexballistics:circuit_board"),
    }))
    write_json(recipes / "slbm_tube.json", shaped("apexballistics:slbm_tube", 1, [
        "AHA",
        "PDP",
        "ARA",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "H": item_ing("minecraft:heart_of_the_sea"),
        "P": item_ing("minecraft:prismarine_shard"),
        "D": item_ing("minecraft:dispenser"),
        "R": item_ing("apexballistics:circuit_board"),
    }))
    write_json(recipes / "cruise_pad.json", shaped("apexballistics:cruise_pad", 1, [
        " A ",
        "IDI",
        "ARA",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "I": item_ing("minecraft:iron_ingot"),
        "D": item_ing("minecraft:dispenser"),
        "R": item_ing("apexballistics:circuit_board"),
    }))
    write_json(recipes / "sam_battery.json", shaped("apexballistics:sam_battery", 1, [
        " A ",
        "ODO",
        "ARA",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "O": item_ing("minecraft:observer"),
        "D": item_ing("minecraft:dispenser"),
        "R": item_ing("apexballistics:guidance_chip"),
    }))
    write_json(recipes / "radar.json", shaped("apexballistics:radar", 1, [
        " Q ",
        "ACA",
        "IRI",
    ], {
        "Q": item_ing("minecraft:ender_eye"),
        "A": item_ing("apexballistics:apex_alloy"),
        "C": item_ing("apexballistics:circuit_board"),
        "I": item_ing("minecraft:iron_ingot"),
        "R": item_ing("minecraft:redstone_block"),
    }))
    write_json(recipes / "targeting_tablet.json", shaped("apexballistics:targeting_tablet", 1, [
        "GGG",
        "GCG",
        "GGG",
    ], {
        "G": item_ing("minecraft:glass_pane"),
        "C": item_ing("apexballistics:guidance_chip"),
    }))
    write_json(recipes / "manpads.json", shaped("apexballistics:manpads", 1, [
        "A  ",
        "ICI",
        "  A",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "I": item_ing("minecraft:iron_ingot"),
        "C": item_ing("apexballistics:circuit_board"),
    }))
    write_json(recipes / "gauss_rifle.json", shaped("apexballistics:gauss_rifle", 1, [
        "ACC",
        "NRI",
        "  I",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "C": item_ing("minecraft:copper_ingot"),
        "N": item_ing("minecraft:netherite_ingot"),
        "R": item_ing("minecraft:redstone_block"),
        "I": item_ing("minecraft:iron_ingot"),
    }))
    write_json(recipes / "railgun.json", shaped("apexballistics:railgun", 1, [
        "AEE",
        "NGC",
        "  A",
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "E": item_ing("minecraft:echo_shard"),
        "N": item_ing("minecraft:netherite_ingot"),
        "G": item_ing("apexballistics:gauss_rifle"),
        "C": item_ing("apexballistics:guidance_chip"),
    }))
    write_json(recipes / "plasma_blade.json", shaped("apexballistics:plasma_blade", 1, [
        "  B",
        " A ",
        "S  ",
    ], {
        "B": item_ing("minecraft:blaze_rod"),
        "A": item_ing("apexballistics:apex_alloy"),
        "S": item_ing("minecraft:netherite_sword"),
    }))
    write_json(recipes / "apex_helmet.json", shaped("apexballistics:apex_helmet", 1, [
        "AAA", "A A"
    ], {"A": item_ing("apexballistics:apex_alloy")}))
    write_json(recipes / "apex_chestplate.json", shaped("apexballistics:apex_chestplate", 1, [
        "A A", "AAA", "AAA"
    ], {"A": item_ing("apexballistics:apex_alloy")}))
    write_json(recipes / "apex_leggings.json", shaped("apexballistics:apex_leggings", 1, [
        "AAA", "A A", "A A"
    ], {"A": item_ing("apexballistics:apex_alloy")}))
    write_json(recipes / "apex_boots.json", shaped("apexballistics:apex_boots", 1, [
        "A A", "A A"
    ], {"A": item_ing("apexballistics:apex_alloy")}))

    loot = DATA / "loot_table" / "blocks"
    for name in BLOCKS:
        write_json(loot / f"{name}.json", {
            "type": "minecraft:block",
            "pools": [{
                "bonus_rolls": 0.0,
                "conditions": [{"condition": "minecraft:survives_explosion"}],
                "entries": [{"type": "minecraft:item", "name": f"apexballistics:{name}"}],
                "rolls": 1.0,
            }],
        })

    write_json(MC_DATA / "tags" / "block" / "mineable" / "pickaxe.json", {
        "replace": False,
        "values": [f"apexballistics:{n}" for n in BLOCKS],
    })
    write_json(MC_DATA / "tags" / "block" / "needs_iron_tool.json", {
        "replace": False,
        "values": [f"apexballistics:{n}" for n in BLOCKS],
    })
    print("generated assets")


if __name__ == "__main__":
    main()
