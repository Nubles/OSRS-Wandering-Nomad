import { CARAVAN, dist, Geography, LUMBRIDGE, Port, Tile, TOWNS, VOLCANO } from './geography';
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
/** Wave 3 boons, as in NomadSim.java. */
export const FORESIGHT = 'Foresight';
export const WARD = 'Ward';
export const WAKE = 'Wake';
export const LEAP = 'Leap';
export const LODESTONE = 'Lodestone';
export const KEYSTONE = 'Keystone';
export const COMMON = 'common';
export const RARE = 'rare';
export const LEGENDARY = 'legendary';
const COMMON_BOONS = [EXPAND, TAILWIND, ANCHOR, LANTERN, CALM, FORESIGHT, WARD];
const RARE_BOONS = [RUDDER, BRIDGE, WAKE, LEAP, LODESTONE, KEYSTONE];
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

// World events (the Idea Atlas's second wave), as in NomadSim.java. Each has its own dice.
const STRIKE_EVERY = 5;
const STRIKES = 3;
export const STRIKE_MINUTES = 2;
export const STRIKE = 'the lightning strike';
const SURGE_REACH = 40;
const STAMPEDE_EVERY_MIN = 1200;
const STAMPEDE_EVERY_MAX = 1680;
export const STAMPEDE_WARN = 5;
export const STAMPEDE_MINUTES = 10;
const STAMPEDE_SPEEDUP = 4;
const AURORA_EVERY_MIN = 720;
const AURORA_EVERY_MAX = 1440;
export const AURORA_MINUTES = 60;
export const GROW = 1;
export const RUSH = 2;
export const SEEK = 3;
export const STILL = 4;
export const GIFT = 5;
const RUSH_SPEEDUP = 4;
const ECLIPSE_REACH = 5;
export const ECLIPSE_MINUTES = 60;
const STRAY_SPEED = 3;
const RIFT_EVERY_MIN = 1800;
const RIFT_EVERY_MAX = 3600;
export const RIFT_MINUTES = 15;
const RIFT_FAR = 300;
const RIFT_RETRY = 30;
export const LANE_MINUTES = 60;
export const LANE_HALF = 6;
const FLOTSAM_EVERY_MIN = 120;
const FLOTSAM_EVERY_MAX = 360;
export const FLOTSAM_MINUTES = 60;
export const FLOTSAM = 'the flotsam';
export const EXODUS_MINUTES = 600;
const EXODUS_MOOD = 360;
const ORBIT_CHANCE = 0.5;
export const ORBIT_MINUTES = 75;
const ORBIT_NEAR = 8;
const ORBIT_FAR = 24;
const ORBIT_SPEED = 4;
const ORBIT_EASE = 0.2;
const ORBIT_STEER = 0.3;
const FLING_WAY = 150;
export const GRAVITY_MINUTES = 20;
const GRAVITY_PULL = 0.05;
export const HOMING_MINUTES = 720;
const HOMING_PULL = 0.02;
export const KISS_MINUTES = 20;
const KISS_RANGE = 200;
const CARAVAN_SPEED = 1.5;
const CARAVAN_REST = 20;
const CARAVAN_LOOKAHEAD = 8;
const CARAVAN_ARRIVE = 4;
const CARAVAN_CHOICES = 3;
export const CARAVAN_RADIUS = 10;
export const FORESIGHT_LOOK = 720;
export const WARD_MINUTES = 5;
export const WAKE_MINUTES = 60;
export const WAKE_TRAIL = 15;
export const LEAP_TILES = 40;
export const LODE_MINUTES = 180;
const LODE_PULL = 0.12;
export const LODE_REACH = 300;
export const KEYSTONE_MINUTES = 60;
const SHATTER_ODDS = 0.5;
const KINGDOM_FAVOUR = 0.5;
const WANDER_CHANCE = 0.3;
export const WANDER_EVERY = 10;
export const WANDER_STEP = 8;
export const WANDERER = 'the wandering stone';
export const RITE_KILL_REACH = 10;
export const RITE_REACH = CLAIM_REACH + 2;
/** The rites a waystone can ask for (with Rites on), as in Waystone.java. */
export const RITES = ['bow', 'eat', 'fire', 'kill'];

/** The rite a stone asks for, from where it stands. */
export const riteOf = (w: { x: number; y: number }) => RITES[(((w.x * 7 + w.y * 13) % 4) + 4) % 4];

export function riteText(rite: string): string {
  switch (rite) {
    case 'bow': return 'bow to it';
    case 'eat': return 'eat something beside it';
    case 'fire': return 'light a fire beside it';
    default: return 'kill something near it';
  }
}

/** A kingdom's favourite boon, common or rare. As in NomadSim.java. */
export function favourite(regionId: string, rare: boolean): string {
  switch (regionId) {
    case 'misthalin': return rare ? BRIDGE : CALM;
    case 'asgarnia': return rare ? KEYSTONE : ANCHOR;
    case 'kandarin': return rare ? LODESTONE : LANTERN;
    case 'fremennik': return rare ? WAKE : EXPAND;
    case 'desert': return rare ? LEAP : TAILWIND;
    case 'morytania': return rare ? RUDDER : WARD;
    case 'karamja': return rare ? WAKE : TAILWIND;
    case 'kourend': return rare ? LODESTONE : FORESIGHT;
    case 'tirannwn': return rare ? KEYSTONE : FORESIGHT;
    case 'varlamore': return rare ? BRIDGE : EXPAND;
    default: return rare ? LEAP : WARD;
  }
}

/** Minutes as "45 minutes" or "2h 05m". */
export function span(m: number): string {
  if (m < 60) return `${m} ${m === 1 ? 'minute' : 'minutes'}`;
  return `${Math.trunc(m / 60)}h ${m % 60 < 10 ? '0' : ''}${m % 60}m`;
}

/** "the waystone at X", or "the wandering stone". */
const called = (w: Waystone) => (w.landmark === WANDERER ? WANDERER : `the waystone at ${w.landmark}`);
const capital = (t: string) => t.charAt(0).toUpperCase() + t.slice(1);

/** A kingdom's signature event: its name, and what it does. As in NomadSim.java. */
export function auroraName(regionId?: string): string {
  const names: Record<string, string> = {
    misthalin: 'Harvest Moon', asgarnia: "White Knights' Muster", kandarin: 'Glider Winds', fremennik: 'Aurora',
    desert: 'Mirage', morytania: 'Blood Moon', wilderness: 'Bloodlust', karamja: 'Monsoon', kourend: "Council's Blessing",
    tirannwn: 'Crystal Song', varlamore: "Sun's Favour",
  };
  return (regionId && names[regionId]) || 'Aurora';
}

