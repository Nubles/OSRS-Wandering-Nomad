import { useEffect, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography } from '../engine/geography';
import { NomadSim } from '../engine/sim';
import { formatPlayed } from './MapPage';
import { LAWS } from './laws';

/** Everyone who opens the site on the same day watches the same world. */
function todaysSeed(): number {
  const d = new Date();
  return d.getUTCFullYear() * 10000 + (d.getUTCMonth() + 1) * 100 + d.getUTCDate();
}

export function Home({ geo }: { geo: Geography }) {
  const scene = useRef<MapScene | null>(null);
  const [played, setPlayed] = useState(0);

  if (!scene.current) {
    const sim = NomadSim.start(geo, todaysSeed());
    // Skip ahead a little so the circles have already spread out.
    for (let i = 0; i < 240; i++) sim.step(null);
    scene.current = { sim, player: null, forecast: null, fog: false, follow: true };
  }

  useEffect(() => {
    let frame = 0;
    let last = performance.now();
    let owed = 0;
    const loop = (now: number) => {
      frame = requestAnimationFrame(loop);
      owed += ((now - last) / 1000) * 20;
      last = now;
      const s = scene.current!;
      while (owed >= 1) {
        s.sim.step(null);
        owed--;
      }
      setPlayed(s.sim.state.minutes);
    };
    frame = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(frame);
  }, []);

  return (
    <>
      <section className="hero">
        <MapCanvas scene={scene} zoom={1.3} interactive={false} className="hero-map" />
        <div className="hero-shade" />
        <div className="hero-copy">
          <p className="eyebrow">An Old School RuneScape game mode</p>
          <h1>Nomad UIM</h1>
          <p className="lede">Ultimate Ironman, but you can only play inside circles that drift across Gielinor.</p>
          <div className="row">
            <a className="btn btn-primary" href="#/map">
              Explore the circles
            </a>
            <a className="btn" href="#/rules">
              Read the rules
            </a>
          </div>
          <p className="muted small hero-caption">
            Today's world, seed {scene.current.sim.state.seed}, at {formatPlayed(played)} played. Everyone who visits today sees the same
            one.
          </p>
        </div>
      </section>

      <section className="content">
        <h2>Seven laws, one sentence each</h2>
        <div className="laws">
          {LAWS.map((l) => (
            <article key={l.name} className="law">
              <h3>{l.name}</h3>
              <p>{l.short}</p>
            </article>
          ))}
        </div>
      </section>

      <section className="content split">
        <div>
          <h2>Every session has a story</h2>
          <p>
            Main drifts about 15 tiles an hour. Each kingdom has its own faster circle that never leaves home. When Main touches one, you
            can cross in, but the only way out is back into Main. If Main wanders off while you're inside, you're stranded until it comes
            back.
          </p>
          <p>
            Boats carry Main between ports, Achievement Diaries permanently grow a kingdom's circle, and the Inferno only opens when Main
            drifts over the volcano. Nothing is ever locked. Your circle just has to be there.
          </p>
        </div>
        <div className="card">
          <h3>Play it with the plugin</h3>
          <ol className="steps">
            <li>Install Nomad UIM in RuneLite.</li>
            <li>Make an Ultimate Ironman and log in. The run starts at Lumbridge.</li>
            <li>Follow the edge on the ground, the minimap and the world map.</li>
            <li>
              Press <strong>View on website</strong> in the sidebar to open your run here.
            </li>
          </ol>
          <a className="btn" href="#/plugin">
            Plugin guide
          </a>
        </div>
      </section>
    </>
  );
}
