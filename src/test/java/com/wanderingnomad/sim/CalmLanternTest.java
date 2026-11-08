package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CalmLanternTest
{
    @Test
    public void calmBreaksAForkWarningAndHoldsForksAndStormsOffForThreeHours()
    {
        NomadSim sim = NomadSim.start(77);
        while (sim.forkWarning() == 0) sim.step(null);
        sim.boons.add(NomadSim.CALM);
        String line = sim.useBoon(0, 0);
        assertNotNull(line);
        assertTrue(line, line.contains("won't"));
        assertEquals(0, sim.forkWarning());
        assertEquals(NomadSim.CALM_MINUTES, sim.calmLeft());
        for (int m = 0; m < NomadSim.CALM_MINUTES - 1; m++)
        {
            sim.step(null);
            assertEquals(0, sim.forkWarning());
            assertEquals(0, sim.stormWarning());
            assertEquals(0, sim.stormLeft());
        }
        sim.step(null);
        assertEquals(0, sim.calmLeft());
    }

    @Test
    public void calmEndsASquallAndLetsMainGrowBack()
    {
        for (int seed = 1; seed <= 60; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            while (sim.minutes() < 2000 && sim.stormLeft() == 0) sim.step(null);
            if (sim.stormKind() != NomadSim.SQUALL) continue;
            int storms = sim.storms;
            sim.boons.add(NomadSim.CALM);
            assertTrue(sim.useBoon(0, 0).contains("breaks off"));
            assertEquals(0, sim.stormLeft());
            assertEquals(0, sim.stormKind());
            assertEquals(storms + 1, sim.storms);
            assertEquals(Math.min(sim.main().radiusMax(), Math.max(sim.main().radiusMin(), sim.squallFrom)), sim.main().radiusTarget, 1e-9);
            return;
        }
        throw new AssertionError("no squall in 60 seeds");
    }

    @Test
    public void aLanternNeedsToBeLitInsideYourCircle()
    {
        NomadSim sim = NomadSim.start(1);
        sim.boons.add(NomadSim.LANTERN);
        assertNull(sim.useBoon(0, 0));
        Circle m = sim.main();
        assertNull(sim.useBoon(0, 0, new Tile((int) (m.x + m.radius + 30), (int) m.y)));
        assertEquals(1, sim.boons().size());
        assertNotNull(sim.useBoon(0, 0, new Tile((int) Math.round(m.x), (int) Math.round(m.y))));
        assertTrue(sim.lanternLit());
        assertTrue(sim.boons().isEmpty());
    }

    @Test
    public void aLitLanternRaisesTheNextWaystoneAtTheLandmarkNearestIt()
    {
        for (int seed = 1; seed <= 20; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            for (int i = 0; i < 30; i++) sim.step(null);
            Circle m = sim.main();
            Landmarks.Landmark nearest = null;
            Tile at = new Tile((int) Math.round(m.x), (int) Math.round(m.y));
            for (Landmarks.Landmark l : Landmarks.ALL)
            {
                if (!m.contains(l.x, l.y)) continue;
                if (nearest == null || Circle.dist(l.x - at.x, l.y - at.y) < Circle.dist(nearest.x - at.x, nearest.y - at.y)) nearest = l;
            }
            if (nearest == null) continue;
            sim.boons.add(NomadSim.LANTERN);
            assertNotNull(sim.useBoon(0, 0, at));
            assertNotNull(sim.summon(NomadSim.COMMON));
            Waystone w = sim.waystones().get(sim.waystones().size() - 1);
            assertEquals(NomadSim.MAIN, w.circle());
            assertTrue(sim.minutesLeft(w) >= NomadSim.LASTING || (w.x() == nearest.x && w.y() == nearest.y));
            assertFalse(sim.lanternLit());
            return;
        }
        throw new AssertionError("no landmark inside Main");
    }
}
