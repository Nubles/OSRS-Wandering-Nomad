import { useEffect, useMemo, useRef, useState } from 'react';
import { MapCanvas, MapScene } from '../components/MapCanvas';
import { RunCodeBox } from '../components/RunCodeBox';
import { Vignette, VignetteMark } from '../components/Vignette';
import type { Geography } from '../engine/geography';
import type { SavedRun } from '../engine/runcode';
import { compass, NomadSim } from '../engine/sim';
import type { Route } from '../router';
import { Chapter, chapters, drawSigil, ghostPath, played, report, sigil, useRun } from '../story';

type Tab = 'saga' | 'report' | 'sigil' | 'forks';
const TABS: { id: Tab; label: string }[] = [
  { id: 'saga', label: 'The Saga' },
  { id: 'report', label: 'Report card' },
  { id: 'sigil', label: 'Footprint sigil' },
  { id: 'forks', label: 'The other forks' },
];

/** A run's story: the Saga, its report card, its sigil and the forks it never kept. */
export default function Saga({ geo, route }: { geo: Geography; route: Route }) {
  const { run, error } = useRun(route.run);
  const [tab, setTab] = useState<Tab>('saga');

  if (!run) {
    return (
      <div className="page">
        <section className="osrs-win window">
          <h1 className="win-head osrs-title">The Saga</h1>
          <div className="win-body">
            <p>
              Your run as a storybook: a chapter for each act, a little map of where Main went, and the big moments in order. Long quiet
              stretches fold into a line. It also has a one-screen report card, a pixel crest from your footprint, and the forks you
              never kept, still drifting.
            </p>
            <RunCodeBox base="#/saga/" error={error} />
          </div>
        </section>
      </div>
    );
  }

  const story = run.story;
  return (
    <div className="page">
      <section className="osrs-win window">
        <h1 className="win-head osrs-title">
          {run.epithet ? `The run ${run.epithet}` : 'The Saga'} · {played(run.sim.minutes)} played · seed {run.sim.seed}
        </h1>
        <div className="win-body">
          <div className="tabs" role="tablist">
            {TABS.map((t) => (
              <button key={t.id} type="button" role="tab" className={`btn${tab === t.id ? ' is-on' : ''}`} aria-selected={tab === t.id} onClick={() => setTab(t.id)}>
                {t.label}
              </button>
            ))}
            <a className="btn" href={`#/run/${route.run}`}>
              Open on the map
            </a>
          </div>
          {!story ? (
            <div className="osrs-chat notice">
              This run code has no story in it. Copy a fresh one from the plugin's sidebar (Copy run code): newer versions add the run's
              acts, moments, forks and footprint for these pages.
            </div>
          ) : tab === 'saga' ? (
            <SagaBook run={run} />
          ) : tab === 'report' ? (
            <ReportCard run={run} />
          ) : tab === 'sigil' ? (
            <Sigil run={run} />
          ) : (
            <OtherForks geo={geo} run={run} />
          )}
        </div>
      </section>
    </div>
  );
}

function SagaBook({ run }: { run: SavedRun }) {
  const list = useMemo(() => chapters(run.story!, run.sim.minutes), [run]);
  return (
    <>
      {list.map((c) => (
        <ChapterView key={c.start} chapter={c} />
      ))}
      <p className="muted small">The end, for now. {played(run.sim.minutes)} played.</p>
    </>
  );
}

