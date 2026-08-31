"""Deterministic high-detail Wavefront meshes for Apex Ballistics.

Forge's OBJ loader accepts triangles and quads only. Geometry is built from
cylinders, tapers, hatches, wheels, and airframe details so each launcher and
missile reads as a different real-world system rather than a reskinned cube.
"""
from __future__ import annotations

import math
from contextlib import contextmanager

TAU = math.tau

MATERIALS = """newmtl body
Kd 0.78 0.80 0.83
map_Kd #texture0
newmtl dark
Kd 0.16 0.17 0.19
map_Kd #texture0
newmtl accent
Kd 0.82 0.18 0.12
map_Kd #texture0
newmtl glass
Kd 0.22 0.62 0.78
map_Kd #texture0
newmtl olive
Kd 0.38 0.44 0.26
map_Kd #texture0
newmtl concrete
Kd 0.58 0.56 0.52
map_Kd #texture0
newmtl white
Kd 0.90 0.91 0.93
map_Kd #texture0
newmtl navy
Kd 0.16 0.28 0.42
map_Kd #texture0
newmtl yellow
Kd 0.86 0.70 0.16
map_Kd #texture0
newmtl tire
Kd 0.07 0.07 0.08
map_Kd #texture0
newmtl rust
Kd 0.42 0.22 0.12
map_Kd #texture0
"""


def _norm(v: tuple[float, float, float]) -> tuple[float, float, float]:
    length = math.sqrt(sum(c * c for c in v)) or 1.0
    return tuple(c / length for c in v)


def _add(a, b):
    return (a[0] + b[0], a[1] + b[1], a[2] + b[2])


def _sub(a, b):
    return (a[0] - b[0], a[1] - b[1], a[2] - b[2])


def _mul(a, s):
    return (a[0] * s, a[1] * s, a[2] * s)


def _cross(a, b):
    return (a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0])


def _basis(axis: tuple[float, float, float]):
    axis = _norm(axis)
    ref = (0.0, 1.0, 0.0) if abs(axis[1]) < 0.9 else (1.0, 0.0, 0.0)
    side = _norm(_cross(axis, ref))
    up = _norm(_cross(side, axis))
    return axis, side, up


