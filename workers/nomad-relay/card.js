/**
 * Link cards: the small picture Discord (or any chat) shows when someone pastes a live or run
 * link. Drawn here in plain JavaScript, with a pixel font and a tiny PNG encoder, so the relay
 * needs no image library. Nothing here touches KV.
 */

export const CARD_W = 600;
export const CARD_H = 315;
export const SITE = 'https://nubles.github.io/OSRS-Wandering-Nomad/';
export const MOOD_NAMES = ['Roaming', 'Meandering', 'Looping', 'Dashing', 'Lingering', 'Drifting'];
const MAIN_COLOUR = [0xff, 0x98, 0x1f];
const GOLD = [0xff, 0xd5, 0x4f];
const OTHER = [0x9f, 0xb7, 0xc9];
const INK = [0xe8, 0xd9, 0xa8];
const MUTED = [0xa0, 0x95, 0x80];
const BACK = [0x1b, 0x17, 0x12];
const RED = [0xd9, 0x3a, 0x2b];
/** A decoded run's JSON is never allowed past this. */
const MAX_RUN_BYTES = 512 * 1024;

/** 5x7 glyphs, one string of 7 rows per character. */
const FONT = {
  A: '01110 10001 10001 11111 10001 10001 10001', B: '11110 10001 10001 11110 10001 10001 11110',
  C: '01110 10001 10000 10000 10000 10001 01110', D: '11110 10001 10001 10001 10001 10001 11110',
  E: '11111 10000 10000 11110 10000 10000 11111', F: '11111 10000 10000 11110 10000 10000 10000',
  G: '01110 10001 10000 10111 10001 10001 01111', H: '10001 10001 10001 11111 10001 10001 10001',
  I: '01110 00100 00100 00100 00100 00100 01110', J: '00111 00010 00010 00010 00010 10010 01100',
  K: '10001 10010 10100 11000 10100 10010 10001', L: '10000 10000 10000 10000 10000 10000 11111',
  M: '10001 11011 10101 10101 10001 10001 10001', N: '10001 10001 11001 10101 10011 10001 10001',
  O: '01110 10001 10001 10001 10001 10001 01110', P: '11110 10001 10001 11110 10000 10000 10000',
  Q: '01110 10001 10001 10001 10101 10010 01101', R: '11110 10001 10001 11110 10100 10010 10001',
  S: '01111 10000 10000 01110 00001 00001 11110', T: '11111 00100 00100 00100 00100 00100 00100',
  U: '10001 10001 10001 10001 10001 10001 01110', V: '10001 10001 10001 10001 10001 01010 00100',
  W: '10001 10001 10001 10101 10101 10101 01010', X: '10001 10001 01010 00100 01010 10001 10001',
  Y: '10001 10001 01010 00100 00100 00100 00100', Z: '11111 00001 00010 00100 01000 10000 11111',
  0: '01110 10001 10011 10101 11001 10001 01110', 1: '00100 01100 00100 00100 00100 00100 01110',
  2: '01110 10001 00001 00010 00100 01000 11111', 3: '11111 00010 00100 00010 00001 10001 01110',
  4: '00010 00110 01010 10010 11111 00010 00010', 5: '11111 10000 11110 00001 00001 10001 01110',
  6: '00110 01000 10000 11110 10001 10001 01110', 7: '11111 00001 00010 00100 01000 01000 01000',
  8: '01110 10001 10001 01110 10001 10001 01110', 9: '01110 10001 10001 01111 00001 00010 01100',
  ' ': '00000 00000 00000 00000 00000 00000 00000', '.': '00000 00000 00000 00000 00000 01100 01100',
  ',': '00000 00000 00000 00000 01100 00100 01000', ':': '00000 01100 01100 00000 01100 01100 00000',
  '-': '00000 00000 00000 11111 00000 00000 00000', '/': '00001 00010 00010 00100 01000 01000 10000',
  "'": '00100 00100 01000 00000 00000 00000 00000', '!': '00100 00100 00100 00100 00100 00000 00100',
  '?': '01110 10001 00001 00010 00100 00000 00100', '(': '00010 00100 01000 01000 01000 00100 00010',
  ')': '01000 00100 00010 00010 00010 00100 01000', '+': '00000 00100 00100 11111 00100 00100 00000',
  '%': '11001 11010 00010 00100 01000 01011 10011', '&': '01100 10010 10100 01000 10101 10010 01101',
};

