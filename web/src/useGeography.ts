import { useEffect, useState } from 'react';
import { Geography } from './engine/geography';
import { World } from './engine/world';

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
