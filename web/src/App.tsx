import { lazy, ReactNode, Suspense } from 'react';
import type { Geography } from './engine/geography';
import { Explore } from './pages/Explore';
import { Home } from './pages/Home';
import { MapPage } from './pages/MapPage';
import { Plugin } from './pages/Plugin';
import { Rules } from './pages/Rules';
import { Page, Route, useRoute } from './router';
import { useGeography } from './useGeography';

const CirclePage = lazy(() => import('./pages/CirclePage'));
const Saga = lazy(() => import('./pages/Saga'));
const Compare = lazy(() => import('./pages/Compare'));
const Lobby = lazy(() => import('./pages/Lobby'));
const Graveyard = lazy(() => import('./pages/Graveyard'));
const Atlas = lazy(() => import('./pages/Atlas'));
const Firsts = lazy(() => import('./pages/Firsts'));
const Guess = lazy(() => import('./pages/Guess'));
const Prophecy = lazy(() => import('./pages/Prophecy'));

const NAV = [
  { page: 'map', label: 'Map', href: '#/map' },
  { page: 'rules', label: 'Rules', href: '#/rules' },
  { page: 'plugin', label: 'Plugin', href: '#/plugin' },
  { page: 'explore', label: 'Explore', href: '#/explore' },
] as const;

/** The pages listed under Explore count as it in the nav. */
const UNDER_EXPLORE: Page[] = ['explore', 'circle', 'saga', 'compare', 'lobby', 'graveyard', 'atlas', 'firsts', 'guess', 'prophecy'];

/** Pages that need the map's geography before they can draw. */
const NEEDS_GEO: Page[] = ['home', 'map', 'circle', 'saga', 'compare', 'lobby', 'graveyard', 'atlas', 'guess', 'prophecy'];

function body(route: Route, geo: Geography | null): ReactNode {
  if (route.page === 'rules') return <Rules geo={geo} />;
  if (route.page === 'plugin') return <Plugin />;
  if (route.page === 'explore') return <Explore />;
  if (route.page === 'firsts') return <Firsts />;
  if (NEEDS_GEO.includes(route.page) && !geo) return <div className="loading">Loading the map…</div>;
  const g = geo!;
  switch (route.page) {
    case 'home':
      return <Home geo={g} />;
    case 'map':
      return <MapPage geo={g} route={route} />;
    case 'circle':
      return <CirclePage geo={g} route={route} />;
    case 'saga':
      return <Saga geo={g} route={route} />;
    case 'compare':
      return <Compare geo={g} />;
    case 'lobby':
      return <Lobby geo={g} />;
    case 'graveyard':
      return <Graveyard geo={g} />;
    case 'atlas':
      return <Atlas geo={g} />;
    case 'guess':
      return <Guess geo={g} />;
    case 'prophecy':
      return <Prophecy geo={g} route={route} />;
    default:
      return null;
  }
}

export function App() {
  const route = useRoute();
  const geo = useGeography();
  const here = UNDER_EXPLORE.includes(route.page) ? 'explore' : route.page;

  return (
    <div className={`app page-${route.page}`}>
      <header className="topbar osrs-win">
        <a className="brand" href="#/">
          <span className="brand-mark" aria-hidden="true" />
          <span>
            <span className="brand-name">Nomad UIM</span>
            <br />
            <span className="brand-sub">Ultimate Ironman inside drifting circles</span>
          </span>
        </a>
        <nav>
          {NAV.map((n) => (
            <a key={n.page} className="btn" href={n.href} aria-current={here === n.page ? 'page' : undefined}>
              {n.label}
            </a>
          ))}
          <a href="https://github.com/Nubles/OSRS-Wandering-Nomad" target="_blank" rel="noreferrer" className="btn nav-gh">
            GitHub
          </a>
        </nav>
      </header>
      <main>
        <Suspense fallback={<div className="loading">Loading…</div>}>{body(route, geo)}</Suspense>
      </main>
      {route.page !== 'map' && (
        <footer className="footer">
          <p>
            Nomad UIM is a fan-made challenge mode for Old School RuneScape. Map imagery from the{' '}
            <a href="https://github.com/mejrs/layers_osrs" target="_blank" rel="noreferrer">
              mejrs OSRS map layers
            </a>
            . Not affiliated with Jagex.
          </p>
        </footer>
      )}
    </div>
  );
}
