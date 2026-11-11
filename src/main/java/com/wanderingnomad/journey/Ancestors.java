package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ancestor ghosts: the Main circles of your latest past runs keep drifting on the map as faint
 * rings, each played from its own seed with no one inside it, minute for minute with this run.
 * Standing inside one (and your own circle) lights a trial.
 */
public final class Ancestors
{
    /** One ghost: the run it was, and its circles played on from that run's seed. */
    public static final class Ghost
    {
        public final Legacy.Run run;
        final NomadSim sim;

        Ghost(Legacy.Run run, NomadSim sim)
        {
            this.run = run;
            this.sim = sim;
        }

        public Circle main()
        {
            return sim.main();
        }
    }

    private final List<Ghost> ghosts = new ArrayList<>();

    private Ancestors()
    {
    }

    /**
     * Ghosts of the latest runs, each played to {@code minutes}. A long run takes a while to play
     * out, so call it off the client thread.
     */
    public static Ancestors of(List<Legacy.Run> runs, int minutes)
    {
        Ancestors a = new Ancestors();
        for (Legacy.Run r : Legacy.ghosts(runs))
        {
            Ghost g = new Ghost(r, NomadSim.start(r.seed));
            a.ghosts.add(g);
        }
        a.minute(minutes);
        return a;
    }

    /** Plays every ghost on to {@code minutes}. */
    public void minute(int minutes)
    {
        for (Ghost g : ghosts)
        {
            while (g.sim.minutes() < minutes) g.sim.step(null);
        }
    }

    public List<Ghost> ghosts()
    {
        return Collections.unmodifiableList(ghosts);
    }

    /** The ghost whose ring covers a tile, or null. */
    public Ghost at(double x, double y)
    {
        for (Ghost g : ghosts)
        {
            if (g.main().contains(x, y)) return g;
        }
        return null;
    }
}
