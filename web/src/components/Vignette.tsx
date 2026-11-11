import { useEffect, useRef } from 'react';
import { MAP } from '../mapBounds';
import { loadMapImage } from '../useGeography';

export interface VignetteMark {
  x: number;
  y: number;
  colour: string;
  /** A dot, a diamond (a waystone or big moment), a cross (a death) or a ring (a lost fork). */
  shape: 'dot' | 'diamond' | 'cross' | 'ring';
}

interface Props {
  /** Main's track as [x, y] tiles, oldest first. */
  path: [number, number][];
  /** Where you stood, as [x, y] tiles. */
  steps?: [number, number][];
  marks?: VignetteMark[];
  size?: number;
  label?: string;
}

/** A small square of the map around a stretch of the run, its path drawn on it. */
export function Vignette({ path, steps = [], marks = [], size = 220, label }: Props) {
  const ref = useRef<HTMLCanvasElement>(null);
  useEffect(() => {
    let live = true;
    loadMapImage().then((img) => {
      const canvas = ref.current;
      if (!live || !canvas) return;
      const dpr = Math.min(window.devicePixelRatio || 1, 2);
      canvas.width = size * dpr;
      canvas.height = size * dpr;
      const ctx = canvas.getContext('2d')!;
      ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
      const all = [...path, ...steps, ...marks.map((m) => [m.x, m.y] as [number, number])];
      if (all.length === 0) all.push([3222, 3218]);
      const xs = all.map((p) => p[0]);
      const ys = all.map((p) => p[1]);
      const cx = (Math.min(...xs) + Math.max(...xs)) / 2;
      const cy = (Math.min(...ys) + Math.max(...ys)) / 2;
      const span = Math.max(80, Math.max(...xs) - Math.min(...xs) + 60, Math.max(...ys) - Math.min(...ys) + 60);
      const scale = size / span;
      const sx0 = cx - span / 2 - MAP.x0;
      const sy0 = MAP.yTop - (cy + span / 2);
      ctx.fillStyle = '#1d2a3a';
      ctx.fillRect(0, 0, size, size);
      ctx.drawImage(img, sx0, sy0, span, span, 0, 0, size, size);
      ctx.fillStyle = 'rgba(10, 8, 6, 0.25)';
      ctx.fillRect(0, 0, size, size);
      const at = (x: number, y: number) => [(x - (cx - span / 2)) * scale, (cy + span / 2 - y) * scale] as const;
      if (path.length) {
        ctx.beginPath();
        path.forEach(([x, y], i) => {
          const [px, py] = at(x, y);
          if (i) ctx.lineTo(px, py);
          else ctx.moveTo(px, py);
        });
        ctx.strokeStyle = 'rgba(0,0,0,0.6)';
        ctx.lineWidth = 4;
        ctx.stroke();
        ctx.strokeStyle = '#ff981f';
        ctx.lineWidth = 2;
        ctx.stroke();
        const [ex, ey] = at(...path[path.length - 1]);
        ctx.beginPath();
        ctx.arc(ex, ey, 4, 0, Math.PI * 2);
        ctx.fillStyle = '#ff981f';
        ctx.fill();
      }
      for (const [x, y] of steps) {
        const [px, py] = at(x, y);
        ctx.fillStyle = 'rgba(255,255,255,0.75)';
        ctx.fillRect(px - 1, py - 1, 2, 2);
      }
      for (const m of marks) {
        const [px, py] = at(m.x, m.y);
        ctx.fillStyle = m.colour;
        ctx.strokeStyle = m.colour;
        ctx.lineWidth = 2;
        ctx.beginPath();
        if (m.shape === 'diamond') {
          ctx.moveTo(px, py - 6);
          ctx.lineTo(px + 4.5, py);
          ctx.lineTo(px, py + 6);
          ctx.lineTo(px - 4.5, py);
          ctx.closePath();
          ctx.fill();
        } else if (m.shape === 'cross') {
          ctx.moveTo(px - 5, py - 5);
          ctx.lineTo(px + 5, py + 5);
          ctx.moveTo(px + 5, py - 5);
          ctx.lineTo(px - 5, py + 5);
          ctx.stroke();
        } else if (m.shape === 'ring') {
          ctx.setLineDash([3, 3]);
          ctx.arc(px, py, 7, 0, Math.PI * 2);
          ctx.stroke();
          ctx.setLineDash([]);
        } else {
          ctx.arc(px, py, 3, 0, Math.PI * 2);
          ctx.fill();
        }
      }
    });
    return () => {
      live = false;
    };
  }, [path, steps, marks, size]);
  return <canvas ref={ref} style={{ width: size, height: size }} role="img" aria-label={label ?? 'A map of this part of the run'} />;
}
