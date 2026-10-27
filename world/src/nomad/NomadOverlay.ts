import { mat4, vec3 } from "gl-matrix";
import PicoGL, { App, DrawCall, Program, VertexArray, VertexBuffer } from "picogl";

import { DrawnCircle, GroundSource, NomadWorld, TRAIL_MINUTES } from "./NomadWorld";
import { VOLCANO } from "./engine/geography";

/** What a vertex belongs to, which picks its animation in the fragment shader. */
const PLAIN = 0;
const CURTAIN = 1;
const BAND = 2;
const FIRE = 3;
const FLOW = 4;

const VERT = `#version 300 es
precision highp float;

uniform mat4 u_viewProj;
uniform vec2 u_cameraPos;
uniform float u_renderDistance;

layout(location = 0) in vec3 a_pos;
layout(location = 1) in vec4 a_color;
// x: distance along the shape in tiles, y: height above the ground in tiles,
// z: the kind of shape, w: the shape's full height in tiles.
layout(location = 2) in vec4 a_fx;

out vec4 v_color;
out vec4 v_fx;
out float v_fade;

void main() {
    // Fade out where the world itself stops drawing, so nothing hangs over the void.
    vec2 d = abs(a_pos.xz - u_cameraPos);
    float edge = max(d.x, d.y);
    v_fade = 1.0 - smoothstep(u_renderDistance - 24.0, u_renderDistance, edge);
    v_color = a_color;
    v_fx = a_fx;
    gl_Position = u_viewProj * vec4(a_pos, 1.0);
}
`;

const FRAG = `#version 300 es
precision highp float;

uniform float u_time;

in vec4 v_color;
in vec4 v_fx;
in float v_fade;

layout(location = 0) out vec4 fragColor;
// The world writes what is under the mouse here; a zero alpha leaves it as it was.
layout(location = 1) out vec4 interactId;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float line(float d, float w) {
    return smoothstep(w, w * 0.4, abs(d));
}

// A rune-like glyph in a box from -1 to 1: a stem and up to three branches, picked by the seed.
float glyph(vec2 q, float seed) {
    float w = 0.16;
    float g = line(q.x, w) * step(abs(q.y), 1.0);
    float b1 = step(0.5, fract(seed * 7.13));
    float b2 = step(0.5, fract(seed * 13.7));
    float b3 = step(0.6, fract(seed * 3.31));
    float right = step(0.0, q.x) * step(q.x, 0.75);
    float left = step(q.x, 0.0) * step(-0.75, q.x);
    g = max(g, b1 * right * line(q.y - (0.75 - q.x * 0.9), w));
    g = max(g, b2 * right * line(q.y - (0.05 - q.x * 0.9), w));
    g = max(g, b3 * left * line(q.y - (0.5 + q.x * 0.9), w));
    g = max(g, (1.0 - b1) * (1.0 - b2) * step(abs(q.x), 0.7) * line(q.y - 0.55 + abs(q.x) * 0.8, w));
    return g;
}

void main() {
    float kind = v_fx.z;
    float u = v_fx.x;
    float h = v_fx.y;
    float height = max(v_fx.w, 0.001);
    float t = u_time;
    float a = v_color.a;
    vec3 rgb = v_color.rgb;

    if (kind > 0.5 && kind < 1.5) {
        // The curtain: light streaks drifting sideways, runes rising, sparks floating up.
        float fall = clamp(1.0 - h / height, 0.0, 1.0);
        float streak = 0.6 + 0.4 * sin(u * 0.9 + t * 1.1) * sin(u * 0.37 - t * 0.6);
        float base = 0.42 * a * fall * fall * streak;

        vec2 cell = vec2(1.3, 1.1);
        vec2 g = vec2(u, h - t * 0.35) / cell;
        vec2 id = floor(g);
        float rnd = hash(id);
        float rune = 0.0;
        if (rnd > 0.84) {
            vec2 q = (fract(g) - 0.5) / vec2(0.3, 0.38);
            float twinkle = 0.55 + 0.45 * sin(t * 2.3 + rnd * 40.0);
            rune = glyph(q, rnd) * twinkle;
        }

        vec2 sg = vec2(u * 1.6, h * 1.4 - t * 1.1);
        vec2 sid = floor(sg);
        float spark = 0.0;
        if (hash(sid + 3.7) > 0.93) {
            vec2 off = vec2(hash(sid + 1.1), hash(sid + 2.2)) * 0.6 + 0.2;
            spark = smoothstep(0.16, 0.0, length(fract(sg) - off));
        }

        float glow = (rune * 0.85 + spark * 0.9) * a * smoothstep(0.0, 0.25, fall);
        rgb = mix(rgb, vec3(1.0), 0.35 * (rune + spark));
        a = base + glow;
    } else if (kind > 1.5 && kind < 2.5) {
        // The band at the ground: a slow pulse running round the circle.
        a *= 0.75 + 0.35 * sin(u * 0.45 - t * 2.4);
    } else if (kind > 2.5 && kind < 3.5) {
        // Fire and light pillars: flickering, brightest at the base, embers racing up.
        float fall = clamp(1.0 - h / height, 0.0, 1.0);
        float flicker = 0.7 + 0.3 * sin(h * 1.3 - t * 7.0 + u * 2.0);
        vec2 eg = vec2(u * 3.0, h * 0.8 - t * 3.0);
        vec2 eid = floor(eg);
        float ember = 0.0;
        if (hash(eid) > 0.86) {
            ember = smoothstep(0.22, 0.0, length(fract(eg) - 0.5));
        }
        rgb = mix(rgb, vec3(1.0, 0.95, 0.7), 0.5 * fall * fall);
        a = a * pow(fall, 0.8) * flicker + ember * a * fall;
    } else if (kind > 3.5) {
        // Bridges and sea trails: light flowing along them.
        a *= 0.55 + 0.45 * sin(u * 1.8 - t * 4.0);
    }

    fragColor = vec4(rgb, clamp(a, 0.0, 1.0) * v_fade);
    interactId = vec4(0.0);
}
`;

