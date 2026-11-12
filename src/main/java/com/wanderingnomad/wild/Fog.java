package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;

/**
 * Fog bank (a wildcard): now and then, for 30 minutes, the world map, Radar and Forecast go dark
 * and only the wall shows the circle. Ten minutes' warning, and Main lingers while it lasts.
 */
public final class Fog
{
    public static final int MINUTES = 30;
    public static final int WARN = 10;
    static final int GAP_MIN = 4 * 60;
    static final int GAP_MAX = 12 * 60;
    static final int CALM_EVERY = 10;

    int next;
    int warn;
    int left;
    int banks;

    public boolean on()
    {
        return left > 0;
    }

    public int warning()
    {
        return warn;
    }

    public int left()
    {
        return left;
    }

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        if (next == 0) next = now + gap(sim.seed());
        if (left > 0)
        {
            if (--left == 0)
            {
                out.say("The fog lifts. The map, Radar and Forecast are back.");
                banks++;
                next = now + gap(sim.seed());
            }
            else if (left % CALM_EVERY == 0)
            {
                sim.steer(NomadSim.MAIN, 0, 0, NomadSim.LINGER, Math.min(left, CALM_EVERY + 1));
            }
            return;
        }
        if (warn > 0)
        {
            if (--warn > 0) return;
            left = MINUTES;
            sim.steer(NomadSim.MAIN, 0, 0, NomadSim.LINGER, CALM_EVERY + 1);
            out.banner("Fog bank", "For " + MINUTES + " minutes the world map, Radar and Forecast go dark. Only the wall shows the circle, and Main lingers.", true);
            return;
        }
        if (now >= next)
        {
            warn = WARN;
            out.banner("Fog rolls in", "In " + WARN + " minutes the map goes dark for half an hour. Look where your circle is now.", false);
        }
    }

    private int gap(int seed)
    {
        return GAP_MIN + (int) (new Rng(seed ^ ((banks + 1) * 0xF06B)).next() * (GAP_MAX - GAP_MIN));
    }
}
