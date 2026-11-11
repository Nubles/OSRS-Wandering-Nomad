import { useEffect, useMemo, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import { RunCodeBox } from '../components/RunCodeBox';
import type { Geography } from '../engine/geography';
import { MAIN, NomadSim } from '../engine/sim';
import { byKind, inside, KIND_COLOURS, loadPlaces, Place } from '../places';
import type { Route } from '../router';
import { played, useRun } from '../story';
import { hex } from './MapPage';

/** What's in my circle: every useful spot from the game cache inside a run's circles right now. */
export default function CirclePage({ geo, route }: { geo: Geography; route: Route }) {
  const { run, error } = useRun(route.run);
  const [places, setPlaces] = useState<Place[] | null>(null);
  const [placesError, setPlacesError] = useState<string | null>(null);
  const [which, setWhich] = useState<number | null>(null);
  const [hidden, setHidden] = useState<Set<string>>(new Set());
  const scene = useRef<MapScene | null>(null);

  useEffect(() => {
    loadPlaces().then(setPlaces, (e: Error) => setPlacesError(e.message));
  }, []);

  const sim = useMemo(() => (run ? new NomadSim(geo, run.sim) : null), [geo, run]);
  const index = which ?? sim?.state.bound ?? MAIN;
  const circle = sim?.circles[index];
  const found = useMemo(() => (places && circle ? inside(places, circle) : []), [places, circle]);
  const shown = found.filter((p) => !hidden.has(p.kind));

  if (sim) {
    scene.current ??= { sim, player: run?.surface ?? null, forecast: null, fog: false, follow: true };
    scene.current.sim = sim;
    scene.current.focus = circle ? { x: circle.x, y: circle.y } : null;
    scene.current.extra = ({ ctx, at, scale }) => {
      for (const p of shown) {
        const [x, y] = at(p.x, p.y);
        ctx.beginPath();
        ctx.arc(x, y, Math.max(3, Math.min(6, scale * 1.2)), 0, Math.PI * 2);
        ctx.fillStyle = KIND_COLOURS[p.kind] ?? '#fff';
        ctx.fill();
        ctx.lineWidth = 1.5;
        ctx.strokeStyle = p.below ? '#5e35b1' : '#000';
        ctx.stroke();
      }
    };
  }

  if (!run) {
    return (
      <div className="page">
        <section className="osrs-win window">
          <h1 className="win-head osrs-title">What's in my circle</h1>
          <div className="win-body">
            <p>
              Paste a run code and this lists every bank, deposit box, altar, furnace, anvil, range, fairy ring, spinning wheel, fishing
              spot, mining site, rare tree, farming patch and dungeon entrance inside your circle right now. It's read from the game
              cache, so it keeps up when the game changes.
            </p>
            <RunCodeBox base="#/circle/" error={error} />
          </div>
        </section>
      </div>
    );
  }

  return (
    <div className="page-map-split">
      <div className="osrs-win map-frame">
        <MapCanvas scene={scene} zoom={3} className="map-main" />
      </div>
      <aside className="panel osrs-win">
        <div className="osrs-title">What's in my circle</div>
        <p className="muted small">
          {played(run.sim.minutes)} played. Shops and NPCs aren't in the list; everything here is a thing in the world.
        </p>
        {placesError && <div className="osrs-chat notice notice-error">{placesError}</div>}
        <label className="small muted" htmlFor="which-circle">
          Circle
        </label>
        <select id="which-circle" className="input" value={index} onChange={(e) => setWhich(Number(e.target.value))}>
          {sim!.circles.map((c, i) => (
            <option key={i} value={i}>
              {sim!.name(c)}
              {i === sim!.state.bound ? ' (yours)' : ''} · {Math.round(c.radius)} tiles
            </option>
          ))}
        </select>
        {!places ? (
          <p className="muted">Loading the places…</p>
        ) : found.length === 0 ? (
          <p className="muted">Nothing from the list stands inside this circle right now. It's wild country.</p>
        ) : (
          <>
            <div className="chip-row" role="group" aria-label="Show kinds">
              {byKind(found).map(([kind, list]) => (
                <button
                  key={kind}
                  type="button"
                  className={`btn btn-small${hidden.has(kind) ? '' : ' is-on'}`}
                  aria-pressed={!hidden.has(kind)}
                  onClick={() => {
                    const next = new Set(hidden);
                    if (next.has(kind)) next.delete(kind);
                    else next.add(kind);
                    setHidden(next);
                  }}
                >
                  <span className="dot" style={{ background: KIND_COLOURS[kind] }} /> {kind} {list.length}
                </button>
              ))}
            </div>
            {byKind(shown).map(([kind, list]) => (
              <section key={kind}>
                <h3>
                  {kind} ({list.length})
                </h3>
                <ul className="kind-list">
                  {list.map((p, k) => (
                    <li key={k}>
                      <span className="dot" style={{ background: KIND_COLOURS[kind] }} />
                      <span>
                        {p.label ?? kind}
                        {p.below && <span className="muted small"> (underground)</span>}
                      </span>
                      <span className="count">{Math.round(p.off)} tiles in</span>
                    </li>
                  ))}
                </ul>
              </section>
            ))}
          </>
        )}
        <p className="small muted">
          <span className="dot" style={{ background: hex(sim!.colour(circle!)) }} /> Underground spots are drawn over the ground above
          them, ringed in purple. Under the Below law what counts is where you went in.{' '}
          <a href={`#/run/${route.run}`}>Open this run on the map</a>
        </p>
      </aside>
    </div>
  );
}