const MAX_VERTICES = 120_000;

/** Tiles above the ground. */
const BAND_LOW = -0.35;
const BAND_HIGH = 0.3;
const CURTAIN_YOURS = 6;
const CURTAIN_OTHER = 3.5;
const BEAM = 10;
/** The ground glow's width, and how far above the ground it floats so it isn't hidden. */
const GLOW = 3;
const LIFT = 0.08;
/** How long a pulse's shockwave takes to cross the circle, in ms. */
const SHOCK_MS = 2500;
/** The volcano's pillar of fire while Main covers it. */
const ERUPTION = 60;

export interface CircleLabel {
    name: string;
    colour: number;
    yours: boolean;
    /** Where the label stands, in world space. */
    pos: vec3;
}

type Rgb = [number, number, number];

const rgbOf = (colour: number): Rgb => [
    ((colour >> 16) & 255) / 255,
    ((colour >> 8) & 255) / 255,
    (colour & 255) / 255,
];

/**
 * Draws the Nomad circles into the 3D world: walls that follow the ground, with runes and sparks
 * rising up them; shockwaves when a circle pulses; bridges of light where you can cross; trails
 * behind circles at sea; and the volcano erupting while Main covers it.
 */
export class NomadOverlay {
    labels: CircleLabel[] = [];

    private program: Program;
    private positions: VertexBuffer;
    private colours: VertexBuffer;
    private effects: VertexBuffer;
    private vertexArray: VertexArray;
    private drawCall: DrawCall;
    private pos = new Float32Array(MAX_VERTICES * 3);
    private col = new Float32Array(MAX_VERTICES * 4);
    private fx = new Float32Array(MAX_VERTICES * 4);
    private count = 0;
    private cameraPos = new Float32Array(2);

