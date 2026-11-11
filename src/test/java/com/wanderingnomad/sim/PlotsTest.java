package com.wanderingnomad.sim;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlotsTest
{
    @Test
    public void aPlantedPatchHoldsATinyCircleUntilItsCleared()
    {
        Plots p = new Plots();
        p.plant(new Tile(3229, 3459));
        assertTrue(p.contains(3231, 3461));
        assertFalse(p.contains(3240, 3459));
        // Planting the next patch over refreshes the same plot.
        p.plant(new Tile(3230, 3459));
        assertEquals(1, p.size());
        assertTrue(p.clear(new Tile(3228, 3458)));
        assertFalse(p.contains(3229, 3459));
        assertNull(p.save());
    }

    @Test
    public void plotsRunOutAndTheOldestMakesRoom()
    {
        Plots p = new Plots();
        for (int i = 0; i < Plots.MAX; i++)
        {
            p.plant(new Tile(3000 + i * 20, 3200));
            p.minute();
        }
        p.plant(new Tile(2800, 3200));
        assertEquals(Plots.MAX, p.size());
        assertFalse(p.contains(3000, 3200));
        assertTrue(p.contains(2800, 3200));
        int gone = 0;
        for (int m = 0; m < Plots.MINUTES; m++) gone += p.minute();
        assertEquals(Plots.MAX, gone);
        assertEquals(0, p.size());
    }

    @Test
    public void survivesASave()
    {
        Plots p = new Plots();
        p.plant(new Tile(3229, 3459));
        p.minute();
        int[] saved = p.save();
        assertArrayEquals(new int[]{3229, 3459, Plots.MINUTES - 1}, saved);
        Plots back = new Plots(saved);
        assertTrue(back.contains(3229, 3459));
    }
}
