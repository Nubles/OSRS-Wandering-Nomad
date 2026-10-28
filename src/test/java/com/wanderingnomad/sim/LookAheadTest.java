package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class LookAheadTest
{
    private final Gson gson = new Gson();

    @Test
    public void aCopyPlaysTheSameFutureWithoutTouchingTheRun()
    {
        NomadSim sim = NomadSim.start(482913);
        for (int i = 0; i < 300; i++) sim.step(null);
        String before = gson.toJson(sim);
        NomadSim copy = sim.copy();
        assertEquals(before, gson.toJson(copy));
        for (int i = 0; i < 300; i++) assertEquals(sim.copy().step(null), copy.copy().step(null));
        for (int i = 0; i < 300; i++) copy.step(null);
        assertEquals(before, gson.toJson(sim));
    }

    @Test
    public void theForecastNeverSpoilsOrChangesTheRun()
    {
        NomadSim sim = NomadSim.start(7);
        for (int i = 0; i < 120; i++) sim.step(null);
        String before = gson.toJson(sim);
        Forecast f = Forecast.of(sim, 8);
        assertEquals(before, gson.toJson(sim));
        // The same moment always gives the same cone.
        Forecast g = Forecast.of(sim, 8);
        for (int h = 0; h < Forecast.HORIZONS.length; h++)
        {
            for (int c = 0; c < sim.circles().size(); c++)
            {
                assertEquals(gson.toJson(f.points(h, c)), gson.toJson(g.points(h, c)));
            }
        }
        // Main can only have drifted so far in six hours (sailing aside, 3 tiles a minute).
        double[] p = f.points(2, NomadSim.MAIN);
        for (int i = 0; i < p.length; i += 2)
        {
            assertTrue(Circle.dist(p[i] - sim.main().x(), p[i + 1] - sim.main().y()) < 360 * 3);
        }
    }

    @Test
    public void theRadarSeesTheEdgeComing()
    {
        NomadSim sim = NomadSim.start(482913);
        Circle main = sim.main();
        // Stand right in the middle: the edge is far away.
        Radar mid = Radar.of(sim, new Tile((int) Math.round(main.x()), (int) Math.round(main.y())));
        assertEquals(-1, mid.edgeIn);
        // Find a tile just inside the edge that the next 15 minutes leave behind, if any lies ahead.
        boolean found = false;
        for (int a = 0; a < 64 && !found; a++)
        {
            double ang = a * Math.PI * 2 / 64;
            int x = (int) Math.round(main.x() + Math.cos(ang) * (main.radius() - 0.6));
            int y = (int) Math.round(main.y() + Math.sin(ang) * (main.radius() - 0.6));
            if (!main.contains(x, y)) continue;
            Radar r = Radar.of(sim, new Tile(x, y));
            if (r.edgeIn > 0)
            {
                found = true;
                NomadSim copy = sim.copy();
                for (int m = 1; m < r.edgeIn; m++) copy.step(new Tile(x, y));
                assertTrue(copy.bound().contains(x, y));
                copy.step(new Tile(x, y));
                assertTrue(!copy.bound().contains(x, y));
            }
        }
        assertTrue("some edge tile should fall behind within 15 minutes", found);
    }
}