function ChapterView({ chapter }: { chapter: Chapter }) {
  const path = useMemo(() => chapter.track.map((t) => [t[1], t[2]] as [number, number]), [chapter]);
  const steps = useMemo(() => chapter.track.filter((t) => t[4] >= 0).map((t) => [t[4], t[5]] as [number, number]), [chapter]);
  const marks = useMemo(
    () =>
      chapter.lines
        .filter((l) => l.x !== undefined && l.y !== undefined)
        .map(
          (l): VignetteMark => ({
            x: l.x!,
            y: l.y!,
            colour: l.kind === 'death' ? '#ff5252' : l.kind === 'parting' ? '#c9a6ff' : '#ffd54f',
            shape: l.kind === 'death' ? 'cross' : l.kind === 'parting' ? 'ring' : 'diamond',
          }),
        ),
    [chapter],
  );
  return (
    <article className="saga-chapter osrs-inner" style={{ padding: 10, marginBottom: 8 }}>
      <Vignette path={path} steps={steps} marks={marks} label={`Where Main went in ${chapter.title}`} />
      <div>
        <h2 className="osrs-title">{chapter.title}</h2>
        <p className="muted small">
          {played(chapter.start)} to {played(chapter.end)}
          {chapter.opened ? ` · ${chapter.opened}` : ''}
        </p>
        <ol className="saga-lines">
          {chapter.lines.length === 0 && <li className="quiet">The circles drift, and nothing breaks the quiet.</li>}
          {chapter.lines.map((l, k) => (
            <li key={k} className={l.kind === 'quiet' ? 'quiet' : l.kind === 'death' ? 'death' : ''}>
              {l.kind !== 'quiet' && <span className="chat-time">[{played(l.minute)}]</span>} {l.text}
            </li>
          ))}
        </ol>
      </div>
    </article>
  );
}

function ReportCard({ run }: { run: SavedRun }) {
  const r = report(run)!;
  return (
    <div>
      <div className="stat-grid">
        <div className="stat osrs-inner">
          <div className="muted small">Played</div>
          <div className="big">{r.played}</div>
        </div>
        <div className="stat osrs-inner">
          <div className="muted small">Smallest circle survived</div>
          <div className="big">{r.smallest ? `${r.smallest} tiles` : 'n/a'}</div>
        </div>
        {r.counts.map((c) => (
          <div key={c.label} className="stat osrs-inner">
            <div className="muted small">{c.label}</div>
            <div className="big">{c.value.toLocaleString()}</div>
          </div>
        ))}
      </div>
      {r.closest && (
        <div className="osrs-chat notice">
          <strong>Closest escape:</strong> {r.closest}
        </div>
      )}
      <div className="split" style={{ marginTop: 8 }}>
        <Shares title="Hours per kingdom" rows={r.kingdoms} />
        <Shares title="Main's moods" rows={r.moods} />
      </div>
    </div>
  );
}

function Shares({ title, rows }: { title: string; rows: { name: string; hours: number; share: number }[] }) {
  return (
    <section className="osrs-inner" style={{ padding: 10 }}>
      <h3>{title}</h3>
      {rows.length === 0 && <p className="muted small">Nothing counted yet.</p>}
      <ul className="kind-list">
        {rows.map((k) => (
          <li key={k.name} style={{ display: 'block' }}>
            <div className="row" style={{ justifyContent: 'space-between' }}>
              <span>{k.name}</span>
              <span className="count">
                {k.hours}h · {Math.round(k.share * 100)}%
              </span>
            </div>
            <div className="bar">
              <span style={{ width: `${Math.max(2, k.share * 100)}%` }} />
            </div>
          </li>
        ))}
      </ul>
    </section>
  );
}

function Sigil({ run }: { run: SavedRun }) {
  const ref = useRef<HTMLCanvasElement>(null);
  const art = useMemo(() => sigil(run.story?.squares ?? [], run.sim.seed), [run]);
  useEffect(() => {
    const c = ref.current;
    if (!c) return;
    c.width = art.size * 16;
    c.height = art.size * 16;
    drawSigil(c.getContext('2d')!, art, 16);
  }, [art]);
  const save = () => {
    ref.current?.toBlob((blob) => {
      if (!blob) return;
      const a = document.createElement('a');
      a.href = URL.createObjectURL(blob);
      a.download = `nomad-sigil-${run.sim.seed}.png`;
      a.click();
      setTimeout(() => URL.revokeObjectURL(a.href), 1000);
    });
  };
  return (
    <div className="row" style={{ alignItems: 'flex-start', gap: 16 }}>
      <canvas ref={ref} className="sigil-canvas" role="img" aria-label="Your run's footprint sigil" />
      <div style={{ maxWidth: 420 }}>
        <p>
          A crest from the {run.story?.squares?.length ?? 0} map regions you walked inside your circle, laid out the way they lie on the
          map and mirrored like a shield. The seed picks its colours, so no two runs share one.
        </p>
        <p className="muted small">256 pixels square, sharp at any size: made to be a profile picture.</p>
        <button type="button" className="btn btn-big" onClick={save}>
          Save as a picture
        </button>
      </div>
    </div>
  );
}