    constructor(
        private readonly app: App,
        private readonly nomad: NomadWorld,
        private readonly groundNear: GroundSource,
    ) {
        this.program = app.createProgram(VERT, FRAG);
        this.positions = app.createVertexBuffer(
            PicoGL.FLOAT,
            3,
            this.pos.length,
            PicoGL.DYNAMIC_DRAW,
        );
        this.colours = app.createVertexBuffer(
            PicoGL.FLOAT,
            4,
            this.col.length,
            PicoGL.DYNAMIC_DRAW,
        );
        this.effects = app.createVertexBuffer(PicoGL.FLOAT, 4, this.fx.length, PicoGL.DYNAMIC_DRAW);
        this.vertexArray = app
            .createVertexArray()
            .vertexAttributeBuffer(0, this.positions)
            .vertexAttributeBuffer(1, this.colours)
            .vertexAttributeBuffer(2, this.effects);
        this.drawCall = app.createDrawCall(this.program, this.vertexArray);
    }

    /** Height of the ground in viewer units (up is negative y, one unit a tile). */
    ground(x: number, y: number): number {
        const near = this.groundNear(x, y);
        return -(near ?? this.nomad.groundFar(x, y));
    }

    draw(
        viewProj: mat4,
        cameraX: number,
        cameraZ: number,
        renderDistance: number,
        cullBackFace: boolean,
        timeMs: number,
    ): void {
        this.build(cameraX, cameraZ, timeMs);
        if (this.count === 0) return;
        this.positions.data(this.pos.subarray(0, this.count * 3));
        this.colours.data(this.col.subarray(0, this.count * 4));
        this.effects.data(this.fx.subarray(0, this.count * 4));
        this.cameraPos[0] = cameraX;
        this.cameraPos[1] = cameraZ;

        const app = this.app;
        app.disable(PicoGL.CULL_FACE);
        app.depthMask(false);
        app.enable(PicoGL.BLEND);
        app.blendFunc(PicoGL.SRC_ALPHA, PicoGL.ONE);
        this.drawCall
            .uniform("u_viewProj", viewProj)
            .uniform("u_cameraPos", this.cameraPos)
            .uniform("u_renderDistance", renderDistance)
            .uniform("u_time", (timeMs / 1000) % 3600)
            .drawRanges([0, this.count])
            .draw();
        app.blendFunc(PicoGL.SRC_ALPHA, PicoGL.ONE_MINUS_SRC_ALPHA);
        app.depthMask(true);
        if (cullBackFace) app.enable(PicoGL.CULL_FACE);
    }

    private build(cameraX: number, cameraZ: number, timeMs: number): void {
        this.count = 0;
        this.labels = [];
        const circles = this.nomad.circles();

        // A pulse flashes its circle's wall and sends a shockwave across the ground.
        const flash = new Map<number, number>();
        for (const p of this.nomad.recentPulses(timeMs)) {
            const k = (timeMs - p.at) / SHOCK_MS;
            const c = circles.find((d) => d.index === p.index);
            if (!c || k < 0 || k >= 1) continue;
            flash.set(p.index, Math.max(flash.get(p.index) ?? 0, 1 - k));
            this.shockwave(c, k);
        }

        for (const [index, trail] of this.nomad.trails) {
            const c = circles.find((d) => d.index === index);
            if (c) this.trail(trail, rgbOf(c.colour), this.nomad.sim?.state.minutes ?? 0);
        }
        for (const c of circles) {
            this.circle(c, cameraX, cameraZ, flash.get(c.index) ?? 0);
        }
        for (const [a, b] of this.nomad.crossings(circles)) {
            this.bridge(a, b);
        }
        if (this.nomad.volcanoOpen()) this.eruption();
        const p = this.nomad.player;
        if (p) this.pillar(p.x + 0.5, p.y + 0.5, BEAM, 0.35, [1, 0.92, 0.6], 0.85);
    }

