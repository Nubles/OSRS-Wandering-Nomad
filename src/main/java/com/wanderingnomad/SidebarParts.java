package com.wanderingnomad;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.Collections;
import java.util.List;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * The sidebar's drawn pieces. Each one holds the latest {@link NomadPanel.View}, has a fixed size so
 * the sidebar never jumps, and only repaints when it's handed a new view.
 */
final class SidebarParts
{
    /** The width everything in the sidebar is drawn to, inside its 10px borders. */
    static final int WIDTH = NomadPanel.CONTENT_WIDTH;
    static final Color CARD = ColorScheme.DARKER_GRAY_COLOR;
    static final Color MUTED = new Color(0x9A9A9A);
    static final Color RARE = new Color(0xC58CFF);
    static final Color IN = new Color(0x8FE0A2);
    static final Color OUT = new Color(0xFF8A80);

    private SidebarParts()
    {
    }

    /** A drawn piece of the sidebar: fixed size, smooth edges, and the view it last drew. */
    abstract static class Part extends JPanel
    {
        NomadPanel.View view;

        Part(int height)
        {
            Dimension size = new Dimension(WIDTH, height);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
            setOpaque(false);
        }

        void show(NomadPanel.View view)
        {
            this.view = view;
            repaint();
        }

        @Override
        protected final void paintComponent(Graphics g)
        {
            super.paintComponent(g);
            if (view == null) return;
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            try
            {
                paint(g2, view);
            }
            finally
            {
                g2.dispose();
            }
        }

        abstract void paint(Graphics2D g, NomadPanel.View v);
    }

    static void card(Graphics2D g, int x, int y, int w, int h, Color accent)
    {
        g.setColor(CARD);
        g.fill(new RoundRectangle2D.Float(x, y, w, h, 8, 8));
        if (accent != null)
        {
            g.setColor(accent);
            g.fill(new RoundRectangle2D.Float(x, y, 3, h, 3, 3));
        }
    }

    static void text(Graphics2D g, Font font, String s, int x, int y, Color colour)
    {
        g.setFont(font);
        g.setColor(colour);
        g.drawString(s, x, y);
    }

    /** Text cut short with an ellipsis so it fits {@code width}. */
    static String fit(FontMetrics fm, String s, int width)
    {
        if (fm.stringWidth(s) <= width) return s;
        String dots = "…";
        int end = s.length();
        while (end > 0 && fm.stringWidth(s.substring(0, end) + dots) > width) end--;
        return s.substring(0, end) + dots;
    }

    /** A small arrow at (cx, cy) pointing along {@code angle} on the map (east 0, north pi/2). */
    static void arrow(Graphics2D g, float cx, float cy, double angle, float size, Color colour)
    {
        double ux = Math.cos(angle);
        double uy = -Math.sin(angle);
        Path2D p = new Path2D.Double();
        p.moveTo(cx + ux * size, cy + uy * size);
        p.lineTo(cx - ux * size * 0.8 - uy * size * 0.65, cy - uy * size * 0.8 + ux * size * 0.65);
        p.lineTo(cx - ux * size * 0.35, cy - uy * size * 0.35);
        p.lineTo(cx - ux * size * 0.8 + uy * size * 0.65, cy - uy * size * 0.8 - ux * size * 0.65);
        p.closePath();
        g.setColor(colour);
        g.fill(p);
    }

    static void diamond(Graphics2D g, float cx, float cy, float s, Color colour)
    {
        Path2D d = new Path2D.Float();
        d.moveTo(cx, cy - s);
        d.lineTo(cx + s * 0.7f, cy);
        d.lineTo(cx, cy + s);
        d.lineTo(cx - s * 0.7f, cy);
        d.closePath();
        g.setColor(colour);
        g.fill(d);
        g.setStroke(new BasicStroke(1f));
        g.setColor(Color.BLACK);
        g.draw(d);
    }

