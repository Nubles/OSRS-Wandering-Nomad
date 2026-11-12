package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;
import java.util.HashSet;
import java.util.Set;

/**
 * Egg steps (a wildcard): every third trial you finish lays an Egg, which hatches into a random
 * boon after 1,500 new tiles walked inside your circle. Only tiles it hasn't been carried over
 * count, so pacing back and forth does nothing. One egg at a time.
 */
public final class Eggs
{
    public static final int STEPS = 1500;
    static final int EVERY = 3;
    static final double RARE = 0.25;

    boolean egg;
    int trials;
    int hatched;
    Set<Integer> walked = new HashSet<>();
    boolean waiting;

    public boolean held()
    {
        return egg;
    }

    public int steps()
    {
        return walked == null ? 0 : walked.size();
    }

    /** A trial was finished. */
    void trial(Out out)
    {
        trials++;
        if (trials % EVERY != 0 || egg) return;
        egg = true;
        walked = new HashSet<>();
        waiting = false;
        out.banner("An Egg", "It hatches into a random boon after " + String.format("%,d", STEPS) + " new tiles walked inside your circle.", false);
    }

    /** Each game tick: the tile you stand on, if you're inside. */
    void step(Tile at, boolean inside)
    {
        if (!egg || !inside || at == null || walked.size() >= STEPS) return;
        walked.add(at.x * 16384 + at.y);
    }

    void minute(NomadSim sim, Out out)
    {
        if (!egg || walked.size() < STEPS) return;
        Rng dice = new Rng(sim.seed() ^ ((hatched + 1) * 0x6E66));
        String tier = dice.next() < RARE ? NomadSim.RARE : NomadSim.COMMON;
        String boon = sim.grantBoon(tier, 0xE66 + hatched);
        if (boon == null)
        {
            if (!waiting) out.say("Your Egg wobbles: it hatches when your hands have room for a boon.");
            waiting = true;
            return;
        }
        egg = false;
        hatched++;
        walked = new HashSet<>();
        waiting = false;
        out.banner("The Egg hatched", "Out came " + boon + " (" + tier + ").", true);
    }

    void ready()
    {
        if (walked == null) walked = new HashSet<>();
    }
}
