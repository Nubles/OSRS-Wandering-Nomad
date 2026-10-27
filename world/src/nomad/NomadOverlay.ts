import { mat4, vec3 } from "gl-matrix";
import PicoGL, { App, DrawCall, Program, VertexArray, VertexBuffer } from "picogl";

import { DrawnCircle, GroundSource, NomadWorld } from "./NomadWorld";

const VERT = `#version 300 es
precision highp float;

uniform mat4 u_viewProj;
uniform vec2 u_cameraPos;
uniform float u_renderDistance;

layout(location = 0) in vec3 a_pos;
layout(location = 1) in vec4 a_color;

out vec4 v_color;

void main() {
    // Fade out where the world itself stops drawing, so walls never hang over the void.
    vec2 d = abs(a_pos.xz - u_cameraPos);
    float edge = max(d.x, d.y);
    float fade = 1.0 - smoothstep(u_renderDistance - 24.0, u_renderDistance, edge);
    v_color = vec4(a_color.rgb, a_color.a * fade);
    gl_Position = u_viewProj * vec4(a_pos, 1.0);
}
`;

const FRAG = `#version 300 es
precision highp float;

in vec4 v_color;

layout(location = 0) out vec4 fragColor;
// The world writes what is under the mouse here; a zero alpha leaves it as it was.
layout(location = 1) out vec4 interactId;

void main() {
    fragColor = v_color;
    interactId = vec4(0.0);
}
`;

const MAX_SEGMENTS = 320;
const MAX_VERTICES = 16 * MAX_SEGMENTS * 18 + 64;

/** Tiles above the ground. */
const BAND_LOW = -0.35;
const BAND_HIGH = 0.3;
const CURTAIN_YOURS = 5;
const CURTAIN_OTHER = 3;
const BEAM = 10;
/** The ground glow's width, and how far above the ground it floats so it isn't hidden. */
const GLOW = 3;
const LIFT = 0.08;

export interface CircleLabel {
    name: string;
    colour: number;
    yours: boolean;
    /** Where the label stands, in world space. */
    pos: vec3;
}

/**
 * Draws the Nomad circles into the 3D world as glowing walls that follow the ground: a bright
 * band where the circle meets the ground and a curtain fading upward. Your circle is taller.
 */
export class NomadOverlay {
    labels: CircleLabel[] = [];

