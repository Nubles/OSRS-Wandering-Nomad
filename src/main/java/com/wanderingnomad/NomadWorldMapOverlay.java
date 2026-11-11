package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Comet;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
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
        if (plugin.config().footprint()) drawFootprint(graphics, sim, bounds, west, south, zoom);
        List<Circle> circles = sim.circles();
        Forecast forecast = plugin.forecast();
        if (forecast != null && plugin.config().forecast())
        {
            drawForecast(graphics, forecast, circles, sim.boundIndex(), bounds, west, south, zoom);
        }
        int yours = sim.boundIndex();
        drawWakes(graphics, sim, bounds, west, south, zoom);
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
        // An open rift's far half is Main too, drawn like it with a label saying how long it stays open.
        Circle rift = sim.rift();
        if (rift != null)
        {
            double px = bounds.x + (rift.x() - west) * zoom;
            double py = bounds.y + bounds.height - (rift.y() - south) * zoom;
            double pr = Math.max(MIN_RADIUS_PX, rift.radius() * zoom);
            Ellipse2D ring = new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2);
            drawCircle(graphics, rift, ring, yours == NomadSim.MAIN, sim.linked(NomadSim.MAIN));
            Font f = graphics.getFont();
            graphics.setFont(FontManager.getRunescapeSmallFont());
            pill(graphics, "Rift: Main, " + sim.riftLeft() + " min", (float) px, (float) (ring.getMinY() - 4), new Color(0xC08BFF));
            graphics.setFont(f);
        }
        // The sea lane Main left in its wake: a ribbon that counts as Main for a while.
        double[] lane = sim.lane();
        if (lane != null)
        {
            Path2D.Double ribbon = new Path2D.Double();
            ribbon.moveTo(bounds.x + (lane[0] - west) * zoom, bounds.y + bounds.height - (lane[1] - south) * zoom);
            ribbon.lineTo(bounds.x + (lane[2] - west) * zoom, bounds.y + bounds.height - (lane[3] - south) * zoom);
            float width = (float) Math.max(3, NomadSim.LANE_HALF * 2 * zoom);
            graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(NomadPlugin.colour(sim.main(), 70));
            graphics.draw(ribbon);
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
        String heir = plugin.heir();
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            Ellipse2D ring = rings[j];
            if (!ring.intersects(bounds)) continue;
            String name = c.name();
            if (j == NomadSim.MAIN && heir != null) name += ", " + heir;
            if (j == yours) name += " (you)";
            Color colour = NomadPlugin.colour(c, 255);
            double top = ring.getMinY() - 4;
            if (top < bounds.y + 12) top = ring.getMaxY() + 12;
            pill(graphics, name, (float) ring.getCenterX(), (float) top, colour);
        }
        drawWaystones(graphics, sim, bounds, west, south, zoom);
        drawWanderings(graphics, sim, bounds, west, south, zoom, now);
        drawWorldWays(graphics, sim, plugin.config().plotCircles() ? plugin.plots() : java.util.Collections.emptyList(), bounds, west, south,
            zoom, now);
        if (plugin.spots().on()) drawSpots(graphics, bounds, west, south, zoom);
        drawOmen(graphics, bounds, west, south, zoom);
        drawRivals(graphics, bounds, west, south, zoom);
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

    /** A pilgrimage omen's landmark: a pale blue ring and its name, so you can plan the walk. */
    private void drawOmen(Graphics2D graphics, Rectangle bounds, int west, int south, float zoom)
    {
        com.wanderingnomad.journey.Omen o = plugin.omen();
        if (o == null || !com.wanderingnomad.journey.Omen.PILGRIMAGE.equals(o.kind())) return;
        double px = bounds.x + (o.x() - west) * zoom;
        double py = bounds.y + bounds.height - (o.y() - south) * zoom;
        if (!bounds.contains(px, py)) return;
        Color colour = new Color(0x9FE9FF);
        double r = Math.max(6, com.wanderingnomad.journey.JourneyWatch.AT_LANDMARK * zoom);
        graphics.setStroke(new BasicStroke(2f));
        graphics.setColor(colour);
        graphics.draw(new Ellipse2D.Double(px - r, py - r, 2 * r, 2 * r));
        outlined(graphics, "Omen: " + o.landmark(), (float) (px + r + 4), (float) py + 4, colour, false);
    }

    /**
     * The footprint: every 64-tile map region you've walked in inside your circle, filled faintly
     * in Main's colour, so the world map shows the run's whole journey at a glance.
     */
    private void drawFootprint(Graphics2D graphics, NomadSim sim, Rectangle bounds, int west, int south, float zoom)
    {
        java.util.Set<String> squares = plugin.footprint();
        if (squares.isEmpty()) return;
        Color c = NomadPlugin.colour(sim.main(), 255);
        graphics.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 46));
        double size = 64 * zoom;
        for (String id : squares)
        {
            int region;
            try
            {
                region = Integer.parseInt(id);
            }
            catch (NumberFormatException e)
            {
                continue;
            }
            int x0 = (region >> 8) << 6;
            int y0 = (region & 0xFF) << 6;
            double px = bounds.x + (x0 - west) * zoom;
            double py = bounds.y + bounds.height - (y0 + 64 - south) * zoom;
            if (px > bounds.getMaxX() || py > bounds.getMaxY() || px + size < bounds.x || py + size < bounds.y) continue;
            graphics.fill(new java.awt.geom.Rectangle2D.Double(px, py, size, size));
        }
    }

    private static final BasicStroke RIVAL = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{3f, 5f}, 0f);

    /**
     * Rivals: each friend's circle as a dotted ring in their colour with their name, their Main too
     * if they're in a region circle, and a dot where they stood when they last sent.
     */
    private void drawRivals(Graphics2D graphics, Rectangle bounds, int west, int south, float zoom)
    {
        long now = System.currentTimeMillis();
        for (com.wanderingnomad.rival.Rival r : plugin.rivals())
        {
            com.wanderingnomad.rival.Rival.Snapshot s = r.latest();
            if (s == null) continue;
            boolean fresh = r.fresh(now);
            Color colour = new Color(r.colour);
            if (!fresh) colour = new Color(colour.getRed(), colour.getGreen(), colour.getBlue(), 150);
            Circle bound = s.sim.bound();
            Circle main = s.sim.main();
            for (Circle c : bound == main ? java.util.Collections.singletonList(main) : java.util.Arrays.asList(main, bound))
            {
                double px = bounds.x + (c.x() - west) * zoom;
                double py = bounds.y + bounds.height - (c.y() - south) * zoom;
                double pr = Math.max(MIN_RADIUS_PX, c.radius() * zoom);
                Ellipse2D ring = new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2);
                if (!ring.intersects(bounds)) continue;
                graphics.setStroke(SHADOW);
                graphics.setColor(new Color(0, 0, 0, 120));
                graphics.draw(ring);
                graphics.setStroke(RIVAL);
                graphics.setColor(colour);
                graphics.draw(ring);
                if (c == bound)
                {
                    double top = ring.getMaxY() + 12;
                    pill(graphics, r.name + (fresh ? "" : " (away)"), (float) ring.getCenterX(), (float) top, colour);
                }
            }
            if (fresh && s.surface != null)
            {
                double px = bounds.x + (s.surface.x - west) * zoom;
                double py = bounds.y + bounds.height - (s.surface.y - south) * zoom;
                if (!bounds.contains(px, py)) continue;
                graphics.setColor(Color.BLACK);
                graphics.fill(new Ellipse2D.Double(px - 4, py - 4, 8, 8));
                graphics.setColor(colour);
                graphics.fill(new Ellipse2D.Double(px - 3, py - 3, 6, 6));
            }
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
            Color colour = !yours ? NomadPlugin.colour(sim.circles().get(w.circle()), 255)
                : w.legendary() ? SidebarParts.LEGEND : w.pilgrim() ? SidebarParts.RARE : new Color(0xFFD54F);
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
            if (w.wanders())
            {
                // Where it hops next, so you can head it off.
                double[] hop = sim.nextHop(w);
                if (hop != null)
                {
                    double hx = bounds.x + (hop[0] - west) * zoom;
                    double hy = bounds.y + bounds.height - (hop[1] - south) * zoom;
                    graphics.setStroke(DASHED);
                    graphics.setColor(colour);
                    graphics.draw(new Line2D.Double(px, py, hx, hy));
                    graphics.draw(new Ellipse2D.Double(hx - 3, hy - 3, 6, 6));
                }
            }
            String text = NomadPanel.stoneName(w) + " " + NomadPanel.stoneLeft(sim, w);
            graphics.setColor(Color.BLACK);
            graphics.drawString(text, (float) px + 8, (float) py + 5);
            graphics.setColor(colour);
            graphics.drawString(text, (float) px + 7, (float) py + 4);
        }
    }

    /** A Wake: the ground each circle left in the last few minutes that still counts as it, as a faint wash. */
    private static void drawWakes(Graphics2D graphics, NomadSim sim, Rectangle bounds, int west, int south, float zoom)
    {
        List<Circle> all = new java.util.ArrayList<>(sim.circles());
        if (sim.twin() != null) all.add(sim.twin());
        for (Circle c : all)
        {
            double[] w = c.wake();
            if (w.length == 0) continue;
            java.awt.geom.Area area = new java.awt.geom.Area();
            for (int k = 0; k + 2 < w.length; k += 3)
            {
                double px = bounds.x + (w[k] - west) * zoom;
                double py = bounds.y + bounds.height - (w[k + 1] - south) * zoom;
                double pr = Math.max(2, w[k + 2] * zoom);
                area.add(new java.awt.geom.Area(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2)));
            }
            graphics.setColor(NomadPlugin.colour(c, 45));
            graphics.fill(area);
            graphics.setStroke(DASHED);
            graphics.setColor(NomadPlugin.colour(c, 110));
            graphics.draw(area);
        }
    }

    private static final Color SKY = new Color(0xBFE3FF);
    private static final Color SEA = new Color(0x6FC3E8);
    private static final BasicStroke DOTTED = new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 10f, new float[]{2f, 6f}, 0f);
    private static final BasicStroke DASHED = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{6f, 5f}, 0f);

    /**
     * What wanders besides the circles: a fork's lost half (dashed), a comet's path and the comet
     * itself, and dotted lines to where Main is sailing or the port it's heading for.
     */
    private static void drawWanderings(Graphics2D graphics, NomadSim sim, Rectangle bounds, int west, int south, float zoom, long now)
    {
        Circle ghost = sim.ghost();
        if (ghost != null)
        {
            double px = bounds.x + (ghost.x() - west) * zoom;
            double py = bounds.y + bounds.height - (ghost.y() - south) * zoom;
            double pr = Math.max(MIN_RADIUS_PX, ghost.radius() * zoom);
            Color colour = NomadPlugin.colour(ghost, 150);
            graphics.setStroke(DASHED);
            graphics.setColor(colour);
            graphics.draw(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
            outlined(graphics, "Lost half · " + NomadPanel.time(sim.ghostLeft()), (float) px, (float) (py - pr - 4), colour, true);
        }
        // A Lodestone's pull: a dotted line from your circle to the landmark it leans toward.
        Circle yours = sim.bound();
        com.wanderingnomad.sim.Tile lode = yours.lode();
        if (lode != null)
        {
            double ax = bounds.x + (yours.x() - west) * zoom;
            double ay = bounds.y + bounds.height - (yours.y() - south) * zoom;
            double bx = bounds.x + (lode.x - west) * zoom;
            double by = bounds.y + bounds.height - (lode.y - south) * zoom;
            graphics.setStroke(DOTTED);
            graphics.setColor(Moments.GOLD);
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            String name = sim.lodestoneTo();
            outlined(graphics, "Lodestone" + (name == null ? "" : ": " + name) + " · " + NomadPanel.time(yours.lodeLeft()),
                (float) bx, (float) by - 6, Moments.GOLD, true);
        }
        Circle main = sim.main();
        String to = main.sailing() ? main.voyageTo() : main.portOfCall();
        Geography.Port port = to == null ? null : Geography.port(to);
        if (port != null)
        {
            double ax = bounds.x + (main.x() - west) * zoom;
            double ay = bounds.y + bounds.height - (main.y() - south) * zoom;
            double bx = bounds.x + (port.tile.x - west) * zoom;
            double by = bounds.y + bounds.height - (port.tile.y - south) * zoom;
            graphics.setStroke(DOTTED);
            graphics.setColor(SEA);
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            graphics.fill(new Ellipse2D.Double(bx - 4, by - 4, 8, 8));
            outlined(graphics, (main.sailing() ? "Sailing to " : "Bound for ") + to, (float) bx + 7, (float) by + 4, SEA, false);
        }
        Comet k = sim.comet();
        if (k != null)
        {
            double ax = bounds.x + (k.x() - west) * zoom;
            double ay = bounds.y + bounds.height - (k.y() - south) * zoom;
            double bx = bounds.x + (k.endX() - west) * zoom;
            double by = bounds.y + bounds.height - (k.endY() - south) * zoom;
            double pulse = 0.5 + 0.5 * Math.sin(now / 250.0);
            float width = (float) Math.max(2, 2 * NomadSim.COMET_RADIUS * zoom);
            graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            graphics.setColor(NomadSceneOverlay.alpha(SKY, k.flying() ? 110 : (int) (40 + 50 * pulse)));
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            graphics.setStroke(THIN);
            graphics.setColor(SKY);
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            graphics.setColor(Color.WHITE);
            graphics.fill(new Ellipse2D.Double(ax - 5, ay - 5, 10, 10));
            outlined(graphics, k.flying() ? "Comet · " + k.flightLeft() + "m" : "Comet in " + k.warning() + "m", (float) ax + 8, (float) ay - 6, SKY, false);
        }
    }

    private static final Color TRAVEL = new Color(0xA8E6A1);
    private static final Color STAR = new Color(0xFFF1A8);
    private static final Color PLOT = new Color(0x8FE0A2);

    /**
     * The world's own ways on the map: the travel hop Main is about to make, the coordinate
     * tide's pull, the fallen star holding your circle, and your plot circles.
     */
    private static void drawWorldWays(Graphics2D graphics, NomadSim sim, List<int[]> plots, Rectangle bounds, int west, int south,
        float zoom, long now)
    {
        com.wanderingnomad.sim.Networks.Node from = sim.hopFrom() == null ? null : com.wanderingnomad.sim.Networks.byName(sim.hopFrom());
        com.wanderingnomad.sim.Networks.Node to = sim.hopTo() == null ? null : com.wanderingnomad.sim.Networks.byName(sim.hopTo());
        if (from != null && to != null)
        {
            double ax = bounds.x + (from.x - west) * zoom;
            double ay = bounds.y + bounds.height - (from.y - south) * zoom;
            double bx = bounds.x + (to.x - west) * zoom;
            double by = bounds.y + bounds.height - (to.y - south) * zoom;
            graphics.setStroke(DOTTED);
            graphics.setColor(TRAVEL);
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            graphics.fill(new Ellipse2D.Double(bx - 4, by - 4, 8, 8));
            outlined(graphics, "Hop by " + com.wanderingnomad.sim.Networks.NAMES[to.network] + " in " + sim.hopWarn() + "m: " + to.name,
                (float) bx + 7, (float) by + 4, TRAVEL, false);
        }
        Circle yours = sim.bound();
        com.wanderingnomad.sim.Tile tide = sim.tide();
        if (tide != null)
        {
            double ax = bounds.x + (yours.x() - west) * zoom;
            double ay = bounds.y + bounds.height - (yours.y() - south) * zoom;
            double bx = bounds.x + (tide.x - west) * zoom;
            double by = bounds.y + bounds.height - (tide.y - south) * zoom;
            graphics.setStroke(DOTTED);
            graphics.setColor(SEA);
            graphics.draw(new Line2D.Double(ax, ay, bx, by));
            // An X marks the dig spot.
            graphics.setStroke(THIN);
            graphics.draw(new Line2D.Double(bx - 4, by - 4, bx + 4, by + 4));
            graphics.draw(new Line2D.Double(bx - 4, by + 4, bx + 4, by - 4));
            outlined(graphics, "Tide: your clue", (float) bx + 7, (float) by + 4, SEA, false);
        }
        com.wanderingnomad.sim.Tile star = sim.star();
        if (star != null)
        {
            double px = bounds.x + (star.x - west) * zoom;
            double py = bounds.y + bounds.height - (star.y - south) * zoom;
            double pulse = 0.5 + 0.5 * Math.sin(now / 300.0);
            double r = 4 + 2 * pulse;
            Path2D.Double spark = new Path2D.Double();
            for (int k = 0; k < 8; k++)
            {
                double a = k * Math.PI / 4;
                double d = k % 2 == 0 ? r * 1.6 : r * 0.6;
                if (k == 0) spark.moveTo(px + Math.cos(a) * d, py - Math.sin(a) * d);
                else spark.lineTo(px + Math.cos(a) * d, py - Math.sin(a) * d);
            }
            spark.closePath();
            graphics.setColor(STAR);
            graphics.fill(spark);
            outlined(graphics, "Fallen star · " + NomadPanel.time(yours.starHold()), (float) px + 9, (float) py + 4, STAR, false);
        }
        for (int[] p : plots)
        {
            double px = bounds.x + (p[0] - west) * zoom;
            double py = bounds.y + bounds.height - (p[1] - south) * zoom;
            double pr = Math.max(3, com.wanderingnomad.sim.Plots.RADIUS * zoom);
            graphics.setStroke(THIN);
            graphics.setColor(NomadSceneOverlay.alpha(PLOT, 200));
            graphics.draw(new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2));
            if (zoom >= 2) outlined(graphics, "Plot · " + NomadPanel.time(p[2]), (float) px, (float) (py - pr - 3), PLOT, true);
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
