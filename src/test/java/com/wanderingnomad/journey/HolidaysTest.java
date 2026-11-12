package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalDate;
import org.junit.Test;

public class HolidaysTest
{
    @Test
    public void easterFallsOnTheRightSundays()
    {
        assertEquals(LocalDate.of(2024, 3, 31), Holidays.easter(2024));
        assertEquals(LocalDate.of(2025, 4, 20), Holidays.easter(2025));
        assertEquals(LocalDate.of(2026, 4, 5), Holidays.easter(2026));
        assertEquals(LocalDate.of(2027, 3, 28), Holidays.easter(2027));
    }

    @Test
    public void stormsTakeTheSeasonsName()
    {
        assertEquals("Gale", Holidays.storm(LocalDate.of(2026, 10, 19), true));
        assertEquals("Haunt gale", Holidays.storm(LocalDate.of(2026, 10, 20), true));
        assertEquals("Haunt squall", Holidays.storm(LocalDate.of(2026, 11, 2), false));
        assertNull(Holidays.season(LocalDate.of(2026, 11, 3)));
        assertEquals("Snowfall squall", Holidays.storm(LocalDate.of(2026, 12, 25), false));
        assertNull(Holidays.season(LocalDate.of(2027, 1, 1)));
    }

    @Test
    public void easterPaysOneStoneEachDayOfTheWeekend()
    {
        Journey j = new Journey().ready();
        assertFalse(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 2)));
        assertTrue(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 3)));
        assertFalse(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 3)));
        assertTrue(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 5)));
        assertTrue(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 6)));
        assertFalse(Holidays.easterStoneDue(j, LocalDate.of(2026, 4, 7)));
    }
}
