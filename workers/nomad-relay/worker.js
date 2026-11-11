/**
 * Nomad UIM live relay. The plugin publishes a run under a random live code
 * (POST /n/<code>, with a private write token); the website follows it
 * (GET /n/<code>). The run is the same run code the plugin puts in links
 * ("N1." + gzipped JSON); the relay only opens one to draw a link card.
 *
 * The website's shared pages, each opt-in in the plugin:
 *   GET /lobby          live runs that joined the lobby (metadata on run:<code>)
 *   POST /g/<code>      a tombstone; GET /g lists them, without codes
 *   POST /a/<code>      kingdoms and map regions walked; GET /a sums them
 *   GET /c/<code>       a live link's card page, /c/<code>/card.png its picture
 *   GET /r/<run code>   the same for a run code
 *
 * Built on the pattern of Fate Locked's fate-relay: the id is checked against
 * the exact shape its client makes, writes are rate-limited per IP before any
 * KV access, and the first write claims a code by storing a hash of its token.
 */

import { cardHtml, decodeRun, describe, renderCard, SITE } from './card.js';

export const LIVE_ROUTE = /^\/n\/([^/]*)$/;
const GRAVE_ROUTE = /^\/g\/([^/]*)$/;
const ATLAS_ROUTE = /^\/a\/([^/]*)$/;
const LIVE_CARD_ROUTE = /^\/c\/([A-Za-z0-9_-]{24})(\/card\.png)?$/;
const RUN_CARD_ROUTE = /^\/r\/(N1\.[A-Za-z0-9_-]{16,14000})(\/card\.png)?$/;
/** 18 random bytes in base64url, made by the plugin. */
export const LIVE_CODE_RE = /^[A-Za-z0-9_-]{24}$/;
/** 32 random bytes in base64url, kept by the plugin and never shown. */
const TOKEN_RE = /^Bearer ([A-Za-z0-9_-]{43})$/;
export const MAX_BODY_BYTES = 16 * 1024;
const RUN_CODE_RE = /^N1\.[A-Za-z0-9_-]{16,}$/;
/** A run stays watchable for a week after its last publish. */
const RUN_TTL_SECONDS = 7 * 86400;
/** A code stays claimed for 90 days after its last claim refresh. */
const OWNER_TTL_SECONDS = 90 * 86400;
const OWNER_REFRESH_MS = 7 * 86400 * 1000;
/** Viewers poll; the edge answers repeats from cache for this long. */
const READ_MAX_AGE_SECONDS = 10;

const LIMITERS = { write: 'WRITE_LIMITER', claim: 'CLAIM_LIMITER' };
/** The lobby shows runs that sent within this long. */
const LOBBY_FRESH_MS = 30 * 60_000;
/** Tombstones and atlas entries stay a year after their last write. */
const SHARED_TTL_SECONDS = 365 * 86400;
const SHARE_BODY_BYTES = 32 * 1024;
const MOODS = ['Roaming', 'Meandering', 'Looping', 'Dashing', 'Lingering', 'Drifting'];
/** The atlas's grid of the map's 64-tile regions: region x 16 to 63, region y 38 to 69. */
export const ATLAS_X0 = 16;
export const ATLAS_Y0 = 38;
export const ATLAS_COLS = 48;
export const ATLAS_ROWS = 32;
const KINGDOM_COUNT = 31;

async function withinLimit(env, kind, request) {
  const limiter = env?.[LIMITERS[kind]];
  if (!limiter || typeof limiter.limit !== 'function') return true;
  const ip = request.headers.get('CF-Connecting-IP') || 'unknown';
  try {
    const { success } = await limiter.limit({ key: `${kind}:${ip}` });
    return success !== false;
  } catch {
    return true;
  }
}

function cors() {
  return {
    'Access-Control-Allow-Origin': '*',
    'Access-Control-Allow-Methods': 'GET, POST, DELETE, OPTIONS',
    'Access-Control-Allow-Headers': 'Content-Type, If-None-Match, Authorization',
    'Access-Control-Expose-Headers': 'ETag',
    'Access-Control-Max-Age': '86400',
  };
}

