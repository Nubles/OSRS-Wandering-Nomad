package com.wanderingnomad;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * Narrator captions: the narrator's line as a lower third near the foot of the game view, fading
 * in and out. Only draws while a caption is up, and never in photo mode.
 */
@Singleton
class NomadCaptionOverlay extends Overlay
{
    private static final long FADE_MS = 500;
    private static final Color BACK = new Color(10, 10, 14, 200);
    private static final Color TEXT = new Color(0xF2EEE6);
    private static final int MAX_WIDTH = 520;

    private final Client client;
    private final NomadPlugin plugin;

    @Inject
    NomadCaptionOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        StreamDesk desk = plugin.desk();
        if (desk == null || NomadPlugin.photoMode) return null;
        long now = System.currentTimeMillis();
        String caption = desk.caption(now);
        if (caption == null) return null;
        long age = now - desk.captionAt();
        float alpha = Math.min(1f, Math.min(age / (float) FADE_MS, (StreamDesk.CAPTION_MS - age) / (float) FADE_MS));
        if (alpha <= 0) return null;

        Font font = FontManager.getRunescapeFont().deriveFont(Font.ITALIC, 16f);
        FontMetrics fm = graphics.getFontMetrics(font);
        int vw = client.getViewportWidth();
        List<String> lines = wrap(caption, fm, Math.min(MAX_WIDTH, vw - 60));
        int pad = 10;
        int lineH = fm.getHeight();
        int textW = 0;
        for (String l : lines) textW = Math.max(textW, fm.stringWidth(l));
        int w = textW + pad * 2 + 6;
        int h = lines.size() * lineH + pad * 2 - 4;
        int x = client.getViewportXOffset() + (vw - w) / 2;
        int y = client.getViewportYOffset() + client.getViewportHeight() - h - 48;

        Composite composite = graphics.getComposite();
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, alpha)));
        graphics.setColor(BACK);
        graphics.fillRect(x, y, w, h);
        graphics.setColor(Moments.STREAM);
        graphics.fillRect(x, y, 4, h);
        graphics.setFont(font);
        graphics.setColor(TEXT);
        int ty = y + pad + fm.getAscent() - 2;
        for (String l : lines)
        {
            graphics.drawString(l, x + pad + 4, ty);
            ty += lineH;
        }
        graphics.setComposite(composite);
        return null;
    }

    private static List<String> wrap(String text, FontMetrics fm, int width)
    {
        List<String> out = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" "))
        {
            String next = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(next) > width && line.length() > 0)
            {
                out.add(line.toString());
                line = new StringBuilder(word);
            }
            else
            {
                line = new StringBuilder(next);
            }
        }
        if (line.length() > 0) out.add(line.toString());
        return out;
    }
}