    /** Your circle's name and whether you're in it, the edge countdown as a ring, and its mood and heading. */
    static final class Header extends Part
    {
        Header()
        {
            super(92);
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            card(g, 0, 0, WIDTH, 92, v.colour);
            if (!v.live)
            {
                text(g, FontManager.getRunescapeBoldFont(), "Nomad UIM", 12, 26, Color.WHITE);
                text(g, FontManager.getRunescapeSmallFont(), "Log in to start or resume your run.", 12, 46, MUTED);
                return;
            }
            // The ring empties as the edge closes in on you; it's full while the edge is more than a quarter hour off.
            int d = 58;
            int rx = WIDTH - d - 10;
            int ry = 17;
            g.setStroke(new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(255, 255, 255, 30));
            g.draw(new Ellipse2D.Float(rx, ry, d, d));
            Color ring = v.outside ? OUT : v.edgeIn > 0 ? NomadStatusOverlay.urgency(v.edgeIn) : IN;
            double share = v.outside ? 1 : v.edgeIn > 0 ? v.edgeIn / (double) com.wanderingnomad.sim.Radar.MINUTES : 1;
            g.setColor(ring);
            g.draw(new Arc2D.Double(rx, ry, d, d, 90, -360 * share, Arc2D.OPEN));
            String big = v.outside ? "OUT" : v.edgeIn > 0 ? v.edgeIn + "m" : "15m+";
            Font bigFont = FontManager.getRunescapeBoldFont();
            FontMetrics fm = g.getFontMetrics(bigFont);
            text(g, bigFont, big, rx + (d - fm.stringWidth(big)) / 2, ry + d / 2 + 4, ring);
            String small = v.outside ? "outside" : "to edge";
            Font smallFont = FontManager.getRunescapeSmallFont();
            FontMetrics sm = g.getFontMetrics(smallFont);
            text(g, smallFont, small, rx + (d - sm.stringWidth(small)) / 2, ry + d / 2 + 15, MUTED);

            int tx = 12;
            int tw = rx - tx - 6;
            Font bold = FontManager.getRunescapeBoldFont();
            text(g, bold, fit(g.getFontMetrics(bold), v.name, tw), tx, 22, v.colour);
            text(g, FontManager.getRunescapeFont(), v.state, tx, 40, v.stateColour);
            arrow(g, tx + 5, 53, v.heading, 6, v.colour);
            text(g, FontManager.getRunescapeSmallFont(), fit(sm, v.mood, tw - 14), tx + 14, 57, Color.WHITE);
            text(g, FontManager.getRunescapeSmallFont(), fit(sm, Math.round(v.radius) + " tiles · played " + v.played, tw), tx, 73, MUTED);
            text(g, FontManager.getRunescapeSmallFont(), fit(sm, v.where, tw), tx, 86, MUTED);
        }
    }

    /**
     * Every circle near you, scaled to fit: yours filled, the rest outlined in their colours, the
     * next hour's Forecast for yours, waystones as diamonds and you as a white dot. Hover a circle
     * for its name.
     */
    static final class MiniMap extends Part
    {
        private static final int H = 176;

        MiniMap()
        {
            super(H);
            setToolTipText("");
        }

        /** Tiles across the map: your circle with room around it, so neighbours show. */
        private static double span(NomadPanel.View v)
        {
            return Math.max(120, v.radius * 2 + 70);
        }

