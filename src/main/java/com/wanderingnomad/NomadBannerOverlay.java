package com.wanderingnomad;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayDeque;
import java.util.Deque;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;

/**
 * The run's big moments as a banner near the top of the game view: it slides down, holds for a
 * few seconds and fades. Banners queue, so they never overlap. Big moments (a crossing opening,
 * a waystone claimed, a rare waystone) are larger, with a gold trim. Only draws while one is showing.
 */
@Singleton
class NomadBannerOverlay extends Overlay
{
    private static final long SLIDE_MS = 350;
    private static final long HOLD_MS = 3000;
    private static final long FADE_MS = 600;
    /** Never more than this many waiting, so a burst of moments doesn't keep the screen busy for minutes. */
    private static final int MAX_QUEUED = 6;
    private static final Color BACK = new Color(22, 19, 15, 225);
    private static final Color SUB = new Color(0xE8E2D4);

    private final Client client;
    private final Deque<Moments.Banner> queue = new ArrayDeque<>();
    private Moments.Banner showing;
    private long shownAt;

    @Inject
    NomadBannerOverlay(Client client)
    {
        this.client = client;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_WIDGETS);
        setPriority(Overlay.PRIORITY_HIGH);
    }

    /** Queues a banner. Safe from any thread the overlay renders on or the client thread. */
    synchronized void show(Moments.Banner banner)
    {
        if (queue.size() >= MAX_QUEUED) queue.pollFirst();
        queue.addLast(banner);
    }

    synchronized void clear()
    {
        queue.clear();
        showing = null;
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        Moments.Banner b;
        long age;
        synchronized (this)
        {
            long now = System.currentTimeMillis();
            if (showing != null && now - shownAt > SLIDE_MS + HOLD_MS + FADE_MS) showing = null;
            if (showing == null && !queue.isEmpty())
            {
                showing = queue.pollFirst();
                shownAt = now;
            }
            if (showing == null) return null;
            b = showing;
            age = now - shownAt;
        }

        // Slides down while fading in, holds, then fades out where it stands.
        float in = Math.min(1f, age / (float) SLIDE_MS);
        float ease = 1 - (1 - in) * (1 - in) * (1 - in);
        float out = age > SLIDE_MS + HOLD_MS ? Math.max(0f, 1 - (age - SLIDE_MS - HOLD_MS) / (float) FADE_MS) : 1f;
        float alpha = Math.min(ease, out);

        Font titleFont = FontManager.getRunescapeBoldFont().deriveFont(b.big ? 20f : 16f);
        Font subFont = FontManager.getRunescapeFont();
        FontMetrics tm = graphics.getFontMetrics(titleFont);
        FontMetrics sm = graphics.getFontMetrics(subFont);
        int icon = b.big ? 34 : 28;
        int pad = 12;
        int textW = Math.max(tm.stringWidth(b.title), sm.stringWidth(b.subtitle));
        int w = Math.max(b.big ? 300 : 260, pad + icon + 10 + textW + pad + 4);
        int h = b.big ? 64 : 54;
        int vx = client.getViewportXOffset();
        int vw = client.getViewportWidth();
        int x = vx + (vw - w) / 2;
        int y = client.getViewportYOffset() + 34 - (int) ((1 - ease) * 24);

        Composite composite = graphics.getComposite();
        Paint paint = graphics.getPaint();
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));

        RoundRectangle2D box = new RoundRectangle2D.Float(x, y, w, h, 10, 10);
        if (b.big)
        {
            // A soft gold halo around the big ones.
            for (int k = 3; k >= 1; k--)
            {
                graphics.setColor(NomadSceneOverlay.alpha(Moments.GOLD, 26 * k));
                graphics.setStroke(new BasicStroke(k * 3f));
                graphics.draw(new RoundRectangle2D.Float(x - k, y - k, w + k * 2, h + k * 2, 12 + k, 12 + k));
            }
        }
        graphics.setPaint(new GradientPaint(x, y, BACK, x, y + h, new Color(12, 10, 8, 235)));
        graphics.fill(box);
        // An accent of the moment's colour down the left and along the top.
        graphics.setColor(b.colour);
        graphics.fill(new RoundRectangle2D.Float(x, y, 5, h, 4, 4));
        graphics.setPaint(new GradientPaint(x, y, NomadSceneOverlay.alpha(b.colour, 160), x + w, y, NomadSceneOverlay.alpha(b.colour, 0)));
        graphics.fillRect(x + 5, y, w - 10, 2);
        graphics.setPaint(paint);
        graphics.setStroke(new BasicStroke(b.big ? 2f : 1f));
        graphics.setColor(b.big ? Moments.GOLD : NomadSceneOverlay.alpha(b.colour, 120));
        graphics.draw(box);

        int ix = x + pad;
        int iy = y + (h - icon) / 2;
        drawIcon(graphics, b, ix, iy, icon, age);

        int tx = ix + icon + 10;
        int ty = y + (b.big ? 28 : 24);
        shadowed(graphics, titleFont, b.title, tx, ty, b.colour);
        shadowed(graphics, subFont, b.subtitle, tx, ty + sm.getAscent() + 5, SUB);

        graphics.setComposite(composite);
        return null;
    }

    private static void shadowed(Graphics2D g, Font font, String text, int x, int y, Color colour)
    {
        g.setFont(font);
        g.setColor(Color.BLACK);
        g.drawString(text, x + 1, y + 1);
        g.setColor(colour);
        g.drawString(text, x, y);
    }

    /** A small drawn emblem for each kind of moment, with a gentle pulse. The sidebar's timeline uses it too. */
    static void drawIcon(Graphics2D g, Moments.Banner b, int x, int y, int s, long age)
    {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double pulse = 0.5 + 0.5 * Math.sin(age / 220.0);
        float cx = x + s / 2f;
        float cy = y + s / 2f;
        g.setColor(NomadSceneOverlay.alpha(b.colour, (int) (40 + 40 * pulse)));
        g.fill(new Ellipse2D.Float(x - 2, y - 2, s + 4, s + 4));
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.setColor(b.colour);
        float r = s * 0.32f;
        switch (b.icon)
        {
            case RING:
                g.draw(new Ellipse2D.Float(cx - r, cy - r, r * 2, r * 2));
                g.fill(new Ellipse2D.Float(cx - 2.5f, cy - 2.5f, 5, 5));
                break;
            case CROSSING:
                g.draw(new Ellipse2D.Float(cx - r * 1.45f, cy - r * 0.8f, r * 1.6f, r * 1.6f));
                g.draw(new Ellipse2D.Float(cx - r * 0.15f, cy - r * 0.8f, r * 1.6f, r * 1.6f));
                break;
            case WAYSTONE:
            {
                Path2D d = new Path2D.Float();
                d.moveTo(cx, cy - r * 1.3f);
                d.lineTo(cx + r * 0.85f, cy);
                d.lineTo(cx, cy + r * 1.3f);
                d.lineTo(cx - r * 0.85f, cy);
                d.closePath();
                g.fill(d);
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(1f));
                g.draw(d);
                break;
            }
            case BOON:
            case STAR:
            {
                Path2D star = new Path2D.Float();
                int points = b.icon == Moments.Icon.STAR ? 5 : 4;
                for (int k = 0; k < points * 2; k++)
                {
                    double a = -Math.PI / 2 + k * Math.PI / points;
                    double rr = k % 2 == 0 ? r * 1.35 : r * 0.5;
                    double px = cx + Math.cos(a) * rr;
                    double py = cy + Math.sin(a) * rr;
                    if (k == 0) star.moveTo(px, py);
                    else star.lineTo(px, py);
                }
                star.closePath();
                g.fill(star);
                break;
            }
            case MOOD:
            {
                Path2D swirl = new Path2D.Float();
                swirl.moveTo(cx - r, cy + r * 0.6f);
                swirl.curveTo(cx - r, cy - r * 1.2f, cx + r * 1.2f, cy - r * 1.2f, cx + r * 0.9f, cy + r * 0.2f);
                g.draw(swirl);
                Path2D head = new Path2D.Float();
                head.moveTo(cx + r * 0.9f, cy + r * 0.75f);
                head.lineTo(cx + r * 0.35f, cy);
                head.lineTo(cx + r * 1.4f, cy - r * 0.05f);
                head.closePath();
                g.fill(head);
                break;
            }
            case EDGE:
            {
                Path2D tri = new Path2D.Float();
                tri.moveTo(cx, cy - r * 1.3f);
                tri.lineTo(cx + r * 1.3f, cy + r);
                tri.lineTo(cx - r * 1.3f, cy + r);
                tri.closePath();
                g.draw(tri);
                g.fillRect((int) cx - 1, (int) (cy - r * 0.45f), 3, (int) (r * 0.8f));
                g.fillRect((int) cx - 1, (int) (cy + r * 0.5f), 3, 3);
                break;
            }
            case FORK:
            {
                // One circle splitting into two, drifting apart.
                float h = r * 0.75f;
                g.draw(new Ellipse2D.Float(cx - r * 1.35f, cy - h * 0.5f - h, h * 2, h * 2));
                g.draw(new Ellipse2D.Float(cx + r * 1.35f - h * 2, cy + h * 0.5f - h, h * 2, h * 2));
                g.fill(new Ellipse2D.Float(cx - 2, cy - 2, 4, 4));
                break;
            }
            default:
                break;
        }
    }
}
