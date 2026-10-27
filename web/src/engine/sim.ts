import { dist, Geography, LUMBRIDGE, Tile, VOLCANO } from './geography';
import { Rng } from './rng';

/**
 * The TypeScript twin of NomadSim.java. Same state, same field names (so a run saved by the
 * plugin loads here as is), same order of random draws. Shared test vectors keep the two equal.
 */

export const MAIN = 0;
export const MAIN_MIN = 12;
export const MAIN_MAX = 48;
export const MAIN_START = 24;
export const REGION_MIN = 12;
export const REGION_MAX = 32;
export const REGION_START = 20;

const DRIFT = 15 / 60;
const REGION_DRIFT = 45 / 60;
const SAIL_SPEED = 3;
const BOARD_CHANCE = 1 / 90;
const RETARGET_CHANCE = 1 / 240;
const RADIUS_EASE = 1 / 60;
const PULSE_CHANCE = 1 / 1500;
const PULSE = 8;
const OUTSIDE_SPEEDUP = 5;
const STRANDED_PULL = 0.05;
/** Deep in a peninsula (this many cells from open land), Main leans toward the way out. */
const POCKET_DEPTH = 5;
const POCKET_PULL = 0.1;

export interface Voyage {
  to: string;
  tx: number;
  ty: number;
}

export interface CircleState {
  /** Absent for the Main circle. */
  regionId?: string;
  x: number;
  y: number;
  heading: number;
  radius: number;
  radiusTarget: number;
  radiusMin: number;
  radiusMax: number;
  pulse: number;
  tiers: number;
  voyage?: Voyage;
  atEdge: boolean;
  waiting: boolean;
  touchingYours: boolean;
}

export interface SimState {
  seed: number;
  rngState: number;
  minutes: number;
  bound: number;
  outside: boolean;
  sailingUnlocked: boolean;
  circles: CircleState[];
}

export const contains = (c: CircleState, x: number, y: number) => dist(x - c.x, y - c.y) <= c.radius;
export const touches = (a: CircleState, b: CircleState) => dist(b.x - a.x, b.y - a.y) <= a.radius + b.radius;

export class NomadSim {
  constructor(readonly geo: Geography, public state: SimState) {}

  static start(geo: Geography, seed: number): NomadSim {
    const rng = new Rng(seed);
    const circles: CircleState[] = [];
    const base = (x: number, y: number, heading: number, start: number, min: number, max: number): CircleState => ({
      x, y, heading, radius: start, radiusTarget: start, radiusMin: min, radiusMax: max,
      pulse: 0, tiers: 0, atEdge: false, waiting: false, touchingYours: false,
    });
    circles.push(base(LUMBRIDGE.x, LUMBRIDGE.y, rng.next() * Math.PI * 2, MAIN_START, MAIN_MIN, MAIN_MAX));
    for (const r of geo.regions) {
      circles.push({ regionId: r.id, ...base(r.home.x, r.home.y, rng.next() * Math.PI * 2, REGION_START, REGION_MIN, REGION_MAX) });
    }
    return new NomadSim(geo, {
      seed: seed | 0, rngState: rng.state, minutes: 0, bound: MAIN, outside: false, sailingUnlocked: false, circles,
    });
  }

  /** A deep copy, for looking ahead without touching this run. */
  clone(): NomadSim {
    return new NomadSim(this.geo, structuredClone(this.state));
  }

  get circles() {
    return this.state.circles;
  }

  get main() {
    return this.state.circles[MAIN];
  }

  get bound() {
    return this.state.circles[this.state.bound];
  }

  name(c: CircleState): string {
    return c.regionId ? this.geo.region(c.regionId)!.name : 'Main';
  }

  colour(c: CircleState): number {
    return c.regionId ? this.geo.region(c.regionId)!.colour : 0xff981f;
  }

  stranded(): boolean {
    return this.state.bound !== MAIN && !touches(this.bound, this.main);
  }

  volcanoOpen(): boolean {
    return this.state.bound === MAIN && contains(this.main, VOLCANO.x, VOLCANO.y);
  }

  linked(index: number): boolean {
    return index !== this.state.bound && (this.state.bound === MAIN || index === MAIN);
  }

  canCross(index: number, player: Tile | null): boolean {
    if (index < 0 || index >= this.circles.length || !this.linked(index)) return false;
    const to = this.circles[index];
    if (!touches(this.bound, to)) return false;
    return player == null || (contains(this.bound, player.x, player.y) && contains(to, player.x, player.y));
  }

