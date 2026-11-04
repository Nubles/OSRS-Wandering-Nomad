package com.wanderingnomad.rival;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;

/**
 * A friend's live run, followed through the relay: the name you gave them, their live code, and
 * the latest run the relay had. Only the plugin's poller writes it, and the rest only read it.
 */
public final class Rival
{
    public final String name;
    public final String code;
    public final int colour;
    volatile Snapshot latest;
    /** Why the last read failed, if it did; null once a read works. */
    volatile String problem;
    /** The relay's version of the run we have, for If-None-Match. */
    volatile String etag;

    /** One read of the relay: their run as it was published, and when. */
    public static final class Snapshot
    {
        public final NomadSim sim;
        public final Tile surface;
        public final boolean online;
        /** When they published it, in epoch milliseconds. */
        public final long at;

        Snapshot(NomadSim sim, Tile surface, boolean online, long at)
        {
            this.sim = sim;
            this.surface = surface;
            this.online = online;
            this.at = at;
        }
    }

    Rival(String name, String code, int colour)
    {
        this.name = name;
        this.code = code;
        this.colour = colour;
    }

    public Snapshot latest()
    {
        return latest;
    }

    public String problem()
    {
        return problem;
    }

    public String etag()
    {
        return etag;
    }

    /** Online and published within the last {@link Rivals#FRESH_MS}, so where they stand still means something. */
    public boolean fresh(long now)
    {
        Snapshot s = latest;
        return s != null && s.online && now - s.at <= Rivals.FRESH_MS;
    }

    /** Their Main circle, and the circle they're bound to. */
    public Circle main()
    {
        Snapshot s = latest;
        return s == null ? null : s.sim.main();
    }

    public Circle bound()
    {
        Snapshot s = latest;
        return s == null ? null : s.sim.bound();
    }
}
