package com.wanderingnomad.stream;

import com.wanderingnomad.journey.Epithets;
import com.wanderingnomad.journey.Sigil;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Run wrapped: every 100 played hours, six stat cards for sharing, the six most out-of-the-ordinary
 * of a pool of 30 ("12% of your life within 3 tiles of the edge").
 */
public final class Wrapped
{
    /** Played minutes between wraps. */
    public static final int EVERY = 6000;
    public static final int CARDS = 6;
    /** Lumbridge to Varrock, in tiles, for the walking card. */
    static final int LUMBRIDGE_TO_VARROCK = 220;

    public static final class Card
    {
        public final String big;
        public final String line;
        final double score;

        Card(String big, String line, double score)
        {
            this.big = big;
            this.line = line;
            this.score = score;
        }
    }

    /** One stat of the pool: its metric, how much is ordinary per 100 hours, and its words. */
    private static final class Stat
    {
        final String key;
        final double par;
        final String line;
        final String unit;

        Stat(String key, double par, String unit, String line)
        {
            this.key = key;
            this.par = par;
            this.unit = unit;
            this.line = line;
        }
    }

    static final String PERCENT = "%";
    static final String TIME = "time";
    static final String COUNT = "";

    private static final List<Stat> POOL = new ArrayList<>();

    static
    {
        POOL.add(new Stat(Session.EDGE_MINUTES, 10, PERCENT, "of your life within 3 tiles of the edge"));
        POOL.add(new Stat(Session.OUT_MINUTES, 4, PERCENT, "of your life outside your circle"));
        POOL.add(new Stat(NearMiss.METRIC, 10, COUNT, "close calls: the edge within a tile, and you held"));
        POOL.add(new Stat(Epithets.WALKED, 20000, COUNT, "tiles walked, Lumbridge to Varrock {trips} times over"));
        POOL.add(new Stat(Epithets.CROSSINGS, 15, COUNT, "crossings from one circle into another"));
        POOL.add(new Stat(Epithets.FORKS, 40, COUNT, "forks, and a half kept every time"));
        POOL.add(new Stat(Epithets.VOYAGES, 5, COUNT, "voyages at sea with Main"));
        POOL.add(new Stat(Epithets.STORMS, 10, COUNT, "storms weathered"));
        POOL.add(new Stat(Epithets.CLAIMED, 30, COUNT, "waystones claimed"));
        POOL.add(new Stat(Epithets.BOONS, 25, COUNT, "boons spent"));
        POOL.add(new Stat(Epithets.STREAK, 600, TIME, "your longest stretch inside without a step out"));
        POOL.add(new Stat(Epithets.SQUARES, 150, COUNT, "map squares walked inside your circle"));
        POOL.add(new Stat(Epithets.LANDMARKS, 40, COUNT, "landmarks stood at"));
        POOL.add(new Stat(Epithets.REGIONS, 6, COUNT, "kingdoms walked with the circle"));
        POOL.add(new Stat(Epithets.OMENS, 15, COUNT, "omens met"));
        POOL.add(new Stat(Epithets.BOSSES, 5, COUNT, "kinds of boss killed"));
        POOL.add(new Stat(Epithets.COLLECTION, 80, COUNT, "collection log slots filled"));
        POOL.add(new Stat(Epithets.KILLS, 3000, COUNT, "kills inside the circle"));
        POOL.add(new Stat(Epithets.HARBOURS, 6, COUNT, "harbours called at"));
        POOL.add(new Stat(Epithets.COMEBACKS, 5, COUNT, "comebacks from the brink"));
        POOL.add(new Stat(Epithets.CLUES, 20, COUNT, "clue scrolls done"));
        POOL.add(new Stat(Epithets.SPECIES, 60, COUNT, "kinds of monster in the Bestiary"));
        POOL.add(new Stat(Epithets.SCARS, 5, COUNT, "scars on your edge"));
        POOL.add(new Stat(Epithets.CASTAWAYS, 1, COUNT, "times the sea cast Main away"));
        POOL.add(new Stat(Epithets.COMETS, 2, COUNT, "comets caught"));
        POOL.add(new Stat(Epithets.STARS, 5, COUNT, "layers of fallen star mined"));
        POOL.add(new Stat(Epithets.HOMEWARD, 3, COUNT, "times Main came home with you"));
        POOL.add(new Stat(Epithets.TASKS, 40, COUNT, "combat tasks done"));
        POOL.add(new Stat(Epithets.RENDEZVOUS, 1, COUNT, "rendezvous with a rival"));
        POOL.add(new Stat(Echoes.METRIC, 5, COUNT, "echoes from places you came back to"));
    }

