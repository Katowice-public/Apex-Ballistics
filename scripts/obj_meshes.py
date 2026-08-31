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


def _scale3(scale) -> tuple[float, float, float]:
    if isinstance(scale, (int, float)):
        value = float(scale)
        return (value, value, value)
    return (float(scale[0]), float(scale[1]), float(scale[2]))


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
        self.scale = (1.0, 1.0, 1.0)
        self.yaw = 0.0
        self.pitch = 0.0

    @contextmanager
    def at(self, origin, scale=1.0, yaw: float = 0.0, pitch: float = 0.0):
        previous = (self.origin, self.scale, self.yaw, self.pitch)
        sx, sy, sz = _scale3(scale)
        self.origin = _add(self.origin, origin)
        self.scale = (self.scale[0] * sx, self.scale[1] * sy, self.scale[2] * sz)
        self.yaw += yaw
        self.pitch += pitch
        try:
            yield
        finally:
            self.origin, self.scale, self.yaw, self.pitch = previous

    def _xf(self, p):
        x, y, z = p
        if self.pitch:
            c, s = math.cos(self.pitch), math.sin(self.pitch)
            y, z = y * c - z * s, y * s + z * c
        if self.yaw:
            c, s = math.cos(self.yaw), math.sin(self.yaw)
            x, z = x * c - z * s, x * s + z * c
        x, y, z = x * self.scale[0], y * self.scale[1], z * self.scale[2]
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


# Unscaled airframes are ~0.3–0.8 across and ~1.2–1.9 long. World missiles must be
# more than 3 blocks high AND more than 3 blocks thick.
MISSILE_WORLD_SCALE = {
    "icbm": (11.2, 2.85, 11.2),
    "slbm": (10.4, 3.05, 10.4),
    "srbm": (11.0, 2.70, 11.0),
    "alcm": (4.80, 3.20, 9.40),
    "cruise_missile": (4.50, 3.15, 9.20),
    "sam": (8.60, 2.55, 8.60),
    "aam": (9.80, 2.70, 9.80),
    "interceptor": (10.4, 2.60, 10.4),
}


def add_missile_mesh(mesh: ObjBuilder, kind: str) -> None:
    """World/item missiles are more than 3 blocks high and more than 3 blocks thick."""
    if kind == "strike_drone":
        add_strike_drone_mesh(mesh)
        return
    with mesh.at((0.0, 0.0, 0.0), scale=MISSILE_WORLD_SCALE.get(kind, (10.0, 2.7, 10.0))):
        _add_missile_airframe(mesh, kind)


def _add_missile_airframe(mesh: ObjBuilder, kind: str) -> None:
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


def _vls(mesh: ObjBuilder) -> None:
    # Mk 41-style 2x2 cells with hatches, uptake, and deck bolts.
    mesh.box(0.00, 0.00, 0.00, 1.00, 0.52, 1.00, "dark")
    mesh.box(0.02, 0.52, 0.02, 0.98, 0.58, 0.98, "body")
    cells = ((0.26, 0.26), (0.74, 0.26), (0.26, 0.74), (0.74, 0.74))
    for i, (x, z) in enumerate(cells):
        mesh.box(x - 0.18, 0.56, z - 0.18, x + 0.18, 0.64, z + 0.18, "dark")
        mesh.box(x - 0.16, 0.64, z - 0.16, x + 0.16, 0.70, z + 0.16, "yellow")
    mesh.box(0.44, 0.58, 0.44, 0.56, 0.92, 0.56, "dark")
    mesh.box(0.42, 0.90, 0.42, 0.58, 0.98, 0.58, "accent")
    mesh.bolt_ring((0.5, 0.58, 0.5), (0, 1, 0), 0.46, 16, 0.012, "yellow")
    mesh.box(0.02, 0.58, 0.46, 0.10, 0.66, 0.54, "accent")


def add_radar_base_mesh(mesh: ObjBuilder) -> None:
    """Two-and-a-half-block radar pedestal with a mast the spinning dish sits on."""
    scale = 2.20
    shift = 0.5 * (1.0 - scale)
    with mesh.at((shift, 0.0, shift), scale=scale):
        mesh.box(-0.18, 0.00, -0.18, 1.18, 0.18, 1.18, "dark")
        mesh.box(0.04, 0.18, 0.04, 0.96, 0.28, 0.96, "olive")
        mesh.box(0.10, 0.20, 0.10, 0.42, 0.58, 0.42, "olive")
        mesh.box(0.14, 0.36, 0.14, 0.38, 0.54, 0.38, "glass")
        mesh.cylinder((0.5, 0.16, 0.5), (0.5, 1.42, 0.5), 0.16, 28, "body", True)
        mesh.cylinder((0.5, 1.38, 0.5), (0.5, 1.58, 0.5), 0.30, 32, "dark", True)
        mesh.box(0.38, 1.54, 0.38, 0.62, 1.70, 0.62, "yellow")
        mesh.cylinder((0.96, 0.18, 0.96), (0.96, 0.82, 0.96), 0.05, 12, "dark", True)
        mesh.box(0.70, 0.18, 0.70, 1.12, 0.40, 1.12, "body")
        mesh.bolt_ring((0.5, 0.18, 0.5), (0, 1, 0), 0.52, 12, 0.016, "yellow")


def add_radar_dish_mesh(mesh: ObjBuilder) -> None:
    mesh.dish((0.0, 0.0, 0.0), 1.48, 0.92, rings=16, segments=48, material="white")
    mesh.cylinder((0, 0, 0.18), (0, 0, 1.55), 0.045, 16, "accent", True)
    mesh.sphere((0, 0, 1.62), 0.10, 14, 10, "accent")
    for a in (0.0, TAU / 3, 2 * TAU / 3):
        x, y = math.cos(a) * 1.12, math.sin(a) * 1.12
        mesh.cylinder((x, y, 0.42), (0, 0, 1.42), 0.022, 8, "dark", True)
    mesh.box(-0.20, -0.20, -0.14, 0.20, 0.20, 0.20, "dark")
    mesh.cylinder((0, 0, -0.40), (0, 0, 0.06), 0.11, 16, "body", True)
    mesh.box(-0.36, -0.08, -0.48, 0.36, 0.08, -0.18, "dark")
    mesh.bolt_ring((0, 0, 0.02), (0, 0, 1), 0.26, 10, 0.018, "yellow")


def add_ciws_base(mesh: ObjBuilder) -> None:
    scale = 2.10
    shift = 0.5 * (1.0 - scale)
    with mesh.at((shift, 0.0, shift), scale=scale):
        mesh.box(-0.12, 0.00, -0.12, 1.12, 0.14, 1.12, "dark")
        mesh.box(0.02, 0.14, 0.02, 0.98, 0.28, 0.98, "white")
        mesh.cylinder((0.5, 0.22, 0.5), (0.5, 0.62, 0.5), 0.32, 32, "white", True)
        mesh.bolt_ring((0.5, 0.26, 0.5), (0, 1, 0), 0.48, 12, 0.016, "yellow")
        mesh.box(0.06, 0.14, 0.06, 0.32, 0.44, 0.32, "dark")
        mesh.box(0.74, 0.14, 0.74, 1.04, 0.38, 1.04, "accent")
        mesh.box(0.78, 0.38, 0.78, 0.98, 0.70, 0.98, "dark")


def add_ciws_turret(mesh: ObjBuilder) -> None:
    """Turret head centered at origin, barrels along +Z, for BER yaw/spin."""
    with mesh.at((0.0, 0.0, 0.0), scale=1.85):
        mesh.sphere((0.0, 0.18, 0.0), 0.30, 18, 14, "white")
        mesh.box(-0.28, -0.06, -0.30, 0.28, 0.40, 0.34, "white")
        mesh.box(-0.42, 0.02, -0.16, -0.26, 0.30, 0.18, "dark")
        mesh.box(0.26, 0.02, -0.16, 0.42, 0.30, 0.18, "dark")
        mesh.cylinder((0.0, 0.12, 0.22), (0.0, 0.12, 0.52), 0.10, 16, "dark", True)
        for i in range(6):
            a = TAU * i / 6
            x, y = math.cos(a) * 0.078, 0.12 + math.sin(a) * 0.078
            mesh.cylinder((x, y, 0.36), (x, y, 1.18), 0.018, 8, "dark", True)
        mesh.box(-0.08, 0.34, -0.10, 0.08, 0.54, 0.16, "accent")
        mesh.box(-0.18, 0.36, -0.04, 0.18, 0.42, 0.22, "yellow")