        private double[] centre(NomadPanel.View v)
        {
            return Double.isNaN(v.px) ? new double[]{v.cx, v.cy} : new double[]{v.px, v.py};
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            card(g, 0, 0, WIDTH, H, null);
            if (!v.live) return;
            double[] c = centre(v);
            double scale = (H - 8) / span(v);
            g.clip(new RoundRectangle2D.Float(0, 0, WIDTH, H, 8, 8));
            if (v.cone != null)
            {
                Path2D cone = new Path2D.Double();
                for (int k = 0; k < v.cone.length; k++)
                {
                    double x = WIDTH / 2.0 + (v.cone[k][0] - c[0]) * scale;
                    double y = H / 2.0 - (v.cone[k][1] - c[1]) * scale;
                    if (k == 0) cone.moveTo(x, y);
                    else cone.lineTo(x, y);
                }
                cone.closePath();
                g.setColor(NomadSceneOverlay.alpha(v.colour, 34));
                g.fill(cone);
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g.setColor(NomadSceneOverlay.alpha(v.colour, 140));
                g.draw(cone);
            }
            for (NomadPanel.MapCircle m : v.circles)
            {
                Ellipse2D e = ring(m, c, scale);
                g.setColor(NomadSceneOverlay.alpha(m.colour, m.yours ? 70 : 26));
                g.fill(e);
                g.setStroke(new BasicStroke(m.yours ? 2.2f : m.linked ? 1.6f : 1f));
                g.setColor(NomadSceneOverlay.alpha(m.colour, m.yours ? 255 : m.linked ? 220 : 150));
                g.draw(e);
            }
            for (NomadPanel.MapStone s : v.stones)
            {
                diamond(g, (float) (WIDTH / 2.0 + (s.x - c[0]) * scale), (float) (H / 2.0 - (s.y - c[1]) * scale), 5, s.colour);
            }
            if (!Double.isNaN(v.px))
            {
                float x = (float) (WIDTH / 2.0 + (v.px - c[0]) * scale);
                float y = (float) (H / 2.0 - (v.py - c[1]) * scale);
                g.setColor(Color.BLACK);
                g.fill(new Ellipse2D.Float(x - 4, y - 4, 8, 8));
                g.setColor(Color.WHITE);
                g.fill(new Ellipse2D.Float(x - 3, y - 3, 6, 6));
            }
            text(g, FontManager.getRunescapeSmallFont(), "N", WIDTH - 12, 13, MUTED);
            text(g, FontManager.getRunescapeSmallFont(), Math.round(span(v)) + " tiles across", 6, H - 6, MUTED);
        }

        private static Ellipse2D ring(NomadPanel.MapCircle m, double[] c, double scale)
        {
            double x = WIDTH / 2.0 + (m.x - c[0]) * scale;
            double y = H / 2.0 - (m.y - c[1]) * scale;
            double r = Math.max(3, m.r * scale);
            return new Ellipse2D.Double(x - r, y - r, r * 2, r * 2);
        }

        @Override
        public String getToolTipText(MouseEvent e)
        {
            NomadPanel.View v = view;
            if (v == null || !v.live) return null;
            double[] c = centre(v);
            double scale = (H - 8) / span(v);
            NomadPanel.MapCircle best = null;
            for (NomadPanel.MapCircle m : v.circles)
            {
                if (!ring(m, c, scale).contains(e.getX(), e.getY())) continue;
                if (best == null || m.r < best.r) best = m;
            }
            return best == null ? null : best.name + (best.yours ? " (yours)" : best.linked ? " (you can cross into it)" : "");
        }
    }

    /** One held boon as a card, or an empty slot showing how to earn one. */
    static final class BoonCard extends Part
    {
        static final int H = 44;
        private final NomadPanel.BoonSlot slot;

