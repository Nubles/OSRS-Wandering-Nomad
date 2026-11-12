package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;

/**
 * Chaos rift (a wildcard): now and then a purple rift opens on the ground inside your circle.
 * Step on it to take an hour's visible handicap (a gale on your circle, a dash, or a squeeze);
 * stay inside the whole hour for a rare boon. Never offered while a storm brews or blows.
 */
public final class Chaos
{
    public static final String GALE = "Gale";
    public static final String DASH = "Dash";
    public static final String SQUEEZE = "Squeeze";
    static final String[] HANDICAPS = {GALE, DASH, SQUEEZE};
    static final String[] WORDS = {
        "a gale doubles your circle's drift", "your circle breaks into a dash", "your circle squeezes a quarter smaller"};
    public static final int MINUTES = 60;
    static final int OPEN = 60;
    static final int AT = 2;
    static final int GAP_MIN = 6 * 60;
    static final int GAP_MAX = 12 * 60;
    static final int RETRY = 30;
    static final int DASH_EVERY = 15;

    int next;
    int rifts;
    /** The open rift: where it is in words, its tile and minutes left to step on it. */
    String at;
    int x;
    int y;
    int open;
    /** The handicap under way, minutes left, and whether you've stayed inside. */
    String handicap;
    int left;
    boolean clean;

    public String rift()
    {
        return open > 0 ? at : null;
    }

    public int x()
    {
        return x;
    }

    public int y()
    {
        return y;
    }

    public int open()
    {
        return open;
    }

    public String handicap()
    {
        return left > 0 ? handicap : null;
    }

    public int left()
    {
        return left;
    }

    public boolean clean()
    {
        return clean;
    }

    void minute(NomadSim sim, Tile you, boolean inside, Out out)
    {
        int now = sim.minutes();
        if (next == 0) next = now + gap(sim.seed());
        if (left > 0)
        {
            if (!inside && you != null && clean)
            {
                clean = false;
                out.say("You stepped outside: the chaos rift's boon is lost, but the handicap runs on.");
            }
            if (DASH.equals(handicap) && left % DASH_EVERY == 0) sim.steer(sim.boundIndex(), 0, 0, NomadSim.DASH, DASH_EVERY + 1);
            if (--left > 0) return;
            if (!clean)
            {
                out.say("The chaos rift's hour is up.");
                return;
            }
            String boon = sim.grantBoon(NomadSim.RARE, 0xC4A05 + rifts);
            if (boon != null)
            {
                out.banner("Chaos weathered", "A whole hour inside: the rift gives you " + boon + " (rare).", true);
            }
            else
            {
                out.banner("Chaos weathered", "A whole hour inside. Your hands are full, so a rare waystone rises instead.", true);
                out.summon(NomadSim.RARE, "Weathering the chaos rift");
            }
            return;
        }
        if (open > 0)
        {
            if (you != null && inside && Math.abs(you.x - x) <= AT && Math.abs(you.y - y) <= AT)
            {
                accept(sim, out);
                return;
            }
            if (--open == 0)
            {
                out.say("The chaos rift closes.");
                next = now + gap(sim.seed());
            }
            return;
        }
        if (now < next) return;
        if (sim.stormWarning() > 0 || sim.stormLeft() > 0 || sim.main().sailing())
        {
            next = now + RETRY;
            return;
        }
        Circle c = sim.bound();
        Rng dice = new Rng(sim.seed() ^ ((rifts + 1) * 0xC4A05));
        for (int k = 0; k < 40; k++)
        {
            double a = dice.next() * Math.PI * 2;
            double d = c.radius() * (0.35 + dice.next() * 0.45);
            int rx = (int) Math.round(c.x() + Math.cos(a) * d);
            int ry = (int) Math.round(c.y() + Math.sin(a) * d);
            if (!Geography.onLand(rx, ry) || stoneAt(sim, rx, ry)) continue;
            at = Math.round(d) + " tiles " + NomadSim.compass(a) + " of your circle's middle";
            x = rx;
            y = ry;
            open = OPEN;
            rifts++;
            out.banner("A chaos rift", "It opened " + at + ". Step on it to take an hour's handicap; stay inside the whole hour for a rare boon.", false);
            return;
        }
        next = now + RETRY;
    }

    private void accept(NomadSim sim, Out out)
    {
        open = 0;
        int k = (int) (new Rng(sim.seed() ^ (rifts * 0x5A17)).next() * HANDICAPS.length);
        handicap = HANDICAPS[k];
        left = MINUTES;
        clean = true;
        int b = sim.boundIndex();
        if (GALE.equals(handicap)) sim.tailwind(b, MINUTES);
        else if (DASH.equals(handicap)) sim.steer(b, 0, 0, NomadSim.DASH, DASH_EVERY + 1);
        else sim.squeeze(b, 0.25);
        next = sim.minutes() + MINUTES + gap(sim.seed());
        out.banner("Chaos accepted", "For an hour " + WORDS[k] + ". Stay inside the whole hour for a rare boon.", true);
    }

    static boolean stoneAt(NomadSim sim, int x, int y)
    {
        for (com.wanderingnomad.sim.Waystone w : sim.waystones())
        {
            if (w.x() == x && w.y() == y) return true;
        }
        return false;
    }

    private int gap(int seed)
    {
        return GAP_MIN + (int) (new Rng(seed ^ ((rifts + 1) * 0x9C4A)).next() * (GAP_MAX - GAP_MIN));
    }
}
