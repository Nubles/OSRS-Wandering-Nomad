package com.wanderingnomad.stream;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * "Next time on": at logout, what the circles will do in the first minutes of the next session
 * if you log in and do nothing, read from the run's own future. Fighting it is the hook.
 */
public final class NextTime
{
    public static final int MINUTES = 20;
    /** The most beats the card tells. */
    static final int MAX = 5;
    static final String QUIET = "A quiet start: the circles drift on, and the edge keeps clear of you.";

    private NextTime()
    {
    }

    /** The beats, each as "In N min: ...", for standing on {@code at} (null counts as inside). */
    public static List<String> of(NomadSim sim, Tile at, int minutes)
    {
        NomadSim copy = sim.copy();
        List<String> out = new ArrayList<>();
        boolean wasOut = at != null && !copy.inYours(at.x, at.y);
        String mood = copy.bound().moodName();
        int bound = copy.boundIndex();
        for (int m = 1; m <= minutes && out.size() < MAX; m++)
        {
            List<String> lines = copy.step(at);
            String when = "In " + m + " min: ";
            for (String line : lines)
            {
                if (out.size() < MAX) out.add(when + line);
            }
            if (at != null && !wasOut && copy.isOutside() && out.size() < MAX)
            {
                out.add(when + "the edge passes you, and you're outside.");
            }
            wasOut = at != null && copy.isOutside();
            if (copy.boundIndex() == bound && !copy.bound().moodName().equals(mood) && out.size() < MAX)
            {
                out.add(when + "your circle turns " + copy.bound().moodName().toLowerCase() + ".");
            }
            mood = copy.bound().moodName();
            bound = copy.boundIndex();
        }
        if (out.isEmpty()) out.add(QUIET);
        return out;
    }
}
