package com.wanderingnomad.sim;

import java.util.HashMap;
import java.util.Map;

/**
 * The walk over land to each port: for a port, every land cell holds how many cells of walking it
 * is to the port's cell, so a circle bound for it can follow the way in around coasts and coves.
 * Worked out the first time a port is asked about. Mirrors harbourField in geography.ts.
 */
final class Harbours
{
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};

    private final World w;
    private final Map<String, short[]> fields = new HashMap<>();

    Harbours(World w)
    {
        this.w = w;
    }

    /** Cells of walking from a tile to the port, or -1 if no walk over land leads there. */
    synchronized int steps(Geography.Port p, double x, double y)
    {
        int i = index(x, y);
        return i < 0 ? -1 : field(p)[i];
    }

    /**
     * Where to head for next on the shortest walk to the port: the furthest of the next few cells
     * along it that can be reached in a straight line over land. Null if no walk leads there.
     */
    synchronized double[] wayTo(Geography.Port p, double x, double y, int lookahead)
    {
        int cols = w.cols();
        int i = index(x, y);
        short[] field = field(p);
        if (i < 0 || field[i] < 0) return null;
        int row = i / cols;
        int col = i - row * cols;
        double[] best = centre(col, row);
        for (int step = 0; step < lookahead; step++)
        {
            int here = field[row * cols + col];
            if (here == 0) break;
            for (int[] n : NEIGHBOURS)
            {
                int c = col + n[0];
                int r = row + n[1];
                if (c < 0 || r < 0 || c >= cols || r >= w.rows()) continue;
                int there = field[r * cols + c];
                if (there >= 0 && there < here)
                {
                    col = c;
                    row = r;
                    break;
                }
            }
            double[] next = centre(col, row);
            if (!inSight(x, y, next[0], next[1])) break;
            best = next;
        }
        return best;
    }

    /**
     * The middle of the cell on the walk to the port nearest a tile: its own cell or one beside it.
     * Null if none of them is on the walk.
     */
    synchronized double[] snap(Geography.Port p, double x, double y)
    {
        int cols = w.cols();
        int i = index(x, y);
        if (i < 0) return null;
        short[] field = field(p);
        int row = i / cols;
        int col = i - row * cols;
        double[] best = null;
        double bestD = 0;
        for (int dr = -1; dr <= 1; dr++)
        {
            for (int dc = -1; dc <= 1; dc++)
            {
                int c = col + dc;
                int r = row + dr;
                if (c < 0 || r < 0 || c >= cols || r >= w.rows() || field[r * cols + c] < 0) continue;
                double[] at = centre(c, r);
                double d = Circle.dist(at[0] - x, at[1] - y);
                if (best == null || d < bestD)
                {
                    best = at;
                    bestD = d;
                }
            }
        }
        return best;
    }

    private double[] centre(int col, int row)
    {
        return new double[] {w.x0() + (col + 0.5) * w.cell(), w.y0() + (row + 0.5) * w.cell()};
    }

    /** Whether the straight line between two points stays over land, checked every half tile. */
    private boolean inSight(double ax, double ay, double bx, double by)
    {
        int n = (int) Math.ceil(Circle.dist(bx - ax, by - ay) * 2);
        for (int k = 1; k <= n; k++)
        {
            if (!w.land(ax + (bx - ax) * k / n, ay + (by - ay) * k / n)) return false;
        }
        return true;
    }

    private int index(double x, double y)
    {
        int col = (int) Math.floor((x - w.x0()) / w.cell());
        int row = (int) Math.floor((y - w.y0()) / w.cell());
        if (col < 0 || row < 0 || col >= w.cols() || row >= w.rows()) return -1;
        return row * w.cols() + col;
    }

    private short[] field(Geography.Port p)
    {
        short[] f = fields.get(p.name);
        if (f == null)
        {
            f = build(p.tile);
            fields.put(p.name, f);
        }
        return f;
    }

    private short[] build(Tile t)
    {
        int cols = w.cols();
        int rows = w.rows();
        short[] field = new short[cols * rows];
        java.util.Arrays.fill(field, (short) -1);
        int start = index(t.x, t.y);
        if (start < 0 || !land(start)) return field;
        int[] queue = new int[cols * rows];
        int head = 0;
        int tail = 0;
        field[start] = 0;
        queue[tail++] = start;
        while (head < tail)
        {
            int i = queue[head++];
            if (field[i] >= Short.MAX_VALUE) continue;
            int r = i / cols;
            int c = i - r * cols;
            for (int[] n : NEIGHBOURS)
            {
                int cc = c + n[0];
                int rr = r + n[1];
                if (cc < 0 || rr < 0 || cc >= cols || rr >= rows) continue;
                int j = rr * cols + cc;
                if (field[j] >= 0 || !land(j)) continue;
                field[j] = (short) (field[i] + 1);
                queue[tail++] = j;
            }
        }
        return field;
    }

    private boolean land(int i)
    {
        int r = i / w.cols();
        int c = i - r * w.cols();
        return w.land(w.x0() + (c + 0.5) * w.cell(), w.y0() + (r + 0.5) * w.cell());
    }
}