export function auroraKind(regionId?: string): number {
  switch (regionId ?? '') {
    case 'misthalin':
    case 'kourend':
      return GIFT;
    case 'asgarnia':
    case 'morytania':
      return SEEK;
    case 'kandarin':
    case 'wilderness':
      return RUSH;
    case 'desert':
    case 'tirannwn':
      return STILL;
    default:
      return GROW;
  }
}
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
  caravan: temper('Steady', 1, 1, [1, 0, 0, 0, 0, 0]),
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
  /** Minutes after docking before Main boards a boat again, unless a Helmsman sends it. For the caravan, minutes it rests at a town. */
  shore: number;
  /** Orbit: the landmark tile it circles, how far out, which way, and the minutes left. */
  orbitX: number;
  orbitY: number;
  orbitR: number;
  orbitSide: number;
  orbitLeft: number;
  /** Lodestone: the landmark tile it leans toward, and the minutes left. */
  lodeX: number;
  lodeY: number;
  lodeLeft: number;
  /** Wake: minutes left leaving one, and the ground it holds as x, y, radius triples, oldest first. */
  wakeLeft: number;
  wake?: number[];
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
  /** Minutes it lasts however its circle drifts (lightning, flotsam), 0 if only its circle leaving ends it. */
  life: number;
  /** A wandering stone hops on along its heading every 10 minutes. */
  wander: boolean;
  heading: number;
  /** Rites (a setting): whether its rite is done. */
  riteDone: boolean;
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
  bolts: number;
  stampedes: number;
  nextStampede: number;
  stampedeWarn: number;
  stampedeLeft: number;
  auroras: number;
  nextAurora: number;
  auroraCircle: number;
  auroraLeft: number;
  auroraGrow: number;
  eclipse: number;
  eclipseLeft: number;
  rifts: number;
  nextRift: number;
  rift?: CircleState;
  riftAt?: string;
  riftLeft: number;
  /** Sea lane: tail x, y and head x, y. */
  lane?: number[];
  laneAge: number;
  flotsams: number;
  nextFlotsam: number;
  /** Per kingdom, the played minute + 1 Main's middle was last in it, 0 if never. */
  visits?: number[];
  kingdom?: string;
  kingdomSince: number;
  gravityWith: number;
  gravity: number;
  homingX: number;
  homingY: number;
  homingLeft: number;
  borderKiss: boolean;
  kissA: number;
  kissB: number;
  kissLeft: number;
  deep: boolean;
  caravanLegs: number;
  foreseen: number;
  foreseenAxis: number;
  wardLeft: number;
  keystone: boolean;
  keystoneLeft: number;
  shatterNext: boolean;
  shattered: number;
  vigil: boolean;
  vigilAt?: string;
  rites: boolean;
  riteAt?: string;
}

const WAVE_TWO_ZEROES = [
  'bolts', 'stampedes', 'nextStampede', 'stampedeWarn', 'stampedeLeft', 'auroras', 'nextAurora', 'auroraCircle', 'auroraLeft',
  'auroraGrow', 'eclipse', 'eclipseLeft', 'rifts', 'nextRift', 'riftLeft', 'laneAge', 'flotsams', 'nextFlotsam', 'kingdomSince',
  'gravityWith', 'gravity', 'homingX', 'homingY', 'homingLeft', 'kissA', 'kissB', 'kissLeft', 'caravanLegs',
  'foreseen', 'foreseenAxis', 'wardLeft', 'keystoneLeft', 'shattered',
] as const;

const WAVE_THREE_FALSES = ['keystone', 'shatterNext', 'vigil', 'rites'] as const;

const ORBIT_ZEROES = ['orbitX', 'orbitY', 'orbitR', 'orbitSide', 'orbitLeft', 'lodeX', 'lodeY', 'lodeLeft', 'wakeLeft'] as const;

