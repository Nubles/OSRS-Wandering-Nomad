package com.wanderingnomad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import org.junit.Test;

public class SandboxTest
{
    private final Gson gson = new Gson();

    @Test
    public void testingInTheSandboxNeverTouchesTheRealRun()
    {
        NomadSim real = NomadSim.start(483574);
        Tile at = new Tile(3222, 3218);
        for (int m = 0; m < 10; m++) real.step(at);
        String before = gson.toJson(real);
        RunStats stats = new RunStats();
        stats.tilesWalked = 42;
        RuleGuard guard = new RuleGuard();
        Milestones milestones = new Milestones();

        Sandbox box = new Sandbox(gson);
        box.enter(real, stats, guard, milestones);
        assertTrue(box.active());
        assertNotSame(real, box.sim);
        assertNotSame(stats, box.stats);
        assertEquals(42, box.stats.tilesWalked);

        // Everything the Testing buttons do lands on the copy.
        box.sim.forkSoonForTesting(false);
        for (int m = 0; m < 45; m++) box.sim.step(at);
        box.stats.tilesWalked = 999;
        assertEquals(before, gson.toJson(real));
        assertEquals(42, stats.tilesWalked);

        box.exit();
        assertFalse(box.active());
        assertSame(real, box.sim);
        assertSame(stats, box.stats);
        assertSame(guard, box.guard);
        assertSame(milestones, box.milestones);
        assertEquals(before, gson.toJson(real));
    }

    @Test
    public void enteringTwiceKeepsTheFirstRealRun()
    {
        NomadSim real = NomadSim.start(1);
        Sandbox box = new Sandbox(gson);
        box.enter(real, new RunStats(), new RuleGuard(), new Milestones());
        NomadSim copy = box.sim;
        box.enter(copy, new RunStats(), new RuleGuard(), new Milestones());
        assertSame(real, box.real());
        box.exit();
        assertSame(real, box.sim);
    }
}
