# Nomad UIM

A RuneLite plugin for **Nomad UIM**: Ultimate Ironman, but everything you do happens inside circles that drift across Gielinor.

- **The Main circle** is where you start, over Lumbridge. It roams the whole world, turns back at the coast, and from a port it can sail a ferry or charter route (any route once Sailing is unlocked).
- **Eight region circles** (Kandarin, Fremennik, Asgarnia, Misthalin, the Desert, Morytania, the Wilderness and Karamja) each wander inside their own kingdom only.
- **Crossing:** where Main touches a region circle, you can cross into it. The only way out of a region circle is back into Main. If Main moves on, you're stranded until it comes back, and your circle edges toward the border nearest it.
- **Achievement Diaries** grow their region's circle: from 8–24 tiles up to 24–40 with every tier done.
- **Outside your circle**, it drifts 5x faster and shrinks a tile a minute.
- **The volcano** (Fight Caves and Inferno) only counts while you're in Main and Main covers it.

## Status: prototype (Solo mode)

What works:

- The full circle simulation, seeded and deterministic, on the account's played time (it only moves while you're logged in).
- The surface tile you count as standing on: underground, in an instance or on a boat it stays the last surface tile you stood on.
- Diary progress read from the game, growing region circles automatically.
- Your circle's edge in the game world and on the minimap, every circle on the world map, a status box and a sidebar with crossings and the run log.
- The run saves per account and resumes where it left off.

Not yet:

- Boons and waystones, the Forecast cone, Passport, the hash-chained run log and Season mode.
- Sailing is a config tick box for now rather than read from the game.
- Kingdom outlines are the mock-up's schematic ones and need tracing against the real map.

## Layout

- `src/main/java/com/wanderingnomad/sim/` is the rules engine: plain Java with no RuneLite types, so it's unit tested without a client.
- `src/main/java/com/wanderingnomad/` is the RuneLite plugin: reading the game, saving, overlays and the sidebar.

## Building

```
gradle check jar     # tests and a jar in build/libs
gradle runClient     # RuneLite in developer mode with the plugin loaded
```