    private circle(c: DrawnCircle, cameraX: number, cameraZ: number, flash: number): void {
        const [r, g, b] = rgbOf(c.colour);
        const strength = (c.yours ? 1 : 0.6) * (1 + 1.5 * flash);
        const curtain = (c.yours ? CURTAIN_YOURS : CURTAIN_OTHER) * (1 + 0.6 * flash);
        const segments = Math.max(64, Math.min(320, Math.ceil(2 * Math.PI * c.radius)));
        if (this.count + (segments + 1) * 18 > MAX_VERTICES) return;

        const inner = Math.max(0, c.radius - GLOW);
        const cx = c.x + 0.5;
        const cz = c.y + 0.5;
        const step = (2 * Math.PI * c.radius) / segments;
        let px = 0;
        let pz = 0;
        let pg = 0;
        let qx = 0;
        let qz = 0;
        let qg = 0;
        for (let i = 0; i <= segments; i++) {
            const angle = (i / segments) * Math.PI * 2;
            const cos = Math.cos(angle);
            const sin = Math.sin(angle);
            const x = cx + cos * c.radius;
            const z = cz + sin * c.radius;
            const gy = this.ground(x, z);
            const ix = cx + cos * inner;
            const iz = cz + sin * inner;
            const ig = this.ground(ix, iz);
            if (i > 0) {
                const u0 = (i - 1) * step;
                const u1 = i * step;
                // The band where the circle meets the ground.
                this.wall(
                    px,
                    pz,
                    x,
                    z,
                    pg - BAND_LOW,
                    gy - BAND_LOW,
                    pg - BAND_HIGH,
                    gy - BAND_HIGH,
                    [r, g, b],
                    0.9 * strength,
                    0.9 * strength,
                    u0,
                    u1,
                    0,
                    0,
                    BAND,
                    1,
                );
                // The curtain above it; the shader draws its fade, runes and sparks.
                this.wall(
                    px,
                    pz,
                    x,
                    z,
                    pg - BAND_HIGH,
                    gy - BAND_HIGH,
                    pg - curtain,
                    gy - curtain,
                    [r, g, b],
                    strength,
                    strength,
                    u0,
                    u1,
                    0,
                    curtain,
                    CURTAIN,
                    curtain,
                );
                // A glow on the ground just inside the edge, so the circle reads from above too.
                const ga = 0.5 * strength;
                this.vertex(px, pg - LIFT, pz, r, g, b, ga, u0, 0, PLAIN, 1);
                this.vertex(x, gy - LIFT, z, r, g, b, ga, u1, 0, PLAIN, 1);
                this.vertex(ix, ig - LIFT, iz, r, g, b, 0, u1, 0, PLAIN, 1);
                this.vertex(px, pg - LIFT, pz, r, g, b, ga, u0, 0, PLAIN, 1);
                this.vertex(ix, ig - LIFT, iz, r, g, b, 0, u1, 0, PLAIN, 1);
                this.vertex(qx, qg - LIFT, qz, r, g, b, 0, u0, 0, PLAIN, 1);
            }
            px = x;
            pz = z;
            pg = gy;
            qx = ix;
            qz = iz;
            qg = ig;
        }

        // The label stands on the part of the wall nearest the camera, or straight ahead from inside.
        let dx = cameraX - cx;
        let dz = cameraZ - cz;
        const d = Math.sqrt(dx * dx + dz * dz) || 1;
        dx /= d;
        dz /= d;
        const lx = cx + dx * c.radius;
        const lz = cz + dz * c.radius;
        this.labels.push({
            name: c.name,
            colour: c.colour,
            yours: c.yours,
            pos: vec3.fromValues(lx, this.ground(lx, lz) - curtain - 0.5, lz),
        });
    }

