import { useEffect, useMemo, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography } from '../engine/geography';
import { decodeRun, SavedRun } from '../engine/runcode';
import { NomadSim } from '../engine/sim';
import { lastRun, played } from '../story';

const COLOURS = ['#ff981f', '#4fc3f7', '#ce93d8', '#8ce99a'];
const MAX_RUNS = 4;
const CODES_KEY = 'nomad.compare';
/** The trail behind each run's Main, in played minutes. */
const TRAIL = 24 * 60;

interface Loaded {
  code: string;
  run: SavedRun | null;
  error?: string;
}

/** Where a run's Main was at a played minute, eased between its hourly samples; null before its first. */
export function at(run: SavedRun, minute: number): { x: number; y: number; r: number; you: [number, number] | null } | null {
  const track = run.story?.track ?? [];
  if (!track.length) {
    const m = run.sim.circles[0];
    return minute >= run.sim.minutes ? { x: m.x, y: m.y, r: m.radius, you: run.surface ? [run.surface.x, run.surface.y] : null } : null;
  }
  if (minute < track[0][0]) return null;
  let i = track.findIndex((t) => t[0] > minute);
  if (i < 0) i = track.length;
  const a = track[i - 1];
  const b = track[i] ?? a;
  const k = b[0] === a[0] ? 0 : (minute - a[0]) / (b[0] - a[0]);
  const near = k < 0.5 ? a : b;
  return {
    x: a[1] + (b[1] - a[1]) * k,
    y: a[2] + (b[2] - a[2]) * k,
    r: a[3] + (b[3] - a[3]) * k,
    you: near[4] >= 0 ? [near[4], near[5]] : null,
  };
}

function savedCodes(): string[] {
  try {
    const v = JSON.parse(sessionStorage.getItem(CODES_KEY) ?? '[]');
    if (Array.isArray(v) && v.length) return v.slice(0, MAX_RUNS).map(String);
  } catch {
    /* none kept */
  }
  return [lastRun(), '', '', ''].slice(0, MAX_RUNS);
}

