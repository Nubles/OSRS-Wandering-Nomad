package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Waystone;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Your circle on the minimap: its edge as a soft glowing ring, the ground outside it lightly
 * dimmed, the edges of circles you can cross into, and small arrows on the rim pointing to the
 * nearest open crossing and to a waystone you can claim. Everything is clipped to the round map.
 */
class NomadMinimapOverlay extends Overlay
{
    /** The minimap in each layout: fixed, resizable, and resizable with the bottom bar. */
    private static final int[] MINIMAPS = {
        InterfaceID.Toplevel.MINIMAP,
        InterfaceID.ToplevelOsrsStretch.MINIMAP,
        InterfaceID.ToplevelPreEoc.MINIMAP,
    };
    /** How far off the minimap a point may be and still be placed, in local units. */
    private static final int FAR = 400 * Perspective.LOCAL_TILE_SIZE;
    private static final BasicStroke[] GLOW = {
        new BasicStroke(6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
        new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
        new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
    };
    private static final int[] GLOW_ALPHA = {40, 90, 235};
    private static final Color SHADE = new Color(0, 0, 0, 70);

    private final Client client;
    private final NomadPlugin plugin;

    @Inject
    NomadMinimapOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        if (sim == null || !plugin.config().drawMinimap()) return null;
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (player == null || view == null || view.getScene() == null || view.getScene().isInstance()) return null;
        WorldPoint at = player.getWorldLocation();
        Ellipse2D minimap = minimapShape();
        LocalPoint feet = player.getLocalLocation();
        Point centre = feet == null ? null : Perspective.localToMinimap(client, feet);
        if (at == null || minimap == null || centre == null) return null;

        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Shape before = graphics.getClip();
        graphics.clip(minimap);

        Circle yours = sim.bound();
        Path2D ring = ring(view, yours);
        if (ring != null)
        {
            Area outside = new Area(minimap);
            outside.subtract(new Area(ring));
            graphics.setColor(SHADE);
            graphics.fill(outside);
        }
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle o = sim.circles().get(j);
            if (j == sim.boundIndex() || !sim.linked(j) || yours.gap(o) > 40) continue;
            Path2D other = ring(view, o);
            if (other != null) glow(graphics, other, NomadPlugin.colour(o, 255), 0.6);
        }
        if (ring != null) glow(graphics, ring, NomadPlugin.colour(yours, 255), 1);
        graphics.setClip(before);

