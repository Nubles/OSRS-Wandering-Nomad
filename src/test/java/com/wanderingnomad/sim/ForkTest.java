package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class ForkTest
{
    /** Plays until a log line containing text, and returns the minute it came on (or -1). */
    private static int playUntil(NomadSim sim, String text, int limit)
    {
        for (int i = 0; i < limit; i++)
        {
            List<String> log = sim.step(null);
            for (String line : log)
            {
                if (line.contains(text)) return sim.minutes();
            }
        }
        return -1;
    }

    @Test
    public void aForkComesWithinFourHoursAndWarnsTenMinutesAhead()
    {
        NomadSim sim = NomadSim.start(7);
        int warned = playUntil(sim, "about to fork", 600);
        assertTrue("warned at " + warned, warned >= NomadSim.FORK_EVERY_MIN && warned <= NomadSim.FORK_EVERY_MAX);
        assertEquals(NomadSim.FORK_WARN, sim.forkWarning());
        assertNull(sim.twin());
        int forked = playUntil(sim, "forked!", 60);
        assertEquals(warned + NomadSim.FORK_WARN, forked);
        assertNotNull(sim.twin());
        assertEquals(0, sim.quietMinutes());
    }

    @Test
    public void theHalfYouStandInLivesOn()
    {
        for (int seed : new int[]{7, 314, 2024})
        {
            NomadSim sim = NomadSim.start(seed);
            assertTrue(playUntil(sim, "forked!", 900) > 0);
            Circle twin = sim.twin();
            String settled = null;
            for (int i = 0; i < 200 && settled == null; i++)
            {
                // Follow the twin's centre: both halves count as yours while they part.
                Tile you = new Tile((int) Math.round(twin.x), (int) Math.round(twin.y));
                assertTrue(sim.inYours(you.x, you.y));
                for (String line : sim.step(you))
                {
                    if (line.contains("halves parted")) settled = line;
                }
                assertFalse(sim.isOutside());
            }
            assertNotNull(settled);
            assertTrue(settled, settled.contains("You stood in the " + NomadSim.compass(twin.heading)));
            assertTrue(sim.main() == twin);
            assertNull(sim.twin());
        }
    }

    @Test
    public void inNeitherHalfTheSeedDecidesTheSameWayEveryTime()
    {
        NomadSim a = NomadSim.start(482913);
        NomadSim b = NomadSim.start(482913);
        Tile away = new Tile(2000, 2000);
        for (int i = 0; i < 1500; i++)
        {
            assertEquals(a.step(away), b.step(away));
        }
        assertEquals(a.main().x, b.main().x, 0);
        assertEquals(a.main().y, b.main().y, 0);
    }

    @Test
    public void compassWords()
    {
        assertEquals("east", NomadSim.compass(0));
        assertEquals("north", NomadSim.compass(Math.PI / 2));
        assertEquals("west", NomadSim.compass(Math.PI));
        assertEquals("south", NomadSim.compass(-Math.PI / 2));
        assertEquals("north-west", NomadSim.compass(3 * Math.PI / 4 + 0.1));
    }
}
