package com.wanderingnomad.journey;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

/**
 * A run's sigil: Main's whole path drawn small, coloured by act, dashed where it sailed, with a
 * branch wherever a fork parted. Kept on the trophy shelf and saved as a PNG.
 */
public final class Sigil
{
    /** Each act's colour, act I first. */
    static final int[] ACT_COLOURS = {0xE8D9A8, 0x9FE9FF, 0xFFB040, 0xC3A6FF, 0x8CE99A, 0xFF8FA3};

    private Sigil()
    {
    }

    /** An act's colour on the sigil, from 0 for act I. */
    public static Color actColour(int act)
    {
        return new Color(ACT_COLOURS[Math.floorMod(act, ACT_COLOURS.length)]);
    }

    /** Draws a run's sigil {@code size} pixels square; {@code card} puts it on a dark card ringed in the run's colour. */
    public static BufferedImage render(Legacy.Run run, int size, boolean card)
    {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        Color ring = new Color(run.colour & 0xFFFFFF);
        float line = Math.max(1.2f, size / 70f);
        if (card)
        {
            float arc = size / 6f;
            g.setColor(new Color(0x1E1E1E));
            g.fill(new RoundRectangle2D.Float(0, 0, size, size, arc, arc));
            g.setColor(new Color(ring.getRed(), ring.getGreen(), ring.getBlue(), 150));
            g.setStroke(new BasicStroke(line));
            g.draw(new Ellipse2D.Float(size * 0.06f, size * 0.06f, size * 0.88f, size * 0.88f));
        }
        int[] s = run.sigil == null ? new int[0] : run.sigil;
        int n = s.length / 3;
        if (n == 0)
        {
            g.setColor(ring);
            float r = size / 14f;
            g.fill(new Ellipse2D.Float(size / 2f - r, size / 2f - r, 2 * r, 2 * r));
            g.dispose();
            return img;
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (int i = 0; i < n; i++)
        {
            minX = Math.min(minX, s[i * 3]);
            maxX = Math.max(maxX, s[i * 3]);
            minY = Math.min(minY, s[i * 3 + 1]);
            maxY = Math.max(maxY, s[i * 3 + 1]);
        }
        // A short run's path keeps a sensible size rather than filling the card.
        double span = Math.max(64, Math.max(maxX - minX, maxY - minY));
        double inner = size * (card ? 0.62 : 0.86);
        double scale = inner / span;
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        float[] px = new float[n];
        float[] py = new float[n];
        for (int i = 0; i < n; i++)
        {
            px[i] = (float) (size / 2.0 + (s[i * 3] - midX) * scale);
            // North is up on the map, so the tile y runs up the image.
            py[i] = (float) (size / 2.0 - (s[i * 3 + 1] - midY) * scale);
        }
        Stroke solid = new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
        Stroke dashed = new BasicStroke(line, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 1f, new float[]{line * 2.5f, line * 2f}, 0f);
        int i = 1;
        while (i < n)
        {
            int style = style(s[i * 3 + 2]);
            GeneralPath path = new GeneralPath();
            path.moveTo(px[i - 1], py[i - 1]);
            while (i < n && style(s[i * 3 + 2]) == style)
            {
                path.lineTo(px[i], py[i]);
                i++;
            }
            g.setColor(actColour(style >> 1));
            g.setStroke((style & 1) != 0 ? dashed : solid);
            g.draw(path);
        }
        // A fork that parted: a short branch off the path, the half that faded.
        g.setStroke(new BasicStroke(line * 0.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        float branch = Math.max(3, size / 18f);
        for (int k = 1; k < n; k++)
        {
            int flags = s[k * 3 + 2];
            if ((flags & Journey.FORKED) == 0) continue;
            double heading = Math.atan2(py[k] - py[k - 1], px[k] - px[k - 1]);
            if (px[k] == px[k - 1] && py[k] == py[k - 1]) heading = 0;
            double a = heading + Math.PI * 0.75;
            Color c = actColour(flags >> Journey.ACT_SHIFT);
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 150));
            GeneralPath fork = new GeneralPath();
            fork.moveTo(px[k], py[k]);
            fork.lineTo((float) (px[k] + Math.cos(a) * branch), (float) (py[k] + Math.sin(a) * branch));
            g.draw(fork);
        }
        float start = Math.max(2, line * 1.6f);
        g.setStroke(new BasicStroke(line * 0.8f));
        g.setColor(actColour(0));
        g.draw(new Ellipse2D.Float(px[0] - start, py[0] - start, 2 * start, 2 * start));
        float end = Math.max(2.5f, line * 2.2f);
        g.setColor(ring);
        g.fill(new Ellipse2D.Float(px[n - 1] - end, py[n - 1] - end, 2 * end, 2 * end));
        g.dispose();
        return img;
    }

    /** A segment's look: its act above the lowest bit, and whether it was at sea. */
    private static int style(int flags)
    {
        return ((flags >> Journey.ACT_SHIFT) << 1) | (flags & Journey.AT_SEA);
    }
}
