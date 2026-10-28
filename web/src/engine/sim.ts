import { dist, Geography, LUMBRIDGE, Tile, VOLCANO } from './geography';
import { LANDMARKS, Landmark } from './landmarks';
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
const OUTSIDE_SPEEDUP = 5;
const STRANDED_PULL = 0.05;
/** Deep in a peninsula (this many cells from open land), Main leans toward the way out. */
const POCKET_DEPTH = 5;
const CRAMPED_DEPTH = 2;
const POCKET_PULL = 0.1;
/** Lean: within this many tiles of the edge, your circle turns toward you, up to 3 degrees a minute. */
const LEAN_BAND = 2;
const LEAN = (3 * Math.PI) / 180;

export const ROAM = 0;
export const MEANDER = 1;
export const LOOP = 2;
export const DASH = 3;
export const LINGER = 4;
export const DRIFT_MOOD = 5;
export const MOOD_NAMES = ['Roaming', 'Meandering', 'Looping', 'Dashing', 'Lingering', 'Drifting'];
const MOOD_LOG = [
  'sets off for somewhere new', 'starts to meander', 'starts to circle round', 'breaks into a dash',
  'slows to a crawl', 'drifts aimlessly',
];
/** Per mood: speed, shortest and longest minutes, turn wobble, and how hard it heads for its waypoint. */
const MOOD_SPEED = [1.0, 0.9, 0.9, 2.2, 0.15, 1.0];
const MOOD_MIN = [90, 60, 60, 20, 30, 60];
const MOOD_MAX = [240, 180, 150, 60, 90, 180];
const MOOD_WOBBLE = [0.006, 0.004, 0.003, 0.002, 0.03, 0.012];
const MOOD_PULL = [0.05, 0.02, 0.008, 0.06, 0, 0.01];
const TURN_EASE = 0.1;
const TURN_MAX = 0.08;
const SPEED_EASE = 0.05;
const MEANDER_SWING = 0.03;
const MEANDER_STEP = (Math.PI * 2) / 80;
const LOOP_MIN = 0.018;
const LOOP_MAX = 0.035;
const FEEL_MINUTES = 30;
const FEEL_SPLAY = 0.6;
const AVOID_TURN = 0.06;
const AVOID_GIVE_UP = 45;
const WAY_TRIES = 8;
const MAIN_WAY = [150, 500];
const REGION_WAY = [20, 150];
const TRAIL = 3;

/** Boons, by tier. Mirrors NomadSim.java. */
export const EXPAND = 'Expand';
export const TAILWIND = 'Tailwind';
export const ANCHOR = 'Anchor';
export const RUDDER = 'Rudder';
export const BRIDGE = 'Bridge';
export const COMMON = 'common';
export const RARE = 'rare';
const COMMON_BOONS = [EXPAND, TAILWIND, ANCHOR];
const RARE_BOONS = [RUDDER, BRIDGE];
export const BOONS_HELD = 3;
export const CLAIM_REACH = 3;
const EXPAND_TILES = 12;
const EXPAND_MINUTES = 120;
const TAILWIND_MINUTES = 60;
const ANCHOR_MINUTES = 60;
const RUDDER_TURN = Math.PI / 4;
const RUDDER_WAY = 150;
const BRIDGE_MINUTES = 10;
export const BRIDGE_REACH = 60;
const LASTING = 30;

export interface Temperament {
  name: string;
  speed: number;
  /** How likely each mood is to come next, by mood index. */
  weights: number[];
  duration: number;
  pulses: number;
  pulse: number;
  breathing: number;
  hungry: boolean;
}

const temper = (name: string, speed: number, duration: number, weights: number[], extra: Partial<Temperament> = {}): Temperament => ({
  name, speed, duration, weights, pulses: 1, pulse: 8, breathing: 1, hungry: false, ...extra,
});

