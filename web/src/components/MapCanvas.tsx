import { useEffect, useRef } from 'react';
import type { Tile } from '../engine/geography';
import { VOLCANO } from '../engine/geography';
import type { Forecast } from '../engine/forecast';
import { contains, MAIN, NomadSim, touches } from '../engine/sim';
import { MAP } from '../mapBounds';
import { loadMapImage } from '../useGeography';

export interface MapScene {
  sim: NomadSim;
  player: Tile | null;
  forecast: Forecast | null;
  /** Darken everything outside your circle. */
  fog: boolean;
  /** Keep the camera on your circle. */
  follow: boolean;
}

interface Props {
  /** Read every frame, so the canvas never waits on React to redraw. */
  scene: React.RefObject<MapScene | null>;
  /** Tiles per screen pixel at the start; smaller is closer. */
  zoom?: number;
  interactive?: boolean;
  onPick?: (tile: Tile) => void;
  className?: string;
}

interface View {
  cx: number;
  cy: number;
  /** Screen pixels per tile. */
  scale: number;
}

const MIN_SCALE = 0.18;
const MAX_SCALE = 8;

export function MapCanvas({ scene, zoom = 1, interactive = true, onPick, className }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const view = useRef<View | null>(null);
  const onPickRef = useRef(onPick);
  onPickRef.current = onPick;

  useEffect(() => {
    const canvas = canvasRef.current!;
    const ctx = canvas.getContext('2d')!;
    let img: HTMLImageElement | null = null;
    loadMapImage().then((i) => (img = i), () => undefined);
    let frame = 0;
    let dpr = 1;
    let w = 0;
    let h = 0;

    const resize = () => {
      dpr = Math.min(window.devicePixelRatio || 1, 2);
      w = canvas.clientWidth;
      h = canvas.clientHeight;
      canvas.width = Math.round(w * dpr);
      canvas.height = Math.round(h * dpr);
    };
    const ro = new ResizeObserver(resize);
    ro.observe(canvas);
    resize();

    const toScreen = (v: View, x: number, y: number) => [(x - v.cx) * v.scale + w / 2, (v.cy - y) * v.scale + h / 2] as const;
    const toWorld = (v: View, sx: number, sy: number) => ({ x: (sx - w / 2) / v.scale + v.cx, y: v.cy - (sy - h / 2) / v.scale });

    const draw = (t: number) => {
      frame = requestAnimationFrame(draw);
      const s = scene.current;
      if (!s || w === 0) return;
      const sim = s.sim;
      const yours = sim.bound;
      if (!view.current) view.current = { cx: yours.x, cy: yours.y, scale: zoom };
      const v = view.current;
      if (s.follow) {
        v.cx += (yours.x - v.cx) * 0.08;
        v.cy += (yours.y - v.cy) * 0.08;
      }

      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      ctx.fillStyle = '#1d2a3a';
      ctx.fillRect(0, 0, w, h);
      if (img) {
        const [ix, iy] = toScreen(v, MAP.x0, MAP.yTop);
        ctx.imageSmoothingEnabled = v.scale < 2;
        ctx.drawImage(img, ix, iy, (MAP.x1 - MAP.x0) * v.scale, (MAP.yTop - MAP.yBottom) * v.scale);
      }

      // Fog: everything outside your circle dims.
      if (s.fog) {
        const [fx, fy] = toScreen(v, yours.x, yours.y);
        ctx.beginPath();
        ctx.rect(0, 0, w, h);
        ctx.arc(fx, fy, yours.radius * v.scale, 0, Math.PI * 2, true);
        ctx.fillStyle = 'rgba(8, 10, 16, 0.55)';
        ctx.fill('evenodd');
      }

      // Forecast cone: soft clouds of where each circle might be.
      if (s.forecast) {
        const watch = new Set([MAIN, sim.state.bound]);
        s.forecast.points.forEach((byCircle, hIdx) => {
          for (const i of watch) {
            const pts = byCircle[i];
            if (!pts) continue;
            const colour = hex(sim.colour(sim.circles[i]));
            const hull = convexHull(pts.map((p) => toScreen(v, p.x, p.y)));
            if (hull.length > 2) {
              ctx.beginPath();
              hull.forEach(([x, y], k) => (k ? ctx.lineTo(x, y) : ctx.moveTo(x, y)));
              ctx.closePath();
              ctx.fillStyle = withAlpha(colour, 0.13 - hIdx * 0.03);
              ctx.fill();
              ctx.setLineDash([3, 4]);
              ctx.strokeStyle = withAlpha(colour, 0.6);
              ctx.lineWidth = 1;
              ctx.stroke();
              ctx.setLineDash([]);
              const top = hull.reduce((a, b) => (b[1] < a[1] ? b : a));
              rsText(ctx, `${s.forecast!.horizons[hIdx] / 60}h`, top[0], top[1] - 4, colour, 'RSSmall');
            }
          }
        });
      }

      // Ports and the volcano.
      if (v.scale > 0.35) {
        for (const p of sim.geo.ports) {
          const [px, py] = toScreen(v, p.tile.x, p.tile.y);
          ctx.fillStyle = 'rgba(255,255,255,0.85)';
          ctx.beginPath();
          ctx.arc(px, py, 2.5, 0, Math.PI * 2);
          ctx.fill();
        }
      }
      {
        const [vx, vy] = toScreen(v, VOLCANO.x, VOLCANO.y);
        const open = sim.volcanoOpen();
        ctx.fillStyle = open ? '#ff5a36' : 'rgba(255,90,54,0.55)';
        ctx.beginPath();
        ctx.moveTo(vx, vy - 7);
        ctx.lineTo(vx + 6, vy + 4);
        ctx.lineTo(vx - 6, vy + 4);
        ctx.closePath();
        ctx.fill();
        ctx.strokeStyle = 'rgba(0,0,0,0.6)';
        ctx.lineWidth = 1;
        ctx.stroke();
      }

      // Circles, regions first so Main draws on top.
      const order = sim.circles.map((_, i) => i).filter((i) => i !== MAIN);
      order.push(MAIN);
      for (const i of order) {
        const c = sim.circles[i];
        const isYours = i === sim.state.bound;
        const colour = hex(sim.colour(c));
        const [x, y] = toScreen(v, c.x, c.y);
        const r = c.radius * v.scale;
        const crossable = sim.linked(i) && touches(yours, c);

        ctx.beginPath();
        ctx.arc(x, y, r, 0, Math.PI * 2);
        ctx.fillStyle = withAlpha(colour, isYours ? 0.12 : 0.07);
        ctx.fill();
        if (isYours) {
          ctx.shadowColor = colour;
          ctx.shadowBlur = 14;
        }
        ctx.lineWidth = i === MAIN ? 3 : isYours ? 2.5 : 1.75;
        ctx.strokeStyle = colour;
        if (crossable) ctx.setLineDash([8, 5]);
        ctx.stroke();
        ctx.setLineDash([]);
        ctx.shadowBlur = 0;

        if (c.pulse > 0) {
          const k = 1 - c.pulse / 45;
          ctx.beginPath();
          ctx.arc(x, y, r + k * 18, 0, Math.PI * 2);
          ctx.strokeStyle = withAlpha(colour, 0.6 * (1 - k));
          ctx.lineWidth = 2;
          ctx.stroke();
        }
        // A breathing halo so the map always looks alive.
        const breathe = 0.5 + 0.5 * Math.sin(t / 900 + i);
        ctx.beginPath();
        ctx.arc(x, y, r + 3 + breathe * 2, 0, Math.PI * 2);
        ctx.strokeStyle = withAlpha(colour, 0.12 + breathe * 0.08);
        ctx.lineWidth = 1;
        ctx.stroke();

        if (c.voyage) {
          const [tx, ty] = toScreen(v, c.voyage.tx, c.voyage.ty);
          ctx.setLineDash([6, 6]);
          ctx.lineDashOffset = -t / 60;
          ctx.strokeStyle = withAlpha(colour, 0.9);
          ctx.lineWidth = 2;
          ctx.beginPath();
          ctx.moveTo(x, y);
          ctx.lineTo(tx, ty);
          ctx.stroke();
          ctx.setLineDash([]);
          ctx.lineDashOffset = 0;
        }

        if (v.scale > 0.3 || i === MAIN || isYours) {
          const label = sim.name(c) + (isYours ? ' (you)' : '');
          rsText(ctx, label, x, y - r - 6, colour, i === MAIN || isYours ? 'RSBold' : 'RS');
        }
      }

      if (s.player) {
        const [px, py] = toScreen(v, s.player.x, s.player.y);
        const inside = contains(yours, s.player.x, s.player.y);
        ctx.beginPath();
        ctx.arc(px, py, 5, 0, Math.PI * 2);
        ctx.fillStyle = inside ? '#ffffff' : '#ff5252';
        ctx.fill();
        ctx.lineWidth = 2;
        ctx.strokeStyle = '#000';
        ctx.stroke();
      }
    };
    frame = requestAnimationFrame(draw);

    // Pan, zoom and pick.
    const pointers = new Map<number, { x: number; y: number }>();
    let moved = 0;
    let pinch = 0;
    const local = (e: PointerEvent | WheelEvent) => {
      const b = canvas.getBoundingClientRect();
      return { x: e.clientX - b.left, y: e.clientY - b.top };
    };
    const zoomAt = (sx: number, sy: number, factor: number) => {
      const v = view.current;
      if (!v) return;
      const before = toWorld(v, sx, sy);
      v.scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, v.scale * factor));
      const after = toWorld(v, sx, sy);
      v.cx += before.x - after.x;
      v.cy += before.y - after.y;
      if (scene.current) scene.current.follow = false;
    };
    const down = (e: PointerEvent) => {
      canvas.setPointerCapture(e.pointerId);
      pointers.set(e.pointerId, local(e));
      moved = 0;
      if (pointers.size === 2) {
        const [a, b] = [...pointers.values()];
        pinch = Math.hypot(a.x - b.x, a.y - b.y);
      }
    };
    const move = (e: PointerEvent) => {
      const prev = pointers.get(e.pointerId);
      if (!prev || !view.current) return;
      const p = local(e);
      pointers.set(e.pointerId, p);
      if (pointers.size === 2) {
        const [a, b] = [...pointers.values()];
        const d = Math.hypot(a.x - b.x, a.y - b.y);
        if (pinch > 0) zoomAt((a.x + b.x) / 2, (a.y + b.y) / 2, d / pinch);
        pinch = d;
        moved += 10;
        return;
      }
      const dx = p.x - prev.x;
      const dy = p.y - prev.y;
      moved += Math.abs(dx) + Math.abs(dy);
      if (moved > 4) {
        view.current.cx -= dx / view.current.scale;
        view.current.cy += dy / view.current.scale;
        if (scene.current) scene.current.follow = false;
      }
    };
    const up = (e: PointerEvent) => {
      const p = pointers.get(e.pointerId);
      pointers.delete(e.pointerId);
      if (pointers.size < 2) pinch = 0;
      if (p && moved <= 4 && pointers.size === 0 && view.current && onPickRef.current) {
        const wpt = toWorld(view.current, p.x, p.y);
        onPickRef.current({ x: Math.round(wpt.x), y: Math.round(wpt.y) });
      }
    };
    const wheel = (e: WheelEvent) => {
      e.preventDefault();
      const p = local(e);
      zoomAt(p.x, p.y, Math.exp(-e.deltaY * 0.0015));
    };
    if (interactive) {
      canvas.addEventListener('pointerdown', down);
      canvas.addEventListener('pointermove', move);
      canvas.addEventListener('pointerup', up);
      canvas.addEventListener('pointercancel', up);
      canvas.addEventListener('wheel', wheel, { passive: false });
    }
    return () => {
      cancelAnimationFrame(frame);
      ro.disconnect();
      canvas.removeEventListener('pointerdown', down);
      canvas.removeEventListener('pointermove', move);
      canvas.removeEventListener('pointerup', up);
      canvas.removeEventListener('pointercancel', up);
      canvas.removeEventListener('wheel', wheel);
    };
  }, [scene, zoom, interactive]);

  /** Lets the page buttons zoom too. */
  const nudge = (factor: number) => {
    const v = view.current;
    if (v) v.scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, v.scale * factor));
  };

  return (
    <div className={`map-canvas ${className ?? ''}`}>
      <canvas ref={canvasRef} aria-label="Map of Gielinor with the Nomad circles" role="img" />
      {interactive && (
        <div className="map-zoom">
          <button type="button" aria-label="Zoom in" onClick={() => nudge(1.4)}>+</button>
          <button type="button" aria-label="Zoom out" onClick={() => nudge(1 / 1.4)}>−</button>
          <button
            type="button"
            aria-label="Follow your circle"
            title="Follow your circle"
            onClick={() => scene.current && (scene.current.follow = true)}
          >
            ◎
          </button>
        </div>
      )}
    </div>
  );
}

