package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;

/**
 * Visa (a wildcard): crossing into a kingdom's circle stamps a 3-hour visa, half an hour longer
 * for each diary tier done there. Overstay it and that circle dashes for Main to deport you.
 * Nothing is blocked: it only moves the circle.
 */
public final class Visa
{
    public static final int HOURS = 3;
    static final int PER_TIER = 30;
    static final int WARN = 15;
    static final int AIM_EVERY = 10;

    int circle = -1;
    int since;
    boolean deporting;
    int lastAim = -1000;

    /** Minutes left on your visa, or -1 if you're not in a kingdom's circle; 0 once overstayed. */
    public int left(NomadSim sim)
    {
        if (circle < 0 || circle != sim.boundIndex()) return -1;
        return Math.max(0, length(sim.bound()) - (sim.minutes() - since));
    }

    static int length(Circle c)
    {
        return HOURS * 60 + PER_TIER * c.tiers();
    }

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        int b = sim.boundIndex();
        Circle c = sim.bound();
        if (b == NomadSim.MAIN || !c.isKingdom())
        {
            circle = -1;
            deporting = false;
            return;
        }
        if (b != circle)
        {
            circle = b;
            since = now;
            deporting = false;
            out.say("Visa stamped: " + time(length(c)) + " in " + c.name() + ".");
            return;
        }
        int left = length(c) - (now - since);
        if (left == WARN) out.say("Your visa for " + c.name() + " runs out in " + WARN + " minutes.");
        if (!deporting && left <= 0)
        {
            deporting = true;
            lastAim = -1000;
            out.banner("Visa overstayed", c.name() + "'s circle dashes for Main to deport you. Cross into Main to settle it.", true);
        }
        if (deporting && now - lastAim >= AIM_EVERY)
        {
            lastAim = now;
            Circle m = sim.main();
            sim.steer(b, (int) Math.round(m.x()), (int) Math.round(m.y()), NomadSim.DASH, AIM_EVERY + 5);
        }
    }

    static String time(int minutes)
    {
        return minutes % 60 == 0 ? minutes / 60 + " hours" : minutes / 60 + "h " + minutes % 60 + "m";
    }
}
