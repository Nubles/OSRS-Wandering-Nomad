package com.wanderingnomad;

import com.wanderingnomad.journey.Acts;
import com.wanderingnomad.journey.Curiosities;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Legacy;
import com.wanderingnomad.journey.Lineage;
import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.journey.Omens;
import com.wanderingnomad.journey.Prophecy;
import com.wanderingnomad.journey.Rank;
import com.wanderingnomad.journey.Renown;
import com.wanderingnomad.journey.Trial;
import com.wanderingnomad.journey.Trials;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** The trials and rank as the sidebar shows them: a snapshot made on the client thread. */
final class JourneyView
{
    static final JourneyView EMPTY = new JourneyView();
    /** Trials shown as "up next" in the sidebar. */
    static final int NEXT = 3;

    /** One trial's row. */
    static final class Row
    {
        final String chapter;
        final String name;
        final String detail;
        final String tier;
        final Color colour;
        final double progress;
        final String count;
        final boolean done;
        final boolean carried;
        final int renown;
        /** How many runs on this account have done it before. */
        final int times;

        Row(Trial t, Journey j, Renown r)
        {
            chapter = t.chapter;
            boolean hidden = t.riddle != null && !j.isDone(t);
            name = hidden ? "???" : t.name;
            detail = hidden ? t.riddle : t.detail;
            tier = t.tier.label;
            colour = new Color(t.tier.colour);
            progress = j.progress(t);
            done = j.isDone(t);
            carried = j.isCarried(t);
            renown = t.tier.renown;
            times = r.times(t);
            int v = Math.min(t.target, Math.max(0, j.value(t.metric)));
            count = done ? "Done" : carried ? "Before this run" : String.format("%,d / %,d", v, t.target);
        }
    }

    boolean live;
    String rank = Rank.WANDERER.title;
    Color rankColour = new Color(Rank.WANDERER.colour);
    int renown;
    String nextRank;
    int nextRenown;
    double toNext;
    int done;
    int total;
    int runs;
    String palette = Rank.WANDERER.palette;
    List<String> palettes = Collections.singletonList(Rank.WANDERER.palette);
    /** The omen under way: its title, what it asks, minutes left and progress; null title if none. */
    String omen;
    String omenAsk;
    int omenLeft;
    double omenProgress;
    /** The run's oath: its trial's name, or null; how far along; and the names it could be sworn to. */
    String oath;
    double oathProgress;
    boolean oathKept;
    /** Whether the oath can still be sworn or changed. */
    boolean oathOpen;
    List<String> oaths = Collections.emptyList();
    /** The run ledger's text, set by the plugin, which has the run; null before login. */
    String ledger;
    /** One line per rival followed, set by the plugin; empty when Rivals is off. */
    List<String> rivals = Collections.emptyList();
    /** Rivals whose run has been read, so a new run can race one of them on their seed. */
    List<String> rivalNames = Collections.emptyList();
    List<Row> next = Collections.emptyList();
    List<Row> all = Collections.emptyList();
    /** Main's line now, such as "Gertrude III", and its family tree, oldest first. */
    String heir;
    List<String> lineage = Collections.emptyList();
    /** The curiosities log: each one's name, what it takes, and how often the account has seen it (0 if never). */
    List<String[]> curios = Collections.emptyList();
    int curiosFound;
    /** Legacy: the act under way, the run's title so far (set by the plugin), the account's run number, and whether it's laid down. */
    String act;
    String title;
    int generation = 1;
    boolean retired;
    /** Prophecy oath: whether they can be sealed now, and each sealed one with what has come true so far (empty if none). */
    boolean prophecyOpen;
    List<String> prophecies = Collections.emptyList();
    /** Wager oath, set by the plugin: the two halves' compass names while a call can be made (null otherwise), and the half called. */
    String wagerWith;
    String wagerAgainst;
    String wagered;
    /** Ended runs on the account, newest first. */
    List<Legacy.Run> legacy = Collections.emptyList();

    static JourneyView of(Journey j, Renown r, List<Trial> trials)
    {
        return of(j, r, trials, 0);
    }

    static JourneyView of(Journey j, Renown r, List<Trial> trials, int minutes)
    {
        JourneyView v = new JourneyView();
        if (j == null || r == null || trials == null) return v;
        v.live = true;
        Rank rank = r.rank();
        v.rank = rank.title;
        v.rankColour = new Color(rank.colour);
        v.renown = r.renown();
        Rank n = rank.next();
        v.nextRank = n == null ? null : n.title;
        v.nextRenown = n == null ? 0 : n.renown;
        v.toNext = r.toNext();
        v.done = j.doneCount();
        v.total = trials.size();
        v.runs = r.runs();
        v.palette = r.paletteName();
        v.palettes = r.unlocked();
        List<String> oaths = new ArrayList<>();
        for (Trial t : trials)
        {
            if (Trials.OATHS.contains(t.id) && !j.isCarried(t) && (!j.isDone(t) || t.id.equals(j.oath()))) oaths.add(t.name);
            if (t.id.equals(j.oath()))
            {
                v.oath = t.name;
                v.oathProgress = j.progress(t);
                v.oathKept = j.isDone(t);
            }
        }
        v.oaths = oaths;
        v.oathOpen = j.canSwear(minutes);
        Omen o = Omens.current(j);
        if (o != null)
        {
            v.omen = o.title();
            v.omenAsk = o.ask();
            v.omenLeft = Math.max(0, o.deadline() - minutes);
            v.omenProgress = o.progress(Omens.counted(j, o), minutes);
        }
        List<Row> next = new ArrayList<>();
        for (Trial t : j.next(trials, NEXT)) next.add(new Row(t, j, r));
        v.next = next;
        List<Row> all = new ArrayList<>();
        for (String chapter : Trials.CHAPTERS)
        {
            for (Trial t : trials)
            {
                if (t.chapter.equals(chapter)) all.add(new Row(t, j, r));
            }
        }
        v.all = all;
        v.heir = Lineage.current(j);
        v.lineage = Lineage.tree(j, minutes);
        List<String[]> curios = new ArrayList<>();
        for (Curiosities.Curio c : Curiosities.ALL) curios.add(new String[]{c.name, c.detail, String.valueOf(r.curioTimes(c.id))});
        v.curios = curios;
        v.curiosFound = r.curiosFound();
        v.act = Acts.current(j);
        v.generation = Math.max(1, r.runs());
        List<Legacy.Run> legacy = new ArrayList<>(r.legacy());
        Collections.reverse(legacy);
        v.legacy = legacy;
        v.prophecyOpen = Prophecy.canSeal(j, minutes);
        String[] sealed = j.prophecy();
        if (sealed != null)
        {
            List<String> lines = new ArrayList<>();
            for (int i = 0; i < 3; i++)
            {
                String came = j.truth()[i];
                lines.add(Prophecy.ASKS[i] + ": " + sealed[i] + (came == null ? " (not yet known)" : sealed[i].equals(came) ? " (so it was)"
                    : " (it was " + came + ")"));
            }
            v.prophecies = lines;
        }
        return v;
    }
}
