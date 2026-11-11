package com.wanderingnomad.stream;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Waystone;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The session prophecy: each episode opens with one cryptic line that the drift will make true if
 * you follow the circle, read from the run's own seeded future. It's revealed when the episode
 * ends, and a prophecy you didn't let come true is stamped "Fate defied".
 */
public final class Foretold
{
    /** Played minutes of the future the prophecy reads: a long evening's play. */
    public static final int WINDOW = 120;
    /** Main must end at least this far from where it began for a heading to count. */
    static final double WANDER = 15;
    /** Within this many tiles of where it began, Main "lingered". */
    static final double LINGER = 30;

    public static final String REGION = "region";
    public static final String LANDMARK = "landmark";
    public static final String SAIL = "sail";
    public static final String STORM = "storm";
    public static final String STONE = "stone";
    public static final String MOODS = "moods";
    public static final String WAY = "way";
    public static final String STAY = "stay";

    public String kind;
    public String arg;
    /** The cryptic line, as told at the start. */
    public String line;
    /** Whether it came true, once revealed; null until then. */
    public Boolean cameTrue;

    public Foretold()
    {
    }

    Foretold(String kind, String arg, String line)
    {
        this.kind = kind;
        this.arg = arg;
        this.line = line;
    }

    /**
     * Reads the next {@link #WINDOW} minutes on a copy of the run, as if you followed the circle,
     * and picks one thing that happens, rarer things first. {@code number} is the episode's.
     */
    public static Foretold foretell(NomadSim sim, int number)
    {
        NomadSim copy = sim.copy();
        Circle start = copy.main();
        double sx = start.x();
        double sy = start.y();
        Region home = Geography.regionAt(sx, sy);
        Set<String> regions = new LinkedHashSet<>();
        Set<String> covered = new HashSet<>();
        for (Landmarks.Landmark l : Landmarks.ALL) if (start.contains(l.x, l.y)) covered.add(l.name);
        String landmark = null;
        Set<String> stones = new HashSet<>();
        for (Waystone w : copy.waystones()) stones.add(w.landmark() + "|" + w.circle());
        boolean sailed = false;
        boolean storm = false;
        boolean stone = false;
        int moods = 0;
        String mood = start.moodName();
        for (int m = 0; m < WINDOW; m++)
        {
            copy.step(null);
            Circle main = copy.main();
            Region r = Geography.regionAt(main.x(), main.y());
            if (r != null && r != home) regions.add(r.name);
            if (landmark == null)
            {
                for (Landmarks.Landmark l : Landmarks.ALL)
                {
                    if (!covered.contains(l.name) && main.contains(l.x, l.y))
                    {
                        landmark = l.name;
                        break;
                    }
                }
            }
            sailed |= main.sailing();
            storm |= copy.stormLeft() > 0;
            for (Waystone w : copy.waystones())
            {
                if (stones.add(w.landmark() + "|" + w.circle()) && (w.circle() == NomadSim.MAIN || w.circle() == copy.boundIndex())) stone = true;
            }
            if (!main.moodName().equals(mood))
            {
                mood = main.moodName();
                moods++;
            }
        }
        int pick = mix(copy.seed() * 31L + number);
        boolean alt = (pick & 1) == 0;
        List<Foretold> rare = new ArrayList<>();
        if (sailed) rare.add(new Foretold(SAIL, null, alt ? "Salt water will carry the circle before the night is out."
            : "The circle hears the sea calling, and it will answer."));
        if (storm) rare.add(new Foretold(STORM, null, alt ? "The sky will break over the circle tonight."
            : "Thunder is owed, and it will be paid before you rest."));
        if (!regions.isEmpty())
        {
            String r = regions.iterator().next();
            rare.add(new Foretold(REGION, r, alt ? "The heart of the circle will beat in " + r + " before the night is out."
                : r + " is waiting, and the circle will answer."));
        }
        if (!rare.isEmpty()) return rare.get(Math.floorMod(pick >> 1, rare.size()));
        List<Foretold> common = new ArrayList<>();
        if (stone) common.add(new Foretold(STONE, null, alt ? "A stone will rise for the nomad tonight."
            : "Something old will push up through the earth inside the circle."));
        if (landmark != null) common.add(new Foretold(LANDMARK, landmark, alt ? "The circle will pass its shadow over " + landmark + "."
            : landmark + " lies in the circle's path tonight."));
        if (moods >= 2) common.add(new Foretold(MOODS, String.valueOf(moods), alt ? "The circle will change its heart " + moods + " times."
            : moods + " moods will pass through the circle before you rest."));
        if (!common.isEmpty()) return common.get(Math.floorMod(pick >> 1, common.size()));
        double dx = copy.main().x() - sx;
        double dy = copy.main().y() - sy;
        if (Math.hypot(dx, dy) >= WANDER)
        {
            String way = NomadSim.compass(Math.atan2(dy, dx));
            return new Foretold(WAY, way, alt ? "The circle will end the night " + way + " of where it began."
                : "The " + way + " wind will take the circle.");
        }
        return new Foretold(STAY, null, alt ? "The circle will not stray far tonight." : "The circle will linger, as if waiting.");
    }

    /** Whether the episode made it true. */
    public boolean judge(Session s)
    {
        switch (kind)
        {
            case REGION:
                return s.regions.contains(arg);
            case LANDMARK:
                return s.landmarks.contains(arg);
            case SAIL:
                return s.count(Session.SAILED) > 0;
            case STORM:
                return s.count(Session.STORM_MINUTES) > 0;
            case STONE:
                return s.count(Session.STONES_RISEN) > 0;
            case MOODS:
                return s.count(Session.MAIN_MOODS) >= Integer.parseInt(arg);
            case WAY:
            {
                double dx = s.endX - s.startX;
                double dy = s.endY - s.startY;
                return Math.hypot(dx, dy) >= WANDER && NomadSim.compass(Math.atan2(dy, dx)).equals(arg);
            }
            default:
                return Math.hypot(s.endX - s.startX, s.endY - s.startY) < LINGER;
        }
    }

    /** The reveal, once judged. */
    public String reveal()
    {
        return "\"" + line + "\" " + (Boolean.TRUE.equals(cameTrue) ? "It came to pass." : "Fate defied.");
    }

    static int mix(long v)
    {
        long h = v * 0x9E3779B97F4A7C15L;
        h ^= h >>> 32;
        h *= 0x45D9F3BL;
        return (int) (h ^ (h >>> 29));
    }
}