class ObjBuilder:
    def __init__(self, name: str):
        self.name = name
        self.lines = [f"mtllib {name}.mtl", f"o {name}", "s 1"]
        self.index = 1
        self.material = ""
        self.origin = (0.0, 0.0, 0.0)
        self.scale = 1.0
        self.yaw = 0.0

    @contextmanager
    def at(self, origin, scale: float = 1.0, yaw: float = 0.0):
        previous = (self.origin, self.scale, self.yaw)
        self.origin = _add(self.origin, origin)
        self.scale *= scale
        self.yaw += yaw
        try:
            yield
        finally:
            self.origin, self.scale, self.yaw = previous

    def _xf(self, p):
        x, y, z = p
        if self.yaw:
            c, s = math.cos(self.yaw), math.sin(self.yaw)
            x, z = x * c - z * s, x * s + z * c
        x, y, z = x * self.scale, y * self.scale, z * self.scale
        return _add(self.origin, (x, y, z))

    def use(self, material: str) -> None:
        if self.material != material:
            self.lines.append(f"usemtl {material}")
            self.material = material

    def face(self, points, material: str = "body", uvs=None) -> None:
        self.use(material)
        start = self.index
        n = len(points)
        if uvs is None:
            uvs = [(0.0, 0.0), (1.0, 0.0), (1.0, 1.0), (0.0, 1.0)][:n]
        for x, y, z in points:
            px, py, pz = self._xf((x, y, z))
            self.lines.append(f"v {px:.6f} {py:.6f} {pz:.6f}")
        for u, v in uvs:
            self.lines.append(f"vt {u:.6f} {v:.6f}")
        refs = [f"{start + i}/{start + i}" for i in range(n)]
        self.lines.append("f " + " ".join(refs))
        self.index += n

    def box(self, x0, y0, z0, x1, y1, z1, material: str = "body") -> None:
        self.face([(x0, y0, z0), (x1, y0, z0), (x1, y1, z0), (x0, y1, z0)], material)
        self.face([(x1, y0, z1), (x0, y0, z1), (x0, y1, z1), (x1, y1, z1)], material)
        self.face([(x0, y0, z1), (x0, y0, z0), (x0, y1, z0), (x0, y1, z1)], material)
        self.face([(x1, y0, z0), (x1, y0, z1), (x1, y1, z1), (x1, y1, z0)], material)
        self.face([(x0, y1, z0), (x1, y1, z0), (x1, y1, z1), (x0, y1, z1)], material)
        self.face([(x0, y0, z1), (x1, y0, z1), (x1, y0, z0), (x0, y0, z0)], material)

    def cylinder(self, start, end, radius, segments: int = 28, material: str = "body",
                 caps: bool = True, radius_end=None, v0: float = 0.0, v1: float = 1.0) -> None:
        radius_end = radius if radius_end is None else radius_end
        axis, side, up = _basis(_sub(end, start))

        def ring(center, radius_value, angle):
            return _add(center, _add(_mul(side, radius_value * math.cos(angle)),
                                     _mul(up, radius_value * math.sin(angle))))

        for i in range(segments):
            a0 = TAU * i / segments
            a1 = TAU * (i + 1) / segments
            u0, u1 = i / segments, (i + 1) / segments
            self.face(
                [ring(start, radius, a0), ring(start, radius, a1),
                 ring(end, radius_end, a1), ring(end, radius_end, a0)],
                material,
                uvs=[(u0, v0), (u1, v0), (u1, v1), (u0, v1)],
            )
            if caps:
                self.face([start, ring(start, radius, a1), ring(start, radius, a0)], material)
                self.face([end, ring(end, radius_end, a0), ring(end, radius_end, a1)], material)

    def cone(self, base, tip, radius, segments: int = 28, material: str = "accent") -> None:
        self.cylinder(base, tip, radius, segments, material, caps=True, radius_end=0.001)

    def torus(self, center, normal, major, minor, segs: int = 16, tube: int = 10,
              material: str = "tire") -> None:
        axis, side, up = _basis(normal)
        for i in range(segs):
            a0 = TAU * i / segs
            a1 = TAU * (i + 1) / segs
            for j in range(tube):
                b0 = TAU * j / tube
                b1 = TAU * (j + 1) / tube

                def pt(a, b):
                    radial = _add(_mul(side, math.cos(a)), _mul(up, math.sin(a)))
                    ring = _add(center, _mul(radial, major))
                    return _add(ring, _add(_mul(radial, minor * math.cos(b)),
                                           _mul(axis, minor * math.sin(b))))

                self.face([pt(a0, b0), pt(a1, b0), pt(a1, b1), pt(a0, b1)], material)

    def sphere(self, center, radius, slices: int = 16, stacks: int = 10, material: str = "white") -> None:
        for i in range(stacks):
            v0 = math.pi * i / stacks
            v1 = math.pi * (i + 1) / stacks
            r0, r1 = radius * math.sin(v0), radius * math.sin(v1)
            y0, y1 = center[1] + radius * math.cos(v0), center[1] + radius * math.cos(v1)
            for j in range(slices):
                a0 = TAU * j / slices
                a1 = TAU * (j + 1) / slices
                self.face([
                    (center[0] + r0 * math.cos(a0), y0, center[2] + r0 * math.sin(a0)),
                    (center[0] + r0 * math.cos(a1), y0, center[2] + r0 * math.sin(a1)),
                    (center[0] + r1 * math.cos(a1), y1, center[2] + r1 * math.sin(a1)),
                    (center[0] + r1 * math.cos(a0), y1, center[2] + r1 * math.sin(a0)),
                ], material)

    def dish(self, center, radius, depth, rings: int = 10, segments: int = 36,
             material: str = "body") -> None:
        for ring in range(rings):
            r0 = radius * ring / rings
            r1 = radius * (ring + 1) / rings
            z0 = depth * (r0 / radius) ** 2
            z1 = depth * (r1 / radius) ** 2
            for i in range(segments):
                a0 = TAU * i / segments
                a1 = TAU * (i + 1) / segments
                p00 = (center[0] + r0 * math.cos(a0), center[1] + r0 * math.sin(a0), center[2] + z0)
                p01 = (center[0] + r0 * math.cos(a1), center[1] + r0 * math.sin(a1), center[2] + z0)
                p11 = (center[0] + r1 * math.cos(a1), center[1] + r1 * math.sin(a1), center[2] + z1)
                p10 = (center[0] + r1 * math.cos(a0), center[1] + r1 * math.sin(a0), center[2] + z1)
                self.face([p00, p01, p11, p10], material)
                self.face([p10, p11, p01, p00], "dark")

    def fin(self, y0, y1, span, root, thickness=0.016, sweep=0.04, material: str = "dark") -> None:
        # Four clipped-delta fins in XZ.
        tip_y0 = y0 + sweep
        tip_y1 = y1 - sweep * 0.35
        for sx, sz in ((1, 0), (-1, 0), (0, 1), (0, -1)):
            inner = [(sx * root, y0, sz * root), (sx * root, y1, sz * root)]
            outer = [(sx * span, tip_y0, sz * span), (sx * span, tip_y1, sz * span)]
            t = _mul(_norm((-sz, 0, sx) if sx or sz else (1, 0, 0)), thickness * 0.5)
            pts_a = [_add(inner[0], t), _add(outer[0], t), _add(outer[1], t), _add(inner[1], t)]
            pts_b = [_sub(inner[0], t), _sub(inner[1], t), _sub(outer[1], t), _sub(outer[0], t)]
            self.face(pts_a, material)
            self.face(pts_b, material)
            self.face([pts_a[0], pts_b[0], pts_b[3], pts_a[3]], material)
            self.face([pts_a[1], pts_a[2], pts_b[2], pts_b[1]], material)

    def bolt_ring(self, center, normal, radius, count=8, size=0.012, material: str = "dark") -> None:
        axis, side, up = _basis(normal)
        for i in range(count):
            a = TAU * i / count
            p = _add(center, _add(_mul(side, radius * math.cos(a)), _mul(up, radius * math.sin(a))))
            self.cylinder(p, _add(p, _mul(axis, size * 1.6)), size, 8, material, caps=True)

    def write(self, directory) -> None:
        directory.mkdir(parents=True, exist_ok=True)
        (directory / f"{self.name}.obj").write_text("\n".join(self.lines) + "\n")
        (directory / f"{self.name}.mtl").write_text(MATERIALS)