/** How each circle likes to move. Mirrors Temperament.java. */
export const MAIN_TEMPERAMENT = temper('Restless', 1, 1, [4, 2, 1, 1.5, 0.7, 1]);
export const TEMPERAMENTS: Record<string, Temperament> = {
  misthalin: temper('Gentle', 0.7, 1.4, [3, 2, 0.5, 0.3, 1.5, 2]),
  asgarnia: temper('Dutiful', 1, 1, [2, 1, 4, 0.5, 0.5, 1]),
  kandarin: temper('Curious', 1, 0.6, [2, 3, 1, 1, 0.5, 1]),
  fremennik: temper('Stubborn', 0.5, 1.5, [3, 1, 1, 0.3, 2, 1], { pulse: 12 }),
  // Only dashes and rests, and a mood never repeats, so it alternates between them.
  desert: temper('Sandstorm', 1, 1, [0, 0, 0, 4, 4, 0]),
  morytania: temper('Hungry', 1, 1, [2, 2, 1, 1, 1, 1], { pulses: 2, hungry: true }),
  wilderness: temper('Wild', 1.5, 0.7, [2, 1, 1, 3, 0.3, 1]),
  karamja: temper('Tidal', 1, 1, [1, 2, 2, 1, 1, 1], { breathing: 3 }),
  kourend: temper('Stately', 0.8, 1.5, [4, 1, 2, 0.5, 1, 1]),
  tirannwn: temper('Elusive', 1.1, 0.6, [1, 4, 1, 1, 0.5, 1]),
  varlamore: temper('Proud', 1, 1, [2, 1, 3, 2, 0.5, 0.5]),
};

export const temperamentOf = (c: CircleState): Temperament => (c.regionId && TEMPERAMENTS[c.regionId]) || MAIN_TEMPERAMENT;

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
  /** The current mood (see MOOD_NAMES) and the played minutes it has left. */
  mood: number;
  moodLeft: number;
  /** How fast the heading is turning, in radians a minute. */
  turn: number;
  /** The turn a loop holds, or a meander swings around, this mood. */
  bend: number;
  /** Minutes into a meander's swing. */
  swing: number;
  /** Multiplies the drift speed; eases toward the mood's own speed. */
  speed: number;
  /** Where this mood is heading, 0 when nowhere in particular. */
  wx: number;
  wy: number;
  /** The last few places it headed for, as x, y pairs. */
  trail?: number[];
  /** Minutes in a row spent turning away from a coast or border ahead. */
  avoid: number;
  /** Boons on this circle: tiles Expand added to its range, and the minutes left on each. */
  expand: number;
  expandLeft: number;
  tailwindLeft: number;
  anchorLeft: number;
}

/** A waystone holding one boon at a landmark inside a circle (by index). */
export interface Waystone {
  landmark: string;
  x: number;
  y: number;
  circle: number;
  boon: string;
}

export interface SimState {
  seed: number;
  rngState: number;
  minutes: number;
  bound: number;
  outside: boolean;
  sailingUnlocked: boolean;
  circles: CircleState[];
  waystones: Waystone[];
  boons: string[];
  pending: string[];
  summons: number;
  bridgeLeft: number;
}

export const contains = (c: CircleState, x: number, y: number) => dist(x - c.x, y - c.y) <= c.radius;
export const touches = (a: CircleState, b: CircleState) => dist(b.x - a.x, b.y - a.y) <= a.radius + b.radius;

export class NomadSim {
  constructor(readonly geo: Geography, public state: SimState) {
    // Runs saved before circles had moods load with these missing, as the plugin's Gson leaves them at 0.
    for (const c of state.circles) {
      c.mood ??= 0;
      c.moodLeft ??= 0;
      c.turn ??= 0;
      c.bend ??= 0;
      c.swing ??= 0;
      c.speed ??= 0;
      c.wx ??= 0;
      c.wy ??= 0;
      c.avoid ??= 0;
      c.expand ??= 0;
      c.expandLeft ??= 0;
      c.tailwindLeft ??= 0;
      c.anchorLeft ??= 0;
    }
    // And runs saved before waystones.
    state.waystones ??= [];
    state.boons ??= [];
    state.pending ??= [];
    state.summons ??= 0;
    state.bridgeLeft ??= 0;
  }

