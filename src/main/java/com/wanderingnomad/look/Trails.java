package com.wanderingnomad.look;

import com.wanderingnomad.sim.NomadSim;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comet tails: each circle's middle over the last three played hours, a point a minute, for the
 * world map to draw as a fading trail, thicker where the circle lingered.
 */
public final class Trails
{
    public static final int MINUTES = 180;

    private final Map<Integer, Deque<double[]>> paths = new HashMap<>();
    private int seed;

    /** A played minute: notes where every circle's middle is. A new run starts fresh. */
    public synchronized void sample(NomadSim sim)
    {
        if (sim.seed() != seed)
        {
            paths.clear();
            seed = sim.seed();
        }
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Deque<double[]> d = paths.computeIfAbsent(j, k -> new ArrayDeque<>());
            d.addLast(new double[]{sim.circles().get(j).x(), sim.circles().get(j).y()});
            while (d.size() > MINUTES) d.removeFirst();
        }
    }

    /** A circle's trail, oldest first. */
    public synchronized List<double[]> of(int circle)
    {
        Deque<double[]> d = paths.get(circle);
        return d == null ? new ArrayList<>() : new ArrayList<>(d);
    }

    public synchronized void clear()
    {
        paths.clear();
    }

    /** How thick a stretch of trail is drawn: thicker the less it moved that minute. */
    public static float width(double moved)
    {
        return (float) Math.max(1.5, Math.min(5, 5 - moved * 1.5));
    }
}