    /** A bright ring racing out across the ground from the centre, k from 0 to 1. */
    private shockwave(c: DrawnCircle, k: number): void {
        const [r, g, b] = rgbOf(c.colour);
        const ease = 1 - (1 - k) * (1 - k);
        const outer = (c.radius + 6) * ease + 1;
        const width = 2 + 3 * k;
        const innerR = Math.max(0, outer - width);
        const alpha = 0.9 * (1 - k);
        const cx = c.x + 0.5;
        const cz = c.y + 0.5;
        const segments = Math.max(48, Math.min(240, Math.ceil(2 * Math.PI * outer)));
        if (this.count + segments * 12 > MAX_VERTICES) return;
        for (let i = 0; i < segments; i++) {
            const a0 = (i / segments) * Math.PI * 2;
            const a1 = ((i + 1) / segments) * Math.PI * 2;
            const o0x = cx + Math.cos(a0) * outer;
            const o0z = cz + Math.sin(a0) * outer;
            const o1x = cx + Math.cos(a1) * outer;
            const o1z = cz + Math.sin(a1) * outer;
            const i0x = cx + Math.cos(a0) * innerR;
            const i0z = cz + Math.sin(a0) * innerR;
            const i1x = cx + Math.cos(a1) * innerR;
            const i1z = cz + Math.sin(a1) * innerR;
            const o0 = this.ground(o0x, o0z) - 0.15;
            const o1 = this.ground(o1x, o1z) - 0.15;
            const n0 = this.ground(i0x, i0z) - 0.15;
            const n1 = this.ground(i1x, i1z) - 0.15;
            this.vertex(o0x, o0, o0z, r, g, b, alpha, 0, 0, PLAIN, 1);
            this.vertex(o1x, o1, o1z, r, g, b, alpha, 0, 0, PLAIN, 1);
            this.vertex(i1x, n1, i1z, r, g, b, 0, 0, 0, PLAIN, 1);
            this.vertex(o0x, o0, o0z, r, g, b, alpha, 0, 0, PLAIN, 1);
            this.vertex(i1x, n1, i1z, r, g, b, 0, 0, 0, PLAIN, 1);
            this.vertex(i0x, n0, i0z, r, g, b, 0, 0, 0, PLAIN, 1);
            // A low wall of light rides the front of the wave.
            this.wall(
                o0x,
                o0z,
                o1x,
                o1z,
                o0,
                o1,
                o0 - 1.5,
                o1 - 1.5,
                [r, g, b],
                alpha,
                0,
                0,
                0,
                0,
                0,
                PLAIN,
                1,
            );
        }
    }

    /**
     * An arch of light over the lens where your circle and another overlap: the crossing.
     * It springs from the two points where the walls meet and carries both circles' colours.
     */
    private bridge(a: DrawnCircle, b: DrawnCircle): void {
        const ax = a.x + 0.5;
        const az = a.y + 0.5;
        const bx = b.x + 0.5;
        const bz = b.y + 0.5;
        const d = Math.hypot(bx - ax, bz - az);
        if (d < 0.01 || d < Math.abs(a.radius - b.radius)) return; // one inside the other
        const along = (d * d + a.radius * a.radius - b.radius * b.radius) / (2 * d);
        const half = Math.sqrt(Math.max(0, a.radius * a.radius - along * along));
        const ux = (bx - ax) / d;
        const uz = (bz - az) / d;
        const mx = ax + ux * along;
        const mz = az + uz * along;
        const p1x = mx - uz * half;
        const p1z = mz + ux * half;
        const p2x = mx + uz * half;
        const p2z = mz - ux * half;
        const chord = 2 * half;
        const rise = Math.min(14, 3 + chord * 0.45);
        const [r1, g1, b1] = rgbOf(a.colour);
        const [r2, g2, b2] = rgbOf(b.colour);
        const n = Math.max(16, Math.min(96, Math.ceil(chord)));
        const thick = 0.8;
        if (this.count + n * 12 + 24 > MAX_VERTICES) return;
        let lx = p1x;
        let lz = p1z;
        let ly = this.ground(p1x, p1z);
        for (let i = 1; i <= n; i++) {
            const s = i / n;
            const x = p1x + (p2x - p1x) * s;
            const z = p1z + (p2z - p1z) * s;
            const base = this.ground(p1x, p1z) * (1 - s) + this.ground(p2x, p2z) * s;
            const y = Math.min(base, this.ground(x, z)) - rise * Math.sin(Math.PI * s);
            const k0 = (i - 1) / n;
            const mix = (k: number): Rgb => [
                r1 + (r2 - r1) * k,
                g1 + (g2 - g1) * k,
                b1 + (b2 - b1) * k,
            ];
            const c0 = mix(k0);
            const c1 = mix(s);
            const u0 = k0 * chord;
            const u1 = s * chord;
            // A ribbon seen from the side and one seen from above, so it shows from any angle.
            this.vertex(lx, ly + thick, lz, c0[0], c0[1], c0[2], 0.8, u0, 0, FLOW, 1);
            this.vertex(x, y + thick, z, c1[0], c1[1], c1[2], 0.8, u1, 0, FLOW, 1);
            this.vertex(x, y - thick, z, c1[0], c1[1], c1[2], 0.8, u1, 0, FLOW, 1);
            this.vertex(lx, ly + thick, lz, c0[0], c0[1], c0[2], 0.8, u0, 0, FLOW, 1);
            this.vertex(x, y - thick, z, c1[0], c1[1], c1[2], 0.8, u1, 0, FLOW, 1);
            this.vertex(lx, ly - thick, lz, c0[0], c0[1], c0[2], 0.8, u0, 0, FLOW, 1);
            const wx = ux * thick;
            const wz = uz * thick;
            this.vertex(lx - wx, ly, lz - wz, c0[0], c0[1], c0[2], 0.6, u0, 0, FLOW, 1);
            this.vertex(x - wx, y, z - wz, c1[0], c1[1], c1[2], 0.6, u1, 0, FLOW, 1);
            this.vertex(x + wx, y, z + wz, c1[0], c1[1], c1[2], 0.6, u1, 0, FLOW, 1);
            this.vertex(lx - wx, ly, lz - wz, c0[0], c0[1], c0[2], 0.6, u0, 0, FLOW, 1);
            this.vertex(x + wx, y, z + wz, c1[0], c1[1], c1[2], 0.6, u1, 0, FLOW, 1);
            this.vertex(lx + wx, ly, lz + wz, c0[0], c0[1], c0[2], 0.6, u0, 0, FLOW, 1);
            lx = x;
            ly = y;
            lz = z;
        }
        // Pillars where the arch meets the ground.
        this.pillar(p1x, p1z, 4, 0.5, [r1, g1, b1], 0.7);
        this.pillar(p2x, p2z, 4, 0.5, [r2, g2, b2], 0.7);
    }

