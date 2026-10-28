package com.wanderingnomad.sim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Where waystones appear: a fixed list of open surface tiles at well-known places, curated once
 * and never patched. Only the tile matters; nothing here depends on quests or content. The
 * website's geography.ts lists the same places in the same order.
 */
public final class Landmarks
{
    public static final class Landmark
    {
        public final String name;
        public final int x;
        public final int y;

        Landmark(String name, int x, int y)
        {
            this.name = name;
            this.x = x;
            this.y = y;
        }
    }

    public static final List<Landmark> ALL = Collections.unmodifiableList(Arrays.asList(
        // Misthalin
        new Landmark("Lumbridge Castle", 3222, 3218),
        new Landmark("Fred's farm", 3190, 3273),
        new Landmark("Draynor Village", 3093, 3245),
        new Landmark("Draynor Manor", 3109, 3352),
        new Landmark("Champions' Guild", 3191, 3362),
        new Landmark("Varrock Square", 3212, 3424),
        new Landmark("Grand Exchange", 3165, 3480),
        new Landmark("Barbarian Village", 3082, 3420),
        new Landmark("Edgeville", 3093, 3493),
        new Landmark("Lumber Yard", 3302, 3490),
        new Landmark("Digsite", 3360, 3417),
        new Landmark("Paterdomus", 3405, 3488),
        new Landmark("Ice Mountain", 3008, 3478),
        new Landmark("Black Knights' Fortress", 3016, 3514),
        // Asgarnia
        new Landmark("Falador Park", 2994, 3376),
        new Landmark("Port Sarim", 3028, 3217),
        new Landmark("Rimmington", 2957, 3214),
        new Landmark("Crafting Guild", 2933, 3285),
        new Landmark("Mudskipper Point", 2995, 3115),
        new Landmark("Taverley", 2895, 3443),
        new Landmark("Burthorpe", 2899, 3544),
        new Landmark("Goblin Village", 2956, 3505),
        // Kandarin
        new Landmark("Catherby", 2810, 3437),
        new Landmark("Seers' Village", 2726, 3485),
        new Landmark("Camelot", 2757, 3477),
        new Landmark("Hemenster", 2636, 3440),
        new Landmark("Fishing Guild", 2611, 3393),
        new Landmark("Legends' Guild", 2729, 3348),
        new Landmark("East Ardougne market", 2662, 3305),
        new Landmark("West Ardougne", 2530, 3300),
        new Landmark("Witchaven", 2720, 3285),
        new Landmark("Port Khazard", 2660, 3160),
        new Landmark("Tree Gnome Village", 2525, 3165),
        new Landmark("Observatory", 2440, 3160),
        new Landmark("Castle Wars", 2442, 3088),
        new Landmark("Yanille", 2606, 3093),
        new Landmark("Feldip Hills", 2550, 2950),
        new Landmark("Tree Gnome Stronghold", 2461, 3444),
        new Landmark("Barbarian Outpost", 2530, 3570),
        // Fremennik
        new Landmark("Rellekka", 2660, 3657),
        new Landmark("The Lighthouse", 2510, 3640),
        new Landmark("Mountain Camp", 2800, 3680),
        new Landmark("Neitiznot", 2336, 3805),
        new Landmark("Jatizso", 2412, 3801),
        new Landmark("Miscellania", 2535, 3862),
        new Landmark("Lunar Isle", 2100, 3915),
        // Kharidian Desert
        new Landmark("Al Kharid", 3293, 3174),
        new Landmark("Emir's Arena", 3365, 3266),
        new Landmark("Shantay Pass", 3304, 3117),
        new Landmark("Bedabin Camp", 3170, 3030),
        new Landmark("Pollnivneach", 3359, 2970),
        new Landmark("Nardah", 3428, 2915),
        new Landmark("Agility Pyramid", 3364, 2830),
        new Landmark("Sophanem", 3296, 2780),
        new Landmark("Uzer", 3490, 3090),
        // Morytania
        new Landmark("Canifis", 3494, 3483),
        new Landmark("Fenkenstrain's Castle", 3550, 3550),
        new Landmark("Port Phasmatys", 3687, 3502),
        new Landmark("Darkmeyer", 3600, 3360),
        new Landmark("Barrows", 3565, 3289),
        new Landmark("Mort'ton", 3489, 3288),
        new Landmark("Burgh de Rott", 3496, 3211),
        new Landmark("Mos Le'Harmless", 3680, 2970),
        // Wilderness
        new Landmark("Ferox Enclave", 3130, 3630),
        new Landmark("Graveyard of Shadows", 3160, 3670),
        new Landmark("Chaos Temple", 3236, 3635),
        new Landmark("Bandit Camp", 3039, 3700),
        new Landmark("Lava Maze", 3075, 3850),
        new Landmark("Demonic Ruins", 3287, 3886),
        new Landmark("Rogues' Castle", 3285, 3930),
        new Landmark("Mage Arena", 3105, 3934),
        new Landmark("Wilderness Agility Course", 2998, 3915),
        // Karamja
        new Landmark("Musa Point", 2914, 3176),
        new Landmark("Brimhaven", 2760, 3178),
        new Landmark("Tai Bwo Wannai", 2790, 3065),
        new Landmark("Shilo Village", 2852, 2955),
        new Landmark("Ape Atoll", 2755, 2784),
        // Kourend & Kebos
        new Landmark("Kourend Castle", 1639, 3673),
        new Landmark("Hosidius", 1745, 3590),
        new Landmark("Port Piscarilius", 1803, 3746),
        new Landmark("Arceuus", 1700, 3790),
        new Landmark("Lovakengj", 1500, 3815),
        new Landmark("Shayzien", 1500, 3600),
        new Landmark("Wintertodt Camp", 1630, 3940),
        new Landmark("Farming Guild", 1248, 3727),
        new Landmark("Mount Quidamortem", 1250, 3570),
        // Tirannwn
        new Landmark("Lletya", 2340, 3170),
        new Landmark("Elf Camp", 2200, 3255),
        new Landmark("Port Tyras", 2150, 3120),
        new Landmark("Zul-Andra", 2200, 3055),
        // Varlamore
        new Landmark("Civitas illa Fortis", 1680, 3105),
        new Landmark("Fortis Colosseum", 1800, 3105),
        new Landmark("Hunter Guild", 1557, 3047)
    ));

    private Landmarks()
    {
    }
}
