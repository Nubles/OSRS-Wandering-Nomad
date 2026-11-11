package com.wanderingnomad.journey;

import java.time.LocalDate;
import java.time.Month;

/**
 * Holiday weather: the real calendar flavours the storms. Late October's storms are Haunts,
 * December's are Snowfalls, and over the Easter weekend each day you play summons a common
 * waystone. By the date alone, never tied to Jagex's own events.
 */
public final class Holidays
{
    public static final String HAUNT = "Haunt";
    public static final String SNOWFALL = "Snowfall";
    /** Journey value holding the last Easter day (as an epoch day) that paid its waystone. */
    static final String PAID_EASTER = "paid:easter";

    private Holidays()
    {
    }

    /** The season's storm name for a date, or null outside the seasons. */
    public static String season(LocalDate d)
    {
        if (d.getMonth() == Month.DECEMBER) return SNOWFALL;
        if (d.getMonth() == Month.OCTOBER && d.getDayOfMonth() >= 20) return HAUNT;
        if (d.getMonth() == Month.NOVEMBER && d.getDayOfMonth() <= 2) return HAUNT;
        return null;
    }

    /** A storm's name on a date: "Gale" or "Squall", or the season's name with the kind after it, such as "Haunt gale". */
    public static String storm(LocalDate d, boolean gale)
    {
        String kind = gale ? "Gale" : "Squall";
        String season = season(d);
        return season == null ? kind : season + " " + kind.toLowerCase();
    }

    /** Easter Sunday in the Gregorian calendar (the anonymous Gregorian algorithm). */
    public static LocalDate easter(int year)
    {
        int a = year % 19;
        int b = year / 100;
        int c = year % 100;
        int d = b / 4;
        int e = b % 4;
        int f = (b + 8) / 25;
        int g = (b - f + 1) / 3;
        int h = (19 * a + b - d - g + 15) % 30;
        int i = c / 4;
        int k = c % 4;
        int l = (32 + 2 * e + 2 * i - h - k) % 7;
        int m = (a + 11 * h + 22 * l) / 451;
        int month = (h + l - 7 * m + 114) / 31;
        int day = (h + l - 7 * m + 114) % 31 + 1;
        return LocalDate.of(year, month, day);
    }

    /** Whether a date falls in the Easter weekend: Good Friday to Easter Monday. */
    public static boolean easterWeekend(LocalDate d)
    {
        LocalDate sunday = easter(d.getYear());
        return !d.isBefore(sunday.minusDays(2)) && !d.isAfter(sunday.plusDays(1));
    }

    /** True once per Easter weekend day of a run: that day's bonus waystone is due. Marks it paid. */
    public static boolean easterStoneDue(Journey j, LocalDate d)
    {
        if (!easterWeekend(d)) return false;
        int day = (int) d.toEpochDay();
        Integer paid = j.values.get(PAID_EASTER);
        if (paid != null && paid == day) return false;
        j.values.put(PAID_EASTER, day);
        return true;
    }
}
