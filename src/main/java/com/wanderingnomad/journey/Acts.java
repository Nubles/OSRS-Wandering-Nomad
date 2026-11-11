package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Region;
import java.util.Map;

/**
 * Acts: a run splits at its natural milestones (Main's first voyage you follow, your first diary
 * tier, 1,500 total level and your first 99), each opening with a title banner and a fresh mood
 * for Main. Each act is named for the kingdom you spent most time in during it.
 */
public final class Acts
{
    public static final String VOYAGE = "voyage";
    public static final String DIARY = "diary";
    public static final String TOTAL = "total";
    public static final String NINETY_NINE = "99";
    static final String[] OPENERS = {VOYAGE, DIARY, TOTAL, NINETY_NINE};
    static final int TOTAL_MARK = 1500;
    private static final String[] NUMERALS = {"I", "II", "III", "IV", "V", "VI"};

    /** A new act: its number (from 1), the act before it as named, and what opened it. */
    public static final class Opening
    {
        public final int number;
        public final String closed;
        public final String opener;

        Opening(int number, String closed, String opener)
        {
            this.number = number;
            this.closed = closed;
            this.opener = opener;
        }

        public String title()
        {
            return "Act " + numeral(number);
        }

        /** The banner's line: how the last act is remembered, and what opened this one. */
        public String line()
        {
            return "Act " + numeral(number - 1) + " was " + closed + ". " + why(opener) + " opens the next, and Main picks a fresh mood.";
        }
    }

    private Acts()
    {
    }

    public static String numeral(int n)
    {
        return n >= 1 && n <= NUMERALS.length ? NUMERALS[n - 1] : String.valueOf(n);
    }

    static String why(String opener)
    {
        switch (opener)
        {
            case VOYAGE:
                return "Following Main across the sea";
            case DIARY:
                return "Your first diary tier";
            case TOTAL:
                return "Reaching 1,500 total level";
            default:
                return "Your first 99";
        }
    }

    /** A played minute inside a kingdom's circle (null for Main), toward this act's name. */
    public static void minute(Journey j, String regionId, int now)
    {
        // Act I always opens the run, even on a run saved before acts were kept.
        if (j.acts.isEmpty()) j.acts.add(new Journey.Act(0, null));
        if (regionId != null) j.actKingdoms.merge(regionId, 1, Integer::sum);
    }

    /** The kingdom (region id) this act has been lived in most, or null if none yet. */
    public static String dominant(Journey j)
    {
        String best = null;
        int most = 0;
        for (Map.Entry<String, Integer> e : j.actKingdoms.entrySet())
        {
            if (e.getValue() > most || (e.getValue() == most && best != null && e.getKey().compareTo(best) < 0))
            {
                best = e.getKey();
                most = e.getValue();
            }
        }
        return best;
    }

    /** An act's name from its kingdom: "the Misthalin act", or "the wandering act" if no kingdom held it. */
    public static String name(String regionId)
    {
        Region r = regionId == null ? null : Geography.region(regionId);
        return r == null ? "the wandering act" : "the " + r.name + " act";
    }

    /** The act under way, numbered from 1, with its provisional name. */
    public static String current(Journey j)
    {
        int n = Math.max(1, j.acts.size());
        return "Act " + numeral(n) + ": " + name(dominant(j));
    }

    /** Whether a milestone has been reached during the run. */
    static boolean reached(Journey j, String opener)
    {
        switch (opener)
        {
            case VOYAGE:
                return j.value(Trials.VOYAGES) >= 1;
            case DIARY:
                return j.start.containsKey(Trials.DIARY_TIERS) && j.value(Trials.DIARY_TIERS) > j.start.get(Trials.DIARY_TIERS);
            case TOTAL:
                return j.start.containsKey(Trials.TOTAL_LEVEL) && j.start.get(Trials.TOTAL_LEVEL) < TOTAL_MARK
                    && j.value(Trials.TOTAL_LEVEL) >= TOTAL_MARK;
            default:
                return j.start.containsKey(Trials.MAXED) && j.value(Trials.MAXED) > j.start.get(Trials.MAXED);
        }
    }

    /** Opens the next act if a milestone not yet used has been reached; null if none. One per call. */
    public static Opening check(Journey j, int now)
    {
        // Act I always opens the run, even on a run saved before acts were kept.
        if (j.acts.isEmpty()) j.acts.add(new Journey.Act(0, null));
        for (String opener : OPENERS)
        {
            boolean used = false;
            for (Journey.Act a : j.acts) used |= opener.equals(a.opened);
            if (used || !reached(j, opener)) continue;
            Journey.Act last = j.acts.get(j.acts.size() - 1);
            last.kingdom = dominant(j);
            j.acts.add(new Journey.Act(now, opener));
            j.actKingdoms.clear();
            return new Opening(j.acts.size(), name(last.kingdom), opener);
        }
        return null;
    }

    /** Every act, as "Act I: the Misthalin act (0h 00m)", for the epilogue. */
    public static java.util.List<String> lines(Journey j, int now)
    {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (int i = 0; i < j.acts.size(); i++)
        {
            Journey.Act a = j.acts.get(i);
            boolean last = i == j.acts.size() - 1;
            String kingdom = last ? dominant(j) : a.kingdom;
            int end = last ? now : j.acts.get(i + 1).start;
            out.add("Act " + numeral(i + 1) + ": " + name(kingdom) + ", " + hours(end - a.start)
                + (a.opened == null ? "" : ", opened by " + Character.toLowerCase(why(a.opened).charAt(0)) + why(a.opened).substring(1)));
        }
        return out;
    }

    static String hours(int minutes)
    {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
