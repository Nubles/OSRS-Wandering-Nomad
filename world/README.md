# Nomad UIM: the game world in 3D

This folder is [dennisdev/rs-map-viewer](https://github.com/dennisdev/rs-map-viewer), the viewer behind [osrs.world](https://osrs.world), imported at upstream commit `d3c569b` (BSD 2-Clause, see `LICENSE`). On top of it, the Nomad circles are drawn into the world:

- `src/nomad/NomadWorld.ts` loads a run from the link (`?seed=`, `?run=` with a plugin run code, or `?live=` with a live code) and plays it with the shared rules engine.
- `src/nomad/NomadOverlay.ts` draws each circle as a glowing wall that follows the ground, inside the WebGL renderer.
- `src/nomad/NomadPanel.tsx` is the panel and the circle names.
- `scripts/copy-engine.js` copies the engine (`web/src/engine`) and its data in before every start and build. Edit the engine there, not here.
- `scripts/build_cache.py` builds the game cache the viewer loads (into `public/caches/`) from abextm/osrs-cache at a pinned commit, keeping only the indexes the viewer reads.

The site serves the build at `world/`. To run it locally:

```
$ cd world
$ yarn install
$ python3 scripts/build_cache.py
$ yarn start
```

The upstream README follows.

# RuneScape Map Viewer

[Website](https://osrs.world) | [Discord](https://discord.gg/WfEWPE5wUd)

A webapp for exploring current and historical versions of RuneScape.

## Running locally

```
$ git clone https://github.com/dennisdev/rs-map-viewer.git
$ cd rs-map-viewer
$ yarn install
$ yarn run download-caches
$ yarn start
```

## Credits

-   Jagex
-   [RuneLite](https://github.com/runelite/runelite)
-   [OpenRS2 Archive](https://archive.openrs2.org/) - Caches
-   [RuneScape Archive](https://rs-archive.github.io/) - Caches
-   [OSRS Wiki](https://oldschool.runescape.wiki/) - Item spawns
-   [2004scape](https://github.com/2004scape/Server) - Npc spawns
-   [2009scape](https://gitlab.com/2009scape/2009scape) - Npc spawns
-   [RuneStar](https://github.com/RuneStar/fonts) - Fonts
-   [Blurite](https://github.com/blurite/pathfinder) - Some pathfinding stuff
-   [RuneApps Model Viewer](https://github.com/skillbert/rsmv) - Some procedural texture stuff
