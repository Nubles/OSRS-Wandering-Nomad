/**
 * The live relay (workers/nomad-relay). The plugin publishes a run under a live code; this
 * reads it. VITE_RELAY_URL points a local build at another relay.
 */
export const RELAY = (import.meta.env.VITE_RELAY_URL as string | undefined) || 'https://nomad-relay.fatelocked.workers.dev';

export const LIVE_CODE_RE = /^[A-Za-z0-9_-]{24}$/;

/** How often the page asks the relay for news. */
export const POLL_MS = 20_000;
/** The plugin sends at least this often while you play, so a longer silence means it stopped. */
export const STALE_MS = 10 * 60_000;

export interface LiveRun {
  run: string;
  online: boolean;
  /** When the relay received it, in ms. */
  at: number;
  version: number;
}

export type LiveFetch = { kind: 'run'; data: LiveRun; etag: string | null } | { kind: 'same' } | { kind: 'missing' };

export async function fetchLive(code: string, etag: string | null, base = RELAY): Promise<LiveFetch> {
  const r = await fetch(`${base}/n/${code}`, { headers: etag ? { 'If-None-Match': etag } : {}, cache: 'no-store' });
  if (r.status === 304) return { kind: 'same' };
  if (r.status === 404) return { kind: 'missing' };
  if (!r.ok) throw new Error(`The live relay answered ${r.status}.`);
  return { kind: 'run', data: (await r.json()) as LiveRun, etag: r.headers.get('ETag') };
}

/** Whether the player is still playing: online at the last send, and that send is recent. */
export function isLive(data: LiveRun, now = Date.now()): boolean {
  return data.online && now - data.at < STALE_MS;
}

export function ago(ms: number): string {
  const m = Math.round(ms / 60_000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  return h < 48 ? `${h} h ago` : `${Math.round(h / 24)} days ago`;
}
