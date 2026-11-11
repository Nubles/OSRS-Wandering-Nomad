package com.wanderingnomad.journey;

import java.util.ArrayList;
import java.util.List;

/**
 * What a run leaves when it ends: its epilogue, its sigil and where Main last stood. The account
 * keeps the latest few in its renown, for the trophy shelf, the rings of lineage, ancestor ghosts
 * and an Ancestral start.
 */
public final class Legacy
{
    /** Runs kept on the account. */
    public static final int KEPT = 12;
    /** Rings of lineage: only runs this long count, and at most this many bands. */
    public static final int BAND_MINUTES = 10 * 60;
    public static final int BANDS = 7;
    /** Ancestor ghosts: the latest runs drawn. */
    public static final int GHOSTS = 5;
    /** Points kept in a sigil. */
    public static final int SIGIL_POINTS = 160;

    /** One ended run. */
    public static final class Run
    {
        public int seed;
        public int minutes;
        public int generation;
        public String heir;
        public String epithet;
        public int colour;
        public List<String> lines = new ArrayList<>();
        /** Main's path, simplified: x, y, flags triples. */
        public int[] sigil;
        public int endX;
        public int endY;
        public long ended;
        /** Where you last died in the run (x, y), its grave for later runs; null if you never did. */
        public int[] grave;

        /** "Generation 3, Gertrude III the Seaborn". */
        public String title()
        {
            return "Generation " + generation + ", " + (heir == null ? "Main" : heir) + " " + (epithet == null ? "" : epithet);
        }
    }

    private Legacy()
    {
    }

    /** Main's path cut down to at most {@code max} samples, fork marks kept on the samples that stay. */
    public static int[] simplify(List<int[]> path, int max)
    {
        int n = path.size();
        if (n == 0) return new int[0];
        int keep = Math.min(n, max);
        int[] out = new int[keep * 3];
        for (int k = 0; k < keep; k++)
        {
            int from = (int) ((long) k * n / keep);
            int to = (int) ((long) (k + 1) * n / keep);
            int[] p = path.get(from);
            int flags = p[2];
            for (int i = from + 1; i < to; i++) flags |= path.get(i)[2] & Journey.FORKED;
            out[k * 3] = p[0];
            out[k * 3 + 1] = p[1];
            out[k * 3 + 2] = flags;
        }
        return out;
    }

    /** The colours of the rings of lineage: the latest long runs, newest first. */
    public static List<Integer> bands(List<Run> runs)
    {
        List<Integer> out = new ArrayList<>();
        for (int i = runs.size() - 1; i >= 0 && out.size() < BANDS; i--)
        {
            if (runs.get(i).minutes >= BAND_MINUTES) out.add(runs.get(i).colour);
        }
        return out;
    }

    /** The runs whose ghosts drift on the map, newest first. */
    public static List<Run> ghosts(List<Run> runs)
    {
        List<Run> out = new ArrayList<>();
        for (int i = runs.size() - 1; i >= 0 && out.size() < GHOSTS; i--) out.add(runs.get(i));
        return out;
    }
}
