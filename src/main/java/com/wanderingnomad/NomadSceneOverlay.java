package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.awt.BasicStroke;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.util.List;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * Your circle's edge tiles in the game world, solid, and the edges of circles you could cross
 * into, thinner. Only on the surface: underground or in an instance the edge isn't where you are.
 */
class NomadSceneOverlay extends Overlay
{
    /** Tiles from the player that get drawn; past this the scene ends anyway. */
    private static final int REACH = 40;
    private static final BasicStroke YOURS = new BasicStroke(2f);
    private static final BasicStroke OTHER = new BasicStroke(1f);

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
        if (sim == null || !plugin.config().drawEdge()) return null;
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (player == null || view == null || view.getScene() == null || view.getScene().isInstance()) return null;
        WorldPoint at = player.getWorldLocation();
        if (at == null) return null;
        List<Circle> circles = sim.circles();
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            boolean yours = j == sim.boundIndex();
            if (!yours && !(sim.linked(j) && sim.bound().touches(c))) continue;
            draw(graphics, view, c, at, yours);
        }
        return null;
    }

    private void draw(Graphics2D graphics, WorldView view, Circle c, WorldPoint at, boolean yours)
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
}
