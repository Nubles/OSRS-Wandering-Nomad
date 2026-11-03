package com.wanderingnomad;

import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.journey.Omens;
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
            name = t.name;
            detail = t.detail;
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
    List<Row> next = Collections.emptyList();
    List<Row> all = Collections.emptyList();

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
            int now = Omen.WANDERLUST.equals(o.kind()) ? j.value(Trials.WALKED) : Omens.trophies(j);
            v.omenProgress = o.progress(now, minutes);
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
        return v;
    }
}
