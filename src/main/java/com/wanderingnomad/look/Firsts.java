package com.wanderingnomad.look;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Things seen for the first time, kept as one short saved line: the kingdoms whose title card a
 * run has shown, or the coach tips already given. Tied to a run's seed, or to none (0).
 */
public final class Firsts
{
    private final int seed;
    private final Set<String> seen = new LinkedHashSet<>();

    private Firsts(int seed)
    {
        this.seed = seed;
    }

    /** What was saved, if it was saved for this seed; empty otherwise. */
    public static Firsts read(String saved, int seed)
    {
        Firsts f = new Firsts(seed);
        if (saved == null) return f;
        int bar = saved.indexOf('|');
        if (bar < 0) return f;
        try
        {
            if (Integer.parseInt(saved.substring(0, bar)) != seed) return f;
        }
        catch (NumberFormatException e)
        {
            return f;
        }
        for (String s : saved.substring(bar + 1).split(",")) if (!s.isEmpty()) f.seen.add(s);
        return f;
    }

    /** True the first time {@code key} is seen, and notes it. */
    public boolean first(String key)
    {
        return seen.add(key.replace(",", " ").replace("|", " "));
    }

    public boolean seen(String key)
    {
        return seen.contains(key.replace(",", " ").replace("|", " "));
    }

    public Set<String> all()
    {
        return seen;
    }

    public void clear()
    {
        seen.clear();
    }

    public String write()
    {
        return seed + "|" + String.join(",", seen);
    }
}
