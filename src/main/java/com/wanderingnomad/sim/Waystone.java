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
    /** Its tier: common, rare or legendary (null in runs saved before tiers were kept). */
    String tier;
    /** Played minutes since it rose. A common stone left alone ripens into a rare one. */
    int age;
    /** A pilgrim stone rises just outside your circle and can be claimed from outside it. */
    boolean pilgrim;

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

    public String tier()
    {
        return tier == null ? NomadSim.COMMON : tier;
    }

    public int age()
    {
        return age;
    }

    public boolean pilgrim()
    {
        return pilgrim;
    }

    public boolean legendary()
    {
        return NomadSim.LEGENDARY.equals(tier);
    }

    /** Minutes until a common stone ripens into a rare one, or -1 if it won't. */
    public int ripensIn()
    {
        return NomadSim.COMMON.equals(tier) && !pilgrim ? Math.max(0, NomadSim.RIPEN - age) : -1;
    }

    Waystone copy()
    {
        Waystone w = new Waystone();
        w.landmark = landmark;
        w.x = x;
        w.y = y;
        w.circle = circle;
        w.boon = boon;
        w.tier = tier;
        w.age = age;
        w.pilgrim = pilgrim;
        return w;
    }
}
