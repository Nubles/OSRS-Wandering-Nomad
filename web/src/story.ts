import { useEffect, useState } from 'react';
import { regionName, type Geography, type Tile } from './engine/geography';
import { decodeRun, SavedRun, Story } from './engine/runcode';
import { compass, MOOD_NAMES, NomadSim } from './engine/sim';
import { Rng } from './engine/rng';

/**
 * What the website's run pages share: the last pasted run code, the Saga's chapters, the report
 * card, the forks a run didn't keep, its sigil, a seed's prophecy and the Circle Guesser's days.
 * Pure where it can be, so the tests can hold it.
 */

const LAST_RUN_KEY = 'nomad.lastRun';

export function lastRun(): string {
  try {
    return localStorage.getItem(LAST_RUN_KEY) ?? '';
  } catch {
    return '';
  }
}

export function rememberRun(code: string): void {
  try {
    localStorage.setItem(LAST_RUN_KEY, code);
  } catch {
    /* private mode: it just isn't remembered */
  }
}

/** Decodes a run code once it changes; remembers good ones for the other pages. */
export function useRun(code: string | undefined): { run: SavedRun | null; error: string | null } {
  const [state, setState] = useState<{ run: SavedRun | null; error: string | null }>({ run: null, error: null });
  useEffect(() => {
    let live = true;
    if (!code) {
      setState({ run: null, error: null });
      return;
    }
    decodeRun(code)
      .then((run) => {
        if (!live) return;
        rememberRun(code.trim());
        setState({ run, error: null });
      })
      .catch((e: Error) => live && setState({ run: null, error: e.message }));
    return () => {
      live = false;
    };
  }, [code]);
  return state;
}

export function played(minutes: number): string {
  const h = Math.floor(minutes / 60);
  return h > 0 ? `${h}h ${String(minutes % 60).padStart(2, '0')}m` : `${minutes}m`;
}

const ROMAN = ['I', 'II', 'III', 'IV', 'V', 'VI', 'VII', 'VIII', 'IX', 'X', 'XI', 'XII'];

export interface SagaLine {
  minute: number;
  text: string;
  kind: string;
  x?: number;
  y?: number;
}

export interface Chapter {
  title: string;
  start: number;
  end: number;
  kingdom?: string;
  opened?: string;
  lines: SagaLine[];
  /** Main's track through the chapter, for its vignette. */
  track: number[][];
}

/** What opened an act, worded as the plugin's Acts.why words it. Codes that already carry a sentence keep it. */
const OPENERS: Record<string, string> = {
  voyage: 'Opened by following Main across the sea',
  diary: 'Opened by your first diary tier',
  total: 'Opened by reaching 1,500 total level',
  '99': 'Opened by your first 99',
};

/** A beat's title and line as one sentence, without doubling the title's own full stop or "!". */
function sentence(title: string | undefined, line: string | undefined): string {
  if (!title) return line ?? '';
  if (!line) return title;
  return `${title}${/[.!?]$/.test(title) ? '' : '.'} ${line}`;
}

/** A quiet stretch longer than this becomes one line in the Saga. */
export const QUIET_HOURS = 6;

/**
 * The Saga: the run's acts as chapters, each with its beats in order and quiet stretches folded
 * into a single line, so a 300-hour run still reads in a few minutes.
 */
