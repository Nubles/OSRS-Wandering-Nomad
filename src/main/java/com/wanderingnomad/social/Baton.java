package com.wanderingnomad.social;

import com.wanderingnomad.sim.SavedRun;
import java.util.ArrayList;
import java.util.List;

/**
 * Baton: a relay run passed between friends. Each plays a leg on their own account, then hands on
 * a code, and the next leg starts with the circles where the last one left them. The team scores
 * distance, kingdoms and waystones, never levels.
 */
public final class Baton
{
    public static final int TEAM_MAX = 24;
    /** Played minutes a new leg counts as inside while its runner travels to the circle. */
    public static final int GRACE_MINUTES = 120;

    /** One leg: who ran it, for how long, and what it did. */
    public static final class Leg
    {
        public String name;
        public int minutes;
        public int tiles;
        public int kingdoms;
        public int stones;

        public Leg()
        {
        }

        public Leg(String name, int minutes, int tiles, int kingdoms, int stones)
        {
            this.name = Codes.clean(name, Bottle.NAME_MAX);
            this.minutes = Math.max(0, minutes);
            this.tiles = Math.max(0, tiles);
            this.kingdoms = Math.max(0, kingdoms);
            this.stones = Math.max(0, stones);
        }

        /** A tile walked is worth a hundredth of a point, a kingdom ten and a waystone five. */
        public int score()
        {
            return tiles / 100 + kingdoms * 10 + stones * 5;
        }
    }

    public String team;
    public List<Leg> legs = new ArrayList<>();
    /** The circles as the last leg left them; only in the code, not kept on the run. */
    public SavedRun run;

    /** The baton handed on after {@code leg}: the team's legs so far, and the circles to carry. */
    public static Baton hand(Baton before, String team, Leg leg, SavedRun run)
    {
        Baton b = new Baton();
        b.team = Codes.clean(before != null && before.team != null ? before.team : team, TEAM_MAX);
        if (b.team.isEmpty()) b.team = "Relay";
        if (before != null) b.legs.addAll(before.legs);
        b.legs.add(leg);
        b.run = run;
        return b;
    }

    /** Checks a pasted baton; false if it's not a real one. */
    public boolean valid()
    {
        if (run == null || run.sim == null || legs == null || legs.isEmpty()) return false;
        team = Codes.clean(team, TEAM_MAX);
        for (Leg l : legs) l.name = Codes.clean(l.name, Bottle.NAME_MAX);
        return true;
    }

    public int score()
    {
        int s = 0;
        for (Leg l : legs) s += l.score();
        return s;
    }

    /** The team's story, a line per leg and the score. */
    public List<String> lines()
    {
        List<String> out = new ArrayList<>();
        out.add("Team " + team + ": " + legs.size() + (legs.size() == 1 ? " leg, " : " legs, ") + String.format("%,d", score()) + " points.");
        for (int i = 0; i < legs.size(); i++)
        {
            Leg l = legs.get(i);
            out.add("Leg " + (i + 1) + ", " + l.name + ": " + (l.minutes / 60) + "h " + String.format("%02d", l.minutes % 60) + "m, "
                + String.format("%,d", l.tiles) + " tiles, " + l.kingdoms + (l.kingdoms == 1 ? " kingdom, " : " kingdoms, ")
                + l.stones + (l.stones == 1 ? " waystone" : " waystones") + " (" + l.score() + " points)");
        }
        return out;
    }
}