  crossTo(index: number, player: Tile | null): string | null {
    if (!this.canCross(index, player)) return null;
    this.state.bound = index;
    this.state.outside = false;
    this.circles.forEach((c, j) => {
      c.waiting = false;
      c.touchingYours = this.linked(j) && touches(this.bound, c);
    });
    const to = this.bound;
    return to.regionId
      ? `You crossed into ${this.name(to)}. It never leaves its kingdom, and the only way out is back into Main.`
      : 'You crossed back into the Main circle.';
  }

  setDiaryTiers(regionId: string, tiers: number): string | null {
    const c = this.circles.find((k) => k.regionId === regionId);
    if (!c) return null;
    const r = this.geo.region(regionId)!;
    const clamped = Math.max(0, Math.min(this.geo.maxTiers(r), tiers));
    if (clamped === c.tiers) return null;
    const grew = clamped > c.tiers;
    c.tiers = clamped;
    const bonus = r.diaries.length === 0 ? 0 : (4.0 * clamped) / r.diaries.length;
    const shift = REGION_MIN + bonus - c.radiusMin;
    c.radiusMin += shift;
    c.radiusMax += shift;
    c.radius = clamp(c.radius + shift, c.radiusMin, c.radiusMax);
    c.radiusTarget = clamp(c.radiusTarget + shift, c.radiusMin, c.radiusMax);
    if (!grew) return null;
    c.pulse = 45;
    return `Diary progress: the ${r.name} circle now ranges ${javaRound(c.radiusMin)} to ${javaRound(c.radiusMax)} tiles.`;
  }

  /** Plays one minute and returns the log lines it produced. A null player counts as inside. */
  step(player: Tile | null, rng = new Rng(this.state.rngState)): string[] {
    const s = this.state;
    const log: string[] = [];
    const b = this.bound;
    const main = this.main;

    const out = player != null && !contains(b, player.x, player.y);
    if (out !== s.outside) {
      s.outside = out;
      log.push(out ? 'You are outside your circle. It drifts 5x faster and shrinks 1 tile a minute.' : 'You are back inside your circle.');
    }
    if (out && b.radius > b.radiusMin) {
      b.radius = Math.max(b.radiusMin, b.radius - 1);
      b.radiusTarget = b.radius;
    }

    const stranded = this.stranded();
    if (!stranded) b.waiting = false;
    this.circles.forEach((c, j) => {
      const yours = j === s.bound;
      const pulled = yours && stranded && !!c.regionId;
      this.move(c, rng, yours && out ? OUTSIDE_SPEEDUP : 1, yours, pulled ? main : null, pulled ? STRANDED_PULL : 0, log);
    });
    s.minutes++;

    if (s.minutes % 5 === 0) {
      const yours = this.bound;
      this.circles.forEach((c, j) => {
        if (!this.linked(j)) return;
        const touching = touches(yours, c);
        if (touching && !c.touchingYours) log.push(`Crossing open: your circle touches ${this.name(c)}.`);
        if (!touching && c.touchingYours) {
          log.push(s.bound !== MAIN
            ? `Main moved on. You are stranded in ${this.name(yours)} until it comes back.`
            : `Crossing closed with ${this.name(c)}.`);
        }
        c.touchingYours = touching;
      });
    }
    s.rngState = rng.state;
    return log;
  }

