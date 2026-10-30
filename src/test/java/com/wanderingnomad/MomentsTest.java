package com.wanderingnomad;

import static org.junit.Assert.assertEquals;
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
        assertTrue(new Moments().observe(sim, false, null).isEmpty());
    }

    @Test
    public void steppingOutAndBackInIsToldOnceEach()
    {
        NomadSim sim = NomadSim.start(1);
        Moments moments = new Moments();
        moments.observe(sim, false, null);

        List<Moments.Banner> out = moments.observe(sim, true, null);
        assertEquals(1, out.size());
        assertEquals("Outside your circle", out.get(0).title);
        assertTrue(moments.observe(sim, true, null).isEmpty());

        List<Moments.Banner> back = moments.observe(sim, false, null);
        assertEquals(1, back.size());
        assertEquals("Back inside", back.get(0).title);
    }

    @Test
    public void resetForgetsTheLastLook()
    {
        NomadSim sim = NomadSim.start(1);
        Moments moments = new Moments();
        moments.observe(sim, false, null);
        moments.reset();
        assertTrue(moments.observe(sim, true, null).isEmpty());
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
