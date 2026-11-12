import { describe, expect, it } from 'vitest';
import worker from './worker.js';
import { crc32, decodeRun, describe as describeRun } from './card.js';

/** An in-memory KV with the calls the worker makes. */
function kv() {
  const m = new Map();
  const meta = new Map();
  return {
    m,
    meta,
    async get(k, type) {
      const v = m.get(k);
      return v === undefined ? null : type === 'json' ? JSON.parse(v) : v;
    },
    async put(k, v, opts) {
      m.set(k, v);
      if (opts?.metadata) meta.set(k, opts.metadata);
      else meta.delete(k);
    },
    async delete(k) {
      m.delete(k);
      meta.delete(k);
    },
    async list({ prefix = '' } = {}) {
      const keys = [...m.keys()].filter((k) => k.startsWith(prefix)).sort().map((name) => ({ name, metadata: meta.get(name) }));
      return { keys, list_complete: true };
    },
  };
}

/** A real run code: a small run, gzipped, the way the plugin makes one. */
async function runCode(run) {
  const gz = new Uint8Array(
    await new Response(new Blob([JSON.stringify(run)]).stream().pipeThrough(new CompressionStream('gzip'))).arrayBuffer(),
  );
  let s = '';
  for (const b of gz) s += String.fromCharCode(b);
  return 'N1.' + btoa(s).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

const SMALL_RUN = {
  sim: {
    seed: 7,
    minutes: 760,
    bound: 1,
    circles: [
      { x: 3222, y: 3218, radius: 40, mood: 1, heading: 0.5 },
      { regionId: 'misthalin', x: 3250, y: 3300, radius: 60, mood: 0, heading: 1 },
      { regionId: 'desert', x: 3300, y: 3000, radius: 70, mood: 0, heading: 1 },
    ],
  },
  surface: { x: 3230, y: 3220 },
  epithet: 'the Seaborn',
};

const get = (env, path, headers = {}) => worker.fetch(new Request(`https://relay.test${path}`, { headers }), env);
const send = (env, path, { method = 'POST', token = TOKEN, body } = {}) =>
  worker.fetch(
    new Request(`https://relay.test${path}`, {
      method,
      headers: token ? { Authorization: `Bearer ${token}` } : {},
      body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
    }),
    env,
  );

const CODE = 'abcdefghijklmnopqrstuvwx';
const TOKEN = 'A'.repeat(43);
const OTHER = 'B'.repeat(43);
const RUN = 'N1.' + 'H4sIAAAAAAAAA'.padEnd(40, 'x');

const call = (env, method, { code = CODE, token, body, headers = {} } = {}) =>
  worker.fetch(
    new Request(`https://relay.test/n/${code}`, {
      method,
      headers: { ...(token ? { Authorization: `Bearer ${token}` } : {}), ...headers },
      body: body === undefined ? undefined : typeof body === 'string' ? body : JSON.stringify(body),
    }),
    env,
  );

describe('nomad relay', () => {
  it('publishes and serves a run', async () => {
    const env = { LIVE: kv() };
    expect((await call(env, 'GET')).status).toBe(404);
    const post = await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } });
    expect(post.status).toBe(200);
    const get = await call(env, 'GET');
    expect(get.status).toBe(200);
    const json = await get.json();
    expect(json).toMatchObject({ run: RUN, online: true });
    expect(get.headers.get('Access-Control-Allow-Origin')).toBe('*');
    const etag = get.headers.get('ETag');
    expect((await call(env, 'GET', { headers: { 'If-None-Match': etag } })).status).toBe(304);
  });

  it('lets only the first token write a code', async () => {
    const env = { LIVE: kv() };
    await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } });
    expect((await call(env, 'POST', { token: OTHER, body: { run: RUN, online: false } })).status).toBe(403);
    expect((await call(env, 'DELETE', { token: OTHER })).status).toBe(403);
    expect((await call(env, 'DELETE', { token: TOKEN })).status).toBe(204);
    expect((await call(env, 'GET')).status).toBe(404);
    // The token's hash is stored, never the token.
    expect([...env.LIVE.m.values()].join()).not.toContain(TOKEN);
  });

  it('versions only go up', async () => {
    const env = { LIVE: kv() };
    const a = await (await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } })).json();
    const b = await (await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } })).json();
    expect(b.version).toBeGreaterThan(a.version);
  });

  it('refuses bad codes, tokens and bodies', async () => {
    const env = { LIVE: kv() };
    expect((await call(env, 'GET', { code: 'short' })).status).toBe(404);
    expect((await call(env, 'POST', { body: { run: RUN, online: true } })).status).toBe(401);
    expect((await call(env, 'POST', { token: 'x', body: { run: RUN, online: true } })).status).toBe(401);
    expect((await call(env, 'POST', { token: TOKEN, body: 'not json' })).status).toBe(400);
    expect((await call(env, 'POST', { token: TOKEN, body: { run: 'hello', online: true } })).status).toBe(400);
    expect((await call(env, 'POST', { token: TOKEN, body: { run: RUN } })).status).toBe(400);
    const big = { run: 'N1.' + 'x'.repeat(20000), online: true };
    expect((await call(env, 'POST', { token: TOKEN, body: big })).status).toBe(413);
    expect(env.LIVE.m.size).toBe(0);
  });

  it('applies the rate limits', async () => {
    const no = { limit: async () => ({ success: false }) };
    const env = { LIVE: kv(), WRITE_LIMITER: no };
    expect((await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } })).status).toBe(429);
    const env2 = { LIVE: kv(), CLAIM_LIMITER: no };
    expect((await call(env2, 'POST', { token: TOKEN, body: { run: RUN, online: true } })).status).toBe(429);
    expect(env2.LIVE.m.size).toBe(0);
  });

  it('lists live runs that joined the lobby, and only those', async () => {
    const env = { LIVE: kv() };
    const lobby = { name: 'Zezima<b>!', x: 3222, y: 3218, r: 40, mood: 'Dashing' };
    await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true, lobby } });
    await call(env, 'POST', { code: 'z'.repeat(24), token: OTHER, body: { run: RUN, online: true } });
    const res = await get(env, '/lobby');
    expect(res.status).toBe(200);
    const { runs } = await res.json();
    expect(runs).toHaveLength(1);
    expect(runs[0]).toMatchObject({ code: CODE, name: 'Zezimab', x: 3222, y: 3218, r: 40, mood: 'Dashing', online: true });
    // Leaving the lobby is the next send without it.
    await call(env, 'POST', { token: TOKEN, body: { run: RUN, online: true } });
    expect((await (await get(env, '/lobby')).json()).runs).toHaveLength(0);
  });

  it('keeps tombstones without their codes, and lets their owner take them back', async () => {
    const env = { LIVE: kv() };
    const grave = { name: 'Nomad', x: 3200, y: 3200, minutes: 600, seed: 9, kingdom: 'Misthalin', circle: 'Main', cx: 3210, cy: 3205, r: 30, epitaph: 'Wandered 10h 00m. <script>', hardcore: true, items: 24 };
    expect((await send(env, `/g/${CODE}`, { body: grave })).status).toBe(204);
    expect((await send(env, `/g/${CODE}`, { body: { x: 'nope' } })).status).toBe(400);
    expect((await send(env, `/g/${CODE}`, { token: null, body: grave })).status).toBe(401);
    const { graves } = await (await get(env, '/g')).json();
    expect(graves).toHaveLength(1);
    expect(graves[0]).toMatchObject({ name: 'Nomad', x: 3200, minutes: 600, hardcore: true, items: 24 });
    expect(graves[0].epitaph).not.toContain('<');
    expect(JSON.stringify(graves)).not.toContain(CODE);
    expect((await send(env, `/g/${CODE}`, { method: 'DELETE', token: OTHER })).status).toBe(403);
    expect((await send(env, `/g/${CODE}`, { method: 'DELETE' })).status).toBe(204);
    expect([...env.LIVE.m.keys()].filter((k) => k.startsWith('grave:'))).toHaveLength(0);
  });

  it('sums the atlas of wandering', async () => {
    const env = { LIVE: kv() };
    const sq = (rx, ry) => (rx << 8) | ry;
    await send(env, `/a/${CODE}`, { body: { k: 0b1001, squares: [sq(50, 50), sq(51, 50), sq(200, 200)] } });
    await send(env, `/a/${'y'.repeat(24)}`, { token: OTHER, body: { k: 0b1000, squares: [sq(50, 50)] } });
    // A run's newer entry replaces its older one.
    await send(env, `/a/${CODE}`, { body: { k: 0b1001, squares: [sq(50, 50), sq(51, 50)] } });
    const atlas = await (await get(env, '/a')).json();
    expect(atlas.runs).toBe(2);
    expect(atlas.kingdoms[0]).toBe(1);
    expect(atlas.kingdoms[3]).toBe(2);
    const at = (rx, ry) => atlas.squares[(ry - atlas.grid.y0) * atlas.grid.cols + (rx - atlas.grid.x0)];
    expect(at(50, 50)).toBe(2);
    expect(at(51, 50)).toBe(1);
    expect(at(52, 50)).toBe(0);
  });

  it('draws link cards for live runs and run codes', async () => {
    const env = { LIVE: kv() };
    const code = await runCode(SMALL_RUN);
    expect((await decodeRun(code)).sim.minutes).toBe(760);
    await call(env, 'POST', { token: TOKEN, body: { run: code, online: true } });
    const page = await get(env, `/c/${CODE}`);
    expect(page.status).toBe(200);
    const html = await page.text();
    expect(html).toContain('og:image');
    expect(html).toContain(`/c/${CODE}/card.png`);
    expect(html).toContain(`#/live/${CODE}`);
    expect(html).toContain('the Seaborn');
    const png = new Uint8Array(await (await get(env, `/c/${CODE}/card.png`)).arrayBuffer());
    expect([...png.slice(1, 4)].map((c) => String.fromCharCode(c)).join('')).toBe('PNG');
    // Every chunk's CRC checks out.
    let at = 8;
    while (at < png.length) {
      const len = new DataView(png.buffer).getUint32(at);
      const crc = new DataView(png.buffer).getUint32(at + 8 + len);
      expect(crc).toBe(crc32(png.subarray(at + 4, at + 8 + len)));
      at += 12 + len;
    }
    const runPage = await (await get(env, `/r/${code}`)).text();
    expect(runPage).toContain('12h 40m played');
    // A gone run still gets a generic card.
    const gone = await (await get(env, `/c/${'q'.repeat(24)}`)).text();
    expect(gone).toContain('Nomad UIM');
    expect(describeRun(null, { online: false, at: 0 }).badge).toBe('LIVE');
  });
});
