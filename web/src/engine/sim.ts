import { dist, Geography, LUMBRIDGE, Port, Tile, VOLCANO } from './geography';
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
export const LANTERN = 'Lantern';
export const CALM = 'Calm';
/** Legendary boons, as in NomadSim.java. */
export const CROWN = "Wanderer's Crown";
export const HELMSMAN = 'Helmsman';
export const GATHERING = 'Gathering';
export const COMMON = 'common';
export const RARE = 'rare';
export const LEGENDARY = 'legendary';
const COMMON_BOONS = [EXPAND, TAILWIND, ANCHOR, LANTERN, CALM];
const RARE_BOONS = [RUDDER, BRIDGE];
const LEGENDARY_BOONS = [CROWN, HELMSMAN, GATHERING];
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
/** Calm keeps forks and storms off Main for this long, and breaks one that's brewing or blowing. */
export const CALM_MINUTES = 180;
const LASTING = 30;
/** Ripening, pilgrim stones, the Crown, Gathering, comets, Homeward, Reunion and Castaway, as in NomadSim.java. */
export const RIPEN = 30;
const PILGRIM_CHANCE = 0.25;
/** Ports of call, as in NomadSim.java. */
const PORT_CALL_CHANCE = 0.25;
const PORT_CALL_RANGE = 500;
const PORT_PULL = 0.5;
const PORT_LOOKAHEAD = 8;
const SHORE_LEAVE = 180;
export const PILGRIM_MINUTES = 60;
export const CROWN_MINUTES = 60;
const CROWN_SPEED = 10;
export const GATHER_MINUTES = 60;
const GATHER_PULL = 0.12;
const COMET_EVERY_MIN = 900;
const COMET_EVERY_MAX = 1500;
export const COMET_WARN = 20;
export const COMET_MINUTES = 30;
export const COMET_RADIUS = 6;
const COMET_SPEED = 20;
const COMET_RETRY = 30;
export const HOMEWARD_COOLDOWN = 360;
const HOMEWARD_RADIUS = 12;
export const GHOST_MINUTES = 360;
const GHOST_DRIFT = DRIFT / 2;
const GHOST_APART = 10;
const CASTAWAY_CHANCE = 0.1;
/** A fork comes a random 30 minutes to 4 played hours after the last one parted, with a 10-minute warning. */
const FORK_EVERY_MIN = 30;
const FORK_EVERY_MAX = 240;
export const QUIET = 240;
export const FORK_WARN = 10;
/** The halves part at this many tiles a minute each, and settle it after this many minutes regardless. */
const FORK_SPEED = 0.6;
const FORK_GIVE_UP = 90;
/** Main only forks with room to split: at least this radius, and this many tries at an axis. */
const FORK_MIN_RADIUS = 14;
const FORK_TRIES = 12;
const FORK_REACH = 60;
/** A fork that can't start now (at sea, anchored or too small) tries again this much later. */
const FORK_RETRY = 30;
/** Storms over Main, as in NomadSim.java. */
export const STORM_EVERY_MIN = 360;
export const STORM_EVERY_MAX = 720;
export const STORM_WARN = 10;
export const STORM_MINUTES = 30;
const STORM_RETRY = 15;
export const GALE = 1;
export const SQUALL = 2;
const GALE_SPEEDUP = 3;
const SQUALL_SHRINK = 0.75;
const COMPASS = ['east', 'north-east', 'north', 'north-west', 'west', 'south-west', 'south', 'south-east'];

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
  /** Castaway: the port a storm at sea washes it up at instead, once it's this close to where it meant to go. */
  wreck?: string;
  wreckAt?: number;
}

/** A comet streaking across the map, as in Comet.java. */
export interface Comet {
  x: number;
  y: number;
  dx: number;
  dy: number;
  warn: number;
  left: number;
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
  /** Minutes left on the Wanderer's Crown, while this circle follows you. */
  crownLeft: number;
  /** The port Main has set course for, to board a boat when it gets there. */
  port?: string;
  /** Minutes after docking before Main boards a boat again, unless a Helmsman sends it. */
  shore: number;
}

/** A waystone holding one boon at a landmark inside a circle (by index). */
export interface Waystone {
  landmark: string;
  x: number;
  y: number;
  circle: number;
  boon: string;
  /** common, rare or legendary; missing in runs saved before tiers were kept. */
  tier?: string;
  /** Played minutes since it rose. */
  age: number;
  /** Rises just outside your circle and can be claimed from outside it. */
  pilgrim: boolean;
}

