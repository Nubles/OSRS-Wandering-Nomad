package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.NomadSim;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Point;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.widgets.Widget;
import net.runelite.api.worldmap.WorldMap;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Every circle on the world map: yours solid and filled, Main dashed, the region circles thin,
 * each with a tick showing its heading. Tiles are placed with the same maths RuneLite uses for
 * map pins: the map widget's bounds, the zoom in pixels per tile and the tile at its centre.
 */
class NomadWorldMapOverlay extends Overlay
{
    private static final BasicStroke YOURS = new BasicStroke(3f);
    private static final BasicStroke MAIN = new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{10f, 5f}, 0f);
    private static final BasicStroke CONE = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{3f, 4f}, 0f);
    private static final BasicStroke REGION = new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f,
        new float[]{5f, 4f}, 0f);

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
        List<Circle> circles = sim.circles();
        Forecast forecast = plugin.forecast();
        if (forecast != null && plugin.config().forecast())
        {
            drawForecast(graphics, forecast, circles, sim.boundIndex(), bounds, west, south, zoom);
        }
        for (int j = circles.size() - 1; j >= 0; j--)
        {
            Circle c = circles.get(j);
            boolean yours = j == sim.boundIndex();
            double px = bounds.x + (c.x() - west) * zoom;
            double py = bounds.y + bounds.height - (c.y() - south) * zoom;
            double pr = c.radius() * zoom;
            Ellipse2D ring = new Ellipse2D.Double(px - pr, py - pr, pr * 2, pr * 2);
            if (yours)
            {
                graphics.setColor(NomadPlugin.colour(c, 50));
                graphics.fill(ring);
            }
            graphics.setStroke(yours ? YOURS : c.isMain() ? MAIN : REGION);
            graphics.setColor(NomadPlugin.colour(c, yours || c.isMain() ? 230 : 160));
            graphics.draw(ring);
            double tick = pr + Math.max(8, 18 * zoom / 4);
            graphics.draw(new Line2D.Double(px, py, px + Math.cos(c.heading()) * tick, py - Math.sin(c.heading()) * tick));
        }
        graphics.setClip(before);
        return null;
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
