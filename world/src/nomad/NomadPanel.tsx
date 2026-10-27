import { vec3, vec4 } from "gl-matrix";
import { useEffect, useRef, useState } from "react";

import { Camera } from "../mapviewer/Camera";
import { MapViewer } from "../mapviewer/MapViewer";
import { WebGLMapViewerRenderer } from "../mapviewer/webgl/WebGLMapViewerRenderer";
import "./NomadPanel.css";
import { DrawnCircle, NomadWorld, SPEEDS, randomSeed } from "./NomadWorld";

const OVERVIEW_PITCH = -300;

/** Moves the camera to look down on a circle from its south side, north up. */
export function frameCircle(mapViewer: MapViewer, nomad: NomadWorld, c: DrawnCircle): void {
    const probe = new Camera(0, 0, 0, OVERVIEW_PITCH, 0);
    probe.update(1, 1);
    // The camera looks down its own -z axis.
    const forward = vec3.fromValues(
        -probe.cameraMatrix[8],
        -probe.cameraMatrix[9],
        -probe.cameraMatrix[10],
    );
    const distance = Math.min(110, c.radius * 1.7 + 14);
    const cx = c.x + 0.5;
    const cz = c.y + 0.5;
    const ground = -nomad.groundFar(cx, cz);
    mapViewer.setCamera({
        position: vec3.fromValues(
            cx - forward[0] * distance,
            ground - forward[1] * distance,
            cz - forward[2] * distance,
        ),
        pitch: OVERVIEW_PITCH,
        yaw: 0,
    });
    mapViewer.updateSearchParams();
}

function played(minutes: number): string {
    const d = Math.floor(minutes / 1440);
    const h = Math.floor((minutes % 1440) / 60);
    const m = minutes % 60;
    return d > 0 ? `${d}d ${h}h ${m}m` : h > 0 ? `${h}h ${m}m` : `${m}m`;
}

function ago(ms: number): string {
    const m = Math.round(ms / 60_000);
    if (m < 1) return "just now";
    if (m < 60) return `${m} min ago`;
    const h = Math.round(m / 60);
    return h < 48 ? `${h} h ago` : `${Math.round(h / 24)} days ago`;
}

const hex = (colour: number) => "#" + colour.toString(16).padStart(6, "0");

interface NomadPanelProps {
    mapViewer: MapViewer;
    nomad: NomadWorld;
}

