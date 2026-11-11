package com.wanderingnomad.stream;

import java.util.HashMap;
import java.util.Map;

/**
 * Echoes: come back to a kingdom after {@link #GAP} played minutes away and a banner recalls the
 * biggest thing that happened there last time. Only places where something worth telling happened
 * have an echo.
 */
public final class Echoes
{
    public static final int GAP = 600;
    /** The lightest beat worth an echo. */
    static final int WORTH = 2;
    public static final String METRIC = "echoes";

    public static final class Place
    {
        public int last = -1;
        public String best;
        public int bestWeight;
        public int bestMinute;
    }

    /** An echo to tell: the kingdom, what happened, and how many played minutes ago. */
    public static final class Echo
    {
        public final String region;
        public final String line;
        public final int ago;

        Echo(String region, String line, int ago)
        {
            this.region = region;
            this.line = line;
            this.ago = ago;
        }
    }

    public Map<String, Place> places = new HashMap<>();
    public String current;

    public Echoes ready()
    {
        if (places == null) places = new HashMap<>();
        return this;
    }

    /** Remembers a beat in the kingdom it happened in, if it's the biggest there (a tie goes to the newer). */
    public void note(String region, int minute, String text, int weight)
    {
        if (region == null || weight < WORTH) return;
        Place p = places.computeIfAbsent(region, k -> new Place());
        if (weight >= p.bestWeight)
        {
            p.best = text;
            p.bestWeight = weight;
            p.bestMinute = minute;
        }
    }

    /** A played minute in {@code region} (null at sea or unknown). Returns the echo for coming back, or null. */
    public Echo minute(String region, int minute)
    {
        if (region == null)
        {
            current = null;
            return null;
        }
        Place p = places.computeIfAbsent(region, k -> new Place());
        Echo echo = null;
        if (!region.equals(current) && p.last >= 0 && minute - p.last >= GAP && p.best != null)
        {
            echo = new Echo(region, p.best, minute - p.bestMinute);
        }
        p.last = minute;
        current = region;
        return echo;
    }
}
