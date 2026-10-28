import { readFileSync } from 'node:fs';
import { gunzipSync } from 'node:zlib';
import { describe, expect, it } from 'vitest';
import { Geography } from './geography';
import { Rng } from './rng';
import { NomadSim, SimState } from './sim';
import { World } from './world';
import vectors from './vectors.json';

const world = new World(new Uint8Array(gunzipSync(readFileSync(new URL('../../public/world.dat', import.meta.url)))));
const geo = new Geography(world);

interface Case {
  seed: number;
  minutes: number;
  events: { at: number; kind: string; args: (string | number | boolean)[] }[];
  hours: { minute: number; bound: number; rngState: number; circles: number[][] }[];
  log: [number, string][];
  final: SimState;
}

const CASES = (vectors as unknown as { cases: Case[] }).cases;

describe('the TypeScript sim matches the plugin', () => {
  it('draws the same random numbers', () => {
    const rng = new Rng(482913);
    expect([rng.next(), rng.next(), rng.next()]).toEqual([0.036796912318095565, 0.8229757314547896, 0.6929165113251656]);
  });

  it('places the ports and kingdom homes like Java', () => {
    expect(world.land(3241, 3226)).toBe(true); // the River Lum counts as land
    expect(world.land(2990, 3100)).toBe(false);
    expect(geo.regionAt(3222, 3218)?.id).toBe('misthalin');
    expect(geo.regionAt(1680, 3100)?.id).toBe('varlamore');
  });

  for (const c of CASES) {
    it(`replays seed ${c.seed} exactly`, () => {
      const sim = NomadSim.start(geo, c.seed);
      let player: { x: number; y: number } | null = null;
      const log: [number, string][] = [];
      const hours: Case['hours'] = [];
      for (let minute = 0; minute < c.minutes; minute++) {
        for (const e of c.events.filter((ev) => ev.at === minute)) {
          let line: string | null = null;
          if (e.kind === 'player') player = e.args.length ? { x: e.args[0] as number, y: e.args[1] as number } : null;
          else if (e.kind === 'cross') for (let i = 0; i < sim.circles.length && line == null; i++) line = sim.crossTo(i, null);
          else if (e.kind === 'diary') line = sim.setDiaryTiers(e.args[0] as string, e.args[1] as number);
          else if (e.kind === 'sailing') sim.state.sailingUnlocked = e.args[0] as boolean;
          else if (e.kind === 'summon') line = sim.summon(e.args[0] as string);
          else if (e.kind === 'boon') line = sim.useBoon(e.args[0] as number, e.args[1] as number);
          if (line != null) log.push([minute, line]);
        }
        for (const line of sim.step(player)) log.push([minute, line]);
        if (sim.state.minutes % 60 === 0) {
          hours.push({
            minute: sim.state.minutes,
            bound: sim.state.bound,
            rngState: sim.state.rngState,
            circles: sim.circles.map((k) => [k.x, k.y, k.radius, k.heading]),
          });
        }
      }
      expect(log).toEqual(c.log);
      expect(hours).toEqual(c.hours);
      expect(JSON.parse(JSON.stringify(sim.state))).toEqual(c.final);
    });
  }

  it('resumes a run the plugin saved', () => {
    const c = CASES[0];
    const resumed = new NomadSim(geo, structuredClone(c.final));
    const fresh = new NomadSim(geo, structuredClone(c.final));
    for (let i = 0; i < 120; i++) expect(resumed.step(null)).toEqual(fresh.step(null));
    expect(resumed.state).toEqual(fresh.state);
  });
});

describe('run codes', () => {
  it('round-trip a saved run', async () => {
    const { encodeRun, decodeRun } = await import('./runcode');
    const c = CASES[1];
    const code = await encodeRun({ sim: c.final, surface: { x: 3200, y: 3200 } });
    expect(code.startsWith('N1.')).toBe(true);
    expect(code.length).toBeLessThan(2500);
    expect((await decodeRun(code)).sim).toEqual(c.final);
  });

  it('reads a code made the way the plugin makes it', async () => {
    const { decodeRun } = await import('./runcode');
    const json = JSON.stringify({ sim: CASES[0].final });
    const { gzipSync } = await import('node:zlib');
    const code = 'N1.' + gzipSync(Buffer.from(json)).toString('base64url');
    expect((await decodeRun(code)).sim.seed).toBe(1);
  });
});

describe('the forecast', () => {
  it('spreads out and never touches the real run', async () => {
    const { forecast } = await import('./forecast');
    const sim = NomadSim.start(geo, 42);
    const before = JSON.stringify(sim.state);
    const f = forecast(sim, 12);
    expect(JSON.stringify(sim.state)).toBe(before);
    expect(f.points[2][0]).toHaveLength(12);
    const xs = f.points[2][0].map((p) => p.x);
    expect(Math.max(...xs) - Math.min(...xs)).toBeGreaterThan(1);
  });
});
