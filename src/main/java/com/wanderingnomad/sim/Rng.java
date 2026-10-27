package com.wanderingnomad.sim;

/**
 * Mulberry32: a tiny seeded generator whose whole state is one int, so a run can be saved and
 * resumed exactly. It gives the same numbers as the web mock-up's generator for the same seed.
 */
public final class Rng
{
    private int state;

    public Rng(int seed)
    {
        state = seed;
    }

    /** A number in [0, 1). */
    public double next()
    {
        state += 0x6D2B79F5;
        int t = state;
        t = (t ^ (t >>> 15)) * (t | 1);
        t ^= t + (t ^ (t >>> 7)) * (t | 61);
        return ((t ^ (t >>> 14)) & 0xFFFFFFFFL) / 4294967296.0;
    }

    public int state()
    {
        return state;
    }
}
