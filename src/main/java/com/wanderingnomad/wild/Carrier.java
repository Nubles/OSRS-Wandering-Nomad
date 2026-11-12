package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;

/**
 * Redeploy (a wildcard): after a death, a small circle at your respawn walks toward your circle
 * to carry you back, and your circle dashes to meet it. Inside the carrier counts as inside. It
 * keeps to land, and fades after a few hours or once it meets your circle. Your pile still can't
 * be reclaimed.
 */
public final class Carrier
{
    public static final int RADIUS = 8;
    static final double SPEED = 10;
    static final int MINUTES = 3 * 60;
    static final int AIM_EVERY = 10;
    /** After a death, it starts where you respawn: once you're this far from where you fell, or after this many ticks. */
    static final int RESPAWN_TILES = 20;
    static final int RESPAWN_TICKS = 25;

    double x;
    double y;
    int left;
    int lastAim = -1000;
    transient boolean pending;
    transient int fellX;
    transient int fellY;
    transient int ticks;

    public boolean on()
    {
        return left > 0;
    }

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    public int left()
    {
        return left;
    }

    public boolean contains(Tile t)
    {
        return left > 0 && t != null && Math.hypot(t.x - x, t.y - y) <= RADIUS;
    }

    /** You fell here; the carrier starts once you've respawned. */
    void died(Tile at)
    {
        pending = true;
        ticks = 0;
        fellX = at == null ? 0 : at.x;
        fellY = at == null ? 0 : at.y;
    }

    /** Each game tick: starts the carrier at your respawn. True when it starts. */
    boolean tick(NomadSim sim, Tile at, Out out)
    {
        if (!pending || at == null) return false;
        ticks++;
        if (Math.hypot(at.x - fellX, at.y - fellY) < RESPAWN_TILES && ticks < RESPAWN_TICKS) return false;
        pending = false;
        if (sim.bound().contains(at.x, at.y)) return false;
        x = at.x;
        y = at.y;
        left = MINUTES;
        lastAim = -1000;
        out.banner("Redeploy", "A carrier circle rises where you respawned and walks you back. Stay inside it; your circle dashes to meet it.", true);
        return true;
    }

    void minute(NomadSim sim, Out out)
    {
        if (left <= 0) return;
        Circle home = sim.bound();
        double d = Math.hypot(home.x() - x, home.y() - y);
        if (d <= home.radius() + RADIUS - 2)
        {
            left = 0;
            out.say("The carrier reached your circle and fades. You're home.");
            return;
        }
        if (--left == 0)
        {
            out.say("The carrier fades: it couldn't reach your circle in time.");
            return;
        }
        double heading = Math.atan2(home.y() - y, home.x() - x);
        double step = Math.min(SPEED, d);
        // Keeps to land, turning up to a right angle either way to stay on it; waits if it can't.
        for (double off : new double[]{0, 0.5, -0.5, 1.0, -1.0, 1.5, -1.5})
        {
            double nx = x + Math.cos(heading + off) * step;
            double ny = y + Math.sin(heading + off) * step;
            if (Geography.onLand(nx, ny))
            {
                x = nx;
                y = ny;
                break;
            }
        }
        if (sim.minutes() - lastAim >= AIM_EVERY)
        {
            lastAim = sim.minutes();
            sim.steer(sim.boundIndex(), (int) Math.round(x), (int) Math.round(y), NomadSim.DASH, AIM_EVERY + 5);
        }
    }
}
