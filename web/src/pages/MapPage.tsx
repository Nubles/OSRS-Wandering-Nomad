import { useCallback, useEffect, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography, Tile } from '../engine/geography';
import { forecast } from '../engine/forecast';
import { decodeRun, encodeRun } from '../engine/runcode';
import { contains, MAIN, NomadSim } from '../engine/sim';
import type { Route } from '../router';

const SPEEDS = [
  { label: '1 min/s', perSecond: 1 },
  { label: '10 min/s', perSecond: 10 },
  { label: '1 h/s', perSecond: 60 },
];
const LOG_LIMIT = 80;

interface Props {
  geo: Geography;
  route: Route;
}

export function MapPage({ geo, route }: Props) {
  const scene = useRef<MapScene | null>(null);
  const [, setTick] = useState(0);
  const [runId, setRunId] = useState(0);
  const [playing, setPlaying] = useState(true);
  const [speed, setSpeed] = useState(1);
  const [log, setLog] = useState<{ minute: number; line: string }[]>([]);
  const [fog, setFog] = useState(true);
  const [showForecast, setShowForecast] = useState(false);
  const [loaded, setLoaded] = useState<'seed' | 'run'>('seed');
  const [error, setError] = useState<string | null>(null);
  const [codeInput, setCodeInput] = useState('');
  const [copied, setCopied] = useState<string | null>(null);
  const lastForecast = useRef(-Infinity);
  const chatRef = useRef<HTMLOListElement>(null);
  const fogRef = useRef(fog);
  fogRef.current = fog;

  const refresh = useCallback(() => setTick((t) => t + 1), []);

  const startSeed = useCallback(
    (seed: number) => {
      scene.current = { sim: NomadSim.start(geo, seed), player: null, forecast: null, fog: fogRef.current, follow: true };
      setLoaded('seed');
      setRunId((r) => r + 1);
      setLog([{ minute: 0, line: `A new run on seed ${seed}. You start in the Main circle at Lumbridge.` }]);
      lastForecast.current = -Infinity;
      refresh();
    },
    [geo, refresh],
  );

  // Load from the link: a run code, a seed, or a fresh random seed.
  useEffect(() => {
    let live = true;
    setError(null);
    if (route.run) {
      decodeRun(route.run)
        .then((run) => {
          if (!live) return;
          scene.current = {
            sim: new NomadSim(geo, run.sim),
            player: run.surface ?? null,
            forecast: null,
            fog: fogRef.current,
            follow: true,
          };
          setLoaded('run');
          setRunId((r) => r + 1);
          setPlaying(false);
          setLog([{ minute: run.sim.minutes, line: 'Loaded your run from the plugin. Press play to see where it could drift next.' }]);
          lastForecast.current = -Infinity;
          refresh();
        })
        .catch((e: Error) => {
          if (!live) return;
          setError(e.message);
          startSeed(randomSeed());
        });
    } else {
      const s = Number(route.params.get('seed'));
      startSeed(Number.isFinite(s) && route.params.has('seed') ? s | 0 : randomSeed());
    }
    return () => {
      live = false;
    };
  }, [geo, route, startSeed, refresh]);

  // The clock: plays minutes at the chosen speed and refreshes the panel a few times a second.
  useEffect(() => {
    if (!playing) return;
    let frame = 0;
    let last = performance.now();
    let owed = 0;
    let lastUi = 0;
    const loop = (now: number) => {
      frame = requestAnimationFrame(loop);
      const s = scene.current;
      if (!s) return;
      owed += ((now - last) / 1000) * SPEEDS[speed].perSecond;
      last = now;
      const steps = Math.min(Math.floor(owed), 600);
      owed -= steps;
      if (steps > 0) {
        const lines: { minute: number; line: string }[] = [];
        for (let i = 0; i < steps; i++) {
          for (const line of s.sim.step(s.player)) lines.push({ minute: s.sim.state.minutes, line });
        }
        if (lines.length) setLog((l) => [...lines.reverse(), ...l].slice(0, LOG_LIMIT));
        if (showForecast && s.sim.state.minutes - lastForecast.current >= 15) {
          s.forecast = forecast(s.sim);
          lastForecast.current = s.sim.state.minutes;
        }
      }
      if (now - lastUi > 150) {
        lastUi = now;
        refresh();
      }
    };
    frame = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frame);
  }, [playing, speed, showForecast, refresh]);

  useEffect(() => {
    const s = scene.current;
    if (!s) return;
    s.fog = fog;
    if (showForecast) {
      s.forecast = forecast(s.sim);
      lastForecast.current = s.sim.state.minutes;
    } else {
      s.forecast = null;
    }
  }, [fog, showForecast, runId]);

  // Like the game's chatbox, the newest line sits at the bottom.
  useEffect(() => {
    const el = chatRef.current;
    if (el) el.scrollTop = el.scrollHeight;
  }, [log]);

  const s = scene.current;
  const sim = s?.sim;

  const pick = useCallback(
    (tile: Tile) => {
      if (!scene.current) return;
      scene.current.player = tile;
      refresh();
    },
    [refresh],
  );

  const crossTo = (i: number) => {
    if (!s) return;
    const line = s.sim.crossTo(i, s.player);
    if (line) setLog((l) => [{ minute: s.sim.state.minutes, line }, ...l].slice(0, LOG_LIMIT));
    refresh();
  };

  const setDiary = (regionId: string, tiers: number) => {
    if (!s) return;
    const line = s.sim.setDiaryTiers(regionId, tiers);
    if (line) setLog((l) => [{ minute: s.sim.state.minutes, line }, ...l].slice(0, LOG_LIMIT));
    refresh();
  };

  const share = async () => {
    if (!s) return;
    const base = `${location.origin}${location.pathname}`;
    const url =
      loaded === 'run'
        ? `${base}#/run/${await encodeRun({ sim: s.sim.state, surface: s.player ?? undefined })}`
        : `${base}#/map?seed=${s.sim.state.seed}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(loaded === 'run' ? 'Link to this run copied.' : 'Link to this seed copied.');
    } catch {
      setCopied(url);
    }
  };

  const loadCode = async () => {
    const code = codeInput.trim();
    if (!code) return;
    try {
      await decodeRun(code);
      location.hash = `#/run/${code}`;
      setCodeInput('');
    } catch (e) {
      setError((e as Error).message);
    }
  };

  if (!sim || !s) return <div className="loading">Loading the map…</div>;

  const yours = sim.bound;
  const status = {
    yours,
    where: s.player ? (contains(yours, s.player.x, s.player.y) ? 'inside' : 'outside') : 'unknown',
    stranded: sim.stranded(),
    volcano: sim.volcanoOpen(),
  };

  const crossings = sim.circles
    .map((c, i) => ({ c, i }))
    .filter(({ i }) => sim.linked(i) && sim.canCross(i, null));

  return (
    <div className="map-page">
      <div className="map-col">
        <div className="osrs-win map-frame">
          <MapCanvas scene={scene} zoom={1.4} onPick={pick} className="map-main" />
        </div>
        <div className="osrs-chat chatbox">
          <ol className="chat-lines" ref={chatRef} aria-label="Run log">
            {[...log].reverse().map((l, k) => (
              <li key={`${l.minute}-${k}`}>
                <span className="chat-time">[{formatPlayed(l.minute)}]</span> {l.line}
              </li>
            ))}
          </ol>
          <div className="chat-tabs">
            <span className="btn is-on">Run log</span>
            <span className="chat-note">Tap the map to stand somewhere. Drag to pan, scroll or pinch to zoom.</span>
          </div>
        </div>
      </div>
      <aside className="panel osrs-win">
        {error && (
          <div className="osrs-chat notice notice-error" role="alert">
            {error}
            <button type="button" className="link" onClick={() => setError(null)}>Dismiss</button>
          </div>
        )}
        <section>
          <div className="run-head">
            <div>
              <div className="osrs-title">{loaded === 'run' ? 'Your run, from the plugin' : `Seed ${sim.state.seed}`}</div>
              <div className="clock">{formatPlayed(sim.state.minutes)}</div>
              <div className="muted small">played time</div>
            </div>
            <button type="button" className="btn btn-big" onClick={() => setPlaying((p) => !p)}>
              {playing ? 'Pause' : 'Play'}
            </button>
          </div>
          <div className="speeds" role="group" aria-label="Speed">
            {SPEEDS.map((sp, i) => (
              <button key={sp.label} type="button" className="btn" aria-pressed={speed === i} onClick={() => setSpeed(i)}>
                {sp.label}
              </button>
            ))}
          </div>
        </section>

        <section className="status">
          <div className="status-row">
            <span className="dot" style={{ background: hex(sim.colour(status.yours)) }} />
            <span>
              You are bound to <span className="value">{sim.name(status.yours)}</span>
              {status.yours.voyage ? <>, sailing to {status.yours.voyage.to}</> : null}.
            </span>
          </div>
          <div className="status-row muted">
            {status.where === 'unknown' && 'Tap the map to stand somewhere.'}
            {status.where === 'inside' && <span className="ok">You are inside your circle.</span>}
            {status.where === 'outside' && (
              <span className="warn">You are outside. Your circle drifts 5x faster and shrinks a tile a minute.</span>
            )}
          </div>
          {status.stranded && (
            <div className="osrs-chat notice">Stranded: Main has moved on. Your circle leans toward it and waits at the border.</div>
          )}
          <div className="status-row muted small">
            Inferno and Fight Caves: {status.volcano ? <strong className="ok">open</strong> : 'closed'} (Main must cover the volcano)
          </div>
        </section>

        <section>
          <h3>Crossings</h3>
          {crossings.length === 0 ? (
            <p className="muted small">
              {sim.state.bound === MAIN
                ? 'No region circle touches Main right now.'
                : 'Main is not touching your circle. The only way out is back into Main.'}
            </p>
          ) : (
            <ul className="crossings">
              {crossings.map(({ c, i }) => {
                const ok = sim.canCross(i, s.player);
                return (
                  <li key={i}>
                    <span className="dot" style={{ background: hex(sim.colour(c)) }} />
                    <span className="grow name">{sim.name(c)}</span>
                    <button
                      type="button"
                      className="btn btn-small"
                      disabled={!ok}
                      title={ok ? '' : 'Stand where the two circles overlap'}
                      onClick={() => crossTo(i)}
                    >
                      Cross
                    </button>
                  </li>
                );
              })}
            </ul>
          )}
          {crossings.length > 0 && s.player && crossings.some(({ i }) => !sim.canCross(i, s.player)) && (
            <p className="muted small">To cross, stand where the two circles overlap.</p>
          )}
        </section>

        <section>
          <h3>Map</h3>
          <label className="check">
            <input type="checkbox" checked={fog} onChange={(e) => setFog(e.target.checked)} /> Fog outside your circle
          </label>
          <label className="check">
            <input type="checkbox" checked={showForecast} onChange={(e) => setShowForecast(e.target.checked)} /> Forecast (where Main and your
            circle could be in 1, 3 and 6 hours)
          </label>
          <label className="check">
            <input
              type="checkbox"
              checked={sim.state.sailingUnlocked}
              onChange={(e) => {
                sim.state.sailingUnlocked = e.target.checked;
                refresh();
              }}
            />{' '}
            Sailing unlocked (Main can sail from any port to any port)
          </label>
        </section>

        <section>
          <h3>Circles</h3>
          <ul className="circles">
            {sim.circles.map((c, i) => {
              const region = c.regionId ? geo.region(c.regionId) : undefined;
              const max = region ? geo.maxTiers(region) : 0;
              const gap = Math.max(0, Math.round(Math.hypot(c.x - status.yours.x, c.y - status.yours.y) - c.radius - status.yours.radius));
              return (
                <li key={i} className={i === sim.state.bound ? 'is-yours' : ''}>
                  <span className="dot" style={{ background: hex(sim.colour(c)) }} />
                  <span className="grow">
                    <span className="name">{sim.name(c)}</span>
                    <span className="muted small">
                      {' '}
                      · {Math.round(c.radius)} tiles
                      {i !== sim.state.bound && (gap === 0 ? ' · touching' : ` · ${gap} away`)}
                    </span>
                  </span>
                  {region && max > 0 && (
                    <span className="diary" title={`Diary tiers done: ${c.tiers} of ${max}`}>
                      <button type="button" className="btn" aria-label={`Fewer ${region.name} diary tiers`} onClick={() => setDiary(region.id, c.tiers - 1)} disabled={c.tiers === 0}>
                        −
                      </button>
                      <span>
                        {c.tiers}/{max}
                      </span>
                      <button type="button" className="btn" aria-label={`More ${region.name} diary tiers`} onClick={() => setDiary(region.id, c.tiers + 1)} disabled={c.tiers >= max}>
                        +
                      </button>
                    </span>
                  )}
                </li>
              );
            })}
          </ul>
          <p className="muted small">The numbers are Achievement Diary tiers. Each one permanently grows that kingdom's circle.</p>
        </section>

        <section>
          <h3>Share or load</h3>
          <div className="row">
            <button type="button" className="btn" onClick={share}>
              Copy link
            </button>
            <button type="button" className="btn" onClick={() => (location.hash = `#/map?seed=${randomSeed()}`)}>
              New seed
            </button>
          </div>
          {copied && <p className="muted small break">{copied}</p>}
          <label className="small muted" htmlFor="runcode">
            Paste a run code from the plugin's sidebar:
          </label>
          <div className="row">
            <input id="runcode" className="input" value={codeInput} placeholder="N1.…" onChange={(e) => setCodeInput(e.target.value)} />
            <button type="button" className="btn" onClick={loadCode} disabled={!codeInput.trim()}>
              Load
            </button>
          </div>
        </section>
      </aside>
    </div>
  );
}

export function formatPlayed(minutes: number): string {
  const h = Math.floor(minutes / 60);
  const m = minutes % 60;
  return `${h}h ${String(m).padStart(2, '0')}m`;
}

export function hex(rgb: number): string {
  return `#${rgb.toString(16).padStart(6, '0')}`;
}

function randomSeed(): number {
  return Math.floor(Math.random() * 1_000_000_000);
}
