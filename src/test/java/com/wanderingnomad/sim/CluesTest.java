package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class CluesTest
{
    @Test
    public void readsACoordinateClue()
    {
        // The Observatory's middle.
        Tile t = Clues.digSpot("00 degrees 00 minutes north 00 degrees 00 minutes east");
        assertEquals(2440, t.x);
        assertEquals(3161, t.y);
        // One degree and 30 minutes south and west: 32 + 16 tiles each way.
        t = Clues.digSpot("01 degree 30 minutes south\n01 degree 30 minutes west");
        assertEquals(2440 - 48, t.x);
        assertEquals(3161 - 48, t.y);
        // As the clue scroll's widget holds it, with line breaks as tags.
        t = Clues.digSpot("00 degrees 05 minutes north<br>01 degree 13 minutes east");
        assertEquals(2440 + 32 + 7, t.x);
        assertEquals(3161 + 3, t.y);
    }

    @Test
    public void ignoresOtherClues()
    {
        assertNull(Clues.digSpot("Dig near some giant mushrooms, behind the Grand Tree."));
        assertNull(Clues.digSpot(null));
    }
}