def add_laser_base(mesh: ObjBuilder) -> None:
    mesh.box(0.08, 0.00, 0.08, 0.92, 0.14, 0.92, "dark")
    mesh.box(0.16, 0.14, 0.16, 0.84, 0.52, 0.72, "body")
    mesh.cylinder((0.5, 0.50, 0.48), (0.5, 0.70, 0.48), 0.11, 20, "dark", True)
    mesh.box(0.20, 0.14, 0.70, 0.40, 0.40, 0.88, "accent")
    for x in (0.26, 0.40, 0.54, 0.68):
        mesh.box(x, 0.52, 0.20, x + 0.06, 0.78, 0.28, "yellow")
    mesh.bolt_ring((0.5, 0.14, 0.5), (0, 1, 0), 0.38, 8, 0.012, "yellow")


def add_laser_head(mesh: ObjBuilder) -> None:
    """Emitter centered at origin, beam axis +Z."""
    mesh.cylinder((0.0, 0.0, -0.10), (0.0, 0.0, 0.28), 0.075, 20, "white", True)
    mesh.cylinder((0.0, 0.0, 0.26), (0.0, 0.0, 0.40), 0.095, 18, "glass", True)
    mesh.box(-0.10, -0.08, -0.16, 0.10, 0.08, -0.06, "dark")
    mesh.box(-0.03, 0.06, -0.04, 0.03, 0.14, 0.10, "accent")


def add_system_mesh(mesh: ObjBuilder, name: str) -> None:
    if name == "ciws":
        add_ciws_base(mesh)
    elif name == "laser_defense":
        add_laser_base(mesh)
    elif name == "passive_radar":
        mesh.box(0.30, 0.00, 0.30, 0.70, 0.12, 0.70, "dark")
        mesh.cylinder((0.5, 0.10, 0.5), (0.5, 1.05, 0.5), 0.05, 14, "body", True)
        mesh.box(0.22, 0.70, 0.46, 0.78, 1.00, 0.54, "dark")
        for i in range(7):
            z = 0.20 + i * 0.08
            mesh.box(0.18, 0.78, z, 0.82, 0.82, z + 0.02, "yellow")
        mesh.box(0.42, 0.12, 0.42, 0.58, 0.32, 0.58, "olive")
    elif name == "command_console":
        mesh.box(0.04, 0.00, 0.12, 0.96, 0.10, 0.96, "dark")
        mesh.box(0.08, 0.10, 0.22, 0.92, 0.42, 0.90, "body")
        mesh.box(0.10, 0.42, 0.48, 0.36, 0.86, 0.86, "dark")
        mesh.box(0.12, 0.46, 0.52, 0.34, 0.82, 0.84, "glass")
        mesh.box(0.38, 0.42, 0.46, 0.62, 0.94, 0.88, "dark")
        mesh.box(0.40, 0.48, 0.50, 0.60, 0.90, 0.86, "glass")
        mesh.box(0.64, 0.42, 0.48, 0.90, 0.86, 0.86, "dark")
        mesh.box(0.66, 0.46, 0.52, 0.88, 0.82, 0.84, "glass")
        mesh.box(0.12, 0.40, 0.24, 0.88, 0.46, 0.48, "dark")
        for x in range(8):
            mesh.box(0.16 + x * 0.09, 0.42, 0.28, 0.22 + x * 0.09, 0.45, 0.36, "yellow")
        mesh.box(0.22, 0.00, 0.00, 0.78, 0.08, 0.18, "dark")
        mesh.cylinder((0.16, 0.10, 0.78), (0.16, 0.78, 0.78), 0.028, 10, "accent", True)
        mesh.cylinder((0.84, 0.10, 0.78), (0.84, 0.62, 0.78), 0.024, 10, "yellow", True)
        mesh.torus((0.78, 0.48, 0.30), (0, 1, 0), 0.05, 0.012, 12, 8, "yellow")
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
        mesh.cylinder((0.0, 0.08, -0.62), (0.0, 0.08, 0.58), 0.072, 24, "olive", True)
        mesh.cylinder((0.0, 0.08, 0.50), (0.0, 0.08, 0.78), 0.092, 18, "dark", True)
        mesh.cylinder((0.0, 0.08, 0.72), (0.0, 0.08, 0.88), 0.055, 14, "glass", True)
        mesh.box(-0.035, -0.16, -0.08, 0.035, 0.08, 0.14, "dark")
        mesh.box(-0.10, -0.02, 0.02, -0.035, 0.06, 0.22, "olive")
        mesh.box(-0.028, 0.14, 0.06, 0.028, 0.26, 0.32, "dark")
        mesh.box(-0.022, 0.20, 0.10, 0.022, 0.24, 0.28, "glass")
        mesh.box(0.06, 0.00, -0.16, 0.18, 0.12, 0.16, "olive")
        mesh.cylinder((0.0, 0.08, -0.62), (0.0, 0.08, -0.46), 0.048, 14, "accent", True)
        mesh.cylinder((0.12, 0.06, 0.00), (0.12, 0.06, 0.28), 0.018, 10, "dark", True)
        mesh.bolt_ring((0.0, 0.08, 0.50), (0, 0, 1), 0.078, 8, 0.008, "yellow")
    elif name == "gauss_rifle":
        mesh.box(-0.055, 0.00, -0.28, 0.055, 0.12, 0.42, "dark")
        mesh.cylinder((0.0, 0.07, 0.18), (0.0, 0.07, 1.02), 0.032, 16, "body", True)
        for z in (0.28, 0.40, 0.52, 0.64, 0.76, 0.88):
            mesh.cylinder((0.0, 0.07, z), (0.0, 0.07, z + 0.035), 0.052, 12, "accent", False)
        mesh.box(-0.035, -0.16, -0.08, 0.035, 0.02, 0.12, "dark")
        mesh.box(-0.018, -0.04, 0.18, 0.018, 0.02, 0.34, "dark")
        mesh.box(-0.05, 0.10, -0.24, 0.05, 0.20, 0.08, "glass")
        mesh.box(-0.07, 0.00, 0.32, 0.07, 0.16, 0.50, "yellow")
        mesh.box(0.04, 0.02, -0.10, 0.12, 0.10, 0.16, "body")
        mesh.cylinder((0.0, 0.07, 0.98), (0.0, 0.07, 1.08), 0.022, 10, "dark", True)
    elif name == "railgun":
        mesh.box(-0.09, 0.00, -0.22, 0.09, 0.14, 0.48, "dark")
        mesh.box(-0.07, 0.12, 0.04, -0.018, 0.18, 1.02, "accent")
        mesh.box(0.018, 0.12, 0.04, 0.07, 0.18, 1.02, "accent")
        mesh.box(-0.016, 0.08, 0.10, 0.016, 0.14, 0.90, "glass")
        for z in (0.22, 0.40, 0.58, 0.76):
            mesh.box(-0.08, 0.04, z, 0.08, 0.16, z + 0.04, "yellow")
        mesh.box(-0.035, -0.18, -0.04, 0.035, 0.02, 0.14, "dark")
        mesh.box(-0.06, 0.14, -0.18, 0.06, 0.26, 0.10, "body")
        mesh.box(-0.04, 0.18, -0.14, 0.04, 0.24, 0.04, "glass")
        mesh.box(0.08, 0.02, -0.06, 0.16, 0.12, 0.22, "dark")
    elif name == "plasma_blade":
        mesh.box(-0.035, -0.05, -0.18, 0.035, 0.05, 0.20, "dark")
        mesh.cylinder((0.0, 0.0, 0.16), (0.0, 0.0, 0.26), 0.045, 12, "yellow", True)
        mesh.box(-0.055, -0.045, 0.20, 0.055, 0.045, 0.30, "accent")
        mesh.box(-0.018, -0.012, 0.28, 0.018, 0.012, 1.02, "glass")
        mesh.box(-0.010, -0.022, 0.32, 0.010, 0.022, 0.96, "glass")
        mesh.box(-0.04, -0.03, -0.08, 0.04, 0.03, 0.04, "body")
    elif name == "targeting_tablet":
        mesh.box(-0.24, -0.025, -0.34, 0.24, 0.035, 0.34, "dark")
        mesh.box(-0.20, 0.032, -0.28, 0.20, 0.042, 0.26, "glass")
        mesh.box(-0.22, -0.035, -0.30, 0.22, -0.022, 0.30, "body")
        mesh.cylinder((0.18, 0.00, 0.30), (0.18, 0.07, 0.30), 0.018, 8, "accent", True)
        for x in (-0.12, 0.00, 0.12):
            mesh.box(x - 0.03, 0.036, 0.20, x + 0.03, 0.044, 0.28, "yellow")
    elif name == "jammer":
        mesh.box(-0.18, 0.00, -0.14, 0.18, 0.30, 0.16, "olive")
        mesh.cylinder((0.0, 0.28, 0.00), (0.0, 0.82, 0.00), 0.018, 10, "yellow", True)
        mesh.cylinder((0.0, 0.80, 0.00), (0.0, 0.88, 0.00), 0.04, 10, "accent", True)
        mesh.box(-0.14, 0.08, -0.16, 0.14, 0.24, -0.11, "glass")
        mesh.box(-0.20, 0.00, -0.10, -0.16, 0.14, 0.12, "dark")
        mesh.box(0.16, 0.00, -0.10, 0.20, 0.14, 0.12, "dark")
        mesh.box(-0.08, 0.30, -0.04, 0.08, 0.36, 0.04, "body")
    else:
        mesh.box(-0.1, 0.0, -0.3, 0.1, 0.1, 0.3, "body")