  static start(geo: Geography, seed: number): NomadSim {
    const rng = new Rng(seed);
    const circles: CircleState[] = [];
    const base = (x: number, y: number, heading: number, start: number, min: number, max: number): CircleState => ({
      x, y, heading, radius: start, radiusTarget: start, radiusMin: min, radiusMax: max,
      pulse: 0, tiers: 0, atEdge: false, waiting: false, touchingYours: false,
      mood: 0, moodLeft: 0, turn: 0, bend: 0, swing: 0, speed: 1, wx: 0, wy: 0, avoid: 0,
      expand: 0, expandLeft: 0, tailwindLeft: 0, anchorLeft: 0,
    });
    circles.push(base(LUMBRIDGE.x, LUMBRIDGE.y, rng.next() * Math.PI * 2, MAIN_START, MAIN_MIN, MAIN_MAX));
    for (const r of geo.regions) {
      circles.push({ regionId: r.id, ...base(r.home.x, r.home.y, rng.next() * Math.PI * 2, REGION_START, REGION_MIN, REGION_MAX) });
    }
    return new NomadSim(geo, {
      seed: seed | 0, rngState: rng.state, minutes: 0, bound: MAIN, outside: false, sailingUnlocked: false, circles,
      waystones: [], boons: [], pending: [], summons: 0, bridgeLeft: 0,
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
    if (!touches(this.bound, to)) {
      // An open Bridge reaches any circle within 60 tiles, from anywhere inside yours.
      if (this.state.bridgeLeft <= 0 || gap(this.bound, to) > BRIDGE_REACH) return false;
      return player == null || contains(this.bound, player.x, player.y);
    }
    return player == null || (contains(this.bound, player.x, player.y) && contains(to, player.x, player.y));
  }

  /** A milestone summons a waystone of the tier; it rises now, or once a landmark drifts into reach. */
  summon(tier: string): string | null {
    this.state.pending.push(tier === RARE ? RARE : COMMON);
    const log: string[] = [];
    this.placePending(log);
    return log.length ? log[0] : null;
  }

  /** Spends the boon at index. side is for Rudder: 1 turns left, -1 right. */
  useBoon(index: number, side: number): string | null {
    const s = this.state;
    if (index < 0 || index >= s.boons.length) return null;
    const boon = s.boons[index];
    const c = this.bound;
    let line: string;
    switch (boon) {
      case EXPAND:
        if (c.expandLeft <= 0) shiftRange(c, EXPAND_TILES);
        c.expandLeft = EXPAND_MINUTES;
        c.pulse = 45;
        line = `Expand: your circle grows by ${EXPAND_TILES} tiles for 2 hours.`;
        break;
      case TAILWIND:
        c.tailwindLeft = TAILWIND_MINUTES;
        line = 'Tailwind: your circle drifts twice as fast for an hour.';
        break;
      case ANCHOR:
        c.anchorLeft = ANCHOR_MINUTES;
        line = 'Anchor: your circle holds still for an hour.';
        break;
      case RUDDER: {
        if (side === 0 || c.voyage) return null;
        c.heading += (side > 0 ? 1 : -1) * RUDDER_TURN;
        c.turn = 0;
        const wx = round(c.x + Math.cos(c.heading) * RUDDER_WAY);
        const wy = round(c.y + Math.sin(c.heading) * RUDDER_WAY);
        const region = c.regionId ? this.geo.region(c.regionId)! : null;
        const ok = region ? this.geo.inRegion(region, wx, wy) : this.geo.onLand(wx, wy);
        c.wx = ok ? wx : 0;
        c.wy = ok ? wy : 0;
        line = `Rudder: your circle turns 45 degrees to the ${side > 0 ? 'left' : 'right'}.`;
        break;
      }
      case BRIDGE:
        s.bridgeLeft = BRIDGE_MINUTES;
        line = `Bridge: for 10 minutes you can cross to any circle within ${BRIDGE_REACH} tiles, and walking between them doesn't count as outside.`;
        break;
      default:
        return null;
    }
    s.boons.splice(index, 1);
    return line;
  }

  /** Roughly how many played minutes before a waystone's circle drifts off it. An estimate. */
  minutesLeft(w: Waystone): number {
    return minutesLeft(this.circles[w.circle], w.x, w.y);
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
    const shift = REGION_MIN + bonus + c.expand - c.radiusMin;
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

    // An open Bridge lets you walk between circles without it counting as outside.
    const out = player != null && !contains(b, player.x, player.y) && s.bridgeLeft <= 0;
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
      const lean = yours && !out && player != null && dist(player.x - c.x, player.y - c.y) >= c.radius - LEAN_BAND ? player : null;
      const speedup = (yours && out ? OUTSIDE_SPEEDUP : 1) * (c.tailwindLeft > 0 ? 2 : 1);
      this.move(c, rng, speedup, yours, pulled ? main : null, pulled ? STRANDED_PULL : 0, lean, log);
    });
    s.minutes++;
    this.tickBoons(log);

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
    this.waystones(player, log);
    s.rngState = rng.state;
    return log;
  }

