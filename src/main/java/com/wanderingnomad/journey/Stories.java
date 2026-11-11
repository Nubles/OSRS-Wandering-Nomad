package com.wanderingnomad.journey;

import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Story;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/** Builds a run's {@link Story} from its journey, small enough to ride in a run code. */
public final class Stories
{
    /** At most this many hourly points, this many beats and this many squares go in a code. */
    static final int TRACK = 240;
    static final int BEATS = 90;
    static final int SQUARES = 2500;

    private Stories()
    {
    }

    public static Story of(NomadSim sim, Journey j, boolean hardcore)
    {
        Story s = new Story();
        s.track = spread(j.hours, TRACK);
        s.kingdoms = new LinkedHashMap<>(j.kingdomMinutes);
        s.moods = new LinkedHashMap<>(j.moodMinutes);
        s.smallest = j.smallest;
        s.closest = j.closestCall();
        s.partings = j.partings.toArray(new int[0][]);
        TreeSet<Integer> squares = new TreeSet<>();
        for (String id : j.items(Trials.SQUARES))
        {
            try
            {
                squares.add(Integer.parseInt(id));
            }
            catch (NumberFormatException e)
            {
                // Not a square id.
            }
            if (squares.size() >= SQUARES) break;
        }
        s.squares = squares.stream().mapToInt(Integer::intValue).toArray();
        s.acts = new ArrayList<>();
        for (Journey.Act a : j.acts) s.acts.add(new Story.Chapter(a.start, a.opened, a.kingdom));
        List<Story.Beat> beats = new ArrayList<>();
        for (Journey.Moment m : j.moments) beats.add(new Story.Beat(m.minute, m.x, m.y, "moment", m.title, m.line));
        for (Journey.Scar sc : j.scars) beats.add(new Story.Beat(sc.minute, -1, -1, sc.kind, null, sc.line));
        beats.sort(Comparator.comparingInt(b -> b.minute));
        s.beats = beats.size() > BEATS ? new ArrayList<>(beats.subList(beats.size() - BEATS, beats.size())) : beats;
        Map<String, Integer> c = new LinkedHashMap<>();
        c.put("walked", j.value(Trials.WALKED));
        c.put("claimed", j.value(Trials.CLAIMED));
        c.put("boons", j.value(Trials.BOONS));
        c.put("crossings", j.value(Trials.CROSSINGS));
        c.put("forks", sim.forks());
        c.put("storms", sim.storms());
        c.put("voyages", j.value(Trials.VOYAGES));
        c.put("harbours", j.value(Trials.HARBOURS));
        c.put("regions", j.value(Trials.REGIONS));
        c.put("landmarks", j.value(Trials.LANDMARKS));
        c.put("omens", j.value(Trials.OMENS));
        c.put("trials", j.doneCount());
        c.put("streak", j.value(Trials.STREAK));
        int deaths = 0;
        for (Journey.Scar sc : j.scars) if ("death".equals(sc.kind)) deaths++;
        c.put("deaths", deaths);
        s.counts = c;
        s.grave = j.grave();
        s.hardcore = hardcore;
        return s;
    }

    /** At most {@code max} of the list's points, spread evenly, keeping the first and the last. */
    static int[][] spread(List<int[]> list, int max)
    {
        int n = list.size();
        if (n <= max) return list.toArray(new int[0][]);
        int[][] out = new int[max][];
        for (int i = 0; i < max; i++) out[i] = list.get((int) Math.round(i * (n - 1) / (double) (max - 1)));
        return out;
    }
}
