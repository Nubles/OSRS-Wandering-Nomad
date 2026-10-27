#!/usr/bin/env python3
"""
Builds src/main/resources/com/wanderingnomad/sim/world.dat: where the land is and which kingdom
each part of it belongs to. The circles read it to stay on land and inside their kingdom.

Sources, both fetched at a pinned version:
  - Land and water: the rendered surface map from mejrs/layers_osrs (1 pixel per tile at zoom 0).
    A pixel is water when it is clearly blue. Rivers and other water narrower than about 9 tiles
    are folded back into the land, so a circle crosses a river instead of bouncing off it.
  - Kingdoms: Fate Locked's region chunk table (data/regionChunks.ts), which follows the One Chunk
    Man Chunk Picker's kingdom grouping. Land outside every listed chunk counts as sea.

Format (big-endian):
  'NMW1', int cell (tiles per cell side), int x0, int y0, int cols, int rows,
  land bits row by row from the south, cols*rows bits packed MSB first,
  int chunkX0, int chunkY0, int chunkCols, int chunkRows, then one byte per chunk: kingdom
  index + 1 in KINGDOMS order, 0 for none.

Usage: python3 scripts/build_world.py [--preview] [--site-map]   (needs numpy and Pillow; caches tiles in build/maptiles)
"""
import gzip
import io
import json
import os
import re
import struct
import sys
import urllib.request
from collections import deque

import numpy as np
from PIL import Image

MAP_TILES = "https://raw.githubusercontent.com/mejrs/layers_osrs/master/mapsquares/-1/0/0_{x}_{y}.png"
REGION_CHUNKS = "https://raw.githubusercontent.com/Nubles/OSRS-Fate-Locked/58dd596a09d1e082e956d18af1f39bda6ede9ac8/data/regionChunks.ts"

# Kingdom ids in the order the Java side lists them (Geography.KINGDOM_ORDER).
KINGDOMS = [
    ("kandarin", "Kandarin"), ("fremennik", "Fremennik"), ("asgarnia", "Asgarnia"),
    ("misthalin", "Misthalin"), ("desert", "Kharidian Desert"), ("morytania", "Morytania"),
    ("wilderness", "Wilderness"), ("karamja", "Karamja"), ("kourend", "Kourend & Kebos"),
    ("tirannwn", "Tirannwn"), ("varlamore", "Varlamore"), ("islands", "Islands & Others"),
    ("seas", "The Open Seas"),
]

# Zoom 0 tiles cover 4x4 map squares (256x256 tiles); these span the surface world.
TX0, TX1, TY0, TY1 = 4, 16, 8, 16
CELL = 4
SURFACE_MAX_Y = 4160
RIVER = 4  # water thinner than 2 * RIVER + 1 tiles is folded into the land

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CACHE = os.path.join(ROOT, "build", "maptiles")
OUT = os.path.join(ROOT, "src", "main", "resources", "com", "wanderingnomad", "sim", "world.dat")


def fetch(url):
    with urllib.request.urlopen(url, timeout=60) as r:
        return r.read()


def world_image():
    os.makedirs(CACHE, exist_ok=True)
    w = (TX1 - TX0 + 1) * 256
    h = (TY1 - TY0 + 1) * 256
    img = np.zeros((h, w, 3), dtype=np.int32)
    for tx in range(TX0, TX1 + 1):
        for ty in range(TY0, TY1 + 1):
            path = os.path.join(CACHE, f"{tx}_{ty}.png")
            if not os.path.exists(path):
                try:
                    data = fetch(MAP_TILES.format(x=tx, y=ty))
                except Exception:
                    continue
                with open(path, "wb") as f:
                    f.write(data)
            try:
                tile = np.asarray(Image.open(path).convert("RGB"), dtype=np.int32)
            except Exception:
                continue  # a tile past the map's edge
            # Row 0 of the stitched image is the north edge.
            row = (TY1 - ty) * 256
            img[row:row + 256, (tx - TX0) * 256:(tx - TX0 + 1) * 256] = tile
    return img


def shift_min(mask, k):
    """Erode with a (2k+1)-square kernel."""
    out = mask.copy()
    for axis in (0, 1):
        acc = out.copy()
        for d in range(1, k + 1):
            acc &= np.roll(out, d, axis) & np.roll(out, -d, axis)
        out = acc
    return out


def shift_max(mask, k):
    """Dilate with a (2k+1)-square kernel."""
    out = mask.copy()
    for axis in (0, 1):
        acc = out.copy()
        for d in range(1, k + 1):
            acc |= np.roll(out, d, axis) | np.roll(out, -d, axis)
        out = acc
    return out


