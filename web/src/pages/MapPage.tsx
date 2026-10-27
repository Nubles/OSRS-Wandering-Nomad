import { lazy, Suspense, useCallback, useEffect, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography, Tile } from '../engine/geography';
import { forecast } from '../engine/forecast';
import { decodeRun, encodeRun } from '../engine/runcode';
import { contains, MAIN, NomadSim } from '../engine/sim';
import { ago, fetchLive, isLive, LIVE_CODE_RE, LiveRun, POLL_MS } from '../live';
import type { Route } from '../router';

const Map3D = lazy(() => import('../components/Map3D'));
const VIEW_KEY = 'nomad.view';

function savedView(): '2d' | '3d' {
  try {
    return localStorage.getItem(VIEW_KEY) === '3d' ? '3d' : '2d';
  } catch {
    return '2d';
  }
}

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
  const [view, setView] = useState(savedView);
  const [showForecast, setShowForecast] = useState(false);
  const [loaded, setLoaded] = useState<'seed' | 'run' | 'live'>('seed');
  const [liveRun, setLiveRun] = useState<LiveRun | null>(null);
  const liveRef = useRef<LiveRun | null>(null);
  liveRef.current = liveRun;
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
    setLiveRun(null);
    if (route.live) {
      const code = route.live;
      if (!LIVE_CODE_RE.test(code)) {
        setError('That live link is not complete. Copy it again from the plugin.');
        startSeed(randomSeed());
        return;
      }
      let etag: string | null = null;
      let first = true;
      let timer = 0;
      const poll = async () => {
        try {
          const got = await fetchLive(code, etag);
          if (!live) return;
          if (got.kind === 'missing') {
            if (first) {
              setError("This live run hasn't been shared yet. Turn on Share live in the plugin's settings and play for a minute.");
              startSeed(randomSeed());
              setPlaying(false);
            }
          } else if (got.kind === 'run') {
            etag = got.etag;
            const run = await decodeRun(got.data.run);
            if (!live) return;
            const sim = new NomadSim(geo, run.sim);
            const player = run.surface ?? null;
            // Catch up on the minutes played since the plugin sent this, up to the point it would have sent again.
            if (isLive(got.data)) {
              const behind = Math.min(Math.floor((Date.now() - got.data.at) / 60_000), 10);
              for (let i = 0; i < behind; i++) sim.step(player);
            }
            const view = scene.current;
            scene.current = { sim, player, forecast: null, fog: fogRef.current, follow: view?.follow ?? true };
            setLoaded('live');
            setLiveRun(got.data);
            setPlaying(isLive(got.data));
            if (first) {
              setRunId((r) => r + 1);
              setLog([{ minute: sim.state.minutes, line: 'Watching a live run. The circles move as the player plays.' }]);
            }
            lastForecast.current = -Infinity;
            refresh();
          }
          first = false;
        } catch (e) {
          if (live && first) setError((e as Error).message);
        }
        if (live) timer = window.setTimeout(poll, POLL_MS);
      };
      poll();
      return () => {
        live = false;
        clearTimeout(timer);
      };
    }
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
      // A live run whose player stopped sending stays where it was last seen.
      if (loaded === 'live' && (!liveRef.current || !isLive(liveRef.current))) {
        last = now;
        return;
      }
      // Live runs move at the game's own pace: one played minute a minute.
      owed += ((now - last) / 1000) * (loaded === 'live' ? 1 / 60 : SPEEDS[speed].perSecond);
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
  }, [playing, speed, showForecast, refresh, loaded]);

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
      if (!scene.current || loaded === 'live') return;
      scene.current.player = tile;
      refresh();
    },
    [refresh, loaded],
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
      loaded === 'live'
        ? location.href
        : loaded === 'run'
        ? `${base}#/run/${await encodeRun({ sim: s.sim.state, surface: s.player ?? undefined })}`
        : `${base}#/map?seed=${s.sim.state.seed}`;
    try {
      await navigator.clipboard.writeText(url);
      setCopied(loaded === 'live' ? 'Live link copied.' : loaded === 'run' ? 'Link to this run copied.' : 'Link to this seed copied.');
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
          {view === '3d' ? (
            <Suspense fallback={<div className="loading">Loading the 3D map…</div>}>
              <Map3D scene={scene} onPick={pick} className="map-main" />
            </Suspense>
          ) : (
            <MapCanvas scene={scene} zoom={1.4} onPick={pick} className="map-main" />
          )}
          <div className="map-mode" role="group" aria-label="Map view">
            {(['2d', '3d'] as const).map((m) => (
              <button
                key={m}
                type="button"
                className={`btn${view === m ? ' is-on' : ''}`}
                aria-pressed={view === m}
                onClick={() => {
                  setView(m);
                  try {
                    localStorage.setItem(VIEW_KEY, m);
                  } catch {
                    /* private mode: the choice just isn't remembered */
                  }
                }}
              >
                {m.toUpperCase()}
              </button>
            ))}
          </div>
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
            <span className="chat-note">
              {loaded === 'live' ? '' : 'Tap the map to stand somewhere. '}
              {view === '3d' ? 'Drag to spin, right-drag or two fingers to move, scroll or pinch to zoom.' : 'Drag to pan, scroll or pinch to zoom.'}
            </span>
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
              <div className="osrs-title">
                {loaded === 'live' ? 'Live run' : loaded === 'run' ? 'Your run, from the plugin' : `Seed ${sim.state.seed}`}
              </div>
              <div className="clock">{formatPlayed(sim.state.minutes)}</div>
              <div className="muted small">played time</div>
            </div>
            {loaded !== 'live' && (
              <button type="button" className="btn btn-big" onClick={() => setPlaying((p) => !p)}>
                {playing ? 'Pause' : 'Play'}
              </button>
            )}
          </div>
          {loaded === 'live' && liveRun && (
            <div className="live-status">
              {isLive(liveRun) ? (
                <>
                  <span className="live-dot is-on" /> <span className="ok">Live</span>
                  <span className="muted small"> · updated {ago(Date.now() - liveRun.at)}</span>
                </>
              ) : (
                <>
                  <span className="live-dot" /> <span className="warn">Offline</span>
                  <span className="muted small"> · last played {ago(Date.now() - liveRun.at)}</span>
                </>
              )}
            </div>
          )}
          {loaded !== 'live' && (
          <div className="speeds" role="group" aria-label="Speed">
            {SPEEDS.map((sp, i) => (
              <button key={sp.label} type="button" className="btn" aria-pressed={speed === i} onClick={() => setSpeed(i)}>
                {sp.label}
              </button>
            ))}
          </div>
          )}
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
            {status.where === 'unknown' && (loaded === 'live' ? 'Waiting for the player\'s position.' : 'Tap the map to stand somewhere.')}
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
                      disabled={!ok || loaded === 'live'}
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
              disabled={loaded === 'live'}
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
                      <button type="button" className="btn" aria-label={`Fewer ${region.name} diary tiers`} onClick={() => setDiary(region.id, c.tiers - 1)} disabled={c.tiers === 0 || loaded === 'live'}>
                        −
                      </button>
                      <span>
                        {c.tiers}/{max}
                      </span>
                      <button type="button" className="btn" aria-label={`More ${region.name} diary tiers`} onClick={() => setDiary(region.id, c.tiers + 1)} disabled={c.tiers >= max || loaded === 'live'}>
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
