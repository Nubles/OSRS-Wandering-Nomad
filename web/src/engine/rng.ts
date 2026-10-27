/** Mulberry32, bit for bit the same as the plugin's Rng.java. */
export class Rng {
  constructor(public state: number) {
    this.state = state | 0;
  }

  /** A number in [0, 1). */
  next(): number {
    this.state = (this.state + 0x6d2b79f5) | 0;
    let t = this.state;
    t = Math.imul(t ^ (t >>> 15), t | 1);
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61);
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296;
  }
}
