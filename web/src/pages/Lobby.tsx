import { useEffect, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography, Tile } from '../engine/geography';
import { NomadSim } from '../engine/sim';
import { ago, fetchShared, LobbyRun } from '../live';

const REFRESH_MS = 60_000;

/** The Nomad lobby: every live run that joined it, as a small breathing circle to click and watch. */
export default function Lobby({ geo }: { geo: Geography }) {
  const [runs, setRuns] = useState<LobbyRun[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const runsRef = useRef<LobbyRun[]>([]);
  const scene = useRef<MapScene | null>(null);
  scene.current ??= { sim: NomadSim.start(geo, 1), player: null, forecast: null, fog: false, follow: true, bare: true, focus: { x: 2900, y: 3300 } };

  useEffect(() => {
    let live = true;
    let timer = 0;
    const poll = async () => {
      try {
        const got = await fetchShared<{ runs: LobbyRun[] }>('/lobby');
        if (!live) return;
        setRuns(got.runs);
        runsRef.current = got.runs;
        setError(null);
      } catch (e) {
        if (live) setError((e as Error).message);
      }
      if (live) timer = window.setTimeout(poll, REFRESH_MS);
    };
    poll();
    return () => {
      live = false;
      clearTimeout(timer);
    };
  }, []);

  scene.current.extra = ({ ctx, at, scale, t, text }) => {
    runsRef.current.forEach((r, i) => {
      const [x, y] = at(r.x, r.y);
      const breathe = 0.5 + 0.5 * Math.sin(t / 900 + i * 1.7);
      const radius = Math.max(6, r.r * scale) + breathe * 3;
      const colour = r.online ? '#ff981f' : '#a09580';
      ctx.beginPath();
      ctx.arc(x, y, radius, 0, Math.PI * 2);
      ctx.fillStyle = r.online ? 'rgba(255,152,31,0.18)' : 'rgba(160,149,128,0.12)';
      ctx.fill();
      ctx.lineWidth = 2;
      ctx.strokeStyle = colour;
      ctx.stroke();
      text(r.name ?? 'A Nomad', x, y - radius - 5, colour, 'RSBold');
    });
  };

  const pick = (tile: Tile) => {
    const v = runsRef.current
      .map((r) => ({ r, d: Math.hypot(r.x - tile.x, r.y - tile.y) }))
      .filter(({ r, d }) => d <= Math.max(r.r, 25))
      .sort((a, b) => a.d - b.d)[0];
    if (v) location.hash = `#/live/${v.r.code}`;
  };

  return (
    <div className="page-map-split">
      <div className="osrs-win map-frame">
        <MapCanvas scene={scene} zoom={0.35} onPick={pick} className="map-main" />
      </div>
      <aside className="panel osrs-win">
        <div className="osrs-title">The Nomad lobby</div>
        <p className="muted small">
          Every live run that joined the lobby, where its circle is now. Click a circle to watch it. It refreshes every minute.
        </p>
        {error && <div className="osrs-chat notice notice-error">{error}</div>}
        {runs === null && !error && <p className="muted">Looking for live runs…</p>}
        {runs && runs.length === 0 && <p>Nobody is in the lobby right now. Check back later, or join it yourself.</p>}
        {runs && runs.length > 0 && (
          <ul className="kind-list">
            {runs.map((r) => (
              <li key={r.code}>
                <span className="dot" style={{ background: r.online ? '#ff981f' : '#a09580' }} />
                <a href={`#/live/${r.code}`}>{r.name ?? 'A Nomad'}</a>
                <span className="muted small">
                  {geo.regionAt(r.x, r.y)?.name ?? 'at sea'}
                  {r.mood ? `, ${r.mood.toLowerCase()}` : ''}
                </span>
                <span className="count">{r.online ? 'live' : ago(Date.now() - r.at)}</span>
              </li>
            ))}
          </ul>
        )}
        <h3>Join the lobby</h3>
        <p className="small">
          In the plugin's settings, turn on <strong>Share live on the website</strong>, then under <strong>Website</strong> turn on{' '}
          <strong>Join the Nomad lobby</strong>. A <strong>Lobby name</strong> is optional; leave it empty to show as "A Nomad". Both
          are off until you turn them on.
        </p>
      </aside>
    </div>
  );
}