/** The Nomad panel over the 3D world: the run, the clock, and a way to each circle. */
export function NomadPanel({ mapViewer, nomad }: NomadPanelProps): JSX.Element | null {
    const [, setVersion] = useState(0);
    const [open, setOpen] = useState(true);
    const [list, setList] = useState(false);

    useEffect(() => {
        const timer = window.setInterval(() => setVersion(nomad.version), 250);
        return () => clearInterval(timer);
    }, [nomad]);

    const sim = nomad.sim;
    if (!sim) return null;
    const circles = nomad.circles().sort((a, b) => a.index - b.index);
    const yours = circles.find((c) => c.yours)!;

    let title: string;
    if (nomad.mode === "live") {
        title = nomad.isLivePlaying()
            ? "Live: playing now"
            : `Live: last seen ${ago(Date.now() - nomad.liveAt)}`;
    } else if (nomad.mode === "run") {
        title = "Your run, from the plugin";
    } else {
        title = `Seed ${sim.state.seed}`;
    }

    if (!open) {
        return (
            <button className="nomad-panel nomad-tab rs-border" onClick={() => setOpen(true)}>
                Nomad UIM
            </button>
        );
    }

    return (
        <div className="nomad-panel rs-border">
            <div className="nomad-head">
                <span className="nomad-title">Nomad UIM</span>
                <button
                    className="nomad-x"
                    onClick={() => setOpen(false)}
                    aria-label="Hide the panel"
                >
                    ×
                </button>
            </div>
            <div className="nomad-line">{title}</div>
            <div className="nomad-line">Played {played(sim.state.minutes)}</div>
            <div className="nomad-line">
                Bound to <span className="nomad-dot" style={{ background: hex(yours.colour) }} />
                <span style={{ color: hex(yours.colour) }}>{yours.name}</span>
                {yours.sailing ? " (at sea)" : ""}
            </div>
            {nomad.error && <div className="nomad-error">{nomad.error}</div>}

            <div className="nomad-row">
                <button className="nomad-btn" onClick={() => frameCircle(mapViewer, nomad, yours)}>
                    Go to your circle
                </button>
                <button className="nomad-btn" onClick={() => setList(!list)}>
                    {list ? "Hide circles" : "All circles"}
                </button>
            </div>

            {nomad.mode !== "live" && (
                <div className="nomad-row">
                    {SPEEDS.map((s, i) => (
                        <button
                            key={s.label}
                            className={"nomad-btn" + (nomad.speed === i ? " on" : "")}
                            onClick={() => nomad.setSpeed(i)}
                        >
                            {s.label}
                        </button>
                    ))}
                </div>
            )}

            {list && (
                <ul className="nomad-list">
                    {circles.map((c) => (
                        <li key={c.index}>
                            <button onClick={() => frameCircle(mapViewer, nomad, c)}>
                                <span className="nomad-dot" style={{ background: hex(c.colour) }} />
                                {c.name}
                                <span className="nomad-dim"> r{Math.round(c.radius)}</span>
                            </button>
                        </li>
                    ))}
                </ul>
            )}

            <div className="nomad-row">
                <a className="nomad-btn" href={nomad.siteLink()}>
                    2D map
                </a>
                {nomad.mode === "seed" && (
                    <a className="nomad-btn" href={`?seed=${randomSeed()}`}>
                        New seed
                    </a>
                )}
            </div>
        </div>
    );
}

/** Circle names standing on the walls, moved every frame without re-rendering React. */
export function NomadLabels({ mapViewer }: { mapViewer: MapViewer }): JSX.Element {
    const root = useRef<HTMLDivElement>(null);

    useEffect(() => {
        let frame = 0;
        const clip = vec4.create();
        const loop = () => {
            frame = requestAnimationFrame(loop);
            const el = root.current;
            const renderer = mapViewer.renderer;
            if (!el || !(renderer instanceof WebGLMapViewerRenderer) || !renderer.nomadOverlay)
                return;
            const labels = renderer.nomadOverlay.labels;
            while (el.children.length < labels.length) {
                el.appendChild(document.createElement("div"));
            }
            const camera = mapViewer.camera;
            const canvas = renderer.canvas;
            const w = canvas.clientWidth;
            const h = canvas.clientHeight;
            const limit = mapViewer.renderDistance - 8;
            for (let i = 0; i < el.children.length; i++) {
                const div = el.children[i] as HTMLDivElement;
                const label = labels[i];
                if (!label) {
                    div.style.display = "none";
                    continue;
                }
                const p = label.pos;
                const far =
                    Math.max(Math.abs(p[0] - camera.getPosX()), Math.abs(p[2] - camera.getPosZ())) >
                    limit;
                vec4.transformMat4(
                    clip,
                    vec4.fromValues(p[0], p[1], p[2], 1),
                    camera.viewProjMatrix,
                );
                if (far || clip[3] <= 0) {
                    div.style.display = "none";
                    continue;
                }
                const x = ((clip[0] / clip[3]) * 0.5 + 0.5) * w;
                const y = (1 - ((clip[1] / clip[3]) * 0.5 + 0.5)) * h;
                div.style.display = "";
                div.className = "nomad-label" + (label.yours ? " yours" : "");
                div.style.color = hex(label.colour);
                div.style.transform = `translate(${x.toFixed(1)}px, ${y.toFixed(
                    1,
                )}px) translate(-50%, -100%)`;
                if (div.textContent !== label.name) div.textContent = label.name;
            }
        };
        frame = requestAnimationFrame(loop);
        return () => cancelAnimationFrame(frame);
    }, [mapViewer]);

    return <div ref={root} className="nomad-labels" />;
}
