package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import java.util.ArrayList;
import java.util.List;

/**
 * The run ledger: one run's story as plain text, to read in the sidebar or paste into Discord or a
 * video description. It's made from what the plugin already keeps, so it never asks the game for
 * anything.
 */
public final class Ledger
{
    /** How many of the latest trials it names. */
    static final int LATEST = 5;

    private Ledger()
    {
    }

    public static List<String> lines(NomadSim sim, Journey j, Renown r, List<Trial> trials)
    {
        List<String> out = new ArrayList<>();
        Rank rank = r.rank();
        out.add("Nomad UIM run ledger, seed " + sim.seed() + (j.race() != null ? ", racing " + j.race() + " on their seed" : ""));
        out.add("Played " + time(sim.minutes()) + " | " + rank.title + ", " + String.format("%,d", r.renown())
            + " renown (" + String.format("%,d", j.earned()) + " this run)");
        out.add("Walked " + String.format("%,d", j.value(Trials.WALKED)) + " tiles inside | longest streak inside "
            + time(j.value(Trials.STREAK)));
        out.add("Waystones claimed " + j.value(Trials.CLAIMED) + " | boons spent " + j.value(Trials.BOONS)
            + " | crossings " + j.value(Trials.CROSSINGS));
        out.add("Forks parted " + j.value(Trials.FORKS) + " | storms weathered " + sim.storms()
            + " | voyages followed " + j.value(Trials.VOYAGES));
        out.add("Harbours " + j.value(Trials.HARBOURS) + "/" + Geography.PORTS.size()
            + " | regions " + j.value(Trials.REGIONS) + "/" + Geography.REGIONS.size()
            + " | landmarks " + j.value(Trials.LANDMARKS) + "/" + Landmarks.ALL.size()
            + " | map regions walked " + j.value(Trials.SQUARES));
        out.add("Omens met " + j.value(Trials.OMENS) + " of " + j.omensDealt()
            + (j.value(Trials.RENDEZVOUS) > 0 ? " | rivals met " + j.value(Trials.RENDEZVOUS) : ""));
        Trial oath = null;
        for (Trial t : trials)
        {
            if (t.id.equals(j.oath())) oath = t;
        }
        if (oath != null)
        {
            out.add("Oath: " + oath.name + (j.isDone(oath) ? ", kept" : ", " + Math.round(j.progress(oath) * 100) + "% of the way"));
        }
        out.add("Bestiary: " + Bestiary.reached(j, 100) + " monsters past 100 kills, " + Bestiary.reached(j, 1_000) + " past 1,000"
            + (r.titles().isEmpty() ? "" : " | titles " + String.join(", ", r.titles())));
        out.add("Trials " + j.doneCount() + "/" + trials.size() + " done");
        List<String> latest = new ArrayList<>();
        for (String id : j.done)
        {
            for (Trial t : trials)
            {
                if (t.id.equals(id)) latest.add(t.name + " (" + t.tier.label + ")");
            }
        }
        if (!latest.isEmpty())
        {
            int from = Math.max(0, latest.size() - LATEST);
            out.add("Latest: " + String.join(", ", latest.subList(from, latest.size())));
        }
        return out;
    }

    public static String text(NomadSim sim, Journey j, Renown r, List<Trial> trials)
    {
        return String.join("\n", lines(sim, j, r, trials));
    }

    static String time(int minutes)
    {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0 ? h + "h " + String.format("%02d", m) + "m" : m + "m";
    }
}
