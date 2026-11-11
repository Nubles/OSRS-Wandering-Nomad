package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Comet;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Curiosities: rare coincidences of the circles that the plugin spots by itself, kept in a
 * collection log across runs. No guide can farm them, since they come from where the circles
 * happen to drift. A fixed list, written once; each is about once in tens to hundreds of hours.
 */
public final class Curiosities
{
    /** One curiosity: its id (saved), its name and what it takes. */
    public static final class Curio
    {
        public final String id;
        public final String name;
        public final String detail;

        Curio(String id, String name, String detail)
        {
            this.id = id;
            this.name = name;
            this.detail = detail;
        }
    }

    public static final Curio ECLIPSE = new Curio("eclipse", "Eclipse", "Main and a kingdom's circle share a middle, within 5 tiles.");
    public static final Curio STORM_COMET = new Curio("storm-comet", "Comet in a storm", "A comet streaks by while a storm blows over Main.");
    public static final Curio TRIPLE = new Curio("triple", "Triple point", "Stand inside three circles at once.");
    public static final Curio AT_YOUR_FEET = new Curio("at-your-feet", "At your feet", "A waystone rises within 5 tiles of you.");
    public static final Curio FULL_HOUSE = new Curio("full-house", "Full house", "Hold three of the same boon.");
    public static final Curio QUICK_REUNION = new Curio("quick-reunion", "Quick reunion",
        "A fork's lost half finds Main again within two hours of the parting.");
    public static final Curio CROWDED = new Curio("crowded", "Crowded", "Main touches two kingdoms' circles at once.");
    public static final Curio GIANT = new Curio("giant", "Giant", "Main grows to within 3 tiles of its full size.");
    public static final Curio HOMECOMING = new Curio("homecoming", "Homecoming",
        "After a day of play, Main's middle passes within 10 tiles of Lumbridge, where it began.");
    public static final Curio STAR_TOUCHED = new Curio("star-touched", "Star-touched", "A comet flies over a risen waystone.");
    public static final Curio GHOST_LIGHT = new Curio("ghost-light", "Ghost light", "A comet flies through a fork's lost half.");
    public static final Curio WASHED_UP = new Curio("washed-up", "Washed up at your feet",
        "Main washes up from a castaway voyage with you standing inside it.");
    public static final Curio ROUND_TRIP = new Curio("round-trip", "Round trip", "Main sails back to the port it last sailed from.");

    public static final List<Curio> ALL = Collections.unmodifiableList(Arrays.asList(
        ECLIPSE, STORM_COMET, TRIPLE, AT_YOUR_FEET, FULL_HOUSE, QUICK_REUNION, CROWDED, GIANT, HOMECOMING,
        STAR_TOUCHED, GHOST_LIGHT, WASHED_UP, ROUND_TRIP));

    /** Renown for finding one for the first time on the account. */
    public static final int RENOWN = 50;

    private static final Pattern BOARDED = Pattern.compile("boarded a boat at (.+), bound for (.+)\\.$");

    /** What the last look saw, for the curiosities that are changes. Not saved: a reload only forgets a minute or two. */
    private final Set<String> stones = new HashSet<>();
    /** Curiosities seen at the last look, so one that lasts a while is only told when it begins. */
    private final Set<Curio> showing = new HashSet<>();
    private boolean primed;
    private int ghostLeft;
    private boolean castaway;
    private boolean sailing;
    private String lastFrom;
    private double cometX = Double.NaN;
    private double cometY = Double.NaN;

    public static Curio byId(String id)
    {
        for (Curio c : ALL)
        {
            if (c.id.equals(id)) return c;
        }
        return null;
    }

    /** Forgets the last look, as for a new run. */
    public void reset()
    {
        stones.clear();
        showing.clear();
        primed = false;
        ghostLeft = 0;
        castaway = false;
        sailing = false;
        lastFrom = null;
        cometX = cometY = Double.NaN;
    }