        BoonCard(NomadPanel.BoonSlot slot, int width)
        {
            super(H);
            this.slot = slot;
            Dimension size = new Dimension(width, H);
            setPreferredSize(size);
            setMinimumSize(size);
            setMaximumSize(size);
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            int w = getWidth();
            if (slot == null)
            {
                g.setColor(new Color(255, 255, 255, 14));
                g.fill(new RoundRectangle2D.Float(0, 0, w, H, 8, 8));
                g.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{3f, 3f}, 0f));
                g.setColor(new Color(255, 255, 255, 50));
                g.draw(new RoundRectangle2D.Float(0.5f, 0.5f, w - 1, H - 1, 8, 8));
                text(g, FontManager.getRunescapeSmallFont(), "Empty slot. Earn one:", 10, 18, MUTED);
                text(g, FontManager.getRunescapeSmallFont(), "100 total level, 25 QP or a first kill", 10, 32, MUTED);
                return;
            }
            Color tone = slot.rare ? RARE : Moments.GOLD;
            card(g, 0, 0, w, H, tone);
            Path2D star = new Path2D.Float();
            for (int k = 0; k < 8; k++)
            {
                double a = -Math.PI / 2 + k * Math.PI / 4;
                double r = k % 2 == 0 ? 9 : 3.5;
                double x = 18 + Math.cos(a) * r;
                double y = H / 2.0 + Math.sin(a) * r;
                if (k == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.closePath();
            g.setColor(tone);
            g.fill(star);
            Font bold = FontManager.getRunescapeBoldFont();
            text(g, bold, slot.name, 34, 18, tone);
            text(g, FontManager.getRunescapeSmallFont(), slot.rare ? "rare" : "common", 34 + g.getFontMetrics(bold).stringWidth(slot.name) + 6, 18, MUTED);
            FontMetrics sm = g.getFontMetrics(FontManager.getRunescapeSmallFont());
            text(g, FontManager.getRunescapeSmallFont(), fit(sm, slot.description, w - 40), 34, 34, Color.LIGHT_GRAY);
        }
    }

    /** Boons working right now, each with a bar of the time it has left. */
    static final class Active extends Part
    {
        private static final int ROW = 22;

        Active()
        {
            super(0);
        }

        @Override
        void show(NomadPanel.View view)
        {
            int h = view.active.isEmpty() ? 0 : view.active.size() * ROW + 4;
            Dimension size = new Dimension(WIDTH, h);
            if (!size.equals(getPreferredSize()))
            {
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(size);
                revalidate();
            }
            super.show(view);
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            int y = 2;
            Font small = FontManager.getRunescapeSmallFont();
            for (NomadPanel.ActiveBoon a : v.active)
            {
                text(g, small, a.name + " active", 2, y + 10, Moments.GOLD);
                String left = NomadPanel.time(a.left) + " left";
                text(g, small, left, WIDTH - g.getFontMetrics(small).stringWidth(left) - 2, y + 10, Color.LIGHT_GRAY);
                g.setColor(new Color(255, 255, 255, 30));
                g.fill(new RoundRectangle2D.Float(2, y + 13, WIDTH - 4, 4, 4, 4));
                g.setColor(Moments.GOLD);
                g.fill(new RoundRectangle2D.Float(2, y + 13, (float) ((WIDTH - 4) * Math.min(1, a.left / (double) a.total)), 4, 4, 4));
                y += ROW;
            }
        }
    }

    /** Risen waystones, nearest first: boon, how far and which way, and how long before they fade. */
    static final class Waystones extends Part
    {
        private static final int ROW = 34;

        Waystones()
        {
            super(ROW);
        }

        @Override
        void show(NomadPanel.View view)
        {
            int rows = Math.max(1, view.waystones.size()) + (view.waystoneNote == null ? 0 : 1);
            Dimension size = new Dimension(WIDTH, rows * ROW);
            if (!size.equals(getPreferredSize()))
            {
                setPreferredSize(size);
                setMinimumSize(size);
                setMaximumSize(size);
                revalidate();
            }
            super.show(view);
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            Font small = FontManager.getRunescapeSmallFont();
            Font bold = FontManager.getRunescapeBoldFont();
            FontMetrics sm = g.getFontMetrics(small);
            int y = 0;
            if (v.waystones.isEmpty())
            {
                card(g, 0, y, WIDTH, ROW - 4, null);
                text(g, small, v.live ? "None risen. Milestones summon them." : "Log in to see your waystones.", 10, y + 19, MUTED);
                y += ROW;
            }
            for (NomadPanel.StoneRow s : v.waystones)
            {
                card(g, 0, y, WIDTH, ROW - 4, s.yours ? Moments.GOLD : s.colour);
                diamond(g, 14, y + 15, 7, s.yours ? Moments.GOLD : s.colour);
                text(g, bold, s.boon, 28, y + 13, s.rare ? RARE : Moments.GOLD);
                String right = s.left;
                text(g, small, right, WIDTH - sm.stringWidth(right) - 8, y + 13, Color.LIGHT_GRAY);
                String where = Double.isNaN(s.angle) ? s.where : s.distance + " tiles · " + s.where;
                text(g, small, fit(sm, where, WIDTH - 50), 28, y + 25, MUTED);
                if (!Double.isNaN(s.angle)) arrow(g, WIDTH - 14, y + 21, s.angle, 5, Color.WHITE);
                y += ROW;
            }
            if (v.waystoneNote != null)
            {
                text(g, small, fit(sm, v.waystoneNote, WIDTH - 10), 4, y + 14, MUTED);
            }
        }
    }