        if (plugin.config().minimapArrows())
        {
            double[] crossing = nearestCrossing(sim);
            if (crossing != null) pointer(graphics, view, minimap, centre, crossing, NomadPlugin.colour(sim.circles().get((int) crossing[2]), 255));
            Waystone stone = nearestWaystone(sim, at);
            if (stone != null) pointer(graphics, view, minimap, centre, new double[]{stone.x(), stone.y()}, Moments.GOLD);
        }
        return null;
    }

    /** A circle's whole edge in minimap pixels, or null if it can't be placed. */
    private Path2D ring(WorldView view, Circle c)
    {
        int n = Math.max(96, (int) Math.ceil(Math.PI * 2 * c.radius() / 0.75));
        Path2D path = new Path2D.Float();
        for (int i = 0; i < n; i++)
        {
            double a = i * Math.PI * 2 / n;
            Point p = toMinimap(view, c.x() + Math.cos(a) * c.radius(), c.y() + Math.sin(a) * c.radius());
            if (p == null) return null;
            if (i == 0) path.moveTo(p.getX(), p.getY());
            else path.lineTo(p.getX(), p.getY());
        }
        path.closePath();
        return path;
    }

    private static void glow(Graphics2D graphics, Shape line, Color colour, double strength)
    {
        for (int k = 0; k < GLOW.length; k++)
        {
            graphics.setStroke(GLOW[k]);
            graphics.setColor(NomadSceneOverlay.alpha(colour, (int) (GLOW_ALPHA[k] * strength)));
            graphics.draw(line);
        }
    }

    /**
     * Points toward {@code target}: an arrow on the minimap's rim while it's off the map, and a
     * small marker on it once it's in view.
     */
    private void pointer(Graphics2D graphics, WorldView view, Ellipse2D minimap, Point centre, double[] target, Color colour)
    {
        Point p = toMinimap(view, target[0], target[1]);
        if (p == null) return;
        double dx = p.getX() - centre.getX();
        double dy = p.getY() - centre.getY();
        double d = Math.hypot(dx, dy);
        if (d < 1) return;
        double ux = dx / d;
        double uy = dy / d;
        graphics.setStroke(new BasicStroke(1f));
        if (minimap.contains(p.getX(), p.getY()))
        {
            Ellipse2D dot = new Ellipse2D.Double(p.getX() - 3.5, p.getY() - 3.5, 7, 7);
            graphics.setColor(colour);
            graphics.fill(dot);
            graphics.setColor(Color.BLACK);
            graphics.draw(dot);
            return;
        }
        // Just inside the rim, along the line from you toward it.
        double rx = minimap.getWidth() / 2 - 7;
        double ry = minimap.getHeight() / 2 - 7;
        double t = 1 / Math.sqrt(ux * ux / (rx * rx) + uy * uy / (ry * ry));
        double tx = minimap.getCenterX() + ux * t;
        double ty = minimap.getCenterY() + uy * t;
        Path2D arrow = new Path2D.Double();
        arrow.moveTo(tx + ux * 7, ty + uy * 7);
        arrow.lineTo(tx - uy * 5 - ux * 4, ty + ux * 5 - uy * 4);
        arrow.lineTo(tx + uy * 5 - ux * 4, ty - ux * 5 - uy * 4);
        arrow.closePath();
        graphics.setColor(colour);
        graphics.fill(arrow);
        graphics.setColor(Color.BLACK);
        graphics.draw(arrow);
    }

    /** Where to walk for the nearest crossing that's open now, with the circle's index third; null if none. */
    private static double[] nearestCrossing(NomadSim sim)
    {
        Circle yours = sim.bound();
        int best = -1;
        for (int j = 0; j < sim.circles().size(); j++)
        {
            if (j == sim.boundIndex() || !sim.linked(j) || !yours.touches(sim.circles().get(j))) continue;
            if (best < 0 || yours.gap(sim.circles().get(j)) < yours.gap(sim.circles().get(best))) best = j;
        }
        if (best < 0) return null;
        double[] p = NomadSceneOverlay.crossingPoint(yours, sim.circles().get(best));
        return new double[]{p[0], p[1], best};
    }

    /** The nearest waystone in your circle while you've room for its boon. */
    private static Waystone nearestWaystone(NomadSim sim, WorldPoint at)
    {
        if (sim.boons().size() >= NomadSim.BOONS_HELD) return null;
        Waystone best = null;
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != sim.boundIndex()) continue;
            if (best == null || Math.hypot(w.x() - at.getX(), w.y() - at.getY()) < Math.hypot(best.x() - at.getX(), best.y() - at.getY())) best = w;
        }
        return best;
    }

    /** A world position (tile units, tile centres on whole numbers) on the minimap, even when it's off the edge of it. */
    private Point toMinimap(WorldView view, double wx, double wy)
    {
        int lx = (int) Math.round((wx - view.getBaseX()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        int ly = (int) Math.round((wy - view.getBaseY()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        return Perspective.localToMinimap(client, new LocalPoint(lx, ly, view), FAR);
    }

    private Ellipse2D minimapShape()
    {
        for (int id : MINIMAPS)
        {
            Widget minimap = client.getWidget(id);
            if (minimap == null || minimap.isHidden()) continue;
            Rectangle bounds = minimap.getBounds();
            if (bounds != null && bounds.width > 0 && bounds.height > 0)
            {
                return new Ellipse2D.Double(bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
        return null;
    }
}
