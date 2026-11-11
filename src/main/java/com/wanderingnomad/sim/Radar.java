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
    /** How far ahead the tide line shows your circle's edge. */
    public static final int TIDE = 5;

    /** Played minutes until your tile falls outside your circle, or -1 if not within {@link #MINUTES}. */
    public final int edgeIn;
    /** Per circle: minutes until it touches yours, 0 if it does now, -1 if not linked or not soon. */
    public final int[] crossingIn;
    /** The played minute this was worked out at. */
    public final int minutes;
    /** Your circle {@link #TIDE} minutes from now, as {x, y, radius}; null if it'll be at sea. */
    public final double[] tide;

    private Radar(int edgeIn, int[] crossingIn, int minutes, double[] tide)
    {
        this.edgeIn = edgeIn;
        this.crossingIn = crossingIn;
        this.minutes = minutes;
        this.tide = tide;
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
        // Either half of a forked Main counts as inside.
        boolean inside = player == null || sim.inYours(player.x, player.y);
        int edge = inside ? -1 : 0;
        NomadSim copy = sim.copy();
        double[] tide = null;
        for (int m = 1; m <= MINUTES; m++)
        {
            copy.step(player);
            Circle yours = copy.circles.get(bound);
            if (m == TIDE && yours.voyage == null) tide = new double[] {yours.x, yours.y, yours.radius};
            if (edge < 0 && player != null && !copy.inYours(player.x, player.y)) edge = m;
            for (int j = 0; j < n; j++)
            {
                if (crossing[j] < 0 && sim.linked(j) && yours.touches(copy.circles.get(j))) crossing[j] = m;
            }
        }
        return new Radar(edge, crossing, sim.minutes(), tide);
    }
}
