import { useEffect, useMemo, useRef, useState } from 'react';
import type { Geography } from '../engine/geography';
import type { Route } from '../router';
import { prophecy } from '../story';

const W = 1200;
const H = 630;

function wrap(ctx: CanvasRenderingContext2D, text: string, width: number): string[] {
  const lines: string[] = [];
  let line = '';
  for (const word of text.split(' ')) {
    const next = line ? `${line} ${word}` : word;
    if (ctx.measureText(next).width > width && line) {
      lines.push(line);
      line = word;
    } else {
      line = next;
    }
  }
  if (line) lines.push(line);
  return lines;
}

/** The card: the seed, its prophecy and a ring like Main's, on the site's dark parchment. */
function drawCard(canvas: HTMLCanvasElement, seed: number, line: string): void {
  canvas.width = W;
  canvas.height = H;
  const ctx = canvas.getContext('2d')!;
  const g = ctx.createRadialGradient(W * 0.75, H / 2, 40, W * 0.75, H / 2, 520);
  g.addColorStop(0, '#3a2a16');
  g.addColorStop(1, '#1b1712');
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, W, H);
  ctx.strokeStyle = '#ff981f';
  ctx.lineWidth = 6;
  ctx.strokeRect(3, 3, W - 6, H - 6);
  for (let k = 0; k < 3; k++) {
    ctx.beginPath();
    ctx.arc(W * 0.78, H / 2, 150 + k * 34, 0, Math.PI * 2);
    ctx.strokeStyle = `rgba(255,152,31,${0.7 - k * 0.22})`;
    ctx.lineWidth = 5 - k;
    ctx.stroke();
  }
  ctx.fillStyle = '#ffd54f';
  ctx.font = "56px 'RSBold', sans-serif";
  ctx.fillText('Nomad UIM', 60, 110);
  ctx.fillStyle = '#a09580';
  ctx.font = "34px 'RS', sans-serif";
  ctx.fillText(`Seed ${seed}: a prophecy`, 60, 165);
  ctx.fillStyle = '#e8d9a8';
  ctx.font = "44px 'RSBold', sans-serif";
  wrap(ctx, line, 640).forEach((l, i) => ctx.fillText(l, 60, 260 + i * 56));
  ctx.fillStyle = '#a09580';
  ctx.font = "28px 'RS', sans-serif";
  ctx.fillText('Play the seed and see if it comes true.', 60, H - 50);
}

/** Seed prophecy: roll a seed, read a vague line about its opening hours, and share it as a card. */
export default function Prophecy({ geo, route }: { geo: Geography; route: Route }) {
  const fromLink = Number(route.params.get('seed'));
  const [seed, setSeed] = useState(() => (route.params.has('seed') && Number.isFinite(fromLink) ? fromLink | 0 : roll()));
  const [input, setInput] = useState('');
  const [copied, setCopied] = useState<string | null>(null);
  const line = useMemo(() => prophecy(geo, seed), [geo, seed]);
  const ref = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (ref.current) document.fonts.ready.then(() => ref.current && drawCard(ref.current, seed, line));
  }, [seed, line]);

  const link = `${location.origin}${location.pathname}#/prophecy?seed=${seed}`;
  const save = () =>
    ref.current?.toBlob((blob) => {
      if (!blob) return;
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `nomad-prophecy-${seed}.png`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    });

  return (
    <div className="page">
      <section className="osrs-win window">
        <h1 className="win-head osrs-title">Seed prophecy</h1>
        <div className="win-body">
          <p className="muted small">
            Every run grows from a seed. This one reads the seed's first six hours, with Main left to itself, and speaks of them in vague
            words: a direction, a kind of country, a split, salt or storm. Never a tile.
          </p>
          <p className="prophecy-line">"{line}"</p>
          <canvas ref={ref} style={{ width: '100%', maxWidth: 600, aspectRatio: '1200 / 630' }} role="img" aria-label={`Seed ${seed}: ${line}`} />
          <div className="row" style={{ marginTop: 8 }}>
            <button type="button" className="btn btn-big" onClick={() => setSeed(roll())}>
              Roll another seed
            </button>
            <button type="button" className="btn" onClick={save}>
              Save the card
            </button>
            <button
              type="button"
              className="btn"
              onClick={() => navigator.clipboard.writeText(`Seed ${seed}: "${line}"\n${link}`).then(() => setCopied('Copied.'), () => setCopied(link))}
            >
              Copy as text
            </button>
            <a className="btn" href={`#/map?seed=${seed}`}>
              Play this seed
            </a>
          </div>
          {copied && <p className="muted small break">{copied}</p>}
          <div className="row" style={{ marginTop: 8 }}>
            <input className="input" aria-label="A seed to read" placeholder="Or type a seed" value={input} onChange={(e) => setInput(e.target.value.replace(/[^0-9-]/g, ''))} />
            <button type="button" className="btn" disabled={!input} onClick={() => setSeed(Number(input) | 0)}>
              Read it
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}

function roll(): number {
  return Math.floor(Math.random() * 1_000_000_000);
}
