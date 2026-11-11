# Nomad UIM

A RuneLite plugin for **Nomad UIM**: Ultimate Ironman, but everything you do happens inside circles that drift across Gielinor.

- **The Main circle** is where you start, over Lumbridge. It roams the whole world and turns back at the coast. While a port sits inside it, it may board a quest-free boat (Veos, Captain Barnaby, the charter ships and others), or, once Sailing is unlocked, sail to any dock your Sailing level reaches (each dock's level and quest are from the wiki's Docking points page, so Main never sails somewhere you can't follow).
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

### Waystones and boons

- **Milestones summon waystones:** every 100 total level and every 25 quest points summons a common one, and the first kill of any boss (any "kill count is: 1" message) a rare one. Only progress made during the run counts.
- **A waystone** rises at one of 93 landmarks (`sim/Landmarks.java`, open surface tiles in towns and well-known places) inside your circle, Main, or the region circle nearest yours, preferring one its circle should keep for at least half an hour. If none is in reach, it waits until one drifts in. It fades once its circle drifts off the landmark.
- **Claim it** by standing within 3 tiles of it, inside its circle. You hold three boons at most. In game it stands as a pillar of light, gold when it's in your circle, a gold arrow on the ground points to it, and the world map shows every waystone with its boon and roughly how long it has left.
- **Boons** are spent from the sidebar: **Expand** (+12 tiles for 2 hours), **Tailwind** (twice the drift for an hour), **Anchor** (holds still for an hour), **Lantern** (light it where you stand, and the next waystone to rise in your circle rises at the landmark nearest that spot), **Calm** (no fork or storm comes over Main for 3 hours, and one that's brewing or blowing breaks off) and the rare **Rudder** (turn 45 degrees left or right, once) and **Bridge** (for 10 minutes, cross to any circle within 60 tiles, and the walk doesn't count as outside).
- A waystone's dice come from the seed and how many have been placed, never from the drift's own, so earning a boon never changes where the circles go. The website's engine does the same, and the shared vectors cover summoning, claiming and every boon.

### Storms

- Every 6 to 12 played hours a storm brews over the Main circle, with a 10-minute warning: a **Gale** drives Main three times as fast for half an hour, or a **Squall** squeezes it to three quarters of its size for half an hour before it eases back. Storms wait out forks and voyages, and forks wait out storms. Their dice come from the seed and how many storms came before, in both engines, and the shared vectors cover them.

### Journey: trials, renown and ranks

- **Trials** are the run's goals: 60+ of them in eight chapters (First steps, The long road, Forks and fate, Kingdoms, Atlas, Seafarer, Deeds, Treasure and glory), each Bronze, Silver, Gold or Legend. The full list is `journey/Trials.java`; the sidebar's **All trials** button shows every one with its progress.
- **Everything is read, never ticked:** the circles give tiles walked, waystones claimed, boons spent, crossings, forks parted while you're inside, region circles you've been in, landmarks you've stood at (within 4 tiles), following Main across the sea (back inside within an hour of it docking), played time and your longest stretch without stepping outside. The game gives total level, quest points, 99s and diary tiers, plus its own chat lines for boss kill counts, Treasure Trails, combat tasks, pets and new collection log items (the same wordings RuneLite's Screenshot and Loot Tracker plugins match).
- **Only the run counts:** account trials (levels, quests, diaries, 99s) pay out only if the run is what takes you past the target; ones you'd passed before it show as "Before this run". The max total level follows the game's skill count (2,376 with Sailing).
- **Renown and ranks:** each trial pays renown once per run (Bronze 10, Silver 25, Gold 50, Legend 100). Renown is kept across runs and raises your Nomad rank, from Wanderer to Nomad Sovereign. Every rank unlocks an edge colour for the Main circle, picked in the sidebar. Ranks only change looks, never the rules.

