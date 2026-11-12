package com.wanderingnomad.stream;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An episode's drama score, 0 to 100, from its close calls, forks, storms, rare finds, deaths and
 * the rest, each worth so much up to a cap, with the breakdown shown so the number is believable.
 */
public final class Drama
{
    public static final class Part
    {
        public final String label;
        public final int count;
        public final int points;

        Part(String label, int count, int points)
        {
            this.label = label;
            this.count = count;
            this.points = points;
        }

        public String line()
        {
            return label + " x" + count + ": +" + points;
        }
    }

    public final int total;
    public final List<Part> parts;

    private Drama(int total, List<Part> parts)
    {
        this.total = total;
        this.parts = Collections.unmodifiableList(parts);
    }

    public static Drama of(Session s)
    {
        List<Part> parts = new ArrayList<>();
        int sum = 0;
        sum += part(parts, "Close calls", s.count(Beat.NEAR), 8, 24);
        sum += part(parts, "Forks parted", s.count(Beat.PARTED), 10, 20);
        sum += part(parts, "Deaths", s.count(Beat.DEATH), 20, 20);
        sum += part(parts, "Storms", s.count(Beat.STORM), 6, 12);
        sum += part(parts, "Rare stones and comets", s.count(Beat.RARE) + s.count(Beat.COMET), 6, 18);
        sum += part(parts, "Castaways", s.count(Beat.CASTAWAY), 8, 8);
        sum += part(parts, "New acts", s.count(Beat.ACT), 10, 10);
        sum += part(parts, "New collection log slots", s.count(Beat.LOG), 4, 12);
        sum += part(parts, "Crossings", s.count(Beat.CROSS), 3, 9);
        sum += part(parts, "Waystones claimed", s.count(Beat.CLAIM), 2, 6);
        int out = s.count(Session.OUT_MINUTES);
        if (out > 0)
        {
            int points = Math.min(10, out / 3);
            if (points > 0) parts.add(new Part("Minutes outside", out, points));
            sum += points;
        }
        sum += part(parts, "Moments you marked", s.count(Beat.MARK), 2, 6);
        return new Drama(Math.min(100, sum), parts);
    }

    private static int part(List<Part> parts, String label, int count, int each, int cap)
    {
        if (count <= 0) return 0;
        int points = Math.min(cap, count * each);
        parts.add(new Part(label, count, points));
        return points;
    }

    /** "3rd of 14 episodes": where a score ranks among the earlier ones (ties rank together). */
    public static String rank(int score, List<Integer> earlier)
    {
        int above = 0;
        for (int e : earlier) if (e > score) above++;
        int place = above + 1;
        return ordinal(place) + " of " + (earlier.size() + 1) + (earlier.isEmpty() ? " episode" : " episodes");
    }

    static String ordinal(int n)
    {
        int tens = n % 100;
        String suffix = tens >= 11 && tens <= 13 ? "th" : n % 10 == 1 ? "st" : n % 10 == 2 ? "nd" : n % 10 == 3 ? "rd" : "th";
        return n + suffix;
    }
}
