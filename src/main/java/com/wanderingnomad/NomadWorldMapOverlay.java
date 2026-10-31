package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Waystone;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Every circle on the world map, filled in its own colour and named, with an arrow showing where
 * it's heading. Yours is boldest, Main is dashed when you're elsewhere, and where a circle you can
 * cross into overlaps yours the overlap glows. Hover a circle for its details; circles you care
 * about that are off the edge of the map get a pointer at the border. Tiles are placed with the
 * same maths RuneLite uses for map pins: the map widget's bounds, the zoom in pixels per tile and
 * the tile at its centre.
 */
class NomadWorldMapOverlay extends Overlay
{
    private static final BasicStroke YOURS = new BasicStroke(3f);
    private static final BasicStroke MAIN = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{10f, 5f}, 0f);
    private static final BasicStroke CONE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{3f, 4f}, 0f);
    private static final BasicStroke REGION = new BasicStroke(2f);
    private static final BasicStroke ARROW = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final BasicStroke THIN = new BasicStroke(1f);
    private static final BasicStroke SHADOW = new BasicStroke(4.5f);
    private static final BasicStroke YOURS_SHADOW = new BasicStroke(6f);
    /** Circles never shrink below this on screen, so a far zoom still shows them. */
    private static final double MIN_RADIUS_PX = 4;

    private final Client client;
    private final NomadPlugin plugin;

    @Inject
    NomadWorldMapOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setPriority(Overlay.PRIORITY_LOW);
        setLayer(OverlayLayer.MANUAL);
        drawAfterInterface(InterfaceID.WORLDMAP);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        if (sim == null || !plugin.config().drawWorldMap()) return null;
        Widget map = client.getWidget(InterfaceID.Worldmap.MAP_CONTAINER);
        WorldMap worldMap = client.getWorldMap();
        if (map == null || map.isHidden() || worldMap == null) return null;
        Rectangle bounds = map.getBounds();
        Point centre = worldMap.getWorldMapPosition();
        float zoom = worldMap.getWorldMapZoom();
        if (bounds == null || centre == null || zoom <= 0) return null;

        int west = centre.getX() - (int) Math.ceil(bounds.getWidth() / zoom) / 2;
        int south = centre.getY() - (int) Math.ceil(bounds.getHeight() / zoom) / 2;
        Shape before = graphics.getClip();
        graphics.clip(bounds);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Focus: a dark veil over the map and its icons, so the circles drawn on top stand out.
        int focus = plugin.config().mapFocus();
        if (focus > 0)
        {
            graphics.setColor(new Color(0, 0, 0, Math.min(255, focus * 255 / 100)));
            graphics.fill(bounds);
        }
        List<Circle> circles = sim.circles();
        Forecast forecast = plugin.forecast();
        if (forecast != null && plugin.config().forecast())
        {
            drawForecast(graphics, forecast, circles, sim.boundIndex(), bounds, west, south, zoom);
        }
        int yours = sim.boundIndex();
        Ellipse2D[] rings = new Ellipse2D[circles.size()];
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            double px = bounds.x + (c.x() - west) * zoom;
            double py = bounds.y + bounds.height - (c.y() - south) * zoom;
            double pr = Math.max(MIN_RADIUS_PX, c.radius() * zoom);
            rings[j] = new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2);
        }
        long now = System.currentTimeMillis();

        // Others first, yours on top.
        for (int j = circles.size() - 1; j >= 0; j--)
        {
            if (j != yours) drawCircle(graphics, circles.get(j), rings[j], false, sim.linked(j));
        }
        // While Main is forked its other half is drawn like it, with a little less weight.
        Circle twin = sim.twin();
        if (twin != null)
        {
            double px = bounds.x + (twin.x() - west) * zoom;
            double py = bounds.y + bounds.height - (twin.y() - south) * zoom;
            double pr = Math.max(MIN_RADIUS_PX, twin.radius() * zoom);
            drawCircle(graphics, twin, new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2), yours == NomadSim.MAIN, sim.linked(NomadSim.MAIN));
        }
        Fade fade = plugin.fade(now);
        if (fade != null)
        {
            double px = bounds.x + (fade.x - west) * zoom;
            double py = bounds.y + bounds.height - (fade.y - south) * zoom;
            drawFade(graphics, fade, px, py, Math.max(MIN_RADIUS_PX, fade.r * zoom), now);
        }
        drawCircle(graphics, circles.get(yours), rings[yours], true, false);
        drawOverlaps(graphics, sim, rings, now);
        if (sim.forkWarning() > 0) drawForkArrows(graphics, sim, rings[NomadSim.MAIN]);

        Font font = graphics.getFont();
        graphics.setFont(FontManager.getRunescapeSmallFont());
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            Ellipse2D ring = rings[j];
            if (!ring.intersects(bounds)) continue;
            String name = j == yours ? c.name() + " (you)" : c.name();
            Color colour = NomadPlugin.colour(c, 255);
            double top = ring.getMinY() - 4;
            if (top < bounds.y + 12) top = ring.getMaxY() + 12;
            pill(graphics, name, (float) ring.getCenterX(), (float) top, colour);
        }
        drawWaystones(graphics, sim, bounds, west, south, zoom);
        if (plugin.spots().on()) drawSpots(graphics, bounds, west, south, zoom);
        drawPointers(graphics, sim, rings, bounds);
        drawHover(graphics, sim, rings, bounds);
        graphics.setFont(font);
        graphics.setClip(before);
        return null;
    }

    /** One circle: a fill in its colour, its rim, and an arrow from the rim along its heading. */
    private static void drawCircle(Graphics2D graphics, Circle c, Ellipse2D ring, boolean yours, boolean linked)
    {
        graphics.setColor(NomadPlugin.colour(c, yours ? 60 : c.isMain() ? 40 : linked ? 34 : 24));
        graphics.fill(ring);
        // A dark band under the rim keeps it readable over busy parts of the map.
        graphics.setStroke(yours ? YOURS_SHADOW : SHADOW);
        graphics.setColor(new Color(0, 0, 0, 150));
        graphics.draw(ring);
        graphics.setStroke(yours ? YOURS : c.isMain() ? MAIN : REGION);
        graphics.setColor(NomadPlugin.colour(c, yours || c.isMain() ? 255 : linked ? 235 : 200));
        graphics.draw(ring);

        double pr = ring.getWidth() / 2;
        double cx = ring.getCenterX();
        double cy = ring.getCenterY();
        double ux = Math.cos(c.heading());
        double uy = -Math.sin(c.heading());
        double len = Math.max(10, Math.min(28, pr * 0.6));
        double sx = cx + ux * pr;
        double sy = cy + uy * pr;
        double ex = sx + ux * len;
        double ey = sy + uy * len;
        graphics.setStroke(ARROW);
        graphics.draw(new Line2D.Double(sx, sy, ex, ey));
        Path2D.Double head = new Path2D.Double();
        head.moveTo(ex + ux * 6, ey + uy * 6);
        head.lineTo(ex - uy * 5, ey + ux * 5);
        head.lineTo(ex + uy * 5, ey - ux * 5);
        head.closePath();
        graphics.fill(head);
    }

    /**
     * The half that didn't survive a parting, dissolving: its ring swells and thins as it fades,
     * and breaks into motes that drift outward and wink out. Also drawn by the minimap and sidebar.
     */
    static void drawFade(Graphics2D graphics, Fade fade, double cx, double cy, double radius, long now)
    {
        double t = Math.max(0, Math.min(1, fade.progress(now)));
        double s = Fade.strength(t);
        if (s <= 0.01) return;
        double r = radius * Fade.grow(t);
        Ellipse2D ring = new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2);
        graphics.setColor(NomadSceneOverlay.alpha(fade.colour, (int) (50 * s)));
        graphics.fill(ring);
        graphics.setStroke(new BasicStroke((float) Math.max(0.6, 2.5 * s), BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND,
            10f, new float[]{(float) (4 + 10 * t), (float) (3 + 9 * t)}, (float) (t * 40)));
        graphics.setColor(NomadSceneOverlay.alpha(fade.colour, (int) (230 * s)));
        graphics.draw(ring);
        // Motes leave the rim and drift outward as it goes.
        int motes = 16;
        for (int k = 0; k < motes; k++)
        {
            double a = k * Math.PI * 2 / motes + t * 0.6;
            double d = r * (1 + 0.35 * t) + (k % 3) * 3;
            double size = 2 + 2.5 * (1 - t);
            graphics.setColor(NomadSceneOverlay.alpha(k % 2 == 0 ? Color.WHITE : fade.colour, (int) (200 * s)));
            graphics.fill(new Ellipse2D.Double(cx + Math.cos(a) * d - size / 2, cy - Math.sin(a) * d - size / 2, size, size));
        }
    }

    /**
     * While a fork is coming: two arrows from Main's centre showing the ways its halves will go,
     * each labelled with its compass direction and the minutes left.
     */
    private static void drawForkArrows(Graphics2D graphics, NomadSim sim, Ellipse2D main)
    {
        double cx = main.getCenterX();
        double cy = main.getCenterY();
        double len = Math.max(24, main.getWidth() / 2 + 18);
        for (double heading : new double[]{sim.forkAxis(), sim.forkAxis() + Math.PI})
        {
            double ux = Math.cos(heading);
            double uy = -Math.sin(heading);
            double ex = cx + ux * len;
            double ey = cy + uy * len;
            graphics.setStroke(new BasicStroke(5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(new Color(0, 0, 0, 150));
            graphics.draw(new Line2D.Double(cx, cy, ex, ey));
            graphics.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(NomadStatusOverlay.FORK);
            graphics.draw(new Line2D.Double(cx, cy, ex, ey));
            Path2D head = new Path2D.Double();
            head.moveTo(ex + ux * 9, ey + uy * 9);
            head.lineTo(ex - uy * 6, ey + ux * 6);
            head.lineTo(ex + uy * 6, ey - ux * 6);
            head.closePath();
            graphics.fill(head);
            outlined(graphics, NomadSim.compass(heading), (float) (ex + ux * 20), (float) (ey + uy * 20 + 4), NomadStatusOverlay.FORK, true);
        }
        outlined(graphics, "Fork in " + sim.forkWarning() + " min", (float) cx, (float) (cy - 6), NomadStatusOverlay.FORK, true);
    }

    /** Where a circle you can cross into overlaps yours, the overlap glows and pulses: that's the crossing. */
    private static void drawOverlaps(Graphics2D graphics, NomadSim sim, Ellipse2D[] rings, long now)
    {
        int yours = sim.boundIndex();
        double pulse = 0.5 + 0.5 * Math.sin(now / 350.0);
        for (int j = 0; j < rings.length; j++)
        {
            if (!sim.linked(j) || !sim.bound().touches(sim.circles().get(j))) continue;
            Area lens = new Area(rings[yours]);
            lens.intersect(new Area(rings[j]));
            if (lens.isEmpty()) continue;
            Color colour = NomadPlugin.colour(sim.circles().get(j), 255);
            graphics.setColor(NomadSceneOverlay.alpha(colour, (int) (90 + 90 * pulse)));
            graphics.fill(lens);
            graphics.setStroke(THIN);
            graphics.setColor(NomadSceneOverlay.alpha(Color.WHITE, (int) (140 + 100 * pulse)));
            graphics.draw(lens);
        }
    }

    /**
     * Your circle and Main, when they're off the part of the map you're looking at, get a pointer
     * at the map's border saying which way they are.
     */
    private static void drawPointers(Graphics2D graphics, NomadSim sim, Ellipse2D[] rings, Rectangle bounds)
    {
        int[] watch = sim.boundIndex() == NomadSim.MAIN ? new int[]{NomadSim.MAIN} : new int[]{sim.boundIndex(), NomadSim.MAIN};
        double mx = bounds.getCenterX();
        double my = bounds.getCenterY();
        for (int j : watch)
        {
            Ellipse2D ring = rings[j];
            if (ring.intersects(bounds)) continue;
            double dx = ring.getCenterX() - mx;
            double dy = ring.getCenterY() - my;
            double d = Math.hypot(dx, dy);
            if (d < 1) continue;
            double ux = dx / d;
            double uy = dy / d;
            // Walk out from the middle to just inside the border.
            double t = Math.min((bounds.width / 2.0 - 26) / Math.max(1e-6, Math.abs(ux)),
                (bounds.height / 2.0 - 26) / Math.max(1e-6, Math.abs(uy)));
            double tx = mx + ux * t;
            double ty = my + uy * t;
            Circle c = sim.circles().get(j);
            Color colour = NomadPlugin.colour(c, 255);
            Path2D.Double arrow = new Path2D.Double();
            arrow.moveTo(tx + ux * 14, ty + uy * 14);
            arrow.lineTo(tx - uy * 9 - ux * 4, ty + ux * 9 - uy * 4);
            arrow.lineTo(tx + uy * 9 - ux * 4, ty - ux * 9 - uy * 4);
            arrow.closePath();
            graphics.setColor(colour);
            graphics.fill(arrow);
            graphics.setStroke(THIN);
            graphics.setColor(Color.BLACK);
            graphics.draw(arrow);
            String name = j == sim.boundIndex() ? c.name() + " (you)" : c.name();
            outlined(graphics, name, (float) (tx - ux * 22), (float) (ty - uy * 22 + 4), colour, true);
        }
    }

    /**
     * Testing: every tile a waystone can rise on, as a small diamond (green open, red blocked,
     * blue not checked yet), with its name beside the one under the mouse.
     */
    private void drawSpots(Graphics2D graphics, Rectangle bounds, int west, int south, float zoom)
    {
        Point mouse = client.getMouseCanvasPosition();
        Landmarks.Landmark hovered = null;
        double hx = 0;
        double hy = 0;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            double px = bounds.x + (l.x - west) * zoom;
            double py = bounds.y + bounds.height - (l.y - south) * zoom;
            if (!bounds.contains(px, py)) continue;
            Path2D.Double d = new Path2D.Double();
            d.moveTo(px, py - 5);
            d.lineTo(px + 4, py);
            d.lineTo(px, py + 5);
            d.lineTo(px - 4, py);
            d.closePath();
            graphics.setColor(plugin.spots().colour(l));
            graphics.fill(d);
            graphics.setStroke(THIN);
            graphics.setColor(Color.BLACK);
            graphics.draw(d);
            if (mouse != null && Math.hypot(mouse.getX() - px, mouse.getY() - py) < 8)
            {
                hovered = l;
                hx = px;
                hy = py;
            }
        }
        if (hovered != null)
        {
            String text = hovered.name + " (" + hovered.x + ", " + hovered.y + ")" + (plugin.spots().isBlocked(hovered) ? " blocked" : "");
            outlined(graphics, text, (float) hx + 8, (float) hy + 4, plugin.spots().colour(hovered), false);
        }
    }

    /** Hovering a circle shows what it's doing: size, mood, how far it is from you and whether you can cross. */
    private void drawHover(Graphics2D graphics, NomadSim sim, Ellipse2D[] rings, Rectangle bounds)
    {
        Point mouse = client.getMouseCanvasPosition();
        if (mouse == null || !bounds.contains(mouse.getX(), mouse.getY())) return;
        int pick = -1;
        for (int j = 0; j < rings.length; j++)
        {
            if (!rings[j].contains(mouse.getX(), mouse.getY())) continue;
            if (pick < 0 || rings[j].getWidth() < rings[pick].getWidth()) pick = j;
        }
        if (pick < 0) return;
        Circle c = sim.circles().get(pick);
        List<String> lines = new ArrayList<>();
        lines.add(c.name() + (pick == sim.boundIndex() ? " · your circle" : c.isMain() ? " · Main" : ""));
        lines.add("Radius " + Math.round(c.radius()) + " tiles");
        lines.add("Mood: " + c.moodName().toLowerCase() + (c.sailing() ? ", at sea to " + c.voyageTo() : ""));
        String effects = NomadPanel.effects(c);
        if (!effects.isEmpty()) lines.add(effects.substring("<br>".length()));
        Player player = client.getLocalPlayer();
        WorldPoint at = player == null ? null : player.getWorldLocation();
        if (at != null)
        {
            double d = Math.hypot(c.x() - at.getX(), c.y() - at.getY()) - c.radius();
            lines.add(d <= 0 ? "You're inside it" : Math.round(d) + " tiles from its edge");
        }
        if (sim.linked(pick))
        {
            Radar radar = plugin.radar();
            int in = radar == null || pick >= radar.crossingIn.length ? -1 : radar.crossingIn[pick];
            double gap = sim.bound().gap(c);
            lines.add(gap <= 0 ? "Touching yours: you can cross in the overlap"
                : in > 0 ? "Touches yours in about " + in + " min"
                : Math.round(gap) + " tiles from yours");
        }
        else if (pick != sim.boundIndex())
        {
            lines.add("Reach it through Main");
        }

        FontMetrics fm = graphics.getFontMetrics();
        int w = 0;
        for (String l : lines) w = Math.max(w, fm.stringWidth(l));
        int lineH = fm.getHeight();
        int boxW = w + 14;
        int boxH = lines.size() * lineH + 10;
        int x = Math.min(mouse.getX() + 14, bounds.x + bounds.width - boxW - 4);
        int y = Math.min(mouse.getY() + 14, bounds.y + bounds.height - boxH - 4);
        graphics.setColor(new Color(30, 27, 22, 225));
        graphics.fillRect(x, y, boxW, boxH);
        graphics.setStroke(THIN);
        graphics.setColor(NomadPlugin.colour(c, 255));
        graphics.drawRect(x, y, boxW, boxH);
        for (int k = 0; k < lines.size(); k++)
        {
            Color colour = k == 0 ? NomadPlugin.colour(c, 255) : Color.WHITE;
            outlined(graphics, lines.get(k), x + 7, y + 5 + (k + 1) * lineH - fm.getDescent(), colour, false);
        }
    }

    /** A name on a dark rounded tag edged in the circle's colour, so it can't be mistaken for a map label. */
    private static void pill(Graphics2D graphics, String text, float cx, float baseline, Color colour)
    {
        FontMetrics fm = graphics.getFontMetrics();
        int w = fm.stringWidth(text) + 10;
        int h = fm.getAscent() + 5;
        int x = Math.round(cx - w / 2f);
        int y = Math.round(baseline - fm.getAscent() - 2);
        graphics.setColor(new Color(20, 18, 14, 215));
        graphics.fillRoundRect(x, y, w, h, 8, 8);
        graphics.setStroke(THIN);
        graphics.setColor(colour);
        graphics.drawRoundRect(x, y, w, h, 8, 8);
        graphics.drawString(text, x + 5, Math.round(baseline));
    }

    /** Text with a dark outline, readable on any part of the map. */
    private static void outlined(Graphics2D graphics, String text, float x, float y, Color colour, boolean centred)
    {
        if (centred) x -= graphics.getFontMetrics().stringWidth(text) / 2f;
        graphics.setColor(Color.BLACK);
        graphics.drawString(text, x + 1, y + 1);
        graphics.drawString(text, x - 1, y + 1);
        graphics.drawString(text, x + 1, y - 1);
        graphics.drawString(text, x - 1, y - 1);
        graphics.setColor(colour);
        graphics.drawString(text, x, y);
    }

    /**
     * The Forecast cone, as the website draws it: for Main and your circle, the hull around where
     * the imagined futures put it at 1, 3 and 6 hours, fainter further out.
     */
    private static void drawForecast(Graphics2D graphics, Forecast forecast, List<Circle> circles, int yours,
        Rectangle bounds, int west, int south, float zoom)
    {
        int[] watch = yours == NomadSim.MAIN ? new int[]{NomadSim.MAIN} : new int[]{NomadSim.MAIN, yours};
        for (int h = 0; h < Forecast.HORIZONS.length; h++)
        {
            for (int i : watch)
            {
                if (i >= circles.size()) continue;
                double[] p = forecast.points(h, i);
                double[][] screen = new double[p.length / 2][];
                for (int k = 0; k < screen.length; k++)
                {
                    screen[k] = new double[]{bounds.x + (p[k * 2] - west) * zoom, bounds.y + bounds.height - (p[k * 2 + 1] - south) * zoom};
                }
                List<double[]> hull = hull(screen);
                if (hull.size() < 3) continue;
                Path2D.Double shape = new Path2D.Double();
                double[] top = hull.get(0);
                for (int k = 0; k < hull.size(); k++)
                {
                    double[] q = hull.get(k);
                    if (k == 0) shape.moveTo(q[0], q[1]);
                    else shape.lineTo(q[0], q[1]);
                    if (q[1] < top[1]) top = q;
                }
                shape.closePath();
                Circle c = circles.get(i);
                graphics.setColor(NomadPlugin.colour(c, (int) (255 * (0.13 - h * 0.03))));
                graphics.fill(shape);
                graphics.setStroke(CONE);
                graphics.setColor(NomadPlugin.colour(c, 150));
                graphics.draw(shape);
                graphics.drawString(Forecast.HORIZONS[h] / 60 + "h", (float) top[0] - 6, (float) top[1] - 4);
            }
        }
    }

    /**
     * Each waystone as a diamond, gold if it's in your circle, with its boon and roughly how long
     * before its circle drifts off it, so you can judge whether the walk is worth it.
     */
    private static void drawWaystones(Graphics2D graphics, NomadSim sim, Rectangle bounds, int west, int south, float zoom)
    {
        for (Waystone w : sim.waystones())
        {
            double px = bounds.x + (w.x() - west) * zoom;
            double py = bounds.y + bounds.height - (w.y() - south) * zoom;
            boolean yours = w.circle() == sim.boundIndex();
            Color colour = yours ? new Color(0xFFD54F) : NomadPlugin.colour(sim.circles().get(w.circle()), 255);
            Path2D.Double diamond = new Path2D.Double();
            diamond.moveTo(px, py - 7);
            diamond.lineTo(px + 5, py);
            diamond.lineTo(px, py + 7);
            diamond.lineTo(px - 5, py);
            diamond.closePath();
            graphics.setColor(colour);
            graphics.fill(diamond);
            graphics.setStroke(new BasicStroke(1f));
            graphics.setColor(Color.BLACK);
            graphics.draw(diamond);
            String text = w.boon() + " ~" + NomadPanel.roughly(sim.minutesLeft(w));
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, (float) px + 8, (float) py + 5);
            graphics.setColor(colour);
            graphics.drawString(text, (float) px + 7, (float) py + 4);
        }
    }

    /** The convex hull of points, by Andrew's monotone chain. */
    static List<double[]> hull(double[][] pts)
    {
        double[][] s = pts.clone();
        Arrays.sort(s, (a, b) -> a[0] != b[0] ? Double.compare(a[0], b[0]) : Double.compare(a[1], b[1]));
        List<double[]> out = new ArrayList<>();
        for (int pass = 0; pass < 2; pass++)
        {
            int start = out.size();
            for (int k = 0; k < s.length; k++)
            {
                double[] q = pass == 0 ? s[k] : s[s.length - 1 - k];
                while (out.size() >= start + 2 && cross(out.get(out.size() - 2), out.get(out.size() - 1), q) <= 0)
                {
                    out.remove(out.size() - 1);
                }
                out.add(q);
            }
            out.remove(out.size() - 1);
        }
        return out;
    }

    private static double cross(double[] o, double[] a, double[] b)
    {
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0]);
    }
}
