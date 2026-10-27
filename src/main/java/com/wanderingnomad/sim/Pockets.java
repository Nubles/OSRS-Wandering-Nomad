package com.wanderingnomad.sim;

/**
 * Pockets of land: peninsulas and coves behind a narrow neck, where a circle could bounce around
 * for days. Each land cell holds how many cells of walking it is out to open land, where open land
 * is a cell with at least 70% land within 32 tiles of it. Mirrors pocketField in geography.ts.
 */
final class Pockets
{
    private static final int[][] NEIGHBOURS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int OPEN_RADIUS = 8;
    private static final double OPEN_SHARE = 0.7;

    private final World w;
    /** 0 in open land, -1 at sea or where no walk leads out (an island). */
    private final int[] field;

    Pockets(World w)
    {
        this.w = w;
        int cols = w.cols();
        int rows = w.rows();
        byte[] land = new byte[cols * rows];
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                land[r * cols + c] = (byte) (w.land(w.x0() + (c + 0.5) * w.cell(), w.y0() + (r + 0.5) * w.cell()) ? 1 : 0);
            }
        }
        int[] sat = new int[(cols + 1) * (rows + 1)];
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                sat[(r + 1) * (cols + 1) + c + 1] = land[r * cols + c] + sat[r * (cols + 1) + c + 1]
                    + sat[(r + 1) * (cols + 1) + c] - sat[r * (cols + 1) + c];
            }
        }
        int window = (2 * OPEN_RADIUS + 1) * (2 * OPEN_RADIUS + 1);
        field = new int[cols * rows];
        java.util.Arrays.fill(field, -1);
        int[] queue = new int[cols * rows];
        int head = 0;
        int tail = 0;
        for (int r = 0; r < rows; r++)
        {
            for (int c = 0; c < cols; c++)
            {
                if (land[r * cols + c] == 0) continue;
                int c0 = Math.max(0, c - OPEN_RADIUS);
                int r0 = Math.max(0, r - OPEN_RADIUS);
                int c1 = Math.min(cols, c + OPEN_RADIUS + 1);
                int r1 = Math.min(rows, r + OPEN_RADIUS + 1);
                int count = sat[r1 * (cols + 1) + c1] - sat[r0 * (cols + 1) + c1] - sat[r1 * (cols + 1) + c0] + sat[r0 * (cols + 1) + c0];
                if (count >= window * OPEN_SHARE)
                {
                    field[r * cols + c] = 0;
                    queue[tail++] = r * cols + c;
                }
            }
        }
        while (head < tail)
        {
            int i = queue[head++];
            int r = i / cols;
            int c = i - r * cols;
            for (int[] n : NEIGHBOURS)
            {
                int cc = c + n[0];
                int rr = r + n[1];
                if (cc < 0 || rr < 0 || cc >= cols || rr >= rows) continue;
                int j = rr * cols + cc;
                if (land[j] == 0 || field[j] >= 0) continue;
                field[j] = field[i] + 1;
                queue[tail++] = j;
            }
        }
    }

    int depth(double x, double y)
    {
        int col = (int) Math.floor((x - w.x0()) / w.cell());
        int row = (int) Math.floor((y - w.y0()) / w.cell());
        if (col < 0 || row < 0 || col >= w.cols() || row >= w.rows()) return -1;
        return field[row * w.cols() + col];
    }

    /** A point a few cells along the shortest walk out to open land, or null outside a pocket. */
    double[] wayOut(double x, double y, int lookahead)
    {
        int cols = w.cols();
        int col = (int) Math.floor((x - w.x0()) / w.cell());
        int row = (int) Math.floor((y - w.y0()) / w.cell());
        if (col < 0 || row < 0 || col >= cols || row >= w.rows() || field[row * cols + col] <= 0) return null;
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
        }
        return new double[] {w.x0() + (col + 0.5) * w.cell(), w.y0() + (row + 0.5) * w.cell()};
    }
}
