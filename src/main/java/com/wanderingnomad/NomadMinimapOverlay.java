package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
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

/** Your circle's edge as dots on the minimap. */
class NomadMinimapOverlay extends Overlay
{
    private static final int REACH = 20;
    /** The minimap in each layout: fixed, resizable, and resizable with the bottom bar. */
    private static final int[] MINIMAPS = {
        InterfaceID.Toplevel.MINIMAP,
        InterfaceID.ToplevelOsrsStretch.MINIMAP,
        InterfaceID.ToplevelPreEoc.MINIMAP,
    };

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
        Shape minimap = minimapClip();
        if (at == null || minimap == null) return null;

        Circle c = sim.bound();
        Shape before = graphics.getClip();
        graphics.clip(minimap);
        graphics.setColor(NomadPlugin.colour(c, 230));
        for (Tile t : EdgeTiles.near(c, at.getX(), at.getY(), REACH))
        {
            LocalPoint local = LocalPoint.fromWorld(view, t.x, t.y);
            if (local == null) continue;
            Point dot = Perspective.localToMinimap(client, local);
            if (dot != null) graphics.fillRect(dot.getX() - 1, dot.getY() - 1, 3, 3);
        }
        graphics.setClip(before);
        return null;
    }

    private Shape minimapClip()
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
