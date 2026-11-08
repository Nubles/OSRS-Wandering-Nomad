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
    /** Oaths kept, over every run; the first unlocks the Oathbound edge colour. */
    int oathsKept;
    /** Titles earned for 10,000 kills of one monster, such as "Goblin Bane", kept across runs. */
    java.util.Set<String> titles = new java.util.TreeSet<>();
    public static final String OATHBOUND = "Oathbound";
    public static final int OATHBOUND_COLOUR = 0xFFF2C2;
    public static final int OATH_RENOWN = 300;

    public Renown ready()
    {
        if (times == null) times = new HashMap<>();
        if (titles == null) titles = new java.util.TreeSet<>();
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

    /** The edge colour's name in use, Oathbound included. */
    public String paletteName()
    {
        return OATHBOUND.equals(palette) && oathsKept > 0 ? OATHBOUND : palette().palette;
    }

    /** The edge colour in use, as 0xRRGGBB. */
    public int paletteColour()
    {
        return OATHBOUND.equals(palette) && oathsKept > 0 ? OATHBOUND_COLOUR : palette().colour;
    }

    /** Every colour unlocked, in rank order, Oathbound last. */
    public java.util.List<String> unlocked()
    {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Rank r : Rank.values())
        {
            if (r.ordinal() <= rank().ordinal()) out.add(r.palette);
        }
        if (oathsKept > 0) out.add(OATHBOUND);
        return out;
    }

    /** Picks an unlocked colour by name; false if it's locked or unknown. */
    public boolean pick(String name)
    {
        if (OATHBOUND.equalsIgnoreCase(name))
        {
            if (oathsKept <= 0) return false;
            palette = OATHBOUND;
            return true;
        }
        Rank r = Rank.byPalette(name);
        if (r == null || r.ordinal() > rank().ordinal()) return false;
        palette = r.palette;
        return true;
    }

    /** An oath kept: its renown, and the Oathbound colour. Returns the new rank if it went up, else null. */
    public Rank keepOath()
    {
        oathsKept++;
        return add(OATH_RENOWN);
    }

    /** Adds a title; true if it's new to the account. */
    public boolean earnTitle(String title)
    {
        return titles.add(title);
    }

    public java.util.Set<String> titles()
    {
        return java.util.Collections.unmodifiableSet(titles);
    }

    public int oathsKept()
    {
        return oathsKept;
    }
}
