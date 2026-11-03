package com.wanderingnomad.sim;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public class PortsTest
{
    @Test
    public void beforeSailingOnlyTheQuestFreeBoats()
    {
        List<String> from = Geography.routesFrom("Rimmington", 0, Collections.<String>emptyList());
        assertEquals(Arrays.asList("East Ardougne", "Brimhaven"), from);
        assertFalse("Captain Tock needs The Corsair Curse", from.contains("Corsair Cove"));
    }

    @Test
    public void sailingReachesOnlyTheDocksYourLevelAndQuestsAllow()
    {
        List<String> missing = Arrays.asList(Geography.CORSAIR_CURSE, Geography.FREMENNIK_TRIALS);
        List<String> low = Geography.routesFrom("Port Sarim", 20, missing);
        assertTrue(low.contains("Catherby"));
        assertTrue(low.contains("Land's End"));
        assertTrue("the Void Knights' squire is quest-free whatever your level", low.contains("Void Knights' Outpost"));
        assertFalse(Geography.routesFrom("Rimmington", 20, missing).contains("Port Khazard"));
        List<String> high = Geography.routesFrom("Port Sarim", 99, missing);
        assertFalse(high.contains("Rellekka"));
        assertFalse(high.contains("Waterbirth Island"));
        assertTrue(Geography.routesFrom("Port Sarim", 99, Collections.<String>emptyList()).contains("Rellekka"));
    }

    @Test
    public void anUnknownLevelKeepsTheOldEveryPortRule()
    {
        List<String> all = Geography.routesFrom("Port Sarim", true);
        assertEquals(Geography.PORTS.size() - 1, all.size());
    }
}
