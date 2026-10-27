package com.wanderingnomad.sim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A kingdom with its own region circle, an outline and the diaries that grow it. */
public final class Region
{
    public final String id;
    public final String name;
    /** The region circle's colour, as 0xRRGGBB. */
    public final int colour;
    /** Diary names as the plugin reads them, such as "Lumbridge" or "Ardougne". */
    public final List<String> diaries;
    /** Where the region circle starts, inside the outline. */
    public final Tile home;
    private final int[][] outline;

    Region(String id, String name, int colour, String[] diaries, Tile home, int[][] outline)
    {
        this.id = id;
        this.name = name;
        this.colour = colour;
        this.diaries = Collections.unmodifiableList(Arrays.asList(diaries));
        this.home = home;
        this.outline = outline;
    }

    public boolean contains(double x, double y)
    {
        boolean inside = false;
        for (int i = 0, j = outline.length - 1; i < outline.length; j = i++)
        {
            double xi = outline[i][0], yi = outline[i][1], xj = outline[j][0], yj = outline[j][1];
            if (((yi > y) != (yj > y)) && (x < (xj - xi) * (y - yi) / (yj - yi) + xi))
            {
                inside = !inside;
            }
        }
        return inside;
    }

    /** Each diary tier is a step; a region with two diaries takes twice as many to fill. */
    public int maxTiers()
    {
        return 4 * diaries.size();
    }
}
