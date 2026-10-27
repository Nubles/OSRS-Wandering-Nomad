package com.wanderingnomad.sim;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The fixed outline of Gielinor the circles move over: land and kingdoms (from world.dat), ports
 * and the boat routes between them. It never lists quests, items or other content, so it never
 * needs patching when the game changes; only a new landmass would mean rebuilding world.dat.
 */
public final class Geography
{
    static final World WORLD = World.load();

    /** The eleven kingdoms with a region circle. Indexes follow scripts/build_world.py. */
    public static final List<Region> REGIONS = Collections.unmodifiableList(Arrays.asList(
        new Region("kandarin", "Kandarin", 0x4FC3F7, 0, "Kandarin", "Ardougne"),
        new Region("fremennik", "Fremennik", 0xB0BEC5, 1, "Fremennik"),
        new Region("asgarnia", "Asgarnia", 0x7986CB, 2, "Falador"),
        new Region("misthalin", "Misthalin", 0x9CCC65, 3, "Lumbridge", "Varrock"),
        new Region("desert", "Kharidian Desert", 0xFFD54F, 4, "Desert"),
        new Region("morytania", "Morytania", 0xBA68C8, 5, "Morytania"),
        new Region("wilderness", "Wilderness", 0xEF5350, 6, "Wilderness"),
        new Region("karamja", "Karamja", 0x26A69A, 7, "Karamja"),
        new Region("kourend", "Kourend & Kebos", 0xF06292, 8, "Kourend"),
        new Region("tirannwn", "Tirannwn", 0x81C784, 9, "Western"),
        // Varlamore has no Achievement Diary yet, so its circle doesn't grow.
        new Region("varlamore", "Varlamore", 0xFFA726, 10)
    ));

    /** The Main circle's start, and the volcano over TzHaar that only the Main circle opens. */
    public static final Tile LUMBRIDGE = new Tile(3222, 3218);
    public static final Tile VOLCANO = new Tile(2856, 3168);

    public static final class Port
    {
        public final String name;
        /** A land tile at the dock, where the Main circle's centre lands after a voyage. */
        public final Tile tile;

        Port(String name, int chunkX, int chunkY)
        {
            this.name = name;
            this.tile = nearestLand(chunkX * 64 + 32, chunkY * 64 + 32);
        }
    }

    /** Docks by map chunk, as Fate Locked's travel table lists them (data/travelLinks.ts). */
    public static final List<Port> PORTS = Collections.unmodifiableList(Arrays.asList(
        new Port("Port Sarim", 47, 50),
        new Port("Musa Point", 46, 49),
        new Port("Rimmington", 45, 50),
        new Port("Brimhaven", 43, 50),
        new Port("East Ardougne", 41, 51),
        new Port("Catherby", 43, 53),
        new Port("Port Khazard", 41, 49),
        new Port("Corsair Cove", 40, 44),
        new Port("Void Knights' Outpost", 41, 41),
        new Port("Entrana", 44, 52),
        new Port("Port Piscarilius", 28, 57),
        new Port("Land's End", 23, 53),
        new Port("Rellekka", 41, 57),
        new Port("Waterbirth Island", 39, 58)
    ));

    /**
     * Boats anyone can take without a quest, each sailing between every pair of its docks: the
     * Port Sarim ferry, Veos, the Entrana monks, Captain Barnaby, Captain Tock, the Void Knights'
     * squire, Jarvald, and the charter ships' quest-free ports.
     */
    private static final String[][] BOATS = {
        {"Port Sarim", "Musa Point"},
        {"Port Sarim", "Port Piscarilius", "Land's End"},
        {"Port Sarim", "Entrana"},
        {"East Ardougne", "Brimhaven", "Rimmington"},
        {"Rimmington", "Corsair Cove"},
        {"Port Sarim", "Void Knights' Outpost"},
        {"Rellekka", "Waterbirth Island"},
        {"Port Sarim", "Musa Point", "Brimhaven", "Catherby", "Port Khazard", "Corsair Cove", "Port Piscarilius", "Land's End"},
    };

