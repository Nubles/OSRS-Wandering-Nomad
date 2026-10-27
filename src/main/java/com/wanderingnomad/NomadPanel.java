package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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
    private final Runnable onNewRun;
    private final Runnable onViewOnWebsite;
    private final Runnable onCopyRunCode;
    private final JLabel title = new JLabel();
    private final JLabel detail = new JLabel();
    private final JPanel crossings = new JPanel(new GridLayout(0, 1, 0, 4));
    private final JLabel diary = new JLabel();
    private final JTextArea logArea = new JTextArea();

    NomadPanel(IntConsumer onCross, Runnable onNewRun, Runnable onViewOnWebsite, Runnable onCopyRunCode)
    {
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
        add(diary);
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
        web.add(view);
        web.add(copy);
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
        display(View.of(null, null, Collections.<String>emptyList()));
    }

    void display(View v)
    {
        title.setText(v.title);
        title.setForeground(v.colour);
        detail.setText(v.detail);
        diary.setText(v.diary);
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
        String noCrossing = " ";
        final List<Option> options = new ArrayList<>();
        List<String> log = Collections.emptyList();

        static View of(NomadSim sim, Tile at, List<String> log)
        {
            View v = new View();
            v.log = log;
            if (sim == null) return v;
            Circle c = sim.bound();
            v.title = c.name() + " circle · " + Math.round(c.radius()) + " tiles";
            v.colour = NomadPlugin.colour(c, 255);
            Region over = Geography.regionAt(c.x(), c.y());
            String where = c.sailing() ? "at sea, bound for " + c.voyageTo() : over == null ? "on the coast" : "over " + over.name;
            v.detail = "<html>Played " + time(sim.minutes()) + ", seed " + sim.seed() + "<br>Your circle is " + where
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
                String label = open ? (can ? "Cross into " + o.name() : o.name() + ": stand in the overlap")
                    : o.name() + ": " + Math.round(gap) + " tiles apart";
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
