package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar, top to bottom: a header card with your circle and the edge countdown, a live mini
 * map, your boons, the risen waystones, the crossings, the run's tallies, a timeline of its moments,
 * and buttons for the website, the run code and a new run. Every piece has a fixed width, buttons
 * are only rebuilt when they change, and it only ever shows a {@link View} taken on the client thread.
 */
class NomadPanel extends PluginPanel
{
    /** The width inside the sidebar's 10px borders. */
    static final int CONTENT_WIDTH = PANEL_WIDTH - 20;
    /** Boon minutes when fresh, for the time-left bars. */
    private static final int EXPAND_MINUTES = 120;
    private static final int TAILWIND_MINUTES = 60;
    private static final int ANCHOR_MINUTES = 60;
    private static final int BRIDGE_MINUTES = 10;

    private final IntConsumer onCross;
    private final BiConsumer<Integer, Integer> onBoon;
    private final SidebarParts.Header header = new SidebarParts.Header();
    private final SidebarParts.MiniMap map = new SidebarParts.MiniMap();
    private final JLabel boonsTitle = heading("Boons");
    private final JPanel boons = column();
    private final SidebarParts.Active active = new SidebarParts.Active();
    private final SidebarParts.Waystones waystones = new SidebarParts.Waystones();
    private final JPanel crossings = column();
    private final SidebarParts.Stats stats = new SidebarParts.Stats();
    private final SidebarParts.Timeline timeline = new SidebarParts.Timeline();
    private final JTextArea logArea = new JTextArea();
    private final List<SidebarParts.Part> drawn = new ArrayList<>();
    private String shownBoons;
    private String shownCrossings;