    static
    {
        for (Region r : REGIONS)
        {
            r.setHome(middleOf(r));
        }
    }

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

    /** The kingdom with a region circle that a tile lies in, or null over water or elsewhere. */
    public static Region regionAt(double x, double y)
    {
        if (!WORLD.land(x, y)) return null;
        int k = WORLD.kingdom(x, y);
        for (Region r : REGIONS)
        {
            if (r.index == k) return r;
        }
        return null;
    }

    private static Pockets pockets;

    private static Pockets pockets()
    {
        if (pockets == null) pockets = new Pockets(WORLD);
        return pockets;
    }

    /** How many cells of walking it is from a tile out to open land: 0 in open land, -1 at sea or on an island. */
    public static int pocketDepth(double x, double y)
    {
        return pockets().depth(x, y);
    }

    /**
     * In a pocket of land (a peninsula, a cove behind a narrow neck), a point a few cells along the
     * shortest walk out to open land, as {x, y}; null in open land or where no walk leads out.
     */
    public static double[] wayOut(double x, double y, int lookahead)
    {
        return pockets().wayOut(x, y, lookahead);
    }

    /** Any land the Main circle may drift over, islands included. */
    public static boolean onLand(double x, double y)
    {
        return WORLD.land(x, y);
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
     * Where a boat can take the Main circle from this port. Before Sailing, the quest-free boats
     * only; after Sailing, any port.
     */
    public static List<String> routesFrom(String name, boolean sailing)
    {
        Set<String> out = new LinkedHashSet<>();
        if (sailing)
        {
            for (Port p : PORTS)
            {
                if (!p.name.equals(name)) out.add(p.name);
            }
            return new ArrayList<>(out);
        }
        for (String[] boat : BOATS)
        {
            if (!Arrays.asList(boat).contains(name)) continue;
            for (String stop : boat)
            {
                if (!stop.equals(name)) out.add(stop);
            }
        }
        return new ArrayList<>(out);
    }

    /** The land cell nearest a tile, searched outward ring by ring. */
    static Tile nearestLand(int x, int y)
    {
        int step = WORLD.cell();
        for (int ring = 0; ring < 64; ring++)
        {
            for (int dx = -ring; dx <= ring; dx++)
            {
                for (int dy = -ring; dy <= ring; dy++)
                {
                    if (Math.max(Math.abs(dx), Math.abs(dy)) != ring) continue;
                    int tx = x + dx * step, ty = y + dy * step;
                    if (WORLD.land(tx, ty)) return new Tile(tx, ty);
                }
            }
        }
        return new Tile(x, y);
    }

    /** The kingdom's land cell nearest the middle of all its land. */
    private static Tile middleOf(Region r)
    {
        int step = WORLD.cell();
        long sx = 0, sy = 0, n = 0;
        for (int row = 0; row < WORLD.rows(); row++)
        {
            for (int col = 0; col < WORLD.cols(); col++)
            {
                int x = WORLD.x0() + col * step + step / 2, y = WORLD.y0() + row * step + step / 2;
                if (r.contains(x, y))
                {
                    sx += x;
                    sy += y;
                    n++;
                }
            }
        }
        if (n == 0) throw new IllegalStateException(r.name + " has no land in world.dat");
        double mx = (double) sx / n, my = (double) sy / n;
        Tile best = null;
        double bestD = Double.MAX_VALUE;
        for (int row = 0; row < WORLD.rows(); row++)
        {
            for (int col = 0; col < WORLD.cols(); col++)
            {
                int x = WORLD.x0() + col * step + step / 2, y = WORLD.y0() + row * step + step / 2;
                double d = Circle.dist(x - mx, y - my);
                if (d < bestD && r.contains(x, y))
                {
                    bestD = d;
                    best = new Tile(x, y);
                }
            }
        }
        return best;
    }
}
