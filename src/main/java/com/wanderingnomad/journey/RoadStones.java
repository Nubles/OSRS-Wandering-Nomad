package com.wanderingnomad.journey;

import com.wanderingnomad.sim.NomadSim;
import java.util.ArrayList;
import java.util.List;

/**
 * Waystones the road itself summons, outside the sim: Footprint stones for new map regions walked
 * inside your circle, the Unbroken road for long stretches without stepping outside, and
 * Collection stones for new collection log slots. The marks already paid live in the run's
 * {@link Journey}, so each is summoned once.
 */
public final class RoadStones
{
    /** A common waystone for every this many new map regions walked inside your circle. */
    public static final int SQUARES_EVERY = 25;
    /** A rare waystone for every this many played minutes in a row inside. */
    public static final int ROAD_MINUTES = 480;
    /** A waystone for every this many new collection log slots this run, rare on every {@link #COLLECTION_RARE}th. */
    public static final int COLLECTION_EVERY = 50;
    public static final int COLLECTION_RARE = 250;

    static final String PAID_SQUARES = "paid:squares";
    static final String PAID_COLLECTION = "paid:collection";

    private RoadStones()
    {
    }

    /** One waystone owed: its tier and what earned it. */
    public static final class Due
    {
        public final String tier;
        public final String why;

        Due(String tier, String why)
        {
            this.tier = tier;
            this.why = why;
        }
    }

    /**
     * The waystones owed since the last call, marked paid. Call once a played minute, after
     * {@link Journey#minute(boolean)}. A run saved before these stones existed starts counting from
     * where it stands, so it isn't handed a pile at once.
     */
    public static List<Due> due(Journey j)
    {
        List<Due> out = new ArrayList<>();
        int squares = j.value(Trials.SQUARES);
        int squaresPaid = paid(j, PAID_SQUARES, squares / SQUARES_EVERY);
        while ((squaresPaid + 1) * SQUARES_EVERY <= squares)
        {
            squaresPaid++;
            out.add(new Due(NomadSim.COMMON, squaresPaid * SQUARES_EVERY + " map regions walked inside your circle"));
        }
        j.values.put(PAID_SQUARES, squaresPaid);

        int slots = j.value(Trials.COLLECTION);
        int slotsPaid = paid(j, PAID_COLLECTION, slots / COLLECTION_EVERY);
        while ((slotsPaid + 1) * COLLECTION_EVERY <= slots)
        {
            slotsPaid++;
            int mark = slotsPaid * COLLECTION_EVERY;
            out.add(new Due(mark % COLLECTION_RARE == 0 ? NomadSim.RARE : NomadSim.COMMON, mark + " new collection log slots"));
        }
        j.values.put(PAID_COLLECTION, slotsPaid);

        int streak = j.streak();
        if (streak > 0 && streak % ROAD_MINUTES == 0)
        {
            out.add(new Due(NomadSim.RARE, "The Unbroken road (" + streak / 60 + " hours without stepping outside)"));
        }
        return out;
    }

    /** Minutes in a row inside right now, and to the next Unbroken road stone. */
    public static int roadToGo(Journey j)
    {
        return ROAD_MINUTES - j.streak() % ROAD_MINUTES;
    }

    /** Map regions until the next Footprint stone. */
    public static int squaresToGo(Journey j)
    {
        return SQUARES_EVERY - j.value(Trials.SQUARES) % SQUARES_EVERY;
    }

    /** The marks already paid, or {@code baseline} the first time, for a run saved before they were counted. */
    private static int paid(Journey j, String key, int baseline)
    {
        Integer was = j.values.get(key);
        return was == null ? baseline : was;
    }
}