def add_radar_item_mesh(mesh: ObjBuilder) -> None:
    """Inventory radar includes a parked dish so the icon is not a bare pedestal."""
    add_radar_base_mesh(mesh)
    with mesh.at((0.5, 3.42, 0.5), scale=0.72, pitch=-0.40):
        add_radar_dish_mesh(mesh)


def add_ciws_item_mesh(mesh: ObjBuilder) -> None:
    add_ciws_base(mesh)
    with mesh.at((0.5, 1.22, 0.5)):
        add_ciws_turret(mesh)


def add_laser_item_mesh(mesh: ObjBuilder) -> None:
    add_laser_base(mesh)
    with mesh.at((0.5, 0.70, 0.48)):
        add_laser_head(mesh)


def add_hatch_mesh(mesh: ObjBuilder, suffix: str) -> None:
    """Circular silo blast hatch. suffix is bottom, top, or open (vanilla trapdoor states)."""
    if suffix == "top":
        y0, y1 = 0.82, 0.98
    else:
        y0, y1 = 0.00, 0.16
    mesh.box(0.00, y0, 0.00, 1.00, y0 + 0.04, 1.00, "concrete")
    mesh.cylinder((0.5, y0 + 0.03, 0.5), (0.5, y1, 0.5), 0.46, 32, "dark", False)
    mesh.bolt_ring((0.5, y1, 0.5), (0, 1, 0), 0.42, 12, 0.014, "yellow")
    if suffix == "open":
        mesh.box(0.08, 0.14, 0.78, 0.92, 0.96, 0.96, "body")
        mesh.cylinder((0.5, 0.16, 0.86), (0.5, 0.92, 0.86), 0.36, 28, "dark", True)
        mesh.box(0.42, 0.16, 0.70, 0.58, 0.22, 0.90, "yellow")
        for z in (0.22, 0.78):
            mesh.cylinder((0.12, 0.04, z), (0.12, 0.38, z), 0.03, 10, "yellow", True)
    else:
        mesh.cylinder((0.5, y0 + 0.04, 0.5), (0.5, y1, 0.5), 0.38, 32, "body", True)
        mesh.box(0.46, y1, 0.18, 0.54, y1 + 0.04, 0.82, "yellow")
        mesh.cylinder((0.18, y0, 0.18), (0.18, y1 + 0.08, 0.18), 0.035, 10, "yellow", True)
        mesh.cylinder((0.82, y0, 0.18), (0.82, y1 + 0.08, 0.18), 0.035, 10, "yellow", True)


def add_door_mesh(mesh: ObjBuilder, kind: str, suffix: str) -> None:
    """Vault door occupying vanilla door space: closed on west (x=0), open left south / right north."""
    thick = 0.20
    security = kind == "security_door"
    body = "body" if not security else "navy"
    if suffix.endswith("_open"):
        if "left" in suffix:
            x0, x1, z0, z1 = 0.00, 1.00, 1.00 - thick, 1.00
        else:
            x0, x1, z0, z1 = 0.00, 1.00, 0.00, thick
    else:
        x0, x1, z0, z1 = 0.00, thick, 0.00, 1.00
    mesh.box(x0, 0.00, z0, x1, 1.00, z1, body)
    # Inner plate and bolts.
    inset = 0.03
    if suffix.endswith("_open"):
        mesh.box(x0 + inset, 0.06, z0 + 0.02, x1 - inset, 0.94, z1 - 0.02, "dark")
    else:
        mesh.box(x0 + 0.02, 0.06, z0 + inset, x1 - 0.02, 0.94, z1 - inset, "dark")
    if "bottom" in suffix:
        wheel_y = 0.62
        if suffix.endswith("_open"):
            cx, cz = 0.50, (z0 + z1) * 0.5
            mesh.torus((cx, wheel_y, cz), (0, 0, 1), 0.12, 0.025, 14, 8, "yellow")
            mesh.cylinder((cx, wheel_y, z0 - 0.02), (cx, wheel_y, z1 + 0.02), 0.04, 10, "accent", True)
        else:
            cx, cz = (x0 + x1) * 0.5, 0.50
            mesh.torus((cx, wheel_y, cz), (1, 0, 0), 0.12, 0.025, 14, 8, "yellow")
            mesh.cylinder((x0 - 0.02, wheel_y, cz), (x1 + 0.02, wheel_y, cz), 0.04, 10, "accent", True)
        mesh.box(x0 if not suffix.endswith("_open") else 0.08,
                 0.08, z0 if suffix.endswith("_open") else 0.08,
                 x1 if not suffix.endswith("_open") else 0.22,
                 0.18, z1 if suffix.endswith("_open") else 0.92, "yellow")
    else:
        if security:
            if suffix.endswith("_open"):
                mesh.box(0.28, 0.35, z0 + 0.01, 0.72, 0.78, z1 - 0.01, "glass")
            else:
                mesh.box(x0 + 0.01, 0.35, 0.28, x1 - 0.01, 0.78, 0.72, "glass")
        mesh.box(0.08 if suffix.endswith("_open") else x0,
                 0.82, 0.08 if not suffix.endswith("_open") else z0,
                 0.92 if suffix.endswith("_open") else x1,
                 0.90, 0.92 if not suffix.endswith("_open") else z1, "accent")


def add_door_item_mesh(mesh: ObjBuilder, kind: str) -> None:
    security = kind == "security_door"
    mesh.box(0.38, 0.00, 0.08, 0.62, 1.00, 0.92, "navy" if security else "body")
    mesh.box(0.40, 0.06, 0.12, 0.60, 0.94, 0.88, "dark")
    mesh.torus((0.50, 0.48, 0.50), (1, 0, 0), 0.12, 0.025, 14, 8, "yellow")
    mesh.box(0.36, 0.08, 0.10, 0.40, 0.16, 0.90, "yellow")
    if security:
        mesh.box(0.41, 0.52, 0.28, 0.48, 0.82, 0.72, "glass")
    mesh.bolt_ring((0.50, 0.20, 0.50), (1, 0, 0), 0.28, 8, 0.012, "yellow")