function OtherForks({ geo, run }: { geo: Geography; run: SavedRun }) {
  const partings = run.story?.partings ?? [];
  const [pick, setPick] = useState(Math.max(0, partings.length - 1));
  const scene = useRef<MapScene | null>(null);
  const ghosts = useMemo(() => partings.map((p) => ghostPath(geo, p, run.sim.minutes)), [geo, partings, run]);
  if (!scene.current) {
    scene.current = { sim: new NomadSim(geo, run.sim), player: null, forecast: null, fog: false, follow: true, bare: true };
  }
  const p = partings[pick];
  scene.current.focus = ghosts[pick]?.[ghosts[pick].length - 1] ?? null;
  scene.current.extra = ({ ctx, at, scale, t, text }) => {
    ghosts.forEach((g, i) => {
      if (!g.length) return;
      const on = i === pick;
      ctx.beginPath();
      g.forEach((q, k) => {
        const [x, y] = at(q.x, q.y);
        if (k) ctx.lineTo(x, y);
        else ctx.moveTo(x, y);
      });
      ctx.setLineDash([5, 5]);
      ctx.strokeStyle = on ? 'rgba(201,166,255,0.9)' : 'rgba(201,166,255,0.35)';
      ctx.lineWidth = on ? 2 : 1;
      ctx.stroke();
      ctx.setLineDash([]);
      const end = g[g.length - 1];
      const [ex, ey] = at(end.x, end.y);
      const breathe = 0.5 + 0.5 * Math.sin(t / 700 + i);
      ctx.beginPath();
      ctx.arc(ex, ey, partings[i][4] * scale, 0, Math.PI * 2);
      ctx.fillStyle = `rgba(201,166,255,${on ? 0.12 + breathe * 0.08 : 0.05})`;
      ctx.fill();
      ctx.strokeStyle = `rgba(201,166,255,${on ? 0.8 : 0.3})`;
      ctx.lineWidth = on ? 2 : 1;
      ctx.stroke();
      if (on) text('The half you lost', ex, ey - partings[i][4] * scale - 6, '#c9a6ff', 'RSBold');
      // Where the kept half went on from, for comparison.
      const [kx, ky] = at(partings[i][5], partings[i][6]);
      ctx.beginPath();
      ctx.arc(kx, ky, 3, 0, Math.PI * 2);
      ctx.fillStyle = on ? '#ff981f' : 'rgba(255,152,31,0.4)';
      ctx.fill();
    });
  };
  if (!partings.length) return <p className="muted">No fork has parted in this run yet. When one does, its lost half drifts on here.</p>;
  return (
    <div>
      <p>
        Every fork splits Main in two, and only one half lives on. Here the lost halves keep drifting at Main's pace, as if they had
        been kept. Only forks already parted show: never one that is coming.
      </p>
      <div className="row" style={{ marginBottom: 6 }}>
        <label className="small muted" htmlFor="fork-pick">
          Fork
        </label>
        <select id="fork-pick" className="input" value={pick} onChange={(e) => setPick(Number(e.target.value))}>
          {partings.map((q, i) => (
            <option key={i} value={i}>
              {played(q[0])}: the {compass(q[3] / 1000)} half
            </option>
          ))}
        </select>
        {p && <span className="muted small">{Math.round((run.sim.minutes - p[0]) / 60)} hours of drift since it faded</span>}
      </div>
      <div className="osrs-win map-frame" style={{ height: '56vh' }}>
        <MapCanvas scene={scene} zoom={1.2} className="map-main" />
      </div>
    </div>
  );
}