    /**
     * The curiosities that begin this minute, from the run as it is now and the minute's log. One
     * that lasts several minutes is only returned when it begins; it may come again later.
     */
    public List<Curio> spot(NomadSim sim, Tile player, List<String> lines)
    {
        List<Curio> out = new ArrayList<>();
        Circle m = sim.main();
        List<Circle> circles = sim.circles();

        int touching = 0;
        for (int j = 1; j < circles.size(); j++)
        {
            Circle o = circles.get(j);
            if (!o.isKingdom()) continue;
            if (Circle.dist(m.x() - o.x(), m.y() - o.y()) <= 5) add(out, ECLIPSE);
            if (m.touches(o)) touching++;
        }
        if (sim.eclipse() > 0) add(out, ECLIPSE);
        if (touching >= 2) add(out, CROWDED);
        if (sim.comet() != null && sim.comet().flying() && sim.stormLeft() > 0) add(out, STORM_COMET);
        if (m.radius() >= m.radiusMax() - 3 && !m.sailing()) add(out, GIANT);
        if (sim.minutes() >= 24 * 60 && !m.sailing()
            && Circle.dist(m.x() - Geography.LUMBRIDGE.x, m.y() - Geography.LUMBRIDGE.y) <= 10)
        {
            add(out, HOMECOMING);
        }
        List<String> boons = sim.boons();
        if (boons.size() >= NomadSim.BOONS_HELD && new HashSet<>(boons).size() == 1) add(out, FULL_HOUSE);
        if (player != null)
        {
            int inside = 0;
            for (Circle c : circles)
            {
                if (c.contains(player.x, player.y)) inside++;
            }
            if (sim.twin() != null && sim.twin().contains(player.x, player.y)) inside++;
            if (inside >= 3) add(out, TRIPLE);
        }

        Set<String> stonesNow = new HashSet<>();
        for (Waystone w : sim.waystones())
        {
            String key = w.landmark() + "|" + w.circle();
            stonesNow.add(key);
            if (primed && !stones.contains(key) && player != null && Circle.dist(w.x() - player.x, w.y() - player.y) <= 5)
            {
                add(out, AT_YOUR_FEET);
            }
        }

        Comet k = sim.comet();
        if (k != null && k.flying() && !Double.isNaN(cometX))
        {
            for (Waystone w : sim.waystones())
            {
                if (NomadSim.segmentDist(w.x(), w.y(), cometX, cometY, k.x(), k.y()) <= NomadSim.COMET_RADIUS) add(out, STAR_TOUCHED);
            }
            Circle ghost = sim.ghost();
            if (ghost != null && NomadSim.segmentDist(ghost.x(), ghost.y(), cometX, cometY, k.x(), k.y()) <= ghost.radius())
            {
                add(out, GHOST_LIGHT);
            }
        }

        for (String line : lines)
        {
            if (line.startsWith("Reunion!") && ghostLeft >= NomadSim.GHOST_MINUTES - 120) add(out, QUICK_REUNION);
            if (line.startsWith("Castaway!")) castaway = true;
            Matcher b = BOARDED.matcher(line);
            if (b.find())
            {
                if (b.group(2).equals(lastFrom)) add(out, ROUND_TRIP);
                lastFrom = b.group(1);
            }
        }
        if (sailing && !m.sailing())
        {
            if (castaway && player != null && m.contains(player.x, player.y)) add(out, WASHED_UP);
            castaway = false;
        }

        primed = true;
        stones.clear();
        stones.addAll(stonesNow);
        ghostLeft = sim.ghost() == null ? 0 : sim.ghostLeft();
        sailing = m.sailing();
        cometX = k != null && k.flying() ? k.x() : Double.NaN;
        cometY = k != null && k.flying() ? k.y() : Double.NaN;
        List<Curio> fresh = new ArrayList<>();
        for (Curio c : out)
        {
            if (!showing.contains(c)) fresh.add(c);
        }
        showing.clear();
        showing.addAll(out);
        return fresh;
    }

    private static void add(List<Curio> out, Curio c)
    {
        if (!out.contains(c)) out.add(c);
    }
}