def add_component_mesh(mesh: ObjBuilder, name: str) -> None:
    """Unique 3D inventory meshes for remaining craft items (no 2D generated sprites)."""
    if name == "apex_alloy":
        mesh.box(-0.18, 0.00, -0.08, 0.18, 0.06, 0.08, "glass")
        mesh.box(-0.16, 0.06, -0.06, 0.16, 0.10, 0.06, "body")
        mesh.box(-0.14, 0.00, -0.07, -0.10, 0.11, 0.07, "dark")
    elif name == "circuit_board":
        mesh.box(-0.22, 0.00, -0.16, 0.22, 0.03, 0.16, "olive")
        mesh.box(-0.14, 0.03, -0.08, 0.00, 0.08, 0.08, "dark")
        mesh.box(0.04, 0.03, -0.06, 0.14, 0.07, 0.06, "accent")
        for x in (-0.18, -0.10, 0.02, 0.10, 0.18):
            mesh.cylinder((x, 0.00, 0.12), (x, 0.05, 0.12), 0.012, 8, "yellow", True)
        mesh.box(-0.20, 0.03, 0.10, 0.20, 0.035, 0.12, "yellow")
    elif name == "guidance_chip":
        mesh.box(-0.10, 0.00, -0.10, 0.10, 0.04, 0.10, "dark")
        mesh.box(-0.07, 0.04, -0.07, 0.07, 0.07, 0.07, "glass")
        for i in range(6):
            x = -0.08 + i * 0.032
            mesh.box(x, -0.01, -0.12, x + 0.012, 0.03, -0.10, "yellow")
            mesh.box(x, -0.01, 0.10, x + 0.012, 0.03, 0.12, "yellow")
    elif name == "solid_fuel":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.28, 0.0), 0.08, 18, "rust", True)
        mesh.cylinder((0.0, 0.26, 0.0), (0.0, 0.32, 0.0), 0.06, 14, "dark", True)
        mesh.cylinder((0.0, 0.02, 0.0), (0.0, 0.26, 0.0), 0.02, 10, "dark", False)
    elif name == "warhead":
        mesh.cylinder((0.0, -0.10, 0.0), (0.0, 0.08, 0.0), 0.09, 20, "dark", True)
        mesh.cone((0.0, 0.08, 0.0), (0.0, 0.32, 0.0), 0.09, 20, "accent")
        mesh.cylinder((0.0, -0.04, 0.0), (0.0, -0.02, 0.0), 0.10, 18, "yellow", False)
    elif name == "gauss_slug":
        mesh.cylinder((0.0, 0.02, -0.18), (0.0, 0.02, 0.16), 0.025, 12, "body", True)
        mesh.cone((0.0, 0.02, 0.16), (0.0, 0.02, 0.28), 0.025, 12, "glass")
        mesh.box(-0.04, 0.00, -0.16, 0.04, 0.04, -0.10, "dark")
    elif name == "advanced_propellant":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.26, 0.0), 0.07, 16, "olive", True)
        mesh.cylinder((0.0, 0.24, 0.0), (0.0, 0.30, 0.0), 0.05, 12, "dark", True)
        mesh.box(-0.04, 0.08, -0.08, 0.04, 0.18, -0.06, "yellow")
    elif name == "energy_cell":
        mesh.box(-0.08, 0.00, -0.08, 0.08, 0.22, 0.08, "body")
        mesh.box(-0.06, 0.22, -0.04, 0.00, 0.28, 0.04, "accent")
        mesh.box(0.02, 0.22, -0.04, 0.06, 0.26, 0.04, "dark")
        mesh.box(-0.09, 0.06, -0.09, 0.09, 0.10, 0.09, "yellow")
        mesh.box(-0.06, 0.04, -0.06, 0.06, 0.18, 0.06, "glass")
    elif name == "capacitor":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.24, 0.0), 0.09, 18, "yellow", True)
        mesh.box(-0.03, 0.24, -0.03, -0.01, 0.32, 0.03, "dark")
        mesh.box(0.01, 0.24, -0.03, 0.03, 0.32, 0.03, "accent")
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.04, 0.0), 0.10, 16, "dark", True)
    elif name.startswith("guidance_"):
        mesh.box(-0.12, 0.00, -0.10, 0.12, 0.08, 0.10, "dark")
        mesh.box(-0.10, 0.08, -0.08, 0.10, 0.12, 0.08, "body")
        if name.endswith("radar"):
            with mesh.at((0.0, 0.18, 0.0), scale=0.22, pitch=-0.6):
                add_radar_dish_mesh(mesh)
        elif name.endswith("infrared"):
            mesh.sphere((0.0, 0.16, 0.08), 0.06, 12, 8, "glass")
        elif name.endswith("terrain"):
            mesh.box(-0.10, 0.12, -0.04, 0.10, 0.16, 0.12, "yellow")
            mesh.cylinder((0.0, 0.16, 0.04), (0.0, 0.22, 0.04), 0.02, 8, "accent", True)
        elif name.endswith("inertial"):
            mesh.cylinder((0.0, 0.12, 0.0), (0.0, 0.20, 0.0), 0.05, 12, "glass", True)
        elif name.endswith("coordinate"):
            mesh.cylinder((0.0, 0.12, 0.0), (0.0, 0.28, 0.0), 0.012, 8, "yellow", True)
            mesh.box(-0.04, 0.26, -0.04, 0.04, 0.30, 0.04, "accent")
        else:
            mesh.cylinder((0.08, 0.12, 0.0), (0.08, 0.30, 0.0), 0.012, 8, "yellow", True)
            mesh.cylinder((-0.08, 0.12, 0.0), (-0.08, 0.24, 0.0), 0.012, 8, "accent", True)
    elif name == "emp_payload":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.16, 0.0), 0.10, 18, "dark", True)
        mesh.torus((0.0, 0.18, 0.0), (0, 1, 0), 0.10, 0.025, 16, 10, "yellow")
        mesh.torus((0.0, 0.24, 0.0), (0, 1, 0), 0.08, 0.02, 14, 8, "glass")
    elif name == "incendiary_payload":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.22, 0.0), 0.09, 16, "rust", True)
        mesh.cone((0.0, 0.22, 0.0), (0.0, 0.32, 0.0), 0.05, 12, "accent")
        mesh.box(-0.03, 0.08, 0.08, 0.03, 0.16, 0.12, "yellow")
    elif name == "penetrator_payload":
        mesh.cylinder((0.0, -0.04, 0.0), (0.0, 0.10, 0.0), 0.07, 16, "dark", True)
        mesh.cone((0.0, 0.10, 0.0), (0.0, 0.36, 0.0), 0.07, 18, "body")
        mesh.fin(-0.02, 0.08, 0.14, 0.07, 0.01, 0.02, "dark")
    elif name == "fragmentation_payload":
        mesh.sphere((0.0, 0.12, 0.0), 0.12, 14, 10, "dark")
        for a in range(8):
            ang = TAU * a / 8
            mesh.box(math.cos(ang) * 0.10 - 0.015, 0.04, math.sin(ang) * 0.10 - 0.015,
                     math.cos(ang) * 0.10 + 0.015, 0.20, math.sin(ang) * 0.10 + 0.015, "yellow")
    elif name == "decoy_warhead":
        mesh.box(-0.12, 0.00, -0.08, 0.12, 0.10, 0.08, "olive")
        for x in (-0.08, 0.00, 0.08):
            mesh.cylinder((x, 0.10, 0.0), (x, 0.24, 0.0), 0.022, 8, "white", True)
            mesh.cone((x, 0.24, 0.0), (x, 0.30, 0.0), 0.022, 8, "accent")
    elif name == "mirv_warhead":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.10, 0.0), 0.12, 18, "dark", True)
        for a in range(3):
            ang = TAU * a / 3
            x, z = math.cos(ang) * 0.06, math.sin(ang) * 0.06
            mesh.cone((x, 0.10, z), (x, 0.28, z), 0.035, 12, "accent")
        mesh.box(-0.02, 0.08, -0.02, 0.02, 0.14, 0.02, "yellow")
    elif "fuse" in name:
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.14, 0.0), 0.05, 14, "yellow", True)
        if name.startswith("proximity"):
            mesh.sphere((0.0, 0.20, 0.0), 0.05, 12, 8, "glass")
        elif name.startswith("airburst"):
            mesh.box(-0.04, 0.14, -0.04, 0.04, 0.22, 0.04, "accent")
            mesh.cylinder((0.0, 0.22, 0.0), (0.0, 0.30, 0.0), 0.015, 8, "dark", True)
        else:
            mesh.box(-0.05, 0.14, -0.03, 0.05, 0.20, 0.03, "dark")
            mesh.box(-0.02, 0.20, -0.02, 0.02, 0.28, 0.02, "body")
    elif name == "two_stage_motor":
        mesh.cylinder((0.0, -0.16, 0.0), (0.0, 0.04, 0.0), 0.08, 18, "white", False)
        mesh.cylinder((0.0, 0.04, 0.0), (0.0, 0.22, 0.0), 0.065, 16, "body", False)
        mesh.cylinder((0.0, -0.18, 0.0), (0.0, -0.12, 0.0), 0.09, 16, "dark", True)
        mesh.cylinder((0.0, 0.02, 0.0), (0.0, 0.05, 0.0), 0.085, 16, "accent", False)
    elif name == "three_stage_motor":
        mesh.cylinder((0.0, -0.20, 0.0), (0.0, -0.02, 0.0), 0.085, 18, "white", False)
        mesh.cylinder((0.0, -0.02, 0.0), (0.0, 0.12, 0.0), 0.07, 16, "body", False)
        mesh.cylinder((0.0, 0.12, 0.0), (0.0, 0.24, 0.0), 0.055, 14, "white", False)
        for y in (-0.04, 0.10):
            mesh.cylinder((0.0, y, 0.0), (0.0, y + 0.025, 0.0), 0.09, 16, "accent", False)
        mesh.cylinder((0.0, -0.22, 0.0), (0.0, -0.16, 0.0), 0.095, 16, "dark", True)
    elif name.endswith("_package"):
        mesh.box(-0.14, 0.00, -0.12, 0.14, 0.16, 0.12, "olive" if "reliability" in name else "body")
        mesh.box(-0.12, 0.16, -0.10, 0.12, 0.18, 0.10, "yellow")
        mesh.box(-0.03, 0.18, -0.03, 0.03, 0.22, 0.03, "dark")
    elif name.endswith("_module") or name == "anti_jam_module":
        mesh.box(-0.12, 0.00, -0.10, 0.12, 0.10, 0.10, "navy")
        mesh.box(-0.10, 0.10, -0.08, 0.10, 0.14, 0.08, "dark")
        if "thermal" in name:
            mesh.box(-0.08, 0.14, -0.04, 0.08, 0.16, 0.08, "accent")
        elif "rwr" in name:
            mesh.cylinder((0.0, 0.14, 0.0), (0.0, 0.28, 0.0), 0.015, 8, "yellow", True)
        elif "shield" in name:
            mesh.sphere((0.0, 0.18, 0.0), 0.06, 10, 8, "glass")
        elif "mobility" in name:
            mesh.box(-0.10, 0.12, -0.02, 0.10, 0.18, 0.02, "yellow")
        elif "camouflage" in name:
            mesh.box(-0.08, 0.14, -0.06, 0.08, 0.16, 0.06, "olive")
        elif "medical" in name:
            mesh.box(-0.08, 0.14, -0.02, 0.08, 0.18, 0.02, "accent")
            mesh.box(-0.02, 0.10, -0.08, 0.02, 0.22, 0.08, "accent")
        else:
            mesh.cylinder((-0.06, 0.14, 0.0), (-0.06, 0.26, 0.0), 0.012, 8, "yellow", True)
            mesh.cylinder((0.06, 0.14, 0.0), (0.06, 0.26, 0.0), 0.012, 8, "accent", True)
    elif name == "flare":
        mesh.cylinder((0.0, 0.00, 0.0), (0.0, 0.28, 0.0), 0.035, 12, "white", True)
        mesh.cylinder((0.0, 0.24, 0.0), (0.0, 0.32, 0.0), 0.04, 10, "accent", True)
        mesh.box(-0.04, 0.00, -0.04, 0.04, 0.04, 0.04, "dark")
    elif name == "apex_helmet":
        mesh.sphere((0.0, 0.16, 0.0), 0.16, 14, 10, "glass")
        mesh.box(-0.14, 0.00, -0.12, 0.14, 0.10, 0.12, "dark")
        mesh.box(-0.12, 0.10, 0.08, 0.12, 0.18, 0.16, "glass")
        mesh.box(-0.16, 0.08, -0.04, -0.12, 0.18, 0.08, "yellow")
    elif name == "apex_chestplate":
        mesh.box(-0.20, 0.02, -0.10, 0.20, 0.32, 0.10, "glass")
        mesh.box(-0.16, 0.08, 0.08, 0.16, 0.24, 0.12, "accent")
        mesh.box(-0.22, 0.18, -0.06, -0.16, 0.30, 0.06, "dark")
        mesh.box(0.16, 0.18, -0.06, 0.22, 0.30, 0.06, "dark")
        mesh.box(-0.06, 0.12, 0.10, 0.06, 0.22, 0.14, "yellow")
    elif name == "apex_leggings":
        mesh.box(-0.14, 0.18, -0.08, 0.14, 0.28, 0.08, "glass")
        mesh.box(-0.14, 0.00, -0.07, -0.02, 0.20, 0.07, "dark")
        mesh.box(0.02, 0.00, -0.07, 0.14, 0.20, 0.07, "dark")
        mesh.box(-0.12, 0.08, -0.08, -0.04, 0.12, 0.08, "yellow")
    elif name == "apex_boots":
        mesh.box(-0.16, 0.00, -0.08, -0.02, 0.10, 0.14, "dark")
        mesh.box(0.02, 0.00, -0.08, 0.16, 0.10, 0.14, "dark")
        mesh.box(-0.16, 0.00, 0.10, -0.02, 0.05, 0.20, "glass")
        mesh.box(0.02, 0.00, 0.10, 0.16, 0.05, 0.20, "glass")
        mesh.box(-0.14, 0.10, -0.04, -0.04, 0.14, 0.08, "yellow")
        mesh.box(0.04, 0.10, -0.04, 0.14, 0.14, 0.08, "yellow")
    elif name.endswith("_bomb"):
        add_bomb_mesh(mesh, name)
    elif name.endswith("_perk"):
        add_perk_chip_mesh(mesh, name)
    else:
        mesh.box(-0.10, 0.00, -0.10, 0.10, 0.16, 0.10, "body")