/** A run code's JSON, or null if it isn't one. */
export async function decodeRun(code) {
  if (typeof code !== 'string' || !code.startsWith('N1.')) return null;
  try {
    const b64 = code.slice(3).replace(/-/g, '+').replace(/_/g, '/');
    const bin = atob(b64 + '='.repeat((4 - (b64.length % 4)) % 4));
    const gz = Uint8Array.from(bin, (c) => c.charCodeAt(0));
    const reader = new Blob([gz]).stream().pipeThrough(new DecompressionStream('gzip')).getReader();
    const parts = [];
    let total = 0;
    for (;;) {
      const { done, value } = await reader.read();
      if (done) break;
      total += value.byteLength;
      if (total > MAX_RUN_BYTES) {
        await reader.cancel();
        return null;
      }
      parts.push(value);
    }
    const run = JSON.parse(new TextDecoder().decode(concat(parts, total)));
    return run && run.sim && Array.isArray(run.sim.circles) && run.sim.circles.length ? run : null;
  } catch {
    return null;
  }
}

/** What a card says about a run: a title, one line under it and the lines drawn on the picture. */
export function describe(run, live) {
  if (!run) {
    return {
      title: live ? 'Nomad UIM: a live run' : 'Nomad UIM: a run',
      line: 'Ultimate Ironman inside drifting circles. Open the link to watch.',
      badge: live ? 'LIVE' : 'RUN',
      lines: ['ULTIMATE IRONMAN', 'INSIDE DRIFTING CIRCLES'],
    };
  }
  const sim = run.sim;
  const main = sim.circles[0];
  const bound = sim.circles[sim.bound | 0] ?? main;
  const mood = MOOD_NAMES[main.mood | 0] ?? 'Roaming';
  const played = time(sim.minutes | 0);
  const where = bound === main ? 'Main' : regionName(bound.regionId);
  const online = live?.online && Date.now() - (live.at ?? 0) < 15 * 60_000;
  const badge = !live ? 'RUN' : online ? 'LIVE' : 'OFFLINE';
  const title = `Nomad UIM${run.epithet ? `: ${run.epithet}` : ''} · ${live ? (online ? 'live now' : 'last seen ' + ago(Date.now() - live.at)) : played + ' played'}`;
  return {
    title,
    line: `Main is ${mood.toLowerCase()}, ${Math.round(main.radius)} tiles across its radius. Bound to ${where}. ${played} played.`,
    badge,
    lines: [`MAIN IS ${mood.toUpperCase()}`, `RADIUS ${Math.round(main.radius)} TILES`, `${played.toUpperCase()} PLAYED`, `BOUND TO ${where.toUpperCase()}`],
  };
}

/** The card as a PNG. */
export async function renderCard(run, live) {
  const img = new Canvas(CARD_W, CARD_H);
  img.fill(BACK);
  // A soft warm glow behind the circles.
  for (let y = 0; y < CARD_H; y++) {
    for (let x = 0; x < CARD_W; x++) {
      const d = Math.hypot(x - 450, y - 158) / 260;
      if (d < 1) img.blend(x, y, [0x3a, 0x2a, 0x16], 0.6 * (1 - d) * (1 - d));
    }
  }
  img.frame(MAIN_COLOUR, 3);
  const said = describe(run, live);
  img.text('NOMAD UIM', 24, 26, 4, GOLD);
  const badgeColour = said.badge === 'LIVE' ? RED : said.badge === 'OFFLINE' ? [0x55, 0x4c, 0x40] : [0x6b, 0x4a, 0x1f];
  const bw = said.badge.length * 12 + 14;
  img.rect(24, 70, bw, 24, badgeColour);
  img.text(said.badge, 31, 75, 2, [0xff, 0xff, 0xff]);
  said.lines.forEach((l, i) => img.text(l.slice(0, 24), 24, 116 + i * 30, 2, i === 0 ? INK : MUTED));
  img.text('WATCH ON THE WEBSITE', 24, 280, 1, MUTED);
  img.minX = 300;
  if (run) drawCircles(img, run);
  else img.ring(450, 158, 90, 4, MAIN_COLOUR, 1);
  img.minX = 0;
  img.frame(MAIN_COLOUR, 3);
  return encodePng(img);
}