    /** A wake of light on the water behind a circle at sea, fading with age. */
    private trail(points: { x: number; y: number; minute: number }[], rgb: Rgb, now: number): void {
        if (points.length < 2 || this.count + points.length * 6 > MAX_VERTICES) return;
        const [r, g, b] = rgb;
        let u = 0;
        for (let i = 1; i < points.length; i++) {
            const p0 = points[i - 1];
            const p1 = points[i];
            const dx = p1.x - p0.x;
            const dz = p1.y - p0.y;
            const len = Math.hypot(dx, dz);
            if (len < 0.01) continue;
            const wx = (-dz / len) * 1.6;
            const wz = (dx / len) * 1.6;
            const a0 = 0.7 * Math.max(0, 1 - (now - p0.minute) / TRAIL_MINUTES);
            const a1 = 0.7 * Math.max(0, 1 - (now - p1.minute) / TRAIL_MINUTES);
            const x0 = p0.x + 0.5;
            const z0 = p0.y + 0.5;
            const x1 = p1.x + 0.5;
            const z1 = p1.y + 0.5;
            const y0 = this.ground(x0, z0) - 0.2;
            const y1 = this.ground(x1, z1) - 0.2;
            this.vertex(x0 - wx, y0, z0 - wz, r, g, b, a0, u, 0, FLOW, 1);
            this.vertex(x1 - wx, y1, z1 - wz, r, g, b, a1, u + len, 0, FLOW, 1);
            this.vertex(x1 + wx, y1, z1 + wz, r, g, b, a1, u + len, 0, FLOW, 1);
            this.vertex(x0 - wx, y0, z0 - wz, r, g, b, a0, u, 0, FLOW, 1);
            this.vertex(x1 + wx, y1, z1 + wz, r, g, b, a1, u + len, 0, FLOW, 1);
            this.vertex(x0 + wx, y0, z0 + wz, r, g, b, a0, u, 0, FLOW, 1);
            u += len;
        }
    }

