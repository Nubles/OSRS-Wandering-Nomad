package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;

/**
 * Bonfire (a wildcard): a fire you light inside your circle leaves a 6-tile ember pocket that
 * stays legal for 30 minutes after the circle moves off it. One ember at a time, two hours apart.
 */
public final class Embers
{
    public static final int RADIUS = 6;
    static final int LINGER = 30;
    static final int COOLDOWN = 120;

    boolean lit;
    int x;
    int y;
    /** The played minute your circle moved off it, or -1 while it still covers it. */
    int leftAt = -1;
    int ready;

    public boolean on()
    {
        return lit;
    }

    public int x()
    {
        return x;
    }

    public int y()
    {
        return y;
    }

    /** Minutes the pocket has left once your circle has moved off it, or -1 while your circle still covers it. */
    public int left(int now)
    {
        return !lit ? 0 : leftAt < 0 ? -1 : Math.max(0, LINGER - (now - leftAt));
    }

    public boolean contains(Tile t)
    {
        return lit && t != null && Math.hypot(t.x - x, t.y - y) <= RADIUS;
    }

    /** A fire caught where you stand. */
    void light(Tile at, boolean inside, int now, Out out)
    {
        if (at == null || !inside) return;
        if (now < ready)
        {
            return;
        }
        boolean moved = lit;
        lit = true;
        x = at.x;
        y = at.y;
        leftAt = -1;
        ready = now + COOLDOWN;
        out.say((moved ? "The old embers die as new ones catch. " : "") + "Your fire leaves embers: a " + RADIUS
            + "-tile pocket that stays legal for " + LINGER + " minutes after your circle moves on. The next in " + COOLDOWN / 60 + " hours.");
    }

    void minute(NomadSim sim, Out out)
    {
        if (!lit) return;
        int now = sim.minutes();
        if (sim.bound().contains(x, y))
        {
            leftAt = -1;
            return;
        }
        if (leftAt < 0) leftAt = now;
        if (now - leftAt >= LINGER)
        {
            lit = false;
            out.say("Your embers go out.");
        }
    }

    /** Played minutes until a fire leaves embers again (0 if one would now). */
    public int rest(int now)
    {
        return Math.max(0, ready - now);
    }
}