  private tickBoons(log: string[]) {
    const s = this.state;
    this.circles.forEach((c, j) => {
      const yours = j === s.bound;
      if (c.tailwindLeft > 0 && --c.tailwindLeft === 0 && yours) log.push('The Tailwind has died down.');
      if (c.anchorLeft > 0 && --c.anchorLeft === 0 && yours) log.push('Your circle lifted its Anchor and drifts again.');
      if (c.expandLeft > 0 && --c.expandLeft === 0) {
        shiftRange(c, -c.expand);
        if (yours) log.push(`Expand wore off: your circle shrank back by ${EXPAND_TILES} tiles.`);
      }
    });
    if (s.bridgeLeft > 0 && --s.bridgeLeft === 0) log.push('The Bridge closed.');
  }

  private waystones(player: Tile | null, log: string[]) {
    const s = this.state;
    if (player != null && s.boons.length < BOONS_HELD) {
      for (let i = 0; i < s.waystones.length; i++) {
        const w = s.waystones[i];
        if (w.circle !== s.bound || !contains(this.bound, player.x, player.y)) continue;
        if (dist(player.x - w.x, player.y - w.y) > CLAIM_REACH) continue;
        s.waystones.splice(i, 1);
        s.boons.push(w.boon);
        log.push(`You claimed the waystone at ${w.landmark}: ${w.boon}. Spend it from the sidebar.`);
        break;
      }
    }
    for (let i = s.waystones.length - 1; i >= 0; i--) {
      const w = s.waystones[i];
      if (contains(this.circles[w.circle], w.x, w.y)) continue;
      s.waystones.splice(i, 1);
      log.push(`The waystone at ${w.landmark} faded: its circle drifted off it.`);
    }
    this.placePending(log);
  }

  private placePending(log: string[]) {
    const s = this.state;
    while (s.pending.length) {
      const line = this.place(s.pending[0]);
      if (line == null) return;
      s.pending.shift();
      log.push(line);
    }
  }

