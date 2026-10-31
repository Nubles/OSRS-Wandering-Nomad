package com.wanderingnomad;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.wanderingnomad.sim.NomadSim;
import java.util.List;
import org.junit.Test;

public class MomentsTest
{
    @Test
    public void firstLookOnlyTakesStock()
    {
        NomadSim sim = NomadSim.start(1);
        assertTrue(new Moments().observe(sim, false, null, 1000).isEmpty());
    }

    @Test
    public void steppingOutCountsOnlyOnceYouStayOut()
    {
        NomadSim sim = NomadSim.start(1);
        Moments moments = new Moments();
        moments.observe(sim, false, null, 0);

        // A step out and straight back in is nothing.
        assertTrue(moments.observe(sim, true, null, 600).isEmpty());
        assertTrue(moments.observe(sim, false, null, 1200).isEmpty());

        assertTrue(moments.observe(sim, true, null, 2000).isEmpty());
        List<Moments.Banner> out = moments.observe(sim, true, null, 2000 + Moments.SETTLE_MS);
        assertEquals(1, out.size());
        assertEquals("Outside your circle", out.get(0).title);
        assertFalse(out.get(0).quiet);
        assertTrue(moments.observe(sim, true, null, 5000).isEmpty());
    }

    @Test
    public void quickRepeatsAreQuiet()
    {
        NomadSim sim = NomadSim.start(1);
        Moments moments = new Moments();
        long t = 0;
        moments.observe(sim, false, null, t);
        moments.observe(sim, true, null, t += 100);
        Moments.Banner out = moments.observe(sim, true, null, t += Moments.SETTLE_MS).get(0);
        moments.observe(sim, false, null, t += 100);
        Moments.Banner back = moments.observe(sim, false, null, t += Moments.SETTLE_MS).get(0);
        assertFalse(out.quiet);
        assertTrue(back.quiet);
        assertEquals("Back inside", back.title);

        // A minute later, it's told out loud again.
        moments.observe(sim, true, null, t += Moments.QUIET_MS);
        assertFalse(moments.observe(sim, true, null, t + Moments.SETTLE_MS).get(0).quiet);
    }

    @Test
    public void resetForgetsTheLastLook()
    {
        NomadSim sim = NomadSim.start(1);
        Moments moments = new Moments();
        moments.observe(sim, false, null, 0);
        moments.reset();
        assertTrue(moments.observe(sim, true, null, 5000).isEmpty());
        assertTrue(moments.observe(sim, true, null, 9000).isEmpty());
    }

    @Test
    public void everySampleHasWords()
    {
        for (Moments.Banner b : Moments.samples(NomadSim.start(1)))
        {
            assertTrue(!b.title.isEmpty() && !b.subtitle.isEmpty());
        }
    }
}