def add_missile_mesh(mesh: ObjBuilder, kind: str) -> None:
    """Unique airframes: ICBM/SLBM/SRBM ballistic; ALCM/cruise air-breathers; SAM/AAM/interceptor."""
    if kind == "icbm":
        # Minuteman-class three-stage ICBM with raceway and RV, no large fins.
        mesh.cylinder((0, -0.92, 0), (0, -0.84, 0), 0.10, 24, "dark", True, 0.07)
        mesh.cone((0, -0.84, 0), (0, -0.92, 0), 0.055, 20, "accent")
        mesh.cylinder((0, -0.84, 0), (0, -0.22, 0), 0.145, 28, "white", False)
        mesh.cylinder((0, -0.22, 0), (0, -0.14, 0), 0.145, 24, "dark", False, 0.12)
        mesh.cylinder((0, -0.14, 0), (0, 0.28, 0), 0.12, 28, "white", False)
        mesh.cylinder((0, 0.28, 0), (0, 0.34, 0), 0.12, 20, "dark", False, 0.10)
        mesh.cylinder((0, 0.34, 0), (0, 0.62, 0), 0.10, 24, "white", False)
        mesh.cylinder((0, 0.62, 0), (0, 0.70, 0), 0.10, 18, "dark", False)
        mesh.cylinder((0, 0.70, 0), (0, 0.78, 0), 0.088, 20, "body", False)
        mesh.cone((0, 0.78, 0), (0, 0.98, 0), 0.088, 24, "accent")
        mesh.box(0.11, -0.78, -0.018, 0.155, 0.58, 0.018, "dark")
        for y in (-0.70, -0.40, 0.00, 0.40):
            mesh.cylinder((0, y, 0), (0, y + 0.018, 0), 0.152, 28, "accent", False)
        mesh.bolt_ring((0, -0.84, 0), (0, 1, 0), 0.11, 10, 0.01)
    elif kind == "slbm":
        # Trident-class: fat, blunt, rubberized rings, no fins, flared skirt.
        mesh.cylinder((0, -0.82, 0), (0, -0.70, 0), 0.16, 28, "dark", True, 0.12)
        mesh.cylinder((0, -0.70, 0), (0, 0.42, 0), 0.155, 32, "navy", False)
        mesh.cylinder((0, 0.42, 0), (0, 0.62, 0), 0.155, 24, "navy", False, 0.12)
        mesh.cone((0, 0.62, 0), (0, 0.86, 0), 0.12, 28, "body")
        for y in (-0.55, -0.25, 0.05, 0.30):
            mesh.cylinder((0, y, 0), (0, y + 0.03, 0), 0.168, 32, "dark", False)
        mesh.box(-0.02, -0.62, 0.14, 0.02, 0.38, 0.18, "dark")
    elif kind == "srbm":
        # Scud-class tactical ballistic with large rear fins and ogive.
        mesh.cylinder((0, -0.70, 0), (0, -0.62, 0), 0.09, 20, "dark", True, 0.06)
        mesh.cylinder((0, -0.62, 0), (0, 0.28, 0), 0.125, 28, "olive", False)
        mesh.cylinder((0, 0.28, 0), (0, 0.42, 0), 0.125, 20, "olive", False, 0.09)
        mesh.cone((0, 0.42, 0), (0, 0.78, 0), 0.09, 28, "accent")
        mesh.fin(-0.58, -0.18, 0.32, 0.125, 0.02, 0.05, "dark")
        mesh.box(0.10, -0.50, -0.016, 0.14, 0.22, 0.016, "dark")
        mesh.cylinder((0, -0.10, 0), (0, -0.08, 0), 0.135, 28, "yellow", False)
    elif kind == "alcm":
        _cruise_airframe(mesh, inlet_z=-0.08, wing_y=-0.02, length=1.05, olive=True)
    elif kind == "cruise_missile":
        _cruise_airframe(mesh, inlet_z=-0.10, wing_y=-0.06, length=1.18, olive=False)
        mesh.cylinder((0, -0.62, 0), (0, -0.48, 0), 0.07, 20, "dark", True)
    elif kind == "sam":
        mesh.cylinder((0, -0.72, 0), (0, -0.64, 0), 0.055, 18, "dark", True, 0.04)
        mesh.cylinder((0, -0.64, 0), (0, 0.48, 0), 0.072, 24, "white", False)
        mesh.cone((0, 0.48, 0), (0, 0.78, 0), 0.072, 24, "accent")
        mesh.fin(-0.58, -0.28, 0.20, 0.072, 0.012, 0.03, "dark")
        mesh.fin(0.18, 0.38, 0.14, 0.072, 0.010, 0.02, "dark")
        mesh.cylinder((0, 0.10, 0), (0, 0.12, 0), 0.078, 24, "yellow", False)
    elif kind == "aam":
        mesh.cylinder((0, -0.64, 0), (0, -0.56, 0), 0.048, 16, "dark", True, 0.032)
        mesh.cylinder((0, -0.56, 0), (0, 0.42, 0), 0.062, 24, "white", False)
        mesh.cone((0, 0.42, 0), (0, 0.70, 0), 0.062, 22, "glass")
        mesh.box(-0.22, -0.08, -0.008, 0.22, 0.22, 0.008, "dark")
        mesh.box(-0.008, -0.08, -0.22, 0.008, 0.22, 0.22, "dark")
        mesh.fin(-0.52, -0.28, 0.16, 0.062, 0.01, 0.02, "dark")
        mesh.cylinder((0, 0.42, 0), (0, 0.52, 0), 0.012, 10, "dark", True)
    else:  # interceptor / THAAD-like kill vehicle
        mesh.cylinder((0, -0.78, 0), (0, -0.68, 0), 0.07, 20, "dark", True, 0.045)
        mesh.cylinder((0, -0.68, 0), (0, 0.22, 0), 0.078, 26, "white", False)
        mesh.cylinder((0, 0.22, 0), (0, 0.34, 0), 0.078, 18, "dark", False, 0.055)
        mesh.cone((0, 0.34, 0), (0, 0.72, 0), 0.055, 24, "accent")
        mesh.fin(-0.62, -0.38, 0.16, 0.078, 0.01, 0.02, "dark")
        for a in range(8):
            ang = TAU * a / 8
            x, z = math.cos(ang) * 0.06, math.sin(ang) * 0.06
            mesh.cylinder((x, 0.28, z), (x * 1.4, 0.32, z * 1.4), 0.01, 8, "dark", True)


