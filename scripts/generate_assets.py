#!/usr/bin/env python3
"""Generate Apex Ballistics item/block textures, models, recipes, loot tables, and tags."""
from __future__ import annotations

import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from obj_meshes import (
    COMPONENT_OBJ,
    HANDHELD_OBJ,
    MISSILE_OBJ,
    OBJ_BLOCKS,
    ObjBuilder,
    add_ciws_item_mesh,
    add_ciws_turret,
    add_component_mesh,
    add_door_item_mesh,
    add_door_mesh,
    add_handheld_mesh,
    add_hatch_mesh,
    add_laser_head,
    add_laser_item_mesh,
    add_launcher_mesh,
    add_missile_mesh,
    add_radar_base_mesh,
    add_radar_dish_mesh,
    add_radar_item_mesh,
    add_system_mesh,
)
from textures import armor_layer, block_texture, item_texture, launcher_gui_texture, write_png

ROOT = Path("/workspace/src/main/resources")
ASSETS = ROOT / "assets" / "apexballistics"
DATA = ROOT / "data" / "apexballistics"
MC_DATA = ROOT / "data" / "minecraft"

ITEMS = [
    "apex_alloy", "circuit_board", "guidance_chip", "solid_fuel", "warhead", "gauss_slug",
    "advanced_propellant", "energy_cell", "capacitor",
    "icbm", "slbm", "srbm", "alcm", "cruise_missile", "sam", "aam", "interceptor",
    "manpads", "gauss_rifle", "railgun", "plasma_blade", "targeting_tablet",
    "apex_helmet", "apex_chestplate", "apex_leggings", "apex_boots",
    "guidance_inertial", "guidance_coordinate", "guidance_terrain", "guidance_radar",
    "guidance_infrared", "guidance_command", "emp_payload", "incendiary_payload",
    "penetrator_payload", "fragmentation_payload", "decoy_warhead", "mirv_warhead",
    "proximity_fuse", "airburst_fuse", "delayed_fuse", "two_stage_motor",
    "three_stage_motor", "precision_package", "reliability_package", "anti_jam_module",
    "flare", "jammer", "thermal_module", "rwr_module", "shield_module",
    "mobility_module", "camouflage_module", "medical_module",
]
HANDHELD = {"gauss_rifle", "railgun", "plasma_blade", "manpads", "jammer"}
LAUNCHER_BLOCKS = {
    "icbm_silo", "slbm_tube", "cruise_pad", "sam_battery", "mobile_launcher", "vls"
}
BLOCKS = [
    "apex_alloy_block", *sorted(LAUNCHER_BLOCKS), "radar", "missile_assembly",
    "ciws", "laser_defense", "passive_radar", "command_console", "submarine_control",
    "missile_rack", "loading_crane", "propellant_refinery", "maintenance_station",
    "capacitor_charger", "reinforced_concrete", "white_reinforced_concrete",
    "black_reinforced_concrete", "olive_reinforced_concrete", "hazard_concrete",
    "blast_steel", "bunker_glass",
]
DOORS = ["blast_door", "security_door"]
TRAPDOORS = ["silo_hatch"]
ALL_BLOCKS = BLOCKS + DOORS + TRAPDOORS


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
    tex_gui = ASSETS / "textures" / "gui"
    models_item = ASSETS / "models" / "item"
    models_block = ASSETS / "models" / "block"
    blockstates = ASSETS / "blockstates"

    for name in ITEMS:
        write_png(tex_item / f"{name}.png", 512, 512, item_texture(name))
        if name in MISSILE_OBJ or name in HANDHELD_OBJ or name in COMPONENT_OBJ:
            continue
        parent = "minecraft:item/handheld" if name in HANDHELD else "minecraft:item/generated"
        write_json(models_item / f"{name}.json", {
            "parent": parent,
            "textures": {"layer0": f"apexballistics:item/{name}"},
        })

    for name in BLOCKS:
        write_png(tex_block / f"{name}.png", 512, 512, block_texture(name))
        if name not in OBJ_BLOCKS:
            write_json(models_block / f"{name}.json", {
                "parent": "minecraft:block/cube_all",
                "textures": {"all": f"apexballistics:block/{name}"},
            })
            write_json(models_item / f"{name}.json", {"parent": f"apexballistics:block/{name}"})
        if name in LAUNCHER_BLOCKS:
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

    for name in DOORS:
        write_png(tex_block / f"{name}.png", 512, 512, block_texture(name))
        write_png(tex_block / f"{name}_bottom.png", 512, 512, block_texture(name))
        write_png(tex_block / f"{name}_top.png", 512, 512, block_texture(name))
        variants = {}
        closed_rotation = {"east": 0, "north": 270, "south": 90, "west": 180}
        open_left_rotation = {"east": 90, "north": 0, "south": 180, "west": 270}
        open_right_rotation = {"east": 270, "north": 180, "south": 0, "west": 90}
        for facing in ("east", "north", "south", "west"):
            for half in ("lower", "upper"):
                for hinge in ("left", "right"):
                    for opened in (False, True):
                        suffix = f"{'bottom' if half == 'lower' else 'top'}_{hinge}"
                        if opened:
                            suffix += "_open"
                        rotation = (open_left_rotation if hinge == "left" else open_right_rotation)[facing] \
                            if opened else closed_rotation[facing]
                        value = {"model": f"apexballistics:block/{name}_{suffix}"}
                        if rotation:
                            value["y"] = rotation
                        key = f"facing={facing},half={half},hinge={hinge},open={str(opened).lower()}"
                        variants[key] = value
        write_json(blockstates / f"{name}.json", {"variants": variants})
        for half in ("bottom", "top"):
            for hinge in ("left", "right"):
                for opened in (False, True):
                    suffix = f"{half}_{hinge}" + ("_open" if opened else "")
                    door_mesh = ObjBuilder(f"{name}_{suffix}")
                    add_door_mesh(door_mesh, name, suffix)
                    door_mesh.write(models_block)
                    write_json(models_block / f"{name}_{suffix}.json", {
                        "loader": "forge:obj",
                        "model": f"apexballistics:models/block/{name}_{suffix}.obj",
                        "flip_v": True,
                        "automatic_culling": False,
                        "shade_quads": True,
                        "textures": {
                            "texture0": f"apexballistics:block/{name}",
                            "particle": f"apexballistics:block/{name}",
                        },
                    })
        door_item = ObjBuilder(name)
        add_door_item_mesh(door_item, name)
        door_item.write(models_item)

    for name in TRAPDOORS:
        write_png(tex_block / f"{name}.png", 512, 512, block_texture(name))
        variants = {}
        rotation = {"east": 90, "north": 0, "south": 180, "west": 270}
        for facing in ("east", "north", "south", "west"):
            for half in ("bottom", "top"):
                for opened in (False, True):
                    suffix = "open" if opened else half
                    value = {"model": f"apexballistics:block/{name}_{suffix}"}
                    if opened and rotation[facing]:
                        value["y"] = rotation[facing]
                    key = f"facing={facing},half={half},open={str(opened).lower()}"
                    variants[key] = value
        write_json(blockstates / f"{name}.json", {"variants": variants})
        for suffix in ("bottom", "top", "open"):
            hatch = ObjBuilder(f"{name}_{suffix}")
            add_hatch_mesh(hatch, suffix)
            hatch.write(models_block)
            write_json(models_block / f"{name}_{suffix}.json", {
                "loader": "forge:obj",
                "model": f"apexballistics:models/block/{name}_{suffix}.obj",
                "flip_v": True,
                "automatic_culling": False,
                "shade_quads": True,
                "textures": {
                    "texture0": f"apexballistics:block/{name}",
                    "particle": f"apexballistics:block/{name}",
                },
            })
        hatch_item = ObjBuilder(name)
        add_hatch_mesh(hatch_item, "bottom")
        hatch_item.write(models_item)

    item_display = {
        "gui": {"rotation": [25, 225, 0], "translation": [0, 0, 0], "scale": [0.72, 0.72, 0.72]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 2, 0], "scale": [0.45, 0.45, 0.45]},
        "fixed": {"rotation": [0, 0, 0], "translation": [0, 0, 0], "scale": [0.90, 0.90, 0.90]},
        "thirdperson_righthand": {"rotation": [0, 90, 55], "translation": [0, 3, 1], "scale": [0.55, 0.55, 0.55]},
        "firstperson_righthand": {"rotation": [0, 90, 25], "translation": [1, 2, 1], "scale": [0.60, 0.60, 0.60]},
    }
    weapon_display = {
        "gui": {"rotation": [45, 225, 0], "translation": [0, 2, 0], "scale": [0.85, 0.85, 0.85]},
        "ground": {"rotation": [0, 0, 0], "translation": [0, 3, 0], "scale": [0.55, 0.55, 0.55]},
        "fixed": {"rotation": [0, 180, 0], "translation": [0, 0, 0], "scale": [0.90, 0.90, 0.90]},
        "thirdperson_righthand": {"rotation": [0, 90, -35], "translation": [0, 4, 2], "scale": [0.70, 0.70, 0.70]},
        "firstperson_righthand": {"rotation": [0, 90, -25], "translation": [4, 2, 2], "scale": [0.80, 0.80, 0.80]},
        "thirdperson_lefthand": {"rotation": [0, -90, 35], "translation": [0, 4, 2], "scale": [0.70, 0.70, 0.70]},
        "firstperson_lefthand": {"rotation": [0, -90, 25], "translation": [4, 2, 2], "scale": [0.80, 0.80, 0.80]},
    }

    def obj_descriptor(model_path: str, texture: str, display=None) -> dict:
        data = {
            "loader": "forge:obj",
            "model": model_path,
            "flip_v": True,
            "automatic_culling": False,
            "shade_quads": True,
            "textures": {"texture0": texture, "particle": texture},
        }
        if display is not None:
            data["display"] = display
        return data

    for name in sorted(MISSILE_OBJ):
        mesh = ObjBuilder(name)
        add_missile_mesh(mesh, name)
        mesh.write(models_item)
        write_json(models_item / f"{name}.json", obj_descriptor(
            f"apexballistics:models/item/{name}.obj",
            f"apexballistics:item/{name}",
            item_display,
        ))

    for name in sorted(HANDHELD_OBJ):
        mesh = ObjBuilder(name)
        add_handheld_mesh(mesh, name)
        mesh.write(models_item)
        write_json(models_item / f"{name}.json", obj_descriptor(
            f"apexballistics:models/item/{name}.obj",
            f"apexballistics:item/{name}",
            weapon_display,
        ))

    for name in sorted(LAUNCHER_BLOCKS):
        mesh = ObjBuilder(name)
        add_launcher_mesh(mesh, name)
        mesh.write(models_block)
        descriptor = obj_descriptor(
            f"apexballistics:models/block/{name}.obj",
            f"apexballistics:block/{name}",
        )
        write_json(models_block / f"{name}.json", descriptor)
        write_json(models_item / f"{name}.json", descriptor | {"display": item_display})

    for name in sorted(OBJ_BLOCKS - LAUNCHER_BLOCKS):
        mesh = ObjBuilder(name)
        if name == "radar":
            add_radar_base_mesh(mesh)
        else:
            add_system_mesh(mesh, name)
        mesh.write(models_block)
        descriptor = obj_descriptor(
            f"apexballistics:models/block/{name}.obj",
            f"apexballistics:block/{name}",
        )
        write_json(models_block / f"{name}.json", descriptor)
        if name == "radar":
            radar_item = ObjBuilder("radar")
            add_radar_item_mesh(radar_item)
            radar_item.write(models_item)
            write_json(models_item / "radar.json", obj_descriptor(
                "apexballistics:models/item/radar.obj",
                "apexballistics:block/radar",
                item_display,
            ))
        elif name == "ciws":
            ciws_item = ObjBuilder("ciws")
            add_ciws_item_mesh(ciws_item)
            ciws_item.write(models_item)
            write_json(models_item / "ciws.json", obj_descriptor(
                "apexballistics:models/item/ciws.obj",
                "apexballistics:block/ciws",
                item_display,
            ))
        elif name == "laser_defense":
            laser_item = ObjBuilder("laser_defense")
            add_laser_item_mesh(laser_item)
            laser_item.write(models_item)
            write_json(models_item / "laser_defense.json", obj_descriptor(
                "apexballistics:models/item/laser_defense.obj",
                "apexballistics:block/laser_defense",
                item_display,
            ))
        else:
            write_json(models_item / f"{name}.json", descriptor | {"display": item_display})

    for name in sorted(COMPONENT_OBJ):
        mesh = ObjBuilder(name)
        add_component_mesh(mesh, name)
        mesh.write(models_item)
        write_json(models_item / f"{name}.json", obj_descriptor(
            f"apexballistics:models/item/{name}.obj",
            f"apexballistics:item/{name}",
            item_display,
        ))

    for name in DOORS:
        write_json(models_item / f"{name}.json", obj_descriptor(
            f"apexballistics:models/item/{name}.obj",
            f"apexballistics:block/{name}",
            item_display,
        ))
    for name in TRAPDOORS:
        write_json(models_item / f"{name}.json", obj_descriptor(
            f"apexballistics:models/item/{name}.obj",
            f"apexballistics:block/{name}",
            item_display,
        ))

    dish = ObjBuilder("radar_dish_component")
    add_radar_dish_mesh(dish)
    dish.write(models_item)
    write_json(models_item / "radar_dish_component.json", obj_descriptor(
        "apexballistics:models/item/radar_dish_component.obj",
        "apexballistics:block/radar",
        item_display,
    ))
    turret = ObjBuilder("ciws_turret_component")
    add_ciws_turret(turret)
    turret.write(models_item)
    write_json(models_item / "ciws_turret_component.json", obj_descriptor(
        "apexballistics:models/item/ciws_turret_component.obj",
        "apexballistics:block/ciws",
        item_display,
    ))
    laser_head = ObjBuilder("laser_head_component")
    add_laser_head(laser_head)
    laser_head.write(models_item)
    write_json(models_item / "laser_head_component.json", obj_descriptor(
        "apexballistics:models/item/laser_head_component.obj",
        "apexballistics:block/laser_defense",
        item_display,
    ))

    write_png(tex_armor / "apex_composite_layer_1.png", 512, 256, armor_layer(1))
    write_png(tex_armor / "apex_composite_layer_2.png", 512, 256, armor_layer(2))
    write_png(tex_entity / "missile.png", 512, 512, item_texture("icbm"))
    for launcher in ("silo", "tube", "pad", "sam_battery", "mobile", "vls"):
        write_png(tex_gui / f"launcher_{launcher}.png", 512, 512,
                  launcher_gui_texture(launcher))

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

    write_json(recipes / "advanced_propellant.json", shapeless("apexballistics:advanced_propellant", 2, [
        item_ing("apexballistics:solid_fuel"), item_ing("minecraft:blaze_powder"),
        item_ing("minecraft:redstone"),
    ]))
    write_json(recipes / "energy_cell.json", shaped("apexballistics:energy_cell", 2, [
        "ACA", "RER", "ACA"
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "C": item_ing("minecraft:copper_ingot"),
        "R": item_ing("minecraft:redstone"),
        "E": item_ing("minecraft:ender_pearl"),
    }))
    write_json(recipes / "capacitor.json", shaped("apexballistics:capacitor", 2, [
        "CRC", "AEA", "CRC"
    ], {
        "C": item_ing("minecraft:copper_ingot"),
        "R": item_ing("minecraft:redstone"),
        "A": item_ing("apexballistics:apex_alloy"),
        "E": item_ing("apexballistics:energy_cell"),
    }))
    write_json(recipes / "interceptor.json", missile_recipe("interceptor", "minecraft:nether_star"))

    module_ingredients = {
        "guidance_inertial": "minecraft:compass",
        "guidance_coordinate": "minecraft:recovery_compass",
        "guidance_terrain": "minecraft:filled_map",
        "guidance_radar": "minecraft:ender_eye",
        "guidance_infrared": "minecraft:magma_cream",
        "guidance_command": "minecraft:comparator",
        "emp_payload": "minecraft:lightning_rod",
        "incendiary_payload": "minecraft:fire_charge",
        "penetrator_payload": "minecraft:netherite_ingot",
        "fragmentation_payload": "minecraft:iron_nugget",
        "decoy_warhead": "minecraft:firework_rocket",
        "mirv_warhead": "minecraft:nether_star",
        "proximity_fuse": "minecraft:observer",
        "airburst_fuse": "minecraft:daylight_detector",
        "delayed_fuse": "minecraft:repeater",
        "two_stage_motor": "apexballistics:advanced_propellant",
        "three_stage_motor": "minecraft:dragon_breath",
        "precision_package": "minecraft:amethyst_shard",
        "reliability_package": "minecraft:netherite_scrap",
        "anti_jam_module": "minecraft:echo_shard",
    }
    for name, ingredient in module_ingredients.items():
        write_json(recipes / f"{name}.json", shapeless(f"apexballistics:{name}", 1, [
            item_ing("apexballistics:guidance_chip"),
            item_ing("apexballistics:circuit_board"),
            item_ing(ingredient),
        ]))

    write_json(recipes / "flare.json", shapeless("apexballistics:flare", 4, [
        item_ing("minecraft:firework_rocket"), item_ing("minecraft:blaze_powder"),
        item_ing("minecraft:glowstone_dust"),
    ]))
    write_json(recipes / "jammer.json", shaped("apexballistics:jammer", 1, [
        "ACA", "EGE", "ACA"
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "C": item_ing("apexballistics:circuit_board"),
        "E": item_ing("minecraft:echo_shard"),
        "G": item_ing("apexballistics:guidance_chip"),
    }))

    armor_modules = {
        "thermal_module": "minecraft:magma_cream",
        "rwr_module": "minecraft:bell",
        "shield_module": "minecraft:shield",
        "mobility_module": "minecraft:elytra",
        "camouflage_module": "minecraft:invisibility_potion",
        "medical_module": "minecraft:golden_apple",
    }
    # Potion item identifiers cannot encode potion NBT in this simple recipe;
    # use fermented spider eye as the camouflage electronics reagent.
    armor_modules["camouflage_module"] = "minecraft:fermented_spider_eye"
    for name, ingredient in armor_modules.items():
        write_json(recipes / f"{name}.json", shapeless(f"apexballistics:{name}", 1, [
            item_ing("apexballistics:capacitor"),
            item_ing("apexballistics:circuit_board"),
            item_ing(ingredient),
        ]))

    infrastructure = [
        "mobile_launcher", "vls", "missile_assembly", "ciws", "laser_defense",
        "passive_radar", "command_console", "submarine_control", "missile_rack",
        "loading_crane", "propellant_refinery", "maintenance_station",
        "capacitor_charger",
    ]
    for name in infrastructure:
        write_json(recipes / f"{name}.json", shaped(f"apexballistics:{name}", 1, [
            "ACA", "IRI", "AAA"
        ], {
            "A": item_ing("apexballistics:apex_alloy"),
            "C": item_ing("apexballistics:circuit_board"),
            "I": item_ing("minecraft:iron_block"),
            "R": item_ing("minecraft:redstone_block"),
        }))

    concrete_colors = {
        "reinforced_concrete": "minecraft:gray_concrete",
        "white_reinforced_concrete": "minecraft:white_concrete",
        "black_reinforced_concrete": "minecraft:black_concrete",
        "olive_reinforced_concrete": "minecraft:green_concrete",
        "hazard_concrete": "minecraft:yellow_concrete",
    }
    for name, concrete in concrete_colors.items():
        write_json(recipes / f"{name}.json", shaped(f"apexballistics:{name}", 4, [
            "I I", "CBC", "I I"
        ], {
            "I": item_ing("minecraft:iron_ingot"),
            "C": item_ing(concrete),
            "B": item_ing("minecraft:obsidian"),
        }))
    write_json(recipes / "blast_steel.json", shaped("apexballistics:blast_steel", 4, [
        "IAI", "ANA", "IAI"
    ], {
        "I": item_ing("minecraft:iron_block"),
        "A": item_ing("apexballistics:apex_alloy"),
        "N": item_ing("minecraft:netherite_scrap"),
    }))
    write_json(recipes / "bunker_glass.json", shaped("apexballistics:bunker_glass", 4, [
        "AGA", "GOG", "AGA"
    ], {
        "A": item_ing("apexballistics:apex_alloy"),
        "G": item_ing("minecraft:glass"),
        "O": item_ing("minecraft:obsidian"),
    }))
    for name in DOORS:
        write_json(recipes / f"{name}.json", shaped(f"apexballistics:{name}", 1, [
            "SS", "CC", "SS"
        ], {
            "S": item_ing("apexballistics:blast_steel"),
            "C": item_ing("apexballistics:circuit_board"),
        }))
    write_json(recipes / "silo_hatch.json", shaped("apexballistics:silo_hatch", 1, [
        "SSS", "CAC"
    ], {
        "S": item_ing("apexballistics:blast_steel"),
        "C": item_ing("apexballistics:circuit_board"),
        "A": item_ing("apexballistics:apex_alloy"),
    }))

    loot = DATA / "loot_table" / "blocks"
    for name in ALL_BLOCKS:
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
        "values": [f"apexballistics:{n}" for n in ALL_BLOCKS],
    })
    write_json(MC_DATA / "tags" / "block" / "needs_iron_tool.json", {
        "replace": False,
        "values": [f"apexballistics:{n}" for n in ALL_BLOCKS],
    })

    lang_path = ASSETS / "lang" / "en_us.json"
    lang = json.loads(lang_path.read_text()) if lang_path.exists() else {}
    display_overrides = {
        "icbm": "ICBM", "slbm": "SLBM", "srbm": "SRBM", "alcm": "ALCM",
        "sam": "SAM", "aam": "AAM", "vls": "Vertical Launch System",
        "ciws": "CIWS Point Defense", "rwr_module": "Radar Warning Module",
        "mirv_warhead": "MIRV Warhead", "emp_payload": "EMP Payload",
        "anti_jam_module": "Anti-Jam Module", "jammer": "Electronic Jammer",
        "manpads": "MANPADS",
        "manpads": "MANPADS",
    }
    for name in ITEMS:
        lang[f"item.apexballistics.{name}"] = display_overrides.get(
            name, name.replace("_", " ").title())
    for name in ALL_BLOCKS:
        lang[f"block.apexballistics.{name}"] = display_overrides.get(
            name, name.replace("_", " ").title())
    lang["item.apexballistics.radar_dish_component"] = "Radar Dish"
    lang["item.apexballistics.ciws_turret_component"] = "CIWS Turret"
    lang["item.apexballistics.laser_head_component"] = "Laser Emitter"
    lang["entity.apexballistics.flare"] = "Countermeasure Flare"
    lang["item.apexballistics.interceptor.desc"] = "High-altitude interceptor optimized for hostile missiles."
    lang["item.apexballistics.missile_module.desc"] = "Install at a Missile Assembly Station."
    lang["item.apexballistics.jammer.desc"] = "Disrupts nearby radar guidance while active; consumes durability."
    lang["item.apexballistics.armor_module.install"] = "Install at a Maintenance Station."
    module_descriptions = {
        "thermal": "Highlights living heat signatures within 24 blocks.",
        "rwr": "Warns when a guided missile is tracking the wearer.",
        "shield": "Consumes suit energy to reduce incoming damage.",
        "mobility": "Crouch while airborne for a powered vertical boost.",
        "camouflage": "Crouching activates energy-consuming optical camouflage.",
        "medical": "Automatically stabilizes critically injured wearers.",
    }
    for module, description in module_descriptions.items():
        lang[f"item.apexballistics.armor_module.{module}.desc"] = description
    lang.update({
        "screen.apexballistics.launcher.silo": "Strategic ICBM Launch Control",
        "screen.apexballistics.launcher.tube": "Submerged SLBM Fire Control",
        "screen.apexballistics.launcher.pad": "Cruise Missile Mission Computer",
        "screen.apexballistics.launcher.sam_battery": "Surface-to-Air Engagement Console",
        "screen.apexballistics.launcher.mobile": "Mobile Launcher Tactical Console",
        "screen.apexballistics.launcher.vls": "Vertical Launch System Control",
        "screen.apexballistics.launch": "AUTHORIZE LAUNCH",
        "screen.apexballistics.eject": "SAFE EJECT",
        "screen.apexballistics.type": "Platform: %s",
        "screen.apexballistics.magazine": "Magazine: %s / %s",
        "screen.apexballistics.integrity": "System integrity: %s%%",
        "screen.apexballistics.cooldown": "Cycle time: %s ticks",
        "screen.apexballistics.emp": "EMP lockout: %s ticks",
        "screen.apexballistics.airburst": "Airburst setting: %s blocks",
        "screen.apexballistics.target": "Target grid: X %s / Z %s",
        "screen.apexballistics.no_target": "Target grid: UNPROGRAMMED",
        "tooltip.apexballistics.guidance": "Guidance: %s",
        "tooltip.apexballistics.payload": "Payload: %s",
        "tooltip.apexballistics.fuse": "Fuse: %s",
        "tooltip.apexballistics.reliability": "Reliability: %s%%",
        "tooltip.apexballistics.module": "Module: %s / %s",
        "tooltip.apexballistics.waypoints": "Programmed waypoints: %s",
        "tooltip.apexballistics.airburst_height": "Programmed airburst height: %s blocks",
        "tooltip.apexballistics.energy": "Suit energy: %s / %s",
        "tooltip.apexballistics.armor_module": "Installed module: %s",
        "tooltip.apexballistics.heat": "Heat: %s / %s",
        "tooltip.apexballistics.active": "ACTIVE",
        "tooltip.apexballistics.inactive": "Inactive",
        "message.apexballistics.waypoint_added": "Waypoint added: %s, %s, %s",
        "message.apexballistics.airburst_height": "Airburst height set to %s blocks.",
        "message.apexballistics.assembly_no_missile": "Carry a missile to install this module.",
        "message.apexballistics.module_installed": "Installed %s into %s.",
        "message.apexballistics.missile_spec": "Guidance %s | Payload %s | Fuse %s | %s stage(s) | %s%% reliable",
        "message.apexballistics.missile_warning": "MISSILE WARNING: incoming %s",
        "message.apexballistics.jammer_on": "Electronic jammer active.",
        "message.apexballistics.jammer_off": "Electronic jammer off.",
        "message.apexballistics.overheated": "Weapon overheated. Allow it to cool.",
        "message.apexballistics.radar_emp": "Radar disabled by EMP.",
        "message.apexballistics.passive_contacts": "Passive detector: %s missile emissions.",
        "message.apexballistics.system_status": "Energy %s | Integrity %s%% | EMP lockout %s ticks",
        "message.apexballistics.rack_status": "Missile rack: %s × %s",
        "message.apexballistics.maintenance_complete": "Missile serviced to full reliability.",
        "message.apexballistics.armor_module_installed": "Installed armor module: %s",
        "message.apexballistics.maintenance_launchers": "Nearby launcher components repaired.",
        "message.apexballistics.charger_invalid": "Hold Apex armor, a gauss rifle, or a railgun.",
        "message.apexballistics.charged": "Equipment charged and cooled.",
        "message.apexballistics.refinery_input": "Insert blaze powder to refine advanced propellant.",
        "message.apexballistics.crane_needs_missile": "Hold a missile for crane loading.",
        "message.apexballistics.crane_loaded": "Crane loaded a nearby compatible launcher.",
        "message.apexballistics.crane_no_launcher": "No compatible launcher within crane reach.",
        "message.apexballistics.submarine_status": "Submarine control: %s submerged SLBM tube(s) linked.",
        "message.apexballistics.command_status": "Network tracks %s missiles (%s hostile). Faction: %s",
    })
    sound_subtitles = {
        "ballistic_launch": "A heavy missile thunders out of its silo",
        "cruise_launch": "A cruise missile engine ignites",
        "interceptor_launch": "An interceptor missile launches",
        "missile_flight": "A missile engine roars overhead",
        "heavy_explosion": "A strategic warhead detonates",
        "light_explosion": "A missile warhead detonates",
        "radar_servo": "Radar servos sweep",
    }
    for sound, subtitle in sound_subtitles.items():
        lang[f"subtitles.apexballistics.{sound}"] = subtitle
    write_json(lang_path, lang)
    write_json(ASSETS / "sounds.json", {
        name: {
            "subtitle": f"subtitles.apexballistics.{name}",
            "sounds": [{"name": f"apexballistics:{name}", "stream": name in {
                "ballistic_launch", "cruise_launch", "missile_flight",
                "heavy_explosion"
            }}],
        }
        for name in (
            "ballistic_launch", "cruise_launch", "interceptor_launch",
            "missile_flight", "heavy_explosion", "light_explosion", "radar_servo"
        )
    })
    print("generated assets")


if __name__ == "__main__":
    main()
