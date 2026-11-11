package com.wanderingnomad.look;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Region;

/** Kingdom stingers: each circle's own short sound as you cross into it, under a second each. */
public final class Stingers
{
    /** Prayer chimes, short and distinct: one per kingdom in the order the kingdoms are listed. */
    static final int[] KINGDOMS = {2662, 2664, 2665, 2666, 2667, 2668, 2669, 2670, 2675, 2676, 2677, 2678, 2682, 2684, 2686, 2687,
        2688, 2689, 2690, 2691};
    /** Main's own: the chivalry chime. */
    static final int MAIN = 3826;
    /** The caravan's: the protect item chime. */
    static final int CARAVAN = 1982;

    private Stingers()
    {
    }

    public static int of(Circle c)
    {
        if (c.isCaravan()) return CARAVAN;
        if (c.isMain()) return MAIN;
        Region r = c.region();
        int i = r == null ? 0 : Math.max(0, Geography.REGIONS.indexOf(r));
        return KINGDOMS[i % KINGDOMS.length];
    }
}
