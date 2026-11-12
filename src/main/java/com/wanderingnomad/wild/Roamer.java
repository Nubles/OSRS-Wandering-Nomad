package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;

/**
 * The Roamer (a wildcard): a tiny wisp circle roams the land fast. When it wanders into Main it
 * holds still for 5 minutes: step into it for a rare boon. Each time it slips away it gets
 * bolder, and after three escapes it lingers an hour.
 */
public final class Roamer
{
    public static final int RADIUS = 4;
    static final double SPEED = 3;
    static final double FLEE_SPEED = 8;
    static final int WINDOW = 5;
    static final int LINGER = 60;
    static final int ESCAPES = 3;
    static final int FLEE = 30;
    /** It first appears this far from Main, and again this long after it's caught. */
    static final int FAR = 250;
    static final int REST = 12 * 60;
    static final int FIRST = 60;

    boolean out;
    double x;
    double y;
    double heading;
    int window;
    int escapes;
    int flee;
    int rest = FIRST;
    int caught;
    transient boolean full;

    public boolean on()
    {
        return out;
    }

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    public int window()
    {
        return window;
    }

    public int escapes()
    {
        return escapes;
    }

    /** Each game tick and minute: whether you're standing in it while it waits. True if you caught it. */
    boolean touch(NomadSim sim, Tile at, Out out)
    {
        if (!this.out || window <= 0 || at == null || Math.hypot(at.x - x, at.y - y) > RADIUS) return false;
        String boon = sim.grantBoon(NomadSim.RARE, 0x40A3 + caught);
        if (boon == null)
        {
            if (!full) out.say("The Roamer waits for you: your hands are full. Spend a boon and step in again.");
            full = true;
            return false;
        }
        caught++;
        this.out = false;
        window = 0;
        escapes = 0;
        rest = REST;
        out.banner("You caught the Roamer", "It gives up " + boon + " (rare) and fades. Another roams in " + REST / 60 + " hours.", true);
        return true;
    }

    void minute(NomadSim sim, Out o)
    {
        int now = sim.minutes();
        Rng dice = new Rng(sim.seed() ^ (now * 0x40A3C1) ^ 0x7A11);
        if (!out)
        {
            if (rest > 0 && --rest > 0) return;
            if (!spawn(sim, dice)) return;
            o.say("A Roamer wisp is loose, " + (int) Math.round(Math.hypot(x - sim.main().x(), y - sim.main().y())) + " tiles "
                + NomadSim.compass(Math.atan2(y - sim.main().y(), x - sim.main().x())) + " of Main. If it wanders into Main, step into it.");
            return;
        }
        Circle m = sim.main();
        if (window > 0)
        {
            if (--window > 0) return;
            escapes++;
            flee = FLEE;
            heading = Math.atan2(y - m.y(), x - m.x());
            o.say(escapes >= ESCAPES ? "The Roamer slipped away again. Next time it meets Main, it lingers an hour."
                : "The Roamer slipped away.");
            return;
        }
        double speed = flee > 0 ? FLEE_SPEED : SPEED;
        if (flee > 0) flee--;
        else heading += (dice.next() - 0.5) * 0.6;
        move(speed, dice);
        if (flee == 0 && Math.hypot(x - m.x(), y - m.y()) <= m.radius() - RADIUS)
        {
            window = escapes >= ESCAPES ? LINGER : WINDOW;
            full = false;
            o.banner("The Roamer is in Main", "It holds still for " + window + " minutes: step into it for a rare boon.", true);
        }
    }

    private boolean spawn(NomadSim sim, Rng dice)
    {
        Circle m = sim.main();
        for (int k = 0; k < 30; k++)
        {
            Landmarks.Landmark l = Landmarks.ALL.get((int) (dice.next() * Landmarks.ALL.size()));
            double d = Math.hypot(l.x - m.x(), l.y - m.y());
            if (d < FAR || d > FAR * 3 || !Geography.onLand(l.x, l.y)) continue;
            x = l.x;
            y = l.y;
            heading = dice.next() * Math.PI * 2;
            out = true;
            window = 0;
            flee = 0;
            return true;
        }
        rest = 30;
        return false;
    }

    /** Moves on over land, turning to stay on it. */
    private void move(double speed, Rng dice)
    {
        for (double off : new double[]{0, 0.6, -0.6, 1.2, -1.2, 2.0, -2.0, Math.PI})
        {
            double nx = x + Math.cos(heading + off) * speed;
            double ny = y + Math.sin(heading + off) * speed;
            if (Geography.onLand(nx, ny) && Geography.pocketDepth(nx, ny) == 0)
            {
                x = nx;
                y = ny;
                heading += off;
                return;
            }
        }
        heading = dice.next() * Math.PI * 2;
    }
}
