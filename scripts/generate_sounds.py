#!/usr/bin/env python3
"""Generate looping siren OGGs (synthetic tones, no third-party samples)."""
from __future__ import annotations

import math
import struct
import subprocess
import tempfile
import wave
from pathlib import Path

RATE = 22050
OUT = Path("/workspace/src/main/resources/assets/apexballistics/sounds")


def write_wav(path: Path, samples: list[float]) -> None:
    with wave.open(str(path), "w") as wav:
        wav.setnchannels(1)
        wav.setsampwidth(2)
        wav.setframerate(RATE)
        frames = b"".join(struct.pack("<h", max(-32767, min(32767, int(s * 30000)))) for s in samples)
        wav.writeframes(frames)


def ffmpeg_ogg(wav_path: Path, ogg_path: Path) -> None:
    subprocess.check_call([
        "ffmpeg", "-y", "-loglevel", "error", "-i", str(wav_path),
        "-c:a", "libvorbis", "-q:a", "4", str(ogg_path),
    ])


def air_raid(seconds: float = 3.2) -> list[float]:
    samples = []
    n = int(RATE * seconds)
    for i in range(n):
        t = i / RATE
        cycle = t % 3.0
        if cycle < 1.5:
            freq = 420 + 360 * (cycle / 1.5)
        else:
            freq = 780 - 360 * ((cycle - 1.5) / 1.5)
        env = 0.55 + 0.45 * math.sin(math.tau * t / 3.0)
        samples.append(math.sin(math.tau * freq * t) * env * 0.9)
    return samples


def industrial(seconds: float = 2.4) -> list[float]:
    samples = []
    n = int(RATE * seconds)
    for i in range(n):
        t = i / RATE
        high = (t % 0.8) < 0.4
        freq = 640 if high else 390
        buzz = 0.25 * math.sin(math.tau * freq * 2 * t)
        samples.append((math.sin(math.tau * freq * t) + buzz) * 0.7)
    return samples


def nuclear(seconds: float = 4.0) -> list[float]:
    samples = []
    n = int(RATE * seconds)
    for i in range(n):
        t = i / RATE
        cycle = t % 4.0
        freq = 330 + 220 * (0.5 - 0.5 * math.cos(math.tau * cycle / 4.0))
        env = 0.45 + 0.55 * math.sin(math.pi * cycle / 4.0)
        samples.append(math.sin(math.tau * freq * t) * env)
    return samples


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory() as tmp:
        tmp_path = Path(tmp)
        jobs = {
            "air_raid_siren": air_raid(),
            "industrial_siren": industrial(),
            "nuclear_siren": nuclear(),
        }
        for name, samples in jobs.items():
            wav = tmp_path / f"{name}.wav"
            write_wav(wav, samples)
            ffmpeg_ogg(wav, OUT / f"{name}.ogg")
    print("generated siren sounds")


if __name__ == "__main__":
    main()
