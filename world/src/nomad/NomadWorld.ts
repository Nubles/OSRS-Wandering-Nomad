import { Geography } from "./engine/geography";
import HEIGHTS from "./engine/heightBounds.json";
import MAP from "./engine/mapBounds.json";
import { decodeRun } from "./engine/runcode";
import { CircleState, NomadSim } from "./engine/sim";
import { World } from "./engine/world";

/** Where this app is served, and the Nomad website it belongs to (one folder up). */
export const BASE = process.env.PUBLIC_URL || "";
export const SITE = process.env.REACT_APP_SITE_URL || BASE.replace(/\/world\/?$/, "") + "/";

const RELAY = process.env.REACT_APP_RELAY_URL || "https://nomad-relay.fatelocked.workers.dev";
const LIVE_CODE_RE = /^[A-Za-z0-9_-]{24}$/;
const POLL_MS = 20_000;
const STALE_MS = 10 * 60_000;

/** Played minutes per real second, for seeds and runs. Live runs move at the game's own pace. */
export const SPEEDS = [
    { label: "Pause", perSecond: 0 },
    { label: "1 min/s", perSecond: 1 },
    { label: "10 min/s", perSecond: 10 },
    { label: "1 h/s", perSecond: 60 },
];
export const DEFAULT_SPEED = 1;

export type Mode = "seed" | "run" | "live";

/** One circle as drawn this frame: eased between the last two played minutes. */
export interface DrawnCircle {
    index: number;
    name: string;
    colour: number;
    x: number;
    y: number;
    radius: number;
    yours: boolean;
    sailing: boolean;
}

interface Ground {
    cols: number;
    rows: number;
    data: Uint8Array;
}

/** Gives the ground height at a tile from the 3D world when that part of it is loaded. */
export type GroundSource = (x: number, y: number) => number | undefined;

/**
 * The Nomad side of the 3D world: loads a run from the link (a seed, a run code from the plugin
 * or a live code), plays it, and says where each circle is. It knows nothing about drawing.
 */
export class NomadWorld {
    sim?: NomadSim;
    player: { x: number; y: number } | null = null;
    mode: Mode = "seed";
    speed = DEFAULT_SPEED;
    error?: string;
    liveCode?: string;
    runCode?: string;
    /** When the live run was last sent, and whether its player is still playing. */
    liveAt = 0;
    liveOnline = false;
    /** Bumped whenever something the panel shows changes. */
    version = 0;
    onLoaded?: () => void;

    private geo?: Geography;
    private ground?: Ground;
    private prev: { x: number; y: number; radius: number }[] = [];
    private owed = 0;
    private last = -1;
    private pollTimer = 0;
    private etag: string | null = null;

    async load(params: URLSearchParams): Promise<void> {
        const [geo, ground] = await Promise.all([loadGeography(), loadGround()]);
        this.geo = geo;
        this.ground = ground;
        const live = params.get("live");
        const run = params.get("run");
        const seed = params.get("seed");
        try {
            if (live) {
                if (!LIVE_CODE_RE.test(live))
                    throw new Error(
                        "That live link is not complete. Copy it again from the plugin.",
                    );
                this.liveCode = live;
                this.mode = "live";
                await this.poll(true);
            } else if (run) {
                const saved = await decodeRun(run).catch((e: Error) => {
                    throw new Error(
                        e.message.startsWith("That ")
                            ? e.message
                            : "That run code is not complete. Copy it again from the plugin.",
                    );
                });
                this.runCode = run;
                this.mode = "run";
                this.use(new NomadSim(geo, saved.sim), saved.surface ?? null);
                this.speed = 0;
            }
        } catch (e) {
            this.error = (e as Error).message;
        }
        if (!this.sim) {
            // A link that didn't load still shows something: the seed given, or a new one.
            const s = Number(seed);
            this.mode = "seed";
            this.use(NomadSim.start(geo, seed && Number.isFinite(s) ? s | 0 : randomSeed()), null);
        }
        this.onLoaded?.();
    }

    /** The link parameters that bring this run back. */
    params(): Record<string, string> {
        if (this.mode === "live" && this.liveCode) return { live: this.liveCode };
        if (this.mode === "run" && this.runCode) return { run: this.runCode };
        return this.sim ? { seed: String(this.sim.state.seed) } : {};
    }

    /** This run on the website's 2D map. */
    siteLink(): string {
        if (this.mode === "live" && this.liveCode) return `${SITE}#/live/${this.liveCode}`;
        if (this.mode === "run" && this.runCode) return `${SITE}#/run/${this.runCode}`;
        return `${SITE}#/map${this.sim ? `?seed=${this.sim.state.seed}` : ""}`;
    }

    isLivePlaying(now = Date.now()): boolean {
        return this.mode === "live" && this.liveOnline && now - this.liveAt < STALE_MS;
    }

