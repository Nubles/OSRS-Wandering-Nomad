package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StormTest
{
    @Test
    public void aStormBrewsWithinTwelveHoursAndBothKindsTurnUp()
    {
        boolean gale = false;
        boolean squall = false;
        for (int seed = 1; seed <= 30; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            int brewed = -1;
            for (int m = 0; m < NomadSim.STORM_EVERY_MAX + 4 * 60 && brewed < 0; m++)
            {
                sim.step(null);
                if (sim.stormWarning() == NomadSim.STORM_WARN) brewed = sim.minutes();
            }
            assertTrue("seed " + seed + " brewed a storm", brewed >= NomadSim.STORM_EVERY_MIN);
            gale |= sim.stormKind() == NomadSim.GALE;
            squall |= sim.stormKind() == NomadSim.SQUALL;
        }
        assertTrue(gale && squall);
    }

    @Test
    public void aSquallShrinksMainThenLetsItGrowBack()
    {
        for (int seed = 1; seed <= 60; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            while (sim.minutes() < 2000 && sim.stormLeft() == 0) sim.step(null);
            if (sim.stormKind() != NomadSim.SQUALL) continue;
            double before = sim.squallFrom;
            assertEquals(Math.max(sim.main().radiusMin(), before * NomadSim.SQUALL_SHRINK), sim.main().radius(), 3);
            while (sim.stormLeft() > 0) sim.step(null);
            assertEquals(0, sim.stormKind());
            assertTrue(sim.main().radiusTarget >= sim.main().radius() - 1e-9);
            return;
        }
        throw new AssertionError("no squall in 60 seeds");
    }

    @Test
    public void noForkStartsDuringAStorm()
    {
        for (int seed = 1; seed <= 40; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            for (int m = 0; m < 1800; m++)
            {
                boolean storming = sim.stormLeft() > 0 || sim.stormWarning() > 0;
                boolean calm = sim.forkWarning() == 0 && sim.twin() == null;
                sim.step(null);
                if (storming && calm && sim.stormLeft() > 0) assertEquals("seed " + seed, 0, sim.forkWarning());
            }
        }
    }
}
