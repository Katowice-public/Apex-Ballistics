"""Shared IDs for build blocks, facility doors, sirens, and cable hardware."""
from __future__ import annotations

DYE_COLORS = [
    "white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray",
    "light_gray", "cyan", "purple", "blue", "brown", "green", "red", "black",
]

DYE_RGB = {
    "white": (226, 226, 220),
    "orange": (216, 118, 42),
    "magenta": (178, 72, 168),
    "light_blue": (90, 168, 210),
    "yellow": (214, 186, 54),
    "lime": (118, 186, 42),
    "pink": (214, 138, 158),
    "gray": (76, 80, 84),
    "light_gray": (148, 148, 144),
    "cyan": (42, 148, 148),
    "purple": (118, 62, 168),
    "blue": (52, 82, 168),
    "brown": (118, 78, 48),
    "green": (78, 118, 42),
    "red": (168, 52, 48),
    "black": (28, 30, 32),
}

BUILD_FAMILIES = (
    "bunker_concrete",
    "bunker_bricks",
    "bunker_tiles",
    "blast_glass",
    "reinforced_glass",
)

LEGACY_BUILD_BLOCKS = [
    "reinforced_concrete",
    "white_reinforced_concrete",
    "black_reinforced_concrete",
    "olive_reinforced_concrete",
    "hazard_concrete",
    "blast_steel",
    "bunker_glass",
]

STEEL_BLOCKS = (
    [f"steel_plate_{i:02d}" for i in range(1, 9)]
    + [f"steel_panel_{i:02d}" for i in range(1, 9)]
    + [f"floor_marking_{i:02d}" for i in range(1, 9)]
)

# width, height, hatch?, thickness (blocks), depth (hatches only; unused for doors)
FACILITY_DOORS = [
    ("personnel_door", 1, 2, False, 0.14, 1),
    ("blast_door", 1, 2, False, 0.38, 1),
    ("security_door", 1, 2, False, 0.18, 1),
    ("airlock_door", 1, 2, False, 0.28, 1),
    ("silo_hatch", 1, 1, True, 0.28, 1),
    ("submarine_hatch", 1, 1, True, 0.24, 1),
    ("maintenance_hatch", 1, 1, True, 0.12, 1),
    ("bunker_door", 2, 2, False, 0.32, 1),
    ("vault_door", 2, 3, False, 0.42, 1),
    ("vehicle_door", 3, 3, False, 0.28, 1),
    ("silo_blast_leaf", 2, 2, True, 0.30, 2),
    ("hangar_shutter", 3, 2, False, 0.16, 1),
]

SIRENS = [
    ("air_raid_siren", "AIR RAID"),
    ("industrial_siren", "FACTORY"),
    ("nuclear_warning_siren", "CIVIL DEFENSE"),
]


def colored_build_ids() -> list[str]:
    ids = []
    for family in BUILD_FAMILIES:
        for color in DYE_COLORS:
            ids.append(f"{family}_{color}")
    return ids


def new_build_ids() -> list[str]:
    return colored_build_ids() + list(STEEL_BLOCKS)


def all_16_build_ids() -> list[str]:
    return LEGACY_BUILD_BLOCKS + new_build_ids()


def is_glass(block_id: str) -> bool:
    return "glass" in block_id


def door_ids() -> list[str]:
    return [entry[0] for entry in FACILITY_DOORS]


def siren_ids() -> list[str]:
    return [entry[0] for entry in SIRENS]
