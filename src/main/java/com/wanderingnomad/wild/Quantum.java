package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Waystone;
import java.util.ArrayList;
import java.util.List;

/**
 * Quantum waystone (a wildcard): half the rare waystones are quantum, with two more spots on open
 * ground in their circle. Out of sight for 3 seconds, one jumps to another of its three spots, so
 * you claim it by keeping it in view the whole walk. Its spots show as ghosts on the maps.
 */
public final class Quantum
{
    public static final int UNSEEN_TICKS = 5;
    static final double SHARE = 0.5;
    static final String ELSEWHERE = "a quantum spot";

    public static final class Stone
    {
        int x;
        int y;
        /** Its three spots as x, y, and the first's landmark name. */
        int[][] spots;
        String home;
        int jumps;
        transient int unseen;
        transient boolean jumped;

        public int x()
        {
            return x;
        }

        public int y()
        {
            return y;
        }

        public int[][] spots()
        {
            return spots;
        }

        public int jumps()
        {
            return jumps;
        }
    }

    List<Stone> stones = new ArrayList<>();
    /** Rare stones already weighed, by tile, so each is decided once. */
    List<Integer> weighed = new ArrayList<>();

    public List<Stone> stones()
    {
        return stones;
    }

    /** Each minute: forgets stones that are gone, and weighs new rare ones. */
    void minute(NomadSim sim, Out out)
    {
        stones.removeIf(s -> find(sim, s) == null);
        for (Waystone w : sim.waystones())
        {
            if (!NomadSim.RARE.equals(w.tier()) || w.pilgrim() || w.life() > 0 || w.wanders()) continue;
            int key = w.x() * 16384 + w.y();
            if (weighed.contains(key) || tracked(w)) continue;
            weighed.add(key);
            if (weighed.size() > 200) weighed.remove(0);
            Rng dice = new Rng(sim.seed() ^ key ^ 0x9A47);
            if (dice.next() >= SHARE) continue;
            Circle c = sim.circles().get(w.circle());
            List<int[]> spots = new ArrayList<>();
            spots.add(new int[]{w.x(), w.y()});
            for (int k = 0; k < 40 && spots.size() < 3; k++)
            {
                double a = dice.next() * Math.PI * 2;
                double d = c.radius() * (0.25 + dice.next() * 0.6);
                int x = (int) Math.round(c.x() + Math.cos(a) * d);
                int y = (int) Math.round(c.y() + Math.sin(a) * d);
                if (!Geography.onLand(x, y) || Chaos.stoneAt(sim, x, y) || near(spots, x, y)) continue;
                spots.add(new int[]{x, y});
            }
            if (spots.size() < 3) continue;
            Stone s = new Stone();
            s.x = w.x();
            s.y = w.y();
            s.home = w.landmark();
            s.spots = spots.toArray(new int[0][]);
            stones.add(s);
            out.say("The rare waystone at " + w.landmark() + " is quantum: out of sight for 3 seconds, it jumps between three spots in "
                + c.name() + ". Keep it in view as you walk to it.");
        }
    }

    private static boolean near(List<int[]> spots, int x, int y)
    {
        for (int[] s : spots) if (Math.hypot(s[0] - x, s[1] - y) < 8) return true;
        return false;
    }

    /**
     * Each game tick, with whether each stone's tile is on screen (by index into {@link #stones()}).
     * One unseen for 3 seconds jumps once, and waits there until it's seen again.
     */
    void tick(NomadSim sim, boolean[] seen, Rng dice)
    {
        for (int i = 0; i < stones.size() && i < seen.length; i++)
        {
            Stone s = stones.get(i);
            if (seen[i])
            {
                s.unseen = 0;
                s.jumped = false;
                continue;
            }
            if (s.jumped || ++s.unseen < UNSEEN_TICKS) continue;
            s.jumped = true;
            Waystone w = find(sim, s);
            if (w == null) continue;
            int from = 0;
            for (int k = 0; k < s.spots.length; k++) if (s.spots[k][0] == w.x() && s.spots[k][1] == w.y()) from = k;
            int to = (from + 1 + (int) (dice.next() * (s.spots.length - 1))) % s.spots.length;
            int[] spot = s.spots[to];
            if (!sim.moveWaystone(w, spot[0], spot[1], to == 0 ? s.home : ELSEWHERE)) continue;
            s.x = spot[0];
            s.y = spot[1];
            s.jumps++;
        }
    }

    private boolean tracked(Waystone w)
    {
        for (Stone s : stones) if (s.x == w.x() && s.y == w.y()) return true;
        return false;
    }

    static Waystone find(NomadSim sim, Stone s)
    {
        for (Waystone w : sim.waystones()) if (w.x() == s.x && w.y() == s.y) return w;
        return null;
    }

    void ready()
    {
        if (stones == null) stones = new ArrayList<>();
        if (weighed == null) weighed = new ArrayList<>();
    }
}
