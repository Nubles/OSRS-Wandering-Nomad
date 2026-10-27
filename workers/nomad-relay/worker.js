/**
 * Nomad UIM live relay. The plugin publishes a run under a random live code
 * (POST /n/<code>, with a private write token); the website follows it
 * (GET /n/<code>). The run is the same run code the plugin puts in links
 * ("N1." + gzipped JSON), so the relay never parses game data.
 *
 * Built on the pattern of Fate Locked's fate-relay: the id is checked against
 * the exact shape its client makes, writes are rate-limited per IP before any
 * KV access, and the first write claims a code by storing a hash of its token.
 */

export const LIVE_ROUTE = /^\/n\/([^/]*)$/;
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
  await env.LIVE.put(`run:${code}`, JSON.stringify(stored), { expirationTtl: RUN_TTL_SECONDS });
  return text(JSON.stringify({ version: stored.version }), 200, { 'Content-Type': 'application/json' });
}

export default {
  async fetch(request, env, ctx) {
    if (request.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors() });
    const url = new URL(request.url);
    const m = LIVE_ROUTE.exec(url.pathname);
    if (!m || !LIVE_CODE_RE.test(m[1])) return text('not found', 404);
    const code = m[1];
    if (request.method === 'GET') return read(request, env, code, ctx);
    if (request.method === 'POST' || request.method === 'DELETE') return write(request, env, code);
    return text('method not allowed', 405, { Allow: 'GET, POST, DELETE, OPTIONS' });
  },
};
