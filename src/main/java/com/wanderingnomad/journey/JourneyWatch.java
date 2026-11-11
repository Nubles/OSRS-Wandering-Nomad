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
    public static final int AT_LANDMARK = 4;
    /** Played minutes after Main docks in which being back inside still counts as following it. */
    static final int FOLLOW_MINUTES = 60;

    private boolean primed;
    private boolean forked;
    private boolean sailing;
    private Tile lastTile;
    private String voyageTo;
    /** Comebacks: ticks seen, which half of a fork you stand in (1 Main, 2 its twin, 0 neither or both) and the tick you stepped into it. */
    private int ticks;
    private int side;
    private int sideTick;
    /** Ticks in a row within a tile of your circle's middle, for a secret trial. */
    private int still;
    /** Ticks in a fork's last half minute, as the comeback counts it. */
    static final int FORK_LATE = 50;
    static final int STILL_TICKS = 100;

    /** The game's own region id for a tile: which 64 by 64 square of the map it's in. */
    public static int region(int x, int y)
    {
        return ((x >> 6) << 8) | (y >> 6);
    }

    public void reset()
    {
        primed = false;
        lastTile = null;
        side = 0;
        still = 0;
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
        ticks++;
        if (nowForked && at != null)
        {
            boolean inMain = sim.main().contains(at.x, at.y);
            boolean inTwin = sim.twin().contains(at.x, at.y);
            int now = inMain && !inTwin ? 1 : inTwin && !inMain ? 2 : 0;
            if (now != side && now != 0) sideTick = ticks;
            side = now;
        }
        if (forked && !nowForked && main)
        {
            if (inside)
            {
                j.add(Trials.FORKS, 1);
                if (sim.stormLeft() > 0 && sim.stormKind() == NomadSim.SQUALL) j.add(Trials.SQUALL_FORKS, 1);
                if (ticks - sideTick <= FORK_LATE && sideTick > 0)
                {
                    int secs = (int) Math.round((ticks - sideTick) * 0.6);
                    j.comeback(Trials.COMEBACK_FORK, "stepped into the half you kept " + secs + " seconds before the fork parted", 60 - secs);
                }
            }
            else if (at != null)
            {
                Circle m = sim.main();
                j.scar(Math.atan2(at.y - m.y(), at.x - m.x()), "fork", "A fork parted while you stood outside, at "
                    + Acts.hours(sim.minutes()) + ".", sim.minutes());
            }
            side = 0;
            sideTick = 0;
        }
        forked = nowForked;
        j.atLeast(Trials.STORMS, sim.storms());
        if (sim.stormLeft() > 0 && !sim.main().sailing())
        {
            com.wanderingnomad.sim.Region over = com.wanderingnomad.sim.Geography.regionAt(sim.main().x(), sim.main().y());
            if (over != null) j.addTo(Trials.STORM_KINGDOMS, over.id);
        }
        if (nowSailing) voyageTo = sim.main().voyageTo();
        if (sailing && !nowSailing)
        {
            j.docked = sim.minutes();
            j.dockedAt = voyageTo;
        }
        sailing = nowSailing;
        if (j.docked >= 0)
        {
            if (sim.minutes() - j.docked > FOLLOW_MINUTES) j.docked = -1;
            else if (main && inside && !nowSailing)
            {
                j.add(Trials.VOYAGES, 1);
                if (j.dockedAt != null) j.addTo(Trials.HARBOURS, j.dockedAt);
                if (j.dockedAt != null && j.firstHarbour == null) j.firstHarbour = j.dockedAt;
                j.docked = -1;
            }
        }
        if (!inside || at == null)
        {
            still = 0;
            return;
        }
        still = Math.hypot(at.x - yours.x(), at.y - yours.y()) <= 1 ? still + 1 : 0;
        j.atLeast(Trials.SECRET_STILL, still / STILL_TICKS);
        int around = 0;
        for (Circle c : sim.circles()) if (c.contains(at.x, at.y)) around++;
        if (sim.twin() != null && sim.twin().contains(at.x, at.y)) around++;
        if (around >= 3) j.atLeast(Trials.SECRET_TRIPLE, 1);
        if (main && sim.stormLeft() > 0 && Math.hypot(at.x - sim.main().x(), at.y - sim.main().y()) <= 3) j.atLeast(Trials.SECRET_EYE, 1);
        if (yours.isKingdom()) j.addTo(Trials.REGIONS, yours.region().id);
        j.addTo(Trials.SQUARES, String.valueOf(region(at.x, at.y)));
        if (at.equals(lastTile)) return;
        lastTile = at;
        Landmarks.Landmark kept = Trials.seedLandmark(sim.seed());
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (Math.abs(l.x - at.x) > AT_LANDMARK || Math.abs(l.y - at.y) > AT_LANDMARK) continue;
            j.addTo(Trials.LANDMARKS, l.name);
            if (l == kept) j.atLeast(Trials.SECRET_SEED, 1);
        }
    }
}