export const contains = (c: CircleState, x: number, y: number) => dist(x - c.x, y - c.y) <= c.radius;
export const isCaravan = (c: CircleState) => c.regionId === 'caravan';
export const isKingdom = (c: CircleState) => c.regionId != null && c.regionId !== 'caravan';
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
    // And runs saved before the world events.
    for (const k of WAVE_TWO_ZEROES) state[k] ??= 0;
    state.borderKiss ??= false;
    state.deep ??= false;
    for (const c of [...state.circles, state.twin, state.ghost, state.rift]) {
      if (c) for (const k of ORBIT_ZEROES) c[k] ??= 0;
    }
    for (const w of state.waystones) {
      w.life ??= 0;
      w.wander ??= false;
      w.heading ??= 0;
      w.riteDone ??= false;
    }
    for (const k of WAVE_THREE_FALSES) state[k] ??= false;
    if (!state.circles.some(isCaravan)) this.addCaravan();
  }

  /** The caravan sets out from a town the seed picks, for the next town along. As in NomadSim.java. */
  private addCaravan() {
    const dice = new Rng(this.state.seed ^ 0x3243f6a9);
    const from = TOWNS[Math.floor(dice.next() * TOWNS.length)];
    const to = this.nextTown(from, null, dice)!;
    this.state.circles.push({
      ...blank(from.tile.x, from.tile.y, 0, CARAVAN_RADIUS, CARAVAN_RADIUS, CARAVAN_RADIUS),
      regionId: CARAVAN.id,
      port: to.name,
    });
  }

  static start(geo: Geography, seed: number): NomadSim {
    const rng = new Rng(seed);
    const circles: CircleState[] = [];
    circles.push(blank(LUMBRIDGE.x, LUMBRIDGE.y, rng.next() * Math.PI * 2, MAIN_START, MAIN_MIN, MAIN_MAX));
    for (const r of geo.regions) {
      circles.push({ regionId: r.id, ...blank(r.home.x, r.home.y, rng.next() * Math.PI * 2, REGION_START, REGION_MIN, REGION_MAX) });
    }
    const state = {
      seed: seed | 0, rngState: rng.state, minutes: 0, bound: MAIN, outside: false, sailingUnlocked: false, sailingLevel: 0, questsMissing: [], circles,
      waystones: [], boons: [], pending: [], summons: 0, bridgeLeft: 0, calmLeft: 0, lanternX: 0, lanternY: 0,
      forks: 0, nextFork: 0, lastBig: 0, forkWarn: 0, forkAxis: 0, forkAge: 0,
      storms: 0, nextStorm: 0, stormWarn: 0, stormLeft: 0, stormKind: 0, squallFrom: 0,
      ripened: 0, comets: 0, nextComet: 0, homewardLeft: 0, ghostLeft: 0, ghostApart: false, voyages: 0, gatherLeft: 0,
      borderKiss: false, deep: false, keystone: false, shatterNext: false, vigil: false, rites: false,
    } as unknown as SimState;
    for (const k of WAVE_TWO_ZEROES) state[k] = 0;
    // The constructor adds the caravan.
    return new NomadSim(geo, state);
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

  /** Whether a tile is inside Main, counting both halves while it's forked, an open rift's far half and a sea lane. */
  inMain(x: number, y: number): boolean {
    const s = this.state;
    return contains(this.main, x, y) || (s.twin != null && contains(s.twin, x, y)) || (s.rift != null && contains(s.rift, x, y))
      || this.inLane(x, y);
  }

  /** Whether a tile lies in the sea lane Main left behind it. */
  inLane(x: number, y: number): boolean {
    const l = this.state.lane;
    return l != null && segmentDist(x, y, l[0], l[1], l[2], l[3]) <= LANE_HALF;
  }

  /** The kingdom circle a border kiss lets you cross straight into, by index, or -1. */
  kissing(): number {
    const s = this.state;
    if (s.kissLeft <= 0) return -1;
    if (s.bound + 1 === s.kissA) return s.kissB - 1;
    if (s.bound + 1 === s.kissB) return s.kissA - 1;
    return -1;
  }

  /** The caravan circle's index. */
  caravanIndex(): number {
    return this.circles.findIndex(isCaravan);
  }

  /** Testing only: the next stampede, aurora, rift or flotsam comes at the next minute. */
  eventSoonForTesting(what: string) {
    const s = this.state;
    const at = Math.max(1, s.minutes);
    if (what === 'stampede' && s.stampedeWarn === 0 && s.stampedeLeft === 0) s.nextStampede = at;
    else if (what === 'aurora' && s.auroraLeft === 0) s.nextAurora = at;
    else if (what === 'rift' && !s.rift) s.nextRift = at;
    else if (what === 'flotsam') s.nextFlotsam = at;
  }

  setBorderKiss(on: boolean) {
    const s = this.state;
    s.borderKiss = on;
    if (!on) s.kissA = s.kissB = s.kissLeft = 0;
  }

  /** Whether a tile is inside your circle, counting both of Main's halves while it's forked. */
  inYours(x: number, y: number): boolean {
    const s = this.state;
    return (s.bound === MAIN ? this.inMain(x, y) : contains(this.bound, x, y)) || inWake(this.bound, x, y)
      || (s.bound === MAIN && s.twin != null && inWake(s.twin, x, y));
  }

  name(c: CircleState): string {
    return c.regionId ? this.geo.region(c.regionId)?.name ?? c.regionId : 'Main';
  }

  colour(c: CircleState): number {
    return c.regionId ? this.geo.region(c.regionId)?.colour ?? 0xffffff : 0xff981f;
  }

  stranded(): boolean {
    return this.state.bound !== MAIN && !touches(this.bound, this.main);
  }

  volcanoOpen(): boolean {
    return this.state.bound === MAIN && contains(this.main, VOLCANO.x, VOLCANO.y);
  }

  linked(index: number): boolean {
    return index !== this.state.bound && (this.state.bound === MAIN || index === MAIN || (index >= 0 && index === this.kissing()));
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
      case FORESIGHT:
        line = this.foresight();
        break;
      case WAKE:
        c.wakeLeft = WAKE_MINUTES;
        if (s.bound === MAIN && s.twin) s.twin.wakeLeft = WAKE_MINUTES;
        line = `Wake: for an hour the ground your circle leaves stays yours for ${WAKE_TRAIL} minutes after.`;
        break;
      case LEAP: {
        const leapt = this.leap(c);
        if (leapt == null) return null;
        line = leapt;
        break;
      }
      case KEYSTONE:
        if (s.twin) {
          s.keystoneLeft = KEYSTONE_MINUTES;
          line = 'Keystone: both halves of Main live on for another hour before they part.';
        } else {
          s.keystone = true;
          line = 'Keystone: at the next fork, both halves of Main live on for an hour before they part.';
        }
        break;
      default:
        return null;
    }
    s.boons.splice(index, 1);
    return line;
  }

  /** Lodestone: your circle leans toward a landmark for 3 hours. As in NomadSim.java. */
  useLodestone(index: number, landmark: string): string | null {
    const s = this.state;
    if (index < 0 || index >= s.boons.length || s.boons[index] !== LODESTONE) return null;
    if (!this.lodestoneTargets().includes(landmark)) return null;
    const l = LANDMARKS.find((k) => k.name === landmark)!;
    const c = this.bound;
    c.lodeX = l.x;
    c.lodeY = l.y;
    c.lodeLeft = LODE_MINUTES;
    if (s.bound === MAIN && s.twin) {
      s.twin.lodeX = l.x;
      s.twin.lodeY = l.y;
      s.twin.lodeLeft = LODE_MINUTES;
    }
    s.boons.splice(index, 1);
    return `Lodestone: for 3 hours your circle leans toward ${l.name}.`;
  }

  /** The landmarks a Lodestone can point your circle at, nearest first. */
  lodestoneTargets(): string[] {
    const c = this.bound;
    const region = isKingdom(c) ? this.geo.region(c.regionId!)! : null;
    const near: Landmark[] = [];
    for (const l of LANDMARKS) {
      const d = dist(l.x - c.x, l.y - c.y);
      if (d > LODE_REACH || d < c.radius / 2) continue;
      if (region && !this.geo.inRegion(region, l.x, l.y)) continue;
      let at = 0;
      while (at < near.length && dist(near[at].x - c.x, near[at].y - c.y) <= d) at++;
      near.splice(at, 0, l);
    }
    return near.map((l) => l.name);
  }

  /** Foresight: plays a copy of the run ahead to find the next fork's warning. */
  private foresight(): string {
    const s = this.state;
    const ahead = this.clone();
    ahead.state.vigil = false;
    ahead.state.rites = false;
    for (let k = 0; k < FORESIGHT_LOOK; k++) {
      if (ahead.state.forkWarn > 0 && !ahead.state.twin) break;
      ahead.step(null);
    }
    if (ahead.state.forkWarn <= 0 || ahead.state.twin) {
      s.foreseen = 0;
      return `Foresight: no fork comes in the next ${FORESIGHT_LOOK / 60} hours.`;
    }
    s.foreseen = ahead.state.minutes;
    s.foreseenAxis = ahead.state.forkAxis;
    return `Foresight: the next fork comes in about ${span(Math.max(1, s.foreseen - s.minutes))}, one half heading ${compass(s.foreseenAxis)} and the other ${compass(s.foreseenAxis + Math.PI)}.`;
  }

  /** Leap: jumps up to 40 tiles along its heading, as far as stays on land or in its kingdom. */
  private leap(c: CircleState): string | null {
    if (c.voyage) return null;
    const region = isKingdom(c) ? this.geo.region(c.regionId!)! : null;
    for (let d = LEAP_TILES; d >= 4; d -= 2) {
      const nx = c.x + Math.cos(c.heading) * d;
      const ny = c.y + Math.sin(c.heading) * d;
      const ok = region ? this.geo.inRegion(region, nx, ny) : this.geo.onLand(nx, ny);
      if (!ok) continue;
      c.x = nx;
      c.y = ny;
      c.wx = c.wy = 0;
      c.orbitLeft = 0;
      c.pulse = 45;
      return `Leap: your circle jumped ${d} tiles ${compass(c.heading)}.`;
    }
    return null;
  }

  setShatterNext(on: boolean) {
    this.state.shatterNext = on;
  }

  setVigil(on: boolean) {
    this.state.vigil = on;
    if (!on) delete this.state.vigilAt;
  }

  setRites(on: boolean) {
    this.state.rites = on;
    if (!on) delete this.state.riteAt;
  }

  /** A rite done where you stand. As in NomadSim.java. */
  performRite(rite: string, player: Tile | null): string | null {
    const s = this.state;
    if (!s.rites || player == null) return null;
    const reach = rite === 'kill' ? RITE_KILL_REACH : RITE_REACH;
    let done: string | null = null;
    for (const w of s.waystones) {
      if (w.riteDone || riteOf(w) !== rite || dist(player.x - w.x, player.y - w.y) > reach) continue;
      w.riteDone = true;
      done ??= called(w);
    }
    return done == null ? null : `Rite done: ${done} will give up its boon now.`;
  }

  /** Where a wandering stone hops next, as [x, y, heading], or null. As in NomadSim.java. */
  nextHop(w: Waystone): [number, number, number] | null {
    if (!w.wander) return null;
    const c = this.circles[w.circle];
    for (let k = 0; k < 8; k++) {
      const t = k === 0 ? 0 : k === 7 ? 4 : Math.trunc((k + 1) / 2) * (k % 2 === 1 ? 1 : -1);
      const h = w.heading + (t * Math.PI) / 4;
      const nx = javaRound(w.x + Math.cos(h) * WANDER_STEP);
      const ny = javaRound(w.y + Math.sin(h) * WANDER_STEP);
      if (!contains(c, nx, ny) || !this.geo.onLand(nx, ny) || this.takenAt(nx, ny)) continue;
      return [nx, ny, h];
    }
    return null;
  }

  private hop(w: Waystone, log: string[]) {
    const to = this.nextHop(w);
    if (!to) return;
    [w.x, w.y, w.heading] = to;
    if (w.circle === this.state.bound) log.push(`The wandering stone hopped ${WANDER_STEP} tiles ${compass(w.heading)}.`);
  }

  /** Wake: a circle leaving one adds where it is each minute; once it stops, the oldest minute fades each minute. */
  private wakes(log: string[]) {
    const s = this.state;
    const all: (CircleState | undefined)[] = [...this.circles, s.twin];
    all.forEach((c, j) => {
      if (!c) return;
      if (c.wakeLeft > 0) {
        const w = c.wake ?? [];
        const keep = Math.min(w.length, (WAKE_TRAIL - 1) * 3);
        c.wake = [...w.slice(w.length - keep), c.x, c.y, c.radius];
        if (--c.wakeLeft === 0 && j === s.bound && c === this.bound) {
          log.push(`Your circle stops leaving a Wake; the ground behind it fades over ${WAKE_TRAIL} minutes.`);
        }
      } else if (c.wake) {
        if (c.wake.length <= 3) delete c.wake;
        else c.wake = c.wake.slice(3);
      }
    });
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
    const left = minutesLeft(this.circles[w.circle], w.x, w.y);
    return w.life > 0 ? Math.min(left, Math.max(0, w.life - w.age)) : left;
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
    if (!to.regionId) return 'You crossed back into the Main circle.';
    if (isCaravan(to)) return 'You joined the caravan. It walks the roads between bank towns, and the only way off is back into Main.';
    return `You crossed into ${this.name(to)}. It never leaves its kingdom, and the only way out is back into Main.`;
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
      const ward = s.boons.indexOf(WARD);
      if (out && s.wardLeft <= 0 && ward >= 0) {
        s.boons.splice(ward, 1);
        s.wardLeft = WARD_MINUTES;
        log.push(`Your Ward breaks: for ${WARD_MINUTES} minutes, being outside costs nothing.`);
      }
    }
    // A broken Ward: outside, your circle neither shrinks nor hurries.
    const penalty = out && s.wardLeft <= 0;
    if (penalty && b.radius > b.radiusMin) {
      b.radius = Math.max(b.radiusMin, b.radius - 1);
      b.radiusTarget = b.radius;
    }

    const stranded = this.stranded();
    if (!stranded) b.waiting = false;
    const partner = this.gravityPartner(player, out, log);
    this.circles.forEach((c, j) => {
      const yours = j === s.bound;
      const aurora = j === s.auroraCircle && s.auroraLeft > 0 ? auroraKind(c.regionId) : 0;
      const pulled = yours && stranded && isKingdom(c);
      const gather = (s.gatherLeft > 0 && isKingdom(c)) || aurora === SEEK;
      let toward: CircleState | null = pulled || gather ? main : null;
      let pull = pulled ? STRANDED_PULL : gather ? GATHER_PULL : 0;
      // Gravity: standing where two circles overlap draws their middles together.
      if (toward == null && partner >= 0 && (j === s.bound || j === partner)) {
        toward = this.circles[j === s.bound ? partner : s.bound];
        pull = GRAVITY_PULL;
      }
      const lean = yours && !out && player != null && dist(player.x - c.x, player.y - c.y) >= c.radius - LEAN_BAND ? player : null;
      const speedup = (yours && penalty ? OUTSIDE_SPEEDUP : 1) * (c.tailwindLeft > 0 ? 2 : 1)
        * (j === MAIN && s.stormLeft > 0 && s.stormKind === GALE ? GALE_SPEEDUP : 1)
        * (isKingdom(c) && s.stampedeLeft > 0 ? STAMPEDE_SPEEDUP : 1)
        * (aurora === RUSH ? RUSH_SPEEDUP : 1);
      if (j === MAIN && s.twin) part(c, s.twin, this.geo);
      else if (s.eclipse > 0 && j === s.eclipse) {
        // Eclipsed: it moves as one with Main.
        c.x = main.x;
        c.y = main.y;
        c.heading = main.heading;
        if (c.pulse > 0) c.pulse--;
      } else if (yours && c.crownLeft > 0 && player != null && !c.voyage) this.follow(c, player);
      else if (j === MAIN && s.deep && !c.voyage && (s.minutes & 1) === 1) {
        // Deep time: underground, Main drifts only every other minute.
      } else if (aurora === STILL) {
        // Its aurora holds it still.
      } else if (isCaravan(c)) this.caravan(c, yours, stranded, log);
      else this.move(c, rng, speedup, yours, toward, pull, lean, log);
    });
    this.wakes(log);
    s.minutes++;
    this.tickBoons(log);
    this.fork(player, log);
    this.storm(log);
    this.ghost(log);
    this.comet(player, log);
    this.stampede(log);
    this.aurora(log);
    this.eclipseStep(log);
    this.riftStep(player, log);
    this.seaLane();
    this.flotsam(log);
    this.exodus(log);
    this.kiss(log);

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
        shiftRange(c, -EXPAND_TILES);
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
    if (s.homingLeft > 0) s.homingLeft--;
    if (s.wardLeft > 0 && --s.wardLeft === 0) log.push("The Ward's protection has worn off.");
    this.circles.forEach((c, j) => {
      if (c.lodeLeft > 0 && --c.lodeLeft === 0 && j === s.bound) log.push("The Lodestone's pull has faded.");
    });
    if (s.twin && s.twin.lodeLeft > 0) s.twin.lodeLeft--;
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
    let keptVigil: string | undefined;
    let askedRite: string | undefined;
    if (player != null && s.boons.length < BOONS_HELD) {
      for (let i = 0; i < s.waystones.length; i++) {
        const w = s.waystones[i];
        const anywhere = w.pilgrim || w.tier === LEGENDARY;
        if (w.circle !== s.bound || (!anywhere && !contains(this.bound, player.x, player.y))) continue;
        if (dist(player.x - w.x, player.y - w.y) > CLAIM_REACH) continue;
        const key = `${w.landmark}|${w.circle}`;
        if (s.rites && !w.riteDone) {
          askedRite = key;
          if (key !== s.riteAt) log.push(`Rite: to claim ${called(w)}, ${riteText(riteOf(w))}.`);
          break;
        }
        const host = this.circles[w.circle];
        if (s.vigil && !anywhere && dist(w.x - host.x, w.y - host.y) > host.radius / 2 && key !== s.vigilAt) {
          keptVigil = key;
          log.push(`Vigil: stand your ground on ${called(w)} for a minute to claim it.`);
          break;
        }
        s.waystones.splice(i, 1);
        if (s.shatterNext && w.tier !== LEGENDARY) {
          s.shatterNext = false;
          const dice = new Rng(s.seed ^ Math.imul(s.shattered + 1, 0x2f1b3c4d));
          s.shattered++;
          if (dice.next() < SHATTER_ODDS) {
            const won = RARE_BOONS[Math.floor(dice.next() * RARE_BOONS.length)];
            s.boons.push(won);
            log.push(`You shattered ${called(w)}: it burst into ${won} (rare)!`);
          } else {
            log.push(`You shattered ${called(w)}: it crumbled to nothing.`);
          }
          break;
        }
        s.boons.push(w.boon);
        log.push(`You claimed ${called(w)}: ${w.boon}. Spend it from the sidebar.`);
        if (w.circle === MAIN) {
          // Homing: Main leans back toward where you claimed it, less and less over 12 hours.
          s.homingX = w.x;
          s.homingY = w.y;
          s.homingLeft = HOMING_MINUTES;
        }
        break;
      }
    }
    if (keptVigil != null) s.vigilAt = keptVigil;
    else delete s.vigilAt;
    if (askedRite != null) s.riteAt = askedRite;
    else delete s.riteAt;
    for (let i = s.waystones.length - 1; i >= 0; i--) {
      const w = s.waystones[i];
      if (w.tier === LEGENDARY) continue;
      if (w.life > 0 && w.age >= w.life) {
        s.waystones.splice(i, 1);
        log.push(w.landmark === STRIKE ? 'The waystone at the lightning strike burned out.' : `The waystone at ${w.landmark} is gone.`);
        continue;
      }
      if (w.pilgrim) {
        if (w.age < PILGRIM_MINUTES) continue;
        s.waystones.splice(i, 1);
        log.push(`The pilgrim stone at ${w.landmark} sank back into the ground.`);
        continue;
      }
      if (contains(this.circles[w.circle], w.x, w.y)) continue;
      s.waystones.splice(i, 1);
      log.push(`${capital(called(w))} faded: its circle drifted off it.`);
    }
    for (const w of s.waystones) {
      w.age++;
      if (w.wander && w.age % WANDER_EVERY === 0) this.hop(w, log);
      if (w.pilgrim || w.life > 0 || w.tier !== COMMON || w.age !== RIPEN) continue;
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
    // Kingdom stones: the Wilderness only gives rare ones, and a kingdom's stone often holds its favourite boon.
    const c = this.circles[j];
    const rare = tier === RARE || (isKingdom(c) && c.regionId === 'wilderness');
    const pool = rare ? RARE_BOONS : COMMON_BOONS;
    const l: Landmark = LANDMARKS[k];
    let boon = pool[Math.floor(rng.next() * pool.length)];
    if (isKingdom(c) && rng.next() < KINGDOM_FAVOUR) boon = favourite(c.regionId!, rare);
    const w = this.stone(l, j, rare ? RARE : COMMON, boon);
    if (rare && rng.next() < WANDER_CHANCE) {
      w.wander = true;
      w.landmark = WANDERER;
      w.heading = round(rng.next() * Math.PI * 2 * 1e4) / 1e4;
    }
    return `${w.wander ? 'A wandering stone rose at ' : 'A waystone rose at '}${l.name}${j === s.bound ? ' in your circle' : ` in ${this.name(c)}`}, holding ${w.boon} (${rare ? 'rare' : 'common'}).${w.wander ? ` It hops ${WANDER_STEP} tiles every ${WANDER_EVERY} minutes.` : ''}`;
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
    const w: Waystone = { landmark: l.name, x: l.x, y: l.y, circle, boon, tier, age: 0, pilgrim: false, life: 0, wander: false, heading: 0, riteDone: false };
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
    } else if (region && isKingdom(c) && !geo.inRegion(region, c.x, c.y)) {
      // Out of its kingdom (after an eclipse): it walks straight home.
      const home = region.home;
      const d = dist(home.x - c.x, home.y - c.y);
      if (d > 0) {
        const step = Math.min(d, STRAY_SPEED);
        c.heading = Math.atan2(home.y - c.y, home.x - c.x);
        c.x += ((home.x - c.x) / d) * step;
        c.y += ((home.y - c.y) / d) * step;
      }
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
      const orbiting = c.orbitLeft > 0;
      if (orbiting) orbit(c, yours, log, this.geo);
      else {
        if (c.moodLeft <= 0) this.startMood(c, t, rng, allowed, yours, log);
        c.moodLeft--;
      }
      const m = c.mood;
      // In a peninsula or cove, Main forgets its mood and keeps a steady pace until it's out.
      // Bound for a port, it goes into the cove the port sits in, and holds its course.
      const depth = region || c.port != null ? 0 : geo.pocketDepth(c.x, c.y);
      const cramped = depth >= CRAMPED_DEPTH;
      const pocket = depth >= POCKET_DEPTH;
      const steady = cramped || c.port != null;

      if (!orbiting) {
        const target = steady ? 0 : m === LOOP ? c.bend : m === MEANDER ? c.bend * Math.sin(c.swing * MEANDER_STEP) : 0;
        c.swing++;
        c.turn += (target - c.turn) * TURN_EASE + (rng.next() - 0.5) * 2 * MOOD_WOBBLE[m];
        c.turn = clamp(c.turn, -TURN_MAX, TURN_MAX);
        c.heading += c.turn;
        c.speed += ((steady ? 1 : MOOD_SPEED[m]) - c.speed) * SPEED_EASE;
      }

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
      } else if (c.wx !== 0 && MOOD_PULL[m] > 0 && !cramped && !orbiting) steer(c, c.wx, c.wy, MOOD_PULL[m]);
      // Homing: the last waystone you claimed in Main pulls it back a little, less and less.
      const s = this.state;
      if (!c.regionId && s.homingLeft > 0 && depth === 0 && c.port == null && !orbiting) {
        steer(c, s.homingX, s.homingY, (HOMING_PULL * s.homingLeft) / HOMING_MINUTES);
      }
      // Lodestone: it leans toward the landmark you picked.
      if (c.lodeLeft > 0 && c.port == null && !orbiting) steer(c, c.lodeX, c.lodeY, LODE_PULL);
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
          c.orbitLeft = 0;
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
    if (!c.regionId && s.helmsTo == null && this.geo.pocketDepth(c.x, c.y) === 0) {
      // Orbit, on its own dice: now and then it circles a landmark near its middle instead.
      const spin = new Rng(s.seed ^ Math.imul(s.minutes + 1, 0x7a3b1c5d));
      if (spin.next() < ORBIT_CHANCE && this.startOrbit(c, spin, yours, log)) return;
    }
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

  /** Orbit: picks a landmark 8 to 24 tiles from Main's middle to circle round; false if there's none. */
  private startOrbit(c: CircleState, spin: Rng, yours: boolean, log: string[]): boolean {
    const near = LANDMARKS.filter((l) => {
      const d = dist(l.x - c.x, l.y - c.y);
      return d >= ORBIT_NEAR && d <= ORBIT_FAR && this.geo.onLand(l.x, l.y);
    });
    if (!near.length) return false;
    const l = near[Math.floor(spin.next() * near.length)];
    c.orbitX = l.x;
    c.orbitY = l.y;
    c.orbitR = dist(l.x - c.x, l.y - c.y);
    c.orbitSide = spin.next() < 0.5 ? 1 : -1;
    c.orbitLeft = ORBIT_MINUTES;
    c.wx = c.wy = 0;
    delete c.port;
    if (yours) log.push(`Your circle starts to orbit ${l.name}.`);
    return true;
  }

  /** Lightning: a bolt strikes a spot of land inside Main, leaving a common waystone for 2 minutes. As in NomadSim.java. */
  private strike(m: CircleState, log: string[]) {
    const s = this.state;
    const dice = new Rng(s.seed ^ Math.imul(s.bolts + 1, 0x4cf5ad43));
    s.bolts++;
    for (let k = 0; k < 12; k++) {
      const a = dice.next() * Math.PI * 2;
      const d = Math.sqrt(dice.next()) * m.radius * 0.9;
      const x = round(m.x + Math.cos(a) * d);
      const y = round(m.y + Math.sin(a) * d);
      if (!this.geo.onLand(x, y) || this.takenAt(x, y)) continue;
      const boon = COMMON_BOONS[Math.floor(dice.next() * COMMON_BOONS.length)];
      s.waystones.push({ landmark: STRIKE, x, y, circle: MAIN, boon, tier: COMMON, age: 0, pilgrim: false, life: STRIKE_MINUTES, wander: false, heading: 0, riteDone: false });
      log.push(`Lightning! A bolt struck inside the Main circle, ${javaRound(d)} tiles ${compass(a)} of its middle, and left a waystone holding ${boon} (common) for ${STRIKE_MINUTES} minutes.`);
      return;
    }
  }

  private takenAt(x: number, y: number): boolean {
    return this.state.waystones.some((w) => w.x === x && w.y === y);
  }

  /** Storm surge: the storm drives Main out to sea from a port inside it or near its edge. True if it did. */
  private surge(m: CircleState, log: string[]): boolean {
    const s = this.state;
    const missing = s.questsMissing ?? [];
    const ports = this.geo.ports.filter((p) => dist(p.tile.x - m.x, p.tile.y - m.y) <= m.radius + SURGE_REACH
      && this.geo.routesFrom(p.name, this.dockLevel(), missing).length > 0);
    if (!ports.length) return false;
    const dice = new Rng(s.seed ^ Math.imul(s.storms + 1, 0x2545f491));
    const port = ports[Math.floor(dice.next() * ports.length)];
    const routes = this.geo.routesFrom(port.name, this.dockLevel(), missing);
    const to = routes[Math.floor(dice.next() * routes.length)];
    log.push(`Storm surge! The storm broke over ${port.name} and drives the Main circle out to sea.`);
    this.board(m, port, to, log);
    return true;
  }

  private stampede(log: string[]) {
    const s = this.state;
    if (s.stampedeLeft > 0) {
      if (--s.stampedeLeft > 0) return;
      s.stampedes++;
      s.nextStampede = 0;
      log.push("The stampede is over: the kingdoms' circles slow back down.");
      return;
    }
    if (s.stampedeWarn > 0) {
      if (--s.stampedeWarn > 0) return;
      s.stampedeLeft = STAMPEDE_MINUTES;
      log.push(`Stampede! Every kingdom's circle dashes for ${STAMPEDE_MINUTES} minutes.`);
      return;
    }
    if (s.nextStampede <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.stampedes + 1, 0x6a09e667));
      s.nextStampede = s.minutes + STAMPEDE_EVERY_MIN + Math.trunc(dice.next() * (STAMPEDE_EVERY_MAX - STAMPEDE_EVERY_MIN));
    }
    if (s.minutes < s.nextStampede) return;
    s.stampedeWarn = STAMPEDE_WARN;
    log.push(`A stampede is coming! In ${STAMPEDE_WARN} minutes every kingdom's circle dashes for ${STAMPEDE_MINUTES} minutes. The Main circle stays calm.`);
  }

  private aurora(log: string[]) {
    const s = this.state;
    if (s.auroraLeft > 0) {
      if (--s.auroraLeft > 0) return;
      const c = this.circles[s.auroraCircle];
      if (s.auroraGrow !== 0) shiftRange(c, -s.auroraGrow);
      s.auroraGrow = 0;
      log.push(`The ${auroraName(c.regionId)} over ${this.name(c)} has passed.`);
      s.auroraCircle = 0;
      s.auroras++;
      s.nextAurora = 0;
      return;
    }
    if (s.nextAurora <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.auroras + 1, 0x3c6ef372));
      s.nextAurora = s.minutes + AURORA_EVERY_MIN + Math.trunc(dice.next() * (AURORA_EVERY_MAX - AURORA_EVERY_MIN));
    }
    if (s.minutes < s.nextAurora) return;
    const dice = new Rng(s.seed ^ Math.imul(s.auroras + 1, 0x0bb67ae8));
    const kingdoms: number[] = [];
    this.circles.forEach((c, j) => {
      if (isKingdom(c) && j !== s.eclipse) kingdoms.push(j);
    });
    const j = kingdoms[Math.floor(dice.next() * kingdoms.length)];
    const c = this.circles[j];
    s.auroraCircle = j;
    s.auroraLeft = AURORA_MINUTES;
    const name = auroraName(c.regionId);
    const where = this.name(c);
    switch (auroraKind(c.regionId)) {
      case GROW:
        s.auroraGrow = round(c.radius);
        shiftRange(c, s.auroraGrow);
        c.pulse = 45;
        log.push(`${name} over ${where}! Its circle doubles in size for an hour.`);
        break;
      case RUSH:
        log.push(`${name} over ${where}! Its circle rushes about four times as fast for an hour.`);
        break;
      case SEEK:
        log.push(`${name} over ${where}! Its circle marches toward the Main circle for an hour.`);
        break;
      case STILL:
        log.push(`${name} over ${where}! Its circle holds perfectly still for an hour.`);
        break;
      default: {
        const rose = this.gift(j, dice);
        log.push(`${name} over ${where}!${rose == null ? ' Its circle glows for an hour.' : ` ${rose}`}`);
      }
    }
  }

  private gift(j: number, dice: Rng): string | null {
    const c = this.circles[j];
    const free = LANDMARKS.filter((l) => contains(c, l.x, l.y) && !this.takenAt(l.x, l.y));
    if (!free.length) return null;
    const l = free[Math.floor(dice.next() * free.length)];
    const w = this.stone(l, j, COMMON, COMMON_BOONS[Math.floor(dice.next() * COMMON_BOONS.length)]);
    return `A waystone rose at ${l.name} in ${this.name(c)}, holding ${w.boon} (common).`;
  }

  private eclipseStep(log: string[]) {
    const s = this.state;
    const m = this.main;
    if (s.eclipse > 0) {
      if (--s.eclipseLeft > 0 && !m.voyage && !s.twin) return;
      const c = this.circles[s.eclipse];
      const home = this.geo.inRegion(this.geo.region(c.regionId!)!, c.x, c.y);
      log.push(`The eclipse is over. The ${this.name(c)} circle goes its own way${home ? '.' : ', walking home to its kingdom.'}`);
      s.eclipse = 0;
      s.eclipseLeft = 0;
      return;
    }
    if (s.twin || m.voyage) return;
    for (let j = 0; j < this.circles.length; j++) {
      const c = this.circles[j];
      if (!isKingdom(c) || dist(c.x - m.x, c.y - m.y) > ECLIPSE_REACH) continue;
      s.eclipse = j;
      s.eclipseLeft = ECLIPSE_MINUTES;
      c.x = m.x;
      c.y = m.y;
      c.pulse = 45;
      s.lastBig = s.minutes;
      const name = this.name(c);
      log.push(`Eclipse! The Main circle and the ${name} circle line up and move as one for an hour. The ${name} circle can leave its kingdom with Main.`);
      return;
    }
  }

  private riftStep(player: Tile | null, log: string[]) {
    const s = this.state;
    const m = this.main;
    if (s.rift) {
      if (--s.riftLeft > 0) return;
      const there = s.bound === MAIN && player != null && contains(s.rift, player.x, player.y) && !contains(m, player.x, player.y);
      if (there) {
        m.x = s.rift.x;
        m.y = s.rift.y;
        delete m.voyage;
        delete m.port;
        m.orbitLeft = 0;
        m.moodLeft = 0;
        m.wx = m.wy = 0;
        m.turn = 0;
        m.pulse = 45;
        s.lastBig = s.minutes;
        log.push(`The rift closed. You stood at ${s.riftAt}, so the Main circle is there now.`);
      } else {
        log.push('The rift closed. The Main circle stays where it was.');
      }
      delete s.rift;
      delete s.riftAt;
      s.rifts++;
      s.nextRift = 0;
      return;
    }
    if (s.nextRift <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.rifts + 1, 0x510e527f));
      s.nextRift = s.minutes + RIFT_EVERY_MIN + Math.trunc(dice.next() * (RIFT_EVERY_MAX - RIFT_EVERY_MIN));
    }
    if (s.minutes < s.nextRift) return;
    if (m.voyage || s.twin || s.forkWarn > 0) {
      s.nextRift = s.minutes + RIFT_RETRY;
      return;
    }
    const far = LANDMARKS.filter((l) => dist(l.x - m.x, l.y - m.y) >= RIFT_FAR && this.geo.pocketDepth(l.x, l.y) === 0);
    if (!far.length) {
      s.nextRift = s.minutes + RIFT_RETRY;
      return;
    }
    const dice = new Rng(s.seed ^ Math.imul(s.rifts + 1, 0x1f83d9ab));
    const l = far[Math.floor(dice.next() * far.length)];
    const rift = structuredClone(m);
    rift.x = l.x;
    rift.y = l.y;
    delete rift.voyage;
    delete rift.port;
    rift.crownLeft = 0;
    rift.orbitLeft = 0;
    s.rift = rift;
    s.riftAt = l.name;
    s.riftLeft = RIFT_MINUTES;
    s.lastBig = s.minutes;
    log.push(`A rift opened! For ${RIFT_MINUTES} minutes the Main circle also stands at ${l.name}, far to the ${compass(Math.atan2(l.y - m.y, l.x - m.x))}. Teleport there and you're inside; when the rift closes, Main is whichever side you stand in.`);
  }

  private seaLane() {
    const s = this.state;
    const lane = s.lane;
    if (!lane) return;
    const m = this.main;
    s.laneAge++;
    if (m.voyage) {
      lane[2] = m.x;
      lane[3] = m.y;
    }
    if (s.laneAge <= LANE_MINUTES) return;
    const d = dist(lane[2] - lane[0], lane[3] - lane[1]);
    if (d <= SAIL_SPEED) {
      if (!m.voyage) delete s.lane;
      else {
        lane[0] = lane[2];
        lane[1] = lane[3];
      }
      return;
    }
    lane[0] += ((lane[2] - lane[0]) / d) * SAIL_SPEED;
    lane[1] += ((lane[3] - lane[1]) / d) * SAIL_SPEED;
  }

  private flotsam(log: string[]) {
    const s = this.state;
    if (!s.sailingUnlocked) return;
    if (s.nextFlotsam <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.flotsams + 1, 0x5be0cd19));
      s.nextFlotsam = s.minutes + FLOTSAM_EVERY_MIN + Math.trunc(dice.next() * (FLOTSAM_EVERY_MAX - FLOTSAM_EVERY_MIN));
    }
    if (s.minutes < s.nextFlotsam) return;
    const dice = new Rng(s.seed ^ Math.imul(s.flotsams + 1, 0x9b05688c | 0));
    s.flotsams++;
    s.nextFlotsam = 0;
    const m = this.main;
    for (let k = 0; k < 16; k++) {
      const a = dice.next() * Math.PI * 2;
      const d = Math.sqrt(dice.next()) * m.radius;
      const x = round(m.x + Math.cos(a) * d);
      const y = round(m.y + Math.sin(a) * d);
      if (this.geo.onLand(x, y) || !contains(m, x, y) || this.takenAt(x, y)) continue;
      const boon = COMMON_BOONS[Math.floor(dice.next() * COMMON_BOONS.length)];
      s.waystones.push({ landmark: FLOTSAM, x, y, circle: MAIN, boon, tier: COMMON, age: 0, pilgrim: false, life: FLOTSAM_MINUTES, wander: false, heading: 0, riteDone: false });
      log.push(`Flotsam is floating at sea inside the Main circle, ${compass(a)} of its middle, holding ${boon} (common). Sail through it within the hour to claim it.`);
      return;
    }
  }

  private exodus(log: string[]) {
    const s = this.state;
    const m = this.main;
    if (m.voyage) return;
    const regions = this.geo.regions;
    if (!s.visits || s.visits.length !== regions.length) s.visits = regions.map(() => 0);
    const here = this.geo.regionAt(m.x, m.y);
    if (here) s.visits[regions.indexOf(here)] = s.minutes + 1;
    if ((here?.id ?? null) !== (s.kingdom ?? null)) {
      if (here) s.kingdom = here.id;
      else delete s.kingdom;
      s.kingdomSince = s.minutes;
      return;
    }
    if (!here || s.minutes - s.kingdomSince < EXODUS_MINUTES || s.twin || m.port != null || m.orbitLeft > 0) return;
    s.kingdomSince = s.minutes;
    let best = -1;
    regions.forEach((r, k) => {
      if (r === here || this.geo.pocketDepth(r.home.x, r.home.y) !== 0) return;
      if (this.geo.harbourSteps(this.geo.homeOf(r), m.x, m.y) < 0) return;
      if (best < 0 || s.visits![k] < s.visits![best]) best = k;
    });
    if (best < 0) return;
    const to = regions[best];
    m.wx = to.home.x;
    m.wy = to.home.y;
    m.mood = ROAM;
    m.moodLeft = EXODUS_MOOD;
    m.avoid = 0;
    log.push(`Exodus! After ${Math.trunc(EXODUS_MINUTES / 60)} hours in ${here.name}, the Main circle grows restless and sets off for ${to.name}.`);
  }

  private gravityPartner(player: Tile | null, out: boolean, log: string[]): number {
    const s = this.state;
    if (s.gravityWith > 0 && (s.gravityWith > this.circles.length || !touches(this.bound, this.circles[s.gravityWith - 1]))) {
      s.gravityWith = 0;
      s.gravity = 0;
    }
    if (player == null || out || isCaravan(this.bound)) return -1;
    let partner = -1;
    for (let j = 0; j < this.circles.length && partner < 0; j++) {
      const c = this.circles[j];
      if (j === s.bound || !this.linked(j) || isCaravan(c) || j === s.eclipse) continue;
      if (contains(this.bound, player.x, player.y) && contains(c, player.x, player.y)) partner = j;
    }
    if (partner < 0) return -1;
    if (partner + 1 !== s.gravityWith) {
      s.gravityWith = partner + 1;
      s.gravity = 0;
    }
    if (s.gravity >= GRAVITY_MINUTES) return -1;
    if (s.gravity++ === 0) {
      log.push(`Gravity: you stand where your circle overlaps ${this.name(this.circles[partner])}, so their middles drift together, for up to ${GRAVITY_MINUTES} minutes.`);
    }
    return partner;
  }

  private kiss(log: string[]) {
    const s = this.state;
    if (s.kissA > 0 && (s.kissA > this.circles.length || s.kissB > this.circles.length
      || !touches(this.circles[s.kissA - 1], this.circles[s.kissB - 1]))) {
      if (s.kissLeft > 0) log.push('The border kiss closed: the two circles drifted apart.');
      s.kissA = s.kissB = s.kissLeft = 0;
    }
    if (s.kissLeft > 0) {
      if (s.bound === MAIN || gap(this.bound, this.main) > KISS_RANGE || --s.kissLeft === 0) {
        s.kissLeft = 0;
        log.push('The border kiss closed.');
      }
      return;
    }
    if (s.kissA > 0 || !s.borderKiss || s.bound === MAIN) return;
    const b = this.bound;
    if (!isKingdom(b) || gap(b, this.main) > KISS_RANGE) return;
    for (let j = 0; j < this.circles.length; j++) {
      const c = this.circles[j];
      if (j === s.bound || !isKingdom(c) || !touches(b, c)) continue;
      s.kissA = s.bound + 1;
      s.kissB = j + 1;
      s.kissLeft = KISS_MINUTES;
      s.lastBig = s.minutes;
      log.push(`Border kiss! Your circle touches ${this.name(c)} across the border: for ${KISS_MINUTES} minutes you may cross straight into it.`);
      return;
    }
  }

  /** The Trade road: the caravan walks the roads between bank towns and rests at each. As in NomadSim.java. */
  private caravan(c: CircleState, yours: boolean, stranded: boolean, log: string[]) {
    const s = this.state;
    if (c.pulse > 0) c.pulse--;
    if (c.anchorLeft > 0) return;
    let to = c.port == null ? undefined : this.geo.town(c.port);
    if (yours && stranded) {
      const m = this.main;
      const home = this.nearestTown(m.x, m.y, c);
      if (home && dist(m.x - c.x, m.y - c.y) <= dist(m.x - home.tile.x, m.y - home.tile.y) + CARAVAN_ARRIVE) {
        // As near Main as the town nearest it, or nearer: it leaves the road and goes looking for Main.
        c.shore = 0;
        const d = dist(m.x - c.x, m.y - c.y);
        if (d <= 0) return;
        const nx = c.x + ((m.x - c.x) / d) * CARAVAN_SPEED;
        const ny = c.y + ((m.y - c.y) / d) * CARAVAN_SPEED;
        if (!this.geo.onLand(nx, ny)) return;
        c.heading = Math.atan2(m.y - c.y, m.x - c.x);
        c.x = nx;
        c.y = ny;
        return;
      }
      if (home && home !== to) {
        to = home;
        c.port = home.name;
        c.shore = 0;
      }
    }
    if (!to) {
      const dice = new Rng(s.seed ^ Math.imul(s.caravanLegs + 1, 0x6c8e9cf5));
      s.caravanLegs++;
      const next = this.nextTown(null, c, dice);
      if (!next) return;
      to = next;
      c.port = next.name;
    }
    if (c.shore > 0) {
      if (--c.shore === 0) {
        const dice = new Rng(s.seed ^ Math.imul(s.caravanLegs + 1, 0x6c8e9cf5));
        s.caravanLegs++;
        const next = this.nextTown(to, c, dice);
        if (next) {
          c.port = next.name;
          if (yours) log.push(`The caravan sets out from ${to.name} for ${next.name}.`);
        }
      }
      return;
    }
    if (dist(to.tile.x - c.x, to.tile.y - c.y) <= CARAVAN_ARRIVE) {
      c.shore = CARAVAN_REST;
      if (yours) log.push(`The caravan reached ${to.name} and rests there for ${CARAVAN_REST} minutes.`);
      return;
    }
    const way = this.geo.wayTo(to, c.x, c.y, CARAVAN_LOOKAHEAD);
    let nx = c.x;
    let ny = c.y;
    if (way) {
      const wd = dist(way.x - c.x, way.y - c.y);
      const step = Math.min(wd, CARAVAN_SPEED * (c.tailwindLeft > 0 ? 2 : 1));
      if (wd > 0) {
        c.heading = Math.atan2(way.y - c.y, way.x - c.x);
        nx = c.x + ((way.x - c.x) / wd) * step;
        ny = c.y + ((way.y - c.y) / wd) * step;
      }
    }
    if (!way || !this.geo.onLand(nx, ny)) {
      // Off the road (a corner of river in the way): it steps onto the road's nearest cell instead.
      const snap = this.geo.snapToWalk(to, c.x, c.y);
      if (!snap) return;
      nx = snap.x;
      ny = snap.y;
    }
    c.x = nx;
    c.y = ny;
  }

  /** One of the three towns nearest Main that the caravan isn't at and can walk to, as the seed picks. */
  private nextTown(from: Port | null, c: CircleState | null, dice: Rng): Port | null {
    const m = this.main;
    const options: Port[] = [];
    for (const p of TOWNS) {
      if (p === from) continue;
      if (c && this.geo.harbourSteps(p, c.x, c.y) < 0) continue;
      const d = dist(p.tile.x - m.x, p.tile.y - m.y);
      let at = 0;
      while (at < options.length && dist(options[at].tile.x - m.x, options[at].tile.y - m.y) <= d) at++;
      options.splice(at, 0, p);
    }
    if (!options.length) return null;
    return options[Math.floor(dice.next() * Math.min(CARAVAN_CHOICES, options.length))];
  }

  /** The town nearest a point that the caravan can walk to. */
  private nearestTown(x: number, y: number, c: CircleState): Port | null {
    let best: Port | null = null;
    let bestD = 0;
    for (const p of TOWNS) {
      const d = dist(p.tile.x - x, p.tile.y - y);
      if (best != null && d >= bestD) continue;
      if (this.geo.harbourSteps(p, c.x, c.y) < 0) continue;
      best = p;
      bestD = d;
    }
    return best;
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
    const from = port.name;
    const wrecks = this.geo.ports
      .filter((p) => p.name !== from && p.name !== to && this.geo.routesFrom(p.name, level, missing).length > 0)
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
    c.orbitLeft = 0;
    // Sea lane: the way it sails counts as inside behind it for a while.
    this.state.lane = [c.x, c.y, c.x, c.y];
    this.state.laneAge = 0;
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
      if (--s.stormLeft > 0) {
        const gone = STORM_MINUTES - s.stormLeft;
        if (s.stormKind === SQUALL && gone % STRIKE_EVERY === 0 && Math.trunc(gone / STRIKE_EVERY) <= STRIKES) this.strike(m, log);
        return;
      }
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
      if (m.anchorLeft <= 0 && m.shore <= 0 && this.surge(m, log)) {
        s.stormKind = 0;
        s.storms++;
        s.nextStorm = 0;
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
      if (s.keystoneLeft > 0) {
        // Keystone: both halves live on until it gives way.
        if (--s.keystoneLeft === 0) log.push('The Keystone gave way: the halves of Main part as they drift apart, and the one you stand in lives on.');
        return;
      }
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
      if (s.keystone) {
        s.keystone = false;
        s.keystoneLeft = KEYSTONE_MINUTES;
        log.push('The Keystone holds: both halves live on for an hour before they part.');
      }
      return;
    }
    if (s.nextFork <= 0) {
      const dice = new Rng(s.seed ^ Math.imul(s.forks + 1, 0x85ebca6b | 0));
      s.nextFork = s.minutes + FORK_EVERY_MIN + Math.trunc(dice.next() * (FORK_EVERY_MAX - FORK_EVERY_MIN));
    }
    // Pacing: after 4 quiet hours the next fork comes now.
    if (s.minutes - s.lastBig >= QUIET) s.nextFork = Math.min(s.nextFork, s.minutes);
    if (s.minutes < s.nextFork) return;
    if (m.voyage || m.anchorLeft > 0 || m.radius < FORK_MIN_RADIUS || s.stormWarn > 0 || s.stormLeft > 0 || s.calmLeft > 0 || s.rift) {
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
    s.foreseen = 0;
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

/** A circle with nothing on it yet. */
function blank(x: number, y: number, heading: number, start: number, min: number, max: number): CircleState {
  return {
    x, y, heading, radius: start, radiusTarget: start, radiusMin: min, radiusMax: max,
    pulse: 0, tiers: 0, atEdge: false, waiting: false, touchingYours: false,
    mood: 0, moodLeft: 0, turn: 0, bend: 0, swing: 0, speed: 1, wx: 0, wy: 0, avoid: 0,
    expand: 0, expandLeft: 0, tailwindLeft: 0, anchorLeft: 0, crownLeft: 0, shore: 0,
    orbitX: 0, orbitY: 0, orbitR: 0, orbitSide: 0, orbitLeft: 0, lodeX: 0, lodeY: 0, lodeLeft: 0, wakeLeft: 0,
  };
}

/** Whether a tile lies in the ground a circle's Wake still holds. */
export function inWake(c: CircleState, x: number, y: number): boolean {
  const w = c.wake;
  if (!w) return false;
  for (let k = 0; k + 2 < w.length; k += 3) if (dist(x - w[k], y - w[k + 1]) <= w[k + 2]) return true;
  return false;
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

/** Orbit: steers round its landmark, holding its distance, then flings off in a straight line. As in NomadSim.java. */
function orbit(c: CircleState, yours: boolean, log: string[], geo: Geography) {
  const a = Math.atan2(c.y - c.orbitY, c.x - c.orbitX);
  const d = dist(c.x - c.orbitX, c.y - c.orbitY);
  const off = clamp(((d - c.orbitR) / c.orbitR) * 2, -0.6, 0.6);
  const want = a + c.orbitSide * (Math.PI / 2 + off);
  const turn = want - c.heading;
  c.heading += Math.atan2(Math.sin(turn), Math.cos(turn)) * ORBIT_STEER;
  c.turn = 0;
  c.speed += (ORBIT_SPEED - c.speed) * ORBIT_EASE;
  if (--c.orbitLeft > 0) return;
  // Fling: off in a straight line, dashing.
  c.mood = DASH;
  c.moodLeft = MOOD_MAX[DASH];
  const wx = round(c.x + Math.cos(c.heading) * FLING_WAY);
  const wy = round(c.y + Math.sin(c.heading) * FLING_WAY);
  const ok = geo.onLand(wx, wy) && geo.pocketDepth(wx, wy) === 0;
  c.wx = ok ? wx : 0;
  c.wy = ok ? wy : 0;
  if (yours) log.push(`Your circle flings off ${compass(c.heading)}!`);
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
