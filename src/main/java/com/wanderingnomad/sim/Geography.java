package com.wanderingnomad.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The fixed outline of Gielinor the circles move over: kingdoms, ports and boat routes. It is
 * coarse on purpose (a few dozen points per kingdom) and never lists quests, items or other
 * content, so it never needs patching when the game changes.
 *
 * <p>The outlines are the prototype's schematic ones from the mock-up and still need tracing
 * against the real map before a release.
 */
public final class Geography
{
    public static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(
        new Region("kandarin", "Kandarin", 0x4FC3F7, new String[]{"Kandarin", "Ardougne"}, new Tile(2580, 3300),
            new int[][]{{2420, 3050}, {2660, 3050}, {2700, 3200}, {2860, 3300}, {2860, 3520}, {2740, 3540}, {2600, 3560}, {2420, 3480}}),
        new Region("fremennik", "Fremennik", 0xB0BEC5, new String[]{"Fremennik"}, new Tile(2750, 3700),
            new int[][]{{2600, 3560}, {2740, 3540}, {2860, 3520}, {2920, 3610}, {2860, 3760}, {2640, 3760}, {2590, 3650}}),
        new Region("asgarnia", "Asgarnia", 0x7986CB, new String[]{"Falador"}, new Tile(2980, 3300),
            new int[][]{{2860, 3300}, {2980, 3200}, {3060, 3190}, {3100, 3250}, {3100, 3520}, {2860, 3520}}),
        new Region("misthalin", "Misthalin", 0x9CCC65, new String[]{"Lumbridge", "Varrock"}, new Tile(3290, 3360),
            new int[][]{{3100, 3150}, {3270, 3140}, {3300, 3250}, {3400, 3330}, {3400, 3520}, {3100, 3520}, {3100, 3250}}),
        new Region("desert", "Kharidian Desert", 0xFFD54F, new String[]{"Desert"}, new Tile(3380, 3040),
            new int[][]{{3270, 3140}, {3200, 2920}, {3520, 2920}, {3480, 3200}, {3400, 3330}, {3300, 3250}}),
        new Region("morytania", "Morytania", 0xBA68C8, new String[]{"Morytania"}, new Tile(3610, 3400),
            new int[][]{{3400, 3330}, {3480, 3200}, {3740, 3180}, {3780, 3560}, {3400, 3560}}),
        new Region("wilderness", "Wilderness", 0xEF5350, new String[]{"Wilderness"}, new Tile(3200, 3740),
            new int[][]{{2944, 3520}, {3400, 3520}, {3400, 3940}, {2944, 3940}}),
        new Region("karamja", "Karamja", 0x26A69A, new String[]{"Karamja"}, new Tile(2840, 3030),
            new int[][]{{2730, 3230}, {2900, 3215}, {2960, 3160}, {2950, 2900}, {2770, 2880}, {2715, 3000}})
    ));

    /** The Main circle's start, and the volcano over TzHaar that only the Main circle opens. */
    public static final Tile LUMBRIDGE = new Tile(3222, 3218);
    public static final Tile VOLCANO = new Tile(2856, 3168);

    public static final class Port
    {
        public final String name;
        public final Tile tile;

        Port(String name, int x, int y)
        {
            this.name = name;
            this.tile = new Tile(x, y);
        }
    }

    public static final List<Port> PORTS = Collections.unmodifiableList(Arrays.asList(
        new Port("Port Sarim", 3029, 3217),
        new Port("Musa Point", 2956, 3146),
        new Port("Brimhaven docks", 2770, 3215),
        new Port("Ardougne docks", 2683, 3271),
        new Port("Catherby docks", 2792, 3414),
        new Port("Port Khazard", 2674, 3144),
        new Port("Port Phasmatys", 3702, 3503)
    ));

    private static final String[][] FERRIES = {{"Port Sarim", "Musa Point"}, {"Ardougne docks", "Brimhaven docks"}};
    private static final List<String> CHARTER = Arrays.asList("Port Sarim", "Brimhaven docks", "Catherby docks",
        "Port Khazard", "Port Phasmatys");

    private Geography()
    {
    }

    public static Region region(String id)
    {
        for (Region r : REGIONS)
        {
            if (r.id.equals(id)) return r;
        }
        return null;
    }

    /** The kingdom a point lies in, or null over water. */
    public static Region regionAt(double x, double y)
    {
        for (Region r : REGIONS)
        {
            if (r.contains(x, y)) return r;
        }
        return null;
    }

    public static boolean onLand(double x, double y)
    {
        return regionAt(x, y) != null;
    }

    public static Port port(String name)
    {
        for (Port p : PORTS)
        {
            if (p.name.equals(name)) return p;
        }
        return null;
    }

    /**
     * Where a boat can take the Main circle from this port. Before Sailing, ferries and charter
     * ships only; after Sailing, any port.
     */
    public static List<String> routesFrom(String name, boolean sailing)
    {
        List<String> out = new ArrayList<>();
        if (sailing)
        {
            for (Port p : PORTS)
            {
                if (!p.name.equals(name)) out.add(p.name);
            }
            return out;
        }
        for (String[] f : FERRIES)
        {
            if (f[0].equals(name)) out.add(f[1]);
            if (f[1].equals(name)) out.add(f[0]);
        }
        if (CHARTER.contains(name))
        {
            for (String n : CHARTER)
            {
                if (!n.equals(name) && !out.contains(n)) out.add(n);
            }
        }
        return out;
    }
}