function text(body, status, extra = {}) {
  return new Response(body, { status, headers: { ...cors(), 'Cache-Control': 'no-store', ...extra } });
}

async function readBodyWithin(request, limit) {
  const declared = Number(request.headers.get('Content-Length'));
  if (Number.isFinite(declared) && declared > limit) return null;
  if (!request.body) return '';
  const reader = request.body.getReader();
  const chunks = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    total += value.byteLength;
    if (total > limit) {
      await reader.cancel();
      return null;
    }
    chunks.push(value);
  }
  const bytes = new Uint8Array(total);
  let at = 0;
  for (const c of chunks) {
    bytes.set(c, at);
    at += c.byteLength;
  }
  return new TextDecoder().decode(bytes);
}

async function tokenHash(token) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(token));
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, '0')).join('');
}

/** Whether this token may write the code, claiming it when nobody has. */
async function authorise(env, request, code, token) {
  const ownerKey = `own:${code}`;
  const hash = await tokenHash(token);
  const owner = await env.LIVE.get(ownerKey, 'json');
  if (owner) {
    if (owner.hash !== hash) return 'forbidden';
    if (Date.now() - owner.at > OWNER_REFRESH_MS) {
      await env.LIVE.put(ownerKey, JSON.stringify({ hash, at: Date.now() }), { expirationTtl: OWNER_TTL_SECONDS });
    }
    return 'ok';
  }
  if (!(await withinLimit(env, 'claim', request))) return 'limited';
  await env.LIVE.put(ownerKey, JSON.stringify({ hash, at: Date.now() }), { expirationTtl: OWNER_TTL_SECONDS });
  return 'ok';
}