export function chapters(story: Story, minutes: number): Chapter[] {
  const acts = story.acts?.length ? story.acts : [{ start: 0, opened: 'The run begins at Lumbridge.' }];
  const beats = [...(story.beats ?? [])].sort((a, b) => a.minute - b.minute);
  const track = story.track ?? [];
  const partings = story.partings ?? [];
  return acts.map((a, i) => {
    const start = a.start;
    const end = i + 1 < acts.length ? acts[i + 1].start : minutes;
    const raw: SagaLine[] = [];
    for (const b of beats) {
      if (b.minute < start || b.minute >= end) continue;
      const text = sentence(b.title, b.line);
      if (text) raw.push({ minute: b.minute, text, kind: b.kind, x: b.x >= 0 ? b.x : undefined, y: b.y >= 0 ? b.y : undefined });
    }
    for (const p of partings) {
      if (p[0] < start || p[0] >= end) continue;
      raw.push({ minute: p[0], text: `A fork parted. The ${compass(p[3] / 1000)} half faded and drifts on somewhere without you.`, kind: 'parting', x: p[1], y: p[2] });
    }
    raw.sort((x, y) => x.minute - y.minute);
    const lines: SagaLine[] = [];
    let last = start;
    for (const l of raw) {
      if (l.minute - last >= QUIET_HOURS * 60) {
        lines.push({ minute: last, text: `${Math.round((l.minute - last) / 60)} quiet hours pass.`, kind: 'quiet' });
      }
      lines.push(l);
      last = l.minute;
    }
    if (end - last >= QUIET_HOURS * 60 && i + 1 < acts.length) {
      lines.push({ minute: last, text: `${Math.round((end - last) / 60)} quiet hours pass.`, kind: 'quiet' });
    }
    return {
      title: `Act ${ROMAN[i] ?? i + 1}${a.kingdom ? `: ${regionName(a.kingdom)}` : ''}`,
      start,
      end,
      kingdom: a.kingdom ? regionName(a.kingdom) : undefined,
      opened: a.opened ? OPENERS[a.opened] ?? a.opened : undefined,
      lines,
      track: track.filter((t) => t[0] >= start && t[0] <= end),
    };
  });
}

export interface Report {
  played: string;
  kingdoms: { name: string; hours: number; share: number }[];
  moods: { name: string; hours: number; share: number }[];
  smallest: number | null;
  closest: string | null;
  counts: { label: string; value: number }[];
}

const COUNT_LABELS: [string, string][] = [
  ['walked', 'Tiles walked inside'],
  ['claimed', 'Waystones claimed'],
  ['boons', 'Boons spent'],
  ['forks', 'Forks outlived'],
  ['storms', 'Storms weathered'],
  ['crossings', 'Crossings'],
  ['voyages', 'Voyages followed'],
  ['harbours', 'Harbours'],
  ['landmarks', 'Landmarks'],
  ['omens', 'Omens met'],
  ['trials', 'Trials done'],
  ['deaths', 'Deaths'],
];

/** The one-screen report card: hours per kingdom and mood, the smallest circle and the closest escape. */
export function report(run: SavedRun): Report | null {
  const s = run.story;
  if (!s) return null;
  const spread = (m: Record<string, number> | undefined) => {
    const total = Object.values(m ?? {}).reduce((a, b) => a + b, 0) || 1;
    return Object.entries(m ?? {})
      .map(([name, mins]) => ({ name, hours: Math.round((mins / 60) * 10) / 10, share: mins / total }))
      .sort((a, b) => b.share - a.share);
  };
  return {
    played: played(run.sim.minutes),
    kingdoms: spread(s.kingdoms),
    moods: spread(s.moods),
    smallest: s.smallest ? s.smallest : null,
    closest: s.closest ?? null,
    counts: COUNT_LABELS.filter(([k]) => s.counts && k in s.counts).map(([k, label]) => ({ label, value: s.counts![k] })),
  };
}

/**
 * The other fork: where a fork's lost half would be now, drifting on at Main's usual pace with a
 * slow wander of its own, turning from coasts. Only for forks already parted.
 */
export function ghostPath(geo: Geography, parting: number[], now: number, step = 10): Tile[] {
  const [minute, x0, y0, headingMilli] = parting;
  const rng = new Rng(Math.imul(minute + 1, 0x9e3779b1) ^ (x0 * 31 + y0));
  let x = x0;
  let y = y0;
  let h = headingMilli / 1000;
  let turn = 0;
  const path: Tile[] = [{ x, y }];
  const span = Math.max(0, now - minute);
  // At most two days of drift, which is far past the point anyone could follow it.
  for (let m = step; m <= Math.min(span, 2880); m += step) {
    turn = turn * 0.8 + (rng.next() - 0.5) * 0.3;
    h += turn;
    const pace = 0.35 * step;
    let nx = x + Math.cos(h) * pace;
    let ny = y + Math.sin(h) * pace;
    for (let k = 0; k < 12 && !geo.onLand(Math.round(nx), Math.round(ny)); k++) {
      h += 0.5;
      nx = x + Math.cos(h) * pace;
      ny = y + Math.sin(h) * pace;
    }
    if (!geo.onLand(Math.round(nx), Math.round(ny))) break;
    x = nx;
    y = ny;
    path.push({ x: Math.round(x), y: Math.round(y) });
  }
  return path;
}