def _cruise_airframe(mesh: ObjBuilder, inlet_z: float, wing_y: float, length: float, olive: bool) -> None:
    body = "olive" if olive else "dark"
    half = length * 0.5
    mesh.cylinder((0, -half + 0.12, 0), (0, half - 0.22, 0), 0.078, 24, body, False)
    mesh.cone((0, half - 0.22, 0), (0, half, 0), 0.078, 22, "glass")
    mesh.cylinder((0, -half, 0), (0, -half + 0.12, 0), 0.06, 18, "dark", True)
    mesh.box(-0.38, wing_y - 0.04, -0.01, 0.38, wing_y + 0.18, 0.01, "dark")
    mesh.box(-0.01, -half + 0.16, -0.16, 0.01, -half + 0.36, 0.16, "dark")
    mesh.box(-0.01, -half + 0.18, 0.06, 0.01, -half + 0.42, 0.18, "dark")
    mesh.box(-0.045, -half + 0.10, inlet_z - 0.10, 0.045, -half + 0.42, inlet_z, "dark")
    mesh.cylinder((0, -half + 0.12, inlet_z - 0.04), (0, -half + 0.40, inlet_z - 0.04),
                  0.028, 14, "dark", True)


def add_launcher_mesh(mesh: ObjBuilder, name: str) -> None:
    if name == "icbm_silo":
        _icbm_silo(mesh)
    elif name == "slbm_tube":
        _slbm_tube(mesh)
    elif name == "cruise_pad":
        _cruise_pad(mesh)
    elif name == "sam_battery":
        _sam_battery(mesh)
    elif name == "mobile_launcher":
        _mobile_launcher(mesh)
    elif name == "vls":
        _vls(mesh)


def _icbm_silo(mesh: ObjBuilder) -> None:
    # Minuteman-style silo headworks: apron, collar, open blast doors, hydraulics, missile.
    mesh.box(0.00, 0.00, 0.00, 1.00, 0.10, 1.00, "concrete")
    mesh.box(0.02, 0.10, 0.02, 0.98, 0.16, 0.98, "dark")
    mesh.cylinder((0.5, 0.14, 0.5), (0.5, 0.28, 0.5), 0.42, 36, "body", False)
    mesh.cylinder((0.5, 0.16, 0.5), (0.5, 0.26, 0.5), 0.34, 32, "dark", False)
    mesh.bolt_ring((0.5, 0.28, 0.5), (0, 1, 0), 0.40, 12, 0.016, "yellow")
    # Open two-leaf blast doors parked to the sides.
    mesh.box(-0.18, 0.16, 0.18, 0.10, 0.22, 0.82, "dark")
    mesh.box(0.90, 0.16, 0.18, 1.18, 0.22, 0.82, "dark")
    for z in (0.22, 0.78):
        mesh.cylinder((0.12, 0.18, z), (0.12, 0.42, z), 0.03, 10, "yellow", True)
        mesh.cylinder((0.88, 0.18, z), (0.88, 0.42, z), 0.03, 10, "yellow", True)
    # Umbilical mast and vents.
    mesh.box(0.86, 0.16, 0.44, 0.94, 1.05, 0.56, "body")
    mesh.box(0.84, 0.70, 0.42, 0.96, 0.78, 0.50, "accent")
    mesh.cylinder((0.16, 0.16, 0.16), (0.16, 0.55, 0.16), 0.04, 12, "dark", True)
    mesh.cylinder((0.16, 0.55, 0.16), (0.16, 0.62, 0.16), 0.055, 12, "dark", True)
    mesh.box(0.08, 0.16, 0.70, 0.14, 0.72, 0.92, "dark")
    for y in range(4):
        mesh.box(0.46, 0.20 + y * 0.18, 0.14, 0.54, 0.24 + y * 0.18, 0.18, "yellow")
    with mesh.at((0.5, 0.95, 0.5), scale=0.55):
        add_missile_mesh(mesh, "icbm")


def _slbm_tube(mesh: ObjBuilder) -> None:
    # Ohio-class muzzle hatch and inner tube.
    mesh.cylinder((0.5, 0.00, 0.5), (0.5, 0.08, 0.5), 0.48, 36, "navy", True)
    mesh.cylinder((0.5, 0.06, 0.5), (0.5, 1.15, 0.5), 0.32, 36, "dark", False)
    mesh.cylinder((0.5, 0.10, 0.5), (0.5, 1.18, 0.5), 0.24, 32, "body", False)
    for y in (0.28, 0.62, 0.96):
        mesh.cylinder((0.5, y, 0.5), (0.5, y + 0.04, 0.5), 0.34, 32, "accent", False)
    # Clamshell hatch open.
    mesh.box(0.18, 1.12, 0.48, 0.82, 1.18, 1.05, "navy")
    mesh.cylinder((0.5, 1.14, 0.50), (0.5, 1.14, 1.02), 0.03, 10, "dark", True)
    mesh.box(0.10, 0.08, 0.10, 0.22, 0.22, 0.90, "dark")
    mesh.box(0.78, 0.08, 0.10, 0.90, 0.22, 0.90, "dark")
    mesh.torus((0.5, 0.20, 0.5), (0, 1, 0), 0.36, 0.03, 20, 8, "yellow")
    with mesh.at((0.5, 0.55, 0.5), scale=0.62):
        add_missile_mesh(mesh, "slbm")


