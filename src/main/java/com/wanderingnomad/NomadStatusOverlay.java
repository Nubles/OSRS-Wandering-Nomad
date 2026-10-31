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
import net.runelite.client.ui.overlay.components.ComponentConstants;
import net.runelite.client.ui.overlay.components.LineComponent;
import net.runelite.client.ui.overlay.components.ProgressBarComponent;
import net.runelite.client.ui.overlay.components.TitleComponent;

/** A small box: your circle, inside or outside, the edge distance and any open crossing. */
class NomadStatusOverlay extends OverlayPanel
{
    private static final Color INSIDE = new Color(0x8FE0A2);
    private static final Color OUTSIDE = new Color(0xFF8A80);
    private static final Color STRANDED = new Color(0xFFCF8F);
    static final Color FORK = new Color(0xFFC46B);
    private static final Color CALM = new Color(0x6FD08C);
    private static final Color AMBER = new Color(0xFFB040);
    private static final Color URGENT = new Color(0xFF4F45);
    /** The box's width with the extra rows on: room for the heading row and two boon chips side by side. */
    private static final int WIDTH = 170;

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
            // Either half of a forked Main counts, so the nearest edge is the one you're inside.
            double toEdge = NomadPlugin.toEdge(sim, at.x, at.y);
            String edge = toEdge >= 0 ? (int) Math.floor(toEdge) + " tiles" : (int) Math.ceil(-toEdge) + " out";
            panelComponent.getChildren().add(LineComponent.builder().left("To edge").right(edge).build());
        }
        if (sim.forkWarning() > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Fork in " + sim.forkWarning() + " min")
                .right(NomadSim.compass(sim.forkAxis()) + " / " + NomadSim.compass(sim.forkAxis() + Math.PI))
                .rightColor(FORK).build());
        }
        else if (sim.twin() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Forked").right("pick a half").rightColor(FORK).build());
        }
        Radar radar = plugin.radar();
        boolean hud = plugin.config().hudExtras();
        // A fixed width, so the box doesn't change size as names and numbers come and go.
        panelComponent.setPreferredSize(new Dimension(hud ? WIDTH : ComponentConstants.STANDARD_WIDTH, 0));
        if (radar != null && radar.edgeIn > 0)
        {
            Color urgency = urgency(radar.edgeIn);
            panelComponent.getChildren().add(LineComponent.builder().left("Edge reaches you")
                .right("~" + radar.edgeIn + " min").rightColor(hud ? urgency : radar.edgeIn <= NomadPlugin.EDGE_WARNING ? OUTSIDE : STRANDED).build());
            if (hud)
            {
                // Empties as the edge closes in, shifting from calm to urgent.
                ProgressBarComponent bar = new ProgressBarComponent();
                bar.setMinimum(0);
                bar.setMaximum(Radar.MINUTES);
                bar.setValue(radar.edgeIn);
                bar.setLabelDisplayMode(ProgressBarComponent.LabelDisplayMode.TEXT_ONLY);
                bar.setCenterLabel("");
                bar.setForegroundColor(urgency);
                bar.setBackgroundColor(new Color(0, 0, 0, 120));
                panelComponent.getChildren().add(bar);
            }
        }
        if (hud)
        {
            panelComponent.getChildren().add(new HudParts.Heading(c.heading(), c.moodName(), NomadPlugin.colour(c, 255)));
            HudParts.Chips chips = new HudParts.Chips();
            if (c.expandLeft() > 0) chips.add(NomadSim.EXPAND + " " + NomadPanel.time(c.expandLeft()));
            if (c.tailwindLeft() > 0) chips.add(NomadSim.TAILWIND + " " + NomadPanel.time(c.tailwindLeft()));
            if (c.anchorLeft() > 0) chips.add(NomadSim.ANCHOR + " " + NomadPanel.time(c.anchorLeft()));
            if (sim.bridgeLeft() > 0) chips.add(NomadSim.BRIDGE + " " + sim.bridgeLeft() + "m");
            if (!chips.isEmpty()) panelComponent.getChildren().add(chips);
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

    /** Calm green with a quarter hour to go, through amber, to red as the edge arrives. */
    static Color urgency(int minutes)
    {
        float t = Math.max(0f, Math.min(1f, (minutes - 1) / (float) (Radar.MINUTES - 1)));
        Color a = t < 0.5f ? URGENT : AMBER;
        Color b = t < 0.5f ? AMBER : CALM;
        float u = t < 0.5f ? t * 2 : (t - 0.5f) * 2;
        return new Color(
            Math.round(a.getRed() + (b.getRed() - a.getRed()) * u),
            Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * u),
            Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * u));
    }
}
