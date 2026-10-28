package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * The living edge in the game world. Your circle's edge rises out of the ground as a glowing
 * curtain, with bands of light running along it and sparks drifting up. Where a circle you can
 * cross into reaches inside yours, its wall stands brighter: that stretch is the gateway. An arrow
 * on the ground points to the nearest open crossing, or one the Radar says opens soon, and a gold
 * one to the nearest waystone you can claim. Waystones stand as pillars of light.
 * Only on the surface: underground or in an instance the edge isn't where you are.
 */
class NomadSceneOverlay extends Overlay
{
    /** Tiles from the player that get drawn; past this the scene ends anyway. */
    private static final int REACH = 40;
    /** The wall stands just outside the edge tiles, so every tile inside is within it. */
    private static final double WALL_OUT = 0.5;
    /** Wall height in local units (128 to a tile); a player is about 200 tall. */
    private static final int WALL_HEIGHT = 170;
    private static final int GATE_HEIGHT = 260;
    /** Wall segments are about half a tile long. */
    private static final double SEGMENT = 0.5;
    private static final BasicStroke GROUND = new BasicStroke(2f);
    private static final BasicStroke YOURS = new BasicStroke(2f);
    private static final BasicStroke OTHER = new BasicStroke(1f);
    private static final BasicStroke ARROW = new BasicStroke(1.5f);
    private static final Color GOLD = new Color(0xFFD54F);
    /** A waystone's pillar, in local units: about seven tiles tall. */
    private static final int BEACON_HEIGHT = 900;

    private final Client client;
    private final NomadPlugin plugin;

    @Inject
    NomadSceneOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        if (sim == null) return null;
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (player == null || view == null || view.getScene() == null || view.getScene().isInstance()) return null;
        WorldPoint at = player.getWorldLocation();
        if (at == null) return null;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        NomadConfig config = plugin.config();
        long now = System.currentTimeMillis();

