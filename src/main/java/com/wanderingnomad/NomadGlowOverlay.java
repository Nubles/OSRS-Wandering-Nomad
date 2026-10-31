package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import javax.inject.Inject;
import net.runelite.api.Client;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The game view's border glows amber as the edge closes in, sooner when the Radar sees it
 * coming, and pulses red while you're outside. Underground it follows the surface tile you count as on.
 */
class NomadGlowOverlay extends Overlay
{
    /** Tiles from the edge at which the glow starts. */
    private static final double NEAR = 6;
    private static final int DEPTH = 70;
    private static final Color WARM = new Color(0xFFB040);
    private static final Color OUT = new Color(0xFF3B30);

    private final Client client;
    private final NomadPlugin plugin;

    @Inject
    NomadGlowOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
        setPriority(Overlay.PRIORITY_LOW);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        Tile at = plugin.surface();
        if (sim == null || at == null || !plugin.config().edgeGlow()) return null;
        Circle c = sim.bound();
        double toEdge = NomadPlugin.toEdge(sim, at.x, at.y);
        double strength;
        Color colour;
        if (toEdge < 0)
        {
            colour = OUT;
            strength = 0.7 + 0.3 * Math.sin(System.currentTimeMillis() / 250.0);
        }
        else
        {
            colour = WARM;
            strength = Math.max(0, (NEAR - toEdge) / NEAR);
            Radar radar = plugin.radar();
            if (radar != null && radar.edgeIn > 0) strength = Math.max(strength, (Radar.MINUTES + 1 - radar.edgeIn) / (double) (Radar.MINUTES + 1) * 0.8);
            strength *= 0.85 + 0.15 * Math.sin(System.currentTimeMillis() / 600.0);
        }
        if (strength <= 0.02) return null;

        int x = client.getViewportXOffset();
        int y = client.getViewportYOffset();
        int w = client.getViewportWidth();
        int h = client.getViewportHeight();
        int a = (int) (150 * Math.min(1, strength));
        Color edge = NomadSceneOverlay.alpha(colour, a);
        Color clear = NomadSceneOverlay.alpha(colour, 0);
        int d = Math.min(DEPTH, Math.min(w, h) / 4);
        Paint before = graphics.getPaint();
        graphics.setPaint(new GradientPaint(x, y, edge, x, y + d, clear));
        graphics.fillRect(x, y, w, d);
        graphics.setPaint(new GradientPaint(x, y + h, edge, x, y + h - d, clear));
        graphics.fillRect(x, y + h - d, w, d);
        graphics.setPaint(new GradientPaint(x, y, edge, x + d, y, clear));
        graphics.fillRect(x, y, d, h);
        graphics.setPaint(new GradientPaint(x + w, y, edge, x + w - d, y, clear));
        graphics.fillRect(x + w - d, y, d, h);
        graphics.setPaint(before);
        return null;
    }
}