/** The kingdoms in a prophecy's words: never a name or a tile. */
const VAGUE: Record<string, string> = {
  misthalin: 'the green heart',
  asgarnia: 'the white walls',
  kandarin: 'the western realm',
  fremennik: 'cold country',
  desert: 'the sands',
  morytania: 'the dark marsh',
  wilderness: 'the wild north',
  karamja: 'the jungle isle',
  kourend: 'the far shore',
  tirannwn: 'the elven woods',
  varlamore: 'the southern sun',
};

/**
 * A seed's prophecy: one vague line about its opening six hours, read from the run itself (Main
 * standing still for nobody). Words only, no coordinates.
 */
export function prophecy(geo: Geography, seed: number, hours = 6): string {
  const sim = NomadSim.start(geo, seed);
  const start = { x: sim.main.x, y: sim.main.y };
  const moods = new Array(MOOD_NAMES.length).fill(0);
  const reached: string[] = [];
  let forkAt = -1;
  const forksAtStart = sim.state.forks;
  let sailed = false;
  let storm = false;
  for (let m = 0; m < hours * 60; m++) {
    const forks = sim.state.forks;
    sim.step(null);
    moods[sim.main.mood]++;
    if (forkAt < 0 && (sim.state.forks > forks || sim.state.twin)) forkAt = sim.state.minutes;
    if (sim.main.voyage) sailed = true;
    if (sim.state.stormKind > 0 || sim.state.storms > 0) storm = true;
    const r = geo.regionAt(Math.round(sim.main.x), Math.round(sim.main.y));
    if (r && !reached.includes(r.id)) reached.push(r.id);
  }
  const dx = sim.main.x - start.x;
  const dy = sim.main.y - start.y;
  const far = Math.hypot(dx, dy);
  const way = far < 25 ? null : compass(Math.atan2(dy, dx));
  const top = moods.indexOf(Math.max(...moods));
  const temper = ['wanders wide', 'winds and doubles back', 'circles a thing it cannot leave', 'runs', 'waits', 'drifts where it is pushed'][top];
  const away = reached.filter((id) => id !== 'misthalin');
  const parts: string[] = [];
  parts.push(way ? `The circle ${temper}, heading ${way}${away.length ? `, toward ${VAGUE[away[away.length - 1]] ?? 'strange country'}` : ''}.` : `The circle ${temper}, and stays close to home.`);
  const splits = sim.state.forks - forksAtStart;
  if (splits >= 3) parts.push('It splits again and again.');
  else if (splits === 2) parts.push(forkAt < 120 ? 'It splits early, and once more.' : 'It splits twice.');
  else if (forkAt >= 0) parts.push(forkAt < 120 ? 'It splits early.' : forkAt > 240 ? 'Late on, it splits in two.' : 'Once, it splits in two.');
  else parts.push(far > 120 ? 'It holds together and does not look back.' : 'It holds together, and nothing breaks the quiet.');
  if (sailed) parts.push('It will taste salt.');
  else if (storm) parts.push('A storm finds it.');
  return parts.join(' ');
}

/** A footprint sigil: a mirrored pixel crest from the map regions walked, coloured by the seed. */
export interface SigilArt {
  size: number;
  /** One palette index per pixel, row by row: 0 empty, 1 to 4 the palette. */
  pixels: number[];
  palette: string[];
}

export const SIGIL_SIZE = 16;