    /** The volcano over TzHaar erupting in fire while Main covers it, the way into the Inferno. */
    private eruption(): void {
        const x = VOLCANO.x + 0.5;
        const z = VOLCANO.y + 0.5;
        this.pillar(x, z, ERUPTION, 3, [1, 0.42, 0.08], 0.95);
        this.pillar(x, z, ERUPTION * 0.6, 1.2, [1, 0.8, 0.3], 0.9);
        const segments = 64;
        for (let i = 0; i < segments; i++) {
            const a0 = (i / segments) * Math.PI * 2;
            const a1 = ((i + 1) / segments) * Math.PI * 2;
            const radius = 7;
            const x0 = x + Math.cos(a0) * radius;
            const z0 = z + Math.sin(a0) * radius;
            const x1 = x + Math.cos(a1) * radius;
            const z1 = z + Math.sin(a1) * radius;
            const g0 = this.ground(x0, z0);
            const g1 = this.ground(x1, z1);
            this.wall(
                x0,
                z0,
                x1,
                z1,
                g0 + 0.3,
                g1 + 0.3,
                g0 - 5,
                g1 - 5,
                [1, 0.35, 0.05],
                0.8,
                0.8,
                i * 0.7,
                (i + 1) * 0.7,
                0,
                5,
                FIRE,
                5,
            );
        }
    }

    /** A crossed pair of upright quads: a pillar of light seen the same from every side. */
    private pillar(x: number, z: number, height: number, w: number, rgb: Rgb, alpha: number): void {
        const gy = this.ground(x, z);
        this.wall(
            x - w,
            z,
            x + w,
            z,
            gy + 0.2,
            gy + 0.2,
            gy - height,
            gy - height,
            rgb,
            alpha,
            alpha,
            0,
            1,
            0,
            height,
            FIRE,
            height,
        );
        this.wall(
            x,
            z - w,
            x,
            z + w,
            gy + 0.2,
            gy + 0.2,
            gy - height,
            gy - height,
            rgb,
            alpha,
            alpha,
            0.5,
            1.5,
            0,
            height,
            FIRE,
            height,
        );
    }

    /**
     * An upright quad from (x0,z0) to (x1,z1), with its bottom and top heights at each end. Its
     * effect coordinates run u0 to u1 along it and hBottom to hTop up it.
     */
    private wall(
        x0: number,
        z0: number,
        x1: number,
        z1: number,
        bottom0: number,
        bottom1: number,
        top0: number,
        top1: number,
        rgb: Rgb,
        alphaBottom: number,
        alphaTop: number,
        u0: number,
        u1: number,
        hBottom: number,
        hTop: number,
        kind: number,
        height: number,
    ): void {
        const [r, g, b] = rgb;
        this.vertex(x0, bottom0, z0, r, g, b, alphaBottom, u0, hBottom, kind, height);
        this.vertex(x1, bottom1, z1, r, g, b, alphaBottom, u1, hBottom, kind, height);
        this.vertex(x1, top1, z1, r, g, b, alphaTop, u1, hTop, kind, height);
        this.vertex(x0, bottom0, z0, r, g, b, alphaBottom, u0, hBottom, kind, height);
        this.vertex(x1, top1, z1, r, g, b, alphaTop, u1, hTop, kind, height);
        this.vertex(x0, top0, z0, r, g, b, alphaTop, u0, hTop, kind, height);
    }

    private vertex(
        x: number,
        y: number,
        z: number,
        r: number,
        g: number,
        b: number,
        a: number,
        u: number,
        h: number,
        kind: number,
        height: number,
    ): void {
        if (this.count >= MAX_VERTICES) return;
        const i = this.count++;
        this.pos[i * 3] = x;
        this.pos[i * 3 + 1] = y;
        this.pos[i * 3 + 2] = z;
        this.col[i * 4] = r;
        this.col[i * 4 + 1] = g;
        this.col[i * 4 + 2] = b;
        this.col[i * 4 + 3] = a;
        this.fx[i * 4] = u;
        this.fx[i * 4 + 1] = h;
        this.fx[i * 4 + 2] = kind;
        this.fx[i * 4 + 3] = height;
    }

    delete(): void {
        this.vertexArray.delete();
        this.positions.delete();
        this.colours.delete();
        this.effects.delete();
        this.program.delete();
    }
}
