package com.wanderingnomad.social;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.SavedRun;
import java.util.ArrayList;
import java.util.List;

/** Tale of the tape: two run codes side by side, with a winner on each line. */
public final class Tape
{
    /** One line: what it measures, each side's value, and who wins it (0 neither, 1 left, 2 right). */
    public static final class Row
    {
        public final String label;
        public final String left;
        public final String right;
        public final int winner;

        Row(String label, String left, String right, int winner)
        {
            this.label = label;
            this.left = left;
            this.right = right;
            this.winner = winner;
        }
    }

    private Tape()
    {
    }

    public static List<Row> compare(SavedRun a, SavedRun b)
    {
        NomadSim x = a.sim;
        NomadSim y = b.sim;
        List<Row> out = new ArrayList<>();
        out.add(new Row("Epithet", a.epithet == null ? "-" : a.epithet, b.epithet == null ? "-" : b.epithet, 0));
        out.add(row("Played", x.minutes(), y.minutes(), hours(x.minutes()), hours(y.minutes())));
        out.add(row("Main's size", (int) Math.round(x.main().radius()), (int) Math.round(y.main().radius()),
            Math.round(x.main().radius()) + " tiles", Math.round(y.main().radius()) + " tiles"));
        out.add(row("Diary tiers", tiers(x), tiers(y), String.valueOf(tiers(x)), String.valueOf(tiers(y))));
        out.add(row("Forks", x.forks(), y.forks(), String.valueOf(x.forks()), String.valueOf(y.forks())));
        out.add(row("Storms weathered", x.storms(), y.storms(), String.valueOf(x.storms()), String.valueOf(y.storms())));
        out.add(row("Comets caught", x.comets(), y.comets(), String.valueOf(x.comets()), String.valueOf(y.comets())));
        out.add(row("Boons in hand", x.boons().size(), y.boons().size(), String.valueOf(x.boons().size()), String.valueOf(y.boons().size())));
        out.add(row("Combat crowns", x.caTiers(), y.caTiers(), String.valueOf(x.caTiers()), String.valueOf(y.caTiers())));
        out.add(new Row("Fallen", a.fallen ? "Yes" : "No", b.fallen ? "Yes" : "No", a.fallen == b.fallen ? 0 : a.fallen ? 2 : 1));
        return out;
    }

    /** Diary tiers that have grown the kingdoms' circles. */
    static int tiers(NomadSim sim)
    {
        int n = 0;
        for (Circle c : sim.circles()) if (c.isKingdom()) n += c.tiers();
        return n;
    }

    private static Row row(String label, int l, int r, String left, String right)
    {
        return new Row(label, left, right, l == r ? 0 : l > r ? 1 : 2);
    }

    static String hours(int minutes)
    {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