def _cruise_pad(mesh: ObjBuilder) -> None:
    # Inclined rail TEL / pad with blast deflector.
    mesh.box(0.00, 0.00, 0.00, 1.00, 0.10, 1.00, "concrete")
    mesh.box(0.04, 0.10, 0.08, 0.96, 0.16, 0.92, "dark")
    mesh.box(0.18, 0.16, 0.72, 0.82, 0.28, 0.88, "body")
    mesh.box(0.22, 0.22, 0.20, 0.34, 0.34, 0.86, "dark")
    mesh.box(0.66, 0.22, 0.20, 0.78, 0.34, 0.86, "dark")
    # Inclined I-beam rail.
    mesh.box(0.46, 0.28, 0.78, 0.54, 0.40, 0.86, "yellow")
    mesh.box(0.44, 0.30, 0.18, 0.56, 0.42, 0.82, "body")
    mesh.box(0.08, 0.10, 0.78, 0.20, 0.62, 0.96, "dark")
    mesh.box(0.80, 0.10, 0.78, 0.92, 0.62, 0.96, "dark")
    mesh.box(0.04, 0.10, 0.86, 0.96, 0.55, 0.98, "dark")
    mesh.box(0.06, 0.16, 0.06, 0.22, 0.38, 0.22, "olive")
    mesh.box(0.08, 0.38, 0.08, 0.20, 0.50, 0.20, "glass")
    for z in (0.30, 0.48, 0.66):
        mesh.cylinder((0.50, 0.34, z), (0.50, 0.42, z), 0.03, 10, "yellow", True)
    with mesh.at((0.50, 0.62, 0.48), scale=0.55, yaw=0.55):
        add_missile_mesh(mesh, "cruise_missile")


def _sam_battery(mesh: ObjBuilder) -> None:
    # Patriot-style four-canister box launcher on a turntable.
    mesh.box(0.04, 0.00, 0.04, 0.96, 0.14, 0.96, "olive")
    mesh.box(0.08, 0.14, 0.08, 0.36, 0.40, 0.40, "dark")
    mesh.cylinder((0.5, 0.14, 0.5), (0.5, 0.26, 0.5), 0.28, 28, "body", True)
    mesh.box(0.18, 0.24, 0.22, 0.82, 0.38, 0.78, "olive")
    mesh.box(0.22, 0.36, 0.30, 0.78, 0.48, 0.70, "dark")
    for i, x in enumerate((0.32, 0.46, 0.60, 0.74)):
        mesh.box(x - 0.07, 0.44, 0.34, x + 0.07, 1.18, 0.66, "olive")
        mesh.box(x - 0.065, 1.14, 0.36, x + 0.065, 1.22, 0.64, "accent")
        mesh.box(x - 0.02, 0.50, 0.32, x + 0.02, 1.10, 0.34, "yellow")
        mesh.cylinder((x, 0.50, 0.50), (x, 1.12, 0.50), 0.045, 16, "white", False)
        mesh.cone((x, 1.12, 0.50), (x, 1.20, 0.50), 0.045, 14, "accent")
    mesh.box(0.24, 0.44, 0.28, 0.28, 1.10, 0.72, "dark")
    mesh.box(0.72, 0.44, 0.28, 0.76, 1.10, 0.72, "dark")
    mesh.cylinder((0.50, 0.38, 0.72), (0.50, 0.86, 0.50), 0.03, 10, "dark", True)
    mesh.box(0.70, 0.14, 0.08, 0.92, 0.34, 0.30, "dark")
    mesh.cylinder((0.82, 0.34, 0.18), (0.82, 0.62, 0.18), 0.02, 8, "yellow", True)
    mesh.box(0.78, 0.62, 0.14, 0.86, 0.70, 0.26, "accent")
    mesh.bolt_ring((0.5, 0.14, 0.5), (0, 1, 0), 0.40, 10, 0.012, "yellow")


def _mobile_launcher(mesh: ObjBuilder) -> None:
    # Wheeled TEL: cab, 6x wheels, erector with two ballistic rounds.
    mesh.box(0.06, 0.22, 0.12, 0.94, 0.40, 0.88, "olive")
    mesh.box(0.58, 0.40, 0.18, 0.94, 0.78, 0.82, "olive")
    mesh.box(0.62, 0.52, 0.22, 0.92, 0.72, 0.78, "glass")
    mesh.box(0.86, 0.40, 0.34, 0.98, 0.50, 0.42, "yellow")
    mesh.box(0.86, 0.40, 0.58, 0.98, 0.50, 0.66, "yellow")
    mesh.box(0.10, 0.40, 0.22, 0.22, 0.58, 0.30, "dark")
    mesh.cylinder((0.16, 0.58, 0.26), (0.16, 0.78, 0.26), 0.03, 8, "dark", True)
    for x in (0.20, 0.48, 0.78):
        for z, s in ((0.10, 1), (0.90, -1)):
            mesh.torus((x, 0.16, z), (0, 0, 1), 0.10, 0.035, 14, 8, "tire")
            mesh.cylinder((x, 0.16, z - 0.04 * s), (x, 0.16, z + 0.04 * s), 0.045, 10, "dark", True)
    mesh.box(0.04, 0.08, 0.18, 0.12, 0.16, 0.40, "dark")
    mesh.box(0.04, 0.08, 0.60, 0.12, 0.16, 0.82, "dark")
    mesh.box(0.16, 0.38, 0.36, 0.70, 0.48, 0.64, "dark")
    mesh.cylinder((0.26, 0.46, 0.42), (0.18, 1.22, 0.30), 0.07, 18, "olive", False)
    mesh.cylinder((0.42, 0.46, 0.58), (0.34, 1.22, 0.46), 0.07, 18, "olive", False)
    with mesh.at((0.22, 0.88, 0.36), scale=0.40, yaw=-0.4):
        add_missile_mesh(mesh, "srbm")
    with mesh.at((0.38, 0.88, 0.52), scale=0.40, yaw=-0.4):
        add_missile_mesh(mesh, "srbm")


