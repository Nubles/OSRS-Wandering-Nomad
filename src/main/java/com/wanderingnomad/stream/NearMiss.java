package com.wanderingnomad.stream;

/**
 * Near miss: the edge, moving toward you, came within a tile and turned away (or you stepped in)
 * before it passed you. Stamped "Close call" with the seconds you had to spare, at most once every
 * {@link #COOLDOWN} played minutes.
 */
public final class NearMiss
{
    /** Within this many tiles of the edge is close. */
    public static final double TILES = 1.0;
    public static final int COOLDOWN = 10;
    /** The journey metric that tallies close calls. */
    public static final String METRIC = "nearmisses";

    private int armed = -1;
    private double armedMargin;
    private int last = Integer.MIN_VALUE / 2;

    /**
     * One played minute. {@code before} and {@code after} are tiles from your tile in to your edge
     * before and after the circles moved (below 0 is outside). Returns the seconds you had to spare
     * when a close call is confirmed, or -1.
     */
    public int minute(int minute, double before, double after, boolean inside)
    {
        if (!inside || after < 0 || Double.isNaN(before) || Double.isNaN(after))
        {
            armed = -1;
            return -1;
        }
        double closing = before - after;
        if (closing > 0 && after <= TILES && after < closing)
        {
            // The edge would reach you within a minute at this pace.
            int seconds = (int) Math.max(1, Math.min(59, Math.round(after / closing * 60)));
            armed = armed < 0 ? seconds : Math.min(armed, seconds);
            armedMargin = armed == seconds ? after : Math.min(armedMargin, after);
            return -1;
        }
        if (armed >= 0 && after > armedMargin)
        {
            int seconds = armed;
            armed = -1;
            if (minute - last < COOLDOWN) return -1;
            last = minute;
            return seconds;
        }
        return -1;
    }

    public void reset()
    {
        armed = -1;
        last = Integer.MIN_VALUE / 2;
    }
}
