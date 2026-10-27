package com.wanderingnomad.sim;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/** A kingdom with its own region circle: its land, and the diaries that grow it. */
public final class Region
{
    public final String id;
    public final String name;
    /** The region circle's colour, as 0xRRGGBB. */
    public final int colour;
    /** Diary names as the plugin reads them, such as "Lumbridge" or "Ardougne". */
    public final List<String> diaries;
    /** The kingdom's index in world.dat. */
    final int index;
    private Tile home;

    Region(String id, String name, int colour, int index, String... diaries)
    {
        this.id = id;
        this.name = name;
        this.colour = colour;
        this.index = index;
        this.diaries = Collections.unmodifiableList(Arrays.asList(diaries));
    }

    /** Where the region circle starts: the kingdom's land nearest its middle. */
    public Tile home()
    {
        return home;
    }

    void setHome(Tile home)
    {
        this.home = home;
    }

    /** Land in this kingdom. */
    public boolean contains(double x, double y)
    {
        return Geography.WORLD.kingdom(x, y) == index && Geography.WORLD.land(x, y);
    }

    /** Each diary tier is a step; a region with two diaries takes twice as many to fill. */
    public int maxTiers()
    {
        return 4 * diaries.size();
    }
}
