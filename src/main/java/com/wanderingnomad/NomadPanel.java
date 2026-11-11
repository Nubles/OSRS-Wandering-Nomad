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
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
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
    /** Spends a Helmsman (by its index) on a port. */
    private BiConsumer<Integer, String> onHelm = (index, port) -> { };
    /** The ports a Helmsman could send Main to, as last shown. */
    private volatile List<String> helmPorts = Collections.emptyList();
    /** Spends a Lodestone (by its index) on a landmark. */
    private BiConsumer<Integer, String> onLode = (index, landmark) -> { };
    /** The landmarks a Lodestone could point your circle at, as last shown. */
    private volatile List<String> lodeTargets = Collections.emptyList();
    /** Turns Shatter on or off for the next waystone you claim. */
    private Consumer<Boolean> onShatter = on -> { };
    private final JCheckBox shatter = new JCheckBox("Shatter the next waystone I claim");
    /** Encore: spends the banked minutes of Anchor. */
    private Runnable onEncore = () -> { };
    private final JButton encore = button("Spend Encore", "Every new music track banks a minute of Anchor: spend them all to hold "
        + "your circle still that long", () -> onEncore.run());
    private boolean fillingShatter;
    /** Legacy: lays the run down; calls a fork wager (true for the half heading the fork's way); seals the three prophecies. */
    private Runnable onRetire = () -> { };
    private Consumer<Boolean> onWager = with -> { };
    private Consumer<String[]> onSeal = picks -> { };
    private final JPanel wagerRow = new JPanel(new GridLayout(1, 2, 4, 0));
    private final JButton wagerWith = small("Wager", "Call the half of Main you'll keep when the fork parts. Right pays 40 renown.",
        () -> onWager.accept(true));
    private final JButton wagerAgainst = small("Wager", "Call the half of Main you'll keep when the fork parts. Right pays 40 renown.",
        () -> onWager.accept(false));
    private final JButton prophecies = small("Prophecies", "Seal three prophecies in a run's first hour; they're revealed at 100 hours",
        this::showProphecies);
    private final JButton layDown = new JButton("Lay down the staff");
    /** Fellowship: an action and what was typed or pasted for it; the lines shown, and the menu's button. */
    private BiConsumer<String, String> onSocial = (action, input) -> { };
    private final JLabel fellowship = note(" ");
    private List<String> shownFellowship;
    private final JButton fellowshipMenu = small("Fellowship...", "Bottles, dares, the clan torch, relays, colours, the clan atlas and the "
        + "tale of the tape", this::showFellowshipMenu);
    /** Streaming: an action and what was pasted for it; the lines shown, and the menu's button. */
    private BiConsumer<String, String> onStream = (action, input) -> { };
    private final JLabel streaming = note(" ");
    private List<String> shownStream;
    private final JButton streamMenu = small("Streaming...", "Recording, chapters, Run wrapped, chat's votes, viewer flags and photo mode",
        this::showStreamMenu);
    private final SidebarParts.Header header = new SidebarParts.Header();
    private final SidebarParts.MiniMap map = new SidebarParts.MiniMap();
    private final JLabel boonsTitle = heading("Boons");
    private final JPanel boons = column();
    private final SidebarParts.Active active = new SidebarParts.Active();
    private final SidebarParts.Waystones waystones = new SidebarParts.Waystones();
    private final JPanel crossings = column();
    private final SidebarParts.Stats stats = new SidebarParts.Stats();
    private final SidebarParts.Journey journey = new SidebarParts.Journey();
    private final JComboBox<String> palette = new JComboBox<>();
    private final JComboBox<String> oath = new JComboBox<>();
    private final JLabel rivals = note(" ");
    private List<String> shownRivals = Collections.emptyList();
    private List<String> shownOaths = Collections.emptyList();
    private boolean fillingOaths;
    /** The palettes the box lists, so it's only refilled when a rank unlocks one. */
    private List<String> shownPalettes = Collections.emptyList();
    private boolean fillingPalettes;
    private volatile JourneyView lastJourney = JourneyView.EMPTY;
    private final SidebarParts.Timeline timeline = new SidebarParts.Timeline();
    private final JTextArea logArea = new JTextArea();
    private final List<SidebarParts.Part> drawn = new ArrayList<>();
    private String shownBoons;
    private String shownCrossings;

    NomadPanel(IntConsumer onCross, BiConsumer<Integer, Integer> onBoon, Consumer<String> onNewRun, Runnable onViewOnWebsite,
        Runnable onCopyRunCode, Runnable onCopyLiveLink, Consumer<String> onPalette, Consumer<String> onOath)
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
        add(gap(2));
        shatter.setOpaque(false);
        shatter.setFont(FontManager.getRunescapeSmallFont());
        shatter.setForeground(Color.LIGHT_GRAY);
        shatter.setToolTipText("Instead of taking its boon, break the stone: half the time it bursts into a rare boon, "
            + "half the time it crumbles to nothing. Legendary stones can't be shattered.");
        shatter.addActionListener(e ->
        {
            if (!fillingShatter) onShatter.accept(shatter.isSelected());
        });
        fix(shatter, 18);
        add(shatter);
        encore.setVisible(false);
        fix(encore, 24);
        add(encore);
        add(gap(8));
        add(heading("Crossings"));
        add(gap(4));
        add(crossings);
        add(gap(10));
        add(heading("Run"));
        add(gap(4));
        add(stats);
        add(gap(10));
        add(heading("Journey"));
        add(gap(4));
        add(journey);
        add(gap(4));
        JPanel journeyRow = new JPanel(new GridLayout(1, 2, 4, 0));
        journeyRow.setOpaque(false);
        journeyRow.add(small("All trials", "Every trial of the run, chapter by chapter", this::showTrials));
        journeyRow.add(small("Run ledger", "The run's story so far, to copy into Discord or a video", this::showLedger));
        fix(journeyRow, 24);
        add(journeyRow);
        add(gap(4));
        JPanel legacyRow = new JPanel(new GridLayout(1, 2, 4, 0));
        legacyRow.setOpaque(false);
        legacyRow.add(small("Legacy", "Your ended runs: their epilogues and sigils", this::showLegacy));
        legacyRow.add(prophecies);
        fix(legacyRow, 24);
        add(legacyRow);
        add(gap(4));
        wagerRow.setOpaque(false);
        wagerRow.add(wagerWith);
        wagerRow.add(wagerAgainst);
        wagerRow.setVisible(false);
        fix(wagerRow, 24);
        add(wagerRow);
        add(gap(4));
        palette.setToolTipText("The Main circle's edge colour. Ranks unlock more.");
        palette.setFont(FontManager.getRunescapeSmallFont());
        palette.addActionListener(e ->
        {
            Object picked = palette.getSelectedItem();
            if (!fillingPalettes && picked != null) onPalette.accept(picked.toString());
        });
        fix(palette, 24);
        add(palette);
        add(gap(4));
        oath.setToolTipText("Swear one long goal as this run's oath. Keep it for 300 renown and the Oathbound edge colour.");
        oath.setFont(FontManager.getRunescapeSmallFont());
        oath.addActionListener(e ->
        {
            Object picked = oath.getSelectedItem();
            if (!fillingOaths && picked != null) onOath.accept(picked.toString().replaceFirst("^Oath: ", ""));
        });
        fix(oath, 24);
        add(oath);
        rivals.setVisible(false);
        rivals.setAlignmentX(LEFT_ALIGNMENT);
        add(rivals);
        add(gap(10));
        add(heading("Fellowship"));
        add(gap(4));
        fellowship.setAlignmentX(LEFT_ALIGNMENT);
        add(fellowship);
        add(gap(4));
        fix(fellowshipMenu, 24);
        add(fellowshipMenu);
        add(gap(10));
        add(heading("Streaming"));
        add(gap(4));
        streaming.setAlignmentX(LEFT_ALIGNMENT);
        add(streaming);
        add(gap(4));
        fix(streamMenu, 24);
        add(streamMenu);
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
            List<String> names = lastJourney.rivalNames;
            JComboBox<String> race = new JComboBox<>();
            race.addItem("No race");
            for (String n : names) race.addItem("Race " + n + " on their seed");
            Object message = names.isEmpty() ? "This deletes the current run on this account and starts a new one. Continue?"
                : new Object[]{"This deletes the current run on this account and starts a new one. Continue?", race};
            int ok = JOptionPane.showConfirmDialog(this, message, "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok != JOptionPane.OK_OPTION) return;
            int picked = race.getSelectedIndex();
            onNewRun.accept(picked > 0 ? names.get(picked - 1) : null);
        });
        buttons.add(newRun);
        layDown.setToolTipText("End the run here: Main stops, and its epilogue and sigil are kept on your account");
        layDown.addActionListener(e ->
        {
            int ok = JOptionPane.showConfirmDialog(this, "Lay down the staff? The run ends here: its circles stop, and its epilogue and sigil "
                + "are kept on your account. You can start a new run after.", "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) onRetire.run();
        });
        buttons.add(layDown);
        fix(buttons, 5 * 26 + 12);
        add(buttons);

        drawn.add(header);
        drawn.add(map);
        drawn.add(active);
        drawn.add(waystones);
        drawn.add(stats);
        drawn.add(journey);
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

    void onHelm(BiConsumer<Integer, String> onHelm)
    {
        this.onHelm = onHelm;
    }

    void onLode(BiConsumer<Integer, String> onLode)
    {
        this.onLode = onLode;
    }

    void onShatter(Consumer<Boolean> onShatter)
    {
        this.onShatter = onShatter;
    }

    void onEncore(Runnable onEncore)
    {
        this.onEncore = onEncore;
    }

    void onStream(BiConsumer<String, String> onStream)
    {
        this.onStream = onStream;
    }

    void onSocial(BiConsumer<String, String> onSocial)
    {
        this.onSocial = onSocial;
    }

    void onLegacy(Runnable onRetire, Consumer<Boolean> onWager, Consumer<String[]> onSeal)
    {
        this.onRetire = onRetire;
        this.onWager = onWager;
        this.onSeal = onSeal;
    }

    void display(View v)
    {
        helmPorts = v.helmPorts;
        lodeTargets = v.lodeTargets;
        if (shatter.isSelected() != v.shatterNext)
        {
            fillingShatter = true;
            shatter.setSelected(v.shatterNext);
            fillingShatter = false;
        }
        shatter.setEnabled(v.live);
        encore.setVisible(v.live && v.encore > 0);
        encore.setText("Spend Encore: hold still " + v.encore + " min");
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

        lastJourney = v.journey;
        JourneyView jv = v.journey;
        boolean wager = v.live && jv.wagerWith != null;
        if (wager)
        {
            wagerWith.setText("Wager: " + abbreviate(jv.wagerWith) + " half");
            wagerAgainst.setText("Wager: " + abbreviate(jv.wagerAgainst) + " half");
        }
        if (wagerRow.isVisible() != wager)
        {
            wagerRow.setVisible(wager);
            revalidate();
        }
        layDown.setEnabled(v.live && !jv.retired);
        if (!jv.fellowship.equals(shownFellowship))
        {
            shownFellowship = jv.fellowship;
            StringBuilder html = new StringBuilder("<html>");
            if (shownFellowship.isEmpty()) html.append("Nothing under way. Wave, then Bow, to greet other Nomads.");
            for (int i = 0; i < shownFellowship.size(); i++) html.append(i == 0 ? "" : "<br>").append(escape(shownFellowship.get(i)));
            fellowship.setText(html.append("</html>").toString());
            fix(fellowship, 4 + 14 * Math.max(1, shownFellowship.size()));
            revalidate();
        }
        if (!v.stream.equals(shownStream))
        {
            shownStream = v.stream;
            StringBuilder html = new StringBuilder("<html>");
            if (shownStream.isEmpty()) html.append("Off air. Episodes start at login; Start recording for chapters and a stake.");
            for (int i = 0; i < shownStream.size(); i++) html.append(i == 0 ? "" : "<br>").append(escape(shownStream.get(i)));
            streaming.setText(html.append("</html>").toString());
            fix(streaming, 4 + 14 * Math.max(1, wrapped(shownStream)));
            revalidate();
        }
        prophecies.setText(jv.prophecyOpen ? "Seal prophecies" : "Prophecies");
        if (!v.journey.rivals.equals(shownRivals))
        {
            shownRivals = v.journey.rivals;
            StringBuilder html = new StringBuilder("<html><b style='color:#ffffff'>Rivals</b>");
            for (String line : shownRivals) html.append("<br>").append(line.replace("&", "&amp;").replace("<", "&lt;"));
            rivals.setText(html.append("</html>").toString());
            rivals.setVisible(!shownRivals.isEmpty());
            fix(rivals, 16 + 26 * shownRivals.size());
            revalidate();
        }
        if (!v.journey.palettes.equals(shownPalettes) || !v.journey.palette.equals(palette.getSelectedItem()))
        {
            fillingPalettes = true;
            if (!v.journey.palettes.equals(shownPalettes))
            {
                shownPalettes = v.journey.palettes;
                palette.removeAllItems();
                for (String name : shownPalettes) palette.addItem(name);
            }
            palette.setSelectedItem(v.journey.palette);
            fillingPalettes = false;
        }

        List<String> oathItems = new ArrayList<>();
        oathItems.add("Oath: None");
        for (String name : v.journey.oaths) oathItems.add("Oath: " + name);
        String oathPick = v.journey.oath == null ? "Oath: None" : "Oath: " + v.journey.oath + (v.journey.oathKept ? " (kept)" : "");
        if (v.journey.oathKept || !v.journey.oathOpen)
        {
            oathItems = Collections.singletonList(oathPick);
        }
        if (!oathItems.equals(shownOaths) || !oathPick.equals(oath.getSelectedItem()))
        {
            fillingOaths = true;
            if (!oathItems.equals(shownOaths))
            {
                shownOaths = oathItems;
                oath.removeAllItems();
                for (String item : oathItems) oath.addItem(item);
            }
            oath.setSelectedItem(oathPick);
            fillingOaths = false;
        }
        oath.setEnabled(v.journey.live && v.journey.oathOpen);

        timeline.show(v.moments);
        String log = String.join("\n", v.log);
        if (!log.equals(logArea.getText())) logArea.setText(log);
        revalidate();
    }

    /** Every trial of the run, by chapter, in a window of its own. */
    private void showTrials()
    {
        JourneyView j = lastJourney;
        StringBuilder html = new StringBuilder("<html><body style='font-family:sans-serif;font-size:10px;color:#dddddd;background:#1e1e1e'>");
        if (!j.live) html.append("<p>Log in to see your trials.</p>");
        else
        {
            html.append(String.format("<p><b style='color:%s'>%s</b> &middot; %,d renown &middot; %d of %d trials this run &middot; run %d</p>",
                hex(j.rankColour), j.rank, j.renown, j.done, j.total, Math.max(1, j.runs)));
        }
        String chapter = null;
        for (JourneyView.Row r : j.all)
        {
            if (!r.chapter.equals(chapter))
            {
                if (chapter != null) html.append("</table>");
                chapter = r.chapter;
                html.append("<h3 style='margin:8px 0 2px 0;color:#ffffff'>").append(chapter).append("</h3><table width='100%' cellpadding='2'>");
            }
            String state = r.done ? "<b style='color:#8fe0a2'>Done</b>" : r.carried ? "<span style='color:#9a9a9a'>Before this run</span>"
                : (int) Math.round(r.progress * 100) + "%";
            html.append("<tr><td><b style='color:").append(r.done ? "#ffffff" : "#cccccc").append("'>").append(r.name).append("</b>")
                .append(r.times > 0 ? " <span style='color:#9a9a9a'>(done in " + r.times + (r.times == 1 ? " run" : " runs") + ")</span>" : "")
                .append("<br><span style='color:#9a9a9a'>").append(r.detail).append(" ").append(r.done || r.carried ? "" : r.count).append("</span></td>")
                .append("<td align='right' valign='top' style='color:").append(hex(r.colour)).append("'>").append(r.tier).append(" +").append(r.renown)
                .append("<br>").append(state).append("</td></tr>");
        }
        if (chapter != null) html.append("</table>");
        if (j.live && !j.lineage.isEmpty())
        {
            html.append("<h3 style='margin:8px 0 2px 0;color:#ffffff'>Lineage</h3><p style='margin:0'>");
            for (String line : j.lineage) html.append(escape(line)).append("<br>");
            html.append("</p>");
        }
        if (j.live)
        {
            html.append("<h3 style='margin:8px 0 2px 0;color:#ffffff'>Curiosities (").append(j.curiosFound).append(" of ")
                .append(j.curios.size()).append(")</h3><table width='100%' cellpadding='2'>");
            for (String[] c : j.curios)
            {
                boolean found = !"0".equals(c[2]);
                html.append("<tr><td><b style='color:").append(found ? "#e6c3ff" : "#777777").append("'>").append(found ? escape(c[0]) : "???")
                    .append("</b><br><span style='color:#9a9a9a'>").append(escape(c[1])).append("</span></td><td align='right' valign='top'>")
                    .append(found ? "Seen " + c[2] + (c[2].equals("1") ? " time" : " times") : "").append("</td></tr>");
            }
            html.append("</table>");
        }
        html.append("</body></html>");
        JEditorPane pane = new JEditorPane("text/html", html.toString());
        pane.setEditable(false);
        pane.setBackground(ColorScheme.DARK_GRAY_COLOR);
        pane.setCaretPosition(0);
        JScrollPane scroll = new JScrollPane(pane);
        scroll.setPreferredSize(new Dimension(420, 520));
        JOptionPane.showMessageDialog(this, scroll, "Nomad UIM: trials", JOptionPane.PLAIN_MESSAGE);
    }

    private static String escape(String text)
    {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    /** Roughly how many sidebar lines some text takes once wrapped. */
    private static int wrapped(List<String> lines)
    {
        int n = 0;
        for (String line : lines) n += 1 + line.length() / 38;
        return n;
    }

    /** The Streaming menu: recording, chapters, Run wrapped, chat's votes, viewer flags and photo mode. */
    private void showStreamMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        item(menu, "Start recording", "Chapters count from now, and the episode gets its stake", () -> onStream.accept("record", ""));
        item(menu, "Mark a moment", "A chapter of your own (there's a hotkey for this in the settings)", () -> onStream.accept("mark", ""));
        item(menu, "Chapters...", "YouTube chapters, a marker list and an EDL for your editor", () -> onStream.accept("chapters", ""));
        item(menu, "Wrap the episode", "Reveals the prophecy, settles the stake and scores the drama", () ->
        {
            int ok = JOptionPane.showConfirmDialog(this, "End this episode now? A new one starts at your next login, or when you start recording.",
                "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) onStream.accept("wrap", "");
        });
        menu.addSeparator();
        item(menu, "Run wrapped...", "Six stat cards from your run so far, to save and share", () -> onStream.accept("wrapped", ""));
        item(menu, "Another talking point", "A fresh dead-air card from your run", () -> onStream.accept("another", ""));
        menu.addSeparator();
        item(menu, "Chat paints the wall...", "Paste chat's !colour, !tone and !name votes, or a votes code", () -> pasteStream("votes",
            "Paste chat lines (bob: !colour teal, !tone dry, !name Gertrude) or a votes code. Each voter counts once; the looks last this episode."));
        item(menu, "Clear chat's wall", "Back to your own colour, narrator and name", () -> onStream.accept("clear-votes", ""));
        item(menu, "Plant viewer flags...", "Paste chat's !flag lines or a flags code", () -> pasteStream("flags",
            "Paste chat lines (bob: !flag 3222 3218, or amy: !flag Varrock Square) or a flags code. One flag per viewer, 200 at most."));
        item(menu, "Clear viewer flags", "Takes every flag off the map", () -> onStream.accept("clear-flags", ""));
        menu.addSeparator();
        item(menu, "Photo mode", "Hides the Nomad HUD and turns the glow and shade up full; again to turn it off", () -> onStream.accept("photo", ""));
        menu.show(streamMenu, 0, streamMenu.getHeight());
    }

    private void pasteStream(String action, String prompt)
    {
        JTextArea area = new JTextArea(6, 28);
        area.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(area);
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{prompt, scroll}, "Nomad UIM", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION && !area.getText().trim().isEmpty()) onStream.accept(action, area.getText().trim());
    }

    /** The episode's chapters: YouTube's list, a marker list and an EDL, each to copy or save. */
    void showChapters(String[] c)
    {
        if (c == null)
        {
            JOptionPane.showMessageDialog(this, "No episode yet: one starts when you log in.", "Nomad UIM", JOptionPane.PLAIN_MESSAGE);
            return;
        }
        javax.swing.JTabbedPane tabs = new javax.swing.JTabbedPane();
        String[] names = {"YouTube", "Markers", "EDL"};
        for (int i = 0; i < names.length; i++)
        {
            JTextArea area = new JTextArea(c[i], 12, 34);
            area.setEditable(false);
            tabs.addTab(names[i], new JScrollPane(area));
        }
        Object[] options = {"Copy this tab", "Save EDL", "Close"};
        int picked = JOptionPane.showOptionDialog(this, tabs, "Nomad UIM: chapters of " + c[3], JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
        if (picked == 0)
        {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(c[tabs.getSelectedIndex()]), null);
        }
        if (picked != 1) return;
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File("nomad-" + c[3].toLowerCase().replace(' ', '-') + ".edl"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try
        {
            java.nio.file.Files.write(chooser.getSelectedFile().toPath(), c[2].getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, "Couldn't save the EDL: " + e.getMessage(), "Nomad UIM", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Run wrapped's cards, to save as a picture. */
    void showWrapped(java.awt.image.BufferedImage img)
    {
        java.awt.Image small = img.getScaledInstance(img.getWidth() / 2, img.getHeight() / 2, java.awt.Image.SCALE_SMOOTH);
        Object[] options = {"Save picture", "Close"};
        int picked = JOptionPane.showOptionDialog(this, new JLabel(new javax.swing.ImageIcon(small)), "Nomad UIM: Run wrapped",
            JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[1]);
        if (picked != 0) return;
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File("nomad-run-wrapped.png"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try
        {
            javax.imageio.ImageIO.write(img, "png", chooser.getSelectedFile());
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, "Couldn't save the picture: " + e.getMessage(), "Nomad UIM", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** The Fellowship menu: every code Nomads hand each other, and the tale of the tape. */
    private void showFellowshipMenu()
    {
        JPopupMenu menu = new JPopupMenu();
        item(menu, "Throw a bottle...", "Stand on a coast inside your circle", () ->
        {
            String note = JOptionPane.showInputDialog(this, "Your note, up to " + com.wanderingnomad.social.Bottle.NOTE_MAX + " characters. "
                + "Whoever pastes the code finds it washed up somewhere their circle must reach.", "Throw a bottle", JOptionPane.PLAIN_MESSAGE);
            if (note != null && !note.trim().isEmpty()) onSocial.accept("bottle", note);
        });
        item(menu, "Open a bottle...", "Paste a bottle code someone threw", () -> paste("open-bottle", "Paste a bottle code."));
        menu.addSeparator();
        item(menu, "Dare a rival...", "Make a dare for a rival to finish in 24 played hours", this::makeDare);
        item(menu, "Take a dare...", "Paste a rival's dare code", () -> paste("take-dare", "Paste a dare code. You'll have 24 played hours."));
        item(menu, "Give up the dare", "Once a day", () -> onSocial.accept("give-up-dare", ""));
        menu.addSeparator();
        item(menu, "Light a clan torch...", "Your clan's one torch, passed hand to hand", () ->
        {
            String clan = JOptionPane.showInputDialog(this, "Your clan's name. Only one torch per clan, so agree who lights it.",
                "Light a clan torch", JOptionPane.PLAIN_MESSAGE);
            if (clan != null && !clan.trim().isEmpty()) onSocial.accept("light-torch", clan);
        });
        item(menu, "Pass the torch", "Then stand beside them inside your circle: they salute, then you do", () -> onSocial.accept("pass-torch", ""));
        item(menu, "Take a torch...", "Paste a torch code that names you", () -> paste("take-torch", "Paste the torch code you were sent."));
        item(menu, "Reclaim the torch", "A torch you passed a week ago that went no further", () -> onSocial.accept("reclaim-torch", ""));
        menu.addSeparator();
        item(menu, "Hand on the baton...", "End your leg of a relay and pass the circles on", () ->
        {
            String team = JOptionPane.showInputDialog(this, "This ends your leg: the run is laid down, and the code carries its circles to "
                + "the next runner. Team name (kept from the last leg if this run is one):", "Hand on the baton", JOptionPane.PLAIN_MESSAGE);
            if (team != null) onSocial.accept("baton", team);
        });
        item(menu, "Take a baton...", "Start the relay's next leg where the last runner left the circles", () ->
        {
            int ok = JOptionPane.showConfirmDialog(this, "Taking a baton replaces your current run with the relay's next leg. Continue?",
                "Nomad UIM", JOptionPane.OK_CANCEL_OPTION);
            if (ok == JOptionPane.OK_OPTION) paste("take-baton", "Paste the baton code.");
        });
        menu.addSeparator();
        item(menu, "Gift a colour...", "An unlocked edge colour a rival can fly for one run", () -> colours("gift"));
        item(menu, "Stake a colour...", "The loser of a race flies the winner's colour for a week", () -> colours("stake"));
        item(menu, "Wear colours...", "Paste a gift or a stake you lost", () -> paste("wear", "Paste a colours code."));
        menu.addSeparator();
        item(menu, "Copy my footprint", "The map squares you've walked inside your circle, for a clan atlas", () -> onSocial.accept("footprint", ""));
        item(menu, "Add to clan atlas...", "Paste clanmates' footprint codes", () -> paste("atlas", "Paste one or more footprint codes."));
        item(menu, "Tale of the tape...", "Two runs side by side", () -> paste("tape", "Paste a run code to set against yours, or two to compare."));
        menu.show(fellowshipMenu, 0, fellowshipMenu.getHeight());
    }

    private static void item(JPopupMenu menu, String label, String tip, Runnable action)
    {
        JMenuItem item = new JMenuItem(label);
        item.setToolTipText(tip);
        item.addActionListener(e -> action.run());
        menu.add(item);
    }

    /** Asks for a pasted code (or several), then hands it on. */
    private void paste(String action, String prompt)
    {
        JTextArea area = new JTextArea(4, 28);
        area.setLineWrap(true);
        JScrollPane scroll = new JScrollPane(area);
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{prompt, scroll}, "Nomad UIM", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION && !area.getText().trim().isEmpty()) onSocial.accept(action, area.getText().trim());
    }

    private static final String[] DARE_KINDS = {"Walk tiles", "Claim waystones", "Cross between circles", "Kill new kinds of monster",
        "Stand at landmarks", "Walk new map squares"};

    private void makeDare()
    {
        JComboBox<String> kind = new JComboBox<>(DARE_KINDS);
        JComboBox<String> size = new JComboBox<>(com.wanderingnomad.social.Dare.SIZES);
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"What to dare them:", kind, "How big:", size}, "Dare a rival",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        onSocial.accept("dare", com.wanderingnomad.social.Dare.KINDS[kind.getSelectedIndex()] + "|" + size.getSelectedIndex());
    }

    private void colours(String kind)
    {
        List<String> mine = new ArrayList<>();
        for (String p : lastJourney.palettes) if (!p.contains("'s ")) mine.add(p);
        if (mine.isEmpty()) return;
        JComboBox<String> pick = new JComboBox<>(mine.toArray(new String[0]));
        int ok = JOptionPane.showConfirmDialog(this, new Object[]{"gift".equals(kind) ? "The colour to give for one run:"
            : "The colour you stake on the race:", pick}, "gift".equals(kind) ? "Gift a colour" : "Stake a colour",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (ok == JOptionPane.OK_OPTION) onSocial.accept(kind, (String) pick.getSelectedItem());
    }

    /** Tale of the tape: two runs side by side, each line's winner in gold. */
    void showTape(List<com.wanderingnomad.social.Tape.Row> rows, boolean two)
    {
        StringBuilder html = new StringBuilder("<html><table cellspacing=4><tr><th></th><th>" + (two ? "First" : "Theirs") + "</th><th>"
            + (two ? "Second" : "Yours") + "</th></tr>");
        for (com.wanderingnomad.social.Tape.Row r : rows)
        {
            html.append("<tr><td>").append(escape(r.label)).append("</td><td>").append(cell(r.left, r.winner == 1)).append("</td><td>")
                .append(cell(r.right, r.winner == 2)).append("</td></tr>");
        }
        JOptionPane.showMessageDialog(this, html.append("</table></html>").toString(), "Nomad UIM: tale of the tape", JOptionPane.PLAIN_MESSAGE);
    }

    private static String cell(String text, boolean won)
    {
        return won ? "<b style='color:#ffd54f'>" + escape(text) + "</b>" : escape(text);
    }

    /** "north-east" as "NE". */
    static String abbreviate(String compass)
    {
        StringBuilder out = new StringBuilder();
        for (String part : compass.split("-")) if (!part.isEmpty()) out.append(Character.toUpperCase(part.charAt(0)));
        return out.toString();
    }

    /** The trophy shelf: every ended run's sigil, title and epilogue, newest first. */
    private void showLegacy()
    {
        List<com.wanderingnomad.journey.Legacy.Run> runs = lastJourney.legacy;
        if (runs.isEmpty())
        {
            JOptionPane.showMessageDialog(this, "No run has ended on this account yet. When one does (or you lay down the staff), "
                + "its epilogue and sigil are kept here.", "Nomad UIM: legacy", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JPanel shelf = new JPanel(new GridLayout(0, 1, 0, 6));
        shelf.setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (com.wanderingnomad.journey.Legacy.Run run : runs)
        {
            JPanel row = new JPanel(new BorderLayout(8, 0));
            row.setOpaque(false);
            JLabel sigil = new JLabel(new javax.swing.ImageIcon(com.wanderingnomad.journey.Sigil.render(run, 56, true)));
            row.add(sigil, BorderLayout.WEST);
            String first = run.lines.isEmpty() ? "" : run.lines.get(0);
            JLabel words = new JLabel("<html><b>" + escape(run.title()) + "</b><br>" + escape(first.replaceFirst("^[^:]*: ", "")) + "</html>");
            words.setForeground(Color.WHITE);
            row.add(words, BorderLayout.CENTER);
            row.add(small("Read", "Its epilogue, and its sigil to save", () -> showRun(run)), BorderLayout.EAST);
            shelf.add(row);
        }
        JScrollPane scroll = new JScrollPane(shelf);
        scroll.setPreferredSize(new Dimension(380, Math.min(320, runs.size() * 62 + 8)));
        scroll.getVerticalScrollBar().setUnitIncrement(15);
        JOptionPane.showMessageDialog(this, scroll, "Nomad UIM: legacy", JOptionPane.PLAIN_MESSAGE);
    }

    /** One ended run: its sigil and epilogue, with the sigil to save as a picture. */
    void showRun(com.wanderingnomad.journey.Legacy.Run run)
    {
        JPanel body = new JPanel(new BorderLayout(10, 0));
        body.setOpaque(false);
        body.add(new JLabel(new javax.swing.ImageIcon(com.wanderingnomad.journey.Sigil.render(run, 140, true))), BorderLayout.WEST);
        javax.swing.JTextArea area = new javax.swing.JTextArea(String.join("\n", run.lines));
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(FontManager.getRunescapeSmallFont());
        area.setBackground(ColorScheme.DARK_GRAY_COLOR);
        area.setForeground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(300, 180));
        body.add(scroll, BorderLayout.CENTER);
        Object[] options = {"Save sigil", "Copy epilogue", "Close"};
        int picked = JOptionPane.showOptionDialog(this, body, "Nomad UIM: " + run.title(), JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[2]);
        if (picked == 1)
        {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                new java.awt.datatransfer.StringSelection(String.join("\n", run.lines)), null);
        }
        if (picked != 0) return;
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File("nomad-sigil-generation-" + run.generation + ".png"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) return;
        try
        {
            javax.imageio.ImageIO.write(com.wanderingnomad.journey.Sigil.render(run, 512, true), "png", chooser.getSelectedFile());
        }
        catch (java.io.IOException e)
        {
            JOptionPane.showMessageDialog(this, "Couldn't save the sigil: " + e.getMessage(), "Nomad UIM", JOptionPane.WARNING_MESSAGE);
        }
    }

    /** Seals the prophecies while the first hour lasts; after, shows them with what has come true. */
    private void showProphecies()
    {
        JourneyView j = lastJourney;
        if (!j.live) return;
        if (!j.prophecyOpen)
        {
            String text = j.prophecies.isEmpty() ? "No prophecies were sealed this run. They can only be sealed in a run's first hour."
                : "<html>Revealed at 100 hours, for " + com.wanderingnomad.journey.Prophecy.RENOWN + " renown each that comes true:<br><br>"
                + escape(String.join("\n", j.prophecies)).replace("\n", "<br>") + "</html>";
            JOptionPane.showMessageDialog(this, text, "Nomad UIM: prophecies", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        JComboBox<String> diary = new JComboBox<>(com.wanderingnomad.journey.Prophecy.diaryChoices().toArray(new String[0]));
        JComboBox<String> port = new JComboBox<>(com.wanderingnomad.journey.Prophecy.portChoices().toArray(new String[0]));
        JComboBox<String> place = new JComboBox<>(com.wanderingnomad.journey.Prophecy.placeChoices().toArray(new String[0]));
        Object[] message = {
            "Seal three prophecies. They can't be changed, and each that comes true pays "
                + com.wanderingnomad.journey.Prophecy.RENOWN + " renown at 100 hours.",
            "The kingdom whose diary you'll finish a tier of first:", diary,
            "The first port you'll follow Main into:", port,
            "Where Main will be at 100 hours:", place,
        };
        int ok = JOptionPane.showConfirmDialog(this, message, "Nomad UIM: seal prophecies", JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE);
        if (ok != JOptionPane.OK_OPTION) return;
        onSeal.accept(new String[]{(String) diary.getSelectedItem(), (String) port.getSelectedItem(), (String) place.getSelectedItem()});
    }

    /** The run ledger in a window of its own, with a button to copy it. */
    private void showLedger()
    {
        String text = lastJourney.ledger;
        if (text == null) text = "Log in to see the run ledger.";
        javax.swing.JTextArea area = new javax.swing.JTextArea(text);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setFont(FontManager.getRunescapeSmallFont());
        area.setBackground(ColorScheme.DARK_GRAY_COLOR);
        area.setForeground(Color.WHITE);
        area.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(380, 220));
        Object[] options = {"Copy", "Close"};
        int picked = JOptionPane.showOptionDialog(this, scroll, "Nomad UIM: run ledger", JOptionPane.DEFAULT_OPTION,
            JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
        if (picked == 0 && lastJourney.ledger != null)
        {
            java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new java.awt.datatransfer.StringSelection(text), null);
        }
    }

    private static String hex(Color c)
    {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
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
        else if (NomadSim.HELMSMAN.equals(slot.name))
        {
            JButton[] port = new JButton[1];
            port[0] = small("Port", "Choose the port Main sails to next", () -> portMenu(port[0], slot.index));
            side.add(port[0]);
        }
        else if (NomadSim.LODESTONE.equals(slot.name))
        {
            JButton[] aim = new JButton[1];
            aim[0] = small("Aim", "Choose the landmark your circle leans toward", () -> lodeMenu(aim[0], slot.index));
            side.add(aim[0]);
        }
        else if (NomadSim.WARD.equals(slot.name))
        {
            JButton held = small("Held", "A Ward works while you hold it: the next time you step outside, it breaks and "
                + "being outside costs nothing for " + NomadSim.WARD_MINUTES + " minutes", () -> { });
            held.setEnabled(false);
            side.add(held);
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

    /** The Helmsman's ports, as a menu under its button. */
    private void portMenu(JComponent under, int index)
    {
        JPopupMenu menu = new JPopupMenu();
        for (String port : helmPorts)
        {
            JMenuItem item = new JMenuItem("Sail to " + port);
            item.addActionListener(e -> onHelm.accept(index, port));
            menu.add(item);
        }
        if (helmPorts.isEmpty())
        {
            JMenuItem none = new JMenuItem("No port Main can sail to yet");
            none.setEnabled(false);
            menu.add(none);
        }
        menu.show(under, 0, under.getHeight());
    }

    /** The landmarks a Lodestone can point at, nearest first, as a menu under its button. */
    private void lodeMenu(JComponent under, int index)
    {
        JPopupMenu menu = new JPopupMenu();
        List<String> targets = lodeTargets;
        for (int k = 0; k < Math.min(LODE_CHOICES, targets.size()); k++)
        {
            String landmark = targets.get(k);
            JMenuItem item = new JMenuItem("Lean toward " + landmark);
            item.addActionListener(e -> onLode.accept(index, landmark));
            menu.add(item);
        }
        if (targets.isEmpty())
        {
            JMenuItem none = new JMenuItem("No landmark within " + NomadSim.LODE_REACH + " tiles");
            none.setEnabled(false);
            menu.add(none);
        }
        menu.show(under, 0, under.getHeight());
    }

    /** How many landmarks the Lodestone's menu offers, nearest first. */
    private static final int LODE_CHOICES = 12;

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
            case NomadSim.LANTERN:
                return "Light it where you stand: the next waystone in your circle rises at the landmark nearest it";
            case NomadSim.CALM:
                return "No fork or storm for 3 hours, and one under way breaks off";
            case NomadSim.CROWN:
                return "For 1 hour your circle follows you at a walk";
            case NomadSim.HELMSMAN:
                return "Pick a port: Main heads for the nearest dock and sails there";
            case NomadSim.GATHERING:
                return "For 1 hour every kingdom's circle drifts toward Main";
            case NomadSim.FORESIGHT:
                return "See when the next fork comes and which ways its halves will go";
            case NomadSim.WARD:
                return "Held, not spent: the next time you step outside it breaks, and being outside costs nothing for "
                    + NomadSim.WARD_MINUTES + " minutes";
            case NomadSim.WAKE:
                return "For 1 hour the ground your circle leaves stays yours for " + NomadSim.WAKE_TRAIL + " minutes after";
            case NomadSim.LEAP:
                return "Your circle jumps up to " + NomadSim.LEAP_TILES + " tiles the way it's heading, landing on land";
            case NomadSim.LODESTONE:
                return "Pick a landmark: for 3 hours your circle leans toward it";
            case NomadSim.KEYSTONE:
                return "At the next fork both halves live on for an hour, so you needn't choose at once";
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
        if (c.crownLeft() > 0) on.add("Crown " + time(c.crownLeft()));
        if (c.wakeLeft() > 0) on.add("Wake " + time(c.wakeLeft()));
        if (c.lodeLeft() > 0) on.add("Lodestone " + time(c.lodeLeft()));
        return on.isEmpty() ? "" : "<br>" + String.join(" · ", on);
    }

    /** A boon's tier: common, rare or legendary. */
    static String tier(String boon)
    {
        return NomadSim.legendaryBoon(boon) ? NomadSim.LEGENDARY : NomadSim.rareBoon(boon) ? NomadSim.RARE : NomadSim.COMMON;
    }

    /** A waystone's boon, with "Pilgrim" or "Legendary" before it when it's one of those. */
    static String stoneName(Waystone w)
    {
        return w.legendary() ? "Legendary " + w.boon() : w.pilgrim() ? "Pilgrim " + w.boon()
            : w.wanders() ? "Wandering " + w.boon() : w.boon();
    }

    /**
     * How long a waystone has, in a few words: "~45m" until its circle drifts off it, "38m to
     * claim" for a pilgrim stone, "never fades" for a legendary one, and when a common one ripens.
     */
    static String stoneLeft(NomadSim sim, Waystone w)
    {
        int left = sim.minutesLeft(w);
        String out = left < 0 ? "never fades" : w.pilgrim() ? left + "m to claim" : "~" + roughly(left);
        if (w.ripensIn() > 0) out += " · rare in " + w.ripensIn() + "m";
        return out;
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
        final String tier;

        BoonSlot(int index, String name)
        {
            this.index = index;
            this.name = name;
            this.description = describe(name);
            this.tier = tier(name);
        }
    }

    static final class ActiveBoon
    {
        final String name;
        final int left;
        final int total;
        /** Said in place of the time left, for a boon that waits rather than runs down; null otherwise. */
        final String note;

        ActiveBoon(String name, int left, int total)
        {
            this.name = name;
            this.left = left;
            this.total = Math.max(total, left);
            this.note = null;
        }

        /** A boon that's armed, not running: its bar stays full. */
        ActiveBoon(String name, String note)
        {
            this.name = name;
            this.left = 1;
            this.total = 1;
            this.note = note;
        }

        String title()
        {
            return note != null ? name : name + " active";
        }
    }

    static final class StoneRow
    {
        String boon;
        String tier;
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

        /** When Home Teleport can next carry Main: "Ready" or the time left. */
        String homeward = "Ready";
        /** Ports a Helmsman could send Main to. */
        List<String> helmPorts = Collections.emptyList();
        /** Landmarks a Lodestone could point at, nearest first. */
        List<String> lodeTargets = Collections.emptyList();
        /** Whether the next waystone you claim is shattered. */
        boolean shatterNext;
        /** Encore: minutes of Anchor banked from music tracks. */
        int encore;
        /** Leylines: "Ready", the charge's time left, or "Off". */
        String leyline = "Off";
        /** Second wind: a hardcore run's one death has happened. */
        boolean fallen;
        /** The Unbroken road: played minutes in a row inside, and Footprint squares walked, for the Run card. */
        String road = "-";
        String squares = "-";

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
        JourneyView journey = JourneyView.EMPTY;

        List<Moment> moments = Collections.emptyList();
        List<String> log = Collections.emptyList();
        /** The Streaming section's lines. */
        List<String> stream = Collections.emptyList();

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
            v.mood = (c.orbitLeft() > 0 ? "Orbiting" : c.moodName()) + (c.sailing() ? ", sailing to " + c.voyageTo()
                : c.portOfCall() != null ? ", bound for " + c.portOfCall()
                    + (sim.helmsmanTo() != null && c.isMain() ? " to sail for " + sim.helmsmanTo() : "") : "");
            String lode = sim.lodestoneTo();
            if (lode != null) v.mood += ", leaning toward " + lode;
            if (sim.tide() != null) v.mood += ", tide pulls to your clue";
            if (sim.star() != null) v.mood = "Held by a fallen star";
            if (c.isCaravan())
            {
                v.mood = c.resting() > 0 ? "Resting at " + c.bankTown() + ", " + c.resting() + " min" : "On the road to " + c.bankTown();
            }
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
            com.wanderingnomad.sim.Comet comet = sim.comet();
            if (comet != null && comet.flying())
            {
                v.forkLine = "Comet streaking " + NomadSim.compass(comet.heading()) + ": stand in its path, " + comet.flightLeft() + " min";
            }
            else if (sim.forkWarning() > 0)
            {
                v.forkLine = "Fork in " + sim.forkWarning() + " min: " + NomadSim.compass(sim.forkAxis()) + " / "
                    + NomadSim.compass(sim.forkAxis() + Math.PI);
            }
            else if (comet != null)
            {
                v.forkLine = "Comet in " + comet.warning() + " min, heading " + NomadSim.compass(comet.heading()) + " through Main";
            }
            else if (sim.twin() != null)
            {
                v.forkLine = "Forked: stand in the half you want";
            }
            else if (sim.hopWarn() > 0)
            {
                com.wanderingnomad.sim.Networks.Node hop = com.wanderingnomad.sim.Networks.byName(sim.hopTo());
                v.forkLine = "Main hops by " + (hop == null ? "travel" : com.wanderingnomad.sim.Networks.NAMES[hop.network]) + " to "
                    + sim.hopTo() + " in " + sim.hopWarn() + " min";
            }
            else if (sim.stormWarning() > 0)
            {
                v.forkLine = NomadPlugin.stormName(sim.stormKind() == NomadSim.GALE) + " over Main in " + sim.stormWarning() + " min";
            }
            else if (sim.stormLeft() > 0)
            {
                v.forkLine = NomadPlugin.stormName(sim.stormKind() == NomadSim.GALE) + (sim.stormKind() == NomadSim.GALE ? ": Main 3x fast" : ": Main shrunk") + ", " + sim.stormLeft() + " min left";
            }
            else if (sim.ghost() != null)
            {
                v.forkLine = "The lost half drifts nearby: " + time(sim.ghostLeft()) + " to meet it again";
            }
            else if (sim.rift() != null)
            {
                v.forkLine = "Rift open at " + sim.riftAt() + ": stand there as it closes, " + sim.riftLeft() + " min";
            }
            else if (sim.eclipse() > 0)
            {
                v.forkLine = "Eclipse: Main and " + sim.circles().get(sim.eclipse()).name() + " move as one, " + sim.eclipseLeft() + " min";
            }
            else if (sim.stampedeWarning() > 0)
            {
                v.forkLine = "Stampede in " + sim.stampedeWarning() + " min: every kingdom's circle dashes";
            }
            else if (sim.stampedeLeft() > 0)
            {
                v.forkLine = "Stampede: the kingdoms dash, " + sim.stampedeLeft() + " min left";
            }
            else if (sim.kissing() >= 0)
            {
                v.forkLine = "Border kiss: cross into " + sim.circles().get(sim.kissing()).name() + ", " + sim.kissLeft() + " min left";
            }
            else if (sim.auroraCircle() > 0)
            {
                Circle a = sim.circles().get(sim.auroraCircle());
                v.forkLine = NomadSim.auroraName(a.region().id) + " over " + a.name() + ", " + sim.auroraLeft() + " min left";
            }
            else if (sim.lane() != null && c.isMain())
            {
                v.forkLine = "Sea lane: Main's wake counts as Main for a while";
            }
            else if (sim.homingLeft() > 0 && c.isMain())
            {
                v.forkLine = "Homing: Main leans back toward your last waystone, " + time(sim.homingLeft());
            }
            else if (sim.foreseen() > sim.minutes())
            {
                v.forkLine = "Foreseen: a fork in ~" + time(sim.foreseen() - sim.minutes()) + ", " + NomadSim.compass(sim.foreseenAxis())
                    + " / " + NomadSim.compass(sim.foreseenAxis() + Math.PI) + (sim.keystoneArmed() ? ", Keystone ready" : "");
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
            if (sim.calmLeft() > 0) v.active.add(new ActiveBoon(NomadSim.CALM, sim.calmLeft(), NomadSim.CALM_MINUTES));
            if (c.crownLeft() > 0) v.active.add(new ActiveBoon(NomadSim.CROWN, c.crownLeft(), NomadSim.CROWN_MINUTES));
            if (sim.gatherLeft() > 0) v.active.add(new ActiveBoon(NomadSim.GATHERING, sim.gatherLeft(), NomadSim.GATHER_MINUTES));
            if (c.wakeLeft() > 0) v.active.add(new ActiveBoon(NomadSim.WAKE, c.wakeLeft(), NomadSim.WAKE_MINUTES));
            if (c.lodeLeft() > 0) v.active.add(new ActiveBoon(NomadSim.LODESTONE, c.lodeLeft(), NomadSim.LODE_MINUTES));
            if (sim.wardLeft() > 0) v.active.add(new ActiveBoon(NomadSim.WARD, sim.wardLeft(), NomadSim.WARD_MINUTES));
            if (sim.keystoneLeft() > 0) v.active.add(new ActiveBoon(NomadSim.KEYSTONE, sim.keystoneLeft(), NomadSim.KEYSTONE_MINUTES));
            if (sim.keystoneArmed()) v.active.add(new ActiveBoon(NomadSim.KEYSTONE, "armed for the next fork"));
            if (sim.shatterNext()) v.active.add(new ActiveBoon("Shatter", "the next waystone you claim"));
            v.shatterNext = sim.shatterNext();
            v.encore = sim.encore();
            v.leyline = sim.leyWait() > 0 ? time(sim.leyWait()) : "Ready";
            if (c.pops() > 0) v.active.add(new ActiveBoon("Log pop +" + c.pops(), c.popLeft(), NomadSim.POP_MINUTES));
            if (c.starHold() > 0) v.active.add(new ActiveBoon("Star hold", c.starHold(), NomadSim.STAR_HOLD));
            if (c.starTiles() > 0) v.active.add(new ActiveBoon("Stardust +" + c.starTiles(), c.stardustLeft(), NomadSim.STAR_MINUTES));
            if (sim.catchUp() > 0) v.active.add(new ActiveBoon("Catch-up", sim.catchUp(), NomadSim.INTERMISSION_MAX));
            if (sim.encore() > 0) v.active.add(new ActiveBoon("Encore", sim.encore() + " min of Anchor banked"));
            if (sim.leyWait() > 0) v.active.add(new ActiveBoon("Leyline charging", sim.leyWait(), NomadSim.LEY_WAIT));
            if (sim.boons().contains(NomadSim.LODESTONE)) v.lodeTargets = sim.lodestoneTargets();
            v.homeward = sim.homewardLeft() > 0 ? time(sim.homewardLeft()) : "Ready";
            if (sim.boons().contains(NomadSim.HELMSMAN)) v.helmPorts = sim.helmsmanPorts();

            for (Waystone w : sim.waystones())
            {
                StoneRow s = new StoneRow();
                s.boon = stoneName(w);
                s.tier = w.tier();
                s.yours = w.circle() == sim.boundIndex();
                s.colour = NomadPlugin.colour(sim.circles().get(w.circle()), 255);
                s.where = (w.wanders() ? "Wandering" : w.landmark()) + (s.yours ? "" : ", in " + sim.circles().get(w.circle()).name());
                if (sim.ritesOn() && !w.riteDone()) s.where += " · rite: " + Waystone.riteText(w.rite());
                s.left = stoneLeft(sim, w);
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
