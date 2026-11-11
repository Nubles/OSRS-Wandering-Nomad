package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.wanderingnomad.sim.NomadSim;
import java.util.List;
import org.junit.Test;

public class RoadStonesTest
{
    @Test
    public void everyTwentyFifthRegionSummonsOneCommonStone()
    {
        Journey j = new Journey().ready();
        assertTrue(RoadStones.due(j).isEmpty());
        for (int i = 0; i < 24; i++) j.addTo(Trials.SQUARES, "r" + i);
        assertTrue(RoadStones.due(j).isEmpty());
        j.addTo(Trials.SQUARES, "r24");
        List<RoadStones.Due> due = RoadStones.due(j);
        assertEquals(1, due.size());
        assertEquals(NomadSim.COMMON, due.get(0).tier);
        assertTrue(RoadStones.due(j).isEmpty());
        assertEquals(25, RoadStones.squaresToGo(j));
    }

    @Test
    public void everyFiftiethSlotSummonsAndTheTwoHundredFiftiethIsRare()
    {
        Journey j = new Journey().ready();
        RoadStones.due(j);
        int rare = 0;
        int common = 0;
        for (int i = 1; i <= 250; i++)
        {
            j.addTo(Trials.COLLECTION, "item " + i);
            for (RoadStones.Due d : RoadStones.due(j))
            {
                if (NomadSim.RARE.equals(d.tier)) rare++;
                else common++;
            }
        }
        assertEquals(4, common);
        assertEquals(1, rare);
    }

    @Test
    public void eightHoursInsideInARowSummonsARareStone()
    {
        Journey j = new Journey().ready();
        int rare = 0;
        for (int m = 0; m < RoadStones.ROAD_MINUTES * 2; m++)
        {
            j.minute(true);
            for (RoadStones.Due d : RoadStones.due(j)) if (NomadSim.RARE.equals(d.tier)) rare++;
        }
        assertEquals(2, rare);
        j.minute(false);
        assertEquals(RoadStones.ROAD_MINUTES, RoadStones.roadToGo(j));
    }

    @Test
    public void aRunSavedBeforeTheStonesStartsFromWhereItStands()
    {
        Journey j = new Journey().ready();
        for (int i = 0; i < 60; i++) j.addTo(Trials.SQUARES, "r" + i);
        assertTrue(RoadStones.due(j).isEmpty());
        for (int i = 60; i < 75; i++) j.addTo(Trials.SQUARES, "r" + i);
        assertEquals(1, RoadStones.due(j).size());
    }
}
