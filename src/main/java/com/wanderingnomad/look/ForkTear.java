package com.wanderingnomad.look;

/**
 * The fork tear: during a fork's warning, a glowing crack spreads across Main where it will split,
 * from the middle outward, reaching the edge as the warning runs out. The split is across the
 * fork's axis (the halves part along it).
 */
public final class ForkTear
{
    private ForkTear()
    {
    }

    /** How far the tear has spread, 0 to 1, with {@code left} of {@code warning} minutes to go. */
    public static double progress(int left, int warning)
    {
        if (warning <= 0) return 0;
        return Math.max(0.08, Math.min(1, 1 - (left - 1) / (double) warning));
    }

    /** The tear's points, a jagged line through the middle across {@code axis}, {@code progress} of the way to the edge. */
    public static double[][] crack(double cx, double cy, double r, double axis, double progress, int seed)
    {
        double across = axis + Math.PI / 2;
        double ux = Math.cos(across);
        double uy = Math.sin(across);
        double nx = -uy;
        double ny = ux;
        double half = r * Math.max(0, Math.min(1, progress));
        int steps = Math.max(2, (int) Math.ceil(half * 2 / 1.5));
        double[][] out = new double[steps + 1][];
        for (int i = 0; i <= steps; i++)
        {
            double t = -half + i * (half * 2 / steps);
            // A small jitter either side, the same every frame for the same fork.
            double jitter = i == 0 || i == steps ? 0 : (hash(seed * 31 + i) - 0.5) * 1.2;
            out[i] = new double[]{cx + ux * t + nx * jitter, cy + uy * t + ny * jitter};
        }
        return out;
    }

    static double hash(int v)
    {
        int h = v * 0x45D9F3B;
        h ^= h >>> 16;
        h *= 0x45D9F3B;
        h ^= h >>> 16;
        return (h & 0xFFFF) / 65535.0;
    }
}
