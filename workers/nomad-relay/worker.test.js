import { describe, expect, it } from 'vitest';
import worker from './worker.js';

/** An in-memory KV with the calls the worker makes. */
function kv() {
  const m = new Map();
  return {
    m,
    async get(k, type) {
      const v = m.get(k);
      return v === undefined ? null : type === 'json' ? JSON.parse(v) : v;
    },
    async put(k, v) {
      m.set(k, v);
    },
    async delete(k) {
      m.delete(k);
    },
  };
}

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
});