  /** Places a waystone with its own dice, so a milestone never changes where the circles go. Mirrors place in NomadSim.java. */
  private place(tier: string): string | null {
    const s = this.state;
    const reach = [s.bound];
    if (s.bound !== MAIN) {
      reach.push(MAIN);
    } else {
      let nearest = -1;
      this.circles.forEach((c, j) => {
        if (!this.linked(j)) return;
        if (nearest < 0 || gap(this.bound, c) < gap(this.bound, this.circles[nearest])) nearest = j;
      });
      if (nearest >= 0) reach.push(nearest);
    }
    let options: [number, number][] = [];
    for (const j of reach) {
      const c = this.circles[j];
      LANDMARKS.forEach((l, k) => {
        if (!contains(c, l.x, l.y) || s.waystones.some((w) => w.x === l.x && w.y === l.y)) return;
        options.push([k, j]);
      });
    }
    if (!options.length) return null;
    const lasting = options.filter(([k, j]) => minutesLeft(this.circles[j], LANDMARKS[k].x, LANDMARKS[k].y) >= LASTING);
    if (lasting.length) options = lasting;
    const rng = new Rng(s.seed ^ Math.imul(s.summons + 1, 0x9e3779b9 | 0));
    s.summons++;
    const [k, j] = options[Math.floor(rng.next() * options.length)];
    const pool = tier === RARE ? RARE_BOONS : COMMON_BOONS;
    const l: Landmark = LANDMARKS[k];
    const w: Waystone = { landmark: l.name, x: l.x, y: l.y, circle: j, boon: pool[Math.floor(rng.next() * pool.length)] };
    s.waystones.push(w);
    return `A waystone rose at ${l.name}${j === s.bound ? ' in your circle' : ` in ${this.name(this.circles[j])}`}, holding ${w.boon} (${tier === RARE ? 'rare' : 'common'}).`;
  }