function drawCircles(img, run) {
  const sim = run.sim;
  const main = sim.circles[0];
  const bound = sim.bound | 0;
  const cx = 450;
  const cy = 158;
  const reach = Math.max(main.radius * 2.6, 40);
  const scale = 125 / reach;
  const at = (x, y) => [cx + (x - main.x) * scale, cy - (y - main.y) * scale];
  sim.circles.forEach((c, i) => {
    if (i === 0) return;
    const d = Math.hypot(c.x - main.x, c.y - main.y);
    if (d - c.radius > reach * 1.4) return;
    const [x, y] = at(c.x, c.y);
    const colour = regionColour(c.regionId);
    img.disc(x, y, c.radius * scale, colour, 0.08);
    img.ring(x, y, c.radius * scale, i === bound ? 3 : 1.5, colour, i === bound ? 1 : 0.75);
  });
  if (sim.twin) {
    const [x, y] = at(sim.twin.x, sim.twin.y);
    img.ring(x, y, sim.twin.radius * scale, 2, MAIN_COLOUR, 0.6);
  }
  const [mx, my] = at(main.x, main.y);
  img.disc(mx, my, main.radius * scale, MAIN_COLOUR, 0.14);
  img.ring(mx, my, main.radius * scale, 3.5, bound === 0 ? GOLD : MAIN_COLOUR, 1);
  // Its heading, as a short arrow from the middle.
  const h = main.heading ?? 0;
  for (let k = 0; k < 18; k++) img.disc(mx + Math.cos(h) * k * 1.6, my - Math.sin(h) * k * 1.6, 1.6, MAIN_COLOUR, 0.9);
  if (run.surface) {
    const [px, py] = at(run.surface.x, run.surface.y);
    if (px > 300 && px < CARD_W - 6 && py > 6 && py < CARD_H - 6) {
      img.disc(px, py, 6, [0, 0, 0], 1);
      img.disc(px, py, 4, [0xff, 0xff, 0xff], 1);
    }
  }
}

/** The page a card link serves: the tags chats read, and a redirect for people. */
export function cardHtml({ title, line, image, target }) {
  const e = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
  return `<!doctype html>
<html lang="en"><head><meta charset="utf-8">
<title>${e(title)}</title>
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta property="og:type" content="website">
<meta property="og:site_name" content="Nomad UIM">
<meta property="og:title" content="${e(title)}">
<meta property="og:description" content="${e(line)}">
<meta property="og:image" content="${e(image)}">
<meta property="og:image:width" content="${CARD_W}">
<meta property="og:image:height" content="${CARD_H}">
<meta property="og:url" content="${e(target)}">
<meta name="twitter:card" content="summary_large_image">
<meta name="theme-color" content="#ff981f">
<meta http-equiv="refresh" content="0; url=${e(target)}">
</head><body style="background:#1b1712;color:#e8d9a8;font-family:sans-serif">
<p><a style="color:#ffd54f" href="${e(target)}">Open the run on the Nomad UIM website</a></p>
</body></html>`;
}

class Canvas {
  constructor(w, h) {
    this.w = w;
    this.h = h;
    this.px = new Uint8Array(w * h * 3);
    /** Pixels left of this stay untouched, so the circles never run under the words. */
    this.minX = 0;
  }

  fill(c) {
    for (let i = 0; i < this.w * this.h; i++) this.px.set(c, i * 3);
  }

  blend(x, y, c, a) {
    x |= 0;
    y |= 0;
    if (x < this.minX || y < 0 || x >= this.w || y >= this.h || a <= 0) return;
    const i = (y * this.w + x) * 3;
    const k = Math.min(1, a);
    for (let j = 0; j < 3; j++) this.px[i + j] = Math.round(this.px[i + j] * (1 - k) + c[j] * k);
  }

  rect(x, y, w, h, c) {
    for (let yy = y; yy < y + h; yy++) for (let xx = x; xx < x + w; xx++) this.blend(xx, yy, c, 1);
  }

  frame(c, t) {
    this.rect(0, 0, this.w, t, c);
    this.rect(0, this.h - t, this.w, t, c);
    this.rect(0, 0, t, this.h, c);
    this.rect(this.w - t, 0, t, this.h, c);
  }

  disc(cx, cy, r, c, a) {
    const x0 = Math.max(0, Math.floor(cx - r - 1));
    const x1 = Math.min(this.w - 1, Math.ceil(cx + r + 1));
    const y0 = Math.max(0, Math.floor(cy - r - 1));
    const y1 = Math.min(this.h - 1, Math.ceil(cy + r + 1));
    for (let y = y0; y <= y1; y++) {
      for (let x = x0; x <= x1; x++) {
        const cover = Math.max(0, Math.min(1, r + 0.5 - Math.hypot(x + 0.5 - cx, y + 0.5 - cy)));
        if (cover > 0) this.blend(x, y, c, a * cover);
      }
    }
  }

  ring(cx, cy, r, width, c, a) {
    const half = width / 2;
    const x0 = Math.max(0, Math.floor(cx - r - half - 1));
    const x1 = Math.min(this.w - 1, Math.ceil(cx + r + half + 1));
    const y0 = Math.max(0, Math.floor(cy - r - half - 1));
    const y1 = Math.min(this.h - 1, Math.ceil(cy + r + half + 1));
    for (let y = y0; y <= y1; y++) {
      for (let x = x0; x <= x1; x++) {
        const cover = Math.max(0, Math.min(1, half + 0.5 - Math.abs(Math.hypot(x + 0.5 - cx, y + 0.5 - cy) - r)));
        if (cover > 0) this.blend(x, y, c, a * cover);
      }
    }
  }

