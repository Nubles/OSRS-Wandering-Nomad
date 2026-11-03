package com.wanderingnomad.journey;

import java.util.HashMap;
import java.util.Map;

/**
 * What the account keeps across runs: the renown its trials have earned, how often each trial has
 * been done, and the edge colour picked. Starting a new run keeps all of it.
 */
public final class Renown
{
    int renown;
    /** Trial id to how many runs have done it. */
    Map<String, Integer> times = new HashMap<>();
    /** The Main edge colour picked, by its name; null for the rank's newest. */
    String palette;
    int runs;

    public Renown ready()
    {
        if (times == null) times = new HashMap<>();
        return this;
    }

    public int renown()
    {
        return renown;
    }

    public Rank rank()
    {
        return Rank.of(renown);
    }

    public int times(Trial t)
    {
        return times.getOrDefault(t.id, 0);
    }

    /** Pays a trial's renown; returns the new rank if it went up, else null. */
    public Rank award(Trial t)
    {
        Rank before = rank();
        renown += t.tier.renown;
        times.merge(t.id, 1, Integer::sum);
        Rank after = rank();
        return after != before ? after : null;
    }

    /** Pays renown that isn't a trial's, such as an omen's; returns the new rank if it went up, else null. */
    public Rank add(int amount)
    {
        Rank before = rank();
        renown += Math.max(0, amount);
        Rank after = rank();
        return after != before ? after : null;
    }

    /** Renown from this rank to the next, from 0 to 1 (1 at the top). */
    public double toNext()
    {
        Rank r = rank();
        Rank n = r.next();
        if (n == null) return 1;
        return (renown - r.renown) / (double) (n.renown - r.renown);
    }

    public void newRun()
    {
        runs++;
    }

    public int runs()
    {
        return runs;
    }

    /** The edge colour in use: the one picked if the rank has it, else the rank's own. */
    public Rank palette()
    {
        Rank picked = palette == null ? null : Rank.byPalette(palette);
        Rank rank = rank();
        return picked != null && picked.ordinal() <= rank.ordinal() ? picked : rank;
    }

    /** Picks an unlocked colour by name; false if it's locked or unknown. */
    public boolean pick(String name)
    {
        Rank r = Rank.byPalette(name);
        if (r == null || r.ordinal() > rank().ordinal()) return false;
        palette = r.palette;
        return true;
    }
}