def kingdom_chunks():
    src = fetch(REGION_CHUNKS).decode("utf-8")
    names = {name: i for i, (_, name) in enumerate(KINGDOMS)}
    chunks = {}
    for m in re.finditer(r"'([^']+)': \[([\s\S]*?)\n  \],", src):
        if m.group(1) not in names:
            sys.exit(f"unknown kingdom in the chunk table: {m.group(1)}")
        for cx, cy in re.findall(r"cx: (\d+), cy: (\d+)", m.group(2)):
            chunks.setdefault((int(cx), int(cy)), names[m.group(1)])
    return chunks


def main():
    img = world_image()
    h, w, _ = img.shape
    x0, y0 = TX0 * 256, TY0 * 256
    r, g, b = img[..., 0], img[..., 1], img[..., 2]
    water = (b > r + 28) & (b > g + 12)
    # Fold rivers and narrow channels back into the land: open the water mask.
    sea = shift_max(shift_min(water, RIVER), RIVER)

    chunks = kingdom_chunks()
    ys = y0 + (h - 1 - np.arange(h))  # world y of each image row
    xs = x0 + np.arange(w)
    cx = (xs >> 6)[None, :].repeat(h, 0)
    cy = (ys >> 6)[:, None].repeat(w, 1)
    listed = np.zeros((h, w), dtype=bool)
    for (kx, ky) in chunks:
        listed |= (cx == kx) & (cy == ky)
    land = ~sea & listed & (cy < (SURFACE_MAX_Y >> 6))

    # Cells: land when most of the cell is land. Rows run from the south.
    rows, cols = h // CELL, w // CELL
    flipped = land[::-1]
    cells = flipped[:rows * CELL, :cols * CELL].reshape(rows, CELL, cols, CELL).mean(axis=(1, 3)) >= 0.5

    out = io.BytesIO()
    out.write(b"NMW1")
    out.write(struct.pack(">iiiii", CELL, x0, y0, cols, rows))
    out.write(np.packbits(cells.astype(np.uint8).ravel()).tobytes())
    kx0, ky0 = x0 >> 6, y0 >> 6
    kcols, krows = w >> 6, h >> 6
    out.write(struct.pack(">iiii", kx0, ky0, kcols, krows))
    table = bytearray(kcols * krows)
    for (kx, ky), k in chunks.items():
        if kx0 <= kx < kx0 + kcols and ky0 <= ky < ky0 + krows:
            table[(ky - ky0) * kcols + (kx - kx0)] = k + 1
    out.write(bytes(table))

    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "wb") as f:
        f.write(gzip.compress(out.getvalue(), mtime=0))
    print(f"world.dat: {cols}x{rows} cells of {CELL} tiles, {int(cells.sum())} land, {len(chunks)} kingdom chunks")

    if "--site-map" in sys.argv:
        site_map(img, land, x0, y0)

    if "--preview" in sys.argv:
        preview = np.zeros((rows, cols, 3), dtype=np.uint8)
        preview[:] = (40, 80, 200)
        colours = [(79, 195, 247), (176, 190, 197), (121, 134, 203), (156, 204, 101), (255, 213, 79),
                   (186, 104, 200), (239, 83, 80), (38, 166, 154), (240, 98, 146), (129, 199, 132),
                   (255, 167, 38), (161, 136, 127), (144, 164, 174)]
        for row in range(rows):
            wy = y0 + row * CELL
            for col in range(cols):
                if cells[row, col]:
                    k = chunks.get(((x0 + col * CELL) >> 6, wy >> 6))
                    preview[row, col] = colours[k] if k is not None else (120, 160, 80)
        Image.fromarray(preview[::-1]).save(os.path.join(ROOT, "build", "world-preview.png"))


def site_map(img, land, x0, y0):
    """The website's background: the game map at 1 pixel per tile, cropped to the land plus a margin."""
    h = img.shape[0]
    rows = np.where(land.any(axis=1))[0]
    cols = np.where(land.any(axis=0))[0]
    margin = 64
    top, bottom = max(0, rows[0] - margin), min(h, rows[-1] + 1 + margin)
    left, right = max(0, cols[0] - margin), min(img.shape[1], cols[-1] + 1 + margin)
    crop = np.clip(img[top:bottom, left:right], 0, 255).astype(np.uint8)
    web = os.path.join(ROOT, "web", "public")
    os.makedirs(web, exist_ok=True)
    Image.fromarray(crop).save(os.path.join(web, "map.jpg"), quality=82, optimize=True, progressive=True)
    # World x of the left edge, and world y of the top and bottom edges (y grows northward).
    bounds = {"x0": x0 + int(left), "x1": x0 + int(right), "yTop": y0 + (h - int(top)), "yBottom": y0 + (h - int(bottom))}
    with open(os.path.join(ROOT, "web", "src", "mapBounds.json"), "w") as f:
        f.write(json.dumps(bounds) + "\n")
    print(f"map.jpg: {right - left}x{bottom - top}, {bounds}")


if __name__ == "__main__":
    main()
