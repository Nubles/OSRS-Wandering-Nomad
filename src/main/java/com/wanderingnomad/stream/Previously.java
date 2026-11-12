package com.wanderingnomad.stream;

import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Waystone;
import com.wanderingnomad.social.Dare;
import java.util.ArrayList;
import java.util.List;

/**
 * "Previously on": logging in after a break, a three-line card recaps the last episode's biggest
 * beats and ends on the thread it left hanging.
 */
public final class Previously
{
    /** A break at least this long, in real minutes, earns a recap; a shorter one carries the episode on. */
    public static final int BREAK_MINUTES = 30;
    static final int BEATS = 3;

    private Previously()
    {
    }

    /** The card's lines: up to three beats, then the open thread if there is one. */
    public static List<String> of(Session last)
    {
        List<String> out = new ArrayList<>();
        for (Beat b : last.top(BEATS)) out.add(b.text());
        if (out.isEmpty()) out.add("A quiet episode: " + hours(last.played()) + " of steady drifting.");
        if (last.openThread != null) out.add(last.openThread);
        return out;
    }

    /** What's left hanging as the episode ends, in priority order. */
    public static String openThread(NomadSim sim, Journey j, boolean outside, int now)
    {
        Circle main = sim.main();
        if (sim.forkWarning() > 0)
        {
            return "Left hanging: Main was about to fork, " + NomadSim.compass(sim.forkAxis()) + " or " + NomadSim.compass(sim.forkAxis() + Math.PI) + ".";
        }
        if (sim.stormWarning() > 0) return "Left hanging: a storm was brewing over Main.";
        if (main.sailing() && main.voyageTo() != null) return "Left hanging: Main was at sea, bound for " + main.voyageTo() + ".";
        if (outside) return "Left hanging: you logged out outside your circle.";
        Dare d = j == null ? null : j.dare();
        if (d != null && now < d.deadline && d.progress(j) < d.target) return "Left hanging: " + d.from + "'s dare, " + lower(d.ask()) + ", still open.";
        for (Waystone w : sim.waystones())
        {
            if (w.circle() == sim.boundIndex()) return "Left hanging: a " + w.boon() + " waystone still stands at " + w.landmark() + ".";
        }
        return "Left hanging: Main was drifting " + NomadSim.compass(main.heading()) + ", " + main.moodName().toLowerCase() + ".";
    }

    static String lower(String s)
    {
        return s.isEmpty() ? s : Character.toLowerCase(s.charAt(0)) + s.substring(1);
    }

    public static String hours(int minutes)
    {
        return minutes / 60 + "h " + String.format("%02d", minutes % 60) + "m";
    }
}