  /** Pixel text with a one-pixel shadow, the way the game draws its labels. */
  text(s, x, y, scale, c) {
    for (const [dx, colour] of [[scale > 1 ? scale / 2 : 1, [0, 0, 0]], [0, c]]) {
      let at = x;
      for (const ch of s.toUpperCase()) {
        const g = FONT[ch] ?? FONT['?'];
        g.split(' ').forEach((row, ry) => {
          for (let rx = 0; rx < 5; rx++) {
            if (row[rx] === '1') this.rect(at + rx * scale + dx, y + ry * scale + dx, scale, scale, colour);
          }
        });
        at += 6 * scale;
      }
    }
  }
}

async function encodePng(img) {
  const raw = new Uint8Array((img.w * 3 + 1) * img.h);
  for (let y = 0; y < img.h; y++) {
    raw[y * (img.w * 3 + 1)] = 0;
    raw.set(img.px.subarray(y * img.w * 3, (y + 1) * img.w * 3), y * (img.w * 3 + 1) + 1);
  }
  const reader = new Blob([raw]).stream().pipeThrough(new CompressionStream('deflate')).getReader();
  const parts = [];
  let total = 0;
  for (;;) {
    const { done, value } = await reader.read();
    if (done) break;
    parts.push(value);
    total += value.byteLength;
  }
  const idat = concat(parts, total);
  const ihdr = new Uint8Array(13);
  const v = new DataView(ihdr.buffer);
  v.setUint32(0, img.w);
  v.setUint32(4, img.h);
  ihdr[8] = 8; // bit depth
  ihdr[9] = 2; // RGB
  const chunks = [chunk('IHDR', ihdr), chunk('IDAT', idat), chunk('IEND', new Uint8Array(0))];
  const sig = Uint8Array.of(137, 80, 78, 71, 13, 10, 26, 10);
  return concat([sig, ...chunks], sig.length + chunks.reduce((n, c) => n + c.length, 0));
}

function chunk(type, data) {
  const out = new Uint8Array(12 + data.length);
  const v = new DataView(out.buffer);
  v.setUint32(0, data.length);
  for (let i = 0; i < 4; i++) out[4 + i] = type.charCodeAt(i);
  out.set(data, 8);
  v.setUint32(8 + data.length, crc32(out.subarray(4, 8 + data.length)));
  return out;
}

let CRC_TABLE = null;
export function crc32(bytes) {
  if (!CRC_TABLE) {
    CRC_TABLE = new Uint32Array(256);
    for (let n = 0; n < 256; n++) {
      let c = n;
      for (let k = 0; k < 8; k++) c = c & 1 ? 0xedb88320 ^ (c >>> 1) : c >>> 1;
      CRC_TABLE[n] = c >>> 0;
    }
  }
  let c = 0xffffffff;
  for (const b of bytes) c = CRC_TABLE[(c ^ b) & 0xff] ^ (c >>> 8);
  return (c ^ 0xffffffff) >>> 0;
}

function concat(parts, total) {
  const out = new Uint8Array(total);
  let at = 0;
  for (const p of parts) {
    out.set(p, at);
    at += p.byteLength;
  }
  return out;
}

function time(minutes) {
  const h = Math.floor(minutes / 60);
  return `${h}h ${String(minutes % 60).padStart(2, '0')}m`;
}

function ago(ms) {
  const m = Math.max(1, Math.round(ms / 60_000));
  if (m < 60) return `${m} min ago`;
  const h = Math.round(m / 60);
  return h < 48 ? `${h} h ago` : `${Math.round(h / 24)} days ago`;
}

/** The kingdoms, as the plugin's Geography names and colours them. */
const KINGDOMS = {
  kandarin: ['Kandarin', 0x4fc3f7], fremennik: ['Fremennik', 0xb0bec5], asgarnia: ['Asgarnia', 0x7986cb],
  misthalin: ['Misthalin', 0x9ccc65], desert: ['Kharidian Desert', 0xffd54f], morytania: ['Morytania', 0xba68c8],
  wilderness: ['Wilderness', 0xef5350], karamja: ['Karamja', 0x26a69a], kourend: ['Kourend & Kebos', 0xf06292],
  tirannwn: ['Tirannwn', 0x81c784], varlamore: ['Varlamore', 0xffa726], caravan: ['Caravan', 0xd7a86e],
};

function regionName(id) {
  return KINGDOMS[id]?.[0] ?? 'a kingdom';
}

function regionColour(id) {
  const c = KINGDOMS[id]?.[1];
  return c === undefined ? OTHER : [(c >> 16) & 255, (c >> 8) & 255, c & 255];
}