def add_air_raid_siren_base(mesh: ObjBuilder) -> None:
    """Civil dual-trumpet air-raid siren: squat cabinet and twin masts."""
    mesh.box(0.12, 0.00, 0.12, 0.88, 0.18, 0.88, "dark")
    mesh.box(0.20, 0.18, 0.20, 0.80, 0.62, 0.80, "accent")
    mesh.box(0.24, 0.28, 0.24, 0.76, 0.52, 0.76, "dark")
    mesh.cylinder((0.32, 0.60, 0.50), (0.32, 1.35, 0.50), 0.05, 14, "yellow", True)
    mesh.cylinder((0.68, 0.60, 0.50), (0.68, 1.35, 0.50), 0.05, 14, "yellow", True)
    mesh.box(0.28, 0.62, 0.46, 0.72, 0.70, 0.54, "body")
    for x in (0.28, 0.72):
        mesh.box(x - 0.06, 0.18, 0.18, x + 0.06, 0.34, 0.30, "yellow")
    mesh.bolt_ring((0.5, 0.18, 0.5), (0, 1, 0), 0.34, 8, 0.012, "yellow")


def add_air_raid_horns(mesh: ObjBuilder) -> None:
    """Pair of rotating trumpets centered at origin, bells along +Z."""
    for x in (-0.22, 0.22):
        mesh.cylinder((x, 0.0, -0.10), (x, 0.0, 0.18), 0.05, 14, "dark", True)
        mesh.cylinder((x, 0.0, 0.16), (x, 0.0, 0.62), 0.09, 16, "accent", True, 0.16)
        mesh.cylinder((x, 0.0, 0.60), (x, 0.0, 0.72), 0.17, 16, "yellow", True)
        mesh.box(x - 0.03, -0.12, -0.04, x + 0.03, 0.12, 0.08, "dark")
    mesh.box(-0.28, -0.04, -0.16, 0.28, 0.04, -0.04, "body")


