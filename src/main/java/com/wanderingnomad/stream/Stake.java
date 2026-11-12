package com.wanderingnomad.stream;

import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import com.wanderingnomad.social.Dare;
import java.util.HashMap;
import java.util.Map;

/**
 * The episode stake: when you start recording, one headline stake for the episode, read only from
 * what the run already shows (a fork's warning, a storm brewing, a waystone's time left, an open
 * dare, the edge's countdown, the Forecast's odds). At the episode's end it says how it went.
 * Pure framing: no rewards.
 */
public final class Stake
{
    public static final String FORK = "fork";
    public static final String STORM = "storm";
    public static final String STONE = "stone";
    public static final String DARE = "dare";
    public static final String EDGE = "edge";
    public static final String REACH = "reach";
    public static final String INSIDE = "inside";
    /** The Forecast's odds must be at least this to make a stake. */
    static final int ODDS = 20;

    public String kind;
    public String headline;
    public String arg;
    public int minute;
    /** The episode's tallies when the stake was set, to tell what changed. */
    public Map<String, Integer> base = new HashMap<>();
    public String result;

    /** The stake for now, the most pressing first. {@code edgeIn} is the Radar's minutes to the edge, or -1. */
    public static Stake pick(NomadSim sim, Tile you, Journey j, int edgeIn, Forecast forecast, Session s)
    {
        Stake k = new Stake();
        k.minute = sim.minutes();
        k.base = new HashMap<>(s.counts);
        if (sim.forkWarning() > 0)
        {
            k.kind = FORK;
            k.headline = "Main forks in " + sim.forkWarning() + " min, " + NomadSim.compass(sim.forkAxis()) + " or "
                + NomadSim.compass(sim.forkAxis() + Math.PI) + ": which half lives?";
            return k;
        }
        if (sim.stormWarning() > 0)
        {
            k.kind = STORM;
            k.headline = "A storm breaks over Main in " + sim.stormWarning() + " min.";
            return k;
        }
        Waystone near = null;
        double best = Double.MAX_VALUE;
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != sim.boundIndex() || w.legendary() || you == null) continue;
            double d = Math.hypot(w.x() - you.x, w.y() - you.y);
            if (d < best)
            {
                best = d;
                near = w;
            }
        }
        if (near != null)
        {
            k.kind = STONE;
            k.arg = near.landmark() + "|" + near.circle();
            int left = sim.minutesLeft(near);
            k.headline = "A " + near.boon() + " waystone at " + near.landmark() + (left >= 0 ? " fades in about " + left + " min" : " waits")
                + ": can you claim it?";
            return k;
        }
        Dare d = j == null ? null : j.dare();
        if (d != null && sim.minutes() < d.deadline && d.progress(j) < d.target)
        {
            k.kind = DARE;
            int hours = Math.max(1, (d.deadline - sim.minutes() + 59) / 60);
            k.headline = d.from + "'s dare: " + Previously.lower(d.ask()) + ", " + hours + (hours == 1 ? " hour" : " hours") + " left.";
            return k;
        }
        if (edgeIn > 0)
        {
            k.kind = EDGE;
            k.headline = "The edge reaches you in " + edgeIn + " min. Can you stay inside all episode?";
            return k;
        }
        String region = null;
        int odds = 0;
        if (forecast != null)
        {
            Region here = Geography.regionAt(sim.main().x(), sim.main().y());
            Map<String, Integer> tally = new HashMap<>();
            int last = Forecast.HORIZONS.length - 1;
            double[] p = forecast.points(last, NomadSim.MAIN);
            int futures = p.length / 2;
            for (int f = 0; f < futures; f++)
            {
                Region r = Geography.regionAt(p[f * 2], p[f * 2 + 1]);
                if (r != null && r != here) tally.merge(r.name, 1, Integer::sum);
            }
            for (Map.Entry<String, Integer> e : tally.entrySet())
            {
                int pct = e.getValue() * 100 / Math.max(1, futures);
                if (pct > odds)
                {
                    odds = pct;
                    region = e.getKey();
                }
            }
        }
        if (region != null && odds >= ODDS)
        {
            k.kind = REACH;
            k.arg = region;
            k.headline = "The Forecast gives Main a " + odds + "% chance of reaching " + region + " within "
                + Forecast.HORIZONS[Forecast.HORIZONS.length - 1] / 60 + " hours.";
            return k;
        }
        k.kind = INSIDE;
        k.headline = "Stay inside your circle for the whole episode.";
        return k;
    }

    private int since(Session s, String key)
    {
        Integer b = base.get(key);
        return s.count(key) - (b == null ? 0 : b);
    }

    /** How the stake went, by the episode's end. */
    public String resolve(NomadSim sim, Journey j, Session s)
    {
        int out = since(s, Beat.OUT);
        switch (kind)
        {
            case FORK:
                result = since(s, Beat.PARTED) > 0 ? "The halves parted, and you were in the half that lived." : "Main hadn't forked by the end.";
                break;
            case STORM:
                result = since(s, Session.STORM_MINUTES) > 0 ? "The storm broke, and you rode it out." : "The storm never broke.";
                break;
            case STONE:
            {
                boolean standing = false;
                for (Waystone w : sim.waystones()) if ((w.landmark() + "|" + w.circle()).equals(arg)) standing = true;
                result = standing ? "The waystone still stands." : since(s, Beat.CLAIM) > 0 ? "Claimed." : "It faded.";
                break;
            }
            case DARE:
            {
                Dare d = j == null ? null : j.dare();
                result = d == null || d.progress(j) >= d.target ? "The dare is settled." : "Still open: " + d.progress(j) + " of " + d.target + ".";
                break;
            }
            case REACH:
                result = s.regions.contains(arg) ? "Main reached " + arg + "." : "Main never reached " + arg + ".";
                break;
            default:
                result = out == 0 ? "Not one step outside." : "Stepped outside " + out + (out == 1 ? " time." : " times.");
                break;
        }
        if (EDGE.equals(kind) && out == 0) result = "The edge never caught you.";
        return result;
    }
}
