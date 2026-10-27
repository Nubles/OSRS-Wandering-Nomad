import { Home } from './pages/Home';
import { MapPage } from './pages/MapPage';
import { Plugin } from './pages/Plugin';
import { Rules } from './pages/Rules';
import { useRoute } from './router';
import { useGeography } from './useGeography';

const NAV = [
  { page: 'map', label: 'Map', href: '#/map' },
  { page: 'rules', label: 'Rules', href: '#/rules' },
  { page: 'plugin', label: 'Plugin', href: '#/plugin' },
] as const;

export function App() {
  const route = useRoute();
  const geo = useGeography();

  return (
    <div className={`app page-${route.page}`}>
      <header className="topbar">
        <a className="brand" href="#/">
          <span className="brand-mark" aria-hidden="true" />
          Nomad UIM
        </a>
        <nav>
          {NAV.map((n) => (
            <a key={n.page} href={n.href} aria-current={route.page === n.page ? 'page' : undefined}>
              {n.label}
            </a>
          ))}
          <a href="https://github.com/Nubles/OSRS-Wandering-Nomad" target="_blank" rel="noreferrer" className="nav-gh">
            GitHub
          </a>
        </nav>
      </header>
      <main>
        {route.page === 'rules' && <Rules />}
        {route.page === 'plugin' && <Plugin />}
        {(route.page === 'home' || route.page === 'map') &&
          (geo ? route.page === 'home' ? <Home geo={geo} /> : <MapPage geo={geo} route={route} /> : <div className="loading">Loading the map…</div>)}
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