def add_industrial_siren_mesh(mesh: ObjBuilder) -> None:
    """Stacked factory horns on a motor housing — visually distinct from the air-raid pair."""
    mesh.box(0.08, 0.00, 0.16, 0.92, 0.22, 0.84, "olive")
    mesh.box(0.18, 0.22, 0.24, 0.82, 0.70, 0.76, "dark")
    mesh.cylinder((0.5, 0.68, 0.5), (0.5, 0.92, 0.5), 0.22, 20, "yellow", True)
    mesh.cylinder((0.5, 0.90, 0.5), (0.5, 1.18, 0.5), 0.16, 18, "accent", True, 0.28)
    mesh.cylinder((0.5, 1.16, 0.5), (0.5, 1.38, 0.5), 0.10, 16, "body", True, 0.20)
    mesh.box(0.12, 0.24, 0.28, 0.22, 0.62, 0.72, "yellow")
    mesh.box(0.78, 0.24, 0.28, 0.88, 0.62, 0.72, "yellow")
    mesh.torus((0.5, 0.46, 0.5), (0, 1, 0), 0.28, 0.04, 16, 10, "dark")
    mesh.box(0.36, 0.22, 0.10, 0.64, 0.40, 0.24, "glass")


def add_nuclear_siren_base(mesh: ObjBuilder) -> None:
    """Tall civil-defense pole; the large horn is a separate rotating component."""
    mesh.box(0.22, 0.00, 0.22, 0.78, 0.16, 0.78, "concrete")
    mesh.cylinder((0.5, 0.14, 0.5), (0.5, 2.35, 0.5), 0.07, 16, "yellow", True)
    mesh.cylinder((0.5, 0.14, 0.5), (0.5, 0.28, 0.5), 0.14, 16, "dark", True)
    mesh.box(0.42, 1.10, 0.42, 0.58, 1.22, 0.58, "accent")
    mesh.box(0.18, 0.16, 0.42, 0.32, 0.70, 0.58, "dark")
    mesh.bolt_ring((0.5, 0.16, 0.5), (0, 1, 0), 0.24, 8, 0.012, "yellow")


def add_nuclear_horn(mesh: ObjBuilder) -> None:
    """Single oversized civil-defense projector, bell along +Z."""
    mesh.box(-0.10, -0.10, -0.18, 0.10, 0.10, 0.08, "dark")
    mesh.cylinder((0.0, 0.0, 0.04), (0.0, 0.0, 0.42), 0.12, 18, "yellow", True)
    mesh.cylinder((0.0, 0.0, 0.40), (0.0, 0.0, 0.88), 0.18, 20, "accent", True, 0.36)
    mesh.cylinder((0.0, 0.0, 0.86), (0.0, 0.0, 0.96), 0.38, 20, "body", True)
    mesh.box(-0.04, 0.10, -0.06, 0.04, 0.22, 0.20, "dark")


def add_cable_block_mesh(mesh: ObjBuilder) -> None:
    """Floor conduit run with armored jacket, glands, and a visible conductor."""
    mesh.box(0.18, 0.00, 0.00, 0.82, 0.16, 1.00, "dark")
    mesh.cylinder((0.50, 0.20, 0.00), (0.50, 0.20, 1.00), 0.11, 18, "olive", False)
    mesh.cylinder((0.50, 0.20, 0.00), (0.50, 0.20, 1.00), 0.045, 12, "yellow", False)
    mesh.cylinder((0.50, 0.20, 0.04), (0.50, 0.20, 0.18), 0.15, 16, "dark", True)
    mesh.cylinder((0.50, 0.20, 0.82), (0.50, 0.20, 0.96), 0.15, 16, "dark", True)
    mesh.box(0.42, 0.28, 0.40, 0.58, 0.40, 0.60, "accent")
    for z in (0.30, 0.50, 0.70):
        mesh.torus((0.50, 0.20, z), (0, 0, 1), 0.13, 0.02, 12, 8, "yellow")


def add_cable_item_mesh(mesh: ObjBuilder) -> None:
    """Coiled cable spool for the inventory/item, distinct from the floor conduit."""
    mesh.cylinder((0.0, 0.02, 0.0), (0.0, 0.08, 0.0), 0.16, 20, "dark", True)
    mesh.cylinder((0.0, 0.22, 0.0), (0.0, 0.28, 0.0), 0.16, 20, "dark", True)
    mesh.cylinder((0.0, 0.04, 0.0), (0.0, 0.26, 0.0), 0.04, 12, "body", True)
    mesh.torus((0.0, 0.15, 0.0), (0, 1, 0), 0.12, 0.035, 18, 10, "olive")
    mesh.torus((0.0, 0.15, 0.0), (0, 1, 0), 0.08, 0.028, 16, 10, "yellow")
    mesh.cylinder((0.10, 0.15, 0.00), (0.22, 0.04, 0.10), 0.018, 8, "accent", True)


def add_drone_launcher_mesh(mesh: ObjBuilder) -> None:
    """4-long × 2-high TEL car. Origin is rear-left; nose/ramp toward -Z (north)."""
    # Chassis / frame
    mesh.box(0.06, 0.18, -3.92, 1.94, 0.46, -0.08, "olive")
    mesh.box(0.10, 0.46, -3.88, 1.90, 0.58, -1.18, "dark")
    mesh.box(0.02, 0.00, -3.96, 1.98, 0.10, -0.04, "dark")
    # Cab at the rear (z near 0)
    mesh.box(0.12, 0.46, -1.18, 1.88, 1.62, -0.06, "olive")
    mesh.box(0.22, 0.78, -0.22, 1.78, 1.48, 0.04, "glass")
    mesh.box(0.18, 0.72, -1.12, 1.82, 1.42, -0.92, "glass")
    mesh.box(0.28, 0.50, -1.10, 1.72, 0.78, -0.18, "dark")
    mesh.box(1.78, 0.86, -0.62, 1.98, 1.02, -0.42, "yellow")
    mesh.box(0.02, 0.86, -0.62, 0.22, 1.02, -0.42, "yellow")
    mesh.box(0.70, 1.52, -0.70, 1.30, 1.78, -0.18, "dark")
    mesh.box(0.78, 1.62, -1.05, 1.22, 1.92, -0.55, "navy")
    # 45-degree launch rail from bed toward the nose
    for i in range(10):
        t0, t1 = i / 10.0, (i + 1) / 10.0
        z0 = -1.20 + t0 * (-3.70 + 1.20)
        z1 = -1.20 + t1 * (-3.70 + 1.20)
        y0 = 0.58 + t0 * 1.35
        y1 = 0.58 + t1 * 1.35
        mesh.box(0.42, y0, min(z0, z1), 1.58, y1 + 0.10, max(z0, z1), "body")
        mesh.box(0.38, y0 + 0.02, min(z0, z1), 0.48, y1 + 0.16, max(z0, z1), "yellow")
        mesh.box(1.52, y0 + 0.02, min(z0, z1), 1.62, y1 + 0.16, max(z0, z1), "yellow")
    mesh.box(0.48, 1.78, -3.82, 1.52, 1.98, -3.42, "accent")
    mesh.cylinder((1.00, 0.62, -1.28), (1.00, 1.88, -3.55), 0.05, 12, "dark", True)
    # Wheels along both sides
    for z in (-0.42, -1.55, -2.55, -3.52):
        for x, s in ((0.08, 1), (1.92, -1)):
            mesh.torus((x, 0.28, z), (1, 0, 0), 0.24, 0.08, 16, 10, "tire")
            mesh.cylinder((x - 0.08 * s, 0.28, z), (x + 0.08 * s, 0.28, z), 0.10, 12, "dark", True)
    # Lights, grill, exhaust, stowage
    mesh.box(0.22, 0.52, -0.08, 0.48, 0.70, 0.06, "accent")
    mesh.box(1.52, 0.52, -0.08, 1.78, 0.70, 0.06, "accent")
    mesh.box(0.30, 0.22, -3.98, 0.70, 0.38, -3.88, "yellow")
    mesh.box(1.30, 0.22, -3.98, 1.70, 0.38, -3.88, "yellow")
    mesh.cylinder((0.22, 0.48, -3.40), (0.22, 0.48, -3.85), 0.06, 10, "dark", True)
    mesh.box(0.16, 0.58, -2.40, 0.38, 1.10, -1.70, "dark")
    mesh.box(1.62, 0.58, -2.40, 1.84, 1.10, -1.70, "dark")
    mesh.bolt_ring((1.00, 0.20, -2.00), (0, 1, 0), 0.72, 10, 0.018, "yellow")


