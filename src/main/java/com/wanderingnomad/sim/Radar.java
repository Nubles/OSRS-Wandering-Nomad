package com.wanderingnomad.sim;

import java.util.Arrays;

/**
 * The next few minutes, exactly: a copy of the run played forward with its own dice while you
 * stand still. It says when the edge will reach you and when each crossing opens. Fifteen
 * minutes is short enough not to spoil the run, and long enough to finish what you're doing.
 */
public final class Radar
{
    public static final int MINUTES = 15;

    /** Played minutes until your tile falls outside your circle, or -1 if not within {@link #MINUTES}. */
    public final int edgeIn;
    /** Per circle: minutes until it touches yours, 0 if it does now, -1 if not linked or not soon. */
    public final int[] crossingIn;
    /** The played minute this was worked out at. */
    public final int minutes;

    private Radar(int edgeIn, int[] crossingIn, int minutes)
    {
        this.edgeIn = edgeIn;
        this.crossingIn = crossingIn;
        this.minutes = minutes;
    }

    public static Radar of(NomadSim sim, Tile player)
    {
        int n = sim.circles().size();
        int[] crossing = new int[n];
        Arrays.fill(crossing, -1);
        int bound = sim.boundIndex();
        for (int j = 0; j < n; j++)
        {
            if (sim.linked(j) && sim.bound().touches(sim.circles().get(j))) crossing[j] = 0;
        }
        boolean inside = player == null || sim.bound().contains(player.x, player.y);
        int edge = inside ? -1 : 0;
        NomadSim copy = sim.copy();
        for (int m = 1; m <= MINUTES; m++)
        {
            copy.step(player);
            Circle yours = copy.circles.get(bound);
            if (edge < 0 && player != null && !yours.contains(player.x, player.y)) edge = m;
            for (int j = 0; j < n; j++)
            {
                if (crossing[j] < 0 && sim.linked(j) && yours.touches(copy.circles.get(j))) crossing[j] = m;
            }
        }
        return new Radar(edge, crossing, sim.minutes());
    }
}
