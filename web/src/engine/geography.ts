import { World } from './world';

export interface Tile {
  x: number;
  y: number;
}

export interface Region {
  id: string;
  name: string;
  /** The region circle's colour, as 0xRRGGBB. */
  colour: number;
  /** The kingdom's index in world.dat. */
  index: number;
  /** Diary names, such as "Lumbridge" or "Ardougne". */
  diaries: string[];
  /** Where the region circle starts: the kingdom's land nearest its middle. */
  home: Tile;
}

export interface Port {
  name: string;
  /** A land tile at the dock, where the Main circle's centre lands after a voyage. */
  tile: Tile;
}

/** The eleven kingdoms with a region circle, in the order of Geography.java. */
const REGIONS: Omit<Region, 'home'>[] = [
  { id: 'kandarin', name: 'Kandarin', colour: 0x4fc3f7, index: 0, diaries: ['Kandarin', 'Ardougne'] },
  { id: 'fremennik', name: 'Fremennik', colour: 0xb0bec5, index: 1, diaries: ['Fremennik'] },
  { id: 'asgarnia', name: 'Asgarnia', colour: 0x7986cb, index: 2, diaries: ['Falador'] },
  { id: 'misthalin', name: 'Misthalin', colour: 0x9ccc65, index: 3, diaries: ['Lumbridge', 'Varrock'] },
  { id: 'desert', name: 'Kharidian Desert', colour: 0xffd54f, index: 4, diaries: ['Desert'] },
  { id: 'morytania', name: 'Morytania', colour: 0xba68c8, index: 5, diaries: ['Morytania'] },
  { id: 'wilderness', name: 'Wilderness', colour: 0xef5350, index: 6, diaries: ['Wilderness'] },
  { id: 'karamja', name: 'Karamja', colour: 0x26a69a, index: 7, diaries: ['Karamja'] },
  { id: 'kourend', name: 'Kourend & Kebos', colour: 0xf06292, index: 8, diaries: ['Kourend'] },
  { id: 'tirannwn', name: 'Tirannwn', colour: 0x81c784, index: 9, diaries: ['Western'] },
  // Varlamore has no Achievement Diary yet, so its circle doesn't grow.
  { id: 'varlamore', name: 'Varlamore', colour: 0xffa726, index: 10, diaries: [] },
];

/** Docks by map chunk, as in Geography.java. */
const PORT_CHUNKS: [string, number, number][] = [
  ['Port Sarim', 47, 50],
  ['Musa Point', 46, 49],
  ['Rimmington', 45, 50],
  ['Brimhaven', 43, 50],
  ['East Ardougne', 41, 51],
  ['Catherby', 43, 53],
  ['Port Khazard', 41, 49],
  ['Corsair Cove', 40, 44],
  ["Void Knights' Outpost", 41, 41],
  ['Entrana', 44, 52],
  ['Port Piscarilius', 28, 57],
  ["Land's End", 23, 53],
  ['Rellekka', 41, 57],
  ['Waterbirth Island', 39, 58],
];

/** Quest-free boats, each sailing between every pair of its docks. */
const BOATS: string[][] = [
  ['Port Sarim', 'Musa Point'],
  ['Port Sarim', 'Port Piscarilius', "Land's End"],
  ['Port Sarim', 'Entrana'],
  ['East Ardougne', 'Brimhaven', 'Rimmington'],
  ['Rimmington', 'Corsair Cove'],
  ['Port Sarim', "Void Knights' Outpost"],
  ['Rellekka', 'Waterbirth Island'],
  ['Port Sarim', 'Musa Point', 'Brimhaven', 'Catherby', 'Port Khazard', 'Corsair Cove', 'Port Piscarilius', "Land's End"],
];

export const LUMBRIDGE: Tile = { x: 3222, y: 3218 };
export const VOLCANO: Tile = { x: 2856, y: 3168 };

/** The outline of Gielinor the circles move over, built from world.dat like Geography.java. */
export class Geography {
  readonly regions: Region[];
  readonly ports: Port[];

  constructor(readonly world: World) {
    this.ports = PORT_CHUNKS.map(([name, cx, cy]) => ({ name, tile: this.nearestLand(cx * 64 + 32, cy * 64 + 32) }));
    this.regions = REGIONS.map((r) => ({ ...r, home: { x: 0, y: 0 } }));
    for (const r of this.regions) r.home = this.middleOf(r);
  }

  region(id: string): Region | undefined {
    return this.regions.find((r) => r.id === id);
  }

  /** The kingdom with a region circle that a tile lies in, or undefined. */
  regionAt(x: number, y: number): Region | undefined {
    if (!this.world.land(x, y)) return undefined;
    const k = this.world.kingdom(x, y);
    return this.regions.find((r) => r.index === k);
  }

  onLand(x: number, y: number): boolean {
    return this.world.land(x, y);
  }

  inRegion(r: Region, x: number, y: number): boolean {
    return this.world.kingdom(x, y) === r.index && this.world.land(x, y);
  }

  maxTiers(r: Region): number {
    return 4 * r.diaries.length;
  }

  port(name: string): Port | undefined {
    return this.ports.find((p) => p.name === name);
  }

  /** Before Sailing, the quest-free boats only; after Sailing, any port. */
  routesFrom(name: string, sailing: boolean): string[] {
    const out = new Set<string>();
    if (sailing) {
      for (const p of this.ports) if (p.name !== name) out.add(p.name);
      return [...out];
    }
    for (const boat of BOATS) {
      if (!boat.includes(name)) continue;
      for (const stop of boat) if (stop !== name) out.add(stop);
    }
    return [...out];
  }

  private nearestLand(x: number, y: number): Tile {
    const step = this.world.cell;
    for (let ring = 0; ring < 64; ring++) {
      for (let dx = -ring; dx <= ring; dx++) {
        for (let dy = -ring; dy <= ring; dy++) {
          if (Math.max(Math.abs(dx), Math.abs(dy)) !== ring) continue;
          const tx = x + dx * step;
          const ty = y + dy * step;
          if (this.world.land(tx, ty)) return { x: tx, y: ty };
        }
      }
    }
    return { x, y };
  }

  private middleOf(r: Omit<Region, 'home'>): Tile {
    const w = this.world;
    const step = w.cell;
    let sx = 0;
    let sy = 0;
    let n = 0;
    const cells: Tile[] = [];
    for (let row = 0; row < w.rows; row++) {
      for (let col = 0; col < w.cols; col++) {
        const x = w.x0 + col * step + (step >> 1);
        const y = w.y0 + row * step + (step >> 1);
        if (w.kingdom(x, y) === r.index && w.land(x, y)) {
          sx += x;
          sy += y;
          n++;
          cells.push({ x, y });
        }
      }
    }
    if (n === 0) throw new Error(`${r.name} has no land in world.dat`);
    const mx = sx / n;
    const my = sy / n;
    let best = cells[0];
    let bestD = Infinity;
    // Same scan order as Java, so ties go to the same cell.
    for (const c of cells) {
      const d = dist(c.x - mx, c.y - my);
      if (d < bestD) {
        bestD = d;
        best = c;
      }
    }
    return best;
  }
}

/** Distance as sqrt(dx² + dy²), which every platform rounds the same way (Math.hypot does not). */
export function dist(dx: number, dy: number): number {
  return Math.sqrt(dx * dx + dy * dy);
}