def add_perk_workbench_mesh(mesh: ObjBuilder) -> None:
    mesh.box(0.02, 0.00, 0.02, 0.98, 0.14, 0.98, "dark")
    mesh.box(0.08, 0.14, 0.08, 0.92, 0.92, 0.92, "olive")
    mesh.box(0.04, 0.92, 0.04, 0.96, 1.02, 0.96, "body")
    mesh.box(0.12, 1.02, 0.18, 0.88, 1.38, 0.42, "navy")
    mesh.box(0.18, 1.10, 0.22, 0.82, 1.32, 0.28, "glass")
    mesh.box(0.14, 1.02, 0.50, 0.46, 1.22, 0.86, "dark")
    mesh.box(0.54, 1.02, 0.50, 0.86, 1.18, 0.86, "yellow")
    mesh.cylinder((0.22, 1.02, 0.22), (0.22, 1.48, 0.22), 0.03, 8, "accent", True)
    mesh.box(0.20, 0.30, 0.00, 0.80, 0.82, 0.10, "dark")
    mesh.box(0.28, 0.40, 0.10, 0.72, 0.70, 0.16, "glass")
    mesh.bolt_ring((0.5, 0.14, 0.5), (0, 1, 0), 0.42, 8, 0.014, "yellow")


def add_strike_drone_mesh(mesh: ObjBuilder) -> None:
    """Predator-style UAV, fuselage along +Z."""
    mesh.cylinder((0.0, 0.04, -0.55), (0.0, 0.04, 0.62), 0.07, 18, "olive", True)
    mesh.cone((0.0, 0.04, 0.62), (0.0, 0.04, 0.92), 0.07, 16, "dark")
    mesh.cylinder((0.0, 0.04, -0.55), (0.0, 0.04, -0.78), 0.045, 12, "dark", True)
    mesh.box(-0.72, 0.02, -0.12, 0.72, 0.06, 0.22, "olive")
    mesh.box(-0.70, 0.03, 0.04, -0.18, 0.055, 0.18, "dark")
    mesh.box(0.18, 0.03, 0.04, 0.70, 0.055, 0.18, "dark")
    mesh.box(-0.22, 0.02, -0.72, -0.02, 0.28, -0.58, "olive")
    mesh.box(0.02, 0.02, -0.72, 0.22, 0.28, -0.58, "olive")
    mesh.box(-0.04, 0.10, -0.18, 0.04, 0.22, 0.10, "dark")
    mesh.cylinder((0.0, 0.00, 0.28), (0.0, -0.10, 0.28), 0.03, 10, "glass", True)
    mesh.box(-0.05, -0.02, 0.05, 0.05, 0.04, 0.28, "dark")
    mesh.box(-0.03, -0.08, 0.12, 0.03, 0.00, 0.22, "accent")
    mesh.cylinder((0.0, 0.04, -0.20), (0.0, 0.16, -0.20), 0.012, 8, "yellow", True)


def add_bomb_mesh(mesh: ObjBuilder, name: str) -> None:
    color = {
        "he_bomb": "olive",
        "cluster_bomb": "yellow",
        "bunker_bomb": "dark",
        "incendiary_bomb": "accent",
    }.get(name, "olive")
    mesh.cylinder((0.0, -0.16, 0.0), (0.0, 0.18, 0.0), 0.07, 16, color, True)
    mesh.cone((0.0, 0.18, 0.0), (0.0, 0.32, 0.0), 0.07, 14, "dark")
    mesh.box(-0.01, -0.18, -0.08, 0.01, 0.06, 0.08, "dark")
    mesh.box(-0.08, -0.18, -0.01, 0.08, 0.06, 0.01, "dark")
    if name == "cluster_bomb":
        for x in (-0.04, 0.04):
            mesh.cylinder((x, -0.10, 0.04), (x, 0.10, 0.04), 0.02, 8, "yellow", True)
    if name == "bunker_bomb":
        mesh.cone((0.0, -0.16, 0.0), (0.0, -0.28, 0.0), 0.05, 12, "body")
    if name == "incendiary_bomb":
        mesh.cylinder((0.0, 0.06, 0.0), (0.0, 0.10, 0.0), 0.075, 12, "yellow", False)


def add_perk_chip_mesh(mesh: ObjBuilder, name: str) -> None:
    accent = {
        "range_perk": "glass",
        "damage_perk": "accent",
        "accuracy_perk": "yellow",
        "speed_perk": "navy",
    }.get(name, "body")
    mesh.box(-0.10, 0.00, -0.10, 0.10, 0.03, 0.10, "dark")
    mesh.box(-0.07, 0.03, -0.07, 0.07, 0.07, 0.07, accent)
    for i in range(5):
        x = -0.08 + i * 0.035
        mesh.box(x, -0.01, -0.12, x + 0.012, 0.025, -0.10, "yellow")
        mesh.box(x, -0.01, 0.10, x + 0.012, 0.025, 0.12, "body")


def add_showcase_mesh(mesh: ObjBuilder) -> None:
    """Museum plinth; the loaded missile is drawn by the block-entity renderer."""
    mesh.box(0.08, 0.00, 0.08, 0.92, 0.12, 0.92, "dark")
    mesh.box(0.16, 0.12, 0.16, 0.84, 0.58, 0.84, "navy")
    mesh.box(0.12, 0.58, 0.12, 0.88, 0.68, 0.88, "body")
    mesh.cylinder((0.5, 0.66, 0.5), (0.5, 0.78, 0.5), 0.16, 20, "yellow", True)
    mesh.box(0.22, 0.20, 0.22, 0.78, 0.52, 0.28, "glass")
    mesh.box(0.22, 0.20, 0.72, 0.78, 0.52, 0.78, "glass")
    mesh.bolt_ring((0.5, 0.12, 0.5), (0, 1, 0), 0.38, 8, 0.012, "yellow")


