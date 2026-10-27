package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/** The tiles along a circle's edge: inside it, with at least one side neighbour outside. */
final class EdgeTiles
{
    private EdgeTiles()
    {
    }

    /** Edge tiles within {@code reach} tiles of {@code near} (Chebyshev distance). */
    static List<Tile> near(Circle c, int nearX, int nearY, int reach)
    {
        List<Tile> out = new ArrayList<>();
        int r = (int) Math.ceil(c.radius()) + 1;
        int x0 = Math.max((int) Math.floor(c.x()) - r, nearX - reach);
        int x1 = Math.min((int) Math.ceil(c.x()) + r, nearX + reach);
        int y0 = Math.max((int) Math.floor(c.y()) - r, nearY - reach);
        int y1 = Math.min((int) Math.ceil(c.y()) + r, nearY + reach);
        for (int x = x0; x <= x1; x++)
        {
            for (int y = y0; y <= y1; y++)
            {
                if (!c.contains(x, y)) continue;
                if (!c.contains(x + 1, y) || !c.contains(x - 1, y) || !c.contains(x, y + 1) || !c.contains(x, y - 1))
                {
                    out.add(new Tile(x, y));
                }
            }
        }
        return out;
    }
}
