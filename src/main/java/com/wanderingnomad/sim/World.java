package com.wanderingnomad.sim;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.zip.GZIPInputStream;

/**
 * Where the land is and which kingdom each part of it belongs to, from world.dat (built by
 * scripts/build_world.py from the rendered game map and the kingdom chunk table). Land is kept in
 * cells of a few tiles; kingdoms by 64-tile map chunk.
 */
final class World
{
    private final int cell;
    private final int x0;
    private final int y0;
    private final int cols;
    private final int rows;
    private final byte[] land;
    private final int chunkX0;
    private final int chunkY0;
    private final int chunkCols;
    private final int chunkRows;
    private final byte[] kingdoms;

    private World(DataInputStream in) throws IOException
    {
        byte[] magic = new byte[4];
        in.readFully(magic);
        if (magic[0] != 'N' || magic[1] != 'M' || magic[2] != 'W' || magic[3] != '1')
        {
            throw new IOException("world.dat has the wrong header");
        }
        cell = in.readInt();
        x0 = in.readInt();
        y0 = in.readInt();
        cols = in.readInt();
        rows = in.readInt();
        land = new byte[(cols * rows + 7) / 8];
        in.readFully(land);
        chunkX0 = in.readInt();
        chunkY0 = in.readInt();
        chunkCols = in.readInt();
        chunkRows = in.readInt();
        kingdoms = new byte[chunkCols * chunkRows];
        in.readFully(kingdoms);
    }

    static World load()
    {
        try (InputStream raw = World.class.getResourceAsStream("world.dat"))
        {
            if (raw == null) throw new IllegalStateException("world.dat is missing from the jar");
            return new World(new DataInputStream(new GZIPInputStream(raw)));
        }
        catch (IOException e)
        {
            throw new UncheckedIOException(e);
        }
    }

    boolean land(double x, double y)
    {
        int col = (int) Math.floor((x - x0) / cell);
        int row = (int) Math.floor((y - y0) / cell);
        if (col < 0 || row < 0 || col >= cols || row >= rows) return false;
        int bit = row * cols + col;
        return (land[bit >> 3] & (0x80 >> (bit & 7))) != 0;
    }

    /** The kingdom index (in the builder's order) of the chunk holding a tile, or -1. */
    int kingdom(double x, double y)
    {
        int cx = (int) Math.floor(x / 64) - chunkX0;
        int cy = (int) Math.floor(y / 64) - chunkY0;
        if (cx < 0 || cy < 0 || cx >= chunkCols || cy >= chunkRows) return -1;
        return (kingdoms[cy * chunkCols + cx] & 0xFF) - 1;
    }

    int cell()
    {
        return cell;
    }

    int x0()
    {
        return x0;
    }

    int y0()
    {
        return y0;
    }

    int cols()
    {
        return cols;
    }

    int rows()
    {
        return rows;
    }
}