def _vls(mesh: ObjBuilder) -> None:
    # Mk 41-style 2x2 cells with hatches, uptake, and deck bolts.
    mesh.box(0.00, 0.00, 0.00, 1.00, 0.52, 1.00, "dark")
    mesh.box(0.02, 0.52, 0.02, 0.98, 0.58, 0.98, "body")
    cells = ((0.26, 0.26), (0.74, 0.26), (0.26, 0.74), (0.74, 0.74))
    for i, (x, z) in enumerate(cells):
        mesh.box(x - 0.18, 0.56, z - 0.18, x + 0.18, 0.64, z + 0.18, "dark")
        if i < 2:
            mesh.box(x - 0.16, 0.64, z - 0.16, x + 0.16, 0.70, z + 0.16, "yellow")
        else:
            mesh.box(x - 0.16, 0.64, z - 0.02, x + 0.22, 0.72, z + 0.16, "body")
            mesh.cylinder((x, 0.58, z), (x, 0.92, z), 0.07, 16, "white", False)
            mesh.cone((x, 0.92, z), (x, 1.08, z), 0.07, 16, "accent")
    mesh.box(0.44, 0.58, 0.44, 0.56, 0.92, 0.56, "dark")
    mesh.box(0.42, 0.90, 0.42, 0.58, 0.98, 0.58, "accent")
    mesh.bolt_ring((0.5, 0.58, 0.5), (0, 1, 0), 0.46, 16, 0.012, "yellow")
    mesh.box(0.02, 0.58, 0.46, 0.10, 0.66, 0.54, "accent")


def add_radar_base_mesh(mesh: ObjBuilder) -> None:
    mesh.box(0.10, 0.00, 0.10, 0.90, 0.14, 0.90, "dark")
    mesh.box(0.18, 0.14, 0.18, 0.46, 0.42, 0.46, "olive")
    mesh.box(0.22, 0.28, 0.22, 0.42, 0.40, 0.42, "glass")
    mesh.cylinder((0.5, 0.12, 0.5), (0.5, 0.78, 0.5), 0.12, 24, "body", True)
    mesh.cylinder((0.5, 0.76, 0.5), (0.5, 0.88, 0.5), 0.22, 28, "dark", True)
    mesh.box(0.42, 0.86, 0.42, 0.58, 0.96, 0.58, "yellow")
    mesh.cylinder((0.78, 0.14, 0.78), (0.78, 0.50, 0.78), 0.04, 10, "dark", True)
    mesh.box(0.62, 0.14, 0.62, 0.90, 0.28, 0.90, "body")


def add_radar_dish_mesh(mesh: ObjBuilder) -> None:
    mesh.dish((0.0, 0.0, 0.0), 0.48, 0.36, rings=12, segments=40, material="white")
    mesh.cylinder((0, 0, 0.08), (0, 0, 0.62), 0.022, 14, "accent", True)
    mesh.sphere((0, 0, 0.64), 0.045, 12, 8, "accent")
    for a in (0.0, TAU / 3, 2 * TAU / 3):
        x, y = math.cos(a) * 0.38, math.sin(a) * 0.38
        mesh.cylinder((x, y, 0.22), (0, 0, 0.58), 0.012, 8, "dark", True)
    mesh.box(-0.08, -0.08, -0.06, 0.08, 0.08, 0.10, "dark")
    mesh.cylinder((0, 0, -0.18), (0, 0, 0.02), 0.05, 14, "body", True)
    mesh.box(-0.16, -0.04, -0.22, 0.16, 0.04, -0.10, "dark")


