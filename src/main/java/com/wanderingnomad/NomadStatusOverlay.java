package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import javax.inject.Inject;
import net.runelite.client.ui.overlay.OverlayPanel;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** A small box: your circle, inside or outside, the edge distance and any open crossing. */
class NomadStatusOverlay extends OverlayPanel
{
    private static final Color INSIDE = new Color(0x8FE0A2);
    private static final Color OUTSIDE = new Color(0xFF8A80);
    private static final Color STRANDED = new Color(0xFFCF8F);

    private final NomadPlugin plugin;

    @Inject
    NomadStatusOverlay(NomadPlugin plugin)
    {
        this.plugin = plugin;
        setPosition(OverlayPosition.TOP_LEFT);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        if (sim == null || !plugin.config().statusBox()) return null;
        Circle c = sim.bound();
        Tile at = plugin.surface();
        panelComponent.getChildren().add(TitleComponent.builder()
            .text(c.name() + " circle").color(NomadPlugin.colour(c, 255)).build());

        String state;
        Color colour;
        // Read live from the tile you're on: the run itself only checks once a played minute, which
        // left this saying Inside while To edge already said you were out. An open Bridge doesn't count.
        if (plugin.outsideNow() && sim.bridgeLeft() <= 0)
        {
            state = "Outside";
            colour = OUTSIDE;
        }
        else if (sim.stranded())
        {
            state = "Stranded";
            colour = STRANDED;
        }
        else
        {
            state = "Inside";
            colour = INSIDE;
        }
        panelComponent.getChildren().add(LineComponent.builder().left("Status").right(state).rightColor(colour).build());
        if (at != null)
        {
            double d = Math.hypot(at.x - c.x(), at.y - c.y());
            String edge = d <= c.radius() ? (int) Math.floor(c.radius() - d) + " tiles" : (int) Math.ceil(d - c.radius()) + " out";
            panelComponent.getChildren().add(LineComponent.builder().left("To edge").right(edge).build());
        }
        Radar radar = plugin.radar();
        if (radar != null && radar.edgeIn > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Edge reaches you")
                .right("~" + radar.edgeIn + " min").rightColor(radar.edgeIn <= NomadPlugin.EDGE_WARNING ? OUTSIDE : STRANDED).build());
        }
        panelComponent.getChildren().add(LineComponent.builder().left("Radius").right(Math.round(c.radius()) + " tiles").build());
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle o = sim.circles().get(j);
            if (sim.linked(j) && sim.bound().touches(o))
            {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Crossing").right(o.name()).rightColor(NomadPlugin.colour(o, 255)).build());
            }
            else if (radar != null && radar.crossingIn[j] > 0)
            {
                panelComponent.getChildren().add(LineComponent.builder()
                    .left("Crossing soon").right(o.name() + " ~" + radar.crossingIn[j] + "m").rightColor(NomadPlugin.colour(o, 200)).build());
            }
        }
        Waystone near = null;
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != sim.boundIndex() || at == null) continue;
            if (near == null || Math.hypot(w.x() - at.x, w.y() - at.y) < Math.hypot(near.x() - at.x, near.y() - at.y)) near = w;
        }
        if (near != null)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Waystone")
                .right(near.boon() + " " + Math.round(Math.hypot(near.x() - at.x, near.y() - at.y)) + "t ~" + NomadPanel.roughly(sim.minutesLeft(near)))
                .rightColor(new Color(0xFFD54F)).build());
        }
        if (!sim.boons().isEmpty())
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Boons").right(String.join(", ", sim.boons())).build());
        }
        if (sim.volcanoOpen())
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Volcano").right("Open").rightColor(INSIDE).build());
        }
        return super.render(graphics);
    }
}