  private move(c: CircleState, rng: Rng, speedup: number, yours: boolean, toward: CircleState | null, pull: number, log: string[]) {
    const geo = this.geo;
    if (c.voyage) {
      const v = c.voyage;
      const d = dist(v.tx - c.x, v.ty - c.y);
      if (d <= SAIL_SPEED) {
        c.x = v.tx;
        c.y = v.ty;
        if (yours) log.push(`Your circle docked at ${v.to}.`);
        delete c.voyage;
        c.heading = rng.next() * Math.PI * 2;
      } else {
        c.x += ((v.tx - c.x) / d) * SAIL_SPEED;
        c.y += ((v.ty - c.y) / d) * SAIL_SPEED;
      }
    } else {
      c.heading += (rng.next() - 0.5) * 0.09;
      if (toward) steer(c, toward.x, toward.y, pull);
      if (!c.regionId && geo.pocketDepth(c.x, c.y) >= POCKET_DEPTH) {
        const out = geo.wayOut(c.x, c.y, 2)!;
        steer(c, out.x, out.y, POCKET_PULL);
      }
      const v = (c.regionId ? REGION_DRIFT : DRIFT) * speedup;
      const nx = c.x + Math.cos(c.heading) * v;
      const ny = c.y + Math.sin(c.heading) * v;
      const region = c.regionId ? geo.region(c.regionId)! : null;
      const allowed = (x: number, y: number) => (region ? geo.inRegion(region, x, y) : geo.onLand(x, y));
      if (allowed(nx, ny)) {
        c.x = nx;
        c.y = ny;
        c.atEdge = false;
      } else {
        const waitingForMain = region != null && toward != null;
        if (!waitingForMain) {
          c.heading = openHeading(c, v, rng, allowed);
          // Turn and step in the same minute, so a circle can never freeze against a shore.
          const tx = c.x + Math.cos(c.heading) * v;
          const ty = c.y + Math.sin(c.heading) * v;
          if (allowed(tx, ty)) {
            c.x = tx;
            c.y = ty;
          }
        }
        if (region) {
          if (yours && waitingForMain && !c.waiting) {
            log.push(`Your circle is waiting at the ${region.name} border, as close to Main as it can get.`);
          }
          if (yours && !waitingForMain && !c.atEdge) log.push(`Your circle reached the ${region.name} border and turned back.`);
          if (yours && waitingForMain) c.waiting = true;
        } else if (yours && !c.atEdge) {
          log.push('Your circle hit the coast and turned back inland.');
        }
        c.atEdge = true;
      }
      if (!c.regionId) this.maybeBoard(c, rng, log);
    }

    if (rng.next() < RETARGET_CHANCE) {
      c.radiusTarget = c.radiusMin + rng.next() * (c.radiusMax - c.radiusMin);
    }
    const dr = c.radiusTarget - c.radius;
    c.radius += Math.sign(dr) * Math.min(Math.abs(dr), RADIUS_EASE);
    if (rng.next() < PULSE_CHANCE) {
      const nr = clamp(c.radius + (rng.next() < 0.5 ? PULSE : -PULSE), c.radiusMin, c.radiusMax);
      if (yours && Math.abs(nr - c.radius) > 0.5) {
        log.push(`Pulse! Your circle ${nr > c.radius ? 'grew' : 'shrank'} by ${javaRound(Math.abs(nr - c.radius))} tiles.`);
      }
      c.radius = nr;
      c.radiusTarget = nr;
      c.pulse = 45;
    }
    if (c.pulse > 0) c.pulse--;
  }

  private maybeBoard(c: CircleState, rng: Rng, log: string[]) {
    const port = this.geo.ports.find((p) => contains(c, p.tile.x, p.tile.y));
    if (!port || rng.next() >= BOARD_CHANCE) return;
    const routes = this.geo.routesFrom(port.name, this.state.sailingUnlocked);
    if (routes.length === 0) return;
    const to = routes[Math.floor(rng.next() * routes.length)];
    const dest = this.geo.port(to)!;
    c.x = port.tile.x;
    c.y = port.tile.y;
    c.voyage = { to, tx: dest.tile.x, ty: dest.tile.y };
    log.push(`The Main circle boarded a boat at ${port.name}, bound for ${to}.`);
  }
}

const LOOK_DIRECTIONS = 32;
const LOOK_RANGE = 96;
const LOOK_SHARE = 0.75;

/**
 * At a coast or border the circle looks around and heads for open ground. It scores 32 headings by
 * how far it could walk along each (from its next step, a tile at a time, out to 96 tiles), then
 * picks one of the headings that score at least 75% of the best. A blind random turn could keep a
 * circle bouncing around inside a peninsula like the Lighthouse's for days.
 */
function openHeading(c: CircleState, step: number, rng: Rng, ok: (x: number, y: number) => boolean): number {
  const scores: number[] = [];
  let best = 0;
  for (let k = 0; k < LOOK_DIRECTIONS; k++) {
    const a = (k * Math.PI * 2) / LOOK_DIRECTIONS;
    const cos = Math.cos(a);
    const sin = Math.sin(a);
    let score = 0;
    for (let d = step; d <= LOOK_RANGE && ok(c.x + cos * d, c.y + sin * d); d += 1) score++;
    scores.push(score);
    best = Math.max(best, score);
  }
  if (best === 0) return c.heading + Math.PI;
  const good: number[] = [];
  scores.forEach((score, k) => {
    if (score >= best * LOOK_SHARE) good.push(k);
  });
  return (good[Math.floor(rng.next() * good.length)] * Math.PI * 2) / LOOK_DIRECTIONS;
}

function steer(c: CircleState, tx: number, ty: number, weight: number) {
  let d = Math.atan2(ty - c.y, tx - c.x) - c.heading;
  d = Math.atan2(Math.sin(d), Math.cos(d));
  c.heading += d * weight;
}

function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, v));
}

/** Java's Math.round: halves round up. */
function javaRound(v: number): number {
  return Math.floor(v + 0.5);
}
