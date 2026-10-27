#!/usr/bin/env python3
"""
Builds the game cache the 3D world loads, into public/caches/<name>/.

The source is abextm/osrs-cache at a pinned commit: one "flat cache" text file per index, with
each archive stored as a base64 container. This turns those into the game's own disk layout
(main_file_cache.dat2, .idxN and .idx255), rebuilding the reference tables from the flat files'
metadata. Only the indexes the viewer reads are kept, so a first visit downloads less; the rest
get empty tables.

Usage: python3 scripts/build_cache.py   (downloads into build-cache/ the first time)
"""
import base64
import json
import os
import struct
import urllib.request

COMMIT = "f8c0df47b59ce103aa727c608299e90ebafbf2d2"
REVISION = 241
DATE = "2026-10-20"
NAME = f"osrs-{REVISION}_{DATE}"
# animations, skeletons, configs, maps, models, sprites, textures, graphics defaults. Index 22 (the newer keyframe
# animations, about 95MB) is left out: the few creatures that use them stand still instead.
INDEXES = [0, 1, 2, 5, 7, 8, 9, 17]
LAST_INDEX = 24

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DOWNLOADS = os.path.join(ROOT, "build-cache")
OUT = os.path.join(ROOT, "public", "caches")
URL = "https://raw.githubusercontent.com/abextm/osrs-cache/{commit}/{index}.flatcache"

SECTOR = 520


def parse(path):
    header, archives, cur = {}, [], None
    with open(path) as f:
        for line in f:
            key, _, value = line.rstrip("\n").partition("=")
            if key == "id":
                cur = {"id": int(value), "namehash": 0, "revision": 0, "crc": 0, "contents": b"", "files": []}
                archives.append(cur)
            elif cur is None:
                header[key] = value
            elif key == "contents":
                cur["contents"] = base64.b64decode(value)
            elif key in ("namehash", "revision", "crc"):
                cur[key] = int(value)
            elif key == "file":
                fid, _, name = value.partition("=")
                cur["files"].append((int(fid), int(name or 0)))
    archives.sort(key=lambda a: a["id"])
    return header, archives


def big_smart(v):
    return struct.pack(">h", v) if v < 32768 else struct.pack(">I", v | 0x80000000)


def i32(v):
    return struct.pack(">i", ((v + 2 ** 31) % 2 ** 32) - 2 ** 31)


def reference_table(header, archives):
    protocol = int(header.get("protocol", 7))
    named = header.get("named", "false") == "true" or any(a["namehash"] for a in archives)
    small = (lambda v: big_smart(v)) if protocol >= 7 else (lambda v: struct.pack(">H", v))
    out = bytearray([protocol])
    if protocol >= 6:
        out += i32(int(header.get("revision", 0)))
    out.append(1 if named else 0)
    out += small(len(archives))
    last = 0
    for a in archives:
        out += small(a["id"] - last)
        last = a["id"]
    if named:
        for a in archives:
            out += i32(a["namehash"])
    for a in archives:
        out += i32(a["crc"])
    for a in archives:
        out += i32(a["revision"])
    for a in archives:
        out += small(len(a["files"]))
    for a in archives:
        last = 0
        for fid, _ in a["files"]:
            out += small(fid - last)
            last = fid
    if named:
        for a in archives:
            for _, name in a["files"]:
                out += i32(name)
    return bytes(out)


class Writer:
    def __init__(self, out):
        self.out = out
        self.dat = open(os.path.join(out, "main_file_cache.dat2"), "wb")
        self.dat.write(b"\0" * SECTOR)  # sector 0 is never used
        self.next = 1
        self.indexes = {}

    def put(self, index, archive, data):
        extended = archive > 0xFFFF
        size = 510 if extended else 512
        first = self.next
        chunks = [data[i:i + size] for i in range(0, len(data), size)] or [b""]
        for n, chunk in enumerate(chunks):
            sector = self.next
            self.next += 1
            following = self.next if n + 1 < len(chunks) else 0
            if extended:
                head = struct.pack(">IH", archive, n)
            else:
                head = struct.pack(">HH", archive, n)
            head += following.to_bytes(3, "big") + bytes([index])
            self.dat.write((head + chunk).ljust(SECTOR, b"\0"))
        entries = self.indexes.setdefault(index, {})
        entries[archive] = (len(data), first)

    def close(self, indexes):
        self.dat.close()
        # An index left out still gets an empty file, so asking it for an archive finds nothing.
        for index in indexes:
            self.indexes.setdefault(index, {})
        for index, entries in self.indexes.items():
            count = max(entries) + 1 if entries else 0
            buf = bytearray(6 * count)
            for archive, (length, sector) in entries.items():
                buf[archive * 6:archive * 6 + 6] = length.to_bytes(3, "big") + sector.to_bytes(3, "big")
            with open(os.path.join(self.out, f"main_file_cache.idx{index}"), "wb") as f:
                f.write(buf)


def download(index):
    path = os.path.join(DOWNLOADS, f"{index}.flatcache")
    if not os.path.exists(path):
        os.makedirs(DOWNLOADS, exist_ok=True)
        with urllib.request.urlopen(URL.format(commit=COMMIT, index=index), timeout=600) as r, open(path + ".tmp", "wb") as f:
            while True:
                chunk = r.read(1 << 20)
                if not chunk:
                    break
                f.write(chunk)
        os.replace(path + ".tmp", path)
    return path


def main():
    out = os.path.join(OUT, NAME)
    os.makedirs(out, exist_ok=True)
    w = Writer(out)
    tables = {}
    for index in range(LAST_INDEX + 1):
        if index not in INDEXES:
            tables[index] = None
            continue
        header, archives = parse(download(index))
        for a in archives:
            if a["contents"]:
                w.put(index, a["id"], a["contents"])
        tables[index] = reference_table(header, archives)
        print(f"index {index}: {len(archives)} archives")
    for index, table in tables.items():
        if table is None:
            table = bytes([7]) + i32(0) + bytes([0]) + big_smart(0)
        w.put(255, index, bytes([0]) + struct.pack(">i", len(table)) + table)
    w.close(range(LAST_INDEX + 1))

    # Map squares in this cache are not encrypted, so there are no keys to hand out.
    with open(os.path.join(out, "keys.json"), "w") as f:
        f.write("{}\n")
    size = sum(os.path.getsize(os.path.join(out, n)) for n in os.listdir(out))
    info = [{"name": NAME, "game": "oldschool", "environment": "live", "revision": REVISION,
             "timestamp": f"{DATE}T00:00:00.000Z", "size": size}]
    with open(os.path.join(OUT, "caches.json"), "w") as f:
        json.dump(info, f, indent=2)
        f.write("\n")
    print(f"{NAME}: {size / 1048576:.0f}MB")


if __name__ == "__main__":
    main()
