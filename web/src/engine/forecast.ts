import { NomadSim } from './sim';
import { Rng } from './rng';

/** Where a circle might be after each horizon: one point per imagined future. */
export interface Forecast {
  horizons: number[];
  /** [horizon][circle] -> points */
  points: { x: number; y: number }[][][];
}

export const HORIZONS = [60, 180, 360];

/**
 * The Forecast cone. The real future is fixed by the seed, so showing it would spoil the run.
 * Instead this plays the run forward many times with made-up dice, which shows where each
 * circle could plausibly get to, not where it will.
 */
export function forecast(sim: NomadSim, futures = 40, horizons = HORIZONS): Forecast {
  const points = horizons.map(() => sim.circles.map(() => [] as { x: number; y: number }[]));
  const dice = new Rng((sim.state.rngState ^ 0x5bd1e995) + sim.state.minutes);
  const end = horizons[horizons.length - 1];
  for (let f = 0; f < futures; f++) {
    const copy = sim.clone();
    const rng = new Rng(Math.floor(dice.next() * 4294967296));
    let h = 0;
    for (let m = 1; m <= end; m++) {
      copy.step(null, rng);
      if (m === horizons[h]) {
        copy.circles.forEach((c, i) => points[h][i].push({ x: c.x, y: c.y }));
        h++;
      }
    }
  }
  return { horizons, points };
}
