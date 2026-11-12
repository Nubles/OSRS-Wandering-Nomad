package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.runelite.client.ui.FontManager;
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
    static final Color STORM = new Color(0x8FB8FF);
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
        if (sim == null || !plugin.config().statusBox() || NomadPlugin.photoMode) return null;
        Circle c = sim.bound();
        Tile at = plugin.surface();
        // Streamer layout: the key numbers big enough to read on a stream. Backseat shield: no countdowns here.
        Font big = plugin.config().streamerLayout() ? FontManager.getRunescapeBoldFont().deriveFont(22f) : null;
        boolean shielded = plugin.shielded();
        StreamDesk desk = plugin.desk();
        String voted = desk == null ? null : desk.votedName();
        panelComponent.getChildren().add(TitleComponent.builder()
            .text((c.isMain() && voted != null ? voted : c.name()) + " circle").color(NomadPlugin.colour(c, 255)).build());

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
        else if (plugin.sheltered())
        {
            String wild = plugin.wildShelter();
            state = plugin.kidnapped() ? "Carried off" : sim.intermission() ? "Intermission" : plugin.plotHeld() ? "In a plot"
                : plugin.delveLeft() > 0 ? "Delving" : wild != null ? wild : "Inside";
            colour = INSIDE;
        }
        else
        {
            state = "Inside";
            colour = INSIDE;
        }
        panelComponent.getChildren().add(LineComponent.builder().left("Status").right(state).rightColor(colour).rightFont(big).build());
        if (at != null)
        {
            // Either half of a forked Main counts, so the nearest edge is the one you're inside.
            double toEdge = NomadPlugin.toEdge(sim, at.x, at.y);
            String edge = toEdge >= 0 ? (int) Math.floor(toEdge) + " tiles" : (int) Math.ceil(-toEdge) + " out";
            panelComponent.getChildren().add(LineComponent.builder().left("To edge").right(edge).rightFont(big).build());
        }
        // With boons and timers shown as infoboxes only, the box leaves them out.
        boolean boxRows = plugin.config().boonDisplay() != NomadConfig.BoonDisplay.INFOBOXES;
        if (boxRows && sim.forkWarning() > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Fork in " + sim.forkWarning() + " min")
                .right(NomadSim.compass(sim.forkAxis()) + " / " + NomadSim.compass(sim.forkAxis() + Math.PI))
                .rightColor(FORK).build());
        }
        else if (boxRows && sim.twin() != null)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Forked").right("pick a half").rightColor(FORK).build());
        }
        if (sim.hopWarn() > 0)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Hop in " + sim.hopWarn() + " min")
                .right(sim.hopTo()).rightColor(FORK).build());
        }
        if (boxRows && (sim.stormWarning() > 0 || sim.stormLeft() > 0))
        {
            String kind = NomadPlugin.stormName(sim.stormKind() == NomadSim.GALE);
            panelComponent.getChildren().add(LineComponent.builder()
                .left(sim.stormWarning() > 0 ? kind + " in " + sim.stormWarning() + " min" : kind)
                .right(sim.stormWarning() > 0 ? "over Main" : sim.stormLeft() + " min left").rightColor(STORM).build());
        }
        Radar radar = shielded ? null : plugin.radar();
        boolean hud = plugin.config().hudExtras();
        // A fixed width, so the box doesn't change size as names and numbers come and go.
        panelComponent.setPreferredSize(new Dimension(hud ? WIDTH : ComponentConstants.STANDARD_WIDTH, 0));
        if (radar != null && radar.edgeIn > 0)
        {
            Color urgency = urgency(radar.edgeIn);
            panelComponent.getChildren().add(LineComponent.builder().left("Edge reaches you")
                .right("~" + radar.edgeIn + " min").rightColor(hud ? urgency : radar.edgeIn <= NomadPlugin.EDGE_WARNING ? OUTSIDE : STRANDED)
                .rightFont(big).build());
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
            if (boxRows) for (Effect e : effects(sim, c)) chips.add(e.label.isEmpty() ? e.name : e.name + " " + e.label);
            if (!chips.isEmpty()) panelComponent.getChildren().add(chips);
        }
        panelComponent.getChildren().add(LineComponent.builder().left("Radius").right(Math.round(c.radius()) + " tiles").rightFont(big).build());
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
                .right(near.boon() + " " + Math.round(Math.hypot(near.x() - at.x, near.y() - at.y)) + "t " + NomadPanel.stoneLeft(sim, near))
                .rightColor(new Color(0xFFD54F)).build());
        }
        com.wanderingnomad.sim.Comet comet = sim.comet();
        if (comet != null)
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Comet")
                .right(comet.flying() ? "streaking, " + comet.flightLeft() + "m" : "in " + comet.warning() + "m")
                .rightColor(new Color(0xBFE3FF)).build());
        }
        if (boxRows && !sim.boons().isEmpty())
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Boons").right(String.join(", ", sim.boons())).build());
        }
        if (sim.volcanoOpen())
        {
            panelComponent.getChildren().add(LineComponent.builder().left("Volcano").right("Open").rightColor(INSIDE).build());
        }
        return super.render(graphics);
    }

    /** An active effect: its name, how it reads beside the name, and minutes left (-1 when it has no clock). */
    static final class Effect
    {
        final String name;
        final String label;
        final int minutes;

        Effect(String name, String label, int minutes)
        {
            this.name = name;
            this.label = label;
            this.minutes = minutes;
        }
    }

    private static void timed(List<Effect> out, String name, int minutes)
    {
        out.add(new Effect(name, NomadPanel.time(minutes), minutes));
    }

    /** Every effect running on your circle and the run, in the order the status box shows them. */
    static List<Effect> effects(NomadSim sim, Circle c)
    {
        List<Effect> out = new ArrayList<>();
        if (c.expandLeft() > 0) timed(out, NomadSim.EXPAND, c.expandLeft());
        if (c.tailwindLeft() > 0) timed(out, NomadSim.TAILWIND, c.tailwindLeft());
        if (c.anchorLeft() > 0) timed(out, NomadSim.ANCHOR, c.anchorLeft());
        if (sim.bridgeLeft() > 0) out.add(new Effect(NomadSim.BRIDGE, sim.bridgeLeft() + "m", sim.bridgeLeft()));
        if (sim.calmLeft() > 0) timed(out, NomadSim.CALM, sim.calmLeft());
        if (sim.lanternLit()) out.add(new Effect(NomadSim.LANTERN, "lit", -1));
        if (c.wakeLeft() > 0) timed(out, NomadSim.WAKE, c.wakeLeft());
        if (c.lodeLeft() > 0) timed(out, NomadSim.LODESTONE, c.lodeLeft());
        if (sim.wardLeft() > 0) out.add(new Effect(NomadSim.WARD, sim.wardLeft() + "m", sim.wardLeft()));
        else if (sim.boons().contains(NomadSim.WARD)) out.add(new Effect(NomadSim.WARD, "held", -1));
        if (sim.keystoneLeft() > 0) out.add(new Effect(NomadSim.KEYSTONE, sim.keystoneLeft() + "m", sim.keystoneLeft()));
        else if (sim.keystoneArmed()) out.add(new Effect(NomadSim.KEYSTONE, "armed", -1));
        if (sim.shatterNext()) out.add(new Effect("Shatter", "next", -1));
        if (c.pops() > 0) out.add(new Effect("Log pop", "+" + c.pops(), -1));
        if (c.starHold() > 0) timed(out, "Star hold", c.starHold());
        if (c.starTiles() > 0) out.add(new Effect("Stardust", "+" + c.starTiles(), -1));
        if (sim.catchUp() > 0) out.add(new Effect("Catch-up", sim.catchUp() + "m", sim.catchUp()));
        if (sim.encore() > 0) out.add(new Effect("Encore", sim.encore() + "m", sim.encore()));
        if (sim.tide() != null) out.add(new Effect("Tide", "", -1));
        return out;
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
