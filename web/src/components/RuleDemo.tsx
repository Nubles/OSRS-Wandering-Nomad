import { useEffect, useRef, useState } from 'react';
import type { Geography, Tile } from '../engine/geography';
import { contains, NomadSim } from '../engine/sim';
import { MapCanvas, MapScene } from './MapCanvas';

/** How each law's demo plays: speed in played minutes a second, and where you stand. */
const DEMOS: Record<string, { perSecond: number; stand: 'middle' | 'edge' | 'outside' | null; seed: number; warm: number; note: string }> = {
  Inside: { perSecond: 2, stand: 'middle', seed: 101, warm: 30, note: 'Tap the map to stand somewhere. White is inside your circle, red is outside.' },
  Drift: { perSecond: 60, stand: null, seed: 202, warm: 60, note: 'An hour a second. Main roams slowly; the kingdom circles patrol faster.' },
  Lean: { perSecond: 15, stand: 'edge', seed: 303, warm: 30, note: 'You stand at the edge. Watch the circle turn your way, and the edge slip off you as it does.' },
  Breathe: { perSecond: 120, stand: null, seed: 404, warm: 60, note: 'Two hours a second: every circle grows, shrinks and now and then pulses.' },
  Cross: { perSecond: 30, stand: null, seed: 505, warm: 240, note: 'A dashed edge is a circle you could cross into: stand in the overlap.' },
  Shore: { perSecond: 60, stand: null, seed: 606, warm: 120, note: 'Main turns back at the coast, and only crosses water by boat.' },
  Outside: { perSecond: 5, stand: 'outside', seed: 707, warm: 30, note: 'You stand outside: your circle drifts five times as fast and shrinks a tile a minute.' },
  Below: { perSecond: 10, stand: 'middle', seed: 808, warm: 30, note: 'The white dot is the entrance you went down. Underground, you count as standing there.' },
};

/** Where the demo puts you, from your circle now. */
function standing(sim: NomadSim, stand: 'middle' | 'edge' | 'outside'): Tile {
  const c = sim.bound;
  const h = c.heading + Math.PI / 2;
  const d = stand === 'middle' ? 0 : stand === 'edge' ? c.radius - 1 : c.radius + 6;
  return { x: Math.round(c.x + Math.cos(h) * d), y: Math.round(c.y + Math.sin(h) * d) };
}

/** A tiny live demo of one law, made from the same engine as the plugin. */
export function RuleDemo({ geo, law }: { geo: Geography; law: string }) {
  const demo = DEMOS[law];
  const scene = useRef<MapScene | null>(null);
  const [radius, setRadius] = useState(0);
  const [inside, setInside] = useState<boolean | null>(null);

  if (!scene.current && demo) {
    const sim = NomadSim.start(geo, demo.seed);
    for (let i = 0; i < demo.warm; i++) sim.step(null);
    scene.current = { sim, player: demo.stand ? standing(sim, demo.stand) : null, forecast: null, fog: law === 'Inside' || law === 'Outside', follow: true };
  }

  useEffect(() => {
    if (!demo) return;
    let frame = 0;
    let last = performance.now();
    let owed = 0;
    const loop = (now: number) => {
      frame = requestAnimationFrame(loop);
      const s = scene.current!;
      owed += ((now - last) / 1000) * demo.perSecond;
      last = now;
      while (owed >= 1) {
        s.sim.step(s.player);
        owed--;
      }
      // Outside keeps you outside, so the cost keeps showing; everything else leaves you where you are.
      if (demo.stand === 'outside' && s.player && contains(s.sim.bound, s.player.x, s.player.y)) s.player = standing(s.sim, 'outside');
      setRadius(Math.round(s.sim.bound.radius));
      setInside(s.player ? contains(s.sim.bound, s.player.x, s.player.y) : null);
    };
    frame = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frame);
  }, [demo]);

  if (!demo) return null;
  return (
    <div className="demo-box osrs-win">
      <MapCanvas
        scene={scene}
        zoom={law === 'Drift' || law === 'Shore' || law === 'Cross' ? 0.6 : 2.2}
        onPick={law === 'Inside' ? (t) => scene.current && (scene.current.player = t) : undefined}
        className="map-main"
      />
      <p className="small muted" style={{ padding: '4px 8px', margin: 0 }}>
        {demo.note} Your circle: {radius} tiles
        {inside === null ? '' : inside ? ', you are inside.' : ', you are outside.'}
      </p>
    </div>
  );
}
