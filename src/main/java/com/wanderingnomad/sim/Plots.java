package com.wanderingnomad.sim;

import java.util.ArrayList;
import java.util.List;

/**
 * Plot circles (a house rule): a farming patch planted inside your circle keeps a tiny circle
 * there until it's cleared, for {@link #MINUTES} played minutes at most, {@link #MAX} at once.
 * Walking from your circle into one counts as inside. Saved as x, y, minutes-left triples.
 */
public final class Plots
{
    public static final int RADIUS = 4;
    public static final int MINUTES = 480;
    public static final int MAX = 6;

    private final List<int[]> plots = new ArrayList<>();

    public Plots()
    {
    }

    public Plots(int[] saved)
    {
        if (saved == null) return;
        for (int i = 0; i + 2 < saved.length; i += 3)
        {
            if (saved[i + 2] > 0) plots.add(new int[]{saved[i], saved[i + 1], saved[i + 2]});
        }
    }

    /** The plots as x, y, minutes-left triples, or null when there are none. */
    public int[] save()
    {
        if (plots.isEmpty()) return null;
        int[] out = new int[plots.size() * 3];
        for (int i = 0; i < plots.size(); i++) System.arraycopy(plots.get(i), 0, out, i * 3, 3);
        return out;
    }

    /** A patch planted at a tile: a new plot circle, or a full timer for the one already there. At the cap the oldest goes. */
    public void plant(Tile at)
    {
        int[] p = near(at);
        if (p != null)
        {
            p[2] = MINUTES;
            return;
        }
        if (plots.size() >= MAX)
        {
            int low = 0;
            for (int i = 1; i < plots.size(); i++) if (plots.get(i)[2] < plots.get(low)[2]) low = i;
            plots.remove(low);
        }
        plots.add(new int[]{at.x, at.y, MINUTES});
    }

    /** A patch cleared at a tile: its plot circle goes. True if there was one. */
    public boolean clear(Tile at)
    {
        int[] p = near(at);
        return p != null && plots.remove(p);
    }

    /** One played minute: every plot counts down. Returns how many ran out. */
    public int minute()
    {
        int gone = 0;
        for (int i = plots.size() - 1; i >= 0; i--)
        {
            if (--plots.get(i)[2] <= 0)
            {
                plots.remove(i);
                gone++;
            }
        }
        return gone;
    }

    public boolean contains(double x, double y)
    {
        for (int[] p : plots)
        {
            if (Circle.dist(p[0] - x, p[1] - y) <= RADIUS) return true;
        }
        return false;
    }

    /** Each plot as x, y, minutes left. */
    public List<int[]> all()
    {
        return java.util.Collections.unmodifiableList(plots);
    }

    public int size()
    {
        return plots.size();
    }

    private int[] near(Tile at)
    {
        if (at == null) return null;
        for (int[] p : plots)
        {
            if (Circle.dist(p[0] - at.x, p[1] - at.y) <= RADIUS) return p;
        }
        return null;
    }
}
