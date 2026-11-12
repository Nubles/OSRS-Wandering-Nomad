package com.wanderingnomad.stream;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Waystone;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One episode: a play session from login to logout (a short break carries on the same one), with
 * its beats, its tallies, where Main went, its session prophecy and its stake. Plain fields so it
 * saves as JSON.
 */
public final class Session
{
    /** The most beats an episode keeps; the lightest go first. */
    public static final int BEATS_MAX = 300;
    /** Within this many tiles of your circle's edge counts as living on the edge. */
    public static final double EDGE_TILES = 3;
    /** Tallies kept besides each kind of beat. */
    public static final String OUT_MINUTES = "outmin";
    public static final String EDGE_MINUTES = "edgemin";
    public static final String STORM_MINUTES = "stormmin";
    public static final String SAILED = "sailed";
    public static final String MAIN_MOODS = "mainmoods";
    /** Waystones that rose in Main or your circle. */
    public static final String STONES_RISEN = "stonesrisen";

    public int number;
    public long seed;
    public long startedAt;
    public long lastSeen;
    /** When you started recording, or 0 if you haven't. */
    public long recordingAt;
    public int startMinute;
    public int minute;
    public double startX;
    public double startY;
    public double endX;
    public double endY;
    public String mainMood;
    public List<Beat> beats = new ArrayList<>();
    public Map<String, Integer> counts = new HashMap<>();
    /** Kingdoms Main's middle passed over, and landmarks it covered. */
    public Set<String> regions = new HashSet<>();
    public Set<String> landmarks = new HashSet<>();
    /** Waystones seen in Main or your circle, by landmark and circle. */
    public Set<String> stones = new HashSet<>();
    public Foretold prophecy;
    public Stake stake;
    /** What was left hanging when you logged out, for "Previously on". */
    public String openThread;
    public boolean wrapped;

    public static Session start(int number, NomadSim sim, long now)
    {
        Session s = new Session();
        s.number = number;
        s.seed = sim.seed();
        s.startedAt = now;
        s.lastSeen = now;
        s.startMinute = sim.minutes();
        s.minute = sim.minutes();
        s.startX = sim.main().x();
        s.startY = sim.main().y();
        s.endX = s.startX;
        s.endY = s.startY;
        s.mainMood = sim.main().moodName();
        s.cover(sim.main());
        s.stones(sim, false);
        return s;
    }

    /** Fills in what an older save didn't have. */
    public Session ready()
    {
        if (beats == null) beats = new ArrayList<>();
        if (counts == null) counts = new HashMap<>();
        if (regions == null) regions = new HashSet<>();
        if (landmarks == null) landmarks = new HashSet<>();
        if (stones == null) stones = new HashSet<>();
        return this;
    }

    public int count(String key)
    {
        Integer n = counts.get(key);
        return n == null ? 0 : n;
    }

    public void add(String key, int by)
    {
        counts.merge(key, by, Integer::sum);
    }

    /** Played minutes this episode has lasted. */
    public int played()
    {
        return minute - startMinute;
    }

    /** Notes a beat; past the cap, the lightest earliest one is dropped. */
    public Beat beat(Beat b)
    {
        beats.add(b);
        add(b.kind, 1);
        if (beats.size() > BEATS_MAX)
        {
            Beat lightest = beats.get(0);
            for (Beat x : beats) if (x.weight < lightest.weight) lightest = x;
            beats.remove(lightest);
        }
        return b;
    }

    /**
     * A played minute: where Main went, its mood, and how close you lived to your edge.
     * {@code margin} is tiles from your tile in to your edge, below 0 outside, NaN if unknown.
     */
    public void minute(NomadSim sim, double margin, long now)
    {
        minute = sim.minutes();
        lastSeen = now;
        Circle main = sim.main();
        endX = main.x();
        endY = main.y();
        cover(main);
        stones(sim, true);
        if (!main.moodName().equals(mainMood))
        {
            mainMood = main.moodName();
            add(MAIN_MOODS, 1);
        }
        if (main.sailing()) add(SAILED, 1);
        if (sim.stormLeft() > 0) add(STORM_MINUTES, 1);
        if (!Double.isNaN(margin))
        {
            if (margin < 0) add(OUT_MINUTES, 1);
            else if (margin <= EDGE_TILES) add(EDGE_MINUTES, 1);
        }
    }

    private void stones(NomadSim sim, boolean count)
    {
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != NomadSim.MAIN && w.circle() != sim.boundIndex()) continue;
            if (stones.add(w.landmark() + "|" + w.circle()) && count) add(STONES_RISEN, 1);
        }
    }

    private void cover(Circle main)
    {
        Region r = Geography.regionAt(main.x(), main.y());
        if (r != null) regions.add(r.name);
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (main.contains(l.x, l.y)) landmarks.add(l.name);
        }
    }

    /** Played minutes since the last beat that wasn't just stepping in or out. */
    public int quiet()
    {
        int last = startMinute;
        for (Beat b : beats)
        {
            if (!Beat.OUT.equals(b.kind) && !Beat.BACK.equals(b.kind)) last = Math.max(last, b.minute);
        }
        return minute - last;
    }

    /** The {@code n} heaviest beats, earliest first among equals, told in the order they happened. */
    public List<Beat> top(int n)
    {
        List<Beat> sorted = new ArrayList<>(beats);
        sorted.removeIf(b -> b.weight <= 0);
        sorted.sort(Comparator.comparingInt((Beat b) -> -b.weight).thenComparingLong(b -> b.at));
        List<Beat> out = new ArrayList<>(sorted.subList(0, Math.min(n, sorted.size())));
        out.sort(Comparator.comparingLong(b -> b.at));
        return out;
    }

    /** The latest beat worth a chapter, or null. */
    public Beat latest()
    {
        for (int i = beats.size() - 1; i >= 0; i--)
        {
            if (beats.get(i).weight >= 2) return beats.get(i);
        }
        return null;
    }
}
