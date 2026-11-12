#!/usr/bin/env python3
"""
Builds web/public/places.json for "What's in my circle": the useful places in the game world,
read from the game cache itself, so it updates when the cache does.

From the same pinned abextm/osrs-cache commit the 3D world uses, it reads every object's
definition (its name and menu options) and every object
placed in every map square, then keeps:

- objects told apart by name and menu options the way the plugin's gains and losses tells them
  (bank booths and chests, altars you can pray at, furnaces, ranges, anvils, fairy rings), and
- deposit boxes, spinning wheels, looms and potter's wheels by name.

Places of one kind within a few tiles of each other become one place. Fishing spots are creatures
and the cache doesn't place them, so they, mining sites, dungeon entrances, rare trees and farming
patches come from RuneLite's world map plugin (BSD 2-Clause). Shops are people too, and no open
table places them, so they're left out.

Usage: python3 web/scripts/build_places.py   (downloads into web/build-cache/ the first time,
or reuses world/build-cache/ if the 3D world was built)
"""
import base64
import bz2
import gzip
import json
import os
import struct
import sys
import urllib.request

COMMIT = "f8c0df47b59ce103aa727c608299e90ebafbf2d2"
URL = "https://raw.githubusercontent.com/abextm/osrs-cache/{commit}/{index}.flatcache"
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REPO = os.path.dirname(ROOT)
CACHES = [os.path.join(REPO, "world", "build-cache"), os.path.join(ROOT, "build-cache")]
OUT = os.path.join(ROOT, "public", "places.json")

CONFIG_INDEX = 2
MAP_INDEX = 5
LOC_GROUP = 6
# The map index archive that holds world areas, not a map square.
WORLD_AREA = (98 << 8) | 199
# Places of one kind closer than this are one place.
MERGE = 6


def flatcache(index):
    for d in CACHES:
        p = os.path.join(d, f"{index}.flatcache")
        if os.path.exists(p):
            return p
    os.makedirs(CACHES[1], exist_ok=True)
    p = os.path.join(CACHES[1], f"{index}.flatcache")
    print(f"downloading index {index}...", file=sys.stderr)
    urllib.request.urlretrieve(URL.format(commit=COMMIT, index=index), p)
    return p


def parse(path):
    archives, cur = [], None
    with open(path) as f:
        for line in f:
            key, _, value = line.rstrip("\n").partition("=")
            if key == "id":
                cur = {"id": int(value), "namehash": 0, "contents": b"", "files": []}
                archives.append(cur)
            elif cur is None:
                continue
            elif key == "contents":
                cur["contents"] = base64.b64decode(value)
            elif key == "namehash":
                cur["namehash"] = int(value)
            elif key == "file":
                fid, _, _ = value.partition("=")
                cur["files"].append(int(fid))
    return archives


def unpack(container):
    kind = container[0]
    length = struct.unpack(">i", container[1:5])[0]
    if kind == 0:
        return container[5:5 + length]
    data = container[9:9 + length]
    if kind == 1:
        return bz2.decompress(b"BZh1" + data)
    return gzip.decompress(data)


def split(data, files):
    n = len(files)
    if n == 1:
        return {files[0]: data}
    chunks = data[-1]
    p = len(data) - 1 - chunks * n * 4
    sizes = []
    for _ in range(chunks):
        size = 0
        row = []
        for _ in range(n):
            size += struct.unpack(">i", data[p:p + 4])[0]
            p += 4
            row.append(size)
        sizes.append(row)
    out = [bytearray() for _ in range(n)]
    off = 0
    for row in sizes:
        for f in range(n):
            out[f] += data[off:off + row[f]]
            off += row[f]
    return {files[f]: bytes(out[f]) for f in range(n)}


class Buf:
    def __init__(self, data):
        self.d = data
        self.p = 0

    def u8(self):
        v = self.d[self.p]
        self.p += 1
        return v

    def s8(self):
        v = self.u8()
        return v - 256 if v > 127 else v

    def u16(self):
        v = struct.unpack(">H", self.d[self.p:self.p + 2])[0]
        self.p += 2
        return v

    def s16(self):
        v = struct.unpack(">h", self.d[self.p:self.p + 2])[0]
        self.p += 2
        return v

    def u24(self):
        return (self.u8() << 16) | (self.u8() << 8) | self.u8()

    def i32(self):
        v = struct.unpack(">i", self.d[self.p:self.p + 4])[0]
        self.p += 4
        return v

    def i64(self):
        self.p += 8

    def counted(self, size, extra=0):
        """Skips a count byte's worth of {@code size}-byte entries (plus {@code extra} more)."""
        n = self.u8()
        self.p += (n + extra) * size

    def string(self):
        end = self.d.index(0, self.p)
        s = self.d[self.p:end].decode("latin-1")
        self.p = end + 1
        return s

    def null_string(self):
        if self.d[self.p] == 0:
            self.p += 1
            return None
        return self.string()

    def big_smart(self):
        if self.d[self.p] >= 128:
            return self.i32() & 0x7FFFFFFF
        v = self.u16()
        return -1 if v == 32767 else v

    def usmart(self):
        if self.d[self.p] < 128:
            return self.u8()
        return self.u16() - 0x8000

    def smart3(self):
        total = 0
        v = self.usmart()
        while v == 32767:
            total += 32767
            v = self.usmart()
        return total + v


