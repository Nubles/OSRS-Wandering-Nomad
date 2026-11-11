/** Every page beyond the map, in three groups. */
const GROUPS = [
  {
    title: 'Your run',
    note: 'Paste a run code from the plugin (Copy run code in the sidebar).',
    pages: [
      { href: '#/saga', name: 'The Saga', line: 'Your run as a storybook, a chapter per act, with a report card, a sigil and the forks you never kept.' },
      { href: '#/circle', name: "What's in my circle", line: 'Every bank, altar, furnace, fishing spot and dungeon inside your circle right now, from the game cache.' },
      { href: '#/compare', name: 'Side by side', line: 'Up to four runs on one map, lined up by played hour, with one scrubber.' },
    ],
  },
  {
    title: 'Together',
    note: 'Built from what players chose to share in the plugin settings. Nothing is ranked.',
    pages: [
      { href: '#/lobby', name: 'Nomad lobby', line: 'Every live run that joined the lobby, as a small breathing circle you can click to watch.' },
      { href: '#/graveyard', name: 'The Graveyard', line: 'Tombstones of opted-in deaths, each with its circle and an epitaph, and a way to visit it in 3D.' },
      { href: '#/atlas', name: 'Atlas of wandering', line: "Where Nomads' circles have taken them, and the regions no Nomad has stood in yet." },
      { href: '#/firsts', name: 'Hall of firsts', line: 'The first player to reach each trial, rank and finish line.' },
    ],
  },
  {
    title: 'Play',
    note: 'Little games made from the same engine as the plugin.',
    pages: [
      { href: '#/guess', name: 'Circle Guesser', line: 'A daily game: the camera turns inside five circles and you pin where each one is.' },
      { href: '#/prophecy', name: 'Seed prophecy', line: 'Roll a seed and read a vague line about its opening hours, as a card to share.' },
      { href: '#/rules', name: 'The living rulebook', line: 'Every law with a tiny demo you can poke.' },
    ],
  },
];

export function Explore() {
  return (
    <div className="page">
      {GROUPS.map((g) => (
        <section key={g.title} className="osrs-win window">
          <h2 className="win-head osrs-title">{g.title}</h2>
          <div className="win-body">
            <p className="muted small">{g.note}</p>
            <div className="laws">
              {g.pages.map((p) => (
                <a key={p.href} className="law osrs-inner explore-card" href={p.href}>
                  <h3>{p.name}</h3>
                  <p>{p.line}</p>
                </a>
              ))}
            </div>
          </div>
        </section>
      ))}
    </div>
  );
}
