package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

/** Comets, Reunion, Castaway, Homeward, ripening, pilgrim and legendary stones, and the legendary boons. */
public class WaveOneTest
{
    @Test
    public void aCometComesWithinAFewDaysOfPlayAndCrossesMain()
    {
        for (int seed = 1; seed <= 12; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            // A comet waits while Main is at sea, so allow for a voyage or two.
            while (sim.minutes() < NomadSim.COMET_EVERY_MAX + 1200 && sim.comet() == null) sim.step(null);
            Comet k = sim.comet();
            assertNotNull("seed " + seed + " saw a comet", k);
            assertTrue(sim.minutes() >= NomadSim.COMET_EVERY_MIN);
            // Its line passes through Main's middle as announced.
            Circle m = sim.main();
            double d = NomadSim.segmentDist(m.x, m.y, k.x(), k.y(), k.endX(), k.endY());
            assertTrue(d < 1);
        }
    }

    @Test
    public void standingInTheCometsPathCatchesIt()
    {
        NomadSim sim = NomadSim.start(5);
        sim.cometSoonForTesting();
        while (sim.comet() == null) sim.step(null);
        Comet k = sim.comet();
        Tile spot = new Tile((int) Math.round(k.x() + k.dx * 15), (int) Math.round(k.y() + k.dy * 15));
        boolean caught = false;
        for (int i = 0; i < 60 && !caught; i++)
        {
            for (String line : sim.step(spot)) caught |= line.startsWith("You caught the comet");
        }
        assertTrue(caught);
        assertNull(sim.comet());
        assertEquals(1, sim.comets());
    }

    @Test
    public void aForksLostHalfDriftsOnAndSometimesComesBack()
    {
        int reunions = 0;
        for (int seed = 1; seed <= 40; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            boolean ghostSeen = false;
            while (sim.minutes() < 3000)
            {
                for (String line : sim.step(null)) if (line.startsWith("Reunion!")) reunions++;
                if (sim.ghost() != null)
                {
                    ghostSeen = true;
                    assertTrue(sim.ghostLeft() <= NomadSim.GHOST_MINUTES);
                    assertTrue(Geography.onLand(sim.ghost().x(), sim.ghost().y()));
                }
            }
            assertTrue("seed " + seed + " forked", ghostSeen);
        }
        assertTrue("some lost halves came back", reunions > 0);
    }

    @Test
    public void aCastawayWashesUpAtADockMainCanSailFrom()
    {
        int wrecks = 0;
        for (int seed = 1; seed <= 120 && wrecks < 3; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            sim.setSailingUnlocked(true);
            while (sim.minutes() < 4000)
            {
                for (String line : sim.step(null))
                {
                    if (!line.startsWith("Castaway!")) continue;
                    wrecks++;
                    String to = sim.main().voyageTo();
                    assertNotNull(to);
                    assertTrue(!Geography.routesFrom(to, 99, java.util.Collections.<String>emptyList()).isEmpty());
                }
            }
        }
        assertTrue("some voyages ended up castaway", wrecks > 0);
    }

    @Test
    public void mainSetsCourseForPortsAndSailsAboutOnceADay()
    {
        int voyages = 0;
        int minutes = 0;
        for (int seed = 1; seed <= 20; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            int docked = -1;
            while (sim.minutes() < 3000)
            {
                for (String line : sim.step(null))
                {
                    if (line.startsWith("Your circle docked")) docked = sim.minutes();
                    if (!line.startsWith("The Main circle boarded")) continue;
                    voyages++;
                    // Ashore for a while after docking before it sails again.
                    assertTrue(docked < 0 || sim.minutes() - docked >= NomadSim.SHORE_LEAVE);
                }
            }
            minutes += sim.minutes();
        }
        double hoursEach = minutes / 60.0 / voyages;
        assertTrue("a voyage every " + hoursEach + " hours", hoursEach > 8 && hoursEach < 48);
    }

    @Test
    public void bothTwinsFollowTheSameWalkToAPort()
    {
        // From Lumbridge the walk to Port Sarim goes round the river, never through water.
        Geography.Port sarim = Geography.port("Port Sarim");
        double x = Geography.LUMBRIDGE.x;
        double y = Geography.LUMBRIDGE.y;
        assertTrue(Geography.harbourSteps(sarim, x, y) > 0);
        for (int i = 0; i < 400 && Geography.harbourSteps(sarim, x, y) > 0; i++)
        {
            double[] way = Geography.wayTo(sarim, x, y, NomadSim.PORT_LOOKAHEAD);
            assertNotNull(way);
            assertTrue(Geography.onLand(way[0], way[1]));
            x = way[0];
            y = way[1];
        }
        assertEquals(0, Geography.harbourSteps(sarim, x, y));
        assertEquals(-1, Geography.harbourSteps(Geography.port("Waterbirth Island"), Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y));
    }

