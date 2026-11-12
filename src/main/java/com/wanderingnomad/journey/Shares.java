package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.social.Codes;
import java.util.Map;
import java.util.TreeSet;

/**
 * What the opt-in Website settings send to the relay, and nothing more: a lobby circle, a
 * tombstone, and the kingdoms and map regions for the atlas. The relay checks every field again.
 */
public final class Shares
{
    /** A lobby or grave name: at most an RSN's length. */
    public static final int NAME_MAX = 12;
    static final int EPITAPH_MAX = 140;
    static final int SQUARES_MAX = 4000;

    private Shares()
    {
    }

    /** Your circle on the lobby map. */
    public static final class Lobby
    {
        public String name;
        public int x;
        public int y;
        public int r;
        public String mood;
    }

    /** A tombstone for the Graveyard. */
    public static final class Grave
    {
        public String name;
        public int x;
        public int y;
        public int minutes;
        public int seed;
        public String kingdom;
        public String circle;
        public int cx;
        public int cy;
        public int r;
        public String epitaph;
        public boolean hardcore;
        public int items;
    }

    /** The atlas: kingdoms as a bitmask over {@link Geography#REGIONS}, and map regions walked. */
    public static final class Atlas
    {
        public int k;
        public int[] squares;
    }

    public static Lobby lobby(String name, NomadSim sim)
    {
        Circle c = sim.bound();
        Lobby l = new Lobby();
        l.name = name(name);
        l.x = (int) Math.round(c.x());
        l.y = (int) Math.round(c.y());
        l.r = (int) Math.max(1, Math.round(c.radius()));
        l.mood = sim.main().moodName();
        return l;
    }

    public static Grave grave(String name, NomadSim sim, Journey j, int x, int y, boolean hardcore, int items)
    {
        Circle c = sim.bound();
        Region over = Geography.regionAt(x, y);
        Grave g = new Grave();
        g.name = name(name);
        g.x = x;
        g.y = y;
        g.minutes = sim.minutes();
        g.seed = sim.seed();
        g.kingdom = over == null ? null : over.name;
        g.circle = c.name();
        g.cx = (int) Math.round(c.x());
        g.cy = (int) Math.round(c.y());
        g.r = (int) Math.max(1, Math.round(c.radius()));
        g.epitaph = epitaph(sim, j);
        g.hardcore = hardcore;
        g.items = Math.max(0, items);
        return g;
    }

    public static Atlas atlas(Journey j)
    {
        Atlas a = new Atlas();
        for (Map.Entry<String, Integer> e : j.kingdomMinutes.entrySet())
        {
            for (int i = 0; i < Geography.REGIONS.size() && i < 31; i++)
            {
                if (Geography.REGIONS.get(i).name.equals(e.getKey()) && e.getValue() > 0) a.k |= 1 << i;
            }
        }
        TreeSet<Integer> squares = new TreeSet<>();
        for (String id : j.items(Trials.SQUARES))
        {
            try
            {
                squares.add(Integer.parseInt(id));
            }
            catch (NumberFormatException e)
            {
                // Not a square id.
            }
            if (squares.size() >= SQUARES_MAX) break;
        }
        a.squares = squares.stream().mapToInt(Integer::intValue).toArray();
        return a;
    }

    /** One line from the ledger for a tombstone: how long, how far, and the run's most telling numbers. */
    public static String epitaph(NomadSim sim, Journey j)
    {
        StringBuilder b = new StringBuilder("Wandered " + Ledger.time(sim.minutes()));
        int walked = j.value(Trials.WALKED);
        if (walked > 0) b.append(", walked ").append(String.format("%,d", walked)).append(" tiles inside");
        if (sim.forks() > 0) b.append(", outlived ").append(sim.forks()).append(sim.forks() == 1 ? " fork" : " forks");
        int claimed = j.value(Trials.CLAIMED);
        if (claimed > 0) b.append(", claimed ").append(claimed).append(claimed == 1 ? " waystone" : " waystones");
        b.append('.');
        String closest = j.closestCall();
        if (closest != null && b.length() + closest.length() + 1 <= EPITAPH_MAX) b.append(' ').append(closest);
        return Codes.clean(b.toString(), EPITAPH_MAX);
    }

    private static String name(String name)
    {
        String n = Codes.clean(name, NAME_MAX).replaceAll("[^A-Za-z0-9 _-]", "").trim();
        return n.isEmpty() ? null : n;
    }
}
