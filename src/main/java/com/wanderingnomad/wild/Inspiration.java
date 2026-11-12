package com.wanderingnomad.wild;

/**
 * Inspiration (a wildcard): a bold play (a comeback: a photo finish, back from the brink, a last
 * breath) earns one Inspiration, held outside the boon cap, one at most. Spend it from the sidebar
 * to reroll the coming fork, the omen or your circle's mood.
 */
public final class Inspiration
{
    public static final String MOOD = "mood";
    public static final String FORK = "fork";
    public static final String OMEN = "omen";

    boolean held;
    int seen = -1;
    int spent;

    public boolean held()
    {
        return held;
    }

    /** Each minute, with the run's comeback count. */
    void minute(int comebacks, Out out)
    {
        if (seen < 0) seen = comebacks;
        if (comebacks <= seen) return;
        seen = comebacks;
        if (held) return;
        held = true;
        out.banner("Inspired", "A bold play earns Inspiration. Spend it from the sidebar to reroll the coming fork, the omen or your circle's mood.", true);
    }

    /** Spends it; true if there was one. */
    boolean spend()
    {
        if (!held) return false;
        held = false;
        spent++;
        return true;
    }
}
