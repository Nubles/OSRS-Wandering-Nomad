import type { SimState } from './sim';

/**
 * A run code is how the plugin hands a run to the website: "N1." followed by the saved run
 * (the plugin's SavedRun JSON), gzipped and base64url-encoded. It fits in a link.
 */
export interface SavedRun {
  sim: SimState;
  surface?: { x: number; y: number };
  /** The run's epithet so far, such as "the Seaborn". */
  epithet?: string;
  retired?: boolean;
  /** The run's story (the plugin's sim/Story.java), in codes copied for the website. */
  story?: Story;
}

/** A run's story: what the Saga, report card, side-by-side and the other forks are drawn from. */
export interface Story {
  v: number;
  /** Hourly: minute, Main's x, y and radius, where you stood (or -1), the circle you were bound to. */
  track?: number[][];
  /** Played minutes by the kingdom you stood in, and by Main's mood. */
  kingdoms?: Record<string, number>;
  moods?: Record<string, number>;
  /** The smallest your circle got with you inside, in tiles. */
  smallest?: number;
  closest?: string;
  /** Each fork's lost half: minute, x, y, heading in thousandths, radius, the kept half's x and y. */
  partings?: number[][];
  /** Map regions walked inside, as RuneLite region ids. */
  squares?: number[];
  acts?: { start: number; opened?: string; kingdom?: string }[];
  beats?: { minute: number; x: number; y: number; kind: string; title?: string; line?: string }[];
  counts?: Record<string, number>;
  grave?: number[];
  hardcore?: boolean;
}

const PREFIX = 'N1.';

export async function encodeRun(run: SavedRun): Promise<string> {
  const gz = await pipe(new TextEncoder().encode(JSON.stringify(run)), new CompressionStream('gzip'));
  return PREFIX + toBase64Url(gz);
}

export async function decodeRun(code: string): Promise<SavedRun> {
  const trimmed = code.trim();
  if (!trimmed.startsWith(PREFIX)) throw new Error('That is not a Nomad UIM run code. It should start with N1.');
  const json = new TextDecoder().decode(await pipe(fromBase64Url(trimmed.slice(PREFIX.length)), new DecompressionStream('gzip')));
  const run = JSON.parse(json) as SavedRun;
  if (!run?.sim || !Array.isArray(run.sim.circles) || run.sim.circles.length === 0) {
    throw new Error('That run code has no circles in it.');
  }
  return run;
}

async function pipe(bytes: Uint8Array, stream: CompressionStream | DecompressionStream): Promise<Uint8Array> {
  const out = new Blob([bytes]).stream().pipeThrough(stream);
  return new Uint8Array(await new Response(out).arrayBuffer());
}

function toBase64Url(bytes: Uint8Array): string {
  let s = '';
  for (const b of bytes) s += String.fromCharCode(b);
  return btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function fromBase64Url(s: string): Uint8Array {
  const b64 = s.replace(/-/g, '+').replace(/_/g, '/');
  const bin = atob(b64 + '='.repeat((4 - (b64.length % 4)) % 4));
  return Uint8Array.from(bin, (c) => c.charCodeAt(0));
}
