package com.wanderingnomad;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import net.runelite.client.ui.overlay.components.LayoutableRenderableEntity;

/** Small drawn rows for the status box: the drift heading with its mood, and boon chips. */
final class HudParts
{
    private HudParts()
    {
    }

    /** Shared plumbing: where the box puts a row and how wide it may be. */
    private abstract static class Row implements LayoutableRenderableEntity
    {
        final Rectangle bounds = new Rectangle();
        Point location = new Point();
        Dimension size = new Dimension(0, 0);

        @Override
        public Rectangle getBounds()
        {
            return bounds;
        }

        @Override
        public void setPreferredLocation(Point location)
        {
            this.location = location;
        }

        @Override
        public void setPreferredSize(Dimension size)
        {
            this.size = size;
        }
    }

    /** "Drifting" with an arrow turned to the circle's heading (east = 0, north = pi/2) and its mood. */
    static final class Heading extends Row
    {
        private final double heading;
        private final String mood;
        private final Color colour;

        Heading(double heading, String mood, Color colour)
        {
            this.heading = heading;
            this.mood = mood;
            this.colour = colour;
        }

        @Override
        public Dimension render(Graphics2D g)
        {
            FontMetrics fm = g.getFontMetrics();
            int h = Math.max(16, fm.getHeight());
            int w = size.width;
            int x = location.x;
            int y = location.y;
            g.setColor(Color.BLACK);
            g.drawString("Drifting", x + 1, y + fm.getAscent() + 1);
            g.setColor(Color.WHITE);
            g.drawString("Drifting", x, y + fm.getAscent());

            int moodW = fm.stringWidth(mood);
            int ax = x + w - moodW - 16;
            float cx = ax + 6;
            float cy = y + h / 2f;
            double ux = Math.cos(heading);
            double uy = -Math.sin(heading);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Path2D arrow = new Path2D.Double();
            arrow.moveTo(cx + ux * 6, cy + uy * 6);
            arrow.lineTo(cx - ux * 5 - uy * 4, cy - uy * 5 + ux * 4);
            arrow.lineTo(cx - ux * 2, cy - uy * 2);
            arrow.lineTo(cx - ux * 5 + uy * 4, cy - uy * 5 - ux * 4);
            arrow.closePath();
            g.setColor(colour);
            g.fill(arrow);
            g.setStroke(new BasicStroke(1f));
            g.setColor(Color.BLACK);
            g.draw(arrow);

            g.setColor(Color.BLACK);
            g.drawString(mood, x + w - moodW + 1, y + fm.getAscent() + 1);
            g.setColor(colour);
            g.drawString(mood, x + w - moodW, y + fm.getAscent());
            bounds.setBounds(x, y, w, h);
            return new Dimension(w, h);
        }
    }

    /** Active boons as little rounded chips with their time left, wrapping onto a second line if they must. */
    static final class Chips extends Row
    {
        private final List<String> labels = new ArrayList<>();

        void add(String label)
        {
            labels.add(label);
        }

        boolean isEmpty()
        {
            return labels.isEmpty();
        }

        @Override
        public Dimension render(Graphics2D g)
        {
            FontMetrics fm = g.getFontMetrics();
            int w = size.width;
            int chipH = fm.getHeight() + 2;
            int x = location.x;
            int y = location.y;
            int cx = x;
            int cy = y;
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (String label : labels)
            {
                int cw = fm.stringWidth(label) + 10;
                if (cx > x && cx + cw > x + w)
                {
                    cx = x;
                    cy += chipH + 3;
                }
                RoundRectangle2D chip = new RoundRectangle2D.Float(cx, cy, cw, chipH, chipH, chipH);
                g.setColor(new Color(255, 213, 79, 50));
                g.fill(chip);
                g.setStroke(new BasicStroke(1f));
                g.setColor(Moments.GOLD);
                g.draw(chip);
                g.setColor(Color.BLACK);
                g.drawString(label, cx + 6, cy + fm.getAscent() + 2);
                g.setColor(Moments.GOLD);
                g.drawString(label, cx + 5, cy + fm.getAscent() + 1);
                cx += cw + 4;
            }
            int h = cy - y + chipH;
            bounds.setBounds(x, y, w, h);
            return new Dimension(w, h);
        }
    }
}
