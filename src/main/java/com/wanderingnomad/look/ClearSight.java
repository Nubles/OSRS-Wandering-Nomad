package com.wanderingnomad.look;

/**
 * Clear sight: one accessibility preset. A colour-blind-safe palette (Okabe and Ito's eight),
 * thick high-contrast edges, a pattern per kind of circle (Main solid, kingdoms dashed, the
 * caravan dotted) and every banner also told in chat.
 */
public final class ClearSight
{
    /** Okabe and Ito's colour-blind-safe colours, without black. */
    static final int[] SAFE = {0xE69F00, 0x56B4E9, 0x009E73, 0xF0E442, 0x0072B2, 0xD55E00, 0xCC79A7, 0xFFFFFF};

    private ClearSight()
    {
    }

    /** A circle's colour in the safe palette: Main always orange, the others by their index. */
    public static int colour(int index, boolean main)
    {
        if (main) return SAFE[0];
        return SAFE[1 + Math.floorMod(index - 1, SAFE.length - 1)];
    }

    /** A dash pattern for a kind of circle: null (solid) for Main, dashes for kingdoms, dots for the caravan. */
    public static float[] pattern(boolean main, boolean caravan, float width)
    {
        if (main) return null;
        return caravan ? new float[]{width, width * 2} : new float[]{width * 4, width * 2.5f};
    }

    /** How much thicker edges are drawn with high contrast on. */
    public static final float THICK = 2.2f;
}
