import { readFileSync } from 'node:fs';
import { gunzipSync } from 'node:zlib';
import { describe, expect, it } from 'vitest';
import { Geography } from './engine/geography';
import { NomadSim } from './engine/sim';
import type { SavedRun, Story } from './engine/runcode';
import { World } from './engine/world';
import { inside, normalise } from './places';
import { chapters, ghostPath, GUESS_ROUNDS, guessRound, guessScore, guessSquare, prophecy, QUIET_HOURS, report, sigil } from './story';

const geo = new Geography(new World(new Uint8Array(gunzipSync(readFileSync(new URL('../public/world.dat', import.meta.url))))));

const STORY: Story = {
  v: 1,
  track: [
    [0, 3222, 3218, 24, 3222, 3218, 0],
    [60, 3230, 3225, 25, 3231, 3224, 0],
    [1200, 3300, 3300, 30, -1, -1, 0],
  ],
  kingdoms: { Misthalin: 900, Asgarnia: 300 },
  moods: { Roaming: 600, Dashing: 600 },
  smallest: 13,
  closest: 'Two tiles from the edge as a fork parted.',
  partings: [[700, 3250, 3260, 1570, 24, 3240, 3240]],
  squares: [(50 << 8) | 50, (51 << 8) | 50, (51 << 8) | 51],
  acts: [
    { start: 0, opened: 'The run begins.' },
    { start: 900, opened: 'A storm opened the second act.', kingdom: 'Asgarnia' },
  ],
  beats: [
    { minute: 30, x: 3222, y: 3218, kind: 'moment', title: 'Waystone risen', line: 'A common stone rose.' },
    { minute: 800, x: -1, y: -1, kind: 'death', line: 'You died at 13h 20m.' },
  ],
  counts: { walked: 1234, forks: 1, deaths: 1 },
  hardcore: false,
};

const RUN = (story?: Story): SavedRun => ({ sim: NomadSim.start(geo, 9).state, story });

describe('the Saga', () => {
  it('turns acts into chapters and folds quiet stretches', () => {
    const list = chapters(STORY, 1500);
    expect(list).toHaveLength(2);
    expect(list[0].title).toBe('Act I');
    expect(list[1].title).toBe('Act II: Asgarnia');
    const lines = list[0].lines.map((l) => l.kind);
    // The waystone, ten quiet hours, the fork's lost half, and the death.
    expect(lines).toEqual(['moment', 'quiet', 'parting', 'death']);
    expect(list[0].lines[1].text).toBe('11 quiet hours pass.');
    expect(QUIET_HOURS).toBeGreaterThan(1);
    expect(list[1].track.map((t) => t[0])).toEqual([1200]);
  });

  it('makes a report card with shares that add up', () => {
    const r = report(RUN(STORY))!;
    expect(r.kingdoms[0]).toMatchObject({ name: 'Misthalin', hours: 15 });
    expect(r.kingdoms.reduce((a, k) => a + k.share, 0)).toBeCloseTo(1);
    expect(r.smallest).toBe(13);
    expect(r.counts.find((c) => c.label === 'Deaths')?.value).toBe(1);
    expect(report(RUN())).toBeNull();
  });
});

describe('the other fork', () => {
  it('drifts on land, the same way every time', () => {
    const a = ghostPath(geo, STORY.partings![0], 1700);
    const b = ghostPath(geo, STORY.partings![0], 1700);
    expect(a).toEqual(b);
    expect(a.length).toBeGreaterThan(10);
    for (const p of a) expect(geo.onLand(p.x, p.y)).toBe(true);
    expect(ghostPath(geo, STORY.partings![0], 600)).toHaveLength(1);
  });
});

describe('the sigil', () => {
  it('is mirrored, framed, and differs by seed', () => {
    const a = sigil(STORY.squares!, 1);
    const b = sigil(STORY.squares!, 2);
    const n = a.size;
    for (let y = 0; y < n; y++) for (let x = 0; x < n; x++) expect(a.pixels[y * n + x]).toBe(a.pixels[y * n + (n - 1 - x)]);
    expect(a.pixels.some((v) => v === 4)).toBe(true);
    expect(a.palette).not.toEqual(b.palette);
  });
});

describe('the prophecy and the guesser', () => {
  it('reads a seed the same way every time, in words only', () => {
    const line = prophecy(geo, 12345);
    expect(line).toBe(prophecy(geo, 12345));
    expect(line).not.toMatch(/\d{3,}/);
    expect(line.length).toBeGreaterThan(20);
  });

  it('gives everyone the same rounds and fair scores', () => {
    expect(guessRound(300, 2)).toEqual(guessRound(300, 2));
    expect(guessRound(300, 2)).not.toEqual(guessRound(301, 2));
    expect(GUESS_ROUNDS).toBe(5);
    expect(guessScore(10, 24)).toBe(1000);
    expect(guessScore(5000, 24)).toBe(0);
    expect(guessScore(300, 24)).toBeGreaterThan(guessScore(600, 24));
    expect(guessSquare(1000)).toBe('🟩');
    expect(guessSquare(0)).toBe('🟥');
  });
});

describe('places', () => {
  it('moves dungeons up to the ground above and drops instances', () => {
    const list = normalise({ Bank: [[3200, 3200], [3200, 9800], [3200, 8000]], 'Fishing spot': [[3100, 3100, 'Shrimp']] });
    expect(list).toHaveLength(3);
    expect(list[1]).toMatchObject({ y: 3400, below: true });
    expect(list[2].label).toBe('Shrimp');
    expect(inside(list, { x: 3200, y: 3210, radius: 20 }).map((p) => p.y)).toEqual([3200]);
  });
});
