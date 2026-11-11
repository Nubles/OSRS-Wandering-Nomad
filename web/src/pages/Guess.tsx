import { lazy, Suspense, useMemo, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import type { Geography, Tile } from '../engine/geography';
import { NomadSim } from '../engine/sim';
import { GUESS_ROUNDS, guessDay, guessRound, guessScore, guessSquare } from '../story';

const Map3D = lazy(() => import('../components/Map3D'));

interface Result {
  score: number;
  off: number;
}

const key = (day: number) => `nomad.guess.${day}`;

function saved(day: number): Result[] {
  try {
    const v = JSON.parse(localStorage.getItem(key(day)) ?? '[]');
    return Array.isArray(v) ? v.slice(0, GUESS_ROUNDS) : [];
  } catch {
    return [];
  }
}

/** The Circle Guesser: the camera turns inside five circles from today's seeds, and you pin where each one is. */
export default function Guess({ geo }: { geo: Geography }) {
  const day = guessDay();
  const [results, setResults] = useState<Result[]>(() => saved(day));
  const [pin, setPin] = useState<Tile | null>(null);
  const [revealed, setRevealed] = useState<Result | null>(null);
  const [copied, setCopied] = useState(false);
  const round = Math.min(results.length - (revealed ? 1 : 0), GUESS_ROUNDS - 1);
  const done = results.length >= GUESS_ROUNDS && !revealed;

  const target = useMemo(() => {
    const { seed, minutes } = guessRound(day, round);
    const sim = NomadSim.start(geo, seed);
    for (let i = 0; i < minutes; i++) sim.step(null);
    return sim;
  }, [geo, day, round]);

  const view3d = useRef<MapScene | null>(null);
  view3d.current = { sim: target, player: null, forecast: null, fog: true, follow: true };

  const board = useRef<MapScene | null>(null);
  board.current ??= { sim: target, player: null, forecast: null, fog: false, follow: false, bare: true, focus: { x: 2800, y: 3300 } };
  const main = target.main;
  board.current.extra = ({ ctx, at, scale, text }) => {
    if (revealed) {
      const [cx, cy] = at(main.x, main.y);
      ctx.beginPath();
      ctx.arc(cx, cy, main.radius * scale, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(255,152,31,0.2)';
      ctx.fill();
      ctx.strokeStyle = '#ff981f';
      ctx.lineWidth = 3;
      ctx.stroke();
      text('The circle', cx, cy - main.radius * scale - 6, '#ff981f', 'RSBold');
      if (pin) {
        const [px, py] = at(pin.x, pin.y);
        ctx.setLineDash([5, 4]);
        ctx.beginPath();
        ctx.moveTo(px, py);
        ctx.lineTo(cx, cy);
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1.5;
        ctx.stroke();
        ctx.setLineDash([]);
      }
    }
    if (pin) {
      const [px, py] = at(pin.x, pin.y);
      ctx.beginPath();
      ctx.arc(px, py, 6, 0, Math.PI * 2);
      ctx.fillStyle = '#ffd54f';
      ctx.fill();
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 2;
      ctx.stroke();
    }
  };

  const guess = () => {
    if (!pin) return;
    const off = Math.hypot(pin.x - main.x, pin.y - main.y);
    const r = { score: guessScore(off, main.radius), off: Math.round(off) };
    const next = [...results, r];
    setResults(next);
    setRevealed(r);
    if (board.current) {
      board.current.focus = { x: main.x, y: main.y };
      board.current.follow = true;
    }
    try {
      localStorage.setItem(key(day), JSON.stringify(next));
    } catch {
      /* not kept: a refresh starts the day again */
    }
  };

  const next = () => {
    setRevealed(null);
    setPin(null);
    if (board.current) board.current.follow = false;
  };

  const total = results.reduce((a, r) => a + r.score, 0);
  const share = `Nomad Circle Guesser #${day} · ${total.toLocaleString()}/${(GUESS_ROUNDS * 1000).toLocaleString()}\n${results
    .map((r) => guessSquare(r.score))
    .join('')}\n${location.origin}${location.pathname}#/guess`;

  return (
    <div className="page">
      <section className="osrs-win window">
        <h1 className="win-head osrs-title">
          Circle Guesser #{day} · circle {Math.min(results.length + (revealed || done ? 0 : 1), GUESS_ROUNDS)} of {GUESS_ROUNDS}
        </h1>
        <div className="win-body">
          {done ? (
            <div>
              <p className="prophecy-line">{total.toLocaleString()} points</p>
              <p className="grid-share" aria-label="Your results">
                {results.map((r) => guessSquare(r.score)).join('')}
              </p>
              <ul className="kind-list">
                {results.map((r, i) => (
                  <li key={i}>
                    Circle {i + 1}: {r.off === 0 ? 'right on it' : `${r.off.toLocaleString()} tiles off`}
                    <span className="count">{r.score}</span>
                  </li>
                ))}
              </ul>
              <div className="row">
                <button
                  type="button"
                  className="btn btn-big"
                  onClick={() =>
                    navigator.clipboard.writeText(share).then(
                      () => setCopied(true),
                      () => setCopied(false),
                    )
                  }
                >
                  {copied ? 'Copied' : 'Copy your result'}
                </button>
                <span className="muted small">New circles at midnight UTC. Everyone gets the same five.</span>
              </div>
            </div>
          ) : (
            <>
              <p className="muted small">
                The left view turns slowly inside a Main circle from one of today's seeds. Drag it to look around. Pin where you think it
                is on the map to the right, then press Guess. Inside the circle scores 1,000.
              </p>
              <div className="guess-stage">
                <div className="osrs-win map-frame">
                  <Suspense fallback={<div className="loading">Loading the 3D view…</div>}>
                    <Map3D key={round} scene={view3d} className="map-main" spin reach={260} />
                  </Suspense>
                </div>
                <div className="osrs-win map-frame">
                  <MapCanvas scene={board} zoom={0.3} onPick={(t) => !revealed && setPin(t)} className="map-main" />
                </div>
              </div>
              <div className="row" style={{ marginTop: 8 }}>
                {revealed ? (
                  <>
                    <span className="value">
                      {guessSquare(revealed.score)} {revealed.off.toLocaleString()} tiles off: {revealed.score} points
                    </span>
                    <button type="button" className="btn btn-big" onClick={next}>
                      {results.length >= GUESS_ROUNDS ? 'See your day' : 'Next circle'}
                    </button>
                  </>
                ) : (
                  <button type="button" className="btn btn-big" disabled={!pin} onClick={guess}>
                    Guess
                  </button>
                )}
              </div>
            </>
          )}
        </div>
      </section>
    </div>
  );
}