    NomadPanel(IntConsumer onCross, BiConsumer<Integer, Integer> onBoon, Runnable onNewRun, Runnable onViewOnWebsite,
        Runnable onCopyRunCode, Runnable onCopyLiveLink)
    {
        this.onBoon = onBoon;
        this.onCross = onCross;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(header);
        add(gap(8));
        add(map);
        add(gap(10));
        add(boonsTitle);
        add(gap(4));
        add(boons);
        add(gap(4));
        add(active);
        add(gap(8));
        add(heading("Waystones"));
        add(gap(4));
        add(waystones);
        add(gap(8));
        add(heading("Crossings"));
        add(gap(4));
        add(crossings);
        add(gap(10));
        add(heading("Run"));
        add(gap(4));
        add(stats);
        add(gap(10));
        add(heading("Moments"));
        add(gap(4));
        JScrollPane scroll = new JScrollPane(timeline, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        fix(scroll, 186);
        add(scroll);
        add(gap(10));
        add(heading("Run log"));
        add(gap(4));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        logArea.setForeground(Color.LIGHT_GRAY);
        logArea.setFont(FontManager.getRunescapeSmallFont());
        logArea.setMargin(new Insets(4, 6, 4, 6));
        JScrollPane logScroll = new JScrollPane(logArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED,
            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        logScroll.setBorder(BorderFactory.createEmptyBorder());
        fix(logScroll, 110);
        add(logScroll);
        add(gap(10));

        JPanel buttons = new JPanel(new GridLayout(0, 1, 0, 4));
        buttons.setOpaque(false);
        buttons.add(button("Copy run code", "Copies a code you can paste on the website or share", onCopyRunCode));
        buttons.add(button("View on website", "Opens your circles on the Nomad UIM website, exactly where they are now", onViewOnWebsite));
        buttons.add(button("Copy live link", "A link that follows your run as you play (turn on Share live in the plugin's settings)", onCopyLiveLink));
        JButton newRun = new JButton("Start a new run");
        newRun.addActionListener(e ->
        {
            int ok = JOptionPane.showConfirmDialog(this,
                "This deletes the current run on this account and starts a new one. Continue?",
                "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) onNewRun.run();
        });
        buttons.add(newRun);
        fix(buttons, 4 * 26 + 12);
        add(buttons);

        drawn.add(header);
        drawn.add(map);
        drawn.add(active);
        drawn.add(waystones);
        drawn.add(stats);
        // BoxLayout lines children up by their alignment, which differs between labels (left) and
        // panels (centre); with both, rows slide sideways whenever something changes width.
        for (Component child : getComponents())
        {
            if (child instanceof JComponent) ((JComponent) child).setAlignmentX(LEFT_ALIGNMENT);
        }
        display(View.of(null, null, Collections.<String>emptyList(), null, null, null, null, Collections.<Moment>emptyList(), false));
    }

    /**
     * Adds a row of testing buttons at the bottom, for a developer client: the same shortcuts as
     * the ::nomad commands. They really change the run.
     */
    void addTestButtons(String[] labels, Runnable[] actions)
    {
        JComponent title = heading("Testing (changes your run)");
        title.setAlignmentX(LEFT_ALIGNMENT);
        add(gap(10));
        add(title);
        add(gap(4));
        JPanel grid = new JPanel(new GridLayout(0, 2, 4, 4));
        grid.setOpaque(false);
        for (int i = 0; i < labels.length; i++) grid.add(small(labels[i], labels[i], actions[i]));
        fix(grid, ((labels.length + 1) / 2) * 26);
        add(grid);
        revalidate();
    }

    void display(View v)
    {
        for (SidebarParts.Part p : drawn) p.show(v);
        boonsTitle.setText(("Boons " + v.held + "/" + NomadSim.BOONS_HELD).toUpperCase());

        // Buttons are only rebuilt when they change, so they don't blink or lose the mouse on every update.
        StringBuilder boonKey = new StringBuilder().append(v.live);
        for (BoonSlot b : v.boons) boonKey.append(b.index).append(b.name).append('|');
        if (!boonKey.toString().equals(shownBoons))
        {
            shownBoons = boonKey.toString();
            boons.removeAll();
            for (int i = 0; i < NomadSim.BOONS_HELD; i++)
            {
                boons.add(boonRow(i < v.boons.size() ? v.boons.get(i) : null, v));
                if (i < NomadSim.BOONS_HELD - 1) boons.add(gap(4));
            }
            fix(boons, NomadSim.BOONS_HELD * SidebarParts.BoonCard.H + (NomadSim.BOONS_HELD - 1) * 4);
            boons.revalidate();
        }
        for (Component row : boons.getComponents())
        {
            if (!(row instanceof JPanel)) continue;
            for (Component c : ((JPanel) row).getComponents())
            {
                if (c instanceof SidebarParts.BoonCard) ((SidebarParts.BoonCard) c).show(v);
            }
        }

        StringBuilder crossKey = new StringBuilder().append(v.live);
        for (CrossRow c : v.crossings) crossKey.append(c.index).append(c.status).append(c.can).append('|');
        if (!crossKey.toString().equals(shownCrossings))
        {
            shownCrossings = crossKey.toString();
            crossings.removeAll();
            if (v.crossings.isEmpty()) crossings.add(note(v.live ? "No circle to cross into from here." : " "));
            for (CrossRow c : v.crossings) crossings.add(crossRow(c));
            int rows = Math.max(1, v.crossings.size());
            fix(crossings, rows * 34);
            crossings.revalidate();
        }

        timeline.show(v.moments);
        String log = String.join("\n", v.log);
        if (!log.equals(logArea.getText())) logArea.setText(log);
        revalidate();
    }

    /** A boon card, with its Use button (or a Rudder's two) beside it. */
    private JPanel boonRow(BoonSlot slot, View v)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        if (slot == null)
        {
            SidebarParts.BoonCard card = new SidebarParts.BoonCard(null, CONTENT_WIDTH);
            card.show(v);
            row.add(card, BorderLayout.CENTER);
            fix(row, SidebarParts.BoonCard.H);
            return row;
        }
        JPanel side = new JPanel(new GridLayout(0, 1, 0, 2));
        side.setOpaque(false);
        if (NomadSim.RUDDER.equals(slot.name))
        {
            side.add(small("Left", "Turn your circle 45 degrees left", () -> onBoon.accept(slot.index, 1)));
            side.add(small("Right", "Turn your circle 45 degrees right", () -> onBoon.accept(slot.index, -1)));
        }
        else
        {
            side.add(small("Use", slot.description, () -> onBoon.accept(slot.index, 0)));
        }
        int sideW = 50;
        side.setPreferredSize(new Dimension(sideW, SidebarParts.BoonCard.H));
        SidebarParts.BoonCard card = new SidebarParts.BoonCard(slot, CONTENT_WIDTH - sideW - 4);
        card.show(v);
        row.add(card, BorderLayout.CENTER);
        row.add(side, BorderLayout.EAST);
        fix(row, SidebarParts.BoonCard.H);
        return row;
    }

    /** A crossing: the circle and how far off it is, with a Cross button once you can. */
    private JPanel crossRow(CrossRow c)
    {
        JPanel row = new JPanel(new BorderLayout(4, 0));
        row.setOpaque(false);
        SidebarParts.Part info = new SidebarParts.Part(30)
        {
            @Override
            void paint(Graphics2D g, View v)
            {
                SidebarParts.card(g, 0, 0, getWidth(), 30, c.colour);
                Font bold = FontManager.getRunescapeBoldFont();
                Font smallFont = FontManager.getRunescapeSmallFont();
                FontMetrics sm = g.getFontMetrics(smallFont);
                SidebarParts.text(g, bold, SidebarParts.fit(g.getFontMetrics(bold), c.name, getWidth() - 14), 10, 13, c.colour);
                SidebarParts.text(g, smallFont, SidebarParts.fit(sm, c.status, getWidth() - 14), 10, 25,
                    c.open ? SidebarParts.IN : Color.LIGHT_GRAY);
            }
        };
        info.view = View.EMPTY;
        int buttonW = c.can ? 56 : 0;
        Dimension size = new Dimension(CONTENT_WIDTH - buttonW - (c.can ? 4 : 0), 30);
        info.setPreferredSize(size);
        info.setMinimumSize(size);
        info.setMaximumSize(size);
        row.add(info, BorderLayout.CENTER);
        if (c.can)
        {
            JButton cross = small("Cross", "Cross into " + c.name, () -> onCross.accept(c.index));
            cross.setPreferredSize(new Dimension(buttonW, 30));
            row.add(cross, BorderLayout.EAST);
        }
        fix(row, 30);
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(row, BorderLayout.NORTH);
        fix(wrap, 34);
        return wrap;
    }

    private static JButton button(String label, String tip, Runnable action)
    {
        JButton b = new JButton(label);
        b.setToolTipText(tip);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JButton small(String label, String tip, Runnable action)
    {
        JButton b = button(label, tip, action);
        b.setMargin(new Insets(0, 2, 0, 2));
        b.setFont(FontManager.getRunescapeSmallFont());
        return b;
    }

    private static JLabel note(String text)
    {
        JLabel l = new JLabel(text);
        l.setFont(FontManager.getRunescapeSmallFont());
        l.setForeground(SidebarParts.MUTED);
        return l;
    }

    private static JPanel column()
    {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setOpaque(false);
        return p;
    }

    /** Pins a component to the sidebar's width and the given height, so nothing around it moves. */
    private static void fix(JComponent c, int height)
    {
        Dimension size = new Dimension(CONTENT_WIDTH, height);
        c.setPreferredSize(size);
        c.setMinimumSize(size);
        c.setMaximumSize(size);
        c.setAlignmentX(LEFT_ALIGNMENT);
    }

    static String time(int minutes)
    {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }

    private static JLabel heading(String text)
    {
        JLabel l = new JLabel(text.toUpperCase());
        l.setForeground(Color.GRAY);
        l.setFont(FontManager.getRunescapeSmallFont());
        return l;
    }

    private static Component gap(int height)
    {
        JComponent gap = (JComponent) Box.createRigidArea(new Dimension(CONTENT_WIDTH, height));
        // Left-aligned like everything else: BoxLayout shifts and squeezes rows when alignments are mixed.
        gap.setAlignmentX(LEFT_ALIGNMENT);
        return gap;
    }

    /** What each boon does. */
    static String describe(String boon)
    {
        switch (boon)
        {
            case NomadSim.EXPAND:
                return "Your circle grows by 12 tiles for 2 hours";
            case NomadSim.TAILWIND:
                return "Your circle drifts twice as fast for 1 hour";
            case NomadSim.ANCHOR:
                return "Your circle stops drifting for 1 hour";
            case NomadSim.RUDDER:
                return "Turn your circle 45 degrees, once";
            case NomadSim.BRIDGE:
                return "For 10 minutes, cross to any circle within 60 tiles";
            default:
                return boon;
        }
    }

    /** The boons working on a circle, such as "<br>Tailwind 34m · Expand 1h 50m". */
    static String effects(Circle c)
    {
        List<String> on = new ArrayList<>();
        if (c.expandLeft() > 0) on.add("Expand " + time(c.expandLeft()));
        if (c.tailwindLeft() > 0) on.add("Tailwind " + time(c.tailwindLeft()));
        if (c.anchorLeft() > 0) on.add("Anchor " + time(c.anchorLeft()));
        return on.isEmpty() ? "" : "<br>" + String.join(" · ", on);
    }

    static String roughly(int minutes)
    {
        if (minutes >= 600) return "10h+";
        int m = Math.max(5, (minutes + 4) / 5 * 5);
        return m < 60 ? m + "m" : (m / 60) + "h " + String.format("%02d", m % 60) + "m";
    }

    /**
     * A moment for the timeline: when it happened, in played minutes, and its banner. Steps in and
     * out along the edge in quick succession share one moment, counted.
     */
    static final class Moment
    {
        final int minute;
        final Moments.Banner banner;
        /** The wall-clock time it first happened, for folding quick repeats together. */
        final long at;
        final int count;

        Moment(int minute, Moments.Banner banner, long at, int count)
        {
            this.minute = minute;
            this.banner = banner;
            this.at = at;
            this.count = count;
        }

        String title()
        {
            return count > 1 ? "In and out ×" + count : banner.title;
        }

        String subtitle()
        {
            return count > 1 ? "Along the edge. Now " + (banner.event == Moments.Event.LEFT ? "outside." : "inside.") : banner.subtitle;
        }
    }

    static final class MapCircle
    {
        final double x;
        final double y;
        final double r;
        final Color colour;
        final String name;
        final boolean yours;
        final boolean linked;
        /** Tiles between its edge and yours. */
        final double gap;

        MapCircle(Circle c, boolean yours, boolean linked, double gap)
        {
            this.x = c.x();
            this.y = c.y();
            this.r = c.radius();
            this.colour = NomadPlugin.colour(c, 255);
            this.name = c.name();
            this.yours = yours;
            this.linked = linked;
            this.gap = gap;
        }
    }

    static final class MapStone
    {
        final double x;
        final double y;
        final Color colour;

        MapStone(double x, double y, Color colour)
        {
            this.x = x;
            this.y = y;
            this.colour = colour;
        }
    }

    static final class BoonSlot
    {
        final int index;
        final String name;
        final String description;
        final boolean rare;

        BoonSlot(int index, String name)
        {
            this.index = index;
            this.name = name;
            this.description = describe(name);
            this.rare = Moments.rare(name);
        }
    }

    static final class ActiveBoon
    {
        final String name;
        final int left;
        final int total;

        ActiveBoon(String name, int left, int total)
        {
            this.name = name;
            this.left = left;
            this.total = Math.max(total, left);
        }
    }

    static final class StoneRow
    {
        String boon;
        boolean rare;
        boolean yours;
        Color colour;
        long distance;
        /** Which way it lies from you on the map, or NaN if your tile isn't known. */
        double angle = Double.NaN;
        String where;
        String left;
        double sortKey;
    }

    static final class CrossRow
    {
        final int index;
        final String name;
        final Color colour;
        final boolean open;
        final boolean can;
        final String status;

        CrossRow(int index, String name, Color colour, boolean open, boolean can, String status)
        {
            this.index = index;
            this.name = name;
            this.colour = colour;
            this.open = open;
            this.can = can;
            this.status = status;
        }
    }

    /** Everything the panel shows, worked out on the client thread. */
    static final class View
    {
        static final View EMPTY = new View();

        boolean live;
        String name = "Nomad UIM";
        Color colour = Color.WHITE;
        String state = "";
        Color stateColour = Color.WHITE;
        boolean outside;
        int edgeIn = -1;
        String mood = "";
        double heading;
        double radius;
        String played = "0h 00m";
        String where = "";
        long seed;

        double cx;
        double cy;
        double px = Double.NaN;
        double py = Double.NaN;
        final List<MapCircle> circles = new ArrayList<>();
        final List<MapStone> stones = new ArrayList<>();
        double[][] cone;
        /** Main's centre and radius, for the fork's arrows. */
        double mainX;
        double mainY;
        double mainR;
        /** Minutes until Main forks (0 if none is coming), the heading its half takes, and the fork's state in words. */
        int forkWarning;
        double forkAxis;
        String forkLine;
        /** Played minutes since the last fork, voyage or crossing. */
        int quiet;
        /** The last parting's faded half while it's dissolving, or null. */
        Fade fade;

        int held;
        final List<BoonSlot> boons = new ArrayList<>();
        final List<ActiveBoon> active = new ArrayList<>();
        final List<StoneRow> waystones = new ArrayList<>();
        String waystoneNote;
        final List<CrossRow> crossings = new ArrayList<>();

        int walked;
        int crossingsMade;
        int claimed;
        int timesOutside;
        String guardShort = "Off";
        boolean guardClean = true;
        String diaryShort = "-";

        List<Moment> moments = Collections.emptyList();
        List<String> log = Collections.emptyList();

        static View of(NomadSim sim, Tile at, List<String> log, Radar radar, RuleGuard guard, Forecast forecast,
            RunStats stats, List<Moment> moments, boolean outsideNow)
        {
            View v = new View();
            v.log = log;
            v.moments = moments;
            if (sim == null) return v;
            v.live = true;
            Circle c = sim.bound();
            v.name = c.name() + " circle";
            v.colour = NomadPlugin.colour(c, 255);
            v.outside = outsideNow && sim.bridgeLeft() <= 0;
            v.state = v.outside ? "Outside your circle" : sim.stranded() ? "Stranded: Main moved on" : "Inside";
            v.stateColour = v.outside ? SidebarParts.OUT : sim.stranded() ? new Color(0xFFCF8F) : SidebarParts.IN;
            v.edgeIn = radar == null ? -1 : radar.edgeIn;
            v.mood = c.moodName() + (c.sailing() ? ", sailing to " + c.voyageTo() : "");
            v.heading = c.heading();
            v.radius = c.radius();
            v.played = time(sim.minutes());
            v.seed = sim.seed();
            Region over = Geography.regionAt(c.x(), c.y());
            v.where = c.sailing() ? "At sea" : over == null ? "On the coast" : "Over " + over.name;

            // The map: circles near yours, waystones, you, and the next hour's Forecast for yours.
            v.cx = c.x();
            v.cy = c.y();
            if (at != null)
            {
                v.px = at.x;
                v.py = at.y;
            }
            for (int j = 0; j < sim.circles().size(); j++)
            {
                Circle o = sim.circles().get(j);
                v.circles.add(new MapCircle(o, j == sim.boundIndex(), sim.linked(j), c.gap(o)));
            }
            // A forked Main's other half, drawn like Main.
            if (sim.twin() != null)
            {
                v.circles.add(new MapCircle(sim.twin(), sim.boundIndex() == NomadSim.MAIN, sim.linked(NomadSim.MAIN), c.gap(sim.twin())));
            }
            Circle main = sim.main();
            v.mainX = main.x();
            v.mainY = main.y();
            v.mainR = main.radius();
            v.forkWarning = sim.forkWarning();
            v.forkAxis = sim.forkAxis();
            v.quiet = sim.quietMinutes();
            if (sim.forkWarning() > 0)
            {
                v.forkLine = "Fork in " + sim.forkWarning() + " min: " + NomadSim.compass(sim.forkAxis()) + " / "
                    + NomadSim.compass(sim.forkAxis() + Math.PI);
            }
            else if (sim.twin() != null)
            {
                v.forkLine = "Forked: stand in the half you want";
            }
            // Yours last, so it draws on top.
            v.circles.sort((a, b) -> Boolean.compare(a.yours, b.yours));
            for (Waystone w : sim.waystones())
            {
                v.stones.add(new MapStone(w.x(), w.y(), w.circle() == sim.boundIndex() ? Moments.GOLD
                    : NomadPlugin.colour(sim.circles().get(w.circle()), 255)));
            }
            if (forecast != null && sim.boundIndex() < sim.circles().size())
            {
                double[] p = forecast.points(0, sim.boundIndex());
                double[][] pts = new double[p.length / 2][];
                for (int k = 0; k < pts.length; k++) pts[k] = new double[]{p[k * 2], p[k * 2 + 1]};
                List<double[]> hull = NomadWorldMapOverlay.hull(pts);
                if (hull.size() >= 3) v.cone = hull.toArray(new double[0][]);
            }

            v.held = sim.boons().size();
            for (int i = 0; i < sim.boons().size(); i++) v.boons.add(new BoonSlot(i, sim.boons().get(i)));
            if (c.expandLeft() > 0) v.active.add(new ActiveBoon(NomadSim.EXPAND, c.expandLeft(), EXPAND_MINUTES));
            if (c.tailwindLeft() > 0) v.active.add(new ActiveBoon(NomadSim.TAILWIND, c.tailwindLeft(), TAILWIND_MINUTES));
            if (c.anchorLeft() > 0) v.active.add(new ActiveBoon(NomadSim.ANCHOR, c.anchorLeft(), ANCHOR_MINUTES));
            if (sim.bridgeLeft() > 0) v.active.add(new ActiveBoon(NomadSim.BRIDGE, sim.bridgeLeft(), BRIDGE_MINUTES));

            for (Waystone w : sim.waystones())
            {
                StoneRow s = new StoneRow();
                s.boon = w.boon();
                s.rare = Moments.rare(w.boon());
                s.yours = w.circle() == sim.boundIndex();
                s.colour = NomadPlugin.colour(sim.circles().get(w.circle()), 255);
                s.where = w.landmark() + (s.yours ? "" : ", in " + sim.circles().get(w.circle()).name());
                s.left = "~" + roughly(sim.minutesLeft(w));
                if (at != null)
                {
                    s.distance = Math.round(Math.hypot(w.x() - at.x, w.y() - at.y));
                    s.angle = Math.atan2(w.y() - at.y, w.x() - at.x);
                    s.sortKey = s.distance;
                }
                v.waystones.add(s);
            }
            v.waystones.sort((a, b) -> Double.compare(a.sortKey, b.sortKey));
            if (sim.pendingSummons() > 0)
            {
                v.waystoneNote = sim.pendingSummons() + " summoned, waiting for a landmark to drift into reach";
            }

            for (int j = 0; j < sim.circles().size(); j++)
            {
                if (j == sim.boundIndex() || !sim.linked(j)) continue;
                Circle o = sim.circles().get(j);
                double gap = sim.bound().gap(o);
                boolean open = gap <= 0;
                boolean can = sim.canCross(j, at);
                int soon = radar == null || j >= radar.crossingIn.length ? -1 : radar.crossingIn[j];
                String status = open ? (can ? "Open now: you're in the overlap" : "Open now: stand in the overlap")
                    : Math.round(gap) + " tiles apart" + (soon > 0 ? " · touching in ~" + soon + " min" : "");
                v.crossings.add(new CrossRow(j, o.name(), NomadPlugin.colour(o, 255), open, can, status));
            }
            v.crossings.sort((a, b) -> Double.compare(sim.bound().gap(sim.circles().get(a.index)),
                sim.bound().gap(sim.circles().get(b.index))));
            while (v.crossings.size() > 4) v.crossings.remove(v.crossings.size() - 1);

            if (stats != null)
            {
                v.walked = stats.tilesWalked;
                v.crossingsMade = stats.crossings;
                v.claimed = stats.claimed;
                v.timesOutside = stats.timesOutside;
            }
            v.guardClean = guard == null || guard.clean();
            v.guardShort = guard == null ? "Off" : guard.clean() ? "Clean" : guard.xp + " XP out";
            Region home = c.region();
            v.diaryShort = home == null ? "-" : home.maxTiers() == 0 ? "None" : c.tiers() + "/" + home.maxTiers();
            return v;
        }
    }
}
