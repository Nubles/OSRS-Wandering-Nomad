package com.wanderingnomad.sim;

/**
 * The Forecast cone, the same as the website's forecast.ts. The real future is fixed by the seed,
 * so showing it would spoil the run. Instead this plays the run forward many times with made-up
 * dice, which shows where each circle could plausibly get to, not where it will.
 */
public final class Forecast
{
    /** Played minutes ahead: 1, 3 and 6 hours. */
    public static final int[] HORIZONS = {60, 180, 360};
    public static final int FUTURES = 40;

    /** [horizon][circle] -> x, y pairs, one per imagined future. */
    private final double[][][] points;
    private final int minutes;

    private Forecast(double[][][] points, int minutes)
    {
        this.points = points;
        this.minutes = minutes;
    }

    /** Imagines futures from a copy of the run, so {@code sim} itself is never touched. */
    public static Forecast of(NomadSim sim)
    {
        return of(sim, FUTURES);
    }

    static Forecast of(NomadSim sim, int futures)
    {
        int circles = sim.circles().size();
        double[][][] points = new double[HORIZONS.length][circles][futures * 2];
        Rng dice = new Rng((sim.rngState ^ 0x5bd1e995) + sim.minutes);
        int end = HORIZONS[HORIZONS.length - 1];
        for (int f = 0; f < futures; f++)
        {
            NomadSim copy = sim.copy();
            Rng rng = new Rng((int) (long) Math.floor(dice.next() * 4294967296.0));
            int h = 0;
            for (int m = 1; m <= end; m++)
            {
                copy.step(null, rng);
                if (m == HORIZONS[h])
                {
                    for (int i = 0; i < circles; i++)
                    {
                        points[h][i][f * 2] = copy.circles.get(i).x;
                        points[h][i][f * 2 + 1] = copy.circles.get(i).y;
                    }
                    h++;
                }
            }
        }
        return new Forecast(points, sim.minutes);
    }

    /** The played minute this Forecast was made at. */
    public int minutes()
    {
        return minutes;
    }

    /** Where circle {@code circle} might be at horizon {@code horizon}, as x, y pairs. */
    public double[] points(int horizon, int circle)
    {
        return points[horizon][circle];
    }
}
