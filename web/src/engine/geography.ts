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
  /** The Sailing level its docking point needs (oldschool.runescape.wiki/w/Docking_points). */
  sailing: number;
  /** A quest its docking point needs done, or null. */
  quest: string | null;
}

export const CORSAIR_CURSE = 'The Corsair Curse';
export const FREMENNIK_TRIALS = 'The Fremennik Trials';

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

/** Docks by map chunk, with each docking point's Sailing level and quest, as in Geography.java. */
const PORT_CHUNKS: [string, number, number, number, string | null][] = [
  ['Port Sarim', 47, 50, 1, null],
  ['Musa Point', 46, 49, 10, null],
  ['Rimmington', 45, 50, 18, null],
  ['Brimhaven', 43, 50, 25, null],
  ['East Ardougne', 41, 51, 28, null],
  ['Catherby', 43, 53, 20, null],
  ['Port Khazard', 41, 49, 30, null],
  ['Corsair Cove', 40, 44, 40, CORSAIR_CURSE],
  ["Void Knights' Outpost", 41, 41, 50, null],
  ['Entrana', 44, 52, 36, null],
  ['Port Piscarilius', 28, 57, 15, null],
  ["Land's End", 23, 53, 5, null],
  ['Rellekka', 41, 57, 62, FREMENNIK_TRIALS],
  ['Waterbirth Island', 39, 58, 74, FREMENNIK_TRIALS],
];

/** Quest-free boats, each sailing between every pair of its docks. */
const BOATS: string[][] = [
  ['Port Sarim', 'Musa Point'],
  ['Port Sarim', 'Port Piscarilius', "Land's End"],
  ['Port Sarim', 'Entrana'],
  ['East Ardougne', 'Brimhaven', 'Rimmington'],
  ['Port Sarim', "Void Knights' Outpost"],
  ['Rellekka', 'Waterbirth Island'],
  ['Port Sarim', 'Musa Point', 'Brimhaven', 'Catherby', 'Port Khazard', 'Corsair Cove', 'Port Piscarilius', "Land's End"],
];

/** The Trade road's caravan: a small circle with no kingdom that travels between bank towns. As in Geography.java. */
export const CARAVAN: Region = { id: 'caravan', name: 'Caravan', colour: 0xd7a86e, index: -1, diaries: [], home: { x: 0, y: 0 } };

/** The bank towns the caravan travels between, in the order of Geography.java. */
export const TOWNS: Port[] = ([
  ['Lumbridge', 3222, 3218],
  ['Varrock', 3185, 3436],
  ['Draynor Village', 3092, 3245],
  ['Falador', 3013, 3355],
  ['Edgeville', 3094, 3493],
  ['Al Kharid', 3269, 3167],
  ['Catherby', 2809, 3441],
  ["Seers' Village", 2725, 3491],
  ['East Ardougne', 2655, 3283],
  ['Yanille', 2612, 3094],
  ['Canifis', 3512, 3480],
  ['Shantay Pass', 3308, 3120],
] as [string, number, number][]).map(([name, x, y]) => ({ name, tile: { x, y }, sailing: 0, quest: null }));

export const LUMBRIDGE: Tile = { x: 3222, y: 3218 };
export const VOLCANO: Tile = { x: 2856, y: 3168 };

/** The outline of Gielinor the circles move over, built from world.dat like Geography.java. */
export class Geography {
  readonly regions: Region[];
  readonly ports: Port[];

  constructor(readonly world: World) {
    this.ports = PORT_CHUNKS.map(([name, cx, cy, sailing, quest]) => ({
      name,
      tile: this.nearestLand(cx * 64 + 32, cy * 64 + 32),
      sailing,
      quest,
    }));
    this.regions = REGIONS.map((r) => ({ ...r, home: { x: 0, y: 0 } }));
    for (const r of this.regions) r.home = this.middleOf(r);
  }

  region(id: string): Region | undefined {
    return this.regions.find((r) => r.id === id) ?? (id === CARAVAN.id ? CARAVAN : undefined);
  }

  town(name: string): Port | undefined {
    return TOWNS.find((p) => p.name === name);
  }

  private homes = new Map<string, Port>();

  /** A kingdom's home tile as a destination for a walk over land. As in Geography.java. */
  homeOf(r: Region): Port {
    let p = this.homes.get(r.id);
    if (!p) {
      p = { name: `home:${r.id}`, tile: r.home, sailing: 0, quest: null };
      this.homes.set(r.id, p);
    }
    return p;
  }

