package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;

/**
 * The Wager oath: while a fork is coming, call the half you'll keep. If you stand in that half
 * when the fork parts, the wager pays renown. Calls close once the halves have split.
 */
public final class Wagers
{
    public static final int RENOWN = 40;

    public enum Result
    {
        NONE,
        WON,
        LOST,
    }

    private Wagers()
    {
    }

    /** Whether a call can be made now: a fork's warning is running while Main is yours, and none is called yet. */
    public static boolean open(Journey j, NomadSim sim)
    {
        return sim.forkWarning() > 0 && sim.boundIndex() == NomadSim.MAIN && sim.twin() == null && j.wager == 0;
    }

    /** Calls a half: {@code withAxis} true for the half that heads the fork's way, false for the other. */
    public static boolean call(Journey j, NomadSim sim, boolean withAxis)
    {
        if (!open(j, sim)) return false;
        Circle m = sim.main();
        j.wager = withAxis ? 1 : -1;
        j.wagerX = m.x();
        j.wagerY = m.y();
        j.wagerAxis = sim.forkAxis();
        j.wagerForks = sim.forks();
        return true;
    }

    /** The heading of the half called, or NaN if none. */
    public static double called(Journey j)
    {
        return j.wager == 0 ? Double.NaN : j.wager > 0 ? j.wagerAxis : j.wagerAxis + Math.PI;
    }

    /**
     * Every tick: once the fork has parted, judges the call by which side of where Main stood the
     * surviving half went, if you were inside it. A fork that never came (calmed away) is dropped.
     */
    public static Result settle(Journey j, NomadSim sim, boolean inside)
    {
        if (j.wager == 0) return Result.NONE;
        if (sim.forks() <= j.wagerForks)
        {
            // Called off: no warning, no fork under way.
            if (sim.forkWarning() == 0 && sim.twin() == null) j.wager = 0;
            return Result.NONE;
        }
        Circle m = sim.main();
        double side = (m.x() - j.wagerX) * Math.cos(j.wagerAxis) + (m.y() - j.wagerY) * Math.sin(j.wagerAxis);
        boolean won = inside && sim.boundIndex() == NomadSim.MAIN && Math.signum(side) == j.wager;
        j.wager = 0;
        if (won) j.add(Trials.WAGERS, 1);
        return won ? Result.WON : Result.LOST;
    }
}
