import type { Tile } from './geography';
import { dist } from './geography';

/**
 * Rings and roots: the travel networks Main can hop the way it sails, once you've unlocked them,
 * and the Wilderness obelisks. In the same order as Networks.java.
 */
export const FAIRY_RING = 0;
export const SPIRIT_TREE = 1;
export const GLIDER = 2;
export const BALLOON = 3;
export const CARPET = 4;
export const CANOE = 5;
export const QUETZAL = 6;
export const NETWORK_NAMES = ['fairy ring', 'spirit tree', 'gnome glider', 'balloon', 'magic carpet', 'canoe', 'quetzal'];

export interface Stop extends Tile {
  network: number;
  name: string;
}

export const STOPS: Stop[] = [
  { network: 0, name: 'Mudskipper Point (AIQ)', x: 2996, y: 3114 },
  { network: 0, name: 'the Wizards\' Tower (DIS)', x: 3108, y: 3149 },
  { network: 0, name: 'Edgeville (DKR)', x: 3129, y: 3496 },
  { network: 0, name: 'Canifis (CKS)', x: 3447, y: 3470 },
  { network: 0, name: 'Mort Myre (BKR)', x: 3469, y: 3431 },
  { network: 0, name: 'the Legends\' Guild (BLR)', x: 2740, y: 3351 },
  { network: 0, name: 'McGrubor\'s Wood (ALS)', x: 2644, y: 3495 },
  { network: 0, name: 'Piscatoris (AKQ)', x: 2319, y: 3619 },
  { network: 0, name: 'Castle Wars (BKP)', x: 2385, y: 3035 },
  { network: 0, name: 'south Tai Bwo Wannai (CKR)', x: 2801, y: 3003 },
  { network: 0, name: 'the Feldip Hills (AKS)', x: 2571, y: 2956 },
  { network: 0, name: 'Nardah (DLQ)', x: 3423, y: 3016 },
  { network: 0, name: 'Sinclair Mansion (CJR)', x: 2705, y: 3576 },
  { network: 1, name: 'Tree Gnome Village', x: 2542, y: 3170 },
  { network: 1, name: 'the Gnome Stronghold', x: 2461, y: 3444 },
  { network: 1, name: 'the Battlefield of Khazard', x: 2555, y: 3259 },
  { network: 1, name: 'the Grand Exchange', x: 3185, y: 3508 },
  { network: 1, name: 'the Feldip Hills', x: 2488, y: 2850 },
  { network: 1, name: 'the Poison Waste', x: 2339, y: 3109 },
  { network: 2, name: 'Ta Quir Priw (Gnome Stronghold)', x: 2465, y: 3501 },
  { network: 2, name: 'Sindarpos (White Wolf Mountain)', x: 2848, y: 3497 },
  { network: 2, name: 'Lemanto Andra (Digsite)', x: 3321, y: 3427 },
  { network: 2, name: 'Kar-Hewo (Al Kharid)', x: 3284, y: 3213 },
  { network: 2, name: 'Gandius (Karamja)', x: 2971, y: 2969 },
  { network: 2, name: 'Lemantolly Undri (Feldip Hills)', x: 2544, y: 2970 },
  { network: 3, name: 'Entrana', x: 2809, y: 3356 },
  { network: 3, name: 'Taverley', x: 2936, y: 3420 },
  { network: 3, name: 'the Crafting Guild', x: 2924, y: 3302 },
  { network: 3, name: 'Varrock', x: 3298, y: 3481 },
  { network: 3, name: 'Castle Wars', x: 2462, y: 3108 },
  { network: 3, name: 'the Grand Tree', x: 2480, y: 3458 },
  { network: 4, name: 'the Shantay Pass', x: 3311, y: 3109 },
  { network: 4, name: 'Pollnivneach', x: 3349, y: 3002 },
  { network: 4, name: 'Uzer', x: 3469, y: 3113 },
  { network: 4, name: 'the Bedabin Camp', x: 3181, y: 3043 },
  { network: 4, name: 'Nardah', x: 3401, y: 2916 },
  { network: 4, name: 'Sophanem', x: 3285, y: 2813 },
  { network: 5, name: 'Lumbridge', x: 3241, y: 3235 },
  { network: 5, name: 'the Champions\' Guild', x: 3202, y: 3343 },
  { network: 5, name: 'Barbarian Village', x: 3111, y: 3409 },
  { network: 5, name: 'Edgeville', x: 3132, y: 3510 },
  { network: 6, name: 'Civitas illa Fortis', x: 1697, y: 3140 },
  { network: 6, name: 'the Hunter Guild', x: 1585, y: 3053 },
  { network: 6, name: 'the Sunset Coast', x: 1548, y: 2995 },
  { network: 6, name: 'Quetzacalli Gorge', x: 1510, y: 3222 },
  { network: 6, name: 'Aldarin', x: 1389, y: 2901 },
  { network: 6, name: 'Cam Torum', x: 1446, y: 3108 },
];

export const OBELISKS: Tile[] = [
  { x: 3156, y: 3620 },
  { x: 3227, y: 3667 },
  { x: 3035, y: 3732 },
  { x: 3106, y: 3794 },
  { x: 2980, y: 3866 },
  { x: 3307, y: 3916 },
];

export const stopsOf = (network: number) => STOPS.filter((n) => n.network === network);
export const stopByName = (name: string | undefined) => STOPS.find((n) => n.name === name) ?? null;

/** The obelisk within `reach` tiles of a tile, or null. */
export function obeliskNear(x: number, y: number, reach: number): Tile | null {
  for (const o of OBELISKS) if (dist(o.x - x, o.y - y) <= reach) return o;
  return null;
}
