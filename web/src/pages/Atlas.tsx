import { useEffect, useMemo, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography } from '../engine/geography';
import { NomadSim } from '../engine/sim';
import { AtlasSummary, fetchShared } from '../live';

/** The plugin's kingdoms in its own order, the order the atlas counts them in. */
const KINGDOMS = ['Kandarin', 'Fremennik', 'Asgarnia', 'Misthalin', 'Kharidian Desert', 'Morytania', 'Wilderness', 'Karamja', 'Kourend & Kebos', 'Tirannwn', 'Varlamore'];

/** Whether a 64-tile map region has any land in it (sampled every 8 tiles). */
function hasLand(geo: Geography, rx: number, ry: number): boolean {
  for (let dx = 4; dx < 64; dx += 8) for (let dy = 4; dy < 64; dy += 8) if (geo.onLand(rx * 64 + dx, ry * 64 + dy)) return true;
  return false;
}

/** The atlas of wandering: where opted-in runs' circles have taken people, and where no Nomad has stood. */
export default function Atlas({ geo }: { geo: Geography }) {
  const [atlas, setAtlas] = useState<AtlasSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [unvisitedOnly, setUnvisitedOnly] = useState(false);
  const scene = useRef<MapScene | null>(null);
  scene.current ??= { sim: NomadSim.start(geo, 1), player: null, forecast: null, fog: false, follow: false, bare: true, focus: { x: 2700, y: 3300 } };

  useEffect(() => {
    fetchShared<AtlasSummary>('/a').then(setAtlas, (e: Error) => setError(e.message));
  }, []);

  const cells = useMemo(() => {
    if (!atlas) return [];
    const out: { rx: number; ry: number; n: number }[] = [];
    const { x0, y0, cols, rows } = atlas.grid;
    for (let gy = 0; gy < rows; gy++) {
      for (let gx = 0; gx < cols; gx++) {
        const rx = x0 + gx;
        const ry = y0 + gy;
        if (!hasLand(geo, rx, ry)) continue;
        out.push({ rx, ry, n: atlas.squares[gy * cols + gx] ?? 0 });
      }
    }
    return out;
  }, [atlas, geo]);
  const most = Math.max(1, ...cells.map((c) => c.n));
  const untouched = cells.filter((c) => c.n === 0);
  const untouchedBy = useMemo(() => {
    const m = new Map<string, number>();
    for (const c of untouched) {
      const name = geo.regionAt(c.rx * 64 + 32, c.ry * 64 + 32)?.name ?? 'Elsewhere';
      m.set(name, (m.get(name) ?? 0) + 1);
    }
    return [...m.entries()].sort((a, b) => b[1] - a[1]);
  }, [untouched, geo]);

  scene.current.extra = ({ ctx, at }) => {
    for (const c of cells) {
      if (unvisitedOnly && c.n > 0) continue;
      const [x0, y0] = at(c.rx * 64, (c.ry + 1) * 64);
      const [x1, y1] = at((c.rx + 1) * 64, c.ry * 64);
      if (c.n === 0) {
        ctx.strokeStyle = 'rgba(201,166,255,0.7)';
        ctx.lineWidth = 1;
        ctx.setLineDash([3, 3]);
        ctx.strokeRect(x0 + 1, y0 + 1, x1 - x0 - 2, y1 - y0 - 2);
        ctx.setLineDash([]);
        continue;
      }
      const k = Math.sqrt(c.n / most);
      ctx.fillStyle = `rgba(255, ${Math.round(213 - 120 * k)}, ${Math.round(79 - 60 * k)}, ${0.2 + 0.5 * k})`;
      ctx.fillRect(x0, y0, x1 - x0, y1 - y0);
    }
  };

  return (
    <div className="page-map-split">
      <div className="osrs-win map-frame">
        <MapCanvas scene={scene} zoom={0.4} className="map-main" />
      </div>
      <aside className="panel osrs-win">
        <div className="osrs-title">Atlas of wandering</div>
        <p className="muted small">
          Each square is one of the map's 64-tile regions. Gold squares are where opted-in runs' circles have taken their players, brighter
          for more runs. Dashed purple squares are land no Nomad has stood in yet.
        </p>
        {error && <div className="osrs-chat notice notice-error">{error}</div>}
        {!atlas && !error && <p className="muted">Gathering the maps…</p>}
        {atlas && (
          <>
            <p>
              <span className="value">{atlas.runs}</span> {atlas.runs === 1 ? 'run has' : 'runs have'} shared their wandering.{' '}
              <span className="value">{untouched.length}</span> regions of land are still untouched.
            </p>
            <label className="check">
              <input type="checkbox" checked={unvisitedOnly} onChange={(e) => setUnvisitedOnly(e.target.checked)} /> Only the untouched
              regions
            </label>
            <h3>Runs that reached each kingdom</h3>
            <ul className="kind-list">
              {KINGDOMS.map((k, i) => (
                <li key={k}>
                  <span>{k}</span>
                  <span className="count">{atlas.kingdoms[i] ?? 0}</span>
                </li>
              ))}
            </ul>
            {untouchedBy.length > 0 && (
              <>
                <h3>Where no Nomad has stood</h3>
                <ul className="kind-list">
                  {untouchedBy.map(([name, n]) => (
                    <li key={name}>
                      <span>{name}</span>
                      <span className="count">{n} regions</span>
                    </li>
                  ))}
                </ul>
              </>
            )}
          </>
        )}
        <h3>Add your run</h3>
        <p className="small">
          In the plugin's settings under <strong>Website</strong>, turn on <strong>Add to the atlas of wandering</strong>. Once an hour it
          sends which kingdoms and map regions you've walked inside your circle: no tiles and no times.
        </p>
      </aside>
    </div>
  );
}