  /** The middle of the walk's cell nearest a tile (its own or one beside it); null if none is on the walk. Mirrors Harbours.snap. */
  snapToWalk(p: Port, x: number, y: number): Tile | null {
    const w = this.world;
    const i = cellIndex(w, x, y);
    if (i < 0) return null;
    const field = this.harbourField(p);
    const row = Math.floor(i / w.cols);
    const col = i - row * w.cols;
    let best: Tile | null = null;
    let bestD = 0;
    for (let dr = -1; dr <= 1; dr++) {
      for (let dc = -1; dc <= 1; dc++) {
        const c = col + dc;
        const r = row + dr;
        if (c < 0 || r < 0 || c >= w.cols || r >= w.rows || field[r * w.cols + c] < 0) continue;
        const at = { x: w.x0 + (c + 0.5) * w.cell, y: w.y0 + (r + 0.5) * w.cell };
        const d = dist(at.x - x, at.y - y);
        if (best == null || d < bestD) {
          best = at;
          bestD = d;
        }
      }
    }
    return best;
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

  private pockets: Int32Array | null = null;

  private pocketField(): Int32Array {
    return (this.pockets ??= pocketField(this.world));
  }

  /** How many cells of walking it is from a tile out to open land: 0 in open land, -1 at sea or on an island. */
  pocketDepth(x: number, y: number): number {
    const w = this.world;
    const col = Math.floor((x - w.x0) / w.cell);
    const row = Math.floor((y - w.y0) / w.cell);
    if (col < 0 || row < 0 || col >= w.cols || row >= w.rows) return -1;
    return this.pocketField()[row * w.cols + col];
  }

  /**
   * In a pocket of land (a peninsula, a cove behind a narrow neck), a point a few cells along the
   * shortest walk out to open land; null in open land or where no walk leads out (an island).
   * Open land is a cell with at least 70% land within 32 tiles of it.
   */
  wayOut(x: number, y: number, lookahead: number): Tile | null {
    const w = this.world;
    const field = this.pocketField();
    let col = Math.floor((x - w.x0) / w.cell);
    let row = Math.floor((y - w.y0) / w.cell);
    if (col < 0 || row < 0 || col >= w.cols || row >= w.rows || field[row * w.cols + col] <= 0) return null;
    for (let step = 0; step < lookahead; step++) {
      const here = field[row * w.cols + col];
      if (here === 0) break;
      for (const [dc, dr] of NEIGHBOURS) {
        const c = col + dc;
        const r = row + dr;
        if (c < 0 || r < 0 || c >= w.cols || r >= w.rows) continue;
        const there = field[r * w.cols + c];
        if (there >= 0 && there < here) {
          col = c;
          row = r;
          break;
        }
      }
    }
    return { x: w.x0 + (col + 0.5) * w.cell, y: w.y0 + (row + 0.5) * w.cell };
  }

  private harbours = new Map<string, Int16Array>();

  private harbourField(p: Port): Int16Array {
    let f = this.harbours.get(p.name);
    if (!f) {
      f = harbourField(this.world, p.tile);
      this.harbours.set(p.name, f);
    }
    return f;
  }

  /** Cells of walking over land from a tile to a port, or -1 if no walk leads there. Mirrors Harbours.java. */
  harbourSteps(p: Port, x: number, y: number): number {
    const i = cellIndex(this.world, x, y);
    return i < 0 ? -1 : this.harbourField(p)[i];
  }

  /**
   * Where to head for next on the shortest walk over land to a port: the furthest of the next few
   * cells along it that can be reached in a straight line over land. Null if no walk leads there.
   */
  wayTo(p: Port, x: number, y: number, lookahead: number): Tile | null {
    const w = this.world;
    const i = cellIndex(w, x, y);
    const field = this.harbourField(p);
    if (i < 0 || field[i] < 0) return null;
    let row = Math.floor(i / w.cols);
    let col = i - row * w.cols;
    const centre = (c: number, r: number): Tile => ({ x: w.x0 + (c + 0.5) * w.cell, y: w.y0 + (r + 0.5) * w.cell });
    let best = centre(col, row);
    for (let step = 0; step < lookahead; step++) {
      const here = field[row * w.cols + col];
      if (here === 0) break;
      for (const [dc, dr] of NEIGHBOURS) {
        const c = col + dc;
        const r = row + dr;
        if (c < 0 || r < 0 || c >= w.cols || r >= w.rows) continue;
        const there = field[r * w.cols + c];
        if (there >= 0 && there < here) {
          col = c;
          row = r;
          break;
        }
      }
      const next = centre(col, row);
      if (!inSight(w, x, y, next.x, next.y)) break;
      best = next;
    }
    return best;
  }

  inRegion(r: Region, x: number, y: number): boolean {
    // The caravan has no kingdom: it may go anywhere on land.
    if (r.index < 0) return this.world.land(x, y);
    return this.world.kingdom(x, y) === r.index && this.world.land(x, y);
  }

  maxTiers(r: Region): number {
    return 4 * r.diaries.length;
  }

  port(name: string): Port | undefined {
    return this.ports.find((p) => p.name === name);
  }

  /**
   * Where a boat can take Main from a port: every quest-free boat's stops, and with Sailing
   * (`sailing` is the level, 0 without it; `true` means level unknown) every dock your level
   * reaches whose quest isn't in `questsMissing`. Sailing's docks come first, in the list's order.
   */
  routesFrom(name: string, sailing: number | boolean, questsMissing: string[] = []): string[] {
    const level = sailing === true ? 99 : sailing === false ? 0 : sailing;
    const out = new Set<string>();
    if (level > 0) {
      for (const p of this.ports) {
        if (p.name === name || p.sailing > level) continue;
        if (p.quest !== null && questsMissing.includes(p.quest)) continue;
        out.add(p.name);
      }
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

const NEIGHBOURS = [[1, 0], [-1, 0], [0, 1], [0, -1]] as const;
const OPEN_RADIUS = 8;
const OPEN_SHARE = 0.7;

function cellIndex(w: World, x: number, y: number): number {
  const col = Math.floor((x - w.x0) / w.cell);
  const row = Math.floor((y - w.y0) / w.cell);
  if (col < 0 || row < 0 || col >= w.cols || row >= w.rows) return -1;
  return row * w.cols + col;
}

/** Whether the straight line between two points stays over land, checked every half tile. */
function inSight(w: World, ax: number, ay: number, bx: number, by: number): boolean {
  const n = Math.ceil(dist(bx - ax, by - ay) * 2);
  for (let k = 1; k <= n; k++) {
    if (!w.land(ax + ((bx - ax) * k) / n, ay + ((by - ay) * k) / n)) return false;
  }
  return true;
}

/** Steps (in cells) over land from each land cell to a port's cell; -1 for water or no way there. */
function harbourField(w: World, t: Tile): Int16Array {
  const { cols, rows, cell, x0, y0 } = w;
  const field = new Int16Array(cols * rows).fill(-1);
  const start = cellIndex(w, t.x, t.y);
  const land = (i: number) => {
    const r = Math.floor(i / cols);
    const c = i - r * cols;
    return w.land(x0 + (c + 0.5) * cell, y0 + (r + 0.5) * cell);
  };
  if (start < 0 || !land(start)) return field;
  const queue = new Int32Array(cols * rows);
  let head = 0;
  let tail = 0;
  field[start] = 0;
  queue[tail++] = start;
  while (head < tail) {
    const i = queue[head++];
    if (field[i] >= 32767) continue;
    const r = Math.floor(i / cols);
    const c = i - r * cols;
    for (const [dc, dr] of NEIGHBOURS) {
      const cc = c + dc;
      const rr = r + dr;
      if (cc < 0 || rr < 0 || cc >= cols || rr >= rows) continue;
      const j = rr * cols + cc;
      if (field[j] >= 0 || !land(j)) continue;
      field[j] = field[i] + 1;
      queue[tail++] = j;
    }
  }
  return field;
}

/** Steps (in cells) from each land cell to the nearest open land: 0 in open land, -1 for water or no way out. */
function pocketField(w: World): Int32Array {
  const { cols, rows, cell, x0, y0 } = w;
  const land = new Uint8Array(cols * rows);
  for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) land[r * cols + c] = w.land(x0 + (c + 0.5) * cell, y0 + (r + 0.5) * cell) ? 1 : 0;
  // Summed-area table for land counts in a square window.
  const sat = new Int32Array((cols + 1) * (rows + 1));
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      sat[(r + 1) * (cols + 1) + c + 1] = land[r * cols + c] + sat[r * (cols + 1) + c + 1] + sat[(r + 1) * (cols + 1) + c] - sat[r * (cols + 1) + c];
    }
  }
  const window = (2 * OPEN_RADIUS + 1) ** 2;
  const field = new Int32Array(cols * rows).fill(-1);
  const queue = new Int32Array(cols * rows);
  let head = 0;
  let tail = 0;
  for (let r = 0; r < rows; r++) {
    for (let c = 0; c < cols; c++) {
      if (!land[r * cols + c]) continue;
      const c0 = Math.max(0, c - OPEN_RADIUS);
      const r0 = Math.max(0, r - OPEN_RADIUS);
      const c1 = Math.min(cols, c + OPEN_RADIUS + 1);
      const r1 = Math.min(rows, r + OPEN_RADIUS + 1);
      const count = sat[r1 * (cols + 1) + c1] - sat[r0 * (cols + 1) + c1] - sat[r1 * (cols + 1) + c0] + sat[r0 * (cols + 1) + c0];
      if (count >= window * OPEN_SHARE) {
        field[r * cols + c] = 0;
        queue[tail++] = r * cols + c;
      }
    }
  }
  while (head < tail) {
    const i = queue[head++];
    const r = Math.floor(i / cols);
    const c = i - r * cols;
    for (const [dc, dr] of NEIGHBOURS) {
      const cc = c + dc;
      const rr = r + dr;
      if (cc < 0 || rr < 0 || cc >= cols || rr >= rows) continue;
      const j = rr * cols + cc;
      if (!land[j] || field[j] >= 0) continue;
      field[j] = field[i] + 1;
      queue[tail++] = j;
    }
  }
  return field;
}