async function read(request, env, code, ctx) {
  const cache = typeof caches !== 'undefined' ? caches.default : null;
  const cacheKey = new Request(new URL(request.url).toString(), { method: 'GET' });
  let stored = null;
  const hit = cache ? await cache.match(cacheKey) : null;
  if (hit) {
    stored = await hit.json();
  } else {
    stored = await env.LIVE.get(`run:${code}`, 'json');
    if (!stored) return text('not found', 404);
    if (cache) {
      const copy = new Response(JSON.stringify(stored), { headers: { 'Cache-Control': `public, max-age=${READ_MAX_AGE_SECONDS}` } });
      ctx?.waitUntil?.(cache.put(cacheKey, copy));
    }
  }
  const etag = String(stored.version);
  const asked = request.headers.get('If-None-Match');
  if (asked && asked.replace(/^W\//, '').replace(/"/g, '') === etag) {
    return new Response(null, { status: 304, headers: { ...cors(), ETag: etag, 'Cache-Control': 'no-store' } });
  }
  return new Response(JSON.stringify(stored), {
    status: 200,
    headers: { ...cors(), 'Content-Type': 'application/json', ETag: etag, 'Cache-Control': 'no-store' },
  });
}

async function write(request, env, code) {
  const auth = TOKEN_RE.exec(request.headers.get('Authorization') || '');
  if (!auth) return text('unauthorised', 401);
  if (!(await withinLimit(env, 'write', request))) return text('too many requests', 429, { 'Retry-After': '60' });

  if (request.method === 'DELETE') {
    const who = await authorise(env, request, code, auth[1]);
    if (who === 'forbidden') return text('forbidden', 403);
    if (who === 'limited') return text('too many requests', 429, { 'Retry-After': '60' });
    await env.LIVE.delete(`run:${code}`);
    return text(null, 204);
  }

  const raw = await readBodyWithin(request, MAX_BODY_BYTES);
  if (raw === null) return text('too large', 413);
  let body;
  try {
    body = JSON.parse(raw);
  } catch {
    return text('bad request', 400);
  }
  if (!body || typeof body.run !== 'string' || !RUN_CODE_RE.test(body.run) || typeof body.online !== 'boolean') {
    return text('bad request', 400);
  }
  const who = await authorise(env, request, code, auth[1]);
  if (who === 'forbidden') return text('forbidden', 403);
  if (who === 'limited') return text('too many requests', 429, { 'Retry-After': '60' });

  const previous = await env.LIVE.get(`run:${code}`, 'json');
  const now = Date.now();
  const stored = { run: body.run, online: body.online, at: now, version: Math.max(now, (previous?.version ?? 0) + 1) };
  // The lobby reads every run's metadata in one list, so joining it costs no extra write.
  const lobby = cleanLobby(body.lobby);
  const metadata = { at: now, online: body.online, ...(lobby ? { lobby } : {}) };
  await env.LIVE.put(`run:${code}`, JSON.stringify(stored), { expirationTtl: RUN_TTL_SECONDS, metadata });
  return text(JSON.stringify({ version: stored.version }), 200, { 'Content-Type': 'application/json' });
}

/** Integers in range, or null. */
function int(v, lo, hi) {
  return Number.isInteger(v) && v >= lo && v <= hi ? v : null;
}

/** A name a player chose to show: letters, digits, spaces, _ and -, at most 12. */
function cleanName(v) {
  if (typeof v !== 'string') return null;
  const n = v.replace(/[^A-Za-z0-9 _-]/g, '').replace(/\s+/g, ' ').trim().slice(0, 12);
  return n || null;
}

function cleanText(v, max) {
  if (typeof v !== 'string') return null;
  const t = v.replace(/[<>\u0000-\u001f\u007f]/g, ' ').replace(/\s+/g, ' ').trim().slice(0, max);
  return t || null;
}

export function cleanLobby(v) {
  if (!v || typeof v !== 'object') return null;
  const x = int(v.x, 0, 16000);
  const y = int(v.y, 0, 16000);
  const r = int(v.r, 1, 600);
  if (x === null || y === null || r === null) return null;
  return { name: cleanName(v.name), x, y, r, mood: MOODS.includes(v.mood) ? v.mood : null };
}

export function cleanGrave(v) {
  if (!v || typeof v !== 'object') return null;
  const x = int(v.x, 0, 16000);
  const y = int(v.y, 0, 16000);
  const minutes = int(v.minutes, 0, 10_000_000);
  if (x === null || y === null || minutes === null) return null;
  return {
    name: cleanName(v.name),
    x,
    y,
    minutes,
    seed: int(v.seed, -2147483648, 2147483647) ?? 0,
    kingdom: cleanText(v.kingdom, 40),
    circle: cleanText(v.circle, 40),
    cx: int(v.cx, 0, 16000) ?? x,
    cy: int(v.cy, 0, 16000) ?? y,
    r: int(v.r, 1, 600) ?? 10,
    epitaph: cleanText(v.epitaph, 140),
    hardcore: v.hardcore === true,
    items: int(v.items, 0, 100) ?? 0,
  };
}

/** The atlas entry: kingdoms as a bitmask, and the walked regions as a hex bitset over the atlas grid. */
export function cleanAtlas(v) {
  if (!v || typeof v !== 'object') return null;
  const k = int(v.k, 0, 2 ** KINGDOM_COUNT - 1);
  if (k === null || !Array.isArray(v.squares) || v.squares.length > 20000) return null;
  const bits = new Uint8Array((ATLAS_COLS * ATLAS_ROWS) / 8);
  for (const id of v.squares) {
    if (!Number.isInteger(id)) continue;
    const gx = (id >> 8) - ATLAS_X0;
    const gy = (id & 255) - ATLAS_Y0;
    if (gx < 0 || gy < 0 || gx >= ATLAS_COLS || gy >= ATLAS_ROWS) continue;
    const i = gy * ATLAS_COLS + gx;
    bits[i >> 3] |= 1 << (i & 7);
  }
  return { k, s: [...bits].map((b) => b.toString(16).padStart(2, '0')).join('') };
}

/** Answers from the edge cache for {@code seconds}, else builds the JSON and caches it. */
async function cachedJson(request, ctx, seconds, build) {
  const cache = typeof caches !== 'undefined' ? caches.default : null;
  const key = new Request(new URL(request.url).toString(), { method: 'GET' });
  const hit = cache ? await cache.match(key) : null;
  if (hit) return new Response(hit.body, { status: 200, headers: { ...cors(), 'Content-Type': 'application/json', 'Cache-Control': `public, max-age=${seconds}` } });
  const body = JSON.stringify(await build());
  if (cache) {
    ctx?.waitUntil?.(cache.put(key, new Response(body, { headers: { 'Content-Type': 'application/json', 'Cache-Control': `public, max-age=${seconds}` } })));
  }
  return new Response(body, { status: 200, headers: { ...cors(), 'Content-Type': 'application/json', 'Cache-Control': `public, max-age=${seconds}` } });
}

/** Every key under a prefix with its metadata, up to {@code max}. */
async function listAll(env, prefix, max) {
  const out = [];
  let cursor;
  do {
    const page = await env.LIVE.list({ prefix, cursor });
    out.push(...page.keys);
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor && out.length < max);
  return out.slice(0, max);
}

/** The lobby: every live run that joined it and sent lately, as small circles. */
async function lobbyList(request, env, ctx) {
  return cachedJson(request, ctx, 60, async () => {
    const now = Date.now();
    const runs = [];
    for (const k of await listAll(env, 'run:', 5000)) {
      const m = k.metadata;
      if (!m?.lobby || now - m.at > LOBBY_FRESH_MS) continue;
      runs.push({ code: k.name.slice(4), ...m.lobby, online: m.online === true, at: m.at });
    }
    return { runs };
  });
}

/** One write to a shared page under a live code, with the same token claim as the live run. */
async function shareWrite(request, env, code, kind) {
  const auth = TOKEN_RE.exec(request.headers.get('Authorization') || '');
  if (!auth) return text('unauthorised', 401);
  if (!(await withinLimit(env, 'write', request))) return text('too many requests', 429, { 'Retry-After': '60' });
  if (request.method === 'DELETE') {
    const who = await authorise(env, request, code, auth[1]);
    if (who !== 'ok') return text(who === 'forbidden' ? 'forbidden' : 'too many requests', who === 'forbidden' ? 403 : 429);
    for (const k of await listAll(env, `${kind}:${code}`, 1000)) await env.LIVE.delete(k.name);
    return text(null, 204);
  }
  const raw = await readBodyWithin(request, SHARE_BODY_BYTES);
  if (raw === null) return text('too large', 413);
  let body;
  try {
    body = JSON.parse(raw);
  } catch {
    return text('bad request', 400);
  }
  const clean = kind === 'grave' ? cleanGrave(body) : cleanAtlas(body);
  if (!clean) return text('bad request', 400);
  const who = await authorise(env, request, code, auth[1]);
  if (who === 'forbidden') return text('forbidden', 403);
  if (who === 'limited') return text('too many requests', 429, { 'Retry-After': '60' });
  const now = Date.now();
  // A grave per death (by played minute); one atlas entry per run, replaced each time.
  const key = kind === 'grave' ? `grave:${code}:${clean.minutes}` : `atlas:${code}`;
  await env.LIVE.put(key, '', { expirationTtl: SHARED_TTL_SECONDS, metadata: { ...clean, at: now } });
  return text(null, 204);
}

/** The Graveyard: every opted-in tombstone, newest first. No codes: a grave never leads to a live run. */
async function graveList(request, env, ctx) {
  return cachedJson(request, ctx, 300, async () => {
    const graves = (await listAll(env, 'grave:', 2000)).map((k) => k.metadata).filter(Boolean);
    graves.sort((a, b) => b.at - a.at);
    return { graves };
  });
}

/** The atlas of wandering: how many runs have been to each kingdom and map region. */
async function atlasSummary(request, env, ctx) {
  return cachedJson(request, ctx, 3600, async () => {
    const kingdoms = new Array(KINGDOM_COUNT).fill(0);
    const squares = new Array(ATLAS_COLS * ATLAS_ROWS).fill(0);
    let runs = 0;
    for (const k of await listAll(env, 'atlas:', 20000)) {
      const m = k.metadata;
      if (!m) continue;
      runs++;
      for (let i = 0; i < KINGDOM_COUNT; i++) if (m.k & (1 << i)) kingdoms[i]++;
      const s = String(m.s || '');
      for (let b = 0; b < s.length / 2; b++) {
        const byte = parseInt(s.slice(b * 2, b * 2 + 2), 16);
        for (let j = 0; j < 8; j++) if (byte & (1 << j)) squares[b * 8 + j]++;
      }
    }
    return { runs, kingdoms, squares, grid: { x0: ATLAS_X0, y0: ATLAS_Y0, cols: ATLAS_COLS, rows: ATLAS_ROWS } };
  });
}

/** A link card: the page chats unfurl, or its picture. Cached for an hour; a generic card if the run is gone. */
async function card(request, env, ctx, { live, run: runCode, png }) {
  const cache = typeof caches !== 'undefined' ? caches.default : null;
  const key = new Request(new URL(request.url).toString(), { method: 'GET' });
  const hit = cache ? await cache.match(key) : null;
  if (hit) return hit;
  let run = null;
  let liveState = null;
  if (live) {
    const stored = await env.LIVE.get(`run:${live}`, 'json').catch(() => null);
    if (stored) {
      run = await decodeRun(stored.run);
      liveState = { online: stored.online, at: stored.at };
    } else {
      liveState = { online: false, at: 0 };
    }
  } else {
    run = await decodeRun(runCode);
  }
  const headers = { ...cors(), 'Cache-Control': 'public, max-age=3600' };
  let response;
  if (png) {
    response = new Response(await renderCard(run, liveState), { status: 200, headers: { ...headers, 'Content-Type': 'image/png' } });
  } else {
    const origin = new URL(request.url).origin;
    const path = live ? `/c/${live}` : `/r/${runCode}`;
    const said = describe(run, liveState);
    const hour = Math.floor(Date.now() / 3_600_000);
    const html = cardHtml({
      title: said.title,
      line: said.line,
      image: `${origin}${path}/card.png?h=${hour}`,
      target: live ? `${SITE}#/live/${live}` : `${SITE}#/run/${runCode}`,
    });
    response = new Response(html, { status: 200, headers: { ...headers, 'Content-Type': 'text/html; charset=utf-8' } });
  }
  if (cache) ctx?.waitUntil?.(cache.put(key, response.clone()));
  return response;
}

export default {
  async fetch(request, env, ctx) {
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });
    const url = new URL(request.url);
    if (request.method === 'GET') {
      if (url.pathname === '/lobby') return lobbyList(request, env, ctx);
      if (url.pathname === '/g') return graveList(request, env, ctx);
      if (url.pathname === '/a') return atlasSummary(request, env, ctx);
      const lc = LIVE_CARD_ROUTE.exec(url.pathname);
      if (lc) return card(request, env, ctx, { live: lc[1], png: !!lc[2] });
      const rc = RUN_CARD_ROUTE.exec(url.pathname);
      if (rc) return card(request, env, ctx, { run: rc[1], png: !!rc[2] });
    }
    const g = GRAVE_ROUTE.exec(url.pathname) ?? ATLAS_ROUTE.exec(url.pathname);
    if (g) {
      if (!LIVE_CODE_RE.test(g[1])) return text('not found', 404);
      if (request.method !== 'POST' && request.method !== 'DELETE') return text('method not allowed', 405, { Allow: 'POST, DELETE, OPTIONS' });
      return shareWrite(request, env, g[1], url.pathname.startsWith('/g/') ? 'grave' : 'atlas');
    }
    const m = LIVE_ROUTE.exec(url.pathname);
    if (!m || !LIVE_CODE_RE.test(m[1])) return text('not found', 404);
    const code = m[1];
    if (request.method === 'GET') return read(request, env, code, ctx);
    if (request.method === 'POST' || request.method === 'DELETE') return write(request, env, code);
    return text('method not allowed', 405, { Allow: 'GET, POST, DELETE, OPTIONS' });
  },
};
