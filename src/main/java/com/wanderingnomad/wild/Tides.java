package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;

/**
 * Tide tables (a wildcard): a 6-hour tide. While it rises Main leans a little toward the nearest
 * coast every half hour, and while it falls a little inland. It only nudges the heading.
 */
public final class Tides
{
    public static final int PERIOD = 6 * 60;
    static final int EVERY = 30;
    static final double NUDGE = 10 * Math.PI / 180;
    static final int REACH = 160;
    static final int STEP = 8;
    static final int RAYS = 16;

    boolean rising;
    boolean known;

    /** Whether the tide is rising at a played minute, offset by the seed. */
    public static boolean rising(int seed, int minute)
    {
        int phase = Math.floorMod(minute + Math.floorMod(seed, PERIOD), PERIOD);
        return phase < PERIOD / 2;
    }

    /** Minutes until the tide turns. */
    public static int turns(int seed, int minute)
    {
        int phase = Math.floorMod(minute + Math.floorMod(seed, PERIOD), PERIOD);
        return (phase < PERIOD / 2 ? PERIOD / 2 : PERIOD) - phase;
    }

    /** The heading from a point to the nearest water within reach, or NaN if there's none. */
    static double coast(double x, double y)
    {
        for (int d = STEP; d <= REACH; d += STEP)
        {
            for (int k = 0; k < RAYS; k++)
            {
                double a = k * Math.PI * 2 / RAYS;
                if (!Geography.onLand(x + Math.cos(a) * d, y + Math.sin(a) * d)) return a;
            }
        }
        return Double.NaN;
    }

    /** The turn toward (rising) or away from (falling) the coast, no more than the nudge. */
    static double nudge(double heading, double coast, boolean rising)
    {
        double want = rising ? coast : coast + Math.PI;
        double d = Math.atan2(Math.sin(want - heading), Math.cos(want - heading));
        return Math.max(-NUDGE, Math.min(NUDGE, d));
    }

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        boolean r = rising(sim.seed(), now);
        if (!known || r != rising)
        {
            if (known) out.say(r ? "The tide turns and rises: Main leans for the coast." : "The tide turns and falls: Main leans inland.");
            known = true;
            rising = r;
        }
        Circle m = sim.main();
        if (now % EVERY != 0 || m.sailing()) return;
        double coast = coast(m.x(), m.y());
        if (Double.isNaN(coast)) return;
        sim.nudge(NomadSim.MAIN, nudge(m.heading(), coast, r));
    }
}
