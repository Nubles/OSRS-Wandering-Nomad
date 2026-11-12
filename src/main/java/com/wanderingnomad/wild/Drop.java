package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;

/**
 * The Drop (a wildcard): a run opens with a world-sized circle over Lumbridge that closes for an
 * hour onto a landing zone, where Main is born. The circles wait until it lands; inside the
 * closing circle counts as inside, and arriving late only costs outside time.
 */
public final class Drop
{
    public static final int MINUTES = 60;
    static final int START_RADIUS = 360;
    /** The landing zone is this far from Lumbridge, on open land. */
    static final int NEAR = 40;
    static final int FAR = 150;

    int left;
    int fromX;
    int fromY;
    int toX;
    int toY;

    /** Picks the landing zone from the seed and starts the hour. Returns where Main will be born. */
    public Tile begin(int seed)
    {
        Rng dice = new Rng(seed ^ 0xD80B5EED);
        Tile from = Geography.LUMBRIDGE;
        Tile to = from;
        for (int k = 0; k < 60; k++)
        {
            double a = dice.next() * Math.PI * 2;
            double d = NEAR + dice.next() * (FAR - NEAR);
            int x = (int) Math.round(from.x + Math.cos(a) * d);
            int y = (int) Math.round(from.y + Math.sin(a) * d);
            if (Geography.onLand(x, y) && Geography.pocketDepth(x, y) == 0)
            {
                to = new Tile(x, y);
                break;
            }
        }
        fromX = from.x;
        fromY = from.y;
        toX = to.x;
        toY = to.y;
        left = MINUTES;
        return to;
    }

    public boolean on()
    {
        return left > 0;
    }

    public int left()
    {
        return left;
    }

    /** The closing circle now, as x, y and radius; null once it has landed. */
    public double[] circle()
    {
        if (left <= 0) return null;
        double k = (MINUTES - left) / (double) MINUTES;
        // It closes fast at first and slows as it lands, like a parachute.
        double ease = 1 - (1 - k) * (1 - k);
        return new double[]{
            fromX + (toX - fromX) * ease,
            fromY + (toY - fromY) * ease,
            START_RADIUS + (Circle.MAIN_START - START_RADIUS) * ease};
    }

    public boolean contains(Tile t)
    {
        double[] c = circle();
        return c != null && t != null && Math.hypot(t.x - c[0], t.y - c[1]) <= c[2];
    }

    /** A played minute of the drop. Returns true on the minute it lands. */
    boolean minute(Out out)
    {
        if (left <= 0) return false;
        left--;
        if (left == 15) out.say("The Drop: 15 minutes until Main lands.");
        if (left > 0) return false;
        out.banner("Main has landed", "The drop is over and the run begins. Outside Main now counts as outside.", true);
        return true;
    }

    public Tile landing()
    {
        return new Tile(toX, toY);
    }
}