/** Side by side: up to four runs on one map, lined up by played hour, with one scrubber. */
export default function Compare({ geo }: { geo: Geography }) {
  const [codes, setCodes] = useState<string[]>(() => {
    const c = savedCodes();
    while (c.length < MAX_RUNS) c.push('');
    return c;
  });
  const [runs, setRuns] = useState<Loaded[]>([]);
  const [minute, setMinute] = useState(0);
  const [playing, setPlaying] = useState(false);
  const scene = useRef<MapScene | null>(null);

  const load = async () => {
    try {
      sessionStorage.setItem(CODES_KEY, JSON.stringify(codes));
    } catch {
      /* not kept */
    }
    const out: Loaded[] = [];
    for (const code of codes.map((c) => c.trim()).filter(Boolean)) {
      try {
        out.push({ code, run: await decodeRun(code) });
      } catch (e) {
        out.push({ code, run: null, error: (e as Error).message });
      }
    }
    setRuns(out);
    setMinute(0);
  };

  useEffect(() => {
    if (codes.some((c) => c.trim())) load();
    // Only the codes kept from last time load by themselves.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const good = runs.filter((r) => r.run) as { code: string; run: SavedRun }[];
  const end = Math.max(60, ...good.map((r) => r.run.sim.minutes));

  useEffect(() => {
    if (!playing) return;
    const timer = window.setInterval(() => setMinute((m) => Math.min(end, m + 60)), 120);
    return () => clearInterval(timer);
  }, [playing, end]);

  useEffect(() => {
    if (playing && minute >= end) setPlaying(false);
  }, [playing, minute, end]);

  const first = good[0]?.run;
  const sim = useMemo(() => (first ? new NomadSim(geo, first.sim) : NomadSim.start(geo, 1)), [geo, first]);
  scene.current ??= { sim, player: null, forecast: null, fog: false, follow: true, bare: true };
  scene.current.sim = sim;
  const heads = good.map((g) => at(g.run, minute));
  const firstHead = heads.find(Boolean);
  scene.current.focus = firstHead ? { x: firstHead.x, y: firstHead.y } : null;
  scene.current.extra = ({ ctx, at: toScreen, scale, text }) => {
    good.forEach((g, i) => {
      const colour = COLOURS[i];
      // A day's trail behind, from the hourly samples.
      const track = (g.run.story?.track ?? []).filter((t) => t[0] <= minute && t[0] >= minute - TRAIL);
      if (track.length > 1) {
        ctx.beginPath();
        track.forEach((t, k) => {
          const [x, y] = toScreen(t[1], t[2]);
          if (k) ctx.lineTo(x, y);
          else ctx.moveTo(x, y);
        });
        ctx.strokeStyle = colour;
        ctx.globalAlpha = 0.6;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.globalAlpha = 1;
      }
      const h = heads[i];
      if (!h) return;
      const [x, y] = toScreen(h.x, h.y);
      ctx.beginPath();
      ctx.arc(x, y, h.r * scale, 0, Math.PI * 2);
      ctx.fillStyle = colour + '22';
      ctx.fill();
      ctx.strokeStyle = colour;
      ctx.lineWidth = 2.5;
      ctx.stroke();
      text(`Run ${i + 1}`, x, y - h.r * scale - 6, colour, 'RSBold');
      if (h.you) {
        const [px, py] = toScreen(h.you[0], h.you[1]);
        ctx.beginPath();
        ctx.arc(px, py, 4, 0, Math.PI * 2);
        ctx.fillStyle = colour;
        ctx.fill();
        ctx.strokeStyle = '#000';
        ctx.lineWidth = 1.5;
        ctx.stroke();
      }
    });
  };

  return (
    <div className="page-map-split">
      <div className="osrs-win map-frame">
        <MapCanvas scene={scene} zoom={0.6} className="map-main" />
      </div>
      <aside className="panel osrs-win">
        <div className="osrs-title">Side by side</div>
        <p className="muted small">
          Up to four runs on one map, lined up by played hour. Paste each run code from the plugin's sidebar. Codes with a story move
          hour by hour; older ones show only where they are now.
        </p>
        {codes.map((c, i) => (
          <div key={i} className="row" style={{ marginBottom: 4 }}>
            <span className="dot" style={{ background: COLOURS[i] }} />
            <input
              className="input grow"
              aria-label={`Run ${i + 1} code`}
              placeholder={`Run ${i + 1}: N1.…`}
              value={c}
              onChange={(e) => setCodes(codes.map((x, k) => (k === i ? e.target.value : x)))}
            />
          </div>
        ))}
        <div className="row">
          <button type="button" className="btn" onClick={load} disabled={!codes.some((c) => c.trim())}>
            Load
          </button>
        </div>
        {runs.filter((r) => r.error).map((r, i) => (
          <div key={i} className="osrs-chat notice notice-error">
            {r.error}
          </div>
        ))}
        {good.length > 0 && (
          <section>
            <h3>Hour {Math.floor(minute / 60)}</h3>
            <input
              className="scrubber"
              type="range"
              min={0}
              max={end}
              step={60}
              value={minute}
              aria-label="Played time"
              onChange={(e) => setMinute(Number(e.target.value))}
            />
            <div className="row">
              <button type="button" className="btn" onClick={() => setPlaying((p) => !p)}>
                {playing ? 'Pause' : 'Play'}
              </button>
              <span className="muted small">{played(minute)} of {played(end)}</span>
            </div>
            <ul className="kind-list">
              {good.map((g, i) => {
                const h = heads[i];
                const region = h ? geo.regionAt(Math.round(h.x), Math.round(h.y)) : undefined;
                return (
                  <li key={i}>
                    <span className="dot" style={{ background: COLOURS[i] }} />
                    <span>
                      Run {i + 1}
                      {g.run.epithet ? `, ${g.run.epithet}` : ''}: {h ? `Main in ${region?.name ?? 'open water'}, ${Math.round(h.r)} tiles` : 'not started yet'}
                    </span>
                    <span className="count">{played(g.run.sim.minutes)}</span>
                  </li>
                );
              })}
            </ul>
          </section>
        )}
      </aside>
    </div>
  );
}