    private Wrapped()
    {
    }

    /** How many stats the pool holds. */
    public static int pool()
    {
        return POOL.size();
    }

    /** The six most extreme cards for a run's stats after {@code minutes} played. */
    public static List<Card> of(Map<String, Integer> stats, int minutes)
    {
        double hundreds = Math.max(0.01, minutes / (double) EVERY);
        List<Card> cards = new ArrayList<>();
        for (Stat s : POOL)
        {
            Integer raw = stats.get(s.key);
            int v = raw == null ? 0 : raw;
            if (PERCENT.equals(s.unit))
            {
                double pct = minutes <= 0 ? 0 : v * 100.0 / minutes;
                double score = Math.abs(Math.log((pct + 0.5) / (s.par + 0.5)));
                cards.add(new Card(pct < 10 ? String.format("%.1f%%", pct) : Math.round(pct) + "%", s.line, score));
                continue;
            }
            if (v <= 0) continue;
            double expected = TIME.equals(s.unit) ? s.par : s.par * hundreds;
            double score = Math.log((v + 1.0) / (expected + 1.0));
            String big = TIME.equals(s.unit) ? Previously.hours(v) : String.format("%,d", v);
            String line = s.line.replace("{trips}", String.valueOf(Math.max(1, v / LUMBRIDGE_TO_VARROCK)));
            cards.add(new Card(big, line, score));
        }
        cards.sort(Comparator.comparingDouble((Card c) -> -c.score));
        return new ArrayList<>(cards.subList(0, Math.min(CARDS, cards.size())));
    }

    /** The cards as a shareable picture, three across and two down. */
    public static BufferedImage render(String title, String subtitle, List<Card> cards)
    {
        int w = 1200;
        int h = 760;
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(0x15161A));
        g.fillRect(0, 0, w, h);
        g.setColor(new Color(0xE8D9A8));
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 44));
        g.drawString(title, 48, 78);
        g.setColor(new Color(0xA9A9A9));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 22));
        g.drawString(subtitle, 50, 114);
        int cw = 352;
        int ch = 268;
        for (int i = 0; i < cards.size(); i++)
        {
            int x = 48 + (i % 3) * (cw + 24);
            int y = 150 + (i / 3) * (ch + 24);
            Color accent = Sigil.actColour(i);
            g.setColor(new Color(0x23252B));
            g.fill(new RoundRectangle2D.Float(x, y, cw, ch, 28, 28));
            g.setColor(accent);
            g.fillRect(x + 24, y + 24, 44, 6);
            g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 64));
            g.drawString(cards.get(i).big, x + 24, y + 112);
            g.setColor(new Color(0xE0E0E0));
            g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 24));
            wrap(g, cards.get(i).line, x + 24, y + 156, cw - 48, 30);
        }
        g.setColor(new Color(0x6F6F6F));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 18));
        g.drawString("Nomad UIM", w - 150, h - 24);
        g.dispose();
        return img;
    }

    private static void wrap(Graphics2D g, String text, int x, int y, int width, int lineHeight)
    {
        FontMetrics fm = g.getFontMetrics();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" "))
        {
            String next = line.length() == 0 ? word : line + " " + word;
            if (fm.stringWidth(next) > width && line.length() > 0)
            {
                g.drawString(line.toString(), x, y);
                y += lineHeight;
                line = new StringBuilder(word);
            }
            else
            {
                line = new StringBuilder(next);
            }
        }
        if (line.length() > 0) g.drawString(line.toString(), x, y);
    }
}
