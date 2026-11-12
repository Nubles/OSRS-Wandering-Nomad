package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;

/**
 * Meteor shower (a wildcard): once or twice a run, for 15 minutes, a common waystone lands on
 * every landmark inside Main. The three-boon cap still holds, so you choose.
 */
public final class Meteors
{
    public static final int LIFE = 15;
    static final int FIRST_MIN = 4 * 60;
    static final int FIRST_MAX = 20 * 60;
    static final int SECOND_MIN = 6 * 60;
    static final int SECOND_MAX = 30 * 60;
    static final int RETRY = 30;

    int[] at;
    int done;

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        if (at == null)
        {
            Rng dice = new Rng(sim.seed() ^ 0x3E7E0125);
            int first = FIRST_MIN + (int) (dice.next() * (FIRST_MAX - FIRST_MIN));
            at = dice.next() < 0.5 ? new int[]{first} : new int[]{first, first + SECOND_MIN + (int) (dice.next() * (SECOND_MAX - SECOND_MIN))};
        }
        if (done >= at.length || now < at[done]) return;
        int n = sim.meteorShower(LIFE);
        if (n == 0)
        {
            at[done] = now + RETRY;
            return;
        }
        done++;
        out.banner("Meteor shower", n == 1 ? "A waystone fell on Main's one landmark. It burns out in " + LIFE + " minutes."
            : n + " waystones fell on Main's landmarks. They burn out in " + LIFE + " minutes; your hands hold three.", true);
    }

    /** Played minutes until the next shower, or -1 if none is left. */
    public int next(int now)
    {
        return at == null || done >= at.length ? -1 : Math.max(0, at[done] - now);
    }
}
