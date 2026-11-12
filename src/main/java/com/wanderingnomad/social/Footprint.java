package com.wanderingnomad.social;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

/**
 * Clan atlas: a footprint code carries the map squares someone has walked inside their circle.
 * Pasted together, a clan's footprints light one shared map; a newer code from the same name
 * replaces the older.
 */
public final class Footprint
{
    public String name;
    public long at;
    public int[] squares;

    public static Footprint of(String name, Set<String> squares, long now)
    {
        Footprint f = new Footprint();
        f.name = Codes.clean(name, Bottle.NAME_MAX);
        f.at = now;
        Set<Integer> ids = new TreeSet<>();
        for (String s : squares)
        {
            try
            {
                ids.add(Integer.parseInt(s));
            }
            catch (NumberFormatException e)
            {
                // Not a square id: skipped.
            }
        }
        f.squares = ids.stream().mapToInt(Integer::intValue).toArray();
        return f;
    }

    /** Checks a pasted footprint; false if it's not a real one. */
    public boolean valid()
    {
        if (name == null || squares == null) return false;
        name = Codes.clean(name, Bottle.NAME_MAX);
        return !name.isEmpty() && squares.length <= 65536;
    }

    /** Every square lit by the clan's footprints and your own run, as region ids. */
    public static Set<Integer> union(Collection<Footprint> clan, Set<String> mine)
    {
        Set<Integer> out = new TreeSet<>();
        for (Footprint f : clan)
        {
            if (f.squares == null) continue;
            for (int s : f.squares) out.add(s);
        }
        for (String s : mine)
        {
            try
            {
                out.add(Integer.parseInt(s));
            }
            catch (NumberFormatException e)
            {
                // Not a square id: skipped.
            }
        }
        return out;
    }
}
