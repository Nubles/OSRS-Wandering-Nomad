package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import org.junit.Test;

public class BestiaryTest
{
    private static Bestiary.Result kills(Journey j, String monster, int level, int n, int minute)
    {
        Bestiary.Result last = null;
        for (int i = 0; i < n; i++)
        {
            Bestiary.Result r = Bestiary.kill(j, monster, level, true, minute);
            if (r != null) last = r;
        }
        return last;
    }

    @Test
    public void milestonesPayRenownWaystonesAndATitle()
    {
        Journey j = new Journey().ready();
        Bestiary.Result r = kills(j, "Goblin", 5, 10, 100);
        assertEquals(10, r.count);
        assertEquals(5, r.renown);
        assertNull(r.waystone);
        r = kills(j, "Goblin", 5, 90, 100);
        assertEquals(100, r.count);
        assertEquals("common", r.waystone);
        r = kills(j, "Goblin", 5, 900, 400);
        assertEquals("rare", r.waystone);
        r = kills(j, "Goblin", 5, 9000, 900);
        assertEquals(10_000, r.count);
        assertEquals("Goblin Bane", r.title);
        assertNull(r.waystone);
        assertEquals(10_000, Bestiary.count(j, "Goblin"));
    }

    @Test
    public void killsOutsideDontCount()
    {
        Journey j = new Journey().ready();
        for (int i = 0; i < 20; i++) assertNull(Bestiary.kill(j, "Cow", 2, false, 5));
        assertEquals(0, Bestiary.count(j, "Cow"));
    }

    @Test
    public void levelOneMonstersPayRenownButNoWaystones()
    {
        Journey j = new Journey().ready();
        Bestiary.Result r = kills(j, "Chicken", 1, 100, 50);
        assertEquals(100, r.count);
        assertNull(r.waystone);
        assertFalse(r.waiting);
        assertEquals(15, r.renown);
    }

    @Test
    public void waystonesWaitForTheHourlyCapAndAreNeverLost()
    {
        Journey j = new Journey().ready();
        assertEquals("common", kills(j, "Goblin", 5, 100, 100).waystone);
        Bestiary.Result r = kills(j, "Cow", 2, 100, 120);
        assertNull(r.waystone);
        assertTrue(r.waiting);
        assertNull(Bestiary.due(j, 159));
        assertEquals("common", Bestiary.due(j, 160));
        assertNull(Bestiary.due(j, 161));
        assertEquals(1, Bestiary.reached(j, 100) - 1);
    }

    @Test
    public void survivesASaveAndAnOldSave()
    {
        Gson gson = new Gson();
        Journey j = new Journey().ready();
        kills(j, "Goblin", 5, 100, 100);
        kills(j, "Cow", 2, 100, 110);
        Journey back = gson.fromJson(gson.toJson(j), Journey.class).ready();
        assertEquals(100, Bestiary.count(back, "Cow"));
        assertEquals("common", Bestiary.due(back, 200));
        Journey old = gson.fromJson("{\"values\":{}}", Journey.class).ready();
        assertNull(Bestiary.due(old, 500));
        Renown r = gson.fromJson("{\"renown\":5}", Renown.class).ready();
        assertTrue(r.earnTitle("Goblin Bane"));
        assertFalse(r.earnTitle("Goblin Bane"));
    }
}
