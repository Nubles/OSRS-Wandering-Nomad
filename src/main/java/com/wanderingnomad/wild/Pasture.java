package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Pasture (a wildcard): skilling marks the 64-tile map square you stand in as grazed for a day.
 * Main won't linger over grazed ground, and when it moves off grazed ground onto fresh pasture it
 * lingers there a while.
 */
public final class Pasture
{
    public static final int SQUARE = 64;
    public static final int FADE = 24 * 60;
    static final int EVERY = 30;
    static final int LINGER = 30;

    /** Grazed squares, by key, with the played minute they were last grazed. */
    Map<Integer, Integer> grazed = new HashMap<>();
    int last = -1;

    static int key(double x, double y)
    {
        return ((int) x / SQUARE) * 1024 + (int) y / SQUARE;
    }

    /** XP in a gathering or artisan skill where you stand. */
    void skill(Tile at, int now)
    {
        if (at == null) return;
        grazed.put(key(at.x, at.y), now);
    }

    public boolean grazed(double x, double y, int now)
    {
        Integer at = grazed.get(key(x, y));
        return at != null && now - at < FADE;
    }

    /** Grazed squares still fresh in memory, as x, y of their south-west corner and minutes left. */
    public java.util.List<int[]> squares(int now)
    {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (Map.Entry<Integer, Integer> e : grazed.entrySet())
        {
            int left = FADE - (now - e.getValue());
            if (left > 0) out.add(new int[]{(e.getKey() / 1024) * SQUARE, (e.getKey() % 1024) * SQUARE, left});
        }
        return out;
    }

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        if (now % EVERY != 0) return;
        for (Iterator<Map.Entry<Integer, Integer>> it = grazed.entrySet().iterator(); it.hasNext(); )
        {
            if (now - it.next().getValue() >= FADE) it.remove();
        }
        Circle m = sim.main();
        int here = key(m.x(), m.y());
        boolean wasGrazed = last >= 0 && last != here && grazed(((last / 1024) * SQUARE), (last % 1024) * SQUARE, now);
        last = here;
        if (m.sailing()) return;
        if (grazed(m.x(), m.y(), now))
        {
            if (sim.mood(NomadSim.MAIN) == NomadSim.LINGER && sim.rerollMood(NomadSim.MAIN))
            {
                out.say("Main won't linger on grazed ground: it moves on.");
            }
        }
        else if (wasGrazed && sim.steer(NomadSim.MAIN, 0, 0, NomadSim.LINGER, LINGER))
        {
            out.say("Fresh pasture: Main lingers here a while.");
        }
    }

    void ready()
    {
        if (grazed == null) grazed = new HashMap<>();
    }
}
