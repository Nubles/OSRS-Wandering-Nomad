package com.wanderingnomad.social;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.Region;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;

/**
 * The Lodestar: once a week, at an hour and a landmark worked out from the date alone, a Lodestar
 * rises for an hour and every Main within reach leans toward it. Everyone's plugin works out the
 * same one, so strangers' circles all pull to the same spot with no server.
 */
public final class Lodestar
{
    /** How long it shines, how far away a circle still feels it, and how close counts as arriving. */
    public static final long HOUR_MS = 60 * 60 * 1000L;
    public static final int REACH = 400;
    public static final int ARRIVED = 6;

    /** One week's Lodestar: its week (as year * 100 + ISO week), when it rises (epoch ms, UTC) and where. */
    public static final class Event
    {
        public final int week;
        public final long rises;
        public final Landmarks.Landmark landmark;

        Event(int week, long rises, Landmarks.Landmark landmark)
        {
            this.week = week;
            this.rises = rises;
            this.landmark = landmark;
        }

        public boolean shining(long now)
        {
            return now >= rises && now < rises + HOUR_MS;
        }
    }

    private Lodestar()
    {
    }

    private static volatile List<Landmarks.Landmark> safe;

    /** Landmarks a Lodestar may rise at: on land, in a kingdom, never in the Wilderness. Worked out once. */
    static List<Landmarks.Landmark> safe()
    {
        List<Landmarks.Landmark> known = safe;
        if (known != null) return known;
        List<Landmarks.Landmark> out = new ArrayList<>();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            Region r = Geography.regionAt(l.x, l.y);
            if (r != null && !"wilderness".equals(r.id) && Geography.onLand(l.x, l.y)) out.add(l);
        }
        safe = java.util.Collections.unmodifiableList(out);
        return safe;
    }

    /** The Lodestar of the week (Monday to Sunday, UTC) holding {@code day}. */
    public static Event of(LocalDate day)
    {
        LocalDate monday = day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        int week = monday.get(IsoFields.WEEK_BASED_YEAR) * 100 + monday.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        long h = mix(week * 0x9E3779B97F4A7C15L + 0x10DE57A2L);
        int dayOfWeek = (int) Math.floorMod(h, 7L);
        h = mix(h);
        // Evenings and afternoons in Europe and the Americas: 14:00 to 23:00 UTC.
        int hour = 14 + (int) Math.floorMod(h, 10L);
        h = mix(h);
        List<Landmarks.Landmark> safe = safe();
        Landmarks.Landmark l = safe.get((int) Math.floorMod(h, (long) safe.size()));
        long rises = monday.plusDays(dayOfWeek).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() + hour * HOUR_MS;
        return new Event(week, rises, l);
    }

    /** The Lodestar shining now, or the next one to come. */
    public static Event next(long now)
    {
        LocalDate today = java.time.Instant.ofEpochMilli(now).atZone(ZoneOffset.UTC).toLocalDate();
        Event e = of(today);
        return now < e.rises + HOUR_MS ? e : of(today.plusWeeks(1));
    }

    static long mix(long z)
    {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }
}