def params(b):
    for _ in range(b.u8()):
        kind = b.u8()
        b.u24()
        if kind == 0:
            b.i32()
        elif kind == 1:
            b.string()
        elif kind == 2:
            b.i64()
        else:
            raise ValueError("param type")


def loc(data):
    """An object's name, menu options and world map icon, following the 3D world's own decoder."""
    b = Buf(data)
    name, actions, icon = None, [None] * 5, -1
    while True:
        op = b.u8()
        if op == 0:
            return name, actions, icon
        if op == 1:
            b.counted(3)
        elif op == 2:
            name = b.string()
        elif op == 3:
            b.string()
        elif op == 5:
            b.counted(2)
        elif op == 6:
            b.counted(5)
        elif op == 7:
            b.counted(4)
        elif op in (14, 15, 19, 28, 29, 39, 69, 75):
            b.u8()
        elif op in (17, 18, 21, 22, 23, 25, 27, 62, 64, 73, 74, 88, 89, 90, 94, 97, 98, 103, 105, 168, 169, 177, 189, 190, 191):
            pass
        elif op == 24:
            b.u16()
        elif 30 <= op < 39:
            a = b.string()
            if op - 30 < 5:
                actions[op - 30] = None if a.lower() == "hidden" else a
        elif op in (40, 41):
            b.counted(4)
        elif op in (44, 45, 61, 65, 66, 67, 68, 167):
            b.u16()
        elif op in (60, 82, 107):
            icon = b.u16()
        elif op in (70, 71, 72):
            b.s16()
        elif op in (77, 92):
            b.u16()
            b.u16()
            if op == 92:
                b.u16()
            b.counted(2, 1)
        elif op == 78:
            b.u16()
            b.u8()
            b.u8()
        elif op == 79:
            b.u16()
            b.u16()
            b.u8()
            b.u8()
            b.counted(2)
        elif op == 81:
            b.u8()
        elif op in (91, 95, 96, 104, 178):
            b.u8()
        elif op == 93:
            b.u8()
            b.u16()
            b.u8()
            b.u16()
        elif op == 99:
            b.u8()
            b.u16()
        elif op == 100:
            b.u8()
            b.u8()
            b.string()
        elif op == 101:
            b.u8()
            b.u16()
            b.u16()
            b.i32()
            b.i32()
            b.null_string()
        elif op == 102:
            b.u8()
            b.u16()
            b.u16()
            b.u16()
            b.i32()
            b.i32()
            b.null_string()
        elif op == 106:
            for _ in range(b.u8()):
                b.u16()
                b.u8()
        elif 150 <= op < 155:
            a = b.string()
            actions[op - 150] = None if a.lower() == "hidden" else a
        elif op == 160:
            b.counted(2)
        elif op == 163:
            b.p += 4
        elif op in (170, 171):
            b.usmart()
        elif op == 173:
            b.u16()
            b.u16()
        elif op == 249:
            params(b)
        else:
            raise ValueError(f"opcode {op}")


def kind(name, actions):
    """The plugin's gains and losses rule (look/Amenities.java), for objects."""
    n = (name or "").lower().strip()
    acts = {a.lower().strip() for a in actions if a}
    if "deposit" in n:
        return None
    if "bank" in acts or n in ("bank booth", "bank chest"):
        return "Bank"
    if "altar" in n and ("pray-at" in acts or "pray" in acts):
        return "Altar"
    if n in ("furnace", "small furnace") or "smelt" in acts:
        return "Furnace"
    if n in ("range", "cooking range") or (n.endswith(" range") and "cook" in acts):
        return "Range"
    if n == "anvil":
        return "Anvil"
    if n == "fairy ring":
        return "Fairy ring"
    if n == "spirit tree":
        return "Spirit tree"
    return None


def extra(name, actions):
    """A few more places told by name and options alone: deposit boxes, spinning wheels, looms and potter's wheels."""
    n = (name or "").lower().strip()
    acts = {a.lower().strip() for a in actions if a}
    if "deposit" in n and "deposit" in acts:
        return "Deposit box"
    if n == "spinning wheel":
        return "Spinning wheel"
    if n == "loom":
        return "Loom"
    if n in ("potter's wheel", "pottery wheel"):
        return "Potter's wheel"
    return None