export interface SimState {
  seed: number;
  rngState: number;
  minutes: number;
  bound: number;
  outside: boolean;
  sailingUnlocked: boolean;
  /** The account's Sailing level, 0 if not known (then every dock counts as in reach). */
  sailingLevel?: number;
  /** Quests a dock needs that the account hasn't done. */
  questsMissing?: string[];
  circles: CircleState[];
  waystones: Waystone[];
  boons: string[];
  pending: string[];
  summons: number;
  bridgeLeft: number;
  /** Minutes left on Calm, and where a Lantern was lit (0, 0 when unlit). */
  calmLeft: number;
  lanternX: number;
  lanternY: number;
  /** Forks so far, the played minute the next is due (0 until scheduled), and the minute of the last big moment. */
  forks: number;
  nextFork: number;
  lastBig: number;
  /** Minutes of warning left before Main splits, and the heading Main's half takes (the other takes the opposite). */
  forkWarn: number;
  forkAxis: number;
  /** While forked: the other half, and the minutes since the split. */
  twin?: CircleState;
  forkAge: number;
  /** Storms so far, the next one's minute, its warning and time left, its kind, and Main's size before a Squall. */
  storms: number;
  nextStorm: number;
  stormWarn: number;
  stormLeft: number;
  stormKind: number;
  squallFrom: number;
  ripened: number;
  comet?: Comet;
  comets: number;
  nextComet: number;
  homewardLeft: number;
  ghost?: CircleState;
  ghostLeft: number;
  ghostApart: boolean;
  voyages: number;
  helmsTo?: string;
  gatherLeft: number;
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
      c.crownLeft ??= 0;
      c.shore ??= 0;
      if (c.voyage) c.voyage.wreckAt ??= 0;
    }
    // And runs saved before waystones.
    state.waystones ??= [];
    state.boons ??= [];
    state.pending ??= [];
    state.summons ??= 0;
    state.bridgeLeft ??= 0;
    // And runs saved before Calm and Lantern.
    state.calmLeft ??= 0;
    state.lanternX ??= 0;
    state.lanternY ??= 0;
    // And runs saved before forks.
    state.forks ??= 0;
    state.nextFork ??= 0;
    state.lastBig ??= 0;
    state.forkWarn ??= 0;
    state.forkAxis ??= 0;
    state.forkAge ??= 0;
    // And runs saved before storms.
    state.storms ??= 0;
    state.nextStorm ??= 0;
    state.stormWarn ??= 0;
    state.stormLeft ??= 0;
    state.stormKind ??= 0;
    state.squallFrom ??= 0;
    // And runs saved before comets, ghosts, Homeward and legendary boons.
    for (const w of state.waystones) {
      w.age ??= 0;
      w.pilgrim ??= false;
    }
    if (state.twin) {
      state.twin.crownLeft ??= 0;
      state.twin.shore ??= 0;
    }
    if (state.ghost) {
      state.ghost.crownLeft ??= 0;
      state.ghost.shore ??= 0;
    }
    state.ripened ??= 0;
    state.comets ??= 0;
    state.nextComet ??= 0;
    state.homewardLeft ??= 0;
    state.ghostLeft ??= 0;
    state.ghostApart ??= false;
    state.voyages ??= 0;
    state.gatherLeft ??= 0;
  }

  static start(geo: Geography, seed: number): NomadSim {
    const rng = new Rng(seed);
    const circles: CircleState[] = [];
    const base = (x: number, y: number, heading: number, start: number, min: number, max: number): CircleState => ({
      x, y, heading, radius: start, radiusTarget: start, radiusMin: min, radiusMax: max,
      pulse: 0, tiers: 0, atEdge: false, waiting: false, touchingYours: false,
      mood: 0, moodLeft: 0, turn: 0, bend: 0, swing: 0, speed: 1, wx: 0, wy: 0, avoid: 0,
      expand: 0, expandLeft: 0, tailwindLeft: 0, anchorLeft: 0, crownLeft: 0, shore: 0,
    });
    circles.push(base(LUMBRIDGE.x, LUMBRIDGE.y, rng.next() * Math.PI * 2, MAIN_START, MAIN_MIN, MAIN_MAX));
    for (const r of geo.regions) {
      circles.push({ regionId: r.id, ...base(r.home.x, r.home.y, rng.next() * Math.PI * 2, REGION_START, REGION_MIN, REGION_MAX) });
    }
    return new NomadSim(geo, {
      seed: seed | 0, rngState: rng.state, minutes: 0, bound: MAIN, outside: false, sailingUnlocked: false, sailingLevel: 0, questsMissing: [], circles,
      waystones: [], boons: [], pending: [], summons: 0, bridgeLeft: 0, calmLeft: 0, lanternX: 0, lanternY: 0,
      forks: 0, nextFork: 0, lastBig: 0, forkWarn: 0, forkAxis: 0, forkAge: 0,
      storms: 0, nextStorm: 0, stormWarn: 0, stormLeft: 0, stormKind: 0, squallFrom: 0,
      ripened: 0, comets: 0, nextComet: 0, homewardLeft: 0, ghostLeft: 0, ghostApart: false, voyages: 0, gatherLeft: 0,
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

  /** While Main is forked, the other half. */
  get twin(): CircleState | undefined {
    return this.state.twin;
  }

  /** Played minutes since the last fork, voyage or crossing. */
  quietMinutes(): number {
    return this.state.minutes - this.state.lastBig;
  }

  /** Whether a tile is inside Main, counting both halves while it's forked. */
  inMain(x: number, y: number): boolean {
    return contains(this.main, x, y) || (this.state.twin != null && contains(this.state.twin, x, y));
  }

  /** Whether a tile is inside your circle, counting both of Main's halves while it's forked. */
  inYours(x: number, y: number): boolean {
    return this.state.bound === MAIN ? this.inMain(x, y) : contains(this.bound, x, y);
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
    this.state.pending.push(tier === RARE || tier === LEGENDARY ? tier : COMMON);
    const log: string[] = [];
    this.placePending(log);
    return log.length ? log[0] : null;
  }

  /** Spends the boon at index. side is for Rudder: 1 turns left, -1 right; player is for Lantern, lit inside your circle. */
  useBoon(index: number, side: number, player: Tile | null = null): string | null {
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
      case LANTERN:
        if (player == null || !contains(c, player.x, player.y)) return null;
        s.lanternX = player.x;
        s.lanternY = player.y;
        line = 'Lantern: the next waystone to rise in your circle rises at the landmark nearest where you lit it.';
        break;
      case CALM:
        line = this.calm();
        break;
      case CROWN:
        if (c.voyage) return null;
        c.crownLeft = CROWN_MINUTES;
        c.anchorLeft = 0;
        c.pulse = 45;
        line = "Wanderer's Crown: for an hour your circle follows you wherever you walk.";
        break;
      case GATHERING:
        s.gatherLeft = GATHER_MINUTES;
        line = "Gathering: for an hour every kingdom's circle turns toward the Main circle.";
        break;
      default:
        return null;
    }
    s.boons.splice(index, 1);
    return line;
  }

  /** Helmsman: Main's next voyage goes to the port you pick. As in NomadSim.java. */
  useHelmsman(index: number, port: string): string | null {
    const s = this.state;
    if (index < 0 || index >= s.boons.length || s.boons[index] !== HELMSMAN) return null;
    if (!this.helmsmanPorts().includes(port)) return null;
    s.helmsTo = port;
    s.boons.splice(index, 1);
    const m = this.main;
    const near = m.voyage ? null : this.nearestPort(m, port, Number.MAX_VALUE);
    if (!near) return `Helmsman: the next time a port is inside the Main circle, it sets sail for ${port}.`;
    setCourse(m, near);
    return `Helmsman: the Main circle sets course for ${near.name}, and will sail from there for ${port}.`;
  }

  /** The ports a Helmsman can send Main to. */
  helmsmanPorts(): string[] {
    const out: string[] = [];
    for (const p of this.geo.ports) {
      for (const to of this.geo.routesFrom(p.name, this.dockLevel(), this.state.questsMissing ?? [])) if (!out.includes(to)) out.push(to);
    }
    return out;
  }

  private dockLevel(): number {
    const s = this.state;
    return !s.sailingUnlocked ? 0 : s.sailingLevel && s.sailingLevel > 0 ? s.sailingLevel : 99;
  }

  /** Homeward: a Home Teleport from inside Main carries Main with you. As in NomadSim.java. */
  homeward(from: Tile | null, to: Tile | null): string | null {
    const s = this.state;
    if (to == null || s.bound !== MAIN) return null;
    if (from != null && !this.inMain(from.x, from.y)) return null;
    if (s.homewardLeft > 0) return `Homeward is still gathering itself (${s.homewardLeft} more minutes), so the Main circle stayed behind.`;
    if (!this.geo.onLand(to.x, to.y)) return null;
    const m = this.main;
    const collapsed = s.twin || s.forkWarn > 0 ? ' The fork it was in collapsed.' : '';
    if (s.twin) {
      delete s.twin;
      s.forkAge = 0;
      s.forks++;
      s.nextFork = 0;
    }
    if (s.forkWarn > 0) {
      s.forkWarn = 0;
      s.nextFork = s.minutes + FORK_RETRY;
    }
    delete m.voyage;
    delete m.port;
    m.x = to.x;
    m.y = to.y;
    m.radius = clamp(HOMEWARD_RADIUS, m.radiusMin, m.radiusMax);
    m.radiusTarget = m.radius;
    m.moodLeft = 0;
    m.wx = m.wy = 0;
    m.turn = 0;
    m.pulse = 45;
    s.homewardLeft = HOMEWARD_COOLDOWN;
    s.outside = false;
    s.lastBig = s.minutes;
    return `Homeward! The Main circle followed your Home Teleport and stands around you now, shrunk to ${javaRound(m.radius)} tiles. It can do that again in 6 played hours.${collapsed}`;
  }

  /** Testing only: the next comet is announced at the next minute. */
  cometSoonForTesting() {
    if (!this.state.comet) this.state.nextComet = Math.max(1, this.state.minutes);
  }

  /** Calm: no fork or storm over Main for 3 hours; a fork's warning or a storm under way breaks off now. As in NomadSim.java. */
  private calm(): string {
    const s = this.state;
    s.calmLeft = CALM_MINUTES;
    let broke = '';
    if (s.forkWarn > 0) {
      s.forkWarn = 0;
      s.nextFork = s.minutes + FORK_RETRY;
      broke = " The fork that was coming won't.";
    }
    if (s.stormWarn > 0) {
      s.stormWarn = 0;
      s.stormKind = 0;
      s.nextStorm = s.minutes + STORM_RETRY;
      broke = ' The storm that was brewing holds off.';
    } else if (s.stormLeft > 0) {
      const m = this.main;
      if (s.stormKind === SQUALL) m.radiusTarget = clamp(s.squallFrom, m.radiusMin, m.radiusMax);
      s.stormLeft = 0;
      s.stormKind = 0;
      s.storms++;
      s.nextStorm = 0;
      broke = ' The storm breaks off.';
    }
    return 'Calm: no fork or storm comes over the Main circle for 3 hours.' + broke;
  }

  /** Roughly how many played minutes before a waystone's circle drifts off it. An estimate. */
  minutesLeft(w: Waystone): number {
    if (w.tier === LEGENDARY) return -1;
    if (w.pilgrim) return Math.max(0, PILGRIM_MINUTES - (w.age ?? 0));
    return minutesLeft(this.circles[w.circle], w.x, w.y);
  }

  crossTo(index: number, player: Tile | null): string | null {
    if (!this.canCross(index, player)) return null;
    this.state.bound = index;
    this.state.outside = false;
    this.state.lastBig = this.state.minutes;
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

    // An open Bridge lets you walk between circles without it counting as outside, and either
    // half of a forked Main counts as yours.
    const out = player != null && !this.inYours(player.x, player.y) && s.bridgeLeft <= 0;
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
      const gather = s.gatherLeft > 0 && !!c.regionId;
      const lean = yours && !out && player != null && dist(player.x - c.x, player.y - c.y) >= c.radius - LEAN_BAND ? player : null;
      const speedup = (yours && out ? OUTSIDE_SPEEDUP : 1) * (c.tailwindLeft > 0 ? 2 : 1)
        * (j === MAIN && s.stormLeft > 0 && s.stormKind === GALE ? GALE_SPEEDUP : 1);
      if (j === MAIN && s.twin) part(c, s.twin, this.geo);
      else if (yours && c.crownLeft > 0 && player != null && !c.voyage) this.follow(c, player);
      else this.move(c, rng, speedup, yours, pulled || gather ? main : null, pulled ? STRANDED_PULL : gather ? GATHER_PULL : 0, lean, log);
    });
    s.minutes++;
    this.tickBoons(log);
    this.fork(player, log);
    this.storm(log);
    this.ghost(log);
    this.comet(player, log);

    if (s.minutes % 5 === 0) {
      const yours = this.bound;
      this.circles.forEach((c, j) => {
        if (!this.linked(j)) return;
        const touching = touches(yours, c);
        if (touching && !c.touchingYours) {
          log.push(`Crossing open: your circle touches ${this.name(c)}.`);
          s.lastBig = s.minutes;
        }
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
    this.circles.forEach((c, j) => {
      if (c.crownLeft > 0 && --c.crownLeft === 0 && j === s.bound) log.push("The Wanderer's Crown has dimmed: your circle wanders on its own again.");
    });
    if (s.bridgeLeft > 0 && --s.bridgeLeft === 0) log.push('The Bridge closed.');
    if (s.calmLeft > 0 && --s.calmLeft === 0) log.push('The Calm has lifted: forks and storms can come again.');
    if (s.gatherLeft > 0 && --s.gatherLeft === 0) log.push("The Gathering is over: the kingdoms' circles go their own ways.");
    if (s.homewardLeft > 0 && --s.homewardLeft === 0) log.push('Homeward is ready again: a Home Teleport from inside Main will carry it with you.');
  }

  /** The Wanderer's Crown: the circle's middle walks toward you, up to 10 tiles a minute. */
  private follow(c: CircleState, player: Tile) {
    const d = dist(player.x - c.x, player.y - c.y);
    if (d < 1) return;
    const step = Math.min(CROWN_SPEED, d);
    const nx = c.x + ((player.x - c.x) / d) * step;
    const ny = c.y + ((player.y - c.y) / d) * step;
    const region = c.regionId ? this.geo.region(c.regionId)! : null;
    const ok = region ? this.geo.inRegion(region, nx, ny) : this.geo.onLand(nx, ny);
    if (!ok) return;
    c.x = nx;
    c.y = ny;
    c.heading = Math.atan2(player.y - c.y, player.x - c.x);
  }

  /** Reunion: the lost half drifts on and can merge back into Main. As in NomadSim.java. */
  private ghost(log: string[]) {
    const s = this.state;
    const g = s.ghost;
    if (!g) return;
    if (--s.ghostLeft <= 0) {
      delete s.ghost;
      s.ghostApart = false;
      log.push('The lost half of the last fork has faded for good.');
      return;
    }
    for (const turn of [0, 0.5, -0.5, 1, -1, 2, -2, Math.PI]) {
      const nx = g.x + Math.cos(g.heading + turn) * GHOST_DRIFT;
      const ny = g.y + Math.sin(g.heading + turn) * GHOST_DRIFT;
      if (!this.geo.onLand(nx, ny)) continue;
      g.x = nx;
      g.y = ny;
      g.heading += turn;
      break;
    }
    const m = this.main;
    if (s.twin || m.voyage) return;
    const gp = gap(m, g);
    if (gp >= GHOST_APART) s.ghostApart = true;
    if (!s.ghostApart || gp > 0) return;
    const r = Math.min(m.radiusMax, m.radius + g.radius / 2);
    log.push(`Reunion! The lost half drifted back into the Main circle and merged with it. Main grows to ${javaRound(r)} tiles.`);
    m.radius = r;
    m.radiusTarget = r;
    m.pulse = 45;
    delete s.ghost;
    s.ghostApart = false;
    s.ghostLeft = 0;
    s.lastBig = s.minutes;
  }

  /** Schedules, announces and flies comets. As in NomadSim.java. */
  private comet(player: Tile | null, log: string[]) {
    const s = this.state;
    const k = s.comet;
    if (k) {
      if (k.warn > 0) {
        if (--k.warn === 0) log.push('The comet is streaking across the sky now. Stand in its path!');
        return;
      }
      const ox = k.x;
      const oy = k.y;
      k.x += k.dx;
      k.y += k.dy;
      k.left--;
      if (player != null && segmentDist(player.x, player.y, ox, oy, k.x, k.y) <= COMET_RADIUS) {
        delete s.comet;
        s.comets++;
        s.nextComet = 0;
        s.lastBig = s.minutes;
        s.pending.push(RARE);
        log.push('You caught the comet! A rare waystone answers its call.');
        return;
      }
      if (k.left > 0) return;
      delete s.comet;
      s.comets++;
      s.nextComet = 0;
      log.push('The comet passed over and was gone.');
      return;
    }
    if (s.nextComet <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.comets + 1, 0x5bd1e995 | 0));
      s.nextComet = s.minutes + COMET_EVERY_MIN + Math.trunc(dice.next() * (COMET_EVERY_MAX - COMET_EVERY_MIN));
    }
    if (s.minutes < s.nextComet) return;
    const m = this.main;
    if (m.voyage) {
      s.nextComet = s.minutes + COMET_RETRY;
      return;
    }
    const dice = new Rng(s.seed ^ Math.imul(s.comets + 1, 0x1b873593));
    const a = round(dice.next() * Math.PI * 2 * 1e4) / 1e4;
    const half = (COMET_SPEED * COMET_MINUTES) / 2.0;
    s.comet = {
      x: m.x - Math.cos(a) * half,
      y: m.y - Math.sin(a) * half,
      dx: Math.cos(a) * COMET_SPEED,
      dy: Math.sin(a) * COMET_SPEED,
      warn: COMET_WARN,
      left: COMET_MINUTES,
    };
    log.push(`A comet is coming! In ${COMET_WARN} minutes it streaks ${compass(a)} across the map, straight through the Main circle. Stand in its path to catch it.`);
  }

  private waystones(player: Tile | null, log: string[]) {
    const s = this.state;
    if (player != null && s.boons.length < BOONS_HELD) {
      for (let i = 0; i < s.waystones.length; i++) {
        const w = s.waystones[i];
        const anywhere = w.pilgrim || w.tier === LEGENDARY;
        if (w.circle !== s.bound || (!anywhere && !contains(this.bound, player.x, player.y))) continue;
        if (dist(player.x - w.x, player.y - w.y) > CLAIM_REACH) continue;
        s.waystones.splice(i, 1);
        s.boons.push(w.boon);
        log.push(`You claimed the waystone at ${w.landmark}: ${w.boon}. Spend it from the sidebar.`);
        break;
      }
    }
    for (let i = s.waystones.length - 1; i >= 0; i--) {
      const w = s.waystones[i];
      if (w.tier === LEGENDARY) continue;
      if (w.pilgrim) {
        if (w.age < PILGRIM_MINUTES) continue;
        s.waystones.splice(i, 1);
        log.push(`The pilgrim stone at ${w.landmark} sank back into the ground.`);
        continue;
      }
      if (contains(this.circles[w.circle], w.x, w.y)) continue;
      s.waystones.splice(i, 1);
      log.push(`The waystone at ${w.landmark} faded: its circle drifted off it.`);
    }
    for (const w of s.waystones) {
      w.age++;
      if (w.pilgrim || w.tier !== COMMON || w.age !== RIPEN) continue;
      const dice = new Rng(s.seed ^ Math.imul(s.ripened + 1, 0x68e31da4));
      s.ripened++;
      w.tier = RARE;
      w.boon = RARE_BOONS[Math.floor(dice.next() * RARE_BOONS.length)];
      log.push(`The waystone at ${w.landmark} ripened: it holds ${w.boon} (rare) now.`);
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
    // A legendary stone rises only in your own circle, at its landmark furthest from the middle.
    if (tier === LEGENDARY) reach.splice(0, reach.length, s.bound);
    const taken = (l: Landmark) => s.waystones.some((w) => w.x === l.x && w.y === l.y);
    let options: [number, number][] = [];
    for (const j of reach) {
      const c = this.circles[j];
      LANDMARKS.forEach((l, k) => {
        if (!contains(c, l.x, l.y) || taken(l)) return;
        options.push([k, j]);
      });
    }
    // Pilgrim spots: landmarks just outside your circle, no further past its edge than its radius.
    const pilgrims: [number, number][] = [];
    if (tier === RARE) {
      const c = this.bound;
      LANDMARKS.forEach((l, k) => {
        const d = dist(l.x - c.x, l.y - c.y);
        if (d <= c.radius + CLAIM_REACH || d > c.radius * 2 || taken(l)) return;
        pilgrims.push([k, s.bound]);
      });
    }
    if (!options.length && !pilgrims.length) return null;
    if (tier === LEGENDARY) return this.placeLegendary(options);
    const rng = new Rng(s.seed ^ Math.imul(s.summons + 1, 0x9e3779b9 | 0));
    if (pilgrims.length && (!options.length || rng.next() < PILGRIM_CHANCE)) {
      s.summons++;
      const [k, j] = pilgrims[Math.floor(rng.next() * pilgrims.length)];
      const l = LANDMARKS[k];
      const w = this.stone(l, j, RARE, RARE_BOONS[Math.floor(rng.next() * RARE_BOONS.length)]);
      w.pilgrim = true;
      return `A pilgrim stone rose at ${l.name}, just outside your circle, holding ${w.boon} (rare). Step out to claim it within the hour.`;
    }
    const all = options;
    const lasting = options.filter(([k, j]) => minutesLeft(this.circles[j], LANDMARKS[k].x, LANDMARKS[k].y) >= LASTING);
    if (lasting.length) options = lasting;
    // A lit Lantern takes the lasting landmark nearest it in your circle, else the nearest at all.
    const lit = s.lanternX !== 0 || s.lanternY !== 0 ? this.nearestLantern(options) ?? this.nearestLantern(all) : null;
    s.summons++;
    let [k, j] = options[Math.floor(rng.next() * options.length)];
    if (lit) {
      [k, j] = lit;
      s.lanternX = 0;
      s.lanternY = 0;
    }
    const pool = tier === RARE ? RARE_BOONS : COMMON_BOONS;
    const l: Landmark = LANDMARKS[k];
    const w = this.stone(l, j, tier === RARE ? RARE : COMMON, pool[Math.floor(rng.next() * pool.length)]);
    return `A waystone rose at ${l.name}${j === s.bound ? ' in your circle' : ` in ${this.name(this.circles[j])}`}, holding ${w.boon} (${tier === RARE ? 'rare' : 'common'}).`;
  }

  /** A legendary stone: the landmark in your circle furthest from its middle. It never fades. */
  private placeLegendary(options: [number, number][]): string | null {
    if (!options.length) return null;
    const s = this.state;
    const c = this.bound;
    let best = options[0];
    let bestD = -1;
    for (const o of options) {
      const l = LANDMARKS[o[0]];
      const d = dist(l.x - c.x, l.y - c.y);
      if (d > bestD) {
        best = o;
        bestD = d;
      }
    }
    const rng = new Rng(s.seed ^ Math.imul(s.summons + 1, 0x9e3779b9 | 0));
    s.summons++;
    const l = LANDMARKS[best[0]];
    const w = this.stone(l, best[1], LEGENDARY, LEGENDARY_BOONS[Math.floor(rng.next() * LEGENDARY_BOONS.length)]);
    return `A legendary waystone rose at ${l.name}, holding the ${w.boon}. It never fades.`;
  }

  private stone(l: Landmark, circle: number, tier: string, boon: string): Waystone {
    const w: Waystone = { landmark: l.name, x: l.x, y: l.y, circle, boon, tier, age: 0, pilgrim: false };
    this.state.waystones.push(w);
    return w;
  }

  /** The option in your circle nearest the lit Lantern, or null if none is in your circle. */
  private nearestLantern(options: [number, number][]): [number, number] | null {
    const s = this.state;
    let best: [number, number] | null = null;
    let bestD = 0;
    for (const o of options) {
      if (o[1] !== s.bound) continue;
      const l = LANDMARKS[o[0]];
      const d = dist(l.x - s.lanternX, l.y - s.lanternY);
      if (best == null || d < bestD) {
        best = o;
        bestD = d;
      }
    }
    return best;
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
      if (v.wreck != null && dist(v.tx - c.x, v.ty - c.y) <= (v.wreckAt ?? 0)) {
        const w = this.geo.port(v.wreck)!;
        log.push(`Castaway! A storm at sea blew the Main circle off course. It will wash up at ${w.name} instead of ${v.to}.`);
        v.to = w.name;
        v.tx = w.tile.x;
        v.ty = w.tile.y;
        delete v.wreck;
        v.wreckAt = 0;
      }
      const d = dist(v.tx - c.x, v.ty - c.y);
      if (d <= SAIL_SPEED) {
        c.x = v.tx;
        c.y = v.ty;
        if (yours) log.push(`Your circle docked at ${v.to}.`);
        delete c.voyage;
        c.shore = SHORE_LEAVE;
        c.heading = rng.next() * Math.PI * 2;
        c.turn = 0;
        c.moodLeft = 0;
      } else {
        c.x += ((v.tx - c.x) / d) * SAIL_SPEED;
        c.y += ((v.ty - c.y) / d) * SAIL_SPEED;
      }
    } else {
      if (c.shore > 0) c.shore--;
      if (c.moodLeft <= 0) this.startMood(c, t, rng, allowed, yours, log);
      c.moodLeft--;
      const m = c.mood;
      // In a peninsula or cove, Main forgets its mood and keeps a steady pace until it's out.
      // Bound for a port, it goes into the cove the port sits in, and holds its course.
      const depth = region || c.port != null ? 0 : geo.pocketDepth(c.x, c.y);
      const cramped = depth >= CRAMPED_DEPTH;
      const pocket = depth >= POCKET_DEPTH;
      const steady = cramped || c.port != null;

      const target = steady ? 0 : m === LOOP ? c.bend : m === MEANDER ? c.bend * Math.sin(c.swing * MEANDER_STEP) : 0;
      c.swing++;
      c.turn += (target - c.turn) * TURN_EASE + (rng.next() - 0.5) * 2 * MOOD_WOBBLE[m];
      c.turn = clamp(c.turn, -TURN_MAX, TURN_MAX);
      c.heading += c.turn;
      c.speed += ((steady ? 1 : MOOD_SPEED[m]) - c.speed) * SPEED_EASE;

      if (c.wx !== 0 && dist(c.wx - c.x, c.wy - c.y) < c.radius / 2) {
        // Arrived: it'll pick a new mood, and somewhere new to head for, next minute.
        c.wx = c.wy = 0;
        c.moodLeft = 0;
        delete c.port;
      }
      if (c.port != null) {
        // Bound for a port: it follows the walk there over land, round coasts and into coves.
        const way = geo.wayTo(geo.port(c.port)!, c.x, c.y, PORT_LOOKAHEAD);
        if (way) steer(c, way.x, way.y, PORT_PULL);
        else {
          delete c.port;
          c.wx = c.wy = 0;
        }
      } else if (c.wx !== 0 && MOOD_PULL[m] > 0 && !cramped) steer(c, c.wx, c.wy, MOOD_PULL[m]);
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
      if (!toward && !pocket && c.port == null && !feelClear(c, c.heading, v, allowed)) {
        const left = feelClear(c, c.heading + FEEL_SPLAY, v, allowed);
        const right = feelClear(c, c.heading - FEEL_SPLAY, v, allowed);
        const side = left === right ? (c.turn >= 0 ? 1 : -1) : left ? 1 : -1;
        c.heading += side * AVOID_TURN;
        c.turn = side * Math.abs(c.turn);
        if (++c.avoid >= AVOID_GIVE_UP) {
          c.wx = c.wy = 0;
          c.moodLeft = 0;
          c.avoid = 0;
          delete c.port;
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
        } else if (yours && !c.atEdge && c.port == null) {
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
    const s = this.state;
    if (!c.regionId && (s.helmsTo != null || (c.shore <= 0 && this.geo.pocketDepth(c.x, c.y) === 0))) {
      // A port of call, from open land only and on its own dice. A Helmsman always heads for the nearest port.
      const call = new Rng(s.seed ^ Math.imul(s.minutes + 1, 0x2f0b3a49));
      const p = s.helmsTo != null ? this.nearestPort(c, s.helmsTo, Number.MAX_VALUE)
        : call.next() < PORT_CALL_CHANCE ? this.nearestPort(c, null, PORT_CALL_RANGE) : null;
      if (p) {
        setCourse(c, p);
        if (yours) log.push(`Your circle ${MOOD_LOG[mood]}, setting course for ${p.name} to board a boat.`);
        return;
      }
    }
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
    let port = this.geo.ports.find((p) => contains(c, p.tile.x, p.tile.y));
    const s = this.state;
    // At its port of call, it boards there.
    const call = c.port == null ? undefined : this.geo.port(c.port);
    const arrived = call != null && contains(c, call.tile.x, call.tile.y);
    if (arrived) port = call;
    if (port && s.helmsTo != null && port.name !== s.helmsTo) {
      this.board(c, port, s.helmsTo, log);
      log.push('The Helmsman holds the course: no storm will blow it off.');
      delete s.helmsTo;
      return;
    }
    if (!port || (!arrived && (c.shore > 0 || rng.next() >= BOARD_CHANCE))) return;
    const level = this.dockLevel();
    const missing = s.questsMissing ?? [];
    const routes = this.geo.routesFrom(port.name, level, missing);
    if (routes.length === 0) {
      if (arrived) delete c.port;
      return;
    }
    const to = routes[Math.floor(rng.next() * routes.length)];
    this.board(c, port, to, log);
    const cast = new Rng(s.seed ^ Math.imul(s.voyages, 0x7feb352d));
    if (cast.next() >= CASTAWAY_CHANCE) return;
    const wrecks = this.geo.ports
      .filter((p) => p.name !== port.name && p.name !== to && this.geo.routesFrom(p.name, level, missing).length > 0)
      .map((p) => p.name);
    if (!wrecks.length) return;
    c.voyage!.wreck = wrecks[Math.floor(cast.next() * wrecks.length)];
    c.voyage!.wreckAt = dist(c.voyage!.tx - c.x, c.voyage!.ty - c.y) / 2;
  }

  /**
   * The nearest port, in a straight line, that Main can walk to and that isn't already inside it,
   * within range tiles. With helm, any such port but that one; without, only ports with a route
   * Main can sail. As in NomadSim.java.
   */
  private nearestPort(c: CircleState, helm: string | null, range: number): Port | null {
    const missing = this.state.questsMissing ?? [];
    let best: Port | null = null;
    let bestD = range;
    for (const p of this.geo.ports) {
      if (p.name === helm || contains(c, p.tile.x, p.tile.y)) continue;
      if (helm == null && this.geo.routesFrom(p.name, this.dockLevel(), missing).length === 0) continue;
      const d = dist(p.tile.x - c.x, p.tile.y - c.y);
      if (d > bestD || this.geo.harbourSteps(p, c.x, c.y) < 0) continue;
      best = p;
      bestD = d;
    }
    return best;
  }

  private board(c: CircleState, port: { name: string; tile: Tile }, to: string, log: string[]) {
    const dest = this.geo.port(to)!;
    c.x = port.tile.x;
    c.y = port.tile.y;
    c.voyage = { to, tx: dest.tile.x, ty: dest.tile.y, wreckAt: 0 };
    this.state.voyages++;
    delete c.port;
    c.wx = c.wy = 0;
    log.push(`The Main circle boarded a boat at ${port.name}, bound for ${to}.`);
    this.state.lastBig = this.state.minutes;
  }

  /**
   * Forks: schedules the next one, brings it forward after 4 quiet hours, warns 10 minutes
   * ahead, splits Main in two, and once the halves have parted keeps the one you stand in.
   */
  /** Brews, starts and ends storms over Main, as in NomadSim.java. */
  private storm(log: string[]) {
    const s = this.state;
    const m = this.main;
    if (s.stormLeft > 0) {
      if (--s.stormLeft > 0) return;
      if (s.stormKind === SQUALL) m.radiusTarget = clamp(s.squallFrom, m.radiusMin, m.radiusMax);
      log.push(s.stormKind === GALE ? 'The gale has blown itself out.' : 'The squall has passed. The Main circle eases back to its size.');
      s.stormKind = 0;
      s.storms++;
      s.nextStorm = 0;
      return;
    }
    if (s.stormWarn > 0) {
      if (--s.stormWarn > 0) return;
      if (s.twin || m.voyage) {
        log.push('The storm blew past the Main circle.');
        s.stormKind = 0;
        s.nextStorm = s.minutes + STORM_RETRY;
        return;
      }
      s.stormLeft = STORM_MINUTES;
      s.lastBig = s.minutes;
      if (s.stormKind === SQUALL) {
        s.squallFrom = m.radius;
        m.radius = Math.max(m.radiusMin, m.radius * SQUALL_SHRINK);
        m.radiusTarget = m.radius;
        log.push(`Squall! The Main circle shrinks to ${javaRound(m.radius)} tiles for ${STORM_MINUTES} minutes.`);
      } else {
        log.push(`Gale! The Main circle drifts three times as fast for ${STORM_MINUTES} minutes.`);
      }
      return;
    }
    if (s.nextStorm <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.storms + 1, 0x165667b1));
      s.nextStorm = s.minutes + STORM_EVERY_MIN + Math.trunc(dice.next() * (STORM_EVERY_MAX - STORM_EVERY_MIN));
    }
    if (s.minutes < s.nextStorm) return;
    if (s.twin || m.voyage || s.forkWarn > 0 || s.calmLeft > 0) {
      s.nextStorm = s.minutes + STORM_RETRY;
      return;
    }
    const dice = new Rng(s.seed ^ Math.imul(s.storms + 1, 0x27d4eb2d));
    s.stormKind = dice.next() < 0.5 ? GALE : SQUALL;
    s.stormWarn = STORM_WARN;
    log.push(`A storm is brewing over the Main circle: a ${s.stormKind === GALE ? 'gale' : 'squall'} in ${STORM_WARN} minutes.`);
  }

  private fork(player: Tile | null, log: string[]) {
    const s = this.state;
    const m = this.main;
    if (s.twin) {
      s.forkAge++;
      if (gap(m, s.twin) <= 0 && s.forkAge < FORK_GIVE_UP) return;
      this.settle(player, log);
      return;
    }
    if (s.forkWarn > 0) {
      if (m.voyage) {
        // Main set sail during the warning: the fork waits for dry land.
        s.forkWarn = 0;
        s.nextFork = s.minutes + FORK_RETRY;
        return;
      }
      if (--s.forkWarn > 0) return;
      const twin = structuredClone(m);
      m.heading = s.forkAxis;
      twin.heading = s.forkAxis + Math.PI;
      m.turn = 0;
      twin.turn = 0;
      s.twin = twin;
      s.forkAge = 0;
      s.lastBig = s.minutes;
      log.push(`The Main circle forked! One half heads ${compass(s.forkAxis)}, the other ${compass(s.forkAxis + Math.PI)}. Stand in the half you want when they part.`);
      return;
    }
    if (s.nextFork <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.forks + 1, 0x85ebca6b | 0));
      s.nextFork = s.minutes + FORK_EVERY_MIN + Math.trunc(dice.next() * (FORK_EVERY_MAX - FORK_EVERY_MIN));
    }
    // Pacing: after 4 quiet hours the next fork comes now.
    if (s.minutes - s.lastBig >= QUIET) s.nextFork = Math.min(s.nextFork, s.minutes);
    if (s.minutes < s.nextFork) return;
    if (m.voyage || m.anchorLeft > 0 || m.radius < FORK_MIN_RADIUS || s.stormWarn > 0 || s.stormLeft > 0 || s.calmLeft > 0) {
      s.nextFork = s.minutes + FORK_RETRY;
      return;
    }
    // The axis that leaves both halves the most land to part across.
    const dice = new Rng(s.seed ^ Math.imul(s.forks + 1, 0xc2b2ae35 | 0));
    let best = -1;
    for (let k = 0; k < FORK_TRIES; k++) {
      const a = round(dice.next() * Math.PI * 2 * 1e4) / 1e4;
      const score = Math.min(reach(m, a, this.geo), reach(m, a + Math.PI, this.geo));
      if (score > best) {
        best = score;
        s.forkAxis = a;
      }
    }
    s.forkWarn = FORK_WARN;
    log.push(`The Main circle is about to fork. In ${FORK_WARN} minutes it splits in two, one half heading ${compass(s.forkAxis)} and the other ${compass(s.forkAxis + Math.PI)}. The half you're standing in when they part lives on.`);
  }

  /** The halves have parted: the one you stand in lives on as Main, or the seed picks. */
  private settle(player: Tile | null, log: string[]) {
    const s = this.state;
    const m = this.main;
    const twin = s.twin!;
    const inA = s.bound === MAIN && player != null && contains(m, player.x, player.y);
    const inB = s.bound === MAIN && player != null && contains(twin, player.x, player.y);
    const chose = inA || inB;
    let keepTwin: boolean;
    if (inA && inB) keepTwin = dist(player!.x - twin.x, player!.y - twin.y) < dist(player!.x - m.x, player!.y - m.y);
    else if (chose) keepTwin = inB;
    else {
      // The seed never keeps the half deeper in a peninsula, so forks can't keep throwing
      // Main back into one; otherwise it tosses a coin.
      const depth = Math.max(0, this.geo.pocketDepth(m.x, m.y));
      const twinDepth = Math.max(0, this.geo.pocketDepth(twin.x, twin.y));
      keepTwin = depth !== twinDepth ? twinDepth < depth : new Rng(s.seed ^ Math.imul(s.forks + 1, 0x27d4eb2f | 0)).next() < 0.5;
    }
    const kept = keepTwin ? twin : m;
    // Reunion: the lost half drifts on as a ghost for a while.
    const lost = structuredClone(keepTwin ? m : twin);
    delete lost.voyage;
    lost.crownLeft = 0;
    delete lost.port;
    s.ghost = lost;
    s.ghostLeft = GHOST_MINUTES;
    s.ghostApart = false;
    kept.moodLeft = 0;
    kept.wx = kept.wy = 0;
    kept.turn = 0;
    kept.pulse = 45;
    s.circles[MAIN] = kept;
    const way = compass(kept.heading);
    delete s.twin;
    s.forkAge = 0;
    s.forks++;
    s.nextFork = 0;
    s.lastBig = s.minutes;
    log.push(chose
      ? `The halves parted. You stood in the ${way} half, so it lives on as the Main circle; the other faded.`
      : `The halves parted. The seed chose the ${way} half, which lives on as the Main circle; the other faded.`);
  }
}

