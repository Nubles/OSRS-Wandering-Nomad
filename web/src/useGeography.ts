import { useEffect, useState } from 'react';
import { Geography } from './engine/geography';
import { World } from './engine/world';
import HEIGHTS from './heightBounds.json';

let cached: Promise<Geography> | null = null;

/** Loads world.dat once (the same file the plugin ships) and builds the geography from it. */
export function loadGeography(): Promise<Geography> {
  cached ??= fetch(`${import.meta.env.BASE_URL}world.dat`)
    .then((r) => {
      if (!r.ok) throw new Error(`world.dat: ${r.status}`);
      return r.arrayBuffer();
    })
    .then((buf) => World.fromGzip(buf))
    .then((w) => new Geography(w));
  return cached;
}

export function useGeography(): Geography | null {
  const [geo, setGeo] = useState<Geography | null>(null);
  useEffect(() => {
    let live = true;
    loadGeography().then((g) => live && setGeo(g));
    return () => {
      live = false;
    };
  }, []);
  return geo;
}

let mapImage: Promise<HTMLImageElement> | null = null;

export function loadMapImage(): Promise<HTMLImageElement> {
  mapImage ??= new Promise((resolve, reject) => {
    const img = new Image();
    img.decoding = 'async';
    img.onload = () => resolve(img);
    img.onerror = reject;
    img.src = `${import.meta.env.BASE_URL}map.jpg`;
  });
  return mapImage;
}

export interface Heights {
  cols: number;
  rows: number;
  /** Game units per stored step (128 game units = 1 tile). */
  scale: number;
  /** One byte per sample, rows from the north, the same way up as map.jpg. */
  data: Uint8Array;
}

let heights: Promise<Heights> | null = null;

/** Ground heights from the game's map data, for the 3D view (built by scripts/build_heights.py). */
export function loadHeights(): Promise<Heights> {
  heights ??= fetch(`${import.meta.env.BASE_URL}heights.bin`)
    .then((r) => {
      if (!r.ok) throw new Error(`heights.bin: ${r.status}`);
      return r.arrayBuffer();
    })
    .then((buf) => {
      const data = new Uint8Array(buf);
      if (data.length !== HEIGHTS.cols * HEIGHTS.rows) throw new Error('heights.bin has the wrong size');
      return { ...HEIGHTS, data };
    });
  return heights;
}
