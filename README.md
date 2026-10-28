# Nomad UIM

A RuneLite plugin for **Nomad UIM**: Ultimate Ironman, but everything you do happens inside circles that drift across Gielinor.

- **The Main circle** is where you start, over Lumbridge. It roams the whole world and turns back at the coast. While a port sits inside it, it may board a quest-free boat (Veos, Captain Barnaby, the charter ships and others), or sail to any port once Sailing is unlocked.
- **Eleven region circles** (Kandarin, Fremennik, Asgarnia, Misthalin, the Desert, Morytania, the Wilderness, Karamja, Kourend & Kebos, Tirannwn and Varlamore) each patrol their own kingdom, three times as fast as Main, and never follow it.
- **Moods and temperaments:** every hour or two each circle changes mood (roaming somewhere new, meandering, looping, dashing, lingering or drifting), its path curves, and it bends away from coasts before reaching them. Each kingdom has a temperament that sets which moods it favours and how fast it goes (`Temperament.java`); Main heads for far-off waypoints that favour ground it hasn't covered lately.
- **Lean:** standing within 2 tiles of your circle's edge turns it toward you, up to 3 degrees a minute.
- **Crossing:** where Main touches a region circle, you can cross into it. The only way out of a region circle is back into Main. If Main moves on, you're stranded until it comes back, and your circle edges toward the border nearest it.
- **Achievement Diaries** grow their region's circle: from 12–32 tiles up to 28–48 with every tier done.
- **Outside your circle**, it drifts 5x faster and shrinks a tile a minute.
- **The volcano** (Fight Caves and Inferno) only counts while you're in Main and Main covers it.

Website: https://nubles.github.io/OSRS-Wandering-Nomad/ · Design doc: https://claude.ai/artifact/Qixr6kDiCH9LRC9b3ZBrtU · Interactive mock-up: https://claude.ai/artifact/SgfYJt417gniq56Pxxonwd

## Status: prototype (Solo mode)

What works:

- The full circle simulation, seeded and deterministic, on the account's played time (it only moves while you're logged in).
- The surface tile you count as standing on: underground, in an instance or on a boat it stays the last surface tile you stood on.
- Diary progress read from the game, growing region circles automatically.
- Your circle's edge in the game world and on the minimap, every circle on the world map, a status box and a sidebar with crossings and the run log.
- The run saves per account and resumes where it left off.
- **View on website** and **Copy run code** in the sidebar open your run on the website. The run travels inside the link (gzipped JSON), so nothing is uploaded anywhere.

Not yet:

- Boons and waystones, the Forecast cone in game, live sync to the website, Passport, the hash-chained run log and Season mode.
- Sailing is a config tick box for now rather than read from the game.

## The map

`src/main/resources/com/wanderingnomad/sim/world.dat` holds where the land is (in 4-tile cells) and which kingdom each 64-tile map chunk belongs to. `scripts/build_world.py` builds it from the rendered surface map ([mejrs/layers_osrs](https://github.com/mejrs/layers_osrs)) and Fate Locked's kingdom chunk table, which follows the One Chunk Man Chunk Picker. Rivers and channels narrower than about 9 tiles count as land, so circles cross them instead of bouncing. Rebuild it only when the game adds new land.

## Layout

- `src/main/java/com/wanderingnomad/sim/` is the rules engine: plain Java with no RuneLite types, so it's unit tested without a client.
- `src/main/java/com/wanderingnomad/` is the RuneLite plugin: reading the game, saving, overlays and the sidebar.
- `web/` is the website (React, TypeScript, Vite). `web/src/engine/` is a TypeScript twin of the rules engine that reads the same `world.dat`. `web/src/engine/vectors.json` is a shared test file: `SharedVectorsTest` (Java) and `sim.test.ts` (TypeScript) both replay it and must match it exactly, so the plugin and the website can never disagree about where a circle is. After changing the rules, regenerate it with `SharedVectors.main` and commit it.

## Building

```
gradle check jar     # tests and a jar in build/libs
gradle runClient     # RuneLite in developer mode with the plugin loaded

cd web
npm ci
npm run dev          # the website at http://localhost:5173
npm test             # engine tests, including the shared vectors
```

Pushes to `main` deploy the website to GitHub Pages (`.github/workflows/website.yml`). `python3 scripts/build_world.py --site-map` rebuilds the website's map image (`web/public/map.jpg`) alongside `world.dat`. `python3 scripts/build_heights.py` rebuilds `web/public/heights.bin`, the ground heights for the website's 3D view, from the game's map data (abextm/osrs-cache at a pinned commit). `world/` is the game world in 3D: the [osrs.world](https://osrs.world) viewer with the circles drawn in it, served at the site's `world/` (see `world/README.md`).

## Live view

Turning on **Share live** in the plugin sends the run code to a small Cloudflare Worker in `workers/nomad-relay/` about every five minutes (sooner after a crossing or event). Anyone with the link `#/live/<code>` on the website watches the circles move between updates. The relay keeps only the latest run code for seven days and stores a hash of the write token, never the token.

Deploy it once from `workers/nomad-relay/`:

```
npx wrangler kv namespace create LIVE   # paste the id into wrangler.toml
npx wrangler@latest deploy
```

If the workers.dev URL isn't `https://nomad-relay.fatelocked.workers.dev`, update `LiveShare.RELAY` in the plugin and set `VITE_RELAY_URL` for the website build.