/** Map labels the way the game draws them: a pixel font with a 1px black shadow. */
function rsText(ctx: CanvasRenderingContext2D, text: string, x: number, y: number, colour: string, font: 'RS' | 'RSBold' | 'RSSmall') {
  ctx.font = `16px ${font}, sans-serif`;
  ctx.textAlign = 'center';
  ctx.fillStyle = '#000';
  ctx.fillText(text, Math.round(x) + 1, Math.round(y) + 1);
  ctx.fillStyle = colour;
  ctx.fillText(text, Math.round(x), Math.round(y));
}

function hex(rgb: number): string {
  return `#${rgb.toString(16).padStart(6, '0')}`;
}

function withAlpha(hexColour: string, a: number): string {
  const n = parseInt(hexColour.slice(1), 16);
  return `rgba(${n >> 16}, ${(n >> 8) & 255}, ${n & 255}, ${Math.max(0, a)})`;
}

/** Andrew's monotone chain. */
function convexHull(points: (readonly [number, number])[]): (readonly [number, number])[] {
  const pts = [...points].sort((a, b) => a[0] - b[0] || a[1] - b[1]);
  if (pts.length < 3) return pts;
  const cross = (o: readonly number[], a: readonly number[], b: readonly number[]) =>
    (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
  const lower: (readonly [number, number])[] = [];
  for (const p of pts) {
    while (lower.length >= 2 && cross(lower[lower.length - 2], lower[lower.length - 1], p) <= 0) lower.pop();
    lower.push(p);
  }
  const upper: (readonly [number, number])[] = [];
  for (let i = pts.length - 1; i >= 0; i--) {
    const p = pts[i];
    while (upper.length >= 2 && cross(upper[upper.length - 2], upper[upper.length - 1], p) <= 0) upper.pop();
    upper.push(p);
  }
  return lower.slice(0, -1).concat(upper.slice(0, -1));
}
