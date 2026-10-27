// The site reads the same world.dat the plugin ships, so the two can never disagree about
// where the land and kingdoms are. This copies it into public/ before dev, test and build.
import { copyFileSync, mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const web = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const world = resolve(web, '../src/main/resources/com/wanderingnomad/sim/world.dat');
mkdirSync(resolve(web, 'public'), { recursive: true });
copyFileSync(world, resolve(web, 'public/world.dat'));
