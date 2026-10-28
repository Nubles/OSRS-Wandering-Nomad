package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.PluginPanel;

/**
 * The sidebar: your circle, the crossings you can take, the region's diary progress, the run log,
 * links to the website and a button to start over. It only ever shows a {@link View} taken on the client thread.
 */
class NomadPanel extends PluginPanel
{
    private final IntConsumer onCross;
    private final BiConsumer<Integer, Integer> onBoon;
    private final Runnable onNewRun;
    private final Runnable onViewOnWebsite;
    private final Runnable onCopyRunCode;
    private final Runnable onCopyLiveLink;
    private final JLabel title = new JLabel();
    private final JLabel detail = new JLabel();
    private final JPanel crossings = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JLabel diary = new JLabel();
    private final JLabel guard = new JLabel();
    private final JLabel boonsTitle = heading("Boons");
    private final JPanel boons = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JLabel waystones = new JLabel();
    private final JTextArea logArea = new JTextArea();

    NomadPanel(IntConsumer onCross, BiConsumer<Integer, Integer> onBoon, Runnable onNewRun, Runnable onViewOnWebsite,
        Runnable onCopyRunCode, Runnable onCopyLiveLink)
    {
        this.onBoon = onBoon;
        this.onCopyLiveLink = onCopyLiveLink;
        this.onCross = onCross;
        this.onNewRun = onNewRun;
        this.onViewOnWebsite = onViewOnWebsite;
        this.onCopyRunCode = onCopyRunCode;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        add(title);
        add(detail);
        add(gap());
        add(heading("Crossings"));
        crossings.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(crossings);
        add(gap());
        add(boonsTitle);
        boons.setBackground(ColorScheme.DARK_GRAY_COLOR);
        add(boons);
        add(gap());
        add(heading("Waystones"));
        add(waystones);
        add(gap());
        add(diary);
        add(gap());
        add(heading("Rule guard"));
        add(guard);
        add(gap());
        add(heading("Run log"));
        logArea.setEditable(false);
        logArea.setLineWrap(true);
        logArea.setWrapStyleWord(true);
        logArea.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        logArea.setForeground(Color.LIGHT_GRAY);
        logArea.setRows(14);
        add(logArea);
        add(gap());
        add(heading("Website"));
        JPanel web = new JPanel(new GridLayout(0, 1, 0, 4));
        web.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JButton view = new JButton("View on website");
        view.setToolTipText("Opens your circles on the Nomad UIM website, exactly where they are now");
        view.addActionListener(e -> this.onViewOnWebsite.run());
        JButton copy = new JButton("Copy run code");
        copy.setToolTipText("Copies a code you can paste on the website or share");
        copy.addActionListener(e -> this.onCopyRunCode.run());
        JButton live = new JButton("Copy live link");
        live.setToolTipText("A link that follows your run as you play (turn on Share live in the plugin's settings)");
        live.addActionListener(e -> this.onCopyLiveLink.run());
        web.add(view);
        web.add(copy);
        web.add(live);
        add(web);
        add(gap());
        JButton newRun = new JButton("Start a new run");
        newRun.addActionListener(e ->
        {
            int ok = JOptionPane.showConfirmDialog(this,
                "This deletes the current run on this account and starts a new one. Continue?",
                "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) onNewRun.run();
        });
        JPanel bottom = new JPanel(new BorderLayout());
        bottom.add(newRun, BorderLayout.CENTER);
        add(bottom);
        display(View.of(null, null, Collections.<String>emptyList(), null, null));
    }

    void display(View v)
    {
        title.setText(v.title);
        title.setForeground(v.colour);
        detail.setText(v.detail);
        diary.setText(v.diary);
        guard.setText(v.guard);
        boonsTitle.setText(("Boons " + v.boonCount + "/" + NomadSim.BOONS_HELD).toUpperCase());
        waystones.setText(v.waystones);
        boons.removeAll();
        if (v.boons.isEmpty()) boons.add(new JLabel("<html>None yet. Milestones summon waystones; stand on one to claim its boon.</html>"));
        for (Boon b : v.boons)
        {
            JButton button = new JButton(b.label);
            button.setToolTipText(b.tip);
            button.addActionListener(e -> onBoon.accept(b.index, b.side));
            boons.add(button);
        }
        crossings.removeAll();
        if (v.options.isEmpty())
        {
            crossings.add(new JLabel(v.noCrossing));
        }
        for (Option o : v.options)
        {
            JButton b = new JButton(o.label);
            b.setEnabled(o.enabled);
            b.addActionListener(e -> onCross.accept(o.index));
            crossings.add(b);
        }
        logArea.setText(String.join("\n", v.log));
        revalidate();
        repaint();
    }

    static String time(int minutes)
    {
        return (minutes / 60) + "h " + String.format("%02d", minutes % 60) + "m";
    }

    private static JLabel heading(String text)
    {
        JLabel l = new JLabel(text.toUpperCase());
        l.setForeground(Color.GRAY);
        return l;
    }

    private static JLabel gap()
    {
        JLabel l = new JLabel(" ");
        l.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        return l;
    }

    static final class Boon
    {
        final int index;
        final int side;
        final String label;
        final String tip;

        Boon(int index, int side, String label, String tip)
        {
            this.index = index;
            this.side = side;
            this.label = label;
            this.tip = tip;
        }
    }

    /** What each boon does, for its button's tooltip. */
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

    static final class Option
    {
        final int index;
        final String label;
        final boolean enabled;

        Option(int index, String label, boolean enabled)
        {
            this.index = index;
            this.label = label;
            this.enabled = enabled;
        }
    }

    /** Everything the panel shows, worked out on the client thread. */
    static final class View
    {
        String title = "Nomad UIM";
        Color colour = Color.WHITE;
        String detail = "Log in to start or resume your run.";
        String diary = " ";
        String guard = " ";
        String waystones = " ";
        int boonCount;
        final List<Boon> boons = new ArrayList<>();
        String noCrossing = " ";
        final List<Option> options = new ArrayList<>();
        List<String> log = Collections.emptyList();

        static View of(NomadSim sim, Tile at, List<String> log, Radar radar, RuleGuard guard)
        {
            View v = new View();
            v.log = log;
            if (sim == null) return v;
            v.guard = guard == null
                ? "<html>Off. Turn on Rule guard in the settings to note XP, loot and kills outside.</html>"
                : guard.clean()
                ? "<html>Clean: nothing gained outside your circle.</html>"
                : "<html>Outside so far: " + guard.xp + " XP, " + guard.loot + " loot, " + guard.kills + " kills."
                    + "<br>Fighting back and travelling home are allowed.<br>"
                    + String.join("<br>", guard.notes.subList(0, Math.min(3, guard.notes.size()))) + "</html>";
            v.boonCount = sim.boons().size();
            for (int i = 0; i < sim.boons().size(); i++)
            {
                String b = sim.boons().get(i);
                if (NomadSim.RUDDER.equals(b))
                {
                    v.boons.add(new Boon(i, 1, "Rudder: turn left", describe(b)));
                    v.boons.add(new Boon(i, -1, "Rudder: turn right", describe(b)));
                }
                else
                {
                    v.boons.add(new Boon(i, 0, "Use " + b, describe(b)));
                }
            }
            List<String> stones = new ArrayList<>();
            for (Waystone w : sim.waystones())
            {
                Circle in = sim.circles().get(w.circle());
                String where = w.circle() == sim.boundIndex() ? "your circle" : in.name();
                String away = at == null ? "" : ", " + Math.round(Math.hypot(w.x() - at.x, w.y() - at.y)) + " tiles away";
                stones.add("<b>" + w.boon() + "</b> at " + w.landmark() + " in " + where + away
                    + ", about " + roughly(sim.minutesLeft(w)) + " left");
            }
            if (sim.pendingSummons() > 0)
            {
                stones.add(sim.pendingSummons() + " summoned, waiting for a landmark to drift into reach");
            }
            if (sim.bridgeLeft() > 0) stones.add("Bridge open for " + sim.bridgeLeft() + " more min");
            v.waystones = stones.isEmpty() ? "<html>None. Every 100 total level, 25 quest points or first boss kill summons one.</html>"
                : "<html>" + String.join("<br>", stones) + "</html>";

            Circle c = sim.bound();
            v.title = c.name() + " circle · " + Math.round(c.radius()) + " tiles";
            v.colour = NomadPlugin.colour(c, 255);
            Region over = Geography.regionAt(c.x(), c.y());
            String where = c.sailing() ? "at sea, bound for " + c.voyageTo() : over == null ? "on the coast" : "over " + over.name;
            v.detail = "<html>Played " + time(sim.minutes()) + ", seed " + sim.seed() + "<br>Your circle is " + where
                + ", " + c.moodName().toLowerCase()
                + (radar != null && radar.edgeIn > 0 ? "<br>The edge reaches you in about " + radar.edgeIn + " min." : "")
                + effects(c)
                + (sim.isOutside() ? "<br><b>You are outside it.</b>" : "")
                + (sim.stranded() ? "<br>Stranded: Main has moved on." : "") + "</html>";

            List<Option> all = new ArrayList<>();
            for (int j = 0; j < sim.circles().size(); j++)
            {
                if (!sim.linked(j)) continue;
                Circle o = sim.circles().get(j);
                double gap = sim.bound().gap(o);
                boolean open = gap <= 0;
                boolean can = sim.canCross(j, at);
                int soon = radar == null || j >= radar.crossingIn.length ? -1 : radar.crossingIn[j];
                String label = open ? (can ? "Cross into " + o.name() : o.name() + ": stand in the overlap")
                    : o.name() + ": " + Math.round(gap) + " tiles apart" + (soon > 0 ? ", touching in ~" + soon + " min" : "");
                all.add(new Option(j, label, can));
            }
            all.sort((a, b) -> Double.compare(sim.bound().gap(sim.circles().get(a.index)),
                sim.bound().gap(sim.circles().get(b.index))));
            v.options.addAll(all.subList(0, Math.min(3, all.size())));
            v.noCrossing = "No circle to cross into.";

            Region home = c.region();
            v.diary = home == null
                ? "<html>Diaries grow region circles. Cross into one to see its progress.</html>"
                : home.maxTiers() == 0
                ? "<html>" + home.name + " has no Achievement Diary, so its circle doesn't grow.</html>"
                : "<html>" + home.name + " diaries: " + c.tiers() + "/" + home.maxTiers() + " tiers<br>Range "
                    + Math.round(c.radiusMin()) + " to " + Math.round(c.radiusMax()) + " tiles</html>";
            return v;
        }
    }
}
