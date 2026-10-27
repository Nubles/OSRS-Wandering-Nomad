package com.wanderingnomad.sim;

/** A surface tile in OSRS world coordinates. */
public final class Tile
{
    public final int x;
    public final int y;

    public Tile(int x, int y)
    {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object o)
    {
        return o instanceof Tile && ((Tile) o).x == x && ((Tile) o).y == y;
    }

    @Override
    public int hashCode()
    {
        return x * 31 + y;
    }

    @Override
    public String toString()
    {
        return "(" + x + ", " + y + ")";
    }
}
