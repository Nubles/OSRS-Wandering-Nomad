#!/usr/bin/env python3
"""
Builds web/public/heights.bin: the ground height of the surface world, for the website's 3D view.

Source: the game cache's map squares, from abextm/osrs-cache at a pinned commit (index 5, one
archive per map square, archive id = regionX << 8 | regionY, file 0 = terrain). Terrain files are
not encrypted. Tiles without a stored height use the client's own procedural height noise.

Format: one unsigned byte per sample, rows from the north (matching map.jpg), STEP tiles per sample.
A byte is the block's mean height above the lowest ground in units of SCALE game units
(128 game units = 1 tile). The sizes are written to web/src/heightBounds.json.

Usage: python3 scripts/build_heights.py   (needs numpy; caches the cache file in build/)
"""
import base64
import bz2
import gzip
import json
import math
import os
import struct
import urllib.request

import numpy as np

CACHE_URL = "https://raw.githubusercontent.com/abextm/osrs-cache/f8c0df47b59ce103aa727c608299e90ebafbf2d2/5.flatcache"
STEP = 4
SCALE = 16

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHED = os.path.join(ROOT, "build", "maps-5.flatcache")
BOUNDS = os.path.join(ROOT, "web", "src", "mapBounds.json")
OUT = os.path.join(ROOT, "web", "public", "heights.bin")
OUT_BOUNDS = os.path.join(ROOT, "web", "src", "heightBounds.json")


def load_cache():
    if not os.path.exists(CACHED):
        os.makedirs(os.path.dirname(CACHED), exist_ok=True)
        with urllib.request.urlopen(CACHE_URL, timeout=300) as r, open(CACHED + ".tmp", "wb") as f:
            f.write(r.read())
        os.replace(CACHED + ".tmp", CACHED)
    archives, cur = {}, None
    with open(CACHED) as f:
        for line in f:
            key, _, value = line.rstrip("\n").partition("=")
            if key == "id":
                cur = {"files": 0}
                archives[int(value)] = cur
            elif cur is None:
                continue
            elif key == "contents":
                cur["contents"] = value
            elif key == "file":
                cur["files"] += 1
    return archives


def unpack(archive):
    """The first file of an archive (the terrain, for a map square)."""
    b = base64.b64decode(archive["contents"])
    kind, length = b[0], struct.unpack(">i", b[1:5])[0]
    if kind == 0:
        data = b[5:5 + length]
    elif kind == 1:
        data = bz2.decompress(b"BZh1" + b[9:9 + length])
    else:
        data = gzip.decompress(b[9:9 + length])
    n = archive["files"]
    if n == 1:
        return data
    chunks = data[-1]
    p = len(data) - 1 - chunks * n * 4
    out, offset = bytearray(), 0
    for _ in range(chunks):
        size = 0
        for f in range(n):
            size += struct.unpack(">i", data[p:p + 4])[0]
            p += 4
            if f == 0:
                out += data[offset:offset + size]
            offset += size
    return bytes(out)


def terrain(data):
    """Plane 0 heights as a 64x64 array [x][y]; procedural tiles are left as None-marked -1."""
    h = np.full((64, 64), 1, dtype=np.int64)  # 1 marks "procedural"
    p = 0
    for z in range(4):
        for x in range(64):
            for y in range(64):
                while True:
                    a = struct.unpack_from(">H", data, p)[0]
                    p += 2
                    if a == 0:
                        break
                    if a == 1:
                        v = data[p]
                        p += 1
                        if z == 0:
                            h[x, y] = -(0 if v == 1 else v) * 8
                        break
                    if a <= 49:
                        p += 2
    return h


# The client's procedural height noise, vectorised. Integer maths wraps at 32 bits as in Java.
COS = np.array([int(65536.0 * math.cos(i * 0.0030679615)) for i in range(2048)], dtype=np.int64)


def i32(n):
    return ((n + 2 ** 31) & 0xFFFFFFFF) - 2 ** 31


def noise(x, y):
    n = i32(x + y * 57)
    n = i32(n ^ i32(n << 13))
    return ((i32(n * i32(i32(n * n) * 15731 + 789221) + 1376312589) & 0x7FFFFFFF) >> 19) & 255


def smooth(x, y):
    corners = noise(x - 1, y - 1) + noise(x + 1, y - 1) + noise(x - 1, y + 1) + noise(x + 1, y + 1)
    sides = noise(x - 1, y) + noise(x + 1, y) + noise(x, y - 1) + noise(x, y + 1)
    return noise(x, y) // 4 + sides // 8 + corners // 16


def interpolate(a, b, x, f):
    k = (65536 - COS[x * 1024 // f]) >> 1
    return ((k * b) >> 16) + ((a * (65536 - k)) >> 16)


def interpolated(x, y, f):
    ix, fx, iy, fy = x // f, x & (f - 1), y // f, y & (f - 1)
    a = interpolate(smooth(ix, iy), smooth(ix + 1, iy), fx, f)
    b = interpolate(smooth(ix, iy + 1), smooth(ix + 1, iy + 1), fx, f)
    return interpolate(a, b, fy, f)


def procedural(x, y):
    n = (interpolated(x + 45365, y + 91923, 4) - 128 + ((interpolated(10294 + x, y + 37821, 2) - 128) >> 1)
         + ((interpolated(x, y, 1) - 128) >> 2))
    n = np.trunc(0.3 * n).astype(np.int64) + 35
    return -np.clip(n, 10, 60) * 8


def main():
    with open(BOUNDS) as f:
        b = json.load(f)
    width, height = b["x1"] - b["x0"], b["yTop"] - b["yBottom"]
    archives = load_cache()

    # World heights for every tile of the map image, rows from the north.
    world = np.zeros((height, width), dtype=np.int64)
    ys, xs = np.mgrid[0:height, 0:width]
    tile_x, tile_y = xs + b["x0"], b["yTop"] - 1 - ys
    flat = procedural(tile_x + 932731, tile_y + 556238)
    world[:] = flat
    for rx in range(b["x0"] // 64, (b["x1"] - 1) // 64 + 1):
        for ry in range(b["yBottom"] // 64, (b["yTop"] - 1) // 64 + 1):
            archive = archives.get(rx << 8 | ry)
            if archive is None or "contents" not in archive:
                continue
            h = terrain(unpack(archive))
            for lx in range(64):
                px = rx * 64 + lx - b["x0"]
                if not 0 <= px < width:
                    continue
                for ly in range(64):
                    py = b["yTop"] - 1 - (ry * 64 + ly)
                    if 0 <= py < height and h[lx, ly] != 1:
                        world[py, px] = h[lx, ly]

    cols, rows = math.ceil(width / STEP), math.ceil(height / STEP)
    padded = np.pad(world, ((0, rows * STEP - height), (0, cols * STEP - width)), mode="edge")
    blocks = padded.reshape(rows, STEP, cols, STEP).mean(axis=(1, 3))
    up = -blocks  # game heights grow downward
    base = up.min()
    q = np.clip(np.round((up - base) / SCALE), 0, 255).astype(np.uint8)
    with open(OUT, "wb") as f:
        f.write(q.tobytes())
    with open(OUT_BOUNDS, "w") as f:
        json.dump({"step": STEP, "cols": cols, "rows": rows, "scale": SCALE}, f)
        f.write("\n")
    clipped = int((np.round((up - base) / SCALE) > 255).sum())
    print(f"heights.bin: {cols}x{rows}, range {int(up.max() - base)} game units, {clipped} samples clipped")


if __name__ == "__main__":
    main()