        if (config.drawEdge())
        {
            boolean wall = config.edgeStyle() != NomadConfig.EdgeStyle.TILES;
            boolean tiles = config.edgeStyle() != NomadConfig.EdgeStyle.WALL;
            List<Circle> circles = sim.circles();
            Circle yours = sim.bound();
            for (int j = 0; j < circles.size(); j++)
            {
                Circle c = circles.get(j);
                boolean mine = j == sim.boundIndex();
                if (!mine && !(sim.linked(j) && yours.touches(c))) continue;
                if (tiles) drawTiles(graphics, view, c, at, mine);
                if (wall) drawWall(graphics, view, c, mine ? null : yours, at, now);
            }
        }
        drawBeacons(graphics, view, sim, now);
        if (config.compass())
        {
            drawCompass(graphics, view, sim, at);
            drawWaystoneArrow(graphics, view, sim, at);
        }
        return null;
    }

    private void drawTiles(Graphics2D graphics, WorldView view, Circle c, WorldPoint at, boolean yours)
    {
        for (Tile t : EdgeTiles.near(c, at.getX(), at.getY(), REACH))
        {
            LocalPoint local = LocalPoint.fromWorld(view, t.x, t.y);
            if (local == null) continue;
            Polygon poly = Perspective.getCanvasTilePoly(client, local);
            if (poly == null) continue;
            OverlayUtil.renderPolygon(graphics, poly, NomadPlugin.colour(c, yours ? 220 : 140),
                NomadPlugin.colour(c, yours ? 40 : 20), yours ? YOURS : OTHER);
        }
    }

    /**
     * Draws a circle's wall near the player. With {@code inside} set, only the stretch inside that
     * circle is drawn, taller and brighter: the gateway into it.
     */
    private void drawWall(Graphics2D graphics, WorldView view, Circle c, Circle inside, WorldPoint at, long now)
    {
        double r = c.radius() + WALL_OUT;
        int n = Math.max(48, (int) Math.ceil(Math.PI * 2 * r / SEGMENT));
        boolean gate = inside != null;
        int height = (int) ((gate ? GATE_HEIGHT : WALL_HEIGHT) * (c.pulsing() ? 1.35 : 1));
        int plane = view.getPlane();
        double phase = now / 1000.0;

        Point g0 = null;
        Point t0 = null;
        for (int i = 0; i <= n; i++)
        {
            double a = i * Math.PI * 2 / n;
            double wx = c.x() + Math.cos(a) * r;
            double wy = c.y() + Math.sin(a) * r;
            boolean near = Math.abs(wx - at.getX()) <= REACH && Math.abs(wy - at.getY()) <= REACH;
            boolean shown = near && (!gate || inside.contains(wx, wy));
            LocalPoint local = shown ? local(view, wx, wy) : null;
            Point g1 = local == null ? null : Perspective.localToCanvas(client, local, plane, 0);
            Point t1 = local == null ? null : Perspective.localToCanvas(client, local, plane, height);
            if (g0 != null && t0 != null && g1 != null && t1 != null)
            {
                // Bands of light run along the wall, a little faster on a gateway.
                double band = 0.5 + 0.5 * Math.sin(a * c.radius() * 0.9 - phase * (gate ? 3.2 : 1.8));
                int alpha = (int) ((gate ? 150 : 95) * (0.55 + 0.45 * band));
                segment(graphics, c, g0, g1, t0, t1, alpha, gate);
                if (i % 3 == 0) spark(graphics, c, g0, t0, (phase * 0.35 + i * 0.137) % 1.0);
            }
            g0 = g1;
            t0 = t1;
        }
    }

    private static void segment(Graphics2D graphics, Circle c, Point g0, Point g1, Point t0, Point t1, int alpha, boolean gate)
    {
        Polygon quad = new Polygon(
            new int[]{g0.getX(), g1.getX(), t1.getX(), t0.getX()},
            new int[]{g0.getY(), g1.getY(), t1.getY(), t0.getY()}, 4);
        float gx = (g0.getX() + g1.getX()) / 2f;
        float gy = (g0.getY() + g1.getY()) / 2f;
        float tx = (t0.getX() + t1.getX()) / 2f;
        float ty = (t0.getY() + t1.getY()) / 2f;
        Color base = gate ? brighter(c) : NomadPlugin.colour(c, 255);
        Paint before = graphics.getPaint();
        graphics.setPaint(new GradientPaint(gx, gy, alpha(base, alpha), tx, ty, alpha(base, 0)));
        graphics.fill(quad);
        graphics.setPaint(before);
        graphics.setStroke(GROUND);
        graphics.setColor(alpha(base, Math.min(255, alpha + 120)));
        graphics.drawLine(g0.getX(), g0.getY(), g1.getX(), g1.getY());
    }

    /** A spark drifting up the wall, fading as it rises. */
    private static void spark(Graphics2D graphics, Circle c, Point ground, Point top, double rise)
    {
        int x = (int) (ground.getX() + (top.getX() - ground.getX()) * rise);
        int y = (int) (ground.getY() + (top.getY() - ground.getY()) * rise);
        graphics.setColor(alpha(Color.WHITE, (int) (170 * (1 - rise))));
        graphics.fillOval(x - 1, y - 1, 3, 3);
        graphics.setColor(NomadPlugin.colour(c, (int) (120 * (1 - rise))));
        graphics.drawOval(x - 2, y - 2, 5, 5);
    }

    /**
     * An arrow on the ground a couple of tiles from you, toward the nearest crossing that's open or
     * opening within the Radar's 15 minutes, with its name and distance at the tip.
     */
    private void drawCompass(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        Radar radar = plugin.radar();
        if (radar == null) return;
        Circle yours = sim.bound();
        int best = -1;
        for (int j = 0; j < sim.circles().size(); j++)
        {
            int in = radar.crossingIn[j];
            if (in < 0) continue;
            if (best < 0 || in < radar.crossingIn[best]
                || in == radar.crossingIn[best] && yours.gap(sim.circles().get(j)) < yours.gap(sim.circles().get(best)))
            {
                best = j;
            }
        }
        if (best < 0) return;
        Circle to = sim.circles().get(best);
        int in = radar.crossingIn[best];
        Tile here = new Tile(at.getX(), at.getY());
        LocalPoint feet = LocalPoint.fromWorld(view, at.getX(), at.getY());
        if (feet == null) return;
        if (in == 0 && sim.canCross(best, here))
        {
            Point label = Perspective.localToCanvas(client, feet, view.getPlane(), 260);
            if (label != null)
            {
                OverlayUtil.renderTextLocation(graphics, label, "In the overlap: cross into " + to.name() + " from the sidebar",
                    brighter(to));
            }
            return;
        }

        double[] target = crossingPoint(yours, to);
        double d = Math.hypot(target[0] - at.getX(), target[1] - at.getY());
        String text = to.name() + " · " + Math.round(d) + " tiles" + (in > 0 ? " · opens in ~" + in + " min" : "");
        drawArrow(graphics, view, at, target, NomadPlugin.colour(to, 255), brighter(to), text);
    }

    /** A gold arrow to the nearest waystone in your circle, while you have room for its boon. */
    private void drawWaystoneArrow(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        if (sim.boons().size() >= NomadSim.BOONS_HELD) return;
        Waystone best = null;
        double bestD = 0;
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != sim.boundIndex()) continue;
            double d = Math.hypot(w.x() - at.getX(), w.y() - at.getY());
            if (best == null || d < bestD)
            {
                best = w;
                bestD = d;
            }
        }
        if (best == null || bestD <= NomadSim.CLAIM_REACH) return;
        String text = best.boon() + " waystone · " + Math.round(bestD) + " tiles · ~" + NomadPanel.roughly(sim.minutesLeft(best)) + " left";
        drawArrow(graphics, view, at, new double[]{best.x(), best.y()}, GOLD, GOLD, text);
    }

    /** Waystones in the scene stand as pillars of light, with their boon and time left above. */
    private void drawBeacons(Graphics2D graphics, WorldView view, NomadSim sim, long now)
    {
        int plane = view.getPlane();
        for (Waystone w : sim.waystones())
        {
            LocalPoint lp = LocalPoint.fromWorld(view, w.x(), w.y());
            if (lp == null) continue;
            Point ground = Perspective.localToCanvas(client, lp, plane, 0);
            Point top = Perspective.localToCanvas(client, lp, plane, BEACON_HEIGHT);
            if (ground == null || top == null) continue;
            boolean claimable = w.circle() == sim.boundIndex();
            Color colour = claimable ? GOLD : NomadPlugin.colour(sim.circles().get(w.circle()), 255);
            double pulse = 0.5 + 0.5 * Math.sin(now / 400.0);
            Paint before = graphics.getPaint();
            for (int width = 14; width >= 4; width -= 5)
            {
                int a = (int) ((width == 4 ? 220 : 70) * (0.7 + 0.3 * pulse));
                graphics.setPaint(new GradientPaint(ground.getX(), ground.getY(), alpha(colour, a), top.getX(), top.getY(), alpha(colour, 0)));
                graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.drawLine(ground.getX(), ground.getY(), top.getX(), top.getY());
            }
            graphics.setPaint(before);
            Polygon tile = Perspective.getCanvasTilePoly(client, lp);
            if (tile != null) OverlayUtil.renderPolygon(graphics, tile, colour, alpha(colour, 60), YOURS);
            Point label = Perspective.localToCanvas(client, lp, plane, BEACON_HEIGHT / 3);
            if (label != null)
            {
                String text = w.boon() + " · ~" + NomadPanel.roughly(sim.minutesLeft(w)) + (claimable ? "" : " · in " + sim.circles().get(w.circle()).name());
                OverlayUtil.renderTextLocation(graphics, label, text, colour);
            }
        }
    }

    /** An arrow on the ground from about 1.5 to 4 tiles out toward {@code target}, labelled at its tip. */
    private void drawArrow(Graphics2D graphics, WorldView view, WorldPoint at, double[] target, Color fill, Color label, String text)
    {
        double dx = target[0] - at.getX();
        double dy = target[1] - at.getY();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1) return;
        double ux = dx / d;
        double uy = dy / d;
        // Shaft from 1.5 to 3 tiles out, head to 4, in tile units around the player's tile.
        double[][] shape = {
            {1.5, -0.18}, {3.0, -0.18}, {3.0, -0.5}, {4.0, 0}, {3.0, 0.5}, {3.0, 0.18}, {1.5, 0.18},
        };
        int[] xs = new int[shape.length];
        int[] ys = new int[shape.length];
        for (int k = 0; k < shape.length; k++)
        {
            double along = shape[k][0];
            double side = shape[k][1];
            double wx = at.getX() + ux * along - uy * side;
            double wy = at.getY() + uy * along + ux * side;
            LocalPoint lp = local(view, wx, wy);
            Point p = lp == null ? null : Perspective.localToCanvas(client, lp, view.getPlane(), 0);
            if (p == null) return;
            xs[k] = p.getX();
            ys[k] = p.getY();
        }
        Polygon arrow = new Polygon(xs, ys, shape.length);
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
        graphics.setColor(alpha(fill, (int) (120 + 90 * pulse)));
        graphics.fill(arrow);
        graphics.setStroke(ARROW);
        graphics.setColor(alpha(Color.WHITE, 200));
        graphics.draw(arrow);

        LocalPoint tip = local(view, at.getX() + ux * 4.6, at.getY() + uy * 4.6);
        Point where = tip == null ? null : Perspective.localToCanvas(client, tip, view.getPlane(), 40);
        if (where != null) OverlayUtil.renderTextLocation(graphics, where, text, label);
    }

    /**
     * Where to walk to cross: the middle of the overlap along the line between the centres, or,
     * before they touch, the point of your edge nearest the other circle.
     */
    static double[] crossingPoint(Circle yours, Circle to)
    {
        double dx = to.x() - yours.x();
        double dy = to.y() - yours.y();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1e-6) return new double[]{yours.x(), yours.y()};
        double along = yours.touches(to)
            ? Math.max(0, Math.min(d, (d - to.radius() + yours.radius()) / 2))
            : yours.radius();
        return new double[]{yours.x() + dx / d * along, yours.y() + dy / d * along};
    }

    /** A local point for a world position between tile centres, or null outside the scene. */
    private static LocalPoint local(WorldView view, double wx, double wy)
    {
        int lx = (int) Math.round((wx - view.getBaseX()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        int ly = (int) Math.round((wy - view.getBaseY()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        int max = Perspective.LOCAL_TILE_SIZE;
        if (lx < max || ly < max || lx >= (view.getSizeX() - 1) * max || ly >= (view.getSizeY() - 1) * max) return null;
        return new LocalPoint(lx, ly, view);
    }

    private static Color brighter(Circle c)
    {
        Color base = NomadPlugin.colour(c, 255);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * 0.7f, Math.min(1f, hsb[2] * 1.25f + 0.1f));
    }

    static Color alpha(Color c, int alpha)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