    /** The run's tallies in two columns. */
    static final class Stats extends Part
    {
        private static final int ROW = 16;

        Stats()
        {
            super(ROW * 4 + 10);
        }

        @Override
        void paint(Graphics2D g, NomadPanel.View v)
        {
            card(g, 0, 0, WIDTH, getHeight(), null);
            if (!v.live) return;
            String[][] cells = {
                {"Played", v.played}, {"Walked", v.walked + " tiles"},
                {"Crossings", String.valueOf(v.crossingsMade)}, {"Claimed", String.valueOf(v.claimed)},
                {"Outside", v.timesOutside + (v.timesOutside == 1 ? " time" : " times")}, {"Seed", String.valueOf(v.seed)},
                {"Guard", v.guardShort}, {"Diaries", v.diaryShort},
            };
            Font small = FontManager.getRunescapeSmallFont();
            FontMetrics sm = g.getFontMetrics(small);
            int half = WIDTH / 2;
            for (int k = 0; k < cells.length; k++)
            {
                int x = k % 2 == 0 ? 8 : half + 4;
                int y = 15 + (k / 2) * ROW;
                text(g, small, cells[k][0], x, y, MUTED);
                Color colour = "Guard".equals(cells[k][0]) ? (v.guardClean ? IN : OUT) : Color.WHITE;
                String value = fit(sm, cells[k][1], half - 50);
                text(g, small, value, x + half - 12 - sm.stringWidth(value) - (k % 2 == 0 ? 0 : 4), y, colour);
            }
        }
    }

    /** The run's latest moments, newest first, with the time they happened. */
    static final class Timeline extends JPanel
    {
        private static final int ROW = 30;
        private List<NomadPanel.Moment> moments = Collections.emptyList();

        Timeline()
        {
            setOpaque(true);
            setBackground(CARD);
        }

        void show(List<NomadPanel.Moment> moments)
        {
            if (moments.equals(this.moments)) return;
            this.moments = moments;
            Dimension size = new Dimension(WIDTH - 14, Math.max(1, moments.size()) * ROW);
            setPreferredSize(size);
            revalidate();
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g0)
        {
            super.paintComponent(g0);
            Graphics2D g = (Graphics2D) g0.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Font small = FontManager.getRunescapeSmallFont();
            Font bold = FontManager.getRunescapeBoldFont();
            FontMetrics sm = g.getFontMetrics(small);
            int w = getWidth();
            if (moments.isEmpty())
            {
                text(g, small, "Moments will gather here.", 8, 19, MUTED);
            }
            int y = 0;
            for (NomadPanel.Moment m : moments)
            {
                NomadBannerOverlay.drawIcon(g, m.banner, 6, y + 7, 16, 0);
                String when = NomadPanel.time(m.minute);
                text(g, small, when, w - sm.stringWidth(when) - 6, y + 13, MUTED);
                text(g, bold, fit(g.getFontMetrics(bold), m.banner.title, w - 34 - sm.stringWidth(when) - 8), 28, y + 13, m.banner.colour);
                text(g, small, fit(sm, m.banner.subtitle, w - 34), 28, y + 25, Color.LIGHT_GRAY);
                g.setColor(new Color(255, 255, 255, 14));
                g.drawLine(6, y + ROW - 1, w - 6, y + ROW - 1);
                y += ROW;
            }
            g.dispose();
        }
    }
}
