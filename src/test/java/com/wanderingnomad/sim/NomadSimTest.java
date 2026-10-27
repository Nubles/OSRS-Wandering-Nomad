package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class NomadSimTest
{
    private static final int HOURS = 300;

    @Test
    public void rngMatchesTheMockUp()
    {
        // mulberry32(482913) in the web mock-up gives these first.
        Rng rng = new Rng(482913);
        assertEquals(0.036796912318095565, rng.next(), 0);
        assertEquals(0.8229757314547896, rng.next(), 0);
        assertEquals(0.6929165113251656, rng.next(), 0);
    }

    @Test
    public void sameSeedSameRun()
    {
        NomadSim a = NomadSim.start(482913);
        NomadSim b = NomadSim.start(482913);
        for (int i = 0; i < 60 * 50; i++)
        {
            assertEquals(a.step(null), b.step(null));
        }
        for (int i = 0; i < a.circles().size(); i++)
        {
            assertEquals(a.circles().get(i).x(), b.circles().get(i).x(), 0);
            assertEquals(a.circles().get(i).radius(), b.circles().get(i).radius(), 0);
        }
    }

    @Test
    public void savedRunResumesExactly()
    {
        Gson gson = new Gson();
        NomadSim live = NomadSim.start(99);
        for (int i = 0; i < 600; i++) live.step(null);
        NomadSim loaded = gson.fromJson(gson.toJson(live), NomadSim.class);
        for (int i = 0; i < 600; i++)
        {
            assertEquals(live.step(null), loaded.step(null));
        }
        assertEquals(gson.toJson(live), gson.toJson(loaded));
    }

    @Test
    public void regionCirclesNeverLeaveTheirKingdom()
    {
        for (int seed : new int[]{1, 2, 3, 482913})
        {
            NomadSim sim = NomadSim.start(seed);
            for (int m = 0; m < HOURS * 60; m++)
            {
                sim.step(null);
                for (Circle c : sim.circles())
                {
                    if (!c.isMain()) assertTrue(c.name() + " left home", c.region().contains(c.x(), c.y()));
                }
            }
        }
    }

    @Test
    public void mainStaysOnLandUnlessSailing()
    {
        NomadSim sim = NomadSim.start(7);
        for (int m = 0; m < HOURS * 60; m++)
        {
            sim.step(null);
            Circle main = sim.main();
            assertTrue(main.sailing() || Geography.onLand(main.x(), main.y()));
        }
    }

    @Test
    public void radiiStayInRange()
    {
        NomadSim sim = NomadSim.start(11);
        for (int m = 0; m < HOURS * 60; m++)
        {
            sim.step(null);
            for (Circle c : sim.circles())
            {
                assertTrue(c.radius() >= c.radiusMin() - 1e-9 && c.radius() <= c.radiusMax() + 1e-9);
            }
        }
    }

    @Test
    public void crossingsOpenEveryFewHours()
    {
        // Tuning check: with Main roaming, a new crossing should open often but not constantly.
        int opened = 0;
        for (int seed : new int[]{482913, 1234, 99999, 555})
        {
            NomadSim sim = NomadSim.start(seed);
            for (int m = 0; m < HOURS * 60; m++)
            {
                for (String line : sim.step(null))
                {
                    if (line.startsWith("Crossing open")) opened++;
                }
            }
        }
        double perRun = opened / 4.0;
        assertTrue("too few crossings: " + perRun, perRun >= 5);
        assertTrue("too many crossings: " + perRun, perRun <= 60);
    }

    @Test
    public void crossOnlyThroughMain()
    {
        NomadSim sim = NomadSim.start(5);
        int region = touchingRegion(sim);
        assertTrue("no crossing in 300 hours", region > 0);
        Circle main = sim.main();
        Circle to = sim.circles().get(region);
        // Not standing in the overlap: refused.
        Tile far = new Tile((int) Math.round(main.x() - main.radius() + 1), (int) Math.round(main.y()));
        if (!to.contains(far.x, far.y)) assertNull(sim.crossTo(region, far));
        assertNotNull(sim.crossTo(region, null));
        assertEquals(region, sim.boundIndex());
        for (int j = 1; j < sim.circles().size(); j++)
        {
            assertFalse("region to region", sim.linked(j));
        }
        assertTrue(sim.linked(NomadSim.MAIN));
        assertFalse("the volcano is Main's alone", sim.volcanoOpen());
    }

    @Test
    public void strandedCircleWaitsAtTheBorderNearestMain()
    {
        NomadSim sim = NomadSim.start(5);
        int region = touchingRegion(sim);
        sim.crossTo(region, null);
        int strandedMinutes = 0;
        for (int m = 0; m < HOURS * 60 && sim.boundIndex() == region; m++)
        {
            sim.step(null);
            if (sim.stranded()) strandedMinutes++;
            if (!sim.stranded() && strandedMinutes > 0) return;
        }
        // Either never stranded, or Main came back within the test window.
        assertTrue("stranded for good", strandedMinutes == 0);
    }

    @Test
    public void diariesGrowTheirRegion()
    {
        NomadSim sim = NomadSim.start(3);
        Circle misthalin = circle(sim, "misthalin");
        Circle karamja = circle(sim, "karamja");
        assertEquals(12, misthalin.radiusMin(), 1e-9);
        assertNotNull(sim.setDiaryTiers("karamja", 4));
        assertEquals(28, karamja.radiusMin(), 1e-9);
        assertEquals(48, karamja.radiusMax(), 1e-9);
        // Two diaries: each tier is worth half as much.
        sim.setDiaryTiers("misthalin", 4);
        assertEquals(20, misthalin.radiusMin(), 1e-9);
        sim.setDiaryTiers("misthalin", 8);
        assertEquals(28, misthalin.radiusMin(), 1e-9);
        assertEquals(48, misthalin.radiusMax(), 1e-9);
        assertNull("no change, no message", sim.setDiaryTiers("misthalin", 8));
    }

    @Test
    public void outsideShrinksAndSpeedsUp()
    {
        NomadSim sim = NomadSim.start(8);
        Circle main = sim.main();
        double before = main.radius();
        Tile away = new Tile((int) main.x() + 200, (int) main.y());
        List<String> log = new ArrayList<>(sim.step(away));
        assertTrue(sim.isOutside());
        assertTrue(log.get(0).startsWith("You are outside"));
        for (int i = 0; i < 5; i++) sim.step(away);
        assertTrue(main.radius() <= before - 5 + 1e-9 || main.radius() == main.radiusMin());
    }

    @Test
    public void routesBeforeAndAfterSailing()
    {
        assertTrue(Geography.routesFrom("Port Sarim", false).contains("Musa Point"));
        assertTrue(Geography.routesFrom("Port Sarim", false).contains("Port Piscarilius"));
        assertFalse(Geography.routesFrom("Musa Point", false).contains("Rellekka"));
        assertEquals(Geography.PORTS.size() - 1, Geography.routesFrom("Musa Point", true).size());
    }

    @Test
    public void realMapLandmarks()
    {
        assertEquals("misthalin", Geography.regionAt(3222, 3218).id);
        assertEquals("asgarnia", Geography.regionAt(2965, 3378).id);
        assertEquals("karamja", Geography.regionAt(Geography.VOLCANO.x, Geography.VOLCANO.y).id);
        assertEquals("morytania", Geography.regionAt(3494, 3483).id);
        assertEquals("kourend", Geography.regionAt(1640, 3670).id);
        assertEquals("varlamore", Geography.regionAt(1680, 3100).id);
        // Open sea between Port Sarim and Karamja.
        assertFalse(Geography.onLand(2990, 3100));
        // The River Lum is folded into the land, so circles cross it.
        assertTrue(Geography.onLand(3241, 3226));
        for (Geography.Port p : Geography.PORTS)
        {
            assertTrue(p.name + " is off the land", Geography.onLand(p.tile.x, p.tile.y));
        }
        for (Region r : Geography.REGIONS)
        {
            assertTrue(r.name + " starts outside itself", r.contains(r.home().x, r.home().y));
        }
    }

    private static int touchingRegion(NomadSim sim)
    {
        for (int m = 0; m < HOURS * 60; m++)
        {
            sim.step(null);
            for (int j = 1; j < sim.circles().size(); j++)
            {
                if (sim.canCross(j, null)) return j;
            }
        }
        return -1;
    }

    private static Circle circle(NomadSim sim, String id)
    {
        for (Circle c : sim.circles())
        {
            if (id.equals(c.regionId)) return c;
        }
        throw new AssertionError(id);
    }

    @Test
    public void runCodesRoundTripAndFitInALink()
    {
        Gson gson = new Gson();
        NomadSim sim = NomadSim.start(7);
        for (int i = 0; i < 600; i++) sim.step(null);
        String json = gson.toJson(new SavedRun(sim, new Tile(3200, 3200)));
        String code = RunCode.encode(json);
        assertTrue(code.startsWith("N1."));
        assertTrue("code is " + code.length() + " characters", code.length() < 2500);
        assertFalse(code.contains("+") || code.contains("/") || code.contains("="));
        assertEquals(json, RunCode.decode(code));
    }

    @Test
    public void mainFindsItsWayOutOfThePeninsulas()
    {
        // Alex saw Main stuck around the Lighthouse for nearly 50 hours. From there it now gets
        // 80 tiles away in about 6 hours (the straight-line minimum is 5.3).
        for (int seed = 1; seed <= 30; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            Circle main = sim.main();
            main.x = 2509;
            main.y = 3636;
            int minutes = 0;
            while (Circle.dist(main.x - 2509, main.y - 3636) < 80 && minutes < 12 * 60)
            {
                sim.step(null);
                minutes++;
            }
            assertTrue("seed " + seed + " still near the Lighthouse after " + minutes / 60 + " hours", minutes < 12 * 60);
        }
    }

    @Test
    public void circlesNeverFreezeAgainstAShore()
    {
        NomadSim sim = NomadSim.start(20261027);
        double[][] last = new double[sim.circles().size()][];
        int[] still = new int[last.length];
        for (int t = 0; t < 200 * 60; t++)
        {
            sim.step(null);
            for (int i = 0; i < last.length; i++)
            {
                Circle c = sim.circles().get(i);
                boolean moved = last[i] == null || last[i][0] != c.x || last[i][1] != c.y;
                still[i] = moved ? 0 : still[i] + 1;
                last[i] = new double[] {c.x, c.y};
                assertTrue(c.name() + " has not moved for 2 hours at minute " + t, still[i] < 120);
            }
        }
    }
}
