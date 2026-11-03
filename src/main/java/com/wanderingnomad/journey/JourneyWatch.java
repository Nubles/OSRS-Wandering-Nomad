package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;

/**
 * Reads the circles' side of the trials off the run, tick by tick, the way the banners do: being
 * inside a region circle, standing at a landmark, a fork parting around you, and following Main
 * across the sea. The first look only primes it, so loading a run never counts anything.
 */
public final class JourneyWatch
{
    /** How close counts as standing at a landmark, in tiles each way. */
    static final int AT_LANDMARK = 4;
    /** Played minutes after Main docks in which being back inside still counts as following it. */
    static final int FOLLOW_MINUTES = 60;

    private boolean primed;
    private boolean forked;
    private boolean sailing;
    private Tile lastTile;

    public void reset()
    {
        primed = false;
        lastTile = null;
    }

    /** Every game tick. {@code inside}: standing inside your circle (either half of a forked Main). */
    public void tick(NomadSim sim, Tile at, boolean inside, Journey j)
    {
        Circle yours = sim.bound();
        boolean main = sim.boundIndex() == NomadSim.MAIN;
        boolean nowForked = main && sim.twin() != null;
        boolean nowSailing = sim.main().sailing();
        if (!primed)
        {
            primed = true;
            forked = nowForked;
            sailing = nowSailing;
        }
        if (forked && !nowForked && main && inside) j.add(Trials.FORKS, 1);
        forked = nowForked;
        if (sailing && !nowSailing) j.docked = sim.minutes();
        sailing = nowSailing;
        if (j.docked >= 0)
        {
            if (sim.minutes() - j.docked > FOLLOW_MINUTES) j.docked = -1;
            else if (main && inside && !nowSailing)
            {
                j.add(Trials.VOYAGES, 1);
                j.docked = -1;
            }
        }
        if (!inside || at == null) return;
        if (yours.region() != null) j.addTo(Trials.REGIONS, yours.region().id);
        if (at.equals(lastTile)) return;
        lastTile = at;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (Math.abs(l.x - at.x) <= AT_LANDMARK && Math.abs(l.y - at.y) <= AT_LANDMARK) j.addTo(Trials.LANDMARKS, l.name);
        }
    }
}
