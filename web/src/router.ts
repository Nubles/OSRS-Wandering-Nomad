import { useEffect, useState } from 'react';

/** Hash routes, so GitHub Pages can serve every page from one index.html. */
export interface Route {
  page: 'home' | 'map' | 'rules' | 'plugin';
  params: URLSearchParams;
  /** A run code from #/run/<code>. */
  run?: string;
}

export function parseRoute(hash: string): Route {
  const h = hash.replace(/^#\/?/, '');
  if (h.startsWith('run/')) return { page: 'map', params: new URLSearchParams(), run: decodeURIComponent(h.slice(4)) };
  const [path, query = ''] = h.split('?');
  const params = new URLSearchParams(query);
  if (path === 'map' || path === 'rules' || path === 'plugin') return { page: path, params };
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
