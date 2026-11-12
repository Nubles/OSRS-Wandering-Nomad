package com.wanderingnomad.journey;

import java.util.ArrayList;
import java.util.List;

/**
 * Circle lineage: Main has a name, and each fork's surviving half is its next generation, so the
 * run keeps a family tree ("Gertrude IV, born at 31h 12m, lived 2h 05m"). Cosmetic only. The
 * names are a fixed list written once; the run's seed picks one.
 */
public final class Lineage
{
    static final String[] NAMES = {
        "Gertrude", "Aldous", "Wren", "Bartholomew", "Isolde", "Cuthbert", "Maud", "Osric", "Hildegard", "Edwin",
        "Rowena", "Godfrey", "Agatha", "Percival", "Elspeth", "Ambrose", "Winifred", "Tobias", "Morwenna", "Lucan",
        "Beatrix", "Ulric", "Sybil", "Fenwick", "Ottilie", "Rufus", "Clementine", "Hamish", "Ysolda", "Barnaby",
    };

    private Lineage()
    {
    }

    /** The name the run's seed gives Main's line. */
    public static String family(long seed)
    {
        int h = (int) (seed ^ (seed >>> 32));
        h = (h ^ (h >>> 16)) * 0x45D9F3B;
        h = h ^ (h >>> 16);
        return NAMES[Math.floorMod(h, NAMES.length)];
    }

    /** A generation's name, such as "Gertrude III". */
    public static String name(String family, int generation)
    {
        return family + " " + roman(generation);
    }

    /** The current generation's name for a run. */
    public static String current(Journey j)
    {
        return j.family == null ? null : name(j.family, Math.max(1, j.births.size()));
    }

    /**
     * Brings the run's family tree up to date: a generation is born at each fork's parting. A run
     * saved before lineage was kept starts its tree now, with the generations before unknown.
     * Returns the new generation's name if one was just born, else null.
     */
    public static String sync(Journey j, long seed, int forks, int minute)
    {
        if (j.family == null) j.family = family(seed);
        int generations = forks + 1;
        if (j.births.isEmpty())
        {
            for (int g = 1; g < generations; g++) j.births.add(-1);
            j.births.add(generations == 1 ? 0 : -1);
            return null;
        }
        String born = null;
        while (j.births.size() < generations)
        {
            j.births.add(minute);
            born = name(j.family, j.births.size());
        }
        return born;
    }

    /** The family tree, oldest first, one line per generation. */
    public static List<String> tree(Journey j, int minute)
    {
        List<String> out = new ArrayList<>();
        if (j.family == null) return out;
        for (int g = 0; g < j.births.size(); g++)
        {
            int born = j.births.get(g);
            boolean last = g == j.births.size() - 1;
            int end = last ? minute : j.births.get(g + 1);
            String line = name(j.family, g + 1);
            if (born < 0)
            {
                line += last ? ", alive now" : ", before the tree was kept";
            }
            else
            {
                line += ", born at " + time(born) + (end >= born ? (last ? ", alive for " : ", lived ") + time(end - born) : "");
                if (!last) line += ", then forked";
            }
            out.add(line);
        }
        return out;
    }

    static String roman(int n)
    {
        if (n <= 0 || n >= 4000) return String.valueOf(n);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] marks = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++)
        {
            while (n >= values[i])
            {
                out.append(marks[i]);
                n -= values[i];
            }
        }
        return out.toString();
    }

    private static String time(int minutes)
    {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
