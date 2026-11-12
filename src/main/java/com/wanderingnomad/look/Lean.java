package com.wanderingnomad.look;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;

/**
 * Visible lean: while you stand within the lean band of your circle's edge, a chevron on the edge
 * nearest you shows the circle turning toward you, sized by how much of the turn is still to come.
 */
public final class Lean
{
    private Lean()
    {
    }

    /**
     * The chevron for you at ({@code px}, {@code py}) in circle {@code c}: the edge point (x, y),
     * the outward unit vector (ux, uy), the share of a full minute's turn still to come (0 to 1)
     * and the way it turns (1 anticlockwise, -1 clockwise, 0 none). Null when you're not leaning it.
     */
    public static double[] mark(Circle c, double px, double py)
    {
        double dx = px - c.x();
        double dy = py - c.y();
        double d = Math.hypot(dx, dy);
        if (d < 1e-6 || d > c.radius() || d < c.radius() - NomadSim.LEAN_BAND) return null;
        double diff = Math.atan2(dy, dx) - c.heading();
        diff = Math.atan2(Math.sin(diff), Math.cos(diff));
        double share = Math.min(1, Math.abs(diff) / NomadSim.LEAN);
        double ux = dx / d;
        double uy = dy / d;
        return new double[]{c.x() + ux * c.radius(), c.y() + uy * c.radius(), ux, uy, share, Math.signum(diff)};
    }
}