def add_system_mesh(mesh: ObjBuilder, name: str) -> None:
    if name == "ciws":
        mesh.box(0.12, 0.00, 0.12, 0.88, 0.18, 0.88, "dark")
        mesh.cylinder((0.5, 0.16, 0.5), (0.5, 0.42, 0.5), 0.22, 24, "white", True)
        mesh.box(0.32, 0.40, 0.28, 0.68, 0.72, 0.70, "white")
        mesh.sphere((0.5, 0.88, 0.5), 0.16, 14, 10, "white")
        mesh.box(0.18, 0.42, 0.38, 0.34, 0.70, 0.62, "dark")
        for i in range(6):
            a = TAU * i / 6
            x, y = 0.50 + math.cos(a) * 0.05, 0.56 + math.sin(a) * 0.05
            mesh.cylinder((x, y, 0.68), (x, y, 1.05), 0.012, 8, "dark", True)
        mesh.cylinder((0.5, 0.56, 0.68), (0.5, 0.56, 0.86), 0.05, 12, "dark", True)
    elif name == "laser_defense":
        mesh.box(0.10, 0.00, 0.10, 0.90, 0.22, 0.90, "dark")
        mesh.box(0.18, 0.22, 0.18, 0.82, 0.55, 0.70, "body")
        mesh.cylinder((0.5, 0.52, 0.48), (0.5, 0.78, 0.48), 0.12, 20, "dark", True)
        mesh.cylinder((0.5, 0.70, 0.48), (0.5, 0.70, 1.05), 0.08, 20, "white", True)
        mesh.cylinder((0.5, 0.70, 1.02), (0.5, 0.70, 1.12), 0.10, 16, "glass", True)
        mesh.box(0.22, 0.22, 0.72, 0.40, 0.48, 0.88, "accent")
        for x in (0.28, 0.40, 0.52, 0.64):
            mesh.box(x, 0.55, 0.22, x + 0.06, 0.82, 0.28, "yellow")
    elif name == "passive_radar":
        mesh.box(0.30, 0.00, 0.30, 0.70, 0.12, 0.70, "dark")
        mesh.cylinder((0.5, 0.10, 0.5), (0.5, 1.05, 0.5), 0.05, 14, "body", True)
        mesh.box(0.22, 0.70, 0.46, 0.78, 1.00, 0.54, "dark")
        for i in range(7):
            z = 0.20 + i * 0.08
            mesh.box(0.18, 0.78, z, 0.82, 0.82, z + 0.02, "yellow")
        mesh.box(0.42, 0.12, 0.42, 0.58, 0.32, 0.58, "olive")
    elif name == "command_console":
        mesh.box(0.08, 0.00, 0.18, 0.92, 0.12, 0.92, "dark")
        mesh.box(0.10, 0.12, 0.28, 0.90, 0.46, 0.88, "body")
        mesh.box(0.14, 0.46, 0.52, 0.38, 0.82, 0.78, "glass")
        mesh.box(0.40, 0.48, 0.50, 0.60, 0.88, 0.78, "glass")
        mesh.box(0.62, 0.46, 0.52, 0.86, 0.82, 0.78, "glass")
        mesh.box(0.16, 0.46, 0.32, 0.84, 0.50, 0.50, "dark")
        mesh.box(0.30, 0.00, 0.00, 0.70, 0.08, 0.22, "dark")
        for x in range(5):
            mesh.box(0.18 + x * 0.14, 0.46, 0.34, 0.28 + x * 0.14, 0.50, 0.44, "yellow")
        mesh.cylinder((0.18, 0.12, 0.70), (0.18, 0.70, 0.70), 0.03, 10, "accent", True)
    elif name == "submarine_control":
        mesh.box(0.10, 0.00, 0.16, 0.90, 0.40, 0.88, "navy")
        mesh.cylinder((0.5, 0.40, 0.58), (0.5, 1.20, 0.58), 0.06, 16, "body", True)
        mesh.cylinder((0.5, 1.18, 0.58), (0.5, 1.28, 0.58), 0.09, 16, "glass", True)
        mesh.torus((0.28, 0.48, 0.38), (0, 0, 1), 0.08, 0.018, 12, 8, "yellow")
        mesh.torus((0.72, 0.48, 0.38), (0, 0, 1), 0.08, 0.018, 12, 8, "yellow")
        mesh.box(0.20, 0.40, 0.22, 0.80, 0.52, 0.48, "glass")
    elif name == "missile_rack":
        mesh.box(0.08, 0.00, 0.08, 0.20, 0.90, 0.20, "dark")
        mesh.box(0.80, 0.00, 0.08, 0.92, 0.90, 0.20, "dark")
        mesh.box(0.08, 0.00, 0.80, 0.20, 0.90, 0.92, "dark")
        mesh.box(0.80, 0.00, 0.80, 0.92, 0.90, 0.92, "dark")
        mesh.box(0.08, 0.82, 0.08, 0.92, 0.90, 0.92, "olive")
        for z in (0.28, 0.50, 0.72):
            mesh.cylinder((0.18, 0.38, z), (0.82, 0.38, z), 0.05, 14, "white", True)
            mesh.cone((0.82, 0.38, z), (0.96, 0.38, z), 0.05, 12, "accent")
    elif name == "loading_crane":
        mesh.box(0.20, 0.00, 0.20, 0.80, 0.16, 0.80, "dark")
        mesh.box(0.38, 0.16, 0.38, 0.62, 0.70, 0.62, "yellow")
        mesh.cylinder((0.50, 0.16, 0.50), (0.50, 0.70, 0.50), 0.08, 16, "body", True)
        for i in range(6):
            z0 = 0.20 + i * 0.16
            mesh.box(0.42, 0.70, z0, 0.58, 0.84, z0 + 0.04, "yellow")
        mesh.box(0.42, 0.70, 0.20, 0.58, 0.82, 1.18, "yellow")
        mesh.box(0.46, 0.52, 1.05, 0.54, 0.72, 1.12, "dark")
        mesh.cylinder((0.5, 0.20, 1.08), (0.5, 0.52, 1.08), 0.015, 8, "dark", True)
        mesh.box(0.46, 0.16, 1.04, 0.54, 0.22, 1.12, "accent")
        mesh.box(0.28, 0.16, 0.28, 0.48, 0.40, 0.50, "glass")
    elif name == "propellant_refinery":
        mesh.box(0.05, 0.00, 0.05, 0.95, 0.10, 0.95, "concrete")
        mesh.cylinder((0.30, 0.10, 0.45), (0.30, 0.95, 0.45), 0.18, 24, "olive", True)
        mesh.cylinder((0.70, 0.10, 0.45), (0.70, 0.78, 0.45), 0.16, 24, "dark", True)
        mesh.cylinder((0.30, 0.78, 0.45), (0.70, 0.82, 0.45), 0.04, 10, "dark", True)
        mesh.cylinder((0.82, 0.10, 0.78), (0.82, 1.20, 0.78), 0.07, 16, "body", True)
        mesh.box(0.08, 0.10, 0.70, 0.28, 0.40, 0.90, "yellow")
        mesh.box(0.10, 0.40, 0.18, 0.90, 0.46, 0.28, "dark")
    elif name == "maintenance_station":
        mesh.box(0.05, 0.00, 0.20, 0.95, 0.42, 0.90, "dark")
        mesh.box(0.08, 0.42, 0.24, 0.92, 0.48, 0.86, "body")
        mesh.box(0.10, 0.00, 0.05, 0.38, 0.36, 0.22, "olive")
        mesh.box(0.62, 0.48, 0.40, 0.90, 0.95, 0.70, "yellow")
        mesh.cylinder((0.76, 0.70, 0.40), (0.76, 0.70, 0.22), 0.03, 8, "dark", True)
        mesh.box(0.20, 0.48, 0.30, 0.48, 0.72, 0.78, "white")
        mesh.box(0.70, 0.00, 0.05, 0.92, 0.28, 0.20, "accent")
    elif name == "capacitor_charger":
        mesh.box(0.12, 0.00, 0.12, 0.88, 0.20, 0.88, "dark")
        mesh.box(0.22, 0.20, 0.22, 0.78, 0.62, 0.70, "body")
        mesh.cylinder((0.38, 0.62, 0.46), (0.38, 1.05, 0.46), 0.08, 16, "yellow", True)
        mesh.cylinder((0.62, 0.62, 0.46), (0.62, 1.05, 0.46), 0.08, 16, "yellow", True)
        mesh.cylinder((0.38, 1.02, 0.46), (0.62, 1.08, 0.46), 0.03, 8, "accent", True)
        mesh.box(0.18, 0.20, 0.72, 0.40, 0.48, 0.90, "glass")
    elif name == "missile_assembly":
        mesh.box(0.00, 0.00, 0.10, 1.00, 0.12, 0.90, "dark")
        mesh.box(0.08, 0.12, 0.18, 0.18, 0.55, 0.82, "olive")
        mesh.box(0.82, 0.12, 0.18, 0.92, 0.55, 0.82, "olive")
        mesh.box(0.08, 0.50, 0.40, 0.92, 0.58, 0.60, "yellow")
        with mesh.at((0.5, 0.42, 0.5), scale=0.48, yaw=1.57):
            add_missile_mesh(mesh, "srbm")
        mesh.box(0.20, 0.12, 0.12, 0.42, 0.28, 0.22, "accent")
        mesh.cylinder((0.16, 0.58, 0.50), (0.84, 0.72, 0.50), 0.02, 8, "dark", True)
    else:
        mesh.box(0.1, 0.0, 0.1, 0.9, 0.8, 0.9, "body")