    @Test
    public void homewardCarriesMainOnceThenCoolsDown()
    {
        NomadSim sim = NomadSim.start(3);
        for (int i = 0; i < 30; i++) sim.step(null);
        Circle m = sim.main();
        Tile from = new Tile((int) Math.round(m.x), (int) Math.round(m.y));
        Tile lumbridge = Geography.LUMBRIDGE;
        Tile there = new Tile(lumbridge.x + 200, lumbridge.y + 300);
        if (!Geography.onLand(there.x, there.y)) there = new Tile(lumbridge.x - 30, lumbridge.y + 120);
        String line = sim.homeward(from, there);
        assertNotNull(line);
        assertEquals(there.x, sim.main().x(), 0.001);
        assertEquals(12, sim.main().radius(), 0.001);
        assertEquals(NomadSim.HOMEWARD_COOLDOWN, sim.homewardLeft());
        // Again at once: it stays put.
        assertTrue(sim.homeward(there, lumbridge).startsWith("Homeward is still"));
        assertEquals(there.x, sim.main().x(), 0.001);
        // From outside Main, nothing happens.
        for (int i = 0; i < NomadSim.HOMEWARD_COOLDOWN; i++) sim.step(null);
        assertEquals(0, sim.homewardLeft());
        assertNull(sim.homeward(new Tile(there.x + 500, there.y), lumbridge));
    }

    @Test
    public void aCommonStoneRipensAfterHalfAnHour()
    {
        for (int seed = 1; seed <= 20; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            sim.summon(NomadSim.COMMON);
            if (sim.waystones().isEmpty()) continue;
            Waystone w = sim.waystones().get(0);
            assertEquals(NomadSim.RIPEN, w.ripensIn());
            for (int i = 0; i < NomadSim.RIPEN && !sim.waystones().isEmpty(); i++) sim.step(null);
            if (sim.waystones().isEmpty()) continue;
            assertEquals(NomadSim.RARE, sim.waystones().get(0).tier());
            return;
        }
        throw new AssertionError("no stone lasted long enough to ripen");
    }

    @Test
    public void pilgrimStonesRiseOutsideAndCanBeClaimedFromThere()
    {
        for (int seed = 1; seed <= 60; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            // Away from Lumbridge first, where no landmark lies in the ring just outside Main.
            for (int i = 0; i < 600; i++) sim.step(null);
            if (sim.main().voyageTo() != null) continue;
            for (int k = 0; k < 6; k++)
            {
                sim.summon(NomadSim.RARE);
                Waystone p = null;
                for (Waystone w : sim.waystones()) if (w.pilgrim()) p = w;
                if (p == null) continue;
                assertTrue(!sim.main().contains(p.x(), p.y()));
                Tile on = new Tile(p.x(), p.y());
                sim.step(on);
                assertTrue(sim.boons().contains(p.boon()));
                return;
            }
        }
        throw new AssertionError("no pilgrim stone rose");
    }

    @Test
    public void legendaryStonesNeverFadeAndTheCrownFollowsYou()
    {
        NomadSim sim = NomadSim.start(11);
        sim.summon(NomadSim.LEGENDARY);
        assertEquals(1, sim.waystones().size());
        Waystone w = sim.waystones().get(0);
        assertTrue(w.legendary());
        for (int i = 0; i < 600; i++) sim.step(null);
        assertEquals(1, sim.waystones().size());

        sim.boons.add(NomadSim.CROWN);
        Circle m = sim.main();
        Tile me = new Tile((int) Math.round(m.x) + 3, (int) Math.round(m.y));
        assertNotNull(sim.useBoon(sim.boons().size() - 1, 0, me));
        double x0 = sim.main().x();
        Tile walk = new Tile(me.x + 30, me.y);
        if (!Geography.onLand(walk.x, walk.y)) walk = new Tile(me.x - 30, me.y);
        for (int i = 0; i < 5; i++) sim.step(walk);
        assertTrue(Math.abs(sim.main().x() - walk.x) < Math.abs(x0 - walk.x));
    }

    @Test
    public void helmsmanSendsMainToThePortYouChose()
    {
        NomadSim sim = NomadSim.start(314);
        sim.setSailingUnlocked(true);
        sim.boons.add(NomadSim.HELMSMAN);
        List<String> ports = sim.helmsmanPorts();
        assertTrue(ports.size() > 3);
        String pick = ports.get(ports.size() - 1);
        assertNull(sim.useHelmsman(0, "Nowhere"));
        assertNotNull(sim.useHelmsman(0, pick));
        assertEquals(pick, sim.helmsmanTo());
        for (int i = 0; i < 20000 && sim.helmsmanTo() != null; i++) sim.step(null);
        assertNull(sim.helmsmanTo());
        assertEquals(pick, sim.main().voyageTo());
    }
}
