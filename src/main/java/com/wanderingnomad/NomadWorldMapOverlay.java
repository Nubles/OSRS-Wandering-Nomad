package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
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
}
