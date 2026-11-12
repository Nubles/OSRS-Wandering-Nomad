import { useEffect, useState } from 'react';

export type Page =
  | 'home'
  | 'map'
  | 'rules'
  | 'plugin'
  | 'explore'
  | 'circle'
  | 'saga'
  | 'compare'
  | 'lobby'
  | 'graveyard'
  | 'atlas'
  | 'firsts'
  | 'guess'
  | 'prophecy';

const PAGES: Page[] = ['map', 'rules', 'plugin', 'explore', 'circle', 'saga', 'compare', 'lobby', 'graveyard', 'atlas', 'firsts', 'guess', 'prophecy'];

/** Hash routes, so GitHub Pages can serve every page from one index.html. */
export interface Route {
  page: Page;
  params: URLSearchParams;
  /** A run code from #/run/<code>, #/saga/<code> or #/circle/<code>. */
  run?: string;
  /** A live code from #/live/<code>. */
  live?: string;
}

export function parseRoute(hash: string): Route {
  const h = hash.replace(/^#\/?/, '');
  if (h.startsWith('live/')) return { page: 'map', params: new URLSearchParams(), live: decodeURIComponent(h.slice(5)) };
  if (h.startsWith('run/')) return { page: 'map', params: new URLSearchParams(), run: decodeURIComponent(h.slice(4)) };
  for (const p of ['saga', 'circle'] as const) {
    if (h.startsWith(`${p}/`)) return { page: p, params: new URLSearchParams(), run: decodeURIComponent(h.slice(p.length + 1)) };
  }
  const [path, query = ''] = h.split('?');
  const params = new URLSearchParams(query);
  if ((PAGES as string[]).includes(path)) return { page: path as Page, params };
  return { page: 'home', params };
}

export function useRoute(): Route {
  const [route, setRoute] = useState(() => parseRoute(location.hash));
  useEffect(() => {
    const on = () => {
      setRoute(parseRoute(location.hash));
      window.scrollTo(0, 0);
    };
    window.addEventListener('hashchange', on);
    return () => window.removeEventListener('hashchange', on);
  }, []);
  return route;
}
