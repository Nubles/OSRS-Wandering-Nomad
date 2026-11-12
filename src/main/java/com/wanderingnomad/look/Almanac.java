package com.wanderingnomad.look;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * The fog of odds: past the Radar's 15 minutes, a sidebar almanac in four vague words, read from
 * the Forecast's imagined futures. Each Forecast rolls fresh dice, so the words never harden into
 * a promise.
 */
public final class Almanac
{
    public static final String UNLIKELY = "unlikely";
    public static final String POSSIBLE = "possible";
    public static final String LIKELY = "likely";
    public static final String CERTAIN = "almost certain";

    private Almanac()
    {
    }

    /** A share of futures as one of four words. */
    public static String word(double share)
    {
        return share < 0.15 ? UNLIKELY : share < 0.45 ? POSSIBLE : share < 0.8 ? LIKELY : CERTAIN;
    }

    /** The almanac's lines; empty without a Forecast. */
    public static List<String> of(NomadSim sim, Forecast f, Tile you)
    {
        List<String> out = new ArrayList<>();
        if (f == null) return out;
        Circle main = sim.main();
        double[] h1 = f.points(0, NomadSim.MAIN);
        double[] h3 = f.points(1, NomadSim.MAIN);
        double[] h6 = f.points(2, NomadSim.MAIN);
        int futures = h1.length / 2;
        if (futures == 0) return out;
        if (you != null)
        {
            int over = 0;
            for (int i = 0; i < futures; i++) if (Math.hypot(h1[i * 2] - you.x, h1[i * 2 + 1] - you.y) <= main.radius()) over++;
            out.add("Main still over this spot in 1h: " + word(over / (double) futures));
        }
        Region here = Geography.regionAt(main.x(), main.y());
        int moved = 0;
        for (int i = 0; i < futures; i++)
        {
            Region r = Geography.regionAt(h3[i * 2], h3[i * 2 + 1]);
            if (r != null && r != here) moved++;
        }
        out.add("Main in another kingdom by 3h: " + word(moved / (double) futures));
        int sea = 0;
        for (int i = 0; i < futures; i++) if (!Geography.onLand(h6[i * 2], h6[i * 2 + 1])) sea++;
        out.add("Main at sea at 6h: " + word(sea / (double) futures));
        int bound = sim.boundIndex();
        if (bound != NomadSim.MAIN)
        {
            if (bound >= f.circles()) return out;
            double[] b3 = f.points(1, bound);
            Circle c = sim.bound();
            int touch = 0;
            for (int i = 0; i < futures; i++)
            {
                if (Math.hypot(h3[i * 2] - b3[i * 2], h3[i * 2 + 1] - b3[i * 2 + 1]) <= main.radius() + c.radius()) touch++;
            }
            out.add("A way back into Main by 3h: " + word(touch / (double) futures));
        }
        else
        {
            String best = null;
            int most = -1;
            for (int j = 1; j < Math.min(sim.circles().size(), f.circles()); j++)
            {
                if (!sim.linked(j)) continue;
                Circle c = sim.circles().get(j);
                double[] p = f.points(0, j);
                int touch = 0;
                for (int i = 0; i < futures; i++)
                {
                    if (Math.hypot(h1[i * 2] - p[i * 2], h1[i * 2 + 1] - p[i * 2 + 1]) <= main.radius() + c.radius()) touch++;
                }
                if (touch > most)
                {
                    most = touch;
                    best = c.name();
                }
            }
            if (best != null) out.add("A crossing with " + best + " within 1h: " + word(most / (double) futures));
        }
        return out;
    }
}
