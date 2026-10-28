package com.wanderingnomad.sim;

/**
 * A waystone a milestone summoned: it holds one boon at a landmark inside a circle, and it fades
 * once that circle drifts off the landmark. Stand on it, inside that circle, to claim the boon.
 */
public final class Waystone
{
    String landmark;
    int x;
    int y;
    /** The circle it stands in, by index. */
    int circle;
    String boon;

    public String landmark()
    {
        return landmark;
    }

    public int x()
    {
        return x;
    }

    public int y()
    {
        return y;
    }

    public int circle()
    {
        return circle;
    }

    public String boon()
    {
        return boon;
    }

    Waystone copy()
    {
        Waystone w = new Waystone();
        w.landmark = landmark;
        w.x = x;
        w.y = y;
        w.circle = circle;
        w.boon = boon;
        return w;
    }
}
