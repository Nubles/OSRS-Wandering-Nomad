package com.wanderingnomad.look;

import com.wanderingnomad.sim.NomadSim;

/**
 * The claim ceremony: claiming a waystone sets off a firework over you and a rising three-note
 * chime, bigger for a rare and biggest for a legendary. One plays at a time; a rarer claim takes
 * over a commoner one still playing, never the other way round.
 */
public final class Ceremony
{
    public static final int NOTES = 3;
    /** Time between the chime's notes. */
    public static final long NOTE_MS = 420;
    /** How long the firework shows. */
    public static final long SHOW_MS = 2600;

    /** The chime for each tier, lowest note first. */
    private static final int[][] CHIMES = {
        {2266, 3924, 3925},
        {3924, 3925, 3825},
        {3925, 3825, 200},
    };

    private String tier;
    private long startedAt = Long.MIN_VALUE / 2;
    private int played = NOTES;

    /** 0 for common, 1 for rare, 2 for legendary. */
    public static int rank(String tier)
    {
        return NomadSim.LEGENDARY.equals(tier) ? 2 : NomadSim.RARE.equals(tier) ? 1 : 0;
    }

    /** The boon a "Waystone claimed" banner names ("Tailwind is yours. ..."), or null. */
    public static String boonOf(String subtitle)
    {
        if (subtitle == null) return null;
        int at = subtitle.indexOf(" is yours");
        return at <= 0 ? null : subtitle.substring(0, at);
    }

    /** Offers a claim's ceremony; true if it plays now. */
    public boolean offer(String tier, long now)
    {
        if (playing(now) && rank(tier) < rank(this.tier)) return false;
        this.tier = tier;
        startedAt = now;
        played = 0;
        return true;
    }

    public boolean playing(long now)
    {
        return tier != null && now - startedAt < SHOW_MS;
    }

    /** The note due now, 0 to 2, once each; -1 when none is. */
    public int due(long now)
    {
        if (tier == null || played >= NOTES || now - startedAt < played * NOTE_MS) return -1;
        return played++;
    }

    /** The sound effect for note {@code note} of a tier's chime. */
    public static int sound(String tier, int note)
    {
        return CHIMES[rank(tier)][Math.max(0, Math.min(NOTES - 1, note))];
    }

    /** How far through the firework, 0 to 1, or -1 when none is showing. */
    public double progress(long now)
    {
        if (!playing(now)) return -1;
        return Math.max(0, (now - startedAt) / (double) SHOW_MS);
    }

    public String tier()
    {
        return tier;
    }
}