    private program: Program;
    private positions: VertexBuffer;
    private colours: VertexBuffer;
    private vertexArray: VertexArray;
    private drawCall: DrawCall;
    private pos = new Float32Array(MAX_VERTICES * 3);
    private col = new Float32Array(MAX_VERTICES * 4);
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
        this.vertexArray = app
            .createVertexArray()
            .vertexAttributeBuffer(0, this.positions)
            .vertexAttributeBuffer(1, this.colours);
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
    ): void {
        this.build(cameraX, cameraZ);
        if (this.count === 0) return;
        this.positions.data(this.pos.subarray(0, this.count * 3));
        this.colours.data(this.col.subarray(0, this.count * 4));
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
            .drawRanges([0, this.count])
            .draw();
        app.blendFunc(PicoGL.SRC_ALPHA, PicoGL.ONE_MINUS_SRC_ALPHA);
        app.depthMask(true);
        if (cullBackFace) app.enable(PicoGL.CULL_FACE);
    }

    private build(cameraX: number, cameraZ: number): void {
        this.count = 0;
        this.labels = [];
        for (const c of this.nomad.circles()) {
            this.circle(c, cameraX, cameraZ);
        }
        const p = this.nomad.player;
        if (p) this.beam(p.x + 0.5, p.y + 0.5);
    }

    private circle(c: DrawnCircle, cameraX: number, cameraZ: number): void {
        const r = ((c.colour >> 16) & 255) / 255;
        const g = ((c.colour >> 8) & 255) / 255;
        const b = (c.colour & 255) / 255;
        const strength = c.yours ? 1 : 0.6;
        const curtain = c.yours ? CURTAIN_YOURS : CURTAIN_OTHER;
        const segments = Math.max(64, Math.min(MAX_SEGMENTS, Math.ceil(2 * Math.PI * c.radius)));
        if (this.count + (segments + 1) * 18 > MAX_VERTICES) return;

        const inner = Math.max(0, c.radius - GLOW);
        let px = 0;
        let pz = 0;
        let pg = 0;
        let qx = 0;
        let qz = 0;
        let qg = 0;
        for (let i = 0; i <= segments; i++) {
            const a = (i / segments) * Math.PI * 2;
            const cos = Math.cos(a);
            const sin = Math.sin(a);
            const x = c.x + 0.5 + cos * c.radius;
            const z = c.y + 0.5 + sin * c.radius;
            const gy = this.ground(x, z);
            const ix = c.x + 0.5 + cos * inner;
            const iz = c.y + 0.5 + sin * inner;
            const ig = this.ground(ix, iz);
            if (i > 0) {
                this.quad(
                    px,
                    pz,
                    x,
                    z,
                    pg + -BAND_LOW,
                    gy + -BAND_LOW,
                    pg - BAND_HIGH,
                    gy - BAND_HIGH,
                    r,
                    g,
                    b,
                    0.9 * strength,
                    0.9 * strength,
                );
                this.quad(
                    px,
                    pz,
                    x,
                    z,
                    pg - BAND_HIGH,
                    gy - BAND_HIGH,
                    pg - curtain,
                    gy - curtain,
                    r,
                    g,
                    b,
                    0.45 * strength,
                    0,
                );
                // A glow on the ground just inside the edge, so the circle reads from above too.
                this.vertex(px, pg - LIFT, pz, r, g, b, 0.5 * strength);
                this.vertex(x, gy - LIFT, z, r, g, b, 0.5 * strength);
                this.vertex(ix, ig - LIFT, iz, r, g, b, 0);
                this.vertex(px, pg - LIFT, pz, r, g, b, 0.5 * strength);
                this.vertex(ix, ig - LIFT, iz, r, g, b, 0);
                this.vertex(qx, qg - LIFT, qz, r, g, b, 0);
            }
            px = x;
            pz = z;
            pg = gy;
            qx = ix;
            qz = iz;
            qg = ig;
        }

        // The label stands on the part of the wall nearest the camera, or straight ahead from inside.
        let dx = cameraX - (c.x + 0.5);
        let dz = cameraZ - (c.y + 0.5);
        const d = Math.sqrt(dx * dx + dz * dz) || 1;
        dx /= d;
        dz /= d;
        const lx = c.x + 0.5 + dx * c.radius;
        const lz = c.y + 0.5 + dz * c.radius;
        this.labels.push({
            name: c.name,
            colour: c.colour,
            yours: c.yours,
            pos: vec3.fromValues(lx, this.ground(lx, lz) - curtain - 0.5, lz),
        });
    }

    /** A pillar of light where the player last stood. */
    private beam(x: number, z: number): void {
        const gy = this.ground(x, z);
        const w = 0.35;
        this.quad(
            x - w,
            z,
            x + w,
            z,
            gy + 0.2,
            gy + 0.2,
            gy - BEAM,
            gy - BEAM,
            1,
            0.92,
            0.6,
            0.85,
            0,
        );
        this.quad(
            x,
            z - w,
            x,
            z + w,
            gy + 0.2,
            gy + 0.2,
            gy - BEAM,
            gy - BEAM,
            1,
            0.92,
            0.6,
            0.85,
            0,
        );
    }

    /** A vertical quad from (x0,z0) to (x1,z1), bottom at y0s, top at y1s; alpha fades bottom to top. */
    private quad(
        x0: number,
        z0: number,
        x1: number,
        z1: number,
        bottom0: number,
        bottom1: number,
        top0: number,
        top1: number,
        r: number,
        g: number,
        b: number,
        alphaBottom: number,
        alphaTop: number,
    ): void {
        this.vertex(x0, bottom0, z0, r, g, b, alphaBottom);
        this.vertex(x1, bottom1, z1, r, g, b, alphaBottom);
        this.vertex(x1, top1, z1, r, g, b, alphaTop);
        this.vertex(x0, bottom0, z0, r, g, b, alphaBottom);
        this.vertex(x1, top1, z1, r, g, b, alphaTop);
        this.vertex(x0, top0, z0, r, g, b, alphaTop);
    }

    private vertex(
        x: number,
        y: number,
        z: number,
        r: number,
        g: number,
        b: number,
        a: number,
    ): void {
        const i = this.count++;
        this.pos[i * 3] = x;
        this.pos[i * 3 + 1] = y;
        this.pos[i * 3 + 2] = z;
        this.col[i * 4] = r;
        this.col[i * 4 + 1] = g;
        this.col[i * 4 + 2] = b;
        this.col[i * 4 + 3] = a;
    }

    delete(): void {
        this.vertexArray.delete();
        this.positions.delete();
        this.colours.delete();
        this.program.delete();
    }
}