export function sigil(squares: number[], seed: number): SigilArt {
  const n = SIGIL_SIZE;
  const half = n / 2;
  const rng = new Rng(seed ^ 0x51e1);
  const hue = Math.floor(rng.next() * 360);
  const palette = [
    `hsl(${hue} 45% 38%)`,
    `hsl(${hue} 70% 58%)`,
    `hsl(${(hue + 45) % 360} 85% 70%)`,
    '#f4e7c1',
    `hsl(${(hue + 200) % 360} 30% 18%)`,
  ];
  // A shield's outline: everything outside it stays empty.
  const inShield = (x: number, y: number) => {
    if (x < 0 || x >= n || y < 0 || y >= n) return false;
    const cx = Math.abs(x + 0.5 - half) / half;
    const cy = (y + 0.5) / n;
    return cy < 0.6 ? cx < 0.9 : cx < 0.9 * Math.sqrt(Math.max(0, (1 - cy) / 0.4));
  };
  // Each square lights a cell of the half-crest, by its place on the map.
  const lit = new Array(half * n).fill(0);
  const xs = squares.map((s) => s >> 8);
  const ys = squares.map((s) => s & 255);
  const minX = Math.min(...xs, 99);
  const maxX = Math.max(...xs, 0);
  const minY = Math.min(...ys, 99);
  const maxY = Math.max(...ys, 0);
  const w = Math.max(1, maxX - minX + 1);
  const h = Math.max(1, maxY - minY + 1);
  for (let i = 0; i < squares.length; i++) {
    const gx = Math.min(half - 1, Math.floor(((xs[i] - minX) / w) * half));
    const gy = Math.min(n - 1, Math.floor(((maxY - ys[i]) / h) * n));
    lit[gy * half + gx]++;
  }
  // A few seeded marks, so two crests from small footprints still differ.
  for (let k = 0; k < 6; k++) lit[Math.floor(rng.next() * n) * half + Math.floor(rng.next() * half)] += 1;
  const at = (x: number, y: number) => (x >= 0 && x < half && y >= 0 && y < n ? lit[y * half + x] : 0);
  const pixels = new Array(n * n).fill(0);
  for (let y = 0; y < n; y++) {
    for (let x = 0; x < n; x++) {
      if (!inShield(x, y)) continue;
      if (!inShield(x - 1, y) || !inShield(x + 1, y) || !inShield(x, y + 1) || !inShield(x, y - 1)) {
        pixels[y * n + x] = 4;
        continue;
      }
      const src = x < half ? x : n - 1 - x;
      const v = at(src, y);
      const near = at(src - 1, y) + at(src + 1, y) + at(src, y - 1) + at(src, y + 1);
      // Walked squares glow; their neighbours take the crest's colour; the rest is the field.
      pixels[y * n + x] = v >= 2 ? 3 : v === 1 ? 2 : near > 0 ? 1 : 5;
    }
  }
  return { size: n, pixels, palette };
}

/** Draws a sigil at {@code scale} pixels per cell. */
export function drawSigil(ctx: CanvasRenderingContext2D, art: SigilArt, scale: number): void {
  ctx.clearRect(0, 0, art.size * scale, art.size * scale);
  for (let y = 0; y < art.size; y++) {
    for (let x = 0; x < art.size; x++) {
      const v = art.pixels[y * art.size + x];
      if (!v) continue;
      ctx.fillStyle = art.palette[v - 1];
      ctx.fillRect(x * scale, y * scale, scale, scale);
    }
  }
}

/** The Circle Guesser's day: days since 1 January 2026 in UTC, so everyone gets the same five circles. */
export function guessDay(now = Date.now()): number {
  return Math.floor((now - Date.UTC(2026, 0, 1)) / 86_400_000);
}

export const GUESS_ROUNDS = 5;

/** A round's run: its seed and how many played minutes in. */
export function guessRound(day: number, round: number): { seed: number; minutes: number } {
  const rng = new Rng(day * 7919 + round * 104729 + 17);
  return { seed: Math.floor(rng.next() * 1_000_000_000), minutes: 600 + Math.floor(rng.next() * 40) * 60 };
}

/** Points for a guess this many tiles off: 1,000 inside the circle's own reach, none past 1,500 tiles. */
export function guessScore(off: number, radius: number): number {
  if (off <= radius) return 1000;
  return Math.round(1000 * Math.max(0, 1 - (off - radius) / 1500) ** 2);
}

export function guessSquare(score: number): string {
  return score >= 900 ? '🟩' : score >= 500 ? '🟨' : score >= 150 ? '🟧' : '🟥';
}
