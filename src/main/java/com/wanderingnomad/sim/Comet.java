package com.wanderingnomad.sim;

/**
 * A comet: about once a day of play it streaks across the map in a straight line through the
 * Main circle, after a 20-minute warning. Stand in its path as it passes to catch it, and a rare
 * waystone answers. Plain fields so it saves with the run.
 */
public final class Comet
{
    /** Where it is now (before it starts moving, where it will start). */
    double x;
    double y;
    /** How far it moves each played minute. */
    double dx;
    double dy;
    /** Minutes of warning left before it starts to move, then minutes of flight left. */
    int warn;
    int left;

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    /** Where its flight ends. */
    public double endX()
    {
        return x + dx * left;
    }

    public double endY()
    {
        return y + dy * left;
    }

    public double heading()
    {
        return StrictMath.atan2(dy, dx);
    }

    public int warning()
    {
        return warn;
    }

    public int flightLeft()
    {
        return left;
    }

    public boolean flying()
    {
        return warn <= 0;
    }

    Comet copy()
    {
        Comet c = new Comet();
        c.x = x;
        c.y = y;
        c.dx = dx;
        c.dy = dy;
        c.warn = warn;
        c.left = left;
        return c;
    }
}
