#!/usr/bin/env python3
"""Orthographic previews of OBJ meshes for walkthrough artifacts."""
from __future__ import annotations

import math
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parent))
from textures import write_png

ROOT = Path("/workspace/src/main/resources/assets/apexballistics/models")
OUT = Path("/tmp/mesh-previews")
COLORS = {
    "body": (180, 186, 192),
    "dark": (48, 50, 54),
    "accent": (200, 52, 40),
    "glass": (70, 170, 200),
    "olive": (92, 108, 62),
    "concrete": (150, 146, 138),
    "white": (228, 230, 234),
    "navy": (48, 78, 112),
    "yellow": (214, 176, 48),
    "tire": (24, 24, 26),
    "rust": (120, 64, 36),
}


def parse_obj(path: Path):
    verts = []
    faces = []
    material = "body"
    for line in path.read_text().splitlines():
        if line.startswith("v "):
            _, x, y, z = line.split()[:4]
            verts.append((float(x), float(y), float(z)))
        elif line.startswith("usemtl "):
            material = line.split()[1]
        elif line.startswith("f "):
            idx = [int(p.split("/")[0]) - 1 for p in line.split()[1:]]
            faces.append((idx, material))
    return verts, faces


def render(path: Path, size=512):
    verts, faces = parse_obj(path)
    if not verts:
        return
    # Isometric-ish rotation.
    ang = math.radians(38)
    ca, sa = math.cos(ang), math.sin(ang)
    rot = []
    minx = miny = minz = 1e9
    maxx = maxy = maxz = -1e9
    for x, y, z in verts:
        # yaw then pitch
        x2 = x * ca - z * sa
        z2 = x * sa + z * ca
        y2 = y * 0.92 - z2 * 0.35
        z3 = y * 0.35 + z2 * 0.92
        rot.append((x2, y2, z3))
        minx, maxx = min(minx, x2), max(maxx, x2)
        miny, maxy = min(miny, y2), max(maxy, y2)
        minz, maxz = min(minz, z3), max(maxz, z3)
    span = max(maxx - minx, maxy - miny, 0.01)
    scale = (size - 48) / span
    cx = (minx + maxx) * 0.5
    cy = (miny + maxy) * 0.5
    zbuf = [-1e9] * (size * size)
    buf = bytearray(size * size * 4)
    bg = (18, 22, 28, 255)
    for i in range(0, len(buf), 4):
        buf[i:i + 4] = bytes(bg)

    def put(x, y, z, color):
        if 0 <= x < size and 0 <= y < size:
            i = y * size + x
            if z >= zbuf[i]:
                zbuf[i] = z
                o = i * 4
                buf[o:o + 4] = bytes((*color, 255))

    def shade(color, z):
        t = 0.55 + 0.45 * ((z - minz) / (maxz - minz + 1e-6))
        return tuple(max(0, min(255, int(c * t))) for c in color)

    for idx, material in faces:
        pts = [rot[i] for i in idx]
        color = COLORS.get(material, (160, 160, 160))
        xs = [int((p[0] - cx) * scale + size * 0.5) for p in pts]
        ys = [int(size * 0.55 - (p[1] - cy) * scale) for p in pts]
        zs = [p[2] for p in pts]
        min_xi, max_xi = max(0, min(xs)), min(size - 1, max(xs))
        min_yi, max_yi = max(0, min(ys)), min(size - 1, max(ys))
        if max_xi <= min_xi or max_yi <= min_yi:
            continue
        # barycentric fill for triangles; fan quads
        tris = [idx] if len(idx) == 3 else [idx[:3], [idx[0], idx[2], idx[3]]] if len(idx) == 4 else []
        screens = list(zip(xs, ys, zs))
        fan = []
        if len(screens) == 3:
            fan = [screens]
        elif len(screens) == 4:
            fan = [screens[:3], [screens[0], screens[2], screens[3]]]
        for tri in fan:
            (x0, y0, z0), (x1, y1, z1), (x2, y2, z2) = tri
            area = (x1 - x0) * (y2 - y0) - (x2 - x0) * (y1 - y0)
            if area == 0:
                continue
            for y in range(min(y0, y1, y2), max(y0, y1, y2) + 1):
                for x in range(min(x0, x1, x2), max(x0, x1, x2) + 1):
                    w0 = (x1 - x) * (y2 - y) - (x2 - x) * (y1 - y)
                    w1 = (x2 - x) * (y0 - y) - (x0 - x) * (y2 - y)
                    w2 = (x0 - x) * (y1 - y) - (x1 - x) * (y0 - y)
                    if area < 0:
                        w0, w1, w2, area = -w0, -w1, -w2, -area
                    if w0 >= 0 and w1 >= 0 and w2 >= 0:
                        a = w0 / area
                        b = w1 / area
                        c = w2 / area
                        z = a * z0 + b * z1 + c * z2
                        put(x, y, z, shade(color, z))
    OUT.mkdir(parents=True, exist_ok=True)
    write_png(OUT / (path.stem + ".png"), size, size, bytes(buf))


def main():
    targets = [
        "block/icbm_silo.obj", "block/slbm_tube.obj", "block/cruise_pad.obj",
        "block/sam_battery.obj", "block/mobile_launcher.obj", "block/vls.obj",
        "block/radar.obj", "block/ciws.obj", "block/laser_defense.obj",
        "block/silo_hatch_bottom.obj", "block/blast_door_bottom_left.obj",
        "item/icbm.obj", "item/slbm.obj", "item/srbm.obj", "item/alcm.obj",
        "item/cruise_missile.obj", "item/sam.obj", "item/aam.obj",
        "item/interceptor.obj", "item/radar_dish_component.obj", "item/manpads.obj",
        "item/ciws.obj", "item/warhead.obj", "item/mirv_warhead.obj",
        "item/apex_helmet.obj", "item/ciws_turret_component.obj",
    ]
    for rel in targets:
        render(ROOT / rel)
        print("preview", rel)
    print("wrote", OUT)


if __name__ == "__main__":
    main()
