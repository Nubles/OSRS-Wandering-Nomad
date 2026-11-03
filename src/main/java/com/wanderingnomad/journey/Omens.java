package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * Deals the run's omens: the first an hour in, then one every 2 to 4 played hours after the last
 * ends, each drawn from the run's seed and how many came before, so a run's omens can't be
 * rerolled by relogging. Plugin-side only; the circles never see them.
 */
public final class Omens
{
    static final int FIRST = 60;
    static final int GAP_MIN = 120;
    static final int GAP_MAX = 240;
    /** How long each kind gives you, in played minutes. */
    static final int PILGRIMAGE_MINUTES = 90;
    static final int HOLD_MINUTES = 120;
    static final int WANDER_MINUTES = 120;
    static final int WANDER_TILES = 1500;
    static final int HUNT_MINUTES = 180;
    /** A pilgrimage's landmark is inside your circle, this far from you in tiles. */
    static final int NEAR = 20;
    static final int FAR = 110;
    public static final int RENOWN = 25;

    public enum Result
    {
        NONE,
        DEALT,
        MET,
        MISSED,
    }

    private Omens()
    {
    }

    /** "Metric" for the hunt: boss kill counts and Treasure Trails, all of them. */
    public static int trophies(Journey j)
    {
        int n = j.value(Trials.KILLS);
        for (String tier : Trials.CLUE_TIERS) n += j.value(Trials.clue(tier));
        return n;
    }

    /**
     * Once a played minute, and right after anything an omen counts: deals the next omen when
     * it's due, and settles the current one. {@code at} is your surface tile (null if unknown).
     */
    public static Result minute(Journey j, NomadSim sim, Tile at, boolean inside)
    {
        int now = sim.minutes();
        Omen o = j.omen;
        if (o == null)
        {
            if (j.nextOmen == 0) j.nextOmen = FIRST;
            if (now < j.nextOmen) return Result.NONE;
            j.omen = deal(j, sim, at, now);
            return j.omen == null ? Result.NONE : Result.DEALT;
        }
        boolean met;
        boolean missed = false;
        switch (o.kind)
        {
            case Omen.PILGRIMAGE:
                met = inside && at != null && Math.abs(at.x - o.x) <= JourneyWatch.AT_LANDMARK && Math.abs(at.y - o.y) <= JourneyWatch.AT_LANDMARK;
                break;
            case Omen.HOLD_FAST:
                missed = !inside && at != null;
                met = !missed && now >= o.deadline;
                break;
            case Omen.WANDERLUST:
                met = j.value(Trials.WALKED) - o.base >= o.target;
                break;
            default:
                met = trophies(j) - o.base >= o.target;
                break;
        }
        if (!met && now >= o.deadline) missed = true;
        if (!met && !missed) return Result.NONE;
        j.omen = null;
        j.omensDealt++;
        Rng dice = dice(sim.seed(), j.omensDealt);
        j.nextOmen = now + GAP_MIN + (int) (dice.next() * (GAP_MAX - GAP_MIN + 1));
        if (met)
        {
            j.add(Trials.OMENS, 1);
            return Result.MET;
        }
        return Result.MISSED;
    }

    /** The omen that was just settled or dealt is gone; this is what the sidebar shows now. */
    public static Omen current(Journey j)
    {
        return j.omen;
    }

    static Rng dice(int seed, int index)
    {
        return new Rng(seed ^ ((index + 1) * 0x2C1B3C6D) ^ 0x5EED0E5);
    }

    static Omen deal(Journey j, NomadSim sim, Tile at, int now)
    {
        Rng dice = dice(sim.seed(), j.omensDealt + 1000);
        List<String> kinds = new ArrayList<>();
        kinds.add(Omen.HOLD_FAST);
        kinds.add(Omen.WANDERLUST);
        kinds.add(Omen.HUNT);
        List<Landmarks.Landmark> near = new ArrayList<>();
        if (at != null)
        {
            Circle yours = sim.bound();
            for (Landmarks.Landmark l : Landmarks.ALL)
            {
                double d = Math.hypot(l.x - at.x, l.y - at.y);
                if (d >= NEAR && d <= FAR && yours.contains(l.x, l.y)) near.add(l);
            }
            // A pilgrimage is the omen's heart: twice as likely when a landmark is in reach.
            if (!near.isEmpty())
            {
                kinds.add(Omen.PILGRIMAGE);
                kinds.add(Omen.PILGRIMAGE);
            }
        }
        Omen o = new Omen();
        o.kind = kinds.get((int) (dice.next() * kinds.size()));
        o.start = now;
        switch (o.kind)
        {
            case Omen.PILGRIMAGE:
                Landmarks.Landmark l = near.get((int) (dice.next() * near.size()));
                o.landmark = l.name;
                o.x = l.x;
                o.y = l.y;
                o.deadline = now + PILGRIMAGE_MINUTES;
                break;
            case Omen.HOLD_FAST:
                o.deadline = now + HOLD_MINUTES;
                break;
            case Omen.WANDERLUST:
                o.base = j.value(Trials.WALKED);
                o.target = WANDER_TILES;
                o.deadline = now + WANDER_MINUTES;
                break;
            default:
                o.base = trophies(j);
                o.target = 1;
                o.deadline = now + HUNT_MINUTES;
                break;
        }
        return o;
    }
}
