import type { CircleState } from './engine/sim';

/** A useful spot from the game cache (web/scripts/build_places.py): a bank, an altar, a fishing spot and so on. */
export interface Place {
  kind: string;
  x: number;
  y: number;
  label?: string;
  /** Underground, placed at the surface above it (the plugin's Below law does the same with entrances). */
  below: boolean;
}

/** Each kind's dot on the map. */
export const KIND_COLOURS: Record<string, string> = {
  Bank: '#ffd54f',
  'Deposit box': '#ffe082',
  Altar: '#e1f5fe',
  Furnace: '#ff7043',
  Anvil: '#b0bec5',
  Range: '#ffab91',
  'Fairy ring': '#ce93d8',
  'Spirit tree': '#81c784',
  'Spinning wheel': '#f5deb3',
  Loom: '#d7ccc8',
  "Potter's wheel": '#bcaaa4',
  'Fishing spot': '#4fc3f7',
  'Mining site': '#a1887f',
  'Rare tree': '#66bb6a',
  'Farming patch': '#aed581',
  Dungeon: '#9575cd',
};

/** The surface map's rows; anything else is a dungeon one map up, or an instance with no surface. */
const SURFACE_Y = [2400, 4300];
const BELOW_OFFSET = 6400;

let cached: Promise<Place[]> | null = null;

export function loadPlaces(): Promise<Place[]> {
  cached ??= fetch(`${import.meta.env.BASE_URL}places.json`)
    .then((r) => {
      if (!r.ok) throw new Error(`places.json: ${r.status}`);
      return r.json() as Promise<{ places: Record<string, (number | string)[][]> }>;
    })
    .then((d) => normalise(d.places));
  return cached;
}

export function normalise(raw: Record<string, (number | string)[][]>): Place[] {
  const out: Place[] = [];
  for (const [kind, list] of Object.entries(raw)) {
    for (const p of list) {
      const x = Number(p[0]);
      let y = Number(p[1]);
      let below = false;
      if (y >= SURFACE_Y[0] + BELOW_OFFSET && y <= SURFACE_Y[1] + BELOW_OFFSET) {
        y -= BELOW_OFFSET;
        below = true;
      }
      if (y < SURFACE_Y[0] || y > SURFACE_Y[1]) continue;
      out.push({ kind, x, y, label: typeof p[2] === 'string' ? p[2] : undefined, below });
    }
  }
  return out;
}

/** The places inside a circle, nearest its middle first. */
export function inside(places: Place[], c: Pick<CircleState, 'x' | 'y' | 'radius'>): (Place & { off: number })[] {
  return places
    .map((p) => ({ ...p, off: Math.hypot(p.x - c.x, p.y - c.y) }))
    .filter((p) => p.off <= c.radius)
    .sort((a, b) => a.off - b.off);
}

/** Places grouped by kind, kinds in the order of KIND_COLOURS. */
export function byKind<T extends Place>(list: T[]): [string, T[]][] {
  const order = Object.keys(KIND_COLOURS);
  const groups = new Map<string, T[]>();
  for (const p of list) groups.set(p.kind, [...(groups.get(p.kind) ?? []), p]);
  return [...groups.entries()].sort((a, b) => order.indexOf(a[0]) - order.indexOf(b[0]));
}