def add_handheld_mesh(mesh: ObjBuilder, name: str) -> None:
    if name == "manpads":
        mesh.cylinder((0.0, 0.08, -0.55), (0.0, 0.08, 0.70), 0.07, 20, "olive", True)
        mesh.cylinder((0.0, 0.08, 0.62), (0.0, 0.08, 0.82), 0.085, 16, "dark", True)
        mesh.box(-0.03, -0.12, -0.05, 0.03, 0.08, 0.12, "dark")
        mesh.box(-0.02, 0.12, 0.10, 0.02, 0.22, 0.28, "glass")
        mesh.box(0.06, 0.00, -0.10, 0.16, 0.10, 0.18, "olive")
        mesh.cylinder((0.0, 0.08, -0.55), (0.0, 0.08, -0.42), 0.05, 12, "accent", True)
    elif name == "gauss_rifle":
        mesh.box(-0.05, 0.00, -0.20, 0.05, 0.10, 0.85, "dark")
        mesh.cylinder((0.0, 0.06, 0.20), (0.0, 0.06, 0.95), 0.035, 14, "body", True)
        for z in (0.30, 0.42, 0.54, 0.66, 0.78):
            mesh.cylinder((0.0, 0.06, z), (0.0, 0.06, z + 0.04), 0.055, 12, "accent", False)
        mesh.box(-0.03, -0.12, -0.05, 0.03, 0.02, 0.10, "dark")
        mesh.box(-0.04, 0.08, -0.18, 0.04, 0.16, 0.05, "glass")
        mesh.box(-0.06, 0.00, 0.40, 0.06, 0.14, 0.55, "yellow")
    elif name == "railgun":
        mesh.box(-0.08, 0.00, -0.15, 0.08, 0.12, 0.70, "dark")
        mesh.box(-0.06, 0.10, 0.10, -0.02, 0.16, 0.95, "accent")
        mesh.box(0.02, 0.10, 0.10, 0.06, 0.16, 0.95, "accent")
        mesh.box(-0.07, 0.04, 0.20, 0.07, 0.10, 0.55, "glass")
        mesh.box(-0.03, -0.14, -0.02, 0.03, 0.02, 0.12, "dark")
        mesh.box(-0.05, 0.12, -0.12, 0.05, 0.22, 0.08, "body")
    elif name == "plasma_blade":
        mesh.box(-0.03, -0.05, -0.12, 0.03, 0.05, 0.22, "dark")
        mesh.box(-0.05, -0.04, 0.18, 0.05, 0.04, 0.28, "accent")
        mesh.box(-0.02, -0.02, 0.26, 0.02, 0.02, 0.95, "glass")
        mesh.cylinder((0.0, 0.0, 0.22), (0.0, 0.0, 0.28), 0.04, 10, "yellow", True)
    elif name == "targeting_tablet":
        mesh.box(-0.22, -0.02, -0.32, 0.22, 0.03, 0.32, "dark")
        mesh.box(-0.18, 0.03, -0.26, 0.18, 0.04, 0.26, "glass")
        mesh.box(-0.20, -0.03, -0.28, 0.20, -0.02, 0.28, "body")
        mesh.cylinder((0.16, 0.00, 0.28), (0.16, 0.06, 0.28), 0.02, 8, "accent", True)
    elif name == "jammer":
        mesh.box(-0.16, 0.00, -0.12, 0.16, 0.28, 0.16, "olive")
        mesh.cylinder((0.0, 0.28, 0.00), (0.0, 0.72, 0.00), 0.02, 8, "yellow", True)
        mesh.box(-0.12, 0.08, -0.14, 0.12, 0.22, -0.10, "glass")
        mesh.box(-0.18, 0.00, -0.08, -0.14, 0.12, 0.12, "dark")
    else:
        mesh.box(-0.1, 0.0, -0.3, 0.1, 0.1, 0.3, "body")


OBJ_BLOCKS = {
    "icbm_silo", "slbm_tube", "cruise_pad", "sam_battery", "mobile_launcher", "vls",
    "radar", "ciws", "laser_defense", "passive_radar", "command_console",
    "submarine_control", "missile_rack", "loading_crane", "propellant_refinery",
    "maintenance_station", "capacitor_charger", "missile_assembly",
}

HANDHELD_OBJ = {
    "manpads", "gauss_rifle", "railgun", "plasma_blade", "targeting_tablet", "jammer",
}

MISSILE_OBJ = {
    "icbm", "slbm", "srbm", "alcm", "cruise_missile", "sam", "aam", "interceptor",
}
