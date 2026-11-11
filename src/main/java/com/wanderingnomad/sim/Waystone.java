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
    /** Minutes it lasts before it fades whatever its circle does, 0 if only its circle drifting off it ends it (lightning, flotsam). */
    int life;
    /** A wandering stone hops to a landmark ahead every {@link NomadSim#WANDER_EVERY} minutes, along this heading. */
    boolean wander;
    double heading;
    /** Rites (a setting): whether its rite has been done. */
    boolean riteDone;

    /** The rites a waystone can ask for before it gives up its boon (with Rites on). */
    public static final String BOW = "bow";
    public static final String EAT = "eat";
    public static final String FIRE = "fire";
    public static final String KILL = "kill";
    static final String[] RITES = {BOW, EAT, FIRE, KILL};

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

    /** Minutes it lasts however its circle drifts, or 0 if it lasts until its circle leaves it. */
    public int life()
    {
        return life;
    }

    public boolean wanders()
    {
        return wander;
    }

    public double heading()
    {
        return heading;
    }

    public boolean riteDone()
    {
        return riteDone;
    }

    /** The rite this stone asks for (with Rites on): one of {@link #BOW}, {@link #EAT}, {@link #FIRE}, {@link #KILL}, from where it stands. */
    public String rite()
    {
        return RITES[Math.floorMod(x * 7 + y * 13, RITES.length)];
    }

    /** The rite in words, to follow "to claim it, ". */
    public static String riteText(String rite)
    {
        switch (rite)
        {
            case BOW: return "bow to it";
            case EAT: return "eat something beside it";
            case FIRE: return "light a fire beside it";
            default: return "kill something near it";
        }
    }

    public boolean legendary()
    {
        return NomadSim.LEGENDARY.equals(tier);
    }

    /** Minutes until a common stone ripens into a rare one, or -1 if it won't. */
    public int ripensIn()
    {
        return NomadSim.COMMON.equals(tier) && !pilgrim && life == 0 ? Math.max(0, NomadSim.RIPEN - age) : -1;
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
        w.life = life;
        w.wander = wander;
        w.heading = heading;
        w.riteDone = riteDone;
        return w;
    }
}