  private move(c: CircleState, rng: Rng, speedup: number, yours: boolean, toward: CircleState | null, pull: number, lean: Tile | null, log: string[]) {
    const geo = this.geo;
    const t = temperamentOf(c);
    const region = c.regionId ? geo.region(c.regionId)! : null;
    const allowed = (x: number, y: number) => (region ? geo.inRegion(region, x, y) : geo.onLand(x, y));
    if (c.anchorLeft > 0) {
      // Anchored: it holds its place, voyage and all, and only breathes.
    } else if (c.voyage) {
      const v = c.voyage;
      const d = dist(v.tx - c.x, v.ty - c.y);
      if (d <= SAIL_SPEED) {
        c.x = v.tx;
        c.y = v.ty;
        if (yours) log.push(`Your circle docked at ${v.to}.`);
        delete c.voyage;
        c.heading = rng.next() * Math.PI * 2;
        c.turn = 0;
        c.moodLeft = 0;
      } else {
        c.x += ((v.tx - c.x) / d) * SAIL_SPEED;
        c.y += ((v.ty - c.y) / d) * SAIL_SPEED;
      }
    } else {
      if (c.moodLeft <= 0) this.startMood(c, t, rng, allowed, yours, log);
      c.moodLeft--;
      const m = c.mood;
      // In a peninsula or cove, Main forgets its mood and keeps a steady pace until it's out.
      const depth = region ? 0 : geo.pocketDepth(c.x, c.y);
      const cramped = depth >= CRAMPED_DEPTH;
      const pocket = depth >= POCKET_DEPTH;

      const target = cramped ? 0 : m === LOOP ? c.bend : m === MEANDER ? c.bend * Math.sin(c.swing * MEANDER_STEP) : 0;
      c.swing++;
      c.turn += (target - c.turn) * TURN_EASE + (rng.next() - 0.5) * 2 * MOOD_WOBBLE[m];
      c.turn = clamp(c.turn, -TURN_MAX, TURN_MAX);
      c.heading += c.turn;
      c.speed += ((cramped ? 1 : MOOD_SPEED[m]) - c.speed) * SPEED_EASE;

      if (c.wx !== 0 && dist(c.wx - c.x, c.wy - c.y) < c.radius / 2) {
        // Arrived: it'll pick a new mood, and somewhere new to head for, next minute.
        c.wx = c.wy = 0;
        c.moodLeft = 0;
      }
      if (c.wx !== 0 && MOOD_PULL[m] > 0 && !cramped) steer(c, c.wx, c.wy, MOOD_PULL[m]);
      if (toward) steer(c, toward.x, toward.y, pull);
      if (pocket) {
        const out = geo.wayOut(c.x, c.y, 2)!;
        steer(c, out.x, out.y, POCKET_PULL);
      }
      if (lean) {
        let d = Math.atan2(lean.y - c.y, lean.x - c.x) - c.heading;
        d = Math.atan2(Math.sin(d), Math.cos(d));
        c.heading += clamp(d, -LEAN, LEAN);
      }

      const v = (c.regionId ? REGION_DRIFT : DRIFT) * t.speed * c.speed * speedup;
      // Feel the way ahead and bend away from a coast or border before reaching it.
      if (!toward && !pocket && !feelClear(c, c.heading, v, allowed)) {
        const left = feelClear(c, c.heading + FEEL_SPLAY, v, allowed);
        const right = feelClear(c, c.heading - FEEL_SPLAY, v, allowed);
        const side = left === right ? (c.turn >= 0 ? 1 : -1) : left ? 1 : -1;
        c.heading += side * AVOID_TURN;
        c.turn = side * Math.abs(c.turn);
        if (++c.avoid >= AVOID_GIVE_UP) {
          c.wx = c.wy = 0;
          c.moodLeft = 0;
          c.avoid = 0;
        }
      } else {
        c.avoid = 0;
      }

      const nx = c.x + Math.cos(c.heading) * v;
      const ny = c.y + Math.sin(c.heading) * v;
      if (allowed(nx, ny)) {
        c.x = nx;
        c.y = ny;
        c.atEdge = false;
      } else {
        const waitingForMain = region != null && toward != null;
        if (!waitingForMain) {
          c.heading = openHeading(c, v, rng, allowed);
          c.turn = 0;
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

    if (rng.next() < RETARGET_CHANCE * t.breathing) {
      const u = rng.next();
      c.radiusTarget = c.radiusMin + (t.hungry ? u * u : u) * (c.radiusMax - c.radiusMin);
    }
    const dr = c.radiusTarget - c.radius;
    c.radius += Math.sign(dr) * Math.min(Math.abs(dr), RADIUS_EASE);
    if (rng.next() < PULSE_CHANCE * t.pulses) {
      const nr = clamp(c.radius + (rng.next() < 0.5 ? t.pulse : -t.pulse), c.radiusMin, c.radiusMax);
      if (yours && Math.abs(nr - c.radius) > 0.5) {
        log.push(`Pulse! Your circle ${nr > c.radius ? 'grew' : 'shrank'} by ${javaRound(Math.abs(nr - c.radius))} tiles.`);
      }
      c.radius = nr;
      c.radiusTarget = nr;
      c.pulse = 45;
    }
    if (c.pulse > 0) c.pulse--;
  }

  /**
   * Picks the next mood (never the same twice running, weighted by the temperament), its length,
   * a loop direction or meander swing, and, once it has reached its last one or given up on it, a
   * waypoint: the best of a few random spots, the one furthest from where the circle has headed
   * lately. Mirrors startMood in NomadSim.java.
   */
  private startMood(c: CircleState, t: Temperament, rng: Rng, allowed: (x: number, y: number) => boolean, yours: boolean, log: string[]) {
    let total = 0;
    for (let k = 0; k < MOOD_NAMES.length; k++) if (k !== c.mood) total += t.weights[k];
    let pick = rng.next() * total;
    let mood = -1;
    for (let k = 0; k < MOOD_NAMES.length; k++) {
      if (k === c.mood || t.weights[k] <= 0) continue;
      mood = k;
      pick -= t.weights[k];
      if (pick < 0) break;
    }
    if (mood < 0) mood = c.mood;
    c.mood = mood;
    c.moodLeft = round((MOOD_MIN[mood] + rng.next() * (MOOD_MAX[mood] - MOOD_MIN[mood])) * t.duration);
    const side = rng.next() < 0.5 ? -1 : 1;
    c.bend = round(side * (mood === MEANDER ? MEANDER_SWING : LOOP_MIN + rng.next() * (LOOP_MAX - LOOP_MIN)) * 1e4) / 1e4;
    c.swing = 0;

    if (c.wx !== 0) {
      if (yours) log.push(`Your circle ${MOOD_LOG[mood]}.`);
      return;
    }
    const range = c.regionId ? REGION_WAY : MAIN_WAY;
    const island = !c.regionId && this.geo.pocketDepth(c.x, c.y) < 0;
    let bestScore = -1;
    let bx = 0;
    let by = 0;
    for (let k = 0; k < WAY_TRIES; k++) {
      const a = rng.next() * Math.PI * 2;
      const d = range[0] + rng.next() * (range[1] - range[0]);
      const x = round(c.x + Math.cos(a) * d);
      const y = round(c.y + Math.sin(a) * d);
      if (!allowed(x, y)) continue;
      if (!c.regionId && !island && this.geo.pocketDepth(x, y) !== 0) continue;
      let score = dist(x - c.x, y - c.y);
      if (c.trail) for (let i = 0; i + 1 < c.trail.length; i += 2) score = Math.min(score, dist(x - c.trail[i], y - c.trail[i + 1]));
      if (score > bestScore) {
        bestScore = score;
        bx = x;
        by = y;
      }
    }
    c.wx = bx;
    c.wy = by;
    if (bx !== 0) c.trail = [bx, by, ...(c.trail ?? []).slice(0, 2 * (TRAIL - 1))];
    if (yours) log.push(`Your circle ${MOOD_LOG[mood]}.`);
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

/** Moves a circle's whole range, and its radius with it. */
function shiftRange(c: CircleState, tiles: number) {
  c.expand += tiles;
  c.radiusMin += tiles;
  c.radiusMax += tiles;
  c.radius = clamp(c.radius + tiles, c.radiusMin, c.radiusMax);
  c.radiusTarget = clamp(c.radiusTarget + tiles, c.radiusMin, c.radiusMax);
}

/** Tiles between two circles' edges; 0 or less once they overlap. */
export const gap = (a: CircleState, b: CircleState) => dist(b.x - a.x, b.y - a.y) - a.radius - b.radius;

/** Roughly how long until a circle, keeping its heading and pace, drifts off a tile. */
function minutesLeft(c: CircleState, x: number, y: number): number {
  const v = c.voyage
    ? SAIL_SPEED
    : (c.regionId ? REGION_DRIFT : DRIFT) * temperamentOf(c).speed * Math.max(0.15, c.speed) * (c.tailwindLeft > 0 ? 2 : 1);
  const h = c.voyage ? Math.atan2(c.voyage.ty - c.y, c.voyage.tx - c.x) : c.heading;
  const px = x - c.x;
  const py = y - c.y;
  const b = px * Math.cos(h) + py * Math.sin(h);
  const disc = b * b - (px * px + py * py) + c.radius * c.radius;
  if (disc < 0) return c.anchorLeft;
  return c.anchorLeft + Math.trunc(Math.max(0, (b + Math.sqrt(disc)) / v));
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

/** Whether the drift about half an hour ahead along a heading, and halfway there, is still allowed. */
function feelClear(c: CircleState, heading: number, v: number, ok: (x: number, y: number) => boolean): boolean {
  const reach = clamp(v * FEEL_MINUTES, 6, 40);
  const cos = Math.cos(heading);
  const sin = Math.sin(heading);
  return ok(c.x + cos * reach, c.y + sin * reach) && ok(c.x + (cos * reach) / 2, c.y + (sin * reach) / 2);
}

/** Rounds halves up, exactly as NomadSim.java does. */
function round(v: number): number {
  return Math.floor(v + 0.5);
}

function clamp(v: number, lo: number, hi: number): number {
  return Math.max(lo, Math.min(hi, v));
}

/** Java's Math.round: halves round up. */
function javaRound(v: number): number {
  return Math.floor(v + 0.5);
}
