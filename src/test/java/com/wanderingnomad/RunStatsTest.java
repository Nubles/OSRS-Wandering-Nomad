package com.wanderingnomad;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RunStatsTest
{
    @Test
    public void walkingCountsTilesButNotTeleports()
    {
        RunStats stats = new RunStats();
        stats.walked(1, 0);
        stats.walked(2, 1);
        stats.walked(0, 0);
        stats.walked(40, -12);
        assertEquals(3, stats.tilesWalked);
    }

    @Test
    public void momentsAreTallied()
    {
        RunStats stats = new RunStats();
        stats.count(Moments.Event.CROSSED);
        stats.count(Moments.Event.CLAIMED);
        stats.count(Moments.Event.CLAIMED);
        stats.count(Moments.Event.LEFT);
        stats.count(Moments.Event.OTHER);
        assertEquals(1, stats.crossings);
        assertEquals(2, stats.claimed);
        assertEquals(1, stats.timesOutside);
    }
}
