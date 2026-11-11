package com.wanderingnomad.stream;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the streaming tools keep on an account: the episode under way and the last one, every
 * episode's drama score, and the run's echoes, narrator lines, flags and wraps. Saved as JSON.
 */
public final class StreamState
{
    /** Episodes kept for ranking drama scores. */
    public static final int EPISODES_KEPT = 500;

    public static final class Episode
    {
        public int number;
        public long ended;
        public int minutes;
        public int drama;
        public String prophecy;
        public Boolean cameTrue;
    }

    /** The run the per-run parts below belong to. */
    public long seed;
    public int sessions;
    public Session session;
    public Session last;
    public List<Episode> episodes = new ArrayList<>();
    public Echoes echoes = new Echoes();
    public Narrator narrator = new Narrator();
    public Flags flags = new Flags();
    /** Wraps given this run, in hundreds of played hours. */
    public int wrapped;
    /** The latest "Next time on" card, and dead-air facts offered lately. */
    public List<String> nextTime = new ArrayList<>();
    public List<String> recentFacts = new ArrayList<>();

    public StreamState ready()
    {
        if (episodes == null) episodes = new ArrayList<>();
        echoes = echoes == null ? new Echoes() : echoes.ready();
        narrator = narrator == null ? new Narrator() : narrator.ready();
        flags = flags == null ? new Flags() : flags.ready();
        if (nextTime == null) nextTime = new ArrayList<>();
        if (recentFacts == null) recentFacts = new ArrayList<>();
        if (session != null) session.ready();
        if (last != null) last.ready();
        return this;
    }

    /** A different run: its echoes, wraps and flags start fresh; the episode history stays. */
    public void run(long seed)
    {
        if (this.seed == seed) return;
        this.seed = seed;
        echoes = new Echoes();
        flags = new Flags();
        wrapped = 0;
        nextTime = new ArrayList<>();
        if (session != null && session.seed != seed) session = null;
    }

    /** Drama scores of every earlier episode. */
    public List<Integer> scores()
    {
        List<Integer> out = new ArrayList<>();
        for (Episode e : episodes) out.add(e.drama);
        return out;
    }

    /** Ends an episode: judges its prophecy, ranks its drama and keeps it as the last one. Returns its rank. */
    public String finish(Session s, long now)
    {
        Drama d = Drama.of(s);
        String rank = Drama.rank(d.total, scores());
        Episode e = new Episode();
        e.number = s.number;
        e.ended = now;
        e.minutes = s.played();
        e.drama = d.total;
        if (s.prophecy != null)
        {
            if (s.prophecy.cameTrue == null) s.prophecy.cameTrue = s.prophecy.judge(s);
            e.prophecy = s.prophecy.line;
            e.cameTrue = s.prophecy.cameTrue;
        }
        episodes.add(e);
        while (episodes.size() > EPISODES_KEPT) episodes.remove(0);
        s.wrapped = true;
        last = s;
        if (session == s) session = null;
        return rank;
    }
}
