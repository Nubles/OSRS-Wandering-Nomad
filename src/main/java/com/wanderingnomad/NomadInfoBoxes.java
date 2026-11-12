package com.wanderingnomad;

import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.overlay.infobox.InfoBox;
import net.runelite.client.ui.overlay.infobox.InfoBoxManager;

/**
 * Boon infoboxes: active boons, the boons you hold, the omen, a coming fork and a storm as
 * standard RuneLite infoboxes, for anyone who keeps their timers there. Each has a small drawn
 * badge and a tooltip saying what it is. Client thread.
 */
@Singleton
class NomadInfoBoxes
{
    private static final Color GOLD = new Color(0xFFD54F);
    private static final Color OMEN = new Color(0xC9A6FF);

    private final InfoBoxManager manager;
    private final Map<String, Box> boxes = new LinkedHashMap<>();
    private final Map<String, BufferedImage> badges = new HashMap<>();

    @Inject
    NomadInfoBoxes(InfoBoxManager manager)
    {
        this.manager = manager;
    }

    static final class Box extends InfoBox
    {
        private final String key;
        private String text = "";
        private Color colour = Color.WHITE;

        Box(String key, BufferedImage image, Plugin plugin)
        {
            super(image, plugin);
            this.key = key;
        }

        @Override
        public String getText()
        {
            return text;
        }

        @Override
        public Color getTextColor()
        {
            return colour;
        }

        @Override
        public String getName()
        {
            return "NomadUIM_" + key;
        }
    }

    /** Brings the infoboxes in line with the run; none when {@code on} is false. */
    void sync(Plugin plugin, NomadSim sim, Omen omen, boolean on)
    {
        Map<String, Object[]> want = new LinkedHashMap<>();
        if (on && sim != null)
        {
            Circle c = sim.bound();
            for (NomadStatusOverlay.Effect e : NomadStatusOverlay.effects(sim, c))
            {
                want.put("boon:" + e.name, new Object[]{e.name, e.minutes >= 0 ? shortTime(e.minutes) : e.label, GOLD,
                    e.name + (e.label.isEmpty() ? "" : ": " + e.label) + describe(e.name)});
            }
            if (!sim.boons().isEmpty())
            {
                want.put("held", new Object[]{"Boons", sim.boons().size() + "/" + NomadSim.BOONS_HELD, GOLD,
                    "Boons held: " + String.join(", ", sim.boons()) + ". Spend them from the sidebar."});
            }
            if (sim.forkWarning() > 0)
            {
                want.put("fork", new Object[]{"Fork", sim.forkWarning() + "m", NomadStatusOverlay.FORK,
                    "Main forks in " + sim.forkWarning() + " min: one half heads " + NomadSim.compass(sim.forkAxis()) + ", the other "
                        + NomadSim.compass(sim.forkAxis() + Math.PI) + ". The half you stand in lives on."});
            }
            else if (sim.twin() != null)
            {
                want.put("fork", new Object[]{"Fork", "pick", NomadStatusOverlay.FORK, "Main has forked: stand in the half you want to keep."});
            }
            if (sim.stormWarning() > 0 || sim.stormLeft() > 0)
            {
                String kind = NomadPlugin.stormName(sim.stormKind() == NomadSim.GALE);
                boolean coming = sim.stormWarning() > 0;
                want.put("storm", new Object[]{"Storm", (coming ? sim.stormWarning() : sim.stormLeft()) + "m", NomadStatusOverlay.STORM,
                    coming ? kind + " over Main in " + sim.stormWarning() + " min" : kind + " over Main: " + sim.stormLeft() + " min left"});
            }
            if (omen != null)
            {
                int left = omen.deadline() - sim.minutes();
                want.put("omen", new Object[]{"Omen", left > 0 ? shortTime(left) : "", OMEN, "Omen, " + omen.title() + ": " + omen.ask()});
            }
        }
        for (Iterator<Map.Entry<String, Box>> it = boxes.entrySet().iterator(); it.hasNext(); )
        {
            Map.Entry<String, Box> e = it.next();
            if (want.containsKey(e.getKey())) continue;
            manager.removeInfoBox(e.getValue());
            it.remove();
        }
        for (Map.Entry<String, Object[]> e : want.entrySet())
        {
            Object[] w = e.getValue();
            Box b = boxes.get(e.getKey());
            if (b == null)
            {
                b = new Box(e.getKey(), badge((String) w[0], (Color) w[2]), plugin);
                boxes.put(e.getKey(), b);
                manager.addInfoBox(b);
            }
            b.text = (String) w[1];
            b.colour = (Color) w[2];
            b.setTooltip((String) w[3]);
        }
    }

    void clear()
    {
        for (Box b : boxes.values()) manager.removeInfoBox(b);
        boxes.clear();
    }

    private static String describe(String boon)
    {
        String d = NomadPanel.describe(boon);
        return d == null || d.isEmpty() ? "" : ". " + d + ".";
    }

    /** "45m" under an hour, "3h" over. */
    static String shortTime(int minutes)
    {
        return minutes < 60 ? minutes + "m" : minutes / 60 + "h";
    }

    /** A round badge in the thing's colour with its first two letters. */
    private BufferedImage badge(String name, Color colour)
    {
        String key = name + colour.getRGB();
        BufferedImage cached = badges.get(key);
        if (cached != null) return cached;
        BufferedImage img = new BufferedImage(32, 32, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(24, 20, 16, 235));
        g.fillOval(2, 2, 28, 28);
        g.setStroke(new BasicStroke(2.5f));
        g.setColor(colour);
        g.drawOval(3, 3, 26, 26);
        String glyph = name.length() <= 2 ? name : name.substring(0, 1).toUpperCase() + name.substring(1, 2).toLowerCase();
        Font font = FontManager.getRunescapeBoldFont().deriveFont(14f);
        g.setFont(font);
        FontMetrics fm = g.getFontMetrics();
        int x = 16 - fm.stringWidth(glyph) / 2;
        int y = 16 + (fm.getAscent() - fm.getDescent()) / 2;
        g.setColor(Color.BLACK);
        g.drawString(glyph, x + 1, y + 1);
        g.setColor(colour);
        g.drawString(glyph, x, y);
        g.dispose();
        badges.put(key, img);
        return img;
    }
}