def add_facility_door_mesh(mesh: ObjBuilder, kind: str, opened: bool) -> None:
    """North-facing facility door/hatch. Open poses park the leaf out of the opening."""
    from catalog import FACILITY_DOORS
    spec = next(entry for entry in FACILITY_DOORS if entry[0] == kind)
    width, height, hatch, thickness, depth = spec[1], spec[2], spec[3], spec[4], spec[5]
    if hatch:
        if opened:
            mesh.box(0.04, 0.00, max(0.55, depth - 0.22), width - 0.04, min(1.35, 0.2 + width),
                     max(0.55, depth - 0.22) + thickness, "dark")
            mesh.cylinder((width * 0.5, 0.08, depth - 0.08),
                          (width * 0.5, 1.05, depth - 0.08), min(0.36, width * 0.35), 24, "body", True)
            mesh.box(width * 0.45, 0.00, depth - 0.20, width * 0.55, 0.10, depth, "yellow")
        else:
            mesh.box(0.00, 0.00, 0.00, width, thickness * 0.35, depth, "concrete")
            if kind == "maintenance_hatch":
                mesh.box(0.06, thickness * 0.30, 0.06, width - 0.06, thickness, depth - 0.06, "yellow")
                for x in range(3):
                    mesh.box(0.12 + x * 0.28, thickness * 0.4, 0.10, 0.18 + x * 0.28, thickness + 0.02, depth - 0.10, "dark")
            elif kind == "submarine_hatch":
                mesh.cylinder((width * 0.5, 0.02, depth * 0.5),
                              (width * 0.5, thickness, depth * 0.5), min(0.46, width * 0.46), 28, "navy", True)
                mesh.torus((width * 0.5, thickness + 0.02, depth * 0.5), (0, 1, 0), 0.16, 0.03, 14, 8, "yellow")
            else:
                mesh.cylinder((width * 0.5, 0.02, depth * 0.5),
                              (width * 0.5, thickness, depth * 0.5), min(0.48, width * 0.46), 32, "dark", True)
                mesh.box(width * 0.46, thickness, 0.12, width * 0.54, thickness + 0.04, depth - 0.12, "yellow")
                mesh.bolt_ring((width * 0.5, thickness, depth * 0.5), (0, 1, 0),
                               min(0.42, width * 0.4), 12, 0.014, "yellow")
        return

    body = {
        "personnel_door": "body",
        "blast_door": "dark",
        "security_door": "navy",
        "airlock_door": "body",
        "bunker_door": "olive",
        "vault_door": "yellow",
        "vehicle_door": "olive",
        "hangar_shutter": "body",
    }.get(kind, "body")
    if opened:
        mesh.box(-thickness, 0.00, 0.02, 0.04, height, width, body)
        mesh.box(-thickness + 0.03, 0.08, 0.08, -0.01, height - 0.08, width - 0.08, "dark")
        if kind in ("blast_door", "vault_door", "airlock_door"):
            mesh.torus((-thickness * 0.5, min(1.1, height * 0.55), width * 0.5),
                       (1, 0, 0), 0.14, 0.03, 14, 8, "yellow")
        return
    mesh.box(0.00, 0.00, 0.00, width, height, thickness, body)
    mesh.box(0.04, 0.06, 0.03, width - 0.04, height - 0.06, thickness - 0.03, "dark")
    if kind == "personnel_door":
        mesh.box(width - 0.18, 0.92, thickness - 0.02, width - 0.08, 1.08, thickness + 0.04, "yellow")
        mesh.box(0.08, 0.08, 0.02, 0.16, height - 0.08, thickness - 0.02, "yellow")
    elif kind == "blast_door":
        mesh.torus((width * 0.5, 1.05, thickness * 0.5), (0, 0, 1), 0.16, 0.035, 16, 10, "yellow")
        mesh.bolt_ring((width * 0.5, 0.4, thickness), (0, 0, 1), 0.32, 10, 0.016, "yellow")
        mesh.box(0.06, 0.10, 0.00, width - 0.06, 0.22, thickness + 0.02, "accent")
    elif kind == "security_door":
        mesh.box(0.18, 1.05, 0.02, width - 0.18, 1.70, thickness - 0.01, "glass")
        mesh.box(width - 0.20, 0.90, thickness - 0.02, width - 0.08, 1.02, thickness + 0.03, "yellow")
    elif kind == "airlock_door":
        mesh.cylinder((width * 0.5, height * 0.55, thickness * 0.5),
                      (width * 0.5, height * 0.55, thickness + 0.02), 0.22, 20, "glass", True)
        mesh.box(-0.04, 0.20, 0.04, 0.08, height - 0.20, thickness - 0.04, "yellow")
        mesh.box(width - 0.08, 0.20, 0.04, width + 0.04, height - 0.20, thickness - 0.04, "yellow")
    elif kind == "bunker_door":
        for y in (0.2, 1.0, 1.7):
            mesh.box(0.08, y, thickness - 0.02, width - 0.08, y + 0.08, thickness + 0.02, "yellow")
        mesh.torus((0.45, 1.0, thickness), (0, 0, 1), 0.18, 0.04, 14, 8, "accent")
    elif kind == "vault_door":
        mesh.cylinder((width * 0.55, 1.4, thickness * 0.4),
                      (width * 0.55, 1.4, thickness + 0.06), 0.42, 28, "body", True)
        mesh.torus((width * 0.55, 1.4, thickness + 0.04), (0, 0, 1), 0.28, 0.05, 16, 10, "yellow")
        mesh.box(0.10, 0.08, 0.00, width - 0.10, 0.22, thickness + 0.02, "accent")
    elif kind == "vehicle_door":
        for x in range(6):
            mesh.box(0.08 + x * 0.48, 0.10, 0.02, 0.22 + x * 0.48, height - 0.10, thickness - 0.02, "dark")
        mesh.box(0.10, height - 0.18, 0.00, width - 0.10, height - 0.06, thickness + 0.02, "yellow")
    elif kind == "hangar_shutter":
        slat = 0.16
        y = 0.04
        while y < height - 0.04:
            mesh.box(0.04, y, 0.02, width - 0.04, min(height - 0.04, y + slat * 0.7), thickness - 0.02,
                     "yellow" if int(y / slat) % 2 == 0 else "dark")
            y += slat
        mesh.box(0.00, height - 0.12, 0.00, width, height, thickness + 0.02, "accent")


def add_facility_door_item_mesh(mesh: ObjBuilder, kind: str) -> None:
    from catalog import FACILITY_DOORS
    spec = next(entry for entry in FACILITY_DOORS if entry[0] == kind)
    hatch = spec[3]
    if hatch:
        mesh.cylinder((0.5, 0.02, 0.5), (0.5, 0.18, 0.5), 0.42, 28, "dark", True)
        mesh.box(0.08, 0.00, 0.08, 0.92, 0.04, 0.92, "concrete")
        if kind == "submarine_hatch":
            mesh.torus((0.5, 0.20, 0.5), (0, 1, 0), 0.16, 0.03, 14, 8, "yellow")
        elif kind == "maintenance_hatch":
            mesh.box(0.20, 0.16, 0.20, 0.80, 0.20, 0.80, "yellow")
        else:
            mesh.bolt_ring((0.5, 0.18, 0.5), (0, 1, 0), 0.34, 10, 0.012, "yellow")
        return
    add_facility_door_mesh(mesh, kind, False)


OBJ_BLOCKS = {
    "icbm_silo", "slbm_tube", "cruise_pad", "sam_battery", "mobile_launcher", "vls",
    "radar", "ciws", "laser_defense", "passive_radar", "command_console",
    "submarine_control", "missile_rack", "loading_crane", "propellant_refinery",
    "maintenance_station", "capacitor_charger", "missile_assembly",
    "air_raid_siren", "industrial_siren", "nuclear_warning_siren",
    "missile_showcase", "drone_launcher", "perk_workbench",
}

HANDHELD_OBJ = {
    "manpads", "gauss_rifle", "railgun", "plasma_blade", "targeting_tablet", "jammer",
}

MISSILE_OBJ = {
    "icbm", "slbm", "srbm", "alcm", "cruise_missile", "sam", "aam", "interceptor",
    "strike_drone",
}

COMPONENT_OBJ = {
    "apex_alloy", "circuit_board", "guidance_chip", "solid_fuel", "warhead",
    "gauss_slug", "advanced_propellant", "energy_cell", "capacitor",
    "guidance_inertial", "guidance_coordinate", "guidance_terrain", "guidance_radar",
    "guidance_infrared", "guidance_command", "emp_payload", "incendiary_payload",
    "penetrator_payload", "fragmentation_payload", "decoy_warhead", "mirv_warhead",
    "proximity_fuse", "airburst_fuse", "delayed_fuse", "two_stage_motor",
    "three_stage_motor", "precision_package", "reliability_package", "anti_jam_module",
    "flare", "thermal_module", "rwr_module", "shield_module", "mobility_module",
    "camouflage_module", "medical_module", "apex_helmet", "apex_chestplate",
    "apex_leggings", "apex_boots",
    "he_bomb", "cluster_bomb", "bunker_bomb", "incendiary_bomb",
    "range_perk", "damage_perk", "accuracy_perk", "speed_perk",
}

ANIMATED_COMPONENTS = {
    "ciws_turret_component", "laser_head_component", "radar_dish_component",
    "air_raid_horn_component", "nuclear_horn_component",
}