- **Oaths:** swear one long goal as the run's oath from the sidebar: every diary tier, round the world (follow Main into all 14 ports), every landmark, max total, 50 bosses or every region circle. You can change it for an hour after swearing, then it binds. Keeping it pays 300 renown and unlocks the Oathbound edge colour on every run.
- **Omens:** an hour into the run, and then every 2 to 4 played hours, an omen turns up with a deadline: a **Pilgrimage** to a landmark inside your circle (90 minutes), **Hold fast** (don't step outside for 2 hours), **Wanderlust** (walk 1,500 tiles in 2 hours) or **The hunt** (a boss kill count or a Treasure Trail in 3 hours). Meeting one pays 25 renown and summons a waystone; missing one costs nothing. They're dealt from the run's seed, so relogging can't reroll them.
- **Bestiary:** every monster you kill inside your circle is counted by name. 10 kills pays renown, 100 summons a common waystone, 1,000 a rare one, and 10,000 pays big renown and a title such as "Goblin Bane" that the account keeps. Waystones come only from monsters of combat level 2 and up, at most one an hour; one earned sooner waits its turn. Kills are read from the same event RuneLite's Loot Tracker uses, so a monster that drops nothing isn't counted.
- **Run ledger:** the sidebar's Run ledger button shows the run's story so far (time played, rank and renown earned this run, tiles walked, waystones, forks, storms, voyages, harbours, regions, landmarks, omens, the oath and the latest trials) with a Copy button for Discord or a video description.
- **Footprint:** every 64-tile map region (the game's own regions; Lumbridge Castle is 12850) you walk in while inside your circle lights up faintly on the world map, so the run's whole journey shows at a glance. The Atlas chapter counts them: 25, 100 and 300 regions.
- **Rivals:** turn on **Follow rivals** and paste up to three friends' live links (as `Name=link`). Their circles show on your world map as dotted rings in their colour, with a dot where they last stood, and the sidebar tells how far apart you are. When a rival's circle touches yours you get a banner; stand inside their circle while they stand inside yours for the **Rendezvous** trial (meet three for **Caravan**). It only reads the relay, never writes, and a rival's run never changes yours.
- **Race a rival:** when you start a new run with rivals followed, you can pick one to race on their seed. Your circles start out exactly like theirs and part ways as you each play; the run ledger notes the race.

### The living edge

- **The Wall:** your circle's edge rises out of the ground as a glowing curtain, with bands of light running along it and sparks drifting up. It stands taller while the circle pulses. Where a circle you can cross into reaches inside yours, its wall stands brighter: that stretch is the gateway. (Edge style: Wall, Tiles or Both.)
- **Radar:** every minute, and whenever you step onto a new tile, the plugin plays a copy of your run 15 minutes ahead while you stand still. The status box and sidebar say when the edge will reach you and when each crossing opens, and a chat warning fires 5 minutes out. Fifteen minutes is short enough not to spoil the run.
- **Edge glow:** the game view's border glows amber as the edge closes in, and pulses red while you're outside.
- **Crossing compass:** an arrow on the ground points to the nearest open crossing, or one opening soon, with its name and distance.
- **Forecast:** the website's 1, 3 and 6 hour cones on the in-game world map, for Main and your circle. Like the website's, it imagines 40 futures with made-up dice, so it shows where a circle could get to, never where it will. It runs off the client thread every 15 played minutes.
- **Rule guard:** XP, loot, ground pickups and kills while you're outside your circle are noted in the run log and the sidebar. The rules allow fighting back and travelling home, so these are notes for the run's record, not verdicts, and nothing is ever blocked.

### Streaming

Everything here frames the run for streams and videos; nothing changes a rule. It lives in `stream/` (pure rules, with tests) and `StreamDesk.java`, under the **Streaming** settings section and the sidebar's **Streaming...** menu.

- **Episodes:** each play session is an episode. A break under 30 minutes carries it on; a longer one ends it, and the next login opens with **Previously on** (the last episode's three biggest moments and the thread it left hanging). At logout the sidebar shows **Next time on**: what the circles will do in your next session's first 20 minutes if you do nothing.
- **Session prophecy:** each episode opens with one cryptic line read from the run's own future, true if you follow the circle ("Salt water will carry the circle before the night is out"). It's revealed when the episode ends, or stamped **Fate defied**.
- **Start recording** times the chapters from now and stamps one **stake** from what the run already shows (a fork's warning, a storm brewing, a waystone's time left, an open dare, the edge's countdown, the Forecast's odds). **Wrap the episode** reveals the prophecy, settles the stake and gives a **drama score** out of 100 with its breakdown, ranked against every episode.
- **Close calls** (the edge within a tile, moving toward you, then turning away), **Echoes** (back in a kingdom after 10+ hours, a banner recalls what happened there), and **Run wrapped** (every 100 hours, six stat cards picked from a pool of 30, saved as a picture).
- **Chapters:** YouTube chapters, a marker list and an EDL for your editor, from the episode's biggest moments, with a hotkey to mark your own.
- **Narrator captions** (off by default): a lower third in a saga, dry or hushed voice, never repeating a line within 20 played hours.
- **Chat paints the wall:** paste chat's `!colour`, `!tone` and `!name` votes to set the edge colour, narrator and circle name for the episode. **Viewer flags:** paste chat's `!flag 3222 3218` or `!flag Varrock Square` lines; when the circle takes a flag in, a banner names its viewer. Both are pasted, so no relay is needed, and names are filtered.
- **Photo mode** (a hotkey) hides the Nomad HUD and turns the glow and shade up full. **Backseat shield** moves the Radar's countdowns and the Forecast into the sidebar. **Dead-air cards** (off by default) offer a talking point in the sidebar after 15 quiet minutes. **Stream feed** (off by default) writes text files for OBS to `.runelite/nomad-uim/stream`, and **Streamer layout** enlarges the status box's key numbers.

### How it looks

The pure parts live in `look/` (with tests); the drawing is in the overlays, `EdgeWall3D.java` and the sidebar. Each has its own setting.

- **The 3D wall's motion** follows your circle's mood: quick streaks when it dashes, a slow swell when it lingers, a shimmer when it meanders. **Wall motion** can make it one steady wave or still.
- **Fork tear:** during a fork's warning, a glowing crack spreads across Main where it will split, on the ground, the wall and the world map.
- **Standing stones:** each waystone stands as a stone obelisk under a pillar of light you can see from far off. You can't click it, and it goes when claimed.
- **Storm skies:** while a Gale or Squall blows, rain falls inside Main near you and lightning flickers. Never in instances.
- **Visible lean:** near your circle's edge, a chevron on the edge shows it turning toward you. The **tide line** fades while you fight.
- **Comet tails:** the world map draws the last three hours of Main's path and your circle's, thicker where they lingered. Hovering a circle also shows its temperament, and past the Radar it says a touch is "later".
- **Kingdom title cards:** walking into a kingdom for the first time in a run fades in its name and temperament; after that, a small banner. **Kingdom stingers:** crossing into a circle plays that circle's own chime.
- **Claim ceremony:** a firework over you and a rising chime, bigger for rares; a rarer claim always takes over a commoner one.
- **Gains and losses:** when the edge moves past a bank, altar, furnace, range, anvil, fairy ring or spirit tree near you, a banner says "Gained: Furnace" or "Lost: Bank". They're told apart by their own names and menu options, so there's no list to keep.
- **Black box:** the sidebar's **Replay the black box** plays your last 3 minutes and your circle's edge from above, ten times as fast. After a death, chat points you to it.
- **Almanac:** past the Radar's 15 minutes, the sidebar reads the Forecast in four vague words (unlikely, possible, likely, almost certain).
- **Coach marks:** in your first run, a one-time tip the first time each thing happens. **Show tips again** brings them back.
- **Moment camera** (off by default): a captioned screenshot at each big moment, saved to `.runelite/nomad-uim/moments`, 20 a session at most.
- **Edge alerts:** RuneLite notifications for the edge reaching you while you stand still, a fork coming and a waystone rising, one setting each.
- **Boons and timers** can show as RuneLite infoboxes, in the status box, or both.
- **Clear sight:** one accessibility setting. Colour safe swaps in a colour-blind-safe palette, high contrast makes edges thick and patterned by kind (Main solid, kingdoms dashed, the caravan dotted), and All does both and tells every banner in chat too.

### The website

The site (`web/`) is playable with no account and no server; the shared pages read the relay only when someone opted in.

- **Your run:** a run code copied from the sidebar now carries the run's **story**: an hourly track, hours by kingdom and mood, the smallest circle, each fork's lost half, the acts and the big moments. **The Saga** (`#/saga`) turns it into chapters with a little map each, folding quiet stretches into a line, with a one-screen **report card**, a **footprint sigil** (a pixel crest from the map regions you walked, coloured by the seed) and **the other forks** (the halves you didn't keep, still drifting). The map page draws the lost halves too.
- **What's in my circle** (`#/circle`): every bank, deposit box, altar, furnace, anvil, range, fairy ring, spinning wheel, loom, potter's wheel, fishing spot, mining site, rare tree, farming patch and dungeon entrance inside a circle right now. `web/scripts/build_places.py` reads them from the game cache and RuneLite's world map tables into `web/public/places.json`.
- **Side by side** (`#/compare`): up to four run codes on one map, aligned by played hour, with one scrubber.
- **Wishlist pings:** star landmarks on the map page; on a live run, the tab pings you when the Radar sees your circle reach one in the next 15 minutes.
- **Together:** the **Nomad lobby** (`#/lobby`, live runs as breathing circles), **the Graveyard** (`#/graveyard`, tombstones with an epitaph from the ledger, each one visitable in the 3D world viewer with `world/?grave=`), the **atlas of wandering** (`#/atlas`, a heatmap of regions walked and the land no Nomad has stood on) and the **Hall of firsts** (`#/firsts`, curated in `web/public/firsts.json`; claims come in as GitHub issues). Each is opt-in in the plugin's **Website** settings, off by default.
- **Play:** the **Circle Guesser** (`#/guess`, five circles a day, the 3D view turning inside each, a shareable grid), **Seed prophecy** (`#/prophecy`, one vague line about a seed's opening hours, as a card) and the **living rulebook** (each law on the Rules page has a demo to poke).
- **Link cards:** the sidebar's **Copy Discord card link** gives a relay link that unfurls as a picture of your circles with Main's mood and size and a LIVE badge, cached for an hour.

Not yet:

- Campfire (the fourth-circle boon), Forks, Passport, the hash-chained run log and Season mode.
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

The same worker serves the website's shared pages, each written only by players who turned it on: `GET /lobby` (live runs that joined the lobby, read from the live runs' own metadata, so it costs no extra write), `POST /g/<code>` and `GET /g` (tombstones, listed without their codes), `POST /a/<code>` and `GET /a` (kingdoms and map regions walked, summed), and the link cards `GET /c/<live code>` and `GET /r/<run code>` (a page with Open Graph tags, and `/card.png`, drawn in the worker with no image library). Redeploy after pulling so the new routes go live.