RUNELITE = "master"
RUNELITE_URL = "https://raw.githubusercontent.com/runelite/runelite/{ref}/runelite-client/src/main/java/net/runelite/client/plugins/worldmap/{name}.java"
# RuneLite's world map plugin (BSD 2-Clause) keeps the places that are people and creatures, not objects:
# fishing spots, plus mining sites, dungeon entrances, rare trees and farming patches, by hand.
TABLES = {
    "FishingSpotLocation": "Fishing spot",
    "MiningSiteLocation": "Mining site",
    "DungeonLocation": "Dungeon",
    "RareTreeLocation": "Rare tree",
    "FarmingPatchLocation": "Farming patch",
}


def nice(word):
    return word.replace("_", " ").capitalize()


def runelite():
    import re
    out = {}
    for name, label in TABLES.items():
        try:
            src = urllib.request.urlopen(RUNELITE_URL.format(ref=RUNELITE, name=name)).read().decode("utf-8")
        except Exception as e:
            print(f"couldn't read {name}: {e}", file=sys.stderr)
            continue
        start = re.search(r"^enum \w+\s*\{", src, re.M)
        if not start:
            continue
        body = src[start.end():]
        body = body[:body.index(";")]
        starts = [(m.start(), m.group(1)) for m in re.finditer(r"^\t([A-Z0-9_]+)\(", body, re.M)]
        for k, (at, entry) in enumerate(starts):
            args = body[at:starts[k + 1][0] if k + 1 < len(starts) else len(body)]
            points = [(int(x), int(y)) for x, y, z in re.findall(r"new WorldPoint\((\d+),\s*(\d+),\s*(\d+)\)", args) if z == "0"]
            if not points:
                continue
            quoted = re.findall(r'"([^"]+)"', args)
            if name == "FishingSpotLocation":
                what = ", ".join(sorted({nice(t) for t in re.findall(r"FishingSpot\.([A-Z_]+)", args)}))
            elif name == "MiningSiteLocation":
                what = ", ".join(sorted({nice(t) for t in re.findall(r"Ore\.([A-Z_]+)", args)}))
            elif quoted:
                what = quoted[0]
            else:
                what = nice(entry)
            for x, y in points:
                out.setdefault(label, []).append([x, y, what])
    for label, pts in out.items():
        print(f"{label}: {len(pts)} from RuneLite", file=sys.stderr)
    return out


def main():
    configs = {a["id"]: a for a in parse(flatcache(CONFIG_INDEX))}
    locs = split(unpack(configs[LOC_GROUP]["contents"]), configs[LOC_GROUP]["files"])
    wanted = {}
    skipped = 0
    for lid, data in locs.items():
        try:
            name, actions, icon = loc(data)
        except Exception:
            skipped += 1
            continue
        k = kind(name, actions)
        e = extra(name, actions)
        if k or e:
            wanted[lid] = (k, e)
    print(f"{len(locs)} objects, {len(wanted)} kept, {skipped} not read", file=sys.stderr)

    # Each map square is one archive, numbered (x << 8) | y, holding its terrain (file 0) and its objects (file 1).
    maps = {a["id"]: a for a in parse(flatcache(MAP_INDEX))}
    places = {}
    squares = 0
    for mx in range(0, 256):
        for my in range(0, 256):
            a = maps.get((mx << 8) | my)
            if a is None or a["id"] == WORLD_AREA or 1 not in a["files"]:
                continue
            try:
                data = split(unpack(a["contents"]), a["files"])[1]
            except Exception:
                continue
            squares += 1
            b = Buf(data)
            oid = -1
            while True:
                d = b.smart3()
                if d == 0:
                    break
                oid += d
                pos = 0
                while True:
                    pd = b.usmart()
                    if pd == 0:
                        break
                    pos += pd - 1
                    b.u8()
                    if oid not in wanted:
                        continue
                    x = mx * 64 + ((pos >> 6) & 0x3F)
                    y = my * 64 + (pos & 0x3F)
                    level = pos >> 12
                    if level != 0:
                        continue
                    for label in wanted[oid]:
                        if label:
                            places.setdefault(label, []).append((x, y))
    print(f"{squares} map squares read", file=sys.stderr)

    labelled = runelite()
    out = {}
    for label, pts in sorted(places.items()):
        merged = []
        for x, y in sorted(pts):
            for m in merged:
                if abs(m[0] / m[2] - x) <= MERGE and abs(m[1] / m[2] - y) <= MERGE:
                    m[0] += x
                    m[1] += y
                    m[2] += 1
                    break
            else:
                merged.append([x, y, 1])
        out[label] = [[round(m[0] / m[2]), round(m[1] / m[2])] for m in merged]
    for label, pts in sorted(labelled.items()):
        out[label] = pts
    with open(OUT, "w") as f:
        json.dump({"cache": COMMIT, "runelite": RUNELITE, "places": out}, f, separators=(",", ":"))
    print(f"wrote {OUT}: {sum(len(v) for v in out.values())} places in {len(out)} kinds", file=sys.stderr)


if __name__ == "__main__":
    main()
