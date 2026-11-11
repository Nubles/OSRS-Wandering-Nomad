package com.wanderingnomad.social;

import com.wanderingnomad.journey.Bestiary;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Trials;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;

/**
 * The Gauntlet: a dare sent to a rival as a code, built from the omens' kinds. They have 24 played
 * hours to finish it, and it scales to what their circle can reach when they take it up.
 */
public final class Dare
{
    public static final String WALK = "walk";
    public static final String CLAIM = "claim";
    public static final String CROSS = "cross";
    public static final String SPECIES = "species";
    public static final String LANDMARKS = "landmarks";
    public static final String SQUARES = "squares";
    public static final String[] KINDS = {WALK, CLAIM, CROSS, SPECIES, LANDMARKS, SQUARES};
    public static final String[] SIZES = {"Small", "Medium", "Large"};
    static final int[][] TARGETS = {{1500, 3000, 6000}, {1, 2, 4}, {2, 4, 8}, {3, 6, 12}, {2, 4, 8}, {4, 8, 16}};
    public static final int[] RENOWN = {30, 60, 120};
    public static final int HOURS = 24;

    public enum Result
    {
        NONE,
        WON,
        LOST,
    }

    /** Sent: who dared, what and how big, and when. */
    public String from;
    public String kind;
    public int size;
    public long made;
    /** Taken up: the target it scaled to, the count when taken, and the played minute it runs out. */
    public int target;
    public int base;
    public int deadline;

    public static Dare send(String from, String kind, int size, long now)
    {
        Dare d = new Dare();
        d.from = Codes.clean(from, Bottle.NAME_MAX);
        d.kind = kind;
        d.size = Math.max(0, Math.min(SIZES.length - 1, size));
        d.made = now;
        return d;
    }

    static int kindIndex(String kind)
    {
        for (int i = 0; i < KINDS.length; i++) if (KINDS[i].equals(kind)) return i;
        return -1;
    }

    /** The count a dare of this kind reads from the run. */
    public static int count(Journey j, String kind)
    {
        switch (kind)
        {
            case WALK:
                return j.value(Trials.WALKED);
            case CLAIM:
                return j.value(Trials.CLAIMED);
            case CROSS:
                return j.value(Trials.CROSSINGS);
            case SPECIES:
                return Bestiary.reached(j, 1);
            case LANDMARKS:
                return j.value(Trials.LANDMARKS);
            default:
                return j.value(Trials.SQUARES);
        }
    }

    /**
     * Takes up a pasted dare: scales it to your circle and starts its clock. False if it's not a
     * real dare. A landmark dare asks for no more landmarks than your circle holds.
     */
    public boolean take(Journey j, NomadSim sim)
    {
        int k = kindIndex(kind);
        if (k < 0 || from == null) return false;
        from = Codes.clean(from, Bottle.NAME_MAX);
        size = Math.max(0, Math.min(SIZES.length - 1, size));
        target = TARGETS[k][size];
        if (LANDMARKS.equals(kind))
        {
            Circle yours = sim.bound();
            int inside = 0;
            for (Landmarks.Landmark l : Landmarks.ALL) if (yours.contains(l.x, l.y)) inside++;
            target = Math.max(1, Math.min(target, inside));
        }
        base = count(j, kind);
        deadline = sim.minutes() + HOURS * 60;
        return true;
    }

    public int progress(Journey j)
    {
        return Math.max(0, Math.min(target, count(j, kind) - base));
    }

    /** Each played minute: done, run out, or neither yet. */
    public Result settle(Journey j, int now)
    {
        if (progress(j) >= target) return Result.WON;
        return now >= deadline ? Result.LOST : Result.NONE;
    }

    public int renown()
    {
        return RENOWN[Math.max(0, Math.min(RENOWN.length - 1, size))];
    }

    /** "Walk 3,000 tiles", for the sidebar and the code's sender. */
    public String ask()
    {
        int n = target > 0 ? target : TARGETS[Math.max(0, kindIndex(kind))][size];
        switch (kind)
        {
            case WALK:
                return String.format("Walk %,d tiles", n);
            case CLAIM:
                return "Claim " + n + (n == 1 ? " waystone" : " waystones");
            case CROSS:
                return "Cross between circles " + n + (n == 1 ? " time" : " times");
            case SPECIES:
                return "Kill " + n + " kinds of monster you haven't this run";
            case LANDMARKS:
                return "Stand at " + n + (n == 1 ? " landmark" : " landmarks");
            default:
                return "Walk inside " + n + " new map squares";
        }
    }
}
