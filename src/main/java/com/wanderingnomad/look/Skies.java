package com.wanderingnomad.look;

/**
 * Storm skies: while a Gale or Squall blows over Main, rain falls inside it near you (slanted by
 * the wind in a Gale) and lightning flickers now and then. A fixed number of drops, so it never
 * costs more than that.
 */
public final class Skies
{
    /** Drops drawn at once, at most. */
    public static final int DROPS = 70;
    /** Drops fall within this many tiles of you. */
    public static final int REACH = 10;
    /** How long a drop takes to fall. */
    static final double FALL_MS = 700;
    /** Lightning comes at most once in this long. */
    static final long PERIOD = 7000;

    private Skies()
    {
    }

    /**
     * Drop {@code i} now: its offset from you in tiles (x, y) and how far it has fallen (0 at the
     * top, 1 at the ground). Each fall lands somewhere new.
     */
    public static double[] drop(int i, long now)
    {
        double clock = now / FALL_MS + ForkTear.hash(i * 17 + 5);
        // A long: the wall clock in fall lengths (about 2.5 billion) is past an int.
        long fall = (long) Math.floor(clock);
        double x = (ForkTear.hash((int) (i * 31 + fall * 7)) * 2 - 1) * REACH;
        double y = (ForkTear.hash((int) (i * 43 + fall * 11 + 1)) * 2 - 1) * REACH;
        return new double[]{x, y, clock - fall};
    }

    /** The lightning's brightness now, 0 to 1: a quick double flicker in some periods, nothing in others. */
    public static double flash(long now)
    {
        long window = Math.floorDiv(now, PERIOD);
        if (ForkTear.hash((int) window * 7 + 3) > 0.55) return 0;
        long at = (long) (ForkTear.hash((int) window * 13 + 1) * (PERIOD - 600));
        long d = now - window * PERIOD - at;
        if (d < 0 || d > 420) return 0;
        if (d < 90) return 1 - d / 90.0 * 0.3;
        if (d < 160) return 0.15;
        if (d < 240) return 0.85;
        return 0.85 * (1 - (d - 240) / 180.0);
    }
}
