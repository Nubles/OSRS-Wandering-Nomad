import type { Tile } from './geography';

export interface Landmark extends Tile {
  name: string;
}

/**
 * Where waystones appear: open surface tiles at well-known places, in the same order as
 * Landmarks.java. Only the tile matters; nothing here depends on quests or content.
 */
export const LANDMARKS: Landmark[] = ([
  // Misthalin
  ['Lumbridge Castle', 3222, 3218],
  ["Fred's farm", 3190, 3273],
  ['Draynor Village', 3093, 3245],
  ['Draynor Manor', 3109, 3352],
  ["Champions' Guild", 3191, 3362],
  ['Varrock Square', 3212, 3424],
  ['Grand Exchange', 3165, 3480],
  ['Barbarian Village', 3082, 3420],
  ['Edgeville', 3093, 3493],
  ['Lumber Yard', 3302, 3490],
  ['Digsite', 3360, 3417],
  ['Paterdomus', 3405, 3488],
  ['Ice Mountain', 3008, 3478],
  ["Black Knights' Fortress", 3016, 3514],
  // Asgarnia
  ['Falador Park', 2994, 3376],
  ['Port Sarim', 3028, 3217],
  ['Rimmington', 2957, 3214],
  ['Crafting Guild', 2933, 3285],
  ['Mudskipper Point', 2995, 3115],
  ['Taverley', 2895, 3443],
  ['Burthorpe', 2899, 3544],
  ['Goblin Village', 2956, 3505],
  // Kandarin
  ['Catherby', 2810, 3437],
  ["Seers' Village", 2726, 3485],
  ['Camelot', 2757, 3477],
  ['Hemenster', 2636, 3440],
  ['Fishing Guild', 2611, 3393],
  ["Legends' Guild", 2729, 3348],
  ['East Ardougne market', 2662, 3305],
  ['West Ardougne', 2530, 3300],
  ['Witchaven', 2720, 3285],
  ['Port Khazard', 2660, 3160],
  ['Tree Gnome Village', 2525, 3165],
  ['Observatory', 2440, 3160],
  ['Castle Wars', 2442, 3088],
  ['Yanille', 2606, 3093],
  ['Feldip Hills', 2550, 2950],
  ['Tree Gnome Stronghold', 2461, 3444],
  ['Barbarian Outpost', 2530, 3570],
  // Fremennik
  ['Rellekka', 2660, 3657],
  ['The Lighthouse', 2510, 3640],
  ['Mountain Camp', 2800, 3680],
  ['Neitiznot', 2336, 3805],
  ['Jatizso', 2412, 3801],
  ['Miscellania', 2535, 3862],
  ['Lunar Isle', 2100, 3915],
  // Kharidian Desert
  ['Al Kharid', 3293, 3174],
  ["Emir's Arena", 3365, 3266],
  ['Shantay Pass', 3304, 3117],
  ['Bedabin Camp', 3170, 3030],
  ['Pollnivneach', 3359, 2970],
  ['Nardah', 3428, 2915],
  ['Agility Pyramid', 3364, 2830],
  ['Sophanem', 3296, 2780],
  ['Uzer', 3490, 3090],
  // Morytania
  ['Canifis', 3494, 3483],
  ["Fenkenstrain's Castle", 3550, 3550],
  ['Port Phasmatys', 3687, 3502],
  ['Darkmeyer', 3600, 3360],
  ['Barrows', 3565, 3289],
  ["Mort'ton", 3489, 3288],
  ['Burgh de Rott', 3496, 3211],
  ["Mos Le'Harmless", 3680, 2970],
  // Wilderness
  ['Ferox Enclave', 3130, 3630],
  ['Graveyard of Shadows', 3160, 3670],
  ['Chaos Temple', 3236, 3635],
  ['Bandit Camp', 3039, 3700],
  ['Lava Maze', 3075, 3850],
  ['Demonic Ruins', 3287, 3886],
  ["Rogues' Castle", 3285, 3930],
  ['Mage Arena', 3105, 3934],
  ['Wilderness Agility Course', 2998, 3915],
  // Karamja
  ['Musa Point', 2914, 3176],
  ['Brimhaven', 2760, 3178],
  ['Tai Bwo Wannai', 2790, 3065],
  ['Shilo Village', 2852, 2955],
  ['Ape Atoll', 2755, 2784],
  // Kourend & Kebos
  ['Kourend Castle', 1639, 3673],
  ['Hosidius', 1745, 3590],
  ['Port Piscarilius', 1803, 3746],
  ['Arceuus', 1700, 3790],
  ['Lovakengj', 1500, 3815],
  ['Shayzien', 1500, 3600],
  ['Wintertodt Camp', 1630, 3940],
  ['Farming Guild', 1248, 3727],
  ['Mount Quidamortem', 1250, 3570],
  // Tirannwn
  ['Lletya', 2340, 3170],
  ['Elf Camp', 2200, 3255],
  ['Port Tyras', 2150, 3120],
  ['Zul-Andra', 2200, 3055],
  // Varlamore
  ['Civitas illa Fortis', 1680, 3105],
  ['Fortis Colosseum', 1800, 3105],
  ['Hunter Guild', 1557, 3047],
] as [string, number, number][]).map(([name, x, y]) => ({ name, x, y }));