/** A compass word for a heading, such as north-west. */
export function compass(heading: number): string {
  const a = Math.atan2(Math.sin(heading), Math.cos(heading));
  const k = Math.floor(a / (Math.PI / 4) + 0.5);
  return COMPASS[((k % 8) + 8) % 8];
}

/** Distance from (px, py) to the segment from (ax, ay) to (bx, by). */
export function segmentDist(px: number, py: number, ax: number, ay: number, bx: number, by: number): number {
  const vx = bx - ax;
  const vy = by - ay;
  const len2 = vx * vx + vy * vy;
  const t = len2 === 0 ? 0 : clamp(((px - ax) * vx + (py - ay) * vy) / len2, 0, 1);
  return dist(px - (ax + t * vx), py - (ay + t * vy));
}

/** How many tiles of land lie along a heading from Main's centre, up to 60. */
function reach(c: CircleState, heading: number, geo: Geography): number {
  const cos = Math.cos(heading);
  const sin = Math.sin(heading);
  let d = 1;
  while (d <= FORK_REACH && geo.onLand(c.x + cos * d, c.y + sin * d)) d++;
  return d - 1;
}

/** Moves the two halves of a forked Main apart, each along its own heading, over land. */
function part(a: CircleState, b: CircleState, geo: Geography) {
  b.radius = a.radius;
  b.radiusTarget = a.radius;
  for (const h of [a, b]) {
    for (const turn of [0, 0.5, -0.5, 1, -1]) {
      const nx = h.x + Math.cos(h.heading + turn) * FORK_SPEED;
      const ny = h.y + Math.sin(h.heading + turn) * FORK_SPEED;
      if (!geo.onLand(nx, ny)) continue;
      h.x = nx;
      h.y = ny;
      break;
    }
    if (h.pulse > 0) h.pulse--;
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

/** Sets Main's course for a port: it heads there and boards a boat once it's inside. */
function setCourse(c: CircleState, p: Port) {
  c.port = p.name;
  c.wx = p.tile.x;
  c.wy = p.tile.y;
  c.avoid = 0;
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