    /** Plays the minutes owed since the last frame. */
    update(nowMs: number): void {
        const sim = this.sim;
        if (!sim) return;
        const dt = this.last < 0 ? 0 : Math.min((nowMs - this.last) / 1000, 1);
        this.last = nowMs;
        const perSecond =
            this.mode === "live"
                ? this.isLivePlaying()
                    ? 1 / 60
                    : 0
                : SPEEDS[this.speed].perSecond;
        this.owed += dt * perSecond;
        const steps = Math.min(Math.floor(this.owed), 600);
        if (steps > 0) {
            this.owed -= steps;
            for (let i = 0; i < steps; i++) {
                if (i === steps - 1) this.remember();
                sim.step(this.player);
            }
            this.version++;
        }
        if (perSecond === 0) this.owed = 0;
    }

    /** Each circle where it is drawn now, the one you are bound to last so it draws on top. */
    circles(): DrawnCircle[] {
        const sim = this.sim;
        if (!sim) return [];
        const t = Math.min(this.owed, 1);
        const out: DrawnCircle[] = [];
        sim.circles.forEach((c: CircleState, i: number) => {
            const p = this.prev[i];
            // A jump (a voyage docking, a run reloading) is drawn as it is.
            const ease = p && Math.abs(p.x - c.x) + Math.abs(p.y - c.y) < 8;
            out.push({
                index: i,
                name: sim.name(c),
                colour: sim.colour(c),
                x: ease ? p.x + (c.x - p.x) * t : c.x,
                y: ease ? p.y + (c.y - p.y) * t : c.y,
                radius: ease ? p.radius + (c.radius - p.radius) * t : c.radius,
                yours: i === sim.state.bound,
                sailing: !!c.voyage,
            });
        });
        out.sort((a, b) => Number(a.yours) - Number(b.yours));
        return out;
    }

    /** Ground height in tiles (up is positive) from heights.bin, for parts of the world not loaded. */
    groundFar(x: number, y: number): number {
        const g = this.ground;
        if (!g) return 0;
        const col = Math.floor((x - MAP.x0) / HEIGHTS.step);
        const row = Math.floor((MAP.yTop - 1 - y) / HEIGHTS.step);
        if (col < 0 || row < 0 || col >= g.cols || row >= g.rows) return 0;
        return (g.data[row * g.cols + col] * HEIGHTS.scale) / 128;
    }

    setSpeed(speed: number): void {
        this.speed = speed;
        this.version++;
    }

    dispose(): void {
        clearTimeout(this.pollTimer);
    }

    private use(sim: NomadSim, player: { x: number; y: number } | null): void {
        this.sim = sim;
        this.player = player;
        this.prev = [];
        this.owed = 0;
        this.version++;
    }

    private remember(): void {
        this.prev = this.sim!.circles.map((c: CircleState) => ({
            x: c.x,
            y: c.y,
            radius: c.radius,
        }));
    }

    private async poll(first: boolean): Promise<void> {
        const code = this.liveCode!;
        try {
            const r = await fetch(`${RELAY}/n/${code}`, {
                headers: this.etag ? { "If-None-Match": this.etag } : {},
                cache: "no-store",
            });
            if (r.status === 404) {
                if (first) {
                    throw new Error(
                        "This live run hasn't been shared yet. Turn on Share live in the plugin's settings and play for a minute.",
                    );
                }
            } else if (r.ok) {
                this.etag = r.headers.get("ETag");
                const data = (await r.json()) as { run: string; online: boolean; at: number };
                const saved = await decodeRun(data.run);
                const sim = new NomadSim(this.geo!, saved.sim);
                const player = saved.surface ?? null;
                this.liveAt = data.at;
                this.liveOnline = data.online;
                // Catch up on the minutes played since the plugin sent this, up to when it would send again.
                if (this.isLivePlaying()) {
                    const behind = Math.min(Math.floor((Date.now() - data.at) / 60_000), 10);
                    for (let i = 0; i < behind; i++) sim.step(player);
                }
                this.use(sim, player);
            } else if (r.status !== 304 && first) {
                throw new Error(`The live relay answered ${r.status}.`);
            }
        } finally {
            if (!first || this.sim)
                this.pollTimer = window.setTimeout(() => this.poll(false).catch(() => {}), POLL_MS);
        }
    }
}

export function randomSeed(): number {
    return (Math.random() * 2 ** 31) | 0;
}

let geography: Promise<Geography> | undefined;

function loadGeography(): Promise<Geography> {
    geography ??= fetch(`${BASE}/nomad/world.dat`)
        .then((r) => {
            if (!r.ok) throw new Error(`world.dat: ${r.status}`);
            return r.arrayBuffer();
        })
        .then((buf) => World.fromGzip(buf))
        .then((w) => new Geography(w));
    return geography;
}

function loadGround(): Promise<Ground> {
    return fetch(`${BASE}/nomad/heights.bin`)
        .then((r) => {
            if (!r.ok) throw new Error(`heights.bin: ${r.status}`);
            return r.arrayBuffer();
        })
        .then((buf) => ({ cols: HEIGHTS.cols, rows: HEIGHTS.rows, data: new Uint8Array(buf) }));
}
