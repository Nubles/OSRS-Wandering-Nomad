package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class WaystoneTest
{
    @Test
    public void aWaystoneRisesAtALandmarkInReachAndIsClaimedByStandingOnIt()
    {
        NomadSim sim = NomadSim.start(2024);
        // Lumbridge Castle is the start, so it's in reach.
        assertNotNull(sim.summon(NomadSim.COMMON));
        Waystone w = sim.waystones().get(0);
        assertTrue(sim.circles().get(w.circle()).contains(w.x(), w.y()));
        if (w.circle() == sim.boundIndex())
        {
            sim.step(new Tile(w.x(), w.y() + NomadSim.CLAIM_REACH));
            assertEquals(1, sim.boons().size());
            assertTrue(sim.waystones().isEmpty());
        }
    }

    @Test
    public void summonsDoNotChangeWhereTheCirclesGo()
    {
        NomadSim plain = NomadSim.start(77);
        NomadSim summoned = NomadSim.start(77);
        for (int i = 0; i < 600; i++)
        {
            if (i % 50 == 0) summoned.summon(i % 100 == 0 ? NomadSim.RARE : NomadSim.COMMON);
            plain.step(null);
            summoned.step(null);
        }
        for (int i = 0; i < plain.circles().size(); i++)
        {
            assertEquals(plain.circles().get(i).x(), summoned.circles().get(i).x(), 0);
            assertEquals(plain.circles().get(i).y(), summoned.circles().get(i).y(), 0);
        }
    }

    @Test
    public void youHoldThreeBoonsAtMost()
    {
        NomadSim sim = NomadSim.start(5);
        sim.boons.add(NomadSim.ANCHOR);
        sim.boons.add(NomadSim.ANCHOR);
        sim.boons.add(NomadSim.ANCHOR);
        sim.summon(NomadSim.COMMON);
        Waystone w = sim.waystones().get(0);
        sim.bound = w.circle();
        sim.step(new Tile(w.x(), w.y()));
        assertEquals(3, sim.boons().size());
    }

    @Test
    public void expandGrowsTheRangeAndGivesItBack()
    {
        NomadSim sim = NomadSim.start(9);
        Circle c = sim.bound();
        double min = c.radiusMin(), max = c.radiusMax(), r = c.radius();
        sim.boons.add(NomadSim.EXPAND);
        assertNotNull(sim.useBoon(0, 0));
        assertEquals(min + 12, c.radiusMin(), 0);
        assertEquals(max + 12, c.radiusMax(), 0);
        assertEquals(r + 12, c.radius(), 1e-9);
        for (int i = 0; i < 120; i++) sim.step(null);
        assertEquals(0, c.expandLeft());
        assertEquals(min, c.radiusMin(), 0);
        assertEquals(max, c.radiusMax(), 0);
    }

    @Test
    public void anchorHoldsTheCircleStill()
    {
        NomadSim sim = NomadSim.start(9);
        sim.boons.add(NomadSim.ANCHOR);
        sim.useBoon(0, 0);
        double x = sim.bound().x(), y = sim.bound().y();
        for (int i = 0; i < 59; i++) sim.step(null);
        assertEquals(x, sim.bound().x(), 0);
        assertEquals(y, sim.bound().y(), 0);
        sim.step(null);
        sim.step(null);
        assertTrue(sim.bound().x() != x || sim.bound().y() != y);
    }

    @Test
    public void aBridgeCrossesAGapAndTheWalkIsNotOutside()
    {
        NomadSim sim = NomadSim.start(482913);
        int target = -1;
        for (int i = 0; i < 6000 && target < 0; i++)
        {
            sim.step(null);
            for (int j = 1; j < sim.circles().size(); j++)
            {
                double gap = sim.main().gap(sim.circles().get(j));
                if (gap > 5 && gap < NomadSim.BRIDGE_REACH) target = j;
            }
        }
        assertTrue("some region circle should come within 60 tiles", target > 0);
        Tile centre = new Tile((int) Math.round(sim.main().x()), (int) Math.round(sim.main().y()));
        assertFalse(sim.canCross(target, centre));
        sim.boons.add(NomadSim.BRIDGE);
        assertNotNull(sim.useBoon(0, 0));
        assertTrue(sim.canCross(target, centre));
        assertNotNull(sim.crossTo(target, centre));
        sim.step(centre);
        assertFalse(sim.isOutside());
    }

    @Test
    public void runsSavedBeforeWaystonesStillLoad()
    {
        Gson gson = new Gson();
        String old = gson.toJson(NomadSim.start(3)).replaceAll(",\"waystones\":\\[\\],\"boons\":\\[\\],\"pending\":\\[\\],\"summons\":0,\"bridgeLeft\":0", "");
        assertFalse(old.contains("waystones"));
        NomadSim sim = gson.fromJson(old, NomadSim.class);
        sim.summon(NomadSim.COMMON);
        sim.step(null);
        assertEquals(sim.waystones().size() + sim.pendingSummons(), 1);
    }
}
