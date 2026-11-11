import { useEffect, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography } from '../engine/geography';
import { NomadSim } from '../engine/sim';
import { ago, fetchShared, Grave } from '../live';
import { played } from '../story';

/** A grave in the 3D world viewer: the camera flies to the tile and shows the circle as it was, fading. */
export function graveLink(g: Grave): string {
  const q = new URLSearchParams({ grave: [g.x, g.y, g.items, g.minutes, g.cx, g.cy, g.r].join(',') });
  if (g.name) q.set('name', g.name);
  return `${import.meta.env.BASE_URL}world/?${q}`;
}

/** The Graveyard: a shared map of opted-in deaths, each a tombstone with its circle and an epitaph. */
export default function Graveyard({ geo }: { geo: Geography }) {
  const [graves, setGraves] = useState<Grave[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [picked, setPicked] = useState<number | null>(null);
  const gravesRef = useRef<Grave[]>([]);
  const pickedRef = useRef<number | null>(null);
  pickedRef.current = picked;
  const scene = useRef<MapScene | null>(null);
  scene.current ??= { sim: NomadSim.start(geo, 1), player: null, forecast: null, fog: false, follow: true, bare: true, focus: { x: 3000, y: 3300 } };

  useEffect(() => {
    fetchShared<{ graves: Grave[] }>('/g').then(
      (got) => {
        setGraves(got.graves);
        gravesRef.current = got.graves;
      },
      (e: Error) => setError(e.message),
    );
  }, []);

  const pick = (i: number) => {
    setPicked(i);
    const g = gravesRef.current[i];
    if (scene.current && g) {
      scene.current.focus = { x: g.x, y: g.y };
      scene.current.follow = true;
    }
  };

  scene.current.extra = ({ ctx, at, scale, t, text }) => {
    gravesRef.current.forEach((g, i) => {
      const on = pickedRef.current === i;
      const [x, y] = at(g.x, g.y);
      if (on) {
        const [cx, cy] = at(g.cx, g.cy);
        ctx.beginPath();
        ctx.arc(cx, cy, g.r * scale, 0, Math.PI * 2);
        ctx.setLineDash([6, 5]);
        ctx.strokeStyle = `rgba(232,217,168,${0.4 + 0.3 * Math.sin(t / 900)})`;
        ctx.lineWidth = 2;
        ctx.stroke();
        ctx.setLineDash([]);
      }
      // A little headstone.
      const s = on ? 1.6 : 1;
      ctx.fillStyle = g.hardcore ? '#d9d4c7' : '#b8b2a6';
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.moveTo(x - 4 * s, y + 5 * s);
      ctx.lineTo(x - 4 * s, y - 2 * s);
      ctx.arc(x, y - 2 * s, 4 * s, Math.PI, 0);
      ctx.lineTo(x + 4 * s, y + 5 * s);
      ctx.closePath();
      ctx.fill();
      ctx.stroke();
      if (g.hardcore) {
        ctx.fillStyle = '#d93a2b';
        ctx.fillRect(x - 1 * s, y - 3 * s, 2 * s, 5 * s);
      }
      if (on) text(g.name ?? 'A Nomad', x, y - 12 * s, '#e8d9a8', 'RSBold');
    });
  };

  const g = picked !== null ? graves?.[picked] : undefined;
  return (
    <div className="page-map-split">
      <div className="osrs-win map-frame">
        <MapCanvas
          scene={scene}
          zoom={0.6}
          onPick={(tile) => {
            const near = gravesRef.current
              .map((gr, i) => ({ i, d: Math.hypot(gr.x - tile.x, gr.y - tile.y) }))
              .sort((a, b) => a.d - b.d)[0];
            if (near && near.d < 30) pick(near.i);
          }}
          className="map-main"
        />
      </div>
      <aside className="panel osrs-win">
        <div className="osrs-title">The Graveyard</div>
        <p className="muted small">
          Deaths from runs whose players chose to rest here. Nothing is ranked: each stone is a story. A red mark is a Hardcore death.
        </p>
        {error && <div className="osrs-chat notice notice-error">{error}</div>}
        {g && (
          <div className="osrs-chat notice">
            <strong>{g.name ?? 'A Nomad'}</strong>
            {g.hardcore ? ' (Hardcore)' : ''} fell in {g.kingdom ?? 'the open'} at {played(g.minutes)} played, inside {g.circle ?? 'their circle'}
            . {g.epitaph}
            <br />
            <a href={graveLink(g)} target="_blank" rel="noopener">
              Visit the grave in 3D
            </a>{' '}
            <span className="muted small">(the game world viewer, about 120MB the first time; it shows the pile's {g.items} items as a beam)</span>
          </div>
        )}
        {graves === null && !error && <p className="muted">Reading the stones…</p>}
        {graves && graves.length === 0 && <p>No stones yet. May it stay that way.</p>}
        {graves && graves.length > 0 && (
          <ul className="kind-list">
            {graves.map((gr, i) => (
              <li key={i}>
                <button type="button" className="link" style={{ marginLeft: 0 }} onClick={() => pick(i)}>
                  {gr.name ?? 'A Nomad'}
                </button>
                <span className="muted small">
                  {gr.kingdom ?? 'the open'} · {played(gr.minutes)}
                </span>
                <span className="count">{ago(Date.now() - gr.at)}</span>
              </li>
            ))}
          </ul>
        )}
        <h3>Rest here</h3>
        <p className="small">
          In the plugin's settings under <strong>Website</strong>, turn on <strong>Rest in the Graveyard</strong>. When you die, a stone
          goes up with the tile, the played time, your circle and an epitaph from your ledger. A <strong>Lobby name</strong> puts your
          name on it; without one it reads "A Nomad".
        </p>
      </aside>
    </div>
  );
}
