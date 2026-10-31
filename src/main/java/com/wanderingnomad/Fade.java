package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import java.awt.Color;

/**
 * The half of a forked Main that didn't survive the parting, caught where it was, so the maps can
 * show it dissolving for a few seconds instead of it vanishing between one tick and the next.
 */
final class Fade
{
    /** How long the dissolve lasts, in real time. */
    static final long MS = 3500;

    final double x;
    final double y;
    final double r;
    final Color colour;
    final long start;

    Fade(Circle c, long start)
    {
        this(c.x(), c.y(), c.radius(), NomadPlugin.colour(c, 255), start);
    }

    private Fade(double x, double y, double r, Color colour, long start)
    {
        this.x = x;
        this.y = y;
        this.r = r;
        this.colour = colour;
        this.start = start;
    }

    /** The same fade, starting again now: for replaying it from the sidebar. */
    Fade again(long now)
    {
        return new Fade(x, y, r, colour, now);
    }

    /** How far through it is, 0 to 1, or more than 1 once it's done. */
    double progress(long now)
    {
        return (now - start) / (double) MS;
    }

    /** How strongly to draw it at {@code progress}: full at first, easing out to nothing. */
    static double strength(double progress)
    {
        double t = Math.max(0, Math.min(1, progress));
        return (1 - t) * (1 - t);
    }

    /** How much bigger it has grown at {@code progress}, as it dissolves outward. */
    static double grow(double progress)
    {
        return 1 + 0.18 * Math.max(0, Math.min(1, progress));
    }
}
