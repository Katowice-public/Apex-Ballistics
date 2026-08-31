# Apex Ballistics

A Minecraft **1.21.1 Forge** mod focused on strategic missiles, anti-air, and high-tech armor and weapons. Everything ships in **one JAR** — no extra libraries.

Requires:

- Minecraft **1.21.1**
- Forge **52.1.0** or newer for 1.21.1
- Java **21**

## Install

1. Install [Minecraft Forge 1.21.1](https://files.minecraftforge.net/net/minecraftforge/forge/index_1.21.1.html).
2. Drop `apexballistics-2.0.0.jar` into your `.minecraft/mods` folder.
3. Launch the Forge 1.21.1 profile.

Build it yourself with `./gradlew build`. The playable jar is `build/libs/apexballistics-2.0.0.jar`.

## What's in 2.0

### 3D flight and animation

Missiles use an articulated 3D in-flight model rather than a flat item sprite.
Three procedural animations run smoothly on every client without an animation
library:

1. motor ignition recoil and nozzle pulse;
2. ballistic roll stabilization;
3. active guidance-fin corrections (more aggressive on SAMs and AAMs).

### Missiles

| Round | Role | How to fire |
| --- | --- | --- |
| **ICBM** | Intercontinental ballistic, huge yield | ICBM silo + targeting tablet |
| **SLBM** | Submarine-launched ballistic | SLBM tube (best underwater / at sea level) |
| **SRBM** | Short-range tactical ballistic | ICBM silo |
| **ALCM** | Air-launched cruise | Cruise pad, MANPADS, or by hand |
| **Cruise missile** | Low-altitude terrain follower | Cruise pad |
| **SAM** | Surface-to-air homing | SAM battery (auto) or MANPADS |
| **AAM** | Air-to-air homing | MANPADS or by hand |

Blast power is configurable in `apexballistics-common.toml` (`missileGriefing` turns block damage off).

### Launchers and sensors

- **ICBM Silo** — load ICBM/SRBM, program a target, sneak-use or pulse redstone to fire.
- **SLBM Launch Tube** — same flow for SLBMs.
- **Cruise Launch Pad** — ALCM and cruise missiles.
- **SAM Battery** — auto-tracks phantoms, ghasts, elytra players, other missiles, and other airborne threats.
- **Search Radar** — right-click to list nearby airborne contacts; pulses a warning to nearby players.
- **Targeting Tablet** — use on a block to lock coordinates, then use on a launcher to program the strike.

### Weapons

- **MANPADS** — shoulder tube that consumes SAM / AAM / ALCM from your inventory.
- **Gauss Rifle** — rapid magnetic slug thrower (uses gauss slugs).
- **Railgun** — hold to charge, release a heavier slug.
- **Plasma Blade** — netherite-tier energy sword that ignites targets.

### Apex composite armor

Better protection than netherite, with per-piece bonuses:

- Helmet: night vision
- Chestplate: fire resistance
- Leggings: speed
- Boots: reduced fall damage
- Full set: resistance, plus heavy cuts to explosion and fire damage

## Quick start

1. Craft **Apex Alloy**, **Circuit Boards**, **Guidance Chips**, **Solid Fuel**, and a **Warhead**.
2. Assemble a missile and its matching launcher.
3. Place the launcher, right-click it with the missile to load.
4. Optional: lock a target with the tablet, then use the tablet on the launcher.
5. Shift-right-click the launcher (or power it with redstone) to fire.

All items live in the **Apex Ballistics** creative tab.

## Modular engineering

Carry a missile and use modules on the **Missile Assembly Station** to change
its guidance, payload, fuse, motor stages, accuracy, reliability, and
anti-jamming capability. Targeting tablets support up to six waypoints:
click normally to begin a route and sneak-click additional blocks to append
waypoints.

Payloads include EMP, incendiary, penetrator, fragmentation, decoy, and MIRV.
Fuses include impact, proximity, airburst, and delayed modes. Ballistic rounds
support staged boost and re-entry effects; terrain-following guidance samples
the landscape ahead of cruise missiles.

## Layered defense and infrastructure

- Linked radar extends compatible SAM battery coverage and reports tracking
  quality through terrain occlusion.
- Passive detectors, CIWS, laser defense, interceptors, VLS blocks, mobile
  deployable launchers, and command consoles form a layered defense network.
- Missile racks, loading cranes, propellant refineries, maintenance stations,
  capacitor chargers, and submarine control blocks provide logistics.
- Reinforced concrete variants, blast steel, bunker glass, animated blast and
  security doors, and silo hatches support hardened base construction.

## Optional factions and IFF

Factions are optional and use vanilla scoreboard teams. Players with no team
remain neutral and are not automatically attacked by player-owned defenses.
Members of the same team are friendly. The reserved team name `outlaws` is
treated as hostile:

```text
/team add outlaws
/team join outlaws <player>
```

No player is required to join a faction.

## Powered equipment

Apex armor stores energy and accepts one module per piece at a Maintenance
Station: thermal vision, radar warning, shield, mobility, camouflage, or
medical support. Capacitor Chargers refill armor and cool gauss/rail weapons.
Magnetic weapons build heat and temporarily refuse to fire when overheated.

## Config

`config/apexballistics-common.toml`

- `missileGriefing` — destroy blocks on detonation
- `icbmBlast` / `slbmBlast` / `cruiseBlast` / `samBlast` — explosion power (vanilla TNT is 4.0)

## Design notes

The guidance, flight profiles, arming delay, target acquisition, trails, and
relative yields are designed to feel believable at Minecraft's scale. They are
gameplay simulations, not real-world weapon-design calculations. Server owners
can disable terrain damage with `missileGriefing=false`.
